// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.avanues.database.repositories

import com.avanues.database.dto.ContextPreferenceDTO

/**
 * Repository interface for context preferences.
 * Tracks command preferences per context for adaptive learning.
 * Abstracts database implementation (SQLDelight).
 */
interface IContextPreferenceRepository {

    /**
     * Insert a new preference.
     * @return The ID of the inserted preference.
     */
    suspend fun insert(preference: ContextPreferenceDTO): Long

    /**
     * Get all preferences.
     */
    suspend fun getAll(): List<ContextPreferenceDTO>

    /**
     * Get preference for a specific command and context.
     */
    suspend fun get(commandId: String, contextKey: String): ContextPreferenceDTO?

    /**
     * Get all preferences for a command.
     */
    suspend fun getForCommand(commandId: String): List<ContextPreferenceDTO>

    /**
     * Get most used commands (ranked by total usage count).
     */
    suspend fun getMostUsedCommands(limit: Long): List<Pair<String, Long>>

    /**
     * Get most used contexts (ranked by total usage count).
     */
    suspend fun getMostUsedContexts(limit: Long): List<Pair<String, Long>>

    /**
     * Get recent preferences.
     */
    suspend fun getRecent(limit: Long): List<ContextPreferenceDTO>

    /**
     * Count distinct commands being tracked.
     */
    suspend fun countCommands(): Long

    /**
     * Count distinct contexts being tracked.
     */
    suspend fun countContexts(): Long

    /**
     * Get average success rate across all commands.
     */
    suspend fun getAverageSuccessRate(): Double

    /**
     * Update an existing preference.
     */
    suspend fun update(preference: ContextPreferenceDTO)

    /**
     * Increment usage counts for a command/context pair.
     */
    suspend fun incrementCounts(commandId: String, contextKey: String, successIncrement: Long, timestamp: Long)

    /**
     * Delete a preference.
     */
    suspend fun delete(id: Long)

    /**
     * Delete all preferences.
     */
    suspend fun deleteAll()

    /**
     * Apply time decay to preference counts.
     * Reduces the weight of older preference records.
     */
    suspend fun applyTimeDecay(currentTime: Long, decayFactor: Float)
}
