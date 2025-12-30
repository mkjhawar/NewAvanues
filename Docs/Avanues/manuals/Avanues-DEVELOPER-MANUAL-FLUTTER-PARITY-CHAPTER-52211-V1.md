# Chapter 30: Flutter Parity Components

**Version:** 3.0.0-flutter-parity
**Last Updated:** 2025-11-22
**Target Audience:** Developers migrating from Flutter or seeking 100% component parity
**Status:** ✅ Android Complete (58 components)

---

## Table of Contents

### 30.1 [Overview](#301-overview)
- Flutter Parity Achievement
- Component Categories
- Platform Support Matrix

### 30.2 [Implicit Animations (8 components)](#302-implicit-animations)
- AnimatedContainer
- AnimatedOpacity
- AnimatedPositioned
- AnimatedDefaultTextStyle
- AnimatedPadding
- AnimatedSize
- AnimatedAlign
- AnimatedScale

### 30.3 [Transitions & Hero (15 components)](#303-transitions--hero)
- FadeTransition
- SlideTransition
- Hero (Shared Element Transitions)
- ScaleTransition
- RotationTransition
- And 10 more advanced transitions

### 30.4 [Flex & Positioning Layouts (10 components)](#304-flex--positioning-layouts)
- Wrap
- Expanded
- Flexible
- Flex
- Padding
- Align
- Center
- SizedBox
- ConstrainedBox
- FittedBox

### 30.5 [Advanced Scrolling (7 components)](#305-advanced-scrolling)
- ListView.builder
- ListView.separated
- GridView.builder
- PageView
- ReorderableListView
- CustomScrollView
- Slivers

### 30.6 [Material Chips & Lists (8 components)](#306-material-chips--lists)
- ActionChip
- FilterChip
- ChoiceChip
- InputChip
- CheckboxListTile
- SwitchListTile
- ExpansionTile
- FilledButton

### 30.7 [Advanced Material (10 components)](#307-advanced-material)
- PopupMenuButton
- RefreshIndicator
- IndexedStack
- And 7 more components

### 30.8 [Migration Guide](#308-migration-guide)
- Flutter to AVAMagic Mapping
- Code Examples
- Common Patterns

### 30.9 [Performance Considerations](#309-performance-considerations)
- Animation Performance
- List Rendering Optimization
- Memory Management

### 30.10 [API Reference](#3010-api-reference)
- Quick Lookup Table
- Property Mappings

---

## 30.1 Overview

### Flutter Parity Achievement

AVAMagic has achieved **100% Flutter component parity** on Android with 58 new components added in version 3.0.0.

```
┌─────────────────────────────────────────────────────────────────┐
│                   FLUTTER PARITY MILESTONE                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Before: 112/170 components (66% parity) 🟡                     │
│  After:  170/170 components (100% parity) ✅                    │
│                                                                 │
│  New Components: 58                                             │
│  Test Coverage: 647 tests (94%)                                 │
│  Lines of Code: ~18,511                                         │
│  Documentation: 100% KDoc coverage                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Component Categories

The 58 Flutter Parity components are organized into 6 major categories:

| Category | Components | Use Cases | Priority |
|----------|-----------|-----------|----------|
| **Implicit Animations** | 8 | Property-based animations | P0 |
| **Transitions & Hero** | 15 | Navigation transitions, shared elements | P0 |
| **Flex Layouts** | 10 | Responsive layouts, flexible sizing | P0 |
| **Advanced Scrolling** | 7 | Efficient lists, grids, pages | P0 |
| **Material Chips & Lists** | 8 | Selection, filtering, expandable content | P1 |
| **Advanced Material** | 10 | Menus, refresh, stacks, rich text | P1 |

### Platform Support Matrix

| Platform | Status | Components Available | Renderers |
|----------|--------|---------------------|-----------|
| **Android** | ✅ Complete | 58/58 (100%) | 5 mapper files |
| **iOS** | 🔴 Pending | 0/58 (0%) | Planned Week 3 |
| **Web** | 🔴 Pending | 0/58 (0%) | Planned Week 3 |
| **Desktop** | 🔴 Pending | 0/58 (0%) | Planned Week 3 |

---

## 30.2 Implicit Animations

Implicit animations automatically animate property changes without manual animation controllers.

### AnimatedContainer

A container that animates changes to its properties over a duration.

**Flutter Equivalent:** `AnimatedContainer`

#### API Reference

```kotlin
data class AnimatedContainer(
    val duration: Duration,                    // Required: animation duration
    val curve: Curve = Curve.Linear,          // Animation easing curve
    val alignment: AlignmentGeometry? = null, // Child alignment
    val padding: Spacing? = null,             // Inner padding
    val color: Color? = null,                 // Background color
    val decoration: BoxDecoration? = null,    // Border, shadow, gradient
    val width: Size? = null,                  // Container width
    val height: Size? = null,                 // Container height
    val margin: Spacing? = null,              // Outer spacing
    val transform: Matrix4? = null,           // Transform matrix
    val child: Any? = null,                   // Child widget
    val onEnd: (() -> Unit)? = null          // Completion callback
)
```

#### Usage Example (Kotlin)

```kotlin
var selected by remember { mutableStateOf(false) }

