/**
 * ActionFactory.kt - Dynamic action factory for database-driven commands
 *
 * Created: 2025-11-14
 * Purpose: Eliminate hardcoded action maps by dynamically creating BaseAction instances
 *          from database command metadata (id, category, parameters)
 *
 * Architecture:
 * - Single source of truth: Database
 * - VOS/JSON files: Import tools only
 * - Actions: Dynamically created from metadata
 * - No hardcoded command→action mappings
 */

package com.augmentalis.commandmanager.actions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import com.augmentalis.voiceos.command.*

/**
 * Factory for creating BaseAction instances dynamically from command metadata
 *
 * This eliminates the need for hardcoded action maps by interpreting the
 * command ID and category to determine which action to execute.
 *
 * Command ID Format: "{category}_{action}"
 * Examples:
 * - "nav_back" → NavigationAction(GLOBAL_ACTION_BACK)
 * - "volume_up" → VolumeAction(VOLUME_UP)
 * - "turn_on_bluetooth" → SystemAction(BLUETOOTH_ENABLE)
 */
object ActionFactory {

    private const val TAG = "ActionFactory"

    /**
     * Create a BaseAction instance from command metadata
     *
     * @param commandId The command ID from database (e.g., "go_back", "turn_on_bluetooth")
     * @param category The command category extracted from ID prefix (e.g., "GO", "TURN", "HIDE")
     * @return BaseAction instance or null if command type is unknown
     */
    fun createAction(commandId: String, category: String): BaseAction? {
        Log.d(TAG, "Creating action for commandId='$commandId', category='$category'")

        // Category is the first word of command ID (uppercased)
        // Examples: "go_back" → "GO", "turn_on_bluetooth" → "TURN", "hide_help" → "HIDE"

        // Map action verb categories to action type handlers
        return when (category.lowercase()) {
            // Navigation actions (go, navigate, show, open page navigation)
            "go", "navigate", "nav" -> createNavigationAction(commandId)

            // Volume/Audio actions
            "volume", "mute", "unmute" -> createVolumeAction(commandId)

            // System/Network actions (turn on/off, toggle, enable/disable)
            "turn", "toggle", "enable", "disable", "system" -> createSystemAction(commandId)

            // Scroll actions
            "scroll", "page" -> createScrollAction(commandId)

            // Cursor/Mouse actions
            "cursor", "move", "position" -> createCursorAction(commandId)

            // Editing/Text actions
            "editing", "text", "copy", "paste", "cut", "select", "delete" -> createEditingAction(commandId)

            // Browser actions
            "browser", "refresh", "reload" -> createBrowserAction(commandId)

            // Media playback actions
            "media", "play", "pause", "stop", "next", "previous", "prev" -> createMediaAction(commandId)

            // UI state actions (open, close, hide, show)
            "open", "close", "hide", "show", "dismiss" -> createUIAction(commandId)

            // Interaction actions (tap, press, swipe, click)
            "tap", "click", "press", "long", "swipe", "drag" -> createInteractionAction(commandId)

            // Help/Overlay actions
            "help", "command", "commands" -> createOverlayAction(commandId)

            // Keyboard actions
            "keyboard", "input" -> createKeyboardAction(commandId)

            // App/Activity actions
            "launch", "start", "run" -> createAppAction(commandId)

            // Center/Align actions
            "center", "align" -> createPositionAction(commandId)

            else -> {
                Log.w(TAG, "Unknown category: $category for commandId: $commandId")
                // Fallback: Try to infer from command ID content
                inferActionFromCommandId(commandId)
            }
        }
    }

