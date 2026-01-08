/**
 * OverlayThemes.kt - Predefined theme presets for common use cases
 *
 * KMP-compatible version providing predefined overlay themes.
 * Includes accessibility-focused themes (HighContrast, LargeText, ColorblindFriendly).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.features

/**
 * Collection of predefined overlay themes
 *
 * Usage:
 * ```kotlin
 * // Use predefined theme by name
 * val theme = OverlayThemes.getTheme("Material3Dark")
 *
 * // Get default theme
 * val default = OverlayThemes.getDefault()
 *
 * // List all available themes
 * val names = OverlayThemes.getThemeNames()
 * ```
 */
object OverlayThemes {

    /**
     * Material 3 Dark Theme (Default)
     *
     * Modern Material Design 3 aesthetic with semi-transparent dark backgrounds
     * and vibrant accent colors. Best for general use on AOSP/Android devices.
     *
     * - Semi-transparent dark backgrounds (allows context visibility)
     * - Material 3 color system
     * - Smooth animations
     * - Balanced spacing
     */
    private val Material3Dark = OverlayTheme(
        // Colors from Material 3 dark theme
        primaryColor = 0xFF2196F3,  // Blue
        backgroundColor = 0xEE1E1E1E,
        backdropColor = 0x4D000000,  // Black with 0.3 alpha
        textPrimaryColor = 0xFFFFFFFF,
        textSecondaryColor = 0xB3FFFFFF,  // White with 0.7 alpha

        // State colors
        badgeEnabledWithNameColor = 0xFF4CAF50,  // Green
        badgeEnabledNoNameColor = 0xFFFF9800,    // Orange
        statusSuccessColor = 0xFF4CAF50,
        statusErrorColor = 0xFFF44336,

        // Standard Material spacing
        paddingLarge = 16f,
        cornerRadiusLarge = 12f,
        elevationHigh = 16f,

        // Default animations
        animationDurationNormal = 200,
        animationDurationFast = 150
    )

    /**
     * High Contrast Theme (Accessibility)
     *
     * Maximum contrast for users with vision impairments.
     * Meets WCAG AAA standards (7:1 contrast ratio).
     *
     * - Pure black/white backgrounds
     * - No transparency
     * - Thicker borders
     * - Larger touch targets
     */
    private val HighContrast = OverlayTheme(
        // Maximum contrast
        primaryColor = 0xFF00BFFF,  // Bright cyan
        backgroundColor = 0xFF000000,  // Pure black
        backdropColor = 0xFF000000,    // No transparency
        textPrimaryColor = 0xFFFFFFFF,  // Pure white
        textSecondaryColor = 0xFFFFFFFF,

        // High contrast state colors
        badgeEnabledWithNameColor = 0xFF00FF00,  // Bright green
        badgeEnabledNoNameColor = 0xFFFFAA00,    // Bright orange
        statusSuccessColor = 0xFF00FF00,
        statusErrorColor = 0xFFFF0000,

        // Thicker borders for visibility
        borderWidthMedium = 3f,
        borderWidthThick = 4f,

        // Larger text
        titleFontSize = 20f,
        bodyFontSize = 18f,
        badgeFontSize = 18f,

        // Larger touch targets
        badgeSize = 48f,
        minimumTouchTargetSize = 56f,

        // WCAG AAA standard
        minimumContrastRatio = 7.0f
    )

    /**
     * Minimalist Theme
     *
     * Clean, understated design with minimal visual clutter.
     * Monochromatic with subtle accents.
     *
     * - Muted colors
     * - Thin borders
     * - Smaller badges
     * - Faster animations
     */
    private val Minimalist = OverlayTheme(
        // Muted monochrome palette
        primaryColor = 0xFF9E9E9E,  // Grey
        backgroundColor = 0xCC000000,  // Less opaque
        backdropColor = 0x33000000,    // Subtle dimming (0.2 alpha)
        textPrimaryColor = 0xFFFFFFFF,
        textSecondaryColor = 0x99FFFFFF,  // 0.6 alpha

        // Subtle state colors
        badgeEnabledWithNameColor = 0xFFBBBBBB,
        badgeEnabledNoNameColor = 0xFF999999,
        statusSuccessColor = 0xFF888888,
        statusErrorColor = 0xFFAAAAAA,

        // Minimal spacing
        paddingMedium = 6f,
        paddingLarge = 12f,

        // Smaller components
        badgeSize = 28f,
        iconSizeMedium = 20f,

        // Subtle shapes
        cornerRadiusMedium = 6f,
        elevationMedium = 4f,

        // Fast animations
        animationDurationFast = 100,
        animationDurationNormal = 150
    )

