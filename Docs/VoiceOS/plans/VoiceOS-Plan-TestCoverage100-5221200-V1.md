# VoiceOS Test Coverage to 100/100 - Implementation Plan

**Plan ID:** VoiceOS-Plans-TestCoverage100-5221200-V1
**Created:** 2025-12-23
**Goal:** Achieve 100/100 code health through comprehensive test coverage
**Current Coverage:** ~30% (estimated)
**Target Coverage:** 95%+
**Strategy:** Code Proximity Clustering + Sprint-Based Swarm Execution
**Execution Mode:** YOLO + Swarm (5 parallel agents)

---

## Executive Summary

This plan organizes test coverage work into **5 code proximity clusters** mapped to **6 two-week sprints**. Each cluster groups related components to minimize context switching and maximize parallel execution efficiency.

**Test Targets:**
- **Primary:** 25 files modified in deep analysis fixes (must have 95%+ coverage)
- **Secondary:** Critical untested components (database, service lifecycle, speech)
- **Total:** ~60 files requiring comprehensive test coverage

**Swarm Rationale:**
- 5 independent test clusters enable parallel execution
- Each cluster has dedicated test infrastructure
- Minimal dependencies between clusters
- Estimated time savings: 60% (180 hours → 72 hours)

---

## Test Coverage Analysis (CoT)

```
REASONING: Analyze current state and gaps

CURRENT STATE:
- Source Files: 215 Kotlin files
- Test Files: 15 existing tests
- Test Ratio: ~7% (215/15)
- Estimated Coverage: <30%

CRITICAL GAPS (from deep analysis fixes):
Wave 1 - Database (4 files):
  ✗ VoiceOSCoreDatabaseAdapter.kt - 0 tests (311 LOC, 0% coverage)
  ✗ DatabaseMetrics.kt - 0 tests (311 LOC, 0% coverage)
  ✗ QueryExtensions.kt - 0 tests (150 LOC, 0% coverage)
  ✗ CleanupWorker.kt - 0 tests (40 LOC, 0% coverage)
  TOTAL: ~812 LOC untested

Wave 2 - Service (4 files):
  ✗ VoiceOSService.kt - 1 basic test (2,500+ LOC, <10% coverage)
  ✗ ServiceLifecycleManager.kt - 0 tests (400 LOC, 0% coverage)
  ✗ VoiceRecognitionManager.kt - 0 tests (300 LOC, 0% coverage)
  ✗ IPCManager.kt - 0 tests (500 LOC, 0% coverage)
  TOTAL: ~3,700 LOC untested

Wave 3 - Speech (6 files):
  ✗ SpeechEngineManager.kt - 0 tests (800 LOC, 0% coverage)
  ✗ ISpeechEngine.kt - 0 tests (interface, needs contract tests)
  ✗ GoogleEngineAdapter.kt - 0 tests (450 LOC, 0% coverage)
  ✗ AzureEngineAdapter.kt - 0 tests (480 LOC, 0% coverage)
  ✗ VoskEngineAdapter.kt - 0 tests (430 LOC, 0% coverage)
  ✗ SpeechEngineFactory.kt - 0 tests (170 LOC, 0% coverage)
  TOTAL: ~2,330 LOC untested

Wave 4 - Concurrency (3 files):
  ✗ ActionCoordinator.kt - 0 tests (350 LOC, 0% coverage)
  ✗ CommandDispatcher.kt - 0 tests (600 LOC, 0% coverage)
  ✗ ExplorationEngine.kt - 0 tests (1,200 LOC, 0% coverage)
  TOTAL: ~2,150 LOC untested

Wave 5 - UI/UX (8 files):
  ✗ NumberedSelectionOverlay.kt - 0 tests (400 LOC, 0% coverage)
  ✗ ConfidenceOverlay.kt - 0 tests (200 LOC, 0% coverage)
  ✗ FloatingProgressWidget.kt - 0 tests (300 LOC, 0% coverage)
  ✗ RenameHintOverlay.kt - 0 tests (150 LOC, 0% coverage)
  ✗ CommandStatusOverlay.kt - 0 tests (200 LOC, 0% coverage)
  ✗ HelpMenuHandler.kt - 0 tests (250 LOC, 0% coverage)
  ✗ SelectHandler.kt - 0 tests (350 LOC, 0% coverage)
  ✗ NumberHandler.kt - 0 tests (200 LOC, 0% coverage)
  TOTAL: ~2,050 LOC untested

GRAND TOTAL: ~11,042 LOC untested from deep analysis fixes alone

ADDITIONAL CRITICAL COMPONENTS (not in deep analysis):
- Command handlers (15 files, ~3,000 LOC)
- LearnApp core (10 files, ~5,000 LOC)
- Database repositories (8 files, ~2,000 LOC)
TOTAL ADDITIONAL: ~10,000 LOC

OVERALL UNTESTED: ~21,000 LOC
TARGET: 95% coverage = ~20,000 LOC with tests
TESTS NEEDED: ~500-600 test cases
```

---

## Code Proximity Clusters (ToT)

