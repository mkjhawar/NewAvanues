# AvanueUI Component Reference — v4.0

**Standalone reference for developers and AI building UI in the Avanues ecosystem.**
**Date:** 2026-02-10 | **Module:** `:Modules:AvanueUI`

---

## TL;DR

AvanueUI is a **theme-driven** design system. Write UI with **unified components** — the active `MaterialMode` (GLASS / WATER / PLAIN) controls the visual rendering. You never choose between glass and water manually.

```kotlin
// ONE line — theme handles the rest:
AvanueCard(onClick = { }) { Text("Hello") }
```

---

## Setup

### Dependency
```kotlin
// build.gradle.kts
implementation(project(":Modules:AvanueUI"))
```

### Theme Provider (App Root)
```kotlin
import com.augmentalis.avanueui.theme.AvanueThemeProvider
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.avanueui.theme.OceanColors
import com.augmentalis.avanueui.theme.OceanGlass
import com.augmentalis.avanueui.display.DisplayProfile

AvanueThemeProvider(
    colors = OceanColors,
    glass = OceanGlass,
    materialMode = MaterialMode.GLASS,
    displayProfile = DisplayProfile.PHONE
) {
    AppContent()
}
```

---

## MaterialMode

Controls how all unified components render:

| Mode | Effect | Description |
|------|--------|-------------|
| `GLASS` | Frosted glass overlay | Blur + translucent surface + highlight border |
| `WATER` | Liquid refraction | Blur + caustic shimmer + specular highlight + refraction |
| `PLAIN` | Standard Material3 | AvanueTheme colors, no effects |

**Access:** `AvanueTheme.materialMode`

---

## Unified Components

All imports from: `com.augmentalis.avanueui.components.*`

### AvanueSurface
Base interactive surface. Use for panels, headers, overlays.
```kotlin
AvanueSurface(
    onClick = { },               // optional — null = non-interactive
    modifier = Modifier,
    shape = GlassDefaults.shape, // RoundedCornerShape(12.dp)
    color = AvanueTheme.colors.surface,
    contentColor = AvanueTheme.colors.textPrimary
) {
    // content
}
```

### AvanueCard
Card container with column content scope.
```kotlin
AvanueCard(
    onClick = { },               // optional
    modifier = Modifier,
    shape = GlassDefaults.shape
) {
    // ColumnScope — items stack vertically
    Text("Title")
    Text("Body")
}
```

### AvanueButton
Primary action button.
```kotlin
AvanueButton(
    onClick = { submit() },
    modifier = Modifier,
    enabled = true,
    shape = GlassDefaults.shape,
    contentPadding = ButtonDefaults.ContentPadding
) {
    // RowScope
    Icon(Icons.Default.Send, null)
    Spacer(Modifier.width(8.dp))
    Text("Submit")
}
```

### AvanueChip
Filter/tag chip.
```kotlin
AvanueChip(
    onClick = { toggle() },
    label = { Text("Active") },
    modifier = Modifier,
    enabled = true,
    leadingIcon = { Icon(Icons.Default.Check, null) },
    trailingIcon = null,
    shape = GlassShapes.chipShape
)
```

### AvanueBubble
Chat message bubble.
```kotlin
AvanueBubble(
    modifier = Modifier,
    align = BubbleAlign.END,     // START (incoming), END (outgoing), CENTER (system)
    shape = GlassShapes.bubbleEnd,
    color = AvanueTheme.colors.surface,
    contentColor = AvanueTheme.colors.textPrimary
) {
    Text("Hello!")
}
```

### AvanueFAB
Floating action button.
```kotlin
AvanueFAB(
    onClick = { addItem() },
    modifier = Modifier,
    shape = GlassShapes.fabShape
) {
    Icon(Icons.Default.Add, contentDescription = "Add")
}
```

