/**
 * Model Download Manager for AVA
 *
 * Orchestrates on-demand ML model downloads with progress tracking.
 *
 * Features:
 * - Download models from various sources (HTTP, Hugging Face, Firebase, GCS)
 * - Flow-based progress tracking
 * - Pause/Resume support
 * - Checksum verification
 * - Network error handling with retry
 * - Concurrent download management
 * - Cache-first loading
 *
 * Reduces APK size from 160MB to ~8MB by downloading models on-demand.
 *
 * Usage:
 * ```
 * val manager = ModelDownloadManager(context, cacheManager)
 *
 * // Download with progress tracking
 * manager.downloadModel(config)
 *     .collect { state ->
 *         when (state) {
 *             is DownloadState.Downloading -> {
 *                 println("Progress: ${state.getProgressPercentage()}%")
 *             }
 *             is DownloadState.Completed -> {
 *                 println("Downloaded to: ${state.filePath}")
 *             }
 *             is DownloadState.Error -> {
 *                 println("Error: ${state.message}")
 *             }
 *         }
 *     }
 * ```
 *
 * Created: 2025-11-03
 * Author: AVA AI Team
 */

package com.augmentalis.ava.features.llm.download

import android.content.Context
import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Model download manager
 *
 * @property context Android context
 * @property cacheManager Model cache manager
 */
