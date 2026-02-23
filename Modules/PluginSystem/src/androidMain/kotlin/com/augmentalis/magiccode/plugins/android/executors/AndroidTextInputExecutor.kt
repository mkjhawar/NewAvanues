/**
 * AndroidTextInputExecutor.kt - Android implementation of TextInputExecutor
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Bridges the TextInputPlugin to Android AccessibilityService for
 * text input, editing, and clipboard operations.
 */
package com.augmentalis.magiccode.plugins.android.executors

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.magiccode.plugins.android.ServiceRegistry
import com.augmentalis.magiccode.plugins.builtin.TextInputExecutor

/**
 * Android implementation of TextInputExecutor.
 *
 * Uses AccessibilityService and InputConnection APIs for text operations.
 *
 * @param serviceRegistry Registry to retrieve AccessibilityService from
 */
// recycle() deprecated API 34+ (no-op on 34+, still needed for minSdk 29)
@Suppress("DEPRECATION")
class AndroidTextInputExecutor(
    private val serviceRegistry: ServiceRegistry
) : TextInputExecutor {

    private val accessibilityService: AccessibilityService?
        get() = serviceRegistry.getSync(ServiceRegistry.ACCESSIBILITY_SERVICE)

    override suspend fun enterText(text: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val focusedNode = findFocusedEditableNode(rootNode) ?: findFirstEditableNode(rootNode)
        rootNode.recycle()

        return if (focusedNode != null) {
            val bundle = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    (focusedNode.text?.toString() ?: "") + text)
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            focusedNode.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun deleteCharacter(): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val focusedNode = findFocusedEditableNode(rootNode)
        rootNode.recycle()

        return if (focusedNode != null) {
            val currentText = focusedNode.text?.toString() ?: ""
            if (currentText.isNotEmpty()) {
                val newText = currentText.dropLast(1)
                val bundle = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                }
                val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                focusedNode.recycle()
                result
            } else {
                focusedNode.recycle()
                false
            }
        } else {
            false
        }
    }

    override suspend fun clearText(): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val focusedNode = findFocusedEditableNode(rootNode)
        rootNode.recycle()

        return if (focusedNode != null) {
            val bundle = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            focusedNode.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun selectAll(): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val focusedNode = findFocusedEditableNode(rootNode)
        rootNode.recycle()

        return if (focusedNode != null) {
            val text = focusedNode.text?.toString() ?: ""
            val bundle = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length)
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)
            focusedNode.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun copy(): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val focusedNode = findFocusedEditableNode(rootNode)
        rootNode.recycle()

        return if (focusedNode != null) {
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_COPY)
            focusedNode.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun cut(): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val focusedNode = findFocusedEditableNode(rootNode)
        rootNode.recycle()

        return if (focusedNode != null) {
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)
            focusedNode.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun paste(): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val focusedNode = findFocusedEditableNode(rootNode)
        rootNode.recycle()

        return if (focusedNode != null) {
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            focusedNode.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun undo(): Boolean {
        // Android doesn't have a universal undo action via AccessibilityService
        // Try to send Ctrl+Z key event - only works on some apps
        return false
    }

    override suspend fun redo(): Boolean {
        // Android doesn't have a universal redo action via AccessibilityService
        return false
    }

    override suspend fun search(query: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        // Try to find a search field and enter the query
        val searchNode = findSearchField(rootNode)
        rootNode.recycle()

        return if (searchNode != null) {
            // Focus and enter text
            searchNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val bundle = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
            }
            val result = searchNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            searchNode.recycle()
            result
        } else {
            false
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private fun findFocusedEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable && root.isFocused) {
            return AccessibilityNodeInfo.obtain(root)
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findFocusedEditableNode(child)
            child.recycle()
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun findFirstEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable && root.isVisibleToUser) {
            return AccessibilityNodeInfo.obtain(root)
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            child.recycle()
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun findSearchField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for editable fields with search-related hints
        if (root.isEditable && root.isVisibleToUser) {
            val hintText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                root.hintText?.toString()?.lowercase() ?: ""
            } else ""
            val contentDesc = root.contentDescription?.toString()?.lowercase() ?: ""
            val resourceId = root.viewIdResourceName?.lowercase() ?: ""

            if (hintText.contains("search") ||
                contentDesc.contains("search") ||
                resourceId.contains("search")) {
                return AccessibilityNodeInfo.obtain(root)
            }
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findSearchField(child)
            child.recycle()
            if (found != null) {
                return found
            }
        }
        return null
    }
}
