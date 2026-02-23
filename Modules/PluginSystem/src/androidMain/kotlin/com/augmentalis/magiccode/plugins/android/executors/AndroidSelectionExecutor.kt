/**
 * AndroidSelectionExecutor.kt - Android implementation of SelectionPlugin executors
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Bridges the SelectionPlugin to Android for clipboard and selection operations.
 * Provides separate implementations for IClipboardProvider and ISelectionExecutor
 * to avoid method signature conflicts.
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
import com.augmentalis.magiccode.plugins.builtin.IClipboardProvider
import com.augmentalis.magiccode.plugins.builtin.ISelectionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of ISelectionExecutor.
 *
 * Handles text selection operations via AccessibilityService.
 *
 * @param serviceRegistry Registry to retrieve AccessibilityService from
 */
// recycle() deprecated API 34+ (no-op on 34+, still needed for minSdk 29)
@Suppress("DEPRECATION")
class AndroidSelectionExecutor(
    private val serviceRegistry: ServiceRegistry
) : ISelectionExecutor {

    private val accessibilityService: AccessibilityService?
        get() = serviceRegistry.getSync(ServiceRegistry.ACCESSIBILITY_SERVICE)

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

    override suspend fun selectText(text: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val focusedNode = findFocusedEditableNode(rootNode)
        rootNode.recycle()

        return if (focusedNode != null) {
            val fullText = focusedNode.text?.toString() ?: ""
            val start = fullText.indexOf(text, ignoreCase = true)
            if (start >= 0) {
                val bundle = Bundle().apply {
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, start + text.length)
                }
                val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)
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

    override suspend fun clearSelection(): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val focusedNode = findFocusedEditableNode(rootNode)
        rootNode.recycle()

        return if (focusedNode != null) {
            // Set selection to cursor position (collapse selection)
            val bundle = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 0)
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)
            focusedNode.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun getSelectedText(): String? {
        val service = accessibilityService ?: return null
        val rootNode = service.rootInActiveWindow ?: return null

        val focusedNode = findFocusedEditableNode(rootNode)
        rootNode.recycle()

        return if (focusedNode != null) {
            val text = focusedNode.text?.toString()
            val start = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                focusedNode.textSelectionStart
            } else -1
            val end = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                focusedNode.textSelectionEnd
            } else -1

            focusedNode.recycle()

            if (text != null && start >= 0 && end > start && end <= text.length) {
                text.substring(start, end)
            } else {
                null
            }
        } else {
            null
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
}

/**
 * Android implementation of IClipboardProvider.
 *
 * Handles direct clipboard operations (copy text to clipboard, get clipboard text).
 *
 * @param serviceRegistry Registry to retrieve AccessibilityService from
 */
class AndroidClipboardProvider(
    private val serviceRegistry: ServiceRegistry
) : IClipboardProvider {

    private val accessibilityService: AccessibilityService?
        get() = serviceRegistry.getSync(ServiceRegistry.ACCESSIBILITY_SERVICE)

    private val clipboardManager: ClipboardManager?
        get() = accessibilityService?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    override suspend fun copy(text: String): Boolean = withContext(Dispatchers.Main) {
        val clipboard = clipboardManager ?: return@withContext false
        val clip = ClipData.newPlainText("text", text)
        clipboard.setPrimaryClip(clip)
        true
    }

    override suspend fun paste(): String? = withContext(Dispatchers.Main) {
        val clipboard = clipboardManager ?: return@withContext null
        if (!clipboard.hasPrimaryClip()) return@withContext null

        val item = clipboard.primaryClip?.getItemAt(0)
        item?.text?.toString()
    }

    override suspend fun clear(): Boolean = withContext(Dispatchers.Main) {
        val clipboard = clipboardManager ?: return@withContext false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
            true
        } else {
            // Pre-P: Set empty clip
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            true
        }
    }

    override suspend fun hasContent(): Boolean = withContext(Dispatchers.Main) {
        clipboardManager?.hasPrimaryClip() == true
    }

    override suspend fun getContentType(): String? = withContext(Dispatchers.Main) {
        val clipboard = clipboardManager ?: return@withContext null
        if (!clipboard.hasPrimaryClip()) return@withContext null

        val clip = clipboard.primaryClip ?: return@withContext null
        if (clip.description.mimeTypeCount > 0) {
            clip.description.getMimeType(0)
        } else {
            null
        }
    }
}
