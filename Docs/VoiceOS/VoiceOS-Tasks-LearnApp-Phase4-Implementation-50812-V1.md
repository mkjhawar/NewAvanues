# VoiceOS-Tasks-LearnApp-Phase4-Implementation-50812-V1

**Date:** 2025-12-08
**Module:** LearnApp Phase 4 - Implementation Tasks
**Platform:** Android
**Status:** 📋 TASK LIST
**Type:** Detailed Task Breakdown

---

## Task Overview

This document provides a detailed, actionable task list for LearnApp Phase 4 implementation. Each task includes:
- Clear description
- Estimated duration
- Dependencies
- Success criteria
- Acceptance test

**Total Tasks:** 22
**Total Duration:** 10-15 days

---

## Phase 4.1: CommandDiscoveryManager (5 Tasks)

### Task 4.1.1: Update CommandDiscoveryManager API

**Description:** Modify CommandDiscoveryManager interface to accept ExplorationStats and return CommandDiscoveryResult

**Duration:** 4 hours

**Dependencies:** None

**Files:**
- `CommandDiscoveryManager.kt` (modify interface)
- `CommandDiscoveryManagerImpl.kt` (create implementation)
- `CommandPriority.kt` (create enum)
- `DiscoveredCommand.kt` (create data class)
- `CommandDiscoveryResult.kt` (create data class)
- `TutorialStep.kt` (create data class)

**Subtasks:**
1. Define `CommandDiscoveryResult` data class
2. Define `DiscoveredCommand` data class
3. Define `CommandPriority` enum (HIGH, MEDIUM, LOW)
4. Define `TutorialStep` data class
5. Update `CommandDiscoveryManager` interface
6. Create `CommandDiscoveryManagerImpl` skeleton

**Success Criteria:**
- ✅ API compiles without errors
- ✅ All data classes have proper KDoc
- ✅ Interface defines clear contract

**Acceptance Test:**
```kotlin
@Test
fun `interface compiles and can be instantiated`() {
    val manager: CommandDiscoveryManager = CommandDiscoveryManagerImpl(/* deps */)
    assertNotNull(manager)
}
```

---

### Task 4.1.2: Implement Priority Calculation Algorithm

**Description:** Implement algorithm to calculate command priority based on frequency and clickability

**Duration:** 6 hours

**Dependencies:** Task 4.1.1

**Files:**
- `CommandDiscoveryManagerImpl.kt` (implement `calculatePriority()`)

**Algorithm:**
```
HIGH priority:
  - frequency ≥ 5 AND clickability ≥ 0.7
  - OR: element is in top 10% of both frequency and clickability

MEDIUM priority:
  - frequency ≥ 2 OR clickability ≥ 0.5
  - OR: element is in top 50% of either metric

LOW priority:
  - Everything else
```

**Subtasks:**
1. Implement `calculatePriority(vuid, metrics)` method
2. Add percentile calculation helper
3. Handle edge cases (0 frequency, null clickability)
4. Add extensive unit tests

**Success Criteria:**
- ✅ Algorithm produces expected results for known inputs
- ✅ Edge cases handled gracefully
- ✅ Unit tests cover 100% of code paths

**Acceptance Test:**
```kotlin
@Test
fun `HIGH priority for frequent and clickable elements`() {
    val metric = VUIDMetric(frequency = 10, clickabilityScore = 0.9)
    val priority = manager.calculatePriority("vuid1", metric)
    assertEquals(CommandPriority.HIGH, priority)
}

@Test
fun `MEDIUM priority for moderately used elements`() {
    val metric = VUIDMetric(frequency = 3, clickabilityScore = 0.6)
    val priority = manager.calculatePriority("vuid2", metric)
    assertEquals(CommandPriority.MEDIUM, priority)
}

@Test
fun `LOW priority for rarely used elements`() {
    val metric = VUIDMetric(frequency = 1, clickabilityScore = 0.3)
    val priority = manager.calculatePriority("vuid3", metric)
    assertEquals(CommandPriority.LOW, priority)
}
```

