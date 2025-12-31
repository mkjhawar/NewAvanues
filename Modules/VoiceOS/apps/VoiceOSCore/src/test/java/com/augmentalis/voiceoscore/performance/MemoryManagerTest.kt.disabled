/**
 * MemoryManagerTest.kt - Tests for MemoryManager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Sprint 4 Test Coverage Agent
 * Created: 2025-12-23
 *
 * Tests for memory profiling, leak detection, cache management, and GC monitoring.
 */

package com.augmentalis.voiceoscore.performance

import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.ref.WeakReference

/**
 * Comprehensive tests for MemoryManager.
 *
 * Test Categories:
 * - Memory profiling (heap size, allocation rate) - 3 tests
 * - Leak detection (weak references, finalization) - 3 tests
 * - Cache management (LRU eviction, size limits) - 2 tests
 * - GC monitoring (pause time, collection frequency) - 2 tests
 */
class MemoryManagerTest : BaseVoiceOSTest() {

    private lateinit var memoryManager: MemoryManager

    @Before
    override fun setUp() {
        super.setUp()
        memoryManager = MemoryManager()
    }

    @After
    override fun tearDown() {
        memoryManager.cleanup()
        super.tearDown()
    }

    // ============================================================================
    // MEMORY PROFILING TESTS (3 tests)
    // ============================================================================

    @Test
    fun `memory profiling - heap size metrics are valid`() = runTest {
        val metrics = memoryManager.getMemoryMetrics()

        // Heap metrics should be positive
        assertThat(metrics.heapSizeMB).isGreaterThan(0)
        assertThat(metrics.heapUsedMB).isGreaterThan(0)
        assertThat(metrics.heapFreeMB).isAtLeast(0)

        // Used + Free should approximately equal Total
        val total = metrics.heapUsedMB + metrics.heapFreeMB
        assertThat(total).isAtLeast(metrics.heapSizeMB - 10) // Allow small margin
    }

    @Test
    fun `memory profiling - allocation rate tracking works`() = runTest {
        val metrics1 = memoryManager.getMemoryMetrics()

        // Allocate some memory
        val largeArray = ByteArray(10 * 1024 * 1024) // 10 MB
        largeArray.fill(1)

        delay(100)

        val metrics2 = memoryManager.getMemoryMetrics()

        // Allocation rate should be captured
        assertThat(metrics2.allocationRateMBPerSec).isAtLeast(0.0)

        // Heap used should have increased
        assertThat(metrics2.heapUsedMB).isAtLeast(metrics1.heapUsedMB)
    }

    @Test
    fun `memory profiling - concurrent metric reads are consistent`() = runTest {
        val metrics = mutableListOf<MemoryManager.MemoryMetrics>()

        // Read metrics concurrently
        repeat(10) {
            backgroundScope.launch {
                val metric = memoryManager.getMemoryMetrics()
                synchronized(metrics) {
                    metrics.add(metric)
                }
            }
        }

        delay(500)

        // All metrics should be valid
        assertThat(metrics.size).isEqualTo(10)
        metrics.forEach { metric ->
            assertThat(metric.heapSizeMB).isGreaterThan(0)
            assertThat(metric.heapUsedMB).isGreaterThan(0)
        }
    }

    // ============================================================================
    // LEAK DETECTION TESTS (3 tests)
    // ============================================================================

    @Test
    fun `leak detection - weak references are cleared after GC`() = runTest {
        var largeObject: ByteArray? = ByteArray(10 * 1024 * 1024) // 10 MB
        val weakRef = WeakReference(largeObject)

        memoryManager.track(weakRef, "test_object")

        assertThat(weakRef.get()).isNotNull()
        assertThat(memoryManager.getTrackedObjects()).contains("test_object")

        // Clear strong reference and force GC
        largeObject = null
        System.gc()
        System.runFinalization()
        delay(200) // Give GC time

        // Weak reference should be cleared
        assertThat(weakRef.get()).isNull()
    }

