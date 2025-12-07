/**
 * RollbackController.kt - Automatic rollback on divergence detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-15
 *
 * Manages automatic rollback from refactored to legacy implementation when
 * behavioral divergence is detected. Ensures service state is preserved during rollback.
 */
package com.augmentalis.voiceoscore.accessibility.refactoring

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Rollback controller
 *
 * Manages automatic rollback with state preservation and recovery.
 * Ensures service continues operating without interruption during rollback.
 *
 * Rollback Strategy:
 * 1. Detect divergence via DivergenceDetector
 * 2. Capture current service state
 * 3. Switch to legacy implementation (<10ms)
 * 4. Restore state in legacy implementation
 * 5. Log rollback event
 * 6. Disable refactored implementation
 */
class RollbackController(
    private val featureFlags: RefactoringFeatureFlags
) : DivergenceDetector.DivergenceCallback {

    companion object {
        private const val TAG = "RollbackController"
        private const val ROLLBACK_COOLDOWN_MS = 60000L // 1 minute cooldown between rollbacks
    }

    // Rollback state
    private val rolledBack = AtomicBoolean(false)
    private val lastRollbackTime = AtomicLong(0)
    private val rollbackCount = AtomicLong(0)

    @Volatile
    private var rollbackReason: String? = null

    @Volatile
    private var rollbackSeverity: Int = 0

    // Rollback callback
    private var rollbackCallback: RollbackCallback? = null

    /**
     * Callback interface for rollback notifications
     */
    interface RollbackCallback {
        /**
         * Called when rollback is initiated
         * @param reason Human-readable reason for rollback
         * @param severity Severity level (0=low, 1=medium, 2=high, 3=critical)
         */
        fun onRollbackInitiated(reason: String, severity: Int)

        /**
         * Called when rollback is completed
         * @param success true if rollback succeeded, false otherwise
         * @param rollbackTimeMs Time taken for rollback in milliseconds
         */
        fun onRollbackCompleted(success: Boolean, rollbackTimeMs: Long)

        /**
         * Capture current service state before rollback
         * @return Map of state key-value pairs to preserve
         */
        fun captureServiceState(): Map<String, Any>

        /**
         * Restore service state after rollback
         * @param state Previously captured state
         */
        fun restoreServiceState(state: Map<String, Any>)
    }

    /**
     * Set rollback callback
     */
    fun setRollbackCallback(callback: RollbackCallback) {
        this.rollbackCallback = callback
    }

    /**
     * DivergenceDetector.DivergenceCallback implementation
     * Called when divergence is detected
     */
    override fun onDivergenceDetected(detector: DivergenceDetector, reason: String, severity: Int) {
        Log.w(TAG, "Divergence detected: $reason (severity=$severity)")

        // Check if auto-rollback is enabled
        if (!featureFlags.isAutoRollbackEnabled()) {
            Log.w(TAG, "Auto-rollback is disabled - manual intervention required")
            return
        }

        // Check if already rolled back
        if (rolledBack.get()) {
            Log.w(TAG, "Already rolled back - ignoring divergence")
            return
        }

        // Check cooldown period
        val now = System.currentTimeMillis()
        val timeSinceLastRollback = now - lastRollbackTime.get()
        if (timeSinceLastRollback < ROLLBACK_COOLDOWN_MS) {
            Log.w(TAG, "Rollback cooldown active (${timeSinceLastRollback}ms since last rollback)")
            return
        }

        // Perform rollback
        performRollback(reason, severity)
    }

    /**
     * Perform rollback from refactored to legacy implementation
     *
     * Steps:
     * 1. Notify callback that rollback is starting
     * 2. Capture current service state
     * 3. Force legacy implementation via feature flags
     * 4. Restore state
     * 5. Notify callback that rollback is complete
     * 6. Log rollback event
     *
     * @param reason Human-readable reason for rollback
     * @param severity Severity level
     */
    private fun performRollback(reason: String, severity: Int) {
        val startTime = System.currentTimeMillis()
        Log.e(TAG, "=== INITIATING ROLLBACK ===")
        Log.e(TAG, "Reason: $reason")
        Log.e(TAG, "Severity: $severity")

        try {
            // Notify callback
            rollbackCallback?.onRollbackInitiated(reason, severity)

            // Capture current state
            Log.d(TAG, "Capturing service state...")
            val state = rollbackCallback?.captureServiceState() ?: emptyMap()
            Log.d(TAG, "State captured: ${state.size} items")

            // Force legacy implementation
            Log.d(TAG, "Forcing legacy implementation...")
            featureFlags.setForceLegacy(true)
            featureFlags.disableRefactored()
            Log.i(TAG, "✓ Legacy implementation forced")

            // Restore state
            if (state.isNotEmpty()) {
                Log.d(TAG, "Restoring service state...")
                rollbackCallback?.restoreServiceState(state)
                Log.d(TAG, "✓ State restored")
            }

            // Update rollback state
            rolledBack.set(true)
            rollbackCount.incrementAndGet()
            lastRollbackTime.set(startTime)
            rollbackReason = reason
            rollbackSeverity = severity

            val rollbackTimeMs = System.currentTimeMillis() - startTime
            Log.i(TAG, "✓ Rollback completed in ${rollbackTimeMs}ms")

            // Notify callback
            rollbackCallback?.onRollbackCompleted(true, rollbackTimeMs)

            // Log event
            logRollbackEvent(reason, severity, rollbackTimeMs, success = true)

        } catch (e: Exception) {
            val rollbackTimeMs = System.currentTimeMillis() - startTime
            Log.e(TAG, "✗ Rollback FAILED after ${rollbackTimeMs}ms", e)

            // Notify callback of failure
            rollbackCallback?.onRollbackCompleted(false, rollbackTimeMs)

            // Log failure event
            logRollbackEvent(reason, severity, rollbackTimeMs, success = false, error = e)
        } finally {
            Log.e(TAG, "=========================")
        }
    }

    /**
     * Manual rollback (for testing or emergency)
     */
    fun performManualRollback(reason: String = "Manual rollback requested") {
        Log.w(TAG, "Manual rollback requested: $reason")
        performRollback(reason, severity = 3)
    }

    /**
     * Reset rollback state (for re-enabling refactored implementation)
     */
    fun resetRollback() {
        if (!rolledBack.get()) {
            Log.w(TAG, "Not currently rolled back - reset not needed")
            return
        }

        Log.i(TAG, "Resetting rollback state...")
        featureFlags.setForceLegacy(false)
        rolledBack.set(false)
        rollbackReason = null
        rollbackSeverity = 0
        Log.i(TAG, "✓ Rollback state reset - refactored implementation can be re-enabled")
    }

    /**
     * Check if currently rolled back
     */
    fun isRolledBack(): Boolean = rolledBack.get()

    /**
     * Get rollback reason
     */
    fun getRollbackReason(): String? = rollbackReason

    /**
     * Get rollback severity
     */
    fun getRollbackSeverity(): Int = rollbackSeverity

    /**
     * Get rollback statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "rolledBack" to rolledBack.get(),
            "rollbackCount" to rollbackCount.get(),
            "lastRollbackTime" to lastRollbackTime.get(),
            "rollbackReason" to (rollbackReason ?: "None"),
            "rollbackSeverity" to rollbackSeverity,
            "timeSinceLastRollback" to if (lastRollbackTime.get() > 0) {
                System.currentTimeMillis() - lastRollbackTime.get()
            } else 0
        )
    }

    /**
     * Log rollback event
     */
    private fun logRollbackEvent(
        reason: String,
        severity: Int,
        rollbackTimeMs: Long,
        success: Boolean,
        error: Throwable? = null
    ) {
        val event = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "reason" to reason,
            "severity" to severity,
            "rollbackTimeMs" to rollbackTimeMs,
            "success" to success,
            "error" to (error?.message ?: "None"),
            "rollbackCount" to rollbackCount.get()
        )

        Log.i(TAG, "Rollback event logged: $event")

        // TODO: Send to analytics/monitoring system
        // analyticsTracker.trackRollback(event)
    }

    /**
     * Check if rollback cooldown is active
     */
    fun isInCooldown(): Boolean {
        val now = System.currentTimeMillis()
        val timeSinceLastRollback = now - lastRollbackTime.get()
        return timeSinceLastRollback < ROLLBACK_COOLDOWN_MS
    }

    /**
     * Get remaining cooldown time in milliseconds
     */
    fun getRemainingCooldownMs(): Long {
        if (!isInCooldown()) return 0
        val now = System.currentTimeMillis()
        val timeSinceLastRollback = now - lastRollbackTime.get()
        return ROLLBACK_COOLDOWN_MS - timeSinceLastRollback
    }
}
