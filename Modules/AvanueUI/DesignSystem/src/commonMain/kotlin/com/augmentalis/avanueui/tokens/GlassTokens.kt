package com.augmentalis.avanueui.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glass effect constants. Static recipe parameters shared across all themes.
 * Theme-variable colors (overlay, tint, glow) are in [AvanueGlassScheme].
 */
object GlassTokens {
    // Overlay opacity per level
    const val lightOverlay: Float = 0.10f
    const val mediumOverlay: Float = 0.15f
    const val heavyOverlay: Float = 0.22f

    // Blur radius per level
    val lightBlur: Dp = 6.dp
    val mediumBlur: Dp = 8.dp
    val heavyBlur: Dp = 10.dp

    // Border gradient ratios (top-light simulation)
    const val borderTopOpacity: Float = 0.30f
    const val borderBottomOpacity: Float = 0.15f

    // Background gradient stops (3-color vertical)
    const val surfaceTopOpacity: Float = 0.10f
    const val surfaceTintOpacity: Float = 0.15f
    const val surfaceBottomOpacity: Float = 0.05f

    // Shadow
    const val shadowOpacity: Float = 0.25f
    const val glowOpacity: Float = 0.30f
    val shadowElevation: Dp = 8.dp

    // Border widths
    val borderSubtle: Dp = 0.5.dp
    val borderDefault: Dp = 1.dp
    val borderStrong: Dp = 1.5.dp
    val borderFocused: Dp = 2.dp

    fun resolve(id: String): Any? = when (id) {
        "glass.lightOverlay" -> lightOverlay
        "glass.mediumOverlay" -> mediumOverlay
        "glass.heavyOverlay" -> heavyOverlay
        "glass.lightBlur" -> lightBlur
        "glass.mediumBlur" -> mediumBlur
        "glass.heavyBlur" -> heavyBlur
        "glass.borderTopOpacity" -> borderTopOpacity
        "glass.borderBottomOpacity" -> borderBottomOpacity
        "glass.surfaceTopOpacity" -> surfaceTopOpacity
        "glass.surfaceTintOpacity" -> surfaceTintOpacity
        "glass.surfaceBottomOpacity" -> surfaceBottomOpacity
        "glass.shadowOpacity" -> shadowOpacity
        "glass.glowOpacity" -> glowOpacity
        "glass.shadowElevation" -> shadowElevation
        "glass.borderSubtle" -> borderSubtle
        "glass.borderDefault" -> borderDefault
        "glass.borderStrong" -> borderStrong
        "glass.borderFocused" -> borderFocused
        else -> null
    }
}
