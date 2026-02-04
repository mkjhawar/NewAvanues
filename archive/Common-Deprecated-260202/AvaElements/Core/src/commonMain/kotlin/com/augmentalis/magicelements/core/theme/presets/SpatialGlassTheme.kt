package com.augmentalis.avaelements.core.theme.presets

import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.theme.*
import com.augmentalis.avaelements.core.tokens.*

/**
 * Spatial Glass Theme
 *
 * Augmentalis' spatial computing theme for XR/AR/MR devices.
 * Designed for see-through displays like AR glasses and XR headsets.
 *
 * Features:
 * - Transparent glass materials for see-through displays
 * - Depth and layering for spatial computing
 * - High contrast for legibility in varied environments
 * - Optimized for pass-through AR/MR
 * - Spatial audio-reactive design tokens
 * - Adaptive opacity based on environment
 *
 * Platform mappings:
 * - XR: Native spatial materials (visionOS, Meta Quest, HoloLens)
 * - iOS/iPadOS: SwiftUI with RealityKit integration
 * - Android: ARCore-compatible glass materials
 * - Web: WebXR with transparent backgrounds
 */
object SpatialGlassTheme {
    /**
     * XR theme (single mode for spatial computing)
     *
     * Note: Spatial computing doesn't have traditional light/dark modes.
     * Instead, it adapts opacity based on environment lighting.
     */
    val XR: UniversalTheme = createXRTheme()

    /**
     * Create XR Spatial Glass theme
     */
    private fun createXRTheme(): UniversalTheme {
        return UniversalTheme(
            id = "spatialglass-xr",
            name = "Spatial Glass XR",
            mode = ThemeMode.XR,
            visualStyle = VisualStyle.VISIONOS2_SPATIAL_GLASS,
            tokens = createXRTokens(),
            platformOverrides = PlatformOverrides(
                xr = XrOverrides(
                    glassOpacity = 0.25f,           // Very transparent for see-through
                    depthSeparation = 0.08f,         // 8cm depth between layers
                    spatialAudio = true,
                    depthBlur = true,
                    materialType = XrMaterialType.GLASS
                )
            )
        )
    }

