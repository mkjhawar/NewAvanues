# VoiceOS Technical Debt Resolution Plan

**Module:** VoiceOS/apps/VoiceOSCore
**Created:** 2025-12-17
**Version:** 1.0.0
**Mode:** .cot .tot .rot .swarm

---

## Executive Summary

| Metric | Value |
|--------|-------|
| Total Tasks | 42 |
| Phases | 3 |
| Estimated Effort | High (multi-day) |
| Swarm Recommended | YES (30+ files, 3 phases) |
| Risk Level | CRITICAL (ANR, memory leaks) |

---

## Reasoning Analysis

### Chain-of-Thought (CoT) - Sequential Dependencies

```
1. runBlocking removal → requires handler interface changes → affects all 13 handlers
2. Overlay state fixes → requires ComposeView understanding → affects 4 overlays
3. GlassMorphism creation → utility file → no dependencies
4. !! removal → independent per file → parallelizable
5. Room→SQLDelight → requires schema mapping → affects 27 files
6. Transaction wrappers → requires database understanding → affects adapters
7. applyTimeDecay → requires algorithm design → affects 4 files
8. VoiceOSService decomposition → major refactor → depends on all above
9. ActionCoordinator extraction → depends on runBlocking fix
10. Handler interface abstraction → depends on coordinator extraction
```

**Critical Path:** runBlocking → ActionCoordinator → Handler Interface → VoiceOSService

### Tree-of-Thought (ToT) - Alternative Approaches

```
                    ┌─────────────────────────────────────┐
                    │     Fix VoiceOSCore Tech Debt       │
                    └─────────────────┬───────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          ▼                           ▼                           ▼
   ┌──────────────┐          ┌──────────────┐          ┌──────────────┐
   │  Approach A  │          │  Approach B  │          │  Approach C  │
   │ Sequential   │          │ By Severity  │          │ By Proximity │
   └──────┬───────┘          └──────┬───────┘          └──────┬───────┘
          │                         │                         │
  Risk: High                 Risk: Medium              Risk: LOW ✓
  Time: Longest              Time: Medium              Time: Shortest
  Conflicts: Many            Conflicts: Some           Conflicts: Minimal
```

**Selected:** Approach C (By Proximity) - Groups related files to minimize merge conflicts

### Recursive-of-Thought (RoT) - Decomposition

```
Fix Tech Debt
├── Phase 1: Critical Runtime (P0)
│   ├── Subproblem 1.1: runBlocking in ActionCoordinator
│   │   ├── Leaf: Make executeAction suspend
│   │   ├── Leaf: Add executeActionSync wrapper
│   │   └── Leaf: Update 6 callers
│   ├── Subproblem 1.2: Overlay State Antipatterns
│   │   ├── Leaf: Fix CommandStatusOverlay (3 states)
│   │   ├── Leaf: Fix ConfidenceOverlay (2 states)
│   │   ├── Leaf: Fix ContextMenuOverlay (4 states)
│   │   └── Leaf: Fix NumberedSelectionOverlay (3 states)
│   ├── Subproblem 1.3: GlassMorphism utility
│   │   └── Leaf: Create GlassMorphismModifier.kt
│   └── Subproblem 1.4: Force unwrap operators
│       ├── Leaf: BluetoothHandler.kt (4 occurrences)
│       ├── Leaf: WebCommandCoordinator.kt (3 occurrences)
│       └── Leaf: Others (15 occurrences)
├── Phase 2: Data Integrity (P1)
│   ├── Subproblem 2.1: Room→SQLDelight Migration
│   │   ├── Leaf: scraping/entities (8 files)
│   │   ├── Leaf: scraping/dao (8 files)
│   │   ├── Leaf: learnweb (4 files)
│   │   └── Leaf: learnapp/database (4 files)
│   ├── Subproblem 2.2: Transaction Wrappers
│   │   ├── Leaf: VoiceOSCoreDatabaseAdapter
│   │   └── Leaf: LearnAppDatabaseAdapter
│   └── Subproblem 2.3: applyTimeDecay Implementation
│       ├── Leaf: SQLDelightCommandUsageRepository
│       └── Leaf: SQLDelightContextPreferenceRepository
└── Phase 3: Architecture (P2)
    ├── Subproblem 3.1: VoiceOSService Decomposition
    │   ├── Leaf: Extract VoiceRecognitionManager
    │   ├── Leaf: Extract OverlayCoordinator
    │   └── Leaf: Extract CommandDispatcher
    ├── Subproblem 3.2: ActionCoordinator Extraction
    │   ├── Leaf: Extract MetricsCollector
    │   └── Leaf: Extract HandlerRegistry
    └── Subproblem 3.3: Handler Interface Abstraction
        ├── Leaf: Create IVoiceOSContext interface
        └── Leaf: Update 13 handlers
```

