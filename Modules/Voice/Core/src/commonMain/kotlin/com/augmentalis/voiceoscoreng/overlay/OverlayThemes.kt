/**
 * OverlayThemes.kt - Dynamic theme registry for overlay system
 *
 * Provides a central registry for managing overlay themes with support for:
 * - Default themes (light, dark, high_contrast)
 * - Custom theme registration/unregistration
 * - Current theme tracking
 * - Reset functionality for testing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.overlay

import com.augmentalis.voiceoscoreng.features.OverlayTheme

/**
 * Dynamic registry for overlay themes.
 *
 * Unlike [com.augmentalis.voiceoscoreng.features.OverlayThemes] which provides
 * predefined theme presets, this registry allows runtime registration and
 * management of themes for the overlay system.
 *
 * Usage:
 * ```kotlin
 * // Get current theme
 * val theme = OverlayThemes.getCurrent()
 *
 * // Switch to dark theme
 * OverlayThemes.setCurrent("dark")
 *
 * // Register custom theme
 * OverlayThemes.register(myCustomTheme)
 *
 * // Get specific theme
 * val dark = OverlayThemes.get("dark")
 * ```
 */
object OverlayThemes {

    /**
     * Internal storage for registered themes.
     * Key: theme name (lowercase), Value: OverlayTheme instance
     */
    private val themes = mutableMapOf<String, OverlayTheme>()

    /**
     * Currently active theme. Defaults to LIGHT.
     */
    private var currentTheme: OverlayTheme = OverlayTheme.LIGHT

    /**
     * Default light theme for the overlay system.
     * White/light gray backgrounds optimized for daytime use.
     */
    private val LIGHT = OverlayTheme(
        primaryColor = 0xFF1976D2,  // Blue
        backgroundColor = 0xEEFFFFFF,  // White with slight transparency
        backdropColor = 0x4DFFFFFF,    // White with 0.3 alpha
        textPrimaryColor = 0xFF000000,  // Black
        textSecondaryColor = 0xB3000000,  // Black with 0.7 alpha
        textDisabledColor = 0xFF808080,   // Gray
        borderColor = 0xFF000000,
        dividerColor = 0x1A000000,  // Black with 0.1 alpha
        cardBackgroundColor = 0xEEF5F5F5,
        tooltipBackgroundColor = 0xEE333333,
        badgeEnabledWithNameColor = 0xFF2E7D32,  // Dark green
        badgeEnabledNoNameColor = 0xFFF57C00,    // Dark orange
        statusSuccessColor = 0xFF2E7D32,
        statusErrorColor = 0xFFC62828
    )

    /**
     * Default dark theme for the overlay system.
     * Dark backgrounds optimized for low-light environments.
     */
    private val DARK = OverlayTheme(
        primaryColor = 0xFF2196F3,  // Blue
        backgroundColor = 0xEE1E1E1E,
        backdropColor = 0x4D000000,  // Black with 0.3 alpha
        textPrimaryColor = 0xFFFFFFFF,
        textSecondaryColor = 0xB3FFFFFF,  // White with 0.7 alpha
        textDisabledColor = 0xFF808080,
        borderColor = 0xFFFFFFFF,
        dividerColor = 0x1AFFFFFF,  // White with 0.1 alpha
        cardBackgroundColor = 0xEE1E1E1E,
        tooltipBackgroundColor = 0xEE000000,
        badgeEnabledWithNameColor = 0xFF4CAF50,  // Green
        badgeEnabledNoNameColor = 0xFFFF9800,    // Orange
        statusSuccessColor = 0xFF4CAF50,
        statusErrorColor = 0xFFF44336
    )

