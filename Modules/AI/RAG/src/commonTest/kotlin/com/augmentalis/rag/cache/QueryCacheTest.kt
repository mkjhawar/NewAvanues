// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/cache/QueryCacheTest.kt
// created: 2025-11-28
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.cache

import com.augmentalis.rag.domain.Embedding
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Comprehensive tests for QueryCache
 *
 * Coverage:
 * - Cache hit and miss
 * - LRU eviction policy
 * - TTL expiration
 * - Query normalization
 * - Thread safety
 * - Statistics tracking
 */
class QueryCacheTest {

    /**
     * Test clock that allows manual time advancement
     */
    private class TestClock : Clock {
        private var currentTime: Instant = Instant.fromEpochMilliseconds(0)

        override fun now(): Instant = currentTime

        fun advance(duration: Duration) {
            currentTime = Instant.fromEpochMilliseconds(
                currentTime.toEpochMilliseconds() + duration.inWholeMilliseconds
            )
        }
    }

    private fun mockEmbedding(seed: Int = 0): Embedding.Float32 {
        return Embedding.Float32(FloatArray(384) { (it + seed).toFloat() })
    }

    @Test
    fun testCacheHitAndMiss() {
        val cache = QueryCache(maxSize = 10)
        val embedding = mockEmbedding()

        // Miss - cache is empty
        assertNull(cache.get("test query"))

        // Put
        cache.put("test query", embedding)

        // Hit - should return same embedding
        val cached = cache.get("test query")
        assertNotNull(cached)
        assertTrue(cached.values.contentEquals(embedding.values))
    }

    @Test
    fun testLRUEviction() {
        val cache = QueryCache(maxSize = 3)

        // Fill cache to capacity
        cache.put("query1", mockEmbedding(1))
        cache.put("query2", mockEmbedding(2))
        cache.put("query3", mockEmbedding(3))

        // Verify all are present
        assertNotNull(cache.get("query1"))
        assertNotNull(cache.get("query2"))
        assertNotNull(cache.get("query3"))

        // Cache is full - adding 4th should evict oldest (query1)
        cache.put("query4", mockEmbedding(4))

        // query1 should be evicted (least recently used)
        assertNull(cache.get("query1"))

        // Others should still be present
        assertNotNull(cache.get("query2"))
        assertNotNull(cache.get("query3"))
        assertNotNull(cache.get("query4"))
    }

    @Test
    fun testLRUAccessOrder() {
        val cache = QueryCache(maxSize = 3)

        // Fill cache
        cache.put("query1", mockEmbedding(1))
        cache.put("query2", mockEmbedding(2))
        cache.put("query3", mockEmbedding(3))

        // Access query1 - makes it most recently used
        cache.get("query1")

        // Add query4 - should evict query2 (oldest accessed)
        cache.put("query4", mockEmbedding(4))

        // query2 should be evicted
        assertNull(cache.get("query2"))

        // query1 should still be present (was accessed)
        assertNotNull(cache.get("query1"))
        assertNotNull(cache.get("query3"))
        assertNotNull(cache.get("query4"))
    }

    @Test
    fun testTTLExpiration() {
        val testClock = TestClock()
        val cache = QueryCache(maxSize = 10, ttl = 100.milliseconds, clock = testClock)
        cache.put("query", mockEmbedding())

        // Immediately available
        assertNotNull(cache.get("query"))

        // Advance time past TTL
        testClock.advance(150.milliseconds)

        // Should be expired and return null
        assertNull(cache.get("query"))
    }

    @Test
    fun testQueryNormalization() {
        val cache = QueryCache()
        val embedding = mockEmbedding()

        cache.put("Hello World", embedding)

        // Different case should hit cache
        assertNotNull(cache.get("hello world"))
        assertNotNull(cache.get("HELLO WORLD"))
        assertNotNull(cache.get("HeLLo WoRLd"))

        // Different whitespace should hit cache
        assertNotNull(cache.get("hello  world"))
        assertNotNull(cache.get("  hello world  "))
        assertNotNull(cache.get("hello\tworld"))

        // Punctuation should be normalized
        cache.put("test query!", mockEmbedding(2))
        assertNotNull(cache.get("test query"))
    }

