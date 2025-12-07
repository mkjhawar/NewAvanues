/**
 * ScreenContextEntity.kt - Screen-level context for accessibility scraping
 *
 * Migrated from Room to SQLDelight (Phase 2)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Scraping Migration Specialist (Agent 2)
 * Created: 2025-11-27
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing screen-level context information
 *
 * Captures high-level context about the screen/window where elements appear.
 * Enables AI to understand flows, navigation patterns, and screen purpose.
 *
 * @property id Auto-generated primary key
 * @property screenHash MD5 hash of screen signature
 * @property appId Foreign key to ScrapedAppEntity
 * @property packageName Package name of the app
 * @property activityName Activity/window class name (if available)
 * @property windowTitle Window title or header text
 * @property screenType Inferred screen type (e.g., "login", "checkout", "settings")
 * @property formContext Form-specific context (e.g., "registration", "payment")
 * @property navigationLevel Depth in navigation hierarchy (0 = main screen)
 * @property primaryAction Primary user action on this screen
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
    val navigationLevel: Int = 0,
    val primaryAction: String?,
    val elementCount: Int = 0,
    val hasBackButton: Boolean = false,
    val firstScraped: Long = System.currentTimeMillis(),
    val lastScraped: Long = System.currentTimeMillis(),
    val visitCount: Int = 1
)
