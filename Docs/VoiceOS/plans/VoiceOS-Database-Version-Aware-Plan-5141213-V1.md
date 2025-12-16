# Implementation Plan: Version-Aware Command Lifecycle Management

**Plan ID:** VoiceOS-Plan-VersionAwareCommandManagement-51213-V1
**Version:** 1.0
**Date:** 2025-12-13
**Spec:** VoiceOS-Spec-VersionAwareCommandManagement-51213-V1.md
**Author:** Manoj Jhawar
**Estimated Duration:** 6 weeks (1 developer) | 2 weeks (swarm with 3 agents)

---

## Executive Summary

### Overview

This plan implements version-aware command lifecycle management to prevent database bloat from app updates. The system will track app versions, intelligently deprecate outdated commands, and implement automatic cleanup.

### Platforms

- ✅ **Android** (Kotlin) - Version detection, UI, WorkManager
- ✅ **KMP** (SQLDelight) - Database schema, repositories
- ❌ iOS - Not applicable
- ❌ Web - Not applicable
- ❌ Backend - Not applicable

### Complexity Assessment

| Metric | Value | Assessment |
|--------|-------|------------|
| Platforms | 1 (Android/KMP) | Medium |
| Total Tasks | 42 | High |
| Database Changes | Schema v2→v3 | High Risk |
| New Classes | 8 | Medium |
| Tests Required | 60+ | High |
| Swarm Recommended | **YES** | 42 tasks, critical path dependencies |

### Success Metrics

| Metric | Baseline | Target |
|--------|----------|--------|
| Commands per app | 1,000-5,000 | 100-200 |
| Database size (100 apps) | ~500MB | ~50MB |
| Query performance | Linear O(n) | Constant O(1) |
| Command relevance | ~20% | ~95% |

---

## Implementation Phases

### Phase 1: Database Schema & Migration (Week 1)

**Priority:** P0 - Critical Foundation
**Estimated:** 5 days
**Dependencies:** None
**Risk:** High (schema migration on production data)

#### Tasks

**1.1 Create Migration SQL**
- **File:** `Modules/VoiceOS/core/database/src/commonMain/sqldelight/migrations/2.sqm`
- **Action:** Add 4 columns (appVersion, versionCode, lastVerified, isDeprecated)
- **Action:** Create 2 indexes (idx_gc_app_version, idx_gc_last_verified)
- **Validation:** Test on empty DB, 100 command DB, 100K command DB
- **Time:** 4 hours

**1.2 Update DatabaseMigrations.kt**
- **File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/migrations/DatabaseMigrations.kt`
- **Action:** Add `migrateV2ToV3()` method
- **Action:** Execute column additions and index creation
- **Action:** Update `migrate()` to handle v2→v3
- **Validation:** Migration completes without errors
- **Time:** 3 hours

**1.3 Update Schema Queries**
- **File:** `Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/GeneratedCommand.sq`
- **Action:** Add new queries:
  - `markVersionDeprecated`
  - `updateCommandVersion`
  - `updateCommandDeprecated`
  - `deleteDeprecatedCommands`
  - `getDeprecatedCommands`
  - `getActiveCommands`
- **Action:** Update existing INSERT/UPDATE queries to include new columns
- **Time:** 4 hours

**1.4 Update GeneratedCommandDTO**
- **File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/dto/GeneratedCommandDTO.kt`
- **Action:** Add 4 new fields with defaults
- **Action:** Update mapper extension `toGeneratedCommandDTO()`
- **Validation:** Compilation succeeds
- **Time:** 2 hours

**1.5 Migration Testing**
- **File:** `Modules/VoiceOS/core/database/src/androidInstrumentedTest/kotlin/com/augmentalis/database/MigrationV2ToV3Test.kt`
- **Action:** Test empty database migration
- **Action:** Test migration with existing data (100, 1K, 10K, 100K commands)
- **Action:** Verify indexes created correctly (EXPLAIN QUERY PLAN)
- **Action:** Test rollback on failure
- **Tests:** 8 test cases
- **Time:** 6 hours

