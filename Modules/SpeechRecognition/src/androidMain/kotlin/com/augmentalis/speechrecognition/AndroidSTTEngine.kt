/**
 * AndroidSTTEngine.kt - Android SpeechRecognizer-based speech recognition engine
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-27
 * Updated: 2026-01-18 - Migrated to KMP androidMain
 *
 * SOLID-refactored orchestrator that coordinates specialized components:
 * - AndroidSTTConfig: Configuration management
 * - AndroidSTTListener: RecognitionListener implementation
 * - AndroidSTTRecognizer: SpeechRecognizer wrapper
 */
package com.augmentalis.speechrecognition

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Android SpeechRecognizer-based speech recognition engine.
 * Uses Google's online speech recognition service.
 */
class AndroidSTTEngine(private val context: Context) {

    companion object {
        private const val TAG = "AndroidSTTEngine"
        private const val ENGINE_NAME = "AndroidSTT"
        private const val ENGINE_VERSION = "3.0.0-KMP"
        private const val SILENCE_CHECK_INTERVAL = 500L
    }

    // Components
    private val config = AndroidSTTConfig(context)
    private val performanceMonitor = PerformanceMonitor(ENGINE_NAME)
    private val errorRecoveryManager = ErrorRecoveryManager(ENGINE_NAME, context)
    private val learningSystem = LearningSystem(ENGINE_NAME, context)
    private val commandCache = CommandCache()
    private lateinit var recognizer: AndroidSTTRecognizer
    private lateinit var listener: AndroidSTTListener

    // Coroutine management
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    // State
    @Volatile private var isInitialized = false
    @Volatile private var lastExecutedCommandTime = System.currentTimeMillis()
    private var silenceStartTime = 0L
    private var timeoutJob: Job? = null

