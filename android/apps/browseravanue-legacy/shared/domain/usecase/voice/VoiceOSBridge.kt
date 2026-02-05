package com.augmentalis.browseravanue.domain.usecase.voice

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridge between BrowserAvanue and VoiceOS core
 *
 * Architecture:
 * - Connects BrowserAvanue voice commands to VoiceOS
 * - Bidirectional communication (commands in, results out)
 * - Event-based architecture using Flows
 * - Supports command confirmation and feedback
 * - Thread-safe with SharedFlow
 *
 * Communication Flow:
 * ```
 * VoiceOS Core
 *     ↓ (parsed command)
 * VoiceOSBridge.onCommandReceived()
 *     ↓
 * VoiceCommandProcessor.processCommand()
 *     ↓
 * VoiceOSBridge.sendResult()
 *     ↓ (confirmation/error)
 * VoiceOS Core (speaks result)
 * ```
 *
 * Features:
 * - Command routing to processor
 * - Result feedback to VoiceOS
 * - Command history tracking
 * - Error handling and reporting
 * - Voice confirmation messages
 *
 * Integration Points:
 * - VoiceOS Command Parser → onCommandReceived()
 * - VoiceCommandProcessor → processes commands
 * - VoiceOS TTS → speaks confirmation
 *
 * Usage:
 * ```
 * val bridge = VoiceOSBridge(processor)
 * bridge.commandResults.collect { result ->
 *     voiceOS.speak(result.message)
 * }
 * bridge.onCommandReceived("open google.com", currentTabId)
 * ```
 */
class VoiceOSBridge(
    private val commandProcessor: VoiceCommandProcessor
) {

    // Command results flow (emits results for VoiceOS to speak)
    private val _commandResults = MutableSharedFlow<VoiceCommandResult>(replay = 0)
    val commandResults: SharedFlow<VoiceCommandResult> = _commandResults.asSharedFlow()

    // Command history (last 20 commands)
    private val commandHistory = mutableListOf<VoiceCommandHistoryEntry>()
    private val maxHistorySize = 20

    /**
     * Receive command from VoiceOS
     *
     * Called when VoiceOS parses a voice command
     *
     * @param command Natural language command text
     * @param currentTabId Currently active tab ID (for context)
     */
    suspend fun onCommandReceived(command: String, currentTabId: String? = null) {
        // Add to history
        addToHistory(command)

        // Process command
        val result = commandProcessor.processCommand(command, currentTabId)

        // Emit result for VoiceOS to speak
        _commandResults.emit(result)
    }

    /**
     * Get command history
     *
     * @return List of recent commands
     */
    fun getCommandHistory(): List<VoiceCommandHistoryEntry> {
        return commandHistory.toList()
    }

    /**
     * Clear command history
     */
    fun clearHistory() {
        commandHistory.clear()
    }

    /**
     * Add command to history
     */
    private fun addToHistory(command: String) {
        val entry = VoiceCommandHistoryEntry(
            command = command,
            timestamp = System.currentTimeMillis()
        )

        commandHistory.add(0, entry) // Add to beginning

        // Trim to max size
        if (commandHistory.size > maxHistorySize) {
            commandHistory.removeAt(commandHistory.size - 1)
        }
    }

    /**
     * Repeat last command
     *
     * @param currentTabId Currently active tab ID
     * @return Command result
     */
    suspend fun repeatLastCommand(currentTabId: String? = null): VoiceCommandResult {
        val lastCommand = commandHistory.firstOrNull()?.command

        return if (lastCommand != null) {
            onCommandReceived(lastCommand, currentTabId)
            VoiceCommandResult(
                success = true,
                message = "Repeating: $lastCommand",
                action = VoiceCommandAction.UNKNOWN
            )
        } else {
            VoiceCommandResult(
                success = false,
                message = "No previous command to repeat",
                action = VoiceCommandAction.UNKNOWN
            )
        }
    }

    /**
     * Get available commands list (for "help" command)
     *
     * @return List of command categories with examples
     */
    fun getAvailableCommands(): List<VoiceCommandCategory> {
        return listOf(
            VoiceCommandCategory(
                name = "Navigation",
                commands = listOf(
                    "open [website]",
                    "go to [website]",
                    "search [query]",
                    "go back",
                    "go forward",
                    "refresh",
                    "stop",
                    "home"
                )
            ),
            VoiceCommandCategory(
                name = "Tab Management",
                commands = listOf(
                    "new tab",
                    "close tab",
                    "switch to tab [number]",
                    "next tab",
                    "previous tab",
                    "reopen tab",
                    "duplicate tab"
                )
            ),
            VoiceCommandCategory(
                name = "Favorites",
                commands = listOf(
                    "add to favorites",
                    "show favorites",
                    "open favorite [name]"
                )
            ),
            VoiceCommandCategory(
                name = "Scroll",
                commands = listOf(
                    "scroll up",
                    "scroll down",
                    "scroll to top",
                    "scroll to bottom",
                    "scroll left",
                    "scroll right"
                )
            ),
            VoiceCommandCategory(
                name = "Zoom",
                commands = listOf(
                    "zoom in",
                    "zoom out",
                    "reset zoom"
                )
            ),
            VoiceCommandCategory(
                name = "Privacy",
                commands = listOf(
                    "incognito",
                    "clear history",
                    "clear cache"
                )
            )
        )
    }

    /**
     * Register VoiceOS command listener
     *
     * Called by VoiceOS to register for commands
     * Returns true if registration successful
     */
    fun registerWithVoiceOS(): Boolean {
        // TODO: Integrate with actual VoiceOS core
        // This would register BrowserAvanue as a command handler
        // VoiceOS.registerCommandHandler("browser", this)
        return true
    }

    /**
     * Unregister from VoiceOS
     */
    fun unregisterFromVoiceOS() {
        // TODO: Unregister from VoiceOS core
        // VoiceOS.unregisterCommandHandler("browser")
    }
}

/**
 * Voice command history entry
 */
data class VoiceCommandHistoryEntry(
    val command: String,
    val timestamp: Long
)

/**
 * Voice command category (for help)
 */
data class VoiceCommandCategory(
    val name: String,
    val commands: List<String>
)
