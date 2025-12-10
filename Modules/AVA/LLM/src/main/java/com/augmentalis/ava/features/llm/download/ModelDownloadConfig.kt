/**
 * Model Download Configuration for AVA
 *
 * Defines metadata and configuration for downloadable ML models.
 * Supports multiple model types (LLM, NLU, TTS, etc.) with versioning.
 *
 * Features:
 * - Model metadata (name, size, URL, checksum)
 * - Required vs optional models
 * - Model versions and compatibility
 * - Download source configuration
 *
 * Created: 2025-11-03
 * Author: AVA AI Team
 */

package com.augmentalis.ava.features.llm.download

/**
 * Model type enumeration
 */
enum class ModelType {
    /** Large Language Model */
    LLM,

    /** Natural Language Understanding */
    NLU,

    /** Text-to-Speech */
    TTS,

    /** Speech-to-Text */
    STT,

    /** Tokenizer */
    TOKENIZER,

    /** Vocabulary */
    VOCABULARY,

    /** Other model types */
    OTHER
}

/**
 * Model priority for download order
 */
enum class ModelPriority {
    /** Critical models required for basic functionality */
    CRITICAL,

    /** Important models for core features */
    HIGH,

    /** Optional models for enhanced features */
    MEDIUM,

    /** Nice-to-have models */
    LOW
}

/**
 * Download source configuration
 */
sealed class DownloadSource {
    /**
     * HTTP/HTTPS URL download
     *
     * @property url Direct download URL
     * @property headers Optional HTTP headers
     */
    data class Http(
        val url: String,
        val headers: Map<String, String> = emptyMap()
    ) : DownloadSource()

    /**
     * Hugging Face model hub
     *
     * @property repoId Repository ID (e.g., "onnx-community/mobilebert-uncased-ONNX")
     * @property filename File name within the repository
     * @property revision Git revision (branch, tag, or commit hash)
     */
    data class HuggingFace(
        val repoId: String,
        val filename: String,
        val revision: String = "main"
    ) : DownloadSource() {
        fun toUrl(): String = "https://huggingface.co/$repoId/resolve/$revision/$filename"
    }

    /**
     * Firebase Storage
     *
     * @property bucket Storage bucket name
     * @property path File path within bucket
     */
    data class FirebaseStorage(
        val bucket: String,
        val path: String
    ) : DownloadSource()

    /**
     * Google Cloud Storage
     *
     * @property bucket Storage bucket name
     * @property objectPath Object path within bucket
     */
    data class GoogleCloudStorage(
        val bucket: String,
        val objectPath: String
    ) : DownloadSource() {
        fun toUrl(): String = "https://storage.googleapis.com/$bucket/$objectPath"
    }
}

/**
 * Model download configuration
 *
 * @property id Unique identifier for the model
 * @property name Human-readable model name
 * @property type Model type
 * @property version Model version
 * @property description Optional description
 * @property source Download source
 * @property size Expected file size in bytes
 * @property checksum SHA-256 checksum for verification
 * @property priority Download priority
 * @property isRequired Whether model is required for app functionality
 * @property dependencies List of model IDs this model depends on
 * @property minApiLevel Minimum Android API level required
 * @property maxApiLevel Maximum Android API level supported (null = no limit)
 * @property metadata Additional metadata
 */
data class ModelDownloadConfig(
    val id: String,
    val name: String,
    val type: ModelType,
    val version: String,
    val description: String? = null,
    val source: DownloadSource,
    val size: Long,
    val checksum: String? = null,
    val priority: ModelPriority = ModelPriority.MEDIUM,
    val isRequired: Boolean = false,
    val dependencies: List<String> = emptyList(),
    val minApiLevel: Int = 21,
    val maxApiLevel: Int? = null,
    val metadata: Map<String, String> = emptyMap()
) {

    /**
     * Get human-readable size
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "%.2f GB".format(size / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Check if model is compatible with current API level
     */
    fun isCompatibleWithApiLevel(apiLevel: Int): Boolean {
        return apiLevel >= minApiLevel && (maxApiLevel == null || apiLevel <= maxApiLevel)
    }

    /**
     * Get download URL from source
     */
    fun getDownloadUrl(): String = when (source) {
        is DownloadSource.Http -> source.url
        is DownloadSource.HuggingFace -> source.toUrl()
        is DownloadSource.GoogleCloudStorage -> source.toUrl()
        is DownloadSource.FirebaseStorage -> {
            // Firebase Storage requires SDK, return placeholder
            "firebase://${source.bucket}/${source.path}"
        }
    }
}

/**
 * Collection of model configurations
 */
