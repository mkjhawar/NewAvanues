// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.dto

import kotlinx.datetime.Clock

/**
 * Data Transfer Object for ErrorReport.
 * Stores error information for debugging and analytics.
 */
data class ErrorReportDTO(
    val id: Long = 0,
    val errorType: String,
    val message: String,
    val stackTrace: String? = null,
    val context: String? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isSent: Boolean = false,
    val sentAt: Long? = null
)

/**
 * Extension to convert SQLDelight entity to DTO.
 */
fun com.augmentalis.database.Error_report.toErrorReportDTO(): ErrorReportDTO = ErrorReportDTO(
    id = id,
    errorType = errorType,
    message = errorMessage,
    stackTrace = stackTrace,
    context = context,
    timestamp = timestamp,
    isSent = isSent == 1L,
    sentAt = null // Schema doesn't have sentAt field
)
