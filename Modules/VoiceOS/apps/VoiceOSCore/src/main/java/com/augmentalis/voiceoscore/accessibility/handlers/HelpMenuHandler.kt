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
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.augmentalis.voiceoscore.accessibility.VoiceOSService
import com.augmentalis.voiceoscore.accessibility.overlays.MenuItem
import com.augmentalis.voiceoscore.accessibility.overlays.OverlayManager
import com.augmentalis.voiceoscore.utils.ConditionalLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handler for help system and command discovery
 * Provides user guidance and command reference
 *
 * Now integrated with OverlayManager for context menu help system.
 */
class HelpMenuHandler(
    private val service: VoiceOSService
) : ActionHandler {

    companion object {
        private const val TAG = "HelpMenuHandler"

        // Documentation URLs (primary + fallback)
        private const val DOCS_URL_ONLINE = "https://augmentalis.gitlab.io/voiceos/developer-manual/"
        private const val DOCS_URL_GITHUB = "https://github.com/augmentalis/voiceos/tree/main/docs/developer-manual"

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

    // Overlay manager for help menus
    private val overlayManager by lazy {
        OverlayManager.getInstance(service)
    }

    override fun initialize() {
        ConditionalLogger.d(TAG) { "Initializing HelpMenuHandler" }
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
        ConditionalLogger.d(TAG) { "Executing help action: $action" }

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
                ConditionalLogger.w(TAG) { "Unknown help action: $action" }
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
                ConditionalLogger.d(TAG) { "Help menu already visible" }
                return true
            }

            ConditionalLogger.i(TAG) { "Showing help menu" }
            isHelpMenuVisible = true

            // Show help menu as context menu with actionable items
            val menuItems = listOf(
                MenuItem(
                    id = "show_commands",
                    label = "Show Commands",
                    icon = Icons.Default.List,
                    number = 1,
                    action = {
                        showCommandList()
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "tutorial",
                    label = "Tutorial",
                    icon = Icons.Default.School,
                    number = 2,
                    action = {
                        showTutorial()
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "navigation_help",
                    label = "Navigation Help",
                    icon = Icons.Default.Navigation,
                    number = 3,
                    action = {
                        showCategoryHelp("navigation")
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "system_help",
                    label = "System Help",
                    icon = Icons.Default.Settings,
                    number = 4,
                    action = {
                        showCategoryHelp("system")
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "documentation",
                    label = "Open Documentation",
                    icon = Icons.Default.OpenInBrowser,
                    number = 5,
                    action = {
                        openDocumentation()
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "hide_help",
                    label = "Hide Help",
                    icon = Icons.Default.Close,
                    number = 6,
                    action = {
                        hideHelpMenu()
                    }
                )
            )

            overlayManager.showContextMenu(menuItems, "VOS4 Help Menu")

            // Auto-hide after delay
            helpScope.launch {
                delay(30000) // 30 seconds (longer for menu browsing)
                if (isHelpMenuVisible) {
                    hideHelpMenu()
                }
            }

            true
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error showing help menu" }
            false
        }
    }

    /**
     * Hide help menu
     */
    private fun hideHelpMenu(): Boolean {
        return try {
            if (!isHelpMenuVisible) {
                ConditionalLogger.d(TAG) { "Help menu already hidden" }
                return true
            }

            ConditionalLogger.i(TAG) { "Hiding help menu" }
            isHelpMenuVisible = false
            currentHelpCategory = null

            // Hide help context menu
            overlayManager.hideContextMenu()
            overlayManager.hideCommandStatus()

            true
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error hiding help menu" }
            false
        }
    }

    /**
     * Show comprehensive command list
     */
    private fun showCommandList(): Boolean {
        return try {
            ConditionalLogger.i(TAG) { "Showing command list" }

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

            // Show command list via command status overlay
            overlayManager.showCommandStatus(
                command = "Voice Commands",
                state = com.augmentalis.voiceoscore.accessibility.overlays.CommandState.SUCCESS,
                message = commandsText
            )

            // Auto-hide after longer delay for reading
            helpScope.launch {
                delay(15000) // 15 seconds
                hideCommandList()
            }

            true
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error showing command list" }
            false
        }
    }

    /**
     * Hide command list
     */
    private fun hideCommandList(): Boolean {
        return try {
            ConditionalLogger.i(TAG) { "Hiding command list" }
            overlayManager.hideCommandStatus()
            true
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error hiding command list" }
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
                ConditionalLogger.w(TAG) { "Unknown help category: $category" }
                return false
            }

            ConditionalLogger.i(TAG) { "Showing help for category: $category" }
            currentHelpCategory = category

            val helpText = buildString {
                appendLine("${category.uppercase()} COMMANDS:\n")
                commands.forEach { command ->
                    appendLine("• $command")
                }
                appendLine("\nSay 'help menu' for more categories")
            }

            // Show category help via command status overlay
            overlayManager.showCommandStatus(
                command = "${category.uppercase()} Commands",
                state = com.augmentalis.voiceoscore.accessibility.overlays.CommandState.SUCCESS,
                message = helpText
            )

            // Auto-hide after delay
            helpScope.launch {
                delay(10000) // 10 seconds
                currentHelpCategory = null
                overlayManager.hideCommandStatus()
            }

            true
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error showing category help" }
            false
        }
    }

    /**
     * Show tutorial/getting started
     */
    private fun showTutorial(): Boolean {
        return try {
            ConditionalLogger.i(TAG) { "Showing tutorial" }

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

            // Show tutorial via command status overlay
            overlayManager.showCommandStatus(
                command = "VOS4 Tutorial",
                state = com.augmentalis.voiceoscore.accessibility.overlays.CommandState.SUCCESS,
                message = tutorialText
            )

            // Auto-hide after delay
            helpScope.launch {
                delay(20000) // 20 seconds for tutorial reading
                overlayManager.hideCommandStatus()
            }

            true
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error showing tutorial" }
            false
        }
    }

    /**
     * Open external documentation with smart fallback
     *
     * Priority:
     * 1. GitLab Pages (best UX - rendered markdown)
     * 2. GitHub repo (fallback - raw markdown)
     * 3. Built-in help menu (offline fallback)
     */
    private fun openDocumentation(): Boolean {
        return try {
            ConditionalLogger.i(TAG) { "Opening VOS4 Developer Manual" }

            // Try primary documentation URL (GitLab Pages)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DOCS_URL_ONLINE)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            service.startActivity(intent)

            // Show feedback via command status overlay
            overlayManager.showCommandStatus(
                command = "Opening Documentation",
                state = com.augmentalis.voiceoscore.accessibility.overlays.CommandState.EXECUTING,
                message = "Opening VOS4 Developer Manual..."
            )

            // Auto-hide after short delay
            helpScope.launch {
                delay(2000)
                overlayManager.hideCommandStatus()
            }

            true
        } catch (e: Exception) {
            ConditionalLogger.w(TAG) { "Primary docs URL failed (${e.message}), trying GitHub fallback" }

            try {
                // Fallback to GitHub repository
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DOCS_URL_GITHUB)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                service.startActivity(intent)

                overlayManager.showCommandStatus(
                    command = "Opening Documentation",
                    state = com.augmentalis.voiceoscore.accessibility.overlays.CommandState.EXECUTING,
                    message = "Opening documentation (GitHub)..."
                )

                helpScope.launch {
                    delay(2000)
                    overlayManager.hideCommandStatus()
                }

                true
            } catch (e2: Exception) {
                ConditionalLogger.e(TAG, e2) { "All documentation URLs failed, showing built-in help" }

                // Final fallback: built-in help menu
                showHelpMenu()
                true
            }
        }
    }

    /**
     * Show help text via command status overlay
     * (Deprecated: Now uses OverlayManager throughout)
     */
    @Deprecated("Use overlayManager.showCommandStatus() instead")
    private fun showHelpToast(message: String) {
        try {
            // Fallback to toast only if overlay manager fails
            Toast.makeText(service, message, Toast.LENGTH_LONG).show()
            ConditionalLogger.i(TAG) { "Help content displayed via toast (fallback): $message" }
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error showing help toast" }
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
        ConditionalLogger.d(TAG) { "Disposing HelpMenuHandler" }
        helpScope.cancel()
        isHelpMenuVisible = false
        currentHelpCategory = null
    }
}