// filename: Modules/AVA/core/Domain/src/commonMain/kotlin/com/augmentalis/ava/core/domain/usecase/SendMessageUseCase.kt
// created: 2025-12-18
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.core.domain.usecase

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.model.MessageRole
import com.augmentalis.ava.core.domain.repository.ConversationRepository
import com.augmentalis.ava.core.domain.repository.MessageRepository

/**
 * Use case for sending a message in a conversation.
 *
 * Responsibilities:
 * - Validate conversation exists
 * - Create message entity with proper metadata
 * - Persist message to repository
 * - Handle errors gracefully
 *
 * Single Responsibility: Orchestrates message sending logic
 */
class SendMessageUseCase(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {

    /**
     * Send a message to a conversation
     *
     * @param conversationId ID of the conversation
     * @param content Message content
     * @param role Message role (USER, ASSISTANT, SYSTEM)
     * @param intent Detected intent (optional)
     * @param confidence Intent confidence score (optional)
     * @param metadata Additional metadata (optional)
     * @return Result containing the created message
     */
    suspend operator fun invoke(
        conversationId: String,
        content: String,
        role: MessageRole,
        intent: String? = null,
        confidence: Float? = null,
        metadata: Map<String, String>? = null
    ): Result<Message> {
        try {
            // Validate conversation exists
            val conversationResult = conversationRepository.getConversationById(conversationId)
            if (conversationResult is Result.Error) {
                return Result.Error(
                    exception = conversationResult.exception,
                    message = "Conversation not found: $conversationId"
                )
            }

            // Create message entity
            val message = Message(
                id = generateMessageId(),
                conversationId = conversationId,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis(),
                intent = intent,
                confidence = confidence,
                metadata = metadata
            )

            // Persist message
            return messageRepository.addMessage(message)

        } catch (e: Exception) {
            return Result.Error(
                exception = e,
                message = "Failed to send message: ${e.message}"
            )
        }
    }

    /**
     * Generate a unique message ID
     * Platform-specific implementation can override this
     */
    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(0..999999).random()}"
    }
}
