# AvaElements iOS SwiftUI Renderer

The iOS renderer for AvaElements that converts cross-platform UI components to native SwiftUI views using Kotlin/Native.

## Overview

This module provides a bridge between Kotlin AvaElements components and iOS SwiftUI, enabling you to:

- Define UIs once in Kotlin using AvaElements DSL
- Render them natively in SwiftUI on iOS
- Support iOS 26 Liquid Glass theme and visionOS Spatial Glass
- Maintain full type safety across the Kotlin/Swift boundary
- Access iOS-specific design tokens and themes

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    AvaElements Core                        │
│                   (Kotlin Multiplatform)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Components  │  │    Theme     │  │   Modifiers  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              iOS SwiftUI Renderer (Kotlin/Native)            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              SwiftUIRenderer.kt                       │  │
│  │  • Converts components to SwiftUIView bridge models  │  │
│  │  • Applies iOS themes and design tokens              │  │
│  │  • Maps modifiers to SwiftUI equivalents             │  │
│  └──────────────────────────────────────────────────────┘  │
│                              ▼                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          SwiftUIView Bridge Models                    │  │
│  │  • ViewType, SwiftUIModifier, SwiftUIColor           │  │
│  │  • Consumed by Swift code                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                Swift Integration Layer                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         AvaElementsView.swift                       │  │
│  │  • Renders SwiftUIView models as native SwiftUI      │  │
│  │  • Applies modifiers and styling                     │  │
│  │  • Handles user interactions                         │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ▼
                    Native SwiftUI Views
```

## Features

### Component Support

All 13 AvaElements components are fully supported:

**Layout Components:**
- ✅ Column → VStack
- ✅ Row → HStack
- ✅ Container → ZStack
- ✅ ScrollView → ScrollView
- ✅ Card → RoundedRectangle + VStack with elevation

**Basic Components:**
- ✅ Text → Text
- ✅ Button → Button
- ✅ TextField → TextField
- ✅ Checkbox → Toggle (checkbox style)
- ✅ Switch → Toggle
- ✅ Icon → Image (SF Symbols)
- ✅ Image → AsyncImage

### Theme Support

- ✅ iOS 26 Liquid Glass theme
- ✅ visionOS 2 Spatial Glass theme
- ✅ Material Design 3 (cross-platform)
- ✅ Custom theme conversion
- ✅ Design token generation

### Modifier Support

- ✅ Padding (uniform and edge-specific)
- ✅ Background colors and gradients
- ✅ Borders and corner radius
- ✅ Shadows and elevation
- ✅ Opacity and visibility
- ✅ Size constraints (fillMaxWidth, fillMaxHeight, etc.)
- ✅ Font styling and typography
- ✅ Foreground colors

## Installation

### 1. Add Gradle Dependency

In your `build.gradle.kts`:

```kotlin
kotlin {
    ios()
    iosSimulatorArm64()

    sourceSets {
        val iosMain by getting {
            dependencies {
                implementation(project(":AvaElements:Renderers:iOS"))
            }
        }
    }
}
```

### 2. Build iOS Framework

```bash
cd AvaElements/Renderers/iOS
./gradlew buildIOSFramework
```

This generates:
- Debug framework: `build/bin/ios/debugFramework/AvaElementsiOS.framework`
- Release framework: `build/bin/ios/releaseFramework/AvaElementsiOS.framework`

### 3. Build XCFramework (for distribution)

```bash
./gradlew buildXCFramework
```

Output: `build/xcframework/AvaElementsiOS.xcframework`

### 4. Add Framework to Xcode

1. Drag `AvaElementsiOS.framework` into your Xcode project
2. Ensure it's added to "Frameworks, Libraries, and Embedded Content"
3. Set to "Embed & Sign"

### 5. Add Swift Bridge Files

Copy these files to your Xcode project:
- `AvaElementsView.swift` (see SWIFT_INTEGRATION.md)
- `ModifierExtensions.swift` (see SWIFT_INTEGRATION.md)

## Usage

### Kotlin Side

#### Example 1: Basic Usage

```kotlin
import com.augmentalis.avaelements.dsl.*
import com.augmentalis.avaelements.renderer.ios.*

