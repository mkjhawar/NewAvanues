package com.augmentalis.alc.provider

import com.augmentalis.alc.domain.ProviderConfig
import com.augmentalis.alc.domain.ProviderType
import com.augmentalis.alc.engine.ILLMProvider

/**
 * Factory for creating LLM providers
 */
object LLMProviderFactory {

    /**
     * Create a cloud provider based on configuration
     */
    fun createCloudProvider(config: ProviderConfig): ILLMProvider {
        return when (config.type) {
            ProviderType.ANTHROPIC -> AnthropicProvider(config)
            ProviderType.OPENAI -> OpenAIProvider(config)
            ProviderType.GROQ -> GroqProvider(config)
            ProviderType.OPENROUTER -> OpenRouterProvider(config)
            ProviderType.GOOGLE_AI -> GoogleAIProvider(config)
            ProviderType.HUGGINGFACE -> HuggingFaceProvider(config)
            else -> throw IllegalArgumentException("Unsupported cloud provider: ${config.type}")
        }
    }

    /**
     * Create provider with fallback chain
     */
    fun createWithFallback(
        primary: ProviderConfig,
        fallbacks: List<ProviderConfig>
    ): FallbackProvider {
        return FallbackProvider(
            primary = createCloudProvider(primary),
            fallbacks = fallbacks.map { createCloudProvider(it) }
        )
    }
}

/**
 * Provider with automatic fallback on failure
 */
class FallbackProvider(
    private val primary: ILLMProvider,
    private val fallbacks: List<ILLMProvider>
) : ILLMProvider by primary {

    private val allProviders = listOf(primary) + fallbacks

    override fun chat(
        messages: List<com.augmentalis.alc.domain.ChatMessage>,
        options: com.augmentalis.alc.domain.GenerationOptions
    ) = kotlinx.coroutines.flow.flow {
        var lastError: Exception? = null
        var success = false

        for (provider in allProviders) {
            if (success) break
            try {
                var providerFailed = false
                provider.chat(messages, options).collect { response ->
                    when (response) {
                        is com.augmentalis.alc.domain.LLMResponse.Error -> {
                            if (!response.retryable) {
                                emit(response)
                                success = true
                            } else {
                                lastError = Exception(response.message)
                                providerFailed = true
                            }
                        }
                        else -> {
                            emit(response)
                            if (response is com.augmentalis.alc.domain.LLMResponse.Complete) {
                                success = true
                            }
                        }
                    }
                }
                if (!providerFailed && !success) success = true
            } catch (e: Exception) {
                lastError = e
                continue
            }
        }

        if (!success) {
            emit(com.augmentalis.alc.domain.LLMResponse.Error(
                message = lastError?.message ?: "All providers failed",
                code = "FALLBACK_EXHAUSTED",
                retryable = false
            ))
        }
    }
}
