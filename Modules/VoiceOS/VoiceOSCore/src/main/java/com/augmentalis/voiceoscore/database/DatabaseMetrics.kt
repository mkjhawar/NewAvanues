/**
 * DatabaseMetrics.kt - Database operation metrics collector
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: database-foundation agent
 * Created: 2025-12-22
 *
 * Provides comprehensive metrics collection for database operations
 * to track performance, errors, and usage patterns.
 */

package com.augmentalis.voiceoscore.database

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Singleton metrics collector for database operations.
 *
 * Tracks:
 * - Operation counts (queries, inserts, updates, deletes)
 * - Error rates and types
 * - Performance metrics (execution time, throughput)
 * - Resource usage (memory, transaction count)
 *
 * ## Usage:
 * ```kotlin
 * // Track operation
 * DatabaseMetrics.trackOperation(
 *     operation = "deleteAppSpecificElements",
 *     durationMs = 150,
 *     success = true,
 *     itemCount = 1500
 * )
 *
 * // Track error
 * DatabaseMetrics.trackError(
 *     operation = "filterByApp",
 *     error = exception,
 *     context = "packageName=com.example.app"
 * )
 *
 * // Get metrics summary
 * val summary = DatabaseMetrics.getSummary()
 * Log.i(TAG, "Database metrics: $summary")
 * ```
 */
object DatabaseMetrics {
    private const val TAG = "DatabaseMetrics"

    private val mutex = Mutex()
    private val operationMetrics = mutableMapOf<String, OperationStats>()
    private val errorMetrics = mutableMapOf<String, ErrorStats>()
    private var totalOperations = 0L
    private var totalErrors = 0L
    private var totalDurationMs = 0L

    /**
     * Statistics for a specific operation type.
     */
    data class OperationStats(
        val operationName: String,
        var count: Long = 0,
        var totalDurationMs: Long = 0,
        var minDurationMs: Long = Long.MAX_VALUE,
        var maxDurationMs: Long = 0,
        var totalItems: Long = 0,
        var successCount: Long = 0,
        var failureCount: Long = 0
    ) {
        val avgDurationMs: Long
            get() = if (count > 0) totalDurationMs / count else 0

        val successRate: Double
            get() = if (count > 0) successCount.toDouble() / count else 0.0

        val avgItemsPerOperation: Long
            get() = if (count > 0) totalItems / count else 0
    }

    /**
     * Statistics for errors by operation type.
     */
    data class ErrorStats(
        val operationName: String,
        var count: Long = 0,
        val errorTypes: MutableMap<String, Long> = mutableMapOf(),
        val lastError: String? = null,
        var lastErrorTimestamp: Long = 0
    )

    /**
     * Overall metrics summary.
     */
    data class MetricsSummary(
        val totalOperations: Long,
        val totalErrors: Long,
        val totalDurationMs: Long,
        val avgDurationMs: Long,
        val errorRate: Double,
        val operationBreakdown: Map<String, OperationStats>,
        val errorBreakdown: Map<String, ErrorStats>,
        val topSlowOperations: List<Pair<String, Long>>,
        val topFailedOperations: List<Pair<String, Long>>
    )

    /**
     * Track a database operation.
     *
     * @param operation Operation name (e.g., "deleteAppSpecificElements")
     * @param durationMs Execution time in milliseconds
     * @param success Whether the operation succeeded
     * @param itemCount Number of items affected (optional)
     */
    suspend fun trackOperation(
        operation: String,
        durationMs: Long,
        success: Boolean,
        itemCount: Int = 0
    ) {
        mutex.withLock {
            totalOperations++
            totalDurationMs += durationMs

            val stats = operationMetrics.getOrPut(operation) {
                OperationStats(operation)
            }

            stats.count++
            stats.totalDurationMs += durationMs
            stats.minDurationMs = minOf(stats.minDurationMs, durationMs)
            stats.maxDurationMs = maxOf(stats.maxDurationMs, durationMs)
            stats.totalItems += itemCount

            if (success) {
                stats.successCount++
            } else {
                stats.failureCount++
            }

            // Log slow operations (>1 second)
            if (durationMs > 1000) {
                Log.w(TAG, "Slow operation: $operation took ${durationMs}ms")
            }
        }
    }