    /**
     * Gaming Theme
     *
     * High-energy theme with neon accents and bold colors.
     * Inspired by gaming UI/HUDs.
     *
     * - Neon accent colors
     * - Dark backgrounds with glowing elements
     * - Sharp corners
     * - Snappy animations
     */
    private val Gaming = OverlayTheme(
        // Neon color scheme
        primaryColor = 0xFF00FF41,  // Neon green
        backgroundColor = 0xEE0A0A0A,  // Very dark
        backdropColor = 0x80000000,    // Darker dimming (0.5 alpha)
        textPrimaryColor = 0xFF00FF41,  // Neon green text
        textSecondaryColor = 0xB300FF41,  // Neon green with 0.7 alpha

        // Vibrant state colors
        badgeEnabledWithNameColor = 0xFF00FF41,  // Neon green
        badgeEnabledNoNameColor = 0xFFFF00FF,    // Neon magenta
        statusSuccessColor = 0xFF00FF41,
        statusErrorColor = 0xFFFF0066,  // Neon red

        // Sharp corners (not rounded)
        cornerRadiusSmall = 2f,
        cornerRadiusMedium = 4f,
        cornerRadiusLarge = 6f,

        // Glowing borders
        borderWidthMedium = 2f,
        borderColor = 0xFF00FF41,

        // High elevation for depth
        elevationHigh = 24f,

        // Snappy animations
        animationDurationFast = 100,
        animationDurationNormal = 150
    )

    /**
     * Professional Theme
     *
     * Formal, corporate-friendly design.
     * Navy blue and grey with conservative styling.
     *
     * - Navy blue accent
     * - Neutral grey backgrounds
     * - Conservative spacing
     * - Smooth but not flashy animations
     */
    private val Professional = OverlayTheme(
        // Corporate color scheme
        primaryColor = 0xFF1976D2,  // Navy blue
        backgroundColor = 0xEE2C2C2C,  // Dark grey
        backdropColor = 0x66000000,    // 0.4 alpha
        textPrimaryColor = 0xFFFFFFFF,
        textSecondaryColor = 0xFFB0B0B0,  // Light grey

        // Professional state colors
        badgeEnabledWithNameColor = 0xFF1976D2,  // Navy
        badgeEnabledNoNameColor = 0xFF757575,    // Grey
        statusSuccessColor = 0xFF388E3C,  // Dark green
        statusErrorColor = 0xFFD32F2F,    // Dark red

        // Conservative spacing
        paddingLarge = 20f,
        paddingXLarge = 28f,

        // Refined corners
        cornerRadiusMedium = 10f,
        cornerRadiusLarge = 14f,

        // Moderate elevation
        elevationMedium = 8f,
        elevationHigh = 16f,

        // Smooth animations
        animationDurationNormal = 250,
        animationDurationSlow = 350
    )

    /**
     * Material 3 Light Theme
     *
     * Light backgrounds for daytime use.
     * Inverted color scheme from Material3Dark.
     */
    private val Material3Light = OverlayTheme(
        // Light theme colors
        primaryColor = 0xFF1976D2,  // Blue
        backgroundColor = 0xEEFFFFFF,  // White with slight transparency
        backdropColor = 0x4DFFFFFF,    // White with 0.3 alpha
        textPrimaryColor = 0xFF000000,  // Black
        textSecondaryColor = 0xB3000000,  // Black with 0.7 alpha

        // Inverted state colors for light background
        badgeEnabledWithNameColor = 0xFF2E7D32,  // Dark green
        badgeEnabledNoNameColor = 0xFFF57C00,    // Dark orange
        statusSuccessColor = 0xFF2E7D32,
        statusErrorColor = 0xFFC62828,

        // Standard Material spacing
        paddingLarge = 16f,
        cornerRadiusLarge = 12f,
        elevationHigh = 16f
    )

    /**
     * Large Text Theme (Accessibility)
     *
     * Theme optimized for users who need larger text.
     * Based on Material3Dark but with increased font sizes.
     */
    private val LargeText = OverlayTheme(
        // Same colors as Material3Dark
        primaryColor = 0xFF2196F3,
        backgroundColor = 0xEE1E1E1E,
        backdropColor = 0x4D000000,
        textPrimaryColor = 0xFFFFFFFF,
        textSecondaryColor = 0xB3FFFFFF,

        badgeEnabledWithNameColor = 0xFF4CAF50,
        badgeEnabledNoNameColor = 0xFFFF9800,
        statusSuccessColor = 0xFF4CAF50,
        statusErrorColor = 0xFFF44336,

        // Larger font sizes
        titleFontSize = 22f,
        bodyFontSize = 18f,
        captionFontSize = 16f,
        smallFontSize = 14f,
        badgeFontSize = 18f,
        instructionFontSize = 22f,

        // Larger components for accessibility
        badgeSize = 40f,
        iconSizeMedium = 28f,
        iconSizeLarge = 40f,

        // Larger touch targets
        minimumTouchTargetSize = 52f
    )

