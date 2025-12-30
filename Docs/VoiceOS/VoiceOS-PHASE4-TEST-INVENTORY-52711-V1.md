# Phase 4: Test Inventory & Migration Checklist

**Agent 4 - Test Migration Specialist**
**Date:** 2025-11-27
**Total Tests:** 101 (19 existing + 51 move + 27 migrate + 4 create)

---

## Test Categories

### ✅ Category A: Infrastructure Tests (19 tests) - EXISTING

**Status:** Already written, ready to run when compilation works

**Location:** `/modules/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/database/`

| File | Tests | Status | Dependencies |
|------|-------|--------|--------------|
| RepositoryTransactionTest.kt | 7 | ✅ Written | Compilation |
| RepositoryQueryTest.kt | 12 | ✅ Written | Compilation |

**Tests:**
1. Transaction commits on success
2. Transaction rollback on error
3. Multiple repositories in same transaction
4. Nested transaction operations
5. Batch insert creates multiple records
6. Transaction failure isolation
7. Query and insert in same transaction
8. Query by primary key
9. Query by foreign key
10. Complex queries with joins
11. Pagination support
12. Sorting and filtering
13. Count queries
14. Exists queries
15. Delete operations
16. Update operations
17. Upsert operations
18. Batch operations
19. Concurrent query safety

**Action:** Run once compilation succeeds
**Time:** 5 minutes
**Expected Result:** 19/19 pass

---

### ✅ Category B: Accessibility Tests (51 tests) - MOVE ONLY

**Status:** Zero database dependencies - just move files once compilation works

**Location:** `/modules/apps/VoiceOSCore/src/test/java.disabled/com/augmentalis/voiceoscore/accessibility/`

#### B1: Handler Tests (3 files, ~30 tests)

| File | Tests | Dependencies | Action |
|------|-------|--------------|--------|
| handlers/DragHandlerTest.kt | ~12 | VoiceOSService mocks | Move |
| handlers/GazeHandlerTest.kt | ~10 | VoiceOSService mocks | Move |
| handlers/GestureHandlerTest.kt | ~8 | VoiceOSService mocks | Move |

**DragHandlerTest.kt Test Classes:**
- Initialization (2 tests)
- CommandHandling (3 tests)
- DragOperations (7 tests)
- GestureBuilding (2 tests)
- CursorTracking (2 tests)
- ErrorHandling (3 tests)
- LegacyCompatibility (3 tests)

#### B2: Tree Processing Tests (1 file, ~15 tests)

| File | Tests | Dependencies | Action |
|------|-------|--------------|--------|
| tree/AccessibilityTreeProcessorTest.kt | ~15 | AccessibilityNodeInfo mocks | Move |

**Test Classes:**
- NodeTraversal (6 tests)
- ElementDetection (9 tests)
- ActionMapping (7 tests)
- MemoryManagement (6 tests)
- BoundsCalculation (4 tests)
- FilteringAndPrioritization (4 tests)

#### B3: Overlay Tests (2 files, ~8 tests)

| File | Tests | Dependencies | Action |
|------|-------|--------------|--------|
| overlays/ConfidenceOverlayTest.kt | ~4 | Android UI mocks | Move |
| overlays/OverlayManagerTest.kt | ~4 | Android UI mocks | Move |

#### B4: Lifecycle Tests (4 files, ~16 tests)

| File | Tests | Dependencies | Action |
|------|-------|--------------|--------|
| lifecycle/AccessibilityNodeManagerSimpleTest.kt | ~3 | Accessibility API | Move |
| lifecycle/AccessibilityNodeManagerTest.kt | ~6 | Accessibility API | Move |
| lifecycle/AsyncQueryManagerTest.kt | ~4 | Coroutines | Move |
| lifecycle/SafeNodeTraverserTest.kt | ~3 | Accessibility API | Move |

#### B5: Utility Tests (2 files, ~6 tests)

