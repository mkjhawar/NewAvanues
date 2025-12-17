/**
 * NumberOverlayConfig.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09 12:37:30 PDT
 */
/**
 * NumberOverlayConfig.kt
 *
 * Purpose: User preferences and configuration for number overlay system
 * Provides persistent storage and runtime configuration
 *
 * Features:
 * - Enable/disable overlays
 * - Position preference (anchor point)
 * - Size scaling (style variants)
 * - Color scheme selection
 * - Animation preferences
 * - Accessibility mode detection
 *
 * Created: 2025-10-09 12:37:30 PDT
 */
package com.augmentalis.voiceoscore.ui.overlays

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.provider.Settings
import androidx.core.content.edit

/**
 * Configuration for number overlay system
 *
 * This class handles user preferences and system accessibility settings
 */
data class NumberOverlayConfig(
    /**
     * Enable/disable overlay system
     */
    val enabled: Boolean = true,

    /**
     * Style variant (determines size, colors, effects)
     */
    val styleVariant: StyleVariant = StyleVariant.STANDARD,

    /**
     * Anchor point for badge placement
     */
    val anchorPoint: AnchorPoint = AnchorPoint.TOP_RIGHT,

    /**
     * Show overlays in landscape mode
     */
    val showInLandscape: Boolean = true,

    /**
     * Show overlays in portrait mode
     */
    val showInPortrait: Boolean = true,

    /**
     * Hide overlays when window loses focus
     */
    val hideOnWindowFocusLoss: Boolean = false,

    /**
     * Auto-hide overlays after timeout (milliseconds)
     * 0 = never auto-hide
     */
    val autoHideTimeoutMs: Long = 0,

    /**
     * Enable fade-in/fade-out animations
     */
    val enableAnimations: Boolean = true,

    /**
     * Animation duration (milliseconds)
     */
    val animationDurationMs: Long = 200,

    /**
     * Show only for clickable elements
     */
    val clickableOnly: Boolean = false,

    /**
     * Show only for enabled elements
     */
    val enabledOnly: Boolean = true,

    /**
     * Minimum element size to show overlay (dp)
     */
    val minElementSizeDp: Int = 24,

    /**
     * Maximum number of overlays to show
     */
    val maxOverlays: Int = 100,

    /**
     * Enable hardware acceleration
     */
    val hardwareAcceleration: Boolean = true,

    /**
     * Respect system accessibility settings
     */
    val respectAccessibilitySettings: Boolean = true,

    /**
     * Custom scale factor (1.0 = normal, 1.5 = 150%, etc.)
     */
    val scaleFactor: Float = 1.0f
) {

    /**
     * Check if overlays should be shown in current orientation
     */
    fun shouldShowInOrientation(orientation: Int): Boolean {
        return when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> showInLandscape
            Configuration.ORIENTATION_PORTRAIT -> showInPortrait
            else -> true
        }
    }

    /**
     * Get effective style variant based on system settings
     */
    fun getEffectiveStyleVariant(context: Context): StyleVariant {
        if (!respectAccessibilitySettings) {
            return styleVariant
        }

        // Check for high contrast mode
        if (isHighContrastEnabled(context)) {
            return StyleVariant.HIGH_CONTRAST
        }

        // Check for large text mode
        if (isLargeTextEnabled(context)) {
            return StyleVariant.LARGE_TEXT
        }

        return styleVariant
    }

    /**
     * Check if high contrast is enabled
     */
    private fun isHighContrastEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                "high_text_contrast_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if large text is enabled
     */
    private fun isLargeTextEnabled(context: Context): Boolean {
        val fontScale = context.resources.configuration.fontScale
        return fontScale >= 1.3f
    }

    companion object {
        /**
         * Default configuration
         */
        fun default() = NumberOverlayConfig()

        /**
         * Minimal configuration (performance mode)
         */
        fun minimal() = NumberOverlayConfig(
            styleVariant = StyleVariant.COMPACT,
            enableAnimations = false,
            hardwareAcceleration = true,
            maxOverlays = 50
        )

        /**
         * Accessibility-focused configuration
         */
        fun accessibility() = NumberOverlayConfig(
            styleVariant = StyleVariant.LARGE_TEXT,
            enableAnimations = false,
            respectAccessibilitySettings = true,
            clickableOnly = true,
            enabledOnly = true,
            minElementSizeDp = 32
        )

        /**
         * Testing configuration (show everything)
         */
        fun testing() = NumberOverlayConfig(
            enabled = true,
            clickableOnly = false,
            enabledOnly = false,
            maxOverlays = 200,
            hideOnWindowFocusLoss = false
        )
    }
}

