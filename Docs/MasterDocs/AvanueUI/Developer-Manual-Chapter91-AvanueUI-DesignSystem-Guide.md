# Developer Manual — Chapter 91: AvanueUI Design System Guide

**Date:** 2026-02-08 | **Branch:** `060226-1-consolidation-framework` | **Version:** 1.0

---

## Overview

AvanueUI is the unified design system for the Avanues ecosystem. As of v3.1, all design tokens, themes, display profiles, glass components, and settings UI are consolidated into a single Gradle module: `:Modules:AvanueUI`.

This chapter covers everything you need to build UIs with AvanueUI — from adding the dependency to creating custom themes and glass effects.

---

## 1. Module Structure (Post-Consolidation)

```
Modules/AvanueUI/src/
├── commonMain/kotlin/com/augmentalis/avanueui/
│   ├── tokens/            ← 9 static token files
│   │   ├── SpacingTokens.kt
│   │   ├── ShapeTokens.kt
│   │   ├── SizeTokens.kt
│   │   ├── ElevationTokens.kt
│   │   ├── AnimationTokens.kt
│   │   ├── GlassTokens.kt
│   │   ├── ResponsiveTokens.kt
│   │   ├── TypographyTokens.kt
│   │   └── TokenResolver.kt
│   ├── theme/             ← Theme colors, glass, M3 integration
│   │   ├── AvanueTheme.kt          (AvanueThemeProvider + AvanueTheme facade)
│   │   ├── AvanueColorScheme.kt    (interface + resolve())
│   │   ├── AvanueGlassScheme.kt    (interface)
│   │   ├── OceanColors.kt          (cool blue palette)
│   │   ├── OceanGlass.kt           (blue glass effects)
│   │   ├── SunsetColors.kt         (warm coral palette)
│   │   └── SunsetGlass.kt          (coral glass effects)
│   ├── display/           ← Responsive UI profiles
│   │   ├── DisplayProfile.kt       (6 profiles: PHONE/TABLET/4x GLASS)
│   │   ├── DisplayProfileResolver.kt (pure-function detection)
│   │   └── DisplayUtils.kt         (composable utilities)
│   ├── glass/             ← Glass type definitions
│   │   ├── GlassLevel.kt           (LIGHT/MEDIUM/HEAVY)
│   │   └── GlassBorder.kt          (border configuration)
│   └── components/
│       ├── glass/         ← Glass UI components
│       │   ├── GlassExtensions.kt       (Modifier.glass(), GlassShapes, GlassDefaults)
│       │   ├── GlassmorphicComponents.kt (GlassSurface, GlassCard, OceanButton, etc.)
│       │   ├── GlassmorphismCore.kt      (GlassMorphismConfig, GlassPresets)
│       │   ├── PulseDot.kt              (PulseDot, StatusBadge)
│       │   └── ComponentProvider.kt      (IconVariant, ButtonVariant)
│       └── settings/      ← Settings screen components
│           └── SettingsComponents.kt     (SettingsSection, SettingsToggle, etc.)
├── androidMain/kotlin/com/augmentalis/avanueui/
│   └── overlay/
│       ├── MagicCommandOverlay.kt
│       └── MagicCommandOverlayExample.kt
├── iosMain/...
└── desktopMain/...
```

**Single dependency in `build.gradle.kts`:**
```kotlin
implementation(project(":Modules:AvanueUI"))
```

---

## 2. Token System

All tokens are **static objects** — no `@Composable` annotation needed. Import and use anywhere.

### 2.1 SpacingTokens (8dp grid)

```kotlin
import com.augmentalis.avanueui.tokens.SpacingTokens

Column(modifier = Modifier.padding(SpacingTokens.md)) {  // 16dp
    Text("Content", modifier = Modifier.padding(bottom = SpacingTokens.sm))  // 8dp
}
```

| Token | Value | Use Case |
|-------|-------|----------|
| `xxs` | 2dp | Tight inline |
| `xs` | 4dp | Icon-to-text gap |
| `sm` | 8dp | Compact padding |
| `md` | 16dp | **Default** padding |
| `lg` | 24dp | Section spacing |
| `xl` | 32dp | Large gaps |
| `xxl` | 48dp | Major separators |
| `huge` | 64dp | Hero spacing |

