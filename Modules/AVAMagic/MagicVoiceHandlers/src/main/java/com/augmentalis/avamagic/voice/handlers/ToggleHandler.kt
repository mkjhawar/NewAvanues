/**
 * ToggleHandler.kt
 *
 * Created: 2026-01-27 00:00 PST
 * Last Modified: 2026-01-27 00:00 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Voice command handler for VoiceOS toggle/switch controls
 * Features: Turn on/off, enable/disable, toggle switches by name or focus
 * Location: CommandManager module
 *
 * Changelog:
 * - v1.0.0 (2026-01-27): Initial implementation for toggle voice commands
 */

package com.augmentalis.avamagic.voice.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandHandler
import com.augmentalis.commandmanager.CommandRegistry
import com.augmentalis.commandmanager.processor.NodeFinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Voice command handler for toggle/switch controls
 *
 * Routes commands to toggle UI elements (switches, checkboxes, toggles)
 * via AccessibilityService.
 *
 * Supported Commands:
 * - "turn on [name]" / "enable [name]" - turn on toggle by name
 * - "turn off [name]" / "disable [name]" - turn off toggle by name
 * - "toggle [name]" / "switch [name]" - flip toggle state by name
 * - "on" / "off" - for focused toggle
 * - "toggle" - flip focused toggle
 *
 * Design:
 * - Command parsing and routing only (no UI logic)
 * - Uses AccessibilityService for toggle manipulation
 * - Thread-safe singleton pattern
 * - Implements CommandHandler for CommandRegistry integration
 */
