package com.augmentalis.ava.core.domain.model

/**
 * Domain model for a conversation
 * Represents a chat session between user and AVA
 */
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val isArchived: Boolean = false,
    val metadata: Map<String, String>? = null,
    val preview: String = ""
)