```
Tree of Thought - Test Organization Strategies

STRATEGY 1: Layer-Based Clustering
Pros: Matches architecture, clear boundaries
Cons: Cross-layer dependencies, integration gaps
Clusters: Data → Logic → Service → UI

STRATEGY 2: Feature-Based Clustering
Pros: End-to-end coverage, user-centric
Cons: Many cross-feature dependencies
Clusters: Learning → Commands → Speech → Settings

STRATEGY 3: Proximity-Based Clustering (SELECTED)
Pros: Minimal context switching, parallel execution
Cons: Requires dependency analysis upfront
Clusters: Files in same directory + related dependencies

DECISION: Use Proximity-Based Clustering
Rationale:
- Maps directly to deep analysis fix clusters
- Developers already familiar with these groupings
- Enables immediate parallel execution
- Matches existing codebase organization
```

### Dependency Graph

```
CLUSTER 1 (Database Foundation Tests):
Files: VoiceOSCoreDatabaseAdapter, DatabaseMetrics, QueryExtensions, CleanupWorker
Dependencies: NONE (can start immediately)
Test Types: Unit, Integration, Performance
Estimated Tests: 120 test cases

CLUSTER 2 (Service Lifecycle Tests):
Files: VoiceOSService, ServiceLifecycleManager, VoiceRecognitionManager, IPCManager
Dependencies: Cluster 1 (database mocks)
Test Types: Unit, Integration, State Machine, Concurrency
Estimated Tests: 150 test cases

CLUSTER 3 (Speech Engine Tests):
Files: SpeechEngineManager, ISpeechEngine, All Adapters, Factory
Dependencies: NONE (independent subsystem)
Test Types: Unit, Integration, Contract, Mock external SDKs
Estimated Tests: 100 test cases

CLUSTER 4 (Concurrency & Performance Tests):
Files: ActionCoordinator, CommandDispatcher, ExplorationEngine
Dependencies: Cluster 2 (service mocks)
Test Types: Unit, Concurrency, Performance, ANR detection
Estimated Tests: 90 test cases

CLUSTER 5 (UI/UX & Accessibility Tests):
Files: All overlays, handlers
Dependencies: Cluster 2 (service mocks), Cluster 3 (speech mocks)
Test Types: Unit, Compose UI, Accessibility, WCAG verification
Estimated Tests: 140 test cases

TOTAL TESTS: ~600 test cases
```

---

## Sprint Plan (RoT - Reflective Optimization)

### Sprint Organization

```
OPTIMIZATION: Balance load and minimize dependencies

SPRINT 1 (Weeks 1-2): Foundation + Infrastructure
- Cluster 1: Database Tests (120 tests)
- Test infrastructure setup
- CI/CD integration
- Coverage reporting

SPRINT 2 (Weeks 3-4): Independent Subsystems
- Cluster 3: Speech Engine Tests (100 tests)
- Parallel to Sprint 1 if resources available
- Mock external SDKs (Vivoka, Azure, Vosk)

SPRINT 3 (Weeks 5-6): Service Layer
- Cluster 2: Service Lifecycle Tests (150 tests)
- Depends on Cluster 1 completion
- State machine verification
- Initialization timeout tests

SPRINT 4 (Weeks 7-8): Concurrency & Performance
- Cluster 4: Concurrency Tests (90 tests)
- Depends on Cluster 2 completion
- ANR detection tests
- Performance benchmarks

SPRINT 5 (Weeks 9-10): UI/UX & Accessibility
- Cluster 5: UI Tests (140 tests)
- Depends on Clusters 2 + 3
- Compose UI tests
- WCAG compliance verification

SPRINT 6 (Weeks 11-12): Integration & Polish
- End-to-end integration tests
- Coverage gap analysis
- Performance optimization
- Documentation updates

TOTAL DURATION: 12 weeks (3 months)
PARALLEL EXECUTION: Reduce to 8-10 weeks with 2-3 developers
```

---

## Cluster 1: Database Foundation Tests

**Agent:** database-test-coverage
**Sprint:** 1 (Weeks 1-2)
**Files:** 4
**Estimated Tests:** 120
**Target Coverage:** 95%+
**Dependencies:** NONE (can start immediately)

### Test Breakdown

#### VoiceOSCoreDatabaseAdapter.kt (40 tests)

**Unit Tests (25 tests):**
```kotlin
class VoiceOSCoreDatabaseAdapterTest {
    // deleteAppSpecificElements tests (8 tests)
    @Test fun `deleteAppSpecificElements - success with valid package name`()
    @Test fun `deleteAppSpecificElements - handles empty package name`()
    @Test fun `deleteAppSpecificElements - handles non-existent package`()
    @Test fun `deleteAppSpecificElements - verifies transaction rollback on error`()
    @Test fun `deleteAppSpecificElements - verifies all tables cleaned`()
    @Test fun `deleteAppSpecificElements - concurrent deletion safety`()
    @Test fun `deleteAppSpecificElements - timeout handling`()
    @Test fun `deleteAppSpecificElements - IO dispatcher usage verified`()

    // filterByApp tests (6 tests)
    @Test fun `filterByApp - returns correct elements for package`()
    @Test fun `filterByApp - returns empty list for non-existent package`()
    @Test fun `filterByApp - handles null package name gracefully`()
    @Test fun `filterByApp - verifies IO dispatcher usage`()
    @Test fun `filterByApp - handles large result sets`()
    @Test fun `filterByApp - concurrent access safety`()

    // Batch operations tests (6 tests)
    @Test fun `updateFormGroups - batch update success`()
    @Test fun `updateFormGroups - no runBlocking verification`()
    @Test fun `updateFormGroups - transaction consistency`()
    @Test fun `insertElements - batch insert verification`()
    @Test fun `insertElements - duplicate handling`()
    @Test fun `insertElements - error rollback verification`()

    // Error handling tests (5 tests)
    @Test fun `database error - proper exception propagation`()
    @Test fun `database error - cleanup on failure`()
    @Test fun `database error - transaction rollback verified`()
    @Test fun `database error - logging verification`()
    @Test fun `database error - retry logic not present (expected)`()
}
```