data class ModelRegistry(
    val models: List<ModelDownloadConfig>
) {

    /**
     * Get model by ID
     */
    fun getModel(id: String): ModelDownloadConfig? = models.find { it.id == id }

    /**
     * Get all required models
     */
    fun getRequiredModels(): List<ModelDownloadConfig> = models.filter { it.isRequired }

    /**
     * Get all optional models
     */
    fun getOptionalModels(): List<ModelDownloadConfig> = models.filter { !it.isRequired }

    /**
     * Get models by type
     */
    fun getModelsByType(type: ModelType): List<ModelDownloadConfig> = models.filter { it.type == type }

    /**
     * Get models by priority
     */
    fun getModelsByPriority(priority: ModelPriority): List<ModelDownloadConfig> =
        models.filter { it.priority == priority }

    /**
     * Get models sorted by priority (critical first)
     */
    fun getModelsByPriorityOrder(): List<ModelDownloadConfig> =
        models.sortedBy { it.priority.ordinal }

    /**
     * Get dependencies for a model
     */
    fun getDependencies(modelId: String): List<ModelDownloadConfig> {
        val model = getModel(modelId) ?: return emptyList()
        return model.dependencies.mapNotNull { getModel(it) }
    }

    /**
     * Get all dependencies recursively
     */
    fun getAllDependencies(modelId: String): List<ModelDownloadConfig> {
        val visited = mutableSetOf<String>()
        val result = mutableListOf<ModelDownloadConfig>()

        fun collectDependencies(id: String) {
            if (id in visited) return
            visited.add(id)

            val deps = getDependencies(id)
            for (dep in deps) {
                collectDependencies(dep.id)
                result.add(dep)
            }
        }

        collectDependencies(modelId)
        return result
    }

    /**
     * Calculate total size for models
     */
    fun getTotalSize(modelIds: List<String>): Long =
        modelIds.mapNotNull { getModel(it) }.sumOf { it.size }

    /**
     * Get models compatible with API level
     */
    fun getCompatibleModels(apiLevel: Int): List<ModelDownloadConfig> =
        models.filter { it.isCompatibleWithApiLevel(apiLevel) }
}

/**
 * Predefined model configurations for AVA
 */
object AVAModelRegistry {

    /**
     * Gemma 2B IT model (quantized)
     */
    val GEMMA_2B_IT = ModelDownloadConfig(
        id = "gemma-2b-it-q4",
        name = "Gemma 2B Instruct",
        type = ModelType.LLM,
        version = "1.0.0",
        description = "Quantized Gemma 2B model for on-device inference",
        source = DownloadSource.HuggingFace(
            repoId = "mlc-ai/gemma-2b-it-q4f16_1-MLC",
            filename = "params_shard_0.bin"
        ),
        size = 1_500_000_000L, // ~1.5GB
        checksum = ChecksumHelper.KnownChecksums.GEMMA_2B_IT_Q4_PARAMS,
        priority = ModelPriority.HIGH,
        isRequired = false,
        minApiLevel = 26
    )

    /**
     * MobileBERT INT8 for NLU
     */
    val MOBILEBERT_INT8 = ModelDownloadConfig(
        id = "mobilebert-uncased-int8",
        name = "MobileBERT INT8",
        type = ModelType.NLU,
        version = "1.0.0",
        description = "Quantized MobileBERT for intent classification",
        source = DownloadSource.HuggingFace(
            repoId = "onnx-community/mobilebert-uncased-ONNX",
            filename = "onnx/model_int8.onnx"
        ),
        size = 25_500_000L, // ~25.5MB
        checksum = ChecksumHelper.KnownChecksums.MOBILEBERT_INT8_ONNX,
        priority = ModelPriority.CRITICAL,
        isRequired = true,
        dependencies = listOf("mobilebert-vocab"),
        minApiLevel = 21
    )

    /**
     * MobileBERT Vocabulary
     */
    val MOBILEBERT_VOCAB = ModelDownloadConfig(
        id = "mobilebert-vocab",
        name = "MobileBERT Vocabulary",
        type = ModelType.VOCABULARY,
        version = "1.0.0",
        description = "Vocabulary file for MobileBERT tokenizer",
        source = DownloadSource.HuggingFace(
            repoId = "onnx-community/mobilebert-uncased-ONNX",
            filename = "vocab.txt"
        ),
        size = 460_000L, // ~460KB
        checksum = ChecksumHelper.KnownChecksums.MOBILEBERT_VOCAB,
        priority = ModelPriority.CRITICAL,
        isRequired = true,
        minApiLevel = 21
    )

    /**
     * Complete model registry
     */
    val REGISTRY = ModelRegistry(
        models = listOf(
            GEMMA_2B_IT,
            MOBILEBERT_INT8,
            MOBILEBERT_VOCAB
        )
    )
}
