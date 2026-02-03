package com.augmentalis.avaelements.renderer.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.common.color.UniversalColor
import com.augmentalis.avaelements.common.color.ColorUtils
import com.augmentalis.avaelements.common.spacing.EdgeInsets
import com.augmentalis.avaelements.common.spacing.CornerRadius
import com.augmentalis.avaelements.common.spacing.Border
import com.augmentalis.avaelements.common.spacing.Shadow
import com.augmentalis.avaelements.common.spacing.Size
import com.augmentalis.avaelements.common.alignment.AlignmentConverter
import com.augmentalis.avaelements.common.alignment.LayoutDirection as SharedLayoutDirection
import androidx.compose.ui.unit.LayoutDirection as ComposeLayoutDirection
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment

/**
 * Android Compose Bridge for Shared Utilities
 *
 * This file provides Compose-specific extensions and conversions for the shared
 * utilities from `com.augmentalis.avaelements.common.*`.
 *
 * Purpose:
 * - Convert shared types to Compose types
 * - Provide convenient extension functions for Android renderer
 * - Eliminate duplicate conversion code across mapper files
 *
 * Shared Utilities Used:
 * - AlignmentConverter - RTL-aware alignment conversion
 * - ColorUtils - Color manipulation (lighten, darken, mix, contrast)
 * - EdgeInsets, CornerRadius, Border, Shadow - Spacing/sizing utilities
 * - PropertyExtractor - Type-safe property extraction (used directly, no bridge needed)
 *
 * @since 3.1.0
 */

// ════════════════════════════════════════════════════════════════════════════
// Color Conversion
// ════════════════════════════════════════════════════════════════════════════

/**
 * Convert UniversalColor to Jetpack Compose Color
 */
fun UniversalColor.toCompose(): ComposeColor {
    return ComposeColor(
        red = this.red,
        green = this.green,
        blue = this.blue,
        alpha = this.alpha
    )
}

/**
 * Convert Compose Color to UniversalColor
 */
fun ComposeColor.toUniversal(): UniversalColor {
    return UniversalColor(
        alpha = this.alpha,
        red = this.red,
        green = this.green,
        blue = this.blue
    )
}

/**
 * Convert ARGB Int to Compose Color
 */
fun Int.toComposeColor(): ComposeColor {
    return UniversalColor.fromArgb(this).toCompose()
}

// ════════════════════════════════════════════════════════════════════════════
// Color Manipulation (Compose Extensions)
// ════════════════════════════════════════════════════════════════════════════

/**
 * Lighten a Compose Color by a factor (0.0 - 1.0)
 * Uses shared ColorUtils for consistent behavior across platforms
 *
 * Example: Color.Blue.lighten(0.3f) // 30% lighter blue
 */
fun ComposeColor.lighten(factor: Float): ComposeColor {
    return ColorUtils.lighten(this.toUniversal(), factor).toCompose()
}

/**
 * Darken a Compose Color by a factor (0.0 - 1.0)
 * Uses shared ColorUtils for consistent behavior across platforms
 *
 * Example: Color.Red.darken(0.2f) // 20% darker red
 */
fun ComposeColor.darken(factor: Float): ComposeColor {
    return ColorUtils.darken(this.toUniversal(), factor).toCompose()
}

/**
 * Adjust saturation by a factor (-1.0 to 1.0)
 * Negative values desaturate, positive values saturate.
 *
 * Example: Color.Green.saturate(0.5f) // More saturated green
 */
fun ComposeColor.saturate(factor: Float): ComposeColor {
    return ColorUtils.saturate(this.toUniversal(), factor).toCompose()
}

/**
 * Mix two Compose Colors together
 * @param other The color to mix with
 * @param ratio 0.0 = 100% this color, 1.0 = 100% other color
 *
 * Example: Color.Red.mix(Color.Blue, 0.5f) // Purple (50/50 mix)
 */
fun ComposeColor.mix(other: ComposeColor, ratio: Float): ComposeColor {
    return ColorUtils.mix(this.toUniversal(), other.toUniversal(), ratio).toCompose()
}

/**
 * Get contrasting foreground color (black or white) based on luminance
 * Uses WCAG luminance calculation
 *
 * Example: backgroundColor.contrastingForeground() // Black or white for best contrast
 */
fun ComposeColor.contrastingForeground(): ComposeColor {
    return ColorUtils.contrastingForeground(this.toUniversal()).toCompose()
}

/**
 * Calculate contrast ratio between two colors (WCAG)
 * Returns ratio from 1:1 to 21:1
 */
fun ComposeColor.contrastRatio(other: ComposeColor): Float {
    return ColorUtils.contrastRatio(this.toUniversal(), other.toUniversal())
}

