/**
 * AndroidUIExecutor.kt - Android UI executor
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Android implementation of UIExecutor using AccessibilityService.
 */
package com.augmentalis.voiceoscoreng.handlers

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay

/**
 * Android implementation of [UIExecutor].
 *
 * Uses AccessibilityService to perform UI interactions.
 *
 * @param accessibilityServiceProvider Provider for the accessibility service instance
 * @param vuidLookup Function to lookup element by VUID
 */
class AndroidUIExecutor(
    private val accessibilityServiceProvider: () -> AccessibilityService?,
    private val vuidLookup: (String) -> AccessibilityNodeInfo? = { null }
) : UIExecutor {

    override suspend fun clickByText(text: String): Boolean {
        val node = findNodeByText(text) ?: findNodeByDescription(text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun clickByVuid(vuid: String): Boolean {
        val node = vuidLookup(vuid) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun longClickByText(text: String): Boolean {
        val node = findNodeByText(text) ?: findNodeByDescription(text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    override suspend fun longClickByVuid(vuid: String): Boolean {
        val node = vuidLookup(vuid) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    override suspend fun doubleClickByText(text: String): Boolean {
        val node = findNodeByText(text) ?: findNodeByDescription(text) ?: return false
        val firstClick = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        delay(50) // Small delay between clicks
        val secondClick = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return firstClick && secondClick
    }

    override suspend fun expand(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
    }

    override suspend fun collapse(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
    }

    override suspend fun setChecked(target: String, checked: Boolean): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target) ?: return false

        // Check current state
        val isChecked = node.isChecked
        if (isChecked == checked) return true // Already in desired state

        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun toggle(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun focus(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    override suspend fun dismiss(): Boolean {
        val service = accessibilityServiceProvider() ?: return false

        // Try to find and click dismiss/close button
        val dismissNode = findNodeByText("dismiss")
            ?: findNodeByText("close")
            ?: findNodeByText("cancel")
            ?: findNodeByText("ok")
            ?: findNodeByDescription("dismiss")
            ?: findNodeByDescription("close")

        if (dismissNode != null) {
            return dismissNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Fallback to back action
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * Find a node by its text content.
     */
    private fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val service = accessibilityServiceProvider() ?: return null
        val rootNode = service.rootInActiveWindow ?: return null
        return findNodeByTextRecursive(rootNode, text.lowercase())
    }

    /**
     * Recursively search for node by text.
     */
    private fun findNodeByTextRecursive(
        node: AccessibilityNodeInfo,
        searchText: String
    ): AccessibilityNodeInfo? {
        // Check this node's text
        node.text?.toString()?.lowercase()?.let { nodeText ->
            if (nodeText.contains(searchText)) {
                return node
            }
        }

        // Check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeByTextRecursive(child, searchText)
                if (result != null) return result
            }
        }

        return null
    }

    /**
     * Find a node by its content description.
     */
    private fun findNodeByDescription(description: String): AccessibilityNodeInfo? {
        val service = accessibilityServiceProvider() ?: return null
        val rootNode = service.rootInActiveWindow ?: return null
        return findNodeByDescriptionRecursive(rootNode, description.lowercase())
    }

    /**
     * Recursively search for node by content description.
     */
    private fun findNodeByDescriptionRecursive(
        node: AccessibilityNodeInfo,
        searchDescription: String
    ): AccessibilityNodeInfo? {
        // Check this node's content description
        node.contentDescription?.toString()?.lowercase()?.let { nodeDescription ->
            if (nodeDescription.contains(searchDescription)) {
                return node
            }
        }

        // Check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeByDescriptionRecursive(child, searchDescription)
                if (result != null) return result
            }
        }

        return null
    }
}
