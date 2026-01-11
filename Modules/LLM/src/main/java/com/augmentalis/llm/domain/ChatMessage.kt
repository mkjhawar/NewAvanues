package com.augmentalis.llm.domain

/**
 * Represents a message in a conversation
 *
 * Compatible with OpenAI chat format for easy integration with various LLM providers.
 */
data class ChatMessage(
    /**
     * The role of the message author
     */
    val role: MessageRole,

    /**
     * The content of the message
     */
    val content: String,

    /**
     * Optional message name (for function calling / multi-agent systems)
     */
    val name: String? = null,

    /**
     * Timestamp when the message was created
     */
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Role of the message author
 *
 * Matches OpenAI chat completion roles for compatibility.
 */
enum class MessageRole {
    /**
     * System message - sets behavior/instructions for the assistant
     * Example: "You are AVA, a helpful privacy-first AI assistant"
     */
    SYSTEM,

    /**
     * User message - input from the human user
     * Example: "What's the weather today?"
     */
    USER,

    /**
     * Assistant message - response from the AI
     * Example: "I can help you check the weather..."
     */
    ASSISTANT,

    /**
     * Tool/Function message - result from a function call
     * Used in advanced scenarios with function calling
     */
    TOOL;

    /**
     * Convert to lowercase string (as expected by OpenAI API)
     */
    fun toApiString(): String = name.lowercase()

    companion object {
        /**
         * Parse from API string
         */
        fun fromApiString(value: String): MessageRole {
            return when (value.lowercase()) {
                "system" -> SYSTEM
                "user" -> USER
                "assistant" -> ASSISTANT
                "tool" -> TOOL
                else -> throw IllegalArgumentException("Unknown message role: $value")
            }
        }
    }
}
