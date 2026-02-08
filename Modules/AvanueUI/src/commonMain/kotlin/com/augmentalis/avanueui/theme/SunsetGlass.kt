package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Sunset theme glass effect recipe.
 * White overlay on dark backgrounds, warm coral glow.
 */
object SunsetGlass : AvanueGlassScheme {
    override val overlayColor = Color.White
    override val tintColor = Color(0xFF2D1B33)       // SunsetLavender
    override val shadowColor = Color.Black
    override val glowColor = Color(0xFFFF6B35)       // SunsetCoral
    override val glassBorderTint = Color(0x26FF6B35)  // SunsetCoral @ 15%
}
