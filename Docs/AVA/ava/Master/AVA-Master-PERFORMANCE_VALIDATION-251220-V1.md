# AVA AI Performance Validation Guide

## Overview

This document provides comprehensive guidelines for validating AVA AI's performance against defined budgets. Performance validation ensures the system meets real-time responsiveness requirements for a high-quality user experience.

## Performance Budgets

### NLU Pipeline

| Component | Target | Maximum | Notes |
|-----------|--------|---------|-------|
| Tokenization | < 5ms | 10ms | BERT WordPiece |
| Model Inference | < 50ms | 100ms | MobileBERT INT8 with NNAPI |
| **Total NLU** | **< 60ms** | **120ms** | End-to-end classification |

### Database Operations

| Operation | Target | Maximum | Test Size |
|-----------|--------|---------|-----------|
| Insert Message | < 10ms | 20ms | Single message |
| Bulk Insert | < 500ms | 1000ms | 1000 messages |
| Query Messages | < 100ms | 200ms | 100 messages with pagination |
| Hash Lookup | < 10ms | 20ms | Training example deduplication |
| Memory Search | < 50ms | 100ms | Full-text search |

### UI Performance

| Metric | Target | Maximum | Notes |
|--------|--------|---------|-------|
| Screen Load | < 300ms | 500ms | Cold start to interactive |
| List Scroll | 60 FPS | 30 FPS | Smooth scrolling |
| Dialog Open | < 100ms | 200ms | Add/Edit example dialogs |
| Model Download | N/A | N/A | Network dependent, show progress |

### Memory Budgets

| Component | Target | Maximum | Notes |
|-----------|--------|---------|-------|
| App Baseline | < 80MB | 120MB | Without model |
| Model Loaded | < 150MB | 200MB | With MobileBERT in memory |
| Peak Usage | < 200MB | 300MB | During inference |

## Validation Methodology

### 1. NLU Performance Testing

#### Setup

```bash
# Run on physical device (not emulator) for accurate timing
adb devices

# Install app in release mode
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

#### Integration Tests

```bash
# Run NLU integration tests
./gradlew :features:nlu:connectedAndroidTest

# Filter for performance tests only
./gradlew :features:nlu:connectedAndroidTest \
  --tests "*performance*"
```

#### Manual Validation

1. Navigate to Teach-Ava screen
2. Add 20+ training examples across 5 intents
3. Test classification with logcat timing:

```bash
adb logcat | grep "IntentClassifier"
```

Look for log lines:
```
IntentClassifier: Tokenization: 3ms
IntentClassifier: Inference: 47ms
IntentClassifier: Total: 50ms
```

#### Expected Results

✅ **Pass Criteria:**
- Tokenization: < 5ms on 95% of samples
- Inference: < 50ms on 95% of samples
- Total: < 60ms on 90% of samples

❌ **Fail Criteria:**
- Any operation > 2x target (> 120ms total)
- Consistent failures to meet budget

### 2. Database Performance Testing

#### Repository Tests

```bash
# Run all repository tests
./gradlew :core:data:testDebugUnitTest

# Specific performance tests
./gradlew :core:data:testDebugUnitTest \
  --tests "*Performance*"
```

#### Instrumented Tests

```bash
# Run database instrumentation tests
./gradlew :core:data:connectedAndroidTest
```

#### Benchmark Suite

Create manual benchmark:

```kotlin
@Test
fun benchmark_insertMessages() = runTest {
    val messages = List(1000) { index ->
        createTestMessage(id = index.toLong())
    }

    val elapsed = measureTimeMillis {
        messages.forEach { message ->
            repository.insertMessage(message)
        }
    }

    println("Insert 1000 messages: ${elapsed}ms")
    assertTrue(elapsed < 500, "Expected < 500ms, actual: ${elapsed}ms")
}
```

#### Expected Results

✅ **Pass Criteria:**
- Insert 1000 messages: < 500ms
- Query 100 messages: < 100ms
- Hash lookup: < 10ms

### 3. UI Performance Testing

#### Compose Performance Profiler

1. Open Android Studio Profiler
2. Select CPU profiling
3. Navigate through UI flows
4. Analyze frame timing

**Key Metrics:**
- Frame time: < 16ms (60 FPS)
- Janky frames: < 5% of total

#### Systrace Analysis

```bash
# Capture systrace
python systrace.py --time=10 -o trace.html sched gfx view

# Open trace.html in Chrome
chrome trace.html
```

**Look for:**
- UI thread blocking: Should be < 16ms
- Dropped frames: Highlight stutters
- Memory allocations: Minimize during scroll

#### Manual UI Testing Checklist

- [ ] Teach-Ava list scrolls smoothly with 50+ examples
- [ ] Add Example dialog opens < 100ms
- [ ] Training example cards render without flicker
- [ ] Filter bottom sheet animates smoothly
- [ ] Model download shows progress updates
- [ ] No visible lag when typing in text fields

### 4. Memory Profiling

#### Android Studio Memory Profiler

1. Open Memory Profiler
2. Perform these actions:
   - Download model
   - Initialize classifier
   - Run 20 classifications
   - Navigate between screens
3. Force GC and check for leaks

**Expected Memory Usage:**
```
Baseline (no model):      60-80MB
Model downloaded:         80-100MB
Model loaded in memory:   120-150MB
Peak during inference:    150-200MB
After GC:                 80-120MB
```

#### LeakCanary

```gradle
debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
```

Install and run app in debug mode. LeakCanary will detect memory leaks automatically.

### 5. Battery & CPU Usage

#### Battery Historian

```bash
# Reset battery stats
adb shell dumpsys batterystats --reset

# Use app for 10 minutes
# ... user testing ...

# Dump battery stats
adb bugreport bugreport.zip

