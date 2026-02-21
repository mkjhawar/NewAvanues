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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
                setBody(buildJsonObject { put("name", model) }.toString())
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

    // ==================== Command Interpretation (VoiceOS AI Integration) ====================

    /**
     * Interpret a voice command utterance using LLM
     *
     * Maps natural language utterances to available commands when NLU confidence is low.
     */
    override suspend fun interpretCommand(
        utterance: String,
        availableCommands: List<String>,
        context: String?
    ): CommandInterpretationResult {
        if (!isInitialized) {
            return CommandInterpretationResult.Error("OllamaProvider not initialized")
        }

        val startTime = currentTimeMillis()

        try {
            // Build the interpretation prompt
            val prompt = buildCommandInterpretationPrompt(utterance, availableCommands, context)

            logger.debug("Command interpretation prompt: ${prompt.take(200)}...")

            // Generate response
            val responseBuilder = StringBuilder()
            generateResponse(prompt, GenerationOptions(
                temperature = 0.3f, // Lower temperature for more deterministic matching
                maxTokens = 200     // Short response expected
            )).collect { response ->
                when (response) {
                    is LLMResponse.Streaming -> responseBuilder.append(response.chunk)
                    is LLMResponse.Complete -> responseBuilder.append(response.fullText)
                    is LLMResponse.Error -> {
                        logger.error("Interpretation error: ${response.message}")
                    }
                }
            }

            val llmResponse = responseBuilder.toString().trim()
            logger.debug("LLM interpretation response: $llmResponse")

            // Parse the response
            val result = parseCommandInterpretationResponse(llmResponse, availableCommands)

            val latency = currentTimeMillis() - startTime
            logger.info("Command interpretation completed in ${latency}ms: ${result::class.simpleName}")

            return result

        } catch (e: Exception) {
            logger.error("Failed to interpret command", e)
            return CommandInterpretationResult.Error("Interpretation failed: ${e.message}")
        }
    }

    /**
     * Clarify a command when multiple candidates match
     */
    override suspend fun clarifyCommand(
        utterance: String,
        candidates: List<String>
    ): ClarificationResult {
        if (!isInitialized) {
            return ClarificationResult(
                selectedCommand = null,
                confidence = 0f,
                clarificationQuestion = "Voice assistant is not ready. Please try again."
            )
        }

        val startTime = currentTimeMillis()

        try {
            // Build the clarification prompt
            val prompt = buildClarificationPrompt(utterance, candidates)

            logger.debug("Clarification prompt: ${prompt.take(200)}...")

            // Generate response
            val responseBuilder = StringBuilder()
            generateResponse(prompt, GenerationOptions(
                temperature = 0.3f,
                maxTokens = 150
            )).collect { response ->
                when (response) {
                    is LLMResponse.Streaming -> responseBuilder.append(response.chunk)
                    is LLMResponse.Complete -> responseBuilder.append(response.fullText)
                    is LLMResponse.Error -> {
                        logger.error("Clarification error: ${response.message}")
                    }
                }
            }

            val llmResponse = responseBuilder.toString().trim()
            logger.debug("LLM clarification response: $llmResponse")

            // Parse the response
            val result = parseClarificationResponse(llmResponse, candidates)

            val latency = currentTimeMillis() - startTime
            logger.info("Command clarification completed in ${latency}ms")

            return result

        } catch (e: Exception) {
            logger.error("Failed to clarify command", e)
            return ClarificationResult(
                selectedCommand = null,
                confidence = 0f,
                clarificationQuestion = "I had trouble understanding. Could you please repeat?"
            )
        }
    }

    /**
     * Build prompt for command interpretation
     */
    private fun buildCommandInterpretationPrompt(
        utterance: String,
        availableCommands: List<String>,
        context: String?
    ): String {
        val contextLine = if (context != null) "Context: $context\n" else ""
        val commandList = availableCommands.joinToString("\n") { "- $it" }

        return """You are a voice command interpreter. Given a user's spoken command, determine which available command best matches their intent.

$contextLine
Available commands:
$commandList

User said: "$utterance"

Instructions:
1. Analyze the user's intent
2. Match it to the most appropriate command from the list
3. Respond in this exact format:
   COMMAND: <command_name>
   CONFIDENCE: <0.0-1.0>
   REASONING: <brief explanation>

If no command matches, respond:
   COMMAND: NONE
   CONFIDENCE: 0.0
   REASONING: <why no match>

Response:"""
    }

    /**
     * Parse command interpretation response from LLM
     */
    private fun parseCommandInterpretationResponse(
        response: String,
        availableCommands: List<String>
    ): CommandInterpretationResult {
        try {
            // Extract COMMAND line
            val commandMatch = Regex("COMMAND:\\s*([^\\n]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val commandStr = commandMatch?.groupValues?.get(1)?.trim() ?: ""

            // Extract CONFIDENCE line
            val confidenceMatch = Regex("CONFIDENCE:\\s*([0-9.]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val confidence = confidenceMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.5f

            // Extract REASONING line
            val reasoningMatch = Regex("REASONING:\\s*([^\\n]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val reasoning = reasoningMatch?.groupValues?.get(1)?.trim()

            // Check for NONE / no match
            if (commandStr.equals("NONE", ignoreCase = true) || commandStr.isEmpty()) {
                return CommandInterpretationResult.NoMatch
            }

            // Verify command is in available list (case-insensitive match)
            val matchedCommand = availableCommands.find {
                it.equals(commandStr, ignoreCase = true)
            }

            return if (matchedCommand != null) {
                CommandInterpretationResult.Interpreted(
                    matchedCommand = matchedCommand,
                    confidence = confidence.coerceIn(0f, 1f),
                    reasoning = reasoning
                )
            } else {
                // LLM returned a command not in the list - try fuzzy match
                val fuzzyMatch = findFuzzyMatch(commandStr, availableCommands)
                if (fuzzyMatch != null) {
                    CommandInterpretationResult.Interpreted(
                        matchedCommand = fuzzyMatch,
                        confidence = (confidence * 0.8f).coerceIn(0f, 1f), // Reduce confidence for fuzzy match
                        reasoning = reasoning
                    )
                } else {
                    CommandInterpretationResult.NoMatch
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse interpretation response: $response", e)
            return CommandInterpretationResult.Error("Parse error: ${e.message}")
        }
    }

    /**
     * Build prompt for command clarification
     */
    private fun buildClarificationPrompt(
        utterance: String,
        candidates: List<String>
    ): String {
        val candidateList = candidates.mapIndexed { index, cmd -> "${index + 1}. $cmd" }
            .joinToString("\n")

        return """You are a voice assistant helping to clarify a user's command. The user said something that could match multiple commands.

User said: "$utterance"

Possible matching commands:
$candidateList

Instructions:
1. Determine if you can confidently select one command
2. If confident, respond:
   SELECT: <command_name>
   CONFIDENCE: <0.7-1.0>
3. If unsure, create a clarifying question:
   SELECT: NONE
   CONFIDENCE: <0.0-0.6>
   QUESTION: <simple question to clarify user intent>

Keep questions natural and conversational. Example: "Would you like to open the camera app or take a photo?"

Response:"""
    }

    /**
     * Parse clarification response from LLM
     */
    private fun parseClarificationResponse(
        response: String,
        candidates: List<String>
    ): ClarificationResult {
        try {
            // Extract SELECT line
            val selectMatch = Regex("SELECT:\\s*([^\\n]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val selectStr = selectMatch?.groupValues?.get(1)?.trim() ?: ""

            // Extract CONFIDENCE line
            val confidenceMatch = Regex("CONFIDENCE:\\s*([0-9.]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val confidence = confidenceMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.5f

            // Extract QUESTION line
            val questionMatch = Regex("QUESTION:\\s*([^\\n]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val question = questionMatch?.groupValues?.get(1)?.trim()

            // Check if a command was selected
            if (selectStr.equals("NONE", ignoreCase = true) || selectStr.isEmpty()) {
                return ClarificationResult(
                    selectedCommand = null,
                    confidence = confidence.coerceIn(0f, 1f),
                    clarificationQuestion = question ?: generateDefaultClarificationQuestion(candidates)
                )
            }

            // Find matching command
            val matchedCommand = candidates.find {
                it.equals(selectStr, ignoreCase = true)
            } ?: findFuzzyMatch(selectStr, candidates)

            return if (matchedCommand != null && confidence >= 0.7f) {
                ClarificationResult(
                    selectedCommand = matchedCommand,
                    confidence = confidence.coerceIn(0f, 1f),
                    clarificationQuestion = null
                )
            } else {
                ClarificationResult(
                    selectedCommand = null,
                    confidence = confidence.coerceIn(0f, 1f),
                    clarificationQuestion = question ?: generateDefaultClarificationQuestion(candidates)
                )
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse clarification response: $response", e)
            return ClarificationResult(
                selectedCommand = null,
                confidence = 0f,
                clarificationQuestion = generateDefaultClarificationQuestion(candidates)
            )
        }
    }

    /**
     * Generate a default clarification question
     */
    private fun generateDefaultClarificationQuestion(candidates: List<String>): String {
        return when (candidates.size) {
            0 -> "I'm not sure what you'd like me to do. Could you please repeat?"
            1 -> "Did you mean ${formatCommandName(candidates[0])}?"
            2 -> "Did you want to ${formatCommandName(candidates[0])} or ${formatCommandName(candidates[1])}?"
            else -> {
                val firstTwo = candidates.take(2).joinToString(", ") { formatCommandName(it) }
                "Did you mean $firstTwo, or something else?"
            }
        }
    }

    /**
     * Format command name for display (snake_case to natural language)
     */
    private fun formatCommandName(command: String): String {
        return command
            .replace("_", " ")
            .replace("-", " ")
            .lowercase()
    }

    /**
     * Find fuzzy match for command name
     */
    private fun findFuzzyMatch(input: String, commands: List<String>): String? {
        val normalizedInput = input.lowercase().replace("_", "").replace("-", "").replace(" ", "")

        return commands.find { cmd ->
            val normalizedCmd = cmd.lowercase().replace("_", "").replace("-", "").replace(" ", "")
            normalizedInput.contains(normalizedCmd) || normalizedCmd.contains(normalizedInput)
        }
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
