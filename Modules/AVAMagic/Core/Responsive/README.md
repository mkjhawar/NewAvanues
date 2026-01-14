# MagicUI Responsive System

**DeviceManager-Powered Responsive Design for MagicUI**

## Overview

The MagicUI Responsive System integrates VoiceOS DeviceManager with MagicUI to provide comprehensive device-aware responsive design capabilities. Components automatically adapt to device type, screen size, orientation, and foldable states.

## Features

✅ **Full Device Awareness** - Direct integration with DeviceManager
✅ **Breakpoint System** - Material Design 3 breakpoints (XS/SM/MD/LG/XL)
✅ **Device Type Detection** - Phone, Tablet, Foldable, Wearable, TV, Desktop, XR
✅ **Foldable Support** - Hinge angle, posture, crease avoidance
✅ **Orientation Tracking** - Portrait/Landscape detection
✅ **Backward Compatible** - Existing components continue working

## Architecture

```
┌─────────────────────────────────────────────┐
│         MagicUI Component                   │
│  ┌───────────────────────────────────────┐  │
│  │   ResponsiveProvider                  │  │
│  │  (provides ResponsiveContext)         │  │
│  └────────────────┬──────────────────────┘  │
│                   │                          │
│  ┌────────────────▼──────────────────────┐  │
│  │   ResponsiveModifiers                 │  │
│  │   - WidthByBreakpoint                 │  │
│  │   - HeightByBreakpoint                │  │
│  │   - WidthByDeviceType                 │  │
│  │   - FoldableAware                     │  │
│  │   - OrientationAware                  │  │
│  └────────────────┬──────────────────────┘  │
│                   │                          │
│  ┌────────────────▼──────────────────────┐  │
│  │   DeviceManager (VoiceOS)             │  │
│  │   - DisplayProfile                    │  │
│  │   - ScalingProfile                    │  │
│  │   - FoldableDeviceManager             │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

## Quick Start

### 1. Wrap Your App with ResponsiveProvider

```kotlin
@Composable
fun App() {
    ResponsiveProvider {
        // Your app content
        MyScreen()
    }
}
```

### 2. Use Responsive Helpers

```kotlin
@Composable
fun MyScreen() {
    // Check device type
    if (isPhone()) {
        PhoneLayout()
    } else if (isTablet()) {
        TabletLayout()
    }

    // Check breakpoint
    val columns = responsive(
        xs = 1,
        sm = 2,
        md = 3,
        lg = 4,
        xl = 5,
        default = 1
    )

    // Check orientation
    if (isPortrait()) {
        VerticalLayout()
    } else {
        HorizontalLayout()
    }
}
```

### 3. Use ResponsiveModifiers

```kotlin
@Composable
fun AdaptiveCard() {
    Card(
        modifier = ResponsiveModifier.WidthByBreakpoint(
            xs = Size.Fill,                    // Full width on phones
            sm = Size.Percent(80f),            // 80% on small tablets
            md = Size.Fixed(600f, Size.Unit.DP), // Fixed 600dp on tablets
            lg = Size.Fixed(800f, Size.Unit.DP)  // Fixed 800dp on desktops
        ).toComposeModifier()
    ) {
        // Card content
    }
}
```

## Responsive Modifiers

### WidthByBreakpoint / HeightByBreakpoint

Adapt size based on screen width breakpoints:

```kotlin
ResponsiveModifier.WidthByBreakpoint(
    xs = Size.Fill,              // <600dp
    sm = Size.Percent(90f),      // 600-839dp
    md = Size.Fixed(700f),       // 840-1239dp
    lg = Size.Fixed(900f),       // 1240-1439dp
    xl = Size.Fixed(1200f)       // ≥1440dp
)
```

### WidthByDeviceType

Adapt based on device type:

```kotlin
ResponsiveModifier.WidthByDeviceType(
    phone = Size.Fill,
    tablet = Size.Fixed(600f),
    desktop = Size.Fixed(1000f),
    foldable = Size.Percent(80f)
)
```

### FoldableAware

Adapt to foldable device state:

```kotlin
ResponsiveModifier.FoldableAware(
    closed = Size.Fill,
    open = Size.Percent(50f),
    avoidCrease = true
)
```

### OrientationAware

Adapt to device orientation:

```kotlin
ResponsiveModifier.OrientationAware(
    portrait = Size.Fill,
    landscape = Size.Fixed(600f)
)
```

### PaddingByBreakpoint

Adaptive padding:

```kotlin
ResponsiveModifier.PaddingByBreakpoint(
    xs = 8f,
    sm = 12f,
    md = 16f,
    lg = 24f,
    xl = 32f
)
```

## Responsive Helper Functions

### Device Type Checks

```kotlin
@Composable
fun Example() {
    when {
        isPhone() -> PhoneUI()
        isTablet() -> TabletUI()
        isDesktop() -> DesktopUI()
        isFoldable() -> FoldableUI()
        isWearable() -> WearableUI()
        isTV() -> TVUI()
        isXR() -> XRUI()
    }
}
```

### Breakpoint Checks

```kotlin
@Composable
fun Example() {
    when {
        isCompact() -> CompactUI()    // XS
        isMedium() -> MediumUI()      // SM/MD
        isExpanded() -> ExpandedUI()  // LG/XL
    }
}
```

### Conditional Composables

```kotlin
@Composable
fun Example() {
    OnPhone {
        Text("Only shown on phones")
    }

    OnTablet {
        Text("Only shown on tablets")
    }

    OnPortrait {
        VerticalLayout()
    }

    OnLandscape {
        HorizontalLayout()
    }
}
```

### Responsive Values

```kotlin
@Composable
fun Example() {
    val fontSize = responsive(
        xs = 14.sp,
        sm = 16.sp,
        md = 18.sp,
        lg = 20.sp,
        xl = 22.sp,
        default = 16.sp
    )

    Text("Responsive Text", fontSize = fontSize)
}
```

## Access Device Metrics

```kotlin
@Composable
fun Example() {
    val context = requireResponsiveContext()

    // Device metrics
    val metrics = context.metrics
    println("Screen: ${metrics.widthPixels}x${metrics.heightPixels}")
    println("Density: ${metrics.density}")
    println("DPI: ${metrics.densityDpi}")
    println("Diagonal: ${metrics.diagonalInches} inches")

    // Breakpoint
    val breakpoint = context.breakpoint
    println("Current breakpoint: $breakpoint")

    // Device type
    val deviceType = context.deviceType
    println("Device type: $deviceType")

    // Foldable state
    val foldState = context.foldState
    if (foldState != null) {
        println("Hinge angle: ${foldState.hingeAngle}")
        println("Should avoid crease: ${foldState.shouldAvoidCrease}")
    }
}
```

## Responsive Design Tokens

Use pre-defined responsive tokens from `DesignTokens.kt`:

```kotlin
import com.augmentalis.avanues.avamagic.designsystem.ResponsiveTokens

