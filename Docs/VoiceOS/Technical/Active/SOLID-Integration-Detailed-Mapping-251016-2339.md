# SOLID Integration Detailed Mapping

**Created:** 2025-10-16 23:39:52 PDT
**Purpose:** Detailed mapping of VoiceOSService inline code → SOLID components
**Status:** Ready for phased integration

---

## Overview

**What We Have:**
- ✅ 7 SOLID implementation classes (8,200+ LOC) - Fully functional
- ✅ 7 Interface definitions - Complete contracts
- ✅ 496 comprehensive tests - All written
- ✅ Everything compiles successfully

**What We Need:**
- Replace VoiceOSService inline logic with calls to SOLID components
- Reduce VoiceOSService from 1,385 LOC → ~400 LOC orchestration layer
- Maintain 100% functional equivalence
- Test after each component integration

---

## Component-by-Component Integration Map

### Phase 1: StateManager Integration (LOWEST RISK - 2 hours)

**SOLID Component:**
- **File:** `StateManagerImpl.kt` (687 LOC)
- **Interface:** `IStateManager`
- **Tests:** 70 tests in `StateManagerImplTest.kt`
- **Dependencies:** None (standalone)

**VoiceOSService Code to Replace:**

| Current Field/Method | Line(s) | Replace With | Component Method |
|---------------------|---------|--------------|------------------|
| `private var isServiceReady = false` | 127 | Delete field | `stateManager.updateReadiness(ready)` |
| `private var isVoiceInitialized = false` | 132 | Delete field | `stateManager.updateVoiceReady(ready)` |
| `private var foregroundServiceActive = false` | 137 | Delete field | `stateManager.updateForegroundState(active)` |
| `private var appInBackground = false` | 138 | Delete field | `stateManager.updateBackgroundState(background)` |
| `private var voiceSessionActive = false` | 139 | Delete field | `stateManager.updateVoiceSessionState(active)` |
| Direct state reads throughout | Multiple | Replace with | `stateManager.serviceState.value.isServiceReady` |

**New Code to Add:**
```kotlin
// Field declaration
@Inject
lateinit var stateManager: IStateManager

// In onCreate() or onServiceConnected()
// StateManager is injected by Hilt - just initialize
lifecycleScope.launch {
    stateManager.initialize()
}

// Replace state updates
// OLD: isServiceReady = true
// NEW: stateManager.updateReadiness(true)

// Replace state reads
// OLD: if (isServiceReady)
// NEW: if (stateManager.isReady)

// In onDestroy()
stateManager.cleanup()
```

**Affected Methods:**
- `onCreate()` - Initialize
- `onServiceConnected()` - Set ready
- `initializeVoiceRecognition()` - Set voice ready
- `evaluateForegroundServiceNeed()` - Check/set foreground state
- `onStart()` - Set background state
- `onStop()` - Set background state
- `onDestroy()` - Cleanup

**Success Criteria:**
- [ ] All state fields removed from VoiceOSService
- [ ] All state updates go through StateManager
- [ ] All state reads use StateManager
- [ ] Service initializes correctly
- [ ] State transitions work
- [ ] No compilation errors

**Estimated Changes:** ~25 locations

---

### Phase 2: DatabaseManager Integration (MEDIUM RISK - 3 hours)

**SOLID Component:**
- **File:** `DatabaseManagerImpl.kt` (1,252 LOC)
- **Interface:** `IDatabaseManager`
- **Tests:** 99 tests in `DatabaseManagerImplTest.kt`
- **Dependencies:** None (but Foundation depends on Phase 1)

**VoiceOSService Code to Replace:**

| Current Field/Method | Line(s) | Replace With | Component Method |
|---------------------|---------|--------------|------------------|
| `private var scrapingDatabase: AppScrapingDatabase? = null` | 192 | Delete field | Managed internally by DatabaseManager |
| `private suspend fun registerDatabaseCommands()` | 305-441 | Replace body | `databaseManager.getVoiceCommands()` |
| Direct database DAO calls | Multiple | Replace with | `databaseManager.saveScrapedElements()` |
| `fun onNewCommandsGenerated()` | 442-447 | Replace body | `databaseManager.notifyCommandsUpdated()` |

