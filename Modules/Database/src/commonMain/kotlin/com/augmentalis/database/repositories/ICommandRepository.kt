// Author: Manoj Jhawar

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.CustomCommandDTO

/**
 * Repository interface for custom voice commands.
 * Abstracts database implementation (Room or SQLDelight).
 */
interface ICommandRepository {

    /**
     * Insert a new command.
     * @return The ID of the inserted command.
     */
    suspend fun insert(command: CustomCommandDTO): Long

    /**
     * Get command by ID.
     */
    suspend fun getById(id: Long): CustomCommandDTO?

    /**
     * Get all commands.
     */
    suspend fun getAll(): List<CustomCommandDTO>

    /**
     * Get all active commands.
     */
    suspend fun getActive(): List<CustomCommandDTO>

    /**
     * Get active commands for a specific language.
     */
    suspend fun getActiveByLanguage(language: String): List<CustomCommandDTO>

    /**
     * Get commands by language.
     */
    suspend fun getByLanguage(language: String): List<CustomCommandDTO>

    /**
     * Get most used commands.
     */
    suspend fun getMostUsed(limit: Int): List<CustomCommandDTO>

    /**
     * Search commands by name.
     */
    suspend fun searchByName(query: String): List<CustomCommandDTO>

    /**
     * Update an existing command.
     */
    suspend fun update(command: CustomCommandDTO)

    /**
     * Increment usage count for a command.
     */
    suspend fun incrementUsage(id: Long)

    /**
     * Set active status for a command.
     */
    suspend fun setActiveStatus(id: Long, isActive: Boolean)

    /**
     * Delete command by ID.
     */
    suspend fun delete(id: Long)

    /**
     * Delete all commands.
     */
    suspend fun deleteAll()

    /**
     * Count all commands.
     */
    suspend fun count(): Long

    /**
     * Count active commands.
     */
    suspend fun countActive(): Long
}