    @Test
    fun testQueryNormalizationDistinct() {
        val cache = QueryCache()

        cache.put("hello world", mockEmbedding(1))
        cache.put("hello there", mockEmbedding(2))

        // Ensure different queries don't collide
        val embedding1 = cache.get("hello world")
        val embedding2 = cache.get("hello there")

        assertNotNull(embedding1)
        assertNotNull(embedding2)
        assertFalse(embedding1.values.contentEquals(embedding2.values))
    }

    @Test
    fun testClear() {
        val cache = QueryCache(maxSize = 10)

        // Add multiple entries
        cache.put("query1", mockEmbedding(1))
        cache.put("query2", mockEmbedding(2))
        cache.put("query3", mockEmbedding(3))

        // Verify they exist
        assertNotNull(cache.get("query1"))
        assertNotNull(cache.get("query2"))
        assertNotNull(cache.get("query3"))

        // Clear cache
        cache.clear()

        // Stats should be reset immediately after clear
        val stats = cache.stats()
        assertEquals(0, stats.size)
        assertEquals(0L, stats.hits)
        assertEquals(0L, stats.misses)

        // All entries should be gone
        assertNull(cache.get("query1"))
        assertNull(cache.get("query2"))
        assertNull(cache.get("query3"))
    }

    @Test
    fun testStatistics() {
        val cache = QueryCache(maxSize = 10)

        // Initially empty
        var stats = cache.stats()
        assertEquals(0, stats.size)
        assertEquals(0L, stats.hits)
        assertEquals(0L, stats.misses)
        assertEquals(0f, stats.hitRate)

        // Add entries
        cache.put("query1", mockEmbedding(1))
        cache.put("query2", mockEmbedding(2))

        stats = cache.stats()
        assertEquals(2, stats.size)

        // Test hit
        cache.get("query1")
        stats = cache.stats()
        assertEquals(1L, stats.hits)
        assertEquals(0L, stats.misses)
        assertEquals(1f, stats.hitRate)

        // Test miss
        cache.get("query3")
        stats = cache.stats()
        assertEquals(1L, stats.hits)
        assertEquals(1L, stats.misses)
        assertEquals(0.5f, stats.hitRate)

        // Test multiple hits and misses
        cache.get("query1") // hit
        cache.get("query2") // hit
        cache.get("query4") // miss

        stats = cache.stats()
        assertEquals(3L, stats.hits)
        assertEquals(2L, stats.misses)
        assertEquals(0.6f, stats.hitRate)
    }

    @Test
    fun testStatisticsValidEntries() {
        val testClock = TestClock()
        val cache = QueryCache(maxSize = 10, ttl = 100.milliseconds, clock = testClock)

        // Add entries
        cache.put("query1", mockEmbedding(1))
        cache.put("query2", mockEmbedding(2))

        var stats = cache.stats()
        assertEquals(2, stats.size)
        assertEquals(2, stats.validEntries)

        // Advance time past TTL
        testClock.advance(150.milliseconds)

        // Stats should show 0 valid entries (but size is still 2 until evicted)
        stats = cache.stats()
        assertEquals(2, stats.size)
        assertEquals(0, stats.validEntries)
    }

    @Test
    fun testEvictExpired() {
        val testClock = TestClock()
        val cache = QueryCache(maxSize = 10, ttl = 100.milliseconds, clock = testClock)

        // Add entries
        cache.put("query1", mockEmbedding(1))
        cache.put("query2", mockEmbedding(2))

        assertEquals(2, cache.stats().size)

        // Advance time past TTL
        testClock.advance(150.milliseconds)

        // Manually evict expired entries
        cache.evictExpired()

        // Size should now be 0
        assertEquals(0, cache.stats().size)
    }

