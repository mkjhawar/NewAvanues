package com.augmentalis.learnappcore.metrics

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * ExplorationMetrics - Performance telemetry for exploration
 *
 * Collects counters and histograms for monitoring exploration performance.
 * Thread-safe singleton for use across the application.
 *
 * Usage:
 * ```kotlin
 * // Increment counters
 * ExplorationMetrics.incrementScreens()
 * ExplorationMetrics.incrementCommands()
 *
 * // Record timing histograms
 * val start = System.currentTimeMillis()
 * // ... operation ...
 * ExplorationMetrics.recordScreenCapture(System.currentTimeMillis() - start)
 *
 * // Get report
 * val report = ExplorationMetrics.getReport()
 * println("Screens explored: ${report.counters["screens_explored"]}")
 * println("Avg screen capture: ${report.histograms["screen_capture_ms"]?.avg}ms")
 *
 * // Reset metrics
 * ExplorationMetrics.reset()
 * ```
 *
 * @since 2.1.0
 */
object ExplorationMetrics {

    // Counters - Thread-safe atomic operations
    private val counters = ConcurrentHashMap<String, AtomicLong>()

    // Histograms - Synchronized lists for percentile calculation
    private val histograms = ConcurrentHashMap<String, MutableList<Long>>()

    // ========================================
    // Counter Methods
    // ========================================

    /**
     * Increment the count of screens explored
     */
    fun incrementScreens() = increment("screens_explored")

    /**
     * Increment the count of UI elements discovered
     */
    fun incrementElements() = increment("elements_discovered")

    /**
     * Increment the count of voice commands generated
     */
    fun incrementCommands() = increment("commands_generated")

    /**
     * Increment the count of elements skipped due to Do Not Click list
     */
    fun incrementDNCSkipped() = increment("dnc_skipped")

    /**
     * Increment the count of login screens detected
     */
    fun incrementLoginDetected() = increment("login_detected")

    /**
     * Increment the count of dynamic content detected
     */
    fun incrementDynamicContent() = increment("dynamic_content_detected")

    /**
     * Increment the count of batch flushes
     */
    fun incrementBatchFlush() = increment("batch_flush")

    /**
     * Increment the count of navigation actions
     */
    fun incrementNavigation() = increment("navigation_actions")

    /**
     * Increment the count of errors encountered
     */
    fun incrementErrors() = increment("errors")

    // ========================================
    // Histogram Methods
    // ========================================

    /**
     * Record screen capture duration in milliseconds
     *
     * @param durationMs Duration of screen capture operation
     */
    fun recordScreenCapture(durationMs: Long) = record("screen_capture_ms", durationMs)

    /**
     * Record command generation duration in milliseconds
     *
     * @param durationMs Duration of command generation operation
     */
    fun recordCommandGeneration(durationMs: Long) = record("command_gen_ms", durationMs)

    /**
     * Record batch flush duration in milliseconds
     *
     * @param durationMs Duration of batch flush operation
     */
    fun recordBatchFlush(durationMs: Long) = record("batch_flush_ms", durationMs)

    /**
     * Record navigation duration in milliseconds
     *
     * @param durationMs Duration of navigation operation
     */
    fun recordNavigation(durationMs: Long) = record("navigation_ms", durationMs)

    /**
     * Record element processing duration in milliseconds
     *
     * @param durationMs Duration of element processing operation
     */
    fun recordElementProcessing(durationMs: Long) = record("element_processing_ms", durationMs)

    /**
     * Record safety check duration in milliseconds
     *
     * @param durationMs Duration of safety check operation
     */
    fun recordSafetyCheck(durationMs: Long) = record("safety_check_ms", durationMs)

    /**
     * Record export duration in milliseconds
     *
     * @param durationMs Duration of export operation
     */
    fun recordExport(durationMs: Long) = record("export_ms", durationMs)

    // ========================================
    // Core Operations (Private)
    // ========================================

    /**
     * Increment a counter by name
     * Thread-safe atomic increment
     *
     * @param name Counter name
     */
    private fun increment(name: String) {
        counters.getOrPut(name) { AtomicLong(0) }.incrementAndGet()
    }

    /**
     * Record a value to a histogram
     * Thread-safe synchronized list append
     *
     * @param name Histogram name
     * @param value Value to record
     */
    private fun record(name: String, value: Long) {
        val histogram = histograms.getOrPut(name) {
            mutableListOf<Long>().apply {
                // Pre-allocate reasonable capacity to reduce resizing
                if (this is ArrayList) {
                    this.ensureCapacity(1000)
                }
            }
        }

        synchronized(histogram) {
            histogram.add(value)
        }
    }

    // ========================================
    // Reporting
    // ========================================

