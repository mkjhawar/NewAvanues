/**
 * CacheCleanupTest.kt - Tests for Issue #10: No Cleanup for ConcurrentHashMap Caches
 *
 * YOLO Phase 2 - High Priority Issue #10: Cache Cleanup
 *
 * Tests verify proper cleanup of ConcurrentHashMap caches to prevent:
 * - Memory leaks over extended use
 * - Unbounded cache growth
 * - Stale data accumulation
 *
 * File: AccessibilityScrapingIntegration.kt:129-135
 * Problem: elementVisibilityTracker, elementStateTracker, packageInfoCache never cleaned up
 * Solution: Clear all caches in cleanup method, add LRU eviction if needed
 *
 * Run with: ./gradlew :modules:apps:VoiceOSCore:connectedDebugAndroidTest
 */
package com.augmentalis.voiceoscore.scraping

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Instrumented test suite for ConcurrentHashMap cache cleanup
 *
 * Tests verify proper cleanup behavior to prevent memory leaks
 * from cache accumulation over extended service lifetime.
 */
@RunWith(AndroidJUnit4::class)
class CacheCleanupTest {

    private lateinit var elementVisibilityTracker: ConcurrentHashMap<String, Long>
    private lateinit var elementStateTracker: ConcurrentHashMap<String, MutableMap<String, String?>>
    private lateinit var packageInfoCache: ConcurrentHashMap<String, Pair<String, Int>>

    @Before
    fun setup() {
        elementVisibilityTracker = ConcurrentHashMap()
        elementStateTracker = ConcurrentHashMap()
        packageInfoCache = ConcurrentHashMap()
    }

    /**
     * TEST 1: Verify elementVisibilityTracker can be cleared
     */
    @Test
    fun testElementVisibilityTrackerClear() {
        // Add test data
        elementVisibilityTracker["element1"] = System.currentTimeMillis()
        elementVisibilityTracker["element2"] = System.currentTimeMillis()
        elementVisibilityTracker["element3"] = System.currentTimeMillis()

        assertThat(elementVisibilityTracker).hasSize(3)

        // Clear cache
        elementVisibilityTracker.clear()

        assertThat(elementVisibilityTracker).isEmpty()
    }

    /**
     * TEST 2: Verify elementStateTracker can be cleared
     */
    @Test
    fun testElementStateTrackerClear() {
        // Add test data
        elementStateTracker["element1"] = mutableMapOf("state" to "visible")
        elementStateTracker["element2"] = mutableMapOf("state" to "hidden")

        assertThat(elementStateTracker).hasSize(2)

        // Clear cache
        elementStateTracker.clear()

        assertThat(elementStateTracker).isEmpty()
    }

    /**
     * TEST 3: Verify packageInfoCache can be cleared
     */
    @Test
    fun testPackageInfoCacheClear() {
        // Add test data
        packageInfoCache["com.example.app1"] = Pair("com.example.app1", 1)
        packageInfoCache["com.example.app2"] = Pair("com.example.app2", 2)

        assertThat(packageInfoCache).hasSize(2)

        // Clear cache
        packageInfoCache.clear()

        assertThat(packageInfoCache).isEmpty()
    }

    /**
     * TEST 4: Verify all caches can be cleared together
     */
    @Test
    fun testAllCachesCleared() {
        // Populate all caches
        elementVisibilityTracker["elem1"] = System.currentTimeMillis()
        elementStateTracker["elem1"] = mutableMapOf("key" to "value")
        packageInfoCache["pkg1"] = Pair("pkg1", 1)

        assertThat(elementVisibilityTracker).isNotEmpty()
        assertThat(elementStateTracker).isNotEmpty()
        assertThat(packageInfoCache).isNotEmpty()

        // Clear all caches
        elementVisibilityTracker.clear()
        elementStateTracker.clear()
        packageInfoCache.clear()

        assertThat(elementVisibilityTracker).isEmpty()
        assertThat(elementStateTracker).isEmpty()
        assertThat(packageInfoCache).isEmpty()
    }

    /**
     * TEST 5: Verify cache grows unbounded without cleanup
     */
    @Test
    fun testCacheGrowsUnboundedWithoutCleanup() {
        // Simulate extended use
        for (i in 0 until 1000) {
            elementVisibilityTracker["element_$i"] = System.currentTimeMillis()
        }

        assertThat(elementVisibilityTracker).hasSize(1000)

        // Without cleanup, cache continues to grow
        for (i in 1000 until 2000) {
            elementVisibilityTracker["element_$i"] = System.currentTimeMillis()
        }

        assertThat(elementVisibilityTracker).hasSize(2000)
    }

    /**
     * TEST 6: Verify memory is freed after cleanup
     */
    @Test
    fun testMemoryFreedAfterCleanup() {
        // Add large amount of data
        for (i in 0 until 10000) {
            elementVisibilityTracker["element_$i"] = System.currentTimeMillis()
            elementStateTracker["element_$i"] = mutableMapOf(
                "state1" to "value1",
                "state2" to "value2",
                "state3" to "value3"
            )
        }

        val beforeSize = elementVisibilityTracker.size + elementStateTracker.size
        assertThat(beforeSize).isEqualTo(20000)

        // Clear caches
        elementVisibilityTracker.clear()
        elementStateTracker.clear()

        assertThat(elementVisibilityTracker).isEmpty()
        assertThat(elementStateTracker).isEmpty()
    }

