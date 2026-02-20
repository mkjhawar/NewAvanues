/**
 * IosSpeechRecognitionService.kt - iOS speech recognition with Apple Speech + Whisper
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 *
 * Dual-engine implementation supporting:
 * - Apple SFSpeechRecognizer (default): streaming, on-device (iOS 16+), multi-locale
 * - Whisper (fallback/offline): batch via cinterop whisper_bridge, auto-download models
 *
 * Engine selection based on SpeechConfig.engine:
 * - APPLE_SPEECH → SFSpeechRecognizer (this class, inline)
 * - WHISPER → delegates to IosWhisperEngine
 */
package com.augmentalis.speechrecognition

import com.augmentalis.nlu.matching.CommandMatchingService
import com.augmentalis.speechrecognition.whisper.IosWhisperConfig
import com.augmentalis.speechrecognition.whisper.IosWhisperEngine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.AVFAudio.*
import platform.Foundation.*
import platform.Speech.*

/**
 * iOS implementation of SpeechRecognitionService.
 *
 * Supports two engines:
 * - **APPLE_SPEECH**: SFSpeechRecognizer with streaming recognition, on-device mode,
 *   and native platform integration. This is the default and recommended engine.
 * - **WHISPER**: Offline Whisper.cpp via Kotlin/Native cinterop. Provides guaranteed
 *   offline recognition with multi-language support and translation capabilities.
 *
 * Engine switching happens at `initialize()` time based on `SpeechConfig.engine`.
 * Both engines share the same result/error/state flows for transparent integration.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSpeechRecognitionService : SpeechRecognitionService {

    companion object {
        private const val TAG = "IosSpeechService"
        private const val AUDIO_BUFFER_SIZE: UInt = 1024u

        // SFSpeechRecognizerAuthorizationStatus raw values
        // K/N 2.x maps NS_ENUM as a distinct type; cast to Long for comparison
        private const val AUTH_NOT_DETERMINED = 0L
        private const val AUTH_DENIED = 1L
        private const val AUTH_RESTRICTED = 2L
        private const val AUTH_AUTHORIZED = 3L
    }

    // State
    private var _state: ServiceState = ServiceState.UNINITIALIZED
    override val state: ServiceState get() = _state

    // Configuration
    private var config: SpeechConfig = SpeechConfig.default()

    // Command matching
    private val commandMatcher = CommandMatchingService()
    private val resultProcessor = ResultProcessor(commandMatcher = commandMatcher)
    private val commandCache = CommandCache()

    // Flows
    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1)
    override val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    override val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    private val _stateFlow = MutableSharedFlow<ServiceState>(replay = 1)
    override val stateFlow: SharedFlow<ServiceState> = _stateFlow.asSharedFlow()

    // Apple Speech components (used when engine == APPLE_SPEECH)
    private var speechRecognizer: SFSpeechRecognizer? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private var audioEngine: AVAudioEngine? = null

    // Whisper engine (used when engine == WHISPER)
    private var whisperEngine: IosWhisperEngine? = null
    private var whisperResultJob: Job? = null

    // Track which engine is active
    private var activeEngine: SpeechEngine = SpeechEngine.APPLE_SPEECH

    // Coroutine scope for processing results
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Initialize with configuration.
     * Routes to the appropriate engine based on config.engine.
     */
    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return try {
            config.validate().getOrThrow()

            this.config = config
            updateState(ServiceState.INITIALIZING)

            // Configure result processor
            resultProcessor.setMode(config.mode)
            resultProcessor.setConfidenceThreshold(config.confidenceThreshold)
            resultProcessor.setFuzzyMatchingEnabled(config.enableFuzzyMatching)

            activeEngine = config.engine

            when (config.engine) {
                SpeechEngine.WHISPER -> initializeWhisper()
                else -> initializeAppleSpeech()
            }
        } catch (e: Exception) {
            logError(TAG, "Initialization failed", e)
            updateState(ServiceState.ERROR)
            _errorFlow.emit(SpeechError.unknownError(e.message))
            Result.failure(e)
        }
    }

    /**
     * Initialize Apple SFSpeechRecognizer engine.
     */
    private suspend fun initializeAppleSpeech(): Result<Unit> {
        activeEngine = SpeechEngine.APPLE_SPEECH

        // Create SFSpeechRecognizer with locale
        val locale = NSLocale(localeIdentifier = config.language)
        speechRecognizer = SFSpeechRecognizer(locale = locale)

        if (speechRecognizer == null) {
            logError(TAG, "SFSpeechRecognizer unavailable for locale: ${config.language}")
            updateState(ServiceState.ERROR)
            _errorFlow.emit(SpeechError.modelError("Speech recognition unavailable for ${config.language}"))
            return Result.failure(IllegalStateException("SFSpeechRecognizer unavailable"))
        }

        // Request authorization
        requestAuthorization()

        // Create audio engine
        audioEngine = AVAudioEngine()

        logInfo(TAG, "Apple Speech initialized with locale: ${config.language}, " +
                "on-device: ${speechRecognizer?.supportsOnDeviceRecognition}")
        updateState(ServiceState.READY)

        return Result.success(Unit)
    }

    /**
     * Initialize Whisper engine via cinterop.
     */
    private suspend fun initializeWhisper(): Result<Unit> {
        activeEngine = SpeechEngine.WHISPER

        // Map SpeechConfig to IosWhisperConfig
        val whisperConfig = IosWhisperConfig(
            language = config.language.substringBefore("-"), // BCP-47 → ISO 639-1
            numThreads = 0 // auto-detect
        )

        val engine = IosWhisperEngine()
        val success = engine.initialize(whisperConfig)

        if (!success) {
            logError(TAG, "Whisper engine initialization failed")
            updateState(ServiceState.ERROR)
            _errorFlow.emit(SpeechError.modelError("Whisper model initialization failed"))
            return Result.failure(IllegalStateException("Whisper engine init failed"))
        }

        whisperEngine = engine

        // Forward Whisper result and error flows to our shared flows
        whisperResultJob?.cancel()
        whisperResultJob = scope.launch {
            launch {
                engine.resultFlow.collect { result ->
                    // Process through command matching pipeline
                    val processed = resultProcessor.processResult(
                        text = result.text,
                        confidence = result.confidence,
                        engine = config.engine,
                        isPartial = result.isPartial,
                        alternatives = result.alternatives
                    )
                    processed?.let { _resultFlow.emit(it) }
                        ?: _resultFlow.emit(result) // Emit raw if processor returns null
                }
            }
            launch {
                engine.errorFlow.collect { error ->
                    _errorFlow.emit(error)
                }
            }
        }

        logInfo(TAG, "Whisper engine initialized: model=${whisperConfig.modelSize}, " +
                "threads=${whisperConfig.effectiveThreadCount()}")
        updateState(ServiceState.READY)

        return Result.success(Unit)
    }

    /**
     * Request speech recognition authorization.
     * Cast SFSpeechRecognizerAuthorizationStatus to Long for K/N 2.x compatibility
     * (NS_ENUM is a distinct type, not directly comparable to Long constants).
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun requestAuthorization() {
        suspendCancellableCoroutine<Unit> { cont ->
            SFSpeechRecognizer.requestAuthorization { rawStatus ->
                val status = (rawStatus as? Number)?.toLong() ?: -1L
                when (status) {
                    AUTH_AUTHORIZED -> {
                        logInfo(TAG, "Speech recognition authorized")
                        cont.resume(Unit) {}
                    }
                    AUTH_DENIED -> {
                        logError(TAG, "Speech recognition denied by user")
                        scope.launch {
                            _errorFlow.emit(SpeechError.permissionError("Speech recognition permission denied"))
                        }
                        cont.resume(Unit) {}
                    }
                    AUTH_RESTRICTED -> {
                        logError(TAG, "Speech recognition restricted on this device")
                        scope.launch {
                            _errorFlow.emit(SpeechError.permissionError("Speech recognition restricted"))
                        }
                        cont.resume(Unit) {}
                    }
                    AUTH_NOT_DETERMINED -> {
                        logInfo(TAG, "Speech recognition authorization not determined")
                        cont.resume(Unit) {}
                    }
                    else -> cont.resume(Unit) {}
                }
            }
        }
    }

    override suspend fun startListening(): Result<Unit> {
        return when (activeEngine) {
            SpeechEngine.WHISPER -> startWhisperListening()
            else -> startAppleSpeechListening()
        }
    }

    /**
     * Start Apple Speech listening.
     */
    private suspend fun startAppleSpeechListening(): Result<Unit> {
        return try {
            if (!state.canStart()) {
                return Result.failure(IllegalStateException("Cannot start from state: $state"))
            }

            // Cancel any existing task
            cancelRecognitionTask()

            // Configure audio session
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryRecord, error = null)
            audioSession.setActive(true, error = null)

            // Create recognition request
            val request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = true

            // Enable on-device recognition if available (iOS 16+)
            if (speechRecognizer?.supportsOnDeviceRecognition == true) {
                request.requiresOnDeviceRecognition = true
            }
            recognitionRequest = request

            // Install tap on audio engine input node
            val inputNode = audioEngine!!.inputNode
            val recordingFormat = inputNode.outputFormatForBus(0u)

            inputNode.installTapOnBus(
                bus = 0u,
                bufferSize = AUDIO_BUFFER_SIZE,
                format = recordingFormat
            ) { buffer, _ ->
                buffer?.let { recognitionRequest?.appendAudioPCMBuffer(it) }
            }

            // Start audio engine
            audioEngine?.prepare()
            audioEngine?.startAndReturnError(null)

            // Start recognition task
            recognitionTask = speechRecognizer?.recognitionTaskWithRequest(request) { result, error ->
                if (error != null) {
                    val nsError = error as NSError
                    logError(TAG, "Recognition error: ${nsError.localizedDescription}")
                    scope.launch {
                        onError(SpeechError.audioError(nsError.localizedDescription))
                    }
                    stopAudioEngine()
                    return@recognitionTaskWithRequest
                }

                result?.let { speechResult ->
                    val text = speechResult.bestTranscription.formattedString
                    val isFinal = speechResult.isFinal()

                    // Get confidence from last segment (segments is NSArray of SFTranscriptionSegment)
                    val segments = speechResult.bestTranscription.segments
                    val lastSegment = segments.lastOrNull() as? SFTranscriptionSegment
                    val segmentConfidence = lastSegment?.confidence ?: 0.0f

                    // Collect alternatives from other transcriptions
                    val alternatives = speechResult.transcriptions
                        .drop(1)
                        .mapNotNull { (it as? SFTranscription)?.formattedString }

                    scope.launch {
                        onRecognitionResult(
                            text = text,
                            confidence = segmentConfidence,
                            isPartial = !isFinal,
                            alternatives = alternatives
                        )
                    }

                    if (isFinal) {
                        stopAudioEngine()
                    }
                }
            }

            updateState(ServiceState.LISTENING)
            logInfo(TAG, "Apple Speech listening started")

            Result.success(Unit)
        } catch (e: Exception) {
            logError(TAG, "Failed to start Apple Speech listening", e)
            _errorFlow.emit(SpeechError.audioError(e.message))
            Result.failure(e)
        }
    }

    /**
     * Start Whisper listening.
     */
    private suspend fun startWhisperListening(): Result<Unit> {
        return try {
            val engine = whisperEngine
                ?: return Result.failure(IllegalStateException("Whisper engine not initialized"))

            if (!state.canStart()) {
                return Result.failure(IllegalStateException("Cannot start from state: $state"))
            }

            val started = engine.startListening(config.mode)
            if (!started) {
                return Result.failure(IllegalStateException("Whisper engine failed to start listening"))
            }

            updateState(ServiceState.LISTENING)
            logInfo(TAG, "Whisper listening started")
            Result.success(Unit)
        } catch (e: Exception) {
            logError(TAG, "Failed to start Whisper listening", e)
            _errorFlow.emit(SpeechError.audioError(e.message))
            Result.failure(e)
        }
    }

    override suspend fun stopListening(): Result<Unit> {
        return try {
            when (activeEngine) {
                SpeechEngine.WHISPER -> {
                    whisperEngine?.stopListening()
                }
                else -> {
                    stopAudioEngine()
                    cancelRecognitionTask()
                }
            }
            updateState(ServiceState.STOPPED)
            logInfo(TAG, "Stopped listening (engine=$activeEngine)")
            Result.success(Unit)
        } catch (e: Exception) {
            logError(TAG, "Failed to stop listening", e)
            Result.failure(e)
        }
    }

    override suspend fun pause(): Result<Unit> {
        return try {
            if (state == ServiceState.LISTENING) {
                when (activeEngine) {
                    SpeechEngine.WHISPER -> whisperEngine?.pause()
                    else -> audioEngine?.pause()
                }
                updateState(ServiceState.PAUSED)
                logInfo(TAG, "Paused (engine=$activeEngine)")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resume(): Result<Unit> {
        return try {
            if (state == ServiceState.PAUSED) {
                when (activeEngine) {
                    SpeechEngine.WHISPER -> whisperEngine?.resume()
                    else -> audioEngine?.startAndReturnError(null)
                }
                updateState(ServiceState.LISTENING)
                logInfo(TAG, "Resumed (engine=$activeEngine)")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setCommands(staticCommands: List<String>, dynamicCommands: List<String>) {
        commandCache.setStaticCommands(staticCommands)
        commandCache.setDynamicCommands(dynamicCommands)

        commandMatcher.registerCommands(commandCache.getAllCommands())
        resultProcessor.syncCommandsToMatcher()

        logDebug(TAG, "Updated commands: ${staticCommands.size} static, ${dynamicCommands.size} dynamic")
    }

    override fun setMode(mode: SpeechMode) {
        config = config.withMode(mode)
        resultProcessor.setMode(mode)
        whisperEngine?.setMode(mode)
        logInfo(TAG, "Mode set to: $mode")
    }

    override fun setLanguage(language: String) {
        config = config.withLanguage(language)
        logInfo(TAG, "Language set to: $language")

        when (activeEngine) {
            SpeechEngine.WHISPER -> {
                whisperEngine?.setLanguage(language.substringBefore("-"))
            }
            else -> {
                // Recreate speech recognizer with new locale
                val locale = NSLocale(localeIdentifier = language)
                speechRecognizer = SFSpeechRecognizer(locale = locale)
            }
        }
    }

    override fun isListening(): Boolean = state == ServiceState.LISTENING

    override fun isReady(): Boolean = state.isOperational()

    override fun getConfig(): SpeechConfig = config

    override suspend fun release() {
        try {
            updateState(ServiceState.DESTROYING)

            // Release Whisper engine if active
            whisperResultJob?.cancel()
            whisperResultJob = null
            whisperEngine?.destroy()
            whisperEngine = null

            // Release Apple Speech components
            stopAudioEngine()
            cancelRecognitionTask()

            resultProcessor.clear()
            commandCache.clear()
            commandMatcher.clear()

            speechRecognizer = null
            audioEngine = null
            scope.cancel()

            updateState(ServiceState.UNINITIALIZED)
            logInfo(TAG, "Released (engine=$activeEngine)")
        } catch (e: Exception) {
            logError(TAG, "Error during release", e)
        }
    }

    // MARK: - Internal Callbacks (Apple Speech)

    /**
     * Process recognition result from Apple Speech.
     */
    internal suspend fun onRecognitionResult(
        text: String,
        confidence: Float,
        isPartial: Boolean = false,
        alternatives: List<String> = emptyList()
    ) {
        val result = resultProcessor.processResult(
            text = text,
            confidence = confidence,
            engine = config.engine,
            isPartial = isPartial,
            alternatives = alternatives
        )

        result?.let {
            _resultFlow.emit(it)
        }
    }

    /**
     * Handle error from Apple Speech.
     */
    internal suspend fun onError(error: SpeechError) {
        _errorFlow.emit(error)
        if (!error.isRecoverable) {
            updateState(ServiceState.ERROR)
        }
    }

    // MARK: - Private Helpers

    private fun stopAudioEngine() {
        audioEngine?.stop()
        audioEngine?.inputNode?.removeTapOnBus(0u)
        recognitionRequest?.endAudio()
        recognitionRequest = null
    }

    private fun cancelRecognitionTask() {
        recognitionTask?.cancel()
        recognitionTask = null
    }

    private suspend fun updateState(newState: ServiceState) {
        _state = newState
        _stateFlow.emit(newState)
    }
}
