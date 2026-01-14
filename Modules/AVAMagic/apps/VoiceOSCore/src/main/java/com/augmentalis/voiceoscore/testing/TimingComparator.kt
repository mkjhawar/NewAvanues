/**
 * TimingComparator.kt - Compare execution timing between implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Testing Framework
 * Created: 2025-10-15 02:48:36 PDT
 */
package com.augmentalis.voiceoscore.testing

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Timing measurement for a single execution
 */
data class TimingMeasurement(
    val methodName: String,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Aggregated timing statistics for a method
 */
data class TimingStats(
    val methodName: String,
    val count: Long = 0,
    val totalTimeMs: Long = 0,
    val minMs: Long = Long.MAX_VALUE,
    val maxMs: Long = 0,
    val avgMs: Double = 0.0,
    val p50Ms: Long = 0,  // Median
    val p95Ms: Long = 0,
    val p99Ms: Long = 0,
    val measurements: List<Long> = emptyList()
) {
    /**
     * Check if timing is within acceptable threshold of another
     */
    fun isWithinThreshold(other: TimingStats, thresholdPercent: Float): Boolean {
        if (count == 0L || other.count == 0L) return true // No data to compare

        val diff = abs(avgMs - other.avgMs)
        val maxAvg = maxOf(avgMs, other.avgMs)

        val percentDiff: Float = if (maxAvg > 0.0) {
            ((diff / maxAvg) * 100.0).toFloat()
        } else {
            0.0f
        }

        return percentDiff <= thresholdPercent
    }

    /**
     * Calculate percentage difference from another
     */
    fun percentDifferenceFrom(other: TimingStats): Float {
        if (other.avgMs == 0.0) return 0f

        val diff = avgMs - other.avgMs
        return (diff / other.avgMs * 100).toFloat()
    }
}

/**
 * Compare execution timing between legacy and refactored implementations
 *
 * Features:
 * - Statistical analysis (P50, P95, P99)
 * - Threshold-based divergence detection
 * - Outlier detection
 * - Performance regression detection
 */
class TimingComparator {
    private val legacyTimings = ConcurrentHashMap<String, MutableList<Long>>()
    private val refactoredTimings = ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Record a timing measurement
     */
    fun recordLegacyTiming(methodName: String, durationMs: Long) {
        legacyTimings.getOrPut(methodName) { mutableListOf() }.add(durationMs)
    }

    fun recordRefactoredTiming(methodName: String, durationMs: Long) {
        refactoredTimings.getOrPut(methodName) { mutableListOf() }.add(durationMs)
    }

    /**
     * Compare timing between legacy and refactored for a specific method
     *
     * @param legacyTimeMs Legacy execution time
     * @param refactoredTimeMs Refactored execution time
     * @param methodName Method being compared
     * @param thresholdPercent Acceptable timing difference percentage (default 20%)
     * @return List of divergences
     */
    fun compare(
        legacyTimeMs: Long,
        refactoredTimeMs: Long,
        methodName: String,
        thresholdPercent: Float = DEFAULT_THRESHOLD_PERCENT
    ): List<DivergenceDetail> {
        // Record measurements
        recordLegacyTiming(methodName, legacyTimeMs)
        recordRefactoredTiming(methodName, refactoredTimeMs)

        val divergences = mutableListOf<DivergenceDetail>()

        // Calculate percentage difference
        val maxTime = maxOf(legacyTimeMs, refactoredTimeMs)
        val diff = abs(legacyTimeMs - refactoredTimeMs)

        val percentDiff = if (maxTime > 0) {
            (diff.toFloat() / maxTime.toFloat()) * 100f
        } else 0f

        // Determine severity based on threshold
        val severity = when {
            percentDiff > 50f -> DivergenceSeverity.MEDIUM  // >50% difference
            percentDiff > thresholdPercent -> DivergenceSeverity.LOW  // >20% difference
            else -> return emptyList()  // Within threshold
        }

        val faster = if (legacyTimeMs < refactoredTimeMs) "legacy" else "refactored"
        val slower = if (faster == "legacy") "refactored" else "legacy"

        divergences.add(
            DivergenceDetail(
                category = DivergenceCategory.TIMING,
                severity = severity,
                message = "Timing difference in $methodName: $faster is ${percentDiff.toInt()}% faster ($diff ms)",
                legacyValue = legacyTimeMs,
                refactoredValue = refactoredTimeMs,
                metadata = mapOf(
                    "method" to methodName,
                    "percentDiff" to percentDiff,
                    "faster" to faster,
                    "slower" to slower,
                    "threshold" to thresholdPercent
                )
            )
        )

        return divergences
    }

    /**
     * Compare aggregated statistics for a method
     *
     * Uses all recorded measurements to compute P50, P95, P99
     */
    fun compareStats(
        methodName: String,
        thresholdPercent: Float = DEFAULT_THRESHOLD_PERCENT
    ): List<DivergenceDetail> {
        val legacyStats = computeStats(methodName, legacyTimings[methodName] ?: emptyList())
        val refactoredStats = computeStats(methodName, refactoredTimings[methodName] ?: emptyList())

        if (legacyStats.count == 0L || refactoredStats.count == 0L) {
            return listOf(
                DivergenceDetail(
                    category = DivergenceCategory.TIMING,
                    severity = DivergenceSeverity.LOW,
                    message = "Insufficient data for $methodName: legacy=${legacyStats.count}, refactored=${refactoredStats.count}",
                    legacyValue = legacyStats.count,
                    refactoredValue = refactoredStats.count
                )
            )
        }

        val divergences = mutableListOf<DivergenceDetail>()

        // Check if within threshold
        if (!legacyStats.isWithinThreshold(refactoredStats, thresholdPercent)) {
            val percentDiff = refactoredStats.percentDifferenceFrom(legacyStats)
            val severity = when {
                abs(percentDiff) > 50f -> DivergenceSeverity.MEDIUM
                else -> DivergenceSeverity.LOW
            }

            val improvement = if (percentDiff < 0) "faster" else "slower"

            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.TIMING,
                    severity = severity,
                    message = "Performance $improvement in $methodName: avg ${abs(percentDiff).toInt()}% difference",
                    legacyValue = legacyStats,
                    refactoredValue = refactoredStats,
                    metadata = mapOf(
                        "method" to methodName,
                        "percentDiff" to percentDiff,
                        "legacyAvg" to legacyStats.avgMs,
                        "refactoredAvg" to refactoredStats.avgMs,
                        "legacyP95" to legacyStats.p95Ms,
                        "refactoredP95" to refactoredStats.p95Ms
                    )
                )
            )
        }

        // Check for outliers (P99 vs P50)
        checkOutliers(methodName, legacyStats, "legacy", divergences)
        checkOutliers(methodName, refactoredStats, "refactored", divergences)

        return divergences
    }

    /**
     * Check for outliers in timing measurements
     */
    private fun checkOutliers(
        methodName: String,
        stats: TimingStats,
        implementation: String,
        divergences: MutableList<DivergenceDetail>
    ) {
        if (stats.count < 10) return // Need sufficient data

        // Check if P99 is significantly higher than P50
        val outlierRatio = stats.p99Ms.toFloat() / stats.p50Ms.toFloat()

        if (outlierRatio > OUTLIER_THRESHOLD) {
            divergences.add(
                DivergenceDetail(
                    category = DivergenceCategory.TIMING,
                    severity = DivergenceSeverity.LOW,
                    message = "Timing outliers detected in $implementation implementation of $methodName",
                    legacyValue = if (implementation == "legacy") stats else null,
                    refactoredValue = if (implementation == "refactored") stats else null,
                    metadata = mapOf(
                        "method" to methodName,
                        "implementation" to implementation,
                        "p50" to stats.p50Ms,
                        "p99" to stats.p99Ms,
                        "ratio" to outlierRatio,
                        "warning" to "P99 is ${outlierRatio}x higher than P50"
                    )
                )
            )
        }
    }

    /**
     * Compute statistics from measurements
     */
    private fun computeStats(methodName: String, measurements: List<Long>): TimingStats {
        if (measurements.isEmpty()) {
            return TimingStats(methodName = methodName)
        }

        val sorted = measurements.sorted()
        val count = sorted.size.toLong()
        val total = sorted.sum()
        val min = sorted.first()
        val max = sorted.last()
        val avg = total.toDouble() / count

        // Calculate percentiles
        val p50 = percentile(sorted, 50)
        val p95 = percentile(sorted, 95)
        val p99 = percentile(sorted, 99)

        return TimingStats(
            methodName = methodName,
            count = count,
            totalTimeMs = total,
            minMs = min,
            maxMs = max,
            avgMs = avg,
            p50Ms = p50,
            p95Ms = p95,
            p99Ms = p99,
            measurements = sorted
        )
    }

    /**
     * Calculate percentile from sorted measurements
     */
    private fun percentile(sorted: List<Long>, percentile: Int): Long {
        if (sorted.isEmpty()) return 0L

        val index = ((percentile / 100.0) * (sorted.size - 1)).toInt()
        return sorted[index]
    }

    /**
     * Get all recorded stats
     */
    fun getAllStats(): Map<String, Pair<TimingStats, TimingStats>> {
        val allMethods = (legacyTimings.keys + refactoredTimings.keys).toSet()

        return allMethods.associateWith { method ->
            val legacy = computeStats(method, legacyTimings[method] ?: emptyList())
            val refactored = computeStats(method, refactoredTimings[method] ?: emptyList())
            Pair(legacy, refactored)
        }
    }

    /**
     * Reset all recorded timings
     */
    fun reset() {
        legacyTimings.clear()
        refactoredTimings.clear()
    }

    /**
     * Reset timings for a specific method
     */
    fun resetMethod(methodName: String) {
        legacyTimings.remove(methodName)
        refactoredTimings.remove(methodName)
    }

    companion object {
        private const val TAG = "TimingComparator"
        const val DEFAULT_THRESHOLD_PERCENT = 20f  // ±20% is acceptable
        const val OUTLIER_THRESHOLD = 3.0f  // P99 > 3x P50 indicates outliers
    }
}

/**
 * Simple timing utility for measuring execution time
 */
inline fun <T> measureTiming(block: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val result = block()
    val duration = System.currentTimeMillis() - start
    return Pair(result, duration)
}

/**
 * Timing wrapper for suspend functions
 */
suspend inline fun <T> measureTimingSuspend(crossinline block: suspend () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val result = block()
    val duration = System.currentTimeMillis() - start
    return Pair(result, duration)
}
