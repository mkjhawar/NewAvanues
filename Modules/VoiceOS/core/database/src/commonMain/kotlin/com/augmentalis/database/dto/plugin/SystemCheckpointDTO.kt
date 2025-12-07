/**
 * SystemCheckpointDTO.kt - Data Transfer Object for System Checkpoint
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto.plugin

/**
 * Data Transfer Object for SystemCheckpoint entity.
 * Represents system state checkpoints for rollback capability.
 */
data class SystemCheckpointDTO(
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val stateJson: String,
    val pluginStatesJson: String?
)

/**
 * Transaction type for checkpoint operations.
 */
enum class TransactionType {
    INSTALL,
    UPDATE,
    UNINSTALL,
    ENABLE,
    DISABLE
}
