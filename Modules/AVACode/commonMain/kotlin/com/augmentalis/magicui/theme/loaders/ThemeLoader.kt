package com.augmentalis.avamagic.theme.loaders

import com.augmentalis.avamagic.theme.ThemeConfig

/**
 * Unified theme loader with automatic format detection.
 *
 * Supports multiple theme formats with AMF as the preferred format:
 * - **AMF (.amf)**: AvaMagic Format - compact, fast, AI-friendly (preferred)
 * - **YAML (.yaml, .yml)**: Human-readable but verbose (legacy)
 * - **JSON (.json)**: Machine-readable (legacy)
 *
 * ## Usage
 *
 * ```kotlin
 * // Auto-detect format from content
 * val theme = ThemeLoader.load(content)
 *
 * // Specify format explicitly
 * val theme = ThemeLoader.load(content, ThemeFormat.AMF)
 *
 * // Load from file (auto-detect by extension)
 * val theme = ThemeLoader.loadFromFile(fileName, content)
 * ```
 *
 * ## Format Detection
 *
 * When format is not specified, detection order:
 * 1. Check for AMF header (`---` delimiters + `schema:` line)
 * 2. Check for JSON structure (`{` as first non-whitespace)
 * 3. Fall back to YAML
 *
 * @since 3.2.0
 */
object ThemeLoader {

    /**
     * Load theme with automatic format detection.
     *
     * @param content The theme content string
     * @return Parsed ThemeConfig
     * @throws IllegalArgumentException if format cannot be detected or content is invalid
     */
    fun load(content: String): ThemeConfig {
        val format = detectFormat(content)
        return load(content, format)
    }

    /**
     * Load theme with explicit format.
     *
     * @param content The theme content string
     * @param format The format to use for parsing
     * @return Parsed ThemeConfig
     * @throws IllegalArgumentException if content is invalid for the specified format
     */
    fun load(content: String, format: ThemeFormat): ThemeConfig {
        return when (format) {
            ThemeFormat.AMF -> AmfThemeLoader.load(content)
            ThemeFormat.YAML -> YamlThemeLoader.load(content)
            ThemeFormat.JSON -> JsonThemeLoader.load(content)
        }
    }

    /**
     * Load theme from file with format detection by extension.
     *
     * @param fileName The file name (used for extension detection)
     * @param content The file content
     * @return Parsed ThemeConfig
     * @throws IllegalArgumentException if format cannot be detected or content is invalid
     */
    fun loadFromFile(fileName: String, content: String): ThemeConfig {
        val format = AmfThemeLoader.detectFormat(fileName) ?: detectFormat(content)
        return load(content, format)
    }

    /**
     * Detect format from content.
     *
     * @param content The content to analyze
     * @return Detected format
     */
    fun detectFormat(content: String): ThemeFormat {
        val trimmed = content.trimStart()

        // Check for AMF format (has --- delimiters and schema: line)
        if (AmfThemeLoader.isValidAmfFormat(content)) {
            return ThemeFormat.AMF
        }

        // Check for JSON (starts with {)
        if (trimmed.startsWith("{")) {
            return ThemeFormat.JSON
        }

        // Default to YAML
        return ThemeFormat.YAML
    }

    /**
     * Serialize theme to specified format.
     *
     * @param theme The theme to serialize
     * @param format Target format (AMF recommended)
     * @return Serialized string
     */
    fun serialize(theme: ThemeConfig, format: ThemeFormat = ThemeFormat.AMF): String {
        return when (format) {
            ThemeFormat.AMF -> AmfThemeSerializer.serialize(theme)
            ThemeFormat.YAML -> YamlThemeSerializer.serialize(theme)
            ThemeFormat.JSON -> JsonThemeSerializer.serialize(theme)
        }
    }

    /**
     * Convert theme from one format to another.
     *
     * @param content Source content
     * @param sourceFormat Source format (or null for auto-detect)
     * @param targetFormat Target format
     * @return Converted content string
     */
    fun convert(
        content: String,
        sourceFormat: ThemeFormat? = null,
        targetFormat: ThemeFormat = ThemeFormat.AMF
    ): String {
        val format = sourceFormat ?: detectFormat(content)
        val theme = load(content, format)
        return serialize(theme, targetFormat)
    }
}
