/**
 * NodeCacheManager.android.kt - Android implementation of accessibility node caching
 *
 * Provides LRU cache for AVID to AccessibilityNodeInfo mapping.
 * Android-specific implementation handling native accessibility nodes.
 *
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: JITLearning/handlers/NodeCacheManager.kt
 *
 * @since 3.0.0 (KMP Migration)
 */

package com.augmentalis.voiceoscoreng.jit

import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Android Node Cache Manager
 *
 * LRU cache for accessibility nodes with automatic eviction.
 *
 * Features:
 * - LRU eviction (max 100 nodes by default)
 * - Automatic node recycling (pre-Android 14)
 * - Hierarchy traversal and caching
 * - AVID generation from node properties
 * - Dual storage: NodeReference for cross-platform + native AccessibilityNodeInfo
 *
 * Thread Safety: Not thread-safe, caller must synchronize
 */
class AndroidNodeCacheManager(
    private val maxCacheSize: Int = 100
) : INodeCache {

    companion object {
        private const val TAG = "AndroidNodeCacheManager"
    }

    // LRU cache: AVID -> NodeReference (cross-platform)
    private val referenceCache = object : LinkedHashMap<String, NodeReference>(
        maxCacheSize + 1,
        0.75f,
        true // Access order for LRU
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, NodeReference>?): Boolean {
            val shouldRemove = size > maxCacheSize
            if (shouldRemove && eldest != null) {
                // Also remove from native cache
                nativeCache.remove(eldest.key)?.let { node ->
                    recycleNode(node)
                }
            }
            return shouldRemove
        }
    }

    // Native cache: AVID -> AccessibilityNodeInfo (Android-specific)
    private val nativeCache = mutableMapOf<String, AccessibilityNodeInfo>()

    // ===== INodeCache Implementation =====

    override fun getNodeReference(avid: String): NodeReference? {
        return referenceCache[avid]
    }

    override fun cacheNodeReference(avid: String, node: NodeReference) {
        referenceCache[avid] = node
    }

    override fun invalidateCache() {
        // Recycle all native nodes
        for ((_, node) in nativeCache) {
            recycleNode(node)
        }
        nativeCache.clear()
        referenceCache.clear()
        Log.d(TAG, "Cache invalidated")
    }

    override fun getCacheSize(): Int = referenceCache.size

    override fun clear() {
        invalidateCache()
    }

    override fun isCached(avid: String): Boolean {
        return referenceCache.containsKey(avid)
    }

    override fun getAllCachedAvids(): Set<String> {
        return referenceCache.keys.toSet()
    }

    override fun findNodes(predicate: (NodeReference) -> Boolean): List<NodeReference> {
        return referenceCache.values.filter(predicate)
    }

    override fun findByClassName(className: String): List<NodeReference> {
        return referenceCache.values.filter { node ->
            node.className == className || node.className.endsWith(".$className")
        }
    }

    override fun findByText(text: String): List<NodeReference> {
        return referenceCache.values.filter { node ->
            node.text?.contains(text, ignoreCase = true) == true ||
            node.contentDescription?.contains(text, ignoreCase = true) == true
        }
    }

    override fun findClickableNodes(): List<NodeReference> {
        return referenceCache.values.filter { it.isClickable }
    }

    // ===== Android-Specific Methods =====

    /**
     * Get native AccessibilityNodeInfo by AVID.
     *
     * @param avid Element AVID
     * @return Cached native node or null if not found
     */
    fun getNativeNode(avid: String): AccessibilityNodeInfo? {
        return nativeCache[avid]
    }

    /**
     * Cache native AccessibilityNodeInfo with AVID.
     *
     * @param avid Element AVID
     * @param node Native accessibility node to cache
     */
    fun cacheNativeNode(avid: String, node: AccessibilityNodeInfo) {
        // Create cross-platform reference
        val reference = createNodeReference(avid, node)
        referenceCache[avid] = reference
        nativeCache[avid] = node
    }

    /**
     * Build cache from root node hierarchy.
     *
     * Traverses hierarchy and caches all nodes with generated AVIDs.
     *
     * @param root Root accessibility node
     */
    fun buildCache(root: AccessibilityNodeInfo) {
        invalidateCache()
        traverseAndCache(root)
        Log.d(TAG, "Cache built with ${referenceCache.size} nodes")
    }

    /**
     * Find native node by AVID in hierarchy.
     *
     * Searches tree for node matching AVID. Uses cache first, then searches.
     *
     * @param root Root node to search from
     * @param avid Target AVID
     * @return Found native node or null
     */
    fun findNativeNodeByAvid(root: AccessibilityNodeInfo, avid: String): AccessibilityNodeInfo? {
        // Check cache first
        nativeCache[avid]?.let { return it }

        // Search tree
        return searchTreeForAvid(root, avid)
    }

    /**
     * Generate AVID for node.
     *
     * Creates deterministic AVID from node properties.
     *
     * @param node Node to generate AVID for
     * @return Generated AVID
     */
    fun generateNodeAvid(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        sb.append(node.className ?: "")
        sb.append(node.viewIdResourceName ?: "")
        sb.append(node.text ?: "")
        sb.append(node.contentDescription ?: "")
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        sb.append(bounds.toString())
        return sb.toString().hashCode().toString(16)
    }

    // ===== Private Helper Methods =====

    private fun traverseAndCache(node: AccessibilityNodeInfo) {
        val avid = generateNodeAvid(node)
        cacheNativeNode(avid, node)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseAndCache(child)
        }
    }

    private fun searchTreeForAvid(node: AccessibilityNodeInfo, targetAvid: String): AccessibilityNodeInfo? {
        val nodeAvid = generateNodeAvid(node)
        if (nodeAvid == targetAvid) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = searchTreeForAvid(child, targetAvid)
            if (found != null) return found

            recycleNode(child)
        }

        return null
    }

    private fun createNodeReference(avid: String, node: AccessibilityNodeInfo): NodeReference {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        return NodeReference(
            avid = avid,
            className = node.className?.toString() ?: "",
            resourceId = node.viewIdResourceName,
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            boundsLeft = bounds.left,
            boundsTop = bounds.top,
            boundsRight = bounds.right,
            boundsBottom = bounds.bottom,
            isClickable = node.isClickable,
            isFocusable = node.isFocusable,
            isEditable = node.isEditable,
            isScrollable = node.isScrollable
        )
    }

    private fun recycleNode(node: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            node.recycle()
        }
    }
}
