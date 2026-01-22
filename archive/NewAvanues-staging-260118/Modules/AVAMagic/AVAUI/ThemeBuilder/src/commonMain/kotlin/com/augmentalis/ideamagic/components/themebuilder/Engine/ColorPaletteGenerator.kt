package com.augmentalis.avamagic.components.themebuilder.Engine

import com.augmentalis.avaelements.core.types.Color
import kotlin.math.*

/**
 * Palette generation modes
 */
enum class PaletteMode {
    COMPLEMENTARY,      // Opposite on color wheel (180°)
    ANALOGOUS,          // Adjacent on color wheel (±30°)
    TRIADIC,            // Evenly spaced (120°)
    TETRADIC,           // Rectangle on color wheel (90°, 180°, 270°)
    SPLIT_COMPLEMENTARY, // Complementary ±30°
    MONOCHROMATIC       // Same hue, different lightness/saturation
}

/**
 * Color palette generator for creating harmonious color schemes
 */
class ColorPaletteGenerator {

    /**
     * Generate a color palette from a seed color
     *
     * @param seedColor Base color to generate palette from
     * @param mode Palette generation algorithm
     * @return List of colors in the palette
     */
    fun generatePalette(seedColor: Color, mode: PaletteMode): List<Color> {
        val hsv = rgbToHSV(seedColor)

        return when (mode) {
            PaletteMode.COMPLEMENTARY -> generateComplementary(hsv)
            PaletteMode.ANALOGOUS -> generateAnalogous(hsv)
            PaletteMode.TRIADIC -> generateTriadic(hsv)
            PaletteMode.TETRADIC -> generateTetradic(hsv)
            PaletteMode.SPLIT_COMPLEMENTARY -> generateSplitComplementary(hsv)
            PaletteMode.MONOCHROMATIC -> generateMonochromatic(hsv)
        }
    }

    /**
     * Generate complementary palette (2 colors)
     */
    private fun generateComplementary(hsv: HSV): List<Color> {
        return listOf(
            hsvToRGB(hsv),
            hsvToRGB(hsv.copy(hue = (hsv.hue + 180f) % 360f))
        )
    }

    /**
     * Generate analogous palette (3 colors)
     */
    private fun generateAnalogous(hsv: HSV): List<Color> {
        return listOf(
            hsvToRGB(hsv.copy(hue = (hsv.hue - 30f + 360f) % 360f)),
            hsvToRGB(hsv),
            hsvToRGB(hsv.copy(hue = (hsv.hue + 30f) % 360f))
        )
    }

    /**
     * Generate triadic palette (3 colors)
     */
    private fun generateTriadic(hsv: HSV): List<Color> {
        return listOf(
            hsvToRGB(hsv),
            hsvToRGB(hsv.copy(hue = (hsv.hue + 120f) % 360f)),
            hsvToRGB(hsv.copy(hue = (hsv.hue + 240f) % 360f))
        )
    }

    /**
     * Generate tetradic palette (4 colors)
     */
    private fun generateTetradic(hsv: HSV): List<Color> {
        return listOf(
            hsvToRGB(hsv),
            hsvToRGB(hsv.copy(hue = (hsv.hue + 90f) % 360f)),
            hsvToRGB(hsv.copy(hue = (hsv.hue + 180f) % 360f)),
            hsvToRGB(hsv.copy(hue = (hsv.hue + 270f) % 360f))
        )
    }

    /**
     * Generate split complementary palette (3 colors)
     */
    private fun generateSplitComplementary(hsv: HSV): List<Color> {
        val complementary = (hsv.hue + 180f) % 360f
        return listOf(
            hsvToRGB(hsv),
            hsvToRGB(hsv.copy(hue = (complementary - 30f + 360f) % 360f)),
            hsvToRGB(hsv.copy(hue = (complementary + 30f) % 360f))
        )
    }

    /**
     * Generate monochromatic palette (5 colors with varying lightness)
     */
    private fun generateMonochromatic(hsv: HSV): List<Color> {
        return listOf(
            hsvToRGB(hsv.copy(value = 0.3f)),
            hsvToRGB(hsv.copy(value = 0.5f)),
            hsvToRGB(hsv),
            hsvToRGB(hsv.copy(value = min(hsv.value + 0.2f, 1f))),
            hsvToRGB(hsv.copy(value = min(hsv.value + 0.4f, 1f)))
        )
    }

