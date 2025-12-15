# VoiceOS Implementation Plan - Version Management P2 Enhancements
**Plan ID:** VoiceOS-Plan-VersionManagement-P2-Enhancements-5141217-V1
**Date:** 2025-12-14
**Status:** READY
**Priority:** P2 (Post-Launch Enhancements)
**Depends On:** VoiceOS-Implementation-VersionAwareCommandManagement-5141217-V1

---

## Overview

This plan outlines **Phase 2 enhancements** for the version-aware command management system. Phase 1 (core functionality) is complete and production-ready. These enhancements focus on optimization, user experience, and operational excellence.

---

## Prerequisites

✅ **Phase 1 Complete** - Core version management implemented
✅ **Production Deployed** - System running in production
✅ **Baseline Metrics** - Initial performance and usage data collected

---

## Enhancement Categories

### Category 1: Performance Optimization (FR-5)
**Goal:** 80% reduction in rescan time for app updates

### Category 2: User Experience (FR-6)
**Goal:** Full visibility and control of version management in LearnApp UI

### Category 3: Operational Excellence
**Goal:** Production-grade logging, monitoring, and observability

### Category 4: Code Quality
**Goal:** Address technical debt from Phase 1

---

## Phase 1: Performance Optimization

### Task 1.1: Implement Intelligent Rescan (FR-5)

**Description:** Add screen hash comparison to skip unchanged screens during app updates.

**Current Behavior:**
```
App updates v100 → v101
    ↓
JIT rescans ALL screens (100% rescan)
    ↓
Generates commands for 100% of screens
```

**Enhanced Behavior:**
```
App updates v100 → v101
    ↓
Calculate hash for each screen
    ↓
Compare with v100 screen hashes (from screen_context table)
    ↓
Rescan ONLY changed screens (20% rescan)
    ↓
Generates commands for 20% of screens (80% efficiency gain)
```

**Implementation Steps:**

1. **Add screen hash to screen_context table**
   ```sql
   ALTER TABLE screen_context ADD COLUMN screenHash TEXT;
   CREATE INDEX idx_sc_hash ON screen_context(packageName, screenHash);
   ```

2. **Implement hash calculation**
   ```kotlin
   // ScreenHashCalculator.kt
   fun calculateScreenHash(elements: List<ElementInfo>): String {
       // Hash based on: element count, types, labels, positions
       return elements
           .sortedBy { it.elementHash }
           .joinToString("|") { "${it.actionType}:${it.text}" }
           .toSHA256()
   }
   ```

3. **Add hash comparison logic**
   ```kotlin
   // ExplorationEngine.kt
   suspend fun shouldRescanScreen(packageName: String, screenHash: String): Boolean {
       val previousHash = screenContextRepo.getScreenHash(packageName, screenHash)
       return previousHash != screenHash  // Rescan only if changed
   }
   ```

4. **Update JIT learner to use hash comparison**
   ```kotlin
   // JustInTimeLearner.kt
   val screenHash = hashCalculator.calculateScreenHash(elements)
   if (shouldRescanScreen(packageName, screenHash)) {
       learnCurrentScreen()  // Rescan
   } else {
       Log.d(TAG, "Screen unchanged, skipping rescan")  // Skip
   }
   ```

**Files to Create:**
- `ScreenHashCalculator.kt` (~150 lines)
- `ScreenHashCalculatorTest.kt` (~100 lines)

**Files to Modify:**
- `ExplorationEngine.kt` (add hash comparison)
- `JustInTimeLearner.kt` (integrate hash calculator)
- `IScreenContextRepository.kt` (add `getScreenHash()`, `updateScreenHash()`)
- Database migration v3 → v4 (add screenHash column)

**Expected Performance:**
- Baseline: 100% rescan on app update
- Target: 20% rescan (80% efficiency)
- Avg screens per app: 50
- Time savings: ~40 screens * 500ms = 20 seconds per update

**Testing:**
- Unit tests: Hash calculation correctness
- Integration tests: Hash comparison logic
- Performance tests: Measure rescan reduction

**Completion Criteria:**
- [ ] Screen hash calculation implemented
- [ ] Database migration v3→v4 complete
- [ ] Hash comparison integrated
- [ ] 80%+ rescan reduction measured
- [ ] Tests passing (5 new tests)

---

### Task 1.2: Database Query Optimization

**Description:** Optimize N+1 query problem in `getDeprecatedCommandStats()`.

