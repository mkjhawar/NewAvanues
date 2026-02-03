package com.augmentalis.avaelements.core.theme

import com.augmentalis.avaelements.core.tokens.*

/**
 * Universal Theme System
 *
 * A SINGLE theme definition that works across:
 * - Android (Material3 / Compose)
 * - iOS (SwiftUI / UIKit)
 * - macOS (AppKit / SwiftUI)
 * - Windows (WinUI / UWP)
 * - Web (CSS / React)
 * - XR (visionOS / Meta Quest / Spatial)
 *
 * Each platform renderer maps tokens to native equivalents.
 */
data class UniversalTheme(
    /** Theme identifier */
    val id: String,

    /** Display name */
    val name: String,

    /** Theme mode */
    val mode: ThemeMode,

    /** Visual style */
    val visualStyle: VisualStyle,

    /** Design tokens (the heart of the theme) */
    val tokens: DesignTokens,

    /** Platform-specific overrides */
    val platformOverrides: PlatformOverrides = PlatformOverrides()
) {
    /**
     * Get appropriate theme for current mode
     */
    companion object {
        /**
         * Create theme pair (light + dark)
         */
        fun createPair(
            id: String,
            name: String,
            visualStyle: VisualStyle,
            lightTokens: DesignTokens,
            darkTokens: DesignTokens,
            xrTokens: DesignTokens? = null
        ): ThemePair {
            return ThemePair(
                light = UniversalTheme(
                    id = "$id-light",
                    name = "$name Light",
                    mode = ThemeMode.LIGHT,
                    visualStyle = visualStyle,
                    tokens = lightTokens
                ),
                dark = UniversalTheme(
                    id = "$id-dark",
                    name = "$name Dark",
                    mode = ThemeMode.DARK,
                    visualStyle = visualStyle,
                    tokens = darkTokens
                ),
                xr = xrTokens?.let {
                    UniversalTheme(
                        id = "$id-xr",
                        name = "$name XR",
                        mode = ThemeMode.XR,
                        visualStyle = visualStyle,
                        tokens = it
                    )
                }
            )
        }
    }
}

/**
 * Theme mode
 */
enum class ThemeMode {
    /** Light mode (default) */
    LIGHT,

    /** Dark mode */
    DARK,

    /** XR/Spatial mode (transparent, see-through) */
    XR,

    /** Auto (follows system preference) */
    AUTO
}

/**
 * Visual style determines the look and feel
 */
enum class VisualStyle {
    /**
     * Modern Material Design
     * - Contemporary material aesthetic
     * - Rounded corners (8dp)
     * - Elevation-based shadows
     * - Dynamic color system
     * - Maps to: ModernUITheme
     */
    MATERIAL3,

    /**
     * Liquid Glass
     * - Subtle glassmorphism effects
     * - Soft shadows with blur
     * - System-optimized appearance
     * - More rounded corners (12dp)
     * - Maps to: LiquidGlassTheme
     */
    IOS26_LIQUID_GLASS,

    /**
     * Spatial Glass (XR/AR/MR)
     * - Transparent glass materials for see-through displays
     * - Depth and layering for spatial computing
     * - High contrast for AR visibility
     * - Spatial audio-reactive
     * - Maps to: SpatialGlassTheme
     */
    VISIONOS2_SPATIAL_GLASS,

    /**
     * Frost Glass (Standard Glassmorphism)
     * - Frosted glass effect with blur backgrounds
     * - High transparency and vivid borders
     * - Backdrop blur support
     * - Layered depth
     * - Maps to: FrostGlassTheme
     */
    GLASSMORPHISM,

    /**
     * Fluent Design
     * - Acrylic materials
     * - Reveal highlights
     * - Light/depth effects
     * - Future: FluentTheme
     */
    FLUENT,

    /**
     * Custom - user-defined style
     */
    CUSTOM
}

/**
 * Theme pair (light + dark + optional XR)
 */
data class ThemePair(
    val light: UniversalTheme,
    val dark: UniversalTheme,
    val xr: UniversalTheme? = null
) {
    /**
     * Get theme for mode
     */
    fun getTheme(mode: ThemeMode, systemIsDark: Boolean = false): UniversalTheme {
        return when (mode) {
            ThemeMode.LIGHT -> light
            ThemeMode.DARK -> dark
            ThemeMode.XR -> xr ?: light
            ThemeMode.AUTO -> if (systemIsDark) dark else light
        }
    }
}

/**
 * Platform-specific overrides
 *
 * Sometimes a platform needs special handling.
 * These overrides apply on top of base tokens.
 */
data class PlatformOverrides(
    val android: AndroidOverrides? = null,
    val ios: IosOverrides? = null,
    val web: WebOverrides? = null,
    val xr: XrOverrides? = null
)

/**
 * Android-specific overrides
 */
data class AndroidOverrides(
    /** Use Material You dynamic colors */
    val useDynamicColors: Boolean = false,

    /** Edge-to-edge mode */
    val edgeToEdge: Boolean = true,

    /** Status bar style */
    val statusBarStyle: StatusBarStyle = StatusBarStyle.AUTO
)

enum class StatusBarStyle {
    LIGHT, DARK, AUTO
}

/**
 * iOS-specific overrides
 */
data class IosOverrides(
    /** Use SF Pro font */
    val useSFPro: Boolean = true,

    /** UIBlurEffect style */
    val blurStyle: BlurStyle = BlurStyle.SYSTEMCHROME,

    /** Vibrancy */
    val useVibrancy: Boolean = false
)

enum class BlurStyle {
    SYSTEMCHROME,
    SYSTEMTHICK,
    SYSTEMTHIN,
    SYSTEMMATERIAL,
    NONE
}

/**
 * Web-specific overrides
 */
data class WebOverrides(
    /** Generate CSS custom properties */
    val generateCssVariables: Boolean = true,

    /** Use CSS backdrop-filter for glassmorphism */
    val useBackdropFilter: Boolean = true,

    /** Prefer system fonts */
    val preferSystemFonts: Boolean = true
)

/**
 * XR/Spatial-specific overrides
 */
data class XrOverrides(
    /** Glass opacity (0.0 = transparent, 1.0 = opaque) */
    val glassOpacity: Float = 0.3f,

    /** Depth separation (meters in XR space) */
    val depthSeparation: Float = 0.05f,

    /** Enable spatial audio feedback */
    val spatialAudio: Boolean = true,

    /** Enable depth-based blur */
    val depthBlur: Boolean = true,

    /** Material type */
    val materialType: XrMaterialType = XrMaterialType.GLASS
)

enum class XrMaterialType {
    /** Transparent glass (see-through) */
    GLASS,

    /** Frosted glass (blurred) */
    FROSTED_GLASS,

    /** Acrylic (Windows-style) */
    ACRYLIC,

    /** Solid (opaque) */
    SOLID,

    /** Holographic */
    HOLOGRAPHIC
}
