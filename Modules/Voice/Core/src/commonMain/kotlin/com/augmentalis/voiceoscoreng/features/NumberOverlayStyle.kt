/**
 * NumberOverlayStyle.kt - Configuration for number overlay badges
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * Ported from VoiceOS VoiceOSCore module to KMP-compatible implementation.
 * Original: com.augmentalis.voiceoscore.accessibility.overlays.NumberOverlayStyle
 */
package com.augmentalis.voiceoscoreng.features

/**
 * Badge shape for number overlay.
 *
 * Determines the visual shape of the numbered badge displayed over UI elements.
 */
enum class BadgeShape {
    /** Filled solid circle (default) */
    FILLED_CIRCLE,

    /** Circle with stroke outline only */
    OUTLINED_CIRCLE,

    /** Square badge */
    SQUARE,

    /** Rectangle with rounded corners */
    ROUNDED_RECT
}

/**
 * Accessibility shape for colorblind-friendly badge differentiation.
 *
 * Provides dual encoding (color + shape) for users with color vision deficiency (CVD).
 * When shape accessibility is enabled, badge shapes vary based on element state,
 * allowing users to distinguish item states without relying solely on color.
 *
 * ## Accessibility Rationale
 *
 * Approximately 8% of males and 0.5% of females have some form of color vision
 * deficiency. The most common forms (deuteranopia and protanopia) make it difficult
 * to distinguish between green and orange/red colors used in the standard badge
 * color scheme.
 *
 * By using distinct shapes for different states, users with CVD can:
 * - Identify enabled items with names (CIRCLE) vs without names (SQUARE)
 * - Recognize disabled items (DIAMOND) without relying on the grey color
 *
 * ## Shape Meanings
 *
 * | Shape   | State                          | Color (Default) |
 * |---------|--------------------------------|-----------------|
 * | CIRCLE  | Enabled + has name             | Green           |
 * | SQUARE  | Enabled + no name              | Orange          |
 * | DIAMOND | Disabled (regardless of name)  | Grey            |
 *
 * ## Usage
 *
 * Enable shape accessibility via [NumberOverlayStyle.useShapeAccessibility]:
 * ```kotlin
 * val style = NumberOverlayStyle(useShapeAccessibility = true)
 * ```
 *
 * Or use the predefined [NumberOverlayStyles.SHAPE_ACCESSIBLE] style.
 *
 * @see NumberOverlayStyle.useShapeAccessibility
 * @see NumberOverlayRenderer.getAccessibilityShape
 */
enum class AccessibilityShape {
    /**
     * Circle shape for enabled items with a meaningful name.
     *
     * Indicates the element can be selected by voice using its name.
     */
    CIRCLE,

    /**
     * Square shape for enabled items without a meaningful name.
     *
     * Indicates the element can be selected by number only.
     */
    SQUARE,

    /**
     * Diamond (rotated square) shape for disabled items.
     *
     * Indicates the element cannot currently be selected.
     */
    DIAMOND
}

/**
 * Anchor point for badge positioning relative to element.
 *
 * Determines which corner of the UI element the badge is anchored to.
 */
enum class AnchorPoint {
    /** Top-left corner of the element */
    TOP_LEFT,

    /** Top-right corner of the element (default) */
    TOP_RIGHT,

    /** Bottom-left corner of the element */
    BOTTOM_LEFT,

    /** Bottom-right corner of the element */
    BOTTOM_RIGHT
}

