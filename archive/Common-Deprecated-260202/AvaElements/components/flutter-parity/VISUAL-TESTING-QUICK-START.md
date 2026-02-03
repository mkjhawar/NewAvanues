# Visual Testing Quick Start Guide

**For:** Developers implementing visual tests for Flutter Parity components
**Framework:** Paparazzi v1.3.1
**Date:** 2025-11-22

---

## 🚀 Quick Start (5 Minutes)

### 1. Run Existing Tests

```bash
# Generate baselines (first time only)
./gradlew :flutter-parity:recordPaparazziDebug

# Run visual regression tests
./gradlew :flutter-parity:verifyPaparazziDebug

# View HTML report
open flutter-parity/build/reports/paparazzi/index.html
```

### 2. Write Your First Test

```kotlin
// Create: src/test/kotlin/.../visual/YourComponentVisualTest.kt

import app.cash.paparazzi.Paparazzi
import com.augmentalis.avaelements.flutter.visual.PaparazziConfig
import org.junit.Rule
import org.junit.Test

class YourComponentVisualTest {
    @get:Rule
    val paparazzi = PaparazziConfig.createDefault()

    @Test
    fun yourComponent_defaultState_light() {
        paparazzi.snapshot {
            // YourComponentMapper(
            //     component = YourComponent(...)
            // )
        }
    }
}
```

### 3. Generate & Verify

```bash
# Generate baseline for your new test
./gradlew :flutter-parity:recordPaparazziDebug --tests YourComponentVisualTest

# Verify it passes
./gradlew :flutter-parity:verifyPaparazziDebug --tests YourComponentVisualTest
```

---

## 📋 Test Templates

### Template 1: Basic Component (Light/Dark)

```kotlin
@Test
fun component_defaultState_light() {
    paparazzi.snapshot {
        ComponentMapper(component = Component(...))
    }
}

@Test
fun component_defaultState_dark() {
    // Create dark theme paparazzi or use:
    // PaparazziConfig.createDark()
    paparazzi.snapshot(name = "Component_dark") {
        ComponentMapper(component = Component(...))
    }
}
```

### Template 2: State Matrix (Enabled/Disabled/Selected)

```kotlin
@Test
fun component_allStates_light() {
    val states = mapOf(
        "default" to Component(enabled = true, selected = false),
        "selected" to Component(enabled = true, selected = true),
        "disabled" to Component(enabled = false, selected = false),
        "selectedDisabled" to Component(enabled = false, selected = true)
    )

    states.forEach { (name, component) ->
        paparazzi.snapshot(name = "Component_$name") {
            ComponentMapper(component = component)
        }
    }
}
```

### Template 3: Device Matrix (All 4 Devices)

```kotlin
@Test
fun component_allDevices_light() {
    DeviceConfigurations.ALL_DEVICES.forEach { device ->
        paparazzi.unsafeUpdateConfig(device)
        val deviceName = DeviceConfigurations.DEVICE_NAMES[device] ?: "Unknown"

        paparazzi.snapshot(name = "Component_${deviceName}") {
            ComponentMapper(component = Component(...))
        }
    }
}
```

### Template 4: Animation Keyframes

```kotlin
@Test
fun animationComponent_keyframes_light() {
    val frames = listOf(
        "start_0pct" to 0.0f,
        "mid_50pct" to 0.5f,
        "end_100pct" to 1.0f
    )

    frames.forEach { (name, progress) ->
        paparazzi.snapshot(name = "AnimComponent_$name") {
            AnimComponentMapper(
                component = AnimComponent(
                    value = progress,
                    duration = Duration.milliseconds(1000),
                    child = ...
                )
            )
        }
    }
}
```

### Template 5: Accessibility (200% Font Scale)

```kotlin
@Test
fun component_accessibility_largeText() {
    val accessibilityPaparazzi = PaparazziConfig.createAccessibility()

    accessibilityPaparazzi.snapshot(name = "Component_200pct") {
        ComponentMapper(component = Component(...))
    }
}
```

---

## 🎯 Common Test Patterns

### Pattern: Interactive Component (Full Coverage)

```kotlin
class ButtonVisualTest {
    @get:Rule
    val paparazzi = PaparazziConfig.createDefault()

    @Test
    fun button_allStates_light() {
        // Enabled, disabled, pressed
    }

    @Test
    fun button_allDevices() {
        // Pixel 6, Tablet, Fold, 4a
    }

    @Test
    fun button_darkTheme() {
        // Dark mode
    }

    @Test
    fun button_accessibility() {
        // 200% font scale, touch targets
    }
}
```

### Pattern: Layout Component (Variants)

```kotlin
@Test
fun layoutComponent_variants_light() {
    val variants = listOf(
        "empty" to 0,
        "single" to 1,
        "few" to 5,
        "many" to 50
    )

    variants.forEach { (name, itemCount) ->
        paparazzi.snapshot(name = "Layout_$name") {
            LayoutMapper(
                component = Layout(itemCount = itemCount, ...)
            )
        }
    }
}
```

### Pattern: Scrolling Component (Performance)

