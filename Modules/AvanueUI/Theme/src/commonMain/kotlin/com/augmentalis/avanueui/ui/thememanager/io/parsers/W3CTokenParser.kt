package com.augmentalis.avanueui.ui.thememanager.io.parsers

import com.augmentalis.avanueui.core.*
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable

/**
 * W3C Design Tokens Parser
 *
 * Parses themes from the W3C Design Tokens Community Group specification format.
 * See: https://design-tokens.github.io/community-group/format/
 *
 * Specification (October 2025):
 * - Tokens are defined as JSON objects with `$value` and `$type` properties
 * - Supports: color, dimension, fontFamily, fontWeight, duration, etc.
 * - Groups can be nested for organization
 * - Supports token aliases via `{reference.path}` syntax
 *
 * Example W3C Token:
 * ```json
 * {
 *   "color": {
 *     "primary": {
 *       "$value": "#6750A4",
 *       "$type": "color",
 *       "$description": "Primary brand color"
 *     }
 *   }
 * }
 * ```
 *
 * @since 1.0.0
 */
class W3CTokenParser {

    /**
     * Parse W3C Design Tokens JSON into a MagicUI Theme
     *
     * @param json The W3C tokens JSON string
     * @return Result containing the parsed Theme or an error
     */
    fun parse(json: String): Result<Theme> = runCatching {
        val jsonElement = Json.parseToJsonElement(json)
        require(jsonElement is JsonObject) { "Root must be a JSON object" }

        val tokens = parseTokenGroup(jsonElement)

        // Build theme from tokens
        buildTheme(tokens)
    }

    /**
     * Parse a token group (recursive for nested groups)
     *
     * @param obj The JSON object to parse
     * @param prefix Current path prefix for nested groups
     * @return Map of token paths to token values
     */
    private fun parseTokenGroup(
        obj: JsonObject,
        prefix: String = ""
    ): Map<String, W3CToken> {
        val tokens = mutableMapOf<String, W3CToken>()

        for ((key, value) in obj) {
            // Skip metadata keys
            if (key.startsWith("$")) continue

            val path = if (prefix.isEmpty()) key else "$prefix.$key"

            when (value) {
                is JsonObject -> {
                    // Check if this is a token (has $value) or a group
                    if (value.containsKey("\$value")) {
                        tokens[path] = parseToken(value, path)
                    } else {
                        // Nested group - recurse
                        tokens.putAll(parseTokenGroup(value, path))
                    }
                }
                else -> {
                    // Invalid format
                    throw IllegalArgumentException("Token at $path must be an object")
                }
            }
        }

        return tokens
    }

    /**
     * Parse a single token object
     *
     * @param obj The token JSON object
     * @param path The token path (for error messages)
     * @return Parsed W3CToken
     */
    private fun parseToken(obj: JsonObject, path: String): W3CToken {
        val value = obj["\$value"]
            ?: throw IllegalArgumentException("Token at $path missing \$value")

        val type = obj["\$type"]?.jsonPrimitive?.content
            ?: inferType(value)

        val description = obj["\$description"]?.jsonPrimitive?.content

        return W3CToken(
            path = path,
            value = value,
            type = type,
            description = description
        )
    }

    /**
     * Infer token type from value
     *
     * @param value The token value
     * @return Inferred type string
     */
    private fun inferType(value: JsonElement): String = when {
        value is JsonPrimitive && value.isString -> {
            val str = value.content
            when {
                str.startsWith("#") || str.startsWith("rgb") -> "color"
                str.endsWith("px") || str.endsWith("dp") || str.endsWith("sp") -> "dimension"
                str.endsWith("ms") || str.endsWith("s") -> "duration"
                else -> "string"
            }
        }
        value is JsonPrimitive && value.doubleOrNull != null -> "number"
        else -> "unknown"
    }

    /**
     * Build a MagicUI Theme from parsed W3C tokens
     *
     * Maps W3C token paths to MagicUI theme properties.
     *
     * @param tokens Map of token paths to values
     * @return Constructed Theme
     */
    private fun buildTheme(tokens: Map<String, W3CToken>): Theme {
        val name = tokens["theme.name"]?.asString() ?: "Imported W3C Theme"
        val platform = ThemePlatform.Custom

        // Parse colors
        val colorScheme = buildColorScheme(tokens)

        // Parse typography
        val typography = buildTypography(tokens)

        // Parse shapes
        val shapes = buildShapes(tokens)

        // Parse spacing
        val spacing = buildSpacing(tokens)

        // Parse elevation
        val elevation = buildElevation(tokens)

        // Parse animation
        val animation = buildAnimation(tokens)

        return Theme(
            name = name,
            platform = platform,
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            spacing = spacing,
            elevation = elevation,
            material = null, // W3C spec doesn't support glass/spatial materials
            animation = animation
        )
    }

