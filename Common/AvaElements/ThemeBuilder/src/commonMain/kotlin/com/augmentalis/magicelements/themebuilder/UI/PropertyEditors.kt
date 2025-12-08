package com.augmentalis.avaelements.themebuilder.UI

import com.augmentalis.avaelements.core.Color
import com.augmentalis.avaelements.core.Font
import com.augmentalis.avaelements.core.FontWeight
import com.augmentalis.avaelements.themebuilder.Engine.ColorPaletteGenerator
import com.augmentalis.avaelements.themebuilder.Engine.PaletteMode

/**
 * Property editor components for theme customization
 */
object PropertyEditors {

    /**
     * Color picker with palette generation
     */
    class ColorPicker {
        private val paletteGenerator = ColorPaletteGenerator()

        /**
         * Current color value
         */
        var currentColor: Color = Color.White
            private set

        /**
         * Color history (recently used colors)
         */
        private val colorHistory = mutableListOf<Color>()
        private val maxHistorySize = 10

        /**
         * Update the current color
         */
        fun setColor(color: Color) {
            currentColor = color
            addToHistory(color)
        }

        /**
         * Set color from hex string
         */
        fun setColorFromHex(hex: String): Result<Unit> {
            return try {
                val color = Color.hex(hex)
                setColor(color)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(IllegalArgumentException("Invalid hex color: $hex"))
            }
        }

