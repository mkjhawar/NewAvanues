package com.augmentalis.cockpit.mvp

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ocean Theme tokens for MagicUI glassmorphic styling
 * Pre-MagicUI wrapper implementation
 *
 * Complete design token system for consistent UI styling
 * Migration-ready: When MagicUI ships, replace imports only
 */
object OceanTheme {
    // ========================================
    // Glassmorphic Surface Tokens
    // ========================================
    val glassSurface = Color.White.copy(alpha = 0.03f)        // 3% - nearly transparent
    val glassSurfaceHover = Color.White.copy(alpha = 0.05f)   // 5% - subtle hover
    val glassSurfacePressed = Color.White.copy(alpha = 0.08f) // 8% - gentle press
    val glassBorder = Color.White.copy(alpha = 0.10f)         // 10% - hair-thin
    val glassBorderFocus = Color.White.copy(alpha = 0.25f)    // 25% (unchanged)
    val glassBlur = 32.dp                                     // 32dp - stronger blur
    val glassShadow = Color.Black.copy(alpha = 0.08f)         // 8% - soft shadow

    // Advanced Glass Effects (XR Materials)
    val glassRefraction = 0.12f                               // Refraction strength
    val glassFresnelPower = 3.5f                             // Fresnel edge glow
    val glassRimLight = Color.White.copy(alpha = 0.15f)      // Edge highlight
    val glassAmbientOcclusion = Color.Black.copy(alpha = 0.20f) // Corner darkening

    // ========================================
    // Color Palette
    // ========================================
    val primary = Color(0xFF60A5FA)
    val primaryDark = Color(0xFF3B82F6)
    val success = Color(0xFF34D399)
    val successDark = Color(0xFF10B981)
    val error = Color(0xFFF87171)
    val errorDark = Color(0xFFEF4444)
    val warning = Color(0xFFFBBF24)
    val warningDark = Color(0xFFF59E0B)
    val info = Color(0xFF60A5FA)
    val infoDark = Color(0xFF3B82F6)

    // ========================================
    // Deep Ocean Professional Palette
    // ========================================
    // Core ocean depths
    val oceanDeep = Color(0xFF0A2540)        // Deep midnight blue
    val oceanMid = Color(0xFF1A4D6F)         // Professional medium blue
    val oceanLight = Color(0xFF2E5C7F)       // Muted light blue
    val oceanAccent = Color(0xFF3A7CA5)      // Subtle accent blue
    val oceanFrost = Color(0xFF4A90B8)       // Frosted glass highlight

    // Chrome/Steel Metallics
    val chromeLight = Color(0xFFB8C5D0)      // Light chrome
    val chromeMid = Color(0xFF8A9BA8)        // Medium chrome
    val chromeDark = Color(0xFF4A5A6A)       // Dark chrome shadow

    // Bioluminescent Accents (subtle, not neon)
    val biolumBlue = Color(0xFF2D5F7F)       // Deep bioluminescent blue
    val biolumTeal = Color(0xFF2A6B6A)       // Muted teal glow
    val biolumAqua = Color(0xFF3A7B8A)       // Subdued aqua

    // Text colors
    val textPrimary = Color.White.copy(alpha = 0.95f)
    val textSecondary = Color.White.copy(alpha = 0.80f)
    val textTertiary = Color.White.copy(alpha = 0.60f)
    val textDisabled = Color.White.copy(alpha = 0.40f)

    // Background gradient
    val backgroundStart = Color(0xFF1A1A2E)
    val backgroundEnd = Color(0xFF16213E)
    val backgroundOverlay = Color.Black.copy(alpha = 0.30f)

    // ========================================
    // Shapes
    // ========================================
    val glassShapeDefault: Shape = RoundedCornerShape(6.dp)
    val glassShapeSmall: Shape = RoundedCornerShape(4.dp)
    val glassShapeLarge: Shape = RoundedCornerShape(8.dp)
    val glassShapeButton: Shape = RoundedCornerShape(6.dp)
    val glassShapeCard: Shape = RoundedCornerShape(8.dp)
    val glassShapeDialog: Shape = RoundedCornerShape(10.dp)

    // ========================================
    // Elevations
    // ========================================
    val elevationDefault: Dp = 4.dp
    val elevationHigh: Dp = 6.dp
    val elevationLow: Dp = 2.dp
    val shadowElevationDefault: Dp = 8.dp
    val shadowElevationHigh: Dp = 12.dp
    val shadowElevationLow: Dp = 4.dp

    // ========================================
    // Sizes - Windows
    // ========================================
    val windowWidthDefault: Dp = 240.dp
    val windowHeightDefault: Dp = 180.dp
    val windowWidthLarge: Dp = 320.dp
    val windowHeightLarge: Dp = 240.dp
    val windowWidthSmall: Dp = 180.dp
    val windowHeightSmall: Dp = 135.dp

    // ========================================
    // Sizes - Buttons & Icons
    // ========================================
    val buttonSizeDefault: Dp = 40.dp
    val buttonSizeSmall: Dp = 32.dp
    val buttonSizeLarge: Dp = 48.dp
    val iconSizeDefault: Dp = 24.dp
    val iconSizeSmall: Dp = 18.dp
    val iconSizeLarge: Dp = 32.dp

    // ========================================
    // Spacing
    // ========================================
    val spacingXSmall: Dp = 4.dp
    val spacingSmall: Dp = 8.dp
    val spacingMedium: Dp = 12.dp
    val spacingDefault: Dp = 16.dp
    val spacingLarge: Dp = 24.dp
    val spacingXLarge: Dp = 32.dp

    // ========================================
    // Border Widths
    // ========================================
    val borderWidthDefault: Dp = 0.5.dp   // Hair-thin precision
    val borderWidthThick: Dp = 1.dp       // Refined thick state
    val borderWidthFocus: Dp = 1.5.dp     // Subtle focus glow

    // ========================================
    // Animation Durations (ms)
    // ========================================
    const val animationDurationFast = 150
    const val animationDurationDefault = 250
    const val animationDurationSlow = 350

    // ========================================
    // Physics-Based Animation
    // ========================================
    // Spring dynamics for natural XR motion
    const val springStiffnessLow = 200f      // Gentle, floaty motion
    const val springStiffnessMedium = 400f   // Balanced responsiveness
    const val springStiffnessHigh = 800f     // Snappy, precise motion
    const val springDampingRatio = 0.7f      // Slightly underdamped (natural bounce)

    // Easing curves (for non-spring animations)
    val easingDecelerate = androidx.compose.animation.core.CubicBezierEasing(
        0.0f, 0.0f, 0.2f, 1.0f
    )
    val easingAccelerate = androidx.compose.animation.core.CubicBezierEasing(
        0.4f, 0.0f, 1.0f, 1.0f
    )
    val easingStandard = androidx.compose.animation.core.CubicBezierEasing(
        0.4f, 0.0f, 0.2f, 1.0f
    )
}
