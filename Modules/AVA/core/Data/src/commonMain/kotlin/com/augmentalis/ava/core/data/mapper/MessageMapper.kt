package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.model.MessageRole
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.augmentalis.ava.core.data.db.Message as DbMessage

/**
 * Mapper functions for Domain Message <-> SQLDelight Message
 * Updated to use SQLDelight generated classes (Room removed)
 */

private val json = Json { ignoreUnknownKeys = true }

/**
 * Convert SQLDelight Message to Domain Message
 */
fun DbMessage.toDomain(): Message {
    return Message(
        id = id,
        conversationId = conversation_id,
        role = MessageRole.valueOf(role),
        content = content,
        timestamp = timestamp,
        intent = intent,
        confidence = confidence?.toFloat(),
        metadata = metadata?.let {
            try {
                json.decodeFromString<Map<String, String>>(it)
            } catch (e: Exception) {
                null
            }
        }
    )
}

/**
 * Convert Domain Message to SQLDelight insert parameters
 * Returns a lambda that can be passed to MessageQueries.insert()
 */
fun Message.toInsertParams(): MessageInsertParams {
    return MessageInsertParams(
        id = id,
        conversation_id = conversationId,
        role = role.name,
        content = content,
        timestamp = timestamp,
        intent = intent,
        confidence = confidence?.toDouble(),
        metadata = metadata?.let { json.encodeToString(it) }
    )
}

/**
 * Parameters for inserting a message via SQLDelight
 */
data class MessageInsertParams(
    val id: String,
    val conversation_id: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val intent: String?,
    val confidence: Double?,
    val metadata: String?
)
