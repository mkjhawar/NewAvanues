package com.augmentalis.webavanue

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.augmentalis.webavanue.AppColors
import com.augmentalis.webavanue.AppTypography
import com.augmentalis.webavanue.AvanueColors
import com.augmentalis.webavanue.AvanueTypography
import com.augmentalis.webavanue.WebAvanueColors
import com.augmentalis.webavanue.WebAvanueTypography

/**
 * AppTheme - Main theme provider for WebAvanue
 *
 * Automatically detects environment and applies appropriate theme:
 * - APP_BRANDING (WebAvanue's unique purple/blue branding) - Standalone mode
 * - AVANUE (Avanues system theme) - Avanues ecosystem mode
 *
 * Theme Selection Logic:
 * 1. If user preference set → use that
 * 2. If Avanues ecosystem detected → AVANUE (system theme overrides app branding)
 * 3. If standalone → APP_BRANDING (WebAvanue's unique colors)
 *
 * Users can override auto-detection via Settings screen.
 *
 * Usage:
 * ```kotlin
 * AppTheme {
 *     // Your UI components here
 * }
 * ```
 *
 * Theme-agnostic components access colors/typography via:
 * ```kotlin
 * val colors = LocalAppColors.current
 * val typography = LocalAppTypography.current
 * val themeType = LocalThemeType.current
 * ```
 *
 * @param darkTheme Whether to use dark theme (defaults to system preference)
 * @param dynamicColor Whether to use dynamic colors (future feature)
 * @param themeType Override theme type (null = auto-detect)
 * @param content Composable content
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeType: ThemeType? = null,
    content: @Composable () -> Unit
) {
    // Resolve theme (auto-detect or user preference)
    val resolvedTheme = remember(themeType) {
        resolveTheme(themeType ?: ThemePreferences.getTheme())
    }

    // Create color and typography providers based on theme
    val colors: AppColors = remember(resolvedTheme, darkTheme) {
        when (resolvedTheme) {
            ThemeType.APP_BRANDING -> WebAvanueColors(isDark = darkTheme)
            ThemeType.AVANUE -> AvanueColors(isDark = darkTheme)
            ThemeType.AUTO -> WebAvanueColors(isDark = darkTheme) // Fallback
        }
    }

    val typography: AppTypography = remember(resolvedTheme) {
        when (resolvedTheme) {
            ThemeType.APP_BRANDING -> WebAvanueTypography()
            ThemeType.AVANUE -> AvanueTypography()
            ThemeType.AUTO -> WebAvanueTypography() // Fallback
        }
    }

    // Provide theme via CompositionLocal
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalThemeType provides resolvedTheme
    ) {
        // Also provide Material theme for Material3 components
        MaterialTheme(
            colorScheme = if (darkTheme) {
                androidx.compose.material3.darkColorScheme(
                    primary = colors.primary,
                    onPrimary = colors.onPrimary,
                    primaryContainer = colors.primaryContainer,
                    onPrimaryContainer = colors.onPrimaryContainer,
                    secondary = colors.secondary,
                    onSecondary = colors.onSecondary,
                    background = colors.background,
                    onBackground = colors.onBackground,
                    surface = colors.surface,
                    onSurface = colors.onSurface,
                    error = colors.error,
                    onError = colors.onError
                )
            } else {
                androidx.compose.material3.lightColorScheme(
                    primary = colors.primary,
                    onPrimary = colors.onPrimary,
                    primaryContainer = colors.primaryContainer,
                    onPrimaryContainer = colors.onPrimaryContainer,
                    secondary = colors.secondary,
                    onSecondary = colors.onSecondary,
                    background = colors.background,
                    onBackground = colors.onBackground,
                    surface = colors.surface,
                    onSurface = colors.onSurface,
                    error = colors.error,
                    onError = colors.onError
                )
            },
            content = content
        )
    }
}

/**
 * CompositionLocal for accessing AppColors anywhere in the composition
 */
val LocalAppColors = compositionLocalOf<AppColors> {
    error("No AppColors provided")
}

/**
 * CompositionLocal for accessing AppTypography anywhere in the composition
 */
val LocalAppTypography = compositionLocalOf<AppTypography> {
    error("No AppTypography provided")
}

/**
 * CompositionLocal for accessing current theme type
 */
val LocalThemeType = compositionLocalOf<ThemeType> {
    ThemeType.APP_BRANDING
}
