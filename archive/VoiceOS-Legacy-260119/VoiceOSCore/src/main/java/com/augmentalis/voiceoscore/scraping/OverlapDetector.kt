/**
 * OverlapDetector.kt - Detects and resolves overlapping UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-23
 *
 * PROBLEM SOLVED:
 * When multiple elements occupy the same screen coordinates (overlapping),
 * users may interact with the wrong element. This detector uses z-order
 * analysis to identify which element is actually visible on top.
 *
 * CRITICAL FIX:
 * Analysis identified crashes and incorrect actions when users try to interact
 * with elements that are visually obscured by overlays, dialogs, or other elements.
 *
 * DISAMBIGUATION STRATEGY:
 * 1. Detect overlapping bounds using Rect.intersect()
 * 2. Analyze z-order (element depth in tree, drawing order)
 * 3. Check visibility flags (isVisibleToUser)
 * 4. Prefer topmost, fully-visible element
 *
 * See: VoiceOS-Analysis-CommandGeneration-EdgeCases-251223-V1.md (Edge Case 2)
 * See: VoiceOS-Plan-CommandGeneration-Fixes-251223-V1.md (Cluster 2.2)
 */
package com.augmentalis.voiceoscore.scraping

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects and resolves overlapping UI elements based on z-order.
 *
 * **Usage:**
 * ```kotlin
 * val detector = OverlapDetector()
 * val nodes = listOf(node1, node2, node3)
 * val topmost = detector.getTopmostElement(nodes)
 * if (topmost != null) {
 *     topmost.performAction(AccessibilityNodeInfo.ACTION_CLICK)
 * }
 * ```
 */
class OverlapDetector {

    companion object {
        private const val TAG = "OverlapDetector"

        /**
         * Metrics for overlap detection.
         */
        data class OverlapMetrics(
            var overlappingPairsDetected: Int = 0,
            var zOrderResolutions: Int = 0,
            var totalComparisons: Int = 0
        )

        private val metrics = OverlapMetrics()

        /**
         * Get current overlap detection metrics.
         */
        fun getMetrics(): OverlapMetrics = metrics.copy()

        /**
         * Reset metrics (for testing).
         */
        fun resetMetrics() {
            metrics.overlappingPairsDetected = 0
            metrics.zOrderResolutions = 0
            metrics.totalComparisons = 0
        }
    }

    /**
     * Detect if two elements have overlapping bounds.
     *
     * @param node1 First element
     * @param node2 Second element
     * @return true if bounds overlap, false otherwise
     */
    fun hasOverlap(node1: AccessibilityNodeInfo, node2: AccessibilityNodeInfo): Boolean {
        metrics.totalComparisons++

        val bounds1 = Rect()
        val bounds2 = Rect()
        node1.getBoundsInScreen(bounds1)
        node2.getBoundsInScreen(bounds2)

        val overlaps = Rect.intersects(bounds1, bounds2)
        if (overlaps) {
            metrics.overlappingPairsDetected++
            Log.d(TAG, "Overlap detected: ${node1.className} vs ${node2.className}")
        }

        return overlaps
    }

    /**
     * Get topmost element from a list of overlapping elements.
     *
     * Uses z-order heuristics:
     * 1. Element with highest drawingOrder (if available)
     * 2. Element with shallowest depth in tree (closer to root)
     * 3. Element that is visibleToUser
     *
     * @param nodes List of potentially overlapping elements
     * @return Topmost (visible) element, or null if none valid
     */
    fun getTopmostElement(nodes: List<AccessibilityNodeInfo>): AccessibilityNodeInfo? {
        if (nodes.isEmpty()) return null
        if (nodes.size == 1) return nodes[0]

        Log.d(TAG, "Resolving topmost element from ${nodes.size} candidates")

        // Filter to only visible elements first
        val visibleNodes = nodes.filter { it.isVisibleToUser }
        if (visibleNodes.isEmpty()) {
            Log.w(TAG, "No visible nodes in candidate set")
            return null
        }

        if (visibleNodes.size == 1) {
            Log.d(TAG, "Single visible node found")
            return visibleNodes[0]
        }

        // Strategy 1: Use drawingOrder if available (API 24+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val maxDrawingOrder = visibleNodes.maxByOrNull { it.drawingOrder }
            if (maxDrawingOrder != null) {
                metrics.zOrderResolutions++
                Log.d(TAG, "Resolved by drawingOrder: ${maxDrawingOrder.className}")
                return maxDrawingOrder
            }
        }

        // Strategy 2: Use tree depth (shallower = on top)
        val minDepth = visibleNodes.minByOrNull { getNodeDepth(it) }
        if (minDepth != null) {
            metrics.zOrderResolutions++
            Log.d(TAG, "Resolved by depth: ${minDepth.className}")
            return minDepth
        }

