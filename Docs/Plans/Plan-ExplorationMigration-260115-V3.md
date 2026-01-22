# Implementation Plan: Exploration Engine Migration to KMP

**Date:** 2026-01-15 | **Version:** V3 (KMP Compatibility Analysis)
**Type:** CODE MIGRATION PLAN
**Source:** VoiceOSCore (OLD) → VoiceOSCoreNG (NEW KMP)
**Scope:** ~2,978 lines across 8 files

---

## KMP Compatibility Analysis

### ✅ CAN be KMP (commonMain) - ~600 lines

| Component | Source | Lines | KMP Status | Notes |
|-----------|--------|-------|------------|-------|
| DangerDetector | DangerDetector.kt | 227 | ✅ 100% KMP | Pure string matching |
| ExplorationDebugCallback | ExplorationDebugCallback.kt | 103 | ✅ 100% KMP | Interface only |
| ExplorationFrame | DFSExplorer.kt:39-53 | 15 | ✅ 100% KMP | Data class |
| ExploreScreenResult | DFSExplorer.kt:58-70 | 13 | ✅ 100% KMP | Sealed class |
| DFSState | DFSExplorer.kt:75-82 | 8 | ⚠️ Refactor | Replace java.util.Stack |
| CumulativeTracking | DFSExplorer.kt:90-108 | 19 | ⚠️ Refactor | Replace ConcurrentHashMap |
| ClickFailureReason | ElementClicker.kt:37-42 | 6 | ✅ 100% KMP | Data class |
| ExplorationConfig | NEW | ~80 | ✅ KMP | Configuration |
| Interfaces | NEW | ~200 | ✅ KMP | Abstractions |

### ❌ MUST stay Android (androidMain) - ~2,300 lines

| Component | Source | Lines | Android Deps |
|-----------|--------|-------|--------------|
| ElementClicker | ElementClicker.kt | 405 | AccessibilityService, GestureDescription, AccessibilityNodeInfo |
| DFSExplorer | DFSExplorer.kt | 596 | AccessibilityService, AccessibilityNodeInfo, Context |
| ElementRegistrar | ElementRegistrar.kt | 418 | Context, UUIDCreator, ThirdPartyUuidGenerator |
| ExplorationMetrics | ExplorationMetrics.kt | 197 | Context, WindowManager, Android overlays |
| ExplorationNotifier | ExplorationNotifier.kt | 216 | NotificationManager, ToneGenerator |
| ExplorationEngine | EXISTING + merge | ~500 | Orchestrates all above |

---

## Target Structure (Final)

```
Modules/VoiceOSCoreNG/src/
├── commonMain/kotlin/com/augmentalis/voiceoscoreng/exploration/
│   │
│   │  # Data Classes (KMP)
│   ├── ExplorationFrame.kt           # DFS stack frame
│   ├── DFSState.kt                   # Algorithm state (uses ArrayDeque instead of Stack)
│   ├── ExploreScreenResult.kt        # Result sealed class
│   ├── CumulativeTracking.kt         # Progress (uses atomics for thread safety)
│   ├── ClickFailureReason.kt         # Telemetry data
│   ├── ExplorationConfig.kt          # Tunable parameters
│   │
│   │  # Interfaces (KMP)
│   ├── IExplorationEngine.kt         # Main engine interface
│   ├── IElementClicker.kt            # Click operations
│   ├── IElementRegistrar.kt          # UUID/alias registration
│   ├── IExplorationMetrics.kt        # Metrics tracking
│   ├── IExplorationNotifier.kt       # User notifications
│   ├── ExplorationDebugCallback.kt   # Debug events
│   │
│   │  # Pure Logic (KMP)
│   └── DangerDetector.kt             # Dangerous element detection
│
└── androidMain/kotlin/com/augmentalis/voiceoscoreng/exploration/
    │
    │  # EXISTING
    ├── ExplorationEngine.kt          # UPDATE: implement IExplorationEngine
    │
    │  # NEW (migrate from OLD)
    ├── DFSExplorer.kt                # DFS algorithm
    ├── ElementClicker.kt             # Click operations
    ├── ElementRegistrar.kt           # UUID/alias registration
    ├── ExplorationMetrics.kt         # Metrics tracking
    └── ExplorationNotifier.kt        # Notifications
```

---

## Phase Breakdown

### Phase 1: Create commonMain/exploration/ folder + Data Classes

