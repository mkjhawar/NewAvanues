package com.augmentalis.ava.core.domain.usecase

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.repository.ConversationRepository
import com.augmentalis.ava.core.domain.repository.MessageRepository

/**
 * Export format options
 */
enum class ExportFormat {
    JSON,
    CSV
}

/**
 * Privacy options for export
 */
data class PrivacyOptions(
    val excludeMetadata: Boolean = false,
    val excludeConfidenceScores: Boolean = false,
    val excludeIntents: Boolean = false,
    val anonymizeTimestamps: Boolean = false
)

/**
 * Export result containing file content
 */
data class ExportResult(
    val content: String,
    val filename: String,
    val mimeType: String
)

/**
 * Use case for exporting conversations to various formats
 * Platform-specific implementation required for JSON/CSV formatting
 */
expect class ExportConversationUseCase(
    conversationRepository: ConversationRepository,
    messageRepository: MessageRepository
) {
    /**
     * Export a single conversation
     */
    suspend fun exportConversation(
        conversationId: String,
        format: ExportFormat,
        privacyOptions: PrivacyOptions = PrivacyOptions()
    ): Result<ExportResult>

    /**
     * Export all conversations
     */
    suspend fun exportAllConversations(
        format: ExportFormat,
        privacyOptions: PrivacyOptions = PrivacyOptions()
    ): Result<ExportResult>
}
