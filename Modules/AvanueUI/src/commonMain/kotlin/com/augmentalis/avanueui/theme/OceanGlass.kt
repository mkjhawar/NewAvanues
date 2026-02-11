package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Ocean theme glass effect recipe.
 *
 * @deprecated Use [LunaGlass] instead. Ocean maps to Luna palette in Theme v5.0.
 */
@Deprecated("Use LunaGlass instead", ReplaceWith("LunaGlass"))
object OceanGlass : AvanueGlassScheme {
    override val overlayColor = Color.White
    override val tintColor = Color(0xFF1E293B)
    override val shadowColor = Color.Black
    override val glowColor = Color(0xFF3B82F6)
    override val glassBorderTint = Color(0x263B82F6)
}