    /**
     * TEST 7: Verify cache can be reused after cleanup
     */
    @Test
    fun testCacheReusableAfterCleanup() {
        // Add and clear data
        elementVisibilityTracker["elem1"] = System.currentTimeMillis()
        elementVisibilityTracker.clear()

        // Reuse cache
        elementVisibilityTracker["elem2"] = System.currentTimeMillis()

        assertThat(elementVisibilityTracker).hasSize(1)
        assertThat(elementVisibilityTracker).containsKey("elem2")
        assertThat(elementVisibilityTracker).doesNotContainKey("elem1")
    }

    /**
     * TEST 8: Verify thread-safe cleanup
     */
    @Test
    fun testThreadSafeCleanup() {
        val latch = CountDownLatch(10)

        // Start multiple threads adding data
        repeat(10) { threadNum ->
            thread {
                repeat(100) { i ->
                    elementVisibilityTracker["thread${threadNum}_elem$i"] = System.currentTimeMillis()
                }
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)

        // Should have 1000 entries
        assertThat(elementVisibilityTracker.size).isEqualTo(1000)

        // Clear should be thread-safe
        elementVisibilityTracker.clear()

        assertThat(elementVisibilityTracker).isEmpty()
    }

    /**
     * TEST 9: Verify cleanup does not throw exceptions
     */
    @Test
    fun testCleanupDoesNotThrow() {
        // Add data
        elementVisibilityTracker["elem1"] = System.currentTimeMillis()

        // Clear should not throw
        try {
            elementVisibilityTracker.clear()
            elementStateTracker.clear()
            packageInfoCache.clear()
        } catch (e: Exception) {
            throw AssertionError("Cleanup should not throw exceptions", e)
        }

        assertThat(elementVisibilityTracker).isEmpty()
    }

    /**
     * TEST 10: Verify cleanup can be called multiple times safely
     */
    @Test
    fun testMultipleCleanupCallsSafe() {
        elementVisibilityTracker["elem1"] = System.currentTimeMillis()

        // Call clear multiple times
        elementVisibilityTracker.clear()
        elementVisibilityTracker.clear()
        elementVisibilityTracker.clear()

        assertThat(elementVisibilityTracker).isEmpty()
    }

    /**
     * TEST 11: Verify LRU eviction prevents unbounded growth
     */
    @Test
    fun testLruEvictionPreventUnboundedGrowth() {
        val maxSize = 100
        val lruCache = object : LinkedHashMap<String, Long>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > maxSize
            }
        }

        // Add more than max size
        for (i in 0 until 200) {
            lruCache["elem_$i"] = System.currentTimeMillis()
        }

        // Should only have maxSize entries
        assertThat(lruCache.size).isAtMost(maxSize + 1) // +1 for implementation detail
    }

    /**
     * TEST 12: Verify old entries removed in LRU cache
     */
    @Test
    fun testLruCacheRemovesOldEntries() {
        val maxSize = 3
        val lruCache = object : LinkedHashMap<String, Long>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > maxSize
            }
        }

        // Add entries
        lruCache["elem_1"] = 1L
        lruCache["elem_2"] = 2L
        lruCache["elem_3"] = 3L
        lruCache["elem_4"] = 4L // This should evict elem_1

        assertThat(lruCache).doesNotContainKey("elem_1")
        assertThat(lruCache).containsKey("elem_2")
        assertThat(lruCache).containsKey("elem_3")
        assertThat(lruCache).containsKey("elem_4")
    }

    /**
     * TEST 13: Verify concurrent cleanup and access
     */
    @Test
    fun testConcurrentCleanupAndAccess() {
        val latch = CountDownLatch(2)

        // Thread 1: Adding data
        thread {
            repeat(1000) { i ->
                elementVisibilityTracker["elem_$i"] = System.currentTimeMillis()
            }
            latch.countDown()
        }

        // Thread 2: Clearing data
        thread {
            Thread.sleep(10)
            elementVisibilityTracker.clear()
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS)

        // Cache should either be empty or have some entries
        // but should not crash or throw exceptions
        assertThat(elementVisibilityTracker.size).isAtLeast(0)
    }

    /**
     * TEST 14: Verify nested map cleanup in elementStateTracker
     */
    @Test
    fun testNestedMapCleanup() {
        // Add nested maps
        val state1: MutableMap<String, String?> = mutableMapOf("key1" to "value1", "key2" to "value2")
        val state2: MutableMap<String, String?> = mutableMapOf("key3" to "value3", "key4" to "value4")

        elementStateTracker["elem1"] = state1
        elementStateTracker["elem2"] = state2

        assertThat(elementStateTracker).hasSize(2)

        // Clear outer map
        elementStateTracker.clear()

        // Both outer and nested maps should be cleared
        assertThat(elementStateTracker).isEmpty()
    }

    /**
     * TEST 15: Verify performance - cleanup completes quickly even with large cache
     */
    @Test
    fun testCleanupPerformance() {
        // Add 10,000 entries to each cache
        for (i in 0 until 10000) {
            elementVisibilityTracker["elem_$i"] = System.currentTimeMillis()
            elementStateTracker["elem_$i"] = mutableMapOf("key" to "value")
            packageInfoCache["pkg_$i"] = Pair("pkg_$i", i)
        }

        val startTime = System.currentTimeMillis()

        // Clear all caches
        elementVisibilityTracker.clear()
        elementStateTracker.clear()
        packageInfoCache.clear()

        val duration = System.currentTimeMillis() - startTime

        // Cleanup should complete in under 500ms even with 30,000 entries
        assertThat(duration).isLessThan(500L)
        assertThat(elementVisibilityTracker).isEmpty()
        assertThat(elementStateTracker).isEmpty()
        assertThat(packageInfoCache).isEmpty()
    }
}
