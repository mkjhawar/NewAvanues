/**
 * OverlayConfig.kt - User configuration and preferences for overlay system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-01-06
 *
 * KMP-compatible overlay configuration system.
 * Manages user preferences including:
 * - Theme selection
 * - Accessibility settings (large text, high contrast, reduced motion)
 * - Custom color overrides
 * - Animation preferences
 * - Display and feedback settings
 *
 * Platform-specific storage implementations use expect/actual pattern.
 */
package com.augmentalis.voiceoscoreng.features

/**
 * User configuration for overlay system (KMP-compatible)
 *
 * This class manages all overlay-related user preferences in a platform-agnostic way.
 * For persistent storage, platform-specific implementations should use the
 * expect/actual pattern or inject a storage adapter.
 *
 * Usage:
 * ```kotlin
 * val config = OverlayConfig()
 *
 * // Change theme
 * config.themeName = "HighContrast"
 *
 * // Enable accessibility features
 * config.largeText = true
 * config.reducedMotion = true
 *
 * // Validate configuration
 * val validation = config.validate()
 * if (!validation.isValid) {
 *     println("Errors: ${validation.errors}")
 * }
 * ```
 */
class OverlayConfig {

    // ===== THEME SETTINGS =====

    /**
     * Current theme name
     * Setting an empty string resets to default ("Material3Dark")
     */
    var themeName: String = DEFAULT_THEME_NAME
        set(value) {
            field = value.ifEmpty { DEFAULT_THEME_NAME }
        }

    // ===== ACCESSIBILITY SETTINGS =====

    /**
     * Enable large text mode (increases font sizes by 25%)
     */
    var largeText: Boolean = false

    /**
     * Enable high contrast mode (darker colors, higher contrast ratios)
     */
    var highContrast: Boolean = false

    /**
     * Enable reduced motion mode (disables animations)
     */
    var reducedMotion: Boolean = false

    // ===== DISPLAY SETTINGS =====

    /**
     * Show numbers on overlay elements
     */
    var numbersEnabled: Boolean = true

    /**
     * Show labels on overlay elements
     */
    var labelsEnabled: Boolean = true

    // ===== FEEDBACK SETTINGS =====

    /**
     * Enable voice feedback for overlay interactions
     */
    var voiceFeedback: Boolean = true

    /**
     * Enable haptic feedback for overlay interactions
     */
    var hapticFeedback: Boolean = true

    // ===== CUSTOM COLOR =====

    /**
     * Custom primary color override (null if not set)
     * Stored as ARGB Long value (0xAARRGGBB format)
     */
    var customPrimaryColor: Long? = null
        private set

    /**
     * Check if custom primary color is set
     */
    val hasCustomPrimaryColor: Boolean
        get() = customPrimaryColor != null

    /**
     * Set custom primary color
     * @param color ARGB color value as Long (0xAARRGGBB format)
     */
    fun setCustomPrimaryColor(color: Long) {
        customPrimaryColor = color
    }

    /**
     * Clear custom primary color (revert to theme default)
     */
    fun clearCustomPrimaryColor() {
        customPrimaryColor = null
    }

    // ===== COMPUTED PROPERTIES =====

    /**
     * Font scale multiplier based on large text setting
     * Normal: 1.0, Large text: 1.25 (25% increase)
     */
    val fontScale: Float
        get() = if (largeText) LARGE_TEXT_FONT_SCALE else 1.0f

    /**
     * Contrast multiplier based on high contrast setting
     * Normal: 1.0, High contrast: 1.5 (50% darker backgrounds)
     */
    val contrastMultiplier: Float
        get() = if (highContrast) HIGH_CONTRAST_MULTIPLIER else 1.0f

    /**
     * Whether animations are enabled (inverse of reduced motion)
     */
    val animationsEnabled: Boolean
        get() = !reducedMotion

    /**
     * Animation duration in milliseconds
     * Returns 0 if reduced motion is enabled, otherwise default duration
     */
    val animationDurationMs: Long
        get() = if (reducedMotion) 0L else DEFAULT_ANIMATION_DURATION_MS

    // ===== VALIDATION =====

    /**
     * Validate current configuration
     * @return ConfigValidationResult with errors and warnings
     */
    fun validate(): ConfigValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validate theme name is not blank
        if (themeName.isBlank()) {
            errors.add("Theme name cannot be blank")
        }

        // Warn if conflicting settings
        if (highContrast && customPrimaryColor != null) {
            warnings.add("Custom primary color may conflict with high contrast mode")
        }

        // Warn if both numbers and labels are disabled
        if (!numbersEnabled && !labelsEnabled) {
            warnings.add("Both numbers and labels are disabled - overlays may be difficult to use")
        }

        // Warn if both feedback types are disabled
        if (!voiceFeedback && !hapticFeedback) {
            warnings.add("Both voice and haptic feedback are disabled - user may not receive confirmation of actions")
        }

        return ConfigValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    // ===== EXPORT/IMPORT =====

