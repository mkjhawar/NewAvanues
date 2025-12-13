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
     * Insert multiple generated commands in a single transaction.
     * Significantly faster than sequential inserts for large batches.
     *
     * Performance: ~50ms for 100 commands vs ~1000ms sequential
     *
     * @param commands List of commands to insert
     */
    suspend fun insertBatch(commands: List<GeneratedCommandDTO>)

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
     * Get all commands (alias for getAll for compatibility).
     */
    suspend fun getAllCommands(): List<GeneratedCommandDTO>

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

    /**
     * Get all commands for a specific package.
     * @param packageName App package name (appId)
     * @return List of commands for the package
     */
    suspend fun getByPackage(packageName: String): List<GeneratedCommandDTO>

    /**
     * Update an existing command.
     * Used for updating synonyms and other command properties.
     * @param command Command to update
     */
    suspend fun update(command: GeneratedCommandDTO)

    /**
     * Get all commands with pagination support.
     *
     * Prevents memory issues when retrieving large datasets.
     *
     * @param limit Maximum number of commands to return
     * @param offset Number of commands to skip
     * @return List of commands (up to limit)
     */
    suspend fun getAllPaginated(limit: Int, offset: Int): List<GeneratedCommandDTO>

    /**
     * Get commands by package with pagination support.
     *
     * @param packageName App package name
     * @param limit Maximum number of commands to return
     * @param offset Number of commands to skip
     * @return List of commands (up to limit)
     */
    suspend fun getByPackagePaginated(packageName: String, limit: Int, offset: Int): List<GeneratedCommandDTO>

    /**
     * Get commands by package using keyset pagination (cursor-based).
     *
     * More efficient than offset-based pagination for large datasets.
     * Uses the last ID from previous page as cursor.
     *
     * @param packageName App package name
     * @param lastId ID of last command from previous page (0 for first page)
     * @param limit Maximum number of commands to return
     * @return List of commands (up to limit)
     */
    suspend fun getByPackageKeysetPaginated(packageName: String, lastId: Long, limit: Int): List<GeneratedCommandDTO>

    /**
     * Get commands by action type with pagination support.
     *
     * @param actionType Action type filter
     * @param limit Maximum number of commands to return
     * @param offset Number of commands to skip
     * @return List of commands (up to limit)
     */
    suspend fun getByActionTypePaginated(actionType: String, limit: Int, offset: Int): List<GeneratedCommandDTO>
}
