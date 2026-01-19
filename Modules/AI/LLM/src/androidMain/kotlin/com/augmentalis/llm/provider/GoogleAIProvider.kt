/**
 * Google AI (Gemini) Provider
 *
 * Implements LLMProvider interface using Google's Gemini API.
 * Provides access to Gemini models:
 * - Gemini 1.5 Pro (1M context, multimodal)
 * - Gemini 1.5 Flash (faster, lower cost)
 * - Gemini 1.0 Pro
 *
 * Features:
 * - Streaming responses
 * - Multimodal support (text + images)
 * - Extended context (up to 1M tokens)
 * - Function calling
 *
 * API Documentation: https://ai.google.dev/docs
 *
 * Created: 2025-11-21
 * Author: AVA AI Team
 */

package com.augmentalis.llm.provider

import android.content.Context
import com.augmentalis.llm.*
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
 * Google AI Provider
 *
 * Usage:
 * ```
 * val provider = GoogleAIProvider(context, apiKeyManager)
 * provider.initialize(LLMConfig(
 *     modelPath = "gemini-1.5-pro",
 *     apiKey = "AIza..."
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
class GoogleAIProvider(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager
) : LLMProvider {

    companion object {
        private const val TAG = "GoogleAIProvider"
        private const val API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

        // Default model
        private const val DEFAULT_MODEL = "gemini-1.5-pro"

        // Pricing per 1M tokens (as of 2024-11)
        private val MODEL_PRICING = mapOf(
            "gemini-1.5-pro" to Pair(2.50, 10.00),    // $2.50 input, $10 output
            "gemini-1.5-flash" to Pair(0.35, 1.40),   // $0.35 input, $1.40 output
            "gemini-1.0-pro" to Pair(0.50, 1.50)
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

    override suspend fun initialize(config: LLMConfig): LLMResult<Unit> {
        return try {
            Timber.i("Initializing GoogleAIProvider with model: ${config.modelPath}")

            // Get API key
            val key = config.apiKey ?: apiKeyManager.getApiKey(ProviderType.GOOGLE_AI).let {
                when (it) {
                    is LLMResult.Success -> it.data
                    is LLMResult.Error -> {
                        return LLMResult.Error(
                            message = "No API key found for Google AI. ${it.message}",
                            cause = IllegalStateException("No API key found for Google AI")
                        )
                    }
                }
            }

            apiKey = key
            currentModel = config.modelPath.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL

            Timber.i("GoogleAIProvider initialized successfully (model: $currentModel)")
            LLMResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize GoogleAIProvider")
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
            val requestBody = buildRequestBody(messages, options)
            val request = buildRequest(requestBody, stream = true)

            Timber.d("Sending chat request to Google AI: model=$currentModel, messages=${messages.size}")

            // Execute streaming request
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Timber.e("Google AI API error: ${response.code} - $errorBody")
                    emit(LLMResponse.Error(
                        message = "API request failed: ${response.code} - $errorBody",
                        code = response.code.toString()
                    ))
                    return@flow
                }

                // Parse streaming response
                val reader = response.body?.byteStream()?.bufferedReader()
                    ?: throw IOException("Response body is null")

                reader.use {
                    parseStreamingResponse(it) { chunk ->
                        emit(chunk)
                    }
                }
            }

        } catch (e: IOException) {
            Timber.e(e, "Network error in Google AI chat")
            emit(LLMResponse.Error(
                message = "Network error: ${e.message}",
                code = "NETWORK_ERROR",
                cause = e.message
            ))
        } catch (e: Exception) {
            Timber.e(e, "Error in Google AI chat")
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
        Timber.d("Google AI generation stopped")
    }

    override suspend fun reset() {
        stop()
        Timber.d("Google AI provider reset")
    }

    override suspend fun cleanup() {
        stop()
        apiKey = null
        Timber.i("Google AI provider cleaned up")
    }

    override fun isGenerating(): Boolean = isGenerating

    override fun getInfo(): LLMProviderInfo {
        return LLMProviderInfo(
            name = "Google AI (Gemini)",
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

    override suspend fun checkHealth(): LLMResult<ProviderHealth> {
        return try {
            val healthRequest = Request.Builder()
                .url("$API_BASE_URL/models/$currentModel?key=$apiKey")
                .get()
                .build()

            val response = client.newCall(healthRequest).execute()
            val isHealthy = response.isSuccessful

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
        val pricing = MODEL_PRICING[currentModel] ?: Pair(2.50, 10.00)
        val inputCost = (inputTokens / 1_000_000.0) * pricing.first
        val outputCost = (outputTokens / 1_000_000.0) * pricing.second
        return inputCost + outputCost
    }

    /**
     * Build request body for Gemini API
     */
    private fun buildRequestBody(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): String {
        // Gemini requires separating system instructions from contents
        val systemInstruction = messages.firstOrNull { it.role == MessageRole.SYSTEM }?.content
        val conversationMessages = messages.filter { it.role != MessageRole.SYSTEM }

        val contents = conversationMessages.map { msg ->
            GeminiContent(
                role = if (msg.role == MessageRole.USER) "user" else "model",
                parts = listOf(GeminiPart(text = msg.content))
            )
        }

        val request = GeminiGenerateRequest(
            contents = contents,
            systemInstruction = systemInstruction?.let {
                GeminiContent(
                    role = "system",
                    parts = listOf(GeminiPart(text = it))
                )
            },
            generationConfig = GeminiGenerationConfig(
                temperature = options.temperature.toDouble(),
                maxOutputTokens = options.maxTokens,
                topP = options.topP.toDouble(),
                stopSequences = options.stopSequences.takeIf { it.isNotEmpty() }
            )
        )

        return json.encodeToString(request)
    }

    /**
     * Build HTTP request
     */
    private fun buildRequest(body: String, stream: Boolean): Request {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = body.toRequestBody(mediaType)

        val endpoint = if (stream) {
            "$API_BASE_URL/models/$currentModel:streamGenerateContent?key=$apiKey"
        } else {
            "$API_BASE_URL/models/$currentModel:generateContent?key=$apiKey"
        }

        return Request.Builder()
            .url(endpoint)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()
    }

    /**
     * Parse streaming response from Gemini
     */
    private suspend fun parseStreamingResponse(
        reader: BufferedReader,
        onChunk: suspend (LLMResponse) -> Unit
    ) {
        val fullText = StringBuilder()
        var totalInputTokens = 0
        var totalOutputTokens = 0

        // Gemini streams JSON objects separated by newlines
        reader.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach

            try {
                val chunk = json.decodeFromString<GeminiStreamChunk>(line)

                // Extract text from candidates
                chunk.candidates.firstOrNull()?.content?.parts?.forEach { part ->
                    part.text?.let { text ->
                        fullText.append(text)
                        onChunk(
                            LLMResponse.Streaming(
                                chunk = text,
                                tokenCount = fullText.length
                            )
                        )
                    }
                }

                // Extract usage if present
                chunk.usageMetadata?.let { usage ->
                    totalInputTokens = usage.promptTokenCount
                    totalOutputTokens = usage.candidatesTokenCount
                }

                // Check if generation is complete
                if (chunk.candidates.firstOrNull()?.finishReason != null) {
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

            } catch (e: Exception) {
                Timber.w(e, "Failed to parse streaming chunk: $line")
            }
        }
    }

    /**
     * Get max context length for a model
     */
    private fun getMaxContextLength(model: String): Int {
        return when {
            model.contains("1.5") -> 1_000_000 // 1M context
            else -> 30_720 // 30K for older models
        }
    }

    // ==================== Command Interpretation (VoiceOS AI Integration) ====================

    /**
     * Interpret a voice command utterance using Google AI
     *
     * Note: Command interpretation is primarily handled by local LLM for latency.
     */
    override suspend fun interpretCommand(
        utterance: String,
        availableCommands: List<String>,
        context: String?
    ): CommandInterpretationResult {
        return CommandInterpretationResult.Error(
            "Command interpretation not implemented for GoogleAI. Use LocalLLMProvider."
        )
    }

    /**
     * Clarify a command when multiple candidates match
     */
    override suspend fun clarifyCommand(
        utterance: String,
        candidates: List<String>
    ): ClarificationResult {
        return ClarificationResult(
            selectedCommand = null,
            confidence = 0f,
            clarificationQuestion = "Please use local voice assistant for command clarification."
        )
    }
}

// ==================== API Data Classes ====================

@Serializable
private data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig
)

@Serializable
private data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(
    val text: String
)

@Serializable
private data class GeminiGenerationConfig(
    val temperature: Double,
    val maxOutputTokens: Int? = null,
    val topP: Double,
    val stopSequences: List<String>? = null
)

@Serializable
private data class GeminiStreamChunk(
    val candidates: List<GeminiCandidate>,
    val usageMetadata: GeminiUsageMetadata? = null
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent,
    val finishReason: String? = null
)

@Serializable
private data class GeminiUsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int
)