AnimatedContainer(
    duration = Duration.milliseconds(300),
    width = if (selected) Size.dp(200f) else Size.dp(100f),
    height = if (selected) Size.dp(200f) else Size.dp(100f),
    color = if (selected) Colors.Blue else Colors.Red,
    curve = Curves.EaseInOut,
    child = Text("Tap Me"),
    onEnd = { println("Animation completed") }
)
```

#### Flutter Equivalent

```dart
AnimatedContainer(
  duration: Duration(milliseconds: 300),
  width: selected ? 200.0 : 100.0,
  height: selected ? 200.0 : 100.0,
  color: selected ? Colors.blue : Colors.red,
  curve: Curves.easeInOut,
  child: Text('Tap Me'),
  onEnd: () => print('Animation completed'),
)
```

#### Voice DSL Example

```
AnimatedContainer {
  duration: 300ms
  width: selected ? 200 : 100
  height: selected ? 200 : 100
  color: selected ? blue : red
  curve: easeInOut

  Text "Tap Me"

  onEnd: "playSound('success')"
}
```

#### Performance Considerations

- Animations run at **60 FPS** on Android using Jetpack Compose's animation framework
- Multiple property animations are **synchronized** and run in parallel
- Layout changes trigger recomposition **only for affected components**
- Uses **hardware acceleration** for transform animations
- Memory efficient: reuses animation objects

#### Common Use Cases

1. **Expanding/Collapsing Cards**
   ```kotlin
   AnimatedContainer(
       duration = Duration.milliseconds(300),
       height = if (expanded) Size.dp(200f) else Size.dp(80f),
       child = CardContent()
   )
   ```

2. **Color Transitions on State Change**
   ```kotlin
   AnimatedContainer(
       duration = Duration.milliseconds(200),
       color = if (isError) Colors.Red else Colors.Green,
       child = StatusIndicator()
   )
   ```

3. **Smooth Resizing for Responsive UI**
   ```kotlin
   AnimatedContainer(
       duration = Duration.milliseconds(400),
       width = if (isLandscape) Size.dp(600f) else Size.dp(300f),
       curve = Curves.EaseInOut
   )
   ```

---

### AnimatedOpacity

Animates the opacity of a widget.

**Flutter Equivalent:** `AnimatedOpacity`

#### API Reference

```kotlin
data class AnimatedOpacity(
    val opacity: Float,                       // Target opacity (0.0 to 1.0)
    val duration: Duration,                   // Animation duration
    val curve: Curve = Curve.Linear,         // Easing curve
    val onEnd: (() -> Unit)? = null,         // Completion callback
    val child: Any                            // Child widget
)
```

#### Usage Example

```kotlin
var visible by remember { mutableStateOf(true) }

AnimatedOpacity(
    opacity = if (visible) 1.0f else 0.0f,
    duration = Duration.milliseconds(500),
    curve = Curves.EaseIn,
    child = Image("logo.png")
)
```

#### Flutter Equivalent

```dart
AnimatedOpacity(
  opacity: visible ? 1.0 : 0.0,
  duration: Duration(milliseconds: 500),
  curve: Curves.easeIn,
  child: Image.asset('logo.png'),
)
```

---

### AnimatedPositioned

Animates position changes for a child in a Stack.

**Flutter Equivalent:** `AnimatedPositioned`

#### API Reference

```kotlin
data class AnimatedPositioned(
    val duration: Duration,                   // Animation duration
    val curve: Curve = Curve.Linear,         // Easing curve
    val left: Size? = null,                   // Left offset
    val top: Size? = null,                    // Top offset
    val right: Size? = null,                  // Right offset
    val bottom: Size? = null,                 // Bottom offset
    val width: Size? = null,                  // Explicit width
    val height: Size? = null,                 // Explicit height
    val child: Any,                           // Child widget
    val onEnd: (() -> Unit)? = null          // Completion callback
)
```

#### Usage Example

```kotlin
var position by remember { mutableStateOf(Position.TopLeft) }

