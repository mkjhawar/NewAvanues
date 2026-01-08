package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

/**
 * Device-agnostic launcher detection system using Android HOME intent query.
 *
 * This detector identifies all installed launcher applications (home screens) on any Android device
 * by querying the system for packages that handle the HOME intent. This approach works automatically
 * across all device manufacturers (Google Pixel, Samsung, OnePlus, Xiaomi, RealWear, etc.) without
 * requiring hardcoded package lists.
 *
 * ## Purpose
 * During app scraping and learning, we need to filter out launcher screens from the captured hierarchy.
 * Launcher screens contaminate the app database because:
 * - They appear during BACK recovery when navigation fails
 * - They get saved as part of the app hierarchy (wrong package association)
 * - They pollute the database with non-app UI elements
 *
 * ## Design Decisions
 * - **Device-agnostic**: Uses official Android API (PackageManager.queryIntentActivities)
 * - **Zero maintenance**: No hardcoded device lists, works on all Android devices automatically
 * - **Performance**: App-lifetime caching (launchers don't change during runtime)
 * - **Graceful degradation**: Returns empty set on errors, logs warnings, doesn't crash
 * - **Single responsibility**: Only detects launchers (not system UI or other system packages)
 *
 * ## Usage
 * ```kotlin
 * val detector = LauncherDetector(context)
 *
 * // Get all launcher packages
 * val launchers = detector.detectLauncherPackages()
 * // Returns: ["com.realwear.launcher", "com.google.android.apps.nexuslauncher"]
 *
 * // Check if specific package is a launcher
 * if (detector.isLauncher("com.realwear.launcher")) {
 *     // Skip scraping this window
 * }
 * ```
 *
 * ## Thread Safety
 * This class is thread-safe. The cache is lazily initialized on first access and safe for
 * concurrent reads. Multiple threads can call detectLauncherPackages() simultaneously.
 *
 * @param context Android context for accessing PackageManager
 *
 * @see com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine Integration with LearnApp
 */
class LauncherDetector(private val context: Context) {

    private val TAG = "LauncherDetector"

    /**
     * Cached launcher packages for app lifetime.
     *
     * Rationale for app-lifetime caching:
     * - Launcher packages don't change during app runtime
     * - PackageManager query is relatively expensive (disk I/O)
     * - May be called hundreds of times during LearnApp exploration
     * - User installing new launcher mid-session is extremely rare edge case
     *
     * Alternative approaches considered and rejected:
     * - No caching: Too expensive (repeated I/O)
     * - TTL-based cache (24h): Adds complexity without meaningful benefit
     * - Observable cache (invalidate on package changes): Over-engineered for this use case
     */
    @Volatile
    private var cachedLauncherPackages: Set<String>? = null