        // Fallback: Return first visible element
        Log.w(TAG, "Using fallback: first visible element")
        return visibleNodes[0]
    }

    /**
     * Get topmost element at specific coordinates.
     *
     * Useful for point-based disambiguation when user tries to interact
     * with elements at specific screen coordinates.
     *
     * @param nodes List of elements to check
     * @param x X coordinate on screen
     * @param y Y coordinate on screen
     * @return Topmost element at coordinates, or null if none found
     */
    fun getTopmostElementAtPoint(nodes: List<AccessibilityNodeInfo>, x: Int, y: Int): AccessibilityNodeInfo? {
        // Filter nodes that contain the point
        val containingNodes = nodes.filter { node ->
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            bounds.contains(x, y)
        }

        if (containingNodes.isEmpty()) {
            Log.d(TAG, "No elements found at point ($x, $y)")
            return null
        }

        Log.d(TAG, "Found ${containingNodes.size} elements at point ($x, $y)")
        return getTopmostElement(containingNodes)
    }

    /**
     * Calculate node depth in accessibility tree.
     *
     * Depth = distance from root node.
     * Root node has depth 0, its children have depth 1, etc.
     *
     * @param node Node to calculate depth for
     * @return Depth in tree (0 = root)
     */
    private fun getNodeDepth(node: AccessibilityNodeInfo): Int {
        var depth = 0
        var current = node.parent

        while (current != null) {
            depth++
            val next = current.parent
            current.recycle()
            current = next
        }

        return depth
    }

    /**
     * Find all overlapping elements in a list.
     *
     * Returns pairs of elements that have overlapping bounds.
     *
     * @param nodes List of elements to analyze
     * @return List of overlapping pairs (each pair is Pair<node1, node2>)
     */
    fun findOverlappingPairs(nodes: List<AccessibilityNodeInfo>): List<Pair<AccessibilityNodeInfo, AccessibilityNodeInfo>> {
        val overlappingPairs = mutableListOf<Pair<AccessibilityNodeInfo, AccessibilityNodeInfo>>()

        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                if (hasOverlap(nodes[i], nodes[j])) {
                    overlappingPairs.add(Pair(nodes[i], nodes[j]))
                }
            }
        }

        Log.d(TAG, "Found ${overlappingPairs.size} overlapping pairs in ${nodes.size} nodes")
        return overlappingPairs
    }

    /**
     * Disambiguate element when multiple elements share similar properties.
     *
     * Used when command matching returns multiple candidates due to
     * similar text, content description, or UI properties.
     *
     * @param candidates List of candidate elements
     * @param preferenceHint Optional hint for disambiguation (e.g., "topmost", "clickable")
     * @return Best candidate element, or null if none valid
     */
    fun disambiguate(candidates: List<AccessibilityNodeInfo>, preferenceHint: String? = null): AccessibilityNodeInfo? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates[0]

        Log.d(TAG, "Disambiguating ${candidates.size} candidates with hint: $preferenceHint")

        return when (preferenceHint) {
            "topmost" -> getTopmostElement(candidates)
            "clickable" -> candidates.firstOrNull { it.isClickable }
            "enabled" -> candidates.firstOrNull { it.isEnabled }
            else -> getTopmostElement(candidates) // Default: topmost
        }
    }

    /**
     * Check if element is fully obscured by another element.
     *
     * An element is fully obscured if another element completely covers
     * its bounds AND the covering element is on top (higher z-order).
     *
     * @param element Element to check
     * @param allNodes All nodes in current screen
     * @return true if element is fully obscured, false otherwise
     */
    fun isFullyObscured(element: AccessibilityNodeInfo, allNodes: List<AccessibilityNodeInfo>): Boolean {
        val elementBounds = Rect()
        element.getBoundsInScreen(elementBounds)

        for (other in allNodes) {
            if (other == element) continue

            val otherBounds = Rect()
            other.getBoundsInScreen(otherBounds)

            // Check if 'other' fully contains 'element'
            if (otherBounds.contains(elementBounds.left, elementBounds.top) &&
                otherBounds.contains(elementBounds.right, elementBounds.bottom)) {

                // Check if 'other' is on top (visible and higher z-order)
                if (other.isVisibleToUser && isHigherZOrder(other, element)) {
                    Log.d(TAG, "Element ${element.className} is fully obscured by ${other.className}")
                    return true
                }
            }
        }

        return false
    }

    /**
     * Compare z-order of two elements.
     *
     * @param element1 First element
     * @param element2 Second element
     * @return true if element1 has higher z-order than element2
     */
    private fun isHigherZOrder(element1: AccessibilityNodeInfo, element2: AccessibilityNodeInfo): Boolean {
        // Use drawingOrder if available (API 24+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return element1.drawingOrder > element2.drawingOrder
        }

        // Fallback: Use tree depth (shallower = higher z-order)
        return getNodeDepth(element1) < getNodeDepth(element2)
    }

    /**
     * Log overlap detection metrics.
     */
    fun logMetrics() {
        Log.i(
            TAG,
            "Overlap Metrics: Comparisons=${metrics.totalComparisons}, " +
                    "Overlaps=${metrics.overlappingPairsDetected}, " +
                    "Resolutions=${metrics.zOrderResolutions}"
        )
    }
}
