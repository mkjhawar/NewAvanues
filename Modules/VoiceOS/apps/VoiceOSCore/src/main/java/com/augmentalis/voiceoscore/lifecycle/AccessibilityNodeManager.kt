/**
 * AccessibilityNodeManager.kt - RAII Pattern for AccessibilityNodeInfo Lifecycle Management
 *
 * YOLO Phase 1 - Critical Issue #2: Missing Node Recycling in Error Paths
 *
 * Purpose:
 * Guarantees that ALL AccessibilityNodeInfo instances are properly recycled,
 * even in error paths, early returns, and exception scenarios.
 *
 * Pattern: Resource Acquisition Is Initialization (RAII) via Kotlin's AutoCloseable
 *
 * Usage:
 * ```kotlin
 * AccessibilityNodeManager().use { manager ->
 *     val child = manager.track(node.getChild(0))
 *     // Process child
 * } // Automatic recycling on scope exit
 * ```
 *
 * Safety Guarantees:
 * - All tracked nodes recycled on close()
 * - Exception-safe (try-finally semantics)
 * - Circular reference detection
 * - Depth limit enforcement
 * - Null-safe (handles null nodes gracefully)
 * - Idempotent close() (safe to call multiple times)
 *
 * @author YOLO TDD Implementation
 * @since Phase 1 - Week 1
 */
package com.augmentalis.voiceoscore.lifecycle

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Manages lifecycle of AccessibilityNodeInfo instances with automatic cleanup.
 *
 * Implements AutoCloseable to work with Kotlin's .use{} pattern, ensuring
 * all nodes are recycled even if exceptions occur.
 */
class AccessibilityNodeManager : AutoCloseable {

    companion object {
        private const val TAG = "AccessibilityNodeManager"
        private const val DEFAULT_MAX_DEPTH = 50
    }

    // Track all allocated nodes for cleanup
    private val nodes = mutableListOf<AccessibilityNodeInfo>()

    // Track if already closed (for idempotent close)
    private var closed = false

    /**
     * Track a node for automatic recycling.
     *
     * @param node The AccessibilityNodeInfo to track (null-safe)
     * @return The same node (for convenient chaining), or null if input was null
     */
    fun track(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node != null && !closed) {
            nodes.add(node)
        }
        return node
    }

    /**
     * Traverse accessibility tree with automatic node tracking and cleanup.
     *
     * Features:
     * - Automatic depth limiting (prevents stack overflow)
     * - Circular reference detection (prevents infinite loops)
     * - Null-safe (skips null children)
     * - All accessed nodes automatically recycled
     *
     * @param root The root node to start traversal
     * @param maxDepth Maximum depth to traverse (default: 50)
     * @param block Callback invoked for each node with (node, depth)
     */
    fun traverse(
        root: AccessibilityNodeInfo,
        maxDepth: Int = DEFAULT_MAX_DEPTH,
        block: (AccessibilityNodeInfo, Int) -> Unit
    ) {
        // Track visited nodes to detect cycles
        val visited = mutableSetOf<Int>()

        traverseInternal(root, 0, maxDepth, visited, block)
    }

    /**
     * Internal recursive traversal implementation.
     *
     * @param node Current node
     * @param depth Current depth
     * @param maxDepth Maximum allowed depth
     * @param visited Set of visited node IDs (for cycle detection)
     * @param block Callback for each node
     */
    private fun traverseInternal(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int,
        visited: MutableSet<Int>,
        block: (AccessibilityNodeInfo, Int) -> Unit
    ) {
        // Depth limit check
        if (depth > maxDepth) {
            return
        }

        // Circular reference detection
        val nodeId = System.identityHashCode(node)
        if (nodeId in visited) {
            return
        }
        visited.add(nodeId)

        // Process current node
        block(node, depth)

        // Traverse children
        for (i in 0 until node.childCount) {
            val child = track(node.getChild(i))
            if (child != null) {
                traverseInternal(child, depth + 1, maxDepth, visited, block)
            }
        }
    }

    /**
     * Release all tracked nodes.
     *
     * Guarantees:
     * - All nodes recycled exactly once
     * - Exceptions during recycle are logged but don't prevent other nodes from being recycled
     * - Idempotent (safe to call multiple times)
     */
    override fun close() {
        if (closed) {
            return  // Already closed, don't recycle again
        }

        closed = true

        // Recycle all tracked nodes
        nodes.forEach { node ->
            try {
                node.recycle()
            } catch (e: Exception) {
                // Log but don't crash - continue recycling other nodes
                Log.w(TAG, "Failed to recycle node: ${e.message}", e)
            }
        }

        // Clear list
        nodes.clear()
    }
}

/**
 * Extension function for convenient usage.
 *
 * Example:
 * ```kotlin
 * rootNode.use { root ->
 *     // Process root
 *     root.getChild(0).use { child ->
 *         // Process child
 *     } // child auto-recycled
 * } // root auto-recycled
 * ```
 */
inline fun <T> AccessibilityNodeInfo?.use(block: (AccessibilityNodeInfo) -> T): T? {
    if (this == null) return null

    return try {
        block(this)
    } finally {
        try {
            this.recycle()
        } catch (e: IllegalStateException) {
            // Already recycled - ignore
            Log.w("AccessibilityNodeInfo", "Node already recycled", e)
        }
    }
}
