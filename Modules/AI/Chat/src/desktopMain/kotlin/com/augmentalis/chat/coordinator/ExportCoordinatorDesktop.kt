/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Desktop implementation of Export Coordinator.
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToString
import kotlinx.serialization.json.put
import java.awt.Desktop
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Desktop (JVM) implementation of IExportCoordinator.
 *
 * Handles conversation export on desktop platforms:
 * - JSON export with full metadata
 * - CSV export for spreadsheet analysis
 * - File system storage
 * - System share/open functionality
 *
 * @author Manoj Jhawar
 * @since 2025-01-16
 */
class ExportCoordinatorDesktop(
    private val conversationManager: ConversationManagerDesktop
) : IExportCoordinator {

    // ==================== State ====================

    private val _isExporting = MutableStateFlow(false)
    override val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    override val exportError: StateFlow<String?> = _exportError.asStateFlow()

    // ==================== Configuration ====================

    // Default export directory (user's documents folder)
    private val exportDirectory: File by lazy {
        val documentsPath = System.getProperty("user.home") + File.separator + "Documents"
        val avaExportDir = File(documentsPath, "AVA Exports")
        if (!avaExportDir.exists()) {
            avaExportDir.mkdirs()
        }
        avaExportDir
    }

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    // ==================== Export Operations ====================

    override suspend fun exportConversation(
        conversationId: String,
        format: ExportFormat,
        privacyOptions: PrivacyOptions
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            _isExporting.value = true
            _exportError.value = null

            // Get messages from conversation manager
            val messages = conversationManager.getAllMessages(conversationId)

            if (messages.isEmpty()) {
                return@withContext Result.Error(
                    exception = IllegalStateException("No messages to export"),
                    message = "Conversation has no messages to export"
                )
            }

            // Get conversation metadata
            val conversations = conversationManager.conversations.value
            val conversation = conversations.find { it.id == conversationId }

            // Generate export content
            val (content, mimeType) = when (format) {
                ExportFormat.JSON -> generateJsonExport(
                    conversationId = conversationId,
                    conversationTitle = conversation?.title ?: "Untitled",
                    messages = messages.map { msg ->
                        ExportMessage(
                            id = msg.id,
                            role = msg.role.name.lowercase(),
                            content = if (privacyOptions.anonymizeUserContent && msg.role.name == "USER") {
                                "[REDACTED]"
                            } else {
                                msg.content
                            },
                            timestamp = if (privacyOptions.redactTimestamps) null else msg.timestamp,
                            metadata = if (privacyOptions.redactMetadata) null else msg.metadata
                        )
                    },
                    privacyOptions = privacyOptions
                )
                ExportFormat.CSV -> generateCsvExport(
                    messages = messages.map { msg ->
                        ExportMessage(
                            id = msg.id,
                            role = msg.role.name.lowercase(),
                            content = if (privacyOptions.anonymizeUserContent && msg.role.name == "USER") {
                                "[REDACTED]"
                            } else {
                                msg.content
                            },
                            timestamp = if (privacyOptions.redactTimestamps) null else msg.timestamp,
                            metadata = if (privacyOptions.redactMetadata) null else msg.metadata
                        )
                    }
                )
            }

            // Generate filename
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val extension = when (format) {
                ExportFormat.JSON -> "json"
                ExportFormat.CSV -> "csv"
            }
            val filename = "ava_conversation_${conversationId.take(8)}_$timestamp.$extension"

            // Write to file
            val exportFile = File(exportDirectory, filename)
            exportFile.writeText(content)

            val result = ExportResult(
                filename = filename,
                content = content,
                mimeType = mimeType,
                filePath = exportFile.absolutePath
            )

            println("[ExportCoordinatorDesktop] Exported conversation to: ${exportFile.absolutePath}")
            _isExporting.value = false

            Result.Success(result)
        } catch (e: Exception) {
            _isExporting.value = false
            _exportError.value = "Export failed: ${e.message}"
            Result.Error(
                exception = e,
                message = "Failed to export conversation: ${e.message}"
            )
        }
    }

    override suspend fun exportAllConversations(
        format: ExportFormat,
        privacyOptions: PrivacyOptions
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            _isExporting.value = true
            _exportError.value = null

            val conversations = conversationManager.conversations.value

            if (conversations.isEmpty()) {
                return@withContext Result.Error(
                    exception = IllegalStateException("No conversations to export"),
                    message = "No conversations available to export"
                )
            }

            // Collect all conversation data
            val allExportData = conversations.map { conversation ->
                val messages = conversationManager.getAllMessages(conversation.id)
                ConversationExportData(
                    id = conversation.id,
                    title = conversation.title,
                    createdAt = conversation.createdAt,
                    updatedAt = conversation.updatedAt,
                    messages = messages.map { msg ->
                        ExportMessage(
                            id = msg.id,
                            role = msg.role.name.lowercase(),
                            content = if (privacyOptions.anonymizeUserContent && msg.role.name == "USER") {
                                "[REDACTED]"
                            } else {
                                msg.content
                            },
                            timestamp = if (privacyOptions.redactTimestamps) null else msg.timestamp,
                            metadata = if (privacyOptions.redactMetadata) null else msg.metadata
                        )
                    }
                )
            }

            // Generate export content
            val (content, mimeType) = when (format) {
                ExportFormat.JSON -> generateBulkJsonExport(allExportData, privacyOptions)
                ExportFormat.CSV -> generateBulkCsvExport(allExportData)
            }

            // Generate filename
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val extension = when (format) {
                ExportFormat.JSON -> "json"
                ExportFormat.CSV -> "csv"
            }
            val filename = "ava_all_conversations_$timestamp.$extension"

            // Write to file
            val exportFile = File(exportDirectory, filename)
            exportFile.writeText(content)

            val result = ExportResult(
                filename = filename,
                content = content,
                mimeType = mimeType,
                filePath = exportFile.absolutePath
            )

            println("[ExportCoordinatorDesktop] Exported all conversations to: ${exportFile.absolutePath}")
            _isExporting.value = false

            Result.Success(result)
        } catch (e: Exception) {
            _isExporting.value = false
            _exportError.value = "Export failed: ${e.message}"
            Result.Error(
                exception = e,
                message = "Failed to export conversations: ${e.message}"
            )
        }
    }

    override fun shareExport(result: ExportResult): Result<Unit> {
        return try {
            val filePath = result.filePath
            if (filePath != null && Desktop.isDesktopSupported()) {
                val file = File(filePath)
                if (file.exists()) {
                    // Open the file with the default application
                    Desktop.getDesktop().open(file)
                    println("[ExportCoordinatorDesktop] Opened export file: $filePath")
                    Result.Success(Unit)
                } else {
                    Result.Error(
                        exception = IllegalStateException("Export file not found"),
                        message = "Export file no longer exists: $filePath"
                    )
                }
            } else {
                // If Desktop is not supported, just report the file location
                println("[ExportCoordinatorDesktop] Export saved to: $filePath")
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to open export: ${e.message}"
            )
        }
    }

    override fun clearError() {
        _exportError.value = null
    }

    // ==================== Export Helpers ====================

    private fun generateJsonExport(
        conversationId: String,
        conversationTitle: String,
        messages: List<ExportMessage>,
        privacyOptions: PrivacyOptions
    ): Pair<String, String> {
        // Build a fully-typed JsonObject so kotlinx.serialization can encode it
        // without needing a serializer for Map<String, Any>.
        val exportData: JsonObject = buildJsonObject {
            put("exportVersion", "1.0")
            put("exportedAt", Clock.System.now().toString())
            put("conversation", buildJsonObject {
                put("id", conversationId)
                put("title", conversationTitle)
                put("messageCount", messages.size)
            })
            put("messages", buildJsonArray {
                messages.forEach { msg ->
                    add(buildJsonObject {
                        put("id", msg.id)
                        put("role", msg.role)
                        put("content", msg.content)
                        if (msg.timestamp != null) put("timestamp", msg.timestamp)
                        if (msg.metadata != null) {
                            put("metadata", buildJsonObject {
                                msg.metadata.forEach { (k, v) -> put(k, v) }
                            })
                        }
                    })
                }
            })
        }

        val content = json.encodeToString(JsonObject.serializer(), exportData)
        return content to "application/json"
    }

    private fun generateCsvExport(messages: List<ExportMessage>): Pair<String, String> {
        val header = "id,role,content,timestamp"
        val rows = messages.map { msg ->
            val escapedContent = msg.content.replace("\"", "\"\"")
            "${msg.id},${msg.role},\"$escapedContent\",${msg.timestamp ?: ""}"
        }

        val content = listOf(header) + rows
        return content.joinToString("\n") to "text/csv"
    }

    private fun generateBulkJsonExport(
        conversations: List<ConversationExportData>,
        privacyOptions: PrivacyOptions
    ): Pair<String, String> {
        // Build a fully-typed JsonObject to avoid SerializationException on Map<String, Any>.
        val exportData: JsonObject = buildJsonObject {
            put("exportVersion", "1.0")
            put("exportedAt", Clock.System.now().toString())
            put("totalConversations", conversations.size)
            put("conversations", buildJsonArray {
                conversations.forEach { conv ->
                    add(buildJsonObject {
                        put("id", conv.id)
                        put("title", conv.title)
                        put("createdAt", conv.createdAt)
                        put("updatedAt", conv.updatedAt)
                        put("messageCount", conv.messages.size)
                        put("messages", buildJsonArray {
                            conv.messages.forEach { msg ->
                                add(buildJsonObject {
                                    put("id", msg.id)
                                    put("role", msg.role)
                                    put("content", msg.content)
                                    if (msg.timestamp != null) put("timestamp", msg.timestamp)
                                    if (msg.metadata != null) {
                                        put("metadata", buildJsonObject {
                                            msg.metadata.forEach { (k, v) -> put(k, v) }
                                        })
                                    }
                                })
                            }
                        })
                    })
                }
            })
        }

        val content = json.encodeToString(JsonObject.serializer(), exportData)
        return content to "application/json"
    }

    private fun generateBulkCsvExport(conversations: List<ConversationExportData>): Pair<String, String> {
        val header = "conversation_id,conversation_title,message_id,role,content,timestamp"
        val rows = conversations.flatMap { conv ->
            conv.messages.map { msg ->
                val escapedContent = msg.content.replace("\"", "\"\"")
                val escapedTitle = conv.title.replace("\"", "\"\"")
                "${conv.id},\"$escapedTitle\",${msg.id},${msg.role},\"$escapedContent\",${msg.timestamp ?: ""}"
            }
        }

        val content = listOf(header) + rows
        return content.joinToString("\n") to "text/csv"
    }

    // ==================== Data Classes ====================

    private data class ExportMessage(
        val id: String,
        val role: String,
        val content: String,
        val timestamp: Long?,
        val metadata: Map<String, String>?
    )

    private data class ConversationExportData(
        val id: String,
        val title: String,
        val createdAt: Long,
        val updatedAt: Long,
        val messages: List<ExportMessage>
    )

    companion object {
        @Volatile
        private var INSTANCE: ExportCoordinatorDesktop? = null

        /**
         * Get singleton instance of ExportCoordinatorDesktop.
         *
         * @param conversationManager Conversation manager for data access
         * @return Singleton instance
         */
        fun getInstance(
            conversationManager: ConversationManagerDesktop = ConversationManagerDesktop.getInstance()
        ): ExportCoordinatorDesktop {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExportCoordinatorDesktop(conversationManager).also {
                    INSTANCE = it
                }
            }
        }
    }
}