Stack {
    AnimatedPositioned(
        duration = Duration.milliseconds(400),
        left = if (position == Position.TopLeft) Size.dp(0f) else Size.dp(200f),
        top = if (position == Position.TopLeft) Size.dp(0f) else Size.dp(200f),
        child = FloatingActionButton(icon = "add")
    )
}
```

---

### AnimatedPadding

Animates padding changes.

**Flutter Equivalent:** `AnimatedPadding`

#### API Reference

```kotlin
data class AnimatedPadding(
    val padding: Spacing,                     // Target padding
    val duration: Duration,                   // Animation duration
    val curve: Curve = Curve.Linear,         // Easing curve
    val child: Any,                           // Child widget
    val onEnd: (() -> Unit)? = null          // Completion callback
)
```

---

### AnimatedSize

Animates size changes automatically.

**Flutter Equivalent:** `AnimatedSize`

#### API Reference

```kotlin
data class AnimatedSize(
    val duration: Duration,                   // Animation duration
    val curve: Curve = Curve.Linear,         // Easing curve
    val alignment: AlignmentGeometry = Alignment.Center,
    val child: Any,                           // Child widget
    val onEnd: (() -> Unit)? = null          // Completion callback
)
```

---

### AnimatedAlign

Animates alignment changes.

**Flutter Equivalent:** `AnimatedAlign`

#### API Reference

```kotlin
data class AnimatedAlign(
    val alignment: AlignmentGeometry,         // Target alignment
    val duration: Duration,                   // Animation duration
    val curve: Curve = Curve.Linear,         // Easing curve
    val child: Any,                           // Child widget
    val onEnd: (() -> Unit)? = null          // Completion callback
)
```

---

### AnimatedScale

Animates scale transformations.

**Flutter Equivalent:** `AnimatedScale`

#### API Reference

```kotlin
data class AnimatedScale(
    val scale: Float,                         // Target scale (1.0 = original)
    val duration: Duration,                   // Animation duration
    val curve: Curve = Curve.Linear,         // Easing curve
    val alignment: AlignmentGeometry = Alignment.Center,
    val child: Any,                           // Child widget
    val onEnd: (() -> Unit)? = null          // Completion callback
)
```

#### Usage Example

```kotlin
var pressed by remember { mutableStateOf(false) }

AnimatedScale(
    scale = if (pressed) 0.95f else 1.0f,
    duration = Duration.milliseconds(100),
    child = Button("Click Me")
)
```

---

### AnimatedDefaultTextStyle

Animates text style changes.

**Flutter Equivalent:** `AnimatedDefaultTextStyle`

#### API Reference

```kotlin
data class AnimatedDefaultTextStyle(
    val style: TextStyle,                     // Target text style
    val duration: Duration,                   // Animation duration
    val curve: Curve = Curve.Linear,         // Easing curve
    val child: Any,                           // Child widget
    val onEnd: (() -> Unit)? = null          // Completion callback
)
```

---

## 30.3 Transitions & Hero

Explicit animations using AnimationController and Tween.

### Hero

Creates shared element transitions between screens.

**Flutter Equivalent:** `Hero`

#### API Reference

```kotlin
data class Hero(
    val tag: String,                          // Unique tag for matching
    val child: Any,                           // Child widget
    val createRectTween: ((Rect, Rect) -> RectTween)? = null,
    val flightShuttleBuilder: ((
        BuildContext,
        Animation<Double>,
        HeroFlightDirection,
        BuildContext,
        BuildContext
    ) -> Widget)? = null
)
```

#### Usage Example

```kotlin
// Screen 1: List of items
Hero(
    tag = "product-${product.id}",
    child = Image(product.imageUrl)
)

// Screen 2: Product detail
Hero(
    tag = "product-${product.id}",
    child = Image(product.imageUrl)
)
```

#### Flutter Equivalent

```dart
// Screen 1
Hero(
  tag: 'product-${product.id}',
  child: Image.network(product.imageUrl),
)

// Screen 2
Hero(
  tag: 'product-${product.id}',
  child: Image.network(product.imageUrl),
)
```

#### Performance Notes

- Shared element transitions use **MaterialSharedAxis** on Android
- Smooth **60 FPS** transitions
- Automatically handles different aspect ratios
- Works with navigation library out of the box

---

### FadeTransition

Animates opacity using an AnimationController.

**Flutter Equivalent:** `FadeTransition`

#### API Reference

```kotlin
data class FadeTransition(
    val opacity: Animation<Float>,            // Opacity animation
    val child: Any                            // Child widget
)
```

---

### SlideTransition

Animates position using a fractional offset.

**Flutter Equivalent:** `SlideTransition`

#### API Reference

```kotlin
data class SlideTransition(
    val position: Animation<Offset>,          // Position animation
    val transformHitTests: Boolean = true,
    val child: Any                            // Child widget
)
```

#### Usage Example

```kotlin
val offsetAnimation = Tween(
    begin = Offset(0f, 1f),  // Off screen bottom
    end = Offset.Zero        // Final position
).animate(CurvedAnimation(
    parent = animationController,
    curve = Curves.EaseOut
))

