package com.augmentalis.nlu

import com.augmentalis.ava.core.common.Result
import com.augmentalis.nlu.locale.LocaleManager
import kotlinx.coroutines.await
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int32Array
import org.khronos.webgl.get
import kotlin.js.Promise
import kotlin.math.sqrt

/**
 * JS/Web implementation of IntentClassifier using ONNX Runtime Web
 *
 * Runs BERT inference in the browser via ONNX Runtime Web WASM backend.
 * Dynamically adapts to the active model's embedding dimension:
 * - MobileBERT: 384-dim (English, ~30ms inference)
 * - mALBERT: 768-dim (52 languages, ~60ms inference)
 *
 * Architecture:
 * 1. Input text → BertTokenizer → token IDs
 * 2. Token IDs → ONNX Runtime Web (WASM) → hidden states
 * 3. Hidden states → mean pooling → L2 normalize → sentence embedding
 * 4. Sentence embedding × intent embeddings → cosine similarity → best match
 *
 * Fallback: Keyword matching (Jaccard similarity) when model is unavailable
 *
 * Note: Uses int32 tensors to avoid BigInt interop complexity
 */
actual class IntentClassifier private constructor() {

    private var ortSession: dynamic = null
    private var tokenizer: BertTokenizer? = null
    private var modelManager: ModelManager? = null
    private val localeManager = LocaleManager()
    private var isInitialized = false

    // Pre-computed intent embeddings: intentId → L2-normalized embedding
    private val intentEmbeddings = mutableMapOf<String, FloatArray>()

    // Locale-aware embeddings: locale → (intentId → embedding)
    private val localeEmbeddings = mutableMapOf<String, Map<String, FloatArray>>()

    /**
     * Initialize ONNX Runtime Web session and load model
     *
     * @param modelPath IndexedDB key or URL of the ONNX model
     * @return Result indicating success or failure
     */
    actual suspend fun initialize(modelPath: String): Result<Unit> {
        return try {
            if (isInitialized) {
                console.log("[IntentClassifier] Already initialized, skipping")
                return Result.Success(Unit)
            }

            console.log("[IntentClassifier] Starting initialization...")

            // Initialize model manager and ensure model is downloaded
            val mgr = ModelManager()
            modelManager = mgr

            val downloadResult = mgr.downloadModelsIfNeeded { progress ->
                console.log("[IntentClassifier] Download progress: ${(progress * 100).toInt()}%")
            }

            if (downloadResult is Result.Error) {
                console.warn("[IntentClassifier] Model download failed, will use keyword fallback")
                // Don't fail initialization — keyword matching still works
            }

            // Initialize tokenizer
            val tok = BertTokenizer()
            val vocabText = mgr.getVocabText()
            if (vocabText != null) {
                tok.loadVocabFromText(vocabText)
                console.log("[IntentClassifier] Tokenizer loaded with ${tok.getVocabSize()} tokens")
            } else {
                console.warn("[IntentClassifier] No vocabulary available, tokenizer using stub vocab")
            }
            tokenizer = tok

            // Create ONNX session if model buffer is available
            val modelBuffer = mgr.getModelArrayBuffer()
            if (modelBuffer != null) {
                ortSession = createOnnxSession(modelBuffer)
                console.log("[IntentClassifier] ONNX session created (${mgr.getActiveModelType().displayName})")
            } else {
                console.warn("[IntentClassifier] No model buffer, semantic classification unavailable")
            }

            isInitialized = true
            console.log("[IntentClassifier] === Initialization Complete ===")
            Result.Success(Unit)
        } catch (e: Exception) {
            console.error("[IntentClassifier] Initialization failed: ${e.message}")
            // Mark as initialized anyway — keyword fallback is available
            isInitialized = true
            Result.Success(Unit)
        }
    }

    /**
     * Classify user utterance into one of the candidate intents
     *
     * Uses semantic similarity when ONNX model is loaded, falls back to keyword matching.
     *
     * @param utterance User input text
     * @param candidateIntents List of possible intents
     * @return Result with classification and confidence
     */
    actual suspend fun classifyIntent(
        utterance: String,
        candidateIntents: List<String>
    ): Result<IntentClassification> {
        return try {
            if (utterance.isBlank()) {
                return Result.Error(
                    exception = IllegalArgumentException("Empty utterance"),
                    message = "Utterance cannot be empty"
                )
            }

            if (candidateIntents.isEmpty()) {
                return Result.Error(
                    exception = IllegalArgumentException("No candidate intents"),
                    message = "At least one intent required"
                )
            }

            val startTime = kotlin.js.Date().getTime().toLong()

            // Try semantic classification first (ONNX model)
            val scores = if (ortSession != null && tokenizer?.isVocabLoaded() == true) {
                classifyWithModel(utterance, candidateIntents)
            } else {
                // Fallback to keyword matching
                candidateIntents.map { intent ->
                    computeKeywordScore(intent, utterance)
                }
            }

            val inferenceTime = kotlin.js.Date().getTime().toLong() - startTime

            // Find best matching intent
            val bestIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
            val confidence = scores[bestIndex]

            val threshold = if (ortSession != null) 0.6f else 0.5f
            val intent = if (confidence >= threshold && bestIndex < candidateIntents.size) {
                candidateIntents[bestIndex]
            } else {
                "unknown"
            }

            Result.Success(
                IntentClassification(
                    intent = intent,
                    confidence = confidence,
                    inferenceTimeMs = inferenceTime,
                    allScores = candidateIntents.zip(scores).toMap()
                )
            )
        } catch (e: Exception) {
            console.error("[IntentClassifier] Classification failed: ${e.message}")
            Result.Error(
                exception = e,
                message = "Intent classification failed: ${e.message}"
            )
        }
    }

    /**
     * Clean up ONNX session resources
     */
    actual fun close() {
        if (ortSession != null) {
            try {
                ortSession.release()
            } catch (_: Exception) {
                // Best-effort cleanup
            }
            ortSession = null
        }
        isInitialized = false
        intentEmbeddings.clear()
        localeEmbeddings.clear()
        console.log("[IntentClassifier] Resources released")
    }

    /**
     * Get all intents that have pre-computed embeddings
     */
    actual fun getLoadedIntents(): List<String> {
        return intentEmbeddings.keys.toList()
    }

    /**
     * Classify a voice command utterance against known command phrases
     *
     * Steps:
     * 1. Exact matching (fast path)
     * 2. Semantic classification via ONNX or keyword fallback
     * 3. Ambiguity detection
     * 4. NoMatch if below threshold
     */
    actual suspend fun classifyCommand(
        utterance: String,
        commandPhrases: List<String>,
        confidenceThreshold: Float,
        ambiguityThreshold: Float
    ): CommandClassificationResult {
        return try {
            if (utterance.isBlank()) {
                return CommandClassificationResult.Error("Utterance cannot be empty")
            }

            if (commandPhrases.isEmpty()) {
                return CommandClassificationResult.NoMatch
            }

            // Step 1: Exact matching (fast path)
            val normalizedUtterance = utterance.trim().lowercase()
            for (phrase in commandPhrases) {
                if (normalizedUtterance == phrase.trim().lowercase()) {
                    return CommandClassificationResult.Match(
                        commandId = phrase,
                        confidence = 1.0f,
                        matchMethod = MatchMethod.EXACT
                    )
                }
            }

            // Step 2: Semantic/keyword classification
            val classificationResult = classifyIntent(utterance, commandPhrases)

            when (classificationResult) {
                is Result.Error -> {
                    CommandClassificationResult.Error(
                        classificationResult.message ?: "Classification failed"
                    )
                }
                is Result.Success -> {
                    val classification = classificationResult.data
                    val sortedScores = classification.allScores.entries
                        .sortedByDescending { it.value }
                        .toList()

                    if (sortedScores.isEmpty()) {
                        return CommandClassificationResult.NoMatch
                    }

                    val topScore = sortedScores[0].value
                    val topCommand = sortedScores[0].key

                    // Below threshold → no match
                    if (topScore < confidenceThreshold) {
                        return CommandClassificationResult.NoMatch
                    }

                    // Check for ambiguity
                    val ambiguousCandidates = sortedScores
                        .filter { it.value >= confidenceThreshold && (topScore - it.value) <= ambiguityThreshold }
                        .map { CommandCandidate(commandId = it.key, confidence = it.value) }

                    if (ambiguousCandidates.size > 1) {
                        return CommandClassificationResult.Ambiguous(candidates = ambiguousCandidates)
                    }

                    // Clear match
                    val matchMethod = if (ortSession != null) MatchMethod.SEMANTIC else MatchMethod.FUZZY
                    CommandClassificationResult.Match(
                        commandId = topCommand,
                        confidence = topScore,
                        matchMethod = matchMethod
                    )
                }
            }
        } catch (e: Exception) {
            console.error("[IntentClassifier] classifyCommand error: ${e.message}")
            CommandClassificationResult.Error("Command classification failed: ${e.message}")
        }
    }

    actual companion object {
        private var INSTANCE: IntentClassifier? = null

        actual fun getInstance(context: Any): IntentClassifier {
            return INSTANCE ?: IntentClassifier().also {
                INSTANCE = it
            }
        }
    }

    // ─── ONNX Inference ─────────────────────────────────────────

    /**
     * Run ONNX model inference for semantic classification
     *
     * Returns similarity scores for each candidate intent.
     * Uses pre-computed intent embeddings if available, otherwise
     * computes embeddings on-the-fly for candidates too.
     */
    private suspend fun classifyWithModel(
        utterance: String,
        candidateIntents: List<String>
    ): List<Float> {
        // Compute utterance embedding
        val queryEmbedding = computeNormalizedEmbedding(utterance)
            ?: return candidateIntents.map { computeKeywordScore(it, utterance) }

        // Calculate similarity for each candidate
        return candidateIntents.map { intent ->
            // Try locale-aware embeddings first
            val locale = localeManager.getCurrentLocale()
            val fallbackChain = localeManager.getFallbackChain(locale)

            var embedding: FloatArray? = null
            for (loc in fallbackChain) {
                embedding = localeEmbeddings[loc]?.get(intent)
                if (embedding != null) break
            }

            // Try global embeddings
            if (embedding == null) {
                embedding = intentEmbeddings[intent]
            }

            if (embedding != null) {
                cosineSimilarity(queryEmbedding, embedding)
            } else {
                // No pre-computed embedding — use keyword score as fallback
                computeKeywordScore(intent, utterance)
            }
        }
    }

    /**
     * Compute L2-normalized sentence embedding for text
     *
     * Pipeline: tokenize → ONNX inference → mean pool → L2 normalize
     */
    private suspend fun computeNormalizedEmbedding(text: String): FloatArray? {
        val tok = tokenizer ?: return null
        if (ortSession == null) return null

        val tokens = tok.tokenize(text)
        val inputIds = tokens.inputIds
        val attentionMask = tokens.attentionMask
        val tokenTypeIds = tokens.tokenTypeIds

        // Create int32 typed arrays (not int64 — avoids BigInt interop)
        val seqLen = inputIds.size
        val inputIdsArray = Int32Array(seqLen)
        val attentionMaskArray = Int32Array(seqLen)
        val tokenTypeIdsArray = Int32Array(seqLen)

        for (i in 0 until seqLen) {
            inputIdsArray.asDynamic()[i] = inputIds[i].toInt()
            attentionMaskArray.asDynamic()[i] = attentionMask[i].toInt()
            tokenTypeIdsArray.asDynamic()[i] = tokenTypeIds[i].toInt()
        }

        return try {
            // Create ONNX tensors via JS dynamic interop
            // onnxruntime-web is loaded as an npm module by the Kotlin/JS bundler
            val ort: dynamic = js("require('onnxruntime-web')")
            val shape = arrayOf(1, seqLen)

            val inputIdsTensor = ort.Tensor("int32", inputIdsArray, shape)
            val attentionMaskTensor = ort.Tensor("int32", attentionMaskArray, shape)
            val tokenTypeIdsTensor = ort.Tensor("int32", tokenTypeIdsArray, shape)

            val feeds: dynamic = js("({})")
            feeds["input_ids"] = inputIdsTensor
            feeds["attention_mask"] = attentionMaskTensor
            feeds["token_type_ids"] = tokenTypeIdsTensor

            // Run inference
            val results: dynamic = (ortSession.run(feeds) as Promise<dynamic>).await()

            // Extract output tensor — shape: [1, seqLen, hiddenSize]
            val outputNames: dynamic = js("Object.keys")(results)
            val firstOutputName = outputNames[0] as String
            val outputTensor: dynamic = results[firstOutputName]
            val outputData = outputTensor.data.unsafeCast<Float32Array>()
            val outputDims: dynamic = outputTensor.dims

            val outputSeqLen = (outputDims[1] as Number).toInt()
            val hiddenSize = (outputDims[2] as Number).toInt()

            // Read all token embeddings into a flat array
            val allTokenEmbeddings = FloatArray(outputSeqLen * hiddenSize)
            for (i in allTokenEmbeddings.indices) {
                allTokenEmbeddings[i] = outputData[i]
            }

            // Mean pooling with attention mask
            val pooled = meanPooling(allTokenEmbeddings, attentionMask, outputSeqLen, hiddenSize)

            // L2 normalize
            l2Normalize(pooled)
        } catch (e: Exception) {
            console.error("[IntentClassifier] ONNX inference failed: ${e.message}")
            null
        }
    }

    /**
     * Create an ONNX Runtime Web session from an ArrayBuffer
     */
    private suspend fun createOnnxSession(modelBuffer: dynamic): dynamic {
        val ort: dynamic = js("require('onnxruntime-web')")
        val sessionOptions: dynamic = js("({})")
        sessionOptions["executionProviders"] = arrayOf("wasm")

        // Convert ArrayBuffer to Uint8Array for ONNX Runtime
        val uint8Array: dynamic = js("new Uint8Array")(modelBuffer)
        return (ort.InferenceSession.create(uint8Array, sessionOptions) as Promise<dynamic>).await()
    }

    // ─── Math Functions ─────────────────────────────────────────

    /**
     * Mean pooling with attention mask
     *
     * Averages all non-padding token embeddings to create a sentence embedding.
     * Dimension-agnostic: works with any hiddenSize (384, 768, etc.)
     */
    private fun meanPooling(
        allTokenEmbeddings: FloatArray,
        attentionMask: LongArray,
        seqLen: Int,
        hiddenSize: Int
    ): FloatArray {
        val result = FloatArray(hiddenSize) { 0.0f }
        var tokenCount = 0

        for (i in 0 until seqLen) {
            if (attentionMask[i] == 1L) {
                tokenCount++
                for (j in 0 until hiddenSize) {
                    result[j] += allTokenEmbeddings[i * hiddenSize + j]
                }
            }
        }

        if (tokenCount > 0) {
            for (j in 0 until hiddenSize) {
                result[j] /= tokenCount.toFloat()
            }
        }

        return result
    }

    /**
     * L2 normalize a vector to unit length
     */
    private fun l2Normalize(vector: FloatArray): FloatArray {
        var magnitude = 0.0f
        for (value in vector) {
            magnitude += value * value
        }
        magnitude = sqrt(magnitude)

        return if (magnitude > 0) {
            FloatArray(vector.size) { i -> vector[i] / magnitude }
        } else {
            vector
        }
    }

    /**
     * Cosine similarity between two L2-normalized vectors
     * For normalized vectors: cos(a, b) = dot(a, b)
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0.0f

        var dotProduct = 0.0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        return dotProduct
    }

    // ─── Keyword Fallback ───────────────────────────────────────

    /**
     * Keyword-based similarity using Jaccard index + exact match bonus
     *
     * This serves as a reliable fallback when:
     * - ONNX model is not loaded (download failed, first load)
     * - No pre-computed embeddings for the intent
     * - Inference timeout
     *
     * Uses locale-aware synonym expansion for non-English locales.
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

        // Bonus for exact keyword matches
        val exactMatches = intentKeywords.count { keyword -> utteranceWords.contains(keyword) }
        val exactMatchBonus = if (intentKeywords.isNotEmpty()) {
            (exactMatches.toFloat() / intentKeywords.size.toFloat()) * 0.3f
        } else {
            0.0f
        }

        return (jaccardScore + exactMatchBonus).coerceIn(0.0f, 1.0f)
    }

    // ─── Embedding Management ───────────────────────────────────

    /**
     * Add a pre-computed embedding for an intent (global, not locale-specific)
     */
    fun addIntentEmbedding(intent: String, embedding: FloatArray) {
        intentEmbeddings[intent] = embedding
    }

    /**
     * Add locale-specific intent embeddings
     *
     * @param locale BCP-47 locale code (e.g., "es-ES")
     * @param embeddings Map of intentId → L2-normalized embedding
     */
    fun addLocaleEmbeddings(locale: String, embeddings: Map<String, FloatArray>) {
        localeEmbeddings[locale] = embeddings
    }

    /**
     * Get the active model's embedding dimension (or 0 if not loaded)
     */
    fun getEmbeddingDimension(): Int {
        return modelManager?.getEmbeddingDimension() ?: 0
    }

    /**
     * Pre-compute embeddings for a batch of intents
     *
     * @param intents Map of intentId to example utterance text
     * @return Number of embeddings successfully computed
     */
    suspend fun precomputeEmbeddings(intents: Map<String, String>): Int {
        var count = 0
        for ((intentId, exampleText) in intents) {
            val embedding = computeNormalizedEmbedding(exampleText)
            if (embedding != null) {
                intentEmbeddings[intentId] = embedding
                count++
            }
        }
        return count
    }
}
