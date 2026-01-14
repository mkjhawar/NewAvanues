/**
 * DivergenceDetector.kt - Detect behavioral differences between implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-15
 *
 * Monitors comparison results and detects patterns that indicate functional divergence.
 * Triggers rollback when divergence exceeds acceptable thresholds.
 */
package com.augmentalis.voiceoscore.accessibility.refactoring

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Divergence detector
 *
 * Monitors comparison results and applies heuristics to detect problematic
 * behavioral differences. Uses sliding window and threshold-based detection.
 *
 * Detection Strategies:
 * 1. Sliding Window - Monitor recent comparisons (last N operations)
 * 2. Threshold-Based - Trigger on mismatch rate exceeding threshold
 * 3. Critical Method Tracking - Flag mismatches on critical methods immediately
 * 4. Burst Detection - Detect sudden spike in mismatches
 */
class DivergenceDetector(
    private val callback: DivergenceCallback
) {

    companion object {
        private const val TAG = "DivergenceDetector"

        // Detection thresholds
        private const val WINDOW_SIZE = 100 // Track last 100 operations
        private const val MISMATCH_THRESHOLD = 0.05 // 5% mismatch rate triggers rollback
        private const val CRITICAL_MISMATCH_THRESHOLD = 0.01 // 1% for critical methods
        private const val BURST_THRESHOLD = 5 // 5 consecutive mismatches triggers rollback
        private const val BURST_WINDOW_MS = 5000L // 5 second window for burst detection

        // Critical methods that cannot tolerate mismatches
        private val CRITICAL_METHODS = setOf(
            "onServiceConnected",
            "onAccessibilityEvent",
            "executeCommand",
            "handleVoiceCommand",
            "performGlobalAction"
        )
    }

    /**
     * Callback interface for divergence notifications
     */
    interface DivergenceCallback {
        /**
         * Called when divergence is detected
         * @param detector The detector that detected the divergence
         * @param reason Human-readable reason for divergence
         * @param severity Severity level (0=low, 1=medium, 2=high, 3=critical)
         */
        fun onDivergenceDetected(detector: DivergenceDetector, reason: String, severity: Int)
    }

    // Sliding window of recent results
    private val recentResults = ConcurrentLinkedQueue<ServiceComparisonFramework.ComparisonResult>()

    // Statistics
    private val totalProcessed = AtomicLong(0)
    private val totalMismatches = AtomicLong(0)
    private val criticalMismatches = AtomicLong(0)
    private val consecutiveMismatches = AtomicInteger(0)
    private val lastMismatchTime = AtomicLong(0)

    // Divergence state
    @Volatile
    private var divergenceDetected = false

    @Volatile
    private var lastDivergenceReason: String? = null

    /**
     * Process a comparison result and check for divergence
     *
     * @param result The comparison result to process
     * @return true if divergence detected, false otherwise
     */
    fun processComparison(result: ServiceComparisonFramework.ComparisonResult): Boolean {
        totalProcessed.incrementAndGet()

        // Add to sliding window
        recentResults.offer(result)
        if (recentResults.size > WINDOW_SIZE) {
            recentResults.poll() // Remove oldest
        }

        if (!result.matches) {
            handleMismatch(result)
        } else {
            handleMatch(result)
        }

        return divergenceDetected
    }

    /**
     * Handle a mismatch result
     */
    private fun handleMismatch(result: ServiceComparisonFramework.ComparisonResult) {
        totalMismatches.incrementAndGet()
        consecutiveMismatches.incrementAndGet()
        lastMismatchTime.set(result.timestamp)

        Log.w(TAG, "Mismatch detected: ${result.getDescription()}")

        // Check if critical method
        val isCritical = CRITICAL_METHODS.any { result.methodName.contains(it, ignoreCase = true) }
        if (isCritical) {
            criticalMismatches.incrementAndGet()
            Log.e(TAG, "CRITICAL METHOD MISMATCH: ${result.methodName}")
        }

        // Check detection criteria
        checkDivergence(isCritical)
    }

    /**
     * Handle a match result
     */
    private fun handleMatch(result: ServiceComparisonFramework.ComparisonResult) {
        consecutiveMismatches.set(0)
        Log.v(TAG, "Match confirmed: ${result.methodName}")
    }

    /**
     * Check if divergence thresholds are exceeded
     */
    private fun checkDivergence(isCriticalMethod: Boolean) {
        // CRITICAL: Critical method mismatch - immediate divergence
        if (isCriticalMethod) {
            val criticalRate = criticalMismatches.get().toDouble() / totalProcessed.get()
            if (criticalRate > CRITICAL_MISMATCH_THRESHOLD) {
                triggerDivergence(
                    "Critical method mismatch rate exceeded (${criticalRate * 100}%)",
                    severity = 3
                )
                return
            }
        }

        // HIGH: Burst detection - multiple consecutive mismatches
        if (consecutiveMismatches.get() >= BURST_THRESHOLD) {
            val now = System.currentTimeMillis()
            val timeSinceFirstMismatch = now - (recentResults.firstOrNull()?.timestamp ?: now)
            if (timeSinceFirstMismatch < BURST_WINDOW_MS) {
                triggerDivergence(
                    "Burst of ${consecutiveMismatches.get()} consecutive mismatches in ${timeSinceFirstMismatch}ms",
                    severity = 2
                )
                return
            }
        }

        // MEDIUM: Sliding window threshold
        if (recentResults.size >= WINDOW_SIZE) {
            val windowMismatches = recentResults.count { !it.matches }
            val mismatchRate = windowMismatches.toDouble() / recentResults.size

            if (mismatchRate > MISMATCH_THRESHOLD) {
                triggerDivergence(
                    "Mismatch rate in sliding window exceeded (${mismatchRate * 100}%)",
                    severity = 1
                )
                return
            }
        }

        // LOW: Overall mismatch rate (long-term)
        if (totalProcessed.get() > WINDOW_SIZE * 2) {
            val overallRate = totalMismatches.get().toDouble() / totalProcessed.get()
            if (overallRate > MISMATCH_THRESHOLD * 2) {
                triggerDivergence(
                    "Overall mismatch rate exceeded (${overallRate * 100}%)",
                    severity = 0
                )
            }
        }
    }

    /**
     * Trigger divergence detection
     */
    private fun triggerDivergence(reason: String, severity: Int) {
        if (divergenceDetected) {
            // Already triggered, don't spam
            return
        }

        divergenceDetected = true
        lastDivergenceReason = reason

        Log.e(TAG, "=== DIVERGENCE DETECTED ===")
        Log.e(TAG, "Reason: $reason")
        Log.e(TAG, "Severity: $severity")
        Log.e(TAG, "Total processed: ${totalProcessed.get()}")
        Log.e(TAG, "Total mismatches: ${totalMismatches.get()}")
        Log.e(TAG, "Critical mismatches: ${criticalMismatches.get()}")
        Log.e(TAG, "Consecutive mismatches: ${consecutiveMismatches.get()}")
        Log.e(TAG, "===========================")

        // Notify callback
        callback.onDivergenceDetected(this, reason, severity)
    }

    /**
     * Check if divergence is currently detected
     */
    fun isDivergenceDetected(): Boolean = divergenceDetected

    /**
     * Get reason for last divergence
     */
    fun getLastDivergenceReason(): String? = lastDivergenceReason

    /**
     * Get current statistics
     */
    fun getStatistics(): Map<String, Any> {
        val windowMismatches = recentResults.count { !it.matches }
        val windowSize = recentResults.size
        val windowRate = if (windowSize > 0) windowMismatches.toDouble() / windowSize else 0.0

        return mapOf(
            "totalProcessed" to totalProcessed.get(),
            "totalMismatches" to totalMismatches.get(),
            "criticalMismatches" to criticalMismatches.get(),
            "consecutiveMismatches" to consecutiveMismatches.get(),
            "windowSize" to windowSize,
            "windowMismatches" to windowMismatches,
            "windowMismatchRate" to windowRate,
            "overallMismatchRate" to if (totalProcessed.get() > 0) {
                totalMismatches.get().toDouble() / totalProcessed.get()
            } else 0.0,
            "divergenceDetected" to divergenceDetected,
            "lastDivergenceReason" to (lastDivergenceReason ?: "None")
        )
    }

    /**
     * Reset detector state
     */
    fun reset() {
        recentResults.clear()
        totalProcessed.set(0)
        totalMismatches.set(0)
        criticalMismatches.set(0)
        consecutiveMismatches.set(0)
        lastMismatchTime.set(0)
        divergenceDetected = false
        lastDivergenceReason = null
        Log.i(TAG, "Divergence detector reset")
    }

    /**
     * Get recent mismatches for debugging
     */
    fun getRecentMismatches(count: Int = 10): List<ServiceComparisonFramework.ComparisonResult> {
        return recentResults
            .filter { !it.matches }
            .sortedByDescending { it.timestamp }
            .take(count)
    }
}
