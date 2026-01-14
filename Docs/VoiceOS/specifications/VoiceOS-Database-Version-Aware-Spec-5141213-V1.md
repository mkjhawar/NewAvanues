# VoiceOS - Version-Aware Command Lifecycle Management Specification

**Document ID:** VoiceOS-Spec-VersionAwareCommandManagement-51213-V1
**Version:** 1.0
**Date:** 2025-12-13
**Author:** Manoj Jhawar
**Status:** Draft
**Platform:** Android (Kotlin), KMP (SQLDelight)
**Module:** Core Database, JIT Learning Service

---

## Executive Summary

### Overview

VoiceOS currently accumulates commands indefinitely as apps update, leading to database bloat and performance degradation. This specification defines a **Version-Aware Command Lifecycle Management** system that tracks app versions, intelligently deprecates outdated commands, and implements smart cleanup strategies.

### Business Value

| Metric | Current | Target | Impact |
|--------|---------|--------|--------|
| Commands per app (Gmail, 50 updates) | ~1,000-5,000 | ~100-200 | **80-95% reduction** |
| Database size (100 apps, 2 years) | ~500MB | ~50MB | **90% reduction** |
| Command query time | Linear degradation | Constant O(1) | **Consistent performance** |
| User-relevant commands | ~20% | ~95% | **5x improvement** |

### Key Features

1. **App Version Tracking** - Detect version changes automatically
2. **Smart Deprecation** - Mark old commands without immediate deletion
3. **Graceful Cleanup** - 30-day grace period before removal
4. **Selective Preservation** - Keep user-approved commands indefinitely
5. **Intelligent Rescan** - Only verify changed screens

---

## Problem Statement

### Current Issues

**Issue 1: Command Accumulation Over Time**
```
Gmail v1.0 (100 commands) → v2.0 (120 commands) → v3.0 (130 commands)
Result after 50 updates: 1,000-5,000 commands
Reality: Only ~150 commands are relevant for current version
```

**Issue 2: No Version Awareness**
- System cannot distinguish between v1.0 and v50.0 commands
- Outdated "compose" button command from v1.0 still exists alongside v50.0 version
- No mechanism to identify which commands work with installed app version

**Issue 3: Database Bloat**
- 100 apps × 50 updates × 100 commands/update = 500,000 commands
- Actual relevant commands: 100 apps × 150 commands = 15,000 commands
- **97% of database is obsolete data**

**Issue 4: Performance Degradation**
- Query time increases linearly with obsolete commands
- Pagination helps but doesn't solve root cause
- Memory usage includes deprecated commands

### User Impact

**Developer Experience:**
- Cluttered command lists with duplicate/obsolete commands
- Difficult to debug which command version is executing
- Slow performance after extended usage

**End User Experience:**
- Commands may fail if they reference removed UI elements
- Voice recognition confused by multiple similar commands
- Slower app launch as database grows

---

## Functional Requirements

### FR-1: App Version Detection

**Priority:** P0 (Critical)
**Platform:** Android

**Requirements:**
1. System SHALL detect installed app version using PackageManager
2. System SHALL extract both versionName (string) and versionCode (integer)
3. System SHALL detect version changes on:
   - App launch
   - JIT learning session start
   - Manual user-triggered rescan
4. System SHALL handle version downgrades (rare but possible)

**API:**
```kotlin
interface AppVersionDetector {
    fun getCurrentVersion(packageName: String): AppVersion?
    fun detectVersionChange(packageName: String): VersionChange
}

data class AppVersion(
    val versionName: String,  // "8.2024.11.123"
    val versionCode: Int      // 82024
)

sealed class VersionChange {
    data class FirstInstall(val version: AppVersion) : VersionChange()
    data class Updated(val oldVersion: Int, val newVersion: Int) : VersionChange()
    data class Downgraded(val oldVersion: Int, val newVersion: Int) : VersionChange()
    data class NoChange(val version: Int) : VersionChange()
    object AppNotInstalled : VersionChange()
}
```

**Acceptance Criteria:**
- ✓ Detects Gmail update from v2024.10 to v2024.11 within 1 second
- ✓ Handles apps not installed (returns AppNotInstalled)
- ✓ Correctly identifies downgrades (e.g., beta → stable)
- ✓ Works on Android API 21+ (versionCode compatibility)

---

### FR-2: Database Schema Extensions

**Priority:** P0 (Critical)
**Platform:** KMP (SQLDelight)

