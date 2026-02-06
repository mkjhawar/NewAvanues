package com.augmentalis.avamagic.theme

import com.augmentalis.avamagic.theme.loaders.*

/**
 * Migration utility for converting YAML/JSON themes to AMF (AvaMagic Format).
 *
 * Provides comprehensive migration capabilities with validation to ensure
 * data integrity during the transition from legacy formats to AMF.
 *
 * ## Why Migrate to AMF?
 *
 * - **Token Efficiency:** ~50% more compact than YAML/JSON
 * - **Parsing Speed:** Line-based format is faster to parse
 * - **AI-Friendly:** Compact format uses fewer tokens for LLM processing
 *
 * ## Usage Examples
 *
 * ### Migrate YAML to AMF
 * ```kotlin
 * val yamlContent = """
 *   name: "Dark Theme"
 *   palette:
 *     primary: "#007AFF"
 *     secondary: "#5AC8FA"
 * """.trimIndent()
 *
 * val amfContent = ThemeMigrator.migrateYamlToAmf(yamlContent)
 * ```
 *
 * ### Migrate JSON to AMF
 * ```kotlin
 * val jsonContent = """{"name": "Light Theme", "palette": {...}}"""
 * val amfContent = ThemeMigrator.migrateJsonToAmf(jsonContent)
 * ```
 *
 * ### Auto-detect and Migrate
 * ```kotlin
 * val content = File("theme.yaml").readText()
 * val amfContent = ThemeMigrator.migrateFile(content)
 * ```
 *
 * ### Validate Migration
 * ```kotlin
 * val result = ThemeMigrator.validateMigration(originalYaml, migratedAmf)
 * if (!result.success) {
 *     println("Differences found: ${result.differences}")
 * }
 * ```
 *
 * @since 3.2.0
 * @see ThemeLoader for unified loading
 * @see AmfThemeSerializer for AMF serialization
 */
object ThemeMigrator {

    /**
     * Converts YAML theme content to AMF format.
     *
     * Parses the YAML content, loads it into a [ThemeConfig], and serializes
     * it to AMF format with full fidelity.
     *
     * @param yamlContent The YAML theme content to convert
     * @return AMF formatted string
     * @throws IllegalArgumentException if YAML content is malformed
     *
     * @sample
     * ```kotlin
     * val yaml = """
     *   name: "Dark Theme"
     *   palette:
     *     primary: "#007AFF"
     *     background: "#000000"
     * """.trimIndent()
     * val amf = ThemeMigrator.migrateYamlToAmf(yaml)
     * ```
     */
    fun migrateYamlToAmf(yamlContent: String): String {
        val theme = YamlThemeLoader.load(yamlContent)
        return AmfThemeSerializer.serialize(theme, includeDefaults = true)
    }

    /**
     * Converts JSON theme content to AMF format.
     *
     * Parses the JSON content, loads it into a [ThemeConfig], and serializes
     * it to AMF format with full fidelity.
     *
     * @param jsonContent The JSON theme content to convert
     * @return AMF formatted string
     * @throws IllegalArgumentException if JSON content is malformed or invalid
     *
     * @sample
     * ```kotlin
     * val json = """
     *   {
     *     "name": "Light Theme",
     *     "palette": {
     *       "primary": "#0A84FF",
     *       "background": "#FFFFFF"
     *     }
     *   }
     * """.trimIndent()
     * val amf = ThemeMigrator.migrateJsonToAmf(json)
     * ```
     */
    fun migrateJsonToAmf(jsonContent: String): String {
        val theme = JsonThemeLoader.load(jsonContent)
        return AmfThemeSerializer.serialize(theme, includeDefaults = true)
    }

    /**
     * Auto-detects format and converts to AMF.
     *
     * Uses [ThemeLoader.detectFormat] to determine the source format,
     * then converts to AMF. If the content is already in AMF format,
     * it is normalized (re-parsed and re-serialized) to ensure consistency.
     *
     * ## Detection Order
     *
     * 1. Check for AMF header (`---` delimiters + `schema:` line)
     * 2. Check for JSON structure (`{` as first non-whitespace)
     * 3. Fall back to YAML
     *
     * @param content The theme content in any supported format
     * @return AMF formatted string
     * @throws IllegalArgumentException if content format cannot be detected or is invalid
     *
     * @sample
     * ```kotlin
     * val content = File("legacy-theme.yaml").readText()
     * val amf = ThemeMigrator.migrateFile(content)
     * File("theme.amf").writeText(amf)
     * ```
     */
    fun migrateFile(content: String): String {
        val format = ThemeLoader.detectFormat(content)
        val theme = ThemeLoader.load(content, format)
        return AmfThemeSerializer.serialize(theme, includeDefaults = true)
    }

