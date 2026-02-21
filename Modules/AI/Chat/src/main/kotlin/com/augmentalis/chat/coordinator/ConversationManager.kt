/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from ChatViewModel (SRP)
 */

package com.augmentalis.chat.coordinator

import android.util.Log
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.prefs.ChatPreferences
import com.augmentalis.ava.core.data.prefs.ConversationMode
import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.repository.ConversationRepository
import com.augmentalis.ava.core.domain.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conversation Manager - Single Responsibility: Conversation Lifecycle Management
 *
 * Extracted from ChatViewModel as part of SOLID refactoring.
 * Handles all conversation-related operations:
 * - Conversation creation, loading, and switching
 * - Message loading with pagination
 * - Conversation list management with caching
 * - History overlay state coordination
 *
 * Thread-safe: Uses StateFlow for all mutable state.
 *
 * @param conversationRepository Repository for conversation CRUD
 * @param messageRepository Repository for message operations
 * @param chatPreferences User preferences for conversation mode, caching
 *
 * @author Manoj Jhawar
 * @since 2025-01-15
 */
@Singleton
class ConversationManager @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val chatPreferences: ChatPreferences
) : IConversationManager {

    companion object {
        private const val TAG = "ConversationManager"

        /** Default page size for message pagination */
        private const val DEFAULT_PAGE_SIZE = 50

        /** Default cache TTL for conversation list */
        private const val DEFAULT_CACHE_TTL_MS = 5000L

        /** Timeout for repository operations */
        private const val OPERATION_TIMEOUT_MS = 5000L
    }

    // ==================== State ====================

    private val _activeConversationId = MutableStateFlow<String?>(null)
    override val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    override val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    override val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _showHistoryOverlay = MutableStateFlow(false)
    override val showHistoryOverlay: StateFlow<Boolean> = _showHistoryOverlay.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ==================== Pagination State ====================

    private val _messageOffset = MutableStateFlow(0)
    override val messageOffset: StateFlow<Int> = _messageOffset.asStateFlow()

    private val _hasMoreMessages = MutableStateFlow(true)
    override val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    private val _totalMessageCount = MutableStateFlow(0)
    override val totalMessageCount: StateFlow<Int> = _totalMessageCount.asStateFlow()

    // ==================== Caching ====================

    @Volatile
    private var conversationsCacheTimestamp = 0L

    private val messagePageSize: Int
        get() = chatPreferences.getMessagePageSize()

    private val conversationsCacheTTL: Long
        get() = chatPreferences.getConversationsCacheTTL()

    // ==================== Conversation Operations ====================

    /**
     * Initialize conversation on app startup.
     * Respects conversation mode preference (APPEND or NEW).
     */
    override suspend fun initialize(): Result<Unit> {
        return try {
            Log.d(TAG, "Initializing conversation...")
            _isLoading.value = true
            _errorMessage.value = null

            val mode = chatPreferences.conversationMode.value
            Log.d(TAG, "Conversation mode: $mode")

            when (mode) {
                ConversationMode.APPEND -> initializeAppendMode()
                ConversationMode.NEW -> initializeNewMode()
            }

            // Load conversations for history overlay
            loadConversations()

            Log.i(TAG, "Conversation initialization complete")
            Result.Success(Unit)

        } catch (e: Exception) {
            val errorMsg = "Initialization failed: ${e.message}"
            _errorMessage.value = errorMsg
            Log.e(TAG, "Exception in initialize", e)
            Result.Error(e, errorMsg)
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun initializeAppendMode(): Result<Unit> {
        // Try to restore last active conversation
        val lastActiveId = chatPreferences.getLastActiveConversationId()
        Log.d(TAG, "Last active conversation ID: $lastActiveId")

        if (lastActiveId != null) {
            when (val result = conversationRepository.getConversationById(lastActiveId)) {
                is Result.Success -> {
                    _activeConversationId.value = lastActiveId
                    loadMessages(lastActiveId)
                    Log.d(TAG, "Restored last active conversation: ${result.data.title} (ID: $lastActiveId)")
                    return Result.Success(Unit)
                }
                is Result.Error -> {
                    Log.w(TAG, "Last active conversation not found, falling back to most recent")
                }
            }
        }

        // No valid last active ID, load most recent or create new
        val conversations = withTimeoutOrNull(OPERATION_TIMEOUT_MS) {
            conversationRepository.getAllConversations().first()
        } ?: emptyList()

        if (conversations.isNotEmpty()) {
            val mostRecent = conversations.maxByOrNull { it.updatedAt }
            if (mostRecent != null) {
                _activeConversationId.value = mostRecent.id
                loadMessages(mostRecent.id)
                chatPreferences.setLastActiveConversationId(mostRecent.id)
                Log.d(TAG, "Loaded most recent conversation: ${mostRecent.title} (ID: ${mostRecent.id})")
                return Result.Success(Unit)
            }
        }

        // No conversations exist, create a new one
        Log.d(TAG, "No conversations found, creating new conversation")
        return createNewConversationInternal("New Conversation")
    }

    private suspend fun initializeNewMode(): Result<Unit> {
        Log.d(TAG, "NEW mode: Creating new conversation")
        return createNewConversationInternal("New Conversation")
    }

    private suspend fun createNewConversationInternal(title: String): Result<Unit> {
        return when (val result = conversationRepository.createConversation(title)) {
            is Result.Success -> {
                _activeConversationId.value = result.data.id
                loadMessages(result.data.id)
                chatPreferences.setLastActiveConversationId(result.data.id)
                Log.d(TAG, "Created new conversation: ${result.data.title} (ID: ${result.data.id})")
                Result.Success(Unit)
            }
            is Result.Error -> {
                val errorMsg = "Failed to create conversation: ${result.message}"
                _errorMessage.value = errorMsg
                Log.e(TAG, errorMsg, result.exception)
                Result.Error(result.exception, errorMsg)
            }
        }
    }

    /**
     * Create a new conversation and switch to it.
     */
    override suspend fun createNewConversation(title: String): Result<String> {
        return try {
            _isLoading.value = true
            _errorMessage.value = null

            Log.d(TAG, "Creating new conversation: $title")

            when (val result = conversationRepository.createConversation(title)) {
                is Result.Success -> {
                    val conversation = result.data
                    Log.d(TAG, "Created conversation: ${conversation.title} (ID: ${conversation.id})")

                    // Invalidate conversations cache
                    invalidateConversationsCache()

                    // Switch to new conversation
                    switchConversation(conversation.id)

                    Log.i(TAG, "Successfully created and switched to new conversation")
                    Result.Success(conversation.id)
                }
                is Result.Error -> {
                    val errorMsg = "Failed to create conversation: ${result.message}"
                    _errorMessage.value = errorMsg
                    Log.e(TAG, errorMsg, result.exception)
                    Result.Error(result.exception, errorMsg)
                }
            }

        } catch (e: Exception) {
            val errorMsg = "Failed to create conversation: ${e.message}"
            _errorMessage.value = errorMsg
            Log.e(TAG, "Exception in createNewConversation", e)
            Result.Error(e, errorMsg)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Switch to a different conversation.
     */
    override suspend fun switchConversation(conversationId: String): Result<Unit> {
        return try {
            _isLoading.value = true
            _errorMessage.value = null

            Log.d(TAG, "Switching to conversation: $conversationId")

            when (val result = conversationRepository.getConversationById(conversationId)) {
                is Result.Success -> {
                    val conversation = result.data
                    Log.d(TAG, "Found conversation: ${conversation.title}")

                    // Clear current messages
                    clearMessages()

                    // Update active conversation ID
                    _activeConversationId.value = conversationId

                    // Save to preferences for restoration
                    chatPreferences.setLastActiveConversationId(conversationId)

                    // Load messages for new conversation
                    loadMessages(conversationId)

                    // Dismiss history overlay
                    dismissHistory()

                    Log.i(TAG, "Successfully switched to conversation: ${conversation.title}")
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    val errorMsg = "Conversation not found: ${result.message}"
                    _errorMessage.value = errorMsg
                    Log.e(TAG, errorMsg, result.exception)
                    Result.Error(result.exception, errorMsg)
                }
            }

        } catch (e: Exception) {
            val errorMsg = "Failed to switch conversation: ${e.message}"
            _errorMessage.value = errorMsg
            Log.e(TAG, "Exception in switchConversation", e)
            Result.Error(e, errorMsg)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Delete a conversation and all its messages.
     */
    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            _isLoading.value = true
            _errorMessage.value = null

            Log.d(TAG, "Deleting conversation: $conversationId")

            when (val result = conversationRepository.deleteConversation(conversationId)) {
                is Result.Success -> {
                    Log.i(TAG, "Conversation deleted successfully")

                    // Invalidate conversations cache
                    invalidateConversationsCache()
                    loadConversations()

                    // If deleted conversation was active, switch to most recent
                    if (_activeConversationId.value == conversationId) {
                        val convos = _conversations.value
                        if (convos.isNotEmpty()) {
                            switchConversation(convos.first().id)
                        } else {
                            createNewConversation()
                        }
                    }

                    Result.Success(Unit)
                }
                is Result.Error -> {
                    val errorMsg = "Failed to delete conversation: ${result.message}"
                    _errorMessage.value = errorMsg
                    Log.e(TAG, errorMsg, result.exception)
                    Result.Error(result.exception, errorMsg)
                }
            }

        } catch (e: Exception) {
            val errorMsg = "Failed to delete conversation: ${e.message}"
            _errorMessage.value = errorMsg
            Log.e(TAG, "Exception in deleteConversation", e)
            Result.Error(e, errorMsg)
        } finally {
            _isLoading.value = false
        }
    }

    // ==================== Message Operations ====================

    /**
     * Load messages for a conversation with pagination.
     */
    override suspend fun loadMessages(conversationId: String): Result<Unit> {
        return try {
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "Loading messages for conversation: $conversationId")

            // Reset pagination state
            _messageOffset.value = 0
            _hasMoreMessages.value = true
            _totalMessageCount.value = 0

            // Get total message count
            var currentTotalCount = 0
            when (val countResult = messageRepository.getMessageCount(conversationId)) {
                is Result.Success -> {
                    currentTotalCount = countResult.data
                    _totalMessageCount.value = currentTotalCount
                    Log.d(TAG, "Total messages in conversation: $currentTotalCount")
                }
                is Result.Error -> {
                    Log.w(TAG, "Failed to get message count: ${countResult.message}")
                }
            }

            // Load initial page using pagination
            when (val result = messageRepository.getMessagesPaginated(
                conversationId = conversationId,
                limit = messagePageSize,
                offset = 0
            )) {
                is Result.Success -> {
                    val loadedMessages = result.data
                    _messages.value = loadedMessages
                    val newOffset = loadedMessages.size

                    // Update hasMore flag
                    val hasMore = loadedMessages.size >= messagePageSize && newOffset < currentTotalCount
                    _messageOffset.value = newOffset
                    _hasMoreMessages.value = hasMore
                    _totalMessageCount.value = currentTotalCount

                    val loadTime = System.currentTimeMillis() - startTime
                    Log.i(TAG, "=== Message Load Performance ===")
                    Log.i(TAG, "  Loaded: ${loadedMessages.size} messages")
                    Log.i(TAG, "  Total: $currentTotalCount messages")
                    Log.i(TAG, "  Load time: ${loadTime}ms")
                    Log.i(TAG, "  Has more: $hasMore")

                    Result.Success(Unit)
                }
                is Result.Error -> {
                    val errorMsg = "Failed to load messages: ${result.message}"
                    _errorMessage.value = errorMsg
                    Log.e(TAG, errorMsg, result.exception)
                    Result.Error(result.exception, errorMsg)
                }
            }

        } catch (e: Exception) {
            val errorMsg = "Failed to load messages: ${e.message}"
            _errorMessage.value = errorMsg
            Log.e(TAG, "Exception in loadMessages", e)
            Result.Error(e, errorMsg)
        }
    }

    /**
     * Load more messages (pagination).
     */
    override suspend fun loadMoreMessages(): Result<Unit> {
        // Early return if no more messages or already loading
        if (!_hasMoreMessages.value || _isLoading.value) {
            Log.d(TAG, "loadMoreMessages: skipped (hasMore=${_hasMoreMessages.value}, isLoading=${_isLoading.value})")
            return Result.Success(Unit)
        }

        val conversationId = _activeConversationId.value
        if (conversationId == null) {
            Log.w(TAG, "loadMoreMessages: No active conversation")
            return Result.Error(IllegalStateException("No active conversation"), "No active conversation")
        }

        return try {
            _isLoading.value = true
            _errorMessage.value = null

            val startTime = System.currentTimeMillis()
            val currentOffset = _messageOffset.value
            val currentTotalCount = _totalMessageCount.value
            Log.d(TAG, "Loading more messages: offset=$currentOffset, limit=$messagePageSize")

            when (val result = messageRepository.getMessagesPaginated(
                conversationId = conversationId,
                limit = messagePageSize,
                offset = currentOffset
            )) {
                is Result.Success -> {
                    val newMessages = result.data
                    if (newMessages.isNotEmpty()) {
                        // Prepend older messages to the beginning
                        val currentMessages = _messages.value
                        _messages.value = newMessages + currentMessages

                        // Update pagination state
                        val newOffset = minOf(currentOffset + newMessages.size, currentTotalCount)
                        val hasMore = newMessages.size >= messagePageSize && newOffset < currentTotalCount
                        _messageOffset.value = newOffset
                        _hasMoreMessages.value = hasMore

                        val loadTime = System.currentTimeMillis() - startTime
                        Log.i(TAG, "=== Load More Messages Performance ===")
                        Log.i(TAG, "  Loaded: ${newMessages.size} messages")
                        Log.i(TAG, "  Total loaded: $newOffset/$currentTotalCount messages")
                        Log.i(TAG, "  Load time: ${loadTime}ms")
                        Log.i(TAG, "  Has more: $hasMore")
                    } else {
                        _hasMoreMessages.value = false
                        Log.d(TAG, "No more messages to load")
                    }
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    val errorMsg = "Failed to load more messages: ${result.message}"
                    _errorMessage.value = errorMsg
                    Log.e(TAG, errorMsg, result.exception)
                    Result.Error(result.exception, errorMsg)
                }
            }

        } catch (e: Exception) {
            val errorMsg = "Failed to load more messages: ${e.message}"
            _errorMessage.value = errorMsg
            Log.e(TAG, "Exception in loadMoreMessages", e)
            Result.Error(e, errorMsg)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Clear all messages and reset pagination.
     */
    override fun clearMessages() {
        _messages.value = emptyList()
        _messageOffset.value = 0
        _hasMoreMessages.value = true
        _totalMessageCount.value = 0
    }

    // ==================== History Overlay Operations ====================

    /**
     * Show conversation history overlay.
     */
    override fun showHistory() {
        _showHistoryOverlay.value = true
        Log.d(TAG, "History overlay shown")
    }

    /**
     * Dismiss conversation history overlay.
     */
    override fun dismissHistory() {
        _showHistoryOverlay.value = false
        Log.d(TAG, "History overlay dismissed")
    }

    /**
     * Load all conversations from repository with caching.
     */
    override suspend fun loadConversations(): Result<Unit> {
        return try {
            // Check cache freshness
            val now = System.currentTimeMillis()
            if (_conversations.value.isNotEmpty() &&
                (now - conversationsCacheTimestamp) < conversationsCacheTTL) {
                val cacheAge = now - conversationsCacheTimestamp
                Log.d(TAG, "Using cached conversations (age: ${cacheAge}ms, TTL: ${conversationsCacheTTL}ms)")
                return Result.Success(Unit)
            }

            Log.d(TAG, "Loading conversations from repository...")
            val startTime = System.currentTimeMillis()

            // Fetch all conversations with timeout
            val conversations = withTimeoutOrNull(OPERATION_TIMEOUT_MS) {
                conversationRepository.getAllConversations().first()
            } ?: emptyList()

            // Sort by updatedAt descending
            val sortedConversations = if (conversations.isEmpty()) {
                Log.d(TAG, "Conversations list is empty")
                emptyList()
            } else {
                conversations.sortedByDescending { it.updatedAt }
            }

            // Update state and cache
            _conversations.value = sortedConversations
            conversationsCacheTimestamp = now

            val loadTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Loaded ${sortedConversations.size} conversations in ${loadTime}ms")

            Result.Success(Unit)

        } catch (e: Exception) {
            val errorMsg = "Failed to load conversations: ${e.message}"
            _errorMessage.value = errorMsg
            Log.e(TAG, "Exception in loadConversations", e)
            Result.Error(e, errorMsg)
        }
    }

    // ==================== Utility ====================

    /**
     * Clear error message.
     */
    override fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Save active conversation ID to preferences.
     */
    override fun saveActiveConversationId(conversationId: String?) {
        if (conversationId != null) {
            chatPreferences.setLastActiveConversationId(conversationId)
        }
    }

    /**
     * Invalidate conversations cache.
     */
    override fun invalidateConversationsCache() {
        conversationsCacheTimestamp = 0L
        Log.d(TAG, "Conversations cache invalidated")
    }
}
