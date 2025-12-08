package com.augmentalis.avaelements.common.properties

/**
 * Color Property Extractor
 *
 * Handles extraction and parsing of color values from property maps.
 * Part of the SOLID refactoring of PropertyExtractor.
 *
 * Responsibilities:
 * - Color extraction as ARGB integers
 * - Hex color parsing (#RGB, #ARGB, #RRGGBB, #AARRGGBB)
 * - Named color resolution (black, white, red, etc.)
 *
 * Supported color formats:
 * - Integer: Direct ARGB value
 * - Hex: #RGB, #ARGB, #RRGGBB, #AARRGGBB
 * - Named: "black", "white", "red", "green", "blue", etc.
 *
 * SRP: Single responsibility - color extraction and parsing only
 * ISP: Clients depend only on color extraction methods
 */
object ColorPropertyExtractor {

    // ─────────────────────────────────────────────────────────────
    // Color Extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Extract color as ARGB int
     * Supports: hex string, int value, or color name
     *
     * @param props Property map
     * @param key Property key
     * @param default Default color (opaque black)
     * @return ARGB color value
     */
    fun getColorArgb(
        props: Map<String, Any?>,
        key: String,
        default: Int = 0xFF000000.toInt()
    ): Int {
        return when (val value = props[key]) {
            is Int -> value
            is Long -> value.toInt()
            is String -> parseColorString(value) ?: default
            else -> default
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Color Parsing
    // ─────────────────────────────────────────────────────────────

    /**
     * Parse color string to ARGB int
     *
     * Supports:
     * - Named colors: "black", "white", "red", etc.
     * - Hex #RGB: #F00 -> #FFFF0000
     * - Hex #ARGB: #8F00 -> #88FF0000
     * - Hex #RRGGBB: #FF0000 -> #FFFF0000
     * - Hex #AARRGGBB: #88FF0000 -> #88FF0000
     *
     * @param color Color string to parse
     * @return ARGB int value or null if invalid
     */
    private fun parseColorString(color: String): Int? {
        val clean = color.trim().lowercase()

        // Named colors
        val named = NAMED_COLORS[clean]
        if (named != null) return named

        // Hex colors
        val hex = clean.removePrefix("#")
        return try {
            when (hex.length) {
                3 -> {
                    // #RGB -> #FFRRGGBB
                    val r = hex[0].toString().repeat(2).toInt(16)
                    val g = hex[1].toString().repeat(2).toInt(16)
                    val b = hex[2].toString().repeat(2).toInt(16)
                    (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
                4 -> {
                    // #ARGB -> #AARRGGBB
                    val a = hex[0].toString().repeat(2).toInt(16)
                    val r = hex[1].toString().repeat(2).toInt(16)
                    val g = hex[2].toString().repeat(2).toInt(16)
                    val b = hex[3].toString().repeat(2).toInt(16)
                    (a shl 24) or (r shl 16) or (g shl 8) or b
                }
                6 -> {
                    // #RRGGBB -> #FFRRGGBB
                    hex.toLong(16).toInt() or (0xFF shl 24)
                }
                8 -> {
                    // #AARRGGBB
                    hex.toLong(16).toInt()
                }
                else -> null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Named Colors
    // ─────────────────────────────────────────────────────────────

    /**
     * Map of named colors to ARGB values
     * Supports common CSS/HTML color names
     */
    private val NAMED_COLORS = mapOf(
        "transparent" to 0x00000000,
        "black" to 0xFF000000.toInt(),
        "white" to 0xFFFFFFFF.toInt(),
        "red" to 0xFFFF0000.toInt(),
        "green" to 0xFF00FF00.toInt(),
        "blue" to 0xFF0000FF.toInt(),
        "yellow" to 0xFFFFFF00.toInt(),
        "cyan" to 0xFF00FFFF.toInt(),
        "magenta" to 0xFFFF00FF.toInt(),
        "gray" to 0xFF808080.toInt(),
        "grey" to 0xFF808080.toInt(),
        "orange" to 0xFFFFA500.toInt(),
        "purple" to 0xFF800080.toInt(),
        "pink" to 0xFFFFC0CB.toInt(),
        "brown" to 0xFFA52A2A.toInt()
    )
}
