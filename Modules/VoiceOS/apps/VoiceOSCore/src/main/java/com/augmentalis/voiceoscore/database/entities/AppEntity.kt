/**
 * AppEntity.kt - Entity representing an installed/learned app
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Data class representing an app in the VoiceOS database.
 * Used for both installed apps and learned apps tracking.
 */

package com.augmentalis.voiceoscore.database.entities

import android.graphics.drawable.Drawable

/**
 * App Entity
 *
 * Represents an installed or learned application in VoiceOS.
 * Contains metadata about the app and its learning/exploration status.
 *
 * @property appId Unique identifier (usually package name)
 * @property packageName Android package name
 * @property appName Display name of the app
 * @property icon App icon drawable (nullable, not persisted to database)
 * @property isSystemApp Whether this is a system app
 * @property versionCode App version code
 * @property versionName App version name
 * @property installTime Installation timestamp
 * @property updateTime Last update timestamp
 * @property isFullyLearned Whether app has been fully learned
 * @property exploredElementCount Number of elements explored
 * @property scrapedElementCount Number of elements scraped
 * @property totalScreens Total screens discovered
 * @property lastExplored Last exploration timestamp
 * @property lastScraped Last scraping timestamp
 * @property learnAppEnabled Whether LearnApp is enabled for this app
 * @property dynamicScrapingEnabled Whether dynamic scraping is enabled
 * @property maxScrapeDepth Maximum scraping depth
 * @property appHash Hash of app structure for change detection
 */
data class AppEntity(
    val appId: String,
    val packageName: String,
    val appName: String = "",
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val versionCode: Long = 0L,
    val versionName: String = "",
    val installTime: Long = 0L,
    val updateTime: Long = 0L,
    val isFullyLearned: Boolean? = false,
    val exploredElementCount: Int? = 0,
    val scrapedElementCount: Int? = 0,
    val totalScreens: Int? = 0,
    val lastExplored: Long? = null,
    val lastScraped: Long? = null,
    val learnAppEnabled: Boolean? = true,
    val dynamicScrapingEnabled: Boolean? = false,
    val maxScrapeDepth: Int? = 5,
    val appHash: String? = null
) {
    companion object {
        /**
         * Create entity from package name with defaults
         */
        fun fromPackageName(packageName: String, appName: String = ""): AppEntity {
            return AppEntity(
                appId = packageName,
                packageName = packageName,
                appName = appName
            )
        }
    }
}
