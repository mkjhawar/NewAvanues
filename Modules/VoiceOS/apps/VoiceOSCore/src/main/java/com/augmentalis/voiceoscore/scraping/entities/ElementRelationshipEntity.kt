/**
 * ElementRelationshipEntity.kt - Semantic element relationship data for accessibility scraping database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-17
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing semantic relationships between scraped UI elements
 *
 * MIGRATION NOTE: This entity has been migrated to use SQLDelight.
 * The schema is defined in: core/database/src/commonMain/sqldelight/com/augmentalis/database/ElementRelationship.sq
 *
 * This entity stores inferred semantic relationships between UI elements such as:
 * - Labels that describe input fields
 * - Buttons that submit forms
 * - Elements that trigger navigation
 * - Parent-child containment relationships
 *
 * These relationships are used to enhance voice command understanding and
 * provide better context for accessibility interactions.
 *
 * @property id Auto-generated primary key
 * @property sourceElementHash Hash of the source element in the relationship
 * @property targetElementHash Hash of the target element in the relationship
 * @property relationshipType Type of relationship (from RelationshipType constants)
 * @property relationshipData Additional JSON data about the relationship
 * @property confidence Confidence score for the inferred relationship (0.0 to 1.0)
 * @property inferredBy Method used to infer the relationship (e.g., "heuristic_proximity", "ml_model")
 * @property createdAt Timestamp when relationship was recorded (milliseconds)
 * @property updatedAt Timestamp when relationship was last updated (milliseconds)
 */
data class ElementRelationshipEntity(
    val id: Long = 0,
    val sourceElementHash: String,
    val targetElementHash: String,
    val relationshipType: String,
    val relationshipData: String? = null,
    val confidence: Float = 1.0f,
    val inferredBy: String = "unknown",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
