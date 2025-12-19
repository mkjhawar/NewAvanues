package com.augmentalis.avanues.avamagic.components.themebuilder.Engine

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Shadow
import com.augmentalis.avaelements.core.types.CornerRadius
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.math.pow

/**
 * Importer for loading themes from various formats
 */
class ThemeImporter {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Import theme from JSON string
     *
     * @param jsonString JSON theme definition
     * @return Parsed theme object
     * @throws IllegalArgumentException if JSON is invalid
     */
    fun importFromJSON(jsonString: String): Result<Theme> {
        return try {
            val theme = json.decodeFromString<Theme>(jsonString)
            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to parse JSON theme: ${e.message}", e))
        }
    }

    /**
     * Import theme from YAML string
     *
     * @param yamlString YAML theme definition
     * @return Parsed theme object
     */
    fun importFromYAML(yamlString: String): Result<Theme> {
        return try {
            // Convert YAML to JSON (simplified parser for basic YAML)
            val jsonString = yamlToJson(yamlString)
            importFromJSON(jsonString)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to parse YAML theme: ${e.message}", e))
        }
    }

    /**
     * Import theme from Kotlin DSL file
     *
     * This method parses DSL-style theme definitions back into Theme objects.
     * Since we can't eval Kotlin code, we parse it as structured text.
     *
     * @param dslString Kotlin DSL theme definition
     * @return Parsed theme object
     */
    fun importFromDSL(dslString: String): Result<Theme> {
        return try {
            val theme = parseDSLTheme(dslString)
            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to parse DSL theme: ${e.message}", e))
        }
    }

    /**
     * Parse DSL-style theme definition
     */
    private fun parseDSLTheme(dsl: String): Theme {
        // Extract theme name
        val nameRegex = """name\s*=\s*"([^"]+)"""".toRegex()
        val name = nameRegex.find(dsl)?.groupValues?.get(1) ?: "Imported Theme"

        // Extract platform
        val platformRegex = """platform\s*=\s*ThemePlatform\.(\w+)""".toRegex()
        val platformStr = platformRegex.find(dsl)?.groupValues?.get(1) ?: "Material3_Expressive"
        val platform = ThemePlatform.valueOf(platformStr)

        // Extract color scheme
        val colorScheme = parseColorScheme(dsl)

        // Extract typography
        val typography = parseTypography(dsl)

        // Extract shapes
        val shapes = parseShapes(dsl)

        // Extract spacing
        val spacing = parseSpacing(dsl)

        // Extract elevation
        val elevation = parseElevation(dsl)

        return Theme(
            name = name,
            platform = platform,
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            spacing = spacing,
            elevation = elevation
        )
    }

    /**
     * Parse color scheme from DSL
     */
    private fun parseColorScheme(dsl: String): ColorScheme {
        val colorSchemeSection = extractSection(dsl, "colorScheme")

        return ColorScheme(
            mode = extractColorMode(colorSchemeSection),
            primary = extractColor(colorSchemeSection, "primary"),
            onPrimary = extractColor(colorSchemeSection, "onPrimary"),
            primaryContainer = extractColor(colorSchemeSection, "primaryContainer"),
            onPrimaryContainer = extractColor(colorSchemeSection, "onPrimaryContainer"),
            secondary = extractColor(colorSchemeSection, "secondary"),
            onSecondary = extractColor(colorSchemeSection, "onSecondary"),
            secondaryContainer = extractColor(colorSchemeSection, "secondaryContainer"),
            onSecondaryContainer = extractColor(colorSchemeSection, "onSecondaryContainer"),
            tertiary = extractColor(colorSchemeSection, "tertiary"),
            onTertiary = extractColor(colorSchemeSection, "onTertiary"),
            tertiaryContainer = extractColor(colorSchemeSection, "tertiaryContainer"),
            onTertiaryContainer = extractColor(colorSchemeSection, "onTertiaryContainer"),
            error = extractColor(colorSchemeSection, "error"),
            onError = extractColor(colorSchemeSection, "onError"),
            errorContainer = extractColor(colorSchemeSection, "errorContainer"),
            onErrorContainer = extractColor(colorSchemeSection, "onErrorContainer"),
            surface = extractColor(colorSchemeSection, "surface"),
            onSurface = extractColor(colorSchemeSection, "onSurface"),
            surfaceVariant = extractColor(colorSchemeSection, "surfaceVariant"),
            onSurfaceVariant = extractColor(colorSchemeSection, "onSurfaceVariant"),
            background = extractColor(colorSchemeSection, "background"),
            onBackground = extractColor(colorSchemeSection, "onBackground"),
            outline = extractColor(colorSchemeSection, "outline"),
            outlineVariant = extractColor(colorSchemeSection, "outlineVariant")
        )
    }

