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
import android.view.KeyEvent
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
            phrase in listOf("select all", "highlight all") -> performSelectAll(focusedNode)

            phrase in listOf("copy", "copy that", "copy text") -> performCopy(focusedNode)

            phrase in listOf("paste", "paste text") ->
                performNodeAction(focusedNode, AccessibilityNodeInfo.ACTION_PASTE, "Pasted")

            phrase in listOf("cut", "cut text") -> performCut(focusedNode)

            phrase in listOf("undo", "undo that", "take back") -> performUndo()
            phrase in listOf("redo", "redo that") -> performRedo()

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

    /**
     * Select all text using ACTION_SET_SELECTION with full range.
     *
     * ACTION_SELECT (value 4) selects the *node* for accessibility focus,
     * NOT the text content. ACTION_SET_SELECTION with start=0, end=length
     * is the correct API for text selection.
     */
    private fun performSelectAll(node: AccessibilityNodeInfo?): HandlerResult {
        if (node == null) return HandlerResult.failure("No text field focused")
        val text = node.text?.toString()
        if (text.isNullOrEmpty()) return HandlerResult.failure("No text to select")

        return try {
            val args = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length)
            }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
            if (success) HandlerResult.success("All text selected")
            else HandlerResult.failure("Failed to select all")
        } catch (e: Exception) {
            HandlerResult.failure("Select all failed: ${e.message}")
        }
    }

    /**
     * Copy with pre-check: verify text is actually selected before attempting copy.
     */
    private fun performCopy(node: AccessibilityNodeInfo?): HandlerResult {
        if (node == null) return HandlerResult.failure("No text field focused")
        val selStart = node.textSelectionStart
        val selEnd = node.textSelectionEnd
        if (selStart < 0 || selEnd <= selStart) {
            return HandlerResult.failure("No text selected — say 'select all' first")
        }
        return performNodeAction(node, AccessibilityNodeInfo.ACTION_COPY, "Copied")
    }

    /**
     * Cut with pre-check: verify text is actually selected before attempting cut.
     */
    private fun performCut(node: AccessibilityNodeInfo?): HandlerResult {
        if (node == null) return HandlerResult.failure("No text field focused")
        val selStart = node.textSelectionStart
        val selEnd = node.textSelectionEnd
        if (selStart < 0 || selEnd <= selStart) {
            return HandlerResult.failure("No text selected — say 'select all' first")
        }
        return performNodeAction(node, AccessibilityNodeInfo.ACTION_CUT, "Cut")
    }

    /**
     * Undo via Ctrl+Z key event injection.
     *
     * Android Accessibility API doesn't expose undo directly. We inject
     * a Ctrl+Z key combination which most text editors honor (Chrome,
     * Gmail, Keep, etc.). Falls back gracefully if injection fails.
     */
    private fun performUndo(): HandlerResult {
        return try {
            val success = injectKeyCombo(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON)
            if (success) HandlerResult.success("Undone")
            else HandlerResult.failure("Undo not supported in this app")
        } catch (e: Exception) {
            HandlerResult.failure("Undo failed: ${e.message}")
        }
    }

    /**
     * Redo via Ctrl+Shift+Z key event injection.
     */
    private fun performRedo(): HandlerResult {
        return try {
            val success = injectKeyCombo(
                KeyEvent.KEYCODE_Z,
                KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON
            )
            if (success) HandlerResult.success("Redone")
            else HandlerResult.failure("Redo not supported in this app")
        } catch (e: Exception) {
            HandlerResult.failure("Redo failed: ${e.message}")
        }
    }

    /**
     * Inject a key combination via the Android `input` shell command.
     *
     * AccessibilityService cannot use Instrumentation.sendKeySync() (requires
     * signature-level INJECT_EVENTS permission) or InputConnection (not available).
     * The `input keyevent` shell command is the most reliable cross-device approach
     * for injecting key combos like Ctrl+Z from an accessibility service process.
     *
     * @param keyCode Android KeyEvent keycode (e.g., KeyEvent.KEYCODE_Z)
     * @param metaState Modifier keys (e.g., KeyEvent.META_CTRL_ON)
     * @return true if injection succeeded, false otherwise
     */
    private fun injectKeyCombo(keyCode: Int, metaState: Int): Boolean {
        return try {
            // Build the `input keyevent` command with modifier keys.
            // `input keyevent --longpress K1 K2` sends K1 down, K2 down+up, K1 up — simulating a combo.
            val keyCodes = mutableListOf<String>()
            if (metaState and KeyEvent.META_CTRL_ON != 0) {
                keyCodes.add(KeyEvent.KEYCODE_CTRL_LEFT.toString())
            }
            if (metaState and KeyEvent.META_SHIFT_ON != 0) {
                keyCodes.add(KeyEvent.KEYCODE_SHIFT_LEFT.toString())
            }
            keyCodes.add(keyCode.toString())

            val command = "input keyevent --longpress ${keyCodes.joinToString(" ")}"
            Log.d(TAG, "Injecting key combo: $command")

            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Log.d(TAG, "Key combo injected successfully")
                true
            } else {
                Log.w(TAG, "Key injection returned exit code $exitCode")
                false
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Key injection blocked by security policy: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Key injection failed", e)
            false
        }
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
