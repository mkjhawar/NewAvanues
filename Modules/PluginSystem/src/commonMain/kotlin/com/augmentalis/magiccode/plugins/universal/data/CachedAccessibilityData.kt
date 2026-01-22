/**
 * CachedAccessibilityData.kt - Caching decorator for AccessibilityDataProvider
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides a caching layer for AccessibilityDataProvider with LRU caching,
 * TTL-based expiry, and memory-aware cache sizing.
 */
package com.augmentalis.magiccode.plugins.universal.data

import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.ScreenContext
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * Caching decorator for [AccessibilityDataProvider].
 *
 * Wraps an existing AccessibilityDataProvider with a caching layer that:
 * - Uses LRU (Least Recently Used) eviction for element cache
 * - TTL-based expiry for command data
 * - Automatic cache invalidation on screen changes
 * - Memory-aware cache sizing
 *
 * ## Cache Strategy
 * - **Elements**: LRU cache with configurable size. Hot elements (frequently
 *   accessed) stay in cache longer.
 * - **Commands**: TTL-based cache. Commands expire after configurable duration
 *   to ensure freshness after learning operations.
 * - **Navigation**: Long TTL since navigation graphs change infrequently.
 * - **Preferences**: No caching (preferences are usually small and may change).
 *
 * ## Usage
 * ```kotlin
 * val baseProvider = AccessibilityDataProviderImpl(repos...)
 * val cachedProvider = CachedAccessibilityData(
 *     delegate = baseProvider,
 *     elementCacheSize = 200,
 *     commandCacheTtlMs = 120_000 // 2 minutes
 * )
 *
 * // Use cachedProvider - caching is transparent
 * val elements = cachedProvider.getCurrentScreenElements()
 * ```
 *
 * @property delegate The underlying data provider to cache
 * @property elementCacheSize Maximum number of elements to cache
 * @property commandCacheTtlMs TTL for command cache in milliseconds
 * @since 1.0.0
 * @see AccessibilityDataProvider
 * @see AccessibilityDataProviderImpl
 */
