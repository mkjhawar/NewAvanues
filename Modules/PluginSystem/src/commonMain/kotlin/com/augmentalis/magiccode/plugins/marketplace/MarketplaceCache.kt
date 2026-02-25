package com.augmentalis.magiccode.plugins.marketplace

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * In-memory cache for marketplace data with expiration support.
 *
 * MarketplaceCache provides efficient caching of marketplace API responses
 * to reduce network requests and improve responsiveness. It supports
 * automatic expiration, manual invalidation, and cache statistics.
 *
 * ## Cache Types
 * The cache stores different types of marketplace data:
 * - **Search results**: Cached by query + filters hash
 * - **Plugin details**: Cached by plugin ID
 * - **Version lists**: Cached by plugin ID
 *
 * ## Expiration
 * Each cache entry has a configurable TTL (time-to-live):
 * - Default search TTL: 5 minutes
 * - Default details TTL: 15 minutes
 * - Default versions TTL: 30 minutes
 *
 * Expired entries are returned as cache misses and lazily removed.
 *
 * ## Thread Safety
 * All cache operations are thread-safe using coroutine mutex.
 *
 * ## Usage
 * ```kotlin
 * val cache = MarketplaceCache()
 *
 * // Cache search results
 * cache.cacheSearch("voice commands", filters, results)
 *
 * // Retrieve cached results
 * val cached = cache.getCachedSearch("voice commands", filters)
 *
 * // Invalidate when data changes
 * cache.invalidate("com.example.plugin")
 * ```
 *
 * @param searchTtlMs TTL for search results in milliseconds
 * @param detailsTtlMs TTL for plugin details in milliseconds
 * @param versionsTtlMs TTL for version lists in milliseconds
 * @param maxSearchEntries Maximum number of search result entries to cache
 * @param maxDetailsEntries Maximum number of detail entries to cache
 * @since 1.0.0
 * @see MarketplaceApi
 */
