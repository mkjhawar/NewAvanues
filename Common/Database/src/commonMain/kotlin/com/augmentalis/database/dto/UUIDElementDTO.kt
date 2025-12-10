/**
 * UUIDElementDTO.kt - Data transfer object for UUID elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanues.database.dto

import com.avanues.database.uuid.Uuid_elements

/**
 * DTO for UUID element data.
 */
data class UUIDElementDTO(
    val uuid: String,
    val name: String?,
    val type: String,
    val description: String?,
    val parentUuid: String?,
    val isEnabled: Boolean,
    val priority: Int,
    val timestamp: Long,
    val metadataJson: String?,
    val positionJson: String?
)

/**
 * Convert SQLDelight entity to DTO.
 */
fun Uuid_elements.toUUIDElementDTO(): UUIDElementDTO = UUIDElementDTO(
    uuid = uuid,
    name = name,
    type = type,
    description = description,
    parentUuid = parent_uuid,
    isEnabled = is_enabled == 1L,
    priority = priority.toInt(),
    timestamp = timestamp,
    metadataJson = metadata_json,
    positionJson = position_json
)
