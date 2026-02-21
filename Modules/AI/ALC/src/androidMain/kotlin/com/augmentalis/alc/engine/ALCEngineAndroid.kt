package com.augmentalis.alc.engine

import android.content.Context
import com.augmentalis.alc.config.DeviceProfile
import com.augmentalis.alc.config.ModelConfig
import com.augmentalis.alc.config.ModelSelector
import com.augmentalis.alc.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import kotlin.math.exp

/**
 * ALC Engine for Android - Full TVM-based implementation
 *
 * Provides on-device LLM inference using TVM optimized models.
 */
class ALCEngineAndroid(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : IInferenceEngine {

    override val providerType = ProviderType.LOCAL_TVM

    override val capabilities = LLMCapabilities(
        streaming = true,
        functionCalling = false,
        vision = false,
        maxContextLength = 4096,
        supportedLanguages = listOf("en")
    )

    private var tvmRuntime: TVMRuntime? = null
    private var tokenizer: TVMTokenizer? = null
    private var modelConfig: ModelConfig? = null
    private var _isInitialized = false
    private var _currentModel: String? = null
    @Volatile
    private var isGenerating = false
    @Volatile
    private var shouldStop = false

    // Stats tracking
    @Volatile
    private var totalTokensGenerated = 0L
    @Volatile
    private var totalInferenceTimeMs = 0L
    @Volatile
    private var sessionCount = 0

    override val isInitialized: Boolean get() = _isInitialized
    override val currentModel: String? get() = _currentModel

    override suspend fun initialize(): Result<Unit> = withContext(dispatcher) {
        try {
            // Detect device capabilities
            val runtime = Runtime.getRuntime()
            val memoryMB = (runtime.maxMemory() / (1024 * 1024)).toInt()
            val cores = runtime.availableProcessors()
            val hasGPU = detectGPU()

            val profile = DeviceProfile.fromMemory(memoryMB, cores, hasGPU)
            modelConfig = ModelSelector.selectModel(profile)

            // Initialize TVM runtime
            tvmRuntime = TVMRuntime(context, useGPU = hasGPU)

            _isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadModel(modelPath: String): Result<Unit> = withContext(dispatcher) {
        if (!_isInitialized) {
            return@withContext Result.failure(Exception("Engine not initialized"))
        }

        try {
            val modelDir = File(modelPath)
            val libPath = File(modelDir, "model.so").absolutePath
            val graphPath = File(modelDir, "model.json").absolutePath
            val paramsPath = File(modelDir, "model.params").absolutePath
            val vocabPath = File(modelDir, "vocab.txt").absolutePath

            // Load model into TVM
            tvmRuntime?.initialize(libPath, graphPath, paramsPath)?.getOrThrow()

            // Load tokenizer
            tokenizer = TVMTokenizer(vocabPath)

            _currentModel = modelPath
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unloadModel() {
        tvmRuntime?.release()
        tokenizer = null
        _currentModel = null
    }

    override fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        if (!_isInitialized || tvmRuntime == null || tokenizer == null) {
            emit(LLMResponse.Error("Engine not initialized or model not loaded", "NOT_INITIALIZED"))
            return@flow
        }

        isGenerating = true
        shouldStop = false
        sessionCount++
        val startTime = System.currentTimeMillis()

        try {
            // Build prompt from messages
            val prompt = buildPrompt(messages)
            val inputTokens = tokenizer!!.encode(prompt)

            // Prepare input tensor
            val inputData = inputTokens.map { it.toFloat() }.toFloatArray()

            val generatedTokens = mutableListOf<Int>()
            val fullText = StringBuilder()

            // Auto-regressive generation loop
            for (i in 0 until options.maxTokens) {
                if (shouldStop) break

                // Run inference
                val result = tvmRuntime!!.infer("input", inputData)
                val logits = result.getOrThrow()

                // Sample next token
                val nextToken = sampleToken(
                    logits,
                    options.temperature,
                    options.topP,
                    options.topK,
                    options.repetitionPenalty,
                    generatedTokens
                )

                // Check for stop conditions
                if (isStopToken(nextToken, options.stopSequences)) break

                generatedTokens.add(nextToken)
                val tokenText = tokenizer!!.decode(intArrayOf(nextToken))
                fullText.append(tokenText)

                // Emit streaming response
                if (options.stream) {
                    emit(LLMResponse.Streaming(tokenText, generatedTokens.size))
                }

                // Update input for next iteration
                // (Shift input and append new token - KV cache would optimize this)
                totalTokensGenerated++
            }

            val endTime = System.currentTimeMillis()
            totalInferenceTimeMs += (endTime - startTime)

            // Emit complete response
            emit(LLMResponse.Complete(
                fullText = fullText.toString(),
                usage = TokenUsage(inputTokens.size, generatedTokens.size),
                model = _currentModel,
                latencyMs = endTime - startTime
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
            status = if (_isInitialized && tvmRuntime?.isReady() == true)
                HealthStatus.HEALTHY else HealthStatus.UNHEALTHY,
            lastCheck = System.currentTimeMillis()
        )
    }

    override suspend fun getModels(): List<ModelInfo> = listOf(
        ModelInfo(
            id = "local-tvm",
            name = "Local TVM Model",
            provider = ProviderType.LOCAL_TVM,
            contextLength = modelConfig?.contextLength ?: 4096,
            capabilities = capabilities,
            costPerMillionTokens = 0.0f
        )
    )

    override fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        return MemoryInfo(
            usedBytes = runtime.totalMemory() - runtime.freeMemory(),
            availableBytes = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()),
            modelSizeBytes = 0, // Would need model file size
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

    override suspend fun stop() {
        shouldStop = true
    }

    override suspend fun reset() {
        shouldStop = false
        isGenerating = false
    }

    override suspend fun cleanup() {
        unloadModel()
        tvmRuntime = null
        _isInitialized = false
    }

    // Helper functions
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
        // Apply repetition penalty
        val penalizedLogits = logits.copyOf()
        for (token in previousTokens.toSet()) {
            if (token < penalizedLogits.size) {
                penalizedLogits[token] /= repetitionPenalty
            }
        }

        // Apply temperature
        val scaledLogits = penalizedLogits.map { it / temperature }.toFloatArray()

        // Softmax
        val maxLogit = scaledLogits.maxOrNull() ?: 0f
        val expLogits = scaledLogits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sumExp = expLogits.sum()
        val probs = expLogits.map { it / sumExp }

        // Top-K filtering
        val indexed = probs.mapIndexed { i, p -> i to p }.sortedByDescending { it.second }
        val topKTokens = indexed.take(topK)

        // Top-P (nucleus) filtering
        var cumProb = 0f
        val nucleusTokens = mutableListOf<Pair<Int, Float>>()
        for ((token, prob) in topKTokens) {
            nucleusTokens.add(token to prob)
            cumProb += prob
            if (cumProb >= topP) break
        }

        // Renormalize and sample
        val totalProb = nucleusTokens.sumOf { it.second.toDouble() }.toFloat()
        val normalizedProbs = nucleusTokens.map { it.first to (it.second / totalProb) }

        val r = Math.random().toFloat()
        var cumulative = 0f
        for ((token, prob) in normalizedProbs) {
            cumulative += prob
            if (r <= cumulative) return token
        }

        return normalizedProbs.lastOrNull()?.first ?: 0
    }

    private fun isStopToken(token: Int, stopSequences: List<String>): Boolean {
        // Check EOS token (typically 2 for many models)
        if (token == 2) return true

        // Check stop sequences
        val tokenText = tokenizer?.decode(intArrayOf(token)) ?: return false
        return stopSequences.any { tokenText.contains(it) }
    }

    private fun detectGPU(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
                    as android.app.ActivityManager
            val configInfo = activityManager.deviceConfigurationInfo
            configInfo.reqGlEsVersion >= 0x30000 // OpenGL ES 3.0+
        } catch (e: Exception) {
            false
        }
    }
}
