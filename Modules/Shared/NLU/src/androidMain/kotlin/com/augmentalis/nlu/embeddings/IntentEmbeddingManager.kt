/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from IntentClassifier (SRP)
 */

package com.augmentalis.nlu.embeddings

import android.content.Context
import android.util.Log
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.createDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * IntentEmbeddingManager - Single Responsibility: Intent Embedding Storage/Retrieval
 *
 * Extracted from IntentClassifier as part of SOLID refactoring.
 * Handles all embedding-related operations:
 * - Pre-computed intent embedding storage
 * - Database-based trained embedding persistence
 * - Embedding similarity calculations
 * - Embedding precomputation orchestration
 *
 * Thread-safe: Uses synchronized map for embedding storage.
 *
 * @param context Android context for database access
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-12-17
 */
class IntentEmbeddingManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "IntentEmbeddingManager"
    }

    // ==================== State ====================

    /**
     * Pre-computed intent embeddings for semantic similarity matching.
     * Key: intent name, Value: normalized embedding vector
     * Thread-safe: Uses synchronized map for concurrent access safety.
     */
    private val intentEmbeddings: MutableMap<String, FloatArray> =
        java.util.Collections.synchronizedMap(mutableMapOf())

    /**
     * Flag indicating whether precomputation is complete.
     */
    private val _isPreComputationComplete = MutableStateFlow(false)
    val isPreComputationComplete: StateFlow<Boolean> = _isPreComputationComplete.asStateFlow()

    /**
     * Database instance for persisted embeddings.
     */
    private val database: AVADatabase by lazy {
        DatabaseDriverFactory(context).createDriver().createDatabase()
    }

    // ==================== Embedding Storage ====================

    /**
     * Get the number of loaded embeddings.
     */
    fun getEmbeddingCount(): Int = intentEmbeddings.size

    /**
     * Check if embeddings are loaded.
     */
    fun hasEmbeddings(): Boolean = intentEmbeddings.isNotEmpty()

    /**
     * Get embedding for an intent.
     *
     * @param intent Intent name
     * @return Embedding vector or null if not found
     */
    fun getEmbedding(intent: String): FloatArray? = intentEmbeddings[intent]

    /**
     * Add or update an embedding.
     *
     * @param intent Intent name
     * @param embedding Normalized embedding vector
     */
    fun addEmbedding(intent: String, embedding: FloatArray) {
        intentEmbeddings[intent] = embedding
    }

    /**
     * Remove an embedding.
     *
     * @param intent Intent name
     */
    fun removeEmbedding(intent: String) {
        intentEmbeddings.remove(intent)
    }

    /**
     * Clear all embeddings.
     */
    fun clearEmbeddings() {
        intentEmbeddings.clear()
    }

    /**
     * Get all intent names with embeddings.
     */
    fun getIntentNames(): Set<String> = intentEmbeddings.keys.toSet()

    // ==================== Similarity Calculations ====================

    /**
     * Compute similarity scores between a query embedding and all stored intents.
     *
     * @param queryEmbedding Normalized query embedding vector
     * @return List of (intent, similarity) pairs sorted by similarity descending
     */
    fun computeSimilarities(queryEmbedding: FloatArray): List<Pair<String, Float>> {
        return intentEmbeddings.map { (intent, embedding) ->
            intent to cosineSimilarity(queryEmbedding, embedding)
        }.sortedByDescending { it.second }
    }

    /**
     * Find best matching intent for a query embedding.
     *
     * @param queryEmbedding Normalized query embedding vector
     * @param candidates List of candidate intents to consider
     * @return Best matching intent and confidence score, or null if no embeddings
     */
    fun findBestMatch(
        queryEmbedding: FloatArray,
        candidates: List<String>
    ): Pair<String, Float>? {
        if (intentEmbeddings.isEmpty()) return null

        var bestIntent: String? = null
        var bestScore = Float.MIN_VALUE

        for (intent in candidates) {
            val embedding = intentEmbeddings[intent] ?: continue
            val score = cosineSimilarity(queryEmbedding, embedding)
            if (score > bestScore) {
                bestScore = score
                bestIntent = intent
            }
        }

        return bestIntent?.let { it to bestScore }
    }

    /**
     * Calculate cosine similarity between two vectors.
     *
     * @param a First vector (should be normalized)
     * @param b Second vector (should be normalized)
     * @return Cosine similarity in range [-1, 1]
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vector dimensions must match: ${a.size} vs ${b.size}" }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }

    // ==================== Database Operations ====================

    /**
     * Load trained embeddings from database.
     * ADR-013: Self-learning NLU with LLM-as-Teacher
     */
    suspend fun loadTrainedEmbeddings() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading trained embeddings from database...")

            // Load from intent_embedding table (pre-computed embeddings)
            val storedEmbeddings = database.intentEmbeddingQueries.selectAll().executeAsList()

            for (stored in storedEmbeddings) {
                val embedding = bytesToFloatArray(stored.embedding_vector)
                if (embedding.isNotEmpty()) {
                    intentEmbeddings[stored.intent_id] = embedding
                    Log.d(TAG, "Loaded embedding: ${stored.intent_id}")
                }
            }

            Log.i(TAG, "Loaded ${storedEmbeddings.size} embeddings from database")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load embeddings: ${e.message}", e)
        }
    }

    /**
     * Save a trained embedding to database.
     *
     * Issue 3.4 Fix: Atomic update - only update in-memory map after successful DB write.
     * Previously, the in-memory map was updated regardless of DB success, causing
     * inconsistent state if the database write failed.
     *
     * @param intentName Intent name
     * @param embedding Embedding vector
     * @param utterance Original utterance used for training (stored as source)
     * @return true if save succeeded, false otherwise
     */
    suspend fun saveTrainedEmbedding(
        intentName: String,
        embedding: FloatArray,
        utterance: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val embeddingBytes = floatArrayToBytes(embedding)
            val now = System.currentTimeMillis()

            // Issue 3.4: Write to database FIRST, then update in-memory map
            // This ensures we don't have stale in-memory state if DB fails
            database.intentEmbeddingQueries.insert(
                intent_id = intentName,
                locale = "en",
                embedding_vector = embeddingBytes,
                embedding_dimension = embedding.size.toLong(),
                model_version = "mALBERT-1.0",
                normalization_type = "l2",
                ontology_id = null,
                created_at = now,
                updated_at = now,
                example_count = 1,
                source = "USER_TRAINED"
            )

            // Database write succeeded - now safe to update in-memory map
            // Using synchronized map ensures thread-safe update
            intentEmbeddings[intentName] = embedding

            Log.d(TAG, "Saved trained embedding: $intentName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save trained embedding: ${e.message}", e)
            false
        }
    }

    // ==================== Precomputation ====================

    /**
     * Mark precomputation as complete.
     */
    fun markPreComputationComplete() {
        _isPreComputationComplete.value = true
        Log.i(TAG, "Pre-computation complete. Loaded ${intentEmbeddings.size} embeddings")
    }

    /**
     * Log embedding status summary.
     */
    fun logEmbeddingStatus() {
        Log.i(TAG, "=== Embedding Status ===")
        Log.i(TAG, "Total embeddings: ${intentEmbeddings.size}")
        intentEmbeddings.keys.take(10).forEach { intent ->
            Log.d(TAG, "  ✓ $intent")
        }
        if (intentEmbeddings.size > 10) {
            Log.d(TAG, "  ... and ${intentEmbeddings.size - 10} more")
        }
    }

    // ==================== Utility Functions ====================

    /**
     * Convert byte array to float array (for database retrieval).
     */
    private fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        if (bytes.isEmpty()) return floatArrayOf()

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(bytes.size / 4)
        buffer.asFloatBuffer().get(floats)
        return floats
    }

    /**
     * Convert float array to byte array (for database storage).
     */
    private fun floatArrayToBytes(floats: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asFloatBuffer().put(floats)
        return buffer.array()
    }

    /**
     * L2 normalize a vector.
     *
     * @param vector Input vector
     * @return Normalized vector with unit length
     */
    fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0f
        for (v in vector) {
            sumSquares += v * v
        }
        val norm = kotlin.math.sqrt(sumSquares)
        return if (norm > 0) {
            FloatArray(vector.size) { vector[it] / norm }
        } else {
            vector.copyOf()
        }
    }
}
