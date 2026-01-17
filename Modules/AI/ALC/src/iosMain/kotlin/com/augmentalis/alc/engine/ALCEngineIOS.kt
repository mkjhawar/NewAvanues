package com.augmentalis.alc.engine

import com.augmentalis.alc.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.Foundation.*
import platform.UIKit.UIDevice
import kotlin.math.exp

/**
 * ALC Engine for iOS - Full Core ML implementation
 *
 * Provides on-device LLM inference using Apple's Neural Engine.
 */
class ALCEngineIOS : IInferenceEngine {

    override val providerType = ProviderType.LOCAL_COREML

    override val capabilities = LLMCapabilities(
        streaming = true,
        functionCalling = false,
        vision = false,
        maxContextLength = 4096,
        supportedLanguages = listOf("en")
    )

    private var coreMLRuntime: CoreMLRuntime? = null
    private var tokenizer: IOSTokenizer? = null
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
            coreMLRuntime = CoreMLRuntime()
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
            // Load Core ML model
            coreMLRuntime?.loadModel("$modelPath/model.mlmodelc")?.getOrThrow()

            // Load tokenizer
            tokenizer = IOSTokenizer("$modelPath/vocab.txt")

            _currentModel = modelPath
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unloadModel() {
        coreMLRuntime?.release()
        tokenizer = null
        _currentModel = null
    }

    override fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        if (!_isInitialized || coreMLRuntime == null || tokenizer == null) {
            emit(LLMResponse.Error("Engine not initialized", "NOT_INITIALIZED"))
            return@flow
        }

        isGenerating = true
        shouldStop = false
        sessionCount++
        val startTime = NSDate.date().timeIntervalSince1970 * 1000

        try {
            val prompt = buildPrompt(messages)
            val inputTokens = tokenizer!!.encode(prompt)
            val inputData = inputTokens.map { it.toFloat() }.toFloatArray()

            val generatedTokens = mutableListOf<Int>()
            val fullText = StringBuilder()

            for (i in 0 until options.maxTokens) {
                if (shouldStop) break

                val result = coreMLRuntime!!.predict(
                    "input_ids",
                    inputData,
                    listOf(1, inputData.size)
                )
                val logits = result.getOrThrow()

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
                val tokenText = tokenizer!!.decode(intArrayOf(nextToken))
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
                is LLMResponse.Complete -> {
                    usage = response.usage
                    latency = response.latencyMs ?: 0
                }
                is LLMResponse.Error -> throw Exception(response.message)
            }
        }

        return LLMResponse.Complete(builder.toString(), usage, _currentModel, latencyMs = latency)
    }

    override suspend fun healthCheck(): ProviderHealth {
        return ProviderHealth(
            provider = providerType,
            status = if (_isInitialized && coreMLRuntime?.isReady() == true)
                HealthStatus.HEALTHY else HealthStatus.UNHEALTHY,
            lastCheck = (NSDate.date().timeIntervalSince1970 * 1000).toLong()
        )
    }

    override suspend fun getModels(): List<ModelInfo> = listOf(
        ModelInfo(
            id = "local-coreml",
            name = "Local Core ML Model",
            provider = ProviderType.LOCAL_COREML,
            contextLength = 4096,
            capabilities = capabilities,
            costPerMillionTokens = 0.0f
        )
    )

    override fun getMemoryInfo(): MemoryInfo {
        val processInfo = NSProcessInfo.processInfo
        val physicalMemory = processInfo.physicalMemory.toLong()
        return MemoryInfo(
            usedBytes = 0, // Would need mach_task_info
            availableBytes = physicalMemory,
            modelSizeBytes = 0,
            kvCacheSizeBytes = 0
        )
    }

    override fun getStats(): EngineStats {
        val avgTps = if (totalInferenceTimeMs > 0)
            (totalTokensGenerated * 1000f) / totalInferenceTimeMs else 0f

        return EngineStats(
            totalTokensGenerated = totalTokensGenerated,
            averageTokensPerSecond = avgTps,
            totalInferenceTimeMs = totalInferenceTimeMs,
            sessionCount = sessionCount
        )
    }

    override suspend fun stop() { shouldStop = true }
    override suspend fun reset() { shouldStop = false; isGenerating = false }

    override suspend fun cleanup() {
        unloadModel()
        coreMLRuntime = null
        _isInitialized = false
    }

    private fun buildPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        for (msg in messages) {
            when (msg.role) {
                MessageRole.SYSTEM -> sb.append("<|system|>\n${msg.content}\n")
                MessageRole.USER -> sb.append("<|user|>\n${msg.content}\n")
                MessageRole.ASSISTANT -> sb.append("<|assistant|>\n${msg.content}\n")
                else -> sb.append("${msg.content}\n")
            }
        }
        sb.append("<|assistant|>\n")
        return sb.toString()
    }

    private fun sampleToken(
        logits: FloatArray,
        temperature: Float,
        topP: Float,
        topK: Int,
        repetitionPenalty: Float,
        previousTokens: List<Int>
    ): Int {
        val penalizedLogits = logits.copyOf()
        for (token in previousTokens.toSet()) {
            if (token < penalizedLogits.size) {
                penalizedLogits[token] /= repetitionPenalty
            }
        }

        val scaledLogits = penalizedLogits.map { it / temperature }.toFloatArray()
        val maxLogit = scaledLogits.maxOrNull() ?: 0f
        val expLogits = scaledLogits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sumExp = expLogits.sum()
        val probs = expLogits.map { it / sumExp }

        val indexed = probs.mapIndexed { i, p -> i to p }.sortedByDescending { it.second }
        val topKTokens = indexed.take(topK)

        var cumProb = 0f
        val nucleusTokens = mutableListOf<Pair<Int, Float>>()
        for ((token, prob) in topKTokens) {
            nucleusTokens.add(token to prob)
            cumProb += prob
            if (cumProb >= topP) break
        }

        val totalProb = nucleusTokens.sumOf { it.second.toDouble() }.toFloat()
        val normalizedProbs = nucleusTokens.map { it.first to (it.second / totalProb) }

        val r = kotlin.random.Random.nextFloat()
        var cumulative = 0f
        for ((token, prob) in normalizedProbs) {
            cumulative += prob
            if (r <= cumulative) return token
        }

        return normalizedProbs.lastOrNull()?.first ?: 0
    }

    private fun isStopToken(token: Int, stopSequences: List<String>): Boolean {
        if (token == 2) return true
        val tokenText = tokenizer?.decode(intArrayOf(token)) ?: return false
        return stopSequences.any { tokenText.contains(it) }
    }
}
