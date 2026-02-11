/**
 * LunaWater.kt - Luna palette water effect recipe (Theme v5.1)
 *
 * Silver highlights with violet caustic shimmer. Dark + Light variants.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object LunaWater : AvanueWaterScheme {
    override val highlightColor = Color(0xFF818CF8)         // Moonlit Indigo
    override val causticColor = Color(0xFF7C3AED)           // Violet shimmer
    override val refractionTint = Color(0x1A818CF8)         // Indigo @ 10%
    override val depthShadowColor = Color(0x4D000000)       // Black @ 30%
    override val surfaceTint = Color(0xFF1A1D2E)            // Luna surface
    override val borderTint = Color(0x40818CF8)             // Indigo @ 25%
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}

object LunaWaterLight : AvanueWaterScheme {
    override val highlightColor = Color(0xFF818CF8)
    override val causticColor = Color(0xFF7C3AED)
    override val refractionTint = Color(0x1A818CF8)
    override val depthShadowColor = Color(0x1A000000)
    override val surfaceTint = Color(0xFFEDE9FE)            // Lavender surface
    override val borderTint = Color(0x40818CF8)
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}
