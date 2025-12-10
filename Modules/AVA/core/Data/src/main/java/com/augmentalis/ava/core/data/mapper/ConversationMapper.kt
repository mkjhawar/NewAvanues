package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.domain.model.Conversation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.augmentalis.ava.core.data.db.Conversation as DbConversation

/**
 * Mapper functions for Domain Conversation <-> SQLDelight Conversation
 * Updated to use SQLDelight generated classes (Room removed)
 */

private val json = Json { ignoreUnknownKeys = true }

/**
 * Convert SQLDelight Conversation to Domain Conversation
 */
fun DbConversation.toDomain(): Conversation {
    return Conversation(
        id = id,
        title = title,
        createdAt = created_at,
        updatedAt = updated_at,
        messageCount = message_count.toInt(),
        isArchived = is_archived,
        metadata = metadata?.let {
            try {
                json.decodeFromString<Map<String, String>>(it)
            } catch (e: Exception) {
                null
            }
        },
        preview = ""
    )
}

/**
 * Convert SelectAllWithPreview result to Domain Conversation
 * This mapper includes the preview field from the SQL query
 */
fun com.augmentalis.ava.core.data.db.SelectAllWithPreview.toDomain(): Conversation {
    return Conversation(
        id = id,
        title = title,
        createdAt = created_at,
        updatedAt = updated_at,
        messageCount = message_count.toInt(),
        isArchived = is_archived,
        metadata = metadata?.let {
            try {
                json.decodeFromString<Map<String, String>>(it)
            } catch (e: Exception) {
                null
            }
        },
        preview = preview.take(50) // Truncate to 50 chars as requested
    )
}

/**
 * Convert Domain Conversation to SQLDelight insert parameters
 */
fun Conversation.toInsertParams(): ConversationInsertParams {
    return ConversationInsertParams(
        id = id,
        title = title,
        created_at = createdAt,
        updated_at = updatedAt,
        message_count = messageCount.toLong(),
        is_archived = isArchived,
        metadata = metadata?.let { json.encodeToString(it) }
    )
}

/**
 * Parameters for inserting a conversation via SQLDelight
 */
data class ConversationInsertParams(
    val id: String,
    val title: String,
    val created_at: Long,
    val updated_at: Long,
    val message_count: Long,
    val is_archived: Boolean,
    val metadata: String?
)