**New Code to Add:**
```kotlin
// Field declaration
@Inject
lateinit var databaseManager: IDatabaseManager

// In onCreate() or initialization
lifecycleScope.launch {
    databaseManager.initialize()
}

// Replace registerDatabaseCommands()
private suspend fun registerDatabaseCommands() {
    val commands = databaseManager.getVoiceCommands()
    commandCache.clear()
    commandCache.addAll(commands.map { it.commandText })
    speechManager.updateVocabulary(commandCache.toSet())
}

// Replace direct database calls
// OLD: scrapingDatabase?.scrapedElementDao?.insert(element)
// NEW: databaseManager.saveScrapedElements(listOf(element))

// In onDestroy()
databaseManager.cleanup()
```

**Affected Methods:**
- `onCreate()` - Initialize database
- `registerDatabaseCommands()` - Complete rewrite
- `onNewCommandsGenerated()` - Update vocabulary
- Any direct DB access - Route through manager
- `onDestroy()` - Cleanup

**Success Criteria:**
- [ ] scrapingDatabase field removed
- [ ] All database operations through DatabaseManager
- [ ] Command registration works
- [ ] No data loss
- [ ] Cache works correctly
- [ ] No compilation errors

**Estimated Changes:** ~30 locations

---

### Phase 3: SpeechManager Integration (MEDIUM RISK - 3 hours)

**SOLID Component:**
- **File:** `SpeechManagerImpl.kt` (856 LOC)
- **Interface:** `ISpeechManager`
- **Tests:** 72 tests in `SpeechManagerImplTest.kt`
- **Dependencies:** StateManager (Phase 1)

**VoiceOSService Code to Replace:**

| Current Field/Method | Line(s) | Replace With | Component Method |
|---------------------|---------|--------------|------------------|
| `lateinit var speechEngineManager: SpeechEngineManager` | 156 | Delete field | Managed by SpeechManager |
| `private fun initializeVoiceRecognition()` | 731-758 | Replace body | `speechManager.initialize()` |
| `private fun registerVoiceCmd()` | 695-724 | Replace body | `speechManager.updateVocabulary()` |
| Direct speech engine calls | Multiple | Replace with | `speechManager.startListening()` |
| Speech result handling | Multiple | Replace with | `speechManager.recognitionResults.collect {}` |

**New Code to Add:**
```kotlin
// Field declaration
@Inject
lateinit var speechManager: ISpeechManager

// Remove speechEngineManager field

// In initialization
lifecycleScope.launch {
    speechManager.initialize()

    // Collect recognition results
    speechManager.recognitionResults.collect { result ->
        when (result) {
            is RecognitionResult.Partial -> handlePartialResult(result.text)
            is RecognitionResult.Final -> handleVoiceCommand(result.text, result.confidence)
            is RecognitionResult.Error -> Log.e(TAG, "Recognition error: ${result.message}")
        }
    }
}

// Replace initializeVoiceRecognition()
private fun initializeVoiceRecognition() {
    lifecycleScope.launch {
        speechManager.initialize()
        stateManager.updateVoiceReady(speechManager.isReady)
    }
}

// Replace registerVoiceCmd()
private fun registerVoiceCmd() {
    lifecycleScope.launch {
        val allCommands = commandCache + staticCommandCache + allRegisteredCommands
        speechManager.updateVocabulary(allCommands.toSet())
    }
}

// In onDestroy()
speechManager.cleanup()
```

**Affected Methods:**
- `initializeVoiceRecognition()` - Complete rewrite
- `registerVoiceCmd()` - Complete rewrite
- `handleVoiceCommand()` - Receives from speech manager
- Any speech engine access - Route through manager
- `onDestroy()` - Cleanup

**Success Criteria:**
- [ ] speechEngineManager field removed
- [ ] Speech recognition works
- [ ] Vocabulary updates work
- [ ] Engine switching works
- [ ] Recognition results received
- [ ] No compilation errors

**Estimated Changes:** ~35 locations

---

### Phase 4: UIScrapingService Integration (MEDIUM-HIGH RISK - 3 hours)

**SOLID Component:**
- **File:** `UIScrapingServiceImpl.kt` (456 LOC + helpers)
- **Interface:** `IUIScrapingService`
- **Tests:** 75 tests in `UIScrapingServiceImplTest.kt`
- **Dependencies:** DatabaseManager (Phase 2)

