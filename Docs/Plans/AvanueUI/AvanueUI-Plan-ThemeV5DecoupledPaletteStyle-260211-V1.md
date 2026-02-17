# AvanueUI Theme System v5.0 — Decoupled Palette + Style Architecture

**Date:** 2026-02-11
**Module:** AvanueUI (Modules/AvanueUI/) + Avanues App
**Branch:** `VoiceOSCore-KotlinUpdate`
**Type:** Architecture Redesign
**Status:** IMPLEMENTED

## Summary

Decoupled the AvanueUI theme system into two independent axes:
- **Axis 1: Color Palette** (`AvanueColorPalette`) — Sol, Luna, Terra, Hydra
- **Axis 2: Material Style** (`MaterialMode`) — Glass, Water, Cupertino, MountainView

16 possible combinations (any palette x any style).
Default: Hydra + Water = "translucent sapphire"

## Changes Made

### New Files (13)
| File | Type |
|------|------|
| `theme/AvanueColorPalette.kt` | Palette enum with 4 entries |
| `theme/SolColors.kt` | Warm golden sun color scheme |
| `theme/LunaColors.kt` | Cool moonlit silver color scheme |
| `theme/TerraColors.kt` | Natural earth/forest color scheme |
| `theme/HydraColors.kt` | Royal translucent sapphire (DEFAULT) |
| `theme/SolGlass.kt` | Amber glow glass recipe |
| `theme/LunaGlass.kt` | Indigo glow glass recipe |
| `theme/TerraGlass.kt` | Forest green glow glass recipe |
| `theme/HydraGlass.kt` | Sapphire glow glass recipe |
| `theme/SolWater.kt` | Amber highlight water recipe |
| `theme/LunaWater.kt` | Silver highlight water recipe |
| `theme/TerraWater.kt` | Green highlight water recipe |
| `theme/HydraWater.kt` | Sapphire highlight water recipe |

### Modified Files (13)
| File | Change |
|------|--------|
| `theme/MaterialMode.kt` | Added CUPERTINO, renamed PLAIN→MOUNTAIN_VIEW, added displayName+fromString |
| `theme/AvanueTheme.kt` | Defaults changed from Ocean/GLASS to Hydra/WATER |
| `theme/AvanueThemeVariant.kt` | @Deprecated, delegates to AvanueColorPalette |
| `components/AvanueSurface.kt` | Added CUPERTINO + MOUNTAIN_VIEW branches |
| `components/AvanueCard.kt` | Added CUPERTINO + MOUNTAIN_VIEW branches |
| `components/AvanueButton.kt` | Added CUPERTINO + MOUNTAIN_VIEW branches |
| `components/AvanueChip.kt` | Added CUPERTINO + MOUNTAIN_VIEW branches |
| `components/AvanueBubble.kt` | Added CUPERTINO + MOUNTAIN_VIEW branches |
| `components/AvanueFAB.kt` | Added CUPERTINO + MOUNTAIN_VIEW branches |
| `components/AvanueIconButton.kt` | Added CUPERTINO + MOUNTAIN_VIEW branches |
| `AvanuesSettingsRepository.kt` | New palette+style keys, migration from old variant |
| `MainActivity.kt` | Uses AvanueColorPalette+MaterialMode instead of variant |
| `SystemSettingsProvider.kt` | Two independent dropdowns |

### Deprecated Files (9)
All marked with @Deprecated + ReplaceWith:
- OceanColors→LunaColors, SunsetColors→SolColors, LiquidColors→HydraColors
- OceanGlass→LunaGlass, SunsetGlass→SolGlass, LiquidGlass→HydraGlass
- OceanWater→LunaWater, SunsetWater→SolWater, LiquidWater→HydraWater

## Migration

DataStore migration is automatic:
- Old `theme_variant=OCEAN` → `palette=LUNA, style=GLASS`
- Old `theme_variant=SUNSET` → `palette=SOL, style=GLASS`
- Old `theme_variant=LIQUID` → `palette=HYDRA, style=WATER`
- Old `MaterialMode.PLAIN` → `MaterialMode.MOUNTAIN_VIEW`

## Cupertino Style Notes
- 0dp elevation (flat, no shadow)
- 12dp corner radius (iOS continuous corners)
- 0.33dp hairline border (borderSubtle color)
- No glass/water visual effects

## MountainView Style Notes
- Standard M3 elevation (1dp tonal for cards)
- Standard M3 shape scale
- No glass/water visual effects
