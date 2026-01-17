package com.augmentalis.llm

/**
 * Represents a response from an LLM provider
 *
 * This sealed class encapsulates different response types:
 * - Streaming: Incremental text chunks as they're generated
 * - Complete: Full response when generation finishes
 * - Error: Error information if generation fails
 */
sealed class LLMResponse {

    /**
     * Streaming text chunk
     *
     * Emitted continuously as the model generates tokens.
     * Use this to implement typewriter effects in the UI.
     *
     * @param chunk The text generated in this iteration
     * @param tokenCount Number of tokens generated so far (optional)
     */
    data class Streaming(
        val chunk: String,
        val tokenCount: Int? = null
    ) : LLMResponse()

    /**
     * Complete response
     *
     * Emitted once at the end of generation with the full text.
     * Also includes generation statistics.
     *
     * @param fullText The complete generated text
     * @param usage Token usage statistics
     */
    data class Complete(
        val fullText: String,
        val usage: TokenUsage
    ) : LLMResponse()

    /**
     * Error response
     *
     * Emitted when generation fails due to an error.
     *
     * @param message Human-readable error message
     * @param code Error code (if applicable)
     * @param cause Original exception message (if available)
     */
    data class Error(
        val message: String,
        val code: String? = null,
        val cause: String? = null
    ) : LLMResponse()
}

/**
 * Token usage statistics
 *
 * Tracks token consumption for monitoring and billing purposes.
 */
data class TokenUsage(
    /**
     * Number of tokens in the prompt/input
     */
    val promptTokens: Int,

    /**
     * Number of tokens generated in the response
     */
    val completionTokens: Int,

    /**
     * Total tokens used (prompt + completion)
     */
    val totalTokens: Int = promptTokens + completionTokens
)

/**
 * Extension function to extract text from any LLMResponse
 *
 * Useful for collecting streamed chunks or getting the final text.
 */
fun LLMResponse.getText(): String? {
    return when (this) {
        is LLMResponse.Streaming -> chunk
        is LLMResponse.Complete -> fullText
        is LLMResponse.Error -> null
    }
}

/**
 * Extension function to check if response is an error
 */
fun LLMResponse.isError(): Boolean = this is LLMResponse.Error

/**
 * Extension function to check if response is streaming
 */
fun LLMResponse.isStreaming(): Boolean = this is LLMResponse.Streaming

/**
 * Extension function to check if response is complete
 */
fun LLMResponse.isComplete(): Boolean = this is LLMResponse.Complete