| File | Tests | Dependencies | Action |
|------|-------|--------------|--------|
| utils/EventPriorityManagerTest.kt | ~3 | None | Move |
| utils/SafeNullHandlerTest.kt | ~3 | None | Move |

#### B6: Mock Infrastructure (2 files, 0 tests)

| File | Purpose | Action |
|------|---------|--------|
| mocks/MockVoiceAccessibilityService.kt | Test utility | Move |
| mocks/MockVoiceRecognitionManager.kt | Test utility | Move |

#### B7: Test Scenarios (3 files, ~10 tests)

| File | Tests | Dependencies | Action |
|------|-------|--------------|--------|
| test/EndToEndVoiceTest.kt | ~4 | Service mocks | Move |
| test/PerformanceTest.kt | ~3 | Service mocks | Move |
| test/CommandExecutionVerifier.kt | ~3 | Service mocks | Move |

**Migration Steps:**
```bash
# Once compilation succeeds:
cd /Volumes/M-Drive/Coding/VoiceOS/modules/apps/VoiceOSCore

# Move all files
mv src/test/java.disabled/com/augmentalis/voiceoscore/accessibility/handlers/*.kt \
   src/test/java/com/augmentalis/voiceoscore/accessibility/handlers/

mv src/test/java.disabled/com/augmentalis/voiceoscore/accessibility/tree/*.kt \
   src/test/java/com/augmentalis/voiceoscore/accessibility/tree/

mv src/test/java.disabled/com/augmentalis/voiceoscore/accessibility/overlays/*.kt \
   src/test/java/com/augmentalis/voiceoscore/accessibility/overlays/

mv src/test/java.disabled/com/augmentalis/voiceoscore/accessibility/lifecycle/*.kt \
   src/test/java/com/augmentalis/voiceoscore/accessibility/lifecycle/

mv src/test/java.disabled/com/augmentalis/voiceoscore/accessibility/utils/*.kt \
   src/test/java/com/augmentalis/voiceoscore/accessibility/utils/

mv src/test/java.disabled/com/augmentalis/voiceoscore/accessibility/mocks/*.kt \
   src/test/java/com/augmentalis/voiceoscore/accessibility/mocks/

mv src/test/java.disabled/com/augmentalis/voiceoscore/accessibility/test/*.kt \
   src/test/java/com/augmentalis/voiceoscore/accessibility/test/

# Update imports if needed (should not be necessary)
```

**Action:** Move files
**Time:** 30 minutes (including verification)
**Expected Result:** 51/51 pass immediately

---

### ⚠️ Category C: Database Tests (27 tests) - MIGRATE

**Status:** Need Entity→DTO migration, blocked by Agents 1-2

**Location:** `/modules/apps/VoiceOSCore/src/test/java.disabled/com/augmentalis/voiceoscore/`

#### C1: Scraping Validation Tests (12 tests) - BLOCKED BY AGENT 2

| File | Tests | Missing Dependencies | Agent |
|------|-------|---------------------|-------|
| scraping/validation/UUIDIntegrationTest.kt | 10 | ScrapedElementDTO | Agent 2 |
| scraping/validation/CachedElementHierarchyTest.kt | ? | ScrapedHierarchyDTO | Agent 2 |
| scraping/validation/DataFlowValidationTest.kt | ? | All scraping DTOs | Agent 2 |
| scraping/validation/HierarchyIntegrityTest.kt | ? | ScrapedHierarchyDTO | Agent 2 |
| scraping/validation/ScrapingDatabaseSyncTest.kt | ? | All scraping DTOs | Agent 2 |

**UUIDIntegrationTest.kt Tests:**
1. UUID generation metrics - 100% success
2. UUID generation metrics - low rate detection
3. UUID registration metrics - 100% success
4. UUID registration metrics - low rate detection
5. UUID uniqueness - no duplicates
6. UUID uniqueness - duplicate detection
7. UUID coverage measurement
8. UUID coverage - low coverage detection
9. Combined UUID metrics
10. UUID metrics - zero elements edge case

