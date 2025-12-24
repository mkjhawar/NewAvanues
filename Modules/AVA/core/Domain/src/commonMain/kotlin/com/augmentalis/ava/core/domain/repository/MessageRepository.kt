package com.augmentalis.ava.core.domain.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.model.MessageRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for message operations
 */
interface MessageRepository {
    
    /**
     * Get all messages in a conversation
     * @param conversationId Conversation VUID (Voice Universal Identifier)
     * @return Flow emitting messages ordered by timestamp
     */
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>
    
    /**
     * Get paginated messages (for infinite scroll)
     * @param conversationId Conversation VUID (Voice Universal Identifier)
     * @param limit Number of messages to fetch
     * @param offset Starting offset
     * @return Result containing list of messages
     */
    suspend fun getMessagesPaginated(
        conversationId: String,
        limit: Int,
        offset: Int
    ): Result<List<Message>>
    
    /**
     * Add a new message to conversation
     * @param message Message to add
     * @return Result containing created message with generated ID
     */
    suspend fun addMessage(message: Message): Result<Message>
    
    /**
     * Get messages by role (user/assistant/system)
     * @param conversationId Conversation VUID (Voice Universal Identifier)
     * @param role Filter by role
     * @return Flow emitting filtered messages
     */
    fun getMessagesByRole(conversationId: String, role: MessageRole): Flow<List<Message>>
    
    /**
     * Count messages in conversation
     * @param conversationId Conversation VUID (Voice Universal Identifier)
     * @return Message count
     */
    suspend fun getMessageCount(conversationId: String): Result<Int>

    /**
     * Get recent messages for context window (last N messages)
     * Used to provide conversation context to LLM
     * @param conversationId Conversation VUID (Voice Universal Identifier)
     * @param limit Number of recent messages to fetch (default 10)
     * @return Result containing list of recent messages ordered by timestamp
     */
    suspend fun getRecentMessagesForContext(
        conversationId: String,
        limit: Int = 10
    ): Result<List<Message>>
}