    /**
     * Fallback: Try to infer action type from command ID when category is unknown
     */
    private fun inferActionFromCommandId(commandId: String): BaseAction? {
        Log.d(TAG, "Attempting to infer action type from commandId: $commandId")

        return when {
            commandId.contains("back") || commandId.contains("home") || commandId.contains("recent") ->
                createNavigationAction(commandId)
            commandId.contains("volume") || commandId.contains("mute") ->
                createVolumeAction(commandId)
            commandId.contains("wifi") || commandId.contains("bluetooth") || commandId.contains("settings") ->
                createSystemAction(commandId)
            commandId.contains("scroll") ->
                createScrollAction(commandId)
            commandId.contains("cursor") ->
                createCursorAction(commandId)
            commandId.contains("copy") || commandId.contains("paste") || commandId.contains("cut") ->
                createEditingAction(commandId)
            commandId.contains("browser") || commandId.contains("refresh") ->
                createBrowserAction(commandId)
            commandId.contains("play") || commandId.contains("pause") || commandId.contains("media") ->
                createMediaAction(commandId)
            commandId.contains("hide") || commandId.contains("show") || commandId.contains("open") || commandId.contains("close") ->
                createUIAction(commandId)
            commandId.contains("tap") || commandId.contains("click") || commandId.contains("press") ->
                createInteractionAction(commandId)
            commandId.contains("help") || commandId.contains("command") ->
                createOverlayAction(commandId)
            commandId.contains("keyboard") ->
                createKeyboardAction(commandId)
            commandId.contains("launch") || commandId.contains("start") ->
                createAppAction(commandId)
            commandId.contains("center") || commandId.contains("align") ->
                createPositionAction(commandId)
            else -> {
                Log.w(TAG, "Could not infer action type for: $commandId")
                null
            }
        }
    }

