package com.augmentalis.magicui.theme.loaders

import com.augmentalis.magicui.theme.ThemeConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Loads AvaUI themes from JSON files.
 *
 * Leverages kotlinx.serialization for automatic deserialization of ThemeConfig.
 * Supports standard JSON format with optional fields using defaults.
 *
 * ## JSON Format
 *
 * ```json
 * {
 *   "name": "Dark Theme",
 *   "palette": {
 *     "primary": "#007AFF",
 *     "secondary": "#5AC8FA",
 *     "background": "#000000",
 *     "surface": "#1C1C1E",
 *     "error": "#FF3B30"
 *   },
 *   "typography": {
 *     "h1": {
 *       "size": 28.0,
 *       "weight": "bold",
 *       "fontFamily": "system"
 *     },
 *     "body": {
 *       "size": 16.0,
 *       "weight": "regular"
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
 * ## Usage Example
 *
 * ```kotlin
 * val jsonContent = File("theme.json").readText()
 * val theme = JsonThemeLoader.load(jsonContent)
 * ```
 *
 * ## Format Support
 *
 * The loader handles:
 * - **Missing fields**: Uses sensible defaults from ThemeConfig
 * - **Flexible parsing**: Accepts both strict and lenient JSON
 * - **Unknown keys**: Safely ignored to support future extensions
 * - **Type safety**: Strong typing through kotlinx.serialization
 *
 * ## Error Handling
 *
 * Throws [IllegalArgumentException] if:
 * - JSON is malformed or invalid
 * - Required fields are missing
 * - Field types don't match expected types
 *
 * @since 3.1.0
 */
object JsonThemeLoader {

    /**
     * JSON configuration with lenient parsing.
     *
     * Configured to:
     * - Ignore unknown keys for forward compatibility
     * - Allow lenient parsing for flexibility
     * - Use pretty printing for debugging output
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    /**
     * Loads theme from JSON string.
     *
     * Deserializes the JSON string into a complete ThemeConfig object.
     * All optional fields will use their defaults if not specified in the JSON.
     *
     * @param jsonString The JSON content to parse
     * @return A fully-formed ThemeConfig with all defaults applied
     * @throws IllegalArgumentException if JSON is malformed or invalid
     *
     * @see ThemeConfig for the complete structure and default values
     */
    fun load(jsonString: String): ThemeConfig {
        return try {
            json.decodeFromString<ThemeConfig>(jsonString)
        } catch (e: SerializationException) {
            throw IllegalArgumentException(
                "Failed to parse JSON theme: ${e.message}",
                e
            )
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid JSON theme format: ${e.message}",
                e
            )
        }
    }
}
