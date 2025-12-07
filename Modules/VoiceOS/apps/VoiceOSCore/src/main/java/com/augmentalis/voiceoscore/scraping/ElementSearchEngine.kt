/**
 * ElementSearchEngine.kt - Tier 3 real-time element search by properties
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI-Assisted Implementation
 * Created: 2025-12-01
 *
 * Searches the live accessibility tree for elements by multiple properties.
 * This is Tier 3 of the voice command pipeline - used when Tier 2 (database lookup) fails.
 *
 * Search Priority Order:
 * 1. ViewId resource name (95% reliability, fastest)
 * 2. Bounds + Text combination (85% reliability)
 * 3. Class + ContentDescription (70% reliability)
 * 4. Text only (60% reliability, fallback)
 *
 * Performance Target: <20ms
 *
 * Part of Voice Command Element Persistence feature (Phase 3)
 */
package com.augmentalis.voiceoscore.scraping

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.utils.forEachChild
import com.augmentalis.voiceoscore.utils.useNodeOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Tier 3 Real-Time Element Search Engine
 *
 * Searches the live accessibility tree without database dependency.
 * Uses multiple search strategies in priority order for maximum reliability.
 *
 * ## Architecture:
 * ```
 * Voice Command → Tier 1 (static) → Tier 2 (database) → Tier 3 (this) → Execute
 * ```
 *
 * ## Usage:
 * ```kotlin
 * val engine = ElementSearchEngine(accessibilityService)
 * val criteria = ElementSearchCriteria(viewIdResourceName = "com.app:id/submit")
 * val node = engine.findElement(criteria)
 * node?.performAction(ACTION_CLICK)
 * ```
 *
 * @param accessibilityService The accessibility service for tree access
 */
