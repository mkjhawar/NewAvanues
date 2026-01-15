package com.augmentalis.avamagic.theme.loaders

import com.augmentalis.avamagic.theme.AnimationConfig
import com.augmentalis.avamagic.theme.ComponentStyle
import com.augmentalis.avamagic.theme.TextStyle
import com.augmentalis.avamagic.theme.ThemeConfig
import com.augmentalis.avamagic.theme.ThemeEffects
import com.augmentalis.avamagic.theme.ThemePalette
import com.augmentalis.avamagic.theme.ThemeSpacing
import com.augmentalis.avamagic.theme.ThemeTypography

/**
 * Parser for AVU/AMF theme files (unified format).
 *
 * Supports the unified AVU (Avanues Universal) format and legacy AMF (AvaMagic Format).
 * Provides ~50% size reduction vs YAML/JSON with fast line-based parsing.
 *
 * ## AVU Theme Format (.avu / .amf)
 *
 * ```
 * # AVU Format v1.0
 * # Type: THEME
 * ---
 * schema: avu-thm-1.0
 * ---
 * THM:Dark Theme:1.0.0
 * PAL:primary:#007AFF
 * PAL:secondary:#5AC8FA
 * PAL:background:#000000
 * TYP:h1:28:bold:system
 * TYP:body:16:regular:system
 * SPC:xs:4:sm:8:md:16:lg:24:xl:32
 * EFX:shadow:true:blur:8:elevation:4
 * CMP:button:radius:8:padding:12
 * ANI:fade:300:easeInOut
 * ```
 *
 * ## Record Types
 *
 * | Prefix | Purpose | Format |
 * |--------|---------|--------|
 * | `THM:` | Theme metadata | `THM:name:version` |
 * | `PAL:` | Palette color | `PAL:key:#hexcolor` |
 * | `TYP:` | Typography | `TYP:style:size:weight:family` |
 * | `SPC:` | Spacing | `SPC:xs:val:sm:val:md:val:lg:val:xl:val` |
 * | `EFX:` | Effects | `EFX:shadow:bool:blur:val:elevation:val` |
 * | `CMP:` | Component style | `CMP:name:k1:v1:k2:v2` |
 * | `ANI:` | Animation | `ANI:name:duration:easing` |
 *
 * @since 3.2.0
 * @see <a href="AVU-Format-Specification-V1.md">AVU Format Specification</a>
 */
object AmfThemeParser {

    private const val HEADER_DELIMITER = "---"
    private const val SCHEMA_PREFIX = "schema:"
    // Support both AVU (new) and AMF (legacy) schema prefixes
    private val THEME_SCHEMAS = listOf("avu-thm", "amf-thm")

    /**
     * Parse AMF format theme content.
     *
     * @param content The AMF format string content
     * @return Parsed ThemeConfig with defaults for missing fields
     * @throws AmfParseException if format is invalid or malformed
     */
    fun parse(content: String): ThemeConfig {
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        // Find content section (after second ---)
        val delimiterIndices = lines.mapIndexedNotNull { index, line ->
            if (line == HEADER_DELIMITER) index else null
        }

        if (delimiterIndices.size < 2) {
            throw AmfParseException("Invalid AMF format: missing header delimiters (---)")
        }

        // Validate schema (supports both avu-thm and amf-thm)
        val headerLines = lines.subList(delimiterIndices[0] + 1, delimiterIndices[1])
        val hasValidSchema = headerLines.any { line ->
            line.startsWith(SCHEMA_PREFIX) && THEME_SCHEMAS.any { schema -> line.contains(schema) }
        }
        if (!hasValidSchema) {
            throw AmfParseException("Invalid schema: expected 'schema: avu-thm-*' or 'schema: amf-thm-*'")
        }

        // Parse content records
        val contentLines = lines.subList(delimiterIndices[1] + 1, lines.size)
        return parseRecords(contentLines)
    }

