package com.augmentalis.ava.features.llm.domain

import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.flow.Flow

/**
 * Main interface for LLM (Large Language Model) providers in AVA AI
 *
 * This interface abstracts different LLM implementations:
 * - LocalLLMProvider: On-device inference with MLC LLM
 * - CloudLLMProvider: Cloud API calls (Gemini, GPT, etc.)
 * - HybridLLMProvider: Intelligent routing between local and cloud
 *
 * All methods are suspend functions to support coroutine-based async operations.
 * Responses are streamed via Flow<String> for real-time UI updates (typewriter effect).
 */
interface LLMProvider {

    /**
     * Initialize the LLM provider
     *
     * For local providers: Load model into memory
     * For cloud providers: Validate API keys and connectivity
     *
     * @param config Configuration for the provider
     * @return Result.Success if initialization succeeds, Result.Error otherwise
     */
    suspend fun initialize(config: LLMConfig): Result<Unit>

    /**
     * Generate a streaming response to a single prompt
     *
     * This is a stateless operation - no conversation history is maintained.
     * Use chat() for multi-turn conversations.
     *
     * @param prompt User's input text
     * @param options Generation options (temperature, max tokens, etc.)
     * @return Flow of text chunks (stream) as they're generated
     */
    suspend fun generateResponse(
        prompt: String,
        options: GenerationOptions = GenerationOptions()
    ): Flow<LLMResponse>

    /**
     * Generate a streaming response within a conversation context
     *
     * This maintains conversation history for multi-turn dialogs.
     * The provider handles formatting messages into the model's expected format.
     *
     * @param messages Conversation history (system, user, assistant messages)
     * @param options Generation options
     * @return Flow of text chunks (stream) as they're generated
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions = GenerationOptions()
    ): Flow<LLMResponse>

    /**
     * Stop any ongoing generation
     *
     * This cancels the current inference and stops the streaming response.
     */
    suspend fun stop()

    /**
     * Reset the provider state
     *
     * Clears conversation history, cancels ongoing generations.
     * Model remains loaded in memory.
     */
    suspend fun reset()

    /**
     * Clean up resources and unload the model
     *
     * For local providers: Unload model from memory
     * For cloud providers: Close network connections
     *
     * Call this when the LLM is no longer needed (e.g., app shutdown)
     */
    suspend fun cleanup()

    /**
     * Check if the provider is currently processing a request
     *
     * @return true if generating, false otherwise
     */
    fun isGenerating(): Boolean

    /**
     * Get provider metadata (name, version, capabilities)
     *
     * @return Provider information
     */
    fun getInfo(): LLMProviderInfo

    /**
     * Check provider health status
     *
     * Performs a lightweight health check to verify the provider is operational.
     * For cloud providers: Pings the API endpoint
     * For local providers: Checks if model is loaded
     *
     * @return Result.Success with ProviderHealth if healthy, Result.Error otherwise
     */
    suspend fun checkHealth(): Result<ProviderHealth>

    /**
     * Estimate cost for a given number of tokens
     *
     * For cloud providers: Returns actual cost based on provider pricing
     * For local providers: Returns 0.0 (no cost)
     *
     * @param inputTokens Number of input tokens
     * @param outputTokens Number of output tokens
     * @return Estimated cost in USD
     */
    fun estimateCost(inputTokens: Int, outputTokens: Int): Double
}

/**
 * Configuration for LLM providers
 */
data class LLMConfig(
    /**
     * Path to the model (local providers) or API endpoint (cloud providers)
     */
    val modelPath: String,

    /**
     * Model library name (for MLC LLM)
     * Example: "llama-2-7b-chat-hf-q4f16_1"
     */
    val modelLib: String? = null,

    /**
     * API key for cloud providers
     */
    val apiKey: String? = null,

    /**
     * Device to use for inference (e.g., "opencl", "vulkan", "cpu")
     * Only applicable for local providers
     */
    val device: String = "opencl",

    /**
     * Maximum memory budget in MB
     * Only applicable for local providers
     */
    /**
     * Maximum memory budget in MB
     * Only applicable for local providers
     */
    val maxMemoryMB: Int = 2048,

    /**
     * Runtime to use (e.g., "MLC", "LiteRT", "GGUF")
     * null = auto-detect
     */
    val llmRuntime: String? = null
)