class ElementSearchEngine(
    private val accessibilityService: AccessibilityService
) {
    companion object {
        private const val TAG = "ElementSearchEngine"
        private const val SEARCH_TIMEOUT_MS = 20L
        private const val MAX_SEARCH_DEPTH = 15

        // Bounds tolerance for position-based matching (pixels)
        private const val BOUNDS_TOLERANCE = 10
    }

    /**
     * Find element by search criteria
     *
     * Tries search strategies in priority order:
     * 1. ViewId (most reliable)
     * 2. Bounds + Text (position-based)
     * 3. Class + Description (type-based)
     * 4. Text only (fallback)
     *
     * @param criteria Search criteria with element properties
     * @return Matching AccessibilityNodeInfo or null. CALLER MUST RECYCLE.
     */
    suspend fun findElement(criteria: ElementSearchCriteria): AccessibilityNodeInfo? {
        if (criteria.isEmpty()) {
            Log.w(TAG, "Empty search criteria provided")
            return null
        }

        val startTime = System.currentTimeMillis()

        return withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
            withContext(Dispatchers.Main) {
                val root = accessibilityService.rootInActiveWindow
                if (root == null) {
                    Log.w(TAG, "No root node available for search")
                    return@withContext null
                }

                try {
                    // Priority 1: ViewId (most reliable, fastest)
                    criteria.viewIdResourceName?.takeIf { criteria.hasViewId() }?.let { viewId ->
                        findByViewId(root, viewId)?.let { node ->
                            val elapsed = System.currentTimeMillis() - startTime
                            Log.d(TAG, "Found by viewId in ${elapsed}ms: $viewId")
                            return@withContext node
                        }
                    }

                    // Priority 2: Bounds + Text combination
                    if (criteria.hasBoundsAndText()) {
                        val bounds = criteria.bounds
                        val text = criteria.text
                        if (bounds != null && text != null) {
                            findByBoundsAndText(root, bounds, text)?.let { node ->
                                val elapsed = System.currentTimeMillis() - startTime
                                Log.d(TAG, "Found by bounds+text in ${elapsed}ms")
                                return@withContext node
                            }
                        }
                    }

                    // Priority 3: Class + ContentDescription
                    if (criteria.hasClassAndDescription()) {
                        val className = criteria.className
                        val contentDesc = criteria.contentDescription
                        if (className != null && contentDesc != null) {
                            findByClassAndDesc(root, className, contentDesc)?.let { node ->
                                val elapsed = System.currentTimeMillis() - startTime
                                Log.d(TAG, "Found by class+desc in ${elapsed}ms")
                                return@withContext node
                            }
                        }
                    }

                    // Priority 4: Text only (fallback)
                    criteria.text?.takeIf { criteria.hasTextOnly() }?.let { text ->
                        findByText(root, text)?.let { node ->
                            val elapsed = System.currentTimeMillis() - startTime
                            Log.d(TAG, "Found by text in ${elapsed}ms: $text")
                            return@withContext node
                        }
                    }

                    val elapsed = System.currentTimeMillis() - startTime
                    Log.w(TAG, "Element not found after ${elapsed}ms, criteria: $criteria")
                    null
                } finally {
                    root.recycle()
                }
            }
        } ?: run {
            val elapsed = System.currentTimeMillis() - startTime
            Log.w(TAG, "Search timed out after ${elapsed}ms")
            null
        }
    }

    /**
     * Find element by viewIdResourceName
     *
     * Uses Android's built-in findAccessibilityNodeInfosByViewId for fast lookup.
     * This is the most reliable search method as viewIds are stable.
     *
     * @param root Root node to search from
     * @param viewId Full viewId (e.g., "com.example:id/submit_button")
     * @return First matching node or null. CALLER MUST RECYCLE.
     */
    private fun findByViewId(root: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        try {
            // Use Android's built-in viewId search (very fast)
            val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
            if (!nodes.isNullOrEmpty()) {
                // Return first match, recycle the rest
                val result = nodes[0]
                nodes.drop(1).forEach { it.recycle() }
                return result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching by viewId: $viewId", e)
        }
        return null
    }

    /**
     * Find element by bounds and text combination
     *
     * Matches elements that are at approximately the same screen position
     * AND have matching text. Good for apps with dynamic viewIds.
     *
     * @param root Root node to search from
     * @param targetBounds Expected screen bounds
     * @param text Expected text content
     * @return Matching node or null. CALLER MUST RECYCLE.
     */
    private fun findByBoundsAndText(
        root: AccessibilityNodeInfo,
        targetBounds: Rect,
        text: String
    ): AccessibilityNodeInfo? {
        return searchRecursive(root, 0) { node ->
            val nodeBounds = Rect()
            node.getBoundsInScreen(nodeBounds)

            val boundsMatch = boundsApproximatelyEqual(nodeBounds, targetBounds)
            val textMatch = node.text?.toString()?.contains(text, ignoreCase = true) == true

            boundsMatch && textMatch
        }
    }

    /**
     * Find element by className and contentDescription
     *
     * Matches elements by type and accessibility label.
     * Useful for icon buttons and images without visible text.
     *
     * @param root Root node to search from
     * @param className Expected class (e.g., "android.widget.ImageButton")
     * @param description Expected content description
     * @return Matching node or null. CALLER MUST RECYCLE.
     */
    private fun findByClassAndDesc(
        root: AccessibilityNodeInfo,
        className: String,
        description: String
    ): AccessibilityNodeInfo? {
        return searchRecursive(root, 0) { node ->
            val classMatch = node.className?.toString()?.contains(className, ignoreCase = true) == true
            val descMatch = node.contentDescription?.toString()?.contains(description, ignoreCase = true) == true

            classMatch && descMatch
        }
    }

    /**
     * Find element by text content
     *
     * Fallback search that looks for text matches. Less reliable than
     * viewId but works on any app.
     *
     * @param root Root node to search from
     * @param text Text to search for (case-insensitive)
     * @return First actionable matching node or null. CALLER MUST RECYCLE.
     */
    private fun findByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        // Try Android's built-in text search first (fast)
        try {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (!nodes.isNullOrEmpty()) {
                // Find first actionable match
                for (node in nodes) {
                    if (node.isClickable || node.isFocusable || node.isEditable) {
                        // Recycle non-selected nodes
                        nodes.filter { it != node }.forEach { it.recycle() }
                        return node
                    }
                }
                // If no actionable found, return first match anyway
                val result = nodes[0]
                nodes.drop(1).forEach { it.recycle() }
                return result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in text search: $text", e)
        }

        // Fallback to recursive search for partial matches
        return searchRecursive(root, 0) { node ->
            val nodeText = node.text?.toString() ?: ""
            val nodeDesc = node.contentDescription?.toString() ?: ""

            (nodeText.contains(text, ignoreCase = true) ||
             nodeDesc.contains(text, ignoreCase = true)) &&
            (node.isClickable || node.isFocusable)
        }
    }

    /**
     * Recursive search with predicate
     *
     * Traverses the tree looking for first node matching predicate.
     * Properly recycles non-matching nodes.
     *
     * @param node Current node being checked
     * @param depth Current depth (for limiting)
     * @param predicate Function to test each node
     * @return Matching node or null. CALLER MUST RECYCLE.
     */
    private fun searchRecursive(
        node: AccessibilityNodeInfo,
        depth: Int,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        // Depth limit
        if (depth > MAX_SEARCH_DEPTH) {
            return null
        }

        // Check current node
        if (predicate(node)) {
            // Don't recycle - caller needs this node
            // Note: We can't return a copy, must return original
            // This is a limitation of AccessibilityNodeInfo
            return node
        }

        // Search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                val result = searchRecursive(child, depth + 1, predicate)
                if (result != null) {
                    // Found a match in subtree - don't recycle child if it's the result
                    if (result != child) {
                        child.recycle()
                    }
                    return result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching child at depth $depth", e)
            } finally {
                // Only recycle if not the result
                // This is handled above for matches
            }
            // Recycle non-matching child
            child.recycle()
        }

        return null
    }

    /**
     * Check if two bounds are approximately equal
     *
     * Allows for small differences due to animations or measurement timing.
     */
    private fun boundsApproximatelyEqual(a: Rect, b: Rect): Boolean {
        return kotlin.math.abs(a.left - b.left) <= BOUNDS_TOLERANCE &&
               kotlin.math.abs(a.top - b.top) <= BOUNDS_TOLERANCE &&
               kotlin.math.abs(a.right - b.right) <= BOUNDS_TOLERANCE &&
               kotlin.math.abs(a.bottom - b.bottom) <= BOUNDS_TOLERANCE
    }

    /**
     * Find all elements matching criteria
     *
     * For cases where multiple matches are acceptable (e.g., list items).
     *
     * @param criteria Search criteria
     * @param maxResults Maximum number of results to return
     * @return List of matching nodes. CALLER MUST RECYCLE ALL.
     */
    suspend fun findAllElements(
        criteria: ElementSearchCriteria,
        maxResults: Int = 10
    ): List<AccessibilityNodeInfo> {
        if (criteria.isEmpty()) return emptyList()

        return withTimeoutOrNull(SEARCH_TIMEOUT_MS * 2) {  // Double timeout for multi-search
            withContext(Dispatchers.Main) {
                val results = mutableListOf<AccessibilityNodeInfo>()
                val root = accessibilityService.rootInActiveWindow ?: return@withContext emptyList()

                try {
                    // Use text search for finding multiple matches
                    if (criteria.text != null) {
                        val nodes = root.findAccessibilityNodeInfosByText(criteria.text)
                        if (!nodes.isNullOrEmpty()) {
                            results.addAll(nodes.take(maxResults))
                            nodes.drop(maxResults).forEach { it.recycle() }
                        }
                    }

                    results
                } finally {
                    root.recycle()
                }
            }
        } ?: emptyList()
    }
}
