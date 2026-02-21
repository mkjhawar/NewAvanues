/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Desktop implementation of Conversation Manager.
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * Desktop (JVM) implementation of IConversationManager.
 *
 * Manages conversation lifecycle on desktop:
 * - In-memory conversation storage (can be extended to SQLite)
 * - Message pagination
 * - History overlay state
 *
 * For persistence, this can be extended to use:
 * - SQLDelight with JVM SQLite driver
 * - File-based JSON storage
 * - REST API backend
 *
 * @author Manoj Jhawar
 * @since 2025-01-16
 */
class ConversationManagerDesktop : IConversationManager {

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

    // Pagination state
    private val _messageOffset = MutableStateFlow(0)
    override val messageOffset: StateFlow<Int> = _messageOffset.asStateFlow()

    private val _hasMoreMessages = MutableStateFlow(false)
    override val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    private val _totalMessageCount = MutableStateFlow(0)
    override val totalMessageCount: StateFlow<Int> = _totalMessageCount.asStateFlow()

    // ==================== Internal Storage ====================

    // In-memory conversation storage (indexed by conversation ID)
    private val conversationStore = mutableMapOf<String, Conversation>()

    // In-memory message storage (indexed by conversation ID -> list of messages)
    private val messageStore = mutableMapOf<String, MutableList<Message>>()

    // Pagination settings
    private val pageSize = 50

    // Mutex for thread-safe operations
    private val storageMutex = Mutex()

    // Preferences (simulated)
    private var savedActiveConversationId: String? = null
    private var conversationMode = ConversationMode.APPEND

    enum class ConversationMode {
        APPEND, // Continue last conversation
        NEW     // Always start fresh
    }

    // ==================== Initialization ====================

    override suspend fun initialize(): Result<Unit> = storageMutex.withLock {
        try {
            _isLoading.value = true
            println("[ConversationManagerDesktop] Initializing...")

            when (conversationMode) {
                ConversationMode.APPEND -> {
                    // Try to restore last active conversation
                    val lastId = savedActiveConversationId
                    if (lastId != null && conversationStore.containsKey(lastId)) {
                        _activeConversationId.value = lastId
                        loadMessages(lastId)
                    } else if (conversationStore.isNotEmpty()) {
                        // Use most recent conversation
                        val mostRecent = conversationStore.values
                            .maxByOrNull { it.updatedAt }
                        mostRecent?.let {
                            _activeConversationId.value = it.id
                            loadMessages(it.id)
                        }
                    } else {
                        // Create new conversation
                        createNewConversation()
                    }
                }
                ConversationMode.NEW -> {
                    createNewConversation()
                }
            }

            loadConversations()

            _isLoading.value = false
            println("[ConversationManagerDesktop] Initialization complete")
            Result.Success(Unit)
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = "Failed to initialize: ${e.message}"
            Result.Error(
                exception = e,
                message = "Initialization failed: ${e.message}"
            )
        }
    }

    // ==================== Conversation Operations ====================

    override suspend fun createNewConversation(title: String): Result<String> = storageMutex.withLock {
        _createNewConversation(title)
    }

    /**
     * Internal create-new-conversation that assumes the storageMutex is already held.
     * Called directly from deleteConversation to avoid re-entrant lock acquisition.
     */
    private fun _createNewConversation(title: String = "New Conversation"): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val now = Clock.System.now().toEpochMilliseconds()

            val conversation = Conversation(
                id = id,
                title = title,
                createdAt = now,
                updatedAt = now,
                messageCount = 0
            )

            conversationStore[id] = conversation
            messageStore[id] = mutableListOf()

            _activeConversationId.value = id
            _messages.value = emptyList()
            _messageOffset.value = 0
            _hasMoreMessages.value = false
            _totalMessageCount.value = 0

            // Update conversation list
            _conversations.value = conversationStore.values
                .sortedByDescending { it.updatedAt }
                .toList()

