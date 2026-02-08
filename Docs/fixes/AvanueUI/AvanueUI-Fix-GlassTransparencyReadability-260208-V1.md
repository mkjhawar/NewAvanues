# AvanueUI-Fix-GlassTransparencyReadability-260208-V1

## Problem
Text throughout the app (HomeScreen dashboard + Settings) was barely readable due to compound transparency:
1. Glass modifier opacity was 5/8/12% (LIGHT/MEDIUM/HEAVY) - cards nearly invisible
2. Material3 dark color scheme used `Color(0x1AFFFFFF)` (10% white glass) for `surface`, causing auto-derived `surfaceContainerLow` etc. to also be near-invisible
3. `SettingsGroupCard` used `surfaceContainerLow` which wasn't explicitly defined

## Root Cause
Two design systems with conflicting surface strategies:
- **OceanTheme** (Foundation): Solid colors (`surface = #1E293B`)
- **ColorTokens** (DesignTokens): Glass colors (`DarkSurface = 0x1AFFFFFF`)

Glass modifier then applied 5-8% alpha on solid OceanTheme.surface, making cards invisible.

## Fix (Hybrid Approach)
### 1. Moderate Glass Opacity (still ethereal, but visible)
**File:** `AvanueUI/Foundation/.../OceanThemeExtensions.kt`
- LIGHT: 5% -> 12%
- MEDIUM: 8% -> 18%
- HEAVY: 12% -> 25%

### 2. Solid Material3 Surfaces
**File:** `AvanueUI/.../AvanueTheme.kt`
- `surface` = `ColorTokens.OceanMid` (#1E293B solid, was 10% white glass)
- `surfaceVariant` = `ColorTokens.OceanShallow` (#334155 solid, was 15% white glass)
- Added explicit `surfaceContainerLowest` through `surfaceContainerHighest` using solid ocean blues

### 3. Consistency Updates
- `OceanDesignTokens.Glass` constants updated to match
- `OceanTheme.glassLight/Medium/Heavy` overlay colors updated
- `GlassLevel` enum docs updated

## Files Modified
| File | Change |
|------|--------|
| `AvanueUI/Foundation/.../OceanThemeExtensions.kt` | Glass opacity 5/8/12% -> 12/18/25% |
| `AvanueUI/.../AvanueTheme.kt` | Dark scheme: solid surfaces + container hierarchy |
| `AvanueUI/.../DesignTokens.kt` | Added `SurfaceHighest` token (#475569) |
| `AvanueUI/Foundation/.../OceanDesignTokens.kt` | Glass constants updated |
| `AvanueUI/Foundation/.../OceanTheme.kt` | Glass overlay colors updated |
| `AvanueUI/Foundation/.../GlassmorphicComponents.kt` | GlassLevel docs updated |

## Impact
- All GlassCard/GlassSurface instances across app now have visible card boundaries
- Material3 ListItem, Card, SettingsGroupCard have solid backgrounds for text contrast
- Glass aesthetic preserved (ethereal feel, not opaque)
