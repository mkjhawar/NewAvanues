# VoiceOSCore Deep Functional Analysis

**Date:** 2026-01-19 | **Version:** V1 | **Author:** Claude
**Status:** Complete Analysis | **Priority:** P0 - Critical

---

## Executive Summary

This document provides a comprehensive functional analysis of the VoiceOSCore consolidation task. We analyzed 567 total files across two locations to determine the optimal consolidation strategy.

### Key Metrics

| Metric | Value |
|--------|-------|
| **MASTER files** | 217 (KMP structure) |
| **LEGACY files** | 490 (Android-only) |
| **Conflict files** | 50 (exist in both) |
| **KMP-ready in LEGACY** | 141 (can move to commonMain) |
| **Android-specific** | 349 (must stay in androidMain) |

---

## Part 1: Location Analysis

### Location 1: MASTER (`Modules/VoiceOSCore`)

**Structure:** Full KMP
```
src/
├── commonMain/kotlin/com/augmentalis/voiceoscore/  (185 files)
├── androidMain/kotlin/com/augmentalis/voiceoscore/ (11 files)
├── iosMain/kotlin/com/augmentalis/voiceoscore/     (11 files)
└── desktopMain/kotlin/com/augmentalis/voiceoscore/ (11 files)
```

**Package:** `com.augmentalis.voiceoscore` (FLAT structure)

**Android imports:** 0 (fully platform-agnostic in commonMain)

**Key characteristics:**
- Clean KMP architecture
- Platform-specific implementations via expect/actual
- Factory patterns for platform bridging
- All handler interfaces defined in common

### Location 2: LEGACY (`Modules/VoiceOS/VoiceOSCore`)

**Structure:** Legacy Android
```
src/
├── main/java/com/augmentalis/voiceoscore/  (379 files)
├── test/                                    (61 files)
└── androidTest/                             (50 files)
```

**Package:** `com.augmentalis.voiceoscore` with 100+ sub-packages:
- `.accessibility.*` (27 packages)
- `.learnapp.*` (25 packages)
- `.scraping.*` (5 packages)
- `.commands.*` (3 packages)
- etc.

**Android imports:** 349 files have Android-specific imports

---

## Part 2: Conflict File Analysis (50 files)

### 2.1 MASTER Superior (30 files) - Keep MASTER

These files are larger and more complete in MASTER due to KMP abstractions:

| File | MASTER | LEGACY | Reason to Keep MASTER |
|------|--------|--------|----------------------|
| FrameworkDetector.kt | 790 | 271 | 3x larger, comprehensive detection |
| OverlayCoordinator.kt | 560 | 248 | 2x larger, full coordination |
| QuantizedCommand.kt | 200 | 35 | 6x larger, complete model |
| UIHandler.kt | 450 | 294 | More complete abstraction |
| LoginScreenDetector.kt | 449 | 273 | More detection patterns |
| OverlayThemes.kt | 427 | 294 | More theme support |
| SpeechEngineManager.kt | 433 | - | Clean orchestration |
| ConfidenceOverlay.kt | 413 | 297 | Better state model |
| OverlayConfig.kt | 400 | 356 | More configuration |
| DeviceHandler.kt | 396 | 224 | Platform abstraction |

**Full list of 30 files:**
ActionCategory, ActionCoordinator, AppFramework, AppHandler, BaseOverlay,
ConfidenceOverlay, ContextMenuOverlay, DeviceHandler, ElementProcessingResult,
ExplorationState, FrameworkDetector, HandlerRegistry, HashUtils, InputValidator,
ISpeechEngine, ISpeechEngineFactory, LoginScreenDetector, MetricsCollector,
NavigationHandler, NumberedSelectionOverlay, NumberOverlayStyle, OverlayConfig,
OverlayCoordinator, OverlayTheme, OverlayThemes, QuantizedCommand, QuantizedContext,
QuantizedNavigation, QuantizedScreen, UIHandler

### 2.2 LEGACY Has More Features (20 files) - Need Enhancement Merge

These files have significant additional functionality in LEGACY:

---

#### **SelectHandler.kt** (MASTER: 271 vs LEGACY: 836 = 3.1x)

**MASTER provides:**
- `SelectionAction` enum (6 values)
- `IClipboardProvider` interface
- Basic methods: selectAll, selectText, copy, cut, paste, clearSelection

