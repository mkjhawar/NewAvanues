// filename: Universal/AVA/Core/Theme/src/androidMain/kotlin/com/augmentalis/ava/core/theme/AvaTheme.android.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.avanueui

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of dynamic color support
 * Material You dynamic colors available on Android 12+ (API 31+)
 */
internal actual fun supportsDynamicColor(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/**
 * Get dynamic light color scheme on Android
 * Uses system wallpaper colors on Android 12+
 */
@Composable
internal actual fun dynamicLightColorScheme(): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context)
    } else {
        LightColorScheme
    }
}

/**
 * Get dynamic dark color scheme on Android
 * Uses system wallpaper colors on Android 12+
 */
@Composable
internal actual fun dynamicDarkColorScheme(): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        DarkColorScheme
    }
}

// Expose color schemes for Android-specific code
private val LightColorScheme = androidx.compose.material3.lightColorScheme(
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

private val DarkColorScheme = androidx.compose.material3.darkColorScheme(
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