/**
 * Configuration persistence manager
 *
 * Handles saving and loading configuration from SharedPreferences
 */
class NumberOverlayConfigManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Save configuration
     */
    fun saveConfig(config: NumberOverlayConfig) {
        prefs.edit {
            putBoolean(KEY_ENABLED, config.enabled)
            putString(KEY_STYLE_VARIANT, config.styleVariant.name)
            putString(KEY_ANCHOR_POINT, config.anchorPoint.name)
            putBoolean(KEY_SHOW_IN_LANDSCAPE, config.showInLandscape)
            putBoolean(KEY_SHOW_IN_PORTRAIT, config.showInPortrait)
            putBoolean(KEY_HIDE_ON_FOCUS_LOSS, config.hideOnWindowFocusLoss)
            putLong(KEY_AUTO_HIDE_TIMEOUT, config.autoHideTimeoutMs)
            putBoolean(KEY_ENABLE_ANIMATIONS, config.enableAnimations)
            putLong(KEY_ANIMATION_DURATION, config.animationDurationMs)
            putBoolean(KEY_CLICKABLE_ONLY, config.clickableOnly)
            putBoolean(KEY_ENABLED_ONLY, config.enabledOnly)
            putInt(KEY_MIN_ELEMENT_SIZE, config.minElementSizeDp)
            putInt(KEY_MAX_OVERLAYS, config.maxOverlays)
            putBoolean(KEY_HARDWARE_ACCELERATION, config.hardwareAcceleration)
            putBoolean(KEY_RESPECT_ACCESSIBILITY, config.respectAccessibilitySettings)
            putFloat(KEY_SCALE_FACTOR, config.scaleFactor)
        }
    }

    /**
     * Load configuration
     */
    fun loadConfig(): NumberOverlayConfig {
        return NumberOverlayConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            styleVariant = StyleVariant.valueOf(
                prefs.getString(KEY_STYLE_VARIANT, StyleVariant.STANDARD.name)!!
            ),
            anchorPoint = AnchorPoint.valueOf(
                prefs.getString(KEY_ANCHOR_POINT, AnchorPoint.TOP_RIGHT.name)!!
            ),
            showInLandscape = prefs.getBoolean(KEY_SHOW_IN_LANDSCAPE, true),
            showInPortrait = prefs.getBoolean(KEY_SHOW_IN_PORTRAIT, true),
            hideOnWindowFocusLoss = prefs.getBoolean(KEY_HIDE_ON_FOCUS_LOSS, false),
            autoHideTimeoutMs = prefs.getLong(KEY_AUTO_HIDE_TIMEOUT, 0),
            enableAnimations = prefs.getBoolean(KEY_ENABLE_ANIMATIONS, true),
            animationDurationMs = prefs.getLong(KEY_ANIMATION_DURATION, 200),
            clickableOnly = prefs.getBoolean(KEY_CLICKABLE_ONLY, false),
            enabledOnly = prefs.getBoolean(KEY_ENABLED_ONLY, true),
            minElementSizeDp = prefs.getInt(KEY_MIN_ELEMENT_SIZE, 24),
            maxOverlays = prefs.getInt(KEY_MAX_OVERLAYS, 100),
            hardwareAcceleration = prefs.getBoolean(KEY_HARDWARE_ACCELERATION, true),
            respectAccessibilitySettings = prefs.getBoolean(KEY_RESPECT_ACCESSIBILITY, true),
            scaleFactor = prefs.getFloat(KEY_SCALE_FACTOR, 1.0f)
        )
    }

    /**
     * Reset to default configuration
     */
    fun resetToDefault() {
        saveConfig(NumberOverlayConfig.default())
    }

    /**
     * Check if configuration exists
     */
    fun hasConfig(): Boolean {
        return prefs.contains(KEY_ENABLED)
    }

    companion object {
        private const val PREFS_NAME = "number_overlay_prefs"

        // Preference keys
        private const val KEY_ENABLED = "enabled"
        private const val KEY_STYLE_VARIANT = "style_variant"
        private const val KEY_ANCHOR_POINT = "anchor_point"
        private const val KEY_SHOW_IN_LANDSCAPE = "show_in_landscape"
        private const val KEY_SHOW_IN_PORTRAIT = "show_in_portrait"
        private const val KEY_HIDE_ON_FOCUS_LOSS = "hide_on_focus_loss"
        private const val KEY_AUTO_HIDE_TIMEOUT = "auto_hide_timeout"
        private const val KEY_ENABLE_ANIMATIONS = "enable_animations"
        private const val KEY_ANIMATION_DURATION = "animation_duration"
        private const val KEY_CLICKABLE_ONLY = "clickable_only"
        private const val KEY_ENABLED_ONLY = "enabled_only"
        private const val KEY_MIN_ELEMENT_SIZE = "min_element_size"
        private const val KEY_MAX_OVERLAYS = "max_overlays"
        private const val KEY_HARDWARE_ACCELERATION = "hardware_acceleration"
        private const val KEY_RESPECT_ACCESSIBILITY = "respect_accessibility"
        private const val KEY_SCALE_FACTOR = "scale_factor"
    }
}

