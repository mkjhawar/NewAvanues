/**
 * DesktopWhisperEngine.kt - Desktop Whisper speech recognition engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Desktop equivalent of Android's WhisperEngine. Orchestrates:
 * DesktopWhisperAudio (javax.sound capture) → WhisperVAD (commonMain chunking) →
 * DesktopWhisperNative (JNI transcription)
 *
 * Lifecycle: UNINITIALIZED → LOADING_MODEL → READY → LISTENING → PROCESSING → READY → ...
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.CommandCache
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.SpeechError
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.speechrecognition.logDebug
import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.logWarn
import com.augmentalis.speechrecognition.whisper.vsm.VSMCodec
import com.augmentalis.speechrecognition.whisper.vsm.VSMFormat
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import com.augmentalis.speechrecognition.SpeechMetricsSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

/**
 * Desktop Whisper speech recognition engine.
 *
 * Uses javax.sound.sampled for audio capture and whisper.cpp JNI for transcription.
 * Shares the same VAD algorithm (commonMain) as the Android engine.
 */
class DesktopWhisperEngine {

    companion object {
        private const val TAG = "DesktopWhisperEngine"
        private const val ENGINE_NAME = "Whisper"
        private const val ENGINE_VERSION = "1.0.0"
        private const val MAX_INIT_RETRIES = 2
        private const val INIT_RETRY_DELAY_MS = 1000L
        private const val LISTEN_POLL_MS = 100L
    }

    // State
    private val engineState = AtomicReference(WhisperEngineState.UNINITIALIZED)
    val state: WhisperEngineState get() = engineState.get()

    // Configuration
    private var config = DesktopWhisperConfig()
    private var speechMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND

    // Native context pointer
    @Volatile
    private var contextPtr: Long = 0L

    // Components
    private val audio = DesktopWhisperAudio()
    private var vad: WhisperVAD? = null
    private val commandCache = CommandCache()
    private val modelManager = DesktopWhisperModelManager()

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

    // Metrics snapshot — updated after each transcription
    private val _metricsSnapshot = MutableStateFlow<SpeechMetricsSnapshot?>(null)
    val metricsSnapshot: StateFlow<SpeechMetricsSnapshot?> = _metricsSnapshot.asStateFlow()

    val totalTranscriptions: Int get() = performance.totalTranscriptions
    val averageLatencyMs: Long get() = performance.getAverageLatencyMs()