---

## Phase 1: Critical Runtime Fixes (P0)

**Priority:** CRITICAL
**Risk:** ANR, Memory Leaks, Crashes
**Swarm Agents:** 4

### Task Group 1.1: Remove runBlocking (ANR Prevention)

| Task | File | Change |
|------|------|--------|
| 1.1.1 | ActionCoordinator.kt:306 | Change `executeAction` to suspend function |
| 1.1.2 | ActionCoordinator.kt | Add `executeActionSync` wrapper using `runBlocking` for legacy callers |
| 1.1.3 | VoiceOSService.kt | Update callers to use `executeActionAsync` or suspend context |
| 1.1.4 | AccessibilityScrapingIntegration.kt | Remove runBlocking, use coroutine scope |
| 1.1.5 | LearnAppIntegration.kt | Remove runBlocking, use coroutine scope |
| 1.1.6 | LearnAppDatabaseAdapter.kt | Remove runBlocking, use withContext |

**Before (Line 306):**
```kotlin
val result = runBlocking {
    withTimeoutOrNull(HANDLER_TIMEOUT_MS) {
        handler.execute(category, action, params)
    }
} ?: false
```

**After:**
```kotlin
// In suspend executeAction:
val result = withTimeoutOrNull(HANDLER_TIMEOUT_MS) {
    handler.execute(category, action, params)
} ?: false

// Synchronous wrapper for legacy code:
fun executeActionSync(action: String, params: Map<String, Any> = emptyMap()): Boolean {
    return runBlocking(Dispatchers.Default) { // NOT Main
        executeAction(action, params)
    }
}
```

### Task Group 1.2: Fix Overlay State Antipatterns (Memory Leaks)

| Task | File | Change |
|------|------|--------|
| 1.2.1 | CommandStatusOverlay.kt:71-73 | Change `= mutableStateOf` to `by mutableStateOf` |
| 1.2.2 | ConfidenceOverlay.kt | Change `= mutableStateOf` to `by mutableStateOf` |
| 1.2.3 | ContextMenuOverlay.kt | Change `= mutableStateOf` to `by mutableStateOf` |
| 1.2.4 | NumberedSelectionOverlay.kt | Change `= mutableStateOf` to `by mutableStateOf` |

**Before:**
```kotlin
private var commandState = mutableStateOf("")
private var stateState = mutableStateOf(CommandState.LISTENING)
private var messageState = mutableStateOf<String?>(null)
```

**After:**
```kotlin
private var commandState by mutableStateOf("")
private var stateState by mutableStateOf(CommandState.LISTENING)
private var messageState by mutableStateOf<String?>(null)
```

### Task Group 1.3: Create GlassMorphism Utility

| Task | File | Action |
|------|------|--------|
| 1.3.1 | accessibility/ui/utils/GlassMorphismModifier.kt | CREATE - Glass morphism Compose modifier |

**Target Path:** `apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/ui/utils/GlassMorphismModifier.kt`

**Reference:** `libraries/VoiceUIElements/themes/arvision/GlassMorphism.kt`

### Task Group 1.4: Replace Force Unwrap Operators (!!)

| Task | File | Count | Fix Pattern |
|------|------|-------|-------------|
| 1.4.1 | BluetoothHandler.kt | 4 | `?.let { }` or `?: return` |
| 1.4.2 | WebCommandCoordinator.kt | 3 | `?.let { }` or `?: return` |
| 1.4.3 | NumberOverlayConfig.kt | 2 | `?: defaultValue` |
| 1.4.4 | AccessibilityScrapingIntegration.kt | 2 | `?.let { }` |
| 1.4.5 | ReturnValueComparator.kt | 2 | `?: return` |
| 1.4.6 | LearnWebActivity.kt | 1 | `?: return` |
| 1.4.7 | WebCommandGenerator.kt | 1 | `?: return` |
| 1.4.8 | SnapToElementHandler.kt | 1 | `?.let { }` |
| 1.4.9 | ActionCoordinator.kt | 1 | `?: return` |
| 1.4.10 | RenameHintOverlay.kt | 1 | `?: return` |
| 1.4.11 | DebugOverlayManager.kt | 1 | `?: return` |
| 1.4.12 | ElementInfo.kt | 1 | `?: ""` |
| 1.4.13 | ExplorationEngine.kt | 1 | `?: return` |
| 1.4.14 | JustInTimeLearner.kt | 1 | `?: return` |

