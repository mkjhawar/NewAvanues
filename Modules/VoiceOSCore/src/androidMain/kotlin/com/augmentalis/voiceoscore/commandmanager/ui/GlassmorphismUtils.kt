/**
 * GlassmorphismUtils.kt - CommandManager Glass Morphism Theme
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-02
 * Refactored: 2026-02-02 (consolidated core classes to Common/UI)
 */
package com.augmentalis.voiceoscore.commandmanager.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.datamanager.ui.GlassMorphismConfig
import com.augmentalis.datamanager.ui.DepthLevel

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
