package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Sunset Warm theme color implementation.
 */
object SunsetColors : AvanueColorScheme {
    // Primary - SunsetCoral
    override val primary = Color(0xFFFF6B35)
    override val onPrimary = Color.White
    override val primaryDark = Color(0xFFE55A2B)
    override val primaryLight = Color(0xFFFF8F66)

    // Secondary - WarmGold
    override val secondary = Color(0xFFFBBF24)
    override val onSecondary = Color.Black

    // Tertiary - RosePink
    override val tertiary = Color(0xFFF472B6)

    // Error
    override val error = Color(0xFFEF4444)
    override val onError = Color.White

    // Background & Surface - Deep warm
    override val background = Color(0xFF1A0E1F)
    override val surface = Color(0xFF2D1B33)
    override val surfaceElevated = Color(0xFF3D2947)
    override val surfaceVariant = Color(0xFF4D3758)
    override val surfaceInput = Color(0xFF3D2947)

    // Text
    override val textPrimary = Color(0xFFF5E6D3)
    override val textSecondary = Color(0xFFD4C0B0)
    override val textTertiary = Color(0xFFA08878)
    override val textDisabled = Color(0xFF6B5B52)
    override val textOnPrimary = Color.White

    // Icon
    override val iconPrimary = Color(0xFFFF6B35)
    override val iconSecondary = Color(0xFFD4C0B0)
    override val iconDisabled = Color(0xFF6B5B52)

    // Border
    override val border = Color(0x33FFFFFF)
    override val borderSubtle = Color(0x1AFFFFFF)
    override val borderStrong = Color(0x4DFFFFFF)

    // Semantic
    override val success = Color(0xFF10B981)
    override val warning = Color(0xFFFBBF24)
    override val info = Color(0xFFF472B6)

    // Special
    override val starActive = Color(0xFFFFC107)
}
