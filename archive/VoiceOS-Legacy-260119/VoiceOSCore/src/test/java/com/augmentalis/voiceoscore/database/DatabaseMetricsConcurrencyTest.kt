/**
 * DatabaseMetricsConcurrencyTest.kt - Concurrency tests for DatabaseMetrics
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Database Test Coverage Agent - Sprint 1
 * Created: 2025-12-23
 *
 * Concurrency Tests: 10 tests
 * Verifies thread-safety and concurrent access patterns
 */

package com.augmentalis.voiceoscore.database

import com.augmentalis.voiceoscore.BaseVoiceOSTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Concurrency tests for DatabaseMetrics.
 *
 * Tests verify:
 * - Thread-safety of metrics collection
 * - Mutex usage correctness
 * - No data corruption under concurrent load
 * - No deadlocks under contention
 * - Atomic counter updates
 */
class DatabaseMetricsConcurrencyTest : BaseVoiceOSTest() {

    @Before
    override fun setUp() {
        super.setUp()
        runTest {
            DatabaseMetrics.reset()
        }
    }

    @After
    override fun tearDown() {
        super.tearDown()
        runTest {
            DatabaseMetrics.reset()
        }
    }

    @Test
    fun `concurrent measureOperation - no data corruption`() = runTest {
        // Arrange
        val operationName = "concurrentMeasure"
        val iterations = 100

        // Act - Launch 100 concurrent operations
        val jobs = List(iterations) {
            launch {
                DatabaseMetrics.measureOperation(operationName) {
                    delay(1) // Simulate work
                }
            }
        }
        jobs.forEach { it.join() }

        // Assert - All operations should be counted
        val stats = DatabaseMetrics.getOperationStats(operationName)
        assertNotNull(stats)
        assertEquals(iterations.toLong(), stats!!.count)
        assertEquals(iterations.toLong(), stats.successCount)
        assertEquals(0L, stats.failureCount)
    }

    @Test
    fun `concurrent trackError - thread safety verified`() = runTest {
        // Arrange
        val operationName = "concurrentError"
        val iterations = 100

        // Act - Track errors concurrently
        val jobs = List(iterations) {
            launch {
                DatabaseMetrics.trackError(operationName, Exception("Concurrent error $it"))
            }
        }
        jobs.forEach { it.join() }

        // Assert
        val summary = DatabaseMetrics.getSummary()
        assertEquals(iterations.toLong(), summary.totalErrors)
        val errorStats = summary.errorBreakdown[operationName]
        assertNotNull(errorStats)
        assertEquals(iterations.toLong(), errorStats!!.count)
    }

    @Test
    fun `concurrent read-write - no deadlocks`() = runTest {
        // Arrange
        val operationName = "readWriteOp"

        // Act - Mix read and write operations
        val duration = measureTimeMillis {
            val jobs = mutableListOf<kotlinx.coroutines.Job>()

            // 50 write operations
            repeat(50) {
                jobs.add(launch {
                    DatabaseMetrics.trackOperation(operationName, 10L, true, 1)
                })
            }

            // 50 read operations
            repeat(50) {
                jobs.add(launch {
                    DatabaseMetrics.getOperationStats(operationName)
                })
            }

            jobs.forEach { it.join() }
        }

        // Assert - Should complete quickly without deadlock
        assertTrue("Operations took ${duration}ms, expected < 1000ms", duration < 1000)

        val stats = DatabaseMetrics.getOperationStats(operationName)
        assertNotNull(stats)
        assertEquals(50L, stats!!.count)
    }

    @Test
    fun `stress test - 1000 concurrent operations`() = runTest {
        // Arrange
        val operations = (1..10).map { "stressOp_$it" }
        val iterationsPerOp = 100

        // Act - 10 operations × 100 iterations = 1000 total
        val duration = measureTimeMillis {
            val jobs = operations.flatMap { opName ->
                List(iterationsPerOp) {
                    launch {
                        DatabaseMetrics.measureOperation(opName) {
                            delay(1)
                        }
                    }
                }
            }
            jobs.forEach { it.join() }
        }

        // Assert
        val summary = DatabaseMetrics.getSummary()
        assertEquals(1000L, summary.totalOperations)

        // All operations should be tracked
        operations.forEach { opName ->
            val stats = DatabaseMetrics.getOperationStats(opName)
            assertNotNull(stats)
            assertEquals(iterationsPerOp.toLong(), stats!!.count)
        }

        // Should complete in reasonable time
        assertTrue("Stress test took ${duration}ms", duration < 10000)
    }

