package com.augmentalis.voiceoscore

/**
 * Configuration for LearnApp feature variants.
 *
 * Defines the feature sets for Lite and Dev versions of LearnApp,
 * allowing dynamic switching between modes.
 *
 * All limits are developer-configurable via [DeveloperSettings].
 */
object LearnAppConfig {

    /**
     * Processing mode for element learning.
     */
    enum class ProcessingMode {
        /** Process elements immediately as they are encountered */
        IMMEDIATE,
        /** Collect elements and process in batches */
        BATCH,
        /** Hybrid: immediate for common, batch for complex */
        HYBRID
    }

    /**
     * LearnApp variant configuration.
     * All values are mutable to support developer overrides.
     */
    data class VariantConfig(
        val name: String,
        val tier: LearnAppDevToggle.Tier,
        var processingMode: ProcessingMode,
        var maxElementsPerScan: Int,
        var maxAppsLearned: Int,
        var enableAI: Boolean,
        var enableNLU: Boolean,
        var enableExploration: Boolean,
        var enableFrameworkDetection: Boolean,
        var cacheEnabled: Boolean,
        var analyticsEnabled: Boolean,
        var batchTimeoutMs: Long = 5000L,
        var explorationDepth: Int = 10,
        var enableDebugOverlay: Boolean = false
    ) {
        /**
         * Create a copy with all current values.
         */
        fun snapshot(): VariantConfig = copy()
    }

    /**
     * Developer settings for runtime configuration.
     * These override the tier defaults when enabled.
     */
    object DeveloperSettings {
        /** Enable developer mode to allow overrides */
        var enabled: Boolean = false
            private set

        /** Custom element limit (null = use tier default) */
        var maxElementsPerScan: Int? = null

        /** Custom app limit (null = use tier default) */
        var maxAppsLearned: Int? = null

        /** Force enable AI regardless of tier */
        var forceEnableAI: Boolean = false

        /** Force enable NLU regardless of tier */
        var forceEnableNLU: Boolean = false

        /** Force enable exploration regardless of tier */
        var forceEnableExploration: Boolean = false

        /** Force enable framework detection */
        var forceEnableFrameworkDetection: Boolean = false

        /** Force enable caching */
        var forceEnableCaching: Boolean = false

        /** Force enable analytics */
        var forceEnableAnalytics: Boolean = false

        /** Custom batch timeout in milliseconds */
        var batchTimeoutMs: Long? = null

        /** Custom exploration depth */
        var explorationDepth: Int? = null

        /** Enable debug overlay */
        var enableDebugOverlay: Boolean = false

        /** Processing mode override */
        var processingModeOverride: ProcessingMode? = null

        /**
         * Enable developer mode with optional full unlock.
         * @param unlockAll If true, enables all features regardless of tier.
         */
        fun enable(unlockAll: Boolean = false) {
            enabled = true
            if (unlockAll) {
                forceEnableAI = true
                forceEnableNLU = true
                forceEnableExploration = true
                forceEnableFrameworkDetection = true
                forceEnableCaching = true
                forceEnableAnalytics = true
                enableDebugOverlay = true
                maxElementsPerScan = UNLIMITED
                maxAppsLearned = UNLIMITED
            }
        }

        /**
         * Disable developer mode and reset all overrides.
         */
        fun disable() {
            enabled = false
            reset()
        }

        /**
         * Reset all overrides to defaults (keeps enabled state).
         */
        fun reset() {
            maxElementsPerScan = null
            maxAppsLearned = null
            forceEnableAI = false
            forceEnableNLU = false
            forceEnableExploration = false
            forceEnableFrameworkDetection = false
            forceEnableCaching = false
            forceEnableAnalytics = false
            batchTimeoutMs = null
            explorationDepth = null
            enableDebugOverlay = false
            processingModeOverride = null
        }

        /**
         * Get a summary of current developer settings.
         */
        fun getSummary(): String = buildString {
            append("Developer Mode: ${if (enabled) "ENABLED" else "DISABLED"}\n")
            if (enabled) {
                append("Overrides:\n")
                maxElementsPerScan?.let { append("  - Max Elements: $it\n") }
                maxAppsLearned?.let { append("  - Max Apps: ${if (it == UNLIMITED) "Unlimited" else it}\n") }
                if (forceEnableAI) append("  - AI: FORCED ON\n")
                if (forceEnableNLU) append("  - NLU: FORCED ON\n")
                if (forceEnableExploration) append("  - Exploration: FORCED ON\n")
                if (forceEnableFrameworkDetection) append("  - Framework Detection: FORCED ON\n")
                if (forceEnableCaching) append("  - Caching: FORCED ON\n")
                if (forceEnableAnalytics) append("  - Analytics: FORCED ON\n")
                if (enableDebugOverlay) append("  - Debug Overlay: ENABLED\n")
                batchTimeoutMs?.let { append("  - Batch Timeout: ${it}ms\n") }
                explorationDepth?.let { append("  - Exploration Depth: $it\n") }
                processingModeOverride?.let { append("  - Processing Mode: $it\n") }
            }
        }
    }

