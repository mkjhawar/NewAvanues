# VoiceOSCore Consolidation - Implementation Tasks

**Date:** 2026-01-19 | **Version:** V1 | **Author:** Claude
**Status:** Ready for Implementation | **Priority:** P0 - Critical
**Related:** `Docs/Analysis/VoiceOSCore-Deep-Analysis-260119.md`

---

## Executive Summary

This document provides the detailed task breakdown for consolidating VoiceOSCore modules into a single KMP-first architecture. The goal is to merge LEGACY files into MASTER without breaking Android functionality.

### Metrics

| Category | Count | Target |
|----------|-------|--------|
| MASTER files (keep all) | 217 | Stay in place |
| LEGACY KMP-ready | 141 | → commonMain |
| LEGACY Android-specific | 349 | → androidMain |
| Conflict files (MASTER superior) | 30 | Keep MASTER |
| Conflict files (need merge) | 20 | Merge carefully |

---

## Phase 1: Preparation

**Objective:** Create safety net before any code changes

### Task 1.1: Create Backup Branch
```bash
git checkout -b backup-voiceoscore-pre-consolidation
git push origin backup-voiceoscore-pre-consolidation
```
- [ ] Branch created
- [ ] Branch pushed to remote

### Task 1.2: Document Baseline
- [ ] Run full build: `./gradlew :Modules:VoiceOSCore:assembleDebug`
- [ ] Run tests: `./gradlew :Modules:VoiceOSCore:test`
- [ ] Document test count and pass rate
- [ ] Screenshot/log build output

### Task 1.3: Verify Current State
- [ ] Confirm MASTER location: `Modules/VoiceOSCore/` (217 files)
- [ ] Confirm LEGACY location: `Modules/VoiceOS/VoiceOSCore/` (490 files)
- [ ] Verify no other terminals working on these files

---

## Phase 2: Conflict Resolution (20 Files)

**Objective:** Merge LEGACY features into MASTER for files where LEGACY has more functionality

### Strategy Overview

| Strategy | Files | Approach |
|----------|-------|----------|
| A - Pure KMP | 5 | Extract logic to commonMain |
| B - Android-specific | 10 | Add Android impl to androidMain |
| C - Data merge | 5 | Merge data classes/fields |

### Task 2.1: Strategy A - Pure KMP Features (5 files)

**Target:** Extract KMP-ready logic from LEGACY to enhance MASTER

#### 2.1.1 SpeedController.kt
- **MASTER:** 124 lines (basic speed control)
- **LEGACY:** 320 lines (easing functions, velocity, acceleration)
- **Action:** Extract easing functions, velocity math to commonMain
- **Risk:** LOW (pure math operations)
- [ ] Read both files, identify LEGACY-only features
- [ ] Add easing functions to MASTER
- [ ] Add velocity/acceleration properties
- [ ] Verify compilation

#### 2.1.2 ExplorationStats.kt
- **MASTER:** 89 lines
- **LEGACY:** 156 lines
- **Action:** Merge data classes, add optional fields
- **Risk:** LOW (data classes only)
- [ ] Compare data class fields
- [ ] Add missing fields to MASTER with defaults
- [ ] Verify serialization compatibility

#### 2.1.3 ElementInfo.kt
- **MASTER:** 215 lines
- **LEGACY:** 337 lines
- **Action:** Add ExplorationBehavior enum, additional properties
- **Risk:** LOW (data + enum)
- [ ] Add ExplorationBehavior enum
- [ ] Add missing properties with defaults
- [ ] Update any dependent code

#### 2.1.4 CursorHistoryTracker.kt (KMP-extractable: 80%)
- **MASTER:** 141 lines
- **LEGACY:** 474 lines
- **Action:** Extract movement detection, expiration logic
- **Risk:** MEDIUM (state management)
- [ ] Identify pure logic (movement detection, history tracking)
- [ ] Extract to common, leave Android-specific in android
- [ ] Create interface if needed

#### 2.1.5 OverlayTheme.kt
- **MASTER:** 45 lines
- **LEGACY:** 89 lines
- **Action:** Merge theme properties
- **Risk:** LOW (data only)
- [ ] Add missing theme properties
- [ ] Ensure defaults are appropriate

### Task 2.2: Strategy B - Android-Specific Features (10 files)

**Target:** Keep MASTER in commonMain, add Android implementations

