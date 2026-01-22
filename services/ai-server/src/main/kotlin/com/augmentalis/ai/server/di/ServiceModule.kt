package com.augmentalis.ai.server.di

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.augmentalis.ai.server.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.LongBuffer

/**
 * Service module providing NLU and Embedding capabilities
 */
class ServiceModule private constructor() {

    private val startTime = System.currentTimeMillis()
    private val ortEnvironment = OrtEnvironment.getEnvironment()

    // Model paths
    private val modelDir = File(System.getProperty("user.home"), ".augmentalis/models")
    private val mobileBertPath = File(modelDir, "mobilebert/model.onnx")
    private val miniLmPath = File(modelDir, "minilm/model.onnx")
    private val vocabPath = File(modelDir, "mobilebert/vocab.txt")

    // Lazy-loaded sessions
    private var mobileBertSession: OrtSession? = null
    private var miniLmSession: OrtSession? = null
    private var vocabulary: Map<String, Int>? = null

    // Category keywords for rule-based fallback
    private val categoryKeywords = mapOf(
        InstructionCategory.ARCHITECTURE to listOf("stack", "pattern", "structure", "framework", "library", "module", "layer", "di", "dependency"),
        InstructionCategory.CONVENTIONS to listOf("naming", "format", "style", "lint", "case", "prefix", "suffix", "indent", "convention"),
        InstructionCategory.WORKFLOWS to listOf("process", "ci", "cd", "branch", "merge", "deploy", "release", "review", "commit"),
        InstructionCategory.DOMAIN to listOf("business", "logic", "term", "entity", "model", "rule", "validation", "domain"),
        InstructionCategory.SECURITY to listOf("auth", "permission", "secret", "token", "encrypt", "password", "key", "security"),
        InstructionCategory.TESTING to listOf("test", "mock", "stub", "coverage", "unit", "integration", "e2e", "spec")
    )

    // Tech entity patterns
    private val techPatterns = listOf(
        "kotlin", "swift", "typescript", "javascript", "python", "java",
        "react", "compose", "swiftui", "flutter",
        "hilt", "koin", "dagger",
        "sqldelight", "room", "coredata",
        "ktor", "express", "fastapi",
        "junit", "mockk", "jest", "xctest"
    )

    companion object {
        fun create(): ServiceModule = ServiceModule()
    }

    /**
     * Initialize models (lazy loading)
     */
    private fun ensureModelsLoaded() {
        if (mobileBertSession == null && mobileBertPath.exists()) {
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            mobileBertSession = ortEnvironment.createSession(mobileBertPath.absolutePath, options)
            println("[ServiceModule] Loaded MobileBERT from ${mobileBertPath.absolutePath}")
        }

        if (miniLmSession == null && miniLmPath.exists()) {
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            miniLmSession = ortEnvironment.createSession(miniLmPath.absolutePath, options)
            println("[ServiceModule] Loaded MiniLM from ${miniLmPath.absolutePath}")
        }

        if (vocabulary == null && vocabPath.exists()) {
            vocabulary = vocabPath.readLines()
                .mapIndexed { index, token -> token to index }
                .toMap()
            println("[ServiceModule] Loaded vocabulary: ${vocabulary?.size} tokens")
        }
    }

    /**
     * Classify instruction into category
     */
    suspend fun classify(request: ClassifyRequest): ClassifyResponse = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        ensureModelsLoaded()

        val text = request.text.lowercase()

