/**
 * AppEntity.kt - Minimal DTO for SQLDelight migration compatibility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-26 (SQLDelight migration - Room annotations removed)
 *
 * NOTE: This is a simple data class (not a Room entity).
 * Used by VoiceOSCoreDatabaseAdapter to bridge between
 * legacy Room-style API and new SQLDelight repositories.
 */
package com.augmentalis.voiceoscore.database.entities

/**
 * App Entity - DTO for app metadata
 *
 * Minimal compatibility layer during Room→SQLDelight migration.
 * Actual persistence handled by SQLDelight ScrapedAppDTO.
 */
data class AppEntity(
    val appId: String = "",
    val packageName: String,
    val appName: String = "",
    val icon: android.graphics.Bitmap? = null,
    val isSystemApp: Boolean = false,
    val versionCode: Long = 0,
    val versionName: String = "",
    val installTime: Long = 0,
    val updateTime: Long = 0,
    val isFullyLearned: Boolean = false,
    val exploredElementCount: Int = 0,
    val scrapedElementCount: Int = 0,
    val totalScreens: Int = 0,
    val lastExplored: Long? = null,
    val lastScraped: Long? = null,
    val learnAppEnabled: Boolean = true,
    val dynamicScrapingEnabled: Boolean = true,
    val maxScrapeDepth: Int = 5
)
