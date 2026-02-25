package com.augmentalis.avanueui.themebridge

import com.augmentalis.voiceos.colorpicker.ColorRGBA

/**
 * Color conversion utilities for theme migration.
 *
 * Provides lossless conversion between Android ARGB Int colors and hex strings
 * using the ColorPicker library's ColorRGBA for reliable conversions.
 *
 * ## Usage
 *
 * ```kotlin
 * val utils = ColorConversionUtils()
 *
 * // Int → Hex
 * val hex = utils.intToHex(0xFFFF5722.toInt())  // "#FF5722FF"
 *
 * // Hex → Int
 * val argb = utils.hexToInt("#FF5722")  // 0xFFFF5722
 *
 * // Round-trip (lossless)
 * val original = 0x80FF5722.toInt()
 * val restored = utils.hexToInt(utils.intToHex(original))
 * assert(original == restored)
 * ```
 *
 * @since 3.1.0
 */
class ColorConversionUtils {

    /**
     * Convert Android ARGB Int to hex string.
     *
     * Uses ColorRGBA.toHexString() for reliable conversion.
     *
     * @param argbInt Android color Int (ARGB format: 0xAARRGGBB)
     * @param includeAlpha Include alpha channel in output (default: true)
     * @param uppercase Use uppercase hex digits (default: true)
     * @return Hex string (e.g., "#FF5722FF" or "#FF5722")
     *
     * @throws IllegalArgumentException if argbInt is invalid
     */
    fun intToHex(
        argbInt: Int,
        includeAlpha: Boolean = true,
        uppercase: Boolean = true
    ): String {
        return try {
            val color = ColorRGBA.fromARGBInt(argbInt)
            color.toHexString(includeAlpha = includeAlpha, uppercase = uppercase)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid ARGB color: 0x${argbInt.toString(16)}", e)
        }
    }

    /**
     * Convert hex string to Android ARGB Int.
     *
     * Uses ColorRGBA.fromHexString() for reliable parsing.
     *
     * Supports formats:
     * - #RGB (3 digits, expands to #RRGGBB with alpha FF)
     * - #RGBA (4 digits, with alpha)
     * - #RRGGBB (6 digits, alpha FF implied)
     * - #RRGGBBAA (8 digits, alpha at end)
     *
     * @param hexString Hex color string (with or without #)
     * @return Android color Int (ARGB format: 0xAARRGGBB)
     *
     * @throws IllegalArgumentException if hexString is invalid
     */
    fun hexToInt(hexString: String): Int {
        return try {
            val color = ColorRGBA.fromHexString(hexString)
            color.toARGBInt()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid hex color: $hexString", e)
        }
    }

    /**
     * Validate hex string format.
     *
     * Checks if the string is a valid hex color without throwing exceptions.
     *
     * @param hexString Hex string to validate
     * @return true if valid, false otherwise
     */
    fun isValidHex(hexString: String): Boolean {
        return try {
            ColorRGBA.fromHexString(hexString)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Ensure color has full opacity (alpha = 255).
     *
     * Useful when converting from formats that don't support alpha
     * and you need to ensure full opacity for theme colors.
     *
     * @param argbInt Android color Int
     * @return Color with alpha channel set to 255 (0xFF)
     */
    fun ensureOpaque(argbInt: Int): Int {
        return argbInt or 0xFF000000.toInt()
    }

    /**
     * Extract alpha channel from ARGB Int.
     *
     * @param argbInt Android color Int
     * @return Alpha value (0-255)
     */
    fun extractAlpha(argbInt: Int): Int {
        return (argbInt shr 24) and 0xFF
    }

    /**
     * Extract red channel from ARGB Int.
     *
     * @param argbInt Android color Int
     * @return Red value (0-255)
     */
    fun extractRed(argbInt: Int): Int {
        return (argbInt shr 16) and 0xFF
    }

    /**
     * Extract green channel from ARGB Int.
     *
     * @param argbInt Android color Int
     * @return Green value (0-255)
     */
    fun extractGreen(argbInt: Int): Int {
        return (argbInt shr 8) and 0xFF
    }

    /**
     * Extract blue channel from ARGB Int.
     *
     * @param argbInt Android color Int
     * @return Blue value (0-255)
     */
    fun extractBlue(argbInt: Int): Int {
        return argbInt and 0xFF
    }

    /**
     * Create ARGB Int from individual channels.
     *
     * @param alpha Alpha value (0-255)
     * @param red Red value (0-255)
     * @param green Green value (0-255)
     * @param blue Blue value (0-255)
     * @return Android color Int (ARGB format)
     *
     * @throws IllegalArgumentException if any channel is out of range
     */
    fun createARGB(alpha: Int, red: Int, green: Int, blue: Int): Int {
        require(alpha in 0..255) { "Alpha must be 0-255, got $alpha" }
        require(red in 0..255) { "Red must be 0-255, got $red" }
        require(green in 0..255) { "Green must be 0-255, got $green" }
        require(blue in 0..255) { "Blue must be 0-255, got $blue" }

        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    /**
     * Blend two colors with specified alpha.
     *
     * @param foreground Foreground color (ARGB Int)
     * @param background Background color (ARGB Int)
     * @param alpha Blend alpha (0.0 = background only, 1.0 = foreground only)
     * @return Blended color (ARGB Int)
     */
    fun blend(foreground: Int, background: Int, alpha: Float = 0.5f): Int {
        val fg = ColorRGBA.fromARGBInt(foreground)
        val bg = ColorRGBA.fromARGBInt(background)

        // Use ColorRGBA's lerp for accurate blending
        val blended = bg.lerp(fg, alpha)
        return blended.toARGBInt()
    }

    /**
     * Darken color by percentage.
     *
     * @param argbInt Android color Int
     * @param amount Darken amount (0.0-1.0, where 0.5 = 50% darker)
     * @return Darkened color (ARGB Int)
     */
    fun darken(argbInt: Int, amount: Float): Int {
        val color = ColorRGBA.fromARGBInt(argbInt)
        val darkened = color.darken(amount)
        return darkened.toARGBInt()
    }

    /**
     * Lighten color by percentage.
     *
     * @param argbInt Android color Int
     * @param amount Lighten amount (0.0-1.0, where 0.5 = 50% lighter)
     * @return Lightened color (ARGB Int)
     */
    fun lighten(argbInt: Int, amount: Float): Int {
        val color = ColorRGBA.fromARGBInt(argbInt)
        val lightened = color.lighten(amount)
        return lightened.toARGBInt()
    }

    companion object {
        /**
         * Common Android color constants as ARGB Ints.
         */
        object AndroidColors {
            const val TRANSPARENT = 0x00000000
            const val BLACK = 0xFF000000.toInt()
            const val WHITE = 0xFFFFFFFF.toInt()
            const val RED = 0xFFFF0000.toInt()
            const val GREEN = 0xFF00FF00.toInt()
            const val BLUE = 0xFF0000FF.toInt()
            const val YELLOW = 0xFFFFFF00.toInt()
            const val CYAN = 0xFF00FFFF.toInt()
            const val MAGENTA = 0xFFFF00FF.toInt()
        }
    }
}
