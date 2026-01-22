/**
 * AndroidSpeechRecognitionService.kt - Android speech recognition service implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude (AI Assistant)
 * Created: 2026-01-18
 * Updated: 2026-01-18 - Integrated AndroidSTTEngine
 *
 * Android-specific implementation of SpeechRecognitionService.
 * Delegates to engine implementations (Android STT, Vosk, Vivoka, Whisper, etc.)
 */
package com.augmentalis.speechrecognition

import android.content.Context
import com.augmentalis.nlu.matching.CommandMatchingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Android implementation of SpeechRecognitionService.
 *
 * This class provides the bridge between the common API and Android-specific
 * speech recognition engines (Android STT, Vosk, Vivoka, Whisper, etc.)
 */
class AndroidSpeechRecognitionService : SpeechRecognitionService {

    companion object {
        private const val TAG = "AndroidSpeechService"

        // Context holder for engine initialization
        private var appContext: Context? = null

        /**
         * Set the application context (call from Application.onCreate())
         */
        fun setContext(context: Context) {
            appContext = context.applicationContext
        }
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

    // Coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Engine implementations
    private var androidSTTEngine: AndroidSTTEngine? = null
    // TODO: Add other engines as they are migrated:
    // private var voskEngine: VoskEngine? = null
    // private var vivokaEngine: VivokaEngine? = null
    // private var whisperEngine: WhisperEngine? = null

    // Flows
    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1)
    override val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    override val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    private val _stateFlow = MutableSharedFlow<ServiceState>(replay = 1)
    override val stateFlow: SharedFlow<ServiceState> = _stateFlow.asSharedFlow()

