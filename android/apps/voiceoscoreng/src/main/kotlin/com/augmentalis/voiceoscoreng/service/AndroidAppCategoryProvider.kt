package com.augmentalis.voiceoscoreng.service

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.augmentalis.commandmanager.AppCategory
import com.augmentalis.commandmanager.AppCategoryClassifier
import com.augmentalis.commandmanager.AppCategoryEntry
import com.augmentalis.commandmanager.IAppCategoryProvider
import com.augmentalis.commandmanager.IAppCategoryRepository
import com.augmentalis.commandmanager.IAppPatternGroupRepository
import com.augmentalis.commandmanager.getCategoryWithPatternFallback
import kotlinx.coroutines.runBlocking

/**
 * Android implementation of [IAppCategoryProvider] using hybrid 4-layer classification.
 *
 * This provider implements a comprehensive classification strategy optimized for
 * AOSP/RealWear devices that may not have Play Store access:
 *
 * ## Hybrid Classification Layers (Priority Order)
 *
 * | Layer | Source | Confidence | Description |
 * |-------|--------|------------|-------------|
 * | L1 | Database (ACD file) | 90-95% | Curated known apps from `known-apps.acd` |
 * | L2 | PackageManager API | 85-95% | Android's `ApplicationInfo.category` (API 26+) |
 * | L3 | Permission Heuristics | 75% | Infer from requested permissions |
 * | L4 | Pattern Matching | 70% | Package name substring matching |
 *
 * ## Database Layer (L1)
 *
 * The highest priority source. Load curated app categories from `known-apps.acd`
 * via [AppCategoryLoader] into SQLite. This works on all devices including AOSP
 * without Play Store. The ACD file contains:
 * - RealWear ecosystem apps
 * - Microsoft apps (Teams, Outlook, etc.)
 * - Google apps (Gmail, Maps, etc.)
 * - Common enterprise apps
 *
 * ## PackageManager Layer (L2)
 *
 * Uses `ApplicationInfo.category` (API 26+) for apps installed via Play Store.
 * Returns CATEGORY_UNDEFINED for sideloaded apps on AOSP devices.
 *
 * ## Permission Heuristics Layer (L3)
 *
 * Infers category from requested permissions when other methods fail:
 * - SMS/MMS permissions → MESSAGING
 * - Camera + Audio → MEDIA
 * - Contacts + Phone → PRODUCTIVITY
 *
 * ## Pattern Matching Layer (L4)
 *
 * Fallback to [AppCategoryClassifier.classifyByPattern] for remaining packages.
 *
 * ## Caching
 *
 * All classifications are cached in memory. Results from successful database
 * lookups can optionally be persisted back with source="learned" for future use.
 *
 * @property context Android context for PackageManager access
 * @property categoryRepository Repository for database lookups (optional, can be null for legacy mode)
 * @property patternRepository Repository for pattern groups (optional)
 * @see AppCategoryClassifier for pattern-based classification
 * @see AppCategoryLoader for loading ACD files
 */
