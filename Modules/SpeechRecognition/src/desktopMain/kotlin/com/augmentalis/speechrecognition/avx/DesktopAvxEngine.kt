/**
 * DesktopAvxEngine.kt - AVX (AvaVox) ONNX-based command engine for Desktop JVM
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Desktop JVM implementation of the AVX command engine using Sherpa-ONNX
 * streaming transducer recognition. On Desktop, AVX is the PRIMARY command
 * engine (no Vivoka available) — no pre-filter needed.
 *
 * Architecture:
 * Audio (DesktopWhisperAudio) → OnlineRecognizer (streaming) → endpoint detection
 *     → getResult() → RecognitionResult flow
 *
 * Model storage: ~/.augmentalis/models/avx/Ava-AvxS-{Lang}.aon
 */
package com.augmentalis.speechrecognition.avx

import com.augmentalis.crypto.aon.AONCodec
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.SpeechError
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.logWarn
import com.augmentalis.speechrecognition.whisper.DesktopWhisperAudio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipInputStream

/**
 * Desktop AVX engine using Sherpa-ONNX streaming transducer recognition.
 *
 * On Desktop, AVX is the PRIMARY command engine (no Vivoka available).
 * Whisper remains for dictation. No pre-filter needed.
 *
 * Sherpa-ONNX is an OPTIONAL dependency — if the classes.jar or native
 * libraries aren't present, the engine gracefully reports unavailable
 * (initialize() returns false) without crashing the system.
 */
class DesktopAvxEngine {
    companion object {
        private const val TAG = "DesktopAvxEngine"
        private const val ENGINE_NAME = "AVX"
        private const val ENGINE_VERSION = "1.1.0"
        private const val LISTEN_POLL_MS = 60L

        /** Desktop model storage */
        val MODEL_DIR: File = File(
            System.getProperty("user.home", "."),
            ".augmentalis/models/avx"
        )

        /**
         * Check if Sherpa-ONNX runtime classes are available on the classpath.
         * Call this BEFORE creating DesktopAvxNative or touching any Sherpa types.
         */
        fun isSherpaAvailable(): Boolean {
            return try {
                Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizer")
                true
            } catch (_: ClassNotFoundException) {
                false
            } catch (_: NoClassDefFoundError) {
                false
            }
        }
    }

    private val engineState = AtomicReference(AvxEngineState.UNINITIALIZED)
    val state: AvxEngineState get() = engineState.get()

    private var config = AvxConfig()
    private var speechMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND

    private var avxNative: DesktopAvxNative? = null
    private var extractedModelDir: File? = null