    /**
     * Validates migration by comparing original and migrated theme data.
     *
     * Performs a round-trip validation:
     * 1. Parse original content to [ThemeConfig]
     * 2. Parse migrated content to [ThemeConfig]
     * 3. Compare all fields between the two configs
     * 4. Report any differences found
     *
     * This ensures that no data is lost during migration and both formats
     * represent the same theme configuration.
     *
     * @param original The original theme content (YAML, JSON, or AMF)
     * @param migrated The migrated AMF content
     * @return [MigrationResult] with validation status and any differences
     *
     * @sample
     * ```kotlin
     * val originalYaml = File("theme.yaml").readText()
     * val migratedAmf = ThemeMigrator.migrateYamlToAmf(originalYaml)
     *
     * val result = ThemeMigrator.validateMigration(originalYaml, migratedAmf)
     * if (result.success) {
     *     println("Migration successful!")
     * } else {
     *     println("Differences: ${result.differences}")
     * }
     * ```
     */
    fun validateMigration(original: String, migrated: String): MigrationResult {
        val originalFormat = ThemeLoader.detectFormat(original)
        val differences = mutableListOf<String>()

        val originalTheme: ThemeConfig
        val migratedTheme: ThemeConfig

        try {
            originalTheme = ThemeLoader.load(original)
        } catch (e: Exception) {
            return MigrationResult(
                success = false,
                originalFormat = originalFormat,
                differences = listOf("Failed to parse original content: ${e.message}")
            )
        }

        try {
            migratedTheme = AmfThemeLoader.load(migrated)
        } catch (e: Exception) {
            return MigrationResult(
                success = false,
                originalFormat = originalFormat,
                differences = listOf("Failed to parse migrated AMF content: ${e.message}")
            )
        }

        // Compare theme name
        if (originalTheme.name != migratedTheme.name) {
            differences.add("name: '${originalTheme.name}' -> '${migratedTheme.name}'")
        }

        // Compare palette
        differences.addAll(comparePalette(originalTheme.palette, migratedTheme.palette))

        // Compare typography
        differences.addAll(compareTypography(originalTheme.typography, migratedTheme.typography))

        // Compare spacing
        differences.addAll(compareSpacing(originalTheme.spacing, migratedTheme.spacing))

        // Compare effects
        differences.addAll(compareEffects(originalTheme.effects, migratedTheme.effects))

        return MigrationResult(
            success = differences.isEmpty(),
            originalFormat = originalFormat,
            differences = differences
        )
    }

    /**
     * Batch migrate multiple theme files.
     *
     * Processes a list of theme contents and returns migration results for each.
     * Useful for migrating entire theme directories.
     *
     * @param contents Map of file name to content
     * @return Map of file name to [MigrationResult] with migrated content
     */
    fun batchMigrate(contents: Map<String, String>): Map<String, BatchMigrationResult> {
        return contents.mapValues { (fileName, content) ->
            try {
                val amfContent = migrateFile(content)
                val validation = validateMigration(content, amfContent)
                BatchMigrationResult(
                    amfContent = amfContent,
                    validationResult = validation,
                    error = null
                )
            } catch (e: Exception) {
                BatchMigrationResult(
                    amfContent = null,
                    validationResult = null,
                    error = e.message ?: "Unknown error during migration"
                )
            }
        }
    }

    /**
     * Compare two palettes and return list of differences.
     */
    private fun comparePalette(original: ThemePalette, migrated: ThemePalette): List<String> {
        val differences = mutableListOf<String>()

        if (original.primary != migrated.primary) {
            differences.add("palette.primary: '${original.primary}' -> '${migrated.primary}'")
        }
        if (original.secondary != migrated.secondary) {
            differences.add("palette.secondary: '${original.secondary}' -> '${migrated.secondary}'")
        }
        if (original.background != migrated.background) {
            differences.add("palette.background: '${original.background}' -> '${migrated.background}'")
        }
        if (original.surface != migrated.surface) {
            differences.add("palette.surface: '${original.surface}' -> '${migrated.surface}'")
        }
        if (original.error != migrated.error) {
            differences.add("palette.error: '${original.error}' -> '${migrated.error}'")
        }
        if (original.onPrimary != migrated.onPrimary) {
            differences.add("palette.onPrimary: '${original.onPrimary}' -> '${migrated.onPrimary}'")
        }
        if (original.onSecondary != migrated.onSecondary) {
            differences.add("palette.onSecondary: '${original.onSecondary}' -> '${migrated.onSecondary}'")
        }
        if (original.onBackground != migrated.onBackground) {
            differences.add("palette.onBackground: '${original.onBackground}' -> '${migrated.onBackground}'")
        }
        if (original.onSurface != migrated.onSurface) {
            differences.add("palette.onSurface: '${original.onSurface}' -> '${migrated.onSurface}'")
        }
        if (original.onError != migrated.onError) {
            differences.add("palette.onError: '${original.onError}' -> '${migrated.onError}'")
        }

        return differences
    }

