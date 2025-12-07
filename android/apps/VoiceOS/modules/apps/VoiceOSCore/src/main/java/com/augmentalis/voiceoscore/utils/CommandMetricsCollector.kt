/**
 * CommandMetricsCollector.kt - Command execution metrics and observability
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (AI Assistant)
 * Created: 2025-11-09
 * Phase: 3 (Medium Priority)
 * Issue: Command metrics collection for observability
 */
package com.augmentalis.voiceoscore.utils

import android.util.Log
import com.augmentalis.voiceos.constants.VoiceOSConstants
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe metrics collector for voice command execution tracking
 *
 * Features:
 * - Success/failure rate tracking
 * - Execution time metrics (min, max, average, p95)
 * - Popular command identification
 * - Error pattern analysis
 * - Time-window aggregation
 * - Memory-bounded storage
 *
 * Usage:
 * ```kotlin
 * // Record command execution
 * val startTime = System.currentTimeMillis()
 * val result = executeCommand(command)
 * metricsCollector.recordCommand(
 *     command = "tap button",
 *     success = result.isSuccess,
 *     durationMs = System.currentTimeMillis() - startTime,
 *     errorType = if (!result.isSuccess) "TIMEOUT" else null
 * )
 *
 * // Get metrics summary
 * val summary = metricsCollector.getSummary()
 * Log.i(TAG, "Success rate: ${summary.successRate}%")
 * Log.i(TAG, "Average time: ${summary.avgExecutionTimeMs}ms")
 * Log.i(TAG, "Top commands: ${summary.topCommands.joinToString()}")
 * ```
 *
 * Thread Safety: All methods are thread-safe using ConcurrentHashMap and atomic operations
 */
