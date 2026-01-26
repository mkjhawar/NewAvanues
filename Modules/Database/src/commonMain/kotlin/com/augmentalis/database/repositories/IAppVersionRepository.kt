/**
 * IAppVersionRepository.kt - Repository interface for app version tracking
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Interface for accessing and managing app version records.
 * Used for version-aware command lifecycle management.
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.AppVersionDTO

/**
 * Repository for app version tracking.
 *
 * ## Purpose:
 * Stores and retrieves the last known version for each app that VoiceOS
 * has generated commands for. Used to detect app updates/downgrades.
 *
 * ## Usage:
 * ```kotlin
 * // Store current version after scraping
 * repository.upsertAppVersion(
 *     packageName = "com.google.android.gm",
 *     version = AppVersion("8.2024.11.123", 82024)
 * )
 *
 * // Check for updates
 * val storedVersion = repository.getAppVersion("com.google.android.gm")
 * if (currentVersion.versionCode > storedVersion?.versionCode) {
 *     // App was updated - mark old commands for verification
 * }
 *
 * // Get all tracked apps
 * val allVersions = repository.getAllAppVersions()
 * ```
 *
 * ## Thread Safety:
 * All methods are suspend functions and use Dispatchers.Default internally.
 * Safe to call from any coroutine context.
 */
interface IAppVersionRepository {

    /**
     * Get stored version for an app.
     *
     * @param packageName App package identifier
     * @return AppVersionDTO if exists, null if app not tracked
     */
    suspend fun getAppVersion(packageName: String): AppVersionDTO?

    /**
     * Get all tracked app versions.
     *
     * Returns map of package name to version for all apps that VoiceOS
     * has generated commands for.
     *
     * @return Map of packageName to AppVersionDTO
     */
    suspend fun getAllAppVersions(): Map<String, AppVersionDTO>

    /**
     * Insert or update app version.
     *
     * Uses manual UPSERT logic to atomically insert new record or update existing one.
     *
     * Updates lastChecked timestamp to current time.
     *
     * @param packageName App package identifier
     * @param versionName Version name string
     * @param versionCode Version code number
     */
    suspend fun upsertAppVersion(packageName: String, versionName: String, versionCode: Long)

    /**
     * Update app version (if exists).
     *
     * Only updates if record already exists. Does nothing if not found.
     * Updates lastChecked timestamp to current time.
     *
     * @param packageName App package identifier
     * @param versionName Version name string
     * @param versionCode Version code number
     * @return true if updated, false if not found
     */
    suspend fun updateAppVersion(packageName: String, versionName: String, versionCode: Long): Boolean

    /**
     * Delete app version record.
     *
     * Removes the version tracking for an app.
     * Typically called when app is uninstalled.
     *
     * @param packageName App package identifier
     * @return true if deleted, false if not found
     */
    suspend fun deleteAppVersion(packageName: String): Boolean

    /**
     * Update last checked timestamp.
     *
     * Records that we checked this app's version at the current time.
     * Used for tracking staleness of version data.
     *
     * @param packageName App package identifier
     */
    suspend fun updateLastChecked(packageName: String)

    /**
     * Get apps that haven't been checked recently.
     *
     * Returns apps where last_checked is older than the given timestamp.
     * Useful for finding stale version data.
     *
     * @param olderThan Timestamp in epoch millis
     * @return Map of packageName to AppVersionDTO for stale apps
     */
    suspend fun getStaleAppVersions(olderThan: Long): Map<String, AppVersionDTO>

    /**
     * Get count of tracked apps.
     *
     * @return Number of apps with version records
     */
    suspend fun getCount(): Long

    /**
     * Delete all app version records.
     *
     * **WARNING**: This removes all version tracking data.
     * Only use for testing or database reset.
     */
    suspend fun deleteAll()
}
