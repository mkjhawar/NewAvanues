# iOS Flutter Parity Implementation Plan

**Agent:** Week 3 - iOS Renderer Specialist
**Date:** 2025-11-22
**Status:** In Progress
**Target:** Port 58 Flutter Parity components to iOS using SwiftUI

---

## Executive Summary

This document outlines the complete implementation plan for porting all 58 Flutter Parity components from Android (Jetpack Compose) to iOS (SwiftUI). The implementation follows the same architectural pattern as the Android renderer but adapts to SwiftUI's declarative UI paradigm.

### Component Breakdown

| Category | Count | Android Mapper | iOS Mapper (Target) |
|----------|-------|----------------|---------------------|
| **Animations** | 8 | FlutterParityAnimationMappers.kt | FlutterParityAnimationMappers.kt |
| **Layouts** | 10 | FlutterParityLayoutMappers.kt | FlutterParityLayoutMappers.kt |
| **Scrolling** | 10 | FlutterParityScrollingMappers.kt | FlutterParityScrollingMappers.kt |
| **Material** | 20 | FlutterParityMaterialMappers.kt | FlutterParityMaterialMappers.kt |
| **Transitions** | 10 | FlutterParityTransitionMappers.kt | FlutterParityTransitionMappers.kt |
| **TOTAL** | **58** | 5 files (~2,800 LOC) | 5 files (~2,500 LOC target) |

---

## 1. Animation Components (8 components)

### Components
1. **AnimatedContainer** - Animate size, color, padding, margin, decoration
2. **AnimatedOpacity** - Fade in/out animations
3. **AnimatedPositioned** - Animate position within parent
4. **AnimatedDefaultTextStyle** - Animate text style properties
5. **AnimatedPadding** - Animate padding values
6. **AnimatedSize** - Automatically animate to child's size
7. **AnimatedAlign** - Animate alignment within parent
8. **AnimatedScale** - Scale transformations

### Android → iOS Mapping

| Android (Compose) | iOS (SwiftUI) | Notes |
|-------------------|---------------|-------|
| `animateDpAsState` | `@State` + `.animation()` | SwiftUI uses state-driven animations |
| `animateColorAsState` | `Color` + `.animation()` | Built-in color animation |
| `animateFloatAsState` | `Double` + `.animation()` | Use Double for precision |
| `graphicsLayer { alpha }` | `.opacity()` | Direct modifier |
| `Box` with positioning | `GeometryReader` + `offset()` | More complex in SwiftUI |
| `animateContentSize()` | `.frame()` with animation | Automatic sizing |
| `BiasAlignment` | `Alignment` | Similar concept |
| `graphicsLayer { scale }` | `.scaleEffect()` | Direct modifier |

### SwiftUI Implementation Strategy

```swift
// Example: AnimatedOpacity equivalent
struct AnimatedOpacityView: View {
    let opacity: Double
    let duration: Double
    let curve: Animation

    var body: some View {
        content
            .opacity(opacity)
            .animation(curve, value: opacity)
    }
}
```

### Performance Targets
- **60 FPS** on iPhone 12+
- **Metal acceleration** for transform operations
- **No layout thrashing** - use `.drawingGroup()` for complex animations

---

## 2. Layout Components (10 components)

### Components
1. **Wrap** - Wrap children with automatic flow
2. **Expanded** - Fill available space proportionally
3. **Flexible** - Flexible space allocation
4. **Flex** - Generic flex layout (Row/Column)
5. **Padding** - Edge insets
6. **Align** - 2D alignment
7. **Center** - Center alignment shorthand
8. **SizedBox** - Fixed or constrained size
9. **ConstrainedBox** - Min/max size constraints
10. **FittedBox** - Scale and position child

### Android → iOS Mapping

