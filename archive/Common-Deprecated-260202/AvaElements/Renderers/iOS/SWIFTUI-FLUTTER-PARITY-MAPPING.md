# SwiftUI ↔ Flutter Parity - Complete Mapping Reference

**Quick Reference Guide for iOS Layout Components**

---

## Layout Components (10 Total)

### 1. Wrap Component
**Flutter**:
```dart
Wrap(
  direction: Axis.horizontal,
  spacing: 8.0,
  runSpacing: 4.0,
  alignment: WrapAlignment.start,
  children: [Widget1(), Widget2(), Widget3()],
)
```

**SwiftUI** (iOS 16+):
```swift
WrapLayout(
  direction: .horizontal,
  spacing: 8,
  runSpacing: 4,
  alignment: .leading
) {
  View1()
  View2()
  View3()
}
```

**AVAMagic Kotlin**:
```kotlin
WrapComponent(
  direction = WrapDirection.Horizontal,
  spacing = Spacing.of(8f),
  runSpacing = Spacing.of(4f),
  alignment = WrapAlignment.Start,
  children = listOf(...)
)
```

---

### 2. Expanded Component
**Flutter**:
```dart
Row(
  children: [
    Expanded(
      flex: 2,
      child: Container(color: Colors.red),
    ),
    Expanded(
      flex: 1,
      child: Container(color: Colors.blue),
    ),
  ],
)
```

**SwiftUI**:
```swift
HStack {
  Color.red
    .frame(maxWidth: .infinity)
    .layoutPriority(2)

  Color.blue
    .frame(maxWidth: .infinity)
    .layoutPriority(1)
}
```

**AVAMagic Kotlin**:
```kotlin
FlexComponent(
  direction = FlexDirection.Horizontal,
  children = listOf(
    ExpandedComponent(flex = 2, child = Container(color = Colors.Red)),
    ExpandedComponent(flex = 1, child = Container(color = Colors.Blue))
  )
)
```

---

### 3. Flexible Component
**Flutter**:
```dart
Column(
  children: [
    Flexible(
      flex: 1,
      fit: FlexFit.loose,
      child: Text("Flexible"),
    ),
  ],
)
```

**SwiftUI**:
```swift
VStack {
  Text("Flexible")
    .frame(maxHeight: .infinity)
    .layoutPriority(1)
}
```

**AVAMagic Kotlin**:
```kotlin
FlexComponent(
  direction = FlexDirection.Vertical,
  children = listOf(
    FlexibleComponent(
      flex = 1,
      fit = FlexFit.Loose,
      child = TextComponent("Flexible")
    )
  )
)
```

---

### 4. Row / Column (Flex)
**Flutter Row**:
```dart
Row(
  mainAxisAlignment: MainAxisAlignment.spaceBetween,
  crossAxisAlignment: CrossAxisAlignment.center,
  children: [Text("A"), Text("B"), Text("C")],
)
```

**SwiftUI HStack**:
```swift
HStack(spacing: 0) {
  Spacer()
  Text("A")
  Spacer()
  Text("B")
  Spacer()
  Text("C")
  Spacer()
}
.frame(maxWidth: .infinity)
```

**AVAMagic Kotlin**:
```kotlin
FlexComponent(
  direction = FlexDirection.Horizontal,
  mainAxisAlignment = MainAxisAlignment.SpaceBetween,
  crossAxisAlignment = CrossAxisAlignment.Center,
  children = listOf(Text("A"), Text("B"), Text("C"))
)
```

**Flutter Column**:
```dart
Column(
  mainAxisAlignment: MainAxisAlignment.start,
  crossAxisAlignment: CrossAxisAlignment.stretch,
  children: [Widget1(), Widget2()],
)
```

**SwiftUI VStack**:
```swift
VStack(alignment: .leading, spacing: 0) {
  View1()
    .frame(maxWidth: .infinity)
  View2()
    .frame(maxWidth: .infinity)
}
```

---

### 5. Padding Component
**Flutter**:
```dart
Padding(
  padding: EdgeInsets.only(
    top: 8,
    left: 16,
    bottom: 8,
    right: 16,
  ),
  child: Text("Padded"),
)
```

**SwiftUI**:
```swift
Text("Padded")
  .padding(.top, 8)
  .padding(.leading, 16)
  .padding(.bottom, 8)
  .padding(.trailing, 16)
```

**AVAMagic Kotlin**:
```kotlin
PaddingComponent(
  padding = Spacing.of(top = 8f, left = 16f, bottom = 8f, right = 16f),
  child = TextComponent("Padded")
)
```

---

