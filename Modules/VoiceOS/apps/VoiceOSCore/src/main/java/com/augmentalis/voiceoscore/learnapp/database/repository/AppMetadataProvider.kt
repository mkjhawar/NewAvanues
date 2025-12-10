/**
 * AppMetadataProvider.kt - Provides app metadata from multiple sources
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/database/repository/AppMetadataProvider.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-28
 *
 * Provides app metadata from AppScrapingDatabase or PackageManager fallback
 */

package com.augmentalis.voiceoscore.learnapp.database.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * App Metadata Provider
 *
 * Provides app metadata from multiple sources with fallback logic:
 * 1. First tries AppScrapingDatabase (if app has been scraped)
 * 2. Falls back to Android PackageManager
 * 3. Returns error if package not found
 *
 * @property context Application context for PackageManager access
 * @property scrapedAppMetadataSource Interface for AppScrapingDatabase metadata lookup (optional)
 *
 * @since 1.0.0
 */
class AppMetadataProvider(
    private val context: Context,
    private val scrapedAppMetadataSource: ScrapedAppMetadataSource? = null
) {

    /**
     * Get app metadata from best available source
     *
     * Lookup order:
     * 1. AppScrapingDatabase (if metadata source provided and app exists)
     * 2. PackageManager (always available fallback)
     *
     * @param packageName Package name to look up
     * @return AppMetadata or null if package not found
     *
     * @since 1.0.0
     */
    suspend fun getMetadata(packageName: String): AppMetadata? = withContext(Dispatchers.IO) {
        // Try AppScrapingDatabase first (most recent metadata)
        scrapedAppMetadataSource?.let { source ->
            val scrapedApps = source.getAppsByPackageName(packageName)
            if (scrapedApps.isNotEmpty()) {
                // Use most recent version (sorted by versionCode DESC)
                val latestApp = scrapedApps.first()
                return@withContext AppMetadata(
                    packageName = latestApp.packageName,
                    appName = latestApp.appName,
                    versionCode = latestApp.versionCode.toLong(),
                    versionName = latestApp.versionName,
                    appHash = latestApp.appHash,
                    installTimestamp = latestApp.firstScraped,
                    isSystemApp = false, // Not stored in AppScrapingDatabase
                    isInstalled = true,
                    source = MetadataSource.SCRAPING_DATABASE
                )
            }
        }

        // Fallback to PackageManager
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val appInfo = packageManager.getApplicationInfo(packageName, 0)

            AppMetadata(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                versionCode = extractVersionCode(packageInfo),
                versionName = packageInfo.versionName ?: "unknown",
                appHash = calculateAppHash(packageName, extractVersionCode(packageInfo)),
                installTimestamp = packageInfo.firstInstallTime,
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isInstalled = true,
                source = MetadataSource.PACKAGE_MANAGER
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Extract version code compatible with API 28+
     *
     * @param packageInfo Package info from PackageManager
     * @return Version code as Long
     *
     * @since 1.0.0
     */
    @Suppress("DEPRECATION")
    private fun extractVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }

    /**
     * Calculate app hash from package name and version code
     *
     * Uses MD5 hash for consistency with AppScrapingDatabase.
     *
     * @param packageName Package name
     * @param versionCode Version code
     * @return MD5 hash string
     *
     * @since 1.0.0
     */
    private fun calculateAppHash(packageName: String, versionCode: Long): String {
        val input = "$packageName:$versionCode"
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Resolve package name from app name
     *
     * Searches for installed apps matching the given app name (case-insensitive).
     * Used by RelearnAppCommand to resolve "relearn DeviceInfo" → com.ytheekshana.deviceinfo.
     *
     * @param appName Human-readable app name (e.g., "DeviceInfo", "Microsoft Teams")
     * @return Package name if found, null otherwise
     *
     * @since Phase 4 - Relearn App Command
     */
    suspend fun resolvePackageByAppName(appName: String): String? = withContext(Dispatchers.IO) {
        val normalizedAppName = appName.trim().lowercase()

        // Try AppScrapingDatabase first
        scrapedAppMetadataSource?.let { source ->
            val allApps = source.getAllApps()
            val match = allApps.firstOrNull { app ->
                app.appName.lowercase().contains(normalizedAppName) ||
                        normalizedAppName.contains(app.appName.lowercase())
            }
            if (match != null) {
                return@withContext match.packageName
            }
        }

        // Fallback to PackageManager
        try {
            val packageManager = context.packageManager
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            val match = installedApps.firstOrNull { appInfo ->
                val label = packageManager.getApplicationLabel(appInfo).toString()
                label.lowercase().contains(normalizedAppName) ||
                        normalizedAppName.contains(label.lowercase())
            }

            match?.packageName
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * App Metadata
 *
 * Container for app metadata from multiple sources.
 *
 * @property packageName Android package name (e.g., "com.example.app")
 * @property appName Human-readable app name (e.g., "Example App")
 * @property versionCode Android version code (integer)
 * @property versionName Android version name (e.g., "1.2.3")
 * @property appHash MD5 hash of packageName + versionCode
 * @property installTimestamp When app was first installed (milliseconds)
 * @property isSystemApp Whether app is a system app
 * @property isInstalled Whether app is currently installed
 * @property source Where metadata came from
 *
 * @since 1.0.0
 */
data class AppMetadata(
    val packageName: String,
    val appName: String,
    val versionCode: Long,
    val versionName: String,
    val appHash: String,
    val installTimestamp: Long = System.currentTimeMillis(),
    val isSystemApp: Boolean = false,
    val isInstalled: Boolean = true,
    val source: MetadataSource = MetadataSource.PACKAGE_MANAGER
)

/**
 * Metadata Source
 *
 * Indicates where app metadata was retrieved from.
 */
enum class MetadataSource {
    /** Metadata from AppScrapingDatabase (most recent) */
    SCRAPING_DATABASE,

    /** Metadata from Android PackageManager (fallback) */
    PACKAGE_MANAGER
}
