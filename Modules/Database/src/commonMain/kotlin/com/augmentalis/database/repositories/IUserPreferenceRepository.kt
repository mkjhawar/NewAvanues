// Author: Manoj Jhawar

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.UserPreferenceDTO

/**
 * Repository interface for user preferences.
 * Key-value storage for user settings.
 */
interface IUserPreferenceRepository {

    /**
     * Get preference value by key.
     */
    suspend fun getValue(key: String): String?

    /**
     * Get preference value with default.
     */
    suspend fun getValue(key: String, default: String): String

    /**
     * Set preference value (insert or update).
     */
    suspend fun setValue(key: String, value: String, type: String = "STRING")

    /**
     * Check if preference exists.
     */
    suspend fun exists(key: String): Boolean

    /**
     * Get all preferences.
     */
    suspend fun getAll(): List<UserPreferenceDTO>

    /**
     * Get preferences by type.
     */
    suspend fun getByType(type: String): List<UserPreferenceDTO>

    /**
     * Delete preference by key.
     */
    suspend fun delete(key: String)

    /**
     * Delete all preferences of a type.
     */
    suspend fun deleteByType(type: String)

    /**
     * Delete all preferences.
     */
    suspend fun deleteAll()

    /**
     * Count all preferences.
     */
    suspend fun count(): Long
}
