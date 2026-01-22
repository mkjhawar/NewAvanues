/**
 * LLMPlugin.kt - Large Language Model plugin interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for LLM plugins that provide text generation capabilities.
 * Supports both synchronous and streaming generation modes for flexibility
 * in different use cases (voice feedback, real-time display, etc.).
 */
package com.augmentalis.magiccode.plugins.universal.contracts.ai

import com.augmentalis.magiccode.plugins.universal.PluginCapability
import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Plugin interface for Large Language Model text generation.
 *
 * LLMPlugin extends [UniversalPlugin] to provide text generation capabilities
 * using large language models. It supports both synchronous and streaming
 * generation modes to accommodate different use cases:
 *
 * - **Synchronous**: Best for short completions, batch processing
 * - **Streaming**: Best for real-time display, voice feedback, progressive UX
 *
 * ## Capability
 * Implementations must advertise [PluginCapability.LLM_TEXT_GENERATION].
 *
 * ## Implementation Example
 * ```kotlin
 * class LocalLlamaPlugin : LLMPlugin {
 *     override val pluginId = "com.augmentalis.llm.llama"
 *     override val modelInfo = AIModelInfo.local("Llama-3.2-3B", "3.2.0", "q4_0")
 *     override val contextWindow = 8192
 *     override val supportsStreaming = true
 *
 *     override suspend fun generate(request: GenerationRequest): GenerationResponse {
 *         // Implementation using llama.cpp bindings
 *     }
 *
 *     override fun generateStream(request: GenerationRequest): Flow<GenerationToken> {
 *         return flow {
 *             // Stream tokens as they are generated
 *         }
 *     }
 * }
 * ```
 *
 * ## Thread Safety
 * Implementations should be thread-safe. Multiple concurrent generation
 * requests may be made, especially in streaming mode.
 *
 * @since 1.0.0
 * @see UniversalPlugin
 * @see AIModelInfo
 * @see GenerationRequest
 * @see GenerationResponse
 */
interface LLMPlugin : UniversalPlugin {

    // =========================================================================
    // Model Properties
    // =========================================================================

    /**
     * Information about the underlying AI model.
     *
     * Includes model name, version, provider, and resource requirements.
     *
     * @see AIModelInfo
     */
    val modelInfo: AIModelInfo

    /**
     * Maximum context window size in tokens.
     *
     * This includes both input (prompt + system prompt) and output tokens.
     * Requests exceeding this limit should be rejected or truncated.
     *
     * Common values:
     * - 2048 for smaller models
     * - 4096 for medium models
     * - 8192, 16384, 32768 for larger models
     * - 128000+ for frontier models (GPT-4, Claude 3)
     */
    val contextWindow: Int

    /**
     * Whether this plugin supports streaming generation.
     *
     * If false, [generateStream] should throw [UnsupportedOperationException].
     */
    val supportsStreaming: Boolean

    // =========================================================================
    // Generation Methods
    // =========================================================================

    /**
     * Generate text completion synchronously.
     *
     * Processes the entire generation request and returns the complete response.
     * Use for short completions or when the full response is needed before
     * proceeding.
     *
     * ## Error Handling
     * - Returns [GenerationResponse] with [FinishReason.ERROR] on failures
     * - Throws exceptions only for unrecoverable errors (plugin not ready, etc.)
     *
     * @param request The generation request with prompt and parameters
     * @return Complete generation response
     * @throws IllegalStateException if plugin is not ready
     * @see GenerationRequest
     * @see GenerationResponse
     */
    suspend fun generate(request: GenerationRequest): GenerationResponse

    /**
     * Generate text completion with streaming.
     *
     * Returns a Flow that emits tokens as they are generated. This enables:
     * - Progressive display for better UX
     * - Early cancellation if response is unneeded
     * - Real-time voice synthesis
     *
     * ## Usage
     * ```kotlin
     * plugin.generateStream(request)
     *     .collect { token ->
     *         if (token.isFinal) {
     *             println("\nGeneration complete")
     *         } else {
     *             print(token.token)
     *         }
     *     }
     * ```
     *
     * @param request The generation request with prompt and parameters
     * @return Flow of generation tokens
     * @throws UnsupportedOperationException if streaming not supported
     * @throws IllegalStateException if plugin is not ready
     * @see GenerationToken
     */
    fun generateStream(request: GenerationRequest): Flow<GenerationToken>

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Count tokens in the given text.
     *
     * Returns the number of tokens the model would use to represent the text.
     * Useful for:
     * - Checking if prompt fits in context window
     * - Estimating costs for API-based models
     * - Splitting long documents
     *
     * ## Implementation Note
     * Token counts may vary between models. Use the model-specific tokenizer
     * for accurate counts.
     *
     * @param text Text to tokenize
     * @return Number of tokens
     */
    fun countTokens(text: String): Int

