package com.augmentalis.ava.core.domain.model

/**
 * Domain model for a message within a conversation
 */
data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val intent: String? = null,
    val confidence: Float? = null,
    val metadata: Map<String, String>? = null
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
