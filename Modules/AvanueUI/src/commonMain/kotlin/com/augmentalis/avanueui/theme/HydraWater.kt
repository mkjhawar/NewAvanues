/**
 * HydraWater.kt - Hydra palette water effect recipe (Theme v5.1)
 *
 * Sapphire highlights with amethyst caustic shimmer.
 * Flagship default water recipe. Dark + Light variants.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object HydraWater : AvanueWaterScheme {
    override val highlightColor = Color(0xFF1E40AF)         // Royal Sapphire
    override val causticColor = Color(0xFF8B5CF6)           // Amethyst shimmer
    override val refractionTint = Color(0x1A1E40AF)         // Sapphire @ 10%
    override val depthShadowColor = Color(0x4D000000)       // Black @ 30%
    override val surfaceTint = Color(0xFF0F172A)            // Hydra surface
    override val borderTint = Color(0x401E40AF)             // Sapphire @ 25%
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}

object HydraWaterLight : AvanueWaterScheme {
    override val highlightColor = Color(0xFF1E40AF)
    override val causticColor = Color(0xFF8B5CF6)
    override val refractionTint = Color(0x1A1E40AF)
    override val depthShadowColor = Color(0x1A000000)       // Much lighter shadow
    override val surfaceTint = Color(0xFFF1F5F9)            // Light slate surface
    override val borderTint = Color(0x401E40AF)
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}
