package com.augmentalis.alc.domain

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Role of a message author in a conversation
 */
@Serializable
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    FUNCTION,
    TOOL
}

/**
 * Represents a message in a conversation
 *
 * Compatible with OpenAI/Anthropic chat format for provider interoperability.
 */
@Serializable
data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val name: String? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    companion object {
        fun system(content: String) = ChatMessage(MessageRole.SYSTEM, content)
        fun user(content: String) = ChatMessage(MessageRole.USER, content)
        fun assistant(content: String) = ChatMessage(MessageRole.ASSISTANT, content)
        fun function(name: String, content: String) = ChatMessage(MessageRole.FUNCTION, content, name)
    }
}

/**
 * Token usage statistics
 */
@Serializable
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int = promptTokens + completionTokens
)

/**
 * Generation options for LLM requests
 */
@Serializable
data class GenerationOptions(
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repetitionPenalty: Float = 1.1f,
    val stopSequences: List<String> = emptyList(),
    val stream: Boolean = true
) {
    companion object {
        val DEFAULT = GenerationOptions()
        val PRECISE = GenerationOptions(temperature = 0.1f, topP = 0.5f)
        val CREATIVE = GenerationOptions(temperature = 0.9f, topP = 0.95f)
        val BALANCED = GenerationOptions(temperature = 0.5f, topP = 0.8f)
    }
}
