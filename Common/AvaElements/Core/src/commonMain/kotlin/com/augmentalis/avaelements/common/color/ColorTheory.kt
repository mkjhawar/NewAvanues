package com.augmentalis.avaelements.common.color

/**
 * Color Theory Operations
 *
 * Focused object responsible for color harmony and
 * color wheel-based operations.
 *
 * SOLID Principle: Single Responsibility
 * This object handles only color theory concerns,
 * separated from manipulation and accessibility operations.
 */
object ColorTheory {

    /**
     * Shift the hue of a color by the specified degrees
     *
     * The hue wheel is 360 degrees:
     * - 0° = Red
     * - 60° = Yellow
     * - 120° = Green
     * - 180° = Cyan
     * - 240° = Blue
     * - 300° = Magenta
     *
     * @param color The color to shift
     * @param degrees The degrees to shift (can be negative)
     * @return New color with shifted hue
     */
    fun shiftHue(color: UniversalColor, degrees: Float): UniversalColor {
        val (h, s, l) = color.toHsl()
        val newH = ((h + degrees) % 360f + 360f) % 360f
        return UniversalColor.fromHsl(newH, s, l, color.alpha)
    }

    /**
     * Get the complementary color (opposite on color wheel)
     *
     * Returns the color 180° opposite on the hue wheel,
     * creating maximum contrast while maintaining harmony.
     *
     * @param color The input color
     * @return Complementary color (180° hue shift)
     */
    fun complementary(color: UniversalColor): UniversalColor {
        return shiftHue(color, 180f)
    }

    /**
     * Get triadic color scheme (evenly spaced around color wheel)
     *
     * Returns three colors 120° apart on the hue wheel,
     * creating a balanced and vibrant color scheme.
     *
     * @param color The base color
     * @return List of three colors: [original, +120°, +240°]
     */
    fun triadic(color: UniversalColor): List<UniversalColor> {
        return listOf(
            color,
            shiftHue(color, 120f),
            shiftHue(color, 240f)
        )
    }

    /**
     * Get analogous color scheme (adjacent colors on wheel)
     *
     * Returns three colors 30° apart on the hue wheel,
     * creating a harmonious and cohesive color scheme.
     *
     * @param color The base color
     * @return List of three colors: [-30°, original, +30°]
     */
    fun analogous(color: UniversalColor): List<UniversalColor> {
        return listOf(
            shiftHue(color, -30f),
            color,
            shiftHue(color, 30f)
        )
    }
}
