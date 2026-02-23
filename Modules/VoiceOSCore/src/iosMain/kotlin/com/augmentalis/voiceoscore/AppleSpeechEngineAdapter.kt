/**
 * AppleSpeechEngineAdapter.kt - Bridge from ISpeechEngine to IosSpeechRecognitionService
 *
 * Adapts the SpeechRecognition module's IosSpeechRecognitionService to the
 * VoiceOSCore ISpeechEngine interface. This allows VoiceOSCore to use Apple
 * SFSpeechRecognizer through the same engine abstraction as Android.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore

import com.augmentalis.speechrecognition.IosSpeechRecognitionService
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.ServiceState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Adapts IosSpeechRecognitionService to the ISpeechEngine interface.
 *
 * Maps between two type systems:
 * - SpeechRecognition module: ServiceState enum, RecognitionResult, SpeechError (code: Int), SpeechConfig
 * - VoiceOSCore module: EngineState sealed class, SpeechResult, SpeechError (code: ErrorCode), SpeechConfig
 */
internal class AppleSpeechEngineAdapter : ISpeechEngine {

    private val service = IosSpeechRecognitionService()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    override val results: Flow<SpeechResult> = service.resultFlow.map { result ->
        mapRecognitionResult(result)
    }

    override val errors: Flow<SpeechError> = service.errorFlow.map { error ->
        mapSpeechError(error)
    }

    private var initialized = false
    private var listening = false

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        _state.value = EngineState.Initializing

        val speechConfig = mapToSpeechRecognitionConfig(config)
        val result = service.initialize(speechConfig)

        if (result.isSuccess) {
            initialized = true
            _state.value = EngineState.Ready(SpeechEngine.APPLE_SPEECH)

            // Forward state changes from the service
            scope.launch {
                service.stateFlow.collect { serviceState ->
                    _state.value = mapServiceState(serviceState)
                }
            }
        } else {
            _state.value = EngineState.Error(
                result.exceptionOrNull()?.message ?: "Initialization failed",
                recoverable = true
            )
        }