**Migration Pattern:**
```kotlin
// OLD (Room)
val entity = ScrapedElementEntity(
    elementHash = "hash1",
    packageName = "com.test"
    // ... Room-specific fields
)
dao.insert(entity)

// NEW (SQLDelight)
val dto = ScrapedElementDTO(
    elementHash = "hash1",
    packageName = "com.test"
    // ... SQLDelight-specific fields
)
repository.insert(dto)
```

**Action Required:**
1. Wait for Agent 2 to create scraping DTOs
2. Replace `Entity` → `DTO` in all tests
3. Replace `dao.method()` → `repository.method()`
4. Update assertions for DTO structure

**Time:** 2 hours
**Expected Result:** 12/12 pass

#### C2: LearnApp Tests (8 tests) - BLOCKED BY AGENT 1

| File | Tests | Missing Dependencies | Agent |
|------|-------|---------------------|-------|
| (File location TBD) | ~8 | LearnedAppDTO | Agent 1 |
| (File location TBD) | | ExplorationSessionDTO | Agent 1 |
| (File location TBD) | | LearnAppRepository | Agent 1 |

**Tests Expected:**
- App launch detection
- Session creation
- Screen state capture
- Navigation graph building
- Exploration progress tracking
- Fingerprinting accuracy
- Element classification
- Command generation

**Action Required:**
1. Wait for Agent 1 to create LearnApp adapter layer
2. Create tests using LearnApp DTOs
3. Test repository operations
4. Test workflow integration

**Time:** 1.5 hours
**Expected Result:** 8/8 pass

#### C3: Integration Tests (7 tests) - BLOCKED BY AGENT 3

| File | Tests | Missing Dependencies | Agent |
|------|-------|---------------------|-------|
| accessibility/integration/UUIDCreatorIntegrationTest.kt | ~7 | Full service integration | Agent 3 |

**Tests:**
- UUID generation in live service
- UUID registration with UUIDCreator
- Element-UUID linking
- Hierarchy UUID propagation
- UUID persistence across sessions
- UUID collision handling
- UUID performance under load

**Action Required:**
1. Wait for Agent 3 to enable service integrations
2. Update tests to use live service
3. Test end-to-end workflows
4. Verify integration points

**Time:** 1 hour
**Expected Result:** 7/7 pass

---

### 🆕 Category D: Integration Tests (4 tests) - CREATE

**Status:** Need to create after all dependencies met

**Location:** `/modules/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/integration/`

#### D1: LearnAppWorkflowTest.kt (1 test) - BLOCKED BY AGENTS 1+3

**Purpose:** End-to-end app learning workflow

**Test Scenario:**
```kotlin
@Test
fun `complete app learning workflow`() {
    // 1. Launch app detection
    launchApp("com.example.testapp")

    // 2. Screen state capture
    val screenState = learnAppIntegration.captureCurrentScreen()
    assertNotNull(screenState)

    // 3. Element fingerprinting
    val elements = screenState.elements
    assertTrue(elements.size > 0)

    // 4. Navigation graph building
    performAction("click_button")
    val edge = navigationGraph.getLastEdge()
    assertNotNull(edge)

    // 5. Session tracking
    val session = learnAppIntegration.getCurrentSession()
    assertEquals(2, session.screensVisited)
}
```

**Dependencies:** Agent 1 (LearnApp) + Agent 3 (Service)
**Time:** 30 minutes
**Expected Result:** 1/1 pass

#### D2: ScrapingWorkflowTest.kt (1 test) - BLOCKED BY AGENTS 2+3

**Purpose:** End-to-end element scraping flow

