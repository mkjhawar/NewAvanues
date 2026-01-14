package com.augmentalis.uuidmanager.models

/**
 * Result of a UUID command execution
 */
data class UUIDCommandResult(
    val success: Boolean,
    val targetUUID: String? = null,
    val action: String? = null,
    val message: String? = null,
    val data: Any? = null,
    val executionTime: Long = 0L,
    val error: String? = null
)