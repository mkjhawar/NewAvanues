/**
 * ActionCoordinator.kt - Coordinates action execution across handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-08 - Consolidated dynamic command support (CommandRegistry + fuzzy matching)
 *
 * KMP coordinator for managing handler registration and command execution.
 * Now supports both static handlers AND dynamic screen-specific commands.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.Command
import com.augmentalis.voiceoscore.CommandError
import com.augmentalis.voiceoscore.CommandResult
import com.augmentalis.voiceoscore.CommandSource
import com.augmentalis.voiceoscore.ErrorCode
import com.augmentalis.voiceoscore.command.LocalizedVerbProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Coordinates action execution across multiple handlers.
 *
 * Provides:
 * - Handler registration and lifecycle management
 * - Priority-based command routing
 * - Dynamic command support (screen-specific commands with AVIDs)
 * - Fuzzy matching for voice input variations
 * - Performance metrics collection
 * - Voice command interpretation
 *
 * ## Execution Priority:
 * 1. Dynamic command lookup by AVID (fastest, most accurate)
 * 2. Dynamic command fuzzy match (handles voice variations)
 * 3. Static handler lookup (system commands)
 * 4. Voice interpreter fallback (legacy keyword mapping)
 */
class ActionCoordinator(
    private val voiceInterpreter: IVoiceCommandInterpreter = DefaultVoiceCommandInterpreter,
    private val handlerRegistry: IHandlerRegistry = HandlerRegistry(),
    private val commandRegistry: CommandRegistry = CommandRegistry(),
    private val metrics: IMetricsCollector = MetricsCollector()
) {
    companion object {
        private const val TAG = "ActionCoordinator"
        private const val HANDLER_TIMEOUT_MS = 5000L
        private const val DEFAULT_FUZZY_THRESHOLD = 0.7f
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
    }

    /**
     * Current number of dynamic commands registered.
     */
    val dynamicCommandCount: Int get() = commandRegistry.size

    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // State flow for coordinator status
    private val _state = MutableStateFlow(CoordinatorState.UNINITIALIZED)
    val state: StateFlow<CoordinatorState> = _state.asStateFlow()

    // Event flow for execution results
    private val _results = MutableSharedFlow<ActionCommandResult>(extraBufferCapacity = 64)
    val results: SharedFlow<ActionCommandResult> = _results.asSharedFlow()

    /**
     * Initialize the coordinator with handlers.
     *
     * @param handlers List of handlers to register
     */
    suspend fun initialize(handlers: List<IHandler>) {
        _state.value = CoordinatorState.INITIALIZING

        try {
            // Register all handlers
            handlers.forEach { handler ->
                handlerRegistry.register(handler)
            }

            // Initialize all handlers
            val initCount = handlerRegistry.initializeAll()

            _state.value = CoordinatorState.READY
        } catch (e: Exception) {
            _state.value = CoordinatorState.ERROR
            throw e
        }
    }

    /**
     * Register a handler.
     */
    suspend fun registerHandler(handler: IHandler) {
        handlerRegistry.register(handler)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Dynamic Command Management
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Update dynamic commands from screen scraping.
     *
     * Call this after each screen scan to register the current screen's
     * actionable elements as voice commands.
     *
     * @param commands List of quantized commands from UI elements
     */
    suspend fun updateDynamicCommands(commands: List<QuantizedCommand>) {
        commandRegistry.update(commands)
    }

    /**
     * Update dynamic commands from a specific source, preserving other sources' commands.
     *
     * This is the source-aware variant of [updateDynamicCommands].
     * Each source (e.g., "accessibility", "web") manages its own set of commands.
     * Calling this with source="web" replaces only web commands; accessibility
     * commands remain untouched, and vice versa.
     *
     * @param source Source identifier (e.g., "accessibility", "web")
     * @param commands List of quantized commands from this source
     */
    suspend fun updateDynamicCommandsBySource(source: String, commands: List<QuantizedCommand>) {
        commandRegistry.updateBySourceSuspend(source, commands)
    }

    /**
     * Clear dynamic commands from a specific source only.
     * Other sources' commands remain untouched.
     *
     * @param source Source identifier to clear
     */
    fun clearDynamicCommandsBySource(source: String) {
        commandRegistry.clearBySource(source)
    }

    /**
     * Clear all dynamic commands.
     * Call when leaving an app or screen context is invalid.
     */
    fun clearDynamicCommands() {
        commandRegistry.clear()
    }

    /**
     * Get all current dynamic commands.
     */
    fun getDynamicCommands(): List<QuantizedCommand> {
        return commandRegistry.all()
    }

    /**
     * Process a quantized command.
     *
     * @param command The command to process
     * @return HandlerResult from execution
     */
    suspend fun processCommand(command: QuantizedCommand): HandlerResult {
        val startTime = currentTimeMillis()
        LoggingUtils.d("processCommand: phrase='${command.phrase}', actionType=${command.actionType}, bounds=${command.metadata["bounds"]}", TAG)

        // Find handler — web-source commands bypass priority-based routing.
        // Without this bypass, SystemHandler (priority 1) or AndroidGestureHandler
        // (priority 2) steal overlapping phrases ("go back", "swipe up") before
        // WebCommandHandler (BROWSER, priority 11) gets a chance.
        val handler = if (command.metadata["source"] == "web") {
            handlerRegistry.getHandlersForCategory(ActionCategory.BROWSER)
                .firstOrNull { it.canHandle(command) }
                ?: handlerRegistry.findHandler(command)  // Fallback to priority scan
        } else {
            handlerRegistry.findHandler(command)
        }
        LoggingUtils.d("findHandler result: ${handler?.let { it::class.simpleName } ?: "null"}", TAG)
        if (handler == null) {
            val result = HandlerResult.failure("No handler found for: ${command.phrase}")
            LoggingUtils.w("No handler found for '${command.phrase}'", TAG)
            recordResult(command, result, currentTimeMillis() - startTime)
            return result
        }

        // Execute with timeout
        return try {
            val result = withTimeoutOrNull(HANDLER_TIMEOUT_MS) {
                handler.execute(command)
            } ?: HandlerResult.failure("Handler timed out", recoverable = true)

            recordResult(command, result, currentTimeMillis() - startTime)
            result
        } catch (e: Exception) {
            val result = HandlerResult.failure("Execution error: ${e.message}", recoverable = true)
            recordResult(command, result, currentTimeMillis() - startTime)
            result
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Verb Extraction for Dynamic Commands
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Known action verbs that can prefix a command.
     * User says: "click 4" or "tap Submit" or just "4"
     *
     * Populated from [LocalizedVerbProvider] which includes:
     * - Built-in English verbs (always available)
     * - Locale-specific verbs from VOS files (after VOS load)
     * Already sorted by length descending for longest-match-first extraction.
     */
    private val actionVerbs: List<String>
        get() = LocalizedVerbProvider.getActionVerbs()

    /**
     * Convert CommandActionType to a verb phrase for handler routing.
     * This allows dynamic commands to work like static commands -
     * the actionType determines what action is performed.
     *
     * @param actionType The action type from the command
     * @param target The target element (for element actions)
     * @return Verb phrase for handler routing
     */
    private fun actionTypeToPhrase(actionType: CommandActionType, target: String): String {
        return when (actionType) {
            // Element actions - need target
            CommandActionType.CLICK, CommandActionType.TAP -> "tap $target"
            CommandActionType.LONG_CLICK -> "long press $target"
            CommandActionType.EXECUTE -> "tap $target"  // Default execute to tap
            CommandActionType.FOCUS -> "focus $target"
            CommandActionType.TYPE -> "type $target"

            // Scroll actions
            CommandActionType.SCROLL_DOWN -> "scroll down"
            CommandActionType.SCROLL_UP -> "scroll up"
            CommandActionType.SCROLL_LEFT -> "scroll left"
            CommandActionType.SCROLL_RIGHT -> "scroll right"
            CommandActionType.SCROLL -> "scroll down"  // Default direction

            // Navigation actions - no target needed
            CommandActionType.BACK -> "back"
            CommandActionType.HOME -> "home"
            CommandActionType.RECENT_APPS -> "recent apps"
            CommandActionType.APP_DRAWER -> "app drawer"
            CommandActionType.NAVIGATE -> "tap $target"

            // System actions
            CommandActionType.OPEN_SETTINGS -> "settings"
            CommandActionType.NOTIFICATIONS -> "notifications"
            CommandActionType.CLEAR_NOTIFICATIONS -> "clear notifications"
            CommandActionType.SCREENSHOT -> "screenshot"
            CommandActionType.FLASHLIGHT_ON -> "flashlight on"
            CommandActionType.FLASHLIGHT_OFF -> "flashlight off"

            // Media actions
            CommandActionType.MEDIA_PLAY -> "play"
            CommandActionType.MEDIA_PAUSE -> "pause"
            CommandActionType.MEDIA_NEXT -> "next"
            CommandActionType.MEDIA_PREVIOUS -> "previous"
            CommandActionType.VOLUME_UP -> "volume up"
            CommandActionType.VOLUME_DOWN -> "volume down"
            CommandActionType.VOLUME_MUTE -> "mute"

            // VoiceOS actions
            CommandActionType.VOICE_MUTE -> "voice off"
            CommandActionType.VOICE_WAKE -> "voice on"
            CommandActionType.DICTATION_START -> "dictation"
            CommandActionType.DICTATION_STOP -> "command mode"
            CommandActionType.SHOW_COMMANDS -> "help"
            CommandActionType.NUMBERS_ON -> "numbers on"
            CommandActionType.NUMBERS_OFF -> "numbers off"
            CommandActionType.NUMBERS_AUTO -> "numbers auto"

            // App actions
            CommandActionType.OPEN_APP -> "open $target"
            CommandActionType.CLOSE_APP -> "close app"

            // Text/clipboard actions
            CommandActionType.SELECT_ALL -> "select all"
            CommandActionType.COPY -> "copy"
            CommandActionType.PASTE -> "paste"
            CommandActionType.CUT -> "cut"
            CommandActionType.UNDO -> "undo"
            CommandActionType.REDO -> "redo"
            CommandActionType.DELETE -> "delete"

            // Screen/display actions
            CommandActionType.BRIGHTNESS_UP -> "brightness up"
            CommandActionType.BRIGHTNESS_DOWN -> "brightness down"
            CommandActionType.LOCK_SCREEN -> "lock screen"
            CommandActionType.ROTATE_SCREEN -> "rotate screen"
            CommandActionType.ZOOM_IN -> "zoom in"
            CommandActionType.ZOOM_OUT -> "zoom out"

            // Connectivity actions
            CommandActionType.TOGGLE_WIFI -> "toggle wifi"
            CommandActionType.TOGGLE_BLUETOOTH -> "toggle bluetooth"

            // Cursor actions
            CommandActionType.CURSOR_SHOW -> "show cursor"
            CommandActionType.CURSOR_HIDE -> "hide cursor"
            CommandActionType.CURSOR_CLICK -> "cursor click"

            // Reading/TTS actions
            CommandActionType.READ_SCREEN -> "read screen"
            CommandActionType.STOP_READING -> "stop reading"

            // Input actions
            CommandActionType.SHOW_KEYBOARD -> "show keyboard"
            CommandActionType.HIDE_KEYBOARD -> "hide keyboard"

            // Browser actions
            CommandActionType.RETRAIN_PAGE -> "retrain page"
            CommandActionType.PAGE_BACK -> "go back"
            CommandActionType.PAGE_FORWARD -> "go forward"
            CommandActionType.PAGE_REFRESH -> "refresh page"

            // Page scrolling
            CommandActionType.SCROLL_TO_TOP -> "go to top"
            CommandActionType.SCROLL_TO_BOTTOM -> "go to bottom"

            // Form navigation
            CommandActionType.TAB_NEXT -> "next field"
            CommandActionType.TAB_PREV -> "previous field"
            CommandActionType.SUBMIT_FORM -> "submit form"

            // Gesture actions
            CommandActionType.SWIPE_LEFT -> "swipe left"
            CommandActionType.SWIPE_RIGHT -> "swipe right"
            CommandActionType.SWIPE_UP -> "swipe up"
            CommandActionType.SWIPE_DOWN -> "swipe down"
            CommandActionType.GRAB -> "grab $target"
            CommandActionType.RELEASE -> "release"
            CommandActionType.ROTATE -> "rotate $target"
            CommandActionType.DRAG -> "drag $target"
            CommandActionType.DOUBLE_CLICK -> "double tap $target"
            CommandActionType.HOVER -> "hover $target"

            // Advanced gesture actions
            CommandActionType.PAN -> "pan"
            CommandActionType.TILT -> "tilt"
            CommandActionType.ORBIT -> "orbit"
            CommandActionType.ROTATE_X -> "rotate x"
            CommandActionType.ROTATE_Y -> "rotate y"
            CommandActionType.ROTATE_Z -> "rotate z"
            CommandActionType.PINCH -> "pinch"
            CommandActionType.FLING -> "fling"
            CommandActionType.THROW -> "throw"
            CommandActionType.SCALE -> "scale"
            CommandActionType.RESET_ZOOM -> "reset zoom"
            CommandActionType.SELECT_WORD -> "select word"
            CommandActionType.CLEAR_SELECTION -> "clear selection"
            CommandActionType.HOVER_OUT -> "hover out"

            // Drawing/annotation actions
            CommandActionType.STROKE_START -> "start drawing"
            CommandActionType.STROKE_END -> "stop drawing"
            CommandActionType.ERASE -> "eraser"

            // Default for custom/unknown
            CommandActionType.CUSTOM -> "tap $target"
            CommandActionType.MACRO -> "not implemented"
        }
    }

    /**
     * Extract verb and target from voice input.
     *
     * Examples:
     * - "click 4" -> Pair("click", "4")
     * - "tap Submit" -> Pair("tap", "submit")
     * - "long press delete" -> Pair("long press", "delete")
     * - "4" -> Pair(null, "4")
     * - "scroll down" -> Pair(null, null) - this is a static command
     *
     * @return Pair of (verb, target) or (null, target) if no verb, or (null, null) if static command
     */
    private fun extractVerbAndTarget(voiceInput: String): Pair<String?, String?> {
        val normalized = voiceInput.lowercase().trim()

        // Try to match action verbs (longest first to match "long press" before "press").
        // actionVerbs from LocalizedVerbProvider is pre-sorted by length descending.
        for (verb in actionVerbs) {
            if (normalized.startsWith("$verb ")) {
                val target = normalized.removePrefix("$verb ").trim()
                return if (target.isNotBlank()) Pair(verb, target) else Pair(null, null)
            }
        }

        // No verb found - could be just the target ("4") or a static command ("scroll down")
        // Check if it looks like a target (not a known static command phrase)
        val staticCommand = StaticCommandRegistry.findByPhrase(normalized)
        return if (staticCommand != null) {
            Pair(null, null)  // It's a static command
        } else {
            Pair(null, normalized)  // It's just the target (e.g., "4", "Submit")
        }
    }

    /**
     * Process a voice command string with full dynamic command support.
     *
     * Commands in registry are stored WITHOUT verbs (e.g., "4", "Submit", "More options").
     * User provides verb at runtime: "click 4", "tap Submit", or just "4".
     *
     * Execution priority:
     * 1. Dynamic command by target match (extracts verb, matches target in registry)
     * 2. Dynamic command by fuzzy match (handles voice variations)
     * 3. Static handler match (system commands)
     * 4. Voice interpreter fallback (legacy keyword mapping)
     *
     * Note: NLU/LLM integration happens at platform level (VoiceOSCore androidMain/iosMain)
     *
     * @param text The voice command text
     * @param confidence Confidence level (0-1)
     * @return HandlerResult from execution
     */
    suspend fun processVoiceCommand(text: String, confidence: Float = 1.0f): HandlerResult {
        val normalizedText = text.lowercase().trim()
        LoggingUtils.d("processVoiceCommand: '$normalizedText' (conf: $confidence)", TAG)

        // ═══════════════════════════════════════════════════════════════════
        // Step 1: Try dynamic command lookup (has AVID for direct execution)
        // ═══════════════════════════════════════════════════════════════════
        LoggingUtils.d("Dynamic command registry size: ${commandRegistry.size}", TAG)
        if (commandRegistry.size > 0) {
            // Pre-check: Full-phrase match for web-source commands.
            // Web static commands ("go back", "swipe up") are registered as dynamic
            // commands with source="web" when browser is active. This catches them
            // before extractVerbAndTarget short-circuits on StaticCommandRegistry.
            val webFullPhraseMatch = commandRegistry.findByPhrase(normalizedText)
            if (webFullPhraseMatch != null && webFullPhraseMatch.metadata["source"] == "web") {
                LoggingUtils.d("Web full-phrase match: '${webFullPhraseMatch.phrase}'", TAG)
                return processCommand(webFullPhraseMatch)
            }

            // Extract verb and target from voice input
            // e.g., "click 4" -> verb="click", target="4"
            val (verb, target) = extractVerbAndTarget(normalizedText)
            LoggingUtils.d("Extracted verb='$verb', target='$target'", TAG)

            if (target != null) {
                // Try exact match with extracted target
                val exactMatch = commandRegistry.findByPhrase(target)
                LoggingUtils.d("findByPhrase('$target') = ${exactMatch?.phrase ?: "null"}", TAG)
                if (exactMatch != null) {
                    // Web-sourced commands: pass directly without phrase rewriting.
                    // WebCommandHandler uses metadata (selector, xpath) for JS execution,
                    // not accessibility gesture bounds. Rewriting would route to AndroidGestureHandler.
                    if (exactMatch.metadata["source"] == "web") {
                        LoggingUtils.d("Web command match! phrase='${exactMatch.phrase}', selector=${exactMatch.metadata["selector"]}", TAG)
                        return processCommand(exactMatch)
                    }

                    // Native commands: rewrite phrase for gesture handler routing.
                    // Localized verbs are normalized to canonical English so handlers
                    // always receive English phrases (e.g., "pulsar 4" → "click 4").
                    val actionPhrase = if (verb != null) {
                        val canonical = LocalizedVerbProvider.canonicalVerbFor(verb)
                        "$canonical $target"
                    } else {
                        actionTypeToPhrase(exactMatch.actionType, target)
                    }
                    val actionCommand = exactMatch.copy(phrase = actionPhrase)
                    LoggingUtils.d("Dynamic command match! phrase='$actionPhrase', actionType=${exactMatch.actionType}, bounds=${exactMatch.metadata["bounds"]}", TAG)
                    return processCommand(actionCommand)
                }

                // Then try fuzzy matching on target only
                val matchResult = CommandMatcher.match(
                    voiceInput = target,  // Match against target, not full input
                    registry = commandRegistry,
                    threshold = DEFAULT_FUZZY_THRESHOLD
                )

                when (matchResult) {
                    is CommandMatcher.MatchResult.Exact -> {
                        val matched = matchResult.command
                        if (matched.metadata["source"] == "web") {
                            return processCommand(matched)
                        }
                        val actionPhrase = if (verb != null) {
                            val canonical = LocalizedVerbProvider.canonicalVerbFor(verb)
                            "$canonical $target"
                        } else {
                            actionTypeToPhrase(matched.actionType, target)
                        }
                        val cmd = matched.copy(phrase = actionPhrase)
                        return processCommand(cmd)
                    }
                    is CommandMatcher.MatchResult.Fuzzy -> {
                        if (matchResult.confidence >= HIGH_CONFIDENCE_THRESHOLD) {
                            val matched = matchResult.command
                            if (matched.metadata["source"] == "web") {
                                return processCommand(matched)
                            }
                            val actionPhrase = if (verb != null) {
                                val canonical = LocalizedVerbProvider.canonicalVerbFor(verb)
                                "$canonical $target"
                            } else {
                                actionTypeToPhrase(matched.actionType, target)
                            }
                            val cmd = matched.copy(phrase = actionPhrase)
                            return processCommand(cmd)
                        }
                        // Low confidence fuzzy match - continue to NLU
                    }
                    is CommandMatcher.MatchResult.Ambiguous -> {
                        // Return ambiguous result - caller can show disambiguation UI
                        return HandlerResult.awaitingSelection(
                            message = "${matchResult.candidates.size} matches found. Please be more specific.",
                            matchCount = matchResult.candidates.size,
                            accessibilityAnnouncement = "Multiple matches. Say a number to select."
                        )
                    }
                    is CommandMatcher.MatchResult.NoMatch -> {
                        // Fall through to static handlers
                    }
                }
            }
            // If no target extracted (e.g., just "scroll down"), fall through to static handlers
        }

        // ═══════════════════════════════════════════════════════════════════
        // Step 2: Try static handler lookup
        // ═══════════════════════════════════════════════════════════════════
        LoggingUtils.d("No dynamic match, trying static handlers", TAG)
        val directCommand = QuantizedCommand(
            phrase = normalizedText,
            actionType = CommandActionType.EXECUTE,
            targetAvid = null,
            confidence = confidence
        )

        val canHandle = handlerRegistry.canHandle(normalizedText)
        LoggingUtils.d("handlerRegistry.canHandle('$normalizedText') = $canHandle", TAG)
        if (canHandle) {
            return processCommand(directCommand)
        }

        // ═══════════════════════════════════════════════════════════════════
        // Step 3: Try voice interpreter (keyword fallback)
        // ═══════════════════════════════════════════════════════════════════
        val interpretedAction = interpretVoiceCommand(normalizedText)
        if (interpretedAction != null) {
            val interpretedCommand = directCommand.copy(phrase = interpretedAction)
            return processCommand(interpretedCommand)
        }

        return HandlerResult.failure("Unknown command: $text")
    }

    /**
     * Interpret natural language voice commands into action strings.
     */
    private fun interpretVoiceCommand(command: String): String? {
        return voiceInterpreter.interpret(command)
    }

    /**
     * Check if any handler can handle the command.
     * Checks both dynamic commands and static handlers.
     */
    suspend fun canHandle(command: String): Boolean {
        val normalized = command.lowercase().trim()

        // Check dynamic commands first
        if (commandRegistry.findByPhrase(normalized) != null) {
            return true
        }

        // Check static handlers
        return handlerRegistry.canHandle(normalized)
    }

    /**
     * Get all supported actions.
     * Returns both dynamic commands and static handler actions.
     */
    suspend fun getAllSupportedActions(): List<String> {
        val staticActions = handlerRegistry.getAllSupportedActions()
        val dynamicActions = commandRegistry.all().map { it.phrase }
        return staticActions + dynamicActions
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NLU/LLM Integration - Unified Command Access
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all commands (static + dynamic) as QuantizedCommand for NLU/LLM.
     *
     * This provides a unified view of all available voice commands:
     * - Static commands: System-wide commands (targetAvid = null)
     * - Dynamic commands: Screen-specific element commands (targetAvid = element AVID)
     *
     * The NLU/LLM can use this to:
     * - Understand available actions
     * - Match user intent to commands
     * - Generate appropriate responses
     *
     * @return List of all available QuantizedCommand
     */
    fun getAllQuantizedCommands(): List<QuantizedCommand> {
        val staticCommands = StaticCommandRegistry.allAsQuantized()
        val dynamicCommands = commandRegistry.all()
        return staticCommands + dynamicCommands
    }

    /**
     * Get only static commands as QuantizedCommand.
     *
     * Static commands are always available regardless of screen context.
     *
     * @return List of static QuantizedCommand
     */
    fun getStaticQuantizedCommands(): List<QuantizedCommand> {
        return StaticCommandRegistry.allAsQuantized()
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
        val commands = mutableListOf<QuantizedCommand>()

        if (includeStatic) {
            commands.addAll(StaticCommandRegistry.allAsQuantized())
        }
        if (includeDynamic) {
            commands.addAll(commandRegistry.all())
        }

        return commands.joinToString("\n") { it.toCmdLine() }
    }

    /**
     * Get NLU schema for LLM context.
     *
     * Returns a human-readable schema suitable for LLM prompts,
     * describing all available commands grouped by category.
     *
     * @return Formatted NLU schema string
     */
    fun getNluSchema(): String {
        return buildString {
            append(StaticCommandRegistry.toNluSchema())
            appendLine()
            appendLine("## Dynamic Commands (Current Screen)")
            val dynamicCommands = commandRegistry.all()
            if (dynamicCommands.isEmpty()) {
                appendLine("(No screen-specific commands available)")
            } else {
                dynamicCommands.forEach { cmd ->
                    appendLine("- ${cmd.phrase}: ${cmd.actionType.name} -> AVID:${cmd.targetAvid}")
                }
            }
        }
    }

    /**
     * Record execution result for metrics.
     */
    private suspend fun recordResult(command: QuantizedCommand, result: HandlerResult, durationMs: Long) {
        val timestamp = currentTimeMillis()
        val actionResult = ActionCommandResult(
            command = command,
            result = result,
            durationMs = durationMs,
            timestamp = timestamp
        )

        _results.emit(actionResult)

        // Convert to CommandResult for metrics recording
        val metricsResult = CommandResult(
            success = result.isSuccess,
            command = Command(
                id = command.avid,
                text = command.phrase,
                source = CommandSource.VOICE,
                timestamp = timestamp,
                confidence = 1.0f
            ),
            response = when (result) {
                is HandlerResult.Success -> result.message
                is HandlerResult.Failure -> result.reason
                else -> null
            },
            error = if (result is HandlerResult.Failure) {
                CommandError(
                    code = ErrorCode.EXECUTION_FAILED,
                    message = result.reason
                )
            } else null,
            executionTime = durationMs
        )
        metrics.record(metricsResult)
    }

    /**
     * Get metrics summary.
     */
    fun getMetricsSummary(): MetricsSummary {
        return metrics.getSummary()
    }

    /**
     * Reset all metrics.
     */
    fun resetMetrics() {
        metrics.reset()
    }

    /**
     * Dispose the coordinator.
     */
    suspend fun dispose() {
        _state.value = CoordinatorState.DISPOSING

        try {
            handlerRegistry.disposeAll()
            handlerRegistry.clear()
            commandRegistry.clear()
            scope.cancel()
            _state.value = CoordinatorState.DISPOSED
        } catch (e: Exception) {
            _state.value = CoordinatorState.ERROR
        }
    }

    /**
     * Get debug information.
     */
    suspend fun getDebugInfo(): String {
        return buildString {
            appendLine("ActionCoordinator Debug Info")
            appendLine("State: ${_state.value}")
            appendLine("Handlers: ${handlerRegistry.getHandlerCount()}")
            appendLine("Categories: ${handlerRegistry.getCategoryCount()}")
            appendLine("Dynamic Commands: ${commandRegistry.size}")
            appendLine()
            append(handlerRegistry.getDebugInfo())
            appendLine()
            appendLine("Dynamic Commands:")
            commandRegistry.all().take(10).forEach { cmd ->
                appendLine("  - ${cmd.phrase} (AVID: ${cmd.targetAvid})")
            }
            if (commandRegistry.size > 10) {
                appendLine("  ... and ${commandRegistry.size - 10} more")
            }
            appendLine()
            append(metrics.getDebugInfo())
        }
    }
}

/**
 * Coordinator state.
 */
enum class CoordinatorState {
    UNINITIALIZED,
    INITIALIZING,
    READY,
    DISPOSING,
    DISPOSED,
    ERROR
}

/**
 * Result of command execution in action coordinator.
 * Different from CommandModels.CommandResult - this tracks handler execution results.
 */
data class ActionCommandResult(
    val command: QuantizedCommand,
    val result: HandlerResult,
    val durationMs: Long,
    val timestamp: Long
)
