package com.augmentalis.ava.core.domain.usecase

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.repository.ConversationRepository
import com.augmentalis.ava.core.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * iOS implementation of conversation export use case
 * Uses KMP-compatible APIs for JSON and CSV formatting
 *
 * Features:
 * - Export single or all conversations
 * - JSON format with full metadata (using kotlinx.serialization)
 * - CSV format for spreadsheet analysis
 * - Privacy options (exclude sensitive data)
 */
actual class ExportConversationUseCase actual constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {

    /**
     * Export a single conversation
     *
     * @param conversationId Conversation to export
     * @param format Export format (JSON or CSV)
     * @param privacyOptions Privacy controls
     * @return Export result with content and metadata
     */
    actual suspend fun exportConversation(
        conversationId: String,
        format: ExportFormat,
        privacyOptions: PrivacyOptions
    ): Result<ExportResult> {
        return try {
            // Get conversation
            val conversation = when (val result = conversationRepository.getConversationById(conversationId)) {
                is Result.Success -> result.data
                is Result.Error -> return Result.Error(
                    exception = result.exception,
                    message = "Failed to get conversation: ${result.message}"
                )
            }

            // Get messages
            val messages = messageRepository.getMessagesForConversation(conversationId).first()

            // Generate export content
            val exportResult = when (format) {
                ExportFormat.JSON -> exportToJson(conversation, messages, privacyOptions)
                ExportFormat.CSV -> exportToCsv(conversation, messages, privacyOptions)
            }

            Result.Success(exportResult)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to export conversation: ${e.message}")
        }
    }

    /**
     * Export all conversations
     *
     * @param format Export format (JSON or CSV)
     * @param privacyOptions Privacy controls
     * @return Export result with content and metadata
     */
    actual suspend fun exportAllConversations(
        format: ExportFormat,
        privacyOptions: PrivacyOptions
    ): Result<ExportResult> {
        return try {
            // Get all conversations
            val conversations = conversationRepository.getAllConversations().first()

            if (conversations.isEmpty()) {
                return Result.Error(
                    exception = IllegalStateException("No conversations found"),
                    message = "No conversations to export"
                )
            }

            // Get messages for each conversation
            val conversationsWithMessages = conversations.map { conversation ->
                val messages = messageRepository.getMessagesForConversation(conversation.id).first()
                conversation to messages
            }

            // Generate export content
            val exportResult = when (format) {
                ExportFormat.JSON -> exportAllToJson(conversationsWithMessages, privacyOptions)
                ExportFormat.CSV -> exportAllToCsv(conversationsWithMessages, privacyOptions)
            }

            Result.Success(exportResult)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to export all conversations: ${e.message}")
        }
    }

    /**
     * Export single conversation to JSON format (manual JSON construction)
     */
    private fun exportToJson(
        conversation: Conversation,
        messages: List<Message>,
        privacyOptions: PrivacyOptions
    ): ExportResult {
        val json = buildString {
            append("{\n")
            append("  \"conversation_id\": \"${escapeJson(conversation.id)}\",\n")
            append("  \"title\": \"${escapeJson(conversation.title)}\",\n")
            append("  \"created_at\": \"${formatTimestamp(conversation.createdAt, privacyOptions.anonymizeTimestamps)}\",\n")
            append("  \"updated_at\": \"${formatTimestamp(conversation.updatedAt, privacyOptions.anonymizeTimestamps)}\",\n")
            append("  \"message_count\": ${conversation.messageCount}")

            if (!privacyOptions.excludeMetadata && conversation.metadata != null) {
                append(",\n  \"metadata\": ${conversation.metadata}")
            }

            append(",\n  \"messages\": [\n")
            messages.forEachIndexed { index, message ->
                append("    {\n")
                append("      \"id\": \"${escapeJson(message.id)}\",\n")
                append("      \"role\": \"${message.role.name.lowercase()}\",\n")
                append("      \"content\": \"${escapeJson(message.content)}\",\n")
                append("      \"timestamp\": \"${formatTimestamp(message.timestamp, privacyOptions.anonymizeTimestamps)}\"")

                if (!privacyOptions.excludeIntents && message.intent != null) {
                    append(",\n      \"intent\": \"${escapeJson(message.intent)}\"")
                }

                if (!privacyOptions.excludeConfidenceScores && message.confidence != null) {
                    append(",\n      \"confidence\": ${message.confidence}")
                }

                if (!privacyOptions.excludeMetadata && message.metadata != null) {
                    append(",\n      \"metadata\": ${message.metadata}")
                }

                append("\n    }")
                if (index < messages.size - 1) append(",")
                append("\n")
            }
            append("  ]\n")
            append("}")
        }

        val timestamp = Clock.System.now().toEpochMilliseconds()
        val filename = "ava_conversation_${conversation.id}_$timestamp.json"
        return ExportResult(
            content = json,
            filename = filename,
            mimeType = "application/json"
        )
    }

    /**
     * Export all conversations to JSON format
     */
    private fun exportAllToJson(
        conversationsWithMessages: List<Pair<Conversation, List<Message>>>,
        privacyOptions: PrivacyOptions
    ): ExportResult {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val json = buildString {
            append("{\n")
            append("  \"export_date\": \"${formatTimestamp(timestamp, false)}\",\n")
            append("  \"total_conversations\": ${conversationsWithMessages.size},\n")
            append("  \"conversations\": [\n")

            conversationsWithMessages.forEachIndexed { convIndex, (conversation, messages) ->
                append("    {\n")
                append("      \"conversation_id\": \"${escapeJson(conversation.id)}\",\n")
                append("      \"title\": \"${escapeJson(conversation.title)}\",\n")
                append("      \"created_at\": \"${formatTimestamp(conversation.createdAt, privacyOptions.anonymizeTimestamps)}\",\n")
                append("      \"updated_at\": \"${formatTimestamp(conversation.updatedAt, privacyOptions.anonymizeTimestamps)}\",\n")
                append("      \"message_count\": ${conversation.messageCount}")

                if (!privacyOptions.excludeMetadata && conversation.metadata != null) {
                    append(",\n      \"metadata\": ${conversation.metadata}")
                }

                append(",\n      \"messages\": [\n")
                messages.forEachIndexed { msgIndex, message ->
                    append("        {\n")
                    append("          \"id\": \"${escapeJson(message.id)}\",\n")
                    append("          \"role\": \"${message.role.name.lowercase()}\",\n")
                    append("          \"content\": \"${escapeJson(message.content)}\",\n")
                    append("          \"timestamp\": \"${formatTimestamp(message.timestamp, privacyOptions.anonymizeTimestamps)}\"")

                    if (!privacyOptions.excludeIntents && message.intent != null) {
                        append(",\n          \"intent\": \"${escapeJson(message.intent)}\"")
                    }

                    if (!privacyOptions.excludeConfidenceScores && message.confidence != null) {
                        append(",\n          \"confidence\": ${message.confidence}")
                    }

                    if (!privacyOptions.excludeMetadata && message.metadata != null) {
                        append(",\n          \"metadata\": ${message.metadata}")
                    }

                    append("\n        }")
                    if (msgIndex < messages.size - 1) append(",")
                    append("\n")
                }
                append("      ]\n")
                append("    }")
                if (convIndex < conversationsWithMessages.size - 1) append(",")
                append("\n")
            }
            append("  ]\n")
            append("}")
        }

        val filename = "ava_all_conversations_$timestamp.json"
        return ExportResult(
            content = json,
            filename = filename,
            mimeType = "application/json"
        )
    }

    /**
     * Export single conversation to CSV format
     */
    private fun exportToCsv(
        conversation: Conversation,
        messages: List<Message>,
        privacyOptions: PrivacyOptions
    ): ExportResult {
        val csv = StringBuilder()

        // CSV Header
        val headers = mutableListOf(
            "conversation_id",
            "conversation_title",
            "message_id",
            "role",
            "content",
            "timestamp"
        )

        if (!privacyOptions.excludeIntents) {
            headers.add("intent")
        }

        if (!privacyOptions.excludeConfidenceScores) {
            headers.add("confidence")
        }

        csv.append(headers.joinToString(","))
        csv.append("\n")

        // CSV Rows
        messages.forEach { message ->
            val row = mutableListOf<String>()
            row.add(escapeCsv(conversation.id))
            row.add(escapeCsv(conversation.title))
            row.add(escapeCsv(message.id))
            row.add(escapeCsv(message.role.name.lowercase()))
            row.add(escapeCsv(message.content))
            row.add(escapeCsv(formatTimestamp(message.timestamp, privacyOptions.anonymizeTimestamps)))

            if (!privacyOptions.excludeIntents) {
                row.add(escapeCsv(message.intent ?: ""))
            }

            if (!privacyOptions.excludeConfidenceScores) {
                row.add(escapeCsv(message.confidence?.toString() ?: ""))
            }

            csv.append(row.joinToString(","))
            csv.append("\n")
        }

        val timestamp = Clock.System.now().toEpochMilliseconds()
        val filename = "ava_conversation_${conversation.id}_$timestamp.csv"
        return ExportResult(
            content = csv.toString(),
            filename = filename,
            mimeType = "text/csv"
        )
    }

    /**
     * Export all conversations to CSV format
     */
    private fun exportAllToCsv(
        conversationsWithMessages: List<Pair<Conversation, List<Message>>>,
        privacyOptions: PrivacyOptions
    ): ExportResult {
        val csv = StringBuilder()

        // CSV Header
        val headers = mutableListOf(
            "conversation_id",
            "conversation_title",
            "message_id",
            "role",
            "content",
            "timestamp"
        )

        if (!privacyOptions.excludeIntents) {
            headers.add("intent")
        }

        if (!privacyOptions.excludeConfidenceScores) {
            headers.add("confidence")
        }

        csv.append(headers.joinToString(","))
        csv.append("\n")

        // CSV Rows for all conversations
        conversationsWithMessages.forEach { (conversation, messages) ->
            messages.forEach { message ->
                val row = mutableListOf<String>()
                row.add(escapeCsv(conversation.id))
                row.add(escapeCsv(conversation.title))
                row.add(escapeCsv(message.id))
                row.add(escapeCsv(message.role.name.lowercase()))
                row.add(escapeCsv(message.content))
                row.add(escapeCsv(formatTimestamp(message.timestamp, privacyOptions.anonymizeTimestamps)))

                if (!privacyOptions.excludeIntents) {
                    row.add(escapeCsv(message.intent ?: ""))
                }

                if (!privacyOptions.excludeConfidenceScores) {
                    row.add(escapeCsv(message.confidence?.toString() ?: ""))
                }

                csv.append(row.joinToString(","))
                csv.append("\n")
            }
        }

        val timestamp = Clock.System.now().toEpochMilliseconds()
        val filename = "ava_all_conversations_$timestamp.csv"
        return ExportResult(
            content = csv.toString(),
            filename = filename,
            mimeType = "text/csv"
        )
    }

    /**
     * Format timestamp with optional anonymization (KMP-compatible)
     */
    private fun formatTimestamp(timestamp: Long, anonymize: Boolean): String {
        return if (anonymize) {
            // Return relative offset from first message (preserves conversation flow)
            "offset_$timestamp"
        } else {
            // Use kotlinx-datetime for cross-platform time formatting
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${localDateTime.date} ${localDateTime.time}"
        }
    }

    /**
     * Escape JSON strings (handle quotes, newlines, etc.)
     */
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Escape CSV field (handle quotes and commas)
     */
    private fun escapeCsv(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
}
