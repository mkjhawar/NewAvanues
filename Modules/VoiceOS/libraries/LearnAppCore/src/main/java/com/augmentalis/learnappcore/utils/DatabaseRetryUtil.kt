/**
 * DatabaseRetryUtil.kt - Retry logic for database operations
 *
 * Provides exponential backoff retry wrapper for database operations
 * that may fail due to transient issues.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Phase 4)
 *
 * @since 1.2.0 (P1 Enhancements)
 */

package com.augmentalis.learnappcore.utils

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * Database Retry Utility
 *
 * Provides exponential backoff retry logic for database operations.
 *
 * ## Retry Strategy:
 * - Initial delay: 100ms
 * - Exponential multiplier: 2.0
 * - Max retries: 3
 * - Max delay: 1000ms
 *
 * ## Retryable Errors:
 * - SQLite BUSY errors
 * - SQLite LOCKED errors
 * - Transient I/O errors
 * - Timeout errors
 *
 * ## Usage:
 * ```kotlin
 * val result = withDatabaseRetry {
 *     database.insert(data)
 * }
 * ```
 */
object DatabaseRetryUtil {
    private const val TAG = "DatabaseRetryUtil"

    // Retry configuration
    private const val MAX_RETRIES = 3
    private const val INITIAL_DELAY_MS = 100L
    private const val MAX_DELAY_MS = 1000L
    private const val BACKOFF_MULTIPLIER = 2.0

    /**
     * Execute database operation with retry logic
     *
     * Retries on transient database errors with exponential backoff.
     *
     * @param operation Database operation to execute
     * @return Result of operation
     * @throws Exception if all retries exhausted
     */
    suspend fun <T> withRetry(operation: suspend () -> T): T {
        var lastException: Exception? = null

        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e

                // Check if error is retryable
                if (!isRetryable(e)) {
                    Log.e(TAG, "Non-retryable database error", e)
                    throw e
                }

                // Check if we have retries left
                if (attempt >= MAX_RETRIES) {
                    Log.e(TAG, "Max retries ($MAX_RETRIES) exhausted", e)
                    throw e
                }

                // Calculate delay with exponential backoff
                val delayMs = calculateDelay(attempt)
                Log.w(TAG, "Database operation failed (attempt ${attempt + 1}/$MAX_RETRIES), retrying in ${delayMs}ms: ${e.message}")
                delay(delayMs)
            }
        }

        // Should never reach here, but throw last exception if we do
        throw lastException ?: IllegalStateException("Retry logic failed")
    }

    /**
     * Check if exception is retryable
     *
     * Determines if the error is transient and worth retrying.
     */
    private fun isRetryable(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""

        return when {
            // SQLite BUSY/LOCKED errors
            message.contains("database is locked") -> true
            message.contains("database is busy") -> true
            message.contains("sqlite_busy") -> true
            message.contains("sqlite_locked") -> true

            // I/O errors
            message.contains("i/o error") -> true
            message.contains("disk i/o error") -> true

            // Timeout errors
            message.contains("timeout") -> true

            // Connection errors
            message.contains("connection") -> true

            else -> false
        }
    }

    /**
     * Calculate delay for retry attempt
     *
     * Uses exponential backoff: delay = min(INITIAL_DELAY * 2^attempt, MAX_DELAY)
     */
    private fun calculateDelay(attempt: Int): Long {
        val exponentialDelay = (INITIAL_DELAY_MS * BACKOFF_MULTIPLIER.pow(attempt)).toLong()
        return exponentialDelay.coerceAtMost(MAX_DELAY_MS)
    }
}
