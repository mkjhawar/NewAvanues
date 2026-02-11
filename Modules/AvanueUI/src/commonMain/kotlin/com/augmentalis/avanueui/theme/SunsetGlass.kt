package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Sunset theme glass effect recipe.
 *
 * @deprecated Use [SolGlass] instead. Sunset maps to Sol palette in Theme v5.0.
 */
@Deprecated("Use SolGlass instead", ReplaceWith("SolGlass"))
object SunsetGlass : AvanueGlassScheme {
    override val overlayColor = Color.White
    override val tintColor = Color(0xFF2D1B33)
    override val shadowColor = Color.Black
    override val glowColor = Color(0xFFFF6B35)
    override val glassBorderTint = Color(0x26FF6B35)
}