    /**
     * High contrast theme for accessibility.
     * Maximum contrast for users with vision impairments.
     * Meets WCAG AAA standards (7:1 contrast ratio).
     */
    private val HIGH_CONTRAST = OverlayTheme(
        primaryColor = 0xFF00BFFF,  // Bright cyan
        backgroundColor = 0xFF000000,  // Pure black
        backdropColor = 0xFF000000,    // No transparency
        textPrimaryColor = 0xFFFFFFFF,  // Pure white
        textSecondaryColor = 0xFFFFFFFF,
        textDisabledColor = 0xFFAAAAAA,
        borderColor = 0xFFFFFFFF,
        dividerColor = 0xFFFFFFFF,
        cardBackgroundColor = 0xFF000000,
        tooltipBackgroundColor = 0xFF000000,
        badgeEnabledWithNameColor = 0xFF00FF00,  // Bright green
        badgeEnabledNoNameColor = 0xFFFFAA00,    // Bright orange
        statusSuccessColor = 0xFF00FF00,
        statusErrorColor = 0xFFFF0000,
        borderWidthMedium = 3f,
        borderWidthThick = 4f,
        titleFontSize = 20f,
        bodyFontSize = 18f,
        badgeFontSize = 18f,
        minimumTouchTargetSize = 56f,
        minimumContrastRatio = 7.0f  // WCAG AAA
    )

    init {
        registerDefaults()
    }

    /**
     * Registers a theme with an explicit name.
     *
     * If a theme with the same name already exists, it will be replaced.
     *
     * @param name The name to register the theme under (case-sensitive)
     * @param theme The theme configuration to register
     */
    fun register(name: String, theme: OverlayTheme) {
        themes[name] = theme
    }

    /**
     * Registers a theme using its instructionTextDefault as the name.
     *
     * This is a convenience method for themes that store their name
     * in the instructionTextDefault property.
     *
     * @param theme The theme to register (uses instructionTextDefault as name)
     */
    fun register(theme: OverlayTheme) {
        val name = theme.instructionTextDefault
        if (name.isNotBlank()) {
            themes[name] = theme
        }
    }

    /**
     * Unregisters a theme by name.
     *
     * @param name The name of the theme to unregister
     * @return true if the theme was found and removed, false if not found
     */
    fun unregister(name: String): Boolean {
        if (name.isBlank()) return false
        return themes.remove(name) != null
    }

    /**
     * Gets a theme by name.
     *
     * @param name The name of the theme (case-sensitive)
     * @return The theme if found, null otherwise
     */
    fun get(name: String): OverlayTheme? {
        if (name.isBlank()) return null
        return themes[name]
    }

    /**
     * Gets all registered themes.
     *
     * @return List of all registered themes (copy, modifications don't affect registry)
     */
    fun getAll(): List<OverlayTheme> {
        return themes.values.toList()
    }

    /**
     * Gets the currently active theme.
     *
     * @return The current theme (never null)
     */
    fun getCurrent(): OverlayTheme {
        return currentTheme
    }

    /**
     * Sets the current theme by name.
     *
     * @param name The name of the theme to set as current
     * @return true if the theme was found and set, false if not found
     */
    fun setCurrent(name: String): Boolean {
        if (name.isBlank()) return false
        val theme = themes[name]
        return if (theme != null) {
            currentTheme = theme
            true
        } else {
            false
        }
    }

    /**
     * Sets the current theme directly.
     *
     * This allows setting a theme that may not be registered in the registry.
     *
     * @param theme The theme to set as current
     */
    fun setCurrentTheme(theme: OverlayTheme) {
        currentTheme = theme
    }

    /**
     * Resets the registry to default state.
     *
     * - Clears all registered themes
     * - Re-registers default themes (light, dark, high_contrast)
     * - Sets current theme back to light
     *
     * Primarily used for testing.
     */
    fun reset() {
        themes.clear()
        registerDefaults()
        currentTheme = themes["light"] ?: LIGHT
    }

    /**
     * Registers the default themes.
     */
    private fun registerDefaults() {
        themes["light"] = LIGHT
        themes["dark"] = DARK
        themes["high_contrast"] = HIGH_CONTRAST
    }
}
