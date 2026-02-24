/**
 * DesktopAvxEngine.kt - AVX (AvaVox) ONNX-based command engine for Desktop
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Desktop JVM implementation of the AVX command engine.
 * Uses Sherpa-ONNX via ONNX Runtime JNI (same native bindings as Android,
 * loaded from java.library.path instead of jniLibs).
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
import com.augmentalis.speechrecognition.whisper.WhisperVAD
import com.augmentalis.speechrecognition.whisper.VADState
import com.augmentalis.speechrecognition.whisper.OnSpeechChunkReady
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
import java.util.concurrent.atomic.AtomicReference

/**
 * Desktop AVX engine using Sherpa-ONNX Runtime via JNI.
 *
 * On Desktop, AVX is the PRIMARY command engine (no Vivoka available).
 * No pre-filter needed — AVX processes commands directly, Whisper for dictation.
 */
class DesktopAvxEngine {
    companion object {
        private const val TAG = "DesktopAvxEngine"
        private const val ENGINE_NAME = "AVX"
        private const val ENGINE_VERSION = "1.0.0"
        private const val LISTEN_POLL_MS = 80L

        /** Desktop model storage */
        val MODEL_DIR: File = File(
            System.getProperty("user.home", "."),
            ".augmentalis/models/avx"
        )
    }

    private val engineState = AtomicReference(AvxEngineState.UNINITIALIZED)
    val state: AvxEngineState get() = engineState.get()

    private var config = AvxConfig()
    private var speechMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND

    @Volatile
    private var sessionPtr: Long = 0L

    private val audio = DesktopWhisperAudio()
    private var vad: WhisperVAD? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listenJob: Job? = null

    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1, extraBufferCapacity = 16)
    val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    /**
     * Initialize the Desktop AVX engine.
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
                if (!DesktopAvxNative.ensureLoaded()) {
                    throw IllegalStateException("Failed to load sherpa-onnx native library on Desktop")
                }

                if (!audio.initialize()) {
                    throw IllegalStateException("Failed to initialize audio capture")
                }

                vad = WhisperVAD(
                    speechThreshold = 0f,
                    vadSensitivity = 0.7f,
                    silenceTimeoutMs = 500,
                    minSpeechDurationMs = 150,
                    maxSpeechDurationMs = 10_000,
                    hangoverFrames = 3,
                    paddingMs = 100
                )

                val modelPath = resolveModelPath()
                    ?: throw IllegalStateException("AVX model not found for ${config.language.displayName}")

                logInfo(TAG, "Loading AVX model: $modelPath")

                val modelBytes = File(modelPath).readBytes()
                val decryptedBytes = AONCodec.unwrap(modelBytes)

                val tempDir = File(System.getProperty("java.io.tmpdir"), "avx_tmp")
                tempDir.mkdirs()
                val tempModel = File(tempDir, "model.onnx")

                try {
                    tempModel.writeBytes(decryptedBytes)
                    sessionPtr = DesktopAvxNative.createSession(
                        modelPath = tempModel.absolutePath,
                        numThreads = config.effectiveThreadCount(isAndroid = false),
                        sampleRate = config.sampleRate
                    )
                    if (sessionPtr == 0L) {
                        throw IllegalStateException("Failed to create ONNX Runtime session")
                    }
                } finally {
                    tempModel.delete()
                }

                if (config.hotWords.isNotEmpty()) {
                    DesktopAvxNative.setHotWords(
                        sessionPtr,
                        config.hotWords.map { it.phrase }.toTypedArray(),
                        config.hotWords.map { it.boost }.toFloatArray()
                    )
                }

                engineState.set(AvxEngineState.READY)
                logInfo(TAG, "Desktop AVX engine initialized: ${config.language.displayName}")
                true
            } catch (e: CancellationException) {
                throw e
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
        listenJob = scope.launch { listenLoop() }
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

    fun updateCommands(commands: List<String>) {
        config = config.withCommands(commands)
    }

    fun destroy() {
        listenJob?.cancel()
        audio.release()
        if (sessionPtr != 0L) {
            DesktopAvxNative.freeSession(sessionPtr)
            sessionPtr = 0L
        }
        engineState.set(AvxEngineState.DESTROYED)
        scope.cancel()
    }

    private fun resolveModelPath(): String? {
        config.customModelPath?.let { path ->
            if (File(path).exists()) return path
        }
        val modelFile = File(MODEL_DIR, config.language.aonFileName)
        if (modelFile.exists() && modelFile.length() > 0) return modelFile.absolutePath
        logWarn(TAG, "AVX model not found: ${config.language.aonFileName}")
        return null
    }

    private suspend fun listenLoop() {
        val activeVad = vad ?: return

        activeVad.onSpeechChunkReady = OnSpeechChunkReady { audioData, _ ->
            scope.launch {
                engineState.set(AvxEngineState.PROCESSING)
                transcribeChunk(audioData)
                if (engineState.get() == AvxEngineState.PROCESSING) {
                    engineState.set(AvxEngineState.LISTENING)
                }
            }
        }

        activeVad.reset()

        while (scope.isActive && engineState.get() == AvxEngineState.LISTENING) {
            delay(LISTEN_POLL_MS)
            val samples = audio.drainBuffer()
            if (samples.isNotEmpty()) {
                activeVad.processAudio(samples, System.currentTimeMillis())
            }
        }

        activeVad.flush()
    }

    private suspend fun transcribeChunk(audioData: FloatArray) {
        val ptr = sessionPtr
        if (ptr == 0L) return

        try {
            val startMs = System.currentTimeMillis()

            if (config.hotWords.isNotEmpty()) {
                DesktopAvxNative.setHotWords(
                    ptr,
                    config.hotWords.map { it.phrase }.toTypedArray(),
                    config.hotWords.map { it.boost }.toFloatArray()
                )
            }

            val result = withContext(Dispatchers.Default) {
                DesktopAvxNative.transcribe(ptr, audioData, config.nBestCount)
            }

            if (result.text.isBlank()) return

            val processingTimeMs = System.currentTimeMillis() - startMs

            _resultFlow.emit(RecognitionResult(
                text = result.text,
                confidence = result.confidence,
                isPartial = false,
                isFinal = true,
                alternatives = result.alternatives,
                engine = ENGINE_NAME,
                mode = speechMode.name,
                metadata = mapOf(
                    "processingTimeMs" to processingTimeMs,
                    "language" to config.language.langCode,
                    "hotWordsCount" to config.hotWords.size
                )
            ))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError(TAG, "Desktop AVX transcription error: ${e.message}", e)
        }
    }
}
