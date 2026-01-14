/**
 * OverlayConfig.kt - User configuration and preferences for overlay system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-13
 */
package com.augmentalis.voiceoscore.accessibility.overlays.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.augmentalis.voiceoscore.utils.ConditionalLogger

/**
 * User configuration for overlay system
 *
 * Manages user preferences including:
 * - Theme selection
 * - Accessibility settings (large text, high contrast, reduced motion)
 * - Custom color overrides
 * - Animation preferences
 * - Persistent storage via SharedPreferences
 *
 * Usage:
 * ```kotlin
 * val config = OverlayConfig.getInstance(context)
 * val theme = config.getEffectiveTheme()
 *
 * // Change theme
 * config.setTheme("HighContrast")
 *
 * // Enable accessibility features
 * config.setLargeText(true)
 * config.setReducedMotion(true)
 * ```
 */
class OverlayConfig private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "OverlayConfig"
        private const val PREFS_NAME = "overlay_config"

        // Preference keys
        private const val KEY_THEME_NAME = "theme_name"
        private const val KEY_LARGE_TEXT = "large_text"
        private const val KEY_HIGH_CONTRAST = "high_contrast"
        private const val KEY_REDUCED_MOTION = "reduced_motion"
        private const val KEY_CUSTOM_PRIMARY_COLOR = "custom_primary_color"
        private const val KEY_SHOW_NUMBERS = "show_numbers"
        private const val KEY_SHOW_LABELS = "show_labels"
        private const val KEY_VOICE_FEEDBACK = "voice_feedback"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"

        // Singleton instance
        @Volatile
        private var instance: OverlayConfig? = null

        fun getInstance(context: Context): OverlayConfig {
            return instance ?: synchronized(this) {
                instance ?: OverlayConfig(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ===== THEME SETTINGS =====

    /**
     * Get selected theme name
     */
    fun getThemeName(): String {
        return prefs.getString(KEY_THEME_NAME, "Material3Dark") ?: "Material3Dark"
    }

    /**
     * Set theme by name
     */
    fun setTheme(themeName: String) {
        prefs.edit().putString(KEY_THEME_NAME, themeName).apply()
        ConditionalLogger.i(TAG) { "Theme changed to: $themeName" }
    }

    /**
     * Get base theme (before accessibility modifications)
     */
    fun getBaseTheme(): OverlayTheme {
        return OverlayThemes.getTheme(getThemeName())
    }

    /**
     * Get effective theme with all accessibility modifications applied
     */
    fun getEffectiveTheme(): OverlayTheme {
        var theme = getBaseTheme()

        // Apply accessibility settings
        if (isLargeTextEnabled()) {
            theme = theme.withLargeText()
            ConditionalLogger.d(TAG) { "Applied large text to theme" }
        }

        if (isHighContrastEnabled()) {
            theme = theme.toHighContrast()
            ConditionalLogger.d(TAG) { "Applied high contrast to theme" }
        }

        if (isReducedMotionEnabled()) {
            theme = theme.withReducedMotion()
            ConditionalLogger.d(TAG) { "Applied reduced motion to theme" }
        }

        // Apply custom primary color if set
        getCustomPrimaryColor()?.let { color ->
            theme = theme.withPrimaryColor(color)
            ConditionalLogger.d(TAG) { "Applied custom primary color: #${Integer.toHexString(color.toArgb())}" }
        }

        return theme
    }

    // ===== ACCESSIBILITY SETTINGS =====

    /**
     * Check if large text is enabled
     */
    fun isLargeTextEnabled(): Boolean {
        return prefs.getBoolean(KEY_LARGE_TEXT, false)
    }

    /**
     * Enable/disable large text
     */
    fun setLargeText(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LARGE_TEXT, enabled).apply()
        ConditionalLogger.i(TAG) { "Large text ${if (enabled) "enabled" else "disabled"}" }
    }

    /**
     * Check if high contrast is enabled
     */
    fun isHighContrastEnabled(): Boolean {
        return prefs.getBoolean(KEY_HIGH_CONTRAST, false)
    }

    /**
     * Enable/disable high contrast
     */
    fun setHighContrast(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
        ConditionalLogger.i(TAG) { "High contrast ${if (enabled) "enabled" else "disabled"}" }
    }

    /**
     * Check if reduced motion is enabled
     */
    fun isReducedMotionEnabled(): Boolean {
        return prefs.getBoolean(KEY_REDUCED_MOTION, false)
    }

    /**
     * Enable/disable reduced motion
     */
    fun setReducedMotion(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REDUCED_MOTION, enabled).apply()
        ConditionalLogger.i(TAG) { "Reduced motion ${if (enabled) "enabled" else "disabled"}" }
    }

    // ===== CUSTOM COLORS =====

    /**
     * Get custom primary color (null if not set)
     */
    fun getCustomPrimaryColor(): Color? {
        val argb = prefs.getInt(KEY_CUSTOM_PRIMARY_COLOR, 0)
        return if (argb != 0) Color(argb) else null
    }

    /**
     * Set custom primary color
     */
    fun setCustomPrimaryColor(color: Color) {
        prefs.edit().putInt(KEY_CUSTOM_PRIMARY_COLOR, color.toArgb()).apply()
        ConditionalLogger.i(TAG) { "Custom primary color set: #${Integer.toHexString(color.toArgb())}" }
    }

    /**
     * Clear custom primary color
     */
    fun clearCustomPrimaryColor() {
        prefs.edit().remove(KEY_CUSTOM_PRIMARY_COLOR).apply()
        ConditionalLogger.i(TAG) { "Custom primary color cleared" }
    }

    // ===== DISPLAY SETTINGS =====

    /**
     * Check if numbers should be shown on overlays
     */
    fun areNumbersEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHOW_NUMBERS, true)
    }

    /**
     * Enable/disable numbers on overlays
     */
    fun setNumbersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_NUMBERS, enabled).apply()
        ConditionalLogger.i(TAG) { "Numbers ${if (enabled) "enabled" else "disabled"}" }
    }

    /**
     * Check if labels should be shown on overlays
     */
    fun areLabelsEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHOW_LABELS, true)
    }

    /**
     * Enable/disable labels on overlays
     */
    fun setLabelsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_LABELS, enabled).apply()
        ConditionalLogger.i(TAG) { "Labels ${if (enabled) "enabled" else "disabled"}" }
    }

    // ===== FEEDBACK SETTINGS =====

    /**
     * Check if voice feedback is enabled
     */
    fun isVoiceFeedbackEnabled(): Boolean {
        return prefs.getBoolean(KEY_VOICE_FEEDBACK, true)
    }

    /**
     * Enable/disable voice feedback
     */
    fun setVoiceFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_FEEDBACK, enabled).apply()
        ConditionalLogger.i(TAG) { "Voice feedback ${if (enabled) "enabled" else "disabled"}" }
    }

    /**
     * Check if haptic feedback is enabled
     */
    fun isHapticFeedbackEnabled(): Boolean {
        return prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
    }

    /**
     * Enable/disable haptic feedback
     */
    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
        ConditionalLogger.i(TAG) { "Haptic feedback ${if (enabled) "enabled" else "disabled"}" }
    }

    // ===== UTILITY METHODS =====

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        ConditionalLogger.i(TAG) { "All settings reset to defaults" }
    }

    /**
     * Export current configuration as string (for debugging/support)
     */
    fun exportConfig(): String {
        return buildString {
            appendLine("=== Overlay Configuration ===")
            appendLine("Theme: ${getThemeName()}")
            appendLine("Large Text: ${isLargeTextEnabled()}")
            appendLine("High Contrast: ${isHighContrastEnabled()}")
            appendLine("Reduced Motion: ${isReducedMotionEnabled()}")
            appendLine("Custom Primary Color: ${getCustomPrimaryColor()?.let { "#${Integer.toHexString(it.toArgb())}" } ?: "None"}")
            appendLine("Show Numbers: ${areNumbersEnabled()}")
            appendLine("Show Labels: ${areLabelsEnabled()}")
            appendLine("Voice Feedback: ${isVoiceFeedbackEnabled()}")
            appendLine("Haptic Feedback: ${isHapticFeedbackEnabled()}")
            appendLine("============================")
        }
    }

    /**
     * Log current configuration (for debugging)
     */
    fun logConfig() {
        ConditionalLogger.i(TAG) { exportConfig() }
    }

    /**
     * Validate current configuration
     */
    fun validateConfig(): ConfigValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validate theme exists
        val themeName = getThemeName()
        if (!OverlayThemes.getThemeNames().contains(themeName)) {
            errors.add("Invalid theme name: $themeName")
        }

        // Validate effective theme
        val themeValidation = getEffectiveTheme().validate()
        if (!themeValidation.isValid) {
            warnings.addAll(themeValidation.errors.map { "Theme validation: $it" })
        }

        // Warn if conflicting settings
        if (isHighContrastEnabled() && getCustomPrimaryColor() != null) {
            warnings.add("Custom primary color may conflict with high contrast mode")
        }

        return ConfigValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}

/**
 * Result of configuration validation
 */
data class ConfigValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String> = emptyList()
) {
    override fun toString(): String {
        return buildString {
            if (isValid) {
                appendLine("✅ Configuration is valid")
            } else {
                appendLine("❌ Configuration validation failed:")
                errors.forEach { appendLine("  ERROR: $it") }
            }

            if (warnings.isNotEmpty()) {
                appendLine("⚠️  Warnings:")
                warnings.forEach { appendLine("  WARNING: $it") }
            }
        }
    }
}
