package com.augmentalis.uuidcreator.models

/**
 * Result of a VUID command execution
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
     * Backward-compatible property for targetVUID
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use targetVUID instead", ReplaceWith("targetVUID"))
    val targetUUID: String? get() = targetVUID
}