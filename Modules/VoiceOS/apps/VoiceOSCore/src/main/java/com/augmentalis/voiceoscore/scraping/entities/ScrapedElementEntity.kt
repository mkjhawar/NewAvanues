/**
 * ScrapedElementEntity.kt - UI element data for accessibility scraping database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.scraping.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a scraped UI element
 *
 * This entity stores detailed information about individual UI elements discovered
 * through the accessibility service. Elements are hashed for fast lookup and
 * linked to their parent app via foreign key.
 *
 * @property id Auto-generated primary key
 * @property elementHash MD5 hash of unique identifier (className + viewId + text + contentDesc)
 * @property appId Foreign key to ScrapedAppEntity
 * @property uuid Universal unique identifier from UUIDCreator (enables cross-system element identification)
 * @property className Android view class name (e.g., "android.widget.Button")
 * @property viewIdResourceName Resource ID if available (e.g., "com.example:id/submit_button")
 * @property text Visible text content
 * @property contentDescription Accessibility content description
 * @property bounds JSON string representing element bounds {"left":0,"top":0,"right":100,"bottom":50}
 * @property isClickable Whether element supports click actions
 * @property isLongClickable Whether element supports long click actions
 * @property isEditable Whether element accepts text input
 * @property isScrollable Whether element supports scrolling
 * @property isCheckable Whether element can be checked/unchecked
 * @property isFocusable Whether element can receive focus
 * @property isEnabled Whether element is currently enabled
 * @property depth Depth in the accessibility tree hierarchy
 * @property indexInParent Index among siblings in parent container
 * @property scrapedAt Timestamp when element was scraped (milliseconds)
 * @property semanticRole Inferred semantic role/purpose (e.g., "submit_login", "input_email", "navigate_back")
 * @property inputType Type of input for editable fields (e.g., "email", "password", "phone", "text")
 * @property visualWeight Visual emphasis level ("primary", "secondary", "tertiary", "danger")
 * @property isRequired Whether this field is required (for forms)
 * @property formGroupId Identifier linking related form fields together
 * @property placeholderText Placeholder/hint text for input fields
 * @property validationPattern Expected input validation pattern (e.g., email format, phone format)
 * @property backgroundColor Background color in hex format (for visual prominence detection)
 */
@Entity(
    tableName = "scraped_elements",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedAppEntity::class,
            parentColumns = ["app_id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("app_id"),
        Index(value = ["element_hash"], unique = true),
        Index("view_id_resource_name"),
        Index("uuid")
    ]
)
data class ScrapedElementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "element_hash")
    val elementHash: String,

    @ColumnInfo(name = "app_id")
    val appId: String,

    // Universal UUID for cross-system element identification
    @ColumnInfo(name = "uuid")
    val uuid: String? = null,

    // Accessibility properties
    @ColumnInfo(name = "class_name")
    val className: String,

    @ColumnInfo(name = "view_id_resource_name")
    val viewIdResourceName: String?,

    @ColumnInfo(name = "text")
    val text: String?,

    @ColumnInfo(name = "content_description")
    val contentDescription: String?,

    @ColumnInfo(name = "bounds")
    val bounds: String,

    // Action capabilities
    @ColumnInfo(name = "is_clickable")
    val isClickable: Boolean,

    @ColumnInfo(name = "is_long_clickable")
    val isLongClickable: Boolean,

    @ColumnInfo(name = "is_editable")
    val isEditable: Boolean,

    @ColumnInfo(name = "is_scrollable")
    val isScrollable: Boolean,

    @ColumnInfo(name = "is_checkable")
    val isCheckable: Boolean,

    @ColumnInfo(name = "is_focusable")
    val isFocusable: Boolean,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean,

    // Hierarchy position
    @ColumnInfo(name = "depth")
    val depth: Int,

    @ColumnInfo(name = "index_in_parent")
    val indexInParent: Int,

    // Metadata
    @ColumnInfo(name = "scraped_at")
    val scrapedAt: Long = System.currentTimeMillis(),

    // AI Context Inference (Phase 1)
    @ColumnInfo(name = "semantic_role")
    val semanticRole: String? = null,

    @ColumnInfo(name = "input_type")
    val inputType: String? = null,

    @ColumnInfo(name = "visual_weight")
    val visualWeight: String? = null,

    @ColumnInfo(name = "is_required")
    val isRequired: Boolean? = null,

    // AI Context Inference (Phase 2)
    @ColumnInfo(name = "form_group_id")
    val formGroupId: String? = null,

    @ColumnInfo(name = "placeholder_text")
    val placeholderText: String? = null,

    @ColumnInfo(name = "validation_pattern")
    val validationPattern: String? = null,

    @ColumnInfo(name = "background_color")
    val backgroundColor: String? = null
)
