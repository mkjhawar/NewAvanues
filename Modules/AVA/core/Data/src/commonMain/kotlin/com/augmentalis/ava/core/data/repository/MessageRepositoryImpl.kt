package com.augmentalis.ava.core.data.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.db.ConversationQueries
import com.augmentalis.ava.core.data.db.MessageQueries
import com.augmentalis.ava.core.data.mapper.toDomain
import com.augmentalis.ava.core.data.mapper.toInsertParams
import com.augmentalis.ava.core.data.util.UuidHelper
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.model.MessageRole
import com.augmentalis.ava.core.domain.repository.MessageRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of MessageRepository using SQLDelight
 * Handles message persistence and denormalized conversation count
 *
 * Updated: Room removed, now uses SQLDelight queries directly
 * KMP compatible: Uses UuidHelper for cross-platform UUID generation
 */
class MessageRepositoryImpl(
    private val messageQueries: MessageQueries,
    private val conversationQueries: ConversationQueries
) : MessageRepository {

    override fun getMessagesForConversation(conversationId: String): Flow<List<Message>> {
        return messageQueries.selectByConversationId(conversationId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { messages -> messages.map { it.toDomain() } }
    }

    override suspend fun getMessagesPaginated(
        conversationId: String,
        limit: Int,
        offset: Int
    ): Result<List<Message>> = withContext(Dispatchers.IO) {
        try {
            val messages = messageQueries.selectByConversationIdPaginated(
                conversation_id = conversationId,
                limit = limit.toLong(),
                offset = offset.toLong()
            ).executeAsList()
            Result.Success(messages.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to get messages")
        }
    }

    override suspend fun addMessage(message: Message): Result<Message> = withContext(Dispatchers.IO) {
        try {
            // Generate ID if not provided
            val messageWithId = if (message.id.isEmpty()) {
                message.copy(id = UuidHelper.randomUUID())
            } else {
                message
            }

            // Insert message
            val params = messageWithId.toInsertParams()
            messageQueries.insert(
                id = params.id,
                conversation_id = params.conversation_id,
                role = params.role,
                content = params.content,
                timestamp = params.timestamp,
                intent = params.intent,
                confidence = params.confidence,
                metadata = params.metadata
            )

            // Update conversation message count (VOS4 denormalized pattern)
            conversationQueries.incrementMessageCount(
                updated_at = message.timestamp,
                id = message.conversationId
            )

            Result.Success(messageWithId)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to add message")
        }
    }

    override fun getMessagesByRole(conversationId: String, role: MessageRole): Flow<List<Message>> {
        // SQLDelight doesn't have a selectByRole query, so filter in Kotlin
        return messageQueries.selectByConversationId(conversationId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { messages ->
                messages.filter { it.role == role.name }.map { it.toDomain() }
            }
    }

    override suspend fun getMessageCount(conversationId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = messageQueries.countByConversationId(conversationId)
                .executeAsOne()
                .toInt()
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to get message count")
        }
    }

    override suspend fun getRecentMessagesForContext(
        conversationId: String,
        limit: Int
    ): Result<List<Message>> = withContext(Dispatchers.IO) {
        try {
            val messages = messageQueries.selectLastN(
                conversation_id = conversationId,
                limit = limit.toLong()
            ).executeAsList()
            // Reverse to get chronological order (selectLastN returns DESC)
            Result.Success(messages.reversed().map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to get recent messages for context")
        }
    }
}
