/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.features.nlu

import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Desktop (JVM) implementation of ModelManager
 *
 * Handles model download and management on desktop platforms:
 * - Detection of locally available models
 * - Download from HuggingFace with progress tracking
 * - Multi-platform path handling (Windows/macOS/Linux)
 *
 * Model Priority:
 * 1. Shared external models directory (~/.ava/models)
 * 2. App-specific models directory (./models)
 * 3. Models from classpath resources (fallback)
 */
actual class ModelManager {

    // User home directory models (shared across AVA apps)
    private val homeModelsDir = File(System.getProperty("user.home"), ".ava/models")

    // Current directory models
    private val currentDirModelsDir = File("models")

    // Vocab file
    private val vocabFile = File(currentDirModelsDir, "vocab.txt")

    // Active model (detected at runtime)
    private var activeModelFile: File? = null

    // Model download URLs
    private val mobileBertUrl = "https://huggingface.co/onnx-community/mobilebert-uncased-ONNX/resolve/main/onnx/model_int8.onnx"
    private val vocabUrl = "https://huggingface.co/onnx-community/mobilebert-uncased-ONNX/resolve/main/vocab.txt"

    init {
        // Auto-detect best available model on initialization
        detectBestModel()
    }

    /**
     * Detect and select the best available model
     *
     * Search priority:
     * 1. mobilebert_model.onnx in home directory
     * 2. mobilebert_model.onnx in current directory
     * 3. model.onnx in classpath (fallback)
     */
    private fun detectBestModel(): File? {
        // Priority 1: Home directory (shared)
        val homeModelFile = File(homeModelsDir, "mobilebert_model.onnx")
        if (homeModelFile.exists()) {
            activeModelFile = homeModelFile
            println("[ModelManager] Using shared model: ${homeModelFile.absolutePath}")
            return activeModelFile
        }

        // Priority 2: Current directory
        val currentModelFile = File(currentDirModelsDir, "mobilebert_model.onnx")
        if (currentModelFile.exists()) {
            activeModelFile = currentModelFile
            println("[ModelManager] Using local model: ${currentModelFile.absolutePath}")
            return activeModelFile
        }

        // Priority 3: Try alternate names
        val alternateNames = listOf("model.onnx", "bert_model.onnx")
        for (name in alternateNames) {
            val file = File(currentDirModelsDir, name)
            if (file.exists()) {
                activeModelFile = file
                println("[ModelManager] Using model: ${file.absolutePath}")
                return activeModelFile
            }
        }

        println("[ModelManager] No local model found - download required")
        return null
    }

    /**
     * Check if models are available locally
     */
    actual fun isModelAvailable(): Boolean {
        return (activeModelFile?.exists() == true && vocabFile.exists()) ||
                checkClasspathModel()
    }

    /**
     * Check if model exists in classpath (fallback)
     */
    private fun checkClasspathModel(): Boolean {
        return try {
            val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("models/mobilebert_model.onnx")
                ?: this::class.java.getResourceAsStream("/models/mobilebert_model.onnx")
            resourceStream != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get model file path
     */
    actual fun getModelPath(): String {
        // Return active model if available
        if (activeModelFile?.exists() == true) {
            return activeModelFile!!.absolutePath
        }

        // Try to return default location (might not exist yet)
        return File(currentDirModelsDir, "mobilebert_model.onnx").absolutePath
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
     * @param onProgress Callback for download progress (0.0 to 1.0)
     * @return Result indicating success or failure
     */
    actual suspend fun downloadModelsIfNeeded(
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isModelAvailable()) {
                println("[ModelManager] Models already available")
                return@withContext Result.Success(Unit)
            }

            // Ensure directories exist
            if (!currentDirModelsDir.exists()) {
                currentDirModelsDir.mkdirs()
            }

            val modelFile = File(currentDirModelsDir, "mobilebert_model.onnx")

            // Download MobileBERT model
            if (!modelFile.exists()) {
                println("[ModelManager] Downloading MobileBERT model...")
                onProgress(0.1f)

                val modelResult = downloadFile(
                    url = mobileBertUrl,
                    destination = modelFile,
                    onProgress = { progress ->
                        // Model is 90% of total
                        onProgress(0.1f + (progress * 0.8f))
                    }
                )

                when (modelResult) {
                    is Result.Error -> return@withContext modelResult
                    else -> {}
                }
            }

            // Download vocabulary
            if (!vocabFile.exists()) {
                println("[ModelManager] Downloading vocabulary...")
                onProgress(0.9f)

                val vocabResult = downloadFile(
                    url = vocabUrl,
                    destination = vocabFile,
                    onProgress = { progress ->
                        // Vocab is 10% of total
                        onProgress(0.9f + (progress * 0.1f))
                    }
                )

                when (vocabResult) {
                    is Result.Error -> return@withContext vocabResult
                    else -> {}
                }
            }

            onProgress(1.0f)
            detectBestModel()
            println("[ModelManager] Download complete")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("[ModelManager] Download failed: ${e.message}")
            Result.Error(
                exception = e,
                message = "Failed to download models: ${e.message}"
            )
        }
    }

    /**
     * Download file with progress tracking
     */
    private suspend fun downloadFile(
        url: String,
        destination: File,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            println("[ModelManager] Downloading from: $url")

            val connection = URL(url).openConnection() as? HttpURLConnection
                ?: return@withContext Result.Error(
                    exception = IllegalStateException("Failed to open HTTP connection"),
                    message = "Could not establish HTTP connection to $url"
                )

            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "AVA-Desktop/1.0")

            val fileSize = connection.contentLength

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.Error(
                    exception = IllegalStateException("HTTP ${connection.responseCode}"),
                    message = "Server returned HTTP ${connection.responseCode}"
                )
            }

            // Download with progress tracking
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var totalBytesRead = 0L
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (fileSize > 0) {
                            onProgress(totalBytesRead.toFloat() / fileSize)
                        }
                    }
                }
            }

            connection.disconnect()

            // Validate downloaded file
            if (!destination.exists() || destination.length() == 0L) {
                return@withContext Result.Error(
                    exception = IllegalStateException("Download produced empty file"),
                    message = "Downloaded file is empty or missing"
                )
            }

            println("[ModelManager] Downloaded: ${destination.name} (${destination.length() / 1024 / 1024} MB)")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("[ModelManager] Download error: ${e.message}")
            if (destination.exists()) {
                destination.delete()
            }
            Result.Error(
                exception = e,
                message = "Download failed: ${e.message}"
            )
        }
    }

    /**
     * Copy model from classpath resources (fallback)
     *
     * For development: place models in src/main/resources/models/
     */
    actual suspend fun copyModelFromAssets(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            println("[ModelManager] Copying models from classpath resources...")

            if (!currentDirModelsDir.exists()) {
                currentDirModelsDir.mkdirs()
            }

            // Copy model from classpath
            val modelResourcePath = "/models/mobilebert_model.onnx"
            val vocabResourcePath = "/models/vocab.txt"

            val modelStream = this::class.java.getResourceAsStream(modelResourcePath)
            if (modelStream != null) {
                val modelFile = File(currentDirModelsDir, "mobilebert_model.onnx")
                modelStream.use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
                println("[ModelManager] Copied model from resources: ${modelFile.length() / 1024 / 1024} MB")
            }

            val vocabStream = this::class.java.getResourceAsStream(vocabResourcePath)
            if (vocabStream != null) {
                vocabStream.use { input ->
                    FileOutputStream(vocabFile).use { output ->
                        input.copyTo(output)
                    }
                }
                println("[ModelManager] Copied vocabulary from resources")
            }

            detectBestModel()
            Result.Success(Unit)
        } catch (e: Exception) {
            println("[ModelManager] Failed to copy from resources: ${e.message}")
            Result.Error(
                exception = e,
                message = "Failed to copy models from resources: ${e.message}"
            )
        }
    }

    /**
     * Delete downloaded models (for cache clearing)
     */
    actual fun clearModels(): Result<Unit> {
        return try {
            val modelFile = File(currentDirModelsDir, "mobilebert_model.onnx")
            if (modelFile.exists()) {
                modelFile.delete()
                println("[ModelManager] Deleted model file")
            }
            if (vocabFile.exists()) {
                vocabFile.delete()
                println("[ModelManager] Deleted vocabulary file")
            }
            activeModelFile = null
            Result.Success(Unit)
        } catch (e: Exception) {
            println("[ModelManager] Failed to clear models: ${e.message}")
            Result.Error(
                exception = e,
                message = "Failed to clear models: ${e.message}"
            )
        }
    }

    /**
     * Get total size of downloaded models in bytes
     */
    actual fun getModelsSize(): Long {
        var totalSize = 0L
        val modelFile = File(currentDirModelsDir, "mobilebert_model.onnx")
        if (modelFile.exists()) {
            totalSize += modelFile.length()
        }
        if (vocabFile.exists()) {
            totalSize += vocabFile.length()
        }
        return totalSize
    }

    /**
     * Get the active model type (for consistency with Android implementation)
     *
     * Desktop always uses MobileBERT-384 by default
     *
     * @return ModelType.MOBILEBERT
     */
    fun getActiveModelType(): ModelType {
        return ModelType.MOBILEBERT
    }
}

/**
 * Model type enumeration for cross-platform consistency
 *
 * Desktop implementation supports:
 * - MOBILEBERT: 384-dim, fast, English-only
 *
 * Future support:
 * - MALBERT: 768-dim, multilingual (requires separate download)
 */
enum class ModelType(
    val modelFileName: String,
    val displayName: String,
    val embeddingDimension: Int
) {
    MOBILEBERT("mobilebert_model.onnx", "MobileBERT Lite", 384),
    MALBERT("malbert_model.onnx", "mALBERT Multilingual", 768);

    /**
     * Get model version string for version tracking
     */
    fun getModelVersion(): String = when (this) {
        MOBILEBERT -> "MobileBERT-uncased-onnx-384"
        MALBERT -> "mALBERT-multilingual-v2-768"
    }
}
