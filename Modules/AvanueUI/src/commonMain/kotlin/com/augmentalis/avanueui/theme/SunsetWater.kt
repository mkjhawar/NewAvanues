/**
 * SunsetWater.kt - Sunset theme water effect recipe
 *
 * Warm-tinted water effects: coral highlights, gold caustics.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object SunsetWater : AvanueWaterScheme {
    override val highlightColor = Color(0xFFFF6B35)         // Coral
    override val causticColor = Color(0xFFFBBF24)           // Gold
    override val refractionTint = Color(0x1AFF6B35)         // Coral @ 10%
    override val depthShadowColor = Color(0x4D000000)       // Black @ 30%
    override val surfaceTint = Color(0xFF2D1B33)            // SunsetLavender
    override val borderTint = Color(0x40FF6B35)             // Coral @ 25%
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}
