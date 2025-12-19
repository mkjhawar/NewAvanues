# Desktop Flutter Parity - Quick Start Guide
**For Developers Implementing Components**

---

## 🚀 Getting Started

### Prerequisites
- JVM 17+
- Kotlin 1.9.20+
- Compose Desktop 1.6.10+
- IntelliJ IDEA / Android Studio

### Project Structure
```
Renderers/Desktop/src/desktopMain/kotlin/com/augmentalis/avaelements/renderer/desktop/mappers/flutterparity/
├── FlutterParityLayoutMappers.kt        # Layout components
├── FlutterParityAnimationMappers.kt     # Animation components
├── FlutterParityTransitionMappers.kt    # Transition components
├── FlutterParityScrollingMappers.kt     # Scrolling components
└── FlutterParityMaterialMappers.kt      # Material components
```

---

## 📖 Component Implementation Template

### Basic Structure
```kotlin
/**
 * Render [ComponentName] using Compose Desktop
 *
 * Maps [ComponentName] to Compose [ComposeAPI] with:
 * - Feature 1
 * - Feature 2
 * - Full accessibility support
 *
 * Desktop enhancements:
 * - Mouse hover states
 * - Keyboard shortcuts
 * - High-DPI support
 *
 * Performance: 60+ FPS
 *
 * @param component [ComponentName] component to render
 * @param content Child content renderer
 */
@Composable
fun ComponentNameMapper(
    component: ComponentNameComponent,
    content: @Composable () -> Unit
) {
    // 1. Handle state
    val state = rememberComponentState()

    // 2. Handle layout direction (for RTL)
    val layoutDirection = LocalLayoutDirection.current

    // 3. Build modifier with desktop features
    val modifier = Modifier
        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
        // Add other modifiers

    // 4. Render Compose component
    ComposeComponent(
        modifier = modifier,
        // ... properties
    ) {
        content()
    }
}
```

---

## 🖱️ Desktop Enhancements Checklist

### For Every Interactive Component

✅ **Mouse Cursor**
```kotlin
.pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
```

Cursor Types:
- `HAND_CURSOR` - Clickable elements (buttons, chips, links)
- `MOVE_CURSOR` - Draggable elements (reorderable list items)
- `TEXT_CURSOR` - Text input fields
- `DEFAULT_CURSOR` - Non-interactive elements

✅ **Hover States**
```kotlin
// Use Compose's built-in hover support
// Material3 components handle this automatically
// For custom components, track hover state:
var isHovered by remember { mutableStateOf(false) }
Box(
    modifier = Modifier.hoverable(interactionSource = remember { MutableInteractionSource() })
)
```

✅ **Keyboard Navigation**
```kotlin
// Compose Desktop handles Tab navigation automatically
// Add keyboard shortcuts if needed:
.onKeyEvent { keyEvent ->
    when {
        keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown -> {
            onClick()
            true
        }
        else -> false
    }
}
```

✅ **Focus Indicators**
```kotlin
// Material3 components include focus indicators
// For custom components:
.border(
    width = if (isFocused) 2.dp else 0.dp,
    color = MaterialTheme.colorScheme.primary,
    shape = RoundedCornerShape(4.dp)
)
```

---

## 🌍 RTL Support

### Horizontal Layouts
Always handle RTL for horizontal layouts:

```kotlin
val layoutDirection = LocalLayoutDirection.current

// For scrolling
val actualReverse = if (layoutDirection == LayoutDirection.Rtl) {
    !component.reverse
} else {
    component.reverse
}

// For alignment
val alignment = if (layoutDirection == LayoutDirection.Rtl) {
    Alignment.End
} else {
    Alignment.Start
}
```

### Use `Start`/`End` instead of `Left`/`Right`
```kotlin
// ✅ Good
Modifier.padding(start = 16.dp, end = 16.dp)
Alignment.TopStart
Arrangement.Start

// ❌ Bad
Modifier.padding(left = 16.dp, right = 16.dp)
Alignment.TopLeft
Arrangement.Left
```

---

## 🎨 Code Reuse from Android

### Step 1: Find Android Implementation
```bash
cd Renderers/Android/src/androidMain/kotlin/com/augmentalis/avaelements/renderer/android/mappers/flutterparity/
cat FlutterParity*Mappers.kt | grep "fun ComponentNameMapper"
```

### Step 2: Copy and Adapt
1. Copy the Android function
2. Remove Android-specific imports (replace with desktop equivalents)
3. Add desktop cursor support
4. Add RTL handling for horizontal layouts
5. Test on macOS, Windows, Linux

### Example: Android → Desktop
```kotlin
// Android
@Composable
fun AnimatedOpacityMapper(
    component: AnimatedOpacity,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(...)
    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
        content()
    }
}

// Desktop (added cursor support)
@Composable
fun AnimatedOpacityMapper(
    component: AnimatedOpacity,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(...)
    Box(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)))
    ) {
        content()
    }
}
```

---

## 🧪 Testing

