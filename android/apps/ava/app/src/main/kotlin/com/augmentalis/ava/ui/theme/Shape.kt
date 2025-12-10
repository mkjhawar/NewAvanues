// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/theme/Shape.kt
// created: 2025-11-04 14:10:00 -0800
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape

/**
 * AVA Theme Shapes - Glassmorphic Shape System
 *
 * Defines corner radii and shapes for all UI components.
 * Glassmorphic design typically uses larger, softer corner radii.
 *
 * To adjust component shapes across the app, modify values here.
 */
object AvaShapes {

    // ==================== BASIC SHAPES ====================

    /**
     * No rounding - sharp corners
     */
    val None = RoundedCornerShape(AvaDimensions.CornerNone)

    /**
     * Extra small rounding - 4dp
     * Use for: Small chips, tiny badges
     */
    val ExtraSmall = RoundedCornerShape(AvaDimensions.CornerXs)

    /**
     * Small rounding - 8dp
     * Use for: Small buttons, input fields
     */
    val Small = RoundedCornerShape(AvaDimensions.CornerSm)

    /**
     * Medium rounding - 12dp
     * Use for: Cards, standard buttons, panels
     */
    val Medium = RoundedCornerShape(AvaDimensions.CornerMd)

    /**
     * Large rounding - 16dp
     * Use for: Large cards, bottom sheets, dialogs
     */
    val Large = RoundedCornerShape(AvaDimensions.CornerLg)

    /**
     * Extra large rounding - 20dp
     * Use for: Hero cards, featured content
     */
    val ExtraLarge = RoundedCornerShape(AvaDimensions.CornerXl)

    /**
     * Extra extra large rounding - 24dp
     * Use for: Modal screens, full-screen overlays
     */
    val ExtraExtraLarge = RoundedCornerShape(AvaDimensions.CornerXxl)

    /**
     * Fully rounded - pill shape
     * Use for: Chips, tags, FABs, fully rounded buttons
     */
    val Round = RoundedCornerShape(AvaDimensions.CornerRound)

    // ==================== ASYMMETRIC SHAPES ====================

    /**
     * Rounded top corners only
     * Use for: Bottom sheets, pull-up panels
     */
    val TopRounded = RoundedCornerShape(
        topStart = AvaDimensions.CornerLg,
        topEnd = AvaDimensions.CornerLg,
        bottomStart = AvaDimensions.CornerNone,
        bottomEnd = AvaDimensions.CornerNone
    )

    /**
     * Rounded bottom corners only
     * Use for: Top panels, dropdown menus
     */
    val BottomRounded = RoundedCornerShape(
        topStart = AvaDimensions.CornerNone,
        topEnd = AvaDimensions.CornerNone,
        bottomStart = AvaDimensions.CornerLg,
        bottomEnd = AvaDimensions.CornerLg
    )

    /**
     * Rounded left corners only
     * Use for: Right-side panels, drawers
     */
    val LeftRounded = RoundedCornerShape(
        topStart = AvaDimensions.CornerLg,
        topEnd = AvaDimensions.CornerNone,
        bottomStart = AvaDimensions.CornerLg,
        bottomEnd = AvaDimensions.CornerNone
    )

    /**
     * Rounded right corners only
     * Use for: Left-side panels, navigation drawers
     */
    val RightRounded = RoundedCornerShape(
        topStart = AvaDimensions.CornerNone,
        topEnd = AvaDimensions.CornerLg,
        bottomStart = AvaDimensions.CornerNone,
        bottomEnd = AvaDimensions.CornerLg
    )

    // ==================== CHAT BUBBLE SHAPES ====================

    /**
     * Chat bubble shapes with different corner treatments
     */
    object ChatBubble {
        /**
         * User message bubble (right-aligned)
         * Rounded on left and top-right, sharp on bottom-right
         */
        val User = RoundedCornerShape(
            topStart = AvaDimensions.CornerLg,
            topEnd = AvaDimensions.CornerLg,
            bottomStart = AvaDimensions.CornerLg,
            bottomEnd = AvaDimensions.CornerXs
        )

        /**
         * Assistant message bubble (left-aligned)
         * Rounded on right and top-left, sharp on bottom-left
         */
        val Assistant = RoundedCornerShape(
            topStart = AvaDimensions.CornerLg,
            topEnd = AvaDimensions.CornerLg,
            bottomStart = AvaDimensions.CornerXs,
            bottomEnd = AvaDimensions.CornerLg
        )
    }

    // ==================== GLASS PANEL SHAPES ====================

    /**
     * Glass panel shapes with extra large corners for glassmorphic effect
     */
    object GlassPanel {
        val Small = RoundedCornerShape(AvaDimensions.CornerMd)
        val Medium = RoundedCornerShape(AvaDimensions.CornerLg)
        val Large = RoundedCornerShape(AvaDimensions.CornerXl)
        val ExtraLarge = RoundedCornerShape(AvaDimensions.CornerXxl)
    }

    // ==================== MATERIAL 3 SHAPES ====================

    /**
     * Material 3 Shapes definition
     * Used by MaterialTheme for consistent component shaping
     */
    val Material3 = Shapes(
        extraSmall = ExtraSmall,
        small = Small,
        medium = Medium,
        large = Large,
        extraLarge = ExtraLarge
    )
}

/**
 * Extension functions for common shape combinations
 */

/**
 * Create a shape with custom corner radii for each corner
 */
fun customCornerShape(
    topStart: androidx.compose.ui.unit.Dp = AvaDimensions.CornerNone,
    topEnd: androidx.compose.ui.unit.Dp = AvaDimensions.CornerNone,
    bottomStart: androidx.compose.ui.unit.Dp = AvaDimensions.CornerNone,
    bottomEnd: androidx.compose.ui.unit.Dp = AvaDimensions.CornerNone
): Shape = RoundedCornerShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomStart = bottomStart,
    bottomEnd = bottomEnd
)