**Integration Tests (10 tests):**
```kotlin
class VoiceOSCoreDatabaseAdapterIntegrationTest {
    @Test fun `end-to-end - insert, filter, delete workflow`()
    @Test fun `end-to-end - multiple apps isolation verified`()
    @Test fun `integration - with QueryExtensions batch operations`()
    @Test fun `integration - with CleanupWorker coordination`()
    @Test fun `integration - database migration compatibility`()
    @Test fun `integration - concurrent multi-app operations`()
    @Test fun `integration - large dataset performance (1000+ elements)`()
    @Test fun `integration - transaction boundary verification`()
    @Test fun `integration - foreign key constraints verified`()
    @Test fun `integration - schema version compatibility`()
}
```

**Performance Tests (5 tests):**
```kotlin
class VoiceOSCoreDatabaseAdapterPerformanceTest {
    @Test fun `performance - delete 1000 elements under 500ms`()
    @Test fun `performance - filter 10000 elements under 200ms`()
    @Test fun `performance - batch insert 1000 elements under 1s`()
    @Test fun `performance - concurrent operations no deadlock`()
    @Test fun `performance - memory usage under 50MB for large ops`()
}
```

#### DatabaseMetrics.kt (30 tests)

**Unit Tests (20 tests):**
```kotlin
class DatabaseMetricsTest {
    // measureOperation tests (8 tests)
    @Test fun `measureOperation - captures duration correctly`()
    @Test fun `measureOperation - handles success case`()
    @Test fun `measureOperation - handles failure case`()
    @Test fun `measureOperation - tracks item count`()
    @Test fun `measureOperation - concurrent operation tracking`()
    @Test fun `measureOperation - tracks min/max duration`()
    @Test fun `measureOperation - tracks success/failure counts`()
    @Test fun `measureOperation - operation name validation`()

    // trackError tests (6 tests)
    @Test fun `trackError - records error correctly`()
    @Test fun `trackError - groups by operation name`()
    @Test fun `trackError - tracks error frequency`()
    @Test fun `trackError - concurrent error tracking`()
    @Test fun `trackError - error message sanitization`()
    @Test fun `trackError - error type classification`()

    // Statistics tests (6 tests)
    @Test fun `getOperationStats - returns correct aggregates`()
    @Test fun `getOperationStats - handles empty metrics`()
    @Test fun `getErrorStats - returns error aggregates`()
    @Test fun `reset - clears all metrics`()
    @Test fun `export - generates correct JSON`()
    @Test fun `export - handles large metric sets`()
}
```

**Concurrency Tests (10 tests):**
```kotlin
class DatabaseMetricsConcurrencyTest {
    @Test fun `concurrent measureOperation - no data corruption`()
    @Test fun `concurrent trackError - thread safety verified`()
    @Test fun `concurrent read-write - no deadlocks`()
    @Test fun `stress test - 1000 concurrent operations`()
    @Test fun `stress test - 100 concurrent errors`()
    @Test fun `race condition - increment counters safely`()
    @Test fun `race condition - min/max updates atomic`()
    @Test fun `mutex usage - verified with coroutine testing`()
    @Test fun `mutex usage - no blocking on main thread`()
    @Test fun `mutex usage - fairness under contention`()
}
```

#### QueryExtensions.kt (30 tests)

**Unit Tests (20 tests):**
```kotlin
class QueryExtensionsTest {
    // insertBatch tests (10 tests)
    @Test fun `insertBatch - hierarchy - success with valid data`()
    @Test fun `insertBatch - hierarchy - handles empty list`()
    @Test fun `insertBatch - hierarchy - ID to hash conversion`()
    @Test fun `insertBatch - hierarchy - null hash map handling`()
    @Test fun `insertBatch - hierarchy - transaction wrapper verified`()
    @Test fun `insertBatch - hierarchy - error handling per item`()
    @Test fun `insertBatch - hierarchy - logging verification`()
    @Test fun `insertBatch - hierarchy - duplicate prevention`()
    @Test fun `insertBatch - hierarchy - concurrent safety`()
    @Test fun `insertBatch - hierarchy - large batch performance`()

    // Other batch operations (10 tests)
    @Test fun `insertBatch - elements - success verification`()
    @Test fun `insertBatch - elements - error rollback`()
    @Test fun `insertBatch - commands - batch insert`()
    @Test fun `insertBatch - commands - duplicate handling`()
    @Test fun `updateBatch - elements - mass update`()
    @Test fun `updateBatch - elements - partial failure handling`()
    @Test fun `deleteBatch - elements - mass deletion`()
    @Test fun `deleteBatch - elements - cascade verification`()
    @Test fun `transaction - commit on success`()
    @Test fun `transaction - rollback on failure`()
}
```