class MarketplaceCache(
    private val searchTtlMs: Long = 5 * 60 * 1000L, // 5 minutes
    private val detailsTtlMs: Long = 15 * 60 * 1000L, // 15 minutes
    private val versionsTtlMs: Long = 30 * 60 * 1000L, // 30 minutes
    private val maxSearchEntries: Int = 50,
    private val maxDetailsEntries: Int = 100
) {
    companion object {
        private const val TAG = "MarketplaceCache"
    }

    private val mutex = Mutex()

    // Search results cache: key is hash of query + filters
    private val searchCache = LinkedHashMap<String, CacheEntry<List<PluginListing>>>(
        maxSearchEntries, 0.75f, true
    )

    // Plugin details cache: key is plugin ID
    private val detailsCache = LinkedHashMap<String, CacheEntry<PluginDetails>>(
        maxDetailsEntries, 0.75f, true
    )

    // Version lists cache: key is plugin ID
    private val versionsCache = LinkedHashMap<String, CacheEntry<List<VersionInfo>>>(
        maxDetailsEntries, 0.75f, true
    )

    // Statistics
    private var searchHits: Long = 0
    private var searchMisses: Long = 0
    private var detailsHits: Long = 0
    private var detailsMisses: Long = 0
    private var versionsHits: Long = 0
    private var versionsMisses: Long = 0

    /**
     * Cache entry with timestamp for expiration checking.
     *
     * @param T Type of cached data
     * @property data Cached data
     * @property timestamp Millisecond timestamp when entry was cached
     * @property ttlMs Time-to-live for this entry
     */
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val ttlMs: Long
    ) {
        /**
         * Check if this entry has expired.
         *
         * @return true if entry is older than its TTL
         */
        fun isExpired(): Boolean {
            return Clock.System.now().toEpochMilliseconds() - timestamp > ttlMs
        }

        /**
         * Get remaining TTL in milliseconds.
         *
         * @return Remaining TTL or 0 if expired
         */
        fun remainingTtl(): Long {
            val remaining = ttlMs - (Clock.System.now().toEpochMilliseconds() - timestamp)
            return if (remaining > 0) remaining else 0
        }
    }

    // ==================== Search Results Cache ====================

    /**
     * Cache search results.
     *
     * Stores search results with the default search TTL. If cache is at
     * capacity, the least recently used entry is evicted.
     *
     * @param query Search query string
     * @param filters Search filters applied
     * @param results List of plugin listings to cache
     */
    suspend fun cacheSearch(
        query: String,
        filters: SearchFilters,
        results: List<PluginListing>
    ) {
        mutex.withLock {
            val key = generateSearchKey(query, filters)
            evictIfNeeded(searchCache, maxSearchEntries)
            searchCache[key] = CacheEntry(results, Clock.System.now().toEpochMilliseconds(), searchTtlMs)
            PluginLog.d(TAG, "Cached search results for: $query (${results.size} results)")
        }
    }

    /**
     * Get cached search results.
     *
     * Returns cached results if available and not expired. Updates
     * hit/miss statistics.
     *
     * @param query Search query string
     * @param filters Search filters applied
     * @return Cached results or null if not found/expired
     */
    suspend fun getCachedSearch(
        query: String,
        filters: SearchFilters
    ): List<PluginListing>? {
        return mutex.withLock {
            val key = generateSearchKey(query, filters)
            val entry = searchCache[key]

            when {
                entry == null -> {
                    searchMisses++
                    PluginLog.d(TAG, "Search cache miss: $query")
                    null
                }
                entry.isExpired() -> {
                    searchMisses++
                    searchCache.remove(key)
                    PluginLog.d(TAG, "Search cache expired: $query")
                    null
                }
                else -> {
                    searchHits++
                    PluginLog.d(TAG, "Search cache hit: $query")
                    entry.data
                }
            }
        }
    }

    // ==================== Plugin Details Cache ====================

    /**
     * Cache plugin details.
     *
     * Stores plugin details with the default details TTL.
     *
     * @param pluginId Plugin identifier
     * @param details Plugin details to cache
     */
    suspend fun cacheDetails(pluginId: String, details: PluginDetails) {
        mutex.withLock {
            evictIfNeeded(detailsCache, maxDetailsEntries)
            detailsCache[pluginId] = CacheEntry(details, Clock.System.now().toEpochMilliseconds(), detailsTtlMs)
            PluginLog.d(TAG, "Cached details for: $pluginId")
        }
    }

    /**
     * Get cached plugin details.
     *
     * Returns cached details if available and not expired.
     *
     * @param pluginId Plugin identifier
     * @return Cached details or null if not found/expired
     */
    suspend fun getCachedDetails(pluginId: String): PluginDetails? {
        return mutex.withLock {
            val entry = detailsCache[pluginId]

            when {
                entry == null -> {
                    detailsMisses++
                    PluginLog.d(TAG, "Details cache miss: $pluginId")
                    null
                }
                entry.isExpired() -> {
                    detailsMisses++
                    detailsCache.remove(pluginId)
                    PluginLog.d(TAG, "Details cache expired: $pluginId")
                    null
                }
                else -> {
                    detailsHits++
                    PluginLog.d(TAG, "Details cache hit: $pluginId")
                    entry.data
                }
            }
        }
    }

    // ==================== Version Lists Cache ====================

    /**
     * Cache version list for a plugin.
     *
     * @param pluginId Plugin identifier
     * @param versions List of version info to cache
     */
    suspend fun cacheVersions(pluginId: String, versions: List<VersionInfo>) {
        mutex.withLock {
            evictIfNeeded(versionsCache, maxDetailsEntries)
            versionsCache[pluginId] = CacheEntry(versions, Clock.System.now().toEpochMilliseconds(), versionsTtlMs)
            PluginLog.d(TAG, "Cached versions for: $pluginId (${versions.size} versions)")
        }
    }

    /**
     * Get cached version list.
     *
     * @param pluginId Plugin identifier
     * @return Cached versions or null if not found/expired
     */
    suspend fun getCachedVersions(pluginId: String): List<VersionInfo>? {
        return mutex.withLock {
            val entry = versionsCache[pluginId]

            when {
                entry == null -> {
                    versionsMisses++
                    PluginLog.d(TAG, "Versions cache miss: $pluginId")
                    null
                }
                entry.isExpired() -> {
                    versionsMisses++
                    versionsCache.remove(pluginId)
                    PluginLog.d(TAG, "Versions cache expired: $pluginId")
                    null
                }
                else -> {
                    versionsHits++
                    PluginLog.d(TAG, "Versions cache hit: $pluginId")
                    entry.data
                }
            }
        }
    }

    // ==================== Invalidation ====================

    /**
     * Invalidate all cached data for a specific plugin.
     *
     * Removes details and versions cache for the plugin, and
     * clears search results that may contain the plugin.
     *
     * @param pluginId Plugin identifier to invalidate
     */
    suspend fun invalidate(pluginId: String) {
        mutex.withLock {
            detailsCache.remove(pluginId)
            versionsCache.remove(pluginId)

            // Remove search results that contain this plugin
            val searchKeysToRemove = searchCache.entries
                .filter { (_, entry) -> entry.data.any { it.pluginId == pluginId } }
                .map { it.key }

            searchKeysToRemove.forEach { searchCache.remove(it) }

            PluginLog.i(TAG, "Invalidated cache for: $pluginId (removed ${searchKeysToRemove.size} search entries)")
        }
    }

    /**
     * Invalidate all search results.
     *
     * Clears the search cache while preserving details and versions.
     */
    suspend fun invalidateSearches() {
        mutex.withLock {
            val count = searchCache.size
            searchCache.clear()
            PluginLog.i(TAG, "Invalidated all search cache: $count entries")
        }
    }

    /**
     * Clear all cached data.
     *
     * Removes all entries from all caches. Does not reset statistics.
     */
    suspend fun clearAll() {
        mutex.withLock {
            val searchCount = searchCache.size
            val detailsCount = detailsCache.size
            val versionsCount = versionsCache.size

            searchCache.clear()
            detailsCache.clear()
            versionsCache.clear()

            PluginLog.i(
                TAG,
                "Cleared all caches: $searchCount search, $detailsCount details, $versionsCount versions"
            )
        }
    }

    // ==================== Statistics ====================

    /**
     * Get cache statistics.
     *
     * Returns comprehensive statistics about cache performance
     * including hit rates and entry counts.
     *
     * @return Map of statistic name to value
     */
    suspend fun getStats(): Map<String, Any> {
        return mutex.withLock {
            mapOf(
                "searchEntries" to searchCache.size,
                "searchMaxEntries" to maxSearchEntries,
                "searchHits" to searchHits,
                "searchMisses" to searchMisses,
                "searchHitRate" to calculateHitRate(searchHits, searchMisses),
                "detailsEntries" to detailsCache.size,
                "detailsMaxEntries" to maxDetailsEntries,
                "detailsHits" to detailsHits,
                "detailsMisses" to detailsMisses,
                "detailsHitRate" to calculateHitRate(detailsHits, detailsMisses),
                "versionsEntries" to versionsCache.size,
                "versionsHits" to versionsHits,
                "versionsMisses" to versionsMisses,
                "versionsHitRate" to calculateHitRate(versionsHits, versionsMisses),
                "totalEntries" to (searchCache.size + detailsCache.size + versionsCache.size),
                "searchTtlMs" to searchTtlMs,
                "detailsTtlMs" to detailsTtlMs,
                "versionsTtlMs" to versionsTtlMs
            )
        }
    }

    /**
     * Reset all statistics.
     *
     * Clears hit/miss counters without affecting cached data.
     */
    suspend fun resetStats() {
        mutex.withLock {
            searchHits = 0
            searchMisses = 0
            detailsHits = 0
            detailsMisses = 0
            versionsHits = 0
            versionsMisses = 0
            PluginLog.d(TAG, "Statistics reset")
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Generate cache key for search query and filters.
     */
    private fun generateSearchKey(query: String, filters: SearchFilters): String {
        return buildString {
            append("search:")
            append(query.lowercase().trim())
            append(":cap=${filters.capability ?: ""}")
            append(":cat=${filters.category ?: ""}")
            append(":rate=${filters.minRating ?: ""}")
            append(":sort=${filters.sortBy}")
        }
    }

    /**
     * Evict oldest entry if cache is at capacity.
     */
    private fun <T> evictIfNeeded(cache: LinkedHashMap<String, CacheEntry<T>>, maxSize: Int) {
        while (cache.size >= maxSize) {
            val oldestKey = cache.keys.firstOrNull() ?: break
            cache.remove(oldestKey)
            PluginLog.d(TAG, "Evicted oldest entry: $oldestKey")
        }
    }

    /**
     * Calculate hit rate percentage.
     */
    private fun calculateHitRate(hits: Long, misses: Long): Double {
        val total = hits + misses
        return if (total == 0L) 0.0 else (hits.toDouble() / total) * 100.0
    }

    /**
     * Clean up expired entries from all caches.
     *
     * Proactively removes expired entries to free memory.
     * This is called periodically or on demand.
     *
     * @return Number of entries removed
     */
    suspend fun cleanupExpired(): Int {
        return mutex.withLock {
            var removed = 0

            // Clean search cache
            val expiredSearchKeys = searchCache.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            expiredSearchKeys.forEach { searchCache.remove(it) }
            removed += expiredSearchKeys.size

            // Clean details cache
            val expiredDetailsKeys = detailsCache.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            expiredDetailsKeys.forEach { detailsCache.remove(it) }
            removed += expiredDetailsKeys.size

            // Clean versions cache
            val expiredVersionsKeys = versionsCache.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            expiredVersionsKeys.forEach { versionsCache.remove(it) }
            removed += expiredVersionsKeys.size

            if (removed > 0) {
                PluginLog.i(TAG, "Cleaned up $removed expired entries")
            }

            removed
        }
    }
}
