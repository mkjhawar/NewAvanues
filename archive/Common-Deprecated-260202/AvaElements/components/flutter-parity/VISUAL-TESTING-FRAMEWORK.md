# Visual Testing Framework for Flutter Parity Components

**Date:** 2025-11-22
**Version:** 1.0.0
**Agent:** Week 2 - Agent 3 (Visual Testing Engineer)
**Status:** 🚧 IN PROGRESS

---

## Executive Summary

This document outlines the comprehensive visual testing framework for all 58 Flutter Parity components in the AvaElements library. The framework uses Paparazzi for Android screenshot testing with support for multiple device configurations, dark mode, accessibility modes, and performance validation.

**Target:** 200+ screenshots across 58 components
**Device Matrix:** 4 devices (phone, tablet, foldable, small phone)
**Modes:** Light/Dark, Accessibility (200% scale), RTL layout
**Performance:** Animation frame capture, scroll jank detection

---

## Component Inventory (58 Components)

### Animation Components (8)
1. AnimatedContainer
2. AnimatedOpacity
3. AnimatedPositioned
4. AnimatedDefaultTextStyle
5. AnimatedPadding
6. AnimatedSize
7. AnimatedAlign
8. AnimatedScale

### Layout Components (9)
9. Wrap
10. Expanded
11. Flexible
12. Padding
13. Align
14. Center
15. SizedBox
16. ConstrainedBox
17. FittedBox

### Scrolling Components (7)
18. ListViewBuilder
19. ListViewSeparated
20. GridViewBuilder
21. PageView
22. ReorderableListView
23. CustomScrollView
24. Slivers (SliverList, SliverGrid, SliverFixedExtentList, SliverAppBar)

### Material Components (12)
25. FilterChip
26. ActionChip
27. ChoiceChip
28. InputChip
29. FilledButton
30. PopupMenuButton
31. RefreshIndicator
32. IndexedStack
33. VerticalDivider
34. FadeInImage
35. CircleAvatar
36. RichText

### Material Lists (3)
37. CheckboxListTile
38. SwitchListTile
39. ExpansionTile

### Advanced Material (3)
40. SelectableText
41. EndDrawer
42. Flex

### Transition Components (18)
43. FadeTransition
44. SlideTransition
45. ScaleTransition
46. RotationTransition
47. PositionedTransition
48. SizeTransition
49. AnimatedCrossFade
50. AnimatedSwitcher
51. AnimatedList
52. AnimatedModalBarrier
53. DecoratedBoxTransition
54. AlignTransition
55. DefaultTextStyleTransition
56. RelativePositionedTransition
57. Hero
58. (Additional advanced transitions as implemented)

**Total: 58 components**

---

## Visual Testing Strategy

### 1. Screenshot Testing Framework: Paparazzi

**Why Paparazzi?**
- No emulator/device required
- Fast execution (runs in JVM)
- Pixel-perfect screenshot comparisons
- Easy CI/CD integration
- Native Compose support

**Setup:**
```kotlin
// build.gradle.kts
plugins {
    id("app.cash.paparazzi") version "1.3.1"
}

dependencies {
    testImplementation("app.cash.paparazzi:paparazzi:1.3.1")
}
```

### 2. Device Matrix (4 Devices)

| Device | Resolution | DPI | Purpose |
|--------|-----------|-----|---------|
| Pixel 6 | 1080x2400 | 420 | Standard phone |
| Pixel Tablet | 2560x1600 | 320 | Tablet layout |
| Pixel Fold (unfolded) | 1080x2092 | 420 | Foldable/wide |
| Pixel 4a | 1080x2340 | 440 | Small phone |

### 3. Testing Modes

**Mode Matrix:**
- Light theme
- Dark theme
- Accessibility (200% font scale)
- RTL layout (Arabic/Hebrew)
- Touch target highlighting
- Focus indicators

**Total Combinations:** 58 components × 4 devices × 6 modes = **1,392 potential screenshots**

**Practical Target:** 200-300 screenshots (critical paths + representative samples)

---