**Integration Tests (10 tests):**
```kotlin
class QueryExtensionsIntegrationTest {
    @Test fun `integration - batch insert with adapter`()
    @Test fun `integration - hierarchy relationships maintained`()
    @Test fun `integration - foreign key constraints respected`()
    @Test fun `integration - cascade delete verification`()
    @Test fun `integration - concurrent batch operations`()
    @Test fun `integration - transaction isolation verified`()
    @Test fun `integration - deadlock prevention`()
    @Test fun `integration - error recovery workflow`()
    @Test fun `integration - migration compatibility`()
    @Test fun `integration - performance under load`()
}
```

#### CleanupWorker.kt (20 tests)

**Unit Tests (12 tests):**
```kotlin
class CleanupWorkerTest {
    @Test fun `doWork - success case returns SUCCESS`()
    @Test fun `doWork - failure case returns FAILURE`()
    @Test fun `doWork - retry case returns RETRY`()
    @Test fun `doWork - cleanup operation called`()
    @Test fun `doWork - metrics tracked correctly`()
    @Test fun `doWork - logging verification`()
    @Test fun `doWork - WorkManager params passed correctly`()
    @Test fun `doWork - cancellation handling`()
    @Test fun `doWork - timeout enforcement (if added)`()
    @Test fun `shouldScheduleCleanup - returns correct boolean`()
    @Test fun `shouldScheduleCleanup - last run time checked`()
    @Test fun `shouldScheduleCleanup - interval validation`()
}
```

**Integration Tests (8 tests):**
```kotlin
class CleanupWorkerIntegrationTest {
    @Test fun `WorkManager scheduling - periodic work verified`()
    @Test fun `WorkManager constraints - network not required verified`()
    @Test fun `WorkManager constraints - battery optimization respected`()
    @Test fun `integration - with VoiceOSCoreDatabaseAdapter`()
    @Test fun `integration - cleanup completion notification`()
    @Test fun `integration - failure retry mechanism`()
    @Test fun `integration - cancellation handling`()
    @Test fun `end-to-end - scheduled cleanup workflow`()
}
```

### Test Infrastructure Setup

**Dependencies:**
```kotlin
// build.gradle.kts additions
dependencies {
    // Testing framework
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")

    // Mocking
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")

    // Coroutine testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")

    // Database testing
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.test:core:1.5.0")

    // AndroidX test
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Coverage
    // JaCoCo configured via plugin
}

// JaCoCo configuration
plugins {
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/testDebugUnitTest.exec")
    })
}
```

### Success Criteria

- ✅ All 120 tests passing
- ✅ 95%+ line coverage for all 4 files
- ✅ 90%+ branch coverage
- ✅ Zero flaky tests (100% reproducible)
- ✅ CI/CD integration complete
- ✅ Coverage reports generated automatically

---

## Cluster 2: Service Lifecycle Tests

**Agent:** service-test-coverage
**Sprint:** 3 (Weeks 5-6)
**Files:** 4
**Estimated Tests:** 150
**Target Coverage:** 95%+
**Dependencies:** Cluster 1 (database mocks)

### Test Breakdown

#### VoiceOSService.kt (60 tests)

**State Machine Tests (15 tests):**
```kotlin
class VoiceOSServiceStateTest {
    @Test fun `state transition - CREATED to INITIALIZING`()
    @Test fun `state transition - INITIALIZING to READY`()
    @Test fun `state transition - INITIALIZING to ERROR`()
    @Test fun `state transition - ERROR to INITIALIZING (retry)`()
    @Test fun `state transition - READY to DESTROYED`()
    @Test fun `state transition - invalid transitions rejected`()
    @Test fun `state transition - concurrent access safety`()
    @Test fun `state machine - all states reachable`()
    @Test fun `state machine - no invalid state combinations`()
    @Test fun `state query - serviceState atomic reads`()
    @Test fun `state query - thread-safe observation`()
    @Test fun `state persistence - survives configuration changes`()
    @Test fun `state recovery - after crash scenario`()
    @Test fun `state logging - all transitions logged`()
    @Test fun `state metrics - transition timing tracked`()
}
```

**Initialization Tests (20 tests):**
```kotlin
class VoiceOSServiceInitializationTest {
    @Test fun `initialization - success within timeout`()
    @Test fun `initialization - timeout triggers after 30s`()
    @Test fun `initialization - retry attempt 1 after 1s backoff`()
    @Test fun `initialization - retry attempt 2 after 2s backoff`()
    @Test fun `initialization - retry attempt 3 after 4s backoff`()
    @Test fun `initialization - max retries triggers error state`()
    @Test fun `initialization - cleanup on failure called`()
    @Test fun `initialization - mutex prevents double init`()
    @Test fun `initialization - database initialized first`()
    @Test fun `initialization - speech engine after database`()
    @Test fun `initialization - LearnApp after speech`()
    @Test fun `initialization - all components verified`()
    @Test fun `initialization - partial failure rolls back`()
    @Test fun `initialization - concurrent init requests blocked`()
    @Test fun `initialization - cancellation during init`()
    @Test fun `initialization - force unwraps removed verified`()
    @Test fun `initialization - lateinit config replaced verified`()
    @Test fun `initialization - notification shown on complete`()
    @Test fun `initialization - error notification on failure`()
    @Test fun `initialization - metrics tracked`()
}
```