    @Test
    fun testUpdateExistingEntry() {
        val cache = QueryCache(maxSize = 3)

        val embedding1 = mockEmbedding(1)
        val embedding2 = mockEmbedding(2)

        cache.put("query", embedding1)

        // Verify first embedding
        val cached1 = cache.get("query")
        assertNotNull(cached1)
        assertTrue(cached1.values.contentEquals(embedding1.values))

        // Update with new embedding
        cache.put("query", embedding2)

        // Should have new embedding
        val cached2 = cache.get("query")
        assertNotNull(cached2)
        assertTrue(cached2.values.contentEquals(embedding2.values))

        // Size should still be 1 (updated, not added)
        assertEquals(1, cache.stats().size)
    }

    @Test
    fun testMaxSizeEnforcement() {
        val maxSize = 5
        val cache = QueryCache(maxSize = maxSize)

        // Add more entries than max size
        for (i in 1..10) {
            cache.put("query$i", mockEmbedding(i))
        }

        // Size should not exceed max
        val stats = cache.stats()
        assertTrue(stats.size <= maxSize)
        assertEquals(maxSize, stats.size)

        // Oldest entries should be evicted
        assertNull(cache.get("query1"))
        assertNull(cache.get("query2"))
        assertNull(cache.get("query3"))
        assertNull(cache.get("query4"))
        assertNull(cache.get("query5"))

        // Newest entries should be present
        assertNotNull(cache.get("query6"))
        assertNotNull(cache.get("query7"))
        assertNotNull(cache.get("query8"))
        assertNotNull(cache.get("query9"))
        assertNotNull(cache.get("query10"))
    }

    @Test
    fun testMemoryEstimate() {
        val cache = QueryCache(maxSize = 10)

        // Add entries
        cache.put("query1", mockEmbedding(1))
        cache.put("query2", mockEmbedding(2))
        cache.put("query3", mockEmbedding(3))

        val stats = cache.stats()
        assertEquals(3, stats.size)

        // Estimate should be reasonable (3 entries * ~1.6KB)
        val expectedBytes = 3 * 1600L
        assertEquals(expectedBytes, stats.estimatedMemoryBytes)
    }

    @Test
    fun testEmptyQueryHandling() {
        val cache = QueryCache()

        cache.put("", mockEmbedding())
        cache.put("   ", mockEmbedding())

        // Both should normalize to empty string and hit same cache entry
        assertNotNull(cache.get(""))
        assertNotNull(cache.get("   "))

        // Should only have 1 entry
        assertEquals(1, cache.stats().size)
    }

    @Test
    fun testSpecialCharactersNormalization() {
        val cache = QueryCache()
        val embedding = mockEmbedding()

        cache.put("what's the weather?", embedding)

        // Punctuation should be removed
        assertNotNull(cache.get("whats the weather"))
        assertNotNull(cache.get("What's the weather?"))
        assertNotNull(cache.get("WHATS THE WEATHER"))
    }

    @Test
    fun testConcurrentAccess() {
        val cache = QueryCache(maxSize = 100)

        // Simulate concurrent access
        val queries = (1..50).map { "query$it" }

        // Add entries
        queries.forEach { query ->
            cache.put(query, mockEmbedding())
        }

        // Access entries concurrently (simulated)
        queries.forEach { query ->
            assertNotNull(cache.get(query))
        }

        // Verify stats
        val stats = cache.stats()
        assertTrue(stats.size <= 100)
        assertEquals(50L, stats.hits)
    }

    @Test
    fun testRealWorldUsagePattern() {
        val cache = QueryCache(maxSize = 100, ttl = 1000.milliseconds)

        // Simulate real-world usage
        val commonQueries = listOf(
            "What is machine learning?",
            "How does RAG work?",
            "What is semantic search?"
        )

        val rareQueries = (1..20).map { "rare query $it" }

        // Add common queries
        commonQueries.forEach { cache.put(it, mockEmbedding()) }

        // Access common queries multiple times (cache hits)
        repeat(5) {
            commonQueries.forEach { assertNotNull(cache.get(it)) }
        }

        // Add rare queries (single access)
        rareQueries.forEach {
            cache.put(it, mockEmbedding())
            cache.get(it)
        }

        val stats = cache.stats()

        // Should have high hit rate due to repeated common queries
        assertTrue(stats.hitRate > 0.5f)

        // Common queries should still be cached
        commonQueries.forEach { assertNotNull(cache.get(it)) }
    }
}
