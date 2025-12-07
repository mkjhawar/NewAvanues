/**
 * FeatureFlagManager.kt - Feature flag management for gradual rollout
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-31
 *
 * Phase 3E: Rollout Infrastructure - Production Readiness
 *
 * Manages per-app feature flags to enable gradual rollout of new features.
 * Allows selective enablement of LearnApp exploration and dynamic scraping
 * on a per-package basis.
 */
package com.augmentalis.voiceoscore.accessibility.utils

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Feature Flag Manager
 *
 * Provides per-app feature control for gradual rollout and A/B testing.
 * All feature flags default to ENABLED (opt-out, not opt-in).
 *
 * ## Features:
 * - Per-app LearnApp enablement
 * - Per-app dynamic scraping enablement
 * - Per-app scraping depth limits
 * - Database-backed (persistent across restarts)
 *
 * ## Usage:
 * ```kotlin
 * val manager = FeatureFlagManager(context)
 *
 * // Check if feature enabled
 * if (manager.isLearnAppEnabled("com.example.app")) {
 *     // Run LearnApp exploration
 * }
 *
 * // Get scraping depth limit
 * val maxDepth = manager.getMaxScrapeDepth("com.example.app")
 *
 * // Disable feature for specific app
 * manager.setDynamicScrapingEnabled("com.example.app", false)
 * ```
 */
class FeatureFlagManager(private val context: Context) {

    companion object {
        private const val TAG = "FeatureFlagManager"

        // Default values when app not in database
        private const val DEFAULT_LEARN_APP_ENABLED = true
        private const val DEFAULT_DYNAMIC_SCRAPING_ENABLED = true
        private const val DEFAULT_MAX_SCRAPE_DEPTH = 50  // Matches MAX_DEPTH in AccessibilityScrapingIntegration
    }

    private val database: VoiceOSAppDatabase by lazy {
        VoiceOSAppDatabase.getInstance(context)
    }

