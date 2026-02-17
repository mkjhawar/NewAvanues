/**
 * OceanWater.kt - Ocean theme water effect recipe
 *
 * @deprecated Use [LunaWater] instead. Ocean maps to Luna palette in Theme v5.0.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

@Deprecated("Use LunaWater instead", ReplaceWith("LunaWater"))
object OceanWater : AvanueWaterScheme {
    override val highlightColor = Color(0xFF3B82F6)
    override val causticColor = Color(0xFF06B6D4)
    override val refractionTint = Color(0x1A3B82F6)
    override val depthShadowColor = Color(0x4D000000)
    override val surfaceTint = Color(0xFF1E293B)
    override val borderTint = Color(0x403B82F6)
    override val enableRefraction = true
    override val enableCaustics = true
    override val enableSpecular = true
}