    /**
     * Colorblind Friendly Theme (Accessibility)
     *
     * Uses a palette safe for common colorblindness types.
     * Avoids red-green conflicts, uses blue-orange as primary contrast.
     */
    private val ColorblindFriendly = OverlayTheme(
        // Blue-orange safe palette
        primaryColor = 0xFF0072B2,  // Blue (colorblind safe)
        backgroundColor = 0xEE1E1E1E,
        backdropColor = 0x4D000000,
        textPrimaryColor = 0xFFFFFFFF,
        textSecondaryColor = 0xB3FFFFFF,

        // Colorblind-safe state colors (no red-green)
        // Using blue/orange/yellow which are distinguishable by most colorblind types
        badgeEnabledWithNameColor = 0xFF009E73,  // Teal (distinguishable from orange)
        badgeEnabledNoNameColor = 0xFFE69F00,    // Orange/amber
        statusSuccessColor = 0xFF009E73,          // Teal instead of green
        statusErrorColor = 0xFFCC79A7,            // Pink/magenta instead of red

        // Status colors that contrast well
        statusListeningColor = 0xFF0072B2,   // Blue
        statusProcessingColor = 0xFFF0E442,  // Yellow

        // Standard spacing
        paddingLarge = 16f,
        cornerRadiusLarge = 12f
    )

    // Theme registry for lookup
    private val themes = mapOf(
        "Material3Dark" to Material3Dark,
        "HighContrast" to HighContrast,
        "Minimalist" to Minimalist,
        "Gaming" to Gaming,
        "Professional" to Professional,
        "Material3Light" to Material3Light,
        "LargeText" to LargeText,
        "ColorblindFriendly" to ColorblindFriendly
    )

    // Alias mappings for convenience
    private val aliases = mapOf(
        "default" to "Material3Dark",
        "accessibility" to "HighContrast",
        "minimal" to "Minimalist",
        "neon" to "Gaming",
        "corporate" to "Professional",
        "light" to "Material3Light",
        "largetext" to "LargeText",
        "colorblind" to "ColorblindFriendly"
    )

    /**
     * Get theme by name (case-insensitive, supports aliases)
     *
     * @param name Theme name or alias
     * @return The requested theme, or Material3Dark if not found
     */
    fun getTheme(name: String): OverlayTheme {
        val normalizedName = name.lowercase()

        // Check for exact match (case-insensitive)
        val exactMatch = themes.entries.find { it.key.lowercase() == normalizedName }
        if (exactMatch != null) {
            return exactMatch.value
        }

        // Check for alias
        val aliasedName = aliases[normalizedName]
        if (aliasedName != null) {
            return themes[aliasedName] ?: getDefault()
        }

        // Return default
        return getDefault()
    }

    /**
     * Get all available theme names (excluding aliases)
     *
     * @return List of canonical theme names
     */
    fun getThemeNames(): List<String> = listOf(
        "Material3Dark",
        "HighContrast",
        "Minimalist",
        "Gaming",
        "Professional",
        "Material3Light"
    )

    /**
     * Get the default theme (Material3Dark)
     *
     * @return Default overlay theme
     */
    fun getDefault(): OverlayTheme = Material3Dark

    /**
     * Get theme descriptions for UI display
     *
     * @return Map of theme names to descriptions
     */
    fun getThemeDescriptions(): Map<String, String> = mapOf(
        "Material3Dark" to "Modern Material Design 3 dark theme (Default)",
        "HighContrast" to "Maximum contrast for accessibility (WCAG AAA)",
        "Minimalist" to "Clean, understated design with minimal clutter",
        "Gaming" to "High-energy neon aesthetic inspired by gaming HUDs",
        "Professional" to "Formal corporate design with navy blue accents",
        "Material3Light" to "Light backgrounds for daytime use"
    )

    /**
     * Get all themes including accessibility variants
     *
     * @return Map of all theme names to themes
     */
    fun getAllThemes(): Map<String, OverlayTheme> = themes.toMap()

    /**
     * Get accessibility-focused themes
     *
     * @return Map of accessibility theme names to themes
     */
    fun getAccessibilityThemes(): Map<String, OverlayTheme> = mapOf(
        "HighContrast" to HighContrast,
        "LargeText" to LargeText,
        "ColorblindFriendly" to ColorblindFriendly
    )
}
