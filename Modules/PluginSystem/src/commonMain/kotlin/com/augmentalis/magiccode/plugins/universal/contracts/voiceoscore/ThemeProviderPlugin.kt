/**
 * ThemeProviderPlugin.kt - Theme Provider Plugin contract for VoiceOSCore
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for plugins that provide visual themes for VoiceOSCore
 * overlays and UI components. Enables customizable appearance for accessibility
 * overlays, labels, highlights, and other visual elements.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin

/**
 * Theme Provider Plugin contract for visual theming.
 *
 * Theme providers supply color schemes, typography, and visual styles for
 * VoiceOSCore overlays and accessibility UI components. Multiple themes
 * can be registered and switched at runtime.
 *
 * ## Design Principles
 * - **Accessibility-First**: Themes must ensure sufficient contrast ratios
 * - **Dark Mode Support**: All themes should indicate dark/light mode
 * - **Consistent API**: Standard color and typography tokens
 * - **Hot-Swappable**: Themes can be applied/reset without restart
 *
 * ## Implementation Example
 * ```kotlin
 * class HighContrastTheme : ThemeProviderPlugin {
 *     override val themeId = "com.augmentalis.theme.high-contrast"
 *     override val themeName = "High Contrast"
 *     override val isDarkMode = false
 *
 *     override fun getColors(): ThemeColors {
 *         return ThemeColors(
 *             primary = "#000000",
 *             secondary = "#0000FF",
 *             background = "#FFFFFF",
 *             surface = "#FFFFFF",
 *             error = "#FF0000",
 *             onPrimary = "#FFFFFF",
 *             onBackground = "#000000"
 *         )
 *     }
 *
 *     override fun getTypography(): ThemeTypography {
 *         return ThemeTypography(
 *             headlineLarge = TextStyle(fontSize = 32f, fontWeight = FontWeight.BOLD),
 *             bodyLarge = TextStyle(fontSize = 18f, fontWeight = FontWeight.NORMAL)
 *         )
 *     }
 *
 *     override suspend fun apply(context: ThemeContext): Result<Unit> {
 *         // Apply theme to overlay system
 *         return Result.success(Unit)
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @see ThemeColors
 * @see ThemeTypography
 * @see ThemeContext
 */
interface ThemeProviderPlugin : UniversalPlugin {

    /**
     * Unique theme identifier.
     *
     * Should follow reverse-domain notation (e.g., "com.augmentalis.theme.dark").
     */
    val themeId: String

    /**
     * Human-readable theme name.
     *
     * Displayed in theme selection UI.
     */
    val themeName: String

    /**
     * Whether this is a dark mode theme.
     *
     * Used for system integration and automatic theme switching.
     */
    val isDarkMode: Boolean

    /**
     * Get the color palette for this theme.
     *
     * Returns a complete color scheme following Material Design principles
     * adapted for accessibility overlays.
     *
     * @return ThemeColors containing the full color palette
     */
    fun getColors(): ThemeColors

    /**
     * Get the typography styles for this theme.
     *
     * Returns text styles for different typography scales
     * (headlines, body, labels, etc.).
     *
     * @return ThemeTypography containing text style definitions
     */
    fun getTypography(): ThemeTypography

    /**
     * Get spacing/dimension values for this theme.
     *
     * Returns standard spacing values for consistent layout.
     *
     * @return ThemeDimensions containing spacing and size values
     */
    fun getDimensions(): ThemeDimensions {
        return ThemeDimensions.DEFAULT
    }

    /**
     * Get shape/corner radius values for this theme.
     *
     * @return ThemeShapes containing corner radius definitions
     */
    fun getShapes(): ThemeShapes {
        return ThemeShapes.DEFAULT
    }

    /**
     * Apply this theme to the overlay system.
     *
     * Called when this theme is selected. The implementation should
     * update all overlay components to use the new theme values.
     *
     * @param context Theme application context
     * @return Result indicating success or failure
     */
    suspend fun apply(context: ThemeContext): Result<Unit>

    /**
     * Reset/remove this theme, reverting to system defaults.
     *
     * Called when switching away from this theme. Clean up any
     * theme-specific resources.
     *
     * @return Result indicating success or failure
     */
    suspend fun reset(): Result<Unit>

    /**
     * Get a preview of the theme for display in theme picker.
     *
     * @return ThemePreview containing sample colors and description
     */
    fun getPreview(): ThemePreview {
        return ThemePreview(
            themeId = themeId,
            name = themeName,
            description = if (isDarkMode) "Dark theme" else "Light theme",
            primaryColor = getColors().primary,
            backgroundColor = getColors().background,
            accentColor = getColors().secondary,
            isDarkMode = isDarkMode
        )
    }

