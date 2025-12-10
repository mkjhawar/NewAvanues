# Issue: LearnApp Incorrectly Marks Partially Learned Apps as LEARNED

## Status
| Field | Value |
|-------|-------|
| Module | LearnApp |
| Severity | **HIGH** |
| Status | **ROOT CAUSE IDENTIFIED** |
| Date | 2025-12-06 |
| Affects | v1.6.0 |

## Symptoms

### User Report
Teams app exploration completed with:
- Only "couple of screens" learned
- No left menu elements clicked
- Logs show: "Not marking as fully learned (threshold: 95%)"
- Database shows: `progress=100`, `status='LEARNED'`
- Consent dialog does NOT show on app reopen (app thinks it's already learned)

### Evidence from Logs

**ExplorationEngine_TeamsApp_Logs.txt:**
```
14:39:49.081 ExplorationEngine I  Exploration Statistics:
                                  Screens: 4 total
                                    - Fully explored: 0 (100%)
                                    - Partially explored: 4 (1-99%)
                                    - Unexplored: 0 (0%)
                                  Elements: 10/41 clicked
                                  Overall Completeness: 24.4%
14:39:49.081 ExplorationEngine W  ⚠️ App partially learned (24.390244%)
14:39:49.081 ExplorationEngine W     10/41 elements clicked
14:39:49.081 ExplorationEngine W     0/4 screens fully explored
14:39:49.081 ExplorationEngine W     Not marking as fully learned (threshold: 95%)
```

**voiceos_db.sql:**
```sql
INSERT INTO learned_apps VALUES(
    'com.microsoft.teams',
    'com.microsoft.teams',
    2024453030,
    '1416/1.0.0.2024053003',
    1765012189233,
    1765012189233,
    5,    -- total_screens
    26,   -- total_elements
    '2f8eb96f84b434c85ac08028de99f1c6',
    'COMPLETE',      -- exploration_status (WRONG!)
    'AUTO_DETECT',
    'LEARNED',       -- status (WRONG!)
    100,             -- progress (WRONG! Should be 24)
    26,              -- command_count
    5,               -- screens_explored
    1
);
```

## Root Cause Analysis (ToT)

### Hypothesis 1: ExplorationEngine Incorrectly Marks Apps ❌
**Likelihood:** Low

**Evidence:**
```kotlin
// ExplorationEngine.kt:375-386
if (clickStats.overallCompleteness >= developerSettings.getCompletenessThresholdPercent()) {
    Log.i("ExplorationEngine", "✅ App fully learned (${clickStats.overallCompleteness}%)!")
    repository.markAppAsFullyLearned(packageName, System.currentTimeMillis())
} else {
    Log.w("ExplorationEngine", "⚠️ App partially learned (${clickStats.overallCompleteness}%)")
    Log.w("ExplorationEngine", "   Not marking as fully learned (threshold: 95%)")
}
```

**Analysis:** ExplorationEngine correctly checks completeness (24.4% < 95%) and does NOT call `markAppAsFullyLearned()`. Logs confirm this path executed correctly.

**Conclusion:** ❌ Not the root cause

---

### Hypothesis 2: LearnAppRepository Always Hardcodes LEARNED Status ✅
**Likelihood:** **HIGH**

**Evidence:**
```kotlin
// LearnAppRepository.kt:87-112
suspend fun saveLearnedApp(
    packageName: String,
    appName: String,
    versionCode: Long,
    versionName: String,
    stats: ExplorationStats
) = withContext(Dispatchers.IO) {
    databaseManager.learnedAppQueries.insertLearnedApp(
        package_name = packageName,
        app_name = appName,
        version_code = versionCode,
        version_name = versionName,
        first_learned_at = System.currentTimeMillis(),
        last_updated_at = System.currentTimeMillis(),
        total_screens = stats.totalScreens.toLong(),
        total_elements = stats.totalElements.toLong(),
        app_hash = calculateAppHashWithVersion(packageName, versionCode, versionName),
        exploration_status = ExplorationStatus.COMPLETE,  // HARDCODED!
        learning_mode = "AUTO_DETECT",
        status = "LEARNED",   // HARDCODED! Should be conditional
        progress = 100,       // HARDCODED! Should be actual completeness
        command_count = stats.totalElements.toLong(),
        screens_explored = stats.totalScreens.toLong(),
        is_auto_detect_enabled = 1
    )
}
```

**Call Site:**
```kotlin
// LearnAppIntegration.kt:459-475
is ExplorationState.Completed -> {
    // Save results with actual version info from PackageManager
    scope.launch {
        val metadata = metadataProvider.getMetadata(state.packageName)
        repository.saveLearnedApp(  // ALWAYS CALLED on Completed state
            packageName = state.packageName,
            appName = state.stats.appName,
            versionCode = metadata?.versionCode ?: 1L,
            versionName = metadata?.versionName ?: "unknown",
            stats = state.stats
        )
        ...
    }
}
```

**Analysis:**
1. `ExplorationState.Completed` is emitted even when exploration is only 24% complete
2. `LearnAppIntegration` ALWAYS calls `saveLearnedApp()` on `Completed` state
3. `saveLearnedApp()` hardcodes `status = "LEARNED"` and `progress = 100`
4. This overwrites any partial progress tracking

**Conclusion:** ✅ **This is the root cause**

---

### Hypothesis 3: ExplorationStats Missing Completeness Field ✅
**Likelihood:** Medium

**Evidence:**
```kotlin
// ExplorationStats.kt (data class)
data class ExplorationStats(
    val packageName: String,
    val appName: String,
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val durationMs: Long,
    val maxDepth: Int,
    val dangerousElementsSkipped: Int,
    val loginScreensDetected: Int,
    val scrollableContainersFound: Int
    // MISSING: val completeness: Float
)
```

**Analysis:** `ExplorationStats` does NOT include a `completeness` field, so `saveLearnedApp()` cannot access the actual 24.4% completeness value. It has no way to set the correct progress.

**Conclusion:** ✅ **Contributing factor** - stats object is missing critical data

---

## Selected Root Cause (CoT Trace)

**Primary Bug:** `LearnAppRepository.saveLearnedApp()` hardcodes `status = "LEARNED"` and `progress = 100` regardless of actual exploration completeness.

**Step-by-step failure:**

1. **Exploration runs** (ExplorationEngine)
   - Teams app: 10/41 elements clicked (24.4% completeness)
   - Correctly identifies as "partially learned"
   - Does NOT call `markAppAsFullyLearned()`

2. **Exploration completes** (DFS stack empty)
   - Emits `ExplorationState.Completed(packageName, stats)`
   - Note: "Completed" means "exploration finished", NOT "app fully learned"

3. **Integration layer handles Completed state** (LearnAppIntegration)
   - Calls `repository.saveLearnedApp(packageName, appName, versionCode, versionName, stats)`
   - This is called for BOTH fully learned AND partially learned apps

4. **Database save hardcodes LEARNED status** (LearnAppRepository)
   ```kotlin
   status = "LEARNED",   // WRONG: Should be "NOT_LEARNED" or "PARTIAL"
   progress = 100,       // WRONG: Should be 24 (from completeness)
   ```

5. **Consent dialog checks status on app launch** (ConsentDialogManager)
   - Sees `status = "LEARNED"` → assumes app already learned
   - Does NOT show consent dialog
   - User cannot re-trigger exploration

## Secondary Issues

### Issue 2.1: Left Menu Not Clicked
**Reason:** Exploration only reached 4 screens before external intent navigation:
```
14:39:43.999 Exploration...HybridCLite W  Package changed from com.microsoft.teams to com.realwear.launcher, aborting
14:39:45.852 ExplorationEngine W  BACK presses failed, attempting to relaunch com.microsoft.teams via intent
```

After intent relaunch, entry point screen was already visited, so exploration terminated (this is correct behavior per Hybrid C-Lite design).

### Issue 2.2: Database Size (951 KB)
**Reason:** This was fixed in v1.6 with UNIQUE constraint (commit 87472188). The database dump is from BEFORE the fix was applied. Post-fix database would be much smaller.

## Fix Plan

### Fix 1: Add Completeness to ExplorationStats ✅ Required
```kotlin
// ExplorationStats.kt
data class ExplorationStats(
    val packageName: String,
    val appName: String,
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val durationMs: Long,
    val maxDepth: Int,
    val dangerousElementsSkipped: Int,
    val loginScreensDetected: Int,
    val scrollableContainersFound: Int,
    val completeness: Float  // NEW: Overall completeness percentage (0-100)
)
```

### Fix 2: Update createExplorationStats to Include Completeness ✅ Required
```kotlin
// ExplorationEngine.kt:2881
private suspend fun createExplorationStats(packageName: String): ExplorationStats {
    val stats = screenStateManager.getStats()
    val graph = navigationGraphBuilder.build()
    val graphStats = graph.getStats()
    val elapsed = System.currentTimeMillis() - startTimestamp

    // Get actual completeness from click tracker
    val clickStats = clickTracker.getStats()

    // ... AI context generation ...

    return ExplorationStats(
        packageName = packageName,
        appName = packageName,
        totalScreens = stats.totalScreensDiscovered,
        totalElements = graphStats.totalElements,
        totalEdges = graphStats.totalEdges,
        durationMs = elapsed,
        maxDepth = graphStats.maxDepth,
        dangerousElementsSkipped = dangerousElementsSkipped,
        loginScreensDetected = loginScreensDetected,
        scrollableContainersFound = scrollableContainersFound,
        completeness = clickStats.overallCompleteness  // NEW: Actual completeness
    )
}
```

### Fix 3: Make saveLearnedApp Status/Progress Conditional ✅ Required
```kotlin
// LearnAppRepository.kt:87
suspend fun saveLearnedApp(
    packageName: String,
    appName: String,
    versionCode: Long,
    versionName: String,
    stats: ExplorationStats
) = withContext(Dispatchers.IO) {
    // Determine status based on actual completeness
    val threshold = 95f  // Or get from settings
    val isFullyLearned = stats.completeness >= threshold

    databaseManager.learnedAppQueries.insertLearnedApp(
        package_name = packageName,
        app_name = appName,
        version_code = versionCode,
        version_name = versionName,
        first_learned_at = System.currentTimeMillis(),
        last_updated_at = System.currentTimeMillis(),
        total_screens = stats.totalScreens.toLong(),
        total_elements = stats.totalElements.toLong(),
        app_hash = calculateAppHashWithVersion(packageName, versionCode, versionName),
        exploration_status = if (isFullyLearned) ExplorationStatus.COMPLETE else ExplorationStatus.PARTIAL,
        learning_mode = "AUTO_DETECT",
        status = if (isFullyLearned) "LEARNED" else "NOT_LEARNED",  // FIX: Conditional
        progress = stats.completeness.toInt(),  // FIX: Actual progress (0-100)
        command_count = stats.totalElements.toLong(),
        screens_explored = stats.totalScreens.toLong(),
        is_auto_detect_enabled = 1
    )
}
```

### Fix 4: Remove Redundant markAppAsFullyLearned Call (Optional Cleanup)
```kotlin
// ExplorationEngine.kt:375-381
// BEFORE:
if (clickStats.overallCompleteness >= developerSettings.getCompletenessThresholdPercent()) {
    Log.i("ExplorationEngine", "✅ App fully learned (${clickStats.overallCompleteness}%)!")
    repository.markAppAsFullyLearned(packageName, System.currentTimeMillis())
}

// AFTER: Remove this call - status is now set correctly in saveLearnedApp()
// (Keep the log for debugging)
if (clickStats.overallCompleteness >= developerSettings.getCompletenessThresholdPercent()) {
    Log.i("ExplorationEngine", "✅ App fully learned (${clickStats.overallCompleteness}%)!")
    // repository.markAppAsFullyLearned() no longer needed - saveLearnedApp handles it
}
```

## Prevention

### Code Review Checklist
- [ ] Database status fields must NEVER be hardcoded in production code
- [ ] Always use actual metrics (completeness, progress) from exploration results
- [ ] ExplorationState.Completed ≠ "App fully learned" (naming confusion)
- [ ] Test both success paths (>95%) AND partial paths (<95%)

### Testing Requirements
- [ ] Test with app that reaches 100% completeness → status='LEARNED', progress=100
- [ ] Test with app that reaches <95% completeness → status='NOT_LEARNED', progress=actual%
- [ ] Verify consent dialog shows for partially learned apps on reopen
- [ ] Verify consent dialog does NOT show for fully learned apps

### Architectural Improvements
- Consider renaming `ExplorationState.Completed` to `ExplorationState.Finished` to avoid confusion
- Consider adding validation: `require(progress in 0..100)`
- Consider enum for status: `enum class LearnStatus { NOT_LEARNED, PARTIAL, LEARNED }`

## Related Issues
- Issue #1: Power Down Detection (v1.5) - Dangerous patterns
- Issue #2: Intent Relaunch Recovery (v1.5) - Premature termination
- Issue #3: Duplicate Commands (v1.6) - UNIQUE constraint

## Files Affected
| File | Change Required |
|------|-----------------|
| `ExplorationStats.kt` | Add `completeness: Float` field |
| `ExplorationEngine.kt` | Pass `clickStats.overallCompleteness` to stats |
| `LearnAppRepository.kt` | Make status/progress conditional on completeness |

## Severity Justification

**HIGH** because:
1. **User Impact:** Apps incorrectly marked as learned cannot be re-learned
2. **Data Integrity:** Database contains incorrect progress/status data
3. **UX Broken:** Consent dialog logic relies on correct status
4. **Scope:** Affects ALL apps learned with <95% completeness since v1.0.0

## Test Cases

### Test Case 1: Fully Learned App (>= 95%)
```kotlin
// Setup
val stats = ExplorationStats(
    packageName = "com.test.app",
    appName = "Test App",
    totalScreens = 10,
    totalElements = 100,
    completeness = 98.5f  // >= 95%
)

// Action
repository.saveLearnedApp("com.test.app", "Test App", 1L, "1.0", stats)

// Assert
val app = repository.getLearnedApp("com.test.app")
assertEquals("LEARNED", app.status)
assertEquals(98, app.progress)  // Rounded down
assertEquals(ExplorationStatus.COMPLETE, app.explorationStatus)
```

### Test Case 2: Partially Learned App (< 95%)
```kotlin
// Setup
val stats = ExplorationStats(
    packageName = "com.microsoft.teams",
    appName = "Teams",
    totalScreens = 4,
    totalElements = 41,
    completeness = 24.4f  // < 95%
)

// Action
repository.saveLearnedApp("com.microsoft.teams", "Teams", 1L, "1.0", stats)

// Assert
val app = repository.getLearnedApp("com.microsoft.teams")
assertEquals("NOT_LEARNED", app.status)  // NOT "LEARNED"!
assertEquals(24, app.progress)  // NOT 100!
assertEquals(ExplorationStatus.PARTIAL, app.explorationStatus)
```

### Test Case 3: Consent Dialog Shows for Partial Apps
```kotlin
// Given: App partially learned (24%)
repository.saveLearnedApp("com.test.partial", "Partial", 1L, "1.0", partialStats)

// When: App launched
val shouldShow = consentDialogManager.shouldShowConsent("com.test.partial")

// Then: Consent dialog SHOULD show (app not fully learned)
assertTrue(shouldShow)
```

---

**End of Issue Analysis**
