// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.dto

import kotlinx.datetime.Clock

/**
 * Data Transfer Object for CommandHistory.
 * Records command execution history for analytics.
 */
data class CommandHistoryDTO(
    val id: Long = 0,
    val originalText: String,
    val processedCommand: String,
    val confidence: Double = 0.0,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val language: String = "en",
    val engineUsed: String,
    val success: Boolean = false,
    val executionTimeMs: Long = 0,
    val errorMessage: String? = null
)

/**
 * Extension to convert SQLDelight entity to DTO.
 */
fun com.augmentalis.database.Command_history_entry.toCommandHistoryDTO(): CommandHistoryDTO = CommandHistoryDTO(
    id = id,
    originalText = originalText,
    processedCommand = processedCommand ?: "",
    confidence = confidence,
    timestamp = timestamp,
    language = language,
    engineUsed = engineUsed,
    success = success == 1L,
    executionTimeMs = executionTimeMs,
    errorMessage = null // Schema doesn't have errorMessage field
)
