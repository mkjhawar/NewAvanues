# VoiceOS Version Management Implementation Plan (P3)

## Document Information
- **File:** VoiceOS-Plan-VersionMgmt-251222-V1.md
- **Created:** 2025-12-22
- **Feature:** P3 - Version-Aware Command Lifecycle Management
- **Status:** Planning Phase
- **Original Implementation:** Commit ac17dc225 (2025-12-14)
- **Removal:** Commit ce4e8b5eb (P2 scope reduction)

---

## 1. EXECUTIVE SUMMARY

### 1.1 Objective
Restore and modernize the Version Management feature that automatically tracks app versions, deprecates commands when apps update, and safely cleans up outdated commands with comprehensive safety mechanisms.

### 1.2 Context
6 version management files were removed during P2 scope reduction. The database schema (AppVersion.sq) and repository layer remain intact. This plan details restoring the feature and adapting it to the current Compose + Koin + Manager architecture.

### 1.3 Success Criteria
- [ ] Automatic version tracking for all learned apps
- [ ] Command deprecation on app updates with 0% data loss
- [ ] Safe cleanup with 90% safety limit and 30-day grace period
- [ ] 80% rescan time reduction via hash-based optimization
- [ ] Weekly automated cleanup via WorkManager
- [ ] Zero circular dependencies (SOLID compliance)
- [ ] 90%+ test coverage for critical paths

---

## 2. ORIGINAL IMPLEMENTATION ANALYSIS

### 2.1 Files Removed (P2 Scope Reduction)

| File | Lines | Purpose | Status |
|------|-------|---------|--------|
| AppVersionManager.kt | 453 | Orchestration layer for version workflows | Restore + adapt |
| AppVersionDetector.kt | 254 | Version change detection (API 21-34) | Restore as-is |
| VersionChange.kt | 185 | Sealed class for version states | Restore as-is |
| PackageUpdateReceiver.kt | 220 | Real-time app update detection | Restore + adapt manifest |
| CleanupManager.kt | 328 | Safe command cleanup with preview | Restore + adapt |
| CleanupWorker.kt | 248 | WorkManager periodic cleanup | Restore + adapt |
| ScreenHashCalculator.kt | 192 | Hash-based rescan optimization | Exists (commit 39125d2df) |

**Total:** 1,880 lines across 6 files (ScreenHashCalculator already restored)

### 2.2 Database Schema Status

**AppVersion.sq** - ✅ EXISTS (no changes needed)
```sql
CREATE TABLE app_version (
    package_name TEXT PRIMARY KEY NOT NULL,
    version_name TEXT NOT NULL,
    version_code INTEGER NOT NULL,
    last_checked INTEGER NOT NULL,
    CHECK (version_code >= 0),
    CHECK (last_checked > 0)
);
```

