# Implementation Plan: LearnApp Critical Fixes

**Branch**: `004-learnapp-fixes` | **Date**: 2025-10-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-learnapp-fixes/spec.md`

## Summary

Fix 6 critical issues in LearnApp module that prevent proper app learning: (1) navigation_edges and screen_states database tables not being populated during DFS exploration, (2) consent dialog flickering due to rapid accessibility events, (3) login prompt overlay exists but not wired into LearnAppIntegration, (4) apps generating continuous accessibility events (DeviceInfo) causing UI freezes and duplicate consent dialogs, (5) package version info TODOs not retrieving from PackageManager, (6) error notification TODOs not implemented.

**Technical Approach**: Fix ExplorationEngine to call ScreenStateManager/NavigationGraphBuilder persistence methods, implement event debouncing in AppLaunchDetector/ConsentDialogManager, wire LoginPromptOverlay into LearnAppIntegration.handleExplorationStateChange(), retrieve PackageManager info in metadataProvider calls, implement Toast-based error notifications.

## Technical Context

**Language/Version**: Kotlin 1.9+, Android SDK 29+ (API 29+)
**Primary Dependencies**:
- Android AccessibilityService framework
- Room database with KSP (already configured)
- Kotlin Coroutines + Flow (Dispatchers.Main, Dispatchers.Default)
- UUIDCreator library (third-party UUID generation)

**Storage**: Room database (LearnAppDatabase) with 4 tables: exploration_sessions, learned_apps, navigation_edges, screen_states
**Testing**: JUnit 4, Mockito, Robolectric (Android unit tests), instrumented tests for AccessibilityService
**Target Platform**: Android 10+ (API 29+), AccessibilityService context (no Activity)
**Project Type**: Mobile (Android module within VOS4 multi-module project)
**Performance Goals**:
- Event processing <16ms (maintain 60fps UI)
- Database writes <100ms per transaction
- Exploration completes in <5 minutes for standard 10-20 screen apps
- Zero UI freezes during continuous event flood (10+ events/sec)

**Constraints**:
- Must work within AccessibilityService limitations (no Activity context)
- Cannot use Dialog class (requires Activity) - must use WindowManager overlays
- Database writes must be atomic (Room @Transaction)
- All UI operations must use WidgetOverlayHelper.ensureMainThread() to prevent race conditions
- Event throttling must not miss legitimate app launches (balance responsiveness vs stability)

**Scale/Scope**:
- 20+ LearnApp classes across 8 packages
- ~5000 LOC in LearnApp module
- Affects 6 core classes: ExplorationEngine, AppLaunchDetector, ConsentDialogManager, LearnAppIntegration, ScreenStateManager, NavigationGraphBuilder

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Performance-First Architecture: ✅ PASS

- Event debouncing will reduce processing overhead (currently unbounded)
- Database writes will use Room transactions (already atomic)
- UI operations already use WidgetOverlayHelper.ensureMainThread() (immediate execution on main thread)
- No additional performance-degrading abstractions introduced
- Event throttling improves responsiveness by preventing queue overload

**Action**: Continue - performance improvements expected

### II. Direct Implementation (No Interfaces): ✅ PASS

- All fixes use concrete classes directly
- No new interfaces required
- ExplorationEngine → ScreenStateManager → Room DAO (direct calls)
- AppLaunchDetector → ConsentDialogManager (direct calls with Flow)
- LoginPromptOverlay already exists as concrete class (just needs wiring)

**Action**: Continue - no abstraction violations

### III. Privacy & Accessibility First: ✅ PASS

- All learning happens on-device (no cloud services)
- Consent dialog fixes improve user control (stable, non-flickering)
- Login prompt gives users clear options (Skip/Continue/Stop)
- Error notifications inform users of failures (transparency)
- No new privacy risks introduced

**Action**: Continue - aligns with privacy-first principles

### IV. Modular Independence: ✅ PASS

- All changes contained within LearnApp module
- No new cross-module dependencies
- LearnAppIntegration remains the single integration point with VoiceOSCore
- Database schema already defined (just implementing missing writes)
- Uses com.augmentalis.learnapp namespace (compliant)

**Action**: Continue - module boundaries respected

### V. Quality Through Enforcement: ✅ PASS

- Test-first approach for database writes (verify navigation_edges/screen_states populated)
- Unit tests for event debouncing (verify throttle behavior)
- Integration tests for login prompt wiring (verify pause/resume flow)
- Instrumented tests for consent dialog stability (verify no flickers)
- All quality gates will be enforced before merge

**Action**: Continue - testing strategy defined

**GATE RESULT**: ✅ ALL GATES PASS - Proceed to Phase 0

## Project Structure

### Documentation (this feature)

```text
specs/004-learnapp-fixes/
├── spec.md              # Feature specification (COMPLETE)
├── plan.md              # This file (IN PROGRESS)
├── research.md          # Phase 0: Technical research (TO BE CREATED)
├── data-model.md        # Phase 1: Database schema verification (TO BE CREATED)
├── quickstart.md        # Phase 1: Testing guide (TO BE CREATED)
├── contracts/           # Phase 1: API contracts (NOT APPLICABLE - no new APIs)
├── checklists/
│   └── requirements.md  # Specification validation (COMPLETE)
└── tasks.md             # Phase 2: Task breakdown (TO BE CREATED by /idea.tasks)
```

### Source Code (LearnApp module)

```text
modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/
├── database/
│   ├── LearnAppDatabase.kt                    # Room database (EXISTS - no changes)
│   ├── dao/
│   │   └── LearnAppDao.kt                     # DAO with insert methods (EXISTS - verify methods exist)
│   ├── entities/
│   │   ├── NavigationEdgeEntity.kt            # EXISTS - schema defined
│   │   ├── ScreenStateEntity.kt               # EXISTS - schema defined
│   │   ├── LearnedAppEntity.kt                # EXISTS - update for version fields
│   │   └── ExplorationSessionEntity.kt        # EXISTS - no changes
│   └── repository/
│       ├── LearnAppRepository.kt              # EXISTS - add version retrieval
│       └── AppMetadataProvider.kt             # EXISTS - FIX: get version from PackageManager
│
├── exploration/
│   ├── ExplorationEngine.kt                   # FIX: call persistence methods during DFS
│   ├── ScreenExplorer.kt                      # EXISTS - used by engine
│   └── DFSExplorationStrategy.kt              # EXISTS - no changes
│
├── fingerprinting/
│   ├── ScreenStateManager.kt                  # FIX: ensure persistence methods called
│   └── ScreenFingerprinter.kt                 # EXISTS - generates fingerprints
│
├── navigation/
│   ├── NavigationGraphBuilder.kt              # FIX: ensure navigation edges persisted
│   └── NavigationGraph.kt                     # EXISTS - in-memory graph
│
├── detection/
│   ├── AppLaunchDetector.kt                   # FIX: add event debouncing/throttling
│   └── LearnedAppTracker.kt                   # EXISTS - no changes
│
├── ui/
│   ├── ConsentDialogManager.kt                # FIX: add event throttling, prevent re-showing
│   ├── ConsentDialog.kt                       # EXISTS - fixed in previous commit (WidgetOverlayHelper)
│   ├── ProgressOverlayManager.kt              # EXISTS - working correctly
│   └── widgets/
│       └── WidgetOverlayHelper.kt             # EXISTS - thread-safe overlay pattern
│
├── overlays/
│   └── LoginPromptOverlay.kt                  # EXISTS - FIX: wire into integration
│
└── integration/
    └── LearnAppIntegration.kt                 # FIX: wire login prompt, add error notifications, retrieve versions
