package com.augmentalis.avaelements.core.theme.presets

import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.theme.*
import com.augmentalis.avaelements.core.tokens.*

/**
 * Frost Glass Theme
 *
 * Standard glassmorphism theme with frosted glass effects.
 *
 * Features:
 * - Frosted glass effect with blur backgrounds
 * - High transparency for layering
 * - Vivid, vibrant borders
 * - Soft, diffused shadows
 * - Backdrop blur support
 * - Layered depth with multiple glass surfaces
 *
 * Platform mappings:
 * - Web: backdrop-filter with blur
 * - iOS: UIBlurEffect with vibrancy
 * - Android: RenderEffect blur (API 31+)
 * - XR: Frosted glass materials
 */
object FrostGlassTheme {
    /**
     * Light + Dark theme pair
     */
    val ThemePair: com.augmentalis.avaelements.core.theme.ThemePair = createFrostGlassPair()

    /**
     * Light theme only
     */
    val Light: UniversalTheme = ThemePair.light

    /**
     * Dark theme only
     */
    val Dark: UniversalTheme = ThemePair.dark

    /**
     * Create FrostGlass theme pair
     */
    private fun createFrostGlassPair(): com.augmentalis.avaelements.core.theme.ThemePair {
        return UniversalTheme.createPair(
            id = "frostglass",
            name = "Frost Glass",
            visualStyle = VisualStyle.GLASSMORPHISM,
            lightTokens = createLightTokens(),
            darkTokens = createDarkTokens()
        )
    }