| Task | File | Description |
|------|------|-------------|
| 1.1 | Create folder | `mkdir -p .../commonMain/.../exploration/` |
| 1.2 | ExplorationFrame.kt | DFS stack frame data class |
| 1.3 | DFSState.kt | Algorithm state (ArrayDeque, not Stack) |
| 1.4 | ExploreScreenResult.kt | Result sealed class |
| 1.5 | CumulativeTracking.kt | Thread-safe progress tracking |
| 1.6 | ClickFailureReason.kt | Click failure telemetry |
| 1.7 | ExplorationConfig.kt | Configurable parameters |

### Phase 2: Create commonMain Interfaces

| Task | File | Description |
|------|------|-------------|
| 2.1 | IExplorationEngine.kt | Main exploration interface |
| 2.2 | IElementClicker.kt | Click operations interface |
| 2.3 | IElementRegistrar.kt | Registration interface |
| 2.4 | IExplorationMetrics.kt | Metrics interface |
| 2.5 | IExplorationNotifier.kt | Notification interface |
| 2.6 | ExplorationDebugCallback.kt | Debug callback interface |

### Phase 3: Migrate Pure Logic to commonMain

| Task | File | Description |
|------|------|-------------|
| 3.1 | DangerDetector.kt | Migrate pure string matching logic |

### Phase 4: Migrate Android Code to androidMain

| Task | File | Description |
|------|------|-------------|
| 4.1 | ElementClicker.kt | Migrate with package change |
| 4.2 | ElementRegistrar.kt | Migrate with package change |
| 4.3 | DFSExplorer.kt | Migrate, use commonMain data classes |
| 4.4 | ExplorationMetrics.kt | Migrate with package change |
| 4.5 | ExplorationNotifier.kt | Migrate with package change |

### Phase 5: Integration

| Task | Description |
|------|-------------|
| 5.1 | Update ExplorationEngine.kt to implement IExplorationEngine |
| 5.2 | Add orchestration logic from ExplorationEngineRefactored |
| 5.3 | Verify gradle compilation |

---

## KMP Refactoring Notes

### Stack → ArrayDeque
```kotlin
// OLD (java.util.Stack - NOT KMP)
val explorationStack: Stack<ExplorationFrame> = Stack()

// NEW (kotlin.collections - KMP compatible)
val explorationStack: ArrayDeque<ExplorationFrame> = ArrayDeque()
// push → addLast, pop → removeLast, peek → lastOrNull
```

### ConcurrentHashMap.newKeySet → Atomics/Mutex
```kotlin
// OLD (JVM-specific)
val discoveredVuids: MutableSet<String> = ConcurrentHashMap.newKeySet()

// NEW (KMP - using atomic counter + regular set with mutex)
// Or simpler: just use regular MutableSet for single-threaded access pattern
```

---

## Package Mapping

| OLD Package | NEW Package |
|-------------|-------------|
| `com.augmentalis.voiceoscore.learnapp.exploration` | `com.augmentalis.voiceoscoreng.exploration` |
| `com.augmentalis.voiceoscore.learnapp.models.ElementInfo` | `com.augmentalis.voiceoscoreng.common.ElementInfo` |
| `com.augmentalis.voiceoscore.learnapp.models.ScreenState` | `com.augmentalis.voiceoscoreng.functions.ScreenState` |

---

## Estimated Effort

| Phase | Tasks | New Lines | Time |
|-------|-------|-----------|------|
| Phase 1 | 7 | ~250 | 25 min |
| Phase 2 | 6 | ~200 | 20 min |
| Phase 3 | 1 | ~230 | 15 min |
| Phase 4 | 5 | ~1,800 | 1.5 hours |
| Phase 5 | 3 | ~100 | 20 min |
| **TOTAL** | **22** | **~2,580** | **~3 hours** |

---

## Success Criteria

- [ ] `commonMain/exploration/` has 13 files (data classes + interfaces + DangerDetector)
- [ ] `androidMain/exploration/` has 6 files (1 existing + 5 new)
- [ ] All commonMain files compile without Android dependencies
- [ ] `./gradlew :Modules:VoiceOSCoreNG:compileDebugKotlinAndroid` succeeds
- [ ] ExplorationEngine implements IExplorationEngine

---

**Plan Version:** V3 (KMP Analysis Complete)
**Created:** 2026-01-15
**Implementing:** NOW
