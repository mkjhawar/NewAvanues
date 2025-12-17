/**
 * ScrapedAppDao.kt - Data Access Object for scraped app metadata
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.scraping.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.augmentalis.voiceoscore.scraping.entities.ScrapedAppEntity

/**
 * DAO for ScrapedAppEntity operations
 *
 * Provides database access for app metadata including insertion, querying,
 * and cleanup operations. Uses hash-based lookups for efficient app
 * version detection.
 */
@Dao
interface ScrapedAppDao {

    /**
     * Insert a new scraped app entry
     * Replaces existing entry if app_id already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ScrapedAppEntity): Long

    /**
     * Insert multiple apps in batch
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(apps: List<ScrapedAppEntity>)

    /**
     * Update existing app entry
     */
    @Update
    suspend fun update(app: ScrapedAppEntity)

    /**
     * Find app by app ID
     */
    @Query("SELECT * FROM scraped_apps WHERE app_id = :appId")
    suspend fun getAppById(appId: String): ScrapedAppEntity?

    /**
     * Find app by hash (packageName + versionCode fingerprint)
     * This is the primary method for detecting if an app version has been scraped
     */
    @Query("SELECT * FROM scraped_apps WHERE app_hash = :appHash")
    suspend fun getAppByHash(appHash: String): ScrapedAppEntity?

    /**
     * Find apps by package name (returns all versions)
     */
    @Query("SELECT * FROM scraped_apps WHERE package_name = :packageName ORDER BY version_code DESC")
    suspend fun getAppsByPackageName(packageName: String): List<ScrapedAppEntity>

    /**
     * Get all scraped apps ordered by last scraped time
     */
    @Query("SELECT * FROM scraped_apps ORDER BY last_scraped DESC")
    suspend fun getAllApps(): List<ScrapedAppEntity>

    /**
     * Increment scrape count and update last_scraped timestamp
     */
    @Query("UPDATE scraped_apps SET scrape_count = scrape_count + 1, last_scraped = :timestamp WHERE app_id = :appId")
    suspend fun incrementScrapeCount(appId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Update element count for an app
     */
    @Query("UPDATE scraped_apps SET element_count = :count WHERE app_id = :appId")
    suspend fun updateElementCount(appId: String, count: Int)

    /**
     * Update command count for an app
     */
    @Query("UPDATE scraped_apps SET command_count = :count WHERE app_id = :appId")
    suspend fun updateCommandCount(appId: String, count: Int)

    /**
     * Delete apps older than specified timestamp
     * Used for automatic cleanup (e.g., delete apps not scraped in 7 days)
     *
     * @param timestamp Delete apps where last_scraped < this timestamp
     * @return Number of apps deleted
     */
    @Query("DELETE FROM scraped_apps WHERE last_scraped < :timestamp")
    suspend fun deleteAppsOlderThan(timestamp: Long): Int

    /**
     * Delete app by app ID (cascades to elements and commands)
     */
    @Query("DELETE FROM scraped_apps WHERE app_id = :appId")
    suspend fun deleteApp(appId: String)

    /**
     * Delete app by hash
     */
    @Query("DELETE FROM scraped_apps WHERE app_hash = :appHash")
    suspend fun deleteAppByHash(appHash: String)

    /**
     * Get total number of scraped apps
     */
    @Query("SELECT COUNT(*) FROM scraped_apps")
    suspend fun getAppCount(): Int

    /**
     * Check if app hash exists (fast existence check)
     */
    @Query("SELECT EXISTS(SELECT 1 FROM scraped_apps WHERE app_hash = :appHash)")
    suspend fun appHashExists(appHash: String): Boolean

    /**
     * Mark app as fully learned and record completion timestamp
     *
     * Called when LearnApp mode successfully completes comprehensive UI traversal.
     * Sets isFullyLearned flag to true and records completion timestamp.
     *
     * @param appId App identifier
     * @param timestamp Completion timestamp (defaults to current time)
     */
    @Query("UPDATE scraped_apps SET is_fully_learned = 1, learn_completed_at = :timestamp WHERE app_id = :appId")
    suspend fun markAsFullyLearned(appId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Update scraping mode for an app
     *
     * Used to track whether app is in DYNAMIC or LEARN_APP mode.
     * Typically set to LEARN_APP during active traversal, then back to DYNAMIC after completion.
     *
     * @param appId App identifier
     * @param mode Scraping mode ("DYNAMIC" or "LEARN_APP")
     */
    @Query("UPDATE scraped_apps SET scraping_mode = :mode WHERE app_id = :appId")
    suspend fun updateScrapingMode(appId: String, mode: String)

    /**
     * Get all apps that have been fully learned
     *
     * Useful for UI to show which apps have complete coverage.
     *
     * @return List of fully learned apps, ordered by learn completion time
     */
    @Query("SELECT * FROM scraped_apps WHERE is_fully_learned = 1 ORDER BY learn_completed_at DESC")
    suspend fun getFullyLearnedApps(): List<ScrapedAppEntity>

    /**
     * Get apps that have NOT been fully learned yet
     *
     * Useful for suggesting which apps to run LearnApp mode on.
     *
     * @return List of apps with only partial (dynamic) coverage
     */
    @Query("SELECT * FROM scraped_apps WHERE is_fully_learned = 0 ORDER BY last_scraped DESC")
    suspend fun getPartiallyLearnedApps(): List<ScrapedAppEntity>

    /**
     * Delete app by app ID
     * Used for cascade delete testing
     */
    @Query("DELETE FROM scraped_apps WHERE app_id = :appId")
    suspend fun deleteByAppId(appId: String): Int
}
