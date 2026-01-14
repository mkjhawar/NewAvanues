package com.augmentalis.alc.engine

import com.augmentalis.alc.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import kotlin.math.exp

/**
 * ALC Engine for Desktop - Full ONNX Runtime implementation
 *
 * Provides cross-platform LLM inference for macOS, Windows, and Linux.
 * Supports CUDA, DirectML, CoreML, and CPU execution.
 */
class ALCEngineDesktop(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : IInferenceEngine {

    override val providerType = ProviderType.LOCAL_ONNX

    override val capabilities = LLMCapabilities(
        streaming = true,
        functionCalling = false,
        vision = false,
        maxContextLength = 4096,
        supportedLanguages = listOf("en")
    )

    private var onnxRuntime: ONNXRuntime? = null
    private var tokenizer: DesktopTokenizer? = null
    private var _isInitialized = false
    private var _currentModel: String? = null
    private var isGenerating = false
    private var shouldStop = false

    private var totalTokensGenerated = 0L
    private var totalInferenceTimeMs = 0L
    private var sessionCount = 0

    override val isInitialized: Boolean get() = _isInitialized
    override val currentModel: String? get() = _currentModel

    override suspend fun initialize(): Result<Unit> = withContext(dispatcher) {
        try {
            onnxRuntime = ONNXRuntime()
            onnxRuntime!!.initialize().getOrThrow()
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
            val onnxFile = File(modelDir, "model.onnx")
            val vocabFile = File(modelDir, "vocab.txt")

            if (!onnxFile.exists()) {
                return@withContext Result.failure(Exception("Model file not found: ${onnxFile.path}"))
            }

            onnxRuntime!!.loadModel(onnxFile.absolutePath).getOrThrow()
            tokenizer = DesktopTokenizer(vocabFile.absolutePath)

            _currentModel = modelPath
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unloadModel() {
        onnxRuntime?.release()
        tokenizer = null
        _currentModel = null
    }

    override fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        if (!_isInitialized || onnxRuntime == null || tokenizer == null) {
            emit(LLMResponse.Error("Engine not initialized", "NOT_INITIALIZED"))
            return@flow
        }

        isGenerating = true
        shouldStop = false
        sessionCount++
        val startTime = System.currentTimeMillis()

        try {
            val prompt = buildPrompt(messages)
            val inputTokens = tokenizer!!.encode(prompt)

            val generatedTokens = mutableListOf<Long>()
            val fullText = StringBuilder()

            // Create initial input tensor
            var currentInput = inputTokens

            for (i in 0 until options.maxTokens) {
                if (shouldStop) break

                // Create ONNX tensor
                val inputTensor = onnxRuntime!!.createLongTensor(
                    currentInput,
                    longArrayOf(1, currentInput.size.toLong())
                )

                // Create attention mask
                val attentionMask = LongArray(currentInput.size) { 1L }
                val maskTensor = onnxRuntime!!.createLongTensor(
                    attentionMask,
                    longArrayOf(1, attentionMask.size.toLong())
                )

                // Run inference
                val inputs = mapOf(
                    "input_ids" to inputTensor,
                    "attention_mask" to maskTensor
                )

                val outputs = onnxRuntime!!.infer(inputs).getOrThrow()
                val logitsTensor = outputs["logits"] ?: outputs.values.firstOrNull()
                    ?: throw Exception("No output tensor")

                // Extract logits for last token
                val logitsBuffer = logitsTensor.floatBuffer
                val vocabSize = tokenizer!!.vocabSize()
                val logits = FloatArray(vocabSize)

                // Get logits for last position
                val offset = (currentInput.size - 1) * vocabSize
                for (j in 0 until vocabSize) {
                    logits[j] = logitsBuffer.get(offset + j)
                }

                // Sample next token
                val nextToken = sampleToken(
                    logits,
                    options.temperature,
                    options.topP,
                    options.topK,
                    options.repetitionPenalty,
                    generatedTokens.map { it.toInt() }
                )

                // Clean up tensors
                inputTensor.close()
                maskTensor.close()
                outputs.values.forEach { (it as? ai.onnxruntime.OnnxTensor)?.close() }

                // Check for stop
                if (isStopToken(nextToken, options.stopSequences)) break

                generatedTokens.add(nextToken.toLong())
                val tokenText = tokenizer!!.decode(longArrayOf(nextToken.toLong()))
                fullText.append(tokenText)

                if (options.stream) {
                    emit(LLMResponse.Streaming(tokenText, generatedTokens.size))
                }

                // Update input for next iteration (append new token)
                currentInput = currentInput + nextToken.toLong()
                totalTokensGenerated++
            }

            val endTime = System.currentTimeMillis()
            totalInferenceTimeMs += (endTime - startTime)

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
            status = if (_isInitialized && onnxRuntime?.isReady() == true)
                HealthStatus.HEALTHY else HealthStatus.UNHEALTHY,
            lastCheck = System.currentTimeMillis()
        )
    }

    override suspend fun getModels(): List<ModelInfo> = listOf(
        ModelInfo(
            id = "local-onnx",
            name = "Local ONNX Model",
            provider = ProviderType.LOCAL_ONNX,
            contextLength = 4096,
            capabilities = capabilities,
            costPerMillionTokens = 0.0f
        )
    )

    override fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        return MemoryInfo(
            usedBytes = runtime.totalMemory() - runtime.freeMemory(),
            availableBytes = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()),
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
        onnxRuntime?.close()
        onnxRuntime = null
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

        val r = Math.random().toFloat()
        var cumulative = 0f
        for ((token, prob) in normalizedProbs) {
            cumulative += prob
            if (r <= cumulative) return token
        }

        return normalizedProbs.lastOrNull()?.first ?: 0
    }

    private fun isStopToken(token: Int, stopSequences: List<String>): Boolean {
        if (token == 2) return true
        val tokenText = tokenizer?.decode(longArrayOf(token.toLong())) ?: return false
        return stopSequences.any { tokenText.contains(it) }
    }
}