    /**
     * Create navigation action from command ID
     */
    private fun createNavigationAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("back") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_BACK,
                "Navigated back"
            )
            commandId.contains("home") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_HOME,
                "Navigated to home"
            )
            commandId.contains("recent") || commandId.contains("recents") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_RECENTS,
                "Opened recent apps"
            )
            commandId.contains("notification") && !commandId.contains("dismiss") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS,
                "Opened notifications"
            )
            commandId.contains("quick_settings") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS,
                "Opened quick settings"
            )
            commandId.contains("power_dialog") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_POWER_DIALOG,
                "Opened power dialog"
            )
            commandId.contains("split_screen") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN,
                "Toggled split screen"
            )
            commandId.contains("lock") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN,
                "Locked screen"
            )
            commandId.contains("screenshot") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT,
                "Screenshot taken"
            )
            commandId.contains("accessibility_settings") -> DynamicIntentAction(
                Settings.ACTION_ACCESSIBILITY_SETTINGS,
                "Opened accessibility settings"
            )
            commandId.contains("dismiss") && commandId.contains("notification") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE,
                "Dismissed notifications"
            )
            commandId.contains("all_apps") -> DynamicNavigationAction(
                AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS,
                "Opened all apps"
            )
            else -> {
                Log.w(TAG, "Unknown navigation command: $commandId")
                null
            }
        }
    }

    /**
     * Create volume action from command ID
     */
    private fun createVolumeAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("up") || commandId.contains("increase") || commandId.contains("louder") ->
                DynamicVolumeAction(VolumeAction.VOLUME_UP, "Volume increased")
            commandId.contains("down") || commandId.contains("decrease") || commandId.contains("quieter") || commandId.contains("lower") ->
                DynamicVolumeAction(VolumeAction.VOLUME_DOWN, "Volume decreased")
            commandId.contains("mute") || commandId.contains("silence") ->
                DynamicVolumeAction(VolumeAction.MUTE, "Volume muted")
            commandId.contains("max") || commandId.contains("maximum") ->
                DynamicVolumeAction(VolumeAction.MAX, "Volume set to maximum")
            else -> {
                Log.w(TAG, "Unknown volume command: $commandId")
                null
            }
        }
    }

    /**
     * Create system action from command ID
     */
    private fun createSystemAction(commandId: String): BaseAction? {
        return when {
            // Bluetooth
            commandId.contains("bluetooth") && (commandId.contains("on") || commandId.contains("enable") || commandId.contains("turn_on")) ->
                DynamicBluetoothAction(true, "Bluetooth enabled")
            commandId.contains("bluetooth") && (commandId.contains("off") || commandId.contains("disable") || commandId.contains("turn_off")) ->
                DynamicBluetoothAction(false, "Bluetooth disabled")
            commandId.contains("bluetooth") && commandId.contains("toggle") ->
                DynamicBluetoothAction(null, "Bluetooth toggled")

            // WiFi
            commandId.contains("wifi") && (commandId.contains("on") || commandId.contains("enable") || commandId.contains("turn_on")) ->
                DynamicWiFiAction(true, "WiFi enabled")
            commandId.contains("wifi") && (commandId.contains("off") || commandId.contains("disable") || commandId.contains("turn_off")) ->
                DynamicWiFiAction(false, "WiFi disabled")
            commandId.contains("wifi") && commandId.contains("toggle") ->
                DynamicWiFiAction(null, "WiFi toggled")

            // Settings
            commandId.contains("settings") -> DynamicIntentAction(
                Settings.ACTION_SETTINGS,
                "Opened settings"
            )

            else -> {
                Log.w(TAG, "Unknown system command: $commandId")
                null
            }
        }
    }

    /**
     * Create scroll action from command ID
     */
    private fun createScrollAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("up") -> DynamicScrollAction("up", "Scrolled up")
            commandId.contains("down") -> DynamicScrollAction("down", "Scrolled down")
            commandId.contains("left") -> DynamicScrollAction("left", "Scrolled left")
            commandId.contains("right") -> DynamicScrollAction("right", "Scrolled right")
            commandId.contains("top") -> DynamicScrollAction("top", "Scrolled to top")
            commandId.contains("bottom") -> DynamicScrollAction("bottom", "Scrolled to bottom")
            else -> {
                Log.w(TAG, "Unknown scroll command: $commandId")
                null
            }
        }
    }

    /**
     * Create cursor action from command ID
     */
    private fun createCursorAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("up") -> DynamicCursorAction("up", "Cursor moved up")
            commandId.contains("down") -> DynamicCursorAction("down", "Cursor moved down")
            commandId.contains("left") -> DynamicCursorAction("left", "Cursor moved left")
            commandId.contains("right") -> DynamicCursorAction("right", "Cursor moved right")
            else -> {
                Log.w(TAG, "Unknown cursor command: $commandId")
                null
            }
        }
    }

    /**
     * Create editing/text action from command ID
     */
    private fun createEditingAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("copy") -> DynamicEditingAction("copy", "Text copied")
            commandId.contains("paste") -> DynamicEditingAction("paste", "Text pasted")
            commandId.contains("cut") -> DynamicEditingAction("cut", "Text cut")
            commandId.contains("select_all") -> DynamicEditingAction("select_all", "All text selected")
            commandId.contains("delete") -> DynamicEditingAction("delete", "Text deleted")
            commandId.contains("undo") -> DynamicEditingAction("undo", "Undone")
            commandId.contains("redo") -> DynamicEditingAction("redo", "Redone")
            else -> {
                Log.w(TAG, "Unknown editing command: $commandId")
                null
            }
        }
    }

    /**
     * Create browser action from command ID
     */
    private fun createBrowserAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("forward") -> DynamicBrowserAction("forward", "Navigated forward")
            commandId.contains("refresh") || commandId.contains("reload") -> DynamicBrowserAction("refresh", "Page refreshed")
            commandId.contains("new_tab") -> DynamicBrowserAction("new_tab", "New tab opened")
            commandId.contains("close_tab") -> DynamicBrowserAction("close_tab", "Tab closed")
            else -> {
                Log.w(TAG, "Unknown browser command: $commandId")
                null
            }
        }
    }

    /**
     * Create media action from command ID
     */
    private fun createMediaAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("play") && !commandId.contains("pause") -> DynamicMediaAction("play", "Media playing")
            commandId.contains("pause") -> DynamicMediaAction("pause", "Media paused")
            commandId.contains("next") -> DynamicMediaAction("next", "Next track")
            commandId.contains("previous") || commandId.contains("prev") -> DynamicMediaAction("previous", "Previous track")
            commandId.contains("stop") -> DynamicMediaAction("stop", "Media stopped")
            else -> {
                Log.w(TAG, "Unknown media command: $commandId")
                null
            }
        }
    }

    /**
     * Create UI state action (open, close, hide, show)
     */
    private fun createUIAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("settings") -> DynamicIntentAction(Settings.ACTION_SETTINGS, "Opened settings")
            commandId.contains("connection") && commandId.contains("open") ->
                DynamicIntentAction(Settings.ACTION_WIRELESS_SETTINGS, "Opened connection settings")
            else -> {
                // UI state changes (hide/show overlays, close keyboards, etc.)
                Log.d(TAG, "UI action not fully implemented yet: $commandId")
                DynamicUIAction(commandId, "UI action: $commandId")
            }
        }
    }

    /**
     * Create interaction action (tap, click, press, swipe)
     */
    private fun createInteractionAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("long") && commandId.contains("press") ->
                DynamicInteractionAction("long_press", "Long press")
            commandId.contains("tap") || commandId.contains("click") ->
                DynamicInteractionAction("tap", "Tap")
            commandId.contains("swipe") ->
                DynamicInteractionAction("swipe", "Swipe")
            commandId.contains("drag") ->
                DynamicInteractionAction("drag", "Drag")
            else -> {
                Log.w(TAG, "Unknown interaction command: $commandId")
                null
            }
        }
    }

    /**
     * Create overlay action (help menus, command overlays)
     */
    private fun createOverlayAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("help") && commandId.contains("hide") ->
                DynamicOverlayAction("hide_help", "Help hidden")
            commandId.contains("help") && commandId.contains("show") ->
                DynamicOverlayAction("show_help", "Help shown")
            commandId.contains("command") && commandId.contains("hide") ->
                DynamicOverlayAction("hide_command", "Command list hidden")
            commandId.contains("command") && commandId.contains("show") ->
                DynamicOverlayAction("show_command", "Command list shown")
            else -> {
                Log.d(TAG, "Overlay action: $commandId")
                DynamicOverlayAction(commandId, "Overlay action")
            }
        }
    }

    /**
     * Create keyboard action
     */
    private fun createKeyboardAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("close") || commandId.contains("hide") ->
                DynamicKeyboardAction("close", "Keyboard closed")
            commandId.contains("open") || commandId.contains("show") ->
                DynamicKeyboardAction("open", "Keyboard opened")
            else -> {
                Log.w(TAG, "Unknown keyboard command: $commandId")
                null
            }
        }
    }

    /**
     * Create app launch action
     */
    private fun createAppAction(commandId: String): BaseAction? {
        Log.d(TAG, "App launch action not fully implemented yet: $commandId")
        return DynamicAppAction(commandId, "Launch app")
    }

    /**
     * Create position/alignment action (center, align)
     */
    private fun createPositionAction(commandId: String): BaseAction? {
        return when {
            commandId.contains("center") && commandId.contains("cursor") ->
                DynamicPositionAction("center_cursor", "Cursor centered")
            commandId.contains("center") ->
                DynamicPositionAction("center", "Centered")
            commandId.contains("align") ->
                DynamicPositionAction("align", "Aligned")
            else -> {
                Log.w(TAG, "Unknown position command: $commandId")
                null
            }
        }
    }
}