**Service Lifecycle Tests (15 tests):**
```kotlin
class VoiceOSServiceLifecycleTest {
    @Test fun `onCreate - basic service setup`()
    @Test fun `onCreate - state set to CREATED`()
    @Test fun `onServiceConnected - triggers initialization`()
    @Test fun `onServiceConnected - handles null AccessibilityServiceInfo`()
    @Test fun `onAccessibilityEvent - delegates to handlers`()
    @Test fun `onAccessibilityEvent - handles null event`()
    @Test fun `onInterrupt - stops all operations`()
    @Test fun `onInterrupt - cleans up resources`()
    @Test fun `onDestroy - cleanup called`()
    @Test fun `onDestroy - state set to DESTROYED`()
    @Test fun `onDestroy - all components released`()
    @Test fun `configuration change - service survives`()
    @Test fun `low memory - graceful degradation`()
    @Test fun `permission revoked - service disabled`()
    @Test fun `system restart - service recovers`()
}
```

**Command Processing Tests (10 tests):**
```kotlin
class VoiceOSServiceCommandTest {
    @Test fun `command processing - delegates to dispatcher`()
    @Test fun `command processing - handles timeout`()
    @Test fun `command processing - concurrent commands queued`()
    @Test fun `command processing - priority ordering`()
    @Test fun `command processing - cancellation support`()
    @Test fun `command processing - error handling`()
    @Test fun `command processing - metrics tracked`()
    @Test fun `command processing - logging verification`()
    @Test fun `command processing - result callback invoked`()
    @Test fun `command processing - no UI thread blocking verified`()
}
```

#### ServiceLifecycleManager.kt (35 tests)
#### VoiceRecognitionManager.kt (30 tests)
#### IPCManager.kt (25 tests)

*(Detailed test specs similar to above)*

---

## Cluster 3: Speech Engine Tests

**Agent:** speech-test-coverage
**Sprint:** 2 (Weeks 3-4, parallel with Sprint 1)
**Files:** 6
**Estimated Tests:** 100
**Target Coverage:** 95%+
**Dependencies:** NONE (mock external SDKs)

### Test Breakdown

#### SpeechEngineManager.kt (35 tests)

**Initialization Tests (10 tests):**
```kotlin
class SpeechEngineManagerInitializationTest {
    @Test fun `initialization - single engine success`()
    @Test fun `initialization - double init prevented by mutex`()
    @Test fun `initialization - during shutdown blocked`()
    @Test fun `initialization - atomic flag checks verified`()
    @Test fun `initialization - state update on success`()
    @Test fun `initialization - state update on failure`()
    @Test fun `initialization - cleanup on failure`()
    @Test fun `initialization - timeout enforcement`()
    @Test fun `initialization - error message captured`()
    @Test fun `initialization - thread safety verified`()
}
```

**Engine Switching Tests (10 tests):**
```kotlin
class SpeechEngineManagerSwitchingTest {
    @Test fun `switch engine - Vivoka to Vosk`()
    @Test fun `switch engine - Vosk to Azure`()
    @Test fun `switch engine - cleanup previous engine`()
    @Test fun `switch engine - maintains listening state`()
    @Test fun `switch engine - concurrent switch blocked`()
    @Test fun `switch engine - during recognition handled`()
    @Test fun `switch engine - error recovery`()
    @Test fun `switch engine - state consistency`()
    @Test fun `switch engine - metrics tracked`()
    @Test fun `switch engine - notification sent`()
}
```

**Thread Safety Tests (15 tests):**
```kotlin
class SpeechEngineManagerConcurrencyTest {
    @Test fun `concurrent init attempts - only one succeeds`()
    @Test fun `concurrent destroy calls - safe cleanup`()
    @Test fun `concurrent state reads - no corruption`()
    @Test fun `StateFlow emission - thread-safe`()
    @Test fun `Mutex usage - prevents race conditions`()
    @Test fun `AtomicBoolean usage - verified`()
    @Test fun `Dispatchers.Main usage - documented and verified`()
    @Test fun `stress test - 100 concurrent operations`()
    @Test fun `stress test - rapid init/destroy cycles`()
    @Test fun `stress test - concurrent state queries`()
    @Test fun `deadlock prevention - verified`()
    @Test fun `fairness - no thread starvation`()
    @Test fun `cancellation - coroutine cleanup`()
    @Test fun `timeout - enforced correctly`()
    @Test fun `memory barriers - visibility guaranteed`()
}
```

#### ISpeechEngine.kt (10 tests - Contract Tests)

```kotlin
class ISpeechEngineContractTest {
    // These tests verify that all implementations follow the contract

    @Test fun `contract - all implementations provide initialize`()
    @Test fun `contract - all implementations provide startListening`()
    @Test fun `contract - all implementations provide stopListening`()
    @Test fun `contract - all implementations provide updateCommands`()
    @Test fun `contract - all implementations provide updateConfiguration`()
    @Test fun `contract - all implementations provide destroy`()
    @Test fun `contract - all implementations throw on uninitialized access`()
    @Test fun `contract - all implementations are thread-safe`()
    @Test fun `contract - all implementations emit state updates`()
    @Test fun `contract - all implementations handle cleanup`()
}
```

#### GoogleEngineAdapter.kt (15 tests)
#### AzureEngineAdapter.kt (15 tests)
#### VoskEngineAdapter.kt (15 tests)
#### SpeechEngineFactory.kt (10 tests)