| Android (Compose) | iOS (SwiftUI) | Notes |
|-------------------|---------------|-------|
| `FlowRow` / `FlowColumn` | Custom `FlowLayout` | Requires custom layout |
| `Modifier.weight()` | `.frame(maxWidth: .infinity)` | Different approach |
| `Row` / `Column` | `HStack` / `VStack` | Direct equivalents |
| `Modifier.padding()` | `.padding()` | Direct equivalent |
| `Box` + `contentAlignment` | `ZStack` + `alignment` | Direct equivalent |
| `Modifier.size()` | `.frame()` | Direct equivalent |
| `Modifier.widthIn/heightIn` | `.frame(minWidth:maxWidth:)` | Direct equivalent |
| `Box` + ContentScale | `.scaledToFit()` / `.scaledToFill()` | Direct equivalent |

### SwiftUI Implementation Strategy

```swift
// Example: Wrap equivalent (requires custom layout)
struct FlowLayout: Layout {
    var spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        // Calculate wrapped size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        // Place children with wrapping
    }
}
```

### Performance Targets
- **Minimal recomposition** - Use `@State` judiciously
- **Lazy evaluation** - Use `LazyVStack`/`LazyHStack` where appropriate
- **RTL support** - Automatic with `.environment(\.layoutDirection, .rightToLeft)`

---

## 3. Scrolling Components (10 components)

### Components
1. **ListViewBuilder** - Lazy vertical/horizontal list
2. **GridViewBuilder** - Lazy grid
3. **ListViewSeparated** - List with separators
4. **PageView** - Swipeable pages
5. **ReorderableListView** - Drag-to-reorder list
6. **CustomScrollView** - Custom scrolling with slivers
7. **SliverList** - Sliver list component
8. **SliverGrid** - Sliver grid component
9. **SliverFixedExtentList** - Fixed-height sliver list
10. **SliverAppBar** - Collapsible app bar

### Android → iOS Mapping

| Android (Compose) | iOS (SwiftUI) | Notes |
|-------------------|---------------|-------|
| `LazyColumn` / `LazyRow` | `List` / `ScrollView` + `LazyVStack` | Similar lazy loading |
| `LazyVerticalGrid` | `LazyVGrid` | Direct equivalent |
| Separator between items | `Divider()` between items | SwiftUI has no built-in separator list |
| `HorizontalPager` / `VerticalPager` | `TabView` with `.tabViewStyle(.page)` | Different API |
| `ReorderableLazyList` | `List` with `.onMove()` | Built-in support |
| Mixed sliver scrolling | `ScrollView` + mixed content | Less flexible than Compose |
| `LazyColumn` with headers | `List` with sections | Similar concept |
| Sticky headers | `pinnedViews: [.sectionHeaders]` | Built-in support |

### SwiftUI Implementation Strategy

```swift
// Example: ListViewBuilder equivalent
struct ListViewBuilderView: View {
    let itemCount: Int?
    let itemBuilder: (Int) -> AnyView

    var body: some View {
        List(0..<(itemCount ?? Int.max), id: \.self) { index in
            itemBuilder(index)
        }
    }
}
```

### Performance Targets
- **60 FPS scrolling** with 10,000+ items
- **Memory usage <100 MB** for large lists
- **Lazy loading** - Only render visible items + buffer
- **Prefetching** - Use `.task(priority:)` for data loading

---

## 4. Material Components (20 components)

### Components
1. **FilterChip** - Selectable chip with checkmark
2. **ActionChip** - Action-triggering chip
3. **ChoiceChip** - Single-selection chip
4. **InputChip** - Editable chip with delete
5. **ExpansionTile** - Expandable list item
6. **CheckboxListTile** - List item with checkbox
7. **SwitchListTile** - List item with switch
8. **FilledButton** - Filled button variant
9. **PopupMenuButton** - Context menu button
10. **RefreshIndicator** - Pull-to-refresh
11. **IndexedStack** - Show one child at a time
12. **VerticalDivider** - Vertical separator
13. **FadeInImage** - Image with fade-in loading
14. **CircleAvatar** - Circular avatar image
15. **RichText** - Styled text with spans
16. **SelectableText** - Text with selection
17. **EndDrawer** - Trailing drawer
18. **Hero** - Shared element transition
19. **AnimatedList** - Animated list insertions/removals
20. **AnimatedModalBarrier** - Animated modal backdrop

