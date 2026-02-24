/**
 * AvxEngine.kt - AVX (AvaVox) ONNX-based command recognition engine for Android
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Implements the AVX command engine using Sherpa-ONNX OnlineRecognizer.
 * Key features:
 * - Hot words boosting for known voice commands (instant, no recompilation)
 * - Per-language tuned transducer models (encoder + decoder + joiner)
 * - Streaming recognition with endpoint detection
 * - AON encryption for all model files (archive containing 4 model files)
 *
 * Audio pipeline: Android AudioRecord (16kHz mono) -> OnlineRecognizer -> endpoint -> result
 *
 * Unlike Vivoka (which compiles a grammar in 3-8s), AVX accepts hot words as
 * score multipliers — updating the command list is instantaneous via stream recreation.
 *
 * Model loading: AON archive -> unzip -> 4 temp files -> OnlineRecognizer
 */
package com.augmentalis.speechrecognition.avx

import android.content.Context
import android.os.Environment
import android.util.Log
import com.augmentalis.speechrecognition.ConfidenceScorer
import com.augmentalis.speechrecognition.RecognitionEngine
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.SpeechError
import com.augmentalis.speechrecognition.SpeechMetricsSnapshot
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.speechrecognition.VoiceStateManager
import com.augmentalis.speechrecognition.whisper.WhisperAudio
import com.augmentalis.crypto.aon.AONCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipInputStream

/**
 * AVX (AvaVox) speech recognition engine for Android.
 *
 * Uses Sherpa-ONNX OnlineRecognizer (streaming transducer) for real-time
 * command recognition with hot words boosting.
 *
 * Models are stored as AON-encrypted zip archives containing:
 * - encoder-*.int8.onnx (~40-120MB)
 * - decoder-*.int8.onnx (~0.5MB)
 * - joiner-*.int8.onnx (~0.3MB)
 * - tokens.txt (~5KB)
 */
class AvxEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "AvxEngine"
        private const val ENGINE_NAME = "AVX"
        private const val ENGINE_VERSION = "1.1.0"
        private const val MAX_INIT_RETRIES = 2
        private const val INIT_RETRY_DELAY_MS = 1000L
        private const val LISTEN_POLL_MS = 60L // Fast polling for streaming recognizer

        /** Shared model storage directory */
        val MODEL_DIR = File(
            Environment.getExternalStorageDirectory(),
            "ava-ai-models/avx"
        )

        /**
         * Check if Sherpa-ONNX runtime classes are available on the classpath.
         * Call this BEFORE creating AvxNative or touching any Sherpa types.
         *
         * The Sherpa-ONNX AAR is a compileOnly dependency — it must be included
         * in the app's runtime classpath for AVX to work. If not, this returns
         * false and AVX gracefully disables.
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

    // State
    private val engineState = AtomicReference(AvxEngineState.UNINITIALIZED)
    val state: AvxEngineState get() = engineState.get()

    // Configuration
    private var config = AvxConfig()
    private var speechMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND

    // Sherpa-ONNX wrapper
    private var avxNative: AvxNative? = null

    // Audio capture — reuses WhisperAudio (engine-agnostic 16kHz mono capture)
    private val audio = WhisperAudio()
    private val confidenceScorer = ConfidenceScorer()
    private var voiceStateManager: VoiceStateManager? = null

    // Extracted model temp directory (cleaned up on destroy)
    private var extractedModelDir: File? = null

    // Coroutine management
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listenJob: Job? = null

    // Result flows
    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1, extraBufferCapacity = 16)
    val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    // Metrics
    private val _metricsSnapshot = MutableStateFlow<SpeechMetricsSnapshot?>(null)
    val metricsSnapshot: StateFlow<SpeechMetricsSnapshot?> = _metricsSnapshot.asStateFlow()

    private var totalTranscriptions = 0
    private var totalLatencyMs = 0L

    /**
     * Initialize the AVX engine: decrypt AON model, extract, load into OnlineRecognizer.
     *
     * @param avxConfig AVX-specific configuration
     * @return true if initialization succeeded
     */
    suspend fun initialize(avxConfig: AvxConfig = AvxConfig()): Boolean {
        if (engineState.get() == AvxEngineState.READY) {
            Log.w(TAG, "Already initialized")
            return true
        }

        // Check Sherpa-ONNX availability before touching any Sherpa classes
        if (!isSherpaAvailable()) {
            Log.w(TAG, "Sherpa-ONNX AAR not on classpath — AVX engine disabled")
            engineState.set(AvxEngineState.ERROR)
            return false
        }

        if (!avxConfig.language.hasTransducerModel) {
            Log.e(TAG, "Language ${avxConfig.language.displayName} has no transducer model (tier=${avxConfig.language.tier})")
            return false
        }

        avxConfig.validate().getOrElse { e ->
            Log.e(TAG, "Invalid config: ${e.message}")
            return false
        }

        config = avxConfig
        engineState.set(AvxEngineState.LOADING_MODEL)

        val initStartMs = System.currentTimeMillis()

        return withContext(Dispatchers.IO) {
            var lastError: Exception? = null

            for (attempt in 1..MAX_INIT_RETRIES) {
                try {
                    val success = performInitialization()
                    if (success) {
                        engineState.set(AvxEngineState.READY)
                        val initTime = System.currentTimeMillis() - initStartMs
                        Log.i(TAG, "Engine initialized in ${initTime}ms: lang=${config.language.langCode}, " +
                                "threads=${config.effectiveThreadCount()}, hotWords=${config.hotWords.size}")
                        return@withContext true
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: NoClassDefFoundError) {
                    // Sherpa-ONNX classes missing at runtime — don't retry
                    lastError = RuntimeException("Sherpa-ONNX runtime missing: ${e.message}", e)
                    Log.w(TAG, "Sherpa-ONNX runtime missing — AVX disabled: ${e.message}")
                    break
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Init attempt $attempt failed: ${e.message}")
                    if (attempt < MAX_INIT_RETRIES) {
                        delay(INIT_RETRY_DELAY_MS * attempt)
                    }
                }
            }

            engineState.set(AvxEngineState.ERROR)
            Log.e(TAG, "Initialization failed after $MAX_INIT_RETRIES attempts", lastError)
            scope.launch {
                _errorFlow.emit(SpeechError(
                    code = SpeechError.ERROR_MODEL,
                    message = "AVX initialization failed: ${lastError?.message}",
                    isRecoverable = true,
                    suggestedAction = SpeechError.Action.DOWNLOAD_MODEL
                ))
            }
            false
        }
    }

    /**
     * Start listening for speech via streaming recognition.
     *
     * Unlike the previous VAD-based approach, this uses Sherpa-ONNX's
     * built-in endpoint detection. Audio is continuously fed to the
     * OnlineRecognizer, which accumulates internal state and detects
     * when an utterance ends.
     */
    fun startListening(mode: SpeechMode = speechMode): Boolean {
        val currentState = engineState.get()
        if (currentState != AvxEngineState.READY) {
            Log.w(TAG, "Cannot start listening from state: $currentState")
            return false
        }

        speechMode = mode

        if (!audio.isRecording()) {
            if (!audio.start()) {
                Log.e(TAG, "Failed to start audio capture")
                return false
            }
        }

        engineState.set(AvxEngineState.LISTENING)

        listenJob?.cancel()
        listenJob = scope.launch {
            streamingListenLoop()
        }

        Log.i(TAG, "Listening started, mode=$mode, hotWords=${config.hotWords.size}")
        return true
    }

    /**
     * Stop listening and drain remaining audio.
     */
    fun stopListening() {
        listenJob?.cancel()
        listenJob = null

        // Feed remaining audio to get final result
        val remainingAudio = audio.drainBuffer()
        if (remainingAudio.isNotEmpty()) {
            scope.launch {
                feedAndFinalize(remainingAudio)
            }
        }

        audio.stop()

        if (engineState.get() == AvxEngineState.LISTENING ||
            engineState.get() == AvxEngineState.PROCESSING) {
            engineState.set(AvxEngineState.READY)
        }

        Log.i(TAG, "Listening stopped")
    }

    /**
     * Update the active command list (hot words).
     * Unlike Vivoka's grammar compilation (3-8s), this recreates the stream
     * with new hot words — takes <1ms.
     */
    fun updateCommands(commands: List<String>) {
        config = config.withCommands(commands)
        avxNative?.updateHotWords(config.hotWords)
        Log.d(TAG, "Hot words updated: ${config.hotWords.size} commands")
    }

    fun setMode(mode: SpeechMode) {
        speechMode = mode
    }

    fun setVoiceStateManager(manager: VoiceStateManager) {
        voiceStateManager = manager
    }

    fun isListening(): Boolean = engineState.get() == AvxEngineState.LISTENING
    fun isReady(): Boolean = engineState.get() == AvxEngineState.READY
    fun getEngineName(): String = ENGINE_NAME
    fun getEngineVersion(): String = ENGINE_VERSION
    fun requiresNetwork(): Boolean = false
    fun getMemoryUsageMB(): Int = config.language.approxSizeMB

    /**
     * Destroy the engine and release all resources.
     */
    fun destroy() {
        Log.d(TAG, "Destroying AvxEngine")

        listenJob?.cancel()
        listenJob = null
        audio.release()

        avxNative?.release()
        avxNative = null

        // Clean up extracted model files
        extractedModelDir?.let { dir ->
            dir.deleteRecursively()
            Log.d(TAG, "Cleaned up extracted model dir: ${dir.absolutePath}")
        }
        extractedModelDir = null

        engineState.set(AvxEngineState.DESTROYED)
        scope.cancel()

        Log.d(TAG, "AvxEngine destroyed")
    }

    // --- Private implementation ---

    private suspend fun performInitialization(): Boolean {
        // Step 1: Initialize audio capture
        if (!audio.initialize()) {
            throw IllegalStateException("Failed to initialize audio capture")
        }

        audio.onError = { error ->
            scope.launch {
                _errorFlow.emit(SpeechError.audioError(error))
            }
        }

        // Step 2: Resolve and decrypt AON model archive
        val aonFile = resolveModelFile()
            ?: throw IllegalStateException(
                "AVX model not found for ${config.language.displayName}. " +
                "Expected: ${MODEL_DIR.absolutePath}/${config.language.aonFileName}"
            )

        Log.i(TAG, "Loading AVX model: ${aonFile.absolutePath}")

        // Step 3: Decrypt AON archive and extract model files
        val modelPaths = decryptAndExtractModel(aonFile)

        // Step 4: Create OnlineRecognizer
        val native = AvxNative(context)
        val success = native.createRecognizer(modelPaths, config)
        if (!success) {
            throw IllegalStateException("Failed to create OnlineRecognizer")
        }

        avxNative = native

        Log.i(TAG, "AVX model loaded: ${config.language.displayName}")
        return true
    }

    /**
     * Decrypt the AON archive and extract model files to a temp directory.
     *
     * AON file format: AON header + encrypted zip payload + AON footer
     * Zip contents: encoder.onnx, decoder.onnx, joiner.onnx, tokens.txt
     *
     * The extracted files persist in cache until engine is destroyed,
     * since OnlineRecognizer needs file paths (not byte arrays).
     */
    private fun decryptAndExtractModel(aonFile: File): AvxModelPaths {
        val aonBytes = aonFile.readBytes()
        val zipBytes = AONCodec.unwrap(aonBytes)

        // Extract to a stable cache directory (persists while engine is active)
        val extractDir = File(context.cacheDir, "avx_model_${config.language.langCode}")
        if (extractDir.exists()) extractDir.deleteRecursively()
        extractDir.mkdirs()
        extractedModelDir = extractDir

        // Unzip model files
        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outFile = File(extractDir, entry.name)
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Resolve file paths using the language's model file config
        val files = config.language.modelFiles
        val encoderPath = File(extractDir, files.encoderFilename).absolutePath
        val decoderPath = File(extractDir, files.decoderFilename).absolutePath
        val joinerPath = File(extractDir, files.joinerFilename).absolutePath
        val tokensPath = File(extractDir, files.tokensFilename).absolutePath

        // Verify all files exist
        listOf(encoderPath, decoderPath, joinerPath, tokensPath).forEach { path ->
            if (!File(path).exists()) {
                throw IllegalStateException("Missing model file: $path")
            }
        }

        Log.i(TAG, "Model extracted to: ${extractDir.absolutePath}")
        return AvxModelPaths(encoderPath, decoderPath, joinerPath, tokensPath)
    }

    /**
     * Resolve the model file path.
     * Priority: customModelPath > shared AVX storage
     */
    private fun resolveModelFile(): File? {
        config.customModelPath?.let { path ->
            val file = File(path)
            if (file.exists()) return file
        }

        val modelFile = File(MODEL_DIR, config.language.aonFileName)
        if (modelFile.exists() && modelFile.length() > 0) return modelFile

        Log.w(TAG, "AVX model not found: ${config.language.aonFileName}")
        return null
    }

    /**
     * Streaming listen loop — feeds audio directly to OnlineRecognizer.
     *
     * Unlike the previous VAD-based approach (WhisperVAD -> chunk -> transcribe),
     * this uses Sherpa-ONNX's built-in streaming pipeline:
     * 1. Read audio from WhisperAudio buffer
     * 2. Feed to OnlineRecognizer via acceptWaveform
     * 3. Decode accumulated audio
     * 4. Check for endpoint (silence detection)
     * 5. On endpoint: emit result, reset stream
     */
    private suspend fun streamingListenLoop() {
        Log.d(TAG, "AVX streaming listen loop started")

        val native = avxNative ?: run {
            Log.e(TAG, "AvxNative not initialized")
            return
        }

        native.reset()

        var lastPartialText = ""

        while (scope.isActive && engineState.get() == AvxEngineState.LISTENING) {
            delay(LISTEN_POLL_MS)

            val samples = audio.drainBuffer()
            if (samples.isEmpty()) continue

            // Feed audio to recognizer
            native.acceptWaveform(samples, config.sampleRate)
            native.decode()

            // Check for partial result (for UI feedback)
            val partialResult = native.getResult()
            if (partialResult.text.isNotEmpty() && partialResult.text != lastPartialText) {
                lastPartialText = partialResult.text
                _resultFlow.emit(RecognitionResult(
                    text = partialResult.text,
                    confidence = 0f,
                    isPartial = true,
                    isFinal = false,
                    engine = ENGINE_NAME,
                    mode = speechMode.name
                ))
            }

            // Check for endpoint (end of utterance)
            if (native.isEndpoint()) {
                engineState.set(AvxEngineState.PROCESSING)

                val result = native.getResult()
                if (result.text.isNotBlank()) {
                    emitFinalResult(result)
                }

                // Reset for next utterance
                native.reset()
                lastPartialText = ""

                if (engineState.get() == AvxEngineState.PROCESSING) {
                    engineState.set(AvxEngineState.LISTENING)
                }
            }
        }

        Log.d(TAG, "AVX streaming listen loop ended")
    }

    /**
     * Feed remaining audio and finalize result (called on stop).
     */
    private suspend fun feedAndFinalize(audioData: FloatArray) {
        val native = avxNative ?: return

        native.acceptWaveform(audioData, config.sampleRate)
        native.decode()

        val result = native.getResult()
        if (result.text.isNotBlank()) {
            emitFinalResult(result)
        }
    }

    /**
     * Emit a final recognition result with confidence scoring and metadata.
     */
    private suspend fun emitFinalResult(result: AvxTranscriptionResult) {
        val startMs = System.currentTimeMillis()

        val confidenceResult = confidenceScorer.createResult(
            text = result.text,
            rawConfidence = result.confidence,
            engine = RecognitionEngine.AVX
        )

        totalTranscriptions++

        Log.i(TAG, "AVX result: '${result.text}' conf=${result.confidence} " +
                "(tokens=${result.tokens.size}, alternatives=${result.alternatives.size})")

        val recognitionResult = RecognitionResult(
            text = result.text,
            originalText = result.text,
            confidence = confidenceResult.confidence,
            isPartial = false,
            isFinal = true,
            alternatives = result.alternatives,
            engine = ENGINE_NAME,
            mode = speechMode.name,
            metadata = mapOf(
                "language" to config.language.langCode,
                "hotWordsCount" to config.hotWords.size,
                "tokenCount" to result.tokens.size,
                "decodingMethod" to config.decodingMethod,
                "modelTier" to config.language.tier.name,
                "confidence_level" to confidenceResult.level.name,
                "scoring_method" to confidenceResult.scoringMethod.name
            )
        )

        _resultFlow.emit(recognitionResult)
        voiceStateManager?.updateCommandExecutionTime()
    }
}
