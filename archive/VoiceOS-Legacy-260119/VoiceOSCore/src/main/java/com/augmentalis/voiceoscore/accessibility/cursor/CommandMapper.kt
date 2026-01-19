/**
 * CommandMapper.kt - Voice command to cursor action mapping and dispatching
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.util.Log

/**
 * Sealed class hierarchy for cursor actions
 * Direct implementation - no interfaces (VOS4 compliance)
 */
sealed class CursorAction {
    // Movement actions
    data class Move(val dx: Float, val dy: Float) : CursorAction()
    data class MoveTo(val x: Float, val y: Float) : CursorAction()
    data class MoveDirection(val direction: Direction, val distance: Float = 100f) : CursorAction()

    // Click actions
    object Click : CursorAction()
    object DoubleClick : CursorAction()
    object LongPress : CursorAction()
    data class ClickAt(val x: Float, val y: Float) : CursorAction()

    // Speed actions
    data class SetSpeed(val speed: CursorSpeed) : CursorAction()
    object TogglePrecisionMode : CursorAction()

    // Snap actions
    object SnapToNearest : CursorAction()
    data class SnapToElement(val elementId: String) : CursorAction()

    // History actions
    object Undo : CursorAction()
    object Redo : CursorAction()
    object GoBack : CursorAction()

    // Focus actions
    data class ShowFocus(val state: FocusState = FocusState.FOCUSED) : CursorAction()
    object HideFocus : CursorAction()

    // Utility actions
    object Reset : CursorAction()
    object Stop : CursorAction()

    /**
     * Direction enum for directional movements
     */
    enum class Direction {
        UP, DOWN, LEFT, RIGHT,
        UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT,
        CENTER
    }
}

/**
 * Command pattern with priority and aliases
 */
data class CommandPattern(
    val pattern: String,
    val action: CursorAction,
    val priority: Int = 0,
    val aliases: List<String> = emptyList(),
    val description: String = ""
) {
    /**
     * Check if text matches this command pattern
     */
    fun matches(text: String): Boolean {
        val normalized = text.lowercase().trim()
        return normalized == pattern.lowercase() ||
                aliases.any { it.lowercase() == normalized }
    }

    /**
     * Get all patterns (main + aliases)
     */
    fun getAllPatterns(): List<String> {
        return listOf(pattern) + aliases
    }
}

/**
 * Command execution result
 */
data class CommandResult(
    val success: Boolean,
    val action: CursorAction?,
    val message: String = "",
    val executionTime: Long = 0L
)

/**
 * Mapper for voice commands to cursor actions
 *
 * Features:
 * - Command registration with priorities
 * - Multiple aliases per command
 * - Conflict resolution (highest priority wins)
 * - Action dispatching with callbacks
 * - Command discovery and help
 *
 * Usage:
 * ```
 * val mapper = CommandMapper()
 * mapper.registerCommand("move up", CursorAction.MoveDirection(Direction.UP))
 * val result = mapper.mapCommand("move up")
 * mapper.executeAction(result.action) { success ->
 *     // Handle result
 * }
 * ```
 */
class CommandMapper {

    companion object {
        private const val TAG = "CommandMapper"
    }

    // Registered command patterns
    private val commandPatterns = mutableListOf<CommandPattern>()

    // Action execution callbacks
    private val actionCallbacks = mutableMapOf<Class<out CursorAction>, (CursorAction) -> Boolean>()

    init {
        // Register default commands
        registerDefaultCommands()
    }