---

### Task 4.1.3: Implement Command Discovery Logic

**Description:** Implement `processExplorationCompletion()` to analyze stats and generate CommandDiscoveryResult

**Duration:** 8 hours

**Dependencies:** Task 4.1.2

**Files:**
- `CommandDiscoveryManagerImpl.kt` (implement `processExplorationCompletion()`)

**Subtasks:**
1. Extract VUIDs from ExplorationStats
2. Query VUIDMetricsManager for each VUID
3. Calculate priority for each command
4. Group commands by priority
5. Sort within each group (by frequency DESC)
6. Generate tutorial recommendations (top 5 HIGH priority)
7. Return CommandDiscoveryResult

**Success Criteria:**
- ✅ All commands categorized correctly
- ✅ Commands sorted by frequency within priority groups
- ✅ Tutorial includes top 5 high-priority commands
- ✅ Handles 200+ commands efficiently (<2 seconds)

**Acceptance Test:**
```kotlin
@Test
fun `processExplorationCompletion groups commands by priority`() = runTest {
    // Given: ExplorationStats with 50 elements
    val stats = ExplorationStats(/* 50 elements */)

    // When: Process completion
    val result = manager.processExplorationCompletion("com.test", stats)

    // Then: Commands grouped correctly
    assertEquals(50, result.commandsDiscovered)
    assertTrue(result.highPriorityCommands.isNotEmpty())
    assertTrue(result.mediumPriorityCommands.isNotEmpty())
    assertTrue(result.lowPriorityCommands.isNotEmpty())

    // Tutorial has top 5 high-priority commands
    assertEquals(5, result.tutorialRecommendations.size)
}
```

---

### Task 4.1.4: Create Post-Exploration Overlay

**Description:** Implement CommandDiscoveryOverlay to display discovered commands after exploration

**Duration:** 10 hours

**Dependencies:** Task 4.1.3

**Files:**
- `CommandDiscoveryOverlay.kt` (create class)
- `overlay_command_discovery.xml` (create layout)
- `item_discovered_command.xml` (create RecyclerView item layout)
- `CommandAdapter.kt` (create RecyclerView adapter)

**Subtasks:**
1. Design overlay layout (header, command list, CTA buttons)
2. Create CommandAdapter for RecyclerView
3. Implement overlay show/hide logic
4. Wire up button click listeners
5. Add animations (fade in/out)
6. Test on different screen sizes

**Success Criteria:**
- ✅ Overlay appears within 2 seconds of exploration completion
- ✅ Top 5 commands displayed with descriptions
- ✅ Buttons functional (See All, Start Tutorial, Close)
- ✅ Animations smooth (60fps)
- ✅ Overlay dismisses cleanly

**Acceptance Test:**
```kotlin
@Test
fun `overlay displays top 5 commands`() {
    // Given: CommandDiscoveryResult with 20 commands
    val result = CommandDiscoveryResult(/* 20 commands */)

    // When: Show overlay
    val overlay = CommandDiscoveryOverlay(context, result, {}, {}, {}, {})
    overlay.show()

    // Then: Overlay visible with top 5 commands
    assertTrue(overlay.isShowing())
    val commandList = overlay.findViewById<RecyclerView>(R.id.rvTopCommands)
    assertEquals(5, commandList.adapter?.itemCount)
}
```

---

### Task 4.1.5: Implement Command Tutorial

**Description:** Create interactive tutorial to guide users through discovered commands

**Duration:** 10 hours

**Dependencies:** Task 4.1.4

**Files:**
- `CommandTutorial.kt` (create class)
- `TutorialOverlay.kt` (create overlay)
- `tutorial_overlay.xml` (create layout)

**Subtasks:**
1. Design tutorial flow state machine
2. Create TutorialOverlay UI
3. Implement step progression (next/skip)
4. Add audio guidance (TextToSpeech)
5. Add visual highlighting (element bounding boxes)
6. Implement progress tracking (step 2/5)
7. Save tutorial completion to preferences