**Current Implementation:**
```kotlin
// AppVersionManager.kt:344-349 (N+1 problem)
for (app in trackedApps) {
    val deprecated = commandRepo.getDeprecatedCommands(app.packageName)  // N queries
    // Process each app's deprecated commands
}
```

**Optimized Implementation:**
```kotlin
// Add new repository method
interface IGeneratedCommandRepository {
    suspend fun getAllDeprecatedCommandsByApp(): Map<String, List<GeneratedCommandDTO>>
}

// Use batch query
val deprecatedByApp = commandRepo.getAllDeprecatedCommandsByApp()  // 1 query
for ((packageName, commands) in deprecatedByApp) {
    // Process commands
}
```

**SQL Query:**
```sql
-- Old: N queries (one per app)
SELECT * FROM generated_command WHERE appId = ? AND isDeprecated = 1;

-- New: 1 query (batch all apps)
SELECT * FROM generated_command WHERE isDeprecated = 1 ORDER BY appId;
```

**Files to Modify:**
- `IGeneratedCommandRepository.kt` (add batch method)
- `SQLDelightGeneratedCommandRepository.kt` (implement batch query)
- `AppVersionManager.kt` (use batch method)

**Expected Performance:**
- Baseline: 50 apps * 10ms = 500ms
- Target: 1 query * 15ms = 15ms (97% faster)

**Completion Criteria:**
- [ ] Batch query method added
- [ ] AppVersionManager updated
- [ ] Performance improvement measured (>90% reduction)

---

### Task 1.3: Memory Optimization for Large Datasets

**Description:** Avoid loading all commands into memory for cleanup preview.

**Current Implementation:**
```kotlin
// CleanupManager.kt:168 - Loads ALL commands
val allCommands = commandRepo.getAll()  // 100K commands = ~20MB RAM
```

**Optimized Implementation:**
```kotlin
// Add filtered query to repository
interface IGeneratedCommandRepository {
    suspend fun getDeprecatedCommandsOlderThan(
        timestamp: Long,
        keepUserApproved: Boolean
    ): List<GeneratedCommandDTO>
}

// CleanupManager uses filtered query
val deprecatedCommands = commandRepo.getDeprecatedCommandsOlderThan(
    cutoffTimestamp,
    keepUserApproved
)  // Only loads commands to be deleted (~10K = ~2MB)
```

**Expected Performance:**
- Baseline: Load 100K commands (20MB RAM)
- Target: Load 10K commands (2MB RAM) - 90% reduction

**Completion Criteria:**
- [ ] Filtered repository method added
- [ ] CleanupManager updated to use filtered query
- [ ] Memory usage measured (<5MB for 100K dataset)

---

## Phase 2: User Experience (FR-6)

### Task 2.1: Version Info in Command List UI

**Description:** Display app version information in LearnApp command list.

**UI Mockup:**
```
╔══════════════════════════════════════════════════════╗
║  Generated Commands - Gmail                          ║
╠══════════════════════════════════════════════════════╣
║  Version: v101 (101) • Last Updated: 2 days ago      ║
║  ────────────────────────────────────────────────────║
║  ✓  "Send email"              Active   v101          ║
║  ✓  "Compose message"          Active   v101          ║
║  ⚠  "Archive conversation"    Deprecated v100 (30d)  ║
║  ⚠  "Mark as read"            Deprecated v100 (30d)  ║
╚══════════════════════════════════════════════════════╝
```

**Implementation:**

1. **Add version info to ViewModel**
   ```kotlin
   data class CommandListItem(
       val commandText: String,
       val appVersion: String,
       val versionCode: Long,
       val isDeprecated: Boolean,
       val deprecatedDaysAgo: Int?
   )
   ```

2. **Update Compose UI**
   ```kotlin
   @Composable
   fun CommandListItem(item: CommandListItem) {
       Row {
           Text(item.commandText)
           Spacer()
           if (item.isDeprecated) {
               Badge(
                   text = "Deprecated ${item.deprecatedDaysAgo}d",
                   color = Color.Yellow
               )
           }
           Text("v${item.versionCode}", style = MonospaceSmall)
       }
   }
   ```

**Files to Create:**
- `CommandListViewModel.kt` enhancements

**Files to Modify:**
- `CommandListScreen.kt` (add version display)
- `CommandListItem.kt` (add version fields)