SlideTransition(
    position = offsetAnimation,
    child = Card(content = "Sliding in from bottom")
)
```

---

### ScaleTransition

Animates the scale of a widget.

**Flutter Equivalent:** `ScaleTransition`

#### API Reference

```kotlin
data class ScaleTransition(
    val scale: Animation<Float>,              // Scale animation
    val alignment: AlignmentGeometry = Alignment.Center,
    val child: Any                            // Child widget
)
```

---

### RotationTransition

Animates rotation.

**Flutter Equivalent:** `RotationTransition`

#### API Reference

```kotlin
data class RotationTransition(
    val turns: Animation<Float>,              // Rotation animation (0.0 to 1.0 = full rotation)
    val alignment: AlignmentGeometry = Alignment.Center,
    val child: Any                            // Child widget
)
```

---

### Additional Transitions

The following transitions are also available:

- **PositionedTransition** - Animates Positioned properties
- **SizeTransition** - Animates size along an axis
- **AnimatedCrossFade** - Cross-fades between two children
- **AnimatedSwitcher** - Animates switching between children
- **AnimatedList** - List with animated insertions/removals
- **AnimatedModalBarrier** - Animated modal barrier
- **DecoratedBoxTransition** - Animates box decoration
- **AlignTransition** - Animates alignment
- **DefaultTextStyleTransition** - Animates text style
- **RelativePositionedTransition** - Animates relative position

---

## 30.4 Flex & Positioning Layouts

Flexible and responsive layout components.

### Wrap

Displays children in multiple horizontal or vertical runs.

**Flutter Equivalent:** `Wrap`

#### API Reference

```kotlin
data class WrapComponent(
    val direction: WrapDirection = WrapDirection.Horizontal,
    val alignment: WrapAlignment = WrapAlignment.Start,
    val spacing: Spacing = Spacing.Zero,       // Spacing between children in run
    val runSpacing: Spacing = Spacing.Zero,    // Spacing between runs
    val runAlignment: WrapAlignment = WrapAlignment.Start,
    val crossAxisAlignment: WrapCrossAlignment = WrapCrossAlignment.Start,
    val verticalDirection: VerticalDirection = VerticalDirection.Down,
    val children: List<Any> = emptyList()
)
```

#### Usage Example

```kotlin
Wrap(
    direction = WrapDirection.Horizontal,
    spacing = Spacing.all(8f),
    runSpacing = Spacing.all(4f),
    children = listOf(
        Chip("Flutter"),
        Chip("Kotlin"),
        Chip("Android"),
        Chip("iOS"),
        Chip("Web")
    )
)
```

#### Flutter Equivalent

```dart
Wrap(
  direction: Axis.horizontal,
  spacing: 8.0,
  runSpacing: 4.0,
  children: [
    Chip(label: Text('Flutter')),
    Chip(label: Text('Kotlin')),
    Chip(label: Text('Android')),
    Chip(label: Text('iOS')),
    Chip(label: Text('Web')),
  ],
)
```

#### Common Use Cases

1. **Tag Lists**
2. **Filter Chips**
3. **Responsive Button Groups**
4. **Dynamic Content Layout**

---

### Expanded

Expands a child to fill available space in a Flex container.

**Flutter Equivalent:** `Expanded`

#### API Reference

```kotlin
data class Expanded(
    val flex: Int = 1,                        // Flex factor
    val child: Any                            // Child widget
)
```

#### Usage Example

```kotlin
Row {
    Expanded(
        flex = 2,
        child = Button("Take 2/3")
    )
    Expanded(
        flex = 1,
        child = Button("Take 1/3")
    )
}
```

---

### Flexible

A flexible child in a Flex container.

**Flutter Equivalent:** `Flexible`

#### API Reference

```kotlin
data class Flexible(
    val flex: Int = 1,                        // Flex factor
    val fit: FlexFit = FlexFit.Loose,        // Tight or Loose
    val child: Any                            // Child widget
)
```

---

### Padding

Adds padding around a widget.

**Flutter Equivalent:** `Padding`

#### API Reference

```kotlin
data class Padding(
    val padding: Spacing,                     // Padding amount
    val child: Any                            // Child widget
)
```

#### Usage Example

```kotlin
Padding(
    padding = Spacing.all(16f),
    child = Text("Padded content")
)

