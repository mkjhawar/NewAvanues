package com.augmentalis.avacode.plugins.themes

import kotlinx.serialization.Serializable

/**
 * Theme definition data model.
 *
 * Represents a complete theme with colors, typography, spacing, and effects.
 * Serializable for YAML/JSON parsing.
 *
 * Example YAML:
 * ```yaml
 * name: Dark Theme
 * version: 1.0.0
 * description: A dark color scheme with blue accents
 * author: Augmentalis
 * colors:
 *   primary: "#007AFF"
 *   secondary: "#5856D6"
 *   background: "#1C1C1E"
 *   text: "#FFFFFF"
 * typography:
 *   fontFamily: "SF Pro"
 *   fontSize:
 *     small: 14
 *     medium: 16
 *     large: 20
 * ```
 */
@Serializable
data class ThemeDefinition(
    /**
     * Theme name.
     * Example: "Dark Theme", "Light Theme", "High Contrast"
     */
    val name: String,

    /**
     * Theme version (semver).
     * Example: "1.0.0", "2.1.0-beta"
     */
    val version: String,

    /**
     * Theme description.
     */
    val description: String? = null,

    /**
     * Theme author.
     */
    val author: String? = null,

    /**
     * Color palette.
     */
    val colors: ColorPalette,

    /**
     * Typography settings.
     */
    val typography: Typography,

    /**
     * Spacing scale.
     */
    val spacing: Spacing,

    /**
     * Visual effects (optional).
     */
    val effects: Effects? = null,

    /**
     * Animation settings (optional).
     */
    val animations: Animations? = null,

    /**
     * Base theme to extend from (optional).
     * Example: "system", "light", "dark"
     */
    val extends: String? = null,

    /**
     * Theme metadata.
     */
    val metadata: ThemeMetadata? = null
) {
    /**
     * Get unique theme identifier.
     * Format: "author.name" or just "name" if no author
     */
    fun getThemeId(): String {
        return if (author != null) {
            "${author.lowercase().replace(" ", "-")}.${name.lowercase().replace(" ", "-")}"
        } else {
            name.lowercase().replace(" ", "-")
        }
    }

    /**
     * Check if this theme extends another theme.
     */
    fun hasBaseTheme(): Boolean {
        return extends != null
    }

    /**
     * Validate theme structure.
     * Returns list of validation errors (empty if valid).
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (name.isBlank()) {
            errors.add("Theme name must not be empty")
        }

        if (version.isBlank()) {
            errors.add("Theme version must not be empty")
        }

        // Validate version format (basic semver check)
        if (!version.matches(Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.]+)?(\\+[a-zA-Z0-9.]+)?$"))) {
            errors.add("Theme version must be valid semver format: $version")
        }

        return errors
    }
}

/**
 * Theme metadata.
 */
@Serializable
data class ThemeMetadata(
    /**
     * Theme tags for categorization.
     * Example: "dark", "high-contrast", "minimal"
     */
    val tags: List<String> = emptyList(),

    /**
     * Homepage URL.
     */
    val homepage: String? = null,

    /**
     * License identifier.
     * Example: "MIT", "Apache-2.0"
     */
    val license: String? = null,

    /**
     * Preview image URL (relative to plugin).
     * Example: "assets/images/theme-preview.png"
     */
    val preview: String? = null,

    /**
     * Whether theme supports dark mode.
     */
    val supportsDarkMode: Boolean = false,

    /**
     * Whether theme supports light mode.
     */
    val supportsLightMode: Boolean = false,

    /**
     * Custom metadata fields.
     */
    val custom: Map<String, String> = emptyMap()
)
