/**
 * IGeneratedCommandRepository.kt - Repository interface for generated voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.GeneratedCommandDTO

/**
 * Repository interface for generated voice commands.
 * Provides CRUD operations for command data.
 */
interface IGeneratedCommandRepository {

    /**
     * Insert a new generated command.
     * @return The ID of the inserted command.
     */
    suspend fun insert(command: GeneratedCommandDTO): Long

    /**
     * Get command by ID.
     */
    suspend fun getById(id: Long): GeneratedCommandDTO?

    /**
     * Get all commands for an element.
     */
    suspend fun getByElement(elementHash: String): List<GeneratedCommandDTO>

    /**
     * Get all commands.
     */
    suspend fun getAll(): List<GeneratedCommandDTO>

    /**
     * Get commands by action type.
     */
    suspend fun getByActionType(actionType: String): List<GeneratedCommandDTO>

    /**
     * Get high-confidence commands (above threshold).
     */
    suspend fun getHighConfidence(minConfidence: Double): List<GeneratedCommandDTO>

    /**
     * Get user-approved commands.
     */
    suspend fun getUserApproved(): List<GeneratedCommandDTO>

    /**
     * Fuzzy search for commands by text.
     */
    suspend fun fuzzySearch(searchText: String): List<GeneratedCommandDTO>

    /**
     * Increment usage count and update last used timestamp.
     */
    suspend fun incrementUsage(id: Long, timestamp: Long)

    /**
     * Mark command as user-approved.
     */
    suspend fun markApproved(id: Long)

    /**
     * Update command confidence.
     */
    suspend fun updateConfidence(id: Long, confidence: Double)

    /**
     * Delete command by ID.
     */
    suspend fun deleteById(id: Long)

    /**
     * Delete all commands for an element.
     */
    suspend fun deleteByElement(elementHash: String)

    /**
     * Delete low-quality commands (unused, low confidence).
     */
    suspend fun deleteLowQuality(minConfidence: Double)

    /**
     * Delete all commands.
     */
    suspend fun deleteAll()

    /**
     * Count all commands.
     */
    suspend fun count(): Long
}
