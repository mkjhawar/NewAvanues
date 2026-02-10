# AvanueUI v4.0 — Cross-Project Adoption Guide

**For AI agents and developers integrating AvanueUI across the Avanues ecosystem.**
**Date:** 2026-02-10 | **System Version:** v4.0 (Phase 2 Unified Components)

---

## What Is This Document?

This is the **single source of truth** for adopting AvanueUI across all Avanues projects. It tells you:
1. What AvanueUI is and how it works
2. What each project needs to do (SmartFin, AvanueCentral, NewAvanues)
3. Exact code to write for both Kotlin/Compose and TypeScript/CSS
4. What's compliant, what's not, and how to fix it

**If you're an AI or developer touching UI in any Avanues project, read this first.**

---

## Table of Contents

1. [System Architecture](#1-system-architecture)
2. [Token Reference (All Platforms)](#2-token-reference)
3. [Theme Variants & MaterialMode](#3-theme-variants--materialmode)
4. [Kotlin/Compose Integration](#4-kotlincompose-integration)
5. [TypeScript/CSS Integration](#5-typescriptcss-integration)
6. [SmartFin — What To Do](#6-smartfin--what-to-do)
7. [AvanueCentral — What To Do](#7-avanuecentral--what-to-do)
8. [NewAvanues — What To Do](#8-newavanues--what-to-do)
9. [Compliance Audit Results](#9-compliance-audit-results)
10. [Rules & Anti-Patterns](#10-rules--anti-patterns)

---

## 1. System Architecture

```
┌──────────────────────────────────────────────────────┐
│                    AvanueUI v4.0                      │
├──────────────────────────────────────────────────────┤
│  TOKENS (static, universal, no @Composable needed)   │
│  SpacingTokens · ShapeTokens · SizeTokens            │
│  ElevationTokens · AnimationTokens · GlassTokens     │
│  WaterTokens · ResponsiveTokens · TypographyTokens   │
├──────────────────────────────────────────────────────┤
│  THEME (runtime, @Composable / CSS variables)        │
│  AvanueTheme.colors · AvanueTheme.glass              │
│  AvanueTheme.water · AvanueTheme.materialMode        │
│  AvanueTheme.displayProfile                          │
│  Variants: Ocean(GLASS) · Sunset(GLASS) · Liquid(WATER) │
├──────────────────────────────────────────────────────┤
│  EFFECTS (low-level, per-platform)                   │
│  Modifier.glass() · Modifier.waterEffect()           │
│  Android: AGSL shaders · iOS: .glassEffect()         │
│  Desktop: Skia blur · Web: backdrop-filter + SVG     │
├──────────────────────────────────────────────────────┤
│  COMPONENTS (unified API, theme-driven)              │
│  AvanueSurface · AvanueCard · AvanueButton           │
│  AvanueChip · AvanueBubble · AvanueFAB               │
│  AvanueIconButton · WaterNavigationBar               │
└──────────────────────────────────────────────────────┘
```

### Key Concept: MaterialMode

```
MaterialMode.GLASS  → Frosted glass overlay (blur + translucent surface)
MaterialMode.WATER  → Liquid refraction (Apple Liquid Glass-inspired)
MaterialMode.PLAIN  → Standard Material3 with AvanueTheme colors
```

You write `AvanueCard { ... }` — the active theme variant controls the rendering. **Never choose glass vs water manually.**

### Theme Variants → MaterialMode Mapping

| Variant | MaterialMode | Primary Color | Background |
|---------|-------------|---------------|------------|
| `OCEAN` | `GLASS` | CoralBlue #3B82F6 | Deep Slate #0F172A |
| `SUNSET` | `GLASS` | SunsetCoral #FF6B35 | Deep Warm #1A0E1F |
| `LIQUID` | `WATER` | Cyan Electric #00D4FF | True Black #000000 |

---

## 2. Token Reference

### Spacing (8dp grid)

| Token | Kotlin | CSS Variable | Tailwind | Value |
|-------|--------|-------------|----------|-------|
| `none` | `SpacingTokens.none` | `--spacing-none` | `spacing-0` | 0 |
| `xxs` | `SpacingTokens.xxs` | `--spacing-xxs` | `avanue-xxs` | 2dp / 0.125rem |
| `xs` | `SpacingTokens.xs` | `--spacing-xs` | `avanue-xs` | 4dp / 0.25rem |
| `sm` | `SpacingTokens.sm` | `--spacing-sm` | `avanue-sm` | 8dp / 0.5rem |
| `md` | `SpacingTokens.md` | `--spacing-md` | `avanue-md` | 16dp / 1rem |
| `lg` | `SpacingTokens.lg` | `--spacing-lg` | `avanue-lg` | 24dp / 1.5rem |
| `xl` | `SpacingTokens.xl` | `--spacing-xl` | `avanue-xl` | 32dp / 2rem |
| `xxl` | `SpacingTokens.xxl` | `--spacing-xxl` | `avanue-xxl` | 48dp / 3rem |
| `huge` | `SpacingTokens.huge` | `--spacing-huge` | `avanue-huge` | 64dp / 4rem |
| `minTouchTarget` | `SpacingTokens.minTouchTarget` | `--touch-target` | — | 48dp / 3rem |

### Shape (Corner Radius)

| Token | Kotlin | CSS Variable | Tailwind | Value |
|-------|--------|-------------|----------|-------|
| `none` | `ShapeTokens.none` | `--radius-none` | `rounded-none` | 0 |
| `xs` | `ShapeTokens.xs` | `--radius-xs` | `rounded-avanue-xs` | 4dp / 0.25rem |
| `sm` | `ShapeTokens.sm` | `--radius-sm` | `rounded-avanue-sm` | 8dp / 0.5rem |
| `md` | `ShapeTokens.md` | `--radius-md` | `rounded-avanue-md` | 12dp / 0.75rem |
| `lg` | `ShapeTokens.lg` | `--radius-lg` | `rounded-avanue-lg` | 16dp / 1rem |
| `xl` | `ShapeTokens.xl` | `--radius-xl` | `rounded-avanue-xl` | 20dp / 1.25rem |
| `xxl` | `ShapeTokens.xxl` | `--radius-xxl` | `rounded-avanue-xxl` | 24dp / 1.5rem |
| `full` | `ShapeTokens.full` | `--radius-full` | `rounded-full` | 9999px |

### Elevation (Shadow)

| Token | Kotlin | CSS Variable | Value |
|-------|--------|-------------|-------|
| `none` | `ElevationTokens.none` | `--elevation-none` | 0dp |
| `xs` | `ElevationTokens.xs` | `--elevation-xs` | 1dp |
| `sm` | `ElevationTokens.sm` | `--elevation-sm` | 2dp |
| `md` | `ElevationTokens.md` | `--elevation-md` | 4dp |
| `lg` | `ElevationTokens.lg` | `--elevation-lg` | 8dp |
| `xl` | `ElevationTokens.xl` | `--elevation-xl` | 12dp |
| `xxl` | `ElevationTokens.xxl` | `--elevation-xxl` | 16dp |

### Animation

| Token | Kotlin | CSS Variable | Value |
|-------|--------|-------------|-------|
| `fast` | `AnimationTokens.fast` | `--duration-fast` | 100ms |
| `normal` | `AnimationTokens.normal` | `--duration-normal` | 200ms |
| `medium` | `AnimationTokens.medium` | `--duration-medium` | 300ms |
| `slow` | `AnimationTokens.slow` | `--duration-slow` | 500ms |
| `extraSlow` | `AnimationTokens.extraSlow` | `--duration-extra-slow` | 1000ms |

### Glass Effect Tokens

| Token | Kotlin | CSS | Value |
|-------|--------|-----|-------|
| Light overlay | `GlassTokens.lightOverlayOpacity` | `opacity: 0.10` | 10% |
| Medium overlay | `GlassTokens.mediumOverlayOpacity` | `opacity: 0.15` | 15% |
| Heavy overlay | `GlassTokens.heavyOverlayOpacity` | `opacity: 0.22` | 22% |
| Light blur | `GlassTokens.lightBlur` | `blur(6px)` | 6dp |
| Medium blur | `GlassTokens.mediumBlur` | `blur(8px)` | 8dp |
| Heavy blur | `GlassTokens.heavyBlur` | `blur(10px)` | 10dp |

### Water Effect Tokens (NEW in v4.0)

| Token | Kotlin | CSS | Value |
|-------|--------|-----|-------|
| Regular blur | `WaterTokens.blurRegular` | `blur(12px)` | 12dp |
| Clear blur | `WaterTokens.blurClear` | `blur(4px)` | 4dp |
| Refraction regular | `WaterTokens.refractionRegular` | `feDisplacementMap scale="4"` | 4dp |
| Refraction clear | `WaterTokens.refractionClear` | `feDisplacementMap scale="2"` | 2dp |
| Regular overlay | `WaterTokens.overlayRegular` | `opacity: 0.14` | 14% |
| Clear overlay | `WaterTokens.overlayClear` | `opacity: 0.06` | 6% |
| Press scale | `WaterTokens.pressScaleFactor` | `transform: scale(0.96)` | 0.96x |
| Shimmer duration | `WaterTokens.shimmerDuration` | `animation: 2000ms` | 2000ms |
| Caustic speed | `WaterTokens.causticSpeed` | — | 1.5f |
| Border width | `WaterTokens.borderWidth` | `border-width: 0.5px` | 0.5dp |

### Color Scheme (28 Roles)

| Role | Kotlin | CSS Variable | Ocean | Sunset | Liquid |
|------|--------|-------------|-------|--------|--------|
| primary | `AvanueTheme.colors.primary` | `--avanue-primary` | #3B82F6 | #FF6B35 | #00D4FF |
| onPrimary | `.onPrimary` | `--avanue-on-primary` | #FFFFFF | #FFFFFF | #000000 |
| secondary | `.secondary` | `--avanue-secondary` | #06B6D4 | #FBBF24 | #A78BFA |
| background | `.background` | `--avanue-background` | #0F172A | #1A0E1F | #000000 |
| surface | `.surface` | `--avanue-surface` | #1E293B | #2D1B33 | #1C1C1E |
| surfaceElevated | `.surfaceElevated` | `--avanue-surface-elevated` | #334155 | #3D2B43 | #2C2C2E |
| textPrimary | `.textPrimary` | `--avanue-text-primary` | #F8FAFC | #FFF7ED | #F5F5F7 |
| textSecondary | `.textSecondary` | `--avanue-text-secondary` | #94A3B8 | #D4A27A | #98989D |
| error | `.error` | `--avanue-error` | #EF4444 | #EF4444 | #FF453A |
| success | `.success` | `--avanue-success` | #10B981 | #10B981 | #30D158 |
| warning | `.warning` | `--avanue-warning` | #F59E0B | #F59E0B | #FFD60A |
| border | `.border` | `--avanue-border` | #334155 | #3D2B43 | #38383A |
| borderSubtle | `.borderSubtle` | `--avanue-border-subtle` | #1E293B | #2D1B33 | #2C2C2E |

### Responsive Breakpoints

| Breakpoint | Kotlin | CSS | Value |
|-----------|--------|-----|-------|
| Compact max | `ResponsiveTokens.compactMax` | `max-width: 599px` | 599dp |
| Medium min | `ResponsiveTokens.mediumMin` | `min-width: 600px` | 600dp |
| Expanded min | `ResponsiveTokens.expandedMin` | `min-width: 840px` | 840dp |
| Large min | `ResponsiveTokens.largeMin` | `min-width: 1240px` | 1240dp |
| Extra Large min | `ResponsiveTokens.extraLargeMin` | `min-width: 1440px` | 1440dp |

---

## 3. Theme Variants & MaterialMode

### Kotlin/Compose

```kotlin
// Use AvanueThemeVariant for full configuration:
import com.augmentalis.avanueui.theme.AvanueThemeVariant

val variant = AvanueThemeVariant.LIQUID  // or OCEAN, SUNSET
AvanueThemeProvider(
    colors = variant.colors,
    glass = variant.glass,
    water = variant.water,
    materialMode = variant.materialMode,
    displayProfile = DisplayProfile.PHONE
) {
    AppContent()
}
```

### TypeScript/CSS

```typescript
// MaterialMode type (add to your types)
type MaterialMode = 'glass' | 'water' | 'plain';

// Theme variant configuration
interface AvanueThemeVariant {
  name: string;
  materialMode: MaterialMode;
  colors: AvanueColorScheme;
  glass: AvanueGlassScheme;
  water: AvanueWaterScheme;
}

const OCEAN: AvanueThemeVariant = {
  name: 'ocean',
  materialMode: 'glass',
  colors: OceanColors,
  glass: OceanGlass,
  water: OceanWater,
};

const LIQUID: AvanueThemeVariant = {
  name: 'liquid',
  materialMode: 'water',
  colors: LiquidColors,
  glass: LiquidGlass,
  water: LiquidWater,
};
```

### CSS Classes for MaterialMode

```css
/* Apply on <html> or root container */
.material-mode-glass .avanue-surface {
  backdrop-filter: blur(var(--glass-medium-blur));
  background: rgba(255,255,255, var(--glass-medium-opacity));
  border: 1px solid rgba(255,255,255, 0.20);
}

.material-mode-water .avanue-surface {
  backdrop-filter: blur(var(--water-blur-regular));
  background: rgba(255,255,255, var(--water-overlay-regular));
  border: 0.5px solid;
  border-image: linear-gradient(
    to bottom,
    rgba(255,255,255,0.30),
    rgba(255,255,255,0.10)
  ) 1;
  /* Add shimmer animation for interactive elements */
}

.material-mode-plain .avanue-surface {
  background: hsl(var(--avanue-surface));
  border: 1px solid hsl(var(--avanue-border));
}
```

---

## 4. Kotlin/Compose Integration

### Setup (build.gradle.kts)

```kotlin
dependencies {
    implementation(project(":Modules:AvanueUI"))
}
```

### Theme Provider (Activity/App root)

```kotlin
import com.augmentalis.avanueui.theme.*
import com.augmentalis.avanueui.display.DisplayProfile

@Composable
fun AppRoot() {
    val variant = AvanueThemeVariant.LIQUID // or from user preferences
    AvanueThemeProvider(
        colors = variant.colors,
        glass = variant.glass,
        water = variant.water,
        materialMode = variant.materialMode,
        displayProfile = DisplayProfile.PHONE
    ) {
        // Your app content here
        // AvanueTheme.colors, AvanueTheme.materialMode, etc. are now available
    }
}
```

### Using Unified Components

```kotlin
import com.augmentalis.avanueui.components.*
import com.augmentalis.avanueui.tokens.SpacingTokens

// Card
AvanueCard(onClick = { /* action */ }) {
    Text(
        "Hello World",
        color = AvanueTheme.colors.textPrimary,
        modifier = Modifier.padding(SpacingTokens.md)
    )
}

// Button
AvanueButton(onClick = { }) {
    Text("Click Me")
}

// Surface (for panels, headers, overlays)
AvanueSurface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(ShapeTokens.lg)
) {
    // content
}

// Chip
AvanueChip(
    onClick = { },
    label = { Text("Filter") },
    leadingIcon = { Icon(Icons.Default.Star, null) }
)

// FAB
AvanueFAB(onClick = { }) {
    Icon(Icons.Default.Add, "Add")
}

// Icon Button
AvanueIconButton(onClick = { }) {
    Icon(Icons.Default.Settings, "Settings")
}

// Chat Bubble
AvanueBubble(
    align = BubbleAlign.START,  // or END, CENTER
    modifier = Modifier.fillMaxWidth(0.8f)
) {
    Text("Message text", color = AvanueTheme.colors.textPrimary)
}
```

### Using Tokens Directly

```kotlin
import com.augmentalis.avanueui.tokens.*

// Spacing
Modifier.padding(SpacingTokens.md)           // 16.dp
Modifier.padding(horizontal = SpacingTokens.lg)  // 24.dp

// Shape
RoundedCornerShape(ShapeTokens.md)            // 12.dp corners
RoundedCornerShape(ShapeTokens.full)          // pill shape

// Colors (always from theme, never hardcoded)
Text(color = AvanueTheme.colors.textPrimary)
Box(Modifier.background(AvanueTheme.colors.surface))

// Background gradient (standard pattern)
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    AvanueTheme.colors.background,
                    AvanueTheme.colors.surface.copy(alpha = 0.6f),
                    AvanueTheme.colors.background
                )
            )
        )
)
```

### Low-Level Effects (power users only)

```kotlin
import com.augmentalis.avanueui.glass.GlassLevel
import com.augmentalis.avanueui.water.WaterLevel

// Glass effect on any composable
Box(
    modifier = Modifier
        .glass(
            backgroundColor = AvanueTheme.colors.surface,
            glassLevel = GlassLevel.MEDIUM
        )
)

// Water effect on any composable
Box(
    modifier = Modifier
        .waterEffect(
            backgroundColor = AvanueTheme.colors.surface,
            waterLevel = WaterLevel.REGULAR
        )
)
```

---

## 5. TypeScript/CSS Integration

### Token TypeScript Definitions

Projects consuming AvanueUI on web need these token objects. Both SmartFin and AvanueCentral have v3.1 implementations — below is what v4.0 adds.

#### New: WaterTokens (add to existing token file)

```typescript
export const WaterTokens = {
  // Blur per level
  blurRegular: '12px',
  blurClear: '4px',
  blurIdentity: '0px',

  // Refraction (SVG feDisplacementMap scale)
  refractionRegular: 4,
  refractionClear: 2,
  refractionFrequency: 0.03,

  // Overlay opacity
  overlayRegular: 0.14,
  overlayClear: 0.06,
  overlayIdentity: 0.0,

  // Specular highlight
  specularRadius: '80px',
  specularIntensity: 0.40,
  specularFalloff: 2.0,

  // Interactive behavior
  pressScaleFactor: 0.96,
  pressScaleDuration: '100ms',
  shimmerDuration: '2000ms',

  // Caustics
  causticSpeed: 1.5,
  causticIntensity: 0.12,
  causticScale: 0.02,

  // Border
  borderWidth: '0.5px',
  borderTopOpacity: 0.30,
  borderBottomOpacity: 0.10,

  // Morph
  morphDuration: '400ms',
} as const;
```

#### New: MaterialMode Type

```typescript
export type MaterialMode = 'glass' | 'water' | 'plain';

export interface AvanueThemeConfig {
  variant: 'ocean' | 'sunset' | 'liquid';
  materialMode: MaterialMode;
  appearance: 'light' | 'dark' | 'system';
}

// Variant → MaterialMode mapping
export const VARIANT_MODE_MAP: Record<string, MaterialMode> = {
  ocean: 'glass',
  sunset: 'glass',
  liquid: 'water',
};
```

#### New: Liquid Color Scheme (add alongside Ocean/Sunset)

```typescript
export const LiquidColors = {
  primary: '#00D4FF',
  onPrimary: '#000000',
  primaryDark: '#009DC0',
  primaryLight: '#66E5FF',
  secondary: '#A78BFA',
  onSecondary: '#000000',
  tertiary: '#34D399',
  background: '#000000',
  surface: '#1C1C1E',
  surfaceElevated: '#2C2C2E',
  surfaceVariant: '#3A3A3C',
  textPrimary: '#F5F5F7',
  textSecondary: '#98989D',
  textTertiary: '#636366',
  error: '#FF453A',
  success: '#30D158',
  warning: '#FFD60A',
  info: '#64D2FF',
  border: '#38383A',
  borderSubtle: '#2C2C2E',
  borderStrong: '#48484A',
} as const;

export const LiquidGlass = {
  overlayColor: '#FFFFFF',
  tintColor: '#000000',
  shadowColor: '#000000',
  glowColor: '#00D4FF',
  glassBorderTint: 'rgba(0,212,255,0.15)',
} as const;

export const LiquidWater = {
  highlightColor: 'rgba(0,212,255,0.25)',
  causticColor: 'rgba(167,139,250,0.12)',
  refractionTint: 'rgba(0,212,255,0.08)',
  depthShadowColor: 'rgba(0,0,0,0.30)',
  surfaceTint: 'rgba(0,212,255,0.06)',
  borderTint: 'rgba(0,212,255,0.20)',
} as const;
```

#### New: CSS Custom Properties for Water

```css
:root {
  /* Water effect tokens */
  --water-blur-regular: 12px;
  --water-blur-clear: 4px;
  --water-overlay-regular: 0.14;
  --water-overlay-clear: 0.06;
  --water-refraction-regular: 4;
  --water-refraction-clear: 2;
  --water-border-width: 0.5px;
  --water-press-scale: 0.96;
  --water-shimmer-duration: 2000ms;
  --water-morph-duration: 400ms;
}
```

### CSS Utility Classes

```css
/* Glass surface (existing — keep as is) */
.glass-light {
  backdrop-filter: blur(6px);
  background: rgba(255,255,255,0.10);
  border: 1px solid rgba(255,255,255,0.20);
  border-radius: var(--radius-md);
}
.glass-medium {
  backdrop-filter: blur(8px);
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.20);
  border-radius: var(--radius-md);
}
.glass-heavy {
  backdrop-filter: blur(10px);
  background: rgba(255,255,255,0.22);
  border: 1px solid rgba(255,255,255,0.20);
  border-radius: var(--radius-md);
}

/* Water surface (NEW in v4.0) */
.water-regular {
  backdrop-filter: blur(12px);
  background: rgba(255,255,255,0.14);
  border: 0.5px solid transparent;
  border-image: linear-gradient(
    to bottom,
    rgba(255,255,255,0.30),
    rgba(255,255,255,0.10)
  ) 1;
  border-radius: var(--radius-md);
  transition: transform var(--water-press-scale-duration, 100ms) ease;
}
.water-regular:active {
  transform: scale(0.96);
}
.water-clear {
  backdrop-filter: blur(4px);
  background: rgba(255,255,255,0.06);
  border: 0.5px solid rgba(255,255,255,0.15);
  border-radius: var(--radius-md);
}

/* Shimmer animation for water interactive elements */
@keyframes water-shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.water-interactive:hover {
  background-image: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255,255,255,0.08) 50%,
    transparent 100%
  );
  background-size: 200% 100%;
  animation: water-shimmer 2s infinite;
}
```

### React Unified Component Pattern

```tsx
// AvanueCard.tsx — unified React component
import { useTheme } from '@/providers/theme-provider';

interface AvanueCardProps {
  onClick?: () => void;
  className?: string;
  children: React.ReactNode;
}

export function AvanueCard({ onClick, className, children }: AvanueCardProps) {
  const { materialMode } = useTheme();

  const modeClass = {
    glass: 'glass-medium',
    water: 'water-regular water-interactive',
    plain: 'bg-surface border border-border',
  }[materialMode];

  return (
    <div
      className={cn(
        'rounded-avanue-md p-avanue-md',
        modeClass,
        onClick && 'cursor-pointer',
        className
      )}
      onClick={onClick}
    >
      {children}
    </div>
  );
}
```

---

## 6. SmartFin — What To Do

**Current state:** AvanueUI v3.1 integrated (Ocean + Sunset themes, glass effects, all tokens)
**Target:** AvanueUI v4.0 (add Water effects, MaterialMode, Liquid theme)

### Files To Update

| File | Action | Priority |
|------|--------|----------|
| `client/src/lib/avanueui-tokens.ts` | Add WaterTokens, LiquidColors, LiquidGlass, LiquidWater, MaterialMode type | HIGH |
| `client/src/providers/new-theme-provider.tsx` | Add materialMode to context, map variants to modes | HIGH |
| `tailwind.config.ts` | Add water utility classes, liquid color variables | MEDIUM |
| `client/src/index.css` | Add `.water-*` classes, liquid theme variables, `@keyframes water-shimmer` | MEDIUM |
| `config.theme.json` | Add `"materialMode"` field (auto-derived from variant) | LOW |

### Step-by-Step

1. **Add WaterTokens to `avanueui-tokens.ts`**
   - Copy the `WaterTokens` const from Section 5 above
   - Add `LiquidColors`, `LiquidGlass`, `LiquidWater` consts
   - Add `MaterialMode` type and `VARIANT_MODE_MAP`
   - Export all new types

2. **Update theme provider**
   - Add `materialMode: MaterialMode` to theme context value
   - Derive mode from variant: `VARIANT_MODE_MAP[variant] || 'glass'`
   - Apply `material-mode-${mode}` class to document root
   - Expose via `useTheme()` hook

3. **Add CSS utility classes**
   - Add `.water-regular`, `.water-clear`, `.water-interactive` to globals.css
   - Add `@keyframes water-shimmer`
   - Add `[data-theme="liquid"]` color variables

4. **Create unified React components** (optional, recommended)
   - `AvanueCard`, `AvanueSurface`, `AvanueButton` that read `materialMode`
   - Switch between glass/water/plain CSS classes based on mode
   - Replace direct `glass-medium` usage with unified components

### SmartFin config.theme.json Update

```json
{
  "system": "avanueui",
  "variant": "liquid",
  "appearance": "system",
  "radius": 1.0,
  "glass": "medium",
  "materialMode": "water"
}
```

---

## 7. AvanueCentral — What To Do

**Current state:** AvanueUI v3.1 integrated (Ocean + Sunset themes, glass effects, shadcn components)
**Target:** AvanueUI v4.0 (add Water effects, MaterialMode, Liquid theme)

### Files To Update

| File | Action | Priority |
|------|--------|----------|
| `packages/web/AvanueUI/tokens/water.ts` | CREATE — WaterTokens + LiquidWater scheme | HIGH |
| `packages/web/AvanueUI/tokens/liquid.ts` | CREATE — LiquidColors + LiquidGlass | HIGH |
| `packages/web/AvanueUI/tokens/index.ts` | Add exports for water.ts, liquid.ts | HIGH |
| `packages/web/AvanueUI/tokens/presets.ts` | Add Liquid preset | HIGH |
| `packages/web/app/globals.css` | Add `.water-*` classes, `[data-theme="liquid"]` variables | MEDIUM |
| `packages/web/contexts/ThemeContext.tsx` | Add `materialMode` to context | MEDIUM |
| `packages/web/tailwind.config.ts` | Add water blur/border utilities | LOW |

### Step-by-Step

1. **Create `tokens/water.ts`**
   ```typescript
   export const water = {
     level: {
       regular: { blur: '12px', overlay: 0.14, refraction: 4 },
       clear:   { blur: '4px',  overlay: 0.06, refraction: 2 },
       identity:{ blur: '0px',  overlay: 0.0,  refraction: 0 },
     },
     interactive: {
       pressScale: 0.96,
       pressScaleDuration: '100ms',
       shimmerDuration: '2000ms',
     },
     border: {
       width: '0.5px',
       topOpacity: 0.30,
       bottomOpacity: 0.10,
     },
     caustic: { speed: 1.5, intensity: 0.12, scale: 0.02 },
     morph: { duration: '400ms' },
   } as const;

   export type WaterLevel = keyof typeof water.level;
   ```

2. **Create `tokens/liquid.ts`**
   - LiquidColors (primary=#00D4FF, background=#000000, surface=#1C1C1E)
   - LiquidGlass (overlayColor=#FFFFFF, glowColor=#00D4FF)
   - LiquidWater (highlightColor, causticColor, etc.)

3. **Update `tokens/index.ts`**
   ```typescript
   export * from './water'
   export * from './liquid'
   ```

4. **Update `presets.ts`** — add Liquid preset alongside Ocean and Sunset

5. **Add CSS variables and utility classes** — same as SmartFin section

6. **Create unified React components** (optional)
   - Wrap existing GlassCard etc. with mode-switching logic
   - Or create new `AvanueCard` component that delegates

### Glass Components to Add Water Support

The existing AvanueCentral glass components (`GlassCard`, `GlassButton`, etc.) should become unified:

| Current | Action |
|---------|--------|
| `GlassCard` | Rename to `AvanueCard`, add water/plain branches |
| `GlassButton` | Rename to `AvanueButton`, add water/plain branches |
| `GlassSurface` | Rename to `AvanueSurface`, add water/plain branches |
| `GlassIconButton` | Rename to `AvanueIconButton`, add water/plain branches |
| `GlassChip` | Rename to `AvanueChip`, add water/plain branches |
| `GlassFAB` | Rename to `AvanueFAB`, add water/plain branches |
| `GlassBubble` | Rename to `AvanueBubble`, add water/plain branches |

Keep old Glass* names as re-exports for backward compat.

---

## 8. NewAvanues — What To Do

### 8A. apps/avanues (Main App) — 78% Compliant

**4 files need MaterialTheme.colorScheme → AvanueTheme.colors migration:**

| File | Violations | Fix |
|------|-----------|-----|
| `ui/settings/GlassesSettingsLayout.kt` | 5 × `MaterialTheme.colorScheme.*` | Replace with `AvanueTheme.colors.*` |
| `ui/developer/DeveloperConsoleScreen.kt` | 1 × `MaterialTheme.colorScheme.onSurfaceVariant` | Replace with `AvanueTheme.colors.textSecondary` |
| `ui/about/AboutScreen.kt` | 1 × `MaterialTheme.colorScheme.primary` | Replace with `AvanueTheme.colors.primary` |
| `MainActivity.kt` | `MaterialTheme.colorScheme.*` in theme setup | Ensure AvanueThemeProvider wraps everything |

**Migration mapping:**

| MaterialTheme.colorScheme.X | AvanueTheme.colors.X |
|----------------------------|---------------------|
| `.primary` | `.primary` |
| `.onPrimary` | `.onPrimary` |
| `.surface` | `.surface` |
| `.surfaceContainerHigh` | `.surfaceElevated` |
| `.onSurface` | `.textPrimary` |
| `.onSurfaceVariant` | `.textSecondary` |
| `.error` | `.error` |
| `.outline` | `.border` |
| `.outlineVariant` | `.borderSubtle` |

### 8B. Modules/VoiceOSCore — 0% Compliant (Parallel ARVisionTheme)

**Problem:** VoiceOSCore has its own `ARVisionTheme` with hardcoded "vibrant" colors and custom glassmorphism utils.

**Strategy:** VoiceOSCore is a library module that renders its own UI (HUD overlays, command testing panels). It should consume AvanueUI tokens.

**Files to migrate (10+):**

| File | Current | Target |
|------|---------|--------|
| `managers/hudmanager/ui/ARVisionTheme.kt` | Custom theme object | DELETE — use AvanueTheme |
| `managers/hudmanager/ui/GlassmorphismUtils.kt` | Custom glass util | DELETE — use `Modifier.glass()` |
| `managers/commandmanager/ui/CommandManagerActivity.kt` | MaterialTheme.colorScheme | AvanueTheme.colors |
| `managers/commandmanager/ui/CommandTestingPanel.kt` | MaterialTheme.colorScheme | AvanueTheme.colors |
| `managers/localizationmanager/ui/LocalizationManagerActivity.kt` | MaterialTheme.colorScheme | AvanueTheme.colors |
| `managers/localizationmanager/ui/GlassmorphismUtils.kt` | Duplicate custom glass | DELETE |
| `accessibility/Enhancer.kt` | Hardcoded colors | AvanueTheme.colors |
| `VoiceIndicatorSystem.kt` | Hardcoded VibrantBlue etc. | AvanueTheme.colors.primary etc. |

**Color mapping for VoiceOSCore vibrant palette:**

| VoiceOSCore Color | AvanueTheme Equivalent |
|-------------------|----------------------|
| VibrantBlue (#007AFF) | `AvanueTheme.colors.primary` (theme-dependent) |
| VibrantGreen (#34C759) | `AvanueTheme.colors.success` |
| VibrantOrange (#FF9500) | `AvanueTheme.colors.warning` |
| VibrantRed (#FF3B30) | `AvanueTheme.colors.error` |
| VibrantPurple (#AF52DE) | `AvanueTheme.colors.tertiary` |

**Prerequisite:** VoiceOSCore's `build.gradle.kts` must add `implementation(project(":Modules:AvanueUI"))`.

### 8C. Modules/AI/RAG — 0% Compliant (Vanilla Material3)

**Problem:** All RAG UI screens use `MaterialTheme.colorScheme` and hardcoded `Color.White`.

**Files to migrate (4 UI files):**

| File | Current | Target |
|------|---------|--------|
| `RAGChatScreen.kt` | MaterialTheme + Color.White | AvanueTheme.colors + AvanueCard |
| `RAGSearchScreen.kt` | MaterialTheme | AvanueTheme.colors |
| `DocumentManagementScreen.kt` | MaterialTheme | AvanueTheme.colors + AvanueCard |
| Custom gradient background | `Modifier.gradientBackground()` | Standard AvanueUI gradient pattern |

**Prerequisite:** RAG module's `build.gradle.kts` must add `implementation(project(":Modules:AvanueUI"))`.

### 8D. Modules/AVA/Overlay — Parallel OceanGlassColors

**Problem:** Overlay module has its own `OceanGlassColors` object that duplicates AvanueUI glass tokens.

**Files to migrate:**

| File | Action |
|------|--------|
| `VoiceOrb.kt` | Replace OceanGlassColors with AvanueTheme.glass/colors |
| `GlassMorphicPanel.kt` | Use AvanueUI `Modifier.glass()` |
| `SuggestionChips.kt` | Use `AvanueChip` or `Modifier.glass()` |

### 8E. Modules/LicenseManager — Custom GlassmorphismUtils

**Problem:** Hardcoded status colors and custom glass utilities.

**Action:** Delete `GlassmorphismUtils.kt`, use AvanueTheme.colors for status colors:
- StatusActive → `AvanueTheme.colors.success`
- StatusWarning → `AvanueTheme.colors.warning`
- StatusError → `AvanueTheme.colors.error`
- StatusInfo → `AvanueTheme.colors.info`

---

## 9. Compliance Audit Results

### Per-Project Summary (2026-02-10)

| Project | Platform | AvanueUI Version | Compliance | Action Needed |
|---------|----------|------------------|------------|---------------|
| **SmartFin** | React+Tauri | v3.1 | 85% | Add v4.0 water/liquid tokens |
| **AvanueCentral** | Next.js+React | v3.1 | 85% | Add v4.0 water/liquid tokens |
| **NewAvanues/apps/avanues** | Compose | v4.0 | 78% | Fix 4 MaterialTheme violations |
| **NewAvanues/VoiceOSCore** | Compose | None | 0% | Full migration needed |
| **NewAvanues/RAG** | Compose | None | 0% | Full migration needed |
| **NewAvanues/Overlay** | Compose | Parallel system | 0% | Replace OceanGlassColors |
| **NewAvanues/LicenseManager** | Compose | None | 0% | Replace GlassmorphismUtils |

### Priority Order

1. **SmartFin + AvanueCentral** (v3.1 → v4.0) — token additions only, no breaking changes
2. **NewAvanues/apps/avanues** — 4 quick fixes, already 78% compliant
3. **NewAvanues/VoiceOSCore** — largest migration, most UI files
4. **NewAvanues/RAG** — 4 screens, straightforward replacement
5. **NewAvanues/Overlay** — 3 files, delete OceanGlassColors
6. **NewAvanues/LicenseManager** — 1 file, delete GlassmorphismUtils

---

## 10. Rules & Anti-Patterns

### MUST DO

- Use `AvanueTheme.colors.*` for ALL runtime colors (never `MaterialTheme.colorScheme.*`)
- Use `SpacingTokens.*` for ALL padding/margin (never hardcoded dp values)
- Use `ShapeTokens.*` for ALL corner radii
- Use unified `Avanue*` components (never `Glass*` or `Water*` directly)
- Wrap app root with `AvanueThemeProvider`
- Use CSS variables (`var(--avanue-*)`) on web (never hardcoded hex)

### MUST NOT

- Never use `Color(0xFF...)` hardcoded colors in UI code
- Never use `MaterialTheme.colorScheme.*` — always `AvanueTheme.colors.*`
- Never choose `GlassCard` vs `WaterCard` — use `AvanueCard`
- Never skip `AvanueThemeProvider` wrapping
- Never create parallel theme objects (ARVisionTheme, OceanGlassColors, GlassmorphismUtils)
- Never use `com.avanueui.*` imports (deleted package)
- Never use `:Modules:AvanueUI:DesignSystem` or `:Modules:AvanueUI:Foundation` (merged)

### Background Gradient Pattern (Standard for all screens)

**Kotlin:**
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(
            Brush.verticalGradient(
                listOf(
                    AvanueTheme.colors.background,
                    AvanueTheme.colors.surface.copy(alpha = 0.6f),
                    AvanueTheme.colors.background
                )
            )
        )
)
```

**CSS:**
```css
.screen-background {
  background: linear-gradient(
    to bottom,
    hsl(var(--avanue-background)),
    hsl(var(--avanue-surface) / 0.6),
    hsl(var(--avanue-background))
  );
  min-height: 100vh;
}
```

### TopAppBar Pattern

**Kotlin:**
```kotlin
TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,  // or AvanueTheme.colors.surface
        titleContentColor = AvanueTheme.colors.textPrimary,
        navigationIconContentColor = AvanueTheme.colors.iconPrimary,
        actionIconContentColor = AvanueTheme.colors.iconPrimary
    )
)
```

---

## Appendix: File Locations

### Kotlin Source of Truth
```
NewAvanues/Modules/AvanueUI/src/commonMain/kotlin/com/augmentalis/avanueui/
├── tokens/          → SpacingTokens, ShapeTokens, etc.
├── theme/           → AvanueTheme, OceanColors, LiquidColors, etc.
├── components/      → AvanueSurface, AvanueCard, etc.
├── glass/           → Modifier.glass(), GlassLevel, GlassDefaults
├── water/           → Modifier.waterEffect(), WaterLevel, WaterDefaults
└── display/         → DisplayProfile, DisplayUtils
```

### SmartFin Token File
```
SmartFin/client/src/lib/avanueui-tokens.ts
```

### AvanueCentral Token Files
```
AvanueCentral/packages/web/AvanueUI/tokens/
├── colors.ts, spacing.ts, typography.ts, shadows.ts
├── radius.ts, animations.ts, breakpoints.ts, glass.ts
├── ocean.ts, sunset.ts, gradients.ts, presets.ts
├── water.ts        ← CREATE (v4.0)
├── liquid.ts       ← CREATE (v4.0)
└── index.ts        ← UPDATE (add water, liquid exports)
```

### Web Renderer Types
```
NewAvanues/Modules/AvanueUI/Renderers/Web/src/types/index.ts
  → WaterLevel, WaterEffect, WaterScheme types already defined
```

### Documentation
```
NewAvanues/Docs/MasterDocs/AvanueUI/
├── Developer-Manual-Chapter91-AvanueUI-DesignSystem-Guide.md
├── Developer-Manual-Chapter92-AvanueUI-Phase2-UnifiedComponents.md
├── AvanueUI-Component-Reference-260210-V1.md
└── AvanueUI-Adoption-Guide-260210-V1.md          ← THIS DOCUMENT
```

---

*AvanueUI v4.0 | Avanues EcoSystem | 2026-02-10*
