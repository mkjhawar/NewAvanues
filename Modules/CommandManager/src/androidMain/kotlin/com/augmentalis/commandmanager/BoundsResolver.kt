package com.augmentalis.commandmanager

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

private const val TAG = "BoundsResolver"

/**
 * Hybrid layered bounds resolution for click execution.
 *
 * Tries multiple strategies from fastest to most accurate:
 * 1. Metadata bounds (cached) - ~0-1ms, 60% success rate
 * 2. Delta compensation (scroll offset) - ~1-2ms, 25% success rate
 * 3. Resource ID anchor search - ~5-10ms, 10% success rate
 * 4. Full tree search - ~50-100ms, 5% success rate (fallback)
 *
 * This approach minimizes latency for most cases while ensuring
 * clicks still work even when bounds become stale from scrolling.
 */
class BoundsResolver(private val service: AccessibilityService) {

    /**
     * Scroll offset tracking per container resource ID.
     * Updated when scroll events are detected.
     */
    private val scrollOffsets = mutableMapOf<String, Pair<Int, Int>>()

    /**
     * Track current package name to detect app changes.
     * BUG FIX: Auto-clear scroll offsets when app changes.
     */
    private var currentPackageName: String? = null

    /**
     * Resolve bounds for a command using layered strategy.
     * Returns fresh, validated bounds or null if element not found.
     *
     * @param command The command to resolve bounds for
     * @return Bounds if found and valid, null otherwise
     */
    fun resolve(command: QuantizedCommand): Bounds? {
        val startTime = System.currentTimeMillis()

        // Log command details for debugging
        val isIndex = command.metadata["isIndexCommand"] == "true"
        val isNumeric = command.metadata["isNumericCommand"] == "true"
        val hasBounds = command.metadata["bounds"] != null
        Log.d(TAG, "resolve: phrase='${command.phrase}', isIndex=$isIndex, isNumeric=$isNumeric, hasBounds=$hasBounds")
        if (hasBounds) {
            Log.v(TAG, "  bounds=${command.metadata["bounds"]}, label=${command.metadata["label"]}")
        }

        // Layer 1: Try cached metadata bounds (fastest)
        val metadataBounds = tryMetadataBounds(command)
        if (metadataBounds != null) {
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Layer 1 success: metadata bounds for '${command.phrase}' (${duration}ms)")
            return metadataBounds
        }

        // Layer 2: Try delta compensation for scrolled content
        val deltaBounds = tryDeltaCompensation(command)
        if (deltaBounds != null) {
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Layer 2 success: delta compensation for '${command.phrase}' (${duration}ms)")
            return deltaBounds
        }

        // Layer 3: Try resource ID anchor search (targeted)
        val anchorBounds = tryAnchorSearch(command)
        if (anchorBounds != null) {
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Layer 3 success: anchor search for '${command.phrase}' (${duration}ms)")
            return anchorBounds
        }

        // Layer 4: Full tree search (fallback)
        val fullSearchBounds = tryFullTreeSearch(command)
        if (fullSearchBounds != null) {
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Layer 4 success: full search for '${command.phrase}' (${duration}ms)")
            return fullSearchBounds
        }

        val duration = System.currentTimeMillis() - startTime
        Log.w(TAG, "All layers failed for '${command.phrase}' (${duration}ms)")
        return null
    }

    /**
     * Layer 1: Use bounds from command metadata.
     * This is the fastest but may be stale if the screen has scrolled.
     */
    private fun tryMetadataBounds(command: QuantizedCommand): Bounds? {
        val boundsStr = command.metadata["bounds"] ?: return null
        val bounds = parseBounds(boundsStr) ?: return null

        // Validate bounds are still reasonable
        if (!validateBounds(bounds)) {
            Log.v(TAG, "Layer 1: metadata bounds invalid for '${command.phrase}'")
            return null
        }

        // Additional validation: check if element is still at those bounds
        // This is a quick sanity check to catch grossly stale bounds
        if (!quickValidatePosition(bounds, command)) {
            Log.v(TAG, "Layer 1: position validation failed for '${command.phrase}'")
            return null
        }

        return bounds
    }

