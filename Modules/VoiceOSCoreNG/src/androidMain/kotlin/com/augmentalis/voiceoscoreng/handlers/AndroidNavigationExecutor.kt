/**
 * AndroidNavigationExecutor.kt - Android navigation executor
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Android implementation of NavigationExecutor using AccessibilityService.
 */
package com.augmentalis.voiceoscoreng.handlers

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Android implementation of [NavigationExecutor].
 *
 * Uses AccessibilityService to perform scroll and navigation actions.
 *
 * @param accessibilityServiceProvider Provider for the accessibility service instance
 */
class AndroidNavigationExecutor(
    private val accessibilityServiceProvider: () -> AccessibilityService?
) : NavigationExecutor {

    override suspend fun scrollUp(): Boolean {
        return performScrollAction(
            action = AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
            horizontal = false
        )
    }

    override suspend fun scrollDown(): Boolean {
        return performScrollAction(
            action = AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            horizontal = false
        )
    }

    override suspend fun scrollLeft(): Boolean {
        return performScrollAction(
            action = AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
            horizontal = true
        )
    }

    override suspend fun scrollRight(): Boolean {
        return performScrollAction(
            action = AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            horizontal = true
        )
    }

    override suspend fun next(): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false
        return rootNode.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY)
    }

    override suspend fun previous(): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false
        return rootNode.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY)
    }

    /**
     * Perform a scroll action on the current window.
     *
     * @param action The accessibility action to perform
     * @param horizontal Whether to look for horizontal scrollable nodes
     * @return true if action was successful
     */
    private fun performScrollAction(action: Int, horizontal: Boolean): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(rootNode, horizontal) ?: return false
        return scrollableNode.performAction(action)
    }

    /**
     * Find a scrollable node in the tree.
     *
     * @param node The node to search from
     * @param horizontal Whether to prefer horizontal scrollable nodes
     * @return The first scrollable node found, or null
     */
    private fun findScrollableNode(
        node: AccessibilityNodeInfo,
        horizontal: Boolean
    ): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            return node
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findScrollableNode(child, horizontal)
                if (result != null) return result
            }
        }

        return null
    }
}
