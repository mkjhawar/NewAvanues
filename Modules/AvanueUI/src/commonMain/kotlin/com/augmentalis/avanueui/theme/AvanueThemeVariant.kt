package com.augmentalis.avanueui.theme

/**
 * Available theme variants for the Avanues ecosystem.
 *
 * Each variant provides a complete color scheme and glass effect recipe.
 * Use with AvanueThemeProvider to apply at runtime:
 * ```
 * val variant = AvanueThemeVariant.LIQUID
 * AvanueThemeProvider(colors = variant.colors, glass = variant.glass) { ... }
 * ```
 *
 * Runtime switching: Store the variant name in DataStore preferences,
 * observe as StateFlow, and pass to AvanueThemeProvider.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
enum class AvanueThemeVariant(val displayName: String) {
    OCEAN("Ocean Blue"),
    SUNSET("Sunset Warm"),
    LIQUID("Liquid Glass");

    val colors: AvanueColorScheme
        get() = when (this) {
            OCEAN -> OceanColors
            SUNSET -> SunsetColors
            LIQUID -> LiquidColors
        }

    val glass: AvanueGlassScheme
        get() = when (this) {
            OCEAN -> OceanGlass
            SUNSET -> SunsetGlass
            LIQUID -> LiquidGlass
        }

    val water: AvanueWaterScheme
        get() = when (this) {
            OCEAN -> OceanWater
            SUNSET -> SunsetWater
            LIQUID -> LiquidWater
        }

    companion object {
        /** Default theme variant for new installations */
        val DEFAULT = LIQUID

        /** Parse from stored string, falling back to default */
        fun fromString(value: String): AvanueThemeVariant {
            return try {
                valueOf(value.uppercase())
            } catch (_: Exception) {
                DEFAULT
            }
        }
    }
}
