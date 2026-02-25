/**
 * GPUBenchmark.kt - Performance benchmarking for GPU operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-27
 *
 * Provides performance metrics for GPU vs CPU operations.
 * Used by MagicUI, NLU, AI and other modules for performance testing.
 */
package com.augmentalis.devicemanager.capabilities

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureNanoTime

/**
 * Benchmark utility for comparing GPU vs CPU operations
 */
object GPUBenchmark {

    private const val TAG = "GPUBenchmark"

    private val metrics = ConcurrentHashMap<String, MutableList<Long>>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Performance targets
     */
    object Targets {
        const val GPU_OPERATION_MS = 5L      // Target: < 5ms for GPU operations
        const val CPU_OPERATION_MS = 10L     // Target: < 10ms for CPU operations
        const val CACHE_HIT_RATE = 0.8f      // Target: > 80% cache hit rate
        const val MAX_MEMORY_MB = 5          // Target: < 5MB overhead
    }

    /**
     * Benchmark a specific operation
     */
    fun benchmarkOperation(
        name: String,
        iterations: Int = 1000,
        operation: () -> Unit,
        onComplete: (OperationBenchmarkResult) -> Unit
    ) {
        scope.launch {
            val times = mutableListOf<Long>()

            // Warm-up
            repeat(100) { operation() }

            // Benchmark
            repeat(iterations) {
                val time = measureNanoTime { operation() }
                times.add(time)
            }

            val result = OperationBenchmarkResult(
                name = name,
                iterations = iterations,
                avgNanos = times.average().toLong(),
                minNanos = times.minOrNull() ?: 0,
                maxNanos = times.maxOrNull() ?: 0,
                p95Nanos = times.percentile(95),
                gpuAvailable = GPUCapabilities.isGpuAccelerationAvailable
            )

            Log.i(TAG, result.toSummary())
            onComplete(result)
        }
    }

    /**
     * Compare GPU vs CPU for a given operation pair
     */
    fun compareGpuVsCpu(
        iterations: Int = 1000,
        gpuOperation: (() -> Unit)?,
        cpuOperation: () -> Unit,
        onComplete: (ComparisonResult) -> Unit
    ) {
        scope.launch {
            val gpuTimes = mutableListOf<Long>()
            val cpuTimes = mutableListOf<Long>()

            // Warm-up CPU
            repeat(100) { cpuOperation() }

            // CPU benchmark
            repeat(iterations) {
                val time = measureNanoTime { cpuOperation() }
                cpuTimes.add(time)
            }

            // GPU benchmark (if available)
            if (gpuOperation != null && GPUCapabilities.isGpuAccelerationAvailable) {
                repeat(100) { gpuOperation() } // Warm-up GPU
                repeat(iterations) {
                    val time = measureNanoTime { gpuOperation() }
                    gpuTimes.add(time)
                }
            }

            val result = ComparisonResult(
                iterations = iterations,
                gpuAvailable = gpuOperation != null && GPUCapabilities.isGpuAccelerationAvailable,
                gpuAvgNanos = if (gpuTimes.isNotEmpty()) gpuTimes.average().toLong() else 0,
                gpuP95Nanos = gpuTimes.percentile(95),
                cpuAvgNanos = cpuTimes.average().toLong(),
                cpuP95Nanos = cpuTimes.percentile(95),
                speedup = if (gpuTimes.isNotEmpty() && gpuTimes.average() > 0) {
                    cpuTimes.average() / gpuTimes.average()
                } else 0.0
            )

            Log.i(TAG, result.toSummary())
            onComplete(result)
        }
    }

    /**
     * Calculate percentile from a list of values
     */
    private fun List<Long>.percentile(p: Int): Long {
        if (isEmpty()) return 0
        val sorted = sorted()
        val index = (size * p / 100).coerceIn(0, size - 1)
        return sorted[index]
    }

    /**
     * Record a metric for tracking
     */
    fun recordMetric(name: String, valueNanos: Long) {
        metrics.getOrPut(name) { mutableListOf() }.add(valueNanos)
    }

    /**
     * Get metric summary
     */
    fun getMetricSummary(name: String): MetricSummary? {
        val values = metrics[name] ?: return null
        return MetricSummary(
            name = name,
            count = values.size,
            avgNanos = values.average().toLong(),
            minNanos = values.minOrNull() ?: 0,
            maxNanos = values.maxOrNull() ?: 0,
            p95Nanos = values.percentile(95)
        )
    }

    /**
     * Clear all metrics
     */
    fun clearMetrics() {
        metrics.clear()
    }

    /**
     * Operation benchmark result
     */
    data class OperationBenchmarkResult(
        val name: String,
        val iterations: Int,
        val avgNanos: Long,
        val minNanos: Long,
        val maxNanos: Long,
        val p95Nanos: Long,
        val gpuAvailable: Boolean
    ) {
        fun toSummary(): String = buildString {
            appendLine("=== Benchmark: $name ===")
            appendLine("Iterations: $iterations")
            appendLine("Avg: ${avgNanos / 1000}μs")
            appendLine("P95: ${p95Nanos / 1000}μs")
            appendLine("GPU Available: $gpuAvailable")
        }
    }

    /**
     * GPU vs CPU comparison result
     */
    data class ComparisonResult(
        val iterations: Int,
        val gpuAvailable: Boolean,
        val gpuAvgNanos: Long,
        val gpuP95Nanos: Long,
        val cpuAvgNanos: Long,
        val cpuP95Nanos: Long,
        val speedup: Double
    ) {
        fun toSummary(): String = buildString {
            appendLine("=== GPU vs CPU Comparison ===")
            appendLine("Iterations: $iterations")
            appendLine("GPU Available: $gpuAvailable")
            if (gpuAvailable) {
                appendLine("GPU Avg: ${gpuAvgNanos / 1000}μs")
                appendLine("GPU P95: ${gpuP95Nanos / 1000}μs")
            }
            appendLine("CPU Avg: ${cpuAvgNanos / 1000}μs")
            appendLine("CPU P95: ${cpuP95Nanos / 1000}μs")
            if (speedup > 0) {
                appendLine("Speedup: ${String.format("%.2f", speedup)}x")
            }
        }
    }

    /**
     * Metric summary
     */
    data class MetricSummary(
        val name: String,
        val count: Int,
        val avgNanos: Long,
        val minNanos: Long,
        val maxNanos: Long,
        val p95Nanos: Long
    )
}