    /**
     * Check if the plugin is ready to process requests.
     *
     * Returns true if:
     * - Plugin is in ACTIVE state
     * - Model is loaded and ready
     * - All required resources are available
     *
     * Use this before sending requests to avoid errors.
     *
     * @return true if ready to process generation requests
     */
    fun isReady(): Boolean
}

// =============================================================================
// Request/Response Data Classes
// =============================================================================

/**
 * Request for text generation.
 *
 * Contains all parameters needed to generate text using an LLM.
 * Supports both completion and chat-style prompting.
 *
 * ## Usage
 * ```kotlin
 * val request = GenerationRequest(
 *     prompt = "What is the capital of France?",
 *     systemPrompt = "You are a helpful assistant. Answer concisely.",
 *     maxTokens = 100,
 *     temperature = 0.7f
 * )
 * ```
 *
 * @property prompt The user prompt/query to generate a response for
 * @property systemPrompt Optional system prompt for context/behavior setting
 * @property maxTokens Maximum tokens to generate (null = model default)
 * @property temperature Sampling temperature (0.0 = deterministic, 2.0 = creative)
 * @property topP Nucleus sampling parameter (0.0-1.0)
 * @property stopSequences Sequences that trigger generation to stop
 * @property metadata Additional request metadata for logging/tracking
 *
 * @since 1.0.0
 * @see LLMPlugin.generate
 */
@Serializable
data class GenerationRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val maxTokens: Int? = null,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val stopSequences: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Validate request parameters.
     *
     * @return List of validation errors, empty if valid
     */
    fun validate(): List<String> = buildList {
        if (prompt.isBlank()) add("Prompt cannot be blank")
        if (temperature < 0f || temperature > 2f) add("Temperature must be between 0.0 and 2.0")
        if (topP < 0f || topP > 1f) add("TopP must be between 0.0 and 1.0")
        maxTokens?.let { if (it <= 0) add("MaxTokens must be positive") }
    }

    /**
     * Check if request is valid.
     *
     * @return true if all parameters are valid
     */
    fun isValid(): Boolean = validate().isEmpty()

    companion object {
        /**
         * Create a simple completion request.
         *
         * @param prompt The prompt text
         * @param maxTokens Maximum tokens to generate
         * @return GenerationRequest with default parameters
         */
        fun simple(prompt: String, maxTokens: Int = 256): GenerationRequest =
            GenerationRequest(prompt = prompt, maxTokens = maxTokens)

        /**
         * Create a request with system prompt.
         *
         * @param prompt User prompt
         * @param systemPrompt System/context prompt
         * @param maxTokens Maximum tokens to generate
         * @return GenerationRequest with system prompt
         */
        fun withSystem(
            prompt: String,
            systemPrompt: String,
            maxTokens: Int = 256
        ): GenerationRequest = GenerationRequest(
            prompt = prompt,
            systemPrompt = systemPrompt,
            maxTokens = maxTokens
        )

        /**
         * Create a deterministic request (temperature = 0).
         *
         * @param prompt The prompt text
         * @param systemPrompt Optional system prompt
         * @param maxTokens Maximum tokens to generate
         * @return GenerationRequest with temperature = 0
         */
        fun deterministic(
            prompt: String,
            systemPrompt: String? = null,
            maxTokens: Int = 256
        ): GenerationRequest = GenerationRequest(
            prompt = prompt,
            systemPrompt = systemPrompt,
            maxTokens = maxTokens,
            temperature = 0f,
            topP = 1f
        )
    }
}

/**
 * Response from text generation.
 *
 * Contains the generated text along with metadata about the generation.
 *
 * ## Usage
 * ```kotlin
 * val response = plugin.generate(request)
 * when (response.finishReason) {
 *     FinishReason.COMPLETE -> println("Response: ${response.text}")
 *     FinishReason.MAX_TOKENS -> println("Truncated: ${response.text}...")
 *     FinishReason.ERROR -> println("Error during generation")
 *     else -> println("Stopped: ${response.text}")
 * }
 * ```
 *
 * @property text The generated text
 * @property finishReason Why generation stopped
 * @property tokenCount Number of tokens in the response
 * @property latencyMs Time taken for generation in milliseconds
 * @property metadata Additional response metadata
 *
 * @since 1.0.0
 * @see LLMPlugin.generate
 * @see FinishReason
 */
