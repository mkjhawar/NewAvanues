/**
 * DesktopSpeechRecognitionService.kt - Desktop speech recognition service implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-01-18
 *
 * Desktop-specific implementation of SpeechRecognitionService.
 * Delegates to DesktopWhisperEngine for offline recognition via whisper.cpp JNI.
 * Future: VOSK, Vivoka Desktop, Google Cloud STT, macOS Apple Speech.
 */
package com.augmentalis.speechrecognition

import com.augmentalis.nlu.matching.CommandMatchingService
import com.augmentalis.speechrecognition.whisper.DesktopWhisperConfig
import com.augmentalis.speechrecognition.whisper.DesktopWhisperEngine
import com.augmentalis.speechrecognition.whisper.WhisperEngineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Desktop implementation of SpeechRecognitionService.
 *
 * Routes to the appropriate engine based on [SpeechConfig.engine]:
 * - WHISPER: DesktopWhisperEngine (offline, whisper.cpp via JNI)
 * - Others: not yet implemented, falls back to Whisper
 */
class DesktopSpeechRecognitionService : SpeechRecognitionService {

    companion object {
        private const val TAG = "DesktopSpeechService"
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

    // Engine
    private var whisperEngine: DesktopWhisperEngine? = null

    // Coroutine scope for flow collection
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Flows
    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1)
    override val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    override val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    private val _stateFlow = MutableSharedFlow<ServiceState>(replay = 1)
    override val stateFlow: SharedFlow<ServiceState> = _stateFlow.asSharedFlow()

