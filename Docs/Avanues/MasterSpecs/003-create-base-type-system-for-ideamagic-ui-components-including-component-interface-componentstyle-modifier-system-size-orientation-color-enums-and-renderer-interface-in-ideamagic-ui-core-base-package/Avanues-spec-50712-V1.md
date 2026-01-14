# create-base-type-system-for-avamagic-ui-components-including-component-interface-componentstyle-modifier-system-size-orientation-color-enums-and-renderer-interface-in-avamagic-ui-core-base-package - Feature Specification

**Feature ID:** 003
**Created:** 2025-11-05T08:52:19.406Z
**Profile:** library
**Status:** Draft

---

## Executive Summary

Implement Create base type system for IDEAMagic UI components including Component interface, ComponentStyle, Modifier system, Size/Orientation/Color enums, and Renderer interface in avamagic.ui.core.base package

---

## Problem Statement

**Current State:**
The IDEAMagic UI component library has 44 working data class components but lacks a foundational type system. This has resulted in:
- 15 components removed due to missing base types (DataGrid, Divider, Skeleton, Stepper, Table, Timeline, FileUpload, Radio, ToggleButtonGroup, AppBar, FAB, StickyHeader, StatCard, NotificationCenter)
- No standardized way to render components across platforms (Compose, SwiftUI, HTML)
- No consistent styling system
- No modifier/decorator pattern for component customization
- No type-safe enums for common properties (Size, Orientation, Color)

**Pain Points:**
- Cannot restore 15 removed components without base type definitions
- Cannot implement 56 missing components for Flutter/SwiftUI parity
- Components are plain data classes with no behavioral contracts
- No cross-platform rendering abstraction
- Inconsistent styling approaches across components
- No way to apply modifiers/decorators to components

**Desired State:**
A complete base type system that:
- Defines Component interface as foundation for all UI elements
- Provides ComponentStyle for consistent styling
- Implements Modifier system for component decoration
- Defines common enums (Size, Orientation, Color, Position, etc.)
- Abstracts rendering through Renderer interface
- Enables restoration of 15 removed components
- Unblocks implementation of 56 missing components
- Supports cross-platform rendering (Compose, SwiftUI, HTML)

---

## Requirements

### Functional Requirements

#### 1. Component Interface
- Define `Component` interface with:
  - `id: String?` - Unique identifier (optional)
  - `style: ComponentStyle?` - Styling configuration (optional)
  - `modifiers: List<Modifier>` - Applied decorators (default empty)
  - `render(renderer: Renderer): Any` - Cross-platform rendering method

#### 2. ComponentStyle Class
- Sealed class hierarchy for styling:
  - `padding: Padding?` - Component padding
  - `margin: Margin?` - Component margin
  - `backgroundColor: Color?` - Background color
  - `borderColor: Color?` - Border color
  - `borderWidth: Float?` - Border thickness
  - `borderRadius: Float?` - Corner radius
  - `elevation: Float?` - Shadow/elevation
  - `opacity: Float?` - Transparency (0.0-1.0)
- Support for style composition/merging

#### 3. Modifier System
- `Modifier` sealed interface with common implementations:
  - `Clickable(onClick: () -> Unit)` - Click handler
  - `Draggable(onDrag: (DragEvent) -> Unit)` - Drag behavior
  - `Focusable(onFocus: () -> Unit)` - Focus handling
  - `Testable(testId: String)` - Test automation support
  - `Accessible(contentDescription: String)` - Accessibility
  - `Animated(animation: Animation)` - Animation specs
- Modifier chaining support

#### 4. Common Enums
- `Size`: XS, SM, MD, LG, XL
- `Orientation`: HORIZONTAL, VERTICAL
- `Color`: PRIMARY, SECONDARY, SUCCESS, WARNING, ERROR, INFO, LIGHT, DARK (with RGB/HSL values)
- `Position`: TOP, BOTTOM, LEFT, RIGHT, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
- `Alignment`: START, CENTER, END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY
- `Severity`: INFO, SUCCESS, WARNING, ERROR, CRITICAL

#### 5. Renderer Interface
- Define `Renderer` interface for cross-platform rendering:
  - `renderComponent(component: Component): Any` - Generic render
  - Platform-specific implementations:
    - `ComposeRenderer` - Jetpack Compose (Android/Desktop)
    - `SwiftUIRenderer` - SwiftUI (iOS/macOS)
    - `HTMLRenderer` - Web/HTML output
