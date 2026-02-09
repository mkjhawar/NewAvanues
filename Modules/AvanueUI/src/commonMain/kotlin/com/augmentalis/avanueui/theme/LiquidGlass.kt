package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Liquid Glass theme glass effect recipe.
 *
 * Apple Vision Pro inspired: white overlay on true black,
 * cyan glow for a futuristic liquid shimmer effect.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
object LiquidGlass : AvanueGlassScheme {
    override val overlayColor = Color.White
    override val tintColor = Color(0xFF1C1C1E)         // Apple system gray 6
    override val shadowColor = Color.Black
    override val glowColor = Color(0xFF00D4FF)         // Cyan electric shimmer
    override val glassBorderTint = Color(0x1A00D4FF)   // Cyan @ 10%
}