/**
 * Check if contrast meets WCAG AA standard (4.5:1 for normal text)
 */
fun ComposeColor.meetsWcagAA(background: ComposeColor): Boolean {
    return ColorUtils.meetsWcagAA(this.toUniversal(), background.toUniversal())
}

/**
 * Invert a color
 */
fun ComposeColor.invert(): ComposeColor {
    return ColorUtils.invert(this.toUniversal()).toCompose()
}

/**
 * Convert to grayscale
 */
fun ComposeColor.grayscale(): ComposeColor {
    return ColorUtils.grayscale(this.toUniversal()).toCompose()
}

/**
 * Shift hue by degrees (0-360)
 */
fun ComposeColor.shiftHue(degrees: Float): ComposeColor {
    return ColorUtils.shiftHue(this.toUniversal(), degrees).toCompose()
}

/**
 * Get complementary color (180° hue shift)
 */
fun ComposeColor.complementary(): ComposeColor {
    return ColorUtils.complementary(this.toUniversal()).toCompose()
}

// ════════════════════════════════════════════════════════════════════════════
// Spacing Conversion
// ════════════════════════════════════════════════════════════════════════════

/**
 * Convert EdgeInsets to Compose PaddingValues
 * Automatically handles RTL layout direction
 */
fun EdgeInsets.toPaddingValues(): PaddingValues {
    return PaddingValues(
        start = this.start.dp,
        top = this.top.dp,
        end = this.end.dp,
        bottom = this.bottom.dp
    )
}

/**
 * Convert CornerRadius to Compose RoundedCornerShape
 */
fun CornerRadius.toRoundedCornerShape(): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = this.topStart.dp,
        topEnd = this.topEnd.dp,
        bottomStart = this.bottomStart.dp,
        bottomEnd = this.bottomEnd.dp
    )
}

/**
 * Convert Size to Compose Dp
 */
fun Size.toDp(): androidx.compose.ui.unit.Dp {
    return this.width.dp
}

// ════════════════════════════════════════════════════════════════════════════
// Layout Direction Conversion
// ════════════════════════════════════════════════════════════════════════════

/**
 * Convert Compose LayoutDirection to shared LayoutDirection
 */
fun ComposeLayoutDirection.toShared(): SharedLayoutDirection {
    return when (this) {
        ComposeLayoutDirection.Ltr -> SharedLayoutDirection.Ltr
        ComposeLayoutDirection.Rtl -> SharedLayoutDirection.Rtl
    }
}

/**
 * Convert shared LayoutDirection to Compose LayoutDirection
 */
