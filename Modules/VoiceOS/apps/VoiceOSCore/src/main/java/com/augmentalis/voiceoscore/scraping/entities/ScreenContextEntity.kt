/**
 * ScreenContextEntity.kt - Screen-level context for accessibility scraping
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-18
 * Migrated to SQLDelight: 2025-12-17
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing screen-level context information
 *
 * MIGRATION NOTE: This entity has been migrated to use SQLDelight.
 * The schema is defined in: core/database/src/commonMain/sqldelight/com/augmentalis/database/ScreenContext.sq
 *
 * Captures high-level context about the screen/window where elements appear.
 * Enables AI to understand flows, navigation patterns, and screen purpose.
 *
 * @property id Auto-generated primary key
 * @property screenHash MD5 hash of screen signature (packageName + activityName + window title)
 * @property appId Foreign key to ScrapedAppEntity
 * @property packageName Package name of the app
 * @property activityName Activity/window class name (if available)
 * @property windowTitle Window title or header text
 * @property screenType Inferred screen type (e.g., "login", "checkout", "settings", "home", "form")
 * @property formContext Form-specific context (e.g., "registration", "payment", "search")
 * @property navigationLevel Depth in navigation hierarchy (0 = main screen, 1+ = nested)
 * @property primaryAction Primary user action on this screen (e.g., "submit", "search", "browse")
 * @property elementCount Number of interactive elements on screen
 * @property hasBackButton Whether screen has back navigation
 * @property firstScraped Timestamp when screen was first scraped
 * @property lastScraped Timestamp when screen was last scraped
 * @property visitCount Number of times screen has been scraped
 */
data class ScreenContextEntity(
    val id: Long = 0,
    val screenHash: String,
    val appId: String,
    val packageName: String,
    val activityName: String?,
    val windowTitle: String?,
    val screenType: String?,
    val formContext: String?,
    val navigationLevel: Long = 0,
    val primaryAction: String?,
    val elementCount: Long = 0,
    val hasBackButton: Long = 0,
    val firstScraped: Long = System.currentTimeMillis(),
    val lastScraped: Long = System.currentTimeMillis(),
    val visitCount: Long = 1
)
