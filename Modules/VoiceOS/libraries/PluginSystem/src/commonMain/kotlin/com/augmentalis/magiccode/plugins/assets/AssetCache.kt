package com.augmentalis.magiccode.plugins.assets

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe cache for resolved plugin assets with LRU eviction.
 *
 * AssetCache stores resolved AssetHandle instances to optimize asset resolution
 * performance by avoiding repeated filesystem access and validation for frequently
 * accessed assets.
 *
 * ## Caching Strategy
 * The cache uses a Least Recently Used (LRU) eviction policy:
 * - When cache reaches capacity, least recently accessed entry is evicted
 * - Each access (get) updates the entry's access time
 * - LinkedHashMap with access-order iteration provides O(1) LRU behavior
 *
 * ## Cache Invalidation
 * Cache entries should be invalidated when:
 * - Plugin is uninstalled: Use [invalidatePlugin] to remove all plugin assets
 * - Plugin is updated: Use [invalidatePlugin] to force re-resolution
 * - System low memory: Use [clear] to free memory
 * - Manual refresh needed: Use [remove] for specific assets
 *
 * ## Performance Metrics
 * The cache tracks hit/miss statistics for monitoring:
 * - Hit rate percentage via [getStats] or [getDetailedStats]
 * - Total hits and misses for performance analysis
 * - Cache size and capacity utilization
 * - Oldest entry age for cache freshness monitoring
 *
 * ## Thread Safety
 * All operations are protected by a mutex, ensuring thread-safe access from
 * multiple coroutines. The cache can be safely used in concurrent environments.
 *
 * ## Memory Management
 * Default capacity is 100 entries. Adjust based on:
 * - Available memory constraints
 * - Number of plugins and assets
 * - Asset access patterns
 *
 * @param maxCapacity Maximum number of cached entries (default 100)
 * @since 1.0.0
 * @see AssetHandle
 * @see AssetResolver
 */
class AssetCache(private val maxCapacity: Int = 100) {
    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, CacheEntry>(maxCapacity, 0.75f, true)

    // Hit rate tracking
    private var totalHits: Long = 0
    private var totalMisses: Long = 0

    companion object {
        private const val TAG = "AssetCache"
    }

    /**
     * Cache entry with timestamp for age tracking.
     *
     * @property handle Resolved asset handle
     * @property timestamp Millisecond timestamp when entry was cached
     */
    private data class CacheEntry(
        val handle: AssetHandle,
        val timestamp: Long
    )

    /**
     * Get asset from cache.
     *
     * Retrieves cached AssetHandle if present. Updates hit/miss statistics
     * and access order for LRU eviction.
     *
     * @param uri Asset URI to look up
     * @return AssetHandle if cached, null if not found
     * @since 1.0.0
     */
    suspend fun get(uri: String): AssetHandle? {
        return mutex.withLock {
            val entry = cache[uri]
            if (entry != null) {
                totalHits++
                entry.handle
            } else {
                totalMisses++
                null
            }
        }
    }

    /**
     * Put asset in cache with LRU eviction if necessary.
     *
     * Adds or updates cached AssetHandle. If cache is at capacity,
     * automatically evicts the least recently used entry.
     *
     * @param uri Asset URI key
     * @param handle Resolved asset handle to cache
     * @since 1.0.0
     */
    suspend fun put(uri: String, handle: AssetHandle) {
        mutex.withLock {
            // Remove oldest entry if at capacity
            if (cache.size >= maxCapacity) {
                val oldestKey = cache.keys.first()
                cache.remove(oldestKey)
                PluginLog.d(TAG, "Evicted from cache (LRU): $oldestKey")
            }

            cache[uri] = CacheEntry(
                handle = handle,
                timestamp = System.currentTimeMillis()
            )
            PluginLog.d(TAG, "Cached asset: $uri")
        }
    }

    /**
     * Remove specific asset from cache.
     *
     * Explicitly removes a single cached entry. Useful for invalidating
     * specific assets without clearing entire cache.
     *
     * @param uri Asset URI to remove
     * @return true if entry was removed, false if not found in cache
     * @since 1.0.0
     */
    suspend fun remove(uri: String): Boolean {
        return mutex.withLock {
            cache.remove(uri) != null
        }
    }

    /**
     * Clear all cached assets.
     *
     * Removes all entries from the cache. Useful for freeing memory
     * or forcing complete cache refresh. Does not reset hit/miss statistics.
     *
     * @since 1.0.0
     * @see resetStatistics
     */
    suspend fun clear() {
        mutex.withLock {
            val size = cache.size
            cache.clear()
            PluginLog.i(TAG, "Cleared cache: $size entries removed")
        }
    }

