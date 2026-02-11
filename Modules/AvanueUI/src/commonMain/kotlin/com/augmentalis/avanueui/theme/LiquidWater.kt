/**
 * LiquidWater.kt - Liquid Glass theme water effect recipe
 *
 * @deprecated Use [HydraWater] instead. Liquid maps to Hydra palette in Theme v5.0.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

@Deprecated("Use HydraWater instead", ReplaceWith("HydraWater"))
object LiquidWater : AvanueWaterScheme {
    override val highlightColor = Color(0xFF00D4FF)
    override val causticColor = Color(0xFFA78BFA)
    override val refractionTint = Color(0x1A00D4FF)
    override val depthShadowColor = Color(0x4D000000)
    override val surfaceTint = Color(0xFF1C1C1E)
    override val borderTint = Color(0x4000D4FF)
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}