**Completion Criteria:**
- [ ] Version info displayed in command list
- [ ] Deprecated commands visually distinguished
- [ ] Days until cleanup shown for deprecated commands
- [ ] UI tests passing

---

### Task 2.2: Cleanup Preview Screen

**Description:** Add UI screen to preview cleanup results before execution.

**UI Mockup:**
```
╔══════════════════════════════════════════════════════╗
║  Cleanup Preview                                     ║
╠══════════════════════════════════════════════════════╣
║  Grace Period: 30 days                               ║
║  ────────────────────────────────────────────────────║
║  Commands to Delete:           147                   ║
║  Commands to Preserve:         2,341                 ║
║  Apps Affected:                12                    ║
║  ────────────────────────────────────────────────────║
║  Deletion Percentage:          5.9% ✅               ║
║  ────────────────────────────────────────────────────║
║  Gmail (v101)                  42 commands           ║
║  Maps (v205)                   31 commands           ║
║  Chrome (v120)                 28 commands           ║
║  ...                                                  ║
║  ────────────────────────────────────────────────────║
║  [Cancel]                      [Execute Cleanup]     ║
╚══════════════════════════════════════════════════════╝
```

**Implementation:**

1. **Create ViewModel**
   ```kotlin
   class CleanupPreviewViewModel(
       private val cleanupManager: CleanupManager
   ) : ViewModel() {

       suspend fun loadPreview(gracePeriodDays: Int): CleanupPreview {
           return cleanupManager.previewCleanup(gracePeriodDays, keepUserApproved = true)
       }

       suspend fun executeCleanup(gracePeriodDays: Int): CleanupResult {
           return cleanupManager.executeCleanup(gracePeriodDays, keepUserApproved = true)
       }
   }
   ```

2. **Create Compose Screen**
   ```kotlin
   @Composable
   fun CleanupPreviewScreen(viewModel: CleanupPreviewViewModel) {
       val preview = viewModel.preview.collectAsState()

       Column {
           CleanupStatistics(preview.value)
           AffectedAppsList(preview.value.appsAffected)
           SafetyIndicator(preview.value.deletionPercentage)
           ActionButtons(
               onCancel = { navController.popBackStack() },
               onExecute = { viewModel.executeCleanup() }
           )
       }
   }
   ```

**Files to Create:**
- `CleanupPreviewViewModel.kt` (~100 lines)
- `CleanupPreviewScreen.kt` (~150 lines)
- `CleanupPreviewViewModelTest.kt` (~80 lines)

**Completion Criteria:**
- [ ] Preview screen implemented
- [ ] Statistics displayed correctly
- [ ] Execute cleanup functional
- [ ] Safety warnings shown if >50% deletion

---

### Task 2.3: Manual Cleanup Trigger

**Description:** Add button in LearnApp settings to manually trigger cleanup.

**UI Location:** Settings → Advanced → Command Management → Manual Cleanup

**Implementation:**

1. **Add to Settings Screen**
   ```kotlin
   SettingsSection(title = "Command Management") {
       SettingsButton(
           text = "Run Cleanup Now",
           onClick = { navController.navigate("cleanup_preview") }
       )
       SettingsInfo(
           text = "Last cleanup: 3 days ago",
           value = "Deleted: 42 commands"
       )
   }
   ```

2. **Add navigation route**
   ```kotlin
   NavHost {
       composable("cleanup_preview") {
           CleanupPreviewScreen(viewModel)
       }
   }
   ```

**Files to Modify:**
- `SettingsScreen.kt` (add cleanup section)
- `NavigationGraph.kt` (add route)

**Completion Criteria:**
- [ ] Manual cleanup button added
- [ ] Navigation to preview screen working
- [ ] Last cleanup stats displayed

---

## Phase 3: Operational Excellence

### Task 3.1: Replace println() with Timber Logging

**Description:** Replace all `println()` calls with proper Android logging framework.

**Current Implementation:**
```kotlin
// AppVersionManager.kt (multiple println() calls)
println("[AppVersionManager] Version change detected: $change")
```

**Enhanced Implementation:**
```kotlin
// Add Timber dependency
dependencies {
    implementation("com.jakewharton.timber:timber:5.0.1")
}

// Initialize in Application.onCreate()
class VoiceOSApplication : Application() {
    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())  // Firebase Crashlytics
        }
    }
}

// Replace println()
Timber.d("Version change detected: %s", change)
Timber.i("Cleanup completed: deleted=%d, preserved=%d", deleted, preserved)
Timber.e(exception, "Failed to check app version")
```

