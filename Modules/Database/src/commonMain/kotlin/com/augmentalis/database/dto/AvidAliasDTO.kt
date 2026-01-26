/**
 * AvidAliasDTO.kt - Data Transfer Object for AVID aliases
 *
 * Maps to the avid_aliases SQLDelight table.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto

/**
 * DTO representing an alias for an AVID element.
 *
 * @param id Auto-generated ID
 * @param alias Unique alias string
 * @param avid AVID this alias points to
 * @param isPrimary Whether this is the primary alias
 * @param createdAt Creation timestamp
 */
data class AvidAliasDTO(
    val id: Long = 0,
    val alias: String,
    val avid: String,
    val isPrimary: Boolean = false,
    val createdAt: Long
)
