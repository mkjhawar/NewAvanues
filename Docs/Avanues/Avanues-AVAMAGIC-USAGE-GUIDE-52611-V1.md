# AvaMagic Usage Guide

**Version:** 3.0 | **Last Updated:** 2025-11-26

A cross-platform UI component library for Android, iOS, Desktop, and Web.

---

## Table of Contents

1. [Installation](#installation)
2. [Quick Start](#quick-start)
3. [Components](#components)
4. [Platform Renderers](#platform-renderers)
5. [Shared Utilities](#shared-utilities)
6. [VoiceCursor Integration](#voicecursor-integration)
7. [Theming](#theming)
8. [Best Practices](#best-practices)

---

## Installation

### Gradle (Android/Desktop/Common)

```kotlin
// settings.gradle.kts
include(":AvaElements:Core")
include(":AvaElements:Renderers:Android")
include(":AvaElements:Renderers:Desktop")

// build.gradle.kts (app module)
dependencies {
    // Core library (required)
    implementation(project(":AvaElements:Core"))

    // Platform renderer (pick one)
    implementation(project(":AvaElements:Renderers:Android"))  // Android
    implementation(project(":AvaElements:Renderers:Desktop"))  // Desktop
}
```

### Swift Package Manager (iOS)

```swift
// Package.swift
dependencies: [
    .package(path: "../AvaElements/Renderers/iOS")
]
```

### npm (Web)

```bash
npm install @avamagic/web-renderer
```

---

## Quick Start

### Android (Jetpack Compose)

```kotlin
import com.augmentalis.AvaMagic.elements.buttons.Button
import com.augmentalis.AvaMagic.elements.tags.Chip
import com.augmentalis.AvaMagic.layout.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AvaUI {
                theme = Themes.Material3Light

                Column {
                    padding(16f)

                    Text("Welcome to AvaMagic") {
                        font = Font.Title
                    }

                    Row {
                        Chip(label = "Kotlin")
                        Chip(label = "Multiplatform")
                    }

                    Button.primary("Get Started") {
                        // Handle click
                    }
                }
            }
        }
    }
}
```

### iOS (SwiftUI)

```swift
import AvaElementsIOS

struct ContentView: View {
    var body: some View {
        AvaUI {
            Column {
                Text("Welcome to AvaMagic")
                    .font(.title)

                HStack {
                    Chip(label: "Swift")
                    Chip(label: "Multiplatform")
                }

                Button.primary("Get Started") {
                    // Handle tap
                }
            }
            .padding(16)
        }
    }
}
```

### Web (TypeScript/React)

```tsx
import { AvaUI, Column, Row, Text, Button, Chip } from '@avamagic/web-renderer';

export function App() {
    return (
        <AvaUI theme="material3-light">
            <Column padding={16}>
                <Text variant="title">Welcome to AvaMagic</Text>

                <Row>
                    <Chip label="TypeScript" />
                    <Chip label="Multiplatform" />
                </Row>

                <Button variant="primary" onClick={() => {}}>
                    Get Started
                </Button>
            </Column>
        </AvaUI>
    );
}
```

---

## Components

### 39 Components Across 8 Categories

| Category | Components |
|----------|------------|
| **Buttons** | Button (Primary, Secondary, Outlined, Text, Danger) |
| **Tags** | Chip (with icon, deletable, selectable) |
| **Cards** | Card (with elevation/shadow) |
| **Inputs** | TextField, Checkbox, Switch, Radio, Slider, DatePicker, TimePicker, Dropdown, FileUpload, Rating, SearchBar |
| **Display** | Avatar, Divider, EmptyState, Skeleton, Badge, Icon, Image |
| **Navigation** | AppBar, BottomNav, Breadcrumb, Drawer, Pagination, Tabs |
| **Feedback** | Alert, Dialog, ProgressBar, Spinner, Toast, Tooltip |
| **Data** | Accordion, Carousel, DataGrid, List, Table, Timeline, TreeView, Stepper |
| **Layout** | Container, Row, Column, ScrollView |

### Component Usage Examples

#### Button

```kotlin
// Simple button
Button.primary("Submit") { handleSubmit() }
Button.outlined("Cancel") { handleCancel() }
Button.text("Learn More") { navigate() }

// Full configuration
Button(
    text = "Sign In",
    variant = Button.Variant.Primary,
    enabled = isFormValid,
    loading = isLoading,
    icon = "login",
    onClick = { signIn() }
)
```

#### TextField

```kotlin
TextField(
    value = email,
    placeholder = "Enter email",
    label = "Email",
    leadingIcon = "email",
    isError = !isValidEmail,
    errorText = "Invalid email format",
    onValueChange = { email = it }
)
```

#### Chip

```kotlin
Chip(
    label = "Technology",
    icon = "label",
    selected = isSelected,
    deletable = true,
    onClick = { toggleSelection() },
    onDelete = { removeChip() }
)
```

#### Card

```kotlin
Card(elevation = 4f) {
    Column {
        padding(16f)

        Text("Card Title") { font = Font.Headline }
        Text("Card description goes here")

        Row {
            Button.text("Action 1") { }
            Button.text("Action 2") { }
        }
    }
}
```

#### Data Table

```kotlin
Table(
    columns = listOf(
        Column("name", "Name"),
        Column("email", "Email"),
        Column("status", "Status")
    ),
    data = users,
    sortable = true,
    pagination = Pagination(pageSize = 10),
    onRowClick = { user -> showDetails(user) }
)
```

---

## Platform Renderers

### How It Works

```
┌─────────────────────────────────────────────────────────┐
│                    AvaMagic Core                        │
│  (Platform-agnostic component definitions)              │
└─────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ Android  │    │   iOS    │    │ Desktop  │
    │ Renderer │    │ Renderer │    │ Renderer │
    │ (Compose)│    │(SwiftUI) │    │ (Compose)│
    └──────────┘    └──────────┘    └──────────┘
           │               │               │
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ Compose  │    │ SwiftUI  │    │ Compose  │
    │   UI     │    │   UI     │    │ Desktop  │
    └──────────┘    └──────────┘    └──────────┘
```

### Android Renderer

```kotlin
import com.augmentalis.avaelements.renderer.android.ComposeRenderer

// In your Activity/Fragment
setContent {
    val renderer = ComposeRenderer()
    renderer.applyTheme(Themes.Material3Light)

    RenderComponent(myComponent, renderer)
}
```

### iOS Renderer

```swift
import AvaElementsIOS

struct MyView: View {
    let renderer = SwiftUIRenderer()

    var body: some View {
        renderer.render(component: myComponent)
    }
}
```

### Desktop Renderer

```kotlin
import com.augmentalis.avaelements.renderer.desktop.DesktopRenderer

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        val renderer = DesktopRenderer()
        RenderComponent(myComponent, renderer)
    }
}
```

---

## Shared Utilities

Cross-platform utilities that eliminate code duplication across renderers.

### Color Utilities

```kotlin
import com.augmentalis.avaelements.common.color.*

// Create colors
val primary = UniversalColor.fromHex("#1976D2")
val secondary = UniversalColor.fromArgb(0xFF6750A4.toInt())
val hslColor = UniversalColor.fromHsl(210f, 0.8f, 0.5f)

// Manipulate colors
val lighter = primary.lighten(0.2f)      // 20% lighter
val darker = primary.darken(0.3f)        // 30% darker
val faded = primary.withAlpha(0.5f)      // 50% transparent
val mixed = primary.mix(secondary, 0.5f) // 50/50 blend

// Color theory
val complement = primary.complementary() // Opposite on color wheel
val triadic = primary.triadic()          // 3 colors 120° apart
val analogous = primary.analogous()      // Similar hues

// Accessibility (WCAG)
val textColor = primary.contrastingForeground()
val meetsAA = ColorAccessibility.meetsWcagAA(textColor, primary)  // 4.5:1 ratio
val meetsAAA = ColorAccessibility.meetsWcagAAA(textColor, primary) // 7:1 ratio
```

### Property Extraction

```kotlin
import com.augmentalis.avaelements.common.properties.*

// Type-safe property extraction from component props
val props: Map<String, Any?> = componentProperties

val label = props.getString("label", "Default")
val enabled = props.getBoolean("enabled", true)
val count = props.getInt("count", 0)
val opacity = props.getFloat("opacity", 1f)

// Enum extraction (case-insensitive)
val alignment = props.getEnum("alignment", WrapAlignment.Start)

// Color extraction (supports hex, named colors, ARGB int)
val bgColor = props.getColorArgb("backgroundColor", 0xFFFFFFFF.toInt())

// Dimension extraction (supports dp, sp, px, %)
val padding = props.getDimension("padding", 16f)  // Returns DimensionValue

// List extraction
val tags = props.getStringList("tags", emptyList())
val values = props.getIntList("values", emptyList())

// Callbacks
val onClick = props.getCallback("onClick")
val onValueChange = props.getCallback1<String>("onValueChange")
```

### Spacing Utilities

```kotlin
import com.augmentalis.avaelements.common.spacing.*

// Standard spacing scale (Material Design 4dp base)
val small = SpacingScale.SM   // 8dp
val medium = SpacingScale.MD  // 12dp
val large = SpacingScale.LG   // 16dp
val xl = SpacingScale.XL      // 24dp

// Edge insets (padding/margin)
val padding = EdgeInsets.all(16f)
val symmetric = EdgeInsets.symmetric(horizontal = 16f, vertical = 8f)
val custom = EdgeInsets(start = 8f, top = 16f, end = 8f, bottom = 16f)

// Corner radius
val rounded = CornerRadius.all(8f)
val pill = CornerRadius.all(999f)
val topOnly = CornerRadius(topStart = 16f, topEnd = 16f, bottomStart = 0f, bottomEnd = 0f)

// Shadows
val cardShadow = Shadow.elevation(4f)  // Material elevation style
val customShadow = Shadow(
    color = 0x40000000,
    blurRadius = 8f,
    offsetY = 4f
)
```

### Alignment Conversion

```kotlin
import com.augmentalis.avaelements.common.alignment.*

// Convert universal alignments to platform-specific types
val layoutDirection = LayoutDirection.Ltr

val horizontal = AlignmentConverter.wrapToHorizontal(
    WrapAlignment.SpaceBetween,
    layoutDirection
)

val vertical = AlignmentConverter.mainAxisToVertical(
    MainAxisAlignment.Center
)

// Extension functions
val arrangement = WrapAlignment.Start.toHorizontalArrangement(layoutDirection)

// Custom alignment strategies (OCP extensibility)
AlignmentConverter.registerHorizontalStrategy { alignment, direction ->
    // Custom conversion logic
    null // Return null to fall back to default
}
```

---

## VoiceCursor Integration

Voice-controlled cursor navigation for Android VoiceOS devices.

### Setup

```kotlin
import com.augmentalis.avaelements.input.*

// In Application.onCreate() or Activity.onCreate()
initializeVoiceCursor(applicationContext)
```

### Register Voice Targets

```kotlin
// Check if VoiceCursor is available
if (isVoiceCursorAvailable) {
    val manager = getVoiceCursorManager()

    // Register a clickable target
    manager.registerTarget(VoiceTarget(
        id = "submit-button",
        label = "submit",  // User says "Click submit"
        bounds = Rect(100f, 200f, 300f, 250f),
        onSelect = { handleSubmit() },
        onHover = { isHovered -> updateHighlight(isHovered) }
    ))
}
```

### Voice-Accessible Composable

```kotlin
@Composable
fun VoiceAccessibleButton(
    label: String,
    voiceLabel: String = label.lowercase(),
    onClick: () -> Unit
) {
    val manager = getVoiceCursorManager()
    val id = remember { UUID.randomUUID().toString() }

    DisposableEffect(Unit) {
        if (manager.isAvailable) {
            manager.registerTarget(VoiceTarget(
                id = id,
                label = voiceLabel,
                bounds = Rect.Zero, // Updated in onGloballyPositioned
                onSelect = onClick
            ))
        }
        onDispose { manager.unregisterTarget(id) }
    }

    Button(
        onClick = onClick,
        modifier = Modifier.onGloballyPositioned { coords ->
            val pos = coords.positionInWindow()
            val size = coords.size
            manager.updateTargetBounds(id, Rect(
                pos.x, pos.y,
                pos.x + size.width, pos.y + size.height
            ))
        }
    ) {
        Text(label)
    }
}
```

### Voice Commands

```kotlin
// Standard voice commands
VoiceCommands.CLICK         // "click"
VoiceCommands.DOUBLE_CLICK  // "double click"
VoiceCommands.LONG_PRESS    // "long press"
VoiceCommands.SCROLL_UP     // "scroll up"
VoiceCommands.SCROLL_DOWN   // "scroll down"
VoiceCommands.NEXT          // "next"
VoiceCommands.PREVIOUS      // "previous"
VoiceCommands.SELECT        // "select"

// Custom voice commands
val config = VoiceCursorConfig(
    label = "menu button",
    customCommands = listOf(
        CustomVoiceCommand("expand") { expandMenu() },
        CustomVoiceCommand("collapse", synonyms = listOf("close", "hide")) { collapseMenu() }
    )
)
```

---

## Theming

### Built-in Themes

```kotlin
// Material Design 3
Themes.Material3Light
Themes.Material3Dark

// iOS
Themes.iOS26LiquidGlass
Themes.iOSDark

// Custom
val customTheme = Theme(
    name = "Brand",
    colorScheme = ColorScheme(
        primary = UniversalColor.fromHex("#FF5722"),
        onPrimary = UniversalColor.White,
        secondary = UniversalColor.fromHex("#03DAC6"),
        background = UniversalColor.fromHex("#FAFAFA"),
        surface = UniversalColor.White,
        error = UniversalColor.fromHex("#B00020")
    ),
    typography = Typography.Material3,
    shapes = Shapes.Material3,
    spacing = SpacingScale
)
```

### Apply Theme

```kotlin
// Android
setContent {
    AvaUI {
        theme = customTheme
        // Components use theme automatically
    }
}

// Direct renderer theming
val renderer = ComposeRenderer()
renderer.applyTheme(customTheme)
```

### Dynamic Theming

```kotlin
@Composable
fun ThemedApp() {
    var isDark by remember { mutableStateOf(false) }

    val theme = if (isDark) Themes.Material3Dark else Themes.Material3Light

    AvaUI {
        this.theme = theme

        Column {
            Switch(checked = isDark) {
                onCheckedChange = { isDark = it }
            }

            // Rest of UI adapts to theme
        }
    }
}
```

---

## Best Practices

### 1. Use Shared Utilities

```kotlin
// DON'T: Duplicate color logic per platform
fun lightenColor(color: Int, factor: Float): Int {
    // Complex HSL conversion duplicated in each renderer...
}

// DO: Use shared utilities
val lighter = UniversalColor.fromArgb(color).lighten(factor).toArgb()
```

### 2. Check WCAG Compliance

```kotlin
// Always verify text contrast
val background = UniversalColor.fromHex("#1976D2")
val textColor = if (ColorAccessibility.meetsWcagAA(UniversalColor.White, background)) {
    UniversalColor.White
} else {
    UniversalColor.Black
}
```

### 3. Use Standard Spacing

```kotlin
// DON'T: Magic numbers
padding(12f)
padding(24f)

// DO: Use spacing scale
padding(SpacingScale.MD)  // 12dp
padding(SpacingScale.XL)  // 24dp
```

### 4. Handle Platform Differences

```kotlin
// Use expect/actual for platform-specific code
expect fun getPlatformName(): String

// Android
actual fun getPlatformName() = "Android"

// iOS
actual fun getPlatformName() = "iOS"
```

### 5. Register VoiceCursor on Layout

```kotlin
// Update bounds after layout, not during composition
Modifier.onGloballyPositioned { coordinates ->
    voiceCursorManager.updateTargetBounds(id, coordinates.toRect())
}
```

---

## File Structure

```
Universal/Libraries/AvaElements/
├── Core/src/commonMain/kotlin/com/augmentalis/
│   ├── AvaMagic/                    # Component definitions
│   │   ├── elements/                # 36 interactive components
│   │   └── layout/                  # Layout components
│   └── avaelements/
│       ├── common/                  # Shared utilities
│       │   ├── alignment/           # Alignment conversion
│       │   ├── color/               # Color utilities
│       │   ├── properties/          # Property extraction
│       │   └── spacing/             # Spacing utilities
│       └── input/                   # Input system + VoiceCursor
├── Renderers/
│   ├── Android/                     # Jetpack Compose
│   ├── iOS/                         # SwiftUI
│   ├── Desktop/                     # Compose Desktop
│   └── Web/                         # React/TypeScript
└── examples/                        # Example apps
```

---

## Support

- **Documentation:** `/docs/` folder
- **Examples:** `/Universal/Libraries/AvaElements/examples/`
- **Quick Reference:** `AVAMAGIC-QUICK-REFERENCE.md`
- **Shared Utilities:** `SHARED-UTILITIES-QUICK-REFERENCE.md`

---

**AvaMagic** - Write once, render everywhere.
