/**
 * AvanueWaterScheme.kt - Theme-variable water effect properties
 *
 * Analogous to [AvanueGlassScheme] but for dynamic Water effects.
 * Static recipe parameters (opacity, blur, animation) live in [WaterTokens].
 * This interface provides per-theme colors and capability flags.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

interface AvanueWaterScheme {
    /** Specular highlight tint color */
    val highlightColor: Color
    /** Animated ripple/shimmer color for caustic patterns */
    val causticColor: Color
    /** Displacement overlay blend tint */
    val refractionTint: Color
    /** Depth layer shadow color */
    val depthShadowColor: Color
    /** Base material tint (surface overlay) */
    val surfaceTint: Color
    /** Border gradient brand tint */
    val borderTint: Color
    /** Can disable refraction per-theme (e.g., high contrast mode) */
    val enableRefraction: Boolean
    /** Can disable caustics for performance-sensitive themes */
    val enableCaustics: Boolean
    /** Can disable specular highlights per-theme */
    val enableSpecular: Boolean
}
