/**
 * INavigationOperations.kt - Interface for navigation graph database operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Purpose: Segregated interface for navigation operations (Interface Segregation Principle)
 * Part of Phase 1: SOLID Refactoring - Interface Segregation
 */

package com.augmentalis.voiceoscore.learnapp.database.dao

import com.augmentalis.voiceoscore.learnapp.database.entities.NavigationEdgeEntity

/**
 * Interface for navigation graph database operations
 *
 * Focused interface with 3 core operations for navigation graphs.
 * Follows Interface Segregation Principle - clients depend only on methods they use.
 *
 * @see LearnAppDao Aggregate interface combining all operations
 */
interface INavigationOperations {

    /**
     * Insert a navigation edge
     *
     * @param edge NavigationEdgeEntity to insert
     */
    suspend fun insertNavigationEdge(edge: NavigationEdgeEntity)

    /**
     * Get navigation graph for a package
     *
     * @param packageName Package name
     * @return List of NavigationEdgeEntity records
     */
    suspend fun getNavigationGraph(packageName: String): List<NavigationEdgeEntity>

    /**
     * Delete navigation graph for a package
     *
     * @param packageName Package name
     */
    suspend fun deleteNavigationGraph(packageName: String)
}
