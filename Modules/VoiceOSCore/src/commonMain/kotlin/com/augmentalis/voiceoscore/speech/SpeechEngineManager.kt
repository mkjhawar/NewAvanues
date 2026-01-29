/**
 * SpeechEngineManager.kt - Unified speech engine coordinator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-07
 *
 * Coordinates multiple speech recognition engines, handling:
 * - Engine lifecycle management
 * - Automatic fallback between engines
 * - State observation via StateFlow
 * - Command result emission via SharedFlow
 * - Command registration with active engine
 *
 * KMP migration of VoiceOSCore SpeechEngineManager.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.*
import com.augmentalis.voiceoscore.StaticCommandRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Manager state representing current speech recognition status.
 */
sealed class SpeechManagerState {
    /** Manager not initialized */
    data object Idle : SpeechManagerState()

    /** Initializing engine */
    data class Initializing(val engine: SpeechEngine) : SpeechManagerState()

    /** Ready to listen */
    data class Ready(val engine: SpeechEngine) : SpeechManagerState()

    /** Actively listening */
    data class Listening(val engine: SpeechEngine) : SpeechManagerState()

    /** Processing speech */
    data class Processing(val engine: SpeechEngine) : SpeechManagerState()

    /** Muted (not listening) */
    data class Muted(val engine: SpeechEngine) : SpeechManagerState()

    /** Error state */
    data class Error(val message: String, val engine: SpeechEngine?) : SpeechManagerState()

    val isListening: Boolean get() = this is Listening
    val isReady: Boolean get() = this is Ready || this is Muted
    val activeEngine: SpeechEngine? get() = when (this) {
        is Ready -> engine
        is Listening -> engine
        is Processing -> engine
        is Muted -> engine
        is Initializing -> engine
        else -> null
    }
}

/**
 * Recognized speech command event from speech engine.
 * Different from CommandModels.CommandEvent - this is specific to speech recognition.
 */
data class SpeechCommandEvent(
    /** Matched command phrase */
    val command: String,

    /** Recognition confidence (0.0 - 1.0) */
    val confidence: Float,

    /** Engine that recognized the command */
    val engine: SpeechEngine,

    /** Timestamp */
    val timestamp: Long = currentTimeMillis(),

    /** Whether command requires confirmation */
    val requiresConfirmation: Boolean = confidence < 0.8f
)

/**
 * SpeechEngineManager coordinates speech recognition across engines.
 *
 * ## Usage
 *
 * ```kotlin
 * val manager = SpeechEngineManager(factory)
 *
 * // Initialize with preferred engine
 * manager.initialize(SpeechEngine.VOSK, config)
 *
 * // Register commands
 * manager.updateCommands(listOf("go back", "scroll down"))
 *
 * // Start listening
 * manager.startListening()
 *
 * // Observe commands
 * manager.commands.collect { event ->
 *     handleCommand(event.command)
 * }
 *
 * // Observe state
 * manager.state.collect { state ->
 *     updateUI(state)
 * }
 * ```
 *
 * ## Engine Priority
 *
 * When requested engine fails, manager tries fallback engines:
 * 1. Requested engine
 * 2. VIVOKA (if available, best for command-word)
 * 3. ANDROID_STT (always available on Android)
 * 4. VOSK (offline fallback)
 */