*(Detailed test specs similar to above)*

---

## Cluster 4: Concurrency & Performance Tests

**Agent:** concurrency-test-coverage
**Sprint:** 4 (Weeks 7-8)
**Files:** 3
**Estimated Tests:** 90
**Target Coverage:** 95%+
**Dependencies:** Cluster 2 (service mocks)

### Test Breakdown

#### ActionCoordinator.kt (30 tests)

**ANR Prevention Tests (10 tests):**
```kotlin
class ActionCoordinatorANRTest {
    @Test fun `executeAction - no runBlocking verified`()
    @Test fun `executeAction - uses suspend function`()
    @Test fun `executeAction - dispatcher usage verified`()
    @Test fun `executeAction - timeout enforcement`()
    @Test fun `executeAction - UI thread never blocked`()
    @Test fun `executeAction - concurrent actions queued`()
    @Test fun `executeActionBlocking - throws UnsupportedOperationException`()
    @Test fun `executeActionBlocking - deprecated annotation verified`()
    @Test fun `executeActionBlocking - error logged`()
    @Test fun `performance - action completes under 100ms`()
}
```

**Action Queue Tests (10 tests):**
```kotlin
class ActionCoordinatorQueueTest {
    @Test fun `action queue - FIFO ordering`()
    @Test fun `action queue - concurrent safety`()
    @Test fun `action queue - priority support`()
    @Test fun `action queue - cancellation handling`()
    @Test fun `action queue - overflow handling`()
    @Test fun `action queue - empty queue handling`()
    @Test fun `action queue - metrics tracked`()
    @Test fun `action queue - thread-safe ConcurrentHashMap usage`()
    @Test fun `action queue - no memory leaks`()
    @Test fun `action queue - stress test 1000 actions`()
}
```

**Error Handling Tests (10 tests):**
```kotlin
class ActionCoordinatorErrorTest {
    @Test fun `error - action failure logged`()
    @Test fun `error - exception propagated correctly`()
    @Test fun `error - cleanup on failure`()
    @Test fun `error - partial rollback supported`()
    @Test fun `error - retry mechanism (if implemented)`()
    @Test fun `error - user notification sent`()
    @Test fun `error - metrics recorded`()
    @Test fun `error - state consistency maintained`()
    @Test fun `error - concurrent error handling`()
    @Test fun `error - error recovery workflow`()
}
```

#### CommandDispatcher.kt (35 tests)
#### ExplorationEngine.kt (25 tests)

*(Detailed test specs similar to above)*

---

## Cluster 5: UI/UX & Accessibility Tests

**Agent:** ui-test-coverage
**Sprint:** 5 (Weeks 9-10)
**Files:** 8
**Estimated Tests:** 140
**Target Coverage:** 95%+
**Dependencies:** Clusters 2 + 3 (service + speech mocks)

### Test Breakdown

#### NumberedSelectionOverlay.kt (25 tests)

**WCAG Compliance Tests (10 tests):**
```kotlin
class NumberedSelectionOverlayWCAGTest {
    @Test fun `WCAG - orange badge contrast 4_5 to 1 minimum`()
    @Test fun `WCAG - color change from FF9800 to F57C00 verified`()
    @Test fun `WCAG - text contrast on all backgrounds`()
    @Test fun `WCAG - touch target minimum 48dp`()
    @Test fun `WCAG - focus indicators visible`()
    @Test fun `WCAG - keyboard navigation support`()
    @Test fun `WCAG - screen reader compatibility`()
    @Test fun `WCAG - reduced motion respected`()
    @Test fun `WCAG - color not sole indicator`()
    @Test fun `WCAG - text alternatives provided`()
}
```

**Interaction Tests (10 tests):**
```kotlin
class NumberedSelectionOverlayInteractionTest {
    @Test fun `interaction - number selection triggers action`()
    @Test fun `interaction - haptic feedback on selection`()
    @Test fun `interaction - TTS confirmation spoken`()
    @Test fun `interaction - visual feedback shown`()
    @Test fun `interaction - concurrent selections handled`()
    @Test fun `interaction - touch vs voice selection`()
    @Test fun `interaction - error handling on invalid number`()
    @Test fun `interaction - accessibility focus management`()
    @Test fun `interaction - animation completion awaited`()
    @Test fun `interaction - metrics tracked`()
}
```

**Compose UI Tests (5 tests):**
```kotlin
class NumberedSelectionOverlayComposeTest {
    @Test fun `compose - renders correctly with ComposeView`()
    @Test fun `compose - lifecycle tied to owner`()
    @Test fun `compose - recomposition on state change`()
    @Test fun `compose - theme applied correctly`()
    @Test fun `compose - accessibility tree structure`()
}
```

#### ConfidenceOverlay.kt (20 tests)
#### FloatingProgressWidget.kt (20 tests)
#### RenameHintOverlay.kt (15 tests)
#### CommandStatusOverlay.kt (18 tests)
#### HelpMenuHandler.kt (15 tests)
#### SelectHandler.kt (15 tests)
#### NumberHandler.kt (12 tests)

*(Detailed test specs similar to above)*

---

## Sprint 6: Integration & Polish

**Weeks 11-12**
**Focus:** End-to-end integration, coverage gaps, performance optimization

### Integration Test Suite (30 tests)

