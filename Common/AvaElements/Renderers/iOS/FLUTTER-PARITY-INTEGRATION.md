# Flutter Parity Layout Components - iOS Integration

## Integration Instructions for SwiftUIRenderer.kt

Add the following import at the top of `SwiftUIRenderer.kt`:

```kotlin
import com.augmentalis.avaelements.renderer.ios.mappers.flutterparity.*
import com.augmentalis.avaelements.flutter.layout.*
```

Add the following cases to the `renderComponent()` function's `when` statement (around line 73, before the `else` block):

```kotlin
// Flutter Parity: Layout components
is WrapComponent -> mapWrapComponent(component, ::renderComponent)
is ExpandedComponent -> {
    // Determine if parent is horizontal or vertical flex
    // For now, default to horizontal (can be improved with parent context)
    mapExpandedComponent(component, ::renderComponent, isInHorizontalFlex = true)
}
is FlexibleComponent -> {
    // Determine if parent is horizontal or vertical flex
    // For now, default to horizontal (can be improved with parent context)
    mapFlexibleComponent(component, ::renderComponent, isInHorizontalFlex = true)
}
is FlexComponent -> mapFlexComponent(component, ::renderComponent)
is PaddingComponent -> mapPaddingComponent(component, ::renderComponent)
is AlignComponent -> mapAlignComponent(component, ::renderComponent)
is CenterComponent -> mapCenterComponent(component, ::renderComponent)
is SizedBoxComponent -> mapSizedBoxComponent(component, ::renderComponent)
is ConstrainedBoxComponent -> mapConstrainedBoxComponent(component, ::renderComponent)
is FittedBoxComponent -> mapFittedBoxComponent(component, ::renderComponent)
```

## Component Mapping Summary

| Flutter Component | SwiftUI Equivalent | Mapper Function |
|-------------------|-------------------|-----------------|
| `WrapComponent` | Custom WrapLayout | `mapWrapComponent()` |
| `ExpandedComponent` | `.frame(maxWidth/Height: .infinity)` + `.layoutPriority()` | `mapExpandedComponent()` |
| `FlexibleComponent` | `.frame()` + `.layoutPriority()` | `mapFlexibleComponent()` |
| `FlexComponent` (Row) | `HStack` | `mapFlexComponent()` |
| `FlexComponent` (Column) | `VStack` | `mapFlexComponent()` |
| `PaddingComponent` | `.padding()` | `mapPaddingComponent()` |
| `AlignComponent` | `.frame(alignment:)` | `mapAlignComponent()` |
| `CenterComponent` | `.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)` | `mapCenterComponent()` |
| `SizedBoxComponent` (with child) | `.frame(width:height:)` | `mapSizedBoxComponent()` |
| `SizedBoxComponent` (no child) | `Spacer()` | `mapSizedBoxComponent()` |
| `ConstrainedBoxComponent` | `.frame(minWidth:maxWidth:minHeight:maxHeight:)` | `mapConstrainedBoxComponent()` |
| `FittedBoxComponent` | `.scaledToFit()` / `.scaledToFill()` / `.aspectRatio()` | `mapFittedBoxComponent()` |

## iOS-Specific Enhancements

### 1. Safe Area Support
All layout components automatically respect Safe Area insets via SwiftUI's built-in behavior.

### 2. Dynamic Type
Text components within layout containers automatically adapt to user's preferred text size.

### 3. RTL Language Support
All layout components automatically mirror for RTL languages (Arabic, Hebrew) via SwiftUI's environment.

### 4. Adaptive Layouts
Components automatically adapt for iPhone, iPad, and Mac Catalyst.

### 5. Dark Mode
All components respect iOS system appearance (light/dark mode).

## Testing

Use the following code to test Flutter Parity layout components:

```kotlin
val renderer = SwiftUIRenderer.withLiquidGlass()

// Test Flex (Row)
val rowComponent = FlexComponent(
    direction = FlexDirection.Horizontal,
    mainAxisAlignment = MainAxisAlignment.SpaceBetween,
    crossAxisAlignment = CrossAxisAlignment.Center,
    children = listOf(
        TextComponent("Start"),
        TextComponent("Center"),
        TextComponent("End")
    )
)
val swiftView = renderer.render(rowComponent)

// Test Center
val centerComponent = CenterComponent(
    child = TextComponent("Centered Text")
)
val swiftView2 = renderer.render(centerComponent)

// Test SizedBox
val sizedBoxComponent = SizedBoxComponent(
    width = Size.dp(200f),
    height = Size.dp(100f),
    child = TextComponent("Fixed Size")
)
val swiftView3 = renderer.render(sizedBoxComponent)
```

## Known Limitations

1. **WrapComponent** requires iOS 16+ for native Layout protocol. For iOS 15 and below, it falls back to manual layout calculation.

2. **ExpandedComponent** and **FlexibleComponent** currently assume parent flex direction. In a production implementation, you would track parent context to determine horizontal vs vertical flex.

3. **Baseline alignment** in `CrossAxisAlignment.Baseline` is mapped to `firstTextBaseline` in SwiftUI, which may differ slightly from Flutter's baseline alignment.

## Next Steps

1. Implement parent context tracking to properly determine flex direction for Expanded/Flexible components
2. Add visual regression tests using SwiftUI previews
3. Test on physical devices with various screen sizes (iPhone SE, iPhone 15 Pro Max, iPad Pro)
4. Verify RTL layout behavior with Arabic/Hebrew locales
5. Add performance profiling for complex layouts (100+ nested components)
