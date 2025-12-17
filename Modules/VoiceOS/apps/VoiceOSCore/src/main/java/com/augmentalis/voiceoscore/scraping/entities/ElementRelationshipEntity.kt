/**
 * ElementRelationshipEntity.kt - Element relationships for accessibility scraping
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
 * @property inferredBy How relationship was discovered ("accessibility_tree", "keyword_matching", "user_interaction")
 * @property createdAt Timestamp when relationship was created
 */
@Entity(
    tableName = "element_relationships",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],
            childColumns = ["source_element_hash"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],
            childColumns = ["target_element_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("source_element_hash"),
        Index("target_element_hash"),
        Index("relationship_type"),
        Index(value = ["source_element_hash", "target_element_hash", "relationship_type"], unique = true)
    ]
)
data class ElementRelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "source_element_hash")
    val sourceElementHash: String,

    @ColumnInfo(name = "target_element_hash")
    val targetElementHash: String?,

    @ColumnInfo(name = "relationship_type")
    val relationshipType: String,

    @ColumnInfo(name = "relationship_data")
    val relationshipData: String? = null,

    @ColumnInfo(name = "confidence")
    val confidence: Float = 1.0f,

    @ColumnInfo(name = "inferred_by")
    val inferredBy: String = "accessibility_tree",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
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
