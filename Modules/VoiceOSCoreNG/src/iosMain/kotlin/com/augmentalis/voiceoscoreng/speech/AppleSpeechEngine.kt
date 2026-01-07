/**
 * AppleSpeechEngine.kt - iOS Apple Speech Framework adapter
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-07
 *
 * Implements ISpeechEngine using Apple's Speech.framework.
 * Uses Kotlin/Native to bridge to Swift/Objective-C APIs.
 *
 * References:
 * - Apple Speech Framework: https://developer.apple.com/documentation/speech
 * - SFSpeechRecognizer: https://developer.apple.com/documentation/speech/sfspeechrecognizer
 */
package com.augmentalis.voiceoscoreng.speech

import com.augmentalis.voiceoscoreng.features.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.AVFAudio.*
import platform.Foundation.*
import platform.Speech.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Apple Speech Framework engine for iOS.
 *
 * Provides continuous speech recognition using iOS's built-in
 * speech recognition capabilities.
 *
 * ## Features
 * - On-device recognition (iOS 13+)
 * - Server-based recognition
 * - Continuous streaming
 * - Multiple language support
 *
 * ## Requirements
 * - iOS 10.0+
 * - NSSpeechRecognitionUsageDescription in Info.plist
 * - NSMicrophoneUsageDescription in Info.plist
 */
class AppleSpeechEngine : ISpeechEngine {

    // ═══════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _results = MutableSharedFlow<SpeechResult>(replay = 0)
    override val results: Flow<SpeechResult> = _results.asSharedFlow()

    private val _errors = MutableSharedFlow<SpeechError>(replay = 0)
    override val errors: Flow<SpeechError> = _errors.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Speech components
    private var speechRecognizer: SFSpeechRecognizer? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private var audioEngine: AVAudioEngine? = null

    private var config: SpeechConfig = SpeechConfig.DEFAULT
    private var isInitialized = false
    private var isListening = false

    // ═══════════════════════════════════════════════════════════════════════
    // ISpeechEngine Implementation
    // ═══════════════════════════════════════════════════════════════════════

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        this.config = config

