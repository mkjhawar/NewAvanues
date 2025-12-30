package com.augmentalis.avaelements.common.color

/**
 * Universal Color Utilities
 *
 * Platform-agnostic color manipulation functions shared across
 * Android, iOS, Desktop, and Web renderers.
 *
 * Eliminates duplicate color conversion and manipulation code
 * that was previously copy-pasted across platform renderers.
 */

// ═══════════════════════════════════════════════════════════════
// Universal Color Representation
// ═══════════════════════════════════════════════════════════════

/**
 * Platform-agnostic color representation using ARGB components.
 * Each platform renderer converts to native color types.
 */
data class UniversalColor(
    val alpha: Float,  // 0.0 - 1.0
    val red: Float,    // 0.0 - 1.0
    val green: Float,  // 0.0 - 1.0
    val blue: Float    // 0.0 - 1.0
) {
    companion object {
        val Transparent = UniversalColor(0f, 0f, 0f, 0f)
        val Black = UniversalColor(1f, 0f, 0f, 0f)
        val White = UniversalColor(1f, 1f, 1f, 1f)
        val Red = UniversalColor(1f, 1f, 0f, 0f)
        val Green = UniversalColor(1f, 0f, 1f, 0f)
        val Blue = UniversalColor(1f, 0f, 0f, 1f)

        /**
         * Create from hex string (supports #RGB, #RRGGBB, #AARRGGBB)
         */
        fun fromHex(hex: String): UniversalColor {
            val clean = hex.removePrefix("#")
            return when (clean.length) {
                3 -> {
                    // #RGB
                    val r = clean[0].toString().repeat(2).toInt(16) / 255f
                    val g = clean[1].toString().repeat(2).toInt(16) / 255f
                    val b = clean[2].toString().repeat(2).toInt(16) / 255f
                    UniversalColor(1f, r, g, b)
                }
                6 -> {
                    // #RRGGBB
                    val r = clean.substring(0, 2).toInt(16) / 255f
                    val g = clean.substring(2, 4).toInt(16) / 255f
                    val b = clean.substring(4, 6).toInt(16) / 255f
                    UniversalColor(1f, r, g, b)
                }
                8 -> {
                    // #AARRGGBB
                    val a = clean.substring(0, 2).toInt(16) / 255f
                    val r = clean.substring(2, 4).toInt(16) / 255f
                    val g = clean.substring(4, 6).toInt(16) / 255f
                    val b = clean.substring(6, 8).toInt(16) / 255f
                    UniversalColor(a, r, g, b)
                }
                else -> Black
            }
        }

        /**
         * Create from ARGB integer (0xAARRGGBB)
         */
        fun fromArgb(argb: Int): UniversalColor {
            val a = ((argb shr 24) and 0xFF) / 255f
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            return UniversalColor(a, r, g, b)
        }

        /**
         * Create from RGB integer (0xRRGGBB) - fully opaque
         */
        fun fromRgb(rgb: Int): UniversalColor {
            return fromArgb(rgb or 0xFF000000.toInt())
        }

        /**
         * Create from HSL values
         */
        fun fromHsl(hue: Float, saturation: Float, lightness: Float, alpha: Float = 1f): UniversalColor {
            val (r, g, b) = hslToRgb(hue, saturation, lightness)
            return UniversalColor(alpha, r, g, b)
        }
    }

    /**
     * Convert to ARGB integer
     */
    fun toArgb(): Int {
        val a = (alpha * 255).toInt() and 0xFF
        val r = (red * 255).toInt() and 0xFF
        val g = (green * 255).toInt() and 0xFF
        val b = (blue * 255).toInt() and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Convert to hex string (#AARRGGBB)
     */
    fun toHex(): String {
        val argb = toArgb()
        return "#${argb.toUInt().toString(16).padStart(8, '0').uppercase()}"
    }

    /**
     * Convert to HSL
     */
    fun toHsl(): Triple<Float, Float, Float> = rgbToHsl(red, green, blue)

    /**
     * Get luminance (0.0 - 1.0)
     */
    val luminance: Float
        get() = 0.2126f * red + 0.7152f * green + 0.0722f * blue

    /**
     * Check if color is dark (luminance < 0.5)
     */
    val isDark: Boolean
        get() = luminance < 0.5f

    /**
     * Check if color is light
     */
    val isLight: Boolean
        get() = !isDark
}

// ═══════════════════════════════════════════════════════════════
// Color Utilities Facade
// ═══════════════════════════════════════════════════════════════

/**
 * Facade providing unified access to color operations.
 *
 * Delegates to focused objects following Interface Segregation Principle:
 * - ColorManipulator: Lightening, darkening, saturation, mixing
 * - ColorAccessibility: WCAG compliance and contrast calculations
 * - ColorTheory: Hue shifting and color harmonies
 *
 * Maintains 100% backward compatibility while improving code organization.
 */
object ColorUtils {

    // ─────────────────────────────────────────────────────────────
    // Color Manipulation (delegates to ColorManipulator)
    // ─────────────────────────────────────────────────────────────

    /**
     * Lighten a color by a factor (0.0 - 1.0)
     * Previously duplicated across Android, Desktop, and Web renderers.
     */
    fun lighten(color: UniversalColor, factor: Float): UniversalColor =
        ColorManipulator.lighten(color, factor)

    /**
     * Darken a color by a factor (0.0 - 1.0)
     */
    fun darken(color: UniversalColor, factor: Float): UniversalColor =
        ColorManipulator.darken(color, factor)

    /**
     * Adjust saturation by a factor (-1.0 to 1.0)
     * Negative values desaturate, positive values saturate.
     */
    fun saturate(color: UniversalColor, factor: Float): UniversalColor =
        ColorManipulator.saturate(color, factor)

    /**
     * Adjust alpha/opacity
     */
    fun withAlpha(color: UniversalColor, alpha: Float): UniversalColor =
        ColorManipulator.withAlpha(color, alpha)

    /**
     * Mix two colors together
     * @param ratio 0.0 = 100% color1, 1.0 = 100% color2
     */
    fun mix(color1: UniversalColor, color2: UniversalColor, ratio: Float): UniversalColor =
        ColorManipulator.mix(color1, color2, ratio)

    /**
     * Invert a color
     */
    fun invert(color: UniversalColor): UniversalColor =
        ColorManipulator.invert(color)

    /**
     * Convert to grayscale
     */
    fun grayscale(color: UniversalColor): UniversalColor =
        ColorManipulator.grayscale(color)

    // ─────────────────────────────────────────────────────────────
    // Accessibility (delegates to ColorAccessibility)
    // ─────────────────────────────────────────────────────────────

    /**
     * Get contrasting foreground color (black or white)
     * Uses WCAG luminance calculation.
     */
    fun contrastingForeground(background: UniversalColor): UniversalColor =
        ColorAccessibility.contrastingForeground(background)

    /**
     * Calculate contrast ratio between two colors (WCAG)
     * Returns ratio from 1:1 to 21:1
     */
    fun contrastRatio(color1: UniversalColor, color2: UniversalColor): Float =
        ColorAccessibility.contrastRatio(color1, color2)

    /**
     * Check if contrast meets WCAG AA standard (4.5:1 for normal text)
     */
    fun meetsWcagAA(foreground: UniversalColor, background: UniversalColor): Boolean =
        ColorAccessibility.meetsWcagAA(foreground, background)

    /**
     * Check if contrast meets WCAG AAA standard (7:1 for normal text)
     */
    fun meetsWcagAAA(foreground: UniversalColor, background: UniversalColor): Boolean =
        ColorAccessibility.meetsWcagAAA(foreground, background)

    // ─────────────────────────────────────────────────────────────
    // Color Theory (delegates to ColorTheory)
    // ─────────────────────────────────────────────────────────────

    /**
     * Shift hue by degrees (0-360)
     */
    fun shiftHue(color: UniversalColor, degrees: Float): UniversalColor =
        ColorTheory.shiftHue(color, degrees)

    /**
     * Get complementary color (180° hue shift)
     */
    fun complementary(color: UniversalColor): UniversalColor =
        ColorTheory.complementary(color)

    /**
     * Get triadic colors (120° apart)
     */
    fun triadic(color: UniversalColor): List<UniversalColor> =
        ColorTheory.triadic(color)

    /**
     * Get analogous colors (30° apart)
     */
    fun analogous(color: UniversalColor): List<UniversalColor> =
        ColorTheory.analogous(color)
}

// ═══════════════════════════════════════════════════════════════
// HSL Conversion Utilities
// ═══════════════════════════════════════════════════════════════

/**
 * Convert RGB to HSL
 * @return Triple of (hue 0-360, saturation 0-1, lightness 0-1)
 */
internal fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f

    if (max == min) {
        return Triple(0f, 0f, l) // achromatic
    }

    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)

    val h = when (max) {
        r -> ((g - b) / d + if (g < b) 6f else 0f) * 60f
        g -> ((b - r) / d + 2f) * 60f
        else -> ((r - g) / d + 4f) * 60f
    }

    return Triple(h, s, l)
}

