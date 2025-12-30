# iOS SwiftUI Renderer - Developer Guide

**Version:** 1.0.0
**Last Updated:** 2025-11-19
**Component Count:** 81+ mappers

---

## Overview

The iOS SwiftUI Renderer converts AvaElements components into SwiftUI bridge models that can be consumed by native Swift code. It supports 81+ components with full parity to Android and Web renderers.

---

## Architecture

```
AvaElements Component (Kotlin)
        ↓
    SwiftUIRenderer
        ↓
    SwiftUIView (Bridge Model)
        ↓
    Swift AvaElementsView
        ↓
    Native SwiftUI
```

### Key Components

| Component | Purpose |
|-----------|---------|
| `SwiftUIRenderer` | Original renderer with when-based dispatch |
| `OptimizedSwiftUIRenderer` | Performance-optimized with HashMap dispatch |
| `SwiftUIView` | Bridge data model for Swift consumption |
| `SwiftUIModifier` | View modifier representations |
| `MapperRegistry` | O(1) mapper lookup for performance |

---

## Quick Start

### Basic Usage

```kotlin
// Create renderer
val renderer = SwiftUIRenderer()

// Apply theme
renderer.applyTheme(Themes.Material3Light)

// Render component
val button = ButtonComponent(
    label = "Click Me",
    onClick = { println("Clicked!") }
)
val swiftView = renderer.render(button) as SwiftUIView

// Pass to Swift code
// swiftView is now consumable by Swift
```

### Optimized Rendering (Recommended)

```kotlin
// Use optimized renderer for better performance
val renderer = OptimizedSwiftUIRenderer.withMaterial3()

// Single component
val view = renderer.render(component) as SwiftUIView

// Batch rendering for lists
val views = renderer.renderBatch(listOf(comp1, comp2, comp3))

// With profiling
val result = renderer.renderWithProfiling(component)
println("Render time: ${result.renderTimeMs}ms")
println("Cache hit rate: ${result.cacheStats.hitRate}")
```

### Extension Functions

```kotlin
// Direct conversion
val swiftView = myComponent.toSwiftUI(theme = Themes.iOS26LiquidGlass)

// Optimized conversion
val swiftView = myComponent.toOptimizedSwiftUI()
```

---

## Component Mappers

### File Organization

```
mappers/
├── BasicComponentMappers.kt      # Text, Button, TextField, etc.
├── LayoutMappers.kt              # Column, Row, Card, etc.
├── DataComponentMappers.kt       # Accordion, Timeline, DataGrid, etc.
├── Phase2FeedbackMappers.kt      # Alert, Toast, Snackbar, etc.
└── AdvancedComponentMappers.kt   # 48+ advanced components
```

### Creating a New Mapper

```kotlin
object MyCustomMapper {
    fun map(component: MyCustomComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "title" to component.title,
                "spacing" to 8
            ),
            modifiers = listOf(
                SwiftUIModifier.Padding(16f),
                SwiftUIModifier.Background(SwiftUIColor(1f, 1f, 1f, 1f)),
                SwiftUIModifier.CornerRadius(8f)
            ) + ModifierConverter.convert(component.modifiers),
            children = component.children.map { child ->
                // Recursive rendering
                renderChild(child)
            }
        )
    }
}
```

### Registering with OptimizedSwiftUIRenderer

```kotlin
// In OptimizedSwiftUIRenderer.registerAllMappers()

// Simple component (no children)
mapperRegistry.register<MyCustomComponent> { c, t ->
    MyCustomMapper.map(c, t)
}

// Component with children
mapperRegistry.registerWithChildren<MyCustomComponent> { c, t, r ->
    MyCustomMapper.map(c, t, r)
}
```

---

## SwiftUI Bridge Models

### ViewType Enum

