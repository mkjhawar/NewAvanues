/**
 * CleanupWorker.kt - WorkManager worker for periodic command cleanup
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Executes periodic cleanup of deprecated commands using WorkManager.
 * Runs weekly during device charging with battery not low.
 */

package com.augmentalis.voiceoscore.cleanup

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for automated command cleanup.
 *
 * ## Responsibilities:
 * - Execute periodic cleanup of deprecated commands
 * - Run during optimal conditions (charging, battery not low)
 * - Log cleanup results for monitoring
 * - Retry on transient failures
 *
 * ## Scheduling:
 * ```kotlin
 * // Schedule cleanup to run weekly
 * CleanupWorker.schedulePeriodicCleanup(context)
 *
 * // Cancel scheduled cleanup
 * CleanupWorker.cancelPeriodicCleanup(context)
 * ```
 *
 * ## Configuration:
 * - **Frequency**: Weekly (7 days)
 * - **Grace Period**: 30 days (configurable via INPUT_GRACE_PERIOD_DAYS)
 * - **User-Approved**: Preserved by default (configurable via INPUT_KEEP_USER_APPROVED)
 * - **Constraints**: Requires charging + battery not low
 *
 * @property context Android application context
 * @property params Worker parameters from WorkManager
 */
class CleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    /**
     * Execute cleanup work.
     *
     * ## Workflow:
     * 1. Get database instance (singleton pattern)
     * 2. Create CleanupManager with command repository
     * 3. Execute cleanup with configured parameters
     * 4. Log results for monitoring
     * 5. Return success/retry based on outcome
     *
     * ## Error Handling:
     * - Transient errors (database locked, etc.) → Result.retry()
     * - Permanent errors (invalid config, etc.) → Result.failure()
     * - Success (even if 0 commands deleted) → Result.success()
     *
     * @return Result indicating success, failure, or need to retry
     */
    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting periodic command cleanup")

            // Get input parameters
            val gracePeriodDays = inputData.getInt(INPUT_GRACE_PERIOD_DAYS, DEFAULT_GRACE_PERIOD_DAYS)
            val keepUserApproved = inputData.getBoolean(INPUT_KEEP_USER_APPROVED, DEFAULT_KEEP_USER_APPROVED)

            // Validate parameters
            if (gracePeriodDays <= 0) {
                Log.e(TAG, "Invalid gracePeriodDays: $gracePeriodDays")
                return Result.failure()
            }

            // Get database singleton instance
            val driverFactory = DatabaseDriverFactory(applicationContext)
            val database = VoiceOSDatabaseManager.getInstance(driverFactory)
            val commandRepo = database.generatedCommands

            // Create cleanup manager
            val cleanupManager = CleanupManager(commandRepo)

            // Execute cleanup
            val result = cleanupManager.executeCleanup(
                gracePeriodDays = gracePeriodDays,
                keepUserApproved = keepUserApproved,
                dryRun = false
            )

            // Log result
            Log.i(TAG, "Cleanup completed: $result")

            // Check for errors
            if (result.errors.isNotEmpty()) {
                Log.w(TAG, "Cleanup had ${result.errors.size} errors:")
                result.errors.forEach { error ->
                    Log.w(TAG, "  - $error")
                }
                // Don't retry if we got partial results - consider it success
                // Future improvement: differentiate transient vs permanent errors
            }

            // Log statistics
            Log.i(
                TAG,
                "Cleanup statistics: " +
                        "deleted=${result.deletedCount}, " +
                        "preserved=${result.preservedCount}, " +
                        "gracePeriod=${result.gracePeriodDays}d, " +
                        "keepUserApproved=${result.keepUserApproved}"
            )

            Result.success()
        } catch (e: IllegalArgumentException) {
            // Permanent error (invalid config) - don't retry
            Log.e(TAG, "Invalid cleanup configuration - not retrying", e)
            Result.failure()
        } catch (e: IllegalStateException) {
            // Permanent error (safety limit) - don't retry
            Log.e(TAG, "Cleanup safety limit triggered - not retrying", e)
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed with exception - will retry", e)
            // Retry on other exceptions (database might be temporarily unavailable)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CleanupWorker"

        /**
         * Unique work name for periodic cleanup.
         * Used to ensure only one cleanup job is scheduled.
         */
        const val WORK_NAME = "version_command_cleanup"

        /**
         * Input key for grace period in days.
         */
        private const val INPUT_GRACE_PERIOD_DAYS = "grace_period_days"

        /**
         * Input key for user-approved preservation flag.
         */
        private const val INPUT_KEEP_USER_APPROVED = "keep_user_approved"

        /**
         * Default grace period: 30 days.
         * Commands deprecated more than 30 days ago will be deleted.
         */
        private const val DEFAULT_GRACE_PERIOD_DAYS = 30

        /**
         * Default user-approved preservation: true.
         * User-approved commands are preserved even if deprecated.
         */
        private const val DEFAULT_KEEP_USER_APPROVED = true

        /**
         * Schedule periodic cleanup to run weekly.
         *
         * ## Schedule:
         * - Runs every 7 days
         * - Only when device is charging
         * - Only when battery is not low
         * - Uses KEEP policy (won't reschedule if already scheduled)
         *
         * ## Parameters:
         * You can customize cleanup behavior by modifying inputData:
         * ```kotlin
         * val customRequest = PeriodicWorkRequestBuilder<CleanupWorker>(7, TimeUnit.DAYS)
         *     .setInputData(workDataOf(
         *         "grace_period_days" to 14,  // 2-week grace period
         *         "keep_user_approved" to false  // Don't preserve user-approved
         *     ))
         *     .setConstraints(...)
         *     .build()
         * ```
         *
         * @param context Android application context
         */
        fun schedulePeriodicCleanup(context: Context) {
            Log.i(TAG, "Scheduling periodic cleanup (weekly, 30-day grace period)")

            val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
                repeatInterval = 7,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)  // Don't drain battery
                        .setRequiresCharging(true)       // Run during charging
                        .build()
                )
                // Note: Default input parameters use constants defined above
                // To customize, add .setInputData(workDataOf(...))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // Don't reschedule if already scheduled
                cleanupRequest
            )

            Log.i(TAG, "Periodic cleanup scheduled: $WORK_NAME")
        }

        /**
         * Cancel scheduled periodic cleanup.
         *
         * Useful for:
         * - User disables automatic cleanup in settings
         * - Testing cleanup manually
         * - Uninstalling/disabling VoiceOS
         *
         * @param context Android application context
         */
        fun cancelPeriodicCleanup(context: Context) {
            Log.i(TAG, "Cancelling periodic cleanup: $WORK_NAME")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Check if periodic cleanup is scheduled.
         *
         * Note: This method is temporarily disabled due to ListenableFuture dependency issues.
         * WorkManager's getWorkInfosForUniqueWork() requires Guava ListenableFuture.
         *
         * Alternative: Check WorkManager status directly via WorkManager.getInstance().getWorkInfos()
         * or use WorkManager observables.
         *
         * @param context Android application context
         * @return Always returns false (not implemented)
         */
        @Suppress("UNUSED_PARAMETER")
        fun isCleanupScheduled(context: Context): Boolean {
            Log.w(TAG, "isCleanupScheduled not implemented - requires Guava ListenableFuture dependency")
            return false

            // TODO: Implement using WorkManager observables or LiveData instead:
            // WorkManager.getInstance(context)
            //     .getWorkInfosForUniqueWorkLiveData(WORK_NAME)
            //     .observeForever { workInfos ->
            //         workInfos.any { !it.state.isFinished }
            //     }
        }
    }
}
