package com.augmentalis.avamagic.components.themes

import com.augmentalis.avamagic.components.core.*

/**
 * GlassAvanue - Signature glassmorphic theme for Avanues platform
 *
 * Design Philosophy:
 * - Transparent First: Optimized for AR/MR devices (Vuzix, Vision Pro, Nreal)
 * - Universal: Works beautifully on LCD/OLED (Android, iOS, Desktop)
 * - Depth Aware: Utilizes layers, blur, and shadows for spatial hierarchy
 * - AI-Assisted: Context-aware theming based on environment and user preferences
 * - Adaptive: Dynamic color extraction from wallpapers and ambient lighting
 *
 * Visual Properties:
 * - Background Opacity: 65-75% (0.65-0.75)
 * - Blur Radius: 20-30px (25px default)
 * - Corner Radius: 24px (panels), 12px (icons)
 * - Shadow: Soft diffuse, no hard edges
 * - Base Tint: rgba(255,255,255,0.75)
 * - Accent Color: Dynamic (wallpaper-based)
 * - Font Primary: SF Pro (iOS) / Roboto Medium (Android)
 * - Animation: 250ms ease-in-out
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 * Version: 1.0.0
 * Date: 2025-10-31
 */
object GlassAvanue {

    /**
     * Light mode glass theme (default)
     *
     * Optimized for bright environments and traditional displays.
     * Uses light glass panels (75% white) with dark text for readability.
     */
    val Light = Theme(
        name = "GlassAvanue Light",
        platform = ThemePlatform.Custom,

        colorScheme = ColorScheme(
            mode = ColorScheme.ColorMode.Light,

            // Primary colors (Aurora Blue - #46CBFF)
            primary = Color.hex("#46CBFF"),
            onPrimary = Color.White,
            primaryContainer = Color(0x46, 0xCB, 0xFF, 0.3f),  // 30% opacity
            onPrimaryContainer = Color(0, 0, 0, 0.87f),

            // Secondary colors (Purple - #7C4DFF)
            secondary = Color.hex("#7C4DFF"),
            onSecondary = Color.White,
            secondaryContainer = Color(0x7C, 0x4D, 0xFF, 0.3f),
            onSecondaryContainer = Color(0, 0, 0, 0.87f),

            // Tertiary colors (Coral - #FF6E40)
            tertiary = Color.hex("#FF6E40"),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFF, 0x6E, 0x40, 0.3f),
            onTertiaryContainer = Color(0, 0, 0, 0.87f),

            // Error colors
            error = Color.hex("#B00020"),
            onError = Color.White,
            errorContainer = Color.hex("#FFDAD6"),
            onErrorContainer = Color.hex("#410002"),

            // Surface colors (frosted glass)
            surface = Color(255, 255, 255, 0.75f),  // 75% white glass
            onSurface = Color(0, 0, 0, 0.87f),
            surfaceVariant = Color(255, 255, 255, 0.65f),  // 65% for variety
            onSurfaceVariant = Color(0, 0, 0, 0.60f),
            surfaceTint = Color(0x46, 0xCB, 0xFF, 0.1f),

            // Background (transparent for AR)
            background = Color(0, 0, 0, 0.0f),  // Fully transparent
            onBackground = Color(0, 0, 0, 0.87f),

            // Outline colors
            outline = Color(0, 0, 0, 0.12f),  // Subtle borders
            outlineVariant = Color(0, 0, 0, 0.06f),

            // Special colors
            scrim = Color(0, 0, 0, 0.32f),
            inverseSurface = Color(0, 0, 0, 0.85f),
            inverseOnSurface = Color(255, 255, 255, 0.95f),
            inversePrimary = Color(0x46, 0xCB, 0xFF, 0.8f)
        ),