### AvanueIconButton
Icon-only button.
```kotlin
AvanueIconButton(
    onClick = { toggleMenu() },
    modifier = Modifier,
    enabled = true
) {
    Icon(Icons.Default.Menu, contentDescription = "Menu")
}
```

---

## Design Tokens

All static objects — no `@Composable` needed. Import from `com.augmentalis.avanueui.tokens.*`.

### SpacingTokens (8dp grid)
| Token | Value | Use |
|-------|-------|-----|
| `xxs` | 2dp | Tight inline |
| `xs` | 4dp | Icon gap |
| `sm` | 8dp | Compact padding |
| `md` | **16dp** | Default padding |
| `lg` | 24dp | Section spacing |
| `xl` | 32dp | Large gaps |
| `xxl` | 48dp | Major separators |
| `huge` | 64dp | Hero spacing |

### ShapeTokens (corner radius)
| Token | Value | Use |
|-------|-------|-----|
| `xs` | 4dp | Subtle |
| `sm` | 8dp | Chips |
| `md` | **12dp** | Default cards |
| `lg` | 16dp | Large cards |
| `xl` | 20dp | Extra rounding |
| `full` | 9999dp | Pill/circle |

### Other Tokens
- **SizeTokens** — `iconSm`=16dp, `iconMd`=24dp, `iconLg`=32dp, button heights, touch targets
- **ElevationTokens** — `xs`=1dp through `xxl`=16dp
- **AnimationTokens** — `fast`=100ms, `normal`=200ms, `medium`=300ms, `slow`=500ms
- **GlassTokens** — glass opacity, blur, border values per level
- **WaterTokens** — water blur, refraction, caustic, specular, shimmer values
- **ResponsiveTokens** — M3 breakpoints and grid config

---

## Theme Colors

Access via `AvanueTheme.colors.*` inside `@Composable` functions.

### Color Properties
| Property | Description |
|----------|-------------|
| `primary` | Brand primary |
| `onPrimary` / `textOnPrimary` | Text on primary |
| `secondary` | Brand secondary |
| `background` | Screen background |
| `surface` | Card/panel background |
| `surfaceElevated` | Elevated surface |
| `textPrimary` | Primary text |
| `textSecondary` | Secondary text |
| `error` | Error state |
| `success` | Success state |
| `warning` | Warning state |
| `divider` | Dividers |
| `outline` | Borders |

### Available Schemes
| Scheme | Style | Primary |
|--------|-------|---------|
| `OceanColors` | Cool blue | `#3B82F6` |
| `SunsetColors` | Warm coral | `#FF6B35` |

### Standard Background Pattern
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

---

## Low-Level Effects (Advanced)

For custom UI that doesn't fit unified components.

### Modifier.glass()
```kotlin
import com.augmentalis.avanueui.components.glass.glass
import com.augmentalis.avanueui.glass.GlassLevel

Box(
    modifier = Modifier
        .glass(
            backgroundColor = AvanueTheme.colors.surface,
            glassLevel = GlassLevel.MEDIUM,
            border = GlassDefaults.border,
            shape = RoundedCornerShape(12.dp)
        )
        .padding(16.dp)
)
```

### Modifier.waterEffect()
```kotlin
import com.augmentalis.avanueui.water.waterEffect
import com.augmentalis.avanueui.water.WaterLevel

Box(
    modifier = Modifier
        .waterEffect(
            backgroundColor = AvanueTheme.colors.surface,
            waterLevel = WaterLevel.REGULAR,
            shape = RoundedCornerShape(12.dp),
            interactive = true
        )
        .padding(16.dp)
)
```

### Glass Levels
| Level | Overlay | Blur | Use |
|-------|---------|------|-----|
| `LIGHT` | 10% | 6dp | Subtle panels |
| `MEDIUM` | 15% | 8dp | Default cards |
| `HEAVY` | 22% | 10dp | Modals |

