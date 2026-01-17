/**
 * Model Storage Manager for AVA LLM Models
 *
 * Manages storage and file operations for downloaded LLM models.
 * Provides storage validation, cleanup, and directory management.
 *
 * Features:
 * - Storage space checking
 * - Model file management
 * - Storage usage tracking
 * - Automatic cleanup of old models
 *
 * Storage location: /sdcard/ava-ai-models/llm-gguf/
 *
 * Created: 2025-12-06
 * Author: AVA AI Team
 */

package com.augmentalis.llm.download

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Storage manager for LLM models
 */
class ModelStorageManager(
    private val context: Context
) {

    companion object {
        // Standard storage location for GGUF models
        private const val MODELS_DIR = "ava-ai-models/llm-gguf"

        // Minimum free space buffer (500MB)
        private const val MIN_FREE_SPACE_BYTES = 500L * 1024 * 1024

        // File extension for GGUF models
        private const val GGUF_EXTENSION = ".gguf"
    }

    private val modelsDirectory: File by lazy {
        // Use external storage for easy adb push and compatibility with ModelDiscovery
        val externalDir = Environment.getExternalStorageDirectory()
        File(externalDir, MODELS_DIR).apply {
            if (!exists()) {
                mkdirs()
                Timber.i("Created models directory: $absolutePath")
            }
        }
    }

    /**
     * Storage information
     */
    data class StorageInfo(
        val totalBytes: Long,
        val usedBytes: Long,
        val availableBytes: Long,
        val modelCount: Int
    ) {
        val usedGB: Float get() = usedBytes / (1024f * 1024f * 1024f)
        val availableGB: Float get() = availableBytes / (1024f * 1024f * 1024f)
        val totalGB: Float get() = totalBytes / (1024f * 1024f * 1024f)
    }

    /**
     * Get available storage space
     *
     * @return Available bytes in storage location
     */
    fun getAvailableSpace(): Long {
        return try {
            val stat = StatFs(modelsDirectory.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            Timber.e(e, "Failed to get available space")
            0L
        }
    }

    /**
     * Check if there's enough space for a download
     *
     * Includes safety margin to prevent filling up storage.
     *
     * @param requiredBytes Size of model to download
     * @return True if enough space available
     */
    fun hasEnoughSpace(requiredBytes: Long): Boolean {
        val availableSpace = getAvailableSpace()
        val requiredWithBuffer = requiredBytes + MIN_FREE_SPACE_BYTES

        Timber.d("Storage check - Available: ${formatBytes(availableSpace)}, Required: ${formatBytes(requiredWithBuffer)}")

        return availableSpace >= requiredWithBuffer
    }

    /**
     * Get model storage usage information
     *
     * @return Storage information including used/available space
     */
    suspend fun getModelStorageUsage(): StorageInfo = withContext(Dispatchers.IO) {
        val stat = StatFs(modelsDirectory.absolutePath)
        val totalBytes = stat.totalBytes
        val availableBytes = stat.availableBytes

        val modelFiles = getInstalledModels()
        val usedBytes = modelFiles.sumOf { it.second }

        StorageInfo(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            availableBytes = availableBytes,
            modelCount = modelFiles.size
        )
    }

    /**
     * Get file for a model
     *
     * @param modelId Model identifier
     * @return File object for model (may not exist yet)
     */
    fun getModelFile(modelId: String): File {
        // Store as .gguf file directly in models directory
        return File(modelsDirectory, "$modelId$GGUF_EXTENSION")
    }

    /**
     * Check if model exists
     *
     * @param modelId Model identifier
     * @return True if model file exists
     */
    fun modelExists(modelId: String): Boolean {
        return getModelFile(modelId).exists()
    }

    /**
     * Get model file size
     *
     * @param modelId Model identifier
     * @return Size in bytes, or 0 if not found
     */
    fun getModelSize(modelId: String): Long {
        val file = getModelFile(modelId)
        return if (file.exists()) file.length() else 0L
    }

    /**
     * Delete a model file
     *
     * @param modelId Model identifier
     * @return True if deleted successfully
     */
    fun deleteModel(modelId: String): Boolean {
        return try {
            val file = getModelFile(modelId)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Timber.i("Deleted model: $modelId (${formatBytes(file.length())})")
                }
                deleted
            } else {
                true // Already deleted
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete model: $modelId")
            false
        }
    }

    /**
     * Get list of installed models
     *
     * @return List of (modelId, sizeBytes) pairs
     */
    suspend fun getInstalledModels(): List<Pair<String, Long>> = withContext(Dispatchers.IO) {
        try {
            if (!modelsDirectory.exists()) {
                return@withContext emptyList()
            }

            modelsDirectory.listFiles()
                ?.filter { it.isFile && it.name.endsWith(GGUF_EXTENSION) }
                ?.map { file ->
                    val modelId = file.nameWithoutExtension
                    modelId to file.length()
                }
                ?.sortedBy { it.first }
                ?: emptyList()

        } catch (e: Exception) {
            Timber.e(e, "Failed to list installed models")
            emptyList()
        }
    }

    /**
     * Cleanup old models to free space
     *
     * Deletes oldest models (by last modified time) until target count is reached.
     *
     * @param keepCount Number of models to keep
     * @return List of deleted model IDs
     */
    suspend fun cleanupOldModels(keepCount: Int = 3): List<String> = withContext(Dispatchers.IO) {
        try {
            val models = modelsDirectory.listFiles()
                ?.filter { it.isFile && it.name.endsWith(GGUF_EXTENSION) }
                ?.sortedByDescending { it.lastModified() } // Newest first
                ?: return@withContext emptyList()

            if (models.size <= keepCount) {
                return@withContext emptyList()
            }

            val toDelete = models.drop(keepCount) // Skip the newest keepCount models
            val deleted = mutableListOf<String>()

            for (file in toDelete) {
                if (file.delete()) {
                    deleted.add(file.nameWithoutExtension)
                    Timber.i("Cleaned up old model: ${file.name}")
                }
            }

            deleted

        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup old models")
            emptyList()
        }
    }

    /**
     * Get models directory path
     *
     * @return Absolute path to models directory
     */
    fun getModelsDirectory(): String {
        return modelsDirectory.absolutePath
    }

    /**
     * Verify directory is writable
     *
     * @return True if directory exists and is writable
     */
    fun isDirectoryWritable(): Boolean {
        return try {
            modelsDirectory.exists() && modelsDirectory.canWrite()
        } catch (e: Exception) {
            Timber.e(e, "Failed to check directory permissions")
            false
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
