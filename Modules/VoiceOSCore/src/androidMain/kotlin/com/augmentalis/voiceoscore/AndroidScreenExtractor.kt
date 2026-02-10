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
     * @param containerResourceId Resource ID of parent scrollable container (NAV-500 Fix #2)
     */
    private fun traverseNode(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>,
        depth: Int,
        listIndex: Int,
        isInDynamicContainer: Boolean,
        containerType: String,
        containerResourceId: String = "",
        isParentClickable: Boolean = false
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
            traverseChildren(node, elements, depth, listIndex, isInDynamicContainer, containerType, containerResourceId, isParentClickable)
            return
        }

        // Check if this node is a dynamic container
        val className = node.className?.toString() ?: ""
        val nodeIsDynamicContainer = AccessibilityNodeAdapter.isDynamicContainer(className)
        val effectiveInDynamic = isInDynamicContainer || nodeIsDynamicContainer
        val effectiveContainerType = if (nodeIsDynamicContainer) className else containerType

        // NAV-500 Fix #2: Track container resource ID for scroll offset tracking
        val nodeResourceId = node.viewIdResourceName ?: ""
        val effectiveContainerResourceId = if (nodeIsDynamicContainer && nodeResourceId.isNotBlank()) {
            nodeResourceId  // Use this container's ID
        } else {
            containerResourceId  // Inherit parent's container ID
        }

        // Convert to ElementInfo
        val elementInfo = AccessibilityNodeAdapter.toElementInfo(
            node = node,
            listIndex = listIndex,
            isInDynamicContainer = effectiveInDynamic,
            containerType = effectiveContainerType,
            containerResourceId = effectiveContainerResourceId,
            isParentClickable = isParentClickable
        )

        // Add to list if it has useful content for voice commands
        if (shouldIncludeElement(elementInfo)) {
            elements.add(elementInfo)
        }

        // Traverse children — propagate clickability so text-only children
        // of clickable parents are treated as actionable for command generation
        val effectiveParentClickable = isParentClickable || node.isClickable
        traverseChildren(node, elements, depth, listIndex, effectiveInDynamic, effectiveContainerType, effectiveContainerResourceId, effectiveParentClickable)
    }

    /**
     * Traverse child nodes with proper index tracking for lists.
     *
     * BUG FIX: List indices are now only assigned to direct children of dynamic
     * containers, not nested elements. Nested children inherit parent's index.
     *
     * NAV-500 Fix #2: Added containerResourceId for scroll offset tracking.
     */
    private fun traverseChildren(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>,
        depth: Int,
        currentListIndex: Int,
        isInDynamicContainer: Boolean,
        containerType: String,
        containerResourceId: String = "",
        isParentClickable: Boolean = false
    ) {
        val childCount = node.childCount

        // Check if this node is a dynamic container - its direct children get indices
        val nodeClassName = node.className?.toString() ?: ""
        val isThisNodeDynamicContainer = AccessibilityNodeAdapter.isDynamicContainer(nodeClassName)

        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (e: Exception) {
                Log.w(TAG, "Error getting child $i: ${e.message}")
                null
            }

            if (child != null) {
                try {
                    // BUG FIX: Only assign list index to DIRECT children of dynamic containers
                    // Nested elements (grandchildren etc.) inherit the parent's index
                    val childListIndex = when {
                        // Direct child of a dynamic container AND no index yet -> assign new index
                        isThisNodeDynamicContainer && currentListIndex == -1 -> i
                        // Already have an index (nested element) -> keep it
                        currentListIndex >= 0 -> currentListIndex
                        // Not in a dynamic container -> no index
                        else -> -1
                    }

                    // NAV-500 Fix #2: Pass container resource ID for scroll tracking
                    val childContainerResourceId = if (isThisNodeDynamicContainer) {
                        node.viewIdResourceName ?: containerResourceId
                    } else {
                        containerResourceId
                    }

                    traverseNode(
                        node = child,
                        elements = elements,
                        depth = depth + 1,
                        listIndex = childListIndex,
                        isInDynamicContainer = isInDynamicContainer || isThisNodeDynamicContainer,
                        containerType = if (isThisNodeDynamicContainer) nodeClassName else containerType,
                        containerResourceId = childContainerResourceId,
                        isParentClickable = isParentClickable
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
