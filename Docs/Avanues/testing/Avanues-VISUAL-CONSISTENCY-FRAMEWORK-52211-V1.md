# Visual Consistency Testing Framework
**Cross-Platform Screenshot Comparison & Regression Detection**

**Author:** Agent 4 - Cross-Platform Testing Specialist
**Date:** 2025-11-22
**Status:** Framework Design Complete

---

## EXECUTIVE SUMMARY

### Purpose

Ensure visual consistency of Flutter-parity components across all 4 platforms (Android, iOS, Web, Desktop) with automated screenshot comparison and regression detection.

### Scope

- **Components:** 58 Flutter-parity components
- **Platforms:** 4 (Android, iOS, Web, Desktop)
- **Scenarios:** 8 per component (default, dark, hover, focus, active, disabled, accessibility, RTL)
- **Total Screenshots:** 58 × 4 × 8 = **1,856 screenshots**

### Tools

| Platform | Tool | Technology |
|----------|------|------------|
| Android | Paparazzi | Snapshot testing for Compose |
| iOS | Swift Snapshot Testing | iOS snapshot library |
| Web | Playwright | Browser automation |
| Desktop | Compose Screenshot API | Built-in Compose Desktop |

---

## SECTION 1: VISUAL TESTING STRATEGY

### Test Pyramid for Visual Testing

```
                ╱╲
               ╱  ╲
              ╱Pixel╲          10% - Exact pixel comparison
             ╱Perfect╲         (critical UI elements)
            ╱────────╲
           ╱          ╲
          ╱ Structural ╲       40% - SSIM comparison
         ╱  Similarity  ╲      (general layout validation)
        ╱──────────────╲
       ╱                ╲
      ╱   Visual Smoke   ╲    50% - Renders without crash
     ╱      Tests         ╲   (basic rendering check)
    ╱──────────────────────╲
```

**Total Visual Tests:** 1,856
- **Smoke Tests:** 928 (50%)
- **Structural Tests:** 742 (40%)
- **Pixel-Perfect Tests:** 186 (10%)

---

## SECTION 2: SCREENSHOT SCENARIOS

### 8 Test Scenarios per Component

#### 1. Default State (Light Theme)
**Purpose:** Baseline screenshot in standard configuration
**Configuration:**
- Light theme
- Default size
- No interaction
- Standard font scale (100%)
- LTR layout

#### 2. Dark Theme
**Purpose:** Validate dark mode appearance
**Configuration:**
- Dark theme
- Same size as default
- Color inversion
- Contrast validation

#### 3. Hover State (Web/Desktop Only)
**Purpose:** Validate hover effects
**Configuration:**
- Mouse cursor over component
- Highlight/elevation changes
- Tooltip appearance

#### 4. Focus State
**Purpose:** Validate keyboard focus indicators
**Configuration:**
- Component has focus
- Focus outline visible
- Accessibility ring present

#### 5. Active/Pressed State
**Purpose:** Validate touch/click feedback
**Configuration:**
- Component being pressed
- Ripple effect (Android)
- State changes visible

#### 6. Disabled State
**Purpose:** Validate disabled appearance
**Configuration:**
- Component disabled
- Reduced opacity
- No interaction possible

#### 7. Accessibility (200% Font Scale)
**Purpose:** Validate large text support
**Configuration:**
- Font scale 2.0x
- Layout adapts correctly
- No text truncation
- WCAG 2.1 AA compliance

#### 8. RTL Layout (Arabic/Hebrew)
**Purpose:** Validate right-to-left support
**Configuration:**
- RTL layout direction
- Mirrored UI elements
- Text alignment correct

---

## SECTION 3: COMPARISON ALGORITHMS

### Algorithm Selection by Test Type

#### Pixel-Perfect Comparison
**Use Cases:**
- Icons and graphics
- Buttons and chips
- Color-critical components

**Algorithm:** Direct pixel comparison
**Threshold:** 0.1% difference allowed (anti-aliasing tolerance)

```kotlin
fun pixelPerfectCompare(actual: Bitmap, baseline: Bitmap): Double {
    var differentPixels = 0
    val totalPixels = actual.width * actual.height

    for (y in 0 until actual.height) {
        for (x in 0 until actual.width) {
            if (actual.getPixel(x, y) != baseline.getPixel(x, y)) {
                differentPixels++
            }
        }
    }

    return differentPixels.toDouble() / totalPixels
}
```