### Android → iOS Mapping

| Android (Compose) | iOS (SwiftUI) | Notes |
|-------------------|---------------|-------|
| `FilterChip` / `AssistChip` | Custom chip view | No built-in chips in SwiftUI |
| `ListItem` with controls | Custom list row | More manual in SwiftUI |
| `Button` variants | `Button` with `.buttonStyle()` | Different styling approach |
| `DropdownMenu` | `.contextMenu()` or `Menu` | Different API |
| `SwipeRefresh` | `.refreshable()` | Built-in iOS 15+ |
| `Box` with index | Custom `IndexedView` | Manual implementation |
| `Divider` | `Divider()` | Direct equivalent |
| `AsyncImage` with fade | `AsyncImage` with `.transition()` | Built-in iOS 15+ |
| `Image` + `clip(Circle)` | `Image().clipShape(Circle())` | Direct equivalent |
| `AnnotatedString` | `Text` with `AttributedString` | Similar concept |
| `SelectionContainer` | `TextSelection()` | Built-in iOS 15+ |
| `ModalDrawer` | `.sheet()` or custom | Different paradigm |
| Hero transitions | `.matchedGeometryEffect()` | Built-in iOS 14+ |

### SwiftUI Implementation Strategy

```swift
// Example: FilterChip equivalent
struct FilterChipView: View {
    let label: String
    let selected: Bool
    let onSelected: (Bool) -> Void

    var body: some View {
        Button(action: { onSelected(!selected) }) {
            HStack {
                if selected {
                    Image(systemName: "checkmark")
                }
                Text(label)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(selected ? Color.accentColor : Color.gray.opacity(0.2))
            .foregroundColor(selected ? .white : .primary)
            .cornerRadius(16)
        }
    }
}
```

### Performance Targets
- **Native iOS look** - Use system components where possible
- **Material Design fallback** - Custom implementations for Material-specific components
- **Accessibility** - Full VoiceOver support

---

## 5. Transition Components (10 components)

### Components
1. **FadeTransition** - Opacity-based transition
2. **SlideTransition** - Position-based transition
3. **ScaleTransition** - Scale-based transition
4. **RotationTransition** - Rotation-based transition
5. **PositionedTransition** - Positioned transition
6. **SizeTransition** - Size-based transition
7. **AnimatedCrossFade** - Cross-fade between widgets
8. **AnimatedSwitcher** - Transition when child changes
9. **DecoratedBoxTransition** - Decoration transition
10. **AlignTransition** - Alignment transition

### Android → iOS Mapping

| Android (Compose) | iOS (SwiftUI) | Notes |
|-------------------|---------------|-------|
| `graphicsLayer { alpha }` | `.opacity()` + `.transition()` | Direct equivalent |
| `graphicsLayer { translationX/Y }` | `.offset()` + `.transition()` | Direct equivalent |
| `graphicsLayer { scaleX/Y }` | `.scaleEffect()` + `.transition()` | Direct equivalent |
| `graphicsLayer { rotationZ }` | `.rotationEffect()` + `.transition()` | Direct equivalent |
| Positioned with animation | `.position()` + `.animation()` | More complex |
| Size animation | `.frame()` + `.animation()` | Built-in |
| `AnimatedVisibility` crossfade | `.transition(.opacity)` | Built-in |
| Child replacement animation | Custom with `.id()` + `.transition()` | Requires state management |

### SwiftUI Implementation Strategy

```swift
// Example: FadeTransition equivalent
struct FadeTransitionView: View {
    let opacity: Double
    let duration: Double

    var body: some View {
        content
            .opacity(opacity)
            .animation(.easeInOut(duration: duration), value: opacity)
    }
}
```

### Performance Targets
- **60 FPS** for all transitions
- **Metal acceleration** - Use `.drawingGroup()` for complex transitions
- **No layout thrashing** - Batch animations

---

## Implementation Architecture

### File Structure

