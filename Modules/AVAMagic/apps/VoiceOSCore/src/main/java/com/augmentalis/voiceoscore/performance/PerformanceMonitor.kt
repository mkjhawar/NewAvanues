/**
 * PerformanceMonitor.kt - Performance monitoring and bottleneck detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Sprint 4 Performance Infrastructure
 * Created: 2025-12-23
 *
 * Provides latency tracking, bottleneck detection, metrics collection,
 * and performance regression detection.
 */

package com.augmentalis.voiceoscore.performance

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * Monitors system performance, tracks latencies, and detects bottlenecks.
 *
 * Features:
 * - Percentile latency tracking (p50, p95, p99)
 * - Bottleneck detection via slow operation identification
 * - Counter, histogram, and gauge metrics
 * - Performance regression detection against baselines
 */
class PerformanceMonitor {

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MAX_SAMPLES = 10000
        private const val SLOW_OPERATION_THRESHOLD_MS = 100L
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Operation statistics
    data class OperationStats(
        val operationName: String,
        val sampleCount: Int,
        val minLatencyMs: Long,
        val maxLatencyMs: Long,
        val avgLatencyMs: Double,
        val p50: Long,  // Median
        val p95: Long,  // 95th percentile
        val p99: Long,  // 99th percentile
        val stdDev: Double
    )

    // Latency samples for each operation
    private val latencySamples = ConcurrentHashMap<String, MutableList<Long>>()

    // Counters
    private val counters = ConcurrentHashMap<String, Long>()

    // Gauges
    private val gauges = ConcurrentHashMap<String, Double>()

    // Performance baselines
    private val baselines = ConcurrentHashMap<String, OperationStats>()

    /**
     * Measure operation latency
     */
    suspend fun <T> measureLatency(operationName: String, block: suspend () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val latency = System.currentTimeMillis() - startTime

        recordLatency(operationName, latency)

        if (latency > SLOW_OPERATION_THRESHOLD_MS) {
            Log.w(TAG, "Slow operation detected: $operationName took ${latency}ms")
        }

        return result
    }

    /**
     * Record latency sample
     */
    fun recordLatency(operationName: String, latencyMs: Long) {
        val samples = latencySamples.getOrPut(operationName) { mutableListOf() }

        synchronized(samples) {
            samples.add(latencyMs)

            // Limit sample size to prevent memory growth
            if (samples.size > MAX_SAMPLES) {
                samples.removeAt(0)
            }
        }
    }

    /**
     * Get statistics for an operation
     */
    fun getStatistics(operationName: String): OperationStats? {
        val samples = latencySamples[operationName] ?: return null

        synchronized(samples) {
            if (samples.isEmpty()) return null

            val sorted = samples.sorted()
            val count = sorted.size

            return OperationStats(
                operationName = operationName,
                sampleCount = count,
                minLatencyMs = sorted.first(),
                maxLatencyMs = sorted.last(),
                avgLatencyMs = sorted.average(),
                p50 = percentile(sorted, 50),
                p95 = percentile(sorted, 95),
                p99 = percentile(sorted, 99),
                stdDev = calculateStdDev(sorted)
            )
        }
    }

    /**
     * Calculate percentile
     */
    private fun percentile(sortedSamples: List<Long>, percentile: Int): Long {
        val index = (sortedSamples.size * percentile / 100.0).toInt()
            .coerceIn(0, sortedSamples.size - 1)
        return sortedSamples[index]
    }

    /**
     * Calculate standard deviation
     */
    private fun calculateStdDev(samples: List<Long>): Double {
        if (samples.isEmpty()) return 0.0

        val mean = samples.average()
        val variance = samples.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    /**
     * Detect bottlenecks (operations with consistently high latency)
     */
    fun detectBottlenecks(): List<String> {
        val bottlenecks = mutableListOf<String>()

        latencySamples.forEach { (operationName, samples) ->
            synchronized(samples) {
                if (samples.isEmpty()) return@forEach

                val stats = getStatistics(operationName) ?: return@forEach

                // Bottleneck criteria: p95 > threshold and high variation
                if (stats.p95 > SLOW_OPERATION_THRESHOLD_MS && stats.stdDev > 50.0) {
                    bottlenecks.add(operationName)
                    Log.w(TAG, "Bottleneck detected: $operationName (p95=${stats.p95}ms, σ=${stats.stdDev})")
                }
            }
        }

        return bottlenecks
    }

    /**
     * Increment counter
     */
    fun incrementCounter(counterName: String, delta: Long = 1) {
        counters.compute(counterName) { _, current ->
            (current ?: 0) + delta
        }
    }

    /**
     * Get counter value
     */
    fun getCounter(counterName: String): Long {
        return counters[counterName] ?: 0
    }

    /**
     * Set gauge value
     */
    fun setGauge(gaugeName: String, value: Double) {
        gauges[gaugeName] = value
    }

    /**
     * Get gauge value
     */
    fun getGauge(gaugeName: String): Double? {
        return gauges[gaugeName]
    }

    /**
     * Set performance baseline
     */
    fun setBaseline(operationName: String) {
        val stats = getStatistics(operationName)
        if (stats != null) {
            baselines[operationName] = stats
            Log.d(TAG, "Baseline set for $operationName: p95=${stats.p95}ms")
        }
    }

    /**
     * Check for performance regression
     */
    fun checkRegression(operationName: String, thresholdPercent: Double = 20.0): Boolean {
        val baseline = baselines[operationName] ?: return false
        val current = getStatistics(operationName) ?: return false

        val regressionThreshold = baseline.p95 * (1 + thresholdPercent / 100.0)
        val hasRegression = current.p95 > regressionThreshold

        if (hasRegression) {
            Log.w(TAG, "Performance regression detected for $operationName: " +
                    "baseline p95=${baseline.p95}ms, current p95=${current.p95}ms")
        }

        return hasRegression
    }

    /**
     * Get all operation names being tracked
     */
    fun getTrackedOperations(): List<String> {
        return latencySamples.keys.toList()
    }

    /**
     * Reset all metrics
     */
    fun reset() {
        latencySamples.clear()
        counters.clear()
        gauges.clear()
        Log.d(TAG, "All metrics reset")
    }

    /**
     * Reset specific operation
     */
    fun resetOperation(operationName: String) {
        latencySamples.remove(operationName)
        Log.d(TAG, "Metrics reset for $operationName")
    }

    /**
     * Get summary of all tracked operations
     */
    fun getSummary(): String {
        return buildString {
            appendLine("Performance Monitor Summary")
            appendLine("Tracked Operations: ${latencySamples.size}")
            appendLine()

            latencySamples.keys.sorted().forEach { operationName ->
                val stats = getStatistics(operationName)
                if (stats != null) {
                    appendLine("$operationName:")
                    appendLine("  Samples: ${stats.sampleCount}")
                    appendLine("  p50: ${stats.p50}ms, p95: ${stats.p95}ms, p99: ${stats.p99}ms")
                    appendLine("  Avg: ${"%.2f".format(stats.avgLatencyMs)}ms ± ${"%.2f".format(stats.stdDev)}ms")
                }
            }

            if (counters.isNotEmpty()) {
                appendLine()
                appendLine("Counters:")
                counters.forEach { (name, value) ->
                    appendLine("  $name: $value")
                }
            }

            if (gauges.isNotEmpty()) {
                appendLine()
                appendLine("Gauges:")
                gauges.forEach { (name, value) ->
                    appendLine("  $name: $value")
                }
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        reset()
        baselines.clear()
        Log.d(TAG, "PerformanceMonitor cleaned up")
    }
}
