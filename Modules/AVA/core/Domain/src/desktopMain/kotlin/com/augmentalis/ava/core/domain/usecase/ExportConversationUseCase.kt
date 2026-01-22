package com.augmentalis.ava.core.domain.usecase

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.repository.ConversationRepository
import com.augmentalis.ava.core.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Desktop (JVM) implementation of conversation export use case
 * Uses pure Kotlin string building for JSON (no external JSON library dependency)
 */
actual class ExportConversationUseCase actual constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {

    actual suspend fun exportConversation(
        conversationId: String,
        format: ExportFormat,
        privacyOptions: PrivacyOptions
    ): Result<ExportResult> {
        return try {
            val conversation = when (val result = conversationRepository.getConversationById(conversationId)) {
                is Result.Success -> result.data
                is Result.Error -> return Result.Error(
                    exception = result.exception,
                    message = "Failed to get conversation: ${result.message}"
                )
            }

            val messages = messageRepository.getMessagesForConversation(conversationId).first()

            val exportResult = when (format) {
                ExportFormat.JSON -> exportToJson(conversation, messages, privacyOptions)
                ExportFormat.CSV -> exportToCsv(conversation, messages, privacyOptions)
            }

            Result.Success(exportResult)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to export conversation: ${e.message}")
        }
    }

    actual suspend fun exportAllConversations(
        format: ExportFormat,
        privacyOptions: PrivacyOptions
    ): Result<ExportResult> {
        return try {
            val conversations = conversationRepository.getAllConversations().first()

            if (conversations.isEmpty()) {
                return Result.Error(
                    exception = IllegalStateException("No conversations found"),
                    message = "No conversations to export"
                )
            }

            val conversationsWithMessages = conversations.map { conversation ->
                val messages = messageRepository.getMessagesForConversation(conversation.id).first()
                conversation to messages
            }

            val exportResult = when (format) {
                ExportFormat.JSON -> exportAllToJson(conversationsWithMessages, privacyOptions)
                ExportFormat.CSV -> exportAllToCsv(conversationsWithMessages, privacyOptions)
            }

            Result.Success(exportResult)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to export all conversations: ${e.message}")
        }
    }

    private fun exportToJson(
        conversation: Conversation,
        messages: List<Message>,
        privacyOptions: PrivacyOptions
    ): ExportResult {
        val json = buildString {
            append("{\n")
            append("  \"conversation_id\": ${escapeJson(conversation.id)},\n")
            append("  \"title\": ${escapeJson(conversation.title)},\n")
            append("  \"created_at\": ${escapeJson(formatTimestamp(conversation.createdAt, privacyOptions.anonymizeTimestamps))},\n")
            append("  \"updated_at\": ${escapeJson(formatTimestamp(conversation.updatedAt, privacyOptions.anonymizeTimestamps))},\n")
            append("  \"message_count\": ${conversation.messageCount},\n")

            if (!privacyOptions.excludeMetadata && conversation.metadata != null) {
                append("  \"metadata\": ${mapToJson(conversation.metadata)},\n")
            }

            append("  \"messages\": [\n")
            messages.forEachIndexed { index, message ->
                append("    {\n")
                append("      \"id\": ${escapeJson(message.id)},\n")
                append("      \"role\": ${escapeJson(message.role.name.lowercase())},\n")
                append("      \"content\": ${escapeJson(message.content)},\n")
                append("      \"timestamp\": ${escapeJson(formatTimestamp(message.timestamp, privacyOptions.anonymizeTimestamps))}")

                if (!privacyOptions.excludeIntents && message.intent != null) {
                    append(",\n      \"intent\": ${escapeJson(message.intent)}")
                }
                if (!privacyOptions.excludeConfidenceScores && message.confidence != null) {
                    append(",\n      \"confidence\": ${message.confidence}")
                }
                if (!privacyOptions.excludeMetadata && message.metadata != null) {
                    append(",\n      \"metadata\": ${mapToJson(message.metadata)}")
                }

                append("\n    }")
                if (index < messages.size - 1) append(",")
                append("\n")
            }
            append("  ]\n")
            append("}")
        }

        val filename = "ava_conversation_${conversation.id}_${System.currentTimeMillis()}.json"
        return ExportResult(
            content = json,
            filename = filename,
            mimeType = "application/json"
        )
    }

    private fun exportAllToJson(
        conversationsWithMessages: List<Pair<Conversation, List<Message>>>,
        privacyOptions: PrivacyOptions
    ): ExportResult {
        val json = buildString {
            append("{\n")
            append("  \"export_date\": ${escapeJson(formatTimestamp(System.currentTimeMillis(), false))},\n")
            append("  \"total_conversations\": ${conversationsWithMessages.size},\n")
            append("  \"conversations\": [\n")

            conversationsWithMessages.forEachIndexed { convIndex, (conversation, messages) ->
                append("    {\n")
                append("      \"conversation_id\": ${escapeJson(conversation.id)},\n")
                append("      \"title\": ${escapeJson(conversation.title)},\n")
                append("      \"created_at\": ${escapeJson(formatTimestamp(conversation.createdAt, privacyOptions.anonymizeTimestamps))},\n")
                append("      \"updated_at\": ${escapeJson(formatTimestamp(conversation.updatedAt, privacyOptions.anonymizeTimestamps))},\n")
                append("      \"message_count\": ${conversation.messageCount},\n")

                if (!privacyOptions.excludeMetadata && conversation.metadata != null) {
                    append("      \"metadata\": ${mapToJson(conversation.metadata)},\n")
                }

                append("      \"messages\": [\n")
                messages.forEachIndexed { index, message ->
                    append("        {\n")
                    append("          \"id\": ${escapeJson(message.id)},\n")
                    append("          \"role\": ${escapeJson(message.role.name.lowercase())},\n")
                    append("          \"content\": ${escapeJson(message.content)},\n")
                    append("          \"timestamp\": ${escapeJson(formatTimestamp(message.timestamp, privacyOptions.anonymizeTimestamps))}")

                    if (!privacyOptions.excludeIntents && message.intent != null) {
                        append(",\n          \"intent\": ${escapeJson(message.intent)}")
                    }
                    if (!privacyOptions.excludeConfidenceScores && message.confidence != null) {
                        append(",\n          \"confidence\": ${message.confidence}")
                    }
                    if (!privacyOptions.excludeMetadata && message.metadata != null) {
                        append(",\n          \"metadata\": ${mapToJson(message.metadata)}")
                    }

                    append("\n        }")
                    if (index < messages.size - 1) append(",")
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

        val filename = "ava_all_conversations_${System.currentTimeMillis()}.json"
        return ExportResult(
            content = json,
            filename = filename,
            mimeType = "application/json"
        )
    }

    private fun exportToCsv(
        conversation: Conversation,
        messages: List<Message>,
        privacyOptions: PrivacyOptions
    ): ExportResult {
        val csv = StringBuilder()

        val headers = mutableListOf(
            "conversation_id",
            "conversation_title",
            "message_id",
            "role",
            "content",
            "timestamp"
        )

        if (!privacyOptions.excludeIntents) headers.add("intent")
        if (!privacyOptions.excludeConfidenceScores) headers.add("confidence")

        csv.append(headers.joinToString(","))
        csv.append("\n")

        messages.forEach { message ->
            val row = mutableListOf<String>()
            row.add(escapeCsv(conversation.id))
            row.add(escapeCsv(conversation.title))
            row.add(escapeCsv(message.id))
            row.add(escapeCsv(message.role.name.lowercase()))
            row.add(escapeCsv(message.content))
            row.add(escapeCsv(formatTimestamp(message.timestamp, privacyOptions.anonymizeTimestamps)))

            if (!privacyOptions.excludeIntents) row.add(escapeCsv(message.intent ?: ""))
            if (!privacyOptions.excludeConfidenceScores) row.add(escapeCsv(message.confidence?.toString() ?: ""))

            csv.append(row.joinToString(","))
            csv.append("\n")
        }

        val filename = "ava_conversation_${conversation.id}_${System.currentTimeMillis()}.csv"
        return ExportResult(
            content = csv.toString(),
            filename = filename,
            mimeType = "text/csv"
        )
    }

    private fun exportAllToCsv(
        conversationsWithMessages: List<Pair<Conversation, List<Message>>>,
        privacyOptions: PrivacyOptions
    ): ExportResult {
        val csv = StringBuilder()

        val headers = mutableListOf(
            "conversation_id",
            "conversation_title",
            "message_id",
            "role",
            "content",
            "timestamp"
        )

        if (!privacyOptions.excludeIntents) headers.add("intent")
        if (!privacyOptions.excludeConfidenceScores) headers.add("confidence")

        csv.append(headers.joinToString(","))
        csv.append("\n")

        conversationsWithMessages.forEach { (conversation, messages) ->
            messages.forEach { message ->
                val row = mutableListOf<String>()
                row.add(escapeCsv(conversation.id))
                row.add(escapeCsv(conversation.title))
                row.add(escapeCsv(message.id))
                row.add(escapeCsv(message.role.name.lowercase()))
                row.add(escapeCsv(message.content))
                row.add(escapeCsv(formatTimestamp(message.timestamp, privacyOptions.anonymizeTimestamps)))

                if (!privacyOptions.excludeIntents) row.add(escapeCsv(message.intent ?: ""))
                if (!privacyOptions.excludeConfidenceScores) row.add(escapeCsv(message.confidence?.toString() ?: ""))

                csv.append(row.joinToString(","))
                csv.append("\n")
            }
        }

        val filename = "ava_all_conversations_${System.currentTimeMillis()}.csv"
        return ExportResult(
            content = csv.toString(),
            filename = filename,
            mimeType = "text/csv"
        )
    }

    private fun formatTimestamp(timestamp: Long, anonymize: Boolean): String {
        return if (anonymize) {
            "offset_$timestamp"
        } else {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }

    private fun escapeCsv(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    private fun escapeJson(value: String?): String {
        if (value == null) return "null"
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun mapToJson(map: Map<String, String>?): String {
        if (map == null) return "null"
        val entries = map.entries.joinToString(", ") { (k, v) ->
            "${escapeJson(k)}: ${escapeJson(v)}"
        }
        return "{ $entries }"
    }
}
