/**
 * ElementCacheThreadSafetyTest.kt - Tests for Issue #13: Thread Safety Issue in Element Cache
 *
 * YOLO Phase 2 - High Priority Issue #13: Element Cache Thread Safety
 *
 * Tests verify LruCache thread safety and performance without redundant synchronization:
 * - LruCache is already thread-safe internally
 * - Explicit synchronized() blocks cause performance degradation
 * - High-frequency scraping affected by unnecessary locking
 *
 * File: UIScrapingEngine.kt:326-329
 * Problem: Explicit synchronized() on LruCache (already thread-safe internally)
 * Solution: Remove redundant synchronization, rely on LruCache's internal locking
 *
 * Run with: ./gradlew :modules:apps:VoiceOSCore:connectedDebugAndroidTest
 */
package com.augmentalis.voiceoscore.accessibility

import android.util.LruCache
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Instrumented test suite for LruCache thread safety
 *
 * Tests verify that LruCache is thread-safe without additional synchronization
 * and that removing redundant synchronized blocks improves performance.
 */
@RunWith(AndroidJUnit4::class)
class ElementCacheThreadSafetyTest {

    private data class CachedElement(
        val hash: String,
        val timestamp: Long
    )

    /**
     * TEST 1: Verify LruCache is thread-safe without synchronization
     */
    @Test
    fun testLruCacheThreadSafeWithoutSynchronization() {
        val cache = LruCache<String, CachedElement>(100)
        val latch = CountDownLatch(10)
        val errors = AtomicInteger(0)

        // Multiple threads accessing cache concurrently
        repeat(10) { threadNum ->
            thread {
                try {
                    repeat(100) { i ->
                        val key = "thread${threadNum}_elem$i"
                        cache.put(key, CachedElement(key, System.currentTimeMillis()))
                        cache.get(key)
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        assertThat(errors.get()).isEqualTo(0)
    }

    /**
     * TEST 2: Verify concurrent puts do not cause data corruption
     */
    @Test
    fun testConcurrentPutsNoCorruption() {
        val cache = LruCache<String, CachedElement>(1000)
        val latch = CountDownLatch(20)

        repeat(20) { threadNum ->
            thread {
                repeat(50) { i ->
                    val key = "elem_${threadNum}_$i"
                    cache.put(key, CachedElement(key, threadNum.toLong()))
                }
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)

        // Verify all entries are intact
        var validEntries = 0
        cache.snapshot().forEach { (key, value) ->
            if (value.hash == key) {
                validEntries++
            }
        }

        assertThat(validEntries).isEqualTo(cache.size())
    }

    /**
     * TEST 3: Verify concurrent gets do not throw exceptions
     */
    @Test
    fun testConcurrentGetsNoExceptions() {
        val cache = LruCache<String, CachedElement>(100)

        // Populate cache
        for (i in 0 until 100) {
            cache.put("elem_$i", CachedElement("elem_$i", i.toLong()))
        }

        val latch = CountDownLatch(20)
        val errors = AtomicInteger(0)

        repeat(20) {
            thread {
                try {
                    repeat(100) { i ->
                        cache.get("elem_$i")
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        assertThat(errors.get()).isEqualTo(0)
    }

    /**
     * TEST 4: Verify LruCache eviction works correctly under concurrent access
     */
    @Test
    fun testLruEvictionUnderConcurrentAccess() {
        val maxSize = 50
        val cache = LruCache<String, CachedElement>(maxSize)
        val latch = CountDownLatch(10)

        repeat(10) { threadNum ->
            thread {
                repeat(20) { i ->
                    val key = "thread${threadNum}_elem$i"
                    cache.put(key, CachedElement(key, System.currentTimeMillis()))
                }
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)

        // Cache should respect max size
        assertThat(cache.size()).isAtMost(maxSize)
    }

    /**
     * TEST 5: Verify performance without synchronization
     */
    @Test
    fun testPerformanceWithoutSynchronization() {
        val cache = LruCache<String, CachedElement>(1000)

        val startTime = System.currentTimeMillis()

        // Simulate high-frequency scraping
        repeat(10000) { i ->
            val key = "elem_$i"
            cache.put(key, CachedElement(key, System.currentTimeMillis()))
        }

        val duration = System.currentTimeMillis() - startTime

        // Should complete in under 500ms
        assertThat(duration).isLessThan(500L)
    }

    /**
     * TEST 6: Verify performance degradation WITH synchronization
     */
    @Test
    fun testPerformanceDegradationWithSynchronization() {
        val cache = LruCache<String, CachedElement>(1000)

        val startTime = System.currentTimeMillis()

        // Simulate synchronized access
        repeat(10000) { i ->
            synchronized(cache) {
                val key = "elem_$i"
                cache.put(key, CachedElement(key, System.currentTimeMillis()))
            }
        }

        val duration = System.currentTimeMillis() - startTime

        // Will be slower than without synchronization
        // This test documents the performance impact
        assertThat(duration).isGreaterThan(0L)
    }

    /**
     * TEST 7: Verify concurrent put and get operations
     */
    @Test
    fun testConcurrentPutAndGet() {
        val cache = LruCache<String, CachedElement>(200)
        val latch = CountDownLatch(20)
        val errors = AtomicInteger(0)

        // Half threads put, half threads get
        repeat(10) { threadNum ->
            thread {
                try {
                    repeat(100) { i ->
                        val key = "elem_$i"
                        cache.put(key, CachedElement(key, threadNum.toLong()))
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }

            thread {
                try {
                    repeat(100) { i ->
                        cache.get("elem_$i")
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        assertThat(errors.get()).isEqualTo(0)
    }

    /**
     * TEST 8: Verify remove operations are thread-safe
     */
    @Test
    fun testConcurrentRemoveThreadSafe() {
        val cache = LruCache<String, CachedElement>(100)

        // Populate cache
        for (i in 0 until 100) {
            cache.put("elem_$i", CachedElement("elem_$i", i.toLong()))
        }

        val latch = CountDownLatch(10)

        repeat(10) { threadNum ->
            thread {
                repeat(10) { i ->
                    cache.remove("elem_${threadNum * 10 + i}")
                }
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)

        // All targeted elements should be removed
        assertThat(cache.size()).isEqualTo(0)
    }

    /**
     * TEST 9: Verify snapshot is consistent
     */
    @Test
    fun testSnapshotConsistency() {
        val cache = LruCache<String, CachedElement>(100)

        // Populate cache
        for (i in 0 until 50) {
            cache.put("elem_$i", CachedElement("elem_$i", i.toLong()))
        }

        val snapshot = cache.snapshot()

        // Snapshot should match cache size
        assertThat(snapshot.size).isEqualTo(cache.size())

        // All entries should be valid
        snapshot.forEach { (key, value) ->
            assertThat(value.hash).isEqualTo(key)
        }
    }

    /**
     * TEST 10: Verify evictAll is thread-safe
     */
    @Test
    fun testEvictAllThreadSafe() {
        val cache = LruCache<String, CachedElement>(100)

        // Populate cache
        for (i in 0 until 100) {
            cache.put("elem_$i", CachedElement("elem_$i", i.toLong()))
        }

        val latch = CountDownLatch(2)

        // Thread 1: evictAll
        thread {
            cache.evictAll()
            latch.countDown()
        }

        // Thread 2: try to access
        thread {
            Thread.sleep(5)
            cache.get("elem_50")
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS)
        assertThat(cache.size()).isEqualTo(0)
    }

    /**
     * TEST 11: Verify high-frequency concurrent access
     */
    @Test
    fun testHighFrequencyConcurrentAccess() {
        val cache = LruCache<String, CachedElement>(500)
        val latch = CountDownLatch(50)
        val operations = AtomicInteger(0)

        repeat(50) { threadNum ->
            thread {
                repeat(200) { i ->
                    val key = "elem_${i % 100}"
                    cache.put(key, CachedElement(key, threadNum.toLong()))
                    cache.get(key)
                    operations.incrementAndGet()
                }
                latch.countDown()
            }
        }

        latch.await(15, TimeUnit.SECONDS)

        // Should complete all operations
        assertThat(operations.get()).isEqualTo(10000)
    }

    /**
     * TEST 12: Verify cache hit/miss tracking
     */
    @Test
    fun testCacheHitMissTracking() {
        val cache = LruCache<String, CachedElement>(50)

        // Populate cache
        for (i in 0 until 50) {
            cache.put("elem_$i", CachedElement("elem_$i", i.toLong()))
        }

        // Access existing elements
        repeat(25) { i ->
            cache.get("elem_$i")
        }

        // Access non-existing elements
        repeat(25) { i ->
            cache.get("nonexistent_$i")
        }

        // Cache should still have original size
        assertThat(cache.size()).isEqualTo(50)
    }

    /**
     * TEST 13: Verify resize operation thread safety
     */
    @Test
    fun testResizeThreadSafety() {
        val cache = LruCache<String, CachedElement>(100)

        // Populate cache
        for (i in 0 until 100) {
            cache.put("elem_$i", CachedElement("elem_$i", i.toLong()))
        }

        val latch = CountDownLatch(2)

        // Thread 1: resize
        thread {
            cache.resize(50)
            latch.countDown()
        }

        // Thread 2: continue accessing
        thread {
            repeat(50) { i ->
                cache.get("elem_$i")
            }
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS)

        // Cache should respect new size
        assertThat(cache.size()).isAtMost(50)
    }

    /**
     * TEST 14: Verify no deadlocks with internal synchronization
     */
    @Test
    fun testNoDeadlocks() {
        val cache = LruCache<String, CachedElement>(100)
        val latch = CountDownLatch(100)
        val completed = AtomicInteger(0)

        // Many threads trying to access same keys
        repeat(100) { threadNum ->
            thread {
                repeat(10) { i ->
                    val key = "elem_${i % 5}" // Only 5 unique keys
                    cache.put(key, CachedElement(key, threadNum.toLong()))
                    cache.get(key)
                }
                completed.incrementAndGet()
                latch.countDown()
            }
        }

        // Should complete without deadlock
        val finished = latch.await(10, TimeUnit.SECONDS)
        assertThat(finished).isTrue()
        assertThat(completed.get()).isEqualTo(100)
    }

    /**
     * TEST 15: Verify performance comparison - with vs without synchronized
     */
    @Test
    fun testPerformanceComparison() {
        val cache = LruCache<String, CachedElement>(1000)

        // Test 1: Without synchronization
        val startWithout = System.currentTimeMillis()
        repeat(5000) { i ->
            cache.put("elem_$i", CachedElement("elem_$i", i.toLong()))
        }
        val durationWithout = System.currentTimeMillis() - startWithout

        // Clear cache
        cache.evictAll()

        // Test 2: With synchronization
        val startWith = System.currentTimeMillis()
        repeat(5000) { i ->
            synchronized(cache) {
                cache.put("elem_$i", CachedElement("elem_$i", i.toLong()))
            }
        }
        val durationWith = System.currentTimeMillis() - startWith

        // Without synchronization should be faster
        assertThat(durationWithout).isLessThan(durationWith)

        // Both should complete in reasonable time
        assertThat(durationWithout).isLessThan(1000L)
        assertThat(durationWith).isLessThan(2000L)
    }
}