**Success Criteria:**
- ✅ Tutorial guides through top 5 commands
- ✅ Audio guidance working
- ✅ Progress tracked visually
- ✅ Can skip or complete tutorial
- ✅ Tutorial completion persisted

**Acceptance Test:**
```kotlin
@Test
fun `tutorial guides through all steps`() = runTest {
    // Given: 5 commands
    val commands = listOf(/* 5 commands */)

    // When: Start tutorial
    val tutorial = CommandTutorial(context, commands, service)
    tutorial.startTutorial()

    // Then: Steps through all 5 commands
    repeat(5) {
        tutorial.nextStep()
    }

    // Tutorial completes
    val completed = getTutorialCompletionStatus()
    assertTrue(completed)
}
```

---

## Phase 4.2: Integration Tests (5 Tasks)

### Task 4.2.1: Set Up Test Environment

**Description:** Configure Android instrumentation test environment with required dependencies

**Duration:** 2 hours

**Dependencies:** None

**Files:**
- `build.gradle` (app module)
- `AndroidManifest.xml` (test manifest)

**Subtasks:**
1. Add AndroidX Test dependencies
2. Add UIAutomator dependency
3. Configure testInstrumentationRunner
4. Set up test permissions
5. Create base test class

**Success Criteria:**
- ✅ Tests can run on device
- ✅ All dependencies resolved
- ✅ Test manifest configured

**Acceptance Test:**
```bash
./gradlew connectedAndroidTest
# Should run without configuration errors
```

---

### Task 4.2.2: Full Exploration Flow Test

**Description:** Implement end-to-end test for exploration → metrics → discovery flow

**Duration:** 8 hours

**Dependencies:** Task 4.2.1, Phase 4.1

**Files:**
- `LearnAppFullFlowIntegrationTest.kt`

**Subtasks:**
1. Write test setup (clear database, install test apps)
2. Implement `testFullExplorationFlow_Calculator()`
3. Implement helper: `waitForExplorationComplete()`
4. Implement helper: `getVUIDMetrics()`
5. Implement helper: `getCommandDiscoveryResult()`
6. Implement helper: `waitForOverlay()`
7. Add assertions for all checkpoints

**Success Criteria:**
- ✅ Test passes on 3 different devices
- ✅ Exploration completes successfully
- ✅ Metrics populated correctly
- ✅ Overlay displays with commands

**Acceptance Test:**
Run on:
- Pixel 6 (Android 13)
- Samsung Galaxy S21 (Android 14)
- Realwear HMT-1 (Android 11)

---

### Task 4.2.3: Cross-App Isolation Test

**Description:** Verify that metrics are isolated per app

**Duration:** 4 hours

**Dependencies:** Task 4.2.2

**Files:**
- `LearnAppCrossAppTest.kt`

**Subtasks:**
1. Implement `testCrossAppMetricsIsolation()`
2. Explore 2 different apps sequentially
3. Query metrics for each app
4. Verify no cross-contamination

**Success Criteria:**
- ✅ Calculator metrics don't contain Clock VUIDs
- ✅ Clock metrics don't contain Calculator VUIDs
- ✅ Command counts correct for each app

**Acceptance Test:**
```kotlin
@Test
fun testCrossAppMetricsIsolation() {
    exploreApp("com.android.calculator2")
    exploreApp("com.android.deskclock")

    val calcMetrics = getMetrics("com.android.calculator2")
    val clockMetrics = getMetrics("com.android.deskclock")

    // No overlap
    assertTrue(calcMetrics.keys.intersect(clockMetrics.keys).isEmpty())
}
```

---

### Task 4.2.4: Pause/Resume Test

**Description:** Verify exploration state is preserved across pause/resume

**Duration:** 4 hours

**Dependencies:** Task 4.2.2

**Files:**
- `LearnAppStatePersistenceTest.kt`