### 6. Align Component
**Flutter**:
```dart
Align(
  alignment: Alignment.bottomRight,
  child: Text("Aligned"),
)
```

**SwiftUI**:
```swift
Text("Aligned")
  .frame(
    maxWidth: .infinity,
    maxHeight: .infinity,
    alignment: .bottomTrailing
  )
```

**AVAMagic Kotlin**:
```kotlin
AlignComponent(
  alignment = AlignmentGeometry.BottomEnd,
  child = TextComponent("Aligned")
)
```

---

### 7. Center Component
**Flutter**:
```dart
Center(
  child: Text("Centered"),
)
```

**SwiftUI**:
```swift
Text("Centered")
  .frame(
    maxWidth: .infinity,
    maxHeight: .infinity,
    alignment: .center
  )
```

**AVAMagic Kotlin**:
```kotlin
CenterComponent(
  child = TextComponent("Centered")
)
```

---

### 8. SizedBox Component
**Flutter (with child)**:
```dart
SizedBox(
  width: 200,
  height: 100,
  child: Container(color: Colors.blue),
)
```

**SwiftUI**:
```swift
Color.blue
  .frame(width: 200, height: 100)
```

**AVAMagic Kotlin**:
```kotlin
SizedBoxComponent(
  width = Size.dp(200f),
  height = Size.dp(100f),
  child = Container(color = Colors.Blue)
)
```

**Flutter (spacer)**:
```dart
Column(
  children: [
    Text("Top"),
    SizedBox(height: 20),
    Text("Bottom"),
  ],
)
```

**SwiftUI**:
```swift
VStack {
  Text("Top")
  Spacer()
    .frame(height: 20)
  Text("Bottom")
}
```

---

### 9. ConstrainedBox Component
**Flutter**:
```dart
ConstrainedBox(
  constraints: BoxConstraints(
    minWidth: 100,
    maxWidth: 300,
    minHeight: 50,
    maxHeight: 150,
  ),
  child: Container(),
)
```

**SwiftUI**:
```swift
Rectangle()
  .frame(
    minWidth: 100,
    maxWidth: 300,
    minHeight: 50,
    maxHeight: 150
  )
```

**AVAMagic Kotlin**:
```kotlin
ConstrainedBoxComponent(
  constraints = BoxConstraints(
    minWidth = 100f,
    maxWidth = 300f,
    minHeight = 50f,
    maxHeight = 150f
  ),
  child = Container()
)
```

---

### 10. FittedBox Component
**Flutter (contain)**:
```dart
FittedBox(
  fit: BoxFit.contain,
  child: Image.asset('logo.png'),
)
```

**SwiftUI**:
```swift
Image("logo")
  .resizable()
  .scaledToFit()
```

**AVAMagic Kotlin**:
```kotlin
FittedBoxComponent(
  fit = BoxFit.Contain,
  child = ImageComponent("logo.png")
)
```

**Flutter (cover)**:
```dart
FittedBox(
  fit: BoxFit.cover,
  alignment: Alignment.center,
  child: Image.asset('background.png'),
)
```

**SwiftUI**:
```swift
Image("background")
  .resizable()
  .scaledToFill()
  .clipped()
```

---

## Alignment Mapping

| Flutter | SwiftUI | AVAMagic |
|---------|---------|----------|
| `Alignment.topLeft` | `.topLeading` | `AlignmentGeometry.TopLeft` |
| `Alignment.topCenter` | `.top` | `AlignmentGeometry.TopCenter` |
| `Alignment.topRight` | `.topTrailing` | `AlignmentGeometry.TopEnd` |
| `Alignment.centerLeft` | `.leading` | `AlignmentGeometry.CenterLeft` |
| `Alignment.center` | `.center` | `AlignmentGeometry.Center` |
| `Alignment.centerRight` | `.trailing` | `AlignmentGeometry.CenterEnd` |
| `Alignment.bottomLeft` | `.bottomLeading` | `AlignmentGeometry.BottomLeft` |
| `Alignment.bottomCenter` | `.bottom` | `AlignmentGeometry.BottomCenter` |
| `Alignment.bottomRight` | `.bottomTrailing` | `AlignmentGeometry.BottomEnd` |

---

## MainAxisAlignment Mapping

| Flutter | SwiftUI (HStack) | SwiftUI (VStack) |
|---------|------------------|------------------|
| `MainAxisAlignment.start` | `.leading` | `.top` |
| `MainAxisAlignment.end` | `.trailing` | `.bottom` |
| `MainAxisAlignment.center` | `.center` | `.center` |
| `MainAxisAlignment.spaceBetween` | Spacer distribution | Spacer distribution |
| `MainAxisAlignment.spaceAround` | Spacer distribution | Spacer distribution |
| `MainAxisAlignment.spaceEvenly` | Spacer distribution | Spacer distribution |