### Water Levels
| Level | Blur | Refraction | Use |
|-------|------|------------|-----|
| `REGULAR` | 20dp | Full | Default surfaces |
| `CLEAR` | 10dp | Light | Lighter panels |
| `IDENTITY` | 0dp | None | No effect |

---

## Display Profiles

Access via `AvanueTheme.displayProfile`.

| Profile | Density | Font Scale | Touch Target |
|---------|---------|------------|-------------|
| `PHONE` | 1.0x | 1.0x | 48dp |
| `TABLET` | 1.0x | 1.0x | 48dp |
| `GLASS_MICRO` | 0.625x | 0.75x | 36dp |
| `GLASS_COMPACT` | 0.75x | 0.85x | 40dp |
| `GLASS_STANDARD` | 0.875x | 0.9x | 44dp |
| `GLASS_HD` | 0.9x | 0.95x | 48dp |

All dp/sp auto-scale — write values once, density override handles the rest.

```kotlin
val touchSize = DisplayUtils.minTouchTarget  // auto-adapts
val isGlass = DisplayUtils.isGlass           // true for any glass profile
```

---

## Unique Components (Not Unified)

These don't have glass/water variants and are used directly:

### PulseDot / StatusBadge
```kotlin
import com.augmentalis.avanueui.components.glass.PulseDot
import com.augmentalis.avanueui.components.glass.StatusBadge

PulseDot(state = ServiceState.Running, dotSize = 12.dp)
StatusBadge(state = ServiceState.Running)
```

### WaterNavigationBar
Unique morphing bottom nav (no glass equivalent):
```kotlin
import com.augmentalis.avanueui.components.water.WaterNavigationBar
```

---

## Settings Components

For building settings screens with the Unified Adaptive Settings architecture:

```kotlin
import com.augmentalis.avanueui.components.settings.*

SettingsSection(title = "General") {
    SettingsToggle(title = "Feature", checked = true, onCheckedChange = { })
    SettingsSlider(title = "Volume", value = 0.5f, onValueChange = { })
    SettingsDropdown(title = "Mode", options = listOf("A", "B"), selected = "A", onSelect = { })
    SettingsNavItem(title = "Advanced", onClick = { })
}
```

---

## Migration Quick Reference

### From Glass* → Unified
```
GlassSurface(glassLevel=...) { }  →  AvanueSurface { }
GlassCard(glassLevel=...) { }     →  AvanueCard { }
OceanButton(glass=true) { }       →  AvanueButton { }
GlassChip(glass=true) { }         →  AvanueChip { }
GlassBubble { }                   →  AvanueBubble { }
GlassFloatingActionButton { }     →  AvanueFAB { }
GlassIconButton(glass=true) { }   →  AvanueIconButton { }
```

### From Water* → Unified
```
WaterSurface(waterLevel=...) { }  →  AvanueSurface { }
WaterCard(waterLevel=...) { }     →  AvanueCard { }
WaterButton { }                   →  AvanueButton { }
```

### Parameters Removed (handled by theme)
- `glassLevel` / `waterLevel` — theme decides
- `glass = true/false` — theme decides
- `border` (GlassBorder/WaterBorder) — theme decides

### Parameters That Carry Over
- `onClick`, `modifier`, `shape`, `enabled`, `content`
- `contentPadding`, `leadingIcon`, `trailingIcon`, `label`

---

## Package Map

```
com.augmentalis.avanueui.components.*           ← UNIFIED (primary API)
com.augmentalis.avanueui.components.glass.*     ← Glass (deprecated)
com.augmentalis.avanueui.components.water.*     ← Water (deprecated)
com.augmentalis.avanueui.components.settings.*  ← Settings UI
com.augmentalis.avanueui.tokens.*               ← Static design tokens
com.augmentalis.avanueui.theme.*                ← Theme, colors, MaterialMode
com.augmentalis.avanueui.display.*              ← Display profiles
com.augmentalis.avanueui.glass.*                ← Glass types (GlassLevel, GlassBorder)
com.augmentalis.avanueui.water.*                ← Water types + Modifier.waterEffect()
```

