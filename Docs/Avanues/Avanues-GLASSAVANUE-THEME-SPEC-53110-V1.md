# GlassAvanue Theme Specification

**Platform**: Avanues Ecosystem
**Theme Name**: GlassAvanue
**Version**: 1.0.0
**Date**: 2025-10-31
**Created by**: Manoj Jhawar, manoj@ideahq.net

---

## Overview

**GlassAvanue** is the signature glassmorphic design system for the Avanues platform, designed for mixed reality, smart glasses, and mobile devices. It provides a unified, depth-aware aesthetic that looks native on both transparent AR displays and traditional screens.

### Design Philosophy

- **Transparent First**: Optimized for AR/MR devices (Vuzix, Vision Pro, Nreal)
- **Universal**: Works beautifully on LCD/OLED (Android, iOS, Desktop)
- **Depth Aware**: Utilizes layers, blur, and shadows for spatial hierarchy
- **AI-Assisted**: Context-aware theming based on environment and user preferences
- **Adaptive**: Dynamic color extraction from wallpapers and ambient lighting

---

## 🎨 Design System Specifications

### Visual Properties

| Property | Value | Description |
|----------|-------|-------------|
| **Background Opacity** | 65-75% (0.65-0.75) | Semi-transparent panels |
| **Blur Radius** | 20-30px (25px default) | Frosted glass effect |
| **Corner Radius** | 24px (panels), 12px (icons) | Rounded, soft edges |
| **Shadow** | Soft diffuse, no hard edges | Depth without harshness |
| **Base Tint** | `rgba(255,255,255,0.75)` | Neutral glass tint |
| **Accent Color** | Dynamic (wallpaper-based) | Adaptive primary color |
| **Font Primary** | SF Pro (iOS) / Roboto Medium (Android) | Clean, readable |
| **Animation** | 250ms ease-in-out | Smooth transitions |

### Color Modes

GlassAvanue supports three color modes:
1. **Light Mode**: Light glass with dark text (default)
2. **Dark Mode**: Dark glass with light text
3. **Auto Mode**: Adapts to system/ambient lighting

---

## 🏗️ AvaUI Theme Implementation

### Theme Definition (Kotlin)

