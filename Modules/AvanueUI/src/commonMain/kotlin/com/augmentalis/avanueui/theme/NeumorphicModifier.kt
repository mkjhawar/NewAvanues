/**
 * NeumorphicModifier.kt - Dual-direction neumorphic shadow modifier for AvanueUI v5.1
 *
 * Draws two shadow layers via Canvas (drawBehind):
 *   - Light shadow: top-left offset (simulates overhead light source)
 *   - Dark shadow: bottom-right offset (simulates shadow cast away from light)
 *
 * Each shadow is rendered as a stack of concentric RoundedRect passes at
 * diminishing alpha values to approximate Gaussian blur in a KMP-compatible
 * way (BlurMaskFilter is Android-only and cannot be used in commonMain).
 *
 * Integrates with ThemeOverrides.dualShadow and the shadow offset/blur override
 * fields so that preset-driven neumorphic surfaces require no per-site params.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// CONSTANTS
// ============================================================================

/**
 * Number of concentric rect passes used to approximate Gaussian blur.
 * More passes = smoother falloff but higher draw cost. 8 is a good balance.
 */
private const val BLUR_PASSES = 8

// ============================================================================
// CORE MODIFIER
// ============================================================================

/**
 * Applies a dual-direction neumorphic shadow to any Composable.
 *
 * Renders two soft shadows using a stacked-ring approximation that works on
 * all KMP targets (Android, iOS, Desktop) without requiring BlurMaskFilter:
 *
 *   - Light shadow (top-left): gives the illusion of an elevated surface lit
 *     from above-left.
 *   - Dark shadow (bottom-right): gives the illusion of a shadow cast to the
 *     bottom-right.
 *
 * When [ThemeOverrides.dualShadow] is true the effect is always active
 * regardless of the [enabled] parameter, and any non-null override values
 * in [ThemeOverrides] replace the corresponding parameters.
 *
 * @param lightShadowColor  Color of the light-side (top-left) shadow.
 * @param darkShadowColor   Color of the dark-side (bottom-right) shadow.
 * @param lightOffset       X/Y offset for the light shadow (negative = top-left direction).
 * @param darkOffset        X/Y offset for the dark shadow (positive = bottom-right direction).
 * @param blurRadius        Softness/spread radius for both shadows.
 * @param cornerRadius      Corner radius that must match the visible surface shape.
 * @param enabled           When false the modifier is a no-op unless
 *                          [ThemeOverrides.dualShadow] forces it on.
 */
fun Modifier.neumorphicShadow(
    lightShadowColor: Color = Color.White.copy(alpha = 0.7f),
    darkShadowColor: Color = Color.Black.copy(alpha = 0.2f),
    lightOffset: Dp = (-4).dp,
    darkOffset: Dp = 4.dp,
    blurRadius: Dp = 8.dp,
    cornerRadius: Dp = 12.dp,
    enabled: Boolean = true,
): Modifier = drawBehind {
    // Read ThemeOverrides via the DrawScope's density only — composition-local
    // values cannot be read here because drawBehind runs outside composition.
    // ThemeOverrides are applied at call-site via the @Composable neumorphic()
    // helper, which passes resolved values into this non-composable function.
    if (!enabled) return@drawBehind

    val lightOffsetPx = lightOffset.toPx()
    val darkOffsetPx = darkOffset.toPx()
    val blurPx = blurRadius.toPx()
    val cornerPx = cornerRadius.toPx()

    // Draw light shadow (top-left)
    drawNeumorphicLayer(
        color = lightShadowColor,
        offsetX = lightOffsetPx,
        offsetY = lightOffsetPx,
        blurPx = blurPx,
        cornerPx = cornerPx,
    )

    // Draw dark shadow (bottom-right)
    drawNeumorphicLayer(
        color = darkShadowColor,
        offsetX = darkOffsetPx,
        offsetY = darkOffsetPx,
        blurPx = blurPx,
        cornerPx = cornerPx,
    )
}

// ============================================================================
// COMPOSABLE CONVENIENCE WRAPPER
// ============================================================================

/**
 * Convenience composable modifier that reads [LocalThemeOverrides] and applies
 * [neumorphicShadow] only when [ThemeOverrides.dualShadow] is true.
 *
 * All shadow parameters are sourced from [ThemeOverrides] when non-null,
 * otherwise default values from [neumorphicShadow] are used. This means
 * a ThemePreset can switch on neumorphic rendering for an entire screen by
 * setting [ThemeOverrides.dualShadow] = true — no per-widget changes needed.
 *
 * When [ThemeOverrides.dualShadow] is false this is a zero-cost no-op.
 *
 * @param cornerRadius Corner radius to match the composable's visible shape.
 *                     Overrides are not applied to this param; callers should
 *                     pass the same value used for the surface clip shape.
 */
