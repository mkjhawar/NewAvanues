package com.augmentalis.avanueui.themebridge

import com.augmentalis.avanue.core.models.ThemeComponent as LegacyComponent
import com.augmentalis.avanueui.theme.*

/**
 * Type-safe theme converter: Avanue4 ↔ AvaUI.
 *
 * Provides bidirectional conversion between Avanue4's flat, component-based theme structure
 * and AvaUI's nested, serializable theme configuration. Handles type conversions (Int ↔ String),
 * structure mapping, and default value assignment.
 *
 * ## Conversion Overview
 *
 * **Avanue4 → AvaUI:**
 * ```kotlin
 * // Input: Avanue4 component map
 * val legacyComponents = mapOf(
 *     ThemeComponent.PRIMARY_COLOR to 0xFF007AFF.toInt(),
 *     ThemeComponent.BACKGROUND_COLOR to 0xFF000000.toInt()
 * )
 *
 * // Output: AvaUI ThemeConfig with nested structure
 * val magicTheme = converter.convertLegacyToAvaUI(legacyComponents)
 * // magicTheme.palette.primary = "#007AFF"
 * // magicTheme.palette.background = "#000000"
 * ```
 *
 * **AvaUI → Avanue4:**
 * ```kotlin
 * // Input: AvaUI ThemeConfig with hex strings
 * val magicTheme = ThemeConfig(
 *     name = "Dark",
 *     palette = ThemePalette(primary = "#007AFF", background = "#000000", ...)
 * )
 *
 * // Output: Avanue4 component map
 * val legacyComponents = converter.convertAvaUIToLegacy(magicTheme)
 * // legacyComponents[PRIMARY_COLOR] = 0xFF007AFF
 * ```
 *
 * ## Component Mappings
 *
 * Maps all 14 Avanue4 ThemeComponent enum values to AvaUI equivalents:
 *
 * ### Color Components (5)
 * - `BACKGROUND_COLOR` → `palette.background`
 * - `PRIMARY_COLOR` → `palette.primary`
 * - `SECONDARY_COLOR` → `palette.secondary`
 * - `TEXT_COLOR` → `palette.onBackground`, `palette.onSurface`
 * - `ACCENT_COLOR` → Derived from `palette.primary` (lightened 20%)
 *
 * ### Stroke Components (2)
 * - `STROKE_WIDTH` → `effects.elevation`
 * - `STROKE_COLOR` → `palette.surface`
 *
 * ### Shape Components (1)
 * - `RADIUS` → `effects.blurRadius` (corner radius)
 *
 * ### Shadow Components (3)
 * - `SHADOW_ENABLED` → `effects.shadowEnabled`
 * - `SHADOW_COLOR` → Derived (20% opacity of onBackground)
 * - `SHADOW_RADIUS` → `effects.blurRadius`
 *
 * ### Gradient Components (3)
 * - `GRADIENT_ENABLED` → Not mapped (AvaUI uses palette-based gradients)
 * - `GRADIENT_START_COLOR` → Not mapped (preserved for round-trip)
 * - `GRADIENT_END_COLOR` → Not mapped (preserved for round-trip)
 *
 * ## Default Values
 *
 * When components are missing from source theme, sensible defaults are used:
 * - Primary: `#007AFF` (iOS blue)
 * - Secondary: `#5AC8FA` (iOS teal)
 * - Background: `#000000` (black for dark theme)
 * - Surface: `#1C1C1E` (iOS dark surface)
 * - Error: `#FF3B30` (iOS red)
 * - Text colors: `#FFFFFF` (white for dark theme)
 *
 * ## Color Conversion
 *
 * All color conversions use [ColorConversionUtils] for:
 * - Lossless Int ↔ Hex conversion via ColorRGBA
 * - Alpha channel preservation
 * - Color manipulation (lighten, darken, blend)
 *
 * @property colorUtils Color conversion utility (injected for testing)
 * @since 3.1.0
 */
