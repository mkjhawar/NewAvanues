/**
 * HelpMenuHandler.kt - Help system and command discovery handler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Migration Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.augmentalis.voiceoscore.accessibility.IVoiceOSContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handler for help system and command discovery
 * Provides user guidance and command reference
 */
class HelpMenuHandler(
    private val service: IVoiceOSContext
) : ActionHandler {

    companion object {
        private const val TAG = "HelpMenuHandler"
        
        // Supported actions
        val SUPPORTED_ACTIONS = listOf(
            "show help",
            "hide help",
            "help menu",
            "show commands",
            "hide commands",
            "command list",
            "what can i say",
            "voice commands",
            "help center",
            "tutorial",
            "user guide",
            "documentation"
        )

        // Help menu categories
        private val HELP_CATEGORIES = mapOf(
            "navigation" to listOf(
                "go back", "go home", "scroll up", "scroll down", 
                "scroll left", "scroll right", "page up", "page down"
            ),
            "system" to listOf(
                "volume up", "volume down", "mute", "brightness up", "brightness down",
                "wifi on", "wifi off", "bluetooth on", "bluetooth off"
            ),
            "apps" to listOf(
                "open [app name]", "launch [app name]", "close app", "switch app",
                "recent apps", "app drawer"
            ),
            "input" to listOf(
                "type [text]", "say [text]", "backspace", "enter", "space",
                "clear text", "select all", "copy", "paste", "cut"
            ),
            "ui" to listOf(
                "tap", "click", "double tap", "long press", "swipe up", "swipe down",
                "swipe left", "swipe right", "pinch open", "pinch close"
            ),
            "accessibility" to listOf(
                "show cursor", "hide cursor", "gaze on", "gaze off", "select mode",
                "show numbers", "hide numbers", "help menu", "what can i say"
            )
        )
    }

    private var isHelpMenuVisible = false
    private var currentHelpCategory: String? = null
    private val helpScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun initialize() {
        Log.d(TAG, "Initializing HelpMenuHandler")
        // Initialize help system components if needed
    }

    override fun canHandle(action: String): Boolean {
        return SUPPORTED_ACTIONS.any { supportedAction -> action.contains(supportedAction, ignoreCase = true) }
    }

    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        Log.d(TAG, "Executing help action: $action")

        return when {
            // Show help menu
            action.contains("show help", ignoreCase = true) ||
            action.contains("help menu", ignoreCase = true) ||
            action.contains("help me", ignoreCase = true) -> {
                showHelpMenu()
            }

            // Hide help menu
            action.contains("hide help", ignoreCase = true) ||
            action.contains("close help", ignoreCase = true) -> {
                hideHelpMenu()
            }

            // Show command list
            action.contains("show commands", ignoreCase = true) ||
            action.contains("command list", ignoreCase = true) ||
            action.contains("what can i say", ignoreCase = true) ||
            action.contains("voice commands", ignoreCase = true) -> {
                showCommandList()
            }

            // Hide command list
            action.contains("hide commands", ignoreCase = true) ||
            action.contains("close commands", ignoreCase = true) -> {
                hideCommandList()
            }

            // Show tutorial
            action.contains("tutorial", ignoreCase = true) ||
            action.contains("getting started", ignoreCase = true) -> {
                showTutorial()
            }

            // Open documentation
            action.contains("user guide", ignoreCase = true) ||
            action.contains("documentation", ignoreCase = true) ||
            action.contains("help center", ignoreCase = true) -> {
                openDocumentation()
            }

            // Category-specific help
            action.contains("navigation help", ignoreCase = true) -> {
                showCategoryHelp("navigation")
            }
            action.contains("system help", ignoreCase = true) -> {
                showCategoryHelp("system")
            }
            action.contains("app help", ignoreCase = true) -> {
                showCategoryHelp("apps")
            }
            action.contains("input help", ignoreCase = true) -> {
                showCategoryHelp("input")
            }
            action.contains("ui help", ignoreCase = true) -> {
                showCategoryHelp("ui")
            }
            action.contains("accessibility help", ignoreCase = true) -> {
                showCategoryHelp("accessibility")
            }

            else -> {
                Log.w(TAG, "Unknown help action: $action")
                false
            }
        }
    }

    /**
     * Show main help menu
     */
    private fun showHelpMenu(): Boolean {
        return try {
            if (isHelpMenuVisible) {
                Log.d(TAG, "Help menu already visible")
                return true
            }

            Log.i(TAG, "Showing help menu")
            isHelpMenuVisible = true

            // Show help menu overlay or dialog
            showHelpToast("VOS4 Help Menu\n\nSay:\n• 'show commands' - List all commands\n• 'tutorial' - Getting started\n• 'navigation help' - Navigation commands\n• 'hide help' - Close this menu")

            // Auto-hide after delay
            helpScope.launch {
                delay(8000) // 8 seconds
                if (isHelpMenuVisible) {
                    hideHelpMenu()
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing help menu", e)
            false
        }
    }

    /**
     * Hide help menu
     */
    private fun hideHelpMenu(): Boolean {
        return try {
            if (!isHelpMenuVisible) {
                Log.d(TAG, "Help menu already hidden")
                return true
            }

            Log.i(TAG, "Hiding help menu")
            isHelpMenuVisible = false
            currentHelpCategory = null

            // Hide help overlays/dialogs
            // Implementation would depend on actual overlay system

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding help menu", e)
            false
        }
    }

    /**
     * Show comprehensive command list
     */
    private fun showCommandList(): Boolean {
        return try {
            Log.i(TAG, "Showing command list")

            val commandsText = buildString {
                appendLine("VOS4 Voice Commands:\n")

                HELP_CATEGORIES.forEach { (category, commands) ->
                    appendLine("${category.uppercase()}:")
                    commands.forEach { command ->
                        appendLine("  • $command")
                    }
                    appendLine()
                }

                appendLine("Say 'hide commands' to close")
            }

            showHelpToast(commandsText)

            // Auto-hide after longer delay for reading
            helpScope.launch {
                delay(15000) // 15 seconds
                hideCommandList()
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing command list", e)
            false
        }
    }

    /**
     * Hide command list
     */
    private fun hideCommandList(): Boolean {
        return try {
            Log.i(TAG, "Hiding command list")
            // Implementation would hide the command list overlay
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding command list", e)
            false
        }
    }

    /**
     * Show category-specific help
     */
    private fun showCategoryHelp(category: String): Boolean {
        return try {
            val commands = HELP_CATEGORIES[category]
            if (commands == null) {
                Log.w(TAG, "Unknown help category: $category")
                return false
            }

            Log.i(TAG, "Showing help for category: $category")
            currentHelpCategory = category

            val helpText = buildString {
                appendLine("${category.uppercase()} COMMANDS:\n")
                commands.forEach { command ->
                    appendLine("• $command")
                }
                appendLine("\nSay 'help menu' for more categories")
            }

            showHelpToast(helpText)

            // Auto-hide after delay
            helpScope.launch {
                delay(10000) // 10 seconds
                currentHelpCategory = null
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing category help", e)
            false
        }
    }

    /**
     * Show tutorial/getting started
     */
    private fun showTutorial(): Boolean {
        return try {
            Log.i(TAG, "Showing tutorial")

            val tutorialText = """
                VOS4 Tutorial:
                
                1. BASICS:
                • Say commands clearly and naturally
                • Wait for beep before speaking
                • Commands work in any app
                
                2. GETTING AROUND:
                • "go back" - Previous screen
                • "go home" - Home screen
                • "scroll down" - Scroll content
                
                3. OPENING APPS:
                • "open [app name]" - Launch app
                • "recent apps" - See recent
                
                4. GETTING HELP:
                • "what can I say" - All commands
                • "help menu" - This tutorial
                
                Say 'hide help' when done
            """.trimIndent()

            showHelpToast(tutorialText)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing tutorial", e)
            false
        }
    }

    /**
     * Open external documentation
     */
    private fun openDocumentation(): Boolean {
        return try {
            Log.i(TAG, "Opening documentation")

            // Open official VoiceOS documentation
            val docUrl = "https://docs.augmentalis.com/voiceos"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(docUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            service.startActivity(intent)
            
            showHelpToast("Opening VOS4 documentation...")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening documentation", e)
            
            // Fallback to showing built-in help
            showHelpMenu()
            true
        }
    }

    /**
     * Show help text using overlay system
     *
     * Integrates with VoiceOS overlay manager for consistent UI experience.
     * Falls back to Toast if overlay system is unavailable.
     *
     * TODO (Phase 4 - UX Polish): Integrate with overlay manager when IVoiceOSContext
     * is extended with getOverlayManager() and getSpeechEngine() methods.
     * Current implementation uses Toast as primary display method.
     */
    private fun showHelpToast(message: String) {
        try {
            // Current implementation: Use Toast for help messages
            // Future: Replace with overlayManager.showHelpOverlay(message)
            Toast.makeText(service.context, message, Toast.LENGTH_LONG).show()
            Log.i(TAG, "Help content displayed: $message")

            // TODO: Add TTS announcement when getSpeechEngine() is available in IVoiceOSContext
            // service.getSpeechEngine()?.speak("Help menu displayed", ...)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing help toast", e)
        }
    }

    /**
     * Get all available commands for external queries
     */
    fun getAllCommands(): Map<String, List<String>> {
        return HELP_CATEGORIES.toMap()
    }

    /**
     * Check if help menu is currently visible
     */
    fun isHelpVisible(): Boolean {
        return isHelpMenuVisible
    }

    /**
     * Get current help category
     */
    fun getCurrentHelpCategory(): String? {
        return currentHelpCategory
    }

    override fun dispose() {
        Log.d(TAG, "Disposing HelpMenuHandler")
        helpScope.cancel()
        isHelpMenuVisible = false
        currentHelpCategory = null
    }
}