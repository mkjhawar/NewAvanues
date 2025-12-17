/**
 * AppLaunchDetector.kt - Detects when user launches new apps
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/detection/AppLaunchDetector.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Monitors accessibility events to detect app launches and trigger learning
 */

package com.augmentalis.learnapp.detection

import android.content.Context
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * App Launch Detector
 *
 * Monitors accessibility events to detect when users launch new apps.
 * Triggers consent flow for unlearned apps.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val detector = AppLaunchDetector(context, learnedAppTracker)
 *
 * // In AccessibilityService
 * override fun onAccessibilityEvent(event: AccessibilityEvent) {
 *     detector.onAccessibilityEvent(event)
 * }
 *
 * // Observe launch events
 * detector.appLaunchEvents.collect { event ->
 *     when (event) {
 *         is AppLaunchEvent.NewAppDetected -> {
 *             // Show consent dialog
 *         }
 *     }
 * }
 * ```
 *
 * ## Event Filtering
 *
 * - Only monitors TYPE_WINDOW_STATE_CHANGED events
 * - Filters VoiceOS itself (own package) to prevent self-scraping
 * - Filters system apps (com.android.*, android.*)
 * - Filters already learned apps
 * - Filters recently dismissed apps
 * - Debounces rapid events (100ms window)
 *
 * @property context Application context
 * @property learnedAppTracker Tracker for learned/dismissed apps
 *
 * @since 1.0.0
 */
class AppLaunchDetector(
    private val context: Context,
    private val learnedAppTracker: LearnedAppTracker
) {

    /**
     * Coroutine scope for async operations
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Package manager for app info
     */
    private val packageManager: PackageManager = context.packageManager

    /**
     * Shared flow for app launch events
     */
    private val _appLaunchEvents = MutableSharedFlow<AppLaunchEvent>(replay = 0)
    val appLaunchEvents: SharedFlow<AppLaunchEvent> = _appLaunchEvents.asSharedFlow()

    /**
     * Last event timestamp (for debouncing)
     */
    private var lastEventTimestamp = 0L

    /**
     * Last processed package (for debouncing)
     */
    private var lastProcessedPackage: String? = null

    /**
     * Handle accessibility event
     *
     * Main entry point. Call this from AccessibilityService.onAccessibilityEvent().
     *
     * @param event Accessibility event to process
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Only process window state changes
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // Extract package name
        val packageName = event.packageName?.toString() ?: return

        // Filter out VoiceOS itself (don't scrape own package)
        if (packageName == context.packageName) {
            return
        }

        // Debounce rapid events (100ms window)
        val now = System.currentTimeMillis()
        if (packageName == lastProcessedPackage && (now - lastEventTimestamp) < DEBOUNCE_WINDOW_MS) {
            return
        }

        lastEventTimestamp = now
        lastProcessedPackage = packageName

        // Process in background
        scope.launch {
            processPackageLaunch(packageName)
        }
    }

    /**
     * Process package launch
     *
     * Checks if app should trigger consent dialog.
     *
     * @param packageName Package name that was launched
     */
    private suspend fun processPackageLaunch(packageName: String) {
        // Filter system apps
        if (isSystemApp(packageName)) {
            return
        }

        // Filter already learned apps
        if (learnedAppTracker.isAppLearned(packageName)) {
            return
        }

        // Filter recently dismissed apps
        if (learnedAppTracker.wasRecentlyDismissed(packageName)) {
            return
        }

        // Get app name
        val appName = getAppName(packageName) ?: packageName

        // Emit new app detected event
        _appLaunchEvents.emit(
            AppLaunchEvent.NewAppDetected(
                packageName = packageName,
                appName = appName
            )
        )
    }

    /**
     * Check if package is a system app
     *
     * Filters out system apps that shouldn't be learned.
     *
     * @param packageName Package name to check
     * @return true if system app
     */
    private fun isSystemApp(packageName: String): Boolean {
        // System package prefixes
        val systemPrefixes = listOf(
            "com.android.",
            "android.",
            "com.google.android.ext.",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.realwear.launcher"
        )

        return systemPrefixes.any { packageName.startsWith(it) }
    }

    /**
     * Get human-readable app name
     *
     * @param packageName Package name
     * @return App name or null if not found
     */
    private fun getAppName(packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Check if package is installed
     *
     * @param packageName Package name to check
     * @return true if installed
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get package version code
     *
     * @param packageName Package name
     * @return Version code or -1 if not found
     */
    fun getPackageVersionCode(packageName: String): Long {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            -1L
        }
    }

    /**
     * Get package version name
     *
     * @param packageName Package name
     * @return Version name or null if not found
     */
    fun getPackageVersionName(packageName: String): String? {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Reset debounce state (for testing)
     */
    fun resetDebounce() {
        lastEventTimestamp = 0L
        lastProcessedPackage = null
    }

    companion object {
        /**
         * Debounce window in milliseconds
         */
        private const val DEBOUNCE_WINDOW_MS = 100L
    }
}

/**
 * App Launch Event
 *
 * Sealed class representing different app launch events.
 *
 * @since 1.0.0
 */
sealed class AppLaunchEvent {

    /**
     * New unlearned app detected
     *
     * Triggers consent dialog flow.
     *
     * @property packageName Package name of app
     * @property appName Human-readable app name
     */
    data class NewAppDetected(
        val packageName: String,
        val appName: String
    ) : AppLaunchEvent()

    /**
     * App already learned
     *
     * No action needed.
     *
     * @property packageName Package name of app
     */
    data class AppAlreadyLearned(
        val packageName: String
    ) : AppLaunchEvent()

    /**
     * App recently dismissed
     *
     * User declined to learn, don't show dialog again.
     *
     * @property packageName Package name of app
     */
    data class AppRecentlyDismissed(
        val packageName: String
    ) : AppLaunchEvent()

    /**
     * System app detected
     *
     * System apps are not learned.
     *
     * @property packageName Package name of system app
     */
    data class SystemAppDetected(
        val packageName: String
    ) : AppLaunchEvent()
}
