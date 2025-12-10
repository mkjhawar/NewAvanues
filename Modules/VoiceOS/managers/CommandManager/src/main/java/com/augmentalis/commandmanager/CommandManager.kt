/**
 * CommandManager.kt - Simplified commands manager
 * Direct implementation - no unnecessary abstractions
 */

package com.augmentalis.commandmanager

import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.voiceos.command.*
import com.augmentalis.commandmanager.actions.*
import com.augmentalis.universalipc.UniversalIPCEncoder
import com.augmentalis.commandmanager.loader.CommandLoader
import com.augmentalis.commandmanager.loader.CommandLocalizer
import com.augmentalis.commandmanager.routing.IntentDispatcher
import com.augmentalis.commandmanager.routing.AppIPCRegistry
import com.augmentalis.voiceos.speech.confidence.ConfidenceScorer
import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Commands Manager
 * Zero overhead direct implementation
 */
class CommandManager(private val context: Context) {

    companion object {
        private const val TAG = "CommandManager"

        @Volatile
        private var instance: CommandManager? = null

        fun getInstance(context: Context): CommandManager {
            return instance ?: synchronized(this) {
                instance ?: CommandManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Confidence scoring
    private val confidenceScorer = ConfidenceScorer()

    // Confidence-based execution strategy
    private var requireConfirmationCallback: ((Command, ConfidenceLevel) -> Boolean)? = null
    private var alternativesCallback: ((Command, List<String>) -> String?)? = null

    // Service callback for lifecycle events (Phase 1: Service Monitor)
    private var serviceCallback: com.augmentalis.commandmanager.monitor.ServiceCallback? = null

    // Health status
    private var isHealthy: Boolean = true
    private var lastHealthCheckTime: Long = 0L

    // Multi-language command system (Phase 1)
    private val commandLoader: CommandLoader by lazy { CommandLoader.create(context) }
    private val commandLocalizer: CommandLocalizer by lazy { CommandLocalizer.create(context) }

    // Database command resolver for pattern matching
    private val databaseResolver by lazy {
        com.augmentalis.commandmanager.loader.DatabaseCommandResolver.create(context)
    }

    // Intent dispatcher for app handler registration
    // FIX: Renamed from intentDispatcher to avoid name collision with getIntentDispatcher()
    private val _intentDispatcher: IntentDispatcher by lazy {
        IntentDispatcher(context)
    }

    // Universal IPC encoder for cross-app communication
    private val ipcEncoder = UniversalIPCEncoder()

    // Database command cache (command ID -> CommandMetadata)
    // CommandMetadata contains patterns and category for dynamic action creation
    private data class CommandMetadata(
        val patterns: List<String>,
        val category: String
    )

    private val databaseCommandCache = mutableMapOf<String, CommandMetadata>()
    private var databaseCommandsLoaded = false

    // Action cache (command ID -> BaseAction instance)
    // Actions are created dynamically from database metadata via ActionFactory
    private val actionCache = mutableMapOf<String, BaseAction>()

    /**
     * Execute command directly with confidence-based filtering
     */
    suspend fun executeCommand(command: Command): CommandResult {
        Log.d(TAG, "Executing command: ${command.id} with confidence: ${command.confidence}")

        // Apply confidence-based filtering
        val confidenceLevel = confidenceScorer.getConfidenceLevel(command.confidence)

        when (confidenceLevel) {
            ConfidenceLevel.REJECT -> {
                Log.w(TAG, "Command rejected due to low confidence: ${command.confidence}")
                return CommandResult(
                    success = false,
                    command = command,
                    error = CommandError(
                        ErrorCode.EXECUTION_FAILED,
                        "Confidence too low: ${(command.confidence * 100).toInt()}%"
                    )
                )
            }

            ConfidenceLevel.LOW -> {
                // Low confidence - show alternatives if callback is set
                alternativesCallback?.let { callback ->
                    val alternatives = findAlternativeCommands(command)
                    if (alternatives.isNotEmpty()) {
                        val selectedCommand = callback(command, alternatives)
                        if (selectedCommand != null) {
                            // Execute the selected alternative
                            return executeCommandInternal(
                                command.copy(
                                    id = selectedCommand,
                                    text = selectedCommand
                                )
                            )
                        }
                    }
                }
                // If no callback or no selection, continue with original
                Log.w(TAG, "Low confidence command proceeding: ${command.confidence}")
            }

            ConfidenceLevel.MEDIUM -> {
                // Medium confidence - ask for confirmation if callback is set
                requireConfirmationCallback?.let { callback ->
                    val confirmed = callback(command, confidenceLevel)
                    if (!confirmed) {
                        Log.i(TAG, "Command execution cancelled by user")
                        return CommandResult(
                            success = false,
                            command = command,
                            error = CommandError(ErrorCode.CANCELLED, "User cancelled execution")
                        )
                    }
                }
                // If no callback or confirmed, continue
                Log.i(TAG, "Medium confidence command proceeding: ${command.confidence}")
            }

            ConfidenceLevel.HIGH -> {
                // High confidence - execute immediately
                Log.d(TAG, "High confidence command: ${command.confidence}")
            }
        }

        return executeCommandInternal(command)
    }

    /**
     * Internal command execution without confidence filtering
     * Includes database pattern matching and fuzzy matching
     */
    private suspend fun executeCommandInternal(command: Command): CommandResult {
        Log.d(TAG, "executeCommandInternal: text='${command.text}', id='${command.id}'")

        // Step 1: Try to match command text against database patterns
        var matchedCommandId = matchCommandTextToId(command.text)

        if (matchedCommandId != null) {
            Log.i(TAG, "✓ Pattern match: '${command.text}' -> '$matchedCommandId'")
        } else {
            // Step 2: Fallback to using command.id if no pattern match
            matchedCommandId = command.id
            Log.d(TAG, "No pattern match, using command.id: '$matchedCommandId'")
        }

        // Step 3: Try to get action for the matched command ID
        var action = getActionForCommandId(matchedCommandId)

        // Step 4: If still no action, try fuzzy matching
        if (action == null) {
            val fuzzyMatch = findBestCommandMatch(command.text)
            if (fuzzyMatch != null) {
                matchedCommandId = fuzzyMatch.first
                val similarity = fuzzyMatch.second

                Log.i(TAG, "Fuzzy match found: '$matchedCommandId' (similarity: ${(similarity * 100).toInt()}%)")

                action = getActionForCommandId(matchedCommandId)
            }
        }

        // Step 5: Execute action if found
        return if (action != null) {
            try {
                Log.i(TAG, "✓ Executing action for: '$matchedCommandId'")
                action.invoke(command.copy(id = matchedCommandId))
            } catch (e: Exception) {
                Log.e(TAG, "Command execution failed", e)
                CommandResult(
                    success = false,
                    command = command,
                    error = CommandError(ErrorCode.EXECUTION_FAILED, e.message ?: "Unknown error")
                )
            }
        } else {
            Log.w(TAG, "✗ No action found for: '${command.text}' (tried ID: '$matchedCommandId')")
            CommandResult(
                success = false,
                command = command,
                error = CommandError(ErrorCode.COMMAND_NOT_FOUND, "Unknown command: ${command.text}")
            )
        }
    }

    /**
     * Match spoken command text to command ID using database patterns
     * Examples: "go back" -> "nav_back", "turn up volume" -> "volume_up"
     */
    private fun matchCommandTextToId(commandText: String): String? {
        if (!databaseCommandsLoaded || databaseCommandCache.isEmpty()) {
            Log.d(TAG, "Database commands not loaded yet")
            return null
        }

        val normalizedText = commandText.lowercase().trim()

        // Try exact match first
        for ((commandId, metadata) in databaseCommandCache) {
            for (pattern in metadata.patterns) {
                if (pattern.lowercase().trim() == normalizedText) {
                    Log.d(TAG, "Exact pattern match: '$commandText' -> '$commandId'")
                    return commandId
                }
            }
        }

        // Try contains match (pattern contains the command text)
        for ((commandId, metadata) in databaseCommandCache) {
            for (pattern in metadata.patterns) {
                val normalizedPattern = pattern.lowercase().trim()
                if (normalizedPattern.contains(normalizedText) || normalizedText.contains(normalizedPattern)) {
                    Log.d(TAG, "Partial pattern match: '$commandText' -> '$commandId'")
                    return commandId
                }
            }
        }

        return null
    }

    /**
     * Get action handler for a given command ID
     * Uses ActionFactory to dynamically create actions from database metadata
     * Actions are cached to avoid recreating them for repeated commands
     */
    private fun getActionForCommandId(commandId: String): BaseAction? {
        // Check cache first
        actionCache[commandId]?.let { return it }

        // Get command metadata from database cache
        val metadata = databaseCommandCache[commandId]
        if (metadata == null) {
            Log.w(TAG, "No metadata found for command ID: $commandId")
            return null
        }

        // Create action dynamically using ActionFactory
        val action = ActionFactory.createAction(commandId, metadata.category)
        if (action == null) {
            Log.w(TAG, "ActionFactory could not create action for ID='$commandId', category='${metadata.category}'")
            return null
        }

        // Cache the action for future use
        actionCache[commandId] = action
        Log.d(TAG, "Created and cached action for: $commandId (category: ${metadata.category})")

        return action
    }

    /**
     * Find alternative commands based on similarity
     * Used for LOW confidence level to show user alternatives
     */
    private fun findAlternativeCommands(command: Command): List<String> {
        // Use all patterns from database command cache
        val allPatterns = databaseCommandCache.values.flatMap { it.patterns }

        return confidenceScorer.findAllSimilar(
            recognized = command.text,
            commands = allPatterns,
            minConfidence = ConfidenceScorer.THRESHOLD_LOW,
            maxResults = 3
        ).map { it.first }
    }

    /**
     * Find best matching command using fuzzy matching
     * Used when exact match fails
     *
     * @param commandText The command text to match
     * @return Pair of (command_id, similarity) or null if no good match
     */
    private fun findBestCommandMatch(commandText: String): Pair<String, Float>? {
        // Use all command IDs from database cache
        val allCommandIds = databaseCommandCache.keys.toList()

        return confidenceScorer.findBestMatch(
            recognized = commandText,
            commands = allCommandIds,
            minConfidence = 0.70f  // Use 70% as minimum for fuzzy matching
        )
    }

    /**
     * Set callback for confirmation requests on medium confidence commands
     */
    fun setConfirmationCallback(callback: (Command, ConfidenceLevel) -> Boolean) {
        this.requireConfirmationCallback = callback
        Log.d(TAG, "Confirmation callback registered")
    }

    /**
     * Set callback for alternative selection on low confidence commands
     */
    fun setAlternativesCallback(callback: (Command, List<String>) -> String?) {
        this.alternativesCallback = callback
        Log.d(TAG, "Alternatives callback registered")
    }

    /**
     * Execute command with explicit confidence override
     * Bypasses confidence filtering
     */
    suspend fun executeCommandWithConfidenceOverride(command: Command): CommandResult {
        Log.d(TAG, "Executing command with confidence override: ${command.id}")
        return executeCommandInternal(command)
    }
    
    /**
     * Initialize manager
     */
    fun initialize() {
        Log.d(TAG, "CommandManager initialized")
        isHealthy = true
        lastHealthCheckTime = System.currentTimeMillis()

        // Initialize multi-language command system (Phase 1: CommandLoader Integration)
        // Replaces VOSCommandIngestion with JSON-based localization system
        // Runs asynchronously to avoid blocking initialization
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "Initializing multi-language command system...")

                // Initialize CommandLocalizer (handles locale management + fallback)
                commandLocalizer.initialize()

                // Load commands via CommandLoader (with automatic persistence check)
                val result = commandLoader.initializeCommands()

                when (result) {
                    is CommandLoader.LoadResult.Success -> {
                        Log.i(TAG, "✅ Command system initialized: ${result.commandCount} commands")
                        Log.i(TAG, "   Available locales: ${result.locales.joinToString(", ")}")
                        Log.i(TAG, "   Current locale: ${getCurrentLocale()}")
                    }
                    is CommandLoader.LoadResult.Error -> {
                        Log.e(TAG, "❌ Command initialization failed: ${result.message}")
                    }
                    is CommandLoader.LoadResult.LocaleNotFound -> {
                        Log.w(TAG, "⚠️ Locale not found, using English fallback")
                    }
                }

                // Load database commands for pattern matching
                loadDatabaseCommands()

            } catch (e: Exception) {
                // Log error but don't crash - CommandManager can still operate
                Log.e(TAG, "Failed to initialize command system", e)
            }
        }

        // Notify service callback
        serviceCallback?.onServiceBound()
    }

