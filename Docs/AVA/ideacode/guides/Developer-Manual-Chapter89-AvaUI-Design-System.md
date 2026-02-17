# Developer Manual - Chapter 89: AvaUI Design System

**Version:** 2.0.0
**Date:** 2026-02-11
**Author:** AVA Development Team
**Status:** PARTIALLY SUPERSEDED — See notes below
**Module:** AvanueUI (Modules/AvanueUI/)

---

> ### IMPORTANT: Superseded Sections
>
> This chapter was written for the pre-v5.0 architecture. The following sections are **SUPERSEDED**:
>
> | Section | Status | Replacement |
> |---------|--------|-------------|
> | 1. Overview (Architecture) | **SUPERSEDED** | Chapter 91 Section 1 |
> | 2. Design Tokens | **CURRENT** | Tokens are unchanged |
> | 3. Glassmorphic Components | **DEPRECATED** | Use unified components (Ch 92) |
> | 4. Glass Modifiers | **CURRENT** | `Modifier.glass()` still works |
> | 5. Themes (OceanTheme, MagicTheme, GlassAvanue) | **SUPERSEDED** | Chapter 91 Section 3 (Theme v5.1) |
> | 6. ComponentProvider | **SUPERSEDED** | Use unified components directly |
>
> **For current documentation, see:**
> - **Chapter 91** — AvanueUI Design System Guide v2.0 (Theme v5.1, module structure, palette system)
> - **Chapter 92** — Unified Component Architecture (MaterialMode, unified components)

---

## Table of Contents