## Screenshot Test Categories

### Category 1: Static Component States

**Coverage:** All 58 components in default state

**Test Pattern:**
```kotlin
@Test
fun componentName_defaultState_light() {
    paparazzi.snapshot {
        ComponentNameMapper(
            component = ComponentName(/* default props */)
        )
    }
}

@Test
fun componentName_defaultState_dark() {
    paparazzi.snapshot(theme = "dark") {
        ComponentNameMapper(
            component = ComponentName(/* default props */)
        )
    }
}
```

**Screenshots per component:** 2 (light + dark) × 4 devices = 8 screenshots
**Total:** 58 × 8 = **464 screenshots** (will sample 100)

---

### Category 2: Interactive States

**Coverage:** Material components with interaction states

**States to capture:**
- Enabled (default)
- Disabled
- Pressed/Focused
- Selected (for chips, toggles)
- Error state (for inputs)

**Example: FilterChip**
```kotlin
@Test
fun filterChip_allStates_matrix() {
    val states = listOf(
        "default" to FilterChip(label = "Filter", selected = false),
        "selected" to FilterChip(label = "Filter", selected = true),
        "disabled" to FilterChip(label = "Filter", enabled = false),
        "selectedDisabled" to FilterChip(label = "Filter", selected = true, enabled = false)
    )

    states.forEach { (name, chip) ->
        paparazzi.snapshot(name = "FilterChip_$name") {
            FilterChipMapper(component = chip)
        }
    }
}
```

**Target:** 25 components × 4 states × 2 themes = **200 screenshots**

---

### Category 3: Animation Keyframes

**Coverage:** 8 animation components + 18 transition components

**Frames to capture:**
- Start (0%)
- Mid (50%)
- End (100%)

**Example: AnimatedOpacity**
```kotlin
@Test
fun animatedOpacity_animationFrames() {
    val frames = listOf(
        "start_0pct" to AnimatedOpacity(opacity = 0.0f, duration = Duration.seconds(1), child = Box()),
        "mid_50pct" to AnimatedOpacity(opacity = 0.5f, duration = Duration.seconds(1), child = Box()),
        "end_100pct" to AnimatedOpacity(opacity = 1.0f, duration = Duration.seconds(1), child = Box())
    )

    frames.forEach { (name, component) ->
        paparazzi.snapshot(name = "AnimatedOpacity_$name") {
            AnimatedOpacityMapper(component = component)
        }
    }
}
```

**Target:** 26 animation components × 3 frames × 2 themes = **156 screenshots**

---

### Category 4: Layout Variants

**Coverage:** 9 layout components + 7 scrolling components

**Variants:**
- Empty (0 items)
- Single item (1 item)
- Few items (3-5 items)
- Many items (50+ items)
- Overflow/scroll state

**Example: ListViewBuilder**
```kotlin
@Test
fun listViewBuilder_itemCounts() {
    val variants = listOf(
        "empty" to ListViewBuilder(itemCount = 0, itemBuilder = "buildItem"),
        "single" to ListViewBuilder(itemCount = 1, itemBuilder = "buildItem"),
        "few" to ListViewBuilder(itemCount = 5, itemBuilder = "buildItem"),
        "many" to ListViewBuilder(itemCount = 50, itemBuilder = "buildItem")
    )

    variants.forEach { (name, component) ->
        paparazzi.snapshot(name = "ListViewBuilder_$name") {
            ListViewBuilderMapper(component = component)
        }
    }
}
```

**Target:** 16 layout/scrolling components × 4 variants × 2 themes = **128 screenshots**

---

### Category 5: Accessibility Visual Tests

**Coverage:** All 58 components

**Accessibility modes:**
- Large text (200% scale)
- Touch target highlighting (48dp minimum)
- Focus indicators visible
- Color contrast validation

**Example: Touch Target Highlighting**
```kotlin
@Test
fun allComponents_touchTargets_highlighted() {
    val components = listOf(
        FilterChip(...),
        FilledButton(...),
        CheckboxListTile(...),
        // ... all interactive components
    )

    paparazzi.snapshot(name = "TouchTargets_All", showTouchTargets = true) {
        Column {
            components.forEach { component ->
                ComponentMapper(component = component)
            }
        }
    }
}
```

