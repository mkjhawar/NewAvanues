/**
 * EmbeddingPlugin.kt - Vector embedding plugin interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for embedding plugins that convert text to vector
 * representations for semantic search, similarity matching, and RAG pipelines.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.ai

import com.augmentalis.magiccode.plugins.universal.PluginCapability
import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.magiccode.plugins.universal.currentTimeMillis
import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/**
 * Plugin interface for text embedding/vectorization.
 *
 * EmbeddingPlugin extends [UniversalPlugin] to provide text-to-vector
 * conversion capabilities. Embeddings are essential for:
 *
 * - **Semantic Search**: Find similar content based on meaning
 * - **RAG Pipelines**: Retrieve relevant context for LLM prompts
 * - **Command Matching**: Find closest matching voice commands
 * - **Clustering**: Group similar elements or screens
 *
 * ## Capability
 * Implementations must advertise [PluginCapability.LLM_EMBEDDING].
 *
 * ## Implementation Example
 * ```kotlin
 * class SentenceTransformerPlugin : EmbeddingPlugin {
 *     override val pluginId = "com.augmentalis.embedding.sbert"
 *     override val dimension = 384
 *     override val maxInputLength = 512
 *
 *     override suspend fun embed(text: String): FloatArray {
 *         // Use sentence-transformers model to generate embedding
 *     }
 * }
 * ```
 *
 * ## Thread Safety
 * Implementations should be thread-safe. Multiple concurrent embedding
 * requests may be made.
 *
 * @since 1.0.0
 * @see UniversalPlugin
 */
interface EmbeddingPlugin : UniversalPlugin {

    // =========================================================================
    // Model Properties
    // =========================================================================

    /**
     * Dimension of the embedding vectors.
     *
     * Common dimensions:
     * - 384 for MiniLM/small sentence transformers
     * - 768 for BERT-base models
     * - 1024 for larger models
     * - 1536 for OpenAI ada-002
     * - 3072 for OpenAI text-embedding-3-large
     */
    val dimension: Int

    /**
     * Maximum input text length in tokens/characters.
     *
     * Texts longer than this will be truncated. Implementations should
     * handle truncation gracefully, preferring semantic boundaries.
     *
     * Common values:
     * - 256-512 for small models
     * - 8192 for OpenAI embedding models
     */
    val maxInputLength: Int

    // =========================================================================
    // Embedding Methods
    // =========================================================================

    /**
     * Generate an embedding for a single text.
     *
     * Converts the input text into a dense vector representation suitable
     * for similarity calculations and semantic search.
     *
     * ## Implementation Notes
     * - Should normalize the output vector (L2 norm = 1)
     * - Should handle empty strings gracefully (return zero vector)
     * - Should truncate texts exceeding [maxInputLength]
     *
     * @param text The text to embed
     * @return FloatArray of size [dimension]
     * @throws IllegalStateException if plugin is not ready
     */
    suspend fun embed(text: String): FloatArray

    /**
     * Generate embeddings for multiple texts in batch.
     *
     * More efficient than calling [embed] multiple times due to batching
     * optimizations in most embedding models.
     *
     * ## Performance
     * Batch processing is typically 2-10x faster than sequential calls.
     * Optimal batch sizes are usually 16-64 texts depending on the model.
     *
     * @param texts List of texts to embed
     * @return List of embeddings, one per input text
     * @throws IllegalStateException if plugin is not ready
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray>

    /**
     * Calculate cosine similarity between two embedding vectors.
     *
     * Returns a value between -1.0 and 1.0 where:
     * - 1.0 = identical direction (most similar)
     * - 0.0 = orthogonal (unrelated)
     * - -1.0 = opposite direction (most dissimilar)
     *
     * ## Usage
     * ```kotlin
     * val embedding1 = plugin.embed("hello world")
     * val embedding2 = plugin.embed("hi there")
     * val similarity = plugin.cosineSimilarity(embedding1, embedding2)
     * println("Similarity: $similarity") // ~0.85
     * ```
     *
     * @param a First embedding vector
     * @param b Second embedding vector
     * @return Cosine similarity score (-1.0 to 1.0)
     * @throws IllegalArgumentException if vectors have different dimensions
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float
}

// =============================================================================
// Default Implementation Helpers
// =============================================================================

/**
 * Default cosine similarity implementation.
 *
 * Can be used by plugins that don't have optimized similarity functions.
 *
 * @param a First embedding vector
 * @param b Second embedding vector
 * @return Cosine similarity score
 * @throws IllegalArgumentException if dimensions don't match
 */
fun defaultCosineSimilarity(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size) {
        "Embedding dimensions must match: ${a.size} vs ${b.size}"
    }

    if (a.isEmpty()) return 0f

