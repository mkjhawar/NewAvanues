/**
 * LunaColors.kt - Luna palette color scheme (Theme v5.1)
 *
 * Cool moonlit silver aesthetic: indigo primary, violet secondary.
 * Evolved from OceanColors with a cooler, lunar identity.
 * Dark + Light variants for AppearanceMode axis.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object LunaColors : AvanueColorScheme {
    // Primary - Moonlit Indigo
    override val primary = Color(0xFF818CF8)
    override val onPrimary = Color.Black
    override val primaryDark = Color(0xFF6366F1)
    override val primaryLight = Color(0xFFA5B4FC)

    // Secondary - Violet
    override val secondary = Color(0xFF7C3AED)
    override val onSecondary = Color.White

    // Tertiary - Seafoam Silver
    override val tertiary = Color(0xFF94A3B8)

    // Containers
    override val primaryContainer = Color(0xFF1E1B4B)    // Deep indigo
    override val onPrimaryContainer = Color(0xFFC7D2FE)  // Light indigo text
    override val secondaryContainer = Color(0xFF2E1065)  // Deep violet
    override val onSecondaryContainer = Color(0xFFDDD6FE) // Light violet text
    override val tertiaryContainer = Color(0xFF1E293B)   // Deep slate
    override val onTertiaryContainer = Color(0xFFCBD5E1) // Light slate text
    override val errorContainer = Color(0xFF5F1E1E)
    override val onErrorContainer = Color(0xFFFECACA)

    // Error
    override val error = Color(0xFFEF4444)
    override val onError = Color.White

    // Background & Surface - Deep Midnight
    override val background = Color(0xFF0C0F1A)
    override val surface = Color(0xFF1A1D2E)
    override val surfaceElevated = Color(0xFF282B3E)
    override val surfaceVariant = Color(0xFF363A50)
    override val surfaceInput = Color(0xFF282B3E)

    // Text
    override val textPrimary = Color(0xFFE2E8F0)
    override val textSecondary = Color(0xFFCBD5E1)
    override val textTertiary = Color(0xFF94A3B8)
    override val textDisabled = Color(0xFF64748B)
    override val textOnPrimary = Color.Black

    // Icon
    override val iconPrimary = Color(0xFF818CF8)
    override val iconSecondary = Color(0xFFCBD5E1)
    override val iconDisabled = Color(0xFF64748B)

    // Border
    override val border = Color(0x33FFFFFF)
    override val borderSubtle = Color(0x1AFFFFFF)
    override val borderStrong = Color(0x4DFFFFFF)

    // Semantic
    override val success = Color(0xFF22C55E)
    override val warning = Color(0xFFF59E0B)
    override val info = Color(0xFF818CF8)

    // Special
    override val starActive = Color(0xFFFFC107)
}

/**
 * Luna Light — lavender white background, indigo accents.
 * Same brand identity, inverted for light appearance.
 */
object LunaColorsLight : AvanueColorScheme {
    // Primary - Moonlit Indigo (same brand)
    override val primary = Color(0xFF6366F1)
    override val onPrimary = Color.White
    override val primaryDark = Color(0xFF4F46E5)
    override val primaryLight = Color(0xFF818CF8)

    // Secondary - Violet
    override val secondary = Color(0xFF7C3AED)
    override val onSecondary = Color.White

    // Tertiary - Cool Slate
    override val tertiary = Color(0xFF64748B)

    // Containers - Light tinted backgrounds
    override val primaryContainer = Color(0xFFE0E7FF)    // Indigo 100
    override val onPrimaryContainer = Color(0xFF312E81)  // Dark indigo text
    override val secondaryContainer = Color(0xFFEDE9FE)  // Violet 100
    override val onSecondaryContainer = Color(0xFF4C1D95) // Dark violet text
    override val tertiaryContainer = Color(0xFFF1F5F9)   // Slate 100
    override val onTertiaryContainer = Color(0xFF334155) // Dark slate text
    override val errorContainer = Color(0xFFFEE2E2)
    override val onErrorContainer = Color(0xFF991B1B)

    // Error
    override val error = Color(0xFFDC2626)
    override val onError = Color.White

    // Background & Surface - Lavender White
    override val background = Color(0xFFF5F3FF)
    override val surface = Color(0xFFFFFFFF)
    override val surfaceElevated = Color(0xFFEDE9FE)
    override val surfaceVariant = Color(0xFFE0E7FF)
    override val surfaceInput = Color(0xFFF5F3FF)

    // Text - Dark cool on Light
    override val textPrimary = Color(0xFF1E1B4B)
    override val textSecondary = Color(0xFF4B5563)
    override val textTertiary = Color(0xFF9CA3AF)
    override val textDisabled = Color(0xFFD1D5DB)
    override val textOnPrimary = Color.White

    // Icon
    override val iconPrimary = Color(0xFF6366F1)
    override val iconSecondary = Color(0xFF6B7280)
    override val iconDisabled = Color(0xFFD1D5DB)

    // Border - Black alphas
    override val border = Color(0x1F000000)
    override val borderSubtle = Color(0x0F000000)
    override val borderStrong = Color(0x40000000)

    // Semantic
    override val success = Color(0xFF16A34A)
    override val warning = Color(0xFFD97706)
    override val info = Color(0xFF4F46E5)

    // Special
    override val starActive = Color(0xFFF59E0B)
}
