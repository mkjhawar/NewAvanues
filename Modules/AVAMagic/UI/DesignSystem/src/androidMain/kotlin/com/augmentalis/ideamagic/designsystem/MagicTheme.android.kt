package com.augmentalis.avanues.avamagic.designsystem

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation for dynamic color support (Material You)
 * Available on Android 12 (API 31) and above
 */
internal actual fun supportsDynamicColor(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/**
 * Get Material You dynamic light color scheme on Android
 */
@Composable
internal actual fun dynamicLightColorScheme(): ColorScheme {
    return if (supportsDynamicColor()) {
        dynamicLightColorScheme(LocalContext.current)
    } else {
        // Fallback to static light scheme
        androidx.compose.material3.lightColorScheme(
            primary = ColorTokens.Primary,
            onPrimary = ColorTokens.OnPrimary,
            primaryContainer = ColorTokens.PrimaryContainer,
            onPrimaryContainer = ColorTokens.OnPrimaryContainer
        )
    }
}

/**
 * Get Material You dynamic dark color scheme on Android
 */
@Composable
internal actual fun dynamicDarkColorScheme(): ColorScheme {
    return if (supportsDynamicColor()) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        // Fallback to static dark scheme
        androidx.compose.material3.darkColorScheme(
            primary = ColorTokens.DarkPrimary,
            onPrimary = ColorTokens.DarkOnPrimary,
            primaryContainer = ColorTokens.DarkPrimaryContainer,
            onPrimaryContainer = ColorTokens.DarkOnPrimaryContainer
        )
    }
}
