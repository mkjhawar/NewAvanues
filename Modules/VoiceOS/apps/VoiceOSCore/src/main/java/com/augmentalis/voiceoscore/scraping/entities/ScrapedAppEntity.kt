/**
 * ScrapedAppEntity.kt - App metadata for accessibility scraping database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.scraping.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
@Entity(tableName = "scraped_apps")
data class ScrapedAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "app_id")
    val appId: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "version_code")
    val versionCode: Int,

    @ColumnInfo(name = "version_name")
    val versionName: String,

    @ColumnInfo(name = "app_hash")
    val appHash: String,

    @ColumnInfo(name = "first_scraped")
    val firstScraped: Long,

    @ColumnInfo(name = "last_scraped")
    val lastScraped: Long,

    @ColumnInfo(name = "scrape_count")
    val scrapeCount: Int = 1,

    @ColumnInfo(name = "element_count")
    val elementCount: Int = 0,

    @ColumnInfo(name = "command_count")
    val commandCount: Int = 0,

    /**
     * Whether app has been fully learned via LearnApp mode
     *
     * true = LearnApp mode has completed successfully
     * false = Only dynamic scraping has occurred (partial coverage)
     */
    @ColumnInfo(name = "is_fully_learned")
    val isFullyLearned: Boolean = false,

    /**
     * Timestamp when LearnApp mode completed
     *
     * null = App has not been fully learned yet
     * non-null = Timestamp (milliseconds) when comprehensive learning completed
     */
    @ColumnInfo(name = "learn_completed_at")
    val learnCompletedAt: Long? = null,

    /**
     * Current scraping mode
     *
     * DYNAMIC = Real-time scraping mode (default)
     * LEARN_APP = Full traversal mode (temporary during learning)
     *
     * Note: This typically stays DYNAMIC except during active LearnApp execution
     */
    @ColumnInfo(name = "scraping_mode")
    val scrapingMode: String = "DYNAMIC"
)