# Upload to https://bathist.ef.lc/
```

**Expected Results:**
- AVA should use < 5% battery per hour of active use
- No wake locks during idle
- CPU usage spikes only during inference (< 50ms)

## Performance Optimization Tips

### NLU Optimization

1. **Enable NNAPI:**
```kotlin
val sessionOptions = OrtSession.SessionOptions().apply {
    addNnapi()  // Hardware acceleration
}
```

2. **Reduce Model Size:**
- Use INT8 quantization (already implemented)
- Consider pruning if accuracy allows

3. **Batch Inference:**
- Process multiple utterances in parallel if needed
- Reuse ONNX session (already implemented with singleton)

### Database Optimization

1. **Use Transactions:**
```kotlin
database.withTransaction {
    messages.forEach { insertMessage(it) }
}
```

2. **Optimize Indices:**
- Review query patterns with EXPLAIN QUERY PLAN
- Add composite indices for common filters
- Remove unused indices

3. **Pagination:**
```kotlin
@Query("SELECT * FROM messages LIMIT :limit OFFSET :offset")
fun getMessagesPaginated(limit: Int, offset: Int): List<MessageEntity>
```

### UI Optimization

1. **LazyColumn Keys:**
```kotlin
LazyColumn {
    items(examples, key = { it.id }) { example ->
        TrainingExampleCard(example)
    }
}
```

2. **Remember Expensive Computations:**
```kotlin
val sortedExamples = remember(examples) {
    examples.sortedByDescending { it.usageCount }
}
```

3. **Avoid Recomposition:**
```kotlin
@Stable
data class TrainExample(...)  // Mark data classes as Stable
```

## Continuous Monitoring

### Automated Performance Tests

Add to CI/CD pipeline:

```yaml
# .github/workflows/performance.yml
name: Performance Tests

on: [pull_request]

jobs:
  performance:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run performance tests
        run: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*Performance*
      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: performance-results
          path: app/build/reports/androidTests/
```

### Firebase Performance Monitoring

```gradle
implementation 'com.google.firebase:firebase-perf:20.5.1'
```

```kotlin
val trace = Firebase.performance.newTrace("intent_classification")
trace.start()
// ... classification logic ...
trace.stop()
```

### Custom Metrics

```kotlin
object PerformanceMonitor {
    fun recordInferenceTime(timeMs: Long) {
        if (timeMs > 100) {
            Log.w("Performance", "Slow inference: ${timeMs}ms")
            // Report to analytics
        }
    }
}
```

## Regression Testing

### Performance Baseline

Establish baseline metrics:

```kotlin
class PerformanceBaseline {
    companion object {
        const val TOKENIZATION_MS = 3L
        const val INFERENCE_MS = 45L
        const val DB_INSERT_1K_MS = 300L
        const val DB_QUERY_100_MS = 40L
    }
}
```

### Regression Detection

```kotlin
@Test
fun testNoPerformanceRegression() {
    val actual = measureInferenceTime()
    val baseline = PerformanceBaseline.INFERENCE_MS
    val tolerance = 1.2  // 20% tolerance

    assertTrue(
        actual < baseline * tolerance,
        "Performance regression: ${actual}ms vs baseline ${baseline}ms"
    )
}
```

## Troubleshooting

### Slow Inference (> 100ms)

**Possible Causes:**
- NNAPI not enabled → Check OrtSession options
- CPU fallback → Verify hardware support
- Large input → Verify tokenization truncation
- Memory pressure → Check for leaks

**Debug Steps:**
```kotlin
Log.d("Performance", "NNAPI available: ${ortSession.hasNnapi()}")
Log.d("Performance", "Input size: ${inputIds.size}")
Log.d("Performance", "Memory: ${Runtime.getRuntime().totalMemory() / 1_000_000}MB")
```

### Database Slow Queries

**Possible Causes:**
- Missing index → Add index on frequently queried columns
- Large result set → Implement pagination
- No transaction → Wrap bulk operations in transaction

**Debug Steps:**
```sql
EXPLAIN QUERY PLAN SELECT * FROM messages WHERE conversation_id = ?
```

### UI Jank

**Possible Causes:**
- Heavy computation on main thread → Move to viewModelScope
- Unnecessary recomposition → Use remember/derivedStateOf
- Large list without LazyColumn → Implement lazy loading

**Debug Steps:**
```kotlin
// Enable composition tracing
androidx.compose.ui.tooling.PreviewActivity
```

## Performance Report Template

```markdown
## Performance Validation Report

**Date:** YYYY-MM-DD
**Device:** Pixel 7 (Android 14)
**Build:** Release (v1.0.0)

### NLU Performance
| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Tokenization | < 5ms | 3.2ms | ✅ |
| Inference | < 50ms | 42ms | ✅ |
| Total E2E | < 60ms | 45ms | ✅ |

### Database Performance
| Operation | Target | Actual | Status |
|-----------|--------|--------|--------|
| Insert 1K | < 500ms | 298ms | ✅ |
| Query 100 | < 100ms | 38ms | ✅ |

### Memory Usage
| State | Target | Actual | Status |
|-------|--------|--------|--------|
| Baseline | < 80MB | 72MB | ✅ |
| Model Loaded | < 150MB | 135MB | ✅ |

### Recommendations
- None. All metrics within budget.

**Validated by:** [Name]
**Approved for release:** Yes/No
```

## References

- [Android Performance Best Practices](https://developer.android.com/topic/performance)
- [Jetpack Compose Performance](https://developer.android.com/jetpack/compose/performance)
- [ONNX Runtime Optimization](https://onnxruntime.ai/docs/performance)
- [Room Performance](https://developer.android.com/training/data-storage/room/performance)

---

**Last Updated:** 2025-01-XX
**Next Review:** Every major release
**Owner:** Development Team
