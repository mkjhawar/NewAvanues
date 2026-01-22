package com.augmentalis.alc.engine

import com.augmentalis.alc.domain.*
import kotlinx.coroutines.flow.Flow

/**
 * Core LLM provider interface
 *
 * Implemented by both cloud providers (Anthropic, OpenAI) and
 * local inference engines (TVM, CoreML, ONNX).
 */
interface ILLMProvider {
    val providerType: ProviderType
    val capabilities: LLMCapabilities

    /**
     * Generate a streaming response
     */
    fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions = GenerationOptions.DEFAULT
    ): Flow<LLMResponse>

    /**
     * Generate a complete response (non-streaming)
     */
    suspend fun complete(
        messages: List<ChatMessage>,
        options: GenerationOptions = GenerationOptions.DEFAULT
    ): LLMResponse.Complete

    /**
     * Check provider health
     */
    suspend fun healthCheck(): ProviderHealth

    /**
     * Get available models
     */
    suspend fun getModels(): List<ModelInfo>
}

/**
 * Local inference engine interface
 *
 * Extended interface for on-device LLM execution.
 * Implemented by platform-specific engines (TVM, CoreML, ONNX).
 */
interface IInferenceEngine : ILLMProvider {
    val isInitialized: Boolean
    val currentModel: String?

    /**
     * Initialize the inference engine
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Load a specific model
     */
    suspend fun loadModel(modelPath: String): Result<Unit>

    /**
     * Unload current model to free memory
     */
    suspend fun unloadModel()

    /**
     * Get memory usage statistics
     */
    fun getMemoryInfo(): MemoryInfo

    /**
     * Get engine statistics
     */
    fun getStats(): EngineStats

    /**
     * Stop current generation
     */
    suspend fun stop()

    /**
     * Reset engine state
     */
    suspend fun reset()

    /**
     * Cleanup and release resources
     */
    suspend fun cleanup()
}

/**
 * Memory information
 */
data class MemoryInfo(
    val usedBytes: Long,
    val availableBytes: Long,
    val modelSizeBytes: Long,
    val kvCacheSizeBytes: Long
)

/**
 * Engine statistics
 */
data class EngineStats(
    val totalTokensGenerated: Long,
    val averageTokensPerSecond: Float,
    val totalInferenceTimeMs: Long,
    val sessionCount: Int
)

/**
 * Streaming manager interface
 */
interface IStreamingManager {
    fun startStream(): Flow<String>
    suspend fun appendToken(token: String)
    suspend fun endStream()
    suspend fun cancelStream()
}

/**
 * Memory manager interface for platform-specific memory handling
 */
interface IMemoryManager {
    fun getCurrentUsage(): MemoryInfo
    fun isMemoryAvailable(requiredBytes: Long): Boolean
    suspend fun requestMemory(bytes: Long): Boolean
    suspend fun releaseMemory()
    fun onLowMemory()
}

/**
 * Model loader interface for platform-specific model loading
 */
interface IModelLoader {
    suspend fun loadModel(path: String): Result<Any>
    suspend fun unloadModel()
    fun isModelLoaded(): Boolean
    fun getModelPath(): String?
}

/**
 * Token sampler interface
 */
interface ITokenSampler {
    fun sample(
        logits: FloatArray,
        temperature: Float,
        topP: Float,
        topK: Int,
        repetitionPenalty: Float,
        previousTokens: List<Int>
    ): Int
}