```
Universal/Libraries/AvaElements/Renderers/iOS/src/iosMain/kotlin/
└── com/augmentalis/avaelements/renderer/ios/
    └── mappers/
        └── flutterparity/
            ├── FlutterParityAnimationMappers.kt
            ├── FlutterParityLayoutMappers.kt
            ├── FlutterParityScrollingMappers.kt
            ├── FlutterParityMaterialMappers.kt
            └── FlutterParityTransitionMappers.kt
```

### Mapper Function Pattern

Each mapper follows this pattern:

```kotlin
/**
 * Render [Component] using SwiftUI
 *
 * Maps [Component] to SwiftUI with:
 * - Feature 1
 * - Feature 2
 * - Performance: 60 FPS target
 * - Accessibility: VoiceOver support
 *
 * @param component Component to render
 * @param content Child content renderer (if applicable)
 */
fun ComponentMapper(
    component: ComponentType,
    content: (() -> SwiftUIView)? = null
): SwiftUIView {
    val modifiers = mutableListOf<SwiftUIModifier>()

    // Build modifiers
    // ...

    return SwiftUIView(
        type = ViewType.Specific,
        id = component.id,
        properties = mapOf(/* ... */),
        modifiers = modifiers,
        children = listOfNotNull(content?.invoke())
    )
}
```

### Extension Functions

Common type conversions:

```kotlin
// Size conversions
private fun Size.toCGFloat(): CGFloat
private fun Spacing.toCGFloat(): CGFloat

// Color conversions
private fun Color.toSwiftUIColor(): SwiftUIColor

// Animation conversions
private fun Duration.toAnimation(curve: Curve): SwiftUIAnimation
private fun Curve.toAnimationCurve(): AnimationCurve

// Alignment conversions
private fun AlignmentGeometry.toSwiftUIAlignment(): SwiftUIAlignment
```

---

## Testing Strategy

### Unit Tests (58+ tests)

Each component gets a test file:

```swift
// Example: AnimatedOpacityTests.swift
import XCTest
@testable import AvaElementsiOS

class AnimatedOpacityTests: XCTestCase {
    func testOpacityAnimation() {
        let component = AnimatedOpacity(
            opacity: 0.5,
            duration: Duration(milliseconds: 300),
            curve: Curve.easeInOut
        )

        let view = AnimatedOpacityMapper(component, content: { /* ... */ })

        XCTAssertEqual(view.type, .box)
        XCTAssertTrue(view.modifiers.contains { $0.isOpacity })
    }

    func testPerformance() {
        measure {
            // Render 100 times
            for _ in 0..<100 {
                let view = AnimatedOpacityMapper(component, content: { /* ... */ })
            }
        }
    }
}
```

### Performance Tests

```swift
class FlutterParityPerformanceTests: XCTestCase {
    func testScrollingPerformance() {
        // Measure FPS during 10K item scroll
        let listView = ListViewBuilder(
            itemCount: 10000,
            itemBuilder: { index in /* ... */ }
        )

        measureMetrics([.wallClockTime, .cpu], automaticallyStartMeasuring: false) {
            startMeasuring()
            // Simulate scroll
            stopMeasuring()
        }
    }

    func testAnimationPerformance() {
        // Ensure 60 FPS for animations
        let container = AnimatedContainer(/* ... */)

        // Measure frame rate
        XCTAssertGreaterThanOrEqual(measuredFPS, 60.0)
    }
}
```

### Accessibility Tests

```swift
class FlutterParityAccessibilityTests: XCTestCase {
    func testVoiceOverSupport() {
        let chip = FilterChip(
            label: "Filter",
            selected: true,
            onSelected: { _ in }
        )

        let view = FilterChipMapper(chip)

        // Verify accessibility properties
        XCTAssertNotNil(view.accessibilityLabel)
        XCTAssertTrue(view.isAccessibilityElement)
    }
}
```

---

## Performance Optimization

### 1. Metal Acceleration

Use `.drawingGroup()` for complex animations:

```kotlin
SwiftUIModifier.drawingGroup(opaque: false, colorMode: .nonLinear)
```

