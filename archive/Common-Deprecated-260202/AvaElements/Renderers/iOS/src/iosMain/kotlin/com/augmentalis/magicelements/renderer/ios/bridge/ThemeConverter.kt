package com.augmentalis.avaelements.renderer.ios.bridge

import com.augmentalis.avaelements.core.*

/**
 * Converts AvaElements themes to iOS design tokens
 *
 * This converter transforms cross-platform theme definitions into
 * iOS-specific design tokens that can be consumed by SwiftUI.
 */
class iOSThemeConverter {

    /**
     * Theme design tokens for iOS
     */
    data class iOSDesignTokens(
        val colors: Map<String, SwiftUIColor>,
        val fonts: Map<String, FontDefinition>,
        val shapes: Map<String, Float>,
        val spacing: Map<String, Float>,
        val elevation: Map<String, ShadowValueWithColor>,
        val material: MaterialTokens?
    )

    /**
     * Font definition for iOS
     */
    data class FontDefinition(
        val family: String,
        val size: Float,
        val weight: FontWeight,
        val style: FontStyle
    )

    /**
     * Material effect tokens (glass, mica, etc.)
     */
    data class MaterialTokens(
        val glassBlurRadius: Float?,
        val glassTintColor: SwiftUIColor?,
        val glassThickness: Float?,
        val micaBaseColor: SwiftUIColor?,
        val micaTintOpacity: Float?
    )

    /**
     * Convert a AvaElements theme to iOS design tokens
     */
    fun convert(theme: Theme): iOSDesignTokens {
        return iOSDesignTokens(
            colors = toSwiftUIColors(theme.colorScheme),
            fonts = toSwiftUIFonts(theme.typography),
            shapes = toSwiftUIShapes(theme.shapes),
            spacing = toSwiftUISpacing(theme.spacing),
            elevation = toSwiftUIElevation(theme.elevation),
            material = theme.material?.let { toMaterialTokens(it) }
        )
    }

    /**
     * Convert ColorScheme to SwiftUI semantic colors
     */
    fun toSwiftUIColors(colorScheme: ColorScheme): Map<String, SwiftUIColor> {
        return mapOf(
            // Primary colors
            "primary" to ModifierConverter.convertColor(colorScheme.primary),
            "onPrimary" to ModifierConverter.convertColor(colorScheme.onPrimary),
            "primaryContainer" to ModifierConverter.convertColor(colorScheme.primaryContainer),
            "onPrimaryContainer" to ModifierConverter.convertColor(colorScheme.onPrimaryContainer),

            // Secondary colors
            "secondary" to ModifierConverter.convertColor(colorScheme.secondary),
            "onSecondary" to ModifierConverter.convertColor(colorScheme.onSecondary),
            "secondaryContainer" to ModifierConverter.convertColor(colorScheme.secondaryContainer),
            "onSecondaryContainer" to ModifierConverter.convertColor(colorScheme.onSecondaryContainer),

            // Tertiary colors
            "tertiary" to ModifierConverter.convertColor(colorScheme.tertiary),
            "onTertiary" to ModifierConverter.convertColor(colorScheme.onTertiary),
            "tertiaryContainer" to ModifierConverter.convertColor(colorScheme.tertiaryContainer),
            "onTertiaryContainer" to ModifierConverter.convertColor(colorScheme.onTertiaryContainer),

            // Error colors
            "error" to ModifierConverter.convertColor(colorScheme.error),
            "onError" to ModifierConverter.convertColor(colorScheme.onError),
            "errorContainer" to ModifierConverter.convertColor(colorScheme.errorContainer),
            "onErrorContainer" to ModifierConverter.convertColor(colorScheme.onErrorContainer),

            // Surface colors
            "surface" to ModifierConverter.convertColor(colorScheme.surface),
            "onSurface" to ModifierConverter.convertColor(colorScheme.onSurface),
            "surfaceVariant" to ModifierConverter.convertColor(colorScheme.surfaceVariant),
            "onSurfaceVariant" to ModifierConverter.convertColor(colorScheme.onSurfaceVariant),

            // Background colors
            "background" to ModifierConverter.convertColor(colorScheme.background),
            "onBackground" to ModifierConverter.convertColor(colorScheme.onBackground),

            // Outline colors
            "outline" to ModifierConverter.convertColor(colorScheme.outline),
            "outlineVariant" to ModifierConverter.convertColor(colorScheme.outlineVariant),

            // Special colors
            "scrim" to ModifierConverter.convertColor(colorScheme.scrim)
        ).let { baseMap ->
            // Add optional colors if present
            val optionalColors = mutableMapOf<String, SwiftUIColor>()
            colorScheme.surfaceTint?.let {
                optionalColors["surfaceTint"] = ModifierConverter.convertColor(it)
            }
            colorScheme.inverseSurface?.let {
                optionalColors["inverseSurface"] = ModifierConverter.convertColor(it)
            }
            colorScheme.inverseOnSurface?.let {
                optionalColors["inverseOnSurface"] = ModifierConverter.convertColor(it)
            }
            colorScheme.inversePrimary?.let {
                optionalColors["inversePrimary"] = ModifierConverter.convertColor(it)
            }
            baseMap + optionalColors
        }
    }