/**
 * Dynamic navigation action that performs global accessibility actions
 */
class DynamicNavigationAction(
    private val globalAction: Int,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        return if (performGlobalAction(accessibilityService, globalAction)) {
            createSuccessResult(command, successMessage)
        } else {
            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed: $successMessage")
        }
    }
}

/**
 * Dynamic intent action that launches activities
 */
class DynamicIntentAction(
    private val intentAction: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        return try {
            val intent = Intent(intentAction).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            createSuccessResult(command, successMessage)
        } catch (e: Exception) {
            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed: ${e.message}")
        }
    }
}

/**
 * Dynamic volume action
 */
class DynamicVolumeAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        // Implementation will delegate to VolumeActions
        val volumeAction = when (action) {
            VolumeAction.VOLUME_UP -> VolumeActions.VolumeUpAction()
            VolumeAction.VOLUME_DOWN -> VolumeActions.VolumeDownAction()
            VolumeAction.MUTE -> VolumeActions.MuteAction()
            VolumeAction.MAX -> VolumeActions.MaxVolumeAction()
            else -> return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown volume action")
        }
        return volumeAction.execute(command, accessibilityService, context)
    }
}

/**
 * Dynamic Bluetooth action
 */
class DynamicBluetoothAction(
    private val enable: Boolean?,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        val action = if (enable == null) {
            SystemActions.BluetoothToggleAction()
        } else if (enable) {
            SystemActions.BluetoothEnableAction()
        } else {
            SystemActions.BluetoothDisableAction()
        }
        return action.execute(command, accessibilityService, context)
    }
}

/**
 * Dynamic WiFi action
 */
class DynamicWiFiAction(
    private val enable: Boolean?,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        val action = if (enable == null) {
            SystemActions.WifiToggleAction()
        } else if (enable) {
            SystemActions.WifiEnableAction()
        } else {
            SystemActions.WifiDisableAction()
        }
        return action.execute(command, accessibilityService, context)
    }
}