    /**
     * XR Spatial Glass Tokens
     */
    private fun createXRTokens(): DesignTokens {
        return DesignTokens(
            color = ColorTokens(
                // Primary (Vibrant Blue for AR visibility)
                primary = ColorScale(
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
                    light = Color(0x42, 0xA5, 0xF5),
                    dark = Color(0x00, 0x66, 0xCC),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Secondary (Vibrant Purple)
                secondary = ColorScale(
                    shade50 = Color(0xF3, 0xE5, 0xF5),
                    shade100 = Color(0xE1, 0xBE, 0xE7),
                    shade200 = Color(0xCE, 0x93, 0xD8),
                    shade300 = Color(0xBA, 0x68, 0xC8),
                    shade400 = Color(0xAB, 0x47, 0xBC),
                    shade500 = Color(0xBB, 0x5A, 0xFF),  // Vibrant main
                    shade600 = Color(0xAA, 0x50, 0xE5),
                    shade700 = Color(0x99, 0x46, 0xCC),
                    shade800 = Color(0x88, 0x3C, 0xB2),
                    shade900 = Color(0x77, 0x32, 0x99),
                    main = Color(0xBB, 0x5A, 0xFF),
                    light = Color(0xCE, 0x93, 0xD8),
                    dark = Color(0x99, 0x46, 0xCC),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Tertiary (Vibrant Cyan)
                tertiary = ColorScale(
                    shade50 = Color(0xE0, 0xF7, 0xFA),
                    shade100 = Color(0xB2, 0xEB, 0xF2),
                    shade200 = Color(0x80, 0xDE, 0xEA),
                    shade300 = Color(0x4D, 0xD0, 0xE1),
                    shade400 = Color(0x26, 0xC6, 0xDA),
                    shade500 = Color(0x00, 0xE5, 0xFF),  // Vibrant main
                    shade600 = Color(0x00, 0xCC, 0xE5),
                    shade700 = Color(0x00, 0xB3, 0xCC),
                    shade800 = Color(0x00, 0x99, 0xB2),
                    shade900 = Color(0x00, 0x80, 0x99),
                    main = Color(0x00, 0xE5, 0xFF),
                    light = Color(0x4D, 0xD0, 0xE1),
                    dark = Color(0x00, 0xB3, 0xCC),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Error (Vibrant Red)
                error = ColorScale(
                    shade50 = Color(0xFF, 0xEB, 0xEE),
                    shade100 = Color(0xFF, 0xCD, 0xD2),
                    shade200 = Color(0xEF, 0x9A, 0x9A),
                    shade300 = Color(0xE5, 0x73, 0x73),
                    shade400 = Color(0xEF, 0x53, 0x50),
                    shade500 = Color(0xFF, 0x44, 0x44),  // Vibrant main
                    shade600 = Color(0xE5, 0x3D, 0x3D),
                    shade700 = Color(0xCC, 0x36, 0x36),
                    shade800 = Color(0xB2, 0x2F, 0x2F),
                    shade900 = Color(0x99, 0x28, 0x28),
                    main = Color(0xFF, 0x44, 0x44),
                    light = Color(0xEF, 0x53, 0x50),
                    dark = Color(0xCC, 0x36, 0x36),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Warning (Vibrant Orange)
                warning = ColorScale(
                    shade50 = Color(0xFF, 0xF3, 0xE0),
                    shade100 = Color(0xFF, 0xE0, 0xB2),
                    shade200 = Color(0xFF, 0xCC, 0x80),
                    shade300 = Color(0xFF, 0xB7, 0x4D),
                    shade400 = Color(0xFF, 0xA7, 0x26),
                    shade500 = Color(0xFF, 0xAA, 0x00),  // Vibrant main
                    shade600 = Color(0xE5, 0x99, 0x00),
                    shade700 = Color(0xCC, 0x88, 0x00),
                    shade800 = Color(0xB2, 0x77, 0x00),
                    shade900 = Color(0x99, 0x66, 0x00),
                    main = Color(0xFF, 0xAA, 0x00),
                    light = Color(0xFF, 0xB7, 0x4D),
                    dark = Color(0xCC, 0x88, 0x00),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Info (same as primary)
                info = ColorScale(
                    shade50 = Color(0xE3, 0xF2, 0xFD),
                    shade100 = Color(0xBB, 0xDE, 0xFB),
                    shade200 = Color(0x90, 0xCA, 0xF9),
                    shade300 = Color(0x64, 0xB5, 0xF6),
                    shade400 = Color(0x42, 0xA5, 0xF5),
                    shade500 = Color(0x00, 0x88, 0xFF),
                    shade600 = Color(0x00, 0x77, 0xE5),
                    shade700 = Color(0x00, 0x66, 0xCC),
                    shade800 = Color(0x00, 0x55, 0xB2),
                    shade900 = Color(0x00, 0x44, 0x99),
                    main = Color(0x00, 0x88, 0xFF),
                    light = Color(0x42, 0xA5, 0xF5),
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
                    shade500 = Color(0x00, 0xDD, 0x55),  // Vibrant main
                    shade600 = Color(0x00, 0xC7, 0x4D),
                    shade700 = Color(0x00, 0xB0, 0x44),
                    shade800 = Color(0x00, 0x99, 0x3C),
                    shade900 = Color(0x00, 0x82, 0x33),
                    main = Color(0x00, 0xDD, 0x55),
                    light = Color(0x66, 0xBB, 0x6A),
                    dark = Color(0x00, 0xB0, 0x44),
                    contrastText = Color(0x00, 0x00, 0x00)
                ),

                // Neutral (High contrast grays)
                neutral = ColorScale(
                    shade50 = Color(0xFA, 0xFA, 0xFA),
                    shade100 = Color(0xF5, 0xF5, 0xF5),
                    shade200 = Color(0xE8, 0xE8, 0xE8),
                    shade300 = Color(0xD0, 0xD0, 0xD0),
                    shade400 = Color(0xB0, 0xB0, 0xB0),
                    shade500 = Color(0x90, 0x90, 0x90),
                    shade600 = Color(0x60, 0x60, 0x60),
                    shade700 = Color(0x40, 0x40, 0x40),
                    shade800 = Color(0x30, 0x30, 0x30),
                    shade900 = Color(0x18, 0x18, 0x18),
                    main = Color(0x90, 0x90, 0x90),
                    light = Color(0xD0, 0xD0, 0xD0),
                    dark = Color(0x40, 0x40, 0x40),
                    contrastText = Color(0xFF, 0xFF, 0xFF)
                ),

                // Surface colors (very transparent for see-through)
                surface = SurfaceColors(
                    background = Color(0xFF, 0xFF, 0xFF, 0.0f),     // Fully transparent
                    surface = Color(0xFF, 0xFF, 0xFF, 0.15f),        // 15% white glass
                    surfaceVariant = Color(0xF5, 0xF5, 0xF5, 0.25f), // 25% variant
                    surfaceTint = Color(0x00, 0x88, 0xFF, 0.05f),   // Very subtle tint
                    inverseSurface = Color(0x18, 0x18, 0x18, 0.9f)  // Opaque inverse
                ),

                // Text colors (high contrast for AR)
                text = TextColors(
                    primary = Color(0xFF, 0xFF, 0xFF, 1.0f),       // Fully opaque white
                    secondary = Color(0xFF, 0xFF, 0xFF, 0.75f),   // 75% white
                    disabled = Color(0xFF, 0xFF, 0xFF, 0.45f),     // 45% white
                    hint = Color(0xFF, 0xFF, 0xFF, 0.45f),
                    inverse = Color(0x00, 0x00, 0x00, 1.0f)       // Opaque black
                ),

                // Border colors (high contrast)
                border = BorderColors(
                    default = Color(0xFF, 0xFF, 0xFF, 0.3f),      // 30% white
                    subtle = Color(0xFF, 0xFF, 0xFF, 0.15f),
                    focus = Color(0x00, 0x88, 0xFF, 0.8f),        // Vibrant blue
                    error = Color(0xFF, 0x44, 0x44, 0.8f)         // Vibrant red
                )
            ),

            // Spacing (larger for XR comfort - 12dp base)
            spacing = SpacingTokens(
                unit = 12f,      // Larger for XR
                none = 0f,
                xs = 6f,
                sm = 12f,
                md = 24f,
                lg = 36f,
                xl = 48f,
                xxl = 72f,
                xxxl = 96f,
                paddingSmall = 12f,
                paddingMedium = 24f,
                paddingLarge = 36f,
                gapSmall = 12f,
                gapMedium = 24f,
                gapLarge = 36f
            ),

            // Typography (larger for XR legibility)
            typography = TypographyTokens(
                displayLarge = TextStyle(
                    fontSize = 68f,    // Larger for XR
                    lineHeight = 76f,
                    fontWeight = FontWeight.BOLD,
                    letterSpacing = -0.5f,
                    fontFamily = "system-ui"
                ),
                displayMedium = TextStyle(
                    fontSize = 54f,
                    lineHeight = 62f,
                    fontWeight = FontWeight.BOLD,
                    letterSpacing = -0.3f,
                    fontFamily = "system-ui"
                ),
                displaySmall = TextStyle(
                    fontSize = 43f,
                    lineHeight = 51f,
                    fontWeight = FontWeight.BOLD,
                    letterSpacing = -0.2f,
                    fontFamily = "system-ui"
                ),
                headlineLarge = TextStyle(
                    fontSize = 38f,
                    lineHeight = 46f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = -0.2f,
                    fontFamily = "system-ui"
                ),
                headlineMedium = TextStyle(
                    fontSize = 33f,
                    lineHeight = 41f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = -0.2f,
                    fontFamily = "system-ui"
                ),
                headlineSmall = TextStyle(
                    fontSize = 29f,
                    lineHeight = 37f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = -0.1f,
                    fontFamily = "system-ui"
                ),
                titleLarge = TextStyle(
                    fontSize = 26f,
                    lineHeight = 33f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = -0.1f,
                    fontFamily = "system-ui"
                ),
                titleMedium = TextStyle(
                    fontSize = 20f,
                    lineHeight = 26f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = -0.4f,
                    fontFamily = "system-ui"
                ),
                titleSmall = TextStyle(
                    fontSize = 18f,
                    lineHeight = 24f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = -0.2f,
                    fontFamily = "system-ui"
                ),
                bodyLarge = TextStyle(
                    fontSize = 20f,
                    lineHeight = 26f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = -0.4f,
                    fontFamily = "system-ui"
                ),
                bodyMedium = TextStyle(
                    fontSize = 18f,
                    lineHeight = 24f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = -0.2f,
                    fontFamily = "system-ui"
                ),
                bodySmall = TextStyle(
                    fontSize = 15f,
                    lineHeight = 21f,
                    fontWeight = FontWeight.NORMAL,
                    letterSpacing = -0.1f,
                    fontFamily = "system-ui"
                ),
                labelLarge = TextStyle(
                    fontSize = 18f,
                    lineHeight = 24f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = -0.2f,
                    fontFamily = "system-ui"
                ),
                labelMedium = TextStyle(
                    fontSize = 15f,
                    lineHeight = 21f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = -0.1f,
                    fontFamily = "system-ui"
                ),
                labelSmall = TextStyle(
                    fontSize = 13f,
                    lineHeight = 19f,
                    fontWeight = FontWeight.SEMI_BOLD,
                    letterSpacing = 0.1f,
                    fontFamily = "system-ui"
                )
            ),

            // Border radius (larger for XR)
            radius = RadiusTokens(
                none = 0f,
                xs = 6f,
                sm = 12f,
                md = 18f,
                lg = 24f,
                xl = 32f,
                xxl = 44f,
                full = 9999f,
                button = 18f,
                card = 24f,
                dialog = 32f,
                input = 18f
            ),

            // Elevation (depth-based for XR)
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
                    offsetY = 4f,    // Larger shadows for depth
                    blurRadius = 12f,
                    spreadRadius = 0f
                ),
                level2 = Elevation(
                    level = 2,
                    shadowColor = Color(0, 0, 0, 0.25f),
                    offsetX = 0f,
                    offsetY = 8f,
                    blurRadius = 16f,
                    spreadRadius = 0f
                ),
                level3 = Elevation(
                    level = 3,
                    shadowColor = Color(0, 0, 0, 0.3f),
                    offsetX = 0f,
                    offsetY = 12f,
                    blurRadius = 24f,
                    spreadRadius = 0f
                ),
                level4 = Elevation(
                    level = 4,
                    shadowColor = Color(0, 0, 0, 0.35f),
                    offsetX = 0f,
                    offsetY = 16f,
                    blurRadius = 32f,
                    spreadRadius = 0f
                ),
                level5 = Elevation(
                    level = 5,
                    shadowColor = Color(0, 0, 0, 0.4f),
                    offsetX = 0f,
                    offsetY = 24f,
                    blurRadius = 48f,
                    spreadRadius = 0f
                )
            ),

            // Motion (slower for XR comfort)
            motion = MotionTokens(
                durationFast = 200,      // Slower for XR
                durationNormal = 400,
                durationSlow = 700,
                durationPageTransition = 600,
                easingStandard = "cubic-bezier(0.25, 0.1, 0.25, 1)",
                easingDecelerate = "cubic-bezier(0.0, 0.0, 0.2, 1)",
                easingAccelerate = "cubic-bezier(0.4, 0.0, 1, 1)",
                easingSharp = "cubic-bezier(0.4, 0.0, 0.6, 1)"
            ),

            // Breakpoints (XR device sizes)
            breakpoints = BreakpointTokens(
                xs = 0,       // AR glasses (small FOV)
                sm = 800,     // Standard XR headset
                md = 1200,    // Wide FOV headset
                lg = 1920,    // Ultra-wide FOV
                xl = 3840     // Room-scale XR
            ),

            // Z-index (depth layers for XR)
            zIndex = ZIndexTokens(
                base = 0,
                dropdown = 100,    // Closer depth
                sticky = 200,
                modal = 300,
                popover = 400,
                toast = 500,
                tooltip = 600
            )
        )
    }
}