    /**
     * Initialize with configuration.
     * Creates and initializes the engine specified in [config].
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

            when (config.engine) {
                SpeechEngine.WHISPER -> initializeWhisper()
                // Future engines: VOSK, VIVOKA, GOOGLE_CLOUD, APPLE_SPEECH
                else -> {
                    logWarn(TAG, "Engine ${config.engine} not yet available on Desktop, falling back to Whisper")
                    initializeWhisper()
                }
            }

            logInfo(TAG, "Initialized with engine: ${config.engine}")
            updateState(ServiceState.READY)

            Result.success(Unit)
        } catch (e: Exception) {
            logError(TAG, "Initialization failed", e)
            updateState(ServiceState.ERROR)
            _errorFlow.emit(SpeechError.unknownError(e.message))
            Result.failure(e)
        }
    }

    override suspend fun startListening(): Result<Unit> {
        return try {
            if (!state.canStart()) {
                return Result.failure(IllegalStateException("Cannot start from state: $state"))
            }

            val engine = whisperEngine
            if (engine == null) {
                _errorFlow.emit(SpeechError(
                    code = SpeechError.ERROR_NOT_AVAILABLE,
                    message = "No speech engine initialized",
                    isRecoverable = true,
                    suggestedAction = SpeechError.Action.RETRY
                ))
                return Result.failure(IllegalStateException("No speech engine initialized"))
            }

            val started = engine.startListening(config.mode.toSpeechMode())
            if (started) {
                updateState(ServiceState.LISTENING)
                Result.success(Unit)
            } else {
                _errorFlow.emit(SpeechError.audioError("Failed to start audio capture"))
                Result.failure(IllegalStateException("Engine failed to start listening"))
            }
        } catch (e: Exception) {
            logError(TAG, "Failed to start listening", e)
            _errorFlow.emit(SpeechError.audioError(e.message))
            Result.failure(e)
        }
    }

    override suspend fun stopListening(): Result<Unit> {
        return try {
            whisperEngine?.stopListening()
            updateState(ServiceState.STOPPED)
            logInfo(TAG, "Stopped listening")
            Result.success(Unit)
        } catch (e: Exception) {
            logError(TAG, "Failed to stop listening", e)
            Result.failure(e)
        }
    }

    override suspend fun pause(): Result<Unit> {
        return try {
            if (state == ServiceState.LISTENING) {
                whisperEngine?.pause()
                updateState(ServiceState.PAUSED)
                logInfo(TAG, "Paused")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resume(): Result<Unit> {
        return try {
            if (state == ServiceState.PAUSED) {
                val resumed = whisperEngine?.resume() ?: false
                if (resumed) {
                    updateState(ServiceState.LISTENING)
                    logInfo(TAG, "Resumed")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setCommands(staticCommands: List<String>, dynamicCommands: List<String>) {
        commandCache.setStaticCommands(staticCommands)
        commandCache.setDynamicCommands(dynamicCommands)

        // Update result processor for command matching
        commandMatcher.registerCommands(commandCache.getAllCommands())
        resultProcessor.syncCommandsToMatcher()

        // Also inform the engine for any engine-level command matching
        whisperEngine?.setCommands(staticCommands, dynamicCommands)

        logDebug(TAG, "Updated commands: ${staticCommands.size} static, ${dynamicCommands.size} dynamic")
    }

    override fun setMode(mode: SpeechMode) {
        config = config.withMode(mode)
        resultProcessor.setMode(mode)
        whisperEngine?.setMode(mode)
        logInfo(TAG, "Mode set to: $mode")
    }

    override fun setLanguage(language: String) {
        if (config.language == language) return
        val oldLanguage = config.language
        config = config.withLanguage(language)
        whisperEngine?.setLanguage(language)
        logInfo(TAG, "Language changed: $oldLanguage → $language")
    }

    override fun isListening(): Boolean {
        return whisperEngine?.isListening() ?: false
    }

    override fun isReady(): Boolean {
        return whisperEngine?.isReady() ?: false
    }

    override fun getConfig(): SpeechConfig = config

    override suspend fun release() {
        try {
            updateState(ServiceState.DESTROYING)

            whisperEngine?.destroy()
            whisperEngine = null

            resultProcessor.clear()
            commandCache.clear()
            commandMatcher.clear()
            serviceScope.cancel()

            updateState(ServiceState.UNINITIALIZED)
            logInfo(TAG, "Released")
        } catch (e: Exception) {
            logError(TAG, "Error during release", e)
        }
    }

    // --- Private implementation ---

    /**
     * Initialize the Whisper engine and wire up its result/error flows.
     */
    private suspend fun initializeWhisper() {
        val engine = DesktopWhisperEngine()

        val whisperConfig = DesktopWhisperConfig.autoTuned(config.language)
        val success = engine.initialize(whisperConfig)

        if (!success) {
            throw IllegalStateException(
                "Whisper engine initialization failed. " +
                "Ensure the model is downloaded to: ${whisperConfig.getModelDirectory().absolutePath}"
            )
        }

        // Collect result flow from engine → process through ResultProcessor → emit
        serviceScope.launch {
            engine.resultFlow.collect { result ->
                if (result.isFinal && result.text.isNotBlank()) {
                    // Process through ResultProcessor for command matching
                    val processed = resultProcessor.processResult(
                        text = result.text,
                        confidence = result.confidence,
                        engine = config.engine,
                        isPartial = false,
                        alternatives = emptyList()
                    )
                    processed?.let { _resultFlow.emit(it) }
                } else {
                    // Partial results pass through directly
                    _resultFlow.emit(result)
                }
            }
        }

        // Collect error flow from engine → forward
        serviceScope.launch {
            engine.errorFlow.collect { error ->
                _errorFlow.emit(error)
                if (!error.isRecoverable) {
                    updateState(ServiceState.ERROR)
                }
            }
        }

        whisperEngine = engine
        logInfo(TAG, "Whisper engine initialized")
    }

    /**
     * Map SpeechMode config value to the engine's SpeechMode.
     */
    private fun SpeechMode.toSpeechMode(): SpeechMode = this

    private suspend fun updateState(newState: ServiceState) {
        _state = newState
        _stateFlow.emit(newState)
    }
}
