/**
 * ScrapedAppMetadataSource.kt - Interface for scraping database metadata lookup
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/database/repository/ScrapedAppMetadataSource.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-28
 *
 * Interface abstraction to avoid circular dependency between LearnApp and VoiceOSCore
 */

package com.augmentalis.voiceoscore.learnapp.database.repository

/**
 * Scraped App Metadata Source
 *
 * Interface for retrieving app metadata from AppScrapingDatabase.
 * Avoids circular dependency by defining an interface instead of depending on ScrapedAppDao directly.
 *
 * Implementation lives in VoiceOSCore module and is injected into LearnApp at runtime.
 *
 * @since 1.0.0
 */
interface ScrapedAppMetadataSource {
    /**
     * Get apps by package name sorted by version code descending
     *
     * @param packageName Package name to look up
     * @return List of scraped app metadata (most recent first)
     *
     * @since 1.0.0
     */
    suspend fun getAppsByPackageName(packageName: String): List<ScrapedAppMetadata>

    /**
     * Get all scraped apps
     *
     * Returns all apps in the scraping database.
     * Used for app name → package resolution.
     *
     * @return List of all scraped app metadata
     *
     * @since Phase 4 - Relearn App Command
     */
    suspend fun getAllApps(): List<ScrapedAppMetadata>
}

/**
 * Scraped App Metadata
 *
 * Metadata from AppScrapingDatabase (subset of ScrapedAppEntity fields needed for LearnApp).
 *
 * @property packageName Android package name
 * @property appName Human-readable app name
 * @property versionCode Android version code
 * @property versionName Android version name
 * @property appHash MD5 hash of packageName + versionCode
 * @property firstScraped When app was first scraped (milliseconds)
 *
 * @since 1.0.0
 */
data class ScrapedAppMetadata(
    val packageName: String,
    val appName: String,
    val versionCode: Int,
    val versionName: String,
    val appHash: String,
    val firstScraped: Long
)