### 2.2 ShapeTokens (corner radius)

```kotlin
import com.augmentalis.avanueui.tokens.ShapeTokens

Box(modifier = Modifier.clip(RoundedCornerShape(ShapeTokens.md)))  // 12dp
```

| Token | Value | Use Case |
|-------|-------|----------|
| `xs` | 4dp | Subtle rounding |
| `sm` | 8dp | Chips, small cards |
| `md` | 12dp | **Default** cards |
| `lg` | 16dp | Large cards |
| `xl` | 20dp | Extra rounding |
| `full` | 9999dp | Pill/circle |

### 2.3 Other Static Tokens

- **SizeTokens** — icon sizes (`iconSm`=16, `iconMd`=24, `iconLg`=32), button heights, touch targets
- **ElevationTokens** — shadow levels (`xs`=1dp through `xxl`=16dp)
- **AnimationTokens** — duration constants (`fast`=100ms, `normal`=200ms, `medium`=300ms, `slow`=500ms)
- **GlassTokens** — glass opacity/blur/border values per level
- **ResponsiveTokens** — Material 3 breakpoints and grid configuration
- **TypographyTokens** — full M3 type scale (displayLarge through labelSmall)

---

## 3. Theme System

### 3.1 AvanueThemeProvider (Entry Point)

Wrap your app content with `AvanueThemeProvider`. It sets up:
- Material3 `MaterialTheme` (so M3 components like TextField, Button work)
- Custom CompositionLocals for colors, glass, and display profile
- Density override for automatic dp/sp scaling

```kotlin
import com.augmentalis.avanueui.theme.AvanueThemeProvider
import com.augmentalis.avanueui.theme.OceanColors
import com.augmentalis.avanueui.theme.OceanGlass
import com.augmentalis.avanueui.display.DisplayProfile

@Composable
fun MyApp() {
    AvanueThemeProvider(
        colors = OceanColors,           // or SunsetColors, or custom
        glass = OceanGlass,             // or SunsetGlass
        displayProfile = DisplayProfile.PHONE
    ) {
        MyMainScreen()
    }
}
```

### 3.2 AvanueTheme Facade (Access Point)

Access current theme values inside `@Composable` functions:

```kotlin
import com.augmentalis.avanueui.theme.AvanueTheme

@Composable
fun MyScreen() {
    val bg = AvanueTheme.colors.background       // Color
    val primary = AvanueTheme.colors.primary      // Color
    val overlay = AvanueTheme.glass.overlayColor  // Color
    val profile = AvanueTheme.displayProfile      // DisplayProfile enum
}
```

### 3.3 Available Color Schemes

| Scheme | Style | Primary | Background |
|--------|-------|---------|------------|
| `OceanColors` | Cool blue | `#3B82F6` | `#0F172A` (deep navy) |
| `SunsetColors` | Warm coral | `#FF6B35` | `#1A0E1F` (deep purple) |

### 3.4 Creating a Custom Color Scheme

Implement `AvanueColorScheme`:

```kotlin
object MyBrandColors : AvanueColorScheme {
    override val primary = Color(0xFF...)
    override val onPrimary = Color.White
    // ... all 28 properties
    override fun resolve(id: String): Color? = resolveDefault(id)
}
```

Then use: `AvanueThemeProvider(colors = MyBrandColors) { ... }`

---

## 4. Glass Effects

### 4.1 Three-Level System

| Level | Overlay | Blur | Use Case |
|-------|---------|------|----------|
| `GlassLevel.LIGHT` | 10% | 6dp | Subtle background panels |
| `GlassLevel.MEDIUM` | 15% | 8dp | **Default** — cards, dialogs |
| `GlassLevel.HEAVY` | 22% | 10dp | Prominent overlays, modals |

### 4.2 Modifier.glass()

Apply glass effect to any composable:

```kotlin
import com.augmentalis.avanueui.components.glass.glass
import com.augmentalis.avanueui.components.glass.GlassShapes
import com.augmentalis.avanueui.components.glass.GlassDefaults
import com.augmentalis.avanueui.glass.GlassLevel

Box(
    modifier = Modifier
        .glass(
            backgroundColor = AvanueTheme.colors.surface,
            glassLevel = GlassLevel.MEDIUM,
            border = GlassDefaults.border,
            shape = GlassShapes.default
        )
        .padding(SpacingTokens.md)
) {
    Text("Glass Panel", color = AvanueTheme.colors.textPrimary)
}
```

### 4.3 Glass Components

All glass components are in `com.augmentalis.avanueui.components.glass`.

**GlassSurface** — Base interactive glass surface:
```kotlin
GlassSurface(
    onClick = { /* action */ },
    glassLevel = GlassLevel.MEDIUM,
    border = GlassDefaults.border,
    shape = GlassShapes.default
) {
    Text("Clickable glass")
}
```

**GlassCard** — Card container with glass effect:
```kotlin
GlassCard(glassLevel = GlassLevel.LIGHT) {
    Text("Title", style = MaterialTheme.typography.titleMedium)
    Text("Content")
}
```

**OceanButton** — Primary button (glass optional):
```kotlin
OceanButton(onClick = { submit() }) {
    Text("Submit")
}
```

**GlassChip** — Tag/chip:
```kotlin
GlassChip(
    onClick = { },
    label = { Text("Active") },
    glass = true
)
```

**GlassIndicator** — Row indicator with glass background:
```kotlin
GlassIndicator(glassLevel = GlassLevel.MEDIUM) {
    Icon(Icons.Default.Info, null)
    Text("Status message")
}
```

### 4.4 GlassMorphismConfig (Advanced)

For fine-grained control beyond the three-level system:

```kotlin
import com.augmentalis.avanueui.components.glass.GlassMorphismConfig
import com.augmentalis.avanueui.components.glass.DepthLevel
import com.augmentalis.avanueui.components.glass.GlassPresets
import com.augmentalis.avanueui.components.glass.glassMorphism

// Use a preset
Box(modifier = Modifier.glassMorphism(GlassPresets.Card, DepthLevel.Normal))

// Custom config
val config = GlassMorphismConfig(
    cornerRadius = 16.dp,
    backgroundOpacity = 0.1f,
    borderOpacity = 0.2f,
    tintColor = Color(0xFF2196F3),
    tintOpacity = 0.15f
)
Box(modifier = Modifier.glassMorphism(config, DepthLevel.Prominent))
```

**Presets:** Primary, Success, Warning, Error, Info, Card, Elevated

**Depth levels:** Subtle (0.5x), Normal (1.0x), Prominent (1.5x), Intense (2.0x)

### 4.5 Module-Specific Glass Configs

Create your own `GlassMorphismConfig` objects in your module:

```kotlin
// In your module's GlassConfigs.kt
object MyModuleGlassConfigs {
    val Primary = GlassMorphismConfig(
        tintColor = MyColors.Primary,
        cornerRadius = 16.dp
    )
    val Card = GlassMorphismConfig(
        tintColor = MyColors.Secondary,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.08f
    )
}
```

See `AvidCreator/GlassmorphismUtils.kt` for a real-world example (`UUIDGlassConfigs`).

---

## 5. Display Profiles & Density Scaling

### 5.1 Six Display Profiles

| Profile | Density | Font Scale | Min Touch | Layout |
|---------|---------|------------|-----------|--------|
| `GLASS_MICRO` | 0.625x | 0.75x | 36dp | Paginated |
| `GLASS_COMPACT` | 0.75x | 0.85x | 40dp | Scroll |
| `GLASS_STANDARD` | 0.875x | 0.9x | 44dp | Scroll |
| `PHONE` | 1.0x | 1.0x | 48dp | Adaptive |
| `TABLET` | 1.0x | 1.0x | 48dp | List-detail |
| `GLASS_HD` | 0.9x | 0.95x | 48dp | Adaptive |

### 5.2 How Density Scaling Works

`AvanueThemeProvider` overrides `LocalDensity` based on the display profile. ALL dp/sp values auto-scale with zero code changes.

Example: `SpacingTokens.md` (16dp)
- PHONE (1.0x) → 16dp physical
- GLASS_COMPACT (0.75x) → 12dp physical
- GLASS_MICRO (0.625x) → 10dp physical

