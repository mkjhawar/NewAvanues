/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from ChatViewModel (SRP)
 */

package com.augmentalis.chat.state

import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.chat.coordinator.ConfidenceLearningState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatUIStateManager - Single Responsibility: Chat UI State Management
 *
 * Extracted from ChatViewModel as part of SOLID refactoring.
 * Centralizes all UI-related state for the chat screen:
 * - Message list and loading state
 * - Teach-AVA mode state
 * - History overlay state
 * - Pagination state
 * - Accessibility prompts
 * - App preference resolution
 *
 * Thread-safe: Uses StateFlow for all mutable state.
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
@Singleton
class ChatUIStateManager @Inject constructor() {

    // ==================== Message State ====================

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

    /**
     * Current active conversation ID.
     * Null if no conversation is active (first launch, or all conversations deleted).
     */
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    // ==================== Teach-AVA State ====================

    /**
     * Teach-AVA mode state (Phase 3).
     * Set to message ID when low confidence triggers teach mode, null otherwise.
     * Used to show "Teach-AVA" button in message bubble.
     */
    private val _teachAvaModeMessageId = MutableStateFlow<String?>(null)
    val teachAvaModeMessageId: StateFlow<String?> = _teachAvaModeMessageId.asStateFlow()

    /**
     * Bottom sheet visibility state (Phase 3, Task P3T04).
     * Controls whether the TeachAvaBottomSheet is shown.
     * Set to true when user taps "Teach AVA" button.
     */
    private val _showTeachBottomSheet = MutableStateFlow(false)
    val showTeachBottomSheet: StateFlow<Boolean> = _showTeachBottomSheet.asStateFlow()

    /**
     * Current message being taught (Phase 3, Task P3T04).
     * Holds the message ID for which the teach bottom sheet is displayed.
     * Used to retrieve the utterance text in the bottom sheet UI.
     */
    private val _currentTeachMessageId = MutableStateFlow<String?>(null)
    val currentTeachMessageId: StateFlow<String?> = _currentTeachMessageId.asStateFlow()

    // ==================== History Overlay State ====================

    /**
     * History overlay visibility state (Phase 4, Task P4T02).
     * Controls whether the conversation history overlay is shown.
     * Set to true when user triggers "show_history" intent.
     */
    private val _showHistoryOverlay = MutableStateFlow(false)
    val showHistoryOverlay: StateFlow<Boolean> = _showHistoryOverlay.asStateFlow()

    /**
     * List of all conversations (Phase 4, Task P4T02).
     * Sorted by lastMessageTimestamp descending (most recent first).
     * Updated when history overlay is shown or conversations are modified.
     */
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // ==================== Pagination State ====================

    /**
     * Current offset for message pagination (Phase 5, Task P5T04).
     * Tracks how many messages have been loaded so far.
     * Reset to 0 when switching conversations.
     */
    private val _messageOffset = MutableStateFlow(0)
    val messageOffset: StateFlow<Int> = _messageOffset.asStateFlow()

    /**
     * Flag indicating if there are more messages to load (Phase 5, Task P5T04).
     * Used to show/hide "Load More" button in UI.
     */
    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    /**
     * Total message count for current conversation (Phase 5, Task P5T04).
     * Used to determine if pagination is needed and for progress indicators.
     */
    private val _totalMessageCount = MutableStateFlow(0)
    val totalMessageCount: StateFlow<Int> = _totalMessageCount.asStateFlow()

    // ==================== Accessibility Prompts ====================

    /**
     * ADR-014 Phase 4: Show accessibility permission prompt.
     * Set to true when user tries a gesture/cursor command but accessibility service is not enabled.
     * UI should show a dialog prompting user to enable accessibility service.
     */
    private val _showAccessibilityPrompt = MutableStateFlow(false)
    val showAccessibilityPrompt: StateFlow<Boolean> = _showAccessibilityPrompt.asStateFlow()

    /**
     * ADR-014 Phase B (C4): App preference bottom sheet state.
     * Shown when multiple apps can handle a capability and user must choose.
     */
    private val _showAppPreferenceSheet = MutableStateFlow(false)
    val showAppPreferenceSheet: StateFlow<Boolean> = _showAppPreferenceSheet.asStateFlow()

    /**
     * Capability requiring app resolution.
     */
    private val _resolutionCapability = MutableStateFlow<String?>(null)
    val resolutionCapability: StateFlow<String?> = _resolutionCapability.asStateFlow()

    // ==================== Confidence Learning Dialog State ====================

    /**
     * Confidence learning dialog state (REQ-004).
     * When NLU confidence < threshold, this state is set to trigger the interactive learning dialog.
     * User can confirm interpretation or select from alternates.
     * null when no dialog should be shown.
     */
    private val _confidenceLearningDialogState = MutableStateFlow<ConfidenceLearningState?>(null)
    val confidenceLearningDialogState: StateFlow<ConfidenceLearningState?> = _confidenceLearningDialogState.asStateFlow()

