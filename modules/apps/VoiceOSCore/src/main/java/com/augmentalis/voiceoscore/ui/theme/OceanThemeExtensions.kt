/**
 * OceanThemeExtensions.kt - Ocean Theme Color Tokens & Extensions
 *
 * Centralized theme definitions for Ocean glassmorphic design.
 * All color, gradient, shape, and default values for the Ocean theme.
 *
 * ARCHITECTURE: Migration-Ready Design
 * - When MagicUI is ready: Replace with MagicUI.Theme.Ocean
 * - All components reference these tokens
 * - Single source of truth for theme values
 *
 * Created: 2025-12-03
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ocean Color Palette
 * Future: Replace with MagicUI.Theme.Ocean.Colors
 */
object OceanColors {
    // Primary ocean blues
    val DeepOcean = Color(0xFF0A1929)        // Dark blue background
    val MidOcean = Color(0xFF1A2F42)         // Medium blue
    val ShallowOcean = Color(0xFF2A4A68)     // Light blue

    // Accent teals
    val TealPrimary = Color(0xFF00BCD4)      // Bright teal
    val TealSecondary = Color(0xFF26C6DA)    // Light teal
    val TealGlow = Color(0xFF4DD0E1)         // Glow teal

    // Text colors
    val TextPrimary = Color(0xFFFFFFFF)      // White
    val TextSecondary = Color(0xB3FFFFFF)    // 70% white
    val TextHint = Color(0x80FFFFFF)         // 50% white

    // Status colors
    val Success = Color(0xFF00E676)          // Green
    val Error = Color(0xFFFF5252)            // Red
    val Warning = Color(0xFFFFD740)          // Yellow
}

/**
 * Glass Effect Colors
 * Future: Replace with MagicUI.Theme.Ocean.Glass
 */
object OceanGlass {
    val Surface = Color(0x1AFFFFFF)          // 10% white - glass surface
    val Border = Color(0x33FFFFFF)           // 20% white - glass border
    val Blur = Color(0x0DFFFFFF)             // 5% white - blur overlay
}

/**
 * Ocean Gradients
 * Future: Replace with MagicUI.Theme.Ocean.Gradients
 */
object OceanGradients {
    /**
     * Background gradient - Deep ocean to mid ocean
     */
    val Background = Brush.verticalGradient(
        colors = listOf(
            OceanColors.DeepOcean,
            OceanColors.MidOcean
        )
    )

    /**
     * Glass effect gradient - Subtle white gradient
     */
    val Glass = Brush.verticalGradient(
        colors = listOf(
            OceanGlass.Surface,
            OceanGlass.Blur
        )
    )

    /**
     * Teal accent gradient - Primary to glow
     */
    val Teal = Brush.horizontalGradient(
        colors = listOf(
            OceanColors.TealPrimary,
            OceanColors.TealGlow
        )
    )

    /**
     * Teal subtle gradient - For backgrounds
     */
    val TealSubtle = Brush.horizontalGradient(
        colors = listOf(
            OceanColors.TealPrimary.copy(alpha = 0.1f),
            OceanColors.TealGlow.copy(alpha = 0.1f)
        )
    )
}

/**
 * Glass Shapes
 * Future: Replace with MagicUI.Theme.Ocean.Shapes
 */
object GlassShapes {
    val Card = RoundedCornerShape(16.dp)
    val Bubble = RoundedCornerShape(20.dp)
    val Button = RoundedCornerShape(12.dp)
    val Chip = RoundedCornerShape(16.dp)
    val Panel = RoundedCornerShape(12.dp)
    val Circle = CircleShape
}

/**
 * Glass Component Defaults
 * Future: Replace with MagicUI.Theme.Ocean.Defaults
 */
object GlassDefaults {
    // Padding
    val CardPadding = 20.dp
    val ButtonPadding = 16.dp
    val ChipPaddingHorizontal = 12.dp
    val ChipPaddingVertical = 6.dp
    val PanelPadding = 24.dp

    // Border widths
    val BorderWidth = 1.dp
    val BorderWidthThick = 2.dp

    // Elevation
    val CardElevation = 8.dp
    val BubbleElevation = 2.dp
    val ButtonElevation = 4.dp

    // Sizes
    val IconSizeSmall = 16.dp
    val IconSizeMedium = 24.dp
    val IconSizeLarge = 36.dp
    val ButtonHeightStandard = 48.dp
    val CircularButtonSize = 72.dp
}

/**
 * Modifier Extensions for Ocean Theme
 * Future: Replace with MagicUI.Modifiers.glass*
 */

/**
 * Apply glass surface styling
 *
 * @param shape Shape of the surface
 * @param borderWidth Border width
 */
fun Modifier.glassSurface(
    shape: Shape = GlassShapes.Card,
    borderWidth: Dp = GlassDefaults.BorderWidth
): Modifier = this
    .border(
        width = borderWidth,
        color = OceanGlass.Border,
        shape = shape
    )
    .background(
        color = OceanGlass.Surface,
        shape = shape
    )

/**
 * Apply glass blur overlay
 */
fun Modifier.glassBlur(): Modifier = this
    .background(
        brush = OceanGradients.Glass
    )

/**
 * Apply teal gradient background
 *
 * @param shape Shape of the background
 */
fun Modifier.tealGradient(
    shape: Shape = GlassShapes.Button
): Modifier = this
    .background(
        brush = OceanGradients.Teal,
        shape = shape
    )

/**
 * Apply ocean border
 *
 * @param shape Shape of the border
 * @param width Border width
 * @param color Border color (defaults to glass border)
 */
fun Modifier.oceanBorder(
    shape: Shape = GlassShapes.Card,
    width: Dp = GlassDefaults.BorderWidth,
    color: Color = OceanGlass.Border
): Modifier = this
    .border(
        width = width,
        color = color,
        shape = shape
    )