- Support for nested component rendering
- Context passing (theme, locale, etc.)

#### 6. Supporting Types
- `Padding(top, right, bottom, left)` - Spacing values
- `Margin(top, right, bottom, left)` - External spacing
- `Animation(type, duration, easing)` - Animation specs
- `DragEvent(x, y, deltaX, deltaY)` - Drag data

### Non-Functional Requirements

1. **Performance**
   - Component creation < 1ms
   - Rendering dispatch < 5ms
   - Zero allocation for modifier chains (use inline functions)

2. **Compatibility**
   - Kotlin Multiplatform compatible (JVM, Android, iOS, JS)
   - Works with existing 44 data class components
   - Backward compatible with current Core module structure

3. **Code Quality**
   - 100% KDoc coverage for public APIs
   - Comprehensive unit tests (>90% coverage)
   - Example usage for each type
   - Follows Kotlin coding conventions

4. **Architecture**
   - Package: `com.augmentalis.avamagic.ui.core.base`
   - Location: `Universal/IDEAMagic/UI/Core/src/commonMain/kotlin/com/augmentalis/avamagic/ui/core/base/`
   - Clean separation: types, interfaces, implementations
   - No external dependencies (standalone module)

### Success Criteria

- [ ] All base types compile in Core module (JVM + Android + iOS)
- [ ] Component interface can be implemented by data classes
- [ ] ComponentStyle supports composition and merging
- [ ] Modifier system supports chaining
- [ ] All enums have complete value sets
- [ ] Renderer interface supports all platforms
- [ ] 15 removed components can be restored using these types
- [ ] Unit tests pass with >90% coverage
- [ ] KDoc documentation complete
- [ ] Example usage provided for each type

---

## User Stories

### Story 1: Component Developer

**As a** component library developer
**I want** a Component interface with style and modifier support
**So that** I can create standardized UI components with consistent behavior

**Acceptance Criteria:**
- [ ] I can implement Component interface in data classes
- [ ] I can apply ComponentStyle to customize appearance
- [ ] I can chain modifiers for additional behaviors
- [ ] Components render correctly on all platforms

### Story 2: Platform Renderer Developer

**As a** platform renderer developer
**I want** a Renderer interface that abstracts platform-specific rendering
**So that** I can render components on Compose, SwiftUI, and HTML without changing component definitions

**Acceptance Criteria:**
- [ ] I can implement Renderer for my platform (Compose/SwiftUI/HTML)
- [ ] Renderer receives Component and returns platform-specific output
- [ ] Renderer handles nested components correctly
- [ ] Context (theme, locale) propagates through rendering

### Story 3: App Developer

**As an** application developer
**I want** type-safe enums for common properties (Size, Color, Position)
**So that** I can configure components without magic strings or arbitrary values

