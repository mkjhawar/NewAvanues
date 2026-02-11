/**
 * HydraColors.kt - Hydra palette color scheme (Theme v5.1)
 *
 * Royal translucent sapphire aesthetic — the flagship default palette.
 * Deep, rich sapphire blue (NOT cyan). Evolved from LiquidColors.
 * Dark + Light + XR variants for AppearanceMode and DisplayProfile axes.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object HydraColors : AvanueColorScheme {
    // Primary - Royal Sapphire
    override val primary = Color(0xFF1E40AF)
    override val onPrimary = Color.White
    override val primaryDark = Color(0xFF1E3A8A)
    override val primaryLight = Color(0xFF3B82F6)

    // Secondary - Amethyst
    override val secondary = Color(0xFF8B5CF6)
    override val onSecondary = Color.White

    // Tertiary - Emerald Accent
    override val tertiary = Color(0xFF34D399)

    // Containers
    override val primaryContainer = Color(0xFF0C1A3D)    // Deep sapphire
    override val onPrimaryContainer = Color(0xFFBFDBFE)  // Light sapphire text
    override val secondaryContainer = Color(0xFF2E1065)  // Deep amethyst
    override val onSecondaryContainer = Color(0xFFDDD6FE) // Light amethyst text
    override val tertiaryContainer = Color(0xFF0D3326)   // Deep emerald
    override val onTertiaryContainer = Color(0xFF99F6CC) // Light emerald text
    override val errorContainer = Color(0xFF3C1111)      // Deep red
    override val onErrorContainer = Color(0xFFFFB3AD)    // Light red text

    // Error
    override val error = Color(0xFFEF4444)
    override val onError = Color.White

    // Background & Surface - Sapphire Black
    override val background = Color(0xFF020617)
    override val surface = Color(0xFF0F172A)
    override val surfaceElevated = Color(0xFF1E293B)
    override val surfaceVariant = Color(0xFF334155)
    override val surfaceInput = Color(0xFF1E293B)

    // Text
    override val textPrimary = Color(0xFFF1F5F9)       // Frost White
    override val textSecondary = Color(0xFF94A3B8)      // Silver
    override val textTertiary = Color(0xFF64748B)       // Slate
    override val textDisabled = Color(0xFF475569)       // Dim Slate
    override val textOnPrimary = Color.White

    // Icon
    override val iconPrimary = Color(0xFF1E40AF)
    override val iconSecondary = Color(0xFF94A3B8)
    override val iconDisabled = Color(0xFF475569)

    // Border
    override val border = Color(0x1FFFFFFF)             // White @ 12%
    override val borderSubtle = Color(0x0FFFFFFF)       // White @ 6%
    override val borderStrong = Color(0x40FFFFFF)        // White @ 25%

    // Semantic
    override val success = Color(0xFF22C55E)
    override val warning = Color(0xFFF59E0B)
    override val info = Color(0xFF3B82F6)

    // Special
    override val starActive = Color(0xFFFFC107)
}

/**
 * Hydra Light — frost white background, sapphire accents.
 * Same brand identity, inverted for light appearance.
 */
object HydraColorsLight : AvanueColorScheme {
    // Primary - Royal Sapphire (same brand)
    override val primary = Color(0xFF1E40AF)
    override val onPrimary = Color.White
    override val primaryDark = Color(0xFF1E3A8A)
    override val primaryLight = Color(0xFF3B82F6)

    // Secondary - Amethyst
    override val secondary = Color(0xFF7C3AED)
    override val onSecondary = Color.White

    // Tertiary - Emerald Accent
    override val tertiary = Color(0xFF059669)

    // Containers - Light tinted backgrounds
    override val primaryContainer = Color(0xFFDBEAFE)    // Sapphire 100
    override val onPrimaryContainer = Color(0xFF1E3A8A)  // Dark sapphire text
    override val secondaryContainer = Color(0xFFEDE9FE)  // Amethyst 100
    override val onSecondaryContainer = Color(0xFF4C1D95) // Dark amethyst text
    override val tertiaryContainer = Color(0xFFD1FAE5)   // Emerald 100
    override val onTertiaryContainer = Color(0xFF064E3B) // Dark emerald text
    override val errorContainer = Color(0xFFFEE2E2)      // Red 100
    override val onErrorContainer = Color(0xFF991B1B)    // Dark red text