    private val audio = DesktopWhisperAudio()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listenJob: Job? = null

    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1, extraBufferCapacity = 16)
    val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    /**
     * Initialize the Desktop AVX engine.
     *
     * Pipeline:
     * 1. Load Sherpa-ONNX native library
     * 2. Initialize audio capture
     * 3. Locate and decrypt AON model file
     * 4. Unzip model archive → extract encoder/decoder/joiner/tokens
     * 5. Create OnlineRecognizer with transducer model config
     */
    suspend fun initialize(avxConfig: AvxConfig = AvxConfig()): Boolean {
        if (engineState.get() == AvxEngineState.READY) return true

        avxConfig.validate().getOrElse { e ->
            logError(TAG, "Invalid config: ${e.message}")
            return false
        }

        config = avxConfig
        engineState.set(AvxEngineState.LOADING_MODEL)

        return withContext(Dispatchers.IO) {
            try {
                // Check Sherpa-ONNX availability before touching any Sherpa classes
                if (!isSherpaAvailable()) {
                    logWarn(TAG, "Sherpa-ONNX classes not on classpath — AVX engine disabled")
                    engineState.set(AvxEngineState.ERROR)
                    return@withContext false
                }

                if (!DesktopAvxNative.ensureLoaded()) {
                    throw IllegalStateException("Failed to load sherpa-onnx native library on Desktop")
                }

                if (!audio.initialize()) {
                    throw IllegalStateException("Failed to initialize audio capture")
                }

                val aonFile = resolveModelFile()
                    ?: throw IllegalStateException("AVX model not found for ${config.language.displayName}")

                logInfo(TAG, "Loading AVX model: ${aonFile.absolutePath}")

                val modelPaths = decryptAndExtractModel(aonFile)

                val native = DesktopAvxNative()
                val created = native.createRecognizer(modelPaths, config)
                if (!created) {
                    throw IllegalStateException("Failed to create OnlineRecognizer")
                }

                avxNative = native
                engineState.set(AvxEngineState.READY)
                logInfo(TAG, "Desktop AVX engine initialized: ${config.language.displayName}, " +
                        "model=${aonFile.name}")
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: NoClassDefFoundError) {
                // Sherpa-ONNX classes missing at runtime — engine not available
                engineState.set(AvxEngineState.ERROR)
                logWarn(TAG, "Sherpa-ONNX runtime missing — AVX disabled: ${e.message}")
                false
            } catch (e: Exception) {
                engineState.set(AvxEngineState.ERROR)
                logError(TAG, "Desktop AVX initialization failed: ${e.message}", e)
                false
            }
        }
    }

    fun startListening(mode: SpeechMode = speechMode): Boolean {
        if (engineState.get() != AvxEngineState.READY) return false
        speechMode = mode

        if (!audio.isRecording()) {
            if (!audio.start()) return false
        }

        engineState.set(AvxEngineState.LISTENING)
        listenJob?.cancel()
        listenJob = scope.launch { streamingListenLoop() }
        return true
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        audio.stop()
        if (engineState.get() == AvxEngineState.LISTENING || engineState.get() == AvxEngineState.PROCESSING) {
            engineState.set(AvxEngineState.READY)
        }
    }

    /**
     * Update known commands as hot words for decoder biasing.
     * Instant update — no recompilation needed (unlike Vivoka grammar).
     */
    fun updateCommands(commands: List<String>) {
        config = config.withCommands(commands)
        avxNative?.updateHotWords(config.hotWords)
    }

    fun destroy() {
        listenJob?.cancel()
        audio.release()
        avxNative?.release()
        avxNative = null
        extractedModelDir?.deleteRecursively()
        extractedModelDir = null
        engineState.set(AvxEngineState.DESTROYED)
        scope.cancel()
    }

    /**
     * Streaming recognition loop using OnlineRecognizer.
     *
     * Unlike the old VAD-chunked approach, this feeds audio continuously to the
     * recognizer and relies on Sherpa-ONNX's built-in endpoint detection to
     * determine when an utterance is complete.
     */
    private suspend fun streamingListenLoop() {
        val native = avxNative ?: return
        var lastPartialText = ""

        while (scope.isActive && engineState.get() == AvxEngineState.LISTENING) {
            delay(LISTEN_POLL_MS)

            val samples = audio.drainBuffer()
            if (samples.isEmpty()) continue

            native.acceptWaveform(samples, config.sampleRate)
            native.decode()

            // Emit partial results for UI feedback
            val partial = native.getResult()
            if (partial.text.isNotEmpty() && partial.text != lastPartialText) {
                lastPartialText = partial.text
                _resultFlow.emit(RecognitionResult(
                    text = partial.text,
                    confidence = partial.confidence,
                    isPartial = true,
                    isFinal = false,
                    engine = ENGINE_NAME,
                    mode = speechMode.name,
                    metadata = mapOf(
                        "language" to config.language.langCode,
                        "streaming" to true
                    )
                ))
            }

            // Check for endpoint (end of utterance)
            if (native.isEndpoint()) {
                val finalResult = native.getResult()
                if (finalResult.text.isNotEmpty()) {
                    engineState.set(AvxEngineState.PROCESSING)

                    _resultFlow.emit(RecognitionResult(
                        text = finalResult.text,
                        confidence = finalResult.confidence,
                        isPartial = false,
                        isFinal = true,
                        alternatives = finalResult.alternatives,
                        engine = ENGINE_NAME,
                        mode = speechMode.name,
                        metadata = mapOf(
                            "language" to config.language.langCode,
                            "hotWordsCount" to config.hotWords.size,
                            "tokens" to finalResult.tokens.joinToString(" "),
                            "streaming" to true
                        )
                    ))

                    if (engineState.get() == AvxEngineState.PROCESSING) {
                        engineState.set(AvxEngineState.LISTENING)
                    }
                }

                native.reset()
                lastPartialText = ""
            }
        }
    }

    /**
     * Locate the AON model file for the configured language.
     */
    private fun resolveModelFile(): File? {
        config.customModelPath?.let { path ->
            val file = File(path)
            if (file.exists()) return file
        }
        val modelFile = File(MODEL_DIR, config.language.aonFileName)
        if (modelFile.exists() && modelFile.length() > 0) return modelFile
        logWarn(TAG, "AVX model not found: ${config.language.aonFileName} in ${MODEL_DIR.absolutePath}")
        return null
    }

    /**
     * Decrypt AON file and extract transducer model files.
     *
     * Pipeline:
     * 1. Read .aon file bytes
     * 2. AONCodec.unwrap() → decrypted zip bytes
     * 3. Unzip → 4 model files (encoder, decoder, joiner, tokens)
     * 4. Verify all expected files exist
     *
     * @return Paths to the 4 extracted model files
     */
    private suspend fun decryptAndExtractModel(aonFile: File): AvxModelPaths {
        val aonBytes = aonFile.readBytes()
        val zipBytes = AONCodec.unwrap(aonBytes)

        val extractDir = File(System.getProperty("java.io.tmpdir"), "avx_model_${config.language.langCode}")
        if (extractDir.exists()) extractDir.deleteRecursively()
        extractDir.mkdirs()
        extractedModelDir = extractDir

        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outFile = File(extractDir, entry.name)
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { out ->
                        zis.copyTo(out)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val modelFiles = config.language.modelFiles
        val encoderPath = File(extractDir, modelFiles.encoderFilename).absolutePath
        val decoderPath = File(extractDir, modelFiles.decoderFilename).absolutePath
        val joinerPath = File(extractDir, modelFiles.joinerFilename).absolutePath
        val tokensPath = File(extractDir, modelFiles.tokensFilename).absolutePath

        val missingFiles = listOf(encoderPath, decoderPath, joinerPath, tokensPath)
            .filter { !File(it).exists() }
        if (missingFiles.isNotEmpty()) {
            throw IllegalStateException("Missing model files after extraction: $missingFiles")
        }

        logInfo(TAG, "Model extracted to ${extractDir.absolutePath}: " +
                "${File(encoderPath).length() / 1024}KB encoder, " +
                "${File(tokensPath).length()} bytes tokens")

        return AvxModelPaths(
            encoderPath = encoderPath,
            decoderPath = decoderPath,
            joinerPath = joinerPath,
            tokensPath = tokensPath
        )
    }
}
