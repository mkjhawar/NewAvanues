package com.augmentalis.avanues.avaui.theme

import kotlinx.serialization.Serializable

/**
 * Complete theme configuration for the AvaUI system.
 *
 * A theme defines the visual appearance of all UI components through:
 * - **Palette**: Color definitions
 * - **Typography**: Text styles
 * - **Spacing**: Layout spacing rules
 * - **Effects**: Visual effects (shadows, blur, etc.)
 *
 * ## Example Usage
 *
 * ```kotlin
 * val darkTheme = ThemeConfig(
 *     name = "Dark",
 *     palette = ThemePalette(
 *         primary = "#007AFF",
 *         secondary = "#5AC8FA",
 *         background = "#000000",
 *         surface = "#1C1C1E",
 *         error = "#FF3B30"
 *     ),
 *     typography = ThemeTypography(
 *         h1 = TextStyle(size = 28f, weight = "bold"),
 *         body = TextStyle(size = 16f, weight = "regular")
 *     )
 * )
 * ```
 *
 * @since 3.1.0
 */
@Serializable
data class ThemeConfig(
    val name: String,
    val palette: ThemePalette,
    val typography: ThemeTypography = ThemeTypography(),
    val spacing: ThemeSpacing = ThemeSpacing(),
    val effects: ThemeEffects = ThemeEffects()
)

/**
 * Color palette for theme.
 *
 * All colors are hex strings (#RRGGBB or #AARRGGBB).
 */
@Serializable
data class ThemePalette(
    val primary: String,
    val secondary: String,
    val background: String,
    val surface: String,
    val error: String,
    val onPrimary: String = "#FFFFFF",
    val onSecondary: String = "#FFFFFF",
    val onBackground: String = "#FFFFFF",
    val onSurface: String = "#FFFFFF",
    val onError: String = "#FFFFFF"
)

/**
 * Typography definitions for theme.
 */
@Serializable
data class ThemeTypography(
    val h1: TextStyle = TextStyle(size = 28f, weight = "bold"),
    val h2: TextStyle = TextStyle(size = 22f, weight = "bold"),
    val body: TextStyle = TextStyle(size = 16f, weight = "regular"),
    val caption: TextStyle = TextStyle(size = 12f, weight = "regular")
)

/**
 * Text style definition.
 */
@Serializable
data class TextStyle(
    val size: Float,
    val weight: String,
    val fontFamily: String = "system"
)

/**
 * Spacing rules for theme (in dp).
 */
@Serializable
data class ThemeSpacing(
    val xs: Float = 4f,
    val sm: Float = 8f,
    val md: Float = 16f,
    val lg: Float = 24f,
    val xl: Float = 32f
)

/**
 * Visual effects for theme.
 */
@Serializable
data class ThemeEffects(
    val shadowEnabled: Boolean = true,
    val blurRadius: Float = 8f,
    val elevation: Float = 4f
)
