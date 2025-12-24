/**
 * VUIDAliasDTO.kt - Data transfer object for UUID aliases
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto

import com.augmentalis.database.uuid.Uuid_aliases

/**
 * DTO for UUID alias data.
 */
data class VUIDAliasDTO(
    val id: Long,
    val alias: String,
    val uuid: String,
    val isPrimary: Boolean,
    val createdAt: Long
)

/**
 * Convert SQLDelight entity to DTO.
 */
fun Uuid_aliases.toVUIDAliasDTO(): UUIDAliasDTO = VUIDAliasDTO(
    id = id,
    alias = alias,
    uuid = uuid,
    isPrimary = is_primary == 1L,
    createdAt = created_at
)
