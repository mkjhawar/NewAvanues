package com.augmentalis.alc.engine

import com.augmentalis.alc.domain.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.Foundation.*
import kotlin.math.exp

/**
 * ALC Engine for macOS Native
 *
 * Provides on-device LLM inference using Apple's Core ML framework.
 * Supports Neural Engine, GPU, and CPU execution on macOS.
 *
 * Note: Full CoreML model inference requires proper cinterop bindings.
 * This implementation provides vocabulary loading and tokenization,
 * with stubbed inference for testing until cinterop is configured.
 */
@OptIn(ExperimentalForeignApi::class)
class ALCEngineMacOS : IInferenceEngine {

    override val providerType = ProviderType.LOCAL_COREML

    override val capabilities = LLMCapabilities(
        streaming = true,
        functionCalling = false,
        vision = false,
        maxContextLength = 4096,
        supportedLanguages = listOf("en")
    )

    private var vocab: Map<String, Int> = emptyMap()
    private var reverseVocab: Map<Int, String> = emptyMap()
    private var merges: List<Pair<String, String>> = emptyList()
    private var _isInitialized = false
    private var _currentModel: String? = null
    private var isGenerating = false
    private var shouldStop = false

    private var totalTokensGenerated = 0L
    private var totalInferenceTimeMs = 0L
    private var sessionCount = 0