**Requirements:**
1. System SHALL add `appVersion` column (TEXT, app version string)
2. System SHALL add `versionCode` column (INTEGER, numeric version)
3. System SHALL add `lastVerified` column (INTEGER, timestamp when element last seen)
4. System SHALL add `isDeprecated` column (INTEGER, 0=active, 1=deprecated)
5. System SHALL create composite index on (appId, versionCode, isDeprecated)
6. System SHALL create index on (lastVerified, isDeprecated) for cleanup queries

**Schema:**
```sql
-- Migration v2 → v3
ALTER TABLE commands_generated
ADD COLUMN appVersion TEXT NOT NULL DEFAULT '';

ALTER TABLE commands_generated
ADD COLUMN versionCode INTEGER NOT NULL DEFAULT 0;

ALTER TABLE commands_generated
ADD COLUMN lastVerified INTEGER;

ALTER TABLE commands_generated
ADD COLUMN isDeprecated INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_gc_app_version
ON commands_generated(appId, versionCode, isDeprecated);

CREATE INDEX IF NOT EXISTS idx_gc_last_verified
ON commands_generated(lastVerified, isDeprecated);
```

**Acceptance Criteria:**
- ✓ Migration executes without errors on database with 100,000 existing commands
- ✓ Default values applied correctly to existing records
- ✓ Indexes improve query performance (measured via EXPLAIN QUERY PLAN)
- ✓ Backward compatible: old code continues working with defaults

---

### FR-3: Command Version Lifecycle

**Priority:** P0 (Critical)
**Platform:** Android, KMP

**Requirements:**

**3.1 New Command Creation**
- System SHALL capture app version when creating new command
- System SHALL set lastVerified to current timestamp
- System SHALL set isDeprecated to 0 (active)

**3.2 Command Verification (Element Still Exists)**
- System SHALL update versionCode to current version
- System SHALL update lastVerified to current timestamp
- System SHALL set isDeprecated to 0 (active)
- System SHALL preserve usageCount, isUserApproved, and other metadata

**3.3 Command Deprecation (Element No Longer Exists)**
- System SHALL set isDeprecated to 1
- System SHALL NOT delete immediately (grace period)
- System SHALL preserve command for historical reference

**3.4 Version Update Handling**
- When app updates from v100 to v101:
  - System SHALL mark ALL v100 commands as "pending verification"
  - System SHALL NOT mark as deprecated immediately
  - System SHALL verify commands as they are encountered during normal usage
  - System SHALL mark unverified commands deprecated after 30 days

**API:**
```kotlin
interface CommandLifecycleManager {
    suspend fun createCommand(
        elementHash: String,
        packageName: String,
        currentVersion: AppVersion,
        // ... other parameters
    ): Long

    suspend fun verifyCommand(
        commandId: Long,
        elementStillExists: Boolean,
        currentVersion: AppVersion
    )

    suspend fun handleVersionUpdate(
        packageName: String,
        oldVersion: Int,
        newVersion: Int
    ): UpdateResult
}

data class UpdateResult(
    val packageName: String,
    val oldVersion: Int,
    val newVersion: Int,
    val commandsMarkedForVerification: Int,
    val commandsDeprecated: Int,
    val commandsPreserved: Int
)
```

**Acceptance Criteria:**
- ✓ Gmail updates from v100 to v101: all 150 commands marked for verification
- ✓ User continues using Gmail: 140 commands verified as still valid
- ✓ After 30 days: 10 unverified commands marked deprecated
- ✓ User-approved commands never auto-deprecated

---

### FR-4: Smart Cleanup Strategy

**Priority:** P1 (High)
**Platform:** Android, KMP

**Requirements:**

**4.1 Automatic Cleanup**
- System SHALL run cleanup weekly via WorkManager
- System SHALL delete commands matching ALL criteria:
  - isDeprecated = 1
  - lastVerified > 30 days ago
  - isUserApproved = 0
- System SHALL log cleanup statistics

**4.2 Manual Cleanup**
- User SHALL be able to trigger cleanup via LearnApp UI
- User SHALL see preview before deletion (count, apps affected)
- User SHALL be able to adjust grace period (7, 14, 30, 60, 90 days)

**4.3 Preservation Rules**
- System SHALL NEVER delete commands where isUserApproved = 1
- System SHALL keep most recent 10 commands per element hash (even if deprecated)
- System SHALL keep commands with usageCount > 100 for 90 days (not 30)

