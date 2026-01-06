/**
 * VoiceOSCoreNG.kt - Main facade for VoiceOSCoreNG
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Main entry point and facade for VoiceOSCoreNG functionality.
 * Provides unified access to handlers, speech engines, and command processing.
 */
package com.augmentalis.voiceoscoreng

import com.augmentalis.voiceoscoreng.avu.QuantizedCommand
import com.augmentalis.voiceoscoreng.handlers.*
import com.augmentalis.voiceoscoreng.managers.*
import com.augmentalis.voiceoscoreng.repository.RepositoryProvider
import com.augmentalis.voiceoscoreng.speech.*
import kotlinx.coroutines.flow.*

/**
 * Main facade for VoiceOSCoreNG.
 *
 * Usage:
 * ```kotlin
 * // Initialize
 * val core = VoiceOSCoreNG.Builder()
 *     .withHandlerFactory(myHandlerFactory)
 *     .withSpeechEngine(SpeechEngine.ANDROID_STT)
 *     .build()
 *
 * core.initialize()
 *
 * // Process commands
 * val result = core.processCommand("scroll down")
 *
 * // Cleanup
 * core.dispose()
 * ```
 */
class VoiceOSCoreNG private constructor(
    private val handlerFactory: HandlerFactory,
    private val speechEngineFactory: ISpeechEngineFactory,
    private val configuration: ServiceConfiguration
) {
    // Coordinator manages handler execution
    private val coordinator = ActionCoordinator()

    // State manager tracks service lifecycle
    private val stateManager = ServiceStateManager()

    // Current speech engine
    private var speechEngine: ISpeechEngine? = null

    /**
     * Service state flow.
     */
    val state: StateFlow<ServiceState> = stateManager.state

    /**
     * Command execution results flow.
     */
    val commandResults: SharedFlow<CommandResult> = coordinator.results

    /**
     * Speech recognition results flow.
     */
    val speechResults: SharedFlow<SpeechResult>
        get() = speechEngine?.results ?: MutableSharedFlow()

    /**
     * Initialize the core.
     */
    suspend fun initialize() {
        stateManager.transition(ServiceState.Initializing(0f, "Creating handlers"))

        try {
            // Create handlers using factory
            val handlers = handlerFactory.createHandlers()

            stateManager.transition(ServiceState.Initializing(0.3f, "Registering handlers"))

            // Initialize coordinator with handlers
            coordinator.initialize(handlers)

            stateManager.transition(ServiceState.Initializing(0.6f, "Initializing speech engine"))

            // Create speech engine if configured
            if (configuration.autoStartListening) {
                val engineResult = speechEngineFactory.createEngine(
                    SpeechEngine.valueOf(configuration.speechEngine)
                )
                if (engineResult.isSuccess) {
                    speechEngine = engineResult.getOrNull()
                    val config = SpeechConfig(
                        language = configuration.voiceLanguage,
                        confidenceThreshold = configuration.confidenceThreshold,
                        enableWakeWord = configuration.enableWakeWord,
                        wakeWord = configuration.wakeWord
                    )
                    speechEngine?.initialize(config)
                }
            }

            stateManager.transition(
                ServiceState.Ready(
                    speechEngineActive = speechEngine != null,
                    handlerCount = handlers.size
                )
            )
        } catch (e: Exception) {
            stateManager.transition(ServiceState.Error(e.message ?: "Unknown error", recoverable = true))
            throw e
        }
    }

    /**
     * Process a voice command string.
     *
     * @param text The voice command text
     * @param confidence Confidence level (0-1)
     * @return HandlerResult from execution
     */
    suspend fun processCommand(text: String, confidence: Float = 1.0f): HandlerResult {
        if (!stateManager.canProcessCommands()) {
            return HandlerResult.failure("Service not ready to process commands")
        }

        stateManager.transition(ServiceState.Processing(text, confidence))

        val result = coordinator.processVoiceCommand(text, confidence)

        // Return to ready/listening state
        if (speechEngine != null && speechEngine?.isRecognizing() == true) {
            stateManager.transition(
                ServiceState.Listening(
                    speechEngine = configuration.speechEngine,
                    wakeWordEnabled = configuration.enableWakeWord
                )
            )
        } else {
            stateManager.transition(
                ServiceState.Ready(
                    speechEngineActive = speechEngine != null,
                    handlerCount = coordinator.getAllSupportedActions().size
                )
            )
        }

        return result
    }

    /**
     * Process a quantized command.
     */
    suspend fun processCommand(command: QuantizedCommand): HandlerResult {
        if (!stateManager.canProcessCommands()) {
            return HandlerResult.failure("Service not ready to process commands")
        }

        stateManager.transition(ServiceState.Processing(command.phrase, command.confidence))

        val result = coordinator.processCommand(command)

        // Return to ready state
        stateManager.transition(
            ServiceState.Ready(
                speechEngineActive = speechEngine != null,
                handlerCount = coordinator.getAllSupportedActions().size
            )
        )

        return result
    }

    /**
     * Start listening for voice commands.
     */
    suspend fun startListening(): Result<Unit> {
        val engine = speechEngine ?: return Result.failure(
            IllegalStateException("No speech engine configured")
        )

        val result = engine.startListening()
        if (result.isSuccess) {
            stateManager.transition(
                ServiceState.Listening(
                    speechEngine = configuration.speechEngine,
                    wakeWordEnabled = configuration.enableWakeWord
                )
            )
        }

        return result
    }

    /**
     * Stop listening for voice commands.
     */
    suspend fun stopListening() {
        speechEngine?.stopListening()
        stateManager.transition(
            ServiceState.Ready(
                speechEngineActive = speechEngine != null,
                handlerCount = coordinator.getAllSupportedActions().size
            )
        )
    }

    /**
     * Check if any handler can handle the command.
     */
    suspend fun canHandle(command: String): Boolean {
        return coordinator.canHandle(command)
    }

    /**
     * Get all supported actions.
     */
    suspend fun getAllSupportedActions(): List<String> {
        return coordinator.getAllSupportedActions()
    }

    /**
     * Get metrics summary.
     */
    fun getMetricsSummary(): MetricsSummary {
        return coordinator.getMetricsSummary()
    }

    /**
     * Get debug information.
     */
    suspend fun getDebugInfo(): String {
        return coordinator.getDebugInfo()
    }

    /**
     * Dispose all resources.
     */
    suspend fun dispose() {
        stateManager.transition(ServiceState.Stopping)

        speechEngine?.destroy()
        coordinator.dispose()

        stateManager.transition(ServiceState.Stopped)
    }

    /**
     * Builder for VoiceOSCoreNG.
     */
    class Builder {
        private var handlerFactory: HandlerFactory? = null
        private var speechEngineFactory: ISpeechEngineFactory? = null
        private var configuration: ServiceConfiguration = ServiceConfiguration.DEFAULT

        fun withHandlerFactory(factory: HandlerFactory) = apply {
            this.handlerFactory = factory
        }

        fun withSpeechEngineFactory(factory: ISpeechEngineFactory) = apply {
            this.speechEngineFactory = factory
        }

        fun withConfiguration(config: ServiceConfiguration) = apply {
            this.configuration = config
        }

        fun withSpeechEngine(engine: SpeechEngine) = apply {
            this.configuration = configuration.copy(speechEngine = engine.name)
        }

        fun withWakeWord(wakeWord: String, enabled: Boolean = true) = apply {
            this.configuration = configuration.copy(
                wakeWord = wakeWord,
                enableWakeWord = enabled
            )
        }

        fun withLanguage(language: String) = apply {
            this.configuration = configuration.copy(voiceLanguage = language)
        }

        fun withDebugMode(enabled: Boolean) = apply {
            this.configuration = configuration.copy(debugMode = enabled)
        }

        fun build(): VoiceOSCoreNG {
            return VoiceOSCoreNG(
                handlerFactory = handlerFactory ?: throw IllegalStateException("HandlerFactory required"),
                speechEngineFactory = speechEngineFactory ?: SpeechEngineFactoryProvider.create(),
                configuration = configuration
            )
        }
    }
}

/**
 * Factory for creating handlers.
 * Platform-specific implementations should provide the executors.
 */
interface HandlerFactory {
    /**
     * Create all handlers with platform-specific executors.
     */
    fun createHandlers(): List<IHandler>
}