**Subtasks:**
1. Implement `testPauseResumeExploration()`
2. Start exploration
3. Pause after 5 screens
4. Wait 30 seconds
5. Resume and verify completion

**Success Criteria:**
- ✅ State preserved during pause
- ✅ Exploration resumes correctly
- ✅ All screens explored after resume

**Acceptance Test:**
```kotlin
@Test
fun testPauseResumeExploration() {
    startExploration("com.test.app")
    waitForScreens(5)

    pauseExploration()
    Thread.sleep(30_000)

    resumeExploration()
    waitForCompletion()

    assertTrue(allScreensExplored())
}
```

---

### Task 4.2.5: Edge Case Tests

**Description:** Test edge cases and failure scenarios

**Duration:** 6 hours

**Dependencies:** Task 4.2.2

**Files:**
- `LearnAppEdgeCaseTest.kt`

**Subtasks:**
1. Test app with 0 clickable elements
2. Test app with 500+ clickable elements
3. Test app crash during exploration
4. Test network-dependent app in offline mode
5. Test permission denial scenarios

**Success Criteria:**
- ✅ Graceful failure for 0-element apps
- ✅ Handles 500+ elements without crash
- ✅ Recovers from app crashes
- ✅ Handles offline mode correctly

**Acceptance Test:**
```kotlin
@Test
fun testZeroClickableElements() {
    val result = exploreApp("app.with.no.buttons")
    assertEquals(0, result.commandsDiscovered)
}

@Test
fun testLargeNumberOfElements() {
    val result = exploreApp("app.with.many.buttons")
    assertTrue(result.commandsDiscovered > 500)
}
```

---

## Phase 4.3: VUIDMetrics SQLDelight Migration (7 Tasks)

### Task 4.3.1: Define SQLDelight Schema

**Description:** Create SQLDelight schema for VUID metrics and snapshots

**Duration:** 4 hours

**Dependencies:** None

**Files:**
- `vuid_metrics.sq`

**Subtasks:**
1. Define `vuid_metrics` table
2. Define `vuid_metrics_snapshot` table
3. Add foreign key constraints
4. Create indexes for performance
5. Define queries (insert, select, delete)

**Success Criteria:**
- ✅ Schema compiles without errors
- ✅ Foreign keys work correctly
- ✅ Indexes created successfully

**Acceptance Test:**
```bash
./gradlew clean build
# SQLDelight should generate code without errors
```

---

### Task 4.3.2: Implement Repository

**Description:** Create VUIDMetricsRepository for database operations

**Duration:** 8 hours

**Dependencies:** Task 4.3.1

**Files:**
- `VUIDMetricsRepository.kt`

**Subtasks:**
1. Implement `saveMetrics()`
2. Implement `getMetrics()`
3. Implement `getTopCommands()`
4. Implement `createSnapshot()`
5. Implement `getSnapshots()`
6. Add error handling
7. Add logging

**Success Criteria:**
- ✅ All CRUD operations working
- ✅ Transactions atomic
- ✅ Error handling comprehensive

**Acceptance Test:**
```kotlin
@Test
fun `saveMetrics persists to database`() = runTest {
    val metrics = mapOf("vuid1" to VUIDMetric(/* ... */))

    repository.saveMetrics("com.test", metrics)

    val retrieved = repository.getMetrics("com.test")
    assertEquals(metrics, retrieved)
}
```

---

### Task 4.3.3: Add Repository Tests

**Description:** Create comprehensive unit tests for repository

**Duration:** 6 hours

**Dependencies:** Task 4.3.2

**Files:**
- `VUIDMetricsRepositoryTest.kt`

**Subtasks:**
1. Test save/retrieve metrics
2. Test update existing metrics
3. Test delete metrics
4. Test snapshot creation
5. Test query performance
6. Test error scenarios

**Success Criteria:**
- ✅ 100% coverage for repository
- ✅ All edge cases tested
- ✅ Performance tests pass