**1.6 Backfill Script**
- **File:** `Modules/VoiceOS/core/database/src/androidInstrumentedTest/kotlin/com/augmentalis/database/BackfillVersionDataTest.kt`
- **Action:** Create utility to backfill existing commands with current app versions
- **Action:** Handle apps no longer installed
- **Action:** Test backfill performance
- **Tests:** 4 test cases
- **Time:** 4 hours

**Phase 1 Deliverables:**
- ✅ Migration SQL file (2.sqm)
- ✅ Updated DatabaseMigrations.kt
- ✅ 6 new SQL queries
- ✅ Updated DTO with 4 new fields
- ✅ 12 migration tests passing
- ✅ Backfill script tested

**Phase 1 Total Time:** 23 hours (~3 days)

---

### Phase 2: Repository Layer (Week 1-2)

**Priority:** P0 - Critical Foundation
**Estimated:** 3 days
**Dependencies:** Phase 1 complete
**Risk:** Medium

#### Tasks

**2.1 Update IGeneratedCommandRepository Interface**
- **File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/IGeneratedCommandRepository.kt`
- **Action:** Add version management methods:
  ```kotlin
  suspend fun markVersionDeprecated(packageName: String, oldVersionCode: Int): Int
  suspend fun updateCommandVersion(id: Long, versionCode: Int, lastVerified: Long, isDeprecated: Long)
  suspend fun updateCommandDeprecated(id: Long, isDeprecated: Long)
  suspend fun deleteDeprecatedCommands(olderThan: Long, keepUserApproved: Boolean): Int
  suspend fun getDeprecatedCommands(packageName: String): List<GeneratedCommandDTO>
  suspend fun getActiveCommands(packageName: String, versionCode: Int): List<GeneratedCommandDTO>
  ```
- **Time:** 2 hours

**2.2 Implement Repository Methods**
- **File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightGeneratedCommandRepository.kt`
- **Action:** Implement 6 new methods
- **Action:** Add input validation for each method
- **Action:** Ensure proper use of `Dispatchers.Default` (KMP compatibility)
- **Validation:** Methods compile and follow KMP patterns
- **Time:** 6 hours

**2.3 Repository Unit Tests**
- **File:** `Modules/VoiceOS/core/database/src/androidInstrumentedTest/kotlin/com/augmentalis/database/VersionManagementRepositoryTest.kt`
- **Action:** Test `markVersionDeprecated()` - marks all commands for old version
- **Action:** Test `updateCommandVersion()` - updates version and timestamp
- **Action:** Test `updateCommandDeprecated()` - marks single command
- **Action:** Test `deleteDeprecatedCommands()` - respects grace period and user-approved
- **Action:** Test `getActiveCommands()` - filters by version and deprecated status
- **Action:** Test `getDeprecatedCommands()` - returns only deprecated
- **Tests:** 15 test cases
- **Time:** 8 hours

**2.4 Update Existing Repository Methods**
- **File:** `SQLDelightGeneratedCommandRepository.kt`
- **Action:** Update `insert()` to accept version parameters
- **Action:** Update `insertBatch()` to accept version parameters
- **Action:** Update `update()` to accept version parameters
- **Action:** Add default values for backward compatibility
- **Time:** 3 hours

**2.5 Update Existing Tests**
- **Files:** All existing repository tests
- **Action:** Update test data to include version fields
- **Action:** Ensure all existing tests pass with new schema
- **Time:** 4 hours

**Phase 2 Deliverables:**
- ✅ 6 new repository methods
- ✅ Updated insert/update methods
- ✅ 15 new repository tests passing
- ✅ All existing tests updated and passing

**Phase 2 Total Time:** 23 hours (~3 days)

---

### Phase 3: Version Detection Service (Week 2)

**Priority:** P0 - Critical Business Logic
**Estimated:** 4 days
**Dependencies:** Phase 2 complete
**Risk:** Medium (PackageManager API differences across Android versions)

#### Tasks

