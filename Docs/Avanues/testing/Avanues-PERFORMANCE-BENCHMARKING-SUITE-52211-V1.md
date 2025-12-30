# Performance Benchmarking Suite
**Cross-Platform Performance Validation & Regression Detection**

**Author:** Agent 4 - Cross-Platform Testing Specialist
**Date:** 2025-11-22
**Status:** Framework Design Complete

---

## EXECUTIVE SUMMARY

### Purpose

Ensure consistent 60 FPS performance of all Flutter-parity components across all 4 platforms with automated benchmarking and regression detection.

### Performance Targets

| Metric | Target | Critical Threshold |
|--------|--------|--------------------|
| **Frame Rate** | ≥ 60 FPS | < 55 FPS = Fail |
| **Avg Frame Time** | ≤ 16.67ms | > 20ms = Fail |
| **Max Frame Time** | ≤ 33ms (2 frames) | > 50ms = Fail |
| **Memory Usage** | ≤ 50 MB per component | > 100 MB = Fail |
| **CPU Usage** | ≤ 20% during interaction | > 40% = Fail |
| **Jank Frames** | 0% | > 1% = Fail |

### Scope

- **Components:** 58 Flutter-parity components
- **Platforms:** 4 (Android, iOS, Web, Desktop)
- **Benchmarks:** 5 per component (rendering, animation, scrolling, interaction, memory)
- **Total Benchmarks:** 58 × 4 × 5 = **1,160 performance tests**

---

## SECTION 1: BENCHMARK CATEGORIES

### 1.1 Rendering Performance

**What:** Time to first render (cold start)
**Target:** < 100ms for simple components, < 200ms for complex
**Measurement:** Time from creation to first paint

**Test Implementation:**
```kotlin
@Test
fun `AnimatedContainer rendering performance`() {
    val benchmark = measurePerformance {
        composeTestRule.setContent {
            AnimatedContainer(
                width = 200.dp,
                height = 100.dp,
                backgroundColor = Color.Blue
            )
        }
        composeTestRule.waitForIdle()
    }

    assertTrue(benchmark.avgFrameTime < 100.milliseconds)
}
```

### 1.2 Animation Performance

**What:** Frame rate during animation (60 FPS target)
**Target:** ≥ 60 FPS, ≤ 16.67ms per frame
**Measurement:** Frame timing during full animation cycle

**Test Implementation:**
```kotlin
@Test
fun `AnimatedContainer animation smoothness`() {
    var animating = false

    composeTestRule.setContent {
        var expanded by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100) // Let UI settle
            expanded = true
            animating = true
            delay(1000) // 1 second animation
            animating = false
        }

        AnimatedContainer(
            width = if (expanded) 400.dp else 200.dp,
            height = if (expanded) 200.dp else 100.dp,
            backgroundColor = if (expanded) Color.Red else Color.Blue
        )
    }

    // Measure frames during animation
    val metrics = measureAnimationPerformance(durationMs = 1000)

    assertTrue(metrics.frameRate >= 60.0, "FPS: ${metrics.frameRate}")
    assertTrue(metrics.jankPercentage < 1.0, "Jank: ${metrics.jankPercentage}%")
}
```

### 1.3 Scrolling Performance

**What:** Frame rate during scrolling (for list components)
**Target:** ≥ 60 FPS while scrolling
**Measurement:** Frame timing during scroll gesture

**Test Implementation:**
```kotlin
@Test
fun `ListViewBuilder scrolling performance`() {
    composeTestRule.setContent {
        ListViewBuilder(
            itemCount = 1000,
            itemBuilder = { index ->
                ListTile(
                    title = "Item $index",
                    subtitle = "Description for item $index"
                )
            }
        )
    }

    // Measure performance during scroll
    val metrics = measureScrollPerformance {
        composeTestRule.onNodeWithTag("list")
            .performScrollToIndex(500)
    }

    assertTrue(metrics.frameRate >= 60.0)
    assertTrue(metrics.avgFrameTime < 16.67.milliseconds)
}
```

