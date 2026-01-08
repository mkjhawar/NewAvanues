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
 * HuggingFace Inference API provider - full implementation
 */
class HuggingFaceProvider(config: ProviderConfig) : BaseCloudProvider(config) {

    override val providerType = ProviderType.HUGGINGFACE
    override val baseUrl = config.baseUrl ?: "https://api-inference.huggingface.co"
    override val authHeader = "Authorization" to "Bearer ${config.apiKey ?: ""}"

    override val capabilities = LLMCapabilities(
        streaming = true,
        functionCalling = false,
        vision = false,
        maxContextLength = 32768
    )

    override fun chat(messages: List<ChatMessage>, options: GenerationOptions): Flow<LLMResponse> = flow {
        // Convert messages to prompt format for HF models
        val prompt = buildPrompt(messages)

        val request = HFRequest(
            inputs = prompt,
            parameters = HFParameters(
                maxNewTokens = options.maxTokens,
                temperature = options.temperature,
                topP = options.topP,
                topK = options.topK,
                repetitionPenalty = options.repetitionPenalty,
                doSample = options.temperature > 0,
                returnFullText = false
            ),
            stream = options.stream
        )

        try {
            if (options.stream) {
                streamRequest("/models/${config.model}", request) { parseChunk(it) }.collect { emit(it) }
            } else {
                val response = client.post("$baseUrl/models/${config.model}") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${config.apiKey}")
                    setBody(request)
                }.body<List<HFResponse>>()

                val text = response.firstOrNull()?.generatedText ?: ""
                emit(LLMResponse.Complete(
                    fullText = text,
                    usage = TokenUsage(prompt.split(" ").size, text.split(" ").size), // Approximate
                    model = config.model
                ))
            }
        } catch (e: Exception) {
            emit(LLMResponse.Error(e.message ?: "HuggingFace error", "HF_ERROR", true))
        }
    }

    private fun buildPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        for (msg in messages) {
            when (msg.role) {
                MessageRole.SYSTEM -> sb.append("<|system|>\n${msg.content}\n")
                MessageRole.USER -> sb.append("<|user|>\n${msg.content}\n")
                MessageRole.ASSISTANT -> sb.append("<|assistant|>\n${msg.content}\n")
                else -> sb.append("${msg.content}\n")
            }
        }
        sb.append("<|assistant|>\n")
        return sb.toString()
    }

    private fun parseChunk(data: String): LLMResponse? = try {
        val chunk = json.decodeFromString<HFStreamChunk>(data)
        chunk.token?.text?.let { LLMResponse.Streaming(it) }
    } catch (e: Exception) { null }

    override suspend fun getModels() = listOf(
        ModelInfo("meta-llama/Llama-3.1-70B-Instruct", "Llama 3.1 70B", ProviderType.HUGGINGFACE, 128000, capabilities, 0.0f),
        ModelInfo("mistralai/Mixtral-8x7B-Instruct-v0.1", "Mixtral 8x7B", ProviderType.HUGGINGFACE, 32768, capabilities, 0.0f),
        ModelInfo("microsoft/Phi-3-medium-128k-instruct", "Phi-3 Medium", ProviderType.HUGGINGFACE, 128000, capabilities, 0.0f)
    )
}

@Serializable private data class HFRequest(val inputs: String, val parameters: HFParameters, val stream: Boolean = false)
@Serializable private data class HFParameters(
    @SerialName("max_new_tokens") val maxNewTokens: Int,
    val temperature: Float,
    @SerialName("top_p") val topP: Float,
    @SerialName("top_k") val topK: Int,
    @SerialName("repetition_penalty") val repetitionPenalty: Float,
    @SerialName("do_sample") val doSample: Boolean,
    @SerialName("return_full_text") val returnFullText: Boolean
)
@Serializable private data class HFResponse(@SerialName("generated_text") val generatedText: String)
@Serializable private data class HFStreamChunk(val token: HFToken? = null)
@Serializable private data class HFToken(val text: String)