**LEGACY adds:**
- `SelectionContext` data class with AccessibilityNodeInfo
- `enterSelectionMode()` / `exitSelectionMode()` with overlay feedback
- `performSelectAtCurrentPosition()` - tree traversal
- `findFocusedNode()` / `findEditableNode()` - node discovery
- 3 context menu types: basic, selection, general
- Coroutine scope management

**KMP convertible:** 0% (all Android AccessibilityService dependent)

**Recommendation:** Keep MASTER in commonMain, create AndroidSelectHandler in androidMain

---

#### **CursorHistoryTracker.kt** (MASTER: 141 vs LEGACY: 474 = 3.4x)

**MASTER provides:**
- Basic history with single list + index
- Methods: record, canUndo, canRedo, undo, redo, getCurrent, clear

**LEGACY adds:**
- `HistoricalCursorPosition` with optional description
- `HistoryStatistics` data class
- Dual stack architecture (historyStack + futureStack)
- Significant movement detection (>10px threshold)
- Time-based expiration (5 minutes default)
- Distance calculation: `distanceTo()`, `getTotalDistanceTraveled()`
- `jumpToIndex()`, `findPositionByDescription()`
- `getPositionsInTimeRange()`, `getRecentHistory()`
- `cleanupExpiredEntries()`
- Statistics: totalMoveCount, significantMoveCount

**KMP convertible:** 80% (math/logic is pure Kotlin, only android.util.Log is Android)

**Recommendation:** Extract movement detection, expiration, statistics to commonMain

---

#### **BoundaryDetector.kt** (MASTER: 143 vs LEGACY: 414 = 2.9x)

**MASTER provides:**
- `ScreenBounds` data class (6 properties)
- `Edge` enum (4 values)
- Basic methods: setBounds, isWithinBounds, clamp, getEdge

**LEGACY adds:**
- `BoundaryCheckResult` with detailed result info
- `SafeAreaInsets` for notch/cutout handling
- `BoundaryConfig` with configurable thresholds
- Dynamic display metrics from WindowManager
- Safe area from WindowInsets
- Multi-display support (displayId)
- Overscroll detection with threshold
- Distance-to-edge calculation
- 8-directional edge detection (includes diagonals)
- Screen center calculation
- Display metric updates on config change

**KMP convertible:** 30% (geometry is pure, display queries need Android)

**Recommendation:** Keep geometry in commonMain, add AndroidBoundaryDetector for display metrics

---

#### **HelpMenuHandler.kt** (MASTER: 197 vs LEGACY: 560 = 2.8x)

**MASTER provides:**
- `HelpCategory`, `HelpCommand`, `HelpResult` data classes
- Methods: handleCommand, showAllHelp, searchHelp, registerCategory
- 3 default categories: Navigation, Selection, Cursor

**LEGACY adds:**
- External documentation URLs (GitLab + GitHub fallback)
- Tutorial content with step-by-step instructions
- 6 predefined categories (vs 3)
- Help menu visualization with Material icons
- Category-specific help display
- Tutorial display with auto-hide
- Documentation opening with fallback strategy
- Toast feedback
- Auto-hide with delays (30s menu, 15s commands, 20s tutorial)

**KMP convertible:** 40% (data structures and search are pure)

**Recommendation:** Keep help data/search in commonMain, add AndroidHelpMenuHandler for UI

---

#### **SpeedController.kt** (MASTER: 124 vs LEGACY: 320 = 2.6x)

**MASTER provides:**
- `SpeedLevel` enum (4 values: SLOW, NORMAL, FAST, VERY_FAST)
- Simple multipliers: 0.5x, 1.0x, 2.0x, 4.0x
- Methods: getLevel, setLevel, getMultiplier, increaseSpeed, decreaseSpeed

**LEGACY adds:**
- `CursorSpeed` enum with pixelsPerSecond + accelerationTime
- `EasingFunction` enum (7 types: LINEAR, EASE_IN, EASE_OUT, etc.)
- Velocity-based calculations
- Acceleration curves
- Sine easing specialized function
- Precision mode (0.2x multiplier)
- Frame-rate independent movement
- Time-to-distance conversion
- Velocity capping (5000px/s max)
- Movement threshold for jitter prevention

**KMP convertible:** 95% (all math/physics is pure Kotlin)

**Recommendation:** Migrate easing functions and velocity math to commonMain

---

#### **NumberHandler.kt** (MASTER: 214 vs LEGACY: 530 = 2.5x)

**MASTER provides:**
- `NumberedElement` data class
- `NumberSelectionResult` data class
- Methods: assignNumbers, handleCommand, selectNumber, parseNumber
- Word number support: "one" through "ten"

