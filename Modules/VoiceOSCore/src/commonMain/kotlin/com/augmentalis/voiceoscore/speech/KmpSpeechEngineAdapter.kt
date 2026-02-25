/**
 * KmpSpeechEngineAdapter.kt - Adapter for KMP SpeechRecognitionService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-18
 *
 * Bridges the KMP SpeechRecognitionService to the ISpeechEngine interface
 * used by VoiceOSCoreNG. This enables VoiceOSCoreNG to use the unified
 * SpeechRecognition module with its CommandMatchingService integration.
 *
 * Benefits:
 * - Uses CommandMatchingService for fuzzy/semantic matching
 * - Single unified speech API across all platforms
 * - Consistent result processing and error handling
 */
package com.augmentalis.voiceoscore

import com.augmentalis.speechrecognition.SpeechRecognitionService
import com.augmentalis.speechrecognition.createSpeechRecognitionService
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.ServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Adapter that implements ISpeechEngine using SpeechRecognitionService.
 *
 * This allows VoiceOSCoreNG to use the unified KMP SpeechRecognition module
 * which includes integrated CommandMatchingService for fuzzy/semantic matching.
 *
 * Usage:
 * ```kotlin
 * val core = VoiceOSCoreNG.Builder()
 *     .withSpeechEngineFactory(KmpSpeechEngineFactory())
 *     .build()
 * ```
 */
