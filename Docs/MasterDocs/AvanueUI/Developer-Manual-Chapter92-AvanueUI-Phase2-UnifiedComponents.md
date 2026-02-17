# Developer Manual — Chapter 92: AvanueUI Phase 2 — Unified Component Architecture

**Date:** 2026-02-11 | **Branch:** `VoiceOSCore-KotlinUpdate` | **Version:** 2.0 (Theme v5.1)
**Prerequisite:** Chapter 91 (AvanueUI DesignSystem Guide)

---

## Overview

Phase 2 introduces **Unified Components** — ONE component per concept where the **theme controls rendering** (glass, water, or plain Material3). Developers no longer choose between `GlassCard` vs `WaterCard`. Instead, they write `AvanueCard` and the active `MaterialMode` handles the rest.

### Problem (Before Phase 2)
```kotlin
// Developer must explicitly choose effect family:
GlassCard(glassLevel = GlassLevel.MEDIUM) { ... }   // Glass effect
WaterCard(waterLevel = WaterLevel.REGULAR) { ... }   // Water effect
```

### Solution (After Phase 2)
```kotlin
// ONE component — theme decides rendering:
AvanueCard { ... }
// Glass if MaterialMode.Glass, Water if MaterialMode.Water,
// Cupertino if MaterialMode.Cupertino, M3 if MaterialMode.MountainView
```

---

## Changelog

### v4.0 (2026-02-10) — Phase 2: Unified Component Architecture

**New:**
- `MaterialMode` enum: `Glass`, `Water`, `Cupertino`, `MountainView` (4 modes)
- `AvanueTheme.materialMode` accessor
- 7 unified components: `AvanueSurface`, `AvanueCard`, `AvanueButton`, `AvanueChip`, `AvanueBubble`, `AvanueFAB`, `AvanueIconButton`

### v5.1 (2026-02-11) — Three Independent Axes

**New:**
- `AppearanceMode` enum: `Light`, `Dark`, `Auto`
- `AvanueColorPalette` enum: `SOL`, `LUNA`, `TERRA`, `HYDRA` (decoupled from MaterialMode)
- `AvanueTheme.isDark` accessor + `LocalAppearanceIsDark` CompositionLocal
- Light variants for all 4 palettes (Colors, Glass, Water)
- XR variants for AR smart glasses (`palette.colorsXR`)
- `Cupertino` material mode: 0dp elevation, 12dp corners, hairline borders
- `MountainView` material mode: Standard M3 tonal elevation

**Deprecated (v5.0):**
- `AvanueThemeVariant` → use `AvanueColorPalette` + `MaterialMode` independently
- `MaterialMode.PLAIN` → renamed to `MaterialMode.MountainView`
- `OceanColors`/`SunsetColors`/`LiquidColors` → use `LunaColors`/`SolColors`/`HydraColors`

**Deprecated (v4.0):**
- `GlassSurface` → use `AvanueSurface`
- `GlassCard` → use `AvanueCard`
- `GlassBubble` → use `AvanueBubble`
- `OceanButton` → use `AvanueButton`
- `GlassChip` → use `AvanueChip`
- `GlassFloatingActionButton` → use `AvanueFAB`
- `GlassIconButton` → use `AvanueIconButton`
- `GlassIndicator` → use `AvanueSurface`
- `WaterSurface` → use `AvanueSurface`
- `WaterCard` → use `AvanueCard`
- `WaterButton` → use `AvanueButton`

**Renamed:**
- `AndroidWaterRenderer.kt` → `WaterRendererAndroid.kt`
- `IOSWaterRenderer.kt` → `WaterRendererIOS.kt`
- `DesktopWaterRenderer.kt` → `WaterRendererDesktop.kt`

### v3.1 (2026-02-09) — Phase 1: AvanueWaterUI (Liquid Glass)

- Water effect system: `WaterLevel` (REGULAR/CLEAR/IDENTITY), `Modifier.waterEffect()`
- Water components: `WaterSurface`, `WaterCard`, `WaterButton`, `WaterNavigationBar`
- Water tokens: `WaterTokens` (blur, refraction, caustic, specular parameters)
- Water theme: `AvanueWaterScheme` with `AvanueTheme.water` accessor
- Platform renderers: Android AGSL (API 33+), iOS native glass (iOS 26+), Desktop Skia

### v3.0 (2026-02-08) — Consolidation

