package com.augmentalis.avanueui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.augmentalis.avanueui.display.DisplayProfile
import com.augmentalis.avanueui.tokens.ShapeTokens
import com.augmentalis.avanueui.tokens.TypographyTokens

// ===== CompositionLocals =====

val LocalAvanueColors = staticCompositionLocalOf<AvanueColorScheme> { OceanColors }
val LocalAvanueGlass = staticCompositionLocalOf<AvanueGlassScheme> { OceanGlass }
val LocalDisplayProfile = staticCompositionLocalOf { DisplayProfile.PHONE }

/**
 * Unified theme provider for the Avanues ecosystem.
 *
 * Sets up both Material3 theming (for M3 components like TextField, Button) and
 * AvanueUI custom theming (colors, glass, display profile, density scaling).
 *
 * This is the ONLY theme wrapper your app needs:
 * ```
 * AvanueThemeProvider(
 *     colors = OceanColors,
 *     displayProfile = DisplayProfile.PHONE
 * ) {
 *     MyScreen()  // Has full M3 + AvanueUI theming
 * }
 * ```
 *
 * When [displayProfile] specifies a non-1.0 density scale, [LocalDensity] is overridden
 * so that ALL dp/sp values automatically adapt. Existing code using `SpacingTokens.md`
 * (16dp) will render at 12dp physical on GLASS_COMPACT (0.75x) with zero code changes.
 */
@Composable
fun AvanueThemeProvider(
    colors: AvanueColorScheme = OceanColors,
    glass: AvanueGlassScheme = OceanGlass,
    displayProfile: DisplayProfile = DisplayProfile.PHONE,
    content: @Composable () -> Unit
) {
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density * displayProfile.densityScale,
        fontScale = currentDensity.fontScale * displayProfile.fontScale
    )

    CompositionLocalProvider(
        LocalAvanueColors provides colors,
        LocalAvanueGlass provides glass,
        LocalDisplayProfile provides displayProfile,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = colors.toM3ColorScheme(),
            typography = AvanueTypography,
            shapes = AvanueShapes,
            content = content
        )
    }
}

/**
 * Unified facade for accessing the current theme.
 *
 * Static tokens: access directly (e.g., `SpacingTokens.md`, `ShapeTokens.lg`).
 * Themed values: access via this object (e.g., `AvanueTheme.colors.primary`).
 * Display profile: access via `AvanueTheme.displayProfile`.
 */
object AvanueTheme {
    val colors: AvanueColorScheme
        @Composable get() = LocalAvanueColors.current

    val glass: AvanueGlassScheme
        @Composable get() = LocalAvanueGlass.current

    val displayProfile: DisplayProfile
        @Composable get() = LocalDisplayProfile.current
}

// ===== Material3 Integration =====

/**
 * Maps AvanueColorScheme to Material3 ColorScheme.
 * This allows M3 components (TextField, Button, etc.) to use AvanueUI colors.
 */
private fun AvanueColorScheme.toM3ColorScheme(): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryDark,
    onPrimaryContainer = onPrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondary,
    onSecondaryContainer = onSecondary,
    tertiary = tertiary,
    error = error,
    onError = onError,
    background = background,
    onBackground = textPrimary,
    surface = surface,
    onSurface = textPrimary,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = textSecondary,
    outline = border,
    outlineVariant = borderSubtle,
    surfaceContainerLowest = background,
    surfaceContainerLow = background,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceElevated,
    surfaceContainerHighest = surfaceVariant,
)

/**
 * Typography scale using AvanueUI design tokens.
 */
private val AvanueTypography = Typography(
    displayLarge = TypographyTokens.displayLarge,
    displayMedium = TypographyTokens.displayMedium,
    displaySmall = TypographyTokens.displaySmall,
    headlineLarge = TypographyTokens.headlineLarge,
    headlineMedium = TypographyTokens.headlineMedium,
    headlineSmall = TypographyTokens.headlineSmall,
    titleLarge = TypographyTokens.titleLarge,
    titleMedium = TypographyTokens.titleMedium,
    titleSmall = TypographyTokens.titleSmall,
    bodyLarge = TypographyTokens.bodyLarge,
    bodyMedium = TypographyTokens.bodyMedium,
    bodySmall = TypographyTokens.bodySmall,
    labelLarge = TypographyTokens.labelLarge,
    labelMedium = TypographyTokens.labelMedium,
    labelSmall = TypographyTokens.labelSmall
)

/**
 * Shape scale using AvanueUI design tokens.
 */
private val AvanueShapes = Shapes(
    extraSmall = RoundedCornerShape(ShapeTokens.xs),
    small = RoundedCornerShape(ShapeTokens.sm),
    medium = RoundedCornerShape(ShapeTokens.md),
    large = RoundedCornerShape(ShapeTokens.lg),
    extraLarge = RoundedCornerShape(ShapeTokens.xl)
)
