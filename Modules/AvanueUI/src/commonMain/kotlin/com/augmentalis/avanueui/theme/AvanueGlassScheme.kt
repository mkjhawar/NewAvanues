package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Theme-variable glass effect recipe.
 * Static recipe parameters (opacity ratios, blur radii) are in [GlassTokens].
 * This interface provides the theme-specific colors.
 */
interface AvanueGlassScheme {
    /** Overlay color (white on dark themes, black on light themes) */
    val overlayColor: Color
    /** Tint color for background gradient middle stop */
    val tintColor: Color
    /** Shadow spot color */
    val shadowColor: Color
    /** Ambient glow color (e.g., CoralBlue for Ocean, Coral for Sunset) */
    val glowColor: Color
    /** Glass border tint (subtle brand color on borders) */
    val glassBorderTint: Color
}