@Serializable
data class GenerationResponse(
    val text: String,
    val finishReason: FinishReason,
    val tokenCount: Int,
    val latencyMs: Long,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Check if generation completed successfully.
     *
     * @return true if finished with COMPLETE reason
     */
    fun isComplete(): Boolean = finishReason == FinishReason.COMPLETE

    /**
     * Check if generation was truncated due to token limit.
     *
     * @return true if finished due to MAX_TOKENS
     */
    fun isTruncated(): Boolean = finishReason == FinishReason.MAX_TOKENS

    /**
     * Check if generation encountered an error.
     *
     * @return true if finished with ERROR reason
     */
    fun isError(): Boolean = finishReason == FinishReason.ERROR

    /**
     * Get throughput in tokens per second.
     *
     * @return Tokens per second or null if latency is 0
     */
    fun tokensPerSecond(): Float? {
        return if (latencyMs > 0) {
            (tokenCount.toFloat() / latencyMs) * 1000f
        } else null
    }

    companion object {
        /**
         * Create an error response.
         *
         * @param errorMessage Error description
         * @param latencyMs Time taken before error
         * @return GenerationResponse with ERROR reason
         */
        fun error(errorMessage: String, latencyMs: Long = 0): GenerationResponse =
            GenerationResponse(
                text = "",
                finishReason = FinishReason.ERROR,
                tokenCount = 0,
                latencyMs = latencyMs,
                metadata = mapOf("error" to errorMessage)
            )
    }
}

/**
 * Token emitted during streaming generation.
 *
 * Each token represents a piece of the generated text. The final token
 * has [isFinal] set to true and may contain an empty token string.
 *
 * ## Usage
 * ```kotlin
 * val fullText = StringBuilder()
 * plugin.generateStream(request).collect { token ->
 *     if (!token.isFinal) {
 *         fullText.append(token.token)
 *     }
 * }
 * ```
 *
 * @property token The generated token text (may be empty for final signal)
 * @property isFinal Whether this is the final token in the stream
 *
 * @since 1.0.0
 * @see LLMPlugin.generateStream
 */
@Serializable
data class GenerationToken(
    val token: String,
    val isFinal: Boolean = false
) {
    companion object {
        /**
         * Create a final token signal.
         *
         * @return GenerationToken with isFinal = true
         */
        fun final(): GenerationToken = GenerationToken(token = "", isFinal = true)

        /**
         * Create a content token.
         *
         * @param text Token text
         * @return GenerationToken with the text
         */
        fun of(text: String): GenerationToken = GenerationToken(token = text, isFinal = false)
    }
}

/**
 * Reason why text generation stopped.
 *
 * Indicates why the generation process finished, which affects how
 * the response should be interpreted and used.
 *
 * @since 1.0.0
 * @see GenerationResponse.finishReason
 */
@Serializable
enum class FinishReason {
    /**
     * Generation completed naturally.
     *
     * The model finished generating text and produced an end-of-sequence
     * token. The response is complete and usable as-is.
     */
    COMPLETE,

    /**
     * Generation stopped due to maximum token limit.
     *
     * The [GenerationRequest.maxTokens] limit was reached. The response
     * may be incomplete and may need continuation or should be regenerated
     * with a higher limit.
     */
    MAX_TOKENS,

    /**
     * Generation stopped due to a stop sequence.
     *
     * One of the [GenerationRequest.stopSequences] was encountered in
     * the generated text. This is typically intentional for structured output.
     */
    STOP_SEQUENCE,

    /**
     * Generation failed due to an error.
     *
     * An error occurred during generation. Check [GenerationResponse.metadata]
     * for error details. The response text may be empty or partial.
     */
    ERROR
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Check if this plugin can handle the requested token count.
 *
 * @param promptTokens Number of tokens in the prompt
 * @param maxOutputTokens Desired maximum output tokens
 * @return true if the request fits within the context window
 */
fun LLMPlugin.canHandle(promptTokens: Int, maxOutputTokens: Int): Boolean {
    return (promptTokens + maxOutputTokens) <= contextWindow
}

/**
 * Generate with automatic request validation.
 *
 * @param request The generation request
 * @return GenerationResponse or error response if validation fails
 */
suspend fun LLMPlugin.generateSafe(request: GenerationRequest): GenerationResponse {
    val errors = request.validate()
    if (errors.isNotEmpty()) {
        return GenerationResponse.error("Validation failed: ${errors.joinToString(", ")}")
    }
    if (!isReady()) {
        return GenerationResponse.error("Plugin not ready")
    }
    return generate(request)
}
