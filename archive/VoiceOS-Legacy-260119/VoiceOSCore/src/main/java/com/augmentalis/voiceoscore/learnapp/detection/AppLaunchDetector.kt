/**
 * AppLaunchDetector.kt - Detects when user launches new apps
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/detection/AppLaunchDetector.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Monitors accessibility events to detect app launches and trigger learning
 */

package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
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
     * Developer settings
     */
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

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
     * FIX (2025-11-30): Add buffer capacity to prevent memory leak under rapid app switching
     * extraBufferCapacity=10 keeps last 10 events, DROP_OLDEST prevents unbounded growth
     */
    private val _appLaunchEvents = MutableSharedFlow<AppLaunchEvent>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val appLaunchEvents: SharedFlow<AppLaunchEvent> = _appLaunchEvents.asSharedFlow()

    // FIX (2025-11-30): Removed Layer 1 debouncing variables
    // The Flow collector in LearnAppIntegration already has 500ms debounce
    // Double debouncing was dropping valid app launch events

    /**
     * Handle accessibility event
     *
     * Main entry point. Call this from AccessibilityService.onAccessibilityEvent().
     *
     * @param event Accessibility event to process
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Log all events received for debugging
        val eventTypeStr = AccessibilityEvent.eventTypeToString(event.eventType)
        val packageName = event.packageName?.toString()
        Log.v(TAG, "AppLaunchDetector received event: $eventTypeStr from $packageName")

        // Only process window state changes
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.v(TAG, "Ignoring event (not TYPE_WINDOW_STATE_CHANGED): $eventTypeStr")
            return
        }

        // Extract package name
        if (packageName == null) {
            Log.v(TAG, "Ignoring event (no package name)")
            return
        }

        // Filter out VoiceOS itself (don't scrape own package)
        if (packageName == context.packageName) {
            Log.v(TAG, "Ignoring VoiceOS package: $packageName")
            return
        }

        // FIX (2025-11-30): Removed Layer 1 debouncing - Flow collector handles it
        // The 500ms debounce in LearnAppIntegration.setupEventListeners() is sufficient
        Log.d(TAG, "Processing window state change for package: $packageName")

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
        Log.d(TAG, "processPackageLaunch() checking: $packageName")

        // Filter system apps
        if (isSystemApp(packageName)) {
            Log.v(TAG, "Filtered system app: $packageName")
            return
        }

        // Filter already learned apps
        if (learnedAppTracker.isAppLearned(packageName)) {
            Log.v(TAG, "Filtered already learned app: $packageName")
            return
        }

        // Filter recently dismissed apps
        if (learnedAppTracker.wasRecentlyDismissed(packageName)) {
            Log.v(TAG, "Filtered recently dismissed app: $packageName")
            return
        }

        // Get app name
        val appName = getAppName(packageName) ?: packageName

        Log.i(TAG, "✓ NEW APP DETECTED: $packageName ($appName) - emitting event")

        // FIX (2025-11-30): Emit with retry to prevent event loss (P1-H8)
        val event = AppLaunchEvent.NewAppDetected(
            packageName = packageName,
            appName = appName
        )
        emitWithRetry(event)
    }

    /**
     * Emit event with retry logic
     *
     * FIX (2025-11-30): P1-H8 - Prevents event loss when Flow buffer is temporarily full.
     * Uses exponential backoff for retries.
     *
     * @param event Event to emit
     */
    private suspend fun emitWithRetry(event: AppLaunchEvent) {
        var attempt = 0
        var lastException: Exception? = null
        val maxRetries = developerSettings.getMaxAppLaunchEmitRetries()
        val baseDelayMs = developerSettings.getAppLaunchEmitRetryDelayMs()

        while (attempt < maxRetries) {
            try {
                _appLaunchEvents.emit(event)
                if (attempt > 0) {
                    Log.d(TAG, "Event emitted successfully after ${attempt + 1} attempts")
                }
                return // Success
            } catch (e: Exception) {
                lastException = e
                attempt++
                if (attempt < maxRetries) {
                    // Exponential backoff: baseDelayMs, 2*baseDelayMs, 4*baseDelayMs
                    val delayMs = baseDelayMs * (1 shl (attempt - 1))
                    Log.w(TAG, "Emit attempt $attempt failed, retrying in ${delayMs}ms", e)
                    delay(delayMs)
                }
            }
        }

        // All retries exhausted
        Log.e(TAG, "Failed to emit event after $maxRetries attempts: $event", lastException)
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
        // Exact match system packages
        val systemPackages = setOf(
            "android",  // Core Android system package
            "com.android.systemui"
        )

        if (systemPackages.contains(packageName)) {
            return true
        }

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
     * FIX (2025-11-30): Now a no-op since Layer 1 debouncing was removed
     */
    @Suppress("UNUSED_PARAMETER")
    fun resetDebounce() {
        // No-op: Layer 1 debouncing removed, Flow handles it
    }

    companion object {
        /**
         * Log tag
         */
        private const val TAG = "AppLaunchDetector"

        /**
         * FIX (2025-11-30): Event emission retry configuration
         * P1-H8: Prevents event loss when SharedFlow buffer is temporarily full
         *
         * @deprecated Use developerSettings instance methods instead
         */
        @Deprecated("Use developerSettings.getMaxAppLaunchEmitRetries()", ReplaceWith("developerSettings.getMaxAppLaunchEmitRetries()"))
        private const val MAX_EMIT_RETRIES = 3

        @Deprecated("Use developerSettings.getAppLaunchEmitRetryDelayMs()", ReplaceWith("developerSettings.getAppLaunchEmitRetryDelayMs()"))
        private const val EMIT_RETRY_DELAY_MS = 100L
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
