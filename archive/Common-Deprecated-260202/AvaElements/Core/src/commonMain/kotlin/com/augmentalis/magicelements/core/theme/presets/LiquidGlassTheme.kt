package com.augmentalis.avaelements.core.theme.presets

import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.theme.*
import com.augmentalis.avaelements.core.tokens.*

/**
 * Liquid Glass Theme
 *
 * Augmentalis' original liquid glass design language with subtle glassmorphism.
 *
 * Features:
 * - Subtle glassmorphism effects
 * - Soft shadows with blur
 * - System-optimized typography
 * - Rounded corners (10pt default)
 * - 8pt spacing grid
 * - System blue accents
 * - Vibrancy effects
 *
 * Platform mappings:
 * - iOS: Native SwiftUI with blur effects
 * - Android: Glass-styled components
 * - Web: backdrop-filter for glass effect
 * - XR: Translucent materials
 */
object LiquidGlassTheme {
    /**
     * Light + Dark theme pair
     */
    val ThemePair: com.augmentalis.avaelements.core.theme.ThemePair = createLiquidGlassPair()

    /**
     * Light theme only
     */
    val Light: UniversalTheme = ThemePair.light

    /**
     * Dark theme only
     */
    val Dark: UniversalTheme = ThemePair.dark

    /**
     * Create LiquidGlass theme pair
     */
    private fun createLiquidGlassPair(): com.augmentalis.avaelements.core.theme.ThemePair {
        return UniversalTheme.createPair(
            id = "liquidglass",
            name = "Liquid Glass",
            visualStyle = VisualStyle.IOS26_LIQUID_GLASS,
            lightTokens = createLightTokens(),
            darkTokens = createDarkTokens()
        )
    }

