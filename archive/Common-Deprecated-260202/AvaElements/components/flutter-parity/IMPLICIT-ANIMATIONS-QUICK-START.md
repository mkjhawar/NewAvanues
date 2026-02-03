# Implicit Animations - Quick Start Guide

**Version:** 3.0.0-flutter-parity
**Components:** 8 Implicit Animation Components
**Performance:** 60 FPS minimum

---

## What Are Implicit Animations?

Implicit animations automatically animate property changes over time. You just specify the new value and duration - the framework handles the animation automatically.

**Key Principle:** Change a property → Animation happens automatically ✨

---

## All 8 Components at a Glance

| Component | Purpose | Best For | Performance |
|-----------|---------|----------|-------------|
| **AnimatedContainer** | Multi-property animations | Size, color, padding changes | 60 FPS |
| **AnimatedOpacity** | Fade effects | Show/hide, loading states | 60 FPS ⭐ |
| **AnimatedPositioned** | Stack positioning | Overlays, tooltips | 60 FPS |
| **AnimatedDefaultTextStyle** | Text styling | Typography transitions | 58-60 FPS |
| **AnimatedPadding** | Spacing changes | Focus states, breathing UI | 60 FPS |
| **AnimatedSize** | Auto-sizing | Expandable sections | 58-60 FPS |
| **AnimatedAlign** | Alignment changes | Floating elements | 60 FPS ⭐ |
| **AnimatedScale** | Scale effects | Button feedback, zoom | 60 FPS ⭐ |

⭐ = Best performance (GPU-accelerated)

---

## 1. AnimatedContainer

### When to Use
- Animate multiple properties at once
- Size, color, padding, decoration changes
- Complex container transformations

### Basic Example
```kotlin
var selected by remember { mutableStateOf(false) }

AnimatedContainer(
    duration = Duration.milliseconds(300),
    width = if (selected) Size.dp(200f) else Size.dp(100f),
    height = if (selected) Size.dp(200f) else Size.dp(100f),
    color = if (selected) Color.Blue else Color.Red,
    curve = Curve.EaseInOut,
    child = Text("Tap Me")
)
```

### Flutter Equivalent
```dart
AnimatedContainer(
  duration: Duration(milliseconds: 300),
  width: selected ? 200.0 : 100.0,
  height: selected ? 200.0 : 100.0,
  color: selected ? Colors.blue : Colors.red,
  curve: Curves.easeInOut,
  child: Text('Tap Me'),
)
```

### Common Properties
```kotlin
AnimatedContainer(
    duration = Duration.milliseconds(300),

    // Size
    width = Size.dp(100f),
    height = Size.dp(100f),

    // Spacing
    padding = Spacing.all(16f),
    margin = Spacing.all(8f),

    // Visual
    color = Color.Blue,
    decoration = BoxDecoration(
        borderRadius = BorderRadius.circular(8f),
        border = Border.all(BorderSide(Color.Black, 2f))
    ),

    // Layout
    alignment = AlignmentGeometry.Center,

    child = Text("Content")
)
```

---

## 2. AnimatedOpacity

### When to Use
- Fade in/out effects
- Loading state overlays
- Disabled state visuals
- Best performance (GPU-accelerated)

### Basic Example
```kotlin
var visible by remember { mutableStateOf(true) }

AnimatedOpacity(
    opacity = if (visible) 1.0f else 0.0f,
    duration = Duration.milliseconds(500),
    curve = Curve.EaseInOut,
    child = Container(
        width = Size.dp(200f),
        height = Size.dp(200f),
        color = Color.Blue
    )
)
```

### Flutter Equivalent
```dart
AnimatedOpacity(
  opacity: visible ? 1.0 : 0.0,
  duration: Duration(milliseconds: 500),
  curve: Curves.easeInOut,
  child: Container(
    width: 200.0,
    height: 200.0,
    color: Colors.blue,
  ),
)
```

### Tips
- Opacity 0.0 still takes up space (use AnimatedVisibility for layout changes)
- Fastest animation component - use whenever possible
- Perfect for loading states

---

## 3. AnimatedPositioned

