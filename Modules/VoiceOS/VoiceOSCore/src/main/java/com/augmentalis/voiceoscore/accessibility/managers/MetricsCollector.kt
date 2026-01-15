/**
 * MetricsCollector.kt - Performance metrics tracking for action execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-17
 *
 * Purpose: Single Responsibility - Collect and manage performance metrics
 */
package com.augmentalis.voiceoscore.accessibility.managers

import java.util.concurrent.ConcurrentHashMap

/**
 * Collects and tracks performance metrics for action execution
 * Follows Single Responsibility Principle - only handles metrics
 */
class MetricsCollector {

    private val metrics = ConcurrentHashMap<String, MetricData>()

    /**
     * Data class for storing metric information
     */
    data class MetricData(
        var count: Long = 0,
        var totalTimeMs: Long = 0,
        var successCount: Long = 0,
        var lastExecutionMs: Long = 0
    ) {
        val averageTimeMs: Long
            get() = if (count > 0) totalTimeMs / count else 0

        val successRate: Float
            get() = if (count > 0) successCount.toFloat() / count else 0f
    }

    /**
     * Record a metric for an action execution
     *
     * @param action The action that was executed
     * @param timeMs Time taken to execute in milliseconds
     * @param success Whether the execution was successful
     */
    fun recordMetric(action: String, timeMs: Long, success: Boolean) {
        metrics.getOrPut(action) { MetricData() }.apply {
            count++
            totalTimeMs += timeMs
            lastExecutionMs = timeMs
            if (success) successCount++
        }
    }

    /**
     * Get all metrics
     *
     * @return Immutable map of all metrics
     */
    fun getMetrics(): Map<String, MetricData> {
        return metrics.toMap()
    }

    /**
     * Get metrics for a specific action
     *
     * @param action The action to get metrics for
     * @return Metrics for the action, or null if no metrics exist
     */
    fun getMetricsForAction(action: String): MetricData? {
        return metrics[action]
    }

    /**
     * Clear all metrics
     */
    fun clearMetrics() {
        metrics.clear()
    }

    /**
     * Get summary statistics
     *
     * @return String containing summary of metrics
     */
    fun getSummary(): String {
        return buildString {
            appendLine("Metrics: ${metrics.size} actions tracked")
            metrics.entries.take(10).forEach { (action, data) ->
                appendLine(
                    "  - $action: ${data.count} calls, ${data.averageTimeMs}ms avg, " +
                            "${(data.successRate * 100).toInt()}% success"
                )
            }
        }
    }

    /**
     * Get total number of tracked actions
     */
    fun getTrackedActionCount(): Int = metrics.size

    /**
     * Get total number of executions across all actions
     */
    fun getTotalExecutionCount(): Long = metrics.values.sumOf { it.count }

    /**
     * Get overall success rate
     */
    fun getOverallSuccessRate(): Float {
        val totalCount = metrics.values.sumOf { it.count }
        val totalSuccess = metrics.values.sumOf { it.successCount }
        return if (totalCount > 0) totalSuccess.toFloat() / totalCount else 0f
    }

    /**
     * Get average execution time across all actions
     */
    fun getOverallAverageTimeMs(): Long {
        val totalTime = metrics.values.sumOf { it.totalTimeMs }
        val totalCount = metrics.values.sumOf { it.count }
        return if (totalCount > 0) totalTime / totalCount else 0
    }

    /**
     * Get slowest actions
     *
     * @param limit Number of actions to return
     * @return List of action names sorted by average execution time
     */
    fun getSlowestActions(limit: Int = 5): List<Pair<String, Long>> {
        return metrics.entries
            .map { it.key to it.value.averageTimeMs }
            .sortedByDescending { it.second }
            .take(limit)
    }

    /**
     * Get most frequently executed actions
     *
     * @param limit Number of actions to return
     * @return List of action names sorted by execution count
     */
    fun getMostFrequentActions(limit: Int = 5): List<Pair<String, Long>> {
        return metrics.entries
            .map { it.key to it.value.count }
            .sortedByDescending { it.second }
            .take(limit)
    }

    /**
     * Get actions with lowest success rates
     *
     * @param limit Number of actions to return
     * @return List of action names sorted by success rate
     */
    fun getLeastSuccessfulActions(limit: Int = 5): List<Pair<String, Float>> {
        return metrics.entries
            .filter { it.value.count >= 3 } // Only consider actions executed at least 3 times
            .map { it.key to it.value.successRate }
            .sortedBy { it.second }
            .take(limit)
    }
}