        return try {
            // Create speech recognizer for configured locale
            val locale = NSLocale(localeIdentifier = config.languageCode)
            speechRecognizer = SFSpeechRecognizer(locale = locale)

            if (speechRecognizer == null || !speechRecognizer!!.isAvailable) {
                _state.value = EngineState.Error(
                    "Speech recognition not available",
                    recoverable = false
                )
                return Result.failure(IllegalStateException("Speech recognition not available"))
            }

            // Request authorization
            val authResult = requestAuthorization()
            if (authResult.isFailure) {
                return authResult
            }

            // Setup audio engine
            audioEngine = AVAudioEngine()

            isInitialized = true
            _state.value = EngineState.Ready(SpeechEngine.APPLE_SPEECH)

            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = EngineState.Error(e.message ?: "Init failed", recoverable = true)
            Result.failure(e)
        }
    }

    private suspend fun requestAuthorization(): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            SFSpeechRecognizer.requestAuthorization { status ->
                when (status) {
                    SFSpeechRecognizerAuthorizationStatusAuthorized -> {
                        continuation.resume(Result.success(Unit)) {}
                    }
                    SFSpeechRecognizerAuthorizationStatusDenied,
                    SFSpeechRecognizerAuthorizationStatusRestricted -> {
                        val error = SpeechError(
                            code = SpeechError.ErrorCode.PERMISSION_DENIED,
                            message = "Speech recognition permission denied",
                            recoverable = false
                        )
                        scope.launch { _errors.emit(error) }
                        continuation.resume(Result.failure(
                            SecurityException("Speech recognition permission denied")
                        )) {}
                    }
                    else -> {
                        continuation.resume(Result.failure(
                            IllegalStateException("Authorization not determined")
                        )) {}
                    }
                }
            }
        }
    }

    override suspend fun startListening(): Result<Unit> {
        if (!isInitialized) {
            return Result.failure(IllegalStateException("Engine not initialized"))
        }

        if (isListening) {
            return Result.success(Unit)
        }

        return try {
            // Cancel any existing task
            recognitionTask?.cancel()
            recognitionTask = null

            // Configure audio session
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategoryWithOptions(
                AVAudioSessionCategoryRecord,
                AVAudioSessionCategoryOptionDefaultToSpeaker,
                null
            )
            audioSession.setMode(AVAudioSessionModeMeasurement, null)
            audioSession.setActive(true, null)

            // Create recognition request
            recognitionRequest = SFSpeechAudioBufferRecognitionRequest().apply {
                shouldReportPartialResults = true

                // Enable on-device recognition if available (iOS 13+)
                if (speechRecognizer?.supportsOnDeviceRecognition == true) {
                    requiresOnDeviceRecognition = config.preferOffline
                }
            }

            val request = recognitionRequest
                ?: return Result.failure(IllegalStateException("Failed to create recognition request"))

            // Get audio input node
            val inputNode = audioEngine?.inputNode
                ?: return Result.failure(IllegalStateException("Audio engine has no input node"))

            // Start recognition task
            recognitionTask = speechRecognizer?.recognitionTaskWithRequest(request) { result, error ->
                handleRecognitionResult(result, error)
            }

            // Install tap on audio input
            val recordingFormat = inputNode.outputFormatForBus(0u)
            inputNode.installTapOnBus(
                0u,
                bufferSize = 1024u,
                format = recordingFormat
            ) { buffer, _ ->
                request.appendAudioPCMBuffer(buffer!!)
            }

            // Start audio engine
            audioEngine?.prepare()
            audioEngine?.startAndReturnError(null)

            isListening = true
            _state.value = EngineState.Listening

            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = EngineState.Error(e.message ?: "Start failed", recoverable = true)
            Result.failure(e)
        }
    }

    private fun handleRecognitionResult(result: SFSpeechRecognitionResult?, error: NSError?) {
        if (error != null) {
            scope.launch {
                _errors.emit(SpeechError(
                    code = SpeechError.ErrorCode.RECOGNITION_FAILED,
                    message = error.localizedDescription,
                    recoverable = true
                ))
            }
            return
        }

        result?.let { speechResult ->
            val bestTranscription = speechResult.bestTranscription
            val text = bestTranscription.formattedString
            val isFinal = speechResult.isFinal

            // Calculate confidence from segments
            val confidence = bestTranscription.segments
                .mapNotNull { (it as? SFTranscriptionSegment)?.confidence?.toFloat() }
                .takeIf { it.isNotEmpty() }
                ?.average()?.toFloat()
                ?: 0.8f

            scope.launch {
                _results.emit(SpeechResult(
                    text = text,
                    confidence = confidence,
                    isFinal = isFinal,
                    timestamp = currentTimeMillis()
                ))

                if (isFinal) {
                    _state.value = EngineState.Ready(SpeechEngine.APPLE_SPEECH)
                }
            }
        }
    }

    override suspend fun stopListening() {
        audioEngine?.stop()
        audioEngine?.inputNode?.removeTapOnBus(0u)

        recognitionRequest?.endAudio()
        recognitionTask?.cancel()

        recognitionRequest = null
        recognitionTask = null

        isListening = false

        if (isInitialized) {
            _state.value = EngineState.Ready(SpeechEngine.APPLE_SPEECH)
        }
    }

    override suspend fun updateCommands(commands: List<String>): Result<Unit> {
        // Apple Speech doesn't support custom grammar/vocabulary directly
        // Commands will be handled by CommandWordDetector wrapper
        return Result.success(Unit)
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        this.config = config

        // Update on-device preference if supported
        recognitionRequest?.let { request ->
            if (speechRecognizer?.supportsOnDeviceRecognition == true) {
                request.requiresOnDeviceRecognition = config.preferOffline
            }
        }

        return Result.success(Unit)
    }

    override fun isRecognizing(): Boolean = isListening

    override fun isInitialized(): Boolean = isInitialized

    override fun getEngineType(): SpeechEngine = SpeechEngine.APPLE_SPEECH

    override fun getSupportedFeatures(): Set<EngineFeature> = setOf(
        EngineFeature.CONTINUOUS_RECOGNITION,
        EngineFeature.OFFLINE_MODE, // iOS 13+ on-device
        EngineFeature.PUNCTUATION,
        EngineFeature.WORD_TIMESTAMPS
    )

    override suspend fun destroy() {
        stopListening()

        audioEngine = null
        speechRecognizer = null

        isInitialized = false
        _state.value = EngineState.Destroyed

        scope.cancel()
    }
}

/**
 * iOS-specific time implementation.
 */
actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