**Repository Layer** - ✅ EXISTS
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightAppVersionRepository.kt`
- Interface: IAppVersionRepository (complete)
- All CRUD operations implemented

### 2.3 Integration Points Analysis

**Current Architecture (P2-8e Refactoring):**
```
VoiceOSService (Orchestration Layer)
├── DatabaseManager (SQLDelight lifecycle)
├── IPCManager (Inter-process communication)
├── LifecycleCoordinator (Foreground service management)
└── OverlayManager (UI overlays)
```

**Required Integration:**
- Add VersionManager to manager pattern
- Wire into VoiceOSService initialization
- Integrate with LearnAppIntegration workflow
- Connect to PackageUpdateReceiver broadcasts

---

## 3. ARCHITECTURE DESIGN

### 3.1 Tree of Thought Analysis: Version Detection Approaches

#### Approach 1: Broadcast-Based Detection (Original)
**Pros:**
- Real-time detection (<1 second latency)
- Low CPU overhead (event-driven)
- Covers all app changes (install/update/uninstall)
- Android-native approach

**Cons:**
- BroadcastReceiver registration required
- Requires QUERY_ALL_PACKAGES permission (API 30+)
- Doesn't detect changes when VoiceOS is disabled
- Can miss events during device restart

**Verdict:** ✅ PRIMARY - Best for real-time updates

#### Approach 2: Polling-Based Detection
**Pros:**
- No permissions needed beyond package manager
- Works even when VoiceOS disabled
- Catches changes missed during downtime
- Simple implementation

**Cons:**
- Higher CPU overhead (periodic scanning)
- Delayed detection (polling interval)
- Battery impact if frequent
- Redundant with broadcasts

**Verdict:** ✅ SECONDARY - Use as backup/reconciliation

#### Approach 3: Hybrid Approach (Recommended)
**Implementation:**
1. Primary: PackageUpdateReceiver for real-time detection
2. Secondary: Daily polling during CleanupWorker execution
3. Reconciliation: Full scan on VoiceOSService startup

**Benefits:**
- Real-time updates + guaranteed consistency
- Catches edge cases (missed broadcasts, device restart)
- Minimal overhead (polling piggybacks on cleanup job)

**Selected:** HYBRID APPROACH

### 3.2 Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    VoiceOSService                           │
│                  (Orchestration Layer)                      │
└────────────┬────────────────────────────────────────────────┘
             │
             ├── DatabaseManager (existing)
             ├── IPCManager (existing)
             ├── LifecycleCoordinator (existing)
             ├── OverlayManager (existing)
             │
             └── VersionManager (NEW)
                      │
                      ├── AppVersionDetector (version detection)
                      ├── CleanupCoordinator (cleanup orchestration)
                      └── ScreenHashCalculator (rescan optimization)

┌─────────────────────────────────────────────────────────────┐
│              PackageUpdateReceiver (NEW)                    │
│           (Broadcast receiver - separate class)             │
└─────────────────────────────────────────────────────────────┘
                      │
                      └──> VersionManager.checkAndUpdateApp()

┌─────────────────────────────────────────────────────────────┐
│              CleanupWorker (NEW)                            │
│            (WorkManager - periodic job)                     │
└─────────────────────────────────────────────────────────────┘
                      │
                      └──> VersionManager.performCleanup()
```

### 3.3 Manager Pattern Integration

**New Manager: VersionManager**
```kotlin
class VersionManager(
    private val context: Context,
    private val database: VoiceOSDatabaseManager,
    private val detector: AppVersionDetector,
    private val hashCalculator: ScreenHashCalculator
) {
    // Public API
    suspend fun checkAndUpdateApp(packageName: String): VersionChange
    suspend fun checkAllTrackedApps(): Int
    suspend fun performCleanup(gracePeriodDays: Int = 30): CleanupResult
    suspend fun previewCleanup(gracePeriodDays: Int = 30): CleanupPreview

    // Internal orchestration
    private suspend fun processVersionChange(change: VersionChange)
    private suspend fun deprecateCommandsForVersion(packageName: String, versionCode: Long)
}
```

**Integration with VoiceOSService:**
```kotlin
// VoiceOSService.kt (lazy initialization pattern)
private val versionManager: VersionManager by lazy {
    VersionManager(
        context = applicationContext,
        database = dbManager.voiceOSDatabase,
        detector = AppVersionDetector(applicationContext, dbManager.voiceOSDatabase.appVersions),
        hashCalculator = ScreenHashCalculator
    )
}

override fun onCreate() {
    super.onCreate()
    // ... existing initialization
    initializeVersionManagement()
}

private fun initializeVersionManagement() {
    serviceScope.launch {
        // Schedule weekly cleanup
        CleanupWorker.schedulePeriodicCleanup(applicationContext)

        // Reconciliation: check all tracked apps on startup
        val processed = versionManager.checkAllTrackedApps()
        Log.i(TAG, "Startup version check: processed $processed apps")
    }
}
```

---

## 4. PHASED IMPLEMENTATION PLAN

### Phase 1: Core Version Detection (Days 1-2)

**Files to Restore:**
1. `AppVersionDetector.kt` - Version change detection
2. `VersionChange.kt` - Sealed class hierarchy
3. `AppVersion.kt` - Version data class

**Tasks:**
- [x] Extract files from commit ac17dc225
- [ ] Create version package: `voiceoscore/version/`
- [ ] Copy files unchanged (API 21-34 compatibility already handled)
- [ ] Add unit tests (AppVersionDetectorTest.kt)
- [ ] Verify compilation

**Validation:**
- [ ] Can detect FirstInstall, Updated, Downgraded, NoChange, AppNotInstalled
- [ ] API 21-34 compatibility (versionCode vs longVersionCode)
- [ ] Handles PackageManager.NameNotFoundException gracefully

