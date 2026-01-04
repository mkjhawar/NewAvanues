/**
 * ScrapedAppEntity.kt - App metadata for accessibility scraping
 *
 * Migrated from Room to SQLDelight (Phase 2)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Scraping Migration Specialist (Agent 2)
 * Created: 2025-11-27
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing a scraped application
 *
 * This entity stores high-level metadata about apps that have been analyzed
 * by the accessibility service. The app_hash serves as a unique fingerprint
 * combining package name and version code.
 *
 * @property appId Unique identifier (UUID) generated for this app scraping session
 * @property packageName Android package name (e.g., "com.example.app")
 * @property appName Human-readable app name (e.g., "Example App")
 * @property versionCode Android version code (integer)
 * @property versionName Android version name (e.g., "1.2.3")
 * @property appHash MD5 hash of packageName + versionCode for version detection
 * @property firstScraped Timestamp when app was first scraped (milliseconds)
 * @property lastScraped Timestamp when app was last scraped (milliseconds)
 * @property scrapeCount Number of times app has been scraped
 * @property elementCount Total number of UI elements scraped from this app
 * @property commandCount Total number of voice commands generated for this app
 * @property isFullyLearned Whether app has been fully learned via LearnApp mode
 * @property learnCompletedAt Timestamp when LearnApp mode completed (null if not completed)
 * @property scrapingMode Current scraping mode (DYNAMIC or LEARN_APP)
 */
data class ScrapedAppEntity(
    val appId: String,
    val packageName: String,
    val appName: String,
    val versionCode: Int,
    val versionName: String,
    val appHash: String,
    val firstScraped: Long,
    val lastScraped: Long,
    val scrapeCount: Int = 1,
    val elementCount: Int = 0,
    val commandCount: Int = 0,
    val isFullyLearned: Boolean = false,
    val learnCompletedAt: Long? = null,
    val scrapingMode: String = "DYNAMIC"
)