**Test Scenario:**
```kotlin
@Test
fun `complete scraping workflow`() {
    // 1. Element hash calculation
    val hash = scrapingIntegration.calculateElementHash(node)
    assertNotNull(hash)

    // 2. Hierarchy capture
    val hierarchy = scrapingIntegration.captureHierarchy(rootNode)
    assertTrue(hierarchy.size > 0)

    // 3. Command generation
    val commands = commandGenerator.generateCommands(elements)
    assertTrue(commands.size > 0)

    // 4. Usage tracking
    commandProcessor.executeCommand("click submit")
    val usage = database.getCommandUsage(commands[0].id)
    assertEquals(1, usage.clickCount)

    // 5. State inference
    val state = stateDetector.detectState(hierarchy)
    assertNotNull(state)
}
```

**Dependencies:** Agent 2 (Scraping) + Agent 3 (Service)
**Time:** 30 minutes
**Expected Result:** 1/1 pass

#### D3: VoiceOSServiceLifecycleTest.kt (1 test) - BLOCKED BY AGENT 3

**Purpose:** Service start/stop/crash recovery

**Test Scenario:**
```kotlin
@Test
fun `service lifecycle is robust`() {
    // 1. Service initialization
    val service = startService()
    assertTrue(service.isServiceReady)

    // 2. Integration loading
    assertNotNull(service.learnAppIntegration)
    assertNotNull(service.scrapingIntegration)
    assertNotNull(service.voiceCommandProcessor)

    // 3. Error handling
    simulateError()
    assertTrue(service.isServiceReady) // Should recover

    // 4. Graceful shutdown
    service.onDestroy()
    assertNull(service.learnAppIntegration)

    // 5. Memory cleanup
    val memoryUsage = getMemoryUsage()
    assertTrue(memoryUsage < 100_000_000) // <100MB
}
```

**Dependencies:** Agent 3 (Service)
**Time:** 20 minutes
**Expected Result:** 1/1 pass

#### D4: PerformanceBaselineTest.kt (1 test) - BLOCKED BY ALL AGENTS

**Purpose:** Query performance benchmarking

**Test Scenario:**
```kotlin
@Test
fun `database queries meet performance targets`() {
    // Setup: Insert 10,000 test records
    insertTestData(10_000)

    // Test 1: Simple query (<10ms)
    val start1 = System.nanoTime()
    database.getElementByHash("test-hash")
    val duration1 = (System.nanoTime() - start1) / 1_000_000
    assertTrue(duration1 < 10, "Simple query took ${duration1}ms")

    // Test 2: Complex query with joins (<50ms)
    val start2 = System.nanoTime()
    database.getElementsWithCommands("com.test.app")
    val duration2 = (System.nanoTime() - start2) / 1_000_000
    assertTrue(duration2 < 50, "Complex query took ${duration2}ms")

    // Test 3: Batch insert (<100ms for 100 records)
    val start3 = System.nanoTime()
    insertTestData(100)
    val duration3 = (System.nanoTime() - start3) / 1_000_000
    assertTrue(duration3 < 100, "Batch insert took ${duration3}ms")

    // Test 4: Transaction throughput (>100 tx/sec)
    val txCount = measureThroughput(1000)
    assertTrue(txCount > 100, "Only ${txCount} tx/sec")
}
```

**Dependencies:** All agents
**Time:** 20 minutes
**Expected Result:** 1/1 pass with all performance targets met

---

## Migration Checklist

### Pre-Migration (CURRENT STATUS)

- [x] Test infrastructure created
- [x] Test inventory complete
- [x] Dependencies documented
- [ ] Compilation succeeds (BLOCKING)
- [ ] Agent 1 complete (BLOCKING)
- [ ] Agent 2 complete (BLOCKING)
- [ ] Agent 3 complete (BLOCKING)

### Phase 4a: Infrastructure Tests (15 min)

- [ ] Verify compilation: `./gradlew compileDebugKotlin`
- [ ] Run database tests: `testDebugUnitTest --tests "database.*"`
- [ ] Verify 19/19 tests pass
- [ ] Check test execution time (<1 minute target)

### Phase 4b: Move Accessibility Tests (1 hour)

