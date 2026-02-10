/**
 * VoiceOSCore.kt - Unified KMP Voice Control Module
 *
 * Main entry point and facade for VoiceOSCore functionality.
 * Provides unified access to handlers, speech engines, and command processing.
 *
 * Source Sets:
 * - commonMain: Cross-platform shared logic (result, hash, constants, validation,
 *   exceptions, command-models, accessibility-types, logging, text-utils,
 *   json-utils, database, synonym, cursor, exploration, jit, e2e)
 * - androidMain: Android UI, services, accessibility, speech engines
 * - iosMain: iOS implementations
 * - desktopMain: Desktop implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 * Updated: 2026-01-19 - Merged VoiceOSCoreNG into unified VoiceOSCore
 * Updated: 2026-01-27 - Added handler phrase collection for speech engine (app commands)
 */
package com.augmentalis.voiceoscore

import kotlinx.coroutines.flow.*
import kotlin.collections.flatMap

/**
 * Main facade for VoiceOSCore.
 *
 * Usage:
 * ```kotlin
 * // Initialize
 * val core = VoiceOSCore.Builder()
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
class VoiceOSCore private constructor(
    private val handlerFactory: HandlerFactory,
    private val speechEngineFactory: ISpeechEngineFactory,
    private val configuration: ServiceConfiguration,
    private val commandRegistry: CommandRegistry = CommandRegistry(),
    private val synonymProvider: ISynonymProvider? = null,
    private val staticCommandPersistence: IStaticCommandPersistence? = null
) {
    // Coordinator manages handler execution
    // Uses shared commandRegistry for direct synchronous access
    private val coordinator = ActionCoordinator(
        commandRegistry = commandRegistry
    )

    // State manager tracks service lifecycle
    private val stateManager = ServiceStateManager()

    // Current speech engine
    @Volatile
    private var speechEngine: ISpeechEngine? = null

    // Synonym provider for fuzzy matching
    @Volatile
    private var activeSynonymProvider: ISynonymProvider? = synonymProvider

    /**
     * Service state flow.
     */
    val state: StateFlow<ServiceState> = stateManager.state

    /**
     * Command execution results flow.
     */
    val commandResults: SharedFlow<ActionCommandResult> = coordinator.results


    /**
     * Speech recognition results flow.
     */
    val speechResults: Flow<SpeechResult>
        get() = speechEngine?.results ?: emptyFlow()

    /** Callback for special system actions (easter eggs, dev mode, etc.) */
    var onSystemAction: ((String) -> Unit)? = null

    /**
     * Installed app phrases list
     */
    private val appHandlerPhrases = mutableListOf<String>()

    /**
     * all registered Commands
     */
    private val allRegisteredCommands: MutableSet<String> = mutableSetOf()

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
                        println("[VoiceOSCore] Populated $count static commands to database")
                    } else {
                        println("[VoiceOSCore] Static commands already in database")
                    }
                } catch (e: Exception) {
                    println("[VoiceOSCore] Static command persistence error: ${e.message}")
                    // Continue without persistence - static commands still work in memory
                }
            }

            stateManager.transition(ServiceState.Initializing(0.4f, "Initializing synonyms"))

            // Wire up synonym provider for fuzzy matching
            if (configuration.synonymsEnabled && activeSynonymProvider != null) {
                CommandMatcher.synonymProvider = activeSynonymProvider
                CommandMatcher.defaultLanguage = configuration.effectiveSynonymLanguage()
                println("[VoiceOSCore] Synonym provider initialized for ${configuration.effectiveSynonymLanguage()}")
            }

            stateManager.transition(ServiceState.Initializing(0.5f, "Initializing speech engine"))

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
                        println("[VoiceOSCore] Speech engine initialization failed: ${initResult.exceptionOrNull()?.message}")
                    }

                    // Register static commands and handler phrases with speech engine for voice recognition
                    if (initResult?.isSuccess == true) {
                        stateManager.transition(ServiceState.Initializing(0.7f, "Registering commands with speech engine"))
                        try {
                            // Get static command phrases
                            val staticPhrases = staticCommandPersistence?.getAllPhrases() ?: StaticCommandRegistry.allPhrases()

                            // Collect voice phrases from all handlers (e.g., app names from AppHandler)
                            val handlerPhrases = handlers.flatMap { it.getVoicePhrases() }
                            if (appHandlerPhrases.isEmpty()) {
                                appHandlerPhrases.addAll(handlerPhrases)
                            }

                            // Combine all phrases
                            val allPhrases = (staticPhrases + handlerPhrases).distinct()

                            if (allPhrases.isNotEmpty()) {
                                val updateResult = speechEngine?.updateCommands(allPhrases)
                                if (updateResult?.isSuccess == true) {
                                    println("[VoiceOSCore] Registered ${allPhrases.size} phrases with speech engine (${staticPhrases.size} static, ${handlerPhrases.size} from handlers)")
                                } else {
                                    println("[VoiceOSCore] Failed to register commands with speech engine: ${updateResult?.exceptionOrNull()?.message}")
                                }
                            }
                        } catch (e: Exception) {
                            println("[VoiceOSCore] Error registering commands: ${e.message}")
                            // Continue - commands can still be processed, just not speech-recognized
                        }
                    }

                    // Only auto-start listening if configured AND initialization succeeded
                    if (configuration.autoStartListening && initResult?.isSuccess == true) {
                        speechEngine?.startListening()
                    }
                } else {
                    println("[VoiceOSCore] Speech engine creation failed: ${engineResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("[VoiceOSCore] Speech engine setup failed: ${e.message}")
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
        // Hidden system command intercept
        val normalizedInput = text.lowercase().trim()
        if (normalizedInput == "developer mode 5x" ||
            normalizedInput == "developer mode five x" ||
            normalizedInput == "developer mode 5 x") {
            onSystemAction?.invoke("OPEN_DEVELOPER_SETTINGS")
            return HandlerResult.success("Opening developer settings")
        }

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
        // Merge + de-dupe correctly (set of Strings)
        val newCommands: Set<String> = buildSet {
            addAll(commands)
            addAll(appHandlerPhrases)
        }

        println("[VoiceOSCore] allRegisteredCommands = ${allRegisteredCommands.size} , newCommands = ${newCommands.size}")
        // No change -> skip engine call
        if (newCommands == allRegisteredCommands) {
            return Result.success(Unit)
        }else{
            val removed = allRegisteredCommands - newCommands
            val new = newCommands - allRegisteredCommands

            println("[VoiceOSCore] old removed = $removed")
            println("[VoiceOSCore] new add = $new")

        }

        // Update engine; only update cache if engine update succeeds
        return engine.updateCommands(newCommands.toList())
            .onSuccess {
                allRegisteredCommands.clear()
                allRegisteredCommands.addAll(newCommands)
            }
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
                speechEngine?.updateCommands(phrases + appHandlerPhrases)
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

        stateManager.transition(ServiceState.Stopped)
    }

    /**
     * Builder for VoiceOSCore.
     */
    class Builder {
        private var handlerFactory: HandlerFactory? = null
        private var speechEngineFactory: ISpeechEngineFactory? = null
        private var configuration: ServiceConfiguration = ServiceConfiguration.DEFAULT
        private var commandRegistry: CommandRegistry? = null
        private var synonymProvider: ISynonymProvider? = null
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
         * When provided, both VoiceOSCore and the caller can access the same
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

        fun build(): VoiceOSCore {
            return VoiceOSCore(
                handlerFactory = handlerFactory ?: throw IllegalStateException("HandlerFactory required"),
                speechEngineFactory = speechEngineFactory ?: SpeechEngineFactoryProvider.create(),
                configuration = configuration,
                commandRegistry = commandRegistry ?: CommandRegistry(),
                synonymProvider = synonymProvider,
                staticCommandPersistence = staticCommandPersistence
            )
        }
    }

    companion object {
        const val VERSION = "1.0.0"
        const val MODULE_NAME = "VoiceOSCore"

        /**
         * Platform-specific implementation provider.
         * Set by platform source sets (androidMain, iosMain, desktopMain).
         */
        var platformProvider: PlatformProvider? = null

        /**
         * Initialize the VoiceOSCore module.
         * Must be called before using any VoiceOS functionality.
         */
        fun initializeModule() {
            // Module initialization logic
            // Platform-specific initialization handled by PlatformProvider
        }
    }
}

/**
 * Platform-specific functionality provider interface.
 * Implemented in androidMain, iosMain, and desktopMain.
 */
interface PlatformProvider {
    /**
     * Platform name (Android, iOS, Desktop).
     */
    val platformName: String

    /**
     * Initialize platform-specific components.
     */
    suspend fun initialize()

    /**
     * Cleanup platform-specific resources.
     */
    suspend fun cleanup()
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