    /**
     * Parse typography from DSL
     */
    private fun parseTypography(dsl: String): Typography {
        val typographySection = extractSection(dsl, "typography")

        return Typography(
            displayLarge = extractFont(typographySection, "displayLarge"),
            displayMedium = extractFont(typographySection, "displayMedium"),
            displaySmall = extractFont(typographySection, "displaySmall"),
            headlineLarge = extractFont(typographySection, "headlineLarge"),
            headlineMedium = extractFont(typographySection, "headlineMedium"),
            headlineSmall = extractFont(typographySection, "headlineSmall"),
            titleLarge = extractFont(typographySection, "titleLarge"),
            titleMedium = extractFont(typographySection, "titleMedium"),
            titleSmall = extractFont(typographySection, "titleSmall"),
            bodyLarge = extractFont(typographySection, "bodyLarge"),
            bodyMedium = extractFont(typographySection, "bodyMedium"),
            bodySmall = extractFont(typographySection, "bodySmall"),
            labelLarge = extractFont(typographySection, "labelLarge"),
            labelMedium = extractFont(typographySection, "labelMedium"),
            labelSmall = extractFont(typographySection, "labelSmall")
        )
    }

    /**
     * Parse shapes from DSL
     */
    private fun parseShapes(dsl: String): Shapes {
        val shapesSection = extractSection(dsl, "shapes")

        return Shapes(
            extraSmall = CornerRadius.all(extractFloat(shapesSection, "extraSmall") ?: 4f),
            small = CornerRadius.all(extractFloat(shapesSection, "small") ?: 8f),
            medium = CornerRadius.all(extractFloat(shapesSection, "medium") ?: 12f),
            large = CornerRadius.all(extractFloat(shapesSection, "large") ?: 16f),
            extraLarge = CornerRadius.all(extractFloat(shapesSection, "extraLarge") ?: 28f)
        )
    }

    /**
     * Parse spacing from DSL
     */
    private fun parseSpacing(dsl: String): SpacingScale {
        val spacingSection = extractSection(dsl, "spacing")

        return SpacingScale(
            xs = extractFloat(spacingSection, "xs") ?: 4f,
            sm = extractFloat(spacingSection, "sm") ?: 8f,
            md = extractFloat(spacingSection, "md") ?: 16f,
            lg = extractFloat(spacingSection, "lg") ?: 24f,
            xl = extractFloat(spacingSection, "xl") ?: 32f,
            xxl = extractFloat(spacingSection, "xxl") ?: 48f
        )
    }

    /**
     * Parse elevation from DSL
     */
    private fun parseElevation(dsl: String): ElevationScale {
        val elevationSection = extractSection(dsl, "elevation")

        // TODO: Parse Shadow objects properly
        return ElevationScale()
    }

