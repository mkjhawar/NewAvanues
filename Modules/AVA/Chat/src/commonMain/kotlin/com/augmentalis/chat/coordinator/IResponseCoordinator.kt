package com.augmentalis.chat.coordinator

import com.augmentalis.llm.response.ResponseContext
import com.augmentalis.nlu.IntentClassification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Response Coordinator Interface - Cross-platform response generation
 *
 * Abstracts LLM/template response generation for cross-platform use in KMP.
 * Provides:
 * - LLM streaming responses with template fallback
 * - Self-learning from LLM responses (ADR-013)
 * - Response cleanup and metadata extraction
 *
 * @see ResponseCoordinator for Android implementation
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
interface IResponseCoordinator {
    // ==================== State ====================

    /**
     * Last responder type: "NLU" or "LLM"
     */
    val lastResponder: StateFlow<String?>

    /**
     * Timestamp of last response generation
     */
    val lastResponderTimestamp: StateFlow<Long>

    /**
     * Indicates whether LLM fallback was invoked for the last response
     */
    val llmFallbackInvoked: StateFlow<Boolean>

    // ==================== Thresholds ====================

    /**
     * Confidence threshold below which LLM fallback is triggered
     */
    val llmFallbackThreshold: StateFlow<Float>

    /**
     * Confidence threshold for self-learning from LLM responses
     */
    val selfLearningThreshold: StateFlow<Float>

    // ==================== Response Generation ====================

    /**
     * Response generation result containing content and metadata.
     */
    data class ResponseResult(
        val content: String,
        val wasLLMFallback: Boolean,
        val respondedBy: String
    )

    /**
     * Generate a response for the given classification and context.
     *
     * Handles:
     * - LLM streaming with template fallback
     * - Responder tracking (NLU vs LLM)
     * - Self-learning from low confidence responses
     *
     * @param userMessage User input text
     * @param classification NLU classification result
     * @param context Response context including conversation history
     * @param ragContext Optional RAG context to include
     * @param scope Coroutine scope for self-learning coroutine
     * @return ResponseResult with content and metadata
     */
    suspend fun generateResponse(
        userMessage: String,
        classification: IntentClassification,
        context: ResponseContext,
        ragContext: String? = null,
        scope: CoroutineScope
    ): ResponseResult

    /**
     * Set the last responder and timestamp.
     */
    fun setResponder(responder: String)

    /**
     * Reset fallback flag (called at start of each message).
     */
    fun resetFallbackFlag()
}