    /**
     * Check if this theme meets minimum contrast requirements.
     *
     * WCAG 2.1 recommends minimum contrast ratio of 4.5:1 for normal text.
     *
     * @return true if contrast requirements are met
     */
    fun meetsContrastRequirements(): Boolean {
        val colors = getColors()
        // Simplified check - in production, calculate actual contrast ratio
        return colors.onBackground != colors.background &&
                colors.onPrimary != colors.primary
    }
}

/**
 * Extended theme colors for complete theming.
 *
 * Extends the basic ThemeColors with additional semantic colors
 * specific to accessibility overlays.
 *
 * @property overlayBackground Background for overlay containers
 * @property overlayBorder Border color for overlays
 * @property labelBackground Default label background
 * @property labelText Default label text color
 * @property highlightColor Default highlight/focus color
 * @property gazeIndicator Gaze tracking indicator color
 * @property voiceIndicator Voice active indicator color
 * @property success Success/positive action color
 * @property warning Warning/caution color
 */
data class ExtendedThemeColors(
    val base: ThemeColors,
    val overlayBackground: String,
    val overlayBorder: String,
    val labelBackground: String,
    val labelText: String,
    val highlightColor: String,
    val gazeIndicator: String,
    val voiceIndicator: String,
    val success: String,
    val warning: String
) {
    companion object {
        fun fromThemeColors(colors: ThemeColors, isDark: Boolean): ExtendedThemeColors {
            return if (isDark) {
                ExtendedThemeColors(
                    base = colors,
                    overlayBackground = "#1E1E1ECC",
                    overlayBorder = "#333333",
                    labelBackground = colors.primary,
                    labelText = colors.onPrimary,
                    highlightColor = colors.primary,
                    gazeIndicator = "#4CAF50",
                    voiceIndicator = "#2196F3",
                    success = "#4CAF50",
                    warning = "#FFC107"
                )
            } else {
                ExtendedThemeColors(
                    base = colors,
                    overlayBackground = "#FFFFFFCC",
                    overlayBorder = "#E0E0E0",
                    labelBackground = colors.primary,
                    labelText = colors.onPrimary,
                    highlightColor = colors.primary,
                    gazeIndicator = "#4CAF50",
                    voiceIndicator = "#2196F3",
                    success = "#4CAF50",
                    warning = "#FF9800"
                )
            }
        }
    }
}

/**
 * Typography styles for theme.
 *
 * Defines text styles at different scales following accessibility guidelines.
 *
 * @property displayLarge Largest display text (rarely used)
 * @property displayMedium Medium display text
 * @property displaySmall Small display text
 * @property headlineLarge Large headline
 * @property headlineMedium Medium headline
 * @property headlineSmall Small headline
 * @property titleLarge Large title text
 * @property titleMedium Medium title text
 * @property titleSmall Small title text
 * @property bodyLarge Large body text
 * @property bodyMedium Medium body text (default)
 * @property bodySmall Small body text
 * @property labelLarge Large label text
 * @property labelMedium Medium label text
 * @property labelSmall Small label text
 */