```kotlin
package com.augmentalis.avaelements.themes

import com.augmentalis.avaelements.core.*

/**
 * GlassAvanue - Signature glassmorphic theme for Avanues platform
 *
 * Optimized for:
 * - AR/MR transparent displays
 * - Traditional LCD/OLED screens
 * - Adaptive color from environment
 */
object GlassAvanue {

    /**
     * Light mode glass theme (default)
     */
    val Light = Theme(
        name = "GlassAvanue Light",
        platform = ThemePlatform.Custom,

        colorScheme = ColorScheme(
            mode = ColorScheme.ColorMode.Light,

            // Primary colors (adaptive accent)
            primary = Color(0x46, 0xCB, 0xFF),  // #46CBFF Aurora Blue
            onPrimary = Color(255, 255, 255),
            primaryContainer = Color(0x46, 0xCB, 0xFF, 0.3f),  // 30% opacity
            onPrimaryContainer = Color(0, 0, 0, 0.87f),

            // Secondary colors
            secondary = Color(0x7C, 0x4D, 0xFF),  // #7C4DFF Purple
            onSecondary = Color(255, 255, 255),
            secondaryContainer = Color(0x7C, 0x4D, 0xFF, 0.3f),
            onSecondaryContainer = Color(0, 0, 0, 0.87f),

            // Tertiary colors
            tertiary = Color(0xFF, 0x6E, 0x40),  // #FF6E40 Coral
            onTertiary = Color(255, 255, 255),
            tertiaryContainer = Color(0xFF, 0x6E, 0x40, 0.3f),
            onTertiaryContainer = Color(0, 0, 0, 0.87f),

            // Error colors
            error = Color(0xB0, 0x00, 0x20),
            onError = Color(255, 255, 255),
            errorContainer = Color(0xFF, 0xDA, 0xD6),
            onErrorContainer = Color(0x41, 0x00, 0x02),

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
            fontFamily = "SF Pro",  // iOS default, use Roboto on Android

            // Display styles
            displayLarge = FontStyle(size = 57f, weight = FontWeight.Regular, lineHeight = 64f),
            displayMedium = FontStyle(size = 45f, weight = FontWeight.Regular, lineHeight = 52f),
            displaySmall = FontStyle(size = 36f, weight = FontWeight.Regular, lineHeight = 44f),

            // Headline styles
            headlineLarge = FontStyle(size = 32f, weight = FontWeight.Medium, lineHeight = 40f),
            headlineMedium = FontStyle(size = 28f, weight = FontWeight.Medium, lineHeight = 36f),
            headlineSmall = FontStyle(size = 24f, weight = FontWeight.Medium, lineHeight = 32f),

            // Title styles
            titleLarge = FontStyle(size = 22f, weight = FontWeight.Medium, lineHeight = 28f),
            titleMedium = FontStyle(size = 16f, weight = FontWeight.Medium, lineHeight = 24f),
            titleSmall = FontStyle(size = 14f, weight = FontWeight.Medium, lineHeight = 20f),

            // Body styles
            bodyLarge = FontStyle(size = 16f, weight = FontWeight.Regular, lineHeight = 24f),
            bodyMedium = FontStyle(size = 14f, weight = FontWeight.Regular, lineHeight = 20f),
            bodySmall = FontStyle(size = 12f, weight = FontWeight.Regular, lineHeight = 16f),

            // Label styles
            labelLarge = FontStyle(size = 14f, weight = FontWeight.Medium, lineHeight = 20f),
            labelMedium = FontStyle(size = 12f, weight = FontWeight.Medium, lineHeight = 16f),
            labelSmall = FontStyle(size = 11f, weight = FontWeight.Medium, lineHeight = 16f)
        ),

        shapes = Shapes(
            extraSmall = 4f,   // Tiny elements
            small = 12f,       // Icons, chips
            medium = 24f,      // Cards, panels (PRIMARY)
            large = 32f,       // Large cards
            extraLarge = 48f   // Modal dialogs
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
            level0 = Elevation(dp = 0f, blur = 0f, spread = 0f),
            level1 = Elevation(dp = 2f, blur = 8f, spread = 0f, opacity = 0.08f),
            level2 = Elevation(dp = 4f, blur = 12f, spread = 0f, opacity = 0.12f),
            level3 = Elevation(dp = 8f, blur = 20f, spread = 0f, opacity = 0.16f),
            level4 = Elevation(dp = 12f, blur = 28f, spread = 0f, opacity = 0.20f),
            level5 = Elevation(dp = 16f, blur = 36f, spread = 0f, opacity = 0.24f)
        ),

        material = MaterialSystem(
            type = MaterialType.Glass,
            blurRadius = 25f,        // 20-30px range
            opacity = 0.70f,         // 65-75% range (70% default)
            tintColor = Color(255, 255, 255, 0.75f),
            shadowStrength = 0.20f,  // Soft shadows
            reflectionStrength = 0.15f,
            refractionIndex = 1.05f  // Subtle glass refraction
        ),

        animation = AnimationConfig(
            defaultDuration = 250,    // 250ms
            defaultEasing = Easing.EaseInOut,

            // Specific animation timings
            fastDuration = 150,
            slowDuration = 400,

            // Scale animations for glass pop
            scaleIn = AnimationSpec(duration = 250, easing = Easing.EaseOut, from = 0.95f, to = 1.0f),
            scaleOut = AnimationSpec(duration = 200, easing = Easing.EaseIn, from = 1.0f, to = 0.95f),

            // Fade animations
            fadeIn = AnimationSpec(duration = 250, easing = Easing.EaseInOut, from = 0.0f, to = 1.0f),
            fadeOut = AnimationSpec(duration = 200, easing = Easing.EaseInOut, from = 1.0f, to = 0.0f)
        )
    )

    /**
     * Dark mode glass theme
     */
    val Dark = Light.copy(
        name = "GlassAvanue Dark",
        colorScheme = Light.colorScheme.copy(
            mode = ColorScheme.ColorMode.Dark,

            // Inverted surface colors for dark mode
            surface = Color(0, 0, 0, 0.75f),  // 75% black glass
            onSurface = Color(255, 255, 255, 0.95f),
            surfaceVariant = Color(0, 0, 0, 0.65f),
            onSurfaceVariant = Color(255, 255, 255, 0.70f),
            surfaceTint = Color(0x46, 0xCB, 0xFF, 0.15f),

            background = Color(0, 0, 0, 0.0f),  // Still transparent
            onBackground = Color(255, 255, 255, 0.95f),

            // Adjusted outlines for dark mode
            outline = Color(255, 255, 255, 0.12f),
            outlineVariant = Color(255, 255, 255, 0.06f),

            // Inverted special colors
            inverseSurface = Color(255, 255, 255, 0.85f),
            inverseOnSurface = Color(0, 0, 0, 0.87f)
        ),

        material = Light.material?.copy(
            tintColor = Color(0, 0, 0, 0.75f)  // Dark glass tint
        )
    )

    /**
     * Auto mode - adapts to system settings
     */
    val Auto = Light.copy(
        name = "GlassAvanue Auto",
        colorScheme = Light.colorScheme.copy(
            mode = ColorScheme.ColorMode.Auto
        )
    )

    /**
     * Create custom GlassAvanue theme with specific accent color
     * (e.g., from wallpaper extraction)
     */
    fun withAccent(accentColor: Color, mode: ColorScheme.ColorMode = ColorScheme.ColorMode.Light): Theme {
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
                surfaceTint = accentColor.copy(alpha = 0.1f)
            )
        )
    }
}
```

