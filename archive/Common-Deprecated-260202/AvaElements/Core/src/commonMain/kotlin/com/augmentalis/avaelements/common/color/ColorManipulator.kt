package com.augmentalis.avaelements.common.color

/**
 * Color Manipulation Operations
 *
 * Focused object responsible for color transformations like
 * lightening, darkening, saturation, and mixing.
 *
 * SOLID Principle: Single Responsibility
 * This object handles only color manipulation operations,
 * separated from accessibility and color theory concerns.
 */
object ColorManipulator {

    /**
     * Lighten a color by a factor (0.0 - 1.0)
     *
     * @param color The color to lighten
     * @param factor The lightening factor (0.0 = no change, 1.0 = maximum lightening)
     * @return New color with increased lightness
     */
    fun lighten(color: UniversalColor, factor: Float): UniversalColor {
        val (h, s, l) = color.toHsl()
        val newL = (l + (1f - l) * factor.coerceIn(0f, 1f)).coerceIn(0f, 1f)
        return UniversalColor.fromHsl(h, s, newL, color.alpha)
    }

    /**
     * Darken a color by a factor (0.0 - 1.0)
     *
     * @param color The color to darken
     * @param factor The darkening factor (0.0 = no change, 1.0 = maximum darkening)
     * @return New color with decreased lightness
     */
    fun darken(color: UniversalColor, factor: Float): UniversalColor {
        val (h, s, l) = color.toHsl()
        val newL = (l * (1f - factor.coerceIn(0f, 1f))).coerceIn(0f, 1f)
        return UniversalColor.fromHsl(h, s, newL, color.alpha)
    }

    /**
     * Adjust saturation by a factor (-1.0 to 1.0)
     *
     * Negative values desaturate (move toward grayscale),
     * positive values saturate (increase color intensity).
     *
     * @param color The color to adjust
     * @param factor The saturation factor (-1.0 = grayscale, 0.0 = no change, 1.0 = maximum saturation)
     * @return New color with adjusted saturation
     */
    fun saturate(color: UniversalColor, factor: Float): UniversalColor {
        val (h, s, l) = color.toHsl()
        val newS = if (factor >= 0) {
            (s + (1f - s) * factor).coerceIn(0f, 1f)
        } else {
            (s * (1f + factor)).coerceIn(0f, 1f)
        }
        return UniversalColor.fromHsl(h, newS, l, color.alpha)
    }

    /**
     * Create a new color with adjusted alpha/opacity
     *
     * @param color The color to adjust
     * @param alpha The new alpha value (0.0 = transparent, 1.0 = opaque)
     * @return New color with adjusted alpha
     */
    fun withAlpha(color: UniversalColor, alpha: Float): UniversalColor {
        return color.copy(alpha = alpha.coerceIn(0f, 1f))
    }

    /**
     * Mix two colors together using linear interpolation
     *
     * @param color1 The first color
     * @param color2 The second color
     * @param ratio The mix ratio (0.0 = 100% color1, 1.0 = 100% color2)
     * @return New color blending color1 and color2
     */
    fun mix(color1: UniversalColor, color2: UniversalColor, ratio: Float): UniversalColor {
        val r = ratio.coerceIn(0f, 1f)
        return UniversalColor(
            alpha = color1.alpha + (color2.alpha - color1.alpha) * r,
            red = color1.red + (color2.red - color1.red) * r,
            green = color1.green + (color2.green - color1.green) * r,
            blue = color1.blue + (color2.blue - color1.blue) * r
        )
    }

    /**
     * Invert a color (complement each RGB channel)
     *
     * @param color The color to invert
     * @return New color with inverted RGB values
     */
    fun invert(color: UniversalColor): UniversalColor {
        return UniversalColor(
            alpha = color.alpha,
            red = 1f - color.red,
            green = 1f - color.green,
            blue = 1f - color.blue
        )
    }

    /**
     * Convert a color to grayscale using luminance
     *
     * @param color The color to convert
     * @return New grayscale color with same luminance
     */
    fun grayscale(color: UniversalColor): UniversalColor {
        val gray = color.luminance
        return UniversalColor(color.alpha, gray, gray, gray)
    }
}