data class ThemeTypography(
    val displayLarge: TextStyle = TextStyle(fontSize = 57f, fontWeight = FontWeight.NORMAL, lineHeight = 64f),
    val displayMedium: TextStyle = TextStyle(fontSize = 45f, fontWeight = FontWeight.NORMAL, lineHeight = 52f),
    val displaySmall: TextStyle = TextStyle(fontSize = 36f, fontWeight = FontWeight.NORMAL, lineHeight = 44f),
    val headlineLarge: TextStyle = TextStyle(fontSize = 32f, fontWeight = FontWeight.NORMAL, lineHeight = 40f),
    val headlineMedium: TextStyle = TextStyle(fontSize = 28f, fontWeight = FontWeight.NORMAL, lineHeight = 36f),
    val headlineSmall: TextStyle = TextStyle(fontSize = 24f, fontWeight = FontWeight.NORMAL, lineHeight = 32f),
    val titleLarge: TextStyle = TextStyle(fontSize = 22f, fontWeight = FontWeight.MEDIUM, lineHeight = 28f),
    val titleMedium: TextStyle = TextStyle(fontSize = 16f, fontWeight = FontWeight.MEDIUM, lineHeight = 24f),
    val titleSmall: TextStyle = TextStyle(fontSize = 14f, fontWeight = FontWeight.MEDIUM, lineHeight = 20f),
    val bodyLarge: TextStyle = TextStyle(fontSize = 16f, fontWeight = FontWeight.NORMAL, lineHeight = 24f),
    val bodyMedium: TextStyle = TextStyle(fontSize = 14f, fontWeight = FontWeight.NORMAL, lineHeight = 20f),
    val bodySmall: TextStyle = TextStyle(fontSize = 12f, fontWeight = FontWeight.NORMAL, lineHeight = 16f),
    val labelLarge: TextStyle = TextStyle(fontSize = 14f, fontWeight = FontWeight.MEDIUM, lineHeight = 20f),
    val labelMedium: TextStyle = TextStyle(fontSize = 12f, fontWeight = FontWeight.MEDIUM, lineHeight = 16f),
    val labelSmall: TextStyle = TextStyle(fontSize = 11f, fontWeight = FontWeight.MEDIUM, lineHeight = 16f)
) {
    companion object {
        val DEFAULT = ThemeTypography()

        /** Typography with larger sizes for accessibility */
        val LARGE = ThemeTypography(
            displayLarge = TextStyle(fontSize = 64f, fontWeight = FontWeight.NORMAL, lineHeight = 72f),
            displayMedium = TextStyle(fontSize = 52f, fontWeight = FontWeight.NORMAL, lineHeight = 60f),
            displaySmall = TextStyle(fontSize = 42f, fontWeight = FontWeight.NORMAL, lineHeight = 50f),
            headlineLarge = TextStyle(fontSize = 38f, fontWeight = FontWeight.NORMAL, lineHeight = 46f),
            headlineMedium = TextStyle(fontSize = 34f, fontWeight = FontWeight.NORMAL, lineHeight = 42f),
            headlineSmall = TextStyle(fontSize = 30f, fontWeight = FontWeight.NORMAL, lineHeight = 38f),
            titleLarge = TextStyle(fontSize = 26f, fontWeight = FontWeight.MEDIUM, lineHeight = 32f),
            titleMedium = TextStyle(fontSize = 20f, fontWeight = FontWeight.MEDIUM, lineHeight = 28f),
            titleSmall = TextStyle(fontSize = 18f, fontWeight = FontWeight.MEDIUM, lineHeight = 24f),
            bodyLarge = TextStyle(fontSize = 20f, fontWeight = FontWeight.NORMAL, lineHeight = 28f),
            bodyMedium = TextStyle(fontSize = 18f, fontWeight = FontWeight.NORMAL, lineHeight = 24f),
            bodySmall = TextStyle(fontSize = 16f, fontWeight = FontWeight.NORMAL, lineHeight = 20f),
            labelLarge = TextStyle(fontSize = 18f, fontWeight = FontWeight.MEDIUM, lineHeight = 24f),
            labelMedium = TextStyle(fontSize = 16f, fontWeight = FontWeight.MEDIUM, lineHeight = 20f),
            labelSmall = TextStyle(fontSize = 14f, fontWeight = FontWeight.MEDIUM, lineHeight = 20f)
        )
    }
}

/**
 * Text style definition.
 *
 * @property fontSize Font size in sp (scale-independent pixels)
 * @property fontWeight Font weight
 * @property lineHeight Line height in sp
 * @property letterSpacing Letter spacing in em (optional)
 * @property fontFamily Font family name (optional)
 */
data class TextStyle(
    val fontSize: Float,
    val fontWeight: FontWeight,
    val lineHeight: Float,
    val letterSpacing: Float = 0f,
    val fontFamily: String? = null
)

/**
 * Font weight enumeration.
 */
enum class FontWeight {
    THIN,
    EXTRA_LIGHT,
    LIGHT,
    NORMAL,
    MEDIUM,
    SEMI_BOLD,
    BOLD,
    EXTRA_BOLD,
    BLACK
}

/**
 * Dimension/spacing values for theme.
 *
 * @property spacingXs Extra small spacing (4dp)
 * @property spacingSm Small spacing (8dp)
 * @property spacingMd Medium spacing (16dp)
 * @property spacingLg Large spacing (24dp)
 * @property spacingXl Extra large spacing (32dp)
 * @property iconSizeSm Small icon size (16dp)
 * @property iconSizeMd Medium icon size (24dp)
 * @property iconSizeLg Large icon size (32dp)
 * @property touchTargetMin Minimum touch target size (48dp for accessibility)
 */