- Single module `:Modules:AvanueUI` (merged DesignSystem + Foundation)
- Glass components, tokens, themes, display profiles all in one place

---

## 1. MaterialMode — Theme-Driven Rendering

### 1.1 The Four Modes

| Mode | Rendering | When To Use |
|------|-----------|-------------|
| `Glass` | Frosted glass overlay (`Modifier.glass`) | visionOS-style, standard UIs |
| `Water` | Liquid refraction + caustics (`Modifier.waterEffect`) | AvanueUI-original Liquid Glass (DEFAULT) |
| `Cupertino` | 0dp elevation, 12dp corners, 0.33dp hairline borders | Apple iOS flat style |
| `MountainView` | Standard M3 tonal elevation, M3 shape scale | Google Material3 style |

### 1.2 Setting MaterialMode

**Via AvanueThemeProvider (v5.1 — preferred):**
```kotlin
import com.augmentalis.avanueui.theme.*
import com.augmentalis.avanueui.display.DisplayProfile
import androidx.compose.foundation.isSystemInDarkTheme

val palette = AvanueColorPalette.HYDRA
val style = MaterialMode.Water
val isDark = isSystemInDarkTheme()  // or from AppearanceMode

AvanueThemeProvider(
    colors = palette.colors(isDark),
    glass = palette.glass(isDark),
    water = palette.water(isDark),
    materialMode = style,
    isDark = isDark,
    displayProfile = DisplayProfile.PHONE
) {
    MyAppContent()
}
```

> **Note:** `AvanueThemeVariant` is DEPRECATED. Use `AvanueColorPalette` + `MaterialMode` + `AppearanceMode` independently.

### 1.3 Accessing MaterialMode

```kotlin
import com.augmentalis.avanueui.theme.AvanueTheme

@Composable
fun MyScreen() {
    val mode = AvanueTheme.materialMode  // MaterialMode enum
    // Use for conditional logic if needed
}
```

---

## 2. Unified Components (7)

All unified components live in `com.augmentalis.avanueui.components`.

### 2.1 AvanueSurface

Base interactive surface. Replaces both `GlassSurface` and `WaterSurface`.

```kotlin
import com.augmentalis.avanueui.components.AvanueSurface

AvanueSurface(
    onClick = { /* optional */ },
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
) {
    Text("Content")
}
```

**Parameters:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `onClick` | `(() -> Unit)?` | `null` | Makes surface interactive |
| `modifier` | `Modifier` | `Modifier` | Standard modifier |
| `shape` | `Shape` | `GlassDefaults.shape` | Surface shape |
| `color` | `Color` | `AvanueTheme.colors.surface` | Base color |
| `contentColor` | `Color` | `AvanueTheme.colors.textPrimary` | Content color |
| `content` | `@Composable () -> Unit` | required | Content |

### 2.2 AvanueCard

Card container. Replaces both `GlassCard` and `WaterCard`.

```kotlin
import com.augmentalis.avanueui.components.AvanueCard

AvanueCard(
    onClick = { navigateToDetail() },
    modifier = Modifier.padding(SpacingTokens.sm)
) {
    Text("Card Title", style = MaterialTheme.typography.titleMedium)
    Text("Card body content")
}
```

**Parameters:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `onClick` | `(() -> Unit)?` | `null` | Makes card interactive |
| `modifier` | `Modifier` | `Modifier` | Standard modifier |
| `shape` | `Shape` | `GlassDefaults.shape` | Card shape |
| `content` | `@Composable ColumnScope.() -> Unit` | required | Content (column scope) |

### 2.3 AvanueButton

Primary button. Replaces both `OceanButton` and `WaterButton`.

```kotlin
import com.augmentalis.avanueui.components.AvanueButton

AvanueButton(onClick = { submit() }) {
    Text("Submit")
}
```

**Parameters:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `onClick` | `() -> Unit` | required | Click handler |
| `modifier` | `Modifier` | `Modifier` | Standard modifier |
| `enabled` | `Boolean` | `true` | Enable/disable |
| `shape` | `Shape` | `GlassDefaults.shape` | Button shape |
| `contentPadding` | `PaddingValues` | `ButtonDefaults.ContentPadding` | Inner padding |
| `content` | `@Composable RowScope.() -> Unit` | required | Content (row scope) |

### 2.4 AvanueChip

Chip/tag. Replaces `GlassChip`.