**Acceptance Criteria:**
- [ ] Size enum supports XS, SM, MD, LG, XL
- [ ] Color enum covers all standard UI colors
- [ ] Position enum covers all layout positions
- [ ] Enums are exhaustive (when expressions don't need else)

### Story 4: Component Restorer

**As a** maintainer restoring removed components
**I want** all base types needed to compile DataGrid, Divider, FAB, etc.
**So that** I can restore the 15 removed components without compilation errors

**Acceptance Criteria:**
- [ ] Component interface available for implementation
- [ ] ComponentStyle available for styling
- [ ] Modifier available for decorators
- [ ] Renderer available for cross-platform rendering
- [ ] All enums available (Size, Orientation, Color, Position, Alignment, Severity)


---

## Technical Constraints

1. **Kotlin Multiplatform** - Must compile for JVM, Android, iOS, JS targets
2. **Zero External Dependencies** - Core module must remain standalone (no Compose, no SwiftUI dependencies in base types)
3. **Existing Components** - Must not break 44 existing data class components
4. **Kotlin 1.9.20** - Limited to Kotlin 1.9.20 due to Compose 1.5.10 compatibility
5. **Compose Compatibility** - Must work with Jetpack Compose 1.5.10 and Material3 1.2.1
6. **Gradle 8.5** - Build configuration for Gradle 8.5
7. **Package Structure** - Must fit in `com.augmentalis.avamagic.ui.core.base`

---

## Dependencies

### Internal Dependencies

1. **UI:Core Module** - Base types will be added to existing Core module
   - Current: 44 data class components
   - Package: `com.augmentalis.avamagic.ui`
   - Location: `Universal/IDEAMagic/UI/Core/`

2. **CoreTypes Module** - May reference existing core types if available
   - Location: `Universal/IDEAMagic/CoreTypes/`
   - Types: ActionResult, validation utilities

### External Dependencies

**None** - Base types are foundational and must have zero external dependencies. Platform-specific renderer implementations (ComposeRenderer, SwiftUIRenderer) will be in separate modules with their own dependencies.

---

## Out of Scope

### Explicitly Out of Scope

1. **Platform Renderer Implementations** - ComposeRenderer, SwiftUIRenderer, HTMLRenderer will be separate follow-up tasks
2. **Existing Component Migration** - 44 existing components will NOT be migrated to implement Component interface in this phase
3. **DSL Builders** - Component DSL builders (e.g., `button { }`) are for AvaCode, not Core
4. **Theme System** - Comprehensive theming (light/dark mode, brand colors) is separate
5. **Animation Engine** - Animation implementation (only Animation specs)
6. **Layout System** - Advanced layout containers (FlexBox, Grid) are separate components
7. **State Management** - Component state handling is separate concern
8. **Validation Framework** - Input validation beyond basic type safety

### May Be Included (If Time Permits)

1. Basic example implementations showing how to use each type
2. Migration guide for converting data classes to Component implementations
3. Utility functions for common operations (style merging, modifier chaining)

---

## Technical Architecture

### Package Structure

```
com.augmentalis.avamagic.ui.core.base/
├── Component.kt              # Component interface
├── ComponentStyle.kt         # Styling system
├── Modifier.kt               # Modifier sealed interface
├── Renderer.kt               # Renderer interface
├── enums/
│   ├── Size.kt              # XS, SM, MD, LG, XL
│   ├── Orientation.kt       # HORIZONTAL, VERTICAL
│   ├── Color.kt             # PRIMARY, SECONDARY, etc.
│   ├── Position.kt          # TOP, BOTTOM, LEFT, RIGHT, etc.
│   ├── Alignment.kt         # START, CENTER, END, etc.
│   └── Severity.kt          # INFO, SUCCESS, WARNING, ERROR, CRITICAL
├── types/
│   ├── Padding.kt           # Spacing values
│   ├── Margin.kt            # External spacing
│   ├── Animation.kt         # Animation specs
│   └── DragEvent.kt         # Drag event data
└── modifiers/
    ├── Clickable.kt         # Click modifier
    ├── Draggable.kt         # Drag modifier
    ├── Focusable.kt         # Focus modifier
    ├── Testable.kt          # Test modifier
    ├── Accessible.kt        # Accessibility modifier
    └── Animated.kt          # Animation modifier
```

### Type Definitions

#### Component Interface
```kotlin
interface Component {
    val id: String? get() = null
    val style: ComponentStyle? get() = null
    val modifiers: List<Modifier> get() = emptyList()

    fun render(renderer: Renderer): Any
}
```

#### ComponentStyle Data Class
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
) {
    fun merge(other: ComponentStyle): ComponentStyle
    operator fun plus(other: ComponentStyle): ComponentStyle = merge(other)
}
```

#### Modifier System
```kotlin
sealed interface Modifier {
    fun then(other: Modifier): Modifier = CombinedModifier(this, other)
}

data class Clickable(val onClick: () -> Unit) : Modifier
data class Draggable(val onDrag: (DragEvent) -> Unit) : Modifier
data class Focusable(val onFocus: () -> Unit, val onBlur: () -> Unit = {}) : Modifier
data class Testable(val testId: String) : Modifier
data class Accessible(val contentDescription: String, val role: String? = null) : Modifier
data class Animated(val animation: Animation) : Modifier

private data class CombinedModifier(
    val first: Modifier,
    val second: Modifier
) : Modifier
```

#### Renderer Interface
```kotlin
interface Renderer {
    fun renderComponent(component: Component): Any
    fun <T> withContext(key: String, value: T, block: () -> Any): Any
}
```

#### Enums
```kotlin
enum class Size { XS, SM, MD, LG, XL }

enum class Orientation { HORIZONTAL, VERTICAL }

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