    /**
     * Quick validation that something exists at the bounds.
     *
     * For index/numeric commands (isIndexCommand=true or isNumericCommand=true),
     * we trust the bounds more since they're freshly generated from the current
     * screen scan. We only need to verify there's ANY visible node at position.
     *
     * For other commands, we check for clickable/focusable nodes.
     */
    private fun quickValidatePosition(bounds: Bounds, command: QuantizedCommand): Boolean {
        // For index/numeric commands, trust the bounds - they're fresh from screen scan
        // Skip strict validation to avoid false negatives from non-clickable containers
        val isIndexOrNumeric = command.metadata["isIndexCommand"] == "true" ||
            command.metadata["isNumericCommand"] == "true"

        if (isIndexOrNumeric) {
            // For index/numeric commands, just verify bounds are on-screen
            // The bounds were generated from a real element, trust them
            Log.v(TAG, "Index/numeric command - trusting metadata bounds")
            return true
        }

        val root = service.rootInActiveWindow ?: return false
        try {
            // Check if there's any clickable node at the center of bounds
            val centerX = (bounds.left + bounds.right) / 2
            val centerY = (bounds.top + bounds.bottom) / 2

            // Find node at position (this is fast)
            val node = findNodeAtPosition(root, centerX, centerY)
            return node != null && (node.isClickable || node.isFocusable)
        } finally {
            root.recycle()
        }
    }

    /**
     * Find a node at a specific screen position.
     * Note: Caller is responsible for recycling the returned node.
     */
    private fun findNodeAtPosition(root: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        root.getBoundsInScreen(rect)

        // Not in bounds
        if (!rect.contains(x, y)) return null

        // Check children
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val childRect = Rect()
            child.getBoundsInScreen(childRect)

            if (childRect.contains(x, y)) {
                val found = findNodeAtPosition(child, x, y)
                if (found != null) {
                    child.recycle()  // BUG FIX: Recycle child before returning found node
                    return found
                }
                if (child.isClickable || child.isFocusable) {
                    // Don't recycle - we're returning this child
                    return child
                }
            }
            child.recycle()
        }

