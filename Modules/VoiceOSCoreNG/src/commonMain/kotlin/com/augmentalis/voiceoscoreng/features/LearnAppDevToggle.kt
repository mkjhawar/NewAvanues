package com.augmentalis.voiceoscoreng.features

/**
 * LearnApp Dev Toggle - Feature flag system for VoiceOSCoreNG.
 *
 * Provides a centralized way to enable/disable features based on:
 * - Build type (Debug vs Release)
 * - Feature tier (Lite vs Dev)
 * - System settings
 * - Remote configuration
 *
 * Features can be gated for:
 * - Paywall preparation (features behind subscription)
 * - A/B testing
 * - Staged rollouts
 * - Developer-only features
 */
object LearnAppDevToggle {

    /**
     * Feature tier levels.
     */
    enum class Tier {
        /** Free tier with basic features */
        LITE,
        /** Full feature set for developers/subscribers */
        DEV
    }

    /**
     * Feature categories.
     */
    enum class Category {
        /** Core scraping and VUID generation */
        CORE,
        /** JIT (Just-In-Time) learning */
        JIT,
        /** Exploration/batch learning */
        EXPLORATION,
        /** AI-powered features */
        AI,
        /** Developer tools and debugging */
        DEV_TOOLS,
        /** Analytics and metrics */
        ANALYTICS,
        /** Experimental features */
        EXPERIMENTAL
    }

    /**
     * Individual feature flags.
     */
    enum class Feature(
        val tier: Tier,
        val category: Category,
        val description: String,
        val defaultEnabled: Boolean = true
    ) {
        // ==================== Core Features (LITE) ====================

        /** Basic element scraping */
        ELEMENT_SCRAPING(Tier.LITE, Category.CORE, "Basic UI element scraping"),

        /** VUID generation */
        VUID_GENERATION(Tier.LITE, Category.CORE, "Generate Voice Unique Identifiers"),

        /** Native app detection */
        NATIVE_DETECTION(Tier.LITE, Category.CORE, "Detect native Android/iOS apps"),

        /** Basic voice command execution */
        VOICE_COMMANDS(Tier.LITE, Category.CORE, "Execute basic voice commands"),

        // ==================== JIT Learning (LITE) ====================

        /** JIT element processing */
        JIT_PROCESSING(Tier.LITE, Category.JIT, "Just-in-time element processing"),

        /** Immediate command generation */
        JIT_COMMANDS(Tier.LITE, Category.JIT, "Generate commands on demand"),

        // ==================== Exploration (DEV) ====================

        /** Full app exploration mode */
        EXPLORATION_MODE(Tier.DEV, Category.EXPLORATION, "Full app exploration and learning"),

        /** Batch element processing */
        BATCH_PROCESSING(Tier.DEV, Category.EXPLORATION, "Process elements in batches"),

        /** Screen state caching */
        SCREEN_CACHING(Tier.DEV, Category.EXPLORATION, "Cache screen states for analysis"),

        // ==================== Framework Detection (DEV) ====================

        /** Flutter app detection and handling */
        FLUTTER_DETECTION(Tier.DEV, Category.CORE, "Detect and handle Flutter apps"),

        /** Unity game detection and handling */
        UNITY_DETECTION(Tier.DEV, Category.CORE, "Detect and handle Unity games"),

        /** React Native detection */
        REACT_NATIVE_DETECTION(Tier.DEV, Category.CORE, "Detect and handle React Native apps"),

        /** WebView content handling */
        WEBVIEW_HANDLING(Tier.DEV, Category.CORE, "Handle WebView content"),

        // ==================== AI Features (DEV) ====================

        /** AI-powered element classification */
        AI_CLASSIFICATION(Tier.DEV, Category.AI, "AI-powered element type classification"),

        /** Semantic element naming */
        AI_NAMING(Tier.DEV, Category.AI, "AI-generated element names"),

        /** Context-aware command suggestions */
        AI_SUGGESTIONS(Tier.DEV, Category.AI, "AI command suggestions"),

        // ==================== Developer Tools (DEV) ====================

        /** Debug overlay */
        DEBUG_OVERLAY(Tier.DEV, Category.DEV_TOOLS, "Visual debug overlay"),

        /** Element inspector */
        ELEMENT_INSPECTOR(Tier.DEV, Category.DEV_TOOLS, "Interactive element inspector"),

        /** VUID viewer */
        VUID_VIEWER(Tier.DEV, Category.DEV_TOOLS, "View and search VUIDs"),

        /** Performance profiler */
        PERFORMANCE_PROFILER(Tier.DEV, Category.DEV_TOOLS, "Performance metrics viewer"),

        // ==================== Analytics (DEV) ====================

        /** Usage analytics */
        USAGE_ANALYTICS(Tier.DEV, Category.ANALYTICS, "Track feature usage"),

        /** Command success metrics */
        COMMAND_METRICS(Tier.DEV, Category.ANALYTICS, "Track command success rates"),

        // ==================== Experimental (DEV) ====================

        /** Hierarchy map generation */
        HIERARCHY_MAP(Tier.DEV, Category.EXPERIMENTAL, "Generate hierarchy maps"),

        /** Cross-app command learning */
        CROSS_APP_LEARNING(Tier.DEV, Category.EXPERIMENTAL, "Learn commands across apps");

        /**
         * Check if this feature is available in the given tier.
         */
        fun isAvailableIn(checkTier: Tier): Boolean {
            return when (tier) {
                Tier.LITE -> true // LITE features available in all tiers
                Tier.DEV -> checkTier == Tier.DEV
            }
        }
    }

