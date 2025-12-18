/**
 * VoiceOSTheme.kt - Material 3 Theme for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// VoiceOS Brand Colors
object VoiceOSColors {
    // Primary - Ocean Blue
    val Primary = Color(0xFF4285F4)
    val PrimaryDark = Color(0xFF1A73E8)
    val PrimaryLight = Color(0xFF8AB4F8)
    val OnPrimary = Color.White

    // Secondary - Purple Accent
    val Secondary = Color(0xFF673AB7)
    val SecondaryDark = Color(0xFF512DA8)
    val OnSecondary = Color.White

    // Tertiary - Success Green
    val Tertiary = Color(0xFF4CAF50)
    val TertiaryDark = Color(0xFF388E3C)
    val OnTertiary = Color.White

    // Error - Alert Red
    val Error = Color(0xFFE53935)
    val ErrorDark = Color(0xFFC62828)
    val OnError = Color.White

    // Background & Surface
    val Background = Color(0xFFF8F9FA)
    val BackgroundDark = Color(0xFF121212)
    val Surface = Color.White
    val SurfaceDark = Color(0xFF1E1E1E)
    val SurfaceVariant = Color(0xFFE8EAED)
    val SurfaceVariantDark = Color(0xFF2D2D2D)

    // Text
    val OnBackground = Color(0xFF202124)
    val OnBackgroundDark = Color(0xFFE8EAED)
    val OnSurface = Color(0xFF202124)
    val OnSurfaceDark = Color(0xFFE8EAED)

    // Status Colors
    val StatusActive = Color(0xFF4CAF50)
    val StatusInactive = Color(0xFF9E9E9E)
    val StatusListening = Color(0xFF2196F3)
    val StatusProcessing = Color(0xFFFF9800)
}

private val LightColorScheme = lightColorScheme(
    primary = VoiceOSColors.Primary,
    onPrimary = VoiceOSColors.OnPrimary,
    primaryContainer = VoiceOSColors.PrimaryLight,
    onPrimaryContainer = VoiceOSColors.PrimaryDark,
    secondary = VoiceOSColors.Secondary,
    onSecondary = VoiceOSColors.OnSecondary,
    tertiary = VoiceOSColors.Tertiary,
    onTertiary = VoiceOSColors.OnTertiary,
    error = VoiceOSColors.Error,
    onError = VoiceOSColors.OnError,
    background = VoiceOSColors.Background,
    onBackground = VoiceOSColors.OnBackground,
    surface = VoiceOSColors.Surface,
    onSurface = VoiceOSColors.OnSurface,
    surfaceVariant = VoiceOSColors.SurfaceVariant,
    onSurfaceVariant = VoiceOSColors.OnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = VoiceOSColors.PrimaryLight,
    onPrimary = VoiceOSColors.PrimaryDark,
    primaryContainer = VoiceOSColors.PrimaryDark,
    onPrimaryContainer = VoiceOSColors.PrimaryLight,
    secondary = VoiceOSColors.Secondary,
    onSecondary = VoiceOSColors.OnSecondary,
    tertiary = VoiceOSColors.Tertiary,
    onTertiary = VoiceOSColors.OnTertiary,
    error = VoiceOSColors.Error,
    onError = VoiceOSColors.OnError,
    background = VoiceOSColors.BackgroundDark,
    onBackground = VoiceOSColors.OnBackgroundDark,
    surface = VoiceOSColors.SurfaceDark,
    onSurface = VoiceOSColors.OnSurfaceDark,
    surfaceVariant = VoiceOSColors.SurfaceVariantDark,
    onSurfaceVariant = VoiceOSColors.OnSurfaceDark
)

@Composable
fun VoiceOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
