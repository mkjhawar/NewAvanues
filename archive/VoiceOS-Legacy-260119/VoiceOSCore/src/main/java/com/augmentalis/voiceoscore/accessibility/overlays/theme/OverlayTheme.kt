/**
 * OverlayTheme.kt - Centralized theme configuration for all overlays
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-13
 */
package com.augmentalis.voiceoscore.accessibility.overlays.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceos.accessibility.ElementVoiceState

/**
 * Complete theme configuration for overlay UI
 *
 * Provides centralized control over all visual aspects including:
 * - Colors (backgrounds, text, accents, status)
 * - Typography (font sizes, weights)
 * - Spacing (padding, gaps, margins)
 * - Shapes (corner radii, elevations)
 * - Animations (durations, easing)
 * - Accessibility (contrast, focus indicators)
 */
data class OverlayTheme(
    // ===== COLOR SYSTEM =====

    /** Primary brand/accent color (buttons, highlights, focus) */
    val primaryColor: Color = Color(0xFF2196F3),

    /** Background colors */
    val backgroundColor: Color = Color(0xEE1E1E1E),  // Semi-transparent dark
    val backdropColor: Color = Color.Black.copy(alpha = 0.3f),  // Full-screen dimming

    /** Text colors */
    val textPrimaryColor: Color = Color.White,
    val textSecondaryColor: Color = Color.White.copy(alpha = 0.7f),
    val textDisabledColor: Color = Color.Gray,

    /** Border/divider colors */
    val borderColor: Color = Color.White,
    val dividerColor: Color = Color.White.copy(alpha = 0.1f),

    /** Badge state colors (numbered selection) */
    val badgeEnabledWithNameColor: Color = Color(0xFF4CAF50),   // Green - element has name
    val badgeEnabledNoNameColor: Color = Color(0xFFFF9800),     // Orange - element no name
    val badgeDisabledColor: Color = Color(0xFF9E9E9E),          // Grey - disabled element

    /** Status colors (command feedback) */
    val statusListeningColor: Color = Color(0xFF2196F3),   // Blue
    val statusProcessingColor: Color = Color(0xFFFF9800),  // Orange
    val statusSuccessColor: Color = Color(0xFF4CAF50),     // Green
    val statusErrorColor: Color = Color(0xFFF44336),       // Red

    /** Overlay-specific backgrounds */
    val cardBackgroundColor: Color = Color(0xEE1E1E1E),
    val tooltipBackgroundColor: Color = Color(0xEE000000),

    // ===== TYPOGRAPHY =====

    /** Font sizes */
    val fontSizeTitle: TextUnit = 16.sp,
    val fontSizeBody: TextUnit = 14.sp,
    val fontSizeCaption: TextUnit = 12.sp,
    val fontSizeSmall: TextUnit = 11.sp,
    val fontSizeBadge: TextUnit = 14.sp,
    val fontSizeInstruction: TextUnit = 16.sp,

    /** Font weights */
    val fontWeightBold: FontWeight = FontWeight.Bold,
    val fontWeightMedium: FontWeight = FontWeight.Medium,
    val fontWeightNormal: FontWeight = FontWeight.Normal,

    // ===== SPACING =====

    /** Padding values */
    val paddingSmall: Dp = 4.dp,
    val paddingMedium: Dp = 8.dp,
    val paddingLarge: Dp = 16.dp,
    val paddingXLarge: Dp = 24.dp,

    /** Component spacing */
    val spacingTiny: Dp = 4.dp,
    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 12.dp,
    val spacingLarge: Dp = 16.dp,

    /** Element offsets */
    val badgeOffsetX: Dp = 4.dp,
    val badgeOffsetY: Dp = 4.dp,
    val tooltipOffsetY: Dp = 40.dp,

    // ===== SHAPES =====

    /** Corner radii */
    val cornerRadiusSmall: Dp = 6.dp,
    val cornerRadiusMedium: Dp = 8.dp,
    val cornerRadiusLarge: Dp = 12.dp,
    val cornerRadiusXLarge: Dp = 24.dp,
    val cornerRadiusCircle: Dp = 9999.dp,  // Full circle

    /** Elevations */
    val elevationLow: Dp = 4.dp,
    val elevationMedium: Dp = 8.dp,
    val elevationHigh: Dp = 16.dp,

    /** Border widths */
    val borderWidthThin: Dp = 1.dp,
    val borderWidthMedium: Dp = 2.dp,
    val borderWidthThick: Dp = 3.dp,

    // ===== SIZES =====

    /** Badge dimensions */
    val badgeSize: Dp = 32.dp,
    val badgeNumberSize: Dp = 28.dp,

    /** Icon sizes */
    val iconSizeSmall: Dp = 16.dp,
    val iconSizeMedium: Dp = 24.dp,
    val iconSizeLarge: Dp = 32.dp,

    /** Menu dimensions */
    val menuMinWidth: Dp = 200.dp,
    val menuMaxWidth: Dp = 280.dp,

    /** Tooltip max width */
    val tooltipMaxWidth: Dp = 200.dp,

    // ===== ANIMATIONS =====

    /** Animation durations (milliseconds) */
    val animationDurationFast: Int = 150,
    val animationDurationNormal: Int = 200,
    val animationDurationSlow: Int = 300,

    /** Animation specs */
    val fadeInSpec: AnimationSpec<Float> = tween(animationDurationNormal),
    val fadeOutSpec: AnimationSpec<Float> = tween(animationDurationFast),
    val scaleInSpec: AnimationSpec<Float> = tween(animationDurationNormal),
    val scaleOutSpec: AnimationSpec<Float> = tween(animationDurationFast),

    // ===== ACCESSIBILITY =====

    /** Minimum contrast ratios */
    val minimumContrastRatio: Float = 4.5f,  // WCAG AA standard

    /** Focus indicators */
    val focusIndicatorWidth: Dp = 3.dp,
    val focusIndicatorColor: Color = primaryColor,

    /** Touch target sizes */
    val minimumTouchTargetSize: Dp = 48.dp,  // Android accessibility guideline

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
    val alphaTooltip: Float = 0.9333f,  // 0xEE/0xFF
) {
    /**
     * Create a copy with high contrast accessibility mode
     */
    fun toHighContrast(): OverlayTheme = copy(
        backgroundColor = Color.Black,
        textPrimaryColor = Color.White,
        textSecondaryColor = Color.White,
        borderWidthMedium = 3.dp,
        minimumContrastRatio = 7.0f  // WCAG AAA
    )

    /**
     * Create a copy with custom primary color
     */
    fun withPrimaryColor(color: Color): OverlayTheme = copy(
        primaryColor = color,
        statusListeningColor = color,
        focusIndicatorColor = color
    )

    /**
     * Create a copy with larger text (accessibility)
     */
    fun withLargeText(): OverlayTheme = copy(
        fontSizeTitle = 20.sp,
        fontSizeBody = 18.sp,
        fontSizeCaption = 16.sp,
        fontSizeSmall = 14.sp,
        fontSizeBadge = 18.sp,
        fontSizeInstruction = 20.sp
    )

    /**
     * Create a copy with reduced motion (accessibility)
     */
    fun withReducedMotion(): OverlayTheme = copy(
        animationDurationFast = 0,
        animationDurationNormal = 0,
        animationDurationSlow = 0,
        fadeInSpec = tween(0),
        fadeOutSpec = tween(0),
        scaleInSpec = tween(0),
        scaleOutSpec = tween(0)
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
        if (fontSizeBody < 12.sp) {
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
    private fun calculateContrast(foreground: Color, background: Color): Float {
        val fLuminance = relativeLuminance(foreground)
        val bLuminance = relativeLuminance(background)

        val lighter = maxOf(fLuminance, bLuminance)
        val darker = minOf(fLuminance, bLuminance)

        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Calculate relative luminance for contrast calculation
     */
    private fun relativeLuminance(color: Color): Float {
        val r = if (color.red <= 0.03928f) color.red / 12.92f else Math.pow(((color.red + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        val g = if (color.green <= 0.03928f) color.green / 12.92f else Math.pow(((color.green + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        val b = if (color.blue <= 0.03928f) color.blue / 12.92f else Math.pow(((color.blue + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

        return 0.2126f * r + 0.7152f * g + 0.0722f * b
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
            "✅ Theme passes accessibility validation"
        } else {
            "❌ Theme validation failed:\n" + errors.joinToString("\n") { "  - $it" }
        }
    }
}

// ElementVoiceState is now imported from voiceos-accessibility-types library