    /**
     * Generate tints (lighter variations) of a color
     */
    fun generateTints(color: Color, count: Int = 5): List<Color> {
        val hsv = rgbToHSV(color)
        val step = (1f - hsv.value) / count

        return (0 until count).map { i ->
            hsvToRGB(hsv.copy(value = min(hsv.value + step * i, 1f)))
        }
    }

    /**
     * Generate shades (darker variations) of a color
     */
    fun generateShades(color: Color, count: Int = 5): List<Color> {
        val hsv = rgbToHSV(color)
        val step = hsv.value / count

        return (0 until count).map { i ->
            hsvToRGB(hsv.copy(value = max(hsv.value - step * i, 0f)))
        }
    }

    /**
     * Generate tones (variations with saturation) of a color
     */
    fun generateTones(color: Color, count: Int = 5): List<Color> {
        val hsv = rgbToHSV(color)
        val step = hsv.saturation / count

        return (0 until count).map { i ->
            hsvToRGB(hsv.copy(saturation = max(hsv.saturation - step * i, 0f)))
        }
    }

    /**
     * Find contrasting color (for text on background)
     */
    fun findContrastingColor(backgroundColor: Color, preferDark: Boolean = true): Color {
        val luminance = calculateRelativeLuminance(backgroundColor)

        return if (luminance > 0.5f) {
            // Light background - use dark text
            if (preferDark) Color(0, 0, 0) else Color(33, 33, 33)
        } else {
            // Dark background - use light text
            Color(255, 255, 255)
        }
    }

    /**
     * Calculate relative luminance (WCAG formula)
     */
    private fun calculateRelativeLuminance(color: Color): Float {
        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f

        val rSrgb = if (r <= 0.03928f) r / 12.92f else ((r + 0.055f) / 1.055f).pow(2.4f)
        val gSrgb = if (g <= 0.03928f) g / 12.92f else ((g + 0.055f) / 1.055f).pow(2.4f)
        val bSrgb = if (b <= 0.03928f) b / 12.92f else ((b + 0.055f) / 1.055f).pow(2.4f)

        return 0.2126f * rSrgb + 0.7152f * gSrgb + 0.0722f * bSrgb
    }

    /**
     * Convert RGB color to HSV
     */
    private fun rgbToHSV(color: Color): HSV {
        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }.let { if (it < 0f) it + 360f else it }

        val saturation = if (max == 0f) 0f else delta / max
        val value = max

