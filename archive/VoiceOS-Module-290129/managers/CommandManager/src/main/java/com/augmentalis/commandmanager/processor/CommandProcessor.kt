/**
 * CommandProcessor.kt - Command processing and execution engine
 * Process voice commands and execute appropriate actions
 */

package com.augmentalis.commandmanager.processor

import android.content.Context
import android.util.Log
import com.augmentalis.commandmanager.actions.*
import com.augmentalis.commandmanager.*
import com.augmentalis.commandmanager.definitions.CommandDefinitions
import com.augmentalis.commandmanager.loader.DatabaseCommandResolver
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.Regex

/**
 * Command processor for interpreting and executing voice commands
 * Handles command matching, parameter extraction, and action execution
 */
class CommandProcessor(
    private val context: Context
) {

    companion object {
        private const val TAG = "CommandProcessor"
    }

    // Command action registry
    private val actionRegistry = ConcurrentHashMap<String, CommandHandler>()

    // Command definitions
    private val commandDefinitions = CommandDefinitions()

    // Database command resolver
    private val databaseResolver by lazy {
        DatabaseCommandResolver.create(context)
    }

    // Current language
    private var currentLanguage = "en"

    // Command matching configuration
    private var matchThreshold = 0.7f // Minimum similarity for fuzzy matching
    private var enableFuzzyMatching = true

    // Track if database commands are loaded
    private var databaseCommandsLoaded = false

    /**
     * Initialize the command processor
     */
    suspend fun initialize() {
        // Register all action handlers
        registerActionHandlers()

        // Load built-in command definitions
        commandDefinitions.loadBuiltInCommands()

        // Load database commands
        loadDatabaseCommands()

        val totalCommands = commandDefinitions.getCommandCount()
        Log.i(TAG, "Command processor initialized with ${actionRegistry.size} actions and $totalCommands command definitions")
    }

    /**
     * Load commands from database and add to command definitions
     */
    private suspend fun loadDatabaseCommands() {
        try {
            // Get system locale with fallback
            val locale = getSystemLocale()

            // Load commands from database
            val dbCommands = databaseResolver.getAllCommandDefinitions(
                locale = locale,
                includeFallback = true
            )

            // Add database commands to definitions
            dbCommands.forEach { definition ->
                commandDefinitions.addCustomDefinition(definition)
            }

            databaseCommandsLoaded = true

            Log.i(TAG, "Loaded ${dbCommands.size} commands from database (locale: $locale)")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load database commands, using built-in only")
            databaseCommandsLoaded = false
        }
    }

    /**
     * Get system locale in format matching database locales
     */
    private fun getSystemLocale(): String {
        val locale = java.util.Locale.getDefault()
        val language = locale.language
        val country = locale.country

        return "$language-$country"
    }

    /**
     * Shutdown the command processor
     */
    suspend fun shutdown() {
        actionRegistry.clear()
        databaseCommandsLoaded = false
        Log.i(TAG, "Command processor shutdown")
    }

    /**
     * Process a text command and return the result
     */
    suspend fun processCommand(
        text: String,
        source: CommandSource,
        context: CommandContext?
    ): CommandResult {
        val startTime = System.currentTimeMillis()

        try {
            // Clean and normalize input text
            val normalizedText = normalizeText(text)

            // Find matching command
            val matchResult = findMatchingCommand(normalizedText, context)

            if (matchResult == null) {
                Log.w(TAG, "Command not recognized: '$text'")
                return CommandResult(
                    success = false,
                    command = Command(
                        id = "unknown",
                        text = text,
                        source = source,
                        context = context,
                        timestamp = System.currentTimeMillis()
                    ),
                    error = CommandError(ErrorCode.UNKNOWN_COMMAND, "Command not recognized: '$text'"),
                    executionTime = System.currentTimeMillis() - startTime
                )
            }

            val (definition, parameters, confidence) = matchResult

            // Create command object
            val command = Command(
                id = definition.id,
                text = text,
                parameters = parameters,
                source = source,
                context = context,
                confidence = confidence,
                timestamp = System.currentTimeMillis()
            )

            // Execute command
            return executeCommand(command)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: $text")
            return CommandResult(
                success = false,
                command = Command(
                    id = "error",
                    text = text,
                    source = source,
                    context = context,
                    timestamp = System.currentTimeMillis()
                ),
                error = CommandError(ErrorCode.EXECUTION_FAILED, "Processing error: ${e.message}"),
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * Execute a specific command
     */
    suspend fun executeCommand(command: Command): CommandResult {
        val startTime = System.currentTimeMillis()

        try {
            // Get action handler
            val handler = actionRegistry[command.id]

            if (handler == null) {
                return CommandResult(
                    success = false,
                    command = command,
                    error = CommandError(ErrorCode.UNKNOWN_COMMAND, "No handler for command: ${command.id}"),
                    executionTime = System.currentTimeMillis() - startTime
                )
            }

            // Validate command parameters
            val validationResult = validateCommand(command)
            if (validationResult != null) {
                return validationResult.copy(executionTime = System.currentTimeMillis() - startTime)
            }

            // Execute the command
            val result = handler(command)

            Log.d(TAG, "Executed command: ${command.id} -> ${result.success}")

            return result.copy(executionTime = System.currentTimeMillis() - startTime)

        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: ${command.id}")
            return CommandResult(
                success = false,
                command = command,
                error = CommandError(ErrorCode.EXECUTION_FAILED, "Execution error: ${e.message}"),
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * Set current language
     */
    fun setLanguage(languageCode: String) {
        currentLanguage = languageCode
        // Language is stored locally, no need to update definitions
    }

    /**
     * Get available commands for context
     * Returns all loaded commands (built-in + database)
     */
    fun getAvailableCommands(context: CommandContext?): List<CommandDefinition> {
        return if (context != null) {
            commandDefinitions.getContextualCommands(context)
        } else {
            commandDefinitions.getAllDefinitions()
        }
    }

    /**
     * Get database command statistics
     */
    suspend fun getDatabaseStats(): Map<String, Any> {
        return databaseResolver.getDatabaseStats()
    }

    /**
     * Reload database commands
     * Useful after database updates or language changes
     */
    suspend fun reloadDatabaseCommands() {
        Log.i(TAG, "Reloading database commands...")

        // Clear existing custom definitions
        // Note: CommandDefinitions doesn't have a clear method, so we reload all
        loadDatabaseCommands()
    }

    /**
     * Check if database commands are loaded
     */
    fun isDatabaseCommandsLoaded(): Boolean = databaseCommandsLoaded

    // Private methods

    /**
     * Register all action handlers
     */
    private fun registerActionHandlers() {
        // Navigation actions
        actionRegistry["nav_back"] = NavigationActions.BackAction()::invoke
        actionRegistry["nav_home"] = NavigationActions.HomeAction()::invoke
        actionRegistry["nav_recent_apps"] = NavigationActions.RecentAppsAction()::invoke
        actionRegistry["nav_notifications"] = NavigationActions.NotificationsAction()::invoke
        actionRegistry["nav_quick_settings"] = NavigationActions.QuickSettingsAction()::invoke
        actionRegistry["nav_power_dialog"] = NavigationActions.PowerDialogAction()::invoke
        actionRegistry["nav_split_screen"] = NavigationActions.SplitScreenAction()::invoke
        actionRegistry["nav_lock_screen"] = NavigationActions.LockScreenAction()::invoke
        actionRegistry["nav_screenshot"] = NavigationActions.ScreenshotAction()::invoke

        // Cursor actions - TODO: Refactor to match CursorCommandHandler pattern
        // CursorActions is now handled by CursorCommandHandler via CommandRegistry
        // These individual action classes don't exist - CursorActions is an object with suspend functions
        // actionRegistry["cursor_click"] = CursorActions.ClickAction()::invoke
        // actionRegistry["cursor_double_click"] = CursorActions.DoubleClickAction()::invoke
        // actionRegistry["cursor_long_press"] = CursorActions.LongPressAction()::invoke
        // actionRegistry["cursor_show"] = CursorActions.ShowCursorAction()::invoke
        // actionRegistry["cursor_hide"] = CursorActions.HideCursorAction()::invoke
        // actionRegistry["cursor_center"] = CursorActions.CenterCursorAction()::invoke
        // actionRegistry["cursor_hand"] = CursorActions.HandCursorAction()::invoke
        // actionRegistry["cursor_normal"] = CursorActions.NormalCursorAction()::invoke
        // actionRegistry["cursor_move"] = CursorActions.MoveCursorAction()::invoke

        // Scroll actions
        actionRegistry["scroll_up"] = ScrollActions.ScrollUpAction()::invoke
        actionRegistry["scroll_down"] = ScrollActions.ScrollDownAction()::invoke
        actionRegistry["scroll_left"] = ScrollActions.ScrollLeftAction()::invoke
        actionRegistry["scroll_right"] = ScrollActions.ScrollRightAction()::invoke
        actionRegistry["page_up"] = ScrollActions.PageUpAction()::invoke
        actionRegistry["page_down"] = ScrollActions.PageDownAction()::invoke
        actionRegistry["swipe_up"] = ScrollActions.SwipeUpAction()::invoke
        actionRegistry["swipe_down"] = ScrollActions.SwipeDownAction()::invoke
        actionRegistry["swipe_left"] = ScrollActions.SwipeLeftAction()::invoke
        actionRegistry["swipe_right"] = ScrollActions.SwipeRightAction()::invoke
        actionRegistry["scroll_to_top"] = ScrollActions.ScrollToTopAction()::invoke
        actionRegistry["scroll_to_bottom"] = ScrollActions.ScrollToBottomAction()::invoke

        // Drag actions
        actionRegistry["start_drag"] = DragActions.StartDragAction()::invoke
        actionRegistry["stop_drag"] = DragActions.StopDragAction()::invoke
        actionRegistry["drag_to"] = DragActions.DragToAction()::invoke
        actionRegistry["pinch_open"] = DragActions.PinchOpenAction()::invoke
        actionRegistry["pinch_close"] = DragActions.PinchCloseAction()::invoke
        actionRegistry["zoom_in"] = DragActions.ZoomInAction()::invoke
        actionRegistry["zoom_out"] = DragActions.ZoomOutAction()::invoke
        actionRegistry["rotate"] = DragActions.RotateAction()::invoke

        // Volume actions
        actionRegistry["volume_up"] = VolumeActions.VolumeUpAction()::invoke
        actionRegistry["volume_down"] = VolumeActions.VolumeDownAction()::invoke
        actionRegistry["mute"] = VolumeActions.MuteAction()::invoke
        actionRegistry["unmute"] = VolumeActions.UnmuteAction()::invoke
        actionRegistry["max_volume"] = VolumeActions.MaxVolumeAction()::invoke
        actionRegistry["min_volume"] = VolumeActions.MinVolumeAction()::invoke
        actionRegistry["set_volume_level"] = VolumeActions.SetVolumeLevelAction()::invoke
        actionRegistry["get_volume"] = VolumeActions.GetVolumeAction()::invoke

        // Volume levels 1-15
        for (level in 1..15) {
            actionRegistry["volume_level_$level"] = when (level) {
                1 -> VolumeActions.VolumeLevel1Action()::invoke
                2 -> VolumeActions.VolumeLevel2Action()::invoke
                3 -> VolumeActions.VolumeLevel3Action()::invoke
                4 -> VolumeActions.VolumeLevel4Action()::invoke
                5 -> VolumeActions.VolumeLevel5Action()::invoke
                6 -> VolumeActions.VolumeLevel6Action()::invoke
                7 -> VolumeActions.VolumeLevel7Action()::invoke
                8 -> VolumeActions.VolumeLevel8Action()::invoke
                9 -> VolumeActions.VolumeLevel9Action()::invoke
                10 -> VolumeActions.VolumeLevel10Action()::invoke
                11 -> VolumeActions.VolumeLevel11Action()::invoke
                12 -> VolumeActions.VolumeLevel12Action()::invoke
                13 -> VolumeActions.VolumeLevel13Action()::invoke
                14 -> VolumeActions.VolumeLevel14Action()::invoke
                15 -> VolumeActions.VolumeLevel15Action()::invoke
                else -> VolumeActions.SetVolumeLevelAction()::invoke
            }
        }

        // Dictation actions
        actionRegistry["dictation_start"] = DictationActions.StartDictationAction()::invoke
        actionRegistry["dictation_end"] = DictationActions.EndDictationAction()::invoke
        actionRegistry["dictate_text"] = DictationActions.DictateTextAction()::invoke
        actionRegistry["show_keyboard"] = DictationActions.ShowKeyboardAction()::invoke
        actionRegistry["hide_keyboard"] = DictationActions.HideKeyboardAction()::invoke
        actionRegistry["backspace"] = DictationActions.BackspaceAction()::invoke
        actionRegistry["clear_text"] = DictationActions.ClearTextAction()::invoke
        actionRegistry["enter"] = DictationActions.EnterAction()::invoke
        actionRegistry["space"] = DictationActions.SpaceAction()::invoke
        actionRegistry["tab"] = DictationActions.TabAction()::invoke
        actionRegistry["type_text"] = DictationActions.TypeTextAction()::invoke
        actionRegistry["insert_symbol"] = DictationActions.InsertSymbolAction()::invoke

        // System actions
        actionRegistry["wifi_toggle"] = SystemActions.WifiToggleAction()::invoke
        actionRegistry["wifi_enable"] = SystemActions.WifiEnableAction()::invoke
        actionRegistry["wifi_disable"] = SystemActions.WifiDisableAction()::invoke
        actionRegistry["bluetooth_toggle"] = SystemActions.BluetoothToggleAction()::invoke
        actionRegistry["bluetooth_enable"] = SystemActions.BluetoothEnableAction()::invoke
        actionRegistry["bluetooth_disable"] = SystemActions.BluetoothDisableAction()::invoke
        actionRegistry["open_settings"] = SystemActions.OpenSettingsAction()::invoke
        actionRegistry["device_info"] = SystemActions.DeviceInfoAction()::invoke
        actionRegistry["battery_status"] = SystemActions.BatteryStatusAction()::invoke
        actionRegistry["network_status"] = SystemActions.NetworkStatusAction()::invoke
        actionRegistry["storage_info"] = SystemActions.StorageInfoAction()::invoke

        // Overlay actions
        actionRegistry["show_overlay"] = OverlayActions.ShowOverlayAction()::invoke
        actionRegistry["hide_overlay"] = OverlayActions.HideOverlayAction()::invoke
        actionRegistry["toggle_overlay"] = OverlayActions.ToggleOverlayAction()::invoke
        actionRegistry["show_command_hints"] = OverlayActions.ShowCommandHintsAction()::invoke
        actionRegistry["hide_command_hints"] = OverlayActions.HideCommandHintsAction()::invoke
        actionRegistry["show_help"] = OverlayActions.ShowHelpAction()::invoke
        actionRegistry["hide_help"] = OverlayActions.HideHelpAction()::invoke
        actionRegistry["list_commands"] = OverlayActions.ListCommandsAction()::invoke
        actionRegistry["show_status"] = OverlayActions.ShowStatusAction()::invoke
        actionRegistry["set_overlay_position"] = OverlayActions.SetOverlayPositionAction()::invoke
        actionRegistry["set_overlay_size"] = OverlayActions.SetOverlaySizeAction()::invoke
        actionRegistry["set_overlay_transparency"] = OverlayActions.SetOverlayTransparencyAction()::invoke

        // App actions
        actionRegistry["open_app"] = AppActions.OpenAppAction()::invoke
        actionRegistry["close_app"] = AppActions.CloseAppAction()::invoke
        actionRegistry["switch_app"] = AppActions.SwitchAppAction()::invoke
        actionRegistry["list_running_apps"] = AppActions.ListRunningAppsAction()::invoke
        actionRegistry["find_app"] = AppActions.FindAppAction()::invoke
        actionRegistry["app_info"] = AppActions.AppInfoAction()::invoke
        actionRegistry["force_stop_app"] = AppActions.ForceStopAppAction()::invoke

        // Text actions
        actionRegistry["copy_text"] = TextActions.CopyTextAction()::invoke
        actionRegistry["cut_text"] = TextActions.CutTextAction()::invoke
        actionRegistry["paste_text"] = TextActions.PasteTextAction()::invoke
        actionRegistry["select_all"] = TextActions.SelectAllAction()::invoke
        actionRegistry["select_text"] = TextActions.SelectTextAction()::invoke
        actionRegistry["replace_text"] = TextActions.ReplaceTextAction()::invoke
        actionRegistry["find_text"] = TextActions.FindTextAction()::invoke
        actionRegistry["get_text"] = TextActions.GetTextAction()::invoke
        actionRegistry["insert_text"] = TextActions.InsertTextAction()::invoke
        actionRegistry["undo"] = TextActions.UndoAction()::invoke
        actionRegistry["redo"] = TextActions.RedoAction()::invoke

        android.util.Log.d(TAG, "Registered ${actionRegistry.size} action handlers")
    }

    /**
     * Normalize input text for better matching
     */
    private fun normalizeText(text: String): String {
        return text.lowercase()
            .trim()
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
            .replace(Regex("[^a-z0-9\\s]"), "") // Remove special characters except spaces
    }

    /**
     * Find matching command for given text
     */
    private fun findMatchingCommand(
        text: String,
        context: CommandContext?
    ): CommandMatchResult? {
        val availableCommands = getAvailableCommands(context)

        var bestMatch: CommandMatchResult? = null
        var bestScore = 0f

        for (definition in availableCommands) {
            val patterns = definition.patterns

            for (pattern in patterns) {
                val normalizedPattern = normalizeText(pattern)

                // Exact match
                if (text == normalizedPattern) {
                    val parameters = extractParameters(text, pattern, definition.parameters)
                    return CommandMatchResult(definition, parameters, 1.0f)
                }

                // Starts with match
                if (text.startsWith(normalizedPattern)) {
                    val parameters = extractParameters(text, pattern, definition.parameters)
                    val score = 0.9f
                    if (score > bestScore) {
                        bestScore = score
                        bestMatch = CommandMatchResult(definition, parameters, score)
                    }
                }

                // Contains match
                if (text.contains(normalizedPattern)) {
                    val parameters = extractParameters(text, pattern, definition.parameters)
                    val score = 0.8f
                    if (score > bestScore) {
                        bestScore = score
                        bestMatch = CommandMatchResult(definition, parameters, score)
                    }
                }

                // Fuzzy matching
                if (enableFuzzyMatching) {
                    val similarity = calculateSimilarity(text, normalizedPattern)
                    if (similarity >= matchThreshold && similarity > bestScore) {
                        val parameters = extractParameters(text, pattern, definition.parameters)
                        bestScore = similarity
                        bestMatch = CommandMatchResult(definition, parameters, similarity)
                    }
                }
            }
        }

        return bestMatch
    }

    /**
     * Extract parameters from command text
     */
    private fun extractParameters(
        text: String,
        phrase: String,
        parameterDefs: List<CommandParameter>
    ): Map<String, Any> {
        val parameters = mutableMapOf<String, Any>()

        // Extract text after the command phrase
        val normalizedPhrase = normalizeText(phrase)
        val remainingText = text.removePrefix(normalizedPhrase).trim()

        if (remainingText.isNotEmpty() && parameterDefs.isNotEmpty()) {
            // Simple parameter extraction - can be enhanced
            for (paramDef in parameterDefs) {
                when (paramDef.type) {
                    ParameterType.STRING -> {
                        if (paramDef.name == "text" || paramDef.name == "target") {
                            parameters[paramDef.name] = remainingText
                        }
                    }
                    ParameterType.NUMBER -> {
                        val numberMatch = Regex("\\d+").find(remainingText)
                        numberMatch?.let {
                            parameters[paramDef.name] = it.value.toIntOrNull() ?: (paramDef.defaultValue ?: 0)
                        }
                    }
                    ParameterType.BOOLEAN -> {
                        val boolValue = when {
                            remainingText.contains("true") || remainingText.contains("yes") -> true
                            remainingText.contains("false") || remainingText.contains("no") -> false
                            else -> paramDef.defaultValue as? Boolean ?: false
                        }
                        parameters[paramDef.name] = boolValue
                    }
                    else -> {
                        // Add default value if available
                        paramDef.defaultValue?.let { parameters[paramDef.name] = it }
                    }
                }
            }
        }

        // Add default values for missing required parameters
        for (paramDef in parameterDefs) {
            if (paramDef.required && !parameters.containsKey(paramDef.name)) {
                paramDef.defaultValue?.let { parameters[paramDef.name] = it }
            }
        }

        return parameters
    }

    /**
     * Calculate text similarity for fuzzy matching
     */
    private fun calculateSimilarity(text1: String, text2: String): Float {
        val longer = if (text1.length > text2.length) text1 else text2
        val shorter = if (text1.length > text2.length) text2 else text1

        if (longer.isEmpty()) return 1.0f

        val editDistance = calculateEditDistance(longer, shorter)
        return (longer.length - editDistance) / longer.length.toFloat()
    }

    /**
     * Calculate edit distance (Levenshtein distance)
     */
    private fun calculateEditDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)

        for (i in 0..s1.length) {
            var lastValue = i
            for (j in 0..s2.length) {
                if (i == 0) {
                    costs[j] = j
                } else if (j > 0) {
                    var newValue = costs[j - 1]
                    if (s1[i - 1] != s2[j - 1]) {
                        newValue = minOf(newValue, lastValue, costs[j]) + 1
                    }
                    costs[j - 1] = lastValue
                    lastValue = newValue
                }
            }
            if (i > 0) costs[s2.length] = lastValue
        }

        return costs[s2.length]
    }

    /**
     * Validate command parameters
     */
    private fun validateCommand(command: Command): CommandResult? {
        val definition = commandDefinitions.getAllDefinitions()
            .find { it.id == command.id }

        if (definition == null) {
            return CommandResult(
                success = false,
                command = command,
                error = CommandError(ErrorCode.UNKNOWN_COMMAND, "Command definition not found: ${command.id}")
            )
        }

        // Check required parameters
        for (paramDef in definition.parameters) {
            if (paramDef.required && !command.parameters.containsKey(paramDef.name)) {
                return CommandResult(
                    success = false,
                    command = command,
                    error = CommandError(ErrorCode.INVALID_PARAMETERS, "Missing required parameter: ${paramDef.name}")
                )
            }
        }

        return null // No validation errors
    }
}

/**
 * Command matching result
 */
private data class CommandMatchResult(
    val definition: CommandDefinition,
    val parameters: Map<String, Any>,
    val confidence: Float
)