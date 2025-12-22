/**
 * SpeechEngineManager.kt - EngineManager for speech recognition
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-01-28
 */
package com.augmentalis.voiceoscore.accessibility.speech

// GoogleCloudEngine temporarily disabled
import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.accessibility.utils.ConditionalLogger
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.voiceos.speech.api.SpeechListenerManager
// import com.augmentalis.voiceos.speech.engines.android.AndroidSTTEngine  // DISABLED: User wants only VivokaEngine
import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine  // ENABLED: Primary engine with learning stubbed
// import com.augmentalis.voiceos.speech.engines.vosk.VoskEngine  // DISABLED: Learning dependency
// import com.augmentalis.voiceos.speech.engines.whisper.WhisperEngine  // DISABLED: Learning dependency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


/**
 * SpeechEngineManager for speech recognition functionality
 */
class SpeechEngineManager(private val context: Context) {

    private val _speechState = MutableStateFlow(SpeechState())
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    // Command events flow - separate from state for proper event-based architecture
    private val _commandEvents = MutableSharedFlow<CommandEvent>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val commandEvents: SharedFlow<CommandEvent> = _commandEvents.asSharedFlow()

    private var currentEngine: Any? = null
    private val isInitializing = AtomicBoolean(false)

    /**
     * Single mutex protecting all engine state operations
     *
     * Replaces previous 4 mutexes (engineMutex, initializationMutex,
     * engineCleanupMutex, engineSwitchingMutex) to prevent deadlocks
     * from nested locking and simplify synchronization logic.
     *
     * Protects:
     * - currentEngine reference changes
     * - Engine initialization state
     * - Engine cleanup operations
     * - Engine switching operations
     */
    private val stateMutex = Mutex()

    // Additional race condition protections
    private val isDestroying = AtomicBoolean(false)
    private val lastInitializationAttempt = AtomicLong(0L)
    private val initializationAttempts = AtomicLong(0L)

    // Engine state tracking for better error recovery
    private var lastSuccessfulEngine: SpeechEngine? = null
    private var engineInitializationHistory = mutableMapOf<SpeechEngine, Long>()


