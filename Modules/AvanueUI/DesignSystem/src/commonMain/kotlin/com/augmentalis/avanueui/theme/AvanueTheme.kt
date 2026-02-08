package com.augmentalis.avanueui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.augmentalis.avanueui.tokens.DisplayProfile

/**
 * CompositionLocals for the three theme-variable systems.
 */
val LocalAvanueColors = staticCompositionLocalOf<AvanueColorScheme> { OceanColors }
val LocalAvanueGlass = staticCompositionLocalOf<AvanueGlassScheme> { OceanGlass }
val LocalDisplayProfile = staticCompositionLocalOf { DisplayProfile.PHONE }

/**
 * Provides AvanueUI theme (colors + glass + display profile) to the composition tree.
 *
 * When [displayProfile] specifies a non-1.0 density scale, [LocalDensity] is overridden
 * so that ALL dp/sp values automatically adapt. This means existing code using
 * `SpacingTokens.md` (16dp) will render at 12dp physical on GLASS_COMPACT (0.75x)
 * with zero consumer code changes.
 *
 * Usage:
 * ```
 * AvanueThemeProvider(
 *     colors = SunsetColors,
 *     glass = SunsetGlass,
 *     displayProfile = DisplayProfile.GLASS_COMPACT
 * ) {
 *     MyScreen()
 * }
 * ```
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
        content = content
    )
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
