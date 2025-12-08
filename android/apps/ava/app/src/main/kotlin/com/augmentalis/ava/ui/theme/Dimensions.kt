// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/theme/Dimensions.kt
// created: 2025-11-04 14:10:00 -0800
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * AVA Theme Dimensions - Responsive Spacing and Sizing System
 *
 * Provides consistent spacing, sizing, and elevation values across the app.
 * Based on 8dp grid system for Material Design compliance.
 *
 * To adjust spacing/sizing for the entire app, modify values in this object.
 */
object AvaDimensions {

    // ==================== SPACING SCALE ====================

    /**
     * Spacing scale based on 8dp grid
     * Use these for padding, margin, and gaps
     */
    val SpaceNone = 0.dp
    val SpaceXxs = 2.dp        // Extra extra small
    val SpaceXs = 4.dp         // Extra small
    val SpaceSm = 8.dp         // Small
    val SpaceMd = 16.dp        // Medium (base unit)
    val SpaceLg = 24.dp        // Large
    val SpaceXl = 32.dp        // Extra large
    val SpaceXxl = 48.dp       // Extra extra large
    val Space3xl = 64.dp       // 3X large
    val Space4xl = 96.dp       // 4X large

    // ==================== COMPONENT SIZES ====================

    /**
     * Icon sizes
     */
    val IconXs = 12.dp
    val IconSm = 16.dp
    val IconMd = 24.dp
    val IconLg = 32.dp
    val IconXl = 48.dp
    val IconXxl = 64.dp

    /**
     * Button heights
     */
    val ButtonHeightSm = 32.dp
    val ButtonHeightMd = 48.dp
    val ButtonHeightLg = 56.dp

    /**
     * Button minimum widths
     */
    val ButtonMinWidthSm = 64.dp
    val ButtonMinWidthMd = 88.dp
    val ButtonMinWidthLg = 120.dp

    /**
     * App bar heights
     */
    val AppBarHeight = 56.dp
    val AppBarHeightLarge = 112.dp

    /**
     * Bottom navigation height
     */
    val BottomNavHeight = 56.dp

    /**
     * FAB sizes
     */
    val FabSizeSmall = 40.dp
    val FabSizeMedium = 56.dp
    val FabSizeLarge = 96.dp

    // ==================== CARD AND PANEL SIZES ====================

    /**
     * Card dimensions
     */
    val CardMinHeight = 72.dp
    val CardDefaultHeight = 120.dp
    val CardLargeHeight = 200.dp

    /**
     * Panel widths (for side panels, bottom sheets)
     */
    val PanelWidthSm = 280.dp
    val PanelWidthMd = 360.dp
    val PanelWidthLg = 480.dp

    /**
     * Dialog dimensions
     */
    val DialogMinWidth = 280.dp
    val DialogMaxWidth = 560.dp
    val DialogMinHeight = 120.dp

    // ==================== BORDERS AND STROKES ====================

    /**
     * Border widths
     */
    val BorderNone = 0.dp
    val BorderThin = 0.5.dp
    val BorderDefault = 1.dp
    val BorderThick = 2.dp
    val BorderHeavy = 4.dp

    /**
     * Divider thickness
     */
    val DividerThickness = 1.dp

    // ==================== CORNER RADIUS ====================

    /**
     * Corner radius for various components
     * Glassmorphic design typically uses larger corner radii
     */
    val CornerNone = 0.dp
    val CornerXs = 4.dp
    val CornerSm = 8.dp
    val CornerMd = 12.dp
    val CornerLg = 16.dp
    val CornerXl = 20.dp
    val CornerXxl = 24.dp
    val CornerRound = 999.dp   // For fully rounded elements

    // ==================== ELEVATION ====================

    /**
     * Elevation levels (shadow depth)
     */
    val ElevationNone = 0.dp
    val ElevationXs = 1.dp
    val ElevationSm = 2.dp
    val ElevationMd = 4.dp
    val ElevationLg = 8.dp
    val ElevationXl = 12.dp
    val ElevationXxl = 16.dp

    // ==================== RESPONSIVE BREAKPOINTS ====================

    /**
     * Screen width breakpoints for responsive design
     * Based on Material Design guidelines
     */
    val CompactMaxWidth = 600.dp      // Phones in portrait
    val MediumMinWidth = 600.dp       // Tablets, phones in landscape
    val MediumMaxWidth = 840.dp
    val ExpandedMinWidth = 840.dp     // Large tablets, desktops

    // ==================== LAYOUT CONSTRAINTS ====================

    /**
     * Maximum content widths for readability
     */
    val ContentMaxWidthCompact = 600.dp
    val ContentMaxWidthMedium = 840.dp
    val ContentMaxWidthExpanded = 1200.dp

    /**
     * Minimum touch target size (accessibility)
     */
    val MinTouchTarget = 48.dp

    /**
     * Safe area padding (for edge-to-edge displays)
     */
    val SafeAreaPaddingHorizontal = 16.dp
    val SafeAreaPaddingVertical = 8.dp

    // ==================== CHAT/MESSAGE SPECIFIC ====================

    /**
     * Chat bubble dimensions
     */
    val ChatBubbleMaxWidth = 280.dp
    val ChatBubbleMinWidth = 80.dp
    val ChatBubbleCornerRadius = CornerLg

    /**
     * Avatar sizes
     */
    val AvatarSizeXs = 24.dp
    val AvatarSizeSm = 32.dp
    val AvatarSizeMd = 40.dp
    val AvatarSizeLg = 56.dp
    val AvatarSizeXl = 72.dp

    // ==================== ANIMATION DURATIONS ====================

    /**
     * Standard animation durations (in milliseconds)
     * Note: These are Int, not Dp, but grouped here for theming
     */
    object Animation {
        const val DurationFast = 150        // Quick transitions
        const val DurationNormal = 300      // Standard animations
        const val DurationSlow = 500        // Emphasized animations
        const val DurationVerySlow = 1000   // Long transitions
    }

    // ==================== GLASS BLUR VALUES ====================

    /**
     * Blur radius for glassmorphic effect
     * Note: These are Float values for blur filters
     */
    object Blur {
        const val BlurNone = 0f
        const val BlurLight = 8f
        const val BlurMedium = 16f
        const val BlurHeavy = 24f
        const val BlurIntense = 32f
    }
}
