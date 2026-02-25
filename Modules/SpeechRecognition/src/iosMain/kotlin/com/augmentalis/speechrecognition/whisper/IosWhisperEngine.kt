/**
 * IosWhisperEngine.kt - iOS Whisper speech recognition engine orchestrator
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * Orchestrates the iOS Whisper speech recognition pipeline:
 * IosWhisperAudio (capture) → WhisperVAD (chunking) → IosWhisperNative (transcription)
 *
 * Lifecycle: UNINITIALIZED → LOADING_MODEL → READY → LISTENING → PROCESSING → READY → ...
 * Supports: offline recognition, multi-language, auto language detection, translation.
 *
 * Mirrors the Android WhisperEngine pattern but uses:
 * - IosWhisperNative (cinterop) instead of WhisperNative (JNI)
 * - IosWhisperAudio (AVAudioEngine) instead of WhisperAudio (AudioRecord)
 * - IosWhisperModelManager (NSURLSession) instead of WhisperModelManager (OkHttp)
 * - atomicfu SynchronizedObject instead of AtomicReference (no JVM atomics on K/N)
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.SpeechError
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.speechrecognition.WordTimestamp
import com.augmentalis.speechrecognition.logDebug
import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.logWarn
import com.augmentalis.speechrecognition.whisper.vsm.IosVSMCodec
import com.augmentalis.speechrecognition.whisper.vsm.VSMFormat
import kotlin.concurrent.Volatile
import kotlin.math.roundToInt
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.timeIntervalSince1970

/**
 * Main iOS Whisper speech recognition engine.
 *
 * Integrates AVAudioEngine capture, platform-agnostic VAD chunking,
 * and native whisper.cpp transcription via cinterop into a unified engine.
 *
 * Usage:
 * ```kotlin
 * val engine = IosWhisperEngine()
 * val success = engine.initialize()
 * if (success) {
 *     engine.startListening()
 *     engine.resultFlow.collect { result -> handleResult(result) }
 * }
 * ```
 */
class IosWhisperEngine {

    companion object {
        private const val TAG = "IosWhisperEngine"
        private const val ENGINE_NAME = "Whisper"
        private const val ENGINE_VERSION = "1.0.0"
        private const val MAX_INIT_RETRIES = 2
        private const val INIT_RETRY_DELAY_MS = 1000L

        /** Polling interval for checking audio buffer during listening */
        private const val LISTEN_POLL_MS = 100L
    }

    // State — guarded by stateLock since K/N has no AtomicReference
    private val stateLock = SynchronizedObject()
    @kotlin.concurrent.Volatile
    private var _engineState: WhisperEngineState = WhisperEngineState.UNINITIALIZED
    val state: WhisperEngineState get() = _engineState

    private fun setState(newState: WhisperEngineState) {
        synchronized(stateLock) { _engineState = newState }
    }

    // Configuration
    private var config = IosWhisperConfig()
    private var speechMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND

    // Native context pointer (0 = not loaded)
    @kotlin.concurrent.Volatile
    private var contextPtr: Long = 0L

    // Components
    private val audio = IosWhisperAudio()
    private var vad: WhisperVAD? = null
    private val modelManager = IosWhisperModelManager()

    // Coroutine management
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listenJob: Job? = null