#### 2.2.1 SelectHandler.kt (HIGHEST PRIORITY)
- **MASTER:** 271 lines (clipboard abstraction)
- **LEGACY:** 836 lines (AccessibilityNodeInfo traversal, context menus)
- **Action:** Keep MASTER in common, create AndroidSelectHandler
- **Risk:** HIGH (core selection functionality)
- [ ] Keep MASTER IClipboardProvider interface
- [ ] Create `SelectHandler.android.kt` with AccessibilityNodeInfo logic
- [ ] Create expect/actual pattern if needed
- [ ] Test selection on Android device

#### 2.2.2 CommandGenerator.kt
- **MASTER:** 346 lines (pure command generation)
- **LEGACY:** 729 lines (state-aware, checkable/expandable)
- **Action:** Extract pure logic to common, Android state queries to android
- **Risk:** HIGH (core voice commands)
- [ ] Extract generateCheckableCommands() to common
- [ ] Extract generateExpandableCommands() to common
- [ ] Keep Android-specific state queries in androidMain
- [ ] Test command generation thoroughly

#### 2.2.3 GestureHandler.kt
- **MASTER:** 194 lines (GestureType enum, config)
- **LEGACY:** 469 lines (gesture queueing, multi-stroke, callbacks)
- **Action:** Keep MASTER data in common, Android executor in android
- **Risk:** MEDIUM (gesture execution)
- [ ] Keep GestureType, GestureConfig in common
- [ ] Create IDragExecutor interface in common
- [ ] Implement AndroidGestureExecutor in androidMain
- [ ] Test gestures on device

#### 2.2.4 NumberHandler.kt
- **MASTER:** 214 lines (number parsing, word mappings)
- **LEGACY:** 530 lines (tree traversal, overlay visualization)
- **Action:** Keep MASTER parsing in common, Android overlay in android
- **Risk:** MEDIUM
- [ ] Keep number parsing logic in common
- [ ] Create AndroidNumberHandler with overlay logic
- [ ] Test number commands

#### 2.2.5 HelpMenuHandler.kt
- **MASTER:** 197 lines (HelpCategory, HelpCommand data)
- **LEGACY:** 560 lines (documentation URLs, tutorials, auto-hide)
- **Action:** Keep data in common, Android overlay in android
- **Risk:** LOW
- [ ] Keep HelpCategory, HelpCommand in common
- [ ] Create AndroidHelpMenuHandler with overlay + Intent
- [ ] Test help menu display

#### 2.2.6 BoundaryDetector.kt
- **MASTER:** 143 lines (geometry calculations)
- **LEGACY:** 414 lines (display metrics, safe area, multi-display)
- **Action:** Keep geometry in common, Android display in android
- **Risk:** MEDIUM
- [ ] Keep basic geometry calculations in common
- [ ] Create AndroidBoundaryDetector with display metrics
- [ ] Test boundary detection

#### 2.2.7 DragHandler.kt
- **MASTER:** 200 lines (drag parser)
- **LEGACY:** 421 lines (gesture dispatch, continuous drag)
- **Action:** Keep parser in common, Android executor in android
- **Risk:** MEDIUM
- [ ] Keep drag parsing in common
- [ ] Create AndroidDragExecutor
- [ ] Test drag operations

#### 2.2.8 SpeechEngineManager.kt
- **MASTER:** 433 lines (KMP design)
- **LEGACY:** 860 lines (Vivoka handling, thread safety docs)
- **Action:** Use MASTER design, add LEGACY thread safety
- **Risk:** HIGH (speech recognition core)
- [ ] Review MASTER vs LEGACY thread safety
- [ ] Add any missing safety mechanisms
- [ ] Document thread safety requirements
- [ ] Test speech recognition

#### 2.2.9 OverlayManager.kt
- **MASTER:** 296 lines
- **LEGACY:** 370 lines
- **Action:** Merge overlay management features
- **Risk:** MEDIUM
- [ ] Compare features
- [ ] Add missing overlay management to androidMain
- [ ] Test overlay display

#### 2.2.10 NavigationHandler.kt
- **MASTER:** 245 lines
- **LEGACY:** 489 lines
- **Action:** Keep navigation data in common, Android impl in android
- **Risk:** MEDIUM
- [ ] Extract navigation data structures to common
- [ ] Keep Android navigation execution in androidMain
- [ ] Test navigation commands

### Task 2.3: Strategy C - Data Merge (5 files)

#### 2.3.1 QuantizedCommand.kt
- [ ] Compare data fields
- [ ] Add missing fields to MASTER

#### 2.3.2 QuantizedContext.kt
- [ ] Compare data fields
- [ ] Add missing fields to MASTER