**4.4 Cleanup Safety**
- System SHALL run cleanup in transaction (all-or-nothing)
- System SHALL create backup before first cleanup (for rollback)
- System SHALL validate deletion count before commit (sanity check: < 90% of database)

**API:**
```kotlin
interface CleanupManager {
    suspend fun scheduleAutomaticCleanup(
        intervalDays: Int = 7,
        gracePeriodDays: Int = 30
    )

    suspend fun previewCleanup(
        gracePeriodDays: Int = 30,
        keepUserApproved: Boolean = true
    ): CleanupPreview

    suspend fun executeCleanup(
        gracePeriodDays: Int = 30,
        keepUserApproved: Boolean = true,
        dryRun: Boolean = false
    ): CleanupResult
}

data class CleanupPreview(
    val commandsToDelete: Int,
    val appsAffected: List<String>,
    val databaseSizeReduction: Long,  // bytes
    val oldestCommandDate: Long,
    val newestCommandDate: Long
)

data class CleanupResult(
    val deletedCount: Int,
    val preservedCount: Int,
    val errors: List<String>,
    val durationMs: Long
)
```

**Acceptance Criteria:**
- ✓ Cleanup deletes 1,000 deprecated commands in < 500ms
- ✓ User-approved commands never deleted (even if 1 year old)
- ✓ Rollback works if cleanup encounters error
- ✓ Weekly WorkManager job executes reliably

---

### FR-5: Intelligent Rescan

**Priority:** P1 (High)
**Platform:** Android

**Requirements:**

**5.1 Selective Screen Scanning**
- When app updates, system SHALL:
  - Get list of learned screen hashes for old version
  - Re-explore app to generate new screen hashes
  - Compare old vs new hashes
  - Only rescan screens where hash changed

**5.2 Hash Comparison Strategy**
```
For each screen:
  IF oldHash == newHash THEN
    Mark all commands as verified (element structure unchanged)
  ELSE
    Rescan screen, verify each element individually
```

**5.3 Batch Verification**
- System SHALL verify up to 100 commands per screen visit
- System SHALL prioritize high-usage commands (usageCount DESC)
- System SHALL spread verification across multiple app sessions (not all at once)

**API:**
```kotlin
interface IntelligentRescanner {
    suspend fun compareScreens(
        packageName: String,
        oldVersion: Int,
        newVersion: Int
    ): ScreenComparison

    suspend fun rescanChangedScreens(
        packageName: String,
        changedScreens: List<String>
    ): RescanResult
}

data class ScreenComparison(
    val unchangedScreens: List<String>,  // Hash match
    val changedScreens: List<String>,    // Hash different
    val newScreens: List<String>,        // Not in old version
    val removedScreens: List<String>     // Not in new version
)

data class RescanResult(
    val screensScanned: Int,
    val commandsVerified: Int,
    val commandsDeprecated: Int,
    val newCommandsCreated: Int
)
```

**Acceptance Criteria:**
- ✓ Gmail update: 120/150 screens unchanged → 1,200 commands verified instantly
- ✓ 30 changed screens → rescanned individually
- ✓ Verification completes within 2 app sessions
- ✓ High-usage commands verified first

---

### FR-6: User Interface (LearnApp)

**Priority:** P2 (Medium)
**Platform:** Android (Compose)

**Requirements:**

**6.1 Version Info Display**
```
┌─────────────────────────────────────┐
│  GMAIL - VOICE COMMANDS             │
│  v8.2024.11.123 (current)           │
│                                     │
│  Total: 147 commands                │
│  • Active: 140 (v8.2024.11)         │
│  • Deprecated: 7 (v8.2024.10)       │
│                                     │
│  Last verified: 2 hours ago         │
│  [  Verify Now  ]  [  Cleanup  ]    │
└─────────────────────────────────────┘
```

**6.2 Cleanup Preview UI**
```
┌─────────────────────────────────────┐
│  CLEANUP PREVIEW                    │
│                                     │
│  Commands to delete: 1,247          │
│  Apps affected: 12                  │
│  Space freed: 2.3 MB                │
│                                     │
│  Grace period: [30 days ▼]          │
│                                     │
│  ⚠️ User-approved commands will     │
│     NOT be deleted                  │
│                                     │
│  [  Cancel  ]  [  Clean Up  ]       │
└─────────────────────────────────────┘
```

