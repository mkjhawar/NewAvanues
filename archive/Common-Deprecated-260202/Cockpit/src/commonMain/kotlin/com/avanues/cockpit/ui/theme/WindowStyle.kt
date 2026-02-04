package com.avanues.cockpit.ui.theme

/**
 * Window Style Configuration (User-Configurable via Voice)
 *
 * Two styles:
 * 1. MINIMAL (DEFAULT) - Clean macOS-style, very subtle borders
 * 2. GLASS - Dark-glass borders with soft inner glow
 *
 * Voice Commands:
 * - "Minimal borders"
 * - "Glass borders"
 */
enum class WindowStyle {
    MINIMAL,
    GLASS;

    fun getBorderStyle(): BorderStyle = when (this) {
        MINIMAL -> BorderStyle.Minimal
        GLASS -> BorderStyle.Glass
    }

    fun getShadowStyle(): ShadowStyle = when (this) {
        MINIMAL -> ShadowStyle.Soft
        GLASS -> ShadowStyle.Enhanced
    }
}

sealed class BorderStyle {
    object Minimal : BorderStyle() {
        val width = 1f          // dp
        val color = 0x20000000  // Very subtle dark
        val cornerRadius = 8f   // dp
    }

    object Glass : BorderStyle() {
        val width = 2f          // dp
        val color = 0x40000000  // Dark semi-transparent
        val glowColor = 0x40FFFFFF  // White glow
        val glowBlur = 8f       // dp
        val cornerRadius = 12f  // dp
    }
}

sealed class ShadowStyle {
    object Soft : ShadowStyle() {
        val offsetX = 0f
        val offsetY = 4f        // dp
        val blurRadius = 12f    // dp
        val color = 0x30000000  // Soft dark shadow
    }

    object Enhanced : ShadowStyle() {
        val offsetX = 0f
        val offsetY = 6f        // dp
        val blurRadius = 20f    // dp
        val color = 0x40000000  // Stronger shadow
        val glowBlur = 4f       // Additional glow
    }
}

/**
 * Background configuration matching preferred embodiment
 */
object BackgroundStyle {
    // Neutral gradient colors (beige/tan from image)
    val gradientStart = 0xFFD4C5B0.toInt()  // Light beige
    val gradientEnd = 0xFFB8A596.toInt()    // Darker tan

    // Soft ambient lighting
    val ambientLightIntensity = 0.7f
    val ambientLightColor = 0xFFFFF8E1.toInt()  // Warm white
}

/**
 * Perspective and 3D effects
 */
object PerspectiveStyle {
    // Slight 3D tilt angle (degrees)
    val tiltAngle = 5f

    // Distance between windows and viewer (meters)
    val viewDistance = 2f

    // Optional horizontal curvature for large windows
    val curvatureRadius = 3f  // meters
    val curvatureEnabled = true
}
