/**
 * ScrapedAppMetadataSourceImpl.kt - Implementation of ScrapedAppMetadataSource
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/database/repository/ScrapedAppMetadataSourceImpl.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-08
 *
 * Implementation of ScrapedAppMetadataSource interface using SQLDelight database.
 * Provides app metadata from AppScrapingDatabase (scraped_app table).
 */

package com.augmentalis.voiceoscore.learnapp.database.repository

import android.content.Context
import android.content.pm.PackageManager
import com.augmentalis.database.VoiceOSDatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scraped App Metadata Source Implementation
 *
 * Provides app metadata from AppScrapingDatabase using SQLDelight queries.
 * Used by AppMetadataProvider for app name → package resolution and metadata lookup.
 *
 * ## Architecture
 *
 * This implementation avoids circular dependency between LearnApp and VoiceOSCore
 * by implementing the interface defined in the LearnApp repository module.
 *
 * ## Database Schema
 *
 * Uses `scraped_app` table from AppScrapingDatabase:
 * - appId (PRIMARY KEY)
 * - packageName
 * - versionCode
 * - versionName
 * - appHash
 * - firstScrapedAt
 * - lastScrapedAt
 * - (other fields not used by this interface)
 *
 * Note: scraped_app table does NOT store appName. This implementation
 * uses PackageManager to resolve package name → app name for user-facing features.
 *
 * @property context Application context for PackageManager access
 * @property databaseManager VoiceOS database manager (SQLDelight)
 *
 * @since Phase 4 - Relearn App Command
 */
class ScrapedAppMetadataSourceImpl(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager
) : ScrapedAppMetadataSource {

    /**
     * Get human-readable app name from PackageManager
     *
     * Falls back to package name if app is not installed or name cannot be resolved.
     *
     * @param packageName Package name to resolve
     * @return Human-readable app name or package name as fallback
     */
    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            // App not installed, use package name as fallback
            packageName
        } catch (e: Exception) {
            // Unexpected error, use package name as fallback
            packageName
        }
    }

    /**
     * Get apps by package name sorted by version code descending
     *
     * Returns all versions of the app with the given package name,
     * with most recent version first.
     *
     * @param packageName Package name to look up
     * @return List of scraped app metadata (most recent first)
     *
     * @since Phase 4
     */
    override suspend fun getAppsByPackageName(packageName: String): List<ScrapedAppMetadata> = withContext(Dispatchers.IO) {
        val appName = getAppName(packageName) // Resolve app name once for all versions

        databaseManager.scrapedAppQueries.getByPackage(packageName)
            .executeAsList()
            .sortedByDescending { it.versionCode } // Most recent version first
            .map { entity ->
                ScrapedAppMetadata(
                    packageName = entity.packageName,
                    appName = appName,
                    versionCode = entity.versionCode.toInt(),
                    versionName = entity.versionName,
                    appHash = entity.appHash,
                    firstScraped = entity.firstScrapedAt
                )
            }
    }

    /**
     * Get all scraped apps
     *
     * Returns all apps in the scraping database, sorted by last scraped time descending.
     * Used for app name → package resolution in RelearnAppCommand.
     *
     * @return List of all scraped app metadata (most recently scraped first)
     *
     * @since Phase 4 - Relearn App Command
     */
    override suspend fun getAllApps(): List<ScrapedAppMetadata> = withContext(Dispatchers.IO) {
        databaseManager.scrapedAppQueries.getAll()
            .executeAsList()
            .map { entity ->
                ScrapedAppMetadata(
                    packageName = entity.packageName,
                    appName = getAppName(entity.packageName),
                    versionCode = entity.versionCode.toInt(),
                    versionName = entity.versionName,
                    appHash = entity.appHash,
                    firstScraped = entity.firstScrapedAt
                )
            }
    }
}
