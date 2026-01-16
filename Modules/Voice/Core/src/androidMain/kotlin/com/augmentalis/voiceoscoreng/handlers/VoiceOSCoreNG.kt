package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.features.LearnAppConfig
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
 *
 * // Enable test mode (unlocks all features)
 * VoiceOSCoreNG.enableTestMode()
 *
 * // Configure custom limits
 * VoiceOSCoreNG.configureLimits(
 *     maxElementsPerScan = 200,
 *     maxAppsLearned = 50
 * )
 * ```
 */
object VoiceOSCoreNG {

    private const val VERSION = "2.0.0"
    private const val VERSION_CODE = 2

    private var initialized = false

    /**
     * Initialize VoiceOSCoreNG with the specified configuration.
     *
     * @param tier The feature tier (LITE or DEV)
     * @param isDebug Whether this is a debug build
     * @param enableTestMode If true, enables test mode immediately (default: false in production)
     */
    fun initialize(
        tier: LearnAppDevToggle.Tier,
        isDebug: Boolean,
        enableTestMode: Boolean = false
    ) {
        LearnAppDevToggle.initialize(tier, isDebug)
        LearnAppConfig.setVariant(tier)

        if (enableTestMode || isDebug) {
            // Auto-enable developer mode in debug builds
            LearnAppConfig.DeveloperSettings.enable(unlockAll = enableTestMode)
        }

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
     * Check if test mode is enabled.
     */
    fun isTestModeEnabled(): Boolean = LearnAppConfig.DeveloperSettings.enabled

    /**
     * Get the current tier.
     */
    fun getCurrentTier(): LearnAppDevToggle.Tier = LearnAppDevToggle.getCurrentTier()

    /**
     * Set the current tier.
     */
    fun setTier(tier: LearnAppDevToggle.Tier) {
        LearnAppDevToggle.setTier(tier)
        LearnAppConfig.setVariant(tier)
    }

    /**
     * Toggle between Lite and Dev modes.
     */
    fun toggle() {
        LearnAppDevToggle.toggle()
        LearnAppConfig.setVariant(LearnAppDevToggle.getCurrentTier())
    }

    /**
     * Enable test mode - unlocks all features regardless of tier.
     * This is primarily for development and testing purposes.
     */
    fun enableTestMode() {
        LearnAppConfig.enableTestMode()
    }

    /**
     * Disable test mode and reset to tier defaults.
     */
    fun disableTestMode() {
        LearnAppConfig.DeveloperSettings.disable()
        LearnAppConfig.setVariant(getCurrentTier())
    }

    /**
     * Configure custom limits.
     * Enables developer mode if not already enabled.
     *
     * @param maxElementsPerScan Max elements to capture per scan (-1 for unlimited)
     * @param maxAppsLearned Max apps that can be learned (-1 for unlimited)
     * @param batchTimeoutMs Batch processing timeout in milliseconds
     * @param explorationDepth Maximum depth for exploration
     */
    fun configureLimits(
        maxElementsPerScan: Int? = null,
        maxAppsLearned: Int? = null,
        batchTimeoutMs: Long? = null,
        explorationDepth: Int? = null
    ) {
        if (!LearnAppConfig.DeveloperSettings.enabled) {
            LearnAppConfig.DeveloperSettings.enable(unlockAll = false)
        }

        maxElementsPerScan?.let { LearnAppConfig.DeveloperSettings.maxElementsPerScan = it }
        maxAppsLearned?.let { LearnAppConfig.DeveloperSettings.maxAppsLearned = it }
        batchTimeoutMs?.let { LearnAppConfig.DeveloperSettings.batchTimeoutMs = it }
        explorationDepth?.let { LearnAppConfig.DeveloperSettings.explorationDepth = it }
    }

    /**
     * Enable or disable specific features.
     * Enables developer mode if not already enabled.
     */
    fun configureFeatures(
        enableAI: Boolean? = null,
        enableNLU: Boolean? = null,
        enableExploration: Boolean? = null,
        enableFrameworkDetection: Boolean? = null,
        enableCaching: Boolean? = null,
        enableAnalytics: Boolean? = null,
        enableDebugOverlay: Boolean? = null
    ) {
        if (!LearnAppConfig.DeveloperSettings.enabled) {
            LearnAppConfig.DeveloperSettings.enable(unlockAll = false)
        }

        enableAI?.let { LearnAppConfig.DeveloperSettings.forceEnableAI = it }
        enableNLU?.let { LearnAppConfig.DeveloperSettings.forceEnableNLU = it }
        enableExploration?.let { LearnAppConfig.DeveloperSettings.forceEnableExploration = it }
        enableFrameworkDetection?.let { LearnAppConfig.DeveloperSettings.forceEnableFrameworkDetection = it }
        enableCaching?.let { LearnAppConfig.DeveloperSettings.forceEnableCaching = it }
        enableAnalytics?.let { LearnAppConfig.DeveloperSettings.forceEnableAnalytics = it }
        enableDebugOverlay?.let { LearnAppConfig.DeveloperSettings.enableDebugOverlay = it }
    }

    /**
     * Set processing mode override.
     *
     * @param mode The processing mode to use (null to use tier default)
     */
    fun setProcessingMode(mode: LearnAppConfig.ProcessingMode?) {
        if (!LearnAppConfig.DeveloperSettings.enabled && mode != null) {
            LearnAppConfig.DeveloperSettings.enable(unlockAll = false)
        }
        LearnAppConfig.DeveloperSettings.processingModeOverride = mode
    }

    /**
     * Check if a specific feature is enabled.
     */
    fun isFeatureEnabled(feature: LearnAppDevToggle.Feature): Boolean {
        return LearnAppDevToggle.isEnabled(feature)
    }

    /**
     * Check if AI features are enabled.
     */
    fun isAIEnabled(): Boolean = LearnAppConfig.isAIEnabled()

    /**
     * Check if NLU features are enabled.
     */
    fun isNLUEnabled(): Boolean = LearnAppConfig.isNLUEnabled()

    /**
     * Check if exploration is enabled.
     */
    fun isExplorationEnabled(): Boolean = LearnAppConfig.isExplorationEnabled()

    /**
     * Get current configuration summary.
     */
    fun getConfigSummary(): String = LearnAppConfig.getSummary()

    /**
     * Get the current configuration.
     */
    fun getConfig(): LearnAppConfig.VariantConfig = LearnAppConfig.getConfig()

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
        LearnAppConfig.reset()
    }
}