    // ==================== Wake Word State ====================

    /**
     * StateFlow indicating wake word was detected - UI can show listening indicator.
     */
    private val _wakeWordDetected = MutableStateFlow<String?>(null)
    val wakeWordDetected: StateFlow<String?> = _wakeWordDetected.asStateFlow()

    // ==================== Message Operations ====================

    /**
     * Update the message list.
     */
    fun setMessages(messages: List<Message>) {
        _messages.value = messages
    }

    /**
     * Add a message to the list.
     */
    fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    /**
     * Update a specific message by ID.
     */
    fun updateMessage(messageId: String, update: (Message) -> Message) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) update(msg) else msg
        }
    }

    /**
     * Clear all messages.
     */
    fun clearMessages() {
        _messages.value = emptyList()
        _messageOffset.value = 0
        _hasMoreMessages.value = true
        _totalMessageCount.value = 0
    }

    // ==================== Loading State Operations ====================

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * Set error message (null to clear).
     */
    fun setError(message: String?) {
        _errorMessage.value = message
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // ==================== Conversation Operations ====================

    /**
     * Set the active conversation ID.
     */
    fun setActiveConversationId(conversationId: String?) {
        _activeConversationId.value = conversationId
    }

    /**
     * Update conversation list.
     */
    fun setConversations(conversations: List<Conversation>) {
        _conversations.value = conversations
    }

    // ==================== Teach-AVA Operations ====================

    /**
     * Set teach mode for a message (low confidence trigger).
     */
    fun setTeachAvaModeMessageId(messageId: String?) {
        _teachAvaModeMessageId.value = messageId
    }

    /**
     * Show/hide teach bottom sheet.
     */
    fun setShowTeachBottomSheet(show: Boolean) {
        _showTeachBottomSheet.value = show
    }

    /**
     * Set current message being taught.
     */
    fun setCurrentTeachMessageId(messageId: String?) {
        _currentTeachMessageId.value = messageId
    }

    /**
     * Open teach bottom sheet for a message.
     */
    fun openTeachBottomSheet(messageId: String) {
        _currentTeachMessageId.value = messageId
        _showTeachBottomSheet.value = true
    }

    /**
     * Close teach bottom sheet.
     */
    fun closeTeachBottomSheet() {
        _showTeachBottomSheet.value = false
        _currentTeachMessageId.value = null
    }

    // ==================== History Overlay Operations ====================

    /**
     * Show/hide history overlay.
     */
    fun setShowHistoryOverlay(show: Boolean) {
        _showHistoryOverlay.value = show
    }

    /**
     * Toggle history overlay visibility.
     */
    fun toggleHistoryOverlay() {
        _showHistoryOverlay.value = !_showHistoryOverlay.value
    }

    // ==================== Pagination Operations ====================

    /**
     * Update pagination state.
     */
    fun updatePagination(offset: Int, hasMore: Boolean, totalCount: Int) {
        _messageOffset.value = offset
        _hasMoreMessages.value = hasMore
        _totalMessageCount.value = totalCount
    }

    /**
     * Reset pagination state.
     */
    fun resetPagination() {
        _messageOffset.value = 0
        _hasMoreMessages.value = true
        _totalMessageCount.value = 0
    }

    // ==================== Accessibility Operations ====================

    /**
     * Show accessibility permission prompt.
     */
    fun showAccessibilityPrompt() {
        _showAccessibilityPrompt.value = true
    }

    /**
     * Hide accessibility permission prompt.
     */
    fun hideAccessibilityPrompt() {
        _showAccessibilityPrompt.value = false
    }

    /**
     * Show app preference sheet for capability resolution.
     */
    fun showAppPreferenceSheet(capability: String) {
        _resolutionCapability.value = capability
        _showAppPreferenceSheet.value = true
    }

    /**
     * Hide app preference sheet.
     */
    fun hideAppPreferenceSheet() {
        _showAppPreferenceSheet.value = false
        _resolutionCapability.value = null
    }

    // ==================== Confidence Learning Dialog Operations ====================

    /**
     * Set confidence learning dialog state.
     */
    fun setConfidenceLearningDialogState(state: ConfidenceLearningState?) {
        _confidenceLearningDialogState.value = state
    }

    /**
     * Clear confidence learning dialog.
     */
    fun clearConfidenceLearningDialog() {
        _confidenceLearningDialogState.value = null
    }

    // ==================== Wake Word Operations ====================

    /**
     * Set wake word detected state.
     */
    fun setWakeWordDetected(keyword: String?) {
        _wakeWordDetected.value = keyword
    }

    /**
     * Clear wake word detected state.
     */
    fun clearWakeWordDetected() {
        _wakeWordDetected.value = null
    }
}
