/**
 * AvidCommandResultModel.kt - Result of an AVID command execution
 *
 * Cross-platform KMP model for command results.
 *
 * Updated: 2026-01-18 - Migrated to KMP
 */
package com.augmentalis.avidcreator

/**
 * Result of an AVID command execution
 */
data class AvidCommandResult(
    val success: Boolean,
    val targetAvid: String? = null,
    val action: String? = null,
    val message: String? = null,
    val data: Any? = null,
    val executionTime: Long = 0L,
    val error: String? = null
)