    /**
     * Register default cursor commands
     */
    private fun registerDefaultCommands() {
        Log.d(TAG, "Registering default cursor commands")

        // Movement commands
        registerCommand(
            pattern = "move up",
            action = CursorAction.MoveDirection(CursorAction.Direction.UP),
            priority = 10,
            aliases = listOf("up", "go up"),
            description = "Move cursor up"
        )

        registerCommand(
            pattern = "move down",
            action = CursorAction.MoveDirection(CursorAction.Direction.DOWN),
            priority = 10,
            aliases = listOf("down", "go down"),
            description = "Move cursor down"
        )

        registerCommand(
            pattern = "move left",
            action = CursorAction.MoveDirection(CursorAction.Direction.LEFT),
            priority = 10,
            aliases = listOf("left", "go left"),
            description = "Move cursor left"
        )

        registerCommand(
            pattern = "move right",
            action = CursorAction.MoveDirection(CursorAction.Direction.RIGHT),
            priority = 10,
            aliases = listOf("right", "go right"),
            description = "Move cursor right"
        )

        // Click commands
        registerCommand(
            pattern = "click",
            action = CursorAction.Click,
            priority = 20,
            aliases = listOf("tap", "select"),
            description = "Click at cursor position"
        )

        registerCommand(
            pattern = "double click",
            action = CursorAction.DoubleClick,
            priority = 20,
            aliases = listOf("double tap"),
            description = "Double click at cursor position"
        )

        registerCommand(
            pattern = "long press",
            action = CursorAction.LongPress,
            priority = 20,
            aliases = listOf("hold", "press and hold"),
            description = "Long press at cursor position"
        )

        // Speed commands
        registerCommand(
            pattern = "slow speed",
            action = CursorAction.SetSpeed(CursorSpeed.SLOW),
            priority = 15,
            aliases = listOf("slower", "speed slow"),
            description = "Set cursor to slow speed"
        )

        registerCommand(
            pattern = "normal speed",
            action = CursorAction.SetSpeed(CursorSpeed.MEDIUM),
            priority = 15,
            aliases = listOf("medium speed", "default speed"),
            description = "Set cursor to normal speed"
        )

        registerCommand(
            pattern = "fast speed",
            action = CursorAction.SetSpeed(CursorSpeed.FAST),
            priority = 15,
            aliases = listOf("faster", "speed fast"),
            description = "Set cursor to fast speed"
        )

        registerCommand(
            pattern = "precision mode",
            action = CursorAction.TogglePrecisionMode,
            priority = 15,
            aliases = listOf("precise mode", "fine control"),
            description = "Toggle precision mode"
        )

        // Snap commands
        registerCommand(
            pattern = "snap",
            action = CursorAction.SnapToNearest,
            priority = 25,
            aliases = listOf("snap to element", "find element"),
            description = "Snap to nearest element"
        )

        // History commands
        registerCommand(
            pattern = "undo",
            action = CursorAction.Undo,
            priority = 30,
            aliases = listOf("go back", "back"),
            description = "Undo last cursor movement"
        )

        registerCommand(
            pattern = "redo",
            action = CursorAction.Redo,
            priority = 30,
            aliases = listOf("go forward", "forward"),
            description = "Redo cursor movement"
        )

        // Focus commands
        registerCommand(
            pattern = "show focus",
            action = CursorAction.ShowFocus(),
            priority = 5,
            aliases = listOf("highlight", "show indicator"),
            description = "Show focus indicator"
        )

        registerCommand(
            pattern = "hide focus",
            action = CursorAction.HideFocus,
            priority = 5,
            aliases = listOf("remove highlight", "hide indicator"),
            description = "Hide focus indicator"
        )

        // Utility commands
        registerCommand(
            pattern = "reset cursor",
            action = CursorAction.Reset,
            priority = 5,
            aliases = listOf("reset", "center cursor"),
            description = "Reset cursor to center"
        )

        registerCommand(
            pattern = "stop",
            action = CursorAction.Stop,
            priority = 100,
            aliases = listOf("halt", "freeze"),
            description = "Stop all cursor movement"
        )

        Log.i(TAG, "Registered ${commandPatterns.size} default cursor commands")
    }

