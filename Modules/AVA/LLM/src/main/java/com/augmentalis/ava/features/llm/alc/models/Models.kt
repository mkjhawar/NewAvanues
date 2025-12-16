/**
 * Data Models for ALC Engine Components
 *
 * Created: 2025-10-31
 */

package com.augmentalis.ava.features.llm.alc.models

/**
 * Configuration for loading a model
 */
data class ModelConfig(
    val modelPath: String,
    val modelName: String,
    val language: String = "en",
    val deviceType: String = "opencl",
    val contextLength: Int = 2048,
    val quantization: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * A loaded model instance
 */
data class LoadedModel(
    val config: ModelConfig,
    val handle: Any,  // Opaque handle to native model
    val vocabSize: Int,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Request for model inference
 */
data class InferenceRequest(
    val tokens: List<Int>,
    val cache: Any? = null,  // KV cache from previous inference
    val isPrefill: Boolean = false,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Result from model inference
 */
data class InferenceResult(
    val logits: FloatArray,
    val cache: Any?,  // Updated KV cache
    val tokensPerSecond: Float? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        val otherResult = other as? InferenceResult ?: return false

        if (!logits.contentEquals(otherResult.logits)) return false
        if (cache != otherResult.cache) return false
        if (tokensPerSecond != otherResult.tokensPerSecond) return false
        if (metadata != otherResult.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = logits.contentHashCode()
        result = 31 * result + (cache?.hashCode() ?: 0)
        result = 31 * result + (tokensPerSecond?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }
}

/**
 * Parameters for text generation
 */
data class GenerationParams(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val stopSequences: List<String> = emptyList(),
    val repeatPenalty: Float = 1.1f,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Parameters for token sampling
 */
data class SamplingParams(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f
)

/**
 * Event emitted during streaming generation
 */
sealed class StreamEvent {
    /**
     * A new token was generated
     */
    data class TokenGenerated(
        val token: Int,
        val text: String,
        val logProb: Float? = null
    ) : StreamEvent()

    /**
     * Generation completed
     */
    data class GenerationComplete(
        val totalTokens: Int,
        val duration: Long,
        val tokensPerSecond: Float
    ) : StreamEvent()

    /**
     * Generation error
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : StreamEvent()

    /**
     * Metadata event (e.g., cache hit, memory usage)
     */
    data class Metadata(
        val key: String,
        val value: Any
    ) : StreamEvent()
}

/**
 * Exception thrown during model loading
 */
class ModelLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown during inference
 */
class InferenceException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Model loading state for UI feedback.
 *
 * Provides detailed state information during model initialization,
 * allowing the UI to show appropriate feedback and recovery options.
 */
sealed class ModelLoadingState {
    /**
     * Initial state, no model operation in progress
     */
    object Idle : ModelLoadingState()

    /**
     * Model loading is in progress
     */
    data class Loading(
        val modelType: ModelTypeInfo,
        val progress: Float = 0f,
        val statusMessage: String = "Loading..."
    ) : ModelLoadingState()

    /**
     * Model loaded and ready for inference
     */
    data class Ready(
        val modelType: ModelTypeInfo,
        val modelPath: String,
        val loadTimeMs: Long
    ) : ModelLoadingState()

    /**
     * Model file(s) not found at expected location
     */
    data class Missing(
        val modelType: ModelTypeInfo,
        val searchedPaths: List<String>,
        val suggestedAction: MissingModelAction
    ) : ModelLoadingState()

    /**
     * Model loading failed with error
     */
    data class Error(
        val modelType: ModelTypeInfo,
        val message: String,
        val exception: Throwable? = null,
        val isRecoverable: Boolean = true
    ) : ModelLoadingState()

    /**
     * Model is incompatible with current device/runtime
     */
    data class Incompatible(
        val modelType: ModelTypeInfo,
        val reason: String,
        val requiredCapabilities: List<String>
    ) : ModelLoadingState()
}

/**
 * Types of models in the AVA system
 */
enum class ModelTypeInfo(val displayName: String, val requiredForChat: Boolean) {
    NLU("Intent Recognition", true),
    LLM("Language Model", true),
    EMBEDDING("Embedding Model", false),
    TTS("Text-to-Speech", false),
    WAKE_WORD("Wake Word Detection", false)
}

/**
 * Suggested actions when a model is missing
 */
enum class MissingModelAction(val displayText: String, val actionId: String) {
    DOWNLOAD("Download Model", "download"),
    COPY_FROM_STORAGE("Copy from Storage", "copy"),
    SELECT_DIFFERENT("Select Different Model", "select"),
    CONTINUE_WITHOUT("Continue without this model", "skip")
}
