package com.augmentalis.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Strategy interface for response generation
 *
 * Abstracts the response generation mechanism to support:
 * - Template-based responses (fast, reliable)
 * - LLM-based responses (natural, contextual)
 * - Hybrid responses (LLM with template fallback)
 *
 * Design pattern: Strategy
 * Benefit: Easy to switch between implementations without changing ViewModels
 */
interface ResponseGenerator {

    /**
     * Generate response for user message
     *
     * @param userMessage Original user utterance
     * @param intent Detected intent name
     * @param confidence Confidence score (0.0 to 1.0)
     * @param context Additional context (action results, conversation history)
     * @return Flow of response chunks (streaming)
     */
    suspend fun generateResponse(
        userMessage: String,
        intent: String,
        confidence: Float,
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
@Serializable
data class ResponseContext(
    /**
     * Result from action execution (if intent triggered an action)
     */
    val actionResult: ActionResult? = null,

    /**
     * Recent conversation history (user, assistant) pairs
     */
    val conversationHistory: List<ConversationTurn> = emptyList(),

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
 * Single conversation turn (user message + assistant response)
 */
@Serializable
data class ConversationTurn(
    val userMessage: String,
    val assistantResponse: String
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
        val metadata: Map<String, String> = emptyMap()
    ) : ResponseChunk()

    /**
     * Error
     */
    data class Error(
        val message: String,
        val cause: String? = null
    ) : ResponseChunk()
}

/**
 * Generator metadata
 */
@Serializable
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
@Serializable
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

/**
 * Result from action execution
 *
 * Used to inform response generation about what actions were performed.
 */
@Serializable
data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val errorMessage: String? = null,
    val data: Map<String, String> = emptyMap()
)