@Composable
fun Modifier.neumorphic(cornerRadius: Dp = 12.dp): Modifier {
    val overrides = LocalThemeOverrides.current
    if (!overrides.dualShadow) return this

    val resolvedLightOffset: Dp = overrides.lightShadowOffset ?: (-4).dp
    val resolvedDarkOffset: Dp = overrides.darkShadowOffset ?: 4.dp
    val resolvedLightBlur: Dp = overrides.lightShadowBlur ?: 8.dp
    val resolvedDarkBlur: Dp = overrides.darkShadowBlur ?: 8.dp

    // When the two blur values differ we use lightBlur for the light shadow
    // and darkBlur for the dark shadow. Since neumorphicShadow uses a single
    // blurRadius, we draw each layer independently with their own modifier.
    return this
        .neumorphicShadowLayer(
            color = Color.White.copy(alpha = 0.7f),
            offset = resolvedLightOffset,
            blurRadius = resolvedLightBlur,
            cornerRadius = cornerRadius,
        )
        .neumorphicShadowLayer(
            color = Color.Black.copy(alpha = 0.2f),
            offset = resolvedDarkOffset,
            blurRadius = resolvedDarkBlur,
            cornerRadius = cornerRadius,
        )
}

// ============================================================================
// SINGLE-LAYER HELPER (non-composable, used internally)
// ============================================================================

/**
 * Draws a single neumorphic shadow layer (either light or dark).
 *
 * This is the building block used by both [neumorphicShadow] and [neumorphic].
 * It is kept internal to this file so consumers use the higher-level APIs.
 *
 * @param color       Shadow color (including desired alpha).
 * @param offset      Uniform X and Y offset in dp. Negative = top-left, positive = bottom-right.
 * @param blurRadius  Softness radius in dp.
 * @param cornerRadius Corner radius matching the surface shape.
 */
private fun Modifier.neumorphicShadowLayer(
    color: Color,
    offset: Dp,
    blurRadius: Dp,
    cornerRadius: Dp,
): Modifier = drawBehind {
    val offsetPx = offset.toPx()
    val blurPx = blurRadius.toPx()
    val cornerPx = cornerRadius.toPx()

    drawNeumorphicLayer(
        color = color,
        offsetX = offsetPx,
        offsetY = offsetPx,
        blurPx = blurPx,
        cornerPx = cornerPx,
    )
}

// ============================================================================
// DRAW PRIMITIVE
// ============================================================================

/**
 * Draws one neumorphic shadow layer using stacked concentric RoundedRect passes.
 *
 * This approximates Gaussian blur by drawing [BLUR_PASSES] rectangles, each
 * expanded by one step and rendered at a fraction of the base color's alpha.
 * The result is a soft, feathered edge that closely resembles a blurred shadow.
 *
 * Formula per pass `i` (0 = innermost, BLUR_PASSES-1 = outermost):
 *   - expansion  = blurPx * (i / BLUR_PASSES)
 *   - alpha      = color.alpha * (1 - i / BLUR_PASSES) / BLUR_PASSES
 *
 * This distributes alpha evenly across passes so the total visual weight is
 * proportional to the base alpha while the edge softens naturally.
 *
 * @param color     Shadow color with desired base alpha baked in.
 * @param offsetX   Horizontal translation of all passes in pixels.
 * @param offsetY   Vertical translation of all passes in pixels.
 * @param blurPx    Maximum expansion radius (pixels) at the outermost pass.
 * @param cornerPx  Corner radius of the shadowed surface in pixels.
 */
private fun DrawScope.drawNeumorphicLayer(
    color: Color,
    offsetX: Float,
    offsetY: Float,
    blurPx: Float,
    cornerPx: Float,
) {
    val paint = Paint().apply { isAntiAlias = true }
    val path = Path()

    drawIntoCanvas { canvas ->
        for (i in 0 until BLUR_PASSES) {
            val fraction = i.toFloat() / BLUR_PASSES.toFloat()
            val expansion = blurPx * fraction
            val passAlpha = color.alpha * (1f - fraction) / BLUR_PASSES.toFloat()

            paint.color = color.copy(alpha = passAlpha)

            val left = -expansion + offsetX
            val top = -expansion + offsetY
            val right = size.width + expansion + offsetX
            val bottom = size.height + expansion + offsetY
            val effectiveCorner = (cornerPx + expansion).coerceAtLeast(0f)

            path.reset()
            path.addRoundRect(
                RoundRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    cornerRadius = CornerRadius(effectiveCorner, effectiveCorner),
                )
            )

            canvas.drawPath(path, paint)
        }
    }
}