class ToggleHandler private constructor(
    private val context: Context
) : CommandHandler {

    companion object {
        private const val TAG = "ToggleHandler"
        private const val MODULE_ID = "toggle_handler"

        @Volatile
        private var instance: ToggleHandler? = null

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): ToggleHandler {
            return instance ?: synchronized(this) {
                instance ?: ToggleHandler(context.applicationContext).also {
                    instance = it
                }
            }
        }

        // Command patterns for voice recognition
        private val TURN_ON_PREFIXES = listOf("turn on", "enable")
        private val TURN_OFF_PREFIXES = listOf("turn off", "disable")
        private val TOGGLE_PREFIXES = listOf("toggle", "switch")
        private val STANDALONE_ON = setOf("on")
        private val STANDALONE_OFF = setOf("off")
        private val STANDALONE_TOGGLE = setOf("toggle")

        // Toggle-related class names for accessibility node detection
        private val TOGGLE_CLASS_NAMES = setOf(
            "android.widget.Switch",
            "android.widget.ToggleButton",
            "android.widget.CheckBox",
            "android.widget.CompoundButton",
            "androidx.appcompat.widget.SwitchCompat",
            "com.google.android.material.switchmaterial.SwitchMaterial",
            "androidx.compose.material.Switch",
            "androidx.compose.material3.Switch"
        )
    }

    // CommandHandler interface implementation
    override val moduleId: String = "togglehandler"

    override val supportedCommands: List<String> = listOf(
        // Named toggle commands
        "turn on [name]",
        "turn off [name]",
        "enable [name]",
        "disable [name]",
        "toggle [name]",
        "switch [name]",

        // Focused toggle commands (standalone)
        "on",
        "off",
        "toggle"
    )

    private val commandScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Integration state
    private var isInitialized = false
    private var accessibilityService: AccessibilityService? = null

    init {
        initialize()
        // Register with CommandRegistry automatically
        CommandRegistry.registerHandler(moduleId, this)
    }

    /**
     * Initialize command handler
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }

        return try {
            isInitialized = true
            Log.d(TAG, "ToggleHandler initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }

    /**
     * Set accessibility service reference for toggle operations
     */
    fun setAccessibilityService(service: AccessibilityService?) {
        this.accessibilityService = service
        Log.d(TAG, "AccessibilityService ${if (service != null) "set" else "cleared"}")
    }

    /**
     * CommandHandler interface: Check if this handler can process the command
     * (command is already normalized by CommandRegistry)
     */
    override fun canHandle(command: String): Boolean {
        return when {
            // Named toggle commands
            TURN_ON_PREFIXES.any { command.startsWith(it) } -> true
            TURN_OFF_PREFIXES.any { command.startsWith(it) } -> true
            TOGGLE_PREFIXES.any { command.startsWith(it) } -> true

            // Standalone commands for focused toggle
            command in STANDALONE_ON -> true
            command in STANDALONE_OFF -> true
            command in STANDALONE_TOGGLE -> true

            else -> false
        }
    }

    /**
     * CommandHandler interface: Execute the command
     * (command is already normalized by CommandRegistry)
     */
    override suspend fun handleCommand(command: String): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized for command processing")
            return false
        }

        Log.d(TAG, "Processing toggle command: '$command'")

        return try {
            when {
                // "turn on [name]" or "enable [name]"
                TURN_ON_PREFIXES.any { command.startsWith(it) } -> {
                    val name = extractToggleName(command, TURN_ON_PREFIXES)
                    if (name.isNotEmpty()) {
                        turnOnToggle(name)
                    } else {
                        // No name provided, try focused toggle
                        setFocusedToggleState(true)
                    }
                }

                // "turn off [name]" or "disable [name]"
                TURN_OFF_PREFIXES.any { command.startsWith(it) } -> {
                    val name = extractToggleName(command, TURN_OFF_PREFIXES)
                    if (name.isNotEmpty()) {
                        turnOffToggle(name)
                    } else {
                        // No name provided, try focused toggle
                        setFocusedToggleState(false)
                    }
                }

                // "toggle [name]" or "switch [name]"
                TOGGLE_PREFIXES.any { command.startsWith(it) } -> {
                    val name = extractToggleName(command, TOGGLE_PREFIXES)
                    if (name.isNotEmpty()) {
                        flipToggle(name)
                    } else {
                        // No name provided, try focused toggle
                        flipFocusedToggle()
                    }
                }

                // Standalone "on" - set focused toggle to on
                command in STANDALONE_ON -> setFocusedToggleState(true)

                // Standalone "off" - set focused toggle to off
                command in STANDALONE_OFF -> setFocusedToggleState(false)

                // Standalone "toggle" - flip focused toggle
                command in STANDALONE_TOGGLE -> flipFocusedToggle()

                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: $command", e)
            false
        }
    }

    /**
     * Extract toggle name from command by removing prefix
     */
    private fun extractToggleName(command: String, prefixes: List<String>): String {
        for (prefix in prefixes) {
            if (command.startsWith(prefix)) {
                return command.removePrefix(prefix).trim()
            }
        }
        return ""
    }

    /**
     * Turn on a toggle by name
     * Finds the toggle element and sets it to checked/on state
     */
    private fun turnOnToggle(name: String): Boolean {
        Log.d(TAG, "Turning on toggle: '$name'")

        val service = accessibilityService ?: run {
            Log.e(TAG, "AccessibilityService not available")
            return false
        }

        val rootNode = service.rootInActiveWindow ?: run {
            Log.e(TAG, "No active window")
            return false
        }

        return try {
            val toggleNode = findToggleByName(rootNode, name)

            if (toggleNode != null) {
                val isCurrentlyChecked = toggleNode.isChecked

                if (isCurrentlyChecked) {
                    Log.d(TAG, "Toggle '$name' is already on")
                    true
                } else {
                    val success = performToggleAction(toggleNode)
                    Log.d(TAG, "Toggle '$name' turned on: $success")
                    success
                }
            } else {
                Log.w(TAG, "Toggle '$name' not found")
                false
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Turn off a toggle by name
     * Finds the toggle element and sets it to unchecked/off state
     */
    private fun turnOffToggle(name: String): Boolean {
        Log.d(TAG, "Turning off toggle: '$name'")

        val service = accessibilityService ?: run {
            Log.e(TAG, "AccessibilityService not available")
            return false
        }

        val rootNode = service.rootInActiveWindow ?: run {
            Log.e(TAG, "No active window")
            return false
        }

        return try {
            val toggleNode = findToggleByName(rootNode, name)

            if (toggleNode != null) {
                val isCurrentlyChecked = toggleNode.isChecked

                if (!isCurrentlyChecked) {
                    Log.d(TAG, "Toggle '$name' is already off")
                    true
                } else {
                    val success = performToggleAction(toggleNode)
                    Log.d(TAG, "Toggle '$name' turned off: $success")
                    success
                }
            } else {
                Log.w(TAG, "Toggle '$name' not found")
                false
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Flip a toggle by name (toggle its current state)
     */
    private fun flipToggle(name: String): Boolean {
        Log.d(TAG, "Flipping toggle: '$name'")

        val service = accessibilityService ?: run {
            Log.e(TAG, "AccessibilityService not available")
            return false
        }

        val rootNode = service.rootInActiveWindow ?: run {
            Log.e(TAG, "No active window")
            return false
        }

        return try {
            val toggleNode = findToggleByName(rootNode, name)

            if (toggleNode != null) {
                val success = performToggleAction(toggleNode)
                val newState = if (toggleNode.isChecked) "off" else "on"
                Log.d(TAG, "Toggle '$name' flipped to $newState: $success")
                success
            } else {
                Log.w(TAG, "Toggle '$name' not found")
                false
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Set focused toggle to specified state
     */
    private fun setFocusedToggleState(targetState: Boolean): Boolean {
        Log.d(TAG, "Setting focused toggle to: ${if (targetState) "on" else "off"}")

        val service = accessibilityService ?: run {
            Log.e(TAG, "AccessibilityService not available")
            return false
        }

        val rootNode = service.rootInActiveWindow ?: run {
            Log.e(TAG, "No active window")
            return false
        }

        return try {
            val focusedToggle = findFocusedToggle(rootNode)

            if (focusedToggle != null) {
                val isCurrentlyChecked = focusedToggle.isChecked

                if (isCurrentlyChecked == targetState) {
                    Log.d(TAG, "Focused toggle is already ${if (targetState) "on" else "off"}")
                    true
                } else {
                    val success = performToggleAction(focusedToggle)
                    Log.d(TAG, "Focused toggle set to ${if (targetState) "on" else "off"}: $success")
                    success
                }
            } else {
                Log.w(TAG, "No focused toggle found")
                false
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Flip the focused toggle (toggle its current state)
     */
    private fun flipFocusedToggle(): Boolean {
        Log.d(TAG, "Flipping focused toggle")

        val service = accessibilityService ?: run {
            Log.e(TAG, "AccessibilityService not available")
            return false
        }

        val rootNode = service.rootInActiveWindow ?: run {
            Log.e(TAG, "No active window")
            return false
        }

        return try {
            val focusedToggle = findFocusedToggle(rootNode)

            if (focusedToggle != null) {
                val wasChecked = focusedToggle.isChecked
                val success = performToggleAction(focusedToggle)
                val newState = if (wasChecked) "off" else "on"
                Log.d(TAG, "Focused toggle flipped to $newState: $success")
                success
            } else {
                Log.w(TAG, "No focused toggle found")
                false
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Find a toggle node by its name/text/description
     * Searches the accessibility tree for toggleable elements matching the name
     */
    private fun findToggleByName(rootNode: AccessibilityNodeInfo, name: String): AccessibilityNodeInfo? {
        val lowerName = name.lowercase()

        // Search strategy 1: Find toggle by text/content description
        val matches = NodeFinder.findNodesMatching(rootNode) { node ->
            isToggleNode(node) && matchesName(node, lowerName)
        }

        if (matches.isNotEmpty()) {
            // Recycle non-first matches
            matches.drop(1).forEach { it.recycle() }
            return matches.first()
        }

        // Search strategy 2: Find toggle associated with a label
        val labelMatches = findToggleByAssociatedLabel(rootNode, lowerName)
        if (labelMatches != null) {
            return labelMatches
        }

        // Search strategy 3: Find any checkable node matching name
        val checkableMatches = NodeFinder.findNodesMatching(rootNode) { node ->
            node.isCheckable && matchesName(node, lowerName)
        }

        if (checkableMatches.isNotEmpty()) {
            checkableMatches.drop(1).forEach { it.recycle() }
            return checkableMatches.first()
        }

        return null
    }

    /**
     * Find toggle associated with a label text
     * Handles cases where the toggle itself has no text but is next to a label
     */
    private fun findToggleByAssociatedLabel(rootNode: AccessibilityNodeInfo, name: String): AccessibilityNodeInfo? {
        // Find text nodes matching the name
        val textNodes = NodeFinder.findNodesMatching(rootNode) { node ->
            val nodeText = node.text?.toString()?.lowercase() ?: ""
            val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            nodeText.contains(name) || nodeDesc.contains(name)
        }

        for (textNode in textNodes) {
            // Look for a toggle sibling
            val parent = textNode.parent
            if (parent != null) {
                for (i in 0 until parent.childCount) {
                    val sibling = parent.getChild(i)
                    if (sibling != null && sibling != textNode && isToggleNode(sibling)) {
                        // Found a toggle sibling
                        textNode.recycle()
                        parent.recycle()
                        return sibling
                    }
                    sibling?.recycle()
                }
                parent.recycle()
            }
            textNode.recycle()
        }

        return null
    }

    /**
     * Find the currently focused toggle element
     */
    private fun findFocusedToggle(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Try accessibility focus first
        val accessibilityFocused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (accessibilityFocused != null && isToggleNode(accessibilityFocused)) {
            return accessibilityFocused
        }
        accessibilityFocused?.recycle()

        // Try input focus
        val inputFocused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (inputFocused != null && isToggleNode(inputFocused)) {
            return inputFocused
        }
        inputFocused?.recycle()

        // Search for focused toggle in tree
        val focusedToggle = NodeFinder.findNodesMatching(rootNode) { node ->
            (node.isFocused || node.isAccessibilityFocused) && isToggleNode(node)
        }

        if (focusedToggle.isNotEmpty()) {
            focusedToggle.drop(1).forEach { it.recycle() }
            return focusedToggle.first()
        }

        return null
    }

    /**
     * Check if a node is a toggle-type control (switch, checkbox, toggle button)
     */
    private fun isToggleNode(node: AccessibilityNodeInfo): Boolean {
        // Check if checkable
        if (node.isCheckable) {
            return true
        }

        // Check class name
        val className = node.className?.toString() ?: ""
        if (TOGGLE_CLASS_NAMES.any { className.contains(it, ignoreCase = true) }) {
            return true
        }

        // Check for common toggle-related content descriptions
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (contentDesc.contains("switch") ||
            contentDesc.contains("toggle") ||
            contentDesc.contains("checkbox")) {
            return true
        }

        return false
    }

    /**
     * Check if node text/description matches the search name
     */
    private fun matchesName(node: AccessibilityNodeInfo, name: String): Boolean {
        val nodeText = node.text?.toString()?.lowercase() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val nodeHint = node.hintText?.toString()?.lowercase() ?: ""

        return nodeText.contains(name) ||
               nodeDesc.contains(name) ||
               nodeHint.contains(name) ||
               name.contains(nodeText.ifEmpty { "###" }) ||
               name.contains(nodeDesc.ifEmpty { "###" })
    }

    /**
     * Perform the toggle action on a node
     * Tries multiple methods: click, ACTION_CLICK, setChecked
     */
    private fun performToggleAction(node: AccessibilityNodeInfo): Boolean {
        // Method 1: Try ACTION_CLICK (most common)
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            Log.d(TAG, "Toggle via ACTION_CLICK")
            return true
        }

        // Method 2: Try to find and click parent if node is not clickable
        if (!node.isClickable) {
            var parent = node.parent
            var attempts = 0
            while (parent != null && attempts < 5) {
                if (parent.isClickable) {
                    val success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    parent.recycle()
                    if (success) {
                        Log.d(TAG, "Toggle via parent ACTION_CLICK")
                        return true
                    }
                }
                val grandparent = parent.parent
                parent.recycle()
                parent = grandparent
                attempts++
            }
            parent?.recycle()
        }

        // Method 3: Try ACTION_SELECT
        if (node.performAction(AccessibilityNodeInfo.ACTION_SELECT)) {
            Log.d(TAG, "Toggle via ACTION_SELECT")
            return true
        }

        // Method 4: Try setting checked state directly (API 21+)
        val bundle = Bundle()
        bundle.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE.toString(), !node.isChecked)
        if (node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)) {
            Log.d(TAG, "Toggle via ACTION_SET_SELECTION")
            return true
        }

        Log.w(TAG, "All toggle methods failed")
        return false
    }

    /**
     * Get handler status
     */
    fun getStatus(): ToggleHandlerStatus {
        return ToggleHandlerStatus(
            isInitialized = isInitialized,
            hasAccessibilityService = accessibilityService != null,
            commandsSupported = supportedCommands.size
        )
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        // Unregister from CommandRegistry
        CommandRegistry.unregisterHandler(moduleId)
        commandScope.cancel()
        accessibilityService = null
        instance = null
        Log.d(TAG, "ToggleHandler disposed")
    }
}

/**
 * Toggle handler status
 */
data class ToggleHandlerStatus(
    val isInitialized: Boolean,
    val hasAccessibilityService: Boolean,
    val commandsSupported: Int
)

/**
 * Toggle operation result
 */
sealed class ToggleResult {
    data class Success(val toggleName: String, val newState: Boolean) : ToggleResult()
    data class AlreadyInState(val toggleName: String, val state: Boolean) : ToggleResult()
    data class NotFound(val toggleName: String) : ToggleResult()
    data class Error(val message: String) : ToggleResult()
    object NoAccessibility : ToggleResult()
    object NoFocusedToggle : ToggleResult()
}
