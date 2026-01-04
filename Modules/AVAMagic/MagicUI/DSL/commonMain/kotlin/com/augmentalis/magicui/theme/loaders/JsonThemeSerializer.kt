package com.augmentalis.magicui.theme.loaders

import com.augmentalis.magicui.theme.ThemeConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Serializes AvaUI themes to JSON format.
 *
 * Produces pretty-printed JSON suitable for:
 * - Configuration files
 * - API responses
 * - Theme exports
 * - Storage and persistence
 *
 * ## Usage Example
 *
 * ```kotlin
 * val theme = ThemeConfig(
 *     name = "Dark Theme",
 *     palette = ThemePalette(
 *         primary = "#007AFF",
 *         secondary = "#5AC8FA",
 *         background = "#000000",
 *         surface = "#1C1C1E",
 *         error = "#FF3B30"
 *     )
 * )
 * val json = JsonThemeSerializer.serialize(theme)
 * File("theme.json").writeText(json)
 * ```
 *
 * ## Output Format
 *
 * The serializer produces clean, indented JSON:
 * ```json
 * {
 *   "name": "Dark Theme",
 *   "palette": {
 *     "primary": "#007AFF",
 *     "secondary": "#5AC8FA",
 *     "background": "#000000",
 *     "surface": "#1C1C1E",
 *     "error": "#FF3B30",
 *     "onPrimary": "#FFFFFF",
 *     "onSecondary": "#FFFFFF",
 *     "onBackground": "#FFFFFF",
 *     "onSurface": "#FFFFFF",
 *     "onError": "#FFFFFF"
 *   },
 *   "typography": {
 *     "h1": {
 *       "size": 28.0,
 *       "weight": "bold",
 *       "fontFamily": "system"
 *     },
 *     "h2": {
 *       "size": 22.0,
 *       "weight": "bold",
 *       "fontFamily": "system"
 *     },
 *     "body": {
 *       "size": 16.0,
 *       "weight": "regular",
 *       "fontFamily": "system"
 *     },
 *     "caption": {
 *       "size": 12.0,
 *       "weight": "regular",
 *       "fontFamily": "system"
 *     }
 *   },
 *   "spacing": {
 *     "xs": 4.0,
 *     "sm": 8.0,
 *     "md": 16.0,
 *     "lg": 24.0,
 *     "xl": 32.0
 *   },
 *   "effects": {
 *     "shadowEnabled": true,
 *     "blurRadius": 8.0,
 *     "elevation": 4.0
 *   }
 * }
 * ```
 *
 * ## Features
 *
 * - **Pretty printing**: Human-readable with proper indentation
 * - **Type safety**: Leverages kotlinx.serialization for correctness
 * - **Complete output**: All fields included, even defaults
 * - **Error handling**: Clear exceptions for serialization failures
 *
 * @since 3.1.0
 */
object JsonThemeSerializer {

    /**
     * JSON configuration with pretty printing enabled.
     *
     * Configured to:
     * - Use pretty printing with indentation for readability
     * - Encode defaults to show complete theme structure
     * - Use lenient parsing for flexibility when debugging
     */
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        isLenient = true
    }

    /**
     * Serializes theme to JSON string.
     *
     * Converts a ThemeConfig object into a pretty-printed JSON string.
     * All fields are included in the output, even those with default values.
     *
     * @param theme The ThemeConfig to serialize
     * @return JSON string representation with pretty printing
     * @throws IllegalArgumentException if serialization fails
     *
     * @see ThemeConfig for the complete structure being serialized
     */
    fun serialize(theme: ThemeConfig): String {
        return try {
            json.encodeToString(ThemeConfig.serializer(), theme)
        } catch (e: SerializationException) {
            throw IllegalArgumentException(
                "Failed to serialize theme to JSON: ${e.message}",
                e
            )
        }
    }
}
