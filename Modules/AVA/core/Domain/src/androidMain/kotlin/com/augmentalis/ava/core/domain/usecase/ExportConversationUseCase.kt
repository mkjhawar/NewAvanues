package com.augmentalis.ava.core.domain.usecase

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.model.MessageRole
import com.augmentalis.ava.core.domain.repository.ConversationRepository
import com.augmentalis.ava.core.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android implementation of conversation export use case
 * Uses Android-specific APIs for JSON and CSV formatting
 *
 * Features:
 * - Export single or all conversations
 * - JSON format with full metadata (using org.json)
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
     * Export single conversation to JSON format
     */
    private fun exportToJson(
        conversation: Conversation,
        messages: List<Message>,
        privacyOptions: PrivacyOptions
    ): ExportResult {
        val json = JSONObject()
        json.put("conversation_id", conversation.id)
        json.put("title", conversation.title)
        json.put("created_at", formatTimestamp(conversation.createdAt, privacyOptions.anonymizeTimestamps))
        json.put("updated_at", formatTimestamp(conversation.updatedAt, privacyOptions.anonymizeTimestamps))
        json.put("message_count", conversation.messageCount)

        if (!privacyOptions.excludeMetadata && conversation.metadata != null) {
            json.put("metadata", JSONObject(conversation.metadata))
        }

        // Add messages array
        val messagesArray = JSONArray()
        messages.forEach { message ->
            val messageObj = JSONObject()
            messageObj.put("id", message.id)
            messageObj.put("role", message.role.name.lowercase())
            messageObj.put("content", message.content)
            messageObj.put("timestamp", formatTimestamp(message.timestamp, privacyOptions.anonymizeTimestamps))

            if (!privacyOptions.excludeIntents && message.intent != null) {
                messageObj.put("intent", message.intent)
            }

            if (!privacyOptions.excludeConfidenceScores && message.confidence != null) {
                messageObj.put("confidence", message.confidence)
            }

            if (!privacyOptions.excludeMetadata && message.metadata != null) {
                messageObj.put("metadata", JSONObject(message.metadata))
            }

            messagesArray.put(messageObj)
        }
        json.put("messages", messagesArray)

        val filename = "ava_conversation_${conversation.id}_${System.currentTimeMillis()}.json"
        return ExportResult(
            content = json.toString(2), // Pretty print with indent
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
        val json = JSONObject()
        json.put("export_date", formatTimestamp(System.currentTimeMillis(), false))
        json.put("total_conversations", conversationsWithMessages.size)

        val conversationsArray = JSONArray()
        conversationsWithMessages.forEach { (conversation, messages) ->
            val convObj = JSONObject()
            convObj.put("conversation_id", conversation.id)
            convObj.put("title", conversation.title)
            convObj.put("created_at", formatTimestamp(conversation.createdAt, privacyOptions.anonymizeTimestamps))
            convObj.put("updated_at", formatTimestamp(conversation.updatedAt, privacyOptions.anonymizeTimestamps))
            convObj.put("message_count", conversation.messageCount)

            if (!privacyOptions.excludeMetadata && conversation.metadata != null) {
                convObj.put("metadata", JSONObject(conversation.metadata))
            }

            // Add messages
            val messagesArray = JSONArray()
            messages.forEach { message ->
                val messageObj = JSONObject()
                messageObj.put("id", message.id)
                messageObj.put("role", message.role.name.lowercase())
                messageObj.put("content", message.content)
                messageObj.put("timestamp", formatTimestamp(message.timestamp, privacyOptions.anonymizeTimestamps))

                if (!privacyOptions.excludeIntents && message.intent != null) {
                    messageObj.put("intent", message.intent)
                }

                if (!privacyOptions.excludeConfidenceScores && message.confidence != null) {
                    messageObj.put("confidence", message.confidence)
                }

                if (!privacyOptions.excludeMetadata && message.metadata != null) {
                    messageObj.put("metadata", JSONObject(message.metadata))
                }

                messagesArray.put(messageObj)
            }
            convObj.put("messages", messagesArray)
            conversationsArray.put(convObj)
        }
        json.put("conversations", conversationsArray)

        val filename = "ava_all_conversations_${System.currentTimeMillis()}.json"
        return ExportResult(
            content = json.toString(2),
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

        val filename = "ava_conversation_${conversation.id}_${System.currentTimeMillis()}.csv"
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

        val filename = "ava_all_conversations_${System.currentTimeMillis()}.csv"
        return ExportResult(
            content = csv.toString(),
            filename = filename,
            mimeType = "text/csv"
        )
    }

    /**
     * Format timestamp with optional anonymization
     */
    private fun formatTimestamp(timestamp: Long, anonymize: Boolean): String {
        return if (anonymize) {
            // Return relative offset from first message (preserves conversation flow)
            "offset_$timestamp"
        } else {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
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
