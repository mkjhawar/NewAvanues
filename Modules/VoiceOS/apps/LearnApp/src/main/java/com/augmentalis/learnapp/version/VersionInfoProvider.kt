/**
 * VersionInfoProvider.kt - Provides app version information and update detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-09
 *
 * Extracts and monitors application version information from PackageManager.
 * Tracks version changes and provides compatibility checking for API levels.
 */
package com.augmentalis.learnapp.version

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Comprehensive version information for an application
 */
data class AppVersionInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean
) {
    /**
     * Check if this version is newer than another version
     */
    fun isNewerThan(other: AppVersionInfo): Boolean {
        return this.versionCode > other.versionCode
    }

    /**
     * Check if app was recently updated (within last N milliseconds)
     */
    fun wasRecentlyUpdated(withinMillis: Long = 24 * 60 * 60 * 1000): Boolean {
        return (System.currentTimeMillis() - lastUpdateTime) < withinMillis
    }

    /**
     * Check if app is compatible with current device API level
     */
    fun isCompatibleWithDevice(): Boolean {
        return Build.VERSION.SDK_INT >= minSdkVersion
    }

    /**
     * Get age of current installation in days
     */
    fun getInstallationAgeDays(): Long {
        return (System.currentTimeMillis() - firstInstallTime) / (24 * 60 * 60 * 1000)
    }
}

/**
 * Result of version info retrieval operation
 */
sealed class VersionResult {
    data class Success(val versionInfo: AppVersionInfo) : VersionResult()
    data class Error(val message: String, val exception: Exception? = null) : VersionResult()
}

/**
 * Result of version comparison operation
 */
sealed class UpdateStatus {
    object NoUpdate : UpdateStatus()
    data class Updated(val oldVersion: AppVersionInfo, val newVersion: AppVersionInfo) : UpdateStatus()
    data class Downgraded(val oldVersion: AppVersionInfo, val newVersion: AppVersionInfo) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

/**
 * Provides version information and update tracking for installed applications
 *
 * This class monitors app versions and can detect when applications are updated.
 * It maintains a cache of known versions for comparison.
 *
 * @param context Android context for PackageManager access
 */
class VersionInfoProvider(private val context: Context) {

    companion object {
        private const val TAG = "VersionInfoProvider"
        private const val PREFS_NAME = "learnapp_versions"
        private const val PREFS_VERSION_PREFIX = "version_"
    }

    private val packageManager: PackageManager = context.packageManager
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Cache of currently known versions
    private val versionCache = mutableMapOf<String, AppVersionInfo>()

    // Flow for observing version changes
    private val _versionUpdates = MutableStateFlow<Map<String, AppVersionInfo>>(emptyMap())
    val versionUpdates: StateFlow<Map<String, AppVersionInfo>> = _versionUpdates.asStateFlow()

    /**
     * Get version information for a specific package
     *
     * @param packageName The package name to query
     * @return VersionResult containing version info or error
     */
    fun getVersionInfo(packageName: String): VersionResult {
        if (packageName.isBlank()) {
            return VersionResult.Error("Package name cannot be blank")
        }

        return try {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_META_DATA
            )

            val versionInfo = packageInfoToVersionInfo(packageInfo)

            // Update cache
            versionCache[packageName] = versionInfo

            Log.d(TAG, "Retrieved version info for $packageName: v${versionInfo.versionName} (${versionInfo.versionCode})")

            VersionResult.Success(versionInfo)

        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found: $packageName", e)
            VersionResult.Error("Package not found: $packageName", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version info for $packageName", e)
            VersionResult.Error("Failed to get version info: ${e.message}", e)
        }
    }

