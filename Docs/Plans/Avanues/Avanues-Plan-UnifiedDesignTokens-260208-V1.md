# Unified AvanueUI Design Token System

**Module:** Avanues (AvanueUI/DesignSystem)
**Date:** 2026-02-08
**Branch:** 060226-1-consolidation-framework
**Status:** Phase 1-2 Complete, Phase 3 Partial

## Problem

Three token files with conflicting values:
- **DesignSystem/DesignTokens.kt** (generic M3, 0 consumers)
- **AvanueUI/src/DesignTokens.kt** (Ocean-themed, 3 consumers)
- **Foundation/OceanDesignTokens.kt** (semantic shortcuts, 14+ consumers)

Critical conflict: `OceanDesignTokens.Spacing.md = 12dp` vs `SpacingTokens.Medium = 16dp`
Decision: **md = 16dp** (industry standard M3/HIG/Fluent)

## Architecture

### Static tokens (no theme dependency)
Direct access, no `@Composable` needed:
```
com.augmentalis.avanueui.tokens.SpacingTokens.md  // 16.dp
com.augmentalis.avanueui.tokens.ShapeTokens.lg     // 16.dp
com.augmentalis.avanueui.tokens.SizeTokens.iconMd  // 24.dp
```

### Themed values (via CompositionLocal)
```kotlin
@Composable
fun MyScreen() {
    val colors = AvanueTheme.colors  // current theme
    Text(color = colors.textPrimary, ...)
}
```

### Two CompositionLocals only
- `LocalAvanueColors` -> AvanueColorScheme (colors)
- `LocalAvanueGlass` -> AvanueGlassScheme (glass effect recipe)

## Files Created (Phase 1) - 18 files

### Static tokens (8)
| File | Package | Purpose |
|------|---------|---------|
| `tokens/SpacingTokens.kt` | `com.augmentalis.avanueui.tokens` | Unified md=16dp |
| `tokens/TypographyTokens.kt` | same | M3 type scale |
| `tokens/ShapeTokens.kt` | same | Corner radii |
| `tokens/SizeTokens.kt` | same | Component dims + spatial |
| `tokens/ElevationTokens.kt` | same | Shadow levels |
| `tokens/AnimationTokens.kt` | same | Durations |
| `tokens/ResponsiveTokens.kt` | same | Breakpoints/grid |
| `tokens/GlassTokens.kt` | same | Opacity, blur, gradient |

### Theme (7)
| File | Package | Purpose |
|------|---------|---------|
| `theme/AvanueColorScheme.kt` | `com.augmentalis.avanueui.theme` | Color interface |
| `theme/AvanueGlassScheme.kt` | same | Glass recipe interface |
| `theme/OceanColors.kt` | same | Ocean implementation |
| `theme/OceanGlass.kt` | same | Ocean glass recipe |
| `theme/SunsetColors.kt` | same | Sunset implementation |
| `theme/SunsetGlass.kt` | same | Sunset glass recipe |
| `theme/AvanueTheme.kt` | same | CompositionLocals + facade |

### Glass types (2)
| File | Package | Purpose |
|------|---------|---------|
| `glass/GlassLevel.kt` | `com.augmentalis.avanueui.glass` | LIGHT/MEDIUM/HEAVY |
| `glass/GlassBorder.kt` | same | Border data class |

### AVUDSL support (1)
| File | Package | Purpose |
|------|---------|---------|
| `tokens/TokenResolver.kt` | `com.augmentalis.avanueui.tokens` | String ID resolver |

## Deprecations (Phase 2)
- `Foundation/OceanDesignTokens.kt` - @Deprecated
- `Foundation/OceanTheme.kt` - @Deprecated
- `Foundation/GlassmorphicComponents.kt` - GlassLevel/GlassBorder now typealiases to DesignSystem
- `WebAvanue/OceanDesignTokens.kt` - @Deprecated (local copy)

## Migrations Completed (Phase 3 - Partial)
| File | From | To |
|------|------|-----|
| `HomeScreen.kt` | OceanDesignTokens, OceanTheme | SpacingTokens, AvanueTheme.colors |
| `HubDashboardScreen.kt` | OceanDesignTokens, OceanTheme | SpacingTokens, AvanueTheme.colors |
| `DeveloperConsoleScreen.kt` | OceanDesignTokens | AvanueTheme.colors |
| `GlassesSettingsLayout.kt` | ColorTokens.Primary | OceanColors.primary |
| `apps/avanues/build.gradle.kts` | - | Added DesignSystem dependency |

## Remaining Migration (Phase 3 - Future)
- WebAvanue: 20+ files using local OceanDesignTokens/OceanTheme
- AI/Chat: 2 files using com.avanueui.ColorTokens/ShapeTokens/SizeTokens
- AvidCreator: 2 files using Foundation imports
- AvanueUI/Adapters: 1 file

## Migration Mapping
| Old | New |
|-----|-----|
| `OceanDesignTokens.Spacing.md` (12dp) | `SpacingTokens.md` (16dp) |
| `OceanDesignTokens.Spacing.lg` (16dp) | `SpacingTokens.md` (16dp) |
| `OceanDesignTokens.Text.primary` | `AvanueTheme.colors.textPrimary` |
| `OceanDesignTokens.State.success` | `AvanueTheme.colors.success` |
| `OceanTheme.background` | `AvanueTheme.colors.background` |
| `ColorTokens.Primary` (com.avanueui) | `AvanueTheme.colors.primary` or `OceanColors.primary` |

## Verification Checklist
- [ ] `./gradlew :Modules:AvanueUI:DesignSystem:build` compiles
- [ ] `./gradlew :Modules:AvanueUI:Foundation:build` compiles
- [ ] `./gradlew :apps:avanues:assembleDebug` builds
- [ ] Visual regression check on device
- [ ] `grep -r "OceanDesignTokens" --include="*.kt" apps/` returns 0