---

## TypeScript/Web Equivalent

For web apps using the AvanueUI Web Renderer:

### Component Mapping
| Kotlin | React/TS |
|--------|----------|
| `AvanueSurface` | `<AvanueSurface>` with CSS class `avanue-surface--{mode}` |
| `AvanueCard` | `<AvanueCard>` with CSS class `avanue-card--{mode}` |
| `AvanueButton` | `<AvanueButton>` with CSS class `avanue-button--{mode}` |
| `AvanueChip` | `<AvanueChip>` with CSS class `avanue-chip--{mode}` |

### CSS Variables (Token Mapping)
```css
/* Spacing (matches SpacingTokens) */
--avanue-spacing-xs: 4px;
--avanue-spacing-sm: 8px;
--avanue-spacing-md: 16px;
--avanue-spacing-lg: 24px;
--avanue-spacing-xl: 32px;

/* Shape (matches ShapeTokens) */
--avanue-radius-sm: 8px;
--avanue-radius-md: 12px;
--avanue-radius-lg: 16px;
--avanue-radius-full: 9999px;

/* Glass effect */
--avanue-glass-blur: 8px;
--avanue-glass-overlay: rgba(var(--avanue-surface-rgb), 0.15);
--avanue-glass-border: rgba(255, 255, 255, 0.12);

/* Water effect */
--avanue-water-blur: 20px;
--avanue-water-overlay: rgba(173, 216, 230, 0.12);
--avanue-water-border: rgba(255, 255, 255, 0.3);
--avanue-water-shimmer-duration: 8000ms;
```

### Glass CSS Pattern
```css
.avanue-card--glass {
  background: var(--avanue-glass-overlay);
  backdrop-filter: blur(var(--avanue-glass-blur));
  -webkit-backdrop-filter: blur(var(--avanue-glass-blur));
  border: 1px solid var(--avanue-glass-border);
  border-radius: var(--avanue-radius-md);
}
```

### Water CSS Pattern
```css
.avanue-card--water {
  background: var(--avanue-water-overlay);
  backdrop-filter: blur(var(--avanue-water-blur));
  -webkit-backdrop-filter: blur(var(--avanue-water-blur));
  border: 1px solid var(--avanue-water-border);
  border-radius: var(--avanue-radius-md);
  position: relative;
  overflow: hidden;
}

/* Shimmer overlay */
.avanue-card--water::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    transparent 40%,
    rgba(255, 255, 255, 0.08) 50%,
    transparent 60%
  );
  background-size: 200% 200%;
  animation: water-shimmer var(--avanue-water-shimmer-duration) linear infinite;
}

@keyframes water-shimmer {
  0% { background-position: 200% 200%; }
  100% { background-position: -200% -200%; }
}
```

### MaterialMode in TS
```typescript
enum MaterialMode {
  GLASS = 'GLASS',
  WATER = 'WATER',
  PLAIN = 'PLAIN'
}

// Read from theme context
const { materialMode } = useAvanueTheme();
```

---

## Rules

1. **Always use unified components** (`AvanueCard`, not `GlassCard`)
2. **Always use `AvanueTheme.colors.*`** (never `MaterialTheme.colorScheme.*`)
3. **Never hardcode colors** — use tokens or theme properties
4. **Use SpacingTokens for all padding/margins** (8dp grid system)
5. **Use ShapeTokens for corner radii** (never hardcode dp for corners)
6. **TopAppBar containerColor** = `Color.Transparent` or `AvanueTheme.colors.surface`
7. **Touch targets** = `DisplayUtils.minTouchTarget` (never hardcode 48dp)

---

*AvanueUI v4.0 | 2026-02-10 | Branch: 060226-1-consolidation-framework*
*For full architecture details: Chapter 91 (DesignSystem Guide) + Chapter 92 (Phase 2 Unified)*
