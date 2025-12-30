package com.augmentalis.avaelements.renderer.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.common.alignment.*
import com.augmentalis.avaelements.common.color.UniversalColor
import com.augmentalis.avaelements.common.color.ColorUtils
import com.augmentalis.avaelements.common.spacing.*
import androidx.compose.ui.unit.LayoutDirection as ComposeLayoutDirection
import androidx.compose.foundation.layout.Arrangement

/**
 * Desktop Bridge for Shared Utilities
 *
 * Converts shared cross-platform types (from com.augmentalis.avaelements.common.*)
 * to Compose Desktop equivalents.
 *
 * This bridge eliminates the need for duplicate conversion logic in Desktop mappers
 * and ensures consistency with other platform renderers.
 *
 * Agent 3: Desktop & Web Renderer - Shared Utilities Integration
 * @since 3.0.0-shared-utilities
 */

// ═══════════════════════════════════════════════════════════════
// Color Conversion
// ═══════════════════════════════════════════════════════════════

/**
 * Convert UniversalColor to Compose Color
 */
fun UniversalColor.toComposeColor(): Color {
    return Color(
        red = this.red,
        green = this.green,
        blue = this.blue,
        alpha = this.alpha
    )
}

/**
 * Convert Compose Color to UniversalColor
 */
fun Color.toUniversalColor(): UniversalColor {
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
fun Int.argbToComposeColor(): Color {
    return UniversalColor.fromArgb(this).toComposeColor()
}

// ═══════════════════════════════════════════════════════════════
// Spacing Conversion
// ═══════════════════════════════════════════════════════════════

/**
 * Convert EdgeInsets to Compose PaddingValues
 */
fun EdgeInsets.toPaddingValues(layoutDirection: ComposeLayoutDirection = ComposeLayoutDirection.Ltr): PaddingValues {
    return PaddingValues(
        start = this.start.dp,
        top = this.top.dp,
        end = this.end.dp,
        bottom = this.bottom.dp
    )
}

/**
 * Convert EdgeInsets to individual padding values
 */
fun EdgeInsets.toStartPadding(): Dp = this.start.dp
fun EdgeInsets.toTopPadding(): Dp = this.top.dp
fun EdgeInsets.toEndPadding(): Dp = this.end.dp
fun EdgeInsets.toBottomPadding(): Dp = this.bottom.dp

/**
 * Convert CornerRadius to Compose Shape
 */
fun CornerRadius.toShape(): Shape {
    return if (this.isUniform) {
        RoundedCornerShape(this.uniform.dp)
    } else {
        RoundedCornerShape(
            topStart = this.topStart.dp,
            topEnd = this.topEnd.dp,
            bottomStart = this.bottomStart.dp,
            bottomEnd = this.bottomEnd.dp
        )
    }
}

/**
 * Convert Border to Compose BorderStroke
 */
fun Border.toBorderStroke(): BorderStroke? {
    if (!this.isVisible) return null

    return BorderStroke(
        width = this.width.dp,
        color = this.color.argbToComposeColor()
    )
}

/**
 * Convert Size to Compose Dp (width and height)
 */
fun com.augmentalis.avaelements.common.spacing.Size.toWidthDp(): Dp {
    return if (this.width.isNaN()) Dp.Unspecified else this.width.dp
}

fun com.augmentalis.avaelements.common.spacing.Size.toHeightDp(): Dp {
    return if (this.height.isNaN()) Dp.Unspecified else this.height.dp
}

// ═══════════════════════════════════════════════════════════════
// Alignment Conversion
// ═══════════════════════════════════════════════════════════════

/**
 * Convert LayoutDirection to Compose LayoutDirection
 */
fun LayoutDirection.toComposeLayoutDirection(): ComposeLayoutDirection {
    return when (this) {
        LayoutDirection.Ltr -> ComposeLayoutDirection.Ltr
        LayoutDirection.Rtl -> ComposeLayoutDirection.Rtl
    }
}

/**
 * Convert Compose LayoutDirection to common LayoutDirection
 */
fun ComposeLayoutDirection.toCommonLayoutDirection(): LayoutDirection {
    return when (this) {
        ComposeLayoutDirection.Ltr -> LayoutDirection.Ltr
        ComposeLayoutDirection.Rtl -> LayoutDirection.Rtl
    }
}

/**
 * Convert HorizontalAlignment to Compose Alignment.Horizontal
 */
fun com.augmentalis.avaelements.common.alignment.HorizontalAlignment.toComposeAlignment(): Alignment.Horizontal {
    return when (this) {
        com.augmentalis.avaelements.common.alignment.HorizontalAlignment.Start -> Alignment.Start
        com.augmentalis.avaelements.common.alignment.HorizontalAlignment.End -> Alignment.End
        com.augmentalis.avaelements.common.alignment.HorizontalAlignment.Center -> Alignment.CenterHorizontally
    }
}

/**
 * Convert VerticalAlignment to Compose Alignment.Vertical
 */
fun com.augmentalis.avaelements.common.alignment.VerticalAlignment.toComposeAlignment(): Alignment.Vertical {
    return when (this) {
        com.augmentalis.avaelements.common.alignment.VerticalAlignment.Top -> Alignment.Top
        com.augmentalis.avaelements.common.alignment.VerticalAlignment.Bottom -> Alignment.Bottom
        com.augmentalis.avaelements.common.alignment.VerticalAlignment.Center -> Alignment.CenterVertically
    }
}

/**
 * Convert HorizontalArrangement to Compose Arrangement.Horizontal
 */
fun com.augmentalis.avaelements.common.alignment.HorizontalArrangement.toComposeArrangement(): Arrangement.Horizontal {
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
 * Convert VerticalArrangement to Compose Arrangement.Vertical
 */
fun com.augmentalis.avaelements.common.alignment.VerticalArrangement.toComposeArrangement(): Arrangement.Vertical {
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

// ═══════════════════════════════════════════════════════════════
// RTL-Aware Alignment Helpers
// ═══════════════════════════════════════════════════════════════

/**
 * Convert WrapAlignment to Compose Horizontal Arrangement with RTL support.
 * Uses shared AlignmentConverter logic.
 */
fun WrapAlignment.toComposeHorizontalArrangement(layoutDirection: ComposeLayoutDirection): Arrangement.Horizontal {
    val commonLayoutDirection = layoutDirection.toCommonLayoutDirection()
    val commonArrangement = AlignmentConverter.wrapToHorizontal(this, commonLayoutDirection)
    return commonArrangement.toComposeArrangement()
}

/**
 * Convert WrapAlignment to Compose Vertical Arrangement.
 * Uses shared AlignmentConverter logic.
 */
fun WrapAlignment.toComposeVerticalArrangement(): Arrangement.Vertical {
    val commonArrangement = AlignmentConverter.wrapToVertical(this)
    return commonArrangement.toComposeArrangement()
}

/**
 * Convert MainAxisAlignment to Compose Horizontal Arrangement with RTL support.
 * Uses shared AlignmentConverter logic.
 */
fun MainAxisAlignment.toComposeHorizontalArrangement(layoutDirection: ComposeLayoutDirection): Arrangement.Horizontal {
    val commonLayoutDirection = layoutDirection.toCommonLayoutDirection()
    val commonArrangement = AlignmentConverter.mainAxisToHorizontal(this, commonLayoutDirection)
    return commonArrangement.toComposeArrangement()
}

/**
 * Convert MainAxisAlignment to Compose Vertical Arrangement.
 * Uses shared AlignmentConverter logic.
 */
fun MainAxisAlignment.toComposeVerticalArrangement(): Arrangement.Vertical {
    val commonArrangement = AlignmentConverter.mainAxisToVertical(this)
    return commonArrangement.toComposeArrangement()
}

/**
 * Convert CrossAxisAlignment to Compose Vertical Alignment.
 * Uses shared AlignmentConverter logic.
 */
fun CrossAxisAlignment.toComposeVerticalAlignment(): Alignment.Vertical {
    val commonAlignment = AlignmentConverter.crossAxisToVertical(this)
    return commonAlignment.toComposeAlignment()
}

/**
 * Convert CrossAxisAlignment to Compose Horizontal Alignment.
 * Uses shared AlignmentConverter logic.
 */
fun CrossAxisAlignment.toComposeHorizontalAlignment(): Alignment.Horizontal {
    val commonAlignment = AlignmentConverter.crossAxisToHorizontal(this)
    return commonAlignment.toComposeAlignment()
}

// ═══════════════════════════════════════════════════════════════
// Color Manipulation Helpers
// ═══════════════════════════════════════════════════════════════

/**
 * Lighten a Compose Color by a factor.
 * Uses shared ColorUtils logic.
 */
fun Color.lighten(factor: Float): Color {
    return this.toUniversalColor().lighten(factor).toComposeColor()
}

/**
 * Darken a Compose Color by a factor.
 * Uses shared ColorUtils logic.
 */
fun Color.darken(factor: Float): Color {
    return this.toUniversalColor().darken(factor).toComposeColor()
}

/**
 * Get contrasting foreground color (black or white) for the background.
 * Uses shared ColorUtils logic.
 */
fun Color.contrastingForeground(): Color {
    return this.toUniversalColor().contrastingForeground().toComposeColor()
}

/**
 * Mix two colors together.
 * Uses shared ColorUtils logic.
 */
fun Color.mix(other: Color, ratio: Float): Color {
    return this.toUniversalColor().mix(other.toUniversalColor(), ratio).toComposeColor()
}

// ═══════════════════════════════════════════════════════════════
// Convenience Extensions
// ═══════════════════════════════════════════════════════════════

/**
 * Create EdgeInsets from Dp values
 */
fun paddingOf(all: Dp): EdgeInsets = EdgeInsets.all(all.value)
fun paddingOf(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): EdgeInsets =
    EdgeInsets.symmetric(horizontal.value, vertical.value)
fun paddingOf(start: Dp = 0.dp, top: Dp = 0.dp, end: Dp = 0.dp, bottom: Dp = 0.dp): EdgeInsets =
    EdgeInsets(start.value, top.value, end.value, bottom.value)

/**
 * Create CornerRadius from Dp
 */
fun cornerRadiusOf(all: Dp): CornerRadius = CornerRadius.all(all.value)
fun cornerRadiusOf(topStart: Dp, topEnd: Dp, bottomStart: Dp, bottomEnd: Dp): CornerRadius =
    CornerRadius(topStart.value, topEnd.value, bottomStart.value, bottomEnd.value)