### When to Use
- Position elements within Stack
- Animated overlays
- Drag and drop with snap-back
- Floating action button transitions

### Basic Example
```kotlin
var expanded by remember { mutableStateOf(false) }

Stack {
    AnimatedPositioned(
        duration = Duration.milliseconds(500),
        left = if (expanded) Size.dp(100f) else Size.dp(10f),
        top = if (expanded) Size.dp(100f) else Size.dp(10f),
        width = Size.dp(50f),
        height = Size.dp(50f),
        curve = Curve.FastOutSlowIn,
        child = Container(color = Color.Red)
    )
}
```

### Flutter Equivalent
```dart
Stack(
  children: [
    AnimatedPositioned(
      duration: Duration(milliseconds: 500),
      left: expanded ? 100.0 : 10.0,
      top: expanded ? 100.0 : 10.0,
      width: 50.0,
      height: 50.0,
      curve: Curves.fastOutSlowIn,
      child: Container(color: Colors.red),
    ),
  ],
)
```

### Helper Factories
```kotlin
// Fill parent
AnimatedPositioned.fill(
    duration = Duration.milliseconds(300),
    child = Container(...)
)

// From rect
AnimatedPositioned.fromRect(
    duration = Duration.milliseconds(300),
    rect = Rect.fromLTWH(10f, 20f, 100f, 50f),
    child = Container(...)
)
```

---

## 4. AnimatedDefaultTextStyle

### When to Use
- Text size transitions
- Font weight changes
- Color fades
- Reading mode

### Basic Example
```kotlin
var large by remember { mutableStateOf(false) }

AnimatedDefaultTextStyle(
    style = TextStyle(
        fontSize = if (large) 32f else 16f,
        fontWeight = if (large) FontWeight.Bold else FontWeight.Normal,
        color = if (large) Color.Blue else Color.Black
    ),
    duration = Duration.milliseconds(300),
    curve = Curve.EaseInOut,
    child = Text("Animated Text")
)
```

### Flutter Equivalent
```dart
AnimatedDefaultTextStyle(
  style: TextStyle(
    fontSize: large ? 32.0 : 16.0,
    fontWeight: large ? FontWeight.bold : FontWeight.normal,
    color: large ? Colors.blue : Colors.black,
  ),
  duration: Duration(milliseconds: 300),
  curve: Curves.easeInOut,
  child: Text('Animated Text'),
)
```

### Available Text Properties
```kotlin
TextStyle(
    color = Color.Black,
    fontSize = 18f,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic,
    letterSpacing = 1.5f,
    wordSpacing = 2.0f,
    lineHeight = 1.2f,
    decoration = TextDecoration.Underline,
    decorationColor = Color.Blue,
    fontFamily = "Roboto"
)
```

---

## 5. AnimatedPadding

### When to Use
- Spacing transitions
- Focus state emphasis
- Container breathing effects
- Responsive layouts

### Basic Example
```kotlin
var expanded by remember { mutableStateOf(false) }

AnimatedPadding(
    padding = if (expanded) Spacing.all(32f) else Spacing.all(8f),
    duration = Duration.milliseconds(300),
    curve = Curve.EaseInOut,
    child = Container(
        color = Color.Blue,
        child = Text("Padded Content")
    )
)
```

### Flutter Equivalent
```dart
AnimatedPadding(
  padding: expanded ? EdgeInsets.all(32.0) : EdgeInsets.all(8.0),
  duration: Duration(milliseconds: 300),
  curve: Curves.easeInOut,
  child: Container(
    color: Colors.blue,
    child: Text('Padded Content'),
  ),
)
```

### Asymmetric Padding
```kotlin
AnimatedPadding(
    padding = Spacing.of(
        top = if (selected) 40f else 10f,
        right = 20f,
        bottom = if (selected) 40f else 10f,
        left = 20f
    ),
    duration = Duration.milliseconds(250),
    child = Text("Asymmetric")
)
```

---

## 6. AnimatedSize

### When to Use
- Expandable content sections
- Dynamic form fields
- Collapsible panels
- Auto-sizing containers

