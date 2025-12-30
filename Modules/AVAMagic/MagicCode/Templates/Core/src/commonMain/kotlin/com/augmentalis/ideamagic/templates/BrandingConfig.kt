package com.augmentalis.magicui.templates

/**
 * Branding configuration for generated apps.
 *
 * Defines visual identity including app name, package, colors, logo, and theme.
 *
 * **Example**:
 * ```kotlin
 * val branding = brandingConfig {
 *     name = "TechGadgets Shop"
 *     package = "com.techgadgets.shop"
 *     colors {
 *         primary = Color(0xFF1976D2)
 *         secondary = Color(0xFFFFA726)
 *         accent = Color(0xFF4CAF50)
 *     }
 *     logo = "assets/logo.png"
 *     darkMode = true
 * }
 * ```
 *
 * @since 1.0.0
 */
data class BrandingConfig(
    val name: String,
    val package: String,
    val colors: ColorScheme = ColorScheme.DEFAULT,
    val logo: String? = null,
    val icon: String? = null,
    val fonts: FontConfig = FontConfig.DEFAULT,
    val darkMode: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "App name cannot be blank" }
        require(package.isNotBlank()) { "Package name cannot be blank" }
        require(package.matches(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*"))) {
            "Package name must be valid (e.g., com.example.app)"
        }
    }

    /**
     * Validate branding configuration.
     */
    fun validate() {
        colors.validate()
        fonts.validate()
    }

    companion object {
        /**
         * Default branding configuration.
         */
        val DEFAULT = BrandingConfig(
            name = "My App",
            package = "com.example.myapp"
        )
    }
}

/**
 * Color scheme configuration.
 *
 * Defines primary, secondary, accent colors and their variants.
 *
 * @property primary Primary brand color
 * @property primaryVariant Darker variant of primary color
 * @property secondary Secondary brand color
 * @property secondaryVariant Darker variant of secondary color
 * @property accent Accent color for highlights
 * @property background Background color
 * @property surface Surface color (cards, dialogs)
 * @property error Error color
 * @property onPrimary Text/icon color on primary background
 * @property onSecondary Text/icon color on secondary background
 * @property onBackground Text/icon color on background
 * @property onSurface Text/icon color on surface
 * @property onError Text/icon color on error background
 */
data class ColorScheme(
    val primary: Color,
    val primaryVariant: Color = primary.darken(0.2f),
    val secondary: Color,
    val secondaryVariant: Color = secondary.darken(0.2f),
    val accent: Color = secondary,
    val background: Color = Color.WHITE,
    val surface: Color = Color.WHITE,
    val error: Color = Color.RED,
    val onPrimary: Color = Color.WHITE,
    val onSecondary: Color = Color.WHITE,
    val onBackground: Color = Color.BLACK,
    val onSurface: Color = Color.BLACK,
    val onError: Color = Color.WHITE
) {
    /**
     * Validate color scheme.
     */
    fun validate() {
        // Ensure sufficient contrast for accessibility
        require(primary.contrastRatio(onPrimary) >= 4.5f) {
            "Insufficient contrast between primary ($primary) and onPrimary ($onPrimary). Minimum ratio: 4.5:1"
        }
        require(secondary.contrastRatio(onSecondary) >= 4.5f) {
            "Insufficient contrast between secondary ($secondary) and onSecondary ($onSecondary). Minimum ratio: 4.5:1"
        }
        require(background.contrastRatio(onBackground) >= 4.5f) {
            "Insufficient contrast between background ($background) and onBackground ($onBackground). Minimum ratio: 4.5:1"
        }
    }

    /**
     * Generate dark mode variant of this color scheme.
     */
    fun toDarkMode(): ColorScheme = copy(
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onBackground = Color.WHITE,
        onSurface = Color.WHITE
    )

    companion object {
        /**
         * Material Design 3 default color scheme (Blue/Orange).
         */
        val DEFAULT = ColorScheme(
            primary = Color(0xFF1976D2),        // Blue 700
            primaryVariant = Color(0xFF1565C0), // Blue 800
            secondary = Color(0xFFFFA726),      // Orange 400
            secondaryVariant = Color(0xFFFB8C00) // Orange 600
        )

        /**
         * Material Design 3 indigo/pink color scheme.
         */
        val INDIGO_PINK = ColorScheme(
            primary = Color(0xFF3F51B5),        // Indigo 500
            primaryVariant = Color(0xFF303F9F), // Indigo 700
            secondary = Color(0xFFE91E63),      // Pink 500
            secondaryVariant = Color(0xFFC2185B) // Pink 700
        )

        /**
         * Material Design 3 teal/amber color scheme.
         */
        val TEAL_AMBER = ColorScheme(
            primary = Color(0xFF009688),        // Teal 500
            primaryVariant = Color(0xFF00796B), // Teal 700
            secondary = Color(0xFFFFC107),      // Amber 500
            secondaryVariant = Color(0xFFFFA000) // Amber 700
        )

        /**
         * Material Design 3 purple/green color scheme.
         */
        val PURPLE_GREEN = ColorScheme(
            primary = Color(0xFF9C27B0),        // Purple 500
            primaryVariant = Color(0xFF7B1FA2), // Purple 700
            secondary = Color(0xFF4CAF50),      // Green 500
            secondaryVariant = Color(0xFF388E3C) // Green 700
        )
    }
}

