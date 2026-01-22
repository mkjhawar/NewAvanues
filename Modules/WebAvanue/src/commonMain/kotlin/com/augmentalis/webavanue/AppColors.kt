package com.augmentalis.webavanue

import androidx.compose.ui.graphics.Color

/**
 * AppColors - Theme-agnostic color interface
 *
 * Allows switching between Material Design 3 and IdeaMagic themes
 * without changing UI component code.
 *
 * Implementations:
 * - MaterialColors (Material Design 3) - Default/Standalone mode
 * - IdeaMagicColors (IdeaMagic) - Avanues ecosystem mode
 */
interface AppColors {
    // Primary colors
    val primary: Color
    val onPrimary: Color
    val primaryContainer: Color
    val onPrimaryContainer: Color

    // Secondary colors
    val secondary: Color
    val onSecondary: Color
    val secondaryContainer: Color
    val onSecondaryContainer: Color

    // Tertiary colors
    val tertiary: Color
    val onTertiary: Color
    val tertiaryContainer: Color
    val onTertiaryContainer: Color

    // Error colors
    val error: Color
    val onError: Color
    val errorContainer: Color
    val onErrorContainer: Color

    // Background colors
    val background: Color
    val onBackground: Color

    // Surface colors
    val surface: Color
    val onSurface: Color
    val surfaceVariant: Color
    val onSurfaceVariant: Color

    // Outline colors
    val outline: Color
    val outlineVariant: Color

    // Avanues-specific colors (graceful degradation for non-Avanues themes)
    val voiceActive: Color
        get() = primary

    val voiceInactive: Color
        get() = onSurfaceVariant

    val voiceListening: Color
        get() = secondary

    // Browser-specific colors
    val tabActive: Color
        get() = primaryContainer

    val tabInactive: Color
        get() = surface

    val addressBarBackground: Color
        get() = surfaceVariant
}