---

## 🎨 Usage Examples

### Basic Usage

```kotlin
// Use GlassAvanue theme in your UI
val launcherUI = AvaUI {
    theme = GlassAvanue.Light

    Column {
        SearchBar("Search or ask AVA…")
        AppGrid()
        VoiceOrb()
        QuickPanel()
    }
}
```

### Dynamic Accent from Wallpaper

```kotlin
// Extract dominant color from wallpaper
val wallpaperColor = extractDominantColor(wallpaperBitmap)

// Create custom GlassAvanue theme with that accent
val customTheme = GlassAvanue.withAccent(wallpaperColor)

val launcherUI = AvaUI {
    theme = customTheme
    // All components automatically use wallpaper-based accent
}
```

### Dark Mode Toggle

```kotlin
// Toggle between light and dark
var isDarkMode by remember { mutableStateOf(false) }

val currentTheme = if (isDarkMode) GlassAvanue.Dark else GlassAvanue.Light

AvaUI {
    theme = currentTheme
    // Entire UI updates automatically
}
```

### Adaptive Mode (System-Based)

```kotlin
// Use Auto mode - follows system dark mode setting
AvaUI {
    theme = GlassAvanue.Auto
    // Automatically switches light/dark based on system
}
```

---

## 🏗️ Launcher Components Mapping

| Launcher Component | AvaUI Component | GlassAvanue Styling |
|-------------------|-------------------|---------------------|
| **Home Panel** | `Column` + `Grid` | 24px corners, 70% glass |
| **Search Bar** | `SearchBar` | Top floating, blur 25px |
| **App Icons** | `Avatar` in `Grid` | 12px corners, scale animation |
| **Voice Orb** | `Avatar` + animation | Pulsing, primary accent |
| **Quick Panel** | `BottomNav` | Bottom glass bar, blur |
| **Side Nav** | `Drawer` | Edge swipe, glass panel |
| **Widgets** | `Card` | Semi-transparent, live data |
| **Notifications** | `Toast` + `Alert` | Slide-in glass notifications |
| **App Drawer** | `ScrollView` (horizontal) | 3D carousel effect |
| **Multitask View** | `Card` grid | Preview windows, glass |

---

## 📱 Platform-Specific Adaptations

### Android
```kotlin
// Android uses Roboto instead of SF Pro
GlassAvanue.Light.copy(
    typography = GlassAvanue.Light.typography.copy(
        fontFamily = "Roboto Medium"
    )
)
```

### iOS
```kotlin
// iOS uses SF Pro (default)
GlassAvanue.Light  // No changes needed
```

### AR/MR Devices (Vision Pro, Vuzix, Nreal)
```kotlin
// Increase transparency for see-through displays
GlassAvanue.Light.copy(
    material = GlassAvanue.Light.material?.copy(
        opacity = 0.60f,  // More transparent
        blurRadius = 30f   // More blur for depth
    )
)
```

### Desktop (Electron)
```kotlin
// Desktop can use more elaborate effects
GlassAvanue.Light.copy(
    material = GlassAvanue.Light.material?.copy(
        reflectionStrength = 0.25f,  // More reflection
        blurRadius = 28f
    )
)
```

---

## 🎨 Theme JSON Export

For cross-platform tools and AI agents, GlassAvanue can be exported as JSON:

```json
{
  "theme_name": "GlassAvanue Light",
  "version": "1.0.0",
  "platform": "custom",

  "visual": {
    "base_opacity": 0.70,
    "blur_radius": 25,
    "corner_radius": 24,
    "shadow_strength": 0.20,
    "panel_tint": "rgba(255,255,255,0.75)"
  },

  "colors": {
    "primary": "#46CBFF",
    "secondary": "#7C4DFF",
    "tertiary": "#FF6E40",
    "surface": "rgba(255,255,255,0.75)",
    "background": "rgba(0,0,0,0.0)",
    "error": "#B00020"
  },

  "typography": {
    "font_family": "SF Pro",
    "display_large": { "size": 57, "weight": 400, "line_height": 64 },
    "title_large": { "size": 22, "weight": 500, "line_height": 28 },
    "body_medium": { "size": 14, "weight": 400, "line_height": 20 }
  },

  "animation": {
    "default_duration": 250,
    "default_easing": "ease-in-out",
    "scale_in": { "from": 0.95, "to": 1.0, "duration": 250 },
    "fade_in": { "from": 0.0, "to": 1.0, "duration": 250 }
  },

  "spacing": {
    "xs": 4, "sm": 8, "md": 16, "lg": 24, "xl": 32, "xxl": 48
  },

  "elevation": {
    "level1": { "dp": 2, "blur": 8, "opacity": 0.08 },
    "level2": { "dp": 4, "blur": 12, "opacity": 0.12 },
    "level3": { "dp": 8, "blur": 20, "opacity": 0.16 }
  }
}
```

