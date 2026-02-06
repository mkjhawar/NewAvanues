package com.augmentalis.avamagic.theme.loaders

import com.augmentalis.avamagic.theme.ThemeConfig

/**
 * Loads AvaUI themes from AMF (AvaMagic Format) files.
 *
 * AMF is the preferred format for AvaMagic themes due to:
 * - **Token Efficiency:** ~50% more compact than YAML/JSON
 * - **Parsing Speed:** Line-based format is faster to parse
 * - **AI-Friendly:** Compact format uses fewer tokens for LLM processing
 *
 * ## Usage Example
 *
 * ```kotlin
 * val amfContent = File("theme.amf").readText()
 * val theme = AmfThemeLoader.load(amfContent)
 * ```
 *
 * ## File Extension
 *
 * AMF files use the `.amf` extension. The schema header indicates the content type:
 * - `schema: amf-thm-1.0` - Theme definition
 * - `schema: amf-lyt-1.0` - Layout definition (future)
 *
 * ## Error Handling
 *
 * Throws [AmfParseException] if:
 * - File format is invalid (missing delimiters)
 * - Schema is unsupported or missing
 * - Record format is malformed
 *
 * @since 3.2.0
 * @see AmfThemeParser for format specification
 * @see AmfThemeSerializer for serialization
 */
object AmfThemeLoader {

    /**
     * Load theme from AMF format string.
     *
     * @param amfString The AMF content to parse
     * @return A fully-formed ThemeConfig with all defaults applied
     * @throws AmfParseException if AMF format is invalid
     */
    fun load(amfString: String): ThemeConfig {
        return AmfThemeParser.parse(amfString)
    }

    /**
     * Check if content appears to be valid AMF format.
     *
     * Performs a quick validation without full parsing.
     *
     * @param content The content to check
     * @return true if content appears to be valid AMF format
     */
    fun isValidAmfFormat(content: String): Boolean {
        val lines = content.lines().map { it.trim() }
        val hasDelimiters = lines.count { it == "---" } >= 2
        val hasSchema = lines.any { it.startsWith("schema:") && it.contains("amf-") }
        return hasDelimiters && hasSchema
    }

    /**
     * Detect theme format from file extension.
     *
     * @param fileName The file name or path
     * @return The detected format or null if unknown
     */
    fun detectFormat(fileName: String): ThemeFormat? {
        return when {
            fileName.endsWith(".amf") -> ThemeFormat.AMF
            fileName.endsWith(".yaml") || fileName.endsWith(".yml") -> ThemeFormat.YAML
            fileName.endsWith(".json") -> ThemeFormat.JSON
            else -> null
        }
    }
}

/**
 * Supported theme file formats.
 */
enum class ThemeFormat {
    /** AvaMagic Format - compact line-based format (preferred) */
    AMF,
    /** YAML format - verbose but human-readable (legacy) */
    YAML,
    /** JSON format - machine-readable (legacy) */
    JSON
}
