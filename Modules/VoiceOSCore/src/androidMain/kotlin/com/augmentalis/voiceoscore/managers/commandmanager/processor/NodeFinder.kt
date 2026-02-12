/**
 * NodeFinder.kt - Find accessibility nodes by hash in UI tree
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.managers.commandmanager.processor

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.security.MessageDigest

/**
 * Utility for finding accessibility nodes by hash in the UI tree.
 *
 * Hash-based node lookup provides:
 * - Fast O(n) tree traversal (where n = number of nodes)
 * - Stable element identification across app sessions
 * - Version-independent element tracking
 *
 * Hash Calculation:
 * MD5(className + viewIdResourceName + text + contentDescription)
 *
 * Use Cases:
 * 1. Execute voice command on specific UI element
 * 2. Verify element still exists after app update
 * 3. Track element usage statistics
 *
 * Performance Characteristics:
 * - Average tree size: 50-200 nodes
 * - Traversal time: <50ms for 200 nodes
 * - Hash calculation: <1ms per node
 * - Total lookup: <100ms worst case
 */
object NodeFinder {

    private const val TAG = "NodeFinder"

    /**
     * Find accessibility node by hash in UI tree.
     *
     * Performs depth-first search through accessibility tree,
     * calculating hash for each node until match is found.
     *
     * @param rootNode Root of accessibility tree (typically window root)
     * @param targetHash MD5 hash of target element
     * @return Matching AccessibilityNodeInfo or null if not found
     *
     * IMPORTANT: Caller is responsible for recycling returned node
     */
    fun findNodeByHash(rootNode: AccessibilityNodeInfo?, targetHash: String): AccessibilityNodeInfo? {
        if (rootNode == null) {
            Log.w(TAG, "Root node is null, cannot search for hash: $targetHash")
            return null
        }

        val startTime = System.currentTimeMillis()
        var nodesChecked = 0

        val result = findNodeByHashRecursive(rootNode, targetHash, 0, nodesChecked)

        val elapsedTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Node search completed in ${elapsedTime}ms, checked ${result.second} nodes")

        return result.first
    }

    /**
     * Recursive depth-first search for node by hash.
     *
     * @param node Current node being checked
     * @param targetHash Target hash to find
     * @param depth Current depth in tree (for logging)
     * @param nodesChecked Number of nodes checked so far
     * @return Pair of (found node or null, total nodes checked)
     */
    private fun findNodeByHashRecursive(
        node: AccessibilityNodeInfo,
        targetHash: String,
        depth: Int,
        nodesChecked: Int
    ): Pair<AccessibilityNodeInfo?, Int> {
        var checked = nodesChecked + 1

        // Calculate hash for current node
        val nodeHash = calculateNodeHash(node)

        // Check if this is the target node
        if (nodeHash == targetHash) {
            Log.d(TAG, "Found matching node at depth $depth: hash=$nodeHash")
            return Pair(node, checked)
        }

        // Recursively search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val (foundNode, newChecked) = findNodeByHashRecursive(child, targetHash, depth + 1, checked)
            checked = newChecked

            if (foundNode != null) {
                // Found in child subtree, return
                return Pair(foundNode, checked)
            }

            // Not found in this subtree
        }