class CommandMetricsCollector(
    private val maxCommandTracking: Int = VoiceOSConstants.Metrics.MAX_METRICS_COMMANDS,
    private val windowDurationMs: Long = VoiceOSConstants.Metrics.METRICS_WINDOW_DURATION_MS
) {
    private val TAG = "CommandMetricsCollector"

    // Global counters
    private val totalCommands = AtomicLong(0)
    private val successfulCommands = AtomicLong(0)
    private val failedCommands = AtomicLong(0)

    // Per-command metrics
    private val commandStats = ConcurrentHashMap<String, CommandStats>()

    // Error tracking
    private val errorCounts = ConcurrentHashMap<String, AtomicInteger>()

    // Time window tracking
    private val mutex = Mutex()
    private var windowStartTime = System.currentTimeMillis()
    private var windowCommandCount = 0L

    /**
     * Record a command execution
     *
     * @param command The command that was executed
     * @param success Whether the command succeeded
     * @param durationMs Execution time in milliseconds
     * @param errorType Optional error type if command failed
     */
    fun recordCommand(
        command: String,
        success: Boolean,
        durationMs: Long,
        errorType: String? = null
    ) {
        // Update global counters
        totalCommands.incrementAndGet()
        if (success) {
            successfulCommands.incrementAndGet()
        } else {
            failedCommands.incrementAndGet()
            errorType?.let { recordError(it) }
        }

        // Update command-specific stats
        val stats = commandStats.computeIfAbsent(command) { CommandStats(command) }
        stats.recordExecution(success, durationMs)

        // Check if we need to evict old commands
        if (commandStats.size > maxCommandTracking) {
            evictLeastUsedCommand()
        }
    }

    /**
     * Record an error occurrence
     */
    private fun recordError(errorType: String) {
        errorCounts.computeIfAbsent(errorType) { AtomicInteger(0) }
            .incrementAndGet()
    }

    /**
     * Get comprehensive metrics summary
     */
    fun getSummary(): MetricsSummary {
        val total = totalCommands.get()
        val successful = successfulCommands.get()
        val failed = failedCommands.get()

        val successRate = if (total > 0) {
            (successful.toDouble() / total.toDouble() * 100.0)
        } else {
            0.0
        }

        // Calculate aggregate timing statistics
        val allExecutionTimes = commandStats.values.flatMap { it.getExecutionTimes() }
        val avgTime = if (allExecutionTimes.isNotEmpty()) {
            allExecutionTimes.average()
        } else {
            0.0
        }
        val maxTime = allExecutionTimes.maxOrNull() ?: 0L
        val minTime = allExecutionTimes.minOrNull() ?: 0L
        val p95Time = calculatePercentile(allExecutionTimes, 95.0)

        // Top commands by usage
        val topCommands = commandStats.values
            .sortedByDescending { it.getTotalExecutions() }
            .take(10)
            .map { it.command to it.getTotalExecutions() }

        // Top errors
        val topErrors = errorCounts.entries
            .sortedByDescending { it.value.get() }
            .take(10)
            .map { it.key to it.value.get() }

        return MetricsSummary(
            totalCommands = total,
            successfulCommands = successful,
            failedCommands = failed,
            successRate = successRate,
            avgExecutionTimeMs = avgTime,
            minExecutionTimeMs = minTime,
            maxExecutionTimeMs = maxTime,
            p95ExecutionTimeMs = p95Time,
            topCommands = topCommands,
            topErrors = topErrors,
            uniqueCommandsTracked = commandStats.size
        )
    }

    /**
     * Get metrics for a specific command
     */
    fun getCommandMetrics(command: String): CommandMetrics? {
        val stats = commandStats[command] ?: return null

        val total = stats.getTotalExecutions()
        val successful = stats.getSuccessfulExecutions()
        val failed = stats.getFailedExecutions()

        val successRate = if (total > 0) {
            (successful.toDouble() / total.toDouble() * 100.0)
        } else {
            0.0
        }

        val times = stats.getExecutionTimes()
        val avgTime = if (times.isNotEmpty()) times.average() else 0.0
        val maxTime = times.maxOrNull() ?: 0L
        val minTime = times.minOrNull() ?: 0L

        return CommandMetrics(
            command = command,
            totalExecutions = total,
            successfulExecutions = successful,
            failedExecutions = failed,
            successRate = successRate,
            avgExecutionTimeMs = avgTime,
            minExecutionTimeMs = minTime,
            maxExecutionTimeMs = maxTime
        )
    }

    /**
     * Get current throughput (commands per second)
     */
    suspend fun getThroughput(): Double = mutex.withLock {
        val elapsedMs = System.currentTimeMillis() - windowStartTime
        if (elapsedMs > 0) {
            (windowCommandCount.toDouble() / elapsedMs.toDouble()) * 1000.0
        } else {
            0.0
        }
    }

    /**
     * Reset metrics window
     */
    suspend fun resetWindow() = mutex.withLock {
        windowStartTime = System.currentTimeMillis()
        windowCommandCount = totalCommands.get()
    }

    /**
     * Clear all metrics
     */
    fun reset() {
        totalCommands.set(0)
        successfulCommands.set(0)
        failedCommands.set(0)
        commandStats.clear()
        errorCounts.clear()
    }

    /**
     * Export metrics to JSON-compatible map
     */
    fun exportMetrics(): Map<String, Any> {
        val summary = getSummary()
        return mapOf(
            "timestamp" to System.currentTimeMillis(),
            "totalCommands" to summary.totalCommands,
            "successfulCommands" to summary.successfulCommands,
            "failedCommands" to summary.failedCommands,
            "successRate" to summary.successRate,
            "avgExecutionTimeMs" to summary.avgExecutionTimeMs,
            "maxExecutionTimeMs" to summary.maxExecutionTimeMs,
            "p95ExecutionTimeMs" to summary.p95ExecutionTimeMs,
            "topCommands" to summary.topCommands.map { mapOf("command" to it.first, "count" to it.second) },
            "topErrors" to summary.topErrors.map { mapOf("error" to it.first, "count" to it.second) },
            "uniqueCommands" to summary.uniqueCommandsTracked
        )
    }

    /**
     * Evict least-used command to maintain memory bounds
     */
    private fun evictLeastUsedCommand() {
        val leastUsed = commandStats.values
            .minByOrNull { it.getTotalExecutions() }
            ?.command

        leastUsed?.let {
            commandStats.remove(it)
            Log.d(TAG, "Evicted least-used command: $it")
        }
    }

    /**
     * Calculate percentile from sorted list
     */
    private fun calculatePercentile(times: List<Long>, percentile: Double): Long {
        if (times.isEmpty()) return 0L

        val sorted = times.sorted()
        val index = ((percentile / 100.0) * sorted.size).toInt()
            .coerceIn(0, sorted.size - 1)

        return sorted[index]
    }

    /**
     * Thread-safe per-command statistics
     */
    private class CommandStats(val command: String) {
        private val totalExecutions = AtomicLong(0)
        private val successfulExecutions = AtomicLong(0)
        private val failedExecutions = AtomicLong(0)

        // Store recent execution times (bounded)
        private val executionTimes = ConcurrentHashMap<Long, Long>() // timestamp -> duration
        private val maxStoredTimes = 1000 // Keep last 1000 executions

        fun recordExecution(success: Boolean, durationMs: Long) {
            totalExecutions.incrementAndGet()
            if (success) {
                successfulExecutions.incrementAndGet()
            } else {
                failedExecutions.incrementAndGet()
            }

            // Store execution time
            val timestamp = System.currentTimeMillis()
            executionTimes[timestamp] = durationMs

            // Evict old times if over limit
            if (executionTimes.size > maxStoredTimes) {
                val oldestKey = executionTimes.keys.minOrNull()
                oldestKey?.let { executionTimes.remove(it) }
            }
        }

        fun getTotalExecutions(): Long = totalExecutions.get()
        fun getSuccessfulExecutions(): Long = successfulExecutions.get()
        fun getFailedExecutions(): Long = failedExecutions.get()
        fun getExecutionTimes(): List<Long> = executionTimes.values.toList()
    }
}

/**
 * Comprehensive metrics summary
 */
data class MetricsSummary(
    val totalCommands: Long,
    val successfulCommands: Long,
    val failedCommands: Long,
    val successRate: Double,
    val avgExecutionTimeMs: Double,
    val minExecutionTimeMs: Long,
    val maxExecutionTimeMs: Long,
    val p95ExecutionTimeMs: Long,
    val topCommands: List<Pair<String, Long>>,
    val topErrors: List<Pair<String, Int>>,
    val uniqueCommandsTracked: Int
)

/**
 * Per-command metrics
 */
data class CommandMetrics(
    val command: String,
    val totalExecutions: Long,
    val successfulExecutions: Long,
    val failedExecutions: Long,
    val successRate: Double,
    val avgExecutionTimeMs: Double,
    val minExecutionTimeMs: Long,
    val maxExecutionTimeMs: Long
)
