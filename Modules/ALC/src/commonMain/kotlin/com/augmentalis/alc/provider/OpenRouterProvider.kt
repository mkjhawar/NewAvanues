package com.augmentalis.alc.provider

import com.augmentalis.alc.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * OpenRouter multi-model gateway provider
 */
class OpenRouterProvider(config: ProviderConfig) : BaseCloudProvider(config) {

    override val providerType = ProviderType.OPENROUTER
    override val baseUrl = config.baseUrl ?: "https://openrouter.ai/api"
    override val authHeader = "Authorization" to "Bearer ${config.apiKey ?: ""}"

    override val capabilities = LLMCapabilities(
        streaming = true,
        functionCalling = true,
        vision = true,
        maxContextLength = 128000
    )

    override fun chat(messages: List<ChatMessage>, options: GenerationOptions): Flow<LLMResponse> = flow {
        val request = mapOf(
            "model" to config.model,
            "messages" to messages.map { mapOf("role" to it.role.name.lowercase(), "content" to it.content) },
            "max_tokens" to options.maxTokens,
            "temperature" to options.temperature,
            "stream" to options.stream
        )

        if (options.stream) {
            streamRequest("/v1/chat/completions", request) { parseChunk(it) }.collect { emit(it) }
        } else {
            try {
                val response = client.post("$baseUrl/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${config.apiKey}")
                    setBody(request)
                }.body<OpenRouterResponse>()

                emit(LLMResponse.Complete(
                    fullText = response.choices.firstOrNull()?.message?.content ?: "",
                    usage = TokenUsage(response.usage?.promptTokens ?: 0, response.usage?.completionTokens ?: 0),
                    model = response.model
                ))
            } catch (e: Exception) {
                emit(LLMResponse.Error(e.message ?: "OpenRouter error", "OPENROUTER_ERROR", true))
            }
        }
    }

    private fun parseChunk(data: String): LLMResponse? = try {
        json.decodeFromString<OpenRouterStreamChunk>(data).choices.firstOrNull()?.delta?.content?.let {
            LLMResponse.Streaming(it)
        }
    } catch (e: Exception) { null }

    override suspend fun getModels() = listOf(
        ModelInfo("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", ProviderType.OPENROUTER, 200000, capabilities, 3.0f),
        ModelInfo("openai/gpt-4o", "GPT-4o", ProviderType.OPENROUTER, 128000, capabilities, 5.0f),
        ModelInfo("meta-llama/llama-3.1-405b", "Llama 3.1 405B", ProviderType.OPENROUTER, 128000, capabilities, 2.7f)
    )
}

@Serializable private data class OpenRouterResponse(val model: String, val choices: List<ORChoice>, val usage: ORUsage?)
@Serializable private data class ORChoice(val message: ORMessage, @SerialName("finish_reason") val finishReason: String?)
@Serializable private data class ORMessage(val role: String, val content: String)
@Serializable private data class ORUsage(@SerialName("prompt_tokens") val promptTokens: Int, @SerialName("completion_tokens") val completionTokens: Int)
@Serializable private data class OpenRouterStreamChunk(val choices: List<ORStreamChoice>)
@Serializable private data class ORStreamChoice(val delta: ORDelta)
@Serializable private data class ORDelta(val content: String? = null)
