/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from ChatViewModel (SRP)
 */

package com.augmentalis.ava.features.chat.ui.state

import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.features.chat.ui.components.ConfidenceLearningState
import com.augmentalis.ava.features.chat.domain.SourceCitation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatUIStateManager - Single Responsibility: UI State Management
 *
 * Extracted from ChatViewModel as part of SOLID refactoring.
 * Manages all UI-related state for the Chat feature:
 * - Message list and loading states
 * - Teach-AVA mode state
 * - History overlay visibility
 * - Message pagination
 * - Accessibility prompts
 * - RAG source citations
 *
 * Thread-safe: All state mutations via MutableStateFlow.
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-12-17
 */
@Singleton
class ChatUIStateManager @Inject constructor() {

    // ==================== Message State ====================

    /**
     * Current active conversation ID.
     * Null if no conversation is active (first launch, or all conversations deleted).
     */
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    /**
     * List of messages for the active conversation.
     * Sorted by creation timestamp (oldest first).
     */
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    /**
     * Loading state for async operations (message sending, NLU classification).
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Error message state (null if no error).
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ==================== Teach-AVA State ====================

    /**
     * Teach-AVA mode state.
     * Set to message ID when low confidence triggers teach mode, null otherwise.
     */
    private val _teachAvaModeMessageId = MutableStateFlow<String?>(null)
    val teachAvaModeMessageId: StateFlow<String?> = _teachAvaModeMessageId.asStateFlow()

    /**
     * Bottom sheet visibility state.
     * Controls whether the TeachAvaBottomSheet is shown.
     */
    private val _showTeachBottomSheet = MutableStateFlow(false)
    val showTeachBottomSheet: StateFlow<Boolean> = _showTeachBottomSheet.asStateFlow()

    /**
     * Current message being taught.
     * Holds the message ID for which the teach bottom sheet is displayed.
     */
    private val _currentTeachMessageId = MutableStateFlow<String?>(null)
    val currentTeachMessageId: StateFlow<String?> = _currentTeachMessageId.asStateFlow()

    /**
     * Confidence learning dialog state.
     * When NLU confidence < threshold, this state triggers the interactive learning dialog.
     */
    private val _confidenceLearningDialogState = MutableStateFlow<ConfidenceLearningState?>(null)
    val confidenceLearningDialogState: StateFlow<ConfidenceLearningState?> = _confidenceLearningDialogState.asStateFlow()

    // ==================== History Overlay State ====================

    /**
     * History overlay visibility state.
     * Controls whether the conversation history overlay is shown.
     */
    private val _showHistoryOverlay = MutableStateFlow(false)
    val showHistoryOverlay: StateFlow<Boolean> = _showHistoryOverlay.asStateFlow()

    /**
     * List of all conversations.
     * Sorted by lastMessageTimestamp descending (most recent first).
     */
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // ==================== Pagination State ====================

    /**
     * Current offset for message pagination.
     * Tracks how many messages have been loaded so far.
     */
    private val _messageOffset = MutableStateFlow(0)
    val messageOffset: StateFlow<Int> = _messageOffset.asStateFlow()

    /**
     * Flag indicating if there are more messages to load.
     */
    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    /**
     * Total message count for current conversation.
     */
    private val _totalMessageCount = MutableStateFlow(0)
    val totalMessageCount: StateFlow<Int> = _totalMessageCount.asStateFlow()

    // ==================== Accessibility & App Preference State ====================

    /**
     * Show accessibility permission prompt.
     * Set to true when user tries a gesture/cursor command but accessibility service is not enabled.
     */
    private val _showAccessibilityPrompt = MutableStateFlow(false)
    val showAccessibilityPrompt: StateFlow<Boolean> = _showAccessibilityPrompt.asStateFlow()

    /**
     * App preference bottom sheet state.
     * Shown when multiple apps can handle a capability and user must choose.
     */
    private val _showAppPreferenceSheet = MutableStateFlow(false)
    val showAppPreferenceSheet: StateFlow<Boolean> = _showAppPreferenceSheet.asStateFlow()

    /**
     * Capability requiring app resolution.
     */
    private val _resolutionCapability = MutableStateFlow<String?>(null)
    val resolutionCapability: StateFlow<String?> = _resolutionCapability.asStateFlow()

    // ==================== RAG Source Citations ====================

    /**
     * Recent source citations from RAG retrieval.
     * Updated after each message with RAG context.
     */
    private val _recentSourceCitations = MutableStateFlow<List<SourceCitation>>(emptyList())
    val recentSourceCitations: StateFlow<List<SourceCitation>> = _recentSourceCitations.asStateFlow()

    // ==================== State Mutators ====================

    fun setActiveConversationId(id: String?) {
        _activeConversationId.value = id
    }

    fun setMessages(messages: List<Message>) {
        _messages.value = messages
    }

    fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    fun updateMessage(messageId: String, updater: (Message) -> Message) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) updater(msg) else msg
        }
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setError(error: String?) {
        _errorMessage.value = error
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Teach-AVA mutators

    fun setTeachAvaModeMessageId(messageId: String?) {
        _teachAvaModeMessageId.value = messageId
    }

    fun setShowTeachBottomSheet(show: Boolean) {
        _showTeachBottomSheet.value = show
    }

    fun setCurrentTeachMessageId(messageId: String?) {
        _currentTeachMessageId.value = messageId
    }

    fun setConfidenceLearningDialogState(state: ConfidenceLearningState?) {
        _confidenceLearningDialogState.value = state
    }

    // History overlay mutators

    fun setShowHistoryOverlay(show: Boolean) {
        _showHistoryOverlay.value = show
    }

    fun setConversations(conversations: List<Conversation>) {
        _conversations.value = conversations
    }

    // Pagination mutators

    fun setMessageOffset(offset: Int) {
        _messageOffset.value = offset
    }

    fun setHasMoreMessages(hasMore: Boolean) {
        _hasMoreMessages.value = hasMore
    }

    fun setTotalMessageCount(count: Int) {
        _totalMessageCount.value = count
    }

    fun resetPagination() {
        _messageOffset.value = 0
        _hasMoreMessages.value = true
        _totalMessageCount.value = 0
    }

    // Accessibility mutators

    fun setShowAccessibilityPrompt(show: Boolean) {
        _showAccessibilityPrompt.value = show
    }

    fun setShowAppPreferenceSheet(show: Boolean) {
        _showAppPreferenceSheet.value = show
    }

    fun setResolutionCapability(capability: String?) {
        _resolutionCapability.value = capability
    }

    // RAG source citations

    fun setRecentSourceCitations(citations: List<SourceCitation>) {
        _recentSourceCitations.value = citations
    }

    fun clearRecentSourceCitations() {
        _recentSourceCitations.value = emptyList()
    }

    // ==================== Convenience Methods ====================

    /**
     * Reset all UI state (called when switching conversations).
     */
    fun resetState() {
        _messages.value = emptyList()
        _isLoading.value = false
        _errorMessage.value = null
        _teachAvaModeMessageId.value = null
        _showTeachBottomSheet.value = false
        _currentTeachMessageId.value = null
        _confidenceLearningDialogState.value = null
        resetPagination()
        clearRecentSourceCitations()
    }
}
