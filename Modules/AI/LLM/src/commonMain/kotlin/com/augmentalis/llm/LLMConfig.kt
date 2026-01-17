package com.augmentalis.llm

import kotlinx.serialization.Serializable

/**
 * Configuration for LLM providers
 */
@Serializable
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
    val device: String = "cpu",

    /**
     * Maximum memory budget in MB
     * Only applicable for local providers
     */
    val maxMemoryMB: Int = 2048,

    /**
     * Runtime to use (e.g., "MLC", "LiteRT", "GGUF", "ONNX", "Ollama")
     * null = auto-detect
     */
    val llmRuntime: String? = null,

    /**
     * Base URL for HTTP-based providers (e.g., Ollama)
     */
    val baseUrl: String? = null
)

/**
 * Options for text generation
 */
@Serializable
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
@Serializable
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
@Serializable
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
@Serializable
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
    val lastChecked: Long = currentTimeMillis()
)

/**
 * Health status enum
 */
@Serializable
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
 * Identifies which LLM provider is being used.
 */
@Serializable
enum class ProviderType {
    /**
     * Local on-device LLM (MLC LLM, GGUF, ONNX)
     */
    LOCAL,

    /**
     * Ollama local server
     */
    OLLAMA,

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
