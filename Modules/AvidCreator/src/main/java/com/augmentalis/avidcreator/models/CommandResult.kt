/**
 * CommandResult.kt - Command result from legacy UIKitVoiceCommandSystem
 *
 * EXACT port of CommandResult data class from working legacy implementation
 */

package com.augmentalis.avidcreator.models

/**
 * Command result - EXACT copy from legacy implementation
 *
 * @param success Whether command executed successfully
 * @param targetId Target identifier that was processed
 * @param message Result message
 * @param data Optional result data
 */
data class CommandResult(
    val success: Boolean,
    val targetId: String? = null,
    val message: String? = null,
    val data: Any? = null
)
