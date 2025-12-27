/**
 * OverlayThemes.kt - Predefined theme presets for common use cases
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-13
 */
package com.augmentalis.voiceoscore.accessibility.overlays.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Collection of predefined overlay themes
 *
 * Usage:
 * ```kotlin
 * // Use predefined theme
 * val theme = OverlayThemes.Material3Dark
 *
 * // Or customize
 * val customTheme = OverlayThemes.Minimalist.withPrimaryColor(Color.Green)
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
    val Material3Dark = OverlayTheme(
        // Colors from Material 3 dark theme
        primaryColor = Color(0xFF2196F3),  // Blue
        backgroundColor = Color(0xEE1E1E1E),
        backdropColor = Color.Black.copy(alpha = 0.3f),
        textPrimaryColor = Color.White,
        textSecondaryColor = Color.White.copy(alpha = 0.7f),

        // State colors
        badgeEnabledWithNameColor = Color(0xFF4CAF50),  // Green
        badgeEnabledNoNameColor = Color(0xFFFF9800),    // Orange
        statusSuccessColor = Color(0xFF4CAF50),
        statusErrorColor = Color(0xFFF44336),

        // Standard Material spacing
        paddingLarge = 16.dp,
        cornerRadiusLarge = 12.dp,
        elevationHigh = 16.dp,

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
    val HighContrast = OverlayTheme(
        // Maximum contrast
        primaryColor = Color(0xFF00BFFF),  // Bright cyan
        backgroundColor = Color.Black,      // Pure black
        backdropColor = Color.Black,        // No transparency
        textPrimaryColor = Color.White,     // Pure white
        textSecondaryColor = Color.White,

        // High contrast state colors
        badgeEnabledWithNameColor = Color(0xFF00FF00),  // Bright green
        badgeEnabledNoNameColor = Color(0xFFFFAA00),    // Bright orange
        statusSuccessColor = Color(0xFF00FF00),
        statusErrorColor = Color(0xFFFF0000),

        // Thicker borders for visibility
        borderWidthMedium = 3.dp,
        borderWidthThick = 4.dp,

        // Larger text
        fontSizeTitle = 20.sp,
        fontSizeBody = 18.sp,
        fontSizeBadge = 18.sp,

        // Larger touch targets
        badgeSize = 48.dp,
        minimumTouchTargetSize = 56.dp,

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
    val Minimalist = OverlayTheme(
        // Muted monochrome palette
        primaryColor = Color(0xFF9E9E9E),  // Grey
        backgroundColor = Color(0xCC000000),  // Less opaque
        backdropColor = Color.Black.copy(alpha = 0.2f),  // Subtle dimming
        textPrimaryColor = Color.White,
        textSecondaryColor = Color.White.copy(alpha = 0.6f),

        // Subtle state colors
        badgeEnabledWithNameColor = Color(0xFFBBBBBB),
        badgeEnabledNoNameColor = Color(0xFF999999),
        statusSuccessColor = Color(0xFF888888),
        statusErrorColor = Color(0xFFAAAAAA),

        // Minimal spacing
        paddingMedium = 6.dp,
        paddingLarge = 12.dp,

        // Smaller components
        badgeSize = 28.dp,
        iconSizeMedium = 20.dp,

        // Subtle shapes
        cornerRadiusMedium = 6.dp,
        elevationMedium = 4.dp,

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
    val Gaming = OverlayTheme(
        // Neon color scheme
        primaryColor = Color(0xFF00FF41),  // Neon green
        backgroundColor = Color(0xEE0A0A0A),  // Very dark
        backdropColor = Color.Black.copy(alpha = 0.5f),  // Darker dimming
        textPrimaryColor = Color(0xFF00FF41),  // Neon green text
        textSecondaryColor = Color(0xFF00FF41).copy(alpha = 0.7f),

        // Vibrant state colors
        badgeEnabledWithNameColor = Color(0xFF00FF41),  // Neon green
        badgeEnabledNoNameColor = Color(0xFFFF00FF),    // Neon magenta
        statusSuccessColor = Color(0xFF00FF41),
        statusErrorColor = Color(0xFFFF0066),  // Neon red

        // Sharp corners (not rounded)
        cornerRadiusSmall = 2.dp,
        cornerRadiusMedium = 4.dp,
        cornerRadiusLarge = 6.dp,

        // Glowing borders
        borderWidthMedium = 2.dp,
        borderColor = Color(0xFF00FF41),

        // High elevation for depth
        elevationHigh = 24.dp,

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
    val Professional = OverlayTheme(
        // Corporate color scheme
        primaryColor = Color(0xFF1976D2),  // Navy blue
        backgroundColor = Color(0xEE2C2C2C),  // Dark grey
        backdropColor = Color.Black.copy(alpha = 0.4f),
        textPrimaryColor = Color.White,
        textSecondaryColor = Color(0xFFB0B0B0),  // Light grey

        // Professional state colors
        badgeEnabledWithNameColor = Color(0xFF1976D2),  // Navy
        badgeEnabledNoNameColor = Color(0xFF757575),    // Grey
        statusSuccessColor = Color(0xFF388E3C),  // Dark green
        statusErrorColor = Color(0xFFD32F2F),    // Dark red

        // Conservative spacing
        paddingLarge = 20.dp,
        paddingXLarge = 28.dp,

        // Refined corners
        cornerRadiusMedium = 10.dp,
        cornerRadiusLarge = 14.dp,

        // Moderate elevation
        elevationMedium = 8.dp,
        elevationHigh = 16.dp,

        // Smooth animations
        animationDurationNormal = 250,
        animationDurationSlow = 350
    )

    /**
     * Light Mode Theme (Future)
     *
     * Light backgrounds for daytime use.
     * Currently returns Material3Dark - implement when needed.
     */
    val Material3Light = OverlayTheme(
        // Light theme colors
        primaryColor = Color(0xFF1976D2),  // Blue
        backgroundColor = Color(0xEEFFFFFF),  // White with slight transparency
        backdropColor = Color.White.copy(alpha = 0.3f),
        textPrimaryColor = Color.Black,
        textSecondaryColor = Color.Black.copy(alpha = 0.7f),

        // Inverted state colors for light background
        badgeEnabledWithNameColor = Color(0xFF2E7D32),  // Dark green
        badgeEnabledNoNameColor = Color(0xFFF57C00),    // Dark orange
        statusSuccessColor = Color(0xFF2E7D32),
        statusErrorColor = Color(0xFFC62828),

        // Standard Material spacing
        paddingLarge = 16.dp,
        cornerRadiusLarge = 12.dp,
        elevationHigh = 16.dp
    )

    /**
     * Get theme by name (for user preferences)
     */
    fun getTheme(name: String): OverlayTheme = when (name.lowercase()) {
        "material3dark", "default" -> Material3Dark
        "highcontrast", "accessibility" -> HighContrast
        "minimalist", "minimal" -> Minimalist
        "gaming", "neon" -> Gaming
        "professional", "corporate" -> Professional
        "material3light", "light" -> Material3Light
        else -> Material3Dark
    }

    /**
     * Get all available theme names
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
     * Get theme descriptions for UI
     */
    fun getThemeDescriptions(): Map<String, String> = mapOf(
        "Material3Dark" to "Modern Material Design 3 dark theme (Default)",
        "HighContrast" to "Maximum contrast for accessibility (WCAG AAA)",
        "Minimalist" to "Clean, understated design with minimal clutter",
        "Gaming" to "High-energy neon aesthetic inspired by gaming HUDs",
        "Professional" to "Formal corporate design with navy blue accents",
        "Material3Light" to "Light backgrounds for daytime use"
    )
}
