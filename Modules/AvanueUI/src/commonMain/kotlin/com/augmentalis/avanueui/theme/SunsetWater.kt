/**
 * SunsetWater.kt - Sunset theme water effect recipe
 *
 * @deprecated Use [SolWater] instead. Sunset maps to Sol palette in Theme v5.0.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

@Deprecated("Use SolWater instead", ReplaceWith("SolWater"))
object SunsetWater : AvanueWaterScheme {
    override val highlightColor = Color(0xFFFF6B35)
    override val causticColor = Color(0xFFFBBF24)
    override val refractionTint = Color(0x1AFF6B35)
    override val depthShadowColor = Color(0x4D000000)
    override val surfaceTint = Color(0xFF2D1B33)
    override val borderTint = Color(0x40FF6B35)
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}
