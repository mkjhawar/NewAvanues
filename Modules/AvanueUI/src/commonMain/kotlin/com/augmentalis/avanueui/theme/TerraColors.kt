/**
 * TerraColors.kt - Terra palette color scheme (Theme v5.1)
 *
 * Natural earth/forest aesthetic: forest green primary, warm amber secondary.
 * Dark + Light variants for AppearanceMode axis.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object TerraColors : AvanueColorScheme {
    // Primary - Forest Green
    override val primary = Color(0xFF2D7D46)
    override val onPrimary = Color.White
    override val primaryDark = Color(0xFF1B5E30)
    override val primaryLight = Color(0xFF4ADE80)

    // Secondary - Warm Amber
    override val secondary = Color(0xFFD97706)
    override val onSecondary = Color.White

    // Tertiary - Earth Brown
    override val tertiary = Color(0xFFA16207)

    // Containers
    override val primaryContainer = Color(0xFF0F3D1E)    // Deep forest
    override val onPrimaryContainer = Color(0xFFBBF7D0)  // Light green text
    override val secondaryContainer = Color(0xFF4A2C0A)  // Deep amber
    override val onSecondaryContainer = Color(0xFFFDE68A) // Light gold text
    override val tertiaryContainer = Color(0xFF3D2607)   // Deep earth
    override val onTertiaryContainer = Color(0xFFFDE68A) // Light earth text
    override val errorContainer = Color(0xFF5F1E1E)
    override val onErrorContainer = Color(0xFFFECACA)

    // Error
    override val error = Color(0xFFEF4444)
    override val onError = Color.White

    // Background & Surface - Deep Forest
    override val background = Color(0xFF0F1A10)
    override val surface = Color(0xFF1A2B1C)
    override val surfaceElevated = Color(0xFF253C28)
    override val surfaceVariant = Color(0xFF304D34)
    override val surfaceInput = Color(0xFF253C28)

    // Text
    override val textPrimary = Color(0xFFE8F0E4)
    override val textSecondary = Color(0xFFBFD0B8)
    override val textTertiary = Color(0xFF8AA080)
    override val textDisabled = Color(0xFF5A6B55)
    override val textOnPrimary = Color.White

    // Icon
    override val iconPrimary = Color(0xFF2D7D46)
    override val iconSecondary = Color(0xFFBFD0B8)
    override val iconDisabled = Color(0xFF5A6B55)

    // Border
    override val border = Color(0x33FFFFFF)
    override val borderSubtle = Color(0x1AFFFFFF)
    override val borderStrong = Color(0x4DFFFFFF)

    // Semantic
    override val success = Color(0xFF22C55E)
    override val warning = Color(0xFFF59E0B)
    override val info = Color(0xFF2D7D46)

    // Special
    override val starActive = Color(0xFFFFC107)
}

/**
 * Terra Light — mint white background, forest green accents.
 * Same brand identity, inverted for light appearance.
 */
object TerraColorsLight : AvanueColorScheme {
    // Primary - Forest Green (same brand)
    override val primary = Color(0xFF15803D)
    override val onPrimary = Color.White
    override val primaryDark = Color(0xFF166534)
    override val primaryLight = Color(0xFF22C55E)

    // Secondary - Warm Amber
    override val secondary = Color(0xFFB45309)
    override val onSecondary = Color.White

    // Tertiary - Earth Brown
    override val tertiary = Color(0xFF92400E)

    // Containers - Light tinted backgrounds
    override val primaryContainer = Color(0xFFDCFCE7)    // Green 100
    override val onPrimaryContainer = Color(0xFF14532D)  // Dark green text
    override val secondaryContainer = Color(0xFFFEF3C7)  // Amber 100
    override val onSecondaryContainer = Color(0xFF78350F) // Dark amber text
    override val tertiaryContainer = Color(0xFFFEF3C7)   // Warm 100
    override val onTertiaryContainer = Color(0xFF78350F) // Dark earth text
    override val errorContainer = Color(0xFFFEE2E2)
    override val onErrorContainer = Color(0xFF991B1B)

    // Error
    override val error = Color(0xFFDC2626)
    override val onError = Color.White

    // Background & Surface - Mint White
    override val background = Color(0xFFF0FDF4)
    override val surface = Color(0xFFFFFFFF)
    override val surfaceElevated = Color(0xFFDCFCE7)
    override val surfaceVariant = Color(0xFFBBF7D0)
    override val surfaceInput = Color(0xFFF0FDF4)

    // Text - Dark green-tinted on Light
    override val textPrimary = Color(0xFF14532D)
    override val textSecondary = Color(0xFF4B5563)
    override val textTertiary = Color(0xFF9CA3AF)
    override val textDisabled = Color(0xFFD1D5DB)
    override val textOnPrimary = Color.White

    // Icon
    override val iconPrimary = Color(0xFF15803D)
    override val iconSecondary = Color(0xFF6B7280)
    override val iconDisabled = Color(0xFFD1D5DB)

    // Border - Black alphas
    override val border = Color(0x1F000000)
    override val borderSubtle = Color(0x0F000000)
    override val borderStrong = Color(0x40000000)

    // Semantic
    override val success = Color(0xFF16A34A)
    override val warning = Color(0xFFD97706)
    override val info = Color(0xFF15803D)

    // Special
    override val starActive = Color(0xFFF59E0B)
}
