package com.augmentalis.avaelements.core.theme.presets

import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.theme.*
import com.augmentalis.avaelements.core.tokens.*

/**
 * Modern UI Theme
 *
 * Contemporary material design aesthetic inspired by modern design systems.
 *
 * Features:
 * - Dynamic color system with full color scales
 * - Elevation-based shadows for depth
 * - Rounded corners (8dp default)
 * - Clean sans-serif typography
 * - 8dp spacing grid for consistency
 *
 * Platform mappings:
 * - Android: Material3-compatible components
 * - iOS: Modern SwiftUI styling
 * - Web: Contemporary web design
 * - XR: Solid materials with elevation depth
 */
object ModernUITheme {
    /**
     * Light + Dark theme pair
     */
    val ThemePair: com.augmentalis.avaelements.core.theme.ThemePair = createModernUIPair()

    /**
     * Light theme only
     */
    val Light: UniversalTheme = ThemePair.light

    /**
     * Dark theme only
     */
    val Dark: UniversalTheme = ThemePair.dark

    /**
     * Create ModernUI theme pair
     */
    private fun createModernUIPair(): com.augmentalis.avaelements.core.theme.ThemePair {
        return UniversalTheme.createPair(
            id = "modernui",
            name = "Modern UI",
            visualStyle = VisualStyle.MATERIAL3,
            lightTokens = createLightTokens(),
            darkTokens = createDarkTokens()
        )
    }