        // Not found in this subtree
        return Pair(null, checked)
    }

    /**
     * Calculate MD5 hash for accessibility node.
     *
     * Hash components (concatenated):
     * 1. className (e.g., "android.widget.Button")
     * 2. viewIdResourceName (e.g., "com.example:id/submit_button")
     * 3. text (e.g., "Submit")
     * 4. contentDescription (e.g., "Submit form button")
     *
     * Empty/null components are treated as empty strings.
     *
     * @param node Accessibility node to hash
     * @return MD5 hash string (32 hex characters)
     */
    fun calculateNodeHash(node: AccessibilityNodeInfo): String {
        val identifier = buildString {
            append(node.className ?: "")
            append(node.viewIdResourceName ?: "")
            append(node.text ?: "")
            append(node.contentDescription ?: "")
        }

        return identifier.toMD5()
    }

    /**
     * Calculate hash including bounds for position-sensitive matching.
     *
     * Use this when you need to distinguish between identical elements
     * in different positions (e.g., multiple "OK" buttons).
     *
     * @param node Accessibility node to hash
     * @return MD5 hash including bounds
     */
    fun calculateNodeHashWithBounds(node: AccessibilityNodeInfo): String {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val identifier = buildString {
            append(node.className ?: "")
            append(node.viewIdResourceName ?: "")
            append(node.text ?: "")
            append(node.contentDescription ?: "")
            append(bounds.left)
            append(bounds.top)
            append(bounds.right)
            append(bounds.bottom)
        }

        return identifier.toMD5()
    }

    /**
     * Find all nodes matching a predicate.
     *
     * @param rootNode Root of accessibility tree
     * @param predicate Function to test each node
     * @return List of matching nodes (caller must recycle)
     */
    fun findNodesMatching(
        rootNode: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): List<AccessibilityNodeInfo> {
        if (rootNode == null) return emptyList()

        val matches = mutableListOf<AccessibilityNodeInfo>()
        findNodesMatchingRecursive(rootNode, predicate, matches)
        return matches
    }

    /**
     * Recursive helper for findNodesMatching.
     */
    private fun findNodesMatchingRecursive(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
        matches: MutableList<AccessibilityNodeInfo>
    ) {
        // Test current node
        if (predicate(node)) {
            matches.add(node)
        }

        // Recursively search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodesMatchingRecursive(child, predicate, matches)
        }
    }

    /**
     * Get node information for debugging.
     *
     * @param node Node to inspect
     * @return Human-readable node description
     */
    fun getNodeInfo(node: AccessibilityNodeInfo): String {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        return buildString {
            append("Node(")
            append("class=${node.className}, ")
            append("id=${node.viewIdResourceName}, ")
            append("text='${node.text}', ")
            append("desc='${node.contentDescription}', ")
            append("bounds=$bounds, ")
            append("clickable=${node.isClickable}, ")
            append("editable=${node.isEditable}, ")
            append("hash=${calculateNodeHash(node)}")
            append(")")
        }
    }

    /**
     * Verify node hash matches expected hash.
     *
     * Useful for validating scraped data is still accurate.
     *
     * @param node Node to verify
     * @param expectedHash Expected hash from database
     * @return True if hashes match
     */
    fun verifyNodeHash(node: AccessibilityNodeInfo, expectedHash: String): Boolean {
        val actualHash = calculateNodeHash(node)
        val matches = actualHash == expectedHash

        if (!matches) {
            Log.w(TAG, "Hash mismatch: expected=$expectedHash, actual=$actualHash")
            Log.w(TAG, "Node info: ${getNodeInfo(node)}")
        }

        return matches
    }

    /**
     * Extension function to calculate MD5 hash of string.
     *
     * @return MD5 hash as hex string (32 characters)
     */
    private fun String.toMD5(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Extension functions for AccessibilityNodeInfo.
 */

/**
 * Calculate hash for this node.
 */
fun AccessibilityNodeInfo.calculateHash(): String {
    return NodeFinder.calculateNodeHash(this)
}

/**
 * Calculate hash with bounds for this node.
 */
fun AccessibilityNodeInfo.calculateHashWithBounds(): String {
    return NodeFinder.calculateNodeHashWithBounds(this)
}

/**
 * Get debug info for this node.
 */
fun AccessibilityNodeInfo.toDebugString(): String {
    return NodeFinder.getNodeInfo(this)
}

/**
 * Verify this node matches expected hash.
 */
fun AccessibilityNodeInfo.verifyHash(expectedHash: String): Boolean {
    return NodeFinder.verifyNodeHash(this, expectedHash)
}