```kotlin
import com.augmentalis.avanueui.components.AvanueChip

AvanueChip(
    onClick = { selectFilter() },
    label = { Text("Active") },
    leadingIcon = { Icon(Icons.Default.Check, null) }
)
```

**Parameters:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `onClick` | `() -> Unit` | required | Click handler |
| `label` | `@Composable () -> Unit` | required | Chip label |
| `modifier` | `Modifier` | `Modifier` | Standard modifier |
| `enabled` | `Boolean` | `true` | Enable/disable |
| `leadingIcon` | `@Composable (() -> Unit)?` | `null` | Leading icon |
| `trailingIcon` | `@Composable (() -> Unit)?` | `null` | Trailing icon |
| `shape` | `Shape` | `GlassShapes.chipShape` | Chip shape |

### 2.5 AvanueBubble

Chat bubble. Replaces `GlassBubble`.

```kotlin
import com.augmentalis.avanueui.components.AvanueBubble
import com.augmentalis.avanueui.components.glass.BubbleAlign

AvanueBubble(align = BubbleAlign.END) {
    Text("Hello from user")
}
```

### 2.6 AvanueFAB

Floating action button. Replaces `GlassFloatingActionButton`.

```kotlin
import com.augmentalis.avanueui.components.AvanueFAB

AvanueFAB(onClick = { addItem() }) {
    Icon(Icons.Default.Add, contentDescription = "Add")
}
```

### 2.7 AvanueIconButton

Icon button. Replaces `GlassIconButton`.

```kotlin
import com.augmentalis.avanueui.components.AvanueIconButton

AvanueIconButton(onClick = { toggleMenu() }) {
    Icon(Icons.Default.Menu, contentDescription = "Menu")
}
```

---

## 3. Migration Guide — Kotlin Consumers

### 3.1 Import Changes

```
BEFORE: import com.augmentalis.avanueui.components.glass.GlassCard
AFTER:  import com.augmentalis.avanueui.components.AvanueCard

BEFORE: import com.augmentalis.avanueui.components.glass.GlassSurface
AFTER:  import com.augmentalis.avanueui.components.AvanueSurface

BEFORE: import com.augmentalis.avanueui.components.glass.OceanButton
AFTER:  import com.augmentalis.avanueui.components.AvanueButton

BEFORE: import com.augmentalis.avanueui.components.glass.GlassChip
AFTER:  import com.augmentalis.avanueui.components.AvanueChip

BEFORE: import com.augmentalis.avanueui.components.glass.GlassBubble
AFTER:  import com.augmentalis.avanueui.components.AvanueBubble

BEFORE: import com.augmentalis.avanueui.components.glass.GlassFloatingActionButton
AFTER:  import com.augmentalis.avanueui.components.AvanueFAB

BEFORE: import com.augmentalis.avanueui.components.glass.GlassIconButton
AFTER:  import com.augmentalis.avanueui.components.AvanueIconButton

BEFORE: import com.augmentalis.avanueui.components.glass.GlassIndicator
AFTER:  import com.augmentalis.avanueui.components.AvanueSurface  // (wrap Row manually)

BEFORE: import com.augmentalis.avanueui.components.water.WaterSurface
AFTER:  import com.augmentalis.avanueui.components.AvanueSurface

BEFORE: import com.augmentalis.avanueui.components.water.WaterCard
AFTER:  import com.augmentalis.avanueui.components.AvanueCard

BEFORE: import com.augmentalis.avanueui.components.water.WaterButton
AFTER:  import com.augmentalis.avanueui.components.AvanueButton
```

### 3.2 Parameter Removal

Unified components intentionally do NOT expose `glassLevel`, `waterLevel`, `glass`, or `border` parameters. The theme controls these internally.

```kotlin
// BEFORE:
GlassCard(
    glassLevel = GlassLevel.MEDIUM,
    border = GlassDefaults.border
) { ... }

// AFTER:
AvanueCard { ... }
// The theme provides MEDIUM glass automatically when MaterialMode is GLASS
```

**Parameters that carry over 1:1:** `onClick`, `modifier`, `shape`, `enabled`, `content`, `contentPadding`, `leadingIcon`, `trailingIcon`.

### 3.3 Special Cases

**GlassIndicator → AvanueSurface + Row:**
```kotlin
// BEFORE:
GlassIndicator(glassLevel = GlassLevel.MEDIUM) {
    Icon(...)
    Text("Status")
}

// AFTER:
AvanueSurface(shape = GlassShapes.small) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(...)
        Text("Status")
    }
}
```

