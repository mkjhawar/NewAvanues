/**
 * NumberOverlayStyle.kt - Configuration for number overlay badges
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.overlays

import android.graphics.Color
import android.graphics.Typeface
import com.augmentalis.voiceos.accessibility.AnchorPoint
import com.augmentalis.voiceos.accessibility.BadgeStyle
import com.augmentalis.voiceos.accessibility.ElementVoiceState

/**
 * Style configuration for number overlay badges
 *
 * Provides Material 3 design system colors and positioning for circular badges
 */
data class NumberOverlayStyle(
    // Position configuration
    val anchorPoint: AnchorPoint = AnchorPoint.TOP_RIGHT,
    val offsetX: Int = 4,  // pixels from element edge
    val offsetY: Int = 4,  // pixels from element edge

    // Circle dimensions
    val circleRadius: Int = 16,  // 32dp diameter (16dp radius)
    val strokeWidth: Float = 2f,

    // Material 3 colors (state-based)
    val hasNameColor: Int = Color.parseColor("#4CAF50"),      // Material Green 500 - has command name
    val noNameColor: Int = Color.parseColor("#FF9800"),       // Material Orange 500 - no command name
    val disabledColor: Int = Color.parseColor("#9E9E9E"),     // Material Grey 500 - disabled element

    // Number text styling
    val numberColor: Int = Color.WHITE,
    val numberSize: Float = 14f,  // sp
    val fontWeight: Typeface = Typeface.DEFAULT_BOLD,

    // Visual effects
    val dropShadow: Boolean = true,
    val shadowRadius: Float = 4f,
    val shadowColor: Int = Color.parseColor("#40000000"),  // 25% black
    val shadowOffsetY: Float = 2f,

    // Badge style variants
    val badgeStyle: BadgeStyle = BadgeStyle.FILLED_CIRCLE
)

// AnchorPoint, BadgeStyle, and ElementVoiceState enums are now imported from voiceos-accessibility-types library

/**
 * Predefined style configurations
 */
object NumberOverlayStyles {

    /**
     * Default Material 3 style with green/orange/grey colors
     */
    val DEFAULT = NumberOverlayStyle()

    /**
     * High contrast style for accessibility
     */
    val HIGH_CONTRAST = NumberOverlayStyle(
        hasNameColor = Color.BLACK,
        noNameColor = Color.parseColor("#FF6600"),  // Darker orange
        disabledColor = Color.parseColor("#424242"), // Darker grey
        numberColor = Color.WHITE,
        strokeWidth = 3f,  // Thicker border
        circleRadius = 20,  // Larger for better visibility
        shadowRadius = 6f
    )

    /**
     * Large text mode for visual impairment
     */
    val LARGE_TEXT = NumberOverlayStyle(
        numberSize = 20f,      // Larger font
        circleRadius = 24,     // Larger circle
        strokeWidth = 3f,
        offsetX = 6,
        offsetY = 6
    )

    /**
     * Minimal style with subtle colors
     */
    val MINIMAL = NumberOverlayStyle(
        hasNameColor = Color.parseColor("#81C784"),    // Lighter green
        noNameColor = Color.parseColor("#FFB74D"),     // Lighter orange
        disabledColor = Color.parseColor("#BDBDBD"),   // Lighter grey
        dropShadow = false,
        strokeWidth = 1f
    )

    /**
     * Outlined style (hollow circles)
     */
    val OUTLINED = NumberOverlayStyle(
        badgeStyle = BadgeStyle.OUTLINED_CIRCLE,
        hasNameColor = Color.parseColor("#4CAF50"),
        noNameColor = Color.parseColor("#FF9800"),
        numberColor = Color.parseColor("#4CAF50"),  // Number matches outline
        strokeWidth = 3f,
        dropShadow = false
    )

    /**
     * Square badges
     */
    val SQUARE = NumberOverlayStyle(
        badgeStyle = BadgeStyle.SQUARE,
        circleRadius = 14  // Used as half-width for square
    )

    /**
     * Rounded rectangle badges
     */
    val ROUNDED_RECT = NumberOverlayStyle(
        badgeStyle = BadgeStyle.ROUNDED_RECT,
        circleRadius = 16
    )

    /**
     * Dark mode optimized style
     */
    val DARK_MODE = NumberOverlayStyle(
        hasNameColor = Color.parseColor("#66BB6A"),    // Slightly lighter green for dark backgrounds
        noNameColor = Color.parseColor("#FFA726"),     // Slightly lighter orange
        disabledColor = Color.parseColor("#757575"),   // Mid grey
        numberColor = Color.parseColor("#E0E0E0"),     // Off-white for better contrast
        shadowColor = Color.parseColor("#80000000")    // 50% black shadow
    )

    /**
     * Light mode optimized style
     */
    val LIGHT_MODE = NumberOverlayStyle(
        hasNameColor = Color.parseColor("#43A047"),    // Slightly darker green
        noNameColor = Color.parseColor("#FB8C00"),     // Slightly darker orange
        disabledColor = Color.parseColor("#9E9E9E"),
        numberColor = Color.WHITE,
        shadowColor = Color.parseColor("#40000000")    // 25% black shadow
    )

    /**
     * Colorblind-friendly style (using shapes instead of just colors)
     */
    val COLORBLIND_FRIENDLY = NumberOverlayStyle(
        hasNameColor = Color.parseColor("#0072B2"),    // Blue (instead of green)
        noNameColor = Color.parseColor("#E69F00"),     // Gold/amber
        disabledColor = Color.parseColor("#999999"),   // Grey
        badgeStyle = BadgeStyle.ROUNDED_RECT,          // Different shape to differentiate
        strokeWidth = 3f
    )
}
