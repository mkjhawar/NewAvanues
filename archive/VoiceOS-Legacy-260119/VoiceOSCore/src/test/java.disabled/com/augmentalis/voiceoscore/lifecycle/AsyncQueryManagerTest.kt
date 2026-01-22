/**
 * AsyncQueryManagerTest.kt - TDD Tests for Async Database Query Management
 *
 * YOLO Phase 1 - Critical Issue #1: runBlocking on UI Thread
 *
 * Problem:
 * - AccessibilityScrapingIntegration.kt:819 uses runBlocking on UI thread
 * - Blocks UI thread during database queries
 * - Can cause ANR (Application Not Responding)
 * - Violates Android threading best practices
 *
 * Solution:
 * - Create AsyncQueryManager for non-blocking database access
 * - Use proper coroutines with lifecycle-aware scopes
 * - Provide caching layer to reduce database calls
 * - Ensure all queries run on background thread
 *
 * Test Strategy:
 * - RED: Write comprehensive failing tests first
 * - GREEN: Implement minimal code to pass tests
 * - REFACTOR: Optimize and clean up
 *
 * Coverage Target: 100% (critical path)
 */
package com.augmentalis.voiceoscore.lifecycle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * Test suite for AsyncQueryManager
 *
 * Tests verify:
 * 1. Queries never block UI thread
 * 2. Queries run on background thread (Dispatchers.IO)
 * 3. Results cached to reduce database calls
 * 4. Cache invalidation works correctly
 * 5. Concurrent queries handled properly
 * 6. Memory limits enforced (LRU cache)
 * 7. Error handling doesn't block caller
 * 8. Lifecycle awareness prevents leaks
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AsyncQueryManagerTest {

    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== RED PHASE TESTS (Should FAIL initially) ====================

    /**
     * TEST 1: Verify manager can be created
     * Basic sanity check
     */
    @Test
    fun `test manager can be created`() = runTest {
        val manager = AsyncQueryManager(maxCacheSize = 100)
        assertThat(manager).isNotNull()
        manager.close()
    }

    /**
     * TEST 2: Verify manager implements AutoCloseable
     * Required for use{} pattern and resource safety
     */
    @Test
    fun `test manager implements AutoCloseable`() = runTest {
        val manager: AutoCloseable = AsyncQueryManager()
        assertThat(manager).isInstanceOf(AutoCloseable::class.java)
        manager.close()
    }

    /**
     * TEST 3: Verify query returns result asynchronously
     * Critical: Must not block caller
     */
    @Test
    fun `test query returns result without blocking`() = runTest {
        val manager = AsyncQueryManager()

        val queryExecuted = AtomicInteger(0)
        val queryFunction: suspend () -> String = {
            queryExecuted.incrementAndGet()
            "test-result"
        }

        // Execute query
        val result = manager.query(key = "test-key", queryFunction = queryFunction)

        // Assert
        assertThat(result).isEqualTo("test-result")
        assertThat(queryExecuted.get()).isEqualTo(1)

        manager.close()
    }

    /**
     * TEST 4: Verify queries run on IO dispatcher (not Main)
     * Critical: Must not block UI thread
     */
    @Test
    fun `test queries run on IO dispatcher not Main thread`() = runTest {
        val manager = AsyncQueryManager()

        var threadName = ""
        val queryFunction: suspend () -> String = {
            threadName = Thread.currentThread().name
            "result"
        }

        manager.query(key = "test-key", queryFunction = queryFunction)

        // Assert: Should NOT be on main thread
        assertThat(threadName).doesNotContain("main")
        assertThat(threadName).isNotEmpty()

        manager.close()
    }

    /**
     * TEST 5: Verify cache returns same result without re-executing query
     * Performance optimization
     */
    @Test
    fun `test cache returns result without re-executing query`() = runTest {
        val manager = AsyncQueryManager()

        val executionCount = AtomicInteger(0)
        val queryFunction: suspend () -> String = {
            executionCount.incrementAndGet()
            "cached-result"
        }

        // First call - executes query
        val result1 = manager.query(key = "cache-test", queryFunction = queryFunction)

        // Second call - should return cached result
        val result2 = manager.query(key = "cache-test", queryFunction = queryFunction)

        // Assert
        assertThat(result1).isEqualTo("cached-result")
        assertThat(result2).isEqualTo("cached-result")
        assertThat(executionCount.get()).isEqualTo(1)  // Only executed once!

        manager.close()
    }

    /**
     * TEST 6: Verify different keys execute different queries
     * Cache isolation
     */
    @Test
    fun `test different keys execute different queries`() = runTest {
        val manager = AsyncQueryManager()

        val executionCount = AtomicInteger(0)

        val query1: suspend () -> String = {
            executionCount.incrementAndGet()
            "result-1"
        }

        val query2: suspend () -> String = {
            executionCount.incrementAndGet()
            "result-2"
        }

        // Different keys should execute separately
        val result1 = manager.query(key = "key-1", queryFunction = query1)
        val result2 = manager.query(key = "key-2", queryFunction = query2)

        // Assert
        assertThat(result1).isEqualTo("result-1")
        assertThat(result2).isEqualTo("result-2")
        assertThat(executionCount.get()).isEqualTo(2)  // Both executed

        manager.close()
    }

    /**
     * TEST 7: Verify cache invalidation forces re-execution
     * Cache management
     */
    @Test
    fun `test invalidate forces query re-execution`() = runTest {
        val manager = AsyncQueryManager()

        val executionCount = AtomicInteger(0)
        val queryFunction: suspend () -> String = {
            val count = executionCount.incrementAndGet()
            "result-$count"
        }

        // First call
        val result1 = manager.query(key = "invalidate-test", queryFunction = queryFunction)

        // Invalidate cache
        manager.invalidate("invalidate-test")

        // Second call - should re-execute
        val result2 = manager.query(key = "invalidate-test", queryFunction = queryFunction)

        // Assert
        assertThat(result1).isEqualTo("result-1")
        assertThat(result2).isEqualTo("result-2")
        assertThat(executionCount.get()).isEqualTo(2)  // Executed twice

        manager.close()
    }

    /**
     * TEST 8: Verify LRU cache eviction when max size exceeded
     * Memory management
     */
    @Test
    fun `test LRU cache evicts oldest entries when max size exceeded`() = runTest {
        val manager = AsyncQueryManager(maxCacheSize = 3)

        val executionCounts = mutableMapOf<String, Int>()

        fun createQuery(key: String): suspend () -> String = {
            executionCounts[key] = executionCounts.getOrDefault(key, 0) + 1
            "result-$key"
        }

        // Fill cache to capacity (3 items)
        manager.query("key-1", createQuery("key-1"))  // Exec 1: cache = [key-1]
        manager.query("key-2", createQuery("key-2"))  // Exec 1: cache = [key-1, key-2]
        manager.query("key-3", createQuery("key-3"))  // Exec 1: cache = [key-1, key-2, key-3]

        // Add 4th item - should evict key-1 (least recently used)
        manager.query("key-4", createQuery("key-4"))  // Exec 1: cache = [key-2, key-3, key-4]

        // Verify key-1 was evicted by checking current cache size
        assertThat(manager.getCacheSize()).isEqualTo(3)

        // Re-query key-1 - should re-execute (was evicted)
        // This adds key-1 back, evicting key-2
        manager.query("key-1", createQuery("key-1"))  // Exec 2: cache = [key-3, key-4, key-1]

        // Re-query key-3 and key-4 - should still be cached
        manager.query("key-3", createQuery("key-3"))  // Cache hit
        manager.query("key-4", createQuery("key-4"))  // Cache hit

        // Assert - Verify execution counts match LRU eviction behavior
        assertThat(executionCounts["key-1"]).isEqualTo(2)  // Initial + re-query after eviction
        assertThat(executionCounts["key-2"]).isEqualTo(1)  // Initial only (evicted, not re-queried)
        assertThat(executionCounts["key-3"]).isEqualTo(1)  // Initial only (still cached)
        assertThat(executionCounts["key-4"]).isEqualTo(1)  // Initial only (still cached)

        manager.close()
    }

    /**
     * TEST 9: Verify concurrent queries with same key don't duplicate execution
     * Concurrency optimization
     */
    @Test
    fun `test concurrent queries with same key execute only once`() = runTest {
        val manager = AsyncQueryManager()

        val executionCount = AtomicInteger(0)
        val queryFunction: suspend () -> String = {
            kotlinx.coroutines.delay(50)  // Simulate slow query
            executionCount.incrementAndGet()
            "concurrent-result"
        }

        // Launch 5 concurrent queries with same key
        val jobs = List(5) {
            launch {
                manager.query(key = "concurrent-test", queryFunction = queryFunction)
            }
        }

        jobs.forEach { it.join() }

        // Assert: Should only execute once despite 5 concurrent requests
        assertThat(executionCount.get()).isEqualTo(1)

        manager.close()
    }

    /**
     * TEST 10: Verify query exception propagates to caller
     * Error handling
     */
    @Test
    fun `test query exception propagates to caller`() = runTest {
        val manager = AsyncQueryManager()

        val queryFunction: suspend () -> String = {
            throw IllegalStateException("Query failed")
        }

        // Assert exception thrown
        assertFailsWith<IllegalStateException> {
            manager.query(key = "error-test", queryFunction = queryFunction)
        }

        manager.close()
    }

    /**
     * TEST 11: Verify failed queries not cached
     * Error handling
     */
    @Test
    fun `test failed queries not cached`() = runTest {
        val manager = AsyncQueryManager()

        val executionCount = AtomicInteger(0)
        val queryFunction: suspend () -> String = {
            val count = executionCount.incrementAndGet()
            if (count == 1) {
                throw IllegalStateException("First call fails")
            }
            "success-result"
        }

        // First call fails
        try {
            manager.query(key = "fail-test", queryFunction = queryFunction)
        } catch (e: IllegalStateException) {
            // Expected
        }

        // Second call should re-execute (failure not cached)
        val result = manager.query(key = "fail-test", queryFunction = queryFunction)

        // Assert
        assertThat(result).isEqualTo("success-result")
        assertThat(executionCount.get()).isEqualTo(2)  // Executed twice

        manager.close()
    }

    /**
     * TEST 12: Verify clear() removes all cached entries
     * Cache management
     */
    @Test
    fun `test clear removes all cached entries`() = runTest {
        val manager = AsyncQueryManager()

        val executionCount = AtomicInteger(0)
        val queryFunction: suspend () -> String = {
            executionCount.incrementAndGet()
            "result"
        }

        // Cache multiple queries
        manager.query(key = "key-1", queryFunction = queryFunction)
        manager.query(key = "key-2", queryFunction = queryFunction)
        manager.query(key = "key-3", queryFunction = queryFunction)

        assertThat(executionCount.get()).isEqualTo(3)

        // Clear cache
        manager.clear()

        // Re-query all - should re-execute
        manager.query(key = "key-1", queryFunction = queryFunction)
        manager.query(key = "key-2", queryFunction = queryFunction)
        manager.query(key = "key-3", queryFunction = queryFunction)

        // Assert: All re-executed
        assertThat(executionCount.get()).isEqualTo(6)

        manager.close()
    }

    /**
     * TEST 13: Verify close() cancels in-flight queries
     * Lifecycle management
     */
    @Test
    fun `test close cancels in-flight queries`() = runTest {
        val manager = AsyncQueryManager()

        var queryCompleted = false
        val queryFunction: suspend () -> String = {
            kotlinx.coroutines.delay(1000)  // Long-running query
            queryCompleted = true
            "result"
        }

        // Launch query
        val job = launch {
            try {
                manager.query(key = "cancel-test", queryFunction = queryFunction)
            } catch (e: Exception) {
                // Expected - query cancelled
            }
        }

        // Close manager immediately
        kotlinx.coroutines.delay(10)
        manager.close()

        job.join()

        // Assert: Query should be cancelled before completion
        assertThat(queryCompleted).isFalse()
    }

    /**
     * TEST 14: Verify null values can be cached
     * Edge case handling
     */
    @Test
    fun `test null values can be cached`() = runTest {
        val manager = AsyncQueryManager()

        val executionCount = AtomicInteger(0)
        val queryFunction: suspend () -> String? = {
            executionCount.incrementAndGet()
            null
        }

        // First call
        val result1 = manager.query(key = "null-test", queryFunction = queryFunction)

        // Second call - should return cached null
        val result2 = manager.query(key = "null-test", queryFunction = queryFunction)

        // Assert
        assertThat(result1).isNull()
        assertThat(result2).isNull()
        assertThat(executionCount.get()).isEqualTo(1)  // Only executed once

        manager.close()
    }

    /**
     * TEST 15: Performance test - cache hit performance
     * Verify cache significantly faster than query execution
     */
    @Test
    fun `test cache hit performance is significantly faster`() = runTest {
        val manager = AsyncQueryManager()

        val queryFunction: suspend () -> String = {
            kotlinx.coroutines.delay(10)  // Simulate database query
            "result"
        }

        // First call - measures query execution time
        val startTime1 = System.currentTimeMillis()
        manager.query(key = "perf-test", queryFunction = queryFunction)
        val queryTime = System.currentTimeMillis() - startTime1

        // Second call - measures cache hit time
        val startTime2 = System.currentTimeMillis()
        manager.query(key = "perf-test", queryFunction = queryFunction)
        val cacheTime = System.currentTimeMillis() - startTime2

        // Assert: Cache hit should be at least 5x faster
        assertThat(cacheTime).isLessThan(queryTime / 5)

        manager.close()
    }
}