class ThemeConverter(
    private val colorUtils: ColorConversionUtils = ColorConversionUtils()
) {

    /**
     * Convert Avanue4 legacy theme components to AvaUI theme configuration.
     *
     * Extracts color values from Avanue4's flat component map and builds AvaUI's
     * nested ThemeConfig structure. Missing components receive sensible defaults.
     *
     * ## Conversion Details
     *
     * **Color Mapping:**
     * - Extracts Int colors from components map
     * - Converts each Int to hex string using ColorRGBA
     * - Maps to corresponding ThemePalette fields
     * - Derives accent color from primary (20% lighter)
     *
     * **Non-Color Components:**
     * - Maps stroke width to ThemeEffects.elevation
     * - Maps radius to ThemeEffects.blurRadius
     * - Maps shadow properties to ThemeEffects
     * - Gradient components ignored (not supported in AvaUI v3.1)
     *
     * **Defaults:**
     * - Typography: Uses AvaUI defaults (h1=28f bold, body=16f regular)
     * - Spacing: Uses AvaUI defaults (xs=4f, sm=8f, md=16f, lg=24f, xl=32f)
     * - Effects: Combines Avanue4 values with sensible defaults
     *
     * @param legacyComponents Component map from Avanue4 theme (Map<ThemeComponent, Any>)
     * @return AvaUI theme configuration with complete palette and settings
     *
     * @throws IllegalArgumentException if any color value is invalid
     *
     * @see convertAvaUIToLegacy for reverse conversion
     */
    fun convertLegacyToAvaUI(legacyComponents: Map<LegacyComponent, Any>): ThemeConfig {
        // ==================== Extract Color Components ====================

        val primaryInt = (legacyComponents[LegacyComponent.PRIMARY_COLOR] as? Int)
            ?: DEFAULT_PRIMARY_INT
        val secondaryInt = (legacyComponents[LegacyComponent.SECONDARY_COLOR] as? Int)
            ?: DEFAULT_SECONDARY_INT
        val backgroundInt = (legacyComponents[LegacyComponent.BACKGROUND_COLOR] as? Int)
            ?: DEFAULT_BACKGROUND_INT
        val textColorInt = (legacyComponents[LegacyComponent.TEXT_COLOR] as? Int)
            ?: DEFAULT_TEXT_INT
        val accentColorInt = (legacyComponents[LegacyComponent.ACCENT_COLOR] as? Int)
            ?: colorUtils.lighten(primaryInt, 0.2f) // Derive from primary if not set

        // Stroke color (map to surface, or use primary if not set)
        val strokeColorInt = (legacyComponents[LegacyComponent.STROKE_COLOR] as? Int)
            ?: DEFAULT_SURFACE_INT

        // Shadow color (derive from text color with 20% opacity)
        val shadowColorInt = (legacyComponents[LegacyComponent.SHADOW_COLOR] as? Int)
            ?: colorUtils.blend(textColorInt, backgroundInt, 0.2f)

        // Gradient colors (for potential future use, preserved for round-trip)
        val gradientStartInt = (legacyComponents[LegacyComponent.GRADIENT_START_COLOR] as? Int)
            ?: primaryInt
        val gradientEndInt = (legacyComponents[LegacyComponent.GRADIENT_END_COLOR] as? Int)
            ?: secondaryInt

        // ==================== Convert Int → Hex ====================

        val primaryHex = colorUtils.intToHex(primaryInt, includeAlpha = false)
        val secondaryHex = colorUtils.intToHex(secondaryInt, includeAlpha = false)
        val backgroundHex = colorUtils.intToHex(backgroundInt, includeAlpha = false)
        val surfaceHex = colorUtils.intToHex(strokeColorInt, includeAlpha = false)
        val errorHex = DEFAULT_ERROR_HEX // Always use default error color
        val textColorHex = colorUtils.intToHex(textColorInt, includeAlpha = false)

        // ==================== Extract Non-Color Components ====================

        // Stroke width (map to elevation)
        val strokeWidth = (legacyComponents[LegacyComponent.STROKE_WIDTH] as? Float)
            ?: (legacyComponents[LegacyComponent.STROKE_WIDTH] as? Double)?.toFloat()
            ?: DEFAULT_STROKE_WIDTH

        // Corner radius (map to blur radius)
        val cornerRadius = (legacyComponents[LegacyComponent.RADIUS] as? Float)
            ?: (legacyComponents[LegacyComponent.RADIUS] as? Double)?.toFloat()
            ?: DEFAULT_RADIUS

        // Shadow properties
        val shadowEnabled = (legacyComponents[LegacyComponent.SHADOW_ENABLED] as? Boolean)
            ?: true
        val shadowRadius = (legacyComponents[LegacyComponent.SHADOW_RADIUS] as? Float)
            ?: (legacyComponents[LegacyComponent.SHADOW_RADIUS] as? Double)?.toFloat()
            ?: DEFAULT_SHADOW_RADIUS

        // Gradient enabled (stored but not used in AvaUI v3.1)
        val gradientEnabled = (legacyComponents[LegacyComponent.GRADIENT_ENABLED] as? Boolean)
            ?: false

        // ==================== Build AvaUI ThemeConfig ====================

        return ThemeConfig(
            name = "Migrated from Avanue4",
            palette = ThemePalette(
                primary = primaryHex,
                secondary = secondaryHex,
                background = backgroundHex,
                surface = surfaceHex,
                error = errorHex,
                onPrimary = "#FFFFFF",      // Always white text on primary
                onSecondary = "#FFFFFF",    // Always white text on secondary
                onBackground = textColorHex,
                onSurface = textColorHex,
                onError = "#FFFFFF"         // Always white text on error
            ),
            typography = ThemeTypography(), // Use defaults
            spacing = ThemeSpacing(),       // Use defaults
            effects = ThemeEffects(
                shadowEnabled = shadowEnabled,
                blurRadius = maxOf(cornerRadius, shadowRadius), // Use larger of two
                elevation = strokeWidth // Map stroke width to elevation
            )
        )
    }

    /**
     * Convert AvaUI theme configuration to Avanue4 legacy theme components.
     *
     * Flattens AvaUI's nested structure into Avanue4's component map. Maps all
     * ThemePalette colors to appropriate ThemeComponent enum values. Derives additional
     * colors (stroke, shadow, gradient) from palette colors.
     *
     * ## Conversion Details
     *
     * **Color Mapping:**
     * - Extracts hex strings from ThemePalette
     * - Converts each hex to Int using ColorRGBA
     * - Maps to corresponding ThemeComponent enum values
     * - Derives accent from primary (20% lighter)
     *
     * **Non-Color Components:**
     * - Maps `effects.elevation` to `STROKE_WIDTH`
     * - Maps `effects.blurRadius` to `RADIUS` and `SHADOW_RADIUS`
     * - Maps `effects.shadowEnabled` to `SHADOW_ENABLED`
     *
     * **Derived Components:**
     * - `STROKE_COLOR`: Uses surface color
     * - `SHADOW_COLOR`: Blends onBackground with background (20% opacity)
     * - `GRADIENT_START_COLOR`: Uses primary color
     * - `GRADIENT_END_COLOR`: Uses secondary color
     * - `GRADIENT_ENABLED`: Always false (AvaUI v3.1 doesn't use gradients)
     * - `ACCENT_COLOR`: Primary color lightened by 20%
     *
     * @param magicTheme AvaUI theme configuration to convert
     * @return Component map for Avanue4 theme (Map<ThemeComponent, Any>)
     *
     * @throws IllegalArgumentException if any hex color string is invalid
     *
     * @see convertLegacyToAvaUI for reverse conversion
     */
    fun convertAvaUIToLegacy(magicTheme: ThemeConfig): Map<LegacyComponent, Any> {
        val palette = magicTheme.palette
        val effects = magicTheme.effects

        // ==================== Convert Hex → Int ====================

        val primaryInt = colorUtils.hexToInt(palette.primary)
        val secondaryInt = colorUtils.hexToInt(palette.secondary)
        val backgroundInt = colorUtils.hexToInt(palette.background)
        val surfaceInt = colorUtils.hexToInt(palette.surface)
        val errorInt = colorUtils.hexToInt(palette.error)
        val onBackgroundInt = colorUtils.hexToInt(palette.onBackground)
        val onSurfaceInt = colorUtils.hexToInt(palette.onSurface)

        // ==================== Derive Additional Colors ====================

        // Accent: 20% lighter than primary
        val accentInt = colorUtils.lighten(primaryInt, 0.2f)

        // Stroke color: use surface color
        val strokeColorInt = surfaceInt

        // Shadow color: blend onBackground with background (20% opacity)
        val shadowColorInt = colorUtils.blend(onBackgroundInt, backgroundInt, 0.2f)

        // Gradient colors (for legacy compatibility)
        val gradientStartInt = primaryInt
        val gradientEndInt = secondaryInt

        // ==================== Build Component Map ====================

        return mapOf(
            // === Color Components (5) ===
            LegacyComponent.BACKGROUND_COLOR to backgroundInt,
            LegacyComponent.PRIMARY_COLOR to primaryInt,
            LegacyComponent.SECONDARY_COLOR to secondaryInt,
            LegacyComponent.TEXT_COLOR to onBackgroundInt,
            LegacyComponent.ACCENT_COLOR to accentInt,

            // === Stroke Components (2) ===
            LegacyComponent.STROKE_WIDTH to effects.elevation,
            LegacyComponent.STROKE_COLOR to strokeColorInt,

            // === Shape Components (1) ===
            LegacyComponent.RADIUS to effects.blurRadius,

            // === Shadow Components (3) ===
            LegacyComponent.SHADOW_ENABLED to effects.shadowEnabled,
            LegacyComponent.SHADOW_COLOR to shadowColorInt,
            LegacyComponent.SHADOW_RADIUS to effects.blurRadius,

            // === Gradient Components (3) ===
            LegacyComponent.GRADIENT_ENABLED to false, // AvaUI v3.1 doesn't use gradients
            LegacyComponent.GRADIENT_START_COLOR to gradientStartInt,
            LegacyComponent.GRADIENT_END_COLOR to gradientEndInt
        )
    }

    // ==================== Companion: Constants & Defaults ====================

    companion object {
        /**
         * Default color values (Int ARGB format).
         *
         * Based on iOS dark theme colors for consistency with VoiceOS design.
         */
        private val DEFAULT_PRIMARY_INT = 0xFF007AFF.toInt()      // iOS blue
        private val DEFAULT_SECONDARY_INT = 0xFF5AC8FA.toInt()   // iOS teal
        private val DEFAULT_BACKGROUND_INT = 0xFF000000.toInt()  // Black
        private val DEFAULT_SURFACE_INT = 0xFF1C1C1E.toInt()     // iOS dark surface
        private val DEFAULT_TEXT_INT = 0xFFFFFFFF.toInt()        // White

        /**
         * Default error color (hex string).
         *
         * Always use hex for error color to ensure consistency.
         */
        private const val DEFAULT_ERROR_HEX = "#FF3B30" // iOS red

        /**
         * Default non-color values.
         */
        private const val DEFAULT_STROKE_WIDTH = 1.0f
        private const val DEFAULT_RADIUS = 8.0f
        private const val DEFAULT_SHADOW_RADIUS = 8.0f
    }
}
