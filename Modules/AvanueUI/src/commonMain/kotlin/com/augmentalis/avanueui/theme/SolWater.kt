/**
 * SolWater.kt - Sol palette water effect recipe (Theme v5.1)
 *
 * Warm amber highlights with gold caustic shimmer. Dark + Light variants.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object SolWater : AvanueWaterScheme {
    override val highlightColor = Color(0xFFD97706)         // Amber Gold
    override val causticColor = Color(0xFFFBBF24)           // Gold shimmer
    override val refractionTint = Color(0x1AD97706)         // Amber @ 10%
    override val depthShadowColor = Color(0x4D000000)       // Black @ 30%
    override val surfaceTint = Color(0xFF2D1B0E)            // Sol surface
    override val borderTint = Color(0x40D97706)             // Amber @ 25%
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}

object SolWaterLight : AvanueWaterScheme {
    override val highlightColor = Color(0xFFD97706)
    override val causticColor = Color(0xFFFBBF24)
    override val refractionTint = Color(0x1AD97706)
    override val depthShadowColor = Color(0x1A000000)
    override val surfaceTint = Color(0xFFFFF7ED)            // Warm cream surface
    override val borderTint = Color(0x40D97706)
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}
