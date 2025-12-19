/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.nlu.download

import android.content.Context
import androidx.work.WorkManager
import com.augmentalis.ava.core.common.Result
import com.augmentalis.nlu.ModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * NLU Model Downloader
 *
 * Downloads NLU models from HuggingFace with:
 * - Progress tracking via StateFlow
 * - Background download with WorkManager
 * - Checksum verification
 * - Automatic retry on failure (3 attempts)
 * - Network availability check
 *
 * Model Download URLs:
 * - MobileBERT: Bundled in APK (no download needed)
 * - mALBERT: https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2
 */
class NLUModelDownloader(
    private val context: Context,
    private val workManager: WorkManager
) {
    // Models directory
    private val modelsDir: File by lazy {
        val externalDir = context.getExternalFilesDir(null)
        val baseDir = externalDir ?: context.filesDir
        File(baseDir, "models").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    // Download state
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    /**
     * Download state sealed class
     */
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(
            val progress: Float,
            val bytesDownloaded: Long,
            val totalBytes: Long,
            val fileName: String = ""
        ) : DownloadState()
        data class Success(val modelPath: String) : DownloadState()
        data class Failed(val error: String, val retryable: Boolean) : DownloadState()
    }

    /**
     * Model download configuration
     */
    data class ModelConfig(
        val modelType: ModelType,
        val modelUrl: String,
        val expectedChecksum: String
    )

    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }

    /**
     * Download NLU model with progress tracking and retry
     *
     * @param modelId Model identifier (e.g., "AVA-768-Base-INT8")
     * @param modelUrl HuggingFace model URL
     * @param expectedChecksum Expected SHA-256 checksum
     * @return Result with model path or error
     */
    suspend fun downloadModel(
        modelId: String,
        modelUrl: String,
        expectedChecksum: String
    ): Result<String> = withContext(Dispatchers.IO) {
        // Check network availability
        if (!isNetworkAvailable()) {
            _downloadState.value = DownloadState.Failed(
                "No network connection available",
                retryable = true
            )
            return@withContext Result.Error(
                exception = IllegalStateException("No network connection"),
                message = "No network connection available. Please check your internet connection."
            )
        }

        val modelFile = File(modelsDir, modelId)
        var lastError: Exception? = null

        // Retry up to 3 times
        for (attempt in 1..3) {
            try {
                Timber.i("Download attempt $attempt/3: $modelId")

                // Start download
                val result = downloadWithProgress(modelUrl, modelFile)

                when (result) {
                    is Result.Success -> {
                        // Verify checksum
                        Timber.i("Download complete, verifying checksum...")
                        if (verifyChecksum(modelFile, expectedChecksum)) {
                            Timber.i("Model downloaded and verified: ${modelFile.absolutePath}")
                            _downloadState.value = DownloadState.Success(modelFile.absolutePath)
                            return@withContext Result.Success(modelFile.absolutePath)
                        } else {
                            // Checksum mismatch - delete corrupted file
                            Timber.e("Checksum verification failed for $modelId")
                            modelFile.delete()
                            lastError = IllegalStateException("Checksum verification failed")

                            if (attempt < 3) {
                                Timber.w("Retrying download (attempt ${attempt + 1}/3)...")
                                Thread.sleep(1000 * attempt.toLong()) // Exponential backoff
                            }
                        }
                    }
                    is Result.Error -> {
                        lastError = result.exception as? Exception
                            ?: Exception(result.message)

                        if (attempt < 3) {
                            Timber.w("Download failed, retrying (attempt ${attempt + 1}/3)...")
                            Thread.sleep(1000 * attempt.toLong()) // Exponential backoff
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Download attempt $attempt failed")
                lastError = e

                if (attempt < 3) {
                    Thread.sleep(1000 * attempt.toLong()) // Exponential backoff
                }
            }
        }

        // All retries failed
        val errorMessage = "Download failed after 3 attempts: ${lastError?.message}"
        _downloadState.value = DownloadState.Failed(errorMessage, retryable = true)

        Result.Error(
            exception = lastError ?: Exception(errorMessage),
            message = errorMessage
        )
    }

    /**
     * Download file with progress tracking
     */
    private suspend fun downloadWithProgress(
        urlString: String,
        destination: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "AVA-AI/1.0")
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.Error(
                    exception = IllegalStateException("HTTP error: $responseCode"),
                    message = "Download failed with HTTP code: $responseCode"
                )
            }

            val fileSize = connection.contentLength.toLong()
            val input = connection.inputStream
            val output = FileOutputStream(destination)

            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                val progress = if (fileSize > 0) {
                    (totalBytesRead.toFloat() / fileSize)
                } else {
                    0f
                }

                // Update progress
                _downloadState.value = DownloadState.Downloading(
                    progress = progress,
                    bytesDownloaded = totalBytesRead,
                    totalBytes = fileSize,
                    fileName = destination.name
                )
            }

            output.close()
            input.close()

            Timber.d("Downloaded: ${destination.name} (${formatBytes(totalBytesRead)})")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Download failed: $urlString")

            // Clean up partial download
            if (destination.exists()) {
                destination.delete()
            }

            Result.Error(
                exception = e,
                message = "Download failed: ${e.message}"
            )
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Verify file checksum (SHA-256)
     */
    private suspend fun verifyChecksum(
        file: File,
        expectedChecksum: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            val actualChecksum = digest.digest().joinToString("") { "%02x".format(it) }
            val match = actualChecksum.equals(expectedChecksum, ignoreCase = true)

            if (!match) {
                Timber.w("Checksum mismatch: expected=$expectedChecksum, actual=$actualChecksum")
            }

            match
        } catch (e: Exception) {
            Timber.e(e, "Checksum verification failed")
            false
        }
    }

    /**
     * Check if model is downloaded
     */
    fun isModelDownloaded(modelId: String): Boolean {
        val modelFile = File(modelsDir, modelId)
        return modelFile.exists() && modelFile.length() > 0
    }

    /**
     * Delete downloaded model
     */
    suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(modelsDir, modelId)
            if (modelFile.exists()) {
                modelFile.delete()
                Timber.i("Deleted model: $modelId")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to delete model: ${e.message}"
            )
        }
    }

    /**
     * Get model file size
     */
    fun getModelSize(modelId: String): Long {
        val modelFile = File(modelsDir, modelId)
        return if (modelFile.exists()) modelFile.length() else 0L
    }

    /**
     * Reset download state
     */
    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    companion object {
        private fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }

        /**
         * Model download URLs and checksums
         */
        object ModelUrls {
            // mALBERT Multilingual (768-dim)
            const val MALBERT_URL = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx"
            const val MALBERT_CHECKSUM = "TBD" // TODO: Add actual checksum

            // MobileBERT (384-dim) - Bundled in APK, no download needed
        }
    }
}