    private val listenerManager = SpeechListenerManager()
    private var currentConfiguration = SpeechConfigurationData()
    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        setupListeners()
    }

    /**
     * Setup speech recognition listeners
     */
    private fun setupListeners() {
        listenerManager.onResult = { result ->
            Log.d(TAG, "SPEECH_TEST: onResult result = $result")
            handleSpeechResult(result)
        }

        listenerManager.onError = { error, code ->
            _speechState.value = _speechState.value.copy(
                errorMessage = "Error ($code): $error",
                isListening = false,
                engineStatus = "Error occurred"
            )
        }

        listenerManager.onStateChange = { state, message ->
            _speechState.value = _speechState.value.copy(
                engineStatus = if (message != null) "$state: $message" else state
            )
        }
    }

    /**
     * Handle speech recognition results
     *
     * ARCHITECTURE: Separates state updates from event emission
     * - StateFlow: Updated for monitoring/UI (engine state)
     * - SharedFlow: Emits command events for processing (event-based)
     */
    private fun handleSpeechResult(result: RecognitionResult) {
        Log.d(TAG, "SPEECH_TEST: handleSpeechResult result text= ${result.text}, confidence = ${result.confidence}")
        val currentText = result.text
        val confidence = result.confidence

        // Update state for monitoring/UI
        _speechState.value = _speechState.value.copy(
            currentTranscript = "",
            fullTranscript = currentText,
            confidence = confidence,
            errorMessage = null
        )

        // Emit command event for processing (guarantees delivery to all collectors)
        engineScope.launch {
            _commandEvents.emit(CommandEvent(
                command = currentText,
                confidence = confidence,
                timestamp = System.currentTimeMillis()
            ))
            ConditionalLogger.d(TAG, "SPEECH_TEST: Command event emitted - command='$currentText', confidence=$confidence")
        }
    }

    /**
     * Initialize speech engine with enhanced thread-safe race condition prevention
     * ENHANCED FIX: Prevents multiple concurrent initialization attempts with comprehensive protection
     */
    fun initializeEngine(engine: SpeechEngine) {
        engineScope.launch {
            // Check if system is being destroyed
            if (isDestroying.get()) {
                Log.w(TAG, "Cannot initialize ${engine.name} - EngineManager is being destroyed")
                return@launch
            }

            val currentTime = System.currentTimeMillis()
            lastInitializationAttempt.set(currentTime)
            initializationAttempts.incrementAndGet()

            // Prevent too frequent initialization attempts
            val lastAttempt = engineInitializationHistory[engine] ?: 0L
            if (currentTime - lastAttempt < 1000L) { // 1 second minimum between attempts
                Log.w(TAG, "Initialization attempt too frequent for ${engine.name}, waiting...")
                delay(1000L - (currentTime - lastAttempt))
            }

            // Prevent concurrent initialization attempts
            if (!isInitializing.compareAndSet(false, true)) {
                Log.w(TAG, "Initialization already in progress for ${engine.name}")
                return@launch
            }

            stateMutex.withLock {
                try {
                    Log.i(
                        TAG,
                        "Starting enhanced thread-safe initialization of ${engine.name} (attempt #${initializationAttempts.get()})"
                    )

                    engineInitializationHistory[engine] = currentTime

                    // Enhanced cleanup of previous engine
                    cleanupPreviousEngine()

                    _speechState.value = _speechState.value.copy(
                        selectedEngine = engine,
                        engineStatus = "Initializing ${engine.name}...",
                        errorMessage = null,
                        isInitialized = false
                    )

                    // Create and initialize engine with proper error handling
                    val newEngine = createEngineInstance(engine)
                    val initSuccess = initializeEngineInstanceWithRetry(newEngine, engine)

                    if (initSuccess) {
                        // Update engine reference (already protected by stateMutex)
                        currentEngine = newEngine
                        lastSuccessfulEngine = engine

                        setupEngineListeners(newEngine)

                        _speechState.value = _speechState.value.copy(
                            isInitialized = true,
                            engineStatus = "${engine.name} ready",
                            errorMessage = null
                        )

                        Log.i(TAG, "${engine.name} initialized successfully")
                    } else {
                        // Attempt fallback to last successful engine if available
                        handleInitializationFailure(engine)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Exception during ${engine.name} initialization", e)
                    handleInitializationException(engine, e)
                } finally {
                    isInitializing.set(false)
                }
            }
        }
    }

    /**
     * Enhanced cleanup of previous engine with proper resource management
     *
     * NOTE: Caller must hold stateMutex lock
     */
    private suspend fun cleanupPreviousEngine() {
        try {
            Log.d(TAG, "Enhanced cleanup of previous engine")

            // Stop any ongoing operations
            stopListening()

            // Clean up current engine if exists
            val engine = currentEngine
            if (engine != null) {
                Log.d(TAG, "Cleaning up engine: ${engine::class.simpleName}")

                // Engine-specific cleanup
                try {
                    when (engine) {
                        // is AndroidSTTEngine -> {
                        //     // AndroidSTT cleanup is handled internally
                        // }  // DISABLED: User wants only VivokaEngine

                        // is VoskEngine -> {
                        //     // VoskEngine cleanup
                        //     engine.destroy()
                        // }  // DISABLED: Learning dependency

                        is VivokaEngine -> {
                            // VivokaEngine cleanup
                            engine.destroy()
                        }

                        // is WhisperEngine -> {
                        //     // WhisperEngine cleanup
                        //     engine.destroy()
                        // }  // DISABLED: Learning dependency

                        else -> {
                            Log.w(TAG, "Unknown engine type for cleanup: ${engine::class.simpleName}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Engine cleanup failed (non-critical): ${e.message}")
                }
            }

            currentEngine = null

            // Small delay to ensure cleanup is complete
                delay(200)

            } catch (e: Exception) {
                Log.e(TAG, "Enhanced cleanup failed", e)
                // Continue with initialization even if cleanup failed
            }
        }
    }

    /**
     * Initialize engine instance with retry logic
     */
    private suspend fun initializeEngineInstanceWithRetry(engineInstance: Any, engineType: SpeechEngine): Boolean {
        val maxRetries = 1
        var lastError: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "Engine initialization attempt ${attempt + 1}/$maxRetries for ${engineType.name}")

                val result = initializeEngineInstance(engineInstance, engineType)
                if (result) {
                    Log.i(TAG, "${engineType.name} initialized successfully on attempt ${attempt + 1}")
                    return true
                }
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Engine initialization attempt ${attempt + 1} failed: ${e.message}")

                if (attempt < maxRetries - 1) {
                    delay(1000L * (attempt + 1)) // Progressive delay
                }
            }
        }

        Log.e(TAG, "All initialization attempts failed for ${engineType.name}", lastError)
        return false
    }

    /**
     * Handle initialization failure with fallback strategy
     */
    private suspend fun handleInitializationFailure(engine: SpeechEngine) {
        Log.w(TAG, "Handling initialization failure for ${engine.name}")

        val fallbackEngine = lastSuccessfulEngine
        if (fallbackEngine != null && fallbackEngine != engine) {
            Log.i(TAG, "Attempting fallback to last successful engine: ${fallbackEngine.name}")
            _speechState.value = _speechState.value.copy(
                errorMessage = "Failed to initialize ${engine.name}, falling back to ${fallbackEngine.name}",
                isInitialized = false,
                engineStatus = "Falling back..."
            )

            // Small delay before fallback attempt
            delay(500)

            // Recursive call to initialize fallback (but prevent infinite loops)
            if (initializationAttempts.get() < 5L) {
                initializeEngine(fallbackEngine)
            } else {
                Log.e(TAG, "Too many initialization attempts, stopping fallback")
                _speechState.value = _speechState.value.copy(
                    errorMessage = "Multiple initialization failures - please restart the app",
                    isInitialized = false,
                    engineStatus = "Multiple failures"
                )
            }
        } else {
            _speechState.value = _speechState.value.copy(
                errorMessage = "Failed to initialize ${engine.name}",
                isInitialized = false,
                engineStatus = "Initialization failed"
            )
        }
    }

    /**
     * Handle initialization exceptions with detailed logging
     */
    private fun handleInitializationException(engine: SpeechEngine, exception: Exception) {
        Log.e(TAG, "Exception during ${engine.name} initialization", exception)

        val errorMessage = when {
            exception.message?.contains("Cannot call 'Vsdk.init' multiple times") == true ->
                "Vivoka SDK already initialized - please restart the app"

            exception.message?.contains("timeout") == true ->
                "Engine initialization timed out - please try again"

            exception.message?.contains("permission") == true ->
                "Permissions required for ${engine.name}"

            else ->
                "Initialization error: ${exception.message}"
        }

        _speechState.value = _speechState.value.copy(
            errorMessage = errorMessage,
            isInitialized = false,
            engineStatus = "Initialization failed"
        )
    }

    /**
     * Create engine instance based on selection
     */
    private fun createEngineInstance(engine: SpeechEngine): Any {
        return when (engine) {
            // SpeechEngine.ANDROID_STT -> AndroidSTTEngine(context = context)  // DISABLED: User wants only VivokaEngine
            // SpeechEngine.VOSK -> VoskEngine(context = context)  // DISABLED: Learning dependency
            SpeechEngine.VIVOKA -> VivokaEngine(context = context)
            // SpeechEngine.GOOGLE_CLOUD -> {  // DISABLED: Learning dependency
            //     Log.w(TAG, "GoogleCloudEngine disabled, using Android STT fallback")
            //     AndroidSTTEngine(context = context)
            // }

            // SpeechEngine.WHISPER -> WhisperEngine(context = context)  // DISABLED: Learning dependency
            else -> {
                Log.w(TAG, "Unknown engine $engine, defaulting to VivokaEngine")
                VivokaEngine(context = context)
            }
        }
    }

    /**
     * Initialize engine instance with proper async handling
     */
    private suspend fun initializeEngineInstance(engineInstance: Any, engineType: SpeechEngine): Boolean {
        return try {
            val config = createConfig(engineType)

            when (engineInstance) {
                // is AndroidSTTEngine -> {
                //     engineInstance.initialize(context = context, config)
                // }  // DISABLED: User wants only VivokaEngine

                // is VoskEngine -> {
                //     engineInstance.initialize(config)
                // }  // DISABLED: Learning dependency

                is VivokaEngine -> {
                    engineInstance.initialize(config)
                }

                // is WhisperEngine -> {
                //     engineInstance.initialize(config)
                // }  // DISABLED: Learning dependency

                else -> {
                    Log.e(TAG, "Unknown engine type: $engineInstance")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Engine initialization failed", e)
            false
        }
    }

    /**
     * Setup engine listeners safely
     */
    private fun setupEngineListeners(engineInstance: Any) {
        when (engineInstance) {
            // is AndroidSTTEngine -> {
            //     engineInstance.setResultListener { result ->
            //         listenerManager.onResult?.invoke(result)
            //     }
            //     engineInstance.setErrorListener { error, code ->
            //         listenerManager.onError?.invoke(error, code)
            //     }
            // }  // DISABLED: User wants only VivokaEngine

            // is VoskEngine -> {
            //     engineInstance.setResultListener { result ->
            //         listenerManager.onResult?.invoke(result)
            //     }
            //     engineInstance.setErrorListener { error, code ->
            //         listenerManager.onError?.invoke(error, code)
            //     }
            // }  // DISABLED: Learning dependency

            is VivokaEngine -> {
                engineInstance.setResultListener { result ->
                    Log.d(TAG, "SPEECH_TEST: setResultListener result = $result")
                    listenerManager.onResult?.invoke(result)
                }
                engineInstance.setErrorListener { error, code ->
                    listenerManager.onError?.invoke(error, code)
                }
            }

            // is WhisperEngine -> {
            //     engineInstance.setResultListener { result ->
            //         listenerManager.onResult?.invoke(result)
            //     }
            //     engineInstance.setErrorListener { error, code ->
            //         listenerManager.onError?.invoke(error, code)
            //     }
            // }  // DISABLED: Learning dependency

            else -> {
                Log.w(TAG, "Unknown engine type for listener setup: $engineInstance")
            }
        }
    }

    /**
     * Create configuration for engine
     */
    private fun createConfig(@Suppress("UNUSED_PARAMETER") engine: SpeechEngine): SpeechConfig {
        // Engine parameter will be used when engine-specific config is needed
        return SpeechConfig(
            language = currentConfiguration.language,
            mode = currentConfiguration.mode,
            enableVAD = currentConfiguration.enableVAD,
            confidenceThreshold = currentConfiguration.confidenceThreshold,
            maxRecordingDuration = currentConfiguration.maxRecordingDuration,
            timeoutDuration = currentConfiguration.timeoutDuration,
            enableProfanityFilter = currentConfiguration.enableProfanityFilter,
            engine = engine
        )
    }

    /**
     * Update configuration settings
     */
    fun updateConfiguration(config: SpeechConfigurationData) {
        currentConfiguration = config
        // Re-initialize current engine with new config if needed
        currentEngine?.let {
            _speechState.value.selectedEngine.let { engine ->
                initializeEngine(engine)
            }
        }
    }

    /**
     * Start listening with thread safety
     * CRITICAL FIX: Prevents concurrent access to currentEngine
     */
    fun startListening() {
        if (!_speechState.value.isInitialized) {
            _speechState.value = _speechState.value.copy(
                errorMessage = "Engine not initialized"
            )
            return
        }

        engineScope.launch {
            stateMutex.withLock {
                try {
                    val engine = currentEngine
                    if (engine == null) {
                        _speechState.value = _speechState.value.copy(
                            errorMessage = "No engine available",
                            isListening = false
                        )
                        return@withLock
                    }

                    Log.d(TAG, "Starting listening with engine: ${engine::class.simpleName}")

                    when (engine) {
                        // is AndroidSTTEngine -> engine.startListening(currentConfiguration.mode)  // DISABLED: User wants only VivokaEngine
                        // is VoskEngine -> engine.startListening()  // DISABLED: Learning dependency
                        is VivokaEngine -> {
                            engine.startListening()
                            engine.setDynamicCommands(STATIC_COMMANDS)
                        }
                        // GoogleCloudEngine disabled - handled by fallback
                        // is WhisperEngine -> engine.startListening()  // DISABLED: Learning dependency
                        else -> {
                            _speechState.value = _speechState.value.copy(
                                errorMessage = "Engine type not supported: ${engine::class.simpleName}",
                                isListening = false
                            )
                            return@withLock
                        }
                    }

                    _speechState.value = _speechState.value.copy(
                        isListening = true,
                        engineStatus = "Listening...",
                        errorMessage = null
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start listening", e)
                    _speechState.value = _speechState.value.copy(
                        errorMessage = "Failed to start: ${e.message}",
                        isListening = false
                    )
                }
            }
        }
    }

    // FIX (2025-12-11): Made async to prevent blocking caller (100-500ms operation)
    // Root cause: Synchronous speech engine update blocked caller thread
    // Solution: Launch in engineScope with Dispatchers.IO to run in background
    suspend fun updateCommands(commands: List<String>) = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            try {
                when (currentEngine) {
                    is VivokaEngine -> {
                        Log.d(TAG, "SPEECH_TEST: updateCommands commands (${commands.size} total) = $commands")
                        (currentEngine as VivokaEngine).setDynamicCommands(commands)
                    }
                    else -> {
                        Log.w(TAG, "updateCommands called but no engine is active")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating commands", e)
            }
        }
    }

    /**
     * Stop listening with thread safety
     * CRITICAL FIX: Thread-safe access to currentEngine
     */
    fun stopListening() {
        engineScope.launch {
            stateMutex.withLock {
                try {
                    val engine = currentEngine
                    if (engine != null) {
                        Log.d(TAG, "Stopping listening with engine: ${engine::class.simpleName}")

                        when (engine) {
                            // is AndroidSTTEngine -> engine.stopListening()  // DISABLED: User wants only VivokaEngine
                            // is VoskEngine -> engine.stopListening()  // DISABLED: Learning dependency
                            is VivokaEngine -> engine.stopListening()
                            // GoogleCloudEngine disabled - handled by fallback
                            // is WhisperEngine -> engine.stopListening()  // DISABLED: Learning dependency
                            else -> {
                                Log.w(TAG, "Unknown engine type for stop: ${engine::class.simpleName}")
                            }
                        }
                    }

                    _speechState.value = _speechState.value.copy(
                        isListening = false,
                        engineStatus = "Stopped"
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping listening", e)
                    _speechState.value = _speechState.value.copy(
                        errorMessage = "Error stopping: ${e.message}",
                        isListening = false
                    )
                }
            }
        }
    }

    /**
     * Clear transcript
     */
    fun clearTranscript() {
        _speechState.value = _speechState.value.copy(
            currentTranscript = "",
            fullTranscript = "",
            confidence = 0f
        )
    }

    /**
     * Clean up resources with enhanced protection against race conditions
     * ENHANCED FIX: Comprehensive cleanup with destruction state tracking
     */
    fun onDestroy() {
        // Set destruction flag to prevent new operations
        isDestroying.set(true)

        engineScope.launch {
            try {
                Log.i(TAG, "Enhanced cleanup starting...")

                // Stop all operations first
                stopListening()

                // Wait for any ongoing initialization to complete (with timeout)
                val maxWaitTime = 3000L // 3 seconds
                val startTime = System.currentTimeMillis()

                while (isInitializing.get() && (System.currentTimeMillis() - startTime) < maxWaitTime) {
                    delay(100)
                }

                if (isInitializing.get()) {
                    Log.w(TAG, "Initialization still in progress during cleanup, forcing cleanup")
                }

                // Enhanced cleanup (requires stateMutex)
                stateMutex.withLock {
                    cleanupPreviousEngine()

                    // Clear state
                    engineInitializationHistory.clear()
                    lastSuccessfulEngine = null
                }

                Log.i(TAG, "Enhanced cleanup completed")

            } catch (e: Exception) {
                Log.e(TAG, "Error during enhanced cleanup", e)
            }
        }
    }

    /**
     * Get initialization diagnostics for debugging
     */
    fun getInitializationDiagnostics(): Map<String, Any> {
        return mapOf(
            "total_attempts" to initializationAttempts.get(),
            "last_attempt" to lastInitializationAttempt.get(),
            "is_initializing" to isInitializing.get(),
            "is_destroying" to isDestroying.get(),
            "last_successful_engine" to (lastSuccessfulEngine?.name ?: "none"),
            "engine_history" to engineInitializationHistory.mapKeys { it.key.name },
            "current_engine" to (currentEngine?.javaClass?.simpleName ?: "none")
        )
    }

    companion object {

        private val STATIC_COMMANDS = listOf(
            // Navigation
            "Go back",
            "Navigate back",
            "Back",
            "Previous screen",
            "Go home",
            "Home",
            "Navigate home",
            "Open Home",

            // Settings
            "Open Settings",
            "Settings",
            "Show Settings",
            "System Settings",
            "Device Settings",

            // App Control
            "Open App Drawer",
            "Show Recent Apps",
            "Close App",
            "Exit App",
            "Open Browser",
            "Open Camera",
            "Open Gallery",
            "Open Contacts",

            // Media
            "Play Music",
            "Pause Music",
            "Stop Music",
            "Next Song",
            "Previous Song",
            "Increase Volume",
            "Lower Volume",

            // Calls & Messages
            "Call Contact",
            "Dial Number",
            "Send Message",
            "Open Messages",
            "Open Phone",
            "Answer Call",
            "Reject Call",

            // Utilities
            "Open Calculator",
            "Open Calendar",
            "Show Notifications",
            "Clear Notifications",
            "Take Screenshot",
            "Turn on Flashlight",
            "Turn off Flashlight",

            //
            "mute voice",
            "wake up voice",
            "dictation",
            "end dictation"
        )
        private const val TAG = "SpeechEngineManager"
    }

}

data class SpeechState(
    val isListening: Boolean = false,
    val selectedEngine: SpeechEngine = SpeechEngine.ANDROID_STT,
    val currentTranscript: String = "",
    val fullTranscript: String = "",
    val errorMessage: String? = null,
    val isInitialized: Boolean = false,
    val engineStatus: String = "Not initialized",
    val confidence: Float = 0f
) {
    override fun toString(): String {
        return "SpeechState(isListening=$isListening, selectedEngine=$selectedEngine, currentTranscript='$currentTranscript', fullTranscript='$fullTranscript', errorMessage=$errorMessage, isInitialized=$isInitialized, engineStatus='$engineStatus', confidence=$confidence)"
    }
}

data class SpeechConfigurationData(
    val language: String = "en-US",
    val mode: SpeechMode = SpeechMode.DYNAMIC_COMMAND,
    val enableVAD: Boolean = true,
    val confidenceThreshold: Float = 4000F,
    val maxRecordingDuration: Long = 30000,
    val timeoutDuration: Long = 5000,
    val enableProfanityFilter: Boolean = false
) {
    override fun toString(): String {
        return "SpeechConfigurationData(language='$language', mode=$mode, enableVAD=$enableVAD, confidenceThreshold=$confidenceThreshold, maxRecordingDuration=$maxRecordingDuration, timeoutDuration=$timeoutDuration, enableProfanityFilter=$enableProfanityFilter)"
    }
}

/**
 * Command event emitted when speech recognition produces a result
 *
 * ARCHITECTURE NOTE: Commands are events (discrete occurrences), not state.
 * Using SharedFlow ensures every command triggers collection, even if
 * consecutive commands have identical text.
 *
 * @property command The recognized command text
 * @property confidence Confidence score (0.0 to 1.0)
 * @property timestamp When the command was recognized (millis since epoch)
 */
data class CommandEvent(
    val command: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)