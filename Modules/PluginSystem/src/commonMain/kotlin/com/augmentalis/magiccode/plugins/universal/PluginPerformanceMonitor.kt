/**
 * PluginPerformanceMonitor.kt - Performance monitoring for the plugin system
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Provides performance monitoring, timing instrumentation, and metrics
 * collection for the Universal Plugin Architecture.
 */
package com.augmentalis.magiccode.plugins.universal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

/**
 * Performance monitor for plugin operations.
 *
 * Tracks timing, success rates, and resource usage for plugin operations.
 * Provides real-time metrics and historical data for optimization.
 *
 * ## Usage
 * ```kotlin
 * val monitor = PluginPerformanceMonitor()
 *
 * // Time an operation
 * monitor.time("command_dispatch") {
 *     dispatcher.dispatch(command, context)
 * }
 *
 * // Record a metric
 * monitor.recordLatency("plugin_init", initTimeMs)
 *
 * // Get current metrics
 * val metrics = monitor.metrics.value
 * ```
 */
class PluginPerformanceMonitor {

    /**
     * Performance metrics snapshot.
     */
    data class PerformanceMetrics(
        val operationMetrics: Map<String, OperationMetrics> = emptyMap(),
        val pluginMetrics: Map<String, PluginMetrics> = emptyMap(),
        val systemMetrics: SystemMetrics = SystemMetrics()
    )

    /**
     * Metrics for a specific operation type.
     */
    data class OperationMetrics(
        val operationName: String,
        val count: Long = 0,
        val successCount: Long = 0,
        val failureCount: Long = 0,
        val totalTimeMs: Long = 0,
        val minTimeMs: Long = Long.MAX_VALUE,
        val maxTimeMs: Long = 0,
        val avgTimeMs: Double = 0.0,
        val lastTimeMs: Long = 0,
        val percentile95Ms: Long = 0
    ) {
        val successRate: Float
            get() = if (count > 0) (successCount.toFloat() / count) * 100 else 0f
    }

    /**
     * Metrics for a specific plugin.
     */
    data class PluginMetrics(
        val pluginId: String,
        val commandsHandled: Long = 0,
        val commandsSucceeded: Long = 0,
        val totalExecutionTimeMs: Long = 0,
        val avgExecutionTimeMs: Double = 0.0,
        val lastExecutionTimeMs: Long = 0,
        val initTimeMs: Long = 0,
        val isActive: Boolean = true
    )

    /**
     * System-wide metrics.
     */
    data class SystemMetrics(
        val totalCommandsProcessed: Long = 0,
        val pluginsLoaded: Int = 0,
        val pluginsActive: Int = 0,
        val systemStartTimeMs: Long = getCurrentTimeMs(),
        val uptimeMs: Long = 0,
        val peakLatencyMs: Long = 0
    )

    // Internal state
    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()

    // Latency samples for percentile calculation
    private val latencySamples = mutableMapOf<String, MutableList<Long>>()
    private val maxSamplesPerOperation = 1000

    // Plugin metrics
    private val pluginMetricsMap = mutableMapOf<String, PluginMetrics>()

    /**
     * Time an operation and record its metrics.
     *
     * @param operationName Name of the operation being timed
     * @param success Whether the operation succeeded
     * @param block The operation to time
     * @return Result of the operation
     */
    inline fun <T> time(
        operationName: String,
        success: Boolean = true,
        block: () -> T
    ): T {
        val startTime = getCurrentTimeMs()
        return try {
            block().also {
                recordOperation(operationName, getCurrentTimeMs() - startTime, true)
            }
        } catch (e: Exception) {
            recordOperation(operationName, getCurrentTimeMs() - startTime, false)
            throw e
        }
    }

    /**
     * Time a suspending operation and record its metrics.
     *
     * @param operationName Name of the operation being timed
     * @param block The suspending operation to time
     * @return Result of the operation
     */
    suspend inline fun <T> timeSuspend(
        operationName: String,
        block: () -> T
    ): T {
        val startTime = getCurrentTimeMs()
        return try {
            block().also {
                recordOperation(operationName, getCurrentTimeMs() - startTime, true)
            }
        } catch (e: Exception) {
            recordOperation(operationName, getCurrentTimeMs() - startTime, false)
            throw e
        }
    }