```kotlin
class VoiceOSEndToEndTest {
    // Full workflows
    @Test fun `e2e - service startup to ready state`()
    @Test fun `e2e - app learning workflow complete`()
    @Test fun `e2e - voice command from speech to action`()
    @Test fun `e2e - database cleanup workflow`()
    @Test fun `e2e - engine switching mid-session`()

    // Cross-component integration
    @Test fun `integration - database to service to UI`()
    @Test fun `integration - speech to command to action`()
    @Test fun `integration - LearnApp to database to overlays`()
    @Test fun `integration - error propagation across layers`()
    @Test fun `integration - state synchronization across components`()

    // Performance integration
    @Test fun `performance - cold start under 2s`()
    @Test fun `performance - command processing under 500ms`()
    @Test fun `performance - database query under 100ms`()
    @Test fun `performance - UI render under 16ms (60fps)`()
    @Test fun `performance - memory usage under 150MB`()

    // Stress testing
    @Test fun `stress - 1000 commands in 10s`()
    @Test fun `stress - 10000 database operations`()
    @Test fun `stress - 100 concurrent speech recognitions`()
    @Test fun `stress - 24 hour continuous operation`()
    @Test fun `stress - rapid engine switching 100x`()

    // Recovery testing
    @Test fun `recovery - service crash and restart`()
    @Test fun `recovery - database corruption recovery`()
    @Test fun `recovery - network loss during cloud speech`()
    @Test fun `recovery - low memory scenario`()
    @Test fun `recovery - permission revocation`()

    // Accessibility testing
    @Test fun `accessibility - TalkBack full workflow`()
    @Test fun `accessibility - font scaling 200%`()
    @Test fun `accessibility - reduced motion full workflow`()
    @Test fun `accessibility - voice-only interaction`()
    @Test fun `accessibility - WCAG AA compliance verified`()
}
```

### Coverage Gap Analysis

**Process:**
1. Generate coverage report: `./gradlew jacocoTestReport`
2. Identify files <95% coverage
3. Analyze uncovered branches
4. Add targeted tests for gaps
5. Verify 95%+ coverage achieved

**Tools:**
- JaCoCo for coverage metrics
- SonarQube for code quality dashboard
- GitHub Actions for CI/CD enforcement

---

## Time Estimates

### Sequential Execution (Single Developer)
- Cluster 1: 4 weeks (120 tests)
- Cluster 2: 5 weeks (150 tests)
- Cluster 3: 3.5 weeks (100 tests)
- Cluster 4: 3 weeks (90 tests)
- Cluster 5: 4.5 weeks (140 tests)
- Sprint 6: 4 weeks (integration)
- **Total: 24 weeks (6 months)**

### Parallel Execution (Swarm - 3 Developers)
- Sprint 1: 2 weeks (Cluster 1)
- Sprint 2: 2 weeks (Cluster 3, parallel with Sprint 1)
- Sprint 3: 2 weeks (Cluster 2)
- Sprint 4: 2 weeks (Cluster 4)
- Sprint 5: 2 weeks (Cluster 5)
- Sprint 6: 2 weeks (Integration)
- **Total: 12 weeks (3 months)**

### Speedup: 50% faster (24 weeks → 12 weeks)

---

## CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Test Coverage

on:
  push:
    branches: [ main, Avanues-Main, VoiceOS-Development ]
  pull_request:
    branches: [ main, Avanues-Main ]

jobs:
  test-coverage:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

    - name: Run Unit Tests
      run: ./gradlew :Modules:VoiceOS:apps:VoiceOSCore:testDebugUnitTest

    - name: Generate Coverage Report
      run: ./gradlew :Modules:VoiceOS:apps:VoiceOSCore:jacocoTestReport

    - name: Upload Coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        files: ./Modules/VoiceOS/apps/VoiceOSCore/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
        flags: unittests
        name: codecov-umbrella
        fail_ci_if_error: true

    - name: Check Coverage Threshold
      run: |
        ./gradlew :Modules:VoiceOS:apps:VoiceOSCore:jacocoTestCoverageVerification

    - name: Comment Coverage on PR
      if: github.event_name == 'pull_request'
      uses: madrapps/jacoco-report@v1.6.1
      with:
        paths: ./Modules/VoiceOS/apps/VoiceOSCore/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
        token: ${{ secrets.GITHUB_TOKEN }}
        min-coverage-overall: 95
        min-coverage-changed-files: 90
```

### Coverage Threshold Configuration

```kotlin
// build.gradle.kts
tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")

    violationRules {
        rule {
            limit {
                minimum = "0.95".toBigDecimal() // 95% overall coverage
            }
        }

        rule {
            element = "CLASS"
            limit {
                minimum = "0.90".toBigDecimal() // 90% per class
            }
        }

        rule {
            element = "PACKAGE"
            limit {
                minimum = "0.85".toBigDecimal() // 85% per package
            }
        }
    }
}