    /**
     * Load commands from database and build pattern matching cache
     * This enables matching spoken commands like "go back" to command IDs like "nav_back"
     * Also caches category for dynamic action creation via ActionFactory
     */
    private suspend fun loadDatabaseCommands() {
        try {
            val locale = getCurrentLocale()

            Log.i(TAG, "Loading database commands for locale: $locale")

            // Get commands from database with fallback
            val commands = databaseResolver.getAllCommandDefinitions(
                locale = locale,
                includeFallback = true
            )

            // Clear existing caches
            databaseCommandCache.clear()
            actionCache.clear()

            // Build metadata cache: command ID -> CommandMetadata(patterns, category)
            commands.forEach { cmdDef ->
                databaseCommandCache[cmdDef.id] = CommandMetadata(
                    patterns = cmdDef.patterns,
                    category = cmdDef.category
                )
            }

            databaseCommandsLoaded = true
            val totalPatterns = databaseCommandCache.values.sumOf { it.patterns.size }
            Log.i(TAG, "✅ Loaded ${commands.size} database commands with $totalPatterns total patterns")

            // Log sample for debugging
            if (commands.isNotEmpty()) {
                val sample = commands.first()
                Log.d(TAG, "   Sample: ID='${sample.id}', category='${sample.category}', patterns=${sample.patterns.take(3)}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load database commands", e)
            databaseCommandsLoaded = false
        }
    }

    // ============================================================================
    // Multi-Language Support API (Phase 1: Public API for Language Management)
    // ============================================================================

    /**
     * Get the current locale being used for commands
     * @return Current locale string (e.g., "en-US", "es-ES", "fr-FR", "de-DE")
     */
    fun getCurrentLocale(): String {
        // Access Flow's current value using runBlocking and first()
        return kotlinx.coroutines.runBlocking {
            commandLocalizer.currentLocale.first()
        }
    }

    /**
     * Get list of all available locales from command assets
     * @return List of locale strings (e.g., ["en-US", "es-ES", "fr-FR", "de-DE"])
     */
    suspend fun getAvailableLocales(): List<String> {
        return commandLocalizer.getAvailableLocales()
    }

    /**
     * Switch to a different locale and reload commands
     * @param locale Target locale (e.g., "es-ES", "fr-FR", "de-DE")
     * @return true if switch was successful, false if locale not available
     */
    suspend fun switchLocale(locale: String): Boolean {
        return try {
            val currentLocale = getCurrentLocale()
            Log.i(TAG, "Switching locale from $currentLocale to $locale...")

            // Set locale in CommandLocalizer (handles persistence automatically)
            val switched = commandLocalizer.setLocale(locale)

            if (switched) {
                // Reload commands for new locale via CommandLoader
                val result = commandLoader.initializeCommands()

                // Reload database commands for pattern matching
                loadDatabaseCommands()

                when (result) {
                    is CommandLoader.LoadResult.Success -> {
                        Log.i(TAG, "✅ Locale switched to $locale (${result.commandCount} commands loaded)")
                        true
                    }
                    is CommandLoader.LoadResult.Error -> {
                        Log.e(TAG, "❌ Failed to load commands for locale $locale: ${result.message}")
                        false
                    }
                    is CommandLoader.LoadResult.LocaleNotFound -> {
                        Log.w(TAG, "⚠️ Locale $locale not found, falling back to English")
                        false
                    }
                }
            } else {
                Log.w(TAG, "❌ Failed to switch to locale $locale (not available)")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during locale switch", e)
            false
        }
    }

    /**
     * Reset to system default locale
     * @return true if reset was successful
     */
    suspend fun resetToSystemLocale(): Boolean {
        return try {
            Log.i(TAG, "Resetting to system default locale...")
            commandLocalizer.resetToSystemLocale()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset to system locale", e)
            false
        }
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        Log.d(TAG, "CommandManager cleaned up")
        isHealthy = false

        // Notify service callback
        serviceCallback?.onServiceDisconnected()
    }

    /**
     * Set service callback for lifecycle events (Phase 1: Service Monitor)
     */
    fun setServiceCallback(callback: com.augmentalis.commandmanager.monitor.ServiceCallback) {
        this.serviceCallback = callback
        Log.d(TAG, "Service callback registered")
    }

    /**
     * Health check for service monitoring (Phase 1: Service Monitor)
     * @return true if CommandManager is healthy and operational
     */
    fun healthCheck(): Boolean {
        lastHealthCheckTime = System.currentTimeMillis()

        // Check basic health indicators
        val healthy = isHealthy

        if (!healthy) {
            Log.w(TAG, "Health check failed: isHealthy=$isHealthy")
        }

        return healthy
    }

    /**
     * Restart CommandManager (Phase 1: Service Monitor - Recovery)
     * Attempts to reinitialize the manager
     */
    fun restart() {
        Log.i(TAG, "Restarting CommandManager...")

        try {
            // Clear state
            cleanup()

            // Reinitialize
            initialize()

            Log.i(TAG, "CommandManager restart successful")
        } catch (e: Exception) {
            Log.e(TAG, "CommandManager restart failed", e)
            isHealthy = false
            throw e
        }
    }

    /**
     * Execute command in external app via Universal IPC Protocol
     *
     * Sends voice commands to external apps (WebAvanue, AVA AI, AvaConnect)
     * using the Universal IPC Protocol format via Intent broadcast.
     *
     * Uses AppIPCRegistry to determine app-specific IPC action for secure,
     * isolated communication. Each app listens on its own IPC action:
     * - WebAvanue: com.augmentalis.avanues.web.IPC.COMMAND
     * - AVA AI: com.augmentalis.ava.IPC.COMMAND
     * - AvaConnect: com.augmentalis.avaconnect.IPC.COMMAND
     *
     * Protocol: VCM:commandId:action:param1:param2
     *
     * Example usage:
     * ```kotlin
     * val command = Command(
     *     id = "scroll_top",
     *     text = "scroll to top",
     *     confidence = 0.95f
     * )
     * commandManager.executeExternalCommand(
     *     command = command,
     *     targetApp = "com.augmentalis.Avanues.web"
     * )
     * ```
     *
     * @param command Voice command to send
     * @param targetApp Package name of target app
     * @param params Optional parameters (e.g., url, text, etc.)
     */
    suspend fun executeExternalCommand(
        command: Command,
        targetApp: String,
        params: Map<String, Any> = emptyMap()
    ) {
        try {
            // Get app-specific IPC action from registry
            val ipcAction = AppIPCRegistry.getIPCActionWithFallback(targetApp)

            // Encode command to Universal IPC format
            val ipcMessage = ipcEncoder.encodeVoiceCommand(
                commandId = command.id,
                action = command.id.uppercase(),  // Convert to action format (e.g., scroll_top -> SCROLL_TOP)
                params = params
            )

            // Create Intent broadcast with app-specific action
            val intent = Intent(ipcAction).apply {
                setPackage(targetApp)
                putExtra(UniversalIPCEncoder.EXTRA_SOURCE_APP, context.packageName)
                putExtra(UniversalIPCEncoder.EXTRA_MESSAGE, ipcMessage)
            }

            // Send broadcast to target app
            context.sendBroadcast(intent)

            Log.d(TAG, "✓ Sent IPC command to $targetApp on $ipcAction: $ipcMessage")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to send IPC command to $targetApp", e)
            throw e
        }
    }

    /**
     * Get IntentDispatcher for app handler registration
     *
     * Apps use this to register action handlers for voice commands.
     * CommandManager loads commands from .vos files into SQLDelight database,
     * then routes commands to registered handlers via IntentDispatcher.
     *
     * Example usage from WebAvanue app:
     * ```kotlin
     * val commandManager = CommandManager.getInstance(context)
     * val dispatcher = commandManager.getIntentDispatcher()
     * dispatcher.registerHandler("browser") { command ->
     *     // Handle browser commands like SCROLL_TOP, ZOOM_IN, etc.
     *     browserActionMapper.execute(command)
     * }
     * ```
     *
     * @return IntentDispatcher instance for handler registration
     */
    fun getIntentDispatcher(): IntentDispatcher {
        return _intentDispatcher
    }
}