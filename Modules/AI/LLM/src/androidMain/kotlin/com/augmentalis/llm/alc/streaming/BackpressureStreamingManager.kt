/**
 * Backpressure Streaming Manager
 *
 * Single Responsibility: Manage streaming token generation with backpressure control
 *
 * Implements streaming text generation with:
 * - Backpressure handling to prevent overwhelming consumers
 * - Cancellation support via coroutines
 * - Error recovery and graceful degradation
 * - Progress tracking and metadata events
 *
 * Architecture:
 * - Uses Kotlin Flow for reactive streaming
 * - Channel-based buffering for backpressure
 * - Atomic state tracking for thread safety
 * - Coroutine-based async/await patterns
 *
 * Created: 2025-10-31
 * Author: AVA AI Team
 */

package com.augmentalis.llm.alc.streaming

import com.augmentalis.llm.alc.interfaces.IInferenceStrategy
import com.augmentalis.llm.alc.interfaces.IMemoryManager
import com.augmentalis.llm.alc.interfaces.ISamplerStrategy
import com.augmentalis.llm.alc.interfaces.IStreamingManager
import com.augmentalis.llm.alc.models.GenerationParams
import com.augmentalis.llm.alc.models.InferenceRequest
import com.augmentalis.llm.alc.models.StreamEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Streaming manager with backpressure control
 *
 * @param inferenceStrategy Strategy for running model inference
 * @param samplerStrategy Strategy for sampling tokens from logits
 * @param memoryManager Memory and cache management
 * @param tokenizer Tokenization service for text ↔ tokens conversion
 * @param stopTokens Model-specific stop token IDs (Issue P0-3 fix)
 * @param bufferSize Size of internal buffer for backpressure (default: 32 tokens)
 */