    /**
     * Get current cache size.
     *
     * Returns the number of entries currently in the cache.
     * Useful for monitoring cache utilization.
     *
     * @return Number of cached entries
     * @since 1.0.0
     */
    suspend fun size(): Int {
        return mutex.withLock {
            cache.size
        }
    }

    /**
     * Get cache capacity.
     *
     * Returns the maximum number of entries the cache can hold
     * before LRU eviction occurs.
     *
     * @return Maximum number of entries
     * @since 1.0.0
     */
    fun capacity(): Int {
        return maxCapacity
    }

    /**
     * Get all cached URIs.
     *
     * Returns a snapshot of all asset URIs currently in the cache.
     * Useful for debugging and cache inspection.
     *
     * @return Set of cached asset URIs
     * @since 1.0.0
     */
    suspend fun getCachedUris(): Set<String> {
        return mutex.withLock {
            cache.keys.toSet()
        }
    }

    /**
     * Invalidate cache entries for a specific plugin.
     *
     * Removes all cached assets belonging to the specified plugin.
     * Essential when a plugin is uninstalled or updated to prevent
     * serving stale or invalid assets.
     *
     * ## Example
     * ```kotlin
     * // After plugin update
     * val removedCount = cache.invalidatePlugin("com.example.plugin")
     * println("Invalidated $removedCount assets")
     * ```
     *
     * @param pluginId Plugin identifier (e.g., "com.example.plugin")
     * @return Number of entries invalidated
     * @since 1.0.0
     */
    suspend fun invalidatePlugin(pluginId: String): Int {
        return mutex.withLock {
            val toRemove = cache.keys.filter { uri ->
                uri.startsWith("plugin://$pluginId/")
            }

            toRemove.forEach { uri ->
                cache.remove(uri)
            }

            if (toRemove.isNotEmpty()) {
                PluginLog.i(TAG, "Invalidated ${toRemove.size} cache entries for plugin: $pluginId")
            }

            toRemove.size
        }
    }

    /**
     * Get cache statistics for performance monitoring.
     *
     * Returns basic cache metrics including size, capacity, hit rate,
     * and oldest entry age.
     *
     * @return Map of cache metrics with keys: "size", "capacity", "hitRatePercent", "oldestEntryAge"
     * @since 1.0.0
     * @see getDetailedStats
     */
    suspend fun getStats(): Map<String, Any> {
        return mutex.withLock {
            mapOf(
                "size" to cache.size,
                "capacity" to maxCapacity,
                "hitRatePercent" to calculateHitRate(),
                "oldestEntryAge" to getOldestEntryAge()
            )
        }
    }

    /**
     * Calculate cache hit rate based on tracked hits and misses.
     *
     * Computes percentage of cache lookups that resulted in hits
     * (found in cache) versus misses (not in cache).
     *
     * @return Hit rate percentage (0-100)
     */
    private fun calculateHitRate(): Double {
        val total = totalHits + totalMisses
        if (total == 0L) {
            return 0.0
        }
        return (totalHits.toDouble() / total.toDouble()) * 100.0
    }

    /**
     * Get age of oldest cache entry in milliseconds.
     *
     * Calculates time elapsed since the oldest cache entry was added.
     * Useful for monitoring cache freshness.
     *
     * @return Age in milliseconds, or 0 if cache is empty
     */
    private fun getOldestEntryAge(): Long {
        if (cache.isEmpty()) {
            return 0
        }

        val oldestTimestamp = cache.values.minOfOrNull { it.timestamp } ?: return 0
        return System.currentTimeMillis() - oldestTimestamp
    }

    /**
     * Reset cache statistics (hits, misses).
     *
     * Resets hit and miss counters to zero without clearing cached entries.
     * Useful for starting fresh performance measurements.
     *
     * @since 1.0.0
     */
    suspend fun resetStatistics() {
        mutex.withLock {
            totalHits = 0
            totalMisses = 0
            PluginLog.d(TAG, "Cache statistics reset")
        }
    }

    /**
     * Get detailed cache statistics including hit/miss counts.
     *
     * Returns comprehensive cache metrics including raw hit/miss counts,
     * size, capacity, hit rate percentage, and oldest entry age.
     *
     * @return Map of detailed metrics with keys: "size", "capacity", "totalHits",
     *         "totalMisses", "hitRatePercent", "oldestEntryAgeMs"
     * @since 1.0.0
     * @see getStats
     */
    suspend fun getDetailedStats(): Map<String, Any> {
        return mutex.withLock {
            mapOf(
                "size" to cache.size,
                "capacity" to maxCapacity,
                "totalHits" to totalHits,
                "totalMisses" to totalMisses,
                "hitRatePercent" to calculateHitRate(),
                "oldestEntryAgeMs" to getOldestEntryAge()
            )
        }
    }
}
