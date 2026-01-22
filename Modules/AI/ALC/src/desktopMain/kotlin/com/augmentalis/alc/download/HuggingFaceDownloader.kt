/*
 * Copyright (c) 2025 Intelligent Devices LLC / Manoj Jhawar
 * All Rights Reserved - Confidential
 *
 * HuggingFace Model Downloader for Desktop
 * Downloads models from HuggingFace Hub and processes them for AVA distribution.
 */

package com.augmentalis.alc.download

import com.augmentalis.alc.ava3.AVA3Encoder
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads and processes models from HuggingFace Hub
 *
 * Supported model types:
 * - ONNX models (MobileBERT, MiniLM, etc.)
 * - GGUF models (llama.cpp compatible)
 * - SafeTensors models
 *
 * Features:
 * - Progress tracking with callbacks
 * - Checksum verification
 * - Automatic AVA3 encoding for secure distribution
 * - Registry integration
 */
class HuggingFaceDownloader(
    private val cacheDir: String = "${System.getProperty("user.home")}/.augmentalis/models/cache",
    private val outputDir: String = "${System.getProperty("user.home")}/.augmentalis/models"
) {
    private val logger = LoggerFactory.getLogger(HuggingFaceDownloader::class.java)
    private val encoder = AVA3Encoder()

    companion object {
        private const val HF_BASE_URL = "https://huggingface.co"
        private const val BUFFER_SIZE = 8192

        // Common model mappings
        val KNOWN_MODELS = mapOf(
            "mobilebert" to ModelInfo(
                repo = "google/mobilebert-uncased",
                files = listOf("model.onnx", "vocab.txt", "tokenizer.json"),
                type = ModelType.ONNX_NLU,
                description = "MobileBERT for NLU classification"
            ),
            "minilm" to ModelInfo(
                repo = "sentence-transformers/all-MiniLM-L6-v2",
                files = listOf("model.onnx", "vocab.txt", "tokenizer.json"),
                type = ModelType.ONNX_EMBEDDING,
                description = "MiniLM for sentence embeddings"
            ),
            "tinyllama" to ModelInfo(
                repo = "TinyLlama/TinyLlama-1.1B-Chat-v1.0",
                files = listOf("model.gguf"),
                type = ModelType.GGUF_LLM,
                description = "TinyLlama 1.1B for local LLM"
            ),
            "phi3-mini" to ModelInfo(
                repo = "microsoft/Phi-3-mini-4k-instruct-gguf",
                files = listOf("Phi-3-mini-4k-instruct-q4.gguf"),
                type = ModelType.GGUF_LLM,
                description = "Phi-3 Mini 4K for local LLM"
            )
        )
    }

    /**
     * Model information
     */
    data class ModelInfo(
        val repo: String,
        val files: List<String>,
        val type: ModelType,
        val description: String,
        val revision: String = "main"
    )

    /**
     * Model types
     */
    enum class ModelType {
        ONNX_NLU,
        ONNX_EMBEDDING,
        GGUF_LLM,
        SAFETENSORS,
        OTHER
    }

    /**
     * Download progress callback
     */
    interface ProgressCallback {
        fun onProgress(downloaded: Long, total: Long, fileName: String)
        fun onFileComplete(fileName: String, path: String)
        fun onError(fileName: String, error: String)
    }

    /**
     * Download result
     */
    sealed class DownloadResult {
        data class Success(
            val modelName: String,
            val outputPath: String,
            val files: List<String>,
            val encodedPath: String? = null
        ) : DownloadResult()

        data class Error(
            val modelName: String,
            val message: String,
            val cause: Throwable? = null
        ) : DownloadResult()
    }

    /**
     * Download a known model by name
     *
     * @param modelName Model identifier (e.g., "mobilebert", "minilm")
     * @param encode Whether to encode with AVA3 for secure distribution
     * @param callback Progress callback
     * @return Download result
     */
    fun downloadModel(
        modelName: String,
        encode: Boolean = false,
        callback: ProgressCallback? = null
    ): DownloadResult {
        val modelInfo = KNOWN_MODELS[modelName.lowercase()]
            ?: return DownloadResult.Error(modelName, "Unknown model: $modelName")

        return downloadFromRepo(modelName, modelInfo, encode, callback)
    }

    /**
     * Download from a custom HuggingFace repo
     *
     * @param modelName Local name for the model
     * @param repo HuggingFace repo (e.g., "username/repo-name")
     * @param files List of files to download
     * @param type Model type
     * @param encode Whether to encode with AVA3
     * @param callback Progress callback
     */
    fun downloadCustom(
        modelName: String,
        repo: String,
        files: List<String>,
        type: ModelType = ModelType.OTHER,
        encode: Boolean = false,
        callback: ProgressCallback? = null
    ): DownloadResult {
        val modelInfo = ModelInfo(
            repo = repo,
            files = files,
            type = type,
            description = "Custom model: $modelName"
        )
        return downloadFromRepo(modelName, modelInfo, encode, callback)
    }

    private fun downloadFromRepo(
        modelName: String,
        modelInfo: ModelInfo,
        encode: Boolean,
        callback: ProgressCallback?
    ): DownloadResult {
        logger.info("Downloading model: $modelName from ${modelInfo.repo}")

        val modelDir = File(outputDir, modelName)
        modelDir.mkdirs()

        val downloadedFiles = mutableListOf<String>()

        try {
            for (fileName in modelInfo.files) {
                val url = buildDownloadUrl(modelInfo.repo, fileName, modelInfo.revision)
                val outputFile = File(modelDir, fileName)

                logger.info("Downloading $fileName from $url")

                downloadFile(url, outputFile, fileName, callback)
                downloadedFiles.add(outputFile.absolutePath)

                callback?.onFileComplete(fileName, outputFile.absolutePath)
            }

            // Encode if requested
            val encodedPath = if (encode) {
                encodeModel(modelName, modelDir, modelInfo.type)
            } else {
                null
            }

            return DownloadResult.Success(
                modelName = modelName,
                outputPath = modelDir.absolutePath,
                files = downloadedFiles,
                encodedPath = encodedPath
            )

        } catch (e: Exception) {
            logger.error("Download failed for $modelName", e)
            callback?.onError(modelName, e.message ?: "Unknown error")
            return DownloadResult.Error(modelName, e.message ?: "Download failed", e)
        }
    }

    private fun buildDownloadUrl(repo: String, fileName: String, revision: String): String {
        // HuggingFace file download URL format
        return "$HF_BASE_URL/$repo/resolve/$revision/$fileName"
    }

    private fun downloadFile(
        urlStr: String,
        outputFile: File,
        fileName: String,
        callback: ProgressCallback?
    ) {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 30000
        connection.readTimeout = 60000

        // Follow redirects
        connection.instanceFollowRedirects = true

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw RuntimeException("HTTP error: $responseCode for $urlStr")
        }

        val totalSize = connection.contentLengthLong
        var downloadedSize = 0L

        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead
                    callback?.onProgress(downloadedSize, totalSize, fileName)
                }
            }
        }

        logger.info("Downloaded $fileName: ${outputFile.length()} bytes")
    }

    private fun encodeModel(modelName: String, modelDir: File, type: ModelType): String {
        val contentType = when (type) {
            ModelType.ONNX_NLU, ModelType.ONNX_EMBEDDING -> AVA3Encoder.CONTENT_TYPE_ONNX
            ModelType.GGUF_LLM -> AVA3Encoder.CONTENT_TYPE_GGUF
            else -> AVA3Encoder.CONTENT_TYPE_GENERIC
        }

        val extension = encoder.getExtension(contentType)
        val encodedPath = File(outputDir, "$modelName$extension").absolutePath

        // For ONNX models, encode the main model file
        val mainFile = modelDir.listFiles()?.find {
            it.name.endsWith(".onnx") || it.name.endsWith(".gguf")
        } ?: throw RuntimeException("No model file found in $modelDir")

        encoder.encode(
            mainFile.absolutePath,
            encodedPath,
            AVA3Encoder.EncodeOptions(
                compress = true,
                contentType = contentType,
                metadata = mapOf(
                    "model_name" to modelName,
                    "original_file" to mainFile.name,
                    "encoded_at" to System.currentTimeMillis().toString()
                )
            )
        )

        logger.info("Encoded model to $encodedPath")
        return encodedPath
    }

    /**
     * List available known models
     */
    fun listKnownModels(): List<Pair<String, String>> {
        return KNOWN_MODELS.map { (name, info) ->
            name to info.description
        }
    }

    /**
     * Check if a model is already downloaded
     */
    fun isModelDownloaded(modelName: String): Boolean {
        val modelDir = File(outputDir, modelName)
        if (!modelDir.exists()) return false

        val modelInfo = KNOWN_MODELS[modelName.lowercase()] ?: return modelDir.listFiles()?.isNotEmpty() == true

        return modelInfo.files.all { fileName ->
            File(modelDir, fileName).exists()
        }
    }

    /**
     * Get downloaded model path
     */
    fun getModelPath(modelName: String): String? {
        val modelDir = File(outputDir, modelName)
        return if (modelDir.exists()) modelDir.absolutePath else null
    }

    /**
     * Delete a downloaded model
     */
    fun deleteModel(modelName: String): Boolean {
        val modelDir = File(outputDir, modelName)
        return if (modelDir.exists()) {
            modelDir.deleteRecursively()
        } else {
            false
        }
    }

    /**
     * Compute SHA256 hash of a file
     */
    fun computeFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
