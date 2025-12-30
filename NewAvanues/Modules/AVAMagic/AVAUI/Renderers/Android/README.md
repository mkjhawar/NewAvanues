# AvaElements Android Compose Renderer

The Android Compose Renderer converts AvaElements components to native Jetpack Compose UI, enabling cross-platform UI definitions to run on Android with full Material Design 3 support.

## Features

- **Complete Component Support**: All 13 AvaElements components mapped to Compose
- **Material Design 3**: Full Material 3 theme integration
- **Type-Safe**: Kotlin-first implementation with type safety
- **Modifier System**: Complete modifier conversion from AvaElements to Compose
- **State Management**: Proper state handling with `remember` and `mutableStateOf`
- **Hot Reload**: Full support for Compose hot reload during development

## Installation

Add the renderer to your Android project:

```kotlin
dependencies {
    implementation(project(":AvaElements:Renderers:Android"))
}
```

## Quick Start

### Basic Usage

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create UI using AvaElements DSL
        val ui = AvaUI {
            theme = Themes.Material3Light

            Column {
                padding(16f)

                Text("Hello World") {
                    font = Font.Title
                    color = Color.hex("#6750A4")
                }

                Button("Click Me") {
                    buttonStyle = ButtonStyle.Primary
                    onClick = {
                        println("Button clicked!")
                    }
                }
            }
        }

        setContent {
            AvaUI(ui)
        }
    }
}
```

## Component Mapping

### Layout Components

| AvaElements | Compose | Description |
|--------------|---------|-------------|
| `ColumnComponent` | `Column { }` | Vertical linear layout |
| `RowComponent` | `Row { }` | Horizontal linear layout |
| `ContainerComponent` | `Box { }` | Single-child container |
| `ScrollViewComponent` | `Column/Row` + `verticalScroll/horizontalScroll` | Scrollable content |
| `CardComponent` | `Card { }` | Material 3 Card with elevation |

### Basic Components

| AvaElements | Compose | Description |
|--------------|---------|-------------|
| `TextComponent` | `Text()` | Text display |
| `ButtonComponent` | `Button()`, `TextButton()`, `OutlinedButton()` | Interactive buttons |
| `TextFieldComponent` | `OutlinedTextField()` | Text input field |
| `CheckboxComponent` | `Checkbox()` + `Text()` | Checkbox with label |
| `SwitchComponent` | `Switch()` | Toggle switch |
| `IconComponent` | `Icon()` | Material Icons |
| `ImageComponent` | `AsyncImage()` | Image from URL or local |

## Component Examples

### Text Component

```kotlin
Text("Welcome") {
    font = Font(size = 24f, weight = Font.Weight.Bold)
    color = Color.hex("#1D1B20")
    textAlign = TextAlign.Center
    maxLines = 2
    overflow = TextOverflow.Ellipsis
}
```

**Maps to:**
```kotlin
Text(
    text = "Welcome",
    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    color = Color(0xFF1D1B20),
    textAlign = TextAlign.Center,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)
```

### Button Component

```kotlin
Button("Sign In") {
    buttonStyle = ButtonStyle.Primary
    enabled = true
    fillMaxWidth()
    onClick = {
        viewModel.signIn()
    }
}
```

**Maps to:**
```kotlin
Button(
    onClick = { viewModel.signIn() },
    modifier = Modifier.fillMaxWidth(),
    enabled = true
) {
    Text("Sign In")
}
```

### Column Layout

```kotlin
Column {
    arrangement = Arrangement.SpaceBetween
    horizontalAlignment = Alignment.Center
    padding(16f)
    fillMaxSize()

    Text("Header")
    Text("Content")
    Text("Footer")
}
```

**Maps to:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
    verticalArrangement = Arrangement.SpaceBetween,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text("Header")
    Text("Content")
    Text("Footer")
}
```

### TextField Component

```kotlin
TextField(
    value = "",
    placeholder = "Enter email"
) {
    label = "Email"
    leadingIcon = "email"
    isError = false
    fillMaxWidth()
    onValueChange = { newValue ->
        viewModel.updateEmail(newValue)
    }
}
```

**Maps to:**
```kotlin
var value by remember { mutableStateOf("") }

OutlinedTextField(
    value = value,
    onValueChange = {
        value = it
        viewModel.updateEmail(it)
    },
    modifier = Modifier.fillMaxWidth(),
    label = { Text("Email") },
    placeholder = { Text("Enter email") },
    isError = false
)
```

## Theme Conversion

### Material Design 3 Theme

```kotlin
val ui = AvaUI {
    theme = Themes.Material3Light  // Auto-converts to MaterialTheme

    Column {
        Text("Themed content") {
            // Uses theme colors automatically
        }
    }
}
```

**Results in:**
```kotlin
MaterialTheme(
    colorScheme = lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color(0xFFFFFFFF),
        // ... all theme colors
    ),
    typography = Typography(
        displayLarge = TextStyle(fontSize = 57.sp, ...),
        // ... all text styles
    ),
    shapes = Shapes(
        small = RoundedCornerShape(8.dp),
        // ... all shapes
    )
) {
    // Content
}
```

### Custom Theme

```kotlin
val customTheme = Theme(
    name = "Custom",
    platform = ThemePlatform.Material3_Expressive,
    colorScheme = ColorScheme.Material3Light.copy(
        primary = Color.hex("#FF5722")
    ),
    typography = Typography.Material3,
    shapes = Shapes.Material3,
    spacing = SpacingScale(),
    elevation = ElevationScale()
)

val ui = AvaUI {
    theme = customTheme
    // ...
}
```