    /**
     * Convert Typography to SwiftUI font definitions
     */
    fun toSwiftUIFonts(typography: Typography): Map<String, FontDefinition> {
        return mapOf(
            "displayLarge" to convertFont(typography.displayLarge),
            "displayMedium" to convertFont(typography.displayMedium),
            "displaySmall" to convertFont(typography.displaySmall),
            "headlineLarge" to convertFont(typography.headlineLarge),
            "headlineMedium" to convertFont(typography.headlineMedium),
            "headlineSmall" to convertFont(typography.headlineSmall),
            "titleLarge" to convertFont(typography.titleLarge),
            "titleMedium" to convertFont(typography.titleMedium),
            "titleSmall" to convertFont(typography.titleSmall),
            "bodyLarge" to convertFont(typography.bodyLarge),
            "bodyMedium" to convertFont(typography.bodyMedium),
            "bodySmall" to convertFont(typography.bodySmall),
            "labelLarge" to convertFont(typography.labelLarge),
            "labelMedium" to convertFont(typography.labelMedium),
            "labelSmall" to convertFont(typography.labelSmall)
        )
    }

    private fun convertFont(font: Font): FontDefinition {
        return FontDefinition(
            family = font.family,
            size = font.size,
            weight = ModifierConverter.convertFontWeight(font.weight),
            style = ModifierConverter.convertFontStyle(font)
        )
    }

    /**
     * Convert Shapes to SwiftUI corner radius values
     */
    fun toSwiftUIShapes(shapes: Shapes): Map<String, Float> {
        return mapOf(
            "extraSmall" to shapes.extraSmall.topLeft,  // Assuming uniform radius
            "small" to shapes.small.topLeft,
            "medium" to shapes.medium.topLeft,
            "large" to shapes.large.topLeft,
            "extraLarge" to shapes.extraLarge.topLeft
        )
    }

    /**
     * Convert SpacingScale to SwiftUI spacing values
     */
    fun toSwiftUISpacing(spacing: SpacingScale): Map<String, Float> {
        return mapOf(
            "xs" to spacing.xs,
            "sm" to spacing.sm,
            "md" to spacing.md,
            "lg" to spacing.lg,
            "xl" to spacing.xl,
            "xxl" to spacing.xxl
        )
    }

    /**
     * Convert ElevationScale to SwiftUI shadow values
     */
    fun toSwiftUIElevation(elevation: ElevationScale): Map<String, ShadowValueWithColor> {
        return mapOf(
            "level0" to convertShadow(elevation.level0),
            "level1" to convertShadow(elevation.level1),
            "level2" to convertShadow(elevation.level2),
            "level3" to convertShadow(elevation.level3),
            "level4" to convertShadow(elevation.level4),
            "level5" to convertShadow(elevation.level5)
        )
    }

    private fun convertShadow(shadow: com.augmentalis.avaelements.core.types.Shadow): ShadowValueWithColor {
        return ShadowValueWithColor(
            color = ModifierConverter.convertColor(shadow.color),
            radius = shadow.blurRadius,
            x = shadow.offsetX,
            y = shadow.offsetY
        )
    }

    /**
     * Convert MaterialSystem to material effect tokens
     */
    fun toMaterialTokens(material: MaterialSystem): MaterialTokens {
        return MaterialTokens(
            glassBlurRadius = material.glassMaterial?.blurRadius,
            glassTintColor = material.glassMaterial?.tintColor?.let {
                ModifierConverter.convertColor(it)
            },
            glassThickness = material.glassMaterial?.thickness,
            micaBaseColor = material.micaMaterial?.baseColor?.let {
                ModifierConverter.convertColor(it)
            },
            micaTintOpacity = material.micaMaterial?.tintOpacity
        )
    }

    /**
     * Create iOS 26 Liquid Glass specific tokens
     */
    fun createLiquidGlassTokens(): Map<String, Any> {
        return mapOf(
            "blurEffect" to "systemMaterial",
            "vibrancy" to true,
            "backgroundOpacity" to 0.7f,
            "cornerStyle" to "continuous",
            "shadowIntensity" to 0.15f
        )
    }

    /**
     * Get SwiftUI system font family based on platform
     */
    fun getSystemFontFamily(typography: Typography): String {
        return when {
            typography.bodyLarge.family.contains("SF Pro", ignoreCase = true) -> "SF Pro"
            typography.bodyLarge.family.contains("Segoe", ignoreCase = true) -> "Segoe UI"
            else -> "System"
        }
    }

    /**
     * Map theme platform to iOS specific configurations
     */
    fun getPlatformSpecificConfig(platform: ThemePlatform): Map<String, Any> {
        return when (platform) {
            ThemePlatform.iOS26_LiquidGlass -> mapOf(
                "usesLiquidGlass" to true,
                "cornerStyle" to "continuous",
                "vibrancy" to true,
                "preferredColorScheme" to "light"
            )
            ThemePlatform.visionOS2_SpatialGlass -> mapOf(
                "usesGlassBackground" to true,
                "spatialDepth" to 100f,
                "cornerStyle" to "continuous",
                "vibrancy" to true
            )
            else -> mapOf(
                "usesStandardMaterial" to true,
                "cornerStyle" to "circular"
            )
        }
    }
}