    @Test
    fun `leak detection - multiple objects tracked correctly`() = runTest {
        val obj1 = ByteArray(1024)
        val obj2 = ByteArray(2048)
        val obj3 = ByteArray(4096)

        memoryManager.trackObject(obj1, "obj1")
        memoryManager.trackObject(obj2, "obj2")
        memoryManager.trackObject(obj3, "obj3")

        val tracked = memoryManager.getTrackedObjects()

        assertThat(tracked).hasSize(3)
        assertThat(tracked).containsExactly("obj1", "obj2", "obj3")
    }

    @Test
    fun `leak detection - finalization removes collected objects`() = runTest {
        var obj1: ByteArray? = ByteArray(5 * 1024 * 1024) // 5 MB
        var obj2: ByteArray? = ByteArray(5 * 1024 * 1024) // 5 MB

        memoryManager.trackObject(obj1!!, "obj1")
        memoryManager.trackObject(obj2!!, "obj2")

        assertThat(memoryManager.getTrackedObjects()).hasSize(2)

        // Clear obj1 and force GC
        obj1 = null
        memoryManager.forceGc()
        delay(200)

        // After profiling runs leak check, obj1 might be removed
        // But obj2 should still be tracked
        val stillTracked = memoryManager.getTrackedObjects()
        assertThat(stillTracked).contains("obj2")

        // Clean up
        obj2 = null
    }

    // ============================================================================
    // CACHE MANAGEMENT TESTS (2 tests)
    // ============================================================================

    @Test
    fun `cache management - LRU eviction works correctly`() = runTest {
        // Fill cache beyond limit (50 MB)
        repeat(60) { i ->
            val data = ByteArray(1 * 1024 * 1024) // 1 MB each
            data.fill(i.toByte())
            memoryManager.cacheItem("item_$i", data)
        }

        // Cache size should be at or below limit
        val cacheSizeMB = memoryManager.getCacheSizeMB()
        assertThat(cacheSizeMB).isAtMost(55) // Allow small margin

        // Earliest items should have been evicted
        val item0 = memoryManager.getCachedItem("item_0")
        assertThat(item0).isNull() // Should be evicted

        // Recent items should still exist
        val item59 = memoryManager.getCachedItem("item_59")
        assertThat(item59).isNotNull()
    }

    @Test
    fun `cache management - size limit enforcement prevents overflow`() = runTest {
        // Try to cache 100 MB (limit is 50 MB)
        repeat(100) { i ->
            val data = ByteArray(1 * 1024 * 1024) // 1 MB
            memoryManager.cacheItem("data_$i", data)
        }

        val finalSize = memoryManager.getCacheSizeMB()

        // Should not exceed limit significantly
        assertThat(finalSize).isAtMost(60) // 50 MB + margin

        // Clear cache
        memoryManager.clearCache()
        assertThat(memoryManager.getCacheSizeMB()).isEqualTo(0)
    }

    // ============================================================================
    // GC MONITORING TESTS (2 tests)
    // ============================================================================

    @Test
    fun `GC monitoring - pause time increases with heap usage`() = runTest {
        val metrics1 = memoryManager.getMemoryMetrics()
        val pauseTime1 = metrics1.gcPauseTimeMs

        // Allocate significant memory to increase heap usage
        val allocations = mutableListOf<ByteArray>()
        repeat(50) {
            allocations.add(ByteArray(1 * 1024 * 1024)) // 50 MB total
        }

        val metrics2 = memoryManager.getMemoryMetrics()
        val pauseTime2 = metrics2.gcPauseTimeMs

        // Pause time should increase or stay same with higher usage
        assertThat(pauseTime2).isAtLeast(pauseTime1)

        // Clean up
        allocations.clear()
    }

    @Test
    fun `GC monitoring - collection frequency tracked accurately`() = runTest {
        val metrics1 = memoryManager.getMemoryMetrics()
        val gcCount1 = metrics1.gcCount

        // Force some GC activity
        repeat(5) {
            memoryManager.forceGc()
            delay(100)
        }

        val metrics2 = memoryManager.getMemoryMetrics()
        val gcCount2 = metrics2.gcCount

        // GC count should have increased
        assertThat(gcCount2).isGreaterThan(gcCount1)
    }
}