**LEGACY adds:**
- `ElementInfo` data class with AccessibilityNodeInfo
- `ActionHandler` interface implementation
- Tree traversal: `findInteractiveElements()`
- Interactive element detection (clickable, scrollable, checkable)
- Smart element description
- Position-based sorting
- Overlay visualization
- Bounds filtering (<20px or off-screen)
- Action type detection
- Thread safety (ConcurrentHashMap + @Volatile)
- 30-second timeout

**KMP convertible:** 30% (parsing is pure, tree traversal needs Android)

**Recommendation:** Keep parsing in commonMain, add AndroidNumberHandler

---

#### **GestureHandler.kt** (MASTER: 194 vs LEGACY: 469 = 2.4x)

**MASTER provides:**
- `GestureType` enum (11 values)
- `GestureConfig` data class
- `GestureResult` data class
- Methods: handleCommand, createGesture, parseTapAt

**LEGACY adds:**
- `GesturePathFactory` interface
- Gesture queueing with LinkedList
- Multi-stroke gestures (pinch = 2 paths)
- Path-based gestures (moveTo/lineTo)
- Pinch-to-zoom with dual-finger
- Drag gesture with direction vectors
- Swipe in 4 directions
- Timing management (ViewConfiguration)
- Thread-safe dispatch (AtomicBoolean)
- Gesture callbacks (onCompleted, onCancelled)
- Advanced: performClickAt, performLongPressAt, performDoubleClickAt

**KMP convertible:** 25% (data structures pure, execution needs Android)

**Recommendation:** Keep data/parsing in commonMain, add AndroidGestureExecutor

---

#### **CommandGenerator.kt** (MASTER: 346 vs LEGACY: 729 = 2.1x)

**MASTER provides:**
- Object `CommandGenerator` (stateless)
- Methods: fromElement, fromElementWithPersistence, generateListIndexCommands

**LEGACY adds:**
- Class with state (context, queries)
- `generateStateAwareCommands()` - queries ElementStateHistory
- `generateCheckableCommands()` - checked state toggle
- `generateExpandableCommands()` - expand/collapse state
- `generateSelectableCommands()` - selection state
- `generateInteractionWeightedCommands()` - usage-based ranking

**KMP convertible:** 60% (checkable/expandable/selectable logic is pure)

**Recommendation:** Extract pure command logic to commonMain, keep DB queries in androidMain

---

#### **DragHandler.kt** (MASTER: 200 vs LEGACY: 421 = 2.1x)

**MASTER provides:**
- `DragOperation`, `DragResult`, `Direction` data classes
- Methods: handleCommand, createDrag, parseDragFromTo, parseDragDirection

**LEGACY adds:**
- Gesture execution system
- State tracking job
- Cursor position polling
- Movement detection
- GestureDescription building
- Gesture queue dispatch
- Continuous drag support

**KMP convertible:** 0% (all execution is AccessibilityService dependent)

**Recommendation:** Keep MASTER parser, add AndroidDragExecutor

---

#### **SpeechEngineManager.kt** (MASTER: 433 vs LEGACY: 860 = 2.0x)

**MASTER provides:**
- Clean orchestrator pattern
- StateFlow/SharedFlow based
- Methods: initialize, startListening, stopListening, mute

**LEGACY adds:**
- Extensive threading documentation
- Mutex + AtomicBoolean thread safety
- Hybrid factory for Vivoka
- SQLite state persistence

**KMP convertible:** 80% (MASTER design is superior, LEGACY adds patterns)

**Recommendation:** Use MASTER design, adopt LEGACY thread safety patterns

---

### 2.3 Additional Conflict Files (10 files)

| File | MASTER | LEGACY | Recommendation |
|------|--------|--------|----------------|
| CommandStatusOverlay.kt | 260 | 440 | MASTER state + Android renderer |
| DangerousElementDetector.kt | 276 | 287 | Similar, keep MASTER |
| ElementInfo.kt | 215 | 337 | Merge ExplorationBehavior enum |
| ExplorationStats.kt | 81 | 121 | Merge data classes |
| InputHandler.kt | 211 | 275 | MASTER + Android executor |
| OverlayManager.kt | 296 | 370 | MASTER + Android impl |
| ProcessingMode.kt | 58 | 84 | Merge enums |
| ScreenFingerprinter.kt | 333 | 330 | Similar, keep MASTER |
| SystemHandler.kt | 118 | 203 | MASTER + AndroidSystemExecutor |

---