---

## Phase 2: Data Integrity (P1)

**Priority:** HIGH
**Risk:** Data corruption, Race conditions
**Swarm Agents:** 3

### Task Group 2.1: Room → SQLDelight Migration

#### 2.1.1 scraping/entities (8 files → DELETE after migration)

| File | SQLDelight Equivalent |
|------|----------------------|
| ScreenContextEntity.kt | ScreenContext.sq |
| ScreenTransitionEntity.kt | ScreenTransition.sq |
| UserInteractionEntity.kt | UserInteraction.sq |
| ElementStateHistoryEntity.kt | ElementStateHistory.sq |
| GeneratedCommandEntity.kt | GeneratedCommand.sq |
| ScrapedAppEntity.kt | ScrapedApp.sq |
| ScrapedElementEntity.kt | ScrapedElement.sq |
| ScrapedHierarchyEntity.kt | ScrapedHierarchy.sq |

#### 2.1.2 scraping/dao (8 files → DELETE after migration)

| File | Action |
|------|--------|
| ScrapedAppDao.kt | Migrate to SQLDelight repository |
| ScrapedElementDao.kt | Migrate to SQLDelight repository |
| ScrapedHierarchyDao.kt | Migrate to SQLDelight repository |
| ScreenContextDao.kt | Migrate to SQLDelight repository |
| ScreenTransitionDao.kt | Migrate to SQLDelight repository |
| UserInteractionDao.kt | Migrate to SQLDelight repository |
| ElementRelationshipDao.kt | Migrate to SQLDelight repository |
| ElementStateHistoryDao.kt | Migrate to SQLDelight repository |
| GeneratedCommandDao.kt | Migrate to SQLDelight repository |

#### 2.1.3 scraping/database (1 file)

| File | Action |
|------|--------|
| AppScrapingDatabase.kt | DELETE - Replace with VoiceOSDatabase |

#### 2.1.4 learnweb (4 files)

| File | Action |
|------|--------|
| GeneratedWebCommandDao.kt | Migrate to SQLDelight |
| ScrapedWebElementDao.kt | Migrate to SQLDelight |
| ScrapedWebsiteDao.kt | Migrate to SQLDelight |
| WebScrapingDatabase.kt | DELETE |

#### 2.1.5 learnapp/database/entities (4 files)

| File | Action |
|------|--------|
| ExplorationSessionEntity.kt | Migrate to SQLDelight |
| LearnedAppEntity.kt | Migrate to SQLDelight |
| NavigationEdgeEntity.kt | Migrate to SQLDelight |
| ScreenStateEntity.kt | Migrate to SQLDelight |

### Task Group 2.2: Add Transaction Wrappers

| Task | File | Change |
|------|------|--------|
| 2.2.1 | VoiceOSCoreDatabaseAdapter.kt | Wrap batch operations in `transactionWithResult` |
| 2.2.2 | LearnAppDatabaseAdapter.kt | Wrap batch operations in `transactionWithResult` |

**Pattern:**
```kotlin
suspend fun batchInsert(items: List<Item>) {
    database.transactionWithResult {
        items.forEach { item ->
            queries.insert(item)
        }
    }
}
```

### Task Group 2.3: Implement applyTimeDecay

| Task | File | Implementation |
|------|------|----------------|
| 2.3.1 | SQLDelightCommandUsageRepository.kt | Implement exponential decay algorithm |
| 2.3.2 | SQLDelightContextPreferenceRepository.kt | Implement exponential decay algorithm |

**Algorithm:**
```kotlin
suspend fun applyTimeDecay(decayFactor: Float = 0.95f, intervalHours: Int = 24) {
    val cutoffTime = Clock.System.now().minus(intervalHours.hours).toEpochMilliseconds()
    database.transactionWithResult {
        queries.applyDecay(decayFactor, cutoffTime)
    }
}
```