class AndroidAppCategoryProvider(
    private val context: Context,
    private val categoryRepository: IAppCategoryRepository? = null,
    private val patternRepository: IAppPatternGroupRepository? = null
) : IAppCategoryProvider {

    private val cache = mutableMapOf<String, AppCategory>()
    private val confidenceCache = mutableMapOf<String, Float>()
    private val packageManager: PackageManager = context.packageManager

    /**
     * Gets the category for the specified package using hybrid 4-layer classification.
     *
     * Resolution order:
     * 1. Check in-memory cache
     * 2. Query database (ACD-loaded entries)
     * 3. Query PackageManager API (API 26+)
     * 4. Try permission heuristics
     * 5. Fall back to pattern-based classification
     *
     * @param packageName The Android package name (e.g., "com.twitter.android")
     * @return The determined [AppCategory], never null (defaults to UNKNOWN)
     */
    override fun getCategory(packageName: String): AppCategory {
        // L0: Check in-memory cache first
        cache[packageName]?.let { return it }

        var category: AppCategory? = null
        var confidence = 0.0f

        // L1: Database lookup (highest priority - ACD file data)
        if (categoryRepository != null) {
            val dbEntry = runBlocking {
                categoryRepository.getCategoryWithPatternFallback(packageName, patternRepository!!)
            }
            if (dbEntry != null) {
                category = AppCategoryClassifier.parseCategory(dbEntry.category)
                confidence = dbEntry.confidence
                if (category != AppCategory.UNKNOWN) {
                    cacheResult(packageName, category, confidence)
                    return category
                }
            }
        }

        // L2: PackageManager API (API 26+)
        if (category == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val pmCategory = mapAndroidCategoryToAppCategory(appInfo.category)
                if (pmCategory != null) {
                    category = pmCategory
                    confidence = 0.85f
                    // Optionally save learned category to database
                    saveLearnedCategory(packageName, category, confidence)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // Package not installed
            } catch (e: Exception) {
                // Unexpected error
            }
        }

        // L3: Permission heuristics
        if (category == null) {
            val permCategory = classifyByPermissions(packageName)
            if (permCategory != null) {
                category = permCategory
                confidence = 0.75f
            }
        }

        // L4: Pattern matching (lowest priority fallback)
        if (category == null) {
            @Suppress("DEPRECATION")
            category = AppCategoryClassifier.classifyByPattern(packageName)
            confidence = 0.70f
        }

        cacheResult(packageName, category, confidence)
        return category
    }

    /**
     * Classify app based on its requested permissions.
     *
     * Permission heuristics provide ~75% confidence classification
     * when other methods are unavailable.
     */
    private fun classifyByPermissions(packageName: String): AppCategory? {
        val permissions = try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.GET_PERMISSIONS
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_PERMISSIONS
            }
            packageManager.getPackageInfo(packageName, flags).requestedPermissions?.toSet()
                ?: emptySet()
        } catch (e: Exception) {
            return null
        }

        // Messaging indicators
        if (permissions.contains(Manifest.permission.SEND_SMS) ||
            permissions.contains(Manifest.permission.RECEIVE_SMS) ||
            permissions.contains(Manifest.permission.READ_SMS)) {
            return AppCategory.MESSAGING
        }

        // Media indicators
        if (permissions.contains(Manifest.permission.CAMERA) &&
            permissions.contains(Manifest.permission.RECORD_AUDIO)) {
            return AppCategory.MEDIA
        }

        // Productivity indicators (contacts + calendar)
        if (permissions.contains(Manifest.permission.READ_CONTACTS) &&
            permissions.contains(Manifest.permission.READ_CALENDAR)) {
            return AppCategory.PRODUCTIVITY
        }

        // Browser indicators (internet + downloads)
        val hasInternet = permissions.contains(Manifest.permission.INTERNET)
        val hasDownloads = permissions.any { it.contains("DOWNLOAD") }
        if (hasInternet && hasDownloads && permissions.size < 10) {
            // Simple app with internet + downloads, likely browser
            return AppCategory.BROWSER
        }

        return null
    }

    /**
     * Save a learned category to the database for future lookups.
     */
    private fun saveLearnedCategory(packageName: String, category: AppCategory, confidence: Float) {
        if (categoryRepository == null) return

        runBlocking {
            try {
                val now = System.currentTimeMillis()
                categoryRepository.upsertCategory(
                    packageName = packageName,
                    category = category.name,
                    source = "learned",
                    confidence = confidence,
                    acdVersion = null,
                    createdAt = now,
                    updatedAt = now
                )
            } catch (e: Exception) {
                // Ignore save failures - not critical
            }
        }
    }

    private fun cacheResult(packageName: String, category: AppCategory, confidence: Float) {
        cache[packageName] = category
        confidenceCache[packageName] = confidence
    }

    /**
     * Get the confidence of the last classification for a package.
     */
    fun getConfidence(packageName: String): Float {
        return confidenceCache[packageName] ?: 0.0f
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
     * - ACD file is reloaded
     */
    override fun clearCache() {
        cache.clear()
        confidenceCache.clear()
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
     * Note: On AOSP devices without Play Store, most apps return CATEGORY_UNDEFINED.
     * This is why we have the 4-layer hybrid approach.
     *
     * @param androidCategory The Android category constant from ApplicationInfo
     * @return Mapped [AppCategory] or null if should fall back to next layer
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
            ApplicationInfo.CATEGORY_UNDEFINED -> null // Fall back to next layer
            else -> null // Unknown category, fall back to next layer
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

    /**
     * Get cache statistics for debugging.
     *
     * @return Map of source type to count
     */
    fun getCacheStats(): Map<String, Int> {
        val byConfidence = confidenceCache.entries.groupBy { (_, confidence) ->
            when {
                confidence >= 0.90f -> "database"
                confidence >= 0.85f -> "packagemanager"
                confidence >= 0.75f -> "permissions"
                else -> "pattern"
            }
        }
        return byConfidence.mapValues { it.value.size }
    }

    companion object {
        /**
         * Check if this device supports PackageManager category API.
         *
         * @return true if API 26+ (Oreo or higher)
         */
        fun isPackageManagerCategorySupported(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        }

        /**
         * Create a provider with database support.
         *
         * @param context Android context
         * @param categoryRepository Repository for category lookups
         * @param patternRepository Repository for pattern groups
         * @return Configured provider with full hybrid support
         */
        fun withDatabase(
            context: Context,
            categoryRepository: IAppCategoryRepository,
            patternRepository: IAppPatternGroupRepository
        ): AndroidAppCategoryProvider {
            return AndroidAppCategoryProvider(context, categoryRepository, patternRepository)
        }

        /**
         * Create a legacy provider without database support.
         * Uses only PackageManager + pattern matching.
         *
         * @param context Android context
         * @return Provider with L2-L4 layers only
         */
        fun legacy(context: Context): AndroidAppCategoryProvider {
            return AndroidAppCategoryProvider(context, null, null)
        }
    }
}
