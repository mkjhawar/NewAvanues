/**
 * IScrapedElementRepository.kt - Repository interface for scraped UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.avanues.database.repositories

import com.avanues.database.dto.ScrapedElementDTO

/**
 * Repository interface for scraped UI elements.
 * Provides CRUD operations for element scraping data.
 */
interface IScrapedElementRepository {

    /**
     * Insert or replace a scraped element.
     */
    suspend fun insert(element: ScrapedElementDTO)

    /**
     * Get element by hash.
     */
    suspend fun getByHash(elementHash: String): ScrapedElementDTO?

    /**
     * Get element by hash for a specific app.
     * Prevents cross-app hash collisions by scoping lookup to a single app.
     *
     * @param elementHash Element fingerprint hash
     * @param appId Package name of the app
     * @return Element DTO if found, null otherwise
     */
    suspend fun getByHashAndApp(elementHash: String, appId: String): ScrapedElementDTO?

    /**
     * Get element by UUID for a specific app.
     */
    suspend fun getByUuid(appId: String, uuid: String): ScrapedElementDTO?

    /**
     * Get all elements for an app.
     */
    suspend fun getByApp(appId: String): List<ScrapedElementDTO>

    /**
     * Get all clickable elements for an app.
     */
    suspend fun getClickable(appId: String): List<ScrapedElementDTO>

    /**
     * Get all editable elements for an app.
     */
    suspend fun getEditable(appId: String): List<ScrapedElementDTO>

    /**
     * Get all scrollable elements for an app.
     */
    suspend fun getScrollable(appId: String): List<ScrapedElementDTO>

    /**
     * Get elements by class name for an app.
     */
    suspend fun getByClass(appId: String, className: String): List<ScrapedElementDTO>

    /**
     * Get elements by view ID for an app.
     */
    suspend fun getByViewId(appId: String, viewId: String): List<ScrapedElementDTO>

    /**
     * Delete element by hash.
     */
    suspend fun deleteByHash(elementHash: String)

    /**
     * Delete element by ID.
     */
    suspend fun deleteById(id: Long)

    /**
     * Delete all elements for an app.
     */
    suspend fun deleteByApp(appId: String)

    /**
     * Delete all elements.
     */
    suspend fun deleteAll()

    /**
     * Count all elements.
     */
    suspend fun count(): Long

    /**
     * Count elements by app.
     */
    suspend fun countByApp(appId: String): Long

    /**
     * Count elements by screen hash.
     */
    suspend fun countByScreenHash(appId: String, screenHash: String): Long

    /**
     * Get all elements for a screen hash.
     */
    suspend fun getByScreenHash(appId: String, screenHash: String): List<ScrapedElementDTO>
}