    // ==================== State ====================

    /** Current active tier */
    private var currentTier: Tier = Tier.LITE

    /** Build type: debug or release */
    private var isDebugBuild: Boolean = false

    /** Overrides for individual features */
    private val featureOverrides = mutableMapOf<Feature, Boolean>()

    /** Listeners for tier changes */
    private val tierChangeListeners = mutableListOf<(Tier) -> Unit>()

    // ==================== Configuration ====================

    /**
     * Initialize the dev toggle with configuration.
     *
     * @param tier The feature tier to use
     * @param isDebug Whether this is a debug build
     */
    fun initialize(tier: Tier, isDebug: Boolean) {
        currentTier = tier
        isDebugBuild = isDebug
    }

    /**
     * Set the current tier.
     */
    fun setTier(tier: Tier) {
        if (currentTier != tier) {
            currentTier = tier
            tierChangeListeners.forEach { it(tier) }
        }
    }

    /**
     * Get the current tier.
     */
    fun getCurrentTier(): Tier = currentTier

    /**
     * Check if currently in debug mode.
     */
    fun isDebug(): Boolean = isDebugBuild

    /**
     * Check if Dev mode is enabled.
     */
    fun isDevMode(): Boolean = currentTier == Tier.DEV

    /**
     * Check if Lite mode is enabled.
     */
    fun isLiteMode(): Boolean = currentTier == Tier.LITE

    /**
     * Toggle between Lite and Dev modes.
     */
    fun toggle() {
        setTier(if (currentTier == Tier.LITE) Tier.DEV else Tier.LITE)
    }

    /**
     * Add a listener for tier changes.
     */
    fun addTierChangeListener(listener: (Tier) -> Unit) {
        tierChangeListeners.add(listener)
    }

    /**
     * Remove a tier change listener.
     */
    fun removeTierChangeListener(listener: (Tier) -> Unit) {
        tierChangeListeners.remove(listener)
    }

    // ==================== Feature Checking ====================

    /**
     * Check if a feature is enabled.
     */
    fun isEnabled(feature: Feature): Boolean {
        // Check for explicit override
        featureOverrides[feature]?.let { return it }

        // Check tier availability
        if (!feature.isAvailableIn(currentTier)) {
            return false
        }

        // Check default enabled status
        return feature.defaultEnabled
    }

    /**
     * Check if a feature is enabled (inline for performance).
     */
    inline fun isFeatureEnabled(feature: Feature): Boolean = isEnabled(feature)

    /**
     * Execute action only if feature is enabled.
     */
    inline fun <T> ifEnabled(feature: Feature, action: () -> T): T? {
        return if (isEnabled(feature)) action() else null
    }

    /**
     * Execute action only if feature is enabled, with fallback.
     */
    inline fun <T> ifEnabledOrElse(
        feature: Feature,
        enabled: () -> T,
        disabled: () -> T
    ): T {
        return if (isEnabled(feature)) enabled() else disabled()
    }

    // ==================== Feature Overrides ====================

    /**
     * Override a feature's enabled status.
     */
    fun setOverride(feature: Feature, enabled: Boolean) {
        featureOverrides[feature] = enabled
    }

    /**
     * Remove an override for a feature.
     */
    fun removeOverride(feature: Feature) {
        featureOverrides.remove(feature)
    }

    /**
     * Clear all overrides.
     */
    fun clearOverrides() {
        featureOverrides.clear()
    }

    /**
     * Get all overrides.
     */
    fun getOverrides(): Map<Feature, Boolean> = featureOverrides.toMap()

    // ==================== Querying ====================

    /**
     * Get all features in a category.
     */
    fun getFeaturesByCategory(category: Category): List<Feature> {
        return Feature.entries.filter { it.category == category }
    }

    /**
     * Get all features in a tier.
     */
    fun getFeaturesByTier(tier: Tier): List<Feature> {
        return Feature.entries.filter { it.tier == tier }
    }

    /**
     * Get all enabled features.
     */
    fun getEnabledFeatures(): List<Feature> {
        return Feature.entries.filter { isEnabled(it) }
    }

    /**
     * Get all disabled features.
     */
    fun getDisabledFeatures(): List<Feature> {
        return Feature.entries.filter { !isEnabled(it) }
    }

    /**
     * Check if any feature in a category is enabled.
     */
    fun isCategoryEnabled(category: Category): Boolean {
        return getFeaturesByCategory(category).any { isEnabled(it) }
    }

    // ==================== Testing Helpers ====================

    /**
     * Reset to default state (for testing).
     */
    fun reset() {
        currentTier = Tier.LITE
        isDebugBuild = false
        featureOverrides.clear()
        tierChangeListeners.clear()
    }
}
