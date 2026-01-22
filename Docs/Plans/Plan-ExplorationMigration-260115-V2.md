# Implementation Plan: Exploration Engine Migration to KMP

**Date:** 2026-01-15 | **Version:** V2 (Updated for folder conventions)
**Type:** CODE MIGRATION PLAN
**Source:** VoiceOSCore (OLD) → VoiceOSCoreNG (NEW KMP)
**Scope:** ~2,978 lines across 8 files

---

## Folder Structure Convention (Per Existing Codebase)

The VoiceOSCoreNG module uses **feature-based folders**:
- `commonMain/.../exploration/` - KMP-compatible exploration code
- `commonMain/.../learnapp/` - Already has ExplorationState.kt
- `androidMain/.../exploration/` - Android-specific implementations

**File Naming Convention:**
- `I{Name}.kt` - Interfaces (e.g., `IExplorationEngine.kt`)
- `{Name}.kt` - Classes/Data classes (e.g., `ExplorationFrame.kt`)
- `{Name}Impl.kt` - Implementations (e.g., `ExplorationEngineImpl.kt`)

---

## Target Structure (CORRECTED)

```
Modules/VoiceOSCoreNG/src/
├── commonMain/kotlin/com/augmentalis/voiceoscoreng/
│   ├── exploration/                    # NEW: Create this folder
│   │   ├── ExplorationFrame.kt         # Data class
│   │   ├── DFSState.kt                 # Data class
│   │   ├── CumulativeTracking.kt       # Progress tracking
│   │   ├── ExploreScreenResult.kt      # Sealed class
│   │   ├── ExplorationConfig.kt        # Configuration
│   │   ├── DangerDetector.kt           # Pure logic (no Android deps)
│   │   ├── IExplorationEngine.kt       # Interface
│   │   ├── IElementClicker.kt          # Interface
│   │   ├── IElementRegistrar.kt        # Interface
│   │   ├── IExplorationMetrics.kt      # Interface
│   │   ├── IExplorationNotifier.kt     # Interface
│   │   └── ExplorationDebugCallback.kt # Callback interface
│   │
│   └── learnapp/                       # EXISTING
│       ├── ExplorationState.kt         # Already exists (KEEP)
│       ├── CommandLearner.kt           # Already exists
│       └── JITLearner.kt               # Already exists
│
└── androidMain/kotlin/com/augmentalis/voiceoscoreng/
    └── exploration/                    # EXISTING folder
        ├── ExplorationEngine.kt        # EXISTS - Update to delegate
        ├── DFSExplorer.kt              # NEW: Migrate here
        ├── ElementClicker.kt           # NEW: Migrate here
        ├── ElementRegistrar.kt         # NEW: Migrate here
        ├── ExplorationMetrics.kt       # NEW: Migrate here
        └── ExplorationNotifier.kt      # NEW: Migrate here
```

---

## Phase Breakdown

### Phase 1: Common Data Classes (commonMain/exploration/)
**Goal:** Create KMP-compatible data structures in new `exploration/` folder

| Task | File | Description | Lines |
|------|------|-------------|-------|
| 1.1 | `ExplorationFrame.kt` | Stack frame for DFS exploration | ~60 |
| 1.2 | `DFSState.kt` | DFS algorithm state container | ~50 |
| 1.3 | `CumulativeTracking.kt` | Thread-safe progress counters | ~70 |
| 1.4 | `ExploreScreenResult.kt` | Sealed class for screen results | ~40 |
| 1.5 | `ExplorationConfig.kt` | Tunable exploration parameters | ~80 |

### Phase 2: Common Interfaces (commonMain/exploration/)
**Goal:** Define abstractions for platform implementations

| Task | File | Description | Lines |
|------|------|-------------|-------|
| 2.1 | `IExplorationEngine.kt` | Main exploration engine interface | ~80 |
| 2.2 | `IElementClicker.kt` | Element click operations interface | ~50 |
| 2.3 | `IElementRegistrar.kt` | UUID/alias registration interface | ~50 |
| 2.4 | `IExplorationMetrics.kt` | Metrics tracking interface | ~40 |
| 2.5 | `IExplorationNotifier.kt` | User notification interface | ~40 |
| 2.6 | `ExplorationDebugCallback.kt` | Debug event callback interface | ~50 |

### Phase 3: Common Pure Logic (commonMain/exploration/)
**Goal:** Migrate platform-independent algorithms

| Task | File | Description | Lines |
|------|------|-------------|-------|
| 3.1 | `DangerDetector.kt` | Dangerous element detection (pure string matching) | ~230 |