## Part 3: Legacy-Only File Analysis (400 files)

### 3.1 KMP-Ready Files (141 files)

These have ZERO Android imports and can move directly to commonMain:

**State Detection System (31 files):**
```
BaseStateDetector.kt          - Base class for state detection
DialogStateDetector.kt        - Dialog/popup detection
EmptyStateDetector.kt         - Empty screen detection
ErrorStateDetector.kt         - Error state detection
LoadingStateDetector.kt       - Loading indicator detection
LoginStateDetector.kt         - Login screen detection
PermissionStateDetector.kt    - Permission dialog detection
TutorialStateDetector.kt      - Tutorial/onboarding detection
StateDetectionPipeline.kt     - Detection orchestration
StateDetectorFactory.kt       - Factory for detectors
StateDetectionHelpers.kt      - Helper functions
StateDetectionPatterns.kt     - Pattern definitions
StateDetectionStrategy.kt     - Strategy interface
StateMetadata.kt              - Metadata model
TemporalStateValidator.kt     - Time-based validation
CommandExecutionStateMachine.kt - Command state machine
BlockedState.kt               - Blocked state model
ConnectionState.kt            - Connection state enum
ExplorationState.kt           - Exploration state model
ScreenState.kt                - Screen state model
+ 11 more state-related files
```

**Entity/Model Classes (20 files):**
```
AppEntity.kt                  - App data model
LearnedAppEntity.kt           - Learned app model
ScrapedAppEntity.kt           - Scraped app model
ScrapedElementEntity.kt       - Scraped element model
ScrapedHierarchyEntity.kt     - Hierarchy model
NavigationEdgeEntity.kt       - Navigation graph edge
ExplorationSessionEntity.kt   - Session model
GeneratedCommandEntity.kt     - Command model
ScreenContextEntity.kt        - Screen context
ScreenStateEntity.kt          - Screen state
UserInteractionEntity.kt      - User interaction
ElementRelationshipEntity.kt  - Element relationships
ElementStateHistoryEntity.kt  - State history
ScreenTransitionEntity.kt     - Transition model
+ 6 more entity files
```

**Interface Contracts (24 files):**
```
IDatabaseContext.kt           - Database context interface
ILearnedAppOperations.kt      - Learned app operations
INavigationOperations.kt      - Navigation operations
IScreenStateOperations.kt     - Screen state operations
ISessionOperations.kt         - Session operations
ISpeechContext.kt             - Speech context
IServiceContext.kt            - Service context
IUIContext.kt                 - UI context
IVoiceOSContext.kt            - Main VoiceOS context
IVoiceOSServiceInternal.kt    - Internal service
IVoiceOSServiceLocal.kt       - Local service
+ 13 more interface files
```

**Pattern Matchers (9 files):**
```
ClassNamePatternMatcher.kt    - Match by class name
ResourceIdPatternMatcher.kt   - Match by resource ID
TextPatternMatcher.kt         - Match by text content
MaterialDesignPatternMatcher.kt - Material component patterns
HierarchyPatternMatcher.kt    - Hierarchy-based matching
PatternMatcher.kt             - Base matcher interface
PatternConstants.kt           - Pattern constants
ElementMatcher.kt             - Element matching
RegexSanitizer.kt             - Regex sanitization
```

**Other KMP-Ready (57 files):**
```
AIContext.kt, AvidCreationMetrics.kt, ChecklistManager.kt,
ConfidenceCalibrator.kt, Debouncer.kt, DivergenceReport.kt,
ElementClassification.kt, ExplorationProgress.kt, ExplorationStrategy.kt,
HashUtils.kt, HierarchyMapGenerator.kt, InputValidator.kt,
LLMPromptFormat.kt, LoginPromptAction.kt, MetadataQuality.kt,
NavigationGraph.kt, NavigationGraphBuilder.kt, ProgressTracker.kt,
QuantizedCommand.kt, QuantizedContext.kt, QuantizedNavigation.kt,
ScreenHashCalculator.kt, etc.
```

### 3.2 Android-Specific Files (349 files)

**Accessibility Service Core (16 files):**
```
AccessibilityDashboard.kt     - Dashboard UI
AccessibilityNodeExtensions.kt - Node extensions
AccessibilityNodeManager.kt   - Node management
AccessibilityOverlayService.kt - Overlay service
AccessibilityScrapingIntegration.kt - Scraping integration
AccessibilityServiceMonitor.kt - Service monitor
AccessibilitySettings.kt      - Settings
VoiceOSService.kt             - Main service
VoiceRecognitionManager.kt    - Recognition management
```

