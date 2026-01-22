package com.augmentalis.alc.domain

import kotlinx.serialization.Serializable

/**
 * Supported LLM provider types
 */
@Serializable
enum class ProviderType {
    // Cloud providers
    ANTHROPIC,
    OPENAI,
    GOOGLE_AI,
    OPENROUTER,
    GROQ,
    HUGGINGFACE,

    // Local providers
    LOCAL_TVM,      // Android TVM
    LOCAL_COREML,   // iOS Core ML
    LOCAL_ONNX,     // Desktop ONNX Runtime
    LOCAL_LLAMA_CPP // Desktop llama.cpp
}

/**
 * Provider health status
 */
@Serializable
enum class HealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    UNKNOWN
}

/**
 * Provider health information
 */
@Serializable
data class ProviderHealth(
    val provider: ProviderType,
    val status: HealthStatus,
    val latencyMs: Long? = null,
    val lastCheck: Long? = null,
    val errorMessage: String? = null
)

/**
 * LLM provider capabilities
 */
@Serializable
data class LLMCapabilities(
    val streaming: Boolean = true,
    val functionCalling: Boolean = false,
    val vision: Boolean = false,
    val maxContextLength: Int = 4096,
    val supportedLanguages: List<String> = listOf("en")
)

/**
 * Provider configuration
 */
@Serializable
data class ProviderConfig(
    val type: ProviderType,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val model: String,
    val timeout: Long = 30000,
    val maxRetries: Int = 3
)

/**
 * Model information
 */
@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val provider: ProviderType,
    val contextLength: Int,
    val capabilities: LLMCapabilities,
    val costPerMillionTokens: Float? = null
)
