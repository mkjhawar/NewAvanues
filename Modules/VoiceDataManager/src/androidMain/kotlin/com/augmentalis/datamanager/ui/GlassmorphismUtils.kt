/**
 * GlassmorphismUtils.kt - Glassmorphism UI utilities for VosDataManager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-02
 * 
 * Provides glassmorphism visual effects for data management UI
 */
package com.augmentalis.datamanager.ui

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
 * Glass morphism configuration for data cards
 */
data class GlassMorphismConfig(
    val cornerRadius: Dp = 16.dp,
    val backgroundOpacity: Float = 0.1f,
    val borderOpacity: Float = 0.2f,
    val borderWidth: Dp = 1.dp,
    val tintColor: Color = Color(0xFF00BCD4),
    val tintOpacity: Float = 0.15f,
    val blurRadius: Dp = 0.dp
)

/**
 * Depth level for layered effects
 */
@JvmInline
value class DepthLevel(val value: Float)

/**
 * Glass morphism modifier for data components
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
 * Data manager color palette
 */
object DataColors {
    // Status colors
    val StatusActive = Color(0xFF4CAF50)      // Green
    val StatusSyncing = Color(0xFF2196F3)     // Blue
    val StatusWarning = Color(0xFFFF9800)     // Orange
    val StatusError = Color(0xFFFF5252)       // Red
    val StatusOffline = Color(0xFF9E9E9E)     // Gray
    
    // Data type colors
    val TypePreferences = Color(0xFF673AB7)   // Deep Purple
    val TypeHistory = Color(0xFF2196F3)       // Blue
    val TypeCommands = Color(0xFF00BCD4)      // Cyan
    val TypeGestures = Color(0xFFFF9800)      // Orange
    val TypeStatistics = Color(0xFF4CAF50)    // Green
    val TypeProfiles = Color(0xFF9C27B0)      // Purple
    val TypeLanguages = Color(0xFF3F51B5)     // Indigo
    val TypeErrors = Color(0xFFFF5252)        // Red
    val TypeAnalytics = Color(0xFF00ACC1)     // Cyan 600
    val TypeRetention = Color(0xFF795548)     // Brown
    
    // Storage colors
    val StorageNormal = Color(0xFF4CAF50)     // Green
    val StorageMedium = Color(0xFFFFEB3B)     // Yellow
    val StorageHigh = Color(0xFFFF9800)       // Orange
    val StorageCritical = Color(0xFFFF5252)   // Red
    
    // Action colors
    val ActionExport = Color(0xFF2196F3)      // Blue
    val ActionImport = Color(0xFF4CAF50)      // Green
    val ActionCleanup = Color(0xFFFF9800)     // Orange
    val ActionSync = Color(0xFF00BCD4)        // Cyan
}

/**
 * Pre-defined glass morphism configs for data types
 */
object DataGlassConfigs {
    val Primary = GlassMorphismConfig(
        tintColor = DataColors.TypePreferences,
        cornerRadius = 16.dp
    )
    
    val Storage = GlassMorphismConfig(
        tintColor = DataColors.StorageNormal,
        cornerRadius = 16.dp
    )
    
    val Statistics = GlassMorphismConfig(
        tintColor = DataColors.TypeStatistics,
        cornerRadius = 12.dp
    )
    
    val History = GlassMorphismConfig(
        tintColor = DataColors.TypeHistory,
        cornerRadius = 12.dp
    )
    
    val Commands = GlassMorphismConfig(
        tintColor = DataColors.TypeCommands,
        cornerRadius = 12.dp
    )
    
    val Gestures = GlassMorphismConfig(
        tintColor = DataColors.TypeGestures,
        cornerRadius = 12.dp
    )
    
    val Profiles = GlassMorphismConfig(
        tintColor = DataColors.TypeProfiles,
        cornerRadius = 12.dp
    )
    
    val Errors = GlassMorphismConfig(
        tintColor = DataColors.TypeErrors,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.15f
    )
    
    val Actions = GlassMorphismConfig(
        tintColor = DataColors.ActionSync,
        cornerRadius = 16.dp
    )
}