/**
 * AppMetadataProvider.kt - Provides app metadata from various sources
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Provides app metadata from PackageManager and scraped app database.
 */

package com.augmentalis.voiceoscore.learnapp.database.repository

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.runBlocking

/**
 * App Metadata Provider
 *
 * Provides app metadata from PackageManager and scraped app database.
 */
class AppMetadataProvider(
    private val context: Context,
    private val scrapedAppMetadataSource: ScrapedAppMetadataSource? = null
) {
    private val packageManager: PackageManager = context.packageManager

    /**
     * Get metadata for package
     */
    fun getMetadata(packageName: String): AppMetadata? {
        // First try scraped app database
        scrapedAppMetadataSource?.getMetadata(packageName)?.let { return it }

        // Fall back to PackageManager
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val appInfo = packageManager.getApplicationInfo(packageName, 0)

            AppMetadata(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                versionCode = packageInfo.longVersionCode,
                versionName = packageInfo.versionName ?: "",
                appHash = packageName.hashCode().toString(),
                isInstalled = true,
                source = MetadataSource.PACKAGE_MANAGER
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Check if package is installed
     */
    fun isInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

/**
 * App Metadata
 *
 * Metadata about an installed app.
 */
data class AppMetadata(
    val packageName: String,
    val appName: String,
    val versionCode: Long,
    val versionName: String,
    val appHash: String,
    val isInstalled: Boolean,
    val source: MetadataSource
)

/**
 * Metadata Source
 */
enum class MetadataSource {
    SCRAPED_APP_DATABASE,
    PACKAGE_MANAGER
}

/**
 * Interface for scraped app metadata lookup
 */
interface ScrapedAppMetadataSource {
    fun getMetadata(packageName: String): AppMetadata?
}

/**
 * Implementation of ScrapedAppMetadataSource using VoiceOSDatabaseManager
 */
class ScrapedAppMetadataSourceImpl(
    private val context: Context,
    private val databaseManager: com.augmentalis.database.VoiceOSDatabaseManager
) : ScrapedAppMetadataSource {

    override fun getMetadata(packageName: String): AppMetadata? {
        return try {
            val scrapedApp = runBlocking { databaseManager.scrapedApps.getByPackage(packageName) }
            scrapedApp?.let {
                AppMetadata(
                    packageName = it.packageName,
                    appName = it.packageName, // Use package name as app name fallback
                    versionCode = it.versionCode,
                    versionName = it.versionName,
                    appHash = it.appHash,
                    isInstalled = true,
                    source = MetadataSource.SCRAPED_APP_DATABASE
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