    /**
     * Record a latency measurement for an operation.
     *
     * @param operationName Name of the operation
     * @param latencyMs Latency in milliseconds
     * @param success Whether the operation succeeded
     */
    fun recordOperation(operationName: String, latencyMs: Long, success: Boolean) {
        val current = _metrics.value
        val existing = current.operationMetrics[operationName] ?: OperationMetrics(operationName)

        // Update latency samples
        val samples = latencySamples.getOrPut(operationName) { mutableListOf() }
        samples.add(latencyMs)
        if (samples.size > maxSamplesPerOperation) {
            samples.removeAt(0)
        }

        // Calculate percentile
        val p95 = calculatePercentile(samples, 95)

        val updated = existing.copy(
            count = existing.count + 1,
            successCount = if (success) existing.successCount + 1 else existing.successCount,
            failureCount = if (!success) existing.failureCount + 1 else existing.failureCount,
            totalTimeMs = existing.totalTimeMs + latencyMs,
            minTimeMs = min(existing.minTimeMs, latencyMs),
            maxTimeMs = max(existing.maxTimeMs, latencyMs),
            avgTimeMs = (existing.totalTimeMs + latencyMs).toDouble() / (existing.count + 1),
            lastTimeMs = latencyMs,
            percentile95Ms = p95
        )

        _metrics.value = current.copy(
            operationMetrics = current.operationMetrics + (operationName to updated),
            systemMetrics = current.systemMetrics.copy(
                totalCommandsProcessed = current.systemMetrics.totalCommandsProcessed + 1,
                peakLatencyMs = max(current.systemMetrics.peakLatencyMs, latencyMs),
                uptimeMs = getCurrentTimeMs() - current.systemMetrics.systemStartTimeMs
            )
        )
    }

    /**
     * Record plugin execution metrics.
     *
     * @param pluginId The plugin identifier
     * @param executionTimeMs Execution time in milliseconds
     * @param success Whether execution succeeded
     */
    fun recordPluginExecution(pluginId: String, executionTimeMs: Long, success: Boolean) {
        val existing = pluginMetricsMap[pluginId] ?: PluginMetrics(pluginId)

        val updated = existing.copy(
            commandsHandled = existing.commandsHandled + 1,
            commandsSucceeded = if (success) existing.commandsSucceeded + 1 else existing.commandsSucceeded,
            totalExecutionTimeMs = existing.totalExecutionTimeMs + executionTimeMs,
            avgExecutionTimeMs = (existing.totalExecutionTimeMs + executionTimeMs).toDouble() /
                    (existing.commandsHandled + 1),
            lastExecutionTimeMs = executionTimeMs
        )

        pluginMetricsMap[pluginId] = updated
        updatePluginMetrics()
    }

    /**
     * Record plugin initialization time.
     *
     * @param pluginId The plugin identifier
     * @param initTimeMs Initialization time in milliseconds
     */
    fun recordPluginInit(pluginId: String, initTimeMs: Long) {
        val existing = pluginMetricsMap[pluginId] ?: PluginMetrics(pluginId)
        pluginMetricsMap[pluginId] = existing.copy(initTimeMs = initTimeMs)
        updatePluginMetrics()
    }

    /**
     * Mark a plugin as active or inactive.
     *
     * @param pluginId The plugin identifier
     * @param active Whether the plugin is active
     */
    fun setPluginActive(pluginId: String, active: Boolean) {
        val existing = pluginMetricsMap[pluginId] ?: PluginMetrics(pluginId)
        pluginMetricsMap[pluginId] = existing.copy(isActive = active)
        updatePluginMetrics()
    }