        return result
    }

    override suspend fun startListening(): Result<Unit> {
        val result = service.startListening()
        if (result.isSuccess) {
            listening = true
            _state.value = EngineState.Listening
        }
        return result
    }

    override suspend fun stopListening() {
        service.stopListening()
        listening = false
        if (initialized) {
            _state.value = EngineState.Ready(SpeechEngine.APPLE_SPEECH)
        }
    }

    override suspend fun updateCommands(commands: List<String>): Result<Unit> {
        return try {
            service.setCommands(staticCommands = commands)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        return try {
            service.setLanguage(config.language)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isRecognizing(): Boolean = listening && service.isListening()

    override fun isInitialized(): Boolean = initialized && service.isReady()

    override fun getEngineType(): SpeechEngine = SpeechEngine.APPLE_SPEECH

    override fun getSupportedFeatures(): Set<EngineFeature> = setOf(
        EngineFeature.OFFLINE_MODE,
        EngineFeature.CONTINUOUS_RECOGNITION,
        EngineFeature.PUNCTUATION
    )

    override suspend fun destroy() {
        listening = false
        initialized = false
        service.release()
        scope.cancel()
        _state.value = EngineState.Destroyed
    }

    // ═══════════════════════════════════════════════════════════════════
    // Type Mapping
    // ═══════════════════════════════════════════════════════════════════

    private fun mapRecognitionResult(result: RecognitionResult): SpeechResult {
        return SpeechResult(
            text = result.text,
            confidence = result.confidence,
            isFinal = result.isFinal,
            timestamp = result.timestamp,
            alternatives = result.alternatives.map { alt ->
                SpeechResult.Alternative(text = alt, confidence = result.confidence * 0.8f)
            }
        )
    }

    /**
     * Maps SpeechRecognition module's SpeechError (code: Int) to
     * VoiceOSCore's SpeechError (code: ErrorCode enum).
     */
    private fun mapSpeechError(error: com.augmentalis.speechrecognition.SpeechError): SpeechError {
        val errorCode = when (error.code) {
            com.augmentalis.speechrecognition.SpeechError.ERROR_PERMISSIONS ->
                SpeechError.ErrorCode.PERMISSION_DENIED
            com.augmentalis.speechrecognition.SpeechError.ERROR_AUDIO ->
                SpeechError.ErrorCode.AUDIO_ERROR
            com.augmentalis.speechrecognition.SpeechError.ERROR_NETWORK,
            com.augmentalis.speechrecognition.SpeechError.ERROR_NETWORK_TIMEOUT,
            com.augmentalis.speechrecognition.SpeechError.ERROR_SERVER,
            com.augmentalis.speechrecognition.SpeechError.ERROR_CLIENT ->
                SpeechError.ErrorCode.NETWORK_ERROR
            com.augmentalis.speechrecognition.SpeechError.ERROR_MODEL ->
                SpeechError.ErrorCode.MODEL_NOT_FOUND
            com.augmentalis.speechrecognition.SpeechError.ERROR_NO_MATCH ->
                SpeechError.ErrorCode.RECOGNITION_FAILED
            com.augmentalis.speechrecognition.SpeechError.ERROR_SPEECH_TIMEOUT ->
                SpeechError.ErrorCode.TIMEOUT
            com.augmentalis.speechrecognition.SpeechError.ERROR_NOT_INITIALIZED,
            com.augmentalis.speechrecognition.SpeechError.ERROR_INVALID_STATE ->
                SpeechError.ErrorCode.NOT_INITIALIZED
            com.augmentalis.speechrecognition.SpeechError.ERROR_BUSY ->
                SpeechError.ErrorCode.ENGINE_BUSY
            com.augmentalis.speechrecognition.SpeechError.ERROR_MEMORY,
            com.augmentalis.speechrecognition.SpeechError.ERROR_LICENSE ->
                SpeechError.ErrorCode.UNKNOWN
            else -> SpeechError.ErrorCode.UNKNOWN
        }
        return SpeechError(
            code = errorCode,
            message = error.message,
            recoverable = error.isRecoverable
        )
    }

    private fun mapServiceState(serviceState: ServiceState): EngineState {
        return when (serviceState) {
            ServiceState.UNINITIALIZED -> EngineState.Uninitialized
            ServiceState.INITIALIZING -> EngineState.Initializing
            ServiceState.READY -> EngineState.Ready(SpeechEngine.APPLE_SPEECH)
            ServiceState.LISTENING -> EngineState.Listening
            ServiceState.PROCESSING -> EngineState.Processing
            ServiceState.PAUSED -> EngineState.Ready(SpeechEngine.APPLE_SPEECH)
            ServiceState.STOPPED -> EngineState.Ready(SpeechEngine.APPLE_SPEECH)
            ServiceState.ERROR -> EngineState.Error("Speech engine error", recoverable = true)
            ServiceState.DESTROYING -> EngineState.Destroyed
        }
    }

    /**
     * Maps VoiceOSCore's SpeechConfig to SpeechRecognition module's SpeechConfig.
     *
     * VoiceOSCore SpeechMode has COMBINED_COMMAND which SpeechRecognition lacks —
     * maps to STATIC_COMMAND since the combined behavior is handled at the
     * VoiceOSCore coordinator level, not the engine level.
     */
    private fun mapToSpeechRecognitionConfig(config: SpeechConfig): com.augmentalis.speechrecognition.SpeechConfig {
        val srMode = when (config.mode) {
            SpeechMode.STATIC_COMMAND -> com.augmentalis.speechrecognition.SpeechMode.STATIC_COMMAND
            SpeechMode.DYNAMIC_COMMAND -> com.augmentalis.speechrecognition.SpeechMode.DYNAMIC_COMMAND
            SpeechMode.COMBINED_COMMAND -> com.augmentalis.speechrecognition.SpeechMode.STATIC_COMMAND
            SpeechMode.DICTATION -> com.augmentalis.speechrecognition.SpeechMode.DICTATION
            SpeechMode.FREE_SPEECH -> com.augmentalis.speechrecognition.SpeechMode.FREE_SPEECH
            SpeechMode.MUTED -> com.augmentalis.speechrecognition.SpeechMode.STATIC_COMMAND  // Muted = restricted wake-only grammar
            SpeechMode.HYBRID -> com.augmentalis.speechrecognition.SpeechMode.HYBRID
        }
        return com.augmentalis.speechrecognition.SpeechConfig(
            language = config.language,
            engine = com.augmentalis.speechrecognition.SpeechEngine.APPLE_SPEECH,
            mode = srMode,
            confidenceThreshold = config.confidenceThreshold,
            enableFuzzyMatching = true
        )
    }
}
