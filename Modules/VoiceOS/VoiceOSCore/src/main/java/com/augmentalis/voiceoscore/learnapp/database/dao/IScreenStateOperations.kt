/**
 * IScreenStateOperations.kt - Interface for screen state database operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Purpose: Segregated interface for screen state operations (Interface Segregation Principle)
 * Part of Phase 1: SOLID Refactoring - Interface Segregation
 */

package com.augmentalis.voiceoscore.learnapp.database.dao

import com.augmentalis.voiceoscore.learnapp.database.entities.ScreenStateEntity

/**
 * Interface for screen state database operations
 *
 * Focused interface with 3 core operations for screen states.
 * Follows Interface Segregation Principle - clients depend only on methods they use.
 *
 * @see LearnAppDao Aggregate interface combining all operations
 */
interface IScreenStateOperations {

    /**
     * Insert a screen state
     *
     * @param state ScreenStateEntity to insert
     */
    suspend fun insertScreenState(state: ScreenStateEntity)

    /**
     * Get a screen state by hash
     *
     * @param hash Screen hash
     * @return ScreenStateEntity or null if not found
     */
    suspend fun getScreenState(hash: String): ScreenStateEntity?

    /**
     * Get all screen states for a package
     *
     * @param packageName Package name
     * @return List of ScreenStateEntity records
     */
    suspend fun getScreenStatesForPackage(packageName: String): List<ScreenStateEntity>
}