    /**
     * ModernUI Light Theme Tokens
     */
    private fun createLightTokens(): DesignTokens {
        return DesignTokens(
            color = ColorTokens(
                // Primary (Purple)
                primary = ColorScale(
                    shade50 = Color(0xF3, 0xE5, 0xF5),   // #F3E5F5
                    shade100 = Color(0xE1, 0xBE, 0xE7),  // #E1BEE7
                    shade200 = Color(0xCE, 0x93, 0xD8),  // #CE93D8
                    shade300 = Color(0xBA, 0x68, 0xC8),  // #BA68C8
                    shade400 = Color(0xAB, 0x47, 0xBC),  // #AB47BC
                    shade500 = Color(0x9C, 0x27, 0xB0),  // #9C27B0 - Main
                    shade600 = Color(0x8E, 0x24, 0xAA),  // #8E24AA
                    shade700 = Color(0x7B, 0x1F, 0xA2),  // #7B1FA2
                    shade800 = Color(0x6A, 0x1B, 0x9A),  // #6A1B9A
                    shade900 = Color(0x4A, 0x14, 0x8C),  // #4A148C
                    main = Color(0x9C, 0x27, 0xB0),
                    light = Color(0xBA, 0x68, 0xC8),
                    dark = Color(0x7B, 0x1F, 0xA2),
                    contrastText = Color(0xFF, 0xFF, 0xFF) // White text on purple
                ),

                // Secondary (Teal)
                secondary = ColorScale(
                    shade50 = Color(0xE0, 0xF2, 0xF1),
                    shade100 = Color(0xB2, 0xDF, 0xDB),
                    shade200 = Color(0x80, 0xCB, 0xC4),
                    shade300 = Color(0x4D, 0xB6, 0xAC),
                    shade400 = Color(0x26, 0xA6, 0x9A),
                    shade500 = Color(0x00, 0x96, 0x88),  // Main
                    shade600 = Color(0x00, 0x89, 0x7B),
                    shade700 = Color(0x00, 0x79, 0x6B),
                    shade800 = Color(0x00, 0x69, 0x5C),
                    shade900 = Color(0x00, 0x4D, 0x40),
                    main = Color(0x00, 0x96, 0x88),
                    light = Color(0x4D, 0xB6, 0xAC),
                    dark = Color(0x00, 0x79, 0x6B),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Tertiary (Blue)
                tertiary = ColorScale(
                    shade50 = Color(0xE3, 0xF2, 0xFD),
                    shade100 = Color(0xBB, 0xDE, 0xFB),
                    shade200 = Color(0x90, 0xCA, 0xF9),
                    shade300 = Color(0x64, 0xB5, 0xF6),
                    shade400 = Color(0x42, 0xA5, 0xF5),
                    shade500 = Color(0x21, 0x96, 0xF3),
                    shade600 = Color(0x1E, 0x88, 0xE5),
                    shade700 = Color(0x19, 0x76, 0xD2),
                    shade800 = Color(0x15, 0x65, 0xC0),
                    shade900 = Color(0x0D, 0x47, 0xA1),
                    main = Color(0x21, 0x96, 0xF3),
                    light = Color(0x64, 0xB5, 0xF6),
                    dark = Color(0x19, 0x76, 0xD2),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Error (Red)
                error = ColorScale(
                    shade50 = Color(0xFF, 0xEB, 0xEE),
                    shade100 = Color(0xFF, 0xCD, 0xD2),
                    shade200 = Color(0xEF, 0x9A, 0x9A),
                    shade300 = Color(0xE5, 0x73, 0x73),
                    shade400 = Color(0xEF, 0x53, 0x50),
                    shade500 = Color(0xF4, 0x43, 0x36),  // Main
                    shade600 = Color(0xE5, 0x39, 0x35),
                    shade700 = Color(0xD3, 0x2F, 0x2F),
                    shade800 = Color(0xC6, 0x28, 0x28),
                    shade900 = Color(0xB7, 0x1C, 0x1C),
                    main = Color(0xF4, 0x43, 0x36),
                    light = Color(0xE5, 0x73, 0x73),
                    dark = Color(0xD3, 0x2F, 0x2F),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Warning (Orange)
                warning = ColorScale(
                    shade50 = Color(0xFF, 0xF3, 0xE0),
                    shade100 = Color(0xFF, 0xE0, 0xB2),
                    shade200 = Color(0xFF, 0xCC, 0x80),
                    shade300 = Color(0xFF, 0xB7, 0x4D),
                    shade400 = Color(0xFF, 0xA7, 0x26),
                    shade500 = Color(0xFF, 0x98, 0x00),  // Main
                    shade600 = Color(0xFB, 0x8C, 0x00),
                    shade700 = Color(0xF5, 0x7C, 0x00),
                    shade800 = Color(0xEF, 0x6C, 0x00),
                    shade900 = Color(0xE6, 0x51, 0x00),
                    main = Color(0xFF, 0x98, 0x00),
                    light = Color(0xFF, 0xB7, 0x4D),
                    dark = Color(0xF5, 0x7C, 0x00),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Info (Blue)
                info = ColorScale(
                    shade50 = Color(0xE3, 0xF2, 0xFD),
                    shade100 = Color(0xBB, 0xDE, 0xFB),
                    shade200 = Color(0x90, 0xCA, 0xF9),
                    shade300 = Color(0x64, 0xB5, 0xF6),
                    shade400 = Color(0x42, 0xA5, 0xF5),
                    shade500 = Color(0x21, 0x96, 0xF3),  // Main
                    shade600 = Color(0x1E, 0x88, 0xE5),
                    shade700 = Color(0x19, 0x76, 0xD2),
                    shade800 = Color(0x15, 0x65, 0xC0),
                    shade900 = Color(0x0D, 0x47, 0xA1),
                    main = Color(0x21, 0x96, 0xF3),
                    light = Color(0x64, 0xB5, 0xF6),
                    dark = Color(0x19, 0x76, 0xD2),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Success (Green)
                success = ColorScale(
                    shade50 = Color(0xE8, 0xF5, 0xE9),
                    shade100 = Color(0xC8, 0xE6, 0xC9),
                    shade200 = Color(0xA5, 0xD6, 0xA7),
                    shade300 = Color(0x81, 0xC7, 0x84),
                    shade400 = Color(0x66, 0xBB, 0x6A),
                    shade500 = Color(0x4C, 0xAF, 0x50),  // Main
                    shade600 = Color(0x43, 0xA0, 0x47),
                    shade700 = Color(0x38, 0x8E, 0x3C),
                    shade800 = Color(0x2E, 0x7D, 0x32),
                    shade900 = Color(0x1B, 0x5E, 0x20),
                    main = Color(0x4C, 0xAF, 0x50),
                    light = Color(0x81, 0xC7, 0x84),
                    dark = Color(0x38, 0x8E, 0x3C),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Neutral (Gray)
                neutral = ColorScale(
                    shade50 = Color(0xFA, 0xFA, 0xFA),
                    shade100 = Color(0xF5, 0xF5, 0xF5),
                    shade200 = Color(0xEE, 0xEE, 0xEE),
                    shade300 = Color(0xE0, 0xE0, 0xE0),
                    shade400 = Color(0xBD, 0xBD, 0xBD),
                    shade500 = Color(0x9E, 0x9E, 0x9E),
                    shade600 = Color(0x75, 0x75, 0x75),
                    shade700 = Color(0x61, 0x61, 0x61),
                    shade800 = Color(0x42, 0x42, 0x42),
                    shade900 = Color(0x21, 0x21, 0x21),
                    main = Color(0x9E, 0x9E, 0x9E),
                    light = Color(0xE0, 0xE0, 0xE0),
                    dark = Color(0x61, 0x61, 0x61),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Surface colors
                surface = SurfaceColors(
                    background = Color(0xFF, 0xFF, 0xFF),      // Pure white
                    surface = Color(0xFF, 0xFF, 0xFF),          // White
                    surfaceVariant = Color(0xF5, 0xF5, 0xF5),  // Light gray
                    surfaceTint = Color(0x9C, 0x27, 0xB0),     // Primary tint
                    inverseSurface = Color(0x21, 0x21, 0x21)   // Dark gray
                ),

                // Text colors
                text = TextColors(
                    primary = Color(0x00, 0x00, 0x00, 0.87f),     // 87% black
                    secondary = Color(0x00, 0x00, 0x00, 0.60f),   // 60% black
                    disabled = Color(0x00, 0x00, 0x00, 0.38f),    // 38% black
                    hint = Color(0x00, 0x00, 0x00, 0.38f),
                    inverse = Color(0xFF, 0xFF, 0xFF, 0.87f)      // White on dark
                ),

                // Border colors
                border = BorderColors(
                    default = Color(0xE0, 0xE0, 0xE0),           // Light gray
                    subtle = Color(0xF5, 0xF5, 0xF5),
                    focus = Color(0x9C, 0x27, 0xB0),             // Primary
                    error = Color(0xF4, 0x43, 0x36)              // Error red
                )
            ),

            // Spacing (8dp grid)
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

            // Typography (Clean sans-serif)
            typography = TypographyTokens(
                displayLarge = TextStyle(
                    fontSize = 57f,
                    lineHeight = 64f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = -0.25f,
                    fontFamily = "sans-serif"
                ),
                displayMedium = TextStyle(
                    fontSize = 45f,
                    lineHeight = 52f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0f,
                    fontFamily = "sans-serif"
                ),
                displaySmall = TextStyle(
                    fontSize = 36f,
                    lineHeight = 44f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0f,
                    fontFamily = "sans-serif"
                ),
                headlineLarge = TextStyle(
                    fontSize = 32f,
                    lineHeight = 40f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0f,
                    fontFamily = "sans-serif"
                ),
                headlineMedium = TextStyle(
                    fontSize = 28f,
                    lineHeight = 36f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0f,
                    fontFamily = "sans-serif"
                ),
                headlineSmall = TextStyle(
                    fontSize = 24f,
                    lineHeight = 32f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0f,
                    fontFamily = "sans-serif"
                ),
                titleLarge = TextStyle(
                    fontSize = 22f,
                    lineHeight = 28f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0f,
                    fontFamily = "sans-serif"
                ),
                titleMedium = TextStyle(
                    fontSize = 16f,
                    lineHeight = 24f,
                    fontWeight = FontWeight.MEDIUM,
                    letterSpacing = 0.15f,
                    fontFamily = "sans-serif"
                ),
                titleSmall = TextStyle(
                    fontSize = 14f,
                    lineHeight = 20f,
                    fontWeight = FontWeight.MEDIUM,
                    letterSpacing = 0.1f,
                    fontFamily = "sans-serif"
                ),
                bodyLarge = TextStyle(
                    fontSize = 16f,
                    lineHeight = 24f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0.5f,
                    fontFamily = "sans-serif"
                ),
                bodyMedium = TextStyle(
                    fontSize = 14f,
                    lineHeight = 20f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0.25f,
                    fontFamily = "sans-serif"
                ),
                bodySmall = TextStyle(
                    fontSize = 12f,
                    lineHeight = 16f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = 0.4f,
                    fontFamily = "sans-serif"
                ),
                labelLarge = TextStyle(
                    fontSize = 14f,
                    lineHeight = 20f,
                    fontWeight = FontWeight.MEDIUM,
                    letterSpacing = 0.1f,
                    fontFamily = "sans-serif"
                ),
                labelMedium = TextStyle(
                    fontSize = 12f,
                    lineHeight = 16f,
                    fontWeight = FontWeight.MEDIUM,
                    letterSpacing = 0.5f,
                    fontFamily = "sans-serif"
                ),
                labelSmall = TextStyle(
                    fontSize = 11f,
                    lineHeight = 16f,
                    fontWeight = FontWeight.MEDIUM,
                    letterSpacing = 0.5f,
                    fontFamily = "sans-serif"
                )
            ),

            // Border radius
            radius = RadiusTokens(
                none = 0f,
                xs = 2f,
                sm = 4f,
                md = 8f,
                lg = 12f,
                xl = 16f,
                xxl = 24f,
                full = 9999f,
                button = 8f,
                card = 12f,
                dialog = 28f,
                input = 4f
            ),

            // Elevation (5 levels)
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
                    shadowColor = Color(0, 0, 0, 0.2f),
                    offsetX = 0f,
                    offsetY = 1f,
                    blurRadius = 3f,
                    spreadRadius = 0f
                ),
                level2 = Elevation(
                    level = 2,
                    shadowColor = Color(0, 0, 0, 0.2f),
                    offsetX = 0f,
                    offsetY = 2f,
                    blurRadius = 6f,
                    spreadRadius = 0f
                ),
                level3 = Elevation(
                    level = 3,
                    shadowColor = Color(0, 0, 0, 0.2f),
                    offsetX = 0f,
                    offsetY = 4f,
                    blurRadius = 8f,
                    spreadRadius = 0f
                ),
                level4 = Elevation(
                    level = 4,
                    shadowColor = Color(0, 0, 0, 0.2f),
                    offsetX = 0f,
                    offsetY = 8f,
                    blurRadius = 12f,
                    spreadRadius = 0f
                ),
                level5 = Elevation(
                    level = 5,
                    shadowColor = Color(0, 0, 0, 0.2f),
                    offsetX = 0f,
                    offsetY = 16f,
                    blurRadius = 24f,
                    spreadRadius = 0f
                )
            ),

            // Motion (standard durations)
            motion = MotionTokens(
                durationFast = 100,
                durationNormal = 300,
                durationSlow = 500,
                durationPageTransition = 400,
                easingStandard = "cubic-bezier(0.4, 0.0, 0.2, 1)",
                easingDecelerate = "cubic-bezier(0.0, 0.0, 0.2, 1)",
                easingAccelerate = "cubic-bezier(0.4, 0.0, 1, 1)",
                easingSharp = "cubic-bezier(0.4, 0.0, 0.6, 1)"
            ),

            // Breakpoints (responsive)
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
     * ModernUI Dark Theme Tokens
     */
    private fun createDarkTokens(): DesignTokens {
        return DesignTokens(
            color = ColorTokens(
                // Primary (Purple - lighter for dark mode)
                primary = ColorScale(
                    shade50 = Color(0x4A, 0x14, 0x8C),   // Reversed
                    shade100 = Color(0x6A, 0x1B, 0x9A),
                    shade200 = Color(0x7B, 0x1F, 0xA2),
                    shade300 = Color(0x8E, 0x24, 0xAA),
                    shade400 = Color(0x9C, 0x27, 0xB0),
                    shade500 = Color(0xBA, 0x68, 0xC8),  // Main - lighter
                    shade600 = Color(0xCE, 0x93, 0xD8),
                    shade700 = Color(0xE1, 0xBE, 0xE7),
                    shade800 = Color(0xF3, 0xE5, 0xF5),
                    shade900 = Color(0xF3, 0xE5, 0xF5),
                    main = Color(0xD1, 0xC4, 0xE9),      // Lighter for dark
                    light = Color(0xE1, 0xBE, 0xE7),
                    dark = Color(0xBA, 0x68, 0xC8),
                    contrastText = Color(0x00, 0x00, 0x00) // Black text on light purple
                ),

                // Secondary (Teal - lighter)
                secondary = ColorScale(
                    shade50 = Color(0x00, 0x4D, 0x40),
                    shade100 = Color(0x00, 0x69, 0x5C),
                    shade200 = Color(0x00, 0x79, 0x6B),
                    shade300 = Color(0x00, 0x89, 0x7B),
                    shade400 = Color(0x00, 0x96, 0x88),
                    shade500 = Color(0x4D, 0xB6, 0xAC),  // Main - lighter
                    shade600 = Color(0x80, 0xCB, 0xC4),
                    shade700 = Color(0xB2, 0xDF, 0xDB),
                    shade800 = Color(0xE0, 0xF2, 0xF1),
                    shade900 = Color(0xE0, 0xF2, 0xF1),
                    main = Color(0x80, 0xCB, 0xC4),
                    light = Color(0xB2, 0xDF, 0xDB),
                    dark = Color(0x4D, 0xB6, 0xAC),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Tertiary (Blue - lighter)
                tertiary = ColorScale(
                    shade50 = Color(0x0D, 0x47, 0xA1),
                    shade100 = Color(0x15, 0x65, 0xC0),
                    shade200 = Color(0x19, 0x76, 0xD2),
                    shade300 = Color(0x1E, 0x88, 0xE5),
                    shade400 = Color(0x21, 0x96, 0xF3),
                    shade500 = Color(0x64, 0xB5, 0xF6),
                    shade600 = Color(0x90, 0xCA, 0xF9),
                    shade700 = Color(0xBB, 0xDE, 0xFB),
                    shade800 = Color(0xE3, 0xF2, 0xFD),
                    shade900 = Color(0xE3, 0xF2, 0xFD),
                    main = Color(0x90, 0xCA, 0xF9),
                    light = Color(0xBB, 0xDE, 0xFB),
                    dark = Color(0x64, 0xB5, 0xF6),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Error (Red - lighter)
                error = ColorScale(
                    shade50 = Color(0xB7, 0x1C, 0x1C),
                    shade100 = Color(0xC6, 0x28, 0x28),
                    shade200 = Color(0xD3, 0x2F, 0x2F),
                    shade300 = Color(0xE5, 0x39, 0x35),
                    shade400 = Color(0xF4, 0x43, 0x36),
                    shade500 = Color(0xEF, 0x53, 0x50),
                    shade600 = Color(0xE5, 0x73, 0x73),
                    shade700 = Color(0xEF, 0x9A, 0x9A),
                    shade800 = Color(0xFF, 0xCD, 0xD2),
                    shade900 = Color(0xFF, 0xEB, 0xEE),
                    main = Color(0xEF, 0x9A, 0x9A),
                    light = Color(0xFF, 0xCD, 0xD2),
                    dark = Color(0xE5, 0x73, 0x73),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Warning (Orange - lighter)
                warning = ColorScale(
                    shade50 = Color(0xE6, 0x51, 0x00),
                    shade100 = Color(0xEF, 0x6C, 0x00),
                    shade200 = Color(0xF5, 0x7C, 0x00),
                    shade300 = Color(0xFB, 0x8C, 0x00),
                    shade400 = Color(0xFF, 0x98, 0x00),
                    shade500 = Color(0xFF, 0xA7, 0x26),
                    shade600 = Color(0xFF, 0xB7, 0x4D),
                    shade700 = Color(0xFF, 0xCC, 0x80),
                    shade800 = Color(0xFF, 0xE0, 0xB2),
                    shade900 = Color(0xFF, 0xF3, 0xE0),
                    main = Color(0xFF, 0xB7, 0x4D),
                    light = Color(0xFF, 0xCC, 0x80),
                    dark = Color(0xFF, 0xA7, 0x26),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Info (Blue - lighter)
                info = ColorScale(
                    shade50 = Color(0x0D, 0x47, 0xA1),
                    shade100 = Color(0x15, 0x65, 0xC0),
                    shade200 = Color(0x19, 0x76, 0xD2),
                    shade300 = Color(0x1E, 0x88, 0xE5),
                    shade400 = Color(0x21, 0x96, 0xF3),
                    shade500 = Color(0x64, 0xB5, 0xF6),
                    shade600 = Color(0x90, 0xCA, 0xF9),
                    shade700 = Color(0xBB, 0xDE, 0xFB),
                    shade800 = Color(0xE3, 0xF2, 0xFD),
                    shade900 = Color(0xE3, 0xF2, 0xFD),
                    main = Color(0x90, 0xCA, 0xF9),
                    light = Color(0xBB, 0xDE, 0xFB),
                    dark = Color(0x64, 0xB5, 0xF6),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Success (Green - lighter)
                success = ColorScale(
                    shade50 = Color(0x1B, 0x5E, 0x20),
                    shade100 = Color(0x2E, 0x7D, 0x32),
                    shade200 = Color(0x38, 0x8E, 0x3C),
                    shade300 = Color(0x43, 0xA0, 0x47),
                    shade400 = Color(0x4C, 0xAF, 0x50),
                    shade500 = Color(0x66, 0xBB, 0x6A),
                    shade600 = Color(0x81, 0xC7, 0x84),
                    shade700 = Color(0xA5, 0xD6, 0xA7),
                    shade800 = Color(0xC8, 0xE6, 0xC9),
                    shade900 = Color(0xE8, 0xF5, 0xE9),
                    main = Color(0x81, 0xC7, 0x84),
                    light = Color(0xA5, 0xD6, 0xA7),
                    dark = Color(0x66, 0xBB, 0x6A),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Neutral (Gray - inverted)
                neutral = ColorScale(
                    shade50 = Color(0x21, 0x21, 0x21),
                    shade100 = Color(0x42, 0x42, 0x42),
                    shade200 = Color(0x61, 0x61, 0x61),
                    shade300 = Color(0x75, 0x75, 0x75),
                    shade400 = Color(0x9E, 0x9E, 0x9E),
                    shade500 = Color(0xBD, 0xBD, 0xBD),
                    shade600 = Color(0xE0, 0xE0, 0xE0),
                    shade700 = Color(0xEE, 0xEE, 0xEE),
                    shade800 = Color(0xF5, 0xF5, 0xF5),
                    shade900 = Color(0xFA, 0xFA, 0xFA),
                    main = Color(0xBD, 0xBD, 0xBD),
                    light = Color(0xE0, 0xE0, 0xE0),
                    dark = Color(0x75, 0x75, 0x75),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Surface colors (dark backgrounds)
                surface = SurfaceColors(
                    background = Color(0x12, 0x12, 0x12),      // Very dark gray
                    surface = Color(0x1E, 0x1E, 0x1E),          // Dark gray
                    surfaceVariant = Color(0x2D, 0x2D, 0x2D),  // Slightly lighter
                    surfaceTint = Color(0xD1, 0xC4, 0xE9),     // Light purple tint
                    inverseSurface = Color(0xE0, 0xE0, 0xE0)   // Light gray
                ),

                // Text colors (light on dark)
                text = TextColors(
                    primary = Color(0xFF, 0xFF, 0xFF, 0.87f),     // 87% white
                    secondary = Color(0xFF, 0xFF, 0xFF, 0.60f),   // 60% white
                    disabled = Color(0xFF, 0xFF, 0xFF, 0.38f),    // 38% white
                    hint = Color(0xFF, 0xFF, 0xFF, 0.38f),
                    inverse = Color(0x00, 0x00, 0x00, 0.87f)      // Black on light
                ),

                // Border colors
                border = BorderColors(
                    default = Color(0x42, 0x42, 0x42),           // Dark gray
                    subtle = Color(0x2D, 0x2D, 0x2D),
                    focus = Color(0xD1, 0xC4, 0xE9),             // Light purple
                    error = Color(0xEF, 0x9A, 0x9A)              // Light red
                )
            ),

            // Spacing (same as light)
            spacing = createLightTokens().spacing,

            // Typography (same as light)
            typography = createLightTokens().typography,

            // Border radius (same as light)
            radius = createLightTokens().radius,

            // Elevation (same as light)
            elevation = createLightTokens().elevation,

            // Motion (same as light)
            motion = createLightTokens().motion,

            // Breakpoints (same as light)
            breakpoints = createLightTokens().breakpoints,

            // Z-index (same as light)
            zIndex = createLightTokens().zIndex
        )
    }
}
