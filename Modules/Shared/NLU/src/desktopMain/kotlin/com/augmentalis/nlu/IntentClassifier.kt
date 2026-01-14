/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.nlu

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.LongBuffer

/**
 * Desktop (JVM) implementation of IntentClassifier using ONNX Runtime
 *
 * Features:
 * - ONNX Runtime JVM for cross-platform inference
 * - MobileBERT model support
 * - Semantic similarity matching with pre-computed embeddings
 * - Keyword matching fallback
 *
 * Performance targets:
 * - Inference: < 100ms (target), < 200ms (max)
 * - Memory: < 500MB for model + runtime
 *
 * Note: This is a simplified desktop implementation without database integration.
 * Pre-computed intent embeddings must be loaded externally.
 */
actual class IntentClassifier private constructor() {

    private lateinit var ortEnvironment: OrtEnvironment
    private lateinit var ortSession: OrtSession
    private lateinit var tokenizer: BertTokenizer
    private var isInitialized = false

    // Mutex to prevent concurrent initialization
    private val initializationMutex = Mutex()

    // Pre-computed intent embeddings for semantic similarity matching
    private val intentEmbeddings = mutableMapOf<String, FloatArray>()

    // StateFlow to indicate when pre-computation is complete
    private val _isPreComputationComplete = MutableStateFlow(false)
    val isPreComputationComplete: StateFlow<Boolean> = _isPreComputationComplete.asStateFlow()

    /**
     * Initialize ONNX Runtime and load model
     * Call this once during app startup
     *
     * @param modelPath Path to ONNX model file (e.g., /path/to/mobilebert_model.onnx)
     * @return Result indicating success or failure
     */
    actual suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.Default) {
        // Use mutex to prevent concurrent initialization
        initializationMutex.withLock {
            try {
                if (isInitialized) {
                    println("[IntentClassifier] Already initialized, skipping")
                    return@withContext Result.Success(Unit)
                }

                println("[IntentClassifier] Starting initialization...")

                // Initialize ONNX Runtime environment
                ortEnvironment = OrtEnvironment.getEnvironment()
                println("[IntentClassifier] ONNX Runtime environment initialized")

                // Load model
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    return@withContext Result.Error(
                        exception = IllegalStateException("Model not found: $modelPath"),
                        message = "Model file does not exist: $modelPath"
                    )
                }

                // Create ONNX session
                val sessionOptions = OrtSession.SessionOptions().apply {
                    setIntraOpNumThreads(4)
                    setInterOpNumThreads(2)
                    // Enable memory optimization for desktop
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL)
                }

                ortSession = ortEnvironment.createSession(
                    modelFile.absolutePath,
                    sessionOptions
                )
                println("[IntentClassifier] ONNX model loaded: ${modelFile.name} (${modelFile.length() / 1024 / 1024} MB)")

                // Initialize tokenizer
                tokenizer = BertTokenizer()

                isInitialized = true
                _isPreComputationComplete.value = true

                println("[IntentClassifier] === Initialization Complete ===")
                Result.Success(Unit)
            } catch (e: Exception) {
                println("[IntentClassifier] Initialization failed: ${e.message}")
                e.printStackTrace()
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
     * Returns RAW (unnormalized, mean-pooled) embedding vector.
     * Used for external embedding precomputation.
     *
     * @param text Input text to embed
     * @return Result containing raw embedding vector or error
     */
    suspend fun computeEmbeddingVector(text: String): Result<FloatArray> {
        if (!isInitialized) {
            return Result.Error(
                exception = IllegalStateException("Classifier not initialized"),
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
     * @return Embedding dimension (384 for MobileBERT)
     */
    fun getEmbeddingDimension(): Int {
        return 384  // MobileBERT default dimension
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

            // Create ONNX tensors with resource management
            var inputIdsTensor: OnnxTensor? = null
            var attentionMaskTensor: OnnxTensor? = null
            var tokenTypeIdsTensor: OnnxTensor? = null
            var outputs: OrtSession.Result? = null

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

                // Run inference
                val startTime = System.currentTimeMillis()
                val inputs = mapOf(
                    "input_ids" to inputIdsTensor,
                    "attention_mask" to attentionMaskTensor,
                    "token_type_ids" to tokenTypeIdsTensor
                )

                outputs = ortSession.run(inputs)
                val inferenceTime = System.currentTimeMillis() - startTime

                // Extract embeddings from model output
                // MobileBERT outputs: [batch_size, sequence_length, hidden_size=384]
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
                val queryEmbedding = l2Normalize(pooledEmbedding)

                println("[IntentClassifier] Classifying: \"$utterance\"")
                println("[IntentClassifier] Candidates: ${candidateIntents.joinToString()}")

                // Calculate similarity scores
                val scores = if (intentEmbeddings.isNotEmpty()) {
                    // Use semantic similarity (pre-computed embeddings)
                    println("[IntentClassifier] Using semantic similarity (${intentEmbeddings.size} embeddings)")
                    candidateIntents.map { intent ->
                        val intentEmbed = intentEmbeddings[intent]
                        if (intentEmbed != null) {
                            cosineSimilarity(queryEmbedding, intentEmbed)
                        } else {
                            computeKeywordScore(intent, utterance)
                        }
                    }
                } else {
                    // Fallback to keyword matching
                    println("[IntentClassifier] Using keyword matching (no pre-computed embeddings)")
                    candidateIntents.map { intent ->
                        computeKeywordScore(intent, utterance)
                    }
                }

                // Log all scores
                candidateIntents.forEachIndexed { index, intent ->
                    println("[IntentClassifier]   $intent: ${String.format("%.3f", scores[index])}")
                }

                // Find best matching intent
                val bestIntentIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
                val confidence = scores[bestIntentIndex]

                // Select intent if confidence is above threshold
                val threshold = if (intentEmbeddings.isNotEmpty()) 0.6f else 0.5f
                val intent = if (confidence >= threshold && bestIntentIndex < candidateIntents.size) {
                    candidateIntents[bestIntentIndex]
                } else {
                    "unknown"
                }

                println("[IntentClassifier] Best: ${candidateIntents[bestIntentIndex]} (confidence: $confidence)")
                println("[IntentClassifier] Threshold: $threshold, Decision: $intent (${inferenceTime}ms)")

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
            println("[IntentClassifier] Classification failed: ${e.message}")
            e.printStackTrace()
            Result.Error(
                exception = e,
                message = "Intent classification failed: ${e.message}"
            )
        }
    }

    /**
     * Mean pooling with attention mask
     * Averages all token embeddings (excluding padding) to create sentence embedding
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
     * Cosine similarity between two L2-normalized vectors
     * For normalized vectors: cos(a, b) = a · b (dot product only)
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vectors must have same dimension" }

        var dotProduct = 0.0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }

        return dotProduct
    }

    /**
     * Keyword-based similarity score using Jaccard similarity
     */
    private fun computeKeywordScore(intent: String, utterance: String): Float {
        val intentKeywords = intent.split("_").map { it.lowercase() }.toSet()
        val utteranceWords = utterance.lowercase()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .toSet()

        val intersection = intentKeywords.intersect(utteranceWords)
        val union = intentKeywords.union(utteranceWords)

        val jaccardScore = if (union.isNotEmpty()) {
            intersection.size.toFloat() / union.size.toFloat()
        } else {
            0.0f
        }

        val exactMatches = intentKeywords.count { keyword ->
            utteranceWords.contains(keyword)
        }

        val exactMatchBonus = (exactMatches.toFloat() / intentKeywords.size.toFloat()) * 0.3f
        return (jaccardScore + exactMatchBonus).coerceIn(0.0f, 1.0f)
    }

    /**
     * Compute raw (mean-pooled but NOT normalized) embedding
     */
    private suspend fun computeRawEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        // Tokenize
        val tokens = tokenizer.tokenize(text)
        val inputIds = tokens.inputIds
        val attentionMask = tokens.attentionMask
        val tokenTypeIds = tokens.tokenTypeIds

        // Create tensors with resource management
        var inputIdsTensor: OnnxTensor? = null
        var attentionMaskTensor: OnnxTensor? = null
        var tokenTypeIdsTensor: OnnxTensor? = null
        var outputs: OrtSession.Result? = null

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

            // Run inference
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor,
                "token_type_ids" to tokenTypeIdsTensor
            )
            outputs = ortSession.run(inputs)

            // Extract embeddings
            val outputTensor = outputs.get(0) as OnnxTensor
            val floatBuffer = outputTensor.floatBuffer
            val outputShape = outputTensor.info.shape // [1, seqLen, 384]
            val seqLen = outputShape[1].toInt()
            val hiddenSize = outputShape[2].toInt()

            // Read all token embeddings
            val allEmbeddings = FloatArray(seqLen * hiddenSize)
            floatBuffer.get(allEmbeddings)

            // Apply mean pooling ONLY (no normalization)
            meanPooling(allEmbeddings, attentionMask, seqLen, hiddenSize)
        } finally {
            // Clean up - ALWAYS executes
            inputIdsTensor?.close()
            attentionMaskTensor?.close()
            tokenTypeIdsTensor?.close()
            outputs?.close()
        }
    }

    /**
     * Add a pre-computed embedding for an intent
     * Call this to load external pre-computed embeddings
     *
     * @param intent Intent ID (e.g., "control_lights")
     * @param embedding L2-normalized embedding vector
     */
    fun addIntentEmbedding(intent: String, embedding: FloatArray) {
        require(embedding.size == 384) { "Desktop NLU expects 384-dim embeddings (MobileBERT), got ${embedding.size}" }
        intentEmbeddings[intent] = embedding
        println("[IntentClassifier] Added embedding for intent: $intent (${embedding.size}-dim)")
    }

    /**
     * Load embeddings from a batch of intent texts
     * Useful for pre-computing embeddings during initialization
     *
     * @param intents Map of intent ID to example text
     * @return Number of embeddings successfully computed
     */
    suspend fun precomputeEmbeddings(intents: Map<String, String>): Int {
        var count = 0
        for ((intentId, exampleText) in intents) {
            try {
                val embedding = computeRawEmbedding(exampleText)
                val normalized = l2Normalize(embedding)
                intentEmbeddings[intentId] = normalized
                count++
                println("[IntentClassifier] Pre-computed embedding for: $intentId")
            } catch (e: Exception) {
                System.err.println("[IntentClassifier] Failed to compute embedding for $intentId: ${e.message}")
            }
        }
        return count
    }

    /**
     * Release resources
     * Call this during app shutdown
     */
    actual fun close() {
        if (isInitialized) {
            ortSession.close()
            ortEnvironment.close()
            isInitialized = false
            println("[IntentClassifier] Resources released")
        }
    }

    /**
     * Get all intents that have embeddings
     */
    actual fun getLoadedIntents(): List<String> {
        return intentEmbeddings.keys.toList()
    }

    actual companion object {
        @Volatile
        private var INSTANCE: IntentClassifier? = null

        /**
         * Get singleton instance of IntentClassifier
         * @param context Not used on Desktop (for KMP compatibility)
         */
        actual fun getInstance(context: Any): IntentClassifier {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IntentClassifier().also {
                    INSTANCE = it
                    println("[IntentClassifier] Singleton instance created")
                }
            }
        }
    }
}