    /**
     * Register a new command pattern
     *
     * @param pattern Main command pattern
     * @param action Action to execute
     * @param priority Command priority (higher = checked first)
     * @param aliases Alternative command phrases
     * @param description Human-readable description
     */
    fun registerCommand(
        pattern: String,
        action: CursorAction,
        priority: Int = 0,
        aliases: List<String> = emptyList(),
        description: String = ""
    ) {
        val command = CommandPattern(pattern, action, priority, aliases, description)

        // Check for conflicts
        val conflicts = commandPatterns.filter { existing ->
            command.getAllPatterns().any { newPattern ->
                existing.getAllPatterns().any { existingPattern ->
                    existingPattern.equals(newPattern, ignoreCase = true)
                }
            }
        }

        if (conflicts.isNotEmpty()) {
            Log.w(TAG, "Command pattern '$pattern' conflicts with ${conflicts.size} existing patterns")
            conflicts.forEach {
                Log.w(TAG, "  - Conflict with: ${it.pattern} (priority: ${it.priority})")
            }
        }

        commandPatterns.add(command)

        // Sort by priority (highest first)
        commandPatterns.sortByDescending { it.priority }

        Log.d(TAG, "Registered command: '$pattern' -> ${action.javaClass.simpleName} (priority: $priority)")
    }

    /**
     * Unregister a command pattern
     *
     * @param pattern Pattern to remove
     * @return true if command was found and removed
     */
    fun unregisterCommand(pattern: String): Boolean {
        val removed = commandPatterns.removeAll { it.pattern.equals(pattern, ignoreCase = true) }
        if (removed) {
            Log.d(TAG, "Unregistered command: '$pattern'")
        }
        return removed
    }

    /**
     * Map voice command text to cursor action
     *
     * @param commandText Voice command text
     * @return Command result with action (or null if not found)
     */
    fun mapCommand(commandText: String): CommandResult {
        val startTime = System.currentTimeMillis()
        val normalized = commandText.lowercase().trim()

        Log.d(TAG, "Mapping command: '$commandText'")

        // Find matching command (sorted by priority)
        val match = commandPatterns.firstOrNull { it.matches(normalized) }

        val executionTime = System.currentTimeMillis() - startTime

        return if (match != null) {
            Log.i(TAG, "✓ Mapped command '$commandText' -> ${match.action.javaClass.simpleName}")
            CommandResult(
                success = true,
                action = match.action,
                message = "Command mapped to ${match.action.javaClass.simpleName}",
                executionTime = executionTime
            )
        } else {
            Log.w(TAG, "✗ No mapping found for command: '$commandText'")
            CommandResult(
                success = false,
                action = null,
                message = "Unknown command: $commandText",
                executionTime = executionTime
            )
        }
    }

    /**
     * Register action execution callback
     *
     * @param actionClass Action class to handle
     * @param callback Callback that executes the action (returns success)
     */
    fun <T : CursorAction> registerActionCallback(
        actionClass: Class<T>,
        callback: (T) -> Boolean
    ) {
        @Suppress("UNCHECKED_CAST")
        actionCallbacks[actionClass] = callback as (CursorAction) -> Boolean
        Log.d(TAG, "Registered callback for ${actionClass.simpleName}")
    }

