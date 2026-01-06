/**
 * MetricsCollector.kt - Performance metrics collection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP metrics collector for tracking command execution performance.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.handlers.HandlerResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Collects and aggregates performance metrics for command execution.
 */
class MetricsCollector {

    private val mutex = Mutex()

    // Per-command metrics
    private val commandMetrics = mutableMapOf<String, CommandMetric>()

    // Global metrics
    private var totalCommands: Long = 0
    private var successfulCommands: Long = 0
    private var failedCommands: Long = 0
    private var totalDurationMs: Long = 0

    /**
     * Record a command result.
     */
    suspend fun record(result: CommandResult) {
        mutex.withLock {
            val phrase = result.command.phrase

            // Update command-specific metrics
            val metric = commandMetrics.getOrPut(phrase) { CommandMetric(phrase) }
            metric.record(result)

            // Update global metrics
            totalCommands++
            totalDurationMs += result.durationMs

            when (result.result) {
                is HandlerResult.Success -> successfulCommands++
                is HandlerResult.Failure -> failedCommands++
                else -> { /* InProgress, RequiresInput, NotHandled */ }
            }
        }
    }

    /**
     * Get metrics summary.
     */
    fun getSummary(): MetricsSummary {
        return MetricsSummary(
            totalCommands = totalCommands,
            successfulCommands = successfulCommands,
            failedCommands = failedCommands,
            successRate = if (totalCommands > 0) successfulCommands.toFloat() / totalCommands else 0f,
            averageDurationMs = if (totalCommands > 0) totalDurationMs / totalCommands else 0,
            totalDurationMs = totalDurationMs,
            uniqueCommands = commandMetrics.size,
            topCommands = getTopCommands(10),
            slowestCommands = getSlowestCommands(5)
        )
    }

    /**
     * Get top N most used commands.
     */
    private fun getTopCommands(n: Int): List<CommandMetricSummary> {
        return commandMetrics.values
            .sortedByDescending { it.count }
            .take(n)
            .map { it.toSummary() }
    }

    /**
     * Get slowest N commands by average duration.
     */
    private fun getSlowestCommands(n: Int): List<CommandMetricSummary> {
        return commandMetrics.values
            .filter { it.count > 0 }
            .sortedByDescending { it.averageDurationMs }
            .take(n)
            .map { it.toSummary() }
    }

    /**
     * Get metrics for a specific command.
     */
    fun getMetricsForCommand(phrase: String): CommandMetricSummary? {
        return commandMetrics[phrase]?.toSummary()
    }

    /**
     * Reset all metrics.
     */
    fun reset() {
        commandMetrics.clear()
        totalCommands = 0
        successfulCommands = 0
        failedCommands = 0
        totalDurationMs = 0
    }

    /**
     * Get debug information.
     */
    fun getDebugInfo(): String {
        val summary = getSummary()
        return buildString {
            appendLine("MetricsCollector Debug Info")
            appendLine("Total commands: ${summary.totalCommands}")
            appendLine("Successful: ${summary.successfulCommands} (${(summary.successRate * 100).toInt()}%)")
            appendLine("Failed: ${summary.failedCommands}")
            appendLine("Average duration: ${summary.averageDurationMs}ms")
            appendLine("Unique commands: ${summary.uniqueCommands}")
            appendLine()
            appendLine("Top commands:")
            summary.topCommands.forEach { cmd ->
                appendLine("  - ${cmd.phrase}: ${cmd.count} calls, ${cmd.averageDurationMs}ms avg")
            }
        }
    }
}

/**
 * Per-command metrics.
 */
private class CommandMetric(val phrase: String) {
    var count: Long = 0
        private set
    var successCount: Long = 0
        private set
    var failureCount: Long = 0
        private set
    var totalDurationMs: Long = 0
        private set
    var minDurationMs: Long = Long.MAX_VALUE
        private set
    var maxDurationMs: Long = 0
        private set
    var lastDurationMs: Long = 0
        private set
    var lastTimestamp: Long = 0
        private set

    val averageDurationMs: Long
        get() = if (count > 0) totalDurationMs / count else 0

    val successRate: Float
        get() = if (count > 0) successCount.toFloat() / count else 0f

    fun record(result: CommandResult) {
        count++
        totalDurationMs += result.durationMs
        lastDurationMs = result.durationMs
        lastTimestamp = result.timestamp

        if (result.durationMs < minDurationMs) minDurationMs = result.durationMs
        if (result.durationMs > maxDurationMs) maxDurationMs = result.durationMs

        when (result.result) {
            is HandlerResult.Success -> successCount++
            is HandlerResult.Failure -> failureCount++
            else -> { /* Other result types */ }
        }
    }

    fun toSummary(): CommandMetricSummary = CommandMetricSummary(
        phrase = phrase,
        count = count,
        successCount = successCount,
        failureCount = failureCount,
        successRate = successRate,
        averageDurationMs = averageDurationMs,
        minDurationMs = if (minDurationMs != Long.MAX_VALUE) minDurationMs else 0,
        maxDurationMs = maxDurationMs,
        lastDurationMs = lastDurationMs,
        lastTimestamp = lastTimestamp
    )
}

/**
 * Summary of metrics for a single command.
 */
data class CommandMetricSummary(
    val phrase: String,
    val count: Long,
    val successCount: Long,
    val failureCount: Long,
    val successRate: Float,
    val averageDurationMs: Long,
    val minDurationMs: Long,
    val maxDurationMs: Long,
    val lastDurationMs: Long,
    val lastTimestamp: Long
)

/**
 * Overall metrics summary.
 */
data class MetricsSummary(
    val totalCommands: Long,
    val successfulCommands: Long,
    val failedCommands: Long,
    val successRate: Float,
    val averageDurationMs: Long,
    val totalDurationMs: Long,
    val uniqueCommands: Int,
    val topCommands: List<CommandMetricSummary>,
    val slowestCommands: List<CommandMetricSummary>
)
