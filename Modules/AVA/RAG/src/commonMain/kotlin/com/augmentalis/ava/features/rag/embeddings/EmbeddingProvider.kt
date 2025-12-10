// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/embeddings/EmbeddingProvider.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.embeddings

import com.augmentalis.ava.features.rag.domain.Embedding

/**
 * Interface for generating text embeddings
 *
 * Embeddings are vector representations of text used for semantic search.
 * Different providers (ONNX, local LLM, cloud API) can be used based on
 * availability and performance requirements.
 */
interface EmbeddingProvider {
    /**
     * Name of this embedding provider
     */
    val name: String

    /**
     * Dimension of embeddings produced by this provider
     */
    val dimension: Int

    /**
     * Whether this provider is currently available
     *
     * For example, ONNX may not be available on some platforms,
     * cloud API requires internet connection, etc.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Generate embedding for a single text
     *
     * @param text Text to embed
     * @return Embedding vector
     */
    suspend fun embed(text: String): Result<Embedding.Float32>

    /**
     * Generate embeddings for multiple texts (batched for efficiency)
     *
     * @param texts Texts to embed
     * @return List of embedding vectors
     */
    suspend fun embedBatch(texts: List<String>): Result<List<Embedding.Float32>>

    /**
     * Estimate time to embed a batch (for scheduling)
     *
     * @param count Number of texts
     * @return Estimated time in milliseconds
     */
    fun estimateTimeMs(count: Int): Long
}

/**
 * Factory for creating embedding providers
 */
expect object EmbeddingProviderFactory {
    /**
     * Get ONNX embedding provider
     *
     * @param modelPath Path to ONNX model file
     * @return ONNX provider or null if not available
     */
    fun getONNXProvider(modelPath: String): EmbeddingProvider?

    /**
     * Get local LLM embedding provider
     *
     * Uses the existing LLM loaded for chat to generate embeddings
     *
     * @return Local LLM provider or null if not available
     */
    fun getLocalLLMProvider(): EmbeddingProvider?

    /**
     * Get cloud API embedding provider
     *
     * @param apiKey API key for the service
     * @param endpoint API endpoint URL
     * @return Cloud provider or null if not available
     */
    fun getCloudProvider(apiKey: String, endpoint: String): EmbeddingProvider?
}

/**
 * Result of embedding operations
 */
data class EmbeddingResult(
    val embeddings: List<Embedding.Float32>,
    val timeMs: Long,
    val provider: String
)

/**
 * Metrics for embedding performance
 */
data class EmbeddingMetrics(
    val totalEmbedded: Int,
    val totalTimeMs: Long,
    val averageTimeMs: Long,
    val provider: String
)
