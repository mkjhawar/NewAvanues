/**
 * TextHandler.kt - IHandler for text editing commands
 *
 * Handles: copy, paste, cut, select all, undo, redo, delete
 * Uses accessibility node actions on the focused input field.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

private const val TAG = "TextHandler"

class TextHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.INPUT

    override val supportedActions: List<String> = listOf(
        "select all", "highlight all",
        "copy", "copy that", "copy text",
        "paste", "paste text",
        "cut", "cut text",
        "undo", "undo that", "take back",
        "redo", "redo that",
        "delete", "delete that", "erase", "remove"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        Log.d(TAG, "TextHandler.execute: '$phrase'")

        val focusedNode = findFocusedInputNode()

        return when {
            phrase in listOf("select all", "highlight all") ->
                performNodeAction(focusedNode, AccessibilityNodeInfo.ACTION_SELECT, "All text selected")

            phrase in listOf("copy", "copy that", "copy text") ->
                performNodeAction(focusedNode, AccessibilityNodeInfo.ACTION_COPY, "Copied")

            phrase in listOf("paste", "paste text") ->
                performNodeAction(focusedNode, AccessibilityNodeInfo.ACTION_PASTE, "Pasted")

            phrase in listOf("cut", "cut text") ->
                performNodeAction(focusedNode, AccessibilityNodeInfo.ACTION_CUT, "Cut")

            phrase in listOf("undo", "undo that", "take back") -> performUndo(focusedNode)
            phrase in listOf("redo", "redo that") -> performRedo(focusedNode)

            phrase in listOf("delete", "delete that", "erase", "remove") -> performDelete(focusedNode)

            else -> HandlerResult.notHandled()
        }
    }

    private fun findFocusedInputNode(): AccessibilityNodeInfo? {
        return try {
            service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to find focused input node", e)
            null
        }
    }

    private fun performNodeAction(node: AccessibilityNodeInfo?, action: Int, label: String): HandlerResult {
        if (node == null) return HandlerResult.failure("No text field focused")
        return try {
            val success = node.performAction(action)
            if (success) HandlerResult.success(label)
            else HandlerResult.failure("Failed: $label")
        } catch (e: Exception) {
            HandlerResult.failure("Error: ${e.message}")
        }
    }

    private fun performUndo(node: AccessibilityNodeInfo?): HandlerResult {
        if (node == null) return HandlerResult.failure("No text field focused")
        return HandlerResult.failure(
            "Undo not available via Android accessibility API",
            recoverable = true
        )
    }

    private fun performRedo(node: AccessibilityNodeInfo?): HandlerResult {
        if (node == null) return HandlerResult.failure("No text field focused")
        return HandlerResult.failure(
            "Redo not available via Android accessibility API",
            recoverable = true
        )
    }

    private fun performDelete(node: AccessibilityNodeInfo?): HandlerResult {
        if (node == null) return HandlerResult.failure("No text field focused")
        return try {
            val text = node.text?.toString() ?: return HandlerResult.failure("No text to delete")
            if (text.isEmpty()) return HandlerResult.success("Field already empty")

            // Get selection bounds — if text is selected, delete selection; otherwise delete last char
            val selStart = node.textSelectionStart
            val selEnd = node.textSelectionEnd
            val newText = if (selStart >= 0 && selEnd > selStart) {
                // Delete selected text
                text.removeRange(selStart, selEnd)
            } else {
                // Delete last character (backspace behavior)
                text.dropLast(1)
            }

            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText
                )
            }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (success) HandlerResult.success("Character deleted")
            else HandlerResult.failure("Failed to delete text")
        } catch (e: Exception) {
            HandlerResult.failure("Delete failed: ${e.message}")
        }
    }
}