**OceanButton with glass=false (non-glass button):**
```kotlin
// BEFORE:
OceanButton(onClick = { }, glass = false) { Text("Plain") }

// AFTER — if you want theme-controlled:
AvanueButton(onClick = { }) { Text("Themed") }

// AFTER — if you want always-plain M3 button:
Button(onClick = { }) { Text("Always M3") }
```

### 3.4 Power User Override

If you need explicit control over glass/water level (rare), the old components remain public (deprecated). Use `@Suppress("DEPRECATION")`:

```kotlin
@Suppress("DEPRECATION")
GlassCard(glassLevel = GlassLevel.HEAVY, border = GlassDefaults.border) { ... }
```

---

## 4. Migration Guide — TypeScript / Web Apps

The Web Renderer (`Modules/AvanueUI/Renderers/Web/`) needs updates to reflect the unified component architecture. Here's what needs to change:

### 4.1 Add MaterialMode Type

In `Renderers/Web/src/types/index.ts`:
```typescript
/**
 * Material rendering mode — controls visual effects per component
 */
export enum MaterialMode {
  GLASS = 'Glass',          // Frosted glass overlay (CSS backdrop-filter)
  WATER = 'Water',          // Liquid refraction + caustics (CSS animations + backdrop-filter)
  CUPERTINO = 'Cupertino',  // Apple iOS flat (hairline borders, 0 elevation)
  MOUNTAIN_VIEW = 'MountainView'  // Google M3 standard
}
```

### 4.2 Add MaterialMode to Theme

In `Renderers/Web/src/theme/ThemeConverter.ts`, the theme output should include:
```typescript
interface AvanueWebTheme {
  // ... existing color, spacing, etc. properties
  materialMode: MaterialMode;
}
```

### 4.3 Create Unified Component Wrappers

Create these in `Renderers/Web/src/components/unified/`:

| File | Maps To | Description |
|------|---------|-------------|
| `AvanueSurface.tsx` | `<div>` with glass/water/plain CSS | Unified surface |
| `AvanueCard.tsx` | Card with glass/water/plain CSS | Unified card |
| `AvanueButton.tsx` | Button with glass/water/plain CSS | Unified button |
| `AvanueChip.tsx` | Chip with glass/water/plain CSS | Unified chip |
| `AvanueBubble.tsx` | Chat bubble with glass/water/plain CSS | Unified bubble |
| `AvanueFAB.tsx` | FAB with glass/water/plain CSS | Unified FAB |
| `AvanueIconButton.tsx` | Icon button with glass/water/plain CSS | Unified icon button |

**Example pattern for `AvanueCard.tsx`:**
```tsx
import React from 'react';
import { useAvanueTheme } from '../../theme/ThemeContext';
import { MaterialMode } from '../../types';

interface AvanueCardProps {
  onClick?: () => void;
  className?: string;
  children: React.ReactNode;
}

export const AvanueCard: React.FC<AvanueCardProps> = ({ onClick, className, children }) => {
  const theme = useAvanueTheme();

  const modeClass = {
    [MaterialMode.GLASS]: 'avanue-card--glass',
    [MaterialMode.WATER]: 'avanue-card--water',
    [MaterialMode.CUPERTINO]: 'avanue-card--cupertino',
    [MaterialMode.MOUNTAIN_VIEW]: 'avanue-card--mountain-view',
  }[theme.materialMode];

  return (
    <div
      className={`avanue-card ${modeClass} ${className ?? ''}`}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      {children}
    </div>
  );
};
```

### 4.4 CSS Token Additions

Add water effect CSS to the design token CSS file:

```css
/* Glass mode */
.avanue-card--glass {
  background: var(--avanue-surface-glass);
  backdrop-filter: blur(var(--avanue-glass-blur));
  -webkit-backdrop-filter: blur(var(--avanue-glass-blur));
  border: 1px solid var(--avanue-glass-border);
}

/* Water mode */
.avanue-card--water {
  background: var(--avanue-surface-water);
  backdrop-filter: blur(var(--avanue-water-blur));
  -webkit-backdrop-filter: blur(var(--avanue-water-blur));
  border: 1px solid var(--avanue-water-border);
  animation: water-shimmer var(--avanue-water-shimmer-duration) linear infinite;
}

/* Cupertino mode (Apple flat) */
.avanue-card--cupertino {
  background: var(--avanue-surface);
  border: 0.33px solid var(--avanue-outline);
  border-radius: 12px;
  box-shadow: none;
}

/* MountainView mode (Google M3) */
.avanue-card--mountain-view {
  background: var(--avanue-surface);
  border: 1px solid var(--avanue-outline);
}

/* Water shimmer keyframes */
@keyframes water-shimmer {
  0% { background-position: -200% center; }
  100% { background-position: 200% center; }
}
```

