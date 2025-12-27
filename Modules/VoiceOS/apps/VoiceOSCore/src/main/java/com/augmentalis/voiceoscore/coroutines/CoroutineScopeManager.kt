/**
 * CoroutineScopeManager.kt - Safe coroutine scope management with proper cancellation
 *
 * YOLO Phase 2 - High Priority Issue #9: Coroutine Scope Cancellation
 *
 * Provides safe coroutine scope lifecycle management with:
 * - Proper cancellation using cancelAndJoin()
 * - Verification that all jobs complete
 * - Error handling during cancellation
 * - Prevention of memory leaks from running coroutines
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-09
 */
package com.augmentalis.voiceoscore.coroutines

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive

/**
 * Manager for safe coroutine scope lifecycle
 *
 * Ensures proper cleanup with cancelAndJoin() to prevent memory leaks
 * and guarantee all coroutines are stopped before service destruction.
 *
 * Usage:
 * ```kotlin
 * class MyService {
 *     private val scopeManager = CoroutineScopeManager("MyService")
 *
 *     fun start() {
 *         scopeManager.launch {
 *             // Your coroutine code
 *         }
 *     }
 *
 *     fun cleanup() {
 *         scopeManager.cancelAndJoinAll()
 *     }
 * }
 * ```
 */
class CoroutineScopeManager(
    private val tag: String
) {
    companion object {
        private const val CANCELLATION_TIMEOUT_MS = 5000L
    }

    private val activeJobs = mutableListOf<Job>()
    private var isCancelled = false

    /**
     * Launch a coroutine and track it for proper cancellation
     *
     * @param scope The coroutine scope to launch in
     * @param block The coroutine block to execute
     * @return The launched Job
     */
    fun launch(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit): Job? {
        if (isCancelled) {
            Log.w(tag, "Cannot launch coroutine - scope manager is cancelled")
            return null
        }

        val job = scope.launch(block = block)
        synchronized(activeJobs) {
            activeJobs.add(job)
        }

        // Remove from tracking when complete
        job.invokeOnCompletion {
            synchronized(activeJobs) {
                activeJobs.remove(job)
            }
        }

        return job
    }

    /**
     * Cancel a coroutine scope and wait for all jobs to complete
     *
     * Uses cancelAndJoin() to ensure all coroutines are properly stopped
     * before returning. Handles exceptions during cancellation.
     *
     * @param scope The scope to cancel
     */
    suspend fun cancelAndJoinAll(scope: CoroutineScope) {
        if (isCancelled) {
            Log.d(tag, "Scope manager already cancelled")
            return
        }

        isCancelled = true
        Log.d(tag, "Cancelling coroutine scope and waiting for ${activeJobs.size} jobs...")

        val startTime = System.currentTimeMillis()

        try {
            // First, cancel the scope
            scope.cancel()
            Log.d(tag, "Scope cancelled, waiting for jobs to complete...")

            // Then wait for all tracked jobs to complete
            val jobsCopy = synchronized(activeJobs) {
                activeJobs.toList()
            }

            var successCount = 0
            var errorCount = 0

            for (job in jobsCopy) {
                try {
                    job.join()
                    successCount++
                } catch (e: Exception) {
                    errorCount++
                    Log.w(tag, "Error waiting for job to complete: ${e.javaClass.simpleName}", e)
                }
            }

            val duration = System.currentTimeMillis() - startTime

            Log.i(tag, "✓ Coroutine scope cancelled successfully:")
            Log.i(tag, "  - Jobs completed: $successCount")
            Log.i(tag, "  - Jobs with errors: $errorCount")
            Log.i(tag, "  - Duration: ${duration}ms")
            Log.i(tag, "  - Scope active: ${scope.isActive}")

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(tag, "✗ Error cancelling coroutine scope (duration: ${duration}ms)", e)

            // Still mark as cancelled even if error occurred
            isCancelled = true
        } finally {
            synchronized(activeJobs) {
                activeJobs.clear()
            }
        }
    }

    /**
     * Get the number of active jobs being tracked
     */
    fun getActiveJobCount(): Int {
        return synchronized(activeJobs) {
            activeJobs.size
        }
    }

    /**
     * Check if the scope manager is cancelled
     */
    fun isCancelled(): Boolean = isCancelled
}
