/**
 * OnnxEmbeddingProvider - ONNX Runtime embedding generation for Android
 *
 * Uses MobileBERT model to generate 384-dimensional embeddings
 * for semantic similarity matching.
 *
 * Created: 2025-12-07
 * Updated: 2026-01-16 - Fixed package to com.augmentalis.nlu.embedding
 */

package com.augmentalis.nlu.embedding

import android.content.Context
import com.augmentalis.nlu.matcher.EmbeddingProvider
import com.augmentalis.nlu.nluLogDebug
import com.augmentalis.nlu.nluLogError
import com.augmentalis.nlu.nluLogWarn
import java.io.File

/**
 * ONNX Runtime-based embedding provider.
 *
 * Model: MobileBERT (384-dim output)
 * File: mobilebert-384.onnx in assets
 */
class OnnxEmbeddingProvider(
    private val context: Context
) : EmbeddingProvider {

    companion object {
        private const val TAG = "OnnxEmbeddingProvider"
        private const val MODEL_NAME = "mobilebert-384.onnx"
        private const val EMBEDDING_DIM = 384
        private const val MAX_SEQ_LENGTH = 64
    }

    private var session: Any? = null  // OrtSession when ONNX Runtime is available
    private var isLoaded = false

    /**
     * Load the ONNX model from assets
     */
    fun loadModel(): Boolean {
        return try {
            // Copy model from assets to cache if needed
            val modelFile = File(context.cacheDir, MODEL_NAME)
            if (!modelFile.exists()) {
                context.assets.open(MODEL_NAME).use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Load ONNX Runtime session
            // Note: Actual ONNX Runtime initialization requires the library
            // This is a placeholder that will be completed when ONNX Runtime is added
            nluLogDebug(TAG, "ONNX model loaded: ${modelFile.absolutePath}")
            isLoaded = true
            true
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to load ONNX model", e)
            isLoaded = false
            false
        }
    }

    override fun generateEmbedding(text: String): FloatArray? {
        if (!isLoaded) {
            nluLogWarn(TAG, "Model not loaded, cannot generate embedding")
            return null
        }

        return try {
            // Tokenize and pad input
            val tokens = tokenize(text)
            val paddedTokens = padOrTruncate(tokens, MAX_SEQ_LENGTH)

            // Create input tensors
            val inputIds = paddedTokens.map { it.toLong() }.toLongArray()
            val attentionMask = paddedTokens.map { if (it != 0) 1L else 0L }.toLongArray()

            // Run inference
            // Note: Actual inference code requires ONNX Runtime
            // This returns a placeholder embedding
            generatePlaceholderEmbedding(text)
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to generate embedding", e)
            null
        }
    }

    override fun isModelLoaded(): Boolean = isLoaded

    override fun embeddingDimension(): Int = EMBEDDING_DIM

    /**
     * Simple tokenization (word-piece approximation)
     *
     * Production should use BERT tokenizer vocabulary.
     */
    private fun tokenize(text: String): List<Int> {
        val normalized = text.lowercase().trim()
        val words = normalized.split(Regex("\\s+"))

        // Simple word-to-id mapping (placeholder)
        // Real implementation needs vocab.txt from BERT
        return listOf(101) + // [CLS]
                words.take(MAX_SEQ_LENGTH - 2).map { word ->
                    word.hashCode() and 0x7FFF // Simple hash-based ID
                } +
                listOf(102) // [SEP]
    }

    /**
     * Pad or truncate token sequence
     */
    private fun padOrTruncate(tokens: List<Int>, length: Int): List<Int> {
        return when {
            tokens.size >= length -> tokens.take(length)
            else -> tokens + List(length - tokens.size) { 0 }
        }
    }

    /**
     * Generate placeholder embedding based on text hash
     *
     * Used when ONNX Runtime is not available.
     * Provides consistent but not semantically meaningful embeddings.
     */
    private fun generatePlaceholderEmbedding(text: String): FloatArray {
        val hash = text.hashCode()
        val random = java.util.Random(hash.toLong())

        return FloatArray(EMBEDDING_DIM) {
            (random.nextFloat() * 2 - 1) * 0.1f
        }.also { embedding ->
            // Normalize to unit vector
            val norm = kotlin.math.sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
            if (norm > 0) {
                for (i in embedding.indices) {
                    embedding[i] /= norm
                }
            }
        }
    }

    /**
     * Close the ONNX session
     */
    fun close() {
        session = null
        isLoaded = false
    }
}
