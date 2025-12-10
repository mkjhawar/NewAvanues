package com.augmentalis.ava.core.domain.model

/**
 * App Resolution Models
 *
 * Platform-agnostic models for the Intelligent Resolution System.
 * Used by AppResolverService to communicate resolution results.
 *
 * Part of Intelligent Resolution System (Chapter 71)
 *
 * Author: Manoj Jhawar
 */

/**
 * Result of resolving an app for a capability.
 */
sealed class AppResolution {
    /**
     * App successfully resolved - ready to use.
     */
    data class Resolved(
        val packageName: String,
        val appName: String,
        val source: ResolutionSource
    ) : AppResolution()

    /**
     * Multiple apps available - UI should prompt user to choose.
     */
    data class MultipleAvailable(
        val capability: String,
        val capabilityDisplayName: String,
        val apps: List<InstalledApp>,
        val recommendedIndex: Int = 0
    ) : AppResolution()

    /**
     * No apps available for this capability.
     */
    data class NoneAvailable(
        val capability: String,
        val suggestedApps: List<KnownApp>
    ) : AppResolution()

    /**
     * Unknown capability requested.
     */
    data class UnknownCapability(val capability: String) : AppResolution()
}

/**
 * How the app was resolved.
 */
enum class ResolutionSource {
    /** User explicitly chose this app */
    USER_PREFERENCE,
    /** Only one app available, auto-selected */
    AUTO_DETECTED,
    /** Learned from usage patterns */
    USAGE_PATTERN
}

/**
 * An app installed on the device.
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val iconUri: String? = null
)

/**
 * A known app that can handle a capability.
 * Used for suggestions when no apps are installed.
 */
data class KnownApp(
    val packageName: String,
    val displayName: String,
    val platform: AppPlatform,
    val playStoreUrl: String? = null
)

/**
 * Platform an app is available on.
 */
enum class AppPlatform {
    ANDROID,
    IOS,
    BOTH
}

/**
 * Stored app preference.
 */
data class AppPreference(
    val capability: String,
    val packageName: String,
    val appName: String,
    val setAt: Long,
    val setBy: String
)

/**
 * Capability with current preference status.
 * Used in Settings screen.
 */
data class CapabilityPreference(
    val capability: String,
    val displayName: String,
    val selectedApp: InstalledApp?,
    val availableApps: List<InstalledApp>,
    val canChange: Boolean
)