**Files to Modify:**
- `build.gradle.kts` (add Timber)
- `VoiceOSApplication.kt` (initialize Timber)
- `AppVersionManager.kt` (replace println)
- `CleanupManager.kt` (add structured logging)
- `PackageUpdateReceiver.kt` (add event logging)

**Logging Levels:**
- **DEBUG**: Detailed flow (version detection, hash calculations)
- **INFO**: Important events (cleanup execution, version changes)
- **WARN**: Potential issues (safety limit approached, errors collected)
- **ERROR**: Failures (exceptions, database errors)

**Completion Criteria:**
- [ ] Timber integrated
- [ ] All println() replaced
- [ ] Logging levels appropriate
- [ ] Production logging configured (Crashlytics)

---

### Task 3.2: Add Telemetry/Analytics

**Description:** Track key metrics for monitoring and optimization.

**Metrics to Track:**

| Metric | Purpose | Frequency |
|--------|---------|-----------|
| **cleanup_success_rate** | Monitor WorkManager job success | Per execution |
| **command_deprecation_rate** | % deprecated / total | Daily |
| **safety_limit_triggers** | Count of 90% limit hits | Per execution |
| **avg_cleanup_duration** | Performance monitoring | Per execution |
| **version_detection_latency** | Performance monitoring | Per detection |

**Implementation:**

1. **Add Analytics Interface**
   ```kotlin
   interface IAnalytics {
       fun logCleanupExecuted(result: CleanupResult)
       fun logVersionChange(packageName: String, change: VersionChange)
       fun logSafetyLimitTriggered(percentage: Double)
       fun measurePerformance(operation: String, durationMs: Long)
   }
   ```

2. **Implement Firebase Analytics**
   ```kotlin
   class FirebaseAnalyticsImpl(private val firebaseAnalytics: FirebaseAnalytics) : IAnalytics {
       override fun logCleanupExecuted(result: CleanupResult) {
           firebaseAnalytics.logEvent("cleanup_executed") {
               param("deleted_count", result.deletedCount.toLong())
               param("preserved_count", result.preservedCount.toLong())
               param("duration_ms", result.durationMs)
               param("had_errors", result.errors.isNotEmpty())
           }
       }
   }
   ```

3. **Integrate into Components**
   ```kotlin
   class CleanupManager(
       private val commandRepo: IGeneratedCommandRepository,
       private val analytics: IAnalytics
   ) {
       suspend fun executeCleanup(...): CleanupResult {
           val result = // ... execute cleanup
           analytics.logCleanupExecuted(result)
           return result
       }
   }
   ```

**Files to Create:**
- `IAnalytics.kt` (~50 lines)
- `FirebaseAnalyticsImpl.kt` (~100 lines)
- `AnalyticsModule.kt` (Hilt DI) (~30 lines)

**Files to Modify:**
- `CleanupManager.kt` (add analytics calls)
- `AppVersionManager.kt` (add analytics calls)
- `build.gradle.kts` (add Firebase Analytics)

**Completion Criteria:**
- [ ] Analytics interface defined
- [ ] Firebase Analytics integrated
- [ ] Key metrics tracked
- [ ] Dashboard created (Firebase Console)

---

### Task 3.3: Add Monitoring Alerts

**Description:** Set up alerts for critical issues.

**Alerts to Configure:**

| Alert | Trigger | Action |
|-------|---------|--------|
| **Cleanup Failure Rate** | >10% failures in 24h | Email team |
| **Safety Limit Hit** | >90% deletion attempted | Email + Slack |
| **High Deprecation Rate** | >50% commands deprecated | Email |
| **Version Detection Slow** | >100ms average | Log warning |

**Implementation:**

1. **Firebase Alerts**
   - Configure in Firebase Console
   - Set thresholds for each metric
   - Route to email/Slack

2. **In-App Error Reporting**
   ```kotlin
   class ErrorReporter(private val crashlytics: FirebaseCrashlytics) {
       fun reportSafetyLimitTriggered(percentage: Double, context: String) {
           crashlytics.recordException(
               SafetyLimitException("90% limit triggered: $percentage%")
           )
           crashlytics.setCustomKey("deletion_percentage", percentage)
           crashlytics.setCustomKey("context", context)
       }
   }
   ```

