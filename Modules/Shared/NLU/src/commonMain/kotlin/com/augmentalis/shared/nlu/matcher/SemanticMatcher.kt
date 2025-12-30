/**
 * SemanticMatcher - Embedding-based semantic similarity matching
 *
 * Third stage in hybrid classification using cosine similarity
 * between BERT embeddings.
 *
 * Created: 2025-12-07
 */

package com.augmentalis.shared.nlu.matcher

import com.augmentalis.shared.nlu.model.IntentMatch
import com.augmentalis.shared.nlu.model.MatchMethod
import com.augmentalis.shared.nlu.model.UnifiedIntent
import kotlin.math.sqrt

/**
 * Semantic matcher using pre-computed embeddings.
 *
 * Embedding generation is platform-specific (Android: ONNX Runtime).
 * This class handles similarity calculation and ranking.
 *
 * @property minSimilarity Minimum cosine similarity threshold
 * @property maxCandidates Maximum number of candidates to return
 */
class SemanticMatcher(
    private val minSimilarity: Float = 0.6f,
    private val maxCandidates: Int = 5
) {

    private var indexedIntents: List<UnifiedIntent> = emptyList()
    private var embeddingProvider: EmbeddingProvider? = null

    /**
     * Index intents with embeddings
     */
    fun index(intents: List<UnifiedIntent>) {
        indexedIntents = intents.filter { it.hasEmbedding }
    }

    /**
     * Set embedding provider for runtime embedding generation
     */
    fun setEmbeddingProvider(provider: EmbeddingProvider) {
        embeddingProvider = provider
    }

    /**
     * Find semantic matches for input
     *
     * @param input User input text
     * @param inputEmbedding Pre-computed embedding (optional, generated if null)
     * @return List of matches above similarity threshold
     */
    fun match(input: String, inputEmbedding: FloatArray? = null): List<IntentMatch> {
        val embedding = inputEmbedding ?: embeddingProvider?.generateEmbedding(input)
            ?: return emptyList()

        val candidates = mutableListOf<IntentMatch>()

        for (intent in indexedIntents) {
            intent.embedding?.let { intentEmbedding ->
                val similarity = cosineSimilarity(embedding, intentEmbedding)

                if (similarity >= minSimilarity) {
                    candidates.add(
                        IntentMatch(
                            intent = intent,
                            score = similarity,
                            matchedPhrase = intent.canonicalPhrase,
                            method = MatchMethod.SEMANTIC
                        )
                    )
                }
            }
        }

        return candidates
            .sortedByDescending { it.score * (1 + it.intent.priority * 0.05f) }
            .take(maxCandidates)
    }

    /**
     * Calculate cosine similarity between two vectors
     *
     * @return Similarity score 0.0-1.0
     */
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }

        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator == 0f) 0f else dotProduct / denominator
    }

    /**
     * Calculate Euclidean distance between two vectors
     */
    fun euclideanDistance(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size) return Float.MAX_VALUE

        var sum = 0f
        for (i in v1.indices) {
            val diff = v1[i] - v2[i]
            sum += diff * diff
        }

        return sqrt(sum)
    }

    /**
     * Find K nearest neighbors
     *
     * @param embedding Query embedding
     * @param k Number of neighbors
     * @return K nearest intents by cosine similarity
     */
    fun findKNearest(embedding: FloatArray, k: Int): List<IntentMatch> {
        return indexedIntents
            .mapNotNull { intent ->
                intent.embedding?.let { intentEmbedding ->
                    val similarity = cosineSimilarity(embedding, intentEmbedding)
                    IntentMatch(
                        intent = intent,
                        score = similarity,
                        matchedPhrase = intent.canonicalPhrase,
                        method = MatchMethod.SEMANTIC
                    )
                }
            }
            .sortedByDescending { it.score }
            .take(k)
    }

    /**
     * Check if semantic matching is available
     */
    fun isAvailable(): Boolean {
        return indexedIntents.isNotEmpty() && embeddingProvider != null
    }

    /**
     * Get count of intents with embeddings
     */
    fun embeddedIntentCount(): Int = indexedIntents.size

    /**
     * Clear index
     */
    fun clear() {
        indexedIntents = emptyList()
    }
}

/**
 * Interface for platform-specific embedding generation
 */
interface EmbeddingProvider {
    /**
     * Generate embedding vector for text
     *
     * @param text Input text
     * @return 384-dimension float array (MobileBERT)
     */
    fun generateEmbedding(text: String): FloatArray?

    /**
     * Check if embedding model is loaded
     */
    fun isModelLoaded(): Boolean

    /**
     * Get embedding dimension
     */
    fun embeddingDimension(): Int
}
