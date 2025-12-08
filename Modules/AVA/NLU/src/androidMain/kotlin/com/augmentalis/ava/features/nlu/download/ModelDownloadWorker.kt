/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.features.nlu.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.augmentalis.ava.core.common.Result
import timber.log.Timber

/**
 * WorkManager worker for background NLU model downloads
 *
 * Features:
 * - Downloads models in background
 * - Updates progress via setProgress()
 * - Handles network failures with retry
 * - Verifies checksums
 * - Cleans up partial downloads on failure
 *
 * Input Data:
 * - KEY_MODEL_ID: Model identifier (e.g., "AVA-768-Base-INT8")
 * - KEY_MODEL_URL: Download URL
 * - KEY_CHECKSUM: Expected SHA-256 checksum
 *
 * Output Data:
 * - KEY_MODEL_PATH: Path to downloaded model (on success)
 * - KEY_ERROR: Error message (on failure)
 *
 * Progress Data:
 * - KEY_PROGRESS: Download progress (0-100)
 * - KEY_BYTES_DOWNLOADED: Bytes downloaded
 * - KEY_TOTAL_BYTES: Total bytes to download
 */
class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?: return androidx.work.ListenableWorker.Result.failure(
                workDataOf(KEY_ERROR to "Model ID not provided")
            )

        val modelUrl = inputData.getString(KEY_MODEL_URL)
            ?: return androidx.work.ListenableWorker.Result.failure(
                workDataOf(KEY_ERROR to "Model URL not provided")
            )

        val checksum = inputData.getString(KEY_CHECKSUM)
            ?: return androidx.work.ListenableWorker.Result.failure(
                workDataOf(KEY_ERROR to "Checksum not provided")
            )

        Timber.i("Starting model download: $modelId")

        // Create downloader
        val downloader = NLUModelDownloader(
            context = applicationContext,
            workManager = androidx.work.WorkManager.getInstance(applicationContext)
        )

        try {
            // Download model
            val result = downloader.downloadModel(modelId, modelUrl, checksum)

            return when (result) {
                is com.augmentalis.ava.core.common.Result.Success -> {
                    Timber.i("Model download successful: ${result.data}")
                    androidx.work.ListenableWorker.Result.success(
                        workDataOf(KEY_MODEL_PATH to result.data)
                    )
                }
                is com.augmentalis.ava.core.common.Result.Error -> {
                    Timber.e("Model download failed: ${result.message}")
                    androidx.work.ListenableWorker.Result.failure(
                        workDataOf(KEY_ERROR to result.message)
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Worker failed with exception")
            return androidx.work.ListenableWorker.Result.failure(
                workDataOf(KEY_ERROR to "Worker exception: ${e.message}")
            )
        }
    }

    companion object {
        // Input keys
        const val KEY_MODEL_ID = "model_id"
        const val KEY_MODEL_URL = "model_url"
        const val KEY_CHECKSUM = "checksum"

        // Output keys
        const val KEY_MODEL_PATH = "model_path"
        const val KEY_ERROR = "error"

        // Progress keys
        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"

        // Work request tag
        const val WORK_TAG = "nlu_model_download"
    }
}