    /**
     * Extract a section from DSL by name
     */
    private fun extractSection(dsl: String, sectionName: String): String {
        val regex = """$sectionName\s*=\s*\w+\s*\((.*?)\)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        return regex.find(dsl)?.groupValues?.get(1) ?: ""
    }

    /**
     * Extract color mode from color scheme section
     */
    private fun extractColorMode(section: String): ColorScheme.ColorMode {
        val regex = """mode\s*=\s*ColorMode\.(\w+)""".toRegex()
        val modeStr = regex.find(section)?.groupValues?.get(1) ?: "Light"
        return try {
            ColorScheme.ColorMode.valueOf(modeStr)
        } catch (e: Exception) {
            ColorScheme.ColorMode.Light
        }
    }

    /**
     * Extract color value from section
     */
    private fun extractColor(section: String, colorName: String): Color {
        // Try Color.hex("#RRGGBB") format
        val hexRegex = """$colorName\s*=\s*Color\.hex\("([#\w]+)"\)""".toRegex()
        hexRegex.find(section)?.groupValues?.get(1)?.let { hex ->
            return Color.hex(hex)
        }

        // Try Color.rgb(r, g, b) format
        val rgbRegex = """$colorName\s*=\s*Color\.rgb\((\d+),\s*(\d+),\s*(\d+)\)""".toRegex()
        rgbRegex.find(section)?.let { match ->
            val (r, g, b) = match.destructured
            return Color(r.toInt(), g.toInt(), b.toInt())
        }

        // Default to white
        return Color.White
    }

    /**
     * Extract font definition from section
     */
    private fun extractFont(section: String, fontName: String): Font {
        val fontSection = extractSection(section, fontName)

        val familyRegex = """family\s*=\s*"([^"]+)"""".toRegex()
        val family = familyRegex.find(fontSection)?.groupValues?.get(1) ?: "Roboto"

        val size = extractFloat(fontSection, "size") ?: 14f
        val weight = extractFontWeight(fontSection)

        return Font(
            family = family,
            size = size,
            weight = weight
        )
    }

    /**
     * Extract font weight
     */
    private fun extractFontWeight(section: String): Font.Weight {
        val regex = """weight\s*=\s*FontWeight\.(\w+)""".toRegex()
        val weightStr = regex.find(section)?.groupValues?.get(1) ?: "Regular"
        return try {
            Font.Weight.valueOf(weightStr)
        } catch (e: Exception) {
            Font.Weight.Regular
        }
    }

    /**
     * Extract float value from section
     */
    private fun extractFloat(section: String, valueName: String): Float? {
        val regex = """$valueName\s*=\s*([\d.]+)f?""".toRegex()
        return regex.find(section)?.groupValues?.get(1)?.toFloatOrNull()
    }

    /**
     * Convert YAML to JSON (simplified conversion)
     */
    private fun yamlToJson(yaml: String): String {
        val lines = yaml.lines().filter { it.isNotBlank() && !it.trim().startsWith("#") }
        val json = StringBuilder()
        json.append("{")

        var indent = 0
        var prevIndent = 0
        var isArray = false

        lines.forEachIndexed { index, line ->
            val currentIndent = line.takeWhile { it == ' ' }.length / 2
            val trimmed = line.trim()

            when {
                trimmed.endsWith(":") -> {
                    // Object key
                    val key = trimmed.dropLast(1).trim()
                    if (index > 0) json.append(",")
                    json.append("\"$key\":{")
                    prevIndent = currentIndent
                }
                trimmed.contains(":") -> {
                    // Key-value pair
                    val (key, value) = trimmed.split(":", limit = 2)
                    if (index > 0 && currentIndent == prevIndent) json.append(",")
                    json.append("\"${key.trim()}\":${formatValue(value.trim())}")
                }
                currentIndent < prevIndent -> {
                    // Close nested object
                    json.append("}")
                    prevIndent = currentIndent
                }
            }
        }

        // Close all open braces
        repeat(indent + 1) { json.append("}") }

        return json.toString()
    }

    /**
     * Format YAML value for JSON
     */
    private fun formatValue(value: String): String {
        return when {
            value.startsWith("\"") -> value // Already quoted
            value.startsWith("#") -> "\"$value\"" // Hex color
            value.toIntOrNull() != null -> value // Number
            value.toFloatOrNull() != null -> value // Float
            value == "true" || value == "false" -> value // Boolean
            else -> "\"$value\"" // String
        }
    }

    /**
     * Validate imported theme
     */
    fun validate(theme: Theme): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validate name
        if (theme.name.isBlank()) {
            errors.add("Theme name cannot be empty")
        }

        // Validate color contrast (basic check)
        val contrastRatio = calculateContrastRatio(
            theme.colorScheme.primary,
            theme.colorScheme.onPrimary
        )
        if (contrastRatio < 4.5f) {
            warnings.add("Primary/OnPrimary contrast ratio is ${contrastRatio}, should be at least 4.5:1 for accessibility")
        }

        // Validate font sizes
        listOf(
            theme.typography.displayLarge,
            theme.typography.bodyMedium,
            theme.typography.labelSmall
        ).forEach { font ->
            if (font.size < 10f) {
                warnings.add("Font size ${font.size}sp may be too small for readability")
            }
            if (font.size > 96f) {
                warnings.add("Font size ${font.size}sp may be too large")
            }
        }

        // Validate spacing progression
        val spacings = listOf(
            theme.spacing.xs,
            theme.spacing.sm,
            theme.spacing.md,
            theme.spacing.lg,
            theme.spacing.xl,
            theme.spacing.xxl
        )
        spacings.zipWithNext().forEach { (current, next) ->
            if (next <= current) {
                warnings.add("Spacing values should progress in ascending order")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Calculate contrast ratio between two colors (WCAG formula)
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Float {
        val l1 = calculateRelativeLuminance(color1)
        val l2 = calculateRelativeLuminance(color2)

        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)

        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Calculate relative luminance (WCAG formula)
     */
    private fun calculateRelativeLuminance(color: Color): Float {
        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f

        val rSrgb = if (r <= 0.03928f) r / 12.92f else ((r + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        val gSrgb = if (g <= 0.03928f) g / 12.92f else ((g + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        val bSrgb = if (b <= 0.03928f) b / 12.92f else ((b + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

        return 0.2126f * rSrgb + 0.7152f * gSrgb + 0.0722f * bSrgb
    }
}

/**
 * Validation result for imported themes
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)
