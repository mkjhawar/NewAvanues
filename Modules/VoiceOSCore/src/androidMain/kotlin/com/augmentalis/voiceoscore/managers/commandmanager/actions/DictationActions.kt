/**
 * DictationActions.kt - Dictation and keyboard command actions
 * Path: modules/commands/src/main/java/com/augmentalis/voiceos/commands/actions/DictationActions.kt
 * 
 * Created: 2025-08-19
 * Author: Claude Code
 * Module: Commands
 * 
 * Purpose: Dictation, keyboard, and text input-related voice command actions
 */

package com.augmentalis.voiceoscore.managers.commandmanager.actions

import com.augmentalis.voiceoscore.*
// ACTION_SELECT_ALL import removed
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager

/**
 * Dictation and keyboard command actions
 * Handles text input, keyboard control, and dictation functionality
 */
object DictationActions {
    
    // Dictation state
    private var isDictating = false
    private var dictationBuffer = StringBuilder()
    
    /**
     * Start Dictation Action
     */
    class StartDictationAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return if (isDictating) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Dictation already active")
            } else {
                isDictating = true
                dictationBuffer.clear()
                createSuccessResult(command, "Dictation started")
            }
        }
    }
    
    /**
     * End Dictation Action
     */
    class EndDictationAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return if (!isDictating) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No active dictation session")
            } else {
                val dictatedText = dictationBuffer.toString()
                isDictating = false
                dictationBuffer.clear()
                
                // Insert the dictated text
                if (dictatedText.isNotEmpty()) {
                    insertText(accessibilityService, dictatedText)
                }
                
                createSuccessResult(command, "Dictation ended: '$dictatedText'", dictatedText)
            }
        }
    }
    
    /**
     * Dictate Text Action (for continuous dictation)
     */
    class DictateTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val text = getTextParameter(command, "text")
            
            return if (text == null) {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No text provided for dictation")
            } else {
                if (isDictating) {
                    dictationBuffer.append(text).append(" ")
                    createSuccessResult(command, "Added to dictation: '$text'")
                } else {
                    // Direct text insertion if not in dictation mode
                    insertText(accessibilityService, text)
                    createSuccessResult(command, "Inserted text: '$text'")
                }
            }
        }
    }
    
    /**
     * Show Keyboard Action
     */
    class ShowKeyboardAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                
                // Try to find a text input field and focus it
                val rootNode = accessibilityService?.rootInActiveWindow
                val textField = findEditableNode(rootNode)
                
                if (textField != null) {
                    textField.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    textField.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    createSuccessResult(command, "Keyboard shown")
                } else {
                    // Alternative: try to show soft keyboard
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // For Android 12+, focus a text field if possible
                        val focusedNode = accessibilityService?.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                        if (focusedNode != null) {
                            focusedNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                    }
                    createSuccessResult(command, "Attempted to show keyboard")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to show keyboard: ${e.message}")
            }
        }
    }
    
    /**
     * Hide Keyboard Action
     */
    class HideKeyboardAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // For Android 12+, clear focus from text fields
                    val rootNode = accessibilityService?.rootInActiveWindow
                    val focusedNode = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    focusedNode?.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
                    // Also try to hide using the accessibility service if available
                    accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                } else {
                    @Suppress("DEPRECATION")
                    inputMethodManager.toggleSoftInput(0, 0)
                }
                createSuccessResult(command, "Keyboard hidden")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to hide keyboard: ${e.message}")
            }
        }
    }
    
    /**
     * Backspace Action
     */
    class BackspaceAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val count = getNumberParameter(command, "count")?.toInt() ?: 1
            
            return try {
                val rootNode = accessibilityService?.rootInActiveWindow
                val focusedNode = findFocusedEditableNode(rootNode)
                
                if (focusedNode != null) {
                    repeat(count) {
                        // Try to delete using accessibility action
                        val selection = focusedNode.textSelectionStart
                        if (selection > 0) {
                            val bundle = android.os.Bundle().apply {
                                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selection - 1)
                                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selection)
                            }
                            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)
                            focusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)
                        }
                    }
                    createSuccessResult(command, "Deleted $count character(s)")
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No focused text field found")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to backspace: ${e.message}")
            }
        }
    }
    
    /**
     * Clear Text Action
     */
    class ClearTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                val rootNode = accessibilityService?.rootInActiveWindow
                val focusedNode = findFocusedEditableNode(rootNode)
                
                if (focusedNode != null) {
                    // Select all text and delete
                    focusedNode.performAction(0x20000) // ACTION_SELECT_ALL value
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)
                    createSuccessResult(command, "Text cleared")
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No focused text field found")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to clear text: ${e.message}")
            }
        }
    }
    
    /**
     * Enter/Return Action
     */
    class EnterAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                insertText(accessibilityService, "\n")
                createSuccessResult(command, "Enter pressed")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to press enter: ${e.message}")
            }
        }
    }
    
    /**
     * Space Action
     */
    class SpaceAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val count = getNumberParameter(command, "count")?.toInt() ?: 1
            
            return try {
                val spaces = " ".repeat(count)
                insertText(accessibilityService, spaces)
                createSuccessResult(command, "Inserted $count space(s)")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to insert space: ${e.message}")
            }
        }
    }
    
    /**
     * Tab Action
     */
    class TabAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                insertText(accessibilityService, "\t")
                createSuccessResult(command, "Tab inserted")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to insert tab: ${e.message}")
            }
        }
    }
    
    /**
     * Type Text Action
     */
    class TypeTextAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val text = getTextParameter(command, "text")
            
            return if (text == null) {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No text provided to type")
            } else {
                try {
                    insertText(accessibilityService, text)
                    createSuccessResult(command, "Typed: '$text'")
                } catch (e: Exception) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to type text: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Insert Symbol Action
     */
    class InsertSymbolAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val symbol = getTextParameter(command, "symbol")
            
            return if (symbol == null) {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No symbol provided")
            } else {
                try {
                    val actualSymbol = when (symbol.lowercase()) {
                        "period", "dot" -> "."
                        "comma" -> ","
                        "question mark" -> "?"
                        "exclamation mark", "exclamation" -> "!"
                        "semicolon" -> ";"
                        "colon" -> ":"
                        "apostrophe", "quote" -> "'"
                        "quotation mark", "double quote" -> "\""
                        "hyphen", "dash" -> "-"
                        "underscore" -> "_"
                        "at sign", "at" -> "@"
                        "hash", "pound" -> "#"
                        "dollar" -> "$"
                        "percent" -> "%"
                        "ampersand" -> "&"
                        "asterisk", "star" -> "*"
                        "plus" -> "+"
                        "equals" -> "="
                        "open parenthesis", "left parenthesis" -> "("
                        "close parenthesis", "right parenthesis" -> ")"
                        "open bracket", "left bracket" -> "["
                        "close bracket", "right bracket" -> "]"
                        "open brace", "left brace" -> "{"
                        "close brace", "right brace" -> "}"
                        "less than" -> "<"
                        "greater than" -> ">"
                        "slash", "forward slash" -> "/"
                        "backslash" -> "\\"
                        "pipe", "vertical bar" -> "|"
                        "tilde" -> "~"
                        "grave", "backtick" -> "`"
                        "caret" -> "^"
                        else -> symbol // Use as-is if not recognized
                    }
                    
                    insertText(accessibilityService, actualSymbol)
                    createSuccessResult(command, "Inserted symbol: '$actualSymbol'")
                } catch (e: Exception) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to insert symbol: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Open Input Method Settings Action
     */
    class InputMethodSettingsAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return try {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                createSuccessResult(command, "Opened input method settings")
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to open input method settings: ${e.message}")
            }
        }
    }
    
    // Helper methods
    
    /**
     * Insert text into the focused text field
     */
    private fun insertText(accessibilityService: AccessibilityService?, text: String): Boolean {
        val rootNode = accessibilityService?.rootInActiveWindow
        val focusedNode = findFocusedEditableNode(rootNode)
        
        return if (focusedNode != null) {
            val bundle = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        } else {
            false
        }
    }
    
    /**
     * Find any editable node
     */
    private fun findEditableNode(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        
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
     * Find the focused editable node
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
}