package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Ocean Blue theme color implementation.
 * Values preserved from Foundation/OceanTheme.kt.
 */
object OceanColors : AvanueColorScheme {
    // Primary - CoralBlue
    override val primary = Color(0xFF3B82F6)
    override val onPrimary = Color.White
    override val primaryDark = Color(0xFF2563EB)
    override val primaryLight = Color(0xFF60A5FA)

    // Secondary - TurquoiseCyan
    override val secondary = Color(0xFF06B6D4)
    override val onSecondary = Color.Black

    // Tertiary - SeafoamGreen
    override val tertiary = Color(0xFF10B981)

    // Error - CoralRed
    override val error = Color(0xFFEF4444)
    override val onError = Color.White

    // Background & Surface
    override val background = Color(0xFF0F172A)
    override val surface = Color(0xFF1E293B)
    override val surfaceElevated = Color(0xFF334155)
    override val surfaceVariant = Color(0xFF475569)
    override val surfaceInput = Color(0xFF334155)

    // Text
    override val textPrimary = Color(0xFFE2E8F0)
    override val textSecondary = Color(0xFFCBD5E1)
    override val textTertiary = Color(0xFF94A3B8)
    override val textDisabled = Color(0xFF64748B)
    override val textOnPrimary = Color.White

    // Icon
    override val iconPrimary = Color(0xFF3B82F6)
    override val iconSecondary = Color(0xFFCBD5E1)
    override val iconDisabled = Color(0xFF64748B)

    // Border
    override val border = Color(0x33FFFFFF)
    override val borderSubtle = Color(0x1AFFFFFF)
    override val borderStrong = Color(0x4DFFFFFF)

    // Semantic
    override val success = Color(0xFF10B981)
    override val warning = Color(0xFFF59E0B)
    override val info = Color(0xFF0EA5E9)

    // Special
    override val starActive = Color(0xFFFFC107)
}