// Breakpoint thresholds
ResponsiveTokens.BreakpointXS   // 0dp
ResponsiveTokens.BreakpointSM   // 600dp
ResponsiveTokens.BreakpointMD   // 840dp
ResponsiveTokens.BreakpointLG   // 1240dp
ResponsiveTokens.BreakpointXL   // 1440dp

// Device-specific spacing
ResponsiveTokens.SpacingByDevice.PhoneCompact    // 8dp
ResponsiveTokens.SpacingByDevice.TabletMedium   // 24dp
ResponsiveTokens.SpacingByDevice.DesktopLarge   // 48dp

// Max content widths
ResponsiveTokens.MaxContentWidth.XS  // 360dp
ResponsiveTokens.MaxContentWidth.MD  // 840dp
ResponsiveTokens.MaxContentWidth.XL  // 1440dp

// Grid columns
ResponsiveTokens.GridColumns.XS  // 4
ResponsiveTokens.GridColumns.MD  // 12
```

## Examples

### Adaptive Layout

```kotlin
@Composable
fun AdaptiveLayout() {
    ResponsiveProvider {
        if (isTablet() || isDesktop()) {
            TwoColumnLayout()
        } else {
            SingleColumnLayout()
        }
    }
}

@Composable
fun TwoColumnLayout() {
    Row {
        Column(modifier = Modifier.weight(1f)) {
            MainContent()
        }
        Column(modifier = Modifier.width(300.dp)) {
            Sidebar()
        }
    }
}
```

### Responsive Grid

```kotlin
@Composable
fun ResponsiveGrid() {
    val columns = responsive(
        xs = 1,
        sm = 2,
        md = 3,
        lg = 4,
        xl = 5,
        default = 1
    )

    LazyVerticalGrid(columns = GridCells.Fixed(columns)) {
        items(items) { item ->
            GridItem(item)
        }
    }
}
```

### Foldable-Aware UI

```kotlin
@Composable
fun FoldableUI() {
    val foldState = currentFoldState()

    if (foldState?.isOpen == true) {
        Row {
            // Left panel
            Column(modifier = Modifier.weight(1f)) {
                LeftContent()
            }

            // Avoid crease area
            if (foldState.shouldAvoidCrease) {
                Spacer(modifier = Modifier.width(32.dp))
            }

            // Right panel
            Column(modifier = Modifier.weight(1f)) {
                RightContent()
            }
        }
    } else {
        SinglePanelUI()
    }
}
```

## Dependencies

Add to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":Modules:AVAMagic:Core:Responsive"))
    implementation(project(":Modules:VoiceOS:libraries:DeviceManager"))
}
```

## Benefits

1. **Device-Aware**: Leverage DeviceManager's comprehensive device detection
2. **Type-Safe**: Kotlin DSL with compile-time safety
3. **Declarative**: Compose-first API
4. **Efficient**: Cached device info, minimal recomposition
5. **Foldable-Ready**: First-class support for foldable devices
6. **Material Design 3**: Standard breakpoints and responsive patterns

## License

Copyright © 2025 Augmentalis. All rights reserved.

---

**Author:** Manoj Jhawar
**Version:** 1.0.0
**Created:** 2025-12-23
