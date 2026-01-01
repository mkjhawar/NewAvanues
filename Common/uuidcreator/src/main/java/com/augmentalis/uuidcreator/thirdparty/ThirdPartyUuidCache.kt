/**
 * ThirdPartyUuidCache.kt - Performance cache for third-party UUIDs
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/thirdparty/ThirdPartyUuidCache.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * In-memory cache for generated third-party UUIDs to avoid repeated generation
 */

package com.augmentalis.uuidcreator.thirdparty

import java.util.concurrent.ConcurrentHashMap
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
 * - **Thread-Safe**: ConcurrentHashMap with atomic counters
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
 * val cache = ThirdPartyUuidCache(maxSize = 5000)
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
class ThirdPartyUuidCache(
    private val maxSize: Int = 10_000
) {

    /**
     * Cache storage
     *
     * Maps fingerprint hash → UUID string
     */
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /**
     * Access tracking for LRU eviction
     */
    private val accessOrder = LinkedHashMap<String, Long>(maxSize, 0.75f, true)

    /**
     * Cache hit counter
     */
    private val hitCount = AtomicLong(0)

    /**
     * Cache miss counter
     */
    private val missCount = AtomicLong(0)

    /**
     * Current access timestamp
     */
    private val accessTimestamp = AtomicLong(0)

    /**
     * Get UUID from cache
     *
     * @param fingerprint Accessibility fingerprint
     * @return Cached UUID or null if not found
     */
    fun get(fingerprint: AccessibilityFingerprint): String? {
        val key = fingerprint.generateHash()

        val entry = cache[key]
        if (entry != null) {
            // Cache hit
            hitCount.incrementAndGet()
            updateAccessTime(key)
            return entry.uuid
        }

        // Cache miss
        missCount.incrementAndGet()
        return null
    }

    /**
     * Put UUID in cache
     *
     * If cache is full, evicts least recently used entry.
     *
     * @param fingerprint Accessibility fingerprint
     * @param uuid Generated UUID to cache
     */
    fun put(fingerprint: AccessibilityFingerprint, uuid: String) {
        val key = fingerprint.generateHash()

        // Check if cache is full
        if (cache.size >= maxSize && !cache.containsKey(key)) {
            evictLRU()
        }

        // Store entry
        val entry = CacheEntry(
            uuid = uuid,
            packageName = fingerprint.packageName,
            version = fingerprint.appVersion,
            createdAt = System.currentTimeMillis(),
            fingerprint = fingerprint.serialize()
        )

        cache[key] = entry
        updateAccessTime(key)
    }

    /**
     * Remove entry from cache
     *
     * @param fingerprint Fingerprint to remove
     */
    fun remove(fingerprint: AccessibilityFingerprint) {
        val key = fingerprint.generateHash()
        cache.remove(key)
        accessOrder.remove(key)
    }

    /**
     * Clear entire cache
     */
    fun clear() {
        cache.clear()
        accessOrder.clear()
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
        val keysToRemove = cache.entries
            .filter { it.value.packageName == packageName }
            .map { it.key }

        keysToRemove.forEach { key ->
            cache.remove(key)
            accessOrder.remove(key)
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
        val keysToRemove = cache.entries
            .filter { it.value.packageName == packageName && it.value.version == version }
            .map { it.key }

        keysToRemove.forEach { key ->
            cache.remove(key)
            accessOrder.remove(key)
        }
    }

    /**
     * Get cache statistics
     *
     * @return Current cache stats
     */
    fun getStats(): CacheStats {
        return CacheStats(
            size = cache.size,
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
        return cache.values.filter { it.packageName == packageName }
    }

    /**
     * Update access time for key (LRU tracking)
     *
     * @param key Cache key
     */
    private fun updateAccessTime(key: String) {
        synchronized(accessOrder) {
            accessOrder[key] = accessTimestamp.incrementAndGet()
        }
    }

    /**
     * Evict least recently used entry
     */
    private fun evictLRU() {
        synchronized(accessOrder) {
            val lruKey = accessOrder.keys.firstOrNull() ?: return
            cache.remove(lruKey)
            accessOrder.remove(lruKey)
        }
    }

    /**
     * Get oldest entry
     *
     * @return Oldest cache entry or null if empty
     */
    fun getOldestEntry(): CacheEntry? {
        return cache.values.minByOrNull { it.createdAt }
    }

    /**
     * Get newest entry
     *
     * @return Newest cache entry or null if empty
     */
    fun getNewestEntry(): CacheEntry? {
        return cache.values.maxByOrNull { it.createdAt }
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
        val keysToRemove = cache.entries
            .filter { it.value.createdAt < cutoffTime }
            .map { it.key }

        keysToRemove.forEach { key ->
            cache.remove(key)
            accessOrder.remove(key)
        }

        return keysToRemove.size
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
