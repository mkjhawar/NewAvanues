/**
 * IMetricsCollector.kt - Metrics collection interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Interface for command execution metrics collection.
 * Enables dependency injection and no-op implementations for testing.
 */
package com.augmentalis.commandmanager

/**
 * Interface for command execution metrics collection.
 * Enables dependency injection and no-op implementations for testing.
 */
interface IMetricsCollector {
    /**
     * Record a command result.
     *
     * @param result The command result to record
     */
    suspend fun record(result: CommandResult)

    /**
     * Get metrics summary.
     *
     * @return Overall metrics summary
     */
    fun getSummary(): MetricsSummary

    /**
     * Get metrics for a specific command.
     *
     * @param phrase The command phrase to get metrics for
     * @return Command-specific metrics, or null if not found
     */
    fun getMetricsForCommand(phrase: String): CommandMetricSummary?

    /**
     * Reset all metrics.
     */
    fun reset()

    /**
     * Get debug information.
     *
     * @return Debug information string
     */
    fun getDebugInfo(): String
}

/**
 * No-op metrics collector for testing or performance-critical scenarios.
 * All operations are no-ops and return empty/default values.
 */
object NoOpMetricsCollector : IMetricsCollector {
    override suspend fun record(result: CommandResult) {
        // no-op
    }

    override fun getSummary(): MetricsSummary = MetricsSummary(
        totalCommands = 0,
        successfulCommands = 0,
        failedCommands = 0,
        successRate = 0f,
        averageDurationMs = 0,
        totalDurationMs = 0,
        uniqueCommands = 0,
        topCommands = emptyList(),
        slowestCommands = emptyList()
    )

    override fun getMetricsForCommand(phrase: String): CommandMetricSummary? = null

    override fun reset() {
        // no-op
    }

    override fun getDebugInfo(): String = "NoOpMetricsCollector (metrics disabled)"
}
