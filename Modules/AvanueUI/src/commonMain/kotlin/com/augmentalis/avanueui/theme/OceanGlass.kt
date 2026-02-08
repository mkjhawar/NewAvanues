package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Ocean theme glass effect recipe.
 * White overlay on dark backgrounds, blue glow.
 */
object OceanGlass : AvanueGlassScheme {
    override val overlayColor = Color.White
    override val tintColor = Color(0xFF1E293B)       // OceanMid
    override val shadowColor = Color.Black
    override val glowColor = Color(0xFF3B82F6)       // CoralBlue
    override val glassBorderTint = Color(0x263B82F6)  // CoralBlue @ 15%
}