        typography = Typography(
            // Display styles
            displayLarge = Font(size = 57f, weight = Font.Weight.Regular, lineHeight = 64f),
            displayMedium = Font(size = 45f, weight = Font.Weight.Regular, lineHeight = 52f),
            displaySmall = Font(size = 36f, weight = Font.Weight.Regular, lineHeight = 44f),

            // Headline styles
            headlineLarge = Font(size = 32f, weight = Font.Weight.Medium, lineHeight = 40f),
            headlineMedium = Font(size = 28f, weight = Font.Weight.Medium, lineHeight = 36f),
            headlineSmall = Font(size = 24f, weight = Font.Weight.Medium, lineHeight = 32f),

            // Title styles
            titleLarge = Font(size = 22f, weight = Font.Weight.Medium, lineHeight = 28f),
            titleMedium = Font(size = 16f, weight = Font.Weight.Medium, lineHeight = 24f),
            titleSmall = Font(size = 14f, weight = Font.Weight.Medium, lineHeight = 20f),

            // Body styles
            bodyLarge = Font(size = 16f, weight = Font.Weight.Regular, lineHeight = 24f),
            bodyMedium = Font(size = 14f, weight = Font.Weight.Regular, lineHeight = 20f),
            bodySmall = Font(size = 12f, weight = Font.Weight.Regular, lineHeight = 16f),

            // Label styles
            labelLarge = Font(size = 14f, weight = Font.Weight.Medium, lineHeight = 20f),
            labelMedium = Font(size = 12f, weight = Font.Weight.Medium, lineHeight = 16f),
            labelSmall = Font(size = 11f, weight = Font.Weight.Medium, lineHeight = 16f)
        ),

        shapes = Shapes(
            extraSmall = CornerRadius.all(4f),   // Tiny elements
            small = CornerRadius.all(12f),       // Icons, chips
            medium = CornerRadius.all(24f),      // Cards, panels (PRIMARY)
            large = CornerRadius.all(32f),       // Large cards
            extraLarge = CornerRadius.all(48f)   // Modal dialogs
        ),

        spacing = SpacingScale(
            xs = 4f,
            sm = 8f,
            md = 16f,
            lg = 24f,
            xl = 32f,
            xxl = 48f
        ),

        elevation = ElevationScale(
            // All soft diffuse shadows (no hard edges)
            level0 = Shadow(offsetX = 0f, offsetY = 0f, blurRadius = 0f, spreadRadius = 0f, color = Color.Transparent),
            level1 = Shadow(offsetX = 0f, offsetY = 2f, blurRadius = 8f, spreadRadius = 0f, color = Color(0, 0, 0, 0.08f)),
            level2 = Shadow(offsetX = 0f, offsetY = 4f, blurRadius = 12f, spreadRadius = 0f, color = Color(0, 0, 0, 0.12f)),
            level3 = Shadow(offsetX = 0f, offsetY = 8f, blurRadius = 20f, spreadRadius = 0f, color = Color(0, 0, 0, 0.16f)),
            level4 = Shadow(offsetX = 0f, offsetY = 12f, blurRadius = 28f, spreadRadius = 0f, color = Color(0, 0, 0, 0.20f)),
            level5 = Shadow(offsetX = 0f, offsetY = 16f, blurRadius = 36f, spreadRadius = 0f, color = Color(0, 0, 0, 0.24f))
        ),

        material = MaterialSystem(
            glassMaterial = MaterialSystem.GlassMaterial(
                blurRadius = 25f,        // 20-30px range
                tintColor = Color(255, 255, 255, 0.75f),
                thickness = 2f,
                brightness = 1.0f
            )
        ),

