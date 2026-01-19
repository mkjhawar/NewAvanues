/**
 * SafeNodeTraverser.kt - Stack-Safe Tree Traversal with Cycle Detection
 *
 * YOLO Phase 1 - Critical Issue #5: Infinite Recursion Risk - SOLUTION
 *
 * Problem Solved:
 * - Eliminates stack overflow from circular node references
 * - Prevents infinite loops in malformed accessibility trees
 * - Enforces maximum depth limit
 * - Tracks all visited nodes to detect cycles
 *
 * Features:
 * - Iterative traversal (no recursion - stack-safe)
 * - Cycle detection using visited node tracking
 * - Configurable maximum depth limit
 * - Generic - works with any tree structure
 * - Early termination support
 * - Zero stack overflow risk
 *
 * Usage:
 * ```kotlin
 * // Before (DANGEROUS - can stack overflow):
 * fun findNodeByHash(node: AccessibilityNodeInfo, hash: String): AccessibilityNodeInfo? {
 *     for (i in 0 until node.childCount) {
 *         val child = node.getChild(i) ?: continue
 *         val found = findNodeByHash(child, hash)  // RECURSIVE - can overflow!
 *         if (found != null) return found
 *     }
 *     return null
 * }
 *
 * // After (SAFE - iterative with cycle detection):
 * SafeNodeTraverser<AccessibilityNodeInfo>().traverse(
 *     root = rootNode,
 *     getChildren = { node -> (0 until node.childCount).mapNotNull { node.getChild(it) } },
 *     onVisit = { node, depth ->
 *         if (computeHash(node) == hash) {
 *             // Found it - can safely return
 *             return@traverse false  // Stop traversal
 *         }
 *         true  // Continue
 *     }
 * )
 * ```
 *
 * @property maxDepth Maximum depth to traverse (default 50)
 */
package com.augmentalis.voiceoscore.lifecycle

import android.util.Log
import com.augmentalis.voiceos.constants.VoiceOSConstants
import java.util.*

/**
 * SafeNodeTraverser - Iterative tree traversal with cycle detection
 *
 * Uses explicit stack data structure instead of call stack to prevent stack overflow.
 * Tracks visited nodes by identity to detect and prevent infinite loops.
 *
 * @param T Node type (must support reference equality)
 */