    /** Constant for unlimited (-1) */
    const val UNLIMITED = -1

    /** Default Lite limits */
    object LiteDefaults {
        const val MAX_ELEMENTS_PER_SCAN = 100
        const val MAX_APPS_LEARNED = 25
        const val BATCH_TIMEOUT_MS = 3000L
        const val EXPLORATION_DEPTH = 5
    }

    /** Default Dev limits */
    object DevDefaults {
        const val MAX_ELEMENTS_PER_SCAN = 500
        const val MAX_APPS_LEARNED = UNLIMITED
        const val BATCH_TIMEOUT_MS = 5000L
        const val EXPLORATION_DEPTH = 20
    }

    /**
     * Lite variant configuration - includes NLU/AI for basic voice commands.
     * Limits are developer-configurable defaults.
     */
    private val LITE_BASE_CONFIG = VariantConfig(
        name = "LearnApp Lite",
        tier = LearnAppDevToggle.Tier.LITE,
        processingMode = ProcessingMode.IMMEDIATE,
        maxElementsPerScan = LiteDefaults.MAX_ELEMENTS_PER_SCAN,
        maxAppsLearned = LiteDefaults.MAX_APPS_LEARNED,
        enableAI = true,  // Now enabled for Lite
        enableNLU = true, // NLU enabled for voice commands
        enableExploration = false,
        enableFrameworkDetection = false,
        cacheEnabled = true, // Basic caching for performance
        analyticsEnabled = false,
        batchTimeoutMs = LiteDefaults.BATCH_TIMEOUT_MS,
        explorationDepth = LiteDefaults.EXPLORATION_DEPTH
    )

    /**
     * Dev variant configuration - full features for developers/subscribers.
     */
    private val DEV_BASE_CONFIG = VariantConfig(
        name = "LearnApp Dev",
        tier = LearnAppDevToggle.Tier.DEV,
        processingMode = ProcessingMode.HYBRID,
        maxElementsPerScan = DevDefaults.MAX_ELEMENTS_PER_SCAN,
        maxAppsLearned = DevDefaults.MAX_APPS_LEARNED,
        enableAI = true,
        enableNLU = true,
        enableExploration = true,
        enableFrameworkDetection = true,
        cacheEnabled = true,
        analyticsEnabled = true,
        batchTimeoutMs = DevDefaults.BATCH_TIMEOUT_MS,
        explorationDepth = DevDefaults.EXPLORATION_DEPTH
    )

    /**
     * Current active configuration (with developer overrides applied).
     */
    private var currentConfig: VariantConfig = LITE_BASE_CONFIG.snapshot()

    /**
     * Configuration change listeners.
     */
    private val configChangeListeners = mutableListOf<(VariantConfig) -> Unit>()

    /**
     * Get the current configuration with developer overrides applied.
     */
    fun getConfig(): VariantConfig {
        applyDeveloperOverrides()
        return currentConfig
    }

    /**
     * Get the effective configuration (alias for getConfig).
     */
    fun getEffectiveConfig(): VariantConfig = getConfig()

    /**
     * Apply developer setting overrides to current config.
     */
    private fun applyDeveloperOverrides() {
        if (!DeveloperSettings.enabled) return

        with(DeveloperSettings) {
            maxElementsPerScan?.let { currentConfig.maxElementsPerScan = it }
            maxAppsLearned?.let { currentConfig.maxAppsLearned = it }
            if (forceEnableAI) currentConfig.enableAI = true
            if (forceEnableNLU) currentConfig.enableNLU = true
            if (forceEnableExploration) currentConfig.enableExploration = true
            if (forceEnableFrameworkDetection) currentConfig.enableFrameworkDetection = true
            if (forceEnableCaching) currentConfig.cacheEnabled = true
            if (forceEnableAnalytics) currentConfig.analyticsEnabled = true
            batchTimeoutMs?.let { currentConfig.batchTimeoutMs = it }
            explorationDepth?.let { currentConfig.explorationDepth = it }
            currentConfig.enableDebugOverlay = enableDebugOverlay
            processingModeOverride?.let { currentConfig.processingMode = it }
        }
    }

    /**
     * Set the active variant.
     */
    fun setVariant(tier: LearnAppDevToggle.Tier) {
        val baseConfig = when (tier) {
            LearnAppDevToggle.Tier.LITE -> LITE_BASE_CONFIG
            LearnAppDevToggle.Tier.DEV -> DEV_BASE_CONFIG
        }

        currentConfig = baseConfig.snapshot()
        applyDeveloperOverrides()
        LearnAppDevToggle.setTier(tier)
        configChangeListeners.forEach { it(currentConfig) }
    }

