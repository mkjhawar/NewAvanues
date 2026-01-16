package com.augmentalis.llm.response

import com.augmentalis.ava.core.common.Result
import com.augmentalis.nlu.IntentClassification
import kotlinx.coroutines.flow.Flow

/**
 * Strategy interface for response generation
 *
 * Abstracts the response generation mechanism to support:
 * - Template-based responses (current, fast, reliable)
 * - LLM-based responses (future, natural, contextual)
 * - Hybrid responses (LLM with template fallback)
 *
 * Design pattern: Strategy
 * Benefit: Easy to switch between implementations without changing ChatViewModel
 *
 * Created: 2025-11-10
 * Author: Claude Code (Agent 3)
 */
interface ResponseGenerator {

    /**
     * Generate response for user message
     *
     * @param userMessage Original user utterance
     * @param classification NLU classification result
     * @param context Additional context (action results, conversation history)
     * @return Flow of response chunks (streaming)
     */
    suspend fun generateResponse(
        userMessage: String,
        classification: IntentClassification,
        context: ResponseContext = ResponseContext()
    ): Flow<ResponseChunk>

    /**
     * Check if generator is ready to use
     *
     * @return true if initialized and operational, false otherwise
     */
    fun isReady(): Boolean

    /**
     * Get generator metadata
     *
     * @return Information about this generator
     */
    fun getInfo(): GeneratorInfo
}

/**
 * Context for response generation
 *
 * Contains additional information to guide response generation:
 * - Action results from executed intents
 * - Conversation history for multi-turn context
 * - User preferences and settings
 */
data class ResponseContext(
    /**
     * Result from action execution (if intent triggered an action)
     */
    val actionResult: ActionResult? = null,

    /**
     * Recent conversation history (user, assistant) pairs
     */
    val conversationHistory: List<Pair<String, String>> = emptyList(),

    /**
     * User preferences (language, formality, etc.)
     */
    val preferences: Map<String, String> = emptyMap(),

    /**
     * Additional metadata (time of day, location, etc.)
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Response chunk for streaming
 *
 * Supports progressive UI updates (typewriter effect).
 */
sealed class ResponseChunk {
    /**
     * Text chunk
     */
    data class Text(val content: String) : ResponseChunk()

    /**
     * Complete response (final chunk)
     */
    data class Complete(
        val fullText: String,
        val metadata: Map<String, Any> = emptyMap()
    ) : ResponseChunk()

    /**
     * Error
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : ResponseChunk()
}

/**
 * Generator metadata
 */
data class GeneratorInfo(
    /**
     * Generator name (e.g., "Template", "LLM", "Hybrid")
     */
    val name: String,

    /**
     * Generator type
     */
    val type: GeneratorType,

    /**
     * Whether streaming is supported
     */
    val supportsStreaming: Boolean,

    /**
     * Average latency in ms
     */
    val averageLatencyMs: Long? = null,

    /**
     * Additional metadata
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Generator type enum
 */
enum class GeneratorType {
    /**
     * Template-based (fast, deterministic)
     */
    TEMPLATE,

    /**
     * LLM-based (natural, contextual, slower)
     */
    LLM,

    /**
     * Hybrid (LLM with template fallback)
     */
    HYBRID
}