### Phase 2: Version Manager Integration (Days 2-3)

**Files to Create:**
1. `VersionManager.kt` - Manager pattern wrapper
2. `AppVersionManager.kt` - Restore original orchestration logic

**Tasks:**
- [ ] Create VersionManager following manager pattern
- [ ] Restore AppVersionManager.kt (adapt DI from Hilt to manual)
- [ ] Integrate with VoiceOSService lazy initialization
- [ ] Wire up LearnAppCore and JustInTimeLearner
- [ ] Update LearnAppIntegration to pass AppVersionDetector

**Integration Points:**
```kotlin
// LearnAppCore.kt - ADD parameter
class LearnAppCore(
    // ... existing params
    private val versionDetector: AppVersionDetector? = null
) {
    suspend fun generateVoiceCommand(..., appVersion: AppVersion? = null) {
        // Tag command with version
        val version = appVersion ?: versionDetector?.getCurrentVersion(appId)
        // ... create command with versionCode, isDeprecated=0
    }
}

// JustInTimeLearner.kt - ADD parameter
class JustInTimeLearner(
    // ... existing params
    private val versionDetector: AppVersionDetector? = null
) {
    suspend fun learnCurrentScreen(packageName: String) {
        val currentVersion = versionDetector?.getCurrentVersion(packageName)
        // Pass version to command generation
    }
}
```

**Validation:**
- [ ] New commands tagged with correct appVersion and versionCode
- [ ] VersionManager accessible from VoiceOSService
- [ ] No circular dependencies

### Phase 3: Cleanup System (Days 3-4)

**Files to Restore:**
1. `CleanupManager.kt` - Safe cleanup business logic
2. `CleanupWorker.kt` - WorkManager periodic job
3. `CleanupCoordinator.kt` - NEW: Manager pattern wrapper

**Tasks:**
- [ ] Restore CleanupManager.kt (no changes needed)
- [ ] Restore CleanupWorker.kt (adapt to use VersionManager)
- [ ] Create CleanupCoordinator as manager delegate
- [ ] Add WorkManager dependency (already exists: androidx.work:work-runtime-ktx:2.9.0)
- [ ] Schedule CleanupWorker in VoiceOSService.onCreate()

**Safety Features:**
- 90% deletion safety limit (prevents mass deletion)
- 30-day grace period (configurable 1-365 days)
- User-approved command protection
- Preview mode (dry-run before deletion)
- Transaction safety (atomic operations)

**Validation:**
- [ ] Preview shows accurate statistics
- [ ] Safety limit blocks >90% deletion
- [ ] Grace period enforced correctly
- [ ] User-approved commands preserved
- [ ] Weekly WorkManager job executes

### Phase 4: Real-Time Detection (Days 4-5)

**Files to Restore:**
1. `PackageUpdateReceiver.kt` - Broadcast receiver

**Tasks:**
- [ ] Restore PackageUpdateReceiver.kt
- [ ] Update to use VersionManager instead of AppVersionManager directly
- [ ] Register in AndroidManifest.xml
- [ ] Add QUERY_ALL_PACKAGES permission handling (API 30+)
- [ ] Test broadcast reception

**AndroidManifest.xml Changes:**
```xml
<receiver
    android:name=".receivers.PackageUpdateReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.PACKAGE_ADDED" />
        <action android:name="android.intent.action.PACKAGE_REPLACED" />
        <action android:name="android.intent.action.PACKAGE_REMOVED" />
        <data android:scheme="package" />
    </intent-filter>
</receiver>

<!-- API 30+ permission for package queries -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
```

**Validation:**
- [ ] Receives PACKAGE_ADDED on app install
- [ ] Receives PACKAGE_REPLACED on app update
- [ ] Receives PACKAGE_REMOVED on app uninstall
- [ ] Correctly extracts package name from intent
- [ ] Triggers version check asynchronously

### Phase 5: Hash-Based Rescan Optimization (Day 5)

**Status:** ✅ ALREADY EXISTS (commit 39125d2df)

**Files Present:**
- `ScreenHashCalculator.kt` - SHA-256 hash calculation
- `CleanupPreviewUiState.kt` - UI state for Compose
- `CommandListUiState.kt` - UI state for Compose

