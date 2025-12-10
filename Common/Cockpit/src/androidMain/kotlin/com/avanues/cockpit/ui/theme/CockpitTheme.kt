package com.avanues.cockpit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Cockpit Material 3 Theme
 *
 * Maps Cockpit visual identity to Material 3 color system
 * Supports Minimal and Glass border styles
 */

// Accent colors from Cockpit specs
private val AccentBlue = Color(0xFF2196F3)      // Communication
private val AccentOrange = Color(0xFFFF9800)    // Data/Analytics
private val AccentGreen = Color(0xFF4CAF50)     // Utilities

// Background gradient colors (from preferred embodiment)
private val BackgroundLight = Color(0xFFD4C5B0) // Light beige
private val BackgroundDark = Color(0xFFB8A596)  // Darker tan

// Surface colors for windows
private val SurfaceMinimal = Color(0xFFFAFAFA)  // Very light, minimal
private val SurfaceGlass = Color(0xE6000000)    // Dark glass, semi-transparent

// Cockpit Light Theme
private val CockpitLightColors = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentOrange,
    tertiary = AccentGreen,

    background = BackgroundLight,
    surface = SurfaceMinimal,
    surfaceVariant = Color(0xFFF5F5F5),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1C1C),
    onSurface = Color(0xFF1C1C1C),

    // Borders and outlines
    outline = Color(0x20000000),        // Minimal border (20% dark)
    outlineVariant = Color(0x40000000), // Glass border (40% dark)

    // Container colors for dock, rail, utility belt
    primaryContainer = Color(0xFFE3F2FD),
    secondaryContainer = Color(0xFFFFE0B2),
    tertiaryContainer = Color(0xFFE8F5E9)
)

// Cockpit Dark Theme
private val CockpitDarkColors = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentOrange,
    tertiary = AccentGreen,

    background = BackgroundDark,
    surface = SurfaceGlass,
    surfaceVariant = Color(0xFF2C2C2C),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),

    // Borders and outlines
    outline = Color(0x40FFFFFF),        // Minimal border (light)
    outlineVariant = Color(0x60FFFFFF), // Glass border with glow

    // Container colors for dock, rail, utility belt
    primaryContainer = Color(0xFF1565C0),
    secondaryContainer = Color(0xFFE65100),
    tertiaryContainer = Color(0xFF2E7D32)
)

/**
 * Cockpit Material 3 Theme
 *
 * @param darkTheme Whether to use dark theme
 * @param windowStyle Minimal or Glass window style
 * @param content Composable content
 */
@Composable
fun CockpitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    windowStyle: WindowStyle = WindowStyle.MINIMAL,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> CockpitDarkColors
        else -> CockpitLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CockpitTypography,
        shapes = CockpitShapes,
        content = content
    )
}

/**
 * Helper to get current window style colors
 */
@Composable
fun windowStyleColors(style: WindowStyle = WindowStyle.MINIMAL): WindowStyleColors {
    val isDark = isSystemInDarkTheme()

    return when (style) {
        WindowStyle.MINIMAL -> WindowStyleColors(
            borderColor = if (isDark) Color(0x40FFFFFF) else Color(0x20000000),
            borderWidth = 1f,
            shadowColor = Color(0x30000000),
            glowColor = null
        )
        WindowStyle.GLASS -> WindowStyleColors(
            borderColor = if (isDark) Color(0x60FFFFFF) else Color(0x40000000),
            borderWidth = 2f,
            shadowColor = Color(0x40000000),
            glowColor = if (isDark) Color(0x40FFFFFF) else Color(0x20FFFFFF)
        )
    }
}

data class WindowStyleColors(
    val borderColor: Color,
    val borderWidth: Float,
    val shadowColor: Color,
    val glowColor: Color?
)
