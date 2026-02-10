/**
 * OceanWater.kt - Ocean theme water effect recipe
 *
 * Blue-tinted water effects: CoralBlue highlights, cyan caustics.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object OceanWater : AvanueWaterScheme {
    override val highlightColor = Color(0xFF3B82F6)         // CoralBlue
    override val causticColor = Color(0xFF06B6D4)           // Cyan
    override val refractionTint = Color(0x1A3B82F6)         // CoralBlue @ 10%
    override val depthShadowColor = Color(0x4D000000)       // Black @ 30%
    override val surfaceTint = Color(0xFF1E293B)            // OceanMid
    override val borderTint = Color(0x403B82F6)             // CoralBlue @ 25%
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}
