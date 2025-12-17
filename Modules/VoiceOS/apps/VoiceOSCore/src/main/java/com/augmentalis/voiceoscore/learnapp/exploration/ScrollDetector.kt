/**
 * ScrollDetector.kt - Detects scrollable areas on screen
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Detects and tracks scrollable areas in the UI hierarchy.
 */
package com.augmentalis.voiceoscore.learnapp.exploration

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Scroll Detector
 *
 * Detects scrollable areas on screen for exploration.
 */
class ScrollDetector {

    /**
     * Find all scrollable nodes
     */
    fun findScrollableNodes(rootNode: AccessibilityNodeInfo?): List<ScrollableArea> {
        if (rootNode == null) return emptyList()

        val scrollables = mutableListOf<ScrollableArea>()
        collectScrollables(rootNode, scrollables)
        return scrollables
    }

    /**
     * Recursively collect scrollable nodes
     */
    private fun collectScrollables(node: AccessibilityNodeInfo, scrollables: MutableList<ScrollableArea>) {
        if (node.isScrollable) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            scrollables.add(ScrollableArea(
                node = node,
                bounds = bounds,
                direction = determineScrollDirection(node),
                canScrollForward = canScrollForward(node),
                canScrollBackward = canScrollBackward(node)
            ))
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectScrollables(child, scrollables)
            }
        }
    }

    /**
     * Determine scroll direction
     */
    private fun determineScrollDirection(node: AccessibilityNodeInfo): ScrollDirection {
        val className = node.className?.toString() ?: ""
        return when {
            className.contains("HorizontalScrollView") -> ScrollDirection.HORIZONTAL
            className.contains("ViewPager") -> ScrollDirection.HORIZONTAL
            else -> ScrollDirection.VERTICAL
        }
    }

    /**
     * Check if node can scroll forward
     */
    private fun canScrollForward(node: AccessibilityNodeInfo): Boolean {
        return node.actionList.any {
            it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
    }

    /**
     * Check if node can scroll backward
     */
    private fun canScrollBackward(node: AccessibilityNodeInfo): Boolean {
        return node.actionList.any {
            it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
    }

    /**
     * Check if any scrollable area needs more scrolling
     */
    fun hasMoreContent(scrollables: List<ScrollableArea>): Boolean {
        return scrollables.any { it.canScrollForward }
    }
}

/**
 * Scrollable Area
 */
data class ScrollableArea(
    val node: AccessibilityNodeInfo,
    val bounds: Rect,
    val direction: ScrollDirection,
    val canScrollForward: Boolean,
    val canScrollBackward: Boolean
)

/**
 * Scroll Direction
 */
enum class ScrollDirection {
    VERTICAL,
    HORIZONTAL,
    BOTH
}
