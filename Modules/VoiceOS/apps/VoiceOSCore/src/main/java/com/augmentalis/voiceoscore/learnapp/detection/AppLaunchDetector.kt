/**
 * AppLaunchDetector.kt - Detects app launches for exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Detects when apps are launched to trigger exploration.
 */

package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context

/**
 * App Launch Detector
 *
 * Detects app launches and notifies listeners for exploration triggers.
 */
class AppLaunchDetector(
    private val context: Context,
    private val learnedAppTracker: LearnedAppTracker
) {
    private var currentPackage: String? = null
    private var listeners = mutableListOf<(AppLaunchEvent) -> Unit>()

    /**
     * Register listener for app launch events
     */
    fun addListener(listener: (AppLaunchEvent) -> Unit) {
        listeners.add(listener)
    }

    /**
     * Remove listener
     */
    fun removeListener(listener: (AppLaunchEvent) -> Unit) {
        listeners.remove(listener)
    }

    /**
     * Called when package changes
     */
    fun onPackageChanged(packageName: String, appName: String? = null) {
        if (packageName != currentPackage) {
            val previousPackage = currentPackage
            currentPackage = packageName

            // Check if this is a new app launch
            if (previousPackage != null && packageName != previousPackage) {
                val event = AppLaunchEvent.NewAppDetected(
                    packageName = packageName,
                    appName = appName ?: packageName.substringAfterLast('.'),
                    previousPackage = previousPackage,
                    isLearned = learnedAppTracker.isFullyLearned(packageName),
                    timestamp = System.currentTimeMillis()
                )
                notifyListeners(event)
            }
        }
    }

    /**
     * Check if current app needs learning
     */
    fun needsLearning(): Boolean {
        val pkg = currentPackage ?: return false
        return !learnedAppTracker.isFullyLearned(pkg) &&
               !learnedAppTracker.isExcluded(pkg)
    }

    /**
     * Get current package name
     */
    fun getCurrentPackage(): String? = currentPackage

    private fun notifyListeners(event: AppLaunchEvent) {
        listeners.forEach { it(event) }
    }
}

/**
 * App Launch Event
 *
 * Sealed class hierarchy for app launch detection events.
 */
sealed class AppLaunchEvent {
    abstract val packageName: String
    abstract val timestamp: Long

    /**
     * New app detected event
     */
    data class NewAppDetected(
        override val packageName: String,
        val appName: String,
        val previousPackage: String?,
        val isLearned: Boolean,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AppLaunchEvent()

    /**
     * App resumed event (same app, different activity)
     */
    data class AppResumed(
        override val packageName: String,
        val activityName: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AppLaunchEvent()

    /**
     * App closed event
     */
    data class AppClosed(
        override val packageName: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AppLaunchEvent()

    /**
     * System UI detected
     */
    data class SystemUIDetected(
        override val packageName: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AppLaunchEvent()
}
