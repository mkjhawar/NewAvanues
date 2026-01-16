/**
 * AndroidInputExecutor.kt - Android input executor
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Android implementation of InputExecutor using AccessibilityService.
 */
package com.augmentalis.voiceoscoreng.handlers

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Android implementation of [InputExecutor].
 *
 * Uses AccessibilityService to perform text input and editing actions.
 *
 * @param accessibilityServiceProvider Provider for the accessibility service instance
 */
class AndroidInputExecutor(
    private val accessibilityServiceProvider: () -> AccessibilityService?
) : InputExecutor {

    override suspend fun enterText(text: String): Boolean {
        val focusedNode = findFocusedNode() ?: return false

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

    override suspend fun deleteCharacter(): Boolean {
        val focusedNode = findFocusedNode() ?: return false

        // Fallback to setting text minus last character
        val currentText = focusedNode.text?.toString() ?: return false
        if (currentText.isEmpty()) return false

        val newText = currentText.dropLast(1)
        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
        }
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    override suspend fun clearText(): Boolean {
        val focusedNode = findFocusedNode() ?: return false

        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                ""
            )
        }
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    override suspend fun selectAll(): Boolean {
        val focusedNode = findFocusedNode() ?: return false
        val textLength = focusedNode.text?.length ?: return false

        val arguments = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, textLength)
        }
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments)
    }

    override suspend fun copy(): Boolean {
        val focusedNode = findFocusedNode() ?: return false
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_COPY)
    }

    override suspend fun cut(): Boolean {
        val focusedNode = findFocusedNode() ?: return false
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)
    }

    override suspend fun paste(): Boolean {
        val focusedNode = findFocusedNode() ?: return false
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    override suspend fun undo(): Boolean {
        // Undo is not directly supported in accessibility API
        // Would need to track text history manually
        return false
    }

    override suspend fun redo(): Boolean {
        // Redo is not directly supported in accessibility API
        // Would need to track text history manually
        return false
    }

    override suspend fun search(query: String): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false
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

    /**
     * Find the currently focused input node.
     */
    private fun findFocusedNode(): AccessibilityNodeInfo? {
        val service = accessibilityServiceProvider() ?: return null
        val rootNode = service.rootInActiveWindow ?: return null
        return rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
    }

    /**
     * Find a search field in the current window.
     */
    private fun findSearchField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for nodes with search-related descriptions
        if (node.isEditable) {
            val description = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""
            val hint = node.hintText?.toString()?.lowercase() ?: ""

            if (description.contains("search") ||
                text.contains("search") ||
                hint.contains("search")
            ) {
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