#### 2.3.3 QuantizedNavigation.kt
- [ ] Compare data fields
- [ ] Add missing fields to MASTER

#### 2.3.4 QuantizedScreen.kt
- [ ] Compare data fields
- [ ] Add missing fields to MASTER

#### 2.3.5 ProcessingMode.kt
- [ ] Compare enums
- [ ] Add missing modes to MASTER

---

## Phase 3: KMP Migration (141 Files)

**Objective:** Move KMP-ready LEGACY files to commonMain

### Task 3.1: State Detectors (31 files)
**Risk:** LOW (pure logic, no Android imports)

Files to migrate:
```
BaseStateDetector.kt
DialogStateDetector.kt
EmptyStateDetector.kt
ErrorStateDetector.kt
LoadingStateDetector.kt
LoginStateDetector.kt
MultiStateDetectionEngine.kt
PermissionStateDetector.kt
TutorialStateDetector.kt
StateDetectionPipeline.kt
StateDetectorFactory.kt
... (20 more)
```

- [ ] Copy all 31 files to `commonMain/kotlin/com/augmentalis/voiceoscore/`
- [ ] Remove sub-package paths (flatten to root)
- [ ] Update package declarations
- [ ] Fix any import errors
- [ ] Verify compilation

### Task 3.2: Entities/Models (20 files)
**Risk:** LOW (data classes)

Files to migrate:
```
AppEntity.kt
LearnedAppEntity.kt
ScrapedAppEntity.kt
ScrapedElementEntity.kt
NavigationEdgeEntity.kt
... (15 more)
```

- [ ] Copy files to commonMain
- [ ] Convert Android Rect → Bounds (MASTER class)
- [ ] Update package declarations
- [ ] Verify compilation

### Task 3.3: Interfaces (24 files)
**Risk:** LOW (contracts only)

Files to migrate:
```
IDatabaseContext.kt
ILearnedAppOperations.kt
INavigationOperations.kt
IScreenStateOperations.kt
ISpeechContext.kt
IVoiceOSContext.kt
... (18 more)
```

- [ ] Copy files to commonMain
- [ ] Update package declarations
- [ ] These define contracts, no implementation changes needed

### Task 3.4: Pattern Matchers (9 files)
**Risk:** LOW (string/regex logic)

Files to migrate:
```
ClassNamePatternMatcher.kt
ResourceIdPatternMatcher.kt
TextPatternMatcher.kt
MaterialDesignPatternMatcher.kt
... (5 more)
```

- [ ] Copy files to commonMain
- [ ] Update package declarations
- [ ] Verify regex patterns work cross-platform

### Task 3.5: Utilities (57 files)
**Risk:** MEDIUM (may have hidden dependencies)

- [ ] Review each file for Android imports
- [ ] Move verified KMP-ready files
- [ ] Document any files that can't be moved

---

## Phase 4: Android Migration (349 Files)

**Objective:** Move Android-specific LEGACY files to androidMain

### Task 4.1: Accessibility System (27 files)
```
AccessibilityDashboard.kt
AccessibilityModule.kt
AccessibilityNodeExtensions.kt
... (24 more)
```
- [ ] Copy to `androidMain/kotlin/com/augmentalis/voiceoscore/`
- [ ] Flatten package structure
- [ ] Update package declarations
- [ ] Verify compilation

### Task 4.2: LearnApp System (40+ files)
```
LearnAppActivity.kt
LearnAppCore.kt
LearnAppDao.kt
... (37+ more)
```
- [ ] Copy to androidMain
- [ ] Update package declarations
- [ ] Verify database integration

### Task 4.3: Database Handlers (20+ files)
```
DatabaseBackupManager.kt
DatabaseCommandHandler.kt
DatabaseIntegrityChecker.kt
... (17+ more)
```
- [ ] Copy to androidMain
- [ ] Update package declarations
- [ ] Verify database operations

### Task 4.4: UI Overlays (25+ files)
```
CommandDisambiguationOverlay.kt
CommandLabelOverlay.kt
CursorMenuOverlay.kt
GridOverlay.kt
... (21+ more)
```
- [ ] Copy to androidMain
- [ ] Update package declarations
- [ ] Verify overlay display

### Task 4.5: Remaining Android Files (237 files)
- [ ] Batch copy remaining files
- [ ] Update all package declarations
- [ ] Fix compilation errors
- [ ] Document any issues

---

## Phase 5: Archive LEGACY