```

**Structure Decision**: Using existing LearnApp module structure (mobile Android app module). All fixes are modifications to existing classes - no new major components needed. Database schema already defined, just implementing missing persistence calls.

## Complexity Tracking

**No Constitution Violations** - this section is not needed. All changes align with VOS4 principles:
- Direct implementation (no new interfaces)
- Performance-focused (event throttling reduces overhead)
- Module-contained (all changes in LearnApp)
- Quality-enforced (comprehensive testing required)

## Phase 0: Research & Investigation

### Investigation Tasks

**1. Database Write Investigation** (Priority: P1)
- **Question**: Why are navigation_edges and screen_states tables empty after exploration?
- **Hypothesis**: ExplorationEngine performs DFS traversal but doesn't call persistence methods
- **Research Tasks**:
  - Read ExplorationEngine.kt: Find DFS loop, check if ScreenStateManager.saveScreenState() called
  - Read ScreenStateManager.kt: Verify saveScreenState() method exists and calls LearnAppDao
  - Read NavigationGraphBuilder.kt: Verify addEdge() method calls LearnAppDao.insertNavigationEdge()
  - Read LearnAppDao.kt: Verify @Insert methods exist for NavigationEdgeEntity and ScreenStateEntity
- **Expected Outcome**: Identify exact location(s) where persistence calls are missing

**2. Event Throttling Investigation** (Priority: P1)
- **Question**: How to throttle accessibility events without missing legitimate app launches?
- **Hypothesis**: AppLaunchDetector processes every TYPE_WINDOW_STATE_CHANGED event immediately
- **Research Tasks**:
  - Read AppLaunchDetector.onAccessibilityEvent(): Check for any throttling/debouncing
  - Research Kotlin Flow throttling operators (debounce, sample, conflate)
  - Determine appropriate throttle window (suggestion: 500ms)
  - Consider impact on legitimate rapid app switches
- **Expected Outcome**: Throttling strategy (debounce vs sample vs hybrid)

**3. Consent Dialog Flickering Investigation** (Priority: P1)
- **Question**: Why does consent dialog flicker or reappear?
- **Hypothesis**: Multiple app launch events trigger multiple consent dialogs in rapid succession
- **Research Tasks**:
  - Read ConsentDialogManager.showConsentDialog(): Check if already-showing guard exists
  - Read AppLaunchDetector: Check if duplicate event filtering exists
  - Test with DeviceInfo app (known to generate continuous events)
  - Determine if flickering is caused by rapid show/hide or multiple instances
- **Expected Outcome**: Root cause (duplicate events vs rapid state changes)

**4. Login Prompt Wiring Investigation** (Priority: P2)
- **Question**: How to wire LoginPromptOverlay into exploration flow?
- **Hypothesis**: LoginPromptOverlay class exists but LearnAppIntegration.handleExplorationStateChange() doesn't instantiate it
- **Research Tasks**:
  - Read LoginPromptOverlay.kt: Verify constructor signature and show() method
  - Read LearnAppIntegration.kt line 268: Verify showLoginPromptOverlay() method exists but is not wired
  - Read ExplorationState.PausedForLogin: Verify this state is emitted by ExplorationEngine
  - Check if LoginScreenDetector properly detects login screens
- **Expected Outcome**: Wiring instructions (just need to call existing method)

**5. PackageManager Version Retrieval** (Priority: P3)
- **Question**: How to safely retrieve app version from PackageManager?
- **Hypothesis**: AppMetadataProvider has TODO comments but logic is straightforward
- **Research Tasks**:
  - Read AppMetadataProvider.getMetadata(): Find TODO comments for versionCode/versionName
  - Research PackageManager.getPackageInfo() API (flags, nullability)
  - Handle deprecated versionCode field (use getLongVersionCode() on API 28+)
  - Determine fallback values if PackageManager returns null
- **Expected Outcome**: Code snippet for version retrieval with API level handling

**6. Error Notification Implementation** (Priority: P2)
- **Question**: What type of notification for errors (Toast vs Notification vs Overlay)?
- **Hypothesis**: Toast is simplest and already used in LearnAppIntegration
- **Research Tasks**:
  - Read LearnAppIntegration.showErrorNotification(): Already uses Toast (line 406)
  - Verify Toast works from AccessibilityService context
  - Check if error handling TODO comments are in handleExplorationStateChange()
  - Determine if Toast.LENGTH_LONG is sufficient (3.5 seconds)
- **Expected Outcome**: Confirm Toast approach, identify TODO comment locations

### Research Output Format

Results will be documented in `research.md` with this structure:

```markdown
# Research: LearnApp Critical Fixes

