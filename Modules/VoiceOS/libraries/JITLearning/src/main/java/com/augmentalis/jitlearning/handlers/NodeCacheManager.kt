/**
 * NodeCacheManager.kt - Implementation of accessibility node caching
 *
 * Provides LRU cache for UUID to AccessibilityNodeInfo mapping.
 * Extracted from JITLearningService as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 2.2.0 (SOLID Refactoring)
 */

package com.augmentalis.jitlearning.handlers

import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.LinkedHashMap

/**
 * Node Cache Manager
 *
 * LRU cache for accessibility nodes with automatic eviction.
 *
 * Features:
 * - LRU eviction (max 100 nodes)
 * - Automatic node recycling
 * - Hierarchy traversal and caching
 * - UUID generation from node properties
 *
 * Thread Safety: Not thread-safe, caller must synchronize
 */
class NodeCacheManager(
    private val maxCacheSize: Int = 100
) : INodeCache {

    companion object {
        private const val TAG = "NodeCacheManager"
    }

    // LRU cache: UUID -> AccessibilityNodeInfo
    private val cache = object : LinkedHashMap<String, AccessibilityNodeInfo>(
        maxCacheSize + 1,
        0.75f,
        true // Access order for LRU
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AccessibilityNodeInfo>?): Boolean {
            val shouldRemove = size > maxCacheSize
            if (shouldRemove && eldest != null) {
                // Recycle evicted node
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    eldest.value.recycle()
                }
            }
            return shouldRemove
        }
    }

    override fun getNode(uuid: String): AccessibilityNodeInfo? {
        return cache[uuid]
    }

    override fun cacheNode(uuid: String, node: AccessibilityNodeInfo) {
        cache[uuid] = node
    }

    override fun invalidateCache() {
        // Recycle all cached nodes
        for ((_, node) in cache) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        }
        cache.clear()
        Log.d(TAG, "Cache invalidated")
    }

    override fun buildCache(root: AccessibilityNodeInfo) {
        invalidateCache()
        traverseAndCache(root)
        Log.d(TAG, "Cache built with ${cache.size} nodes")
    }

    private fun traverseAndCache(node: AccessibilityNodeInfo) {
        val uuid = generateNodeUuid(node)
        cacheNode(uuid, node)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseAndCache(child)
        }
    }

    override fun findNodeByUuid(root: AccessibilityNodeInfo, uuid: String): AccessibilityNodeInfo? {
        // Check cache first
        cache[uuid]?.let { return it }

        // Search tree
        return searchTreeForUuid(root, uuid)
    }

    private fun searchTreeForUuid(node: AccessibilityNodeInfo, targetUuid: String): AccessibilityNodeInfo? {
        val nodeUuid = generateNodeUuid(node)
        if (nodeUuid == targetUuid) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = searchTreeForUuid(child, targetUuid)
            if (found != null) return found

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }

        return null
    }

    override fun generateNodeUuid(node: AccessibilityNodeInfo): String {
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

    override fun getCacheSize(): Int = cache.size

    override fun clear() {
        invalidateCache()
    }
}
