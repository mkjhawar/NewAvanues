/**
 * IUserInteractionRepository.kt - Repository interface for user interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.avanues.database.repositories

import com.avanues.database.dto.UserInteractionDTO

/**
 * Repository interface for user interaction tracking.
 * Tracks user interactions with UI elements for analytics.
 */
interface IUserInteractionRepository {

    /**
     * Insert a new interaction record.
     * @return The ID of the inserted interaction.
     */
    suspend fun insert(interaction: UserInteractionDTO): Long

    /**
     * Get interaction by ID.
     */
    suspend fun getById(id: Long): UserInteractionDTO?

    /**
     * Get all interactions for an element.
     */
    suspend fun getByElement(elementHash: String): List<UserInteractionDTO>

    /**
     * Get all interactions for a screen.
     */
    suspend fun getByScreen(screenHash: String): List<UserInteractionDTO>

    /**
     * Get interactions by type.
     */
    suspend fun getByType(interactionType: String): List<UserInteractionDTO>

    /**
     * Get interactions within a time range.
     */
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<UserInteractionDTO>

    /**
     * Get recent interactions.
     */
    suspend fun getRecent(limit: Long): List<UserInteractionDTO>

    /**
     * Delete interaction by ID.
     */
    suspend fun deleteById(id: Long)

    /**
     * Delete all interactions for an element.
     */
    suspend fun deleteByElement(elementHash: String)

    /**
     * Delete interactions older than timestamp.
     */
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Delete all interactions.
     */
    suspend fun deleteAll()

    /**
     * Count all interactions.
     */
    suspend fun count(): Long

    /**
     * Count interactions by type.
     */
    suspend fun countByType(interactionType: String): Long

    /**
     * Get interaction count for a specific element.
     * Used by CommandGenerator for confidence weighting.
     */
    suspend fun getInteractionCount(elementHash: String): Int

    /**
     * Get success/failure ratio for an element.
     * Used by CommandGenerator for reliability scoring.
     * Returns object with successful and failed counts.
     */
    suspend fun getSuccessFailureRatio(elementHash: String): SuccessRatio?
}

/**
 * Data class for success/failure ratio
 */
data class SuccessRatio(
    val successful: Int,
    val failed: Int
)
