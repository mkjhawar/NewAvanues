/**
 * AvidElementDTO.kt - Data Transfer Object for AVID elements
 *
 * Maps to the avid_elements SQLDelight table.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto

/**
 * DTO representing an AVID element.
 *
 * @param avid Unique AVID identifier (primary key)
 * @param name Display name of the element
 * @param type Element type (e.g., "button", "text", "container")
 * @param description Optional description
 * @param parentAvid Parent AVID for hierarchy
 * @param isEnabled Whether the element is enabled
 * @param priority Ordering priority (higher = more important)
 * @param timestamp Creation/update timestamp
 * @param metadataJson JSON string for extra metadata
 * @param positionJson JSON string for position data
 */
data class AvidElementDTO(
    val avid: String,
    val name: String?,
    val type: String,
    val description: String? = null,
    val parentAvid: String? = null,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val timestamp: Long,
    val metadataJson: String? = null,
    val positionJson: String? = null
)
