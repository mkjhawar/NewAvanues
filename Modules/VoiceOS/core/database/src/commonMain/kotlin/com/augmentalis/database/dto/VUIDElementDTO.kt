/**
 * VUIDElementDTO.kt - Data Transfer Object for VUID elements
 *
 * Maps to the vuid_elements SQLDelight table.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto

/**
 * DTO representing a VUID element.
 *
 * @param vuid Unique VUID identifier (primary key)
 * @param name Display name of the element
 * @param type Element type (e.g., "button", "text", "container")
 * @param description Optional description
 * @param parentVuid Parent VUID for hierarchy
 * @param isEnabled Whether the element is enabled
 * @param priority Ordering priority (higher = more important)
 * @param timestamp Creation/update timestamp
 * @param metadataJson JSON string for extra metadata
 * @param positionJson JSON string for position data
 */
data class VUIDElementDTO(
    val vuid: String,
    val name: String?,
    val type: String,
    val description: String? = null,
    val parentVuid: String? = null,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val timestamp: Long,
    val metadataJson: String? = null,
    val positionJson: String? = null
)
