package com.augmentalis.ava.core.domain.repository

import com.augmentalis.ava.core.domain.model.AppPreference
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user's app preferences.
 *
 * Stores which app the user prefers for each capability (email, SMS, music, etc.).
 * Part of Intelligent Resolution System (Chapter 71).
 *
 * Author: Manoj Jhawar
 */
interface AppPreferencesRepository {

    /**
     * Get the preferred app package name for a capability.
     *
     * @param capability The capability ID (e.g., "email", "sms")
     * @return The package name of the preferred app, or null if no preference set
     */
    suspend fun getPreferredApp(capability: String): AppPreference?

    /**
     * Set the preferred app for a capability.
     *
     * @param capability The capability ID
     * @param packageName The package name of the app
     * @param appName The display name of the app
     * @param setBy How this preference was set ("user", "auto", "usage")
     */
    suspend fun setPreferredApp(
        capability: String,
        packageName: String,
        appName: String,
        setBy: String = "user"
    )

    /**
     * Clear the preference for a capability.
     *
     * @param capability The capability ID
     */
    suspend fun clearPreferredApp(capability: String)

    /**
     * Get all stored preferences.
     *
     * @return Map of capability ID to AppPreference
     */
    suspend fun getAllPreferences(): Map<String, AppPreference>

    /**
     * Check if a preference exists for a capability.
     *
     * @param capability The capability ID
     * @return True if a preference is set
     */
    suspend fun hasPreference(capability: String): Boolean

    /**
     * Observe all preferences as a Flow.
     *
     * @return Flow emitting list of all preferences when any changes
     */
    fun observeAllPreferences(): Flow<List<AppPreference>>

    /**
     * Record app usage for learning.
     *
     * @param capability The capability used
     * @param packageName The app package that was used
     * @param contextJson Optional JSON with context (time, location, etc.)
     */
    suspend fun recordUsage(
        capability: String,
        packageName: String,
        contextJson: String? = null
    )

    /**
     * Get the most used app for a capability (from usage patterns).
     *
     * @param capability The capability ID
     * @return The most frequently used package name, or null
     */
    suspend fun getMostUsedApp(capability: String): String?
}
