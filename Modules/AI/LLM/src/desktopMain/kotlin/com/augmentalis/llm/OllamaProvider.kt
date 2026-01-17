package com.augmentalis.llm

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Ollama LLM Provider for Desktop
 *
 * Connects to a local Ollama server to provide LLM inference.
 * This is the primary local LLM provider for desktop platforms.
 *
 * Requirements:
 * - Ollama must be installed and running (https://ollama.ai)
 * - Default URL: http://localhost:11434
 *
 * Features:
 * - Streaming responses via SSE
 * - Multiple model support
 * - Chat completion API compatible
 * - Zero cost (local inference)
 */
class OllamaProvider : LLMProvider {

    private val logger = LoggerFactory.getLogger(OllamaProvider::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000 // 5 minutes for long generations
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 300_000
        }
    }

    private var baseUrl: String = "http://localhost:11434"
    private var modelName: String = "llama3.2"
    private var isInitialized: Boolean = false
    private var isCurrentlyGenerating: Boolean = false

    // Metrics
    private var totalRequests: Int = 0
    private var successfulRequests: Int = 0
    private var lastError: String? = null
    private var averageLatency: Long? = null
    private val latencies = mutableListOf<Long>()

    override suspend fun initialize(config: LLMConfig): LLMResult<Unit> {
        return try {
            logger.info("Initializing OllamaProvider with model: ${config.modelPath}")

            // Set configuration
            baseUrl = config.baseUrl ?: "http://localhost:11434"
            modelName = config.modelLib ?: config.modelPath

            // Test connection
            val response = client.get("$baseUrl/api/tags")

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                logger.info("Connected to Ollama. Available models: $body")
                isInitialized = true
                LLMResult.Success(Unit)
            } else {
                val error = "Failed to connect to Ollama: ${response.status}"
                logger.error(error)
                lastError = error
                LLMResult.Error(error)
            }
        } catch (e: Exception) {
            val error = "Failed to initialize OllamaProvider: ${e.message}"
            logger.error(error, e)
            lastError = error
            LLMResult.Error(error, e)
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
        if (!isInitialized) {
            emit(LLMResponse.Error(
                message = "OllamaProvider not initialized",
                code = "NOT_INITIALIZED"
            ))
            return@flow
        }

        isCurrentlyGenerating = true
        val startTime = currentTimeMillis()
        totalRequests++

        try {
            // Build request
            val ollamaMessages = messages.map { msg ->
                OllamaMessage(
                    role = msg.role.toApiString(),
                    content = msg.content
                )
            }

            val request = OllamaChatRequest(
                model = modelName,
                messages = ollamaMessages,
                stream = true,
                options = OllamaOptions(
                    temperature = options.temperature,
                    top_p = options.topP,
                    num_predict = options.maxTokens ?: 2048,
                    stop = options.stopSequences.takeIf { it.isNotEmpty() },
                    seed = options.seed
                )
            )

            val requestBody = json.encodeToString(OllamaChatRequest.serializer(), request)
            logger.debug("Sending request to Ollama: $requestBody")

            // Make streaming request
            val response = client.post("$baseUrl/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (!response.status.isSuccess()) {
                val error = "Ollama request failed: ${response.status}"
                logger.error(error)
                lastError = error
                emit(LLMResponse.Error(
                    message = error,
                    code = response.status.value.toString()
                ))
                return@flow
            }

            // Process streaming response using line-delimited JSON
            val fullResponse = StringBuilder()
            var totalTokens = 0
            var lastChunk: OllamaChatResponse? = null

            // Read the response body as text and process line by line
            val responseText = response.bodyAsText()
            responseText.lineSequence().forEach { line ->
                if (line.isBlank()) return@forEach

                try {
                    val chunk = json.decodeFromString(OllamaChatResponse.serializer(), line)
                    lastChunk = chunk

                    if (chunk.message?.content != null) {
                        val content = chunk.message.content
                        fullResponse.append(content)
                        totalTokens++

                        emit(LLMResponse.Streaming(
                            chunk = content,
                            tokenCount = totalTokens
                        ))
                    }

                    if (chunk.done == true) {
                        // Final response
                        val latency = currentTimeMillis() - startTime
                        recordLatency(latency)
                        successfulRequests++

                        emit(LLMResponse.Complete(
                            fullText = fullResponse.toString(),
                            usage = TokenUsage(
                                promptTokens = chunk.prompt_eval_count ?: 0,
                                completionTokens = chunk.eval_count ?: totalTokens,
                                totalTokens = (chunk.prompt_eval_count ?: 0) + (chunk.eval_count ?: totalTokens)
                            )
                        ))
                        return@flow
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse chunk: $line", e)
                }
            }

            // If we get here without done=true, emit what we have
            if (fullResponse.isNotEmpty()) {
                val latency = currentTimeMillis() - startTime
                recordLatency(latency)
                successfulRequests++

                emit(LLMResponse.Complete(
                    fullText = fullResponse.toString(),
                    usage = TokenUsage(
                        promptTokens = messages.sumOf { it.content.length / 4 },
                        completionTokens = totalTokens,
                        totalTokens = messages.sumOf { it.content.length / 4 } + totalTokens
                    )
                ))
            }

        } catch (e: Exception) {
            val error = "Ollama generation failed: ${e.message}"
            logger.error(error, e)
            lastError = error
            emit(LLMResponse.Error(
                message = error,
                code = "GENERATION_FAILED",
                cause = e.message
            ))
        } finally {
            isCurrentlyGenerating = false
        }
    }

    override suspend fun stop() {
        isCurrentlyGenerating = false
        // Ollama doesn't have a direct stop API, but we can set the flag
        // which will cause the flow to stop emitting
    }

    override suspend fun reset() {
        isCurrentlyGenerating = false
        // Reset conversation state (Ollama is stateless per request)
    }

    override suspend fun cleanup() {
        isCurrentlyGenerating = false
        isInitialized = false
        client.close()
        logger.info("OllamaProvider cleaned up")
    }

    override fun isGenerating(): Boolean = isCurrentlyGenerating

    override fun getInfo(): LLMProviderInfo {
        return LLMProviderInfo(
            name = "Ollama",
            version = "1.0",
            modelName = modelName,
            isLocal = true,
            capabilities = LLMCapabilities(
                supportsStreaming = true,
                supportsChat = true,
                supportsFunctionCalling = false,
                maxContextLength = 8192
            )
        )
    }

    override suspend fun checkHealth(): LLMResult<ProviderHealth> {
        return try {
            val response = client.get("$baseUrl/api/tags")

            val status = if (response.status.isSuccess()) {
                HealthStatus.HEALTHY
            } else {
                HealthStatus.UNHEALTHY
            }

            LLMResult.Success(
                ProviderHealth(
                    status = status,
                    averageLatencyMs = averageLatency,
                    errorRate = if (totalRequests > 0) {
                        ((totalRequests - successfulRequests).toDouble() / totalRequests)
                    } else null,
                    lastError = lastError,
                    lastChecked = currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            LLMResult.Success(
                ProviderHealth(
                    status = HealthStatus.UNHEALTHY,
                    lastError = e.message,
                    lastChecked = currentTimeMillis()
                )
            )
        }
    }

    override fun estimateCost(inputTokens: Int, outputTokens: Int): Double {
        // Local inference - zero cost
        return 0.0
    }

    /**
     * List available models from Ollama
     */
    suspend fun listModels(): List<String> {
        return try {
            val response = client.get("$baseUrl/api/tags")
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val tagsResponse = json.decodeFromString(OllamaTagsResponse.serializer(), body)
                tagsResponse.models.map { it.name }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Failed to list models", e)
            emptyList()
        }
    }

    /**
     * Pull a model from Ollama registry
     */
    suspend fun pullModel(model: String): LLMResult<Unit> {
        return try {
            val response = client.post("$baseUrl/api/pull") {
                contentType(ContentType.Application.Json)
                setBody("""{"name": "$model"}""")
            }

            if (response.status.isSuccess()) {
                LLMResult.Success(Unit)
            } else {
                LLMResult.Error("Failed to pull model: ${response.status}")
            }
        } catch (e: Exception) {
            LLMResult.Error("Failed to pull model: ${e.message}", e)
        }
    }

    private fun recordLatency(latency: Long) {
        latencies.add(latency)
        if (latencies.size > 100) {
            latencies.removeAt(0)
        }
        averageLatency = latencies.average().toLong()
    }
}

// ==================== Ollama API DTOs ====================

@Serializable
data class OllamaMessage(
    val role: String,
    val content: String
)

@Serializable
data class OllamaOptions(
    val temperature: Float? = null,
    val top_p: Float? = null,
    val num_predict: Int? = null,
    val stop: List<String>? = null,
    val seed: Int? = null
)

@Serializable
data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = true,
    val options: OllamaOptions? = null
)

@Serializable
data class OllamaChatResponse(
    val model: String? = null,
    val created_at: String? = null,
    val message: OllamaMessage? = null,
    val done: Boolean? = null,
    val total_duration: Long? = null,
    val load_duration: Long? = null,
    val prompt_eval_count: Int? = null,
    val prompt_eval_duration: Long? = null,
    val eval_count: Int? = null,
    val eval_duration: Long? = null
)

@Serializable
data class OllamaTagsResponse(
    val models: List<OllamaModelInfo>
)

@Serializable
data class OllamaModelInfo(
    val name: String,
    val modified_at: String? = null,
    val size: Long? = null
)
