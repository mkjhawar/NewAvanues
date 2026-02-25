// Author: Manoj Jhawar

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.VoiceCommandDTO

/**
 * Repository interface for voice commands with locale support.
 * Abstracts database implementation (SQLDelight).
 */
interface IVoiceCommandRepository {

    /**
     * Insert a new voice command.
     * @return The ID of the inserted command.
     */
    suspend fun insert(command: VoiceCommandDTO): Long

    /**
     * Get command by database ID.
     */
    suspend fun getById(id: Long): VoiceCommandDTO?

    /**
     * Get all commands for a specific command ID (across all locales).
     */
    suspend fun getByCommandId(commandId: String): List<VoiceCommandDTO>

    /**
     * Get all commands for a specific locale.
     */
    suspend fun getByLocale(locale: String): List<VoiceCommandDTO>

    /**
     * Get commands by locale with English fallback.
     * Returns commands in the requested locale first, then en-US fallback.
     */
    suspend fun getByLocaleWithFallback(locale: String): List<VoiceCommandDTO>

    /**
     * Get all commands in a category.
     */
    suspend fun getByCategory(category: String): List<VoiceCommandDTO>

    /**
     * Get all enabled commands.
     */
    suspend fun getEnabled(): List<VoiceCommandDTO>

    /**
     * Search commands by trigger phrase.
     */
    suspend fun searchByTrigger(query: String): List<VoiceCommandDTO>

    /**
     * Get all commands.
     */
    suspend fun getAll(): List<VoiceCommandDTO>

    /**
     * Update an existing command.
     */
    suspend fun update(command: VoiceCommandDTO)

    /**
     * Update enabled state for a command.
     */
    suspend fun updateEnabledState(id: Long, isEnabled: Boolean)

    /**
     * Delete command by database ID.
     */
    suspend fun delete(id: Long)

    /**
     * Delete all commands by command ID (all locales).
     */
    suspend fun deleteByCommandId(commandId: String)

    /**
     * Count all commands.
     */
    suspend fun count(): Long

    /**
     * Count commands by locale.
     */
    suspend fun countByLocale(locale: String): Long
}
