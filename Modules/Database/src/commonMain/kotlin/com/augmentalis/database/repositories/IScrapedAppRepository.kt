/**
 * IScrapedAppRepository.kt - Repository interface for scraped app metadata
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.ScrapedAppDTO

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
     * Get app by package hash (for compact AVID format lookups).
     * @param pkgHash 6-char hex hash of reversed package name
     */
    suspend fun getByPkgHash(pkgHash: String): ScrapedAppDTO?

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
