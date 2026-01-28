@file:Suppress("USELESS_CAST", "UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS") // YAML parsing requires runtime type handling

/**
 * YamlThemeParser.kt - YAML theme file parser for VoiceOSCoreNG
 *
 * Parses YAML theme files and converts them to OverlayTheme instances.
 * KMP-compatible implementation using manual YAML parsing.
 *
 * Supports:
 * - Full theme configuration parsing
 * - Theme variants (highContrast, largeText, etc.)
 * - Color hex string to Long conversion
 * - Nested section parsing
 * - YamlThemeConfig data class mapping
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.OverlayTheme

/**
 * Result of YAML theme parsing
 */
sealed class ThemeParseResult {
    /**
     * Successfully parsed theme
     */
    data class Success(
        val theme: OverlayTheme,
        val config: YamlThemeConfig,
        val variants: Map<String, OverlayTheme> = emptyMap(),
        val warnings: List<String> = emptyList()
    ) : ThemeParseResult()

    /**
     * Parsing failed with error
     */
    data class Error(
        val message: String,
        val line: Int? = null,
        val column: Int? = null
    ) : ThemeParseResult()
}

/**
 * YAML theme parser for OverlayTheme configuration
 *
 * Supports parsing YAML theme definitions with:
 * - Full theme definitions
 * - Partial overrides (missing fields use defaults)
 * - Theme variants (light, dark, high-contrast)
 * - Color parsing (hex strings to Long)
 * - Validation warnings
 *
 * Example YAML:
 * ```yaml
 * name: CustomTheme
 * colors:
 *   primary: "#2196F3"
 *   background: "#1E1E1E"
 *   textPrimary: "#FFFFFF"
 * typography:
 *   titleSize: 16
 *   bodySize: 14
 * variants:
 *   highContrast:
 *     colors:
 *       background: "#000000"
 * ```
 */
object YamlThemeParser {

    private const val HEX_COLOR_PREFIX = "#"
    private const val HEX_COLOR_PREFIX_ALT = "0x"

    /**
     * Parse YAML string into OverlayTheme
     *
     * @param yaml YAML theme definition
     * @param baseTheme Optional base theme for partial definitions
     * @return ThemeParseResult with parsed theme or error
     */
    fun parse(yaml: String, baseTheme: OverlayTheme = OverlayTheme.DEFAULT): ThemeParseResult {
        if (yaml.isBlank()) {
            return ThemeParseResult.Error("Empty YAML content")
        }

        val warnings = mutableListOf<String>()

        try {
            val lines = yaml.lines()
            val parsed = parseYamlLines(lines)

            // Extract theme properties
            val themeMetadata = parsed["theme"] as? Map<*, *> ?: emptyMap<String, Any>()
            val colors = parsed["colors"] as? Map<*, *> ?: emptyMap<String, Any>()
            val typography = parsed["typography"] as? Map<*, *> ?: emptyMap<String, Any>()
            val spacing = parsed["spacing"] as? Map<*, *> ?: emptyMap<String, Any>()
            val shapes = parsed["shapes"] as? Map<*, *> ?: emptyMap<String, Any>()
            val elevation = parsed["elevation"] as? Map<*, *> ?: emptyMap<String, Any>()
            val borders = parsed["borders"] as? Map<*, *> ?: emptyMap<String, Any>()
            val sizes = parsed["sizes"] as? Map<*, *> ?: emptyMap<String, Any>()
            val animations = parsed["animations"] as? Map<*, *> ?: emptyMap<String, Any>()
            val accessibility = parsed["accessibility"] as? Map<*, *> ?: emptyMap<String, Any>()
            val opacity = parsed["opacity"] as? Map<*, *> ?: emptyMap<String, Any>()
            val text = parsed["text"] as? Map<*, *> ?: emptyMap<String, Any>()

            // Build YamlThemeConfig from parsed values
            val config = buildYamlThemeConfig(
                themeMetadata = themeMetadata,
                colors = colors,
                typography = typography,
                spacing = spacing,
                shapes = shapes,
                elevation = elevation,
                borders = borders,
                sizes = sizes,
                animations = animations,
                accessibility = accessibility,
                opacity = opacity,
                text = text,
                variants = parsed["variants"] as? Map<*, *>
            )

            // Build theme from parsed values with base theme defaults
            val theme = buildThemeFromParsed(
                baseTheme = baseTheme,
                colors = colors,
                typography = typography,
                spacing = spacing,
                shapes = shapes,
                animation = animations,
                accessibility = accessibility,
                warnings = warnings
            )

            // Parse variants if present
            val variants = parseVariants(
                parsed["variants"] as? Map<*, *>,
                theme,
                warnings
            )

            return ThemeParseResult.Success(
                theme = theme,
                config = config,
                variants = variants,
                warnings = warnings
            )
        } catch (e: Exception) {
            return ThemeParseResult.Error(
                message = "Failed to parse YAML: ${e.message}",
                line = null,
                column = null
            )
        }
    }

    /**
     * Parse YAML string into YamlThemeConfig
     *
     * @param yaml YAML theme definition
     * @return YamlThemeConfig object
     * @throws IllegalArgumentException if parsing fails
     */
    fun parseToConfig(yaml: String): YamlThemeConfig {
        if (yaml.isBlank()) {
            throw IllegalArgumentException("Empty YAML content")
        }

        val lines = yaml.lines()
        val parsed = parseYamlLines(lines)

        val themeMetadata = parsed["theme"] as? Map<*, *> ?: emptyMap<String, Any>()
        val colors = parsed["colors"] as? Map<*, *> ?: emptyMap<String, Any>()
        val typography = parsed["typography"] as? Map<*, *> ?: emptyMap<String, Any>()
        val spacing = parsed["spacing"] as? Map<*, *> ?: emptyMap<String, Any>()
        val shapes = parsed["shapes"] as? Map<*, *> ?: emptyMap<String, Any>()
        val elevation = parsed["elevation"] as? Map<*, *> ?: emptyMap<String, Any>()
        val borders = parsed["borders"] as? Map<*, *> ?: emptyMap<String, Any>()
        val sizes = parsed["sizes"] as? Map<*, *> ?: emptyMap<String, Any>()
        val animations = parsed["animations"] as? Map<*, *> ?: emptyMap<String, Any>()
        val accessibility = parsed["accessibility"] as? Map<*, *> ?: emptyMap<String, Any>()
        val opacity = parsed["opacity"] as? Map<*, *> ?: emptyMap<String, Any>()
        val text = parsed["text"] as? Map<*, *> ?: emptyMap<String, Any>()

        return buildYamlThemeConfig(
            themeMetadata = themeMetadata,
            colors = colors,
            typography = typography,
            spacing = spacing,
            shapes = shapes,
            elevation = elevation,
            borders = borders,
            sizes = sizes,
            animations = animations,
            accessibility = accessibility,
            opacity = opacity,
            text = text,
            variants = parsed["variants"] as? Map<*, *>
        )
    }

