/**
 * HelpScreenHandler.kt - Handler for help screen interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-29
 *
 * Provides help screen data and handles command injection when
 * users tap on commands in the help screen.
 *
 * ## Features:
 * - Provides structured command data for UI rendering
 * - Enables "tap to inject" - user taps a command, it's executed or copied
 * - Supports command search
 * - Tracks recently used commands
 *
 * ## Usage:
 * ```kotlin
 * val handler = HelpScreenHandler(commandInjector)
 *
 * // Get data for UI
 * val categories = handler.getCategories()
 * val quickRef = handler.getQuickReference()
 *
 * // When user taps a command
 * handler.onCommandTapped("scroll down")
 *
 * // Search commands
 * val results = handler.searchCommands("volume")
 * ```
 */
package com.augmentalis.voiceoscore.help

import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Interface for injecting commands from the help screen.
 *
 * When a user taps on a command in the help screen, this interface
 * is used to either execute the command or copy it to input.
 */
interface ICommandInjector {
    /**
     * Execute a voice command as if spoken.
     *
     * @param phrase The command phrase to execute
     * @return true if command was successfully queued/executed
     */
    suspend fun executeCommand(phrase: String): Boolean

    /**
     * Copy command to the text input field.
     * Used for commands that require parameters like "type [text]".
     *
     * @param phrase The command phrase to copy
     * @return true if successfully copied
     */
    suspend fun copyToInput(phrase: String): Boolean

    /**
     * Announce text via TalkBack/accessibility service.
     *
     * @param text Text to announce
     */
    suspend fun announce(text: String)
}

/**
 * State of the help screen.
 */
data class HelpScreenState(
    /**
     * Currently expanded category ID (null if all collapsed).
     */
    val expandedCategoryId: String? = null,

    /**
     * Current search query (empty if not searching).
     */
    val searchQuery: String = "",

    /**
     * Whether showing quick reference table.
     */
    val showingQuickReference: Boolean = false,

    /**
     * Recently tapped commands (for "recent" section).
     */
    val recentCommands: List<String> = emptyList()
)

/**
 * Result of a command tap action.
 */
sealed class CommandTapResult {
    /**
     * Command was executed successfully.
     */
    data class Executed(val phrase: String, val feedback: String) : CommandTapResult()

    /**
     * Command was copied to input (requires parameter).
     */
    data class CopiedToInput(val phrase: String) : CommandTapResult()

    /**
     * Command requires user to provide a parameter.
     */
    data class NeedsParameter(val phrase: String, val parameterHint: String) : CommandTapResult()

    /**
     * Command execution failed.
     */
    data class Failed(val phrase: String, val error: String) : CommandTapResult()
}

/**
 * Handler for help screen interactions.
 *
 * Provides help data and handles command injection from the help UI.
 *
 * @param commandInjector Platform-specific command injector (optional)
 */