**Acceptance Test:**
```kotlin
@Test
fun `query performance under 50ms for 1000 VUIDs`() = runTest {
    // Insert 1000 VUIDs
    val metrics = generateMetrics(1000)
    repository.saveMetrics("com.test", metrics)

    // Query all
    val start = System.nanoTime()
    repository.getMetrics("com.test")
    val elapsedMs = (System.nanoTime() - start) / 1_000_000

    assertTrue(elapsedMs < 50)
}
```

---

### Task 4.3.4: Implement Migration Logic

**Description:** Create migration utility to move from in-memory to SQLDelight

**Duration:** 6 hours

**Dependencies:** Task 4.3.2

**Files:**
- `VUIDMetricsMigration.kt`

**Subtasks:**
1. Implement `migrateFromInMemory()`
2. Add verification logic
3. Add rollback capability
4. Add progress reporting
5. Handle partial failures

**Success Criteria:**
- ✅ All metrics migrated successfully
- ✅ Verification passes
- ✅ No data loss
- ✅ Rollback works if migration fails

**Acceptance Test:**
```kotlin
@Test
fun `migration preserves all data`() = runTest {
    // Given: In-memory with 100 metrics
    val inMemory = VUIDMetricsManager()
    repeat(100) { inMemory.recordElement(/* ... */) }

    // When: Migrate
    val result = VUIDMetricsMigration.migrateFromInMemory(inMemory, repository)

    // Then: All metrics persisted
    assertTrue(result.isSuccess)
    assertEquals(100, result.getOrThrow().migratedVUIDs)
}
```

---

### Task 4.3.5: Update VUIDMetricsManager

**Description:** Modify VUIDMetricsManager to use repository instead of in-memory cache

**Duration:** 6 hours

**Dependencies:** Task 4.3.4

**Files:**
- `VUIDMetricsManager.kt`

**Subtasks:**
1. Remove ConcurrentHashMap
2. Inject VUIDMetricsRepository
3. Update `recordElement()` to use repository
4. Update `getMetrics()` to use repository
5. Add caching layer (optional optimization)

**Success Criteria:**
- ✅ In-memory cache removed
- ✅ All operations use repository
- ✅ No breaking changes to API
- ✅ Performance maintained

**Acceptance Test:**
```kotlin
@Test
fun `VUIDMetricsManager uses repository`() = runTest {
    val manager = VUIDMetricsManager(repository)

    manager.recordElement("com.test", "vuid1", VUIDMetric(/* ... */))

    // Should be in database, not just in-memory
    val metrics = repository.getMetrics("com.test")
    assertTrue(metrics.containsKey("vuid1"))
}
```

---

### Task 4.3.6: Integration with ExplorationEngine

**Description:** Wire repository into exploration flow for real-time persistence

**Duration:** 4 hours

**Dependencies:** Task 4.3.5

**Files:**
- `ExplorationEngine.kt` (modify)
- `LearnAppIntegration.kt` (modify)

**Subtasks:**
1. Initialize repository in LearnAppIntegration
2. Pass repository to VUIDMetricsManager
3. Save metrics on exploration completion
4. Create snapshot for app version

**Success Criteria:**
- ✅ Metrics persisted during exploration
- ✅ Snapshot created on completion
- ✅ No performance degradation

**Acceptance Test:**
```kotlin
@Test
fun `exploration persists metrics to database`() {
    exploreApp("com.test.app")
    waitForCompletion()

    val metrics = repository.getMetrics("com.test.app")
    assertTrue(metrics.isNotEmpty())
}
```

---

### Task 4.3.7: Historical Snapshot Feature

**Description:** Implement historical snapshot tracking for app evolution analysis

**Duration:** 4 hours

**Dependencies:** Task 4.3.6

**Files:**
- `SnapshotManager.kt` (create)

**Subtasks:**
1. Detect app version changes
2. Create snapshot on version change
3. Implement snapshot comparison
4. Add UI to view historical snapshots (optional)

**Success Criteria:**
- ✅ Snapshots created on version change
- ✅ Comparison shows differences
- ✅ Snapshot data accurate