    /**
     * Convert YamlThemeConfig to OverlayTheme
     *
     * @param config YamlThemeConfig object
     * @return OverlayTheme instance
     */
    fun configToOverlayTheme(config: YamlThemeConfig): OverlayTheme {
        return OverlayTheme(
            // Colors
            primaryColor = parseColor(config.colors.primary) ?: OverlayTheme.DEFAULT.primaryColor,
            backgroundColor = parseColor(config.colors.background) ?: OverlayTheme.DEFAULT.backgroundColor,
            backdropColor = parseColor(config.colors.backdrop) ?: OverlayTheme.DEFAULT.backdropColor,
            textPrimaryColor = parseColor(config.colors.textPrimary) ?: OverlayTheme.DEFAULT.textPrimaryColor,
            textSecondaryColor = parseColor(config.colors.textSecondary) ?: OverlayTheme.DEFAULT.textSecondaryColor,
            textDisabledColor = parseColor(config.colors.textDisabled) ?: OverlayTheme.DEFAULT.textDisabledColor,
            borderColor = parseColor(config.colors.border) ?: OverlayTheme.DEFAULT.borderColor,
            dividerColor = parseColor(config.colors.divider) ?: OverlayTheme.DEFAULT.dividerColor,
            badgeEnabledWithNameColor = parseColor(config.colors.badgeEnabledWithName) ?: OverlayTheme.DEFAULT.badgeEnabledWithNameColor,
            badgeEnabledNoNameColor = parseColor(config.colors.badgeEnabledNoName) ?: OverlayTheme.DEFAULT.badgeEnabledNoNameColor,
            badgeDisabledColor = parseColor(config.colors.badgeDisabled) ?: OverlayTheme.DEFAULT.badgeDisabledColor,
            statusListeningColor = parseColor(config.colors.statusListening) ?: OverlayTheme.DEFAULT.statusListeningColor,
            statusProcessingColor = parseColor(config.colors.statusProcessing) ?: OverlayTheme.DEFAULT.statusProcessingColor,
            statusSuccessColor = parseColor(config.colors.statusSuccess) ?: OverlayTheme.DEFAULT.statusSuccessColor,
            statusErrorColor = parseColor(config.colors.statusError) ?: OverlayTheme.DEFAULT.statusErrorColor,
            cardBackgroundColor = parseColor(config.colors.cardBackground) ?: OverlayTheme.DEFAULT.cardBackgroundColor,
            tooltipBackgroundColor = parseColor(config.colors.tooltipBackground) ?: OverlayTheme.DEFAULT.tooltipBackgroundColor,
            focusIndicatorColor = parseColor(config.colors.focusIndicator) ?: OverlayTheme.DEFAULT.focusIndicatorColor,

            // Typography
            titleFontSize = config.typography.titleSize.toFloat(),
            bodyFontSize = config.typography.bodySize.toFloat(),
            captionFontSize = config.typography.captionSize.toFloat(),
            smallFontSize = config.typography.smallSize.toFloat(),
            badgeFontSize = config.typography.badgeSize.toFloat(),
            instructionFontSize = config.typography.instructionSize.toFloat(),

            // Spacing
            paddingSmall = config.spacing.paddingSmall.toFloat(),
            paddingMedium = config.spacing.paddingMedium.toFloat(),
            paddingLarge = config.spacing.paddingLarge.toFloat(),
            paddingXLarge = config.spacing.paddingXLarge.toFloat(),
            spacingTiny = config.spacing.spacingTiny.toFloat(),
            spacingSmall = config.spacing.spacingSmall.toFloat(),
            spacingMedium = config.spacing.spacingMedium.toFloat(),
            spacingLarge = config.spacing.spacingLarge.toFloat(),
            badgeOffsetX = config.spacing.badgeOffsetX.toFloat(),
            badgeOffsetY = config.spacing.badgeOffsetY.toFloat(),
            tooltipOffsetY = config.spacing.tooltipOffsetY.toFloat(),

            // Shapes
            cornerRadiusSmall = config.shapes.cornerRadiusSmall.toFloat(),
            cornerRadiusMedium = config.shapes.cornerRadiusMedium.toFloat(),
            cornerRadiusLarge = config.shapes.cornerRadiusLarge.toFloat(),
            cornerRadiusXLarge = config.shapes.cornerRadiusXLarge.toFloat(),
            cornerRadiusCircle = config.shapes.cornerRadiusCircle.toFloat(),

            // Elevation
            elevationLow = config.elevation.low.toFloat(),
            elevationMedium = config.elevation.medium.toFloat(),
            elevationHigh = config.elevation.high.toFloat(),

            // Borders
            borderWidthThin = config.borders.thin.toFloat(),
            borderWidthMedium = config.borders.medium.toFloat(),
            borderWidthThick = config.borders.thick.toFloat(),

            // Sizes
            badgeSize = config.sizes.badgeSize.toFloat(),
            badgeNumberSize = config.sizes.badgeNumberSize.toFloat(),
            iconSizeSmall = config.sizes.iconSmall.toFloat(),
            iconSizeMedium = config.sizes.iconMedium.toFloat(),
            iconSizeLarge = config.sizes.iconLarge.toFloat(),
            menuMinWidth = config.sizes.menuMinWidth.toFloat(),
            menuMaxWidth = config.sizes.menuMaxWidth.toFloat(),
            tooltipMaxWidth = config.sizes.tooltipMaxWidth.toFloat(),

            // Animations
            animationDurationFast = config.animations.durationFast,
            animationDurationNormal = config.animations.durationNormal,
            animationDurationSlow = config.animations.durationSlow,
            animationEnabled = config.animations.enabled,

            // Accessibility
            minimumContrastRatio = config.accessibility.minimumContrastRatio,
            focusIndicatorWidth = config.accessibility.focusIndicatorWidth.toFloat(),
            minimumTouchTargetSize = config.accessibility.minimumTouchTarget.toFloat(),

            // Text
            instructionTextDefault = config.text.instructionDefault,
            instructionTextMultiple = config.text.instructionMultiple,

            // Opacity
            alphaDisabled = config.opacity.disabled,
            alphaSecondary = config.opacity.secondary,
            alphaHint = config.opacity.hint,
            alphaDivider = config.opacity.divider,
            alphaBackdrop = config.opacity.backdrop,
            alphaTooltip = config.opacity.tooltip
        )
    }

