/**
 * LiquidWater.kt - Liquid Glass theme water effect recipe
 *
 * Apple Vision Pro inspired: cyan electric highlights, violet shimmer caustics.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object LiquidWater : AvanueWaterScheme {
    override val highlightColor = Color(0xFF00D4FF)         // Cyan Electric
    override val causticColor = Color(0xFFA78BFA)           // Violet shimmer
    override val refractionTint = Color(0x1A00D4FF)         // Cyan @ 10%
    override val depthShadowColor = Color(0x4D000000)       // Black @ 30%
    override val surfaceTint = Color(0xFF1C1C1E)            // Apple system gray 6
    override val borderTint = Color(0x4000D4FF)             // Cyan @ 25%
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}
