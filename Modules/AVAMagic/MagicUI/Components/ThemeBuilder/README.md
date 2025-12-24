# AvaElements Theme Builder

A visual theme editor for the AvaElements UI library with live preview and multi-format export capabilities.

## Overview

The Theme Builder is a cross-platform desktop application built with Compose Desktop that enables designers and developers to create, edit, and export custom themes for AvaElements applications.

### Features

- **Visual Theme Editor**: Intuitive three-panel interface for editing all theme properties
- **Live Preview**: Real-time component preview as you edit theme properties
- **Component Gallery**: Browse and preview all 13 AvaElements components
- **Property Inspector**: Edit colors, typography, spacing, shapes, and elevation
- **Multi-Format Export**: Export themes to Kotlin DSL, YAML, JSON, CSS, and Android XML
- **Theme Presets**: Quick-load Material 3, iOS 26, Windows 11, and visionOS themes
- **Undo/Redo**: Full history support with 50 levels of undo
- **Auto-Save**: Automatic theme saving every 30 seconds
- **Theme Validation**: Built-in accessibility and consistency checks
- **Dark Mode Preview**: Toggle between light and dark mode previews
- **Grid Overlay**: Visual grid and spacing guides

## Architecture

### Module Structure

```
ThemeBuilder/
├── src/
│   ├── commonMain/kotlin/
│   │   └── com/augmentalis/avaelements/themebuilder/
│   │       ├── UI/
│   │       │   ├── EditorWindow.kt          # Main editor window
│   │       │   ├── PreviewCanvas.kt         # Live component preview
│   │       │   └── PropertyInspector.kt     # Property editing panel
│   │       ├── Engine/
│   │       │   └── ThemeCompiler.kt         # Theme export compiler
│   │       └── State/
│   │           └── ThemeState.kt            # Editor state management
│   └── jvmMain/kotlin/
│       └── com/augmentalis/avaelements/themebuilder/
│           └── Main.kt                      # Desktop app entry point
├── build.gradle.kts                         # Build configuration
└── README.md                                # This file
```

### Core Components

#### 1. ThemeEditorWindow

The main editor coordinator that manages:
- Three-panel layout (Gallery | Preview | Inspector)
- Theme loading and saving
- Export functionality
- Undo/redo operations
- Validation

```kotlin
val editorWindow = ThemeEditorWindow()
editorWindow.initialize(Themes.Material3Light)
editorWindow.exportTheme(ExportFormat.DSL)
```

#### 2. PreviewCanvas

Renders components with the current theme:
- All 13 foundation and layout components
- Multiple component states (default, hover, pressed, disabled)
- Preview scenes (Login, Settings, Dashboard, Gallery)
- Screen size simulation (Mobile, Tablet, Desktop)

```kotlin
val previewCanvas = PreviewCanvas(stateManager)
val scene = previewCanvas.generatePreviewScene(PreviewScene.LOGIN)
val componentData = previewCanvas.renderComponent("Button")
```

#### 3. PropertyInspector

Editable theme properties:
- **Color Scheme**: Primary, secondary, surface, background, error colors
- **Typography**: Font families, sizes, weights for all text styles
- **Shapes**: Corner radius for small, medium, large components
- **Spacing**: XS, SM, MD, LG, XL, XXL spacing values
- **Elevation**: Shadow properties for 6 elevation levels
- **Material Effects**: Glass, mica, and spatial materials

```kotlin
val inspector = PropertyInspector(stateManager)
inspector.updateProperty("colorScheme.primary", Color.hex("#007AFF"))
val currentValue = inspector.getCurrentValue("spacing.md")
```

#### 4. ThemeCompiler

Compiles themes to multiple formats:

**Kotlin DSL:**
```kotlin
val myTheme = Theme(
    name = "My Custom Theme",
    platform = ThemePlatform.Material3_Expressive,
    colorScheme = ColorScheme(
        primary = Color.hex("#007AFF"),
        onPrimary = Color.White,
        // ...
    )
)
```

**YAML:**
```yaml
Theme:
  name: "My Custom Theme"
  colorScheme:
    primary: "#007AFF"
    onPrimary: "#FFFFFF"
```

**JSON:**
```json
{
  "name": "My Custom Theme",
  "platform": "Material3_Expressive",
  "colorScheme": {
    "primary": "#007AFF"
  }
}
```

**CSS Variables:**
```css
:root {
  --color-primary: rgb(0, 122, 255);
  --color-on-primary: rgb(255, 255, 255);
  --spacing-md: 16px;
}
```

#### 5. ThemeState

State management with reactive updates:
- Theme editing state
- Selected component and properties
- Preview mode and screen size
- Dirty flag and auto-save
- Undo/redo history (50 levels)

