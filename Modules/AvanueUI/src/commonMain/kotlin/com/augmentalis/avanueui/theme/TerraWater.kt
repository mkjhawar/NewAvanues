/**
 * TerraWater.kt - Terra palette water effect recipe (Theme v5.1)
 *
 * Green highlights with warm amber caustic shimmer. Dark + Light variants.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object TerraWater : AvanueWaterScheme {
    override val highlightColor = Color(0xFF2D7D46)         // Forest Green
    override val causticColor = Color(0xFFD97706)           // Warm Amber shimmer
    override val refractionTint = Color(0x1A2D7D46)         // Green @ 10%
    override val depthShadowColor = Color(0x4D000000)       // Black @ 30%
    override val surfaceTint = Color(0xFF1A2B1C)            // Terra surface
    override val borderTint = Color(0x402D7D46)             // Green @ 25%
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}

object TerraWaterLight : AvanueWaterScheme {
    override val highlightColor = Color(0xFF2D7D46)
    override val causticColor = Color(0xFFD97706)
    override val refractionTint = Color(0x1A2D7D46)
    override val depthShadowColor = Color(0x1A000000)
    override val surfaceTint = Color(0xFFDCFCE7)            // Mint surface
    override val borderTint = Color(0x402D7D46)
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}
