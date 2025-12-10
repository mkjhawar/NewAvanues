package com.augmentalis.cockpit.mvp

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ocean Theme tokens for MagicUI glassmorphic styling
 * Pre-MagicUI wrapper implementation
 *
 * Complete design token system for consistent UI styling
 * Migration-ready: When MagicUI ships, replace imports only
 */
object OceanTheme {
    // ========================================
    // Glassmorphic Surface Tokens
    // ========================================
    val glassSurface = Color.White.copy(alpha = 0.08f)
    val glassSurfaceHover = Color.White.copy(alpha = 0.12f)
    val glassSurfacePressed = Color.White.copy(alpha = 0.16f)
    val glassBorder = Color.White.copy(alpha = 0.15f)
    val glassBorderFocus = Color.White.copy(alpha = 0.30f)
    val glassBlur = 20.dp
    val glassShadow = Color.Black.copy(alpha = 0.25f)

    // ========================================
    // Color Palette
    // ========================================
    val primary = Color(0xFF60A5FA)
    val primaryDark = Color(0xFF3B82F6)
    val success = Color(0xFF34D399)
    val successDark = Color(0xFF10B981)
    val error = Color(0xFFF87171)
    val errorDark = Color(0xFFEF4444)
    val warning = Color(0xFFFBBF24)
    val warningDark = Color(0xFFF59E0B)
    val info = Color(0xFF60A5FA)
    val infoDark = Color(0xFF3B82F6)

    // Text colors
    val textPrimary = Color.White.copy(alpha = 0.95f)
    val textSecondary = Color.White.copy(alpha = 0.80f)
    val textTertiary = Color.White.copy(alpha = 0.60f)
    val textDisabled = Color.White.copy(alpha = 0.40f)

    // Background gradient
    val backgroundStart = Color(0xFF1A1A2E)
    val backgroundEnd = Color(0xFF16213E)
    val backgroundOverlay = Color.Black.copy(alpha = 0.30f)

    // ========================================
    // Shapes
    // ========================================
    val glassShapeDefault: Shape = RoundedCornerShape(16.dp)
    val glassShapeSmall: Shape = RoundedCornerShape(12.dp)
    val glassShapeLarge: Shape = RoundedCornerShape(20.dp)
    val glassShapeButton: Shape = RoundedCornerShape(12.dp)
    val glassShapeCard: Shape = RoundedCornerShape(16.dp)
    val glassShapeDialog: Shape = RoundedCornerShape(24.dp)

    // ========================================
    // Elevations
    // ========================================
    val elevationDefault: Dp = 4.dp
    val elevationHigh: Dp = 6.dp
    val elevationLow: Dp = 2.dp
    val shadowElevationDefault: Dp = 8.dp
    val shadowElevationHigh: Dp = 12.dp
    val shadowElevationLow: Dp = 4.dp

    // ========================================
    // Sizes - Windows
    // ========================================
    val windowWidthDefault: Dp = 240.dp
    val windowHeightDefault: Dp = 180.dp
    val windowWidthLarge: Dp = 320.dp
    val windowHeightLarge: Dp = 240.dp
    val windowWidthSmall: Dp = 180.dp
    val windowHeightSmall: Dp = 135.dp

    // ========================================
    // Sizes - Buttons & Icons
    // ========================================
    val buttonSizeDefault: Dp = 40.dp
    val buttonSizeSmall: Dp = 32.dp
    val buttonSizeLarge: Dp = 48.dp
    val iconSizeDefault: Dp = 24.dp
    val iconSizeSmall: Dp = 18.dp
    val iconSizeLarge: Dp = 32.dp

    // ========================================
    // Spacing
    // ========================================
    val spacingXSmall: Dp = 4.dp
    val spacingSmall: Dp = 8.dp
    val spacingMedium: Dp = 12.dp
    val spacingDefault: Dp = 16.dp
    val spacingLarge: Dp = 24.dp
    val spacingXLarge: Dp = 32.dp

    // ========================================
    // Border Widths
    // ========================================
    val borderWidthDefault: Dp = 1.dp
    val borderWidthThick: Dp = 2.dp
    val borderWidthFocus: Dp = 2.dp

    // ========================================
    // Animation Durations (ms)
    // ========================================
    const val animationDurationFast = 150
    const val animationDurationDefault = 250
    const val animationDurationSlow = 350
}