    var dotProduct = 0f
    var normA = 0f
    var normB = 0f

    for (i in a.indices) {
        dotProduct += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }

    val denominator = sqrt(normA) * sqrt(normB)
    return if (denominator > 0f) dotProduct / denominator else 0f
}

/**
 * Calculate Euclidean distance between two embedding vectors.
 *
 * Lower values indicate more similar vectors.
 *
 * @param a First embedding vector
 * @param b Second embedding vector
 * @return Euclidean distance (0.0 = identical)
 * @throws IllegalArgumentException if dimensions don't match
 */
fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size) {
        "Embedding dimensions must match: ${a.size} vs ${b.size}"
    }

    var sumSquares = 0f
    for (i in a.indices) {
        val diff = a[i] - b[i]
        sumSquares += diff * diff
    }
    return sqrt(sumSquares)
}

/**
 * Calculate dot product between two embedding vectors.
 *
 * Useful for similarity when vectors are already normalized.
 *
 * @param a First embedding vector
 * @param b Second embedding vector
 * @return Dot product score
 * @throws IllegalArgumentException if dimensions don't match
 */
fun dotProduct(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size) {
        "Embedding dimensions must match: ${a.size} vs ${b.size}"
    }

    var product = 0f
    for (i in a.indices) {
        product += a[i] * b[i]
    }
    return product
}

/**
 * Normalize a vector to unit length (L2 norm = 1).
 *
 * @param vector The vector to normalize
 * @return Normalized vector
 */
fun normalizeVector(vector: FloatArray): FloatArray {
    var sumSquares = 0f
    for (v in vector) {
        sumSquares += v * v
    }
    val norm = sqrt(sumSquares)
    return if (norm > 0f) {
        FloatArray(vector.size) { i -> vector[i] / norm }
    } else {
        vector.copyOf()
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Embed and find similar texts.
 *
 * Embeds the query and corpus, then returns the most similar texts.
 *
 * @param query The query text
 * @param corpus List of texts to search
 * @param topK Number of results to return
 * @return List of pairs (text, similarity) sorted by similarity descending
 */
suspend fun EmbeddingPlugin.findSimilar(
    query: String,
    corpus: List<String>,
    topK: Int = 5
): List<Pair<String, Float>> {
    if (corpus.isEmpty()) return emptyList()

    val queryEmbedding = embed(query)
    val corpusEmbeddings = embedBatch(corpus)

    return corpus.zip(corpusEmbeddings)
        .map { (text, embedding) -> text to cosineSimilarity(queryEmbedding, embedding) }
        .sortedByDescending { it.second }
        .take(topK)
}

/**
 * Embed text and return with metadata.
 *
 * @param text Text to embed
 * @return EmbeddingResult with vector and metadata
 */
suspend fun EmbeddingPlugin.embedWithMetadata(text: String): EmbeddingResult {
    val startTime = currentTimeMillis()
    val embedding = embed(text)
    val duration = currentTimeMillis() - startTime

    return EmbeddingResult(
        text = text,
        embedding = embedding,
        dimension = dimension,
        latencyMs = duration
    )
}

/**
 * Check if text fits within the model's input length.
 *
 * @param text Text to check
 * @return true if text can be embedded without truncation
 */
fun EmbeddingPlugin.canEmbedWithoutTruncation(text: String): Boolean =
    text.length <= maxInputLength

// =============================================================================
// Helper Data Classes
// =============================================================================

/**
 * Result of an embedding operation with metadata.
 *
 * @property text Original input text
 * @property embedding The embedding vector
 * @property dimension Dimension of the embedding
 * @property latencyMs Time taken for embedding in milliseconds
 *
 * @since 1.0.0
 */
@Serializable
data class EmbeddingResult(
    val text: String,
    val embedding: FloatArray,
    val dimension: Int,
    val latencyMs: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EmbeddingResult

        if (text != other.text) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (dimension != other.dimension) return false
        if (latencyMs != other.latencyMs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + dimension
        result = 31 * result + latencyMs.hashCode()
        return result
    }
}

/**
 * Batch embedding result.
 *
 * @property results Individual embedding results
 * @property totalLatencyMs Total time for batch operation
 * @property averageLatencyMs Average time per embedding
 *
 * @since 1.0.0
 */
@Serializable
data class BatchEmbeddingResult(
    val results: List<EmbeddingResult>,
    val totalLatencyMs: Long,
    val averageLatencyMs: Long
) {
    /**
     * Number of texts embedded.
     */
    val count: Int get() = results.size

    /**
     * Get just the embeddings without metadata.
     */
    fun embeddings(): List<FloatArray> = results.map { it.embedding }
}

// currentTimeMillis imported from com.augmentalis.magiccode.plugins.universal