**VoiceOSService Code to Replace:**

| Current Field/Method | Line(s) | Replace With | Component Method |
|---------------------|---------|--------------|------------------|
| `private val uiScrapingEngine by lazy { ... }` | 162-166 | Delete lazy init | Managed by UIScrapingService |
| `private val nodeCache: MutableList<UIElement>` | 144 | Delete field | Managed internally |
| `scrapingIntegration?.onAccessibilityEvent()` | ~583 | Replace with | `uiScrapingService.extractUIElements()` |
| Direct cache access | Multiple | Replace with | `uiScrapingService.getCachedElements()` |

**New Code to Add:**
```kotlin
// Field declaration
@Inject
lateinit var uiScrapingService: IUIScrapingService

// Remove uiScrapingEngine and nodeCache fields

// In initialization
lifecycleScope.launch {
    uiScrapingService.initialize()
}

// In onAccessibilityEvent()
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (!isServiceReady || event == null) return

    lifecycleScope.launch {
        // Forward to UI scraping service
        uiScrapingService.extractUIElements(event)

        // Existing integrations (LearnApp, etc.) remain unchanged
        learnAppIntegration?.onAccessibilityEvent(event)
    }
}

// Replace cache access
// OLD: nodeCache.find { it.text == query }
// NEW: uiScrapingService.findElement(query)

// In onDestroy()
uiScrapingService.cleanup()
```

**Affected Methods:**
- `onAccessibilityEvent()` - Major rewrite
- Any cache access - Route through service
- Command generation - Use service methods
- `onDestroy()` - Cleanup

**Success Criteria:**
- [ ] uiScrapingEngine removed
- [ ] nodeCache removed
- [ ] UI scraping works
- [ ] Cache works correctly
- [ ] Elements persisted
- [ ] No compilation errors

**Estimated Changes:** ~25 locations

---

### Phase 5: EventRouter Integration (HIGH RISK - 4 hours)

**SOLID Component:**
- **File:** `EventRouterImpl.kt` (823 LOC)
- **Interface:** `IEventRouter`
- **Tests:** 19 tests in `EventRouterImplTest.kt`
- **Dependencies:** StateManager (Phase 1), UIScrapingService (Phase 4)

**VoiceOSService Code to Replace:**

| Current Field/Method | Line(s) | Replace With | Component Method |
|---------------------|---------|--------------|------------------|
| `onAccessibilityEvent()` main body | 562-693 | Delegate to | `eventRouter.routeEvent(event)` |
| `private val eventDebouncer = Debouncer()` | 208 | Delete field | Managed by EventRouter |
| `private val eventCounts = ArrayMap<Int, AtomicLong>()` | 169-176 | Delete field | Managed by EventRouter |
| `private fun isRedundantWindowChange()` | 726-729 | Delete method | EventRouter handles filtering |
| Package filtering logic | Multiple | Delete | EventRouter handles |

**New Code to Add:**
```kotlin
// Field declaration
@Inject
lateinit var eventRouter: IEventRouter

// Remove eventDebouncer, eventCounts fields

// In initialization
lifecycleScope.launch {
    eventRouter.initialize()

    // Set up event handlers
    eventRouter.addEventHandler(EventRouterImpl.EventType.VIEW_CLICKED) { event ->
        // Handle click events
        uiScrapingService.extractUIElements(event)
    }

    eventRouter.addEventHandler(EventRouterImpl.EventType.WINDOW_STATE_CHANGED) { event ->
        // Handle window change events
        handleWindowStateChange(event)
    }

    // Configure debouncing
    eventRouter.setDebounceInterval(1000L)

    // Configure package filters
    eventRouter.addPackageFilter("com.realwear.*")
    eventRouter.addPackageFilter("com.android.systemui")
}

// MAJOR REWRITE - onAccessibilityEvent()
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return

    // Route through EventRouter - it handles everything:
    // - Debouncing
    // - Filtering
    // - Priority queuing
    // - Metrics
    eventRouter.routeEvent(event)
}

// Remove isRedundantWindowChange() - EventRouter handles this

// In onDestroy()
eventRouter.cleanup()
```

**Affected Methods:**
- `onAccessibilityEvent()` - **COMPLETE REWRITE** (most critical change)
- `isRedundantWindowChange()` - DELETE
- Any event filtering logic - DELETE
- Any event metrics - DELETE
- `onDestroy()` - Cleanup