// Make check depend on coverage verification
tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}
```

---

## Success Criteria

### Cluster-Level Success
- ✅ All unit tests pass (100% pass rate)
- ✅ 95%+ line coverage for cluster files
- ✅ 90%+ branch coverage for cluster files
- ✅ Zero flaky tests (100% reproducible)
- ✅ CI/CD integration verified
- ✅ Performance benchmarks met

### Sprint-Level Success
- ✅ All cluster-level criteria met
- ✅ Integration tests pass
- ✅ Coverage reports generated
- ✅ No regression in existing tests
- ✅ Code review approved
- ✅ Documentation updated

### Plan-Level Success (100/100 Code Health)
- ✅ 95%+ overall test coverage
- ✅ All 600+ tests passing
- ✅ Zero P0 bugs
- ✅ Zero flaky tests
- ✅ CI/CD enforcing coverage thresholds
- ✅ Performance benchmarks met
- ✅ Accessibility tests passing
- ✅ Documentation complete
- ✅ SonarQube quality gate: PASSED
- ✅ Code health score: 100/100

---

## Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Test writing slower than estimated | Medium | High | Add 20% buffer to each sprint |
| Flaky tests from async operations | High | Medium | Use Turbine for Flow testing, MockK for coroutines |
| External SDK mocking challenges | Medium | Medium | Create test doubles, document assumptions |
| Coverage gaps in complex code | Medium | High | Review coverage reports weekly, add targeted tests |
| CI/CD integration issues | Low | High | Test CI/CD early in Sprint 1 |
| Team velocity variation | Medium | Medium | Track velocity, adjust sprint scope |

---

## Deliverables

### Code Deliverables
1. **600+ Test Cases** across 5 clusters
2. **95%+ Coverage** for all critical files
3. **Test Infrastructure** (MockK, Turbine, JaCoCo)
4. **CI/CD Integration** (GitHub Actions, coverage enforcement)
5. **Performance Benchmarks** (startup, commands, database)

### Documentation Deliverables
1. **Test Strategy Document** (this plan)
2. **Test Writing Guidelines** (conventions, patterns)
3. **Coverage Reports** (HTML, XML for tools)
4. **Performance Baselines** (before/after metrics)
5. **Troubleshooting Guide** (common test failures)

### Quality Metrics
| Metric | Current | Target | Achievement |
|--------|---------|--------|-------------|
| Line Coverage | ~30% | 95% | +217% |
| Branch Coverage | ~20% | 90% | +350% |
| Test Count | 15 | 600+ | +3,900% |
| Flaky Tests | Unknown | 0 | 100% reliability |
| CI/CD Pass Rate | Unknown | 100% | Enforced |
| Code Health | 85/100 | 100/100 | +18% |

---

## Appendix A: Test Utilities

### Base Test Class

```kotlin
abstract class BaseVoiceOSTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    protected val testScope = TestScope()
    protected val testDispatcher = UnconfinedTestDispatcher(testScope.testScheduler)

    @Before
    fun setupBase() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownBase() {
        Dispatchers.resetMain()
        testScope.cancel()
    }
}
```

### Mock Utilities

```kotlin
object MockFactories {
    fun createMockDatabase(): VoiceOSDatabaseManager {
        return mockk {
            every { scrapedElements } returns mockk(relaxed = true)
            every { generatedCommands } returns mockk(relaxed = true)
            every { screenContexts } returns mockk(relaxed = true)
            every { transaction(any()) } just Runs
        }
    }

    fun createMockSpeechEngine(): ISpeechEngine {
        return mockk {
            coEvery { initialize(any()) } returns true
            every { startListening() } just Runs
            every { stopListening() } just Runs
            coEvery { updateCommands(any()) } just Runs
            every { destroy() } just Runs
        }
    }

    fun createMockContext(): Context {
        return mockk(relaxed = true) {
            every { packageName } returns "com.augmentalis.voiceoscore.test"
            every { applicationContext } returns this@mockk
        }
    }
}
```

### Flow Testing Utilities

```kotlin
object FlowTestUtils {
    suspend fun <T> Flow<T>.test(
        timeout: Duration = 1.seconds,
        block: suspend FlowTurbine<T>.() -> Unit
    ) {
        return this.testIn(CoroutineScope(UnconfinedTestDispatcher()), timeout)
            .apply { block() }
            .cancel()
    }
}
```

---

## Appendix B: Performance Benchmarks

### Baseline Targets

| Operation | Target | Measurement |
|-----------|--------|-------------|
| Service Initialization | <2s | Cold start to READY state |
| Database Query (100 rows) | <50ms | filterByApp with 100 elements |
| Database Insert (1000 rows) | <500ms | Batch insert 1000 elements |
| Command Dispatch | <100ms | Voice command to action start |
| UI Render (overlay) | <16ms | 60fps compliance |
| Speech Recognition | <500ms | Start listening to ready |
| Memory Usage (idle) | <100MB | Service running, no active work |
| Memory Usage (active) | <150MB | Active learning session |

### Benchmark Test Template

```kotlin
class PerformanceBenchmarkTest {
    @Test
    fun `benchmark - service initialization under 2s`() = runTest {
        val startTime = System.currentTimeMillis()

        val service = VoiceOSService()
        service.onCreate()
        service.onServiceConnected()

        // Wait for READY state
        service.serviceState.first { it is ServiceState.READY }

        val duration = System.currentTimeMillis() - startTime

        assertThat(duration).isLessThan(2000) // 2 seconds
        println("Service initialization: ${duration}ms")
    }
}
```

---

**Plan Status:** Ready for Execution
**Next Step:** Launch Sprint 1 (Cluster 1 - Database Tests)
**Swarm Activation:** 3-5 agents recommended for parallel sprints
**YOLO Mode:** Enabled (auto-chain to task generation and implementation)