```kotlin
@Test
fun scrollComponent_jankDetection() {
    (0..10).forEach { scrollPercent ->
        val scrollPos = (100 * scrollPercent) / 10

        paparazzi.snapshot(name = "Scroll_${scrollPercent * 10}pct") {
            ScrollMapper(
                component = Scroll(
                    scrollOffset = scrollPos.toFloat(),
                    ...
                )
            )
        }
    }
}
```

---

## 📊 Checklist for New Components

When creating visual tests for a new component, ensure:

**Basic Coverage:**
- [ ] Light theme default state
- [ ] Dark theme default state
- [ ] All interactive states (enabled/disabled/selected/pressed)
- [ ] Device matrix (at least Pixel 6 + Pixel Tablet)

**Accessibility:**
- [ ] Large text mode (200% font scale)
- [ ] Touch target validation (48dp minimum)
- [ ] Focus indicator visible
- [ ] RTL layout (if applicable)

**Performance (if applicable):**
- [ ] Animation smoothness (60 FPS)
- [ ] Scroll jank detection
- [ ] Memory leak test (100 cycles)

**Documentation:**
- [ ] KDoc on test class
- [ ] Clear test method names
- [ ] Comments for complex scenarios

---

## 🔧 Configuration Reference

### Available Paparazzi Configs

```kotlin
PaparazziConfig.createDefault()      // Pixel 6, light theme
PaparazziConfig.createDark()         // Pixel 6, dark theme
PaparazziConfig.createTablet()       // Pixel Tablet, light theme
PaparazziConfig.createAccessibility() // Pixel 6, 200% font scale
PaparazziConfig.createRTL()          // Pixel 6, RTL (Arabic)
PaparazziConfig.create(device, isDark) // Custom
```

### Available Devices

```kotlin
DeviceConfigurations.PIXEL_6          // 1080x2400, 420 DPI
DeviceConfigurations.PIXEL_TABLET     // 2560x1600, 320 DPI
DeviceConfigurations.PIXEL_FOLD_UNFOLDED // 1080x2092, 420 DPI
DeviceConfigurations.PIXEL_4A         // 1080x2340, 440 DPI
DeviceConfigurations.ALL_DEVICES      // List of all 4
```

---

## 🐛 Troubleshooting

### Test Fails with "No baseline found"

**Solution:** Generate baseline first
```bash
./gradlew :flutter-parity:recordPaparazziDebug
```

### Test Fails with "X% pixels different"

**Root Cause:** Visual regression detected

**Steps:**
1. Open diff image: `flutter-parity/build/reports/paparazzi/images/delta-{testName}.png`
2. Review red-highlighted pixels
3. If intentional change: Update baseline with `recordPaparazziDebug`
4. If bug: Fix code, re-run test

### Paparazzi Plugin Not Found

**Solution:** Sync Gradle
```bash
./gradlew --refresh-dependencies
```

### Out of Memory (OOM) Error

**Solution:** Increase heap size in `gradle.properties`
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
```

---

## 📁 File Locations

```
flutter-parity/
├── src/test/kotlin/com/augmentalis/avaelements/flutter/visual/
│   ├── PaparazziConfig.kt              # Shared config
│   ├── DeviceConfigurations.kt         # Device matrix
│   ├── animation/                      # Animation tests
│   ├── material/                       # Material tests
│   ├── layout/                         # Layout tests
│   ├── scrolling/                      # Scrolling tests
│   ├── accessibility/                  # A11y tests
│   └── performance/                    # Performance tests
└── src/test/snapshots/images/          # Baseline screenshots
```

---

## 🎓 Best Practices

**DO:**
- ✅ Use descriptive test names: `component_state_theme`
- ✅ Group related tests in one file (e.g., all chip types together)
- ✅ Test critical paths first (default state, enabled/disabled)
- ✅ Keep snapshots small (avoid full-screen captures if possible)
- ✅ Update baselines immediately after intentional UI changes

**DON'T:**
- ❌ Commit without reviewing diff images
- ❌ Generate baselines on different machines (use CI for consistency)
- ❌ Test every possible state (focus on representative samples)
- ❌ Ignore accessibility tests (WCAG compliance is critical)
- ❌ Skip dark mode tests (many users prefer dark mode)

---

## 📚 Further Reading

- **Framework Guide:** `VISUAL-TESTING-FRAMEWORK.md` (18 pages)
- **Regression Report:** `VISUAL-REGRESSION-REPORT.md` (17 pages)
- **Deliverable Summary:** `WEEK-2-AGENT-3-DELIVERABLE-SUMMARY.md`
- **Paparazzi Docs:** https://cashapp.github.io/paparazzi/

---

## 🆘 Getting Help

**Sample Tests:**
- `AnimatedOpacityVisualTest.kt` - Animation example
- `ChipsVisualTest.kt` - Material component example
- `AccessibilityVisualTest.kt` - Accessibility example
- `PerformanceVisualTest.kt` - Performance example

**Questions?**
- Review existing test files for patterns
- Check `VISUAL-TESTING-FRAMEWORK.md` for detailed explanations
- Run `./gradlew :flutter-parity:help` for Gradle commands

---

**Last Updated:** 2025-11-22
**Version:** 1.0.0
**Status:** ✅ Production Ready