/**
 * Style configuration for number overlay badges.
 *
 * Provides Material 3 design system colors and positioning for circular badges
 * that display element numbers for voice control navigation.
 *
 * @property anchorPoint Position anchor relative to element bounds
 * @property offsetX Horizontal offset from anchor point (pixels)
 * @property offsetY Vertical offset from anchor point (pixels)
 * @property circleRadius Radius of the badge circle (dp)
 * @property strokeWidth Width of the stroke for outlined styles (dp)
 * @property hasNameColor Color for elements with voice command names (0xAARRGGBB)
 * @property noNameColor Color for elements without voice command names (0xAARRGGBB)
 * @property disabledColor Color for disabled elements (0xAARRGGBB)
 * @property numberColor Color of the number text (0xAARRGGBB)
 * @property numberSize Size of the number text (sp)
 * @property fontWeight Font weight for the number (100-900)
 * @property dropShadow Whether to render drop shadow
 * @property shadowRadius Blur radius of the shadow (dp)
 * @property shadowColor Color of the shadow (0xAARRGGBB)
 * @property shadowOffsetY Vertical offset of shadow (dp)
 * @property badgeStyle Shape variant for the badge
 * @property useShapeAccessibility When true, badge shapes vary based on element state
 *     to provide dual encoding (color + shape) for users with color vision deficiency.
 *     See [AccessibilityShape] for shape meanings.
 */
data class NumberOverlayStyle(
    // Position configuration
    val anchorPoint: AnchorPoint = AnchorPoint.TOP_RIGHT,
    val offsetX: Float = -4f,
    val offsetY: Float = -4f,

    // Circle dimensions
    val circleRadius: Float = 16f,
    val strokeWidth: Float = 2f,

    // Material 3 colors (state-based) - stored as Long in 0xAARRGGBB format
    val hasNameColor: Long = 0xFF4CAF50,      // Material Green 500 - has command name
    val noNameColor: Long = 0xFFFF9800,       // Material Orange 500 - no command name
    val disabledColor: Long = 0xFF757575,     // Grey 600 - disabled element

    // Number text styling
    val numberColor: Long = 0xFFFFFFFF,       // White
    val numberSize: Float = 14f,              // sp
    val fontWeight: Int = 700,                // Bold (CSS font-weight scale)

    // Visual effects
    val dropShadow: Boolean = true,
    val shadowRadius: Float = 4f,
    val shadowColor: Long = 0x40000000,       // 25% black
    val shadowOffsetY: Float = 2f,

    // Badge style variants
    val badgeStyle: BadgeShape = BadgeShape.FILLED_CIRCLE,

    // Accessibility options
    /**
     * Enable shape-based accessibility differentiation for colorblind users.
     *
     * When enabled, badge shapes vary based on element state:
     * - CIRCLE: enabled items with names
     * - SQUARE: enabled items without names
     * - DIAMOND: disabled items
     *
     * This provides dual encoding (color + shape) so users with color vision
     * deficiency can distinguish element states without relying on color alone.
     *
     * @see AccessibilityShape
     * @see NumberOverlayRenderer.getAccessibilityShape
     */
    val useShapeAccessibility: Boolean = false
)

/**
 * Predefined style configurations for number overlays.
 *
 * Provides ready-to-use style variants optimized for different use cases
 * including accessibility, visual preferences, and platform conventions.
 */
object NumberOverlayStyles {

    /**
     * Default Material 3 style with green/orange/grey colors.
     *
     * Standard appearance suitable for most use cases.
     */
    val DEFAULT = NumberOverlayStyle()

    /**
     * High contrast style for accessibility.
     *
     * Features larger size, thicker borders, and darker colors
     * for improved visibility.
     */
    val HIGH_CONTRAST = NumberOverlayStyle(
        circleRadius = 20f,
        strokeWidth = 3f,
        hasNameColor = 0xFF1B5E20,  // Darker green (Green 900)
        noNameColor = 0xFFE65100,   // Darker orange (Orange 900)
        disabledColor = 0xFF424242, // Darker grey
        numberSize = 16f,
        shadowRadius = 6f
    )

    /**
     * Large text mode for visual impairment.
     *
     * Increased dimensions for better readability.
     */
    val LARGE_TEXT = NumberOverlayStyle(
        numberSize = 20f,
        circleRadius = 24f,
        strokeWidth = 3f,
        offsetX = -6f,
        offsetY = -6f
    )

