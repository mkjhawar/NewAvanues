package com.augmentalis.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Desktop implementation of ResponseGenerator
 *
 * Uses Ollama for local LLM inference on desktop platforms.
 * Falls back to template responses if Ollama is unavailable.
 */
class DesktopResponseGenerator(
    private val ollamaProvider: OllamaProvider? = null
) : ResponseGenerator {

    private var isReady: Boolean = false

    /**
     * Initialize with Ollama provider
     */
    suspend fun initialize(config: LLMConfig): LLMResult<Unit> {
        val provider = ollamaProvider ?: OllamaProvider()
        val result = provider.initialize(config)

        isReady = result.isSuccess
        return result
    }

    override suspend fun generateResponse(
        userMessage: String,
        intent: String,
        confidence: Float,
        context: ResponseContext
    ): Flow<ResponseChunk> = flow {
        if (ollamaProvider == null || !isReady) {
            // Fallback to template-based response
            val response = generateTemplateResponse(intent, userMessage, context)
            emit(ResponseChunk.Text(response))
            emit(ResponseChunk.Complete(response))
            return@flow
        }

        try {
            // Build prompt with context
            val prompt = buildPrompt(userMessage, intent, confidence, context)

            val messages = listOf(
                ChatMessage(
                    role = MessageRole.SYSTEM,
                    content = "You are a helpful AI assistant. Respond naturally and concisely."
                ),
                ChatMessage(
                    role = MessageRole.USER,
                    content = prompt
                )
            )

            val fullText = StringBuilder()

            ollamaProvider.chat(messages, GenerationOptions()).collect { response ->
                when (response) {
                    is LLMResponse.Streaming -> {
                        fullText.append(response.chunk)
                        emit(ResponseChunk.Text(response.chunk))
                    }
                    is LLMResponse.Complete -> {
                        emit(ResponseChunk.Complete(
                            fullText = response.fullText,
                            metadata = mapOf(
                                "tokens" to response.usage.totalTokens.toString()
                            )
                        ))
                    }
                    is LLMResponse.Error -> {
                        emit(ResponseChunk.Error(
                            message = response.message,
                            cause = response.cause
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to template on error
            val response = generateTemplateResponse(intent, userMessage, context)
            emit(ResponseChunk.Text(response))
            emit(ResponseChunk.Complete(response))
        }
    }

    override fun isReady(): Boolean = isReady

    override fun getInfo(): GeneratorInfo {
        return GeneratorInfo(
            name = "Desktop Response Generator",
            type = if (ollamaProvider != null) GeneratorType.HYBRID else GeneratorType.TEMPLATE,
            supportsStreaming = true,
            metadata = mapOf(
                "provider" to if (ollamaProvider != null) "Ollama" else "Template"
            )
        )
    }

    private fun buildPrompt(
        userMessage: String,
        intent: String,
        confidence: Float,
        context: ResponseContext
    ): String {
        val builder = StringBuilder()

        builder.append("User message: $userMessage\n")
        builder.append("Detected intent: $intent (confidence: ${String.format("%.2f", confidence)})\n")

        context.actionResult?.let { result ->
            if (result.success) {
                builder.append("Action result: ${result.message}\n")
            } else {
                builder.append("Action failed: ${result.errorMessage}\n")
            }
        }

        if (context.conversationHistory.isNotEmpty()) {
            builder.append("\nRecent conversation:\n")
            context.conversationHistory.takeLast(3).forEach { turn ->
                builder.append("User: ${turn.userMessage}\n")
                builder.append("Assistant: ${turn.assistantResponse}\n")
            }
        }

        builder.append("\nRespond helpfully and concisely.")

        return builder.toString()
    }

    private fun generateTemplateResponse(
        intent: String,
        userMessage: String,
        context: ResponseContext
    ): String {
        // Simple template-based responses for common intents
        return when (intent) {
            "greeting" -> "Hello! How can I help you today?"
            "farewell" -> "Goodbye! Have a great day!"
            "thanks" -> "You're welcome! Let me know if you need anything else."
            "help" -> "I'm here to help! You can ask me questions or give me tasks to perform."
            "unknown" -> "I'm not sure I understood that. Could you please rephrase?"
            else -> {
                // Check for action result
                context.actionResult?.let { result ->
                    if (result.success) {
                        result.message ?: "Done!"
                    } else {
                        result.errorMessage ?: "I encountered an issue. Please try again."
                    }
                } ?: "I'll help you with that."
            }
        }
    }
}
