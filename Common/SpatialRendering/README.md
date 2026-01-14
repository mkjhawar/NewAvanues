# SpatialRendering - Avanues Spatial Rendering Library

**Platform:** Kotlin Multiplatform (Android, iOS, Desktop)
**Package:** `com.avanues.spatial`
**Version:** 1.0.0-alpha

---

## Overview

Reusable spatial rendering library for creating curved, perspective-projected window layouts across AR and LCD displays. Optimized for both transparent AR glasses and regular 2D screens.

**Use Cases:**
- Multi-window spatial workspaces (Cockpit, AVA)
- AR window management (VoiceOS)
- Pseudo-spatial UIs on LCD screens
- Cross-platform spatial experiences

---

## Features

✅ **Display Type Detection** - Auto-detect AR glasses vs LCD screens
✅ **Responsive Sizing** - Adapts to phone/tablet/monitor/TV
✅ **Curved Projection** - Cylindrical wrap with perspective
✅ **Layout Presets** - Arc, Linear, Theater, Grid layouts
✅ **Platform-Agnostic** - Common API, platform implementations
✅ **Developer-Friendly** - Injectable, mockable, testable

---

## Installation

### Gradle (Kotlin DSL)

```kotlin
// settings.gradle.kts
include(":Common:SpatialRendering")

// app/build.gradle.kts
dependencies {
    implementation(project(":Common:SpatialRendering"))
}
```

---

## Quick Start

### 1. Get Display Information

```kotlin
// Android
val displayProvider = AndroidDisplayInfoProvider(context)
val metrics = displayProvider.getDisplayMetrics()
val config = displayProvider.getOptimalDisplayConfig()

println("Screen: ${metrics.widthPx}×${metrics.heightPx}")
println("Type: ${config.displayType}")  // AR_GLASSES or LCD_SCREEN
println("Category: ${metrics.category}")  // PHONE, TABLET, MONITOR, TV
```

### 2. Listen for Display Changes

```kotlin
displayProvider.addDisplayConfigListener { metrics, config ->
    // Update spatial rendering on rotation, external display, etc.
    updateSpatialLayout(config)
}

// Clean up
override fun onDestroy() {
    displayProvider.removeDisplayConfigListener(listener)
}
```

### 3. Calculate Window Dimensions

```kotlin
val layout = ArcFrontLayout
val dimensions = layout.calculateResponsiveDimensions(
    window = myWindow,
    index = 0,
    totalWindows = 3,
    displayConfig = config,
    screenWidthPx = metrics.widthPx,
    screenHeightPx = metrics.heightPx
)

println("Window size: ${dimensions.widthMeters}m × ${dimensions.heightMeters}m")
```

---

## Display Types

### AR Glasses Mode

**Characteristics:**
- Viewing distance: 3-5m
- FOV: 90-110° (wide immersion)
- Curve: Strong (0.9 cylindrical)
- Window sizing: Physical meters

**Optimized for:**
- Rokid Air, Xreal, Vision Pro
- Transparent AR displays
- Room-scale spatial experiences

### LCD Screen Mode