class KmpSpeechEngineAdapter(
    private val targetEngine: SpeechEngine = SpeechEngine.VOSK
) : ISpeechEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Underlying KMP service
    private var service: SpeechRecognitionService? = null

    // State management
    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    // Result flow
    private val _results = MutableSharedFlow<SpeechResult>(replay = 1)
    override val results: Flow<SpeechResult> = _results.asSharedFlow()

    // Error flow
    private val _errors = MutableSharedFlow<SpeechError>(replay = 1)
    override val errors: Flow<SpeechError> = _errors.asSharedFlow()

    // Configuration
    private var currentConfig: SpeechConfig? = null
    private var engineType: SpeechEngine = targetEngine

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return try {
            _state.value = EngineState.Initializing

            // Create KMP service
            val svc = createSpeechRecognitionService()
            service = svc

            // Map config to KMP SpeechConfig
            val kmpConfig = com.augmentalis.speechrecognition.SpeechConfig(
                language = config.language,
                mode = mapModeToKmp(config.mode),
                engine = mapEngineToKmp(engineType),
                confidenceThreshold = config.confidenceThreshold,
                enableFuzzyMatching = true,
                enableSemanticMatching = true,
                enableVAD = config.enableVAD,
                maxRecordingDuration = config.maxRecordingDuration,
                timeoutDuration = config.silenceTimeout
            )

            // Initialize service
            val result = svc.initialize(kmpConfig)
            if (result.isFailure) {
                _state.value = EngineState.Error(
                    result.exceptionOrNull()?.message ?: "Initialization failed",
                    recoverable = true
                )
                return result
            }

            currentConfig = config
            // engineType is already set from constructor - don't update from config

            // Collect state changes
            collectServiceState(svc)

            // Collect results
            collectServiceResults(svc)

            // Collect errors
            collectServiceErrors(svc)

            _state.value = EngineState.Ready(engineType)
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = EngineState.Error(e.message ?: "Unknown error", recoverable = true)
            Result.failure(e)
        }
    }

    private fun collectServiceState(svc: SpeechRecognitionService) {
        scope.launch {
            svc.stateFlow.collect { serviceState ->
                _state.value = when (serviceState) {
                    ServiceState.UNINITIALIZED -> EngineState.Uninitialized
                    ServiceState.INITIALIZING -> EngineState.Initializing
                    ServiceState.READY -> EngineState.Ready(engineType)
                    ServiceState.LISTENING -> EngineState.Listening
                    ServiceState.PROCESSING -> EngineState.Processing
                    ServiceState.PAUSED -> EngineState.Ready(engineType)
                    ServiceState.STOPPED -> EngineState.Ready(engineType)
                    ServiceState.ERROR -> EngineState.Error("Service error", recoverable = true)
                    ServiceState.DESTROYING -> EngineState.Destroyed
                }
            }
        }
    }

    private fun collectServiceResults(svc: SpeechRecognitionService) {
        scope.launch {
            svc.resultFlow.collect { result ->
                _results.emit(mapResult(result))
            }
        }
    }

    private fun collectServiceErrors(svc: SpeechRecognitionService) {
        scope.launch {
            svc.errorFlow.collect { error ->
                _errors.emit(mapError(error))
            }
        }
    }

    override suspend fun startListening(): Result<Unit> {
        val svc = service ?: return Result.failure(IllegalStateException("Not initialized"))
        return svc.startListening()
    }

    override suspend fun stopListening() {
        service?.stopListening()
    }

    override suspend fun updateCommands(commands: List<String>): Result<Unit> {
        return try {
            // Separate static and dynamic commands
            // For this adapter, treat all as dynamic since context changes
            service?.setCommands(
                staticCommands = emptyList(),
                dynamicCommands = commands
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        val svc = service ?: return Result.failure(IllegalStateException("Not initialized"))

        // Apply configuration changes
        svc.setLanguage(config.language)
        svc.setMode(mapModeToKmp(config.mode))
        currentConfig = config

        return Result.success(Unit)
    }

    override fun isRecognizing(): Boolean {
        return _state.value is EngineState.Listening || _state.value is EngineState.Processing
    }

    override fun isInitialized(): Boolean {
        return service != null && service?.isReady() == true
    }

    override fun getEngineType(): SpeechEngine = engineType

    override fun getSupportedFeatures(): Set<EngineFeature> {
        return setOf(
            EngineFeature.OFFLINE_MODE,
            EngineFeature.CONTINUOUS_RECOGNITION,
            EngineFeature.CUSTOM_VOCABULARY,
            EngineFeature.PROFANITY_FILTER
        )
    }

    override suspend fun destroy() {
        _state.value = EngineState.Destroyed
        service?.release()
        service = null
        scope.cancel()
    }

    // =========================================================================
    // Mapping Functions - VoiceOSCore -> SpeechRecognition
    // =========================================================================

    /**
     * Map VoiceOSCore SpeechMode to SpeechRecognition SpeechMode.
     *
     * Key design notes:
     * - MUTED maps to STATIC_COMMAND at the engine level because the engine still
     *   needs to recognize wake commands ("wake up voice", etc.). Grammar filtering
     *   (restricting to exit/wake commands only) is handled upstream in
     *   VoiceOSCore.setSpeechMode() via engine.updateCommands(exitCommands).
     * - COMBINED_COMMAND maps to STATIC_COMMAND because the combined static+dynamic
     *   command matching is handled by VoiceOSCore's ActionCoordinator, not the
     *   speech engine. The engine just needs restricted grammar recognition.
     * - The SpeechRecognition module's SpeechMode enum intentionally omits MUTED
     *   and COMBINED_COMMAND because those are VoiceOSCore-level orchestration
     *   concerns, not speech engine concerns.
     */
    private fun mapModeToKmp(mode: SpeechMode): com.augmentalis.speechrecognition.SpeechMode {
        return when (mode) {
            SpeechMode.STATIC_COMMAND -> com.augmentalis.speechrecognition.SpeechMode.STATIC_COMMAND
            SpeechMode.DYNAMIC_COMMAND -> com.augmentalis.speechrecognition.SpeechMode.DYNAMIC_COMMAND
            SpeechMode.COMBINED_COMMAND -> com.augmentalis.speechrecognition.SpeechMode.STATIC_COMMAND
            SpeechMode.DICTATION -> com.augmentalis.speechrecognition.SpeechMode.DICTATION
            SpeechMode.FREE_SPEECH -> com.augmentalis.speechrecognition.SpeechMode.FREE_SPEECH
            SpeechMode.MUTED -> com.augmentalis.speechrecognition.SpeechMode.STATIC_COMMAND
            SpeechMode.HYBRID -> com.augmentalis.speechrecognition.SpeechMode.HYBRID
        }
    }

    /**
     * Map VoiceOSCore SpeechEngine to SpeechRecognition SpeechEngine
     */
    private fun mapEngineToKmp(engine: SpeechEngine): com.augmentalis.speechrecognition.SpeechEngine {
        return when (engine) {
            SpeechEngine.VOSK -> com.augmentalis.speechrecognition.SpeechEngine.VOSK
            SpeechEngine.VIVOKA -> com.augmentalis.speechrecognition.SpeechEngine.VIVOKA
            SpeechEngine.ANDROID_STT -> com.augmentalis.speechrecognition.SpeechEngine.ANDROID_STT
            SpeechEngine.WHISPER -> com.augmentalis.speechrecognition.SpeechEngine.WHISPER
            SpeechEngine.GOOGLE_CLOUD -> com.augmentalis.speechrecognition.SpeechEngine.GOOGLE_CLOUD
            SpeechEngine.AZURE -> com.augmentalis.speechrecognition.SpeechEngine.AZURE
            SpeechEngine.APPLE_SPEECH -> com.augmentalis.speechrecognition.SpeechEngine.APPLE_SPEECH
            SpeechEngine.AVX -> com.augmentalis.speechrecognition.SpeechEngine.AVX
        }
    }

    /**
     * Map SpeechRecognition RecognitionResult to VoiceOSCore SpeechResult
     */
    private fun mapResult(result: RecognitionResult): SpeechResult {
        return SpeechResult(
            text = result.text,
            confidence = result.confidence,
            isFinal = result.isFinal,
            timestamp = result.timestamp,
            alternatives = result.alternatives.map { alt ->
                SpeechResult.Alternative(alt, result.confidence * 0.9f)
            }
        )
    }

    /**
     * Map SpeechRecognition SpeechError to VoiceOSCore SpeechError
     */
    private fun mapError(error: com.augmentalis.speechrecognition.SpeechError): SpeechError {
        val code = when (error.code) {
            com.augmentalis.speechrecognition.SpeechError.ERROR_NOT_INITIALIZED -> SpeechError.ErrorCode.NOT_INITIALIZED
            com.augmentalis.speechrecognition.SpeechError.ERROR_AUDIO -> SpeechError.ErrorCode.AUDIO_ERROR
            com.augmentalis.speechrecognition.SpeechError.ERROR_NETWORK,
            com.augmentalis.speechrecognition.SpeechError.ERROR_NETWORK_TIMEOUT -> SpeechError.ErrorCode.NETWORK_ERROR
            com.augmentalis.speechrecognition.SpeechError.ERROR_PERMISSIONS -> SpeechError.ErrorCode.PERMISSION_DENIED
            com.augmentalis.speechrecognition.SpeechError.ERROR_NO_MATCH -> SpeechError.ErrorCode.NO_SPEECH_DETECTED
            com.augmentalis.speechrecognition.SpeechError.ERROR_MODEL -> SpeechError.ErrorCode.MODEL_NOT_FOUND
            com.augmentalis.speechrecognition.SpeechError.ERROR_SPEECH_TIMEOUT -> SpeechError.ErrorCode.TIMEOUT
            com.augmentalis.speechrecognition.SpeechError.ERROR_BUSY -> SpeechError.ErrorCode.ENGINE_BUSY
            else -> SpeechError.ErrorCode.UNKNOWN
        }

        return SpeechError(
            code = code,
            message = error.message,
            recoverable = error.isRecoverable
        )
    }
}

/**
 * Factory that creates KmpSpeechEngineAdapter instances.
 *
 * Use this factory with VoiceOSCoreNG.Builder to enable the unified
 * SpeechRecognition module with CommandMatchingService integration.
 */
class KmpSpeechEngineFactory : ISpeechEngineFactory {

    override fun getAvailableEngines(): List<SpeechEngine> {
        return listOf(
            SpeechEngine.VOSK,
            SpeechEngine.ANDROID_STT,
            SpeechEngine.VIVOKA,
            SpeechEngine.WHISPER
        )
    }

    override fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return engine in getAvailableEngines()
    }

    override fun createEngine(engine: SpeechEngine): Result<ISpeechEngine> {
        return Result.success(KmpSpeechEngineAdapter(engine))
    }

    override fun getRecommendedEngine(): SpeechEngine {
        return SpeechEngine.VOSK
    }

    override fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature> {
        return setOf(
            EngineFeature.OFFLINE_MODE,
            EngineFeature.CONTINUOUS_RECOGNITION,
            EngineFeature.CUSTOM_VOCABULARY,
            EngineFeature.PROFANITY_FILTER
        )
    }

    override fun getSetupRequirements(engine: SpeechEngine): EngineRequirements {
        return when (engine) {
            SpeechEngine.VOSK -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 50,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Offline speech recognition with custom vocabulary support"
            )
            SpeechEngine.ANDROID_STT -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = false,
                notes = "Uses Google's online speech recognition"
            )
            else -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = false,
                requiresApiKey = false
            )
        }
    }
}
