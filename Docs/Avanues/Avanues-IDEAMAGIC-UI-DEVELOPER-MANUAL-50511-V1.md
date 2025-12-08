# IDEAMagic UI Components - Developer Manual

**Version**: 2.0.0
**Date**: 2025-11-06
**Module**: IDEAMagic UI:Core, UI:Foundation, AvaCode:Forms, AvaCode:Workflows
**Package**: `com.augmentalis.avamagic`
**Created by**: Manoj Jhawar, manoj@ideahq.net

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Base Type System](#3-base-type-system)
4. [Component Interface](#4-component-interface)
5. [Styling System](#5-styling-system)
6. [Modifier System](#6-modifier-system)
7. [Rendering System](#7-rendering-system)
8. [Core Components Catalog](#8-core-components-catalog)
9. [Foundation Components Catalog](#9-foundation-components-catalog)
10. [Creating Custom Components](#10-creating-custom-components)
11. [Platform Renderers](#11-platform-renderers)
12. [Testing Components](#12-testing-components)
13. [Best Practices](#13-best-practices)
14. [API Reference](#14-api-reference)
15. [AvaCode Forms System](#15-avacode-forms-system)
16. [AvaCode Workflows System](#16-avacode-workflows-system)

---

## 1. Overview

### 1.1 What is IDEAMagic UI?

**IDEAMagic UI** is a cross-platform UI component library for the Avanues ecosystem that provides:

- **Unified Component Model**: Write once, render on Compose/SwiftUI/HTML
- **Type-Safe Styling**: ComponentStyle with composition support
- **Behavior Modifiers**: Decorator pattern for click, drag, focus, accessibility
- **Platform Abstraction**: Renderer interface for cross-platform rendering
- **Flutter/SwiftUI Parity**: 100+ components matching Material/Cupertino/SwiftUI

### 1.2 Module Structure

```
Universal/IDEAMagic/
├── UI/
│   ├── Core/                    # Data classes, base types (44 components)
│   │   ├── base/                # Base type system (NEW!)
│   │   │   ├── Component.kt
│   │   │   ├── ComponentStyle.kt
│   │   │   ├── Modifier.kt
│   │   │   ├── Renderer.kt
│   │   │   ├── enums/          # Size, Color, Position, etc.
│   │   │   ├── types/          # Padding, Margin, Animation, etc.
│   │   │   └── modifiers/      # (Future: separate modifier files)
│   │   ├── form/               # Form components (13)
│   │   ├── display/            # Display components (7)
│   │   ├── feedback/           # Feedback components (9)
│   │   ├── navigation/         # Navigation components (1)
│   │   ├── layout/             # Layout components (1)
│   │   └── data/               # Data components (8)
│   │
│   └── Foundation/              # Magic* components (Compose implementations)
│       └── Magic*.kt            # MagicButton, MagicTextField, etc.
│
├── Components/Renderers/        # Platform-specific renderers (Future)
│   ├── Compose/
│   ├── SwiftUI/
│   └── HTML/
│
└── AvaCode/                   # DSL & codegen (Separate concern)
    └── dsl/
```

### 1.3 Key Concepts

**Component**: Data class implementing Component interface (id, style, modifiers, render)

**ComponentStyle**: Styling properties (padding, colors, borders, effects)

**Modifier**: Behavior decorator (click, drag, focus, test, accessibility, animation)

**Renderer**: Platform-specific rendering implementation (Compose, SwiftUI, HTML)

---

## 2. Architecture

### 2.1 Layered Architecture

```
┌────────────────────────────────────┐
│  Application Layer                 │
│  (VoiceOS, AVAConnect, etc.)       │
└────────────────┬───────────────────┘
                 │
┌────────────────▼───────────────────┐
│  Foundation Layer                  │
│  Magic* Components (Compose)       │
│  MagicButton, MagicTextField, etc. │
└────────────────┬───────────────────┘
                 │
┌────────────────▼───────────────────┐
│  Core Layer (Data Classes)         │
│  ButtonComponent, TextFieldComp... │
└────────────────┬───────────────────┘
                 │
┌────────────────▼───────────────────┐
│  Base Type System                  │
│  Component, Style, Modifier, Enum  │
└────────────────────────────────────┘
```

### 2.2 Component Flow

```
1. Define Component (Core)
   ↓
2. Apply Style & Modifiers
   ↓
3. Render via Platform Renderer
   ↓
4. Display in UI (Compose/SwiftUI/HTML)
```

### 2.3 Design Principles

**1. Immutability**: All components are immutable data classes
**2. Composition**: Styles and modifiers compose via merge/chaining
**3. Platform Agnostic**: Core components have zero platform dependencies
**4. Type Safety**: Enums and sealed interfaces prevent invalid states
**5. Testability**: Pure data classes are easy to test

---

## 3. Base Type System

### 3.1 Component Interface

**Purpose**: Base contract for all UI components

**Location**: `com.augmentalis.avamagic.ui.core.base.Component`

```kotlin
interface Component {
    val id: String? get() = null
    val style: ComponentStyle? get() = null
    val modifiers: List<Modifier> get() = emptyList()
    fun render(renderer: Renderer): Any
}
```

**Properties**:
- `id`: Optional unique identifier (for tracking, testing, state)
- `style`: Optional styling configuration
- `modifiers`: List of behavior decorators (click, drag, etc.)
- `render()`: Delegates rendering to platform-specific renderer

**Example**:
```kotlin
data class ButtonComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val text: String,
    val size: Size = Size.MD,
    val enabled: Boolean = true
) : Component {
    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)
}
```

### 3.2 Type-Safe Enums

**Size**: XS, SM, MD, LG, XL
```kotlin
enum class Size { XS, SM, MD, LG, XL }
```

**Orientation**: HORIZONTAL, VERTICAL
```kotlin
enum class Orientation { HORIZONTAL, VERTICAL }
```

**Color**: 8 semantic colors with RGB values
```kotlin
enum class Color(val rgb: String) {
    PRIMARY("#007AFF"),
    SECONDARY("#5856D6"),
    SUCCESS("#34C759"),
    WARNING("#FF9500"),
    ERROR("#FF3B30"),
    INFO("#5AC8FA"),
    LIGHT("#F2F2F7"),
    DARK("#1C1C1E")
}
```

**Position**: 9 positions (corners + edges + center)
```kotlin
enum class Position {
    TOP, BOTTOM, LEFT, RIGHT, CENTER,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}
```

**Alignment**: 6 flexbox-style alignments
```kotlin
enum class Alignment {
    START, CENTER, END,
    SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY
}
```

**Severity**: 5 levels for alerts/notifications
```kotlin
enum class Severity {
    INFO, SUCCESS, WARNING, ERROR, CRITICAL
}
```

### 3.3 Supporting Types

**Padding**: Internal spacing
```kotlin
data class Padding(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
)

// Convenience constructors
Padding(16f)                     // Uniform
Padding(vertical = 12f, horizontal = 24f)  // Symmetric
Padding(top = 8f, right = 16f, bottom = 8f, left = 16f)  // Individual

// Predefined values
Padding.ZERO, Padding.SMALL (8f), Padding.MEDIUM (16f),
Padding.LARGE (24f), Padding.EXTRA_LARGE (32f)
```

**Margin**: External spacing (same API as Padding)

**Animation**: Animation specifications
```kotlin
data class Animation(
    val type: AnimationType,  // FADE, SLIDE, SCALE, ROTATE
    val duration: Long,
    val easing: Easing = Easing.EASE_IN_OUT  // LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT
)

// Predefined animations
Animation.FADE, Animation.FADE_QUICK, Animation.SLIDE,
Animation.SCALE_IN, Animation.ROTATE
```

**DragEvent**: Drag gesture data
```kotlin
data class DragEvent(
    val x: Float,
    val y: Float,
    val deltaX: Float,
    val deltaY: Float
) {
    val distance: Float  // Euclidean distance
    val angle: Float?    // Direction in degrees (0-360)
    fun isStart(): Boolean
}
```

---

## 4. Component Interface

### 4.1 Implementing Component

**Step 1**: Create data class
```kotlin
data class MyComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    // Component-specific properties
    val text: String,
    val count: Int = 0
) : Component {
    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)
}
```

**Step 2**: Use the component
```kotlin
val component = MyComponent(
    id = "counter",
    text = "Count: 0",
    count = 0,
    style = ComponentStyle(
        padding = Padding(16f),
        backgroundColor = Color.PRIMARY
    ),
    modifiers = listOf(
        Clickable { count++ },
        Testable("counter-component")
    )
)
```

### 4.2 Component Lifecycle

Components are **stateless data classes**. State management is external:

```kotlin
// Component = data (immutable)
val button = ButtonComponent(text = "Click: 0")

// State = mutable (managed externally)
var clickCount by remember { mutableStateOf(0) }

// Update = create new instance
val updatedButton = button.copy(text = "Click: $clickCount")
```

### 4.3 Component Best Practices

**DO**:
- ✅ Keep components immutable (val properties)
- ✅ Use data classes for automatic copy/equals/toString
- ✅ Provide sensible defaults
- ✅ Use type-safe enums instead of strings/ints
- ✅ Document all properties with KDoc

**DON'T**:
- ❌ Store mutable state in components (use external state management)
- ❌ Put business logic in components (they're data, not logic)
- ❌ Create circular dependencies between components
- ❌ Use magic strings/numbers (use enums/constants)

---

## 5. Styling System

### 5.1 ComponentStyle

**Purpose**: Unified styling for all components

**Location**: `com.augmentalis.avamagic.ui.core.base.ComponentStyle`

```kotlin
data class ComponentStyle(
    val padding: Padding? = null,
    val margin: Margin? = null,
    val backgroundColor: Color? = null,
    val borderColor: Color? = null,
    val borderWidth: Float? = null,
    val borderRadius: Float? = null,
    val elevation: Float? = null,
    val opacity: Float? = null
)
```

### 5.2 Style Composition

**Merge Function**: "Non-null wins" strategy
```kotlin
val baseStyle = ComponentStyle(padding = Padding(16f))
val themeStyle = ComponentStyle(backgroundColor = Color.PRIMARY)
val merged = baseStyle.merge(themeStyle)
// Result: padding=16f, backgroundColor=PRIMARY
```

**Plus Operator**: Fluent composition
```kotlin
val finalStyle = baseStyle + themeStyle + componentStyle
```

**Example**:
```kotlin
// Base card style
val cardBase = ComponentStyle(
    padding = Padding(16f),
    borderRadius = 8f,
    elevation = 2f
)

// Error variant
val errorCard = cardBase + ComponentStyle(
    borderColor = Color.ERROR,
    borderWidth = 2f
)

// Success variant
val successCard = cardBase + ComponentStyle(
    backgroundColor = Color.SUCCESS,
    opacity = 0.9f
)
```

### 5.3 Predefined Styles

```kotlin
ComponentStyle.EMPTY          // All nulls
ComponentStyle.CARD           // 16dp padding, 8dp radius, 2dp elevation, light bg
ComponentStyle.BUTTON         // 16dp horizontal, 8dp vertical, primary bg, 4dp radius
ComponentStyle.OUTLINED       // 12dp padding, 1dp border, secondary border, 4dp radius
ComponentStyle.ELEVATED       // 24dp padding, 12dp radius, 8dp elevation, light bg
```

### 5.4 Style Hierarchies

```kotlin
// Application theme
val appTheme = ComponentStyle(backgroundColor = Color.LIGHT)

// Screen-level style
val screenStyle = appTheme + ComponentStyle(padding = Padding(24f))

// Component-level style
val buttonStyle = screenStyle + ComponentStyle(
    backgroundColor = Color.PRIMARY,
    borderRadius = 8f
)
```

---

## 6. Modifier System

### 6.1 Available Modifiers

**Clickable**: Click/tap handling
```kotlin
Clickable(onClick: () -> Unit)

// Usage
val button = ButtonComponent(
    text = "Click Me",
    modifiers = listOf(Clickable { println("Clicked!") })
)
```

**Draggable**: Drag gesture handling
```kotlin
Draggable(onDrag: (DragEvent) -> Unit)

// Usage
var position by remember { mutableStateOf(Offset(0f, 0f)) }
val draggable = Draggable { event ->
    position = position.copy(
        x = position.x + event.deltaX,
        y = position.y + event.deltaY
    )
}
```

**Focusable**: Focus management
```kotlin
Focusable(
    onFocus: () -> Unit,
    onBlur: () -> Unit = {}
)

// Usage
var focused by remember { mutableStateOf(false) }
val focusable = Focusable(
    onFocus = { focused = true },
    onBlur = { focused = false }
)
```

**Testable**: Test automation support
```kotlin
Testable(testId: String)

// Usage
val button = ButtonComponent(
    text = "Submit",
    modifiers = listOf(Testable("submit-button"))
)

// In tests
onNodeWithTag("submit-button").performClick()
```

**Accessible**: Accessibility metadata
```kotlin
Accessible(
    contentDescription: String,
    role: String? = null
)

// Usage
val button = ButtonComponent(
    text = "Submit",
    modifiers = listOf(
        Accessible("Submit form button", role = "button")
    )
)
```

**Animated**: Animation specifications
```kotlin
Animated(animation: Animation)

// Usage
val fadeIn = Animated(Animation.FADE)
val slideUp = Animated(Animation(
    type = AnimationType.SLIDE,
    duration = 500,
    easing = Easing.EASE_OUT
))
```

### 6.2 Modifier Chaining

```kotlin
// Method 1: List of modifiers
val modifiers = listOf(
    Clickable { /* ... */ },
    Testable("my-button"),
    Accessible("Click me")
)

// Method 2: Chaining with then()
val combined = Clickable { /* ... */ }
    .then(Testable("my-button"))
    .then(Accessible("Click me"))

// Method 3: Flatten combined modifier
val flattened = combined.flatten()  // List<Modifier>
```

### 6.3 Processing Modifiers in Renderers

```kotlin
// Extract specific modifier types
val clickable = component.modifiers
    .filterIsInstance<Clickable>()
    .firstOrNull()

// Apply click handler
Button(onClick = { clickable?.onClick?.invoke() }) {
    Text(component.text)
}

// Extract multiple
val testId = component.modifiers
    .filterIsInstance<Testable>()
    .firstOrNull()?.testId

val contentDesc = component.modifiers
    .filterIsInstance<Accessible>()
    .firstOrNull()?.contentDescription
```

---

## 7. Rendering System

### 7.1 Renderer Interface

**Purpose**: Cross-platform rendering abstraction

**Location**: `com.augmentalis.avamagic.ui.core.base.Renderer`

```kotlin
interface Renderer {
    fun renderComponent(component: Component): Any
    fun <T> withContext(key: String, value: T, block: () -> Any): Any
}
```

### 7.2 Platform Implementations (Future)

**ComposeRenderer**: Jetpack Compose (Android/Desktop)
```kotlin
class ComposeRenderer : Renderer {
    @Composable
    override fun renderComponent(component: Component): Any {
        return when (component) {
            is ButtonComponent -> renderButton(component)
            is TextFieldComponent -> renderTextField(component)
            else -> Text("Unknown: ${component::class.simpleName}")
        }
    }

    @Composable
    private fun renderButton(button: ButtonComponent) {
        Button(
            onClick = {
                button.modifiers
                    .filterIsInstance<Clickable>()
                    .firstOrNull()
                    ?.onClick?.invoke()
            },
            modifier = Modifier
                .padding(button.style?.padding?.toPaddingValues() ?: PaddingValues(0.dp))
                .testTag(
                    button.modifiers
                        .filterIsInstance<Testable>()
                        .firstOrNull()
                        ?.testId ?: ""
                )
        ) {
            Text(button.text)
        }
    }

    override fun <T> withContext(key: String, value: T, block: () -> Any): Any {
        return when (key) {
            "theme" -> CompositionLocalProvider(LocalTheme provides value as Theme) {
                block()
            }
            else -> block()
        }
    }
}
```

**SwiftUIRenderer**: SwiftUI (iOS/macOS) - TBD
**HTMLRenderer**: Web/HTML - TBD

### 7.3 Context Propagation

```kotlin
// Set context values
renderer.withContext("theme", darkTheme) {
    renderer.withContext("locale", Locale.FRENCH) {
        component.render(renderer)
    }
}

// Common context keys
// "theme" - Theme configuration
// "locale" - Current locale
// "direction" - LTR/RTL
// "parent" - Parent component
```

---

## 8. Core Components Catalog

### 8.1 Form Components (13)

**TextFieldComponent** (in Foundation as MagicTextField)
```kotlin
data class TextFieldComponent(
    val label: String,
    val value: String,
    val placeholder: String = "",
    val maxLength: Int? = null,
    val multiline: Boolean = false,
    val enabled: Boolean = true
) : Component
```

**Components**:
1. Autocomplete
2. ColorPicker
3. DatePicker
4. DateRangePicker
5. Dropdown
6. IconPicker
7. MultiSelect
8. RangeSlider
9. Rating
10. SearchBar
11. Slider
12. TagInput
13. TimePicker

### 8.2 Display Components (7)

**Components**:
1. Avatar
2. Badge
3. Chip
4. DataTable
5. Timeline
6. Tooltip
7. TreeView

### 8.3 Feedback Components (9)

**AlertComponent**:
```kotlin
data class AlertComponent(
    val message: String,
    val severity: Severity = Severity.INFO,
    val dismissible: Boolean = true
) : Component
```

**Components**:
1. Alert
2. Badge (also in Display)
3. Banner
4. Dialog
5. ProgressBar
6. Snackbar
7. Spinner
8. Toast
9. Tooltip (also in Display)

### 8.4 Data Components (8)

**Components**:
1. Accordion
2. Avatar (also in Display)
3. Carousel
4. Chip (also in Display)
5. EmptyState
6. List
7. Paper
8. TreeView (also in Display)

### 8.5 Navigation Components (1)

**AppBarComponent** (removed, being restored)

### 8.6 Layout Components (1)

**MasonryGridComponent**:
```kotlin
data class MasonryGridComponent(
    val columns: Int = 2,
    val spacing: Float = 8f,
    val items: List<Component> = emptyList()
) : Component
```

---

## 9. Foundation Components Catalog

### 9.1 Magic* Components (Compose)

Foundation components are Compose implementations with Magic* prefix:

**MagicButton**:
```kotlin
@Composable
fun MagicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Size = Size.MD,
    variant: ButtonVariant = ButtonVariant.FILLED
)
```

**MagicTextField**:
```kotlin
@Composable
fun MagicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "",
    placeholder: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxLength: Int? = null,
    multiline: Boolean = false
)
```

**Available Magic* Components**:
- MagicButton, MagicTextField, MagicCheckbox
- MagicCard, MagicChip, MagicBadge
- MagicDialog, MagicSnackbar, MagicToast
- MagicList, MagicAccordion
- And more...

### 9.2 Using Foundation Components

```kotlin
// In Compose code
@Composable
fun MyScreen() {
    Column {
        MagicButton(
            text = "Click Me",
            onClick = { /* handle click */ },
            size = Size.LG
        )

        MagicTextField(
            value = textState,
            onValueChange = { textState = it },
            label = "Enter your name"
        )
    }
}
```

---

## 10. Creating Custom Components

### 10.1 Step-by-Step Guide

**Step 1: Define the Data Class**

```kotlin
package com.example.myapp.components

import com.augmentalis.avamagic.ui.core.base.*
import com.augmentalis.avamagic.ui.core.base.enums.*
import com.augmentalis.avamagic.ui.core.base.types.*

data class CounterComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val count: Int = 0,
    val label: String = "Count",
    val size: Size = Size.MD
) : Component {
    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)
}
```

**Step 2: Add Validation (Optional)**

```kotlin
data class CounterComponent(
    // ... properties ...
) : Component {
    init {
        require(count >= 0) { "Count must be non-negative" }
        require(label.isNotBlank()) { "Label cannot be blank" }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)
}
```

**Step 3: Add Helper Functions**

```kotlin
data class CounterComponent(
    // ... properties ...
) : Component {
    fun increment(): CounterComponent = copy(count = count + 1)
    fun decrement(): CounterComponent = copy(count = maxOf(0, count - 1))
    fun reset(): CounterComponent = copy(count = 0)

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)
}
```

**Step 4: Create Renderer Implementation**

```kotlin
// In ComposeRenderer (when implemented)
@Composable
private fun renderCounter(counter: CounterComponent) {
    val clickable = counter.modifiers
        .filterIsInstance<Clickable>()
        .firstOrNull()

    Card(
        modifier = Modifier
            .padding(counter.style?.padding?.toPaddingValues() ?: PaddingValues(0.dp))
            .clickable { clickable?.onClick?.invoke() }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(counter.label, style = MaterialTheme.typography.titleMedium)
            Text(counter.count.toString(), style = MaterialTheme.typography.displayLarge)
        }
    }
}
```

**Step 5: Write Tests**

```kotlin
class CounterComponentTest {
    @Test
    fun testIncrement() {
        val counter = CounterComponent(count = 5)
        val incremented = counter.increment()
        assertEquals(6, incremented.count)
    }

    @Test
    fun testDecrementAtZero() {
        val counter = CounterComponent(count = 0)
        val decremented = counter.decrement()
        assertEquals(0, decremented.count)
    }

    @Test
    fun testReset() {
        val counter = CounterComponent(count = 10)
        val reset = counter.reset()
        assertEquals(0, reset.count)
    }
}
```

### 10.2 Component Templates

**Simple Display Component**:
```kotlin
data class LabelComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val text: String,
    val color: Color = Color.DARK
) : Component {
    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)
}
```

**Interactive Component**:
```kotlin
data class ToggleComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val checked: Boolean = false,
    val label: String,
    val enabled: Boolean = true
) : Component {
    init {
        require(label.isNotBlank()) { "Label required" }
    }

    fun toggle(): ToggleComponent = copy(checked = !checked)

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)
}
```

**Container Component**:
```kotlin
data class PanelComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val title: String,
    val children: List<Component> = emptyList(),
    val collapsible: Boolean = false,
    val collapsed: Boolean = false
) : Component {
    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)
}
```

---

## 11. Platform Renderers

### 11.1 Renderer Architecture

```
┌──────────────────────────────┐
│  Component (Data)            │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│  Renderer Interface          │
└──────────────┬───────────────┘
               │
       ┌───────┴───────┐
       │               │
       ▼               ▼
┌──────────────┐ ┌──────────────┐
│ Compose      │ │ SwiftUI      │
│ Renderer     │ │ Renderer     │
└──────────────┘ └──────────────┘
       │               │
       ▼               ▼
┌──────────────┐ ┌──────────────┐
│ @Composable  │ │ View         │
│ UI           │ │ (SwiftUI)    │
└──────────────┘ └──────────────┘
```

### 11.2 Implementing a Renderer

**Requirements**:
1. Implement `Renderer` interface
2. Handle all component types (or throw for unsupported)
3. Process modifiers correctly
4. Support context propagation
5. Return platform-specific output

**Example Structure**:
```kotlin
class ComposeRenderer : Renderer {
    // Main dispatcher
    @Composable
    override fun renderComponent(component: Component): Any {
        return when (component) {
            is ButtonComponent -> renderButton(component)
            is TextFieldComponent -> renderTextField(component)
            is CounterComponent -> renderCounter(component)
            // ... all supported components
            else -> renderUnknown(component)
        }
    }

    // Component-specific renderers
    @Composable
    private fun renderButton(button: ButtonComponent) { /* ... */ }

    @Composable
    private fun renderTextField(field: TextFieldComponent) { /* ... */ }

    // Unknown component fallback
    @Composable
    private fun renderUnknown(component: Component) {
        Text("Unknown component: ${component::class.simpleName}")
    }

    // Context support
    override fun <T> withContext(key: String, value: T, block: () -> Any): Any {
        return when (key) {
            "theme" -> CompositionLocalProvider(LocalTheme provides value as Theme) {
                block()
            }
            "locale" -> CompositionLocalProvider(LocalLocale provides value as Locale) {
                block()
            }
            else -> block()
        }
    }
}
```

### 11.3 Renderer Best Practices

**DO**:
- ✅ Handle all core component types
- ✅ Process all modifiers (Clickable, Testable, Accessible, etc.)
- ✅ Apply ComponentStyle correctly (padding, colors, borders)
- ✅ Support nested components (for containers)
- ✅ Provide fallback for unknown components

**DON'T**:
- ❌ Modify component data (components are immutable)
- ❌ Store state in renderer (use external state management)
- ❌ Throw exceptions for unknown components (render fallback)
- ❌ Ignore accessibility modifiers

---

## 12. Testing Components

### 12.1 Unit Testing Data Classes

```kotlin
class MyComponentTest {
    @Test
    fun testDefaultValues() {
        val component = MyComponent(text = "Test")
        assertEquals("Test", component.text)
        assertNull(component.id)
        assertNull(component.style)
        assertTrue(component.modifiers.isEmpty())
    }

    @Test
    fun testWithStyle() {
        val style = ComponentStyle(padding = Padding(16f))
        val component = MyComponent(text = "Test", style = style)
        assertEquals(16f, component.style?.padding?.top)
    }

    @Test
    fun testWithModifiers() {
        var clicked = false
        val component = MyComponent(
            text = "Test",
            modifiers = listOf(Clickable { clicked = true })
        )

        val clickable = component.modifiers.filterIsInstance<Clickable>().first()
        clickable.onClick()
        assertTrue(clicked)
    }

    @Test
    fun testImmutability() {
        val original = MyComponent(text = "Original")
        val modified = original.copy(text = "Modified")
        assertEquals("Original", original.text)
        assertEquals("Modified", modified.text)
    }
}
```

### 12.2 Testing Style Composition

```kotlin
class StyleCompositionTest {
    @Test
    fun testStyleMerge() {
        val base = ComponentStyle(padding = Padding(16f))
        val override = ComponentStyle(backgroundColor = Color.PRIMARY)
        val merged = base.merge(override)

        assertEquals(Padding(16f), merged.padding)
        assertEquals(Color.PRIMARY, merged.backgroundColor)
    }

    @Test
    fun testStyleOverride() {
        val base = ComponentStyle(backgroundColor = Color.LIGHT)
        val override = ComponentStyle(backgroundColor = Color.DARK)
        val merged = base + override

        assertEquals(Color.DARK, merged.backgroundColor)
    }
}
```

### 12.3 Testing Modifier Chaining

```kotlin
class ModifierChainTest {
    @Test
    fun testChaining() {
        val clickable = Clickable { }
        val testable = Testable("my-button")
        val accessible = Accessible("Click me")

        val chained = clickable.then(testable).then(accessible)
        val flattened = chained.flatten()

        assertEquals(3, flattened.size)
        assertTrue(flattened[0] is Clickable)
        assertTrue(flattened[1] is Testable)
        assertTrue(flattened[2] is Accessible)
    }
}
```

### 12.4 Integration Testing with Renderers

```kotlin
@RunWith(AndroidJUnit4::class)
class ComposeRendererTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testButtonRendering() {
        val button = ButtonComponent(
            text = "Click Me",
            modifiers = listOf(Testable("test-button"))
        )

        composeTestRule.setContent {
            val renderer = ComposeRenderer()
            button.render(renderer)
        }

        composeTestRule
            .onNodeWithTag("test-button")
            .assertExists()
            .assertTextEquals("Click Me")
    }

    @Test
    fun testButtonClick() {
        var clicked = false
        val button = ButtonComponent(
            text = "Click Me",
            modifiers = listOf(
                Clickable { clicked = true },
                Testable("test-button")
            )
        )

        composeTestRule.setContent {
            val renderer = ComposeRenderer()
            button.render(renderer)
        }

        composeTestRule.onNodeWithTag("test-button").performClick()
        assertTrue(clicked)
    }
}
```

---

## 13. Best Practices

### 13.1 Component Design

**Single Responsibility**: Each component should have one clear purpose
```kotlin
// ✅ GOOD: Focused component
data class EmailFieldComponent(
    val email: String,
    val valid: Boolean = true
) : Component

// ❌ BAD: Too many responsibilities
data class UserFormComponent(
    val email: String,
    val password: String,
    val confirmPassword: String,
    val agreeToTerms: Boolean,
    val newsletter: Boolean
) : Component  // Should be multiple components
```

**Immutability**: Never use var properties
```kotlin
// ✅ GOOD: Immutable
data class CounterComponent(val count: Int) : Component

// ❌ BAD: Mutable
data class CounterComponent(var count: Int) : Component
```

**Validation**: Validate in init block
```kotlin
data class RatingComponent(
    val rating: Int,
    val maxRating: Int = 5
) : Component {
    init {
        require(rating in 0..maxRating) {
            "Rating must be 0-$maxRating"
        }
    }
}
```

### 13.2 Styling Guidelines

**Theme First**: Define base theme, then override
```kotlin
val appTheme = ComponentStyle(
    padding = Padding.MEDIUM,
    backgroundColor = Color.LIGHT
)

val primaryButton = appTheme + ComponentStyle(
    backgroundColor = Color.PRIMARY
)

val secondaryButton = appTheme + ComponentStyle(
    backgroundColor = Color.SECONDARY
)
```

**Composition over Duplication**:
```kotlin
// ✅ GOOD: Compose styles
val cardBase = ComponentStyle(padding = Padding.MEDIUM, elevation = 2f)
val errorCard = cardBase + ComponentStyle(borderColor = Color.ERROR)
val successCard = cardBase + ComponentStyle(borderColor = Color.SUCCESS)

// ❌ BAD: Duplicate properties
val errorCard = ComponentStyle(padding = Padding.MEDIUM, elevation = 2f, borderColor = Color.ERROR)
val successCard = ComponentStyle(padding = Padding.MEDIUM, elevation = 2f, borderColor = Color.SUCCESS)
```

### 13.3 Modifier Guidelines

**Order Matters**: Apply modifiers in logical order
```kotlin
// ✅ GOOD: Semantic order
listOf(
    Testable("my-button"),       // 1. Testing
    Accessible("Click me"),      // 2. Accessibility
    Clickable { /* ... */ },     // 3. Interaction
    Animated(Animation.FADE)     // 4. Visual effects
)

// ❌ BAD: Random order (harder to read)
listOf(
    Clickable { /* ... */ },
    Testable("my-button"),
    Animated(Animation.FADE),
    Accessible("Click me")
)
```

**Always Testable**: Add Testable modifier to interactive components
```kotlin
// ✅ GOOD: Testable
ButtonComponent(
    text = "Submit",
    modifiers = listOf(
        Testable("submit-button"),
        Clickable { /* ... */ }
    )
)

// ❌ BAD: Not testable
ButtonComponent(
    text = "Submit",
    modifiers = listOf(
        Clickable { /* ... */ }
    )
)  // How to test this?
```

**Always Accessible**: Add Accessible modifier for screen readers
```kotlin
// ✅ GOOD: Accessible
IconButton(
    icon = "delete",
    modifiers = listOf(
        Accessible("Delete item", role = "button")
    )
)

// ❌ BAD: Not accessible
IconButton(
    icon = "delete"
)  // Screen reader users don't know what this does
```

### 13.4 Performance

**Avoid Deep Nesting**: Keep component trees shallow
```kotlin
// ✅ GOOD: Flat structure
PanelComponent(
    children = listOf(header, content, footer)
)

// ❌ BAD: Deep nesting
PanelComponent(
    children = listOf(
        PanelComponent(
            children = listOf(
                PanelComponent(children = listOf(header))
            )
        )
    )
)
```

**Use Predefined Values**: Don't create new instances unnecessarily
```kotlin
// ✅ GOOD: Reuse predefined
val style = ComponentStyle(padding = Padding.MEDIUM)

// ❌ BAD: Create new every time
val style = ComponentStyle(padding = Padding(16f))
```

---

## 14. API Reference

### 14.1 Base Types

**Component Interface**:
- `id: String?` - Optional unique identifier
- `style: ComponentStyle?` - Optional styling
- `modifiers: List<Modifier>` - Behavior decorators
- `render(renderer: Renderer): Any` - Platform rendering

**ComponentStyle Data Class**:
- `padding: Padding?` - Internal spacing
- `margin: Margin?` - External spacing
- `backgroundColor: Color?` - Background fill
- `borderColor: Color?` - Border color
- `borderWidth: Float?` - Border thickness
- `borderRadius: Float?` - Corner radius
- `elevation: Float?` - Shadow/elevation
- `opacity: Float?` - Transparency (0.0-1.0)
- `merge(other: ComponentStyle): ComponentStyle` - Merge styles
- `plus(other: ComponentStyle): ComponentStyle` - Operator overload

**Modifier Sealed Interface**:
- `Clickable(onClick: () -> Unit)` - Click handler
- `Draggable(onDrag: (DragEvent) -> Unit)` - Drag handler
- `Focusable(onFocus: () -> Unit, onBlur: () -> Unit)` - Focus handlers
- `Testable(testId: String)` - Test identifier
- `Accessible(contentDescription: String, role: String?)` - Accessibility
- `Animated(animation: Animation)` - Animation spec
- `then(other: Modifier): Modifier` - Chain modifiers
- `flatten(): List<Modifier>` - Flatten chained modifiers

**Renderer Interface**:
- `renderComponent(component: Component): Any` - Render to platform
- `withContext(key: String, value: T, block: () -> Any): Any` - Context propagation

### 14.2 Enums

**Size**: XS, SM, MD, LG, XL

**Orientation**: HORIZONTAL, VERTICAL

**Color**: PRIMARY, SECONDARY, SUCCESS, WARNING, ERROR, INFO, LIGHT, DARK
- `rgb: String` - Hex color (#RRGGBB)
- `argb: Int` - ARGB integer
- `red/green/blue: Int` - RGB components (0-255)
- `toNormalizedRGB(): Triple<Float, Float, Float>` - 0.0-1.0 floats
- `fromHex(hex: String): Color?` - Parse hex to Color
- `custom(hex: String): Color?` - Alias for fromHex

**Position**: TOP, BOTTOM, LEFT, RIGHT, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT

**Alignment**: START, CENTER, END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY

**Severity**: INFO, SUCCESS, WARNING, ERROR, CRITICAL

### 14.3 Supporting Types

**Padding/Margin**:
- `Padding()` - All zeros
- `Padding(all: Float)` - Uniform
- `Padding(vertical: Float, horizontal: Float)` - Symmetric
- `Padding(top, right, bottom, left)` - Individual
- `horizontal: Float` - left + right
- `vertical: Float` - top + bottom
- `isZero(): Boolean` - All sides are 0
- **Predefined**: ZERO, SMALL (8f), MEDIUM (16f), LARGE (24f), EXTRA_LARGE (32f)

**Animation**:
- `Animation(type, duration, easing)` - Full constructor
- `type: AnimationType` - FADE, SLIDE, SCALE, ROTATE
- `duration: Long` - Milliseconds (validated > 0)
- `easing: Easing` - LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT
- **Predefined**: FADE, FADE_QUICK, FADE_SLOW, SLIDE, SLIDE_QUICK, SCALE_IN, ROTATE

**DragEvent**:
- `DragEvent(x, y, deltaX, deltaY)`
- `distance: Float` - Euclidean distance
- `angle: Float?` - Direction in degrees (0-360°)
- `isStart(): Boolean` - True if no delta

---

## 15. AvaCode Forms System

### 15.1 Overview

**AvaCode Forms** is a declarative Kotlin DSL for creating type-safe forms with automatic database schema generation, comprehensive validation, and two-way data binding. It enables rapid application development with production-ready forms in minutes.

**Key Features**:
- **Declarative DSL**: Kotlin builders for intuitive form definition
- **8 field types**: text, email, password, number, date, boolean, select, textarea
- **16 validation rules**: Required, length, pattern, numeric range, password complexity, custom
- **Auto-database generation**: CREATE TABLE statements for 4 SQL dialects
- **Two-way binding**: Form ↔ Data synchronization with change tracking
- **Completion tracking**: Real-time progress and submission readiness
- **Type-safe**: 100% compile-time type checking

**Module**: `Universal/IDEAMagic/AvaCode/Forms`
**Package**: `com.augmentalis.avamagic.avacode.forms`

### 15.2 Quick Start

```kotlin
// Define a user registration form
val userForm = form("user_registration") {
    textField("username") {
        label("Username")
        required()
        minLength(3)
        maxLength(20)
        pattern("[a-zA-Z0-9_]+")
        unique()
        indexed()
    }

    emailField("email") {
        label("Email Address")
        required()
        unique()
    }

    passwordField("password") {
        label("Password")
        required()
        minLength(8)
        requireUppercase()
        requireNumber()
        requireSpecialChar()
    }

    dateField("birth_date") {
        label("Date of Birth")
        required()
        maxDate(LocalDate.now().minusYears(13)) // Must be 13+
    }

    selectField("country") {
        label("Country")
        options(listOf("US", "UK", "CA", "AU"))
        required()
    }

    booleanField("agree_terms") {
        label("I agree to Terms & Conditions")
        required()
    }
}

// Generate database schema
val schema = userForm.toSchema()
val sql = schema.toSQL(SQLDialect.SQLITE)
// Outputs: CREATE TABLE user_registration (
//   id INTEGER PRIMARY KEY AUTOINCREMENT,
//   username VARCHAR(20) NOT NULL UNIQUE,
//   email VARCHAR(255) NOT NULL UNIQUE,
//   password VARCHAR(255) NOT NULL,
//   birth_date DATE NOT NULL,
//   country VARCHAR(255) NOT NULL,
//   agree_terms BOOLEAN NOT NULL,
//   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
// );
// CREATE INDEX idx_username ON user_registration(username);

// Bind data with validation
val binding = userForm.bind()

// Set values (validates immediately)
binding["username"] = "johndoe"
binding["email"] = "john@example.com"
binding["password"] = "SecurePass123!"
binding["birth_date"] = LocalDate.of(1990, 1, 1)
binding["country"] = "US"
binding["agree_terms"] = true

// Check completion
val completion = binding.getCompletion()
println("Progress: ${completion.overallPercentage}%") // 100%
println("Can submit: ${completion.canSubmit}") // true

// Validate all fields
val result = binding.validate()
when (result) {
    is ValidationResult.Success -> {
        val data = binding.getData()
        saveToDatabase(data)
    }
    is ValidationResult.Failure -> {
        result.errors.forEach { (field, errors) ->
            showError(field, errors)
        }
    }
}
```

### 15.3 Field Types

#### 15.3.1 Text Field
Basic text input with validation.

```kotlin
textField("field_name") {
    label("Display Label")
    placeholder("Enter text...")
    defaultValue("Initial value")
    nullable(false)  // NOT NULL constraint
    unique()         // UNIQUE constraint
    indexed()        // CREATE INDEX
    required()
    minLength(3)
    maxLength(100)
    pattern("[a-zA-Z0-9]+")  // Regex validation
}
```

**SQL Mapping**: `VARCHAR(length)` where length = maxLength or 255

#### 15.3.2 Email Field
Email input with automatic email validation.

```kotlin
emailField("email") {
    label("Email Address")
    required()
    unique()
}
```

**SQL Mapping**: `VARCHAR(255)`
**Automatic Validation**: RFC 5322 email pattern

#### 15.3.3 Password Field
Password input with complexity requirements.

```kotlin
passwordField("password") {
    label("Password")
    required()
    minLength(8)
    maxLength(128)
    requireUppercase()  // At least 1 uppercase
    requireLowercase()  // At least 1 lowercase
    requireNumber()     // At least 1 digit
    requireSpecialChar() // At least 1 special char
}
```

**SQL Mapping**: `VARCHAR(255)`
**Note**: Store hashed passwords, never plain text

#### 15.3.4 Number Field
Numeric input with range validation.

```kotlin
numberField("age") {
    label("Age")
    required()
    min(0.0)
    max(120.0)
    range(18.0, 65.0)  // Shorthand for min+max
}
```

**SQL Mapping**: `INTEGER` or `DOUBLE` based on value type

#### 15.3.5 Date Field
Date input with date range validation.

```kotlin
dateField("birth_date") {
    label("Date of Birth")
    required()
    minDate(LocalDate.of(1900, 1, 1))
    maxDate(LocalDate.now())
}
```

**SQL Mapping**: `DATE`

#### 15.3.6 Boolean Field
Checkbox/toggle input.

```kotlin
booleanField("newsletter") {
    label("Subscribe to newsletter")
    defaultValue(false)
    nullable(false)
}
```

**SQL Mapping**: `BOOLEAN` (or `TINYINT(1)` for MySQL)

#### 15.3.7 Select Field
Dropdown/select input with predefined options.

```kotlin
selectField("status") {
    label("Status")
    options(listOf("active", "inactive", "pending"))
    defaultValue("pending")
    required()
}
```

**SQL Mapping**: `VARCHAR(255)`
**Automatic Validation**: Value must be in options list

#### 15.3.8 TextArea Field
Multi-line text input.

```kotlin
textAreaField("description") {
    label("Description")
    placeholder("Enter detailed description...")
    required()
    minLength(10)
    maxLength(2000)
}
```

**SQL Mapping**: `TEXT`

### 15.4 Validation Rules

#### 15.4.1 Built-in Rules

**Required**: Field must have a value
```kotlin
required() // Error: "This field is required"
```

**Length Constraints**:
```kotlin
minLength(3)    // Error: "Must be at least 3 characters"
maxLength(100)  // Error: "Must be at most 100 characters"
```

**Pattern Matching**:
```kotlin
pattern("[a-zA-Z0-9_]+")  // Error: "Invalid format"
```

**Email Validation**:
```kotlin
// Automatic on emailField
email()  // Error: "Invalid email address"
```

**Numeric Range**:
```kotlin
min(0.0)           // Error: "Must be at least 0"
max(100.0)         // Error: "Must be at most 100"
range(0.0, 100.0)  // Shorthand for min+max
```

**Date Range**:
```kotlin
minDate(LocalDate.of(1900, 1, 1))  // Error: "Date too early"
maxDate(LocalDate.now())            // Error: "Date cannot be in future"
```

**Password Complexity**:
```kotlin
requireUppercase()    // Error: "Must contain uppercase letter"
requireLowercase()    // Error: "Must contain lowercase letter"
requireNumber()       // Error: "Must contain number"
requireSpecialChar()  // Error: "Must contain special character"
```

**List Membership**:
```kotlin
inList(listOf("A", "B", "C"))  // Error: "Invalid option"
```

**Custom Validation**:
```kotlin
custom("Username must not contain 'admin'") { value ->
    !value.toString().contains("admin", ignoreCase = true)
}
```

#### 15.4.2 Composing Rules

Multiple rules can be applied to a single field:

```kotlin
textField("username") {
    required()              // Rule 1
    minLength(3)            // Rule 2
    maxLength(20)           // Rule 3
    pattern("[a-zA-Z0-9_]+") // Rule 4
    custom("Reserved word") { value ->  // Rule 5
        value.toString() !in listOf("admin", "root", "system")
    }
}
```

**Validation Order**: Rules are evaluated in definition order. All errors are collected (not fail-fast).

### 15.5 Database Schema Generation

#### 15.5.1 Automatic DDL Generation

```kotlin
val form = form("users") {
    textField("username") {
        required()
        unique()
        indexed()
        maxLength(50)
    }
    emailField("email") {
        required()
        unique()
    }
    numberField("age") {
        min(0.0)
        max(150.0)
    }
}

val schema = form.toSchema()
```

#### 15.5.2 SQL Dialect Support

**SQLite**:
```kotlin
val sql = schema.toSQL(SQLDialect.SQLITE)
// CREATE TABLE users (
//   id INTEGER PRIMARY KEY AUTOINCREMENT,
//   username VARCHAR(50) NOT NULL UNIQUE,
//   email VARCHAR(255) NOT NULL UNIQUE,
//   age INTEGER,
//   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
// );
// CREATE INDEX idx_username ON users(username);
```

**MySQL**:
```kotlin
val sql = schema.toSQL(SQLDialect.MYSQL)
// CREATE TABLE users (
//   id INT AUTO_INCREMENT PRIMARY KEY,
//   username VARCHAR(50) NOT NULL UNIQUE,
//   email VARCHAR(255) NOT NULL UNIQUE,
//   age INT,
//   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
// ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
// CREATE INDEX idx_username ON users(username);
```

**PostgreSQL**:
```kotlin
val sql = schema.toSQL(SQLDialect.POSTGRESQL)
// CREATE TABLE users (
//   id SERIAL PRIMARY KEY,
//   username VARCHAR(50) NOT NULL UNIQUE,
//   email VARCHAR(255) NOT NULL UNIQUE,
//   age INTEGER,
//   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
// );
// CREATE INDEX idx_username ON users(username);
```

**H2**:
```kotlin
val sql = schema.toSQL(SQLDialect.H2)
// CREATE TABLE users (
//   id IDENTITY PRIMARY KEY,
//   username VARCHAR(50) NOT NULL UNIQUE,
//   email VARCHAR(255) NOT NULL UNIQUE,
//   age INTEGER,
//   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
// );
// CREATE INDEX idx_username ON users(username);
```

#### 15.5.3 Column Constraints

Forms automatically generate appropriate SQL constraints:

- `required()` → `NOT NULL`
- `unique()` → `UNIQUE`
- `indexed()` → `CREATE INDEX`
- `defaultValue(x)` → `DEFAULT x`
- `min()/max()` → `CHECK (column >= min AND column <= max)`

#### 15.5.4 Audit Columns

All tables automatically include:
- `id` - Auto-increment primary key
- `created_at` - Creation timestamp
- `updated_at` - Last update timestamp

### 15.6 Form Data Binding

#### 15.6.1 Creating a Binding

```kotlin
val form = form("contact") { /* ... */ }
val binding = form.bind()
```

#### 15.6.2 Setting Values

**Immediate Validation**:
```kotlin
try {
    binding["email"] = "invalid"  // Throws ValidationException
} catch (e: ValidationException) {
    println("${e.fieldId}: ${e.errors.joinToString()}")
    // Output: email: Invalid email address
}
```

**Batch Updates**:
```kotlin
binding.setData(mapOf(
    "name" to "John Doe",
    "email" to "john@example.com",
    "message" to "Hello, world!"
))
```

#### 15.6.3 Getting Values

```kotlin
val email: String? = binding["email"] as? String
val allData: Map<String, Any?> = binding.getData()
```

#### 15.6.4 Change Tracking

```kotlin
// Register listener
binding.onChange { fieldId, newValue ->
    println("$fieldId changed to: $newValue")
    autoSave(binding.getData())
}

// Check changes
if (binding.hasChanges()) {
    val changes = binding.getChanges()
    changes.forEach { (field, change) ->
        println("$field: ${change.oldValue} → ${change.newValue}")
    }
}

// Reset to original
binding.reset()
```

#### 15.6.5 Validation

**Validate All**:
```kotlin
val result = binding.validate()
when (result) {
    is ValidationResult.Success -> submitForm(binding.getData())
    is ValidationResult.Failure -> {
        result.errors.forEach { (field, errors) ->
            showFieldError(field, errors.joinToString(", "))
        }
    }
}
```

**Validate Single Field**:
```kotlin
val field = binding.form.fields.find { it.id == "email" }!!
val fieldResult = binding.validateField(field)
```

#### 15.6.6 Completion Tracking

```kotlin
val completion = binding.getCompletion()

// Overall progress
println("Overall: ${completion.overallPercentage}%")
println("Required only: ${completion.requiredPercentage}%")

// Field counts
println("Filled: ${completion.filledCount}/${completion.totalFields}")
println("Required filled: ${completion.requiredFilledCount}/${completion.requiredFields}")

// Submission readiness
if (completion.canSubmit) {
    enableSubmitButton()
} else {
    println("Missing required: ${completion.missingRequired.joinToString()}")
}
```

### 15.7 Form Metadata

```kotlin
form("user_profile") {
    // Metadata
    title("User Profile")
    description("Update your profile information")
    version("1.0.0")
    autoSave(true, intervalSeconds = 30)

    // Fields
    textField("name") { /* ... */ }
}
```

**Metadata Properties**:
- `title` - Display title for the form
- `description` - Help text for users
- `version` - Versioning for schema migrations
- `autoSave` - Enable auto-save with interval

### 15.8 Complete Example: E-commerce Checkout

```kotlin
val checkoutForm = form("checkout") {
    title("Checkout")
    description("Complete your purchase")
    version("1.0.0")
    autoSave(true, intervalSeconds = 60)

    // Shipping Address
    textField("shipping_name") {
        label("Full Name")
        required()
        minLength(2)
    }

    textField("shipping_address") {
        label("Street Address")
        required()
        minLength(5)
    }

    textField("shipping_city") {
        label("City")
        required()
    }

    selectField("shipping_state") {
        label("State")
        options(US_STATES)
        required()
    }

    textField("shipping_zip") {
        label("ZIP Code")
        required()
        pattern("\\d{5}")
    }

    // Payment
    selectField("payment_method") {
        label("Payment Method")
        options(listOf("credit_card", "paypal", "apple_pay"))
        required()
    }

    // Terms
    booleanField("agree_terms") {
        label("I agree to Terms & Conditions")
        required()
    }

    booleanField("subscribe_newsletter") {
        label("Send me promotional emails")
        defaultValue(false)
    }
}

// Usage
val binding = checkoutForm.bind()
binding.onChange { field, value ->
    persistDraft(binding.getData())
}

// Check if ready to submit
val completion = binding.getCompletion()
if (completion.canSubmit) {
    val result = binding.validate()
    if (result is ValidationResult.Success) {
        processPayment(binding.getData())
    }
}
```

### 15.9 Best Practices

**DO**:
- ✅ Use semantic field types (emailField, passwordField, etc.)
- ✅ Add validation rules incrementally
- ✅ Always use `required()` for mandatory fields
- ✅ Provide clear labels and error messages
- ✅ Use `unique()` and `indexed()` appropriately
- ✅ Generate and review SQL schema before deployment
- ✅ Handle ValidationExceptions when setting values
- ✅ Track completion for multi-step forms

**DON'T**:
- ❌ Store plain text passwords (hash before storage)
- ❌ Skip validation on client side (always validate)
- ❌ Use overly restrictive patterns (frustrates users)
- ❌ Forget to provide default values for optional fields
- ❌ Ignore SQL dialect differences
- ❌ Create forms with 50+ fields (break into steps)

### 15.10 Future Enhancements

**Phase 5.1 - Advanced Features**:
- Nested forms (complex object structures)
- Conditional fields (show/hide based on values)
- Field dependencies (cross-field validation)
- Calculated fields (computed from other fields)
- Field groups (logical grouping with collapse/expand)

**Phase 5.2 - Async Validation**:
- Async validators (username availability, etc.)
- Debouncing (delay validation during typing)
- Cancellation (cancel pending validations)
- Loading states (track async validation progress)

**Phase 5.3 - Enhanced Field Types**:
- File upload field
- Rich text editor
- Location picker (coordinates)
- Color picker
- Time/DateTime fields

**Phase 5.4 - Internationalization**:
- Localized labels
- Translated error messages
- Locale-aware date/number formatting
- RTL support

---

## 16. AvaCode Workflows System

### 16.1 Overview

**AvaCode Workflows** is a state machine engine for orchestrating multi-step processes with conditional branching, progress tracking, and persistence. It integrates seamlessly with AvaCode Forms to create complex user journeys like onboarding, checkout, surveys, and wizards.

**Key Features**:
- **Declarative DSL**: Kotlin builders for workflow definition
- **State machine**: 5 workflow states, 5 step states
- **Conditional branching**: Show/hide steps based on data
- **Navigation**: next, back, skip, jumpTo with validation
- **Form integration**: Embed FormDefinition in workflow steps
- **Progress tracking**: Real-time percentage and step counts
- **Persistence**: Save/resume workflows across sessions
- **Lifecycle callbacks**: onEnter, onComplete, onSkip hooks

**Module**: `Universal/IDEAMagic/AvaCode/Workflows`
**Package**: `com.augmentalis.avamagic.avacode.workflows`

### 16.2 Quick Start

```kotlin
// Define a multi-step onboarding workflow
val onboarding = workflow("user_onboarding") {
    title("Welcome to Our App")
    description("Complete your profile in 3 easy steps")
    allowBack(true)

    step("registration") {
        title("Create Account")
        form(userRegistrationForm)
        onComplete { data ->
            sendVerificationEmail(data["email"] as String)
        }
    }

    step("profile") {
        title("Build Your Profile")
        form(profileForm)
        onComplete { data ->
            uploadProfilePicture(data["avatar"] as File?)
        }
    }

    step("payment") {
        title("Choose Your Plan")
        form(paymentForm)
        // Only show for premium accounts
        condition { data -> data["account_type"] == "premium" }
        onComplete { data ->
            processPayment(data)
        }
    }

    step("preferences") {
        title("Set Your Preferences")
        form(preferencesForm)
        allowSkip(true)
        onSkip { data ->
            useDefaultPreferences(data)
        }
    }
}

// Create workflow instance
var instance = onboarding.createInstance()

// Execute workflow
while (instance.state == WorkflowState.IN_PROGRESS) {
    // Display current step
    val currentStep = instance.getCurrentStep()
    displayStep(currentStep)

    // Collect user input
    val stepData = collectUserInput(currentStep)

    // Progress to next step
    when (val result = instance.next(stepData)) {
        is WorkflowResult.Success -> instance = result.instance
        is WorkflowResult.ValidationFailed -> showErrors(result.errors)
        is WorkflowResult.Error -> handleError(result.message)
        is WorkflowResult.Completed -> {
            instance = result.instance
            completeOnboarding(instance.data)
        }
    }
}

// Track progress
val progress = instance.getProgress()
println("Step ${progress.currentStep}/${progress.totalSteps} (${progress.percentage}%)")
```

### 16.3 Workflow Definition

#### 16.3.1 Basic Workflow

```kotlin
val workflow = workflow("workflow_id") {
    title("Workflow Title")
    description("Workflow description")
    version("1.0.0")
    allowBack(true)   // Enable back navigation
    allowSkip(false)  // Disable skip by default

    step("step1") { /* ... */ }
    step("step2") { /* ... */ }
    step("step3") { /* ... */ }
}
```

#### 16.3.2 Step Configuration

```kotlin
step("step_id") {
    title("Step Title")
    description("Step description")
    form(myForm)           // Optional: Embed form
    allowBack(true)        // Override workflow setting
    allowSkip(false)       // Override workflow setting

    // Conditional display
    condition { data ->
        data["account_type"] == "premium"
    }

    // Auto-skip logic
    skipIf { data ->
        data["skip_payment"] == true
    }

    // Custom validation (beyond form validation)
    validation { data ->
        if (data["age"] as Int < 18) {
            ValidationResult.Failure(mapOf(
                "age" to listOf("Must be 18 or older")
            ))
        } else {
            ValidationResult.Success
        }
    }

    // Lifecycle callbacks
    onEnter { data ->
        logStepEntry("step_id", data)
    }

    onComplete { data ->
        saveProgress(data)
        sendAnalytics("step_completed", "step_id")
    }

    onSkip { data ->
        logSkip("step_id")
    }
}
```

### 16.4 Workflow States

#### 16.4.1 Workflow States

```kotlin
enum class WorkflowState {
    NOT_STARTED,    // Initial state
    IN_PROGRESS,    // Actively being executed
    COMPLETED,      // Successfully finished
    CANCELLED,      // User cancelled
    FAILED          // Error occurred
}
```

#### 16.4.2 Step States

```kotlin
enum class StepState {
    PENDING,        // Not yet reached
    IN_PROGRESS,    // Currently active
    COMPLETED,      // Successfully finished
    SKIPPED,        // Skipped by user or condition
    FAILED          // Validation failed
}
```

### 16.5 Navigation

#### 16.5.1 Next Step

```kotlin
val result = instance.next(stepData)
when (result) {
    is WorkflowResult.Success -> {
        instance = result.instance
        // Continue to next step
    }
    is WorkflowResult.Completed -> {
        instance = result.instance
        // Workflow finished!
    }
    is WorkflowResult.ValidationFailed -> {
        // Show validation errors
        showErrors(result.errors)
    }
    is WorkflowResult.Error -> {
        // Handle error
        log.error(result.message)
    }
}
```

#### 16.5.2 Back Navigation

```kotlin
val result = instance.back()
when (result) {
    is WorkflowResult.Success -> {
        instance = result.instance
        // Moved to previous step
    }
    is WorkflowResult.Error -> {
        // Back not allowed or at first step
    }
    else -> { /* ... */ }
}
```

#### 16.5.3 Skip Current Step

```kotlin
if (instance.canSkipCurrent()) {
    val result = instance.skip()
    when (result) {
        is WorkflowResult.Success -> {
            instance = result.instance
            // Step skipped
        }
        else -> { /* ... */ }
    }
}
```

#### 16.5.4 Jump to Specific Step

```kotlin
val result = instance.jumpTo("preferences")
when (result) {
    is WorkflowResult.Success -> {
        instance = result.instance
        // Jumped to step
    }
    is WorkflowResult.Error -> {
        // Step not found or not accessible
    }
    else -> { /* ... */ }
}
```

### 16.6 Conditional Branching

#### 16.6.1 Condition (Step doesn't exist)

```kotlin
step("premium_features") {
    // Step only exists if user is premium
    condition { data ->
        data["account_type"] == "premium"
    }
    // ... step config
}
```

**Behavior**: If condition returns `false`, step is not included in workflow at all (not counted in progress).

#### 16.6.2 SkipIf (Step auto-skipped)

```kotlin
step("payment") {
    // Step exists but auto-skips for free users
    skipIf { data ->
        data["account_type"] == "free"
    }
    // ... step config
}
```

**Behavior**: If skipIf returns `true`, step is automatically skipped (counted in progress as SKIPPED).

### 16.7 Form Integration

#### 16.7.1 Embedding Forms in Steps

```kotlin
val registrationForm = form("registration") {
    textField("username") { /* ... */ }
    emailField("email") { /* ... */ }
}

val workflow = workflow("onboarding") {
    step("register") {
        form(registrationForm)  // Embed form
        onComplete { data ->
            // data contains all form fields
            val username = data["username"] as String
            createUser(username)
        }
    }
}
```

#### 16.7.2 Form Validation in Workflow

When a step has a form, validation happens automatically:

```kotlin
// User submits step with invalid data
val result = instance.next(mapOf("email" to "invalid"))

// Result is ValidationFailed with form errors
when (result) {
    is WorkflowResult.ValidationFailed -> {
        result.errors.forEach { (field, errors) ->
            println("$field: ${errors.joinToString()}")
        }
        // Output: email: Invalid email address
    }
    else -> { /* ... */ }
}
```

### 16.8 Progress Tracking

```kotlin
val progress = instance.getProgress()

// Current position
println("Current step: ${progress.currentStep}")
println("Total steps: ${progress.totalSteps}")
println("Current step ID: ${progress.currentStepId}")

// Counts
println("Completed: ${progress.completedSteps}")
println("Pending: ${progress.totalSteps - progress.completedSteps}")

// Percentage
println("Progress: ${progress.percentage}%")

// Status
println("Is complete: ${progress.isComplete}")
```

**Progress Calculation**:
- Only counts steps that pass `condition` check
- Includes COMPLETED and SKIPPED steps
- Excludes PENDING and IN_PROGRESS steps

### 16.9 Persistence

#### 16.9.1 Serialization

```kotlin
// Serialize workflow instance
val serialized: Map<String, Any?> = WorkflowPersistence.serialize(instance)

// Save to storage
localStorage.save("workflow_${userId}", serialized)
database.insert("workflows", serialized)
sharedPreferences.putString("workflow", json.encode(serialized))
```

#### 16.9.2 Deserialization

```kotlin
// Load from storage
val data = localStorage.load("workflow_${userId}")

// Deserialize to workflow instance
val instance = WorkflowPersistence.deserialize(data, workflowDefinition)

// Resume workflow
if (instance != null && instance.state == WorkflowState.IN_PROGRESS) {
    continueWorkflow(instance)
}
```

#### 16.9.3 Storage Interface

```kotlin
interface WorkflowStorage {
    suspend fun save(key: String, instance: WorkflowInstance): Boolean
    suspend fun load(key: String, workflow: WorkflowDefinition): WorkflowInstance?
    suspend fun delete(key: String): Boolean
    suspend fun list(): List<String>
}

// In-memory implementation
class InMemoryWorkflowStorage : WorkflowStorage {
    private val storage = mutableMapOf<String, Map<String, Any?>>()

    override suspend fun save(key: String, instance: WorkflowInstance): Boolean {
        storage[key] = WorkflowPersistence.serialize(instance)
        return true
    }

    override suspend fun load(key: String, workflow: WorkflowDefinition): WorkflowInstance? {
        val data = storage[key] ?: return null
        return WorkflowPersistence.deserialize(data, workflow)
    }

    override suspend fun delete(key: String): Boolean {
        storage.remove(key)
        return true
    }

    override suspend fun list(): List<String> = storage.keys.toList()
}
```

### 16.10 Complete Example: E-commerce Checkout

```kotlin
val checkoutWorkflow = workflow("checkout") {
    title("Complete Your Purchase")
    description("4-step checkout process")
    allowBack(true)

    step("cart_review") {
        title("Review Your Cart")
        form(form("cart") {
            numberField("quantity") {
                min(1.0)
                max(99.0)
            }
            selectField("shipping_speed") {
                options(listOf("standard", "express", "overnight"))
            }
        })
    }

    step("shipping") {
        title("Shipping Address")
        form(shippingForm)
        onComplete { data ->
            calculateShippingCost(data)
        }
    }

    step("payment") {
        title("Payment Information")
        form(paymentForm)
        allowBack(false)  // Security: can't go back from payment
        onComplete { data ->
            val result = processPayment(data)
            if (!result.success) {
                throw Exception("Payment failed: ${result.error}")
            }
        }
    }

    step("confirmation") {
        title("Order Confirmation")
        allowBack(false)
        onEnter { data ->
            val orderId = createOrder(data)
            sendConfirmationEmail(data["email"] as String, orderId)
        }
    }
}

// Usage
var instance = checkoutWorkflow.createInstance(
    mapOf("cart_items" to cartItems)
)

// Execute checkout
fun executeCheckout() {
    when (instance.state) {
        WorkflowState.IN_PROGRESS -> {
            val step = instance.getCurrentStep()
            displayCheckoutStep(step, instance.data)
        }
        WorkflowState.COMPLETED -> {
            showOrderConfirmation(instance.data)
        }
        else -> {
            redirectToCart()
        }
    }
}

// Handle step submission
fun submitStep(stepData: Map<String, Any?>) {
    when (val result = instance.next(stepData)) {
        is WorkflowResult.Success -> {
            instance = result.instance
            saveCheckoutProgress(instance)
            executeCheckout()
        }
        is WorkflowResult.ValidationFailed -> {
            showValidationErrors(result.errors)
        }
        is WorkflowResult.Completed -> {
            instance = result.instance
            completeOrder(instance.data)
        }
        is WorkflowResult.Error -> {
            showError(result.message)
        }
    }
}
```

### 16.11 History Tracking

```kotlin
// Access workflow history
val history = instance.history

history.forEach { transition ->
    println("${transition.timestamp}: ${transition.fromStep} → ${transition.toStep}")
    println("  Action: ${transition.action}")
    println("  Data: ${transition.data}")
}

// Example output:
// 2025-11-06T10:30:00: null → registration (START)
// 2025-11-06T10:32:15: registration → profile (NEXT)
// 2025-11-06T10:35:42: profile → payment (NEXT)
// 2025-11-06T10:36:10: payment → profile (BACK)
// 2025-11-06T10:37:58: profile → payment (NEXT)
// 2025-11-06T10:40:22: payment → preferences (SKIP)
```

### 16.12 Best Practices

**DO**:
- ✅ Use meaningful workflow and step IDs
- ✅ Add titles and descriptions for UX
- ✅ Validate data at each step (don't wait until end)
- ✅ Use conditions for branching logic
- ✅ Use skipIf for optional steps
- ✅ Save progress after each step
- ✅ Handle all WorkflowResult cases
- ✅ Disable back navigation on sensitive steps (payment)
- ✅ Track progress and show users where they are

**DON'T**:
- ❌ Create workflows with 20+ steps (break into separate workflows)
- ❌ Allow back navigation from payment/confirmation steps
- ❌ Store sensitive data unencrypted when persisting
- ❌ Skip validation (always validate before progressing)
- ❌ Ignore error cases in workflow execution
- ❌ Forget to clean up completed workflows from storage

### 16.13 Future Enhancements

**Phase 6.1 - Advanced Features**:
- Parallel steps (fork/join for concurrent tasks)
- Async validation support
- Step timeouts with auto-fail/skip
- Retry logic for failed steps
- Workflow versioning and migration

**Phase 6.2 - Enhanced Persistence**:
- Direct SQL database storage
- Encryption for sensitive data
- Compression to reduce size
- Conflict resolution for concurrent edits

**Phase 6.3 - Analytics & Monitoring**:
- Step analytics (completion rates, drop-offs)
- Performance monitoring (step duration)
- A/B testing (different workflow variations)
- Funnel analysis (conversion tracking)

---

## Appendix A: Revision History

### Version 2.0.0 (2025-11-06)

**Major Update**: Added comprehensive AvaCode Forms and Workflows documentation

**New Chapters**:
- Chapter 15: AvaCode Forms System (85+ pages)
  - Form DSL with 8 field types
  - 16 validation rules
  - Database schema generation for 4 SQL dialects
  - Form binding, change tracking, completion tracking
  - Complete e-commerce checkout example

- Chapter 16: AvaCode Workflows System (70+ pages)
  - State machine with 5 workflow states, 5 step states
  - Conditional branching (condition vs skipIf)
  - Navigation (next, back, skip, jumpTo)
  - Form integration
  - Progress tracking and persistence
  - Complete onboarding and checkout examples

**Updates**:
- Updated header to include AvaCode modules
- Updated table of contents with new chapters
- Updated module listing to include Forms and Workflows

**Statistics**:
- Total pages: ~450 (was ~280)
- New content: ~170 pages
- Code examples: 100+ new examples
- API references: 50+ new methods documented

### Version 1.0.0 (2025-11-05)

**Initial Release**: Complete documentation for IDEAMagic UI Components

**Chapters**:
1. Overview
2. Architecture
3. Base Type System
4. Component Interface
5. Styling System
6. Modifier System
7. Rendering System
8. Core Components Catalog (39 components)
9. Foundation Components Catalog (Magic* components)
10. Creating Custom Components
11. Platform Renderers
12. Testing Components
13. Best Practices
14. API Reference

**Coverage**:
- Phases 1-4 complete (Base Types, UI Components, 3D)
- 39 UI components documented
- Compose renderer architecture
- Type-safe styling and modifier systems

---

## Appendix B: Migration from Old Components

### A.1 Adding Base Types to Existing Components

**Before** (Plain data class):
```kotlin
data class ButtonComponent(
    val text: String,
    val size: Size = Size.MD
)
```

**After** (With base types):
```kotlin
data class ButtonComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val text: String,
    val size: Size = Size.MD
) : Component {
    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)
}
```

### B.2 Migration Checklist

- [ ] Add Component interface implementation
- [ ] Add id, style, modifiers properties
- [ ] Implement render() method
- [ ] Replace custom styling with ComponentStyle
- [ ] Replace custom onClick with Clickable modifier
- [ ] Add Testable modifier for testing
- [ ] Add Accessible modifier for accessibility
- [ ] Update tests to use new properties
- [ ] Update renderers to handle new component

---

## Appendix C: Future Enhancements

### C.1 Planned Additions

**Layout Types**:
- FlexDirection enum (ROW, COLUMN, ROW_REVERSE, COLUMN_REVERSE)
- JustifyContent enum (main axis alignment)
- AlignItems enum (cross axis alignment)
- Wrap enum (WRAP, NOWRAP, WRAP_REVERSE)

**Additional Modifiers**:
- Scrollable (onScroll: (offset) -> Unit)
- Hoverable (onHover, onExit)
- Selectable (selected, onSelect)
- Disabled (disabled flag)
- Hidden (visibility flag)

**Additional Events**:
- KeyEvent (key, modifiers, type)
- ScrollEvent (offsetX, offsetY, deltaX, deltaY)
- HoverEvent (x, y, entered)
- SelectionEvent (start, end, text)

**Theme Support**:
- Theme data class (colors, typography, spacing, shapes)
- ThemeMode enum (LIGHT, DARK, SYSTEM)
- ThemeContext for renderer

**Validation**:
- ComponentStyle validation (opacity 0.0-1.0, borderWidth >= 0)
- Size constraints (minWidth, maxWidth, minHeight, maxHeight)
- Content constraints (maxLength for text)

### C.2 Renderer Implementations

**ComposeRenderer** (Week 5): Android/Desktop rendering
**SwiftUIRenderer** (Week 6): iOS/macOS rendering
**HTMLRenderer** (Week 7): Web rendering

---

## Appendix D: Resources

### D.1 Documentation

- **Base Types Implementation**: `COMPONENT-BASE-TYPES-COMPLETE-251105-0102.md`
- **Gap Analysis**: `COMPONENT-GAP-ANALYSIS-251104.md`
- **Architecture**: `ARCHITECTURE-RESTRUCTURE-COMPLETE-251104.md`
- **IDEACODE Spec**: `specs/003-.../spec.md`
- **IDEACODE Plan**: `specs/003-.../plan.md`

### D.2 Code Locations

- **Base Types**: `Universal/IDEAMagic/UI/Core/src/commonMain/kotlin/com/augmentalis/avamagic/ui/core/base/`
- **Core Components**: `Universal/IDEAMagic/UI/Core/src/commonMain/kotlin/com/augmentalis/avamagic/ui/core/`
- **Foundation**: `Universal/IDEAMagic/UI/Foundation/src/commonMain/kotlin/com/augmentalis/avamagic/ui/foundation/`
- **Tests**: `Universal/IDEAMagic/UI/Core/src/commonTest/kotlin/`

### D.3 Related Systems

- **AvaCode DSL**: Declarative UI description language (separate from UI Core)
- **Content Injection**: IPC-based UI injection for AvanueLaunch panels
- **VoiceOS Integration**: Voice-controlled UI components

---

**End of Developer Manual v2.0.0**
**Last Updated**: 2025-11-06
**Next Review**: 2026-01-06 (After Phase 7 completion)

**Changes in v2.0.0**:
- Added Chapter 15: AvaCode Forms System (complete documentation)
- Added Chapter 16: AvaCode Workflows System (complete documentation)
- Added Appendix A: Revision History
- Updated all references to include Forms and Workflows modules
- 170+ pages of new content
- 100+ new code examples
