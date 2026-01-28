/**
 * EditingActions.kt - Text editing command actions
 * Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/EditingActions.kt
 *
 * Created: 2025-10-10 20:30 PDT
 * Module: CommandManager
 *
 * Purpose: Text manipulation commands (copy, paste, cut, select, undo, redo)
 * Reference: IMPLEMENTATION-INSTRUCTIONS-251010-1734.md - Task 2.2
 */

package com.augmentalis.commandmanager.actions

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.*

/**
 * Editing actions implementation
 * Handles copy, paste, cut, select all, undo, redo operations
 */
class EditingActions(
    private val context: Context,
    private val accessibilityService: AccessibilityService?
) : BaseAction() {

    companion object {
        private const val TAG = "EditingActions"

        // Supported commands
        const val CMD_COPY = "copy"
        const val CMD_PASTE = "paste"
        const val CMD_CUT = "cut"
        const val CMD_SELECT_ALL = "select all"
        const val CMD_UNDO = "undo"
        const val CMD_REDO = "redo"

        // Custom action IDs for undo/redo (not in Android SDK)
        // Using values that don't conflict with standard actions
        private const val ACTION_UNDO = 0x01020036  // android.R.id.undo equivalent
        private const val ACTION_REDO = 0x01020037  // android.R.id.redo equivalent
    }

    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        return when (command.text.lowercase()) {
            CMD_COPY -> performCopy(command)
            CMD_PASTE -> performPaste(command)
            CMD_CUT -> performCut(command)
            CMD_SELECT_ALL -> performSelectAll(command)
            CMD_UNDO -> performUndo(command)
            CMD_REDO -> performRedo(command)
            else -> createErrorResult(
                command,
                ErrorCode.UNKNOWN_COMMAND,
                "Unknown editing command: ${command.text}"
            )
        }
    }

    /**
     * Copy selected text to clipboard
     * Uses AccessibilityService.performGlobalAction() for system-wide copy
     */
    private fun performCopy(command: Command): CommandResult {
        return try {
            val success = accessibilityService?.performGlobalAction(
                AccessibilityNodeInfo.ACTION_COPY
            ) ?: false

            if (success) {
                android.util.Log.d(TAG, "Copy action successful")
                createSuccessResult(command, "Text copied")
            } else {
                // Fallback: Try node-level copy
                val fallbackSuccess = performNodeCopy()
                if (fallbackSuccess) {
                    createSuccessResult(command, "Text copied (fallback)")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Copy failed - no text selected"
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Copy action failed", e)
            createErrorResult(
                command,
                ErrorCode.EXECUTION_FAILED,
                "Copy failed: ${e.message}"
            )
        }
    }

    /**
     * Paste clipboard content
     * Uses AccessibilityService.performGlobalAction() for system-wide paste
     */
    private fun performPaste(command: Command): CommandResult {
        return try {
            // Check if clipboard has content
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (!clipboardManager.hasPrimaryClip()) {
                return createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Clipboard is empty"
                )
            }

            val success = accessibilityService?.performGlobalAction(
                AccessibilityNodeInfo.ACTION_PASTE
            ) ?: false

            if (success) {
                android.util.Log.d(TAG, "Paste action successful")
                createSuccessResult(command, "Text pasted")
            } else {
                // Fallback: Try node-level paste
                val fallbackSuccess = performNodePaste()
                if (fallbackSuccess) {
                    createSuccessResult(command, "Text pasted (fallback)")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Paste failed - no editable field focused"
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Paste action failed", e)
            createErrorResult(
                command,
                ErrorCode.EXECUTION_FAILED,
                "Paste failed: ${e.message}"
            )
        }
    }

    /**
     * Cut selected text to clipboard
     * Copy + delete selected text
     */
    private fun performCut(command: Command): CommandResult {
        return try {
            val rootNode = accessibilityService?.rootInActiveWindow
            val focusedNode = findFocusedEditableNode(rootNode)

            if (focusedNode != null) {
                // Perform cut action on node
                val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)

                if (success) {
                    android.util.Log.d(TAG, "Cut action successful")
                    createSuccessResult(command, "Text cut")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Cut failed - no text selected"
                    )
                }
            } else {
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Cut failed - no editable field focused"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Cut action failed", e)
            createErrorResult(
                command,
                ErrorCode.EXECUTION_FAILED,
                "Cut failed: ${e.message}"
            )
        }
    }

    /**
     * Select all text in focused field
     * Uses AccessibilityNodeInfo.ACTION_SELECT_ALL
     */
    private fun performSelectAll(command: Command): CommandResult {
        return try {
            val rootNode = accessibilityService?.rootInActiveWindow
            val focusedNode = findFocusedEditableNode(rootNode)

            if (focusedNode != null) {
                // Use ACTION_SELECT_ALL constant value (0x20000)
                val success = focusedNode.performAction(0x20000)

                if (success) {
                    android.util.Log.d(TAG, "Select all successful")
                    createSuccessResult(command, "All text selected")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Select all failed"
                    )
                }
            } else {
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Select all failed - no editable field focused"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Select all failed", e)
            createErrorResult(
                command,
                ErrorCode.EXECUTION_FAILED,
                "Select all failed: ${e.message}"
            )
        }
    }

    /**
     * Undo last edit
     * Uses KeyEvent or node-level undo if available
     */
    private fun performUndo(command: Command): CommandResult {
        return try {
            val rootNode = accessibilityService?.rootInActiveWindow
            val focusedNode = findFocusedEditableNode(rootNode)

            if (focusedNode != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Try to perform undo action (API 24+)
                val success = focusedNode.performAction(ACTION_UNDO)

                if (success) {
                    android.util.Log.d(TAG, "Undo successful")
                    createSuccessResult(command, "Undo performed")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Undo not available"
                    )
                }
            } else {
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Undo not supported on this device"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Undo failed", e)
            createErrorResult(
                command,
                ErrorCode.EXECUTION_FAILED,
                "Undo failed: ${e.message}"
            )
        }
    }

    /**
     * Redo last undo
     * Uses KeyEvent or node-level redo if available
     */
    private fun performRedo(command: Command): CommandResult {
        return try {
            val rootNode = accessibilityService?.rootInActiveWindow
            val focusedNode = findFocusedEditableNode(rootNode)

            if (focusedNode != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Try to perform redo action (API 24+)
                val success = focusedNode.performAction(ACTION_REDO)

                if (success) {
                    android.util.Log.d(TAG, "Redo successful")
                    createSuccessResult(command, "Redo performed")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Redo not available"
                    )
                }
            } else {
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Redo not supported on this device"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Redo failed", e)
            createErrorResult(
                command,
                ErrorCode.EXECUTION_FAILED,
                "Redo failed: ${e.message}"
            )
        }
    }

    // ========== Helper Methods ==========

    /**
     * Fallback: Perform node-level copy
     */
    private fun performNodeCopy(): Boolean {
        val rootNode = accessibilityService?.rootInActiveWindow
        val focusedNode = findFocusedEditableNode(rootNode)

        return focusedNode?.performAction(AccessibilityNodeInfo.ACTION_COPY) ?: false
    }

    /**
     * Fallback: Perform node-level paste
     */
    private fun performNodePaste(): Boolean {
        val rootNode = accessibilityService?.rootInActiveWindow
        val focusedNode = findFocusedEditableNode(rootNode)

        return if (focusedNode != null) {
            // Get clipboard content
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboardManager.primaryClip

            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text

                // Insert text at cursor position
                val bundle = android.os.Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text
                    )
                }
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE, bundle)
            } else {
                false
            }
        } else {
            false
        }
    }

    /**
     * Find the focused editable node
     */
    private fun findFocusedEditableNode(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        // Check if current node is focused and editable
        if (rootNode.isEditable && rootNode.isFocused) {
            return rootNode
        }

        // Try to find input-focused node
        val inputFocusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (inputFocusedNode != null && inputFocusedNode.isEditable) {
            return inputFocusedNode
        }

        // Recursively search children
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