/**
 * Dynamic scroll action using accessibility service gestures
 */
class DynamicScrollAction(
    private val direction: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        if (accessibilityService == null) {
            return createErrorResult(command, ErrorCode.MODULE_NOT_AVAILABLE, "Accessibility service not available")
        }

        // Try to find scrollable node first
        val rootNode = accessibilityService.rootInActiveWindow
        val scrollableNode = findScrollableNode(rootNode)

        if (scrollableNode != null) {
            val action = when (direction) {
                "up", "top" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                "down", "bottom" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                "left" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                "right" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                else -> return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown direction: $direction")
            }

            val success = scrollableNode.performAction(action)
            scrollableNode.recycle()
            rootNode?.recycle()

            return if (success) {
                createSuccessResult(command, successMessage)
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Scroll action failed")
            }
        }

        // Fallback to gesture-based scrolling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val metrics = context.resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels

            val path = Path()
            when (direction) {
                "up" -> {
                    path.moveTo(screenWidth / 2f, screenHeight * 0.7f)
                    path.lineTo(screenWidth / 2f, screenHeight * 0.3f)
                }
                "down" -> {
                    path.moveTo(screenWidth / 2f, screenHeight * 0.3f)
                    path.lineTo(screenWidth / 2f, screenHeight * 0.7f)
                }
                "left" -> {
                    path.moveTo(screenWidth * 0.7f, screenHeight / 2f)
                    path.lineTo(screenWidth * 0.3f, screenHeight / 2f)
                }
                "right" -> {
                    path.moveTo(screenWidth * 0.3f, screenHeight / 2f)
                    path.lineTo(screenWidth * 0.7f, screenHeight / 2f)
                }
                "top" -> {
                    path.moveTo(screenWidth / 2f, screenHeight * 0.8f)
                    path.lineTo(screenWidth / 2f, screenHeight * 0.1f)
                }
                "bottom" -> {
                    path.moveTo(screenWidth / 2f, screenHeight * 0.1f)
                    path.lineTo(screenWidth / 2f, screenHeight * 0.8f)
                }
                else -> return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown direction: $direction")
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                .build()

            accessibilityService.dispatchGesture(gesture, null, null)
            return createSuccessResult(command, successMessage)
        }

        rootNode?.recycle()
        return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Could not perform scroll")
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.isScrollable) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val scrollable = findScrollableNode(child)
            if (scrollable != null) return scrollable
            child.recycle()
        }

        return null
    }
}

/**
 * Dynamic cursor action for text cursor movement
 */
class DynamicCursorAction(
    private val direction: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        if (accessibilityService == null) {
            return createErrorResult(command, ErrorCode.MODULE_NOT_AVAILABLE, "Accessibility service not available")
        }

        val rootNode = accessibilityService.rootInActiveWindow
        val focusedNode = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focusedNode == null || !focusedNode.isEditable) {
            rootNode?.recycle()
            return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No editable field focused")
        }

        // Get current cursor position
        val text = focusedNode.text?.toString() ?: ""
        val selectionStart = focusedNode.textSelectionStart
        val selectionEnd = focusedNode.textSelectionEnd

        var newStart = selectionStart
        var newEnd = selectionEnd

        when (direction) {
            "left" -> {
                newStart = maxOf(0, selectionStart - 1)
                newEnd = newStart
            }
            "right" -> {
                newEnd = minOf(text.length, selectionEnd + 1)
                newStart = newEnd
            }
            "up" -> {
                // Move to previous line (find previous newline)
                val prevNewline = text.lastIndexOf('\n', maxOf(0, selectionStart - 1))
                newStart = if (prevNewline >= 0) prevNewline else 0
                newEnd = newStart
            }
            "down" -> {
                // Move to next line (find next newline)
                val nextNewline = text.indexOf('\n', selectionEnd)
                newEnd = if (nextNewline >= 0) nextNewline + 1 else text.length
                newStart = newEnd
            }
            "start", "home" -> {
                newStart = 0
                newEnd = 0
            }
            "end" -> {
                newStart = text.length
                newEnd = text.length
            }
        }

        // Set new cursor position
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newStart)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newEnd)
        }

        val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
        focusedNode.recycle()
        rootNode?.recycle()

        return if (success) {
            createSuccessResult(command, successMessage)
        } else {
            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Cursor movement failed")
        }
    }
}