**3.1 Create AppVersion Data Class**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/version/AppVersion.kt`
- **Action:** Create data class with versionName, versionCode
- **Action:** Add toString() for logging
- **Time:** 1 hour

**3.2 Create VersionChange Sealed Class**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/version/VersionChange.kt`
- **Action:** Create sealed class with 5 variants (FirstInstall, Updated, Downgraded, NoChange, AppNotInstalled)
- **Time:** 1 hour

**3.3 Implement AppVersionDetector**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/version/AppVersionDetector.kt`
- **Action:** Implement `getCurrentVersion(packageName)` using PackageManager
- **Action:** Handle API level differences (API < 28 vs >= 28)
- **Action:** Implement `detectVersionChange(packageName)`
- **Action:** Compare stored version with installed version
- **Action:** Add error handling for NameNotFoundException
- **Validation:** Works on API 21-34
- **Time:** 5 hours

**3.4 AppVersionDetector Unit Tests**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/version/AppVersionDetectorTest.kt`
- **Action:** Mock PackageManager
- **Action:** Test getCurrentVersion() - success case
- **Action:** Test getCurrentVersion() - app not installed
- **Action:** Test detectVersionChange() - first install
- **Action:** Test detectVersionChange() - update
- **Action:** Test detectVersionChange() - downgrade
- **Action:** Test detectVersionChange() - no change
- **Action:** Test API level compatibility (API 21 vs 28+)
- **Tests:** 10 test cases
- **Time:** 6 hours

**3.5 Create AppVersionManager**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/version/AppVersionManager.kt`
- **Action:** Implement `handleVersionUpdate()` - mark old commands for verification
- **Action:** Implement `verifyCommand()` - update version if element exists
- **Action:** Add logging for version changes
- **Action:** Calculate statistics (commands deprecated, preserved, etc.)
- **Time:** 6 hours

**3.6 AppVersionManager Integration Tests**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/androidTest/java/com/augmentalis/voiceoscore/version/AppVersionManagerTest.kt`
- **Action:** Test handleVersionUpdate() - marks 150 Gmail commands
- **Action:** Test verifyCommand() - element still exists
- **Action:** Test verifyCommand() - element not found
- **Action:** Test version update flow end-to-end
- **Tests:** 8 test cases
- **Time:** 6 hours

**Phase 3 Deliverables:**
- ✅ AppVersion data class
- ✅ VersionChange sealed class
- ✅ AppVersionDetector with API compatibility
- ✅ AppVersionManager with version handling
- ✅ 18 tests passing (10 unit + 8 integration)

**Phase 3 Total Time:** 25 hours (~3-4 days)

---

### Phase 4: Command Lifecycle Integration (Week 3)

**Priority:** P0 - Critical Integration
**Estimated:** 5 days
**Dependencies:** Phase 3 complete
**Risk:** High (modifies core JIT learning flow)

#### Tasks

**4.1 Update JustInTimeLearner Command Creation**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/jit/JustInTimeLearner.kt`
- **Action:** Inject `AppVersionDetector`
- **Action:** Get current app version before creating command
- **Action:** Pass version info to `repository.insert()`
- **Action:** Set `lastVerified` to current timestamp
- **Action:** Set `isDeprecated` to 0
- **Validation:** New commands include version info
- **Time:** 4 hours

**4.2 Implement Command Verification on Discovery**
- **File:** `JustInTimeLearner.kt`
- **Action:** On element discovered, check if command exists
- **Action:** If exists with old version → call `verifyCommand()`
- **Action:** If exists with current version → update `lastVerified`
- **Action:** If doesn't exist → create new command
- **Action:** Add telemetry logging
- **Time:** 5 hours

**4.3 Add Version Change Detection Hook**
- **File:** `JustInTimeLearner.kt` or new `VersionChangeListener.kt`
- **Action:** Detect app version change on learning session start
- **Action:** If version changed → call `handleVersionUpdate()`
- **Action:** Log version change event
- **Action:** Trigger background verification if needed
- **Time:** 4 hours

**4.4 Update LearnAppCore Integration**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/core/LearnAppCore.kt`
- **Action:** Pass version info when creating commands
- **Action:** Update all command creation calls
- **Time:** 2 hours