**UI Overlays (25+ files):**
```
AvidCreationDebugOverlay.kt
CommandDisambiguationOverlay.kt
CommandLabelOverlay.kt
CommandStatusOverlay.kt
ConfidenceOverlay.kt
ContextMenuOverlay.kt
CursorMenuOverlay.kt
GridOverlay.kt
HelpOverlay.kt
LoginPromptOverlay.kt
NumberOverlay.kt
PostLearningOverlay.kt
ProgressOverlay.kt
QualityIndicatorOverlay.kt
RenameHintOverlay.kt
VoiceStatusOverlay.kt
VuidCreationOverlay.kt
+ more overlay files
```

**Activities (10+ files):**
```
CleanupPreviewActivity.kt
ContentCaptureSafeComposeActivity.kt
DeveloperSettingsActivity.kt
LearnAppActivity.kt
LearnAppSettingsActivity.kt
LearnWebActivity.kt
MainActivity.kt
PermissionRequestActivity.kt
```

**Handlers with Android Dependencies (25 files):**
```
ActionHandler.kt, AppHandler.kt, BluetoothHandler.kt,
CursorGestureHandler.kt, DatabaseCommandHandler.kt,
DeviceHandler.kt, DragHandler.kt, GestureHandler.kt,
HelpMenuHandler.kt, InputHandler.kt, NavigationHandler.kt,
NumberHandler.kt, RenameCommandHandler.kt, SelectHandler.kt,
SnapToElementHandler.kt, SystemHandler.kt, UIHandler.kt,
VoiceCursorEventHandler.kt, etc.
```

**Database/Persistence (26 files):**
```
DatabaseBackupManager.kt
DatabaseCommandHandler.kt
DatabaseIntegrityChecker.kt
DatabaseManager.kt
DatabaseMetrics.kt
DatabaseProvider.kt
LearnAppDao.kt
LearnAppDatabaseAdapter.kt
LearnAppRepository.kt
VoiceOSCoreDatabaseAdapter.kt
WebScrapingDatabase.kt
+ query files and adapters
```

**LearnApp System (40+ files):**
```
LearnAppCore.kt
LearnAppIntegration.kt
LearnAppNotificationManager.kt
LearnAppPreferences.kt
LearnedAppTracker.kt
JustInTimeLearner.kt
JitElementCapture.kt
ExplorationEngine.kt
ScreenExplorer.kt
+ UI, detection, and integration files
```

---

## Part 4: Platform Parity Analysis

### 4.1 Immediate KMP Benefit (141 files)

These files provide cross-platform functionality TODAY:

| Category | Files | Benefit |
|----------|-------|---------|
| State Detectors | 31 | iOS/Desktop can detect UI states |
| Entities | 20 | Shared data models |
| Interfaces | 24 | Common API contracts |
| Pattern Matchers | 9 | Text/UI pattern recognition |
| Other Logic | 57 | Business logic sharing |

### 4.2 Future Platform Parity Needed (349 files)

**HIGH Priority - Core Functionality:**
| Component | Android | iOS | Desktop |
|-----------|---------|-----|---------|
| Accessibility Service | ✅ | ❌ Need | ❌ Need |
| Gesture Execution | ✅ | ❌ Need | ❌ Need |
| Screen Scraping | ✅ | ❌ Need | ❌ Need |
| Voice Recognition | ✅ | ❌ Need | ❌ Need |

**MEDIUM Priority - UI Components:**
| Component | Android | iOS | Desktop |
|-----------|---------|-----|---------|
| Overlay System | ✅ Compose | ❌ SwiftUI | ❌ Compose Desktop |
| Settings UI | ✅ | ❌ | ❌ |
| Debug Tools | ✅ | ❌ | ❌ |

**LOW Priority - Platform-Specific:**
| Component | Approach |
|-----------|----------|
| Database | Already uses SQLDelight (KMP) |
| Networking | Use Ktor (KMP) |
| File I/O | Use okio (KMP) |

### 4.3 Abstraction Interfaces Needed

For cross-platform support, these interfaces need platform implementations:

```kotlin
// commonMain
expect interface IAccessibilityService {
    fun getScreenContent(): ScreenContent
    fun performAction(action: AccessibilityAction): Boolean
}

expect interface IGestureExecutor {
    suspend fun executeTap(x: Int, y: Int): Boolean
    suspend fun executeSwipe(from: Point, to: Point): Boolean
}

expect interface IScreenCapture {
    suspend fun captureScreen(): Bitmap?
}
```

