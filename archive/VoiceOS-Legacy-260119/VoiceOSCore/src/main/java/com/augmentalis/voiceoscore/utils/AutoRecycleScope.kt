/**
 * AutoRecycleScope.kt - Automatic AccessibilityNodeInfo recycling utility
 *
 * SECURITY FIX: Prevents memory leaks from AccessibilityNodeInfo objects
 * that are not properly recycled.
 *
 * AccessibilityNodeInfo objects MUST be recycled after use to prevent
 * memory leaks. This utility provides a safe, automatic way to ensure
 * recycling happens even when exceptions occur.
 *
 * Usage:
 * ```kotlin
 * withAutoRecycle(node) { safeNode ->
 *     // Use safeNode - will be recycled automatically
 *     safeNode.text
 * }
 *
 * // Or for multiple nodes:
 * withAutoRecycleAll(listOf(node1, node2)) { nodes ->
 *     nodes.forEach { it.performAction(ACTION_CLICK) }
 * }
 * ```
 *
 * @since VOS 4.1
 * @author VOS Development Team
 */

package com.augmentalis.voiceoscore.utils

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.io.Closeable

@PublishedApi
internal const val TAG = "AutoRecycleScope"

/**
 * Execute block with automatic AccessibilityNodeInfo recycling
 *
 * MEMORY FIX: Ensures node is recycled even if an exception occurs.
 *
 * @param node The AccessibilityNodeInfo to manage
 * @param block The code block to execute with the node
 * @return The result of the block, or null if node is null
 */
inline fun <R> withAutoRecycle(
    node: AccessibilityNodeInfo?,
    block: (AccessibilityNodeInfo) -> R
): R? {
    if (node == null) return null

    return try {
        block(node)
    } finally {
        try {
            node.recycle()
        } catch (e: IllegalStateException) {
            // Node was already recycled - this is OK
            Log.d(TAG, "Node already recycled")
        } catch (e: Exception) {
            Log.e(TAG, "Error recycling node", e)
        }
    }
}

/**
 * Execute block with automatic recycling of multiple AccessibilityNodeInfo objects
 *
 * MEMORY FIX: Ensures all nodes are recycled even if an exception occurs.
 *
 * @param nodes List of AccessibilityNodeInfo objects to manage
 * @param block The code block to execute with the nodes
 * @return The result of the block
 */
inline fun <R> withAutoRecycleAll(
    nodes: List<AccessibilityNodeInfo?>,
    block: (List<AccessibilityNodeInfo>) -> R
): R {
    val validNodes = nodes.filterNotNull()

    return try {
        block(validNodes)
    } finally {
        validNodes.forEach { node ->
            try {
                node.recycle()
            } catch (e: IllegalStateException) {
                // Node was already recycled - this is OK
            } catch (e: Exception) {
                Log.e(TAG, "Error recycling node", e)
            }
        }
    }
}

/**
 * Wrapper for AccessibilityNodeInfo that implements Closeable for use with use()
 *
 * Usage:
 * ```kotlin
 * RecyclableNode(node).use { wrapper ->
 *     wrapper.node.text
 * }
 * ```
 */
class RecyclableNode(val node: AccessibilityNodeInfo?) : Closeable {
    override fun close() {
        try {
            node?.recycle()
        } catch (e: IllegalStateException) {
            // Node was already recycled
        } catch (e: Exception) {
            Log.e(TAG, "Error recycling node in RecyclableNode", e)
        }
    }

    /**
     * Check if the wrapped node is valid
     */
    fun isValid(): Boolean = node != null
}

/**
 * Extension function to wrap AccessibilityNodeInfo for automatic recycling
 *
 * Usage:
 * ```kotlin
 * node.autoRecycle().use { wrapper ->
 *     wrapper.node?.text
 * }
 * ```
 */
fun AccessibilityNodeInfo?.autoRecycle(): RecyclableNode = RecyclableNode(this)

/**
 * Pool manager for AccessibilityNodeInfo recycling
 *
 * Tracks nodes and ensures they are all recycled at scope exit.
 * Useful for complex operations that create many nodes.
 *
 * Usage:
 * ```kotlin
 * NodeRecyclePool().use { pool ->
 *     val node1 = pool.track(getRootNode())
 *     val node2 = pool.track(node1?.getChild(0))
 *     // Both nodes recycled automatically at end
 * }
 * ```
 */
class NodeRecyclePool : Closeable {
    private val nodes = mutableListOf<AccessibilityNodeInfo>()
    private var isClosed = false

    /**
     * Track a node for automatic recycling
     *
     * @param node The node to track (can be null)
     * @return The same node for chaining
     */
    fun track(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (isClosed) {
            Log.w(TAG, "Attempted to track node on closed pool")
            node?.recycle()
            return null
        }
        node?.let { nodes.add(it) }
        return node
    }

    /**
     * Track multiple nodes for automatic recycling
     *
     * @param nodeList List of nodes to track
     * @return List of valid (non-null) nodes
     */
    fun trackAll(nodeList: List<AccessibilityNodeInfo?>): List<AccessibilityNodeInfo> {
        if (isClosed) {
            Log.w(TAG, "Attempted to track nodes on closed pool")
            nodeList.forEach { it?.recycle() }
            return emptyList()
        }
        val validNodes = nodeList.filterNotNull()
        nodes.addAll(validNodes)
        return validNodes
    }

    /**
     * Get count of tracked nodes
     */
    fun size(): Int = nodes.size

    /**
     * Recycle all tracked nodes
     */
    override fun close() {
        if (isClosed) return
        isClosed = true

        var recycleCount = 0
        var errorCount = 0

        nodes.forEach { node ->
            try {
                node.recycle()
                recycleCount++
            } catch (e: IllegalStateException) {
                // Already recycled - OK
            } catch (e: Exception) {
                errorCount++
                Log.e(TAG, "Error recycling node in pool", e)
            }
        }
        nodes.clear()

        if (recycleCount > 0 || errorCount > 0) {
            Log.d(TAG, "NodeRecyclePool closed: recycled=$recycleCount, errors=$errorCount")
        }
    }
}

/**
 * Execute block with a NodeRecyclePool
 *
 * Usage:
 * ```kotlin
 * withNodePool { pool ->
 *     val root = pool.track(getRootNode())
 *     root?.let { traverseTree(it, pool) }
 * }
 * ```
 */
inline fun <R> withNodePool(block: (NodeRecyclePool) -> R): R {
    return NodeRecyclePool().use(block)
}

/**
 * Safe tree traversal with automatic recycling
 *
 * Traverses accessibility tree and ensures all nodes are recycled.
 *
 * @param root The root node to start traversal from
 * @param visitor Function called for each node, return true to continue to children
 */
inline fun traverseTreeSafely(
    root: AccessibilityNodeInfo?,
    crossinline visitor: (AccessibilityNodeInfo) -> Boolean
) {
    if (root == null) return

    withNodePool { pool ->
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        pool.track(root)?.let { queue.add(it) }

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            val visitChildren = try {
                visitor(node)
            } catch (e: Exception) {
                Log.e(TAG, "Error in tree visitor", e)
                false
            }

            if (visitChildren) {
                for (i in 0 until node.childCount) {
                    pool.track(node.getChild(i))?.let { queue.add(it) }
                }
            }
        }
    }
}
