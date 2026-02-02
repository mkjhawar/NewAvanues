/**
 * TextActions.kt - Text manipulation command actions
 * Path: modules/commands/src/main/java/com/augmentalis/voiceos/commands/actions/TextActions.kt
 * 
 * Created: 2025-08-19
 * Author: Claude Code
 * Module: Commands
 * 
 * Purpose: Text manipulation and editing-related voice command actions
 */

package com.augmentalis.voiceoscore.managers.commandmanager.actions

import com.augmentalis.voiceoscore.*
import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.AccessibilityActions.ACTION_SELECT_ALL

/**
 * Text manipulation command actions
 * Handles text editing, clipboard operations, and text selection
 */
object TextActions {
    
    // Helper methods for text actions
    private fun createSuccessResult(command: Command, message: String, data: Any? = null): CommandResult {
        return CommandResult(
            success = true,
            command = command,
            response = message,
            data = data
        )
    }
    
    private fun createErrorResult(command: Command, errorCode: ErrorCode, message: String): CommandResult {
        return CommandResult(
            success = false,
            command = command,
            error = CommandError(errorCode, message)
        )
    }
    
    /**
     * Copy Text Action
     */
    class CopyTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val targetText = (command.parameters["text"] as? String)
            
            return try {
                val rootNode = accessibilityService?.rootInActiveWindow
                
                if (targetText != null) {
                    // Copy specific text to clipboard
                    copyToClipboard(context, targetText)
                    createSuccessResult(command, "Copied text: '$targetText'")
                } else {
                    // Copy selected or focused text
                    val success = copySelectedText(rootNode)
                    if (success) {
                        createSuccessResult(command, "Copied selected text")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No text selected or found to copy")
                    }
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to copy text: ${e.message}")
            }
        }
    }
    
    /**
     * Cut Text Action
     */
    class CutTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                val rootNode = accessibilityService?.rootInActiveWindow
                val focusedNode = findFocusedEditableNode(rootNode)
                
                if (focusedNode != null) {
                    val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)
                    if (success) {
                        createSuccessResult(command, "Cut selected text")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to cut text")
                    }
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No editable text field focused")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to cut text: ${e.message}")
            }
        }
    }
    
    /**
     * Paste Text Action
     */
    class PasteTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                val rootNode = accessibilityService?.rootInActiveWindow
                val focusedNode = findFocusedEditableNode(rootNode)
                
                if (focusedNode != null) {
                    val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                    if (success) {
                        val clipboardText = getClipboardText(context)
                        createSuccessResult(command, "Pasted text: '$clipboardText'")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to paste text")
                    }
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No editable text field focused")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to paste text: ${e.message}")
            }
        }
    }
    
    /**
     * Select All Text Action
     */
    class SelectAllAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                val rootNode = accessibilityService?.rootInActiveWindow
                val focusedNode = findFocusedEditableNode(rootNode)
                
                if (focusedNode != null) {
                    val success = focusedNode.performAction(ACTION_SELECT_ALL)
                    if (success) {
                        createSuccessResult(command, "Selected all text")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to select all text")
                    }
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No editable text field focused")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to select all text: ${e.message}")
            }
        }
    }
    
    /**
     * Select Text Action
     */
    class SelectTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val targetText = (command.parameters["text"] as? String)
            val startPos = (command.parameters["start"] as? Number)?.toInt()
            val endPos = (command.parameters["end"] as? Number)?.toInt()
            
            return try {
                val rootNode = accessibilityService?.rootInActiveWindow
                
                when {
                    targetText != null -> {
                        // Select specific text by content
                        selectTextByContent(rootNode, targetText, command)
                    }
                    startPos != null && endPos != null -> {
                        // Select text by position
                        selectTextByPosition(rootNode, startPos, endPos, command)
                    }
                    else -> {
                        createErrorResult(command, ErrorCode.INVALID_PARAMETERS, 
                            "Specify either text content or start/end positions")
                    }
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to select text: ${e.message}")
            }
        }
    }
    
    /**
     * Replace Text Action
     */
    class ReplaceTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val oldText = (command.parameters["oldText"] as? String)
            val newText = (command.parameters["newText"] as? String) ?: ""
            
            return if (oldText == null) {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No text specified to replace")
            } else {
                try {
                    val rootNode = accessibilityService?.rootInActiveWindow
                    val focusedNode = findFocusedEditableNode(rootNode)
                    
                    if (focusedNode != null) {
                        val currentText = focusedNode.text?.toString() ?: ""
                        val replacedText = currentText.replace(oldText, newText)
                        
                        val bundle = android.os.Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replacedText)
                        }
                        
                        val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                        if (success) {
                            createSuccessResult(command, "Replaced '$oldText' with '$newText'")
                        } else {
                            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to replace text")
                        }
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No editable text field focused")
                    }
                } catch (e: Exception) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to replace text: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Find Text Action
     */
    class FindTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val searchText = (command.parameters["text"] as? String)
            
            return if (searchText == null) {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No text specified to find")
            } else {
                try {
                    val rootNode = accessibilityService?.rootInActiveWindow
                    val foundNodes = findAllTextNodes(rootNode, searchText)
                    
                    if (foundNodes.isNotEmpty()) {
                        val locations = foundNodes.mapIndexed { index, node ->
                            val bounds = android.graphics.Rect()
                            node.getBoundsInScreen(bounds)
                            "Match ${index + 1}: (${bounds.centerX()}, ${bounds.centerY()})"
                        }
                        
                        val message = "Found ${foundNodes.size} occurrence(s) of '$searchText'"
                        createSuccessResult(command, message, locations)
                    } else {
                        createSuccessResult(command, "Text '$searchText' not found", emptyList<String>())
                    }
                } catch (e: Exception) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to find text: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Get Text Content Action
     */
    class GetTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                val rootNode = accessibilityService?.rootInActiveWindow
                val focusedNode = findFocusedEditableNode(rootNode)
                
                if (focusedNode != null) {
                    val text = focusedNode.text?.toString() ?: ""
                    if (text.isNotEmpty()) {
                        createSuccessResult(command, "Current text: '$text'", text)
                    } else {
                        createSuccessResult(command, "Text field is empty", "")
                    }
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No text field focused")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to get text: ${e.message}")
            }
        }
    }
    
    /**
     * Insert Text Action
     */
    class InsertTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val text = (command.parameters["text"] as? String)
            val position = (command.parameters["position"] as? Number)?.toInt()
            
            return if (text == null) {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No text specified to insert")
            } else {
                try {
                    val rootNode = accessibilityService?.rootInActiveWindow
                    val focusedNode = findFocusedEditableNode(rootNode)
                    
                    if (focusedNode != null) {
                        if (position != null) {
                            // Insert at specific position
                            insertTextAtPosition(focusedNode, text, position)
                        } else {
                            // Insert at current cursor position
                            val bundle = android.os.Bundle().apply {
                                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                            }
                            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                        }
                        createSuccessResult(command, "Inserted text: '$text'")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No editable text field focused")
                    }
                } catch (e: Exception) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to insert text: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Undo Text Action
     */
    class UndoAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                // Most apps don't support accessibility undo, so this is limited
                createSuccessResult(command, "Undo not supported via accessibility API")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to undo: ${e.message}")
            }
        }
    }
    
    /**
     * Redo Text Action
     */
    class RedoAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                // Most apps don't support accessibility redo, so this is limited
                createSuccessResult(command, "Redo not supported via accessibility API")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to redo: ${e.message}")
            }
        }
    }
    
    // Helper methods
    
    /**
     * Copy text to clipboard
     */
    private fun copyToClipboard(context: Context, text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("VOS4", text)
        clipboardManager.setPrimaryClip(clipData)
    }
    
    /**
     * Get text from clipboard
     */
    private fun getClipboardText(context: Context): String {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboardManager.primaryClip
        return if (clipData != null && clipData.itemCount > 0) {
            clipData.getItemAt(0).text?.toString() ?: ""
        } else {
            ""
        }
    }
    
    /**
     * Copy currently selected text
     */
    private fun copySelectedText(rootNode: AccessibilityNodeInfo?): Boolean {
        val focusedNode = findFocusedEditableNode(rootNode)
        return focusedNode?.performAction(AccessibilityNodeInfo.ACTION_COPY) ?: false
    }
    
    /**
     * Find focused editable node
     */
    private fun findFocusedEditableNode(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        
        if (rootNode.isEditable && rootNode.isFocused) return rootNode
        
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            child?.let {
                val found = findFocusedEditableNode(it)
                if (found != null) return found
            }
        }
        
        return null
    }
    
    /**
     * Select text by content
     */
    private fun selectTextByContent(
        rootNode: AccessibilityNodeInfo?,
        targetText: String,
        command: Command
    ): CommandResult {
        val focusedNode = findFocusedEditableNode(rootNode)
        
        return if (focusedNode != null) {
            val currentText = focusedNode.text?.toString() ?: ""
            val startIndex = currentText.indexOf(targetText)
            
            if (startIndex >= 0) {
                val endIndex = startIndex + targetText.length
                selectTextByPosition(rootNode, startIndex, endIndex, command)
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Text '$targetText' not found")
            }
        } else {
            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No editable text field focused")
        }
    }
    
    /**
     * Select text by position
     */
    private fun selectTextByPosition(
        rootNode: AccessibilityNodeInfo?,
        startPos: Int,
        endPos: Int,
        command: Command
    ): CommandResult {
        val focusedNode = findFocusedEditableNode(rootNode)
        
        return if (focusedNode != null) {
            val bundle = android.os.Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, startPos)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, endPos)
            }
            
            val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)
            if (success) {
                createSuccessResult(command, "Selected text from position $startPos to $endPos")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to select text")
            }
        } else {
            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No editable text field focused")
        }
    }
    
    /**
     * Find all nodes containing specific text
     */
    private fun findAllTextNodes(rootNode: AccessibilityNodeInfo?, searchText: String): List<AccessibilityNodeInfo> {
        if (rootNode == null) return emptyList()
        
        val results = mutableListOf<AccessibilityNodeInfo>()
        val searchLower = searchText.lowercase()
        
        val nodeText = rootNode.text?.toString()?.lowercase() ?: ""
        val contentDesc = rootNode.contentDescription?.toString()?.lowercase() ?: ""
        
        if (nodeText.contains(searchLower) || contentDesc.contains(searchLower)) {
            results.add(rootNode)
        }
        
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            child?.let {
                results.addAll(findAllTextNodes(it, searchText))
            }
        }
        
        return results
    }
    
    /**
     * Insert text at specific position
     */
    private fun insertTextAtPosition(node: AccessibilityNodeInfo, text: String, position: Int) {
        val currentText = node.text?.toString() ?: ""
        val newText = if (position <= currentText.length) {
            currentText.substring(0, position) + text + currentText.substring(position)
        } else {
            currentText + text
        }
        
        val bundle = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }
}