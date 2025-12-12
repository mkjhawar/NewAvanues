/**
 * IScreenContextRepository.kt - Repository interface for screen context
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.ScreenContextDTO

/**
 * Repository interface for screen context metadata.
 * Provides CRUD operations for screen data.
 */
interface IScreenContextRepository {

    /**
     * Insert or replace screen context.
     */
    suspend fun insert(context: ScreenContextDTO)

    /**
     * Get screen by hash.
     */
    suspend fun getByHash(screenHash: String): ScreenContextDTO?

    /**
     * Get all screens for an app.
     */
    suspend fun getByApp(appId: String): List<ScreenContextDTO>

    /**
     * Get all screens by package name.
     */
    suspend fun getByPackage(packageName: String): List<ScreenContextDTO>

    /**
     * Get screens by activity name.
     */
    suspend fun getByActivity(activityName: String): List<ScreenContextDTO>

    /**
     * Get all screens.
     */
    suspend fun getAll(): List<ScreenContextDTO>

    /**
     * Delete screen by hash.
     */
    suspend fun deleteByHash(screenHash: String)

    /**
     * Delete all screens for an app.
     */
    suspend fun deleteByApp(appId: String)

    /**
     * Delete all screens.
     */
    suspend fun deleteAll()

    /**
     * Count all screens.
     */
    suspend fun count(): Long

    /**
     * Count screens by app.
     */
    suspend fun countByApp(appId: String): Long
}
