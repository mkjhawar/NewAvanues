# AvanueUI Design System — Integration Guide

**Version:** 3.1 | **Date:** 2026-02-08 | **Status:** Current
**For:** SmartFin, Terminal Apps, Plugin Developers, Internal Teams

---

## Table of Contents

1. [What is AvanueUI?](#1-what-is-avanueui)
2. [Architecture Overview](#2-architecture-overview)
3. [Getting Started](#3-getting-started)
4. [Token System](#4-token-system)
5. [Theme System](#5-theme-system)
6. [Glass Effects](#6-glass-effects)
7. [Display Profiles & Responsive UI](#7-display-profiles--responsive-ui)
8. [Components](#8-components)
9. [AVID Voice Integration](#9-avid-voice-integration)
10. [Web Integration (React/Tailwind)](#10-web-integration-reacttailwind)
11. [Native Integration (Kotlin/KMP)](#11-native-integration-kotlinkmp)
12. [Migration Guide](#12-migration-guide)
13. [Token Reference Tables](#13-token-reference-tables)

---

## 1. What is AvanueUI?

AvanueUI is the unified design system for the Avanues ecosystem. It provides:

- **Design tokens** — colors, spacing, shapes, typography, elevation, animation, glass effects, responsive breakpoints
- **Theme system** — switchable color schemes (Ocean, Sunset, custom) with full dark mode
- **Glass effects** — glassmorphic UI components with configurable intensity
- **Display profiles** — responsive adaptation for phones, tablets, smart glasses (Vuzix, RealWear, XREAL)
- **Density scaling** — automatic dp/sp scaling per device class with zero code changes
- **AVID voice integration** — build-time voice identifiers for every interactive element
- **Cross-platform** — Kotlin Multiplatform (Android, iOS, Desktop) + Web (React/Tailwind)

### What's New (v3.1, February 2026)

- **Module consolidation:** DesignSystem + Foundation merged into single `:Modules:AvanueUI` module
- Unified token system replacing legacy `OceanDesignTokens`, `DesignTokens`, `MagicTheme`
- `AvanueTheme` replaces all previous theme objects (`OceanTheme`, `MagicTheme`)
- `DisplayProfile` system with 6 profiles including 4 smart glass variants
- Automatic density scaling via `AvanueThemeProvider`
- `TokenResolver` for AVUDSL runtime resolution
- `SunsetColors` theme (warm coral palette) alongside `OceanColors` (blue)
- Glass effect standardization via `GlassTokens` + `GlassLevel` enum
- Legacy `com.avanueui` package fully removed
- Glass components migrated from `com.augmentalis.avamagic.ui.foundation` to `com.augmentalis.avanueui.components.glass`

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    AvanueThemeProvider                       │
│  (entry point — wraps your app content)                     │
│                                                             │
│  Parameters:                                                │
│    colors: AvanueColorScheme  (OceanColors / SunsetColors)  │
│    glass: AvanueGlassScheme   (OceanGlass / SunsetGlass)    │
│    displayProfile: DisplayProfile  (PHONE / TABLET / GLASS) │
│                                                             │
│  Provides:                                                  │
│    CompositionLocal[AvanueColors]                           │
│    CompositionLocal[AvanueGlass]                            │
│    CompositionLocal[DisplayProfile]                         │
│    LocalDensity override (auto-scales all dp/sp)            │
└─────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
    Static Tokens       Theme Colors        Density Scaling
    (import anywhere)   (@Composable)       (automatic)
    - SpacingTokens     - AvanueTheme       - 1.0x on PHONE
    - ShapeTokens         .colors.*         - 0.75x on GLASS_COMPACT
    - SizeTokens        - AvanueTheme       - 0.625x on GLASS_MICRO
    - ElevationTokens     .glass.*
    - AnimationTokens   - AvanueTheme
    - GlassTokens         .displayProfile
    - ResponsiveTokens
    - TypographyTokens
```

### Key Principle: Static vs Dynamic

| Type | Access | Requires @Composable? | Examples |
|------|--------|----------------------|----------|
| **Static tokens** | Direct object access | No | `SpacingTokens.md`, `ShapeTokens.lg` |
| **Theme colors** | `AvanueTheme.colors.*` | Yes | `AvanueTheme.colors.primary` |
| **Glass scheme** | `AvanueTheme.glass.*` | Yes | `AvanueTheme.glass.overlayColor` |
| **Display profile** | `AvanueTheme.displayProfile` | Yes | `AvanueTheme.displayProfile.isGlass` |

---

## 3. Getting Started

### Kotlin/KMP (Android, Desktop, iOS)

**Step 1: Add dependency**

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":Modules:AvanueUI"))
}
```

**Step 2: Wrap your app with AvanueThemeProvider**

```kotlin
import com.augmentalis.avanueui.theme.AvanueThemeProvider
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.OceanColors
import com.augmentalis.avanueui.theme.OceanGlass
import com.augmentalis.avanueui.display.DisplayProfile

@Composable
fun MyApp() {
    AvanueThemeProvider(
        colors = OceanColors,          // or SunsetColors
        glass = OceanGlass,            // or SunsetGlass
        displayProfile = detectedProfile // from DisplayProfileResolver
    ) {
        // All content inside here has access to AvanueTheme
        MyMainScreen()
    }
}
```

**Step 3: Use tokens and theme colors**

```kotlin
import com.augmentalis.avanueui.tokens.*
import com.augmentalis.avanueui.theme.AvanueTheme

@Composable
fun MyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AvanueTheme.colors.background)
            .padding(SpacingTokens.md)  // 16dp, auto-scales per profile
    ) {
        Text(
            text = "Hello AvanueUI",
            color = AvanueTheme.colors.textPrimary,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
```

### Web (React + Tailwind)

```bash
# Install the token package
npm install @avanueui/tokens

# Or copy tokens directly
cp -r AvanueUI/tokens/ your-project/src/AvanueUI/tokens/
```

```typescript
// tailwind.config.ts
import { generateTailwindTheme } from './AvanueUI/tokens'
export default {
  theme: { extend: generateTailwindTheme() }
}
```

See [Section 10: Web Integration](#10-web-integration-reacttailwind) for full details.

---

## 4. Token System

All tokens are static objects — no `@Composable` annotation needed. Import and use anywhere.

### 4.1 SpacingTokens

8dp grid system. Base unit: `md = 16dp`.

```kotlin
import com.augmentalis.avanueui.tokens.SpacingTokens

// Usage
Modifier.padding(SpacingTokens.md)         // 16dp
Modifier.padding(SpacingTokens.sm)         // 8dp
Modifier.padding(
    horizontal = SpacingTokens.lg,         // 24dp
    vertical = SpacingTokens.sm            // 8dp
)
```

| Token | Value | Use Case |
|-------|-------|----------|
| `none` | 0dp | No spacing |
| `xxs` | 2dp | Tight inline spacing |
| `xs` | 4dp | Icon-to-text gap |
| `sm` | 8dp | Compact padding, small gaps |
| `md` | 16dp | **Default padding**, standard gaps |
| `lg` | 24dp | Section spacing, generous padding |
| `xl` | 32dp | Large section gaps |
| `xxl` | 48dp | Major section separators |
| `huge` | 64dp | Hero/full-bleed spacing |
| `minTouchTarget` | 48dp | Minimum touch target (WCAG AA) |
| `minTouchTargetSpatial` | 60dp | AR/VR interaction target |

### 4.2 ShapeTokens

Corner radius scale.

```kotlin
import com.augmentalis.avanueui.tokens.ShapeTokens

// Usage
Modifier.clip(RoundedCornerShape(ShapeTokens.md))  // 12dp corners
```

| Token | Value | Use Case |
|-------|-------|----------|
| `none` | 0dp | Sharp corners |
| `xs` | 4dp | Subtle rounding |
| `sm` | 8dp | Chips, small cards |
| `md` | 12dp | **Default cards**, dialogs |
| `lg` | 16dp | Large cards, bottom sheets |
| `xl` | 20dp | Extra rounding |
| `xxl` | 24dp | Maximum rounding |
| `full` | 9999dp | Pill/circle shape |

### 4.3 SizeTokens

Component dimensions for consistent sizing.

```kotlin
import com.augmentalis.avanueui.tokens.SizeTokens
```

**Icons:**

| Token | Value |
|-------|-------|
| `iconSm` | 16dp |
| `iconMd` | 24dp |
| `iconLg` | 32dp |
| `iconXl` | 48dp |

**Buttons:**

| Token | Value |
|-------|-------|
| `buttonHeightSm` | 32dp |
| `buttonHeightMd` | 40dp |
| `buttonHeightLg` | 48dp |
| `buttonHeightXl` | 56dp |
| `buttonMinWidth` | 88dp |

**Touch Targets:**

| Token | Value |
|-------|-------|
| `touchTarget` | 48dp |
| `touchTargetSpatial` | 60dp |

**App Structure:**

| Token | Value |
|-------|-------|
| `appBarHeight` | 56dp |
| `appBarHeightCompact` | 48dp |
| `textFieldHeight` | 56dp |
| `textFieldHeightSm` | 40dp |
| `textFieldHeightCompact` | 36dp |
| `commandBarHeight` | 64dp |
| `commandBarItemSize` | 48dp |
| `commandBarIconSize` | 24dp |
| `commandBarCornerRadius` | 32dp |
| `voiceButtonSize` | 56dp |
| `voiceButtonSizeCompact` | 48dp |
| `drawerItemHeight` | 48dp |
| `drawerGridItemSize` | 64dp |
| `chatBubbleMaxWidth` | 320dp |

### 4.4 ElevationTokens

Shadow/elevation levels.

```kotlin
import com.augmentalis.avanueui.tokens.ElevationTokens
```

| Token | Value | Use Case |
|-------|-------|----------|
| `none` | 0dp | Flat surfaces |
| `xs` | 1dp | Subtle lift |
| `sm` | 2dp | Cards, list items |
| `md` | 4dp | **Floating elements** |
| `lg` | 8dp | Dialogs, popovers |
| `xl` | 12dp | Modals |
| `xxl` | 16dp | Maximum elevation |

### 4.5 AnimationTokens

Duration constants (milliseconds).

```kotlin
import com.augmentalis.avanueui.tokens.AnimationTokens
```

| Token | Value | Use Case |
|-------|-------|----------|
| `fast` | 100ms | Micro-interactions (ripple, press) |
| `normal` | 200ms | **Default transitions** |
| `medium` | 300ms | Page transitions, reveals |
| `slow` | 500ms | Complex animations |
| `extraSlow` | 1000ms | Splash, onboarding |

### 4.6 GlassTokens

Glass effect parameters — use these to build consistent glassmorphic effects.

```kotlin
import com.augmentalis.avanueui.tokens.GlassTokens
```

**Overlay Opacity (per glass level):**

| Token | Value | Level |
|-------|-------|-------|
| `lightOverlay` | 0.10f | LIGHT |
| `mediumOverlay` | 0.15f | MEDIUM |
| `heavyOverlay` | 0.22f | HEAVY |

**Blur Radius:**

| Token | Value | Level |
|-------|-------|-------|
| `lightBlur` | 6dp | LIGHT |
| `mediumBlur` | 8dp | MEDIUM |
| `heavyBlur` | 10dp | HEAVY |

**Border Width:**

| Token | Value | Use Case |
|-------|-------|----------|
| `borderSubtle` | 0.5dp | Faint glass border |
| `borderDefault` | 1dp | **Standard glass border** |
| `borderStrong` | 1.5dp | Emphasized border |
| `borderFocused` | 2dp | Focus ring |

**Background Gradient Opacity:**

| Token | Value |
|-------|-------|
| `surfaceTopOpacity` | 0.10f |
| `surfaceTintOpacity` | 0.15f |
| `surfaceBottomOpacity` | 0.05f |

**Border Gradient:**

| Token | Value |
|-------|-------|
| `borderTopOpacity` | 0.30f |
| `borderBottomOpacity` | 0.15f |

**Shadow & Glow:**

| Token | Value |
|-------|-------|
| `shadowOpacity` | 0.25f |
| `glowOpacity` | 0.30f |
| `shadowElevation` | 8dp |

### 4.7 ResponsiveTokens

Material 3 breakpoints and grid configuration.

```kotlin
import com.augmentalis.avanueui.tokens.ResponsiveTokens
```

**Standard Breakpoints:**

| Breakpoint | Min Width | Max Width |
|------------|-----------|-----------|
| Compact | 0dp | 599dp |
| Medium | 600dp | 839dp |
| Expanded | 840dp | 1239dp |
| Large | 1240dp | 1439dp |
| Extra Large | 1440dp+ | — |

**Smart Glass Breakpoints:**

| Profile | Min | Max | Devices |
|---------|-----|-----|---------|
| Glass Micro | 0dp | 479dp | Vuzix Blade (480x480), Shield (640x360) |
| Glass Compact | 480dp | 853dp | Vuzix M400, RealWear HMT (854x480) |
| Glass Standard | 854dp | 1279dp | RealWear Nav520 (1280x720) |
| Glass HD | 1280dp+ | — | XREAL Air (1920x1080), Vuzix Z100 |

**Grid Configuration:**

| Breakpoint | Columns | Margin | Gutter |
|------------|---------|--------|--------|
| Compact | 4 | 16dp | 16dp |
| Medium | 8 | 24dp | 24dp |
| Expanded | 12 | 24dp | 24dp |
| Large | 12 | 32dp | 24dp |
| Glass | — | 8dp | 8dp |

**Max Content Width:**

| Breakpoint | Max Width |
|------------|-----------|
| Compact | 360dp |
| Medium | 600dp |
| Expanded | 840dp |
| Large | 1240dp |
| Extra Large | 1440dp |

### 4.8 TypographyTokens

Complete Material 3 type scale.

```kotlin
import com.augmentalis.avanueui.tokens.TypographyTokens
```

| Style | Size | Weight | Line Height | Letter Spacing |
|-------|------|--------|-------------|----------------|
| Display Large | 57sp | Normal | 64sp | -0.25sp |
| Display Medium | 45sp | Normal | 52sp | 0sp |
| Display Small | 36sp | Normal | 44sp | 0sp |
| Headline Large | 32sp | Normal | 40sp | 0sp |
| Headline Medium | 28sp | Normal | 36sp | 0sp |
| Headline Small | 24sp | Normal | 32sp | 0sp |
| Title Large | 22sp | Normal | 28sp | 0sp |
| Title Medium | 16sp | Medium | 24sp | 0.15sp |
| Title Small | 14sp | Medium | 20sp | 0.1sp |
| Body Large | 16sp | Normal | 24sp | 0.5sp |
| Body Medium | 14sp | Normal | 20sp | 0.25sp |
| Body Small | 12sp | Normal | 16sp | 0.4sp |
| Label Large | 14sp | Medium | 20sp | 0.1sp |
| Label Medium | 12sp | Medium | 16sp | 0.5sp |
| Label Small | 11sp | Medium | 16sp | 0.5sp |

---

## 5. Theme System

### 5.1 Color Schemes

AvanueUI ships with two color schemes. Both implement the `AvanueColorScheme` interface.

#### OceanColors (Default — Cool Blue)

| Property | Color | Hex |
|----------|-------|-----|
| `primary` | Coral Blue | `#3B82F6` |
| `onPrimary` | White | `#FFFFFF` |
| `primaryDark` | Dark Blue | `#2563EB` |
| `primaryLight` | Light Blue | `#60A5FA` |
| `secondary` | Turquoise | `#06B6D4` |
| `onSecondary` | Black | `#000000` |
| `tertiary` | Seafoam | `#10B981` |
| `error` | Coral Red | `#EF4444` |
| `onError` | White | `#FFFFFF` |
| `background` | Deep Navy | `#0F172A` |
| `surface` | Slate | `#1E293B` |
| `surfaceElevated` | Light Slate | `#334155` |
| `surfaceVariant` | Mid Slate | `#475569` |
| `surfaceInput` | Light Slate | `#334155` |
| `textPrimary` | Light Gray | `#E2E8F0` |
| `textSecondary` | Mid Gray | `#CBD5E1` |
| `textTertiary` | Cool Gray | `#94A3B8` |
| `textDisabled` | Dark Gray | `#64748B` |
| `textOnPrimary` | White | `#FFFFFF` |
| `iconPrimary` | Coral Blue | `#3B82F6` |
| `iconSecondary` | Mid Gray | `#CBD5E1` |
| `iconDisabled` | Dark Gray | `#64748B` |
| `border` | 20% White | `Color(0x33FFFFFF)` |
| `borderSubtle` | 10% White | `Color(0x1AFFFFFF)` |
| `borderStrong` | 30% White | `Color(0x4DFFFFFF)` |
| `success` | Green | `#10B981` |
| `warning` | Amber | `#F59E0B` |
| `info` | Sky Blue | `#0EA5E9` |
| `starActive` | Amber | `#FFC107` |

#### SunsetColors (Warm Coral)

| Property | Color | Hex |
|----------|-------|-----|
| `primary` | Sunset Coral | `#FF6B35` |
| `onPrimary` | White | `#FFFFFF` |
| `primaryDark` | Dark Coral | `#E55A2B` |
| `primaryLight` | Light Coral | `#FF8F66` |
| `secondary` | Warm Gold | `#FBBF24` |
| `onSecondary` | Black | `#000000` |
| `tertiary` | Rose Pink | `#F472B6` |
| `background` | Deep Purple | `#1A0E1F` |
| `surface` | Lavender | `#2D1B33` |
| `surfaceElevated` | Light Lavender | `#3D2947` |
| `textPrimary` | Warm Beige | `#F5E6D3` |
| `textSecondary` | Tan | `#D4C0B0` |
| `textTertiary` | Dusty Rose | `#A08878` |
| `textDisabled` | Muted Brown | `#6B5B52` |

### 5.2 Accessing Theme Colors

```kotlin
// Inside @Composable functions:
val bg = AvanueTheme.colors.background
val primary = AvanueTheme.colors.primary
val textColor = AvanueTheme.colors.textPrimary
val borderColor = AvanueTheme.colors.border

// In Modifier chains:
Modifier.background(AvanueTheme.colors.surface)
Modifier.border(1.dp, AvanueTheme.colors.border, RoundedCornerShape(ShapeTokens.md))
```

### 5.3 Creating a Custom Color Scheme

Implement the `AvanueColorScheme` interface:

```kotlin
object MyBrandColors : AvanueColorScheme {
    override val primary = Color(0xFFYOUR_COLOR)
    override val onPrimary = Color.White
    override val primaryDark = Color(0xFFDARKER)
    override val primaryLight = Color(0xFFLIGHTER)
    override val secondary = Color(0xFF...)
    override val onSecondary = Color.Black
    override val tertiary = Color(0xFF...)
    override val error = Color(0xFFEF4444)
    override val onError = Color.White
    override val background = Color(0xFF...)
    override val surface = Color(0xFF...)
    override val surfaceElevated = Color(0xFF...)
    override val surfaceVariant = Color(0xFF...)
    override val surfaceInput = Color(0xFF...)
    override val textPrimary = Color(0xFF...)
    override val textSecondary = Color(0xFF...)
    override val textTertiary = Color(0xFF...)
    override val textDisabled = Color(0xFF...)
    override val textOnPrimary = Color.White
    override val iconPrimary = Color(0xFF...)
    override val iconSecondary = Color(0xFF...)
    override val iconDisabled = Color(0xFF...)
    override val border = Color(0x33FFFFFF)
    override val borderSubtle = Color(0x1AFFFFFF)
    override val borderStrong = Color(0x4DFFFFFF)
    override val success = Color(0xFF10B981)
    override val warning = Color(0xFFF59E0B)
    override val info = Color(0xFF0EA5E9)
    override val starActive = Color(0xFFFFC107)

    override fun resolve(id: String): Color? = resolveDefault(id)
}
```

Then use it:

```kotlin
AvanueThemeProvider(colors = MyBrandColors) {
    MyApp()
}
```

### 5.4 Glass Scheme

Each color scheme has a companion glass scheme:

```kotlin
// OceanGlass
overlayColor = Color.White           // White overlay on dark backgrounds
tintColor = Color(0xFF1E293B)        // Slate tint
shadowColor = Color.Black
glowColor = Color(0xFF3B82F6)        // Blue glow
glassBorderTint = Color(0x263B82F6)  // Blue @ 15%

// SunsetGlass
overlayColor = Color.White
tintColor = Color(0xFF2D1B33)        // Lavender tint
shadowColor = Color.Black
glowColor = Color(0xFFFF6B35)        // Coral glow
glassBorderTint = Color(0x26FF6B35)  // Coral @ 15%
```

---

## 6. Glass Effects

### 6.1 Glass Levels

Three intensity levels for glassmorphic effects:

| Level | Overlay Opacity | Blur Radius | Use Case |
|-------|----------------|-------------|----------|
| `LIGHT` | 10% | 6dp | Subtle background panels |
| `MEDIUM` | 15% | 8dp | **Default** — cards, dialogs |
| `HEAVY` | 22% | 10dp | Prominent overlays, modals |

### 6.2 Modifier.glass()

Apply glass effect to any composable:

```kotlin
import com.augmentalis.avanueui.components.glass.glass
import com.augmentalis.avanueui.glass.GlassLevel
import com.augmentalis.avanueui.glass.GlassBorder

Box(
    modifier = Modifier
        .glass(
            backgroundColor = AvanueTheme.colors.surface,
            glassLevel = GlassLevel.MEDIUM,
            border = GlassBorder(1.dp, AvanueTheme.colors.border),
            shape = GlassShapes.default  // 12dp rounded
        )
        .padding(SpacingTokens.md)
) {
    Text("Glass Panel", color = AvanueTheme.colors.textPrimary)
}
```

### 6.3 GlassShapes

Pre-defined shapes for glass components:

| Shape | Radius | Use Case |
|-------|--------|----------|
| `default` | 12dp | Standard panels, cards |
| `small` | 8dp | Chips, small elements |
| `large` | 16dp | Large cards, bottom sheets |
| `extraLarge` | 24dp | Dialogs, overlays |
| `chipShape` | 8dp | Tags, badges |
| `buttonShape` | 12dp | Buttons |
| `fabShape` | 16dp | FABs |
| `dialogShape` | 16dp | Dialogs |
| `bottomSheetShape` | 16dp top, 0dp bottom | Bottom sheets |
| `bubbleStart` | Chat bubble (left tail) | Received messages |
| `bubbleEnd` | Chat bubble (right tail) | Sent messages |
| `circle` | 50% | Circular elements |

### 6.4 GlassDefaults

Common default values for glass components:

```kotlin
import com.augmentalis.avanueui.components.glass.GlassDefaults

// Borders
GlassDefaults.border        // 1dp, AvanueTheme.colors.border
GlassDefaults.borderSubtle  // 0.5dp, AvanueTheme.colors.borderSubtle
GlassDefaults.borderStrong  // 1.5dp, AvanueTheme.colors.borderStrong
GlassDefaults.borderFocused // 2dp, AvanueTheme.colors.primary

// Spacing
GlassDefaults.spacing       // 16dp
GlassDefaults.spacingSmall  // 8dp
GlassDefaults.spacingLarge  // 24dp

// Touch
GlassDefaults.minTouchTarget // 48dp

// Elevation
GlassDefaults.elevation     // 0dp
GlassDefaults.elevationElevated // 2dp
```

---

## 7. Display Profiles & Responsive UI

### 7.1 Six Display Profiles

| Profile | Density Scale | Font Scale | Min Touch | Layout Strategy |
|---------|--------------|------------|-----------|-----------------|
| `GLASS_MICRO` | 0.625x | 0.75x | 36dp | Single pane, paginated |
| `GLASS_COMPACT` | 0.75x | 0.85x | 40dp | Single pane, scroll |
| `GLASS_STANDARD` | 0.875x | 0.9x | 44dp | Single pane, scroll |
| `PHONE` | 1.0x | 1.0x | 48dp | Adaptive |
| `TABLET` | 1.0x | 1.0x | 48dp | List-detail |
| `GLASS_HD` | 0.9x | 0.95x | 48dp | Adaptive |

### 7.2 How Density Scaling Works

`AvanueThemeProvider` overrides `LocalDensity` based on the display profile. This means **all dp and sp values automatically scale** with zero code changes.

Example: `SpacingTokens.md` (16dp)

| Profile | Density Scale | Physical Rendering |
|---------|--------------|-------------------|
| PHONE | 1.0x | 16dp (normal) |
| GLASS_COMPACT | 0.75x | 12dp physical |
| GLASS_MICRO | 0.625x | 10dp physical |

**You write `SpacingTokens.md` once. It adapts everywhere.**

### 7.3 Layout Strategies

```kotlin
val profile = AvanueTheme.displayProfile

when (profile.layoutStrategy) {
    LayoutStrategy.SINGLE_PANE_PAGINATED -> {
        // One item at a time, pagination navigation
        // Best for: Vuzix Blade, Shield
    }
    LayoutStrategy.SINGLE_PANE_SCROLL -> {
        // Scrollable single column
        // Best for: RealWear HMT, Vuzix M400
    }
    LayoutStrategy.ADAPTIVE -> {
        // Material3 WindowSizeClass adaptive
        // Best for: phones, XREAL Air
    }
    LayoutStrategy.LIST_DETAIL -> {
        // Two-pane list-detail layout
        // Best for: tablets, foldables
    }
}
```

### 7.4 DisplayUtils

Display-aware utilities:

```kotlin
import com.augmentalis.avanueui.display.DisplayUtils

@Composable
fun MyComponent() {
    // Touch target that maintains physical minimum across all profiles
    val touchSize = DisplayUtils.minTouchTarget
    // On GLASS_MICRO: 36dp / 0.625 = 57.6dp (renders as 36dp physical)
    // On PHONE: 48dp / 1.0 = 48dp (renders as 48dp physical)

    val isGlass = DisplayUtils.isGlass  // true for any glass profile

    val strategy = DisplayUtils.layoutStrategy  // current layout strategy
}
```

### 7.5 Detecting Display Profile

```kotlin
import com.augmentalis.avanueui.display.DisplayProfileResolver

// Pure function — no platform dependencies
val profile = DisplayProfileResolver.resolve(
    widthPx = windowWidthPixels,
    heightPx = windowHeightPixels,
    densityDpi = displayDensityDpi,
    isSmartGlass = isRunningOnSmartGlasses  // from DeviceCapabilityFactory
)
```

**Smart glass detection heuristics (when `isSmartGlass = true`):**

| Max Dimension | Profile |
|---------------|---------|
| ≤ 640px | GLASS_MICRO |
| ≤ 960px | GLASS_COMPACT |
| ≤ 1280px | GLASS_STANDARD |
| > 1280px | GLASS_HD |

**Phone/tablet detection (when `isSmartGlass = false`):**

| Smallest Width | Profile |
|---------------|---------|
| ≥ 600dp | TABLET |
| < 600dp | PHONE |

### 7.6 Wiring in Your App

```kotlin
// In your Application class:
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DeviceCapabilityFactory.initialize(this)
    }
}

// In your Activity:
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val displayMetrics = resources.displayMetrics
            val profile = DisplayProfileResolver.resolve(
                widthPx = displayMetrics.widthPixels,
                heightPx = displayMetrics.heightPixels,
                densityDpi = displayMetrics.densityDpi,
                isSmartGlass = DeviceCapabilityFactory.isSmartGlass()
            )

            AvanueThemeProvider(
                colors = OceanColors,
                glass = OceanGlass,
                displayProfile = profile
            ) {
                MyApp()
            }
        }
    }
}
```

---

## 8. Components

### 8.1 Glass Components

AvanueUI provides ready-to-use glassmorphic components in the `components.glass` package.

**Dependency:**

```kotlin
implementation(project(":Modules:AvanueUI"))
```

#### GlassSurface

Base interactive glassmorphic surface:

```kotlin
GlassSurface(
    onClick = { /* action */ },
    modifier = Modifier.fillMaxWidth(),
    shape = GlassShapes.default,
    glassLevel = GlassLevel.MEDIUM,
    border = GlassDefaults.border
) {
    Text("Interactive glass surface")
}
```

#### GlassCard

Card container with glass effect:

```kotlin
GlassCard(
    onClick = { /* optional */ },
    modifier = Modifier.fillMaxWidth(),
    glassLevel = GlassLevel.LIGHT
) {
    // ColumnScope content
    Text("Card Title", style = MaterialTheme.typography.titleMedium)
    Text("Card content here")
}
```

#### GlassBubble

Chat message bubble with alignment:

```kotlin
// Received message (left-aligned)
GlassBubble(align = GlassBubble.Alignment.START) {
    Text("Hello from assistant")
}

// Sent message (right-aligned)
GlassBubble(align = GlassBubble.Alignment.END) {
    Text("Hello from user")
}
```

#### OceanButton

Primary button with optional glass effect:

```kotlin
OceanButton(
    onClick = { /* action */ },
    glass = true,              // enable glass effect
    glassLevel = GlassLevel.MEDIUM
) {
    Text("Glass Button")
}

OceanButton(
    onClick = { /* action */ },
    glass = false              // standard M3 button
) {
    Text("Standard Button")
}
```

#### GlassChip

Tag/chip with optional glass:

```kotlin
GlassChip(
    onClick = { /* action */ },
    label = { Text("Active") },
    leadingIcon = { Icon(Icons.Default.Check, null) },
    glass = true
)
```

#### GlassFloatingActionButton

FAB with mandatory glass effect:

```kotlin
GlassFloatingActionButton(
    onClick = { /* action */ },
    glassLevel = GlassLevel.MEDIUM
) {
    Icon(Icons.Default.Add, "Add")
}
```

#### GlassIconButton

Icon button with optional glass:

```kotlin
GlassIconButton(
    onClick = { /* action */ },
    glass = true,
    glassLevel = GlassLevel.LIGHT
) {
    Icon(Icons.Default.Settings, "Settings")
}
```

### 8.2 PulseDot — Service State Indicator

Animated dot with concentric pulse rings:

```kotlin
import com.augmentalis.avanueui.components.glass.PulseDot
import com.augmentalis.avanueui.components.glass.StatusBadge

// Animated dot
PulseDot(
    state = ServiceState.Running,  // Running, Ready, Stopped, Error, Degraded
    dotSize = 12.dp,
    pulseDurationMs = 2000
)

// Pill-shaped status badge
StatusBadge(state = ServiceState.Running)  // Shows "ACTIVE" in green
```

| State | Color | Animation |
|-------|-------|-----------|
| Running | Green (success) | Expanding pulse rings |
| Ready | Blue (info) | Subtle breathing glow |
| Stopped | Gray (textDisabled) | No animation |
| Error | Red (error) | Fast pulse |
| Degraded | Amber (warning) | Slow pulse |

### 8.3 GlassMorphismConfig — Advanced Glass Effects

For fine-grained control:

```kotlin
import com.augmentalis.avanueui.components.glass.GlassMorphismConfig
import com.augmentalis.avanueui.components.glass.DepthLevel
import com.augmentalis.avanueui.components.glass.GlassPresets

// Use a preset
Modifier.glassMorphism(GlassPresets.Card, DepthLevel.Normal)

// Or customize
val config = GlassMorphismConfig(
    cornerRadius = 16.dp,
    backgroundOpacity = 0.1f,
    borderOpacity = 0.2f,
    borderWidth = 1.dp,
    tintColor = Color(0xFF2196F3),
    tintOpacity = 0.15f
)
Modifier.glassMorphism(config, DepthLevel.Prominent)
```

**Presets:**

| Preset | Tint Color | Use Case |
|--------|-----------|----------|
| `Primary` | Blue | Primary action panels |
| `Success` | Green | Success states |
| `Warning` | Orange | Warning panels |
| `Error` | Red | Error states |
| `Info` | Blue | Information panels |
| `Card` | Subtle (small radius) | Content cards |
| `Elevated` | Prominent | Floating overlays |

**Depth Levels:**

| Level | Scale | Effect |
|-------|-------|--------|
| `Subtle` | 0.5x | Barely visible glass |
| `Normal` | 1.0x | Standard glass |
| `Prominent` | 1.5x | Strong glass |
| `Intense` | 2.0x | Maximum glass |

### 8.4 ComponentProvider — Abstraction Layer

For framework-agnostic component usage:

```kotlin
import com.augmentalis.avanueui.components.glass.ComponentProvider
import com.augmentalis.avanueui.components.glass.IconVariant
import com.augmentalis.avanueui.components.glass.ButtonVariant
import com.augmentalis.avanueui.components.glass.SurfaceVariant
```

**IconVariant** (7 color variants):

| Variant | Color Source |
|---------|-------------|
| `Primary` | `AvanueTheme.colors.iconPrimary` |
| `Secondary` | `AvanueTheme.colors.iconSecondary` |
| `Disabled` | `AvanueTheme.colors.iconDisabled` |
| `Success` | `AvanueTheme.colors.success` |
| `Warning` | `AvanueTheme.colors.warning` |
| `Error` | `AvanueTheme.colors.error` |
| `OnPrimary` | `AvanueTheme.colors.onPrimary` |

**ButtonVariant** (4 styles):

| Variant | Style |
|---------|-------|
| `Primary` | Filled with primary color |
| `Secondary` | Outlined |
| `Tertiary` | Text only |
| `Ghost` | Transparent background |

**SurfaceVariant** (4 types):

| Variant | Background |
|---------|-----------|
| `Default` | `surface` |
| `Elevated` | `surfaceElevated` |
| `Input` | `surfaceInput` |
| `Glass` | Glass effect |

---

## 9. AVID Voice Integration

Every interactive AvanueUI element supports AVID (Avanues Voice IDentifier) for voice command integration.

### Element-Level AVID

```kotlin
// Kotlin/Compose
Button(
    onClick = { submit() },
    modifier = Modifier.semantics {
        contentDescription = "AVID-A-000042:BTN:click submit:press,tap"
    }
) {
    Text("Submit")
}
```

```tsx
// React/Web
<Button
  data-avid="AVID-W-000042"
  data-avid-type="BTN"
  data-avid-cmd="click submit"
  data-avid-syn="press,tap"
  onClick={handleSubmit}
>
  Submit
</Button>
```

### 22 AVID Type Codes

| Code | Element | Code | Element |
|------|---------|------|---------|
| BTN | Button | INP | Input |
| TXT | Text | IMG | Image |
| LST | List | ITM | Item |
| SCR | Screen | NAV | Navigation |
| SWT | Switch | SLR | Slider |
| SEL | Select | DIA | Dialog |
| MNU | Menu | CRD | Card |
| TAB | Tab | FAB | FloatingAction |
| PRG | Progress | WBV | WebView |
| CHK | Checkbox | RDO | Radio |
| LNK | Link | HDR | Header |

### AVID Sequence Ranges

| App | Range | Platform |
|-----|-------|----------|
| AvanueCentral | AVID-W-000001 — 000999 | Web |
| SmartFin | AVID-W-001000 — 001999 | Web |
| Avanues App | AVID-A-000001 — 000999 | Android |
| Plugin Slots | AVID-W-002000+ | Any |
| Dynamic/Local | AVIDL-{P}-XXXXXX | Any |

---

## 10. Web Integration (React/Tailwind)

### Token Mapping (Kotlin → CSS/Tailwind)

| Kotlin Token | CSS Variable | Tailwind Class |
|-------------|-------------|----------------|
| `SpacingTokens.xs` (4dp) | `--spacing-xs: 0.25rem` | `p-xs`, `gap-xs` |
| `SpacingTokens.sm` (8dp) | `--spacing-sm: 0.5rem` | `p-sm`, `gap-sm` |
| `SpacingTokens.md` (16dp) | `--spacing-md: 1rem` | `p-md`, `gap-md` |
| `SpacingTokens.lg` (24dp) | `--spacing-lg: 1.5rem` | `p-lg`, `gap-lg` |
| `SpacingTokens.xl` (32dp) | `--spacing-xl: 2rem` | `p-xl`, `gap-xl` |
| `ShapeTokens.sm` (8dp) | `--radius-sm: 0.5rem` | `rounded-sm` |
| `ShapeTokens.md` (12dp) | `--radius-md: 0.75rem` | `rounded-md` |
| `ShapeTokens.lg` (16dp) | `--radius-lg: 1rem` | `rounded-lg` |
| `ElevationTokens.sm` (2dp) | `box-shadow: 0 1px 2px` | `shadow-sm` |
| `ElevationTokens.md` (4dp) | `box-shadow: 0 2px 4px` | `shadow-md` |

### Color Variable Mapping

```css
/* Ocean Theme (default) */
:root {
  --primary: 217 91% 60%;           /* #3B82F6 */
  --on-primary: 0 0% 100%;          /* white */
  --secondary: 187 96% 42%;         /* #06B6D4 */
  --background: 222 47% 11%;        /* #0F172A */
  --surface: 217 33% 17%;           /* #1E293B */
  --surface-elevated: 215 25% 27%;  /* #334155 */
  --text-primary: 214 32% 91%;      /* #E2E8F0 */
  --text-secondary: 213 27% 84%;    /* #CBD5E1 */
  --text-tertiary: 215 16% 62%;     /* #94A3B8 */
  --border: 0 0% 100% / 0.20;       /* 20% white */
  --success: 160 84% 39%;           /* #10B981 */
  --warning: 38 92% 50%;            /* #F59E0B */
  --error: 0 84% 60%;               /* #EF4444 */
  --info: 199 89% 48%;              /* #0EA5E9 */
}
```

### Glass Effect in CSS

```css
.glass-light {
  background: rgba(255, 255, 255, 0.10);
  backdrop-filter: blur(6px);
  border: 0.5px solid rgba(255, 255, 255, 0.10);
}

.glass-medium {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.20);
}

.glass-heavy {
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(10px);
  border: 1.5px solid rgba(255, 255, 255, 0.30);
}
```

### SmartFin-Specific Notes

SmartFin currently uses its own theme system with 8 variants. To adopt AvanueUI tokens:

1. **Map your 8 variants** to AvanueUI token overrides (each variant becomes a CSS variable set)
2. **Replace hardcoded colors** with CSS variables from the token system
3. **Keep your 28 magic components** — they become `blocks/finance/` in the AvanueUI hierarchy
4. **Add AVID attributes** to interactive elements (SmartFin range: AVID-W-001000 — 001999)

---

## 11. Native Integration (Kotlin/KMP)

### Module Dependency

```
Your App
  └── :Modules:AvanueUI     (tokens, theme, display profiles, glass components, PulseDot)
```

**Single dependency:** All tokens, themes, display profiles, and glass components are in `:Modules:AvanueUI`. The DesignSystem and Foundation sub-modules have been consolidated into the root module (v3.1).

### Package Imports

```kotlin
// Tokens (static, use anywhere)
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.tokens.ShapeTokens
import com.augmentalis.avanueui.tokens.SizeTokens
import com.augmentalis.avanueui.tokens.ElevationTokens
import com.augmentalis.avanueui.tokens.AnimationTokens
import com.augmentalis.avanueui.tokens.GlassTokens
import com.augmentalis.avanueui.tokens.ResponsiveTokens
import com.augmentalis.avanueui.tokens.TypographyTokens

// Theme (@Composable required)
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.AvanueThemeProvider
import com.augmentalis.avanueui.theme.OceanColors
import com.augmentalis.avanueui.theme.SunsetColors
import com.augmentalis.avanueui.theme.OceanGlass
import com.augmentalis.avanueui.theme.SunsetGlass

// Display profiles
import com.augmentalis.avanueui.display.DisplayProfile
import com.augmentalis.avanueui.display.DisplayProfileResolver
import com.augmentalis.avanueui.display.DisplayUtils
import com.augmentalis.avanueui.display.LayoutStrategy

// Glass
import com.augmentalis.avanueui.glass.GlassLevel
import com.augmentalis.avanueui.glass.GlassBorder

// Glass components
import com.augmentalis.avanueui.components.glass.GlassSurface
import com.augmentalis.avanueui.components.glass.GlassCard
import com.augmentalis.avanueui.components.glass.GlassBubble
import com.augmentalis.avanueui.components.glass.OceanButton
import com.augmentalis.avanueui.components.glass.GlassChip
import com.augmentalis.avanueui.components.glass.GlassShapes
import com.augmentalis.avanueui.components.glass.GlassDefaults
import com.augmentalis.avanueui.components.glass.GlassMorphismConfig
import com.augmentalis.avanueui.components.glass.GlassPresets
import com.augmentalis.avanueui.components.glass.PulseDot
import com.augmentalis.avanueui.components.glass.StatusBadge

// Settings components
import com.augmentalis.avanueui.components.settings.SettingsSection
import com.augmentalis.avanueui.components.settings.SettingsToggle
import com.augmentalis.avanueui.components.settings.SettingsSlider

// Token resolver (for AVUDSL runtime)
import com.augmentalis.avanueui.tokens.TokenResolver
```

### BANNED Imports (Deleted/Deprecated)

These are removed from the codebase. Do NOT use them:

| Import | Replacement |
|--------|-------------|
| `com.augmentalis.avamagic.ui.foundation.*` | `com.augmentalis.avanueui.components.glass.*` |
| `com.augmentalis.avamagic.ui.foundation.OceanDesignTokens` | `SpacingTokens`, `ShapeTokens`, etc. |
| `com.augmentalis.webavanue.OceanDesignTokens` | Same as above |
| `com.augmentalis.avamagic.designsystem.DesignTokens` | Same as above |
| `com.augmentalis.avamagic.ui.foundation.OceanTheme` | `AvanueTheme.colors` |
| `com.augmentalis.webavanue.OceanTheme` | `AvanueTheme.colors` |
| `com.avanueui.*` (entire old package) | See table below |
| `com.avanueui.ColorTokens.Primary` | `AvanueTheme.colors.primary` |
| `com.avanueui.ShapeTokens.Small` | `ShapeTokens.sm` |
| `com.avanueui.GlassIntensity` | `com.augmentalis.avanueui.glass.GlassLevel` |
| `com.avanueui.GlassSurface` | `com.augmentalis.avanueui.components.glass.GlassSurface` |
| `com.avanueui.OceanButton` | `com.augmentalis.avanueui.components.glass.OceanButton` |
| `com.avanueui.settings.*` | `com.augmentalis.avanueui.components.settings.*` |

### TokenResolver (AVUDSL Runtime)

For dynamic token resolution at runtime (e.g., from `.avp` plugin files):

```kotlin
import com.augmentalis.avanueui.tokens.TokenResolver

// Resolve by category
val spacing: Dp? = TokenResolver.resolveSpacing("md")       // 16.dp
val shape: Dp? = TokenResolver.resolveShape("lg")            // 16.dp
val size: Dp? = TokenResolver.resolveSize("iconMd")          // 24.dp
val elevation: Dp? = TokenResolver.resolveElevation("sm")    // 2.dp
val anim: Int? = TokenResolver.resolveAnimation("normal")    // 200
val style: TextStyle? = TokenResolver.resolveTypography("bodyLarge")

// Resolve colors (requires scheme)
val color: Color? = TokenResolver.resolveColor("primary", AvanueTheme.colors)

// Resolve by full ID
val value: Any? = TokenResolver.resolve("spacing.md", AvanueTheme.colors)
```

---

## 12. Migration Guide

### From OceanTheme to AvanueTheme.colors

| Before | After |
|--------|-------|
| `OceanTheme.background` | `AvanueTheme.colors.background` |
| `OceanTheme.surface` | `AvanueTheme.colors.surface` |
| `OceanTheme.primary` | `AvanueTheme.colors.primary` |
| `OceanTheme.textPrimary` | `AvanueTheme.colors.textPrimary` |
| `OceanTheme.textSecondary` | `AvanueTheme.colors.textSecondary` |
| `OceanTheme.border` | `AvanueTheme.colors.border` |
| `OceanTheme.success` | `AvanueTheme.colors.success` |
| `OceanTheme.warning` | `AvanueTheme.colors.warning` |
| `OceanTheme.error` | `AvanueTheme.colors.error` |
| `OceanTheme.info` | `AvanueTheme.colors.info` |
| `OceanTheme.iconActive` | `AvanueTheme.colors.textPrimary` |
| `OceanTheme.iconInactive` | `AvanueTheme.colors.textTertiary` |
| `OceanTheme.iconOnPrimary` | `AvanueTheme.colors.onPrimary` |
| `OceanTheme.starActive` | `AvanueTheme.colors.starActive` |
| `OceanTheme.borderFocused` | `AvanueTheme.colors.primary` |
| `OceanTheme.voiceListening` | `AvanueTheme.colors.primary` |
| `OceanTheme.loading` | `AvanueTheme.colors.primary` |

### From OceanDesignTokens to Token Objects

| Before | After |
|--------|-------|
| `OceanDesignTokens.Spacing.Small` | `SpacingTokens.sm` |
| `OceanDesignTokens.Spacing.Medium` | `SpacingTokens.md` |
| `OceanDesignTokens.Spacing.Large` | `SpacingTokens.lg` |
| `OceanDesignTokens.Shape.Small` | `ShapeTokens.sm` |
| `OceanDesignTokens.Shape.Medium` | `ShapeTokens.md` |
| `OceanDesignTokens.Shape.Large` | `ShapeTokens.lg` |
| `OceanDesignTokens.Elevation.Level2` | `ElevationTokens.sm` |
| `OceanDesignTokens.Elevation.Level3` | `ElevationTokens.md` |
| `OceanDesignTokens.Size.IconSmall` | `SizeTokens.iconSm` |
| `OceanDesignTokens.Size.IconMedium` | `SizeTokens.iconMd` |
| `OceanDesignTokens.Size.TouchTarget` | `SizeTokens.touchTarget` |

### From ColorTokens to AvanueTheme.colors

| Before | After |
|--------|-------|
| `ColorTokens.Primary` | `AvanueTheme.colors.primary` |
| `ColorTokens.Background` | `AvanueTheme.colors.background` |
| `ColorTokens.Surface` | `AvanueTheme.colors.surface` |

### From Foundation Module to AvanueUI Components

After the v3.1 consolidation, all Foundation glass components moved:

| Before | After |
|--------|-------|
| `com.augmentalis.avamagic.ui.foundation.GlassSurface` | `com.augmentalis.avanueui.components.glass.GlassSurface` |
| `com.augmentalis.avamagic.ui.foundation.GlassCard` | `com.augmentalis.avanueui.components.glass.GlassCard` |
| `com.augmentalis.avamagic.ui.foundation.GlassBubble` | `com.augmentalis.avanueui.components.glass.GlassBubble` |
| `com.augmentalis.avamagic.ui.foundation.OceanButton` | `com.augmentalis.avanueui.components.glass.OceanButton` |
| `com.augmentalis.avamagic.ui.foundation.GlassChip` | `com.augmentalis.avanueui.components.glass.GlassChip` |
| `com.augmentalis.avamagic.ui.foundation.PulseDot` | `com.augmentalis.avanueui.components.glass.PulseDot` |
| `com.augmentalis.avamagic.ui.foundation.StatusBadge` | `com.augmentalis.avanueui.components.glass.StatusBadge` |
| `com.augmentalis.avamagic.ui.foundation.GlassMorphismConfig` | `com.augmentalis.avanueui.components.glass.GlassMorphismConfig` |
| `com.augmentalis.avamagic.ui.foundation.glassMorphism` | `com.augmentalis.avanueui.components.glass.glassMorphism` |
| `com.augmentalis.avamagic.ui.foundation.glass` | `com.augmentalis.avanueui.components.glass.glass` |

### From Legacy com.avanueui to New Packages

| Before | After |
|--------|-------|
| `com.avanueui.GlassIntensity.LIGHT` | `GlassLevel.LIGHT` |
| `com.avanueui.GlassIntensity.MEDIUM` | `GlassLevel.MEDIUM` |
| `com.avanueui.GlassIntensity.HEAVY` | `GlassLevel.HEAVY` |
| `com.avanueui.GlassSurface(intensity=, showBorder=)` | `GlassSurface(glassLevel=, border=)` |
| `com.avanueui.GlassCard(intensity=)` | `GlassCard(glassLevel=)` |
| `com.avanueui.OceanButton(style=PRIMARY)` | `OceanButton()` (primary by default) |
| `com.avanueui.OceanButton(style=SECONDARY)` | `TextButton()` |
| `com.avanueui.settings.*` | `com.augmentalis.avanueui.components.settings.*` |

### Build Dependency Change

| Before | After |
|--------|-------|
| `implementation(project(":Modules:AvanueUI:DesignSystem"))` | `implementation(project(":Modules:AvanueUI"))` |
| `implementation(project(":Modules:AvanueUI:Foundation"))` | `implementation(project(":Modules:AvanueUI"))` |

**Important spacing note:** Old `OceanDesignTokens.Spacing.md` was 12dp. New `SpacingTokens.md` is 16dp (Material 3 standard). Verify your layouts when migrating.

---

## 13. Token Reference Tables

### Complete Color Scheme Interface

```kotlin
interface AvanueColorScheme {
    // Primary
    val primary: Color
    val onPrimary: Color
    val primaryDark: Color
    val primaryLight: Color

    // Secondary & Tertiary
    val secondary: Color
    val onSecondary: Color
    val tertiary: Color

    // Error
    val error: Color
    val onError: Color

    // Background & Surface
    val background: Color
    val surface: Color
    val surfaceElevated: Color
    val surfaceVariant: Color
    val surfaceInput: Color

    // Text
    val textPrimary: Color
    val textSecondary: Color
    val textTertiary: Color
    val textDisabled: Color
    val textOnPrimary: Color

    // Icons
    val iconPrimary: Color
    val iconSecondary: Color
    val iconDisabled: Color

    // Borders
    val border: Color
    val borderSubtle: Color
    val borderStrong: Color

    // Semantic
    val success: Color
    val warning: Color
    val info: Color

    // Special
    val starActive: Color

    // Runtime resolver (for AVUDSL)
    fun resolve(id: String): Color?
}
```

### Complete Glass Scheme Interface

```kotlin
interface AvanueGlassScheme {
    val overlayColor: Color      // Overlay tint (white on dark, black on light)
    val tintColor: Color         // Background gradient middle tint
    val shadowColor: Color       // Shadow color
    val glowColor: Color         // Ambient glow (brand color)
    val glassBorderTint: Color   // Subtle brand tint on borders
}
```

### Display Profile Enum

```kotlin
enum class DisplayProfile(
    val densityScale: Float,
    val fontScale: Float,
    val minTouchTarget: Dp,
    val layoutStrategy: LayoutStrategy
) {
    GLASS_MICRO(0.625f, 0.75f, 36.dp, SINGLE_PANE_PAGINATED),
    GLASS_COMPACT(0.75f, 0.85f, 40.dp, SINGLE_PANE_SCROLL),
    GLASS_STANDARD(0.875f, 0.9f, 44.dp, SINGLE_PANE_SCROLL),
    PHONE(1.0f, 1.0f, 48.dp, ADAPTIVE),
    TABLET(1.0f, 1.0f, 48.dp, LIST_DETAIL),
    GLASS_HD(0.9f, 0.95f, 48.dp, ADAPTIVE);

    val isGlass: Boolean get() = this != PHONE && this != TABLET
    val isPaginated: Boolean get() = layoutStrategy == SINGLE_PANE_PAGINATED
}
```

### Supported Smart Glass Devices

| Device | Resolution | Profile |
|--------|-----------|---------|
| Vuzix Blade | 480x480 | GLASS_MICRO |
| Vuzix Shield | 640x360 | GLASS_MICRO |
| Vuzix M400 | 640x360 | GLASS_COMPACT |
| RealWear HMT-1 | 854x480 | GLASS_COMPACT |
| RealWear Navigator 520 | 1280x720 | GLASS_STANDARD |
| Vuzix M4000 | 1280x720 | GLASS_STANDARD |
| XREAL Air | 1920x1080 | GLASS_HD |
| Vuzix Z100 | 1920x1080 | GLASS_HD |

---

## Quick Reference Card

```
SPACING:  xxs(2) xs(4) sm(8) md(16) lg(24) xl(32) xxl(48) huge(64)
SHAPES:   xs(4) sm(8) md(12) lg(16) xl(20) xxl(24) full(9999)
ICONS:    sm(16) md(24) lg(32) xl(48)
BUTTONS:  sm(32) md(40) lg(48) xl(56)  minWidth(88)
ELEVATE:  xs(1) sm(2) md(4) lg(8) xl(12) xxl(16)
ANIMATE:  fast(100) normal(200) medium(300) slow(500) extraSlow(1000)
GLASS:    LIGHT(10%/6dp) MEDIUM(15%/8dp) HEAVY(22%/10dp)
TOUCH:    standard(48dp) spatial(60dp)

COLORS:   AvanueTheme.colors.{property}
GLASS:    AvanueTheme.glass.{property}
PROFILE:  AvanueTheme.displayProfile

THEMES:   OceanColors (blue) | SunsetColors (coral) | Custom
PROFILES: GLASS_MICRO | GLASS_COMPACT | GLASS_STANDARD | PHONE | TABLET | GLASS_HD
```

---

*Document Version: 3.1 | Created: 2026-02-08 | Updated: 2026-02-08 | Author: Avanues Engineering*
*Applies to: SmartFin, Terminal Apps, Plugin Developers, All Internal Teams*
*Source module: NewAvanues/Modules/AvanueUI/*
