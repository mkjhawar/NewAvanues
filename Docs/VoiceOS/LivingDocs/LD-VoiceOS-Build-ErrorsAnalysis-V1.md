# VoiceOS Build Errors Analysis

**Status:** Living Document
**Date:** 2025-12-17
**Error Count:** 273 remaining (down from 325)
**Build Task:** `:Modules:VoiceOS:apps:VoiceOSCore:compileDebugKotlin`

---

## Summary

| Category | Error Count | Priority |
|----------|-------------|----------|
| ExplorationEngine.kt | 94 | HIGH |
| LearnAppIntegration.kt | 72 | HIGH |
| ElementLabelOverlay.kt | 23 | MEDIUM |
| JustInTimeLearner.kt | 17 | MEDIUM |
| VoiceOSService.kt | 15 | HIGH |
| VoiceOSCoreDatabaseAdapter.kt | 12 | MEDIUM |
| VUIDCreationDebugOverlay.kt | 10 | LOW |
| LearnAppCore.kt | 7 | MEDIUM |
| SettingsAdapter.kt | 6 | LOW |
| Other files | 17 | LOW |
| **Total** | **273** | |

---

## Error Groups by Type

### Group 1: ScreenExplorer Method Signature Mismatch (18 errors)

**Root Cause:** `ScreenExplorer.exploreScreen()` has a different signature than expected.

**Current signature:** `exploreScreen(rootNode: AccessibilityNodeInfo?): ScreenExplorationResult`

**Expected signature in calls:** `exploreScreen(rootNode, packageName, screenHash, activityName)`

**Files Affected:**
- ExplorationEngine.kt (lines 647, 882, 935, 1006, and more)

**Fix Strategy:**
1. Update `ScreenExplorer` to accept additional parameters, OR
2. Update all calls to pass only `rootNode`

---

### Group 2: Missing Screen/Element Properties (22 errors)

**Missing Properties:**

| Property | Type | Used In |
|----------|------|---------|
| `stableId` | String | ElementInfo (5 references) |
| `scrollableContainerCount` | Int | ScreenExplorationResult (5 references) |
| `elementHash` | String | ElementInfo (4 references) |
| `stabilityScore` | Float | ElementInfo (3 references) |
| `isLongClickable` | Boolean | ElementInfo (2 references) |
| `isEditable` | Boolean | ElementInfo (2 references) |
| `completeness` | Float | ExplorationStats (2 references) |
| `clickedElements` | Int | ExplorationStats (2 references) |

**Fix Strategy:**
1. Add missing properties to data classes
2. Or update code to use existing properties

---

### Group 3: Type Inference Failures (13 errors)

**Error:** "Cannot infer a type for this parameter. Please specify it explicitly."

**Common Pattern:**
```kotlin
// Error: type cannot be inferred
elements.map { it.someProperty }

// Fix: add explicit type
elements.map { element: ElementInfo -> element.someProperty }
```

**Files Affected:**
- ExplorationEngine.kt
- LearnAppIntegration.kt

---

### Group 4: Unresolved Overlay/UI References (13 errors)

**Missing Classes/References:**

| Reference | Purpose | Fix |
|-----------|---------|-----|
| `LoginPromptOverlay` | Login detection UI | Create class or stub |
| `LoginPromptConfig` | Login prompt configuration | Create class |
| `MaterialThemeHelper` | Theme utilities | Create or import |
| `getDebugOverlayManager()` | Debug overlay access | Add method |

**Files Affected:**
- LearnAppIntegration.kt
- VUIDCreationDebugOverlay.kt
- ElementLabelOverlay.kt

---

### Group 5: Missing Enum Values (8 errors)

**Missing Enum Values:**

| Enum | Missing Value | Files |
|------|---------------|-------|
| `ExplorationState` | `Paused` | JustInTimeLearner.kt (5 refs) |
| `ConsentEvent` | `NewAppDetected` | LearnAppIntegration.kt (3 refs) |

**Fix Strategy:**
Add missing values to enum definitions:
```kotlin
enum class ExplorationState {
    IDLE,
    RUNNING,
    PAUSED,  // Add this
    COMPLETED,
    FAILED
}
```

---

### Group 6: Database Schema Mismatches (12 errors)

**Missing DTO Properties:**

| Property | DTO | Fix |
|----------|-----|-----|
| `parentElementHash` | ElementRelationship | Add to schema |
| `childElementHash` | ElementRelationship | Add to schema |
| `createdAt` | ElementRelationship | Add to schema |
| `updatedAt` | ScreenContext | Add to schema |
| `triggerElementHash` | NavigationPath | Add to schema |
| `triggerAction` | NavigationPath | Add to schema |
| `avgDurationMs` | NavigationPath | Add to schema |
| `lastTransitionAt` | NavigationPath | Add to schema |

**File:** VoiceOSCoreDatabaseAdapter.kt

**Fix Strategy:**
1. Update SQLDelight schema in `core/database` module
2. Regenerate DTOs
3. Update adapter code

---

### Group 7: Method Missing on Classes (18 errors)

