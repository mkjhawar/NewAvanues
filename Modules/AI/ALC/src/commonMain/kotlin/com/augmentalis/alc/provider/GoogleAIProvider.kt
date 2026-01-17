package com.augmentalis.alc.provider

import com.augmentalis.alc.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Google AI (Gemini) provider - full implementation
 */
class GoogleAIProvider(config: ProviderConfig) : BaseCloudProvider(config) {

    override val providerType = ProviderType.GOOGLE_AI
    override val baseUrl = config.baseUrl ?: "https://generativelanguage.googleapis.com/v1beta"
    override val authHeader = "x-goog-api-key" to (config.apiKey ?: "")

    override val capabilities = LLMCapabilities(
        streaming = true,
        functionCalling = true,
        vision = true,
        maxContextLength = 1000000
    )

    override fun chat(messages: List<ChatMessage>, options: GenerationOptions): Flow<LLMResponse> = flow {
        val systemInstruction = messages.firstOrNull { it.role == MessageRole.SYSTEM }?.content
        val contents = messages.filter { it.role != MessageRole.SYSTEM }.map { msg ->
            GeminiContent(
                role = if (msg.role == MessageRole.USER) "user" else "model",
                parts = listOf(GeminiPart(msg.content))
            )
        }

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction?.let { GeminiContent(parts = listOf(GeminiPart(it))) },
            generationConfig = GeminiGenConfig(options.maxTokens, options.temperature, options.topP, options.topK)
        )

        try {
            val endpoint = if (options.stream) "streamGenerateContent" else "generateContent"
            val response = client.post("$baseUrl/models/${config.model}:$endpoint") {
                contentType(ContentType.Application.Json)
                parameter("key", config.apiKey)
                setBody(request)
            }.body<GeminiResponse>()

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            emit(LLMResponse.Complete(
                fullText = text,
                usage = TokenUsage(
                    response.usageMetadata?.promptTokenCount ?: 0,
                    response.usageMetadata?.candidatesTokenCount ?: 0
                ),
                model = config.model,
                finishReason = response.candidates?.firstOrNull()?.finishReason ?: "STOP"
            ))
        } catch (e: Exception) {
            emit(LLMResponse.Error(e.message ?: "Google AI error", "GOOGLE_AI_ERROR", true))
        }
    }

    override suspend fun getModels() = listOf(
        ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", ProviderType.GOOGLE_AI, 1000000, capabilities, 3.5f),
        ModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", ProviderType.GOOGLE_AI, 1000000, capabilities, 0.075f),
        ModelInfo("gemini-2.0-flash-exp", "Gemini 2.0 Flash", ProviderType.GOOGLE_AI, 1000000, capabilities, 0.0f)
    )
}

@Serializable private data class GeminiRequest(val contents: List<GeminiContent>, val systemInstruction: GeminiContent? = null, val generationConfig: GeminiGenConfig? = null)
@Serializable private data class GeminiContent(val role: String? = null, val parts: List<GeminiPart>)
@Serializable private data class GeminiPart(val text: String)
@Serializable private data class GeminiGenConfig(val maxOutputTokens: Int?, val temperature: Float?, val topP: Float?, val topK: Int?)
@Serializable private data class GeminiResponse(val candidates: List<GeminiCandidate>? = null, val usageMetadata: GeminiUsage? = null)
@Serializable private data class GeminiCandidate(val content: GeminiContent? = null, val finishReason: String? = null)
@Serializable private data class GeminiUsage(val promptTokenCount: Int? = null, val candidatesTokenCount: Int? = null)
