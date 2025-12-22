/**
 * InputHandler.kt - Handles text input and keyboard actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.accessibility.IVoiceOSContext
import com.augmentalis.voiceoscore.security.InputValidator

/**
 * Handler for text input and keyboard actions
 */
class InputHandler(
    private val context: IVoiceOSContext
) : ActionHandler {
    
    companion object {
        private const val TAG = "InputHandler"
        
        val SUPPORTED_ACTIONS = listOf(
            "type", "enter text", "input",
            "delete", "backspace", "clear text",
            "select all", "copy", "cut", "paste",
            "undo", "redo",
            "search", "find"
        )
    }
    
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()
        
        Log.d(TAG, "Executing input action: $normalizedAction")
        
        return when {
            // Text input
            normalizedAction.startsWith("type ") ||
            normalizedAction.startsWith("enter text ") ||
            normalizedAction.startsWith("input ") -> {
                val text = normalizedAction
                    .removePrefix("type ")
                    .removePrefix("enter text ")
                    .removePrefix("input ")
                    .trim()
                enterText(text)
            }
            
            // Deletion
            normalizedAction == "delete" || normalizedAction == "backspace" -> {
                performDelete()
            }
            
            normalizedAction == "clear text" || normalizedAction == "clear all" -> {
                clearText()
            }
            
            // Selection
            normalizedAction == "select all" -> {
                selectAll()
            }
            
            // Clipboard operations
            normalizedAction == "copy" -> {
                performCopy()
            }
            
            normalizedAction == "cut" -> {
                performCut()
            }
            
            normalizedAction == "paste" -> {
                performPaste()
            }
            
            // Undo/Redo
            normalizedAction == "undo" -> {
                performUndo()
            }
            
            normalizedAction == "redo" -> {
                performRedo()
            }
            
            // Search
            normalizedAction.startsWith("search ") || 
            normalizedAction.startsWith("find ") -> {
                val query = normalizedAction
                    .removePrefix("search ")
                    .removePrefix("find ")
                    .trim()
                performSearch(query)
            }
            
            else -> {
                Log.w(TAG, "Unknown input action: $normalizedAction")
                false
            }
        }
    }
    
    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { normalized.startsWith(it)}
    }
    
    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }
    
    private fun enterText(text: String): Boolean {
        val focusedNode = findFocusedNode() ?: return false

        // Validate input for security (XSS, SQL injection, length limits)
        try {
            InputValidator.validateTextInput(text)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Input validation failed: ${e.message}")
            return false
        }

        return if (focusedNode.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else {
            // Try to append text if set text doesn't work
            val currentText = focusedNode.text ?: ""
            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    "$currentText$text"
                )
            }
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
    }
    
    private fun performDelete(): Boolean {
        val focusedNode = findFocusedNode() ?: return false
        
        // Try multiple delete strategies
        if (focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION)) {
            return focusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)
        }
        
        // Fallback to setting empty text for last character
        val currentText = focusedNode.text?.toString() ?: return false
        if (currentText.isNotEmpty()) {
            val newText = currentText.substring(0, currentText.length - 1)
            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText
                )
            }
            return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        
        return false
    }
    
    private fun clearText(): Boolean {
        val focusedNode = findFocusedNode() ?: return false
        
        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                ""
            )
        }
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
    
    private fun selectAll(): Boolean {
        val focusedNode = findFocusedNode() ?: return false
        
        val textLength = focusedNode.text?.length ?: return false
        val arguments = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, textLength)
        }
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments)
    }
    
    private fun performCopy(): Boolean {
        val focusedNode = findFocusedNode() ?: return false
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_COPY)
    }
    
    private fun performCut(): Boolean {
        val focusedNode = findFocusedNode() ?: return false
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)
    }
    
    private fun performPaste(): Boolean {
        val focusedNode = findFocusedNode() ?: return false
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }
    
    private fun performUndo(): Boolean {
        findFocusedNode() ?: return false
        // Undo is not directly supported in accessibility API
        // Would need to track text history manually
        Log.w(TAG, "Undo not yet implemented")
        return false
    }
    
    private fun performRedo(): Boolean {
        findFocusedNode() ?: return false
        // Redo is not directly supported in accessibility API
        // Would need to track text history manually
        Log.w(TAG, "Redo not yet implemented")
        return false
    }
    
    private fun performSearch(query: String): Boolean {
        // Attempt to find search field and enter text
        val rootNode = context.getRootNodeInActiveWindow() ?: return false
        val searchNode = findSearchField(rootNode) ?: return false
        
        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                query
            )
        }
        
        searchNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return searchNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
    
    private fun findFocusedNode(): AccessibilityNodeInfo? {
        val rootNode = context.getRootNodeInActiveWindow() ?: return null
        return rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) 
            ?: rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
    }
    
    private fun findSearchField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for nodes with search-related descriptions
        if (node.isEditable) {
            val description = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""
            val hint = node.hintText?.toString()?.lowercase() ?: ""
            
            if (description.contains("search") || 
                text.contains("search") || 
                hint.contains("search")) {
                return node
            }
        }
        
        // Recursively search children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findSearchField(child)
                if (result != null) return result
            }
        }
        
        return null
    }
}