/**
 * ScrollDetector.kt - Detects scrollable containers
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/scrolling/ScrollDetector.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Detects scrollable containers in accessibility node tree
 */

package com.augmentalis.learnapp.scrolling

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Scroll Detector
 *
 * Detects scrollable containers (vertical and horizontal) in UI tree.
 * Used to find offscreen elements by scrolling.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val detector = ScrollDetector()
 *
 * val rootNode = getRootInActiveWindow()
 * val scrollables = detector.findScrollableContainers(rootNode)
 *
 * scrollables.forEach { scrollable ->
 *     val direction = detector.detectScrollDirection(scrollable)
 *     println("Found ${direction} scrollable")
 * }
 * ```
 *
 * @since 1.0.0
 */
class ScrollDetector {

    /**
     * Find all scrollable containers in tree
     *
     * Traverses node tree and collects all scrollable nodes.
     *
     * @param rootNode Root node to search
     * @return List of scrollable nodes
     */
    fun findScrollableContainers(rootNode: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (rootNode == null) return emptyList()

        val scrollables = mutableListOf<AccessibilityNodeInfo>()

        traverseTree(rootNode) { node ->
            if (node.isScrollable) {
                scrollables.add(node)
            }
        }

        return scrollables
    }

    /**
     * Detect scroll direction of container
     *
     * @param scrollable Scrollable node
     * @return Scroll direction
     */
    fun detectScrollDirection(scrollable: AccessibilityNodeInfo): ScrollDirection {
        val className = scrollable.className?.toString() ?: ""

        // Check for explicit horizontal scrollables
        if (isHorizontalScrollView(className)) {
            return ScrollDirection.HORIZONTAL
        }

        // Check for explicit vertical scrollables
        if (isVerticalScrollView(className)) {
            return ScrollDirection.VERTICAL
        }

        // Check RecyclerView orientation
        if (className.contains("RecyclerView", ignoreCase = true)) {
            return if (isHorizontalLayout(scrollable)) {
                ScrollDirection.HORIZONTAL
            } else {
                ScrollDirection.VERTICAL
            }
        }

        // Default to vertical
        return ScrollDirection.VERTICAL
    }

    /**
     * Check if container is horizontal scroll view
     *
     * @param className Class name
     * @return true if horizontal scroll view
     */
    private fun isHorizontalScrollView(className: String): Boolean {
        return className.contains("HorizontalScrollView", ignoreCase = true) ||
               className.contains("ViewPager", ignoreCase = true) ||
               className.contains("Carousel", ignoreCase = true)
    }

    /**
     * Check if container is vertical scroll view
     *
     * @param className Class name
     * @return true if vertical scroll view
     */
    private fun isVerticalScrollView(className: String): Boolean {
        return className.contains("ScrollView", ignoreCase = true) &&
               !className.contains("HorizontalScrollView", ignoreCase = true)
    }

    /**
     * Check if RecyclerView has horizontal layout
     *
     * @param node RecyclerView node
     * @return true if horizontal layout
     */
    private fun isHorizontalLayout(node: AccessibilityNodeInfo): Boolean {
        // Check if children are arranged horizontally
        // by comparing their X coordinates
        if (node.childCount < 2) {
            return false
        }

        val child1 = node.getChild(0) ?: return false
        val child2 = node.getChild(1) ?: return false

        try {
            val bounds1 = android.graphics.Rect()
            val bounds2 = android.graphics.Rect()

            child1.getBoundsInScreen(bounds1)
            child2.getBoundsInScreen(bounds2)

            // If second child is to the right of first, it's horizontal
            return bounds2.left > bounds1.right
        } finally {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                child1.recycle()
                @Suppress("DEPRECATION")
                child2.recycle()
            }
        }
    }

    /**
     * Traverse tree (DFS)
     *
     * @param node Current node
     * @param visitor Visitor function
     */
    private fun traverseTree(
        node: AccessibilityNodeInfo,
        visitor: (AccessibilityNodeInfo) -> Unit
    ) {
        visitor(node)

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                try {
                    traverseTree(child, visitor)
                } finally {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                }
            }
        }
    }

    /**
     * Check if node can scroll forward
     *
     * @param node Node to check
     * @return true if can scroll forward
     */
    fun canScrollForward(node: AccessibilityNodeInfo): Boolean {
        return node.actionList.any { action ->
            action.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ||
            action.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id ||
            action.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id
        }
    }

    /**
     * Check if node can scroll backward
     *
     * @param node Node to check
     * @return true if can scroll backward
     */
    fun canScrollBackward(node: AccessibilityNodeInfo): Boolean {
        return node.actionList.any { action ->
            action.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD ||
            action.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id ||
            action.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id
        }
    }
}

/**
 * Scroll Direction
 *
 * Direction of scrollable container.
 */
enum class ScrollDirection {
    VERTICAL,
    HORIZONTAL,
    BOTH
}