    /**
     * Track a database error.
     *
     * @param operation Operation name that failed
     * @param error Exception that occurred
     * @param context Additional context (optional)
     */
    suspend fun trackError(
        operation: String,
        error: Throwable,
        context: String? = null
    ) {
        mutex.withLock {
            totalErrors++

            val stats = errorMetrics.getOrPut(operation) {
                ErrorStats(operation)
            }

            stats.count++
            val errorType = error::class.simpleName ?: "Unknown"
            stats.errorTypes[errorType] = (stats.errorTypes[errorType] ?: 0) + 1
            stats.lastErrorTimestamp = System.currentTimeMillis()

            val errorMessage = buildString {
                append(error.message ?: errorType)
                if (context != null) {
                    append(" [")
                    append(context)
                    append("]")
                }
            }

            Log.e(TAG, "Database error in $operation: $errorMessage", error)
        }
    }

    /**
     * Get comprehensive metrics summary.
     */
    suspend fun getSummary(): MetricsSummary {
        return mutex.withLock {
            val avgDuration = if (totalOperations > 0) totalDurationMs / totalOperations else 0
            val errorRate = if (totalOperations > 0) totalErrors.toDouble() / totalOperations else 0.0

            // Top 5 slowest operations by average duration
            val topSlow = operationMetrics.values
                .sortedByDescending { it.avgDurationMs }
                .take(5)
                .map { it.operationName to it.avgDurationMs }

            // Top 5 operations by failure count
            val topFailed = operationMetrics.values
                .sortedByDescending { it.failureCount }
                .take(5)
                .map { it.operationName to it.failureCount }

            MetricsSummary(
                totalOperations = totalOperations,
                totalErrors = totalErrors,
                totalDurationMs = totalDurationMs,
                avgDurationMs = avgDuration,
                errorRate = errorRate,
                operationBreakdown = operationMetrics.toMap(),
                errorBreakdown = errorMetrics.toMap(),
                topSlowOperations = topSlow,
                topFailedOperations = topFailed
            )
        }
    }

    /**
     * Get metrics for a specific operation.
     */
    suspend fun getOperationStats(operation: String): OperationStats? {
        return mutex.withLock {
            operationMetrics[operation]
        }
    }

    /**
     * Reset all metrics.
     *
     * Use this for testing or when starting a new monitoring period.
     */
    suspend fun reset() {
        mutex.withLock {
            operationMetrics.clear()
            errorMetrics.clear()
            totalOperations = 0
            totalErrors = 0
            totalDurationMs = 0
            Log.i(TAG, "Metrics reset")
        }
    }

    /**
     * Log current metrics summary.
     */
    suspend fun logSummary() {
        val summary = getSummary()
        Log.i(TAG, buildString {
            appendLine("=== Database Metrics Summary ===")
            appendLine("Total Operations: ${summary.totalOperations}")
            appendLine("Total Errors: ${summary.totalErrors}")
            appendLine("Error Rate: ${"%.2f".format(summary.errorRate * 100)}%")
            appendLine("Total Duration: ${summary.totalDurationMs}ms")
            appendLine("Avg Duration: ${summary.avgDurationMs}ms")
            appendLine()
            appendLine("Top Slow Operations:")
            summary.topSlowOperations.forEach { (op, duration) ->
                appendLine("  - $op: ${duration}ms avg")
            }
            appendLine()
            appendLine("Top Failed Operations:")
            summary.topFailedOperations.forEach { (op, failures) ->
                appendLine("  - $op: $failures failures")
            }
        })
    }

    /**
     * Measure and track an operation automatically.
     *
     * Usage:
     * ```kotlin
     * val result = DatabaseMetrics.measureOperation("deleteAppSpecificElements") {
     *     deleteAppSpecificElements(packageName)
     * }
     * ```
     */
    suspend inline fun <T> measureOperation(
        operation: String,
        itemCount: Int = 0,
        crossinline block: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        var success = false
        try {
            val result = block()
            success = true
            return result
        } catch (e: Exception) {
            trackError(operation, e)
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            trackOperation(operation, duration, success, itemCount)
        }
    }
}

/**
 * Extension function to track database operation execution.
 */
suspend inline fun <T> trackDatabaseOperation(
    operation: String,
    itemCount: Int = 0,
    crossinline block: suspend () -> T
): T {
    return DatabaseMetrics.measureOperation(operation, itemCount, block)
}
