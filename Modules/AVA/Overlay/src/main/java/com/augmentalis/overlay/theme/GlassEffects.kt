// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/theme/GlassEffects.kt
// created: 2025-11-01 22:30:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 2 - Glassmorphic UI
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphic effect modifiers for AVA overlay UI.
 *
 * Provides reusable modifiers for creating translucent glass effects
 * with blur, shadows, and borders matching VisionOS aesthetic.
 *
 * @author Manoj Jhawar
 */

/**
 * Apply translucent glass background with blur effect
 *
 * @param color Base color for glass (default: dark gray)
 * @param alpha Transparency level (0.0-1.0)
 * @param blurRadius Blur radius in dp (Android 12+ only)
 */
fun Modifier.glassBackground(
    color: Color = Color(0x1E, 0x1E, 0x20),
    alpha: Float = 0.7f,
    blurRadius: Dp = 24.dp
): Modifier = this
    .graphicsLayer {
        // Apply blur effect on Android 12+ (API 31)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            renderEffect = RenderEffect.createBlurEffect(
                blurRadius.toPx(),
                blurRadius.toPx(),
                Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        }
    }
    .background(color.copy(alpha = alpha))

/**
 * Apply glass border (thin white stroke with low opacity)
 *
 * @param shape Shape for the border
 * @param borderAlpha Border opacity (0.0-1.0)
 */
fun Modifier.glassBorder(
    shape: Shape = RoundedCornerShape(24.dp),
    borderAlpha: Float = 0.15f
): Modifier = this
    .border(1.dp, Color.White.copy(borderAlpha), shape)

/**
 * Apply soft elevation shadow for glass effect
 *
 * @param elevation Shadow elevation
 * @param shape Shape for the shadow
 */
fun Modifier.glassShadow(
    elevation: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color.Black.copy(0.25f),
        spotColor = Color.Black.copy(0.25f)
    )

/**
 * Full glass effect combining all glassmorphic properties
 *
 * Applies shadow, background with blur, border, and clipping in correct order
 * for optimal visual appearance matching VisionOS design language.
 *
 * @param color Base glass color
 * @param alpha Transparency level
 * @param blurRadius Blur effect radius (API 31+)
 * @param borderAlpha Border opacity
 * @param elevation Shadow elevation
 * @param shape Overall shape
 */
fun Modifier.glassEffect(
    color: Color = Color(0x1E, 0x1E, 0x20),
    alpha: Float = 0.7f,
    blurRadius: Dp = 24.dp,
    borderAlpha: Float = 0.15f,
    elevation: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier = this
    .glassShadow(elevation, shape)
    .clip(shape)
    .glassBackground(color, alpha, blurRadius)
    .glassBorder(shape, borderAlpha)

/**
 * Glass effect for collapsed orb (higher opacity, stronger blur)
 */
fun Modifier.orbGlassEffect(): Modifier = glassEffect(
    color = Color(0x1E, 0x1E, 0x20),
    alpha = 0.8f,
    blurRadius = 20.dp,
    borderAlpha = 0.2f,
    elevation = 8.dp,
    shape = androidx.compose.foundation.shape.CircleShape
)

/**
 * Glass effect for expanded panel (lower opacity, softer appearance)
 */
fun Modifier.panelGlassEffect(): Modifier = glassEffect(
    color = Color(0x1E, 0x1E, 0x20),
    alpha = 0.7f,
    blurRadius = 28.dp,
    borderAlpha = 0.15f,
    elevation = 10.dp,
    shape = RoundedCornerShape(24.dp)
)

// =========================================================================
// Ocean Glass Design v2.3 - Solid Color Variants (No Transparency Issues)
// =========================================================================

/**
 * Ocean Glass color palette - solid colors for overlay stability.
 *
 * @deprecated Use AvanueTheme.colors.* in composable contexts.
 * This object is kept for non-composable Modifier extensions that
 * cannot access the composition context. Migrate composables to
 * AvanueTheme.colors.background, .surface, .primary, .textPrimary, .borderSubtle.
 */
@Deprecated("Use AvanueTheme.colors.* in composable contexts")
object OceanGlassColors {
    val OceanDarker = Color(0xFF0F172A)    // Solid dark ocean
    val OceanDark = Color(0xFF1E293B)      // Primary panel background
    val OceanMedium = Color(0xFF334155)    // Secondary elements
    val CoralBlue = Color(0xFF3B82F6)      // Accent color
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFCBD5E1)
    val Border = Color(0xFF475569)
}

/**
 * Solid orb effect - Ocean Glass v2.3
 *
 * Uses solid colors instead of transparency to avoid rendering issues
 * on overlays. Maintains visual appeal through layered colors and shadows.
 */
fun Modifier.orbSolidEffect(): Modifier = this
    .shadow(
        elevation = 12.dp,
        shape = androidx.compose.foundation.shape.CircleShape,
        ambientColor = OceanGlassColors.CoralBlue.copy(alpha = 0.3f),
        spotColor = Color.Black.copy(alpha = 0.4f)
    )
    .clip(androidx.compose.foundation.shape.CircleShape)
    .background(OceanGlassColors.OceanDark)
    .border(1.dp, OceanGlassColors.Border, androidx.compose.foundation.shape.CircleShape)

/**
 * Solid panel effect - Ocean Glass v2.3
 *
 * Stable panel background without transparency artifacts.
 * Uses layered solid colors for depth perception.
 */
fun Modifier.panelSolidEffect(): Modifier = this
    .shadow(
        elevation = 16.dp,
        shape = RoundedCornerShape(24.dp),
        ambientColor = Color.Black.copy(alpha = 0.3f),
        spotColor = Color.Black.copy(alpha = 0.4f)
    )
    .clip(RoundedCornerShape(24.dp))
    .background(OceanGlassColors.OceanDarker)
    .border(1.dp, OceanGlassColors.Border, RoundedCornerShape(24.dp))

/**
 * Solid chip effect for suggestions - Ocean Glass v2.3
 */
fun Modifier.chipSolidEffect(): Modifier = this
    .clip(RoundedCornerShape(16.dp))
    .background(OceanGlassColors.OceanMedium)
    .border(1.dp, OceanGlassColors.Border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