    /**
     * Detects all launcher applications installed on the device.
     *
     * Uses the HOME intent query to find all packages that can handle home screen functionality.
     * This is the official Android approach for launcher detection and works across all devices
     * and Android versions.
     *
     * ## Implementation Details
     * Queries PackageManager for activities that handle:
     * - Intent.ACTION_MAIN (main entry point)
     * - Intent.CATEGORY_HOME (home screen category)
     * - MATCH_DEFAULT_ONLY (only default handlers)
     *
     * ## Caching Behavior
     * - First call: Queries PackageManager, caches result
     * - Subsequent calls: Returns cached value instantly
     * - Cache lifetime: Entire app session
     *
     * ## Error Handling
     * On any exception (SecurityException, RuntimeException, etc.):
     * - Logs error with full stack trace
     * - Returns empty set (graceful degradation)
     * - Allows scraping to continue (just won't filter launchers)
     *
     * ## Performance
     * - First call: ~5-10ms (PackageManager query)
     * - Subsequent calls: <1ms (cached)
     *
     * @return Set of launcher package names (e.g., ["com.google.android.apps.nexuslauncher"])
     *         Returns empty set if detection fails or no launchers found
     */
    fun detectLauncherPackages(): Set<String> {
        // Return cached result if available (fast path)
        cachedLauncherPackages?.let {
            Log.v(TAG, "🏠 Returning cached launchers: ${it.size} package(s)")
            return it
        }

        // Slow path: Query PackageManager
        try {
            Log.d(TAG, "🔍 Detecting launcher packages via HOME intent query...")

            // Create HOME intent (standard launcher detection approach)
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

            // Query all activities that can handle HOME intent
            val resolveInfos = context.packageManager.queryIntentActivities(
                homeIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            // Extract package names
            val launchers = resolveInfos
                .map { it.activityInfo.packageName }
                .toSet()

            // Cache result
            cachedLauncherPackages = launchers

            // Log results
            if (launchers.isEmpty()) {
                Log.w(TAG, "⚠️ No launchers detected! This shouldn't happen on real devices.")
                Log.w(TAG, "   Possible causes: Test environment, emulator, system service unavailable")
            } else {
                Log.i(TAG, "✅ Detected ${launchers.size} launcher(s):")
                launchers.forEachIndexed { index, packageName ->
                    Log.i(TAG, "   ${index + 1}. $packageName")
                }
            }

            return launchers

        } catch (e: SecurityException) {
            // SecurityException: App doesn't have permission to query packages
            Log.e(TAG, "❌ SecurityException while detecting launchers (permission issue)", e)
            Log.e(TAG, "   Ensure QUERY_ALL_PACKAGES permission is declared if targeting Android 11+")
            return emptySet() // Graceful degradation

        } catch (e: RuntimeException) {
            // RuntimeException: PackageManager unavailable or system error
            Log.e(TAG, "❌ RuntimeException while detecting launchers (system error)", e)
            return emptySet() // Graceful degradation

        } catch (e: Exception) {
            // Catch-all for any unexpected errors
            Log.e(TAG, "❌ Unexpected error while detecting launchers", e)
            return emptySet() // Graceful degradation
        }
    }

    /**
     * Checks if the given package name is a launcher application.
     *
     * This is a convenience method that wraps detectLauncherPackages() for boolean checks.
     * Uses cached launcher list for performance.
     *
     * ## Usage Examples
     * ```kotlin
     * // Filter launcher windows during scraping
     * val windows = accessibilityService.windows
     * for (window in windows) {
     *     val packageName = window.root?.packageName?.toString() ?: continue
     *     if (launcherDetector.isLauncher(packageName)) {
     *         Log.d(TAG, "Skipping launcher window: $packageName")
     *         continue
     *     }
     *     scrapeWindow(window)
     * }
     *
     * // Validate package before scraping
     * if (launcherDetector.isLauncher(targetPackage)) {
     *     Log.w(TAG, "Cannot learn launcher app: $targetPackage")
     *     return
     * }
     * ```
     *
     * @param packageName Package name to check (e.g., "com.realwear.launcher")
     * @return true if package is a launcher, false otherwise
     */
    fun isLauncher(packageName: String): Boolean {
        return detectLauncherPackages().contains(packageName)
    }

    /**
     * Clears the launcher cache, forcing re-detection on next call.
     *
     * This method is primarily for testing purposes or handling edge cases where
     * a launcher might be installed/uninstalled during app runtime.
     *
     * ## When to Use
     * - Unit tests that need to reset state between test cases
     * - Responding to package installation broadcasts (advanced use case)
     * - Manual cache invalidation in settings/debug screens
     *
     * ## When NOT to Use
     * - Normal operation (cache is designed to last app lifetime)
     * - Performance-critical code paths (clearing cache forces expensive re-query)
     *
     * ## Thread Safety
     * Thread-safe due to @Volatile annotation on cachedLauncherPackages.
     *
     * @see detectLauncherPackages Re-detection happens automatically on next call
     */
    fun clearCache() {
        Log.d(TAG, "🔄 Clearing launcher cache (forced re-detection on next query)")
        cachedLauncherPackages = null
    }

    /**
     * Returns the current cache status for debugging/diagnostics.
     *
     * @return true if launchers are cached, false if next call will query PackageManager
     */
    fun isCached(): Boolean {
        return cachedLauncherPackages != null
    }

    /**
     * Gets detailed diagnostic information about the detector state.
     *
     * Useful for debugging, logging, and diagnostics screens.
     *
     * @return Map containing diagnostic information:
     *         - "cached": Boolean - whether results are cached
     *         - "launcherCount": Int - number of detected launchers (or -1 if not cached)
     *         - "launchers": List<String> - launcher package names (or empty if not cached)
     */
    fun getDiagnostics(): Map<String, Any> {
        val cached = cachedLauncherPackages
        return if (cached != null) {
            mapOf(
                "cached" to true,
                "launcherCount" to cached.size,
                "launchers" to cached.toList()
            )
        } else {
            mapOf(
                "cached" to false,
                "launcherCount" to -1,
                "launchers" to emptyList<String>()
            )
        }
    }

    companion object {
        /**
         * Known system UI packages that are NOT launchers but should still be filtered.
         *
         * These packages are universally present across all Android devices and handle
         * system-level UI (status bar, navigation bar, notifications, etc.).
         *
         * ## Why Separate from Launcher Detection
         * - System UI packages don't handle HOME intent (not launchers by definition)
         * - But they should still be excluded from scraping (not part of target app)
         * - These packages are truly universal (same across all devices)
         *
         * ## Usage
         * ```kotlin
         * if (launcherDetector.isLauncher(pkg) || SYSTEM_UI_PACKAGES.contains(pkg)) {
         *     // Skip this package
         * }
         * ```
         */
        val SYSTEM_UI_PACKAGES = setOf(
            "com.android.systemui"  // Status bar, navigation bar, notifications, quick settings
        )

        /**
         * Checks if package should be excluded from scraping (launcher or system UI).
         *
         * Convenience method combining launcher detection and system UI filtering.
         *
         * @param detector LauncherDetector instance
         * @param packageName Package name to check
         * @return true if package should be excluded from scraping
         */
        @JvmStatic
        fun shouldExcludeFromScraping(detector: LauncherDetector, packageName: String): Boolean {
            return detector.isLauncher(packageName) || SYSTEM_UI_PACKAGES.contains(packageName)
        }
    }
}
