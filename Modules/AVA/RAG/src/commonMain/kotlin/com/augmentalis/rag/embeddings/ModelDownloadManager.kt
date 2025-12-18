// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/embeddings/ModelDownloadManager.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.embeddings

import kotlinx.coroutines.flow.Flow

/**
 * Manages downloading and caching of ONNX embedding models
 *
 * Downloads models on-demand instead of bundling in APK.
 * Reduces initial app size by ~90MB.
 */
interface ModelDownloadManager {
    /**
     * Check if model is downloaded and ready to use
     */
    suspend fun isModelAvailable(modelId: String): Boolean

    /**
     * Get local path to downloaded model file
     *
     * @return Local file path or null if not downloaded
     */
    suspend fun getModelPath(modelId: String): String?

    /**
     * Download a model from remote source
     *
     * @param modelId Model identifier (e.g., "all-MiniLM-L6-v2")
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Result with local file path
     */
    suspend fun downloadModel(
        modelId: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>

    /**
     * Get download progress as Flow
     *
     * Emits progress values from 0.0 to 1.0
     */
    fun observeDownloadProgress(modelId: String): Flow<DownloadProgress>

    /**
     * Delete a downloaded model to free space
     */
    suspend fun deleteModel(modelId: String): Result<Unit>

    /**
     * Get information about a model
     */
    suspend fun getModelInfo(modelId: String): ModelInfo?

    /**
     * List all available models (downloadable)
     */
    suspend fun listAvailableModels(): List<ModelInfo>

    /**
     * List all downloaded models
     */
    suspend fun listDownloadedModels(): List<ModelInfo>

    /**
     * Verify model integrity (checksum)
     */
    suspend fun verifyModel(modelId: String): Result<Boolean>
}

/**
 * Download progress state
 */
sealed class DownloadProgress {
    object Idle : DownloadProgress()
    object Starting : DownloadProgress()
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float
    ) : DownloadProgress()
    data class Verifying(val progress: Float) : DownloadProgress()
    data class Completed(val filePath: String) : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
    object Cancelled : DownloadProgress()
}

/**
 * Model metadata
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val dimension: Int,
    val downloadUrl: String,
    val sha256: String? = null,
    val version: String = "1.0",
    val isDownloaded: Boolean = false,
    val localPath: String? = null
)

/**
 * Predefined models available for download
 */
object AvailableModels {
    // English-only models
    val AVA_ONX_384_BASE = ModelInfo(
        id = "AVA-ONX-384-BASE",
        name = "English Base (FP32)",
        description = "all-MiniLM-L6-v2 - Bundled in app for immediate use",
        sizeBytes = 90_400_000L, // ~90MB
        dimension = 384,
        downloadUrl = "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx",
        sha256 = null
    )

    val AVA_ONX_384_FAST = ModelInfo(
        id = "AVA-ONX-384-FAST",
        name = "Fast & Small (FP32)",
        description = "paraphrase-MiniLM-L3-v2 - Faster, smaller model",
        sizeBytes = 61_000_000L, // ~61MB
        dimension = 384,
        downloadUrl = "https://huggingface.co/sentence-transformers/paraphrase-MiniLM-L3-v2/resolve/main/onnx/model.onnx",
        sha256 = null
    )

    val AVA_ONX_768_QUAL = ModelInfo(
        id = "AVA-ONX-768-QUAL",
        name = "High Quality (FP32)",
        description = "all-mpnet-base-v2 - Best quality, larger file",
        sizeBytes = 420_000_000L, // ~420MB
        dimension = 768,
        downloadUrl = "https://huggingface.co/sentence-transformers/all-mpnet-base-v2/resolve/main/onnx/model.onnx",
        sha256 = null
    )

    // Multilingual models
    val AVA_ONX_384_MULTI = ModelInfo(
        id = "AVA-ONX-384-MULTI",
        name = "Multilingual (FP32)",
        description = "paraphrase-multilingual-MiniLM-L12-v2 - 50+ languages",
        sizeBytes = 470_000_000L, // ~470MB
        dimension = 384,
        downloadUrl = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx",
        sha256 = null
    )

    val AVA_ONX_768_MULTI = ModelInfo(
        id = "AVA-ONX-768-MULTI",
        name = "Multilingual High Quality (FP32)",
        description = "paraphrase-multilingual-mpnet-base-v2 - 50+ languages, best quality",
        sizeBytes = 1_100_000_000L, // ~1.1GB
        dimension = 768,
        downloadUrl = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-mpnet-base-v2/resolve/main/onnx/model.onnx",
        sha256 = null
    )

    fun getDefault() = AVA_ONX_384_BASE

    fun getMultilingualDefault() = AVA_ONX_384_MULTI

    fun getAll() = listOf(
        AVA_ONX_384_BASE,
        AVA_ONX_384_FAST,
        AVA_ONX_768_QUAL,
        AVA_ONX_384_MULTI,
        AVA_ONX_768_MULTI
    )

    fun getEnglishModels() = listOf(
        AVA_ONX_384_BASE,
        AVA_ONX_384_FAST,
        AVA_ONX_768_QUAL
    )

    fun getMultilingualModels() = listOf(
        AVA_ONX_384_MULTI,
        AVA_ONX_768_MULTI
    )

    fun getById(id: String) = getAll().find { it.id == id }
}