    /**
     * Build ColorScheme from tokens
     */
    private fun buildColorScheme(tokens: Map<String, W3CToken>): ColorScheme {
        fun getColor(path: String, default: Color): Color =
            tokens[path]?.asColor() ?: default

        return ColorScheme(
            mode = ColorScheme.ColorMode.Light,
            primary = getColor("color.primary", Color.hex("#6750A4")),
            onPrimary = getColor("color.onPrimary", Color.White),
            primaryContainer = getColor("color.primaryContainer", Color.hex("#EADDFF")),
            onPrimaryContainer = getColor("color.onPrimaryContainer", Color.hex("#21005D")),
            secondary = getColor("color.secondary", Color.hex("#625B71")),
            onSecondary = getColor("color.onSecondary", Color.White),
            secondaryContainer = getColor("color.secondaryContainer", Color.hex("#E8DEF8")),
            onSecondaryContainer = getColor("color.onSecondaryContainer", Color.hex("#1D192B")),
            tertiary = getColor("color.tertiary", Color.hex("#7D5260")),
            onTertiary = getColor("color.onTertiary", Color.White),
            tertiaryContainer = getColor("color.tertiaryContainer", Color.hex("#FFD8E4")),
            onTertiaryContainer = getColor("color.onTertiaryContainer", Color.hex("#31111D")),
            error = getColor("color.error", Color.hex("#B3261E")),
            onError = getColor("color.onError", Color.White),
            errorContainer = getColor("color.errorContainer", Color.hex("#F9DEDC")),
            onErrorContainer = getColor("color.onErrorContainer", Color.hex("#410E0B")),
            surface = getColor("color.surface", Color.hex("#FEF7FF")),
            onSurface = getColor("color.onSurface", Color.hex("#1D1B20")),
            surfaceVariant = getColor("color.surfaceVariant", Color.hex("#E7E0EC")),
            onSurfaceVariant = getColor("color.onSurfaceVariant", Color.hex("#49454F")),
            background = getColor("color.background", Color.hex("#FEF7FF")),
            onBackground = getColor("color.onBackground", Color.hex("#1D1B20")),
            outline = getColor("color.outline", Color.hex("#79747E")),
            outlineVariant = getColor("color.outlineVariant", Color.hex("#CAC4D0"))
        )
    }

    /**
     * Build Typography from tokens
     */
    private fun buildTypography(tokens: Map<String, W3CToken>): Typography {
        fun getFont(prefix: String, default: Font): Font {
            val size = tokens["$prefix.fontSize"]?.asDimension() ?: default.size
            val weight = tokens["$prefix.fontWeight"]?.asWeight() ?: default.weight
            val lineHeight = tokens["$prefix.lineHeight"]?.asDimension() ?: default.lineHeight
            val family = tokens["$prefix.fontFamily"]?.asString()

            return Font(
                family = family ?: "System",
                size = size,
                weight = weight,
                lineHeight = lineHeight
            )
        }

        return Typography(
            displayLarge = getFont("typography.displayLarge", Font(size = 57f)),
            displayMedium = getFont("typography.displayMedium", Font(size = 45f)),
            displaySmall = getFont("typography.displaySmall", Font(size = 36f)),
            headlineLarge = getFont("typography.headlineLarge", Font(size = 32f)),
            headlineMedium = getFont("typography.headlineMedium", Font(size = 28f)),
            headlineSmall = getFont("typography.headlineSmall", Font(size = 24f)),
            titleLarge = getFont("typography.titleLarge", Font(size = 22f)),
            titleMedium = getFont("typography.titleMedium", Font(size = 16f, weight = Font.Weight.Medium)),
            titleSmall = getFont("typography.titleSmall", Font(size = 14f, weight = Font.Weight.Medium)),
            bodyLarge = getFont("typography.bodyLarge", Font(size = 16f)),
            bodyMedium = getFont("typography.bodyMedium", Font(size = 14f)),
            bodySmall = getFont("typography.bodySmall", Font(size = 12f)),
            labelLarge = getFont("typography.labelLarge", Font(size = 14f, weight = Font.Weight.Medium)),
            labelMedium = getFont("typography.labelMedium", Font(size = 12f, weight = Font.Weight.Medium)),
            labelSmall = getFont("typography.labelSmall", Font(size = 11f, weight = Font.Weight.Medium))
        )
    }

    /**
     * Build Shapes from tokens
     */
    private fun buildShapes(tokens: Map<String, W3CToken>): Shapes {
        fun getCornerRadius(path: String, default: Float): CornerRadius {
            val value = tokens[path]?.asDimension() ?: default
            return CornerRadius.all(value)
        }

        return Shapes(
            extraSmall = getCornerRadius("shape.extraSmall", 4f),
            small = getCornerRadius("shape.small", 8f),
            medium = getCornerRadius("shape.medium", 12f),
            large = getCornerRadius("shape.large", 16f),
            extraLarge = getCornerRadius("shape.extraLarge", 28f)
        )
    }

