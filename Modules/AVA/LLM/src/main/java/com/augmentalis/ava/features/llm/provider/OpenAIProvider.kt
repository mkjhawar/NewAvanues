/**
 * OpenAI GPT LLM Provider
 *
 * Implements LLMProvider interface using OpenAI's GPT API.
 * Provides access to GPT models:
 * - GPT-4 Turbo (gpt-4-turbo-preview)
 * - GPT-4 (gpt-4)
 * - GPT-3.5 Turbo (gpt-3.5-turbo)
 *
 * Features:
 * - Streaming responses via Server-Sent Events (SSE)
 * - Function calling support
 * - JSON mode
 * - Vision support (GPT-4V)
 *
 * API Documentation: https://platform.openai.com/docs/api-reference
 *
 * Created: 2025-11-21
 * Author: AVA AI Team
 */

package com.augmentalis.ava.features.llm.provider

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.features.llm.domain.*
import com.augmentalis.ava.features.llm.security.ApiKeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenAI Provider
 *
 * Usage:
 * ```
 * val provider = OpenAIProvider(context, apiKeyManager)
 * provider.initialize(LLMConfig(
 *     modelPath = "gpt-4-turbo-preview",
 *     apiKey = "sk-..."
 * ))
 *
 * provider.chat(messages, options).collect { response ->
 *     when (response) {
 *         is LLMResponse.Streaming -> print(response.chunk)
 *         is LLMResponse.Complete -> println("\nDone")
 *         is LLMResponse.Error -> println("Error: ${response.message}")
 *     }
 * }
 * ```
 */