class CachedAccessibilityData(
    private val delegate: AccessibilityDataProvider,
    private val elementCacheSize: Int = 100,
    private val commandCacheTtlMs: Long = 60_000
) : AccessibilityDataProvider by delegate {

    // =========================================================================
    // Cache Configuration
    // =========================================================================

    /**
     * TTL for navigation graph cache (longer since it changes less frequently).
     */
    private val navigationCacheTtlMs: Long = 300_000L // 5 minutes

    /**
     * TTL for top commands cache.
     */
    private val topCommandsCacheTtlMs: Long = 30_000L // 30 seconds

    // =========================================================================
    // Cache State
    // =========================================================================

    /**
     * Mutex for thread-safe cache operations.
     */
    private val cacheMutex = Mutex()

    /**
     * LRU cache for elements by AVID.
     */
    private val elementCache = LruCache<String, CacheEntry<QuantizedElement>>(elementCacheSize)

    /**
     * Cache for screen commands.
     */
    @Volatile
    private var screenCommandsCache: CacheEntry<List<QuantizedCommand>>? = null

    /**
     * Cache for command history.
     */
    @Volatile
    private var commandHistoryCache: CacheEntry<List<CommandHistoryEntry>>? = null

    /**
     * Cache for top commands by context.
     */
    private val topCommandsCache = mutableMapOf<String, CacheEntry<List<RankedCommand>>>()

    /**
     * Cache for navigation graphs by package name.
     */
    private val navigationCache = mutableMapOf<String, CacheEntry<NavigationGraph>>()

    /**
     * Last known screen context for cache invalidation.
     */
    @Volatile
    private var lastScreenContext: ScreenContext? = null

    // =========================================================================
    // Cache Statistics
    // =========================================================================

    /**
     * Cache hit counter for monitoring.
     */
    @Volatile
    private var cacheHits: Long = 0L

    /**
     * Cache miss counter for monitoring.
     */
    @Volatile
    private var cacheMisses: Long = 0L

    // =========================================================================
    // UI Element Data (Cached)
    // =========================================================================

    override suspend fun getCurrentScreenElements(): List<QuantizedElement> {
        // Elements come from the delegate's flow, no additional caching needed
        // The underlying provider maintains the current state
        return delegate.getCurrentScreenElements()
    }

    override suspend fun getElement(avid: String): QuantizedElement? {
        return cacheMutex.withLock {
            // Check cache first
            val cached = elementCache.get(avid)
            if (cached != null && !cached.isExpired(commandCacheTtlMs)) {
                cacheHits++
                return@withLock cached.value
            }

            // Cache miss - fetch from delegate
            cacheMisses++
            val element = delegate.getElement(avid)

            // Cache the result if found
            if (element != null) {
                elementCache.put(avid, CacheEntry(element))
            }

            element
        }
    }

    // =========================================================================
    // Command Data (Cached)
    // =========================================================================

    override suspend fun getScreenCommands(): List<QuantizedCommand> {
        return cacheMutex.withLock {
            // Check for screen change
            val currentContext = delegate.getScreenContext()
            if (hasScreenChanged(currentContext)) {
                invalidateScreenCaches()
            }

            // Check cache
            val cached = screenCommandsCache
            if (cached != null && !cached.isExpired(commandCacheTtlMs)) {
                cacheHits++
                return@withLock cached.value
            }

            // Cache miss
            cacheMisses++
            val commands = delegate.getScreenCommands()
            screenCommandsCache = CacheEntry(commands)

            commands
        }
    }

    override suspend fun getCommandHistory(limit: Int, successOnly: Boolean): List<CommandHistoryEntry> {
        return cacheMutex.withLock {
            // Check cache (only if same parameters - simple case)
            val cached = commandHistoryCache
            if (cached != null && !cached.isExpired(commandCacheTtlMs)) {
                cacheHits++
                // Apply filters to cached data
                var result = cached.value
                if (successOnly) {
                    result = result.filter { it.success }
                }
                return@withLock result.take(limit)
            }

            // Cache miss - fetch full history and cache it
            cacheMisses++
            val history = delegate.getCommandHistory(limit = 500, successOnly = false)
            commandHistoryCache = CacheEntry(history)

            // Apply filters
            var result = history
            if (successOnly) {
                result = result.filter { it.success }
            }
            result.take(limit)
        }
    }

    override suspend fun getTopCommands(limit: Int, context: String?): List<RankedCommand> {
        val cacheKey = context ?: "__all__"

        return cacheMutex.withLock {
            // Check cache
            val cached = topCommandsCache[cacheKey]
            if (cached != null && !cached.isExpired(topCommandsCacheTtlMs)) {
                cacheHits++
                return@withLock cached.value.take(limit)
            }

            // Cache miss
            cacheMisses++
            val commands = delegate.getTopCommands(limit = 50, context = context)
            topCommandsCache[cacheKey] = CacheEntry(commands)

            commands.take(limit)
        }
    }

    // =========================================================================
    // Screen Context (Delegated with tracking)
    // =========================================================================

    override suspend fun getScreenContext(): ScreenContext {
        val context = delegate.getScreenContext()

        // Track for cache invalidation
        cacheMutex.withLock {
            if (hasScreenChanged(context)) {
                invalidateScreenCaches()
            }
            lastScreenContext = context
        }

        return context
    }

    // =========================================================================
    // Navigation (Cached)
    // =========================================================================

    override suspend fun getNavigationGraph(packageName: String): NavigationGraph {
        return cacheMutex.withLock {
            // Check cache
            val cached = navigationCache[packageName]
            if (cached != null && !cached.isExpired(navigationCacheTtlMs)) {
                cacheHits++
                return@withLock cached.value
            }

            // Cache miss
            cacheMisses++
            val graph = delegate.getNavigationGraph(packageName)
            navigationCache[packageName] = CacheEntry(graph)

            graph
        }
    }

    // =========================================================================
    // Cache Management
    // =========================================================================

    /**
     * Check if screen has changed from last known state.
     */
    private fun hasScreenChanged(current: ScreenContext): Boolean {
        val last = lastScreenContext ?: return false
        return current.screenId() != last.screenId() ||
                current.packageName != last.packageName
    }

    /**
     * Invalidate caches that are screen-specific.
     */
    private fun invalidateScreenCaches() {
        screenCommandsCache = null
        elementCache.clear()
        // Don't clear command history - it's cross-screen
        // Don't clear top commands - filtered by context anyway
    }

    /**
     * Clear all caches.
     *
     * Call this when data may have changed significantly
     * (e.g., after learning operations).
     */
    suspend fun clearAllCaches() {
        cacheMutex.withLock {
            elementCache.clear()
            screenCommandsCache = null
            commandHistoryCache = null
            topCommandsCache.clear()
            navigationCache.clear()
            lastScreenContext = null
        }
    }

    /**
     * Invalidate navigation cache for a specific package.
     *
     * @param packageName Package to invalidate
     */
    suspend fun invalidateNavigationCache(packageName: String) {
        cacheMutex.withLock {
            navigationCache.remove(packageName)
        }
    }

    /**
     * Invalidate command caches (after learning).
     */
    suspend fun invalidateCommandCaches() {
        cacheMutex.withLock {
            screenCommandsCache = null
            commandHistoryCache = null
            topCommandsCache.clear()
        }
    }

    /**
     * Get cache statistics.
     *
     * @return CacheStats with hit/miss counts
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            hits = cacheHits,
            misses = cacheMisses,
            elementCacheSize = elementCache.size,
            elementCacheCapacity = elementCacheSize
        )
    }

    /**
     * Reset cache statistics.
     */
    fun resetStats() {
        cacheHits = 0L
        cacheMisses = 0L
    }
}