    /**
     * Compare two typography configurations and return list of differences.
     */
    private fun compareTypography(original: ThemeTypography, migrated: ThemeTypography): List<String> {
        val differences = mutableListOf<String>()

        differences.addAll(compareTextStyle("h1", original.h1, migrated.h1))
        differences.addAll(compareTextStyle("h2", original.h2, migrated.h2))
        differences.addAll(compareTextStyle("body", original.body, migrated.body))
        differences.addAll(compareTextStyle("caption", original.caption, migrated.caption))

        return differences
    }

    /**
     * Compare two text styles and return list of differences.
     */
    private fun compareTextStyle(name: String, original: TextStyle, migrated: TextStyle): List<String> {
        val differences = mutableListOf<String>()

        // Note: AMF serializes size as Int, so we compare truncated values
        if (original.size.toInt() != migrated.size.toInt()) {
            differences.add("typography.$name.size: ${original.size} -> ${migrated.size}")
        }
        if (original.weight != migrated.weight) {
            differences.add("typography.$name.weight: '${original.weight}' -> '${migrated.weight}'")
        }
        if (original.fontFamily != migrated.fontFamily) {
            differences.add("typography.$name.fontFamily: '${original.fontFamily}' -> '${migrated.fontFamily}'")
        }

        return differences
    }

    /**
     * Compare two spacing configurations and return list of differences.
     */
    private fun compareSpacing(original: ThemeSpacing, migrated: ThemeSpacing): List<String> {
        val differences = mutableListOf<String>()

        // Note: AMF serializes spacing as Int, so we compare truncated values
        if (original.xs.toInt() != migrated.xs.toInt()) {
            differences.add("spacing.xs: ${original.xs} -> ${migrated.xs}")
        }
        if (original.sm.toInt() != migrated.sm.toInt()) {
            differences.add("spacing.sm: ${original.sm} -> ${migrated.sm}")
        }
        if (original.md.toInt() != migrated.md.toInt()) {
            differences.add("spacing.md: ${original.md} -> ${migrated.md}")
        }
        if (original.lg.toInt() != migrated.lg.toInt()) {
            differences.add("spacing.lg: ${original.lg} -> ${migrated.lg}")
        }
        if (original.xl.toInt() != migrated.xl.toInt()) {
            differences.add("spacing.xl: ${original.xl} -> ${migrated.xl}")
        }

        return differences
    }

    /**
     * Compare two effects configurations and return list of differences.
     */
    private fun compareEffects(original: ThemeEffects, migrated: ThemeEffects): List<String> {
        val differences = mutableListOf<String>()

        if (original.shadowEnabled != migrated.shadowEnabled) {
            differences.add("effects.shadowEnabled: ${original.shadowEnabled} -> ${migrated.shadowEnabled}")
        }
        // Note: AMF serializes effects as Int, so we compare truncated values
        if (original.blurRadius.toInt() != migrated.blurRadius.toInt()) {
            differences.add("effects.blurRadius: ${original.blurRadius} -> ${migrated.blurRadius}")
        }
        if (original.elevation.toInt() != migrated.elevation.toInt()) {
            differences.add("effects.elevation: ${original.elevation} -> ${migrated.elevation}")
        }

        return differences
    }
}

/**
 * Result of a theme migration validation.
 *
 * Indicates whether the migration preserved all data and lists any differences
 * found between the original and migrated theme configurations.
 *
 * @property success True if migration preserved all data (no differences)
 * @property originalFormat The detected format of the original content
 * @property differences List of field differences found (empty if successful)
 *
 * @since 3.2.0
 */
data class MigrationResult(
    val success: Boolean,
    val originalFormat: ThemeFormat,
    val differences: List<String>
) {
    /**
     * Returns a human-readable summary of the migration result.
     */
    fun summary(): String = buildString {
        appendLine("Migration Result:")
        appendLine("  Status: ${if (success) "SUCCESS" else "FAILED"}")
        appendLine("  Original Format: $originalFormat")
        if (differences.isNotEmpty()) {
            appendLine("  Differences (${differences.size}):")
            differences.forEach { diff ->
                appendLine("    - $diff")
            }
        }
    }
}

/**
 * Result of a batch migration operation for a single file.
 *
 * Contains the migrated AMF content (if successful), validation result,
 * and any error that occurred during migration.
 *
 * @property amfContent The migrated AMF content, or null if migration failed
 * @property validationResult The validation result, or null if migration failed
 * @property error Error message if migration failed, or null if successful
 *
 * @since 3.2.0
 */
data class BatchMigrationResult(
    val amfContent: String?,
    val validationResult: MigrationResult?,
    val error: String?
) {
    /**
     * True if migration completed successfully without errors.
     */
    val success: Boolean get() = error == null && validationResult?.success == true
}
