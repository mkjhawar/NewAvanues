/**
 * AvanueColorPalette.kt - Decoupled color palette axis (Theme v5.1)
 *
 * Independent from MaterialMode and AppearanceMode — any palette × any style × any appearance.
 * 4 palettes: Sol (sun/gold), Luna (moon/silver), Terra (earth/green), Hydra (water/sapphire).
 * Each palette has Dark + Light color/glass/water variants.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

/**
 * Available color palettes for the Avanues ecosystem.
 *
 * Each palette provides a complete [AvanueColorScheme], [AvanueGlassScheme],
 * and [AvanueWaterScheme] in both dark and light variants. Combine with any
 * [MaterialMode] and [AppearanceMode] for 32 possible visual configurations.
 *
 * Default: [HYDRA] (royal translucent sapphire).
 */
enum class AvanueColorPalette(val displayName: String) {
    /** Warm golden sun — amber gold primary, sunset red secondary */
    SOL("Sol"),
    /** Cool moonlit silver — indigo primary, violet secondary */
    LUNA("Luna"),
    /** Natural earth/forest — forest green primary, warm amber secondary */
    TERRA("Terra"),
    /** Royal translucent sapphire — sapphire primary, amethyst secondary */
    HYDRA("Hydra");

    /** Appearance-aware color scheme accessor (preferred). */
    fun colors(isDark: Boolean): AvanueColorScheme = when (this) {
        SOL -> if (isDark) SolColors else SolColorsLight
        LUNA -> if (isDark) LunaColors else LunaColorsLight
        TERRA -> if (isDark) TerraColors else TerraColorsLight
        HYDRA -> if (isDark) HydraColors else HydraColorsLight
    }

    /** Appearance-aware glass scheme accessor (preferred). */
    fun glass(isDark: Boolean): AvanueGlassScheme = when (this) {
        SOL -> if (isDark) SolGlass else SolGlassLight
        LUNA -> if (isDark) LunaGlass else LunaGlassLight
        TERRA -> if (isDark) TerraGlass else TerraGlassLight
        HYDRA -> if (isDark) HydraGlass else HydraGlassLight
    }

    /** Appearance-aware water scheme accessor (preferred). */
    fun water(isDark: Boolean): AvanueWaterScheme = when (this) {
        SOL -> if (isDark) SolWater else SolWaterLight
        LUNA -> if (isDark) LunaWater else LunaWaterLight
        TERRA -> if (isDark) TerraWater else TerraWaterLight
        HYDRA -> if (isDark) HydraWater else HydraWaterLight
    }

    /** Backward-compatible accessor (dark default). */
    val colors: AvanueColorScheme get() = colors(isDark = true)
    /** Backward-compatible accessor (dark default). */
    val glass: AvanueGlassScheme get() = glass(isDark = true)
    /** Backward-compatible accessor (dark default). */
    val water: AvanueWaterScheme get() = water(isDark = true)

    companion object {
        val DEFAULT = HYDRA

        fun fromString(value: String): AvanueColorPalette = try {
            valueOf(value.uppercase())
        } catch (_: Exception) { DEFAULT }
    }
}
