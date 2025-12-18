/**
 * HuggingFace Model Downloader for MLC-LLM
 *
 * Downloads model weights and configuration from HuggingFace repositories.
 * Supports resume, progress tracking, and checksum verification.
 *
 * Created: 2025-11-07
 * Author: AVA AI Team
 */

package com.augmentalis.llm.alc.loader

import android.content.Context
import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Download progress information
 */
data class DownloadProgress(
    val fileName: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Float
) {
    val isComplete: Boolean get() = bytesDownloaded >= totalBytes
}

/**
 * Model download configuration
 */
data class ModelDownloadConfig(
    /** AVA model ID (saved name on device, e.g., "AVA-GEM-2B-Q4") */
    val modelId: String,
    /** HuggingFace repo URL (download source) */
    val modelUrl: String,
    /** Local source path for testing (copy instead of download) */
    val localSourcePath: String? = null,
    val expectedFiles: List<String> = listOf(
        "params_shard_0.bin",
        "params_shard_1.bin",
        "params_shard_2.bin",
        "params_shard_3.bin",
        "tokenizer.json",
        "tokenizer.model",
        "config.json",
        "mlc-chat-config.json"
    )
)

/**
 * Downloads MLC-LLM models from HuggingFace
 */
class HuggingFaceModelDownloader(
    private val context: Context
) {
    // Use external storage for easier model deployment via adb
    private val modelsDir: File by lazy {
        val externalDir = context.getExternalFilesDir(null)
        val baseDir = externalDir ?: context.filesDir
        File(baseDir, "models").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Check if model is already downloaded
     */
    fun isModelDownloaded(modelId: String): Boolean {
        val modelDir = File(modelsDir, modelId)
        if (!modelDir.exists()) return false

        // Check if essential files exist
        val essentialFiles = listOf(
            "params_shard_0.bin",
            "tokenizer.json",
            "config.json"
        )

        return essentialFiles.all { fileName ->
            File(modelDir, fileName).exists()
        }
    }

    /**
     * Get model directory path
     */
    fun getModelPath(modelId: String): String {
        return File(modelsDir, modelId).absolutePath
    }

    /**
     * Copy model from local source (for testing)
     *
     * Copies .tar archive from local downloads instead of downloading from HuggingFace.
     * Saves with AVA naming convention (e.g., AVA-GEM-2B-Q4.tar).
     *
     * @param config Model configuration with localSourcePath set
     * @return Result indicating success or failure
     */
    suspend fun copyFromLocalSource(config: ModelDownloadConfig): Result<Unit> = withContext(Dispatchers.IO) {
        val localPath = config.localSourcePath
            ?: return@withContext Result.Error(
                exception = IllegalArgumentException("localSourcePath not set"),
                message = "No local source path provided"
            )

        try {
            val sourceFile = File(localPath)
            if (!sourceFile.exists()) {
                return@withContext Result.Error(
                    exception = java.io.FileNotFoundException("Source not found: $localPath"),
                    message = "Local source file not found"
                )
            }

            val modelDir = File(modelsDir, config.modelId)
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }

            // Copy the .tar file with AVA naming
            val destFile = File(modelDir, "${config.modelId}.tar")

            Timber.i("Copying from local source: ${sourceFile.name} → ${destFile.name}")
            sourceFile.copyTo(destFile, overwrite = true)

            Timber.i("Local copy complete: ${config.modelId} (${formatBytes(destFile.length())})")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy from local source")
            Result.Error(
                exception = e,
                message = "Copy failed: ${e.message}"
            )
        }
    }

    /**
     * Download model from HuggingFace or copy from local source
     *
     * Downloads from HuggingFace repo using original model name,
     * but saves to device using AVA naming convention.
     *
     * Example:
     * - Download from: mlc-ai/gemma-2b-it-q4f16_1-MLC
     * - Save as: AVA-GEM-2B-Q4/
     *
     * If localSourcePath is provided, copies from there instead (for testing).
     *
     * @param config Model download configuration
     * @return Flow of download progress
     */
    fun downloadModel(config: ModelDownloadConfig): Flow<DownloadProgress> = flow {
        // If local source available, copy instead of download
        if (config.localSourcePath != null) {
            Timber.i("Local source available, copying instead of downloading: ${config.modelId}")
            val result = copyFromLocalSource(config)
            when (result) {
                is Result.Success -> {
                    emit(DownloadProgress(
                        fileName = "${config.modelId}.tar",
                        bytesDownloaded = File(modelsDir, "${config.modelId}/${config.modelId}.tar").length(),
                        totalBytes = File(modelsDir, "${config.modelId}/${config.modelId}.tar").length(),
                        percentage = 100f
                    ))
                }
                is Result.Error -> {
                    Timber.w("Local copy failed, falling back to download: ${result.message}")
                    // Fall through to download
                }
            }
            if (result is Result.Success) return@flow
        }

        val modelDir = File(modelsDir, config.modelId)
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }

        Timber.i("Starting download: ${config.modelId} from ${config.modelUrl}")
        Timber.d("Saving as: ${config.modelId} (AVA naming convention)")

        // Download each file
        for (fileName in config.expectedFiles) {
            val fileUrl = "${config.modelUrl}/resolve/main/$fileName"
            val outputFile = File(modelDir, fileName)

            // Skip if already exists and has valid size
            if (outputFile.exists() && outputFile.length() > 0) {
                Timber.d("File already exists: $fileName")
                emit(DownloadProgress(
                    fileName = fileName,
                    bytesDownloaded = outputFile.length(),
                    totalBytes = outputFile.length(),
                    percentage = 100f
                ))
                continue
            }

            try {
                downloadFile(fileUrl, outputFile) { progress ->
                    emit(progress.copy(fileName = fileName))
                }
            } catch (e: Exception) {
                // Some files are optional (not all shards exist for every model)
                if (fileName.contains("shard") && fileName != "params_shard_0.bin") {
                    Timber.d("Optional file not found: $fileName")
                } else {
                    Timber.e(e, "Failed to download required file: $fileName")
                    throw e
                }
            }
        }

        Timber.i("Model download complete: ${config.modelId}")
    }

    /**
     * Download single file with progress tracking
     */
    private suspend fun downloadFile(
        urlString: String,
        destination: File,
        onProgress: suspend (DownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "AVA-AI/1.0")

            // Support resume
            val existingBytes = if (destination.exists()) destination.length() else 0
            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw IllegalStateException("HTTP error: $responseCode for $urlString")
            }

            val fileSize = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                // Parse Content-Range header for total size
                connection.getHeaderField("Content-Range")?.let { range ->
                    range.substringAfter('/').toLongOrNull()
                } ?: (connection.contentLength.toLong() + existingBytes)
            } else {
                connection.contentLength.toLong()
            }

            val input = connection.inputStream
            val output = FileOutputStream(destination, existingBytes > 0)

            val buffer = ByteArray(8192)
            var totalBytesRead = existingBytes
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                val percentage = if (fileSize > 0) {
                    (totalBytesRead.toFloat() / fileSize * 100f)
                } else {
                    0f
                }

                onProgress(DownloadProgress(
                    fileName = destination.name,
                    bytesDownloaded = totalBytesRead,
                    totalBytes = fileSize,
                    percentage = percentage
                ))
            }

            output.close()
            input.close()

            Timber.d("Downloaded: ${destination.name} (${formatBytes(totalBytesRead)})")

        } finally {
            connection.disconnect()
        }
    }

    /**
     * Verify file checksum (SHA-256)
     */
    suspend fun verifyChecksum(file: File, expectedChecksum: String): Boolean = withContext(Dispatchers.IO) {
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
                Timber.w("Checksum mismatch for ${file.name}: expected=$expectedChecksum, actual=$actualChecksum")
            }

            match
        } catch (e: Exception) {
            Timber.e(e, "Checksum verification failed for ${file.name}")
            false
        }
    }

    /**
     * Delete downloaded model
     */
    suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelDir = File(modelsDir, modelId)
            if (modelDir.exists()) {
                modelDir.deleteRecursively()
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
     * Get total size of downloaded models
     */
    fun getTotalModelsSize(): Long {
        return modelsDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    /**
     * Get size of specific model
     */
    fun getModelSize(modelId: String): Long {
        val modelDir = File(modelsDir, modelId)
        return modelDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
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
    }
}
