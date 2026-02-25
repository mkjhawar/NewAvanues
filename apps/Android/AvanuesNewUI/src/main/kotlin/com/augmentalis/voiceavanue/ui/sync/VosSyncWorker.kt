/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 *
 * Created: 2026-02-11
 */

package com.augmentalis.voiceavanue.ui.sync

import android.content.Context
import android.os.Environment
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.augmentalis.voiceoscore.vos.sync.SftpAuthMode
import com.augmentalis.voiceoscore.vos.sync.SftpResult
import com.augmentalis.voiceoscore.vos.sync.VosSyncManager
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.data.SftpCredentialStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for periodic VOS file SFTP synchronization.
 *
 * Reads configuration from [AvanuesSettingsRepository] and triggers
 * [VosSyncManager.syncAll] to download .vos files from remote SFTP server.
 *
 * Scheduling:
 * - Periodic: [schedulePeriodic] with configurable interval
 * - One-time: [triggerImmediate] for manual sync
 *
 * Retry policy:
 * - Up to 3 attempts with exponential backoff (30s initial)
 * - Requires connected network and non-low battery
 */
@HiltWorker
class VosSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: VosSyncManager,
    private val settingsRepository: AvanuesSettingsRepository,
    private val credentialStore: SftpCredentialStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Read settings
            val settings = settingsRepository.settings.first()

            // Check if sync is enabled and configured
            if (!settings.vosSyncEnabled) {
                return Result.success()
            }

            val host = settings.vosSftpHost.takeIf { it.isNotBlank() } ?: return Result.success()
            val port = settings.vosSftpPort
            val username = settings.vosSftpUsername.takeIf { it.isNotBlank() } ?: return Result.success()
            val remotePath = settings.vosSftpRemotePath.takeIf { it.isNotBlank() } ?: return Result.success()

            // Build auth mode using encrypted credential store
            val authMode = if (settings.vosSftpKeyPath.isNotBlank()) {
                SftpAuthMode.SshKey(settings.vosSftpKeyPath, credentialStore.getPassphrase())
            } else {
                SftpAuthMode.Password(credentialStore.getPassword())
            }

            // Prepare download directory
            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "commands"
            )

            // Execute sync
            val result = syncManager.syncAll(
                host = host,
                port = port,
                username = username,
                authMode = authMode,
                remotePath = remotePath,
                downloadDir = downloadDir.absolutePath,
                hostKeyChecking = settings.vosSftpHostKeyMode
            )

            when (result) {
                is SftpResult.Success -> {
                    Result.success()
                }
                is SftpResult.Error -> {
                    // Retry up to 3 times
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            // Retry on exceptions
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "vos_sync"

        /**
         * Schedule periodic VOS sync with specified interval.
         *
         * @param context Android context
         * @param intervalHours Sync interval in hours (minimum 1 hour per WorkManager constraints)
         */
        fun schedulePeriodic(context: Context, intervalHours: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<VosSyncWorker>(
                repeatInterval = intervalHours,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    backoffPolicy = BackoffPolicy.EXPONENTIAL,
                    backoffDelay = 30,
                    timeUnit = TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Trigger immediate one-time VOS sync.
         *
         * @param context Android context
         */
        fun triggerImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<VosSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    backoffPolicy = BackoffPolicy.EXPONENTIAL,
                    backoffDelay = 30,
                    timeUnit = TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        /**
         * Cancel all scheduled VOS sync work.
         *
         * @param context Android context
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
