/**
 * ILearnedAppOperations.kt - Interface for learned app database operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Purpose: Segregated interface for learned app operations (Interface Segregation Principle)
 * Part of Phase 1: SOLID Refactoring - Interface Segregation
 */

package com.augmentalis.voiceoscore.learnapp.database.dao

import com.augmentalis.voiceoscore.learnapp.database.entities.LearnedAppEntity

/**
 * Interface for learned app database operations
 *
 * Focused interface with 4 core operations for learned apps.
 * Follows Interface Segregation Principle - clients depend only on methods they use.
 *
 * @see LearnAppDao Aggregate interface combining all operations
 */
interface ILearnedAppOperations {

    /**
     * Insert a new learned app
     *
     * @param app LearnedAppEntity to insert
     */
    suspend fun insertLearnedApp(app: LearnedAppEntity)

    /**
     * Get a learned app by package name
     *
     * @param packageName Package name to look up
     * @return LearnedAppEntity or null if not found
     */
    suspend fun getLearnedApp(packageName: String): LearnedAppEntity?

    /**
     * Get all learned apps
     *
     * @return List of all LearnedAppEntity records
     */
    suspend fun getAllLearnedApps(): List<LearnedAppEntity>

    /**
     * Delete a learned app
     *
     * @param app LearnedAppEntity to delete
     */
    suspend fun deleteLearnedApp(app: LearnedAppEntity)
}
