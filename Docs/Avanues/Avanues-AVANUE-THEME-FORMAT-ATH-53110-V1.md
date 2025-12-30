# Avanue Theme Format (.ath) Specification

**Format Name**: AvanueTheme
**Extension**: `.ath`
**Version**: 1.0.0
**Date**: 2025-10-31 17:00 PDT
**Created by**: Manoj Jhawar, manoj@ideahq.net

---

## Overview

The **Avanue Theme Format (.ath)** is the standardized file format for exporting, sharing, and importing themes in the Avanues ecosystem. It provides a human-readable, JSON-based format that can be used across all platforms (Android, iOS, Web, Desktop).

## Purpose

- **Portability**: Share themes across platforms and apps
- **Customization**: Allow users to create and customize themes
- **AI Generation**: Enable AI-assisted theme generation
- **Version Control**: Track theme changes over time
- **Interchange**: Exchange themes with other design tools

---

## File Format

### Basic Structure

```json
{
  "ath_version": "1.0.0",
  "theme": {
    "name": "GlassAvanue Light",
    "version": "1.0.0",
    "platform": "custom",
    "author": "Manoj Jhawar",
    "created_at": "2025-10-31T17:00:00Z",

    "colors": { },
    "typography": { },
    "shapes": { },
    "spacing": { },
    "elevation": { },
    "material": { },
    "animation": { }
  }
}
```

### Complete Example: GlassAvanue.ath

```json
{
  "ath_version": "1.0.0",
  "theme": {
    "name": "GlassAvanue Light",
    "version": "1.0.0",
    "platform": "custom",
    "author": "Manoj Jhawar",
    "created_at": "2025-10-31T17:00:00Z",
    "description": "Signature glassmorphic theme for AR/MR and traditional displays",

    "colors": {
      "mode": "light",

      "primary": "#46CBFF",
      "onPrimary": "#FFFFFF",
      "primaryContainer": "#46CBFF4D",
      "onPrimaryContainer": "#000000DE",

      "secondary": "#7C4DFF",
      "onSecondary": "#FFFFFF",
      "secondaryContainer": "#7C4DFF4D",
      "onSecondaryContainer": "#000000DE",

      "tertiary": "#FF6E40",
      "onTertiary": "#FFFFFF",
      "tertiaryContainer": "#FF6E404D",
      "onTertiaryContainer": "#000000DE",

      "error": "#B00020",
      "onError": "#FFFFFF",
      "errorContainer": "#FFDAD6",
      "onErrorContainer": "#410002",

      "surface": "#FFFFFFBF",
      "onSurface": "#000000DE",
      "surfaceVariant": "#FFFFFFA6",
      "onSurfaceVariant": "#00000099",
      "surfaceTint": "#46CBFF1A",

      "background": "#00000000",
      "onBackground": "#000000DE",

      "outline": "#0000001F",
      "outlineVariant": "#0000000F",

      "scrim": "#00000052",
      "inverseSurface": "#000000D9",
      "inverseOnSurface": "#FFFFFFF2",
      "inversePrimary": "#46CBFFCC"
    },

    "typography": {
      "fontFamily": "SF Pro",
      "displayLarge": { "size": 57, "weight": 400, "lineHeight": 64 },
      "displayMedium": { "size": 45, "weight": 400, "lineHeight": 52 },
      "displaySmall": { "size": 36, "weight": 400, "lineHeight": 44 },

      "headlineLarge": { "size": 32, "weight": 500, "lineHeight": 40 },
      "headlineMedium": { "size": 28, "weight": 500, "lineHeight": 36 },
      "headlineSmall": { "size": 24, "weight": 500, "lineHeight": 32 },

      "titleLarge": { "size": 22, "weight": 500, "lineHeight": 28 },
      "titleMedium": { "size": 16, "weight": 500, "lineHeight": 24 },
      "titleSmall": { "size": 14, "weight": 500, "lineHeight": 20 },

      "bodyLarge": { "size": 16, "weight": 400, "lineHeight": 24 },
      "bodyMedium": { "size": 14, "weight": 400, "lineHeight": 20 },
      "bodySmall": { "size": 12, "weight": 400, "lineHeight": 16 },

      "labelLarge": { "size": 14, "weight": 500, "lineHeight": 20 },
      "labelMedium": { "size": 12, "weight": 500, "lineHeight": 16 },
      "labelSmall": { "size": 11, "weight": 500, "lineHeight": 16 }
    },

    "shapes": {
      "extraSmall": 4,
      "small": 12,
      "medium": 24,
      "large": 32,
      "extraLarge": 48
    },

    "spacing": {
      "xs": 4,
      "sm": 8,
      "md": 16,
      "lg": 24,
      "xl": 32,
      "xxl": 48
    },

    "elevation": {
      "level0": { "offsetY": 0, "blur": 0, "opacity": 0 },
      "level1": { "offsetY": 2, "blur": 8, "opacity": 0.08 },
      "level2": { "offsetY": 4, "blur": 12, "opacity": 0.12 },
      "level3": { "offsetY": 8, "blur": 20, "opacity": 0.16 },
      "level4": { "offsetY": 12, "blur": 28, "opacity": 0.20 },
      "level5": { "offsetY": 16, "blur": 36, "opacity": 0.24 }
    },

    "material": {
      "type": "glass",
      "blurRadius": 25,
      "opacity": 0.70,
      "tintColor": "#FFFFFFBF",
      "shadowStrength": 0.20,
      "reflectionStrength": 0.15,
      "refractionIndex": 1.05
    },

    "animation": {
      "defaultDuration": 250,
      "defaultEasing": "easeInOut",
      "fastDuration": 150,
      "slowDuration": 400,
      "enableMotion": true,
      "reduceMotion": false
    }
  }
}
```

