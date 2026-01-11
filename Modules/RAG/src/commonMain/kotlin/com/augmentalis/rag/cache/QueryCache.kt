// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/cache/QueryCache.kt
// created: 2025-11-28
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.cache

import com.augmentalis.rag.domain.Embedding
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * LRU cache for query embeddings
 *
 * Caches frequently used queries to avoid re-embedding.
 * Uses LRU eviction policy with TTL expiration.
 *
 * Performance Impact:
 * - Cache hit: ~0ms (vs 50-100ms for embedding generation)
 * - Expected hit rate: 30-50% for typical usage patterns
 * - Memory overhead: ~1.5KB per cached query (384 floats)
 *
 * Thread Safety:
 * - Not thread-safe - external synchronization required for concurrent access
 * - SQLiteRAGRepository uses this from Dispatchers.IO (single-threaded executor)
 */
class QueryCache(
    private val maxSize: Int = 1000,
    private val ttl: Duration = 1.hours
) {
    // Manual LRU tracking using LinkedHashMap insertion order
    // We'll move entries to end on access to maintain LRU order
    private val cache = LinkedHashMap<String, CachedEntry>()

    // Statistics tracking
    private var hits = 0L
    private var misses = 0L

    private data class CachedEntry(
        val embedding: Embedding.Float32,
        val timestamp: Instant,
        var lastAccessed: Instant
    )

    /**
     * Get cached embedding for query
     *
     * @param query Query text (will be normalized)
     * @return Cached embedding or null if not found/expired
     */
    fun get(query: String): Embedding.Float32? {
        val normalized = normalizeQuery(query)
        val entry = cache[normalized]

        if (entry == null) {
            misses++
            return null
        }

        // Check TTL expiration
        val now = Clock.System.now()
        val age = now - entry.timestamp
        if (age > ttl) {
            cache.remove(normalized)
            misses++
            return null
        }

        // Update last accessed time and move to end (LRU)
        entry.lastAccessed = now
        cache.remove(normalized)
        cache[normalized] = entry

        hits++
        return entry.embedding
    }

    /**
     * Put embedding in cache
     *
     * @param query Query text
     * @param embedding Query embedding
     */
    fun put(query: String, embedding: Embedding.Float32) {
        val normalized = normalizeQuery(query)
        val now = Clock.System.now()

        // If entry already exists, remove it first to update position
        if (cache.containsKey(normalized)) {
            cache.remove(normalized)
        }

        // LRU eviction if at capacity
        if (cache.size >= maxSize) {
            // Remove least recently accessed (first in map)
            val lruKey = cache.keys.firstOrNull()
            if (lruKey != null) {
                cache.remove(lruKey)
            }
        }

        cache[normalized] = CachedEntry(
            embedding = embedding,
            timestamp = now,
            lastAccessed = now
        )
    }

    /**
     * Clear all cached entries
     */
    fun clear() {
        cache.clear()
        hits = 0L
        misses = 0L
    }

    /**
     * Get cache statistics
     */
    fun stats(): CacheStats {
        val now = Clock.System.now()
        val validEntries = cache.values.count { (now - it.timestamp) <= ttl }

        return CacheStats(
            size = cache.size,
            validEntries = validEntries,
            maxSize = maxSize,
            hits = hits,
            misses = misses,
            hitRate = if (hits + misses > 0) hits.toFloat() / (hits + misses) else 0f
        )
    }

    /**
     * Remove expired entries from cache
     *
     * Should be called periodically to free memory.
     * Not required for correctness (get() checks expiration).
     */
    fun evictExpired() {
        val now = Clock.System.now()
        val expiredKeys = cache.filterValues { entry ->
            (now - entry.timestamp) > ttl
        }.keys.toList()

        expiredKeys.forEach { cache.remove(it) }
    }

    /**
     * Normalize query for cache key
     *
     * Handles case, whitespace, punctuation variations.
     * This ensures queries like "Hello World" and "hello  world"
     * hit the same cache entry.
     */
    private fun normalizeQuery(query: String): String {
        return query.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9\\s]"), "")
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val size: Int,
    val validEntries: Int,
    val maxSize: Int,
    val hits: Long,
    val misses: Long,
    val hitRate: Float
) {
    /**
     * Memory estimate in bytes
     *
     * Each entry: 384 floats * 4 bytes + overhead ~= 1.6KB
     */
    val estimatedMemoryBytes: Long
        get() = size * 1600L
}