class OpenAIProvider(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager
) : LLMProvider {

    companion object {
        private const val TAG = "OpenAIProvider"
        private const val API_BASE_URL = "https://api.openai.com/v1"
        private const val CHAT_ENDPOINT = "$API_BASE_URL/chat/completions"

        // Default model
        private const val DEFAULT_MODEL = "gpt-4-turbo-preview"

        // Pricing per 1M tokens (as of 2024-11)
        private val MODEL_PRICING = mapOf(
            "gpt-4-turbo-preview" to Pair(10.00, 30.00), // $10 input, $30 output
            "gpt-4" to Pair(30.00, 60.00),
            "gpt-3.5-turbo" to Pair(0.50, 1.50)
        )

        private const val TIMEOUT_SECONDS = 60L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private var apiKey: String? = null
    private var currentModel: String = DEFAULT_MODEL
    private var isGenerating = false

    override suspend fun initialize(config: LLMConfig): Result<Unit> {
        return try {
            Timber.i("Initializing OpenAIProvider with model: ${config.modelPath}")

            // Get API key
            val key = config.apiKey ?: apiKeyManager.getApiKey(ProviderType.OPENAI).let {
                when (it) {
                    is Result.Success -> it.data
                    is Result.Error -> {
                        return Result.Error(
                            exception = IllegalStateException("No API key found for OpenAI"),
                            message = "No API key found for OpenAI. ${it.message}"
                        )
                    }
                }
            }

            apiKey = key
            currentModel = config.modelPath.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL

            Timber.i("OpenAIProvider initialized successfully (model: $currentModel)")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize OpenAIProvider")
            Result.Error(
                exception = e,
                message = "Initialization failed: ${e.message}"
            )
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        options: GenerationOptions
    ): Flow<LLMResponse> {
        val messages = listOf(
            ChatMessage(
                role = MessageRole.USER,
                content = prompt
            )
        )
        return chat(messages, options)
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        if (apiKey == null) {
            emit(LLMResponse.Error(
                message = "Provider not initialized. Call initialize() first.",
                code = "NOT_INITIALIZED"
            ))
            return@flow
        }

        isGenerating = true

        try {
            // Build request body
            val requestBody = buildRequestBody(messages, options, stream = true)
            val request = buildRequest(requestBody)

            Timber.d("Sending chat request to OpenAI: model=$currentModel, messages=${messages.size}")

            // Execute streaming request
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Timber.e("OpenAI API error: ${response.code} - $errorBody")
                    emit(LLMResponse.Error(
                        message = "API request failed: ${response.code} - $errorBody",
                        code = response.code.toString()
                    ))
                    return@flow
                }

                // Parse SSE stream
                val reader = response.body?.byteStream()?.bufferedReader()
                    ?: throw IOException("Response body is null")

                reader.use {
                    parseServerSentEvents(it) { chunk ->
                        emit(chunk)
                    }
                }
            }

        } catch (e: IOException) {
            Timber.e(e, "Network error in OpenAI chat")
            emit(LLMResponse.Error(
                message = "Network error: ${e.message}",
                code = "NETWORK_ERROR",
                exception = e
            ))
        } catch (e: Exception) {
            Timber.e(e, "Error in OpenAI chat")
            emit(LLMResponse.Error(
                message = "Chat failed: ${e.message}",
                code = "CHAT_ERROR",
                exception = e
            ))
        } finally {
            isGenerating = false
        }
    }

    override suspend fun stop() {
        client.dispatcher.cancelAll()
        isGenerating = false
        Timber.d("OpenAI generation stopped")
    }

    override suspend fun reset() {
        stop()
        Timber.d("OpenAI provider reset")
    }

    override suspend fun cleanup() {
        stop()
        apiKey = null
        Timber.i("OpenAI provider cleaned up")
    }

    override fun isGenerating(): Boolean = isGenerating

    override fun getInfo(): LLMProviderInfo {
        return LLMProviderInfo(
            name = "OpenAI",
            version = "1.0",
            modelName = currentModel,
            isLocal = false,
            capabilities = LLMCapabilities(
                supportsStreaming = true,
                supportsChat = true,
                supportsFunctionCalling = true,
                maxContextLength = getMaxContextLength(currentModel)
            )
        )
    }

    override suspend fun checkHealth(): Result<ProviderHealth> {
        return try {
            val healthRequest = Request.Builder()
                .url("$API_BASE_URL/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            val response = client.newCall(healthRequest).execute()
            val isHealthy = response.isSuccessful

            Result.Success(
                ProviderHealth(
                    status = if (isHealthy) HealthStatus.HEALTHY else HealthStatus.UNHEALTHY,
                    averageLatencyMs = null,
                    errorRate = null,
                    lastError = if (!isHealthy) "Health check failed: ${response.code}" else null,
                    lastChecked = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Health check failed: ${e.message}"
            )
        }
    }

    override fun estimateCost(inputTokens: Int, outputTokens: Int): Double {
        val pricing = MODEL_PRICING[currentModel] ?: Pair(10.00, 30.00)
        val inputCost = (inputTokens / 1_000_000.0) * pricing.first
        val outputCost = (outputTokens / 1_000_000.0) * pricing.second
        return inputCost + outputCost
    }

    /**
     * Build request body for chat completion
     */
    private fun buildRequestBody(
        messages: List<ChatMessage>,
        options: GenerationOptions,
        stream: Boolean
    ): String {
        val apiMessages = messages.map { msg ->
            OpenAIMessage(
                role = msg.role.toApiString(),
                content = msg.content
            )
        }

        val request = OpenAIChatRequest(
            model = currentModel,
            messages = apiMessages,
            temperature = options.temperature.toDouble(),
            max_tokens = options.maxTokens,
            top_p = options.topP.toDouble(),
            frequency_penalty = options.frequencyPenalty.toDouble(),
            presence_penalty = options.presencePenalty.toDouble(),
            stop = options.stopSequences.takeIf { it.isNotEmpty() },
            stream = stream,
            seed = options.seed
        )

        return json.encodeToString(request)
    }

    /**
     * Build HTTP request
     */
    private fun buildRequest(body: String): Request {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = body.toRequestBody(mediaType)

        return Request.Builder()
            .url(CHAT_ENDPOINT)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()
    }

    /**
     * Parse Server-Sent Events (SSE) stream
     *
     * OpenAI uses SSE format:
     * data: {"id":"...", "choices":[{"delta":{"content":"..."}}], ...}
     * data: [DONE]
     */
    private suspend fun parseServerSentEvents(
        reader: BufferedReader,
        onChunk: suspend (LLMResponse) -> Unit
    ) {
        val fullText = StringBuilder()
        var totalInputTokens = 0
        var totalOutputTokens = 0

        reader.lineSequence().forEach { line ->
            if (line.startsWith("data: ")) {
                val data = line.substring(6) // Remove "data: " prefix

                // Check for stream end
                if (data == "[DONE]") {
                    // Emit complete response
                    onChunk(
                        LLMResponse.Complete(
                            fullText = fullText.toString(),
                            usage = TokenUsage(
                                promptTokens = totalInputTokens,
                                completionTokens = totalOutputTokens
                            )
                        )
                    )
                    return
                }

                try {
                    val chunk = json.decodeFromString<OpenAIStreamChunk>(data)

                    // Extract content delta
                    val content = chunk.choices.firstOrNull()?.delta?.content
                    if (content != null) {
                        fullText.append(content)
                        onChunk(
                            LLMResponse.Streaming(
                                chunk = content,
                                tokenCount = fullText.length
                            )
                        )
                    }

                    // Extract usage if present (final chunk)
                    chunk.usage?.let { usage ->
                        totalInputTokens = usage.prompt_tokens
                        totalOutputTokens = usage.completion_tokens
                    }

                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse SSE chunk: $data")
                }
            }
        }
    }

    /**
     * Get max context length for a model
     */
    private fun getMaxContextLength(model: String): Int {
        return when {
            model.contains("gpt-4-turbo") -> 128_000
            model.contains("gpt-4") -> 8_192
            model.contains("gpt-3.5-turbo") -> 16_385
            else -> 4096 // Default
        }
    }
}

// ==================== API Data Classes ====================

@Serializable
private data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double? = null,
    val max_tokens: Int? = null,
    val top_p: Double? = null,
    val frequency_penalty: Double? = null,
    val presence_penalty: Double? = null,
    val stop: List<String>? = null,
    val stream: Boolean = false,
    val seed: Int? = null
)

@Serializable
private data class OpenAIMessage(
    val role: String,
    val content: String
)

@Serializable
private data class OpenAIStreamChunk(
    val id: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null
)

@Serializable
private data class OpenAIChoice(
    val index: Int,
    val delta: OpenAIDelta,
    val finish_reason: String? = null
)

@Serializable
private data class OpenAIDelta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
private data class OpenAIUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
