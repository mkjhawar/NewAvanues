package com.augmentalis.cockpit.mvp.curved

import android.graphics.Color

/**
 * Theme configuration for Curved Workspace
 *
 * Supports two parallel UI styles:
 * 1. Ocean Blue Theme (glassmorphic, deep blues)
 * 2. MagicUI/Avanues Theme (vibrant, modern)
 */
sealed class CurvedWorkspaceTheme {

    abstract val backgroundColor: Int
    abstract val windowColors: List<String>
    abstract val windowBorderColor: Int
    abstract val windowBorderWidth: Float
    abstract val windowShadowColor: Int
    abstract val windowShadowRadius: Float
    abstract val textColor: Int
    abstract val accentOpacity: Float

    /**
     * Ocean Blue Theme - Glassmorphic deep ocean aesthetic
     * Professional, calm, immersive XR feel
     */
    object OceanBlue : CurvedWorkspaceTheme() {
        override val backgroundColor = Color.parseColor("#0D1B2A")  // Deep ocean blue
        override val windowColors = listOf(
            "#2D5F7F",  // Deep bioluminescent blue - calm authority
            "#2A6B6A",  // Muted teal glow - ocean depth
            "#3A7B8A",  // Subdued aqua - water reflection
            "#4A90B8",  // Frosted blue - ice clarity
            "#8A9BA8",  // Chrome mid - metallic professionalism
            "#4A5A6A"   // Chrome dark - steel authority
        )
        override val windowBorderColor = Color.parseColor("#1AFFFFFF")  // 10% white (hair-thin)
        override val windowBorderWidth = 0.5f  // Hair-thin borders
        override val windowShadowColor = Color.parseColor("#14000000")  // 8% black (soft shadow)
        override val windowShadowRadius = 8f
        override val textColor = Color.parseColor("#F2FFFFFF")  // Ocean theme textPrimary
        override val accentOpacity = 1.0f  // Fully opaque colored top bars
    }

    /**
     * MagicUI/Avanues Theme - Vibrant, modern, playful
     * Bright gradients, strong colors, energetic feel
     */
    object MagicUI : CurvedWorkspaceTheme() {
        override val backgroundColor = Color.parseColor("#1A1A2E")  // Dark purple-blue
        override val windowColors = listOf(
            "#FF0080",  // Hot pink
            "#00D9FF",  // Cyan
            "#7B2FBE",  // Purple
            "#FFB800",  // Gold
            "#FF4D6D",  // Red-pink
            "#00FF9F"   // Bright green
        )
        override val windowBorderColor = Color.parseColor("#66FFFFFF")  // 40% white (brighter)
        override val windowBorderWidth = 3f  // Thicker borders
        override val windowShadowColor = Color.parseColor("#80000000")  // 50% black (stronger)
        override val windowShadowRadius = 16f  // Larger shadow
        override val textColor = Color.parseColor("#FFFFFF")  // Pure white
        override val accentOpacity = 1.0f  // Fully opaque
    }

    companion object {
        /**
         * Get theme by name
         */
        fun fromName(name: String): CurvedWorkspaceTheme = when (name.lowercase()) {
            "ocean", "oceanblue", "ocean_blue" -> OceanBlue
            "magic", "magicui", "magic_ui", "avanues" -> MagicUI
            else -> OceanBlue  // Default to Ocean Blue
        }
    }
}