**Success Criteria:**
- [ ] onAccessibilityEvent() delegated to EventRouter
- [ ] Event filtering works
- [ ] Debouncing works
- [ ] Event metrics tracked
- [ ] No event loss
- [ ] Performance acceptable
- [ ] No compilation errors

**Estimated Changes:** ~60 locations (MAJOR)

---

### Phase 6: CommandOrchestrator Integration (HIGH RISK - 4 hours)

**SOLID Component:**
- **File:** `CommandOrchestratorImpl.kt` (745 LOC)
- **Interface:** `ICommandOrchestrator`
- **Tests:** 78 tests in `CommandOrchestratorImplTest.kt`
- **Dependencies:** StateManager (Phase 1), SpeechManager (Phase 3)

**VoiceOSService Code to Replace:**

| Current Field/Method | Line(s) | Replace With | Component Method |
|---------------------|---------|--------------|------------------|
| `private fun handleVoiceCommand()` | 973-1073 | Replace body | `commandOrchestrator.executeCommand()` |
| `private fun handleRegularCommand()` | 1075-1142 | DELETE | Orchestrator handles |
| `private fun executeCommand()` | ~800 | Replace body | `commandOrchestrator.executeCommand()` |
| `private var fallbackModeEnabled = false` | 213 | Delete field | Orchestrator manages |
| `private fun enableFallbackMode()` | ~1050 | Delete method | `commandOrchestrator.enableFallbackMode()` |
| `companion fun executeCommand()` | 104-123 | Delegate to | Orchestrator for global actions |
| 3-tier execution logic | 973-1142 | DELETE | Orchestrator handles all tiers |

**New Code to Add:**
```kotlin
// Field declaration
@Inject
lateinit var commandOrchestrator: ICommandOrchestrator

// Remove fallbackModeEnabled field

// In initialization
lifecycleScope.launch {
    commandOrchestrator.initialize(
        accessibilityService = this@VoiceOSService,
        commandManager = commandManagerInstance,
        voiceCommandProcessor = voiceCommandProcessor,
        actionCoordinator = actionCoordinator
    )

    // Register commands
    commandOrchestrator.registerCommands(allRegisteredCommands.toSet())

    // Collect execution results
    commandOrchestrator.executionResults.collect { result ->
        when (result) {
            is ExecutionResult.Success -> Log.i(TAG, "Command executed: ${result.command}")
            is ExecutionResult.Failure -> Log.e(TAG, "Command failed: ${result.reason}")
            is ExecutionResult.NotFound -> Log.w(TAG, "Command not found: ${result.command}")
        }
    }
}

// MAJOR REWRITE - handleVoiceCommand()
private fun handleVoiceCommand(command: String, confidence: Float) {
    lifecycleScope.launch {
        val context = CommandContext(
            source = CommandSource.VOICE,
            timestamp = System.currentTimeMillis(),
            packageName = currentPackageName,
            activityName = currentActivityName
        )

        commandOrchestrator.executeCommand(
            command = command,
            confidence = confidence,
            context = context
        )
    }
}

// DELETE handleRegularCommand() - Orchestrator handles

// Simplify companion executeCommand()
companion object {
    fun executeCommand(commandText: String): Boolean {
        val service = instanceRef?.get() ?: return false
        service.lifecycleScope.launch {
            service.commandOrchestrator.executeCommand(
                command = commandText,
                confidence = 1.0f,
                context = CommandContext(source = CommandSource.STATIC)
            )
        }
        return true
    }
}

// In onDestroy()
commandOrchestrator.cleanup()
```

**Affected Methods:**
- `handleVoiceCommand()` - **COMPLETE REWRITE**
- `handleRegularCommand()` - **DELETE** (170 lines)
- `executeCommand()` - **SIMPLIFY**
- `enableFallbackMode()` - **DELETE**
- Companion `executeCommand()` - **SIMPLIFY**
- `onDestroy()` - Cleanup

**Success Criteria:**
- [ ] 3-tier execution works
- [ ] Tier fallback works
- [ ] Commands execute correctly
- [ ] Confidence thresholds respected
- [ ] Fallback mode works
- [ ] Global actions work
- [ ] No compilation errors

**Estimated Changes:** ~80 locations (MAJOR)

