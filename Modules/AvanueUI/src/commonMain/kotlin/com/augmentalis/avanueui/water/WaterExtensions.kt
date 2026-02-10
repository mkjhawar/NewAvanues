/**
 * WaterExtensions.kt - Primary developer-facing Water effect API
 *
 * Modifier.waterEffect() applies Apple Liquid Glass-inspired dynamic effects.
 * Automatically falls back to Modifier.glass() when WaterLevel.IDENTITY or
 * when the platform doesn't support advanced rendering.
 *
 * Coexists with Glass: both component families are available.
 * Glass = static frosted overlay. Water = dynamic refraction + specular + caustics.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.water

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.glass.GlassDefaults
import com.augmentalis.avanueui.components.glass.glass
import com.augmentalis.avanueui.glass.GlassBorder
import com.augmentalis.avanueui.glass.GlassLevel
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.ShapeTokens
import com.augmentalis.avanueui.tokens.WaterTokens

// ============================================================================
// WATER BORDER
// ============================================================================

/**
 * Border configuration for water components.
 * Gradient border that fades from top to bottom (Apple hairline style).
 */
data class WaterBorder(
    val width: Dp,
    val topColor: Color,
    val bottomColor: Color
) {
    /** Convenience constructor with a single color at different opacities */
    constructor(width: Dp, color: Color) : this(
        width = width,
        topColor = color.copy(alpha = WaterTokens.borderOpacityTop),
        bottomColor = color.copy(alpha = WaterTokens.borderOpacityBottom)
    )
}

// ============================================================================
// WATER SHAPES
// ============================================================================

object WaterShapes {
    val default: Shape = RoundedCornerShape(ShapeTokens.md)   // 12.dp
    val capsule: Shape = RoundedCornerShape(ShapeTokens.full)  // 9999.dp
    val small: Shape = RoundedCornerShape(ShapeTokens.sm)      // 8.dp
    val large: Shape = RoundedCornerShape(ShapeTokens.lg)      // 16.dp
    val circle: Shape = CircleShape
    val navigationBar: Shape = RoundedCornerShape(ShapeTokens.xxl) // 24.dp
}

// ============================================================================
// WATER DEFAULTS
// ============================================================================

object WaterDefaults {
    val shape: Shape = WaterShapes.default

    /** Hairline border with theme-tinted gradient (Apple Liquid Glass style) */
    val border: WaterBorder
        @Composable get() = WaterBorder(
            width = WaterTokens.borderWidth,
            color = AvanueTheme.water.borderTint
        )

    /** Even more subtle border for secondary surfaces */
    val borderSubtle: WaterBorder
        @Composable get() = WaterBorder(
            width = WaterTokens.borderWidth,
            topColor = AvanueTheme.water.borderTint.copy(alpha = WaterTokens.borderOpacityTop * 0.5f),
            bottomColor = AvanueTheme.water.borderTint.copy(alpha = WaterTokens.borderOpacityBottom * 0.5f)
        )
}

// ============================================================================
// ACCESSIBILITY
// ============================================================================

/**
 * Resolves the effective WaterLevel, respecting system accessibility settings.
 *
 * - Reduce transparency enabled -> IDENTITY (no effect)
 * - Reduce motion enabled -> keeps blur but disables caustics/shimmer
 * - Otherwise -> requested level
 *
 * All Water components call this internally, so accessibility is automatic.
 */
@Composable
fun rememberEffectiveWaterLevel(requested: WaterLevel): WaterLevel {
    // WaterLevel.IDENTITY already means "no effect" — pass through
    if (requested == WaterLevel.IDENTITY) return WaterLevel.IDENTITY

    // Platform accessibility checks are handled by platformWaterEffect()
    // which degrades gracefully. Here we just pass through the requested level.
    // The actual IDENTITY override for "reduce transparency" happens at the
    // platform renderer level where system settings are accessible.
    return requested
}

// ============================================================================
// WATER EFFECT MODIFIER
// ============================================================================

/**
 * Apply Apple Liquid Glass-inspired water effect to any composable.
 *
 * This is the primary API for adding water effects. It handles:
 * - Platform capability detection and automatic fallback to glass
 * - WaterLevel-based parameter scaling
 * - Gradient border rendering
 * - Interactive press/shimmer behaviors (when [interactive] = true)
 *
 * @param backgroundColor Base surface color
 * @param waterLevel Effect intensity (REGULAR, CLEAR, IDENTITY)
 * @param shape Shape for clipping and border
 * @param border Optional gradient border (null = no border)
 * @param interactive Enable press scale + shimmer illumination
 */
@Composable
fun Modifier.waterEffect(
    backgroundColor: Color = AvanueTheme.colors.surface,
    waterLevel: WaterLevel = WaterLevel.REGULAR,
    shape: Shape = WaterDefaults.shape,
    border: WaterBorder? = WaterDefaults.border,
    interactive: Boolean = false
): Modifier {
    val effectiveLevel = rememberEffectiveWaterLevel(waterLevel)

    // IDENTITY level: fall back to basic glass overlay (no dynamic effects)
    if (effectiveLevel == WaterLevel.IDENTITY) {
        return this.glass(
            backgroundColor = backgroundColor,
            glassLevel = GlassLevel.LIGHT,
            border = border?.let { GlassBorder(it.width, it.topColor) },
            shape = shape
        )
    }

    // Apply platform-specific water rendering
    val waterModifier = this.platformWaterEffect(
        backgroundColor = backgroundColor,
        waterLevel = effectiveLevel,
        shape = shape,
        interactive = interactive
    )

    // Apply gradient border on top of the water effect
    return if (border != null) {
        waterModifier.border(
            width = border.width,
            brush = Brush.verticalGradient(
                colors = listOf(border.topColor, border.bottomColor)
            ),
            shape = shape
        )
    } else {
        waterModifier
    }
}
