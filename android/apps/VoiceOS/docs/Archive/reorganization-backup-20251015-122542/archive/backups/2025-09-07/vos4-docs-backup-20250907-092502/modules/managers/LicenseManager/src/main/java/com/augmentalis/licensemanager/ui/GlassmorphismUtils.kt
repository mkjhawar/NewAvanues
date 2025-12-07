/**
 * GlassmorphismUtils.kt - Glassmorphism UI utilities for LicenseManager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Provides glassmorphism visual effects matching VoiceAccessibility design
 */
package com.augmentalis.licensemanager.ui

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
 * License manager color palette
 */
object LicenseColors {
    // Status colors
    val StatusActive = Color(0xFF00C853)      // Green
    val StatusWarning = Color(0xFFFF9800)     // Orange  
    val StatusError = Color(0xFFFF5722)       // Red
    val StatusInfo = Color(0xFF2196F3)        // Blue
    
    // License type colors
    val LicenseFree = Color(0xFF9E9E9E)       // Gray
    val LicenseTrial = Color(0xFFFF9800)      // Orange
    val LicensePremium = Color(0xFF673AB7)    // Purple
    val LicenseEnterprise = Color(0xFF1976D2) // Blue
    
    // Glassmorphism tints
    val GlassSuccess = Color(0xFF4CAF50)
    val GlassWarning = Color(0xFFFF9800)
    val GlassError = Color(0xFFF44336)
    val GlassInfo = Color(0xFF2196F3)
    val GlassPrimary = Color(0xFF6200EA)
}

/**
 * Pre-defined glass morphism configs
 */
object LicenseGlassConfigs {
    val Primary = GlassMorphismConfig(
        tintColor = LicenseColors.GlassPrimary,
        cornerRadius = 16.dp
    )
    
    val Success = GlassMorphismConfig(
        tintColor = LicenseColors.GlassSuccess,
        cornerRadius = 16.dp
    )
    
    val Warning = GlassMorphismConfig(
        tintColor = LicenseColors.GlassWarning,
        cornerRadius = 16.dp
    )
    
    val Error = GlassMorphismConfig(
        tintColor = LicenseColors.GlassError,
        cornerRadius = 16.dp
    )
    
    val Info = GlassMorphismConfig(
        tintColor = LicenseColors.GlassInfo,
        cornerRadius = 16.dp
    )
}