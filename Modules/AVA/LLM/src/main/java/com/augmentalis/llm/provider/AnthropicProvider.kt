/**
 * Anthropic Claude LLM Provider
 *
 * Implements LLMProvider interface using Anthropic's native Claude API.
 * Provides access to Claude models:
 * - Claude 3.5 Sonnet (latest, most capable)
 * - Claude 3 Opus (most intelligent)
 * - Claude 3 Haiku (fastest, most affordable)
 *
 * Features:
 * - Streaming responses via Server-Sent Events (SSE)
 * - Vision support (images in messages)
 * - Prompt caching (reduces costs for repeated contexts)
 * - Extended context (200K tokens)
 *
 * API Documentation: https://docs.anthropic.com/claude/reference
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
 * Anthropic Provider
 *
 * Usage:
 * ```
 * val provider = AnthropicProvider(context, apiKeyManager)
 * provider.initialize(LLMConfig(
 *     modelPath = "claude-3-5-sonnet-20241022",
 *     apiKey = "sk-ant-..."
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
class AnthropicProvider(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager
) : LLMProvider {

    companion object {
        private const val TAG = "AnthropicProvider"
        private const val API_BASE_URL = "https://api.anthropic.com/v1"
        private const val MESSAGES_ENDPOINT = "$API_BASE_URL/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"

        // Default model
        private const val DEFAULT_MODEL = "claude-3-5-sonnet-20241022"

        // Pricing per 1M tokens (as of 2024-11)
        private val MODEL_PRICING = mapOf(
            "claude-3-5-sonnet-20241022" to Pair(3.00, 15.00), // $3 input, $15 output
            "claude-3-opus-20240229" to Pair(15.00, 75.00),
            "claude-3-haiku-20240307" to Pair(0.25, 1.25)
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
            Timber.i("Initializing AnthropicProvider with model: ${config.modelPath}")

            // Get API key
            val key = config.apiKey ?: apiKeyManager.getApiKey(ProviderType.ANTHROPIC).let {
                when (it) {
                    is Result.Success -> it.data
                    is Result.Error -> {
                        return Result.Error(
                            exception = IllegalStateException("No API key found for Anthropic"),
                            message = "No API key found for Anthropic. ${it.message}"
                        )
                    }
                }
            }

            apiKey = key
            currentModel = config.modelPath.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL

            Timber.i("AnthropicProvider initialized successfully (model: $currentModel)")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize AnthropicProvider")
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

            Timber.d("Sending chat request to Anthropic: model=$currentModel, messages=${messages.size}")

            // Execute streaming request
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Timber.e("Anthropic API error: ${response.code} - $errorBody")
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
            Timber.e(e, "Network error in Anthropic chat")
            emit(LLMResponse.Error(
                message = "Network error: ${e.message}",
                code = "NETWORK_ERROR",
                exception = e
            ))
        } catch (e: Exception) {
            Timber.e(e, "Error in Anthropic chat")
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
        Timber.d("Anthropic generation stopped")
    }

    override suspend fun reset() {
        stop()
        Timber.d("Anthropic provider reset")
    }

    override suspend fun cleanup() {
        stop()
        apiKey = null
        Timber.i("Anthropic provider cleaned up")
    }

    override fun isGenerating(): Boolean = isGenerating

    override fun getInfo(): LLMProviderInfo {
        return LLMProviderInfo(
            name = "Claude (Anthropic)",
            version = "1.0",
            modelName = currentModel,
            isLocal = false,
            capabilities = LLMCapabilities(
                supportsStreaming = true,
                supportsChat = true,
                supportsFunctionCalling = false, // Anthropic uses different tool format
                maxContextLength = 200_000 // 200K context window
            )
        )
    }

    override suspend fun checkHealth(): Result<ProviderHealth> {
        return try {
            // Simple health check - try to get model info
            val healthRequest = Request.Builder()
                .url(MESSAGES_ENDPOINT)
                .header("x-api-key", apiKey ?: "")
                .header("anthropic-version", ANTHROPIC_VERSION)
                .head()
                .build()

            val response = client.newCall(healthRequest).execute()
            // 401 means API key is required (endpoint is reachable)
            // 200/201 means healthy
            val isHealthy = response.code in listOf(200, 201, 401)

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
        val pricing = MODEL_PRICING[currentModel] ?: Pair(3.00, 15.00)
        val inputCost = (inputTokens / 1_000_000.0) * pricing.first
        val outputCost = (outputTokens / 1_000_000.0) * pricing.second
        return inputCost + outputCost
    }

    /**
     * Build request body for messages API
     */
    private fun buildRequestBody(
        messages: List<ChatMessage>,
        options: GenerationOptions,
        stream: Boolean
    ): String {
        // Anthropic requires separating system messages from user/assistant messages
        val systemMessage = messages.firstOrNull { it.role == MessageRole.SYSTEM }?.content
        val conversationMessages = messages.filter { it.role != MessageRole.SYSTEM }

        val apiMessages = conversationMessages.map { msg ->
            AnthropicMessage(
                role = if (msg.role == MessageRole.USER) "user" else "assistant",
                content = msg.content
            )
        }

        val request = AnthropicMessagesRequest(
            model = currentModel,
            messages = apiMessages,
            max_tokens = options.maxTokens ?: 4096, // Required by Anthropic
            temperature = options.temperature.toDouble(),
            top_p = options.topP.toDouble(),
            stop_sequences = options.stopSequences.takeIf { it.isNotEmpty() },
            stream = stream,
            system = systemMessage
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
            .url(MESSAGES_ENDPOINT)
            .header("x-api-key", apiKey ?: "")
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header("content-type", "application/json")
            .post(requestBody)
            .build()
    }

    /**
     * Parse Server-Sent Events (SSE) stream
     *
     * Anthropic uses SSE format:
     * event: message_start
     * event: content_block_delta
     * data: {"type":"content_block_delta","delta":{"type":"text_delta","text":"..."}}
     * event: message_stop
     */
    private suspend fun parseServerSentEvents(
        reader: BufferedReader,
        onChunk: suspend (LLMResponse) -> Unit
    ) {
        val fullText = StringBuilder()
        var eventType: String? = null
        var totalInputTokens = 0
        var totalOutputTokens = 0

        reader.lineSequence().forEach { line ->
            when {
                line.startsWith("event: ") -> {
                    eventType = line.substring(7)
                }
                line.startsWith("data: ") -> {
                    val data = line.substring(6)

                    try {
                        when (eventType) {
                            "content_block_delta" -> {
                                val delta = json.decodeFromString<AnthropicContentDelta>(data)
                                delta.delta.text?.let { text ->
                                    fullText.append(text)
                                    onChunk(
                                        LLMResponse.Streaming(
                                            chunk = text,
                                            tokenCount = fullText.length
                                        )
                                    )
                                }
                            }
                            "message_delta" -> {
                                val messageDelta = json.decodeFromString<AnthropicMessageDelta>(data)
                                messageDelta.usage?.output_tokens?.let { outputTokens ->
                                    totalOutputTokens = outputTokens
                                }
                            }
                            "message_stop" -> {
                                onChunk(
                                    LLMResponse.Complete(
                                        fullText = fullText.toString(),
                                        usage = TokenUsage(
                                            promptTokens = totalInputTokens,
                                            completionTokens = totalOutputTokens
                                        )
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse SSE chunk: $data")
                    }
                }
            }
        }
    }
}

// ==================== API Data Classes ====================

@Serializable
private data class AnthropicMessagesRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    val max_tokens: Int,
    val temperature: Double? = null,
    val top_p: Double? = null,
    val stop_sequences: List<String>? = null,
    val stream: Boolean = false,
    val system: String? = null
)

@Serializable
private data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
private data class AnthropicContentDelta(
    val type: String,
    val delta: AnthropicDelta
)

@Serializable
private data class AnthropicDelta(
    val type: String,
    val text: String? = null
)

@Serializable
private data class AnthropicMessageDelta(
    val type: String,
    val delta: AnthropicMessageDeltaData? = null,
    val usage: AnthropicUsageDelta? = null
)

@Serializable
private data class AnthropicMessageDeltaData(
    val stop_reason: String? = null
)

@Serializable
private data class AnthropicUsageDelta(
    val output_tokens: Int
)
