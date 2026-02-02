/**
 * GlassmorphismUtils.kt - Glassmorphism UI utilities for LocalizationManager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Provides glassmorphism visual effects for localization UI
 */
package com.augmentalis.voiceoscore.managers.localizationmanager.ui

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
 * Glass morphism configuration for localization cards
 */
data class GlassMorphismConfig(
    val cornerRadius: Dp = 16.dp,
    val backgroundOpacity: Float = 0.1f,
    val borderOpacity: Float = 0.2f,
    val borderWidth: Dp = 1.dp,
    val tintColor: Color = Color(0xFF3F51B5),
    val tintOpacity: Float = 0.15f,
    val blurRadius: Dp = 0.dp
)

/**
 * Depth level for layered effects
 */
@JvmInline
value class DepthLevel(val value: Float)

/**
 * Glass morphism modifier for localization components
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
 * Localization manager color palette
 */
object LocalizationColors {
    // Status colors
    val StatusActive = Color(0xFF4CAF50)        // Green
    val StatusInactive = Color(0xFF9E9E9E)      // Gray
    val StatusDownloading = Color(0xFF2196F3)   // Blue
    val StatusError = Color(0xFFFF5252)         // Red
    val StatusWarning = Color(0xFFFF9800)       // Orange
    
    // Language region colors
    val RegionEurope = Color(0xFF3F51B5)        // Indigo
    val RegionAsia = Color(0xFFE91E63)          // Pink
    val RegionAmericas = Color(0xFF4CAF50)      // Green
    val RegionMiddleEast = Color(0xFFFF9800)    // Orange
    val RegionAfrica = Color(0xFF795548)        // Brown
    val RegionOceania = Color(0xFF00BCD4)       // Cyan
    
    // Feature colors
    val FeatureVosk = Color(0xFF2196F3)         // Blue
    val FeatureVivoka = Color(0xFF9C27B0)       // Purple
    val FeatureTranslation = Color(0xFF00BCD4)  // Cyan
    val FeatureDictation = Color(0xFF4CAF50)    // Green
    val FeatureCommand = Color(0xFFFF5722)      // Deep Orange
    
    // UI accent colors
    val Primary = Color(0xFF3F51B5)             // Indigo
    val Secondary = Color(0xFF00BCD4)           // Cyan
    val Accent = Color(0xFFE91E63)              // Pink
    val Success = Color(0xFF4CAF50)             // Green
    val Warning = Color(0xFFFF9800)             // Orange
    val Error = Color(0xFFFF5252)               // Red
    
    // Download status colors
    val DownloadPending = Color(0xFF9E9E9E)     // Gray
    val DownloadInProgress = Color(0xFF2196F3)  // Blue
    val DownloadComplete = Color(0xFF4CAF50)    // Green
    val DownloadFailed = Color(0xFFFF5252)      // Red
}

/**
 * Pre-defined glass morphism configs for localization components
 */
object LocalizationGlassConfigs {
    val Primary = GlassMorphismConfig(
        tintColor = LocalizationColors.Primary,
        cornerRadius = 16.dp
    )
    
    val CurrentLanguage = GlassMorphismConfig(
        tintColor = LocalizationColors.StatusActive,
        cornerRadius = 16.dp,
        backgroundOpacity = 0.15f
    )
    
    val LanguageCard = GlassMorphismConfig(
        tintColor = LocalizationColors.Primary,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.08f
    )
    
    val RegionCard = GlassMorphismConfig(
        tintColor = LocalizationColors.RegionEurope,
        cornerRadius = 12.dp
    )
    
    val FeatureCard = GlassMorphismConfig(
        tintColor = LocalizationColors.FeatureVosk,
        cornerRadius = 12.dp
    )
    
    val TranslationCard = GlassMorphismConfig(
        tintColor = LocalizationColors.FeatureTranslation,
        cornerRadius = 16.dp
    )
    
    val DownloadCard = GlassMorphismConfig(
        tintColor = LocalizationColors.DownloadInProgress,
        cornerRadius = 12.dp
    )
    
    val SettingsCard = GlassMorphismConfig(
        tintColor = LocalizationColors.Secondary,
        cornerRadius = 16.dp
    )
    
    val Warning = GlassMorphismConfig(
        tintColor = LocalizationColors.Warning,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.15f
    )
    
    val Error = GlassMorphismConfig(
        tintColor = LocalizationColors.Error,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.15f
    )
}