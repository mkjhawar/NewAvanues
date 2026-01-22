package com.augmentalis.webavanue

import androidx.compose.ui.text.TextStyle

/**
 * AppTypography - Theme-agnostic typography interface
 *
 * Allows switching between Material Design 3 and IdeaMagic themes
 * without changing UI component code.
 *
 * Implementations:
 * - MaterialTypography (Material Design 3) - Default/Standalone mode
 * - IdeaMagicTypography (IdeaMagic) - Avanues ecosystem mode
 */
interface AppTypography {
    // Display styles (large headlines)
    val displayLarge: TextStyle
    val displayMedium: TextStyle
    val displaySmall: TextStyle

    // Headline styles
    val headlineLarge: TextStyle
    val headlineMedium: TextStyle
    val headlineSmall: TextStyle

    // Title styles
    val titleLarge: TextStyle
    val titleMedium: TextStyle
    val titleSmall: TextStyle

    // Body styles
    val bodyLarge: TextStyle
    val bodyMedium: TextStyle
    val bodySmall: TextStyle

    // Label styles
    val labelLarge: TextStyle
    val labelMedium: TextStyle
    val labelSmall: TextStyle
}