## Decision 1: Database Persistence Strategy
**Decision**: Call ScreenStateManager.saveScreenState() and NavigationGraphBuilder.addEdge() from ExplorationEngine during DFS traversal
**Rationale**: [findings from investigation]
**Alternatives considered**: [other approaches evaluated]
**Implementation**: [specific code locations and method calls]

## Decision 2: Event Throttling Strategy
**Decision**: Use Flow.debounce(500ms) on appLaunchEvents in LearnAppIntegration
**Rationale**: [findings from investigation]
**Alternatives considered**: [sample vs debounce comparison]
**Implementation**: [specific code changes]

[... continue for all 6 investigations]
```

**Phase 0 Gate**: All 6 investigations complete, all NEEDS CLARIFICATION resolved → Proceed to Phase 1

## Phase 1: Design & Contracts

### Data Model Verification

Since database schema already exists, Phase 1 will verify schema correctness:

**Verify in `data-model.md`**:
- NavigationEdgeEntity fields match NavigationEdge model (source/dest screen IDs, action type, element ID)
- ScreenStateEntity fields match ScreenState model (screen fingerprint, UI hierarchy JSON, element list)
- LearnedAppEntity has versionCode/versionName fields (add if missing)
- Foreign key relationships correct (NavigationEdge → ScreenState, ExplorationSession → LearnedApp)

**Output**: `data-model.md` with verification checklist

### API Contracts

**NOT APPLICABLE** - No new public APIs being added. All changes are internal to LearnApp module:
- Database persistence methods are private to ExplorationEngine
- Event throttling is internal to AppLaunchDetector
- Login prompt wiring is internal to LearnAppIntegration
- Version retrieval is internal to AppMetadataProvider

No `/contracts/` directory needed.

### Quick Start Guide

Create `quickstart.md` for testing the fixes:

```markdown
# Quick Start: Testing LearnApp Fixes

