<!--
filename: VoiceOSCore-JVM-Migration-TODO-251016-1445.md
created: 2025-10-16 14:45:00 PDT
author: Manoj Jhawar
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
reviewed-by: CCA (Claude Code by Anthropic)
purpose: Phased TODO tracking for JVM test module migration
-->

# VoiceOSCore JVM Test Module Migration - Phased TODO

**Created:** 2025-10-16 14:45:00 PDT
**Last Updated:** 2025-10-16 14:45:00 PDT
**Status:** Phase 1 Complete (All Files Copied)
**Target:** `/tests/voiceoscore-unit-tests/`

---

## Migration Overview

**Total Files:** 13 test files
**Current Phase:** Phase 2 - Module Configuration
**Progress:** 13/13 files copied (100%), 0/13 configured (0%)

**Phases:**
- ✅ **Phase 1: COPY** - Copy files to JVM module structure
- ⏳ **Phase 2: CONFIGURE** - Create build.gradle.kts, add to settings.gradle.kts
- ⏳ **Phase 3: ANALYZE** - Identify Android-specific imports
- ⏳ **Phase 4: FIX_IMPORTS** - Apply MockK/Robolectric compatibility
- ⏳ **Phase 5: COMPILE** - Verify compilation in JVM module
- ⏳ **Phase 6: TEST** - Execute and verify tests pass
- ⏳ **Phase 7: ARCHIVE** - Archive original Android test files

---

## Module Configuration Tasks

### CONF-01: Create build.gradle.kts ⏳ IN PROGRESS
**Status:** Not started
**Priority:** P1 - BLOCKING all other work
**File:** `/tests/voiceoscore-unit-tests/build.gradle.kts`

**Required Configuration:**
- [ ] Apply Kotlin JVM plugin (NOT Android)
- [ ] Set Java 17 compatibility
- [ ] Add JUnit 5 dependencies (jupiter-api, jupiter-engine)
- [ ] Add Kotlin test dependencies
- [ ] Add Coroutines test support
- [ ] Add MockK for mocking
- [ ] Add Robolectric for Android framework mocking
- [ ] Add Room dependencies (runtime, ktx, testing)
- [ ] Add project dependencies (VoiceOSCore module)
- [ ] Configure JUnit Platform test task
- [ ] Set source/test directories

**Dependencies Needed:**
```kotlin
// JUnit 5
testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

// Kotlin
testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

// Mocking
testImplementation("io.mockk:mockk:1.13.8")

// Robolectric (for Android framework)
testImplementation("org.robolectric:robolectric:4.11.1")

// Room
testImplementation("androidx.room:room-runtime:2.6.1")
testImplementation("androidx.room:room-ktx:2.6.1")
testImplementation("androidx.room:room-testing:2.6.1")

// Project
testImplementation(project(":modules:apps:VoiceOSCore"))
```

**Deliverable:** Completed build.gradle.kts file

---

### CONF-02: Register Module in settings.gradle.kts ⏳ PENDING
**Status:** Not started
**Priority:** P1 - BLOCKING
**File:** `/settings.gradle.kts`

**Task:** Add line:
```kotlin
include(":tests:voiceoscore-unit-tests")
```

**Deliverable:** Module registered and recognized by Gradle

---

### CONF-03: Verify Module Recognition ⏳ PENDING
**Status:** Not started
**Priority:** P1
**Command:** `./gradlew :tests:voiceoscore-unit-tests:tasks`

**Expected:** Module appears in Gradle project structure

**Deliverable:** Confirmation that Gradle recognizes the module

---

## Implementation Test Files (7 files)

