/**
 * SolColors.kt - Sol palette color scheme (Theme v5.1)
 *
 * Warm golden sun aesthetic: amber gold primary, sunset red secondary.
 * Evolved from SunsetColors with refined warm-amber identity.
 * Dark + Light + XR variants for AppearanceMode and DisplayProfile axes.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object SolColors : AvanueColorScheme {
    // Primary - Amber Gold
    override val primary = Color(0xFFD97706)
    override val onPrimary = Color.White
    override val primaryDark = Color(0xFFB45309)
    override val primaryLight = Color(0xFFFBBF24)

    // Secondary - Sunset Red
    override val secondary = Color(0xFFEF4444)
    override val onSecondary = Color.White

    // Tertiary - Rose Pink
    override val tertiary = Color(0xFFF472B6)

    // Containers
    override val primaryContainer = Color(0xFF4A2C0A)    // Deep amber
    override val onPrimaryContainer = Color(0xFFFDE68A)  // Light gold text
    override val secondaryContainer = Color(0xFF5F1E1E)  // Deep red
    override val onSecondaryContainer = Color(0xFFFECACA) // Light red text
    override val tertiaryContainer = Color(0xFF4D1B3A)   // Deep rose
    override val onTertiaryContainer = Color(0xFFFBCFE8) // Light rose text
    override val errorContainer = Color(0xFF5F1E1E)
    override val onErrorContainer = Color(0xFFFECACA)

    // Error
    override val error = Color(0xFFEF4444)
    override val onError = Color.White

    // Background & Surface - Deep Amber
    override val background = Color(0xFF1A0F05)
    override val surface = Color(0xFF2D1B0E)
    override val surfaceElevated = Color(0xFF3D2917)
    override val surfaceVariant = Color(0xFF4D3720)
    override val surfaceInput = Color(0xFF3D2917)

    // Text
    override val textPrimary = Color(0xFFF5E6D3)
    override val textSecondary = Color(0xFFD4C0A8)
    override val textTertiary = Color(0xFFA08870)
    override val textDisabled = Color(0xFF6B5B45)
    override val textOnPrimary = Color.White

    // Icon
    override val iconPrimary = Color(0xFFD97706)
    override val iconSecondary = Color(0xFFD4C0A8)
    override val iconDisabled = Color(0xFF6B5B45)

    // Border
    override val border = Color(0x33FFFFFF)
    override val borderSubtle = Color(0x1AFFFFFF)
    override val borderStrong = Color(0x4DFFFFFF)

    // Semantic
    override val success = Color(0xFF22C55E)
    override val warning = Color(0xFFF59E0B)
    override val info = Color(0xFFFBBF24)

    // Special
    override val starActive = Color(0xFFFFC107)
}

/**
 * Sol Light — warm cream background, amber gold accents.
 * Same brand identity, inverted for light appearance.
 */
object SolColorsLight : AvanueColorScheme {
    // Primary - Amber Gold (same brand)
    override val primary = Color(0xFFB45309)
    override val onPrimary = Color.White
    override val primaryDark = Color(0xFF92400E)
    override val primaryLight = Color(0xFFD97706)

    // Secondary - Sunset Red
    override val secondary = Color(0xFFDC2626)
    override val onSecondary = Color.White

    // Tertiary - Rose Pink
    override val tertiary = Color(0xFFDB2777)

    // Containers - Light tinted backgrounds
    override val primaryContainer = Color(0xFFFEF3C7)    // Amber 100
    override val onPrimaryContainer = Color(0xFF78350F)  // Dark amber text
    override val secondaryContainer = Color(0xFFFEE2E2)  // Red 100
    override val onSecondaryContainer = Color(0xFF991B1B) // Dark red text
    override val tertiaryContainer = Color(0xFFFCE7F3)   // Rose 100
    override val onTertiaryContainer = Color(0xFF831843) // Dark rose text
    override val errorContainer = Color(0xFFFEE2E2)
    override val onErrorContainer = Color(0xFF991B1B)

    // Error
    override val error = Color(0xFFDC2626)
    override val onError = Color.White

    // Background & Surface - Warm Cream
    override val background = Color(0xFFFFFBF0)
    override val surface = Color(0xFFFFFFFF)
    override val surfaceElevated = Color(0xFFFFF7ED)
    override val surfaceVariant = Color(0xFFFED7AA)
    override val surfaceInput = Color(0xFFFFF7ED)

    // Text - Dark warm on Light
    override val textPrimary = Color(0xFF1C1917)
    override val textSecondary = Color(0xFF57534E)
    override val textTertiary = Color(0xFFA8A29E)
    override val textDisabled = Color(0xFFD6D3D1)
    override val textOnPrimary = Color.White

    // Icon
    override val iconPrimary = Color(0xFFB45309)
    override val iconSecondary = Color(0xFF78716C)
    override val iconDisabled = Color(0xFFD6D3D1)

    // Border - Black alphas
    override val border = Color(0x1F000000)
    override val borderSubtle = Color(0x0F000000)
    override val borderStrong = Color(0x40000000)

    // Semantic
    override val success = Color(0xFF16A34A)
    override val warning = Color(0xFFD97706)
    override val info = Color(0xFFD97706)

    // Special
    override val starActive = Color(0xFFF59E0B)
}

/**
 * Sol XR — additive display variant for AR smart glasses.
 * Amber gold boosted to high luminance. Transparent background.
 */
object SolColorsXR : AvanueColorScheme {
    override val primary = Color(0xFFFBBF24)            // amber-400 (excellent additive)
    override val onPrimary = Color(0xFF1C1917)
    override val primaryDark = Color(0xFFD97706)
    override val primaryLight = Color(0xFFFDE68A)

    override val secondary = Color(0xFFF87171)          // red-400
    override val onSecondary = Color(0xFF1C1917)

    override val tertiary = Color(0xFFF472B6)           // pink-400

    override val primaryContainer = Color(0x40D97706)
    override val onPrimaryContainer = Color(0xFFFDE68A)
    override val secondaryContainer = Color(0x40EF4444)
    override val onSecondaryContainer = Color(0xFFFECACA)
    override val tertiaryContainer = Color(0x40DB2777)
    override val onTertiaryContainer = Color(0xFFFBCFE8)
    override val errorContainer = Color(0x40DC2626)
    override val onErrorContainer = Color(0xFFFECACA)

    override val error = Color(0xFFF87171)
    override val onError = Color.White

    override val background = Color(0x00000000)
    override val surface = Color(0x262D1B0E)
    override val surfaceElevated = Color(0x403D2917)
    override val surfaceVariant = Color(0x334D3720)
    override val surfaceInput = Color(0x403D2917)

    override val textPrimary = Color(0xFFFEF3C7)       // warm white
    override val textSecondary = Color(0xFFD4C0A8)
    override val textTertiary = Color(0xFFA08870)
    override val textDisabled = Color(0xFF6B5B45)
    override val textOnPrimary = Color(0xFF1C1917)

    override val iconPrimary = Color(0xFFFBBF24)
    override val iconSecondary = Color(0xFFD4C0A8)
    override val iconDisabled = Color(0xFF6B5B45)

    override val border = Color(0x33FFFFFF)
    override val borderSubtle = Color(0x1AFFFFFF)
    override val borderStrong = Color(0x66FFFFFF)

    override val success = Color(0xFF4ADE80)
    override val warning = Color(0xFFFBBF24)
    override val info = Color(0xFFFBBF24)

    override val starActive = Color(0xFFFCD34D)
}