## Modifier Conversion

AvaElements modifiers are automatically converted to Compose modifiers:

| AvaElements Modifier | Compose Modifier |
|----------------------|------------------|
| `padding(16f)` | `Modifier.padding(16.dp)` |
| `fillMaxWidth()` | `Modifier.fillMaxWidth()` |
| `fillMaxHeight()` | `Modifier.fillMaxHeight()` |
| `fillMaxSize()` | `Modifier.fillMaxSize()` |
| `background(color)` | `Modifier.background(color)` |
| `border(...)` | `Modifier.border(...)` |
| `cornerRadius(8f)` | `Modifier.clip(RoundedCornerShape(8.dp))` |
| `shadow(...)` | `Modifier.shadow(...)` |
| `opacity(0.5f)` | `Modifier.alpha(0.5f)` |
| `clickable { }` | `Modifier.clickable { }` |
| `rotate(45f)` | `Modifier.rotate(45f)` |
| `scale(1.5f)` | `Modifier.scale(1.5f)` |
| `zIndex(1)` | `Modifier.zIndex(1f)` |

## State Management

The renderer properly handles state for interactive components:

```kotlin
// AvaElements handles state internally
TextField(value = "", placeholder = "Email") {
    onValueChange = { newValue ->
        // Handle value change
    }
}

Checkbox(label = "Remember me", checked = false) {
    onCheckedChange = { isChecked ->
        // Handle checkbox change
    }
}

Switch(checked = true) {
    onCheckedChange = { isOn ->
        // Handle switch toggle
    }
}
```

## Advanced Usage

### Multiple Renderers

```kotlin
val renderer1 = ComposeRenderer()
renderer1.applyTheme(Themes.Material3Light)

val renderer2 = ComposeRenderer()
renderer2.applyTheme(Themes.iOS26LiquidGlass)

setContent {
    Column {
        RenderComponent(component1, renderer = renderer1)
        RenderComponent(component2, renderer = renderer2)
    }
}
```

### Accessing Renderer in Composition

```kotlin
@Composable
fun MyComponent() {
    val renderer = LocalRenderer.current
    // Use renderer
}
```

### Custom Component Rendering

```kotlin
setContent {
    RenderComponent(
        component = myCustomComponent,
        theme = Themes.Material3Light
    )
}
```

## Example Apps

### Login Screen

See `examples/AndroidExample.kt` for complete login screen example with:
- Text fields for email/password
- Buttons (primary and outlined)
- Checkboxes
- Icons
- Full theming

### Dashboard

See `createDashboard()` in examples for:
- Scrollable content
- Cards with elevation
- Statistics display
- Activity feed

### Settings Screen

See `createSettingsScreen()` in examples for:
- Switches for toggles
- Checkboxes for options
- Grouped settings
- Section headers

## Hot Reload Support

The renderer fully supports Compose hot reload:

1. Make changes to your AvaElements DSL
2. Save the file
3. Changes appear instantly in the running app

No rebuild required for UI changes!

## Performance

- **Lazy Rendering**: Components only render when needed
- **Recomposition**: Leverages Compose's smart recomposition
- **State Optimization**: Minimal state recreation
- **Modifier Chains**: Efficient modifier conversion

## Limitations

### Current Limitations

1. **Hover Effects**: Not supported on mobile (desktop only)
2. **Complex Shadows**: Compose shadows don't support X/Y offset like CSS
3. **Translate Transform**: Requires `graphicsLayer` API
4. **Custom Fonts**: Font family mapping needs setup

### Planned Features

- [ ] Advanced gradient support (sweep gradients)
- [ ] Animated modifier transitions
- [ ] Gesture handling
- [ ] Drag and drop
- [ ] Custom shape definitions

## Troubleshooting

### Theme Not Applying

Ensure you're using `AvaUI()` or `RenderComponent()` with theme parameter:

```kotlin
// Correct
AvaUI(ui)  // Applies ui.theme automatically

// Also correct
RenderComponent(component, theme = Themes.Material3Light)
```

### State Not Updating

Make sure you're using the `onValueChange` callback:

```kotlin
TextField(value = "", placeholder = "Email") {
    onValueChange = { newValue ->
        // This is required for state updates
        viewModel.updateEmail(newValue)
    }
}
```

### Icons Not Found

Ensure you're using supported Material Icon names (lowercase):

```kotlin
Icon("home")      // ✓ Correct
Icon("settings")  // ✓ Correct
Icon("person")    // ✓ Correct
Icon("HomeIcon")  // ✗ Won't work
```

## API Reference

### Core Classes

- `ComposeRenderer`: Main renderer class
- `ThemeConverter`: Converts AvaElements themes to Material3
- `ModifierConverter`: Converts AvaElements modifiers to Compose
- `ComponentMapper<T>`: Base interface for component mapping

### Composable Functions

- `AvaUI(ui: AvaUI)`: Render complete AvaUI
- `RenderComponent(component: Component, theme: Theme?)`: Render single component
- `RenderMagicElement(component: Component)`: Render without theme

### CompositionLocals

- `LocalRenderer`: Access current renderer in composition
- `LocalComposeRenderer`: Internal renderer provider

## Contributing

To add support for new components:

1. Create a new mapper in `mappers/` directory
2. Implement `ComponentMapper<T>` interface
3. Add mapper to `ComposeRenderer`
4. Update this README

## License

Part of the AvaElements framework.

## Support

For issues and questions:
- Check the examples in `examples/`
- See the main AvaElements documentation
- Review component mapper implementations
