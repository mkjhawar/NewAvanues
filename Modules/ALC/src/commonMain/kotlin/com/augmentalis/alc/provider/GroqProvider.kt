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
 * Groq LPU provider
 *
 * Ultra-fast inference using Groq's LPU hardware.
 * Supports Llama, Mixtral, and Gemma models.
 */
class GroqProvider(
    config: ProviderConfig
) : BaseCloudProvider(config) {

    override val providerType = ProviderType.GROQ

    override val capabilities = LLMCapabilities(
        streaming = true,
        functionCalling = true,
        vision = false,
        maxContextLength = 32768,
        supportedLanguages = listOf("en", "es", "fr", "de", "zh")
    )

    override val baseUrl = config.baseUrl ?: "https://api.groq.com/openai"
    override val authHeader = "Authorization" to "Bearer ${config.apiKey ?: ""}"

    override fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        val request = GroqRequest(
            model = config.model,
            messages = messages.map { msg ->
                GroqMessage(
                    role = when (msg.role) {
                        MessageRole.SYSTEM -> "system"
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        else -> "user"
                    },
                    content = msg.content
                )
            },
            maxTokens = options.maxTokens,
            temperature = options.temperature,
            topP = options.topP,
            stream = options.stream,
            stop = options.stopSequences.takeIf { it.isNotEmpty() }
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
                }.body<GroqResponse>()

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
                    message = e.message ?: "Groq API error",
                    code = "GROQ_ERROR",
                    retryable = true
                ))
            }
        }
    }

    private fun parseStreamChunk(data: String): LLMResponse? {
        return try {
            val chunk = json.decodeFromString<GroqStreamChunk>(data)
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
            id = "llama-3.1-70b-versatile",
            name = "Llama 3.1 70B",
            provider = ProviderType.GROQ,
            contextLength = 32768,
            capabilities = capabilities,
            costPerMillionTokens = 0.59f
        ),
        ModelInfo(
            id = "llama-3.1-8b-instant",
            name = "Llama 3.1 8B",
            provider = ProviderType.GROQ,
            contextLength = 32768,
            capabilities = capabilities,
            costPerMillionTokens = 0.05f
        ),
        ModelInfo(
            id = "mixtral-8x7b-32768",
            name = "Mixtral 8x7B",
            provider = ProviderType.GROQ,
            contextLength = 32768,
            capabilities = capabilities,
            costPerMillionTokens = 0.24f
        ),
        ModelInfo(
            id = "gemma2-9b-it",
            name = "Gemma 2 9B",
            provider = ProviderType.GROQ,
            contextLength = 8192,
            capabilities = capabilities,
            costPerMillionTokens = 0.20f
        )
    )
}

// Request/Response models (OpenAI-compatible)
@Serializable
private data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Float? = null,
    @SerialName("top_p") val topP: Float? = null,
    val stream: Boolean = false,
    val stop: List<String>? = null
)

@Serializable
private data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
private data class GroqResponse(
    val id: String,
    val model: String,
    val choices: List<GroqChoice>,
    val usage: GroqUsage?
)

@Serializable
private data class GroqChoice(
    val index: Int,
    val message: GroqMessage,
    @SerialName("finish_reason") val finishReason: String?
)

@Serializable
private data class GroqUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int
)

@Serializable
private data class GroqStreamChunk(
    val id: String,
    val model: String,
    val choices: List<GroqStreamChoice>
)

@Serializable
private data class GroqStreamChoice(
    val index: Int,
    val delta: GroqDelta,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class GroqDelta(
    val role: String? = null,
    val content: String? = null
)
