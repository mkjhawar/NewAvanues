/**
 * CommandResultModel.kt - Command result
 *
 * Cross-platform KMP model for command results.
 *
 * @param success Whether command executed successfully
 * @param targetId Target identifier that was processed
 * @param message Result message
 * @param data Optional result data
 */
package com.augmentalis.avidcreator

/**
 * Command result
 */
data class CommandResult(
    val success: Boolean,
    val targetId: String? = null,
    val message: String? = null,
    val data: Any? = null
)