---

## Conversion

### Kotlin to .ath

```kotlin
// Export GlassAvanue theme to .ath file
val theme = GlassAvanue.Light
val athJson = theme.toATH()  // Returns JSON string
File("GlassAvanue-Light.ath").writeText(athJson)
```

### .ath to Kotlin

```kotlin
// Import .ath file to AvaElements Theme
val athJson = File("GlassAvanue-Light.ath").readText()
val theme = Theme.fromATH(athJson)
```

---

## Usage in AvanueLaunch

The AvanueLaunch launcher app uses GlassAvanue theme loaded from:
- **Internal**: Bundled `assets/themes/GlassAvanue-Light.ath`
- **External**: User-downloaded themes from `Downloads/AvanueThemes/`

### Loading Themes

```kotlin
// Load from bundled assets
val theme = loadThemeFromAssets("themes/GlassAvanue-Light.ath")

// Load from external storage
val theme = loadThemeFromFile("/sdcard/Download/AvanueThemes/MyTheme.ath")

// Apply to app
AvaUI {
    theme = theme
    LauncherScreen()
}
```

---

## Platform Support

| Platform | Support | Notes |
|----------|---------|-------|
| Android | ✅ Full | Native .ath loader in AvaElements |
| iOS | ✅ Full | Swift ATH parser |
| Web | ✅ Full | JavaScript JSON parser |
| Desktop | ✅ Full | Kotlin/JVM parser |

---

## File Naming Convention

Format: `{ThemeName}-{Mode}.ath`

Examples:
- `GlassAvanue-Light.ath`
- `GlassAvanue-Dark.ath`
- `GlassAvanue-Auto.ath`
- `MaterialYou-Dynamic.ath`
- `CustomTheme-Gaming.ath`

---

## Version History

- **v1.0.0** (2025-10-31): Initial .ath format specification

---

**Created by**: Manoj Jhawar, manoj@ideahq.net
**Methodology**: IDEACODE 5.0