    /**
     * Update plugin count in system metrics.
     *
     * @param total Total plugins loaded
     * @param active Active plugins count
     */
    fun updatePluginCounts(total: Int, active: Int) {
        val current = _metrics.value
        _metrics.value = current.copy(
            systemMetrics = current.systemMetrics.copy(
                pluginsLoaded = total,
                pluginsActive = active
            )
        )
    }

    /**
     * Get metrics for a specific operation.
     *
     * @param operationName Name of the operation
     * @return Operation metrics or null if not found
     */
    fun getOperationMetrics(operationName: String): OperationMetrics? {
        return _metrics.value.operationMetrics[operationName]
    }

    /**
     * Get metrics for a specific plugin.
     *
     * @param pluginId The plugin identifier
     * @return Plugin metrics or null if not found
     */
    fun getPluginMetrics(pluginId: String): PluginMetrics? {
        return pluginMetricsMap[pluginId]
    }

    /**
     * Get a summary report of current performance.
     *
     * @return Formatted summary string
     */
    fun getSummaryReport(): String {
        val current = _metrics.value
        val sb = StringBuilder()

        sb.appendLine("=== Plugin System Performance Report ===")
        sb.appendLine()

        // System metrics
        sb.appendLine("System Metrics:")
        sb.appendLine("  Total Commands: ${current.systemMetrics.totalCommandsProcessed}")
        sb.appendLine("  Plugins Loaded: ${current.systemMetrics.pluginsLoaded}")
        sb.appendLine("  Plugins Active: ${current.systemMetrics.pluginsActive}")
        sb.appendLine("  Peak Latency: ${current.systemMetrics.peakLatencyMs}ms")
        sb.appendLine("  Uptime: ${current.systemMetrics.uptimeMs / 1000}s")
        sb.appendLine()

        // Operation metrics
        if (current.operationMetrics.isNotEmpty()) {
            sb.appendLine("Operation Metrics:")
            current.operationMetrics.forEach { (name, metrics) ->
                sb.appendLine("  $name:")
                sb.appendLine("    Count: ${metrics.count} (${metrics.successRate}% success)")
                sb.appendLine("    Avg: ${metrics.avgTimeMs.toInt()}ms | Min: ${metrics.minTimeMs}ms | Max: ${metrics.maxTimeMs}ms")
                sb.appendLine("    P95: ${metrics.percentile95Ms}ms")
            }
            sb.appendLine()
        }

        // Plugin metrics
        if (pluginMetricsMap.isNotEmpty()) {
            sb.appendLine("Plugin Metrics:")
            pluginMetricsMap.forEach { (id, metrics) ->
                val shortId = id.substringAfterLast(".")
                sb.appendLine("  $shortId:")
                sb.appendLine("    Commands: ${metrics.commandsHandled} handled")
                sb.appendLine("    Avg Exec: ${metrics.avgExecutionTimeMs.toInt()}ms")
                sb.appendLine("    Init: ${metrics.initTimeMs}ms")
            }
        }

        return sb.toString()
    }

    /**
     * Reset all metrics.
     */
    fun reset() {
        latencySamples.clear()
        pluginMetricsMap.clear()
        _metrics.value = PerformanceMetrics()
    }

    private fun updatePluginMetrics() {
        val current = _metrics.value
        _metrics.value = current.copy(
            pluginMetrics = pluginMetricsMap.toMap()
        )
    }

    private fun calculatePercentile(samples: List<Long>, percentile: Int): Long {
        if (samples.isEmpty()) return 0
        val sorted = samples.sorted()
        val index = ((percentile / 100.0) * sorted.size).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }

    companion object {
        // Singleton instance for global access
        private var _instance: PluginPerformanceMonitor? = null

        val instance: PluginPerformanceMonitor
            get() = _instance ?: PluginPerformanceMonitor().also { _instance = it }

        fun initialize(): PluginPerformanceMonitor {
            _instance = PluginPerformanceMonitor()
            return _instance!!
        }
    }
}

// Platform-specific time function (to be implemented per platform)
expect fun getCurrentTimeMs(): Long