    /**
     * Initialize the engine: load native library and model.
     */
    suspend fun initialize(whisperConfig: DesktopWhisperConfig = DesktopWhisperConfig.autoTuned()): Boolean {
        if (engineState.get() == WhisperEngineState.READY) {
            logWarn(TAG, "Already initialized")
            return true
        }

        whisperConfig.validate().getOrElse { e ->
            logError(TAG, "Invalid config: ${e.message}")
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

            engineState.set(WhisperEngineState.ERROR)
            logError(TAG, "Initialization failed after $MAX_INIT_RETRIES attempts", lastError)
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
     */
    fun startListening(mode: SpeechMode = speechMode): Boolean {
        val currentState = engineState.get()
        if (currentState != WhisperEngineState.READY && currentState != WhisperEngineState.PAUSED) {
            logWarn(TAG, "Cannot start listening from state: $currentState")
            return false
        }

        speechMode = mode

        if (!audio.isRecording()) {
            if (!audio.start()) {
                logError(TAG, "Failed to start audio capture")
                return false
            }
        }

        engineState.set(WhisperEngineState.LISTENING)

        listenJob?.cancel()
        listenJob = scope.launch {
            listenLoop()
        }

        logInfo(TAG, "Listening started, mode=$mode")
        return true
    }

    /**
     * Stop listening.
     */
    fun stopListening() {
        listenJob?.cancel()
        listenJob = null

        // Transcribe remaining buffer
        val remainingAudio = audio.drainBuffer()
        if (remainingAudio.size > config.minSpeechDurationMs * DesktopWhisperAudio.SAMPLE_RATE / 1000) {
            scope.launch {
                transcribeChunk(remainingAudio)
            }
        }

        audio.stop()

        if (engineState.get() == WhisperEngineState.LISTENING ||
            engineState.get() == WhisperEngineState.PROCESSING) {
            engineState.set(WhisperEngineState.READY)
        }

        logInfo(TAG, "Listening stopped")
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
            logInfo(TAG, "Paused")
        }
    }

    /**
     * Resume from paused state.
     */
    fun resume(): Boolean {
        if (engineState.get() != WhisperEngineState.PAUSED) return false
        return startListening(speechMode)
    }

    fun setCommands(staticCommands: List<String>, dynamicCommands: List<String> = emptyList()) {
        commandCache.setStaticCommands(staticCommands)
        commandCache.setDynamicCommands(dynamicCommands)
        logDebug(TAG, "Commands updated: ${staticCommands.size} static, ${dynamicCommands.size} dynamic")
    }

    fun setMode(mode: SpeechMode) {
        speechMode = mode
        logDebug(TAG, "Mode set to: $mode")
    }

    fun setLanguage(language: String) {
        if (config.language == language) return
        config = config.copy(language = language)
        logInfo(TAG, "Language set to: $language")
    }

    fun isListening(): Boolean = engineState.get() == WhisperEngineState.LISTENING
    fun isReady(): Boolean = engineState.get() == WhisperEngineState.READY
    fun getEngineName(): String = ENGINE_NAME
    fun getEngineVersion(): String = ENGINE_VERSION
    fun requiresNetwork(): Boolean = false
    fun getMemoryUsageMB(): Int = config.modelSize.approxSizeMB

    /** Observe model download state for UI integration. */
    val modelDownloadState: StateFlow<ModelDownloadState> get() = modelManager.downloadState

    /** Access the model manager for download/delete operations. */
    fun getModelManager(): DesktopWhisperModelManager = modelManager

    /**
     * Destroy the engine and release all resources.
     */
    fun destroy() {
        logDebug(TAG, "Destroying DesktopWhisperEngine")

        listenJob?.cancel()
        listenJob = null

        audio.release()

        if (contextPtr != 0L) {
            DesktopWhisperNative.freeContext(contextPtr)
            contextPtr = 0L
        }

        commandCache.clear()
        engineState.set(WhisperEngineState.DESTROYED)
        scope.cancel()

        logDebug(TAG, "DesktopWhisperEngine destroyed")
    }

    // --- Private implementation ---

    private suspend fun performInitialization(): Boolean {
        // Step 1: Load native library
        if (!DesktopWhisperNative.ensureLoaded()) {
            throw IllegalStateException("Failed to load whisper-jni native library on desktop")
        }

        // Step 2: Initialize audio
        if (!audio.initialize()) {
            throw IllegalStateException("Failed to initialize desktop audio capture")
        }

        audio.onError = { error ->
            scope.launch {
                _errorFlow.emit(SpeechError.audioError(error))
            }
        }

        // Step 3: Initialize VAD (from commonMain) — use effective params (profile-aware)
        vad = WhisperVAD(
            speechThreshold = 0f, // auto-calibrate
            vadSensitivity = config.effectiveVadSensitivity,
            silenceTimeoutMs = config.effectiveSilenceThresholdMs,
            minSpeechDurationMs = config.effectiveMinSpeechDurationMs,
            maxSpeechDurationMs = config.maxChunkDurationMs,
            hangoverFrames = config.effectiveHangoverFrames,
            paddingMs = 150,
            sampleRate = DesktopWhisperAudio.SAMPLE_RATE,
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

        // Load model — decrypt .vsm to temp file if needed
        logInfo(TAG, "Loading model: $modelPath")
        val ptr: Long

        if (modelPath.endsWith(VSMFormat.VSM_EXTENSION)) {
            // Encrypted VSM: decrypt to temp file, load, delete temp
            val tempDir = File(System.getProperty("java.io.tmpdir", "/tmp"), "vlm_tmp")
            tempDir.mkdirs()

            val codec = VSMCodec()
            val decryptedFile = codec.decryptToTempFile(modelPath, tempDir)
                ?: throw IllegalStateException("VSM decryption failed: $modelPath")

            try {
                ptr = DesktopWhisperNative.initContext(decryptedFile.absolutePath)
                if (ptr == 0L) {
                    throw IllegalStateException("Failed to load whisper model from decrypted VSM")
                }
            } finally {
                decryptedFile.delete()
                logDebug(TAG, "Deleted decrypted temp file")
            }
        } else {
            // Legacy unencrypted .bin
            ptr = DesktopWhisperNative.initContext(modelPath)
            if (ptr == 0L) {
                throw IllegalStateException("Failed to load whisper model: $modelPath")
            }
        }

        contextPtr = ptr
        logInfo(TAG, "Model loaded, system info: ${DesktopWhisperNative.getSystemInfo()}")
        return true
    }

    /**
     * Main listen loop — feeds audio through VAD, which emits speech chunks.
     */
    private suspend fun listenLoop() {
        logDebug(TAG, "Listen loop started (VAD-driven)")

        val activeVad = vad ?: run {
            logError(TAG, "VAD not initialized")
            return
        }

        activeVad.onSpeechChunkReady = OnSpeechChunkReady { audioData, _ ->
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

            val samples = audio.drainBuffer()
            if (samples.isNotEmpty()) {
                val nowMs = System.currentTimeMillis()
                activeVad.processAudio(samples, nowMs)

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

        activeVad.flush()
        logDebug(TAG, "Listen loop ended")
    }

    /**
     * Transcribe an audio chunk using the native whisper engine.
     */
    private suspend fun transcribeChunk(audioData: FloatArray) {
        val ptr = contextPtr
        if (ptr == 0L) {
            logWarn(TAG, "Cannot transcribe — model not loaded")
            return
        }

        try {
            val audioDurationMs = (audioData.size * 1000L) / DesktopWhisperAudio.SAMPLE_RATE

            val result = withContext(Dispatchers.Default) {
                DesktopWhisperNative.transcribeToText(
                    contextPtr = ptr,
                    numThreads = config.effectiveThreadCount(),
                    audioData = audioData
                )
            }

            if (result.text.isBlank()) {
                logDebug(TAG, "Empty transcription (${audioDurationMs}ms audio, ${result.processingTimeMs}ms proc)")
                performance.recordEmptyTranscription(audioDurationMs, result.processingTimeMs)
                _metricsSnapshot.value = performance.toSnapshot(
                    ENGINE_NAME, engineState.get().name, System.currentTimeMillis()
                )
                return
            }

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

            result.detectedLanguage?.let { lang ->
                performance.recordLanguageDetection(lang)
            }

            // Emit updated metrics snapshot
            _metricsSnapshot.value = performance.toSnapshot(
                ENGINE_NAME, engineState.get().name, System.currentTimeMillis()
            )

            logInfo(TAG, "Transcribed: '${result.text}' " +
                    "(${audioDurationMs}ms audio, ${result.processingTimeMs}ms proc, " +
                    "RTF=${"%.2f".format(realTimeFactor)}, conf=${"%.2f".format(result.confidence)})")

            val wordTimestamps = result.segments.map { seg ->
                com.augmentalis.speechrecognition.WordTimestamp(
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
                    "detectedLanguage" to (result.detectedLanguage ?: config.language),
                    "modelSize" to config.modelSize.name,
                    "platform" to "desktop",
                    "confidence_level" to confidenceLevel,
                    "scoring_method" to "WHISPER"
                )
            )

            _resultFlow.emit(recognitionResult)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError(TAG, "Transcription error", e)
            _errorFlow.emit(SpeechError(
                code = SpeechError.ERROR_UNKNOWN,
                message = "Transcription failed: ${e.message}",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.RETRY
            ))
        }
    }
}
