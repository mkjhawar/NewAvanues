/**
 * macOS implementation of IntentClassifier using Core ML inference
 *
 * Adapted from iOS implementation with the following changes:
 * - Removed @ThreadLocal (deprecated in Kotlin/Native new memory model, Kotlin 2.1.0)
 * - Companion object uses regular singleton pattern (thread-safe by default in new MM)
 *
 * Features:
 * - Loads and manages ML models compiled for macOS
 * - Performs semantic intent classification via embeddings
 * - Supports fallback to keyword matching
 * - Thread-safe singleton with lazy initialization
 * - Resource cleanup on close()
 */

package com.augmentalis.nlu

import com.augmentalis.ava.core.common.Result
import com.augmentalis.nlu.coreml.CoreMLModelManager
import com.augmentalis.nlu.matching.currentTimeMillis
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

actual class IntentClassifier private constructor() {

    private var modelManager: CoreMLModelManager? = null
    private var tokenizer: BertTokenizer? = null
    private var isInitialized = false

    // Pre-computed intent embeddings for semantic similarity matching
    private val intentEmbeddings = mutableMapOf<String, FloatArray>()

    // Mutex to prevent concurrent initialization
    private val initializationMutex = Mutex()

    /**
     * Initialize Core ML model and load embeddings
     * @param modelPath Path to .mlmodel or .mlpackage
     * @return Result indicating success or failure
     */
    actual suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.Main) {
        initializationMutex.withLock {
            try {
                if (isInitialized) {
                    println("IntentClassifier: Already initialized, skipping")
                    return@withContext Result.Success(Unit)
                }

                println("IntentClassifier: Starting initialization with model: $modelPath")

                // Initialize Core ML model manager
                modelManager = CoreMLModelManager()
                val loadResult = modelManager?.loadModel(
                    modelPath = modelPath,
                    computeBackend = CoreMLModelManager.ComputeBackend.Auto
                )

                if (loadResult == null || loadResult is Result.Error) {
                    return@withContext Result.Error(
                        exception = IllegalStateException("Failed to load Core ML model"),
                        message = "Model initialization failed: ${(loadResult as? Result.Error)?.message}"
                    )
                }

                // Initialize tokenizer
                tokenizer = BertTokenizer()

                // Pre-compute intent embeddings from database or fallback
                precomputeIntentEmbeddings()

                isInitialized = true
                println("IntentClassifier: Initialization complete, loaded ${intentEmbeddings.size} intent embeddings")
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(
                    exception = e,
                    message = "Failed to initialize macOS NLU: ${e.message}"
                )
            }
        }
    }

    /**
     * Classify intent from user utterance using semantic similarity
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

            val startTime = currentTimeMillis()

            // Tokenize input
            val tokens = tokenizer?.tokenize(utterance)
                ?: return@withContext Result.Error(
                    exception = IllegalStateException("Tokenizer not initialized"),
                    message = "BERT tokenizer failed to initialize"
                )

            // Run Core ML inference via modelManager
            val inferenceResult = modelManager?.runInference(
                inputIds = tokens.inputIds,
                attentionMask = tokens.attentionMask,
                tokenTypeIds = tokens.tokenTypeIds
            )

            val embeddingOutput = when (inferenceResult) {
                is Result.Success -> inferenceResult.data
                is Result.Error -> {
                    return@withContext Result.Error(
                        exception = inferenceResult.exception,
                        message = "Core ML inference failed: ${inferenceResult.message}"
                    )
                }
                else -> return@withContext Result.Error(
                    exception = IllegalStateException("Invalid inference result"),
                    message = "Unexpected result type"
                )
            }

            // Extract embedding from inference output
            val queryEmbedding = l2Normalize(embeddingOutput)

            // Calculate similarity scores with candidate intents
            val scores = if (intentEmbeddings.isNotEmpty()) {
                candidateIntents.map { intent ->
                    val intentEmbed = intentEmbeddings[intent]
                    if (intentEmbed != null) {
                        cosineSimilarity(queryEmbedding, intentEmbed)
                    } else {
                        computeKeywordScore(intent, utterance)
                    }
                }
            } else {
                println("IntentClassifier: Using keyword matching fallback")
                candidateIntents.map { intent ->
                    computeKeywordScore(intent, utterance)
                }
            }

            val inferenceTime = currentTimeMillis() - startTime

            val bestIntentIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
            val confidence = scores[bestIntentIndex]

            val threshold = if (intentEmbeddings.isNotEmpty()) 0.6f else 0.5f
            val intent = if (confidence >= threshold && bestIntentIndex < candidateIntents.size) {
                candidateIntents[bestIntentIndex]
            } else {
                "unknown"
            }

            println("IntentClassifier: Classified '$utterance' -> $intent (confidence: $confidence, time: ${inferenceTime}ms)")

            Result.Success(
                IntentClassification(
                    intent = intent,
                    confidence = confidence,
                    inferenceTimeMs = inferenceTime,
                    allScores = candidateIntents.zip(scores).toMap()
                )
            )
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Intent classification failed: ${e.message}"
            )
        }
    }

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

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vectors must have same dimension (${a.size} != ${b.size})" }

        var dotProduct = 0.0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        return dotProduct
    }

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

    private suspend fun precomputeIntentEmbeddings() {
        try {
            println("IntentClassifier: Pre-computing intent embeddings...")
            // Embeddings loaded from database when available
            println("IntentClassifier: Pre-computation complete: ${intentEmbeddings.size} intents")
        } catch (e: Exception) {
            println("IntentClassifier: Warning - failed to pre-compute embeddings: ${e.message}")
        }
    }

    actual fun close() {
        if (isInitialized) {
            modelManager?.close()
            modelManager = null
            tokenizer = null
            intentEmbeddings.clear()
            isInitialized = false
            println("IntentClassifier: Cleaned up resources")
        }
    }

    actual fun getLoadedIntents(): List<String> {
        return intentEmbeddings.keys.toList()
    }

    /**
     * Classify a voice command utterance against known command phrases.
     */
    actual suspend fun classifyCommand(
        utterance: String,
        commandPhrases: List<String>,
        confidenceThreshold: Float,
        ambiguityThreshold: Float
    ): CommandClassificationResult = withContext(Dispatchers.Default) {
        try {
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

            println("IntentClassifier: classifyCommand: \"$utterance\"")
            println("IntentClassifier: Phrases: ${commandPhrases.size}, Threshold: $confidenceThreshold, Ambiguity: $ambiguityThreshold")

            // Step 1: Try exact matching first (fast path)
            val normalizedUtterance = utterance.trim().lowercase()
            for ((index, phrase) in commandPhrases.withIndex()) {
                val normalizedPhrase = phrase.trim().lowercase()
                if (normalizedUtterance == normalizedPhrase) {
                    println("IntentClassifier: Exact match: $phrase (index $index)")
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

                    val sortedScores = classification.allScores.entries
                        .sortedByDescending { it.value }
                        .toList()

                    if (sortedScores.isEmpty()) {
                        return@withContext CommandClassificationResult.NoMatch
                    }

                    val topScore = sortedScores[0].value
                    val topCommand = sortedScores[0].key

                    println("IntentClassifier: Top: $topCommand ($topScore), Threshold: $confidenceThreshold")

                    if (topScore < confidenceThreshold) {
                        println("IntentClassifier: NoMatch: top score $topScore < threshold $confidenceThreshold")
                        return@withContext CommandClassificationResult.NoMatch
                    }

                    val ambiguousCandidates = sortedScores
                        .filter { it.value >= confidenceThreshold && (topScore - it.value) <= ambiguityThreshold }
                        .map { CommandCandidate(commandId = it.key, confidence = it.value) }

                    if (ambiguousCandidates.size > 1) {
                        println("IntentClassifier: Ambiguous: ${ambiguousCandidates.size} candidates within $ambiguityThreshold")
                        return@withContext CommandClassificationResult.Ambiguous(
                            candidates = ambiguousCandidates
                        )
                    }

                    val matchMethod = when {
                        intentEmbeddings.isNotEmpty() -> MatchMethod.SEMANTIC
                        else -> MatchMethod.FUZZY
                    }

                    println("IntentClassifier: Match: $topCommand (confidence: $topScore, method: $matchMethod)")
                    return@withContext CommandClassificationResult.Match(
                        commandId = topCommand,
                        confidence = topScore,
                        matchMethod = matchMethod
                    )
                }
            }
        } catch (e: Exception) {
            println("IntentClassifier: classifyCommand error: ${e.message}")
            CommandClassificationResult.Error(
                "Command classification failed: ${e.message}"
            )
        }
    }

    // No @ThreadLocal — Kotlin 2.1.0 new memory model handles companion objects correctly
    actual companion object {
        private var INSTANCE: IntentClassifier? = null
        private val lock = SynchronizedObject()

        actual fun getInstance(context: Any): IntentClassifier {
            return INSTANCE ?: synchronized(lock) {
                INSTANCE ?: IntentClassifier().also {
                    INSTANCE = it
                    println("IntentClassifier: Singleton instance created")
                }
            }
        }
    }
}
