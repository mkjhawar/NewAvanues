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

package com.augmentalis.ava.features.llm.metrics

import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe latency metrics tracker
 *
 * Maintains rolling window of recent operations for accurate averages.
 */
class LatencyMetrics(
    private val maxSamples: Int = 100
) {
    // Initialization metrics
    private val initializationLatencies = ConcurrentLinkedQueue<Long>()
    private val totalInitializations = AtomicInteger(0)

    // Inference metrics
    private val inferenceLatencies = ConcurrentLinkedQueue<Long>()
    private val totalInferences = AtomicInteger(0)

    // Model switching metrics
    private val modelSwitchLatencies = ConcurrentLinkedQueue<Long>()
    private val totalModelSwitches = AtomicInteger(0)

    // Error tracking
    private val totalErrors = AtomicInteger(0)
    private val lastErrorTime = AtomicLong(0L)
    private var lastErrorMessage: String? = null

    /**
     * Record initialization latency
     *
     * @param latencyMs Time taken to initialize in milliseconds
     */
    fun recordInitialization(latencyMs: Long) {
        addSample(initializationLatencies, latencyMs)
        totalInitializations.incrementAndGet()
        Timber.d("Initialization latency: ${latencyMs}ms (avg: ${getAverageInitialization()}ms)")
    }

    /**
     * Record inference latency
     *
     * @param latencyMs Time taken for inference in milliseconds
     */
    fun recordInference(latencyMs: Long) {
        addSample(inferenceLatencies, latencyMs)
        totalInferences.incrementAndGet()
    }

    /**
     * Record model switch latency
     *
     * @param latencyMs Time taken to switch models in milliseconds
     */
    fun recordModelSwitch(latencyMs: Long) {
        addSample(modelSwitchLatencies, latencyMs)
        totalModelSwitches.incrementAndGet()
        Timber.d("Model switch latency: ${latencyMs}ms (avg: ${getAverageModelSwitch()}ms)")
    }

    /**
     * Record model unload event
     *
     * Tracks when model is unloaded (e.g., due to low memory)
     */
    fun recordModelUnload() {
        Timber.d("Model unloaded - memory freed")
    }

    /**
     * Record an error occurrence
     *
     * @param errorMessage Error message
     */
    fun recordError(errorMessage: String) {
        totalErrors.incrementAndGet()
        lastErrorTime.set(System.currentTimeMillis())
        lastErrorMessage = errorMessage
        Timber.w("Error recorded: $errorMessage (total errors: ${totalErrors.get()})")
    }

    /**
     * Get average latency across all operations
     *
     * @return Average latency in milliseconds, or null if no data
     */
    fun getAverageLatency(): Long? {
        val allLatencies = mutableListOf<Long>()
        allLatencies.addAll(initializationLatencies)
        allLatencies.addAll(inferenceLatencies)
        allLatencies.addAll(modelSwitchLatencies)

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
        return calculateAverage(initializationLatencies)
    }

    /**
     * Get average inference latency
     *
     * @return Average latency in milliseconds, or null if no data
     */
    fun getAverageInference(): Long? {
        return calculateAverage(inferenceLatencies)
    }

    /**
     * Get average model switch latency
     *
     * @return Average latency in milliseconds, or null if no data
     */
    fun getAverageModelSwitch(): Long? {
        return calculateAverage(modelSwitchLatencies)
    }

    /**
     * Get error rate
     *
     * @return Error rate as percentage (0.0 - 1.0), or null if no operations
     */
    fun getErrorRate(): Float? {
        val total = totalInitializations.get() + totalInferences.get() + totalModelSwitches.get()
        return if (total == 0) {
            null
        } else {
            totalErrors.get().toFloat() / total.toFloat()
        }
    }

    /**
     * Get last error message
     *
     * @return Last error message, or null if no errors
     */
    fun getLastError(): String? {
        return lastErrorMessage
    }

    /**
     * Get time of last error
     *
     * @return Timestamp of last error in milliseconds, or 0 if no errors
     */
    fun getLastErrorTime(): Long {
        return lastErrorTime.get()
    }

    /**
     * Get total number of operations
     *
     * @return Total operations performed
     */
    fun getTotalOperations(): Int {
        return totalInitializations.get() + totalInferences.get() + totalModelSwitches.get()
    }

    /**
     * Get metrics summary
     *
     * @return Human-readable metrics summary
     */
    fun getSummary(): String {
        return buildString {
            appendLine("=== LLM Latency Metrics ===")
            appendLine("Initializations: ${totalInitializations.get()} (avg: ${getAverageInitialization()}ms)")
            appendLine("Inferences: ${totalInferences.get()} (avg: ${getAverageInference()}ms)")
            appendLine("Model Switches: ${totalModelSwitches.get()} (avg: ${getAverageModelSwitch()}ms)")
            appendLine("Total Errors: ${totalErrors.get()} (rate: ${String.format("%.2f%%", (getErrorRate() ?: 0f) * 100)})")
            appendLine("Average Latency: ${getAverageLatency()}ms")
        }
    }

    /**
     * Reset all metrics
     */
    fun reset() {
        initializationLatencies.clear()
        inferenceLatencies.clear()
        modelSwitchLatencies.clear()
        totalInitializations.set(0)
        totalInferences.set(0)
        totalModelSwitches.set(0)
        totalErrors.set(0)
        lastErrorTime.set(0L)
        lastErrorMessage = null
        Timber.d("Metrics reset")
    }

    // Private helper methods

    private fun addSample(queue: ConcurrentLinkedQueue<Long>, sample: Long) {
        queue.offer(sample)

        // Maintain max samples (rolling window)
        while (queue.size > maxSamples) {
            queue.poll()
        }
    }

    private fun calculateAverage(queue: ConcurrentLinkedQueue<Long>): Long? {
        val samples = queue.toList()
        return if (samples.isEmpty()) {
            null
        } else {
            samples.average().toLong()
        }
    }
}