        // Score each category
        val scores = mutableMapOf<String, Float>()
        for ((category, keywords) in categoryKeywords) {
            var score = 0f
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    score += 1f
                }
            }
            scores[category.name] = score / keywords.size
        }

        // Find best category
        val bestCategory = scores.maxByOrNull { it.value }?.key
            ?: InstructionCategory.CONVENTIONS.name
        val confidence = scores[bestCategory] ?: 0f

        // Detect rule type
        val ruleType = detectRuleType(text)

        // Extract entities
        val entities = extractTechEntities(text)

        ClassifyResponse(
            category = InstructionCategory.valueOf(bestCategory),
            confidence = (confidence * 100).coerceIn(0f, 100f) / 100f,
            ruleType = ruleType,
            entities = entities,
            allScores = scores,
            inferenceTimeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Detect rule type from text
     */
    private fun detectRuleType(text: String): RuleType {
        return when {
            text.contains("never") || text.contains("don't") || text.contains("avoid") -> RuleType.PROHIBITION
            text.contains("always") || text.contains("prefer") || text.contains("should") -> RuleType.PREFERENCE
            text.contains(" or ") || text.contains("/") -> RuleType.ALTERNATIVE
            text.contains("must") || text.contains("required") -> RuleType.REQUIREMENT
            text.contains("->") || text.contains("maps to") || text.contains("should be") -> RuleType.MAPPING
            else -> RuleType.PREFERENCE
        }
    }

    /**
     * Extract tech entities from text
     */
    private fun extractTechEntities(text: String): List<String> {
        val found = mutableListOf<String>()
        val lower = text.lowercase()

        for (tech in techPatterns) {
            if (lower.contains(tech)) {
                // Capitalize properly
                found.add(tech.replaceFirstChar { it.uppercase() })
            }
        }

        return found.distinct()
    }

    /**
     * Compute embedding for text
     */
    suspend fun computeEmbedding(request: EmbeddingRequest): EmbeddingResponse = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        ensureModelsLoaded()

        val session = miniLmSession
        if (session == null) {
            // Return placeholder if model not loaded
            return@withContext EmbeddingResponse(
                embedding = List(384) { 0f },
                dimension = 384,
                model = "placeholder",
                inferenceTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // Tokenize (simplified - real impl would use proper tokenizer)
        val tokens = tokenize(request.text)

        // Create tensors
        val inputIds = OnnxTensor.createTensor(
            ortEnvironment,
            LongBuffer.wrap(tokens.inputIds.map { it.toLong() }.toLongArray()),
            longArrayOf(1, tokens.inputIds.size.toLong())
        )
        val attentionMask = OnnxTensor.createTensor(
            ortEnvironment,
            LongBuffer.wrap(tokens.attentionMask.map { it.toLong() }.toLongArray()),
            longArrayOf(1, tokens.attentionMask.size.toLong())
        )

        try {
            val inputs = mapOf(
                "input_ids" to inputIds,
                "attention_mask" to attentionMask
            )

            val outputs = session.run(inputs)
            val outputTensor = outputs.get(0) as OnnxTensor
            val floatBuffer = outputTensor.floatBuffer

            // Mean pooling
            val shape = outputTensor.info.shape
            val seqLen = shape[1].toInt()
            val hiddenSize = shape[2].toInt()

            val allEmbeddings = FloatArray(seqLen * hiddenSize)
            floatBuffer.get(allEmbeddings)

            val embedding = meanPool(allEmbeddings, tokens.attentionMask, seqLen, hiddenSize)
            val finalEmbedding = if (request.normalize) l2Normalize(embedding) else embedding

            outputs.close()

            EmbeddingResponse(
                embedding = finalEmbedding.toList(),
                dimension = hiddenSize,
                model = "minilm",
                inferenceTimeMs = System.currentTimeMillis() - startTime
            )
        } finally {
            inputIds.close()
            attentionMask.close()
        }
    }

    /**
     * Compute similarity between two texts
     */
    suspend fun computeSimilarity(request: SimilarityRequest): SimilarityResponse = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        val emb1 = computeEmbedding(EmbeddingRequest(request.text1, normalize = true))
        val emb2 = computeEmbedding(EmbeddingRequest(request.text2, normalize = true))

        val similarity = cosineSimilarity(
            emb1.embedding.toFloatArray(),
            emb2.embedding.toFloatArray()
        )

        SimilarityResponse(
            similarity = similarity,
            inferenceTimeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Convert instruction to compact format
     */
    suspend fun convert(request: ConvertRequest): ConvertResponse = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // Classify first
        val classification = classify(ClassifyRequest(request.input))

        // Use provided category or detected
        val category = request.category ?: classification.category

        // Build compact format
        val compact = buildCompactFormat(
            request.input,
            classification.ruleType,
            classification.entities
        )

        ConvertResponse(
            original = request.input,
            compact = compact,
            category = category,
            ruleType = classification.ruleType,
            entities = classification.entities,
            confidence = classification.confidence,
            inferenceTimeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Build compact format from instruction
     */
    private fun buildCompactFormat(input: String, ruleType: RuleType, entities: List<String>): String {
        val subject = detectSubject(input, entities)

        return when (ruleType) {
            RuleType.PROHIBITION -> {
                val prohibited = extractProhibited(input)
                val preferred = extractPreferred(input)
                if (preferred != null) {
                    "$subject: $preferred (NEVER $prohibited)"
                } else {
                    "$subject: (NEVER $prohibited)"
                }
            }
            RuleType.PREFERENCE -> {
                val preferred = extractPreferred(input) ?: entities.firstOrNull() ?: "specified"
                "$subject: $preferred"
            }
            RuleType.ALTERNATIVE -> {
                val options = extractAlternatives(input)
                "$subject: ${options.joinToString("|")}"
            }
            RuleType.MAPPING -> {
                val (from, to) = extractMapping(input)
                "$from -> $to"
            }
            RuleType.REQUIREMENT -> {
                val required = extractRequired(input)
                "$subject: $required (required)"
            }
        }
    }

    private fun detectSubject(input: String, entities: List<String>): String {
        val lower = input.lowercase()
        return when {
            lower.contains("di") || lower.contains("dependency injection") -> "DI"
            lower.contains("database") || lower.contains("db") -> "DB"
            lower.contains("auth") -> "Auth"
            lower.contains("test") -> "Test"
            lower.contains("ui") -> "UI"
            lower.contains("api") -> "API"
            lower.contains("naming") -> "Naming"
            lower.contains("style") || lower.contains("format") -> "Style"
            entities.isNotEmpty() -> entities.first()
            else -> "Rule"
        }
    }

    private fun extractProhibited(input: String): String {
        val match = Regex("(?:never|don't|avoid)\\s+(?:use|using)?\\s*(.+?)(?:[,.]|\$)", RegexOption.IGNORE_CASE)
            .find(input)
        return match?.groupValues?.get(1)?.trim() ?: "specified"
    }

    private fun extractPreferred(input: String): String? {
        val match = Regex("(?:use|using|prefer)\\s+(.+?)(?:\\s+(?:instead|rather)|[,.]|\$)", RegexOption.IGNORE_CASE)
            .find(input)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun extractAlternatives(input: String): List<String> {
        val match = Regex("(?:use|using)\\s+(.+?)\\s+(?:or|/)\\s+(.+?)(?:[,.]|\$)", RegexOption.IGNORE_CASE)
            .find(input)
        return if (match != null) {
            listOf(match.groupValues[1].trim(), match.groupValues[2].trim())
        } else {
            listOf("option1", "option2")
        }
    }

    private fun extractMapping(input: String): Pair<String, String> {
        val match = Regex("(.+?)\\s+(?:should be|maps? to|->|→)\\s+(.+?)(?:[,.]|\$)", RegexOption.IGNORE_CASE)
            .find(input)
        return if (match != null) {
            match.groupValues[1].trim() to match.groupValues[2].trim()
        } else {
            "from" to "to"
        }
    }

    private fun extractRequired(input: String): String {
        val match = Regex("(?:must|required)\\s+(?:have|use|be)?\\s*(.+?)(?:[,.]|\$)", RegexOption.IGNORE_CASE)
            .find(input)
        return match?.groupValues?.get(1)?.trim() ?: "specified"
    }

    /**
     * Get health status
     */
    fun getHealth(): HealthResponse {
        ensureModelsLoaded()

        return HealthResponse(
            status = "healthy",
            version = "1.0.0",
            models = mapOf(
                "mobilebert" to ModelStatus(
                    loaded = mobileBertSession != null,
                    path = if (mobileBertPath.exists()) mobileBertPath.absolutePath else null,
                    sizeBytes = if (mobileBertPath.exists()) mobileBertPath.length() else null,
                    lastUsed = null
                ),
                "minilm" to ModelStatus(
                    loaded = miniLmSession != null,
                    path = if (miniLmPath.exists()) miniLmPath.absolutePath else null,
                    sizeBytes = if (miniLmPath.exists()) miniLmPath.length() else null,
                    lastUsed = null
                )
            ),
            uptime = System.currentTimeMillis() - startTime
        )
    }

    // Tokenization helpers (simplified)
    private data class TokenResult(val inputIds: List<Int>, val attentionMask: List<Int>)

    private fun tokenize(text: String, maxLength: Int = 128): TokenResult {
        val vocab = vocabulary
        if (vocab == null) {
            // Fallback: simple word-based tokenization
            val words = text.lowercase().split(Regex("\\s+"))
            val ids = words.take(maxLength - 2).map { it.hashCode() and 0xFFFF }
            val inputIds = listOf(101) + ids + listOf(102) // [CLS] ... [SEP]
            val padded = inputIds + List(maxLength - inputIds.size) { 0 }
            val mask = List(inputIds.size) { 1 } + List(maxLength - inputIds.size) { 0 }
            return TokenResult(padded.take(maxLength), mask.take(maxLength))
        }

        // WordPiece tokenization
        val tokens = mutableListOf<Int>()
        tokens.add(vocab["[CLS]"] ?: 101)

        for (word in text.lowercase().split(Regex("\\s+"))) {
            val wordId = vocab[word]
            if (wordId != null) {
                tokens.add(wordId)
            } else {
                // Try subword tokenization
                var remaining = word
                while (remaining.isNotEmpty() && tokens.size < maxLength - 1) {
                    var found = false
                    for (end in remaining.length downTo 1) {
                        val subword = if (tokens.size > 1) "##${remaining.substring(0, end)}" else remaining.substring(0, end)
                        val subId = vocab[subword]
                        if (subId != null) {
                            tokens.add(subId)
                            remaining = remaining.substring(end)
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        tokens.add(vocab["[UNK]"] ?: 100)
                        break
                    }
                }
            }
            if (tokens.size >= maxLength - 1) break
        }

        tokens.add(vocab["[SEP]"] ?: 102)

        val padded = tokens + List(maxLength - tokens.size) { vocab["[PAD]"] ?: 0 }
        val mask = List(tokens.size) { 1 } + List(maxLength - tokens.size) { 0 }

        return TokenResult(padded.take(maxLength), mask.take(maxLength))
    }

    // Math helpers
    private fun meanPool(embeddings: FloatArray, mask: List<Int>, seqLen: Int, hiddenSize: Int): FloatArray {
        val result = FloatArray(hiddenSize)
        var count = 0

        for (i in 0 until seqLen) {
            if (mask.getOrElse(i) { 0 } == 1) {
                count++
                for (j in 0 until hiddenSize) {
                    result[j] += embeddings[i * hiddenSize + j]
                }
            }
        }

        if (count > 0) {
            for (j in 0 until hiddenSize) {
                result[j] /= count.toFloat()
            }
        }

        return result
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var magnitude = 0f
        for (v in vector) magnitude += v * v
        magnitude = kotlin.math.sqrt(magnitude)

        return if (magnitude > 0) {
            FloatArray(vector.size) { vector[it] / magnitude }
        } else {
            vector
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot
    }
}

private fun List<Float>.toFloatArray(): FloatArray = FloatArray(size) { this[it] }