    /**
     * FrostGlass Light Theme Tokens
     */
    private fun createLightTokens(): DesignTokens {
        return DesignTokens(
            color = ColorTokens(
                // Primary (Vibrant Purple for glass)
                primary = ColorScale(
                    shade50 = Color(0xF3, 0xE5, 0xF5),
                    shade100 = Color(0xE1, 0xBE, 0xE7),
                    shade200 = Color(0xCE, 0x93, 0xD8),
                    shade300 = Color(0xBA, 0x68, 0xC8),
                    shade400 = Color(0xAB, 0x47, 0xBC),
                    shade500 = Color(0xAA, 0x55, 0xFF),  // Vibrant main
                    shade600 = Color(0x99, 0x4C, 0xE5),
                    shade700 = Color(0x88, 0x43, 0xCC),
                    shade800 = Color(0x77, 0x3A, 0xB2),
                    shade900 = Color(0x66, 0x31, 0x99),
                    main = Color(0xAA, 0x55, 0xFF),
                    light = Color(0xBA, 0x68, 0xC8),
                    dark = Color(0x88, 0x43, 0xCC),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Secondary (Vibrant Cyan)
                secondary = ColorScale(
                    shade50 = Color(0xE0, 0xF7, 0xFA),
                    shade100 = Color(0xB2, 0xEB, 0xF2),
                    shade200 = Color(0x80, 0xDE, 0xEA),
                    shade300 = Color(0x4D, 0xD0, 0xE1),
                    shade400 = Color(0x26, 0xC6, 0xDA),
                    shade500 = Color(0x00, 0xDD, 0xFF),  // Vibrant main
                    shade600 = Color(0x00, 0xC7, 0xE5),
                    shade700 = Color(0x00, 0xB0, 0xCC),
                    shade800 = Color(0x00, 0x99, 0xB2),
                    shade900 = Color(0x00, 0x82, 0x99),
                    main = Color(0x00, 0xDD, 0xFF),
                    light = Color(0x4D, 0xD0, 0xE1),
                    dark = Color(0x00, 0xB0, 0xCC),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Tertiary (Vibrant Pink)
                tertiary = ColorScale(
                    shade50 = Color(0xFC, 0xE4, 0xEC),
                    shade100 = Color(0xF8, 0xBB, 0xD0),
                    shade200 = Color(0xF4, 0x8F, 0xB1),
                    shade300 = Color(0xF0, 0x62, 0x92),
                    shade400 = Color(0xEC, 0x40, 0x7A),
                    shade500 = Color(0xFF, 0x00, 0x88),  // Vibrant main
                    shade600 = Color(0xE5, 0x00, 0x7A),
                    shade700 = Color(0xCC, 0x00, 0x6C),
                    shade800 = Color(0xB2, 0x00, 0x5E),
                    shade900 = Color(0x99, 0x00, 0x50),
                    main = Color(0xFF, 0x00, 0x88),
                    light = Color(0xF0, 0x62, 0x92),
                    dark = Color(0xCC, 0x00, 0x6C),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Error (Vibrant Red with transparency)
                error = ColorScale(
                    shade50 = Color(0xFF, 0xEB, 0xEE),
                    shade100 = Color(0xFF, 0xCD, 0xD2),
                    shade200 = Color(0xEF, 0x9A, 0x9A),
                    shade300 = Color(0xE5, 0x73, 0x73),
                    shade400 = Color(0xEF, 0x53, 0x50),
                    shade500 = Color(0xFF, 0x33, 0x55),  // Vibrant main
                    shade600 = Color(0xE5, 0x2E, 0x4C),
                    shade700 = Color(0xCC, 0x29, 0x43),
                    shade800 = Color(0xB2, 0x24, 0x3A),
                    shade900 = Color(0x99, 0x1F, 0x31),
                    main = Color(0xFF, 0x33, 0x55),
                    light = Color(0xEF, 0x53, 0x50),
                    dark = Color(0xCC, 0x29, 0x43),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Warning (Vibrant Amber)
                warning = ColorScale(
                    shade50 = Color(0xFF, 0xF8, 0xE1),
                    shade100 = Color(0xFF, 0xEC, 0xB3),
                    shade200 = Color(0xFF, 0xE0, 0x82),
                    shade300 = Color(0xFF, 0xD5, 0x4F),
                    shade400 = Color(0xFF, 0xCA, 0x28),
                    shade500 = Color(0xFF, 0xBB, 0x00),  // Vibrant main
                    shade600 = Color(0xE5, 0xA8, 0x00),
                    shade700 = Color(0xCC, 0x95, 0x00),
                    shade800 = Color(0xB2, 0x82, 0x00),
                    shade900 = Color(0x99, 0x6F, 0x00),
                    main = Color(0xFF, 0xBB, 0x00),
                    light = Color(0xFF, 0xD5, 0x4F),
                    dark = Color(0xCC, 0x95, 0x00),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Info (Vibrant Blue)
                info = ColorScale(
                    shade50 = Color(0xE3, 0xF2, 0xFD),
                    shade100 = Color(0xBB, 0xDE, 0xFB),
                    shade200 = Color(0x90, 0xCA, 0xF9),
                    shade300 = Color(0x64, 0xB5, 0xF6),
                    shade400 = Color(0x42, 0xA5, 0xF5),
                    shade500 = Color(0x00, 0x88, 0xFF),  // Vibrant main
                    shade600 = Color(0x00, 0x77, 0xE5),
                    shade700 = Color(0x00, 0x66, 0xCC),
                    shade800 = Color(0x00, 0x55, 0xB2),
                    shade900 = Color(0x00, 0x44, 0x99),
                    main = Color(0x00, 0x88, 0xFF),
                    light = Color(0x64, 0xB5, 0xF6),
                    dark = Color(0x00, 0x66, 0xCC),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Success (Vibrant Green)
                success = ColorScale(
                    shade50 = Color(0xE8, 0xF5, 0xE9),
                    shade100 = Color(0xC8, 0xE6, 0xC9),
                    shade200 = Color(0xA5, 0xD6, 0xA7),
                    shade300 = Color(0x81, 0xC7, 0x84),
                    shade400 = Color(0x66, 0xBB, 0x6A),
                    shade500 = Color(0x00, 0xCC, 0x66),  // Vibrant main
                    shade600 = Color(0x00, 0xB8, 0x5C),
                    shade700 = Color(0x00, 0xA3, 0x52),
                    shade800 = Color(0x00, 0x8F, 0x48),
                    shade900 = Color(0x00, 0x7A, 0x3E),
                    main = Color(0x00, 0xCC, 0x66),
                    light = Color(0x81, 0xC7, 0x84),
                    dark = Color(0x00, 0xA3, 0x52),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Neutral (Grays with transparency)
                neutral = ColorScale(
                    shade50 = Color(0xFA, 0xFA, 0xFA, 0.5f),
                    shade100 = Color(0xF5, 0xF5, 0xF5, 0.6f),
                    shade200 = Color(0xEE, 0xEE, 0xEE, 0.7f),
                    shade300 = Color(0xE0, 0xE0, 0xE0, 0.8f),
                    shade400 = Color(0xBD, 0xBD, 0xBD, 0.9f),
                    shade500 = Color(0x9E, 0x9E, 0x9E, 1.0f),
                    shade600 = Color(0x75, 0x75, 0x75, 0.9f),
                    shade700 = Color(0x61, 0x61, 0x61, 0.8f),
                    shade800 = Color(0x42, 0x42, 0x42, 0.7f),
                    shade900 = Color(0x21, 0x21, 0x21, 0.6f),
                    main = Color(0x9E, 0x9E, 0x9E, 1.0f),
                    light = Color(0xE0, 0xE0, 0xE0, 0.8f),
                    dark = Color(0x61, 0x61, 0x61, 0.8f),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Surface colors (frosted glass)
                surface = SurfaceColors(
                    background = Color(0xFF, 0xFF, 0xFF, 0.05f),    // Nearly transparent
                    surface = Color(0xFF, 0xFF, 0xFF, 0.3f),         // 30% white frosted glass
                    surfaceVariant = Color(0xF5, 0xF5, 0xF5, 0.5f),  // 50% variant
                    surfaceTint = Color(0xAA, 0x55, 0xFF, 0.1f),    // Purple tint
                    inverseSurface = Color(0x21, 0x21, 0x21, 0.95f)
                ),

                // Text colors (high contrast for glass)
                text = TextColors(
                    primary = Color(0x00, 0x00, 0x00, 0.95f),      // Almost opaque
                    secondary = Color(0x00, 0x00, 0x00, 0.70f),
                    disabled = Color(0x00, 0x00, 0x00, 0.40f),
                    hint = Color(0x00, 0x00, 0x00, 0.40f),
                    inverse = Color(0xFF, 0xFF, 0xFF, 0.95f)
                ),

                // Border colors (vivid for glass)
                border = BorderColors(
                    default = Color(0xFF, 0xFF, 0xFF, 0.4f),       // Visible white border
                    subtle = Color(0xFF, 0xFF, 0xFF, 0.2f),
                    focus = Color(0xAA, 0x55, 0xFF, 0.9f),         // Vivid purple
                    error = Color(0xFF, 0x33, 0x55, 0.9f)          // Vivid red
                )
            ),

            // Spacing (standard 8dp)
            spacing = SpacingTokens(
                unit = 8f,
                none = 0f,
                xs = 4f,
                sm = 8f,
                md = 16f,
                lg = 24f,
                xl = 32f,
                xxl = 48f,
                xxxl = 64f,
                paddingSmall = 8f,
                paddingMedium = 16f,
                paddingLarge = 24f,
                gapSmall = 8f,
                gapMedium = 16f,
                gapLarge = 24f
            ),

            // Typography (system-ui for platform consistency)
            typography = TypographyTokens(
                displayLarge = TextStyle(
                    fontSize = 57f,
                    lineHeight = 64f,
                    fontWeight = FontWeight.BOLD,
                    letterSpacing = -0.25f,
                    fontFamily = "system-ui"
                ),
                displayMedium = TextStyle(
                    fontSize = 45f,
                    lineHeight = 52f,
                    fontWeight = FontWeight.BOLD,
                    letterSpacing = 0f,
                    fontFamily = "system-ui"
                ),
                displaySmall = TextStyle(
                    fontSize = 36f,
                    lineHeight = 44f,
                    fontWeight = FontWeight.BOLD,
                    letterSpacing = 0f,
                    fontFamily = "system-ui"
                ),
                headlineLarge = TextStyle(
                    fontSize = 32f,
                    lineHeight = 40f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = 0f,
                    fontFamily = "system-ui"
                ),
                headlineMedium = TextStyle(
                    fontSize = 28f,
                    lineHeight = 36f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = 0f,
                    fontFamily = "system-ui"
                ),
                headlineSmall = TextStyle(
                    fontSize = 24f,
                    lineHeight = 32f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = 0f,
                    fontFamily = "system-ui"
                ),
                titleLarge = TextStyle(
                    fontSize = 22f,
                    lineHeight = 28f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0f,
                    fontFamily = "system-ui"
                ),
                titleMedium = TextStyle(
                    fontSize = 16f,
                    lineHeight = 24f,
                    fontWeight = FontWeight.MEDIUM,
                    letterSpacing = 0.15f,
                    fontFamily = "system-ui"
                ),
                titleSmall = TextStyle(
                    fontSize = 14f,
                    lineHeight = 20f,
                    fontWeight = FontWeight.MEDIUM,
                    letterSpacing = 0.1f,
                    fontFamily = "system-ui"
                ),
                bodyLarge = TextStyle(
                    fontSize = 16f,
                    lineHeight = 24f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0.5f,
                    fontFamily = "system-ui"
                ),
                bodyMedium = TextStyle(
                    fontSize = 14f,
                    lineHeight = 20f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0.25f,
                    fontFamily = "system-ui"
                ),
                bodySmall = TextStyle(
                    fontSize = 12f,
                    lineHeight = 16f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0.4f,
                    fontFamily = "system-ui"
                ),
                labelLarge = TextStyle(
                    fontSize = 14f,
                    lineHeight = 20f,
                    fontWeight = FontWeight.MEDIUM,
                    letterSpacing = 0.1f,
                    fontFamily = "system-ui"
                ),
                labelMedium = TextStyle(
                    fontSize = 12f,
                    lineHeight = 16f,
                    fontWeight = FontWeight.MEDIUM,
                    letterSpacing = 0.5f,
                    fontFamily = "system-ui"
                ),
                labelSmall = TextStyle(
                    fontSize = 11f,
                    lineHeight = 16f,
                    fontWeight = FontWeight.MEDIUM,
                    letterSpacing = 0.5f,
                    fontFamily = "system-ui"
                )
            ),

            // Border radius (very rounded for glass)
            radius = RadiusTokens(
                none = 0f,
                xs = 6f,
                sm = 12f,
                md = 16f,     // More rounded
                lg = 20f,
                xl = 28f,
                xxl = 36f,
                full = 9999f,
                button = 16f,
                card = 20f,
                dialog = 28f,
                input = 16f
            ),

            // Elevation (diffused shadows for glass)
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
                    shadowColor = Color(0xAA, 0x55, 0xFF, 0.15f),  // Purple shadow
                    offsetX = 0f,
                    offsetY = 3f,
                    blurRadius = 12f,
                    spreadRadius = -2f
                ),
                level2 = Elevation(
                    level = 2,
                    shadowColor = Color(0xAA, 0x55, 0xFF, 0.2f),
                    offsetX = 0f,
                    offsetY = 6f,
                    blurRadius = 18f,
                    spreadRadius = -3f
                ),
                level3 = Elevation(
                    level = 3,
                    shadowColor = Color(0xAA, 0x55, 0xFF, 0.25f),
                    offsetX = 0f,
                    offsetY = 10f,
                    blurRadius = 24f,
                    spreadRadius = -4f
                ),
                level4 = Elevation(
                    level = 4,
                    shadowColor = Color(0xAA, 0x55, 0xFF, 0.3f),
                    offsetX = 0f,
                    offsetY = 14f,
                    blurRadius = 32f,
                    spreadRadius = -5f
                ),
                level5 = Elevation(
                    level = 5,
                    shadowColor = Color(0xAA, 0x55, 0xFF, 0.35f),
                    offsetX = 0f,
                    offsetY = 20f,
                    blurRadius = 40f,
                    spreadRadius = -6f
                )
            ),

            // Motion (fluid animations)
            motion = MotionTokens(
                durationFast = 150,
                durationNormal = 350,
                durationSlow = 600,
                durationPageTransition = 500,
                easingStandard = "cubic-bezier(0.4, 0.0, 0.2, 1)",
                easingDecelerate = "cubic-bezier(0.0, 0.0, 0.2, 1)",
                easingAccelerate = "cubic-bezier(0.4, 0.0, 1, 1)",
                easingSharp = "cubic-bezier(0.4, 0.0, 0.6, 1)"
            ),

            // Breakpoints (standard responsive)
            breakpoints = BreakpointTokens(
                xs = 0,
                sm = 600,
                md = 960,
                lg = 1280,
                xl = 1920
            ),

            // Z-index
            zIndex = ZIndexTokens(
                base = 0,
                dropdown = 1000,
                sticky = 1100,
                modal = 1300,
                popover = 1400,
                toast = 1500,
                tooltip = 1600
            )
        )
    }

    /**
     * FrostGlass Dark Theme Tokens
     */
    private fun createDarkTokens(): DesignTokens {
        val lightTokens = createLightTokens()

        return lightTokens.copy(
            color = ColorTokens(
                // Keep vibrant colors (same as light for glass effect)
                primary = lightTokens.color.primary,
                secondary = lightTokens.color.secondary,
                tertiary = lightTokens.color.tertiary,
                error = lightTokens.color.error,
                warning = lightTokens.color.warning,
                info = lightTokens.color.info,
                success = lightTokens.color.success,

                // Dark neutral scale
                neutral = ColorScale(
                    shade50 = Color(0x21, 0x21, 0x21, 0.5f),
                    shade100 = Color(0x42, 0x42, 0x42, 0.6f),
                    shade200 = Color(0x61, 0x61, 0x61, 0.7f),
                    shade300 = Color(0x75, 0x75, 0x75, 0.8f),
                    shade400 = Color(0x9E, 0x9E, 0x9E, 0.9f),
                    shade500 = Color(0xBD, 0xBD, 0xBD, 1.0f),
                    shade600 = Color(0xE0, 0xE0, 0xE0, 0.9f),
                    shade700 = Color(0xEE, 0xEE, 0xEE, 0.8f),
                    shade800 = Color(0xF5, 0xF5, 0xF5, 0.7f),
                    shade900 = Color(0xFA, 0xFA, 0xFA, 0.6f),
                    main = Color(0xBD, 0xBD, 0xBD, 1.0f),
                    light = Color(0xE0, 0xE0, 0xE0, 0.9f),
                    dark = Color(0x75, 0x75, 0x75, 0.8f),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Dark frosted glass surfaces
                surface = SurfaceColors(
                    background = Color(0x00, 0x00, 0x00, 0.05f),    // Nearly transparent
                    surface = Color(0x18, 0x18, 0x18, 0.4f),         // 40% dark frosted glass
                    surfaceVariant = Color(0x2C, 0x2C, 0x2C, 0.6f),  // 60% variant
                    surfaceTint = Color(0xAA, 0x55, 0xFF, 0.15f),   // Purple tint
                    inverseSurface = Color(0xF5, 0xF5, 0xF5, 0.95f)
                ),

                // Light text on dark
                text = TextColors(
                    primary = Color(0xFF, 0xFF, 0xFF, 0.95f),
                    secondary = Color(0xFF, 0xFF, 0xFF, 0.70f),
                    disabled = Color(0xFF, 0xFF, 0xFF, 0.40f),
                    hint = Color(0xFF, 0xFF, 0xFF, 0.40f),
                    inverse = Color(0x00, 0x00, 0x00, 0.95f)
                ),

                // Dark borders (vivid)
                border = BorderColors(
                    default = Color(0xFF, 0xFF, 0xFF, 0.2f),
                    subtle = Color(0xFF, 0xFF, 0xFF, 0.1f),
                    focus = Color(0xAA, 0x55, 0xFF, 0.9f),
                    error = Color(0xFF, 0x33, 0x55, 0.9f)
                )
            ),

            // Darker shadows with purple glow
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
                    shadowColor = Color(0xAA, 0x55, 0xFF, 0.25f),
                    offsetX = 0f,
                    offsetY = 3f,
                    blurRadius = 12f,
                    spreadRadius = -2f
                ),
                level2 = Elevation(
                    level = 2,
                    shadowColor = Color(0xAA, 0x55, 0xFF, 0.35f),
                    offsetX = 0f,
                    offsetY = 6f,
                    blurRadius = 18f,
                    spreadRadius = -3f
                ),
                level3 = Elevation(
                    level = 3,
                    shadowColor = Color(0xAA, 0x55, 0xFF, 0.45f),
                    offsetX = 0f,
                    offsetY = 10f,
                    blurRadius = 24f,
                    spreadRadius = -4f
                ),
                level4 = Elevation(
                    level = 4,
                    shadowColor = Color(0xAA, 0x55, 0xFF, 0.55f),
                    offsetX = 0f,
                    offsetY = 14f,
                    blurRadius = 32f,
                    spreadRadius = -5f
                ),
                level5 = Elevation(
                    level = 5,
                    shadowColor = Color(0xAA, 0x55, 0xFF, 0.65f),
                    offsetX = 0f,
                    offsetY = 20f,
                    blurRadius = 40f,
                    spreadRadius = -6f
                )
            )
        )
    }
}