### 2. Lazy Loading

```kotlin
// Use LazyVStack for large lists
ViewType.LazyVStack instead of ViewType.VStack
```

### 3. View Identity

```kotlin
// Use .id() for efficient updates
SwiftUIModifier.id(component.id)
```

### 4. Minimize State

```kotlin
// Only use @State for truly dynamic values
// Prefer passing computed values
```

---

## iOS-Specific Optimizations

### 1. Cupertino Components (Bonus)

Create iOS-native variants:

```kotlin
object CupertinoComponents {
    fun CupertinoButton(/* ... */): SwiftUIView
    fun CupertinoSwitch(/* ... */): SwiftUIView
    fun CupertinoSlider(/* ... */): SwiftUIView
    // ... 20 total Cupertino components
}
```

### 2. iOS 26 Liquid Glass

```kotlin
SwiftUIModifier.background(
    Material.ultraThinMaterial,
    in: RoundedRectangle(cornerRadius: 20, style: .continuous)
)
```

### 3. visionOS Support

```kotlin
SwiftUIModifier.frame(depth: 100)
SwiftUIModifier.glassBackgroundEffect()
```

---

## Deliverables Checklist

- [ ] **5 iOS mapper files** (~2,500 LOC total)
  - [ ] FlutterParityAnimationMappers.kt (8 components, ~400 LOC)
  - [ ] FlutterParityLayoutMappers.kt (10 components, ~600 LOC)
  - [ ] FlutterParityScrollingMappers.kt (10 components, ~700 LOC)
  - [ ] FlutterParityMaterialMappers.kt (20 components, ~800 LOC)
  - [ ] FlutterParityTransitionMappers.kt (10 components, ~400 LOC)

- [ ] **58+ unit tests** (XCTest)
  - [ ] One test per component minimum
  - [ ] Performance tests for scrolling
  - [ ] Accessibility tests for interactive components

- [ ] **Performance validation**
  - [ ] 60 FPS for all animations (iPhone 12+)
  - [ ] <100 MB memory for 10K item lists
  - [ ] Metal acceleration verification

- [ ] **Documentation**
  - [ ] iOS-specific optimization guide
  - [ ] SwiftUI integration examples
  - [ ] Cupertino component guide (bonus)
  - [ ] Performance benchmarks

- [ ] **Bonus: Cupertino components** (if time permits)
  - [ ] 20+ iOS-native component variants
  - [ ] Full iOS design language support

---

## Timeline

**Target:** 6-8 hours

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| **Phase 1** | 2 hours | Animation + Layout mappers (18 components) |
| **Phase 2** | 2 hours | Scrolling mappers (10 components) |
| **Phase 3** | 2 hours | Material mappers (20 components) |
| **Phase 4** | 1 hour | Transition mappers (10 components) |
| **Phase 5** | 1-2 hours | Tests + Documentation |
| **Bonus** | +2 hours | Cupertino components |

---

## Success Criteria

1. ✅ All 58 components implemented
2. ✅ 60 FPS performance on iPhone 12+
3. ✅ <100 MB memory for large lists
4. ✅ Full VoiceOver accessibility
5. ✅ 58+ unit tests passing
6. ✅ Performance benchmarks documented
7. ⭐ Bonus: 20+ Cupertino components

---

## Next Steps

1. ✅ Create directory structure
2. ⏳ Implement FlutterParityAnimationMappers.kt (8 components)
3. ⏳ Implement FlutterParityLayoutMappers.kt (10 components)
4. ⏳ Implement FlutterParityScrollingMappers.kt (10 components)
5. ⏳ Implement FlutterParityMaterialMappers.kt (20 components)
6. ⏳ Implement FlutterParityTransitionMappers.kt (10 components)
7. ⏳ Create comprehensive test suite
8. ⏳ Document iOS-specific optimizations

---

**Status:** Ready to implement
**Priority:** P0 (Critical - iOS is 33% of mobile market)
**Dependencies:** Android Flutter Parity mappers (completed)
