// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/embeddings/AndroidModelDownloadManager.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.embeddings

import android.content.Context
import com.augmentalis.ava.core.common.AVAException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Android implementation of ModelDownloadManager
 *
 * Downloads models to app's files directory for on-demand loading.
 * Uses standard HttpURLConnection for maximum compatibility.
 *
 * ## AVA File Format Convention
 * Uses .AON extension (AVA ONNX Naming) for all downloaded models.
 * AON files are identical to ONNX files in format - same binary content,
 * different extension for AVA branding and proprietary model identification.
 *
 * Platform-aware:
 * - Google Play: Future support for Play Feature Delivery
 * - AOSP/Custom: Downloads from custom server or HuggingFace
 */
class AndroidModelDownloadManager(
    private val context: Context,
    private val customServerUrl: String? = null
) : ModelDownloadManager {

    private val modelsDir: File by lazy {
        // Use external app-specific directory for user-placed models
        // Location: /sdcard/Android/data/{package}/files/models/
        File(context.getExternalFilesDir(null), "models").apply {
            if (!exists()) {
                mkdirs()
                // Create .nomedia file to hide folder from media scanners
                File(this, ".nomedia").createNewFile()
            }
        }
    }

    private val downloadProgressMap = mutableMapOf<String, MutableStateFlow<DownloadProgress>>()

    override suspend fun isModelAvailable(modelId: String): Boolean {
        return getModelFile(modelId).exists()
    }

    override suspend fun getModelPath(modelId: String): String? {
        val file = getModelFile(modelId)
        return if (file.exists()) file.absolutePath else null
    }

    override suspend fun downloadModel(
        modelId: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val modelInfo = AvailableModels.getById(modelId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Unknown model: $modelId")
                )

            val progressFlow = getProgressFlow(modelId)
            progressFlow.value = DownloadProgress.Starting

            val outputFile = getModelFile(modelId)

            // Create temporary file for download
            val tempFile = File(outputFile.parent, "${outputFile.name}.tmp")

            try {
                // Get platform-appropriate download URL
                val downloadUrl = PlatformDetector.getDownloadUrl(
                    context,
                    modelId,
                    customServerUrl
                ) ?: modelInfo.downloadUrl // Fallback to default

                // Download file
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw AVAException.NetworkException("HTTP error: ${connection.responseCode}")
                }

                val totalBytes = connection.contentLength.toLong()
                var downloadedBytes = 0L

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val progress = downloadedBytes.toFloat() / totalBytes
                            progressFlow.value = DownloadProgress.Downloading(
                                bytesDownloaded = downloadedBytes,
                                totalBytes = totalBytes,
                                progress = progress
                            )
                            onProgress?.invoke(progress)
                        }
                    }
                }

                // Verify file size
                if (tempFile.length() != totalBytes) {
                    throw AVAException.NetworkException(
                        "Download incomplete: expected $totalBytes bytes, got ${tempFile.length()}"
                    )
                }

                // Verify checksum if available
                if (modelInfo.sha256 != null) {
                    progressFlow.value = DownloadProgress.Verifying(0.5f)
                    val isValid = verifyChecksum(tempFile, modelInfo.sha256)
                    if (!isValid) {
                        throw AVAException.SecurityException("Checksum verification failed")
                    }
                }

                // Move temp file to final location
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                tempFile.renameTo(outputFile)

                progressFlow.value = DownloadProgress.Completed(outputFile.absolutePath)

                Result.success(outputFile.absolutePath)
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        } catch (e: Exception) {
            val progressFlow = getProgressFlow(modelId)
            progressFlow.value = DownloadProgress.Failed(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    override fun observeDownloadProgress(modelId: String): Flow<DownloadProgress> {
        return getProgressFlow(modelId).asStateFlow()
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = getModelFile(modelId)
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModelInfo(modelId: String): ModelInfo? {
        val baseInfo = AvailableModels.getById(modelId) ?: return null
        val localPath = getModelPath(modelId)

        return baseInfo.copy(
            isDownloaded = localPath != null,
            localPath = localPath
        )
    }

    override suspend fun listAvailableModels(): List<ModelInfo> {
        return AvailableModels.getAll().map { modelInfo ->
            val localPath = getModelPath(modelInfo.id)
            modelInfo.copy(
                isDownloaded = localPath != null,
                localPath = localPath
            )
        }
    }

    override suspend fun listDownloadedModels(): List<ModelInfo> {
        return listAvailableModels().filter { it.isDownloaded }
    }

    override suspend fun verifyModel(modelId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val modelInfo = AvailableModels.getById(modelId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Unknown model: $modelId")
                )

            val file = getModelFile(modelId)
            if (!file.exists()) {
                return@withContext Result.success(false)
            }

            // Verify file size
            if (file.length() != modelInfo.sizeBytes) {
                return@withContext Result.success(false)
            }

            // Verify checksum if available
            if (modelInfo.sha256 != null) {
                val isValid = verifyChecksum(file, modelInfo.sha256)
                return@withContext Result.success(isValid)
            }

            // No checksum available, assume valid if size matches
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get file path for a model
     * Uses .AON extension (AVA ONNX Naming Convention v2)
     */
    private fun getModelFile(modelId: String): File {
        return File(modelsDir, "$modelId.AON")
    }

    /**
     * Get or create progress flow for a model
     */
    private fun getProgressFlow(modelId: String): MutableStateFlow<DownloadProgress> {
        return downloadProgressMap.getOrPut(modelId) {
            MutableStateFlow(DownloadProgress.Idle)
        }
    }

    /**
     * Verify file checksum
     */
    private fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        return actualHash.equals(expectedSha256, ignoreCase = true)
    }

    /**
     * Get total size of downloaded models
     */
    suspend fun getTotalStorageUsed(): Long = withContext(Dispatchers.IO) {
        modelsDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Delete all downloaded models
     */
    suspend fun clearAllModels(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            modelsDir.listFiles()?.forEach { it.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
