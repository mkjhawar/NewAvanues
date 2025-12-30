package com.augmentalis.ava.core.domain.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for conversation operations
 * Implementation in core.data layer
 */
interface ConversationRepository {
    
    /**
     * Get all conversations, ordered by most recent first
     * @return Flow emitting list of conversations (reactive)
     */
    fun getAllConversations(): Flow<List<Conversation>>
    
    /**
     * Get a single conversation by ID
     * @param id Conversation VUID (Voice Universal Identifier)
     * @return Result containing conversation or error
     */
    suspend fun getConversationById(id: String): Result<Conversation>
    
    /**
     * Create a new conversation
     * @param title Initial title (can be auto-generated from first message)
     * @return Result containing created conversation with generated ID
     */
    suspend fun createConversation(title: String): Result<Conversation>
    
    /**
     * Update conversation (typically to update title or metadata)
     * @param conversation Updated conversation
     * @return Result with success/error
     */
    suspend fun updateConversation(conversation: Conversation): Result<Unit>
    
    /**
     * Delete conversation (cascade deletes all messages)
     * @param id Conversation VUID (Voice Universal Identifier)
     * @return Result with success/error
     */
    suspend fun deleteConversation(id: String): Result<Unit>
    
    /**
     * Archive/unarchive conversation
     * @param id Conversation VUID (Voice Universal Identifier)
     * @param archived True to archive, false to unarchive
     * @return Result with success/error
     */
    suspend fun setArchived(id: String, archived: Boolean): Result<Unit>
    
    /**
     * Search conversations by title
     * @param query Search query
     * @return Flow emitting matching conversations
     */
    fun searchConversations(query: String): Flow<List<Conversation>>
}
