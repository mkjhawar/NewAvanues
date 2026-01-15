package com.augmentalis.ava.features.nlu.aon

import android.content.Context
import com.augmentalis.ava.core.common.Result
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.augmentalis.ava.features.nlu.BertTokenizer
import com.augmentalis.ava.features.nlu.IntentClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Computes mALBERT embeddings from AVA 2.0 .aot ontology files
 *
 * This class integrates directly with the .aot format to generate embeddings
 * that are stored in the database for fast runtime lookup.
 *
 * AVA 2.0 .aot Integration:
 * 1. Parse .aot file → SemanticIntentOntologyData objects
 * 2. For each ontology entry:
 *    - Combine description + synonyms into embedding text
 *    - Tokenize with mALBERT tokenizer (TVM 0.22 compatible)
 *    - Run through mALBERT model (768-dim output)
 *    - L2 normalize embedding vector
 *    - Serialize to ByteArray
 *    - Create IntentEmbeddingData
 * 3. Insert embeddings into database
 *
 * Key Features:
 * - .aot-native: Directly reads AVA 2.0 ontology format
 * - Multilingual: mALBERT supports 100+ languages
 * - Efficient: Pre-computation happens once at startup
 * - Persistent: Embeddings cached in database
 *
 * Example usage:
 * ```kotlin
 * val computer = AonEmbeddingComputer(context, intentClassifier)
 * val result = computer.computeEmbeddingsFromAon(ontologyEntity)
 * when (result) {
 *     is Result.Success -> {
 *         val embedding = result.data
 *         // Insert into database
 *     }
 *     is Result.Error -> Log.e(TAG, "Failed: ${result.message}")
 * }
 * ```
 */
/**
 * Data class for intent embedding (replaces Room IntentEmbeddingEntity)
 * Used for computing and storing embeddings before inserting into SQLDelight
 */
data class IntentEmbeddingData(
    val intentId: String,
    val locale: String,
    val embeddingVector: ByteArray,
    val embeddingDimension: Int,
    val modelVersion: String,
    val normalizationType: String = "l2",
    val ontologyId: String? = null,
    val exampleCount: Int = 1,
    val source: String = "AON_SEMANTIC"
) {
    /**
     * Deserialize embedding vector from ByteArray to FloatArray
     */
    fun getEmbedding(): FloatArray {
        val buffer = ByteBuffer.wrap(embeddingVector).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(embeddingDimension) { buffer.float }
    }

    companion object {
        /**
         * Serialize FloatArray embedding to ByteArray for database storage
         */
        fun serializeEmbedding(embedding: FloatArray): ByteArray {
            val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            embedding.forEach { buffer.putFloat(it) }
            return buffer.array()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntentEmbeddingData) return false
        return intentId == other.intentId && locale == other.locale
    }

    override fun hashCode(): Int {
        return 31 * intentId.hashCode() + locale.hashCode()
    }
}