            saveActiveConversationId(id)

            println("[ConversationManagerDesktop] Created new conversation: $id")
            Result.Success(id)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to create conversation: ${e.message}"
            Result.Error(
                exception = e,
                message = "Failed to create conversation: ${e.message}"
            )
        }
    }

    override suspend fun switchConversation(conversationId: String): Result<Unit> = storageMutex.withLock {
        _switchConversation(conversationId)
    }

    /**
     * Internal switch-conversation that assumes the storageMutex is already held.
     * Called directly from deleteConversation to avoid re-entrant lock acquisition.
     */
    private fun _switchConversation(conversationId: String): Result<Unit> {
        return try {
            if (!conversationStore.containsKey(conversationId)) {
                return Result.Error(
                    exception = IllegalArgumentException("Conversation not found"),
                    message = "Conversation not found: $conversationId"
                )
            }

            _activeConversationId.value = conversationId
            // loadMessages does not acquire storageMutex, so it is safe to call here
            val allMessages = messageStore[conversationId] ?: mutableListOf()
            _totalMessageCount.value = allMessages.size
            _messageOffset.value = 0
            val startIndex = maxOf(0, allMessages.size - pageSize)
            _messages.value = allMessages.subList(startIndex, allMessages.size).toList()
            _hasMoreMessages.value = startIndex > 0

            saveActiveConversationId(conversationId)

            println("[ConversationManagerDesktop] Switched to conversation: $conversationId")
            Result.Success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to switch conversation: ${e.message}"
            Result.Error(
                exception = e,
                message = "Failed to switch conversation: ${e.message}"
            )
        }
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> = storageMutex.withLock {
        try {
            conversationStore.remove(conversationId)
            messageStore.remove(conversationId)

            // Update conversation list
            _conversations.value = conversationStore.values
                .sortedByDescending { it.updatedAt }
                .toList()

            // If deleted conversation was active, switch to most recent.
            // Call private helpers directly — storageMutex is already held here;
            // calling the public overrides would deadlock (Mutex is not re-entrant).
            if (_activeConversationId.value == conversationId) {
                val mostRecent = conversationStore.values.maxByOrNull { it.updatedAt }
                if (mostRecent != null) {
                    _switchConversation(mostRecent.id)
                } else {
                    _createNewConversation()
                }
            }

            println("[ConversationManagerDesktop] Deleted conversation: $conversationId")
            Result.Success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to delete conversation: ${e.message}"
            Result.Error(
                exception = e,
                message = "Failed to delete conversation: ${e.message}"
            )
        }
    }

    // ==================== Message Operations ====================

    override suspend fun loadMessages(conversationId: String): Result<Unit> {
        try {
            val allMessages = messageStore[conversationId] ?: mutableListOf()

            _totalMessageCount.value = allMessages.size
            _messageOffset.value = 0

            // Load most recent messages (last pageSize)
            val startIndex = maxOf(0, allMessages.size - pageSize)
            _messages.value = allMessages.subList(startIndex, allMessages.size).toList()
            _hasMoreMessages.value = startIndex > 0

            println("[ConversationManagerDesktop] Loaded ${_messages.value.size} messages for conversation: $conversationId")
            return Result.Success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load messages: ${e.message}"
            return Result.Error(
                exception = e,
                message = "Failed to load messages: ${e.message}"
            )
        }
    }

    override suspend fun loadMoreMessages(): Result<Unit> = storageMutex.withLock {
        try {
            val conversationId = _activeConversationId.value ?: return Result.Success(Unit)
            val allMessages = messageStore[conversationId] ?: return Result.Success(Unit)

            if (!_hasMoreMessages.value) {
                return Result.Success(Unit)
            }

            val currentMessages = _messages.value
            val currentOldestIndex = allMessages.size - currentMessages.size - _messageOffset.value

            // Calculate new range
            val newStartIndex = maxOf(0, currentOldestIndex - pageSize)
            val newEndIndex = currentOldestIndex

            if (newStartIndex < newEndIndex) {
                val olderMessages = allMessages.subList(newStartIndex, newEndIndex)
                _messages.value = olderMessages + currentMessages
                _messageOffset.value = allMessages.size - _messages.value.size
                _hasMoreMessages.value = newStartIndex > 0
            }

            println("[ConversationManagerDesktop] Loaded more messages. Total: ${_messages.value.size}")
            return Result.Success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load more messages: ${e.message}"
            return Result.Error(
                exception = e,
                message = "Failed to load more messages: ${e.message}"
            )
        }
    }

    override fun clearMessages() {
        _messages.value = emptyList()
        _messageOffset.value = 0
        _hasMoreMessages.value = false
        _totalMessageCount.value = 0
    }

    /**
     * Add a message to the current conversation.
     *
     * @param message Message to add
     * @return Result indicating success or failure
     */
    suspend fun addMessage(message: Message): Result<Unit> = storageMutex.withLock {
        try {
            val conversationId = _activeConversationId.value
                ?: return Result.Error(
                    exception = IllegalStateException("No active conversation"),
                    message = "No active conversation"
                )

            // Add to storage
            val messages = messageStore.getOrPut(conversationId) { mutableListOf() }
            messages.add(message)

            // Update conversation
            conversationStore[conversationId]?.let { conv ->
                conversationStore[conversationId] = conv.copy(
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                    messageCount = messages.size
                )
            }

            // Update state
            _messages.value = _messages.value + message
            _totalMessageCount.value = messages.size

            // Update conversation list
            _conversations.value = conversationStore.values
                .sortedByDescending { it.updatedAt }
                .toList()

            println("[ConversationManagerDesktop] Added message to conversation: $conversationId")
            return Result.Success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to add message: ${e.message}"
            return Result.Error(
                exception = e,
                message = "Failed to add message: ${e.message}"
            )
        }
    }

    // ==================== History Overlay ====================

    override fun showHistory() {
        _showHistoryOverlay.value = true
        println("[ConversationManagerDesktop] History overlay shown")
    }

    override fun dismissHistory() {
        _showHistoryOverlay.value = false
        println("[ConversationManagerDesktop] History overlay dismissed")
    }

    override suspend fun loadConversations(): Result<Unit> {
        try {
            _conversations.value = conversationStore.values
                .sortedByDescending { it.updatedAt }
                .toList()

            println("[ConversationManagerDesktop] Loaded ${_conversations.value.size} conversations")
            return Result.Success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load conversations: ${e.message}"
            return Result.Error(
                exception = e,
                message = "Failed to load conversations: ${e.message}"
            )
        }
    }

    // ==================== Utility ====================

    override fun clearError() {
        _errorMessage.value = null
    }

    override fun saveActiveConversationId(conversationId: String?) {
        savedActiveConversationId = conversationId
    }

    override fun invalidateConversationsCache() {
        // In-memory implementation doesn't need cache invalidation
        // This would be relevant for database-backed implementations
    }

    /**
     * Set conversation mode.
     *
     * @param mode APPEND to continue last conversation, NEW to always start fresh
     */
    fun setConversationMode(mode: ConversationMode) {
        conversationMode = mode
    }

    /**
     * Get all messages for a conversation (for export).
     *
     * @param conversationId Conversation ID
     * @return List of all messages
     */
    fun getAllMessages(conversationId: String): List<Message> {
        return messageStore[conversationId]?.toList() ?: emptyList()
    }

    companion object {
        @Volatile
        private var INSTANCE: ConversationManagerDesktop? = null

        /**
         * Get singleton instance of ConversationManagerDesktop.
         *
         * @return Singleton instance
         */
        fun getInstance(): ConversationManagerDesktop {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConversationManagerDesktop().also {
                    INSTANCE = it
                }
            }
        }
    }
}
