package com.augmentalis.avamagic.components.themebuilder.UI

import com.augmentalis.avamagic.core.*
import com.augmentalis.avamagic.core.types.Color
import com.augmentalis.avamagic.core.types.Spacing
import com.augmentalis.avamagic.core.types.CornerRadius
import com.augmentalis.avamagic.components.themebuilder.State.ThemeBuilderStateManager

/**
 * Property inspector for editing theme properties
 * This provides a structured interface for modifying all aspects of a theme
 */
class PropertyInspector(
    private val stateManager: ThemeBuilderStateManager
) {
    /**
     * Property category
     */
    enum class PropertyCategory {
        COLOR_SCHEME,
        TYPOGRAPHY,
        SHAPES,
        SPACING,
        ELEVATION,
        MATERIAL_EFFECTS
    }

    /**
     * Property definition
     */
    data class PropertyDef(
        val name: String,
        val displayName: String,
        val category: PropertyCategory,
        val type: PropertyType,
        val description: String,
        val getValue: (Theme) -> Any,
        val setValue: (Theme, Any) -> Theme
    )

    /**
     * Property type
     */
    sealed class PropertyType {
        data class ColorType(val allowAlpha: Boolean = true) : PropertyType()
        data class NumberType(val min: Float? = null, val max: Float? = null, val step: Float = 1f) : PropertyType()
        data class TextType(val maxLength: Int? = null) : PropertyType()
        data class EnumType(val values: List<String>) : PropertyType()
        data class FontType(val availableFonts: List<String>) : PropertyType()
    }

    /**
     * All editable properties
     */
    val properties: List<PropertyDef> = buildPropertyList()

    private fun buildPropertyList(): List<PropertyDef> {
        return listOf(
            // ==================== Color Scheme ====================
            PropertyDef(
                name = "colorScheme.primary",
                displayName = "Primary Color",
                category = PropertyCategory.COLOR_SCHEME,
                type = PropertyType.ColorType(),
                description = "Main brand color used for primary actions and highlights",
                getValue = { it.colorScheme.primary },
                setValue = { theme, value ->
                    theme.copy(colorScheme = theme.colorScheme.copy(primary = value as Color))
                }
            ),
            PropertyDef(
                name = "colorScheme.onPrimary",
                displayName = "On Primary Color",
                category = PropertyCategory.COLOR_SCHEME,
                type = PropertyType.ColorType(),
                description = "Color for text/icons on primary color",
                getValue = { it.colorScheme.onPrimary },
                setValue = { theme, value ->
                    theme.copy(colorScheme = theme.colorScheme.copy(onPrimary = value as Color))
                }
            ),
            PropertyDef(
                name = "colorScheme.secondary",
                displayName = "Secondary Color",
                category = PropertyCategory.COLOR_SCHEME,
                type = PropertyType.ColorType(),
                description = "Secondary brand color for accents",
                getValue = { it.colorScheme.secondary },
                setValue = { theme, value ->
                    theme.copy(colorScheme = theme.colorScheme.copy(secondary = value as Color))
                }
            ),
            PropertyDef(
                name = "colorScheme.surface",
                displayName = "Surface Color",
                category = PropertyCategory.COLOR_SCHEME,
                type = PropertyType.ColorType(allowAlpha = true),
                description = "Background color for cards and surfaces",
                getValue = { it.colorScheme.surface },
                setValue = { theme, value ->
                    theme.copy(colorScheme = theme.colorScheme.copy(surface = value as Color))
                }
            ),
            PropertyDef(
                name = "colorScheme.background",
                displayName = "Background Color",
                category = PropertyCategory.COLOR_SCHEME,
                type = PropertyType.ColorType(),
                description = "Main background color",
                getValue = { it.colorScheme.background },
                setValue = { theme, value ->
                    theme.copy(colorScheme = theme.colorScheme.copy(background = value as Color))
                }
            ),
            PropertyDef(
                name = "colorScheme.error",
                displayName = "Error Color",
                category = PropertyCategory.COLOR_SCHEME,
                type = PropertyType.ColorType(),
                description = "Color for error states",
                getValue = { it.colorScheme.error },
                setValue = { theme, value ->
                    theme.copy(colorScheme = theme.colorScheme.copy(error = value as Color))
                }
            ),

            // ==================== Typography ====================
            PropertyDef(
                name = "typography.displayLarge.size",
                displayName = "Display Large Size",
                category = PropertyCategory.TYPOGRAPHY,
                type = PropertyType.NumberType(min = 20f, max = 100f, step = 1f),
                description = "Font size for large display text",
                getValue = { it.typography.displayLarge.size },
                setValue = { theme, value ->
                    val font = theme.typography.displayLarge.copy(size = value as Float)
                    theme.copy(typography = theme.typography.copy(displayLarge = font))
                }
            ),
            PropertyDef(
                name = "typography.displayLarge.weight",
                displayName = "Display Large Weight",
                category = PropertyCategory.TYPOGRAPHY,
                type = PropertyType.EnumType(listOf("Thin", "Light", "Regular", "Medium", "SemiBold", "Bold", "Black")),
                description = "Font weight for large display text",
                getValue = { it.typography.displayLarge.weight.toString() },
                setValue = { theme, value ->
                    val weight = Font.Weight.valueOf(value as String)
                    val font = theme.typography.displayLarge.copy(weight = weight)
                    theme.copy(typography = theme.typography.copy(displayLarge = font))
                }
            ),
            PropertyDef(
                name = "typography.bodyLarge.size",
                displayName = "Body Large Size",
                category = PropertyCategory.TYPOGRAPHY,
                type = PropertyType.NumberType(min = 10f, max = 24f, step = 0.5f),
                description = "Font size for body text",
                getValue = { it.typography.bodyLarge.size },
                setValue = { theme, value ->
                    val font = theme.typography.bodyLarge.copy(size = value as Float)
                    theme.copy(typography = theme.typography.copy(bodyLarge = font))
                }
            ),
            PropertyDef(
                name = "typography.bodyLarge.family",
                displayName = "Body Font Family",
                category = PropertyCategory.TYPOGRAPHY,
                type = PropertyType.FontType(
                    availableFonts = listOf(
                        "System",
                        "SF Pro Text",
                        "SF Pro Display",
                        "Segoe UI Variable Text",
                        "Roboto",
                        "Inter",
                        "Open Sans"
                    )
                ),
                description = "Font family for body text",
                getValue = { it.typography.bodyLarge.family },
                setValue = { theme, value ->
                    val font = theme.typography.bodyLarge.copy(family = value as String)
                    theme.copy(typography = theme.typography.copy(bodyLarge = font))
                }
            ),

            // ==================== Shapes ====================
            PropertyDef(
                name = "shapes.small",
                displayName = "Small Corner Radius",
                category = PropertyCategory.SHAPES,
                type = PropertyType.NumberType(min = 0f, max = 32f, step = 1f),
                description = "Corner radius for small components",
                getValue = { it.shapes.small.topLeft },
                setValue = { theme, value ->
                    theme.copy(shapes = theme.shapes.copy(small = CornerRadius.all(value as Float)))
                }
            ),
            PropertyDef(
                name = "shapes.medium",
                displayName = "Medium Corner Radius",
                category = PropertyCategory.SHAPES,
                type = PropertyType.NumberType(min = 0f, max = 48f, step = 1f),
                description = "Corner radius for medium components",
                getValue = { it.shapes.medium.topLeft },
                setValue = { theme, value ->
                    theme.copy(shapes = theme.shapes.copy(medium = CornerRadius.all(value as Float)))
                }
            ),
            PropertyDef(
                name = "shapes.large",
                displayName = "Large Corner Radius",
                category = PropertyCategory.SHAPES,
                type = PropertyType.NumberType(min = 0f, max = 64f, step = 1f),
                description = "Corner radius for large components",
                getValue = { it.shapes.large.topLeft },
                setValue = { theme, value ->
                    theme.copy(shapes = theme.shapes.copy(large = CornerRadius.all(value as Float)))
                }
            ),

            // ==================== Spacing ====================
            PropertyDef(
                name = "spacing.xs",
                displayName = "Extra Small Spacing",
                category = PropertyCategory.SPACING,
                type = PropertyType.NumberType(min = 2f, max = 8f, step = 1f),
                description = "Extra small spacing value",
                getValue = { it.spacing.xs },
                setValue = { theme, value ->
                    theme.copy(spacing = theme.spacing.copy(xs = value as Float))
                }
            ),
            PropertyDef(
                name = "spacing.sm",
                displayName = "Small Spacing",
                category = PropertyCategory.SPACING,
                type = PropertyType.NumberType(min = 4f, max = 16f, step = 1f),
                description = "Small spacing value",
                getValue = { it.spacing.sm },
                setValue = { theme, value ->
                    theme.copy(spacing = theme.spacing.copy(sm = value as Float))
                }
            ),
            PropertyDef(
                name = "spacing.md",
                displayName = "Medium Spacing",
                category = PropertyCategory.SPACING,
                type = PropertyType.NumberType(min = 8f, max = 32f, step = 1f),
                description = "Medium spacing value",
                getValue = { it.spacing.md },
                setValue = { theme, value ->
                    theme.copy(spacing = theme.spacing.copy(md = value as Float))
                }
            ),
            PropertyDef(
                name = "spacing.lg",
                displayName = "Large Spacing",
                category = PropertyCategory.SPACING,
                type = PropertyType.NumberType(min = 16f, max = 48f, step = 1f),
                description = "Large spacing value",
                getValue = { it.spacing.lg },
                setValue = { theme, value ->
                    theme.copy(spacing = theme.spacing.copy(lg = value as Float))
                }
            ),
            PropertyDef(
                name = "spacing.xl",
                displayName = "Extra Large Spacing",
                category = PropertyCategory.SPACING,
                type = PropertyType.NumberType(min = 24f, max = 64f, step = 1f),
                description = "Extra large spacing value",
                getValue = { it.spacing.xl },
                setValue = { theme, value ->
                    theme.copy(spacing = theme.spacing.copy(xl = value as Float))
                }
            ),

            // ==================== Elevation ====================
            PropertyDef(
                name = "elevation.level2.blurRadius",
                displayName = "Shadow Blur (Level 2)",
                category = PropertyCategory.ELEVATION,
                type = PropertyType.NumberType(min = 0f, max = 32f, step = 1f),
                description = "Shadow blur radius for elevation level 2",
                getValue = { it.elevation.level2.blurRadius },
                setValue = { theme, value ->
                    val shadow = theme.elevation.level2.copy(blurRadius = value as Float)
                    theme.copy(elevation = theme.elevation.copy(level2 = shadow))
                }
            ),
            PropertyDef(
                name = "elevation.level2.offsetY",
                displayName = "Shadow Offset (Level 2)",
                category = PropertyCategory.ELEVATION,
                type = PropertyType.NumberType(min = 0f, max = 16f, step = 1f),
                description = "Shadow Y offset for elevation level 2",
                getValue = { it.elevation.level2.offsetY },
                setValue = { theme, value ->
                    val shadow = theme.elevation.level2.copy(offsetY = value as Float)
                    theme.copy(elevation = theme.elevation.copy(level2 = shadow))
                }
            )
        )
    }

    /**
     * Get properties by category
     */
    fun getPropertiesByCategory(category: PropertyCategory): List<PropertyDef> {
        return properties.filter { it.category == category }
    }

    /**
     * Get a specific property
     */
    fun getProperty(name: String): PropertyDef? {
        return properties.find { it.name == name }
    }

    /**
     * Update a property value
     */
    fun updateProperty(propertyName: String, value: Any) {
        val property = getProperty(propertyName) ?: return
        val currentTheme = stateManager.currentState.currentTheme
        val newTheme = property.setValue(currentTheme, value)
        stateManager.updateTheme(newTheme, "Updated ${property.displayName}")
    }

    /**
     * Get current value of a property
     */
    fun getCurrentValue(propertyName: String): Any? {
        val property = getProperty(propertyName) ?: return null
        val currentTheme = stateManager.currentState.currentTheme
        return property.getValue(currentTheme)
    }

    /**
     * Color picker helper
     */
    class ColorPicker {
        /**
         * Generate color palette from a seed color
         */
        fun generatePalette(seedColor: Color, mode: PaletteMode = PaletteMode.COMPLEMENTARY): List<Color> {
            return when (mode) {
                PaletteMode.COMPLEMENTARY -> generateComplementary(seedColor)
                PaletteMode.ANALOGOUS -> generateAnalogous(seedColor)
                PaletteMode.TRIADIC -> generateTriadic(seedColor)
                PaletteMode.MONOCHROMATIC -> generateMonochromatic(seedColor)
            }
        }

        private fun generateComplementary(color: Color): List<Color> {
            val (h, s, l) = rgbToHsl(color)
            return listOf(
                color,
                hslToRgb((h + 180) % 360, s, l),
                hslToRgb(h, s * 0.5f, l * 1.2f),
                hslToRgb((h + 180) % 360, s * 0.5f, l * 1.2f)
            )
        }

        private fun generateAnalogous(color: Color): List<Color> {
            val (h, s, l) = rgbToHsl(color)
            return listOf(
                color,
                hslToRgb((h + 30) % 360, s, l),
                hslToRgb((h - 30 + 360) % 360, s, l),
                hslToRgb((h + 60) % 360, s, l)
            )
        }

        private fun generateTriadic(color: Color): List<Color> {
            val (h, s, l) = rgbToHsl(color)
            return listOf(
                color,
                hslToRgb((h + 120) % 360, s, l),
                hslToRgb((h + 240) % 360, s, l)
            )
        }

        private fun generateMonochromatic(color: Color): List<Color> {
            val (h, s, l) = rgbToHsl(color)
            return listOf(
                hslToRgb(h, s, l * 0.3f),
                hslToRgb(h, s, l * 0.6f),
                color,
                hslToRgb(h, s, l * 1.2f.coerceAtMost(1f)),
                hslToRgb(h, s * 0.5f, l * 1.4f.coerceAtMost(1f))
            )
        }

        private fun rgbToHsl(color: Color): Triple<Float, Float, Float> {
            val r = color.red / 255f
            val g = color.green / 255f
            val b = color.blue / 255f

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min

            val l = (max + min) / 2

            val s = if (delta == 0f) 0f else delta / (1 - Math.abs(2 * l - 1))

            val h = when {
                delta == 0f -> 0f
                max == r -> 60 * (((g - b) / delta) % 6)
                max == g -> 60 * (((b - r) / delta) + 2)
                else -> 60 * (((r - g) / delta) + 4)
            }

            return Triple(h, s, l)
        }

        private fun hslToRgb(h: Float, s: Float, l: Float): Color {
            val c = (1 - Math.abs(2 * l - 1)) * s
            val x = c * (1 - Math.abs((h / 60) % 2 - 1))
            val m = l - c / 2

            val (r1, g1, b1) = when {
                h < 60 -> Triple(c, x, 0f)
                h < 120 -> Triple(x, c, 0f)
                h < 180 -> Triple(0f, c, x)
                h < 240 -> Triple(0f, x, c)
                h < 300 -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }

            return Color(
                ((r1 + m) * 255).toInt(),
                ((g1 + m) * 255).toInt(),
                ((b1 + m) * 255).toInt()
            )
        }
    }

    enum class PaletteMode {
        COMPLEMENTARY,
        ANALOGOUS,
        TRIADIC,
        MONOCHROMATIC
    }
}
