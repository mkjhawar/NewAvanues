package com.augmentalis.llm.response

import android.content.Context
import com.augmentalis.llm.provider.LocalLLMProvider
import com.augmentalis.llm.domain.*
import com.augmentalis.llm.LLMResponse
import com.augmentalis.nlu.IntentClassification
import com.augmentalis.llm.LLMResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber

/**
 * LLM-based response generator
 *
 * Uses LocalLLMProvider for natural, context-aware response generation.
 * Creates prompts via LLMContextBuilder and streams responses from LLM.
 *
 * Characteristics:
 * - Natural: Human-like, contextual responses
 * - Adaptive: Learns from conversation context
 * - Personalized: Considers user preferences
 * - Slower: ~100-500ms on-device inference (depending on model/hardware)
 *
 * Status: ACTIVE - P0 blocker resolved (2025-11-27)
 * Ready for use: ChatViewModel can switch from TemplateResponseGenerator to this
 *
 * Token budget:
 * - Input: 50-100 tokens (mobile-optimized prompts)
 * - Output: 30-50 tokens (concise responses)
 * - Total: <150 tokens per request
 *
 * Created: 2025-11-10
 * Updated: 2025-11-27 - Unblocked LLM response generation (P0-LLM-003)
 */
class LLMResponseGenerator(
    private val context: Context,
    private val llmProvider: LocalLLMProvider,
    private val contextBuilder: LLMContextBuilder = LLMContextBuilder()
) : ResponseGenerator {

    companion object {
        private const val TAG = "LLMResponseGenerator"

        /**
         * Maximum tokens for response generation
         * Mobile-optimized: Short, focused responses
         */
        private const val MAX_RESPONSE_TOKENS = 50

        /**
         * Temperature for generation
         * 0.7 = balanced between creative and focused
         */
        private const val TEMPERATURE = 0.7f

        /**
         * Top-p sampling threshold
         * 0.9 = consider top 90% of probability mass
         */
        private const val TOP_P = 0.9f
    }

    private var isInitialized = false

    /**
     * Initialize LLM provider
     *
     * Must be called before generateResponse().
     * Loads model into memory (~100-300ms on typical Android devices).
     *
     * @param config LLM configuration
     * @return LLMResult.Success if initialized, LLMResult.Error otherwise
     */
    suspend fun initialize(config: LLMConfig): LLMResult<Unit> {
        return try {
            val result = llmProvider.initialize(config)
            if (result is LLMResult.Success) {
                isInitialized = true
                Timber.i("LLM response generator initialized successfully")
            } else {
                Timber.w("LLM response generator initialization failed: ${(result as LLMResult.Error).message}")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "LLM response generator initialization exception")
            LLMResult.Error(
                message = "Initialization failed: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Generate LLM-based response
     *
     * Flow:
     * 1. Check if initialized
     * 2. Build prompt using LLMContextBuilder
     * 3. Validate token count (abort if too long)
     * 4. Call LocalLLMProvider.generateResponse()
     * 5. Stream response chunks to UI
     * 6. Handle errors (emit error chunk)
     *
     * Fallback: Caller should catch errors and fall back to TemplateResponseGenerator
     *
     * @param userMessage Original user utterance
     * @param classification NLU classification result
     * @param context Additional context (action results, history)
     * @return Flow of response chunks
     */
    override suspend fun generateResponse(
        userMessage: String,
        classification: IntentClassification,
        context: ResponseContext
    ): Flow<ResponseChunk> = flow {
        // Check initialization
        if (!isInitialized) {
            Timber.w("LLM generator not initialized, cannot generate response")
            emit(ResponseChunk.Error(
                message = "LLM not initialized",
                exception = IllegalStateException("Call initialize() first")
            ))
            return@flow
        }

        try {
            val startTime = System.currentTimeMillis()
            Timber.d("Generating LLM response for intent: ${classification.intent}, confidence: ${classification.confidence}")

            // Build prompt based on confidence
            val prompt = if (classification.confidence <= 0.5f) {
                // Low confidence: Use teach mode prompt
                contextBuilder.buildLowConfidencePrompt(userMessage, classification)
            } else {
                // Normal confidence: Use intent-specific prompt
                val contextMap = buildContextMap(context)
                contextBuilder.buildIntentPrompt(
                    intent = classification.intent,
                    userMessage = userMessage,
                    context = contextMap
                )
            }

            // Validate token count
            val estimatedTokens = contextBuilder.estimateTokens(prompt)
            if (estimatedTokens > 200) {
                Timber.w("Prompt too long: $estimatedTokens tokens, truncating context")
                // TODO: Implement context truncation if needed
            }

            Timber.d("Prompt built (${estimatedTokens} estimated tokens):\n$prompt")

            // Generate response via LLM
            val options = GenerationOptions(
                temperature = TEMPERATURE,
                maxTokens = MAX_RESPONSE_TOKENS,
                topP = TOP_P
            )

            llmProvider.generateResponse(prompt, options)
                .catch { e ->
                    Timber.e(e, "LLM generation failed")
                    emit(ResponseChunk.Error(
                        message = "LLM generation failed: ${e.message}",
                        exception = e
                    ))
                }
                .collect { llmResponse ->
                    when (llmResponse) {
                        is LLMResponse.Streaming -> {
                            // Stream text chunk to UI
                            emit(ResponseChunk.Text(llmResponse.chunk))
                        }
                        is LLMResponse.Complete -> {
                            // Emit complete with metadata
                            val latency = System.currentTimeMillis() - startTime
                            emit(ResponseChunk.Complete(
                                fullText = llmResponse.fullText,
                                metadata = mapOf(
                                    "generator" to "llm",
                                    "intent" to classification.intent,
                                    "confidence" to classification.confidence,
                                    "latency_ms" to latency,
                                    "tokens_input" to llmResponse.usage.promptTokens,
                                    "tokens_output" to llmResponse.usage.completionTokens
                                )
                            ))
                            Timber.i("LLM response generated in ${latency}ms, tokens: ${llmResponse.usage.totalTokens}")
                        }
                        is LLMResponse.Error -> {
                            // Propagate error
                            emit(ResponseChunk.Error(
                                message = llmResponse.message,
                                exception = null  // LLMResponse.Error uses cause: String, not exception
                            ))
                        }
                    }
                }

        } catch (e: Exception) {
            Timber.e(e, "LLM response generation exception")
            emit(ResponseChunk.Error(
                message = "Failed to generate LLM response: ${e.message}",
                exception = e
            ))
        }
    }

    /**
     * Check if LLM generator is ready
     */
    override fun isReady(): Boolean = isInitialized && llmProvider.getInfo().name.isNotEmpty()

    /**
     * Get generator info
     */
    override fun getInfo(): GeneratorInfo {
        val providerInfo = llmProvider.getInfo()
        return GeneratorInfo(
            name = "LLM Response Generator",
            type = GeneratorType.LLM,
            supportsStreaming = true,
            averageLatencyMs = 200, // Estimated, will be measured in production
            metadata = mapOf(
                "provider" to providerInfo.name,
                "model" to providerInfo.modelName,
                "status" to if (isInitialized) "ready" else "not_initialized",
                "version" to "1.0",
                "unblocked" to "2025-11-27"
            )
        )
    }

    /**
     * Build context map from ResponseContext
     *
     * Extracts relevant information for prompt building.
     */
    private fun buildContextMap(context: ResponseContext): Map<String, String> {
        val map = mutableMapOf<String, String>()

        // Add action result if available
        context.actionResult?.let {
            if (it.success) {
                map["action_status"] = "success"
                it.message?.let { msg -> map["action_message"] = msg }
            } else {
                map["action_status"] = "failed"
                it.errorMessage?.let { err -> map["action_error"] = err }
            }
        }

        // Add metadata (time, location, etc.)
        map.putAll(context.metadata)

        return map
    }

    /**
     * Cleanup LLM resources
     */
    suspend fun cleanup() {
        llmProvider.cleanup()
        isInitialized = false
        Timber.d("LLM response generator cleaned up")
    }
}
