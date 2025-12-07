/**
 * Theme.kt - Material3 theme configuration for VoiceOS Accessibility
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Material3 theme with dark mode optimized for glassmorphism UI.
 */
package com.augmentalis.voiceos.accessibility.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// VoiceOS brand colors
private val VoiceOSBlue = Color(0xFF4285F4)
private val VoiceOSBlueDark = Color(0xFF1565C0)
private val VoiceOSGreen = Color(0xFF00C853)
private val VoiceOSPurple = Color(0xFF673AB7)
private val VoiceOSRed = Color(0xFFFF5722)

// Dark theme colors (primary)
private val DarkColorScheme = darkColorScheme(
    primary = VoiceOSBlue,
    onPrimary = Color.White,
    primaryContainer = VoiceOSBlueDark,
    onPrimaryContainer = Color.White,
    
    secondary = VoiceOSPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF512DA8),
    onSecondaryContainer = Color.White,
    
    tertiary = VoiceOSGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF00A845),
    onTertiaryContainer = Color.White,
    
    error = VoiceOSRed,
    onError = Color.White,
    errorContainer = Color(0xFFE53935),
    onErrorContainer = Color.White,
    
    background = Color(0xFF000000),
    onBackground = Color.White,
    
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFE0E0E0),
    
    outline = Color(0xFF424242),
    outlineVariant = Color(0xFF2E2E2E),
    
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = VoiceOSBlueDark
)

// Light theme colors (fallback, app is primarily dark)
private val LightColorScheme = lightColorScheme(
    primary = VoiceOSBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    
    secondary = VoiceOSPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1C4E9),
    onSecondaryContainer = Color(0xFF311B92),
    
    tertiary = Color(0xFF00A845),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8E6C9),
    onTertiaryContainer = Color(0xFF2E7D32),
    
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFC62828),
    
    background = Color.White,
    onBackground = Color.Black,
    
    surface = Color(0xFFFAFAFA),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0)
)

@Composable
fun AccessibilityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}