fun createLoginScreen(): SwiftUIView {
    val ui = AvaUI {
        theme = Themes.iOS26LiquidGlass

        Column {
            padding(24f)

            Text("Welcome Back") {
                font = Font.Title
            }

            TextField("", "Email") {
                fillMaxWidth()
            }

            Button("Sign In") {
                fillMaxWidth()
                onClick = { println("Login clicked") }
            }
        }
    }

    return SwiftUIRenderer.withLiquidGlass().renderUI(ui)!!
}
```

#### Example 2: Using Component Mappers

```kotlin
val component = TextComponent(
    text = "Hello, iOS!",
    font = Font.Title,
    color = Color.Blue,
    // ... other properties
)

val renderer = SwiftUIRenderer()
renderer.applyTheme(Themes.iOS26LiquidGlass)
val swiftView = renderer.render(component) as SwiftUIView
```

#### Example 3: Accessing Theme Tokens

```kotlin
val renderer = SwiftUIRenderer.withLiquidGlass()

// Get design tokens
val primaryColor = renderer.getThemeColor("primary")
val mediumSpacing = renderer.getThemeSpacing("md")
val largeCornerRadius = renderer.getThemeShape("large")

// Check material effects
if (renderer.usesLiquidGlass()) {
    val material = renderer.getMaterialTokens()
    println("Blur radius: ${material?.glassBlurRadius}")
}
```

### Swift Side

#### Example 1: Basic Rendering

```swift
import SwiftUI
import AvaElementsiOS

struct ContentView: View {
    let loginScreen = IOSExampleKt.createiOSLoginScreen()

    var body: some View {
        AvaElementsView(component: loginScreen)
    }
}
```

#### Example 2: With iOS 26 Liquid Glass Effect

```swift
struct LiquidGlassView: View {
    let content = IOSExampleKt.createiOSLoginScreen()

    var body: some View {
        AvaElementsView(component: content)
            .background(.ultraThinMaterial)  // Native iOS 26 blur
            .cornerRadius(20)
    }
}
```

#### Example 3: With ViewModel

```swift
class AppViewModel: ObservableObject {
    private let renderer = SwiftUIRenderer.companion.withLiquidGlass()
    @Published var currentView: SwiftUIView?

    func loadLoginScreen() {
        currentView = IOSExampleKt.createiOSLoginScreen()
    }

    func loadSettingsScreen() {
        currentView = IOSExampleKt.createiOSSettingsScreen()
    }
}

struct AppView: View {
    @StateObject private var viewModel = AppViewModel()

    var body: some View {
        if let view = viewModel.currentView {
            AvaElementsView(component: view)
        }
    }
}
```

## Component Mapping Reference

| AvaElements | SwiftUI | Notes |
|---------------|---------|-------|
| Column | VStack | Supports spacing and alignment |
| Row | HStack | Supports spacing and alignment |
| Container | ZStack | Supports alignment |
| ScrollView | ScrollView | Supports vertical/horizontal |
| Card | VStack + modifiers | Includes elevation shadows |
| Text | Text | Full font styling support |
| Button | Button | Supports styles and icons |
| TextField | TextField | Supports labels and icons |
| Checkbox | Toggle | Uses checkbox style |
| Switch | Toggle | Uses switch style |
| Icon | Image | Uses SF Symbols |
| Image | AsyncImage | Network image loading |

## Theme Mapping

### iOS 26 Liquid Glass

```kotlin
Themes.iOS26LiquidGlass
```

Maps to:
- SF Pro fonts
- iOS system colors
- Continuous corner radius (14-30pt)
- Glass material with 0.7 opacity
- 30pt blur radius

### visionOS 2 Spatial Glass

```kotlin
Themes.visionOS2SpatialGlass
```

Maps to:
- SF Pro fonts
- Translucent surfaces (0.5 opacity)
- 40pt blur radius
- Spatial depth: 100dp
- Glass effects with subtle tints

## API Reference

### SwiftUIRenderer

```kotlin
class SwiftUIRenderer : Renderer {
    // Apply theme
    fun applyTheme(theme: Theme)

    // Render component
    fun render(component: Component): SwiftUIView

    // Render AvaUI
    fun renderUI(ui: AvaUI): SwiftUIView?