class SpeechEngineManager(
    private val factory: ISpeechEngineFactory,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    // ═══════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════

    private val _state = MutableStateFlow<SpeechManagerState>(SpeechManagerState.Idle)
    val state: StateFlow<SpeechManagerState> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<SpeechCommandEvent>(replay = 0)
    val commands: SharedFlow<SpeechCommandEvent> = _commands.asSharedFlow()

    private val _errors = MutableSharedFlow<SpeechError>(replay = 0)
    val errors: SharedFlow<SpeechError> = _errors.asSharedFlow()

    @Volatile
    private var activeEngine: ISpeechEngine? = null
    @Volatile
    private var activeEngineType: SpeechEngine? = null
    @Volatile
    private var isMuted = false
    @Volatile
    private var currentCommands: List<String> = emptyList()
    @Volatile
    private var currentConfig: SpeechConfig = SpeechConfig()

    // Track engine result collection job
    private var resultCollectionJob: Job? = null

    // ═══════════════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Initialize speech recognition with specified engine.
     *
     * @param engine Preferred speech engine
     * @param config Speech configuration
     * @param wrapWithCommandDetection Whether to wrap continuous engines with CommandWordDetector
     * @return Result indicating success or failure
     */
    suspend fun initialize(
        engine: SpeechEngine = factory.getRecommendedEngine(),
        config: SpeechConfig = SpeechConfig(),
        wrapWithCommandDetection: Boolean = true
    ): Result<Unit> {
        // Destroy existing engine if any
        activeEngine?.destroy()
        resultCollectionJob?.cancel()

        _state.value = SpeechManagerState.Initializing(engine)
        currentConfig = config

        // Try requested engine, then fallbacks
        val enginestoTry = listOf(engine) + getFallbackEngines(engine)

        for (engineType in enginestoTry) {
            if (!factory.isEngineAvailable(engineType)) continue

            val result = tryInitializeEngine(engineType, config, wrapWithCommandDetection)
            if (result.isSuccess) {
                activeEngineType = engineType
                _state.value = SpeechManagerState.Ready(engineType)

                // Register static commands by default
                updateCommands(StaticCommandRegistry.allPhrases())

                return Result.success(Unit)
            }
        }

        _state.value = SpeechManagerState.Error(
            "Failed to initialize any speech engine",
            engine
        )
        return Result.failure(IllegalStateException("No speech engine available"))
    }

    private suspend fun tryInitializeEngine(
        engineType: SpeechEngine,
        config: SpeechConfig,
        wrapWithCommandDetection: Boolean
    ): Result<Unit> {
        val createResult = factory.createEngine(engineType)
        if (createResult.isFailure) {
            return Result.failure(createResult.exceptionOrNull() ?: IllegalStateException("Unknown engine creation error"))
        }

        var engine = createResult.getOrThrow()

        // Wrap continuous engines with command detection
        if (wrapWithCommandDetection && isContiuousEngine(engineType)) {
            engine = engine.withCommandDetection(
                confidenceThreshold = config.confidenceThreshold
            )
        }

        val initResult = engine.initialize(config)
        if (initResult.isFailure) {
            engine.destroy()
            return initResult
        }

        activeEngine = engine

        // Start collecting results
        startResultCollection()

        return Result.success(Unit)
    }

    private fun isContiuousEngine(engine: SpeechEngine): Boolean {
        return engine in listOf(
            SpeechEngine.VOSK,
            SpeechEngine.GOOGLE_CLOUD,
            SpeechEngine.AZURE,
            SpeechEngine.ANDROID_STT,
            SpeechEngine.WHISPER,
            SpeechEngine.APPLE_SPEECH
        )
    }

    private fun getFallbackEngines(primary: SpeechEngine): List<SpeechEngine> {
        return listOf(
            SpeechEngine.VIVOKA,     // Best for command-word
            SpeechEngine.ANDROID_STT, // Always available on Android
            SpeechEngine.VOSK        // Offline fallback
        ).filter { it != primary }
    }

    private fun startResultCollection() {
        val engine = activeEngine ?: return
        val engineType = activeEngineType ?: return

        resultCollectionJob = scope.launch {
            launch {
                engine.results.collect { result ->
                    if (result.isFinal && !isMuted) {
                        _commands.emit(
                            SpeechCommandEvent(
                                command = result.text,
                                confidence = result.confidence,
                                engine = engineType,
                                timestamp = result.timestamp,
                                requiresConfirmation = result.confidence < currentConfig.confidenceThreshold
                            )
                        )
                    }
                }
            }

            launch {
                engine.errors.collect { error ->
                    _errors.emit(error)
                }
            }

            launch {
                engine.state.collect { engineState ->
                    updateManagerState(engineState)
                }
            }
        }
    }

    private fun updateManagerState(engineState: EngineState) {
        val engineType = activeEngineType ?: return

        _state.value = when (engineState) {
            is EngineState.Listening -> {
                if (isMuted) SpeechManagerState.Muted(engineType)
                else SpeechManagerState.Listening(engineType)
            }
            is EngineState.Processing -> SpeechManagerState.Processing(engineType)
            is EngineState.Ready -> {
                if (isMuted) SpeechManagerState.Muted(engineType)
                else SpeechManagerState.Ready(engineType)
            }
            is EngineState.Error -> SpeechManagerState.Error(engineState.message, engineType)
            else -> _state.value
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Listening Control
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Start listening for voice commands.
     */
    suspend fun startListening(): Result<Unit> {
        val engine = activeEngine ?: return Result.failure(
            IllegalStateException("Engine not initialized")
        )

        if (isMuted) {
            return Result.failure(IllegalStateException("Engine is muted"))
        }

        return engine.startListening()
    }

    /**
     * Stop listening.
     */
    suspend fun stopListening() {
        activeEngine?.stopListening()
    }

    /**
     * Mute voice recognition (stop listening, remember state).
     */
    suspend fun mute() {
        isMuted = true
        stopListening()
        activeEngineType?.let {
            _state.value = SpeechManagerState.Muted(it)
        }
    }

    /**
     * Unmute and resume listening.
     */
    suspend fun unmute(): Result<Unit> {
        isMuted = false
        return startListening()
    }

    /**
     * Toggle mute state.
     */
    suspend fun toggleMute(): Result<Unit> {
        return if (isMuted) unmute() else { mute(); Result.success(Unit) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Command Management
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update commands for recognition.
     *
     * @param commands List of command phrases
     */
    suspend fun updateCommands(commands: List<String>): Result<Unit> {
        currentCommands = commands
        return activeEngine?.updateCommands(commands)
            ?: Result.failure(IllegalStateException("Engine not initialized"))
    }

    /**
     * Add commands without replacing existing.
     */
    suspend fun addCommands(commands: List<String>): Result<Unit> {
        return updateCommands(currentCommands + commands)
    }

    /**
     * Get current registered commands.
     */
    fun getCurrentCommands(): List<String> = currentCommands.toList()

    // ═══════════════════════════════════════════════════════════════════════
    // Engine Management
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Switch to a different speech engine.
     */
    suspend fun switchEngine(engine: SpeechEngine): Result<Unit> {
        val wasListening = state.value.isListening
        stopListening()

        val result = initialize(engine, currentConfig)

        if (result.isSuccess) {
            // Restore commands
            if (currentCommands.isNotEmpty()) {
                updateCommands(currentCommands)
            }
            // Resume listening if was listening
            if (wasListening && !isMuted) {
                startListening()
            }
        }

        return result
    }

    /**
     * Get currently active engine type.
     */
    fun getActiveEngine(): SpeechEngine? = activeEngineType

    /**
     * Get available engines.
     */
    fun getAvailableEngines(): List<SpeechEngine> = factory.getAvailableEngines()

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Destroy manager and release resources.
     */
    suspend fun destroy() {
        resultCollectionJob?.cancel()
        activeEngine?.destroy()
        activeEngine = null
        activeEngineType = null
        _state.value = SpeechManagerState.Idle
    }

    /**
     * Check if manager is initialized.
     */
    fun isInitialized(): Boolean = activeEngine?.isInitialized() == true
}
