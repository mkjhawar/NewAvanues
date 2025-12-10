/**
 * IScrapedAppRepository.kt - Repository interface for scraped app metadata
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.avanues.database.repositories

import com.avanues.database.dto.ScrapedAppDTO

/**
 * Repository interface for scraped app metadata.
 * Provides CRUD operations for app scraping data.
 */
interface IScrapedAppRepository {

    /**
     * Insert or replace a scraped app entry.
     */
    suspend fun insert(app: ScrapedAppDTO)

    /**
     * Get app by ID.
     */
    suspend fun getById(appId: String): ScrapedAppDTO?

    /**
     * Get app by package name.
     */
    suspend fun getByPackage(packageName: String): ScrapedAppDTO?

    /**
     * Get all scraped apps.
     */
    suspend fun getAll(): List<ScrapedAppDTO>

    /**
     * Get fully learned apps.
     */
    suspend fun getFullyLearned(): List<ScrapedAppDTO>

    /**
     * Update app statistics (scrape count, element count, command count).
     */
    suspend fun updateStats(
        appId: String,
        scrapeCount: Long,
        elementCount: Long,
        commandCount: Long,
        lastScrapedAt: Long
    )

    /**
     * Mark app as fully learned.
     */
    suspend fun markFullyLearned(appId: String, learnCompletedAt: Long)

    /**
     * Delete app by ID (cascades to elements and commands).
     */
    suspend fun deleteById(appId: String)

    /**
     * Delete all apps.
     */
    suspend fun deleteAll()

    /**
     * Count all apps.
     */
    suspend fun count(): Long
}
