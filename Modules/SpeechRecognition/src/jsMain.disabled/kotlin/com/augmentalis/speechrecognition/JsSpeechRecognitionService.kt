/**
 * JsSpeechRecognitionService.kt - JS/Web speech recognition service implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude (AI Assistant)
 * Created: 2026-01-18
 *
 * JS/Web-specific implementation of SpeechRecognitionService.
 * Uses Web Speech API for browser-based recognition.
 */
package com.augmentalis.speechrecognition

import com.augmentalis.nlu.matching.CommandMatchingService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * JS/Web implementation of SpeechRecognitionService.
 *
 * Uses the Web Speech API (SpeechRecognition) for browser-based recognition.
 */
class JsSpeechRecognitionService : SpeechRecognitionService {

    companion object {
        private const val TAG = "JsSpeechService"
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

    /**
     * Initialize with configuration.
     * Sets up Web Speech API based on config.
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

            // TODO: Check Web Speech API availability
            // val webkitSpeechRecognition = js("window.webkitSpeechRecognition || window.SpeechRecognition")
            // if (webkitSpeechRecognition == null) throw UnsupportedOperationException("Web Speech API not supported")

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

            updateState(ServiceState.LISTENING)
            logInfo(TAG, "Started listening")

            // TODO: Start Web Speech API recognition
            // recognition.start()
            Result.success(Unit)
        } catch (e: Exception) {
            logError(TAG, "Failed to start listening", e)
            _errorFlow.emit(SpeechError.audioError(e.message))
            Result.failure(e)
        }
    }

    override suspend fun stopListening(): Result<Unit> {
        return try {
            updateState(ServiceState.STOPPED)
            logInfo(TAG, "Stopped listening")

            // TODO: Stop Web Speech API recognition
            // recognition.stop()
            Result.success(Unit)
        } catch (e: Exception) {
            logError(TAG, "Failed to stop listening", e)
            Result.failure(e)
        }
    }

    override suspend fun pause(): Result<Unit> {
        return try {
            if (state == ServiceState.LISTENING) {
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

        commandMatcher.registerCommands(commandCache.getAllCommands())
        resultProcessor.syncCommandsToMatcher()

        logDebug(TAG, "Updated commands: ${staticCommands.size} static, ${dynamicCommands.size} dynamic")
    }

    override fun setMode(mode: SpeechMode) {
        config = config.withMode(mode)
        resultProcessor.setMode(mode)
        logInfo(TAG, "Mode set to: $mode")
    }

    override fun setLanguage(language: String) {
        config = config.withLanguage(language)
        logInfo(TAG, "Language set to: $language")
        // TODO: Set Web Speech API language
        // recognition.lang = language
    }

    override fun isListening(): Boolean = state == ServiceState.LISTENING

    override fun isReady(): Boolean = state.isOperational()

    override fun getConfig(): SpeechConfig = config

    override suspend fun release() {
        try {
            updateState(ServiceState.DESTROYING)
            resultProcessor.clear()
            commandCache.clear()
            commandMatcher.clear()

            // TODO: Clean up Web Speech API
            // recognition.abort()

            updateState(ServiceState.UNINITIALIZED)
            logInfo(TAG, "Released")
        } catch (e: Exception) {
            logError(TAG, "Error during release", e)
        }
    }

    /**
     * Process recognition result from Web Speech API.
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
     * Handle error from Web Speech API.
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