### IMPL-01: DatabaseManagerImplTest.kt ✅ Phase 1 Complete
**Size:** 66K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../impl/`
**Tests:** ~100 tests
**Android Imports:** Room (Database, RoomDatabase, Query), Context

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Identified as Strategy 2 (Robolectric)
- ⏳ Phase 4: FIX_IMPORTS - Not started
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - Not started
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** Strategy 2 (Robolectric)
- Uses Room database heavily
- Needs in-memory database with Robolectric
- Context required for Room initialization

**Next Action:** Apply Robolectric setup after module configured

---

### IMPL-02: ServiceMonitorImplTest.kt ✅ Phase 1 Complete
**Size:** 44K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../impl/`
**Tests:** ~100 tests
**Android Imports:** AccessibilityService, AccessibilityEvent

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Identified as Strategy 1 (MockK)
- ⏳ Phase 4: FIX_IMPORTS - Not started
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - Not started
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** Strategy 1 (MockK)
- Mock AccessibilityService
- Mock AccessibilityEvent
- No real Android framework needed

**Next Action:** Replace Android mocks with MockK after module configured

---

### IMPL-03: StateManagerImplTest.kt ✅ Phase 1 Complete
**Size:** 32K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../impl/`
**Tests:** ~100 tests
**Android Imports:** None

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Pure Kotlin (no Android imports)
- ⏳ Phase 4: FIX_IMPORTS - No fixes needed
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - Not started
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** None needed (pure Kotlin)

**Next Action:** Should compile immediately once module configured

---

### IMPL-04: UIScrapingServiceImplTest.kt ✅ Phase 1 Complete
**Size:** 43K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../impl/`
**Tests:** ~100 tests
**Android Imports:** AccessibilityNodeInfo, AccessibilityWindowInfo, Rect

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Identified as Strategy 1 (MockK)
- ⏳ Phase 4: FIX_IMPORTS - Not started
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - Not started
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** Strategy 1 (MockK)
- Mock AccessibilityNodeInfo
- Mock AccessibilityWindowInfo
- Mock Rect
- No real Android framework needed

**Next Action:** Replace Android mocks with MockK after module configured

---

### IMPL-05: CommandOrchestratorImplTest.kt ✅ Phase 1 Complete
**Size:** 60K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../impl/`
**Tests:** ~80 tests
**Android Imports:** None

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Pure Kotlin (no Android imports)
- ⏳ Phase 4: FIX_IMPORTS - No fixes needed
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - Not started
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** None needed (pure Kotlin)

**Next Action:** Should compile immediately once module configured

---

### IMPL-06: EventRouterImplTest.kt ✅ Phase 1 Complete
**Size:** 21K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../impl/`
**Tests:** ~60 tests
**Android Imports:** None

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Pure Kotlin (no Android imports)
- ⏳ Phase 4: FIX_IMPORTS - No fixes needed
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - Not started
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** None needed (pure Kotlin)

**Next Action:** Should compile immediately once module configured

---

### IMPL-07: SpeechManagerImplTest.kt ✅ Phase 1 Complete
**Size:** 35K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../impl/`
**Tests:** ~76 tests
**Android Imports:** None

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Pure Kotlin (no Android imports)
- ⏳ Phase 4: FIX_IMPORTS - No fixes needed
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - Not started
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** None needed (pure Kotlin)

**Next Action:** Should compile immediately once module configured

---

## Integration Test Files (3 files)

### INTG-01: DIPerformanceTest.kt ✅ Phase 1 Complete
**Size:** 12K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../integration/`
**Tests:** ~20 tests
**Android Imports:** Hilt (HiltAndroidRule, HiltAndroidTest, HiltTestApplication)

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Identified as Hilt/DI test
- ⏳ Phase 4: FIX_IMPORTS - Decision needed on Hilt approach
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - Not started
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** TBD - Hilt tests need special handling
- Option A: Use Robolectric for full Hilt support
- Option B: Manual DI without Hilt for JVM tests
- Option C: Keep as Android instrumented test

**Next Action:** Decide on Hilt strategy, likely Robolectric

---

### INTG-02: HiltDITest.kt ✅ Phase 1 Complete
**Size:** 10K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../integration/`
**Tests:** ~15 tests
**Android Imports:** Hilt (HiltAndroidRule, HiltAndroidTest, HiltTestApplication)

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Identified as Hilt/DI test
- ⏳ Phase 4: FIX_IMPORTS - Decision needed on Hilt approach
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - Not started
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** TBD - Same as DIPerformanceTest

**Next Action:** Decide on Hilt strategy, likely Robolectric

---

### INTG-03: MockImplementationsTest.kt ✅ Phase 1 Complete
**Size:** 12K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../integration/`
**Tests:** ~18 tests
**Android Imports:** None

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Pure Kotlin (no Android imports)
- ⏳ Phase 4: FIX_IMPORTS - No fixes needed
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - Not started
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** None needed (pure Kotlin)