### Unit Test Template
```kotlin
// Renderers/Desktop/src/desktopTest/kotlin/.../ComponentNameMapperTest.kt

@Test
fun `ComponentName renders with correct properties`() = runComposeUiTest {
    // Arrange
    val component = ComponentNameComponent(
        property1 = value1,
        property2 = value2
    )

    // Act
    setContent {
        ComponentNameMapper(component) {
            Text("Child content")
        }
    }

    // Assert
    onNodeWithText("Child content").assertExists()
    // Add more assertions
}

@Test
fun `ComponentName handles mouse hover`() = runComposeUiTest {
    var hovered = false
    setContent {
        ComponentNameMapper(component) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .testTag("target")
                    .hoverable(
                        interactionSource = remember { MutableInteractionSource() },
                        onHover = { hovered = it }
                    )
            )
        }
    }

    onNodeWithTag("target").performMouseInput { enter() }
    assertTrue(hovered)
}
```

---

## 📊 Performance Guidelines

### Target Metrics
- **FPS**: 60+ (120 on high-refresh displays)
- **Scroll Latency**: <16ms
- **Memory**: <100 MB for 10K items
- **Startup**: No noticeable delay

### Optimization Tips

1. **Use GPU Acceleration**
```kotlin
// ✅ GPU-accelerated (graphicsLayer)
Modifier.graphicsLayer {
    alpha = 0.5f
    translationX = 100f
    rotationZ = 45f
}

// ❌ CPU-based (triggers recomposition)
Modifier.alpha(0.5f)
```

2. **Lazy Loading**
```kotlin
// ✅ Lazy loading for large lists
LazyColumn {
    items(10000) { index ->
        ItemContent(index)
    }
}

// ❌ Eager loading
Column {
    repeat(10000) { index ->
        ItemContent(index)
    }
}
```

3. **Stable Keys**
```kotlin
// ✅ Stable keys for item reuse
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemContent(item)
    }
}

// ❌ No keys (recreates views)
LazyColumn {
    items(items) { item ->
        ItemContent(item)
    }
}
```

4. **Avoid Unnecessary Recomposition**
```kotlin
// ✅ Remember expensive computations
val processedData = remember(rawData) {
    expensiveProcessing(rawData)
}

// ❌ Recompute every frame
val processedData = expensiveProcessing(rawData)
```

---

## 🐛 Common Issues & Solutions

### Issue: Component not rendering
**Solution**: Check that mapper is registered in main renderer

### Issue: Hover cursor not showing
**Solution**: Add `.pointerHoverIcon()` modifier

### Issue: RTL layout broken
**Solution**: Use `LocalLayoutDirection` and swap Start/End

### Issue: Performance slow
**Solution**: Profile with Compose UI Check, optimize hot paths

### Issue: Tests failing
**Solution**: Use `runComposeUiTest`, check test environment setup

---

## 📚 Resources

### Documentation
- [Compose Desktop Docs](https://github.com/JetBrains/compose-multiplatform)
- [Material3 Components](https://m3.material.io/components)
- [Compose Animation](https://developer.android.com/jetpack/compose/animation)
- [Compose Layout](https://developer.android.com/jetpack/compose/layouts)

### Code References
- Android implementations: `Renderers/Android/.../flutterparity/`
- Desktop implementations: `Renderers/Desktop/.../flutterparity/`
- Common models: `components/flutter-parity/src/commonMain/`

### Tools
- IntelliJ IDEA (Compose preview)
- Compose Multiplatform Wizard
- Kotlin Playground
- Paparazzi (visual regression - Android only, adapt for Desktop)

---

## 🎯 Best Practices

### DO ✅
- Copy from Android implementations
- Add desktop cursor support
- Handle RTL for horizontal layouts
- Use GPU acceleration (graphicsLayer)
- Write comprehensive inline docs
- Test on all 3 platforms (macOS, Windows, Linux)
- Use Material3 components when available
- Follow Compose naming conventions

### DON'T ❌
- Reinvent the wheel (reuse Android code)
- Forget cursor icons
- Use Left/Right instead of Start/End
- Trigger unnecessary recomposition
- Skip documentation
- Test only on one platform
- Mix Material2 and Material3
- Use platform-specific APIs without abstraction

---

## 🔍 Quick Reference

### Import Statements
```kotlin
// Compose Desktop
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

// Compose UI
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Desktop-specific
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.awt.Cursor
```

### Component Categories

| Category | File | Examples |
|----------|------|----------|
| Layout | FlutterParityLayoutMappers.kt | Wrap, Flexible, Padding |
| Animation | FlutterParityAnimationMappers.kt | AnimatedOpacity, AnimatedContainer |
| Transition | FlutterParityTransitionMappers.kt | FadeTransition, SlideTransition |
| Scrolling | FlutterParityScrollingMappers.kt | ListViewBuilder, GridViewBuilder |
| Material | FlutterParityMaterialMappers.kt | FilterChip, CheckboxListTile |

---

## 📞 Getting Help

### Questions?
1. Check Android implementation first
2. Review this guide
3. Read inline documentation
4. Check Compose Desktop docs
5. Ask team for review

### Found a Bug?
1. Check if it's in Android too
2. File issue with reproduction steps
3. Include platform (macOS/Windows/Linux)
4. Add performance metrics if relevant

### Want to Contribute?
1. Pick a component from roadmap
2. Follow this template
3. Add tests
4. Submit PR with docs
5. Request code review

---

**Guide Version**: 1.0
**Last Updated**: 2025-11-23
**Maintainer**: Engineering Team
**Status**: ✅ Active