// Asymmetric padding
Padding(
    padding = Spacing(
        left = 16f,
        top = 8f,
        right = 16f,
        bottom = 24f
    ),
    child = Card(content = "Custom padding")
)
```

---

### Align

Aligns a child within itself.

**Flutter Equivalent:** `Align`

#### API Reference

```kotlin
data class Align(
    val alignment: AlignmentGeometry = Alignment.Center,
    val widthFactor: Float? = null,
    val heightFactor: Float? = null,
    val child: Any? = null
)
```

#### Usage Example

```kotlin
Align(
    alignment = Alignment.BottomRight,
    child = FloatingActionButton(icon = "add")
)
```

---

### Center

Centers a child within itself.

**Flutter Equivalent:** `Center`

#### API Reference

```kotlin
data class Center(
    val widthFactor: Float? = null,
    val heightFactor: Float? = null,
    val child: Any? = null
)
```

---

### SizedBox

A box with a specified size.

**Flutter Equivalent:** `SizedBox`

#### API Reference

```kotlin
data class SizedBox(
    val width: Size? = null,
    val height: Size? = null,
    val child: Any? = null
)
```

#### Usage Example

```kotlin
// Fixed size container
SizedBox(
    width = Size.dp(100f),
    height = Size.dp(100f),
    child = Image("avatar.png")
)

// Spacer
SizedBox(height = Size.dp(16f))
```

---

### ConstrainedBox

Applies constraints to its child.

**Flutter Equivalent:** `ConstrainedBox`

#### API Reference

```kotlin
data class ConstrainedBox(
    val constraints: BoxConstraints,          // Size constraints
    val child: Any
)

data class BoxConstraints(
    val minWidth: Size = Size.dp(0f),
    val maxWidth: Size = Size.Infinity,
    val minHeight: Size = Size.dp(0f),
    val maxHeight: Size = Size.Infinity
)
```

---

### FittedBox

Scales and positions its child within itself.

**Flutter Equivalent:** `FittedBox`

#### API Reference

```kotlin
data class FittedBox(
    val fit: BoxFit = BoxFit.Contain,        // How to fit child
    val alignment: AlignmentGeometry = Alignment.Center,
    val child: Any
)
```

---

## 30.5 Advanced Scrolling

Efficient scrollable components for lists, grids, and pages.

### ListView.builder

Creates a scrollable, lazily-built list.

**Flutter Equivalent:** `ListView.builder`

#### API Reference

```kotlin
data class ListViewBuilder(
    val itemCount: Int,                       // Number of items
    val itemBuilder: (Int) -> Any,           // Builder function
    val scrollDirection: Axis = Axis.Vertical,
    val reverse: Boolean = false,
    val shrinkWrap: Boolean = false,
    val physics: ScrollPhysics? = null,
    val padding: Spacing? = null
)
```

#### Usage Example

```kotlin
ListViewBuilder(
    itemCount = 1000,
    itemBuilder = { index ->
        ListTile(
            title = "Item $index",
            leading = Avatar(url = "avatar_$index.png"),
            onTap = { handleTap(index) }
        )
    }
)
```

#### Flutter Equivalent

```dart
ListView.builder(
  itemCount: 1000,
  itemBuilder: (context, index) {
    return ListTile(
      title: Text('Item $index'),
      leading: CircleAvatar(
        backgroundImage: NetworkImage('avatar_$index.png'),
      ),
      onTap: () => handleTap(index),
    );
  },
)
```

#### Performance Optimization

- **Lazy loading**: Only builds visible items
- **Recycling**: Reuses item widgets when scrolling
- **Efficient for 10,000+ items**
- Memory usage: O(viewport items) not O(total items)

---

### ListView.separated

List with separators between items.

**Flutter Equivalent:** `ListView.separated`

#### API Reference

```kotlin
data class ListViewSeparated(
    val itemCount: Int,
    val itemBuilder: (Int) -> Any,
    val separatorBuilder: (Int) -> Any,       // Builds separators
    val scrollDirection: Axis = Axis.Vertical,
    val physics: ScrollPhysics? = null,
    val padding: Spacing? = null
)
```

#### Usage Example

```kotlin
ListViewSeparated(
    itemCount = 50,
    itemBuilder = { index ->
        ListTile(title = "Item $index")
    },
    separatorBuilder = { index ->
        Divider(height = Size.dp(1f))
    }
)
```

---

### GridView.builder

Lazily-built grid layout.

**Flutter Equivalent:** `GridView.builder`

#### API Reference

```kotlin
data class GridViewBuilder(
    val itemCount: Int,
    val itemBuilder: (Int) -> Any,
    val gridDelegate: SliverGridDelegate,     // Grid configuration
    val scrollDirection: Axis = Axis.Vertical,
    val physics: ScrollPhysics? = null,
    val padding: Spacing? = null
)