    /**
     * Check if LearnApp exploration is enabled for this package
     *
     * @param packageName Package to check
     * @return true if LearnApp should run (default: true)
     */
    suspend fun isLearnAppEnabled(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val app = database.getApp(packageName)
                val enabled = app?.learnAppEnabled ?: DEFAULT_LEARN_APP_ENABLED

                Log.d(TAG, "LearnApp enabled for $packageName: $enabled")
                enabled

            } catch (e: Exception) {
                Log.e(TAG, "Error checking LearnApp flag for $packageName", e)
                DEFAULT_LEARN_APP_ENABLED
            }
        }
    }

    /**
     * Check if dynamic scraping is enabled for this package
     *
     * @param packageName Package to check
     * @return true if scraping should run (default: true)
     */
    suspend fun isDynamicScrapingEnabled(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val app = database.getApp(packageName)
                val enabled = app?.dynamicScrapingEnabled ?: DEFAULT_DYNAMIC_SCRAPING_ENABLED

                Log.d(TAG, "Dynamic scraping enabled for $packageName: $enabled")
                enabled

            } catch (e: Exception) {
                Log.e(TAG, "Error checking dynamic scraping flag for $packageName", e)
                DEFAULT_DYNAMIC_SCRAPING_ENABLED
            }
        }
    }

    /**
     * Get maximum scraping depth for this package
     *
     * Allows per-app depth limiting for performance tuning.
     *
     * @param packageName Package to check
     * @return Maximum depth (default: 50)
     */
    suspend fun getMaxScrapeDepth(packageName: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val app = database.getApp(packageName)
                val depth = app?.maxScrapeDepth ?: DEFAULT_MAX_SCRAPE_DEPTH

                if (app?.maxScrapeDepth != null) {
                    Log.d(TAG, "Custom max scrape depth for $packageName: $depth")
                }

                depth

            } catch (e: Exception) {
                Log.e(TAG, "Error getting max scrape depth for $packageName", e)
                DEFAULT_MAX_SCRAPE_DEPTH
            }
        }
    }

    /**
     * Set LearnApp enabled state for a package
     *
     * @param packageName Package to update
     * @param enabled New enabled state
     */
    suspend fun setLearnAppEnabled(packageName: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val app = database.getApp(packageName)
                if (app != null) {
                    val updated = app.copy(learnAppEnabled = enabled)
                    database.updateApp(updated)
                    Log.i(TAG, "Set LearnApp enabled=$enabled for $packageName")
                } else {
                    Log.w(TAG, "Cannot set LearnApp flag - app not in database: $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting LearnApp flag for $packageName", e)
            }
        }
    }

    /**
     * Set dynamic scraping enabled state for a package
     *
     * @param packageName Package to update
     * @param enabled New enabled state
     */
    suspend fun setDynamicScrapingEnabled(packageName: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val app = database.getApp(packageName)
                if (app != null) {
                    val updated = app.copy(dynamicScrapingEnabled = enabled)
                    database.updateApp(updated)
                    Log.i(TAG, "Set dynamic scraping enabled=$enabled for $packageName")
                } else {
                    Log.w(TAG, "Cannot set dynamic scraping flag - app not in database: $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting dynamic scraping flag for $packageName", e)
            }
        }
    }

    /**
     * Set maximum scraping depth for a package
     *
     * @param packageName Package to update
     * @param depth Maximum depth (null = use default)
     */
    suspend fun setMaxScrapeDepth(packageName: String, depth: Int?) {
        withContext(Dispatchers.IO) {
            try {
                val app = database.getApp(packageName)
                if (app != null) {
                    val updated = app.copy(maxScrapeDepth = depth ?: 5) // Default to 5 if null
                    database.updateApp(updated)
                    Log.i(TAG, "Set max scrape depth=$depth for $packageName")
                } else {
                    Log.w(TAG, "Cannot set max scrape depth - app not in database: $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting max scrape depth for $packageName", e)
            }
        }
    }

    /**
     * Get all feature flags for a package
     *
     * Returns a summary of all flags for debugging/UI display.
     *
     * @param packageName Package to check
     * @return FeatureFlags data class with all flags
     */
    suspend fun getFeatureFlags(packageName: String): FeatureFlags {
        return withContext(Dispatchers.IO) {
            try {
                val app = database.getApp(packageName)
                FeatureFlags(
                    learnAppEnabled = app?.learnAppEnabled ?: DEFAULT_LEARN_APP_ENABLED,
                    dynamicScrapingEnabled = app?.dynamicScrapingEnabled ?: DEFAULT_DYNAMIC_SCRAPING_ENABLED,
                    maxScrapeDepth = app?.maxScrapeDepth ?: DEFAULT_MAX_SCRAPE_DEPTH,
                    isInDatabase = app != null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting feature flags for $packageName", e)
                FeatureFlags(
                    learnAppEnabled = DEFAULT_LEARN_APP_ENABLED,
                    dynamicScrapingEnabled = DEFAULT_DYNAMIC_SCRAPING_ENABLED,
                    maxScrapeDepth = DEFAULT_MAX_SCRAPE_DEPTH,
                    isInDatabase = false
                )
            }
        }
    }

    /**
     * Reset all feature flags to defaults for a package
     *
     * @param packageName Package to reset
     */
    suspend fun resetFeatureFlags(packageName: String) {
        withContext(Dispatchers.IO) {
            try {
                val app = database.getApp(packageName)
                if (app != null) {
                    val updated = app.copy(
                        learnAppEnabled = DEFAULT_LEARN_APP_ENABLED,
                        dynamicScrapingEnabled = DEFAULT_DYNAMIC_SCRAPING_ENABLED,
                        maxScrapeDepth = 5 // Reset to default depth
                    )
                    database.updateApp(updated)
                    Log.i(TAG, "Reset feature flags to defaults for $packageName")
                } else {
                    Log.w(TAG, "Cannot reset flags - app not in database: $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting feature flags for $packageName", e)
            }
        }
    }

    /**
     * Feature flags data class
     */
    data class FeatureFlags(
        val learnAppEnabled: Boolean,
        val dynamicScrapingEnabled: Boolean,
        val maxScrapeDepth: Int,
        val isInDatabase: Boolean
    ) {
        override fun toString(): String {
            return "FeatureFlags(" +
                   "learnApp=$learnAppEnabled, " +
                   "dynamicScraping=$dynamicScrapingEnabled, " +
                   "maxDepth=$maxScrapeDepth, " +
                   "inDb=$isInDatabase)"
        }
    }
}