**Characteristics:**
- Viewing distance: 0.5-2m (arm's length)
- FOV: 60-75° (screen bounds)
- Curve: Subtle (0.3 perspective)
- Window sizing: Screen percentage

**Optimized for:**
- Phones, tablets, monitors, TVs
- Pseudo-spatial "depth" effect
- Responsive to screen size

**Auto-Detection:**

| Device | Width (px) | Distance | Arc Radius | Window Size |
|--------|------------|----------|------------|-------------|
| Phone | < 800 | 0.5m | 0.4m | 30% × 40% |
| Tablet | 800-1400 | 0.7m | 0.56m | 30% × 40% |
| Monitor | 1400-2400 | 0.8m | 0.64m | 30% × 40% |
| TV | > 2400 | 2.0m | 1.6m | 30% × 40% |

---

## API Reference

### DisplayInfoProvider

```kotlin
interface DisplayInfoProvider {
    fun getDisplayMetrics(): DisplayMetrics
    fun detectDisplayType(): DisplayType
    fun getOptimalDisplayConfig(): DisplayConfig
    fun addDisplayConfigListener(listener: DisplayConfigListener)
    fun removeDisplayConfigListener(listener: DisplayConfigListener)
}
```

### DisplayMetrics

```kotlin
data class DisplayMetrics(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Float,
    val densityScale: Float,
    val physicalSizeInches: Float,
    val refreshRateHz: Float,
    val isExternalDisplay: Boolean,
    val orientation: DisplayOrientation,
    val hasARCapability: Boolean,
    val category: DisplayCategory
)
```

### DisplayConfig

```kotlin
data class DisplayConfig(
    val displayType: DisplayType,
    val viewingDistance: Float,
    val fovHorizontal: Float,
    val fovVertical: Float,
    val curveStrength: Float,
    val windowSizeMode: WindowSizeMode
)
```

### DisplayType

```kotlin
enum class DisplayType {
    AR_GLASSES,     // Transparent AR displays
    LCD_SCREEN      // Regular 2D screens
}
```

### DisplayCategory

```kotlin
enum class DisplayCategory {
    PHONE,          // < 7" diagonal
    TABLET,         // 7" - 13"
    MONITOR,        // 13" - 32"
    TV,             // > 32"
    AR_GLASSES      // Transparent display
}
```

---

## Platform Implementations

### Android

```kotlin
class AndroidDisplayInfoProvider(context: Context) : DisplayInfoProvider {
    // Uses DisplayManager, WindowManager, ArCoreApk
}
```

**APIs Used:**
- `DisplayManager` - Primary display info source
- `WindowManager` - Legacy fallback
- `ArCoreApk` - AR capability detection
- `Configuration` - Orientation, UI mode

### iOS (Coming Soon)

```kotlin
class IOSDisplayInfoProvider : DisplayInfoProvider {
    // Uses UIScreen, UIDevice, ARKit
}
```

### Desktop (Coming Soon)

```kotlin
class DesktopDisplayInfoProvider : DisplayInfoProvider {
    // Uses GraphicsEnvironment, GraphicsDevice
}
```

---

## Examples

### Example 1: Basic Spatial Workspace

```kotlin
class SpatialWorkspaceActivity : ComponentActivity() {
    private lateinit var displayProvider: DisplayInfoProvider
    private lateinit var currentConfig: DisplayConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize display provider
        displayProvider = AndroidDisplayInfoProvider(this)
        currentConfig = displayProvider.getOptimalDisplayConfig()

        // Listen for changes
        displayProvider.addDisplayConfigListener { metrics, config ->
            currentConfig = config
            updateSpatialRendering(config)
        }

        setContent {
            SpatialWorkspace(config = currentConfig)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (displayProvider as? AndroidDisplayInfoProvider)?.dispose()
    }
}
```

### Example 2: Custom Display Detection

```kotlin
// Override auto-detection
val customConfig = when {
    isTablet() -> DisplayConfig.forLCDScreen(ScreenType.TABLET)
    isTV() -> DisplayConfig.forLCDScreen(ScreenType.TV)
    else -> displayProvider.getOptimalDisplayConfig()
}
```

### Example 3: Testing with Mock Provider

```kotlin
class MockDisplayInfoProvider : DisplayInfoProvider {
    override fun getDisplayMetrics() = DisplayMetrics(
        widthPx = 1080,
        heightPx = 2400,
        densityDpi = 420f,
        densityScale = 2.625f,
        physicalSizeInches = 6.7f,
        refreshRateHz = 120f,
        isExternalDisplay = false,
        orientation = DisplayOrientation.PORTRAIT,
        hasARCapability = false,
        category = DisplayCategory.PHONE
    )

    override fun detectDisplayType() = DisplayType.LCD_SCREEN
}

// Use in tests
val renderer = SpatialRenderer(MockDisplayInfoProvider())
```

---

## Architecture

```
Common/SpatialRendering/
├── src/
│   ├── commonMain/kotlin/com/avanues/spatial/
│   │   ├── display/
│   │   │   ├── DisplayInfoProvider.kt       # Interface
│   │   │   ├── DisplayMetrics.kt            # Data classes
│   │   │   ├── DisplayConfig.kt             # Configuration
│   │   │   └── DisplayType.kt               # Enums
│   │   ├── projection/
│   │   │   └── CurvedProjection.kt          # 3D → 2D projection
│   │   ├── layout/
│   │   │   ├── LayoutPreset.kt              # Interface
│   │   │   ├── ArcFrontLayout.kt            # Arc layout
│   │   │   ├── LinearLayout.kt              # Linear layout
│   │   │   └── TheaterLayout.kt             # Theater layout
│   │   └── rendering/
│   │       └── SpatialRenderer.kt           # Main renderer
│   │
│   ├── androidMain/kotlin/com/avanues/spatial/
│   │   └── display/
│   │       └── AndroidDisplayInfoProvider.kt  # Android impl
│   │
│   ├── iosMain/kotlin/com/avanues/spatial/
│   │   └── display/
│   │       └── IOSDisplayInfoProvider.kt      # iOS impl (TODO)
│   │
│   └── desktopMain/kotlin/com/avanues/spatial/
│       └── display/
│           └── DesktopDisplayInfoProvider.kt  # Desktop impl (TODO)
```

---

## Dependencies

### Common
- `kotlinx-coroutines-core:1.7.3`
- `kotlinx-serialization-json:1.6.0`

### Android
- `com.google.ar:core:1.40.0` (ARCore)
- `androidx.core:core-ktx:1.12.0`

### iOS
- ARKit (system framework)

### Desktop
- Java AWT (system library)

---

## Best Practices

### ✅ DO

- Inject `DisplayInfoProvider` for testability
- Listen for display config changes
- Dispose providers in `onDestroy()`
- Use `getOptimalDisplayConfig()` for auto-detection
- Mock provider in unit tests

### ❌ DON'T

- Hardcode display dimensions
- Assume fixed screen size
- Forget to unregister listeners
- Use platform-specific code in business logic
- Cache display metrics indefinitely

---

## Roadmap

### v1.0.0 (Current)
- ✅ Display type detection
- ✅ Android DisplayManager integration
- ✅ Responsive window sizing
- ✅ Arc layout preset

### v1.1.0
- ⏸️ iOS ARKit integration
- ⏸️ Desktop GraphicsDevice integration
- ⏸️ Linear and Theater layouts
- ⏸️ Grid layout preset

### v1.2.0
- ⏸️ Display intents/broadcasts
- ⏸️ Configuration persistence
- ⏸️ Performance optimizations
- ⏸️ Accessibility features

---

## Contributing

**Location:** `/Common/SpatialRendering/`
**Language:** Kotlin Multiplatform
**Style:** Follow Avanues coding standards

**Adding Platform Support:**
1. Create `{Platform}DisplayInfoProvider.kt` in `{platform}Main/`
2. Implement `DisplayInfoProvider` interface
3. Use platform-specific display APIs
4. Add tests in `{platform}Test/`

---

## License

Proprietary - Avanues Platform
Copyright © 2025 Avanues

---

## Contact

**Team:** Cockpit Development
**Docs:** `/Docs/Common/SpatialRendering/`
**Issues:** Report via `/i.issue` command

---

**Version:** 1.0.0-alpha
**Updated:** 2025-12-10
