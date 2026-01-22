/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from ChatViewModel (SRP)
 */

package com.augmentalis.chat.coordinator

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.usecase.ExportConversationUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Export Coordinator - Single Responsibility: Conversation Export Operations
 *
 * Extracted from ChatViewModel as part of SOLID refactoring.
 * Handles all export-related operations:
 * - Single and bulk conversation export
 * - File creation and caching
 * - Share intent creation and launching
 * - Privacy options handling
 *
 * Thread-safe: Uses StateFlow for all mutable state.
 *
 * @param context Application context for file operations and share intents
 * @param exportConversationUseCase Use case for export logic
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-01-15
 */
@Singleton
class ExportCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportConversationUseCase: ExportConversationUseCase
) : IExportCoordinator {

    companion object {
        private const val TAG = "ExportCoordinator"
    }

    // ==================== State ====================

    private val _isExporting = MutableStateFlow(false)
    override val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    override val exportError: StateFlow<String?> = _exportError.asStateFlow()

    // ==================== Export Operations ====================

    /**
     * Export a single conversation to file.
     *
     * Flow:
     * 1. Set exporting state to true
     * 2. Call use case to generate export content
     * 3. Write content to cache file
     * 4. Return result with file information
     *
     * @param conversationId ID of conversation to export
     * @param format Export format (JSON or CSV)
     * @param privacyOptions Privacy controls for export
     * @return Result with export file path on success, or error
     */
    override suspend fun exportConversation(
        conversationId: String,
        format: ExportFormat,
        privacyOptions: PrivacyOptions
    ): Result<ExportResult> {
        return try {
            _isExporting.value = true
            _exportError.value = null

            Log.d(TAG, "Exporting conversation: $conversationId (format: $format)")

            // Convert to use case format
            val useCaseFormat = when (format) {
                ExportFormat.JSON -> com.augmentalis.ava.core.domain.usecase.ExportFormat.JSON
                ExportFormat.CSV -> com.augmentalis.ava.core.domain.usecase.ExportFormat.CSV
            }

            val useCasePrivacy = com.augmentalis.ava.core.domain.usecase.PrivacyOptions(
                anonymizeTimestamps = privacyOptions.redactTimestamps,
                excludeMetadata = privacyOptions.redactMetadata
            )

            when (val result = exportConversationUseCase.exportConversation(
                conversationId = conversationId,
                format = useCaseFormat,
                privacyOptions = useCasePrivacy
            )) {
                is Result.Success -> {
                    val useCaseResult = result.data
                    Log.d(TAG, "Export successful: ${useCaseResult.filename}")

                    // Write to cache file
                    val file = File(context.cacheDir, useCaseResult.filename)
                    file.writeText(useCaseResult.content)

                    val exportResult = ExportResult(
                        filename = useCaseResult.filename,
                        content = useCaseResult.content,
                        mimeType = useCaseResult.mimeType,
                        filePath = file.absolutePath
                    )

                    Log.i(TAG, "Successfully exported conversation to: ${file.absolutePath}")
                    Result.Success(exportResult)
                }
                is Result.Error -> {
                    val errorMsg = "Export failed: ${result.message}"
                    _exportError.value = errorMsg
                    Log.e(TAG, errorMsg, result.exception)
                    Result.Error(result.exception, errorMsg)
                }
            }

        } catch (e: Exception) {
            val errorMsg = "Export failed: ${e.message}"
            _exportError.value = errorMsg
            Log.e(TAG, "Exception in exportConversation", e)
            Result.Error(e, errorMsg)
        } finally {
            _isExporting.value = false
        }
    }

    /**
     * Export all conversations to file.
     *
     * Flow:
     * 1. Set exporting state to true
     * 2. Call use case to generate export content for all conversations
     * 3. Write content to cache file
     * 4. Return result with file information
     *
     * @param format Export format (JSON or CSV)
     * @param privacyOptions Privacy controls for export
     * @return Result with export file path on success, or error
     */
    override suspend fun exportAllConversations(
        format: ExportFormat,
        privacyOptions: PrivacyOptions
    ): Result<ExportResult> {
        return try {
            _isExporting.value = true
            _exportError.value = null

            Log.d(TAG, "Exporting all conversations (format: $format)")

            // Convert to use case format
            val useCaseFormat = when (format) {
                ExportFormat.JSON -> com.augmentalis.ava.core.domain.usecase.ExportFormat.JSON
                ExportFormat.CSV -> com.augmentalis.ava.core.domain.usecase.ExportFormat.CSV
            }

            val useCasePrivacy = com.augmentalis.ava.core.domain.usecase.PrivacyOptions(
                anonymizeTimestamps = privacyOptions.redactTimestamps,
                excludeMetadata = privacyOptions.redactMetadata
            )

            when (val result = exportConversationUseCase.exportAllConversations(
                format = useCaseFormat,
                privacyOptions = useCasePrivacy
            )) {
                is Result.Success -> {
                    val useCaseResult = result.data
                    Log.d(TAG, "Export successful: ${useCaseResult.filename}")

                    // Write to cache file
                    val file = File(context.cacheDir, useCaseResult.filename)
                    file.writeText(useCaseResult.content)

                    val exportResult = ExportResult(
                        filename = useCaseResult.filename,
                        content = useCaseResult.content,
                        mimeType = useCaseResult.mimeType,
                        filePath = file.absolutePath
                    )

                    Log.i(TAG, "Successfully exported all conversations to: ${file.absolutePath}")
                    Result.Success(exportResult)
                }
                is Result.Error -> {
                    val errorMsg = "Export failed: ${result.message}"
                    _exportError.value = errorMsg
                    Log.e(TAG, errorMsg, result.exception)
                    Result.Error(result.exception, errorMsg)
                }
            }

        } catch (e: Exception) {
            val errorMsg = "Export failed: ${e.message}"
            _exportError.value = errorMsg
            Log.e(TAG, "Exception in exportAllConversations", e)
            Result.Error(e, errorMsg)
        } finally {
            _isExporting.value = false
        }
    }

    /**
     * Share the exported file via system share dialog.
     *
     * Uses FileProvider for secure file sharing (privacy-first).
     *
     * @param result Export result containing file info
     * @return Result indicating share success or failure
     */
    override fun shareExport(result: ExportResult): Result<Unit> {
        return try {
            val filePath = result.filePath
                ?: return Result.Error(IllegalStateException("No file path in export result"), "No file path")

            val file = File(filePath)
            if (!file.exists()) {
                return Result.Error(IllegalStateException("Export file not found"), "Export file not found")
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = result.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AVA Conversation Export")
                putExtra(Intent.EXTRA_TEXT, "Exported conversation data from AVA AI")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share conversation export")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Log.d(TAG, "Share intent launched successfully")
            Result.Success(Unit)

        } catch (e: Exception) {
            val errorMsg = "Failed to share file: ${e.message}"
            _exportError.value = errorMsg
            Log.e(TAG, "Failed to share file", e)
            Result.Error(e, errorMsg)
        }
    }

    /**
     * Clear export error message.
     */
    override fun clearError() {
        _exportError.value = null
    }
}
