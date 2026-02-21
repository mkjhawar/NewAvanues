/**
 * ElementRelationshipDTO.kt - DTO for semantic element relationships
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-26
 *
 * Data Transfer Object for element relationships.
 * Maps to ElementRelationship.sq schema.
 */

package com.augmentalis.database.dto

import com.augmentalis.database.Element_relationship

/**
 * DTO for element relationships (label-for, triggers, navigates-to, etc.)
 */
data class ElementRelationshipDTO(
    val id: Long,
    val sourceElementHash: String,
    val targetElementHash: String?,
    val relationshipType: String,
    val relationshipData: String? = null,
    val confidence: Double = 1.0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

/**
 * Extension to convert SQLDelight generated type to DTO
 */
fun Element_relationship.toElementRelationshipDTO(): ElementRelationshipDTO {
    return ElementRelationshipDTO(
        id = id,
        sourceElementHash = sourceElementHash,
        targetElementHash = targetElementHash,
        relationshipType = relationshipType,
        relationshipData = relationshipData,
        confidence = confidence ?: 1.0,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
