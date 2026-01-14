/**
 * VOS4 Accessibility Plugin Interface
 *
 * Allows third-party plugins to integrate with VOS4's accessibility service.
 * Plugins can listen to accessibility events, provide voice commands, and
 * execute actions on UI elements.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-10-26
 */
package com.augmentalis.avacode.plugins.vos4

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.avacode.plugins.core.PluginManifest

/**
 * Plugin interface for accessibility features
 *
 * Implementations can:
 * - React to accessibility events (window changes, focus, etc.)
 * - Provide custom voice commands
 * - Execute actions on UI elements
 * - Register custom UI scraping logic
 */
interface AccessibilityPluginInterface {

    /**
     * Plugin manifest with metadata
     */
    val manifest: PluginManifest

    /**
     * Called when accessibility event is received
     *
     * @param event The accessibility event
     * @param rootNode The root node of the active window (nullable)
     * @return true if event was handled, false to pass to next plugin
     */
    fun onAccessibilityEvent(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?): Boolean

    /**
     * Provide custom voice commands for this plugin
     *
     * @return List of voice commands
     */
    fun provideCommands(): List<VoiceCommandDefinition>

    /**
     * Execute a voice command
     *
     * @param commandId The command ID to execute
     * @param parameters Optional parameters for the command
     * @return CommandResult indicating success/failure
     */
    suspend fun executeCommand(commandId: String, parameters: Map<String, Any>?): CommandResult

    /**
     * Called when plugin is initialized
     */
    fun onPluginInitialized()

    /**
     * Called when plugin is disabled
     */
    fun onPluginDisabled()
}

/**
 * Voice command definition for plugins
 */
data class VoiceCommandDefinition(
    val id: String,
    val primaryText: String,
    val synonyms: List<String> = emptyList(),
    val description: String,
    val category: String = "plugin",
    val priority: Int = 50
)

/**
 * Command execution result
 */
data class CommandResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any>? = null
) {
    companion object {
        fun success(message: String, data: Map<String, Any>? = null) =
            CommandResult(true, message, data)

        fun failure(message: String) =
            CommandResult(false, message, null)
    }
}

/**
 * UI element action types supported by plugins
 */
enum class UIActionType {
    CLICK,
    LONG_CLICK,
    FOCUS,
    SCROLL_FORWARD,
    SCROLL_BACKWARD,
    SET_TEXT,
    CLEAR_TEXT,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    CUSTOM
}

/**
 * UI action request from plugin
 */
data class UIActionRequest(
    val actionType: UIActionType,
    val targetElementHash: String? = null,
    val parameters: Map<String, Any>? = null
)