    override val isInitialized: Boolean get() = _isInitialized
    override val currentModel: String? get() = _currentModel

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            _isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadModel(modelPath: String): Result<Unit> = withContext(Dispatchers.Default) {
        if (!_isInitialized) {
            return@withContext Result.failure(Exception("Engine not initialized"))
        }

        try {
            // Load vocabulary first
            loadVocabulary("$modelPath/vocab.txt")
            loadMerges("$modelPath/merges.txt")

            // Note: CoreML model loading stubbed until cinterop is configured
            _currentModel = modelPath
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadVocabulary(path: String) {
        val content = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
            ?: return

        val mutableVocab = mutableMapOf<String, Int>()
        val mutableReverse = mutableMapOf<Int, String>()

        content.toString().split("\n").forEachIndexed { index, line ->
            val token = line.trim()
            if (token.isNotEmpty()) {
                mutableVocab[token] = index
                mutableReverse[index] = token
            }
        }

        vocab = mutableVocab
        reverseVocab = mutableReverse
    }

    private fun loadMerges(path: String) {
        val content = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
            ?: return

        merges = content.toString().split("\n").drop(1).mapNotNull { line ->
            val parts = line.split(" ")
            if (parts.size == 2) parts[0] to parts[1] else null
        }
    }

    override suspend fun unloadModel() {
        vocab = emptyMap()
        reverseVocab = emptyMap()
        merges = emptyList()
        _currentModel = null
    }

    override fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        if (!_isInitialized) {
            emit(LLMResponse.Error("Engine not initialized", "NOT_INITIALIZED"))
            return@flow
        }

        if (_currentModel == null) {
            emit(LLMResponse.Error("Model not loaded", "MODEL_NOT_LOADED"))
            return@flow
        }

        isGenerating = true
        shouldStop = false
        sessionCount++
        val startTime = NSDate.date().timeIntervalSince1970 * 1000

        try {
            val prompt = buildPrompt(messages)
            val inputTokens = encode(prompt)

            val generatedTokens = mutableListOf<Int>()
            val fullText = StringBuilder()
            var contextTokens = inputTokens.toMutableList()

            for (i in 0 until options.maxTokens) {
                if (shouldStop) break

                // Run inference to get next token logits
                val logits = runInference(contextTokens)
                if (logits.isEmpty()) {
                    emit(LLMResponse.Error("Inference returned empty logits", "INFERENCE_ERROR"))
                    return@flow
                }

                // Sample next token
                val nextToken = sampleToken(
                    logits,
                    options.temperature,
                    options.topP,
                    options.topK,
                    options.repetitionPenalty,
                    generatedTokens
                )

                if (isStopToken(nextToken, options.stopSequences)) break

                generatedTokens.add(nextToken)
                contextTokens.add(nextToken)

                // Keep context within limits
                if (contextTokens.size > capabilities.maxContextLength - 100) {
                    contextTokens = contextTokens.takeLast(capabilities.maxContextLength - 200).toMutableList()
                }

                val tokenText = decode(listOf(nextToken))
                fullText.append(tokenText)

                if (options.stream) {
                    emit(LLMResponse.Streaming(tokenText, generatedTokens.size))
                }

                totalTokensGenerated++
            }

            val endTime = NSDate.date().timeIntervalSince1970 * 1000
            totalInferenceTimeMs += (endTime - startTime).toLong()

            emit(LLMResponse.Complete(
                fullText = fullText.toString(),
                usage = TokenUsage(inputTokens.size, generatedTokens.size),
                model = _currentModel,
                latencyMs = (endTime - startTime).toLong()
            ))

        } catch (e: Exception) {
            emit(LLMResponse.Error(e.message ?: "Generation failed", "GENERATION_ERROR", true))
        } finally {
            isGenerating = false
        }
    }

    /**
     * Run inference through CoreML
     * Note: Returns uniform distribution for testing until CoreML cinterop is configured
     */
    private fun runInference(tokens: List<Int>): FloatArray {
        // Return uniform distribution for testing
        val vocabSize = vocab.size.coerceAtLeast(1)
        return FloatArray(vocabSize) { 1.0f / vocabSize }
    }

    private fun sampleToken(
        logits: FloatArray,
        temperature: Float,
        topP: Float,
        topK: Int,
        repetitionPenalty: Float,
        previousTokens: List<Int>
    ): Int {
        if (logits.isEmpty()) return 0

        // Apply repetition penalty
        val penalizedLogits = logits.copyOf()
        previousTokens.toSet().forEach { token ->
            if (token < penalizedLogits.size) {
                penalizedLogits[token] = penalizedLogits[token] / repetitionPenalty
            }
        }

        // Apply temperature
        val scaledLogits = penalizedLogits.map { it / temperature.coerceAtLeast(0.01f) }

        // Softmax
        val maxLogit = scaledLogits.maxOrNull() ?: 0f
        val expLogits = scaledLogits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sumExp = expLogits.sum()
        val probs = expLogits.map { it / sumExp }

        // Top-K filtering
        val indexed = probs.mapIndexed { i, p -> i to p }
            .sortedByDescending { it.second }
            .take(topK.coerceAtLeast(1))

        // Top-P (nucleus) sampling
        var cumulative = 0f
        val nucleus = mutableListOf<Pair<Int, Float>>()
        for ((token, prob) in indexed) {
            nucleus.add(token to prob)
            cumulative += prob
            if (cumulative >= topP) break
        }

        // Normalize nucleus probabilities
        val nucleusSum = nucleus.sumOf { it.second.toDouble() }.toFloat()
        val normalizedNucleus = nucleus.map { it.first to (it.second / nucleusSum) }

        // Random sampling
        val random = kotlin.random.Random.nextFloat()
        var cumulativeProb = 0f
        for ((token, prob) in normalizedNucleus) {
            cumulativeProb += prob
            if (random <= cumulativeProb) return token
        }

        return normalizedNucleus.lastOrNull()?.first ?: 0
    }

    private fun encode(text: String): List<Int> {
        val tokens = mutableListOf<Int>()
        val words = text.lowercase().split(Regex("\\s+"))

        for (word in words) {
            // Try SentencePiece style (with leading space marker)
            val wordWithSpace = "▁$word"
            when {
                vocab.containsKey(wordWithSpace) -> {
                    tokens.add(vocab[wordWithSpace]!!)
                }
                vocab.containsKey(word) -> {
                    tokens.add(vocab[word]!!)
                }
                else -> {
                    // BPE fallback - try subword tokenization
                    val subTokens = bpeEncode(word)
                    tokens.addAll(subTokens)
                }
            }
        }

        return tokens
    }

    private fun bpeEncode(word: String): List<Int> {
        if (word.isEmpty()) return emptyList()

        // Start with character-level tokens
        var tokens = word.map { it.toString() }.toMutableList()

        // Apply BPE merges
        for ((first, second) in merges) {
            var i = 0
            while (i < tokens.size - 1) {
                if (tokens[i] == first && tokens[i + 1] == second) {
                    tokens[i] = first + second
                    tokens.removeAt(i + 1)
                } else {
                    i++
                }
            }
        }

        // Convert to IDs
        return tokens.map { token ->
            vocab[token] ?: vocab["<unk>"] ?: 0
        }
    }

    private fun decode(tokens: List<Int>): String {
        return tokens.mapNotNull { reverseVocab[it] }
            .joinToString("")
            .replace("▁", " ")
            .replace("<s>", "")
            .replace("</s>", "")
            .replace("<pad>", "")
            .trim()
    }

    override suspend fun complete(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): LLMResponse.Complete {
        val builder = StringBuilder()
        var usage = TokenUsage(0, 0)
        var latency = 0L

        chat(messages, options.copy(stream = false)).collect { response ->
            when (response) {
                is LLMResponse.Streaming -> builder.append(response.chunk)
                is LLMResponse.Complete -> { usage = response.usage; latency = response.latencyMs ?: 0 }
                is LLMResponse.Error -> throw Exception(response.message)
            }
        }

        return LLMResponse.Complete(builder.toString(), usage, _currentModel, latencyMs = latency)
    }

    override suspend fun healthCheck(): ProviderHealth {
        return ProviderHealth(
            provider = providerType,
            status = when {
                !_isInitialized -> HealthStatus.UNHEALTHY
                _currentModel == null -> HealthStatus.DEGRADED
                else -> HealthStatus.HEALTHY
            },
            lastCheck = (NSDate.date().timeIntervalSince1970 * 1000).toLong(),
            errorMessage = when {
                !_isInitialized -> "Engine not initialized"
                _currentModel == null -> "No model loaded"
                else -> null
            }
        )
    }

    override suspend fun getModels(): List<ModelInfo> = listOf(
        ModelInfo(
            id = "local-macos-coreml",
            name = "Local macOS Core ML Model",
            provider = ProviderType.LOCAL_COREML,
            contextLength = capabilities.maxContextLength,
            capabilities = capabilities,
            costPerMillionTokens = 0.0f
        )
    )

    override fun getMemoryInfo(): MemoryInfo {
        val processInfo = NSProcessInfo.processInfo
        val totalMemory = processInfo.physicalMemory.toLong()

        // Estimate model size from vocab
        val estimatedModelSize = vocab.size * 4L * 1000 // rough estimate

        return MemoryInfo(
            usedBytes = estimatedModelSize,
            availableBytes = totalMemory,
            modelSizeBytes = estimatedModelSize,
            kvCacheSizeBytes = 0
        )
    }

    override fun getStats(): EngineStats {
        val avgTps = if (totalInferenceTimeMs > 0) {
            (totalTokensGenerated * 1000f) / totalInferenceTimeMs
        } else {
            0f
        }
        return EngineStats(totalTokensGenerated, avgTps, totalInferenceTimeMs, sessionCount)
    }

    override suspend fun stop() { shouldStop = true }
    override suspend fun reset() { shouldStop = false; isGenerating = false }
    override suspend fun cleanup() { unloadModel(); _isInitialized = false }

    private fun buildPrompt(messages: List<ChatMessage>): String {
        return messages.joinToString("\n") { msg ->
            when (msg.role) {
                MessageRole.SYSTEM -> "<|system|>\n${msg.content}"
                MessageRole.USER -> "<|user|>\n${msg.content}"
                MessageRole.ASSISTANT -> "<|assistant|>\n${msg.content}"
                else -> msg.content
            }
        } + "\n<|assistant|>\n"
    }

    private fun isStopToken(token: Int, stopSequences: List<String>): Boolean {
        // Check for EOS token
        if (token == vocab["</s>"] || token == vocab["<eos>"] || token == 2) return true

        val text = decode(listOf(token))
        return stopSequences.any { text.contains(it) }
    }

    /**
     * Get model information if loaded
     */
    fun getModelInfo(): Map<String, Any>? {
        if (_currentModel == null) return null

        return mapOf(
            "inputs" to listOf("input_ids"),
            "outputs" to listOf("logits"),
            "metadata" to mapOf("vocab_size" to vocab.size)
        )
    }
}