**Tasks:**
- [ ] Verify ScreenHashCalculator integration with JustInTimeLearner
- [ ] Confirm shouldRescanScreen() logic uses hashing
- [ ] Validate 80% skip rate metric tracking

**No restoration needed - feature already present!**

### Phase 6: Testing & Documentation (Days 6-7)

**Test Files to Create:**
1. `AppVersionDetectorTest.kt` - Unit tests for detection
2. `VersionManagerTest.kt` - Unit tests for manager
3. `CleanupManagerTest.kt` - Unit tests for cleanup
4. `VersionManagementIntegrationTest.kt` - Restore from commit ac17dc225

**Coverage Targets:**
- Unit tests: 90%+ coverage
- Integration tests: All 7 workflows
- Edge cases: Downgrade, uninstall, safety limit triggers

**Documentation:**
1. Update VoiceOS README with version management overview
2. Create user guide for cleanup settings
3. Document database schema changes (none needed - already migrated)

---

## 5. MIGRATION STRATEGY

### 5.1 Database Migration

**Status:** ✅ NO MIGRATION NEEDED

The AppVersion.sq schema already exists in the current codebase at:
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/app/AppVersion.sq`

**Verification Steps:**
1. Check schema version in DatabaseVersion.sq (should be v3+)
2. Verify indexes exist (idx_av_version_code, idx_av_last_checked)
3. Run test query to confirm table accessibility

### 5.2 Dependency Injection Migration

**Original (Hilt):**
```kotlin
@Module
@InstallIn(ServiceComponent::class)
object AccessibilityModule {
    @Provides
    @ServiceScoped
    fun provideAppVersionDetector(...): AppVersionDetector
}
```

**Current (Manual Lazy Initialization):**
```kotlin
class VoiceOSService : AccessibilityService() {
    private val versionManager: VersionManager by lazy {
        VersionManager(
            context = applicationContext,
            database = dbManager.voiceOSDatabase,
            detector = AppVersionDetector(...),
            hashCalculator = ScreenHashCalculator
        )
    }
}
```

**Migration Path:**
- Remove Hilt annotations
- Use lazy delegation pattern (consistent with current managers)
- Pass dependencies explicitly to VersionManager constructor

### 5.3 UI Framework Migration

**Original (XML Views):**
- No UI components in original implementation
- CleanupPreviewUiState existed but no views

**Current (Jetpack Compose + Material3):**
- Use existing CleanupPreviewUiState.kt (commit 39125d2df)
- Create Compose screens for cleanup preview (future P4 task)
- Settings integration via SettingsScreen.kt (Koin injection)

**No immediate migration needed** - UI is P4 scope

---

## 6. INTEGRATION POINTS

### 6.1 VoiceOSService Integration

**Initialization Sequence:**
```kotlin
override fun onCreate() {
    super.onCreate()

    // Existing initialization
    initializeDatabase()
    initializeManagers()

    // NEW: Version management initialization
    initializeVersionManagement()
}

