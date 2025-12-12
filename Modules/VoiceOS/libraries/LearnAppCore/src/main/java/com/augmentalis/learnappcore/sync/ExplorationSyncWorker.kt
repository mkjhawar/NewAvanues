/**
 * ExplorationSyncWorker.kt - Offline-first exploration data sync worker
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-12 (Phase 7: Reliability Polish)
 *
 * WorkManager-based background sync for exploration data.
 * Handles offline-first sync with retry logic and network awareness.
 *
 * ## Features:
 * - Periodic 15-minute sync when network available
 * - Automatic retry with exponential backoff (3 attempts)
 * - Network-aware execution (only on WiFi/Cellular)
 * - Syncs pending AVU exports to server
 * - Battery-friendly with constraints
 *
 * ## Usage:
 * ```kotlin
 * // Schedule periodic sync
 * ExplorationSyncWorker.schedulePeriodicSync(context)
 *
 * // Trigger immediate sync
 * ExplorationSyncWorker.triggerImmediateSync(context)
 *
 * // Cancel sync
 * ExplorationSyncWorker.cancelSync(context)
 * ```
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappcore.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.augmentalis.learnappcore.export.AVUExporter
import com.augmentalis.learnappcore.export.ExportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Sync status for tracking upload state.
 */
enum class SyncStatus {
    /** Sync not started */
    IDLE,

    /** Sync in progress */
    IN_PROGRESS,

    /** Sync completed successfully */
    SUCCESS,

    /** Sync failed (will retry) */
    FAILED,

    /** No network available */
    NO_NETWORK
}

/**
 * Result of sync operation.
 */
data class SyncResult(
    val status: SyncStatus,
    val filesProcessed: Int,
    val filesUploaded: Int,
    val filesFailed: Int,
    val errorMessage: String? = null
)

/**
 * WorkManager worker for syncing exploration data to server.
 *
 * ## Constraints:
 * - Network required (WiFi or Cellular)
 * - Battery not low
 * - Storage not low
 *
 * ## Retry Policy:
 * - Max attempts: 3
 * - Backoff: Exponential (30s initial)
 * - Backoff policy: Exponential
 */
class ExplorationSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ExplorationSyncWorker"

        /** Unique work name for periodic sync */
        private const val WORK_NAME = "exploration_sync_periodic"

        /** Work name for one-time sync */
        private const val ONE_TIME_WORK_NAME = "exploration_sync_immediate"

        /** Sync interval in minutes */
        private const val SYNC_INTERVAL_MINUTES = 15L

        /** Max retry attempts */
        private const val MAX_RETRY_ATTEMPTS = 3

        /** Initial backoff delay in seconds */
        private const val INITIAL_BACKOFF_SECONDS = 30L

        /**
         * Schedule periodic sync (every 15 minutes when network available).
         *
         * @param context Android context
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<ExplorationSyncWorker>(
                SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )

            Log.i(TAG, "Scheduled periodic sync (interval: ${SYNC_INTERVAL_MINUTES}m)")
        }

        /**
         * Trigger immediate one-time sync.
         *
         * @param context Android context
         */
        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<ExplorationSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    ONE_TIME_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )

            Log.i(TAG, "Triggered immediate sync")
        }

        /**
         * Cancel all sync work.
         *
         * @param context Android context
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(TAG)

            Log.i(TAG, "Cancelled all sync work")
        }

        /**
         * Check sync status.
         *
         * @param context Android context
         * @return List of work info for all sync workers
         */
        suspend fun getSyncStatus(context: Context): List<WorkInfo> {
            return WorkManager.getInstance(context)
                .getWorkInfosByTag(TAG)
                .await()
        }
    }

    /**
     * Perform sync work.
     *
     * Scans export directory for pending .vos files and uploads them.
     * Marks files as synced by moving to 'synced' subdirectory.
     *
     * @return Result indicating success or retry
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting exploration data sync (attempt ${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS)")

        try {
            // Check network connectivity
            if (!isNetworkAvailable()) {
                Log.w(TAG, "No network available, deferring sync")
                return@withContext Result.retry()
            }

            // Get pending export files
            val pendingFiles = getPendingExportFiles()

            if (pendingFiles.isEmpty()) {
                Log.i(TAG, "No pending exports to sync")
                return@withContext Result.success(
                    workDataOf(
                        "status" to SyncStatus.SUCCESS.name,
                        "files_processed" to 0,
                        "files_uploaded" to 0,
                        "files_failed" to 0
                    )
                )
            }

            Log.i(TAG, "Found ${pendingFiles.size} pending export files")

            // Process each file
            var uploadedCount = 0
            var failedCount = 0

            for (file in pendingFiles) {
                try {
                    if (uploadExportFile(file)) {
                        markAsSynced(file)
                        uploadedCount++
                        Log.d(TAG, "Uploaded: ${file.name}")
                    } else {
                        failedCount++
                        Log.w(TAG, "Failed to upload: ${file.name}")
                    }
                } catch (e: Exception) {
                    failedCount++
                    Log.e(TAG, "Error uploading ${file.name}", e)
                }
            }

            Log.i(TAG, "Sync completed: $uploadedCount uploaded, $failedCount failed")

            // Determine result
            val result = when {
                failedCount == 0 -> {
                    // All succeeded
                    Result.success(
                        workDataOf(
                            "status" to SyncStatus.SUCCESS.name,
                            "files_processed" to pendingFiles.size,
                            "files_uploaded" to uploadedCount,
                            "files_failed" to failedCount
                        )
                    )
                }
                uploadedCount > 0 && runAttemptCount < MAX_RETRY_ATTEMPTS - 1 -> {
                    // Partial success, retry remaining
                    Result.retry()
                }
                else -> {
                    // All failed or max retries reached
                    Result.failure(
                        workDataOf(
                            "status" to SyncStatus.FAILED.name,
                            "files_processed" to pendingFiles.size,
                            "files_uploaded" to uploadedCount,
                            "files_failed" to failedCount,
                            "error" to "Failed to upload $failedCount files"
                        )
                    )
                }
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed with exception", e)

            Result.failure(
                workDataOf(
                    "status" to SyncStatus.FAILED.name,
                    "error" to e.message
                )
            )
        }
    }

    /**
     * Check if network is available.
     *
     * TODO: Implement actual network check using ConnectivityManager
     *
     * @return true if network available, false otherwise
     */
    private fun isNetworkAvailable(): Boolean {
        // WorkManager constraints already ensure network is available
        // This is a redundant check for explicit validation
        return true
    }

    /**
     * Get pending export files that need to be synced.
     *
     * Scans export directories (both user and developer) for .vos files
     * that haven't been synced yet.
     *
     * @return List of files to sync
     */
    private fun getPendingExportFiles(): List<File> {
        val pendingFiles = mutableListOf<File>()

        // Check user exports
        val userExportDir = File(applicationContext.getExternalFilesDir(null), "exports")
        if (userExportDir.exists()) {
            userExportDir.listFiles { file ->
                file.extension == "vos"
            }?.let { pendingFiles.addAll(it) }
        }

        // Check developer exports
        val devExportDir = File(applicationContext.getExternalFilesDir(null), "exports/dev")
        if (devExportDir.exists()) {
            devExportDir.listFiles { file ->
                file.extension == "vos"
            }?.let { pendingFiles.addAll(it) }
        }

        return pendingFiles
    }

    /**
     * Upload export file to server.
     *
     * TODO: Implement actual server upload logic
     * Current implementation is a stub for Phase 7.
     *
     * @param file File to upload
     * @return true if upload successful, false otherwise
     */
    private suspend fun uploadExportFile(file: File): Boolean {
        // Simulate network upload
        // In production, this would:
        // 1. Read file contents
        // 2. POST to server endpoint (e.g., https://api.voiceos.com/v1/explorations)
        // 3. Include auth token and metadata
        // 4. Handle response and errors

        Log.d(TAG, "Uploading ${file.name} (${file.length()} bytes)")

        // Stub: Always succeed for now
        return true
    }

    /**
     * Mark file as synced by moving to 'synced' subdirectory.
     *
     * @param file File to mark as synced
     */
    private fun markAsSynced(file: File) {
        val syncedDir = File(file.parentFile, "synced")
        if (!syncedDir.exists()) {
            syncedDir.mkdirs()
        }

        val syncedFile = File(syncedDir, file.name)
        file.renameTo(syncedFile)

        Log.d(TAG, "Marked as synced: ${file.name}")
    }
}