### 1.4 Interaction Performance

**What:** Response time to user input (touch, click, hover)
**Target:** < 100ms response time
**Measurement:** Time from input to state change

**Test Implementation:**
```kotlin
@Test
fun `FilterChip interaction latency`() {
    var selected = false
    var interactionTime = 0L

    composeTestRule.setContent {
        FilterChip(
            selected = selected,
            onClick = {
                interactionTime = System.currentTimeMillis()
                selected = !selected
            },
            label = "Test Chip"
        )
    }

    val startTime = System.currentTimeMillis()
    composeTestRule.onNodeWithText("Test Chip").performClick()
    composeTestRule.waitForIdle()

    val latency = interactionTime - startTime
    assertTrue(latency < 100, "Interaction latency: ${latency}ms")
}
```

### 1.5 Memory Performance

**What:** Memory usage during component lifecycle
**Target:** < 50 MB per component, no leaks
**Measurement:** Heap allocation, garbage collection pressure

**Test Implementation:**
```kotlin
@Test
fun `AnimatedContainer memory usage`() {
    val runtime = Runtime.getRuntime()
    val initialMemory = runtime.totalMemory() - runtime.freeMemory()

    repeat(100) {
        composeTestRule.setContent {
            AnimatedContainer(
                width = 200.dp,
                height = 100.dp,
                backgroundColor = Color.Blue
            )
        }
        composeTestRule.waitForIdle()
    }

    runtime.gc()
    Thread.sleep(1000)

    val finalMemory = runtime.totalMemory() - runtime.freeMemory()
    val leaked = finalMemory - initialMemory

    assertTrue(leaked < 50 * 1024 * 1024, "Memory leak: ${leaked / 1024 / 1024} MB")
}
```

---

## SECTION 2: PLATFORM-SPECIFIC BENCHMARKING

### Android Benchmarking

**Tools:**
- Jetpack Benchmark library
- Android Profiler
- Perfetto traces
- Compose performance tools

**Setup:**
```kotlin
// build.gradle.kts
dependencies {
    androidTestImplementation("androidx.benchmark:benchmark-junit4:1.2.2")
    androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.2.2")
}
```

**Microbenchmark Example:**
```kotlin
@RunWith(AndroidJUnit4::class)
class AnimatedContainerBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun animatedContainerCreation() = benchmarkRule.measureRepeated {
        runWithTimingDisabled {
            // Setup
        }

        // Measure this block
        AndroidView(factory = { context ->
            ComposeView(context).apply {
                setContent {
                    AnimatedContainer(
                        width = 200.dp,
                        height = 100.dp
                    )
                }
            }
        })
    }
}
```

**Macrobenchmark Example:**
```kotlin
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollListView() = benchmarkRule.measureRepeated(
        packageName = "com.augmentalis.avaelements",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()

        val list = device.findObject(By.res("list-view"))
        list.fling(Direction.DOWN)
        device.waitForIdle()
    }
}
```

### iOS Benchmarking

**Tools:**
- XCTest Performance APIs
- Instruments (Time Profiler, Allocations)
- MetricKit

**Setup:**
```swift
import XCTest

class AnimatedContainerBenchmark: XCTestCase {
    func testAnimatedContainerCreation() {
        measure(metrics: [XCTClockMetric(), XCTMemoryMetric()]) {
            let component = AnimatedContainer(
                width: 200,
                height: 100,
                backgroundColor: .blue
            )
            // Render component
        }
    }

    func testAnimationPerformance() {
        let options = XCTMeasureOptions()
        options.iterationCount = 10

        measure(metrics: [XCTClockMetric()], options: options) {
            // Run animation
        }
    }
}
```

### Web Benchmarking

**Tools:**
- Lighthouse
- Chrome DevTools Performance
- WebPageTest
- Playwright Performance API