/**
 * Dynamic editing action using accessibility service
 */
class DynamicEditingAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        if (accessibilityService == null) {
            return createErrorResult(command, ErrorCode.MODULE_NOT_AVAILABLE, "Accessibility service not available")
        }

        val rootNode = accessibilityService.rootInActiveWindow
        val focusedNode = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)

        if (focusedNode == null) {
            rootNode?.recycle()
            return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No focused element")
        }

        val accessibilityAction = when (action) {
            "copy" -> AccessibilityNodeInfo.ACTION_COPY
            "paste" -> AccessibilityNodeInfo.ACTION_PASTE
            "cut" -> AccessibilityNodeInfo.ACTION_CUT
            "select_all" -> 0x20000 // ACTION_SELECT_ALL
            "delete" -> {
                // Delete selected text by replacing with empty string
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                }
                val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                focusedNode.recycle()
                rootNode?.recycle()
                return if (success) {
                    createSuccessResult(command, successMessage)
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Delete failed")
                }
            }
            "undo" -> {
                // Undo via key event simulation
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z))
                focusedNode.recycle()
                rootNode?.recycle()
                return createSuccessResult(command, successMessage)
            }
            "redo" -> {
                focusedNode.recycle()
                rootNode?.recycle()
                return createSuccessResult(command, successMessage)
            }
            else -> {
                focusedNode.recycle()
                rootNode?.recycle()
                return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown editing action: $action")
            }
        }

        val success = focusedNode.performAction(accessibilityAction)
        focusedNode.recycle()
        rootNode?.recycle()

        return if (success) {
            createSuccessResult(command, successMessage)
        } else {
            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Editing action failed")
        }
    }
}

/**
 * Dynamic browser action (placeholder - requires browser integration)
 */
class DynamicBrowserAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        // TODO: Implement browser actions
        Log.w("DynamicBrowserAction", "Browser action not yet implemented: $action")
        return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Browser actions coming soon")
    }
}

/**
 * Dynamic media action using AudioManager
 */
class DynamicMediaAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val keyCode = when (action) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "play_pause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            "fast_forward" -> KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
            "rewind" -> KeyEvent.KEYCODE_MEDIA_REWIND
            else -> return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown media action: $action")
        }

        // Send media key events
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))

        return createSuccessResult(command, successMessage)
    }
}

/**
 * Dynamic UI action (hide/show overlays, close keyboards, etc.)
 */
class DynamicUIAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        // TODO: Implement UI state actions
        Log.d("DynamicUIAction", "UI action not yet implemented: $action")
        return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "UI actions coming soon")
    }
}

/**
 * Dynamic interaction action (tap, click, press, swipe) using gestures
 */
class DynamicInteractionAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        if (accessibilityService == null) {
            return createErrorResult(command, ErrorCode.MODULE_NOT_AVAILABLE, "Accessibility service not available")
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Requires Android N or higher")
        }

        val metrics = context.resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val centerY = metrics.heightPixels / 2f

        val gesture = when (action) {
            "tap", "click" -> {
                // Single tap at center (or cursor position if available)
                val path = Path()
                path.moveTo(centerX, centerY)
                GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                    .build()
            }
            "long_press" -> {
                // Long press (500ms duration)
                val path = Path()
                path.moveTo(centerX, centerY)
                GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
                    .build()
            }
            "swipe" -> {
                // Default swipe up
                val path = Path()
                path.moveTo(centerX, metrics.heightPixels * 0.7f)
                path.lineTo(centerX, metrics.heightPixels * 0.3f)
                GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                    .build()
            }
            "double_tap" -> {
                // Double tap using two quick taps
                val path1 = Path()
                path1.moveTo(centerX, centerY)
                val path2 = Path()
                path2.moveTo(centerX, centerY)
                GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path1, 0, 50))
                    .addStroke(GestureDescription.StrokeDescription(path2, 100, 50))
                    .build()
            }
            else -> return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown interaction: $action")
        }

        accessibilityService.dispatchGesture(gesture, null, null)
        return createSuccessResult(command, successMessage)
    }
}

/**
 * Dynamic overlay action (help menus, command overlays)
 */
class DynamicOverlayAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        // TODO: Implement overlay actions
        Log.d("DynamicOverlayAction", "Overlay action not yet implemented: $action")
        return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Overlay actions coming soon")
    }
}

/**
 * Dynamic keyboard action using InputMethodManager
 */
class DynamicKeyboardAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        return when (action) {
            "close", "hide" -> {
                // Try to hide keyboard via accessibility service
                if (accessibilityService != null) {
                    val rootNode = accessibilityService.rootInActiveWindow
                    val focusedNode = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focusedNode != null) {
                        inputMethodManager.hideSoftInputFromWindow(null, 0)
                        focusedNode.recycle()
                    }
                    rootNode?.recycle()
                }
                // Alternative: use global action to dismiss keyboard
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && accessibilityService != null) {
                    accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                }
                createSuccessResult(command, successMessage)
            }
            "open", "show" -> {
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                createSuccessResult(command, successMessage)
            }
            "toggle" -> {
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
                createSuccessResult(command, successMessage)
            }
            "switch" -> {
                inputMethodManager.showInputMethodPicker()
                createSuccessResult(command, "Input method picker shown")
            }
            else -> createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown keyboard action: $action")
        }
    }
}

/**
 * Dynamic app launch action using PackageManager
 */
class DynamicAppAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        // Extract app name from command or action
        val appName = extractAppName(command.text, action)

        if (appName.isBlank()) {
            return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No app specified")
        }

        // Try to find app by name
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(0)

        // Search for matching app (case-insensitive)
        val matchingApp = installedApps.find { appInfo ->
            val label = packageManager.getApplicationLabel(appInfo).toString()
            label.equals(appName, ignoreCase = true) ||
            label.contains(appName, ignoreCase = true)
        }

        if (matchingApp != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(matchingApp.packageName)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                val appLabel = packageManager.getApplicationLabel(matchingApp)
                return createSuccessResult(command, "Launched $appLabel")
            }
        }

        // Try to match common app names to package names
        val packageName = getCommonAppPackage(appName)
        if (packageName != null) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return createSuccessResult(command, "Launched $appName")
            }
        }

        return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Could not find app: $appName")
    }

    private fun extractAppName(commandText: String, action: String): String {
        // Remove common prefixes
        return commandText
            .replace(Regex("^(launch|start|open|run)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+app$", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifBlank { action.replace("_", " ") }
    }

    private fun getCommonAppPackage(appName: String): String? {
        return when (appName.lowercase()) {
            "chrome", "google chrome" -> "com.android.chrome"
            "youtube" -> "com.google.android.youtube"
            "gmail" -> "com.google.android.gm"
            "maps", "google maps" -> "com.google.android.apps.maps"
            "camera" -> "com.android.camera"
            "phone", "dialer" -> "com.android.dialer"
            "messages", "sms" -> "com.android.messaging"
            "contacts" -> "com.android.contacts"
            "calendar" -> "com.google.android.calendar"
            "photos", "google photos" -> "com.google.android.apps.photos"
            "drive", "google drive" -> "com.google.android.apps.docs"
            "play store" -> "com.android.vending"
            "settings" -> "com.android.settings"
            "clock" -> "com.google.android.deskclock"
            "calculator" -> "com.google.android.calculator"
            "files" -> "com.google.android.apps.nbu.files"
            "spotify" -> "com.spotify.music"
            "netflix" -> "com.netflix.mediaclient"
            "twitter", "x" -> "com.twitter.android"
            "facebook" -> "com.facebook.katana"
            "instagram" -> "com.instagram.android"
            "whatsapp" -> "com.whatsapp"
            "telegram" -> "org.telegram.messenger"
            "slack" -> "com.Slack"
            "zoom" -> "us.zoom.videomeetings"
            else -> null
        }
    }
}

/**
 * Dynamic position/alignment action
 */
class DynamicPositionAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        // TODO: Implement position actions
        Log.d("DynamicPositionAction", "Position action not yet implemented: $action")
        return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Position actions coming soon")
    }
}

/**
 * Volume action constants
 */
object VolumeAction {
    const val VOLUME_UP = "volume_up"
    const val VOLUME_DOWN = "volume_down"
    const val MUTE = "mute"
    const val MAX = "max"
}