    /**
     * Generate a comprehensive metrics report
     *
     * Calculates histogram statistics including percentiles (p50, p95, p99)
     * for all recorded metrics.
     *
     * @return MetricsReport containing all counters and histogram statistics
     */
    fun getReport(): MetricsReport {
        // Snapshot counters
        val counterSnapshot = counters.mapValues { it.value.get() }

        // Calculate histogram statistics
        val histogramStats = histograms.mapValues { (_, values) ->
            synchronized(values) {
                if (values.isEmpty()) {
                    HistogramStats(
                        count = 0,
                        min = 0,
                        max = 0,
                        avg = 0.0,
                        p50 = 0,
                        p95 = 0,
                        p99 = 0
                    )
                } else {
                    // Sort values for percentile calculation
                    val sorted = values.sorted()
                    val count = sorted.size

                    HistogramStats(
                        count = count,
                        min = sorted.first(),
                        max = sorted.last(),
                        avg = sorted.average(),
                        p50 = percentile(sorted, 50.0),
                        p95 = percentile(sorted, 95.0),
                        p99 = percentile(sorted, 99.0)
                    )
                }
            }
        }

        return MetricsReport(
            counters = counterSnapshot,
            histograms = histogramStats
        )
    }

    /**
     * Calculate percentile from sorted list
     *
     * Uses linear interpolation between values for accurate percentile calculation.
     *
     * @param sortedValues List of values in ascending order
     * @param percentile Percentile to calculate (0.0 - 100.0)
     * @return Percentile value
     */
    private fun percentile(sortedValues: List<Long>, percentile: Double): Long {
        if (sortedValues.isEmpty()) return 0
        if (sortedValues.size == 1) return sortedValues[0]

        val index = (percentile / 100.0) * (sortedValues.size - 1)
        val lower = index.toInt()
        val upper = lower + 1

        if (upper >= sortedValues.size) {
            return sortedValues.last()
        }

        // Linear interpolation
        val weight = index - lower
        val lowerValue = sortedValues[lower]
        val upperValue = sortedValues[upper]

        return (lowerValue + weight * (upperValue - lowerValue)).toLong()
    }

    /**
     * Reset all metrics
     *
     * Clears all counters and histogram data. Use this to start fresh
     * measurements for a new exploration session.
     */
    fun reset() {
        counters.clear()
        histograms.clear()
    }

    /**
     * Get current counter value
     *
     * @param name Counter name
     * @return Current counter value, or 0 if not found
     */
    fun getCounter(name: String): Long {
        return counters[name]?.get() ?: 0
    }

    /**
     * Get histogram sample count
     *
     * @param name Histogram name
     * @return Number of samples recorded, or 0 if not found
     */
    fun getHistogramSampleCount(name: String): Int {
        val histogram = histograms[name] ?: return 0
        synchronized(histogram) {
            return histogram.size
        }
    }

    /**
     * Get all counter names
     *
     * @return Set of all registered counter names
     */
    fun getCounterNames(): Set<String> = counters.keys.toSet()

    /**
     * Get all histogram names
     *
     * @return Set of all registered histogram names
     */
    fun getHistogramNames(): Set<String> = histograms.keys.toSet()
}

/**
 * MetricsReport - Snapshot of all metrics at a point in time
 *
 * @property counters Map of counter names to their current values
 * @property histograms Map of histogram names to their calculated statistics
 * @property timestamp Unix timestamp (milliseconds) when report was generated
 */
data class MetricsReport(
    val counters: Map<String, Long>,
    val histograms: Map<String, HistogramStats>,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Format report as human-readable string
     */
    override fun toString(): String = buildString {
        appendLine("=== Exploration Metrics Report ===")
        appendLine("Timestamp: ${java.time.Instant.ofEpochMilli(timestamp)}")
        appendLine()

        if (counters.isNotEmpty()) {
            appendLine("Counters:")
            counters.toSortedMap().forEach { (name, value) ->
                appendLine("  $name: $value")
            }
            appendLine()
        }

        if (histograms.isNotEmpty()) {
            appendLine("Histograms:")
            histograms.toSortedMap().forEach { (name, stats) ->
                appendLine("  $name:")
                appendLine("    count: ${stats.count}")
                appendLine("    min: ${stats.min}ms")
                appendLine("    avg: ${"%.2f".format(stats.avg)}ms")
                appendLine("    p50: ${stats.p50}ms")
                appendLine("    p95: ${stats.p95}ms")
                appendLine("    p99: ${stats.p99}ms")
                appendLine("    max: ${stats.max}ms")
            }
        }
    }
}

/**
 * HistogramStats - Statistical summary of histogram data
 *
 * @property count Number of samples recorded
 * @property min Minimum value
 * @property max Maximum value
 * @property avg Average (mean) value
 * @property p50 50th percentile (median)
 * @property p95 95th percentile
 * @property p99 99th percentile
 */
data class HistogramStats(
    val count: Int,
    val min: Long,
    val max: Long,
    val avg: Double,
    val p50: Long,
    val p95: Long,
    val p99: Long
) {
    /**
     * Format statistics as human-readable string
     */
    override fun toString(): String {
        return "HistogramStats(count=$count, min=${min}ms, avg=${"%.2f".format(avg)}ms, " +
               "p50=${p50}ms, p95=${p95}ms, p99=${p99}ms, max=${max}ms)"
    }
}
