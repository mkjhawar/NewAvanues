package com.augmentalis.voiceoscoreng.core

import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle

/**
 * Main entry point for VoiceOSCoreNG on Android.
 *
 * Provides initialization and configuration for the VoiceOS core functionality
 * including LearnApp Lite and Dev features.
 *
 * Usage:
 * ```kotlin
 * // In Application.onCreate()
 * VoiceOSCoreNG.initialize(
 *     tier = LearnAppDevToggle.Tier.LITE,
 *     isDebug = BuildConfig.DEBUG
 * )
 * ```
 */
object VoiceOSCoreNG {

    private const val VERSION = "1.0.0"
    private const val VERSION_CODE = 1

    private var initialized = false

    /**
     * Initialize VoiceOSCoreNG with the specified configuration.
     *
     * @param tier The feature tier (LITE or DEV)
     * @param isDebug Whether this is a debug build
     */
    fun initialize(
        tier: LearnAppDevToggle.Tier,
        isDebug: Boolean
    ) {
        LearnAppDevToggle.initialize(tier, isDebug)
        initialized = true
    }

    /**
     * Check if VoiceOSCoreNG has been initialized.
     */
    fun isInitialized(): Boolean = initialized

    /**
     * Get the singleton instance (for Java interop).
     */
    fun getInstance(): VoiceOSCoreNG = this

    /**
     * Check if currently in Dev mode.
     */
    fun isDevMode(): Boolean = LearnAppDevToggle.isDevMode()

    /**
     * Check if currently in Lite mode.
     */
    fun isLiteMode(): Boolean = LearnAppDevToggle.isLiteMode()

    /**
     * Get the current tier.
     */
    fun getCurrentTier(): LearnAppDevToggle.Tier = LearnAppDevToggle.getCurrentTier()

    /**
     * Set the current tier.
     */
    fun setTier(tier: LearnAppDevToggle.Tier) {
        LearnAppDevToggle.setTier(tier)
    }

    /**
     * Toggle between Lite and Dev modes.
     */
    fun toggle() {
        LearnAppDevToggle.toggle()
    }

    /**
     * Check if a specific feature is enabled.
     */
    fun isFeatureEnabled(feature: LearnAppDevToggle.Feature): Boolean {
        return LearnAppDevToggle.isEnabled(feature)
    }

    /**
     * Get the VoiceOSCoreNG version string.
     */
    fun getVersion(): String = VERSION

    /**
     * Get the VoiceOSCoreNG version code.
     */
    fun getVersionCode(): Int = VERSION_CODE

    /**
     * Reset VoiceOSCoreNG state (for testing).
     */
    fun reset() {
        initialized = false
        LearnAppDevToggle.reset()
    }
}
