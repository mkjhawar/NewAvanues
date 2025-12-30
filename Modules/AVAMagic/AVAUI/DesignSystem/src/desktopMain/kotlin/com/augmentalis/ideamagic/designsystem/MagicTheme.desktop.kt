package com.augmentalis.avamagic.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Desktop (JVM) implementation - dynamic color not supported
 */
internal actual fun supportsDynamicColor(): Boolean {
    return false
}

/**
 * Desktop fallback to static light color scheme
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
        onSecondaryContainer = ColorTokens.OnSecondaryContainer
    )
}

/**
 * Desktop fallback to static dark color scheme
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
        onSecondaryContainer = ColorTokens.DarkOnSecondaryContainer
    )
}
