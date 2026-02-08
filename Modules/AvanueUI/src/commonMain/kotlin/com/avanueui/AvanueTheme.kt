// filename: Universal/AVA/Core/Theme/src/commonMain/kotlin/com/augmentalis/ava/core/theme/AvaTheme.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.avanueui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * AvanueTheme - Universal Compose Theme for Avanues Ecosystem
 *
 * Provides Material 3 theming with Avanues glassmorphic design system.
 * Works across Android, iOS, and Desktop platforms.
 *
 * Usage:
 * ```kotlin
 * AvanueTheme(darkTheme = false, dynamicColor = true) {
 *     MyScreen()
 * }
 * ```
 *
 * Accessing theme tokens:
 * ```kotlin
 * @Composable
 * fun MyComponent() {
 *     val spacing = AvanueTheme.spacing.medium
 *     val elevation = AvanueTheme.elevation.level2
 * }
 * ```
 */
@Composable
fun AvanueTheme(
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

    val typography = AvanueTypography
    val shapes = AvanueShapes

    // Provide custom spacing, elevation, and other tokens via composition local
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalElevation provides Elevation(),
        LocalBreakpoints provides Breakpoints()
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
 * Light color scheme using AVA glassmorphic design tokens
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
 * Dark color scheme using AVA glassmorphic design tokens
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

    // Solid ocean surfaces for readability (not transparent glass)
    surface = ColorTokens.OceanMid,                           // #1E293B solid
    onSurface = ColorTokens.DarkOnSurface,
    surfaceVariant = ColorTokens.OceanShallow,                // #334155 solid
    onSurfaceVariant = ColorTokens.DarkOnSurfaceVariant,

    // Surface container tonal hierarchy (solid ocean blues)
    surfaceContainerLowest = ColorTokens.DeepOcean,           // #0A1929
    surfaceContainerLow = ColorTokens.OceanDepth,             // #0F172A
    surfaceContainer = ColorTokens.OceanMid,                  // #1E293B
    surfaceContainerHigh = ColorTokens.OceanShallow,          // #334155
    surfaceContainerHighest = ColorTokens.SurfaceHighest,     // #475569

    outline = ColorTokens.DarkOutline,
    outlineVariant = ColorTokens.DarkOutlineVariant
)

// ===== TYPOGRAPHY =====

/**
 * Typography scale using design tokens
 */
private val AvanueTypography = Typography(
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
private val AvanueShapes = Shapes(
    extraSmall = RoundedCornerShape(ShapeTokens.ExtraSmall),
    small = RoundedCornerShape(ShapeTokens.Small),
    medium = RoundedCornerShape(ShapeTokens.Medium),
    large = RoundedCornerShape(ShapeTokens.Large),
    extraLarge = RoundedCornerShape(ShapeTokens.ExtraLarge)
)

// ===== COMPOSITION LOCALS =====

/**
 * Spacing system accessible via AvaTheme
 */
data class Spacing(
    val none: Dp = SpacingTokens.None,
    val extraSmall: Dp = SpacingTokens.ExtraSmall,
    val small: Dp = SpacingTokens.Small,
    val medium: Dp = SpacingTokens.Medium,
    val large: Dp = SpacingTokens.Large,
    val extraLarge: Dp = SpacingTokens.ExtraLarge,
    val extraExtraLarge: Dp = SpacingTokens.ExtraExtraLarge,
    val huge: Dp = SpacingTokens.Huge
)

/**
 * Elevation system accessible via AvaTheme
 */
data class Elevation(
    val level0: Dp = ElevationTokens.Level0,
    val level1: Dp = ElevationTokens.Level1,
    val level2: Dp = ElevationTokens.Level2,
    val level3: Dp = ElevationTokens.Level3,
    val level4: Dp = ElevationTokens.Level4,
    val level5: Dp = ElevationTokens.Level5
)

/**
 * Responsive breakpoints accessible via AvaTheme
 */
data class Breakpoints(
    val compactMaxWidth: Dp = BreakpointTokens.CompactMaxWidth,
    val mediumMinWidth: Dp = BreakpointTokens.MediumMinWidth,
    val expandedMinWidth: Dp = BreakpointTokens.ExpandedMinWidth
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalElevation = staticCompositionLocalOf { Elevation() }
val LocalBreakpoints = staticCompositionLocalOf { Breakpoints() }

/**
 * Object to access theme extensions.
 * Use: AvanueTheme.spacing.medium, AvanueTheme.elevation.level2
 */
object AvanueTheme {
    val spacing: Spacing
        @Composable
        get() = LocalSpacing.current

    val elevation: Elevation
        @Composable
        get() = LocalElevation.current

    val breakpoints: Breakpoints
        @Composable
        get() = LocalBreakpoints.current
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