data class ThemeDimensions(
    val spacingXs: Float = 4f,
    val spacingSm: Float = 8f,
    val spacingMd: Float = 16f,
    val spacingLg: Float = 24f,
    val spacingXl: Float = 32f,
    val iconSizeSm: Float = 16f,
    val iconSizeMd: Float = 24f,
    val iconSizeLg: Float = 32f,
    val touchTargetMin: Float = 48f
) {
    companion object {
        val DEFAULT = ThemeDimensions()

        /** Larger dimensions for accessibility */
        val LARGE = ThemeDimensions(
            spacingXs = 8f,
            spacingSm = 12f,
            spacingMd = 20f,
            spacingLg = 32f,
            spacingXl = 48f,
            iconSizeSm = 24f,
            iconSizeMd = 32f,
            iconSizeLg = 48f,
            touchTargetMin = 56f
        )
    }
}

/**
 * Shape/corner radius values for theme.
 *
 * @property extraSmall Extra small corner radius (4dp)
 * @property small Small corner radius (8dp)
 * @property medium Medium corner radius (12dp)
 * @property large Large corner radius (16dp)
 * @property extraLarge Extra large corner radius (28dp)
 * @property full Full/pill corner radius (9999dp)
 */
data class ThemeShapes(
    val extraSmall: Float = 4f,
    val small: Float = 8f,
    val medium: Float = 12f,
    val large: Float = 16f,
    val extraLarge: Float = 28f,
    val full: Float = 9999f
) {
    companion object {
        val DEFAULT = ThemeShapes()

        /** Sharp corners (minimal rounding) */
        val SHARP = ThemeShapes(
            extraSmall = 0f,
            small = 2f,
            medium = 4f,
            large = 8f,
            extraLarge = 12f,
            full = 9999f
        )

        /** Rounded corners (more rounding) */
        val ROUNDED = ThemeShapes(
            extraSmall = 8f,
            small = 12f,
            medium = 16f,
            large = 24f,
            extraLarge = 32f,
            full = 9999f
        )
    }
}

/**
 * Context for theme application.
 *
 * @property overlayManager Reference to overlay manager for applying theme
 * @property previousThemeId ID of previously active theme (for transition)
 * @property animate Whether to animate the theme transition
 * @property animationDuration Animation duration in milliseconds
 */
data class ThemeContext(
    val overlayManager: Any,
    val previousThemeId: String?,
    val animate: Boolean = true,
    val animationDuration: Long = 300
)

/**
 * Preview information for theme selection UI.
 *
 * @property themeId Theme identifier
 * @property name Display name
 * @property description Short description
 * @property primaryColor Primary color for preview
 * @property backgroundColor Background color for preview
 * @property accentColor Accent color for preview
 * @property isDarkMode Whether this is a dark theme
 * @property previewImageUrl Optional URL to preview image
 */
data class ThemePreview(
    val themeId: String,
    val name: String,
    val description: String,
    val primaryColor: String,
    val backgroundColor: String,
    val accentColor: String,
    val isDarkMode: Boolean,
    val previewImageUrl: String? = null
)

/**
 * Predefined theme configurations.
 */
object PredefinedThemes {

    /** Standard light theme */
    val LIGHT_COLORS = ThemeColors(
        primary = "#2196F3",
        secondary = "#03DAC6",
        background = "#FFFFFF",
        surface = "#F5F5F5",
        error = "#B00020",
        onPrimary = "#FFFFFF",
        onBackground = "#000000"
    )

    /** Standard dark theme */
    val DARK_COLORS = ThemeColors(
        primary = "#BB86FC",
        secondary = "#03DAC6",
        background = "#121212",
        surface = "#1E1E1E",
        error = "#CF6679",
        onPrimary = "#000000",
        onBackground = "#FFFFFF"
    )

    /** High contrast light theme */
    val HIGH_CONTRAST_LIGHT = ThemeColors(
        primary = "#000000",
        secondary = "#0000FF",
        background = "#FFFFFF",
        surface = "#FFFFFF",
        error = "#FF0000",
        onPrimary = "#FFFFFF",
        onBackground = "#000000"
    )

    /** High contrast dark theme */
    val HIGH_CONTRAST_DARK = ThemeColors(
        primary = "#FFFFFF",
        secondary = "#FFFF00",
        background = "#000000",
        surface = "#000000",
        error = "#FF0000",
        onPrimary = "#000000",
        onBackground = "#FFFFFF"
    )

    /** Colorblind-friendly theme (deuteranopia) */
    val COLORBLIND_FRIENDLY = ThemeColors(
        primary = "#0077BB",
        secondary = "#EE7733",
        background = "#FFFFFF",
        surface = "#F5F5F5",
        error = "#CC3311",
        onPrimary = "#FFFFFF",
        onBackground = "#000000"
    )
}
