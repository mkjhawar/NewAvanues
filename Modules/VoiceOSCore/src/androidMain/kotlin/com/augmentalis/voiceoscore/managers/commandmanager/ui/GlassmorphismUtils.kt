/**
 * GlassmorphismUtils.kt - Glassmorphism UI utilities for CommandManager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Provides glassmorphism visual effects matching VOS4 design system
 */
package com.augmentalis.voiceoscore.managers.commandmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
 * Command manager color palette
 */
object CommandColors {
    // Status colors
    val StatusActive = Color(0xFF00C853)      // Green
    val StatusWarning = Color(0xFFFF9800)     // Orange  
    val StatusError = Color(0xFFFF5722)       // Red
    val StatusInfo = Color(0xFF2196F3)        // Blue
    
    // Category colors
    val CategoryNavigation = Color(0xFF2196F3)    // Blue
    val CategoryText = Color(0xFF4CAF50)          // Green
    val CategoryMedia = Color(0xFFFF9800)         // Orange
    val CategorySystem = Color(0xFF9C27B0)        // Purple
    val CategoryApp = Color(0xFF673AB7)           // Deep Purple
    val CategoryAccessibility = Color(0xFF00BCD4) // Cyan
    val CategoryVoice = Color(0xFFE91E63)         // Pink
    val CategoryGesture = Color(0xFF795548)       // Brown
    val CategoryCustom = Color(0xFF607D8B)        // Blue Gray
    
    // Glassmorphism tints
    val GlassSuccess = Color(0xFF4CAF50)
    val GlassWarning = Color(0xFFFF9800)
    val GlassError = Color(0xFFF44336)
    val GlassInfo = Color(0xFF2196F3)
    val GlassPrimary = Color(0xFF1976D2)
}

/**
 * Pre-defined glass morphism configs for different command categories
 */
object CommandGlassConfigs {
    val Primary = GlassMorphismConfig(
        tintColor = CommandColors.GlassPrimary,
        cornerRadius = 16.dp
    )
    
    val Success = GlassMorphismConfig(
        tintColor = CommandColors.GlassSuccess,
        cornerRadius = 16.dp
    )
    
    val Warning = GlassMorphismConfig(
        tintColor = CommandColors.GlassWarning,
        cornerRadius = 16.dp
    )
    
    val Error = GlassMorphismConfig(
        tintColor = CommandColors.GlassError,
        cornerRadius = 16.dp
    )
    
    val Info = GlassMorphismConfig(
        tintColor = CommandColors.GlassInfo,
        cornerRadius = 16.dp
    )
    
    val Navigation = GlassMorphismConfig(
        tintColor = CommandColors.CategoryNavigation,
        cornerRadius = 12.dp
    )
    
    val Text = GlassMorphismConfig(
        tintColor = CommandColors.CategoryText,
        cornerRadius = 12.dp
    )
    
    val Media = GlassMorphismConfig(
        tintColor = CommandColors.CategoryMedia,
        cornerRadius = 12.dp
    )
    
    val System = GlassMorphismConfig(
        tintColor = CommandColors.CategorySystem,
        cornerRadius = 12.dp
    )
}