/**
 * GlassmorphismUtils.kt - VoiceDataManager Glass Morphism Theme
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Refactored: 2026-02-02 (consolidated core classes to Common/UI)
 */
package com.augmentalis.voiceoscore.managers.voicedatamanager.ui

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
import com.avanues.ui.GlassMorphismConfig

// Re-export core classes for backward compatibility
typealias GlassMorphismConfig = com.avanues.ui.GlassMorphismConfig
typealias DepthLevel = com.avanues.ui.DepthLevel

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