**Setup:**
```typescript
import { test } from '@playwright/test';

test('AnimatedContainer rendering performance', async ({ page }) => {
  await page.goto('/components/AnimatedContainer');

  const metrics = await page.evaluate(() => {
    const start = performance.now();

    // Render component
    const container = document.querySelector('[data-testid="animated-container"]');

    const end = performance.now();
    return {
      renderTime: end - start,
      memory: (performance as any).memory?.usedJSHeapSize
    };
  });

  expect(metrics.renderTime).toBeLessThan(100);
});

test('Animation FPS', async ({ page }) => {
  await page.goto('/components/AnimatedContainer');

  // Start frame recording
  await page.evaluate(() => {
    (window as any).frameCount = 0;
    (window as any).startTime = performance.now();

    function countFrame() {
      (window as any).frameCount++;
      requestAnimationFrame(countFrame);
    }
    requestAnimationFrame(countFrame);
  });

  // Trigger animation
  await page.click('[data-testid="animate-button"]');
  await page.waitForTimeout(1000);

  const fps = await page.evaluate(() => {
    const duration = performance.now() - (window as any).startTime;
    return ((window as any).frameCount / duration) * 1000;
  });

  expect(fps).toBeGreaterThanOrEqual(60);
});
```

### Desktop Benchmarking

**Tools:**
- Compose Desktop profiling
- JVM profilers (JProfiler, YourKit)
- Custom performance utilities

**Setup:**
```kotlin
@Test
fun animatedContainerDesktopPerformance() = runDesktopTest {
    val runtime = Runtime.getRuntime()
    val metrics = mutableListOf<Long>()

    repeat(60) {
        val start = System.nanoTime()

        setContent {
            AnimatedContainer(
                width = 200.dp,
                height = 100.dp
            )
        }
        awaitIdle()

        val end = System.nanoTime()
        metrics.add(end - start)
    }

    val avgFrameTime = metrics.average() / 1_000_000 // Convert to ms
    assertTrue(avgFrameTime < 16.67, "Avg frame time: ${avgFrameTime}ms")
}
```

---

## SECTION 3: PERFORMANCE REGRESSION DETECTION

### Baseline Establishment

**Initial Benchmarks:**
1. Run benchmarks on reference hardware
2. Record baseline metrics for each component
3. Store in version-controlled JSON file
4. Tag as baseline version (e.g., `perf-baseline-v1.0`)

**Baseline Format:**
```json
{
  "version": "1.0.0",
  "platform": "android",
  "device": "Pixel 6",
  "date": "2025-11-22",
  "components": {
    "AnimatedContainer": {
      "rendering": {
        "avgMs": 45.2,
        "p50Ms": 42.1,
        "p90Ms": 58.3,
        "p99Ms": 72.1
      },
      "animation": {
        "fps": 60.0,
        "avgFrameMs": 16.5,
        "maxFrameMs": 19.2,
        "jankPercent": 0.0
      },
      "memory": {
        "avgMb": 12.4,
        "peakMb": 18.7
      }
    }
  }
}
```

### Regression Detection

**Thresholds:**
- **Warning:** 10% slower than baseline
- **Failure:** 25% slower than baseline

**Comparison Logic:**
```kotlin
data class RegressionResult(
    val component: String,
    val metric: String,
    val baseline: Double,
    val current: Double,
    val change: Double,
    val severity: Severity
)

enum class Severity {
    IMPROVEMENT, // Faster than baseline
    ACCEPTABLE,  // Within 10% of baseline
    WARNING,     // 10-25% slower
    CRITICAL     // >25% slower
}

fun detectRegression(
    baseline: PerformanceMetrics,
    current: PerformanceMetrics
): RegressionResult {
    val change = ((current.avgFrameTime - baseline.avgFrameTime) / baseline.avgFrameTime) * 100

    val severity = when {
        change < -5.0 -> Severity.IMPROVEMENT
        change < 10.0 -> Severity.ACCEPTABLE
        change < 25.0 -> Severity.WARNING
        else -> Severity.CRITICAL
    }

    return RegressionResult(
        component = "AnimatedContainer",
        metric = "avgFrameTime",
        baseline = baseline.avgFrameTime.inWholeMilliseconds.toDouble(),
        current = current.avgFrameTime.inWholeMilliseconds.toDouble(),
        change = change,
        severity = severity
    )
}
```