**6.3 Version History**
- User SHALL see which commands belong to which app version
- User SHALL be able to filter: "Show only current version"
- User SHALL see deprecation reason (version change, element not found, etc.)

**Acceptance Criteria:**
- ✓ UI updates in real-time during verification
- ✓ Cleanup preview accurate (matches actual deletion count)
- ✓ Version history shows last 5 app versions
- ✓ Filter works with pagination

---

## Non-Functional Requirements

### NFR-1: Performance

| Operation | Target | Measurement |
|-----------|--------|-------------|
| Version detection | < 50ms | Per app |
| Mark commands deprecated (1,000 commands) | < 100ms | Batch update |
| Cleanup execution (10,000 commands) | < 1 second | Transaction time |
| Verification (100 commands) | < 200ms | During normal usage |
| Screen hash comparison | < 500ms | Per app update |

### NFR-2: Data Integrity

- Database migrations SHALL be atomic (all-or-nothing)
- Cleanup SHALL run in transaction (rollback on error)
- Version detection SHALL handle concurrent app updates
- Command verification SHALL be idempotent (safe to run multiple times)

### NFR-3: Backward Compatibility

- Existing commands SHALL work with default version values (empty string, 0)
- Old VoiceOS versions SHALL continue reading database (ignore new columns)
- Migration SHALL preserve all existing data
- API SHALL maintain existing method signatures (add new, don't modify)

### NFR-4: Scalability

- System SHALL handle 100,000 commands across 100 apps
- System SHALL handle 50 app updates per day
- Cleanup SHALL scale linearly with command count
- Indexes SHALL keep query time constant O(1)

### NFR-5: Reliability

- Cleanup SHALL NOT delete > 90% of database in single run (safety check)
- System SHALL log all version changes
- System SHALL recover gracefully from PackageManager errors
- WorkManager SHALL retry failed cleanup jobs

---

## Platform-Specific Details

### Android

**Minimum SDK:** API 21 (Android 5.0)

**Dependencies:**
```gradle
// build.gradle.kts
dependencies {
    // Version detection
    implementation("androidx.core:core-ktx:1.12.0")

    // Background cleanup
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("androidx.work:work-testing:2.9.0")
}
```

**Permissions:**
- None required (PackageManager is unrestricted)

**API Level Compatibility:**
```kotlin
val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    packageInfo.longVersionCode.toInt()
} else {
    @Suppress("DEPRECATION")
    packageInfo.versionCode
}
```

### KMP (SQLDelight)

**Database Version:** 3 (current: 2)

**Migration Strategy:**
```kotlin
object VoiceOSDatabase {
    val Schema: SqlSchema<QueryResult.Value<Unit>> = VoiceOSDatabase.Schema.also {
        it.version = 3
    }
}
```

**Queries:**
```sql
-- High-performance queries using new indexes

-- Get active commands for current version
getActiveCommands:
SELECT * FROM commands_generated
WHERE appId = ?
  AND versionCode = ?
  AND isDeprecated = 0
ORDER BY usageCount DESC, id ASC
LIMIT ?;

-- Get deprecated commands ready for cleanup
getDeprecatedForCleanup:
SELECT * FROM commands_generated
WHERE isDeprecated = 1
  AND lastVerified < ?
  AND isUserApproved = 0
ORDER BY lastVerified ASC;

-- Mark version deprecated
markVersionDeprecated:
UPDATE commands_generated
SET isDeprecated = 1
WHERE appId = ? AND versionCode = ?;
```

---

## Implementation Phases

### Phase 1: Database Schema (Week 1)

**Tasks:**
1. Create migration v2 → v3
2. Add new columns with defaults
3. Create indexes
4. Test migration on 100K command database
5. Backfill existing commands with current version

**Deliverables:**
- `migrations/2.sqm`
- `DatabaseMigrations.kt` updated
- Migration tests

### Phase 2: Version Detection (Week 1-2)

**Tasks:**
1. Implement `AppVersionDetector`
2. Integrate with `JustInTimeLearner`
3. Handle version change events
4. Add logging and telemetry

**Deliverables:**
- `AppVersionDetector.kt`
- `VersionChangeHandler.kt`
- Unit tests (10+ tests)

### Phase 3: Command Lifecycle (Week 2-3)

**Tasks:**
1. Implement `CommandLifecycleManager`
2. Update command creation to include version
3. Implement verification logic
4. Handle deprecation marking

**Deliverables:**
- `CommandLifecycleManager.kt`
- Integration tests (20+ tests)
- Updated `JustInTimeLearner.kt`

### Phase 4: Cleanup System (Week 3-4)

**Tasks:**
1. Implement `CleanupManager`
2. Create WorkManager job
3. Add cleanup preview
4. Implement safety checks

**Deliverables:**
- `CleanupManager.kt`
- `CleanupWorker.kt`
- Background job tests

### Phase 5: UI Integration (Week 4-5)

**Tasks:**
1. Add version info to command list UI
2. Create cleanup preview screen
3. Add manual verification trigger
4. Show version history

**Deliverables:**
- `CommandListViewModel.kt` updates
- `CleanupScreen.kt`
- UI tests

### Phase 6: Testing & Optimization (Week 5-6)

**Tasks:**
1. End-to-end testing
2. Performance benchmarking
3. Migration testing on production data
4. Documentation updates

**Deliverables:**
- E2E test suite
- Performance report
- Developer manual update
- User manual update

---

## Testing Strategy

### Unit Tests

**Coverage Target:** 95%

**Critical Test Cases:**
```kotlin
// Version detection
testDetectVersionUpdate()
testDetectVersionDowngrade()
testHandleAppNotInstalled()
testHandleMultipleVersionChanges()

// Command lifecycle
testCreateCommandWithVersion()
testVerifyCommandUpdatesVersion()
testDeprecateCommandOnElementNotFound()
testPreserveUserApprovedCommands()

// Cleanup
testCleanupDeletesOnlyDeprecated()
testCleanupRespectsGracePeriod()
testCleanupPreservesUserApproved()
testCleanupRollsBackOnError()

// Migration
testMigrationV2ToV3()
testBackfillExistingCommands()
testMigrationPreservesData()
```

### Integration Tests

**Test Scenarios:**
1. Gmail update v100 → v101: Verify 150 commands marked for verification
2. User uses Gmail for 1 hour: 140 commands verified, 10 remain unverified
3. 30 days pass: 10 unverified commands deprecated
4. Weekly cleanup runs: 10 deprecated commands deleted
5. User approves 5 commands: They survive cleanup indefinitely

### Performance Tests

**Benchmarks:**
```kotlin
@Test
fun benchmarkVersionDetection_100Apps() {
    // Target: < 5 seconds for 100 apps
}

@Test
fun benchmarkCleanup_100kCommands() {
    // Target: < 10 seconds for 100,000 commands
}

@Test
fun benchmarkVerification_1kCommands() {
    // Target: < 1 second for 1,000 commands
}
```

### Migration Tests

**Test Data:**
- Empty database (0 commands)
- Small database (100 commands)
- Medium database (10,000 commands)
- Large database (100,000 commands)
- Mixed state (some commands have version, some don't)

---

## Acceptance Criteria

### Must Have (P0)

- ✓ Database migration v2 → v3 completes successfully on 100K command database
- ✓ Version detection works for 100 popular apps (Gmail, Chrome, etc.)
- ✓ Commands created after implementation include version info
- ✓ Deprecated commands are not deleted immediately (grace period)
- ✓ User-approved commands never auto-deleted
- ✓ Cleanup reduces database size by 80%+ after 50 app updates
- ✓ All existing functionality continues working (backward compatible)

### Should Have (P1)

- ✓ Automatic weekly cleanup via WorkManager
- ✓ UI shows version info in command list
- ✓ Manual cleanup with preview
- ✓ Intelligent rescan skips unchanged screens
- ✓ Performance: Cleanup 10K commands in < 1 second

### Nice to Have (P2)

- ✓ Version history showing last 5 app versions
- ✓ Analytics: Commands deprecated per app
- ✓ Export deprecated commands before deletion
- ✓ User notification before first cleanup

---

## Dependencies

### Internal Dependencies

| Module | Dependency | Reason |
|--------|------------|--------|
| core/database | Pagination feature (completed) | Uses same schema |
| JITLearning | Element discovery | Where commands are created |
| LearnApp | UI integration | Show version info |

### External Dependencies

| Library | Version | Usage |
|---------|---------|-------|
| androidx.work | 2.9.0 | Background cleanup |
| SQLDelight | 2.0.0 | Database queries |
| Kotlin Coroutines | 1.7.3 | Async operations |

---

## Risks & Mitigations

### Risk 1: Migration Failure on Large Database

**Probability:** Medium
**Impact:** High

**Mitigation:**
- Test migration on copy of production database first
- Implement rollback mechanism
- Add progress logging
- Provide manual migration script as fallback

### Risk 2: Cleanup Deletes Too Much

**Probability:** Low
**Impact:** Critical

**Mitigation:**
- Safety check: Never delete > 90% in one run
- Dry-run mode for testing
- User preview before deletion
- Keep backup for 30 days

### Risk 3: Version Detection Fails

**Probability:** Low
**Impact:** Medium

**Mitigation:**
- Graceful fallback: Continue without version tracking
- Log failures for debugging
- Retry mechanism for transient errors
- Manual override in UI

### Risk 4: Performance Degradation During Cleanup

**Probability:** Medium
**Impact:** Medium

**Mitigation:**
- Run cleanup during low-usage hours (3 AM)
- Batch deletions (1000 at a time)
- Use transaction for atomicity
- Monitor cleanup duration

---

## Out of Scope

### Explicitly Not Included

❌ **Cross-device sync of version info** - Each device manages its own command versions
❌ **Automatic app update detection** - Relies on user launching app
❌ **Command migration between versions** - Commands are recreated, not migrated
❌ **Version-specific command execution** - Always uses current version commands
❌ **Historical analytics** - Focus on current state, not trends
❌ **Cloud backup of deprecated commands** - Local deletion only

### Future Enhancements

**V2.0 Candidates:**
- ML-based prediction: Which commands likely deprecated without verification
- Smart update scheduling: Trigger rescan during app idle time
- Command diff visualization: Show what changed between versions
- Export/import command sets for sharing between devices

---

## Metrics & Success Criteria

### Quantitative Metrics

| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| Commands per app | 1,000-5,000 | 100-200 | After 50 updates |
| Database size | 500 MB | 50 MB | 100 apps, 2 years |
| Cleanup effectiveness | N/A | 80-95% reduction | Deprecated/total |
| Query performance | Linear degradation | Constant O(1) | With/without cleanup |
| User-approved preservation | N/A | 100% | Never deleted |

### Qualitative Metrics

- ✓ Developers can easily identify current-version commands
- ✓ Users notice faster command list loading
- ✓ Command failures reduce (no more obsolete element references)
- ✓ Database maintenance is "hands-off" (automatic)

---

## References

### Related Documents

- **QUICK-START-TASK-3.md** - Package-based pagination implementation
- **VoiceOS-P2-Features-Developer-Manual-51211-V1.md** - Chapter 13: Pagination
- **GeneratedCommand.sq** - Current database schema
- **DatabaseMigrations.kt** - Existing migration system

### External References

- [SQLDelight Migrations](https://cashapp.github.io/sqldelight/2.0.0/android_sqlite/migrations/)
- [Android PackageManager](https://developer.android.com/reference/android/content/pm/PackageManager)
- [WorkManager Best Practices](https://developer.android.com/topic/libraries/architecture/workmanager/advanced)

---

## Appendix

### A. Database Schema Evolution

**Version 1:** Original schema
**Version 2:** Added `appId` column (pagination feature)
**Version 3:** Added version tracking (this spec)

### B. Example Scenario

**Scenario: Gmail Updates from v100 to v101**

**Day 0: Before Update**
```
Database state:
- 150 commands, all versionCode = 100
- isDeprecated = 0 for all
```

**Day 0: Gmail Updates to v101**
```
System detects update:
- Marks all 150 commands as "pending verification"
- (isDeprecated remains 0, but needs verification)
```

**Day 0-30: Normal Usage**
```
User uses Gmail normally:
- Opens inbox: 20 commands verified → versionCode = 101
- Composes email: 15 commands verified → versionCode = 101
- ...
After 30 days: 140 verified, 10 unverified
```

**Day 30: Automatic Verification Timeout**
```
System marks unverified commands deprecated:
- 10 commands: isDeprecated = 1 (element not seen in 30 days)
- 140 commands: versionCode = 101, isDeprecated = 0
```

**Day 60: Weekly Cleanup Runs**
```
Cleanup deletes deprecated commands:
- 10 commands deleted (deprecated > 30 days)
- Database: 140 commands remaining (all current version)
```

---

**Document Status:** Ready for review
**Next Steps:** Plan phase → Implementation phase
**Estimated Effort:** 6 weeks (1 developer)
**Risk Level:** Medium (database migration risk)

---

**END OF SPECIFICATION**