    // Error
    override val error = Color(0xFFDC2626)
    override val onError = Color.White

    // Background & Surface - Frost White
    override val background = Color(0xFFF8FAFC)
    override val surface = Color(0xFFFFFFFF)
    override val surfaceElevated = Color(0xFFF1F5F9)
    override val surfaceVariant = Color(0xFFE2E8F0)
    override val surfaceInput = Color(0xFFF1F5F9)

    // Text - Dark on Light
    override val textPrimary = Color(0xFF0F172A)
    override val textSecondary = Color(0xFF475569)
    override val textTertiary = Color(0xFF94A3B8)
    override val textDisabled = Color(0xFFCBD5E1)
    override val textOnPrimary = Color.White

    // Icon
    override val iconPrimary = Color(0xFF1E40AF)
    override val iconSecondary = Color(0xFF64748B)
    override val iconDisabled = Color(0xFFCBD5E1)

    // Border - Black alphas
    override val border = Color(0x1F000000)             // Black @ 12%
    override val borderSubtle = Color(0x0F000000)       // Black @ 6%
    override val borderStrong = Color(0x40000000)        // Black @ 25%

    // Semantic
    override val success = Color(0xFF16A34A)
    override val warning = Color(0xFFD97706)
    override val info = Color(0xFF2563EB)

    // Special
    override val starActive = Color(0xFFF59E0B)
}

/**
 * Hydra XR — additive display variant for AR smart glasses.
 * Black = transparent. All surfaces are semi-transparent dark.
 * Primaries are boosted to high luminance for additive visibility.
 * Text is near-white for maximum legibility over real-world scenes.
 */
object HydraColorsXR : AvanueColorScheme {
    // Primary - Bright Sapphire (boosted luminance for additive)
    override val primary = Color(0xFF60A5FA)            // blue-400
    override val onPrimary = Color(0xFF0F172A)
    override val primaryDark = Color(0xFF3B82F6)
    override val primaryLight = Color(0xFF93C5FD)

    // Secondary - Bright Amethyst
    override val secondary = Color(0xFFA78BFA)          // violet-400
    override val onSecondary = Color(0xFF1E1B4B)

    // Tertiary - Bright Emerald
    override val tertiary = Color(0xFF34D399)           // emerald-400

    // Containers - Semi-transparent dark tints
    override val primaryContainer = Color(0x401E40AF)   // Sapphire @ 25%
    override val onPrimaryContainer = Color(0xFFBFDBFE)
    override val secondaryContainer = Color(0x407C3AED) // Amethyst @ 25%
    override val onSecondaryContainer = Color(0xFFDDD6FE)
    override val tertiaryContainer = Color(0x40059669)
    override val onTertiaryContainer = Color(0xFF99F6CC)
    override val errorContainer = Color(0x40DC2626)
    override val onErrorContainer = Color(0xFFFFB3AD)

    // Error
    override val error = Color(0xFFF87171)              // red-400 (bright)
    override val onError = Color.White

    // Background & Surface - Transparent/near-transparent
    override val background = Color(0x00000000)         // Fully transparent
    override val surface = Color(0x260F172A)            // Sapphire @ 15%
    override val surfaceElevated = Color(0x401E293B)    // @ 25%
    override val surfaceVariant = Color(0x33334155)     // @ 20%
    override val surfaceInput = Color(0x401E293B)

    // Text - Near-white for maximum legibility
    override val textPrimary = Color(0xFFF8FAFC)
    override val textSecondary = Color(0xFFCBD5E1)
    override val textTertiary = Color(0xFF94A3B8)
    override val textDisabled = Color(0xFF64748B)
    override val textOnPrimary = Color(0xFF0F172A)

    // Icon - Bright
    override val iconPrimary = Color(0xFF60A5FA)
    override val iconSecondary = Color(0xFFCBD5E1)
    override val iconDisabled = Color(0xFF64748B)

    // Border - White alphas (bright on additive)
    override val border = Color(0x33FFFFFF)
    override val borderSubtle = Color(0x1AFFFFFF)
    override val borderStrong = Color(0x66FFFFFF)

    // Semantic - Bright variants
    override val success = Color(0xFF4ADE80)            // green-400
    override val warning = Color(0xFFFBBF24)            // amber-400
    override val info = Color(0xFF60A5FA)               // blue-400

    // Special
    override val starActive = Color(0xFFFCD34D)
}
