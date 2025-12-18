/**
 * OpenRouter LLM Provider
 *
 * Implements LLMProvider interface using OpenRouter API.
 * OpenRouter provides access to 100+ LLM models through a single API:
 * - GPT-4 Turbo, GPT-3.5
 * - Claude 3.5 Sonnet, Claude 3 Opus
 * - Llama 3.1 (8B, 70B, 405B)
 * - Gemini Pro 1.5
 * - And many more
 *
 * Features:
 * - Streaming responses via Server-Sent Events (SSE)
 * - Cost tracking (OpenRouter returns token usage)
 * - Health checks
 * - Rate limit handling
 *
 * API Documentation: https://openrouter.ai/docs
 *
 * Created: 2025-11-03
 * Author: AVA AI Team
 */

package com.augmentalis.llm.provider

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.llm.domain.*
import com.augmentalis.llm.security.ApiKeyManager
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
 * OpenRouter Provider
 *
 * Usage:
 * ```
 * val provider = OpenRouterProvider(context, apiKeyManager)
 * provider.initialize(LLMConfig(
 *     modelPath = "anthropic/claude-3.5-sonnet",
 *     apiKey = "sk-or-..."
 * ))
 *
 * provider.chat(messages, options).collect { response ->
 *     when (response) {
 *         is LLMResponse.Streaming -> print(response.chunk)
 *         is LLMResponse.Complete -> println("\nDone: ${response.usage}")
 *         is LLMResponse.Error -> println("Error: ${response.message}")
 *     }
 * }
 * ```
 */
class OpenRouterProvider(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager
) : LLMProvider {

    companion object {
        private const val TAG = "OpenRouterProvider"
        private const val API_BASE_URL = "https://openrouter.ai/api/v1"
        private const val CHAT_ENDPOINT = "$API_BASE_URL/chat/completions"

        // Default model (Claude 3.5 Sonnet via OpenRouter)
        private const val DEFAULT_MODEL = "anthropic/claude-3.5-sonnet"

        // Pricing per 1M tokens (approximate, subject to change)
        // See https://openrouter.ai/models for current pricing
        private val MODEL_PRICING = mapOf(
            "anthropic/claude-3.5-sonnet" to Pair(3.00, 15.00), // $3 input, $15 output per 1M tokens
            "openai/gpt-4-turbo" to Pair(10.00, 30.00),
            "meta-llama/llama-3.1-8b-instruct" to Pair(0.10, 0.10),
            "google/gemini-pro-1.5" to Pair(2.50, 10.00)
        )

        private const val TIMEOUT_SECONDS = 60L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
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
            Timber.i("Initializing OpenRouterProvider with model: ${config.modelPath}")

            // Get API key (priority: config > ApiKeyManager)
            val key = config.apiKey ?: apiKeyManager.getApiKey(ProviderType.OPENROUTER).let {
                when (it) {
                    is Result.Success -> it.data
                    is Result.Error -> {
                        return Result.Error(
                            exception = IllegalStateException("No API key found for OpenRouter"),
                            message = "No API key found for OpenRouter. ${it.message}"
                        )
                    }
                }
            }

            apiKey = key
            currentModel = config.modelPath.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL

            Timber.i("OpenRouterProvider initialized successfully (model: $currentModel)")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize OpenRouterProvider")
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

            Timber.d("Sending chat request to OpenRouter: model=$currentModel, messages=${messages.size}")

            // Execute streaming request
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Timber.e("OpenRouter API error: ${response.code} - $errorBody")
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
            Timber.e(e, "Network error in OpenRouter chat")
            emit(LLMResponse.Error(
                message = "Network error: ${e.message}",
                code = "NETWORK_ERROR",
                exception = e
            ))
        } catch (e: Exception) {
            Timber.e(e, "Error in OpenRouter chat")
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
        // Cancel ongoing request
        client.dispatcher.cancelAll()
        isGenerating = false
        Timber.d("OpenRouter generation stopped")
    }

    override suspend fun reset() {
        stop()
        Timber.d("OpenRouter provider reset")
    }

    override suspend fun cleanup() {
        stop()
        apiKey = null
        Timber.i("OpenRouter provider cleaned up")
    }

    override fun isGenerating(): Boolean = isGenerating

    override fun getInfo(): LLMProviderInfo {
        return LLMProviderInfo(
            name = "OpenRouter",
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
                    averageLatencyMs = null, // TODO: Track latency
                    errorRate = null, // TODO: Track error rate
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
        val pricing = MODEL_PRICING[currentModel] ?: Pair(3.00, 15.00) // Default to Claude pricing
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
            OpenRouterMessage(
                role = msg.role.toApiString(),
                content = msg.content
            )
        }

        val request = OpenRouterChatRequest(
            model = currentModel,
            messages = apiMessages,
            temperature = options.temperature.toDouble(),
            max_tokens = options.maxTokens,
            top_p = options.topP.toDouble(),
            frequency_penalty = options.frequencyPenalty.toDouble(),
            presence_penalty = options.presencePenalty.toDouble(),
            stop = options.stopSequences.takeIf { it.isNotEmpty() },
            stream = stream
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
            .header("HTTP-Referer", "https://ava-ai.app") // Required by OpenRouter
            .header("X-Title", "AVA AI Assistant") // Optional, for usage tracking
            .post(requestBody)
            .build()
    }

    /**
     * Parse Server-Sent Events (SSE) stream
     *
     * OpenRouter uses SSE format:
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
                    val chunk = json.decodeFromString<OpenRouterStreamChunk>(data)

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
            model.contains("claude-3.5") -> 200_000
            model.contains("gpt-4") -> 128_000
            model.contains("llama-3.1") -> 131_072
            model.contains("gemini-pro-1.5") -> 1_000_000
            else -> 4096 // Default
        }
    }
}

// ==================== API Data Classes ====================

@Serializable
private data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Double? = null,
    val max_tokens: Int? = null,
    val top_p: Double? = null,
    val frequency_penalty: Double? = null,
    val presence_penalty: Double? = null,
    val stop: List<String>? = null,
    val stream: Boolean = false
)

@Serializable
private data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
private data class OpenRouterStreamChunk(
    val id: String,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage? = null
)

@Serializable
private data class OpenRouterChoice(
    val index: Int,
    val delta: OpenRouterDelta,
    val finish_reason: String? = null
)

@Serializable
private data class OpenRouterDelta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
private data class OpenRouterUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
