package com.augmentalis.cockpit

import kotlinx.serialization.Serializable

/**
 * Platform-agnostic external app resolution and launch.
 *
 * Each platform implements this interface to check whether a 3rd-party app
 * is installed, whether it supports activity embedding (Android 12L+),
 * and to launch it adjacent to the Cockpit window.
 *
 * Architecture:
 * - Android: uses PackageManager for resolution, FLAG_ACTIVITY_LAUNCH_ADJACENT for launch
 * - Desktop: stub (external apps not embeddable on JVM desktop)
 * - iOS: deferred (URL schemes + UIKit embedding TBD)
 *
 * Injected into the ViewModel via DI (Hilt on Android, manual on Desktop).
 */
interface IExternalAppResolver {

    /**
     * Check if the app identified by [packageName] is installed and whether
     * it supports embedded display (e.g., `allowUntrustedActivityEmbedding`
     * in its manifest on Android 12L+).
     */
    fun resolveApp(packageName: String): ExternalAppStatus

    /**
     * Launch the app in a split-screen / adjacent window.
     *
     * On Android this uses FLAG_ACTIVITY_LAUNCH_ADJACENT | FLAG_ACTIVITY_NEW_TASK.
     * On Desktop this could use ProcessBuilder to launch a native process.
     *
     * @param packageName Target app package
     * @param activityName Optional specific activity class to launch
     */
    fun launchAdjacent(packageName: String, activityName: String = "")
}

/**
 * Result of resolving a 3rd-party app's availability and embedding capability.
 */
@Serializable
enum class ExternalAppStatus {
    /** App is not installed on this device */
    NOT_INSTALLED,
    /** App is installed but does not allow activity embedding */
    INSTALLED_NO_EMBED,
    /** App is installed and opts in to activity embedding */
    EMBEDDABLE
}