class BackpressureStreamingManager(
    private val inferenceStrategy: IInferenceStrategy,
    private val samplerStrategy: ISamplerStrategy,
    private val memoryManager: IMemoryManager,
    private val tokenizer: ITokenizer,
    private val stopTokens: Set<Int> = DEFAULT_STOP_TOKENS,
    private val bufferSize: Int = 32
) : IStreamingManager {

    private val mutex = Mutex()
    private val isStreaming = AtomicBoolean(false)
    private val activeRequestId = AtomicInteger(0)
    private var stopRequested = false

    // Local KV cache: stores the opaque cache handle returned by inference so that
    // subsequent decode steps can reuse it instead of reprocessing the full context.
    // Volatile for visibility; protected by the flow's single-coroutine execution model.
    @Volatile
    private var kvCache: Any? = null

    override fun streamGeneration(
        prompt: String,
        params: GenerationParams
    ): Flow<StreamEvent> = flow {
        // Validate state
        if (!isStreaming.compareAndSet(false, true)) {
            emit(StreamEvent.Error("Streaming already in progress"))
            return@flow
        }

        val requestId = activeRequestId.incrementAndGet()
        stopRequested = false

        try {
            Timber.d("Starting streaming generation (request $requestId)")

            // Check memory before starting
            val estimatedMemory = estimateMemoryRequired(prompt, params)
            if (!memoryManager.checkMemoryAvailable(estimatedMemory)) {
                emit(StreamEvent.Error("Insufficient memory for generation"))
                return@flow
            }

            // Tokenize input
            val inputTokens = try {
                tokenizer.encode(prompt)
            } catch (e: Exception) {
                Timber.e(e, "Tokenization failed")
                emit(StreamEvent.Error("Tokenization failed: ${e.message}", e))
                return@flow
            }

            Timber.d("Tokenized prompt: ${inputTokens.size} tokens")
            emit(StreamEvent.Metadata("input_tokens", inputTokens.size))

            // Initialize generation state
            var generatedTokens = 0
            val maxTokens = params.maxTokens
            val startTime = System.currentTimeMillis()
            val currentTokens = inputTokens.toMutableList()
            var isPrefill = true

            // Clear any stale KV cache from a previous request before starting.
            kvCache = null

            // Autoregressive generation loop
            while (generatedTokens < maxTokens && !stopRequested) {
                try {
                    // Run inference, supplying the locally-maintained KV cache so the
                    // model can skip re-processing already-seen tokens (prefill → decode).
                    val inferenceResult = inferenceStrategy.infer(
                        InferenceRequest(
                            tokens = currentTokens,
                            cache = kvCache,
                            isPrefill = isPrefill,
                            metadata = mapOf(
                                "request_id" to requestId,
                                "generated_tokens" to generatedTokens
                            )
                        )
                    )

                    // Persist the updated KV cache returned by the model so the next
                    // decode step can reuse it instead of processing the full sequence.
                    kvCache = inferenceResult.cache
                    memoryManager.setCache(inferenceResult.cache)
                    isPrefill = false // Only the first pass is prefill

                    // Sample next token
                    val nextTokenId = samplerStrategy.sample(
                        inferenceResult.logits,
                        params.toSamplingParams()
                    )

                    // Check for stop conditions
                    if (isStopToken(nextTokenId, params)) {
                        Timber.d("Stop token detected: $nextTokenId")
                        break
                    }

                    // Decode token to text
                    val tokenText = try {
                        tokenizer.decode(listOf(nextTokenId))
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to decode token $nextTokenId")
                        "" // Skip invalid tokens
                    }

                    // Emit token event
                    emit(StreamEvent.TokenGenerated(
                        token = nextTokenId,
                        text = tokenText,
                        logProb = inferenceResult.metadata["log_prob"] as? Float
                    ))

                    // Update state
                    currentTokens.add(nextTokenId)
                    generatedTokens++

                    // Check for stop sequences in recent text
                    if (checkStopSequences(currentTokens, params.stopSequences)) {
                        Timber.d("Stop sequence detected")
                        break
                    }

                    // Context length management
                    if (currentTokens.size > DEFAULT_MAX_CONTEXT) {
                        val trimSize = currentTokens.size - DEFAULT_MAX_CONTEXT
                        currentTokens.subList(0, trimSize).clear()
                        emit(StreamEvent.Metadata("context_trimmed", trimSize))
                    }

                    // Periodic memory optimization
                    if (generatedTokens % MEMORY_CHECK_INTERVAL == 0) {
                        val usage = memoryManager.getCurrentMemoryUsage()
                        emit(StreamEvent.Metadata("memory_usage_bytes", usage))

                        if (usage > memoryManager.getMemoryBudget() * 0.9) {
                            Timber.w("Memory usage high, optimizing...")
                            val freed = memoryManager.optimizeMemory()
                            emit(StreamEvent.Metadata("memory_freed_bytes", freed))
                        }
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Inference failed at token $generatedTokens")
                    emit(StreamEvent.Error(
                        message = "Inference failed: ${e.message}",
                        exception = e
                    ))
                    break
                }
            }

            // Emit completion event
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            val tokensPerSecond = if (duration > 0) {
                (generatedTokens.toFloat() / duration) * 1000f
            } else 0f

            emit(StreamEvent.GenerationComplete(
                totalTokens = generatedTokens,
                duration = duration,
                tokensPerSecond = tokensPerSecond
            ))

            Timber.i("Completed request $requestId: $generatedTokens tokens in ${duration}ms (${tokensPerSecond} tok/s)")

        } catch (e: Exception) {
            Timber.e(e, "Streaming generation failed")
            emit(StreamEvent.Error(
                message = "Generation failed: ${e.message}",
                exception = e
            ))
        } finally {
            isStreaming.set(false)
            stopRequested = false
        }
    }

    override suspend fun stopStreaming() = mutex.withLock {
        if (isStreaming.get()) {
            Timber.d("Stopping streaming generation")
            stopRequested = true
        }
    }

    override fun isStreaming(): Boolean = isStreaming.get()

    /**
     * Estimate memory required for generation
     */
    private fun estimateMemoryRequired(prompt: String, params: GenerationParams): Long {
        // Rough estimate: prompt + max tokens * average token size
        val promptTokens = prompt.length / 4 // Approximation
        val totalTokens = promptTokens + params.maxTokens
        return totalTokens * BYTES_PER_TOKEN
    }

    /**
     * Check if token is a stop token.
     *
     * Issue P0-3 Fix: Now uses model-specific stop tokens passed at construction,
     * rather than hardcoded [0, 1, 2] which doesn't match most models.
     *
     * @param tokenId Token ID to check
     * @param params Generation parameters (reserved for future use)
     * @return true if this token should stop generation
     */
    private fun isStopToken(tokenId: Int, params: GenerationParams): Boolean {
        return tokenId in stopTokens
    }

    /**
     * Check if recent tokens match any stop sequence
     */
    private fun checkStopSequences(tokens: List<Int>, stopSequences: List<String>): Boolean {
        if (stopSequences.isEmpty() || tokens.size < 5) return false

        try {
            // Decode recent tokens
            val recentText = tokenizer.decode(tokens.takeLast(20))

            // Check each stop sequence
            for (stopSeq in stopSequences) {
                if (recentText.contains(stopSeq)) {
                    return true
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to check stop sequences")
        }

        return false
    }

    companion object {
        private const val DEFAULT_MAX_CONTEXT = 2048
        private const val MEMORY_CHECK_INTERVAL = 10
        private const val BYTES_PER_TOKEN = 100L // Rough estimate

        /**
         * Default stop tokens (Issue P0-3 Fix).
         *
         * These are common EOS tokens across model families:
         * - 1: Gemma, Llama 2
         * - 2: Llama 2 alternate, Mistral
         * - 107: Gemma end_of_turn
         *
         * For accurate stop detection, pass model-specific tokens
         * loaded via ModelConfigLoader.getStopTokens(modelPath)
         */
        val DEFAULT_STOP_TOKENS = setOf(1, 2, 107)
    }
}

/**
 * Tokenizer interface for text ↔ token conversion
 */
interface ITokenizer {
    /**
     * Encode text to token IDs
     */
    fun encode(text: String): List<Int>

    /**
     * Decode token IDs to text
     */
    fun decode(tokens: List<Int>): String
}

/**
 * Extension function to convert GenerationParams to SamplingParams
 */
private fun GenerationParams.toSamplingParams() = com.augmentalis.llm.alc.models.SamplingParams(
    temperature = this.temperature,
    topP = this.topP,
    topK = this.topK,
    repeatPenalty = this.repeatPenalty
)

/**
 * Suspend extension to get cache from memory manager
 */
private suspend fun IMemoryManager.getCache(): Any? {
    // Note: This assumes memory manager has cache access
    // In full implementation, this would call a proper method
    return getCacheStats()["cache_handle"]
}

/**
 * Suspend extension to set cache in memory manager
 */
private suspend fun IMemoryManager.setCache(cache: Any?) {
    // Note: This assumes memory manager has cache setter
    // In full implementation, this would call a proper method
    if (cache != null) {
        // Cache update would happen here
    }
}
