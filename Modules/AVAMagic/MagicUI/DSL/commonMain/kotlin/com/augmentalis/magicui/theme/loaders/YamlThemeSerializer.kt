package com.augmentalis.magicui.theme.loaders

import com.augmentalis.magicui.theme.*

/**
 * Serializes AvaUI themes to YAML format.
 *
 * Produces human-readable YAML suitable for:
 * - Plugin theme definitions
 * - Theme exports
 * - Configuration files
 *
 * ## Usage Example
 *
 * ```kotlin
 * val theme = ThemeConfig(
 *     name = "Dark Theme",
 *     palette = ThemePalette(
 *         primary = "#007AFF",
 *         secondary = "#5AC8FA"
 *     )
 * )
 * val yaml = YamlThemeSerializer.serialize(theme)
 * File("theme.yaml").writeText(yaml)
 * ```
 *
 * ## Output Format
 *
 * The serializer produces clean, indented YAML:
 * ```yaml
 * name: "Dark Theme"
 *
 * palette:
 *   primary: "#007AFF"
 *   secondary: "#5AC8FA"
 *   ...
 *
 * typography:
 *   fontFamily: "system"
 *   h1:
 *     size: 28.0
 *     weight: "bold"
 *     fontFamily: "system"
 *   ...
 * ```
 *
 * @since 3.1.0
 */
object YamlThemeSerializer {

    /**
     * Serialize theme to YAML string.
     *
     * @param theme The ThemeConfig to serialize
     * @return YAML string representation
     */
    fun serialize(theme: ThemeConfig): String {
        return buildString {
            appendLine("name: \"${theme.name}\"")
            appendLine()
            appendLine("palette:")
            serializePalette(theme.palette, this)
            appendLine()
            appendLine("typography:")
            serializeTypography(theme.typography, this)
            appendLine()
            appendLine("spacing:")
            serializeSpacing(theme.spacing, this)
            appendLine()
            appendLine("effects:")
            serializeEffects(theme.effects, this)
        }
    }

    /**
     * Serializes color palette to YAML.
     *
     * Writes all palette colors with proper indentation.
     *
     * @param palette The palette to serialize
     * @param builder The string builder to append to
     */
    private fun serializePalette(palette: ThemePalette, builder: StringBuilder) {
        builder.appendLine("  primary: \"${palette.primary}\"")
        builder.appendLine("  secondary: \"${palette.secondary}\"")
        builder.appendLine("  background: \"${palette.background}\"")
        builder.appendLine("  surface: \"${palette.surface}\"")
        builder.appendLine("  error: \"${palette.error}\"")
        builder.appendLine("  onPrimary: \"${palette.onPrimary}\"")
        builder.appendLine("  onSecondary: \"${palette.onSecondary}\"")
        builder.appendLine("  onBackground: \"${palette.onBackground}\"")
        builder.appendLine("  onSurface: \"${palette.onSurface}\"")
        builder.appendLine("  onError: \"${palette.onError}\"")
    }

    /**
     * Serializes typography configuration to YAML.
     *
     * Includes a global fontFamily and individual text styles.
     *
     * @param typography The typography to serialize
     * @param builder The string builder to append to
     */
    private fun serializeTypography(typography: ThemeTypography, builder: StringBuilder) {
        builder.appendLine("  fontFamily: \"${typography.h1.fontFamily}\"")
        builder.appendLine("  h1:")
        serializeTextStyle(typography.h1, builder, "    ")
        builder.appendLine("  h2:")
        serializeTextStyle(typography.h2, builder, "    ")
        builder.appendLine("  body:")
        serializeTextStyle(typography.body, builder, "    ")
        builder.appendLine("  caption:")
        serializeTextStyle(typography.caption, builder, "    ")
    }

    /**
     * Serializes a single text style to YAML.
     *
     * @param style The text style to serialize
     * @param builder The string builder to append to
     * @param indent The indentation prefix for this style
     */
    private fun serializeTextStyle(style: TextStyle, builder: StringBuilder, indent: String) {
        builder.appendLine("${indent}size: ${style.size}")
        builder.appendLine("${indent}weight: \"${style.weight}\"")
        builder.appendLine("${indent}fontFamily: \"${style.fontFamily}\"")
    }

    /**
     * Serializes spacing configuration to YAML.
     *
     * @param spacing The spacing to serialize
     * @param builder The string builder to append to
     */
    private fun serializeSpacing(spacing: ThemeSpacing, builder: StringBuilder) {
        builder.appendLine("  xs: ${spacing.xs}")
        builder.appendLine("  sm: ${spacing.sm}")
        builder.appendLine("  md: ${spacing.md}")
        builder.appendLine("  lg: ${spacing.lg}")
        builder.appendLine("  xl: ${spacing.xl}")
    }

    /**
     * Serializes visual effects configuration to YAML.
     *
     * @param effects The effects to serialize
     * @param builder The string builder to append to
     */
    private fun serializeEffects(effects: ThemeEffects, builder: StringBuilder) {
        builder.appendLine("  shadowEnabled: ${effects.shadowEnabled}")
        builder.appendLine("  blurRadius: ${effects.blurRadius}")
        builder.appendLine("  elevation: ${effects.elevation}")
    }
}
