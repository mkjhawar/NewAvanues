/**
 * OverlayTheme.kt - Centralized theme configuration for all overlays
 *
 * KMP-compatible version without platform-specific dependencies.
 * Uses Long for colors (ARGB format) and Float for dimensions.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscore

import kotlin.math.pow

/**
 * Complete theme configuration for overlay UI
 *
 * Provides centralized control over all visual aspects including:
 * - Colors (backgrounds, text, accents, status) as ARGB Long values
 * - Typography (font sizes in sp as Float)
 * - Spacing (padding, gaps, margins in dp as Float)
 * - Shapes (corner radii, elevations)
 * - Animations (durations in ms)
 * - Accessibility (contrast, focus indicators)
 *
 * Color values are stored as Long in ARGB format: 0xAARRGGBB
 * Dimension values are stored as Float representing dp or sp units
 */
data class OverlayTheme(
    // ===== COLOR SYSTEM (ARGB Long format) =====

    /** Primary brand/accent color (buttons, highlights, focus) */
    val primaryColor: Long = 0xFF2196F3,

    /** Background colors */
    val backgroundColor: Long = 0xEE1E1E1E,  // Semi-transparent dark
    val backdropColor: Long = 0x4D000000,    // Full-screen dimming (0.3 alpha)

    /** Text colors */
    val textPrimaryColor: Long = 0xFFFFFFFF,
    val textSecondaryColor: Long = 0xB3FFFFFF,  // White with 0.7 alpha
    val textDisabledColor: Long = 0xFF808080,   // Gray

    /** Border/divider colors */
    val borderColor: Long = 0xFFFFFFFF,
    val dividerColor: Long = 0x1AFFFFFF,  // White with 0.1 alpha

    /** Badge state colors (numbered selection) */
    val badgeEnabledWithNameColor: Long = 0xFF4CAF50,   // Green - element has name
    val badgeEnabledNoNameColor: Long = 0xFFFF9800,     // Orange - element no name
    val badgeDisabledColor: Long = 0xFF9E9E9E,          // Grey - disabled element

    /** Status colors (command feedback) */
    val statusListeningColor: Long = 0xFF2196F3,   // Blue
    val statusProcessingColor: Long = 0xFFFF9800,  // Orange
    val statusSuccessColor: Long = 0xFF4CAF50,     // Green
    val statusErrorColor: Long = 0xFFF44336,       // Red

    /** Overlay-specific backgrounds */
    val cardBackgroundColor: Long = 0xEE1E1E1E,
    val tooltipBackgroundColor: Long = 0xEE000000,

    /** Focus indicator color (defaults to primary color) */
    val focusIndicatorColor: Long = 0xFF2196F3,

    // ===== TYPOGRAPHY (sp as Float) =====

    /** Font sizes in sp */
    val titleFontSize: Float = 16f,
    val bodyFontSize: Float = 14f,
    val captionFontSize: Float = 12f,
    val smallFontSize: Float = 11f,
    val badgeFontSize: Float = 14f,
    val instructionFontSize: Float = 16f,

    // ===== SPACING (dp as Float) =====

    /** Padding values */
    val paddingSmall: Float = 4f,
    val paddingMedium: Float = 8f,
    val paddingLarge: Float = 16f,
    val paddingXLarge: Float = 24f,

    /** Component spacing */
    val spacingTiny: Float = 4f,
    val spacingSmall: Float = 8f,
    val spacingMedium: Float = 12f,
    val spacingLarge: Float = 16f,

    /** Element offsets */
    val badgeOffsetX: Float = 4f,
    val badgeOffsetY: Float = 4f,
    val tooltipOffsetY: Float = 40f,

    // ===== SHAPES (dp as Float) =====

    /** Corner radii */
    val cornerRadiusSmall: Float = 6f,
    val cornerRadiusMedium: Float = 8f,
    val cornerRadiusLarge: Float = 12f,
    val cornerRadiusXLarge: Float = 24f,
    val cornerRadiusCircle: Float = 9999f,  // Full circle

    /** Elevations */
    val elevationLow: Float = 4f,
    val elevationMedium: Float = 8f,
    val elevationHigh: Float = 16f,

    /** Border widths */
    val borderWidthThin: Float = 1f,
    val borderWidthMedium: Float = 2f,
    val borderWidthThick: Float = 3f,

    // ===== SIZES (dp as Float) =====

    /** Badge dimensions */
    val badgeSize: Float = 32f,
    val badgeNumberSize: Float = 28f,

    /** Icon sizes */
    val iconSizeSmall: Float = 16f,
    val iconSizeMedium: Float = 24f,
    val iconSizeLarge: Float = 32f,

    /** Menu dimensions */
    val menuMinWidth: Float = 200f,
    val menuMaxWidth: Float = 280f,

    /** Tooltip max width */
    val tooltipMaxWidth: Float = 200f,

    // ===== ANIMATIONS (milliseconds) =====

    /** Animation durations (milliseconds) */
    val animationDurationFast: Int = 150,
    val animationDurationNormal: Int = 200,
    val animationDurationSlow: Int = 300,

    /** Animation enabled flag */
    val animationEnabled: Boolean = true,

    // ===== ACCESSIBILITY =====

    /** Minimum contrast ratios */
    val minimumContrastRatio: Float = 4.5f,  // WCAG AA standard

    /** Focus indicators */
    val focusIndicatorWidth: Float = 3f,

    /** Touch target sizes */
    val minimumTouchTargetSize: Float = 48f,  // Android accessibility guideline

    // ===== VOICE FEEDBACK =====

    /** Instruction text defaults */
    val instructionTextDefault: String = "Say a number to select",
    val instructionTextMultiple: String = "Say number or command name",

    // ===== OPACITY VALUES =====

    /** Standard alpha values for consistency */
    val alphaDisabled: Float = 0.5f,
    val alphaSecondary: Float = 0.7f,
    val alphaHint: Float = 0.6f,
    val alphaDivider: Float = 0.1f,
    val alphaBackdrop: Float = 0.3f,
    val alphaTooltip: Float = 0.9333f  // 0xEE/0xFF
) {
    /**
     * Create a copy with high contrast accessibility mode
     */
    fun toHighContrast(): OverlayTheme = copy(
        backgroundColor = 0xFF000000,
        textPrimaryColor = 0xFFFFFFFF,
        textSecondaryColor = 0xFFFFFFFF,
        borderWidthMedium = 3f,
        minimumContrastRatio = 7.0f  // WCAG AAA
    )

    /**
     * Create a copy with custom primary color
     */
    fun withPrimaryColor(color: Long): OverlayTheme = copy(
        primaryColor = color,
        statusListeningColor = color,
        focusIndicatorColor = color
    )

    /**
     * Create a copy with larger text (accessibility)
     */
    fun withLargeText(): OverlayTheme = copy(
        titleFontSize = 20f,
        bodyFontSize = 18f,
        captionFontSize = 16f,
        smallFontSize = 14f,
        badgeFontSize = 18f,
        instructionFontSize = 20f
    )

    /**
     * Create a copy with reduced motion (accessibility)
     */
    fun withReducedMotion(): OverlayTheme = copy(
        animationDurationFast = 0,
        animationDurationNormal = 0,
        animationDurationSlow = 0,
        animationEnabled = false
    )

    /**
     * Validate theme meets accessibility standards
     */
    fun validate(): ThemeValidationResult {
        val errors = mutableListOf<String>()

        // Check contrast ratios (simplified check)
        if (calculateContrast(textPrimaryColor, backgroundColor) < minimumContrastRatio) {
            errors.add("Primary text does not meet minimum contrast ratio")
        }

        // Check touch targets
        if (badgeSize < minimumTouchTargetSize) {
            errors.add("Badge size below minimum touch target (${minimumTouchTargetSize}dp)")
        }

        // Check font sizes
        if (bodyFontSize < 12f) {
            errors.add("Body font size too small (< 12sp)")
        }

        return ThemeValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Calculate approximate contrast ratio between two colors
     * Simplified version - production should use WCAG formula
     */
    private fun calculateContrast(foreground: Long, background: Long): Float {
        val fLuminance = relativeLuminance(foreground)
        val bLuminance = relativeLuminance(background)

        val lighter = maxOf(fLuminance, bLuminance)
        val darker = minOf(fLuminance, bLuminance)

        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Calculate relative luminance for contrast calculation
     */
    private fun relativeLuminance(color: Long): Float {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f

        val rL = if (r <= 0.03928f) r / 12.92f else ((r + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        val gL = if (g <= 0.03928f) g / 12.92f else ((g + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        val bL = if (b <= 0.03928f) b / 12.92f else ((b + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

        return 0.2126f * rL + 0.7152f * gL + 0.0722f * bL
    }

    companion object {
        /** Default theme instance */
        val DEFAULT = OverlayTheme()

        /** Light theme variant */
        val LIGHT = OverlayTheme(
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

        /** Dark theme variant */
        val DARK = OverlayTheme(
            backgroundColor = 0xFF121212,
            cardBackgroundColor = 0xFF1E1E1E
        )

        /** High contrast theme for accessibility */
        val HIGH_CONTRAST = OverlayTheme().toHighContrast()
    }
}

/**
 * Result of theme validation
 */
data class ThemeValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    override fun toString(): String {
        return if (isValid) {
            "Theme passes accessibility validation"
        } else {
            "Theme validation failed:\n" + errors.joinToString("\n") { "  - $it" }
        }
    }
}

// ===== COLOR UTILITY FUNCTIONS =====

/**
 * Create a color with modified alpha value
 * @param color Original color in 0xAARRGGBB format
 * @param alpha Alpha value (0.0 to 1.0)
 * @return New color with modified alpha
 */
fun colorWithAlpha(color: Long, alpha: Float): Long {
    val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)
    return (color and 0x00FFFFFF) or (alphaInt.toLong() shl 24)
}

/**
 * Extract alpha component from color (0.0 to 1.0)
 */
fun extractAlpha(color: Long): Float {
    return ((color shr 24) and 0xFF).toFloat() / 255f
}

/**
 * Extract red component from color (0.0 to 1.0)
 */
fun extractRed(color: Long): Float {
    return ((color shr 16) and 0xFF).toFloat() / 255f
}

/**
 * Extract green component from color (0.0 to 1.0)
 */
fun extractGreen(color: Long): Float {
    return ((color shr 8) and 0xFF).toFloat() / 255f
}

/**
 * Extract blue component from color (0.0 to 1.0)
 */
fun extractBlue(color: Long): Float {
    return (color and 0xFF).toFloat() / 255f
}