// Fixed column count
data class SliverGridDelegateWithFixedCrossAxisCount(
    val crossAxisCount: Int,                  // Number of columns
    val mainAxisSpacing: Float = 0f,
    val crossAxisSpacing: Float = 0f,
    val childAspectRatio: Float = 1f
)
```

#### Usage Example

```kotlin
GridViewBuilder(
    itemCount = 100,
    gridDelegate = SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount = 3,
        mainAxisSpacing = 8f,
        crossAxisSpacing = 8f
    ),
    itemBuilder = { index ->
        Card(
            child = Image("photo_$index.jpg")
        )
    }
)
```

---

### PageView

Scrollable pages with snap behavior.

**Flutter Equivalent:** `PageView`

#### API Reference

```kotlin
data class PageView(
    val children: List<Any>,                  // Page widgets
    val scrollDirection: Axis = Axis.Horizontal,
    val pageSnapping: Boolean = true,
    val physics: ScrollPhysics? = null,
    val onPageChanged: ((Int) -> Unit)? = null
)
```

#### Usage Example

```kotlin
PageView(
    children = listOf(
        OnboardingPage1(),
        OnboardingPage2(),
        OnboardingPage3()
    ),
    onPageChanged = { page ->
        println("Switched to page $page")
    }
)
```

---

### ReorderableListView

List with drag-to-reorder support.

**Flutter Equivalent:** `ReorderableListView`

#### API Reference

```kotlin
data class ReorderableListView(
    val children: List<Any>,
    val onReorder: (Int, Int) -> Unit,       // Callback when reordered
    val padding: Spacing? = null
)
```

---

### CustomScrollView

Advanced scrolling with slivers.

**Flutter Equivalent:** `CustomScrollView`

#### API Reference

```kotlin
data class CustomScrollView(
    val slivers: List<Any>,                   // Sliver widgets
    val scrollDirection: Axis = Axis.Vertical,
    val physics: ScrollPhysics? = null
)
```

---

## 30.6 Material Chips & Lists

Material Design selection and list components.

### FilterChip

Selectable chip for filtering content.

**Flutter Equivalent:** `FilterChip`

#### API Reference

```kotlin
data class FilterChip(
    val label: String,                        // Chip label
    val selected: Boolean = false,            // Selection state
    val enabled: Boolean = true,              // Enabled state
    val showCheckmark: Boolean = true,        // Show checkmark when selected
    val avatar: String? = null,               // Leading avatar
    val contentDescription: String? = null,   // Accessibility
    val onSelected: ((Boolean) -> Unit)? = null,
    val style: ComponentStyle? = null
)
```

#### Usage Example

```kotlin
var categories by remember { mutableStateOf(setOf<String>()) }

Row {
    FilterChip(
        label = "Electronics",
        selected = "electronics" in categories,
        onSelected = { selected ->
            categories = if (selected) {
                categories + "electronics"
            } else {
                categories - "electronics"
            }
        }
    )

    FilterChip(
        label = "Books",
        selected = "books" in categories,
        onSelected = { selected ->
            categories = if (selected) {
                categories + "books"
            } else {
                categories - "books"
            }
        }
    )
}
```

#### Flutter Equivalent

```dart
Set<String> categories = {};

