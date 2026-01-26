/**
 * ScreenContextDTO.kt - DTO for screen context metadata
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 *
 * Data Transfer Object for screen context.
 * Maps to ScreenContext.sq schema.
 */

package com.augmentalis.database.dto

import com.augmentalis.database.Screen_context

/**
 * DTO for screen context
 *
 * Stores metadata about individual screens/activities within apps.
 * Migrated from Room to SQLDelight (Phase 2 - Agent 2)
 */
data class ScreenContextDTO(
    val id: Long,
    val screenHash: String,
    val appId: String,
    val packageName: String,
    val activityName: String?,
    val windowTitle: String?,
    val screenType: String?,
    val formContext: String?,
    val navigationLevel: Long,
    val primaryAction: String?,
    val elementCount: Long,
    val hasBackButton: Long,
    val firstScraped: Long,
    val lastScraped: Long,
    val visitCount: Long,
    /** JSON-serialized contextual text for NLU enhancement (screen titles, breadcrumbs, section headers) */
    val contextualText: String? = null
)

/**
 * Extension to convert SQLDelight generated type to DTO
 */
fun Screen_context.toScreenContextDTO(): ScreenContextDTO {
    return ScreenContextDTO(
        id = id,
        screenHash = screenHash,
        appId = appId,
        packageName = packageName,
        activityName = activityName,
        windowTitle = windowTitle,
        screenType = screenType,
        formContext = formContext,
        navigationLevel = navigationLevel,
        primaryAction = primaryAction,
        elementCount = elementCount,
        hasBackButton = hasBackButton,
        firstScraped = firstScraped,
        lastScraped = lastScraped,
        visitCount = visitCount,
        contextualText = contextualText
    )
}