**Completion Criteria:**
- [ ] Firebase alerts configured
- [ ] Error reporting integrated
- [ ] Test alerts verified

---

## Phase 4: Code Quality & Technical Debt

### Task 4.1: Fix CleanupWorker.isCleanupScheduled()

**Description:** Re-enable `isCleanupScheduled()` by adding Guava dependency.

**Current State:**
```kotlin
// CleanupWorker.kt:241 (disabled)
fun isCleanupScheduled(context: Context): Boolean {
    Log.w(TAG, "isCleanupScheduled not implemented - requires Guava dependency")
    return false
}
```

**Fixed Implementation:**
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.google.guava:guava:32.1.3-android")
}

// CleanupWorker.kt
fun isCleanupScheduled(context: Context): Boolean {
    return try {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_NAME)
            .get(5, TimeUnit.SECONDS)  // 5s timeout

        workInfos.any { !it.state.isFinished }
    } catch (e: TimeoutException) {
        Timber.w(e, "Timeout checking cleanup schedule")
        false
    } catch (e: Exception) {
        Timber.e(e, "Failed to check cleanup schedule")
        false
    }
}
```

**Alternative (Coroutines-based):**
```kotlin
// Use WorkManager's coroutines API instead of Guava
suspend fun isCleanupScheduled(context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_NAME)
            .await()  // Use await() instead of get()

        workInfos.any { !it.state.isFinished }
    } catch (e: Exception) {
        Timber.e(e, "Failed to check cleanup schedule")
        false
    }
}
```

**Recommendation:** Use coroutines-based approach (no Guava needed).

**Files to Modify:**
- `CleanupWorker.kt` (implement method)

**Completion Criteria:**
- [ ] Method re-enabled (coroutines or Guava)
- [ ] Unit tests added
- [ ] No compilation warnings

---

### Task 4.2: Add Performance Benchmarks

**Description:** Create automated benchmarks for critical operations.

**Benchmarks to Add:**

```kotlin
@RunWith(AndroidJUnit4::class)
class VersionManagementBenchmarkTest {

    @Test
    fun benchmark_versionDetection_shouldComplete_under50ms() = runBlocking {
        val detector = AppVersionDetector(context, versionRepo)

        val duration = measureTimeMillis {
            repeat(100) {
                detector.detectVersionChange("com.google.android.gm")
            }
        }

        val avgDuration = duration / 100
        assertTrue("Version detection took ${avgDuration}ms (target: <50ms)", avgDuration < 50)
    }