**Next Action:** Should compile immediately once module configured

---

## Utility Test Files (3 files)

### UTIL-01: RefactoringTestFixtures.kt ✅ Phase 1 Complete
**Size:** 14K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../utils/`
**Tests:** N/A (utility/fixture class)
**Android Imports:** AccessibilityNodeInfo, AccessibilityEvent, Room

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Identified as Strategy 1+2 (MockK + Robolectric)
- ⏳ Phase 4: FIX_IMPORTS - Not started
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - N/A (utility class)
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** Mixed (MockK for accessibility, Robolectric for Room)
- Mock AccessibilityNodeInfo and AccessibilityEvent with MockK
- Use Robolectric for Room database fixtures

**Next Action:** Apply mixed strategy after module configured

---

### UTIL-02: RefactoringTestAssertions.kt ✅ Phase 1 Complete
**Size:** 13K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../utils/`
**Tests:** N/A (utility/assertion class)
**Android Imports:** AccessibilityNodeInfo

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Identified as Strategy 1 (MockK)
- ⏳ Phase 4: FIX_IMPORTS - Not started
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - N/A (utility class)
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** Strategy 1 (MockK)
- Mock AccessibilityNodeInfo for assertions

**Next Action:** Replace Android mocks with MockK after module configured

---

### UTIL-03: RefactoringTestUtils.kt ✅ Phase 1 Complete
**Size:** 11K
**Location:** `/tests/voiceoscore-unit-tests/src/test/kotlin/.../utils/`
**Tests:** N/A (utility class)
**Android Imports:** None

**Phase Status:**
- ✅ Phase 1: COPY - Copied successfully
- ⏳ Phase 2: CONFIGURE - Waiting for build.gradle.kts
- ⏳ Phase 3: ANALYZE - Pure Kotlin (no Android imports)
- ⏳ Phase 4: FIX_IMPORTS - No fixes needed
- ⏳ Phase 5: COMPILE - Not started
- ⏳ Phase 6: TEST - N/A (utility class)
- ⏳ Phase 7: ARCHIVE - Not started

**Compatibility Strategy:** None needed (pure Kotlin)

**Next Action:** Should compile immediately once module configured

---

## Archive Tasks

### ARCH-01: Create Archive Directory ⏳ PENDING
**Status:** Not started
**Priority:** P3 - After all tests verified
**Location:** `/modules/apps/VoiceOSCore/src/test-archived/`

**Task:** Create directory structure for archiving original Android tests

**Deliverable:** Archive directory created

---

### ARCH-02: Move Original Test Files ⏳ PENDING
**Status:** Not started
**Priority:** P3 - After all tests verified
**Dependencies:** All phases 1-6 complete for all files

**Files to Archive (13):**
- [ ] DatabaseManagerImplTest.kt
- [ ] ServiceMonitorImplTest.kt
- [ ] StateManagerImplTest.kt
- [ ] UIScrapingServiceImplTest.kt
- [ ] CommandOrchestratorImplTest.kt
- [ ] EventRouterImplTest.kt
- [ ] SpeechManagerImplTest.kt
- [ ] DIPerformanceTest.kt
- [ ] HiltDITest.kt
- [ ] MockImplementationsTest.kt
- [ ] RefactoringTestFixtures.kt
- [ ] RefactoringTestAssertions.kt
- [ ] RefactoringTestUtils.kt

**Deliverable:** Original tests archived, JVM module as primary test location

---

### ARCH-03: Update Documentation ⏳ PENDING
**Status:** Not started
**Priority:** P3
**Files to Update:**
- [ ] VoiceOSCore module changelog
- [ ] Project status report
- [ ] This TODO (mark complete)
- [ ] Architecture documentation (reference JVM test module)

