package com.augmentalis.voiceoscoreng.features

/**
 * Configuration for LearnApp feature variants.
 *
 * Defines the feature sets for Lite and Dev versions of LearnApp,
 * allowing dynamic switching between modes.
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
     */
    data class VariantConfig(
        val name: String,
        val tier: LearnAppDevToggle.Tier,
        val processingMode: ProcessingMode,
        val maxElementsPerScan: Int,
        val maxAppsLearned: Int,
        val enableAI: Boolean,
        val enableExploration: Boolean,
        val enableFrameworkDetection: Boolean,
        val cacheEnabled: Boolean,
        val analyticsEnabled: Boolean
    )

    /**
     * Lite variant configuration - basic features, no subscription required.
     */
    val LITE_CONFIG = VariantConfig(
        name = "LearnApp Lite",
        tier = LearnAppDevToggle.Tier.LITE,
        processingMode = ProcessingMode.IMMEDIATE,
        maxElementsPerScan = 50,
        maxAppsLearned = 10,
        enableAI = false,
        enableExploration = false,
        enableFrameworkDetection = false,
        cacheEnabled = false,
        analyticsEnabled = false
    )

    /**
     * Dev variant configuration - full features for developers/subscribers.
     */
    val DEV_CONFIG = VariantConfig(
        name = "LearnApp Dev",
        tier = LearnAppDevToggle.Tier.DEV,
        processingMode = ProcessingMode.HYBRID,
        maxElementsPerScan = 500,
        maxAppsLearned = -1, // Unlimited
        enableAI = true,
        enableExploration = true,
        enableFrameworkDetection = true,
        cacheEnabled = true,
        analyticsEnabled = true
    )

    /**
     * Current active configuration.
     */
    private var currentConfig: VariantConfig = LITE_CONFIG

    /**
     * Configuration change listeners.
     */
    private val configChangeListeners = mutableListOf<(VariantConfig) -> Unit>()

    /**
     * Get the current configuration.
     */
    fun getConfig(): VariantConfig = currentConfig

    /**
     * Set the active variant.
     */
    fun setVariant(tier: LearnAppDevToggle.Tier) {
        val newConfig = when (tier) {
            LearnAppDevToggle.Tier.LITE -> LITE_CONFIG
            LearnAppDevToggle.Tier.DEV -> DEV_CONFIG
        }

        if (currentConfig != newConfig) {
            currentConfig = newConfig
            LearnAppDevToggle.setTier(tier)
            configChangeListeners.forEach { it(newConfig) }
        }
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
    fun getProcessingMode(): ProcessingMode = currentConfig.processingMode

    /**
     * Get max elements per scan.
     */
    fun getMaxElementsPerScan(): Int = currentConfig.maxElementsPerScan

    /**
     * Get max apps that can be learned.
     */
    fun getMaxAppsLearned(): Int = currentConfig.maxAppsLearned

    /**
     * Check if AI features are enabled.
     */
    fun isAIEnabled(): Boolean = currentConfig.enableAI

    /**
     * Check if exploration mode is enabled.
     */
    fun isExplorationEnabled(): Boolean = currentConfig.enableExploration

    /**
     * Check if framework detection is enabled.
     */
    fun isFrameworkDetectionEnabled(): Boolean = currentConfig.enableFrameworkDetection

    /**
     * Check if caching is enabled.
     */
    fun isCacheEnabled(): Boolean = currentConfig.cacheEnabled

    /**
     * Check if analytics is enabled.
     */
    fun isAnalyticsEnabled(): Boolean = currentConfig.analyticsEnabled

    /**
     * Get a summary of the current configuration.
     */
    fun getSummary(): String {
        return buildString {
            append("Variant: ${currentConfig.name}\n")
            append("Tier: ${currentConfig.tier}\n")
            append("Processing: ${currentConfig.processingMode}\n")
            append("Max Elements: ${currentConfig.maxElementsPerScan}\n")
            append("Max Apps: ${if (currentConfig.maxAppsLearned < 0) "Unlimited" else currentConfig.maxAppsLearned}\n")
            append("AI: ${if (currentConfig.enableAI) "Enabled" else "Disabled"}\n")
            append("Exploration: ${if (currentConfig.enableExploration) "Enabled" else "Disabled"}\n")
            append("Framework Detection: ${if (currentConfig.enableFrameworkDetection) "Enabled" else "Disabled"}")
        }
    }

    /**
     * Reset to default configuration (for testing).
     */
    fun reset() {
        currentConfig = LITE_CONFIG
        configChangeListeners.clear()
    }
}