**Objective:** Safely archive original LEGACY files

### Task 5.1: Create Archive Directory
```bash
mkdir -p archive/deprecated/VoiceOS-VoiceOSCore-260119
```
- [ ] Directory created

### Task 5.2: Move LEGACY Files
```bash
mv Modules/VoiceOS/VoiceOSCore archive/deprecated/VoiceOS-VoiceOSCore-260119/
```
- [ ] Files moved
- [ ] Verify archive contains all original files

### Task 5.3: Update settings.gradle.kts
- [ ] Remove `include(":Modules:VoiceOS:VoiceOSCore")`
- [ ] Keep `include(":Modules:VoiceOSCore")`
- [ ] Verify build still works

---

## Phase 6: Update External References

**Objective:** Fix imports in dependent modules

### Task 6.1: Actions Module
File: `Modules/Actions/src/androidMain/.../VoiceOSConnection.kt`

```kotlin
// Old
import com.augmentalis.voiceoscore.accessibility.IVoiceOSCallback

// New
import com.augmentalis.voiceoscore.IVoiceOSCallback
```
- [ ] Update imports
- [ ] Verify compilation

### Task 6.2: WebAvanue Module
File: `Modules/WebAvanue/universal/src/commonTest/.../WebAvanuePageExtractorTest.kt`

```kotlin
// Old
import com.augmentalis.voiceoscoreng.common.Bounds

// New
import com.augmentalis.voiceoscore.Bounds
```
- [ ] Update imports
- [ ] Verify compilation

### Task 6.3: Search for Remaining References
```bash
grep -r "voiceos.voiceoscore" --include="*.kt" .
grep -r "voiceoscoreng" --include="*.kt" .
```
- [ ] Find all old references
- [ ] Update each one
- [ ] Document changes

---

## Phase 7: Verification

**Objective:** Ensure nothing is broken

### Task 7.1: Build Verification
- [ ] `./gradlew :Modules:VoiceOSCore:assembleDebug` passes
- [ ] `./gradlew :Modules:VoiceOSCore:test` passes
- [ ] Full monorepo build passes

### Task 7.2: KMP Target Verification
- [ ] Android target compiles
- [ ] iOS target compiles (if configured)
- [ ] Desktop target compiles (if configured)

### Task 7.3: Runtime Verification
- [ ] Voice commands work on Android device
- [ ] Selection commands work
- [ ] Navigation commands work
- [ ] Gesture commands work
- [ ] Help menu displays

### Task 7.4: Documentation
- [ ] Update MASTER-INDEX.md
- [ ] Update MODULE-CONSOLIDATION doc
- [ ] Mark task complete in handover report

---

## Risk Matrix

| Phase | Risk Level | Mitigation |
|-------|------------|------------|
| Phase 1 | LOW | Automated, reversible |
| Phase 2 | HIGH | Test after each file |
| Phase 3 | LOW | Pure logic, no runtime deps |
| Phase 4 | MEDIUM | Batch moves, fix errors |
| Phase 5 | LOW | Archive only, no delete |
| Phase 6 | MEDIUM | Search and verify |
| Phase 7 | N/A | Verification only |

---

## Rollback Plan

If any phase fails:

1. **Immediate:** `git checkout backup-voiceoscore-pre-consolidation`
2. **Partial:** `git stash` and investigate specific issue
3. **Archive recovery:** Files in `archive/deprecated/` are intact

---

## Success Criteria

- [ ] Single VoiceOSCore module with KMP structure
- [ ] All 141 KMP-ready files in commonMain
- [ ] All 349 Android files in androidMain
- [ ] All 20 conflict files properly merged
- [ ] All tests passing
- [ ] Voice commands functional on device
- [ ] No broken imports in dependent modules
- [ ] LEGACY archived (not deleted)

---

## Estimated Effort

| Phase | Tasks | Complexity |
|-------|-------|------------|
| Phase 1 | 3 | LOW |
| Phase 2 | 20 files | HIGH - careful review |
| Phase 3 | 141 files | MEDIUM - batch work |
| Phase 4 | 349 files | MEDIUM - batch work |
| Phase 5 | 3 | LOW |
| Phase 6 | ~10 | MEDIUM |
| Phase 7 | 4 | LOW |

---

**Document Complete** | Ready for Implementation

*Reference handover: `.claude/handovers/Handover-VoiceOSCore-Consolidation-260119.md`*
*Reference analysis: `Docs/Analysis/VoiceOSCore-Deep-Analysis-260119.md`*