1. [Overview](#1-overview)
2. [Design Tokens](#2-design-tokens) (CURRENT)
3. [Glassmorphic Components](#3-glassmorphic-components) (DEPRECATED — see Ch 92)
4. [Glass Modifiers](#4-glass-modifiers) (CURRENT)
5. [Themes](#5-themes) (SUPERSEDED — see Ch 91)
6. [ComponentProvider](#6-componentprovider) (SUPERSEDED)
7. [Spatial/AR Tokens](#7-spatial-ar-tokens) (CURRENT)
8. [ServiceState Integration](#8-servicestate-integration) (CURRENT)
9. [Usage Examples](#9-usage-examples) (SUPERSEDED)
10. [File Locations](#10-file-locations) (SUPERSEDED)

---

## 1. Overview

AvaUI (AvanueUI) is the comprehensive design system powering all Avanues applications. It provides a unified visual language across Android, iOS, Desktop, and Web platforms via Kotlin Multiplatform.

### Core Philosophy

- **Theme-Driven Rendering**: Glass, Water, Cupertino, or MountainView — chosen by `MaterialMode`
- **Cross-Platform**: Single design system across all platforms (KMP)
- **Three Independent Axes**: Color Palette x Material Style x Appearance Mode (32 combinations)
- **Token-Based**: Atomic design tokens ensure consistency
- **AR/VR Ready**: XR-optimized palettes with transparent backgrounds for smart glasses

### Architecture (v5.1)

```
Modules/AvanueUI/src/commonMain/
├── tokens/           # SpacingTokens, ShapeTokens, SizeTokens, etc.
├── theme/            # AvanueColorPalette, MaterialMode, AppearanceMode
│                     # Hydra/Sol/Luna/Terra (Colors/Glass/Water + Light + XR)
├── display/          # DisplayProfile, DisplayUtils
├── glass/            # GlassLevel, GlassBorder
├── water/            # WaterLevel
└── components/       # 7 unified components + legacy glass/water (deprecated)
```

### Theme System (v5.1 — Three Axes)

| Axis | Enum | Values | Default |
|------|------|--------|---------|
| **Color Palette** | `AvanueColorPalette` | SOL, LUNA, TERRA, HYDRA | HYDRA |
| **Material Style** | `MaterialMode` | Glass, Water, Cupertino, MountainView | Water |
| **Appearance** | `AppearanceMode` | Light, Dark, Auto | Auto |

> **DEPRECATED:** `OceanTheme`, `MagicTheme`, `GlassAvanue`, `AvanueThemeVariant`, `Foundation/`, `DesignSystem/`, `Core/` directories.

---

## 2. Design Tokens

Design tokens are the atomic values that define the visual language. All components reference tokens, never hardcoded values.

### 2.1 Color Tokens (via AvanueTheme.colors)

> **DEPRECATED:** `OceanTheme.*` — Use `AvanueTheme.colors.*` instead. Colors vary by palette (Hydra/Sol/Luna/Terra) and appearance (Light/Dark). Values below are for Hydra Dark (default).

#### Background Colors
```kotlin
AvanueTheme.colors.background       // #020617 - Sapphire black
AvanueTheme.colors.surface          // #0F172A - Deep slate
AvanueTheme.colors.surfaceElevated  // #1E293B - Elevated surface
AvanueTheme.colors.surfaceInput     // #1E293B - Input fields
```

#### Primary Colors (Royal Sapphire)
```kotlin
AvanueTheme.colors.primary          // #1E40AF - Royal sapphire
AvanueTheme.colors.primaryDark      // #1E3A8A - Pressed state
AvanueTheme.colors.primaryLight     // #3B82F6 - Hover state
```

#### Text Colors
```kotlin
AvanueTheme.colors.textPrimary      // #F1F5F9 - Primary text
AvanueTheme.colors.textSecondary    // #94A3B8 - Secondary text
AvanueTheme.colors.textTertiary     // #64748B - Tertiary text
AvanueTheme.colors.textDisabled     // #475569 - Disabled text
AvanueTheme.colors.textOnPrimary    // #FFFFFF - Text on primary color
```

#### State Colors
```kotlin
AvanueTheme.colors.success          // #22C55E - Green
AvanueTheme.colors.warning          // #F59E0B - Amber
AvanueTheme.colors.error            // #EF4444 - Red
AvanueTheme.colors.info             // #3B82F6 - Blue
```

#### Border Colors
```kotlin
AvanueTheme.colors.border           // 12% white (dark) / 12% black (light)
AvanueTheme.colors.borderSubtle     // 6% white/black
AvanueTheme.colors.borderStrong     // 25% white/black
```

### 2.2 Spacing Tokens (OceanDesignTokens.Spacing)

```kotlin
OceanDesignTokens.Spacing.xs            // 4dp - Minimal spacing
OceanDesignTokens.Spacing.sm            // 8dp - Small spacing
OceanDesignTokens.Spacing.md            // 12dp - Medium spacing
OceanDesignTokens.Spacing.lg            // 16dp - Large spacing
OceanDesignTokens.Spacing.xl            // 24dp - Extra large spacing
OceanDesignTokens.Spacing.xxl           // 32dp - Extra extra large
OceanDesignTokens.Spacing.touchTarget   // 48dp - Minimum touch target
```

### 2.3 Typography Tokens (TypographyTokens)

#### Display Styles
```kotlin
TypographyTokens.DisplayLarge   // 57sp, Regular, 64sp line height
TypographyTokens.DisplayMedium  // 45sp, Regular, 52sp line height
TypographyTokens.DisplaySmall   // 36sp, Regular, 44sp line height
```

#### Headline Styles
```kotlin
TypographyTokens.HeadlineLarge   // 32sp, Medium, 40sp line height
TypographyTokens.HeadlineMedium  // 28sp, Medium, 36sp line height
TypographyTokens.HeadlineSmall   // 24sp, Medium, 32sp line height
```

#### Title Styles
```kotlin
TypographyTokens.TitleLarge   // 22sp, Medium, 28sp line height
TypographyTokens.TitleMedium  // 16sp, Medium, 24sp line height
TypographyTokens.TitleSmall   // 14sp, Medium, 20sp line height
```

#### Body Styles
```kotlin
TypographyTokens.BodyLarge   // 16sp, Regular, 24sp line height
TypographyTokens.BodyMedium  // 14sp, Regular, 20sp line height
TypographyTokens.BodySmall   // 12sp, Regular, 16sp line height
```

#### Label Styles
```kotlin
TypographyTokens.LabelLarge   // 14sp, Medium, 20sp line height
TypographyTokens.LabelMedium  // 12sp, Medium, 16sp line height
TypographyTokens.LabelSmall   // 11sp, Medium, 16sp line height
```

### 2.4 Shape Tokens (GlassShapes)

```kotlin
GlassShapes.default      // 12dp - Default rounded corners
GlassShapes.small        // 8dp - Small elements
GlassShapes.large        // 16dp - Large cards
GlassShapes.extraLarge   // 24dp - Modal dialogs
GlassShapes.chipShape    // 8dp - Chips
GlassShapes.buttonShape  // 12dp - Buttons
GlassShapes.fabShape     // 16dp - Floating action buttons
GlassShapes.circle       // CircleShape
GlassShapes.full         // 9999dp - Pill shape
GlassShapes.bubbleStart  // Chat bubble (left-aligned)
GlassShapes.bubbleEnd    // Chat bubble (right-aligned)
```

### 2.5 Size Tokens (SizeTokens)

#### Icon Sizes
```kotlin
SizeTokens.Icon.Small       // 16dp
SizeTokens.Icon.Medium      // 24dp
SizeTokens.Icon.Large       // 32dp
SizeTokens.Icon.ExtraLarge  // 48dp
```

#### Button Heights
```kotlin
SizeTokens.Button.Small       // 32dp
SizeTokens.Button.Medium      // 40dp
SizeTokens.Button.Large       // 48dp
SizeTokens.Button.ExtraLarge  // 56dp
```

#### Touch Targets
```kotlin
SizeTokens.MinTouchTarget        // 48dp - Standard minimum
SizeTokens.MinTouchTargetSpatial // 60dp - AR/VR minimum
```

#### Component Sizes
```kotlin
SizeTokens.AppBar.Height         // 56dp
SizeTokens.AppBar.CompactHeight  // 48dp
SizeTokens.CommandBar.Height     // 64dp
SizeTokens.CommandBar.ItemSize   // 48dp
SizeTokens.CommandBar.VoiceButton // 56dp
```

### 2.6 Animation Tokens (AnimationTokens)

```kotlin
AnimationTokens.DurationShort      // 150ms - Fast transitions
AnimationTokens.DurationMedium     // 300ms - Standard transitions
AnimationTokens.DurationLong       // 500ms - Slow transitions
AnimationTokens.DurationExtraLong  // 1000ms - Very slow transitions
```

### 2.7 Responsive Tokens (ResponsiveTokens)

#### Breakpoints
```kotlin
ResponsiveTokens.BreakpointSM  // 600dp - Small screens (phones)
ResponsiveTokens.BreakpointMD  // 840dp - Medium screens (tablets portrait)
ResponsiveTokens.BreakpointLG  // 1240dp - Large screens (tablets landscape)
ResponsiveTokens.BreakpointXL  // 1440dp - Extra large screens (desktop)
```

#### Grid Columns
```kotlin
ResponsiveTokens.GridColumnsXS  // 4 columns (< 600dp)
ResponsiveTokens.GridColumnsSM  // 8 columns (600-840dp)
ResponsiveTokens.GridColumnsMD  // 12 columns (840-1240dp)
ResponsiveTokens.GridColumnsLG  // 12 columns (≥ 1240dp)
```

### 2.8 Elevation Tokens

```kotlin
ElevationTokens.Level0  // 0dp - No elevation
ElevationTokens.Level1  // 1dp - Subtle elevation
ElevationTokens.Level2  // 3dp - Standard elevation
ElevationTokens.Level3  // 6dp - Elevated cards
ElevationTokens.Level4  // 8dp - Dialogs
ElevationTokens.Level5  // 12dp - Floating elements
```

---

## 3. Glassmorphic Components

All glassmorphic components are located in `Foundation/GlassmorphicComponents.kt`.

### 3.1 GlassSurface

Base glassmorphic surface component.

```kotlin
@Composable
fun GlassSurface(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GlassDefaults.shape,
    color: Color = OceanTheme.surface,
    contentColor: Color = OceanTheme.textPrimary,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: GlassBorder? = GlassDefaults.border,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    content: @Composable () -> Unit
)
```

**MagicUI equivalent:** `MagicUI.Surface`

### 3.2 GlassCard

Glassmorphic card component with ColumnScope content.

```kotlin
@Composable
fun GlassCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GlassDefaults.shape,
    colors: CardColors = GlassDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: GlassBorder? = GlassDefaults.border,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    content: @Composable ColumnScope.() -> Unit
)
```

**MagicUI equivalent:** `MagicUI.Card`

### 3.3 GlassBubble

Chat bubble / message component with glass effect.

```kotlin
@Composable
fun GlassBubble(
    modifier: Modifier = Modifier,
    align: BubbleAlign = BubbleAlign.START,
    shape: Shape = when (align) {
        BubbleAlign.START -> GlassShapes.bubbleStart
        BubbleAlign.END -> GlassShapes.bubbleEnd
        BubbleAlign.CENTER -> GlassDefaults.shape
    },
    color: Color = OceanTheme.surface,
    contentColor: Color = OceanTheme.textPrimary,
    border: GlassBorder? = GlassDefaults.border,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    content: @Composable () -> Unit
)
```

**BubbleAlign options:**
- `BubbleAlign.START` - Left-aligned (incoming message)
- `BubbleAlign.END` - Right-aligned (outgoing message)
- `BubbleAlign.CENTER` - Center-aligned (system message)

**MagicUI equivalent:** `MagicUI.ChatBubble`

### 3.4 OceanButton

Primary button with optional glass effect.

```kotlin
@Composable
fun OceanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Boolean = false,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = OceanTheme.primary,
        contentColor = OceanTheme.textOnPrimary
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    shape: Shape = GlassDefaults.shape,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
)
```

**MagicUI equivalent:** `MagicUI.Button`

### 3.5 GlassChip

Chip/tag component with optional glass styling.

```kotlin
@Composable
fun GlassChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    glass: Boolean = false,
    glassLevel: GlassLevel = GlassLevel.LIGHT,
    colors: ChipColors = AssistChipDefaults.assistChipColors(),
    elevation: ChipElevation? = AssistChipDefaults.assistChipElevation(),
    shape: Shape = GlassShapes.chipShape
)
```

**MagicUI equivalent:** `MagicUI.Chip`

### 3.6 GlassFloatingActionButton

FAB with mandatory glass effect.

```kotlin
@Composable
fun GlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.fabShape,
    containerColor: Color = OceanTheme.primary,
    contentColor: Color = OceanTheme.textOnPrimary,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    content: @Composable () -> Unit
)
```

**MagicUI equivalent:** `MagicUI.FAB`

### 3.7 GlassIconButton

Icon button with optional glass effect.

```kotlin
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Boolean = false,
    glassLevel: GlassLevel = GlassLevel.LIGHT,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
)
```

**MagicUI equivalent:** `MagicUI.IconButton`

### 3.8 Glass Levels

```kotlin
enum class GlassLevel {
    LIGHT,   // 5% opacity, 6dp blur, subtle effect
    MEDIUM,  // 8% opacity, 8dp blur, moderate effect
    HEAVY    // 12% opacity, 10dp blur, strong effect
}
```

### 3.9 GlassBorder

```kotlin
data class GlassBorder(
    val width: Dp,
    val color: Color
)

// Defaults
GlassDefaults.border        // 1dp, 30% white
GlassDefaults.borderSubtle  // 0.5dp, 10% white
GlassDefaults.borderStrong  // 1.5dp, 30% white
GlassDefaults.borderFocused // 2dp, primary color
```

---

## 4. Glass Modifiers

Glass modifiers provide reusable styling utilities for creating glassmorphic effects.

### 4.1 Base Glass Modifier

```kotlin
fun Modifier.glass(
    backgroundColor: Color,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    border: GlassBorder? = null,
    shape: Shape = GlassDefaults.shape
): Modifier
```

**Example:**
```kotlin
Box(
    modifier = Modifier
        .size(200.dp)
        .glass(
            backgroundColor = OceanTheme.surface,
            glassLevel = GlassLevel.MEDIUM,
            border = GlassDefaults.border
        )
) {
    Text("Glassmorphic Box")
}
```

### 4.2 Preset Glass Modifiers

```kotlin
// Light glass effect (5% opacity, 6dp blur)
Modifier.glassLight(
    backgroundColor: Color = OceanTheme.surface,
    border: GlassBorder? = GlassDefaults.borderSubtle
)

// Medium glass effect (8% opacity, 8dp blur)
Modifier.glassMedium(
    backgroundColor: Color = OceanTheme.surface,
    border: GlassBorder? = GlassDefaults.border
)

// Heavy glass effect (12% opacity, 10dp blur)
Modifier.glassHeavy(
    backgroundColor: Color = OceanTheme.surface,
    border: GlassBorder? = GlassDefaults.borderStrong
)

// Frosted glass with gradient
Modifier.glassFrosted(
    gradient: Brush = OceanGradients.surfaceGradient,
    blurRadius: Dp = 10.dp,
    border: GlassBorder? = GlassDefaults.border
)
```

### 4.3 Glassmorphism Core Modifier

For advanced glassmorphic effects with full configurability:

```kotlin
fun Modifier.glassMorphism(
    config: GlassMorphismConfig,
    depth: DepthLevel = DepthLevel.Normal
): Modifier

data class GlassMorphismConfig(
    val cornerRadius: Dp = 12.dp,
    val backgroundOpacity: Float = 0.08f,
    val borderOpacity: Float = 0.3f,
    val borderWidth: Dp = 1.dp,
    val tintColor: Color = Color.White,
    val tintOpacity: Float = 0.1f,
    val blurRadius: Dp = 8.dp
)

enum class DepthLevel {
    Subtle,      // 0.5x intensity
    Normal,      // 1.0x intensity
    Prominent,   // 1.5x intensity
    Intense      // 2.0x intensity
}
```

### 4.4 OceanGlass Presets

Preset glass styles for common use cases:

```kotlin
// Card glass (medium effect, standard border)
Modifier.with(OceanGlass) { card() }

// Surface glass (light effect, subtle border)
Modifier.with(OceanGlass) { surface() }

// Elevated glass (heavy effect, strong border)
Modifier.with(OceanGlass) { elevated() }

// Dialog glass (heavy effect with gradient)
Modifier.with(OceanGlass) { dialog() }

// Bubble glass (medium effect, no border)
Modifier.with(OceanGlass) { bubble() }

// Button glass (light effect with primary color)
Modifier.with(OceanGlass) { button() }

// Chip glass (light effect, subtle border)
Modifier.with(OceanGlass) { chip() }
```

### 4.5 OceanGradients

Preset gradients for glassmorphic effects:

```kotlin
OceanGradients.surfaceGradient   // Vertical gradient for surfaces
OceanGradients.dialogGradient    // Radial gradient for dialogs
OceanGradients.primaryGradient   // Primary color gradient
OceanGradients.shimmerGradient   // Shimmer loading effect
OceanGradients.successGradient   // Success state gradient
OceanGradients.errorGradient     // Error state gradient
OceanGradients.warningGradient   // Warning state gradient
OceanGradients.radialGradient    // General radial gradient
```

### 4.6 Glass Presets

Predefined glassmorphism configurations:

```kotlin
GlassPresets.Primary      // Primary color glass
GlassPresets.Success      // Success state glass
GlassPresets.Warning      // Warning state glass
GlassPresets.Error        // Error state glass
GlassPresets.Info         // Info state glass
GlassPresets.Card         // Standard card glass
GlassPresets.Elevated     // Elevated surface glass
```

---

## 5. Themes

### 5.1 OceanTheme (Primary Theme)

The primary dark glassmorphic theme for Avanues applications.

**Location:** `Foundation/OceanTheme.kt`

**Usage:**
```kotlin
import com.augmentalis.avamagic.ui.foundation.OceanTheme

// Access colors directly
val primaryColor = OceanTheme.primary
val surfaceColor = OceanTheme.surface
val textColor = OceanTheme.textPrimary
```

**Color Hierarchy:**
- Background: Deep slate (#0F172A)
- Surface: Slate (#1E293B)
- Surface Elevated: Lighter slate (#334155)
- Primary: Coral Blue (#3B82F6)
- Text: White with varying opacity

### 5.2 AvanueTheme (Material3 Wrapper)

Material3 wrapper with custom extensions for spacing and elevation.

**Location:** `AvanueUI/src/commonMain/kotlin/com/avanueui/AvanueTheme.kt`

**Usage:**
```kotlin
import com.avanueui.AvanueTheme

@Composable
fun MyApp() {
    AvanueTheme {
        // Access Material3 colors
        val primary = MaterialTheme.colorScheme.primary

        // Access custom extensions
        val spacing = AvanueTheme.spacing.medium
        val elevation = AvanueTheme.elevation.level2

        // Your UI
    }
}
```

**Extensions:**
```kotlin
AvanueTheme.spacing.xs       // 4dp
AvanueTheme.spacing.small    // 8dp
AvanueTheme.spacing.medium   // 16dp
AvanueTheme.spacing.large    // 24dp
AvanueTheme.spacing.xl       // 32dp

AvanueTheme.elevation.level1 // 2dp
AvanueTheme.elevation.level2 // 4dp
AvanueTheme.elevation.level3 // 8dp
AvanueTheme.elevation.level4 // 12dp
AvanueTheme.elevation.level5 // 16dp
```

### 5.3 MagicTheme (Extended Material3)

Extended Material3 theme with custom spacing and elevation tokens.

**Location:** `DesignSystem/MagicTheme.kt`

**Usage:**
```kotlin
import com.augmentalis.avamagic.designsystem.MagicTheme
import com.augmentalis.avamagic.designsystem.MagicThemeExtensions

@Composable
fun MyApp() {
    MagicTheme {
        // Access spacing
        val spacing = MagicThemeExtensions.spacing.medium

        // Access elevation
        val elevation = MagicThemeExtensions.elevation.level2

        // Your UI
    }
}
```

### 5.4 GlassAvanue (AR/Smart Glasses Theme)

Specialized theme optimized for AR/MR devices (Vuzix, Vision Pro, Nreal).

**Location:** `Core/GlassAvanue.kt`

**Design Principles:**
- **Transparent First**: 0% background opacity for AR passthrough
- **Card Opacity**: 65-75% for readability without blocking real world
- **Blur Radius**: 20-30px for depth perception
- **Dynamic Adaptation**: Context-aware theming

**Modes:**
```kotlin
import com.augmentalis.avamagic.components.themes.GlassAvanue

// Light mode (75% white glass)
val lightTheme = GlassAvanue.Light

// Dark mode (75% black glass)
val darkTheme = GlassAvanue.Dark

// Auto mode (system-based)
val autoTheme = GlassAvanue.Auto
```

**Custom Accent:**
```kotlin
// Extract dominant color from wallpaper
val wallpaperColor = extractDominantColor(wallpaperBitmap)
val customTheme = GlassAvanue.withAccent(wallpaperColor)
```

**Ambient Light Adaptation:**
```kotlin
// Get ambient light from sensor (0.0-1.0)
val ambientLight = lightSensor.currentLevel
val adaptedTheme = GlassAvanue.adaptToAmbientLight(ambientLight)
```

**Context-Aware Theming:**
```kotlin
enum class AppContext {
    Gaming,    // Neon purple/pink
    Reading,   // More opaque, less blur for clarity
    AR,        // More transparent, more blur for depth
    Focus,     // Dimmer, teal accent
    Social,    // Pink accent
    Default
}

val arTheme = GlassAvanue.forContext(AppContext.AR)
val gamingTheme = GlassAvanue.forContext(AppContext.Gaming)
```

**Visual Properties:**
```kotlin
// Background opacity: 65-75% (0.65-0.75)
// Blur radius: 20-30px (25px default)
// Corner radius: 24px (panels), 12px (icons)
// Shadow: Soft diffuse, no hard edges
// Base tint: rgba(255,255,255,0.75)
// Animation: 250ms ease-in-out
```

### 5.5 Theme Presets from DSL

The AVU DSL provides additional theme presets:

```kotlin
// iOS 26 Liquid Glass theme
Themes.iOS26LiquidGlass

// Material3 Light theme
Themes.Material3Light

// Windows 11 Fluent 2 theme
Themes.Windows11Fluent2

// visionOS 2 Spatial Glass theme
Themes.visionOS2SpatialGlass
```

---

## 6. ComponentProvider (Migration Pattern)

ComponentProvider is an abstraction layer that enables seamless transition between UI frameworks (Material3 → MagicUI).

**Location:** `Foundation/ComponentProvider.kt`

### 6.1 Interface

```kotlin
interface ComponentProvider {
    @Composable
    fun Icon(
        imageVector: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
        variant: IconVariant = IconVariant.DEFAULT
    )

    @Composable
    fun IconButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        variant: ButtonVariant = ButtonVariant.DEFAULT,
        content: @Composable () -> Unit
    )

    @Composable
    fun Button(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        variant: ButtonVariant = ButtonVariant.PRIMARY,
        content: @Composable RowScope.() -> Unit
    )

    @Composable
    fun Surface(
        modifier: Modifier = Modifier,
        variant: SurfaceVariant = SurfaceVariant.DEFAULT,
        shape: Shape = RoundedCornerShape(12.dp),
        onClick: (() -> Unit)? = null,
        content: @Composable () -> Unit
    )

    @Composable
    fun TextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null
    )

    @Composable
    fun FloatingActionButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    )
}
```

### 6.2 Variants

```kotlin
enum class IconVariant {
    DEFAULT,    // Standard icon
    PRIMARY,    // Primary color icon
    SECONDARY,  // Secondary color icon
    DISABLED    // Disabled icon
}

enum class ButtonVariant {
    PRIMARY,    // Filled primary button
    SECONDARY,  // Outlined button
    TERTIARY,   // Text button
    GLASS,      // Glassmorphic button
    DEFAULT     // Standard Material3 button
}

enum class SurfaceVariant {
    DEFAULT,    // Standard surface
    ELEVATED,   // Elevated surface
    GLASS,      // Glassmorphic surface
    CARD        // Card surface
}
```

### 6.3 Implementations

#### OceanComponentProvider (Current)

```kotlin
object OceanComponentProvider : ComponentProvider {
    // Material3 + OceanTheme implementations
}
```

#### MagicUIComponentProvider (Future)

```kotlin
object MagicUIComponentProvider : ComponentProvider {
    // MagicUI implementations (when available)
}
```

### 6.4 Usage Pattern

```kotlin
// Feature flag to switch between providers
val useMagicUI = false  // Set to true when MagicUI ready

val componentProvider = if (useMagicUI) {
    MagicUIComponentProvider
} else {
    OceanComponentProvider
}

// Use provider in UI
@Composable
fun MyScreen(provider: ComponentProvider = OceanComponentProvider) {
    provider.Surface(variant = SurfaceVariant.GLASS) {
        provider.Button(
            onClick = { /* ... */ },
            variant = ButtonVariant.PRIMARY
        ) {
            Text("Click Me")
        }
    }
}
```

**Migration Steps:**
1. Today: Use `OceanComponentProvider` (Material3 + OceanTheme)
2. Tomorrow: Implement `MagicUIComponentProvider`
3. Day After: Flip feature flag (`useMagicUI = true`)
4. Result: **Zero app code changes**

---

## 7. Spatial/AR Tokens

Spatial tokens optimize UI for AR/VR devices and 3D environments.

**Location:** `DesignSystem/SpatialTokens.kt`

### 7.1 Z-Distance Tokens

```kotlin
SpatialSizeTokens.DistanceHUD         // 0.5m - HUD elements
SpatialSizeTokens.DistanceInteractive // 1.0m - Interactive elements
SpatialSizeTokens.DistancePrimary     // 1.5m - Primary content
SpatialSizeTokens.DistanceSecondary   // 2.0m - Secondary content
SpatialSizeTokens.DistanceAmbient     // 3.0m - Ambient/background
```

### 7.2 Spatial Touch Targets

```kotlin
SpatialSizeTokens.MinTouchTarget      // 60dp - AR/VR minimum (vs 48dp standard)
SpatialSizeTokens.ComfortableReach    // 45° cone from eye level
SpatialSizeTokens.MaxVoiceOptions     // 3 - Max voice commands shown at once
```

### 7.3 Voice Interface Tokens

```kotlin
SpatialSizeTokens.MaxVoiceOptions         // 3 - Maximum simultaneous voice options
SpatialSizeTokens.CommandHierarchyLevels  // 2 - Maximum command nesting depth
```

### 7.4 Spatial Awareness

```kotlin
// Adapt UI based on real-world context
GlassAvanue.forContext(AppContext.AR)

// Adapt to ambient lighting
GlassAvanue.adaptToAmbientLight(lightLevel = 0.8f)

// Optimize for transparent passthrough
surface = Color(0, 0, 0, 0.0f)  // Fully transparent background
surfaceVariant = Color(255, 255, 255, 0.65f)  // 65% card opacity
```

---

## 8. ServiceState Integration

ServiceState provides cross-module service lifecycle state for dashboard integration.

**Location:** `Foundation/src/commonMain/kotlin/com/augmentalis/foundation/state/ServiceState.kt`

### 8.1 ServiceState Sealed Class

```kotlin
sealed class ServiceState {
    /** Service is running and operational. */
    data class Running(
        val metadata: Map<String, String> = emptyMap()
    ) : ServiceState()

    /** Service is ready but not actively processing. */
    data class Ready(
        val metadata: Map<String, String> = emptyMap()
    ) : ServiceState()

    /** Service is stopped / not running. */
    data object Stopped : ServiceState()

    /** Service encountered an error. */
    data class Error(
        val message: String,
        val recoverable: Boolean = true
    ) : ServiceState()

    /** Service is running but in a degraded state. */
    data class Degraded(
        val reason: String,
        val metadata: Map<String, String> = emptyMap()
    ) : ServiceState()

    val isActive: Boolean
        get() = this is Running || this is Ready || this is Degraded
}
```

### 8.2 ServiceStateProvider Interface

```kotlin
interface ServiceStateProvider {
    /** Unique identifier for this service (e.g., "voiceoscore", "voicecursor"). */
    val moduleId: String

    /** User-facing display name (e.g., "VoiceAvanue", "VoiceCursor"). */
    val displayName: String

    /** Brief description of what this service does. */
    val description: String

    /** Observable state flow. Emits on every state change. */
    val state: StateFlow<ServiceState>

    /** Optional metadata about the current state. */
    val metadata: StateFlow<Map<String, String>>
}
```

### 8.3 LastHeardCommand

```kotlin
data class LastHeardCommand(
    val phrase: String,
    val confidence: Float,
    val timestampMs: Long,
    val wasExecuted: Boolean = true
) {
    companion object {
        val NONE = LastHeardCommand("", 0f, 0L, false)
    }
}
```

### 8.4 Usage in Module

```kotlin
class VoiceCursorStateProvider : ServiceStateProvider {
    private val _state = MutableStateFlow<ServiceState>(ServiceState.Stopped)
    override val state: StateFlow<ServiceState> = _state.asStateFlow()
    override val moduleId: String = "voicecursor"
    override val displayName: String = "VoiceCursor"
    override val description: String = "Hands-free cursor control"

    fun onServiceStarted() {
        _state.value = ServiceState.Running(mapOf("dwell" to "1.5s"))
    }

    fun onServiceStopped() {
        _state.value = ServiceState.Stopped
    }
}
```

### 8.5 Usage in Dashboard

```kotlin
@Composable
fun ServiceDashboard(providers: List<ServiceStateProvider>) {
    providers.forEach { provider ->
        val currentState by provider.state.collectAsState()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulseDot(state = currentState)

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanTheme.textSecondary
                )
            }
        }
    }
}

@Composable
fun PulseDot(state: ServiceState) {
    val color = when (state) {
        is ServiceState.Running -> OceanTheme.success
        is ServiceState.Ready -> OceanTheme.info
        is ServiceState.Stopped -> OceanTheme.textDisabled
        is ServiceState.Error -> OceanTheme.error
        is ServiceState.Degraded -> OceanTheme.warning
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}
```

---

## 9. Usage Examples

### 9.1 Creating a Glass Card with Status Indicator

```kotlin
@Composable
fun ServiceCard(
    provider: ServiceStateProvider,
    modifier: Modifier = Modifier
) {
    val currentState by provider.state.collectAsState()

    GlassCard(
        onClick = { /* Navigate to service details */ },
        modifier = modifier.fillMaxWidth(),
        glassLevel = GlassLevel.MEDIUM
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OceanDesignTokens.Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when (currentState) {
                                    is ServiceState.Running -> OceanTheme.success
                                    is ServiceState.Ready -> OceanTheme.info
                                    is ServiceState.Stopped -> OceanTheme.textDisabled
                                    is ServiceState.Error -> OceanTheme.error
                                    is ServiceState.Degraded -> OceanTheme.warning
                                },
                                shape = CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(OceanDesignTokens.Spacing.sm))

                    Text(
                        text = provider.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = OceanTheme.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.xs))

                Text(
                    text = provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanTheme.textSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = OceanTheme.textTertiary
            )
        }
    }
}
```

### 9.2 Using OceanGlass Presets

```kotlin
@Composable
fun DialogExample() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(OceanDesignTokens.Spacing.xl)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .with(OceanGlass) { dialog() }  // Apply dialog glass preset
                .padding(OceanDesignTokens.Spacing.lg)
        ) {
            Text(
                text = "Glassmorphic Dialog",
                style = MaterialTheme.typography.headlineMedium,
                color = OceanTheme.textPrimary
            )

            Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.md))

            Text(
                text = "This dialog uses the OceanGlass.dialog() preset for a beautiful frosted glass effect.",
                style = MaterialTheme.typography.bodyMedium,
                color = OceanTheme.textSecondary
            )

            Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OceanButton(
                    onClick = { /* Cancel */ },
                    glass = true
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(OceanDesignTokens.Spacing.sm))

                OceanButton(
                    onClick = { /* Confirm */ }
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}
```

### 9.3 Responsive Layout with Breakpoints

```kotlin
@Composable
fun ResponsiveGrid(items: List<Item>) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val columns = when {
        screenWidth < ResponsiveTokens.BreakpointSM -> ResponsiveTokens.GridColumnsXS  // 4
        screenWidth < ResponsiveTokens.BreakpointMD -> ResponsiveTokens.GridColumnsSM  // 8
        screenWidth < ResponsiveTokens.BreakpointLG -> ResponsiveTokens.GridColumnsMD  // 12
        else -> ResponsiveTokens.GridColumnsLG  // 12
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(OceanDesignTokens.Spacing.md)
    ) {
        items(items) { item ->
            GlassCard(
                modifier = Modifier.padding(OceanDesignTokens.Spacing.sm)
            ) {
                ItemContent(item)
            }
        }
    }
}
```

### 9.4 Theme-Swappable Component

```kotlin
@Composable
fun ThemeSwappableButton(
    text: String,
    onClick: () -> Unit,
    provider: ComponentProvider = OceanComponentProvider
) {
    provider.Button(
        onClick = onClick,
        variant = ButtonVariant.PRIMARY
    ) {
        Text(text)
    }
}

// Usage - swap theme by changing provider
@Composable
fun MyApp() {
    val useMagicUI = false  // Feature flag

    val provider = if (useMagicUI) {
        MagicUIComponentProvider
    } else {
        OceanComponentProvider
    }

    ThemeSwappableButton(
        text = "Click Me",
        onClick = { /* ... */ },
        provider = provider
    )
}
```

### 9.5 PulseDot Animation for Service State

```kotlin
@Composable
fun AnimatedPulseDot(state: ServiceState) {
    val infiniteTransition = rememberInfiniteTransition()

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationTokens.DurationLong,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    val baseColor = when (state) {
        is ServiceState.Running -> OceanTheme.success
        is ServiceState.Ready -> OceanTheme.info
        is ServiceState.Stopped -> OceanTheme.textDisabled
        is ServiceState.Error -> OceanTheme.error
        is ServiceState.Degraded -> OceanTheme.warning
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                color = if (state.isActive) {
                    baseColor.copy(alpha = alpha)
                } else {
                    baseColor
                },
                shape = CircleShape
            )
    )
}
```

### 9.6 Chat Bubble Example

```kotlin
@Composable
fun ChatMessage(
    message: String,
    isUser: Boolean,
    timestamp: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = OceanDesignTokens.Spacing.md,
                vertical = OceanDesignTokens.Spacing.xs
            ),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        GlassBubble(
            align = if (isUser) BubbleAlign.END else BubbleAlign.START,
            color = if (isUser) OceanTheme.primary else OceanTheme.surface,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(OceanDesignTokens.Spacing.md)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) OceanTheme.textOnPrimary else OceanTheme.textPrimary
                )

                Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.xs))

                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) {
                        OceanTheme.textOnPrimary.copy(alpha = 0.7f)
                    } else {
                        OceanTheme.textTertiary
                    }
                )
            }
        }
    }
}
```

### 9.7 AR-Optimized Panel

```kotlin
@Composable
fun ARControlPanel(
    onVoiceCommand: () -> Unit,
    onSettings: () -> Unit
) {
    // Use GlassAvanue theme for AR transparency
    val arTheme = GlassAvanue.forContext(AppContext.AR)

    GlassCard(
        modifier = Modifier
            .width(320.dp)
            .glass(
                backgroundColor = arTheme.colorScheme.surface,
                glassLevel = GlassLevel.LIGHT,  // More transparent for AR
                border = GlassBorder(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f)
                )
            ),
        glassLevel = GlassLevel.LIGHT
    ) {
        Column(
            modifier = Modifier.padding(SpatialSizeTokens.MinTouchTarget / 4)
        ) {
            // Larger touch targets for spatial interaction
            GlassIconButton(
                onClick = onVoiceCommand,
                modifier = Modifier.size(SpatialSizeTokens.MinTouchTarget),
                glass = true
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Command",
                    modifier = Modifier.size(SizeTokens.Icon.Large)
                )
            }

            Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.md))

            GlassIconButton(
                onClick = onSettings,
                modifier = Modifier.size(SpatialSizeTokens.MinTouchTarget),
                glass = true
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(SizeTokens.Icon.Large)
                )
            }
        }
    }
}
```

---

## 10. File Locations

### 10.1 Module Structure

```
Modules/AvanueUI/
├── Foundation/src/commonMain/
│   ├── OceanTheme.kt                    # Core color definitions
│   ├── OceanThemeExtensions.kt          # Glass modifiers, gradients, presets
│   ├── OceanDesignTokens.kt             # Spacing, elevation, glass tokens
│   ├── GlassmorphicComponents.kt        # GlassSurface, GlassCard, etc.
│   ├── ComponentProvider.kt             # Abstraction layer interface
│   └── GlassmorphismCore.kt             # Advanced glassmorphism configs
│
├── Core/src/commonMain/
│   ├── GlassAvanue.kt                   # AR/VR optimized theme
│   └── OceanComponentProvider.kt        # Material3 implementation
│
├── DesignSystem/src/commonMain/
│   ├── DesignTokens.kt                  # Universal design tokens
│   ├── TypographyTokens.kt              # Typography scale
│   ├── SpacingTokens.kt                 # Spacing scale
│   ├── AnimationTokens.kt               # Animation durations
│   ├── ResponsiveTokens.kt              # Breakpoints, grid columns
│   ├── SpatialTokens.kt                 # AR/VR specific tokens
│   └── MagicTheme.kt                    # Extended Material3 theme
│
└── src/commonMain/
    └── AvanueTheme.kt                   # Material3 wrapper with extensions

Modules/Foundation/src/commonMain/kotlin/com/augmentalis/foundation/state/
└── ServiceState.kt                      # Service lifecycle state
```

### 10.2 Import Paths

```kotlin
// OceanTheme colors
import com.augmentalis.avamagic.ui.foundation.OceanTheme

// Glass components
import com.augmentalis.avamagic.ui.foundation.GlassSurface
import com.augmentalis.avamagic.ui.foundation.GlassCard
import com.augmentalis.avamagic.ui.foundation.GlassBubble
import com.augmentalis.avamagic.ui.foundation.OceanButton
import com.augmentalis.avamagic.ui.foundation.GlassChip
import com.augmentalis.avamagic.ui.foundation.GlassFloatingActionButton
import com.augmentalis.avamagic.ui.foundation.GlassIconButton

// Glass modifiers
import com.augmentalis.avamagic.ui.foundation.glass
import com.augmentalis.avamagic.ui.foundation.glassLight
import com.augmentalis.avamagic.ui.foundation.glassMedium
import com.augmentalis.avamagic.ui.foundation.glassHeavy
import com.augmentalis.avamagic.ui.foundation.glassFrosted

// Glass presets
import com.augmentalis.avamagic.ui.foundation.OceanGlass
import com.augmentalis.avamagic.ui.foundation.OceanGradients
import com.augmentalis.avamagic.ui.foundation.GlassDefaults
import com.augmentalis.avamagic.ui.foundation.GlassShapes

// Design tokens
import com.augmentalis.avamagic.ui.foundation.OceanDesignTokens

// GlassAvanue AR theme
import com.augmentalis.avamagic.components.themes.GlassAvanue
import com.augmentalis.avamagic.components.themes.AppContext

// ComponentProvider
import com.augmentalis.avamagic.ui.foundation.ComponentProvider
import com.augmentalis.avamagic.ui.foundation.OceanComponentProvider

// ServiceState
import com.augmentalis.foundation.state.ServiceState
import com.augmentalis.foundation.state.ServiceStateProvider
import com.augmentalis.foundation.state.LastHeardCommand

// Material3 wrapper
import com.avanueui.AvanueTheme
```

### 10.3 Legacy Locations (Deprecated)

These files exist but should NOT be used for new code:

```
Modules/WebAvanue/src/commonMain/kotlin/com/augmentalis/webavanue/
├── OceanDesignTokens.kt          # DEPRECATED - Use Foundation version
├── GlassmorphicComponents.kt     # DEPRECATED - Use Foundation version
└── ComponentProvider.kt          # DEPRECATED - Use Foundation version

Avanues/Web/common/webavanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/
└── presentation/design/          # DEPRECATED - All moved to AvanueUI
```

**Migration:** All new code should import from `Modules/AvanueUI/Foundation/` or `Modules/AvanueUI/Core/`.

---

## Summary

AvaUI (AvanueUI) is a comprehensive, token-based design system providing:

- **Glassmorphic Components**: Beautiful frosted glass UI elements
- **Design Tokens**: Atomic values ensuring consistency
- **Theme Systems**: OceanTheme (primary), MagicTheme (Material3), GlassAvanue (AR/VR)
- **ComponentProvider**: Abstraction layer for framework migration
- **Spatial Tokens**: AR/VR optimized sizing and spacing
- **ServiceState**: Cross-module lifecycle state management

**Key Principles:**
1. **Never hardcode colors/spacing** - always use tokens
2. **Use glassmorphic components** for consistent aesthetic
3. **Leverage ComponentProvider** for future-proof code
4. **Optimize for AR/VR** with spatial tokens and transparent themes
5. **Maintain theme consistency** across all platforms

**Next Steps:**
- Explore Chapter 64 (Ocean Glass Design System) for implementation details
- Review AVU DSL documentation (Chapters 81-87) for plugin-based theming
- Study Chapter 88 (Avanues Consolidated App) for real-world usage

---

**Related Documentation:**
- Chapter 64: Ocean Glass Design System
- Chapter 81-87: AVU DSL Evolution
- Chapter 88: Avanues Consolidated App
- ADR-009: Universal Theming System with Design Tokens
