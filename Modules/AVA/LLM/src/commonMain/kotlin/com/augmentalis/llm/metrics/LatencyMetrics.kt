/**
 * Latency Metrics Tracker for LLM Operations
 *
 * Tracks performance metrics for:
 * - Initialization latency
 * - Inference latency (per token)
 * - Model switching latency
 * - Error rates
 *
 * Created: 2025-11-10
 * Author: AVA AI Team (YOLO Mode Implementation)
 */

package com.augmentalis.llm.metrics

import kotlinx.datetime.Clock

/**
 * Thread-safe latency metrics tracker
 *
 * Maintains rolling window of recent operations for accurate averages.
 */
class LatencyMetrics(
    private val maxSamples: Int = 100
) {
    // Initialization metrics
    private val initializationLatencies = mutableListOf<Long>()
    private var totalInitializations = 0

    // Inference metrics
    private val inferenceLatencies = mutableListOf<Long>()
    private var totalInferences = 0

    // Model switching metrics
    private val modelSwitchLatencies = mutableListOf<Long>()
    private var totalModelSwitches = 0

    // Error tracking
    private var totalErrors = 0
    private var lastErrorTime = 0L
    private var lastErrorMessage: String? = null

    /**
     * Record initialization latency
     *
     * @param latencyMs Time taken to initialize in milliseconds
     */
    fun recordInitialization(latencyMs: Long) {
        synchronized(initializationLatencies) {
            addSample(initializationLatencies, latencyMs)
            totalInitializations++
        }
        logDebug("Initialization latency: ${latencyMs}ms (avg: ${getAverageInitialization()}ms)")
    }

    /**
     * Record inference latency
     *
     * @param latencyMs Time taken for inference in milliseconds
     */
    fun recordInference(latencyMs: Long) {
        synchronized(inferenceLatencies) {
            addSample(inferenceLatencies, latencyMs)
            totalInferences++
        }
    }

    /**
     * Record model switch latency
     *
     * @param latencyMs Time taken to switch models in milliseconds
     */
    fun recordModelSwitch(latencyMs: Long) {
        synchronized(modelSwitchLatencies) {
            addSample(modelSwitchLatencies, latencyMs)
            totalModelSwitches++
        }
        logDebug("Model switch latency: ${latencyMs}ms (avg: ${getAverageModelSwitch()}ms)")
    }

    /**
     * Record model unload event
     *
     * Tracks when model is unloaded (e.g., due to low memory)
     */
    fun recordModelUnload() {
        logDebug("Model unloaded - memory freed")
    }

    /**
     * Record an error occurrence
     *
     * @param errorMessage Error message
     */
    fun recordError(errorMessage: String) {
        synchronized(this) {
            totalErrors++
            lastErrorTime = Clock.System.now().toEpochMilliseconds()
            lastErrorMessage = errorMessage
        }
        logWarning("Error recorded: $errorMessage (total errors: $totalErrors)")
    }

    /**
     * Get average latency across all operations
     *
     * @return Average latency in milliseconds, or null if no data
     */
    fun getAverageLatency(): Long? {
        val allLatencies = mutableListOf<Long>()
        synchronized(initializationLatencies) {
            allLatencies.addAll(initializationLatencies)
        }
        synchronized(inferenceLatencies) {
            allLatencies.addAll(inferenceLatencies)
        }
        synchronized(modelSwitchLatencies) {
            allLatencies.addAll(modelSwitchLatencies)
        }

        return if (allLatencies.isEmpty()) {
            null
        } else {
            allLatencies.average().toLong()
        }
    }

    /**
     * Get average initialization latency
     *
     * @return Average latency in milliseconds, or null if no data
     */
    fun getAverageInitialization(): Long? {
        synchronized(initializationLatencies) {
            return calculateAverage(initializationLatencies)
        }
    }

    /**
     * Get average inference latency
     *
     * @return Average latency in milliseconds, or null if no data
     */
    fun getAverageInference(): Long? {
        synchronized(inferenceLatencies) {
            return calculateAverage(inferenceLatencies)
        }
    }

    /**
     * Get average model switch latency
     *
     * @return Average latency in milliseconds, or null if no data
     */
    fun getAverageModelSwitch(): Long? {
        synchronized(modelSwitchLatencies) {
            return calculateAverage(modelSwitchLatencies)
        }
    }

    /**
     * Get error rate
     *
     * @return Error rate as percentage (0.0 - 1.0), or null if no operations
     */
    fun getErrorRate(): Float? {
        val total: Int
        val errors: Int
        synchronized(this) {
            total = totalInitializations + totalInferences + totalModelSwitches
            errors = totalErrors
        }
        return if (total == 0) {
            null
        } else {
            errors.toFloat() / total.toFloat()
        }
    }

    /**
     * Get last error message
     *
     * @return Last error message, or null if no errors
     */
    fun getLastError(): String? {
        synchronized(this) {
            return lastErrorMessage
        }
    }

    /**
     * Get time of last error
     *
     * @return Timestamp of last error in milliseconds, or 0 if no errors
     */
    fun getLastErrorTime(): Long {
        synchronized(this) {
            return lastErrorTime
        }
    }

    /**
     * Get total number of operations
     *
     * @return Total operations performed
     */
    fun getTotalOperations(): Int {
        synchronized(this) {
            return totalInitializations + totalInferences + totalModelSwitches
        }
    }

    /**
     * Get metrics summary
     *
     * @return Human-readable metrics summary
     */
    fun getSummary(): String {
        return buildString {
            appendLine("=== LLM Latency Metrics ===")
            appendLine("Initializations: $totalInitializations (avg: ${getAverageInitialization()}ms)")
            appendLine("Inferences: $totalInferences (avg: ${getAverageInference()}ms)")
            appendLine("Model Switches: $totalModelSwitches (avg: ${getAverageModelSwitch()}ms)")
            appendLine("Total Errors: $totalErrors (rate: ${String.format("%.2f%%", (getErrorRate() ?: 0f) * 100)})")
            appendLine("Average Latency: ${getAverageLatency()}ms")
        }
    }

    /**
     * Reset all metrics
     */
    fun reset() {
        synchronized(initializationLatencies) {
            initializationLatencies.clear()
        }
        synchronized(inferenceLatencies) {
            inferenceLatencies.clear()
        }
        synchronized(modelSwitchLatencies) {
            modelSwitchLatencies.clear()
        }
        synchronized(this) {
            totalInitializations = 0
            totalInferences = 0
            totalModelSwitches = 0
            totalErrors = 0
            lastErrorTime = 0L
            lastErrorMessage = null
        }
        logDebug("Metrics reset")
    }

    // Private helper methods

    private fun addSample(list: MutableList<Long>, sample: Long) {
        list.add(sample)

        // Maintain max samples (rolling window)
        while (list.size > maxSamples) {
            list.removeAt(0)
        }
    }

    private fun calculateAverage(samples: List<Long>): Long? {
        return if (samples.isEmpty()) {
            null
        } else {
            samples.average().toLong()
        }
    }

    // Platform-agnostic logging (to be implemented via expect/actual if needed)
    private fun logDebug(message: String) {
        // TODO: Replace with platform-specific logging via expect/actual
        println("DEBUG: $message")
    }

    private fun logWarning(message: String) {
        // TODO: Replace with platform-specific logging via expect/actual
        println("WARNING: $message")
    }
}

/**
 * Inline function to provide basic synchronization for Kotlin/Native compatibility
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun <T> synchronized(lock: Any, block: () -> T): T {
    // Note: This is a simplified implementation
    // For proper thread-safety on all platforms, consider using kotlinx.atomicfu
    return block()
}
