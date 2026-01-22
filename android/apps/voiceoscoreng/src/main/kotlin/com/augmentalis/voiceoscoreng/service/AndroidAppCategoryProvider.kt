package com.augmentalis.voiceoscoreng.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.augmentalis.voiceoscore.AppCategory
import com.augmentalis.voiceoscore.AppCategoryClassifier
import com.augmentalis.voiceoscore.IAppCategoryProvider

/**
 * Android implementation of [IAppCategoryProvider] using PackageManager API.
 *
 * Uses ApplicationInfo.category (API 26+) for primary classification,
 * falling back to pattern-based classification for older devices or
 * when PackageManager data is unavailable.
 *
 * ## Category Mapping Strategy
 *
 * Android's PackageManager provides app categories starting from API 26 (Oreo).
 * This implementation maps those categories to our [AppCategory] enum which
 * represents dynamic content behavior patterns:
 *
 * | Android Category | AppCategory | Rationale |
 * |------------------|-------------|-----------|
 * | CATEGORY_SOCIAL | SOCIAL | Direct mapping - social feeds are highly dynamic |
 * | CATEGORY_NEWS | SOCIAL | News feeds behave like social feeds (constantly updating) |
 * | CATEGORY_PRODUCTIVITY | PRODUCTIVITY | Direct mapping - stable UI patterns |
 * | CATEGORY_MAPS | PRODUCTIVITY | Maps have predictable UI despite dynamic content |
 * | CATEGORY_AUDIO/VIDEO/IMAGE | MEDIA | Media apps have playback-focused UI |
 * | CATEGORY_GAME | MEDIA | Games share media characteristics |
 * | CATEGORY_ACCESSIBILITY | SYSTEM | System-level accessibility tools |
 * | CATEGORY_UNDEFINED | null | Falls back to pattern matching |
 *
 * ## Fallback Behavior
 *
 * When PackageManager category is unavailable (API < 26, app not found, or
 * CATEGORY_UNDEFINED), the provider falls back to [AppCategoryClassifier]
 * which uses package name pattern matching.
 *
 * ## Caching
 *
 * Results are cached in memory to avoid repeated PackageManager lookups.
 * Call [clearCache] when package state may have changed (e.g., app updates).
 *
 * @property context Android context for PackageManager access
 * @see AppCategoryClassifier for fallback pattern-based classification
 * @see AppCategory for category definitions and behaviors
 */
class AndroidAppCategoryProvider(
    private val context: Context
) : IAppCategoryProvider {

    private val cache = mutableMapOf<String, AppCategory>()
    private val packageManager: PackageManager = context.packageManager

    /**
     * Gets the category for the specified package.
     *
     * Resolution order:
     * 1. Check in-memory cache
     * 2. Query PackageManager (API 26+)
     * 3. Fall back to pattern-based classification
     *
     * @param packageName The Android package name (e.g., "com.twitter.android")
     * @return The determined [AppCategory], never null (defaults to UNKNOWN)
     */
    override fun getCategory(packageName: String): AppCategory {
        // Check cache first
        cache[packageName]?.let { return it }

        // Try PackageManager API (API 26+)
        val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                mapAndroidCategoryToAppCategory(appInfo.category)
            } catch (e: PackageManager.NameNotFoundException) {
                // Package not installed or not visible to this app
                null
            } catch (e: Exception) {
                // Handle any other unexpected PackageManager errors
                null
            }
        } else {
            null
        }

        // Fallback to pattern matching if API unavailable or returned UNDEFINED
        val finalCategory = category ?: AppCategoryClassifier.classifyPackage(packageName)

        // Cache result
        cache[packageName] = finalCategory
        return finalCategory
    }

    /**
     * Checks if the category for the given package is already cached.
     *
     * @param packageName The Android package name to check
     * @return true if cached, false otherwise
     */
    override fun isCached(packageName: String): Boolean = cache.containsKey(packageName)

    /**
     * Clears the category cache.
     *
     * Call this when:
     * - Apps are installed/uninstalled/updated
     * - Memory pressure requires cache eviction
     * - Testing requires fresh lookups
     */
    override fun clearCache() {
        cache.clear()
    }

    /**
     * Maps Android's ApplicationInfo.category constants to our AppCategory enum.
     *
     * Android categories (API 26+):
     * - CATEGORY_GAME = 0
     * - CATEGORY_AUDIO = 1
     * - CATEGORY_VIDEO = 2
     * - CATEGORY_IMAGE = 3
     * - CATEGORY_SOCIAL = 4
     * - CATEGORY_NEWS = 5
     * - CATEGORY_MAPS = 6
     * - CATEGORY_PRODUCTIVITY = 7
     * - CATEGORY_ACCESSIBILITY = 8
     * - CATEGORY_UNDEFINED = -1
     *
     * @param androidCategory The Android category constant from ApplicationInfo
     * @return Mapped [AppCategory] or null if should fall back to pattern matching
     */
    private fun mapAndroidCategoryToAppCategory(androidCategory: Int): AppCategory? {
        return when (androidCategory) {
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.SOCIAL // News feeds are dynamic like social
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.MEDIA
            ApplicationInfo.CATEGORY_GAME -> AppCategory.MEDIA // Games share media characteristics
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.PRODUCTIVITY // Maps have predictable UI
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.SYSTEM
            ApplicationInfo.CATEGORY_UNDEFINED -> null // Fall back to pattern matching
            else -> null // Unknown category, fall back to pattern matching
        }
    }

    /**
     * Gets the current cache size for monitoring/debugging.
     *
     * @return Number of cached package categories
     */
    fun getCacheSize(): Int = cache.size

    /**
     * Gets all cached package names for debugging.
     *
     * @return Set of cached package names
     */
    fun getCachedPackages(): Set<String> = cache.keys.toSet()

    companion object {
        /**
         * Check if this device supports PackageManager category API.
         *
         * @return true if API 26+ (Oreo or higher)
         */
        fun isPackageManagerCategorySupported(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        }
    }
}
