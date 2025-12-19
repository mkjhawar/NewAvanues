/**
 * ScrapedElementEntity.kt - UI element data for accessibility scraping database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 * Migrated to SQLDelight: 2025-12-17
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing a scraped UI element
 *
 * MIGRATION NOTE: This entity has been migrated to use SQLDelight.
 * The schema is defined in: core/database/src/commonMain/sqldelight/com/augmentalis/database/ScrapedElement.sq
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
 * @property screen_hash Screen hash for grouping elements by screen
 */
data class ScrapedElementEntity(
    val id: Long = 0,
    val elementHash: String,
    val appId: String,
    val uuid: String? = null,
    val className: String,
    val viewIdResourceName: String?,
    val text: String?,
    val contentDescription: String?,
    val bounds: String,
    val isClickable: Long,
    val isLongClickable: Long,
    val isEditable: Long,
    val isScrollable: Long,
    val isCheckable: Long,
    val isFocusable: Long,
    val isEnabled: Long = 1,
    val depth: Long,
    val indexInParent: Long,
    val scrapedAt: Long = System.currentTimeMillis(),
    val semanticRole: String? = null,
    val inputType: String? = null,
    val visualWeight: String? = null,
    val isRequired: Long? = 0,
    val formGroupId: String? = null,
    val placeholderText: String? = null,
    val validationPattern: String? = null,
    val backgroundColor: String? = null,
    val screen_hash: String? = null
)
