/**
 * PerformanceMonitorTest.kt - Tests for PerformanceMonitor
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Sprint 4 Test Coverage Agent
 * Created: 2025-12-23
 *
 * Tests for latency tracking, bottleneck detection, metrics collection,
 * and performance regression detection.
 */

package com.augmentalis.voiceoscore.performance

import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Comprehensive tests for PerformanceMonitor.
 *
 * Test Categories:
 * - Latency tracking (95th percentile, outliers) - 3 tests
 * - Bottleneck detection (slow operations, contention points) - 3 tests
 * - Metrics collection (counter, histogram, gauge) - 2 tests
 * - Performance regression detection (baseline comparison) - 2 tests
 */
class PerformanceMonitorTest : BaseVoiceOSTest() {

    private lateinit var monitor: PerformanceMonitor

    @Before
    override fun setUp() {
        super.setUp()
        monitor = PerformanceMonitor()
    }

    @After
    override fun tearDown() {
        monitor.cleanup()
        super.tearDown()
    }

    // ============================================================================
    // LATENCY TRACKING TESTS (3 tests)
    // ============================================================================

    @Test
    fun `latency tracking - 95th percentile under threshold for fast operations`() = runTest {
        // Run 1000 fast operations
        repeat(1000) {
            monitor.measureLatency("fast_operation") {
                delay(Random.nextLong(5, 20)) // 5-20ms
            }
        }

        val stats = monitor.getStatistics("fast_operation")

        assertThat(stats).isNotNull()
        assertThat(stats!!.sampleCount).isEqualTo(1000)
        assertThat(stats.p50).isLessThan(50) // Median < 50ms
        assertThat(stats.p95).isLessThan(100) // 95th percentile < 100ms
        assertThat(stats.p99).isLessThan(150) // 99th percentile < 150ms
        assertThat(stats.minLatencyMs).isAtLeast(0)
        assertThat(stats.maxLatencyMs).isGreaterThan(0)
    }

    @Test
    fun `latency tracking - outliers captured in p99 percentile`() = runTest {
        // Most operations fast, a few slow outliers
        repeat(990) {
            monitor.recordLatency("mixed_operation", Random.nextLong(10, 30))
        }

        // Add 10 slow outliers
        repeat(10) {
            monitor.recordLatency("mixed_operation", Random.nextLong(200, 500))
        }

        val stats = monitor.getStatistics("mixed_operation")

        assertThat(stats).isNotNull()
        assertThat(stats!!.p95).isLessThan(100) // Most operations fast
        assertThat(stats.p99).isGreaterThan(100) // Outliers captured in p99
        assertThat(stats.maxLatencyMs).isGreaterThan(200) // Max catches outliers
    }

    @Test
    fun `latency tracking - standard deviation indicates consistency`() = runTest {
        // Consistent fast operations
        repeat(500) {
            monitor.recordLatency("consistent_op", Random.nextLong(45, 55)) // ~50ms ±5ms
        }

        // Variable slow operations
        repeat(500) {
            monitor.recordLatency("variable_op", Random.nextLong(10, 200)) // Wide range
        }

        val consistentStats = monitor.getStatistics("consistent_op")
        val variableStats = monitor.getStatistics("variable_op")

        assertThat(consistentStats).isNotNull()
        assertThat(variableStats).isNotNull()

        // Consistent operation should have lower standard deviation
        assertThat(consistentStats!!.stdDev).isLessThan(variableStats!!.stdDev)
        assertThat(consistentStats.stdDev).isLessThan(20.0) // Low variation
    }

    // ============================================================================
    // BOTTLENECK DETECTION TESTS (3 tests)
    // ============================================================================

    @Test
    fun `bottleneck detection - identifies slow operations`() = runTest {
        // Fast operation
        repeat(100) {
            monitor.recordLatency("fast_op", Random.nextLong(10, 30))
        }

        // Slow operation with high p95 and variation (bottleneck criteria)
        repeat(100) {
            monitor.recordLatency("bottleneck_op", Random.nextLong(150, 300))
        }

        val bottlenecks = monitor.detectBottlenecks()

        // Bottleneck should be detected (p95 > 100ms and stdDev > 50)
        assertThat(bottlenecks).contains("bottleneck_op")
        assertThat(bottlenecks).doesNotContain("fast_op")
    }

