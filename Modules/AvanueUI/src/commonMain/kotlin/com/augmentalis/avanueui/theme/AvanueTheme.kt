package com.augmentalis.avanueui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.augmentalis.avanueui.display.DisplayProfile
import com.augmentalis.avanueui.tokens.ShapeTokens
import com.augmentalis.avanueui.tokens.TypographyTokens

// ===== CompositionLocals =====

val LocalAvanueColors = staticCompositionLocalOf<AvanueColorScheme> { HydraColors }
val LocalAvanueGlass = staticCompositionLocalOf<AvanueGlassScheme> { HydraGlass }
val LocalAvanueWater = staticCompositionLocalOf<AvanueWaterScheme> { HydraWater }
val LocalDisplayProfile = staticCompositionLocalOf { DisplayProfile.PHONE }
val LocalMaterialMode = staticCompositionLocalOf { MaterialMode.Water }
val LocalAppearanceIsDark = staticCompositionLocalOf { true }

/**
 * Unified theme provider for the Avanues ecosystem (v5.1).
 *
 * Sets up both Material3 theming (for M3 components like TextField, Button) and
 * AvanueUI custom theming (colors, glass, water, display profile, density scaling).
 * The [isDark] parameter controls the M3 ColorScheme bridge (dark vs light) and
 * is exposed via [AvanueTheme.isDark] for components that need appearance-aware logic.
 *
 * When [displayProfile] specifies a non-1.0 density scale, [LocalDensity] is overridden
 * so that ALL dp/sp values automatically adapt.
 */
@Composable
@Suppress("DEPRECATION")
fun AvanueThemeProvider(
    colors: AvanueColorScheme = HydraColors,
    glass: AvanueGlassScheme = HydraGlass,
    water: AvanueWaterScheme = HydraWater,
    displayProfile: DisplayProfile = DisplayProfile.PHONE,
    materialMode: MaterialMode = MaterialMode.Water,
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density * displayProfile.densityScale,
        fontScale = currentDensity.fontScale * displayProfile.fontScale
    )

    // Keep static AvanueModuleAccents in sync so non-Compose consumers
    // (Canvas overlay, services) always get the current theme colors.
    SideEffect {
        AvanueModuleAccents.setGlobalColors(
            accent = colors.primary,
            onAccent = colors.onPrimary,
            accentMuted = colors.primaryLight
        )
    }

    CompositionLocalProvider(
        LocalAvanueColors provides colors,
        LocalAvanueGlass provides glass,
        LocalAvanueWater provides water,
        LocalDisplayProfile provides displayProfile,
        LocalMaterialMode provides materialMode,
        LocalAppearanceIsDark provides isDark,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = colors.toM3ColorScheme(isDark),
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

    val water: AvanueWaterScheme
        @Composable get() = LocalAvanueWater.current

    val displayProfile: DisplayProfile
        @Composable get() = LocalDisplayProfile.current

    val materialMode: MaterialMode
        @Composable get() = LocalMaterialMode.current

    val isDark: Boolean
        @Composable get() = LocalAppearanceIsDark.current

    /**
     * Get resolved accent colors for a module.
     * Returns custom override if set, otherwise derives from current theme.
     */
    @Composable
    fun moduleAccent(moduleId: String): ModuleAccent {
        val colors = LocalAvanueColors.current
        val override = AvanueModuleAccents.get(moduleId)
        return if (override.isCustom) override else ModuleAccent(
            accent = colors.primary,
            onAccent = colors.onPrimary,
            accentMuted = colors.primaryLight,
            isCustom = false
        )
    }
}

// ===== Material3 Integration =====

/**
 * Maps AvanueColorScheme to Material3 ColorScheme.
 * Uses darkColorScheme or lightColorScheme based on appearance mode.
 * This allows M3 components (TextField, Button, etc.) to use AvanueUI colors.
 */
private fun AvanueColorScheme.toM3ColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
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
    ) else lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
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