#### Structural Similarity (SSIM)
**Use Cases:**
- Complex layouts
- Animation frames
- Responsive components

**Algorithm:** Structural Similarity Index Measure
**Threshold:** SSIM > 0.95

```kotlin
fun ssimCompare(actual: Bitmap, baseline: Bitmap): Double {
    // Luminance comparison
    val luminanceSimilarity = calculateLuminance(actual, baseline)

    // Contrast comparison
    val contrastSimilarity = calculateContrast(actual, baseline)

    // Structure comparison
    val structureSimilarity = calculateStructure(actual, baseline)

    // SSIM formula
    return (luminanceSimilarity * contrastSimilarity * structureSimilarity)
}
```

#### Perceptual Hash (pHash)
**Use Cases:**
- Platform rendering differences
- Font rendering variations
- Minor layout shifts

**Algorithm:** Perceptual hashing
**Threshold:** Hamming distance < 5

```kotlin
fun perceptualHashCompare(actual: Bitmap, baseline: Bitmap): Int {
    val hashActual = calculatePerceptualHash(actual)
    val hashBaseline = calculatePerceptualHash(baseline)
    return hammingDistance(hashActual, hashBaseline)
}
```

---

## SECTION 4: PLATFORM-SPECIFIC IMPLEMENTATION

### Android (Paparazzi)

**Setup:**
```kotlin
// In build.gradle.kts
plugins {
    id("app.cash.paparazzi") version "1.3.1"
}
```

**Test Implementation:**
```kotlin
class AnimatedContainerVisualTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfigurations.PIXEL_6,
        theme = "android:Theme.Material3.Light.NoActionBar",
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false
    )

    @Test
    fun `AnimatedContainer default state`() {
        paparazzi.snapshot {
            AnimatedContainer(
                width = 200.dp,
                height = 100.dp,
                backgroundColor = Color.Blue
            )
        }
    }

    @Test
    fun `AnimatedContainer dark theme`() {
        // Configure dark theme
        paparazzi.snapshot {
            // Component in dark theme
        }
    }

    // ... 6 more scenarios
}
```

**Screenshot Location:**
```
src/test/snapshots/images/
  com.augmentalis.avaelements.flutter.visual/
    AnimatedContainerVisualTest/
      AnimatedContainer_default_state.png
      AnimatedContainer_dark_theme.png
      ...
```

### iOS (Swift Snapshot Testing)

**Setup:**
```swift
// In Package.swift
dependencies: [
    .package(url: "https://github.com/pointfreeco/swift-snapshot-testing", from: "1.10.0")
]
```

**Test Implementation:**
```swift
import XCTest
import SnapshotTesting

class AnimatedContainerVisualTest: XCTestCase {
    func testAnimatedContainerDefault() {
        let component = AnimatedContainer(
            width: 200,
            height: 100,
            backgroundColor: .blue
        )

        assertSnapshot(
            matching: component,
            as: .image,
            named: "default"
        )
    }

    func testAnimatedContainerDark() {
        // Dark theme configuration
    }

    // ... 6 more scenarios
}
```

**Screenshot Location:**
```
__Snapshots__/AnimatedContainerVisualTest/
  testAnimatedContainerDefault.1.png
  testAnimatedContainerDark.1.png
  ...
```

### Web (Playwright)

**Setup:**
```typescript
// playwright.config.ts
export default {
  use: {
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  expect: {
    toHaveScreenshot: {
      maxDiffPixels: 100,
      threshold: 0.2,
    },
  },
};
```

**Test Implementation:**
```typescript
import { test, expect } from '@playwright/test';

test.describe('AnimatedContainer Visual Tests', () => {
  test('default state', async ({ page }) => {
    await page.goto('/components/AnimatedContainer');

    const component = page.locator('[data-testid="animated-container"]');
    await expect(component).toHaveScreenshot('default.png');
  });

  test('dark theme', async ({ page }) => {
    await page.goto('/components/AnimatedContainer?theme=dark');

    const component = page.locator('[data-testid="animated-container"]');
    await expect(component).toHaveScreenshot('dark.png');
  });

  // ... 6 more scenarios
});
```

