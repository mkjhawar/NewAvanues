/**
 * LLM Download Worker for AVA
 *
 * Background worker for downloading large LLM models (2-4GB).
 * Uses WorkManager for reliable downloads that survive app restarts.
 *
 * Features:
 * - Chunked download with HTTP Range requests for resume support
 * - Progress tracking with foreground notification
 * - SHA-256 checksum verification
 * - Automatic retry with exponential backoff
 * - Storage space validation
 * - Model discovery trigger after completion
 *
 * Created: 2025-12-06
 * Author: AVA AI Team
 */

package com.augmentalis.ava.features.llm.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.augmentalis.ava.features.llm.alc.loader.ModelDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Worker for downloading large LLM models
 */
class LLMDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val storageManager = ModelStorageManager(applicationContext)
    private val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "llm_downloads"
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString("model_id") ?: return@withContext Result.failure(
            workDataOf("error" to "Missing model_id")
        )

        val modelName = inputData.getString("model_name") ?: modelId
        val modelUrl = inputData.getString("model_url") ?: return@withContext Result.failure(
            workDataOf("error" to "Missing model_url")
        )

        val sizeBytes = inputData.getLong("size_bytes", 0)
        val checksum = inputData.getString("checksum")
        val resume = inputData.getBoolean("resume", true)

        Timber.i("Starting LLM download: $modelId ($modelName)")

        try {
            // Check storage space
            if (!storageManager.hasEnoughSpace(sizeBytes)) {
                return@withContext Result.failure(
                    workDataOf("error" to "Insufficient storage space")
                )
            }

            // Set foreground notification
            setForeground(createForegroundInfo(modelName, 0))

            // Download with resume support
            val outputFile = storageManager.getModelFile(modelId)
            val downloadResult = downloadWithResume(
                url = modelUrl,
                outputFile = outputFile,
                expectedSize = sizeBytes,
                resume = resume,
                onProgress = { progress, bytesDownloaded, totalBytes, speed ->
                    // Update notification (launch coroutine since setForeground is suspend)
                    GlobalScope.launch {
                        setForeground(createForegroundInfo(modelName, progress, speed))
                    }

                    // Update WorkManager progress
                    setProgressAsync(workDataOf(
                        "model_id" to modelId,
                        "progress" to progress,
                        "bytes_downloaded" to bytesDownloaded,
                        "total_bytes" to totalBytes,
                        "speed_bytes_per_sec" to speed
                    ))
                }
            )

            if (!downloadResult) {
                return@withContext Result.failure(
                    workDataOf("error" to "Download failed")
                )
            }

            // Verify checksum if provided
            if (checksum != null && checksum.isNotEmpty()) {
                Timber.i("Verifying checksum for $modelId")
                setForeground(createForegroundInfo(modelName, 99, 0, "Verifying..."))

                if (!verifyChecksum(outputFile, checksum)) {
                    outputFile.delete()
                    return@withContext Result.failure(
                        workDataOf("error" to "Checksum verification failed")
                    )
                }
                Timber.i("Checksum verified for $modelId")
            }

            // Trigger model discovery to refresh available models
            try {
                val modelDiscovery = ModelDiscovery(applicationContext)
                modelDiscovery.discoverInstalledModels()
                Timber.i("Model discovery refreshed after download")
            } catch (e: Exception) {
                Timber.w(e, "Failed to refresh model discovery")
            }

            Timber.i("Download completed: $modelId (${outputFile.absolutePath})")
            return@withContext Result.success(
                workDataOf("model_path" to outputFile.absolutePath)
            )

        } catch (e: Exception) {
            Timber.e(e, "Download failed for $modelId")
            return@withContext Result.retry() // Retry with exponential backoff
        }
    }

    /**
     * Download file with resume support using HTTP Range requests
     */
    private suspend fun downloadWithResume(
        url: String,
        outputFile: File,
        expectedSize: Long,
        resume: Boolean,
        onProgress: (progress: Int, bytesDownloaded: Long, totalBytes: Long, speedBytesPerSec: Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            // Create parent directory if needed
            outputFile.parentFile?.mkdirs()

            // Check existing file for resume
            val existingBytes = if (resume && outputFile.exists()) {
                outputFile.length()
            } else {
                0L
            }

            Timber.d("Download: $url -> ${outputFile.name} (resume from $existingBytes bytes)")

            // Open connection
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "AVA-AI/1.0")

            // Add Range header for resume
            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
            }

            connection.connect()

            // Check response code
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != HttpURLConnection.HTTP_PARTIAL) {
                Timber.e("HTTP error: $responseCode for $url")
                return@withContext false
            }

            // Get total file size
            val contentLength = connection.contentLength.toLong()
            val totalBytes = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                // Parse Content-Range header
                val contentRange = connection.getHeaderField("Content-Range")
                contentRange?.substringAfterLast('/')?.toLongOrNull() ?: (contentLength + existingBytes)
            } else {
                contentLength
            }

            Timber.i("Downloading: ${outputFile.name} (${formatBytes(totalBytes)})")

            // Validate expected size
            if (expectedSize > 0 && totalBytes != expectedSize) {
                Timber.w("Size mismatch - Expected: $expectedSize, Got: $totalBytes")
            }

            // Download with progress tracking
            val input = connection.inputStream
            val output = FileOutputStream(outputFile, existingBytes > 0) // Append mode if resuming

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesDownloaded = existingBytes
            var lastProgressTime = System.currentTimeMillis()
            var lastBytesDownloaded = bytesDownloaded
            val startTime = System.currentTimeMillis()

            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break

                output.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead

                // Update progress periodically
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastProgressTime >= PROGRESS_UPDATE_INTERVAL_MS) {
                    val progress = ((bytesDownloaded.toFloat() / totalBytes) * 100).toInt()
                    val elapsed = currentTime - lastProgressTime
                    val deltaBytes = bytesDownloaded - lastBytesDownloaded
                    val speed = if (elapsed > 0) (deltaBytes * 1000) / elapsed else 0L

                    onProgress(progress, bytesDownloaded, totalBytes, speed)

                    lastProgressTime = currentTime
                    lastBytesDownloaded = bytesDownloaded
                }
            }

            output.flush()
            output.close()
            input.close()

            val downloadDuration = System.currentTimeMillis() - startTime
            Timber.i("Download complete: ${outputFile.name} (${formatBytes(bytesDownloaded)} in ${downloadDuration}ms)")

            true

        } catch (e: Exception) {
            Timber.e(e, "Download error: ${outputFile.name}")
            false
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Verify file checksum
     */
    private suspend fun verifyChecksum(file: File, expectedChecksum: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(BUFFER_SIZE)

            file.inputStream().use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            val actualChecksum = digest.digest().joinToString("") { "%02x".format(it) }
            val match = actualChecksum.equals(expectedChecksum, ignoreCase = true)

            if (!match) {
                Timber.e("Checksum mismatch - Expected: $expectedChecksum, Got: $actualChecksum")
            }

            match
        } catch (e: Exception) {
            Timber.e(e, "Checksum verification failed")
            false
        }
    }

    /**
     * Create foreground info for notification
     */
    private fun createForegroundInfo(
        modelName: String,
        progress: Int,
        speedBytesPerSec: Long = 0,
        statusText: String? = null
    ): ForegroundInfo {
        // Create notification channel if needed
        createNotificationChannel()

        val title = "Downloading $modelName"
        val text = statusText ?: if (speedBytesPerSec > 0) {
            "$progress% (${formatBytes(speedBytesPerSec)}/s)"
        } else {
            "$progress%"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "LLM Model Downloads",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Downloads of large language models"
            }
            notificationManager.createNotificationChannel(channel)
        }
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