/**
 * Options for text generation
 */
data class GenerationOptions(
    /**
     * Temperature for sampling (0.0 = deterministic, 1.0 = creative)
     * Higher values produce more random outputs
     */
    val temperature: Float = 0.7f,

    /**
     * Maximum number of tokens to generate
     * null = use model's default limit
     */
    val maxTokens: Int? = null,

    /**
     * Top-p (nucleus) sampling threshold
     * Only tokens with cumulative probability <= top_p are considered
     */
    val topP: Float = 0.95f,

    /**
     * Frequency penalty (0.0 = no penalty, 2.0 = strong penalty)
     * Reduces repetition by penalizing frequently used tokens
     */
    val frequencyPenalty: Float = 0.0f,

    /**
     * Presence penalty (0.0 = no penalty, 2.0 = strong penalty)
     * Encourages topic diversity by penalizing tokens that already appeared
     */
    val presencePenalty: Float = 0.0f,

    /**
     * Stop sequences - generation halts when any of these strings are produced
     */
    val stopSequences: List<String> = emptyList(),

    /**
     * Random seed for reproducibility
     * null = non-deterministic
     */
    val seed: Int? = null
)

/**
 * Provider metadata
 */
data class LLMProviderInfo(
    /**
     * Provider name (e.g., "LocalLLM", "Gemini", "GPT-4")
     */
    val name: String,

    /**
     * Provider version
     */
    val version: String,

    /**
     * Model name (e.g., "Gemma-2B-INT4", "GPT-4-Turbo")
     */
    val modelName: String,

    /**
     * Whether this provider runs locally or in the cloud
     */
    val isLocal: Boolean,

    /**
     * Supported features
     */
    val capabilities: LLMCapabilities
)

/**
 * Provider capabilities
 */
data class LLMCapabilities(
    /**
     * Supports streaming responses
     */
    val supportsStreaming: Boolean = true,

    /**
     * Supports multi-turn conversations
     */
    val supportsChat: Boolean = true,

    /**
     * Supports function calling / tools
     */
    val supportsFunctionCalling: Boolean = false,

    /**
     * Maximum context length in tokens
     */
    val maxContextLength: Int = 2048
)

/**
 * Provider health status
 *
 * Tracks operational health of a provider for cascading fallback logic.
 */
data class ProviderHealth(
    /**
     * Health status
     */
    val status: HealthStatus,

    /**
     * Average latency in milliseconds (last 10 requests)
     * null if no requests have been made yet
     */
    val averageLatencyMs: Long? = null,

    /**
     * Error rate (0.0 to 1.0, last 100 requests)
     * null if no requests have been made yet
     */
    val errorRate: Double? = null,

    /**
     * Last error message (if status is UNHEALTHY)
     */
    val lastError: String? = null,

    /**
     * Timestamp when health was last checked
     */
    val lastChecked: Long = System.currentTimeMillis()
)

/**
 * Health status enum
 */
enum class HealthStatus {
    /**
     * Provider is healthy and operational
     */
    HEALTHY,

    /**
     * Provider is degraded (high latency or elevated error rate)
     */
    DEGRADED,

    /**
     * Provider is unhealthy (consecutive failures)
     */
    UNHEALTHY,

    /**
     * Provider status unknown (not yet initialized or checked)
     */
    UNKNOWN
}

/**
 * Provider type enum
 *
 * Identifies which cloud provider is being used.
 */
enum class ProviderType {
    /**
     * Local on-device LLM (MLC LLM with Gemma 2B)
     */
    LOCAL,

    /**
     * OpenRouter (aggregator with 100+ models)
     */
    OPENROUTER,

    /**
     * Anthropic Claude API
     */
    ANTHROPIC,

    /**
     * OpenAI GPT API
     */
    OPENAI,

    /**
     * HuggingFace Inference API
     */
    HUGGINGFACE,

    /**
     * Google Gemini API
     */
    GOOGLE_AI,

    /**
     * Cohere API (optional, future)
     */
    COHERE,

    /**
     * Together AI (optional, future)
     */
    TOGETHER_AI
}