### 4.5 CSS Custom Properties to Add

```css
:root {
  /* Water tokens (matching WaterTokens.kt) */
  --avanue-water-blur: 20px;
  --avanue-water-border: rgba(255, 255, 255, 0.3);
  --avanue-water-overlay-opacity: 0.15;
  --avanue-water-shimmer-duration: 8000ms;
  --avanue-water-refraction-tint: rgba(173, 216, 230, 0.12);

  /* Surface variants */
  --avanue-surface-glass: rgba(var(--avanue-surface-rgb), 0.15);
  --avanue-surface-water: rgba(var(--avanue-surface-rgb), 0.12);
}
```

---

## 5. Migration Guide — iOS Apps (SwiftUI Bridge)

The iOS Renderer bridge (`Renderers/iOS/`) needs these updates:

### 5.1 Add MaterialMode to Bridge Models

In `SwiftUIModels.kt`:
```kotlin
enum class MaterialModeDTO {
    GLASS, WATER, CUPERTINO, MOUNTAIN_VIEW
}
```

### 5.2 ThemeConverter Update

In `ThemeConverter.kt`, pass `materialMode` from `AvanueTheme.materialMode` to the SwiftUI side.

### 5.3 SwiftUI Unified Component Pattern

On the Swift side, create unified wrappers that read materialMode:

```swift
struct AvanueCard<Content: View>: View {
    let materialMode: MaterialMode
    let content: () -> Content

    var body: some View {
        switch materialMode {
        case .glass:
            content()
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        case .water:
            if #available(iOS 26, *) {
                content()
                    .glassEffect(.regular)
            } else {
                content()
                    .background(.ultraThinMaterial)
            }
        case .cupertino:
            content()
                .background(Color(.systemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(.separator, lineWidth: 0.33))
        case .mountainView:
            content()
                .background(Color(.systemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
}
```

---

## 6. Components NOT Unified

These components remain as-is (not deprecated):

| Component | Reason |
|-----------|--------|
| `WaterNavigationBar` | Unique morphing behavior, no glass equivalent |
| `PulseDot` / `StatusBadge` | Unique animation, not a surface component |
| `GlassMorphismConfig` / `Modifier.glassMorphism()` | Advanced low-level API for custom configs |
| `Modifier.glass()` | Low-level glass effect, used by unified components internally |
| `Modifier.waterEffect()` | Low-level water effect, used by unified components internally |

---

## 7. Architecture Decision: M3 Expressive

The `MountainView` mode (formerly `PLAIN`) renders standard Material3 with `AvanueTheme.colors.*`. As of Kotlin 2.1.0 / Compose 1.7.3, when M3 Expressive APIs stabilize in Compose Multiplatform, the `MountainView` branch may switch to `MaterialExpressiveTheme`.

**Impact:** ZERO consumer changes. The unified components abstract this entirely.

---

## 8. Module Structure (Updated)

