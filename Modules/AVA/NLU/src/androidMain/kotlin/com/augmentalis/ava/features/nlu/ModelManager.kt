/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.features.nlu

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.common.AVAException
import com.augmentalis.ava.core.data.db.EmbeddingMetadataQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Android implementation of ModelManager with dual model support
 *
 * Handles:
 * - Dual model architecture (MobileBERT-384 + mALBERT-768)
 * - Auto-detection of best available model
 * - Model download from Hugging Face
 * - APK asset bundling for MobileBERT
 * - Progress tracking and caching
 *
 * Model Selection Priority:
 * 1. mALBERT-768 (external/downloaded) - Best quality, multilingual
 * 2. MobileBERT-384 (bundled in APK) - Fast fallback, English-only
 */
actual class ModelManager(private val context: Context) {

    // Package-aware paths (handles .debug suffix automatically)
    private val packageName = context.packageName
    private val isDebugBuild = packageName.endsWith(".debug")

    // External unified model repository (shared across AVA apps)
    private val externalModelsDir = File("/sdcard/ava-ai-models/embeddings")
    private val externalMalbertFile = File(externalModelsDir, ModelType.MALBERT.modelFileName)
    private val externalMobilebertFile = File(externalModelsDir, ModelType.MOBILEBERT.modelFileName)

    // Package-specific external storage paths (for both release and debug builds)
    private val packageDataPaths = listOf(
        File("/sdcard/Android/data/$packageName/files/models/nlu"),
        File("/sdcard/Android/data/com.augmentalis.ava/files/models/nlu"),
        File("/sdcard/Android/data/com.augmentalis.ava.debug/files/models/nlu")
    )

    // Internal app-specific storage
    private val internalModelsDir = File(context.filesDir.absolutePath, "models")
    private val internalMalbertFile = File(internalModelsDir, ModelType.MALBERT.modelFileName)
    private val internalMobilebertFile = File(internalModelsDir, ModelType.MOBILEBERT.modelFileName)
    private val vocabFile = File(internalModelsDir, "vocab.txt")

    // APK bundled model (MobileBERT only)
    private val apkMobilebertAssetPath = "models/${ModelType.MOBILEBERT.modelFileName}"

    // Active model (detected at runtime)
    private var activeModelType: ModelType? = null
    private var activeModelFile: File? = null

    /**
     * Model download URLs
     * TODO: Configure download URLs when AVA model hosting is ready
     * For now, models must be bundled in APK or placed in external storage manually
     */
    // DOWNLOAD SITE UNDEFINED - URLs commented out until hosting is configured
    // private val mobileBertUrl = "https://example.com/DOWNLOAD_SITE_UNDEFINED"
    // private val malbertUrl = "https://example.com/DOWNLOAD_SITE_UNDEFINED"
    // private val vocabUrl = "https://example.com/DOWNLOAD_SITE_UNDEFINED"

    init {
        if (!internalModelsDir.exists()) {
            internalModelsDir.mkdirs()
        }
        // Auto-detect best available model on initialization
        detectBestModel()
    }

    /**
     * Detect and select the best available model
     *
     * Priority (APK first, then package-specific, then external, then download):
     * 1. MobileBERT-384 (APK assets) - Bundled, always available
     * 2. mALBERT-768 (package data dir) - Package-specific paths (handles .debug)
     * 3. MobileBERT-384 (package data dir) - Package-specific paths
     * 4. mALBERT-768 (external) - Best quality if manually installed
     * 5. MobileBERT-384 (external) - Shared across AVA apps
     * 6. mALBERT-768 (internal) - Downloaded (future)
     * 7. MobileBERT-384 (internal) - Downloaded (future)
     *
     * @return Detected model type
     */
    private fun detectBestModel(): ModelType {
        android.util.Log.d(TAG, "Detecting best model (package: $packageName, debug: $isDebugBuild)")

        val detected = when {
            // Priority 1: MobileBERT from APK assets (bundled, always available)
            apkAssetExists() -> {
                activeModelFile = internalMobilebertFile // Will be copied from assets
                android.util.Log.i(TAG, "Using bundled MobileBERT from APK assets")
                ModelType.MOBILEBERT
            }
            // Priority 2: mALBERT from package-specific paths (handles .debug suffix)
            findModelInPackagePaths(ModelType.MALBERT.modelFileName) != null -> {
                activeModelFile = findModelInPackagePaths(ModelType.MALBERT.modelFileName)
                android.util.Log.i(TAG, "Using package-data mALBERT: ${activeModelFile?.absolutePath}")
                ModelType.MALBERT
            }
            // Priority 3: MobileBERT from package-specific paths
            findModelInPackagePaths(ModelType.MOBILEBERT.modelFileName) != null -> {
                activeModelFile = findModelInPackagePaths(ModelType.MOBILEBERT.modelFileName)
                android.util.Log.i(TAG, "Using package-data MobileBERT: ${activeModelFile?.absolutePath}")
                ModelType.MOBILEBERT
            }
            // Priority 4: mALBERT external (shared across AVA apps, best quality)
            findModelFileCaseInsensitive(externalModelsDir, ModelType.MALBERT.modelFileName) != null -> {
                activeModelFile = findModelFileCaseInsensitive(externalModelsDir, ModelType.MALBERT.modelFileName)
                android.util.Log.i(TAG, "Using external mALBERT: ${activeModelFile?.absolutePath}")
                ModelType.MALBERT
            }
            // Priority 5: MobileBERT external (shared)
            findModelFileCaseInsensitive(externalModelsDir, ModelType.MOBILEBERT.modelFileName) != null -> {
                activeModelFile = findModelFileCaseInsensitive(externalModelsDir, ModelType.MOBILEBERT.modelFileName)
                android.util.Log.i(TAG, "Using external MobileBERT: ${activeModelFile?.absolutePath}")
                ModelType.MOBILEBERT
            }
            // Priority 6: mALBERT internal (downloaded - future)
            findModelFileCaseInsensitive(internalModelsDir, ModelType.MALBERT.modelFileName) != null -> {
                activeModelFile = findModelFileCaseInsensitive(internalModelsDir, ModelType.MALBERT.modelFileName)
                ModelType.MALBERT
            }
            // Priority 7: MobileBERT internal (downloaded - future)
            findModelFileCaseInsensitive(internalModelsDir, ModelType.MOBILEBERT.modelFileName) != null -> {
                activeModelFile = findModelFileCaseInsensitive(internalModelsDir, ModelType.MOBILEBERT.modelFileName)
                ModelType.MOBILEBERT
            }
            // No model found - will trigger download (currently disabled)
            else -> {
                activeModelFile = internalMobilebertFile
                android.util.Log.w(TAG, "No model found - download required but DOWNLOAD SITE UNDEFINED")
                logSearchedPaths()
                ModelType.MOBILEBERT
            }
        }

        activeModelType = detected
        android.util.Log.i(TAG, "Detected model: ${detected.displayName} (${detected.embeddingDimension}-dim)")
        return detected
    }

    /**
     * Find model in package-specific data paths
     */
    private fun findModelInPackagePaths(fileName: String): File? {
        for (path in packageDataPaths) {
            val found = findModelFileCaseInsensitive(path, fileName)
            if (found != null) {
                android.util.Log.d(TAG, "Found model in package path: ${found.absolutePath}")
                return found
            }
        }
        return null
    }

    /**
     * Log all searched paths for debugging when model not found
     */
    private fun logSearchedPaths() {
        android.util.Log.w(TAG, "=== Model Search Paths ===")
        android.util.Log.w(TAG, "Package: $packageName (debug: $isDebugBuild)")

        val allPaths = packageDataPaths + listOf(externalModelsDir, internalModelsDir)
        allPaths.forEachIndexed { index, dir ->
            val exists = dir.exists()
            val files = if (exists) dir.listFiles()?.size ?: 0 else 0
            android.util.Log.w(TAG, "[$index] ${dir.absolutePath} (exists: $exists, files: $files)")
        }
    }

    /**
     * Find model file with case-insensitive matching
     * Supports both .AON and .aon extensions
     */
    private fun findModelFileCaseInsensitive(directory: File, fileName: String): File? {
        if (!directory.exists()) return null

        // Try exact match first
        val exactFile = File(directory, fileName)
        if (exactFile.exists()) return exactFile

        // Try case-insensitive match
        val files = directory.listFiles() ?: return null
        return files.find { it.name.equals(fileName, ignoreCase = true) }
    }

    /**
     * Get currently active model type
     */
    fun getActiveModelType(): ModelType {
        return activeModelType ?: detectBestModel()
    }

    /**
     * Check if models are available locally
     */
    actual fun isModelAvailable(): Boolean {
        detectBestModel()
        return (activeModelFile?.exists() == true || apkAssetExists()) && vocabFile.exists()
    }

    /**
     * Check if MobileBERT exists in APK assets (case-insensitive)
     */
    private fun apkAssetExists(): Boolean {
        return try {
            val assetFiles = context.assets.list("models") ?: return false
            val targetFileName = ModelType.MOBILEBERT.modelFileName
            assetFiles.any { it.equals(targetFileName, ignoreCase = true) }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to check APK assets", e)
            false
        }
    }

    /**
     * Get the actual asset file name (case-insensitive match)
     */
    private fun getApkAssetFileName(): String? {
        return try {
            val assetFiles = context.assets.list("models") ?: return null
            val targetFileName = ModelType.MOBILEBERT.modelFileName
            assetFiles.find { it.equals(targetFileName, ignoreCase = true) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get model file path
     */
    actual fun getModelPath(): String {
        detectBestModel()
        return activeModelFile?.absolutePath ?: internalMobilebertFile.absolutePath
    }

    /**
     * Get vocabulary file path
     */
    actual fun getVocabPath(): String {
        return vocabFile.absolutePath
    }

    /**
     * Download models if not available
     *
     * NOTE: Download functionality is currently disabled.
     * Models must be bundled in APK assets or placed manually in external storage.
     *
     * @param onProgress Callback for download progress (0.0 to 1.0)
     * @return Result indicating success or failure
     */
    actual suspend fun downloadModelsIfNeeded(
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // First try to copy from APK assets
            if (apkAssetExists() && !internalMobilebertFile.exists()) {
                android.util.Log.i(TAG, "Copying model from APK assets...")
                val copyResult = copyModelFromAssetsInternal()
                if (copyResult is Result.Success) {
                    onProgress(1.0f)
                    return@withContext Result.Success(Unit)
                }
            }

            if (isModelAvailable()) {
                onProgress(1.0f)
                return@withContext Result.Success(Unit)
            }

            // DOWNLOAD SITE UNDEFINED - Download functionality disabled
            // Models must be:
            // 1. Bundled in APK assets (models/AVA-384-Base-INT8.AON)
            // 2. Placed manually in /sdcard/ava-ai-models/embeddings/
            val errorMessage = """
                |DOWNLOAD SITE UNDEFINED
                |
                |NLU model download is currently unavailable.
                |Please ensure the model is either:
                |1. Bundled in APK assets at: models/${ModelType.MOBILEBERT.modelFileName}
                |2. Placed manually at: ${externalModelsDir.absolutePath}/${ModelType.MOBILEBERT.modelFileName}
                |
                |Contact support for model files.
            """.trimMargin()

            android.util.Log.e(TAG, errorMessage)

            Result.Error(
                exception = AVAException.ModelException(errorMessage),
                message = "DOWNLOAD SITE UNDEFINED - Model not available"
            )
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to load models: ${e.message}"
            )
        }
    }

    /**
     * Internal helper to copy model from assets (non-suspending for use in downloadModelsIfNeeded)
     */
    private fun copyModelFromAssetsInternal(): Result<Unit> {
        return try {
            val assetFileName = getApkAssetFileName()
            if (assetFileName == null) {
                return Result.Error(
                    exception = AVAException.ModelException("Model not found in APK assets"),
                    message = "Model file not found in APK assets"
                )
            }

            // Copy model
            if (!internalMobilebertFile.exists()) {
                context.assets.open("models/$assetFileName").use { input ->
                    FileOutputStream(internalMobilebertFile).use { output ->
                        input.copyTo(output)
                    }
                }
                android.util.Log.i(TAG, "Model copied from assets: ${internalMobilebertFile.length() / 1024 / 1024} MB")
            }

            // Copy vocabulary if exists
            if (!vocabFile.exists()) {
                try {
                    context.assets.open("models/vocab.txt").use { input ->
                        FileOutputStream(vocabFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    android.util.Log.i(TAG, "Vocabulary copied from assets")
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Vocabulary not found in assets, will need external source")
                }
            }

            detectBestModel()
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to copy from assets", e)
            Result.Error(e, "Failed to copy from assets: ${e.message}")
        }
    }

    // ========================================================================
    // DOWNLOAD FUNCTIONALITY - COMMENTED OUT UNTIL HOSTING IS CONFIGURED
    // ========================================================================
    // Uncomment and configure URLs when AVA model hosting is ready
    //
    // private suspend fun downloadFile(
    //     url: String,
    //     destination: File,
    //     onProgress: (Float) -> Unit
    // ): Result<Unit> = withContext(Dispatchers.IO) {
    //     try {
    //         val connection = java.net.URL(url).openConnection() as? java.net.HttpURLConnection
    //             ?: return@withContext Result.Error(
    //                 exception = AVAException.NetworkException("Failed to open HTTP connection"),
    //                 message = "Could not establish HTTP connection to $url"
    //             )
    //         connection.connectTimeout = 30000
    //         connection.readTimeout = 30000
    //
    //         val fileSize = connection.contentLength
    //
    //         // Use .use{} for automatic resource management (prevents leaks)
    //         connection.inputStream.use { input ->
    //             FileOutputStream(destination).use { output ->
    //                 val buffer = ByteArray(8192)
    //                 var totalBytesRead = 0L
    //                 var bytesRead: Int
    //
    //                 while (input.read(buffer).also { bytesRead = it } != -1) {
    //                     output.write(buffer, 0, bytesRead)
    //                     totalBytesRead += bytesRead
    //
    //                     if (fileSize > 0) {
    //                         onProgress(totalBytesRead.toFloat() / fileSize)
    //                     }
    //                 }
    //             } // output automatically closed
    //         } // input automatically closed
    //
    //         Result.Success(Unit)
    //     } catch (e: Exception) {
    //         if (destination.exists()) {
    //             destination.delete()
    //         }
    //         Result.Error(
    //             exception = e,
    //             message = "Download failed: ${e.message}"
    //         )
    //     }
    // }
    // ========================================================================

    /**
     * Copy model from assets (fallback for bundled models)
     */
    /**
     * Copy MobileBERT model from APK assets to internal storage
     *
     * This is the bundled fallback model included in the APK.
     * Only MobileBERT is bundled; mALBERT must be downloaded.
     */
    actual suspend fun copyModelFromAssets(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.i("ModelManager", "Copying MobileBERT from APK assets...")

            // Copy MobileBERT model from assets
            if (!internalMobilebertFile.exists()) {
                context.assets.open(apkMobilebertAssetPath).use { input ->
                    FileOutputStream(internalMobilebertFile).use { output ->
                        input.copyTo(output)
                    }
                }
                android.util.Log.i("ModelManager", "MobileBERT copied: ${internalMobilebertFile.length() / 1024 / 1024} MB")
            }

            // Copy vocabulary
            if (!vocabFile.exists()) {
                context.assets.open("models/vocab.txt").use { input ->
                    FileOutputStream(vocabFile).use { output ->
                        input.copyTo(output)
                    }
                }
                android.util.Log.i("ModelManager", "Vocabulary copied")
            }

            // Re-detect model after copying from assets
            detectBestModel()

            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ModelManager", "Failed to copy from assets", e)
            Result.Error(
                exception = e,
                message = "Failed to copy models from assets: ${e.message}"
            )
        }
    }

    /**
     * Delete downloaded models (for testing or cache clearing)
     */
    actual fun clearModels(): Result<Unit> {
        return try {
            // Clear both model types from internal storage
            if (internalMobilebertFile.exists()) {
                internalMobilebertFile.delete()
            }
            if (internalMalbertFile.exists()) {
                internalMalbertFile.delete()
            }
            if (vocabFile.exists()) {
                vocabFile.delete()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to clear models: ${e.message}"
            )
        }
    }

    /**
     * Get total size of downloaded models
     */
    actual fun getModelsSize(): Long {
        var totalSize = 0L
        // Check both model types
        if (internalMobilebertFile.exists()) {
            totalSize += internalMobilebertFile.length()
        }
        if (internalMalbertFile.exists()) {
            totalSize += internalMalbertFile.length()
        }
        if (vocabFile.exists()) {
            totalSize += vocabFile.length()
        }
        return totalSize
    }

    /**
     * Check if embeddings need migration due to model change
     *
     * Compares stored metadata with current model to detect:
     * - Dimension changes (384 ↔ 768)
     * - Model name changes (MobileBERT ↔ mALBERT)
     * - Model version updates
     * - Model checksum changes
     *
     * @param metadataQueries SQLDelight queries for accessing stored metadata
     * @return VersionStatus indicating if migration needed
     */
    suspend fun checkVersionStatus(
        metadataQueries: EmbeddingMetadataQueries
    ): Result<VersionStatus> = withContext(Dispatchers.IO) {
        try {
            val currentModel = getCurrentModelVersion()
            val storedMetadata = metadataQueries.selectActive().executeAsOneOrNull()

            val status = when {
                // New install - no metadata exists
                storedMetadata == null -> {
                    android.util.Log.i(TAG, "New install - no embeddings yet")
                    VersionStatus.NewInstall
                }

                // Model dimension changed (e.g., 384 → 768)
                storedMetadata.embedding_dimension.toInt() != currentModel.dimension -> {
                    android.util.Log.w(TAG, "Dimension mismatch: ${storedMetadata.embedding_dimension} → ${currentModel.dimension}")
                    VersionStatus.NeedsMigration(
                        fromModel = storedMetadata.model_name,
                        fromVersion = storedMetadata.model_version,
                        toModel = currentModel.name,
                        toVersion = currentModel.version,
                        reason = "Embedding dimension changed (${storedMetadata.embedding_dimension} → ${currentModel.dimension})"
                    )
                }

                // Model name changed (e.g., MobileBERT → mALBERT)
                storedMetadata.model_name != currentModel.name -> {
                    android.util.Log.w(TAG, "Model changed: ${storedMetadata.model_name} → ${currentModel.name}")
                    VersionStatus.NeedsMigration(
                        fromModel = storedMetadata.model_name,
                        fromVersion = storedMetadata.model_version,
                        toModel = currentModel.name,
                        toVersion = currentModel.version,
                        reason = "Model architecture changed"
                    )
                }

                // Model version changed (e.g., v1.0 → v1.1)
                storedMetadata.model_version != currentModel.version -> {
                    android.util.Log.w(TAG, "Version changed: ${storedMetadata.model_version} → ${currentModel.version}")
                    VersionStatus.NeedsMigration(
                        fromModel = storedMetadata.model_name,
                        fromVersion = storedMetadata.model_version,
                        toModel = currentModel.name,
                        toVersion = currentModel.version,
                        reason = "Model version updated"
                    )
                }

                // Model checksum changed (file replaced)
                storedMetadata.model_checksum != currentModel.checksum && currentModel.checksum != "unknown" -> {
                    android.util.Log.w(TAG, "Checksum changed: ${storedMetadata.model_checksum} → ${currentModel.checksum}")
                    VersionStatus.NeedsMigration(
                        fromModel = storedMetadata.model_name,
                        fromVersion = storedMetadata.model_version,
                        toModel = currentModel.name,
                        toVersion = currentModel.version,
                        reason = "Model file checksum changed (file replaced)"
                    )
                }

                // All matches - no migration needed
                else -> {
                    android.util.Log.i(TAG, "Model version current: ${currentModel.name} ${currentModel.version}")
                    VersionStatus.Current
                }
            }

            Result.Success(status)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Version check failed", e)
            Result.Error(e, "Version check failed: ${e.message}")
        }
    }

    /**
     * Get current model version information
     */
    fun getCurrentModelVersion(): ModelVersion {
        val modelType = getActiveModelType()
        return ModelVersion(
            name = modelType.displayName,
            version = modelType.getModelVersion(),
            dimension = modelType.embeddingDimension,
            checksum = calculateModelChecksum()
        )
    }

    /**
     * Calculate SHA-256 checksum of model file
     *
     * Used to detect if model file has been replaced.
     * Returns "unknown" if file doesn't exist or checksum fails.
     */
    private fun calculateModelChecksum(): String {
        return try {
            val modelFile = activeModelFile ?: return "unknown"
            if (!modelFile.exists()) return "unknown"

            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = modelFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                digest.digest()
            }

            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Checksum calculation failed", e)
            "unknown"
        }
    }

    companion object {
        private const val TAG = "ModelManager"

        // Expected model sizes for validation
        const val MOBILEBERT_MIN_SIZE = 10 * 1024 * 1024L // 10MB
        const val MOBILEBERT_MAX_SIZE = 20 * 1024 * 1024L // 20MB
        const val VOCAB_MIN_SIZE = 400 * 1024L // 400KB
        const val VOCAB_MAX_SIZE = 600 * 1024L // 600KB

        /**
         * Validate model file sizes
         */
        fun validateModelSizes(
            modelSize: Long,
            vocabSize: Long
        ): Boolean {
            val modelValid = modelSize in MOBILEBERT_MIN_SIZE..MOBILEBERT_MAX_SIZE
            val vocabValid = vocabSize in VOCAB_MIN_SIZE..VOCAB_MAX_SIZE
            return modelValid && vocabValid
        }
    }
}

/**
 * Model version status
 *
 * Indicates whether embeddings need migration due to model change
 */
sealed class VersionStatus {
    /**
     * Model version matches stored metadata - no migration needed
     */
    object Current : VersionStatus()

    /**
     * New install - no metadata exists yet
     */
    object NewInstall : VersionStatus()

    /**
     * Model changed - embeddings need migration
     */
    data class NeedsMigration(
        val fromModel: String,
        val fromVersion: String,
        val toModel: String,
        val toVersion: String,
        val reason: String
    ) : VersionStatus()
}

/**
 * Model version information
 *
 * Tracks all attributes needed to detect model changes
 */
data class ModelVersion(
    val name: String,          // "MobileBERT Lite" or "mALBERT Multilingual"
    val version: String,       // "MobileBERT-uncased-onnx-384"
    val dimension: Int,        // 384 or 768
    val checksum: String       // SHA-256 hash
)