/**
 * Convert HSL to RGB
 * @return Triple of (red 0-1, green 0-1, blue 0-1)
 */
internal fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
    if (s == 0f) {
        return Triple(l, l, l) // achromatic
    }

    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    val hNorm = h / 360f

    fun hueToRgb(t: Float): Float {
        val t1 = when {
            t < 0f -> t + 1f
            t > 1f -> t - 1f
            else -> t
        }
        return when {
            t1 < 1f / 6f -> p + (q - p) * 6f * t1
            t1 < 1f / 2f -> q
            t1 < 2f / 3f -> p + (q - p) * (2f / 3f - t1) * 6f
            else -> p
        }
    }

    return Triple(
        hueToRgb(hNorm + 1f / 3f),
        hueToRgb(hNorm),
        hueToRgb(hNorm - 1f / 3f)
    )
}

// ═══════════════════════════════════════════════════════════════
// Extension Functions
// ═══════════════════════════════════════════════════════════════

fun UniversalColor.lighten(factor: Float) = ColorUtils.lighten(this, factor)
fun UniversalColor.darken(factor: Float) = ColorUtils.darken(this, factor)
fun UniversalColor.saturate(factor: Float) = ColorUtils.saturate(this, factor)
fun UniversalColor.withAlpha(alpha: Float) = ColorUtils.withAlpha(this, alpha)
fun UniversalColor.mix(other: UniversalColor, ratio: Float) = ColorUtils.mix(this, other, ratio)
fun UniversalColor.contrastingForeground() = ColorUtils.contrastingForeground(this)
fun UniversalColor.invert() = ColorUtils.invert(this)
fun UniversalColor.grayscale() = ColorUtils.grayscale(this)
fun UniversalColor.shiftHue(degrees: Float) = ColorUtils.shiftHue(this, degrees)
fun UniversalColor.complementary() = ColorUtils.complementary(this)