    /**
     * Get variant OverlayTheme from config
     *
     * @param config YamlThemeConfig object
     * @param variantName Name of the variant
     * @return OverlayTheme for the variant, or base theme if variant not found
     */
    fun getVariantTheme(config: YamlThemeConfig, variantName: String): OverlayTheme {
        val variant = config.variants[variantName] ?: return configToOverlayTheme(config)
        return applyVariantToTheme(configToOverlayTheme(config), variant)
    }

    /**
     * Get list of available variant names
     */
    fun getAvailableVariants(config: YamlThemeConfig): List<String> {
        return config.variants.keys.toList()
    }

    /**
     * Parse color string to Long (ARGB format)
     *
     * Supports formats:
     * - "#RRGGBB" -> 0xFFRRGGBB (full alpha)
     * - "#AARRGGBB" -> 0xAARRGGBB
     * - "0xRRGGBB" -> 0xFFRRGGBB
     * - "0xAARRGGBB" -> 0xAARRGGBB
     *
     * @param colorStr Color string to parse
     * @return Parsed color as Long, or null if invalid
     */
    fun parseColor(colorStr: String): Long? {
        val trimmed = colorStr.trim()

        val hexValue = when {
            trimmed.startsWith(HEX_COLOR_PREFIX) -> trimmed.substring(1)
            trimmed.startsWith(HEX_COLOR_PREFIX_ALT, ignoreCase = true) -> trimmed.substring(2)
            else -> return null
        }

        return try {
            when (hexValue.length) {
                6 -> {
                    // RGB format, add full alpha
                    0xFF000000L or hexValue.toLong(16)
                }
                8 -> {
                    // ARGB format
                    hexValue.toLong(16)
                }
                else -> null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Convert color Long to hex string
     *
     * @param color Color as Long (ARGB format)
     * @return Hex string in "#AARRGGBB" format
     */
    fun colorToHexString(color: Long): String {
        return "#${color.toString(16).uppercase().padStart(8, '0')}"
    }

    // ===== Private Helper Functions =====

    /**
     * Simple YAML line parser (key: value format)
     * Note: This is a simplified parser for theme configs.
     * For complex YAML, consider using a proper YAML library.
     */
    private fun parseYamlLines(lines: List<String>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        var currentSection: String? = null
        var currentSubSection: String? = null
        val currentSectionMap = mutableMapOf<String, Any>()
        val currentSubSectionMap = mutableMapOf<String, Any>()

        for (line in lines) {
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val indent = line.length - line.trimStart().length

            when {
                // Top-level section (no indent, ends with colon)
                indent == 0 && trimmed.endsWith(":") -> {
                    // Save previous section
                    if (currentSection != null) {
                        if (currentSubSection != null && currentSubSectionMap.isNotEmpty()) {
                            currentSectionMap[currentSubSection] = currentSubSectionMap.toMap()
                        }
                        result[currentSection] = currentSectionMap.toMap()
                    }
                    currentSection = trimmed.dropLast(1)
                    currentSubSection = null
                    currentSectionMap.clear()
                    currentSubSectionMap.clear()
                }

                // Sub-section (2-space indent, ends with colon)
                indent == 2 && trimmed.endsWith(":") -> {
                    // Save previous sub-section
                    if (currentSubSection != null && currentSubSectionMap.isNotEmpty()) {
                        currentSectionMap[currentSubSection] = currentSubSectionMap.toMap()
                    }
                    currentSubSection = trimmed.dropLast(1)
                    currentSubSectionMap.clear()
                }

                // Key-value pair (2-space indent for section, 4-space for sub-section)
                trimmed.contains(":") -> {
                    val colonIndex = trimmed.indexOf(":")
                    val key = trimmed.substring(0, colonIndex).trim()
                    val value = trimmed.substring(colonIndex + 1).trim()
                        .removeSurrounding("\"")
                        .removeSurrounding("'")

                    when {
                        indent >= 4 && currentSubSection != null -> {
                            currentSubSectionMap[key] = parseValue(value)
                        }
                        indent >= 2 && currentSection != null -> {
                            currentSectionMap[key] = parseValue(value)
                        }
                        else -> {
                            result[key] = parseValue(value)
                        }
                    }
                }
            }
        }

        // Save final section
        if (currentSection != null) {
            if (currentSubSection != null && currentSubSectionMap.isNotEmpty()) {
                currentSectionMap[currentSubSection] = currentSubSectionMap.toMap()
            }
            result[currentSection] = currentSectionMap.toMap()
        }

        return result
    }

    /**
     * Parse value from string to appropriate type
     */
    private fun parseValue(value: String): Any {
        return when {
            value.isEmpty() -> ""
            value == "true" -> true
            value == "false" -> false
            value.startsWith(HEX_COLOR_PREFIX) || value.startsWith(HEX_COLOR_PREFIX_ALT, ignoreCase = true) -> {
                parseColor(value) ?: value
            }
            value.toIntOrNull() != null -> value.toInt()
            value.toFloatOrNull() != null -> value.toFloat()
            value.toLongOrNull() != null -> value.toLong()
            else -> value
        }
    }

    /**
     * Build OverlayTheme from parsed YAML sections
     */
    private fun buildThemeFromParsed(
        baseTheme: OverlayTheme,
        colors: Map<*, *>,
        typography: Map<*, *>,
        spacing: Map<*, *>,
        shapes: Map<*, *>,
        animation: Map<*, *>,
        accessibility: Map<*, *>,
        warnings: MutableList<String>
    ): OverlayTheme {
        return baseTheme.copy(
            // Colors
            primaryColor = getColorValue(colors, "primary", baseTheme.primaryColor, warnings),
            backgroundColor = getColorValue(colors, "background", baseTheme.backgroundColor, warnings),
            backdropColor = getColorValue(colors, "backdrop", baseTheme.backdropColor, warnings),
            textPrimaryColor = getColorValue(colors, "textPrimary", baseTheme.textPrimaryColor, warnings),
            textSecondaryColor = getColorValue(colors, "textSecondary", baseTheme.textSecondaryColor, warnings),
            textDisabledColor = getColorValue(colors, "textDisabled", baseTheme.textDisabledColor, warnings),
            borderColor = getColorValue(colors, "border", baseTheme.borderColor, warnings),
            dividerColor = getColorValue(colors, "divider", baseTheme.dividerColor, warnings),
            badgeEnabledWithNameColor = getColorValue(colors, "badgeEnabledWithName", baseTheme.badgeEnabledWithNameColor, warnings),
            badgeEnabledNoNameColor = getColorValue(colors, "badgeEnabledNoName", baseTheme.badgeEnabledNoNameColor, warnings),
            badgeDisabledColor = getColorValue(colors, "badgeDisabled", baseTheme.badgeDisabledColor, warnings),
            statusListeningColor = getColorValue(colors, "statusListening", baseTheme.statusListeningColor, warnings),
            statusProcessingColor = getColorValue(colors, "statusProcessing", baseTheme.statusProcessingColor, warnings),
            statusSuccessColor = getColorValue(colors, "statusSuccess", baseTheme.statusSuccessColor, warnings),
            statusErrorColor = getColorValue(colors, "statusError", baseTheme.statusErrorColor, warnings),
            cardBackgroundColor = getColorValue(colors, "cardBackground", baseTheme.cardBackgroundColor, warnings),
            tooltipBackgroundColor = getColorValue(colors, "tooltipBackground", baseTheme.tooltipBackgroundColor, warnings),
            focusIndicatorColor = getColorValue(colors, "focusIndicator", baseTheme.focusIndicatorColor, warnings),

            // Typography
            titleFontSize = getFloatValue(typography, "titleSize", baseTheme.titleFontSize),
            bodyFontSize = getFloatValue(typography, "bodySize", baseTheme.bodyFontSize),
            captionFontSize = getFloatValue(typography, "captionSize", baseTheme.captionFontSize),
            smallFontSize = getFloatValue(typography, "smallSize", baseTheme.smallFontSize),
            badgeFontSize = getFloatValue(typography, "badgeSize", baseTheme.badgeFontSize),
            instructionFontSize = getFloatValue(typography, "instructionSize", baseTheme.instructionFontSize),

            // Spacing
            paddingSmall = getFloatValue(spacing, "paddingSmall", baseTheme.paddingSmall),
            paddingMedium = getFloatValue(spacing, "paddingMedium", baseTheme.paddingMedium),
            paddingLarge = getFloatValue(spacing, "paddingLarge", baseTheme.paddingLarge),
            paddingXLarge = getFloatValue(spacing, "paddingXLarge", baseTheme.paddingXLarge),
            spacingTiny = getFloatValue(spacing, "spacingTiny", baseTheme.spacingTiny),
            spacingSmall = getFloatValue(spacing, "spacingSmall", baseTheme.spacingSmall),
            spacingMedium = getFloatValue(spacing, "spacingMedium", baseTheme.spacingMedium),
            spacingLarge = getFloatValue(spacing, "spacingLarge", baseTheme.spacingLarge),
            badgeOffsetX = getFloatValue(spacing, "badgeOffsetX", baseTheme.badgeOffsetX),
            badgeOffsetY = getFloatValue(spacing, "badgeOffsetY", baseTheme.badgeOffsetY),
            tooltipOffsetY = getFloatValue(spacing, "tooltipOffsetY", baseTheme.tooltipOffsetY),

            // Shapes
            cornerRadiusSmall = getFloatValue(shapes, "cornerRadiusSmall", baseTheme.cornerRadiusSmall),
            cornerRadiusMedium = getFloatValue(shapes, "cornerRadiusMedium", baseTheme.cornerRadiusMedium),
            cornerRadiusLarge = getFloatValue(shapes, "cornerRadiusLarge", baseTheme.cornerRadiusLarge),
            cornerRadiusXLarge = getFloatValue(shapes, "cornerRadiusXLarge", baseTheme.cornerRadiusXLarge),
            cornerRadiusCircle = getFloatValue(shapes, "cornerRadiusCircle", baseTheme.cornerRadiusCircle),
            elevationLow = getFloatValue(shapes, "elevationLow", baseTheme.elevationLow),
            elevationMedium = getFloatValue(shapes, "elevationMedium", baseTheme.elevationMedium),
            elevationHigh = getFloatValue(shapes, "elevationHigh", baseTheme.elevationHigh),
            borderWidthThin = getFloatValue(shapes, "borderWidthThin", baseTheme.borderWidthThin),
            borderWidthMedium = getFloatValue(shapes, "borderWidthMedium", baseTheme.borderWidthMedium),
            borderWidthThick = getFloatValue(shapes, "borderWidthThick", baseTheme.borderWidthThick),
            badgeSize = getFloatValue(shapes, "badgeSize", baseTheme.badgeSize),
            badgeNumberSize = getFloatValue(shapes, "badgeNumberSize", baseTheme.badgeNumberSize),
            iconSizeSmall = getFloatValue(shapes, "iconSizeSmall", baseTheme.iconSizeSmall),
            iconSizeMedium = getFloatValue(shapes, "iconSizeMedium", baseTheme.iconSizeMedium),
            iconSizeLarge = getFloatValue(shapes, "iconSizeLarge", baseTheme.iconSizeLarge),
            menuMinWidth = getFloatValue(shapes, "menuMinWidth", baseTheme.menuMinWidth),
            menuMaxWidth = getFloatValue(shapes, "menuMaxWidth", baseTheme.menuMaxWidth),
            tooltipMaxWidth = getFloatValue(shapes, "tooltipMaxWidth", baseTheme.tooltipMaxWidth),

            // Animation
            animationDurationFast = getIntValue(animation, "durationFast", baseTheme.animationDurationFast),
            animationDurationNormal = getIntValue(animation, "durationNormal", baseTheme.animationDurationNormal),
            animationDurationSlow = getIntValue(animation, "durationSlow", baseTheme.animationDurationSlow),
            animationEnabled = getBoolValue(animation, "enabled", baseTheme.animationEnabled),

            // Accessibility
            minimumContrastRatio = getFloatValue(accessibility, "minimumContrastRatio", baseTheme.minimumContrastRatio),
            focusIndicatorWidth = getFloatValue(accessibility, "focusIndicatorWidth", baseTheme.focusIndicatorWidth),
            minimumTouchTargetSize = getFloatValue(accessibility, "minimumTouchTargetSize", baseTheme.minimumTouchTargetSize),

            // Alpha values
            alphaDisabled = getFloatValue(accessibility, "alphaDisabled", baseTheme.alphaDisabled),
            alphaSecondary = getFloatValue(accessibility, "alphaSecondary", baseTheme.alphaSecondary),
            alphaHint = getFloatValue(accessibility, "alphaHint", baseTheme.alphaHint),
            alphaDivider = getFloatValue(accessibility, "alphaDivider", baseTheme.alphaDivider),
            alphaBackdrop = getFloatValue(accessibility, "alphaBackdrop", baseTheme.alphaBackdrop)
        )
    }

    /**
     * Parse theme variants
     */
    private fun parseVariants(
        variantsMap: Map<*, *>?,
        baseTheme: OverlayTheme,
        warnings: MutableList<String>
    ): Map<String, OverlayTheme> {
        if (variantsMap == null) return emptyMap()

        val variants = mutableMapOf<String, OverlayTheme>()

        for ((key, value) in variantsMap) {
            val variantName = key.toString()
            val variantConfig = value as? Map<*, *> ?: continue

            val colors = variantConfig["colors"] as? Map<*, *> ?: emptyMap<String, Any>()
            val typography = variantConfig["typography"] as? Map<*, *> ?: emptyMap<String, Any>()
            val spacing = variantConfig["spacing"] as? Map<*, *> ?: emptyMap<String, Any>()
            val shapes = variantConfig["shapes"] as? Map<*, *> ?: emptyMap<String, Any>()
            val animation = variantConfig["animation"] as? Map<*, *> ?: emptyMap<String, Any>()
            val accessibility = variantConfig["accessibility"] as? Map<*, *> ?: emptyMap<String, Any>()

            variants[variantName] = buildThemeFromParsed(
                baseTheme = baseTheme,
                colors = colors,
                typography = typography,
                spacing = spacing,
                shapes = shapes,
                animation = animation,
                accessibility = accessibility,
                warnings = warnings
            )
        }

        return variants
    }

    /**
     * Get color value from map, parsing if needed
     */
    private fun getColorValue(
        map: Map<*, *>,
        key: String,
        default: Long,
        warnings: MutableList<String>
    ): Long {
        val value = map[key] ?: return default

        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is String -> {
                parseColor(value) ?: run {
                    warnings.add("Invalid color format for '$key': $value")
                    default
                }
            }
            else -> {
                warnings.add("Unexpected type for color '$key': ${value::class.simpleName}")
                default
            }
        }
    }

    /**
     * Get float value from map
     */
    private fun getFloatValue(map: Map<*, *>, key: String, default: Float): Float {
        val value = map[key] ?: return default

        return when (value) {
            is Float -> value
            is Double -> value.toFloat()
            is Int -> value.toFloat()
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull() ?: default
            else -> default
        }
    }

    /**
     * Get int value from map
     */
    private fun getIntValue(map: Map<*, *>, key: String, default: Int): Int {
        val value = map[key] ?: return default

        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Float -> value.toInt()
            is Double -> value.toInt()
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            else -> default
        }
    }

    /**
     * Get boolean value from map
     */
    private fun getBoolValue(map: Map<*, *>, key: String, default: Boolean): Boolean {
        val value = map[key] ?: return default

        return when (value) {
            is Boolean -> value
            is String -> value.lowercase() == "true"
            else -> default
        }
    }

    /**
     * Get string value from map
     */
    private fun getStringValue(map: Map<*, *>, key: String, default: String): String {
        val value = map[key] ?: return default
        return value.toString()
    }

    // ===== YamlThemeConfig Building =====

    /**
     * Build YamlThemeConfig from parsed YAML sections
     */
    private fun buildYamlThemeConfig(
        themeMetadata: Map<*, *>,
        colors: Map<*, *>,
        typography: Map<*, *>,
        spacing: Map<*, *>,
        shapes: Map<*, *>,
        elevation: Map<*, *>,
        borders: Map<*, *>,
        sizes: Map<*, *>,
        animations: Map<*, *>,
        accessibility: Map<*, *>,
        opacity: Map<*, *>,
        text: Map<*, *>,
        variants: Map<*, *>?
    ): YamlThemeConfig {
        return YamlThemeConfig(
            theme = buildThemeMetadata(themeMetadata),
            colors = buildColorConfig(colors),
            typography = buildTypographyConfig(typography),
            spacing = buildSpacingConfig(spacing),
            shapes = buildShapeConfig(shapes),
            elevation = buildElevationConfig(elevation),
            borders = buildBorderConfig(borders),
            sizes = buildSizeConfig(sizes),
            animations = buildAnimationConfig(animations),
            accessibility = buildAccessibilityConfig(accessibility),
            opacity = buildOpacityConfig(opacity),
            text = buildTextConfig(text),
            variants = buildVariantsConfig(variants)
        )
    }

    private fun buildThemeMetadata(map: Map<*, *>): ThemeMetadata {
        return ThemeMetadata(
            name = getStringValue(map, "name", ThemeMetadata().name),
            version = getStringValue(map, "version", ThemeMetadata().version),
            inherit = map["inherit"]?.toString(),
            platform = getStringValue(map, "platform", ThemeMetadata().platform)
        )
    }

    private fun buildColorConfig(map: Map<*, *>): ColorConfig {
        val default = ColorConfig()
        return ColorConfig(
            primary = getStringValue(map, "primary", default.primary),
            primaryVariant = getStringValue(map, "primaryVariant", default.primaryVariant),
            onPrimary = getStringValue(map, "onPrimary", default.onPrimary),
            background = getStringValue(map, "background", default.background),
            backgroundSolid = getStringValue(map, "backgroundSolid", default.backgroundSolid),
            backdrop = getStringValue(map, "backdrop", default.backdrop),
            surface = getStringValue(map, "surface", default.surface),
            surfaceVariant = getStringValue(map, "surfaceVariant", default.surfaceVariant),
            textPrimary = getStringValue(map, "textPrimary", default.textPrimary),
            textSecondary = getStringValue(map, "textSecondary", default.textSecondary),
            textDisabled = getStringValue(map, "textDisabled", default.textDisabled),
            textHint = getStringValue(map, "textHint", default.textHint),
            border = getStringValue(map, "border", default.border),
            divider = getStringValue(map, "divider", default.divider),
            badgeEnabledWithName = getStringValue(map, "badgeEnabledWithName", default.badgeEnabledWithName),
            badgeEnabledNoName = getStringValue(map, "badgeEnabledNoName", default.badgeEnabledNoName),
            badgeDisabled = getStringValue(map, "badgeDisabled", default.badgeDisabled),
            statusListening = getStringValue(map, "statusListening", default.statusListening),
            statusProcessing = getStringValue(map, "statusProcessing", default.statusProcessing),
            statusSuccess = getStringValue(map, "statusSuccess", default.statusSuccess),
            statusError = getStringValue(map, "statusError", default.statusError),
            confidenceHigh = getStringValue(map, "confidenceHigh", default.confidenceHigh),
            confidenceMedium = getStringValue(map, "confidenceMedium", default.confidenceMedium),
            confidenceLow = getStringValue(map, "confidenceLow", default.confidenceLow),
            confidenceReject = getStringValue(map, "confidenceReject", default.confidenceReject),
            cardBackground = getStringValue(map, "cardBackground", default.cardBackground),
            tooltipBackground = getStringValue(map, "tooltipBackground", default.tooltipBackground),
            focusIndicator = getStringValue(map, "focusIndicator", default.focusIndicator)
        )
    }

    private fun buildTypographyConfig(map: Map<*, *>): TypographyConfig {
        val default = TypographyConfig()
        return TypographyConfig(
            titleSize = getIntValue(map, "titleSize", default.titleSize),
            bodySize = getIntValue(map, "bodySize", default.bodySize),
            captionSize = getIntValue(map, "captionSize", default.captionSize),
            smallSize = getIntValue(map, "smallSize", default.smallSize),
            badgeSize = getIntValue(map, "badgeSize", default.badgeSize),
            instructionSize = getIntValue(map, "instructionSize", default.instructionSize),
            fontWeightRegular = getIntValue(map, "fontWeightRegular", default.fontWeightRegular),
            fontWeightMedium = getIntValue(map, "fontWeightMedium", default.fontWeightMedium),
            fontWeightBold = getIntValue(map, "fontWeightBold", default.fontWeightBold)
        )
    }

    private fun buildSpacingConfig(map: Map<*, *>): SpacingConfig {
        val default = SpacingConfig()
        return SpacingConfig(
            paddingSmall = getIntValue(map, "paddingSmall", default.paddingSmall),
            paddingMedium = getIntValue(map, "paddingMedium", default.paddingMedium),
            paddingLarge = getIntValue(map, "paddingLarge", default.paddingLarge),
            paddingXLarge = getIntValue(map, "paddingXLarge", default.paddingXLarge),
            spacingTiny = getIntValue(map, "spacingTiny", default.spacingTiny),
            spacingSmall = getIntValue(map, "spacingSmall", default.spacingSmall),
            spacingMedium = getIntValue(map, "spacingMedium", default.spacingMedium),
            spacingLarge = getIntValue(map, "spacingLarge", default.spacingLarge),
            badgeOffsetX = getIntValue(map, "badgeOffsetX", default.badgeOffsetX),
            badgeOffsetY = getIntValue(map, "badgeOffsetY", default.badgeOffsetY),
            tooltipOffsetY = getIntValue(map, "tooltipOffsetY", default.tooltipOffsetY)
        )
    }

    private fun buildShapeConfig(map: Map<*, *>): ShapeConfig {
        val default = ShapeConfig()
        return ShapeConfig(
            cornerRadiusSmall = getIntValue(map, "cornerRadiusSmall", default.cornerRadiusSmall),
            cornerRadiusMedium = getIntValue(map, "cornerRadiusMedium", default.cornerRadiusMedium),
            cornerRadiusLarge = getIntValue(map, "cornerRadiusLarge", default.cornerRadiusLarge),
            cornerRadiusXLarge = getIntValue(map, "cornerRadiusXLarge", default.cornerRadiusXLarge),
            cornerRadiusCircle = getIntValue(map, "cornerRadiusCircle", default.cornerRadiusCircle)
        )
    }

    private fun buildElevationConfig(map: Map<*, *>): ElevationConfig {
        val default = ElevationConfig()
        return ElevationConfig(
            low = getIntValue(map, "low", default.low),
            medium = getIntValue(map, "medium", default.medium),
            high = getIntValue(map, "high", default.high)
        )
    }

    private fun buildBorderConfig(map: Map<*, *>): BorderConfig {
        val default = BorderConfig()
        return BorderConfig(
            thin = getIntValue(map, "thin", default.thin),
            medium = getIntValue(map, "medium", default.medium),
            thick = getIntValue(map, "thick", default.thick)
        )
    }

    private fun buildSizeConfig(map: Map<*, *>): SizeConfig {
        val default = SizeConfig()
        return SizeConfig(
            badgeSize = getIntValue(map, "badgeSize", default.badgeSize),
            badgeNumberSize = getIntValue(map, "badgeNumberSize", default.badgeNumberSize),
            badgeRadius = getIntValue(map, "badgeRadius", default.badgeRadius),
            iconSmall = getIntValue(map, "iconSmall", default.iconSmall),
            iconMedium = getIntValue(map, "iconMedium", default.iconMedium),
            iconLarge = getIntValue(map, "iconLarge", default.iconLarge),
            menuMinWidth = getIntValue(map, "menuMinWidth", default.menuMinWidth),
            menuMaxWidth = getIntValue(map, "menuMaxWidth", default.menuMaxWidth),
            tooltipMaxWidth = getIntValue(map, "tooltipMaxWidth", default.tooltipMaxWidth)
        )
    }

    private fun buildAnimationConfig(map: Map<*, *>): AnimationConfig {
        val default = AnimationConfig()
        return AnimationConfig(
            durationFast = getIntValue(map, "durationFast", default.durationFast),
            durationNormal = getIntValue(map, "durationNormal", default.durationNormal),
            durationSlow = getIntValue(map, "durationSlow", default.durationSlow),
            enabled = getBoolValue(map, "enabled", default.enabled),
            easingDefault = getStringValue(map, "easingDefault", default.easingDefault),
            easingEnter = getStringValue(map, "easingEnter", default.easingEnter),
            easingExit = getStringValue(map, "easingExit", default.easingExit)
        )
    }

    private fun buildAccessibilityConfig(map: Map<*, *>): ThemeAccessibilityConfig {
        val default = ThemeAccessibilityConfig()
        return ThemeAccessibilityConfig(
            minimumContrastRatio = getFloatValue(map, "minimumContrastRatio", default.minimumContrastRatio),
            focusIndicatorWidth = getIntValue(map, "focusIndicatorWidth", default.focusIndicatorWidth),
            minimumTouchTarget = getIntValue(map, "minimumTouchTarget", default.minimumTouchTarget),
            confidenceHighThreshold = getFloatValue(map, "confidenceHighThreshold", default.confidenceHighThreshold),
            confidenceMediumThreshold = getFloatValue(map, "confidenceMediumThreshold", default.confidenceMediumThreshold),
            confidenceLowThreshold = getFloatValue(map, "confidenceLowThreshold", default.confidenceLowThreshold)
        )
    }

    private fun buildOpacityConfig(map: Map<*, *>): OpacityConfig {
        val default = OpacityConfig()
        return OpacityConfig(
            disabled = getFloatValue(map, "disabled", default.disabled),
            secondary = getFloatValue(map, "secondary", default.secondary),
            hint = getFloatValue(map, "hint", default.hint),
            divider = getFloatValue(map, "divider", default.divider),
            backdrop = getFloatValue(map, "backdrop", default.backdrop),
            tooltip = getFloatValue(map, "tooltip", default.tooltip)
        )
    }

    private fun buildTextConfig(map: Map<*, *>): TextConfig {
        val default = TextConfig()
        return TextConfig(
            instructionDefault = getStringValue(map, "instructionDefault", default.instructionDefault),
            instructionMultiple = getStringValue(map, "instructionMultiple", default.instructionMultiple),
            listeningPrompt = getStringValue(map, "listeningPrompt", default.listeningPrompt),
            processingPrompt = getStringValue(map, "processingPrompt", default.processingPrompt),
            successPrompt = getStringValue(map, "successPrompt", default.successPrompt),
            errorPrompt = getStringValue(map, "errorPrompt", default.errorPrompt)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildVariantsConfig(variantsMap: Map<*, *>?): Map<String, YamlThemeVariant> {
        if (variantsMap == null) return emptyMap()

        val variants = mutableMapOf<String, YamlThemeVariant>()

        for ((key, value) in variantsMap) {
            val variantName = key.toString()
            val variantConfig = value as? Map<*, *> ?: continue

            val colorsMap = variantConfig["colors"] as? Map<*, *>
            val typographyMap = variantConfig["typography"] as? Map<*, *>
            val spacingMap = variantConfig["spacing"] as? Map<*, *>
            val shapesMap = variantConfig["shapes"] as? Map<*, *>
            val elevationMap = variantConfig["elevation"] as? Map<*, *>
            val bordersMap = variantConfig["borders"] as? Map<*, *>
            val sizesMap = variantConfig["sizes"] as? Map<*, *>
            val animationsMap = variantConfig["animations"] as? Map<*, *>
            val accessibilityMap = variantConfig["accessibility"] as? Map<*, *>

            variants[variantName] = YamlThemeVariant(
                inherit = variantConfig["inherit"]?.toString(),
                colors = colorsMap?.mapKeys { it.key.toString() }?.mapValues { it.value?.toString() ?: "" } ?: emptyMap(),
                typography = typographyMap?.mapKeys { it.key.toString() }?.mapValues { (it.value as? Number)?.toInt() ?: 0 } ?: emptyMap(),
                spacing = spacingMap?.mapKeys { it.key.toString() }?.mapValues { (it.value as? Number)?.toInt() ?: 0 } ?: emptyMap(),
                shapes = shapesMap?.mapKeys { it.key.toString() }?.mapValues { (it.value as? Number)?.toInt() ?: 0 } ?: emptyMap(),
                elevation = elevationMap?.mapKeys { it.key.toString() }?.mapValues { (it.value as? Number)?.toInt() ?: 0 } ?: emptyMap(),
                borders = bordersMap?.mapKeys { it.key.toString() }?.mapValues { (it.value as? Number)?.toInt() ?: 0 } ?: emptyMap(),
                sizes = sizesMap?.mapKeys { it.key.toString() }?.mapValues { (it.value as? Number)?.toInt() ?: 0 } ?: emptyMap(),
                animations = animationsMap?.mapKeys { it.key.toString() }?.mapValues { it.value?.toString() ?: "" } ?: emptyMap(),
                accessibility = accessibilityMap?.mapKeys { it.key.toString() }?.mapValues { it.value?.toString() ?: "" } ?: emptyMap()
            )
        }

        return variants
    }

    // ===== Variant Application =====

    /**
     * Apply a YamlThemeVariant to an OverlayTheme
     */
    private fun applyVariantToTheme(base: OverlayTheme, variant: YamlThemeVariant): OverlayTheme {
        var result = base

        // Apply color overrides
        variant.colors.forEach { (key, value) ->
            val color = parseColor(value) ?: return@forEach
            result = when (key) {
                "primary" -> result.copy(primaryColor = color, focusIndicatorColor = color)
                "background" -> result.copy(backgroundColor = color)
                "backdrop" -> result.copy(backdropColor = color)
                "textPrimary" -> result.copy(textPrimaryColor = color)
                "textSecondary" -> result.copy(textSecondaryColor = color)
                "textDisabled" -> result.copy(textDisabledColor = color)
                "border" -> result.copy(borderColor = color)
                "divider" -> result.copy(dividerColor = color)
                "badgeEnabledWithName" -> result.copy(badgeEnabledWithNameColor = color)
                "badgeEnabledNoName" -> result.copy(badgeEnabledNoNameColor = color)
                "badgeDisabled" -> result.copy(badgeDisabledColor = color)
                "statusListening" -> result.copy(statusListeningColor = color)
                "statusProcessing" -> result.copy(statusProcessingColor = color)
                "statusSuccess" -> result.copy(statusSuccessColor = color)
                "statusError" -> result.copy(statusErrorColor = color)
                "cardBackground" -> result.copy(cardBackgroundColor = color)
                "tooltipBackground" -> result.copy(tooltipBackgroundColor = color)
                "focusIndicator" -> result.copy(focusIndicatorColor = color)
                else -> result
            }
        }

        // Apply typography overrides
        variant.typography.forEach { (key, value) ->
            result = when (key) {
                "titleSize" -> result.copy(titleFontSize = value.toFloat())
                "bodySize" -> result.copy(bodyFontSize = value.toFloat())
                "captionSize" -> result.copy(captionFontSize = value.toFloat())
                "smallSize" -> result.copy(smallFontSize = value.toFloat())
                "badgeSize" -> result.copy(badgeFontSize = value.toFloat())
                "instructionSize" -> result.copy(instructionFontSize = value.toFloat())
                else -> result
            }
        }

        // Apply spacing overrides
        variant.spacing.forEach { (key, value) ->
            result = when (key) {
                "paddingSmall" -> result.copy(paddingSmall = value.toFloat())
                "paddingMedium" -> result.copy(paddingMedium = value.toFloat())
                "paddingLarge" -> result.copy(paddingLarge = value.toFloat())
                "paddingXLarge" -> result.copy(paddingXLarge = value.toFloat())
                else -> result
            }
        }

        // Apply size overrides
        variant.sizes.forEach { (key, value) ->
            result = when (key) {
                "badgeSize" -> result.copy(badgeSize = value.toFloat())
                "iconSmall" -> result.copy(iconSizeSmall = value.toFloat())
                "iconMedium" -> result.copy(iconSizeMedium = value.toFloat())
                "iconLarge" -> result.copy(iconSizeLarge = value.toFloat())
                else -> result
            }
        }

        // Apply shape overrides
        variant.shapes.forEach { (key, value) ->
            result = when (key) {
                "cornerRadiusSmall" -> result.copy(cornerRadiusSmall = value.toFloat())
                "cornerRadiusMedium" -> result.copy(cornerRadiusMedium = value.toFloat())
                "cornerRadiusLarge" -> result.copy(cornerRadiusLarge = value.toFloat())
                else -> result
            }
        }

        // Apply elevation overrides
        variant.elevation.forEach { (key, value) ->
            result = when (key) {
                "low" -> result.copy(elevationLow = value.toFloat())
                "medium" -> result.copy(elevationMedium = value.toFloat())
                "high" -> result.copy(elevationHigh = value.toFloat())
                else -> result
            }
        }

        // Apply border overrides
        variant.borders.forEach { (key, value) ->
            result = when (key) {
                "thin" -> result.copy(borderWidthThin = value.toFloat())
                "medium" -> result.copy(borderWidthMedium = value.toFloat())
                "thick" -> result.copy(borderWidthThick = value.toFloat())
                else -> result
            }
        }

        // Apply animation overrides
        variant.animations.forEach { (key, value) ->
            result = when (key) {
                "durationFast" -> result.copy(animationDurationFast = (value as? Number)?.toInt() ?: result.animationDurationFast)
                "durationNormal" -> result.copy(animationDurationNormal = (value as? Number)?.toInt() ?: result.animationDurationNormal)
                "durationSlow" -> result.copy(animationDurationSlow = (value as? Number)?.toInt() ?: result.animationDurationSlow)
                "enabled" -> result.copy(animationEnabled = value as? Boolean ?: result.animationEnabled)
                else -> result
            }
        }

        // Apply accessibility overrides
        variant.accessibility.forEach { (key, value) ->
            result = when (key) {
                "minimumContrastRatio" -> result.copy(minimumContrastRatio = (value as? Number)?.toFloat() ?: result.minimumContrastRatio)
                "minimumTouchTarget" -> result.copy(minimumTouchTargetSize = (value as? Number)?.toFloat() ?: result.minimumTouchTargetSize)
                "focusIndicatorWidth" -> result.copy(focusIndicatorWidth = (value as? Number)?.toFloat() ?: result.focusIndicatorWidth)
                else -> result
            }
        }

        return result
    }

    // ===== Companion Object for Static Access =====

    /**
     * Default theme file path relative to project resources
     */
    const val DEFAULT_THEME_PATH = "Common/UI/Theme/VoiceOSCoreNGTheme.yaml"

    /**
     * Quick parse helper - parses YAML and returns OverlayTheme
     */
    fun parseToTheme(yamlContent: String): OverlayTheme {
        return when (val result = parse(yamlContent)) {
            is ThemeParseResult.Success -> result.theme
            is ThemeParseResult.Error -> OverlayTheme.DEFAULT
        }
    }

    /**
     * Quick parse helper with variant - parses YAML and returns OverlayTheme with variant applied
     */
    fun parseToTheme(yamlContent: String, variantName: String): OverlayTheme {
        return when (val result = parse(yamlContent)) {
            is ThemeParseResult.Success -> result.variants[variantName] ?: result.theme
            is ThemeParseResult.Error -> OverlayTheme.DEFAULT
        }
    }
}