    // Result flows
    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1)
    val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    // Performance tracking (shared commonMain implementation)
    val performance = WhisperPerformance()

    /**
     * Initialize the engine: verify cinterop bindings and load model.
     *
     * @param whisperConfig iOS-specific Whisper configuration
     * @return true if initialization succeeded
     */
    suspend fun initialize(whisperConfig: IosWhisperConfig = IosWhisperConfig.autoTuned()): Boolean {
        if (_engineState == WhisperEngineState.READY) {
            logWarn(TAG, "Already initialized")
            return true
        }

        whisperConfig.validate().getOrElse { e ->
            logError(TAG, "Invalid config: ${e.message}")
            return false
        }

        config = whisperConfig
        setState(WhisperEngineState.LOADING_MODEL)

        val initStartMs = currentTimeMs()

        return withContext(Dispatchers.Default) {
            var lastError: Exception? = null
            var attempts = 0

            for (attempt in 1..MAX_INIT_RETRIES) {
                attempts = attempt
                try {
                    val success = performInitialization()
                    if (success) {
                        setState(WhisperEngineState.READY)
                        val initTime = currentTimeMs() - initStartMs
                        performance.recordInitialization(initTime, config.modelSize, attempts)
                        logInfo(TAG, "Engine initialized in ${initTime}ms: model=${config.modelSize}, " +
                                "threads=${config.effectiveThreadCount()}, lang=${config.language}")
                        return@withContext true
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    logWarn(TAG, "Init attempt $attempt failed: ${e.message}")
                    if (attempt < MAX_INIT_RETRIES) {
                        delay(INIT_RETRY_DELAY_MS * attempt)
                    }
                }
            }

            setState(WhisperEngineState.ERROR)
            logError(TAG, "Initialization failed after $MAX_INIT_RETRIES attempts")
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
        val currentState = _engineState
        if (currentState != WhisperEngineState.READY && currentState != WhisperEngineState.PAUSED) {
            logWarn(TAG, "Cannot start listening from state: $currentState")
            return false
        }

        speechMode = mode

        if (!audio.isCapturing()) {
            if (!audio.start()) {
                logError(TAG, "Failed to start audio capture")
                return false
            }
        }

        setState(WhisperEngineState.LISTENING)

        // Launch the listen loop that monitors audio and triggers transcription
        listenJob?.cancel()
        listenJob = scope.launch {
            listenLoop()
        }

        logInfo(TAG, "Listening started, mode=$mode")
        return true
    }

    /**
     * Stop listening. Audio capture stops and any pending transcription completes.
     */
    fun stopListening() {
        listenJob?.cancel()
        listenJob = null

        // Flush VAD to transcribe whatever remains
        vad?.flush()

        audio.stop()

        if (_engineState == WhisperEngineState.LISTENING ||
            _engineState == WhisperEngineState.PROCESSING) {
            setState(WhisperEngineState.READY)
        }

        logInfo(TAG, "Listening stopped")
    }

    /**
     * Pause listening — keeps model loaded but stops audio capture.
     */
    fun pause() {
        if (_engineState == WhisperEngineState.LISTENING) {
            listenJob?.cancel()
            listenJob = null
            audio.stop()
            setState(WhisperEngineState.PAUSED)
            logInfo(TAG, "Paused")
        }
    }

    /**
     * Resume from paused state.
     */
    fun resume(): Boolean {
        if (_engineState != WhisperEngineState.PAUSED) return false
        return startListening(speechMode)
    }

    /**
     * Set the speech recognition mode.
     */
    fun setMode(mode: SpeechMode) {
        speechMode = mode
        logDebug(TAG, "Mode set to: $mode")
    }

    /**
     * Set language. May require model reload if switching to/from English-only model.
     */
    fun setLanguage(language: String) {
        if (config.language == language) return
        config = config.copy(language = language)
        logInfo(TAG, "Language set to: $language")
    }

    /**
     * Check if currently listening.
     */
    fun isListening(): Boolean = _engineState == WhisperEngineState.LISTENING

    /**
     * Check if engine is ready (model loaded, can start listening).
     */
    fun isReady(): Boolean = _engineState == WhisperEngineState.READY

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
    fun getModelManager(): IosWhisperModelManager = modelManager

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
        logDebug(TAG, "Destroying IosWhisperEngine")

        listenJob?.cancel()
        listenJob = null

        audio.release()

        if (contextPtr != 0L) {
            IosWhisperNative.freeContext(contextPtr)
            contextPtr = 0L
        }

        setState(WhisperEngineState.DESTROYED)
        scope.cancel()

        logDebug(TAG, "IosWhisperEngine destroyed")
    }

    // --- Private implementation ---

    /**
     * Perform the actual initialization: verify cinterop + load model.
     */
    @OptIn(ExperimentalForeignApi::class)
    private suspend fun performInitialization(): Boolean {
        // Step 1: Verify native library is linked
        if (!IosWhisperNative.ensureAvailable()) {
            throw IllegalStateException("whisper_bridge cinterop bindings not available")
        }

        // Step 2: Initialize audio capture
        if (!audio.initialize()) {
            throw IllegalStateException("Failed to initialize AVAudioEngine capture")
        }

        // Step 3: Initialize VAD (shared commonMain implementation) — use effective params (profile-aware)
        vad = WhisperVAD(
            speechThreshold = 0f, // auto-calibrate from noise floor
            vadSensitivity = config.effectiveVadSensitivity,
            silenceTimeoutMs = config.effectiveSilenceThresholdMs.toLong(),
            minSpeechDurationMs = config.effectiveMinSpeechDurationMs.toLong(),
            maxSpeechDurationMs = config.maxChunkDurationMs.toLong(),
            hangoverFrames = config.effectiveHangoverFrames,
            paddingMs = 150,
            thresholdAlpha = config.effectiveThresholdAlpha,
            minThreshold = config.effectiveMinThreshold
        )

        // Step 4: Load model (auto-download if missing)
        var modelPath = config.resolveModelPath()

        if (modelPath == null) {
            logInfo(TAG, "Model ${config.modelSize.displayName} not found, auto-downloading...")
            val downloaded = modelManager.downloadModel(config.modelSize)
            if (!downloaded) {
                throw IllegalStateException(
                    "Failed to download Whisper model: ${config.modelSize.displayName}. " +
                    "Check network connection and storage space."
                )
            }
            modelPath = config.resolveModelPath()
                ?: throw IllegalStateException(
                    "Model file not found after download: ${config.modelSize.ggmlFileName}"
                )
        }

        logInfo(TAG, "Loading model: $modelPath")

        val ptr: Long
        if (modelPath.endsWith(VSMFormat.VSM_EXTENSION)) {
            // Encrypted .vsm — decrypt to temp file, load, then delete
            val tempDir = "${NSTemporaryDirectory()}vlm_tmp"
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(tempDir)) {
                fileManager.createDirectoryAtPath(
                    tempDir,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }

            val codec = IosVSMCodec()
            val decryptedPath = codec.decryptToTempFile(modelPath, tempDir)
                ?: throw IllegalStateException("VSM decryption failed: $modelPath")

            try {
                ptr = IosWhisperNative.initContext(decryptedPath)
            } finally {
                // Always clean up the decrypted temp file
                fileManager.removeItemAtPath(decryptedPath, error = null)
            }
        } else {
            // Legacy unencrypted .bin — load directly
            ptr = IosWhisperNative.initContext(modelPath)
        }

        if (ptr == 0L) {
            throw IllegalStateException("Failed to load whisper model: $modelPath")
        }

        contextPtr = ptr
        logInfo(TAG, "Model loaded successfully, system info: ${IosWhisperNative.getSystemInfo()}")
        return true
    }

    /**
     * Main listen loop — feeds audio through VAD, which emits speech chunks
     * for transcription. Uses the shared commonMain WhisperVAD for voice
     * activity detection with adaptive threshold, hangover, and padding.
     */
    private suspend fun listenLoop() {
        logDebug(TAG, "Listen loop started (VAD-driven)")

        val activeVad = vad ?: run {
            logError(TAG, "VAD not initialized")
            return
        }

        // Wire VAD chunk callback to transcription
        activeVad.onSpeechChunkReady = OnSpeechChunkReady { audioData, _ ->
            scope.launch {
                setState(WhisperEngineState.PROCESSING)
                transcribeChunk(audioData)
                if (_engineState == WhisperEngineState.PROCESSING) {
                    setState(WhisperEngineState.LISTENING)
                }
            }
        }

        activeVad.reset()
        var lastPartialEmitMs = 0L

        while (scope.isActive && _engineState == WhisperEngineState.LISTENING) {
            delay(LISTEN_POLL_MS)

            // Drain the audio buffer and feed through VAD
            val samples = audio.drainBuffer()
            if (samples.isNotEmpty()) {
                val nowMs = currentTimeMs()
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
        logDebug(TAG, "Listen loop ended")
    }

    /**
     * Transcribe an audio chunk using the native whisper engine via cinterop.
     */
    private suspend fun transcribeChunk(audioData: FloatArray) {
        val ptr = contextPtr
        if (ptr == 0L) {
            logWarn(TAG, "Cannot transcribe — model not loaded")
            return
        }

        try {
            val audioDurationMs = (audioData.size * 1000L) / IosWhisperAudio.SAMPLE_RATE

            val result = withContext(Dispatchers.Default) {
                IosWhisperNative.transcribeToText(
                    contextPtr = ptr,
                    numThreads = config.effectiveThreadCount(),
                    audioData = audioData,
                    language = config.language,
                    translate = config.translateToEnglish
                )
            }

            if (result.text.isBlank()) {
                logDebug(TAG, "Empty transcription result (${audioDurationMs}ms audio, " +
                        "${result.processingTimeMs}ms processing)")
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

            val rtfStr = ((realTimeFactor * 100).roundToInt() / 100.0).toString()
            val confStr = ((result.confidence * 100).roundToInt() / 100.0).toString()
            logInfo(TAG, "Transcribed ${result.text.length} chars " +
                    "(${audioDurationMs}ms audio, ${result.processingTimeMs}ms proc, " +
                    "RTF=$rtfStr, conf=$confStr)")

            // Build word timestamps from segments with actual confidence
            val wordTimestamps = result.segments.map { seg ->
                WordTimestamp(
                    word = seg.text,
                    startTime = seg.startTimeMs / 1000f,
                    endTime = seg.endTimeMs / 1000f,
                    confidence = seg.confidence
                )
            }

            // Classify confidence using same thresholds as ConfidenceScorer
            // (HIGH >0.85, MEDIUM 0.70-0.85, LOW 0.50-0.70, REJECT <0.50)
            val confidenceLevel = when {
                result.confidence >= 0.85f -> "HIGH"
                result.confidence >= 0.70f -> "MEDIUM"
                result.confidence >= 0.50f -> "LOW"
                else -> "REJECT"
            }

            val recognitionResult = RecognitionResult(
                text = result.text,
                originalText = result.text,
                confidence = result.confidence.coerceIn(0f, 1f),
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
                    "detectedLanguage" to (result.detectedLanguage ?: config.language),
                    "confidence_level" to confidenceLevel,
                    "scoring_method" to "WHISPER"
                )
            )

            _resultFlow.emit(recognitionResult)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError(TAG, "Transcription error: ${e.message}")
            _errorFlow.emit(SpeechError(
                code = SpeechError.ERROR_UNKNOWN,
                message = "Transcription failed: ${e.message}",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.RETRY
            ))
        }
    }

    /**
     * Current time in milliseconds using NSDate (no System.currentTimeMillis on K/N).
     */
    private fun currentTimeMs(): Long {
        return (NSDate().timeIntervalSince1970 * 1000).toLong()
    }
}
