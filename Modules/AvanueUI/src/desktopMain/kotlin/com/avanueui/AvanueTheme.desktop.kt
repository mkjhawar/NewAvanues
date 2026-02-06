// filename: Universal/AVA/Core/Theme/src/desktopMain/kotlin/com/augmentalis/ava/core/theme/AvaTheme.desktop.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.avanueui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Desktop implementation of dynamic color support
 * Dynamic colors not supported on Desktop
 */
internal actual fun supportsDynamicColor(): Boolean {
    return false
}

/**
 * Get light color scheme on Desktop
 * Returns static AVA color scheme
 */
@Composable
internal actual fun dynamicLightColorScheme(): ColorScheme {
    return lightColorScheme(
        primary = ColorTokens.Primary,
        onPrimary = ColorTokens.OnPrimary,
        primaryContainer = ColorTokens.PrimaryContainer,
        onPrimaryContainer = ColorTokens.OnPrimaryContainer,
        secondary = ColorTokens.Secondary,
        onSecondary = ColorTokens.OnSecondary,
        secondaryContainer = ColorTokens.SecondaryContainer,
        onSecondaryContainer = ColorTokens.OnSecondaryContainer,
        tertiary = ColorTokens.Tertiary,
        onTertiary = ColorTokens.OnTertiary,
        tertiaryContainer = ColorTokens.TertiaryContainer,
        onTertiaryContainer = ColorTokens.OnTertiaryContainer,
        error = ColorTokens.Error,
        onError = ColorTokens.OnError,
        errorContainer = ColorTokens.ErrorContainer,
        onErrorContainer = ColorTokens.OnErrorContainer,
        background = ColorTokens.Background,
        onBackground = ColorTokens.OnBackground,
        surface = ColorTokens.Surface,
        onSurface = ColorTokens.OnSurface,
        surfaceVariant = ColorTokens.SurfaceVariant,
        onSurfaceVariant = ColorTokens.OnSurfaceVariant,
        outline = ColorTokens.Outline,
        outlineVariant = ColorTokens.OutlineVariant
    )
}

/**
 * Get dark color scheme on Desktop
 * Returns static AVA dark color scheme
 */
@Composable
internal actual fun dynamicDarkColorScheme(): ColorScheme {
    return darkColorScheme(
        primary = ColorTokens.DarkPrimary,
        onPrimary = ColorTokens.DarkOnPrimary,
        primaryContainer = ColorTokens.DarkPrimaryContainer,
        onPrimaryContainer = ColorTokens.DarkOnPrimaryContainer,
        secondary = ColorTokens.DarkSecondary,
        onSecondary = ColorTokens.DarkOnSecondary,
        secondaryContainer = ColorTokens.DarkSecondaryContainer,
        onSecondaryContainer = ColorTokens.DarkOnSecondaryContainer,
        tertiary = ColorTokens.DarkTertiary,
        onTertiary = ColorTokens.DarkOnTertiary,
        tertiaryContainer = ColorTokens.DarkTertiaryContainer,
        onTertiaryContainer = ColorTokens.DarkOnTertiaryContainer,
        error = ColorTokens.DarkError,
        onError = ColorTokens.DarkOnError,
        errorContainer = ColorTokens.DarkErrorContainer,
        onErrorContainer = ColorTokens.DarkOnErrorContainer,
        background = ColorTokens.DarkBackground,
        onBackground = ColorTokens.DarkOnBackground,
        surface = ColorTokens.DarkSurface,
        onSurface = ColorTokens.DarkOnSurface,
        surfaceVariant = ColorTokens.DarkSurfaceVariant,
        onSurfaceVariant = ColorTokens.DarkOnSurfaceVariant,
        outline = ColorTokens.DarkOutline,
        outlineVariant = ColorTokens.DarkOutlineVariant
    )
}