- [ ] Move handlers/*.kt (3 files)
- [ ] Move tree/*.kt (1 file)
- [ ] Move overlays/*.kt (2 files)
- [ ] Move lifecycle/*.kt (4 files)
- [ ] Move utils/*.kt (2 files)
- [ ] Move mocks/*.kt (2 files)
- [ ] Move test/*.kt (3 files)
- [ ] Run accessibility tests: `testDebugUnitTest --tests "accessibility.*"`
- [ ] Verify 51/51 tests pass
- [ ] Fix any import issues

### Phase 4c: Migrate Database Tests (2-3 hours)

#### Scraping Tests (2 hours)
- [ ] Wait for Agent 2 to complete scraping DTOs
- [ ] Update UUIDIntegrationTest.kt (10 tests)
- [ ] Update CachedElementHierarchyTest.kt
- [ ] Update DataFlowValidationTest.kt
- [ ] Update HierarchyIntegrityTest.kt
- [ ] Update ScrapingDatabaseSyncTest.kt
- [ ] Run scraping tests: `testDebugUnitTest --tests "scraping.*"`
- [ ] Verify 12/12 tests pass

#### LearnApp Tests (1 hour)
- [ ] Wait for Agent 1 to complete LearnApp adapter
- [ ] Create/update LearnApp repository tests (8 tests)
- [ ] Run LearnApp tests: `testDebugUnitTest --tests "learnapp.*"`
- [ ] Verify 8/8 tests pass

#### Integration Tests (30 min)
- [ ] Wait for Agent 3 to complete service integration
- [ ] Update UUIDCreatorIntegrationTest.kt (7 tests)
- [ ] Run integration tests: `testDebugUnitTest --tests "integration.*"`
- [ ] Verify 7/7 tests pass

### Phase 4d: Create Integration Tests (1 hour)

- [ ] Create LearnAppWorkflowTest.kt (1 test)
- [ ] Create ScrapingWorkflowTest.kt (1 test)
- [ ] Create VoiceOSServiceLifecycleTest.kt (1 test)
- [ ] Create PerformanceBaselineTest.kt (1 test)
- [ ] Run integration tests
- [ ] Verify 4/4 tests pass

### Phase 4e: Full Test Suite (30 min)

- [ ] Run full test suite: `./gradlew :modules:apps:VoiceOSCore:testDebugUnitTest`
- [ ] Verify 101/101 tests pass
- [ ] Generate coverage report: `./gradlew jacocoTestReport`
- [ ] Verify coverage ≥90% on migrated code
- [ ] Run tests 3 times to check for flakiness
- [ ] Document any failures
- [ ] Mark Phase 4 complete

---

## Success Criteria

### Must Pass
- [ ] All 101 tests pass (100% pass rate)
- [ ] Test coverage ≥90% on migrated code
- [ ] No flaky tests (3 consecutive runs identical)
- [ ] Test execution time <5 minutes total
- [ ] Zero compilation errors
- [ ] Zero test warnings

### Nice to Have
- [ ] Performance tests show <100ms p95
- [ ] Memory usage tests show <100MB
- [ ] Test documentation complete
- [ ] Test patterns documented for future tests

---

## Files Created by Agent 4

1. `/docs/PHASE4-TEST-MIGRATION-STATUS.md` - Comprehensive status
2. `/docs/AGENT4-EXECUTIVE-SUMMARY.md` - Executive summary
3. `/docs/PHASE4-TEST-INVENTORY.md` - This file
4. `/modules/apps/VoiceOSCore/src/test/java/.../TestDatabaseFactory.kt`
5. `/modules/apps/VoiceOSCore/src/test/java/.../BaseRepositoryTest.kt`

---

**Status:** Infrastructure complete, test execution blocked by compilation
**Next Step:** Wait for Agents 1-3, then execute migration in 5-6 hours
**Test Count:** 101 total (19 ready + 51 move + 27 migrate + 4 create)