---

### Phase 7: ServiceMonitor Integration (LOW RISK - 2 hours)

**SOLID Component:**
- **File:** `ServiceMonitorImpl.kt` (927 LOC)
- **Interface:** `IServiceMonitor`
- **Tests:** 83 tests in `ServiceMonitorImplTest.kt`
- **Dependencies:** All components (observes them)

**VoiceOSService Code to Replace:**

| Current Field/Method | Line(s) | Replace With | Component Method |
|---------------------|---------|--------------|------------------|
| `private var serviceMonitor: ServiceMonitor? = null` | 212 | Change type | `IServiceMonitor` |
| Health check logic | Multiple | Replace with | `serviceMonitor.checkComponent()` |
| Metrics collection | Multiple | Replace with | `serviceMonitor.getCurrentMetrics()` |

**New Code to Add:**
```kotlin
// Field declaration (replace existing)
@Inject
lateinit var serviceMonitor: IServiceMonitor

// In initialization (AFTER all other components)
lifecycleScope.launch {
    serviceMonitor.initialize()

    // Register all components for monitoring
    serviceMonitor.registerComponent("StateManager", stateManager)
    serviceMonitor.registerComponent("DatabaseManager", databaseManager)
    serviceMonitor.registerComponent("SpeechManager", speechManager)
    serviceMonitor.registerComponent("UIScrapingService", uiScrapingService)
    serviceMonitor.registerComponent("EventRouter", eventRouter)
    serviceMonitor.registerComponent("CommandOrchestrator", commandOrchestrator)

    serviceMonitor.startMonitoring()

    // Collect alerts
    serviceMonitor.alerts.collect { alert ->
        when (alert.severity) {
            AlertSeverity.CRITICAL -> handleCriticalAlert(alert)
            AlertSeverity.WARNING -> Log.w(TAG, "Monitor alert: ${alert.message}")
            AlertSeverity.INFO -> Log.i(TAG, "Monitor info: ${alert.message}")
        }
    }
}

// Use for health checks
private fun checkServiceHealth() {
    lifecycleScope.launch {
        val health = serviceMonitor.getHealthReport()
        if (health.overall != ComponentHealth.HEALTHY) {
            Log.w(TAG, "Service health degraded: ${health.issues}")
        }
    }
}

// In onDestroy()
serviceMonitor.stopMonitoring()
serviceMonitor.cleanup()
```

**Affected Methods:**
- Initialization - Register components
- Any health check code - Use monitor
- Any metrics code - Use monitor
- `onDestroy()` - Cleanup

**Success Criteria:**
- [ ] All components monitored
- [ ] Health checks work
- [ ] Alerts fire correctly
- [ ] Metrics collected
- [ ] Recovery works
- [ ] No compilation errors

**Estimated Changes:** ~20 locations

---

## Integration Sequence & Dependencies

```
START
  ↓
Phase 1: StateManager (2 hrs)
  └─ No dependencies
     └─ Test: State transitions work
        ↓
Phase 2: DatabaseManager (3 hrs)  [Can run parallel with Phase 1]
  └─ No dependencies
     └─ Test: Database operations work
        ↓
Phase 3: SpeechManager (3 hrs)
  └─ Depends: StateManager (Phase 1)
     └─ Test: Speech recognition works
        ↓
Phase 4: UIScrapingService (3 hrs)
  └─ Depends: DatabaseManager (Phase 2)
     └─ Test: UI scraping works
        ↓
Phase 5: EventRouter (4 hrs)
  └─ Depends: StateManager (Phase 1), UIScrapingService (Phase 4)
     └─ Test: Event routing works
        ↓
Phase 6: CommandOrchestrator (4 hrs)
  └─ Depends: StateManager (Phase 1), SpeechManager (Phase 3)
     └─ Test: Command execution works
        ↓
Phase 7: ServiceMonitor (2 hrs)
  └─ Depends: ALL previous phases
     └─ Test: Monitoring works
        ↓
COMPLETE
```

**Total Time:** 21 hours (with compilation/testing)

**Potential Parallelization:**
- Phase 1 + 2 can run in parallel (save 2 hours)
- Phase 3 + 4 can start in parallel after Phase 1 + 2
- **Optimized Total:** 17 hours

---

## Risk Assessment Per Phase

