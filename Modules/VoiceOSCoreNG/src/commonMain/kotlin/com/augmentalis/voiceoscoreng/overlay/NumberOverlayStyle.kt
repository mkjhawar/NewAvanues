/**
 * NumberOverlayStyle.kt - Styling system for number overlay badges
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * Provides styling configuration for number overlay badges displayed on UI elements
 * for voice control navigation.
 */
package com.augmentalis.voiceoscoreng.overlay

/**
 * Badge shape for number overlays.
 */
enum class BadgeShape {
    /** Filled circle badge */
    FILLED_CIRCLE,
    /** Circle with stroke outline */
    OUTLINED_CIRCLE,
    /** Square badge */
    SQUARE,
    /** Rounded rectangle badge */
    ROUNDED_RECT
}

/**
 * Anchor point for positioning badges relative to elements.
 */
enum class AnchorPoint {
    /** Top-left corner of element */
    TOP_LEFT,
    /** Top-right corner of element */
    TOP_RIGHT,
    /** Bottom-left corner of element */
    BOTTOM_LEFT,
    /** Bottom-right corner of element */
    BOTTOM_RIGHT
}

/**
 * Complete styling configuration for number overlay badges.
 *
 * @property anchorPoint Where to position the badge relative to the element
 * @property offsetX Horizontal offset from anchor point (negative = inward)
 * @property offsetY Vertical offset from anchor point (negative = inward)
 * @property circleRadius Radius of circular badges in dp
 * @property strokeWidth Width of outline stroke for outlined shapes
 * @property hasNameColor Color for elements with voice-accessible names (ARGB)
 * @property noNameColor Color for elements without clear names (ARGB)
 * @property disabledColor Color for disabled elements (ARGB)
 * @property numberColor Color of the number text (ARGB)
 * @property numberSize Font size of numbers in sp
 * @property fontWeight Font weight (400=normal, 700=bold)
 * @property dropShadow Whether to show drop shadow behind badge
 * @property shadowRadius Shadow blur radius in dp
 * @property shadowColor Shadow color (ARGB)
 * @property shadowOffsetY Vertical offset for shadow in dp
 * @property badgeStyle Shape of the badge
 */
data class NumberOverlayStyle(
    val anchorPoint: AnchorPoint = AnchorPoint.TOP_RIGHT,
    val offsetX: Float = -4f,
    val offsetY: Float = -4f,
    val circleRadius: Float = 16f,
    val strokeWidth: Float = 2f,
    val hasNameColor: Long = 0xFF4CAF50,      // Material Green 500
    val noNameColor: Long = 0xFFFF9800,        // Material Orange 500
    val disabledColor: Long = 0xFF757575,      // Grey 600
    val numberColor: Long = 0xFFFFFFFF,        // White
    val numberSize: Float = 14f,
    val fontWeight: Int = 700,                  // Bold
    val dropShadow: Boolean = true,
    val shadowRadius: Float = 4f,
    val shadowColor: Long = 0x40000000,        // 25% black
    val shadowOffsetY: Float = 2f,              // Shadow vertical offset
    val badgeStyle: BadgeShape = BadgeShape.FILLED_CIRCLE
) {
    /**
     * Get badge size (diameter for circles).
     */
    val badgeSize: Float
        get() = circleRadius * 2

    companion object {
        /**
         * Create default style.
         */
        fun default(): NumberOverlayStyle = NumberOverlayStyle()
    }
}

/**
 * Predefined number overlay styles.
 */
object NumberOverlayStyles {

    /** Default Material Design style. */
    val DEFAULT = NumberOverlayStyle()

    /** Compact style for dense UIs. */
    val COMPACT = NumberOverlayStyle(
        circleRadius = 12f,
        numberSize = 10f,
        offsetX = -2f,
        offsetY = -2f
    )

    /** Large style for accessibility. */
    val LARGE = NumberOverlayStyle(
        circleRadius = 20f,
        numberSize = 18f,
        offsetX = -6f,
        offsetY = -6f
    )

    /** High contrast style for enhanced visibility. */
    val HIGH_CONTRAST = NumberOverlayStyle(
        circleRadius = 20f,
        strokeWidth = 3f,
        numberSize = 16f,
        hasNameColor = 0xFF1B5E20,    // Darker green for contrast
        noNameColor = 0xFFE65100,      // Darker orange for contrast
        disabledColor = 0xFF424242,
        numberColor = 0xFFFFFFFF
    )

    /** Large text style for accessibility. */
    val LARGE_TEXT = NumberOverlayStyle(
        circleRadius = 24f,
        numberSize = 20f
    )

    /** Minimal style with subtle semi-transparent appearance. */
    val MINIMAL = NumberOverlayStyle(
        circleRadius = 14f,
        numberSize = 12f,
        hasNameColor = 0x804CAF50,    // 50% transparent green
        noNameColor = 0x80FF9800,      // 50% transparent orange
        dropShadow = false
    )

    /** Colorful style with vibrant colors. */
    val COLORFUL = NumberOverlayStyle(
        hasNameColor = 0xFF2196F3,
        noNameColor = 0xFFE91E63,
        disabledColor = 0xFF9E9E9E
    )

    /** Outlined circle style. */
    val OUTLINED = NumberOverlayStyle(
        badgeStyle = BadgeShape.OUTLINED_CIRCLE,
        hasNameColor = 0xFF4CAF50,
        noNameColor = 0xFFFF9800,
        numberColor = 0xFF4CAF50,
        strokeWidth = 2.5f
    )

    /** Square badge style. */
    val SQUARE = NumberOverlayStyle(
        badgeStyle = BadgeShape.SQUARE,
        circleRadius = 14f
    )

    /** Rounded rectangle badge style. */
    val ROUNDED_RECT = NumberOverlayStyle(
        badgeStyle = BadgeShape.ROUNDED_RECT,
        circleRadius = 14f
    )

    /** Top-left anchor style. */
    val TOP_LEFT_ANCHOR = NumberOverlayStyle(
        anchorPoint = AnchorPoint.TOP_LEFT,
        offsetX = 4f,
        offsetY = -4f
    )

    /** Dark mode optimized style. */
    val DARK_MODE = NumberOverlayStyle(
        hasNameColor = 0xFF66BB6A,    // Lighter green for dark backgrounds
        noNameColor = 0xFFFFA726,      // Lighter orange for dark backgrounds
        numberColor = 0xFFE0E0E0,      // Off-white text
        shadowColor = 0x80000000       // 50% black shadow
    )

    /** Light mode optimized style. */
    val LIGHT_MODE = NumberOverlayStyle(
        hasNameColor = 0xFF43A047,    // Darker green for light backgrounds
        noNameColor = 0xFFFB8C00,      // Darker orange for light backgrounds
        numberColor = 0xFFFFFFFF       // White text
    )

    /** Colorblind-friendly style using blue and gold. */
    val COLORBLIND_FRIENDLY = NumberOverlayStyle(
        hasNameColor = 0xFF2196F3,    // Blue (instead of green)
        noNameColor = 0xFFFFC107,      // Gold (instead of orange)
        badgeStyle = BadgeShape.ROUNDED_RECT
    )

    /** Get all predefined styles. */
    fun all(): List<NumberOverlayStyle> = listOf(
        DEFAULT, HIGH_CONTRAST, LARGE_TEXT, MINIMAL, OUTLINED,
        SQUARE, ROUNDED_RECT, DARK_MODE, LIGHT_MODE, COLORBLIND_FRIENDLY
    )
}