---

## Part 5: External Dependencies

### 5.1 Modules Depending on LEGACY

| Module | Import | Action Required |
|--------|--------|-----------------|
| Actions | `voiceoscore.accessibility.IVoiceOSCallback` | Update import path |
| Actions | `voiceoscore.accessibility.IVoiceOSService` | Update import path |
| WebAvanue | `voiceoscoreng.common.Bounds` | Update to voiceoscore.Bounds |
| WebAvanue | `voiceoscoreng.common.ElementInfo` | Update to voiceoscore.ElementInfo |
| WebAvanue | `voiceoscoreng.extraction.ElementParser` | Update import path |

### 5.2 LEGACY Dependencies on Other Modules

| Dependency | Used In | Status |
|------------|---------|--------|
| AvidCreator | AVID generation | ✅ Now KMP |
| Database | Persistence | ✅ SQLDelight (KMP) |
| SpeechRecognition | Voice input | Needs review |
| CommandManager | Command execution | Needs review |

---

## Part 6: Recommendations Summary

### Immediate Actions (No Risk)

1. **Delete empty folders:** Voice/Core (done)
2. **Migrate 141 KMP-ready files** to commonMain
3. **Keep 30 MASTER-superior conflict files** as-is

### Careful Actions (Low Risk)

4. **Merge 5 conflict files** with extractable pure logic:
   - SpeedController.kt (95% KMP)
   - CursorHistoryTracker.kt (80% KMP)
   - SpeechEngineManager.kt (80% KMP)
   - CommandGenerator.kt (60% KMP)
   - ElementInfo.kt (merge enum)

### Complex Actions (Medium Risk)

5. **Create Android implementations** for 15 conflict files:
   - SelectHandler, NumberHandler, GestureHandler
   - DragHandler, BoundaryDetector, HelpMenuHandler
   - CommandStatusOverlay, InputHandler, SystemHandler, etc.

### Future Actions (Platform Parity)

6. **Create iOS implementations** for:
   - State detection system
   - Pattern matchers
   - Core interfaces

7. **Create Desktop implementations** for:
   - Same as iOS

---

## Appendix A: File Count Summary

| Location | Total | KMP | Android | Compose |
|----------|-------|-----|---------|---------|
| MASTER (VoiceOSCore) | 217 | 217 | 0 | 0 |
| LEGACY (VoiceOS/VoiceOSCore) | 490 | 141 | 349 | 59 |
| **Combined** | **707** | **358** | **349** | **59** |

---

## Appendix B: Package Structure After Consolidation

```
Modules/VoiceOSCore/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/voiceoscore/
│   │   ├── ActionCategory.kt         (MASTER)
│   │   ├── ActionCoordinator.kt      (MASTER)
│   │   ├── BaseStateDetector.kt      (migrated from LEGACY)
│   │   ├── DialogStateDetector.kt    (migrated from LEGACY)
│   │   ├── EasingFunctions.kt        (extracted from LEGACY SpeedController)
│   │   ├── ElementInfo.kt            (MASTER + ExplorationBehavior)
│   │   ├── ... (358 total files)
│   │   └── interfaces/
│   │       ├── IAccessibilityService.kt (expect)
│   │       ├── IGestureExecutor.kt      (expect)
│   │       └── IScreenCapture.kt        (expect)
│   ├── androidMain/kotlin/com/augmentalis/voiceoscore/
│   │   ├── AndroidSelectHandler.kt   (LEGACY implementation)
│   │   ├── AndroidGestureExecutor.kt (LEGACY implementation)
│   │   ├── VoiceOSService.kt         (LEGACY)
│   │   ├── ... (349 total files)
│   │   └── impl/
│   │       ├── AndroidAccessibilityService.kt (actual)
│   │       ├── AndroidGestureExecutor.kt      (actual)
│   │       └── AndroidScreenCapture.kt        (actual)
│   ├── iosMain/kotlin/com/augmentalis/voiceoscore/
│   │   └── impl/
│   │       ├── IOSAccessibilityService.kt (actual - future)
│   │       └── IOSGestureExecutor.kt      (actual - future)
│   └── desktopMain/kotlin/com/augmentalis/voiceoscore/
│       └── impl/
│           ├── DesktopAccessibilityService.kt (actual - future)
│           └── DesktopGestureExecutor.kt      (actual - future)
└── build.gradle.kts
```

---

**Analysis Complete** | Ready for Implementation Planning