**Screenshot Location:**
```
tests/__screenshots__/AnimatedContainer-Visual-Tests/
  default-state-chromium-darwin.png
  dark-theme-chromium-darwin.png
  ...
```

### Desktop (Compose Desktop)

**Setup:**
```kotlin
// In build.gradle.kts
dependencies {
    testImplementation("org.jetbrains.compose.ui:ui-test-junit4:1.5.0")
}
```

**Test Implementation:**
```kotlin
class AnimatedContainerVisualTest {
    @Test
    fun `AnimatedContainer default state`() = runComposeUiTest {
        setContent {
            AnimatedContainer(
                width = 200.dp,
                height = 100.dp,
                backgroundColor = Color.Blue
            )
        }

        val screenshot = captureToImage()
        screenshot.saveToFile("AnimatedContainer_default.png")

        // Compare with baseline
        val baseline = loadBaseline("AnimatedContainer_default.png")
        val difference = compareImages(screenshot, baseline)
        assertTrue(difference < 0.001, "Screenshot differs by $difference")
    }

    // ... 7 more scenarios
}
```

**Screenshot Location:**
```
src/desktopTest/snapshots/
  AnimatedContainerVisualTest/
    AnimatedContainer_default.png
    AnimatedContainer_dark.png
    ...
```

---

## SECTION 5: BASELINE MANAGEMENT

### Creating Baselines

**First Time Setup:**
1. Run tests on reference device/browser
2. Manually review all screenshots
3. Approve and commit to git
4. Tag as baseline version (e.g., `baseline-v1.0`)

**Command:**
```bash
# Android
./gradlew recordPaparazziDebug

# iOS
swift test --enable-code-coverage --generate-snapshots

# Web
npx playwright test --update-snapshots

# Desktop
./gradlew recordDesktopSnapshots
```

### Updating Baselines

**When to Update:**
- Intentional UI changes
- Design system updates
- Platform API updates
- Bug fixes affecting appearance

**Process:**
1. Review all screenshot differences
2. Verify changes are intentional
3. Update baselines
4. Update version tag
5. Document changes in changelog

**Command:**
```bash
# Update specific component
./gradlew updateBaseline --component=AnimatedContainer

# Update all baselines
./gradlew updateAllBaselines
```

### Baseline Storage

**Location:** Git LFS (Large File Storage)
**Naming Convention:** `{component}_{scenario}_{platform}.png`
**Versioning:** Git tags for each baseline version

```
.baselines/
  v1.0/
    android/
      AnimatedContainer_default.png
      AnimatedContainer_dark.png
    ios/
      AnimatedContainer_default.png
    web/
      AnimatedContainer_default.png
    desktop/
      AnimatedContainer_default.png
  v1.1/
    ...
```

---

## SECTION 6: CI/CD INTEGRATION

### Automated Visual Testing Pipeline

```yaml
name: Visual Regression Tests

on: [push, pull_request]

jobs:
  android-visual-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          lfs: true
      - name: Run Paparazzi tests
        run: ./gradlew verifyPaparazziDebug
      - name: Upload screenshots
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: android-visual-diffs
          path: |
            **/build/paparazzi/failures/**
            **/build/paparazzi/delta/**

  ios-visual-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
        with:
          lfs: true
      - name: Run snapshot tests
        run: swift test
      - name: Upload screenshots
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: ios-visual-diffs
          path: __Snapshots__/**

  web-visual-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          lfs: true
      - name: Install dependencies
        run: npm ci
      - name: Install Playwright
        run: npx playwright install
      - name: Run Playwright tests
        run: npx playwright test
      - name: Upload screenshots
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: web-visual-diffs
          path: test-results/**

  desktop-visual-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          lfs: true
      - name: Run Desktop snapshot tests
        run: ./gradlew verifyDesktopSnapshots
      - name: Upload screenshots
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: desktop-visual-diffs
          path: src/desktopTest/snapshots/failures/**

  visual-report:
    needs: [android-visual-tests, ios-visual-tests, web-visual-tests, desktop-visual-tests]
    runs-on: ubuntu-latest
    if: always()
    steps:
      - name: Download all diffs
        uses: actions/download-artifact@v3
      - name: Generate visual report
        run: ./scripts/generate-visual-report.sh
      - name: Comment on PR
        uses: actions/github-script@v6
        with:
          script: |
            const report = require('./visual-report.json');
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: generateCommentBody(report)
            });
```