    @Test
    fun benchmark_cleanup10KCommands_shouldComplete_under1s() = runBlocking {
        // Insert 10K test commands
        val commands = generateTestCommands(10000)
        commandRepo.insertBatch(commands)

        val duration = measureTimeMillis {
            cleanupManager.executeCleanup(gracePeriodDays = 30, keepUserApproved = true)
        }

        assertTrue("Cleanup took ${duration}ms (target: <1000ms)", duration < 1000)
    }
}
```

**Files to Create:**
- `VersionManagementBenchmarkTest.kt` (~200 lines)

**Completion Criteria:**
- [ ] Benchmark tests created
- [ ] All benchmarks pass on reference device
- [ ] Performance metrics documented

---

## Implementation Timeline

### Sprint 1 (1 week): Performance Optimization
- Task 1.1: Intelligent Rescan (3 days)
- Task 1.2: Query Optimization (1 day)
- Task 1.3: Memory Optimization (1 day)
- Testing & validation (2 days)

### Sprint 2 (1 week): User Experience
- Task 2.1: Version Info UI (2 days)
- Task 2.2: Cleanup Preview Screen (3 days)
- Task 2.3: Manual Cleanup Trigger (1 day)
- UI/UX testing (1 day)

### Sprint 3 (1 week): Operational Excellence
- Task 3.1: Timber Logging (2 days)
- Task 3.2: Telemetry/Analytics (2 days)
- Task 3.3: Monitoring Alerts (1 day)
- Integration testing (2 days)

### Sprint 4 (3 days): Code Quality
- Task 4.1: Fix isCleanupScheduled (1 day)
- Task 4.2: Performance Benchmarks (2 days)

**Total Duration:** 4 weeks (assuming 1 developer)

---

## Success Metrics

### Performance
- [ ] 80%+ reduction in rescan time (Task 1.1)
- [ ] 90%+ reduction in N+1 query time (Task 1.2)
- [ ] 90%+ reduction in memory usage (Task 1.3)
- [ ] All benchmarks <target performance (Task 4.2)

### User Experience
- [ ] Version info visible in UI (Task 2.1)
- [ ] Manual cleanup functional (Task 2.2, 2.3)
- [ ] User satisfaction >4.5/5 (post-launch survey)

### Operational
- [ ] Zero println() in production code (Task 3.1)
- [ ] All key metrics tracked (Task 3.2)
- [ ] Alerts configured and tested (Task 3.3)

### Code Quality
- [ ] Technical debt reduced by 80% (Task 4.1, 4.2)
- [ ] All TODO items resolved
- [ ] Code coverage >90%

---

## Risk Assessment

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| **Screen hash collisions** | Medium | Low | Use SHA-256, include position data |
| **UI complexity increases** | Low | Medium | Keep UI simple, progressive disclosure |
| **Performance regression** | High | Low | Benchmark before/after, staged rollout |
| **Analytics data volume** | Medium | Medium | Sample rate control, data aggregation |

---

## Dependencies

### External
- **Timber** (5.0.1): Logging framework
- **Firebase Analytics** (21.5.0): Telemetry
- **Firebase Crashlytics** (18.6.0): Error reporting
- **Guava** (32.1.3-android): Optional for WorkManager (or use coroutines)

### Internal
- Phase 1 completion
- Production deployment
- User feedback collected

---

## Rollout Strategy

### Stage 1: Beta (Week 1-2)
- Deploy to internal testers (50 users)
- Monitor performance metrics
- Collect feedback on UI changes

### Stage 2: Limited Release (Week 3)
- Roll out to 10% of users
- Monitor cleanup effectiveness
- A/B test intelligent rescan

### Stage 3: Full Release (Week 4)
- Roll out to 100% of users
- Monitor all metrics
- Iterate based on data

---

## Documentation Updates

### Files to Update:
- [ ] `VoiceOS-40-Version-Aware-Command-Management-51214-V1.md` (add P2 features)
- [ ] `VoiceOS-Appendix-B-Database-Schema-51711-V1.md` (add v4 schema)
- [ ] `README.md` (update feature list)

### New Documentation:
- [ ] `VoiceOS-Performance-Benchmarks-51215-V1.md`
- [ ] `VoiceOS-Analytics-Dashboard-Guide-51215-V1.md`
- [ ] `VoiceOS-Monitoring-Runbook-51215-V1.md`

---

## Appendix A: Database Schema v4 (Proposed)

### New Columns

**screen_context table:**
```sql
ALTER TABLE screen_context ADD COLUMN screenHash TEXT;
CREATE INDEX idx_sc_hash ON screen_context(packageName, screenHash);
```

### Migration Script

```kotlin
// migrations/3.sqm
ALTER TABLE screen_context ADD COLUMN screenHash TEXT;
CREATE INDEX idx_sc_hash ON screen_context(packageName, screenHash);
```

---

## Appendix B: Estimated Effort

| Task | Effort (person-days) |
|------|---------------------|
| Task 1.1: Intelligent Rescan | 3 |
| Task 1.2: Query Optimization | 1 |
| Task 1.3: Memory Optimization | 1 |
| Task 2.1: Version Info UI | 2 |
| Task 2.2: Cleanup Preview | 3 |
| Task 2.3: Manual Trigger | 1 |
| Task 3.1: Timber Logging | 2 |
| Task 3.2: Analytics | 2 |
| Task 3.3: Monitoring | 1 |
| Task 4.1: Fix isCleanupScheduled | 1 |
| Task 4.2: Benchmarks | 2 |
| **Total** | **19 days** |

With testing, documentation, and buffer: **~4 weeks (1 developer)**

---

## Sign-Off

**Plan Status:** READY FOR APPROVAL

**Prerequisites Checklist:**
- [x] Phase 1 complete
- [x] Production deployed
- [ ] Baseline metrics collected (wait 1 week post-launch)

**Approval Required:**
- [ ] Product Manager: Aman Jhawar
- [ ] Technical Lead: Manoj Jhawar
- [ ] QA Lead: [Name]

---

**End of Plan**

**Next Steps:**
1. Deploy Phase 1 to production
2. Collect baseline metrics (1 week)
3. Get plan approval
4. Begin Sprint 1 (Performance Optimization)