fun SharedLayoutDirection.toCompose(): ComposeLayoutDirection {
    return when (this) {
        SharedLayoutDirection.Ltr -> ComposeLayoutDirection.Ltr
        SharedLayoutDirection.Rtl -> ComposeLayoutDirection.Rtl
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Flutter-Parity to Shared Alignment Type Conversion
// ════════════════════════════════════════════════════════════════════════════

/**
 * Convert Flutter-parity WrapAlignment to shared WrapAlignment
 */
fun com.augmentalis.avaelements.flutter.layout.WrapAlignment.toShared(): com.augmentalis.avaelements.common.alignment.WrapAlignment {
    return when (this) {
        com.augmentalis.avaelements.flutter.layout.WrapAlignment.Start -> com.augmentalis.avaelements.common.alignment.WrapAlignment.Start
        com.augmentalis.avaelements.flutter.layout.WrapAlignment.End -> com.augmentalis.avaelements.common.alignment.WrapAlignment.End
        com.augmentalis.avaelements.flutter.layout.WrapAlignment.Center -> com.augmentalis.avaelements.common.alignment.WrapAlignment.Center
        com.augmentalis.avaelements.flutter.layout.WrapAlignment.SpaceBetween -> com.augmentalis.avaelements.common.alignment.WrapAlignment.SpaceBetween
        com.augmentalis.avaelements.flutter.layout.WrapAlignment.SpaceAround -> com.augmentalis.avaelements.common.alignment.WrapAlignment.SpaceAround
        com.augmentalis.avaelements.flutter.layout.WrapAlignment.SpaceEvenly -> com.augmentalis.avaelements.common.alignment.WrapAlignment.SpaceEvenly
    }
}

/**
 * Convert Flutter-parity MainAxisAlignment to shared MainAxisAlignment
 */
fun com.augmentalis.avaelements.flutter.layout.MainAxisAlignment.toShared(): com.augmentalis.avaelements.common.alignment.MainAxisAlignment {
    return when (this) {
        com.augmentalis.avaelements.flutter.layout.MainAxisAlignment.Start -> com.augmentalis.avaelements.common.alignment.MainAxisAlignment.Start
        com.augmentalis.avaelements.flutter.layout.MainAxisAlignment.End -> com.augmentalis.avaelements.common.alignment.MainAxisAlignment.End
        com.augmentalis.avaelements.flutter.layout.MainAxisAlignment.Center -> com.augmentalis.avaelements.common.alignment.MainAxisAlignment.Center
        com.augmentalis.avaelements.flutter.layout.MainAxisAlignment.SpaceBetween -> com.augmentalis.avaelements.common.alignment.MainAxisAlignment.SpaceBetween
        com.augmentalis.avaelements.flutter.layout.MainAxisAlignment.SpaceAround -> com.augmentalis.avaelements.common.alignment.MainAxisAlignment.SpaceAround
        com.augmentalis.avaelements.flutter.layout.MainAxisAlignment.SpaceEvenly -> com.augmentalis.avaelements.common.alignment.MainAxisAlignment.SpaceEvenly
    }
}

/**
 * Convert Flutter-parity CrossAxisAlignment to shared CrossAxisAlignment
 */
fun com.augmentalis.avaelements.flutter.layout.CrossAxisAlignment.toShared(): com.augmentalis.avaelements.common.alignment.CrossAxisAlignment {
    return when (this) {
        com.augmentalis.avaelements.flutter.layout.CrossAxisAlignment.Start -> com.augmentalis.avaelements.common.alignment.CrossAxisAlignment.Start
        com.augmentalis.avaelements.flutter.layout.CrossAxisAlignment.End -> com.augmentalis.avaelements.common.alignment.CrossAxisAlignment.End
        com.augmentalis.avaelements.flutter.layout.CrossAxisAlignment.Center -> com.augmentalis.avaelements.common.alignment.CrossAxisAlignment.Center
        com.augmentalis.avaelements.flutter.layout.CrossAxisAlignment.Stretch -> com.augmentalis.avaelements.common.alignment.CrossAxisAlignment.Stretch
        com.augmentalis.avaelements.flutter.layout.CrossAxisAlignment.Baseline -> com.augmentalis.avaelements.common.alignment.CrossAxisAlignment.Baseline
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Alignment Conversion (Using Shared AlignmentConverter)
// ════════════════════════════════════════════════════════════════════════════

/**
 * Convert shared HorizontalArrangement to Compose Arrangement.Horizontal
 */
fun com.augmentalis.avaelements.common.alignment.HorizontalArrangement.toCompose(): Arrangement.Horizontal {
    return when (this) {
        is com.augmentalis.avaelements.common.alignment.HorizontalArrangement.Start -> Arrangement.Start
        is com.augmentalis.avaelements.common.alignment.HorizontalArrangement.End -> Arrangement.End
        is com.augmentalis.avaelements.common.alignment.HorizontalArrangement.Center -> Arrangement.Center
        is com.augmentalis.avaelements.common.alignment.HorizontalArrangement.SpaceBetween -> Arrangement.SpaceBetween
        is com.augmentalis.avaelements.common.alignment.HorizontalArrangement.SpaceAround -> Arrangement.SpaceAround
        is com.augmentalis.avaelements.common.alignment.HorizontalArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
        is com.augmentalis.avaelements.common.alignment.HorizontalArrangement.SpacedBy -> Arrangement.spacedBy(this.spacing.dp)
    }
}

/**
 * Convert shared VerticalArrangement to Compose Arrangement.Vertical
 */
fun com.augmentalis.avaelements.common.alignment.VerticalArrangement.toCompose(): Arrangement.Vertical {
    return when (this) {
        is com.augmentalis.avaelements.common.alignment.VerticalArrangement.Top -> Arrangement.Top
        is com.augmentalis.avaelements.common.alignment.VerticalArrangement.Bottom -> Arrangement.Bottom
        is com.augmentalis.avaelements.common.alignment.VerticalArrangement.Center -> Arrangement.Center
        is com.augmentalis.avaelements.common.alignment.VerticalArrangement.SpaceBetween -> Arrangement.SpaceBetween
        is com.augmentalis.avaelements.common.alignment.VerticalArrangement.SpaceAround -> Arrangement.SpaceAround
        is com.augmentalis.avaelements.common.alignment.VerticalArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
        is com.augmentalis.avaelements.common.alignment.VerticalArrangement.SpacedBy -> Arrangement.spacedBy(this.spacing.dp)
    }
}

/**
 * Convert shared HorizontalAlignment to Compose Alignment.Horizontal
 */
fun com.augmentalis.avaelements.common.alignment.HorizontalAlignment.toCompose(): Alignment.Horizontal {
    return when (this) {
        com.augmentalis.avaelements.common.alignment.HorizontalAlignment.Start -> Alignment.Start
        com.augmentalis.avaelements.common.alignment.HorizontalAlignment.End -> Alignment.End
        com.augmentalis.avaelements.common.alignment.HorizontalAlignment.Center -> Alignment.CenterHorizontally
    }
}

/**
 * Convert shared VerticalAlignment to Compose Alignment.Vertical
 */
fun com.augmentalis.avaelements.common.alignment.VerticalAlignment.toCompose(): Alignment.Vertical {
    return when (this) {
        com.augmentalis.avaelements.common.alignment.VerticalAlignment.Top -> Alignment.Top
        com.augmentalis.avaelements.common.alignment.VerticalAlignment.Bottom -> Alignment.Bottom
        com.augmentalis.avaelements.common.alignment.VerticalAlignment.Center -> Alignment.CenterVertically
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Convenience Functions
// ════════════════════════════════════════════════════════════════════════════

/**
 * Create UniversalColor from hex string and convert to Compose Color
 * Example: "#FF5733".toComposeColor()
 */
fun String.toComposeColor(): ComposeColor {
    return UniversalColor.fromHex(this).toCompose()
}

/**
 * Create EdgeInsets from all-same value
 * Example: 16f.toEdgeInsets() // 16dp padding on all sides
 */
fun Float.toEdgeInsets(): EdgeInsets {
    return EdgeInsets.all(this)
}

/**
 * Create CornerRadius from all-same value
 * Example: 8f.toCornerRadius() // 8dp radius on all corners
 */
fun Float.toCornerRadius(): CornerRadius {
    return CornerRadius.all(this)
}

// ════════════════════════════════════════════════════════════════════════════
// Flutter-Parity Alignment Direct to Compose Conversion (with RTL support)
// ════════════════════════════════════════════════════════════════════════════

/**
 * Convert Flutter WrapAlignment directly to Compose Horizontal Arrangement with RTL support
 * Uses shared AlignmentConverter for consistent cross-platform behavior
 */
fun com.augmentalis.avaelements.flutter.layout.WrapAlignment.toHorizontalArrangement(
    layoutDirection: ComposeLayoutDirection
): Arrangement.Horizontal {
    return AlignmentConverter.wrapToHorizontal(
        this.toShared(),
        layoutDirection.toShared()
    ).toCompose()
}

/**
 * Convert Flutter WrapAlignment directly to Compose Vertical Arrangement
 * Uses shared AlignmentConverter for consistent cross-platform behavior
 */
fun com.augmentalis.avaelements.flutter.layout.WrapAlignment.toVerticalArrangement(): Arrangement.Vertical {
    return AlignmentConverter.wrapToVertical(this.toShared()).toCompose()
}

/**
 * Convert Flutter MainAxisAlignment directly to Compose Horizontal Arrangement with RTL support
 * Uses shared AlignmentConverter for consistent cross-platform behavior
 */
fun com.augmentalis.avaelements.flutter.layout.MainAxisAlignment.toHorizontalArrangement(
    layoutDirection: ComposeLayoutDirection
): Arrangement.Horizontal {
    return AlignmentConverter.mainAxisToHorizontal(
        this.toShared(),
        layoutDirection.toShared()
    ).toCompose()
}

/**
 * Convert Flutter MainAxisAlignment directly to Compose Vertical Arrangement
 * Uses shared AlignmentConverter for consistent cross-platform behavior
 */
fun com.augmentalis.avaelements.flutter.layout.MainAxisAlignment.toVerticalArrangement(): Arrangement.Vertical {
    return AlignmentConverter.mainAxisToVertical(this.toShared()).toCompose()
}

/**
 * Convert Flutter CrossAxisAlignment directly to Compose Vertical Alignment
 * Uses shared AlignmentConverter for consistent cross-platform behavior
 */
fun com.augmentalis.avaelements.flutter.layout.CrossAxisAlignment.toVerticalAlignment(): Alignment.Vertical {
    return AlignmentConverter.crossAxisToVertical(this.toShared()).toCompose()
}

/**
 * Convert Flutter CrossAxisAlignment directly to Compose Horizontal Alignment
 * Uses shared AlignmentConverter for consistent cross-platform behavior
 */
fun com.augmentalis.avaelements.flutter.layout.CrossAxisAlignment.toHorizontalAlignment(): Alignment.Horizontal {
    return AlignmentConverter.crossAxisToHorizontal(this.toShared()).toCompose()
}
