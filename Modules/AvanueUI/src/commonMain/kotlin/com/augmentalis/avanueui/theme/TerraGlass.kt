/**
 * TerraGlass.kt - Terra palette glass effect recipe (Theme v5.1)
 *
 * Forest green glow. Dark + Light variants.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object TerraGlass : AvanueGlassScheme {
    override val overlayColor = Color.White
    override val tintColor = Color(0xFF1A2B1C)           // Terra surface
    override val shadowColor = Color.Black
    override val glowColor = Color(0xFF2D7D46)           // Forest Green
    override val glassBorderTint = Color(0x262D7D46)     // Green @ 15%
}

object TerraGlassLight : AvanueGlassScheme {
    override val overlayColor = Color.Black
    override val tintColor = Color(0xFFDCFCE7)           // Mint surface
    override val shadowColor = Color(0x33000000)
    override val glowColor = Color(0xFF2D7D46)           // Same brand
    override val glassBorderTint = Color(0x262D7D46)
}
