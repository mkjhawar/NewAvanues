/**
 * VoiceOSCoreNG.kt - Main facade for VoiceOSCoreNG library
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Central entry point for VoiceOSCoreNG functionality.
 * Manages initialization, tier configuration, feature gates, and version info.
 */
package com.augmentalis.voiceoscoreng.core

import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Main entry point and facade for VoiceOSCoreNG.
 *
 * This singleton provides:
 * - Library initialization with tier and debug configuration
 * - Tier management (LITE vs DEV modes)
 * - Feature gate checks
 * - Version information
 *
 * Usage:
 * ```kotlin
 * // Initialize once at app startup
 * VoiceOSCoreNG.initialize(
 *     tier = LearnAppDevToggle.Tier.DEV,
 *     isDebug = BuildConfig.DEBUG
 * )
 *
 * // Check features
 * if (VoiceOSCoreNG.isFeatureEnabled(Feature.DEBUG_OVERLAY)) {
 *     showDebugOverlay()
 * }
 * ```
 */
object VoiceOSCoreNG {

    private const val VERSION = "1.0.0"
    private const val VERSION_CODE = 1

    private val initialized = AtomicBoolean(false)

    @Volatile
    private var instance: VoiceOSCoreNG? = null

    /**
     * Get singleton instance.
     * Thread-safe lazy initialization.
     */
    fun getInstance(): VoiceOSCoreNG {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    instance = this
                }
            }
        }
        return instance!!
    }

    /**
     * Initialize VoiceOSCoreNG with tier and debug settings.
     *
     * Should be called once during application startup, typically in
     * Application.onCreate() or the main activity.
     *
     * @param tier The feature tier (LITE or DEV)
     * @param isDebug Whether debug mode is enabled
     */
    fun initialize(tier: LearnAppDevToggle.Tier, isDebug: Boolean) {
        if (initialized.compareAndSet(false, true)) {
            // Use LearnAppDevToggle.initialize to set both tier and debug
            LearnAppDevToggle.initialize(tier, isDebug)

            // Initialize type pattern registry for VUID generation
            com.augmentalis.voiceoscoreng.common.TypePatternRegistry.registerDefaults()

            // Register default framework handlers
            com.augmentalis.voiceoscoreng.handlers.FrameworkHandlerRegistry.registerDefaults()
        } else {
            // Already initialized, just update settings via re-initialize
            LearnAppDevToggle.initialize(tier, isDebug)
        }
    }

    /**
     * Check if VoiceOSCoreNG has been initialized.
     */
    fun isInitialized(): Boolean = initialized.get()

    /**
     * Check if running in DEV mode (full features).
     */
    fun isDevMode(): Boolean = LearnAppDevToggle.getCurrentTier() == LearnAppDevToggle.Tier.DEV

    /**
     * Check if running in LITE mode (limited features).
     */
    fun isLiteMode(): Boolean = LearnAppDevToggle.getCurrentTier() == LearnAppDevToggle.Tier.LITE

    /**
     * Set the current tier.
     *
     * @param tier The new tier to set
     */
    fun setTier(tier: LearnAppDevToggle.Tier) {
        LearnAppDevToggle.setTier(tier)
    }

    /**
     * Get the current tier.
     */
    fun getCurrentTier(): LearnAppDevToggle.Tier = LearnAppDevToggle.getCurrentTier()

    /**
     * Toggle between LITE and DEV tiers.
     * Useful for developer settings UI.
     */
    fun toggle() {
        LearnAppDevToggle.toggle()
    }

    /**
     * Check if a specific feature is enabled for the current tier.
     *
     * @param feature The feature to check
     * @return true if the feature is available
     */
    fun isFeatureEnabled(feature: LearnAppDevToggle.Feature): Boolean {
        return LearnAppDevToggle.isFeatureEnabled(feature)
    }

    /**
     * Get the library version string.
     * Format: "major.minor.patch"
     */
    fun getVersion(): String = VERSION

    /**
     * Get the library version code.
     * Increments with each release.
     */
    fun getVersionCode(): Int = VERSION_CODE

    /**
     * Reset to uninitialized state.
     * Primarily for testing purposes.
     */
    fun reset() {
        initialized.set(false)
        LearnAppDevToggle.reset()
    }

    /**
     * Get a summary of current configuration for debugging.
     */
    fun getConfigSummary(): String {
        return buildString {
            appendLine("VoiceOSCoreNG v$VERSION (code: $VERSION_CODE)")
            appendLine("Initialized: ${isInitialized()}")
            appendLine("Tier: ${getCurrentTier()}")
            appendLine("Debug: ${LearnAppDevToggle.isDebug()}")
            appendLine("Dev Mode: ${isDevMode()}")
            appendLine("Lite Mode: ${isLiteMode()}")
        }
    }
}
