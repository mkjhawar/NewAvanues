/**
 * LLM Model Downloader for AVA
 *
 * Specialized downloader for large LLM models (2-4GB) from HuggingFace Hub.
 * Provides background downloads with WorkManager, progress tracking, and resume support.
 *
 * Features:
 * - HuggingFace Hub integration
 * - Large file support (2-4GB+) with resume
 * - WorkManager for background downloads
 * - Progress tracking per model
 * - Storage space validation
 * - Concurrent download management (max 2)
 * - Automatic model discovery after download
 *
 * Created: 2025-12-06
 * Author: AVA AI Team
 */

package com.augmentalis.llm.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.augmentalis.llm.LLMResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * LLM Model Downloader
 *
 * Orchestrates downloads of large LLM models using WorkManager for reliability.
 *
 * @property context Android context
 * @property workManager WorkManager instance for background downloads
 */
class LLMModelDownloader(
    private val context: Context,
    private val workManager: WorkManager
) {

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val huggingFaceClient = HuggingFaceClient(context)
    private val storageManager = ModelStorageManager(context)

    companion object {
        private const val MAX_CONCURRENT_DOWNLOADS = 2
        private const val WORK_TAG_PREFIX = "llm_download_"
    }

    /**
     * Model information from HuggingFace Hub
     */
    data class ModelInfo(
        val modelId: String,
        val name: String,
        val sizeBytes: Long,
        val url: String,
        val checksum: String?,
        val description: String? = null,
        val quantization: String? = null,
        val runtime: String = "llama.cpp"
    ) {
        fun getDisplaySize(): String {
            return when {
                sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024}KB"
                sizeBytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", sizeBytes / (1024.0 * 1024.0))}MB"
                else -> "${String.format("%.2f", sizeBytes / (1024.0 * 1024.0 * 1024.0))}GB"
            }
        }
    }

    /**
     * Download state for LLM models
     */
    sealed class DownloadState {
        object Idle : DownloadState()

        data class Downloading(
            val modelId: String,
            val progress: Float,
            val bytesDownloaded: Long,
            val totalBytes: Long,
            val speedBytesPerSec: Long,
            val estimatedTimeRemaining: Long
        ) : DownloadState() {
            fun getProgressPercentage(): Int = (progress * 100).toInt()

            fun getSpeedMBPerSec(): Float = speedBytesPerSec / (1024f * 1024f)

            fun getETAMinutes(): Int = (estimatedTimeRemaining / 60000).toInt()
        }

        data class Success(val modelId: String, val path: String) : DownloadState()

        data class Failed(
            val modelId: String,
            val error: String,
            val retryable: Boolean
        ) : DownloadState()
    }

    /**
     * Get list of available models for download from HuggingFace
     *
     * @return List of available model information
     */
    suspend fun getAvailableModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            huggingFaceClient.listAvailableModels()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch available models")
            emptyList()
        }
    }

    /**
     * Download a model using WorkManager
     *
     * Creates a background work request that survives app restarts.
     * Supports resume if interrupted.
     *
     * @param modelInfo Model information to download
     * @param resumeIfPossible Whether to resume partial downloads
     * @return LLMResult indicating if download was started
     */
    suspend fun downloadModel(
        modelInfo: ModelInfo,
        resumeIfPossible: Boolean = true
    ): LLMResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check storage space
            if (!storageManager.hasEnoughSpace(modelInfo.sizeBytes)) {
                val shortfall = modelInfo.sizeBytes - storageManager.getAvailableSpace()
                return@withContext LLMResult.Error(
                    message = "Need ${formatBytes(shortfall)} more storage space",
                    cause = IllegalStateException("Insufficient storage")
                )
            }

            // Check concurrent download limit
            val activeDownloads = getActiveDownloadCount()
            if (activeDownloads >= MAX_CONCURRENT_DOWNLOADS) {
                return@withContext LLMResult.Error(
                    message = "Maximum $MAX_CONCURRENT_DOWNLOADS concurrent downloads allowed",
                    cause = IllegalStateException("Too many downloads")
                )
            }

            // Create work request
            val workData = Data.Builder()
                .putString("model_id", modelInfo.modelId)
                .putString("model_name", modelInfo.name)
                .putString("model_url", modelInfo.url)
                .putLong("size_bytes", modelInfo.sizeBytes)
                .putString("checksum", modelInfo.checksum)
                .putBoolean("resume", resumeIfPossible)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build()

            val downloadRequest = OneTimeWorkRequestBuilder<LLMDownloadWorker>()
                .setInputData(workData)
                .setConstraints(constraints)
                .addTag("$WORK_TAG_PREFIX${modelInfo.modelId}")
                .build()

            // Enqueue work
            workManager.enqueueUniqueWork(
                "download_${modelInfo.modelId}",
                ExistingWorkPolicy.KEEP, // Keep existing if already downloading
                downloadRequest
            )

            // Update state
            updateDownloadState(modelInfo.modelId, DownloadState.Downloading(
                modelId = modelInfo.modelId,
                progress = 0f,
                bytesDownloaded = 0L,
                totalBytes = modelInfo.sizeBytes,
                speedBytesPerSec = 0L,
                estimatedTimeRemaining = 0L
            ))

            Timber.i("Started download: ${modelInfo.modelId} (${modelInfo.getDisplaySize()})")
            LLMResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to start download: ${modelInfo.modelId}")
            LLMResult.Error(
                message = "Failed to start download: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Cancel an active download
     *
     * @param modelId Model identifier
     */
    suspend fun cancelDownload(modelId: String) {
        try {
            workManager.cancelUniqueWork("download_$modelId")
            updateDownloadState(modelId, DownloadState.Failed(
                modelId = modelId,
                error = "Download cancelled by user",
                retryable = true
            ))
            Timber.i("Cancelled download: $modelId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel download: $modelId")
        }
    }

    /**
     * Delete a downloaded model
     *
     * @param modelId Model identifier
     * @return LLMResult indicating success or failure
     */
    suspend fun deleteModel(modelId: String): LLMResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val deleted = storageManager.deleteModel(modelId)
            if (deleted) {
                Timber.i("Deleted model: $modelId")
                LLMResult.Success(Unit)
            } else {
                LLMResult.Error(
                    message = "Failed to delete model: $modelId",
                    cause = IllegalStateException("Delete failed")
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting model: $modelId")
            LLMResult.Error(
                message = "Failed to delete model: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Get list of installed models
     *
     * @return List of installed model IDs with sizes
     */
    suspend fun getInstalledModels(): List<Pair<String, Long>> = withContext(Dispatchers.IO) {
        storageManager.getInstalledModels()
    }

    /**
     * Observe download progress for a specific model
     *
     * Monitors WorkManager progress and updates state flow.
     *
     * @param modelId Model identifier
     */
    fun observeDownloadProgress(modelId: String) {
        val workInfoFlow = workManager.getWorkInfosForUniqueWorkFlow("download_$modelId")

        // Note: In production, collect this flow and update _downloadStates
        // For now, WorkManager will handle persistence
    }

    /**
     * Update download state
     */
    private fun updateDownloadState(modelId: String, state: DownloadState) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            this[modelId] = state
        }
    }

    /**
     * Get count of active downloads
     */
    private suspend fun getActiveDownloadCount(): Int = withContext(Dispatchers.IO) {
        val workInfos = workManager.getWorkInfosByTag("$WORK_TAG_PREFIX")
            .get() // Synchronous call is fine in IO dispatcher

        workInfos.count { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    }

    /**
     * Format bytes to human-readable string
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