---

## Phase 3: Architecture Refactoring (P2)

**Priority:** MEDIUM
**Risk:** Regression, API changes
**Swarm Agents:** 2

### Task Group 3.1: Decompose VoiceOSService

| Task | Extract To | Responsibilities |
|------|-----------|-----------------|
| 3.1.1 | VoiceRecognitionManager.kt | Speech recognition lifecycle |
| 3.1.2 | OverlayCoordinator.kt | Overlay display management |
| 3.1.3 | CommandDispatcher.kt | Command routing and execution |
| 3.1.4 | ServiceLifecycleManager.kt | Service lifecycle events |

### Task Group 3.2: Extract ActionCoordinator Responsibilities

| Task | Extract To | Responsibilities |
|------|-----------|-----------------|
| 3.2.1 | MetricsCollector.kt | Performance metrics tracking |
| 3.2.2 | HandlerRegistry.kt | Handler registration and lookup |

### Task Group 3.3: Abstract Handler Dependencies

| Task | File | Change |
|------|------|--------|
| 3.3.1 | IVoiceOSContext.kt | CREATE - Interface for service dependencies |
| 3.3.2 | All 13 handlers | Change `service: VoiceOSService` to `context: IVoiceOSContext` |

**Interface Design:**
```kotlin
interface IVoiceOSContext {
    val accessibilityService: AccessibilityService
    val windowManager: WindowManager
    val packageManager: PackageManager
    fun getRootNode(): AccessibilityNodeInfo?
    fun performGlobalAction(action: Int): Boolean
}
```

---

## Swarm Configuration

### Agent Distribution

| Agent | Phase | Tasks | Files |
|-------|-------|-------|-------|
| Agent 1 | 1.1 | runBlocking removal | 6 files |
| Agent 2 | 1.2 + 1.3 | Overlay fixes + GlassMorphism | 5 files |
| Agent 3 | 1.4 | Force unwrap fixes | 14 files |
| Agent 4 | 2.1 (part 1) | Room migration (scraping) | 17 files |
| Agent 5 | 2.1 (part 2) | Room migration (learnweb/learnapp) | 8 files |
| Agent 6 | 2.2 + 2.3 | Transactions + Time decay | 4 files |
| Agent 7 | 3.1 | VoiceOSService decomposition | 5 files |
| Agent 8 | 3.2 + 3.3 | ActionCoordinator + Handler abstraction | 16 files |

### Execution Order

```
Phase 1 (Parallel - Critical Runtime)
├── Agent 1: runBlocking ─────────┐
├── Agent 2: Overlays ────────────┼── Can run in parallel
└── Agent 3: !! operators ────────┘

Phase 2 (Parallel - Data Integrity) [After Phase 1]
├── Agent 4: Room migration (scraping)
├── Agent 5: Room migration (other)
└── Agent 6: Transactions + decay

Phase 3 (Sequential - Architecture) [After Phase 2]
├── Agent 7: VoiceOSService decomposition
└── Agent 8: ActionCoordinator + Handlers [After Agent 7]
```

---

## Success Criteria

| Criterion | Target |
|-----------|--------|
| Build passes | `./gradlew :Modules:VoiceOS:apps:VoiceOSCore:compileDebugKotlin` |
| Zero runBlocking on Main | No ANR risk |
| Zero Room dependencies | SQLDelight only |
| Zero !! operators | Safe null handling |
| State delegate pattern | All overlays use `by` |
| Transaction coverage | All batch operations wrapped |
| Time decay implemented | Both repositories complete |

---

## Rollback Strategy

Each phase is independently deployable. If issues arise:

1. **Phase 1 rollback:** Revert overlay changes, restore runBlocking (temporary)
2. **Phase 2 rollback:** Keep Room and SQLDelight parallel (data duplication)
3. **Phase 3 rollback:** Keep monolithic services (no functional change)

---

## Files Summary

| Category | Count |
|----------|-------|
| Files to Modify | 48 |
| Files to Create | 8 |
| Files to Delete | 25 |
| Total Changes | 81 |

---

**Plan Version:** 1.0.0
**Created:** 2025-12-17
**Author:** VoiceOS Development Team
