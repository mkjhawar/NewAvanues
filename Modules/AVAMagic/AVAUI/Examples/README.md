# AvaElements Examples

This directory contains examples demonstrating both **DSL** (Kotlin) and **YAML** syntax for AvaElements UI library.

## Examples Overview

### 1. Login Screen
Demonstrates a clean, modern login interface with form validation.

**DSL**: `DSLExample.kt::createLoginScreen()`
**YAML**: `login-screen.yaml`

**Features**:
- iOS 26 Liquid Glass theme
- TextField components with icons
- Primary and outlined button styles
- Proper spacing and alignment

**Platforms**: iOS, Android, macOS, Windows, Web

---

### 2. Settings Screen
Demonstrates a comprehensive settings interface with cards and form controls.

**DSL**: `DSLExample.kt::createSettingsScreen()`
**YAML**: `settings-screen.yaml`

**Features**:
- Material Design 3 theme
- Card-based layout
- Switch and Checkbox components
- ScrollView with nested layouts
- Multiple setting categories

**Platforms**: Android, iOS, Web, Desktop

---

### 3. Dashboard
Demonstrates a data-rich dashboard with statistics and activity feed.

**DSL**: `DSLExample.kt::createDashboard()`
**YAML**: `dashboard.yaml`

**Features**:
- Windows 11 Fluent 2 theme
- Stat cards with icons
- Activity feed
- Quick action buttons
- Complex nested layouts

**Platforms**: Windows, macOS, Linux, Web

---

### 4. visionOS Spatial UI
Demonstrates spatial computing interface for Apple Vision Pro.

**DSL**: `DSLExample.kt::createVisionOSUI()`
**YAML**: `visionos-ui.yaml` *(coming soon)*

**Features**:
- visionOS 2 Spatial Glass theme
- 3D-aware layouts
- Glass material effects
- Depth and layering

**Platforms**: visionOS, Android XR

---

## Running DSL Examples

```kotlin
import com.augmentalis.avaelements.examples.*

fun main() {
    // Create UI
    val loginUI = createLoginScreen()

    // Render to platform (Android example)
    val renderer = AndroidRenderer()
    val androidView = loginUI.render(renderer)

    // Use in your Activity/Fragment
    setContentView(androidView)
}
```

## Using YAML Examples

```kotlin
import com.augmentalis.avaelements.yaml.YamlParser

fun main() {
    // Load YAML file
    val yaml = File("login-screen.yaml").readText()

    // Parse to AvaUI
    val parser = YamlParser()
    val ui = parser.parse(yaml)

    // Render to platform
    val renderer = AndroidRenderer()
    val view = ui.render(renderer)
}
```

## Converting Between Formats

### YAML → DSL
```kotlin
import com.augmentalis.avaelements.yaml.YamlParser

val yaml = """
theme: Material3
components:
  - Button:
      text: "Click Me"
      style: Primary
"""

val parser = YamlParser()
val ui = parser.parse(yaml)
// Now use ui.render() or inspect ui.root
```

### DSL → YAML
```kotlin
import com.augmentalis.avaelements.yaml.YamlGenerator

val ui = AvaUI {
    theme = Themes.Material3Light
    Button("Click Me") {
        buttonStyle = ButtonScope.ButtonStyle.Primary
    }
}

val generator = YamlGenerator()
val yaml = generator.generate(ui)
println(yaml)
```

## Theme Comparison

| Theme | Platform | Key Features | Example |
|-------|----------|--------------|---------|
| **iOS 26 Liquid Glass** | iOS, macOS | Translucent glass, vibrant colors | Login Screen |
| **Material Design 3** | Android, Web | Dynamic color, tonal palettes | Settings Screen |
| **Windows 11 Fluent 2** | Windows, Desktop | Mica material, rounded corners | Dashboard |
| **visionOS 2 Spatial** | visionOS, XR | 3D depth, spatial panels | visionOS UI |

## Component Coverage

These examples demonstrate:

### Foundation (8 components)
- ✅ Button
- ✅ TextField
- ✅ Checkbox
- ✅ Switch
- ✅ Text
- ✅ Icon
- ✅ Image *(Dashboard)*
- ✅ Card

### Layout (6 components)
- ✅ Column
- ✅ Row
- ✅ Container
- ✅ ScrollView
- ✅ Stack *(coming soon)*
- ✅ Grid *(coming soon)*

### Advanced
- 🔲 Dialog
- 🔲 Toast
- 🔲 DatePicker
- 🔲 Slider
- 🔲 Dropdown

**Total**: 13/50 components demonstrated (26%)

## Next Steps

1. **Try the examples**: Run DSLExample.kt or parse the YAML files
2. **Modify them**: Change themes, colors, layouts
3. **Create your own**: Use these as templates for your UIs
4. **Mix and match**: Combine DSL and YAML in the same project

## Platform Renderers

Each platform has a dedicated renderer:

```kotlin
// Android (Jetpack Compose)
val renderer = AndroidRenderer()

// iOS (SwiftUI bridge)
val renderer = iOSRenderer()

// Desktop (Compose Desktop)
val renderer = DesktopRenderer()

// Web (Kotlin/JS + React)
val renderer = WebRenderer()

// visionOS (SwiftUI + RealityKit)
val renderer = VisionOSRenderer()
```

## Best Practices

### When to use DSL
- ✅ Type-safe, compile-time checked
- ✅ IDE autocomplete and refactoring
- ✅ Complex event handling
- ✅ Dynamic UIs with conditional logic
- ✅ Developer-centric workflows

### When to use YAML
- ✅ Designer-friendly syntax
- ✅ Hot-reload during development
- ✅ Server-driven UIs
- ✅ A/B testing and feature flags
- ✅ Localization and theming

## Resources

- **Specification**: `../MAGICELEMENTS_SPECIFICATION.md`
- **Platform Themes**: `../PLATFORM_THEMES_SPEC.md`
- **API Documentation**: *(coming soon)*
- **Migration Guide**: *(coming soon)*

## Contributing

To add a new example:

1. Create DSL version in `DSLExample.kt`
2. Create equivalent YAML in `[name].yaml`
3. Add entry to this README
4. Test on at least 2 platforms

---

**Last Updated**: 2025-10-29
**AvaElements Version**: 1.0.0-alpha
