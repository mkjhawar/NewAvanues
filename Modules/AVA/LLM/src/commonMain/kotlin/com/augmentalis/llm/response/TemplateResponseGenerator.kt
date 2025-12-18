package com.augmentalis.llm.response

import com.augmentalis.nlu.IntentClassification
import com.augmentalis.ava.platform.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Template-based response generator (current implementation)
 *
 * Uses hardcoded templates from IntentTemplates for fast, reliable responses.
 * This is the fallback when LLM is unavailable or fails.
 *
 * Characteristics:
 * - Fast: <1ms response time
 * - Reliable: No network or model dependencies
 * - Deterministic: Same input = same output
 * - Limited: No context awareness, no personalization
 *
 * Usage: Currently used by ChatViewModel for all responses.
 * Future: Will be fallback when LLMResponseGenerator fails.
 *
 * Created: 2025-11-10
 * Author: Claude Code (Agent 3)
 */
class TemplateResponseGenerator : ResponseGenerator {

    /**
     * Generate template-based response
     *
     * Flow:
     * 1. Look up template for intent
     * 2. Return as single streaming chunk
     * 3. Emit complete signal
     *
     * Now uses ImprovedIntentTemplates with:
     * - Multiple response variations
     * - 3-strikes fallback rule
     * - Confidence-based responses
     * - Better user guidance
     *
     * @param userMessage Original user utterance (not used, for interface consistency)
     * @param classification NLU classification result
     * @param context Additional context (used for conversation ID)
     * @return Flow of response chunks
     */
    override suspend fun generateResponse(
        userMessage: String,
        classification: IntentClassification,
        context: ResponseContext
    ): Flow<ResponseChunk> = flow {
        try {
            val startTime = getCurrentTimeMillis()

            // Get improved template with confidence-based response
            val template = ImprovedIntentTemplates.getConfidenceBasedResponse(
                intent = classification.intent,
                confidence = classification.confidence,
                conversationId = context.metadata["conversationId"]
            )

            // Emit as streaming chunk (simulated for consistent interface)
            emit(ResponseChunk.Text(template))

            // Emit complete
            val latency = getCurrentTimeMillis() - startTime
            emit(ResponseChunk.Complete(
                fullText = template,
                metadata = mapOf(
                    "generator" to "template",
                    "intent" to classification.intent,
                    "confidence" to classification.confidence,
                    "latency_ms" to latency
                )
            ))

            Logger.d(TAG, "Template response generated in ${latency}ms for intent: ${classification.intent}")

        } catch (e: Exception) {
            Logger.e(TAG, "Template generation failed", e)
            emit(ResponseChunk.Error(
                message = "Failed to generate template response: ${e.message}",
                exception = e
            ))
        }
    }

    /**
     * Template generator is always ready
     */
    override fun isReady(): Boolean = true

    /**
     * Get generator info
     */
    override fun getInfo(): GeneratorInfo {
        return GeneratorInfo(
            name = "Template Response Generator (Improved)",
            type = GeneratorType.TEMPLATE,
            supportsStreaming = true,
            averageLatencyMs = 1,
            metadata = mapOf(
                "templates_type" to "improved_with_fallback",
                "fallback_strategy" to "3_strikes_rule"
            )
        )
    }

    private companion object {
        private const val TAG = "TemplateResponseGenerator"
    }
}

/**
 * Platform-agnostic time function
 * This is a simple implementation that should be replaced by platform-specific time provider
 */
private expect fun getCurrentTimeMillis(): Long
