/**
 * AndroidScreenExtractor.kt - Extracts UI elements from AccessibilityNodeInfo tree
 *
 * Traverses the accessibility node tree and converts nodes to ElementInfo
 * for use with CommandGenerator and ActionCoordinator.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 */
package com.augmentalis.voiceoscore

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Screen extractor that converts Android AccessibilityNodeInfo tree to List<ElementInfo>.
 *
 * Features:
 * - Depth-limited traversal (prevents infinite recursion)
 * - Visibility filtering
 * - Dynamic container detection
 * - Proper node recycling
 * - Memory-efficient traversal
 */
class AndroidScreenExtractor {

    companion object {
        private const val TAG = "AndroidScreenExtractor"
        private const val MAX_DEPTH = 30
        private const val MIN_ELEMENT_SIZE = 10
    }

    /**
     * Extract all UI elements from the accessibility tree.
     *
     * @param rootNode Root node from AccessibilityService.rootInActiveWindow
     * @return List of ElementInfo representing all actionable/visible elements
     */
    fun extract(rootNode: AccessibilityNodeInfo?): List<ElementInfo> {
        if (rootNode == null) {
            Log.d(TAG, "Root node is null, returning empty list")
            return emptyList()
        }

        val elements = mutableListOf<ElementInfo>()
        val startTime = System.currentTimeMillis()

        try {
            traverseNode(
                node = rootNode,
                elements = elements,
                depth = 0,
                listIndex = -1,
                isInDynamicContainer = false,
                containerType = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during traversal: ${e.message}", e)
        }

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Extracted ${elements.size} elements in ${elapsed}ms")

        return elements
    }

    /**
     * Recursively traverse the accessibility tree.
     *
     * @param node Current node being processed
     * @param elements Accumulator list for results
     * @param depth Current depth in tree
     * @param listIndex Current list index (-1 if not in list)
     * @param isInDynamicContainer Whether we're inside a dynamic container
     * @param containerType Type of container we're in
     */
    private fun traverseNode(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>,
        depth: Int,
        listIndex: Int,
        isInDynamicContainer: Boolean,
        containerType: String
    ) {
        // Depth limit to prevent stack overflow
        if (depth > MAX_DEPTH) {
            Log.w(TAG, "Max depth reached at depth $depth")
            return
        }

        // Visibility check
        if (!node.isVisibleToUser) {
            return
        }

        // Size check - filter tiny/invisible elements
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() < MIN_ELEMENT_SIZE || rect.height() < MIN_ELEMENT_SIZE) {
            // Still traverse children - parent might be small but children visible
            traverseChildren(node, elements, depth, listIndex, isInDynamicContainer, containerType)
            return
        }

        // Check if this node is a dynamic container
        val className = node.className?.toString() ?: ""
        val nodeIsDynamicContainer = AccessibilityNodeAdapter.isDynamicContainer(className)
        val effectiveInDynamic = isInDynamicContainer || nodeIsDynamicContainer
        val effectiveContainerType = if (nodeIsDynamicContainer) className else containerType

        // Convert to ElementInfo
        val elementInfo = AccessibilityNodeAdapter.toElementInfo(
            node = node,
            listIndex = listIndex,
            isInDynamicContainer = effectiveInDynamic,
            containerType = effectiveContainerType
        )

        // Add to list if it has useful content for voice commands
        if (shouldIncludeElement(elementInfo)) {
            elements.add(elementInfo)
        }

        // Traverse children
        traverseChildren(node, elements, depth, listIndex, effectiveInDynamic, effectiveContainerType)
    }

    /**
     * Traverse child nodes with proper index tracking for lists.
     */
    private fun traverseChildren(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>,
        depth: Int,
        currentListIndex: Int,
        isInDynamicContainer: Boolean,
        containerType: String
    ) {
        val childCount = node.childCount

        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (e: Exception) {
                Log.w(TAG, "Error getting child $i: ${e.message}")
                null
            }

            if (child != null) {
                try {
                    // Update list index if we're in a list-like container
                    val childListIndex = if (isInDynamicContainer && currentListIndex == -1) {
                        i  // First level inside dynamic container gets index
                    } else {
                        currentListIndex
                    }

                    traverseNode(
                        node = child,
                        elements = elements,
                        depth = depth + 1,
                        listIndex = childListIndex,
                        isInDynamicContainer = isInDynamicContainer,
                        containerType = containerType
                    )
                } finally {
                    // Recycle child node to prevent memory leaks
                    recycleNode(child)
                }
            }
        }
    }

    /**
     * Determine if an element should be included in the result.
     *
     * Criteria:
     * - Has voice content (text, contentDescription, or resourceId)
     * - Is actionable (clickable or scrollable) OR
     * - Is in a position where parent clickability applies
     */
    private fun shouldIncludeElement(element: ElementInfo): Boolean {
        // Must have some way to identify via voice
        if (!element.hasVoiceContent) {
            return false
        }

        // Include if actionable
        if (element.isActionable) {
            return true
        }

        // Include enabled elements with text (might have inherited clickability)
        if (element.isEnabled && element.text.isNotBlank()) {
            return true
        }

        return false
    }

    /**
     * Safely recycle an AccessibilityNodeInfo.
     * Note: recycle() is deprecated in API 34+ but required for earlier versions.
     */
    @Suppress("DEPRECATION")
    private fun recycleNode(node: AccessibilityNodeInfo) {
        try {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                node.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error recycling node: ${e.message}")
        }
    }
}