    /**
     * Minimal style with subtle colors.
     *
     * Semi-transparent colors for less visual intrusion.
     */
    val MINIMAL = NumberOverlayStyle(
        hasNameColor = 0x804CAF50,    // 50% alpha green
        noNameColor = 0x80FF9800,     // 50% alpha orange
        disabledColor = 0x80757575,   // 50% alpha grey
        dropShadow = false,
        strokeWidth = 1f
    )

    /**
     * Outlined style (hollow circles).
     *
     * Uses stroke outline instead of filled background.
     */
    val OUTLINED = NumberOverlayStyle(
        badgeStyle = BadgeShape.OUTLINED_CIRCLE,
        hasNameColor = 0xFF4CAF50,
        noNameColor = 0xFFFF9800,
        numberColor = 0xFF4CAF50,  // Number matches outline
        strokeWidth = 3f,
        dropShadow = false
    )

    /**
     * Square badges.
     *
     * Alternative shape for different visual style.
     */
    val SQUARE = NumberOverlayStyle(
        badgeStyle = BadgeShape.SQUARE,
        circleRadius = 14f  // Used as half-width for square
    )

    /**
     * Rounded rectangle badges.
     *
     * Pill-shaped badges for wider number display.
     */
    val ROUNDED_RECT = NumberOverlayStyle(
        badgeStyle = BadgeShape.ROUNDED_RECT,
        circleRadius = 16f
    )

    /**
     * Dark mode optimized style.
     *
     * Lighter colors that work better on dark backgrounds.
     */
    val DARK_MODE = NumberOverlayStyle(
        hasNameColor = 0xFF66BB6A,    // Slightly lighter green for dark backgrounds
        noNameColor = 0xFFFFA726,     // Slightly lighter orange
        disabledColor = 0xFF757575,   // Mid grey
        numberColor = 0xFFE0E0E0,     // Off-white for better contrast
        shadowColor = 0x80000000      // 50% black shadow
    )

    /**
     * Light mode optimized style.
     *
     * Darker colors that work better on light backgrounds.
     */
    val LIGHT_MODE = NumberOverlayStyle(
        hasNameColor = 0xFF43A047,    // Slightly darker green
        noNameColor = 0xFFFB8C00,     // Slightly darker orange
        disabledColor = 0xFF9E9E9E,
        numberColor = 0xFFFFFFFF,     // White
        shadowColor = 0x40000000      // 25% black shadow
    )

    /**
     * Colorblind-friendly style (using shapes instead of just colors).
     *
     * Uses blue and gold colors which are distinguishable by most
     * people with color vision deficiencies, combined with different
     * shapes for additional differentiation.
     */
    val COLORBLIND_FRIENDLY = NumberOverlayStyle(
        hasNameColor = 0xFF2196F3,    // Blue (instead of green)
        noNameColor = 0xFFFFC107,     // Gold/amber (instead of orange)
        disabledColor = 0xFF999999,   // Grey
        badgeStyle = BadgeShape.ROUNDED_RECT,
        strokeWidth = 3f
    )

    /**
     * Shape-based accessibility style for colorblind users.
     *
     * Enables dual encoding (color + shape) where badge shapes vary based on
     * element state, allowing users with color vision deficiency to distinguish
     * item states without relying solely on color:
     *
     * | State                    | Shape   | Color        |
     * |--------------------------|---------|--------------|
     * | Enabled + has name       | CIRCLE  | Blue         |
     * | Enabled + no name        | SQUARE  | Gold/Amber   |
     * | Disabled                 | DIAMOND | Grey         |
     *
     * This style combines:
     * - [useShapeAccessibility] = true for shape-based differentiation
     * - Colorblind-friendly blue/gold color palette
     * - Larger stroke width for better visibility
     *
     * @see AccessibilityShape
     * @see NumberOverlayRenderer.getAccessibilityShape
     */
    val SHAPE_ACCESSIBLE = NumberOverlayStyle(
        hasNameColor = 0xFF2196F3,    // Blue (CVD-friendly)
        noNameColor = 0xFFFFC107,     // Gold/amber (CVD-friendly)
        disabledColor = 0xFF999999,   // Grey
        strokeWidth = 3f,
        useShapeAccessibility = true
    )
}
