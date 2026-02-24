/**
 * IElementStateHistoryRepository.kt - Repository interface for element state history
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.ElementStateHistoryDTO

/**
 * Repository interface for element state change history.
 * Tracks state changes of UI elements over time.
 */
interface IElementStateHistoryRepository {

    /**
     * Insert a new state change record.
     * @return The ID of the inserted state change.
     */
    suspend fun insert(stateChange: ElementStateHistoryDTO): Long

    /**
     * Get state change by ID.
     */
    suspend fun getById(id: Long): ElementStateHistoryDTO?

    /**
     * Get all state changes for an element.
     */
    suspend fun getByElement(elementHash: String): List<ElementStateHistoryDTO>

    /**
     * Get all state changes for a screen.
     */
    suspend fun getByScreen(screenHash: String): List<ElementStateHistoryDTO>

    /**
     * Get state changes by state type.
     */
    suspend fun getByStateType(stateType: String): List<ElementStateHistoryDTO>

    /**
     * Get state changes within a time range.
     */
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<ElementStateHistoryDTO>

    /**
     * Get state changes by trigger.
     */
    suspend fun getByTrigger(triggeredBy: String): List<ElementStateHistoryDTO>

    /**
     * Delete state change by ID.
     */
    suspend fun deleteById(id: Long)

    /**
     * Delete all state changes for an element.
     */
    suspend fun deleteByElement(elementHash: String)

    /**
     * Delete state changes older than timestamp.
     */
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Delete all state changes.
     */
    suspend fun deleteAll()

    /**
     * Count all state changes.
     */
    suspend fun count(): Long

    /**
     * Get current state for an element and state type.
     * Returns the most recent state change for the given element and state type.
     * Used by CommandGenerator for state-aware command generation.
     */
    suspend fun getCurrentState(elementHash: String, stateType: String): ElementStateHistoryDTO?
}