enum class Position {
    TOP, BOTTOM, LEFT, RIGHT, CENTER,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

enum class Alignment {
    START, CENTER, END,
    SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY
}

enum class Severity { INFO, SUCCESS, WARNING, ERROR, CRITICAL }
```

#### Supporting Types
```kotlin
data class Padding(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
) {
    constructor(all: Float) : this(all, all, all, all)
    constructor(vertical: Float, horizontal: Float) : this(vertical, horizontal, vertical, horizontal)
}

data class Margin(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
) {
    constructor(all: Float) : this(all, all, all, all)
    constructor(vertical: Float, horizontal: Float) : this(vertical, horizontal, vertical, horizontal)
}

data class Animation(
    val type: AnimationType,
    val duration: Long,
    val easing: Easing = Easing.EASE_IN_OUT
)

enum class AnimationType { FADE, SLIDE, SCALE, ROTATE }
enum class Easing { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

data class DragEvent(
    val x: Float,
    val y: Float,
    val deltaX: Float,
    val deltaY: Float
)
```

### Usage Examples

#### Creating a Component
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

#### Applying Styles and Modifiers
```kotlin
val button = ButtonComponent(
    text = "Click Me",
    style = ComponentStyle(
        padding = Padding(16f),
        backgroundColor = Color.PRIMARY,
        borderRadius = 8f
    ),
    modifiers = listOf(
        Clickable { println("Clicked!") },
        Testable("submit-button"),
        Accessible("Submit form button")
    )
)
```

#### Implementing a Renderer
```kotlin
class ComposeRenderer : Renderer {
    @Composable
    override fun renderComponent(component: Component): Any {
        return when (component) {
            is ButtonComponent -> renderButton(component)
            // ... other components
            else -> Text("Unknown component")
        }
    }

    @Composable
    private fun renderButton(button: ButtonComponent) {
        Button(
            onClick = { button.modifiers.filterIsInstance<Clickable>().firstOrNull()?.onClick?.invoke() },
            modifier = Modifier.padding(button.style?.padding?.toPaddingValues() ?: PaddingValues(0.dp))
        ) {
            Text(button.text)
        }
    }
}
```

---

## Implementation Plan Overview

### Phase 1: Core Interfaces (Day 1)
1. Create `Component.kt` interface
2. Create `Renderer.kt` interface
3. Add KDoc documentation
4. Write unit tests

### Phase 2: Styling System (Day 1)
1. Create `ComponentStyle.kt` data class
2. Implement style merging logic
3. Create `Padding.kt` and `Margin.kt`
4. Add tests for style composition

### Phase 3: Modifier System (Day 2)
1. Create `Modifier.kt` sealed interface
2. Implement 6 common modifiers
3. Add modifier chaining support
4. Add tests for modifier combinations

### Phase 4: Enums (Day 2)
1. Create 6 enum files
2. Add RGB values to Color enum
3. Document each enum value
4. Add tests

### Phase 5: Supporting Types (Day 3)
1. Create `Animation.kt` with enums
2. Create `DragEvent.kt`
3. Add convenience constructors
4. Add tests

### Phase 6: Documentation & Examples (Day 3)
1. Complete KDoc for all public APIs
2. Create usage examples
3. Write migration guide
4. Update architecture docs

---

## Next Steps

1. **Review this specification** - Verify completeness and accuracy
2. **Run `ideacode_plan`** - Generate detailed implementation plan
3. **Create base/ package** - Set up directory structure
4. **Begin implementation** - Start with Phase 1 (Core Interfaces)

---

## Verification Checklist

Before marking this spec as complete, verify:

- [ ] Problem statement clearly explains current pain points
- [ ] All 6 functional requirements are detailed
- [ ] Non-functional requirements cover performance, compatibility, quality
- [ ] Success criteria are measurable
- [ ] User stories cover all stakeholders
- [ ] Technical constraints are documented
- [ ] Dependencies are identified
- [ ] Out of scope items are explicit
- [ ] Technical architecture shows package structure
- [ ] Type definitions include code examples
- [ ] Usage examples demonstrate each feature
- [ ] Implementation plan shows phases
- [ ] Restoration of 15 components is addressed

---

**Generated:** Autonomously by IDEACODE MCP Server
**Enhanced:** With detailed technical requirements and architecture
**Last Updated:** 2025-11-05T09:00:00.000Z
**Status:** Ready for Planning Phase