---

## SECTION 7: FAILURE ANALYSIS

### Visual Diff Report Format

```markdown
# Visual Regression Report

## Summary
- **Total Tests:** 1,856
- **Passed:** 1,820 (98.1%)
- **Failed:** 36 (1.9%)

## Failed Tests by Platform

### Android (12 failures)
| Component | Scenario | Difference | Reason |
|-----------|----------|------------|--------|
| AnimatedContainer | Dark | 2.3% | Background color mismatch |
| FilterChip | Hover | 0.5% | Ripple timing |
...

### iOS (8 failures)
...

### Web (10 failures)
...

### Desktop (6 failures)
...

## Detailed Diffs

### AnimatedContainer - Dark Mode
**Difference:** 2.3% (above 0.1% threshold)
**Affected Pixels:** 4,968 / 216,000

**Side-by-Side Comparison:**
[Baseline] [Actual] [Diff Overlay]

**Analysis:**
Background color changed from #1E1E1E to #212121 (subtle shade difference)

**Action Required:**
- [ ] Verify design intent
- [ ] Update baseline if intentional
- [ ] Fix bug if unintentional
```

---

## SECTION 8: METRICS & REPORTING

### Visual Consistency Metrics

**Per Component:**
```json
{
  "component": "AnimatedContainer",
  "platforms": {
    "android": {
      "scenarios": 8,
      "passed": 8,
      "failed": 0,
      "avgDifference": 0.0003
    },
    "ios": {
      "scenarios": 8,
      "passed": 7,
      "failed": 1,
      "avgDifference": 0.015
    },
    "web": {
      "scenarios": 8,
      "passed": 8,
      "failed": 0,
      "avgDifference": 0.0001
    },
    "desktop": {
      "scenarios": 8,
      "passed": 8,
      "failed": 0,
      "avgDifference": 0.0002
    }
  },
  "overallConsistency": 96.9%
}
```

**Overall Dashboard:**
- Total visual consistency: 98.1%
- Perfect consistency (0% diff): 87.3%
- Acceptable consistency (< 0.1%): 10.8%
- Failed consistency (> 0.1%): 1.9%

---

## SECTION 9: BEST PRACTICES

### Do's

✅ **Use deterministic data** - Fixed dates, names, numbers
✅ **Disable animations for snapshots** - Capture static state
✅ **Use reference devices** - Consistent hardware/browsers
✅ **Review failures manually** - Don't auto-approve
✅ **Version baselines** - Git tags for each release
✅ **Document changes** - Why baseline was updated

### Don'ts

❌ **Don't use random data** - Timestamps, UUIDs, dynamic content
❌ **Don't snapshot animations mid-frame** - Use start/end states
❌ **Don't ignore small differences** - They add up
❌ **Don't update baselines without review** - Always manual approval
❌ **Don't test in production** - Use controlled environments

---

## SECTION 10: TROUBLESHOOTING

### Common Issues

**Issue:** Flaky tests (random failures)
**Cause:** Non-deterministic rendering, timing issues
**Solution:** Add `waitForIdle()`, use fixed data, disable animations

**Issue:** Platform rendering differences
**Cause:** Different font rendering, anti-aliasing
**Solution:** Use SSIM instead of pixel-perfect, allow platform-specific baselines

**Issue:** Screenshot size mismatch
**Cause:** Different screen densities, viewport sizes
**Solution:** Normalize screenshots to fixed size, use logical pixels

**Issue:** Color space differences
**Cause:** sRGB vs Display P3, HDR
**Solution:** Convert to sRGB before comparison

---

**END OF VISUAL CONSISTENCY FRAMEWORK**

**Status:** Design Complete, Implementation Pending
**Next Steps:**
1. Set up Paparazzi for Android (expand from 4 to 58 components)
2. Set up Swift Snapshot Testing for iOS
3. Set up Playwright for Web
4. Set up Compose Desktop screenshots
5. Create baseline screenshots for all platforms
6. Integrate into CI/CD pipeline

**Maintainer:** Agent 4 - Cross-Platform Testing Specialist