```
Modules/AvanueUI/src/commonMain/kotlin/com/augmentalis/avanueui/
├── tokens/                    ← 10 static token files (+WaterTokens)
├── theme/
│   ├── AvanueTheme.kt        ← Updated: +materialMode param, +LocalMaterialMode
│   ├── AvanueColorPalette.kt  ← v5.0: SOL/LUNA/TERRA/HYDRA + isDark overloads
│   ├── AppearanceMode.kt     ← v5.1: Light/Dark/Auto
│   ├── MaterialMode.kt       ← v5.0: Glass/Water/Cupertino/MountainView
│   ├── AvanueThemeVariant.kt ← DEPRECATED
│   ├── AvanueColorScheme.kt
│   ├── AvanueGlassScheme.kt
│   ├── AvanueWaterScheme.kt
│   ├── ModuleAccent.kt        ← Per-module color accents
│   ├── Hydra*.kt, Sol*.kt, Luna*.kt, Terra*.kt  (Colors/Glass/Water + Light + XR)
│   └── Ocean*.kt, Sunset*.kt, Liquid*.kt  (DEPRECATED aliases)
├── display/
├── glass/                     ← GlassLevel, GlassBorder (unchanged)
├── water/                     ← WaterLevel, WaterBorder, waterEffect (unchanged)
└── components/
    ├── AvanueSurface.kt       ← NEW: Unified surface
    ├── AvanueCard.kt          ← NEW: Unified card
    ├── AvanueButton.kt        ← NEW: Unified button
    ├── AvanueChip.kt          ← NEW: Unified chip
    ├── AvanueBubble.kt        ← NEW: Unified bubble
    ├── AvanueFAB.kt           ← NEW: Unified FAB
    ├── AvanueIconButton.kt    ← NEW: Unified icon button
    ├── glass/                 ← @Deprecated (use unified components)
    │   ├── GlassmorphicComponents.kt  (GlassSurface, GlassCard, etc.)
    │   ├── GlassExtensions.kt        (Modifier.glass() — NOT deprecated)
    │   ├── GlassmorphismCore.kt       (Advanced configs — NOT deprecated)
    │   └── PulseDot.kt               (NOT deprecated — unique)
    ├── water/                 ← @Deprecated (use unified components)
    │   ├── WaterSurface.kt
    │   ├── WaterCard.kt
    │   ├── WaterButton.kt
    │   └── WaterNavigationBar.kt      (NOT deprecated — unique)
    └── settings/              ← Unchanged
```

---

## 9. Quick Reference

```
UNIFIED COMPONENTS (NEW — primary API):
  AvanueSurface, AvanueCard, AvanueButton, AvanueChip
  AvanueBubble, AvanueFAB, AvanueIconButton

MATERIAL MODE:
  AvanueTheme.materialMode → GLASS | WATER | PLAIN
  Set via: AvanueThemeProvider(materialMode = MaterialMode.GLASS)
  Or via: AvanueThemeVariant (OCEAN→GLASS, SUNSET→GLASS, LIQUID→WATER)

PACKAGES:
  unified  → com.augmentalis.avanueui.components.*     (new primary)
  glass    → com.augmentalis.avanueui.components.glass.* (deprecated)
  water    → com.augmentalis.avanueui.components.water.* (deprecated)
  mode     → com.augmentalis.avanueui.theme.MaterialMode

MIGRATION PATTERN:
  GlassCard(glassLevel=..., border=...) { }  →  AvanueCard { }
  WaterSurface(waterLevel=...) { }           →  AvanueSurface { }
  OceanButton(glass=true) { }               →  AvanueButton { }
```

---

## 10. Checklist — What Each Platform Needs to Do

### Kotlin/Android (DONE)
- [x] `MaterialMode.kt` added to theme
- [x] `AvanueThemeProvider` updated with materialMode param
- [x] 7 unified components created
- [x] All `apps/avanues/` screens migrated to unified components
- [x] `Modules/AI/Chat/ChatScreen.kt` migrated
- [x] Glass*/Water* components annotated `@Deprecated`
- [x] Build verification passed

### TypeScript/Web (TODO)
- [ ] Add `MaterialMode` enum to `types/index.ts`
- [ ] Add `materialMode` to `ThemeConverter.ts` output
- [ ] Create 7 unified React components in `components/unified/`
- [ ] Add water CSS tokens (blur, shimmer, border)
- [ ] Add mode-specific CSS classes (glass/water/plain)
- [ ] Deprecate old `GlassSurface.tsx`, `OceanButton.tsx` etc.
- [ ] Update `ThemeContext` to provide materialMode

### iOS/SwiftUI (TODO)
- [ ] Add `MaterialModeDTO` to `SwiftUIModels.kt`
- [ ] Update `ThemeConverter.kt` to pass materialMode
- [ ] Create 7 unified SwiftUI views
- [ ] Map water mode to `.glassEffect()` on iOS 26+
- [ ] Deprecate direct glass/water SwiftUI components

### Desktop (Kotlin — DONE via commonMain)
- [x] Shared with Android via commonMain — no extra work
- [x] `WaterRendererDesktop.kt` renamed and working

---

*Chapter 92 | AvanueUI Phase 2 — Unified Component Architecture | 2026-02-10 | Branch: 060226-1-consolidation-framework*
*Prerequisite: Chapter 91 (AvanueUI Design System Guide), Chapter 89 (AvanueWaterUI Phase 1)*