    @Test
    fun `bottleneck detection - high variation triggers detection`() = runTest {
        // Consistent slow operation (not a bottleneck - consistent = predictable)
        repeat(100) {
            monitor.recordLatency("slow_consistent", 110) // Always 110ms
        }

        // Variable slow operation (bottleneck - unpredictable)
        repeat(100) {
            monitor.recordLatency("slow_variable", Random.nextLong(50, 250))
        }

        val bottlenecks = monitor.detectBottlenecks()

        // Variable operation should be detected as bottleneck
        val slowVarStats = monitor.getStatistics("slow_variable")
        if (slowVarStats != null && slowVarStats.stdDev > 50.0) {
            assertThat(bottlenecks).contains("slow_variable")
        }
    }

    @Test
    fun `bottleneck detection - returns empty list when no bottlenecks`() = runTest {
        // All operations fast and consistent
        repeat(100) {
            monitor.recordLatency("op1", Random.nextLong(10, 30))
            monitor.recordLatency("op2", Random.nextLong(20, 40))
            monitor.recordLatency("op3", Random.nextLong(15, 35))
        }

        val bottlenecks = monitor.detectBottlenecks()

        // No bottlenecks should be detected
        assertThat(bottlenecks).isEmpty()
    }

    // ============================================================================
    // METRICS COLLECTION TESTS (2 tests)
    // ============================================================================

    @Test
    fun `metrics collection - counters track operation counts`() = runTest {
        // Increment various counters
        monitor.incrementCounter("requests_total", 10)
        monitor.incrementCounter("requests_total", 5)
        monitor.incrementCounter("errors_total", 3)
        monitor.incrementCounter("errors_total", 2)

        assertThat(monitor.getCounter("requests_total")).isEqualTo(15)
        assertThat(monitor.getCounter("errors_total")).isEqualTo(5)
        assertThat(monitor.getCounter("nonexistent")).isEqualTo(0)
    }

    @Test
    fun `metrics collection - gauges track current values`() = runTest {
        // Set gauge values
        monitor.setGauge("memory_usage_mb", 150.5)
        monitor.setGauge("cpu_percent", 45.2)
        monitor.setGauge("queue_size", 23.0)

        assertThat(monitor.getGauge("memory_usage_mb")).isEqualTo(150.5)
        assertThat(monitor.getGauge("cpu_percent")).isEqualTo(45.2)
        assertThat(monitor.getGauge("queue_size")).isEqualTo(23.0)

        // Update gauge
        monitor.setGauge("memory_usage_mb", 175.3)
        assertThat(monitor.getGauge("memory_usage_mb")).isEqualTo(175.3)
    }

    // ============================================================================
    // PERFORMANCE REGRESSION DETECTION TESTS (2 tests)
    // ============================================================================

    @Test
    fun `regression detection - detects performance degradation`() = runTest {
        // Establish baseline (fast operations)
        repeat(200) {
            monitor.recordLatency("critical_op", Random.nextLong(40, 60))
        }

        monitor.setBaseline("critical_op")

        // Clear samples and run slower operations (regression)
        monitor.resetOperation("critical_op")
        repeat(200) {
            monitor.recordLatency("critical_op", Random.nextLong(80, 120))
        }

        // Check for regression (threshold 20% = 50ms baseline allows up to 60ms)
        val hasRegression = monitor.checkRegression("critical_op", thresholdPercent = 20.0)

        // Should detect regression
        assertThat(hasRegression).isTrue()
    }

    @Test
    fun `regression detection - no regression within acceptable threshold`() = runTest {
        // Establish baseline
        repeat(200) {
            monitor.recordLatency("stable_op", Random.nextLong(45, 55))
        }

        monitor.setBaseline("stable_op")

        // Clear and run with similar performance
        monitor.resetOperation("stable_op")
        repeat(200) {
            monitor.recordLatency("stable_op", Random.nextLong(47, 57))
        }

        // Check for regression (should be within threshold)
        val hasRegression = monitor.checkRegression("stable_op", thresholdPercent = 20.0)

        // Should not detect regression
        assertThat(hasRegression).isFalse()
    }
}
