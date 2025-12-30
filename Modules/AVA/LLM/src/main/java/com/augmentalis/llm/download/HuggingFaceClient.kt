/**
 * HuggingFace Hub Client for AVA
 *
 * Provides integration with HuggingFace Hub API to:
 * - List available models
 * - Get model metadata
 * - Generate download URLs
 *
 * Supports GGUF models for llama.cpp inference.
 *
 * Created: 2025-12-06
 * Author: AVA AI Team
 */

package com.augmentalis.llm.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * HuggingFace Hub API client
 */
class HuggingFaceClient(
    private val context: Context
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val HF_BASE_URL = "https://huggingface.co"
        private const val HF_API_BASE = "https://huggingface.co/api"

        // Predefined AVA-compatible models
        private val PREDEFINED_MODELS = listOf(
            ModelEntry(
                modelId = "AVA-GE2-2B16",
                repo = "google/gemma-2b-it-GGUF",
                filename = "gemma-2b-it-q4_k_m.gguf",
                name = "Gemma 2B Instruct (Q4_K_M)",
                sizeBytes = 1_500_000_000L, // ~1.5GB
                quantization = "Q4_K_M",
                description = "Compact 2B parameter model, balanced quality and speed"
            ),
            ModelEntry(
                modelId = "AVA-GE3-4B16",
                repo = "lmstudio-community/gemma-2-2b-it-GGUF",
                filename = "gemma-2-2b-it-Q4_K_M.gguf",
                name = "Gemma 2 2B Instruct (Q4_K_M)",
                sizeBytes = 1_700_000_000L, // ~1.7GB
                quantization = "Q4_K_M",
                description = "Improved Gemma 2 model with better instruction following"
            ),
            ModelEntry(
                modelId = "PHI3-MINI",
                repo = "microsoft/Phi-3-mini-4k-instruct-gguf",
                filename = "Phi-3-mini-4k-instruct-q4.gguf",
                name = "Phi-3 Mini 3.8B (Q4)",
                sizeBytes = 2_300_000_000L, // ~2.3GB
                quantization = "Q4",
                description = "Microsoft's Phi-3 Mini, excellent reasoning capabilities"
            ),
            ModelEntry(
                modelId = "QWEN2-1.5B",
                repo = "Qwen/Qwen2-1.5B-Instruct-GGUF",
                filename = "qwen2-1_5b-instruct-q4_k_m.gguf",
                name = "Qwen2 1.5B Instruct (Q4_K_M)",
                sizeBytes = 1_000_000_000L, // ~1GB
                quantization = "Q4_K_M",
                description = "Fast and efficient multilingual model"
            )
        )
    }

    /**
     * Model entry for predefined catalog
     */
    @Serializable
    data class ModelEntry(
        val modelId: String,
        val repo: String,
        val filename: String,
        val name: String,
        val sizeBytes: Long,
        val quantization: String?,
        val description: String?
    )

    /**
     * HuggingFace model metadata
     */
    @Serializable
    data class HFModelInfo(
        val id: String,
        val author: String? = null,
        val sha: String? = null,
        val downloads: Int = 0,
        val likes: Int = 0,
        val tags: List<String> = emptyList()
    )

    /**
     * Model file metadata
     */
    @Serializable
    data class ModelMetadata(
        val filename: String,
        val size: Long,
        val sha256: String? = null,
        val lfs: LFSInfo? = null
    )

    @Serializable
    data class LFSInfo(
        val size: Long,
        val sha256: String? = null,
        val pointer_size: Long = 0
    )

    /**
     * List available models from predefined catalog
     *
     * Returns curated list of AVA-compatible GGUF models.
     *
     * @return List of available models
     */
    suspend fun listAvailableModels(): List<LLMModelDownloader.ModelInfo> = withContext(Dispatchers.IO) {
        try {
            PREDEFINED_MODELS.map { entry ->
                LLMModelDownloader.ModelInfo(
                    modelId = entry.modelId,
                    name = entry.name,
                    sizeBytes = entry.sizeBytes,
                    url = getDownloadUrl(entry.repo, entry.filename),
                    checksum = null, // Checksum verification after download
                    description = entry.description,
                    quantization = entry.quantization,
                    runtime = "llama.cpp"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to list available models")
            emptyList()
        }
    }

    /**
     * Get model metadata from HuggingFace Hub
     *
     * Fetches file size and checksum information.
     *
     * @param repo Repository ID (e.g., "google/gemma-2b-it-GGUF")
     * @param filename File name within the repository
     * @return Model metadata
     */
    suspend fun getModelMetadata(
        repo: String,
        filename: String
    ): ModelMetadata? = withContext(Dispatchers.IO) {
        try {
            val url = "$HF_API_BASE/models/$repo/tree/main/$filename"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AVA-AI/1.0")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.w("Failed to fetch metadata: ${response.code}")
                return@withContext null
            }

            val body = response.body?.string()
            if (body.isNullOrEmpty()) {
                return@withContext null
            }

            json.decodeFromString<ModelMetadata>(body)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get model metadata: $repo/$filename")
            null
        }
    }

    /**
     * Get download URL for a model file
     *
     * @param repo Repository ID
     * @param filename File name
     * @param revision Git revision (branch, tag, or commit hash)
     * @return Direct download URL
     */
    fun getDownloadUrl(
        repo: String,
        filename: String,
        revision: String = "main"
    ): String {
        return "$HF_BASE_URL/$repo/resolve/$revision/$filename"
    }

    /**
     * Search HuggingFace Hub for models
     *
     * @param author Filter by author
     * @param filter Filter by tag (e.g., "gguf")
     * @return List of matching models
     */
    suspend fun searchModels(
        author: String? = null,
        filter: String = "gguf"
    ): List<HFModelInfo> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder("$HF_API_BASE/models")
            val params = mutableListOf<String>()

            if (author != null) {
                params.add("author=$author")
            }
            if (filter.isNotEmpty()) {
                params.add("filter=$filter")
            }

            if (params.isNotEmpty()) {
                urlBuilder.append("?${params.joinToString("&")}")
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .header("User-Agent", "AVA-AI/1.0")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.w("Failed to search models: ${response.code}")
                return@withContext emptyList()
            }

            val body = response.body?.string()
            if (body.isNullOrEmpty()) {
                return@withContext emptyList()
            }

            json.decodeFromString<List<HFModelInfo>>(body)

        } catch (e: Exception) {
            Timber.e(e, "Failed to search models")
            emptyList()
        }
    }
}
