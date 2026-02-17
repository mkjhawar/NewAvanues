# AvanueUI Theme v5.1 — AppearanceMode (Light/Dark/Auto) 3rd Axis

**Date:** 2026-02-11
**Module:** AvanueUI (Modules/AvanueUI/) + Avanues App
**Branch:** `VoiceOSCore-KotlinUpdate`
**Status:** IMPLEMENTED

## Architecture: 3 Independent Axes

| Axis | Enum | Values | Default |
|------|------|--------|---------|
| Color Palette | `AvanueColorPalette` | SOL, LUNA, TERRA, HYDRA | HYDRA |
| Material Style | `MaterialMode` | Glass, Water, Cupertino, MountainView | Water |
| Appearance | `AppearanceMode` | Light, Dark, Auto | Auto |

4x4x2 = **32 visual configurations**. Auto follows `isSystemInDarkTheme()`.

## Approach: Branch B — Light/Dark in same file

Each palette file (Colors, Glass, Water) contains both Dark and Light objects.
No new files needed for variants — only 1 new file: `AppearanceMode.kt`.

## New Files (1)

| File | Purpose |
|------|---------|
| `theme/AppearanceMode.kt` | Light/Dark/Auto enum with fromString() |

## Modified Files (17)

| File | Change |
|------|--------|
| `theme/AvanueTheme.kt` | LocalAppearanceIsDark, isDark param, lightColorScheme bridge |
| `theme/AvanueColorPalette.kt` | `colors(isDark)`, `glass(isDark)`, `water(isDark)` |
| `theme/HydraColors.kt` | Added `HydraColorsLight` |
| `theme/SolColors.kt` | Added `SolColorsLight` |
| `theme/LunaColors.kt` | Added `LunaColorsLight` |
| `theme/TerraColors.kt` | Added `TerraColorsLight` |
| `theme/HydraGlass.kt` | Added `HydraGlassLight` |
| `theme/SolGlass.kt` | Added `SolGlassLight` |
| `theme/LunaGlass.kt` | Added `LunaGlassLight` |
| `theme/TerraGlass.kt` | Added `TerraGlassLight` |
| `theme/HydraWater.kt` | Added `HydraWaterLight` |
| `theme/SolWater.kt` | Added `SolWaterLight` |
| `theme/LunaWater.kt` | Added `LunaWaterLight` |
| `theme/TerraWater.kt` | Added `TerraWaterLight` |
| `AvanuesSettingsRepository.kt` | `theme_appearance` DataStore key |
| `MainActivity.kt` | Resolve isDark from AppearanceMode, pass to provider |
| `SystemSettingsProvider.kt` | Appearance dropdown (3rd) |

## Light Palette Design

All light palettes: same brand primary colors, inverted backgrounds/surfaces/text/borders.

| Palette | Background | Surface | Text | Vibe |
|---------|-----------|---------|------|------|
| Hydra Light | `#F8FAFC` Frost White | `#FFFFFF` | `#0F172A` → `#475569` | Clean sapphire |
| Sol Light | `#FFFBF0` Warm Cream | `#FFFFFF` | `#1C1917` → `#57534E` | Warm amber |
| Luna Light | `#F5F3FF` Lavender White | `#FFFFFF` | `#1E1B4B` → `#4B5563` | Cool indigo |
| Terra Light | `#F0FDF4` Mint White | `#FFFFFF` | `#14532D` → `#4B5563` | Fresh green |

## Key Technical Details

- `toM3ColorScheme(isDark)` switches between `darkColorScheme()` and `lightColorScheme()`
- `LocalAppearanceIsDark` CompositionLocal for components needing appearance-aware logic
- `AvanueTheme.isDark` accessor for easy @Composable access
- Glass: overlayColor inverts (White→Black), tintColor matches light surface, shadow softens
- Water: depthShadow lightens, surfaceTint matches light surface
- Backward compat: `palette.colors` (no arg) still returns dark variant

## DataStore Keys

- `theme_palette` — AvanueColorPalette name
- `theme_style` — MaterialMode name
- `theme_appearance` — AppearanceMode name (Light/Dark/Auto)

## Verification

- `./gradlew :Modules:AvanueUI:compileDebugKotlinAndroid` — PASS
- `./gradlew :apps:avanues:compileDebugKotlin` — PASS
- Settings shows 3 independent dropdowns
