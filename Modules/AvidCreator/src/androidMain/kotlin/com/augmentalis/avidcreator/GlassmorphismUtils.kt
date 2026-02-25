/**
 * GlassmorphismUtils.kt - AvidCreator Glass Morphism Theme
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-02
 * Refactored: 2026-02-02 (consolidated core classes to Common/UI)
 */
package com.augmentalis.avidcreator.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.glass.GlassMorphismConfig
import com.augmentalis.avanueui.components.glass.DepthLevel

/**
 * UUID Manager color palette
 */
object UUIDColors {
    // Status colors
    val StatusActive = Color(0xFF4CAF50)        // Green
    val StatusInactive = Color(0xFF9E9E9E)      // Gray
    val StatusFocused = Color(0xFF2196F3)       // Blue
    val StatusSelected = Color(0xFFFF9800)      // Orange
    val StatusError = Color(0xFFFF5252)         // Red

    // Element type colors
    val TypeButton = Color(0xFF6A4C93)          // Purple
    val TypeText = Color(0xFF00BCD4)            // Cyan
    val TypeImage = Color(0xFFE91E63)           // Pink
    val TypeContainer = Color(0xFF3F51B5)       // Indigo
    val TypeList = Color(0xFF4CAF50)            // Green
    val TypeForm = Color(0xFFFF5722)            // Deep Orange
    val TypeDialog = Color(0xFF9C27B0)          // Deep Purple
    val TypeMenu = Color(0xFF795548)            // Brown

    // Navigation colors
    val NavUp = Color(0xFF2196F3)               // Blue
    val NavDown = Color(0xFF03A9F4)             // Light Blue
    val NavLeft = Color(0xFF00BCD4)             // Cyan
    val NavRight = Color(0xFF009688)            // Teal
    val NavForward = Color(0xFF4CAF50)          // Green
    val NavBackward = Color(0xFFFF9800)         // Orange

    // UI accent colors
    val Primary = Color(0xFF6A4C93)             // Purple
    val Secondary = Color(0xFF00BCD4)           // Cyan
    val Accent = Color(0xFFE91E63)              // Pink
    val Success = Color(0xFF4CAF50)             // Green
    val Warning = Color(0xFFFF9800)             // Orange
    val Error = Color(0xFFFF5252)               // Red

    // Registry status colors
    val RegistryEmpty = Color(0xFF9E9E9E)       // Gray
    val RegistryLow = Color(0xFF4CAF50)         // Green
    val RegistryMedium = Color(0xFFFF9800)      // Orange
    val RegistryHigh = Color(0xFFFF5252)        // Red
}

/**
 * Pre-defined glass morphism configs for UUID components
 */
object UUIDGlassConfigs {
    val Primary = GlassMorphismConfig(
        tintColor = UUIDColors.Primary,
        cornerRadius = 16.dp
    )

    val Registry = GlassMorphismConfig(
        tintColor = UUIDColors.Primary,
        cornerRadius = 16.dp,
        backgroundOpacity = 0.12f
    )

    val ElementCard = GlassMorphismConfig(
        tintColor = UUIDColors.TypeContainer,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.08f
    )

    val NavigationCard = GlassMorphismConfig(
        tintColor = UUIDColors.NavForward,
        cornerRadius = 12.dp
    )

    val CommandCard = GlassMorphismConfig(
        tintColor = UUIDColors.TypeButton,
        cornerRadius = 12.dp
    )

    val StatisticsCard = GlassMorphismConfig(
        tintColor = UUIDColors.Secondary,
        cornerRadius = 16.dp
    )

    val TargetCard = GlassMorphismConfig(
        tintColor = UUIDColors.StatusFocused,
        cornerRadius = 12.dp
    )

    val HistoryCard = GlassMorphismConfig(
        tintColor = UUIDColors.TypeList,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.06f
    )

    val Warning = GlassMorphismConfig(
        tintColor = UUIDColors.Warning,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.15f
    )

    val Error = GlassMorphismConfig(
        tintColor = UUIDColors.Error,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.15f
    )
}