### Automated Alerts

**CI/CD Integration:**
```yaml
- name: Run performance benchmarks
  run: ./gradlew benchmark

- name: Compare with baseline
  run: ./scripts/compare-performance.sh

- name: Post results to PR
  if: github.event_name == 'pull_request'
  run: |
    ./scripts/generate-perf-comment.sh > perf-comment.md
    gh pr comment ${{ github.event.pull_request.number }} --body-file perf-comment.md

- name: Fail on critical regression
  run: |
    if grep -q "CRITICAL" perf-results.json; then
      echo "::error::Critical performance regression detected"
      exit 1
    fi
```

---

## SECTION 4: PERFORMANCE OPTIMIZATION GUIDELINES

### Common Performance Issues

**Issue 1: Excessive Recomposition**
**Symptom:** Choppy animations, high CPU usage
**Detection:** Recomposition counters > 1000/second
**Fix:**
```kotlin
// Bad - recomposes entire tree
@Composable
fun SlowComponent(data: List<Item>) {
    Column {
        data.forEach { item ->
            Text(item.text) // Each text recomposes
        }
    }
}

// Good - minimizes recomposition
@Composable
fun FastComponent(data: List<Item>) {
    LazyColumn {
        items(data) { item ->
            ItemRow(item) // Only changed items recompose
        }
    }
}

@Composable
fun ItemRow(item: Item) {
    Text(item.text)
}
```

**Issue 2: Heavy Computations in Composition**
**Symptom:** Slow rendering, frame drops
**Detection:** Composition time > 10ms
**Fix:**
```kotlin
// Bad - computation in composition
@Composable
fun SlowComponent(data: List<Item>) {
    val processed = data.map { heavyComputation(it) } // Runs every recomposition!
    Text(processed.joinToString())
}

// Good - memoize computation
@Composable
fun FastComponent(data: List<Item>) {
    val processed = remember(data) {
        data.map { heavyComputation(it) }
    }
    Text(processed.joinToString())
}
```

**Issue 3: Unoptimized Lists**
**Symptom:** Scroll jank, memory issues
**Detection:** Frame time > 16.67ms during scroll
**Fix:**
```kotlin
// Bad - creates all items upfront
Column {
    items.forEach { item ->
        ItemRow(item)
    }
}

// Good - lazy loading
LazyColumn {
    items(items) { item ->
        ItemRow(item)
    }
}
```

### Performance Best Practices

✅ **Use `LazyColumn/LazyRow`** for long lists
✅ **Memoize expensive calculations** with `remember`
✅ **Derive state carefully** with `derivedStateOf`
✅ **Key your lists properly** for efficient diffing
✅ **Avoid unnecessary recomposition** with `Modifier.drawBehind`
✅ **Profile before optimizing** - measure first!

---

## SECTION 5: BENCHMARK REPORTING

### Performance Report Format