class HelpScreenHandler(
    private val commandInjector: ICommandInjector? = null
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.ACCESSIBILITY

    override val supportedActions: List<String> = listOf(
        "help", "show help", "what can I say", "voice commands"
    )

    /**
     * Current state of the help screen.
     */
    private var state = HelpScreenState()

    /**
     * Maximum recent commands to track.
     */
    private val maxRecentCommands = 10

    // ═══════════════════════════════════════════════════════════════════
    // Data Access
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get all command categories for UI rendering.
     */
    fun getCategories(): List<HelpCategory> = HelpCommandDataProvider.getCategories()

    /**
     * Get quick reference entries for table view.
     */
    fun getQuickReference(): List<QuickReferenceEntry> = HelpCommandDataProvider.getQuickReference()

    /**
     * Get complete help screen data.
     */
    fun getHelpScreenData(): HelpScreenData = HelpCommandDataProvider.getHelpScreenData()

    /**
     * Get commands for a specific category.
     */
    fun getCommandsForCategory(categoryId: String): List<HelpCommand> =
        HelpCommandDataProvider.getCommandsByCategory(categoryId)

    /**
     * Search commands by query.
     */
    fun searchCommands(query: String): List<HelpCommand> =
        HelpCommandDataProvider.searchCommands(query)

    /**
     * Get recently used commands.
     */
    fun getRecentCommands(): List<HelpCommand> {
        val allCommands = getCategories().flatMap { it.commands }
        return state.recentCommands.mapNotNull { phrase ->
            allCommands.find { it.primaryPhrase == phrase }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // State Management
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get current help screen state.
     */
    fun getState(): HelpScreenState = state

    /**
     * Expand a category.
     */
    fun expandCategory(categoryId: String) {
        state = state.copy(expandedCategoryId = categoryId)
    }

    /**
     * Collapse all categories.
     */
    fun collapseAll() {
        state = state.copy(expandedCategoryId = null)
    }

    /**
     * Toggle category expansion.
     */
    fun toggleCategory(categoryId: String) {
        state = if (state.expandedCategoryId == categoryId) {
            state.copy(expandedCategoryId = null)
        } else {
            state.copy(expandedCategoryId = categoryId)
        }
    }

    /**
     * Update search query.
     */
    fun setSearchQuery(query: String) {
        state = state.copy(searchQuery = query)
    }

    /**
     * Toggle quick reference view.
     */
    fun toggleQuickReference() {
        state = state.copy(showingQuickReference = !state.showingQuickReference)
    }

    /**
     * Reset state to initial.
     */
    fun resetState() {
        state = HelpScreenState(recentCommands = state.recentCommands)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Command Interaction
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Handle user tapping on a command in the help screen.
     *
     * For executable commands (no parameters), executes immediately.
     * For parameterized commands (like "type [text]"), copies to input.
     *
     * @param phrase The command phrase that was tapped
     * @return Result of the tap action
     */
    suspend fun onCommandTapped(phrase: String): CommandTapResult {
        // Check if command needs a parameter
        if (isParameterizedCommand(phrase)) {
            val hint = getParameterHint(phrase)
            commandInjector?.copyToInput(phrase.substringBefore("["))
            addToRecentCommands(phrase)
            return CommandTapResult.NeedsParameter(phrase, hint)
        }

        // Execute the command
        val executed = commandInjector?.executeCommand(phrase) ?: false

        return if (executed) {
            addToRecentCommands(phrase)
            commandInjector?.announce("Executed: $phrase")
            CommandTapResult.Executed(phrase, "Command executed")
        } else {
            CommandTapResult.Failed(phrase, "Could not execute command")
        }
    }

    /**
     * Handle long press on a command (shows variations).
     *
     * @param phrase The command phrase
     * @return List of alternative phrases
     */
    fun onCommandLongPress(phrase: String): List<String> {
        val command = findCommand(phrase)
        return command?.variations ?: emptyList()
    }

    /**
     * Copy a command phrase to clipboard (for manual use).
     *
     * @param phrase The command phrase to copy
     */
    suspend fun copyCommandToClipboard(phrase: String) {
        commandInjector?.copyToInput(phrase)
        commandInjector?.announce("Copied: $phrase")
    }

    // ═══════════════════════════════════════════════════════════════════
    // Handler Interface
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        // This handler doesn't execute commands itself - it provides the help UI
        // The actual help screen display is handled by the platform UI layer
        return HandlerResult.Success(
            message = "Opening help screen",
            data = mapOf(
                "action" to "show_help",
                "category_count" to getCategories().size,
                "command_count" to HelpCommandDataProvider.getTotalCommandCount()
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // Private Helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Check if command requires a parameter.
     */
    private fun isParameterizedCommand(phrase: String): Boolean {
        return phrase.contains("[") && phrase.contains("]")
    }

    /**
     * Get the parameter hint for a parameterized command.
     */
    private fun getParameterHint(phrase: String): String {
        val match = Regex("\\[([^]]+)]").find(phrase)
        return match?.groupValues?.getOrNull(1) ?: "value"
    }

    /**
     * Find a command by its primary phrase.
     */
    private fun findCommand(phrase: String): HelpCommand? {
        return getCategories()
            .flatMap { it.commands }
            .find { it.primaryPhrase.equals(phrase, ignoreCase = true) }
    }

    /**
     * Add a command to recent history.
     */
    private fun addToRecentCommands(phrase: String) {
        val current = state.recentCommands.toMutableList()
        current.remove(phrase) // Remove if already present
        current.add(0, phrase) // Add to front
        if (current.size > maxRecentCommands) {
            current.removeAt(current.lastIndex)
        }
        state = state.copy(recentCommands = current)
    }

    companion object {
        /**
         * Commands that can be executed immediately (no parameter needed).
         */
        val EXECUTABLE_COMMANDS = setOf(
            "go back", "back", "go home", "home",
            "scroll up", "scroll down", "scroll left", "scroll right",
            "play", "pause", "next track", "previous track",
            "volume up", "volume down", "mute",
            "numbers on", "numbers off", "numbers auto",
            "take screenshot", "flashlight on", "flashlight off",
            "show notifications", "quick settings",
            "copy", "paste", "select all", "undo", "redo",
            // Web gestures
            "pan left", "pan right", "pan up", "pan down",
            "tilt up", "tilt down",
            "orbit left", "orbit right",
            "rotate x", "rotate y", "rotate z",
            "pinch in", "pinch out",
            "fling up", "fling down", "fling left", "fling right",
            "throw", "scale up", "scale down",
            "reset zoom", "select word", "clear selection",
            "hover out", "grab", "release"
        )
    }
}