### Basic Example
```kotlin
var showDetails by remember { mutableStateOf(false) }

AnimatedSize(
    duration = Duration.milliseconds(300),
    curve = Curve.FastOutSlowIn,
    alignment = AlignmentGeometry.TopCenter,
    child = Column {
        Text("Title")
        if (showDetails) {
            Text("Detail line 1")
            Text("Detail line 2")
        }
    }
)
```

### Flutter Equivalent
```dart
AnimatedSize(
  duration: Duration(milliseconds: 300),
  curve: Curves.fastOutSlowIn,
  alignment: Alignment.topCenter,
  child: Column(
    children: [
      Text('Title'),
      if (showDetails) ...[
        Text('Detail line 1'),
        Text('Detail line 2'),
      ],
    ],
  ),
)
```

### Key Difference
- **AnimatedSize:** Size determined by child (auto)
- **AnimatedContainer:** Size explicitly specified

---

## 7. AnimatedAlign

### When to Use
- Alignment transitions
- Floating element positioning
- Toggle switch animations
- Focus indicators

### Basic Example
```kotlin
var alignRight by remember { mutableStateOf(false) }

AnimatedAlign(
    alignment = if (alignRight) AlignmentGeometry.CenterRight
                else AlignmentGeometry.CenterLeft,
    duration = Duration.milliseconds(400),
    curve = Curve.EaseInOut,
    child = Container(
        width = Size.dp(50f),
        height = Size.dp(50f),
        color = Color.Blue
    )
)
```

### Flutter Equivalent
```dart
AnimatedAlign(
  alignment: alignRight ? Alignment.centerRight : Alignment.centerLeft,
  duration: Duration(milliseconds: 400),
  curve: Curves.easeInOut,
  child: Container(
    width: 50.0,
    height: 50.0,
    color: Colors.blue,
  ),
)
```

### All Alignments
```kotlin
AlignmentGeometry.TopLeft      // (-1, -1)
AlignmentGeometry.TopCenter    // (0, -1)
AlignmentGeometry.TopRight     // (1, -1)
AlignmentGeometry.CenterLeft   // (-1, 0)
AlignmentGeometry.Center       // (0, 0)
AlignmentGeometry.CenterRight  // (1, 0)
AlignmentGeometry.BottomLeft   // (-1, 1)
AlignmentGeometry.BottomCenter // (0, 1)
AlignmentGeometry.BottomRight  // (1, 1)

// Custom
AlignmentGeometry.Custom(0.5f, -0.5f)
```

---

## 8. AnimatedScale

### When to Use
- Button press feedback
- Zoom transitions
- Attention-grabbing effects
- Loading pulse animations

### Basic Example
```kotlin
var zoomed by remember { mutableStateOf(false) }

AnimatedScale(
    scale = if (zoomed) 1.5f else 1.0f,
    duration = Duration.milliseconds(300),
    curve = Curve.EaseInOut,
    alignment = AlignmentGeometry.Center,
    child = Container(
        width = Size.dp(100f),
        height = Size.dp(100f),
        color = Color.Blue
    )
)
```

### Flutter Equivalent
```dart
AnimatedScale(
  scale: zoomed ? 1.5 : 1.0,
  duration: Duration(milliseconds: 300),
  curve: Curves.easeInOut,
  alignment: Alignment.center,
  child: Container(
    width: 100.0,
    height: 100.0,
    color: Colors.blue,
  ),
)
```

### Scale Values
```kotlin
scale = 1.0f  // 100% - original size
scale = 0.5f  // 50% - half size
scale = 2.0f  // 200% - double size
scale = 0.0f  // 0% - collapsed to point
```

### Filter Quality (for images)
```kotlin
AnimatedScale(
    scale = 2.0f,
    filterQuality = FilterQuality.High, // Best quality
    // FilterQuality.None   - Fastest, lowest quality
    // FilterQuality.Low    - Default, good balance
    // FilterQuality.Medium - Better quality
    // FilterQuality.High   - Best quality, slowest
    child = Image("avatar.png")
)
```

---

## Common Patterns

### Duration
```kotlin
Duration.milliseconds(300)  // 300ms - typical
Duration.milliseconds(200)  // 200ms - fast
Duration.milliseconds(500)  // 500ms - slow
Duration.seconds(1)         // 1 second
```

