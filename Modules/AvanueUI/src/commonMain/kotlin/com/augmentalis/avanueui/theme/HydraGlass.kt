/**
 * HydraGlass.kt - Hydra palette glass effect recipe (Theme v5.1)
 *
 * Sapphire glow with amethyst tint. Dark + Light variants.
 * Flagship default glass recipe.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

object HydraGlass : AvanueGlassScheme {
    override val overlayColor = Color.White
    override val tintColor = Color(0xFF0F172A)           // Hydra surface
    override val shadowColor = Color.Black
    override val glowColor = Color(0xFF1E40AF)           // Royal Sapphire
    override val glassBorderTint = Color(0x261E40AF)     // Sapphire @ 15%
}

object HydraGlassLight : AvanueGlassScheme {
    override val overlayColor = Color.Black              // Inverted for light
    override val tintColor = Color(0xFFF1F5F9)           // Light slate surface
    override val shadowColor = Color(0x33000000)         // Softer shadow
    override val glowColor = Color(0xFF1E40AF)           // Same brand
    override val glassBorderTint = Color(0x261E40AF)
}