    /**
     * Parse record lines into ThemeConfig.
     */
    private fun parseRecords(lines: List<String>): ThemeConfig {
        var name = "Unnamed Theme"
        val palette = mutableMapOf<String, String>()
        val typography = mutableMapOf<String, TextStyle>()
        var spacing: ThemeSpacing? = null
        var effects: ThemeEffects? = null
        val componentStyles = mutableMapOf<String, Map<String, String>>()
        val animations = mutableMapOf<String, AnimationConfig>()

        for (line in lines) {
            val parts = line.split(":")
            if (parts.isEmpty()) continue

            when (parts[0]) {
                "THM" -> {
                    if (parts.size >= 2) {
                        name = parts[1]
                    }
                }

                "PAL" -> {
                    if (parts.size >= 3) {
                        // PAL:key:#hexcolor - rejoin color parts in case of extra colons
                        val key = parts[1]
                        val color = parts.drop(2).joinToString(":")
                        palette[key] = color
                    }
                }

                "TYP" -> {
                    if (parts.size >= 5) {
                        // TYP:style:size:weight:family
                        val style = parts[1]
                        val size = parts[2].toFloatOrNull() ?: 16f
                        val weight = parts[3]
                        val family = parts[4]
                        typography[style] = TextStyle(size, weight, family)
                    }
                }

                "SPC" -> {
                    // SPC:xs:4:sm:8:md:16:lg:24:xl:32
                    spacing = parseSpacing(parts.drop(1))
                }

                "EFX" -> {
                    // EFX:shadow:true:blur:8:elevation:4
                    effects = parseEffects(parts.drop(1))
                }

                "CMP" -> {
                    // CMP:button:radius:8:padding:12:minHeight:44
                    if (parts.size >= 4) {
                        val componentName = parts[1]
                        val props = parseKeyValuePairs(parts.drop(2))
                        componentStyles[componentName] = props
                    }
                }

                "ANI" -> {
                    // ANI:fade:300:easeInOut
                    if (parts.size >= 4) {
                        val animName = parts[1]
                        val duration = parts[2].toIntOrNull() ?: 300
                        val easing = parts[3]
                        animations[animName] = AnimationConfig(duration, easing)
                    }
                }
            }
        }

        return ThemeConfig(
            name = name,
            palette = buildPalette(palette),
            typography = buildTypography(typography),
            spacing = spacing ?: ThemeSpacing(),
            effects = effects ?: ThemeEffects(),
            components = componentStyles.mapValues { ComponentStyle(it.value) },
            animations = animations
        )
    }

    /**
     * Parse spacing from key:value pairs.
     * Format: xs:4:sm:8:md:16:lg:24:xl:32
     */
    private fun parseSpacing(parts: List<String>): ThemeSpacing {
        val map = parseKeyValuePairs(parts)
        return ThemeSpacing(
            xs = map["xs"]?.toFloatOrNull() ?: 4f,
            sm = map["sm"]?.toFloatOrNull() ?: 8f,
            md = map["md"]?.toFloatOrNull() ?: 16f,
            lg = map["lg"]?.toFloatOrNull() ?: 24f,
            xl = map["xl"]?.toFloatOrNull() ?: 32f
        )
    }

    /**
     * Parse effects from key:value pairs.
     * Format: shadow:true:blur:8:elevation:4
     */
    private fun parseEffects(parts: List<String>): ThemeEffects {
        val map = parseKeyValuePairs(parts)
        return ThemeEffects(
            shadowEnabled = map["shadow"]?.toBooleanStrictOrNull() ?: true,
            blurRadius = map["blur"]?.toFloatOrNull() ?: 8f,
            elevation = map["elevation"]?.toFloatOrNull() ?: 4f
        )
    }

    /**
     * Parse alternating key:value pairs into a map.
     */
    private fun parseKeyValuePairs(parts: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var i = 0
        while (i + 1 < parts.size) {
            map[parts[i]] = parts[i + 1]
            i += 2
        }
        return map
    }

    /**
     * Build ThemePalette from parsed color map.
     */
    private fun buildPalette(palette: Map<String, String>): ThemePalette {
        return ThemePalette(
            primary = palette["primary"] ?: "#007AFF",
            secondary = palette["secondary"] ?: "#5AC8FA",
            background = palette["background"] ?: "#000000",
            surface = palette["surface"] ?: "#1C1C1E",
            error = palette["error"] ?: "#FF3B30",
            onPrimary = palette["onPrimary"] ?: "#FFFFFF",
            onSecondary = palette["onSecondary"] ?: "#FFFFFF",
            onBackground = palette["onBackground"] ?: "#FFFFFF",
            onSurface = palette["onSurface"] ?: "#FFFFFF",
            onError = palette["onError"] ?: "#FFFFFF"
        )
    }

    /**
     * Build ThemeTypography from parsed text style map.
     */
    private fun buildTypography(typography: Map<String, TextStyle>): ThemeTypography {
        return ThemeTypography(
            h1 = typography["h1"] ?: TextStyle(28f, "bold", "system"),
            h2 = typography["h2"] ?: TextStyle(22f, "bold", "system"),
            body = typography["body"] ?: TextStyle(16f, "regular", "system"),
            caption = typography["caption"] ?: TextStyle(12f, "regular", "system")
        )
    }
}

/**
 * Exception thrown when AMF parsing fails.
 */
class AmfParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