**Target:** 58 components × 2 accessibility modes = **116 screenshots**

---

### Category 6: Performance Visual Tests

**Coverage:** Animation and scrolling components

**Metrics to visualize:**
- Animation smoothness (60 FPS validation)
- Scroll jank detection (green = smooth, red = jank)
- Layout recomposition tracking
- Memory leak indicators

**Example: Animation Smoothness**
```kotlin
@Test
fun animatedContainer_smoothness_60fps() {
    val framesPerSecond = 60
    val duration = 1000 // ms
    val totalFrames = (framesPerSecond * duration) / 1000

    (0..totalFrames).forEach { frame ->
        val progress = frame.toFloat() / totalFrames
        paparazzi.snapshot(name = "AnimatedContainer_frame_$frame") {
            AnimatedContainerMapper(
                component = AnimatedContainer(
                    width = Size.dp(100 + (progress * 100)),
                    height = Size.dp(100),
                    duration = Duration.milliseconds(duration),
                    child = Box()
                )
            )
        }
    }

    // Validate: All frames should be captured at ~16.67ms intervals (60 FPS)
}
```

**Target:** 26 animation components × 60 frames = **1,560 frames** (will sample 30 screenshots for report)

---

## Test File Structure

```
flutter-parity/src/androidTest/kotlin/com/augmentalis/avaelements/flutter/visual/
├── PaparazziConfig.kt                    // Shared configuration
├── DeviceConfigurations.kt               // Device matrix definitions
├── animation/
│   ├── AnimatedContainerVisualTest.kt
│   ├── AnimatedOpacityVisualTest.kt
│   ├── AnimatedPositionedVisualTest.kt
│   ├── AnimatedDefaultTextStyleVisualTest.kt
│   ├── AnimatedPaddingVisualTest.kt
│   ├── AnimatedSizeVisualTest.kt
│   ├── AnimatedAlignVisualTest.kt
│   └── AnimatedScaleVisualTest.kt
├── layout/
│   ├── WrapVisualTest.kt
│   ├── ExpandedFlexibleVisualTest.kt
│   ├── PaddingAlignCenterVisualTest.kt
│   ├── SizedBoxConstrainedBoxVisualTest.kt
│   └── FittedBoxVisualTest.kt
├── scrolling/
│   ├── ListViewBuilderVisualTest.kt
│   ├── GridViewBuilderVisualTest.kt
│   ├── PageViewVisualTest.kt
│   ├── ReorderableListViewVisualTest.kt
│   ├── CustomScrollViewVisualTest.kt
│   └── SliversVisualTest.kt
├── material/
│   ├── ChipsVisualTest.kt               // All 4 chip types
│   ├── ListTilesVisualTest.kt           // All 3 list tile types
│   ├── ButtonsVisualTest.kt
│   ├── AdvancedMaterialVisualTest.kt
│   └── DividersAvatarsVisualTest.kt
├── transitions/
│   ├── FadeSlideScaleVisualTest.kt
│   ├── RotationPositionedVisualTest.kt
│   ├── SizeDecoratedBoxVisualTest.kt
│   ├── AnimatedCrossFadeSwitcherVisualTest.kt
│   └── HeroVisualTest.kt
├── accessibility/
│   ├── LargeTextVisualTest.kt
│   ├── TouchTargetsVisualTest.kt
│   ├── FocusIndicatorsVisualTest.kt
│   └── ColorContrastVisualTest.kt
└── performance/
    ├── AnimationSmoothnessVisualTest.kt
    ├── ScrollJankVisualTest.kt
    └── MemoryLeakVisualTest.kt
```

**Total Test Files:** ~35 files

---

## Baseline Generation

### Initial Baseline Creation