    /**
     * Build SpacingScale from tokens
     */
    private fun buildSpacing(tokens: Map<String, W3CToken>): SpacingScale {
        fun getDimension(path: String, default: Float): Float =
            tokens[path]?.asDimension() ?: default

        return SpacingScale(
            xs = getDimension("spacing.xs", 4f),
            sm = getDimension("spacing.sm", 8f),
            md = getDimension("spacing.md", 16f),
            lg = getDimension("spacing.lg", 24f),
            xl = getDimension("spacing.xl", 32f),
            xxl = getDimension("spacing.xxl", 48f)
        )
    }

    /**
     * Build ElevationScale from tokens
     */
    private fun buildElevation(tokens: Map<String, W3CToken>): ElevationScale {
        fun getShadow(prefix: String, default: Shadow): Shadow {
            val offsetY = tokens["$prefix.offsetY"]?.asDimension() ?: default.offsetY
            val blurRadius = tokens["$prefix.blurRadius"]?.asDimension() ?: default.blurRadius
            return Shadow(offsetY = offsetY, blurRadius = blurRadius)
        }

        return ElevationScale(
            level0 = getShadow("elevation.level0", Shadow(offsetY = 0f, blurRadius = 0f)),
            level1 = getShadow("elevation.level1", Shadow(offsetY = 1f, blurRadius = 3f)),
            level2 = getShadow("elevation.level2", Shadow(offsetY = 2f, blurRadius = 6f)),
            level3 = getShadow("elevation.level3", Shadow(offsetY = 4f, blurRadius = 8f)),
            level4 = getShadow("elevation.level4", Shadow(offsetY = 6f, blurRadius = 12f)),
            level5 = getShadow("elevation.level5", Shadow(offsetY = 8f, blurRadius = 16f))
        )
    }

    /**
     * Build AnimationConfig from tokens
     */
    private fun buildAnimation(tokens: Map<String, W3CToken>): AnimationConfig {
        val duration = tokens["animation.defaultDuration"]?.asDuration()?.toLong() ?: 300L
        return AnimationConfig(defaultDuration = duration)
    }
}

/**
 * Represents a parsed W3C token
 */
@Serializable
private data class W3CToken(
    val path: String,
    val value: JsonElement,
    val type: String,
    val description: String? = null
) {
    /**
     * Convert token value to Color
     */
    fun asColor(): Color? = when {
        value is JsonPrimitive && value.isString -> {
            Color.parse(value.content)
        }
        else -> null
    }

    /**
     * Convert token value to dimension (float)
     */
    fun asDimension(): Float? = when {
        value is JsonPrimitive && value.isString -> {
            val str = value.content
            str.removeSuffix("px")
                .removeSuffix("dp")
                .removeSuffix("sp")
                .removeSuffix("pt")
                .toFloatOrNull()
        }
        value is JsonPrimitive -> value.floatOrNull
        else -> null
    }

    /**
     * Convert token value to duration (milliseconds)
     */
    fun asDuration(): Float? = when {
        value is JsonPrimitive && value.isString -> {
            val str = value.content
            when {
                str.endsWith("ms") -> str.removeSuffix("ms").toFloatOrNull()
                str.endsWith("s") -> str.removeSuffix("s").toFloatOrNull()?.times(1000)
                else -> str.toFloatOrNull()
            }
        }
        value is JsonPrimitive -> value.floatOrNull
        else -> null
    }

    /**
     * Convert token value to font weight
     */
    fun asWeight(): Font.Weight? = when {
        value is JsonPrimitive && value.isString -> {
            when (value.content.lowercase()) {
                "thin", "100" -> Font.Weight.Thin
                "extralight", "200" -> Font.Weight.ExtraLight
                "light", "300" -> Font.Weight.Light
                "normal", "regular", "400" -> Font.Weight.Regular
                "medium", "500" -> Font.Weight.Medium
                "semibold", "600" -> Font.Weight.SemiBold
                "bold", "700" -> Font.Weight.Bold
                "extrabold", "800" -> Font.Weight.ExtraBold
                "black", "900" -> Font.Weight.Black
                else -> null
            }
        }
        value is JsonPrimitive -> {
            value.intOrNull?.let { weight ->
                when (weight) {
                    100 -> Font.Weight.Thin
                    200 -> Font.Weight.ExtraLight
                    300 -> Font.Weight.Light
                    400 -> Font.Weight.Regular
                    500 -> Font.Weight.Medium
                    600 -> Font.Weight.SemiBold
                    700 -> Font.Weight.Bold
                    800 -> Font.Weight.ExtraBold
                    900 -> Font.Weight.Black
                    else -> null
                }
            }
        }
        else -> null
    }

    /**
     * Convert token value to string
     */
    fun asString(): String? = when {
        value is JsonPrimitive && value.isString -> value.content
        else -> null
    }
}