**4.5 Update VoiceOSService Integration**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`
- **Action:** Initialize `AppVersionManager`
- **Action:** Provide version info for dynamic commands
- **Time:** 2 hours

**4.6 JustInTimeLearner Integration Tests**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/learnapp/jit/JustInTimeLearnerVersionTest.kt`
- **Action:** Test new command creation includes version
- **Action:** Test command verification on discovery
- **Action:** Test version update handling
- **Action:** Test old command upgrade to new version
- **Action:** Test deprecation marking
- **Tests:** 12 test cases
- **Time:** 8 hours

**4.7 Update All Existing Command Creation Sites**
- **Files:** 18 files that create GeneratedCommandDTO (from pagination task)
- **Action:** Update to include version parameters
- **Action:** Use current app version or default ""
- **Validation:** All compilation errors resolved
- **Time:** 4 hours

**4.8 Regression Testing**
- **Files:** All existing JIT learning tests
- **Action:** Ensure all existing tests pass
- **Action:** Update test data with version fields
- **Action:** Fix any broken tests
- **Time:** 6 hours

**Phase 4 Deliverables:**
- ✅ JustInTimeLearner version-aware
- ✅ Command creation includes version
- ✅ Command verification on discovery
- ✅ Version change detection
- ✅ 12 new integration tests
- ✅ All existing tests passing

**Phase 4 Total Time:** 35 hours (~4-5 days)

---

### Phase 5: Cleanup System (Week 4)

**Priority:** P1 - Important Automation
**Estimated:** 5 days
**Dependencies:** Phase 4 complete
**Risk:** Medium (WorkManager reliability)

#### Tasks

**5.1 Create CleanupManager**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/cleanup/CleanupManager.kt`
- **Action:** Implement `previewCleanup()` - return statistics
- **Action:** Implement `executeCleanup()` - with dry-run mode
- **Action:** Add safety check (never delete > 90%)
- **Action:** Run cleanup in transaction
- **Action:** Log cleanup results
- **Time:** 8 hours

**5.2 Create CleanupWorker (WorkManager)**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/cleanup/CleanupWorker.kt`
- **Action:** Extend CoroutineWorker
- **Action:** Inject CleanupManager
- **Action:** Execute cleanup with default params (30 days)
- **Action:** Handle errors gracefully
- **Action:** Return success/failure status
- **Time:** 4 hours

**5.3 Schedule Periodic Cleanup**
- **File:** `VoiceOSService.kt` or `CleanupWorker.kt`
- **Action:** Schedule weekly PeriodicWorkRequest
- **Action:** Set constraints (device idle, charging preferred)
- **Action:** Use ExistingPeriodicWorkPolicy.KEEP
- **Time:** 3 hours

