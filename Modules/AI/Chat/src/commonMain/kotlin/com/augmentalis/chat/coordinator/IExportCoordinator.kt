/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Export coordinator interface for cross-platform
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Export Coordinator Interface - Cross-platform export coordination
 *
 * Abstracts conversation export operations for cross-platform use in KMP.
 * Provides:
 * - Single and bulk conversation export
 * - Format selection (JSON, CSV)
 * - Privacy options handling
 * - Share intent coordination
 *
 * SOLID Principle: Single Responsibility
 * - Extracted from ChatViewModel for export-specific concerns
 * - Handles all export formatting, file creation, and sharing
 *
 * @see ExportCoordinator for Android implementation
 *
 * @author Manoj Jhawar
 * @since 2025-01-15
 */
interface IExportCoordinator {

    // ==================== State ====================

    /**
     * Export in progress state.
     * True when an export operation is running.
     */
    val isExporting: StateFlow<Boolean>

    /**
     * Last export error message.
     * Null when no error or after clearError() is called.
     */
    val exportError: StateFlow<String?>

    // ==================== Export Operations ====================

    /**
     * Export a single conversation to file and share.
     *
     * @param conversationId ID of conversation to export
     * @param format Export format (JSON or CSV)
     * @param privacyOptions Privacy controls for export (redact timestamps, etc.)
     * @return Result with export file path on success, or error
     */
    suspend fun exportConversation(
        conversationId: String,
        format: ExportFormat = ExportFormat.JSON,
        privacyOptions: PrivacyOptions = PrivacyOptions()
    ): Result<ExportResult>

    /**
     * Export all conversations to file and share.
     *
     * @param format Export format (JSON or CSV)
     * @param privacyOptions Privacy controls for export
     * @return Result with export file path on success, or error
     */
    suspend fun exportAllConversations(
        format: ExportFormat = ExportFormat.JSON,
        privacyOptions: PrivacyOptions = PrivacyOptions()
    ): Result<ExportResult>

    /**
     * Share the exported file via system share dialog.
     *
     * @param result Export result containing file info
     * @return Result indicating share success or failure
     */
    fun shareExport(result: ExportResult): Result<Unit>

    /**
     * Clear export error message.
     */
    fun clearError()
}

/**
 * Export format enum for cross-platform use.
 */
enum class ExportFormat {
    JSON,
    CSV
}

/**
 * Privacy options for export.
 *
 * @property redactTimestamps Whether to remove timestamps from export
 * @property redactMetadata Whether to remove metadata (intent, confidence)
 * @property anonymizeUserContent Whether to replace user content with placeholders
 */
data class PrivacyOptions(
    val redactTimestamps: Boolean = false,
    val redactMetadata: Boolean = false,
    val anonymizeUserContent: Boolean = false
)

/**
 * Export result containing file information.
 *
 * @property filename Name of exported file
 * @property content File content as string
 * @property mimeType MIME type for sharing
 * @property filePath Full path to exported file (platform-specific)
 */
data class ExportResult(
    val filename: String,
    val content: String,
    val mimeType: String,
    val filePath: String? = null
)
