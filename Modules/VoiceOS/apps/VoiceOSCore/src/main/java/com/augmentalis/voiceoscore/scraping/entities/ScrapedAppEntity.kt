/**
 * ScrapedAppEntity.kt - App metadata for accessibility scraping database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 * Migrated to SQLDelight: 2025-12-17
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing a scraped application
 *
 * MIGRATION NOTE: This entity has been migrated to use SQLDelight.
 * The schema is defined in: core/database/src/commonMain/sqldelight/com/augmentalis/database/ScrapedApp.sq
 *
 * This entity stores high-level metadata about apps that have been analyzed
 * by the accessibility service. The app_hash serves as a unique fingerprint
 * combining package name and version code.
 *
 * @property appId Unique identifier (UUID) generated for this app scraping session
 * @property packageName Android package name (e.g., "com.example.app")
 * @property versionCode Android version code (integer)
 * @property versionName Android version name (e.g., "1.2.3")
 * @property appHash MD5 hash of packageName + versionCode for version detection
 * @property isFullyLearned Whether app has been fully learned via LearnApp mode
 * @property learnCompletedAt Timestamp when LearnApp mode completed (null if not completed)
 * @property scrapingMode Current scraping mode (DYNAMIC or LEARN_APP)
 * @property scrapeCount Number of times app has been scraped
 * @property elementCount Total number of UI elements scraped from this app
 * @property commandCount Total number of voice commands generated for this app
 * @property firstScrapedAt Timestamp when app was first scraped (milliseconds)
 * @property lastScrapedAt Timestamp when app was last scraped (milliseconds)
 */
data class ScrapedAppEntity(
    val appId: String,
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val appHash: String,
    val isFullyLearned: Long = 0,
    val learnCompletedAt: Long? = null,
    val scrapingMode: String = "DYNAMIC",
    val scrapeCount: Long = 0,
    val elementCount: Long = 0,
    val commandCount: Long = 0,
    val firstScrapedAt: Long,
    val lastScrapedAt: Long
)