### Curves
```kotlin
Curve.Linear         // Constant speed
Curve.EaseIn         // Start slow, end fast
Curve.EaseOut        // Start fast, end slow
Curve.EaseInOut      // Start slow, speed up, end slow (most natural)
Curve.FastOutSlowIn  // Material Design standard
```

### Callbacks
```kotlin
AnimatedOpacity(
    opacity = targetOpacity,
    duration = Duration.milliseconds(300),
    child = Content(),
    onEnd = {
        println("Animation completed!")
        // Trigger next action
    }
)
```

---

## Performance Best Practices

### 1. Choose the Right Component
```kotlin
// ✅ GOOD - Use GPU-accelerated components
AnimatedOpacity(opacity = 0.5f, ...)  // GPU
AnimatedScale(scale = 1.5f, ...)      // GPU
AnimatedAlign(alignment = ...)        // GPU

// ⚠️ OKAY - Layout-based components
AnimatedSize(...)                      // Layout recalc
AnimatedDefaultTextStyle(...)          // Text remeasurement
```

### 2. Limit Concurrent Animations
```kotlin
// ✅ GOOD - 5-10 concurrent animations
repeat(5) { AnimatedOpacity(...) }

// ❌ BAD - 50+ concurrent animations
repeat(50) { AnimatedSize(...) }  // Will drop frames
```

### 3. Keep Durations Reasonable
```kotlin
// ✅ GOOD - 200-500ms
Duration.milliseconds(300)

// ❌ BAD - Too slow
Duration.seconds(5)  // User waits too long
```

### 4. Profile on Target Devices
- Test on low-end devices
- Use Android Studio Profiler
- Monitor frame times (<16ms for 60 FPS)

---

## Troubleshooting

### Animation Not Running?
```kotlin
// ❌ BAD - State doesn't change
val opacity = 0.5f  // Constant
AnimatedOpacity(opacity = opacity, ...)

// ✅ GOOD - State changes trigger animation
var opacity by remember { mutableStateOf(0.5f) }
AnimatedOpacity(opacity = opacity, ...)
```

### Animation Too Fast/Slow?
```kotlin
// Adjust duration
duration = Duration.milliseconds(500)  // Slower
duration = Duration.milliseconds(100)  // Faster
```

### Animation Not Smooth?
```kotlin
// Try different curve
curve = Curve.FastOutSlowIn  // Material Design standard
curve = Curve.EaseInOut      // Smooth acceleration/deceleration
```

---

## Migration from Flutter

### Direct Mapping
Most Flutter implicit animations map 1:1:

| Flutter | AvaElements |
|---------|-------------|
| `AnimatedContainer` | `AnimatedContainer` ✅ |
| `AnimatedOpacity` | `AnimatedOpacity` ✅ |
| `AnimatedPositioned` | `AnimatedPositioned` ✅ |
| `AnimatedDefaultTextStyle` | `AnimatedDefaultTextStyle` ✅ |
| `AnimatedPadding` | `AnimatedPadding` ✅ |
| `AnimatedSize` | `AnimatedSize` ✅ |
| `AnimatedAlign` | `AnimatedAlign` ✅ |
| `AnimatedScale` | `AnimatedScale` ✅ |

### Syntax Differences
```dart
// Flutter
Duration(milliseconds: 300)
EdgeInsets.all(16.0)
Alignment.center

// AvaElements (Kotlin)
Duration.milliseconds(300)
Spacing.all(16f)
AlignmentGeometry.Center
```

---

## Next Steps

1. **Read:** [ANIMATION-PERFORMANCE-REPORT.md](./ANIMATION-PERFORMANCE-REPORT.md)
2. **Explore:** Unit tests in `src/commonTest/kotlin/animation/`
3. **Study:** Android mappers in `FlutterParityAnimationMappers.kt`
4. **Build:** Try the examples above in your project

---

**Need Help?**
- Check the comprehensive KDoc in each component file
- Review test cases for usage examples
- See performance report for optimization tips

**Happy Animating! ✨**
