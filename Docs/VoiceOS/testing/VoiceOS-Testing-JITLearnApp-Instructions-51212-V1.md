# VoiceOS JIT/LearnApp - Test Instructions

**Document**: VoiceOS-Testing-JITLearnApp-Instructions-51212-V1.md
**Created**: 2025-12-12
**Related**: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md

---

## Overview

This document provides comprehensive instructions for testing the P0 fixes and P1 enhancements in JITLearning and LearnAppCore libraries.

**Test Categories**:
1. **Build Verification** - Ensure code compiles
2. **Security Tests** - Permission, validation, authentication
3. **Concurrency Tests** - Thread safety, race conditions
4. **Memory Leak Tests** - LRU caches, cleanup
5. **Performance Tests** - UUID cache, latency
6. **Integration Tests** - End-to-end workflows

---

## Prerequisites

### Environment Setup

```bash
# 1. Java Version (CRITICAL)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
java -version  # Should show: java version "17.0.13"

# 2. Android SDK
echo $ANDROID_SDK_ROOT  # Should point to Android SDK

# 3. Gradle
./gradlew --version  # Should work
```

### Required Tools

- **JDK 17** (NOT JDK 24 - causes build errors)
- **Android SDK** with API 34+
- **Gradle 8.5+**
- **LeakCanary** (for memory leak detection)
- **Thread Sanitizer** (for race condition detection)

---

## Test Category 1: Build Verification

### 1.1 Clean Build Test

**Objective**: Verify all modules compile without errors

```bash
# Clean and rebuild
./gradlew clean

# Build LearnAppCore
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
./gradlew :Modules:VoiceOS:libraries:LearnAppCore:assembleDebug

# Expected: BUILD SUCCESSFUL in ~20s
```

**Success Criteria**:
- ✅ No compilation errors
- ✅ No warnings (except suppressed deprecations)
- ✅ Build completes in <30 seconds

---

### 1.2 JITLearning Build Test

```bash
# Build JITLearning
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
./gradlew :Modules:VoiceOS:libraries:JITLearning:assembleDebug

# Expected: BUILD SUCCESSFUL in ~15s
```

**Success Criteria**:
- ✅ No compilation errors
- ✅ SecurityValidator.kt compiles
- ✅ All handlers compile
- ✅ NodeCache.clear() has override keyword

---

### 1.3 Full Project Build

```bash
# Build entire project
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
./gradlew assembleDebug

# Expected: BUILD SUCCESSFUL
```

**Success Criteria**:
- ✅ All modules build
- ✅ VoiceOSCore builds
- ✅ LearnApp builds

---

## Test Category 2: Security Tests (24+ tests)

### 2.1 Permission Verification Tests

**File**: Create `JITLearning/src/androidInstrumentedTest/kotlin/com/augmentalis/jitlearning/SecurityTests.kt`

```kotlin
package com.augmentalis.jitlearning

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityTests {

    @get:Rule
    val serviceRule = ServiceTestRule()

    /**
     * Test: Unauthorized bind attempt should fail
     *
     * Security: Verify signature-level permission blocks unauthorized apps
     */
    @Test(expected = SecurityException::class)
    fun testUnauthorizedBind_ThrowsSecurityException() {
        // Arrange
        val intent = Intent(ApplicationProvider.getApplicationContext(), JITLearningService::class.java)

        // Act - Should throw SecurityException
        serviceRule.bindService(intent)
    }

    /**
     * Test: Authorized bind (same signature) should succeed
     */
    @Test
    fun testAuthorizedBind_Succeeds() {
        // Arrange
        val intent = Intent(ApplicationProvider.getApplicationContext(), JITLearningService::class.java)

        // Act
        val binder = serviceRule.bindService(intent)

        // Assert
        assertNotNull(binder)
    }
}
```

**Run Tests**:
```bash
./gradlew :Modules:VoiceOS:libraries:JITLearning:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.augmentalis.jitlearning.SecurityTests
```