**5.4 CleanupManager Unit Tests**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/cleanup/CleanupManagerTest.kt`
- **Action:** Test previewCleanup() - correct count
- **Action:** Test executeCleanup() - deletes only deprecated
- **Action:** Test executeCleanup() - respects grace period
- **Action:** Test executeCleanup() - preserves user-approved
- **Action:** Test safety check (> 90% deletion blocked)
- **Action:** Test dry-run mode
- **Action:** Test transaction rollback on error
- **Tests:** 10 test cases
- **Time:** 7 hours

**5.5 CleanupWorker Integration Tests**
- **File:** `Modules/VoiceOS/apps/VoiceOSCore/src/androidTest/java/com/augmentalis/voiceoscore/cleanup/CleanupWorkerTest.kt`
- **Action:** Use WorkManager test helpers
- **Action:** Test worker executes successfully
- **Action:** Test worker handles database errors
- **Action:** Test worker scheduling
- **Tests:** 5 test cases
- **Time:** 5 hours

**5.6 Performance Testing**
- **File:** `CleanupPerformanceTest.kt`
- **Action:** Benchmark cleanup with 1K deprecated commands
- **Action:** Benchmark cleanup with 10K deprecated commands
- **Action:** Benchmark cleanup with 100K deprecated commands
- **Action:** Verify < 1 second for 10K commands
- **Tests:** 3 benchmarks
- **Time:** 4 hours

**Phase 5 Deliverables:**
- ✅ CleanupManager with safety checks
- ✅ CleanupWorker with scheduling
- ✅ 15 tests passing (10 unit + 5 integration)
- ✅ Performance benchmarks meeting targets

**Phase 5 Total Time:** 31 hours (~4-5 days)

---

### Phase 6: UI Integration (Week 5)

**Priority:** P2 - Important UX
**Estimated:** 5 days
**Dependencies:** Phase 5 complete
**Risk:** Low

#### Tasks

**6.1 Update CommandListViewModel**
- **File:** `Modules/VoiceOS/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/CommandListViewModel.kt` (or similar)
- **Action:** Add version info to UI state
- **Action:** Expose active vs deprecated command counts
- **Action:** Add filter: "Show only current version"
- **Action:** Add "Verify now" action
- **Time:** 5 hours

**6.2 Create CleanupViewModel**
- **File:** `Modules/VoiceOS/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/CleanupViewModel.kt`
- **Action:** Expose cleanup preview
- **Action:** Handle grace period selection
- **Action:** Execute cleanup with confirmation
- **Action:** Show progress during cleanup
- **Time:** 6 hours

**6.3 Update Command List UI**
- **File:** Command list Composable
- **Action:** Display app version in header
- **Action:** Show active/deprecated counts
- **Action:** Add "Verify now" button
- **Action:** Show last verified timestamp
- **Action:** Add Material3 UI components
- **Time:** 6 hours

**6.4 Create Cleanup Preview Screen**
- **File:** `CleanupScreen.kt`
- **Action:** Show commands to delete count
- **Action:** Show apps affected
- **Action:** Show space freed (MB)
- **Action:** Grace period dropdown (7, 14, 30, 60, 90 days)
- **Action:** Warning about user-approved preservation
- **Action:** Cancel / Execute buttons
- **Time:** 7 hours

**6.5 Create Version History Screen (Optional)**
- **File:** `VersionHistoryScreen.kt`
- **Action:** Show last 5 app versions
- **Action:** Show command count per version
- **Action:** Show deprecated vs active per version
- **Time:** 5 hours

**6.6 UI Tests**
- **Files:** UI test files
- **Action:** Test command list displays version info
- **Action:** Test cleanup preview shows correct counts
- **Action:** Test cleanup execution updates UI
- **Action:** Test filter "current version only"
- **Tests:** 8 test cases
- **Time:** 6 hours

**Phase 6 Deliverables:**
- ✅ Updated command list with version info
- ✅ Cleanup preview screen
- ✅ Version history screen (optional)
- ✅ 8 UI tests passing

**Phase 6 Total Time:** 35 hours (~4-5 days)

---

### Phase 7: Testing & Documentation (Week 6)

**Priority:** P0 - Quality Assurance
**Estimated:** 5 days
**Dependencies:** All previous phases complete
**Risk:** Low

#### Tasks

**7.1 End-to-End Testing**
- **File:** New E2E test suite
- **Action:** Test full flow: App install → update → verification → cleanup
- **Action:** Test Gmail scenario (as per spec Appendix B)
- **Action:** Test Chrome scenario (large app with 300+ commands)
- **Action:** Test 10 app updates over 30 days
- **Tests:** 5 E2E scenarios
- **Time:** 10 hours

**7.2 Migration Testing on Production Data**
- **Action:** Export production database (anonymized)
- **Action:** Run migration v2→v3
- **Action:** Verify data integrity
- **Action:** Test query performance before/after
- **Action:** Document any issues
- **Time:** 6 hours

**7.3 Performance Benchmarking**
- **Action:** Measure command query time with 100K commands
- **Action:** Measure cleanup time with various dataset sizes
- **Action:** Measure memory usage during cleanup
- **Action:** Compare baseline vs optimized performance
- **Time:** 4 hours

**7.4 Update Developer Manual**
- **File:** `Docs/VoiceOS/manuals/developer/VoiceOS-P2-Features-Developer-Manual-51211-V1.md`
- **Action:** Add Chapter 14: Version-Aware Command Management
- **Action:** Document AppVersionManager API
- **Action:** Document CleanupManager API
- **Action:** Add code examples
- **Action:** Add troubleshooting section
- **Content:** ~300 lines
- **Time:** 6 hours

**7.5 Update User Manual**
- **File:** `Docs/VoiceOS/manuals/user/VoiceOS-P2-Features-User-Manual-51211-V1.md`
- **Action:** Add section on version management
- **Action:** Explain cleanup feature
- **Action:** Add FAQ entries
- **Content:** ~100 lines
- **Time:** 3 hours

**7.6 Create Migration Guide**
- **File:** `Docs/VoiceOS/guides/VoiceOS-Migration-V2-V3-Guide-51213-V1.md`
- **Action:** Document migration steps
- **Action:** Document rollback procedure
- **Action:** Add troubleshooting
- **Action:** Include SQL commands for manual intervention
- **Time:** 4 hours

**7.7 Code Review & Cleanup**
- **Action:** Self-review all new code
- **Action:** Remove debug logging
- **Action:** Ensure consistent formatting
- **Action:** Add missing KDoc comments
- **Action:** Check for TODOs and FIXMEs
- **Time:** 5 hours

**Phase 7 Deliverables:**
- ✅ 5 E2E tests passing
- ✅ Migration tested on production data
- ✅ Performance benchmarks documented
- ✅ Developer manual updated
- ✅ User manual updated
- ✅ Migration guide created
- ✅ Code review complete

**Phase 7 Total Time:** 38 hours (~5 days)

---

## Task Summary

### Total Tasks by Phase

| Phase | Tasks | Estimated Hours | Days |
|-------|-------|----------------|------|
| Phase 1: Database Schema | 6 | 23 | 3 |
| Phase 2: Repository Layer | 5 | 23 | 3 |
| Phase 3: Version Detection | 6 | 25 | 3-4 |
| Phase 4: Lifecycle Integration | 8 | 35 | 4-5 |
| Phase 5: Cleanup System | 6 | 31 | 4-5 |
| Phase 6: UI Integration | 6 | 35 | 4-5 |
| Phase 7: Testing & Docs | 7 | 38 | 5 |
| **TOTAL** | **44** | **210** | **~26 days** |

### Sequential vs Parallel Execution

**Sequential (1 developer):**
- 210 hours ÷ 8 hours/day = 26 working days (~6 weeks)

**Parallel (Swarm with 3 agents):**
- Phase 1-2 (Database foundation): 3 days (sequential, can't parallelize)
- Phase 3-4-5 (Business logic): 5 days (parallel across 3 agents)
- Phase 6 (UI): 5 days (parallel with Phase 5)
- Phase 7 (Testing): 5 days (sequential, integration testing)
- **Total: ~10-12 working days (~2 weeks)**

**Swarm Savings:** 14 days (54% reduction)

---

## Testing Strategy

### Test Coverage Goals

| Layer | Target Coverage | Test Count |
|-------|----------------|------------|
| Repository | 95% | 15 |
| Business Logic | 90% | 30 |
| UI | 80% | 8 |
| Integration | 100% critical paths | 13 |
| E2E | 5 scenarios | 5 |
| **TOTAL** | **~90%** | **71** |

### Test Types

**Unit Tests (45):**
- Repository methods (15)
- AppVersionDetector (10)
- AppVersionManager (8)
- CleanupManager (10)
- ViewModel logic (2)

**Integration Tests (21):**
- Migration (12)
- Version management (8)
- Cleanup worker (5)

**UI Tests (8):**
- Command list version display (3)
- Cleanup preview (3)
- Version history (2)

**E2E Tests (5):**
- Gmail update scenario
- Chrome large app scenario
- Multi-app cleanup
- Version downgrade handling
- User-approved preservation

---

## Risk Mitigation

### High-Risk Areas

**Risk 1: Database Migration Failure**
- **Mitigation:** Test on copy of production data first
- **Mitigation:** Implement rollback mechanism
- **Mitigation:** Add progress logging
- **Mitigation:** Create manual migration SQL script as fallback

**Risk 2: Performance Degradation During Cleanup**
- **Mitigation:** Batch deletions (1000 at a time)
- **Mitigation:** Run during low-usage hours (3 AM)
- **Mitigation:** Monitor cleanup duration
- **Mitigation:** Add timeout protection

**Risk 3: WorkManager Scheduling Issues**
- **Mitigation:** Test WorkManager extensively
- **Mitigation:** Add manual trigger option
- **Mitigation:** Log scheduling events
- **Mitigation:** Provide fallback to AlarmManager if needed

---

## Dependencies & Prerequisites

### Code Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| androidx.work | 2.9.0 | Background cleanup |
| SQLDelight | 2.0.0 | Database |
| Kotlin Coroutines | 1.7.3 | Async operations |

### Internal Dependencies

| Module | Reason |
|--------|--------|
| Pagination feature (completed) | Uses same database schema |
| JIT Learning Service | Core integration point |
| LearnApp UI | UI integration |

### Development Environment

- Android Studio Hedgehog or later
- Gradle 8.0+
- Java 17+
- Android SDK API 21-34

---

## Swarm Configuration

### Recommended Swarm Setup

**Swarm Size:** 3 agents
**Parallelization:** Enabled for Phases 3-6

**Agent 1: Database & Repository Specialist**
- Phases 1-2
- Focus: Schema, migrations, repository layer
- Skills: SQLDelight, database optimization

**Agent 2: Business Logic Specialist**
- Phases 3-4
- Focus: Version detection, lifecycle management
- Skills: Kotlin, Android SDK, coroutines

**Agent 3: UI & Integration Specialist**
- Phases 5-6
- Focus: Cleanup system, UI integration
- Skills: Compose, WorkManager, Material3

**All Agents: Phase 7 Testing**
- Collaborative E2E testing
- Documentation review

### Conflict Prevention

| Area | Strategy |
|------|----------|
| Database schema | Agent 1 owns exclusively (Phases 1-2) |
| JustInTimeLearner.kt | Agent 2 owns (Phase 4) |
| UI files | Agent 3 owns (Phase 6) |
| Test files | Agents own their respective test files |

**Merge Strategy:** Feature branch per agent, daily merge to main

---

## Success Criteria

### Functional Criteria

- ✅ Migration v2→v3 completes on 100K command database
- ✅ Version detection works for top 100 apps
- ✅ New commands include version info
- ✅ Deprecated commands preserved for 30 days
- ✅ User-approved commands never auto-deleted
- ✅ Cleanup reduces database by 80%+ after 50 updates
- ✅ All existing features continue working

### Performance Criteria

- ✅ Version detection: < 50ms per app
- ✅ Cleanup execution: < 1 second for 10K commands
- ✅ Query performance: Constant O(1) with indexes
- ✅ Memory usage: No increase during cleanup

### Quality Criteria

- ✅ Test coverage: 90%+
- ✅ Zero critical bugs in testing
- ✅ Documentation complete
- ✅ Code review passed

---

## Rollback Plan

### If Migration Fails

1. **Immediate:** Stop deployment
2. **Rollback:** Restore database from backup
3. **Analysis:** Review migration logs
4. **Fix:** Correct migration SQL
5. **Retest:** Test on copy of production data
6. **Retry:** Deploy with fixes

### If Cleanup Deletes Too Much

1. **Detection:** Safety check triggers (> 90% deletion)
2. **Abort:** Transaction rolls back
3. **Alert:** Log error and notify developer
4. **Investigation:** Review cleanup logic
5. **Manual Intervention:** Restore from backup if needed

---

## Next Steps

### After Plan Approval

1. **Create implementation tasks** - Use TodoWrite to track all 44 tasks
2. **Set up swarm** - Configure 3 agents with specific roles
3. **Begin Phase 1** - Database schema and migration
4. **Daily standups** - Review progress, address blockers
5. **Weekly demos** - Show completed phases to stakeholders

### Chaining Options

- ✅ **Generate tasks:** Create TodoWrite tasks for tracking
- ✅ **Start implementation:** Launch `/i.implement` with swarm
- ✅ **Full automation:** Use `.yolo` for auto-chain to completion

---

## Appendix

### A. File Tree

```
Modules/VoiceOS/
├── core/database/
│   ├── src/commonMain/
│   │   ├── sqldelight/
│   │   │   ├── migrations/
│   │   │   │   └── 2.sqm [NEW]
│   │   │   └── com/augmentalis/database/
│   │   │       └── GeneratedCommand.sq [MODIFIED]
│   │   └── kotlin/com/augmentalis/database/
│   │       ├── dto/
│   │       │   └── GeneratedCommandDTO.kt [MODIFIED]
│   │       ├── migrations/
│   │       │   └── DatabaseMigrations.kt [MODIFIED]
│   │       └── repositories/
│   │           ├── IGeneratedCommandRepository.kt [MODIFIED]
│   │           └── impl/
│   │               └── SQLDelightGeneratedCommandRepository.kt [MODIFIED]
│   └── src/androidInstrumentedTest/
│       └── kotlin/com/augmentalis/database/
│           ├── MigrationV2ToV3Test.kt [NEW]
│           ├── BackfillVersionDataTest.kt [NEW]
│           └── VersionManagementRepositoryTest.kt [NEW]
│
├── apps/VoiceOSCore/
│   └── src/main/java/com/augmentalis/voiceoscore/
│       ├── version/ [NEW PACKAGE]
│       │   ├── AppVersion.kt [NEW]
│       │   ├── VersionChange.kt [NEW]
│       │   ├── AppVersionDetector.kt [NEW]
│       │   └── AppVersionManager.kt [NEW]
│       ├── cleanup/ [NEW PACKAGE]
│       │   ├── CleanupManager.kt [NEW]
│       │   └── CleanupWorker.kt [NEW]
│       ├── learnapp/jit/
│       │   └── JustInTimeLearner.kt [MODIFIED]
│       ├── learnapp/core/
│       │   └── LearnAppCore.kt [MODIFIED]
│       └── accessibility/
│           └── VoiceOSService.kt [MODIFIED]
│
└── apps/LearnApp/ (or similar UI module)
    └── src/main/java/com/augmentalis/learnapp/
        └── ui/
            ├── CommandListViewModel.kt [MODIFIED]
            ├── CleanupViewModel.kt [NEW]
            ├── CleanupScreen.kt [NEW]
            └── VersionHistoryScreen.kt [NEW]

