/**
 * ScrapedElementEntity.kt - UI element data for accessibility scraping
 *
 * Migrated from Room to SQLDelight (Phase 2)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Scraping Migration Specialist (Agent 2)
 * Created: 2025-11-27
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing a scraped UI element
 *
 * This entity stores detailed information about individual UI elements discovered
 * through the accessibility service. Elements are hashed for fast lookup.
 *
 * @property id Auto-generated primary key (SQLDelight)
 * @property elementHash MD5 hash of unique identifier (className + viewId + text + contentDesc)
 * @property appId Foreign key to ScrapedAppEntity
 * @property uuid Universal unique identifier from UUIDCreator
 * @property className Android view class name (e.g., "android.widget.Button")
 * @property viewIdResourceName Resource ID if available
 * @property text Visible text content
 * @property contentDescription Accessibility content description
 * @property bounds JSON string representing element bounds
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
 * @property semanticRole Inferred semantic role/purpose
 * @property inputType Type of input for editable fields
 * @property visualWeight Visual emphasis level
 * @property isRequired Whether this field is required (for forms)
 * @property formGroupId Identifier linking related form fields together
 * @property placeholderText Placeholder/hint text for input fields
 * @property validationPattern Expected input validation pattern
 * @property backgroundColor Background color in hex format
 */
data class ScrapedElementEntity(
    val id: Long = 0,
    val elementHash: String,
    val appId: String,
    val uuid: String? = null,
    val className: String,
    val viewIdResourceName: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val bounds: String,
    val isClickable: Boolean = false,
    val isLongClickable: Boolean = false,
    val isEditable: Boolean = false,
    val isScrollable: Boolean = false,
    val isCheckable: Boolean = false,
    val isFocusable: Boolean = false,
    val isEnabled: Boolean = true,
    val depth: Int = 0,
    val indexInParent: Int = 0,
    val scrapedAt: Long = System.currentTimeMillis(),
    val semanticRole: String? = null,
    val inputType: String? = null,
    val visualWeight: String? = null,
    val isRequired: Boolean = false,
    val formGroupId: String? = null,
    val placeholderText: String? = null,
    val validationPattern: String? = null,
    val backgroundColor: String? = null
)
