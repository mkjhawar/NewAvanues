package com.augmentalis.avanueui.theme

/**
 * Legacy theme variant enum — couples palette + material style.
 *
 * @deprecated Use [AvanueColorPalette] + [MaterialMode] independently.
 * This enum is kept for backward compatibility and migration.
 *
 * Migration mapping:
 * - OCEAN → LUNA + GLASS
 * - SUNSET → SOL + GLASS
 * - LIQUID → HYDRA + WATER
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
@Suppress("DEPRECATION")
@Deprecated("Use AvanueColorPalette + MaterialMode independently")
enum class AvanueThemeVariant(val displayName: String) {
    OCEAN("Ocean Blue"),
    SUNSET("Sunset Warm"),
    LIQUID("Liquid Glass");

    /** Maps to the new decoupled palette */
    val palette: AvanueColorPalette
        get() = when (this) {
            OCEAN -> AvanueColorPalette.LUNA
            SUNSET -> AvanueColorPalette.SOL
            LIQUID -> AvanueColorPalette.HYDRA
        }

    /** Maps to the new decoupled material mode */
    val materialMode: MaterialMode
        get() = when (this) {
            OCEAN -> MaterialMode.Glass
            SUNSET -> MaterialMode.Glass
            LIQUID -> MaterialMode.Water
        }

    /** Delegate to palette colors */
    val colors: AvanueColorScheme get() = palette.colors

    /** Delegate to palette glass scheme */
    val glass: AvanueGlassScheme get() = palette.glass

    /** Delegate to palette water scheme */
    val water: AvanueWaterScheme get() = palette.water

    companion object {
        val DEFAULT = LIQUID

        fun fromString(value: String): AvanueThemeVariant {
            return try {
                valueOf(value.uppercase())
            } catch (_: Exception) {
                DEFAULT
            }
        }
    }
}
