package com.augmentalis.uuidcreator.models

/**
 * Result of a VUID command execution
 *
 * Migration: UUID → VUID (VoiceUniqueID)
 * Created: 2025-12-23
 */
data class VUIDCommandResult(
    val success: Boolean,
    val targetVUID: String? = null,
    val action: String? = null,
    val message: String? = null,
    val data: Any? = null,
    val executionTime: Long = 0L,
    val error: String? = null
) {
    /**
     * Convert to deprecated UUIDCommandResult for backwards compatibility
     */
    fun toUUIDCommandResult(): UUIDCommandResult = UUIDCommandResult(
        success = success,
        targetUUID = targetVUID,
        action = action,
        message = message,
        data = data,
        executionTime = executionTime,
        error = error
    )

    companion object {
        /**
         * Convert from deprecated UUIDCommandResult
         */
        fun fromUUIDCommandResult(result: UUIDCommandResult): VUIDCommandResult = VUIDCommandResult(
            success = result.success,
            targetVUID = result.targetUUID,
            action = result.action,
            message = result.message,
            data = result.data,
            executionTime = result.executionTime,
            error = result.error
        )
    }
}
