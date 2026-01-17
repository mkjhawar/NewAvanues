/**
 * HuggingFace Inference API Provider
 *
 * Implements LLMProvider interface using HuggingFace Inference API.
 * Provides access to thousands of open-source models:
 * - Llama 3.1 (8B, 70B, 405B)
 * - Mistral (7B, Mixtral 8x7B)
 * - Zephyr, CodeLlama, and many more
 *
 * Features:
 * - Streaming responses
 * - Free tier available
 * - Access to cutting-edge open models
 * - Low latency inference
 *
 * API Documentation: https://huggingface.co/docs/api-inference
 *
 * Created: 2025-11-21
 * Author: AVA AI Team
 */

package com.augmentalis.llm.provider

import android.content.Context
import com.augmentalis.llm.LLMConfig
import com.augmentalis.llm.LLMResult
import com.augmentalis.llm.LLMResponse
import com.augmentalis.llm.TokenUsage
import com.augmentalis.llm.ChatMessage
import com.augmentalis.llm.MessageRole
import com.augmentalis.llm.GenerationOptions
import com.augmentalis.llm.LLMProviderInfo
import com.augmentalis.llm.LLMCapabilities
import com.augmentalis.llm.ProviderHealth
import com.augmentalis.llm.HealthStatus
import com.augmentalis.llm.LLMProvider
import com.augmentalis.llm.ProviderType
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
 * HuggingFace Provider
 *
 * Usage:
 * ```
 * val provider = HuggingFaceProvider(context, apiKeyManager)
 * provider.initialize(LLMConfig(
 *     modelPath = "meta-llama/Llama-3.1-8B-Instruct",
 *     apiKey = "hf_..."
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
class HuggingFaceProvider(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager
) : LLMProvider {

    companion object {
        private const val TAG = "HuggingFaceProvider"
        private const val API_BASE_URL = "https://api-inference.huggingface.co/models"

        // Default model (Llama 3.1 8B)
        private const val DEFAULT_MODEL = "meta-llama/Llama-3.1-8B-Instruct"

        // Popular models
        private val POPULAR_MODELS = mapOf(
            "meta-llama/Llama-3.1-8B-Instruct" to 8192,
            "meta-llama/Llama-3.1-70B-Instruct" to 8192,
            "mistralai/Mistral-7B-Instruct-v0.3" to 32768,
            "mistralai/Mixtral-8x7B-Instruct-v0.1" to 32768,
            "HuggingFaceH4/zephyr-7b-beta" to 8192
        )

        private const val TIMEOUT_SECONDS = 120L // HF can be slower
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

    override suspend fun initialize(config: LLMConfig): LLMResult<Unit> {
        return try {
            Timber.i("Initializing HuggingFaceProvider with model: ${config.modelPath}")

            // Get API key
            val key = config.apiKey ?: apiKeyManager.getApiKey(ProviderType.HUGGINGFACE).let {
                when (it) {
                    is LLMResult.Success -> it.data
                    is LLMResult.Error -> {
                        return LLMResult.Error(
                            message = "No API key found for HuggingFace. ${it.message}",
                            cause = IllegalStateException("No API key found for HuggingFace")
                        )
                    }
                }
            }

            apiKey = key
            currentModel = config.modelPath.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL

            Timber.i("HuggingFaceProvider initialized successfully (model: $currentModel)")
            LLMResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize HuggingFaceProvider")
            LLMResult.Error(
                message = "Initialization failed: ${e.message}",
                cause = e
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

            Timber.d("Sending chat request to HuggingFace: model=$currentModel, messages=${messages.size}")

            // Execute streaming request
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Timber.e("HuggingFace API error: ${response.code} - $errorBody")

                    // Handle model loading (503)
                    if (response.code == 503) {
                        emit(LLMResponse.Error(
                            message = "Model is loading. Please try again in a few seconds.",
                            code = "MODEL_LOADING"
                        ))
                    } else {
                        emit(LLMResponse.Error(
                            message = "API request failed: ${response.code} - $errorBody",
                            code = response.code.toString()
                        ))
                    }
                    return@flow
                }

                // Parse response (HF Inference API may not stream for all models)
                val reader = response.body?.byteStream()?.bufferedReader()
                    ?: throw IOException("Response body is null")

                reader.use {
                    parseStreamingResponse(it) { chunk ->
                        emit(chunk)
                    }
                }
            }

        } catch (e: IOException) {
            Timber.e(e, "Network error in HuggingFace chat")
            emit(LLMResponse.Error(
                message = "Network error: ${e.message}",
                code = "NETWORK_ERROR",
                cause = e.message
            ))
        } catch (e: Exception) {
            Timber.e(e, "Error in HuggingFace chat")
            emit(LLMResponse.Error(
                message = "Chat failed: ${e.message}",
                code = "CHAT_ERROR",
                cause = e.message
            ))
        } finally {
            isGenerating = false
        }
    }

    override suspend fun stop() {
        client.dispatcher.cancelAll()
        isGenerating = false
        Timber.d("HuggingFace generation stopped")
    }

    override suspend fun reset() {
        stop()
        Timber.d("HuggingFace provider reset")
    }

    override suspend fun cleanup() {
        stop()
        apiKey = null
        Timber.i("HuggingFace provider cleaned up")
    }

    override fun isGenerating(): Boolean = isGenerating

    override fun getInfo(): LLMProviderInfo {
        return LLMProviderInfo(
            name = "HuggingFace",
            version = "1.0",
            modelName = currentModel,
            isLocal = false,
            capabilities = LLMCapabilities(
                supportsStreaming = true,
                supportsChat = true,
                supportsFunctionCalling = false,
                maxContextLength = POPULAR_MODELS[currentModel] ?: 4096
            )
        )
    }

    override suspend fun checkHealth(): LLMResult<ProviderHealth> {
        return try {
            // Simple health check - ping the model endpoint
            val healthRequest = Request.Builder()
                .url("$API_BASE_URL/$currentModel")
                .header("Authorization", "Bearer $apiKey")
                .head()
                .build()

            val response = client.newCall(healthRequest).execute()
            val isHealthy = response.isSuccessful || response.code == 503 // 503 = model loading

            LLMResult.Success(
                ProviderHealth(
                    status = if (isHealthy) HealthStatus.HEALTHY else HealthStatus.UNHEALTHY,
                    averageLatencyMs = null,
                    errorRate = null,
                    lastError = if (!isHealthy) "Health check failed: ${response.code}" else null,
                    lastChecked = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            LLMResult.Error(
                message = "Health check failed: ${e.message}",
                cause = e
            )
        }
    }

    override fun estimateCost(inputTokens: Int, outputTokens: Int): Double {
        // HuggingFace Inference API has free tier
        // Pro tier pricing varies by model, but generally very affordable
        // Estimate: $0.001 per 1K tokens (average)
        val totalTokens = inputTokens + outputTokens
        return (totalTokens / 1000.0) * 0.001
    }

    /**
     * Build request body for HuggingFace Inference API
     */
    private fun buildRequestBody(
        messages: List<ChatMessage>,
        options: GenerationOptions,
        stream: Boolean
    ): String {
        // Convert messages to prompt (HF Inference API uses text-generation format)
        val prompt = buildPromptFromMessages(messages)

        val request = HFInferenceRequest(
            inputs = prompt,
            parameters = HFParameters(
                temperature = options.temperature.toDouble(),
                max_new_tokens = options.maxTokens ?: 512,
                top_p = options.topP.toDouble(),
                repetition_penalty = 1.0 + options.frequencyPenalty.toDouble(),
                return_full_text = false,
                do_sample = options.temperature > 0.0f
            ),
            options = HFOptions(
                wait_for_model = true,
                use_cache = false
            )
        )

        return json.encodeToString(request)
    }

    /**
     * Build prompt from messages (apply chat template)
     */
    private fun buildPromptFromMessages(messages: List<ChatMessage>): String {
        // For Llama 3.1 chat template
        val prompt = StringBuilder()

        messages.forEach { message ->
            when (message.role) {
                MessageRole.SYSTEM -> prompt.append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n${message.content}<|eot_id|>")
                MessageRole.USER -> prompt.append("<|start_header_id|>user<|end_header_id|>\n\n${message.content}<|eot_id|>")
                MessageRole.ASSISTANT -> prompt.append("<|start_header_id|>assistant<|end_header_id|>\n\n${message.content}<|eot_id|>")
                MessageRole.TOOL -> {} // Tools not supported in this chat template
            }
        }

        // Add assistant header for response
        prompt.append("<|start_header_id|>assistant<|end_header_id|>\n\n")

        return prompt.toString()
    }

    /**
     * Build HTTP request
     */
    private fun buildRequest(body: String): Request {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = body.toRequestBody(mediaType)

        return Request.Builder()
            .url("$API_BASE_URL/$currentModel")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()
    }

    /**
     * Parse streaming response from HuggingFace
     */
    private suspend fun parseStreamingResponse(
        reader: BufferedReader,
        onChunk: suspend (LLMResponse) -> Unit
    ) {
        val fullText = StringBuilder()

        // HF Inference API returns JSON array
        val responseText = reader.readText()

        try {
            val responses = json.decodeFromString<List<HFResponse>>(responseText)

            responses.forEach { response ->
                val generatedText = response.generated_text
                fullText.append(generatedText)

                // Emit as streaming chunks
                generatedText.chunked(10).forEach { chunk ->
                    onChunk(
                        LLMResponse.Streaming(
                            chunk = chunk,
                            tokenCount = fullText.length
                        )
                    )
                }
            }

            // Emit complete response
            onChunk(
                LLMResponse.Complete(
                    fullText = fullText.toString(),
                    usage = TokenUsage(
                        promptTokens = 0, // HF doesn't return token counts
                        completionTokens = fullText.length / 4 // Rough estimate
                    )
                )
            )

        } catch (e: Exception) {
            Timber.w(e, "Failed to parse HF response: $responseText")
            onChunk(
                LLMResponse.Error(
                    message = "Failed to parse response: ${e.message}",
                    code = "PARSE_ERROR"
                )
            )
        }
    }
}

// ==================== API Data Classes ====================

@Serializable
private data class HFInferenceRequest(
    val inputs: String,
    val parameters: HFParameters,
    val options: HFOptions
)

@Serializable
private data class HFParameters(
    val temperature: Double,
    val max_new_tokens: Int,
    val top_p: Double,
    val repetition_penalty: Double,
    val return_full_text: Boolean,
    val do_sample: Boolean
)

@Serializable
private data class HFOptions(
    val wait_for_model: Boolean,
    val use_cache: Boolean
)

@Serializable
private data class HFResponse(
    val generated_text: String
)