        animation = AnimationConfig(
            defaultDuration = 250,    // 250ms
            defaultEasing = Animation.Easing.EaseInOut,
            enableMotion = true,
            reduceMotion = false
        )
    )

    /**
     * Dark mode glass theme
     *
     * Optimized for low-light environments and OLED displays.
     * Uses dark glass panels (75% black) with light text for readability.
     */
    val Dark = Theme(
        name = "GlassAvanue Dark",
        platform = ThemePlatform.Custom,

        colorScheme = ColorScheme(
            mode = ColorScheme.ColorMode.Dark,

            // Primary colors (Aurora Blue - #46CBFF)
            primary = Color.hex("#46CBFF"),
            onPrimary = Color.Black,
            primaryContainer = Color(0x46, 0xCB, 0xFF, 0.3f),
            onPrimaryContainer = Color(255, 255, 255, 0.95f),

            // Secondary colors (Purple - #7C4DFF)
            secondary = Color.hex("#7C4DFF"),
            onSecondary = Color.Black,
            secondaryContainer = Color(0x7C, 0x4D, 0xFF, 0.3f),
            onSecondaryContainer = Color(255, 255, 255, 0.95f),

            // Tertiary colors (Coral - #FF6E40)
            tertiary = Color.hex("#FF6E40"),
            onTertiary = Color.Black,
            tertiaryContainer = Color(0xFF, 0x6E, 0x40, 0.3f),
            onTertiaryContainer = Color(255, 255, 255, 0.95f),

            // Error colors
            error = Color.hex("#FF5449"),
            onError = Color.Black,
            errorContainer = Color.hex("#93000A"),
            onErrorContainer = Color.hex("#FFDAD6"),

            // Surface colors (dark frosted glass)
            surface = Color(0, 0, 0, 0.75f),  // 75% black glass
            onSurface = Color(255, 255, 255, 0.95f),
            surfaceVariant = Color(0, 0, 0, 0.65f),
            onSurfaceVariant = Color(255, 255, 255, 0.70f),
            surfaceTint = Color(0x46, 0xCB, 0xFF, 0.15f),

            // Background (transparent for AR)
            background = Color(0, 0, 0, 0.0f),  // Still transparent
            onBackground = Color(255, 255, 255, 0.95f),

            // Outline colors (light for dark mode)
            outline = Color(255, 255, 255, 0.12f),
            outlineVariant = Color(255, 255, 255, 0.06f),

            // Special colors
            scrim = Color(0, 0, 0, 0.50f),
            inverseSurface = Color(255, 255, 255, 0.85f),
            inverseOnSurface = Color(0, 0, 0, 0.87f),
            inversePrimary = Color(0x46, 0xCB, 0xFF)
        ),

        typography = Light.typography,  // Same typography
        shapes = Light.shapes,           // Same shapes
        spacing = Light.spacing,         // Same spacing
        elevation = Light.elevation,     // Same elevation

        material = MaterialSystem(
            glassMaterial = MaterialSystem.GlassMaterial(
                blurRadius = 25f,
                tintColor = Color(0, 0, 0, 0.75f),  // Dark glass tint
                thickness = 2f,
                brightness = 0.8f  // Slightly dimmer for dark mode
            )
        ),

        animation = Light.animation  // Same animation config
    )

    /**
     * Auto mode - adapts to system settings
     *
     * Automatically switches between Light and Dark based on system preferences.
     * Uses ColorMode.Auto which is handled by the platform renderer.
     */
    val Auto = Theme(
        name = "GlassAvanue Auto",
        platform = ThemePlatform.Custom,

        colorScheme = Light.colorScheme.copy(
            mode = ColorScheme.ColorMode.Auto
        ),

        typography = Light.typography,
        shapes = Light.shapes,
        spacing = Light.spacing,
        elevation = Light.elevation,
        material = Light.material,
        animation = Light.animation
    )

    /**
     * Create custom GlassAvanue theme with specific accent color
     *
     * Use this to create dynamic themes based on wallpaper extraction,
     * ambient lighting, or user preferences.
     *
     * @param accentColor The primary accent color to use
     * @param mode The color mode (Light, Dark, or Auto)
     * @return A new GlassAvanue theme with the custom accent
     *
     * Example:
     * ```
     * // Extract dominant color from wallpaper
     * val wallpaperColor = extractDominantColor(wallpaperBitmap)
     * val customTheme = GlassAvanue.withAccent(wallpaperColor)
     * ```
     */
    fun withAccent(
        accentColor: Color,
        mode: ColorScheme.ColorMode = ColorScheme.ColorMode.Light
    ): Theme {
        val baseTheme = when (mode) {
            ColorScheme.ColorMode.Light -> Light
            ColorScheme.ColorMode.Dark -> Dark
            ColorScheme.ColorMode.Auto -> Auto
        }

        return baseTheme.copy(
            name = "GlassAvanue ${mode.name} (Custom Accent)",
            colorScheme = baseTheme.colorScheme.copy(
                primary = accentColor,
                primaryContainer = accentColor.copy(alpha = 0.3f),
                surfaceTint = accentColor.copy(alpha = if (mode == ColorScheme.ColorMode.Dark) 0.15f else 0.1f)
            )
        )
    }

    /**
     * Adapt theme to ambient light level
     *
     * Adjusts glass opacity and blur radius based on ambient lighting conditions.
     *
     * @param lightLevel Light level from 0.0 (dark) to 1.0 (bright)
     * @param baseTheme The base theme to adapt (defaults to Light)
     * @return Theme adapted for the ambient light level
     *
     * Example:
     * ```
     * // Get ambient light from sensor (0.0-1.0)
     * val ambientLight = lightSensor.currentLevel
     * val adaptedTheme = GlassAvanue.adaptToAmbientLight(ambientLight)
     * ```
     */
    fun adaptToAmbientLight(
        lightLevel: Float,
        baseTheme: Theme = Light
    ): Theme {
        // Map light level to opacity: 65-75% range
        val opacity = 0.65f + (lightLevel * 0.10f)

        // Map light level to blur: 20-30px range
        val blurRadius = 20f + (lightLevel * 10f)

        return baseTheme.copy(
            name = "${baseTheme.name} (Adapted)",
            material = baseTheme.material?.copy(
                glassMaterial = baseTheme.material?.glassMaterial?.copy(
                    blurRadius = blurRadius.coerceIn(20f, 30f),
                    tintColor = baseTheme.material?.glassMaterial?.tintColor?.copy(
                        alpha = opacity.coerceIn(0.65f, 0.75f)
                    ) ?: Color.White.copy(alpha = opacity.coerceIn(0.65f, 0.75f))
                )
            )
        )
    }

    /**
     * Context-aware theming for different app scenarios
     *
     * Adjusts theme properties based on the current app context.
     *
     * @param context The current app context
     * @return Theme optimized for the given context
     */
    fun forContext(context: AppContext): Theme {
        return when (context) {
            AppContext.Gaming -> Dark.copy(
                name = "GlassAvanue Gaming",
                colorScheme = Dark.colorScheme.copy(
                    primary = Color.hex("#FF005C")  // Neon purple/pink for gaming
                )
            )

            AppContext.Reading -> Light.copy(
                name = "GlassAvanue Reading",
                material = Light.material?.copy(
                    glassMaterial = Light.material?.glassMaterial?.copy(
                        blurRadius = 20f,  // Less blur for clarity
                        tintColor = Color.White.copy(alpha = 0.85f)  // More opaque
                    )
                )
            )

            AppContext.AR -> Light.copy(
                name = "GlassAvanue AR",
                material = Light.material?.copy(
                    glassMaterial = Light.material?.glassMaterial?.copy(
                        blurRadius = 30f,  // More blur for depth
                        tintColor = Color.White.copy(alpha = 0.55f)  // More transparent
                    )
                )
            )

            AppContext.Focus -> Dark.copy(
                name = "GlassAvanue Focus",
                colorScheme = Dark.colorScheme.copy(
                    primary = Color.hex("#00BFA5")  // Teal for focus mode
                ),
                material = Dark.material?.copy(
                    glassMaterial = Dark.material?.glassMaterial?.copy(
                        brightness = 0.6f  // Dimmer for less distraction
                    )
                )
            )

            AppContext.Social -> Light.copy(
                name = "GlassAvanue Social",
                colorScheme = Light.colorScheme.copy(
                    primary = Color.hex("#E91E63")  // Pink for social
                )
            )

            else -> Auto
        }
    }
}

/**
 * App context types for context-aware theming
 */
enum class AppContext {
    Gaming,
    Reading,
    AR,
    Focus,
    Social,
    Default
}

/**
 * Extension property to easily convert Theme to GlassAvanue theme
 */
val Theme.asGlassAvanue: Theme
    get() = if (this.platform == ThemePlatform.Custom && this.name.startsWith("GlassAvanue")) {
        this
    } else {
        GlassAvanue.Light
    }

/**
 * Check if a theme is a GlassAvanue theme
 */
val Theme.isGlassAvanue: Boolean
    get() = this.platform == ThemePlatform.Custom && this.name.startsWith("GlassAvanue")