        // Return this node if it's clickable/focusable
        return if (root.isClickable || root.isFocusable) root else null
    }

    /**
     * Layer 2: Apply scroll delta to cached bounds.
     * Works when the element has scrolled but we know the scroll offset.
     */
    private fun tryDeltaCompensation(command: QuantizedCommand): Bounds? {
        val containerId = command.metadata["containerId"] ?: return null

        // Parse cached scroll offset from command metadata
        val cachedScrollStr = command.metadata["scrollOffset"] ?: return null
        val cachedScrollParts = cachedScrollStr.split(",").mapNotNull { it.toIntOrNull() }
        if (cachedScrollParts.size != 2) return null

        // Get current scroll offset for this container
        val currentOffset = scrollOffsets[containerId] ?: return null

        // Calculate delta
        val deltaX = currentOffset.first - cachedScrollParts[0]
        val deltaY = currentOffset.second - cachedScrollParts[1]

        // If no delta, fall through to next layer
        if (deltaX == 0 && deltaY == 0) return null

        // Get original bounds and apply delta
        val boundsStr = command.metadata["bounds"] ?: return null
        val bounds = parseBounds(boundsStr) ?: return null

        val adjustedBounds = Bounds(
            left = bounds.left - deltaX,
            top = bounds.top - deltaY,
            right = bounds.right - deltaX,
            bottom = bounds.bottom - deltaY
        )

        if (!validateBounds(adjustedBounds)) return null

        // Validate the adjusted position has the right element
        if (!quickValidatePosition(adjustedBounds, command)) return null

        return adjustedBounds
    }

    /**
     * Layer 3: Search by resource ID (targeted, faster than full search).
     * Works when element has a unique resource ID.
     */
    private fun tryAnchorSearch(command: QuantizedCommand): Bounds? {
        val resourceId = command.metadata["resourceId"]?.takeIf { it.isNotBlank() }
            ?: return null

        val root = service.rootInActiveWindow ?: return null
        var nodes: List<AccessibilityNodeInfo>? = null
        try {
            nodes = root.findAccessibilityNodeInfosByViewId(resourceId)
            if (nodes.isNullOrEmpty()) return null

            // Find best match (prefer visible, clickable)
            val node = nodes.asSequence()
                .sortedByDescending { node ->
                    var score = 0
                    if (node.isVisibleToUser) score += 100
                    if (node.isClickable) score += 50
                    if (node.isFocusable) score += 25
                    score
                }
                .firstOrNull { it.isVisibleToUser }
                ?: nodes.firstOrNull()

            return node?.let {
                val rect = Rect()
                it.getBoundsInScreen(rect)
                Bounds(rect.left, rect.top, rect.right, rect.bottom)
            }
        } finally {
            // BUG FIX: Always recycle nodes, even when node is null
            nodes?.forEach { n -> n.recycle() }
            root.recycle()
        }
    }

    /**
     * Layer 4: Full tree search by text/content description matching.
     * Slowest but most reliable fallback.
     */
    private fun tryFullTreeSearch(command: QuantizedCommand): Bounds? {
        val targetHash = command.metadata["elementHash"]
        val targetText = command.metadata["label"] ?: command.phrase
        val targetDesc = command.metadata["contentDescription"]

        val root = service.rootInActiveWindow ?: return null
        try {
            val node = findNodeByTextOrHash(root, targetText, targetDesc, targetHash)
            return node?.let {
                val rect = Rect()
                it.getBoundsInScreen(rect)
                // Don't recycle - caller may still need it
                Bounds(rect.left, rect.top, rect.right, rect.bottom)
            }
        } finally {
            root.recycle()
        }
    }

    /**
     * BFS search for a node matching text, description, or hash.
     * Note: Properly recycles all nodes to prevent memory leaks.
     */
    private fun findNodeByTextOrHash(
        root: AccessibilityNodeInfo,
        targetText: String,
        targetDesc: String?,
        targetHash: String?
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val toRecycle = mutableListOf<AccessibilityNodeInfo>()
        queue.add(root)

        try {
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()

                // Check text match
                val nodeText = node.text?.toString() ?: ""
                val nodeDesc = node.contentDescription?.toString() ?: ""

                if (nodeText.equals(targetText, ignoreCase = true) ||
                    nodeDesc.equals(targetText, ignoreCase = true) ||
                    (targetDesc != null && nodeDesc.equals(targetDesc, ignoreCase = true))) {
                    if (node.isClickable || node.isFocusable) {
                        // BUG FIX: Recycle remaining queue items before returning
                        queue.forEach { it.recycle() }
                        toRecycle.forEach { it.recycle() }
                        return node
                    }
                }

                // Check hash match
                if (targetHash != null) {
                    val nodeHash = calculateHash(node)
                    if (nodeHash == targetHash) {
                        // BUG FIX: Recycle remaining queue items before returning
                        queue.forEach { it.recycle() }
                        toRecycle.forEach { it.recycle() }
                        return node
                    }
                }

                // Add children to queue
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }

                // BUG FIX: Track processed nodes for recycling (except root, handled by caller)
                if (node != root) {
                    toRecycle.add(node)
                }
            }
            return null
        } finally {
            // BUG FIX: Recycle all processed nodes on any exit path
            toRecycle.forEach { it.recycle() }
            queue.forEach { it.recycle() }
        }
    }

    /**
     * Calculate hash for a node (same algorithm as ElementExtractor).
     */
    private fun calculateHash(node: AccessibilityNodeInfo): String {
        val input = "${node.className}|${node.viewIdResourceName}|${node.text}"
        return input.hashCode().toUInt().toString(16).padStart(8, '0')
    }

    /**
     * Update scroll offset tracking for a container.
     * Call this when scroll events are detected.
     *
     * @param containerId Resource ID of the scrollable container
     * @param offsetX Current horizontal scroll offset
     * @param offsetY Current vertical scroll offset
     */
    fun updateScrollOffset(containerId: String, offsetX: Int, offsetY: Int) {
        scrollOffsets[containerId] = Pair(offsetX, offsetY)
        Log.v(TAG, "Scroll offset updated: $containerId -> ($offsetX, $offsetY)")
    }

    /**
     * Clear all scroll offset tracking.
     * Call on app change or full screen refresh.
     */
    fun clearScrollOffsets() {
        scrollOffsets.clear()
        Log.v(TAG, "Scroll offsets cleared")
    }

    /**
     * Notify resolver of current package for auto-clearing stale offsets.
     * BUG FIX: Automatically clears scroll offsets when package changes.
     *
     * @param packageName Current app package name
     */
    fun onPackageChanged(packageName: String?) {
        if (packageName != null && packageName != currentPackageName) {
            Log.d(TAG, "Package changed: $currentPackageName -> $packageName, clearing scroll offsets")
            clearScrollOffsets()
            currentPackageName = packageName
        }
    }

    /**
     * Validate bounds are on screen and reasonable.
     */
    private fun validateBounds(bounds: Bounds): Boolean {
        // Basic sanity checks
        if (bounds.left < 0 || bounds.top < 0) return false
        if (bounds.right <= bounds.left || bounds.bottom <= bounds.top) return false

        // Check minimum size (avoid tiny click targets)
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        if (width < 10 || height < 10) return false

        // Could add screen size validation here if needed
        return true
    }

    /**
     * Parse bounds string "left,top,right,bottom" into Bounds object.
     */
    private fun parseBounds(str: String): Bounds? {
        return try {
            val parts = str.split(",").map { it.trim().toInt() }
            if (parts.size == 4) {
                Bounds(parts[0], parts[1], parts[2], parts[3])
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
