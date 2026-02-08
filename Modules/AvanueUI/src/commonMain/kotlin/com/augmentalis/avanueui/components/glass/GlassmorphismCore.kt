/**
 * GlassmorphismCore.kt - Core glassmorphism UI utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Consolidated: 2026-01-19
 *
 * Provides core glassmorphism visual effects for the VOS4 design system.
 * This is the unified implementation - module-specific colors and configs
 * should be defined in their respective modules.
 *
 * Usage:
 * ```kotlin
 * import com.augmentalis.avanueui.components.glass.GlassMorphismConfig
 * import com.augmentalis.avanueui.components.glass.glassMorphism
 *
 * Box(modifier = Modifier.glassMorphism(config = myConfig))
 * ```
 */
package com.augmentalis.avanueui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration for glassmorphism visual effects.
 *
 * @param cornerRadius Corner radius for the glass shape
 * @param backgroundOpacity Base opacity for the background gradient
 * @param borderOpacity Opacity for the border gradient
 * @param borderWidth Width of the glass border
 * @param tintColor Color tint applied to the glass effect
 * @param tintOpacity Opacity of the tint color
 * @param blurRadius Optional blur radius (0.dp for no blur)
 */
data class GlassMorphismConfig(
    val cornerRadius: Dp = 16.dp,
    val backgroundOpacity: Float = 0.1f,
    val borderOpacity: Float = 0.2f,
    val borderWidth: Dp = 1.dp,
    val tintColor: Color = Color(0xFF2196F3), // Material Blue as neutral default
    val tintOpacity: Float = 0.15f,
    val blurRadius: Dp = 0.dp
)

/**
 * Depth level for layered glass effects.
 *
 * Higher values create more prominent effects.
 * Default is 1.0f (standard effect).
 *
 * @param value Depth multiplier (1.0f = normal, 0.5f = subtle, 2.0f = intense)
 */
@JvmInline
value class DepthLevel(val value: Float) {
    companion object {
        val Subtle = DepthLevel(0.5f)
        val Normal = DepthLevel(1.0f)
        val Prominent = DepthLevel(1.5f)
        val Intense = DepthLevel(2.0f)
    }
}

/**
 * Applies glassmorphism visual effect to a Composable.
 *
 * Creates a glass-like appearance with:
 * - Semi-transparent gradient background
 * - Subtle border with gradient
 * - Optional blur effect
 * - Configurable tint color and depth
 *
 * @param config Configuration for the glass effect
 * @param depth Depth level that scales all opacity values
 * @return Modified Modifier with glass effect applied
 */
fun Modifier.glassMorphism(
    config: GlassMorphismConfig = GlassMorphismConfig(),
    depth: DepthLevel = DepthLevel.Normal
): Modifier {
    val adjustedConfig = config.copy(
        backgroundOpacity = config.backgroundOpacity * depth.value,
        borderOpacity = config.borderOpacity * depth.value,
        tintOpacity = config.tintOpacity * depth.value
    )

    return this
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = adjustedConfig.backgroundOpacity),
                    adjustedConfig.tintColor.copy(alpha = adjustedConfig.tintOpacity),
                    Color.White.copy(alpha = adjustedConfig.backgroundOpacity * 0.5f)
                )
            ),
            shape = RoundedCornerShape(adjustedConfig.cornerRadius)
        )
        .border(
            width = adjustedConfig.borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = adjustedConfig.borderOpacity),
                    Color.Transparent,
                    Color.White.copy(alpha = adjustedConfig.borderOpacity * 0.5f)
                )
            ),
            shape = RoundedCornerShape(adjustedConfig.cornerRadius)
        )
        .let { modifier ->
            if (adjustedConfig.blurRadius > 0.dp) {
                modifier.blur(adjustedConfig.blurRadius)
            } else {
                modifier
            }
        }
}

/**
 * Pre-defined glassmorphism configurations for common use cases.
 *
 * For module-specific configs, create your own object in your module
 * that uses these as base configurations.
 */
object GlassPresets {
    /** Primary blue glass effect */
    val Primary = GlassMorphismConfig(
        tintColor = Color(0xFF1976D2),
        cornerRadius = 16.dp
    )

    /** Success green glass effect */
    val Success = GlassMorphismConfig(
        tintColor = Color(0xFF4CAF50),
        cornerRadius = 16.dp
    )

    /** Warning orange glass effect */
    val Warning = GlassMorphismConfig(
        tintColor = Color(0xFFFF9800),
        cornerRadius = 16.dp
    )

    /** Error red glass effect */
    val Error = GlassMorphismConfig(
        tintColor = Color(0xFFF44336),
        cornerRadius = 16.dp
    )

    /** Info blue glass effect */
    val Info = GlassMorphismConfig(
        tintColor = Color(0xFF2196F3),
        cornerRadius = 16.dp
    )

    /** Subtle card with small corners */
    val Card = GlassMorphismConfig(
        cornerRadius = 12.dp,
        backgroundOpacity = 0.08f
    )

    /** Prominent elevated effect */
    val Elevated = GlassMorphismConfig(
        cornerRadius = 16.dp,
        backgroundOpacity = 0.15f,
        borderOpacity = 0.25f
    )
}
