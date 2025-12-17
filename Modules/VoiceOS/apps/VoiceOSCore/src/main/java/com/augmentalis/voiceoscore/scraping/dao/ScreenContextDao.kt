/**
 * ScreenContextDao.kt - Data Access Object for ScreenContextEntity
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.augmentalis.voiceoscore.scraping.entities.ScreenContextEntity

/**
 * DAO for ScreenContextEntity
 *
 * Provides access to screen-level context data for AI inference.
 */
@Dao
interface ScreenContextDao {

    /**
     * Insert or replace screen context
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(screenContext: ScreenContextEntity): Long

    /**
     * Update screen context
     */
    @Update
    suspend fun update(screenContext: ScreenContextEntity)

    /**
     * Get screen context by hash
     */
    @Query("SELECT * FROM screen_contexts WHERE screen_hash = :screenHash")
    suspend fun getByScreenHash(screenHash: String): ScreenContextEntity?

    /**
     * Get all screen contexts for an app
     */
    @Query("SELECT * FROM screen_contexts WHERE app_id = :appId ORDER BY last_scraped DESC")
    suspend fun getScreensForApp(appId: String): List<ScreenContextEntity>

    /**
     * Get screen contexts by type
     */
    @Query("SELECT * FROM screen_contexts WHERE screen_type = :screenType ORDER BY last_scraped DESC")
    suspend fun getScreensByType(screenType: String): List<ScreenContextEntity>

    /**
     * Get most visited screens for an app
     */
    @Query("SELECT * FROM screen_contexts WHERE app_id = :appId ORDER BY visit_count DESC LIMIT :limit")
    suspend fun getMostVisitedScreens(appId: String, limit: Int = 10): List<ScreenContextEntity>

    /**
     * Get recently accessed screens
     */
    @Query("SELECT * FROM screen_contexts ORDER BY last_scraped DESC LIMIT :limit")
    suspend fun getRecentScreens(limit: Int = 20): List<ScreenContextEntity>

    /**
     * Update visit count and last scraped timestamp
     */
    @Query("UPDATE screen_contexts SET visit_count = visit_count + 1, last_scraped = :timestamp WHERE screen_hash = :screenHash")
    suspend fun incrementVisitCount(screenHash: String, timestamp: Long)

    /**
     * Delete screens for a specific app
     */
    @Query("DELETE FROM screen_contexts WHERE app_id = :appId")
    suspend fun deleteScreensForApp(appId: String): Int

    /**
     * Delete old screens (not visited for specified days)
     */
    @Query("DELETE FROM screen_contexts WHERE last_scraped < :timestamp")
    suspend fun deleteOldScreens(timestamp: Long): Int

    /**
     * Get total screen count
     */
    @Query("SELECT COUNT(*) FROM screen_contexts")
    suspend fun getScreenCount(): Int

    /**
     * Get screen count for app
     */
    @Query("SELECT COUNT(*) FROM screen_contexts WHERE app_id = :appId")
    suspend fun getScreenCountForApp(appId: String): Int

    /**
     * Get screen by hash (for testing)
     */
    @Query("SELECT * FROM screen_contexts WHERE screen_hash = :screenHash")
    suspend fun getScreenByHash(screenHash: String): ScreenContextEntity?

    /**
     * Delete screen by hash
     * Used for cascade delete testing
     */
    @Query("DELETE FROM screen_contexts WHERE screen_hash = :screenHash")
    suspend fun deleteByHash(screenHash: String): Int

    /**
     * Get all screens (for testing)
     */
    @Query("SELECT * FROM screen_contexts")
    suspend fun getAllScreens(): List<ScreenContextEntity>
}