    /**
     * Export current configuration as a map
     * Custom primary color is only included if set
     */
    fun exportToMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "themeName" to themeName,
            "largeText" to largeText,
            "highContrast" to highContrast,
            "reducedMotion" to reducedMotion,
            "numbersEnabled" to numbersEnabled,
            "labelsEnabled" to labelsEnabled,
            "voiceFeedback" to voiceFeedback,
            "hapticFeedback" to hapticFeedback
        )

        customPrimaryColor?.let { color ->
            map["customPrimaryColor"] = color
        }

        return map
    }

    /**
     * Import configuration from a map
     * Missing keys are ignored (keeping current values)
     */
    fun importFromMap(map: Map<String, Any>) {
        (map["themeName"] as? String)?.let { themeName = it }
        (map["largeText"] as? Boolean)?.let { largeText = it }
        (map["highContrast"] as? Boolean)?.let { highContrast = it }
        (map["reducedMotion"] as? Boolean)?.let { reducedMotion = it }
        (map["numbersEnabled"] as? Boolean)?.let { numbersEnabled = it }
        (map["labelsEnabled"] as? Boolean)?.let { labelsEnabled = it }
        (map["voiceFeedback"] as? Boolean)?.let { voiceFeedback = it }
        (map["hapticFeedback"] as? Boolean)?.let { hapticFeedback = it }
        (map["customPrimaryColor"] as? Long)?.let { setCustomPrimaryColor(it) }
    }

    /**
     * Export current configuration as a human-readable string (for debugging/support)
     */
    fun exportConfigString(): String {
        return buildString {
            appendLine("=== Overlay Configuration ===")
            appendLine("Theme: $themeName")
            appendLine("Large Text: $largeText (font scale: $fontScale)")
            appendLine("High Contrast: $highContrast (contrast multiplier: $contrastMultiplier)")
            appendLine("Reduced Motion: $reducedMotion (animations: $animationsEnabled)")
            appendLine("Custom Primary Color: ${customPrimaryColor?.let { "0x${it.toString(16).uppercase()}" } ?: "None"}")
            appendLine("Show Numbers: $numbersEnabled")
            appendLine("Show Labels: $labelsEnabled")
            appendLine("Voice Feedback: $voiceFeedback")
            appendLine("Haptic Feedback: $hapticFeedback")
            appendLine("============================")
        }
    }

    // ===== RESET =====

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        themeName = DEFAULT_THEME_NAME
        largeText = false
        highContrast = false
        reducedMotion = false
        numbersEnabled = true
        labelsEnabled = true
        voiceFeedback = true
        hapticFeedback = true
        customPrimaryColor = null
    }

    // ===== COPY =====

    /**
     * Create an independent copy of this configuration
     */
    fun copy(): OverlayConfig {
        return OverlayConfig().also { copy ->
            copy.themeName = this.themeName
            copy.largeText = this.largeText
            copy.highContrast = this.highContrast
            copy.reducedMotion = this.reducedMotion
            copy.numbersEnabled = this.numbersEnabled
            copy.labelsEnabled = this.labelsEnabled
            copy.voiceFeedback = this.voiceFeedback
            copy.hapticFeedback = this.hapticFeedback
            this.customPrimaryColor?.let { copy.setCustomPrimaryColor(it) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverlayConfig) return false

        return themeName == other.themeName &&
            largeText == other.largeText &&
            highContrast == other.highContrast &&
            reducedMotion == other.reducedMotion &&
            numbersEnabled == other.numbersEnabled &&
            labelsEnabled == other.labelsEnabled &&
            voiceFeedback == other.voiceFeedback &&
            hapticFeedback == other.hapticFeedback &&
            customPrimaryColor == other.customPrimaryColor
    }

    override fun hashCode(): Int {
        var result = themeName.hashCode()
        result = 31 * result + largeText.hashCode()
        result = 31 * result + highContrast.hashCode()
        result = 31 * result + reducedMotion.hashCode()
        result = 31 * result + numbersEnabled.hashCode()
        result = 31 * result + labelsEnabled.hashCode()
        result = 31 * result + voiceFeedback.hashCode()
        result = 31 * result + hapticFeedback.hashCode()
        result = 31 * result + (customPrimaryColor?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "OverlayConfig(themeName='$themeName', largeText=$largeText, highContrast=$highContrast, " +
            "reducedMotion=$reducedMotion, numbersEnabled=$numbersEnabled, labelsEnabled=$labelsEnabled, " +
            "voiceFeedback=$voiceFeedback, hapticFeedback=$hapticFeedback, customPrimaryColor=$customPrimaryColor)"
    }

    companion object {
        const val DEFAULT_THEME_NAME = "Material3Dark"
        const val DEFAULT_ANIMATION_DURATION_MS = 300L
        const val LARGE_TEXT_FONT_SCALE = 1.25f
        const val HIGH_CONTRAST_MULTIPLIER = 1.5f

        /**
         * Available theme names for validation
         */
        val AVAILABLE_THEMES = listOf(
            "Material3Dark",
            "Material3Light",
            "HighContrast",
            "Classic",
            "AMOLED"
        )

        /**
         * Create a new config with large text enabled
         */
        fun withLargeText(): OverlayConfig {
            return OverlayConfig().also { it.largeText = true }
        }

        /**
         * Create a new config with high contrast enabled
         */
        fun withHighContrast(): OverlayConfig {
            return OverlayConfig().also { it.highContrast = true }
        }

        /**
         * Create a new config with reduced motion enabled
         */
        fun withReducedMotion(): OverlayConfig {
            return OverlayConfig().also { it.reducedMotion = true }
        }

        /**
         * Create a new config with all accessibility features enabled
         */
        fun withAllAccessibility(): OverlayConfig {
            return OverlayConfig().also {
                it.largeText = true
                it.highContrast = true
                it.reducedMotion = true
            }
        }
    }
}

/**
 * Result of configuration validation
 */
data class ConfigValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    override fun toString(): String {
        return buildString {
            if (isValid) {
                appendLine("Configuration is valid")
            } else {
                appendLine("Configuration validation failed:")
                errors.forEach { appendLine("  ERROR: $it") }
            }

            if (warnings.isNotEmpty()) {
                appendLine("Warnings:")
                warnings.forEach { appendLine("  WARNING: $it") }
            }
        }
    }
}
