/**
 * Model Cache Manager for AVA
 *
 * Manages cached ML models in app private storage.
 *
 * Features:
 * - Check if models exist locally
 * - Get model file paths
 * - Delete old or unused models
 * - Calculate cache size
 * - Clear cache to free space
 * - Validate model integrity
 *
 * Storage structure:
 * - <app_files_dir>/models/<model_id>/
 *   - model.bin (or appropriate extension)
 *   - metadata.json
 *   - checksum.sha256
 *
 * Created: 2025-11-03
 * Author: AVA AI Team
 */

package com.augmentalis.llm.download

import android.content.Context
import com.augmentalis.llm.LLMResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

/**
 * Model cache manager
 *
 * @property context Android context for accessing app storage
 */
class ModelCacheManager(
    private val context: Context
) {

    private val modelsDir: File by lazy {
        // Use external storage for easier model deployment via adb
        // Falls back to internal storage if external not available
        val externalDir = context.getExternalFilesDir(null)
        val baseDir = externalDir ?: context.filesDir
        File(baseDir, "models").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Check if a model exists in cache
     *
     * @param modelId Unique model identifier
     * @return True if model exists and is valid
     */
    suspend fun isModelCached(modelId: String): Boolean = withContext(Dispatchers.IO) {
        val modelDir = getModelDirectory(modelId)
        if (!modelDir.exists()) {
            return@withContext false
        }

        val modelFiles = modelDir.listFiles()?.filter { it.isFile } ?: emptyList()
        modelFiles.isNotEmpty()
    }

    /**
     * Get model file path
     *
     * @param modelId Unique model identifier
     * @param filename Optional filename (searches for first file if null)
     * @return File path or null if not found
     */
    suspend fun getModelPath(modelId: String, filename: String? = null): String? =
        withContext(Dispatchers.IO) {
            val modelDir = getModelDirectory(modelId)
            if (!modelDir.exists()) {
                return@withContext null
            }

            val file = if (filename != null) {
                File(modelDir, filename)
            } else {
                // Return first non-metadata file
                modelDir.listFiles()?.firstOrNull { file ->
                    file.isFile && !file.name.endsWith(".json") && !file.name.endsWith(".sha256")
                }
            }

            file?.takeIf { it.exists() }?.absolutePath
        }

    /**
     * Get all files for a model
     *
     * @param modelId Unique model identifier
     * @return List of file paths
     */
    suspend fun getModelFiles(modelId: String): List<String> = withContext(Dispatchers.IO) {
        val modelDir = getModelDirectory(modelId)
        if (!modelDir.exists()) {
            return@withContext emptyList()
        }

        modelDir.listFiles()
            ?.filter { it.isFile }
            ?.map { it.absolutePath }
            ?: emptyList()
    }

    /**
     * Get model directory
     *
     * @param modelId Unique model identifier
     * @return Directory for model files
     */
    fun getModelDirectory(modelId: String): File {
        return File(modelsDir, modelId)
    }

    /**
     * Delete a model from cache
     *
     * @param modelId Unique model identifier
     * @return LLMResult indicating success or failure
     */
    suspend fun deleteModel(modelId: String): LLMResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelDir = getModelDirectory(modelId)
            if (modelDir.exists()) {
                val deleted = modelDir.deleteRecursively()
                if (deleted) {
                    Timber.i("Deleted model: $modelId")
                    LLMResult.Success(Unit)
                } else {
                    LLMResult.Error(
                        message = "Could not delete model: $modelId",
                        cause = IllegalStateException("Failed to delete model directory")
                    )
                }
            } else {
                LLMResult.Success(Unit) // Already deleted
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
     * Clear all cached models
     *
     * @return LLMResult with number of models deleted
     */
    suspend fun clearCache(): LLMResult<Int> = withContext(Dispatchers.IO) {
        try {
            val models = modelsDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
            var deletedCount = 0

            for (modelDir in models) {
                if (modelDir.deleteRecursively()) {
                    deletedCount++
                }
            }

            Timber.i("Cleared cache: $deletedCount models deleted")
            LLMResult.Success(deletedCount)
        } catch (e: Exception) {
            Timber.e(e, "Error clearing cache")
            LLMResult.Error(
                message = "Failed to clear cache: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Get size of a model in cache
     *
     * @param modelId Unique model identifier
     * @return Size in bytes
     */
    suspend fun getModelSize(modelId: String): Long = withContext(Dispatchers.IO) {
        val modelDir = getModelDirectory(modelId)
        if (!modelDir.exists()) {
            return@withContext 0L
        }

        calculateDirectorySize(modelDir)
    }

    /**
     * Get total cache size
     *
     * @return Total size in bytes
     */
    suspend fun getTotalCacheSize(): Long = withContext(Dispatchers.IO) {
        calculateDirectorySize(modelsDir)
    }

    /**
     * Get list of cached model IDs
     *
     * @return List of model IDs
     */
    suspend fun getCachedModelIds(): List<String> = withContext(Dispatchers.IO) {
        modelsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }

    /**
     * Get cache statistics
     *
     * @return Cache statistics
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        val modelIds = getCachedModelIds()
        val modelSizes = modelIds.associateWith { getModelSize(it) }
        val totalSize = getTotalCacheSize()

        CacheStats(
            modelCount = modelIds.size,
            totalSizeBytes = totalSize,
            modelSizes = modelSizes
        )
    }

    /**
     * Verify model checksum
     *
     * @param modelId Unique model identifier
     * @param expectedChecksum Expected SHA-256 checksum
     * @return LLMResult indicating if checksum matches
     */
    suspend fun verifyChecksum(
        modelId: String,
        expectedChecksum: String
    ): LLMResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val modelPath = getModelPath(modelId)
                ?: return@withContext LLMResult.Error(
                    message = "Model not found in cache: $modelId",
                    cause = IllegalStateException("Model not found")
                )

            val file = File(modelPath)
            val actualChecksum = calculateChecksum(file)

            val matches = actualChecksum.equals(expectedChecksum, ignoreCase = true)
            LLMResult.Success(matches)
        } catch (e: Exception) {
            Timber.e(e, "Error verifying checksum for: $modelId")
            LLMResult.Error(
                message = "Checksum verification failed: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Calculate SHA-256 checksum for a file
     *
     * @param file File to calculate checksum for
     * @return Hex string checksum
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

    /**
     * Calculate directory size recursively
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L

        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }

        return size
    }

    /**
     * Get available storage space
     *
     * @return Available bytes
     */
    fun getAvailableSpace(): Long {
        return modelsDir.usableSpace
    }

    /**
     * Check if there's enough space for download
     *
     * @param requiredBytes Required space in bytes
     * @param safetyMarginBytes Additional margin (default 100MB)
     * @return True if enough space available
     */
    fun hasEnoughSpace(
        requiredBytes: Long,
        safetyMarginBytes: Long = 100 * 1024 * 1024 // 100MB
    ): Boolean {
        return getAvailableSpace() >= (requiredBytes + safetyMarginBytes)
    }

    /**
     * Cleanup old models based on last access time
     *
     * @param maxAgeMs Maximum age in milliseconds
     * @return LLMResult with number of models cleaned up
     */
    suspend fun cleanupOldModels(maxAgeMs: Long): LLMResult<Int> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val models = modelsDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
            var cleanedCount = 0

            for (modelDir in models) {
                val age = currentTime - modelDir.lastModified()
                if (age > maxAgeMs) {
                    if (modelDir.deleteRecursively()) {
                        cleanedCount++
                        Timber.i("Cleaned up old model: ${modelDir.name}")
                    }
                }
            }

            LLMResult.Success(cleanedCount)
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up old models")
            LLMResult.Error(
                message = "Failed to cleanup old models: ${e.message}",
                cause = e
            )
        }
    }
}

/**
 * Cache statistics data class
 */
data class CacheStats(
    val modelCount: Int,
    val totalSizeBytes: Long,
    val modelSizes: Map<String, Long>
) {

    /**
     * Get human-readable total size
     */
    fun getFormattedTotalSize(): String = formatBytes(totalSizeBytes)

    /**
     * Get human-readable model sizes
     */
    fun getFormattedModelSizes(): Map<String, String> =
        modelSizes.mapValues { formatBytes(it.value) }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