private fun initializeVersionManagement() {
    serviceScope.launch(Dispatchers.Default) {
        try {
            // Schedule periodic cleanup (if not already scheduled)
            CleanupWorker.schedulePeriodicCleanup(applicationContext)

            // Startup reconciliation: check all tracked apps
            val processed = versionManager.checkAllTrackedApps()
            Log.i(TAG, "Version management initialized: $processed apps checked")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize version management", e)
            // Non-fatal - version management is enhancement, not critical
        }
    }
}
```

### 6.2 LearnApp Integration

**LearnAppIntegration.kt:**
```kotlin
class LearnAppIntegration(
    private val service: VoiceOSService,
    private val database: VoiceOSDatabaseManager
) {
    private val versionDetector: AppVersionDetector by lazy {
        AppVersionDetector(
            context = service.applicationContext,
            appVersionRepository = database.appVersions
        )
    }

    fun initialize() {
        learnAppCore = LearnAppCore(
            // ... existing params
            versionDetector = versionDetector
        )

        justInTimeLearner = JustInTimeLearner(
            // ... existing params
            versionDetector = versionDetector
        )
    }
}
```

### 6.3 Command Generation Integration

**Workflow:**
1. User triggers learning (manual or JIT)
2. VoiceOS scrapes UI elements
3. **NEW:** AppVersionDetector.getCurrentVersion(packageName)
4. CommandGenerator creates commands with version tags
5. Commands stored with appVersion, versionCode, isDeprecated=0

**Database Fields Updated:**
- `appVersion` (TEXT) - e.g., "8.2024.11.123"
- `versionCode` (INTEGER) - e.g., 82024
- `lastVerified` (INTEGER) - Timestamp of last verification
- `isDeprecated` (INTEGER) - 0=active, 1=deprecated

---

## 7. TESTING STRATEGY

### 7.1 Unit Tests

**AppVersionDetectorTest.kt:**
```kotlin
class AppVersionDetectorTest {
    @Test fun `detectVersionChange - first install`()
    @Test fun `detectVersionChange - app updated`()
    @Test fun `detectVersionChange - app downgraded`()
    @Test fun `detectVersionChange - no change`()
    @Test fun `detectVersionChange - app not installed`()
    @Test fun `detectAllVersionChanges - multiple apps`()
    @Test fun `getCurrentVersion - API 21-27 compatibility`()
    @Test fun `getCurrentVersion - API 28+ compatibility`()
}
```

**CleanupManagerTest.kt:**
```kotlin
class CleanupManagerTest {
    @Test fun `previewCleanup - calculates statistics correctly`()
    @Test fun `executeCleanup - respects grace period`()
    @Test fun `executeCleanup - preserves user-approved commands`()
    @Test fun `executeCleanup - enforces 90% safety limit`()
    @Test fun `executeCleanup - dry run mode does not delete`()
    @Test fun `executeCleanup - handles errors gracefully`()
    @Test fun `calculateDatabaseSizeReduction - estimates correctly`()
    @Test fun `validateGracePeriod - rejects invalid ranges`()
}
```

**VersionManagerTest.kt:**
```kotlin
class VersionManagerTest {
    @Test fun `checkAndUpdateApp - processes first install`()
    @Test fun `checkAndUpdateApp - deprecates on update`()
    @Test fun `checkAndUpdateApp - cleans up on uninstall`()
    @Test fun `checkAllTrackedApps - processes all apps`()
    @Test fun `performCleanup - orchestrates cleanup correctly`()
}
```

### 7.2 Integration Tests

**VersionManagementIntegrationTest.kt** (restore from ac17dc225):
```kotlin
class VersionManagementIntegrationTest {
    @Test fun `end-to-end - first install workflow`()
    @Test fun `end-to-end - app update deprecates old commands`()
    @Test fun `end-to-end - cleanup deletes old deprecated commands`()
    @Test fun `end-to-end - safety limit prevents mass deletion`()
    @Test fun `end-to-end - downgrade workflow`()
    @Test fun `end-to-end - uninstall cleanup`()
    @Test fun `end-to-end - JIT learning tags version correctly`()
}
```

### 7.3 Manual Testing Checklist

**Version Detection:**
- [ ] Install new app → FirstInstall detected, version stored
- [ ] Update app → Updated detected, old commands deprecated
- [ ] Downgrade app → Downgraded detected, current commands deprecated
- [ ] Uninstall app → AppNotInstalled detected, all commands deleted
- [ ] Reopen unchanged app → NoChange detected, no action taken

**Cleanup:**
- [ ] Preview shows accurate counts and app list
- [ ] Safety limit blocks >90% deletion attempt
- [ ] Grace period prevents deletion of recent deprecated commands
- [ ] User-approved commands preserved during cleanup
- [ ] Dry run mode does not modify database

**Performance:**
- [ ] Version check completes <100ms for single app
- [ ] Batch check of 50 apps completes <5 seconds
- [ ] Cleanup of 1000 commands completes <1 second
- [ ] Hash calculation <10ms per screen
- [ ] Weekly WorkManager job completes within 10-minute constraint

---

## 8. RISK ANALYSIS & MITIGATION

### 8.1 Critical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Mass command deletion due to bug | Medium | Critical | 90% safety limit + dry run mode + preview before execute |
| Missed app updates (broadcasts lost) | Low | Medium | Hybrid approach: broadcasts + polling + startup reconciliation |
| Performance degradation on large databases | Low | Medium | Indexed queries + batch operations + background processing |
| API compatibility issues (Android 9-14) | Low | High | Existing API 21-34 compatibility handling + extensive testing |
| Circular dependencies introduced | Low | Medium | Strict layer separation + dependency graph validation |

### 8.2 Data Safety Mechanisms

**Layer 1: Prevention**
- Input validation (grace period 1-365 days)
- Safety limit check (max 90% deletion)
- Preview before execution
- Dry run mode available

**Layer 2: Protection**
- Database transactions (atomic operations)
- User-approved command preservation
- Grace period enforcement (30 days default)
- Soft delete pattern (isDeprecated flag)

**Layer 3: Recovery**
- Detailed logging of all deletions
- Error collection and reporting
- Transactional rollback on failures
- Statistics for audit trail

---

## 9. PERFORMANCE TARGETS

### 9.1 Latency Requirements

| Operation | Target | Maximum | Current (Estimated) |
|-----------|--------|---------|---------------------|
| Single app version check | <50ms | 100ms | ~30ms |
| Batch check (50 apps) | <2s | 5s | ~1.5s |
| Command deprecation (100 commands) | <100ms | 500ms | ~50ms |
| Cleanup preview | <500ms | 1s | ~300ms |
| Cleanup execution (1000 commands) | <1s | 2s | ~800ms |
| Hash calculation (100 elements) | <5ms | 10ms | ~3ms |

### 9.2 Resource Constraints

**Memory:**
- VersionManager: ~500KB overhead
- CleanupManager: <1MB during execution
- Hash calculation: O(n) where n = element count
- Total additional overhead: <2MB

**Battery:**
- PackageUpdateReceiver: Event-driven (minimal)
- CleanupWorker: Weekly, charging-only (negligible)
- Startup reconciliation: <5 seconds CPU time

**Storage:**
- AppVersion table: ~100 bytes per app
- 50 apps: ~5KB
- Hash storage: 64 bytes per screen
- Negligible impact (<100KB total)

---

## 10. ROLLOUT PLAN

### 10.1 Development Phases

**Week 1: Core Implementation**
- Days 1-2: Phase 1 (Version Detection)
- Days 2-3: Phase 2 (Manager Integration)
- Days 3-4: Phase 3 (Cleanup System)

**Week 2: Integration & Testing**
- Days 4-5: Phase 4 (Real-Time Detection)
- Day 5: Phase 5 (Hash Optimization Verification)
- Days 6-7: Phase 6 (Testing & Documentation)

**Week 3: Validation & Polish**
- Manual testing across Android 9-14
- Performance profiling and optimization
- Documentation completion
- Code review and refinements

### 10.2 Success Metrics

**Functional:**
- ✅ All 7 workflows pass integration tests
- ✅ Zero data loss incidents
- ✅ 100% API 21-34 compatibility

**Performance:**
- ✅ 80% rescan skip rate achieved
- ✅ <100ms single app version check
- ✅ <1s cleanup execution for 1000 commands

**Quality:**
- ✅ 90%+ unit test coverage
- ✅ Zero circular dependencies
- ✅ SOLID compliance verified
- ✅ Zero compilation warnings

---

## Critical Files for Implementation

The following files are most critical for implementing this plan:

1. **`/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`**
   - Integration point for VersionManager initialization
   - Orchestration layer following P2-8e manager pattern
   - Requires adding versionManager lazy property and initializeVersionManagement()

2. **`/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightAppVersionRepository.kt`**
   - Already implemented repository layer
   - Interface: IAppVersionRepository provides all CRUD operations
   - No modifications needed - use as-is

3. **`/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/integration/LearnAppIntegration.kt`**
   - Integration point for wiring AppVersionDetector to LearnAppCore
   - Requires adding versionDetector lazy property
   - Pass detector to LearnAppCore and JustInTimeLearner constructors

4. **`/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/app/AppVersion.sq`**
   - Database schema (already exists - no changes)
   - Reference for understanding version tracking data model
   - Queries: getAppVersion, getAllAppVersions, upsertAppVersion, deleteAppVersion

5. **`/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/build.gradle.kts`**
   - Verify WorkManager dependency exists: androidx.work:work-runtime-ktx:2.9.0
   - Already present - no changes needed
   - Reference for dependency verification

---

**END OF IMPLEMENTATION PLAN**
