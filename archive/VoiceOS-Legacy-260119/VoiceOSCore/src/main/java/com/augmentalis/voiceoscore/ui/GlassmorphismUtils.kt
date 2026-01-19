/**
 * GlassmorphismUtils.kt - Glassmorphism UI utilities for VoiceOSCore
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Provides glassmorphism visual effects matching VOS4 design system
 */
package com.augmentalis.voiceoscore.ui

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
 * Glass morphism configuration
 */
data class GlassMorphismConfig(
    val cornerRadius: Dp = 16.dp,
    val backgroundOpacity: Float = 0.1f,
    val borderOpacity: Float = 0.2f,
    val borderWidth: Dp = 1.dp,
    val tintColor: Color = Color(0xFF4285F4),
    val tintOpacity: Float = 0.15f,
    val blurRadius: Dp = 0.dp
)

/**
 * Depth level for glass effects
 */
@JvmInline
value class DepthLevel(val value: Float)

/**
 * Glass morphism modifier
 */
fun Modifier.glassMorphism(
    config: GlassMorphismConfig = GlassMorphismConfig(),
    depth: DepthLevel = DepthLevel(1.0f)
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
 * VoiceOS color palette
 */
object VoiceOSColors {
    // Status colors
    val StatusActive = Color(0xFF00C853)      // Green
    val StatusWarning = Color(0xFFFF9800)     // Orange
    val StatusError = Color(0xFFFF5722)       // Red
    val StatusInfo = Color(0xFF2196F3)        // Blue

    // Feature colors
    val FeatureLearning = Color(0xFF2196F3)     // Blue
    val FeatureAccessibility = Color(0xFF00BCD4) // Cyan
    val FeatureVoice = Color(0xFFE91E63)        // Pink

    // Glassmorphism tints
    val GlassSuccess = Color(0xFF4CAF50)
    val GlassWarning = Color(0xFFFF9800)
    val GlassError = Color(0xFFF44336)
    val GlassInfo = Color(0xFF2196F3)
    val GlassPrimary = Color(0xFF1976D2)
}

/**
 * Pre-defined glass morphism configs for VoiceOS
 */
object VoiceOSGlassConfigs {
    val Primary = GlassMorphismConfig(
        tintColor = VoiceOSColors.GlassPrimary,
        cornerRadius = 16.dp
    )

    val Success = GlassMorphismConfig(
        tintColor = VoiceOSColors.GlassSuccess,
        cornerRadius = 16.dp
    )

    val Warning = GlassMorphismConfig(
        tintColor = VoiceOSColors.GlassWarning,
        cornerRadius = 16.dp
    )

    val Error = GlassMorphismConfig(
        tintColor = VoiceOSColors.GlassError,
        cornerRadius = 16.dp
    )

    val Info = GlassMorphismConfig(
        tintColor = VoiceOSColors.GlassInfo,
        cornerRadius = 16.dp
    )

    val Learning = GlassMorphismConfig(
        tintColor = VoiceOSColors.FeatureLearning,
        cornerRadius = 12.dp
    )

    val Accessibility = GlassMorphismConfig(
        tintColor = VoiceOSColors.FeatureAccessibility,
        cornerRadius = 12.dp
    )

    val Voice = GlassMorphismConfig(
        tintColor = VoiceOSColors.FeatureVoice,
        cornerRadius = 12.dp
    )
}
