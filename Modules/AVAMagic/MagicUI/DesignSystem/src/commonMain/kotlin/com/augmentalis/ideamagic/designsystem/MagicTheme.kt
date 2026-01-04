package com.augmentalis.magicui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle

/**
 * MagicTheme - Main theme composable for IDEAMagic Design System
 *
 * Provides Material 3 theming with custom design tokens and extensions.
 * Supports light/dark mode, dynamic theming, and accessibility features.
 *
 * Usage:
 * ```kotlin
 * MagicTheme(
 *     darkTheme = false,
 *     dynamicColor = true
 * ) {
 *     // Your app content
 *     MyAppScreen()
 * }
 * ```
 *
 * @param darkTheme Whether to use dark theme. Defaults to system preference.
 * @param dynamicColor Whether to use dynamic color (Material You). Android 12+ only.
 * @param content The composable content to wrap with theme.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
@Composable
fun MagicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && supportsDynamicColor() -> {
            if (darkTheme) dynamicDarkColorScheme() else dynamicLightColorScheme()
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = MagicTypography
    val shapes = MagicShapes

    // Provide custom spacing and other tokens via composition local
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalElevation provides Elevation()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

// ===== COLOR SCHEMES =====

/**
 * Light color scheme using design tokens
 */
private val LightColorScheme = lightColorScheme(
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

/**
 * Dark color scheme using design tokens
 */
private val DarkColorScheme = darkColorScheme(
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

// ===== TYPOGRAPHY =====

/**
 * Typography scale using design tokens
 */
private val MagicTypography = Typography(
    displayLarge = TypographyTokens.DisplayLarge,
    displayMedium = TypographyTokens.DisplayMedium,
    displaySmall = TypographyTokens.DisplaySmall,

    headlineLarge = TypographyTokens.HeadlineLarge,
    headlineMedium = TypographyTokens.HeadlineMedium,
    headlineSmall = TypographyTokens.HeadlineSmall,

    titleLarge = TypographyTokens.TitleLarge,
    titleMedium = TypographyTokens.TitleMedium,
    titleSmall = TypographyTokens.TitleSmall,

    bodyLarge = TypographyTokens.BodyLarge,
    bodyMedium = TypographyTokens.BodyMedium,
    bodySmall = TypographyTokens.BodySmall,

    labelLarge = TypographyTokens.LabelLarge,
    labelMedium = TypographyTokens.LabelMedium,
    labelSmall = TypographyTokens.LabelSmall
)

// ===== SHAPES =====

/**
 * Shape scale using design tokens
 */
private val MagicShapes = Shapes(
    extraSmall = RoundedCornerShape(ShapeTokens.ExtraSmall),
    small = RoundedCornerShape(ShapeTokens.Small),
    medium = RoundedCornerShape(ShapeTokens.Medium),
    large = RoundedCornerShape(ShapeTokens.Large),
    extraLarge = RoundedCornerShape(ShapeTokens.ExtraLarge)
)

// ===== COMPOSITION LOCALS =====

/**
 * Spacing system accessible via MaterialTheme
 */
data class Spacing(
    val none: androidx.compose.ui.unit.Dp = SpacingTokens.None,
    val extraSmall: androidx.compose.ui.unit.Dp = SpacingTokens.ExtraSmall,
    val small: androidx.compose.ui.unit.Dp = SpacingTokens.Small,
    val medium: androidx.compose.ui.unit.Dp = SpacingTokens.Medium,
    val large: androidx.compose.ui.unit.Dp = SpacingTokens.Large,
    val extraLarge: androidx.compose.ui.unit.Dp = SpacingTokens.ExtraLarge,
    val extraExtraLarge: androidx.compose.ui.unit.Dp = SpacingTokens.ExtraExtraLarge,
    val huge: androidx.compose.ui.unit.Dp = SpacingTokens.Huge
)

/**
 * Elevation system accessible via MaterialTheme
 */
data class Elevation(
    val level0: androidx.compose.ui.unit.Dp = ElevationTokens.Level0,
    val level1: androidx.compose.ui.unit.Dp = ElevationTokens.Level1,
    val level2: androidx.compose.ui.unit.Dp = ElevationTokens.Level2,
    val level3: androidx.compose.ui.unit.Dp = ElevationTokens.Level3,
    val level4: androidx.compose.ui.unit.Dp = ElevationTokens.Level4,
    val level5: androidx.compose.ui.unit.Dp = ElevationTokens.Level5
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalElevation = staticCompositionLocalOf { Elevation() }

/**
 * Extension to access spacing from MaterialTheme
 */
object MagicThemeExtensions {
    val spacing: Spacing
        @Composable
        get() = LocalSpacing.current

    val elevation: Elevation
        @Composable
        get() = LocalElevation.current
}

// ===== PLATFORM-SPECIFIC DYNAMIC COLOR SUPPORT =====

/**
 * Check if platform supports dynamic color (Material You)
 * Expect/actual pattern - implemented per platform
 */
internal expect fun supportsDynamicColor(): Boolean

/**
 * Get dynamic light color scheme
 * Expect/actual pattern - implemented per platform
 */
@Composable
internal expect fun dynamicLightColorScheme(): ColorScheme

/**
 * Get dynamic dark color scheme
 * Expect/actual pattern - implemented per platform
 */
@Composable
internal expect fun dynamicDarkColorScheme(): ColorScheme