**Missing Methods:**

| Class | Missing Method | Usage |
|-------|----------------|-------|
| `ConsentManager` | `updatePauseState()` | LearnAppIntegration.kt (3 refs) |
| `ConsentManager` | `savePauseState()` | LearnAppIntegration.kt (2 refs) |
| `LearnAppConfig` | `getScreenHashSimilarityThreshold()` | ExplorationEngine.kt (2 refs) |
| `LearnAppConfig` | `getClickRetryDelayMs()` | ExplorationEngine.kt (2 refs) |
| `LearnAppConfig` | `getMinLabelLength()` | LearnAppCore.kt |
| `LearnAppConfig` | `needsAggressiveFallback()` | LearnAppCore.kt |
| `LearnAppConfig` | `needsModerateFallback()` | LearnAppCore.kt |
| `RenameHintOverlay` | `show()` (public) | ScreenActivityDetector.kt (2 refs) |
| `WindowManager` | `toLogString()` | ExplorationEngine.kt (2 refs) |
| `WindowManager` | `getAppWindows()` | ExplorationEngine.kt (2 refs) |
| `MemoryMonitor` | `logMemoryStats()` | VoiceOSService.kt |
| `MemoryMonitor` | `isMemoryPressureHigh()` | VoiceOSService.kt |
| `MemoryMonitor` | `getMemoryStats()` | VoiceOSService.kt |
| `DatabaseManager` | `initializeSchema()` | ExplorationEngine.kt |
| `DatabaseManager` | `markAppAsFullyLearned()` | ExplorationEngine.kt |

---

### Group 8: Parameter Name Mismatches (6 errors)

**ChecklistManager.addScreen() Signature:**
```kotlin
// Expected by callers:
fun addScreen(screenHash: String, screenTitle: String?, elements: List<ElementInfo>)

// Actual signature:
fun addScreen(screenHash: String, screenName: String?, elementCount: Int)
```

**Fix:** Update calls to use correct parameter names

---

### Group 9: When Expression Completeness (4 errors)

**Files with incomplete when expressions:**
- VoiceOSService.kt (line 1515)
- ExplorationEngine.kt (multiple)
- LearnAppIntegration.kt (multiple)

**Fix:** Add `else` branch or handle all cases

---

### Group 10: Variable Assignment Errors (8 errors)

**Pattern:**
```kotlin
// Error: Variable expected
someProperty = value  // where someProperty is val or doesn't exist

// Fix: Use var or correct property name
```

---

### Group 11: Coroutine/Suspend Errors (3 errors)

**Error:** "Suspend function should be called only from a coroutine or another suspend function"

**Files:**
- AppMetadataProvider.kt (line 106)
- ExplorationEngine.kt

**Fix:** Wrap in `runBlocking { }` or make calling function suspend

---

### Group 12: Resource References (3 errors)

**Missing Resources:**

| Resource | Type | File |
|----------|------|------|
| `learnapp_ic_warning` | drawable | AccessibilityServiceMonitor.kt |
| `txt_top_filtered` | id | VUIDCreationDebugOverlay.kt |
| `txt_timestamp` | id | VUIDCreationDebugOverlay.kt |

**Fix:** Create missing drawable/layout resources

---

## Recommended Fix Order

### Phase 1: High-Impact Fixes (estimated 50% reduction)

1. **Fix ScreenExplorer signature** - Resolves 18 errors
2. **Add missing ElementInfo properties** - Resolves ~15 errors
3. **Add missing enum values** - Resolves 8 errors
4. **Fix ChecklistManager parameter names** - Resolves 6 errors

### Phase 2: Database Schema Updates

1. Update SQLDelight schema in `core/database`
2. Add missing columns to tables
3. Regenerate code
4. Update VoiceOSCoreDatabaseAdapter.kt - Resolves 12 errors

### Phase 3: Missing Class Creation

1. Create `LoginPromptOverlay` and `LoginPromptConfig`
2. Add missing methods to existing classes
3. Create stubs for utilities

### Phase 4: Code Cleanup

1. Add type annotations where needed
2. Complete when expressions
3. Fix coroutine calls
4. Add missing resources

---

## Files by Priority

### Priority 1: Fix First
- `learnapp/exploration/ExplorationEngine.kt` (94 errors)
- `learnapp/integration/LearnAppIntegration.kt` (72 errors)
- `accessibility/VoiceOSService.kt` (15 errors)

### Priority 2: Fix Second
- `learnapp/ui/ElementLabelOverlay.kt` (23 errors)
- `learnapp/jit/JustInTimeLearner.kt` (17 errors)
- `database/VoiceOSCoreDatabaseAdapter.kt` (12 errors)

### Priority 3: Fix Last
- `learnapp/metrics/VUIDCreationDebugOverlay.kt` (10 errors)
- `learnapp/core/LearnAppCore.kt` (7 errors)
- `learnapp/settings/ui/SettingsAdapter.kt` (6 errors)
- Other files (17 errors)

---

## Document History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-17 | Initial analysis of 273 remaining errors |

