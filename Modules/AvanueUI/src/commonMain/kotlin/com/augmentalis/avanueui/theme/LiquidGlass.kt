package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Liquid Glass theme glass effect recipe.
 *
 * @deprecated Use [HydraGlass] instead. Liquid maps to Hydra palette in Theme v5.0.
 */
@Deprecated("Use HydraGlass instead", ReplaceWith("HydraGlass"))
object LiquidGlass : AvanueGlassScheme {
    override val overlayColor = Color.White
    override val tintColor = Color(0xFF1C1C1E)
    override val shadowColor = Color.Black
    override val glowColor = Color(0xFF00D4FF)
    override val glassBorderTint = Color(0x1A00D4FF)
}
