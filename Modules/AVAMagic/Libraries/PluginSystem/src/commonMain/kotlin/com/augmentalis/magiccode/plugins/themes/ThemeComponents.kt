package com.augmentalis.avacode.plugins.themes

import kotlinx.serialization.Serializable

/**
 * Color palette for a theme.
 *
 * Supports hex, RGB, and RGBA color formats.
 */
@Serializable
data class ColorPalette(
    val primary: String,
    val secondary: String,
    val background: String,
    val surface: String? = null,
    val text: String,
    val textSecondary: String? = null,
    val accent: String? = null,
    val error: String? = null,
    val warning: String? = null,
    val success: String? = null,
    val info: String? = null,
    val border: String? = null,
    val divider: String? = null,
    val custom: Map<String, String> = emptyMap()
)

/**
 * Typography settings for a theme.
 */
@Serializable
data class Typography(
    val fontFamily: String,
    val fontSize: FontSizes,
    val fontWeight: FontWeights? = null,
    val lineHeight: LineHeights? = null,
    val letterSpacing: LetterSpacing? = null,
    /**
     * Custom font files to load from plugin assets.
     * Map of font family name to font file path (relative to plugin fonts directory).
     * Example: {"Roboto": "Roboto-Regular.ttf", "Roboto-Bold": "Roboto-Bold.ttf"}
     */
    val customFonts: Map<String, String> = emptyMap(),
    /**
     * Font fallback chain.
     * List of font families to try in order if the primary font is not available.
     * Example: ["SF Pro", "Helvetica Neue", "Arial", "sans-serif"]
     */
    val fontFallback: List<String> = emptyList()
)

/**
 * Font size definitions.
 */
@Serializable
data class FontSizes(
    val xs: Int? = null,
    val small: Int,
    val medium: Int,
    val large: Int,
    val xl: Int? = null,
    val xxl: Int? = null,
    val custom: Map<String, Int> = emptyMap()
)

/**
 * Font weight definitions.
 */
@Serializable
data class FontWeights(
    val light: Int? = 300,
    val regular: Int = 400,
    val medium: Int? = 500,
    val semibold: Int? = 600,
    val bold: Int = 700,
    val extrabold: Int? = 800
)

/**
 * Line height definitions.
 */
@Serializable
data class LineHeights(
    val tight: Double? = 1.2,
    val normal: Double = 1.5,
    val relaxed: Double? = 1.75,
    val loose: Double? = 2.0
)

/**
 * Letter spacing definitions.
 */
@Serializable
data class LetterSpacing(
    val tight: Double? = -0.5,
    val normal: Double = 0.0,
    val wide: Double? = 0.5,
    val wider: Double? = 1.0
)

/**
 * Spacing scale for margins, padding, gaps.
 */
@Serializable
data class Spacing(
    val xs: Int,
    val sm: Int,
    val md: Int,
    val lg: Int,
    val xl: Int,
    val xxl: Int? = null,
    val custom: Map<String, Int> = emptyMap()
)

/**
 * Visual effects like shadows, blurs, borders.
 */
@Serializable
data class Effects(
    val borderRadius: BorderRadius? = null,
    val shadows: Shadows? = null,
    val opacity: Opacity? = null,
    val blur: Blur? = null
)

/**
 * Border radius settings.
 */
@Serializable
data class BorderRadius(
    val none: Int = 0,
    val small: Int = 4,
    val medium: Int = 8,
    val large: Int = 12,
    val xl: Int? = 16,
    val full: Int? = 9999,
    val custom: Map<String, Int> = emptyMap()
)

/**
 * Shadow definitions.
 */
@Serializable
data class Shadows(
    val none: String = "none",
    val small: String,
    val medium: String,
    val large: String,
    val xl: String? = null,
    val custom: Map<String, String> = emptyMap()
)

/**
 * Opacity levels.
 */
@Serializable
data class Opacity(
    val transparent: Double = 0.0,
    val semiTransparent: Double = 0.5,
    val opaque: Double = 1.0,
    val custom: Map<String, Double> = emptyMap()
)

/**
 * Blur effects.
 */
@Serializable
data class Blur(
    val none: Int = 0,
    val small: Int = 4,
    val medium: Int = 8,
    val large: Int = 16,
    val custom: Map<String, Int> = emptyMap()
)

/**
 * Animation/transition settings.
 */
@Serializable
data class Animations(
    val duration: AnimationDuration? = null,
    val easing: AnimationEasing? = null
)

/**
 * Animation duration presets (in milliseconds).
 */
@Serializable
data class AnimationDuration(
    val instant: Int = 0,
    val fast: Int = 150,
    val normal: Int = 300,
    val slow: Int = 500,
    val custom: Map<String, Int> = emptyMap()
)

/**
 * Animation easing functions.
 */
@Serializable
data class AnimationEasing(
    val linear: String = "linear",
    val easeIn: String = "ease-in",
    val easeOut: String = "ease-out",
    val easeInOut: String = "ease-in-out",
    val custom: Map<String, String> = emptyMap()
)
