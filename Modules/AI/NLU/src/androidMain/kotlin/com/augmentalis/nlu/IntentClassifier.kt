/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.nlu

import ai.onnxruntime.OnnxTensor
import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.common.AVAException
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.createDatabase
import com.augmentalis.nlu.embeddings.IntentEmbeddingManager
import com.augmentalis.nlu.inference.OnnxSessionManager
import com.augmentalis.nlu.locale.LocaleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.LongBuffer

/**
 * Android implementation of IntentClassifier using ONNX Runtime Mobile
 *
 * Refactored to follow SOLID principles:
 * - Uses OnnxSessionManager for ONNX inference (SRP)
 * - Uses IntentEmbeddingManager for embedding storage/retrieval (SRP)
 * - Acts as a facade/coordinator for NLU operations
 *
 * Performance targets:
 * - Inference: < 50ms (target), < 100ms (max)
 * - Model size: ~12-15MB
 * - Memory: < 100MB peak
 *
 * VOS4 Pattern: Singleton with lazy initialization
 */
actual class IntentClassifier private constructor(
    private val context: Context,
    private val onnxSessionManager: OnnxSessionManager = OnnxSessionManager(context),
    private val embeddingManager: IntentEmbeddingManager = IntentEmbeddingManager(context)
) {

    private lateinit var tokenizer: BertTokenizer
    private var isInitialized = false

    // Locale management for multi-language support
    private val localeManager = LocaleManager(context)

    // Mutex to prevent concurrent initialization
    private val initializationMutex = Mutex()

    // Delegate pre-computation state to embedding manager
    val isPreComputationComplete: StateFlow<Boolean> = embeddingManager.isPreComputationComplete

    /**
     * Initialize ONNX Runtime and load model
     * Call this once during app startup
     */
    actual suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        // Use mutex to prevent concurrent initialization
        initializationMutex.withLock {
            try {
                if (isInitialized) {
                    android.util.Log.d("IntentClassifier", "Already initialized, skipping")
                    return@withContext Result.Success(Unit)
                }

                android.util.Log.d("IntentClassifier", "Starting initialization...")

                // Delegate ONNX session initialization to manager
                val onnxResult = onnxSessionManager.initialize(modelPath)
                if (onnxResult is Result.Error) {
                    return@withContext onnxResult
                }

                // Initialize tokenizer
                tokenizer = BertTokenizer(context)

                // Pre-compute intent embeddings for semantic matching
                precomputeIntentEmbeddings()

                // ADR-013: Load trained embeddings from self-learning
                embeddingManager.loadTrainedEmbeddings()

                // Log embedding status
                android.util.Log.i("IntentClassifier", "=== NLU Initialization Complete ===")
                embeddingManager.logEmbeddingStatus()

                isInitialized = true
                embeddingManager.markPreComputationComplete()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(
                    exception = e,
                    message = "Failed to initialize ONNX Runtime: ${e.message}"
                )
            }
        }
    }

    /**
     * Compute embedding vector for text
     *
     * Exposed for AON embedding precomputation during initialization.
     * Returns RAW (unnormalized, mean-pooled) embedding vector.
     *
     * Used by AonEmbeddingComputer to generate embeddings for semantic ontology entries.
     *
     * Dimension depends on active model:
     * - MobileBERT: 384-dim
     * - mALBERT: 768-dim
     *
     * @param text Input text to embed
     * @return Result containing raw embedding vector or error if classifier not initialized
     */
    suspend fun computeEmbeddingVector(text: String): Result<FloatArray> {
        if (!isInitialized) {
            return Result.Error(
                exception = AVAException.InitializationException("Classifier not initialized"),
                message = "Intent classifier not initialized. Call initialize() first."
            )
        }
        return try {
            Result.Success(computeRawEmbedding(text))
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to compute embedding: ${e.message}"
            )
        }
    }

    /**
     * Get active model's embedding dimension
     *
     * @return Embedding dimension (384 for MobileBERT, 768 for mALBERT)
     */
    fun getEmbeddingDimension(): Int {
        return ModelManager(context).getActiveModelType().embeddingDimension
    }

    /**
     * Classify intent from user utterance
     *
     * @param utterance User input text
     * @param candidateIntents List of possible intents to classify against
     * @return Result with intent and confidence score
     */
    actual suspend fun classifyIntent(
        utterance: String,
        candidateIntents: List<String>
    ): Result<IntentClassification> = withContext(Dispatchers.Default) {
        try {
            if (!isInitialized) {
                return@withContext Result.Error(
                    exception = IllegalStateException("Classifier not initialized"),
                    message = "Call initialize() first"
                )
            }

            if (utterance.isBlank()) {
                return@withContext Result.Error(
                    exception = IllegalArgumentException("Empty utterance"),
                    message = "Utterance cannot be empty"
                )
            }

            if (candidateIntents.isEmpty()) {
                return@withContext Result.Error(
                    exception = IllegalArgumentException("No candidate intents"),
                    message = "At least one intent required"
                )
            }

            // Tokenize input
            val tokens = tokenizer.tokenize(utterance)
            val inputIds = tokens.inputIds
            val attentionMask = tokens.attentionMask
            val tokenTypeIds = tokens.tokenTypeIds

            // Get ONNX environment from manager
            val ortEnvironment = onnxSessionManager.getEnvironment()
                ?: throw IllegalStateException("ONNX environment not initialized")

            // Create ONNX tensors with resource management
            var inputIdsTensor: OnnxTensor? = null
            var attentionMaskTensor: OnnxTensor? = null
            var tokenTypeIdsTensor: OnnxTensor? = null
            var outputs: ai.onnxruntime.OrtSession.Result? = null

            try {
                inputIdsTensor = OnnxTensor.createTensor(
                    ortEnvironment,
                    LongBuffer.wrap(inputIds.map { it.toLong() }.toLongArray()),
                    longArrayOf(1, inputIds.size.toLong())
                )
                attentionMaskTensor = OnnxTensor.createTensor(
                    ortEnvironment,
                    LongBuffer.wrap(attentionMask.map { it.toLong() }.toLongArray()),
                    longArrayOf(1, attentionMask.size.toLong())
                )
                tokenTypeIdsTensor = OnnxTensor.createTensor(
                    ortEnvironment,
                    LongBuffer.wrap(tokenTypeIds.map { it.toLong() }.toLongArray()),
                    longArrayOf(1, tokenTypeIds.size.toLong())
                )

                // Delegate inference to ONNX session manager with timing
                val inputs = mapOf(
                    "input_ids" to inputIdsTensor,
                    "attention_mask" to attentionMaskTensor,
                    "token_type_ids" to tokenTypeIdsTensor
                )

                val (sessionOutputs, inferenceTime) = onnxSessionManager.runWithTiming(inputs)
                outputs = sessionOutputs

                // Extract embeddings from last_hidden_state
                // MobileBERT outputs: [batch_size, sequence_length, hidden_size=384]
                // We use the [CLS] token embedding (first token) for classification
                val outputValue = outputs.get(0)
                val outputTensor = outputValue as? OnnxTensor
                    ?: throw IllegalStateException(
                        "Invalid model output format: expected OnnxTensor, got ${outputValue?.javaClass?.simpleName ?: "null"}"
                    )

                val floatBuffer = outputTensor.floatBuffer
                val outputShape = outputTensor.info.shape // [batch_size, sequence_length, 384]
                val seqLen = outputShape[1].toInt()
                val hiddenSize = outputShape[2].toInt() // 384

                // Read all token embeddings
                val allTokenEmbeddings = FloatArray(seqLen * hiddenSize)
                floatBuffer.get(allTokenEmbeddings)

                // Apply mean pooling to get sentence embedding, then L2 normalize
                val pooledEmbedding = meanPooling(allTokenEmbeddings, attentionMask, seqLen, hiddenSize)
                val queryEmbedding = embeddingManager.l2Normalize(pooledEmbedding)

                // Log classification start
                // PII-safe: log utterance length, not content
                android.util.Log.i("IntentClassifier", "=== Classifying: ${utterance.length}-char input ===")
                android.util.Log.d("IntentClassifier", "Candidate intents: ${candidateIntents.joinToString()}")
                android.util.Log.d("IntentClassifier", "Using method: ${if (embeddingManager.hasEmbeddings()) "Semantic Similarity" else "Keyword Matching"}")

                // Calculate cosine similarity with pre-computed intent embeddings
                val scores = if (embeddingManager.hasEmbeddings()) {
                    // Use semantic similarity (2024 best practice)
                    candidateIntents.map { intent ->
                        val intentEmbed = embeddingManager.getEmbedding(intent)
                        if (intentEmbed != null) {
                            cosineSimilarity(queryEmbedding, intentEmbed)
                        } else {
                            // Fallback to improved keyword matching if embedding not available
                            computeKeywordScore(intent, utterance)
                        }
                    }
                } else {
                    // Fallback to improved keyword matching if embeddings failed to load
                    android.util.Log.w("IntentClassifier", "⚠️ Using keyword matching fallback (embeddings not loaded)")
                    candidateIntents.map { intent ->
                        computeKeywordScore(intent, utterance)
                    }
                }

                // Log all scores
                candidateIntents.forEachIndexed { index, intent ->
                    android.util.Log.d("IntentClassifier", "  $intent: ${scores[index]}")
                }

                // Find best matching intent by ranking
                val bestIntentIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
                val confidence = scores[bestIntentIndex]

                android.util.Log.i("IntentClassifier", "Best match: ${candidateIntents[bestIntentIndex]} (confidence: $confidence)")

                // Select intent if confidence is above threshold
                // For cosine similarity: threshold of 0.6 means 60% semantic similarity
                // For keyword matching fallback: threshold of 0.5 means 50% keyword overlap
                val threshold = if (embeddingManager.hasEmbeddings()) NluThresholds.SEMANTIC_CONFIDENCE_THRESHOLD else NluThresholds.KEYWORD_CONFIDENCE_THRESHOLD
                val isAboveThreshold = confidence >= threshold && bestIntentIndex < candidateIntents.size
                val intent = if (isAboveThreshold) {
                    candidateIntents[bestIntentIndex]
                } else {
                    "unknown"
                }

                android.util.Log.i("IntentClassifier", "Threshold: $threshold, Confidence: $confidence")
                android.util.Log.i("IntentClassifier", "FINAL DECISION: $intent")

                // H-03: Enhanced unknown intent handling
                if (intent == "unknown") {
                    android.util.Log.w("IntentClassifier", "⚠️ Unknown intent detected!")
                    android.util.Log.w("IntentClassifier", "  Confidence: $confidence (threshold: $threshold)")
                    android.util.Log.w("IntentClassifier", "  Input: ${utterance.length}-char input")

                    // Log top 3 candidates for debugging
                    val topCandidates = candidateIntents.zip(scores)
                        .sortedByDescending { it.second }
                        .take(3)
                    android.util.Log.w("IntentClassifier", "  Top candidates:")
                    topCandidates.forEachIndexed { index, (candidateIntent, score) ->
                        android.util.Log.w("IntentClassifier", "    ${index + 1}. $candidateIntent (${String.format("%.3f", score)})")
                    }

                    android.util.Log.w("IntentClassifier", "  Reason: ${if (confidence < threshold) "Low confidence" else "No valid intent"}")
                    android.util.Log.w("IntentClassifier", "  Fallback: Will use LLM for flexible handling")
                }

                android.util.Log.i("IntentClassifier", "=== Classification Complete (${inferenceTime}ms) ===")

                Result.Success(
                    IntentClassification(
                        intent = intent,
                        confidence = confidence,
                        inferenceTimeMs = inferenceTime,
                        allScores = candidateIntents.zip(scores).toMap()
                    )
                )
            } finally {
                // Clean up tensors - ALWAYS executes, even on exception
                inputIdsTensor?.close()
                attentionMaskTensor?.close()
                tokenTypeIdsTensor?.close()
                outputs?.close()
            }
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Intent classification failed: ${e.message}"
            )
        }
    }

    /**
     * Mean pooling with attention mask
     * Averages all token embeddings (excluding padding) to create sentence embedding
     *
     * @param allTokenEmbeddings Flattened array of all token embeddings [seqLen * hiddenSize]
     * @param attentionMask Attention mask indicating real tokens (1) vs padding (0)
     * @param seqLen Sequence length
     * @param hiddenSize Hidden dimension size (384 for MobileBERT)
     * @return Sentence embedding [hiddenSize]
     */
    private fun meanPooling(
        allTokenEmbeddings: FloatArray,
        attentionMask: LongArray,
        seqLen: Int,
        hiddenSize: Int
    ): FloatArray {
        val result = FloatArray(hiddenSize) { 0.0f }
        var tokenCount = 0

        // Sum embeddings for non-padding tokens
        for (i in 0 until seqLen) {
            if (attentionMask[i] == 1L) {  // Real token (not padding)
                tokenCount++
                for (j in 0 until hiddenSize) {
                    result[j] += allTokenEmbeddings[i * hiddenSize + j]
                }
            }
        }

        // Average (avoid division by zero)
        if (tokenCount > 0) {
            for (j in 0 until hiddenSize) {
                result[j] /= tokenCount.toFloat()
            }
        }

        return result
    }

    /**
     * L2 normalize a vector (unit vector)
     * Essential for semantic similarity: ensures embeddings have magnitude 1
     *
     * @param vector Vector to normalize
     * @return Normalized vector with L2 norm = 1
     *
     * Delegates to IntentEmbeddingManager for consistent normalization.
     */
    private fun l2Normalize(vector: FloatArray): FloatArray {
        return embeddingManager.l2Normalize(vector)
    }

    /**
     * Cosine similarity between two L2-normalized vectors
     *
     * For pre-normalized vectors (magnitude = 1.0), cosine similarity
     * is simply the dot product: cos(a, b) = a · b
     *
     * IMPORTANT: Both vectors MUST be L2-normalized before calling this function.
     * The intent embeddings are normalized during precomputation (line 702),
     * and query embeddings are normalized after mean pooling (line 289).
     *
     * @param a First vector (MUST be L2-normalized with magnitude = 1.0)
     * @param b Second vector (MUST be L2-normalized with magnitude = 1.0)
     * @return Similarity score in range [-1, 1]: 1 = identical, 0 = orthogonal, -1 = opposite
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vectors must have same dimension (${a.size} != ${b.size})" }

        // For L2-normalized vectors: cos(a, b) = a · b (dot product only)
        // No magnitude computation needed - both vectors already have magnitude 1.0
        var dotProduct = 0.0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }

        return dotProduct
    }

    /**
     * Compute keyword-based similarity score between intent and utterance
     * 
     * Uses exact word matching with Jaccard similarity to avoid false positives
     * from substring matching (e.g., "hello" shouldn't match "control_lights").
     * 
     * @param intent Intent name (e.g., "control_lights", "check_weather")
     * @param utterance User input text
     * @return Similarity score between 0.0 and 1.0
     */
    private fun computeKeywordScore(intent: String, utterance: String): Float {
        // Extract intent keywords (e.g., "control_lights" -> ["control", "lights"])
        val intentKeywords = intent.split("_").map { it.lowercase() }.toSet()
        
        // Extract utterance words (e.g., "turn on the lights" -> ["turn", "on", "the", "lights"])
        val utteranceWords = utterance.lowercase()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .toSet()
        
        // Calculate Jaccard similarity: |intersection| / |union|
        // This avoids false positives from substring matching
        val intersection = intentKeywords.intersect(utteranceWords)
        val union = intentKeywords.union(utteranceWords)
        
        val jaccardScore = if (union.isNotEmpty()) {
            intersection.size.toFloat() / union.size.toFloat()
        } else {
            0.0f
        }
        
        // Bonus: Check for exact keyword matches (full words, not substrings)
        val exactMatches = intentKeywords.count { keyword ->
            utteranceWords.contains(keyword)
        }
        
        // Calculate final score: Jaccard similarity with bonus for exact matches
        // Exact matches get higher weight to prefer "lights" in "turn on lights" 
        // over partial matches
        val exactMatchBonus = (exactMatches.toFloat() / intentKeywords.size.toFloat()) * NluThresholds.KEYWORD_EXACT_MATCH_BONUS_WEIGHT
        val finalScore = (jaccardScore + exactMatchBonus).coerceIn(0.0f, 1.0f)
        
        android.util.Log.d("IntentClassifier", "    Keyword score for '$intent': " +
            "jaccard=$jaccardScore, exact=$exactMatches/${intentKeywords.size}, final=$finalScore")
        
        return finalScore
    }

    /**
     * Compute embedding for a single text using ONNX model
     *
     * @param text Text to embed
     * @return Sentence embedding vector [384]
     */
    /**
     * Compute raw (mean-pooled but NOT normalized) embedding
     * Used during pre-computation to avoid double normalization
     */
    private suspend fun computeRawEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        // Tokenize
        val tokens = tokenizer.tokenize(text)
        val inputIds = tokens.inputIds
        val attentionMask = tokens.attentionMask
        val tokenTypeIds = tokens.tokenTypeIds

        // Get ONNX environment from manager
        val ortEnvironment = onnxSessionManager.getEnvironment()
            ?: throw IllegalStateException("ONNX environment not initialized")

        // Create tensors with resource management
        var inputIdsTensor: OnnxTensor? = null
        var attentionMaskTensor: OnnxTensor? = null
        var tokenTypeIdsTensor: OnnxTensor? = null
        var outputs: ai.onnxruntime.OrtSession.Result? = null

        try {
            inputIdsTensor = OnnxTensor.createTensor(
                ortEnvironment,
                LongBuffer.wrap(inputIds.map { it.toLong() }.toLongArray()),
                longArrayOf(1, inputIds.size.toLong())
            )
            attentionMaskTensor = OnnxTensor.createTensor(
                ortEnvironment,
                LongBuffer.wrap(attentionMask.map { it.toLong() }.toLongArray()),
                longArrayOf(1, attentionMask.size.toLong())
            )
            tokenTypeIdsTensor = OnnxTensor.createTensor(
                ortEnvironment,
                LongBuffer.wrap(tokenTypeIds.map { it.toLong() }.toLongArray()),
                longArrayOf(1, tokenTypeIds.size.toLong())
            )

            // Delegate inference to ONNX session manager
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor,
                "token_type_ids" to tokenTypeIdsTensor
            )
            outputs = onnxSessionManager.run(inputs)

            // Extract embeddings
            val outputTensor = outputs.get(0) as OnnxTensor
            val floatBuffer = outputTensor.floatBuffer
            val outputShape = outputTensor.info.shape // [1, seqLen, 384]
            val seqLen = outputShape[1].toInt()
            val hiddenSize = outputShape[2].toInt()

            // Read all token embeddings
            val allEmbeddings = FloatArray(seqLen * hiddenSize)
            floatBuffer.get(allEmbeddings)

            // Apply mean pooling ONLY (no normalization yet)
            meanPooling(allEmbeddings, attentionMask, seqLen, hiddenSize)
        } finally {
            // Clean up - ALWAYS executes, even on exception
            inputIdsTensor?.close()
            attentionMaskTensor?.close()
            tokenTypeIdsTensor?.close()
            outputs?.close()
        }
    }

    /**
     * Compute normalized embedding for query classification (internal use)
     */
    private suspend fun computeEmbeddingInternal(text: String): FloatArray = withContext(Dispatchers.Default) {
        val rawEmbedding = computeRawEmbedding(text)
        l2Normalize(rawEmbedding)
    }

    /**
     * Load intent embeddings from database
     * Called during initialization
     *
     * AVA 2.0 Architecture:
     * - Bundled intents: Pre-computed embeddings populated via SQLDelight migration
     * - User-added intents: Computed when user teaches AVA, saved to database
     * - All embeddings loaded from intent_embedding table
     *
     * Performance:
     * - Database lookup: ~10ms (no runtime embedding computation!)
     * - First launch: Instant (embeddings already in database from migration)
     *
     * No file-based fallback - database is the single source of truth.
     * If database is empty, uses keyword matching as graceful degradation.
     */
    private suspend fun precomputeIntentEmbeddings() = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("IntentClassifier", "Loading intent embeddings from database...")

            val locale = localeManager.getCurrentLocale()
            val fallbackChain = localeManager.getFallbackChain(locale)

            android.util.Log.d("IntentClassifier", "Loading embeddings for locale chain: $fallbackChain")

            // Open the database, query all needed data, then close the driver immediately.
            val cachedEmbeddings = withDatabase { database ->
                val embeddingQueries = database.intentEmbeddingQueries
                // Try each locale in fallback chain until embeddings found
                fallbackChain.firstNotNullOfOrNull { localeCode ->
                    embeddingQueries.selectByLocale(localeCode).executeAsList()
                        .takeIf { it.isNotEmpty() }
                } ?: emptyList()
            }

            if (cachedEmbeddings.isNotEmpty()) {
                val loadedLocale = cachedEmbeddings.firstOrNull()?.locale ?: locale
                android.util.Log.i("IntentClassifier", "=== AVA 2.0 Semantic NLU ===")
                android.util.Log.i("IntentClassifier", "Loading ${cachedEmbeddings.size} pre-computed embeddings from database")
                android.util.Log.i("IntentClassifier", "Locale: $loadedLocale (from fallback chain: $fallbackChain)")

                // Load embeddings directly - already L2-normalized and ready to use!
                for (embedding in cachedEmbeddings) {
                    try {
                        // Convert BLOB to FloatArray
                        val blob = embedding.embedding_vector
                        val buffer = java.nio.ByteBuffer.wrap(blob).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                        val vector = FloatArray(embedding.embedding_dimension.toInt()) { buffer.float }
                        // Delegate to embedding manager
                        embeddingManager.addEmbedding(embedding.intent_id, vector)
                        android.util.Log.d("IntentClassifier", "  ✓ Loaded embedding for ${embedding.intent_id} (${vector.size}-dim)")
                    } catch (e: Exception) {
                        android.util.Log.w("IntentClassifier", "  ✗ Failed to load embedding for ${embedding.intent_id}: ${e.message}")
                    }
                }

                android.util.Log.i("IntentClassifier", "Fast loading complete: ${embeddingManager.getEmbeddingCount()} intents ready")
                return@withContext
            }

            // Database is empty - this should only happen on corrupted DB or development
            android.util.Log.w("IntentClassifier", "=== No embeddings in database ===")
            android.util.Log.w("IntentClassifier", "Bundled embeddings should be pre-populated via SQLDelight migration.")
            android.util.Log.w("IntentClassifier", "Run: python tools/embedding-generator/generate_embeddings.py")
            android.util.Log.w("IntentClassifier", "Classification will use keyword matching fallback until embeddings are available.")

            // Try to load from .aot backup file if database is corrupted
            loadFromAotBackup()
        } catch (e: Exception) {
            android.util.Log.e("IntentClassifier", "Failed to load embeddings from database: ${e.message}", e)
            android.util.Log.w("IntentClassifier", "Classification will use keyword matching fallback.")
            loadFromAotBackup()
        }
    }

    /**
     * Load embeddings from .aot backup file (for DB corruption recovery)
     *
     * The .aot file is bundled in APK assets and serves as backup
     * if the database becomes corrupted.
     */
    private suspend fun loadFromAotBackup() = withContext(Dispatchers.IO) {
        try {
            val aotPath = "embeddings/bundled_embeddings.aot"
            context.assets.open(aotPath).use { inputStream ->
                val bytes = inputStream.readBytes()
                val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)

                // Read header: magic (4 bytes) + version (4 bytes) + count (4 bytes) + dimension (4 bytes)
                val magic = ByteArray(4)
                buffer.get(magic)
                if (!magic.contentEquals(byteArrayOf('A'.code.toByte(), 'O'.code.toByte(), 'T'.code.toByte(), 0))) {
                    android.util.Log.w("IntentClassifier", "Invalid .aot magic header")
                    return@withContext
                }

                val version = buffer.int
                val count = buffer.int
                val dimension = buffer.int

                // Skip model version (32 bytes)
                buffer.position(buffer.position() + 32)

                android.util.Log.i("IntentClassifier", "Loading from .aot backup: $count embeddings, ${dimension}-dim, v$version")

                // Read embeddings
                repeat(count) {
                    val intentIdLen = buffer.int
                    val intentIdBytes = ByteArray(intentIdLen)
                    buffer.get(intentIdBytes)
                    val intentId = String(intentIdBytes, Charsets.UTF_8)

                    val embedding = FloatArray(dimension) { buffer.float }
                    // Delegate to embedding manager
                    embeddingManager.addEmbedding(intentId, embedding)
                    android.util.Log.d("IntentClassifier", "  ✓ Loaded from .aot: $intentId")
                }

                android.util.Log.i("IntentClassifier", ".aot backup loaded: ${embeddingManager.getEmbeddingCount()} intents ready")
            }
        } catch (e: java.io.FileNotFoundException) {
            android.util.Log.d("IntentClassifier", "No .aot backup file found (expected on first build)")
        } catch (e: Exception) {
            android.util.Log.w("IntentClassifier", "Failed to load .aot backup: ${e.message}")
        }
    }

    /**
     * Compute and save embedding for a newly taught intent
     *
     * Called when user teaches AVA a new intent via Teach AVA UI.
     * Only computes embeddings for the new intent, not all bundled intents.
     *
     * @param intentId The intent ID (e.g., "custom_greeting")
     * @param examples List of example phrases for this intent
     */
    suspend fun computeAndSaveNewIntent(
        intentId: String,
        examples: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext Result.Error(
                exception = AVAException.InitializationException("Classifier not initialized"),
                message = "Intent classifier not initialized. Call initialize() first."
            )
        }

        try {
            android.util.Log.i("IntentClassifier", "Computing embedding for new intent: $intentId (${examples.size} examples)")

            // Compute RAW embeddings for all examples (mean-pooled but NOT normalized)
            val exampleEmbeddings = mutableListOf<FloatArray>()
            for (example in examples) {
                try {
                    val rawEmbedding = computeRawEmbedding(example)
                    exampleEmbeddings.add(rawEmbedding)
                } catch (e: Exception) {
                    android.util.Log.w("IntentClassifier", "Failed to embed '$example': ${e.message}")
                }
            }

            if (exampleEmbeddings.isEmpty()) {
                return@withContext Result.Error(
                    exception = IllegalStateException("No embeddings computed"),
                    message = "Failed to compute embeddings for any example"
                )
            }

            // Average the RAW embeddings, then normalize ONCE
            val hiddenSize = exampleEmbeddings[0].size
            val avgEmbedding = FloatArray(hiddenSize) { 0.0f }

            for (embedding in exampleEmbeddings) {
                for (j in 0 until hiddenSize) {
                    avgEmbedding[j] += embedding[j]
                }
            }

            for (j in 0 until hiddenSize) {
                avgEmbedding[j] /= exampleEmbeddings.size.toFloat()
            }

            // L2 normalize ONCE
            val normalizedAvg = l2Normalize(avgEmbedding)

            // Delegate to embedding manager for storage
            embeddingManager.addEmbedding(intentId, normalizedAvg)

            // Save to database for persistence
            withDatabase { database ->
                saveIntentEmbeddingToDatabase(database, intentId, normalizedAvg, examples.size)
            }

            android.util.Log.i("IntentClassifier", "  ✓ Saved embedding for $intentId (${normalizedAvg.size}-dim)")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to compute/save intent embedding: ${e.message}"
            )
        }
    }

    // ==================== ADR-013: Self-Learning NLU Methods ====================

    /**
     * Compute embedding for text (public API for self-learning).
     * Returns normalized embedding ready for similarity comparison.
     *
     * @param text Text to embed
     * @return Normalized embedding or null if not initialized
     * @see ADR-013: Self-Learning NLU with LLM-as-Teacher Architecture
     */
    suspend fun computeEmbedding(text: String): FloatArray? {
        if (!isInitialized) {
            android.util.Log.w("IntentClassifier", "computeEmbedding called but not initialized")
            return null
        }
        return try {
            val raw = computeRawEmbedding(text)
            l2Normalize(raw)
        } catch (e: Exception) {
            android.util.Log.e("IntentClassifier", "Failed to compute embedding: ${e.message}")
            null
        }
    }

    /**
     * Find existing embedding by utterance text.
     *
     * @param utterance Exact utterance to find
     * @return Embedding bytes or null if not found
     */
    suspend fun findEmbeddingByUtterance(utterance: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            withDatabase { database ->
                database.trainExampleQueries.findByUtteranceWithEmbedding(utterance).executeAsOneOrNull()
            }?.embedding_vector
        } catch (e: Exception) {
            android.util.Log.e("IntentClassifier", "findEmbeddingByUtterance error: ${e.message}")
            null
        }
    }

    /**
     * Save a trained embedding from LLM teaching.
     *
     * @param utterance The utterance text
     * @param intent The intent classification
     * @param embedding The computed embedding vector
     * @param source Learning source (llm_auto, llm_variation, user, etc.)
     * @param confidence Confidence score 0.0-1.0
     * @return True if saved successfully
     */
    suspend fun saveTrainedEmbedding(
        utterance: String,
        intent: String,
        embedding: FloatArray,
        source: String,
        confidence: Float
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val locale = localeManager.getCurrentLocale()
            val currentTime = System.currentTimeMillis()
            val exampleHash = utterance.hashCode().toString()

            // Convert FloatArray to ByteArray
            val buffer = java.nio.ByteBuffer.allocate(embedding.size * 4)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            embedding.forEach { buffer.putFloat(it) }
            val embeddingBytes = buffer.array()

            withDatabase { database ->
                database.trainExampleQueries.insertWithEmbedding(
                    example_hash = exampleHash,
                    utterance = utterance,
                    intent = intent,
                    locale = locale,
                    source = source,
                    created_at = currentTime,
                    confidence = confidence.toDouble(),
                    embedding_vector = embeddingBytes,
                    embedding_dimension = embedding.size.toLong()
                )
            }

            // Delegate to embedding manager for in-memory cache
            embeddingManager.addEmbedding("trained_${exampleHash}", embedding)

            // PII-safe: log utterance length, not content
            android.util.Log.i("IntentClassifier", "Saved trained embedding: ${utterance.length}-char input -> $intent (conf=$confidence)")
            true
        } catch (e: Exception) {
            android.util.Log.e("IntentClassifier", "saveTrainedEmbedding error: ${e.message}")
            false
        }
    }

    /**
     * Confirm a trained embedding (user feedback positive).
     * Increases confidence and marks as user-confirmed.
     *
     * @param utterance The utterance to confirm
     */
    suspend fun confirmTrainedEmbedding(utterance: String) = withContext(Dispatchers.IO) {
        try {
            withDatabase { database ->
                database.trainExampleQueries.confirmUtterance(utterance)
            }
            android.util.Log.i("IntentClassifier", "Confirmed embedding: ${utterance.length}-char input")
        } catch (e: Exception) {
            android.util.Log.e("IntentClassifier", "confirmTrainedEmbedding error: ${e.message}")
        }
    }

    /**
     * Delete a trained embedding.
     *
     * @param utterance The utterance to delete
     */
    suspend fun deleteTrainedEmbedding(utterance: String) = withContext(Dispatchers.IO) {
        try {
            withDatabase { database ->
                database.trainExampleQueries.deleteByUtterance(utterance)
            }

            // Delegate removal to embedding manager
            val hash = utterance.hashCode().toString()
            embeddingManager.removeEmbedding("trained_$hash")

            android.util.Log.i("IntentClassifier", "Deleted embedding: ${utterance.length}-char input")
        } catch (e: Exception) {
            android.util.Log.e("IntentClassifier", "deleteTrainedEmbedding error: ${e.message}")
        }
    }

    /**
     * Learning statistics result.
     *
     * ADR-014: Extended for unified learning to include VoiceOS commands.
     */
    data class LearningStatsResult(
        val total: Int,
        val llmAuto: Int,
        val llmVariation: Int,
        val user: Int,
        val confirmed: Int,
        /** VoiceOS scraped commands synced to AVA */
        val voiceosCommands: Int = 0,
        /** Count of entries with computed embeddings */
        val withEmbedding: Int = 0
    )

    /**
     * Get learning statistics by source.
     *
     * @return LearningStatsResult with counts by source type
     */
    suspend fun getLearningStats(): LearningStatsResult = withContext(Dispatchers.IO) {
        try {
            val stats = withDatabase { database ->
                database.trainExampleQueries.getLearningStats().executeAsOne()
            }
            LearningStatsResult(
                total = (stats.total ?: 0L).toInt(),
                llmAuto = (stats.llm_auto ?: 0L).toInt(),
                llmVariation = (stats.llm_variation ?: 0L).toInt(),
                user = (stats.user_taught ?: 0L).toInt(),
                confirmed = (stats.confirmed ?: 0L).toInt()
            )
        } catch (e: Exception) {
            android.util.Log.e("IntentClassifier", "getLearningStats error: ${e.message}")
            LearningStatsResult(0, 0, 0, 0, 0)
        }
    }

    /**
     * Clear all learned embeddings (non-bundled).
     * Use with caution - deletes user training data.
     */
    suspend fun clearAllTrainedEmbeddings() = withContext(Dispatchers.IO) {
        try {
            withDatabase { database ->
                database.trainExampleQueries.deleteAllLearned()
            }

            // Delegate clearing trained entries to embedding manager
            embeddingManager.getIntentNames()
                .filter { it.startsWith("trained_") }
                .forEach { embeddingManager.removeEmbedding(it) }

            android.util.Log.w("IntentClassifier", "Cleared all learned embeddings")
        } catch (e: Exception) {
            android.util.Log.e("IntentClassifier", "clearAllTrainedEmbeddings error: ${e.message}")
        }
    }

    /**
     * Load trained embeddings from database into memory.
     * Called after precomputeIntentEmbeddings to include user-taught intents.
     *
     * Note: This method is kept for backward compatibility but delegates to
     * IntentEmbeddingManager.loadTrainedEmbeddings() which is called during initialization.
     */
    private suspend fun loadTrainedEmbeddings() = withContext(Dispatchers.IO) {
        try {
            val trainedExamples = withDatabase { database ->
                database.trainExampleQueries.selectAllWithEmbeddings().executeAsList()
            }

            android.util.Log.i("IntentClassifier", "Loading ${trainedExamples.size} trained embeddings")

            for (example in trainedExamples) {
                try {
                    val blob = example.embedding_vector ?: continue
                    val buffer = java.nio.ByteBuffer.wrap(blob).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    val vector = FloatArray(example.embedding_dimension.toInt()) { buffer.float }
                    // Delegate to embedding manager
                    embeddingManager.addEmbedding("trained_${example.example_hash}", vector)
                } catch (e: Exception) {
                    android.util.Log.w("IntentClassifier", "Failed to load trained embedding ${example.id}: ${e.message}")
                }
            }

            android.util.Log.i("IntentClassifier", "Loaded ${trainedExamples.size} trained embeddings")
        } catch (e: Exception) {
            android.util.Log.e("IntentClassifier", "loadTrainedEmbeddings error: ${e.message}")
        }
    }

    // ==================== End ADR-013 Methods ====================

    /**
     * Create a database connection, execute [block], and close the driver in all code paths.
     *
     * Every call to [DatabaseDriverFactory.createDriver] opens a new SQLite connection.
     * The [SqlDriver] is the resource that must be closed; AVADatabase itself holds no
     * additional resources beyond what the driver provides.
     */
    private inline fun <T> withDatabase(block: (database: AVADatabase) -> T): T {
        val driver = DatabaseDriverFactory(context).createDriver()
        return try {
            block(driver.createDatabase())
        } finally {
            driver.close()
        }
    }

    /**
     * Save a single intent embedding to database
     */
    private suspend fun saveIntentEmbeddingToDatabase(
        database: AVADatabase,
        intentId: String,
        embedding: FloatArray,
        exampleCount: Int
    ) = withContext(Dispatchers.IO) {
        val embeddingQueries = database.intentEmbeddingQueries
        val modelManager = ModelManager(context)
        val modelVersion = modelManager.getCurrentModelVersion()
        val locale = localeManager.getCurrentLocale()
        val currentTime = System.currentTimeMillis()

        // Convert FloatArray to ByteArray for database storage
        val buffer = java.nio.ByteBuffer.allocate(embedding.size * 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        embedding.forEach { buffer.putFloat(it) }
        val embeddingBytes = buffer.array()

        embeddingQueries.insert(
            intent_id = intentId,
            locale = locale,
            embedding_vector = embeddingBytes,
            embedding_dimension = modelVersion.dimension.toLong(),
            model_version = modelVersion.version,
            normalization_type = "L2",
            ontology_id = null,
            created_at = currentTime,
            updated_at = currentTime,
            example_count = exampleCount.toLong(),
            source = "user_taught"
        )
    }

    /**
     * Release resources
     * Call this during app shutdown
     */
    actual fun close() {
        if (isInitialized) {
            // Delegate ONNX cleanup to session manager
            onnxSessionManager.close()
            isInitialized = false
        }
    }

    /**
     * Get all intents that have been loaded during initialization
     *
     * Returns the intent IDs for which embeddings have been pre-computed.
     * This includes all intents from .ava files loaded by IntentSourceCoordinator.
     *
     * @return List of loaded intent IDs, or empty list if not initialized
     */
    actual fun getLoadedIntents(): List<String> {
        // Delegate to embedding manager
        return embeddingManager.getIntentNames().toList()
    }

    /**
     * Classify a voice command utterance against known command phrases.
     *
     * VoiceOS Integration:
     * This method wraps classifyIntent() and adds:
     * - Ambiguity detection when multiple commands score similarly
     * - Structured result types for command processing
     * - Support for different matching strategies
     *
     * @param utterance User's spoken command
     * @param commandPhrases List of known command phrases (used as candidate intents)
     * @param confidenceThreshold Minimum confidence for a valid match
     * @param ambiguityThreshold Max difference between top scores to be ambiguous
     * @return CommandClassificationResult indicating match, ambiguity, no match, or error
     */
    actual suspend fun classifyCommand(
        utterance: String,
        commandPhrases: List<String>,
        confidenceThreshold: Float,
        ambiguityThreshold: Float
    ): CommandClassificationResult = withContext(Dispatchers.Default) {
        try {
            // Validate inputs
            if (!isInitialized) {
                return@withContext CommandClassificationResult.Error(
                    "Classifier not initialized. Call initialize() first."
                )
            }

            if (utterance.isBlank()) {
                return@withContext CommandClassificationResult.Error(
                    "Utterance cannot be empty"
                )
            }

            if (commandPhrases.isEmpty()) {
                return@withContext CommandClassificationResult.NoMatch
            }

            // PII-safe: log utterance length, not content
            android.util.Log.d("IntentClassifier", "=== classifyCommand: ${utterance.length}-char input ===")
            android.util.Log.d("IntentClassifier", "Phrases: ${commandPhrases.size}, Threshold: $confidenceThreshold, Ambiguity: $ambiguityThreshold")

            // Step 1: Try exact/fuzzy matching first (fast path)
            val normalizedUtterance = utterance.trim().lowercase()
            for ((index, phrase) in commandPhrases.withIndex()) {
                val normalizedPhrase = phrase.trim().lowercase()
                if (normalizedUtterance == normalizedPhrase) {
                    android.util.Log.i("IntentClassifier", "Exact match: $phrase (index $index)")
                    return@withContext CommandClassificationResult.Match(
                        commandId = phrase,
                        confidence = 1.0f,
                        matchMethod = MatchMethod.EXACT
                    )
                }
            }

            // Step 2: Use semantic classification via classifyIntent()
            val classificationResult = classifyIntent(utterance, commandPhrases)

            when (classificationResult) {
                is Result.Error -> {
                    return@withContext CommandClassificationResult.Error(
                        classificationResult.message ?: "Classification failed"
                    )
                }
                is Result.Success -> {
                    val classification = classificationResult.data

                    // Extract scores and sort by confidence
                    val sortedScores = classification.allScores.entries
                        .sortedByDescending { it.value }
                        .toList()

                    if (sortedScores.isEmpty()) {
                        return@withContext CommandClassificationResult.NoMatch
                    }

                    val topScore = sortedScores[0].value
                    val topCommand = sortedScores[0].key

                    android.util.Log.d("IntentClassifier", "Top: $topCommand ($topScore), Threshold: $confidenceThreshold")

                    // Check if below confidence threshold
                    if (topScore < confidenceThreshold) {
                        android.util.Log.i("IntentClassifier", "NoMatch: top score $topScore < threshold $confidenceThreshold")
                        return@withContext CommandClassificationResult.NoMatch
                    }

                    // Check for ambiguity: multiple candidates within ambiguityThreshold
                    val ambiguousCandidates = sortedScores
                        .filter { it.value >= confidenceThreshold && (topScore - it.value) <= ambiguityThreshold }
                        .map { CommandCandidate(commandId = it.key, confidence = it.value) }

                    if (ambiguousCandidates.size > 1) {
                        android.util.Log.i("IntentClassifier", "Ambiguous: ${ambiguousCandidates.size} candidates within $ambiguityThreshold")
                        return@withContext CommandClassificationResult.Ambiguous(
                            candidates = ambiguousCandidates
                        )
                    }

                    // Clear match - determine match method
                    val matchMethod = when {
                        embeddingManager.hasEmbeddings() -> MatchMethod.SEMANTIC
                        else -> MatchMethod.FUZZY
                    }

                    android.util.Log.i("IntentClassifier", "Match: $topCommand (confidence: $topScore, method: $matchMethod)")
                    return@withContext CommandClassificationResult.Match(
                        commandId = topCommand,
                        confidence = topScore,
                        matchMethod = matchMethod
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("IntentClassifier", "classifyCommand error: ${e.message}", e)
            CommandClassificationResult.Error(
                "Command classification failed: ${e.message}"
            )
        }
    }

    actual companion object {
        @Volatile
        private var INSTANCE: IntentClassifier? = null

        /**
         * Get singleton instance of IntentClassifier.
         *
         * Thread Safety (Issue I-01, C-02):
         * - Uses double-checked locking for thread-safe initialization
         * - Prevents double instantiation even if Hilt also injects
         * - If Hilt provides instance, callers should use DI instead of getInstance()
         *
         * Migration Note:
         * - Prefer @Inject for new code (Hilt manages lifecycle)
         * - getInstance() is kept for KMP compatibility and legacy code
         * - Both paths return the same singleton instance
         *
         * @param context Platform-specific context
         * @return IntentClassifier singleton instance
         */
        actual fun getInstance(context: Any): IntentClassifier {
            require(context is Context) { "Android implementation requires Context" }
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IntentClassifier(context.applicationContext).also {
                    INSTANCE = it
                    android.util.Log.i("IntentClassifier", "Singleton instance created")
                }
            }
        }

        /**
         * Set the singleton instance (for Hilt integration).
         *
         * Call this from Hilt module to ensure getInstance() returns the same
         * instance that Hilt provides, preventing double instantiation.
         *
         * @param instance IntentClassifier instance from Hilt
         */
        @JvmStatic
        fun setInstance(instance: IntentClassifier) {
            synchronized(this) {
                if (INSTANCE != null && INSTANCE !== instance) {
                    android.util.Log.w("IntentClassifier",
                        "Replacing existing instance - this indicates Hilt/Singleton conflict!")
                }
                INSTANCE = instance
                android.util.Log.i("IntentClassifier", "Instance set from Hilt")
            }
        }

        /**
         * Check if instance exists (for testing/debugging).
         */
        @JvmStatic
        fun hasInstance(): Boolean = INSTANCE != null
    }
}

// IntentClassification data class moved to commonMain
