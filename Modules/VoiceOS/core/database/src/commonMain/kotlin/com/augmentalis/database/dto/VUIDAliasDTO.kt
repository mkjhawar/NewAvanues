/**
 * VUIDAliasDTO.kt - Data Transfer Object for VUID aliases
 *
 * Maps to the vuid_aliases SQLDelight table.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto

/**
 * DTO representing an alias for a VUID element.
 *
 * @param id Auto-generated ID
 * @param alias Unique alias string
 * @param vuid VUID this alias points to
 * @param isPrimary Whether this is the primary alias
 * @param createdAt Creation timestamp
 */
data class VUIDAliasDTO(
    val id: Long = 0,
    val alias: String,
    val vuid: String,
    val isPrimary: Boolean = false,
    val createdAt: Long
)
