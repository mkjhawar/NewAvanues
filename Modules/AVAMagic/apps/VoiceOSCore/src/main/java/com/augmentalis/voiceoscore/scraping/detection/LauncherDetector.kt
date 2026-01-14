/**
 * LauncherDetector.kt - Device-agnostic launcher detection
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/detection/LauncherDetector.kt
 *
 * Author: VoiceOS Restoration Team
 * Created: 2025-11-27
 * Implemented: 2025-11-27
 *
 * Detects launcher apps using PackageManager queries.
 * Replaces hardcoded EXCLUDED_PACKAGES list with dynamic detection.
 */

package com.augmentalis.voiceoscore.scraping.detection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

/**
 * Launcher Detector
 *
 * Detects if a package is a launcher app (home screen).
 * Replaces hardcoded EXCLUDED_PACKAGES list with dynamic detection.
 *
 * **Implementation:**
 * - Queries PackageManager for apps with ACTION_MAIN + CATEGORY_HOME
 * - Caches launcher package names for performance
 * - Supports multiple launchers installed on device
 *
 * **Example Usage:**
 * ```kotlin
 * val detector = LauncherDetector(context)
 * if (detector.isLauncher("com.android.launcher3")) {
 *     // Skip scraping launcher apps
 * }
 * ```
 *
 * @property context Application context
 */
class LauncherDetector(
    private val context: Context
) {

    /**
     * Cached set of launcher package names.
     * Lazily initialized on first access.
     */
    private val launcherPackages: Set<String> by lazy {
        queryLauncherPackages()
    }

    /**
     * Check if package is a launcher app
     *
     * Checks if the given package name is registered as a launcher
     * (handles ACTION_MAIN + CATEGORY_HOME intent).
     *
     * @param packageName Package name to check
     * @return True if launcher, false otherwise
     */
    fun isLauncher(packageName: String): Boolean {
        return launcherPackages.contains(packageName)
    }

    /**
     * Get all installed launcher packages
     *
     * Returns all packages that can handle the home intent.
     * Result is cached for performance.
     *
     * @return List of launcher package names
     */
    fun getAllLaunchers(): List<String> {
        return launcherPackages.toList()
    }

    /**
     * Get default launcher package
     *
     * Returns the package name of the currently set default launcher.
     * May return null if no default is set or if unable to determine.
     *
     * @return Default launcher package name, or null if none
     */
    fun getDefaultLauncher(): String? {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolveInfo?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Query PackageManager for all launcher packages
     *
     * Internal method that queries for all activities that can handle
     * the home intent (ACTION_MAIN + CATEGORY_HOME).
     *
     * @return Set of launcher package names
     */
    private fun queryLauncherPackages(): Set<String> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

            val resolveInfoList: List<ResolveInfo> = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            )

            resolveInfoList
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        } catch (e: Exception) {
            // Return empty set if query fails
            emptySet()
        }
    }
}
