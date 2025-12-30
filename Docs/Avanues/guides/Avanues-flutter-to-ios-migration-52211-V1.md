# Flutter to AVAMagic iOS Migration Guide
**Side-by-Side Comparison and Migration Strategies**

**Version:** 3.0.0
**Last Updated:** 2025-11-22
**Target Audience:** Flutter developers migrating to AVAMagic iOS

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Component Mapping Table](#2-component-mapping-table)
3. [Code Migration Examples](#3-code-migration-examples)
4. [Platform-Specific Differences](#4-platform-specific-differences)
5. [Migration Strategies](#5-migration-strategies)

---

## 1. Introduction

This guide helps Flutter developers migrate their apps to AVAMagic iOS. AVAMagic provides **100% Flutter component parity**, meaning every Flutter widget has an equivalent AVAMagic component that renders natively as SwiftUI.

**Key Differences:**
- **Flutter:** Uses custom rendering engine (Skia)
- **AVAMagic iOS:** Uses native SwiftUI rendering
- **Result:** AVAMagic apps feel more native on iOS

---

## 2. Component Mapping Table

### Implicit Animations (8)

| Flutter Widget | AVAMagic iOS | SwiftUI Output | API Compatibility |
|---------------|--------------|----------------|-------------------|
| `AnimatedContainer` | `AMAnimatedContainer` | `withAnimation { }` + State | 100% |
| `AnimatedOpacity` | `AMAnimatedOpacity` | `.opacity()` modifier | 100% |
| `AnimatedPositioned` | `AMAnimatedPositioned` | `.offset()` modifier | 100% |
| `AnimatedDefaultTextStyle` | `AMAnimatedTextStyle` | `.font()` modifier | 100% |
| `AnimatedPadding` | `AMAnimatedPadding` | `.padding()` modifier | 100% |
| `AnimatedSize` | `AMAnimatedSize` | `.frame()` modifier | 100% |
| `AnimatedAlign` | `AMAnimatedAlign` | `.frame(alignment:)` | 100% |
| `AnimatedScale` | `AMAnimatedScale` | `.scaleEffect()` | 100% |

### Transitions (15)

| Flutter Widget | AVAMagic iOS | SwiftUI Output | Notes |
|---------------|--------------|----------------|-------|
| `FadeTransition` | `AMFadeTransition` | `.opacity()` + animation | Identical visual |
| `SlideTransition` | `AMSlideTransition` | `.offset()` + animation | Identical visual |
| `Hero` | `AMHero` | `matchedGeometryEffect` | Slightly different curve |
| `ScaleTransition` | `AMScaleTransition` | `.scaleEffect()` | Identical visual |
| `RotationTransition` | `AMRotationTransition` | `.rotationEffect()` | Identical visual |

### Layouts (10)

| Flutter Widget | AVAMagic iOS | SwiftUI Output | Differences |
|---------------|--------------|----------------|-------------|
| `Wrap` | `AMWrap` | `LazyVGrid` | Identical behavior |
| `Expanded` | `AMExpanded` | `Spacer()` + `.frame(maxWidth/Height)` | Identical flex behavior |
| `Flexible` | `AMFlexible` | `.frame(idealWidth)` + priority | Identical flex behavior |
| `Padding` | `AMPadding` | `.padding()` | Identical API |
| `Align` | `AMAlign` | `.frame(alignment:)` | Identical API |
| `Center` | `AMCenter` | `.frame(alignment: .center)` | Shorthand for Align |

### Scrolling (7)

| Flutter Widget | AVAMagic iOS | SwiftUI Output | Performance |
|---------------|--------------|----------------|-------------|
| `ListView.builder` | `AMListViewBuilder` | `LazyVStack` | Identical lazy loading |
| `ListView.separated` | `AMListViewSeparated` | `LazyVStack` + Divider | Identical |
| `GridView.builder` | `AMGridViewBuilder` | `LazyVGrid` | Identical |
| `PageView` | `AMPageView` | `TabView(.page)` | Identical |
| `ReorderableListView` | `AMReorderableListView` | `List` + `.onMove` | Identical |
| `CustomScrollView` | `AMCustomScrollView` | `ScrollView` + sections | Similar |
| `Slivers` | `AMSlivers` | Lazy containers | Similar |

### Material Chips (8)

| Flutter Widget | AVAMagic iOS | SwiftUI Output | Visual Differences |
|---------------|--------------|----------------|-------------------|
| `ActionChip` | `AMActionChip` | Custom `Button` | Uses SF Symbols for icons |
| `FilterChip` | `AMFilterChip` | `Toggle` styled as chip | SF Symbol checkmark |
| `ChoiceChip` | `AMChoiceChip` | Radio button styled as chip | SF Symbol selection |
| `InputChip` | `AMInputChip` | Custom view with delete | SF Symbol delete icon |
| `CheckboxListTile` | `AMCheckboxListTile` | `List` + Toggle | iOS-style checkbox |
| `SwitchListTile` | `AMSwitchListTile` | `List` + Toggle | iOS-style switch |
| `ExpansionTile` | `AMExpansionTile` | `DisclosureGroup` | iOS-style disclosure |
| `FilledButton` | `AMFilledButton` | `Button(.borderedProminent)` | iOS button style |

---

## 3. Code Migration Examples

### Example 1: AnimatedContainer

**Flutter (Dart):**
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

**AVAMagic iOS (Kotlin):**
```kotlin
AnimatedContainer(
    duration = Duration.milliseconds(300),
    width = if (selected) Size.dp(200f) else Size.dp(100f),
    height = if (selected) Size.dp(200f) else Size.dp(100f),
    color = if (selected) Color(0xFF2196F3) else Color(0xFFF44336),
    curve = Curves.EaseInOut,
    child = Text("Tap Me"),
    onEnd = { println("Animation completed") }
)
```

**Rendered SwiftUI:**
```swift
ZStack {
    (selected ? Color.blue : Color.red)
    Text("Tap Me")
}
.frame(width: selected ? 200 : 100, height: selected ? 200 : 100)
.animation(.easeInOut(duration: 0.3), value: selected)
```

**Key Differences:**
- Kotlin uses `Size.dp()` instead of `Double`
- Colors use hex values: `Color(0xFF2196F3)`
- Syntax: `if/else` instead of ternary `? :`

---

### Example 2: ListView.builder

**Flutter (Dart):**
```dart
ListView.builder(
  itemCount: 1000,
  itemBuilder: (context, index) {
    return ListTile(
      leading: CircleAvatar(
        child: Text('${index + 1}'),
      ),
      title: Text('Item $index'),
      subtitle: Text('Subtitle $index'),
    );
  },
)
```

**AVAMagic iOS (Kotlin):**
```kotlin
ListViewBuilder(
    itemCount = 1000,
    itemBuilder = { context, index ->
        ListTile(
            leading = CircleAvatar(
                child = Text("${index + 1}")
            ),
            title = Text("Item $index"),
            subtitle = Text("Subtitle $index")
        )
    }
)
```

**Rendered SwiftUI:**
```swift
List {
    LazyVStack {
        ForEach(0..<1000, id: \.self) { index in
            HStack {
                Circle()
                    .frame(width: 40, height: 40)
                    .overlay(Text("\(index + 1)"))

                VStack(alignment: .leading) {
                    Text("Item \(index)")
                        .font(.headline)
                    Text("Subtitle \(index)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
        }
    }
}
```

**Key Differences:**
- Kotlin uses `lambda expressions: { context, index -> }`
- String interpolation: `"$variable"` or `"${expression}"`

---

### Example 3: FilterChip

**Flutter (Dart):**
```dart
FilterChip(
  label: Text('Swift'),
  selected: isSelected,
  onSelected: (bool selected) {
    setState(() {
      isSelected = selected;
    });
  },
  selectedColor: Colors.blue,
  checkmarkColor: Colors.white,
)
```

**AVAMagic iOS (Kotlin):**
```kotlin
FilterChip(
    label = "Swift",
    selected = isSelected,
    onSelected = { selected ->
        isSelected = selected
    },
    selectedColor = Color(0xFF2196F3),
    checkmarkColor = Color.White
)
```

**Rendered SwiftUI:**
```swift
Button(action: {
    isSelected.toggle()
}) {
    HStack {
        if isSelected {
            Image(systemName: "checkmark")
                .foregroundColor(.white)
        }
        Text("Swift")
            .font(.subheadline.weight(.medium))
    }
    .padding(.horizontal, 16)
    .padding(.vertical, 8)
    .background(Capsule().fill(isSelected ? Color.blue : Color.gray))
    .foregroundColor(isSelected ? .white : .primary)
}
```

**Key Differences:**
- iOS uses SF Symbol `checkmark` instead of custom icon
- Button-based implementation (not Toggle)

---

### Example 4: Hero Transitions

**Flutter (Dart):**
```dart
// Source screen
Hero(
  tag: 'imageHero',
  child: Image.network(
    'https://example.com/image.jpg',
    width: 100,
    height: 100,
  ),
)

// Destination screen
Hero(
  tag: 'imageHero', // Same tag
  child: Image.network(
    'https://example.com/image.jpg',
    width: 300,
    height: 300,
  ),
)
```

**AVAMagic iOS (Kotlin):**
```kotlin
// Source screen
Hero(
    tag = "imageHero",
    child = Image(
        url = "https://example.com/image.jpg",
        width = Size.dp(100f),
        height = Size.dp(100f)
    )
)

// Destination screen
Hero(
    tag = "imageHero", // Same tag
    child = Image(
        url = "https://example.com/image.jpg",
        width = Size.dp(300f),
        height = Size.dp(300f)
    )
)
```

**Rendered SwiftUI:**
```swift
// Source view
@Namespace private var heroNamespace

Image(url: URL(string: "https://example.com/image.jpg"))
    .frame(width: 100, height: 100)
    .matchedGeometryEffect(id: "imageHero", in: heroNamespace)

// Destination view
Image(url: URL(string: "https://example.com/image.jpg"))
    .frame(width: 300, height: 300)
    .matchedGeometryEffect(id: "imageHero", in: heroNamespace)
```

**Key Differences:**
- SwiftUI uses `@Namespace` and `matchedGeometryEffect`
- Animation curve is slightly different (SwiftUI uses spring by default)

---

## 4. Platform-Specific Differences

### Icons: Material Icons vs SF Symbols

**Flutter (Material Icons):**
```dart
Icon(Icons.delete)
Icon(Icons.home)
Icon(Icons.settings)
```

**AVAMagic iOS (SF Symbols):**
```kotlin
Icon("trash.fill", source = IconSource.SFSymbol)
Icon("house.fill", source = IconSource.SFSymbol)
Icon("gearshape.fill", source = IconSource.SFSymbol)
```

**Mapping:**
| Material Icon | SF Symbol |
|--------------|-----------|
| `Icons.delete` | `trash.fill` |
| `Icons.home` | `house.fill` |
| `Icons.settings` | `gearshape.fill` |
| `Icons.favorite` | `heart.fill` |
| `Icons.search` | `magnifyingglass` |
| `Icons.add` | `plus` |
| `Icons.close` | `xmark` |

### Corner Radius: Standard vs Continuous

**Flutter:**
```dart
BorderRadius.circular(12.0)
```

**AVAMagic iOS:**
```kotlin
BorderRadius.continuous(12f) // iOS continuous curve
// or
BorderRadius.circular(12f)   // Standard circular
```

**SwiftUI:**
```swift
RoundedRectangle(cornerRadius: 12, style: .continuous) // Recommended
// or
RoundedRectangle(cornerRadius: 12, style: .circular)
```

**Visual Difference:**
- **Continuous:** Smoother, more elegant curve (iOS style)
- **Circular:** Standard circular arc (Flutter style)

### Touch Feedback: Ripple vs Scale

**Flutter (Ripple Effect):**
```dart
InkWell(
  onTap: () {},
  child: Container(...),
)
// Creates ripple animation on tap
```

**AVAMagic iOS (Scale Effect):**
```kotlin
Container(...) {
    onClick = { }
    pressEffect = PressEffect.Scale(0.95f)
}
```

**SwiftUI:**
```swift
Button(action: {}) {
    // Content
}
.scaleEffect(isPressed ? 0.95 : 1.0)
.animation(.spring(), value: isPressed)
// Scales down slightly when pressed
```

### Fonts: Roboto vs SF Pro

**Flutter:**
```dart
TextStyle(
  fontFamily: 'Roboto',
  fontSize: 16,
  fontWeight: FontWeight.w500,
)
```

**AVAMagic iOS:**
```kotlin
TextStyle(
    fontFamily = "SF Pro Text", // iOS default
    fontSize = 17f,             // iOS default body size
    fontWeight = FontWeight.Medium
)
```

**SwiftUI:**
```swift
Text("Hello")
    .font(.system(size: 17, weight: .medium, design: .default))
// Uses SF Pro automatically
```

---

## 5. Migration Strategies

### Strategy 1: Incremental Migration (Recommended)

Migrate one screen at a time:

1. **Identify** screens with Flutter Parity components
2. **Convert** Dart to Kotlin (keep same structure)
3. **Test** on iOS simulator
4. **Iterate** until pixel-perfect
5. **Move** to next screen

**Timeline:** 2-4 weeks for typical app

---

### Strategy 2: Full Rewrite

For small apps (<10 screens):

1. **Audit** all Flutter components used
2. **Map** to AVAMagic iOS equivalents (use table above)
3. **Rewrite** in Kotlin from scratch
4. **Test** thoroughly

**Timeline:** 1-2 weeks for small app

---

### Strategy 3: Hybrid Approach

Keep Flutter for complex screens, use AVAMagic for simple ones:

1. **Classify** screens by complexity
2. **Migrate** simple screens first (lists, forms)
3. **Keep** complex screens in Flutter temporarily
4. **Gradually** migrate complex screens

**Timeline:** Ongoing (months)

---

**END OF GUIDE**

**Version:** 3.0.0
**Last Updated:** 2025-11-22
**Maintained by:** Manoj Jhawar (manoj@ideahq.net)