    // Result flows
    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1)
    val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    // Silence check for dictation mode
    private val silenceCheckRunnable = object : Runnable {
        override fun run() {
            if (config.isDictationActive()) {
                val currentTime = System.currentTimeMillis()
                if (silenceStartTime > 0 && (currentTime - silenceStartTime >= config.getDictationTimeoutMs())) {
                    stopDictation()
                } else {
                    mainHandler.postDelayed(this, SILENCE_CHECK_INTERVAL)
                }
            }
        }
    }

    /**
     * Initialize the engine
     */
    suspend fun initialize(speechConfig: SpeechConfig): Boolean {
        Log.i(TAG, "Initializing $ENGINE_NAME v$ENGINE_VERSION")

        return try {
            // Initialize configuration
            if (!config.initialize(speechConfig)) {
                Log.e(TAG, "Failed to initialize configuration")
                return false
            }

            // Create listener
            listener = AndroidSTTListener(performanceMonitor)
            setupListenerCallbacks()

            // Create recognizer
            recognizer = AndroidSTTRecognizer(context, performanceMonitor)

            // Initialize recognizer on main thread
            val initResult = withContext(Dispatchers.Main) {
                recognizer.initialize(listener)
            }

            if (!initResult) {
                Log.e(TAG, "Failed to initialize recognizer")
                return false
            }

            isInitialized = true
            errorRecoveryManager.resetRetryState()

            Log.i(TAG, "$ENGINE_NAME initialized successfully")
            Log.d(TAG, config.getConfigSummary())

            true
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed: ${e.message}", e)
            scope.launch { _errorFlow.emit(SpeechError.unknownError(e.message)) }
            false
        }
    }

    /**
     * Setup listener callbacks
     */
    private fun setupListenerCallbacks() {
        listener.setOnResultsCallback { results ->
            if (results.isNotEmpty()) {
                processRecognitionResult(results[0])
            }
        }

        listener.setOnPartialResultsCallback { partialText ->
            handleSilenceCheck(partialText)
            // Emit partial result
            scope.launch {
                _resultFlow.emit(RecognitionResult(
                    text = partialText,
                    confidence = 0.5f,
                    isPartial = true,
                    isFinal = false,
                    engine = ENGINE_NAME
                ))
            }
        }

        listener.setOnErrorCallback { errorCode, errorMessage ->
            handleError(errorCode, errorMessage)
        }

        listener.setOnRmsChangedCallback { rmsdB ->
            if (config.isDictationActive() && rmsdB < -2.0f) {
                handleSilenceCheck("")
            }
        }
    }

    /**
     * Start listening
     */
    fun startListening(mode: SpeechMode = config.getSpeechMode()): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Cannot start listening - not initialized")
            return false
        }

        if (recognizer.isListening()) {
            return true
        }

        config.setSpeechMode(mode)
        scope.launch {
            recognizer.startListening(mode, config.getLanguage())
        }

        return true
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        scope.launch {
            recognizer.stopListening()
        }
    }

    /**
     * Check if listening
     */
    fun isListening(): Boolean = if (::recognizer.isInitialized) recognizer.isListening() else false

    /**
     * Changes the speech recognition mode asynchronously.
     *
     * IMPORTANT: This method launches a coroutine and returns before the mode change completes.
     * Callers should observe the config state to confirm the mode has switched before calling
     * startListening(). Internal callers (stopDictation) handle this via sequential coroutine
     * execution within the same scope.
     */
    fun changeMode(mode: SpeechMode): Boolean {
        config.setSpeechMode(mode)

        scope.launch {
            when (mode) {
                SpeechMode.FREE_SPEECH, SpeechMode.DICTATION -> switchToDictationMode()
                else -> switchToCommandMode()
            }
        }

        return true
    }

    /**
     * Switch to command mode
     */
    private suspend fun switchToCommandMode() {
        config.setDictationActive(false)
        config.setSpeechMode(SpeechMode.DYNAMIC_COMMAND)
        mainHandler.removeCallbacks(silenceCheckRunnable)

        if (recognizer.isListening()) {
            recognizer.switchMode(SpeechMode.DYNAMIC_COMMAND)
        }
    }

    /**
     * Switch to dictation mode
     */
    private suspend fun switchToDictationMode() {
        config.setDictationActive(true)
        config.setSpeechMode(SpeechMode.FREE_SPEECH)
        silenceStartTime = 0L
        mainHandler.post(silenceCheckRunnable)

        if (recognizer.isListening()) {
            recognizer.switchMode(SpeechMode.FREE_SPEECH)
        }
    }

    /**
     * Stop dictation mode
     */
    private fun stopDictation() {
        Log.i(TAG, "Stopping dictation mode")
        mainHandler.removeCallbacks(silenceCheckRunnable)
        changeMode(SpeechMode.DYNAMIC_COMMAND)
    }

    /**
     * Set static commands
     */
    fun setStaticCommands(commands: List<String>) {
        Log.i(TAG, "Setting ${commands.size} static commands")
        commandCache.setStaticCommands(commands.filter { it.isNotBlank() })
    }

    /**
     * Set dynamic commands
     */
    fun setDynamicCommands(commands: List<String>) {
        Log.i(TAG, "Setting ${commands.size} dynamic commands")
        commandCache.setDynamicCommands(commands.filter { it.isNotBlank() })
    }

    /**
     * Process recognition result
     */
    private fun processRecognitionResult(text: String) {
        if (text.isBlank()) return

        Log.d(TAG, "Processing result: $text")
        silenceStartTime = 0L

        // Check special commands
        when {
            config.isUnmuteCommand(text) -> {
                handleUnmuteCommand()
                return
            }
            config.isMuteCommand(text) -> {
                handleMuteCommand()
                return
            }
            config.isStartDictationCommand(text) -> {
                scope.launch { switchToDictationMode() }
                return
            }
            config.isStopDictationCommand(text) -> {
                scope.launch { switchToCommandMode() }
                return
            }
        }

        // Process normal command
        scope.launch {
            val matchResult = learningSystem.processWithLearning(
                text,
                commandCache.getAllCommands(),
                0.8f
            )

            val finalText = if (matchResult.source != LearningSystem.MatchSource.NO_MATCH) {
                matchResult.matched
            } else {
                commandCache.findMatch(text) ?: text
            }

            val result = RecognitionResult(
                text = finalText,
                originalText = text,
                confidence = if (finalText == text) 1.0f else matchResult.confidence,
                isFinal = true,
                engine = ENGINE_NAME,
                mode = config.getSpeechMode().name
            )

            _resultFlow.emit(result)
            learningSystem.recordSuccess(finalText, result.confidence)
            errorRecoveryManager.resetRetryState()
            lastExecutedCommandTime = System.currentTimeMillis()
        }
    }

    /**
     * Handle error
     */
    private fun handleError(errorCode: Int, errorMessage: String) {
        Log.e(TAG, "Error: $errorCode - $errorMessage")

        val recoveryAction = errorRecoveryManager.getRecoveryAction(errorCode)

        when (recoveryAction) {
            ErrorRecoveryManager.RecoveryAction.RESTART_IMMEDIATELY -> {
                scope.launch {
                    delay(100)
                    recognizer.restart()
                }
            }
            ErrorRecoveryManager.RecoveryAction.RETRY_WITH_BACKOFF -> {
                scope.launch {
                    delay(errorRecoveryManager.calculateBackoffDelay())
                    recognizer.restart()
                }
            }
            ErrorRecoveryManager.RecoveryAction.WAIT_AND_RETRY -> {
                scope.launch {
                    delay(1000)
                    recognizer.restart()
                }
            }
            ErrorRecoveryManager.RecoveryAction.ABORT -> {
                scope.launch {
                    _errorFlow.emit(SpeechError(
                        code = errorCode,
                        message = errorMessage,
                        isRecoverable = false,
                        suggestedAction = SpeechError.Action.LOG_AND_REPORT
                    ))
                }
            }
            else -> {
                scope.launch {
                    _errorFlow.emit(SpeechError(
                        code = errorCode,
                        message = errorMessage,
                        isRecoverable = true,
                        suggestedAction = SpeechError.Action.RETRY
                    ))
                }
            }
        }
    }

    /**
     * Handle mute command
     */
    private fun handleMuteCommand() {
        config.setVoiceSleeping(true)
        timeoutJob?.cancel()
        Log.i(TAG, "Voice muted")
    }

    /**
     * Handle unmute command
     */
    private fun handleUnmuteCommand() {
        config.setVoiceSleeping(false)
        config.setVoiceEnabled(true)
        lastExecutedCommandTime = System.currentTimeMillis()
        runTimeout()
        Log.i(TAG, "Voice unmuted")
    }

    /**
     * Handle silence check
     */
    private fun handleSilenceCheck(hypothesis: String?) {
        if (config.isDictationActive()) {
            if (hypothesis.isNullOrEmpty()) {
                if (silenceStartTime == 0L) {
                    silenceStartTime = System.currentTimeMillis()
                }
            } else {
                silenceStartTime = 0L
            }
        }
    }

    /**
     * Run voice timeout
     */
    private fun runTimeout() {
        val timeoutMinutes = config.getVoiceTimeoutMinutes()
        timeoutJob = scope.launch {
            while (config.isVoiceEnabled() && !config.isVoiceSleeping()) {
                delay(30000)
                val elapsed = (System.currentTimeMillis() - lastExecutedCommandTime) / 60000
                if (elapsed >= timeoutMinutes) {
                    config.setVoiceSleeping(true)
                    Log.i(TAG, "Voice timeout - entering sleep mode")
                    break
                }
            }
        }
    }

    // Engine information methods
    fun getEngineName(): String = ENGINE_NAME
    fun getEngineVersion(): String = ENGINE_VERSION
    fun requiresNetwork(): Boolean = true
    fun getMemoryUsage(): Int = 15

    fun supportsMode(mode: SpeechMode): Boolean = when (mode) {
        SpeechMode.STATIC_COMMAND, SpeechMode.DYNAMIC_COMMAND,
        SpeechMode.FREE_SPEECH, SpeechMode.DICTATION -> true
        else -> false
    }

    fun getSupportedLanguages(): List<String> = config.getSupportedLanguages()

    fun getPerformanceMetrics(): Map<String, Any> {
        val metrics = performanceMonitor.getMetrics()
        return mapOf(
            "engineVersion" to ENGINE_VERSION,
            "totalSessions" to metrics.totalRecognitions,
            "averageRecognitionTimeMs" to metrics.averageLatency,
            "successRatePercent" to "%.1f".format(metrics.successRate * 100),
            "performanceState" to metrics.performanceState.name
        )
    }

    fun getLearningStats(): Map<String, Any> {
        val stats = learningSystem.getStatistics()
        return mapOf(
            "learnedCommands" to stats.learnedCommandCount,
            "totalRecognitions" to stats.totalRecognitions,
            "successRate" to stats.successRate,
            "averageConfidence" to stats.averageConfidence
        )
    }

    /**
     * Destroy the engine.
     * Recognizer must be destroyed on the main thread before cancelling the coroutine scope.
     */
    fun destroy() {
        Log.d(TAG, "Destroying $ENGINE_NAME")

        timeoutJob?.cancel()
        mainHandler.removeCallbacks(silenceCheckRunnable)

        // Destroy recognizer synchronously before cancelling scope.
        // AndroidSTTRecognizer.destroy() is suspend (uses withContext(Main) internally).
        // runBlocking ensures cleanup completes before we proceed — no leaked scope.
        // This is safe from IO/background threads. From Main, Dispatchers.Main.immediate
        // avoids re-dispatch so withContext(Main) inside recognizer.destroy() won't deadlock.
        if (::recognizer.isInitialized) {
            try {
                val dispatcher = if (Looper.myLooper() == Looper.getMainLooper()) {
                    Dispatchers.Main.immediate
                } else {
                    Dispatchers.IO
                }
                kotlinx.coroutines.runBlocking(dispatcher) {
                    recognizer.destroy()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying recognizer: ${e.message}")
            }
        }

        errorRecoveryManager.destroy()
        learningSystem.destroy()
        performanceMonitor.destroy()
        commandCache.clear()

        isInitialized = false
        scope.cancel()

        Log.d(TAG, "$ENGINE_NAME destroyed")
    }
}