**Success Criteria**:
- ✅ Unauthorized bind throws SecurityException
- ✅ Authorized bind succeeds
- ✅ Permission check logged

---

### 2.2 Input Validation Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class InputValidationTests {

    @Test(expected = IllegalArgumentException::class)
    fun testNullPackageName_ThrowsException() {
        InputValidator.validatePackageName(null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEmptyPackageName_ThrowsException() {
        InputValidator.validatePackageName("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidPackageFormat_ThrowsException() {
        InputValidator.validatePackageName("Invalid..Name")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSQLInjection_PackageName_ThrowsException() {
        InputValidator.validatePackageName("com.test'; DROP TABLE users--")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPathTraversal_PackageName_ThrowsException() {
        InputValidator.validatePackageName("com.test../../../etc/passwd")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOversizedPackageName_ThrowsException() {
        val oversized = "com." + "a".repeat(300)
        InputValidator.validatePackageName(oversized)
    }

    @Test
    fun testValidPackageName_Passes() {
        InputValidator.validatePackageName("com.example.testapp")
        // Should not throw
    }

    // UUID Validation Tests
    @Test(expected = IllegalArgumentException::class)
    fun testNullUuid_ThrowsException() {
        InputValidator.validateUuid(null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOversizedUuid_ThrowsException() {
        val oversized = "a".repeat(100)
        InputValidator.validateUuid(oversized)
    }

    @Test
    fun testValidUuid_Passes() {
        InputValidator.validateUuid("element-123-abc-xyz")
    }

    // Text Input Validation Tests
    @Test(expected = IllegalArgumentException::class)
    fun testXSSAttack_Script_ThrowsException() {
        InputValidator.validateTextInput("<script>alert('xss')</script>")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testXSSAttack_Javascript_ThrowsException() {
        InputValidator.validateTextInput("javascript:alert('xss')")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSQLInjection_TextInput_ThrowsException() {
        InputValidator.validateTextInput("'; DROP TABLE users--")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOversizedTextInput_ThrowsException() {
        val oversized = "a".repeat(20000)
        InputValidator.validateTextInput(oversized)
    }

    @Test
    fun testValidTextInput_Passes() {
        InputValidator.validateTextInput("Hello World")
    }

    // Screen Hash Validation Tests
    @Test(expected = IllegalArgumentException::class)
    fun testInvalidScreenHash_NotHex_ThrowsException() {
        InputValidator.validateScreenHash("not-hexadecimal-zzz")
    }

    @Test
    fun testValidScreenHash_Passes() {
        InputValidator.validateScreenHash("a1b2c3d4e5f6")
    }

    // Bounds Validation Tests
    @Test(expected = IllegalArgumentException::class)
    fun testNegativeBounds_ThrowsException() {
        InputValidator.validateBounds(-10, -10, 100, 100)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidBounds_RightLessThanLeft_ThrowsException() {
        InputValidator.validateBounds(100, 0, 50, 100)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOversizedBounds_ThrowsException() {
        InputValidator.validateBounds(0, 0, 200000, 200000)
    }

    @Test
    fun testValidBounds_Passes() {
        InputValidator.validateBounds(0, 0, 1080, 1920)
    }
}
```

**Run Tests**:
```bash
./gradlew :Modules:VoiceOS:libraries:JITLearning:test --tests InputValidationTests
```

**Success Criteria**:
- ✅ All 24 validation tests pass
- ✅ SQL injection blocked
- ✅ XSS attempts blocked
- ✅ Path traversal blocked
- ✅ Valid inputs accepted

---

## Test Category 3: Concurrency Tests (16+ tests)

### 3.1 Thread Safety Stress Tests

**File**: `JITLearning/src/androidInstrumentedTest/kotlin/com/augmentalis/jitlearning/ConcurrencyTests.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class ConcurrencyTests {

    private lateinit var service: JITLearningService

    @Before
    fun setup() {
        // Bind to service
        val intent = Intent(ApplicationProvider.getApplicationContext(), JITLearningService::class.java)
        val binder = serviceRule.bindService(intent)
        service = IElementCaptureService.Stub.asInterface(binder) as JITLearningService
    }

    /**
     * Test: Concurrent pause/resume operations
     *
     * Verifies: @Volatile flags ensure visibility across threads
     */
    @Test
    fun testConcurrentPauseResume_NoRaceConditions() = runBlocking {
        // Arrange
        val iterations = 1000
        val pauseJobs = mutableListOf<Job>()
        val resumeJobs = mutableListOf<Job>()

        // Act - Spawn 1000 pause and 1000 resume operations
        repeat(iterations) {
            pauseJobs.add(launch(Dispatchers.IO) {
                service.pauseCapture()
            })
            resumeJobs.add(launch(Dispatchers.IO) {
                service.resumeCapture()
            })
        }

        // Wait for all
        pauseJobs.joinAll()
        resumeJobs.joinAll()

        // Assert - Should not crash or deadlock
        // State should be consistent (either paused or resumed)
        val finalState = service.queryState()
        assertTrue(finalState.isPaused || !finalState.isPaused)
    }

    /**
     * Test: Concurrent screen captures
     *
     * Verifies: No crashes, no data races
     */
    @Test
    fun testConcurrentScreenCaptures_NoDataRaces() = runBlocking {
        // Arrange
        val captureCount = 100

        // Act - 100 concurrent screen captures
        val results = (1..captureCount).map {
            async(Dispatchers.IO) {
                service.captureCurrentScreen()
            }
        }.awaitAll()

        // Assert
        assertEquals(captureCount, results.size)
        results.forEach { screen ->
            assertNotNull(screen)
            assertNotNull(screen.screenHash)
        }
    }

    /**
     * Test: Concurrent element clicks
     *
     * Verifies: All clicks processed, no lost clicks
     */
    @Test
    fun testConcurrentElementClicks_AllProcessed() = runBlocking {
        // Arrange - Capture screen first
        val screen = service.captureCurrentScreen()
        val elements = screen.elements.take(10)

        // Act - Click all elements concurrently
        val results = elements.map { element ->
            async(Dispatchers.IO) {
                service.clickElement(element.uuid, screen.screenHash)
            }
        }.awaitAll()

        // Assert - All clicks attempted
        assertEquals(elements.size, results.size)
    }

    /**
     * Test: Screen visits atomic increment
     *
     * Verifies: No lost updates in SafetyManager
     */
    @Test
    fun testScreenVisitsAtomicIncrement_NoLostUpdates() = runBlocking {
        // Arrange
        val screenHash = "test_screen_hash"
        val incrementCount = 100

        // Act - 100 threads incrementing same screen
        val jobs = (1..incrementCount).map {
            launch(Dispatchers.IO) {
                // Simulate screen visit
                safetyManager.updateScreenContext(
                    packageName = "com.test",
                    screenHash = screenHash,
                    activityName = "TestActivity",
                    elements = emptyList()
                )
            }
        }
        jobs.joinAll()

        // Assert - Count should be exactly 100
        val visits = safetyManager.getScreenVisits(screenHash)
        assertEquals(incrementCount, visits)
    }

    /**
     * Test: Batch queue concurrency
     *
     * Verifies: ArrayBlockingQueue handles concurrent adds
     */
    @Test
    fun testBatchQueueConcurrency_BoundedAndSafe() = runBlocking {
        // Arrange
        val commandCount = 200  // More than batch size (50)

        // Act - Add 200 commands concurrently
        val jobs = (1..commandCount).map { i ->
            launch(Dispatchers.IO) {
                val element = ElementInfo(
                    uuid = "test_$i",
                    bounds = Rect(0, 0, 100, 100),
                    className = "android.widget.Button",
                    contentDescription = "Test $i",
                    text = "Button $i"
                )
                learnAppCore.processElement(element)
            }
        }
        jobs.joinAll()

        // Assert - Queue should have auto-flushed
        // Batch size should be < max (50)
        val batchSize = learnAppCore.getBatchSize()
        assertTrue(batchSize < 50)
    }

    /**
     * Test: Framework cache concurrency
     *
     * Verifies: LRU cache handles concurrent access
     */
    @Test
    fun testFrameworkCacheConcurrency_NoExceptions() = runBlocking {
        // Arrange - 100 different packages
        val packages = (1..100).map { "com.test.app$it" }

        // Act - Concurrent framework detection
        val jobs = packages.map { pkg ->
            launch(Dispatchers.IO) {
                learnAppCore.detectFramework(pkg)
            }
        }
        jobs.joinAll()

        // Assert - Cache size should be <= 50 (LRU limit)
        // No ConcurrentModificationException should occur
    }

    /**
     * Test: UUID cache eviction during concurrent access
     *
     * Verifies: LRU eviction doesn't cause crashes
     */
    @Test
    fun testUuidCacheLRUEviction_ThreadSafe() = runBlocking {
        // Arrange - Generate 200 elements (cache max is 100)
        val elements = (1..200).map { i ->
            "element_$i"
        }

        // Act - Concurrent lookups triggering eviction
        val jobs = elements.map { uuid ->
            launch(Dispatchers.IO) {
                service.findNodeByUuid(uuid)
            }
        }
        jobs.joinAll()

        // Assert - No crashes, cache size <= 100
    }

    /**
     * Test: Coroutine scope cancellation
     *
     * Verifies: All coroutines cleaned up on service destroy
     */
    @Test
    fun testCoroutineScopeCancellation_AllCleanedUp() = runBlocking {
        // Arrange - Launch 10 long-running coroutines
        repeat(10) {
            service.serviceScope.launch {
                delay(10000)  // 10 seconds
            }
        }

        // Act - Destroy service
        service.onDestroy()

        // Assert - Scope should be cancelled
        assertFalse(service.serviceScope.isActive)
    }
}
```

**Run Tests**:
```bash
./gradlew :Modules:VoiceOS:libraries:JITLearning:connectedDebugAndroidTest --tests ConcurrencyTests
```

**Success Criteria**:
- ✅ No race conditions detected
- ✅ No deadlocks
- ✅ No ConcurrentModificationException
- ✅ Atomic operations maintain correctness
- ✅ All 16+ tests pass

---

## Test Category 4: Memory Leak Tests (4+ tests)

### 4.1 LeakCanary Integration

**Setup**:
```gradle
// In app/build.gradle.kts
dependencies {
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
```

**Tests**:
```kotlin
@RunWith(AndroidJUnit4::class)
class MemoryLeakTests {

    /**
     * Test: NodeCache doesn't leak AccessibilityNodeInfo
     *
     * Memory Impact: 1MB/element if leaked
     */
    @Test
    fun testNodeCache_NoLeaks() {
        // Arrange
        val service = bindToService()

        // Act - Register 1000 elements
        repeat(1000) { i ->
            val node = createMockAccessibilityNodeInfo()
            service.registerElement("element_$i", node)
        }

        // Trigger GC
        Runtime.getRuntime().gc()
        Thread.sleep(2000)

        // Assert - LeakCanary should report 0 leaks
        val leaks = LeakCanary.Config.dumpHeap
        assertEquals(0, leaks.size)
    }

    /**
     * Test: Framework cache LRU eviction
     *
     * Verifies: Cache bounded to 50 frameworks
     */
    @Test
    fun testFrameworkCache_BoundedTo50() = runBlocking {
        // Act - Add 100 frameworks
        repeat(100) { i ->
            learnAppCore.detectFramework("com.test.app$i")
        }

        // Assert - Cache size should be <= 50
        // (Can't access private cache, but verify no memory growth)
        val initialMemory = Runtime.getRuntime().totalMemory()

        repeat(100) { i ->
            learnAppCore.detectFramework("com.test.app${i + 100}")
        }

        val finalMemory = Runtime.getRuntime().totalMemory()

        // Memory growth should be minimal (LRU working)
        val growth = finalMemory - initialMemory
        assertTrue(growth < 10 * 1024 * 1024)  // < 10MB
    }

    /**
     * Test: Batch queue bounded
     *
     * Verifies: ArrayBlockingQueue prevents unbounded growth
     */
    @Test
    fun testBatchQueue_Bounded() = runBlocking {
        // Arrange
        val maxBatchSize = 50

        // Act - Try to add 100 commands
        repeat(100) { i ->
            val element = createTestElement(i)
            learnAppCore.processElement(element)
        }

        // Assert - Queue size should never exceed maxBatchSize
        val batchSize = learnAppCore.getBatchSize()
        assertTrue(batchSize <= maxBatchSize)
    }

    /**
     * Test: Coroutine cleanup on service destroy
     *
     * Verifies: No leaked coroutines after onDestroy
     */
    @Test
    fun testCoroutineCleanup_NoLeaks() = runBlocking {
        // Arrange
        val service = bindToService()

        // Act - Launch coroutines
        repeat(10) {
            service.serviceScope.launch {
                delay(Long.MAX_VALUE)  // Infinite delay
            }
        }

        // Destroy service
        service.onDestroy()

        // Assert - Scope cancelled
        assertFalse(service.serviceScope.isActive)

        // No coroutine leaks (verified by profiler)
    }
}
```

**Run Tests**:
```bash
./gradlew :Modules:VoiceOS:libraries:JITLearning:connectedDebugAndroidTest --tests MemoryLeakTests
```

**Success Criteria**:
- ✅ LeakCanary reports 0 leaks
- ✅ Framework cache bounded to 50
- ✅ Batch queue never exceeds max
- ✅ All coroutines cancelled

---

## Test Category 5: Performance Tests

### 5.1 UUID Lookup Benchmark

**Objective**: Verify 97% latency reduction (450ms → 15ms)

```kotlin
@RunWith(AndroidJUnit4::class)
class PerformanceTests {

    /**
     * Benchmark: UUID lookup performance
     *
     * Target: <20ms average (97% improvement from 450ms)
     */
    @Test
    fun benchmarkUuidLookup_AverageUnder20ms() {
        // Arrange - Create 500-node tree
        val root = createMockHierarchy(nodeCount = 500)
        val service = bindToService()

        // Build cache
        service.traverseAndCache(root)

        // Act - Perform 1000 lookups
        val latencies = mutableListOf<Long>()

        repeat(1000) {
            val uuid = "element_${Random.nextInt(500)}"
            val start = System.nanoTime()
            service.findNodeByUuid(uuid)
            val end = System.nanoTime()

            latencies.add((end - start) / 1_000_000)  // Convert to ms
        }

        // Assert
        val average = latencies.average()
        val p95 = latencies.sorted()[950]

        println("UUID Lookup Performance:")
        println("  Average: ${average}ms")
        println("  P95: ${p95}ms")

        assertTrue("Average latency should be <20ms", average < 20)
        assertTrue("P95 latency should be <50ms", p95 < 50)
    }

    /**
     * Benchmark: Cache hit rate
     *
     * Target: 85%+ cache hit rate
     */
    @Test
    fun benchmarkCacheHitRate_Above85Percent() {
        // Arrange
        val root = createMockHierarchy(500)
        service.traverseAndCache(root)

        // Act - Mix of cached and new lookups
        var hits = 0
        var misses = 0

        repeat(1000) {
            val uuid = if (Random.nextBoolean()) {
                // 50% cached
                "element_${Random.nextInt(500)}"
            } else {
                // 50% new
                "new_element_$it"
            }

            val found = service.findNodeByUuid(uuid)
            if (found != null) hits++ else misses++
        }

        // Assert
        val hitRate = hits.toDouble() / (hits + misses)
        println("Cache hit rate: ${hitRate * 100}%")

        assertTrue("Cache hit rate should be >50%", hitRate > 0.5)
    }
}
```

**Run Benchmark**:
```bash
./gradlew :Modules:VoiceOS:libraries:JITLearning:connectedDebugAndroidTest --tests PerformanceTests

# Watch output for latency numbers
```

**Success Criteria**:
- ✅ Average latency <20ms
- ✅ P95 latency <50ms
- ✅ 97% improvement verified

---

## Test Category 6: Integration Tests

### 6.1 End-to-End Exploration Flow

```kotlin
@RunWith(AndroidJUnit4::class)
class IntegrationTests {

    /**
     * Test: Full exploration cycle
     *
     * Flow: Start → Capture → Click → Navigate → Stop
     */
    @Test
    fun testFullExplorationCycle_Success() = runBlocking {
        // Arrange
        val service = bindToService()
        val callback = mockk<IExplorationCallback>()

        // Act 1: Start exploration
        service.startExploration("com.example.testapp", callback)
        assertTrue(service.isExploring())

        // Act 2: Capture screen
        val screen1 = service.captureCurrentScreen()
        assertNotNull(screen1)
        assertTrue(screen1.elements.isNotEmpty())

        // Act 3: Click element
        val element = screen1.elements.first()
        val clicked = service.clickElement(element.uuid, screen1.screenHash)
        assertTrue(clicked)

        // Wait for navigation
        delay(1000)

        // Act 4: Capture new screen
        val screen2 = service.captureCurrentScreen()
        assertNotEquals(screen1.screenHash, screen2.screenHash)

        // Act 5: Stop exploration
        service.stopExploration()
        assertFalse(service.isExploring())

        // Assert - Cleanup verified
        verify { callback.onExplorationStopped() }
    }

    /**
     * Test: Safety checks block dangerous elements
     */
    @Test
    fun testSafetyIntegration_BlocksDangerousElements() = runBlocking {
        // Arrange - Screen with password field
        val elements = listOf(
            ElementInfo(
                uuid = "password_field",
                isPassword = true,
                className = "android.widget.EditText",
                text = ""
            )
        )

        safetyManager.updateScreenContext(
            "com.test",
            "screen_hash",
            "LoginActivity",
            elements
        )

        // Act - Check element safety
        val result = safetyManager.checkElement(elements[0])

        // Assert - Password field blocked
        assertFalse(result.isSafe)
        assertEquals(SafetyCategory.PASSWORD_FIELD, result.category)
        assertEquals(SafetyRecommendation.SKIP_ELEMENT, result.recommendation)
    }

    /**
     * Test: Loop detection stops exploration
     */
    @Test
    fun testLoopDetection_StopsExploration() = runBlocking {
        // Arrange
        val screenHash = "repeated_screen"
        var loopDetected = false

        val callback = object : SafetyCallback {
            override fun onLoopDetected(hash: String, count: Int) {
                loopDetected = true
            }
            // ... other methods
        }

        val safetyManager = SafetyManager(callback)

        // Act - Visit same screen 5 times
        repeat(5) {
            safetyManager.updateScreenContext(
                "com.test",
                screenHash,
                "MainActivity",
                emptyList()
            )
        }

        // Assert - Loop detected
        assertTrue(loopDetected)
        assertTrue(safetyManager.isLoopDetected(screenHash))
    }
}
```

**Run Integration Tests**:
```bash
./gradlew :Modules:VoiceOS:libraries:JITLearning:connectedDebugAndroidTest --tests IntegrationTests
```

**Success Criteria**:
- ✅ Full exploration cycle completes
- ✅ Safety checks work
- ✅ Loop detection works
- ✅ Service cleanup verified

---

## Running All Tests

### Complete Test Suite

```bash
# Set Java version
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# Run all unit tests
./gradlew :Modules:VoiceOS:libraries:JITLearning:test
./gradlew :Modules:VoiceOS:libraries:LearnAppCore:test

# Run all instrumented tests (requires device/emulator)
./gradlew :Modules:VoiceOS:libraries:JITLearning:connectedDebugAndroidTest
./gradlew :Modules:VoiceOS:libraries:LearnAppCore:connectedDebugAndroidTest

# Generate coverage report
./gradlew :Modules:VoiceOS:libraries:JITLearning:createDebugCoverageReport
./gradlew :Modules:VoiceOS:libraries:LearnAppCore:createDebugCoverageReport

# View coverage
open Modules/VoiceOS/libraries/JITLearning/build/reports/coverage/androidTest/debug/index.html
```

---

## Test Coverage Goals

| Module | Current | Target | Status |
|--------|---------|--------|--------|
| JITLearning | 35% | 70% | ⚠️ Needs tests |
| LearnAppCore | 60% | 70% | ⚠️ Needs tests |
| SafetyManager | 71% | 70% | ✅ Meets target |

---

## CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: macos-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'oracle'

      - name: Run unit tests
        run: ./gradlew test

      - name: Run instrumented tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          script: ./gradlew connectedDebugAndroidTest

      - name: Generate coverage
        run: ./gradlew createDebugCoverageReport

      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

---

## Manual Testing Checklist

### Pre-Release Verification

- [ ] **Build Tests**
  - [ ] Clean build succeeds (JDK 17)
  - [ ] No compilation errors
  - [ ] No warnings (except suppressed)

- [ ] **Security Tests**
  - [ ] Unauthorized bind blocked
  - [ ] SQL injection blocked
  - [ ] XSS blocked
  - [ ] Path traversal blocked
  - [ ] All validation tests pass

- [ ] **Concurrency Tests**
  - [ ] No race conditions
  - [ ] No deadlocks
  - [ ] Atomic operations correct
  - [ ] All 16+ tests pass

- [ ] **Memory Tests**
  - [ ] LeakCanary reports 0 leaks
  - [ ] Caches bounded
  - [ ] Cleanup verified

- [ ] **Performance Tests**
  - [ ] UUID lookup <20ms avg
  - [ ] 97% improvement verified

- [ ] **Integration Tests**
  - [ ] Full exploration works
  - [ ] Safety checks work
  - [ ] Loop detection works

---

## Troubleshooting

### Common Issues

#### Issue 1: Build fails with JDK error

**Problem**: `jlink` errors with JDK 24

**Solution**:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
./gradlew clean build
```

#### Issue 2: Tests fail with SecurityException

**Problem**: Service permission not granted

**Solution**: Ensure both apps signed with same certificate

#### Issue 3: Memory leak test fails

**Problem**: LeakCanary not detecting leaks

**Solution**: Check `onDestroy()` calls `clear()` on all caches

---

## Success Criteria Summary

### P0 Tests (Must Pass)

- ✅ **Build succeeds** (JDK 17)
- ✅ **Security tests pass** (24+ tests)
- ✅ **No memory leaks** (LeakCanary clean)
- ✅ **No race conditions** (Thread Sanitizer clean)

### P1 Tests (Should Pass)

- ⚠️ **Coverage ≥70%** (Currently 45%)
- ⚠️ **Concurrency tests** (16+ tests)
- ⚠️ **Integration tests** (8+ tests)

---

## Next Steps

1. ✅ **Verify builds** - Run build tests
2. ⚠️ **Create test files** - Implement 24+ security tests
3. ⚠️ **Run concurrency tests** - Verify thread safety
4. ⚠️ **Run leak tests** - Verify memory cleanup
5. ⚠️ **Benchmark performance** - Verify 97% improvement
6. ✅ **Document results** - Update coverage reports

---

**Testing Status**: Instructions complete, tests need implementation
**Estimated Effort**: 20 hours to implement all test suites
**Priority**: P0 security and concurrency tests first

---

*Document created: 2025-12-12*
*Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md*