Docs/VoiceOS/
├── specifications/
│   └── VoiceOS-Spec-VersionAwareCommandManagement-51213-V1.md [CREATED]
├── plans/
│   └── VoiceOS-Plan-VersionAwareCommandManagement-51213-V1.md [THIS FILE]
├── manuals/developer/
│   └── VoiceOS-P2-Features-Developer-Manual-51211-V1.md [TO UPDATE]
├── manuals/user/
│   └── VoiceOS-P2-Features-User-Manual-51211-V1.md [TO UPDATE]
└── guides/
    └── VoiceOS-Migration-V2-V3-Guide-51213-V1.md [TO CREATE]
```

### B. Glossary

| Term | Definition |
|------|------------|
| **Version Code** | Integer app version (e.g., 82024 for v8.2024) |
| **Version Name** | String app version (e.g., "8.2024.11.123") |
| **Deprecated** | Command marked for deletion but not deleted yet |
| **Grace Period** | Time before deprecated commands are deleted (default 30 days) |
| **Verification** | Process of checking if element still exists |
| **Backfill** | Adding version data to existing commands |
| **Cleanup** | Deleting deprecated commands |

---

**Plan Status:** Ready for implementation
**Approval Required:** Yes
**Estimated Start:** Upon approval
**Estimated Completion:** 6 weeks (sequential) | 2 weeks (swarm)

---

**END OF IMPLEMENTATION PLAN**