Row(
  children: [
    FilterChip(
      label: Text('Electronics'),
      selected: categories.contains('electronics'),
      onSelected: (selected) {
        setState(() {
          if (selected) {
            categories.add('electronics');
          } else {
            categories.remove('electronics');
          }
        });
      },
    ),
    FilterChip(
      label: Text('Books'),
      selected: categories.contains('books'),
      onSelected: (selected) {
        setState(() {
          if (selected) {
            categories.add('books');
          } else {
            categories.remove('books');
          }
        });
      },
    ),
  ],
)
```

---

### ActionChip

Chip that performs an action when tapped.

**Flutter Equivalent:** `ActionChip`

#### API Reference

```kotlin
data class ActionChip(
    val label: String,
    val enabled: Boolean = true,
    val avatar: String? = null,
    val onPressed: (() -> Unit)? = null,
    val style: ComponentStyle? = null
)
```

---

### ChoiceChip

Chip for single selection from a set.

**Flutter Equivalent:** `ChoiceChip`

#### API Reference

```kotlin
data class ChoiceChip(
    val label: String,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val avatar: String? = null,
    val onSelected: ((Boolean) -> Unit)? = null,
    val style: ComponentStyle? = null
)
```

---

### InputChip

Chip representing complex data entry.

**Flutter Equivalent:** `InputChip`

#### API Reference

```kotlin
data class InputChip(
    val label: String,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val avatar: String? = null,
    val deleteIcon: String? = null,
    val onPressed: (() -> Unit)? = null,
    val onDeleted: (() -> Unit)? = null,
    val style: ComponentStyle? = null
)
```

---

### ExpansionTile

List tile that expands/collapses to reveal children.

**Flutter Equivalent:** `ExpansionTile`

#### API Reference

```kotlin
data class ExpansionTile(
    val title: String,                        // Tile title
    val subtitle: String? = null,             // Optional subtitle
    val leading: String? = null,              // Leading icon
    val children: List<Any> = emptyList(),    // Expanded content
    val initiallyExpanded: Boolean = false,   // Initial state
    val maintainState: Boolean = false,       // Maintain child state
    val onExpansionChanged: ((Boolean) -> Unit)? = null
)
```

#### Usage Example

```kotlin
ExpansionTile(
    title = "Settings",
    leading = "settings",
    children = listOf(
        ListTile(title = "Notifications", onTap = { /* ... */ }),
        ListTile(title = "Privacy", onTap = { /* ... */ }),
        ListTile(title = "Account", onTap = { /* ... */ })
    )
)
```

---

### CheckboxListTile

List tile with a checkbox.

**Flutter Equivalent:** `CheckboxListTile`

#### API Reference

```kotlin
data class CheckboxListTile(
    val title: String,
    val value: Boolean,
    val onChanged: ((Boolean) -> Unit)? = null,
    val subtitle: String? = null,
    val secondary: String? = null,
    val enabled: Boolean = true
)
```

---

### SwitchListTile

List tile with a switch.

**Flutter Equivalent:** `SwitchListTile`

#### API Reference

```kotlin
data class SwitchListTile(
    val title: String,
    val value: Boolean,
    val onChanged: ((Boolean) -> Unit)? = null,
    val subtitle: String? = null,
    val secondary: String? = null,
    val enabled: Boolean = true
)
```

---

### FilledButton

Material 3 filled button.

**Flutter Equivalent:** `FilledButton`

#### API Reference

```kotlin
data class FilledButton(
    val text: String,
    val enabled: Boolean = true,
    val icon: String? = null,
    val onPressed: (() -> Unit)? = null,
    val style: ComponentStyle? = null
)
```

---

## 30.7 Advanced Material

Advanced Material Design components.

### PopupMenuButton

Button that displays a popup menu.

**Flutter Equivalent:** `PopupMenuButton`

#### API Reference

```kotlin
data class PopupMenuButton(
    val items: List<PopupMenuItem>,
    val onSelected: ((String) -> Unit)? = null,
    val icon: String? = null,
    val tooltip: String? = null,
    val enabled: Boolean = true
)