/**
 * Font configuration.
 *
 * @property primaryFont Primary font family (e.g., "Roboto", "SF Pro")
 * @property secondaryFont Secondary font family (headings)
 * @property monoFont Monospace font (code blocks)
 */
data class FontConfig(
    val primaryFont: String = "Default",
    val secondaryFont: String = "Default",
    val monoFont: String = "Monospace"
) {
    /**
     * Validate font configuration.
     */
    fun validate() {
        require(primaryFont.isNotBlank()) { "Primary font cannot be blank" }
        require(secondaryFont.isNotBlank()) { "Secondary font cannot be blank" }
        require(monoFont.isNotBlank()) { "Mono font cannot be blank" }
    }

    companion object {
        val DEFAULT = FontConfig()
    }
}

/**
 * Represents a color in ARGB format.
 *
 * @property value ARGB color value (0xAARRGGBB)
 */
@JvmInline
value class Color(val value: Long) {
    init {
        require(value in 0x00000000..0xFFFFFFFF) {
            "Color value must be in range 0x00000000..0xFFFFFFFF"
        }
    }

    /**
     * Extract alpha component (0-255).
     */
    val alpha: Int
        get() = ((value shr 24) and 0xFF).toInt()

    /**
     * Extract red component (0-255).
     */
    val red: Int
        get() = ((value shr 16) and 0xFF).toInt()

    /**
     * Extract green component (0-255).
     */
    val green: Int
        get() = ((value shr 8) and 0xFF).toInt()

    /**
     * Extract blue component (0-255).
     */
    val blue: Int
        get() = (value and 0xFF).toInt()

    /**
     * Convert to hex string (e.g., "#FF1976D2").
     */
    fun toHexString(): String {
        return "#${value.toString(16).padStart(8, '0').uppercase()}"
    }

    /**
     * Calculate relative luminance (0.0 - 1.0).
     *
     * Used for contrast ratio calculations.
     */
    fun luminance(): Float {
        val r = red / 255f
        val g = green / 255f
        val b = blue / 255f

        val rLinear = if (r <= 0.03928f) r / 12.92f else Math.pow((r + 0.055) / 1.055, 2.4).toFloat()
        val gLinear = if (g <= 0.03928f) g / 12.92f else Math.pow((g + 0.055) / 1.055, 2.4).toFloat()
        val bLinear = if (b <= 0.03928f) b / 12.92f else Math.pow((b + 0.055) / 1.055, 2.4).toFloat()

        return 0.2126f * rLinear + 0.7152f * gLinear + 0.0722f * bLinear
    }

    /**
     * Calculate contrast ratio with another color.
     *
     * @param other Color to compare with
     * @return Contrast ratio (1.0 - 21.0)
     */
    fun contrastRatio(other: Color): Float {
        val lum1 = luminance()
        val lum2 = other.luminance()

        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)

        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Darken color by a factor (0.0 - 1.0).
     *
     * @param factor Amount to darken (0.0 = no change, 1.0 = black)
     * @return Darkened color
     */
    fun darken(factor: Float): Color {
        require(factor in 0f..1f) { "Factor must be in range 0.0..1.0" }

        val r = (red * (1f - factor)).toInt().coerceIn(0, 255)
        val g = (green * (1f - factor)).toInt().coerceIn(0, 255)
        val b = (blue * (1f - factor)).toInt().coerceIn(0, 255)

        return Color((alpha.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
    }

    /**
     * Lighten color by a factor (0.0 - 1.0).
     *
     * @param factor Amount to lighten (0.0 = no change, 1.0 = white)
     * @return Lightened color
     */
    fun lighten(factor: Float): Color {
        require(factor in 0f..1f) { "Factor must be in range 0.0..1.0" }

        val r = (red + (255 - red) * factor).toInt().coerceIn(0, 255)
        val g = (green + (255 - green) * factor).toInt().coerceIn(0, 255)
        val b = (blue + (255 - blue) * factor).toInt().coerceIn(0, 255)

        return Color((alpha.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
    }

    companion object {
        // Material Design colors
        val WHITE = Color(0xFFFFFFFF)
        val BLACK = Color(0xFF000000)
        val RED = Color(0xFFF44336)
        val PINK = Color(0xFFE91E63)
        val PURPLE = Color(0xFF9C27B0)
        val DEEP_PURPLE = Color(0xFF673AB7)
        val INDIGO = Color(0xFF3F51B5)
        val BLUE = Color(0xFF2196F3)
        val LIGHT_BLUE = Color(0xFF03A9F4)
        val CYAN = Color(0xFF00BCD4)
        val TEAL = Color(0xFF009688)
        val GREEN = Color(0xFF4CAF50)
        val LIGHT_GREEN = Color(0xFF8BC34A)
        val LIME = Color(0xFFCDDC39)
        val YELLOW = Color(0xFFFFEB3B)
        val AMBER = Color(0xFFFFC107)
        val ORANGE = Color(0xFFFF9800)
        val DEEP_ORANGE = Color(0xFFFF5722)
        val BROWN = Color(0xFF795548)
        val GREY = Color(0xFF9E9E9E)
        val BLUE_GREY = Color(0xFF607D8B)

        /**
         * Parse hex color string (e.g., "#FF1976D2" or "1976D2").
         */
        fun parseHex(hex: String): Color {
            val cleanHex = hex.removePrefix("#")
            require(cleanHex.length == 6 || cleanHex.length == 8) {
                "Hex color must be 6 or 8 characters (RRGGBB or AARRGGBB)"
            }

            val value = if (cleanHex.length == 6) {
                // Add full alpha
                "FF$cleanHex".toLong(16)
            } else {
                cleanHex.toLong(16)
            }

            return Color(value)
        }
    }
}

/**
 * DSL builder for branding configuration.
 */
@DslMarker
annotation class BrandingDsl

/**
 * Build a branding configuration.
 */
@BrandingDsl
fun brandingConfig(builder: BrandingConfigBuilder.() -> Unit): BrandingConfig {
    return BrandingConfigBuilder().apply(builder).build()
}

/**
 * Builder for branding configuration.
 */
@BrandingDsl
class BrandingConfigBuilder {
    var name: String = "My App"
    var package: String = "com.example.myapp"
    var colors: ColorScheme = ColorScheme.DEFAULT
    var logo: String? = null
    var icon: String? = null
    var fonts: FontConfig = FontConfig.DEFAULT
    var darkMode: Boolean = true

    /**
     * Configure color scheme.
     */
    fun colors(builder: ColorSchemeBuilder.() -> Unit) {
        colors = ColorSchemeBuilder().apply(builder).build()
    }

    /**
     * Configure fonts.
     */
    fun fonts(builder: FontConfigBuilder.() -> Unit) {
        fonts = FontConfigBuilder().apply(builder).build()
    }

    fun build(): BrandingConfig = BrandingConfig(
        name = name,
        package = package,
        colors = colors,
        logo = logo,
        icon = icon,
        fonts = fonts,
        darkMode = darkMode
    )
}

/**
 * Builder for color scheme.
 */
@BrandingDsl
class ColorSchemeBuilder {
    var primary: Color = Color(0xFF1976D2)
    var primaryVariant: Color? = null
    var secondary: Color = Color(0xFFFFA726)
    var secondaryVariant: Color? = null
    var accent: Color? = null
    var background: Color = Color.WHITE
    var surface: Color = Color.WHITE
    var error: Color = Color.RED
    var onPrimary: Color = Color.WHITE
    var onSecondary: Color = Color.WHITE
    var onBackground: Color = Color.BLACK
    var onSurface: Color = Color.BLACK
    var onError: Color = Color.WHITE

    fun build(): ColorScheme = ColorScheme(
        primary = primary,
        primaryVariant = primaryVariant ?: primary.darken(0.2f),
        secondary = secondary,
        secondaryVariant = secondaryVariant ?: secondary.darken(0.2f),
        accent = accent ?: secondary,
        background = background,
        surface = surface,
        error = error,
        onPrimary = onPrimary,
        onSecondary = onSecondary,
        onBackground = onBackground,
        onSurface = onSurface,
        onError = onError
    )
}

/**
 * Builder for font configuration.
 */
@BrandingDsl
class FontConfigBuilder {
    var primaryFont: String = "Default"
    var secondaryFont: String = "Default"
    var monoFont: String = "Monospace"

    fun build(): FontConfig = FontConfig(
        primaryFont = primaryFont,
        secondaryFont = secondaryFont,
        monoFont = monoFont
    )
}
