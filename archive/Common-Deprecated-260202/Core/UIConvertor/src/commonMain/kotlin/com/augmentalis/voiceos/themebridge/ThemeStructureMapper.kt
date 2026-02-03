package com.augmentalis.voiceos.themebridge

import com.augmentalis.avanue.core.models.ThemeComponent as LegacyComponent
import com.augmentalis.avanues.avaui.theme.ThemeConfig

/**
 * Maps individual theme component updates between legacy Avanue4 and modern AvaUI systems.
 *
 * This mapper enables **incremental theme updates** - when a single component changes in the
 * legacy Avanue4 system (e.g., PRIMARY_COLOR), only that specific field is updated in the
 * AvaUI theme without rebuilding the entire theme structure.
 *
 * ## Key Features
 *
 * - **Efficient Updates**: Only modifies the changed field, preserves all other values
 * - **Type-Safe Mapping**: Handles all 15 legacy ThemeComponent enum values
 * - **Immutable Updates**: Returns new ThemeConfig instance, doesn't mutate existing theme
 * - **Smart Defaults**: Handles unmapped components gracefully
 *
 * ## Component Mapping Strategy
 *
 * ### Direct Color Mappings (ARGB Int → Hex String)
 * - `BACKGROUND_COLOR` → `palette.background`
 * - `PRIMARY_COLOR` → `palette.primary`
 * - `SECONDARY_COLOR` → `palette.secondary`
 * - `TEXT_COLOR` → `palette.onBackground`, `palette.onSurface`
 * - `ACCENT_COLOR` → `palette.error`
 * - `STROKE_COLOR` → `palette.surface`
 *
 * ### Gradient Mappings
 * - `GRADIENT_START_COLOR` → `palette.primary` (fallback)
 * - `GRADIENT_END_COLOR` → `palette.secondary` (fallback)
 * - `GRADIENT_ENABLED` → Not mapped (legacy-specific)
 *
 * ### Effect Mappings
 * - `SHADOW_ENABLED` → `effects.shadowEnabled`
 * - `SHADOW_COLOR` → Not mapped (Material Design uses defaults)
 * - `SHADOW_RADIUS` → `effects.blurRadius`
 * - `RADIUS` → `effects.elevation`
 *
 * ### Stroke Mappings
 * - `STROKE_WIDTH` → Not mapped (legacy-specific)
 *
 * ## Usage
 *
 * ```kotlin
 * val mapper = ThemeStructureMapper()
 * val currentTheme = ThemeConfig(...)
 *
 * // User changed primary color in legacy system
 * val updatedTheme = mapper.updateComponentInMagicTheme(
 *     currentTheme = currentTheme,
 *     component = LegacyComponent.PRIMARY_COLOR,
 *     value = 0xFFFF5722.toInt()  // New ARGB color
 * )
 *
 * // Only palette.primary is changed, everything else preserved
 * assert(updatedTheme.palette.primary == "#FFFF5722")
 * assert(updatedTheme.palette.secondary == currentTheme.palette.secondary)
 * assert(updatedTheme.typography == currentTheme.typography)
 * ```
 *
 * ## Integration with ThemeMigrationBridge
 *
 * This mapper is used by ThemeMigrationBridge to handle incremental updates:
 *
 * ```kotlin
 * override fun onThemeComponentChanged(component: LegacyComponent, value: Any) {
 *     _magicUiTheme.value?.let { currentTheme ->
 *         val updatedTheme = mapper.updateComponentInMagicTheme(
 *             currentTheme, component, value
 *         )
 *         _magicUiTheme.value = updatedTheme
 *     }
 * }
 * ```
 *
 * @property colorUtils Color conversion utilities for Int ↔ Hex conversions
 *
 * @since 3.1.0
 * @see ColorConversionUtils
 */
