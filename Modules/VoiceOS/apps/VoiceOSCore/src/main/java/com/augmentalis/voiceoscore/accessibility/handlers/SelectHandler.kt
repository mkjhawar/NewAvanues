/**
 * SelectHandler.kt - Selection mode and cursor interaction handler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Migration Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.accessibilityservice.AccessibilityService
import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
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

/**
 * Handler for selection mode and cursor-based interactions
 * Provides context-aware selection and menu operations
 *
 * Now integrated with OverlayManager for context menu visualization.
 */
class SelectHandler(
    private val service: VoiceOSService
) : ActionHandler {

    companion object {
        private const val TAG = "SelectHandler"
        
        // Supported actions
        val SUPPORTED_ACTIONS = listOf(
            "select",
            "select mode",
            "selection mode",
            "context menu",
            "menu",
            "back",
            "cancel selection",
            "select all",
            "select text",
            "clear selection",
            "copy",
            "cut",
            "paste",
            "edit menu",
            "action menu"
        )
    }

    private var isSelectionMode = false
    private var currentSelection: SelectionContext? = null
    private val selectionScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Overlay manager for context menus
    private val overlayManager by lazy {
        OverlayManager.getInstance(service)
    }

    /**
     * Context information for current selection
     */
    data class SelectionContext(
        val node: AccessibilityNodeInfo?,
        val bounds: Rect?,
        val selectionStart: Int = -1,
        val selectionEnd: Int = -1,
        val isTextSelection: Boolean = false
    )

    override fun initialize() {
        Log.d(TAG, "Initializing SelectHandler")
        // Initialize selection system components
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
        Log.d(TAG, "Executing selection action: $action")

        return when {
            // Enter selection mode
            action.contains("select mode", ignoreCase = true) ||
            action.contains("selection mode", ignoreCase = true) -> {
                enterSelectionMode()
            }

            // Main select action - context-dependent
            action.equals("select", ignoreCase = true) -> {
                handleSelectAction()
            }

            // Context menu actions
            action.contains("menu", ignoreCase = true) ||
            action.contains("context menu", ignoreCase = true) ||
            action.contains("action menu", ignoreCase = true) -> {
                showContextMenu()
            }

            // Back/cancel actions
            action.equals("back", ignoreCase = true) ||
            action.contains("cancel selection", ignoreCase = true) -> {
                handleBackAction()
            }

            // Text selection actions
            action.contains("select all", ignoreCase = true) -> {
                selectAll()
            }
            action.contains("select text", ignoreCase = true) -> {
                selectText(params["text"] as? String)
            }
            action.contains("clear selection", ignoreCase = true) -> {
                clearSelection()
            }

            // Clipboard actions
            action.equals("copy", ignoreCase = true) -> {
                performClipboardAction(AccessibilityNodeInfo.ACTION_COPY)
            }
            action.equals("cut", ignoreCase = true) -> {
                performClipboardAction(AccessibilityNodeInfo.ACTION_CUT)
            }
            action.equals("paste", ignoreCase = true) -> {
                performClipboardAction(AccessibilityNodeInfo.ACTION_PASTE)
            }

            else -> {
                Log.w(TAG, "Unknown selection action: $action")
                false
            }
        }
    }

    /**
     * Enter selection mode
     */
    private fun enterSelectionMode(): Boolean {
        return try {
            Log.i(TAG, "Entering selection mode")
            isSelectionMode = true

            // Find current focus or cursor position
            updateSelectionContext()

            // Show selection mode indicator via command status overlay
            overlayManager.showCommandStatus(
                command = "Selection Mode",
                state = com.augmentalis.voiceoscore.accessibility.overlays.CommandState.LISTENING,
                message = "Say commands or 'context menu' for options"
            )
            ConditionalLogger.i(TAG) { "Selection mode indicator displayed" }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error entering selection mode", e)
            false
        }
    }

    /**
     * Handle context-dependent select action
     */
    private fun handleSelectAction(): Boolean {
        return try {
            // Check if cursor is visible (legacy compatibility)
            if (isCursorVisible()) {
                // Show context menu at cursor position
                showContextMenuAtCursor()
            } else {
                // Standard select behavior
                performSelectAtCurrentPosition()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling select action", e)
            false
        }
    }

    /**
     * Show context menu at cursor position
     */
    private fun showContextMenuAtCursor(): Boolean {
        return try {
            val cursorPosition = getCursorPosition()
            if (cursorPosition != null) {
                ConditionalLogger.d(TAG) { "Showing context menu at cursor position: $cursorPosition" }

                // Show basic context menu at cursor position
                // (Future: integrate with dedicated cursor manager if available)
                showBasicContextMenu(cursorPosition)
                true
            } else {
                Log.w(TAG, "No cursor position available")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing context menu at cursor", e)
            false
        }
    }

    /**
     * Perform select at current position
     */
    private fun performSelectAtCurrentPosition(): Boolean {
        return try {
            val rootNode = service.rootInActiveWindow
            val focusedNode = findFocusedNode(rootNode)

            if (focusedNode != null) {
                Log.d(TAG, "Performing select on focused node")
                
                // Update selection context
                currentSelection = SelectionContext(
                    node = focusedNode,
                    bounds = Rect().apply { focusedNode.getBoundsInScreen(this) },
                    isTextSelection = focusedNode.isEditable
                )

                // Perform appropriate selection action
                if (focusedNode.isEditable) {
                    // Text selection
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION)
                } else {
                    // Regular selection/click
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }

                true
            } else {
                Log.w(TAG, "No focused node found for selection")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing select action", e)
            false
        }
    }

    /**
     * Show context menu
     */
    private fun showContextMenu(): Boolean {
        return try {
            if (isSelectionMode && currentSelection != null) {
                Log.d(TAG, "Showing context menu for current selection")
                showSelectionContextMenu()
            } else {
                Log.d(TAG, "Showing general context menu")
                showGeneralContextMenu()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing context menu", e)
            false
        }
    }

    /**
     * Handle back action in selection context
     */
    private fun handleBackAction(): Boolean {
        return try {
            if (isSelectionMode) {
                Log.d(TAG, "Exiting selection mode")
                exitSelectionMode()
                true
            } else {
                Log.d(TAG, "Performing standard back action")
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling back action", e)
            false
        }
    }

    /**
     * Select all text in current field
     */
    private fun selectAll(): Boolean {
        return try {
            val rootNode = service.rootInActiveWindow
            val editableNode = findEditableNode(rootNode)

            if (editableNode != null) {
                Log.d(TAG, "Selecting all text")
                
                // Get the current text content
                val text = editableNode.text?.toString() ?: ""
                
                // Use ACTION_SET_SELECTION to select all text (from 0 to text length)
                val bundle = android.os.Bundle().apply {
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length)
                }
                
                val result = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)
                
                if (result) {
                    updateSelectionContext()
                }
                
                result
            } else {
                Log.w(TAG, "No editable field found for select all")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting all text", e)
            false
        }
    }

    /**
     * Select specific text
     */
    private fun selectText(text: String?): Boolean {
        if (text == null) {
            Log.w(TAG, "No text provided for selection")
            return false
        }

        return try {
            val rootNode = service.rootInActiveWindow
            val editableNode = findEditableNode(rootNode)

            if (editableNode != null) {
                Log.d(TAG, "Selecting text: $text")
                
                // Find text in current field and select it
                val nodeText = editableNode.text?.toString() ?: ""
                val startIndex = nodeText.indexOf(text, ignoreCase = true)
                
                if (startIndex >= 0) {
                    val endIndex = startIndex + text.length
                    val bundle = android.os.Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, startIndex)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, endIndex)
                    }
                    
                    val result = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)
                    
                    if (result) {
                        currentSelection = SelectionContext(
                            node = editableNode,
                            bounds = Rect().apply { editableNode.getBoundsInScreen(this) },
                            selectionStart = startIndex,
                            selectionEnd = endIndex,
                            isTextSelection = true
                        )
                    }
                    
                    result
                } else {
                    Log.w(TAG, "Text '$text' not found in current field")
                    false
                }
            } else {
                Log.w(TAG, "No editable field found for text selection")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting text", e)
            false
        }
    }

    /**
     * Clear current selection
     */
    private fun clearSelection(): Boolean {
        return try {
            Log.d(TAG, "Clearing selection")
            
            currentSelection?.node?.let { node ->
                if (node.isEditable) {
                    // Clear text selection by setting cursor to end
                    val text = node.text?.toString() ?: ""
                    val bundle = android.os.Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, text.length)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length)
                    }
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)
                }
            }
            
            currentSelection = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing selection", e)
            false
        }
    }

    /**
     * Perform clipboard action (copy, cut, paste)
     */
    private fun performClipboardAction(action: Int): Boolean {
        return try {
            val actionName = when (action) {
                AccessibilityNodeInfo.ACTION_COPY -> "copy"
                AccessibilityNodeInfo.ACTION_CUT -> "cut"
                AccessibilityNodeInfo.ACTION_PASTE -> "paste"
                else -> "clipboard"
            }
            
            Log.d(TAG, "Performing $actionName action")

            val targetNode = currentSelection?.node ?: findEditableNode(service.rootInActiveWindow)
            
            if (targetNode != null) {
                val result = targetNode.performAction(action)
                
                if (result && action == AccessibilityNodeInfo.ACTION_CUT) {
                    clearSelection()
                }
                
                result
            } else {
                Log.w(TAG, "No target node found for $actionName action")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing clipboard action", e)
            false
        }
    }

    /**
     * Exit selection mode
     */
    private fun exitSelectionMode(): Boolean {
        return try {
            Log.i(TAG, "Exiting selection mode")
            isSelectionMode = false
            currentSelection = null

            // Hide selection mode indicator
            overlayManager.hideCommandStatus()
            overlayManager.hideContextMenu()  // Also hide any open context menus
            ConditionalLogger.i(TAG) { "Selection mode indicators hidden" }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exiting selection mode", e)
            false
        }
    }

    /**
     * Update selection context with current state
     */
    private fun updateSelectionContext() {
        try {
            val rootNode = service.rootInActiveWindow
            val focusedNode = findFocusedNode(rootNode)
            
            if (focusedNode != null) {
                currentSelection = SelectionContext(
                    node = focusedNode,
                    bounds = Rect().apply { focusedNode.getBoundsInScreen(this) },
                    selectionStart = focusedNode.textSelectionStart,
                    selectionEnd = focusedNode.textSelectionEnd,
                    isTextSelection = focusedNode.isEditable
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating selection context", e)
        }
    }

    // Helper methods

    /**
     * Check if cursor is visible (graceful fallback if cursor manager not available)
     */
    private fun isCursorVisible(): Boolean {
        // Future integration point for cursor manager
        // For now, check if we have a focused node as fallback
        val rootNode = service.rootInActiveWindow
        val focusedNode = findFocusedNode(rootNode)
        return focusedNode != null
    }

    /**
     * Get cursor position (graceful fallback to focused node position)
     */
    private fun getCursorPosition(): Rect? {
        // Future integration point for cursor manager
        // For now, use focused node bounds as fallback
        val rootNode = service.rootInActiveWindow
        val focusedNode = findFocusedNode(rootNode)

        return focusedNode?.let {
            Rect().apply { it.getBoundsInScreen(this) }
        }
    }

    /**
     * Show basic context menu at position using OverlayManager
     */
    private fun showBasicContextMenu(position: Rect): Boolean {
        ConditionalLogger.d(TAG) { "Showing basic context menu at position: $position" }

        try {
            val menuItems = listOf(
                MenuItem(
                    id = "go_back",
                    label = "Go Back",
                    icon = Icons.Default.ArrowBack,
                    number = 1,
                    action = {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "go_home",
                    label = "Go Home",
                    icon = Icons.Default.Home,
                    number = 2,
                    action = {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "recent_apps",
                    label = "Recent Apps",
                    icon = Icons.Default.List,
                    number = 3,
                    action = {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "notifications",
                    label = "Notifications",
                    icon = Icons.Default.Notifications,
                    number = 4,
                    action = {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                        overlayManager.hideContextMenu()
                    }
                )
            )

            // Convert Rect to Point (center of rect)
            val centerPoint = Point(
                position.centerX(),
                position.centerY()
            )

            overlayManager.showContextMenuAt(menuItems, centerPoint, "Quick Actions")
            return true

        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error showing basic context menu" }
            return false
        }
    }

    /**
     * Show selection-specific context menu using OverlayManager
     */
    private fun showSelectionContextMenu(): Boolean {
        ConditionalLogger.d(TAG) { "Showing selection context menu" }

        try {
            val menuItems = mutableListOf<MenuItem>()

            // Clipboard actions (always available in selection mode)
            menuItems.add(
                MenuItem(
                    id = "copy",
                    label = "Copy",
                    icon = Icons.Default.ContentCopy,
                    number = 1,
                    action = {
                        performClipboardAction(AccessibilityNodeInfo.ACTION_COPY)
                        overlayManager.hideContextMenu()
                    }
                )
            )

            menuItems.add(
                MenuItem(
                    id = "cut",
                    label = "Cut",
                    icon = Icons.Default.ContentCut,
                    number = 2,
                    action = {
                        performClipboardAction(AccessibilityNodeInfo.ACTION_CUT)
                        overlayManager.hideContextMenu()
                    }
                )
            )

            menuItems.add(
                MenuItem(
                    id = "paste",
                    label = "Paste",
                    icon = Icons.Default.ContentPaste,
                    number = 3,
                    action = {
                        performClipboardAction(AccessibilityNodeInfo.ACTION_PASTE)
                        overlayManager.hideContextMenu()
                    }
                )
            )

            // Text selection actions (if text is selected)
            if (currentSelection?.isTextSelection == true) {
                menuItems.add(
                    MenuItem(
                        id = "select_all",
                        label = "Select All",
                        icon = Icons.Default.SelectAll,
                        number = 4,
                        action = {
                            selectAll()
                            overlayManager.hideContextMenu()
                        }
                    )
                )

                menuItems.add(
                    MenuItem(
                        id = "clear_selection",
                        label = "Clear Selection",
                        icon = Icons.Default.Clear,
                        number = 5,
                        action = {
                            clearSelection()
                            overlayManager.hideContextMenu()
                        }
                    )
                )
            }

            // Exit selection mode
            menuItems.add(
                MenuItem(
                    id = "exit_selection",
                    label = "Exit Selection Mode",
                    icon = Icons.Default.Close,
                    number = menuItems.size + 1,
                    action = {
                        exitSelectionMode()
                    }
                )
            )

            overlayManager.showContextMenu(menuItems, "Selection Menu")
            return true

        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error showing selection context menu" }
            return false
        }
    }

    /**
     * Show general context menu using OverlayManager
     */
    private fun showGeneralContextMenu(): Boolean {
        ConditionalLogger.d(TAG) { "Showing general context menu" }

        try {
            val menuItems = listOf(
                MenuItem(
                    id = "enter_selection",
                    label = "Enter Selection Mode",
                    icon = Icons.Default.SelectAll,
                    number = 1,
                    action = {
                        enterSelectionMode()
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "go_back",
                    label = "Go Back",
                    icon = Icons.Default.ArrowBack,
                    number = 2,
                    action = {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "go_home",
                    label = "Go Home",
                    icon = Icons.Default.Home,
                    number = 3,
                    action = {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "recent_apps",
                    label = "Recent Apps",
                    icon = Icons.Default.List,
                    number = 4,
                    action = {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                        overlayManager.hideContextMenu()
                    }
                ),
                MenuItem(
                    id = "show_numbers",
                    label = "Show Numbers",
                    icon = Icons.Default.Numbers,
                    number = 5,
                    action = {
                        // Trigger number overlay via NumberHandler (would need service reference)
                        overlayManager.hideContextMenu()
                        ConditionalLogger.i(TAG) { "Show numbers requested from context menu" }
                    }
                )
            )

            overlayManager.showContextMenu(menuItems, "Voice Menu")
            return true

        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error showing general context menu" }
            return false
        }
    }

    /**
     * Find focused node in the accessibility tree
     * MEMORY FIX: Properly recycles child nodes that are not the result
     *
     * Note: Caller is responsible for recycling the returned node
     */
    private fun findFocusedNode(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        if (rootNode.isFocused) return rootNode

        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val found = findFocusedNode(child)
            if (found != null) {
                // If result is the child itself, don't recycle (caller will handle)
                // If result is deeper, recycle the intermediate child node
                if (found !== child) {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
                return found
            }
            // No result in this branch, recycle child
            @Suppress("DEPRECATION")
            child.recycle()
        }

        return null
    }

    /**
     * Find editable node in the accessibility tree
     * MEMORY FIX: Properly recycles child nodes that are not the result
     *
     * Note: Caller is responsible for recycling the returned node
     */
    private fun findEditableNode(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        if (rootNode.isEditable && rootNode.isFocused) return rootNode
        if (rootNode.isEditable) return rootNode

        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val found = findEditableNode(child)
            if (found != null) {
                // If result is the child itself, don't recycle (caller will handle)
                // If result is deeper, recycle the intermediate child node
                if (found !== child) {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
                return found
            }
            // No result in this branch, recycle child
            @Suppress("DEPRECATION")
            child.recycle()
        }

        return null
    }

    /**
     * Check if selection mode is active
     */
    fun isInSelectionMode(): Boolean = isSelectionMode

    /**
     * Get current selection context
     */
    fun getCurrentSelection(): SelectionContext? = currentSelection

    override fun dispose() {
        Log.d(TAG, "Disposing SelectHandler")
        selectionScope.cancel()
        isSelectionMode = false
        currentSelection = null
    }
}