/**
 * Builder for creating configurations fluently
 */
class NumberOverlayConfigBuilder {
    private var config = NumberOverlayConfig.default()

    fun enabled(value: Boolean) = apply { config = config.copy(enabled = value) }
    fun styleVariant(variant: StyleVariant) = apply { config = config.copy(styleVariant = variant) }
    fun anchorPoint(point: AnchorPoint) = apply { config = config.copy(anchorPoint = point) }
    fun showInLandscape(value: Boolean) = apply { config = config.copy(showInLandscape = value) }
    fun showInPortrait(value: Boolean) = apply { config = config.copy(showInPortrait = value) }
    fun hideOnFocusLoss(value: Boolean) = apply { config = config.copy(hideOnWindowFocusLoss = value) }
    fun autoHideTimeout(ms: Long) = apply { config = config.copy(autoHideTimeoutMs = ms) }
    fun enableAnimations(value: Boolean) = apply { config = config.copy(enableAnimations = value) }
    fun animationDuration(ms: Long) = apply { config = config.copy(animationDurationMs = ms) }
    fun clickableOnly(value: Boolean) = apply { config = config.copy(clickableOnly = value) }
    fun enabledOnly(value: Boolean) = apply { config = config.copy(enabledOnly = value) }
    fun minElementSize(dp: Int) = apply { config = config.copy(minElementSizeDp = dp) }
    fun maxOverlays(count: Int) = apply { config = config.copy(maxOverlays = count) }
    fun hardwareAcceleration(value: Boolean) = apply { config = config.copy(hardwareAcceleration = value) }
    fun respectAccessibility(value: Boolean) = apply { config = config.copy(respectAccessibilitySettings = value) }
    fun scaleFactor(factor: Float) = apply { config = config.copy(scaleFactor = factor) }

    fun build(): NumberOverlayConfig = config
}

/**
 * Extension function for fluent configuration building
 */
fun NumberOverlayConfig.Companion.build(block: NumberOverlayConfigBuilder.() -> Unit): NumberOverlayConfig {
    return NumberOverlayConfigBuilder().apply(block).build()
}