class SafeNodeTraverser<T : Any>(
    private val maxDepth: Int = VoiceOSConstants.TreeTraversal.MAX_DEPTH
) {

    companion object {
        private const val TAG = "SafeNodeTraverser"

        /**
         * Traverse a tree structure safely with default settings
         *
         * Convenience function for one-time traversal.
         */
        fun <T : Any> traverseSafe(
            root: T,
            maxDepth: Int = VoiceOSConstants.TreeTraversal.MAX_DEPTH,
            getChildren: (T) -> List<T>,
            onVisit: (node: T, depth: Int) -> Boolean = { _, _ -> true }
        ) {
            SafeNodeTraverser<T>(maxDepth).traverse(root, getChildren, onVisit)
        }
    }

    /**
     * Data class to track node and its depth in traversal stack
     */
    private data class NodeWithDepth<T>(
        val node: T,
        val depth: Int
    )

    /**
     * Traverse a tree structure iteratively with cycle detection
     *
     * Uses depth-first traversal with explicit stack (no recursion).
     * Detects cycles by tracking visited nodes using identity (===).
     *
     * @param root Root node to start traversal
     * @param getChildren Function to get children of a node
     * @param onVisit Callback invoked for each node. Return false to stop traversal early.
     *
     * Example:
     * ```kotlin
     * traverser.traverse(
     *     root = rootNode,
     *     getChildren = { it.children },
     *     onVisit = { node, depth ->
     *         println("Visiting ${node.id} at depth $depth")
     *         true  // Continue traversal
     *     }
     * )
     * ```
     */
    fun traverse(
        root: T,
        getChildren: (T) -> List<T>,
        onVisit: (node: T, depth: Int) -> Boolean = { _, _ -> true }
    ) {
        // Use LinkedHashSet to track visited nodes by identity
        // Maintains insertion order for consistent traversal
        val visited = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())

        // Explicit stack for iterative traversal (stack-safe)
        val stack = ArrayDeque<NodeWithDepth<T>>()

        // Start with root node at depth 0
        stack.push(NodeWithDepth(root, 0))

        var nodesVisited = 0
        val startTime = System.currentTimeMillis()

        while (stack.isNotEmpty()) {
            val (node, depth) = stack.pop()

            // Check if already visited (cycle detection)
            if (visited.contains(node)) {
                Log.v(TAG, "Cycle detected - node already visited")
                continue
            }

            // Check depth limit
            if (depth > maxDepth) {
                Log.v(TAG, "Max depth $maxDepth reached, skipping deeper nodes")
                continue
            }

            // Mark as visited
            visited.add(node)
            nodesVisited++

            // Visit the node
            val shouldContinue = try {
                onVisit(node, depth)
            } catch (e: Exception) {
                Log.w(TAG, "Exception in onVisit callback", e)
                // Continue traversal even if callback throws
                true
            }

            if (!shouldContinue) {
                // Early termination requested
                Log.d(TAG, "Traversal terminated early by callback")
                break
            }

            // Add children to stack (in reverse order for depth-first left-to-right)
            try {
                val children = getChildren(node)
                // Reverse to maintain left-to-right order (stack is LIFO)
                for (i in children.size - 1 downTo 0) {
                    val child = children[i]
                    // Only add if not already visited
                    if (!visited.contains(child)) {
                        stack.push(NodeWithDepth(child, depth + 1))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception getting children for node", e)
                // Continue with next node in stack
            }
        }

        val duration = System.currentTimeMillis() - startTime
        if (duration > VoiceOSConstants.TreeTraversal.TRAVERSAL_SLOW_THRESHOLD_MS) {
            Log.d(TAG, "Traversal completed: $nodesVisited nodes in ${duration}ms")
        }
    }

    /**
     * Find first node matching predicate
     *
     * Traverses tree until predicate returns true, then returns that node.
     * Returns null if no node matches.
     *
     * @param root Root node to start search
     * @param getChildren Function to get children
     * @param predicate Function to test each node
     * @return First matching node or null
     */
    fun findFirst(
        root: T,
        getChildren: (T) -> List<T>,
        predicate: (T) -> Boolean
    ): T? {
        var result: T? = null

        traverse(root, getChildren) { node, _ ->
            if (predicate(node)) {
                result = node
                false  // Stop traversal
            } else {
                true  // Continue
            }
        }

        return result
    }

    /**
     * Find all nodes matching predicate
     *
     * Traverses entire tree and collects all matching nodes.
     *
     * @param root Root node to start search
     * @param getChildren Function to get children
     * @param predicate Function to test each node
     * @return List of all matching nodes
     */
    fun findAll(
        root: T,
        getChildren: (T) -> List<T>,
        predicate: (T) -> Boolean
    ): List<T> {
        val results = mutableListOf<T>()

        traverse(root, getChildren) { node, _ ->
            if (predicate(node)) {
                results.add(node)
            }
            true  // Always continue
        }

        return results
    }

    /**
     * Count nodes in tree
     *
     * Safely counts all nodes respecting max depth and cycle detection.
     *
     * @param root Root node
     * @param getChildren Function to get children
     * @return Total node count
     */
    fun count(
        root: T,
        getChildren: (T) -> List<T>
    ): Int {
        var count = 0
        traverse(root, getChildren) { _, _ ->
            count++
            true
        }
        return count
    }

    /**
     * Get maximum depth of tree
     *
     * Returns the deepest depth reached during traversal.
     *
     * @param root Root node
     * @param getChildren Function to get children
     * @return Maximum depth (root = 0)
     */
    fun getMaxDepth(
        root: T,
        getChildren: (T) -> List<T>
    ): Int {
        var maxDepth = 0
        traverse(root, getChildren) { _, depth ->
            if (depth > maxDepth) {
                maxDepth = depth
            }
            true
        }
        return maxDepth
    }
}

/**
 * Extension function for convenient one-time traversal
 *
 * Usage:
 * ```kotlin
 * rootNode.traverseSafe(
 *     maxDepth = 50,
 *     getChildren = { it.children }
 * ) { node, depth ->
 *     println("Visiting $node at depth $depth")
 *     true
 * }
 * ```
 */
fun <T : Any> T.traverseSafe(
    maxDepth: Int = VoiceOSConstants.TreeTraversal.MAX_DEPTH,
    getChildren: (T) -> List<T>,
    onVisit: (node: T, depth: Int) -> Boolean = { _, _ -> true }
) {
    SafeNodeTraverser<T>(maxDepth).traverse(this, getChildren, onVisit)
}

/**
 * Extension function to find first matching node
 *
 * Usage:
 * ```kotlin
 * val found = rootNode.findFirstSafe(
 *     getChildren = { it.children }
 * ) { node ->
 *     node.id == targetId
 * }
 * ```
 */
fun <T : Any> T.findFirstSafe(
    maxDepth: Int = VoiceOSConstants.TreeTraversal.MAX_DEPTH,
    getChildren: (T) -> List<T>,
    predicate: (T) -> Boolean
): T? {
    return SafeNodeTraverser<T>(maxDepth).findFirst(this, getChildren, predicate)
}