---

## 🧠 AI Theming Instructions

For AI agents creating new GlassAvanue variants:

### 1. **Wallpaper-Based Theme Generation**
```kotlin
fun generateFromWallpaper(wallpaperBitmap: Bitmap): Theme {
    // 1. Extract dominant color
    val dominantColor = extractDominantColor(wallpaperBitmap)

    // 2. Generate complementary palette
    val palette = generateColorPalette(dominantColor)

    // 3. Create theme with extracted colors
    return GlassAvanue.withAccent(
        accentColor = palette.primary,
        mode = if (palette.isDark) ColorScheme.ColorMode.Dark else ColorScheme.ColorMode.Light
    )
}
```

### 2. **Ambient Light Adaptation**
```kotlin
fun adaptToAmbientLight(lightLevel: Float): Theme {
    // lightLevel: 0.0 (dark) to 1.0 (bright)

    val opacity = 0.65f + (lightLevel * 0.10f)  // 65-75% range
    val blurRadius = 20f + (lightLevel * 10f)    // 20-30px range

    return GlassAvanue.Light.copy(
        material = GlassAvanue.Light.material?.copy(
            opacity = opacity,
            blurRadius = blurRadius
        )
    )
}
```

### 3. **Context-Aware Theming**
```kotlin
fun themeForContext(context: AppContext): Theme {
    return when (context) {
        AppContext.Gaming -> GlassAvanue.Dark.copy(
            colorScheme = GlassAvanue.Dark.colorScheme.copy(
                primary = Color(0xFF, 0x00, 0x5C)  // Neon purple for gaming
            )
        )

        AppContext.Reading -> GlassAvanue.Light.copy(
            material = GlassAvanue.Light.material?.copy(
                opacity = 0.85f  // More opaque for reading
            )
        )

        AppContext.AR -> GlassAvanue.Light.copy(
            material = GlassAvanue.Light.material?.copy(
                opacity = 0.55f  // More transparent for AR
            )
        )

        else -> GlassAvanue.Auto
    }
}
```

---

## 📊 Design Tokens Reference

### Quick Reference Table

| Token | Light | Dark | Description |
|-------|-------|------|-------------|
| `surface.opacity` | 0.75 | 0.75 | Panel transparency |
| `blur.radius` | 25px | 25px | Glass blur amount |
| `corner.radius` | 24px | 24px | Panel roundness |
| `shadow.strength` | 0.20 | 0.20 | Shadow opacity |
| `primary` | #46CBFF | #46CBFF | Accent color |
| `animation.duration` | 250ms | 250ms | Transition speed |
| `font.family` | SF Pro | SF Pro | Typography |

---

## 🚀 Implementation Roadmap

### Phase 1: Core Theme (COMPLETE)
- ✅ Theme definition structure
- ✅ Light/Dark/Auto modes
- ✅ Color system
- ✅ Typography scale
- ✅ Glass material system

### Phase 2: Platform Integration (IN PROGRESS)
- ⏳ Android Compose implementation
- ⏳ iOS SwiftUI implementation
- ⏳ Web React implementation
- ⏳ Desktop Electron implementation

### Phase 3: Advanced Features (PLANNED)
- ⏳ Wallpaper color extraction
- ⏳ Ambient light adaptation
- ⏳ Context-aware theming
- ⏳ AI-based theme generation
- ⏳ Theme customization UI

---

## 📝 Notes

- **Performance**: Glass effects use hardware acceleration on supported devices
- **Fallback**: On devices without blur support, uses solid colors with reduced opacity
- **Accessibility**: Maintains WCAG AA contrast ratios in all modes
- **Battery**: Adaptive blur reduces on low battery (optional)

---

## 🔗 Related Documentation

- AvaUI Core Theme System: `Universal/Libraries/AvaElements/Core/src/commonMain/kotlin/com/augmentalis/avaelements/core/Theme.kt`
- Android Renderer: `Universal/Libraries/AvaElements/Renderers/Android/`
- iOS Renderer: `Universal/Libraries/AvaElements/Renderers/iOS/`
- Theme Builder: `Universal/Libraries/AvaElements/ThemeBuilder/`

---

**Version History**:
- v1.0.0 (2025-10-31): Initial GlassAvanue specification

**Created by**: Manoj Jhawar, manoj@ideahq.net
**Platform**: Avanues Ecosystem
**Methodology**: IDEACODE 5.0
