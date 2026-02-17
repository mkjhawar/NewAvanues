/**
 * LunaGlass.kt - Luna palette glass effect recipe (Theme v5.1)
 *
 * Moonlit indigo glow. Dark + Light variants.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object LunaGlass : AvanueGlassScheme {
    override val overlayColor = Color.White
    override val tintColor = Color(0xFF1A1D2E)           // Luna surface
    override val shadowColor = Color.Black
    override val glowColor = Color(0xFF818CF8)           // Moonlit Indigo
    override val glassBorderTint = Color(0x26818CF8)     // Indigo @ 15%
}

object LunaGlassLight : AvanueGlassScheme {
    override val overlayColor = Color.Black
    override val tintColor = Color(0xFFEDE9FE)           // Lavender surface
    override val shadowColor = Color(0x33000000)
    override val glowColor = Color(0xFF818CF8)           // Same brand
    override val glassBorderTint = Color(0x26818CF8)
}