**Deliverable:** All documentation reflects JVM test module as standard approach

---

## Progress Summary

### By Phase
- **Phase 1 (COPY):** 13/13 files (100%) ✅
- **Phase 2 (CONFIGURE):** 0/3 tasks (0%) ⏳
- **Phase 3 (ANALYZE):** 13/13 files (100%) ✅
- **Phase 4 (FIX_IMPORTS):** 0/10 files (0%) ⏳
- **Phase 5 (COMPILE):** 0/13 files (0%) ⏳
- **Phase 6 (TEST):** 0/13 files (0%) ⏳
- **Phase 7 (ARCHIVE):** 0/13 files (0%) ⏳

### By Category
**Implementation Tests (7):**
- Pure Kotlin (no Android): 4 files (IMPL-03, IMPL-05, IMPL-06, IMPL-07)
- Strategy 1 (MockK): 2 files (IMPL-02, IMPL-04)
- Strategy 2 (Robolectric): 1 file (IMPL-01)

**Integration Tests (3):**
- Pure Kotlin: 1 file (INTG-03)
- Hilt/DI (TBD): 2 files (INTG-01, INTG-02)

**Utility Files (3):**
- Pure Kotlin: 1 file (UTIL-03)
- Strategy 1 (MockK): 1 file (UTIL-02)
- Mixed Strategy: 1 file (UTIL-01)

---

## Next Steps (Priority Order)

### Immediate (P1)
1. ✅ **Create this TODO document** - Complete
2. ⏳ **CONF-01:** Create build.gradle.kts for JVM module
3. ⏳ **CONF-02:** Register module in settings.gradle.kts
4. ⏳ **CONF-03:** Verify Gradle recognizes module

### Short-term (P2)
5. ⏳ **Apply Strategy 1 fixes** (6 files using MockK)
6. ⏳ **Apply Strategy 2 fixes** (2 files using Robolectric)
7. ⏳ **Decide Hilt strategy** for integration tests
8. ⏳ **Compile JVM module** and fix any errors
9. ⏳ **Run tests** and verify all pass

### Long-term (P3)
10. ⏳ **Archive original tests** once JVM tests verified
11. ⏳ **Update documentation** across project
12. ⏳ **Update TODO** to mark complete

---

## Risks & Mitigations

### Risk 1: Hilt Tests May Not Work in Pure JVM
**Probability:** High
**Impact:** Medium
**Mitigation:**
- Option A: Use Robolectric for full Hilt support (recommended)
- Option B: Rewrite as manual DI tests (more work)
- Option C: Keep as Android instrumented tests (separate from JVM module)

### Risk 2: Room Database Tests May Be Complex
**Probability:** Medium
**Impact:** Medium
**Mitigation:**
- Use Robolectric's in-memory database support
- Follow Room testing best practices
- DatabaseManagerImplTest already has extensive test coverage to validate against

### Risk 3: Test Execution Performance
**Probability:** Low
**Impact:** Low
**Mitigation:**
- JVM tests should be faster than Android tests
- Monitor execution time during Phase 6
- Can parallelize test execution if needed

---

## Reference Documentation

**Architecture:**
- `/docs/voiceos-master/architecture/VoiceOSCore-JVM-Test-Module-Architecture-251016-1434.md`

**Previous Work:**
- Status: `/docs/voiceos-master/status/VoiceOSService-Additional-Compilation-Fixes-251016-1259.md`
- TODO: `/docs/voiceos-master/tasks/VoiceOSService-Refactoring-TODO-251016-1259.md`

**Project Instructions:**
- VOS4: `/Volumes/M Drive/Coding/vos4/CLAUDE.md`
- General: `/Volumes/M Drive/Coding/Docs/agents/claude/CLAUDE.md`

---

**Document Version:** 1.0
**Next Update:** After CONF-01, CONF-02, CONF-03 completion
**Estimated Completion:** TBD based on Phase 2 progress

---

*Current Phase: Module Configuration - Create build.gradle.kts and register module*