    /**
     * Initialize with configuration.
     * Sets up the appropriate engine based on config.
     */
    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return try {
            // Validate config first
            config.validate().getOrThrow()

            this.config = config
            updateState(ServiceState.INITIALIZING)

            // Configure result processor
            resultProcessor.setMode(config.mode)
            resultProcessor.setConfidenceThreshold(config.confidenceThreshold)
            resultProcessor.setFuzzyMatchingEnabled(config.enableFuzzyMatching)

            // Get context
            val context = appContext
                ?: return Result.failure(IllegalStateException("Context not set. Call AndroidSpeechRecognitionService.setContext() first."))

            // Initialize appropriate engine based on config
            val initResult = when (config.engine) {
                SpeechEngine.ANDROID_STT -> initializeAndroidSTT(context, config)
                SpeechEngine.VOSK -> {
                    // TODO: Implement when VoskEngine is migrated
                    logInfo(TAG, "Vosk engine not yet migrated, using Android STT fallback")
                    initializeAndroidSTT(context, config.copy(engine = SpeechEngine.ANDROID_STT))
                }
                SpeechEngine.VIVOKA -> {
                    // TODO: Implement when VivokaEngine is migrated
                    logInfo(TAG, "Vivoka engine not yet migrated, using Android STT fallback")
                    initializeAndroidSTT(context, config.copy(engine = SpeechEngine.ANDROID_STT))
                }
                SpeechEngine.WHISPER -> {
                    // TODO: Implement when WhisperEngine is migrated
                    logInfo(TAG, "Whisper engine not yet migrated, using Android STT fallback")
                    initializeAndroidSTT(context, config.copy(engine = SpeechEngine.ANDROID_STT))
                }
                SpeechEngine.GOOGLE_CLOUD -> {
                    // TODO: Implement when GoogleCloudEngine is migrated
                    logInfo(TAG, "Google Cloud engine not yet migrated, using Android STT fallback")
                    initializeAndroidSTT(context, config.copy(engine = SpeechEngine.ANDROID_STT))
                }
                else -> {
                    logInfo(TAG, "Unknown engine ${config.engine}, using Android STT fallback")
                    initializeAndroidSTT(context, config.copy(engine = SpeechEngine.ANDROID_STT))
                }
            }

            if (!initResult) {
                updateState(ServiceState.ERROR)
                return Result.failure(IllegalStateException("Failed to initialize ${config.engine} engine"))
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

    /**
     * Initialize Android STT engine
     */
    private suspend fun initializeAndroidSTT(context: Context, config: SpeechConfig): Boolean {
        val engine = AndroidSTTEngine(context)
        val success = engine.initialize(config)

        if (success) {
            androidSTTEngine = engine

            // Collect results from engine
            scope.launch {
                engine.resultFlow.collect { result ->
                    _resultFlow.emit(result)
                }
            }

            // Collect errors from engine
            scope.launch {
                engine.errorFlow.collect { error ->
                    _errorFlow.emit(error)
                    if (!error.isRecoverable) {
                        updateState(ServiceState.ERROR)
                    }
                }
            }
        }

        return success
    }

    override suspend fun startListening(): Result<Unit> {
        return try {
            if (!state.canStart()) {
                return Result.failure(IllegalStateException("Cannot start from state: $state"))
            }

            // Delegate to active engine
            val started = when (config.engine) {
                SpeechEngine.ANDROID_STT -> androidSTTEngine?.startListening(config.mode) ?: false
                // TODO: Add other engines
                else -> androidSTTEngine?.startListening(config.mode) ?: false
            }

            if (started) {
                updateState(ServiceState.LISTENING)
                logInfo(TAG, "Started listening")
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Failed to start listening"))
            }
        } catch (e: Exception) {
            logError(TAG, "Failed to start listening", e)
            _errorFlow.emit(SpeechError.audioError(e.message))
            Result.failure(e)
        }
    }

    override suspend fun stopListening(): Result<Unit> {
        return try {
            // Delegate to active engine
            when (config.engine) {
                SpeechEngine.ANDROID_STT -> androidSTTEngine?.stopListening()
                // TODO: Add other engines
                else -> androidSTTEngine?.stopListening()
            }

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
                // Stop engine but don't release it
                when (config.engine) {
                    SpeechEngine.ANDROID_STT -> androidSTTEngine?.stopListening()
                    else -> androidSTTEngine?.stopListening()
                }
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
                // Restart engine
                when (config.engine) {
                    SpeechEngine.ANDROID_STT -> androidSTTEngine?.startListening(config.mode)
                    else -> androidSTTEngine?.startListening(config.mode)
                }
                updateState(ServiceState.LISTENING)
                logInfo(TAG, "Resumed")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setCommands(staticCommands: List<String>, dynamicCommands: List<String>) {
        commandCache.setStaticCommands(staticCommands)
        commandCache.setDynamicCommands(dynamicCommands)

        // Sync to CommandMatchingService
        commandMatcher.registerCommands(commandCache.getAllCommands())
        resultProcessor.syncCommandsToMatcher()

        // Sync to active engine
        androidSTTEngine?.setStaticCommands(staticCommands)
        androidSTTEngine?.setDynamicCommands(dynamicCommands)

        logDebug(TAG, "Updated commands: ${staticCommands.size} static, ${dynamicCommands.size} dynamic")
    }

    override fun setMode(mode: SpeechMode) {
        config = config.withMode(mode)
        resultProcessor.setMode(mode)

        // Update engine mode
        androidSTTEngine?.changeMode(mode)

        logInfo(TAG, "Mode set to: $mode")
    }

    override fun setLanguage(language: String) {
        config = config.withLanguage(language)
        logInfo(TAG, "Language set to: $language")
        // Note: Engine language change requires reinitialization
    }

    override fun isListening(): Boolean {
        return androidSTTEngine?.isListening() ?: false
    }

    override fun isReady(): Boolean = state.isOperational()

    override fun getConfig(): SpeechConfig = config

    override suspend fun release() {
        try {
            updateState(ServiceState.DESTROYING)

            // Destroy all engines
            androidSTTEngine?.destroy()
            androidSTTEngine = null

            // Clear processors
            resultProcessor.clear()
            commandCache.clear()
            commandMatcher.clear()

            // Cancel scope
            scope.cancel()

            updateState(ServiceState.UNINITIALIZED)
            logInfo(TAG, "Released")
        } catch (e: Exception) {
            logError(TAG, "Error during release", e)
        }
    }

    /**
     * Process recognition result from engine.
     * Called by engine implementations when they have a result.
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
     * Handle error from engine.
     */
    internal suspend fun onError(error: SpeechError) {
        _errorFlow.emit(error)
        if (!error.isRecoverable) {
            updateState(ServiceState.ERROR)
        }
    }

    private suspend fun updateState(newState: ServiceState) {
        _state = newState
        _stateFlow.emit(newState)
    }
}
