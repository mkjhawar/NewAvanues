/**
 * GlassmorphismUtils.kt - LicenseManager-specific glassmorphism colors and configs
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Updated: 2026-01-19 (Refactored to use core GlassmorphismCore)
 *
 * Module-specific color palettes and glass configurations for LicenseManager.
 * Core glassmorphism functionality is provided by AvaUI/Foundation.
 */
package com.augmentalis.licensemanager.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
// Core glassmorphism - import from unified location
import com.augmentalis.avamagic.ui.foundation.GlassMorphismConfig
import com.augmentalis.avamagic.ui.foundation.DepthLevel
import com.augmentalis.avamagic.ui.foundation.glassMorphism

// Re-export core types for convenience (maintains backward compatibility)
// Consumers can use: import com.augmentalis.licensemanager.ui.*

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
