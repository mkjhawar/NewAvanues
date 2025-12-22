/**
 * InstalledAppsManager.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Manages tracking and querying of installed applications on the device.
 * Provides app information and monitors app installation/uninstallation events.
 */
package com.augmentalis.voiceoscore.accessibility.managers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages installed applications tracking
 *
 * @param context Application context for PackageManager access
 */
class InstalledAppsManager(private val context: Context) {

    companion object {
        private const val TAG = "InstalledAppsManager"
    }

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

    /**
     * Get list of all installed applications
     *
     * @param includeSystemApps Whether to include system apps
     * @return List of installed app information
     */
    fun getInstalledApps(includeSystemApps: Boolean = false): List<AppInfo> {
        return try {
            val packageManager = context.packageManager
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            packages.mapNotNull { appInfo ->
                try {
                    // Filter system apps if requested
                    if (!includeSystemApps && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                        return@mapNotNull null
                    }

                    val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        versionName = packageInfo.versionName ?: "Unknown",
                        versionCode = packageInfo.longVersionCode,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting info for ${appInfo.packageName}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed apps", e)
            emptyList()
        }
    }

    /**
     * Observe app installation/uninstallation events
     *
     * @return Flow of app lists whenever apps change
     */
    fun observeAppInstalls(): Flow<List<AppInfo>> {
        // Refresh and emit current app list
        _installedApps.value = getInstalledApps()
        return _installedApps.asStateFlow()
    }

    /**
     * Get information about a specific app
     *
     * @param packageName Package name of the app
     * @return App information or null if not found
     */
    fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val packageInfo = packageManager.getPackageInfo(packageName, 0)

            AppInfo(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = packageInfo.longVersionCode,
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "App not found: $packageName")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app info for $packageName", e)
            null
        }
    }

    /**
     * Check if an app is installed
     *
     * @param packageName Package name to check
     * @return True if app is installed
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Refresh the cached app list
     */
    fun refresh() {
        _installedApps.value = getInstalledApps()
    }
}

/**
 * Information about an installed application
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean
)
