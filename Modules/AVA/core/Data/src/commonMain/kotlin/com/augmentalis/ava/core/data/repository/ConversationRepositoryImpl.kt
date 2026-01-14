package com.augmentalis.ava.core.data.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.db.ConversationQueries
import com.augmentalis.ava.core.data.mapper.toDomain
import com.augmentalis.ava.core.data.mapper.toInsertParams
import com.augmentalis.ava.core.data.util.TimeHelper
import com.augmentalis.ava.core.data.util.VuidHelper
import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.repository.ConversationRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of ConversationRepository using SQLDelight
 * Handles conversation persistence
 *
 * Updated: Room removed, now uses SQLDelight queries directly
 * KMP compatible: Uses VuidHelper and TimeHelper for cross-platform functionality
 */
class ConversationRepositoryImpl(
    private val conversationQueries: ConversationQueries
) : ConversationRepository {

    override fun getAllConversations(): Flow<List<Conversation>> {
        return conversationQueries.selectAllWithPreview()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { conversations -> conversations.map { it.toDomain() } }
    }

    override suspend fun getConversationById(id: String): Result<Conversation> = withContext(Dispatchers.IO) {
        try {
            val conversation = conversationQueries.selectById(id).executeAsOneOrNull()
            if (conversation != null) {
                Result.Success(conversation.toDomain())
            } else {
                Result.Error(
                    exception = NoSuchElementException("Conversation not found: $id"),
                    message = "Conversation not found"
                )
            }
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to get conversation")
        }
    }

    override suspend fun createConversation(title: String): Result<Conversation> = withContext(Dispatchers.IO) {
        try {
            val conversation = Conversation(
                id = VuidHelper.randomVUID(),
                title = title,
                createdAt = TimeHelper.currentTimeMillis(),
                updatedAt = TimeHelper.currentTimeMillis(),
                messageCount = 0,
                isArchived = false
            )

            val params = conversation.toInsertParams()
            conversationQueries.insert(
                id = params.id,
                title = params.title,
                created_at = params.created_at,
                updated_at = params.updated_at,
                message_count = params.message_count,
                is_archived = params.is_archived,
                metadata = params.metadata
            )

            Result.Success(conversation)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to create conversation")
        }
    }

    override suspend fun updateConversation(conversation: Conversation): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val params = conversation.toInsertParams()
            conversationQueries.insert(
                id = params.id,
                title = params.title,
                created_at = params.created_at,
                updated_at = params.updated_at,
                message_count = params.message_count,
                is_archived = params.is_archived,
                metadata = params.metadata
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to update conversation")
        }
    }

    override suspend fun deleteConversation(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            conversationQueries.delete(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to delete conversation")
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val timestamp = TimeHelper.currentTimeMillis()
            if (archived) {
                conversationQueries.archive(updated_at = timestamp, id = id)
            } else {
                conversationQueries.unarchive(updated_at = timestamp, id = id)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to archive conversation")
        }
    }

    override fun searchConversations(query: String): Flow<List<Conversation>> {
        return conversationQueries.searchByTitle(query)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { conversations -> conversations.map { it.toDomain() } }
    }
}
