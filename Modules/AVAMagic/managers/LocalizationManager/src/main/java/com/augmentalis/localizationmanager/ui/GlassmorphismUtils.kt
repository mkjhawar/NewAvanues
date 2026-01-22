/**
 * GlassmorphismUtils.kt - LocalizationManager-specific glassmorphism colors and configs
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Updated: 2026-01-19 (Refactored to use core GlassmorphismCore)
 *
 * Module-specific color palettes and glass configurations for LocalizationManager.
 * Core glassmorphism functionality is provided by AvaUI/Foundation.
 */
package com.augmentalis.localizationmanager.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
// Core glassmorphism - import from unified location
import com.augmentalis.avamagic.ui.foundation.GlassMorphismConfig
import com.augmentalis.avamagic.ui.foundation.DepthLevel
import com.augmentalis.avamagic.ui.foundation.glassMorphism

// Re-export core types for convenience (maintains backward compatibility)
// Consumers can use: import com.augmentalis.localizationmanager.ui.*

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
