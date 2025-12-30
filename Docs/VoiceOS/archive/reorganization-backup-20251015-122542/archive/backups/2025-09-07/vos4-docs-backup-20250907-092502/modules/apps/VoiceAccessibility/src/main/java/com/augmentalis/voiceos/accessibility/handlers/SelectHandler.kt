/**
 * SelectHandler.kt - Selection mode and cursor interaction handler
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Migration Team
 * Created: 2025-09-03
 * 
 * Handles selection mode interactions, cursor-based selection workflows,
 * and context menu operations. Critical for accessibility interactions.
 * 
 * Migrated from Legacy Avenue SelectAction.kt with VOS4 patterns
 */
package com.augmentalis.voiceos.accessibility.handlers

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import kotlinx.coroutines.*

/**
 * Handler for selection mode and cursor-based interactions
 * Provides context-aware selection and menu operations
 */
class SelectHandler(
    private val service: VoiceAccessibilityService
) : ActionHandler {

    companion object {
        private const val TAG = "SelectHandler"
        
        // Supported actions
        private val SUPPORTED_ACTIONS = listOf(
            "select",
            "select_mode",
            "selection_mode",
            "context_menu",
            "menu",
            "back",
            "cancel_selection",
            "select_all",
            "select_text",
            "clear_selection",
            "copy",
            "cut",
            "paste",
            "edit_menu",
            "action_menu"
        )
    }

    private var isSelectionMode = false
    private var currentSelection: SelectionContext? = null
    private val selectionScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
        return SUPPORTED_ACTIONS.any { supportedAction ->
            action.contains(supportedAction, ignoreCase = true) ||
            supportedAction.contains(action.replace(" ", "_"), ignoreCase = true)
        }
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
            action.contains("select_mode", ignoreCase = true) ||
            action.contains("selection_mode", ignoreCase = true) -> {
                enterSelectionMode()
            }

            // Main select action - context-dependent
            action.equals("select", ignoreCase = true) -> {
                handleSelectAction()
            }

            // Context menu actions
            action.contains("menu", ignoreCase = true) ||
            action.contains("context_menu", ignoreCase = true) ||
            action.contains("action_menu", ignoreCase = true) -> {
                showContextMenu()
            }

            // Back/cancel actions
            action.equals("back", ignoreCase = true) ||
            action.contains("cancel_selection", ignoreCase = true) -> {
                handleBackAction()
            }

            // Text selection actions
            action.contains("select_all", ignoreCase = true) -> {
                selectAll()
            }
            action.contains("select_text", ignoreCase = true) -> {
                selectText(params["text"] as? String)
            }
            action.contains("clear_selection", ignoreCase = true) -> {
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

            // TODO: Show selection mode indicator
            // service.showSelectionModeIndicator()

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
                Log.d(TAG, "Showing context menu at cursor position: $cursorPosition")

                // TODO: Integrate with cursor manager to show context menu
                // service.getCursorManager()?.showContextMenuAtPosition(cursorPosition)

                // For now, show basic context menu
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

            // TODO: Hide selection mode indicators
            // service.hideSelectionModeIndicator()

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

    private fun isCursorVisible(): Boolean {
        // TODO: Integrate with cursor manager
        return false
    }

    private fun getCursorPosition(): Rect? {
        // TODO: Integrate with cursor manager
        return null
    }

    private fun showBasicContextMenu(position: Rect): Boolean {
        // TODO: Implement basic context menu display
        Log.d(TAG, "Would show context menu at position: $position")
        return true
    }

    private fun showSelectionContextMenu(): Boolean {
        // TODO: Implement selection-specific context menu
        Log.d(TAG, "Would show selection context menu")
        return true
    }

    private fun showGeneralContextMenu(): Boolean {
        // TODO: Implement general context menu
        Log.d(TAG, "Would show general context menu")
        return true
    }

    private fun findFocusedNode(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        
        if (rootNode.isFocused) return rootNode
        
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            child?.let {
                val found = findFocusedNode(it)
                if (found != null) return found
            }
        }
        
        return null
    }

    private fun findEditableNode(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        
        if (rootNode.isEditable && rootNode.isFocused) return rootNode
        if (rootNode.isEditable) return rootNode
        
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            child?.let {
                val found = findEditableNode(it)
                if (found != null) return found
            }
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