    // Get theme tokens
    fun getThemeColor(name: String): SwiftUIColor?
    fun getThemeFont(name: String): FontDefinition?
    fun getThemeShape(name: String): Float?
    fun getThemeSpacing(name: String): Float?
    fun getThemeElevation(name: String): ShadowValue?

    // Check material effects
    fun usesLiquidGlass(): Boolean
    fun usesSpatialGlass(): Boolean
    fun getMaterialTokens(): MaterialTokens?

    companion object {
        fun withLiquidGlass(): SwiftUIRenderer
        fun withSpatialGlass(): SwiftUIRenderer
        fun withMaterial3(): SwiftUIRenderer
    }
}
```

### SwiftUIView Bridge Model

```kotlin
data class SwiftUIView(
    val type: ViewType,
    val id: String?,
    val properties: Map<String, Any>,
    val modifiers: List<SwiftUIModifier>,
    val children: List<SwiftUIView>
)
```

### Extension Functions

```kotlin
// Render component directly
fun Component.toSwiftUI(theme: Theme? = null): SwiftUIView

// Render AvaUI directly
fun AvaUI.toSwiftUI(): SwiftUIView?
```

## Examples

See [iOSExample.kt](src/iosMain/kotlin/com/augmentalis/avaelements/renderer/ios/iOSExample.kt) for complete examples:

1. **Login Screen** - iOS 26 Liquid Glass themed login
2. **Settings Screen** - Card layouts with toggles
3. **visionOS Welcome** - Spatial glass effects
4. **Profile Card** - Component-level rendering
5. **Theme Tokens** - Working with design tokens

## Performance Considerations

### Optimization Tips

1. **Cache Rendered Views**: Don't recreate SwiftUIView on every render
2. **Theme Application**: Apply theme once during initialization
3. **State Management**: Use Swift's native state management
4. **Large Lists**: Use LazyVStack/LazyHStack in Swift for performance

### Memory Management

- Bridge models are lightweight data classes
- Kotlin/Native handles memory automatically
- Swift ARC manages view lifecycle

## Troubleshooting

### Framework Not Found

**Issue**: `Module 'AvaElementsiOS' not found`

**Solution**:
1. Ensure framework is built: `./gradlew buildIOSFramework`
2. Check framework is added to Xcode target
3. Clean build folder in Xcode

### Type Casting Issues

**Issue**: Cannot cast SwiftUIView properties

**Solution**: Use proper type checking in Swift:
```swift
if let spacing = component.properties["spacing"] as? CGFloat {
    // Use spacing
}
```

### Theme Not Applied

**Issue**: Components don't reflect theme colors

**Solution**: Ensure theme is applied before rendering:
```kotlin
val renderer = SwiftUIRenderer()
renderer.applyTheme(Themes.iOS26LiquidGlass)  // Apply first
val view = renderer.render(component)
```

## Platform-Specific Notes

### iOS 26 Liquid Glass

- Use `.background(.ultraThinMaterial)` in SwiftUI for glass effect
- Continuous corner radius: `.clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))`
- Vibrancy: Automatically applied to text over glass surfaces

### visionOS 2

- Add `.frame(depth: 100)` for spatial depth
- Use `.glassBackgroundEffect()` for glass material
- Consider spatial layout with Z-axis positioning

## Build Configuration

The module is configured for:
- iOS 14.0+
- Xcode 14.0+
- Kotlin 1.9.0+
- Kotlin/Native for iOS targets

## Contributing

When adding new components or modifiers:

1. Update bridge models in `SwiftUIModels.kt`
2. Create mapper in `mappers/` directory
3. Update `SwiftUIRenderer.kt` to handle new type
4. Add Swift rendering in `AvaElementsView.swift`
5. Update this README with examples

## License

Part of the AvaElements library.
See main AvaElements LICENSE for details.

## See Also

- [SWIFT_INTEGRATION.md](SWIFT_INTEGRATION.md) - Detailed Swift integration guide
- [Core Documentation](../../Core/README.md) - AvaElements core concepts
- [Master Plan](../../MASTER_PLAN.md) - Complete project roadmap
- [Phase 1 Completion](../../PHASE1_COMPLETION_SUMMARY.md) - Core implementation details

## Support

For issues, questions, or contributions related to the iOS renderer, please refer to the main AvaElements repository.
