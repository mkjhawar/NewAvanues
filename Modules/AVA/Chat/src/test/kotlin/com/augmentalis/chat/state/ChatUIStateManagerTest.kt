/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Unit tests for ChatUIStateManager (SOLID refactoring)
 */

package com.augmentalis.chat.state

import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.model.MessageRole
import com.augmentalis.chat.components.AlternateIntent
import com.augmentalis.chat.components.ConfidenceLearningState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for ChatUIStateManager.
 *
 * Tests cover:
 * - Message state management
 * - Loading and error state
 * - Teach-AVA mode state
 * - History overlay state
 * - Pagination state
 * - Accessibility prompts
 * - Confidence learning dialog
 * - Wake word state
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-12-18
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatUIStateManagerTest {

    private lateinit var stateManager: ChatUIStateManager

    @Before
    fun setUp() {
        stateManager = ChatUIStateManager()
    }

    // ==================== Message State Tests ====================

    @Test
    fun `initial messages list is empty`() {
        assertTrue(stateManager.messages.value.isEmpty())
    }

    @Test
    fun `setMessages updates message list`() {
        val messages = listOf(
            createTestMessage("msg1", "Hello"),
            createTestMessage("msg2", "World")
        )

        stateManager.setMessages(messages)

        assertEquals(2, stateManager.messages.value.size)
        assertEquals("msg1", stateManager.messages.value[0].id)
        assertEquals("msg2", stateManager.messages.value[1].id)
    }

    @Test
    fun `addMessage appends to existing messages`() {
        val initialMessages = listOf(createTestMessage("msg1", "Hello"))
        stateManager.setMessages(initialMessages)

        val newMessage = createTestMessage("msg2", "World")
        stateManager.addMessage(newMessage)

        assertEquals(2, stateManager.messages.value.size)
        assertEquals("msg2", stateManager.messages.value[1].id)
    }

    @Test
    fun `updateMessage modifies specific message`() {
        val messages = listOf(
            createTestMessage("msg1", "Hello"),
            createTestMessage("msg2", "World")
        )
        stateManager.setMessages(messages)

        stateManager.updateMessage("msg1") { it.copy(content = "Updated") }

        assertEquals("Updated", stateManager.messages.value[0].content)
        assertEquals("World", stateManager.messages.value[1].content)
    }

    @Test
    fun `clearMessages resets all message state`() {
        stateManager.setMessages(listOf(createTestMessage("msg1", "Hello")))
        stateManager.updatePagination(10, false, 100)

        stateManager.clearMessages()

        assertTrue(stateManager.messages.value.isEmpty())
        assertEquals(0, stateManager.messageOffset.value)
        assertTrue(stateManager.hasMoreMessages.value)
        assertEquals(0, stateManager.totalMessageCount.value)
    }

    // ==================== Loading State Tests ====================

    @Test
    fun `initial loading state is false`() {
        assertFalse(stateManager.isLoading.value)
    }

    @Test
    fun `setLoading updates loading state`() {
        stateManager.setLoading(true)
        assertTrue(stateManager.isLoading.value)

        stateManager.setLoading(false)
        assertFalse(stateManager.isLoading.value)
    }

    @Test
    fun `initial error message is null`() {
        assertNull(stateManager.errorMessage.value)
    }

    @Test
    fun `setError updates error message`() {
        stateManager.setError("Test error")
        assertEquals("Test error", stateManager.errorMessage.value)
    }

    @Test
    fun `clearError sets error to null`() {
        stateManager.setError("Test error")
        stateManager.clearError()
        assertNull(stateManager.errorMessage.value)
    }

    // ==================== Conversation State Tests ====================

    @Test
    fun `initial active conversation is null`() {
        assertNull(stateManager.activeConversationId.value)
    }

    @Test
    fun `setActiveConversationId updates active conversation`() {
        stateManager.setActiveConversationId("conv123")
        assertEquals("conv123", stateManager.activeConversationId.value)
    }

    @Test
    fun `setConversations updates conversation list`() {
        val conversations = listOf(
            createTestConversation("conv1"),
            createTestConversation("conv2")
        )

        stateManager.setConversations(conversations)

        assertEquals(2, stateManager.conversations.value.size)
    }

    // ==================== Teach-AVA State Tests ====================

    @Test
    fun `initial teach mode is disabled`() {
        assertNull(stateManager.teachAvaModeMessageId.value)
        assertFalse(stateManager.showTeachBottomSheet.value)
        assertNull(stateManager.currentTeachMessageId.value)
    }

    @Test
    fun `setTeachAvaModeMessageId enables teach mode for message`() {
        stateManager.setTeachAvaModeMessageId("msg123")
        assertEquals("msg123", stateManager.teachAvaModeMessageId.value)
    }

    @Test
    fun `openTeachBottomSheet sets both states`() {
        stateManager.openTeachBottomSheet("msg123")

        assertTrue(stateManager.showTeachBottomSheet.value)
        assertEquals("msg123", stateManager.currentTeachMessageId.value)
    }

    @Test
    fun `closeTeachBottomSheet resets both states`() {
        stateManager.openTeachBottomSheet("msg123")
        stateManager.closeTeachBottomSheet()

        assertFalse(stateManager.showTeachBottomSheet.value)
        assertNull(stateManager.currentTeachMessageId.value)
    }

    // ==================== History Overlay Tests ====================

    @Test
    fun `initial history overlay is hidden`() {
        assertFalse(stateManager.showHistoryOverlay.value)
    }

    @Test
    fun `setShowHistoryOverlay updates visibility`() {
        stateManager.setShowHistoryOverlay(true)
        assertTrue(stateManager.showHistoryOverlay.value)

        stateManager.setShowHistoryOverlay(false)
        assertFalse(stateManager.showHistoryOverlay.value)
    }

    @Test
    fun `toggleHistoryOverlay toggles visibility`() {
        assertFalse(stateManager.showHistoryOverlay.value)

        stateManager.toggleHistoryOverlay()
        assertTrue(stateManager.showHistoryOverlay.value)

        stateManager.toggleHistoryOverlay()
        assertFalse(stateManager.showHistoryOverlay.value)
    }

    // ==================== Pagination Tests ====================

    @Test
    fun `initial pagination state is reset`() {
        assertEquals(0, stateManager.messageOffset.value)
        assertTrue(stateManager.hasMoreMessages.value)
        assertEquals(0, stateManager.totalMessageCount.value)
    }

    @Test
    fun `updatePagination sets all pagination values`() {
        stateManager.updatePagination(offset = 50, hasMore = false, totalCount = 100)

        assertEquals(50, stateManager.messageOffset.value)
        assertFalse(stateManager.hasMoreMessages.value)
        assertEquals(100, stateManager.totalMessageCount.value)
    }

    @Test
    fun `resetPagination resets to initial state`() {
        stateManager.updatePagination(50, false, 100)
        stateManager.resetPagination()

        assertEquals(0, stateManager.messageOffset.value)
        assertTrue(stateManager.hasMoreMessages.value)
        assertEquals(0, stateManager.totalMessageCount.value)
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun `initial accessibility prompt is hidden`() {
        assertFalse(stateManager.showAccessibilityPrompt.value)
    }

    @Test
    fun `showAccessibilityPrompt sets state to true`() {
        stateManager.showAccessibilityPrompt()
        assertTrue(stateManager.showAccessibilityPrompt.value)
    }

    @Test
    fun `hideAccessibilityPrompt sets state to false`() {
        stateManager.showAccessibilityPrompt()
        stateManager.hideAccessibilityPrompt()
        assertFalse(stateManager.showAccessibilityPrompt.value)
    }

    @Test
    fun `showAppPreferenceSheet sets capability and visibility`() {
        stateManager.showAppPreferenceSheet("music_playback")

        assertTrue(stateManager.showAppPreferenceSheet.value)
        assertEquals("music_playback", stateManager.resolutionCapability.value)
    }

    @Test
    fun `hideAppPreferenceSheet clears capability and visibility`() {
        stateManager.showAppPreferenceSheet("music_playback")
        stateManager.hideAppPreferenceSheet()

        assertFalse(stateManager.showAppPreferenceSheet.value)
        assertNull(stateManager.resolutionCapability.value)
    }

    // ==================== Confidence Learning Dialog Tests ====================

    @Test
    fun `initial confidence learning dialog is null`() {
        assertNull(stateManager.confidenceLearningDialogState.value)
    }

    @Test
    fun `setConfidenceLearningDialogState updates state`() {
        val state = ConfidenceLearningState(
            userInput = "play music",
            interpretedIntent = "media.play",
            confidence = 0.65f,
            alternateIntents = listOf(
                AlternateIntent("media.pause", "Pause", 0.45f)
            )
        )

        stateManager.setConfidenceLearningDialogState(state)

        assertNotNull(stateManager.confidenceLearningDialogState.value)
        assertEquals("play music", stateManager.confidenceLearningDialogState.value?.userInput)
        assertEquals(0.65f, stateManager.confidenceLearningDialogState.value?.confidence)
    }

    @Test
    fun `clearConfidenceLearningDialog sets state to null`() {
        val state = ConfidenceLearningState(
            userInput = "test",
            interpretedIntent = "test.intent",
            confidence = 0.5f,
            alternateIntents = emptyList()
        )
        stateManager.setConfidenceLearningDialogState(state)

        stateManager.clearConfidenceLearningDialog()

        assertNull(stateManager.confidenceLearningDialogState.value)
    }

    // ==================== Wake Word Tests ====================

    @Test
    fun `initial wake word state is null`() {
        assertNull(stateManager.wakeWordDetected.value)
    }

    @Test
    fun `setWakeWordDetected updates state`() {
        stateManager.setWakeWordDetected("hey ava")
        assertEquals("hey ava", stateManager.wakeWordDetected.value)
    }

    @Test
    fun `clearWakeWordDetected sets state to null`() {
        stateManager.setWakeWordDetected("hey ava")
        stateManager.clearWakeWordDetected()
        assertNull(stateManager.wakeWordDetected.value)
    }

    // ==================== Helper Methods ====================

    private fun createTestMessage(id: String, content: String): Message {
        return Message(
            id = id,
            conversationId = "test-conv",
            role = MessageRole.USER,
            content = content,
            timestamp = System.currentTimeMillis(),
            metadata = null
        )
    }

    private fun createTestConversation(id: String): Conversation {
        return Conversation(
            id = id,
            title = "Test Conversation",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messageCount = 0
        )
    }
}