    /**
     * LiquidGlass Light Theme Tokens
     */
    private fun createLightTokens(): DesignTokens {
        // Use ModernUITheme as base and customize colors for glass effect
        val baseTokens = ModernUITheme.Light.tokens

        return baseTokens.copy(
            color = ColorTokens(
                // Primary (System Blue)
                primary = ColorScale(
                    shade50 = Color(0xE3, 0xF2, 0xFD),
                    shade100 = Color(0xBB, 0xDE, 0xFB),
                    shade200 = Color(0x90, 0xCA, 0xF9),
                    shade300 = Color(0x64, 0xB5, 0xF6),
                    shade400 = Color(0x42, 0xA5, 0xF5),
                    shade500 = Color(0x00, 0x7A, 0xFF),  // Main
                    shade600 = Color(0x00, 0x6D, 0xE5),
                    shade700 = Color(0x00, 0x5E, 0xCC),
                    shade800 = Color(0x00, 0x4F, 0xB2),
                    shade900 = Color(0x00, 0x3D, 0x99),
                    main = Color(0x00, 0x7A, 0xFF),
                    light = Color(0x42, 0xA5, 0xF5),
                    dark = Color(0x00, 0x5E, 0xCC),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Keep other colors from base
                secondary = baseTokens.color.secondary,
                tertiary = baseTokens.color.tertiary,
                error = baseTokens.color.error,
                warning = baseTokens.color.warning,
                info = baseTokens.color.info,
                success = baseTokens.color.success,
                neutral = baseTokens.color.neutral,

                // Glassmorphism surfaces
                surface = SurfaceColors(
                    background = Color(0xFF, 0xFF, 0xFF),       // Pure white
                    surface = Color(0xFF, 0xFF, 0xFF, 0.85f),    // 85% white (more transparent glass)
                    surfaceVariant = Color(0xF2, 0xF2, 0xF7, 0.9f),
                    surfaceTint = Color(0x00, 0x7A, 0xFF, 0.08f), // Very subtle blue tint
                    inverseSurface = Color(0x1C, 0x1C, 0x1E)
                ),

                text = baseTokens.color.text,
                border = baseTokens.color.border
            ),

            // More rounded corners for glass effect
            radius = RadiusTokens(
                none = 0f,
                xs = 4f,
                sm = 8f,
                md = 12f,   // More rounded than ModernUI
                lg = 16f,
                xl = 20f,
                xxl = 28f,
                full = 9999f,
                button = 12f,
                card = 16f,
                dialog = 24f,
                input = 12f
            ),

            // Softer shadows for glass
            elevation = ElevationTokens(
                level0 = Elevation(
                    level = 0,
                    shadowColor = Color(0, 0, 0, 0f),
                    offsetX = 0f,
                    offsetY = 0f,
                    blurRadius = 0f,
                    spreadRadius = 0f
                ),
                level1 = Elevation(
                    level = 1,
                    shadowColor = Color(0, 0, 0, 0.08f),  // Softer
                    offsetX = 0f,
                    offsetY = 2f,
                    blurRadius = 8f,
                    spreadRadius = 0f
                ),
                level2 = Elevation(
                    level = 2,
                    shadowColor = Color(0, 0, 0, 0.1f),
                    offsetX = 0f,
                    offsetY = 4f,
                    blurRadius = 12f,
                    spreadRadius = 0f
                ),
                level3 = Elevation(
                    level = 3,
                    shadowColor = Color(0, 0, 0, 0.12f),
                    offsetX = 0f,
                    offsetY = 6f,
                    blurRadius = 16f,
                    spreadRadius = 0f
                ),
                level4 = Elevation(
                    level = 4,
                    shadowColor = Color(0, 0, 0, 0.15f),
                    offsetX = 0f,
                    offsetY = 10f,
                    blurRadius = 20f,
                    spreadRadius = 0f
                ),
                level5 = Elevation(
                    level = 5,
                    shadowColor = Color(0, 0, 0, 0.18f),
                    offsetX = 0f,
                    offsetY = 14f,
                    blurRadius = 28f,
                    spreadRadius = 0f
                )
            ),

            // Spring-like animations
            motion = MotionTokens(
                durationFast = 150,
                durationNormal = 350,
                durationSlow = 600,
                durationPageTransition = 500,
                easingStandard = "cubic-bezier(0.25, 0.1, 0.25, 1)",  // Spring-like
                easingDecelerate = "cubic-bezier(0.0, 0.0, 0.2, 1)",
                easingAccelerate = "cubic-bezier(0.4, 0.0, 1, 1)",
                easingSharp = "cubic-bezier(0.4, 0.0, 0.6, 1)"
            )
        )
    }

    /**
     * LiquidGlass Dark Theme Tokens
     */
    private fun createDarkTokens(): DesignTokens {
        val baseTokens = ModernUITheme.Dark.tokens

        return baseTokens.copy(
            color = ColorTokens(
                // Primary (Blue - lighter for dark mode)
                primary = ColorScale(
                    shade50 = Color(0x00, 0x3D, 0x99),
                    shade100 = Color(0x00, 0x4F, 0xB2),
                    shade200 = Color(0x00, 0x5E, 0xCC),
                    shade300 = Color(0x00, 0x6D, 0xE5),
                    shade400 = Color(0x00, 0x7A, 0xFF),
                    shade500 = Color(0x0A, 0x84, 0xFF),  // Main - lighter
                    shade600 = Color(0x42, 0xA5, 0xF5),
                    shade700 = Color(0x64, 0xB5, 0xF6),
                    shade800 = Color(0x90, 0xCA, 0xF9),
                    shade900 = Color(0xBB, 0xDE, 0xFB),
                    main = Color(0x0A, 0x84, 0xFF),
                    light = Color(0x64, 0xB5, 0xF6),
                    dark = Color(0x00, 0x6D, 0xE5),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Keep other colors from base
                secondary = baseTokens.color.secondary,
                tertiary = baseTokens.color.tertiary,
                error = baseTokens.color.error,
                warning = baseTokens.color.warning,
                info = baseTokens.color.info,
                success = baseTokens.color.success,
                neutral = baseTokens.color.neutral,

                // Dark glass surfaces
                surface = SurfaceColors(
                    background = Color(0x00, 0x00, 0x00),        // Pure black
                    surface = Color(0x1C, 0x1C, 0x1E, 0.85f),     // 85% dark gray (glass)
                    surfaceVariant = Color(0x2C, 0x2C, 0x2E, 0.9f),
                    surfaceTint = Color(0x0A, 0x84, 0xFF, 0.08f), // Subtle blue tint
                    inverseSurface = Color(0xF2, 0xF2, 0xF7)
                ),

                text = baseTokens.color.text,
                border = baseTokens.color.border
            ),

            // More rounded corners (same as light)
            radius = createLightTokens().radius,

            // Darker, softer shadows for dark mode
            elevation = ElevationTokens(
                level0 = Elevation(
                    level = 0,
                    shadowColor = Color(0, 0, 0, 0f),
                    offsetX = 0f,
                    offsetY = 0f,
                    blurRadius = 0f,
                    spreadRadius = 0f
                ),
                level1 = Elevation(
                    level = 1,
                    shadowColor = Color(0, 0, 0, 0.25f),
                    offsetX = 0f,
                    offsetY = 2f,
                    blurRadius = 8f,
                    spreadRadius = 0f
                ),
                level2 = Elevation(
                    level = 2,
                    shadowColor = Color(0, 0, 0, 0.35f),
                    offsetX = 0f,
                    offsetY = 4f,
                    blurRadius = 12f,
                    spreadRadius = 0f
                ),
                level3 = Elevation(
                    level = 3,
                    shadowColor = Color(0, 0, 0, 0.45f),
                    offsetX = 0f,
                    offsetY = 6f,
                    blurRadius = 16f,
                    spreadRadius = 0f
                ),
                level4 = Elevation(
                    level = 4,
                    shadowColor = Color(0, 0, 0, 0.55f),
                    offsetX = 0f,
                    offsetY = 10f,
                    blurRadius = 20f,
                    spreadRadius = 0f
                ),
                level5 = Elevation(
                    level = 5,
                    shadowColor = Color(0, 0, 0, 0.65f),
                    offsetX = 0f,
                    offsetY = 14f,
                    blurRadius = 28f,
                    spreadRadius = 0f
                )
            ),

            // Motion (same as light)
            motion = createLightTokens().motion
        )
    }
}