```kotlin
enum class ViewType {
    // Layout
    VStack, HStack, ZStack, ScrollView, List,
    LazyVGrid, LazyHGrid,

    // Content
    Text, Button, TextField, Toggle, Image,

    // Feedback
    Alert, Sheet, ProgressView,

    // Navigation
    TabView, NavigationSplitView,

    // Shapes
    Rectangle, Circle, RoundedRectangle,

    // Special
    Spacer, Divider, EmptyView,

    // Custom (for complex components)
    Custom("ComponentName")
}
```

### SwiftUIModifier

```kotlin
// Layout modifiers
SwiftUIModifier.Padding(16f)
SwiftUIModifier.Padding(horizontal = 16f, vertical = 8f)
SwiftUIModifier.Frame(width = 100f, height = 50f)

// Appearance
SwiftUIModifier.Background(SwiftUIColor(1f, 0f, 0f, 1f))
SwiftUIModifier.ForegroundColor(SwiftUIColor.system("primary"))
SwiftUIModifier.CornerRadius(8f)
SwiftUIModifier.Shadow(radius = 4f, x = 0f, y = 2f)
SwiftUIModifier.Opacity(0.8f)

// Shape clipping
SwiftUIModifier.ClipShape("Circle()")
SwiftUIModifier.ClipShape("RoundedRectangle(cornerRadius: 12)")

// Border
SwiftUIModifier.Overlay(
    shape = "RoundedRectangle(cornerRadius: 8)",
    stroke = "Color.accentColor",
    lineWidth = 1f
)
```

### SwiftUIColor

```kotlin
// RGBA (0-1 range)
SwiftUIColor(red = 0.2f, green = 0.4f, blue = 0.8f, alpha = 1f)

// System colors
SwiftUIColor.system("primary")
SwiftUIColor.system("secondary")
SwiftUIColor.system("accentColor")

// Semantic colors for severity
val infoColor = SwiftUIColor(0.89f, 0.95f, 0.99f, 1f)
val successColor = SwiftUIColor(0.91f, 0.96f, 0.91f, 1f)
val warningColor = SwiftUIColor(1f, 0.97f, 0.88f, 1f)
val errorColor = SwiftUIColor(1f, 0.92f, 0.93f, 1f)
```

---

## Theming

### Available Themes

```kotlin
Themes.Material3Light      // Material Design 3 Light
Themes.Material3Dark       // Material Design 3 Dark
Themes.iOS26LiquidGlass    // iOS 26 Liquid Glass effect
Themes.visionOS2SpatialGlass // visionOS 2 Spatial Glass
```

### Applying Themes

```kotlin
val renderer = SwiftUIRenderer()
renderer.applyTheme(Themes.iOS26LiquidGlass)

// Access design tokens
val primaryColor = renderer.getThemeColor("primary")
val bodyFont = renderer.getThemeFont("body")
val mediumSpacing = renderer.getThemeSpacing("md")
val cardRadius = renderer.getThemeShape("card")
val cardShadow = renderer.getThemeElevation("card")

// Check material type
if (renderer.usesLiquidGlass()) {
    // Apply glass effects
}
```

---

## Performance Optimization

### OptimizedSwiftUIRenderer Features

1. **HashMap Dispatch (O(1))**: Uses MapperRegistry instead of when statement
2. **Component Caching**: Caches rendered views by hashCode
3. **Batch Rendering**: Optimized for list rendering
4. **Profiling**: Built-in timing and cache statistics

### Best Practices

```kotlin
// DO: Use OptimizedSwiftUIRenderer for production
val renderer = OptimizedSwiftUIRenderer.withMaterial3()

// DO: Batch render lists
val views = renderer.renderBatch(items)

// DO: Check cache stats during development
val stats = renderer.getCacheStats()
if (stats.hitRate < 0.5f) {
    // Consider why cache isn't being used
}

// DON'T: Create new renderer per component
// Bad:
items.forEach {
    val renderer = SwiftUIRenderer() // Creates new instance each time
    renderer.render(it)
}

// Good:
val renderer = OptimizedSwiftUIRenderer()
items.forEach { renderer.render(it) }
```

### Performance Targets

