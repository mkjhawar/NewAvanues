/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Conversation manager interface for cross-platform
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import kotlinx.coroutines.flow.StateFlow

/**
 * Conversation Manager Interface - Cross-platform conversation coordination
 *
 * Abstracts conversation lifecycle operations for cross-platform use in KMP.
 * Provides:
 * - Conversation creation, loading, and switching
 * - Message loading with pagination
 * - Conversation list management
 * - History overlay state coordination
 *
 * SOLID Principle: Single Responsibility
 * - Extracted from ChatViewModel for conversation lifecycle concerns
 * - Handles all conversation state management and persistence
 *
 * @see ConversationManager for Android implementation
 *
 * @author Manoj Jhawar
 * @since 2025-01-15
 */
interface IConversationManager {

    // ==================== State ====================

    /**
     * Current active conversation ID.
     * Null if no conversation is active.
     */
    val activeConversationId: StateFlow<String?>

    /**
     * List of messages for the active conversation.
     * Sorted by timestamp (oldest first).
     */
    val messages: StateFlow<List<Message>>

    /**
     * List of all conversations.
     * Sorted by updatedAt descending (most recent first).
     */
    val conversations: StateFlow<List<Conversation>>

    /**
     * History overlay visibility state.
     * True when conversation history overlay is shown.
     */
    val showHistoryOverlay: StateFlow<Boolean>

    /**
     * Loading state for conversation operations.
     */
    val isLoading: StateFlow<Boolean>

    /**
     * Error message state.
     */
    val errorMessage: StateFlow<String?>

    // ==================== Pagination State ====================

    /**
     * Current offset for message pagination.
     * Tracks how many messages have been loaded.
     */
    val messageOffset: StateFlow<Int>

    /**
     * Flag indicating if there are more messages to load.
     */
    val hasMoreMessages: StateFlow<Boolean>

    /**
     * Total message count for current conversation.
     */
    val totalMessageCount: StateFlow<Int>

    // ==================== Conversation Operations ====================

    /**
     * Initialize conversation on app startup.
     * Respects conversation mode preference (APPEND or NEW).
     *
     * Flow:
     * 1. Check conversation mode from preferences
     * 2. If APPEND: Restore last active or most recent conversation
     * 3. If NEW: Create fresh conversation
     * 4. Load messages for active conversation
     * 5. Load conversation list for history overlay
     *
     * @return Result indicating success or failure
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Create a new conversation and switch to it.
     *
     * @param title Initial conversation title
     * @return Result with new conversation ID on success
     */
    suspend fun createNewConversation(title: String = "New Conversation"): Result<String>

    /**
     * Switch to a different conversation.
     *
     * @param conversationId ID of conversation to switch to
     * @return Result indicating success or failure
     */
    suspend fun switchConversation(conversationId: String): Result<Unit>

    /**
     * Delete a conversation and all its messages.
     * If deleted conversation was active, switches to most recent.
     *
     * @param conversationId ID of conversation to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit>

    // ==================== Message Operations ====================

    /**
     * Load messages for the active conversation.
     * Uses pagination for performance.
     *
     * @param conversationId ID of conversation to load messages for
     * @return Result indicating success or failure
     */
    suspend fun loadMessages(conversationId: String): Result<Unit>

    /**
     * Load more messages (pagination).
     * Prepends older messages to the list.
     *
     * @return Result indicating success or failure
     */
    suspend fun loadMoreMessages(): Result<Unit>

    /**
     * Clear all messages and reset pagination.
     */
    fun clearMessages()

    // ==================== History Overlay Operations ====================

    /**
     * Show conversation history overlay.
     * Loads/refreshes conversation list.
     */
    fun showHistory()

    /**
     * Dismiss conversation history overlay.
     */
    fun dismissHistory()

    /**
     * Load all conversations from repository.
     * Uses caching with TTL for performance.
     *
     * @return Result indicating success or failure
     */
    suspend fun loadConversations(): Result<Unit>

    // ==================== Utility ====================

    /**
     * Clear error message.
     */
    fun clearError()

    /**
     * Save active conversation ID to preferences for restoration.
     *
     * @param conversationId ID to save (null to clear)
     */
    fun saveActiveConversationId(conversationId: String?)

    /**
     * Invalidate conversations cache.
     * Call when conversations are modified.
     */
    fun invalidateConversationsCache()
}
