/**
 * ElementRelationshipEntity.kt - Element relationships for accessibility scraping
 *
 * Migrated from Room to SQLDelight (Phase 2)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Scraping Migration Specialist (Agent 3B)
 * Created: 2025-11-27
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing relationships between UI elements
 *
 * Models semantic relationships between elements for better context understanding.
 * Examples:
 * - Button submits a form (relationship: "button_submits_form")
 * - Label describes an input field (relationship: "label_for")
 * - Elements belong to same form group (relationship: "form_group_member")
 * - Button navigates to another screen (relationship: "navigates_to")
 *
 * @property id Auto-generated primary key
 * @property sourceElementHash Hash of source element
 * @property targetElementHash Hash of target element (nullable for non-element targets)
 * @property relationshipType Type of relationship (e.g., "submits", "labels", "grouped_with")
 * @property relationshipData Additional JSON data about relationship
 * @property confidence Confidence score for inferred relationships (0.0-1.0)
 * @property createdAt Timestamp when relationship was created
 * @property updatedAt Timestamp when relationship was last updated
 */
data class ElementRelationshipEntity(
    val id: Long? = null,
    val sourceElementHash: String,
    val targetElementHash: String?,
    val relationshipType: String,
    val relationshipData: String? = null,
    val confidence: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Common relationship types
 */
object RelationshipType {
    // Form relationships
    const val FORM_GROUP_MEMBER = "form_group_member"
    const val BUTTON_SUBMITS_FORM = "button_submits_form"
    const val LABEL_FOR = "label_for"
    const val ERROR_FOR = "error_for"

    // Navigation relationships
    const val NAVIGATES_TO = "navigates_to"
    const val BACK_TO = "back_to"

    // Content relationships
    const val DESCRIBES = "describes"
    const val CONTAINS = "contains"
    const val EXPANDS_TO = "expands_to"

    // Action relationships
    const val TRIGGERS = "triggers"
    const val TOGGLES = "toggles"
    const val FILTERS = "filters"
}