**Write your dp values once. They adapt everywhere.**

### 5.3 Detecting Profile

```kotlin
import com.augmentalis.avanueui.display.DisplayProfileResolver

val profile = DisplayProfileResolver.resolve(
    widthPx = displayMetrics.widthPixels,
    heightPx = displayMetrics.heightPixels,
    densityDpi = displayMetrics.densityDpi,
    isSmartGlass = DeviceCapabilityFactory.isSmartGlass()
)
```

### 5.4 Layout Strategy

```kotlin
when (AvanueTheme.displayProfile.layoutStrategy) {
    LayoutStrategy.SINGLE_PANE_PAGINATED -> { /* Vuzix Blade */ }
    LayoutStrategy.SINGLE_PANE_SCROLL    -> { /* RealWear HMT */ }
    LayoutStrategy.ADAPTIVE              -> { /* Phones, XREAL */ }
    LayoutStrategy.LIST_DETAIL           -> { /* Tablets */ }
}
```

### 5.5 DisplayUtils

```kotlin
import com.augmentalis.avanueui.display.DisplayUtils

@Composable
fun MyComponent() {
    val touchSize = DisplayUtils.minTouchTarget  // auto-adapts per profile
    val isGlass = DisplayUtils.isGlass           // true for any glass profile
}
```

---

## 6. Settings Provider Pattern

The settings system uses Hilt `@IntoSet` to contribute settings sections from any module.

### 6.1 Settings Components

```kotlin
import com.augmentalis.avanueui.components.settings.SettingsSection
import com.augmentalis.avanueui.components.settings.SettingsToggle
import com.augmentalis.avanueui.components.settings.SettingsSlider
import com.augmentalis.avanueui.components.settings.SettingsDropdown
import com.augmentalis.avanueui.components.settings.SettingsNavItem
```

### 6.2 Creating a Settings Provider

1. Implement `SettingsProvider`:
```kotlin
class MySettingsProvider @Inject constructor() : SettingsProvider {
    override val category = SettingsCategory(
        id = "my_settings",
        title = "My Settings",
        icon = Icons.Default.Settings,
        order = 600  // after System(500)
    )

    @Composable
    override fun Content(displayMode: SettingsDisplayMode) {
        SettingsSection(title = "General") {
            SettingsToggle(
                title = "Enable Feature",
                checked = isEnabled,
                onCheckedChange = { isEnabled = it }
            )
        }
    }
}
```

2. Add to Hilt module:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object MySettingsModule {
    @Provides
    @IntoSet
    fun provideMySettings(provider: MySettingsProvider): SettingsProvider = provider
}
```

3. **CRITICAL**: Use `@JvmSuppressWildcards` on Set injection:
```kotlin
@Inject
constructor(
    providers: Set<@JvmSuppressWildcards SettingsProvider>
)
```

See Chapter 90 for full details on the Unified Adaptive Settings architecture.

---

## 7. PulseDot — Service State Indicator

Animated dot with concentric pulse rings:

```kotlin
import com.augmentalis.avanueui.components.glass.PulseDot
import com.augmentalis.avanueui.components.glass.StatusBadge

PulseDot(state = ServiceState.Running, dotSize = 12.dp)
StatusBadge(state = ServiceState.Running)  // Shows "ACTIVE" in green
```

| State | Color | Animation |
|-------|-------|-----------|
| Running | Green | Expanding pulse rings |
| Ready | Blue | Subtle glow |
| Stopped | Gray | None |
| Error | Red | Fast pulse |
| Degraded | Amber | Slow pulse |

---

## 8. Migration Guide

### 8.1 From Old Imports to New

**Build dependency:**
```
BEFORE: implementation(project(":Modules:AvanueUI:DesignSystem"))
        implementation(project(":Modules:AvanueUI:Foundation"))
