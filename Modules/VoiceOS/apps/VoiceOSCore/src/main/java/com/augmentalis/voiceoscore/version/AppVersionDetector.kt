/**
 * AppVersionDetector.kt - Service for detecting app version changes
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Detects version changes for installed apps by comparing PackageManager
 * info with database records. Supports API 21-34 with compatibility handling.
 */

package com.augmentalis.voiceoscore.version

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.augmentalis.database.repositories.IAppVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Detects app version changes by comparing installed apps with database records.
 *
 * ## Overview:
 * Compares the currently installed version of an app (from PackageManager)
 * with the version stored in the database (from IAppVersionRepository).
 * Returns a VersionChange sealed class indicating what changed.
 *
 * ## Usage:
 * ```kotlin
 * val detector = AppVersionDetector(context, appVersionRepository)
 *
 * // Check single app
 * val change = detector.detectVersionChange("com.google.android.gm")
 * when (change) {
 *     is VersionChange.Updated -> {
 *         // Handle update - mark old commands for verification
 *         commandRepo.markVersionDeprecated(change.packageName, change.previous.versionCode)
 *     }
 *     is VersionChange.AppNotInstalled -> {
 *         // Clean up commands for uninstalled app
 *         commandRepo.deleteCommandsForApp(change.packageName)
 *     }
 *     // ... handle other cases
 * }
 *
 * // Check all apps with commands in database
 * val changes = detector.detectAllVersionChanges()
 * changes.filter { it.requiresVerification() }.forEach { change ->
 *     // Process updates and downgrades
 * }
 * ```
 *
 * ## API Compatibility:
 * - API 21-27: Uses deprecated `versionCode` (Int)
 * - API 28+: Uses `longVersionCode` (Long)
 * - Handles both seamlessly with @Suppress("DEPRECATION")
 *
 * @property context Android context for PackageManager access
 * @property appVersionRepository Repository for database version lookups
 */
class AppVersionDetector(
    private val context: Context,
    private val appVersionRepository: IAppVersionRepository
) {

    /**
     * Detects version change for a specific app package.
     *
     * Compares the installed version (from PackageManager) with the database version.
     * Returns appropriate VersionChange variant based on comparison.
     *
     * ## Behavior:
     * 1. **App not installed**: Returns `VersionChange.AppNotInstalled`
     * 2. **No database record**: Returns `VersionChange.FirstInstall`
     * 3. **Same version**: Returns `VersionChange.NoChange`
     * 4. **Newer version**: Returns `VersionChange.Updated`
     * 5. **Older version**: Returns `VersionChange.Downgraded`
     *
     * @param packageName App package name (e.g., "com.google.android.gm")
     * @return VersionChange indicating what changed
     */
    suspend fun detectVersionChange(packageName: String): VersionChange = withContext(Dispatchers.Default) {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }

        // Get installed version from PackageManager
        val installedVersion = getInstalledVersion(packageName)

        // If app not installed, return AppNotInstalled
        if (installedVersion == null) {
            return@withContext VersionChange.AppNotInstalled(packageName)
        }

        // Get database version
        val dbVersionDTO = appVersionRepository.getAppVersion(packageName)

        // If no database record, this is a first install
        if (dbVersionDTO == null) {
            return@withContext VersionChange.FirstInstall(
                packageName = packageName,
                current = installedVersion
            )
        }

        // Convert DTO to AppVersion
        val dbVersion = AppVersion(
            versionName = dbVersionDTO.versionName,
            versionCode = dbVersionDTO.versionCode
        )

        // Compare versions
        when {
            installedVersion.versionCode == dbVersion.versionCode -> {
                // Same version
                VersionChange.NoChange(
                    packageName = packageName,
                    version = installedVersion
                )
            }
            installedVersion.versionCode > dbVersion.versionCode -> {
                // App was updated
                VersionChange.Updated(
                    packageName = packageName,
                    previous = dbVersion,
                    current = installedVersion
                )
            }
            else -> {
                // App was downgraded (rare but possible)
                VersionChange.Downgraded(
                    packageName = packageName,
                    previous = dbVersion,
                    current = installedVersion
                )
            }
        }
    }

    /**
     * Detects version changes for all apps tracked in the database.
     *
     * Efficiently checks all apps with commands in the database to detect
     * updates, downgrades, or uninstalls.
     *
     * ## Use Cases:
     * - Periodic background job to detect app updates
     * - VoiceOS service startup to sync app states
     * - Manual "check for updates" user action
     *
     * ## Performance:
     * - Queries all tracked apps from database (1 query)
     * - Checks each app's installed version (N PackageManager calls)
     * - Total: O(N) where N = number of tracked apps
     * - Typical: 20-50 apps, <100ms total time
     *
     * @return List of VersionChange for all tracked apps
     */
    suspend fun detectAllVersionChanges(): List<VersionChange> = withContext(Dispatchers.Default) {
        // Get all package names from database
        val trackedApps = appVersionRepository.getAllAppVersions()

        // Check each app for version changes
        trackedApps.map { (packageName, _) ->
            detectVersionChange(packageName)
        }
    }

    /**
     * Gets the currently installed version for a package.
     *
     * Uses PackageManager to query installed app version.
     * Handles API compatibility for versionCode (deprecated in API 28).
     *
     * ## API Compatibility:
     * - API 21-27: Uses `packageInfo.versionCode` (Int, deprecated)
     * - API 28+: Uses `packageInfo.longVersionCode` (Long)
     *
     * @param packageName App package name
     * @return AppVersion if installed, null if not installed
     */
    private fun getInstalledVersion(packageName: String): AppVersion? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)

            AppVersion(
                versionName = packageInfo.versionName ?: "UNKNOWN",
                versionCode = getVersionCode(packageInfo)
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // App not installed
            null
        }
    }

    /**
     * Extracts version code from PackageInfo with API compatibility.
     *
     * Handles the deprecation of `versionCode` in API 28+.
     *
     * @param packageInfo PackageInfo from PackageManager
     * @return Version code as Long
     */
    @Suppress("DEPRECATION")
    private fun getVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode  // API 28+
        } else {
            packageInfo.versionCode.toLong()  // API < 28
        }
    }

    /**
     * Checks if an app is currently installed.
     *
     * Convenience method to check installation status without version details.
     *
     * @param packageName App package name
     * @return true if app is installed, false otherwise
     */
    suspend fun isAppInstalled(packageName: String): Boolean = withContext(Dispatchers.Default) {
        getInstalledVersion(packageName) != null
    }

    /**
     * Gets installed version for multiple packages in batch.
     *
     * More efficient than calling getInstalledVersion() multiple times
     * as it minimizes context switching.
     *
     * @param packageNames List of package names to check
     * @return Map of packageName to AppVersion (null if not installed)
     */
    suspend fun getInstalledVersions(packageNames: List<String>): Map<String, AppVersion?> =
        withContext(Dispatchers.Default) {
            packageNames.associateWith { packageName ->
                getInstalledVersion(packageName)
            }
        }
}
