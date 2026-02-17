/**
 * SolGlass.kt - Sol palette glass effect recipe (Theme v5.1)
 *
 * Warm amber glow. Dark + Light variants.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object SolGlass : AvanueGlassScheme {
    override val overlayColor = Color.White
    override val tintColor = Color(0xFF2D1B0E)           // Sol surface
    override val shadowColor = Color.Black
    override val glowColor = Color(0xFFD97706)           // Amber Gold
    override val glassBorderTint = Color(0x26D97706)     // Amber @ 15%
}

object SolGlassLight : AvanueGlassScheme {
    override val overlayColor = Color.Black
    override val tintColor = Color(0xFFFFF7ED)           // Warm cream surface
    override val shadowColor = Color(0x33000000)
    override val glowColor = Color(0xFFD97706)           // Same brand
    override val glassBorderTint = Color(0x26D97706)
}