    /**
     * Execute cursor action using registered callbacks
     *
     * @param action Action to execute
     * @return true if action was executed successfully
     */
    fun executeAction(action: CursorAction?): Boolean {
        if (action == null) {
            Log.w(TAG, "Cannot execute null action")
            return false
        }

        Log.d(TAG, "Executing action: ${action.javaClass.simpleName}")

        val callback = actionCallbacks[action.javaClass]
        if (callback != null) {
            return try {
                val success = callback(action)
                if (success) {
                    Log.i(TAG, "✓ Action executed successfully: ${action.javaClass.simpleName}")
                } else {
                    Log.w(TAG, "✗ Action execution failed: ${action.javaClass.simpleName}")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error executing action: ${action.javaClass.simpleName}", e)
                false
            }
        } else {
            Log.w(TAG, "No callback registered for action: ${action.javaClass.simpleName}")
            return false
        }
    }

    /**
     * Map and execute command in one call
     *
     * @param commandText Voice command text
     * @return Command result with execution status
     */
    fun mapAndExecute(commandText: String): CommandResult {
        val result = mapCommand(commandText)

        if (result.success && result.action != null) {
            val executed = executeAction(result.action)
            return result.copy(
                success = executed,
                message = if (executed) "Command executed successfully" else "Command execution failed"
            )
        }

        return result
    }

    /**
     * Get all registered commands
     */
    fun getAllCommands(): List<CommandPattern> {
        return commandPatterns.toList()
    }

    /**
     * Get commands for specific action type
     */
    fun getCommandsForAction(actionClass: Class<out CursorAction>): List<CommandPattern> {
        return commandPatterns.filter { it.action.javaClass == actionClass }
    }

    /**
     * Search commands by pattern
     *
     * @param query Search query
     * @return Matching commands
     */
    fun searchCommands(query: String): List<CommandPattern> {
        val normalized = query.lowercase()
        return commandPatterns.filter { command ->
            command.getAllPatterns().any { it.lowercase().contains(normalized) } ||
                    command.description.lowercase().contains(normalized)
        }
    }

    /**
     * Get command suggestions for partial input
     *
     * @param partial Partial command text
     * @param maxResults Maximum number of suggestions
     * @return List of suggested commands
     */
    fun getSuggestions(partial: String, maxResults: Int = 5): List<String> {
        val normalized = partial.lowercase().trim()

        if (normalized.isEmpty()) return emptyList()

        val suggestions = commandPatterns
            .flatMap { it.getAllPatterns() }
            .filter { it.lowercase().startsWith(normalized) }
            .distinct()
            .take(maxResults)

        Log.d(TAG, "Generated ${suggestions.size} suggestions for '$partial'")
        return suggestions
    }

    /**
     * Get help text for all commands
     */
    fun getHelpText(): String {
        val sb = StringBuilder("Available Cursor Commands:\n\n")

        commandPatterns
            .sortedByDescending { it.priority }
            .forEach { command ->
                sb.append("• ${command.pattern}")
                if (command.aliases.isNotEmpty()) {
                    sb.append(" (${command.aliases.joinToString(", ")})")
                }
                if (command.description.isNotEmpty()) {
                    sb.append("\n  ${command.description}")
                }
                sb.append("\n")
            }

        return sb.toString()
    }

    /**
     * Clear all registered commands
     */
    fun clear() {
        Log.d(TAG, "Clearing all command patterns (${commandPatterns.size} commands)")
        commandPatterns.clear()
        actionCallbacks.clear()
    }

    /**
     * Reset to default commands
     */
    fun resetToDefaults() {
        clear()
        registerDefaultCommands()
        Log.i(TAG, "Reset to default commands")
    }

    /**
     * Get statistics
     */
    fun getStatistics(): CommandMapperStatistics {
        return CommandMapperStatistics(
            totalCommands = commandPatterns.size,
            totalAliases = commandPatterns.sumOf { it.aliases.size },
            registeredCallbacks = actionCallbacks.size,
            averagePriority = commandPatterns.map { it.priority }.average()
        )
    }
}

/**
 * Statistics about command mapper
 */
data class CommandMapperStatistics(
    val totalCommands: Int,
    val totalAliases: Int,
    val registeredCallbacks: Int,
    val averagePriority: Double
) {
    override fun toString(): String {
        return """
            |Command Mapper Statistics:
            |  Total Commands: $totalCommands
            |  Total Aliases: $totalAliases
            |  Registered Callbacks: $registeredCallbacks
            |  Average Priority: ${String.format("%.1f", averagePriority)}
        """.trimMargin()
    }
}
