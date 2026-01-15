/**
 * INodeCache.kt - Interface for accessibility node caching
 *
 * Cross-platform interface for caching UI element references.
 * Platform implementations handle platform-specific node types.
 *
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: JITLearning/handlers/INodeCache.kt
 *
 * @since 3.0.0 (KMP Migration)
 */

package com.augmentalis.voiceoscoreng.jit

/**
 * Node Reference - Cross-platform representation of a UI node
 *
 * Contains essential node properties that can be used across platforms.
 * Platform-specific implementations may hold additional native references.
 */
data class NodeReference(
    /** Unique AVID for this node */
    val avid: String,

    /** Class name of the UI element */
    val className: String,

    /** Resource ID (if available) */
    val resourceId: String?,

    /** Text content (if available) */
    val text: String?,

    /** Content description (if available) */
    val contentDescription: String?,

    /** Bounds in screen coordinates */
    val boundsLeft: Int,
    val boundsTop: Int,
    val boundsRight: Int,
    val boundsBottom: Int,

    /** Interaction capabilities */
    val isClickable: Boolean,
    val isFocusable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean
) {
    /**
     * Get center point of this node.
     */
    fun getCenterX(): Int = (boundsLeft + boundsRight) / 2
    fun getCenterY(): Int = (boundsTop + boundsBottom) / 2

    /**
     * Get width and height.
     */
    fun getWidth(): Int = boundsRight - boundsLeft
    fun getHeight(): Int = boundsBottom - boundsTop

    /**
     * Check if a point is within this node's bounds.
     */
    fun containsPoint(x: Int, y: Int): Boolean {
        return x >= boundsLeft && x <= boundsRight && y >= boundsTop && y <= boundsBottom
    }

    /**
     * Get short class name (without package).
     */
    fun getShortClassName(): String {
        return className.substringAfterLast(".")
    }
}

/**
 * Node Cache Manager Interface
 *
 * Cross-platform interface for caching UI element references.
 * Platform-specific implementations handle native node types.
 *
 * ## Responsibilities:
 * - Cache UI nodes by AVID
 * - Retrieve cached nodes
 * - Invalidate stale cache
 * - Build cache from hierarchy traversal
 * - LRU eviction for memory management
 *
 * ## Usage:
 * ```kotlin
 * val cache: INodeCache = AndroidNodeCacheManager()
 * cache.buildCacheFromRoot(rootNode)
 * val node = cache.getNodeReference("btn-001")
 * ```
 */
interface INodeCache {
    /**
     * Get cached node reference by AVID.
     *
     * @param avid Element AVID
     * @return Cached node reference or null if not found
     */
    fun getNodeReference(avid: String): NodeReference?

    /**
     * Cache a node reference with AVID.
     *
     * @param avid Element AVID
     * @param node Node reference to cache
     */
    fun cacheNodeReference(avid: String, node: NodeReference)

    /**
     * Invalidate entire cache.
     *
     * Clears all cached nodes and frees resources.
     */
    fun invalidateCache()

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

    /**
     * Check if a node is cached.
     *
     * @param avid Element AVID
     * @return true if node is in cache
     */
    fun isCached(avid: String): Boolean

    /**
     * Get all cached AVIDs.
     *
     * @return Set of all cached AVIDs
     */
    fun getAllCachedAvids(): Set<String>

    /**
     * Find nodes matching a predicate.
     *
     * @param predicate Filter function
     * @return List of matching node references
     */
    fun findNodes(predicate: (NodeReference) -> Boolean): List<NodeReference>

    /**
     * Find nodes by class name.
     *
     * @param className Class name to match (can be short or full)
     * @return List of matching node references
     */
    fun findByClassName(className: String): List<NodeReference>

    /**
     * Find nodes by text content.
     *
     * @param text Text to match (partial match)
     * @return List of matching node references
     */
    fun findByText(text: String): List<NodeReference>

    /**
     * Find clickable nodes.
     *
     * @return List of clickable node references
     */
    fun findClickableNodes(): List<NodeReference>
}