AFTER:  implementation(project(":Modules:AvanueUI"))
```

**Theme colors (from OceanTheme):**
```
BEFORE: OceanTheme.background       →  AvanueTheme.colors.background
BEFORE: OceanTheme.primary          →  AvanueTheme.colors.primary
BEFORE: OceanTheme.textPrimary      →  AvanueTheme.colors.textPrimary
```

**Tokens (from OceanDesignTokens):**
```
BEFORE: OceanDesignTokens.Spacing.Medium  →  SpacingTokens.md
BEFORE: OceanDesignTokens.Shape.Small     →  ShapeTokens.sm
BEFORE: OceanDesignTokens.Size.IconMedium →  SizeTokens.iconMd
```

**Glass components (from Foundation):**
```
BEFORE: com.augmentalis.avamagic.ui.foundation.GlassSurface
AFTER:  com.augmentalis.avanueui.components.glass.GlassSurface
```

**Glass intensity (from legacy com.avanueui):**
```
BEFORE: GlassIntensity.LIGHT / .MEDIUM / .HEAVY
AFTER:  GlassLevel.LIGHT / .MEDIUM / .HEAVY

BEFORE: GlassSurface(intensity = GlassIntensity.LIGHT, showBorder = false)
AFTER:  GlassSurface(glassLevel = GlassLevel.LIGHT, border = null)
```

**Settings components:**
```
BEFORE: com.avanueui.settings.SettingsSection
AFTER:  com.augmentalis.avanueui.components.settings.SettingsSection
```

### 8.2 Spacing Value Difference

Old `OceanDesignTokens.Spacing.md` was **12dp**. New `SpacingTokens.md` is **16dp** (Material 3 standard). Verify your layouts after migrating.

### 8.3 BANNED Imports

These packages no longer exist:
- `com.avanueui.*` (entire old package deleted)
- `com.augmentalis.avamagic.ui.foundation.*` (moved to `components.glass`)
- `com.augmentalis.avamagic.designsystem.*` (deleted)
- `com.augmentalis.webavanue.OceanDesignTokens` (deleted)
- `com.augmentalis.webavanue.OceanTheme` (deleted)

---

## 9. Quick Reference

> **NOTE:** As of Phase 2 (Chapter 92), prefer **unified components** (`AvanueCard`, `AvanueSurface`, etc.) over the Glass* components below. The Glass* components are now `@Deprecated` — they still work but the unified API is the recommended path forward. See Chapter 92 for full details.

```
DEPENDENCY:  implementation(project(":Modules:AvanueUI"))

TOKENS:      SpacingTokens.md (16dp)  ShapeTokens.lg (16dp)  SizeTokens.iconMd (24dp)
THEME:       AvanueTheme.colors.primary  AvanueTheme.glass.overlayColor
             AvanueTheme.materialMode (NEW — GLASS/WATER/PLAIN)
DISPLAY:     AvanueTheme.displayProfile  DisplayUtils.isGlass
GLASS:       GlassLevel.LIGHT/MEDIUM/HEAVY  Modifier.glass(...)
WATER:       WaterLevel.REGULAR/CLEAR/IDENTITY  Modifier.waterEffect(...)

UNIFIED COMPONENTS (Phase 2 — preferred):
  AvanueSurface  AvanueCard  AvanueButton  AvanueChip
  AvanueBubble   AvanueFAB   AvanueIconButton

LEGACY COMPONENTS (deprecated — use unified instead):
  GlassSurface  GlassCard  OceanButton  GlassChip
  WaterSurface  WaterCard  WaterButton

STILL CURRENT (not deprecated):
  PulseDot  StatusBadge  WaterNavigationBar

PACKAGES:
  unified  → com.augmentalis.avanueui.components.*             (NEW primary)
  tokens   → com.augmentalis.avanueui.tokens.*
  theme    → com.augmentalis.avanueui.theme.*
  display  → com.augmentalis.avanueui.display.*
  glass    → com.augmentalis.avanueui.glass.* (types)
             com.augmentalis.avanueui.components.glass.* (deprecated components)
  water    → com.augmentalis.avanueui.water.* (types + effects)
             com.augmentalis.avanueui.components.water.* (deprecated components)
  settings → com.augmentalis.avanueui.components.settings.*
```

---

*Chapter 91 | AvanueUI Design System Guide | 2026-02-08 | Branch: 060226-1-consolidation-framework*
*Prerequisite: Chapter 88 (Consolidated App), Chapter 90 (Unified Settings)*
