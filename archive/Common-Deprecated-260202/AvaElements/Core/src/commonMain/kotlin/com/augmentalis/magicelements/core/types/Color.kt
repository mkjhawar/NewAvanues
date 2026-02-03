package com.augmentalis.avaelements.core.types

import kotlinx.serialization.Serializable

/**
 * Color representation for cross-platform use
 *
 * Supports RGB/RGBA, hex notation, and named colors.
 *
 * @since 2.0.0
 */
@Serializable
data class Color(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Float = 1.0f
) {
    init {
        require(red in 0..255) { "Red must be 0-255, got $red" }
        require(green in 0..255) { "Green must be 0-255, got $green" }
        require(blue in 0..255) { "Blue must be 0-255, got $blue" }
        require(alpha in 0.0f..1.0f) { "Alpha must be 0.0-1.0, got $alpha" }
    }

    /**
     * Convert to hex string representation
     *
     * @return Hex color string (e.g., "#FF0000FF")
     */
    fun toHex(): String {
        val a = (alpha * 255).toInt()
        val r = red.toString(16).padStart(2, '0')
        val g = green.toString(16).padStart(2, '0')
        val b = blue.toString(16).padStart(2, '0')
        val aHex = a.toString(16).padStart(2, '0')
        return "#$r$g$b$aHex".uppercase()
    }

    /**
     * Create color with adjusted alpha
     *
     * @param alpha New alpha value (0.0-1.0)
     * @return New color with updated alpha
     */
    fun withAlpha(alpha: Float): Color {
        return copy(alpha = alpha)
    }

    /**
     * Create color with adjusted opacity
     *
     * @param opacity Opacity percentage (0-100)
     * @return New color with updated opacity
     */
    fun withOpacity(opacity: Float): Color {
        require(opacity in 0.0f..100.0f) { "Opacity must be 0-100" }
        return copy(alpha = opacity / 100f)
    }

    companion object {
        /**
         * Create color from hex string
         *
         * @param value Hex string (e.g., "#FF0000", "#FF0000FF")
         * @return Color instance
         * @throws IllegalArgumentException if hex format is invalid
         */
        fun hex(value: String): Color {
            val hex = value.removePrefix("#")
            require(hex.length == 6 || hex.length == 8) {
                "Invalid hex color format: $value (expected #RRGGBB or #RRGGBBAA)"
            }

            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            val a = if (hex.length == 8) hex.substring(6, 8).toInt(16) / 255f else 1.0f

            return Color(r, g, b, a)
        }

        /**
         * Create color from RGB values
         *
         * @param r Red (0-255)
         * @param g Green (0-255)
         * @param b Blue (0-255)
         * @return Color instance
         */
        fun rgb(r: Int, g: Int, b: Int) = Color(r, g, b, 1.0f)

        /**
         * Create color from RGBA values
         *
         * @param r Red (0-255)
         * @param g Green (0-255)
         * @param b Blue (0-255)
         * @param a Alpha (0.0-1.0)
         * @return Color instance
         */
        fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r, g, b, a)

        // Named colors
        val Transparent = Color(0, 0, 0, 0.0f)
        val Black = Color(0, 0, 0)
        val White = Color(255, 255, 255)
        val Red = Color(255, 0, 0)
        val Green = Color(0, 255, 0)
        val Blue = Color(0, 0, 255)
        val Yellow = Color(255, 255, 0)
        val Cyan = Color(0, 255, 255)
        val Magenta = Color(255, 0, 255)
        val Gray = Color(128, 128, 128)
        val DarkGray = Color(64, 64, 64)
        val LightGray = Color(192, 192, 192)
    }
}
