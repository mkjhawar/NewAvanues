/**
 * WhisperEngine.kt - Whisper speech recognition engine orchestrator
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Orchestrates the Whisper speech recognition pipeline:
 * WhisperAudio (capture) → WhisperVAD (chunking) → WhisperNative (transcription)
 *
 * Lifecycle: UNINITIALIZED → LOADING_MODEL → READY → LISTENING → PROCESSING → READY → ...
 * Supports: offline recognition, multi-language, auto language detection, translation.
 */
package com.augmentalis.speechrecognition.whisper

import android.content.Context
import android.util.Log
import com.augmentalis.speechrecognition.CommandCache
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.SpeechError
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.speechrecognition.VoiceStateManager
import com.augmentalis.speechrecognition.whisper.vsm.VSMCodec
import com.augmentalis.speechrecognition.whisper.vsm.VSMFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Main Whisper speech recognition engine.
 *
 * Integrates audio capture, VAD-based chunking, and native whisper.cpp
 * transcription into a unified engine that matches the VivokaEngine pattern.
 */
class WhisperEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "WhisperEngine"
        private const val ENGINE_NAME = "Whisper"
        private const val ENGINE_VERSION = "1.0.0"
        private const val MAX_INIT_RETRIES = 2
        private const val INIT_RETRY_DELAY_MS = 1000L

        /** Polling interval for checking audio buffer during listening */
        private const val LISTEN_POLL_MS = 100L
    }

    // State
    private val engineState = AtomicReference(WhisperEngineState.UNINITIALIZED)
    val state: WhisperEngineState get() = engineState.get()

    // Configuration
    private var config = WhisperConfig()
    private var speechMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND

    // Native context pointer (0 = not loaded)
    @Volatile
    private var contextPtr: Long = 0L

    // Components
    private val audio = WhisperAudio()
    private var vad: WhisperVAD? = null
    private val commandCache = CommandCache()
    private var voiceStateManager: VoiceStateManager? = null
    private val modelManager = WhisperModelManager(context)

    // Coroutine management
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listenJob: Job? = null

    // Result flows
    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1)
    val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    // Performance tracking
    val performance = WhisperPerformance()

    // Legacy accessors for backward compatibility
    val totalTranscriptions: Int get() = performance.totalTranscriptions
    val averageLatencyMs: Long get() = performance.getAverageLatencyMs()

    /**
     * Initialize the engine: load native library and model.
     *
     * @param whisperConfig Whisper-specific configuration
     * @return true if initialization succeeded
     */
    suspend fun initialize(whisperConfig: WhisperConfig = WhisperConfig.autoTuned(context)): Boolean {
        if (engineState.get() == WhisperEngineState.READY) {
            Log.w(TAG, "Already initialized")
            return true
        }

        whisperConfig.validate().getOrElse { e ->
            Log.e(TAG, "Invalid config: ${e.message}")
            return false
        }

        config = whisperConfig
        engineState.set(WhisperEngineState.LOADING_MODEL)

        val initStartMs = System.currentTimeMillis()

        return withContext(Dispatchers.IO) {
            var lastError: Exception? = null
            var attempts = 0

            for (attempt in 1..MAX_INIT_RETRIES) {
                attempts = attempt
                try {
                    val success = performInitialization()
                    if (success) {
                        engineState.set(WhisperEngineState.READY)
                        val initTime = System.currentTimeMillis() - initStartMs
                        performance.recordInitialization(initTime, config.modelSize, attempts)
                        Log.i(TAG, "Engine initialized in ${initTime}ms: model=${config.modelSize}, " +
                                "threads=${config.effectiveThreadCount()}, lang=${config.language}")
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

            engineState.set(WhisperEngineState.ERROR)
            Log.e(TAG, "Initialization failed after $MAX_INIT_RETRIES attempts", lastError)
            scope.launch {
                _errorFlow.emit(SpeechError(
                    code = SpeechError.ERROR_MODEL,
                    message = "Whisper initialization failed: ${lastError?.message}",
                    isRecoverable = true,
                    suggestedAction = SpeechError.Action.DOWNLOAD_MODEL
                ))
            }
            false
        }
    }

    /**
     * Start listening for speech.
     * Audio capture begins, and transcription runs when speech chunks are detected.
     */
    fun startListening(mode: SpeechMode = speechMode): Boolean {
        val currentState = engineState.get()
        if (currentState != WhisperEngineState.READY && currentState != WhisperEngineState.PAUSED) {
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

        engineState.set(WhisperEngineState.LISTENING)

        // Launch the listen loop that monitors audio and triggers transcription
        listenJob?.cancel()
        listenJob = scope.launch {
            listenLoop()
        }

        Log.i(TAG, "Listening started, mode=$mode")
        return true
    }

    /**
     * Stop listening. Audio capture stops and any pending transcription completes.
     */
    fun stopListening() {
        listenJob?.cancel()
        listenJob = null

        // Transcribe whatever remains in the buffer
        val remainingAudio = audio.drainBuffer()
        if (remainingAudio.size > config.minSpeechDurationMs * WhisperAudio.SAMPLE_RATE / 1000) {
            scope.launch {
                transcribeChunk(remainingAudio)
            }
        }

        audio.stop()

        if (engineState.get() == WhisperEngineState.LISTENING ||
            engineState.get() == WhisperEngineState.PROCESSING) {
            engineState.set(WhisperEngineState.READY)
        }

        Log.i(TAG, "Listening stopped")
    }

    /**
     * Pause listening — keeps model loaded but stops audio capture.
     */
    fun pause() {
        if (engineState.get() == WhisperEngineState.LISTENING) {
            listenJob?.cancel()
            listenJob = null
            audio.stop()
            engineState.set(WhisperEngineState.PAUSED)
            Log.i(TAG, "Paused")
        }
    }

    /**
     * Resume from paused state.
     */
    fun resume(): Boolean {
        if (engineState.get() != WhisperEngineState.PAUSED) return false
        return startListening(speechMode)
    }

    /**
     * Set commands for command-mode recognition.
     */
    fun setCommands(staticCommands: List<String>, dynamicCommands: List<String> = emptyList()) {
        commandCache.setStaticCommands(staticCommands)
        commandCache.setDynamicCommands(dynamicCommands)
        Log.d(TAG, "Commands updated: ${staticCommands.size} static, ${dynamicCommands.size} dynamic")
    }

    /**
     * Set the speech recognition mode.
     */
    fun setMode(mode: SpeechMode) {
        speechMode = mode
        Log.d(TAG, "Mode set to: $mode")
    }

    /**
     * Set language. May require model reload if switching to/from English-only model.
     */
    fun setLanguage(language: String) {
        if (config.language == language) return
        config = config.copy(language = language)
        Log.i(TAG, "Language set to: $language")
    }

    /**
     * Attach a VoiceStateManager for mute/sleep/dictation state integration.
     */
    fun setVoiceStateManager(manager: VoiceStateManager) {
        voiceStateManager = manager
    }

    /**
     * Check if currently listening.
     */
    fun isListening(): Boolean = engineState.get() == WhisperEngineState.LISTENING

    /**
     * Check if engine is ready (model loaded, can start listening).
     */
    fun isReady(): Boolean = engineState.get() == WhisperEngineState.READY

    /**
     * Get engine info.
     */
    fun getEngineName(): String = ENGINE_NAME
    fun getEngineVersion(): String = ENGINE_VERSION
    fun requiresNetwork(): Boolean = false
    fun getMemoryUsageMB(): Int = config.modelSize.approxSizeMB

    /** Observe model download state for UI integration. */
    val modelDownloadState: StateFlow<ModelDownloadState> get() = modelManager.downloadState

    /** Access the model manager for download/delete operations. */
    fun getModelManager(): WhisperModelManager = modelManager

    fun getPerformanceMetrics(): Map<String, Any> {
        val metrics = performance.getMetrics().toMutableMap()
        metrics["engineVersion"] = ENGINE_VERSION
        metrics["threads"] = config.effectiveThreadCount()
        metrics["language"] = config.language
        return metrics
    }

    /**
     * Destroy the engine and release all resources.
     */
    fun destroy() {
        Log.d(TAG, "Destroying WhisperEngine")

        listenJob?.cancel()
        listenJob = null

        audio.release()

        if (contextPtr != 0L) {
            WhisperNative.freeContext(contextPtr)
            contextPtr = 0L
        }

        commandCache.clear()
        engineState.set(WhisperEngineState.DESTROYED)
        scope.cancel()

        Log.d(TAG, "WhisperEngine destroyed")
    }

    // --- Private implementation ---

    /**
     * Perform the actual initialization: load native lib + model.
     */
    private suspend fun performInitialization(): Boolean {
        // Step 1: Load native library
        if (!WhisperNative.ensureLoaded()) {
            throw IllegalStateException("Failed to load whisper-jni native library")
        }

        // Step 2: Initialize audio
        if (!audio.initialize()) {
            throw IllegalStateException("Failed to initialize audio capture")
        }

        audio.onError = { error ->
            scope.launch {
                _errorFlow.emit(SpeechError.audioError(error))
            }
        }

        // Step 2b: Initialize VAD
        vad = WhisperVAD(
            speechThreshold = 0f, // auto-calibrate from noise floor
            silenceTimeoutMs = config.silenceThresholdMs,
            minSpeechDurationMs = config.minSpeechDurationMs,
            maxSpeechDurationMs = config.maxChunkDurationMs,
            paddingMs = 150
        )

        // Step 3: Load model (auto-download if missing)
        var modelPath = config.resolveModelPath(context)

        if (modelPath == null) {
            Log.i(TAG, "Model ${config.modelSize.displayName} not found, auto-downloading...")
            val downloaded = modelManager.downloadModel(config.modelSize)
            if (!downloaded) {
                throw IllegalStateException(
                    "Failed to download Whisper model: ${config.modelSize.displayName}. " +
                    "Check network connection and storage space."
                )
            }
            modelPath = config.resolveModelPath(context)
                ?: throw IllegalStateException(
                    "Model file not found after download: ${config.modelSize.ggmlFileName}"
                )
        }

        // Load model — decrypt .vsm to temp file if needed
        Log.i(TAG, "Loading model: $modelPath")
        val ptr: Long

        if (modelPath.endsWith(VSMFormat.VSM_EXTENSION)) {
            // Encrypted VSM: decrypt to temp file, load, delete temp
            val tempDir = File(context.cacheDir, "vlm_tmp")
            tempDir.mkdirs()

            val codec = VSMCodec()
            val decryptedFile = codec.decryptToTempFile(modelPath, tempDir)
                ?: throw IllegalStateException("VSM decryption failed: $modelPath")

            try {
                ptr = WhisperNative.initContext(decryptedFile.absolutePath)
                if (ptr == 0L) {
                    throw IllegalStateException("Failed to load whisper model from decrypted VSM")
                }
            } finally {
                // Always clean up decrypted temp file
                decryptedFile.delete()
                Log.d(TAG, "Deleted decrypted temp file")
            }
        } else {
            // Legacy unencrypted .bin — load directly
            ptr = WhisperNative.initContext(modelPath)
            if (ptr == 0L) {
                throw IllegalStateException("Failed to load whisper model: $modelPath")
            }
        }

        contextPtr = ptr
        Log.i(TAG, "Model loaded successfully, system info: ${WhisperNative.getSystemInfo()}")
        return true
    }

    /**
     * Main listen loop — feeds audio through VAD, which emits speech chunks
     * for transcription. This replaces the simple energy-based approach with
     * proper voice activity detection including adaptive threshold, hangover,
     * and padding.
     */
    private suspend fun listenLoop() {
        Log.d(TAG, "Listen loop started (VAD-driven)")

        val activeVad = vad ?: run {
            Log.e(TAG, "VAD not initialized")
            return
        }

        // Wire VAD chunk callback to transcription
        activeVad.onSpeechChunkReady = OnSpeechChunkReady { audioData, durationMs ->
            scope.launch {
                engineState.set(WhisperEngineState.PROCESSING)
                transcribeChunk(audioData)
                if (engineState.get() == WhisperEngineState.PROCESSING) {
                    engineState.set(WhisperEngineState.LISTENING)
                }
            }
        }

        activeVad.reset()
        var lastPartialEmitMs = 0L

        while (scope.isActive && engineState.get() == WhisperEngineState.LISTENING) {
            delay(LISTEN_POLL_MS)

            // Drain the audio buffer and feed through VAD
            val samples = audio.drainBuffer()
            if (samples.isNotEmpty()) {
                val nowMs = System.currentTimeMillis()
                activeVad.processAudio(samples, nowMs)

                // Emit "listening" partial indicator when VAD detects speech
                if (activeVad.getState() == VADState.SPEECH && nowMs - lastPartialEmitMs > 500) {
                    lastPartialEmitMs = nowMs
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

        // Flush any remaining speech when stopping
        activeVad.flush()
        Log.d(TAG, "Listen loop ended")
    }

    /**
     * Transcribe an audio chunk using the native whisper engine.
     */
    private suspend fun transcribeChunk(audioData: FloatArray) {
        val ptr = contextPtr
        if (ptr == 0L) {
            Log.w(TAG, "Cannot transcribe — model not loaded")
            return
        }

        try {
            val audioDurationMs = (audioData.size * 1000L) / WhisperAudio.SAMPLE_RATE

            val result = withContext(Dispatchers.Default) {
                WhisperNative.transcribeToText(
                    contextPtr = ptr,
                    numThreads = config.effectiveThreadCount(),
                    audioData = audioData
                )
            }

            if (result.text.isBlank()) {
                Log.d(TAG, "Empty transcription result (${audioDurationMs}ms audio, ${result.processingTimeMs}ms processing)")
                performance.recordEmptyTranscription(audioDurationMs, result.processingTimeMs)
                return
            }

            // Update performance stats
            val realTimeFactor = if (audioDurationMs > 0) {
                result.processingTimeMs.toFloat() / audioDurationMs
            } else 0f

            performance.recordTranscription(
                audioDurationMs = audioDurationMs,
                processingTimeMs = result.processingTimeMs,
                textLength = result.text.length,
                segmentCount = result.segments.size,
                avgConfidence = result.confidence
            )

            // Record language detection
            result.detectedLanguage?.let { lang ->
                performance.recordLanguageDetection(lang)
            }

            Log.i(TAG, "Transcribed: '${result.text}' " +
                    "(${audioDurationMs}ms audio, ${result.processingTimeMs}ms proc, " +
                    "RTF=${"%.2f".format(realTimeFactor)}, conf=${"%.2f".format(result.confidence)})")

            // Build word timestamps from segments with actual confidence
            val wordTimestamps = result.segments.map { seg ->
                com.augmentalis.speechrecognition.WordTimestamp(
                    word = seg.text,
                    startTime = seg.startTimeMs / 1000f,
                    endTime = seg.endTimeMs / 1000f,
                    confidence = seg.confidence
                )
            }

            // Emit recognition result with real confidence from token probabilities
            val recognitionResult = RecognitionResult(
                text = result.text,
                originalText = result.text,
                confidence = result.confidence,
                isPartial = false,
                isFinal = true,
                engine = ENGINE_NAME,
                mode = speechMode.name,
                wordTimestamps = wordTimestamps,
                metadata = mapOf(
                    "processingTimeMs" to result.processingTimeMs,
                    "audioDurationMs" to audioDurationMs,
                    "realTimeFactor" to realTimeFactor,
                    "segmentCount" to result.segments.size,
                    "modelSize" to config.modelSize.name,
                    "detectedLanguage" to (result.detectedLanguage ?: config.language)
                )
            )

            _resultFlow.emit(recognitionResult)

            // Update voice state
            voiceStateManager?.updateCommandExecutionTime()

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Transcription error", e)
            _errorFlow.emit(SpeechError(
                code = SpeechError.ERROR_UNKNOWN,
                message = "Transcription failed: ${e.message}",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.RETRY
            ))
        }
    }
}
