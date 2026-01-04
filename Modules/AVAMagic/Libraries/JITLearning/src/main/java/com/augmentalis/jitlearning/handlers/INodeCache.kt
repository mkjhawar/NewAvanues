/**
 * INodeCache.kt - Interface for accessibility node caching
 *
 * Handles UUID to AccessibilityNodeInfo caching with LRU eviction.
 * Extracted from JITLearningService as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 2.2.0 (SOLID Refactoring)
 */

package com.augmentalis.jitlearning.handlers

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Node Cache Manager Interface
 *
 * Responsibilities:
 * - Cache accessibility nodes by UUID
 * - Retrieve cached nodes
 * - Invalidate stale cache
 * - Build cache from hierarchy traversal
 * - LRU eviction for memory management
 *
 * Single Responsibility: Node caching and retrieval
 */
interface INodeCache {
    /**
     * Get cached node by UUID.
     *
     * @param vuid Element UUID
     * @return Cached node or null if not found
     */
    fun getNode(uuid: String): AccessibilityNodeInfo?

    /**
     * Cache node with UUID.
     *
     * @param vuid Element UUID
     * @param node Accessibility node to cache
     */
    fun cacheNode(uuid: String, node: AccessibilityNodeInfo)

    /**
     * Invalidate entire cache.
     *
     * Clears all cached nodes and recycles them properly.
     */
    fun invalidateCache()

    /**
     * Build cache from root node hierarchy.
     *
     * Traverses hierarchy and caches all nodes with generated UUIDs.
     *
     * @param root Root accessibility node
     */
    fun buildCache(root: AccessibilityNodeInfo)

    /**
     * Find node by UUID in hierarchy.
     *
     * Searches tree for node matching UUID. Uses cache first, then searches.
     *
     * @param root Root node to search from
     * @param vuid Target UUID
     * @return Found node or null
     */
    fun findNodeByUuid(root: AccessibilityNodeInfo, uuid: String): AccessibilityNodeInfo?

    /**
     * Generate UUID for node.
     *
     * Creates deterministic UUID from node properties.
     *
     * @param node Node to generate UUID for
     * @return Generated UUID
     */
    fun generateNodeUuid(node: AccessibilityNodeInfo): String

    /**
     * Get cache size.
     *
     * @return Number of cached nodes
     */
    fun getCacheSize(): Int

    /**
     * Clear cache and free resources.
     */
    fun clear()
}