| Metric | Target | Measured |
|--------|--------|----------|
| Single component render | <1ms | ~0.5ms |
| 100 components | <16ms | ~10ms |
| Theme switch | <5ms | ~3ms |
| Cache hit rate | >70% | Variable |

---

## Swift Integration

### Consuming SwiftUIView in Swift

```swift
// SwiftUI View wrapper
struct AvaElementsView: View {
    let component: SwiftUIView

    var body: some View {
        renderView(component)
    }

    @ViewBuilder
    func renderView(_ view: SwiftUIView) -> some View {
        switch view.type {
        case .vStack:
            VStack(spacing: view.properties["spacing"] as? CGFloat ?? 0) {
                ForEach(view.children, id: \.id) { child in
                    renderView(child)
                }
            }
            .applyModifiers(view.modifiers)

        case .text:
            Text(view.properties["text"] as? String ?? "")
                .applyModifiers(view.modifiers)

        // ... other cases
        }
    }
}

// Modifier application
extension View {
    func applyModifiers(_ modifiers: [SwiftUIModifier]) -> some View {
        var view = self
        for modifier in modifiers {
            view = view.applyModifier(modifier)
        }
        return view
    }
}
```

---

## Testing

### Unit Testing Mappers

```kotlin
@Test
fun `ButtonMapper generates correct SwiftUI view`() {
    val button = ButtonComponent(
        id = "test-btn",
        label = "Click Me"
    )

    val result = ButtonMapper.map(button, null)

    assertEquals(ViewType.Button, result.type)
    assertEquals("test-btn", result.id)
    assertEquals("Click Me", result.properties["label"])
}
```

### Performance Testing

```kotlin
@Test
fun `renders 100 components under 16ms`() {
    val renderer = OptimizedSwiftUIRenderer()
    val components = (1..100).map { TextComponent(text = "Item $it") }

    val start = System.currentTimeMillis()
    renderer.renderBatch(components)
    val elapsed = System.currentTimeMillis() - start

    assertTrue(elapsed < 16, "Render took ${elapsed}ms, expected <16ms")
}
```

---

## Troubleshooting

### Common Issues

**Issue: Unknown component type**
```kotlin
// Error: "Unknown component: MyComponent"
// Solution: Register mapper in OptimizedSwiftUIRenderer.registerAllMappers()
```

**Issue: Modifiers not applying**
```kotlin
// Check ModifierConverter.convert() handles your modifier type
// Verify SwiftUIModifier types match Swift implementation
```

**Issue: Children not rendering**
```kotlin
// Use registerWithChildren() instead of register()
// Pass renderChild function to mapper
```

---

## API Reference

### SwiftUIRenderer

| Method | Description |
|--------|-------------|
| `applyTheme(theme: Theme)` | Set active theme |
| `render(component: Component): Any` | Render single component |
| `renderUI(ui: AvaUI): SwiftUIView?` | Render full UI tree |
| `getThemeColor(name: String)` | Get color token |
| `getThemeFont(name: String)` | Get font token |

### OptimizedSwiftUIRenderer

| Method | Description |
|--------|-------------|
| `render(component: Component): Any` | Cached render |
| `renderBatch(components: List<Component>)` | Batch render |
| `renderWithProfiling(component: Component)` | Render with timing |
| `clearCache()` | Clear render cache |
| `setCacheEnabled(enabled: Boolean)` | Toggle caching |
| `getCacheStats()` | Get cache statistics |

---

## Migration Guide

### From SwiftUIRenderer to OptimizedSwiftUIRenderer

```kotlin
// Before
val renderer = SwiftUIRenderer()
renderer.applyTheme(theme)
val view = renderer.render(component)

// After
val renderer = OptimizedSwiftUIRenderer()
renderer.applyTheme(theme)
val view = renderer.render(component)

// Functionally identical, but with:
// - O(1) dispatch
// - Automatic caching
// - Profiling support
```

---

**Created by:** Manoj Jhawar (manoj@ideahq.net)
**IDEACODE Version:** 8.4