```bash
# Generate all baseline screenshots
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:recordPaparazziDebug

# Output location
flutter-parity/src/test/snapshots/images/
├── com.augmentalis.avaelements.flutter.visual.animation.AnimatedContainerVisualTest/
│   ├── animatedContainer_defaultState_light.png
│   ├── animatedContainer_defaultState_dark.png
│   ├── animatedContainer_animation_start.png
│   ├── animatedContainer_animation_mid.png
│   └── animatedContainer_animation_end.png
└── ... (200+ more screenshots)
```

### Baseline Management

**Baseline Storage:**
- Git LFS for large image files
- Organized by component category
- Named: `{component}_{variant}_{theme}_{device}.png`

**Baseline Updates:**
```bash
# After intentional UI changes, update baselines
./gradlew :flutter-parity:recordPaparazziDebug

# Verify baselines
./gradlew :flutter-parity:verifyPaparazziDebug
```

---

## Visual Regression Detection

### Pixel-Diff Algorithm

**Paparazzi's Built-in Comparison:**
- Pixel-by-pixel comparison
- Threshold: 0.1% difference allowed (for anti-aliasing)
- Highlights: Red overlay on changed pixels

**Custom Diff Visualization:**
```kotlin
@Test
fun visualRegression_withDiffHighlight() {
    val baseline = loadBaselineImage("FilterChip_selected.png")
    val current = captureCurrentScreenshot()

    val diff = PixelDiff.compare(baseline, current, threshold = 0.001)

    if (diff.percentDifferent > 0.1) {
        // Generate diff image with red highlights
        val diffImage = PixelDiff.generateDiffImage(baseline, current)
        saveDiffImage("FilterChip_selected_DIFF.png", diffImage)

        fail("Visual regression detected: ${diff.percentDifferent}% pixels changed")
    }
}
```

### Regression Report

**Automated Report Generation:**
```markdown
# Visual Regression Report
**Date:** 2025-11-22
**Build:** #1234
**Commit:** abc123

## Summary
- Total Screenshots: 215
- Passed: 210 ✅
- Failed: 5 ❌
- Percent Diff: 2.3%

## Failures

### FilterChip - Selected State (Dark Mode)
- **Diff:** 12.5% pixels changed
- **Device:** Pixel 6
- **Images:**
  - [Baseline](baseline.png)
  - [Current](current.png)
  - [Diff](diff.png)
- **Root Cause:** Background color changed from #1E88E5 to #2196F3

### AnimatedOpacity - Mid Frame
- **Diff:** 5.2% pixels changed
- **Device:** Pixel Tablet
- **Images:** ...
- **Root Cause:** Opacity interpolation curve changed
```

---

## Device Matrix Testing

### Device Configurations

```kotlin
// DeviceConfigurations.kt
object DeviceConfigurations {
    val PIXEL_6 = DeviceConfig.PIXEL_6.copy(
        screenHeight = 2400,
        screenWidth = 1080,
        xdpi = 420,
        ydpi = 420,
        orientation = ScreenOrientation.PORTRAIT,
        uiMode = Configuration.UI_MODE_NIGHT_NO,
        locale = "en-rUS"
    )

    val PIXEL_TABLET = DeviceConfig.PIXEL_TABLET.copy(
        screenHeight = 1600,
        screenWidth = 2560,
        xdpi = 320,
        ydpi = 320,
        orientation = ScreenOrientation.LANDSCAPE,
        uiMode = Configuration.UI_MODE_NIGHT_NO,
        locale = "en-rUS"
    )

    val PIXEL_FOLD_UNFOLDED = DeviceConfig.PIXEL_FOLD.copy(
        screenHeight = 2092,
        screenWidth = 1080,
        xdpi = 420,
        ydpi = 420,
        orientation = ScreenOrientation.PORTRAIT,
        uiMode = Configuration.UI_MODE_NIGHT_NO,
        locale = "en-rUS"
    )

    val PIXEL_4A = DeviceConfig.PIXEL_4A.copy(
        screenHeight = 2340,
        screenWidth = 1080,
        xdpi = 440,
        ydpi = 440,
        orientation = ScreenOrientation.PORTRAIT,
        uiMode = Configuration.UI_MODE_NIGHT_NO,
        locale = "en-rUS"
    )

    val ALL_DEVICES = listOf(PIXEL_6, PIXEL_TABLET, PIXEL_FOLD_UNFOLDED, PIXEL_4A)
}
```

