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
 * Anthropic Claude provider
 *
 * Supports Claude 3, 3.5, and 4 models.
 */
class AnthropicProvider(
    config: ProviderConfig
) : BaseCloudProvider(config) {

    override val providerType = ProviderType.ANTHROPIC

    override val capabilities = LLMCapabilities(
        streaming = true,
        functionCalling = true,
        vision = true,
        maxContextLength = 200000,
        supportedLanguages = listOf("en", "es", "fr", "de", "ja", "ko", "zh")
    )

    override val baseUrl = config.baseUrl ?: "https://api.anthropic.com"
    override val authHeader = "x-api-key" to (config.apiKey ?: "")

    override fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        val systemMessage = messages.firstOrNull { it.role == MessageRole.SYSTEM }?.content
        val chatMessages = messages.filter { it.role != MessageRole.SYSTEM }

        val request = AnthropicRequest(
            model = config.model,
            maxTokens = options.maxTokens,
            system = systemMessage,
            messages = chatMessages.map { msg ->
                AnthropicMessage(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        else -> "user"
                    },
                    content = msg.content
                )
            },
            stream = options.stream,
            temperature = options.temperature,
            topP = options.topP,
            stopSequences = options.stopSequences.takeIf { it.isNotEmpty() }
        )

        if (options.stream) {
            streamRequest("/v1/messages", request) { data ->
                parseStreamChunk(data)
            }.collect { emit(it) }
        } else {
            try {
                val response = client.post("$baseUrl/v1/messages") {
                    contentType(ContentType.Application.Json)
                    header("x-api-key", config.apiKey)
                    header("anthropic-version", "2023-06-01")
                    setBody(request)
                }.body<AnthropicResponse>()

                emit(LLMResponse.Complete(
                    fullText = response.content.firstOrNull()?.text ?: "",
                    usage = TokenUsage(
                        promptTokens = response.usage.inputTokens,
                        completionTokens = response.usage.outputTokens
                    ),
                    model = response.model,
                    finishReason = response.stopReason ?: "stop"
                ))
            } catch (e: Exception) {
                emit(LLMResponse.Error(
                    message = e.message ?: "Anthropic API error",
                    code = "ANTHROPIC_ERROR",
                    retryable = true
                ))
            }
        }
    }

    private fun parseStreamChunk(data: String): LLMResponse? {
        return try {
            val event = json.decodeFromString<AnthropicStreamEvent>(data)
            when (event.type) {
                "content_block_delta" -> {
                    event.delta?.text?.let { text ->
                        LLMResponse.Streaming(chunk = text)
                    }
                }
                "message_stop" -> {
                    LLMResponse.Complete(
                        fullText = "",
                        usage = TokenUsage(0, 0),
                        finishReason = "stop"
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getModels(): List<ModelInfo> = listOf(
        ModelInfo(
            id = "claude-3-5-sonnet-20241022",
            name = "Claude 3.5 Sonnet",
            provider = ProviderType.ANTHROPIC,
            contextLength = 200000,
            capabilities = capabilities,
            costPerMillionTokens = 3.0f
        ),
        ModelInfo(
            id = "claude-3-opus-20240229",
            name = "Claude 3 Opus",
            provider = ProviderType.ANTHROPIC,
            contextLength = 200000,
            capabilities = capabilities,
            costPerMillionTokens = 15.0f
        ),
        ModelInfo(
            id = "claude-3-5-haiku-20241022",
            name = "Claude 3.5 Haiku",
            provider = ProviderType.ANTHROPIC,
            contextLength = 200000,
            capabilities = capabilities,
            costPerMillionTokens = 0.25f
        )
    )
}

// Request/Response models
@Serializable
private data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String? = null,
    val messages: List<AnthropicMessage>,
    val stream: Boolean = false,
    val temperature: Float? = null,
    @SerialName("top_p") val topP: Float? = null,
    @SerialName("stop_sequences") val stopSequences: List<String>? = null
)

@Serializable
private data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
private data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<AnthropicContent>,
    val model: String,
    @SerialName("stop_reason") val stopReason: String?,
    val usage: AnthropicUsage
)

@Serializable
private data class AnthropicContent(
    val type: String,
    val text: String
)

@Serializable
private data class AnthropicUsage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)

@Serializable
private data class AnthropicStreamEvent(
    val type: String,
    val delta: AnthropicDelta? = null
)

@Serializable
private data class AnthropicDelta(
    val type: String? = null,
    val text: String? = null
)