        return HSV(hue, saturation, value)
    }

    /**
     * Convert HSV color to RGB
     */
    private fun hsvToRGB(hsv: HSV): Color {
        val c = hsv.value * hsv.saturation
        val x = c * (1f - abs((hsv.hue / 60f) % 2f - 1f))
        val m = hsv.value - c

        val (r, g, b) = when {
            hsv.hue < 60f -> Triple(c, x, 0f)
            hsv.hue < 120f -> Triple(x, c, 0f)
            hsv.hue < 180f -> Triple(0f, c, x)
            hsv.hue < 240f -> Triple(0f, x, c)
            hsv.hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            ((r + m) * 255).toInt(),
            ((g + m) * 255).toInt(),
            ((b + m) * 255).toInt()
        )
    }

    /**
     * Lighten a color by a percentage
     */
    fun lighten(color: Color, percentage: Float): Color {
        require(percentage in 0f..100f) { "Percentage must be between 0 and 100" }

        val hsv = rgbToHSV(color)
        val newValue = min(hsv.value + (percentage / 100f), 1f)
        return hsvToRGB(hsv.copy(value = newValue))
    }

    /**
     * Darken a color by a percentage
     */
    fun darken(color: Color, percentage: Float): Color {
        require(percentage in 0f..100f) { "Percentage must be between 0 and 100" }

        val hsv = rgbToHSV(color)
        val newValue = max(hsv.value - (percentage / 100f), 0f)
        return hsvToRGB(hsv.copy(value = newValue))
    }

    /**
     * Saturate a color by a percentage
     */
    fun saturate(color: Color, percentage: Float): Color {
        require(percentage in 0f..100f) { "Percentage must be between 0 and 100" }

        val hsv = rgbToHSV(color)
        val newSaturation = min(hsv.saturation + (percentage / 100f), 1f)
        return hsvToRGB(hsv.copy(saturation = newSaturation))
    }

    /**
     * Desaturate a color by a percentage
     */
    fun desaturate(color: Color, percentage: Float): Color {
        require(percentage in 0f..100f) { "Percentage must be between 0 and 100" }

        val hsv = rgbToHSV(color)
        val newSaturation = max(hsv.saturation - (percentage / 100f), 0f)
        return hsvToRGB(hsv.copy(saturation = newSaturation))
    }

    /**
     * Mix two colors
     */
    fun mix(color1: Color, color2: Color, weight: Float = 0.5f): Color {
        require(weight in 0f..1f) { "Weight must be between 0 and 1" }

        val r = (color1.red * (1 - weight) + color2.red * weight).toInt()
        val g = (color1.green * (1 - weight) + color2.green * weight).toInt()
        val b = (color1.blue * (1 - weight) + color2.blue * weight).toInt()

        return Color(r, g, b)
    }

    /**
     * Invert a color
     */
    fun invert(color: Color): Color {
        return Color(
            255 - color.red,
            255 - color.green,
            255 - color.blue
        )
    }

    /**
     * Convert color to grayscale
     */
    fun grayscale(color: Color): Color {
        val gray = (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue).toInt()
        return Color(gray, gray, gray)
    }

    /**
     * Generate Material Design 3 color scheme from seed color
     */
    fun generateMaterial3Scheme(seedColor: Color, isDark: Boolean = false): Material3ColorScheme {
        val hsv = rgbToHSV(seedColor)

        val primary = seedColor
        val onPrimary = findContrastingColor(primary)

        // Primary container: Lighter/darker version of primary
        val primaryContainer = if (isDark) {
            darken(primary, 60f)
        } else {
            lighten(primary, 60f)
        }
        val onPrimaryContainer = findContrastingColor(primaryContainer)

        // Secondary: 30° shift on color wheel
        val secondary = hsvToRGB(hsv.copy(hue = (hsv.hue + 30f) % 360f))
        val onSecondary = findContrastingColor(secondary)
        val secondaryContainer = if (isDark) {
            darken(secondary, 60f)
        } else {
            lighten(secondary, 60f)
        }
        val onSecondaryContainer = findContrastingColor(secondaryContainer)

        // Tertiary: 60° shift on color wheel
        val tertiary = hsvToRGB(hsv.copy(hue = (hsv.hue + 60f) % 360f))
        val onTertiary = findContrastingColor(tertiary)
        val tertiaryContainer = if (isDark) {
            darken(tertiary, 60f)
        } else {
            lighten(tertiary, 60f)
        }
        val onTertiaryContainer = findContrastingColor(tertiaryContainer)

        // Error color (red hue)
        val error = hsvToRGB(HSV(0f, 0.8f, 0.7f))
        val onError = findContrastingColor(error)
        val errorContainer = if (isDark) {
            Color(140, 29, 24)
        } else {
            Color(249, 222, 220)
        }
        val onErrorContainer = findContrastingColor(errorContainer)

        // Neutral colors
        val background = if (isDark) {
            Color(28, 27, 31)
        } else {
            Color(254, 251, 254)
        }
        val onBackground = findContrastingColor(background)

        val surface = if (isDark) {
            Color(28, 27, 31)
        } else {
            Color(254, 251, 254)
        }
        val onSurface = findContrastingColor(surface)

        val surfaceVariant = if (isDark) {
            Color(73, 69, 79)
        } else {
            Color(231, 224, 236)
        }
        val onSurfaceVariant = findContrastingColor(surfaceVariant)

        val outline = if (isDark) {
            Color(147, 143, 153)
        } else {
            Color(121, 116, 126)
        }

        val outlineVariant = if (isDark) {
            Color(73, 69, 79)
        } else {
            Color(202, 196, 208)
        }

        return Material3ColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant
        )
    }
}

/**
 * HSV color representation
 */
private data class HSV(
    val hue: Float,         // 0-360
    val saturation: Float,  // 0-1
    val value: Float        // 0-1
)

/**
 * Material Design 3 color scheme
 */
data class Material3ColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color
)
