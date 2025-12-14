package com.augmentalis.webavanue.ui.screen.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.ui.screen.effects.glassmorphism as applyGlassmorphism
import com.augmentalis.webavanue.ui.screen.effects.supportsBlur

/**
 * Glassmorphism utility modifiers for WebAvanue browser
 *
 * Provides Ocean Blue frosted glass effects with blur and translucency.
 *
 * Android 12+ (API 31+): True backdrop blur with RenderEffect
 * Android 11 and below: Solid surfaces with same visual style
 */

/**
 * Apply glassmorphism effect with border and blur
 *
 * @param cornerRadius Corner radius for rounded edges
 * @param borderColor Border color
 * @param borderWidth Width of the border
 * @param blurRadius Blur radius (only on API 31+)
 * @param backgroundColor Background/tint color
 */
@Composable
fun Modifier.glassmorphism(
    cornerRadius: Dp = 12.dp,
    borderColor: Color = Color(0x262563EB),  // Ocean blue 15% opacity
    borderWidth: Dp = 1.dp,
    blurRadius: Float = 12f,
    backgroundColor: Color = Color(0x14334155)  // Slate 8% opacity (reduced from 12%)
): Modifier = if (supportsBlur()) {
    // API 31+: Use real blur effect
    this
        .clip(RoundedCornerShape(cornerRadius))
        .applyGlassmorphism(
            blurRadius = blurRadius,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            borderWidth = borderWidth.value
        )
} else {
    // Fallback: Solid surface with border
    this
        .clip(RoundedCornerShape(cornerRadius))
        .border(
            width = borderWidth,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
}

/**
 * Apply glassmorphism effect for tabs (subtle border)
 */
@Composable
fun Modifier.glassTab(
    isActive: Boolean = false
): Modifier = this.glassmorphism(
    cornerRadius = 8.dp,
    borderColor = if (isActive) {
        Color(0x332563EB)  // Ocean blue 20% opacity for active
    } else {
        Color(0x1A2563EB)  // Ocean blue 10% opacity for inactive
    },
    borderWidth = 1.dp,
    blurRadius = 10f,
    backgroundColor = if (isActive) {
        Color(0x14334155)  // Slate 8% opacity for active (reduced from 12%)
    } else {
        Color(0x0A1E293B)  // Slate 4% opacity for inactive (reduced from 8%)
    }
)

/**
 * Apply glassmorphism effect for command bars and toolbars
 */
@Composable
fun Modifier.glassBar(
    cornerRadius: Dp = 0.dp  // Typically no rounded corners for full-width bars
): Modifier = this.glassmorphism(
    cornerRadius = cornerRadius,
    borderColor = Color(0x1A2563EB),  // Ocean blue 10% opacity
    borderWidth = 1.dp,
    blurRadius = 12f,
    backgroundColor = Color(0x14334155)  // Slate 8% opacity (reduced from 12%)
)

/**
 * Apply glassmorphism effect for cards (tab switcher, dialogs)
 */
@Composable
fun Modifier.glassCard(
    cornerRadius: Dp = 16.dp
): Modifier = this.glassmorphism(
    cornerRadius = cornerRadius,
    borderColor = Color(0x262563EB),  // Ocean blue 15% opacity
    borderWidth = 1.5.dp,
    blurRadius = 14f,
    backgroundColor = Color(0x1A334155)  // Slate 10% opacity (reduced from 20%)
)

/**
 * Apply glassmorphism effect for buttons
 */
@Composable
fun Modifier.glassButton(
    isActive: Boolean = false
): Modifier = this.glassmorphism(
    cornerRadius = 8.dp,
    borderColor = if (isActive) {
        Color(0x402563EB)  // Ocean blue 25% opacity for active/pressed
    } else {
        Color(0x262563EB)  // Ocean blue 15% opacity for normal
    },
    borderWidth = 1.dp,
    blurRadius = 10f,
    backgroundColor = if (isActive) {
        Color(0x1A334155)  // Slate 10% opacity for active (reduced from 20%)
    } else {
        Color(0x14334155)  // Slate 8% opacity for normal (reduced from 12%)
    }
)

/**
 * Glassmorphism colors for consistent theming (Blue-tinted)
 */
object GlassColors {
    val surface = Color(0x143B82F6)        // Blue 8% opacity - default glass surface
    val surfaceActive = Color(0x1F3B82F6)  // Blue 12% opacity - active/hover state
    val border = Color(0x263B82F6)         // Blue 15% opacity - default border
    val borderActive = Color(0x333B82F6)   // Blue 20% opacity - active border
    val borderSubtle = Color(0x1A3B82F6)   // Blue 10% opacity - subtle border

    // Tab group indicator colors (Chrome-like)
    val groupBlue = Color(0xFF3B82F6)      // Blue
    val groupGreen = Color(0xFF10B981)     // Green
    val groupYellow = Color(0xFFF59E0B)    // Amber
    val groupRed = Color(0xFFEF4444)       // Red
    val groupPurple = Color(0xFF8B5CF6)    // Purple (only for tab groups, not UI)
    val groupPink = Color(0xFFEC4899)      // Pink
    val groupCyan = Color(0xFF06B6D4)      // Cyan
    val groupOrange = Color(0xFFF97316)    // Orange
}
