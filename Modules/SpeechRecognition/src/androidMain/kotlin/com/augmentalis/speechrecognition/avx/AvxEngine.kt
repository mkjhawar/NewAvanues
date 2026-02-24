/**
 * AvxEngine.kt - AVX (AvaVox) ONNX-based command recognition engine for Android
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Implements the AVX command engine using Sherpa-ONNX Runtime on Android.
 * Key features:
 * - Hot words boosting for known voice commands (instant, no recompilation)
 * - Per-language tuned models (~60-75MB each)
 * - N-best hypotheses for better command matching
 * - AON encryption for all model files
 *
 * Audio pipeline: Android AudioRecord (16kHz mono) -> VAD -> AVX ONNX decoder
 *
 * Unlike Vivoka (which compiles a grammar in 3-8s), AVX accepts hot words as
 * score multipliers — updating the command list is instantaneous.
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
import com.augmentalis.speechrecognition.whisper.WhisperVAD
import com.augmentalis.speechrecognition.whisper.VADState
import com.augmentalis.speechrecognition.whisper.OnSpeechChunkReady
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
import java.util.concurrent.atomic.AtomicReference

/**
 * AVX (AvaVox) speech recognition engine for Android.
 *
 * Uses Sherpa-ONNX Runtime for ONNX model inference with hot words support.
 * Models are stored as AON-encrypted files and decrypted at load time.
 */
class AvxEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "AvxEngine"
        private const val ENGINE_NAME = "AVX"
        private const val ENGINE_VERSION = "1.0.0"
        private const val MAX_INIT_RETRIES = 2
        private const val INIT_RETRY_DELAY_MS = 1000L
        private const val LISTEN_POLL_MS = 80L // Slightly faster than Whisper (commands need low latency)

        /** Shared model storage directory */
        val MODEL_DIR = File(
            Environment.getExternalStorageDirectory(),
            "ava-ai-models/avx"
        )
    }

    // State
    private val engineState = AtomicReference(AvxEngineState.UNINITIALIZED)
    val state: AvxEngineState get() = engineState.get()

    // Configuration
    private var config = AvxConfig()
    private var speechMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND

    // ONNX Runtime session handle (0 = not loaded)
    @Volatile
    private var sessionPtr: Long = 0L

    // Components — reuses WhisperAudio and WhisperVAD since they're engine-agnostic
    private val audio = WhisperAudio()
    private var vad: WhisperVAD? = null
    private val confidenceScorer = ConfidenceScorer()
    private var voiceStateManager: VoiceStateManager? = null

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
     * Initialize the AVX engine: load ONNX model with Sherpa-ONNX Runtime.
     *
     * @param avxConfig AVX-specific configuration
     * @return true if initialization succeeded
     */
    suspend fun initialize(avxConfig: AvxConfig = AvxConfig()): Boolean {
        if (engineState.get() == AvxEngineState.READY) {
            Log.w(TAG, "Already initialized")
            return true
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
     * Start listening for speech with VAD-driven chunking.
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
            listenLoop()
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

        val remainingAudio = audio.drainBuffer()
        val minSamples = (200 * WhisperAudio.SAMPLE_RATE / 1000).toLong()
        if (remainingAudio.size > minSamples) {
            scope.launch {
                transcribeChunk(remainingAudio)
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
     * Unlike Vivoka's grammar compilation (3-8s), this is instantaneous.
     */
    fun updateCommands(commands: List<String>) {
        config = config.withCommands(commands)
        // Hot words are applied on the next recognition pass — no model reload needed
        Log.d(TAG, "Hot words updated: ${config.hotWords.size} commands")
    }

    /**
     * Set recognition mode.
     */
    fun setMode(mode: SpeechMode) {
        speechMode = mode
    }

    /**
     * Attach voice state manager for mute/sleep integration.
     */
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

        if (sessionPtr != 0L) {
            AvxNative.freeSession(sessionPtr)
            sessionPtr = 0L
        }

        engineState.set(AvxEngineState.DESTROYED)
        scope.cancel()

        Log.d(TAG, "AvxEngine destroyed")
    }

    // --- Private implementation ---

    private suspend fun performInitialization(): Boolean {
        // Step 1: Load Sherpa-ONNX native library
        if (!AvxNative.ensureLoaded()) {
            throw IllegalStateException("Failed to load sherpa-onnx native library")
        }

        // Step 2: Initialize audio capture (reuses WhisperAudio — same 16kHz mono)
        if (!audio.initialize()) {
            throw IllegalStateException("Failed to initialize audio capture")
        }

        audio.onError = { error ->
            scope.launch {
                _errorFlow.emit(SpeechError.audioError(error))
            }
        }

        // Step 3: Initialize VAD with tighter settings for command recognition
        vad = WhisperVAD(
            speechThreshold = 0f,
            vadSensitivity = 0.7f,   // Slightly more sensitive than Whisper
            silenceTimeoutMs = 500,   // Faster finalization for short commands
            minSpeechDurationMs = 150, // Shorter min — commands are brief
            maxSpeechDurationMs = 10_000,
            hangoverFrames = 3,       // Less hangover — commands don't trail off
            paddingMs = 100
        )

        // Step 4: Resolve and load model
        val modelPath = resolveModelPath()
            ?: throw IllegalStateException(
                "AVX model not found for ${config.language.displayName}. " +
                "Expected: ${MODEL_DIR.absolutePath}/${config.language.aonFileName}"
            )

        Log.i(TAG, "Loading AVX model: $modelPath")

        // Step 5: Decrypt AON and load into ONNX Runtime
        val modelFile = File(modelPath)
        val modelBytes = modelFile.readBytes()
        val decryptedBytes = AONCodec.unwrap(modelBytes)

        // Write decrypted model to temp file for ONNX Runtime
        val tempDir = File(context.cacheDir, "avx_tmp")
        tempDir.mkdirs()
        val tempModel = File(tempDir, "model.onnx")

        try {
            tempModel.writeBytes(decryptedBytes)
            sessionPtr = AvxNative.createSession(
                modelPath = tempModel.absolutePath,
                numThreads = config.effectiveThreadCount(),
                sampleRate = config.sampleRate
            )
            if (sessionPtr == 0L) {
                throw IllegalStateException("Failed to create ONNX Runtime session")
            }
        } finally {
            tempModel.delete()
            Log.d(TAG, "Deleted decrypted temp model")
        }

        // Step 6: Apply initial hot words
        if (config.hotWords.isNotEmpty()) {
            AvxNative.setHotWords(
                sessionPtr,
                config.hotWords.map { it.phrase }.toTypedArray(),
                config.hotWords.map { it.boost }.toFloatArray()
            )
        }

        Log.i(TAG, "AVX model loaded: ${config.language.displayName}")
        return true
    }

    /**
     * Resolve the model file path.
     * Priority: customModelPath > shared AVX storage
     */
    private fun resolveModelPath(): String? {
        config.customModelPath?.let { path ->
            if (File(path).exists()) return path
        }

        val modelFile = File(MODEL_DIR, config.language.aonFileName)
        if (modelFile.exists() && modelFile.length() > 0) return modelFile.absolutePath

        Log.w(TAG, "AVX model not found: ${config.language.aonFileName}")
        return null
    }

    /**
     * Main listen loop — VAD-driven, same pattern as WhisperEngine.
     */
    private suspend fun listenLoop() {
        Log.d(TAG, "AVX listen loop started")

        val activeVad = vad ?: run {
            Log.e(TAG, "VAD not initialized")
            return
        }

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
                val nowMs = System.currentTimeMillis()
                activeVad.processAudio(samples, nowMs)

                // Emit partial indicator when speech detected
                if (activeVad.getState() == VADState.SPEECH) {
                    _resultFlow.emit(RecognitionResult(
                        text = "",
                        confidence = 0f,
                        isPartial = true,
                        isFinal = false,
                        engine = ENGINE_NAME,
                        mode = speechMode.name
                    ))
                }
            }
        }

        activeVad.flush()
        Log.d(TAG, "AVX listen loop ended")
    }

    /**
     * Transcribe an audio chunk using the ONNX Runtime session.
     * Returns N-best hypotheses for better command matching.
     */
    private suspend fun transcribeChunk(audioData: FloatArray) {
        val ptr = sessionPtr
        if (ptr == 0L) {
            Log.w(TAG, "Cannot transcribe — session not loaded")
            return
        }

        try {
            val startMs = System.currentTimeMillis()

            // Apply current hot words before transcription
            if (config.hotWords.isNotEmpty()) {
                AvxNative.setHotWords(
                    ptr,
                    config.hotWords.map { it.phrase }.toTypedArray(),
                    config.hotWords.map { it.boost }.toFloatArray()
                )
            }

            val result = withContext(Dispatchers.Default) {
                AvxNative.transcribe(
                    sessionPtr = ptr,
                    audioData = audioData,
                    nBest = config.nBestCount
                )
            }

            val processingTimeMs = System.currentTimeMillis() - startMs
            val audioDurationMs = (audioData.size * 1000L) / config.sampleRate

            if (result.text.isBlank()) {
                Log.d(TAG, "Empty AVX result (${audioDurationMs}ms audio, ${processingTimeMs}ms proc)")
                return
            }

            totalTranscriptions++
            totalLatencyMs += processingTimeMs

            val confidenceResult = confidenceScorer.createResult(
                text = result.text,
                rawConfidence = result.confidence,
                engine = RecognitionEngine.AVX
            )

            Log.i(TAG, "AVX result: '${result.text}' conf=${result.confidence} " +
                    "(${audioDurationMs}ms audio, ${processingTimeMs}ms proc, " +
                    "alternatives=${result.alternatives.size})")

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
                    "processingTimeMs" to processingTimeMs,
                    "audioDurationMs" to audioDurationMs,
                    "language" to config.language.langCode,
                    "hotWordsCount" to config.hotWords.size,
                    "nBestCount" to result.alternatives.size + 1,
                    "confidence_level" to confidenceResult.level.name,
                    "scoring_method" to confidenceResult.scoringMethod.name
                )
            )

            _resultFlow.emit(recognitionResult)
            voiceStateManager?.updateCommandExecutionTime()

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AVX transcription error", e)
            _errorFlow.emit(SpeechError(
                code = SpeechError.ERROR_UNKNOWN,
                message = "AVX transcription failed: ${e.message}",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.RETRY
            ))
        }
    }
}
