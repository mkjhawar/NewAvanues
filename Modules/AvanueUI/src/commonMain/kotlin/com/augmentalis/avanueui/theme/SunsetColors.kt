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

    // Containers - tinted backgrounds for cards/chips
    override val primaryContainer = Color(0xFF5F2E1A)    // Deep coral (primary@20% on dark bg)
    override val onPrimaryContainer = Color(0xFFFFD4C2)  // Light coral text on container
    override val secondaryContainer = Color(0xFF5F4B0E)  // Deep gold
    override val onSecondaryContainer = Color(0xFFFDE68A) // Light gold text on container
    override val tertiaryContainer = Color(0xFF4D1B3A)   // Deep rose (tertiary@20% on dark bg)
    override val onTertiaryContainer = Color(0xFFFBCFE8) // Light rose text on container
    override val errorContainer = Color(0xFF5F1E1E)      // Deep red
    override val onErrorContainer = Color(0xFFFECACA)    // Light red text on container

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
