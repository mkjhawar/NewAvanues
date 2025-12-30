package com.augmentalis.magicelements.core.mel

import com.augmentalis.magicelements.core.mel.functions.PluginTier

/**
 * Platform detection for plugin tier enforcement.
 *
 * Detects the current platform and determines the maximum allowed plugin tier.
 * Apple platforms (iOS, macOS, visionOS) are restricted to Tier 1 (DATA) for
 * App Store compliance, while other platforms support Tier 2 (LOGIC).
 *
 * Usage:
 * ```kotlin
 * val platform = TierDetector.detectPlatform()
 * val maxTier = TierDetector.getMaxTier(platform)
 * if (TierDetector.shouldDowngrade(PluginTier.LOGIC, platform)) {
 *     println("Plugin will be downgraded to Tier 1 on this platform")
 * }
 * ```
 */
object TierDetector {
    /**
     * Detect the current platform.
     *
     * Uses platform-specific implementation via expect/actual.
     *
     * @return Detected platform
     */
    fun detectPlatform(): Platform {
        return getCurrentPlatform()
    }

    /**
     * Get the maximum allowed plugin tier for a platform.
     *
     * @param platform Target platform
     * @return Maximum tier allowed (DATA for Apple, LOGIC for others)
     */
    fun getMaxTier(platform: Platform): PluginTier {
        return when (platform) {
            Platform.IOS,
            Platform.MACOS,
            Platform.VISIONOS -> PluginTier.DATA  // Apple platforms: Tier 1 only

            Platform.ANDROID,
            Platform.ANDROID_XR,
            Platform.JVM,
            Platform.WEB,
            Platform.WINDOWS,
            Platform.LINUX,
            Platform.UNKNOWN -> PluginTier.LOGIC  // Non-Apple: Tier 2 supported
        }
    }

    /**
     * Check if a plugin should be downgraded on a platform.
     *
     * @param requestedTier Tier requested by the plugin
     * @param platform Target platform
     * @return true if downgrade is needed
     */
    fun shouldDowngrade(requestedTier: PluginTier, platform: Platform): Boolean {
        val maxTier = getMaxTier(platform)
        return requestedTier == PluginTier.LOGIC && maxTier == PluginTier.DATA
    }

    /**
     * Get effective tier after platform-specific downgrade.
     *
     * @param requestedTier Tier requested by the plugin
     * @param platform Target platform
     * @return Effective tier (may be downgraded from requested)
     */
    fun getEffectiveTier(requestedTier: PluginTier, platform: Platform): PluginTier {
        val maxTier = getMaxTier(platform)
        return if (requestedTier == PluginTier.LOGIC && maxTier == PluginTier.DATA) {
            PluginTier.DATA
        } else {
            requestedTier
        }
    }

    /**
     * Check if platform is an Apple platform.
     */
    fun isApplePlatform(platform: Platform): Boolean {
        return platform in setOf(Platform.IOS, Platform.MACOS, Platform.VISIONOS)
    }

    /**
     * Get platform display name.
     */
    fun getPlatformName(platform: Platform): String {
        return when (platform) {
            Platform.IOS -> "iOS"
            Platform.MACOS -> "macOS"
            Platform.VISIONOS -> "visionOS"
            Platform.ANDROID -> "Android"
            Platform.ANDROID_XR -> "Android XR"
            Platform.JVM -> "JVM"
            Platform.WEB -> "Web"
            Platform.WINDOWS -> "Windows"
            Platform.LINUX -> "Linux"
            Platform.UNKNOWN -> "Unknown"
        }
    }

    /**
     * Get downgrade warning message.
     */
    fun getDowngradeWarning(platform: Platform): String {
        return """
            Plugin tier downgraded to Tier 1 (DATA) on ${getPlatformName(platform)}.

            Tier 2 features (scripts, extended APIs) will be disabled:
            - No custom scripts
            - No HTTP requests
            - No storage operations
            - No navigation APIs
            - No clipboard access
            - No haptic feedback

            Only declarative templates with whitelisted functions are allowed.
            This is required for Apple App Store compliance.
        """.trimIndent()
    }
}

/**
 * Platform enumeration.
 *
 * Covers all supported platforms with explicit Apple vs non-Apple distinction
 * for tier enforcement.
 */
enum class Platform {
    /** iOS (Apple - Tier 1 only) */
    IOS,

    /** macOS (Apple - Tier 1 only) */
    MACOS,

    /** visionOS (Apple - Tier 1 only) */
    VISIONOS,

    /** Android (Google - Tier 2 supported) */
    ANDROID,

    /** Android XR (Google - Tier 2 supported) */
    ANDROID_XR,

    /** JVM Desktop (Tier 2 supported) */
    JVM,

    /** Web Browser (Tier 2 supported) */
    WEB,

    /** Windows (Tier 2 supported) */
    WINDOWS,

    /** Linux (Tier 2 supported) */
    LINUX,

    /** Unknown platform (defaults to Tier 2) */
    UNKNOWN;

    /**
     * Check if this is an Apple platform.
     */
    val isApple: Boolean
        get() = this in setOf(IOS, MACOS, VISIONOS)

    /**
     * Get max tier for this platform.
     */
    val maxTier: PluginTier
        get() = if (isApple) PluginTier.DATA else PluginTier.LOGIC
}

/**
 * Platform-specific implementation of platform detection.
 *
 * Each platform module must provide an actual implementation.
 */
expect fun getCurrentPlatform(): Platform
