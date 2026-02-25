/**
 * IScreenTransitionRepository.kt - Repository interface for screen transitions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.ScreenTransitionDTO

/**
 * Repository interface for screen navigation transitions.
 * Tracks user navigation patterns between screens.
 */
interface IScreenTransitionRepository {

    /**
     * Insert a new transition record.
     * @return The ID of the inserted transition.
     */
    suspend fun insert(transition: ScreenTransitionDTO): Long

    /**
     * Get transition by ID.
     */
    suspend fun getById(id: Long): ScreenTransitionDTO?

    /**
     * Get all transitions from a screen.
     */
    suspend fun getFromScreen(fromScreenHash: String): List<ScreenTransitionDTO>

    /**
     * Get all transitions to a screen.
     */
    suspend fun getToScreen(toScreenHash: String): List<ScreenTransitionDTO>

    /**
     * Get transitions triggered by an element.
     */
    suspend fun getByTrigger(triggerElementHash: String): List<ScreenTransitionDTO>

    /**
     * Get most frequent transitions.
     */
    suspend fun getFrequent(limit: Long): List<ScreenTransitionDTO>

    /**
     * Record a transition (increment count, update average duration).
     */
    suspend fun recordTransition(
        fromScreenHash: String,
        toScreenHash: String,
        durationMs: Long,
        timestamp: Long
    )

    /**
     * Delete transition by ID.
     */
    suspend fun deleteById(id: Long)

    /**
     * Delete all transitions for a screen.
     */
    suspend fun deleteByScreen(screenHash: String)

    /**
     * Delete all transitions.
     */
    suspend fun deleteAll()

    /**
     * Count all transitions.
     */
    suspend fun count(): Long
}