    /**
     * Apply a custom configuration (for testing or advanced use).
     */
    fun applyCustomConfig(config: VariantConfig) {
        currentConfig = config.snapshot()
        applyDeveloperOverrides()
        configChangeListeners.forEach { it(currentConfig) }
    }

    /**
     * Check if currently using Lite variant.
     */
    fun isLite(): Boolean = currentConfig.tier == LearnAppDevToggle.Tier.LITE

    /**
     * Check if currently using Dev variant.
     */
    fun isDev(): Boolean = currentConfig.tier == LearnAppDevToggle.Tier.DEV

    /**
     * Add a listener for configuration changes.
     */
    fun addConfigChangeListener(listener: (VariantConfig) -> Unit) {
        configChangeListeners.add(listener)
    }

    /**
     * Remove a configuration change listener.
     */
    fun removeConfigChangeListener(listener: (VariantConfig) -> Unit) {
        configChangeListeners.remove(listener)
    }

    /**
     * Get the appropriate processing mode.
     */
    fun getProcessingMode(): ProcessingMode = getConfig().processingMode

    /**
     * Get max elements per scan.
     */
    fun getMaxElementsPerScan(): Int = getConfig().maxElementsPerScan

    /**
     * Get max apps that can be learned.
     */
    fun getMaxAppsLearned(): Int = getConfig().maxAppsLearned

    /**
     * Check if AI features are enabled.
     */
    fun isAIEnabled(): Boolean = getConfig().enableAI

    /**
     * Check if NLU features are enabled.
     */
    fun isNLUEnabled(): Boolean = getConfig().enableNLU

    /**
     * Check if exploration mode is enabled.
     */
    fun isExplorationEnabled(): Boolean = getConfig().enableExploration

    /**
     * Check if framework detection is enabled.
     */
    fun isFrameworkDetectionEnabled(): Boolean = getConfig().enableFrameworkDetection

    /**
     * Check if caching is enabled.
     */
    fun isCacheEnabled(): Boolean = getConfig().cacheEnabled

    /**
     * Check if analytics is enabled.
     */
    fun isAnalyticsEnabled(): Boolean = getConfig().analyticsEnabled

    /**
     * Check if debug overlay is enabled.
     */
    fun isDebugOverlayEnabled(): Boolean = getConfig().enableDebugOverlay

    /**
     * Get batch timeout in milliseconds.
     */
    fun getBatchTimeoutMs(): Long = getConfig().batchTimeoutMs

    /**
     * Get exploration depth.
     */
    fun getExplorationDepth(): Int = getConfig().explorationDepth

    /**
     * Get a summary of the current configuration.
     */
    fun getSummary(): String {
        val config = getConfig()
        return buildString {
            append("═══════════════════════════════════════\n")
            append("  LearnApp Configuration Summary\n")
            append("═══════════════════════════════════════\n")
            append("Variant: ${config.name}\n")
            append("Tier: ${config.tier}\n")
            append("Developer Mode: ${if (DeveloperSettings.enabled) "ENABLED" else "disabled"}\n")
            append("───────────────────────────────────────\n")
            append("Processing: ${config.processingMode}\n")
            append("Max Elements: ${if (config.maxElementsPerScan == UNLIMITED) "Unlimited" else config.maxElementsPerScan}\n")
            append("Max Apps: ${if (config.maxAppsLearned == UNLIMITED) "Unlimited" else config.maxAppsLearned}\n")
            append("Batch Timeout: ${config.batchTimeoutMs}ms\n")
            append("Exploration Depth: ${config.explorationDepth}\n")
            append("───────────────────────────────────────\n")
            append("Features:\n")
            append("  AI: ${if (config.enableAI) "✓" else "✗"}\n")
            append("  NLU: ${if (config.enableNLU) "✓" else "✗"}\n")
            append("  Exploration: ${if (config.enableExploration) "✓" else "✗"}\n")
            append("  Framework Detection: ${if (config.enableFrameworkDetection) "✓" else "✗"}\n")
            append("  Caching: ${if (config.cacheEnabled) "✓" else "✗"}\n")
            append("  Analytics: ${if (config.analyticsEnabled) "✓" else "✗"}\n")
            append("  Debug Overlay: ${if (config.enableDebugOverlay) "✓" else "✗"}\n")
            append("═══════════════════════════════════════")
        }
    }

    /**
     * Reset to default configuration (for testing).
     */
    fun reset() {
        currentConfig = LITE_BASE_CONFIG.snapshot()
        DeveloperSettings.disable()
        configChangeListeners.clear()
    }

    /**
     * Enable full test mode - unlocks all features for testing.
     */
    fun enableTestMode() {
        DeveloperSettings.enable(unlockAll = true)
        applyDeveloperOverrides()
        configChangeListeners.forEach { it(currentConfig) }
    }
}