### Device Matrix Test Pattern

```kotlin
@Test
fun filterChip_allDevices_light() {
    DeviceConfigurations.ALL_DEVICES.forEach { device ->
        paparazzi.unsafeUpdateConfig(device)

        paparazzi.snapshot(name = "FilterChip_${device.name}_light") {
            FilterChipMapper(
                component = FilterChip(label = "Filter", selected = true)
            )
        }
    }
}
```

---

## Accessibility Visual Testing

### Large Text Mode (200% Scale)

```kotlin
@Test
fun allComponents_largeText_200percent() {
    val fontScale = 2.0f

    paparazzi.unsafeUpdateConfig(
        DeviceConfigurations.PIXEL_6.copy(fontScale = fontScale)
    )

    val components = ComponentInventory.ALL_58_COMPONENTS

    components.forEach { component ->
        paparazzi.snapshot(name = "${component.name}_largeText") {
            ComponentMapper(component = component)
        }
    }
}
```

### Touch Target Highlighting

```kotlin
@Test
fun interactiveComponents_touchTargets_48dp() {
    val interactiveComponents = listOf(
        FilterChip(...),
        FilledButton(...),
        CheckboxListTile(...),
        SwitchListTile(...)
        // ... all interactive components
    )

    paparazzi.snapshot(name = "TouchTargets_All", showLayoutBounds = true) {
        Column(modifier = Modifier.background(Color.White)) {
            interactiveComponents.forEach { component ->
                Box(
                    modifier = Modifier
                        .size(48.dp) // Minimum touch target
                        .border(2.dp, Color.Red) // Highlight boundary
                ) {
                    ComponentMapper(component = component)
                }
            }
        }
    }

    // Validation: All components should fit within 48dp bounds
}
```

### Focus Indicators

```kotlin
@Test
fun allComponents_focusIndicators_visible() {
    val components = ComponentInventory.ALL_58_COMPONENTS

    components.forEach { component ->
        paparazzi.snapshot(name = "${component.name}_focused") {
            // Simulate focused state
            CompositionLocalProvider(
                LocalFocusManager provides FakeFocusManager(focused = true)
            ) {
                ComponentMapper(component = component)
            }
        }
    }

    // Visual validation: Focus indicators should be visible (border, shadow, color change)
}
```

### Color Contrast Validation

```kotlin
@Test
fun allComponents_colorContrast_WCAG_AA() {
    val components = ComponentInventory.ALL_58_COMPONENTS

    components.forEach { component ->
        val screenshot = paparazzi.snapshot {
            ComponentMapper(component = component)
        }

        val contrastRatio = ColorContrast.calculate(screenshot)

        // WCAG AA requires 4.5:1 for normal text, 3:1 for large text
        assert(contrastRatio >= 4.5) {
            "Component ${component.name} fails WCAG AA contrast: $contrastRatio"
        }
    }
}
```

---

## Performance Visual Testing

### Animation Smoothness (60 FPS)

**Frame Capture Strategy:**
```kotlin
@Test
fun animatedContainer_smoothness_60fps() {
    val duration = 1000 // ms
    val expectedFrames = 60
    val frameInterval = duration / expectedFrames // 16.67ms

    val capturedFrames = mutableListOf<Bitmap>()

    (0..expectedFrames).forEach { frame ->
        val timestamp = frame * frameInterval
        val progress = frame.toFloat() / expectedFrames

        val screenshot = paparazzi.snapshot(
            name = "AnimatedContainer_frame_${frame}_${timestamp}ms"
        ) {
            AnimatedContainerMapper(
                component = AnimatedContainer(
                    width = Size.dp(100 + (progress * 100)),
                    height = Size.dp(100),
                    duration = Duration.milliseconds(duration),
                    child = Box()
                )
            )
        }

        capturedFrames.add(screenshot)
    }

    // Validate: Should have 60-61 frames (allowing for rounding)
    assert(capturedFrames.size in 60..61) {
        "Expected 60 frames, got ${capturedFrames.size}"
    }

    // Validate: Frames should be different (animation is progressing)
    val uniqueFrames = capturedFrames.distinct()
    assert(uniqueFrames.size >= 50) {
        "Animation appears stuck, only ${uniqueFrames.size} unique frames"
    }
}
```

