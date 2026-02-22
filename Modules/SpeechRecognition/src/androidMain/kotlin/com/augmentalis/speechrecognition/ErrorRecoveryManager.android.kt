/**
 * ErrorRecoveryManager.android.kt - Error recovery strategies for speech recognition
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-27
 * Updated: 2026-01-18 - Migrated to KMP androidMain
 */
package com.augmentalis.speechrecognition

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Manages error recovery strategies for speech recognition engines.
 * Provides intelligent recovery based on error type and history.
 */
class ErrorRecoveryManager(
    private val engineName: String,
    private val context: Context
) {

    companion object {
        private const val TAG = "ErrorRecoveryManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 10000L
    }

    // Recovery state
    private var currentRetryCount = 0
    private var lastErrorCode: Int = 0
    private var lastErrorTime: Long = 0
    private var consecutiveErrors: Int = 0

    // Error history for pattern detection
    private val errorHistory = mutableListOf<ErrorEntry>()
    private val maxHistorySize = 50

    /**
     * Determine recovery action for an error
     */
    fun getRecoveryAction(errorCode: Int): RecoveryAction {
        val currentTime = System.currentTimeMillis()

        // Track error
        recordError(errorCode, currentTime)

        // Check if we've exceeded retry limit
        if (currentRetryCount >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "[$engineName] Max retries exceeded for error: $errorCode")
            return RecoveryAction.ABORT
        }

        // Determine action based on error type
        val action = when (errorCode) {
            // Network errors - retry with backoff
            SpeechError.ERROR_NETWORK,
            SpeechError.ERROR_NETWORK_TIMEOUT -> {
                currentRetryCount++
                RecoveryAction.RETRY_WITH_BACKOFF
            }

            // Audio errors - reinitialize audio
            SpeechError.ERROR_AUDIO -> {
                RecoveryAction.REINITIALIZE_AUDIO
            }

            // Timeout - usually user didn't speak, just restart
            SpeechError.ERROR_SPEECH_TIMEOUT -> {
                RecoveryAction.RESTART_IMMEDIATELY
            }

            // No match - normal, restart listening
            SpeechError.ERROR_NO_MATCH -> {
                RecoveryAction.RESTART_IMMEDIATELY
            }

            // Busy - wait and retry
            SpeechError.ERROR_BUSY -> {
                currentRetryCount++
                RecoveryAction.WAIT_AND_RETRY
            }

            // Permission error - cannot recover automatically
            SpeechError.ERROR_PERMISSIONS -> {
                RecoveryAction.REQUEST_PERMISSION
            }

            // Model error - need to download/fix model
            SpeechError.ERROR_MODEL -> {
                RecoveryAction.DOWNLOAD_MODEL
            }

            // Server error - retry with backoff
            SpeechError.ERROR_SERVER -> {
                currentRetryCount++
                RecoveryAction.RETRY_WITH_BACKOFF
            }

            // Unknown error - log and try restart
            else -> {
                Log.w(TAG, "[$engineName] Unknown error code: $errorCode")
                RecoveryAction.LOG_AND_RESTART
            }
        }

        Log.d(TAG, "[$engineName] Recovery action for error $errorCode: $action (retry: $currentRetryCount)")
        return action
    }

    /**
     * Calculate delay for retry with exponential backoff
     */
    fun calculateBackoffDelay(): Long {
        val delay = (BASE_DELAY_MS * (1 shl (currentRetryCount - 1)))
            .coerceAtMost(MAX_DELAY_MS)

        // Add jitter (0-20% of delay)
        val jitter = (delay * 0.2 * Math.random()).toLong()
        return delay + jitter
    }

    /**
     * Record an error for pattern detection
     */
    private fun recordError(errorCode: Int, timestamp: Long) {
        val entry = ErrorEntry(
            errorCode = errorCode,
            timestamp = timestamp,
            engineName = engineName
        )
        errorHistory.add(entry)

        // Trim history
        if (errorHistory.size > maxHistorySize) {
            errorHistory.removeAt(0)
        }

        // Track consecutive errors
        if (errorCode == lastErrorCode && (timestamp - lastErrorTime) < 5000) {
            consecutiveErrors++
        } else {
            consecutiveErrors = 1
        }

        lastErrorCode = errorCode
        lastErrorTime = timestamp
    }

    /**
     * Check if error pattern indicates persistent issue
     */
    fun detectPersistentIssue(): Boolean {
        // Check for same error occurring frequently
        if (consecutiveErrors >= 3) {
            Log.w(TAG, "[$engineName] Persistent issue detected: $consecutiveErrors consecutive errors")
            return true
        }

        // Check error frequency in last minute
        val oneMinuteAgo = System.currentTimeMillis() - 60000
        val recentErrors = errorHistory.count { it.timestamp > oneMinuteAgo }
        if (recentErrors >= 5) {
            Log.w(TAG, "[$engineName] High error frequency: $recentErrors errors in last minute")
            return true
        }

        return false
    }

    /**
     * Reset recovery state (call after successful operation)
     */
    fun resetRetryState() {
        currentRetryCount = 0
        consecutiveErrors = 0
        Log.d(TAG, "[$engineName] Recovery state reset")
    }

    /**
     * Get recovery statistics
     */
    fun getStatistics(): RecoveryStats {
        val totalErrors = errorHistory.size
        val errorsByType = errorHistory.groupingBy { it.errorCode }.eachCount()

        return RecoveryStats(
            totalErrors = totalErrors,
            currentRetryCount = currentRetryCount,
            consecutiveErrors = consecutiveErrors,
            errorsByType = errorsByType,
            hasPersistentIssue = detectPersistentIssue()
        )
    }

    /**
     * Clear error history
     */
    fun clearHistory() {
        errorHistory.clear()
        resetRetryState()
        Log.d(TAG, "[$engineName] Error history cleared")
    }

    /**
     * Destroy and clean up
     */
    fun destroy() {
        clearHistory()
        Log.d(TAG, "[$engineName] Error recovery manager destroyed")
    }

    /**
     * Recovery actions
     */
    enum class RecoveryAction {
        RESTART_IMMEDIATELY,   // Restart recognition immediately
        RETRY_WITH_BACKOFF,    // Retry with exponential backoff
        WAIT_AND_RETRY,        // Wait a fixed time and retry
        REINITIALIZE_AUDIO,    // Reinitialize audio subsystem
        REINITIALIZE_ENGINE,   // Full engine reinitialization
        REQUEST_PERMISSION,    // User needs to grant permission
        DOWNLOAD_MODEL,        // Need to download speech model
        LOG_AND_RESTART,       // Log error and restart
        ABORT                  // Cannot recover, abort operation
    }

    /**
     * Error history entry
     */
    data class ErrorEntry(
        val errorCode: Int,
        val timestamp: Long,
        val engineName: String
    )

    /**
     * Recovery statistics
     */
    data class RecoveryStats(
        val totalErrors: Int,
        val currentRetryCount: Int,
        val consecutiveErrors: Int,
        val errorsByType: Map<Int, Int>,
        val hasPersistentIssue: Boolean
    )
}
