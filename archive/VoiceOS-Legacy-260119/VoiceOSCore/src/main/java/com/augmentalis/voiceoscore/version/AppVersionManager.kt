/**
 * AppVersionManager.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Manages app version lifecycle and command invalidation.
 * Coordinates between AppVersionDetector and repositories for version-aware command management.
 */
package com.augmentalis.voiceoscore.version

import android.content.Context
import android.util.Log
import com.augmentalis.database.repositories.IAppVersionRepository
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager for app version lifecycle and command invalidation
 *
 * Responsibilities:
 * - Track app version changes using AppVersionDetector
 * - Invalidate commands when app versions change
 * - Coordinate version cleanup operations
 *
 * @param context Application context
 * @param detector AppVersionDetector for version change detection
 * @param versionRepo Repository for version storage
 * @param commandRepo Repository for command management
 */
class AppVersionManager(
    private val context: Context,
    private val detector: AppVersionDetector,
    private val versionRepo: IAppVersionRepository,
    private val commandRepo: IGeneratedCommandRepository
) {

    companion object {
        private const val TAG = "AppVersionManager"
    }

    /**
     * Check app version and invalidate commands if version changed
     *
     * @param packageName Package name of the app to check
     * @return True if version changed and commands were invalidated
     */
    suspend fun checkAndInvalidateIfChanged(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            when (val change = detector.detectVersionChange(packageName)) {
                is VersionChange.Upgraded -> {
                    Log.i(TAG, "App upgraded: $packageName ${change.oldVersion.versionName} -> ${change.newVersion.versionName}")
                    invalidateCommandsForApp(packageName, change.oldVersion.versionCode)
                    true
                }
                is VersionChange.Downgraded -> {
                    Log.i(TAG, "App downgraded: $packageName ${change.oldVersion.versionName} -> ${change.newVersion.versionName}")
                    invalidateCommandsForApp(packageName, change.oldVersion.versionCode)
                    true
                }
                is VersionChange.NewApp -> {
                    Log.i(TAG, "New app detected: $packageName ${change.version.versionName}")
                    false // No commands to invalidate for new app
                }
                is VersionChange.NoChange -> {
                    false // No action needed
                }
                is VersionChange.AppNotInstalled -> {
                    Log.w(TAG, "App not installed: $packageName")
                    false
                }
                is VersionChange.Error -> {
                    Log.e(TAG, "Error detecting version change for $packageName: ${change.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking version for $packageName", e)
            false
        }
    }

    /**
     * Invalidate all commands for a specific app version
     *
     * @param packageName Package name of the app
     * @param versionCode Old version code to mark as deprecated
     */
    private suspend fun invalidateCommandsForApp(packageName: String, versionCode: Long) {
        try {
            // Mark all commands for this app version as deprecated
            val invalidatedCount = commandRepo.markVersionDeprecated(packageName, versionCode)
            Log.i(TAG, "Invalidated $invalidatedCount commands for $packageName version $versionCode")
        } catch (e: Exception) {
            Log.e(TAG, "Error invalidating commands for $packageName", e)
        }
    }

    /**
     * Get version info for an app
     *
     * @param packageName Package name of the app
     * @return AppVersion or null if not installed
     */
    suspend fun getVersionInfo(packageName: String): AppVersion? {
        return detector.getVersion(packageName)
    }

    /**
     * Force refresh version data for an app
     *
     * @param packageName Package name of the app
     */
    suspend fun refreshVersion(packageName: String) {
        try {
            val version = detector.getVersion(packageName)
            if (version != null) {
                versionRepo.upsertAppVersion(
                    packageName = packageName,
                    versionName = version.versionName,
                    versionCode = version.versionCode
                )
                Log.d(TAG, "Refreshed version data for $packageName: ${version.versionName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing version for $packageName", e)
        }
    }
}
