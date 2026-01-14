/**
 * VoiceOSCoreNG.kt - Main facade for VoiceOSCoreNG
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-09 - Auto-register static commands with speech engine
 *
 * Main entry point and facade for VoiceOSCoreNG functionality.
 * Provides unified access to handlers, speech engines, and command processing.
 */
package com.augmentalis.voiceoscoreng

import com.augmentalis.voiceoscoreng.common.CommandMatcher
import com.augmentalis.voiceoscoreng.common.CommandRegistry
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.common.StaticCommandRegistry
import com.augmentalis.voiceoscoreng.handlers.*
import com.augmentalis.voiceoscoreng.features.*
import com.augmentalis.voiceoscoreng.synonym.*
import com.augmentalis.voiceoscoreng.nlu.*
import com.augmentalis.voiceoscoreng.llm.*
import com.augmentalis.voiceoscoreng.persistence.IStaticCommandPersistence
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
    private val configuration: ServiceConfiguration,
    private val commandRegistry: CommandRegistry = CommandRegistry(),
    private val synonymProvider: ISynonymProvider? = null,
    private val nluProcessor: INluProcessor? = null,
    private val llmProcessor: ILlmProcessor? = null,
    private val nluConfig: NluConfig = NluConfig.DEFAULT,
    private val llmConfig: LlmConfig = LlmConfig.DEFAULT,
    private val staticCommandPersistence: IStaticCommandPersistence? = null
) {
    // Coordinator manages handler execution (with NLU and LLM support)
    // Uses shared commandRegistry for direct synchronous access
    private val coordinator = ActionCoordinator(
        commandRegistry = commandRegistry,
        nluProcessor = nluProcessor,
        llmProcessor = llmProcessor,
        nluConfig = nluConfig,
        llmConfig = llmConfig
    )

    // State manager tracks service lifecycle
    private val stateManager = ServiceStateManager()

    // Current speech engine
    private var speechEngine: ISpeechEngine? = null

    // Synonym provider for fuzzy matching
    private var activeSynonymProvider: ISynonymProvider? = synonymProvider

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
    val speechResults: Flow<SpeechResult>
        get() = speechEngine?.results ?: emptyFlow()

    /**
     * Initialize the core.
     */
    suspend fun initialize() {
        stateManager.transition(ServiceState.Initializing(0f, "Creating handlers"))

        try {
            // Create handlers using factory
            val handlers = handlerFactory.createHandlers()

            stateManager.transition(ServiceState.Initializing(0.2f, "Registering handlers"))

            // Initialize coordinator with handlers
            coordinator.initialize(handlers)

            stateManager.transition(ServiceState.Initializing(0.3f, "Populating static commands"))

            // Populate static commands to database (if persistence is provided)
            if (staticCommandPersistence != null) {
                try {
                    val count = staticCommandPersistence.populateIfNeeded()
                    if (count > 0) {
                        println("[VoiceOSCoreNG] Populated $count static commands to database")
                    } else {
                        println("[VoiceOSCoreNG] Static commands already in database")
                    }
                } catch (e: Exception) {
                    println("[VoiceOSCoreNG] Static command persistence error: ${e.message}")
                    // Continue without persistence - static commands still work in memory
                }
            }

            stateManager.transition(ServiceState.Initializing(0.4f, "Initializing synonyms"))

            // Wire up synonym provider for fuzzy matching
            if (configuration.synonymsEnabled && activeSynonymProvider != null) {
                CommandMatcher.synonymProvider = activeSynonymProvider
                CommandMatcher.defaultLanguage = configuration.effectiveSynonymLanguage()
                println("[VoiceOSCoreNG] Synonym provider initialized for ${configuration.effectiveSynonymLanguage()}")
            }

            stateManager.transition(ServiceState.Initializing(0.5f, "Initializing NLU"))

            // Initialize NLU processor (BERT-based intent classification)
            if (nluConfig.enabled && nluProcessor != null) {
                try {
                    val nluResult = nluProcessor.initialize()
                    if (nluResult.isSuccess) {
                        println("[VoiceOSCoreNG] NLU processor initialized successfully")
                    } else {
                        println("[VoiceOSCoreNG] NLU initialization failed: ${nluResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    println("[VoiceOSCoreNG] NLU initialization error: ${e.message}")
                    // Continue without NLU - system still works with registry matching
                }
            }

            stateManager.transition(ServiceState.Initializing(0.55f, "Initializing LLM"))

            // Initialize LLM processor (natural language fallback)
            if (llmConfig.enabled && llmProcessor != null) {
                try {
                    val llmResult = llmProcessor.initialize()
                    if (llmResult.isSuccess) {
                        println("[VoiceOSCoreNG] LLM processor initialized successfully")
                    } else {
                        println("[VoiceOSCoreNG] LLM initialization failed: ${llmResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    println("[VoiceOSCoreNG] LLM initialization error: ${e.message}")
                    // Continue without LLM - system still works with registry + NLU
                }
            }

            stateManager.transition(ServiceState.Initializing(0.6f, "Initializing speech engine"))

            // Always create speech engine (not just when autoStartListening)
            try {
                val engineResult = speechEngineFactory.createEngine(
                    SpeechEngine.valueOf(configuration.speechEngine)
                )
                if (engineResult.isSuccess) {
                    speechEngine = engineResult.getOrNull()
                    val config = SpeechConfig(
                        language = configuration.voiceLanguage,
                        confidenceThreshold = configuration.confidenceThreshold
                    )
                    val initResult = speechEngine?.initialize(config)
                    if (initResult?.isFailure == true) {
                        // Log error but don't fail - voice is optional
                        println("[VoiceOSCoreNG] Speech engine initialization failed: ${initResult.exceptionOrNull()?.message}")
                    }

                    // Register static commands with speech engine for voice recognition
                    if (initResult?.isSuccess == true) {
                        stateManager.transition(ServiceState.Initializing(0.7f, "Registering static commands with speech engine"))
                        try {
                            val staticPhrases = staticCommandPersistence?.getAllPhrases() ?: StaticCommandRegistry.allPhrases()
                            if (staticPhrases.isNotEmpty()) {
                                val updateResult = speechEngine?.updateCommands(staticPhrases)
                                if (updateResult?.isSuccess == true) {
                                    println("[VoiceOSCoreNG] Registered ${staticPhrases.size} static command phrases with speech engine")
                                } else {
                                    println("[VoiceOSCoreNG] Failed to register static commands with speech engine: ${updateResult?.exceptionOrNull()?.message}")
                                }
                            }
                        } catch (e: Exception) {
                            println("[VoiceOSCoreNG] Error registering static commands: ${e.message}")
                            // Continue - static commands can still be processed, just not speech-recognized
                        }
                    }

                    // Only auto-start listening if configured AND initialization succeeded
                    if (configuration.autoStartListening && initResult?.isSuccess == true) {
                        speechEngine?.startListening()
                    }
                } else {
                    println("[VoiceOSCoreNG] Speech engine creation failed: ${engineResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("[VoiceOSCoreNG] Speech engine setup failed: ${e.message}")
                // Continue without speech - handlers still work
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
     * Update the speech engine with dynamic commands (phrases only).
     *
     * This must be called after screen changes to register new voice commands
     * with the speech recognition engine (e.g., Vivoka SDK grammar).
     *
     * @param commands List of command phrases to recognize
     * @return Result indicating success or failure
     */
    suspend fun updateCommands(commands: List<String>): Result<Unit> {
        val engine = speechEngine ?: return Result.failure(
            IllegalStateException("No speech engine configured")
        )
        return engine.updateCommands(commands)
    }

    /**
     * Update dynamic commands from UI elements.
     *
     * This is the primary method to call after screen scraping. It:
     * 1. Registers commands with the ActionCoordinator for execution (with VUIDs)
     * 2. Optionally updates the speech engine grammar
     *
     * @param commands List of quantized commands from UI elements
     * @param updateSpeechEngine Whether to also update speech recognition grammar
     * @return Result indicating success or failure
     */
    suspend fun updateDynamicCommands(
        commands: List<QuantizedCommand>,
        updateSpeechEngine: Boolean = true
    ): Result<Unit> {
        return try {
            // Register commands with coordinator (includes VUIDs for direct execution)
            coordinator.updateDynamicCommands(commands)

            // Optionally update speech engine grammar
            if (updateSpeechEngine && speechEngine != null) {
                val phrases = commands.map { it.phrase }
                speechEngine?.updateCommands(phrases)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear all dynamic commands.
     * Call when leaving an app or screen context is invalid.
     */
    fun clearDynamicCommands() {
        coordinator.clearDynamicCommands()
    }

    /**
     * Get current count of dynamic commands.
     */
    val dynamicCommandCount: Int get() = coordinator.dynamicCommandCount

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

    // ═══════════════════════════════════════════════════════════════════════════
    // NLU/LLM Integration - Unified Command Access
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all commands (static + dynamic) as QuantizedCommand for NLU/LLM.
     *
     * This provides a unified view of all available voice commands:
     * - Static commands: System-wide commands (targetVuid = null)
     * - Dynamic commands: Screen-specific element commands (targetVuid = element VUID)
     *
     * Use this to provide command context to NLU/LLM systems.
     *
     * @return List of all available QuantizedCommand
     */
    fun getAllQuantizedCommands(): List<QuantizedCommand> {
        return coordinator.getAllQuantizedCommands()
    }

    /**
     * Get only static commands as QuantizedCommand.
     *
     * Static commands are always available regardless of screen context.
     * Useful for LLM system prompts that need base command vocabulary.
     *
     * @return List of static QuantizedCommand
     */
    fun getStaticQuantizedCommands(): List<QuantizedCommand> {
        return coordinator.getStaticQuantizedCommands()
    }

    /**
     * Get commands in AVU format for NLU/LLM.
     *
     * Format: CMD:uuid:trigger:action:element_uuid:confidence
     *
     * @param includeStatic Include static commands (default: true)
     * @param includeDynamic Include dynamic commands (default: true)
     * @return Multi-line string in AVU CMD format
     */
    fun getCommandsAsAvu(includeStatic: Boolean = true, includeDynamic: Boolean = true): String {
        return coordinator.getCommandsAsAvu(includeStatic, includeDynamic)
    }

    /**
     * Get NLU schema for LLM context.
     *
     * Returns a human-readable schema suitable for LLM prompts,
     * describing all available commands grouped by category.
     *
     * Example usage in LLM prompt:
     * ```
     * val schema = core.getNluSchema()
     * val prompt = """
     * Available voice commands:
     * $schema
     *
     * User said: "${userInput}"
     * Which command matches best?
     * """
     * ```
     *
     * @return Formatted NLU schema string
     */
    fun getNluSchema(): String {
        return coordinator.getNluSchema()
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

        // Dispose NLU and LLM processors
        try {
            nluProcessor?.dispose()
        } catch (e: Exception) {
            println("[VoiceOSCoreNG] NLU dispose error: ${e.message}")
        }

        try {
            llmProcessor?.dispose()
        } catch (e: Exception) {
            println("[VoiceOSCoreNG] LLM dispose error: ${e.message}")
        }

        stateManager.transition(ServiceState.Stopped)
    }

    /**
     * Check if NLU processor is available.
     */
    fun isNluAvailable(): Boolean = nluProcessor?.isAvailable() == true

    /**
     * Check if LLM processor is available.
     */
    fun isLlmAvailable(): Boolean = llmProcessor?.isAvailable() == true

    companion object {
        // Companion object for extension functions
    }

    /**
     * Builder for VoiceOSCoreNG.
     */
    class Builder {
        private var handlerFactory: HandlerFactory? = null
        private var speechEngineFactory: ISpeechEngineFactory? = null
        private var configuration: ServiceConfiguration = ServiceConfiguration.DEFAULT
        private var commandRegistry: CommandRegistry? = null
        private var synonymProvider: ISynonymProvider? = null
        private var nluProcessor: INluProcessor? = null
        private var llmProcessor: ILlmProcessor? = null
        private var nluConfig: NluConfig = NluConfig.DEFAULT
        private var llmConfig: LlmConfig = LlmConfig.DEFAULT
        private var staticCommandPersistence: IStaticCommandPersistence? = null

        fun withHandlerFactory(factory: HandlerFactory) = apply {
            this.handlerFactory = factory
        }

        fun withSpeechEngineFactory(factory: ISpeechEngineFactory) = apply {
            this.speechEngineFactory = factory
        }

        fun withConfiguration(config: ServiceConfiguration) = apply {
            this.configuration = config
        }

        /**
         * Set a shared CommandRegistry for direct synchronous access.
         *
         * When provided, both VoiceOSCoreNG and the caller can access the same
         * registry instance for reading/writing commands without async wrappers.
         *
         * @param registry Shared CommandRegistry instance
         */
        fun withCommandRegistry(registry: CommandRegistry) = apply {
            this.commandRegistry = registry
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

        /**
         * Set the synonym provider for fuzzy voice command matching.
         *
         * Platform-specific creation:
         * ```kotlin
         * // Android
         * val loader = SynonymLoader(
         *     AndroidSynonymPaths(context),
         *     AndroidResourceLoader(context),
         *     AndroidFileLoader()
         * )
         * builder.withSynonymProvider(StaticSynonymProvider(loader))
         *
         * // iOS
         * val loader = SynonymLoader(
         *     IOSSynonymPaths(),
         *     IOSResourceLoader(),
         *     IOSFileLoader()
         * )
         * builder.withSynonymProvider(StaticSynonymProvider(loader))
         * ```
         *
         * @param provider The synonym provider to use
         */
        fun withSynonymProvider(provider: ISynonymProvider) = apply {
            this.synonymProvider = provider
        }

        /**
         * Enable/disable synonym expansion.
         *
         * @param enabled Whether to use synonyms for matching
         * @param language ISO 639-1 language code (null uses voiceLanguage)
         */
        fun withSynonyms(enabled: Boolean, language: String? = null) = apply {
            this.configuration = configuration.copy(
                synonymsEnabled = enabled,
                synonymLanguage = language
            )
        }

        /**
         * Set the NLU processor for semantic intent classification.
         *
         * @param processor NLU processor implementation
         * @param config NLU configuration
         */
        fun withNluProcessor(processor: INluProcessor, config: NluConfig = NluConfig.DEFAULT) = apply {
            this.nluProcessor = processor
            this.nluConfig = config
        }

        /**
         * Set the LLM processor for natural language fallback.
         *
         * @param processor LLM processor implementation
         * @param config LLM configuration
         */
        fun withLlmProcessor(processor: ILlmProcessor, config: LlmConfig = LlmConfig.DEFAULT) = apply {
            this.llmProcessor = processor
            this.llmConfig = config
        }

        /**
         * Enable NLU with default configuration.
         * Uses platform factory to create processor.
         *
         * @param config NLU configuration
         */
        fun withNlu(config: NluConfig = NluConfig.DEFAULT) = apply {
            this.nluConfig = config
            // Processor will be created via factory when build() is called
        }

        /**
         * Enable LLM with default configuration.
         * Uses platform factory to create processor.
         *
         * @param config LLM configuration
         */
        fun withLlm(config: LlmConfig = LlmConfig.DEFAULT) = apply {
            this.llmConfig = config
            // Processor will be created via factory when build() is called
        }

        /**
         * Set the static command persistence for database storage.
         *
         * Static commands will be populated to the database on first run,
         * making them available for:
         * - Speech engine vocabulary loading
         * - Offline availability
         * - User customization (enable/disable)
         *
         * @param persistence Static command persistence implementation
         */
        fun withStaticCommandPersistence(persistence: IStaticCommandPersistence) = apply {
            this.staticCommandPersistence = persistence
        }

        fun build(): VoiceOSCoreNG {
            return VoiceOSCoreNG(
                handlerFactory = handlerFactory ?: throw IllegalStateException("HandlerFactory required"),
                speechEngineFactory = speechEngineFactory ?: SpeechEngineFactoryProvider.create(),
                configuration = configuration,
                commandRegistry = commandRegistry ?: CommandRegistry(),
                synonymProvider = synonymProvider,
                nluProcessor = nluProcessor,
                llmProcessor = llmProcessor,
                nluConfig = nluConfig,
                llmConfig = llmConfig,
                staticCommandPersistence = staticCommandPersistence
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