### Phase 4: Android Implementations (androidMain/exploration/)
**Goal:** Migrate Android-specific exploration code

| Task | File | Description | Lines |
|------|------|-------------|-------|
| 4.1 | `ElementClicker.kt` | Click with retry, gesture fallback | ~420 |
| 4.2 | `ElementRegistrar.kt` | UUID generation, alias management | ~430 |
| 4.3 | `DFSExplorer.kt` | Iterative DFS algorithm | ~620 |
| 4.4 | `ExplorationMetrics.kt` | VUID metrics, debug overlay | ~210 |
| 4.5 | `ExplorationNotifier.kt` | Android notifications | ~230 |

### Phase 5: Integration
**Goal:** Wire up and verify compilation

| Task | Description |
|------|-------------|
| 5.1 | Update `ExplorationEngine.kt` to implement `IExplorationEngine` |
| 5.2 | Add orchestration logic from ExplorationEngineRefactored |
| 5.3 | Verify gradle compilation |

---

## File Mapping (OLD → NEW)

| OLD Location | NEW Location | Action |
|--------------|--------------|--------|
| `voiceoscore/learnapp/exploration/ExplorationEngineRefactored.kt` | `voiceoscoreng/exploration/ExplorationEngine.kt` | Merge into existing |
| `voiceoscore/learnapp/exploration/DFSExplorer.kt` | `voiceoscoreng/exploration/DFSExplorer.kt` | Copy + adapt |
| `voiceoscore/learnapp/exploration/ElementClicker.kt` | `voiceoscoreng/exploration/ElementClicker.kt` | Copy + adapt |
| `voiceoscore/learnapp/exploration/ElementRegistrar.kt` | `voiceoscoreng/exploration/ElementRegistrar.kt` | Copy + adapt |
| `voiceoscore/learnapp/exploration/DangerDetector.kt` | **commonMain**/exploration/DangerDetector.kt | Extract pure logic |
| `voiceoscore/learnapp/exploration/ExplorationMetrics.kt` | `voiceoscoreng/exploration/ExplorationMetrics.kt` | Copy + adapt |
| `voiceoscore/learnapp/exploration/ExplorationNotifier.kt` | `voiceoscoreng/exploration/ExplorationNotifier.kt` | Copy + adapt |
| `voiceoscore/learnapp/exploration/ExplorationDebugCallback.kt` | **commonMain**/exploration/ExplorationDebugCallback.kt | Interface only |

---

## Package Names

| Location | Package |
|----------|---------|
| commonMain/exploration/ | `com.augmentalis.voiceoscoreng.exploration` |
| androidMain/exploration/ | `com.augmentalis.voiceoscoreng.exploration` |

---

## Dependencies to Handle

### Required Imports from OLD codebase:
```kotlin
// Models needed in commonMain
- ElementInfo (from voiceoscoreng.common or extraction)
- ScreenState (from voiceoscoreng.functions)

// Android-specific (androidMain only)
- AccessibilityService
- AccessibilityNodeInfo
- GestureDescription
- Context, Intent
- NotificationManager
```

### Existing VoiceOSCoreNG classes to reuse:
```kotlin
// Already in VoiceOSCoreNG
- ScreenFingerprinter (commonMain/functions/)
- ElementInfo (commonMain/common/)
- ScreenState (needs to verify location)
```

---

## Estimated Effort

| Phase | Tasks | Lines | Time |
|-------|-------|-------|------|
| Phase 1 | 5 | ~300 | 30 min |
| Phase 2 | 6 | ~310 | 30 min |
| Phase 3 | 1 | ~230 | 20 min |
| Phase 4 | 5 | ~1,910 | 2 hours |
| Phase 5 | 3 | ~200 | 30 min |
| **TOTAL** | **20** | **~2,950** | **~4 hours** |

---

## Success Criteria

- [ ] `commonMain/exploration/` folder created with 12 files
- [ ] `androidMain/exploration/` folder has 6 files (1 existing + 5 new)
- [ ] All files compile: `./gradlew :Modules:VoiceOSCoreNG:compileDebugKotlinAndroid`
- [ ] ExplorationEngine implements IExplorationEngine
- [ ] No imports from old `voiceoscore` package

---

## Risk Mitigations

| Risk | Mitigation |
|------|------------|
| Missing ElementInfo/ScreenState | Verify existing classes in VoiceOSCoreNG, create if missing |
| Import conflicts | Use fully qualified names during transition |
| Compilation failures | Incremental migration, compile after each phase |

---

**Plan Version:** V2 (Folder conventions corrected)
**Created:** 2026-01-15
**Auto-implement:** YES (.auto flag)