data class PopupMenuItem(
    val value: String,
    val label: String,
    val enabled: Boolean = true,
    val icon: String? = null
)
```

#### Usage Example

```kotlin
PopupMenuButton(
    icon = "more_vert",
    items = listOf(
        PopupMenuItem(value = "edit", label = "Edit", icon = "edit"),
        PopupMenuItem(value = "delete", label = "Delete", icon = "delete"),
        PopupMenuItem(value = "share", label = "Share", icon = "share")
    ),
    onSelected = { value ->
        when (value) {
            "edit" -> editItem()
            "delete" -> deleteItem()
            "share" -> shareItem()
        }
    }
)
```

---

### RefreshIndicator

Pull-to-refresh functionality.

**Flutter Equivalent:** `RefreshIndicator`

#### API Reference

```kotlin
data class RefreshIndicator(
    val child: Any,                           // Scrollable child
    val onRefresh: suspend () -> Unit,       // Refresh callback
    val color: Color? = null,
    val backgroundColor: Color? = null
)
```

#### Usage Example

```kotlin
RefreshIndicator(
    onRefresh = {
        // Suspend function that fetches data
        viewModel.refreshData()
    },
    child = ListView.builder(
        itemCount = items.size,
        itemBuilder = { index -> ItemCard(items[index]) }
    )
)
```

---

### Additional Components

- **IndexedStack** - Shows one child at a time by index
- **VerticalDivider** - Vertical divider line
- **FadeInImage** - Image with fade-in loading
- **CircleAvatar** - Circular avatar image
- **RichText** - Styled text with multiple spans
- **SelectableText** - Selectable and copyable text
- **EndDrawer** - Right-side navigation drawer

---

## 30.8 Migration Guide

### Flutter to AVAMagic Mapping

#### Property Name Differences

| Flutter | AVAMagic | Notes |
|---------|----------|-------|
| `child:` | `child =` | Named parameter syntax |
| `children:` | `children =` | Named parameter syntax |
| `onTap:` | `onTap =` | Callback syntax |
| `200.0` | `Size.dp(200f)` | Type-safe dimensions |
| `Colors.blue` | `Colors.Blue` | Capitalized color names |
| `Curves.easeInOut` | `Curves.EaseInOut` | Capitalized curve names |

#### Type Conversions

| Flutter Type | AVAMagic Type | Example |
|--------------|---------------|---------|
| `double` | `Float` | `1.5f` instead of `1.5` |
| `Duration` | `Duration` | `Duration.milliseconds(300)` |
| `EdgeInsets` | `Spacing` | `Spacing.all(16f)` |
| `BoxDecoration` | `BoxDecoration` | Same structure |
| `Color` | `Color` | `Color(0xFF6750A4)` |

#### Common Patterns

**Flutter:**
```dart
Container(
  width: 200.0,
  height: 100.0,
  padding: EdgeInsets.all(16.0),
  decoration: BoxDecoration(
    color: Colors.blue,
    borderRadius: BorderRadius.circular(8.0),
  ),
  child: Text('Hello'),
)
```

**AVAMagic:**
```kotlin
Container(
    width = Size.dp(200f),
    height = Size.dp(100f),
    padding = Spacing.all(16f),
    decoration = BoxDecoration(
        color = Colors.Blue,
        borderRadius = BorderRadius.circular(Size.dp(8f))
    ),
    child = Text("Hello")
)
```

---

## 30.9 Performance Considerations

### Animation Performance

- **60 FPS target** on all devices
- **Hardware acceleration** for transforms
- **Composition-based** animations (no recomposition overhead)
- **Parallel animations** for multiple properties

### Optimization Tips

1. **Use Lazy Lists for Large Datasets**
   ```kotlin
   // Good: Lazy builder
   ListViewBuilder(itemCount = 10000, itemBuilder = { ... })

   // Bad: All items created at once
   Column(children = List(10000) { ... })
   ```

2. **Avoid Expensive Operations in Builders**
   ```kotlin
   // Good: Compute once
   val formattedDate = remember(timestamp) {
       formatDate(timestamp)
   }

   // Bad: Computed on every build
   Text(formatDate(timestamp))
   ```

3. **Use Keys for List Items**
   ```kotlin
   ListViewBuilder(
       itemCount = items.size,
       itemBuilder = { index ->
           key(items[index].id) {
               ItemCard(items[index])
           }
       }
   )
   ```

### Memory Management

- **Automatic recycling** in lazy lists
- **Image caching** with LRU policy
- **Animation cleanup** on widget disposal
- **State preservation** for reorderable lists

---

## 30.10 API Reference

### Quick Lookup Table

#### Animations

| Component | Key Properties | Common Use |
|-----------|---------------|------------|
| AnimatedContainer | duration, width, height, color | Expanding cards |
| AnimatedOpacity | opacity, duration | Fade in/out |
| AnimatedPositioned | left, top, duration | Moving elements |
| Hero | tag, child | Shared transitions |

#### Layouts

| Component | Key Properties | Common Use |
|-----------|---------------|------------|
| Wrap | spacing, runSpacing, children | Tag clouds |
| Expanded | flex, child | Responsive rows |
| Padding | padding, child | Spacing |
| SizedBox | width, height | Fixed sizes |

#### Scrolling

| Component | Key Properties | Common Use |
|-----------|---------------|------------|
| ListView.builder | itemCount, itemBuilder | Long lists |
| GridView.builder | itemCount, gridDelegate | Photo grids |
| PageView | children, onPageChanged | Onboarding |

#### Material

| Component | Key Properties | Common Use |
|-----------|---------------|------------|
| FilterChip | label, selected, onSelected | Filtering |
| ExpansionTile | title, children | Collapsible menus |
| PopupMenuButton | items, onSelected | Context menus |

### Voice DSL Syntax

All Flutter Parity components support Voice DSL syntax:

```
AnimatedContainer {
  duration: 300ms
  width: selected ? 200 : 100
  color: primary

  Text "Tap me"
}

Hero tag="product-1" {
  Image url="photo.jpg"
}

Wrap spacing=8 runSpacing=4 {
  Chip "Tag 1"
  Chip "Tag 2"
  Chip "Tag 3"
}
```

---

## Summary

### What You've Learned

- **58 new Flutter Parity components** for Android
- **100% component parity** with Flutter
- **Migration strategies** from Flutter to AVAMagic
- **Performance optimization** techniques
- **Voice DSL syntax** for all components

### Next Steps

1. Explore the [Migration Guide](#308-migration-guide) for your use case
2. Review [Code Examples](#302-implicit-animations) for each component
3. Check [Performance Considerations](#309-performance-considerations)
4. Try the [Quick Start Guide](/docs/FLUTTER-PARITY-QUICK-START.md)

### Resources

- **Component Source Code:** `/Universal/Libraries/AvaElements/components/flutter-parity/`
- **Android Renderers:** `/Universal/Libraries/AvaElements/Renderers/Android/.../flutterparity/`
- **Tests:** `/Universal/Libraries/AvaElements/components/flutter-parity/src/commonTest/`
- **Complete Registry:** `/docs/COMPLETE-COMPONENT-REGISTRY-LIVING.md`

---

**Chapter Status:** ✅ COMPLETE
**Last Updated:** 2025-11-22
**Maintained By:** Manoj Jhawar (manoj@ideahq.net)