    /**
     * Check if an app has been updated since last check
     *
     * Compares current version with cached version from previous check.
     *
     * @param packageName The package name to check
     * @return UpdateStatus indicating whether app was updated, downgraded, or unchanged
     */
    fun checkForUpdate(packageName: String): UpdateStatus {
        val currentResult = getVersionInfo(packageName)
        if (currentResult is VersionResult.Error) {
            return UpdateStatus.Error(currentResult.message)
        }

        val currentVersion = (currentResult as VersionResult.Success).versionInfo
        val cachedVersion = getCachedVersion(packageName)

        return when {
            cachedVersion == null -> {
                // First time seeing this app
                cacheVersion(packageName, currentVersion)
                UpdateStatus.NoUpdate
            }
            currentVersion.versionCode > cachedVersion.versionCode -> {
                Log.i(TAG, "$packageName updated: ${cachedVersion.versionName} -> ${currentVersion.versionName}")
                cacheVersion(packageName, currentVersion)
                UpdateStatus.Updated(cachedVersion, currentVersion)
            }
            currentVersion.versionCode < cachedVersion.versionCode -> {
                Log.w(TAG, "$packageName downgraded: ${cachedVersion.versionName} -> ${currentVersion.versionName}")
                cacheVersion(packageName, currentVersion)
                UpdateStatus.Downgraded(cachedVersion, currentVersion)
            }
            else -> {
                UpdateStatus.NoUpdate
            }
        }
    }

    /**
     * Check if app meets minimum SDK requirements
     *
     * @param packageName The package name to check
     * @param requiredMinSdk Minimum SDK version required
     * @return true if app can run on devices with requiredMinSdk or false if incompatible
     */
    fun checkCompatibility(packageName: String, requiredMinSdk: Int): Boolean {
        val result = getVersionInfo(packageName)
        if (result is VersionResult.Error) {
            Log.e(TAG, "Cannot check compatibility: ${result.message}")
            return false
        }

        val versionInfo = (result as VersionResult.Success).versionInfo
        return versionInfo.minSdkVersion <= requiredMinSdk
    }

    /**
     * Check if app is compatible with current device
     *
     * @param packageName The package name to check
     * @return true if app can run on current device
     */
    fun isCompatibleWithDevice(packageName: String): Boolean {
        return checkCompatibility(packageName, Build.VERSION.SDK_INT)
    }

    /**
     * Get all cached version information
     *
     * @return Map of package names to their version info
     */
    fun getAllCachedVersions(): Map<String, AppVersionInfo> {
        return versionCache.toMap()
    }

    /**
     * Clear cached version for a specific package
     *
     * Useful when you want to force a fresh version check
     *
     * @param packageName The package name to clear
     */
    fun clearCache(packageName: String) {
        versionCache.remove(packageName)
        sharedPreferences.edit().remove("$PREFS_VERSION_PREFIX$packageName").apply()
        Log.d(TAG, "Cleared cache for $packageName")
    }

    /**
     * Clear all cached versions
     */
    fun clearAllCache() {
        versionCache.clear()
        sharedPreferences.edit().clear().apply()
        Log.d(TAG, "Cleared all version cache")
    }

    /**
     * Scan all installed apps and detect any updates
     *
     * @return Map of package names to their update status
     */
    fun scanForUpdates(): Map<String, UpdateStatus> {
        val updateStatuses = mutableMapOf<String, UpdateStatus>()

        try {
            val installedPackages = packageManager.getInstalledPackages(0)

            installedPackages.forEach { packageInfo ->
                val packageName = packageInfo.packageName
                val updateStatus = checkForUpdate(packageName)

                if (updateStatus !is UpdateStatus.NoUpdate) {
                    updateStatuses[packageName] = updateStatus
                }
            }

            Log.d(TAG, "Scan complete: Found ${updateStatuses.size} updates")

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for updates", e)
        }

        return updateStatuses
    }

    /**
     * Get list of recently updated apps
     *
     * @param withinMillis Time window to consider (default: 24 hours)
     * @return List of package names that were recently updated
     */
    fun getRecentlyUpdatedApps(withinMillis: Long = 24 * 60 * 60 * 1000): List<String> {
        val recentlyUpdated = mutableListOf<String>()

        try {
            val installedPackages = packageManager.getInstalledPackages(0)

            installedPackages.forEach { packageInfo ->
                val versionInfo = packageInfoToVersionInfo(packageInfo)
                if (versionInfo.wasRecentlyUpdated(withinMillis)) {
                    recentlyUpdated.add(versionInfo.packageName)
                }
            }

            Log.d(TAG, "Found ${recentlyUpdated.size} recently updated apps")

        } catch (e: Exception) {
            Log.e(TAG, "Error finding recently updated apps", e)
        }

        return recentlyUpdated
    }