---

## CrossAxisAlignment Mapping

| Flutter | SwiftUI (HStack) | SwiftUI (VStack) |
|---------|------------------|------------------|
| `CrossAxisAlignment.start` | `.top` | `.leading` |
| `CrossAxisAlignment.end` | `.bottom` | `.trailing` |
| `CrossAxisAlignment.center` | `.center` | `.center` |
| `CrossAxisAlignment.stretch` | `.frame(maxHeight: .infinity)` | `.frame(maxWidth: .infinity)` |
| `CrossAxisAlignment.baseline` | `.firstTextBaseline` | N/A |

---

## BoxFit Mapping

| Flutter | SwiftUI | Description |
|---------|---------|-------------|
| `BoxFit.fill` | `.frame(max: .infinity)` | Distort to fill |
| `BoxFit.contain` | `.scaledToFit()` | Scale to fit, maintain aspect |
| `BoxFit.cover` | `.scaledToFill()` + `.clipped()` | Fill frame, crop excess |
| `BoxFit.fitWidth` | `.frame(maxWidth: .infinity)` | Match width |
| `BoxFit.fitHeight` | `.frame(maxHeight: .infinity)` | Match height |
| `BoxFit.none` | No modifier | Original size |
| `BoxFit.scaleDown` | `.scaledToFit()` | Like contain, but never upscale |

---

## RTL Support

All components automatically support RTL (Right-to-Left) languages:

| Direction | Flutter | SwiftUI | AVAMagic |
|-----------|---------|---------|----------|
| **Set RTL** | `Directionality(textDirection: TextDirection.rtl, child: ...)` | `.environment(\.layoutDirection, .rightToLeft)` | `FlexComponent(textDirection = TextDirection.RTL, ...)` |
| **Leading** | Auto-mirrors | Auto-mirrors | Auto-mirrors |
| **Trailing** | Auto-mirrors | Auto-mirrors | Auto-mirrors |
| **Start** | Respects direction | Respects direction | Respects direction |
| **End** | Respects direction | Respects direction | Respects direction |

---

## Platform-Specific Differences

### Safe Area
- **Flutter**: Manual `SafeArea` widget
- **SwiftUI**: Automatic via `.safeAreaInset()`
- **AVAMagic iOS**: Automatic (SwiftUI default)

### Dynamic Type
- **Flutter**: Manual font scaling
- **SwiftUI**: Automatic via `@ScaledMetric` and `.font(.body)`
- **AVAMagic iOS**: Automatic (respects system text size)

### Dark Mode
- **Flutter**: Manual theme switching
- **SwiftUI**: Automatic via `@Environment(\.colorScheme)`
- **AVAMagic iOS**: Automatic (respects system appearance)

---

## Performance Considerations

### Layout Calculation
- **Flutter**: Custom layout engine (RenderObject tree)
- **SwiftUI**: Declarative layout system with automatic diffing
- **AVAMagic iOS**: SwiftUI's layout system via bridge

### Optimization Tips
1. Use `SizedBox` for fixed-size spacers (faster than `Spacer()`)
2. Prefer `HStack`/`VStack` over nested `Align` components
3. Use `.layoutPriority()` sparingly (expensive)
4. Avoid deeply nested `Wrap` components (O(n²) layout)

---

## Migration Guide

### From Flutter to AVAMagic iOS

1. Replace `Row` → `FlexComponent(direction = Horizontal)`
2. Replace `Column` → `FlexComponent(direction = Vertical)`
3. Replace `Expanded` → `ExpandedComponent`
4. Replace `Flexible` → `FlexibleComponent`
5. Replace `Padding` → `PaddingComponent`
6. Replace `Align` → `AlignComponent`
7. Replace `Center` → `CenterComponent`
8. Replace `SizedBox` → `SizedBoxComponent`
9. Replace `ConstrainedBox` → `ConstrainedBoxComponent`
10. Replace `FittedBox` → `FittedBoxComponent`

### From SwiftUI to AVAMagic

1. Replace `HStack` → `FlexComponent(direction = Horizontal)`
2. Replace `VStack` → `FlexComponent(direction = Vertical)`
3. Replace `.frame(maxWidth: .infinity)` → `ExpandedComponent`
4. Replace `.padding()` → `PaddingComponent`
5. Replace `.frame(alignment:)` → `AlignComponent`

---

**Last Updated**: 2025-11-22
**Maintained By**: Agent 2 (iOS Layout Components)