class AonEmbeddingComputer(
    private val context: Context,
    private val intentClassifier: IntentClassifier
) {

    companion object {
        private const val TAG = "AonEmbeddingComputer"
    }

    /**
     * Get embedding dimension from active model
     *
     * Dynamically determines dimension based on which model is loaded:
     * - MobileBERT: 384-dim
     * - mALBERT: 768-dim
     */
    private fun getEmbeddingDimension(): Int {
        return intentClassifier.getEmbeddingDimension()
    }

    /**
     * Get model version identifier for database storage
     */
    private fun getModelVersion(): String {
        val modelManager = com.augmentalis.ava.features.nlu.ModelManager(context)
        return modelManager.getActiveModelType().getModelVersion()
    }

    /**
     * Compute embedding for a single ontology entry from .aot file
     *
     * This method creates a rich semantic representation by combining:
     * - description: Semantic intent description for zero-shot learning
     * - canonical_form: Canonical representation of intent
     * - synonyms: All synonym phrases
     *
     * The combined text is tokenized and embedded using mALBERT,
     * producing a 768-dimensional vector that captures semantic meaning.
     *
     * @param ontology Ontology entry from parsed .aot file
     * @return IntentEmbeddingData ready for database insertion
     */
    suspend fun computeEmbeddingFromOntology(
        ontology: SemanticIntentOntologyData
    ): Result<IntentEmbeddingData> = withContext(Dispatchers.Default) {
        try {
            android.util.Log.d(TAG, "Computing embedding for intent: ${ontology.intentId} (${ontology.locale})")

            // Step 1: Create embedding text from .aot ontology data
            val embeddingText = createEmbeddingTextFromOntology(ontology)

            android.util.Log.d(TAG, "  Embedding text (${embeddingText.length} chars): ${embeddingText.take(100)}...")

            // Step 2: Tokenize with mALBERT tokenizer (TVM 0.22 compatible)
            val tokenizer = BertTokenizer(context)
            val tokens = tokenizer.tokenize(embeddingText)

            android.util.Log.d(TAG, "  Tokenized: ${tokens.inputIds.size} tokens")

            // Step 3: Compute embedding using mALBERT model
            // Note: IntentClassifier currently uses MobileBERT (384-dim)
            // TODO: Switch to mALBERT (768-dim) in production
            val embedding = computeRawEmbedding(embeddingText)

            android.util.Log.d(TAG, "  Raw embedding computed: ${embedding.size} dimensions")

            // Step 4: L2 normalize (essential for cosine similarity)
            val normalizedEmbedding = l2Normalize(embedding)

            android.util.Log.d(TAG, "  Normalized embedding (L2 norm should be ~1.0)")

            // Step 5: Serialize to ByteArray for database storage
            val embeddingBytes = IntentEmbeddingData.serializeEmbedding(normalizedEmbedding)

            android.util.Log.d(TAG, "  Serialized to ByteArray: ${embeddingBytes.size} bytes")

            // Step 6: Create IntentEmbeddingData
            val entity = IntentEmbeddingData(
                intentId = ontology.intentId,
                locale = ontology.locale,
                embeddingVector = embeddingBytes,
                embeddingDimension = normalizedEmbedding.size,
                modelVersion = getModelVersion(),
                normalizationType = "l2",
                ontologyId = ontology.ontologyFileSource,
                exampleCount = ontology.synonyms.size + 1,  // synonyms + canonical form
                source = "AON_SEMANTIC"
            )

            android.util.Log.i(TAG, "✓ Successfully computed embedding for ${ontology.intentId}")
            Result.Success(entity)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to compute embedding for ${ontology.intentId}", e)
            Result.Error(
                exception = e,
                message = "Embedding computation failed: ${e.message}"
            )
        }
    }

    /**
     * Create rich embedding text from .aot ontology entry
     *
     * Combines multiple fields from the .aot format:
     * 1. Description: Semantic description for zero-shot learning
     * 2. Canonical form: Primary intent representation
     * 3. Synonyms: All alternative phrasings
     *
     * This creates a comprehensive semantic representation that captures
     * the full meaning of the intent.
     *
     * Example for "send_email":
     * ```
     * User wants to compose and send an electronic message or email.
     * compose and send email
     * send email, compose email, write email, create email, new email
     * ```
     */
    private fun createEmbeddingTextFromOntology(ontology: SemanticIntentOntologyData): String {
        val parts = mutableListOf<String>()

        // 1. Add description (most important for semantic understanding)
        parts.add(ontology.description)

        // 2. Add canonical form (normalized intent representation)
        parts.add(ontology.canonicalForm.replace("_", " "))

        // 3. Add all synonyms (alternative phrasings)
        if (ontology.synonyms.isNotEmpty()) {
            parts.add(ontology.synonyms.joinToString(", "))
        }

        // Join with newlines for clarity (tokenizer handles whitespace)
        return parts.joinToString("\n")
    }

    /**
     * Compute raw embedding using IntentClassifier's BERT model
     *
     * NOTE: Currently uses IntentClassifier's MobileBERT model (384-dim).
     * The model dimension is 384, not 768, so we update EMBEDDING_DIMENSION accordingly.
     *
     * @param text Input text to embed
     * @return Raw (unnormalized) embedding vector from IntentClassifier
     */
    private suspend fun computeRawEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        // Use IntentClassifier's computeEmbeddingVector() method
        // This uses the ONNX Runtime model that's already initialized
        when (val result = intentClassifier.computeEmbeddingVector(text)) {
            is Result.Success -> result.data
            is Result.Error -> throw result.exception
        }
    }

    /**
     * L2 normalize a vector to unit length
     *
     * Essential for cosine similarity: normalized vectors allow
     * cosine similarity to be computed as simple dot product.
     *
     * Formula: v_normalized = v / ||v||_2
     * where ||v||_2 = sqrt(sum(v[i]^2))
     *
     * @param vector Raw embedding vector
     * @return Normalized vector with L2 norm = 1.0
     */
    private fun l2Normalize(vector: FloatArray): FloatArray {
        var magnitude = 0.0f
        for (value in vector) {
            magnitude += value * value
        }
        magnitude = kotlin.math.sqrt(magnitude)

        return if (magnitude > 0) {
            FloatArray(vector.size) { i -> vector[i] / magnitude }
        } else {
            vector
        }
    }

    /**
     * Batch compute embeddings for multiple ontology entries
     *
     * Efficiently processes all intents from a .aot file in one pass.
     *
     * @param ontologies List of ontology entries from .aot file
     * @return List of IntentEmbeddingData objects ready for database insertion
     */
    suspend fun computeEmbeddingsFromOntologies(
        ontologies: List<SemanticIntentOntologyData>
    ): Result<List<IntentEmbeddingData>> = withContext(Dispatchers.Default) {
        try {
            android.util.Log.i(TAG, "=== Batch Computing Embeddings for ${ontologies.size} intents ===")

            val embeddings = mutableListOf<IntentEmbeddingData>()
            var successCount = 0
            var failureCount = 0

            for (ontology in ontologies) {
                when (val result = computeEmbeddingFromOntology(ontology)) {
                    is Result.Success -> {
                        embeddings.add(result.data)
                        successCount++
                    }
                    is Result.Error -> {
                        android.util.Log.w(TAG, "Failed to compute embedding for ${ontology.intentId}: ${result.message}")
                        failureCount++
                    }
                }
            }

            android.util.Log.i(TAG, "=== Batch Complete: $successCount success, $failureCount failures ===")

            if (embeddings.isEmpty()) {
                Result.Error(
                    exception = IllegalStateException("No embeddings computed"),
                    message = "Failed to compute any embeddings"
                )
            } else {
                Result.Success(embeddings)
            }

        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Batch embedding computation failed: ${e.message}"
            )
        }
    }

    /**
     * Compute embeddings from a parsed .aot file
     *
     * Complete pipeline: .aot file → embeddings → database
     *
     * @param aonFile Parsed .aot file
     * @return List of IntentEmbeddingData objects
     */
    suspend fun computeEmbeddingsFromAonFile(
        aonFile: AonFile
    ): Result<List<IntentEmbeddingData>> {
        android.util.Log.i(TAG, "Computing embeddings from .aot file: ${aonFile.sourceFile}")
        android.util.Log.i(TAG, "  Locale: ${aonFile.locale}")
        android.util.Log.i(TAG, "  Ontologies: ${aonFile.ontologies.size}")

        return computeEmbeddingsFromOntologies(aonFile.ontologies)
    }

    /**
     * Verify embedding quality
     *
     * Checks:
     * - Vector dimension matches expected (384 for MobileBERT, 768 for mALBERT)
     * - L2 norm is approximately 1.0
     * - No NaN or infinite values
     *
     * @param embedding Computed embedding
     * @return true if embedding passes quality checks
     */
    fun verifyEmbeddingQuality(embedding: IntentEmbeddingData): Boolean {
        try {
            val vector = embedding.getEmbedding()
            val expectedDim = getEmbeddingDimension()

            // Check dimension
            if (vector.size != expectedDim) {
                android.util.Log.w(TAG, "  ✗ Dimension mismatch: ${vector.size} != $expectedDim")
                return false
            }

            // Check for NaN or infinite values
            if (vector.any { it.isNaN() || it.isInfinite() }) {
                android.util.Log.w(TAG, "  ✗ Contains NaN or infinite values")
                return false
            }

            // Check L2 norm (should be ~1.0 for normalized vector)
            var norm = 0.0f
            for (value in vector) {
                norm += value * value
            }
            norm = kotlin.math.sqrt(norm)

            if (kotlin.math.abs(norm - 1.0f) > 0.01f) {
                android.util.Log.w(TAG, "  ✗ L2 norm not normalized: $norm (expected ~1.0)")
                return false
            }

            android.util.Log.d(TAG, "  ✓ Embedding quality verified (dim=$expectedDim, norm=$norm)")
            return true

        } catch (e: Exception) {
            android.util.Log.e(TAG, "  ✗ Quality verification failed", e)
            return false
        }
    }
}
