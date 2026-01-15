/**
 * AvidCommandResult.kt - Result of an AVID command execution
 *
 * Updated: 2026-01-15 - Migrated to AVID naming
 */
package com.augmentalis.avidcreator.models

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

// Backward compatibility alias
@Deprecated("Use AvidCommandResult instead", ReplaceWith("AvidCommandResult"))
typealias VUIDCommandResult = AvidCommandResult
