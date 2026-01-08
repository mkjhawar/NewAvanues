package com.augmentalis.alc.domain

import kotlinx.serialization.Serializable

/**
 * Represents a response from an LLM provider
 *
 * Sealed class for type-safe response handling:
 * - Streaming: Incremental text chunks during generation
 * - Complete: Full response when generation finishes
 * - Error: Error information if generation fails
 */
@Serializable
sealed class LLMResponse {

    /**
     * Streaming text chunk
     *
     * Emitted continuously as the model generates tokens.
     * Use for typewriter effects in UI.
     */
    @Serializable
    data class Streaming(
        val chunk: String,
        val tokenCount: Int? = null,
        val finishReason: String? = null
    ) : LLMResponse()

    /**
     * Complete response
     *
     * Emitted once at the end of generation with full text and stats.
     */
    @Serializable
    data class Complete(
        val fullText: String,
        val usage: TokenUsage,
        val model: String? = null,
        val finishReason: String = "stop",
        val latencyMs: Long? = null
    ) : LLMResponse()

    /**
     * Error response
     *
     * Emitted when generation fails.
     */
    @Serializable
    data class Error(
        val message: String,
        val code: String? = null,
        val retryable: Boolean = false,
        val cause: String? = null
    ) : LLMResponse()
}

/**
 * Extension to check if response is final
 */
val LLMResponse.isFinal: Boolean
    get() = this is LLMResponse.Complete || this is LLMResponse.Error

/**
 * Extension to extract text content
 */
val LLMResponse.text: String?
    get() = when (this) {
        is LLMResponse.Streaming -> chunk
        is LLMResponse.Complete -> fullText
        is LLMResponse.Error -> null
    }
