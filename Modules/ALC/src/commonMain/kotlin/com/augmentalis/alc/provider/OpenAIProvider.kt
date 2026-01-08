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
 * OpenAI GPT provider
 *
 * Supports GPT-4, GPT-4 Turbo, and GPT-3.5 models.
 */
class OpenAIProvider(
    config: ProviderConfig
) : BaseCloudProvider(config) {

    override val providerType = ProviderType.OPENAI

    override val capabilities = LLMCapabilities(
        streaming = true,
        functionCalling = true,
        vision = true,
        maxContextLength = 128000,
        supportedLanguages = listOf("en", "es", "fr", "de", "ja", "ko", "zh")
    )

    override val baseUrl = config.baseUrl ?: "https://api.openai.com"
    override val authHeader = "Authorization" to "Bearer ${config.apiKey ?: ""}"

    override fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        val request = OpenAIRequest(
            model = config.model,
            messages = messages.map { msg ->
                OpenAIMessage(
                    role = when (msg.role) {
                        MessageRole.SYSTEM -> "system"
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.FUNCTION -> "function"
                        MessageRole.TOOL -> "tool"
                    },
                    content = msg.content,
                    name = msg.name
                )
            },
            maxTokens = options.maxTokens,
            temperature = options.temperature,
            topP = options.topP,
            stream = options.stream,
            stop = options.stopSequences.takeIf { it.isNotEmpty() },
            frequencyPenalty = options.repetitionPenalty - 1.0f
        )

        if (options.stream) {
            streamRequest("/v1/chat/completions", request) { data ->
                parseStreamChunk(data)
            }.collect { emit(it) }
        } else {
            try {
                val response = client.post("$baseUrl/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${config.apiKey}")
                    setBody(request)
                }.body<OpenAIResponse>()

                val choice = response.choices.firstOrNull()
                emit(LLMResponse.Complete(
                    fullText = choice?.message?.content ?: "",
                    usage = TokenUsage(
                        promptTokens = response.usage?.promptTokens ?: 0,
                        completionTokens = response.usage?.completionTokens ?: 0
                    ),
                    model = response.model,
                    finishReason = choice?.finishReason ?: "stop"
                ))
            } catch (e: Exception) {
                emit(LLMResponse.Error(
                    message = e.message ?: "OpenAI API error",
                    code = "OPENAI_ERROR",
                    retryable = true
                ))
            }
        }
    }

    private fun parseStreamChunk(data: String): LLMResponse? {
        return try {
            val chunk = json.decodeFromString<OpenAIStreamChunk>(data)
            val delta = chunk.choices.firstOrNull()?.delta

            delta?.content?.let { content ->
                LLMResponse.Streaming(
                    chunk = content,
                    finishReason = chunk.choices.firstOrNull()?.finishReason
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getModels(): List<ModelInfo> = listOf(
        ModelInfo(
            id = "gpt-4o",
            name = "GPT-4o",
            provider = ProviderType.OPENAI,
            contextLength = 128000,
            capabilities = capabilities,
            costPerMillionTokens = 5.0f
        ),
        ModelInfo(
            id = "gpt-4-turbo",
            name = "GPT-4 Turbo",
            provider = ProviderType.OPENAI,
            contextLength = 128000,
            capabilities = capabilities,
            costPerMillionTokens = 10.0f
        ),
        ModelInfo(
            id = "gpt-3.5-turbo",
            name = "GPT-3.5 Turbo",
            provider = ProviderType.OPENAI,
            contextLength = 16384,
            capabilities = capabilities.copy(vision = false),
            costPerMillionTokens = 0.5f
        )
    )
}

// Request/Response models
@Serializable
private data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Float? = null,
    @SerialName("top_p") val topP: Float? = null,
    val stream: Boolean = false,
    val stop: List<String>? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Float? = null
)

@Serializable
private data class OpenAIMessage(
    val role: String,
    val content: String,
    val name: String? = null
)

@Serializable
private data class OpenAIResponse(
    val id: String,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage?
)

@Serializable
private data class OpenAIChoice(
    val index: Int,
    val message: OpenAIMessage,
    @SerialName("finish_reason") val finishReason: String?
)

@Serializable
private data class OpenAIUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)

@Serializable
private data class OpenAIStreamChunk(
    val id: String,
    val model: String,
    val choices: List<OpenAIStreamChoice>
)

@Serializable
private data class OpenAIStreamChoice(
    val index: Int,
    val delta: OpenAIDelta,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class OpenAIDelta(
    val role: String? = null,
    val content: String? = null
)