## Test 1: Database Population
1. Clear LearnApp database: `adb shell pm clear com.augmentalis.voiceos`
2. Launch VoiceOS, approve consent for Calculator app
3. Wait for exploration to complete (progress overlay disappears)
4. Query database: `adb shell "run-as com.augmentalis.voiceos sqlite3 databases/learnapp_database 'SELECT COUNT(*) FROM navigation_edges'"`
5. **Expected**: Count > 0 (navigation edges populated)

## Test 2: Consent Dialog Stability
1. Install DeviceInfo app (generates continuous events)
2. Launch DeviceInfo
3. Observe consent dialog appearance
4. **Expected**: Dialog appears once, remains stable, no flickering

## Test 3: Login Prompt
1. Launch app with login screen (e.g., banking app)
2. Approve consent for learning
3. Wait for login screen detection
4. **Expected**: Login prompt overlay appears with Skip/Continue/Stop options

## Test 4: Version Tracking
1. Complete learning for any app
2. Query learned_apps table: `SELECT versionCode, versionName FROM learned_apps`
3. Compare with `adb shell dumpsys package [packageName] | grep versionCode`
4. **Expected**: Versions match PackageManager data

## Test 5: Error Notifications
1. Trigger database error (e.g., corrupt database file)
2. Attempt to start learning
3. **Expected**: Toast notification appears with user-friendly error message
```

### Agent Context Update

After Phase 1 design, run:
```bash
cd /Volumes/M Drive/Coding/vos4
.ideacode/scripts/bash/update-agent-context.sh claude
```

This will update `.claude/agent-context-claude.md` with:
- Event throttling patterns (Flow.debounce)
- Room transaction patterns (@Transaction)
- AccessibilityService overlay patterns (WidgetOverlayHelper)

**Phase 1 Gate**: data-model.md verified, quickstart.md created, agent context updated → Ready for /idea.tasks

## Phase 2: Task Breakdown

**NOT CREATED by /idea.plan** - This phase is handled by the `/idea.tasks` command.

The `/idea.tasks` command will generate `tasks.md` with:
- Dependency-ordered tasks from research and design
- Assigned to specialized agents (@vos4-database-expert, @vos4-kotlin-expert, @vos4-android-expert)
- Includes IDE Loop phases (Implement → Defend → Evaluate) for each task

---

## Post-Planning Actions

After this plan is complete:

1. **Run `/idea.tasks`** to generate task breakdown
2. **Or deploy agents directly** for Tier 2 complexity:
   - `@vos4-database-expert` for database persistence fixes
   - `@vos4-kotlin-expert` for event throttling and coroutine fixes
   - `@vos4-android-expert` for consent dialog stability and login prompt wiring
   - `@vos4-test-specialist` for comprehensive test coverage (mandatory)

3. **Testing requirements** (enforced by @vos4-test-specialist):
   - Unit tests: Event debouncing logic, database writes, version retrieval
   - Integration tests: Full exploration flow with database verification
   - Instrumented tests: Consent dialog stability on real device
   - Performance tests: Continuous event handling (10+ events/sec)

---

**Plan Status**: Phase 0 ready for execution
**Next Command**: `/idea.tasks` OR deploy @vos4-orchestrator for Tier 2 implementation