class ThemeStructureMapper(
    private val colorUtils: ColorConversionUtils = ColorConversionUtils()
) {

    /**
     * Update a single component in AvaUI theme from legacy component change.
     *
     * This method performs **incremental updates** by:
     * 1. Converting the value to the appropriate type (if needed)
     * 2. Mapping the legacy component to its AvaUI equivalent(s)
     * 3. Creating a new ThemeConfig with only the mapped field(s) updated
     * 4. Preserving all other theme properties unchanged
     *
     * ## Supported Component Types
     *
     * - **Colors** (Int ARGB): Converted to hex strings
     * - **Booleans**: Used directly
     * - **Floats**: Used directly with range validation where appropriate
     *
     * ## Behavior for Unmapped Components
     *
     * Components that don't have a direct AvaUI equivalent (e.g., STROKE_WIDTH,
     * SHADOW_COLOR, GRADIENT_ENABLED) are gracefully ignored and return the current
     * theme unchanged. This ensures compatibility while migration is in progress.
     *
     * ## Examples
     *
     * ```kotlin
     * // Color component update
     * val updated1 = mapper.updateComponentInMagicTheme(
     *     currentTheme,
     *     LegacyComponent.PRIMARY_COLOR,
     *     0xFF007AFF.toInt()
     * )
     * // Result: palette.primary = "#FF007AFF", all else unchanged
     *
     * // Boolean component update
     * val updated2 = mapper.updateComponentInMagicTheme(
     *     currentTheme,
     *     LegacyComponent.SHADOW_ENABLED,
     *     false
     * )
     * // Result: effects.shadowEnabled = false, all else unchanged
     *
     * // Float component update
     * val updated3 = mapper.updateComponentInMagicTheme(
     *     currentTheme,
     *     LegacyComponent.SHADOW_RADIUS,
     *     12.5f
     * )
     * // Result: effects.blurRadius = 12.5f, all else unchanged
     *
     * // Unmapped component (returns unchanged)
     * val updated4 = mapper.updateComponentInMagicTheme(
     *     currentTheme,
     *     LegacyComponent.GRADIENT_ENABLED,
     *     true
     * )
     * // Result: theme unchanged (GRADIENT_ENABLED not mapped)
     * ```
     *
     * @param currentTheme Current AvaUI theme configuration
     * @param component Legacy theme component that changed
     * @param value New value for the component (type depends on component)
     *
     * @return New ThemeConfig with the updated component, or current theme if:
     *         - Component is not mapped to AvaUI
     *         - Value type is incompatible
     *         - Value is invalid for the component
     *
     * @throws IllegalArgumentException if color conversion fails (invalid ARGB Int)
     */
    fun updateComponentInMagicTheme(
        currentTheme: ThemeConfig,
        component: LegacyComponent,
        value: Any
    ): ThemeConfig {
        return when (component) {
            // === Color Components (Int ARGB → Hex String) ===

            LegacyComponent.PRIMARY_COLOR -> {
                if (value !is Int) return currentTheme
                val hexValue = colorUtils.intToHex(value)
                currentTheme.copy(
                    palette = currentTheme.palette.copy(primary = hexValue)
                )
            }

            LegacyComponent.SECONDARY_COLOR -> {
                if (value !is Int) return currentTheme
                val hexValue = colorUtils.intToHex(value)
                currentTheme.copy(
                    palette = currentTheme.palette.copy(secondary = hexValue)
                )
            }

            LegacyComponent.BACKGROUND_COLOR -> {
                if (value !is Int) return currentTheme
                val hexValue = colorUtils.intToHex(value)
                currentTheme.copy(
                    palette = currentTheme.palette.copy(background = hexValue)
                )
            }

            LegacyComponent.TEXT_COLOR -> {
                // Text color updates both onBackground and onSurface for consistency
                if (value !is Int) return currentTheme
                val hexValue = colorUtils.intToHex(value)
                currentTheme.copy(
                    palette = currentTheme.palette.copy(
                        onBackground = hexValue,
                        onSurface = hexValue
                    )
                )
            }

            LegacyComponent.ACCENT_COLOR -> {
                // Map accent to error color (both serve as accent/emphasis colors)
                if (value !is Int) return currentTheme
                val hexValue = colorUtils.intToHex(value)
                currentTheme.copy(
                    palette = currentTheme.palette.copy(error = hexValue)
                )
            }

            LegacyComponent.STROKE_COLOR -> {
                // Map stroke color to surface (both used for boundaries/containers)
                if (value !is Int) return currentTheme
                val hexValue = colorUtils.intToHex(value)
                currentTheme.copy(
                    palette = currentTheme.palette.copy(surface = hexValue)
                )
            }

            // === Gradient Components ===

            LegacyComponent.GRADIENT_START_COLOR -> {
                // Map gradient start to primary color
                if (value !is Int) return currentTheme
                val hexValue = colorUtils.intToHex(value)
                currentTheme.copy(
                    palette = currentTheme.palette.copy(primary = hexValue)
                )
            }

            LegacyComponent.GRADIENT_END_COLOR -> {
                // Map gradient end to secondary color
                if (value !is Int) return currentTheme
                val hexValue = colorUtils.intToHex(value)
                currentTheme.copy(
                    palette = currentTheme.palette.copy(secondary = hexValue)
                )
            }

            LegacyComponent.GRADIENT_ENABLED -> {
                // AvaUI handles gradients differently (not a simple boolean toggle)
                // Return unchanged - future enhancement could extend ThemeEffects
                currentTheme
            }

            // === Shadow Components ===

            LegacyComponent.SHADOW_ENABLED -> {
                if (value !is Boolean) return currentTheme
                currentTheme.copy(
                    effects = currentTheme.effects.copy(shadowEnabled = value)
                )
            }

            LegacyComponent.SHADOW_COLOR -> {
                // AvaUI uses Material Design shadow system (color determined by elevation)
                // Return unchanged - future enhancement could extend ThemeEffects
                currentTheme
            }

            LegacyComponent.SHADOW_RADIUS -> {
                // Map shadow radius to blur radius (both control shadow softness)
                val floatValue = when (value) {
                    is Float -> value
                    is Double -> value.toFloat()
                    is Int -> value.toFloat()
                    else -> return currentTheme
                }

                // Validate range (0-100dp reasonable for blur radius)
                val validatedValue = floatValue.coerceIn(0f, 100f)

                currentTheme.copy(
                    effects = currentTheme.effects.copy(blurRadius = validatedValue)
                )
            }

            // === Shape Components ===

            LegacyComponent.RADIUS -> {
                // Map corner radius to elevation (both affect depth perception)
                val floatValue = when (value) {
                    is Float -> value
                    is Double -> value.toFloat()
                    is Int -> value.toFloat()
                    else -> return currentTheme
                }

                // Validate range (0-24dp reasonable for elevation)
                val validatedValue = floatValue.coerceIn(0f, 24f)

                currentTheme.copy(
                    effects = currentTheme.effects.copy(elevation = validatedValue)
                )
            }

            // === Stroke Components ===

            LegacyComponent.STROKE_WIDTH -> {
                // Stroke width is legacy-specific (AvaUI uses Material Design borders)
                // Return unchanged
                currentTheme
            }
        }
    }

    /**
     * Get a description of how a legacy component maps to AvaUI.
     *
     * Useful for debugging, documentation, and UI display of mapping information.
     *
     * @param component Legacy theme component
     * @return Human-readable description of the mapping
     */
    fun getMappingDescription(component: LegacyComponent): String {
        return when (component) {
            LegacyComponent.PRIMARY_COLOR ->
                "Maps to palette.primary"
            LegacyComponent.SECONDARY_COLOR ->
                "Maps to palette.secondary"
            LegacyComponent.BACKGROUND_COLOR ->
                "Maps to palette.background"
            LegacyComponent.TEXT_COLOR ->
                "Maps to palette.onBackground and palette.onSurface"
            LegacyComponent.ACCENT_COLOR ->
                "Maps to palette.error (accent/emphasis color)"
            LegacyComponent.STROKE_COLOR ->
                "Maps to palette.surface (boundary/container color)"
            LegacyComponent.GRADIENT_START_COLOR ->
                "Maps to palette.primary (gradient fallback)"
            LegacyComponent.GRADIENT_END_COLOR ->
                "Maps to palette.secondary (gradient fallback)"
            LegacyComponent.GRADIENT_ENABLED ->
                "Not directly mapped (legacy gradient system)"
            LegacyComponent.SHADOW_ENABLED ->
                "Maps to effects.shadowEnabled"
            LegacyComponent.SHADOW_COLOR ->
                "Not directly mapped (Material Design determines shadow color from elevation)"
            LegacyComponent.SHADOW_RADIUS ->
                "Maps to effects.blurRadius (shadow softness)"
            LegacyComponent.STROKE_WIDTH ->
                "Not directly mapped (Material Design uses standard border widths)"
            LegacyComponent.RADIUS ->
                "Maps to effects.elevation (depth perception)"
        }
    }

    /**
     * Check if a legacy component has a direct mapping to AvaUI.
     *
     * @param component Legacy theme component
     * @return true if the component maps to one or more AvaUI properties
     */
    fun isMapped(component: LegacyComponent): Boolean {
        return when (component) {
            LegacyComponent.PRIMARY_COLOR,
            LegacyComponent.SECONDARY_COLOR,
            LegacyComponent.BACKGROUND_COLOR,
            LegacyComponent.TEXT_COLOR,
            LegacyComponent.ACCENT_COLOR,
            LegacyComponent.STROKE_COLOR,
            LegacyComponent.GRADIENT_START_COLOR,
            LegacyComponent.GRADIENT_END_COLOR,
            LegacyComponent.SHADOW_ENABLED,
            LegacyComponent.SHADOW_RADIUS,
            LegacyComponent.RADIUS -> true

            LegacyComponent.GRADIENT_ENABLED,
            LegacyComponent.SHADOW_COLOR,
            LegacyComponent.STROKE_WIDTH -> false
        }
    }

    /**
     * Get all legacy components that affect the AvaUI palette.
     *
     * @return List of legacy components that map to palette properties
     */
    fun getPaletteMappedComponents(): List<LegacyComponent> {
        return listOf(
            LegacyComponent.PRIMARY_COLOR,
            LegacyComponent.SECONDARY_COLOR,
            LegacyComponent.BACKGROUND_COLOR,
            LegacyComponent.TEXT_COLOR,
            LegacyComponent.ACCENT_COLOR,
            LegacyComponent.STROKE_COLOR,
            LegacyComponent.GRADIENT_START_COLOR,
            LegacyComponent.GRADIENT_END_COLOR
        )
    }

    /**
     * Get all legacy components that affect the AvaUI effects.
     *
     * @return List of legacy components that map to effects properties
     */
    fun getEffectsMappedComponents(): List<LegacyComponent> {
        return listOf(
            LegacyComponent.SHADOW_ENABLED,
            LegacyComponent.SHADOW_RADIUS,
            LegacyComponent.RADIUS
        )
    }

    /**
     * Get all legacy components that are not mapped to AvaUI.
     *
     * These components are legacy-specific and don't have direct equivalents
     * in the modern AvaUI theme system.
     *
     * @return List of unmapped legacy components
     */
    fun getUnmappedComponents(): List<LegacyComponent> {
        return listOf(
            LegacyComponent.GRADIENT_ENABLED,
            LegacyComponent.SHADOW_COLOR,
            LegacyComponent.STROKE_WIDTH
        )
    }

    companion object {
        /**
         * Total number of legacy theme components.
         */
        const val TOTAL_LEGACY_COMPONENTS = 15

        /**
         * Number of legacy components that map to AvaUI.
         */
        const val MAPPED_COMPONENTS = 12

        /**
         * Number of legacy components that don't map to AvaUI.
         */
        const val UNMAPPED_COMPONENTS = 3
    }
}
