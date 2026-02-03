package com.augmentalis.avanues.avaui.theme.loaders

import com.augmentalis.avanues.avaui.theme.*
import net.mamoe.yamlkt.Yaml

/**
 * Loads AvaUI themes from YAML files.
 *
 * Supports plugin YAML format:
 * ```yaml
 * name: "Dark Theme"
 * palette:
 *   primary: "#007AFF"
 *   secondary: "#5AC8FA"
 *   background: "#000000"
 * typography:
 *   fontFamily: "Roboto"
 *   h1Size: 28
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * val yamlContent = File("theme.yaml").readText()
 * val theme = YamlThemeLoader.load(yamlContent)
 * ```
 *
 * ## Format Support
 *
 * The loader handles:
 * - **Missing fields**: Uses sensible defaults from ThemeConfig
 * - **Type coercion**: Converts numbers to floats automatically
 * - **Unknown keys**: Safely ignored to support future extensions
 *
 * @since 3.1.0
 */
object YamlThemeLoader {

    private val yaml = Yaml.Default

    /**
     * Load theme from YAML string.
     *
     * @param yamlString The YAML content to parse
     * @return A fully-formed ThemeConfig with all defaults applied
     * @throws IllegalArgumentException if YAML is malformed
     */
    fun load(yamlString: String): ThemeConfig {
        val yamlMap = yaml.decodeYamlFromString(yamlString) as Map<String, Any?>

        return ThemeConfig(
            name = yamlMap["name"] as? String ?: "Unnamed Theme",
            palette = loadPalette(yamlMap["palette"] as? Map<String, Any?>),
            typography = loadTypography(yamlMap["typography"] as? Map<String, Any?>),
            spacing = loadSpacing(yamlMap["spacing"] as? Map<String, Any?>),
            effects = loadEffects(yamlMap["effects"] as? Map<String, Any?>)
        )
    }

    /**
     * Loads color palette from YAML map.
     *
     * @param paletteMap The palette section of the YAML
     * @return ThemePalette with defaults for missing colors
     */
    private fun loadPalette(paletteMap: Map<String, Any?>?): ThemePalette {
        return ThemePalette(
            primary = paletteMap?.get("primary") as? String ?: "#007AFF",
            secondary = paletteMap?.get("secondary") as? String ?: "#5AC8FA",
            background = paletteMap?.get("background") as? String ?: "#000000",
            surface = paletteMap?.get("surface") as? String ?: "#1C1C1E",
            error = paletteMap?.get("error") as? String ?: "#FF3B30",
            onPrimary = paletteMap?.get("onPrimary") as? String ?: "#FFFFFF",
            onSecondary = paletteMap?.get("onSecondary") as? String ?: "#FFFFFF",
            onBackground = paletteMap?.get("onBackground") as? String ?: "#FFFFFF",
            onSurface = paletteMap?.get("onSurface") as? String ?: "#FFFFFF",
            onError = paletteMap?.get("onError") as? String ?: "#FFFFFF"
        )
    }

    /**
     * Loads typography configuration from YAML map.
     *
     * Supports both per-style configuration and a global fontFamily.
     *
     * @param typographyMap The typography section of the YAML
     * @return ThemeTypography with defaults for missing styles
     */
    private fun loadTypography(typographyMap: Map<String, Any?>?): ThemeTypography {
        val fontFamily = typographyMap?.get("fontFamily") as? String ?: "system"

        return ThemeTypography(
            h1 = loadTextStyle(typographyMap?.get("h1") as? Map<String, Any?>, 28f, "bold", fontFamily),
            h2 = loadTextStyle(typographyMap?.get("h2") as? Map<String, Any?>, 22f, "bold", fontFamily),
            body = loadTextStyle(typographyMap?.get("body") as? Map<String, Any?>, 16f, "regular", fontFamily),
            caption = loadTextStyle(typographyMap?.get("caption") as? Map<String, Any?>, 12f, "regular", fontFamily)
        )
    }

    /**
     * Loads a single text style from YAML map.
     *
     * @param styleMap The style configuration
     * @param defaultSize Default font size if not specified
     * @param defaultWeight Default font weight if not specified
     * @param defaultFamily Default font family if not specified
     * @return TextStyle with all properties resolved
     */
    private fun loadTextStyle(
        styleMap: Map<String, Any?>?,
        defaultSize: Float,
        defaultWeight: String,
        defaultFamily: String
    ): TextStyle {
        return TextStyle(
            size = (styleMap?.get("size") as? Number)?.toFloat() ?: defaultSize,
            weight = styleMap?.get("weight") as? String ?: defaultWeight,
            fontFamily = styleMap?.get("fontFamily") as? String ?: defaultFamily
        )
    }

    /**
     * Loads spacing configuration from YAML map.
     *
     * @param spacingMap The spacing section of the YAML
     * @return ThemeSpacing with defaults for missing values
     */
    private fun loadSpacing(spacingMap: Map<String, Any?>?): ThemeSpacing {
        return ThemeSpacing(
            xs = (spacingMap?.get("xs") as? Number)?.toFloat() ?: 4f,
            sm = (spacingMap?.get("sm") as? Number)?.toFloat() ?: 8f,
            md = (spacingMap?.get("md") as? Number)?.toFloat() ?: 16f,
            lg = (spacingMap?.get("lg") as? Number)?.toFloat() ?: 24f,
            xl = (spacingMap?.get("xl") as? Number)?.toFloat() ?: 32f
        )
    }

    /**
     * Loads visual effects configuration from YAML map.
     *
     * @param effectsMap The effects section of the YAML
     * @return ThemeEffects with defaults for missing values
     */
    private fun loadEffects(effectsMap: Map<String, Any?>?): ThemeEffects {
        return ThemeEffects(
            shadowEnabled = effectsMap?.get("shadowEnabled") as? Boolean ?: true,
            blurRadius = (effectsMap?.get("blurRadius") as? Number)?.toFloat() ?: 8f,
            elevation = (effectsMap?.get("elevation") as? Number)?.toFloat() ?: 4f
        )
    }
}