```markdown
# Performance Benchmark Report

**Date:** 2025-11-22
**Commit:** abc123
**Platform:** Android
**Device:** Pixel 6

## Summary

- **Total Components:** 58
- **Passed:** 52 (89.7%)
- **Warnings:** 4 (6.9%)
- **Failures:** 2 (3.4%)

## Regressions Detected

### Critical (2)

| Component | Metric | Baseline | Current | Change | Impact |
|-----------|--------|----------|---------|--------|--------|
| ReorderableListView | Avg Frame Time | 15.2ms | 22.8ms | +50% | Scroll jank |
| AnimatedList | FPS | 60.0 | 48.2 | -20% | Choppy animation |

### Warnings (4)

| Component | Metric | Baseline | Current | Change |
|-----------|--------|----------|---------|--------|
| GridViewBuilder | Render Time | 85ms | 98ms | +15% |
| PageView | Memory | 32MB | 38MB | +19% |

## Improvements (8)

| Component | Metric | Baseline | Current | Change |
|-----------|--------|----------|---------|--------|
| AnimatedOpacity | Avg Frame Time | 16.2ms | 14.1ms | -13% |
| FilterChip | Interaction Latency | 45ms | 32ms | -29% |

## Platform Comparison

| Platform | Avg FPS | Avg Frame Time | Components Passing |
|----------|---------|----------------|-------------------|
| Android | 59.8 | 16.8ms | 52/58 (89.7%) |
| iOS | 60.0 | 16.5ms | 55/58 (94.8%) |
| Web | 57.2 | 17.5ms | 48/58 (82.8%) |
| Desktop | 60.0 | 16.3ms | 56/58 (96.6%) |

**Best Performer:** Desktop (96.6% pass rate)
**Needs Work:** Web (82.8% pass rate)
```

### JSON Export Format

```json
{
  "timestamp": "2025-11-22T10:30:00Z",
  "commit": "abc123",
  "platform": "android",
  "device": "Pixel 6",
  "summary": {
    "totalComponents": 58,
    "passed": 52,
    "warnings": 4,
    "failures": 2,
    "passRate": 89.7
  },
  "components": [
    {
      "name": "AnimatedContainer",
      "status": "passed",
      "metrics": {
        "rendering": {
          "avgMs": 45.2,
          "baseline": 42.1,
          "change": 7.4,
          "severity": "acceptable"
        },
        "animation": {
          "fps": 60.0,
          "baseline": 60.0,
          "change": 0.0,
          "severity": "acceptable"
        }
      }
    }
  ],
  "platformComparison": {
    "android": {"avgFps": 59.8, "passRate": 89.7},
    "ios": {"avgFps": 60.0, "passRate": 94.8},
    "web": {"avgFps": 57.2, "passRate": 82.8},
    "desktop": {"avgFps": 60.0, "passRate": 96.6}
  }
}
```

---

## SECTION 6: CI/CD INTEGRATION

### Performance Testing Pipeline

```yaml
name: Performance Benchmarks

on:
  push:
    branches: [main]
  pull_request:

jobs:
  android-benchmarks:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Android benchmarks
        run: ./gradlew connectedAndroidTest
      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: android-perf-results
          path: build/outputs/androidTest-results/**

  ios-benchmarks:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run iOS benchmarks
        run: swift test --enable-code-coverage
      - name: Parse results
        run: ./scripts/parse-ios-perf.sh

  web-benchmarks:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Install Lighthouse
        run: npm install -g lighthouse
      - name: Run Lighthouse
        run: lighthouse http://localhost:3000 --output json
      - name: Parse results
        run: ./scripts/parse-lighthouse.sh

  desktop-benchmarks:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Desktop benchmarks
        run: ./gradlew desktopBenchmark

  compare-and-report:
    needs: [android-benchmarks, ios-benchmarks, web-benchmarks, desktop-benchmarks]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Download all results
        uses: actions/download-artifact@v3
      - name: Compare with baseline
        run: ./scripts/compare-performance.sh
      - name: Generate report
        run: ./scripts/generate-perf-report.sh
      - name: Comment on PR
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const report = fs.readFileSync('./perf-report.md', 'utf8');
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: report
            });
      - name: Fail on regression
        run: ./scripts/check-regression.sh
```

---

**END OF PERFORMANCE BENCHMARKING SUITE**

**Status:** Framework Design Complete
**Total Benchmarks:** 1,160 (58 components × 4 platforms × 5 metrics)
**Target:** 100% of components meet 60 FPS on all platforms

**Next Steps:**
1. Implement benchmarks for all 58 components on Android
2. Port benchmarks to iOS, Web, Desktop
3. Establish performance baselines
4. Integrate into CI/CD pipeline
5. Set up automated regression detection
6. Create performance dashboard

**Maintainer:** Agent 4 - Cross-Platform Testing Specialist