    /**
     * Convert PackageInfo to AppVersionInfo
     */
    @Suppress("DEPRECATION")
    private fun packageInfoToVersionInfo(packageInfo: PackageInfo): AppVersionInfo {
        val appInfo = packageInfo.applicationInfo

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }

        val minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appInfo?.minSdkVersion ?: 1
        } else {
            // Pre-Android N, default to 1
            1
        }

        return AppVersionInfo(
            packageName = packageInfo.packageName,
            versionName = packageInfo.versionName ?: "unknown",
            versionCode = versionCode,
            targetSdkVersion = appInfo?.targetSdkVersion ?: 0,
            minSdkVersion = minSdkVersion,
            firstInstallTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            isSystemApp = appInfo?.let { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false,
            isUpdatedSystemApp = appInfo?.let { (it.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 } ?: false
        )
    }

    /**
     * Get cached version from SharedPreferences
     */
    private fun getCachedVersion(packageName: String): AppVersionInfo? {
        // First check in-memory cache
        versionCache[packageName]?.let { return it }

        // Then check persistent storage
        val versionCode = sharedPreferences.getLong("${PREFS_VERSION_PREFIX}${packageName}_code", -1L)
        if (versionCode == -1L) return null

        val versionName = sharedPreferences.getString("${PREFS_VERSION_PREFIX}${packageName}_name", null) ?: return null
        val targetSdk = sharedPreferences.getInt("${PREFS_VERSION_PREFIX}${packageName}_target", 0)
        val minSdk = sharedPreferences.getInt("${PREFS_VERSION_PREFIX}${packageName}_min", 0)
        val firstInstall = sharedPreferences.getLong("${PREFS_VERSION_PREFIX}${packageName}_first", 0L)
        val lastUpdate = sharedPreferences.getLong("${PREFS_VERSION_PREFIX}${packageName}_last", 0L)
        val isSystem = sharedPreferences.getBoolean("${PREFS_VERSION_PREFIX}${packageName}_system", false)
        val isUpdatedSystem = sharedPreferences.getBoolean("${PREFS_VERSION_PREFIX}${packageName}_updated_system", false)

        val versionInfo = AppVersionInfo(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            targetSdkVersion = targetSdk,
            minSdkVersion = minSdk,
            firstInstallTime = firstInstall,
            lastUpdateTime = lastUpdate,
            isSystemApp = isSystem,
            isUpdatedSystemApp = isUpdatedSystem
        )

        versionCache[packageName] = versionInfo
        return versionInfo
    }

    /**
     * Cache version to SharedPreferences
     */
    private fun cacheVersion(packageName: String, versionInfo: AppVersionInfo) {
        versionCache[packageName] = versionInfo

        sharedPreferences.edit().apply {
            putLong("${PREFS_VERSION_PREFIX}${packageName}_code", versionInfo.versionCode)
            putString("${PREFS_VERSION_PREFIX}${packageName}_name", versionInfo.versionName)
            putInt("${PREFS_VERSION_PREFIX}${packageName}_target", versionInfo.targetSdkVersion)
            putInt("${PREFS_VERSION_PREFIX}${packageName}_min", versionInfo.minSdkVersion)
            putLong("${PREFS_VERSION_PREFIX}${packageName}_first", versionInfo.firstInstallTime)
            putLong("${PREFS_VERSION_PREFIX}${packageName}_last", versionInfo.lastUpdateTime)
            putBoolean("${PREFS_VERSION_PREFIX}${packageName}_system", versionInfo.isSystemApp)
            putBoolean("${PREFS_VERSION_PREFIX}${packageName}_updated_system", versionInfo.isUpdatedSystemApp)
        }.apply()

        // Update flow
        _versionUpdates.value = versionCache.toMap()
    }
}