class ModelDownloadManager(
    private val context: Context,
    private val cacheManager: ModelCacheManager
) {

    private val activeDownloads = mutableMapOf<String, DownloadJob>()
    private val downloadMutex = Mutex()

    /**
     * Download a model with progress tracking
     *
     * @param config Model download configuration
     * @param forceRedownload Force redownload even if cached
     * @return Flow of download states
     */
    fun downloadModel(
        config: ModelDownloadConfig,
        forceRedownload: Boolean = false
    ): Flow<DownloadState> = flow {
        val modelId = config.id

        try {
            // Check if already cached
            if (!forceRedownload && cacheManager.isModelCached(modelId)) {
                Timber.i("Model already cached: $modelId")
                val filePath = cacheManager.getModelPath(modelId)
                    ?: throw IllegalStateException("Model cached but path not found")

                emit(
                    DownloadState.Completed(
                        modelId = modelId,
                        filePath = filePath,
                        fileSize = File(filePath).length()
                    )
                )
                return@flow
            }

            // Check storage space
            if (!cacheManager.hasEnoughSpace(config.size)) {
                emit(
                    DownloadState.Error(
                        modelId = modelId,
                        error = IllegalStateException("Insufficient storage"),
                        message = "Not enough storage space. Required: ${config.getFormattedSize()}",
                        code = ErrorCode.INSUFFICIENT_STORAGE,
                        canRetry = false
                    )
                )
                return@flow
            }

            // Register active download
            val job = DownloadJob(modelId, config)
            downloadMutex.withLock {
                if (activeDownloads.containsKey(modelId)) {
                    emit(
                        DownloadState.Error(
                            modelId = modelId,
                            error = IllegalStateException("Download already in progress"),
                            message = "Download already in progress for: $modelId",
                            code = ErrorCode.INVALID_CONFIG,
                            canRetry = false
                        )
                    )
                    return@flow
                }
                activeDownloads[modelId] = job
            }

            try {
                // Perform download
                downloadModelInternal(config, job).collect { state ->
                    emit(state)
                }
            } finally {
                // Cleanup
                downloadMutex.withLock {
                    activeDownloads.remove(modelId)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Error downloading model: $modelId")
            emit(
                DownloadState.Error(
                    modelId = modelId,
                    error = e,
                    message = "Download failed: ${e.message}",
                    code = ErrorCode.UNKNOWN,
                    canRetry = true
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Internal download implementation
     */
    private fun downloadModelInternal(
        config: ModelDownloadConfig,
        job: DownloadJob
    ): Flow<DownloadState> = flow {
        val modelId = config.id
        val modelDir = cacheManager.getModelDirectory(modelId)
        modelDir.mkdirs()

        // Determine filename from URL
        val url = config.getDownloadUrl()
        val filename = url.substringAfterLast('/').ifEmpty { "model.bin" }
        val outputFile = File(modelDir, filename)
        val tempFile = File(modelDir, "$filename.tmp")

        try {
            // Initialize connection
            val connection = URL(url).openConnection() as? HttpURLConnection
                ?: throw IllegalStateException("Failed to open HTTP connection")

            connection.apply {
                connectTimeout = 30000
                readTimeout = 30000
                requestMethod = "GET"

                // Add headers for resume support
                if (tempFile.exists() && job.bytesDownloaded > 0) {
                    setRequestProperty("Range", "bytes=${job.bytesDownloaded}-")
                }

                // Add custom headers from config
                if (config.source is DownloadSource.Http) {
                    config.source.headers.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                }
            }

            connection.connect()

            // Check response code
            val responseCode = connection.responseCode
            if (responseCode !in 200..299 && responseCode != 206) {
                throw IllegalStateException("HTTP error: $responseCode")
            }

            // Get file size
            val contentLength = connection.contentLength.toLong()
            val totalBytes = if (responseCode == 206) {
                // Partial content - extract total from Content-Range
                val contentRange = connection.getHeaderField("Content-Range")
                contentRange?.substringAfterLast('/')?.toLongOrNull() ?: (contentLength + job.bytesDownloaded)
            } else {
                contentLength
            }

            // Validate size
            if (config.size > 0 && totalBytes != config.size) {
                Timber.w("Size mismatch - Expected: ${config.size}, Actual: $totalBytes")
            }

            // Download with progress
            val input = connection.inputStream
            val output = FileOutputStream(tempFile, job.bytesDownloaded > 0) // Append if resuming
            val buffer = ByteArray(8192)

            var bytesDownloaded = job.bytesDownloaded
            var lastEmitTime = System.currentTimeMillis()
            var lastBytesDownloaded = bytesDownloaded
            val startTime = System.currentTimeMillis()

            while (!job.isCancelled) {
                // Check if paused
                if (job.isPaused) {
                    emit(
                        DownloadState.Paused(
                            modelId = modelId,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            progress = bytesDownloaded.toFloat() / totalBytes
                        )
                    )
                    // Wait for resume
                    while (job.isPaused && !job.isCancelled) {
                        kotlinx.coroutines.delay(100)
                    }
                    continue
                }

                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break

                output.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead
                job.bytesDownloaded = bytesDownloaded

                // Emit progress (throttled to every 100ms)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastEmitTime >= 100) {
                    val elapsed = currentTime - lastEmitTime
                    val deltaBytes = bytesDownloaded - lastBytesDownloaded
                    val speed = if (elapsed > 0) (deltaBytes * 1000) / elapsed else 0L
                    val remainingBytes = totalBytes - bytesDownloaded
                    val eta = if (speed > 0) (remainingBytes * 1000) / speed else 0L

                    emit(
                        DownloadState.Downloading(
                            modelId = modelId,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            progress = bytesDownloaded.toFloat() / totalBytes,
                            speedBytesPerSecond = speed,
                            estimatedTimeRemainingMs = eta
                        )
                    )

                    lastEmitTime = currentTime
                    lastBytesDownloaded = bytesDownloaded
                }
            }

            output.close()
            input.close()

            // Check if cancelled
            if (job.isCancelled) {
                emit(
                    DownloadState.Error(
                        modelId = modelId,
                        error = IllegalStateException("Download cancelled"),
                        message = "Download cancelled by user",
                        code = ErrorCode.CANCELLED,
                        canRetry = true,
                        bytesDownloaded = bytesDownloaded
                    )
                )
                return@flow
            }

            // Verify checksum if provided
            if (config.checksum != null) {
                emit(
                    DownloadState.Downloading(
                        modelId = modelId,
                        bytesDownloaded = totalBytes,
                        totalBytes = totalBytes,
                        progress = 0.99f // Show progress during verification
                    )
                )

                val actualChecksum = calculateChecksum(tempFile)
                if (!actualChecksum.equals(config.checksum, ignoreCase = true)) {
                    tempFile.delete()
                    emit(
                        DownloadState.Error(
                            modelId = modelId,
                            error = IllegalStateException("Checksum mismatch"),
                            message = "Checksum verification failed",
                            code = ErrorCode.CHECKSUM_MISMATCH,
                            canRetry = true
                        )
                    )
                    return@flow
                }
            }

            // Move temp file to final location
            tempFile.renameTo(outputFile)

            val downloadDuration = System.currentTimeMillis() - startTime

            emit(
                DownloadState.Completed(
                    modelId = modelId,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    checksum = config.checksum,
                    downloadDurationMs = downloadDuration
                )
            )

            Timber.i("Download completed: $modelId (${config.getFormattedSize()}) in ${downloadDuration}ms")

        } catch (e: Exception) {
            Timber.e(e, "Error during download: $modelId")

            // Determine error code
            val errorCode = when {
                e is java.net.SocketTimeoutException -> ErrorCode.TIMEOUT
                e is java.net.UnknownHostException -> ErrorCode.NETWORK_ERROR
                e is java.io.IOException -> ErrorCode.IO_ERROR
                e.message?.contains("HTTP") == true -> ErrorCode.HTTP_ERROR
                else -> ErrorCode.UNKNOWN
            }

            emit(
                DownloadState.Error(
                    modelId = modelId,
                    error = e,
                    message = "Download failed: ${e.message}",
                    code = errorCode,
                    canRetry = true,
                    bytesDownloaded = job.bytesDownloaded
                )
            )

            // Keep temp file for resume
        }
    }

    /**
     * Pause a download
     *
     * @param modelId Model identifier
     * @return Result indicating success or failure
     */
    suspend fun pauseDownload(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            downloadMutex.withLock {
                val job = activeDownloads[modelId]
                    ?: return@withContext Result.Error(
                        exception = IllegalStateException("No active download"),
                        message = "No active download for: $modelId"
                    )

                job.isPaused = true
                Timber.i("Paused download: $modelId")
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to pause download: ${e.message}"
            )
        }
    }

    /**
     * Resume a paused download
     *
     * @param modelId Model identifier
     * @return Result indicating success or failure
     */
    suspend fun resumeDownload(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            downloadMutex.withLock {
                val job = activeDownloads[modelId]
                    ?: return@withContext Result.Error(
                        exception = IllegalStateException("No active download"),
                        message = "No active download for: $modelId"
                    )

                job.isPaused = false
                Timber.i("Resumed download: $modelId")
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to resume download: ${e.message}"
            )
        }
    }

    /**
     * Cancel a download
     *
     * @param modelId Model identifier
     * @return Result indicating success or failure
     */
    suspend fun cancelDownload(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            downloadMutex.withLock {
                val job = activeDownloads[modelId]
                    ?: return@withContext Result.Error(
                        exception = IllegalStateException("No active download"),
                        message = "No active download for: $modelId"
                    )

                job.isCancelled = true
                Timber.i("Cancelled download: $modelId")
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to cancel download: ${e.message}"
            )
        }
    }

    /**
     * Get active downloads
     *
     * @return Map of model ID to download job
     */
    suspend fun getActiveDownloads(): Map<String, DownloadJob> = downloadMutex.withLock {
        activeDownloads.toMap()
    }

    /**
     * Check if model is being downloaded
     *
     * @param modelId Model identifier
     * @return True if download is active
     */
    suspend fun isDownloading(modelId: String): Boolean = downloadMutex.withLock {
        activeDownloads.containsKey(modelId)
    }

    /**
     * Download multiple models sequentially
     *
     * @param configs List of model configurations
     * @return Flow of download states for all models
     */
    fun downloadModels(
        configs: List<ModelDownloadConfig>
    ): Flow<DownloadState> = flow {
        for (config in configs) {
            downloadModel(config).collect { state ->
                emit(state)

                // Log error if model is required (but continue to let caller handle)
                if (state is DownloadState.Error && config.isRequired) {
                    Timber.e("Required model download failed: ${config.id}")
                }
            }
        }
    }

    /**
     * Ensure model is available (download if needed, use cache if available)
     *
     * @param config Model configuration
     * @return Flow of download states
     */
    fun ensureModelAvailable(config: ModelDownloadConfig): Flow<DownloadState> = flow {
        if (cacheManager.isModelCached(config.id)) {
            val filePath = cacheManager.getModelPath(config.id)
                ?: throw IllegalStateException("Model cached but path not found")

            emit(
                DownloadState.Completed(
                    modelId = config.id,
                    filePath = filePath,
                    fileSize = File(filePath).length()
                )
            )
        } else {
            downloadModel(config).collect { state ->
                emit(state)
            }
        }
    }

    /**
     * Calculate SHA-256 checksum
     */
    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)

        file.inputStream().use { input ->
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

/**
 * Download job tracking
 */
data class DownloadJob(
    val modelId: String,
    val config: ModelDownloadConfig,
    var bytesDownloaded: Long = 0L,
    var isPaused: Boolean = false,
    var isCancelled: Boolean = false
)
