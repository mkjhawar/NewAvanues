package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Liquid Glass theme color implementation.
 *
 * @deprecated Use [HydraColors] instead. Liquid maps to Hydra palette in Theme v5.0.
 */
@Deprecated("Use HydraColors instead", ReplaceWith("HydraColors"))
object LiquidColors : AvanueColorScheme {
    // Primary - Cyan Electric (liquid shimmer)
    override val primary = Color(0xFF00D4FF)
    override val onPrimary = Color.Black
    override val primaryDark = Color(0xFF00A5CC)
    override val primaryLight = Color(0xFF66E5FF)

    // Secondary - Soft Violet
    override val secondary = Color(0xFFA78BFA)
    override val onSecondary = Color.Black

    // Tertiary - Emerald
    override val tertiary = Color(0xFF34D399)

    // Containers
    override val primaryContainer = Color(0xFF003340)
    override val onPrimaryContainer = Color(0xFF99EEFF)
    override val secondaryContainer = Color(0xFF2E2150)
    override val onSecondaryContainer = Color(0xFFD4BBFF)
    override val tertiaryContainer = Color(0xFF0D3326)
    override val onTertiaryContainer = Color(0xFF99F6CC)
    override val errorContainer = Color(0xFF3C1111)
    override val onErrorContainer = Color(0xFFFFB3AD)

    // Error - Apple System Red
    override val error = Color(0xFFFF453A)
    override val onError = Color.White

    // Background & Surface - True black + Apple system grays
    override val background = Color(0xFF000000)
    override val surface = Color(0xFF1C1C1E)
    override val surfaceElevated = Color(0xFF2C2C2E)
    override val surfaceVariant = Color(0xFF3A3A3C)
    override val surfaceInput = Color(0xFF2C2C2E)

    // Text - Apple label hierarchy
    override val textPrimary = Color(0xFFF5F5F7)
    override val textSecondary = Color(0xFFA1A1A6)
    override val textTertiary = Color(0xFF636366)
    override val textDisabled = Color(0xFF48484A)
    override val textOnPrimary = Color.Black

    // Icon
    override val iconPrimary = Color(0xFF00D4FF)
    override val iconSecondary = Color(0xFFA1A1A6)
    override val iconDisabled = Color(0xFF48484A)

    // Border - Subtle white glass edges
    override val border = Color(0x1FFFFFFF)
    override val borderSubtle = Color(0x0FFFFFFF)
    override val borderStrong = Color(0x33FFFFFF)

    // Semantic - Apple system colors
    override val success = Color(0xFF30D158)
    override val warning = Color(0xFFFFD60A)
    override val info = Color(0xFF00D4FF)

    // Special
    override val starActive = Color(0xFFFFD60A)
}