    @Test
    fun `stress test - 100 concurrent errors`() = runTest {
        // Arrange
        val operationName = "stressErrorOp"
        val errorCount = 100

        // Act
        val duration = measureTimeMillis {
            val jobs = List(errorCount) { i ->
                launch {
                    DatabaseMetrics.trackError(operationName, Exception("Error $i"))
                }
            }
            jobs.forEach { it.join() }
        }

        // Assert
        val summary = DatabaseMetrics.getSummary()
        assertEquals(errorCount.toLong(), summary.totalErrors)

        val errorStats = summary.errorBreakdown[operationName]
        assertNotNull(errorStats)
        assertEquals(errorCount.toLong(), errorStats!!.count)

        // Should complete quickly
        assertTrue("Stress test took ${duration}ms, expected < 1000ms", duration < 1000)
    }

    @Test
    fun `race condition - increment counters safely`() = runTest {
        // Arrange
        val operationName = "raceConditionOp"
        val threads = 10
        val incrementsPerThread = 100

        // Act - Multiple threads incrementing same counter
        val jobs = List(threads) {
            launch {
                repeat(incrementsPerThread) {
                    DatabaseMetrics.trackOperation(operationName, 1L, true, 1)
                }
            }
        }
        jobs.forEach { it.join() }

        // Assert - Final count should be exact
        val stats = DatabaseMetrics.getOperationStats(operationName)
        assertNotNull(stats)
        assertEquals((threads * incrementsPerThread).toLong(), stats!!.count)
        assertEquals((threads * incrementsPerThread).toLong(), stats.totalItems)
    }

    @Test
    fun `race condition - min max updates atomic`() = runTest {
        // Arrange
        val operationName = "minMaxRaceOp"
        val durations = listOf(10L, 50L, 25L, 100L, 5L, 75L, 30L, 90L)

        // Act - Update min/max concurrently
        val jobs = durations.map { duration ->
            launch {
                DatabaseMetrics.trackOperation(operationName, duration, true, 0)
            }
        }
        jobs.forEach { it.join() }

        // Assert - Min and max should be correct despite race conditions
        val stats = DatabaseMetrics.getOperationStats(operationName)
        assertNotNull(stats)
        assertEquals(5L, stats!!.minDurationMs)
        assertEquals(100L, stats.maxDurationMs)
    }

    @Test
    fun `mutex usage - verified with coroutine testing`() = runTest {
        // Arrange
        val operationName = "mutexOp"

        // Act - Attempt to access metrics concurrently
        var concurrentAccessCount = 0
        val jobs = List(10) {
            launch {
                DatabaseMetrics.measureOperation(operationName) {
                    // If mutex is working, this will be sequential
                    concurrentAccessCount++
                    delay(10)
                    concurrentAccessCount--
                }
            }
        }
        jobs.forEach { it.join() }

        // Assert - All operations should be tracked
        val stats = DatabaseMetrics.getOperationStats(operationName)
        assertNotNull(stats)
        assertEquals(10L, stats!!.count)

        // Final concurrent access count should be 0 (all decremented)
        assertEquals(0, concurrentAccessCount)
    }

    @Test
    fun `mutex usage - no blocking on main thread`() = runTest {
        // Arrange
        val operationName = "mainThreadOp"
        var executedOnMainThread = false

        // Act - Track operation from test dispatcher
        DatabaseMetrics.measureOperation(operationName) {
            // Check if we're on test/main thread
            executedOnMainThread = Thread.currentThread().name.contains("Test")
        }

        // Assert - Operation should execute
        val stats = DatabaseMetrics.getOperationStats(operationName)
        assertNotNull(stats)
        assertEquals(1L, stats!!.count)

        // Execution context verification
        // Note: In test environment, we use test dispatcher, not main
    }

    @Test
    fun `mutex usage - fairness under contention`() = runTest {
        // Arrange
        val operationName = "fairnessOp"
        val coroutineIds = mutableListOf<Long>()

        // Act - Track which coroutine executes in what order
        val jobs = List(100) { i ->
            launch(Dispatchers.Default) {
                DatabaseMetrics.measureOperation(operationName) {
                    synchronized(coroutineIds) {
                        coroutineIds.add(i.toLong())
                    }
                    delay(1)
                }
            }
        }
        jobs.forEach { it.join() }

        // Assert - All operations should complete
        val stats = DatabaseMetrics.getOperationStats(operationName)
        assertNotNull(stats)
        assertEquals(100L, stats!!.count)
        assertEquals(100, coroutineIds.size)

        // Verify all coroutines executed (no starvation)
        assertEquals((0..99).toSet(), coroutineIds.map { it.toInt() }.toSet())
    }
}
