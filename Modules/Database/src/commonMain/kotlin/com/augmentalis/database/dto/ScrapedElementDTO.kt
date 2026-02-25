/**
 * ScrapedElementDTO.kt - DTO for scraped UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-25
 *
 * Data Transfer Object for scraped UI elements.
 * Maps to ScrapedElement.sq schema.
 */

package com.augmentalis.database.dto

import com.augmentalis.database.Scraped_element

/**
 * DTO for scraped UI elements
 *
 * Full schema including semantic analysis, form context, and hierarchy information.
 * Migrated from Room to SQLDelight (Phase 2 - Agent 2)
 */
data class ScrapedElementDTO(
    val id: Long,
    val elementHash: String,
    val appId: String,
    val uuid: String?,
    val className: String,
    val viewIdResourceName: String?,
    val text: String?,
    val contentDescription: String?,
    val bounds: String,  // JSON string
    val isClickable: Long,  // SQLite Boolean (0/1)
    val isLongClickable: Long,
    val isEditable: Long,
    val isScrollable: Long,
    val isCheckable: Long,
    val isFocusable: Long,
    val isEnabled: Long,
    val depth: Long,
    val indexInParent: Long,
    val scrapedAt: Long,
    val semanticRole: String?,
    val inputType: String?,
    val visualWeight: String?,
    val isRequired: Long?,
    val formGroupId: String?,
    val placeholderText: String?,
    val validationPattern: String?,
    val backgroundColor: String?,
    val screen_hash: String?
)

/**
 * Extension to convert SQLDelight generated type to DTO
 */
fun Scraped_element.toScrapedElementDTO(): ScrapedElementDTO {
    return ScrapedElementDTO(
        id = id,
        elementHash = elementHash,
        appId = appId,
        uuid = uuid,
        className = className,
        viewIdResourceName = viewIdResourceName,
        text = text,
        contentDescription = contentDescription,
        bounds = bounds,
        isClickable = isClickable,
        isLongClickable = isLongClickable,
        isEditable = isEditable,
        isScrollable = isScrollable,
        isCheckable = isCheckable,
        isFocusable = isFocusable,
        isEnabled = isEnabled,
        depth = depth,
        indexInParent = indexInParent,
        scrapedAt = scrapedAt,
        semanticRole = semanticRole,
        inputType = inputType,
        visualWeight = visualWeight,
        isRequired = isRequired,
        formGroupId = formGroupId,
        placeholderText = placeholderText,
        validationPattern = validationPattern,
        backgroundColor = backgroundColor,
        screen_hash = screen_hash
    )
}