**Acceptance Test:**
```kotlin
@Test
fun `snapshot created on app version change`() = runTest {
    // Explore v1.0
    exploreApp("com.test.app", version = "1.0")

    // Explore v1.1
    exploreApp("com.test.app", version = "1.1")

    // Should have 2 snapshots
    val snapshots = repository.getSnapshots("com.test.app")
    assertEquals(2, snapshots.size)
}
```

---

## Phase 4.4: Performance Validation (5 Tasks)

### Task 4.4.1: Implement Benchmark Tests

**Description:** Create performance benchmark tests for all critical paths

**Duration:** 8 hours

**Dependencies:** Phase 4.1, Phase 4.3

**Files:**
- `LearnAppPerformanceBenchmarkTest.kt`

**Subtasks:**
1. Implement `benchmark_ExplorationSpeed()`
2. Implement `benchmark_MemoryUsage()`
3. Implement `benchmark_DatabaseOperations()`
4. Implement `benchmark_CommandDiscovery()`
5. Implement `benchmark_OverlayDisplay()`

**Success Criteria:**
- ✅ All benchmarks implemented
- ✅ Baseline metrics documented
- ✅ Benchmarks repeatable

**Acceptance Test:**
```kotlin
@Test
fun benchmark_ExplorationSpeed() {
    val screensPerMinute = measureExplorationSpeed()
    assertTrue(screensPerMinute >= 15)
}
```

---

### Task 4.4.2: Run Benchmarks on Devices

**Description:** Execute benchmarks on 3 different devices and collect results

**Duration:** 4 hours

**Dependencies:** Task 4.4.1

**Devices:**
- Pixel 6 (Android 13)
- Samsung Galaxy S21 (Android 14)
- Realwear HMT-1 (Android 11)

**Subtasks:**
1. Run benchmarks on each device
2. Collect results in spreadsheet
3. Identify bottlenecks
4. Document findings

**Success Criteria:**
- ✅ Benchmarks run on all 3 devices
- ✅ Results documented
- ✅ Bottlenecks identified

**Acceptance Test:**
Results table:
| Metric | Pixel 6 | Galaxy S21 | HMT-1 | Target | Status |
|--------|---------|------------|-------|--------|--------|
| Exploration Speed | 18 spm | 16 spm | 12 spm | 15 spm | ✅/❌ |
| Memory Growth | 42 MB | 48 MB | 38 MB | 50 MB | ✅/❌ |
| DB Save (100) | 420 ms | 380 ms | 510 ms | 500 ms | ✅/❌ |
| Command Discovery | 1.8s | 1.6s | 2.1s | 2s | ✅/❌ |

---

### Task 4.4.3: Profiling and Analysis

**Description:** Use Android Profiler to identify performance bottlenecks

**Duration:** 6 hours

**Dependencies:** Task 4.4.2

**Tools:**
- Android Studio Profiler (CPU, Memory, Network)
- LeakCanary
- SQLDelight profiler

**Subtasks:**
1. Profile exploration engine (CPU)
2. Profile memory usage (heap allocations)
3. Profile database operations (query time)
4. Detect memory leaks (LeakCanary)
5. Document findings

**Success Criteria:**
- ✅ Bottlenecks identified
- ✅ Zero memory leaks
- ✅ Optimization opportunities documented

**Acceptance Test:**
- CPU profile shows no hot spots >20% CPU
- Heap growth linear, no spikes
- No memory leaks detected by LeakCanary

---

### Task 4.4.4: Optimization Implementation

**Description:** Implement optimizations for identified bottlenecks

**Duration:** 8 hours

**Dependencies:** Task 4.4.3

**Target Areas:**
1. ExplorationEngine: Reduce tree traversal overhead
2. VUIDMetrics: Batch database writes
3. CommandDiscovery: Cache priority calculations
4. Overlay: Lazy load command list

**Subtasks:**
1. Optimize bottleneck #1
2. Optimize bottleneck #2
3. Optimize bottleneck #3
4. Test optimizations
5. Measure improvements

