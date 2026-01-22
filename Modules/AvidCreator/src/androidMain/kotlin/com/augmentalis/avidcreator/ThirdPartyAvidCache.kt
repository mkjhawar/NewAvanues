/**
 * ThirdPartyAvidCache.kt - Performance cache for third-party UUIDs
 * Path: libraries/AvidCreator/src/main/java/com/augmentalis/uuidcreator/thirdparty/ThirdPartyAvidCache.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * In-memory cache for generated third-party UUIDs to avoid repeated generation
 */

package com.augmentalis.avidcreator.thirdparty

import android.util.LruCache
import java.util.concurrent.atomic.AtomicLong

/**
 * Third-Party UUID Cache
 *
 * High-performance in-memory cache for third-party generated UUIDs.
 * Prevents redundant UUID generation for same accessibility nodes.
 *
 * ## Caching Strategy
 *
 * - **Key**: Fingerprint hash (deterministic)
 * - **Value**: Generated UUID string
 * - **Eviction**: LRU when max size reached
 * - **Thread-Safe**: Uses Android's LruCache (synchronized internally)
 *
 * ## Performance
 *
 * - **Get**: O(1) lookup
 * - **Put**: O(1) insert
 * - **Clear**: O(n) for package-specific clear
 *
 * ## Usage Example
 *
 * ```kotlin
 * val cache = ThirdPartyAvidCache(maxSize = 5000)
 *
 * // Store UUID
 * cache.put(fingerprint, "com.app.v1.0.0.button-abc123")
 *
 * // Retrieve UUID
 * val uuid = cache.get(fingerprint) // Fast O(1) lookup
 *
 * // Check stats
 * val stats = cache.getStats()
 * println("Hit rate: ${stats.hitRate}")
 * ```
 *
 * @property maxSize Maximum cache size before eviction (default: 10,000)
 *
 * @since 1.0.0
 */
class ThirdPartyAvidCache(
    private val maxSize: Int = 10_000
) {

    /**
     * Thread-safe LRU cache using Android's LruCache
     *
     * LruCache handles both storage and LRU eviction in a synchronized manner.
     * This replaces the previous ConcurrentHashMap + LinkedHashMap combination
     * which had race conditions.
     */
    private val cache = object : LruCache<String, CacheEntry>(maxSize) {
        override fun sizeOf(key: String, value: CacheEntry): Int = 1
    }

    /**
     * Cache hit counter
     */
    private val hitCount = AtomicLong(0)

    /**
     * Cache miss counter
     */
    private val missCount = AtomicLong(0)

    /**
     * Get UUID from cache
     *
     * @param fingerprint Accessibility fingerprint
     * @return Cached UUID or null if not found
     */
    fun get(fingerprint: AccessibilityFingerprint): String? {
        val key = fingerprint.generateHash()

        val entry = cache.get(key)
        if (entry != null) {
            // Cache hit
            hitCount.incrementAndGet()
            return entry.uuid
        }

        // Cache miss
        missCount.incrementAndGet()
        return null
    }

    /**
     * Put UUID in cache
     *
     * LruCache handles eviction automatically when max size is reached.
     *
     * @param fingerprint Accessibility fingerprint
     * @param uuid Generated UUID to cache
     */
    fun put(fingerprint: AccessibilityFingerprint, uuid: String) {
        val key = fingerprint.generateHash()

        // Store entry - LruCache handles eviction automatically
        val entry = CacheEntry(
            uuid = uuid,
            packageName = fingerprint.packageName,
            version = fingerprint.appVersion,
            createdAt = System.currentTimeMillis(),
            fingerprint = fingerprint.serialize()
        )

        cache.put(key, entry)
    }

    /**
     * Remove entry from cache
     *
     * @param fingerprint Fingerprint to remove
     */
    fun remove(fingerprint: AccessibilityFingerprint) {
        val key = fingerprint.generateHash()
        cache.remove(key)
    }

    /**
     * Clear entire cache
     */
    fun clear() {
        cache.evictAll()
        hitCount.set(0)
        missCount.set(0)
    }

    /**
     * Clear cache for specific package
     *
     * Removes all UUIDs for given package name.
     * Use when app is updated or uninstalled.
     *
     * @param packageName Package name to clear
     */
    fun clearPackage(packageName: String) {
        synchronized(cache) {
            val snapshot = cache.snapshot()
            snapshot.entries
                .filter { it.value.packageName == packageName }
                .forEach { cache.remove(it.key) }
        }
    }

    /**
     * Clear cache for specific package version
     *
     * More granular than clearPackage - only removes specific version.
     *
     * @param packageName Package name
     * @param version Version string
     */
    fun clearPackageVersion(packageName: String, version: String) {
        synchronized(cache) {
            val snapshot = cache.snapshot()
            snapshot.entries
                .filter { it.value.packageName == packageName && it.value.version == version }
                .forEach { cache.remove(it.key) }
        }
    }

    /**
     * Get cache statistics
     *
     * @return Current cache stats
     */
    fun getStats(): CacheStats {
        return CacheStats(
            size = cache.size(),
            hitCount = hitCount.get(),
            missCount = missCount.get()
        )
    }

    /**
     * Get cache entries for package
     *
     * Returns all cached UUIDs for specific package.
     *
     * @param packageName Package name
     * @return List of cache entries
     */
    fun getEntriesForPackage(packageName: String): List<CacheEntry> {
        return cache.snapshot().values.filter { it.packageName == packageName }
    }

    /**
     * Get oldest entry
     *
     * @return Oldest cache entry or null if empty
     */
    fun getOldestEntry(): CacheEntry? {
        return cache.snapshot().values.minByOrNull { it.createdAt }
    }

    /**
     * Get newest entry
     *
     * @return Newest cache entry or null if empty
     */
    fun getNewestEntry(): CacheEntry? {
        return cache.snapshot().values.maxByOrNull { it.createdAt }
    }

    /**
     * Prune old entries
     *
     * Removes entries older than specified age.
     *
     * @param maxAgeMs Maximum age in milliseconds
     * @return Number of entries removed
     */
    fun pruneOldEntries(maxAgeMs: Long): Int {
        val cutoffTime = System.currentTimeMillis() - maxAgeMs
        synchronized(cache) {
            val snapshot = cache.snapshot()
            val keysToRemove = snapshot.entries
                .filter { it.value.createdAt < cutoffTime }
                .map { it.key }

            keysToRemove.forEach { cache.remove(it) }
            return keysToRemove.size
        }
    }
}

/**
 * Cache Entry
 *
 * Single cache entry with metadata.
 *
 * @property uuid Generated UUID
 * @property packageName App package name
 * @property version App version
 * @property createdAt Creation timestamp
 * @property fingerprint Serialized fingerprint (for debugging)
 */
data class CacheEntry(
    val uuid: String,
    val packageName: String,
    val version: String,
    val createdAt: Long,
    val fingerprint: String
) {
    /**
     * Entry age in milliseconds
     */
    val ageMs: Long
        get() = System.currentTimeMillis() - createdAt

    /**
     * Entry age in seconds
     */
    val ageSeconds: Long
        get() = ageMs / 1000

    /**
     * Check if entry is older than specified age
     *
     * @param maxAgeMs Maximum age in milliseconds
     * @return true if entry is too old
     */
    fun isOlderThan(maxAgeMs: Long): Boolean {
        return ageMs > maxAgeMs
    }
}