        /**
         * Set color from RGB values
         */
        fun setColorFromRGB(r: Int, g: Int, b: Int): Result<Unit> {
            return try {
                require(r in 0..255) { "Red must be 0-255" }
                require(g in 0..255) { "Green must be 0-255" }
                require(b in 0..255) { "Blue must be 0-255" }

                val color = Color.rgb(r, g, b)
                setColor(color)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Set color from HSV values
         */
        fun setColorFromHSV(h: Float, s: Float, v: Float): Result<Unit> {
            return try {
                require(h in 0f..360f) { "Hue must be 0-360" }
                require(s in 0f..1f) { "Saturation must be 0-1" }
                require(v in 0f..1f) { "Value must be 0-1" }

                // HSV to RGB conversion
                val c = v * s
                val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
                val m = v - c

                val (r, g, b) = when {
                    h < 60f -> Triple(c, x, 0f)
                    h < 120f -> Triple(x, c, 0f)
                    h < 180f -> Triple(0f, c, x)
                    h < 240f -> Triple(0f, x, c)
                    h < 300f -> Triple(x, 0f, c)
                    else -> Triple(c, 0f, x)
                }

                val color = Color.rgb(
                    ((r + m) * 255).toInt(),
                    ((g + m) * 255).toInt(),
                    ((b + m) * 255).toInt()
                )
                setColor(color)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Generate palette from current color
         */
        fun generatePalette(mode: PaletteMode): List<Color> {
            return paletteGenerator.generatePalette(currentColor, mode)
        }

        /**
         * Lighten current color
         */
        fun lighten(percentage: Float): Color {
            val lightened = paletteGenerator.lighten(currentColor, percentage)
            setColor(lightened)
            return lightened
        }

        /**
         * Darken current color
         */
        fun darken(percentage: Float): Color {
            val darkened = paletteGenerator.darken(currentColor, percentage)
            setColor(darkened)
            return darkened
        }

        /**
         * Get color history
         */
        fun getHistory(): List<Color> = colorHistory.toList()

        /**
         * Add color to history
         */
        private fun addToHistory(color: Color) {
            colorHistory.remove(color) // Remove if already exists
            colorHistory.add(0, color)
            if (colorHistory.size > maxHistorySize) {
                colorHistory.removeAt(maxHistorySize)
            }
        }

        /**
         * Get hex string for current color
         */
        fun toHex(): String {
            return "#${currentColor.red.toString(16).padStart(2, '0')}" +
                    "${currentColor.green.toString(16).padStart(2, '0')}" +
                    "${currentColor.blue.toString(16).padStart(2, '0')}"
        }

        /**
         * Get RGB values
         */
        fun toRGB(): Triple<Int, Int, Int> {
            return Triple(currentColor.red, currentColor.green, currentColor.blue)
        }
    }

    /**
     * Font selector with preview
     */
    class FontSelector {
        /**
         * Available font families
         */
        private val availableFonts = listOf(
            "Roboto",
            "Inter",
            "SF Pro",
            "Segoe UI",
            "Arial",
            "Helvetica",
            "Open Sans",
            "Lato",
            "Montserrat",
            "Poppins",
            "Raleway",
            "Ubuntu",
            "Nunito",
            "Playfair Display",
            "Merriweather"
        )

        /**
         * Current font
         */
        var currentFont: Font = Font("Roboto", 14f, FontWeight.Normal)
            private set

        /**
         * Set font family
         */
        fun setFamily(family: String): Result<Unit> {
            return if (family in availableFonts) {
                currentFont = currentFont.copy(family = family)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Font family not available: $family"))
            }
        }

        /**
         * Set font size
         */
        fun setSize(size: Float): Result<Unit> {
            return if (size in 8f..96f) {
                currentFont = currentFont.copy(size = size)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Font size must be 8-96sp"))
            }
        }

        /**
         * Set font weight
         */
        fun setWeight(weight: FontWeight) {
            currentFont = currentFont.copy(weight = weight)
        }

        /**
         * Get available fonts
         */
        fun getAvailableFonts(): List<String> = availableFonts

        /**
         * Get available font weights
         */
        fun getAvailableWeights(): List<FontWeight> = FontWeight.values().toList()

        /**
         * Get font preview text
         */
        fun getPreviewText(text: String = "The quick brown fox jumps over the lazy dog"): String {
            return text
        }
    }

    /**
     * Spacing editor with visual scale
     */
    class SpacingEditor {
        /**
         * Spacing preset patterns
         */
        enum class SpacingPattern {
            LINEAR,          // Even increments
            FIBONACCI,       // Fibonacci sequence
            GEOMETRIC,       // Geometric progression
            MATERIAL_3       // Material Design 3 spacing
        }

        /**
         * Current spacing values
         */
        data class SpacingValues(
            var xs: Float = 4f,
            var sm: Float = 8f,
            var md: Float = 16f,
            var lg: Float = 24f,
            var xl: Float = 32f,
            var xxl: Float = 48f
        )

        val values = SpacingValues()

        /**
         * Apply spacing pattern
         */
        fun applyPattern(pattern: SpacingPattern, baseValue: Float = 8f) {
            when (pattern) {
                SpacingPattern.LINEAR -> {
                    values.xs = baseValue * 0.5f
                    values.sm = baseValue
                    values.md = baseValue * 2f
                    values.lg = baseValue * 3f
                    values.xl = baseValue * 4f
                    values.xxl = baseValue * 6f
                }
                SpacingPattern.FIBONACCI -> {
                    values.xs = baseValue * 0.5f  // 4
                    values.sm = baseValue         // 8
                    values.md = baseValue * 2f    // 16
                    values.lg = baseValue * 3f    // 24
                    values.xl = baseValue * 5f    // 40
                    values.xxl = baseValue * 8f   // 64
                }
                SpacingPattern.GEOMETRIC -> {
                    values.xs = baseValue
                    values.sm = baseValue * 1.5f
                    values.md = baseValue * 2.25f
                    values.lg = baseValue * 3.375f
                    values.xl = baseValue * 5.0625f
                    values.xxl = baseValue * 7.59375f
                }
                SpacingPattern.MATERIAL_3 -> {
                    values.xs = 4f
                    values.sm = 8f
                    values.md = 16f
                    values.lg = 24f
                    values.xl = 32f
                    values.xxl = 48f
                }
            }
        }

        /**
         * Set individual spacing value
         */
        fun setValue(key: String, value: Float): Result<Unit> {
            return if (value in 0f..100f) {
                when (key) {
                    "xs" -> values.xs = value
                    "sm" -> values.sm = value
                    "md" -> values.md = value
                    "lg" -> values.lg = value
                    "xl" -> values.xl = value
                    "xxl" -> values.xxl = value
                    else -> return Result.failure(IllegalArgumentException("Invalid spacing key: $key"))
                }
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Spacing must be 0-100dp"))
            }
        }

        /**
         * Validate spacing progression
         */
        fun validate(): List<String> {
            val warnings = mutableListOf<String>()

            val spacingList = listOf(values.xs, values.sm, values.md, values.lg, values.xl, values.xxl)
            spacingList.zipWithNext().forEach { (current, next) ->
                if (next <= current) {
                    warnings.add("Spacing values should progress in ascending order")
                }
            }

            return warnings
        }
    }

    /**
     * Shape editor for corner radius
     */
    class ShapeEditor {
        /**
         * Shape preset patterns
         */
        enum class ShapePattern {
            ROUNDED,    // Gentle curves
            SHARP,      // Minimal curves
            PILL,       // Maximum curves
            CUSTOM      // User-defined
        }

        /**
         * Current shape values
         */
        data class ShapeValues(
            var extraSmall: Float = 4f,
            var small: Float = 8f,
            var medium: Float = 12f,
            var large: Float = 16f,
            var extraLarge: Float = 28f
        )

        val values = ShapeValues()

        /**
         * Apply shape pattern
         */
        fun applyPattern(pattern: ShapePattern) {
            when (pattern) {
                ShapePattern.ROUNDED -> {
                    values.extraSmall = 4f
                    values.small = 8f
                    values.medium = 12f
                    values.large = 16f
                    values.extraLarge = 28f
                }
                ShapePattern.SHARP -> {
                    values.extraSmall = 0f
                    values.small = 2f
                    values.medium = 4f
                    values.large = 6f
                    values.extraLarge = 8f
                }
                ShapePattern.PILL -> {
                    values.extraSmall = 12f
                    values.small = 16f
                    values.medium = 24f
                    values.large = 32f
                    values.extraLarge = 50f
                }
                ShapePattern.CUSTOM -> {
                    // Leave current values
                }
            }
        }

        /**
         * Set individual shape value
         */
        fun setValue(key: String, value: Float): Result<Unit> {
            return if (value in 0f..50f) {
                when (key) {
                    "extraSmall" -> values.extraSmall = value
                    "small" -> values.small = value
                    "medium" -> values.medium = value
                    "large" -> values.large = value
                    "extraLarge" -> values.extraLarge = value
                    else -> return Result.failure(IllegalArgumentException("Invalid shape key: $key"))
                }
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Corner radius must be 0-50dp"))
            }
        }
    }

    /**
     * Elevation editor for shadow levels
     */
    class ElevationEditor {
        /**
         * Current elevation values
         */
        data class ElevationValues(
            var level0: Float = 0f,
            var level1: Float = 1f,
            var level2: Float = 3f,
            var level3: Float = 6f,
            var level4: Float = 8f,
            var level5: Float = 12f
        )

        val values = ElevationValues()

        /**
         * Set individual elevation value
         */
        fun setValue(level: Int, value: Float): Result<Unit> {
            return if (value in 0f..24f) {
                when (level) {
                    0 -> values.level0 = value
                    1 -> values.level1 = value
                    2 -> values.level2 = value
                    3 -> values.level3 = value
                    4 -> values.level4 = value
                    5 -> values.level5 = value
                    else -> return Result.failure(IllegalArgumentException("Elevation level must be 0-5"))
                }
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Elevation must be 0-24dp"))
            }
        }

        /**
         * Apply Material Design 3 elevation pattern
         */
        fun applyMaterial3Pattern() {
            values.level0 = 0f
            values.level1 = 1f
            values.level2 = 3f
            values.level3 = 6f
            values.level4 = 8f
            values.level5 = 12f
        }
    }

    /**
     * Number slider editor
     */
    class NumberSlider(
        val min: Float,
        val max: Float,
        val step: Float = 1f,
        initialValue: Float = min
    ) {
        var value: Float = initialValue
            private set

        /**
         * Set value with validation
         */
        fun setValue(newValue: Float): Result<Unit> {
            return if (newValue in min..max) {
                value = newValue
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Value must be between $min and $max"))
            }
        }

        /**
         * Increment value by step
         */
        fun increment() {
            val newValue = (value + step).coerceAtMost(max)
            value = newValue
        }

        /**
         * Decrement value by step
         */
        fun decrement() {
            val newValue = (value - step).coerceAtLeast(min)
            value = newValue
        }

        /**
         * Get value as percentage (0-100)
         */
        fun getPercentage(): Float {
            return ((value - min) / (max - min)) * 100f
        }

        /**
         * Set value from percentage (0-100)
         */
        fun setFromPercentage(percentage: Float): Result<Unit> {
            val newValue = min + ((max - min) * (percentage / 100f))
            return setValue(newValue)
        }
    }
}