```kotlin
val stateManager = ThemeBuilderStateManager()
stateManager.updateColorScheme(newColorScheme)
stateManager.undo()
stateManager.redo()
```

## Getting Started

### Prerequisites

- JDK 17 or higher
- Gradle 8.0+
- AvaElements Core module

### Building

```bash
# Navigate to ThemeBuilder directory
cd /Volumes/M\ Drive/Coding/Avanues/Universal/Libraries/AvaElements/ThemeBuilder

# Build the desktop application
./gradlew :ThemeBuilder:packageDmg      # macOS
./gradlew :ThemeBuilder:packageMsi      # Windows
./gradlew :ThemeBuilder:packageDeb      # Linux
```

### Running

```bash
# Run directly from Gradle
./gradlew :ThemeBuilder:run

# Or run the packaged application
open build/compose/binaries/main/dmg/AvaElements\ Theme\ Builder-1.0.0.dmg
```

## Usage

### 1. Launch the Theme Builder

Run the application using one of the methods above. The main window will open with:
- **Left Panel**: Component Gallery
- **Center Panel**: Preview Canvas
- **Right Panel**: Property Inspector

### 2. Select a Starting Theme

Click the palette icon in the toolbar to load a preset:
- Material Design 3
- iOS 26 Liquid Glass
- Windows 11 Fluent 2
- visionOS 2 Spatial Glass

Or start with the default Material 3 theme.

### 3. Edit Theme Properties

In the Property Inspector (right panel), you can edit:

#### Color Scheme
- Click color swatches to change colors
- Use the color picker or enter hex values
- Generate color palettes from a seed color

#### Typography
- Select font families from the dropdown
- Adjust font sizes with sliders
- Choose font weights

#### Spacing
- Adjust spacing values (XS through XXL)
- Preview spacing guides in the canvas

#### Shapes
- Set corner radius for small, medium, large components
- See changes reflected immediately

#### Elevation
- Configure shadow blur and offset
- Preview elevation levels on cards

### 4. Preview Components

In the Component Gallery (left panel):
- Click a component to preview it
- See all component states (default, hover, pressed, disabled)
- Switch between preview scenes:
  - Login Screen
  - Settings Screen
  - Dashboard
  - Component Gallery

### 5. Preview Options

Toggle preview features:
- **Dark Mode**: Preview theme in dark mode
- **Grid Overlay**: Show 12-column grid
- **Spacing Guides**: Visualize spacing values
- **Screen Sizes**: Switch between Mobile, Tablet, Desktop

### 6. Export Theme

Click the "Export" button in the toolbar:
1. Select export format (DSL, YAML, JSON, CSS, Android XML)
2. Click "Generate" to compile the theme
3. Copy the generated code
4. Use in your AvaElements application

### 7. Save Theme

Click "Save" in the toolbar to persist your theme. The app auto-saves every 30 seconds when changes are detected.

### 8. Undo/Redo

Use the undo/redo buttons or keyboard shortcuts:
- **Undo**: Ctrl+Z (Cmd+Z on Mac)
- **Redo**: Ctrl+Y (Cmd+Y on Mac)

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+Z | Undo last change |
| Ctrl+Y | Redo last undone change |
| Ctrl+S | Save current theme |
| Ctrl+E | Export theme |
| Ctrl+D | Toggle dark mode preview |
| Ctrl+G | Toggle grid overlay |
| Ctrl+Shift+G | Toggle spacing guides |
| Ctrl+R | Reset to default theme |

## Integration with AvaElements

### Using Exported Themes

#### Kotlin DSL

```kotlin
// Copy the exported DSL code
val myCustomTheme = Theme(
    name = "My Custom Theme",
    // ... exported properties
)

// Use in your app
AvaUI(theme = myCustomTheme) {
    // Your UI components
}
```

#### YAML

```yaml
# Save exported YAML to themes/my-theme.yaml
Theme:
  name: "My Custom Theme"
  # ... exported properties
```

```kotlin
// Load in your app
val theme = YamlParser.parseTheme(File("themes/my-theme.yaml"))
AvaUI(theme = theme) {
    // Your UI components
}
```

#### JSON

```json
// Save to themes/my-theme.json
{
  "name": "My Custom Theme",
  ...
}
```

```kotlin
// Load in your app
val theme = Json.decodeFromString<Theme>(
    File("themes/my-theme.json").readText()
)
```

## Theme Validation

The Theme Builder includes built-in validation:

### Accessibility Checks
- **Color Contrast**: Ensures WCAG AA compliance (4.5:1 ratio)
- **Font Sizes**: Warns if text is too small or too large
- **Spacing Scale**: Validates proper progression

### Validation Results