// =============================================================================
// Cache Support Classes
// =============================================================================

/**
 * Cache entry with timestamp for TTL checking.
 *
 * @property value Cached value
 * @property timestamp When this entry was created (epoch millis)
 */
internal data class CacheEntry<T>(
    val value: T,
    val timestamp: Long = currentTimeMillis()
) {
    /**
     * Check if this entry has expired.
     *
     * @param ttlMs Time-to-live in milliseconds
     * @return true if entry is older than TTL
     */
    fun isExpired(ttlMs: Long): Boolean {
        return (currentTimeMillis() - timestamp) > ttlMs
    }

    /**
     * Get age of this entry in milliseconds.
     */
    fun ageMs(): Long = currentTimeMillis() - timestamp
}

/**
 * Simple LRU (Least Recently Used) cache implementation.
 *
 * Uses a LinkedHashMap with access-order to implement LRU eviction.
 *
 * @property maxSize Maximum number of entries
 */
internal class LruCache<K, V>(private val maxSize: Int) {

    /**
     * Internal storage using LinkedHashMap with access order.
     */
    private val cache = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    /**
     * Current number of entries in cache.
     */
    val size: Int get() = cache.size

    /**
     * Get a value from cache.
     *
     * @param key Cache key
     * @return Cached value or null
     */
    fun get(key: K): V? = cache[key]

    /**
     * Put a value in cache.
     *
     * @param key Cache key
     * @param value Value to cache
     */
    fun put(key: K, value: V) {
        cache[key] = value
    }

    /**
     * Remove a value from cache.
     *
     * @param key Cache key
     * @return Removed value or null
     */
    fun remove(key: K): V? = cache.remove(key)

    /**
     * Clear all entries.
     */
    fun clear() = cache.clear()

    /**
     * Check if key exists in cache.
     *
     * @param key Cache key
     * @return true if key exists
     */
    fun contains(key: K): Boolean = cache.containsKey(key)
}

/**
 * Cache statistics for monitoring.
 *
 * @property hits Number of cache hits
 * @property misses Number of cache misses
 * @property elementCacheSize Current element cache size
 * @property elementCacheCapacity Maximum element cache capacity
 */
data class CacheStats(
    val hits: Long,
    val misses: Long,
    val elementCacheSize: Int,
    val elementCacheCapacity: Int
) {
    /**
     * Cache hit rate (0.0 - 1.0).
     */
    val hitRate: Float get() {
        val total = hits + misses
        return if (total > 0) hits.toFloat() / total else 0f
    }

    /**
     * Cache miss rate (0.0 - 1.0).
     */
    val missRate: Float get() = 1f - hitRate

    /**
     * Total number of cache accesses.
     */
    val totalAccesses: Long get() = hits + misses

    /**
     * Element cache utilization (0.0 - 1.0).
     */
    val elementCacheUtilization: Float get() {
        return if (elementCacheCapacity > 0) {
            elementCacheSize.toFloat() / elementCacheCapacity
        } else 0f
    }
}

// =============================================================================
// Utility
// =============================================================================

/**
 * Platform-agnostic current time.
 */
private fun currentTimeMillis(): Long {
    return com.augmentalis.magiccode.plugins.universal.currentTimeMillis()
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Create a CachedAccessibilityData with memory-aware sizing.
 *
 * Automatically adjusts cache sizes based on available memory.
 *
 * @param memoryBudgetMb Approximate memory budget in MB
 * @return CachedAccessibilityData with appropriate cache sizes
 */
fun AccessibilityDataProvider.withMemoryAwareCaching(
    memoryBudgetMb: Int = 10
): CachedAccessibilityData {
    // Rough estimate: each element takes ~1KB in cache
    // Allocate 70% to elements, 30% to other caches
    val elementBudgetKb = (memoryBudgetMb * 1024 * 0.7).toInt()
    val elementCacheSize = (elementBudgetKb / 1).coerceIn(50, 500)

    return CachedAccessibilityData(
        delegate = this,
        elementCacheSize = elementCacheSize,
        commandCacheTtlMs = 60_000
    )
}

/**
 * Wrap provider with aggressive caching for low-power mode.
 *
 * Uses longer TTLs to reduce repository access.
 *
 * @return CachedAccessibilityData with longer TTLs
 */
fun AccessibilityDataProvider.withLowPowerCaching(): CachedAccessibilityData {
    return CachedAccessibilityData(
        delegate = this,
        elementCacheSize = 50,
        commandCacheTtlMs = 300_000 // 5 minutes
    )
}