**Success Criteria:**
- ✅ All critical bottlenecks optimized
- ✅ Performance improved by ≥20%
- ✅ No regressions

**Acceptance Test:**
Re-run benchmarks, verify improvements:
- Exploration speed: +20%
- Memory usage: -15%
- Database operations: -30%
- Command discovery: -25%

---

### Task 4.4.5: Final Validation

**Description:** Re-run all benchmarks and validate performance targets met

**Duration:** 4 hours

**Dependencies:** Task 4.4.4

**Subtasks:**
1. Re-run benchmarks on all 3 devices
2. Compare before/after metrics
3. Document improvements
4. Create performance report

**Success Criteria:**
- ✅ All performance targets met or exceeded
- ✅ No memory leaks
- ✅ Performance report complete

**Acceptance Test:**
All performance targets met:
- ✅ Exploration Speed ≥15 screens/min
- ✅ Memory Growth <50MB
- ✅ DB Save (100 VUIDs) <500ms
- ✅ Command Discovery <2s
- ✅ Overlay Display <2s

---

## Task Summary

### By Phase

| Phase | Tasks | Duration | Status |
|-------|-------|----------|--------|
| 4.1 | 5 | 38 hours (5 days) | Pending |
| 4.2 | 5 | 24 hours (3 days) | Pending |
| 4.3 | 7 | 38 hours (5 days) | Pending |
| 4.4 | 5 | 30 hours (4 days) | Pending |
| **Total** | **22** | **130 hours (17 days)** | **Pending** |

### Critical Path

```
4.1.1 → 4.1.2 → 4.1.3 → 4.1.4 → 4.1.5
  ↓
4.2.1 → 4.2.2 → 4.2.3 → 4.2.4 → 4.2.5
  ↓
4.4.1 → 4.4.2 → 4.4.3 → 4.4.4 → 4.4.5
```

**Parallel Work:**
- Phase 4.3 (SQLDelight) can run in parallel with Phase 4.1

---

## Completion Checklist

### Phase 4.1: CommandDiscoveryManager
- [ ] Task 4.1.1: API updated
- [ ] Task 4.1.2: Priority algorithm implemented
- [ ] Task 4.1.3: Discovery logic complete
- [ ] Task 4.1.4: Overlay created
- [ ] Task 4.1.5: Tutorial implemented

### Phase 4.2: Integration Tests
- [ ] Task 4.2.1: Test environment set up
- [ ] Task 4.2.2: Full flow test passing
- [ ] Task 4.2.3: Cross-app test passing
- [ ] Task 4.2.4: Pause/resume test passing
- [ ] Task 4.2.5: Edge case tests passing

### Phase 4.3: SQLDelight Migration
- [ ] Task 4.3.1: Schema defined
- [ ] Task 4.3.2: Repository implemented
- [ ] Task 4.3.3: Repository tests passing
- [ ] Task 4.3.4: Migration logic working
- [ ] Task 4.3.5: VUIDMetricsManager updated
- [ ] Task 4.3.6: Integration complete
- [ ] Task 4.3.7: Snapshot feature working

### Phase 4.4: Performance Validation
- [ ] Task 4.4.1: Benchmarks implemented
- [ ] Task 4.4.2: Benchmarks run on devices
- [ ] Task 4.4.3: Profiling complete
- [ ] Task 4.4.4: Optimizations implemented
- [ ] Task 4.4.5: Final validation passing

---

## Related Documents

- `VoiceOS-Spec-LearnApp-Phase4-Completion-50812-V1.md` - Detailed specification
- `VoiceOS-Plan-LearnApp-Phase4-Implementation-50812-V1.md` - Implementation plan
- `VoiceOS-LearnApp-Phase3-Complete-Summary-53110-V1.md` - Phase 3 completion

---

**Status:** 📋 READY FOR IMPLEMENTATION
**Next Task:** Task 4.1.1 (Update CommandDiscoveryManager API)
**Estimated Start:** 2025-12-09
