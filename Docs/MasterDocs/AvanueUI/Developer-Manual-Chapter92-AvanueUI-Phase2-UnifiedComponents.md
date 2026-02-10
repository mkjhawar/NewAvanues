# Developer Manual — Chapter 92: AvanueUI Phase 2 — Unified Component Architecture

**Date:** 2026-02-10 | **Branch:** `060226-1-consolidation-framework` | **Version:** 1.0
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
// Glass if MaterialMode.GLASS, Water if MaterialMode.WATER, M3 if MaterialMode.PLAIN
```

---

## Changelog

### v4.0 (2026-02-10) — Phase 2: Unified Component Architecture

**New:**
- `MaterialMode` enum: `GLASS`, `WATER`, `PLAIN`
- `AvanueTheme.materialMode` accessor
- 7 unified components: `AvanueSurface`, `AvanueCard`, `AvanueButton`, `AvanueChip`, `AvanueBubble`, `AvanueFAB`, `AvanueIconButton`
- `AvanueThemeVariant` now maps variants to material modes (OCEAN→GLASS, SUNSET→GLASS, LIQUID→WATER)

**Deprecated:**
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

### 1.1 The Three Modes

| Mode | Rendering | When To Use |
|------|-----------|-------------|
| `GLASS` | Frosted glass overlay (`Modifier.glass`) | Ocean/Sunset themes, standard UIs |
| `WATER` | Liquid refraction + caustics (`Modifier.waterEffect`) | Apple-inspired Liquid Glass |
| `PLAIN` | Standard Material3 with AvanueTheme colors | M3 Expressive (future), minimal UIs |

### 1.2 Setting MaterialMode

**Via AvanueThemeProvider:**
```kotlin
import com.augmentalis.avanueui.theme.AvanueThemeProvider
import com.augmentalis.avanueui.theme.MaterialMode

AvanueThemeProvider(
    colors = OceanColors,
    glass = OceanGlass,
    materialMode = MaterialMode.GLASS,  // NEW parameter
    displayProfile = DisplayProfile.PHONE
) {
    MyAppContent()
}
```

**Via AvanueThemeVariant (automatic):**
```kotlin
// Each variant has a default materialMode:
// OCEAN  → MaterialMode.GLASS
// SUNSET → MaterialMode.GLASS
// LIQUID → MaterialMode.WATER
```

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
  GLASS = 'GLASS',     // Frosted glass overlay (CSS backdrop-filter)
  WATER = 'WATER',     // Liquid refraction + caustics (CSS animations + backdrop-filter)
  PLAIN = 'PLAIN'      // Standard Material3 with AvanueTheme colors
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
    [MaterialMode.PLAIN]: 'avanue-card--plain',
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

/* Plain mode (standard M3) */
.avanue-card--plain {
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
    GLASS, WATER, PLAIN
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
        case .plain:
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

The `PLAIN` mode currently renders standard Material3 with `AvanueTheme.colors.*`. When the project upgrades to Kotlin 2.1+ / Compose Multiplatform 1.9+, the `PLAIN` branch will switch to `MaterialExpressiveTheme` — the new M3 Expressive paradigm.

**Impact:** ZERO consumer changes. The unified components abstract this entirely.

---

## 8. Module Structure (Updated)

```
Modules/AvanueUI/src/commonMain/kotlin/com/augmentalis/avanueui/
├── tokens/                    ← 10 static token files (+WaterTokens)
├── theme/
│   ├── AvanueTheme.kt        ← Updated: +materialMode param, +LocalMaterialMode
│   ├── AvanueThemeVariant.kt ← Updated: +materialMode per variant
│   ├── MaterialMode.kt       ← NEW: GLASS/WATER/PLAIN enum
│   ├── AvanueColorScheme.kt
│   ├── AvanueGlassScheme.kt
│   ├── AvanueWaterScheme.kt
│   ├── OceanColors.kt, SunsetColors.kt
│   └── OceanGlass.kt, SunsetGlass.kt, OceanWater.kt, SunsetWater.kt
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