| Phase | Component | Risk | Impact if Failed | Rollback Time |
|-------|-----------|------|------------------|---------------|
| 1 | StateManager | LOW | Service state issues | 15 min |
| 2 | DatabaseManager | MEDIUM | Database access fails | 30 min |
| 3 | SpeechManager | MEDIUM | Speech recognition fails | 30 min |
| 4 | UIScrapingService | MEDIUM-HIGH | UI scraping fails | 45 min |
| 5 | EventRouter | HIGH | Events not processed | 60 min |
| 6 | CommandOrchestrator | HIGH | Commands don't execute | 60 min |
| 7 | ServiceMonitor | LOW | Monitoring unavailable | 15 min |

---

## Testing Strategy Per Phase

### After Each Phase:
1. **Compile:** `./gradlew :modules:apps:VoiceOSCore:compileDebugKotlin`
2. **Run Tests:** Check component's test file passes
3. **Build APK:** `./gradlew :modules:apps:VoiceOSCore:assembleDebug`
4. **Manual Test:** Install on device, test specific functionality
5. **Check Logs:** Verify no errors in logcat
6. **Commit:** Stage, commit with detailed message

### Integration Points to Test:
- Component initialization
- Method calls work correctly
- State updates flow through
- No null pointer exceptions
- Performance acceptable
- Existing integrations still work (LearnApp, VoiceCursor, etc.)

---

## Rollback Plan Per Phase

**If Phase Fails:**
1. **Identify:** Note specific error/crash
2. **Comment Out:** Comment new code, uncomment old code
3. **Test:** Verify service works with old code
4. **Fix:** Debug issue in isolation
5. **Retry:** Re-integrate with fix

**Quick Rollback Commands:**
```bash
# Before each phase, create checkpoint
git add .
git commit -m "checkpoint: before Phase X"

# If phase fails
git reset --hard HEAD~1  # Rollback last commit
git clean -fd            # Clean untracked files
```

---

## Success Criteria - Overall Integration

**Code Quality:**
- [ ] VoiceOSService reduced from 1,385 → ~400 LOC
- [ ] All inline logic moved to SOLID components
- [ ] No code duplication
- [ ] Clean method signatures
- [ ] Proper error handling

**Functional:**
- [ ] All existing features work
- [ ] No new bugs introduced
- [ ] Backward compatibility maintained
- [ ] LearnApp integration works
- [ ] VoiceCursor integration works
- [ ] CommandManager integration works

**Performance:**
- [ ] No performance degradation
- [ ] Memory within targets (<25MB)
- [ ] CPU within targets (<5% idle)
- [ ] Battery acceptable (<0.1% extra)

**Testing:**
- [ ] All 496 tests pass (when test framework fixed)
- [ ] Manual testing successful
- [ ] No crashes
- [ ] No ANRs (Application Not Responding)

---

## Documentation Requirements

**Per Phase:**
- [ ] Update VoiceOSService inline comments
- [ ] Document integration changes
- [ ] Update commit messages
- [ ] Log progress in status doc

**Overall:**
- [ ] Update VoiceOSCore module documentation
- [ ] Update architecture diagrams
- [ ] Update CHANGELOG
- [ ] Create integration completion report
- [ ] Update PROJECT-STATUS-CURRENT.md

---

## Estimated LOC Changes

| Phase | Component | Lines Added | Lines Deleted | Net Change |
|-------|-----------|-------------|---------------|------------|
| 1 | StateManager | 30 | 50 | -20 |
| 2 | DatabaseManager | 40 | 80 | -40 |
| 3 | SpeechManager | 50 | 90 | -40 |
| 4 | UIScrapingService | 35 | 70 | -35 |
| 5 | EventRouter | 60 | 180 | -120 |
| 6 | CommandOrchestrator | 80 | 250 | -170 |
| 7 | ServiceMonitor | 30 | 40 | -10 |
| **Total** | | **325** | **760** | **-435** |

**Result:** VoiceOSService goes from 1,385 LOC → ~950 LOC (after Phase 7)

**Further cleanup possible:** Remove temporary code, consolidate initialization → Target ~400-500 LOC

---

**Document End**

**Created:** 2025-10-16 23:39:52 PDT
**Status:** Ready for Integration
**Next Step:** Create phase-by-phase TODO list and update master TODO
