/**
 * PerformanceMonitor.android.kt - Performance monitoring for speech recognition
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-27
 * Updated: 2026-01-18 - Migrated to KMP androidMain
 */
package com.augmentalis.speechrecognition

import android.util.Log

/**
 * Monitors and tracks performance metrics for speech recognition engines.
 * Provides latency tracking, success rate monitoring, and performance analysis.
 */
class PerformanceMonitor(private val engineName: String) {

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val DEFAULT_SLOW_THRESHOLD_MS = 3000L
    }

    // Session tracking
    private var sessionStartTime: Long = 0L
    private var lastRecognitionTime: Long = 0L

    // Performance metrics
    private var totalRecognitions: Long = 0
    private var successfulRecognitions: Long = 0
    private var failedRecognitions: Long = 0
    private var totalLatencyMs: Long = 0

    // Slow operation tracking
    private val slowOperations = mutableListOf<SlowOperation>()
    private var slowOperationThreshold: Long = DEFAULT_SLOW_THRESHOLD_MS

    // Performance state
    private var performanceState: PerformanceState = PerformanceState.OPTIMAL

    /**
     * Start a new recognition session
     */
    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        Log.d(TAG, "[$engineName] Session started")
    }

    /**
     * Record a recognition attempt
     */
    fun recordRecognition(startTime: Long, success: Boolean, result: String? = null) {
        val endTime = System.currentTimeMillis()
        val latency = endTime - startTime

        totalRecognitions++
        if (success) {
            successfulRecognitions++
        } else {
            failedRecognitions++
        }

        totalLatencyMs += latency
        lastRecognitionTime = endTime

        // Check for slow recognition
        if (latency > slowOperationThreshold) {
            recordSlowOperation("recognition", latency, slowOperationThreshold)
        }

        // Update performance state
        updatePerformanceState()

        Log.d(TAG, "[$engineName] Recognition recorded: success=$success, latency=${latency}ms, result='${result?.take(20) ?: "null"}'")
    }

    /**
     * Record a slow operation
     */
    fun recordSlowOperation(operationType: String, durationMs: Long, threshold: Long) {
        val operation = SlowOperation(
            type = operationType,
            durationMs = durationMs,
            threshold = threshold,
            timestamp = System.currentTimeMillis()
        )
        slowOperations.add(operation)

        // Keep only last 100 slow operations
        if (slowOperations.size > 100) {
            slowOperations.removeAt(0)
        }

        Log.w(TAG, "[$engineName] Slow operation: $operationType took ${durationMs}ms (threshold: ${threshold}ms)")
    }

    /**
     * Get current performance metrics
     */
    fun getMetrics(): Metrics {
        val averageLatency = if (totalRecognitions > 0) {
            totalLatencyMs.toFloat() / totalRecognitions
        } else {
            0f
        }

        val successRate = if (totalRecognitions > 0) {
            successfulRecognitions.toFloat() / totalRecognitions
        } else {
            0f
        }

        return Metrics(
            engineName = engineName,
            totalRecognitions = totalRecognitions,
            successfulRecognitions = successfulRecognitions,
            failedRecognitions = failedRecognitions,
            averageLatency = averageLatency,
            successRate = successRate,
            slowOperationCount = slowOperations.size,
            performanceState = performanceState
        )
    }

    /**
     * Update performance state based on metrics
     */
    private fun updatePerformanceState() {
        val metrics = getMetrics()

        performanceState = when {
            metrics.successRate < 0.5f -> PerformanceState.CRITICAL
            metrics.successRate < 0.7f -> PerformanceState.DEGRADED
            metrics.averageLatency > 5000 -> PerformanceState.SLOW
            metrics.slowOperationCount > 10 -> PerformanceState.WARNING
            else -> PerformanceState.OPTIMAL
        }
    }

    /**
     * Reset all metrics
     */
    fun reset() {
        sessionStartTime = 0L
        lastRecognitionTime = 0L
        totalRecognitions = 0
        successfulRecognitions = 0
        failedRecognitions = 0
        totalLatencyMs = 0
        slowOperations.clear()
        performanceState = PerformanceState.OPTIMAL

        Log.d(TAG, "[$engineName] Performance metrics reset")
    }

    /**
     * Destroy and clean up
     */
    fun destroy() {
        reset()
        Log.d(TAG, "[$engineName] Performance monitor destroyed")
    }

    /**
     * Performance states
     */
    enum class PerformanceState {
        OPTIMAL,    // All metrics within normal range
        WARNING,    // Some metrics showing degradation
        SLOW,       // High latency detected
        DEGRADED,   // Success rate dropping
        CRITICAL    // Significant issues detected
    }

    /**
     * Performance metrics data class
     */
    data class Metrics(
        val engineName: String,
        val totalRecognitions: Long,
        val successfulRecognitions: Long,
        val failedRecognitions: Long,
        val averageLatency: Float,
        val successRate: Float,
        val slowOperationCount: Int,
        val performanceState: PerformanceState
    )

    /**
     * Slow operation data class
     */
    data class SlowOperation(
        val type: String,
        val durationMs: Long,
        val threshold: Long,
        val timestamp: Long
    )
}
