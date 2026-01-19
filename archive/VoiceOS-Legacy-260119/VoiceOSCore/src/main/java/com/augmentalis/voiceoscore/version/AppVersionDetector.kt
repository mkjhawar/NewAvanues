/**
 * AppVersionDetector.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Detects app version changes and manages version-aware command lifecycle.
 * Integrates with IAppVersionRepository for persistent version tracking.
 */
package com.augmentalis.voiceoscore.version

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.augmentalis.database.repositories.IAppVersionRepository
import com.augmentalis.database.dto.AppVersionDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Detects and tracks app version changes
 *
 * @param context Application context for PackageManager access
 * @param repository Repository for storing version data
 */
class AppVersionDetector(
    private val context: Context,
    private val repository: IAppVersionRepository
) {

    companion object {
        private const val TAG = "AppVersionDetector"
    }

    /**
     * Get current version of an installed app
     *
     * @param packageName Package name of the app
     * @return AppVersion or null if app not installed
     */
    suspend fun getVersion(packageName: String): AppVersion? = withContext(Dispatchers.IO) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            AppVersion(
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = packageInfo.longVersionCode
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "App not found: $packageName")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version for $packageName", e)
            null
        }
    }

    /**
     * Detect if app version has changed since last check
     *
     * @param packageName Package name of the app
     * @return VersionChange indicating what happened
     */
    suspend fun detectVersionChange(packageName: String): VersionChange = withContext(Dispatchers.IO) {
        try {
            // Get current installed version
            val currentVersion = getVersion(packageName)
            if (currentVersion == null) {
                return@withContext VersionChange.AppNotInstalled(packageName)
            }

            // Get stored version from database
            val storedVersion = repository.getAppVersion(packageName)
            if (storedVersion == null) {
                // First time seeing this app
                repository.upsertAppVersion(
                    packageName = packageName,
                    versionName = currentVersion.versionName,
                    versionCode = currentVersion.versionCode
                )
                return@withContext VersionChange.NewApp(packageName, currentVersion)
            }

            // Compare versions
            when {
                currentVersion.versionCode > storedVersion.versionCode -> {
                    // App was upgraded
                    repository.upsertAppVersion(
                        packageName = packageName,
                        versionName = currentVersion.versionName,
                        versionCode = currentVersion.versionCode
                    )
                    VersionChange.Upgraded(
                        packageName = packageName,
                        oldVersion = AppVersion(storedVersion.versionName, storedVersion.versionCode),
                        newVersion = currentVersion
                    )
                }
                currentVersion.versionCode < storedVersion.versionCode -> {
                    // App was downgraded
                    repository.upsertAppVersion(
                        packageName = packageName,
                        versionName = currentVersion.versionName,
                        versionCode = currentVersion.versionCode
                    )
                    VersionChange.Downgraded(
                        packageName = packageName,
                        oldVersion = AppVersion(storedVersion.versionName, storedVersion.versionCode),
                        newVersion = currentVersion
                    )
                }
                else -> {
                    // Same version - just update last checked time
                    repository.updateLastChecked(packageName)
                    VersionChange.NoChange(packageName, currentVersion)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting version change for $packageName", e)
            VersionChange.Error(packageName, e.message ?: "Unknown error")
        }
    }

    /**
     * Compare two versions
     *
     * @param version1 First version to compare
     * @param version2 Second version to compare
     * @return Negative if version1 < version2, 0 if equal, positive if version1 > version2
     */
    fun compareVersions(version1: AppVersion, version2: AppVersion): Int {
        return version1.versionCode.compareTo(version2.versionCode)
    }

    /**
     * Check all apps in repository for version changes
     *
     * @return List of version changes detected
     */
    suspend fun checkAllTrackedApps(): List<VersionChange> = withContext(Dispatchers.IO) {
        val allVersions = repository.getAllAppVersions()
        allVersions.keys.map { packageName ->
            detectVersionChange(packageName)
        }
    }

    /**
     * Get apps that haven't been checked recently
     *
     * @param maxAgeMillis Maximum age before considering stale
     * @return Map of package names to stored versions for stale apps
     */
    suspend fun getStaleApps(maxAgeMillis: Long): Map<String, AppVersionDTO> = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - maxAgeMillis
        repository.getStaleAppVersions(cutoffTime)
    }
}

/**
 * Represents an app version
 */
data class AppVersion(
    val versionName: String,
    val versionCode: Long
)

/**
 * Result of version change detection
 */
sealed class VersionChange {
    /**
     * App was not found on device
     */
    data class AppNotInstalled(val packageName: String) : VersionChange()

    /**
     * First time tracking this app
     */
    data class NewApp(val packageName: String, val version: AppVersion) : VersionChange()

    /**
     * App version increased
     */
    data class Upgraded(
        val packageName: String,
        val oldVersion: AppVersion,
        val newVersion: AppVersion
    ) : VersionChange()

    /**
     * App version decreased
     */
    data class Downgraded(
        val packageName: String,
        val oldVersion: AppVersion,
        val newVersion: AppVersion
    ) : VersionChange()

    /**
     * App version unchanged
     */
    data class NoChange(val packageName: String, val version: AppVersion) : VersionChange()

    /**
     * Error during version check
     */
    data class Error(val packageName: String, val message: String) : VersionChange()
}
