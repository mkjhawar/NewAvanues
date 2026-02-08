/**
 * GlassExtensions.kt - Glassmorphic modifiers, shapes, and defaults
 *
 * Replaces deleted OceanThemeExtensions.kt (OceanTheme → AvanueTheme.colors)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avamagic.ui.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.glass.GlassBorder
import com.augmentalis.avanueui.glass.GlassLevel
import com.augmentalis.avanueui.theme.AvanueTheme

// ============================================================================
// GLASS MODIFIER
// ============================================================================

/**
 * Apply glassmorphic effect to any composable.
 *
 * @param backgroundColor Base color (will be made translucent)
 * @param glassLevel Effect strength (LIGHT, MEDIUM, HEAVY)
 * @param border Optional border configuration
 * @param shape Shape for border (if border provided)
 */
fun Modifier.glass(
    backgroundColor: Color,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    border: GlassBorder? = null,
    shape: Shape = GlassShapes.default
): Modifier {
    val opacity = when (glassLevel) {
        GlassLevel.LIGHT -> 0.10f
        GlassLevel.MEDIUM -> 0.15f
        GlassLevel.HEAVY -> 0.22f
    }

    val luminance = 0.2126f * backgroundColor.red +
        0.7152f * backgroundColor.green +
        0.0722f * backgroundColor.blue
    val glassOverlay = if (luminance < 0.5f) {
        Color.White.copy(alpha = opacity)
    } else {
        Color.Black.copy(alpha = opacity)
    }

    return this
        .background(glassOverlay)
        .then(
            if (border != null) {
                Modifier.border(
                    width = border.width,
                    color = border.color,
                    shape = shape
                )
            } else Modifier
        )
}

// ============================================================================
// GLASS SHAPES
// ============================================================================

object GlassShapes {
    val default: Shape = RoundedCornerShape(12.dp)
    val small: Shape = RoundedCornerShape(8.dp)
    val large: Shape = RoundedCornerShape(16.dp)
    val extraLarge: Shape = RoundedCornerShape(24.dp)
    val chipShape: Shape = RoundedCornerShape(8.dp)
    val buttonShape: Shape = RoundedCornerShape(12.dp)
    val fabShape: Shape = RoundedCornerShape(16.dp)
    val dialogShape: Shape = RoundedCornerShape(16.dp)
    val bottomSheetShape: Shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = 0.dp, bottomEnd = 0.dp
    )
    val bubbleStart: Shape = RoundedCornerShape(
        topStart = 2.dp, topEnd = 12.dp,
        bottomStart = 12.dp, bottomEnd = 12.dp
    )
    val bubbleEnd: Shape = RoundedCornerShape(
        topStart = 12.dp, topEnd = 2.dp,
        bottomStart = 12.dp, bottomEnd = 12.dp
    )
    val circle: Shape = CircleShape
}

// ============================================================================
// GLASS DEFAULTS
// ============================================================================

object GlassDefaults {
    val shape: Shape = GlassShapes.default

    val border: GlassBorder
        @Composable get() = GlassBorder(
            width = 1.dp,
            color = AvanueTheme.colors.border
        )

    val borderSubtle: GlassBorder
        @Composable get() = GlassBorder(
            width = 0.5.dp,
            color = AvanueTheme.colors.borderSubtle
        )

    val borderStrong: GlassBorder
        @Composable get() = GlassBorder(
            width = 1.5.dp,
            color = AvanueTheme.colors.borderStrong
        )

    val borderFocused: GlassBorder
        @Composable get() = GlassBorder(
            width = 2.dp,
            color = AvanueTheme.colors.primary
        )

    @Composable
    fun cardColors(): CardColors = CardDefaults.cardColors(
        containerColor = AvanueTheme.colors.surface,
        contentColor = AvanueTheme.colors.textPrimary,
        disabledContainerColor = AvanueTheme.colors.surface.copy(alpha = 0.5f),
        disabledContentColor = AvanueTheme.colors.textDisabled
    )

    val spacing: Dp = 16.dp
    val spacingSmall: Dp = 8.dp
    val spacingLarge: Dp = 24.dp
    val minTouchTarget: Dp = 48.dp
    val elevation: Dp = 0.dp
    val elevationElevated: Dp = 2.dp
}