**Visual Report:**
- Thumbnail strip of all 60 frames
- Histogram showing frame time distribution
- Green = 60 FPS, Yellow = 55-59 FPS, Red = <55 FPS

### Scroll Jank Detection

```kotlin
@Test
fun listViewBuilder_scrollJank_detection() {
    val itemCount = 100
    val scrollPositions = (0..100 step 10).toList() // Scroll positions: 0%, 10%, 20%, ..., 100%

    scrollPositions.forEach { scrollPercent ->
        paparazzi.snapshot(name = "ListView_scroll_${scrollPercent}pct") {
            // Simulate scroll position
            val scrollOffset = (itemCount * scrollPercent) / 100

            ListViewBuilderMapper(
                component = ListViewBuilder(
                    itemCount = itemCount,
                    itemBuilder = "buildItem",
                    controller = ScrollController(initialScrollOffset = scrollOffset.toFloat())
                )
            )
        }
    }

    // Visual validation: Smooth scroll progression, no duplicated/missing items
}
```

### Layout Recomposition Tracking

```kotlin
@Test
fun animatedSize_recomposition_minimal() {
    var recompositionCount = 0

    paparazzi.snapshot {
        AnimatedSizeMapper(
            component = AnimatedSize(
                duration = Duration.milliseconds(1000),
                child = Box(
                    modifier = Modifier.onGloballyPositioned {
                        recompositionCount++
                    }
                )
            )
        )
    }

    // Validation: Should only recompose when size changes, not on every frame
    assert(recompositionCount <= 5) {
        "Too many recompositions: $recompositionCount (expected ≤5)"
    }
}
```

---

## Deliverables Summary

### Test Files (35 files)
- Animation visual tests: 8 files
- Layout visual tests: 5 files
- Scrolling visual tests: 6 files
- Material visual tests: 5 files
- Transition visual tests: 5 files
- Accessibility visual tests: 4 files
- Performance visual tests: 3 files

### Screenshots (200-300 total)
- Static component states: ~100 screenshots
- Interactive states: ~50 screenshots
- Animation keyframes: ~30 screenshots
- Layout variants: ~30 screenshots
- Accessibility modes: ~30 screenshots
- Performance validation: ~20 screenshots

### Reports
1. **Visual Regression Report** - Automated diff analysis
2. **Device Compatibility Matrix** - 4 devices × 58 components
3. **Accessibility Visual Audit** - WCAG compliance
4. **Performance Visual Benchmarks** - 60 FPS validation

### Timeline
- **Setup (Day 1):** Paparazzi configuration, device matrix, base utilities
- **Implementation (Days 2-3):** 35 test files, 200+ screenshots
- **Validation (Day 4):** Run all tests, generate baselines
- **Reporting (Day 5):** Generate reports, document findings

**Total Time:** 3-4 hours (as requested)

---

## Next Steps

### Immediate Actions
1. Set up Paparazzi in build.gradle.kts
2. Create PaparazziConfig.kt and DeviceConfigurations.kt
3. Implement first visual test (FilterChip)
4. Generate baseline for 1 component
5. Validate pixel-diff algorithm

### Short-term (Week 2)
1. Implement all 35 visual test files
2. Generate 200+ baselines
3. Run visual regression suite
4. Document failures and root causes

### Long-term (Week 3+)
1. Integrate into CI/CD pipeline
2. Set up automatic baseline updates
3. Create visual diff dashboard
4. Performance monitoring integration

---

**Status:** 🚧 Framework designed, ready for implementation
**Next Agent:** Begin implementation (WEEK 2 - AGENT 3)