```kotlin
val validator = ThemeValidator()
val result = validator.validate(theme)

if (!result.isValid) {
    result.errors.forEach { error ->
        println("ERROR: $error")
    }
}

result.warnings.forEach { warning ->
    println("WARNING: $warning")
}
```

## Advanced Features

### Color Palette Generation

Generate harmonious color palettes from a seed color:

```kotlin
val colorPicker = PropertyInspector.ColorPicker()

// Complementary palette
val complementary = colorPicker.generatePalette(
    seedColor = Color.hex("#007AFF"),
    mode = PaletteMode.COMPLEMENTARY
)

// Analogous palette
val analogous = colorPicker.generatePalette(
    seedColor = Color.hex("#007AFF"),
    mode = PaletteMode.ANALOGOUS
)

// Triadic palette
val triadic = colorPicker.generatePalette(
    seedColor = Color.hex("#007AFF"),
    mode = PaletteMode.TRIADIC
)

// Monochromatic palette
val monochromatic = colorPicker.generatePalette(
    seedColor = Color.hex("#007AFF"),
    mode = PaletteMode.MONOCHROMATIC
)
```

### Hot Reload

Enable live preview updates:

```kotlin
val hotReload = HotReloadManager(stateManager)
hotReload.enable()
hotReload.onThemeUpdate { theme ->
    // Refresh preview with new theme
}
```

### Auto-Save

Configure automatic saving:

```kotlin
val autoSave = AutoSaveManager(stateManager) { theme ->
    // Save theme to disk
    File("themes/autosave.json").writeText(
        Json.encodeToString(theme)
    )
}

// Change interval to 60 seconds
autoSave.setInterval(60_000L)
```

## Troubleshooting

### Build Issues

**Problem**: Compose Desktop plugin not found
```bash
# Solution: Add Compose plugin to build.gradle.kts
plugins {
    id("org.jetbrains.compose") version "1.5.10"
}
```

**Problem**: AvaElements Core not found
```bash
# Solution: Ensure Core module is built first
./gradlew :Core:build
./gradlew :ThemeBuilder:build
```

### Runtime Issues

**Problem**: Window doesn't open
```
# Check Java version
java -version  # Should be 17+
```

**Problem**: Colors not displaying correctly
```
# Ensure theme has valid color values (0.0-1.0 for RGB)
```

## Examples

### Creating a Custom Brand Theme

```kotlin
// 1. Start with a preset
editorWindow.loadPredefinedTheme(ThemePlatform.Material3_Expressive)

// 2. Update brand colors
inspector.updateProperty("colorScheme.primary", Color.hex("#FF6B35"))
inspector.updateProperty("colorScheme.secondary", Color.hex("#004E89"))

// 3. Adjust typography
inspector.updateProperty("typography.bodyLarge.family", "Inter")
inspector.updateProperty("typography.bodyLarge.size", 16f)

// 4. Set spacing
inspector.updateProperty("spacing.md", 20f)

// 5. Export
val dslCode = editorWindow.exportTheme(ExportFormat.DSL)
File("MyBrandTheme.kt").writeText(dslCode)
```

### Preview Different Screen Sizes

```kotlin
// Mobile preview
stateManager.setScreenSize(ScreenSize.MOBILE)

// Tablet preview
stateManager.setScreenSize(ScreenSize.TABLET)

// Desktop preview
stateManager.setScreenSize(ScreenSize.DESKTOP)

// Custom size
stateManager.setScreenSize(
    ScreenSize("Custom", width = 800, height = 600)
)
```

## Contributing

To add new features to the Theme Builder:

1. **New Property Types**: Extend `PropertyInspector.PropertyType`
2. **New Export Formats**: Add to `ThemeCompiler` methods
3. **New Components**: Add to `PreviewCanvas.availableComponents`
4. **New Validation Rules**: Add to `ThemeValidator.validate()`

## Future Enhancements

Planned features for future releases:

- [ ] Visual color picker with HSL/RGB controls
- [ ] Typography preview with custom text
- [ ] Component state animation preview
- [ ] Theme comparison view (side-by-side)
- [ ] Import from Figma/Sketch
- [ ] Collaborative editing
- [ ] Theme marketplace
- [ ] AI-powered color palette suggestions
- [ ] Accessibility score visualization
- [ ] Theme versioning and history

## License

Part of the Avanues AvaElements library.
Copyright (c) 2025 Augmentalis.

## Support

For issues or questions:
- GitHub Issues: [Create an issue](https://github.com/augmentalis/avaelements/issues)
- Documentation: [AvaElements Docs](../docs/)
- Email: support@augmentalis.com

---

**Version**: 1.0.0
**Last Updated**: 2025-10-29
**Status**: Phase 2 Implementation
