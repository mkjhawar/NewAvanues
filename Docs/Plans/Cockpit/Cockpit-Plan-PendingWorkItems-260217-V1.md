# Cockpit + VoiceOS — Pending Work Items Plan
**Date:** 2026-02-17 | **Mode:** YOLO + CoT + ToT | **Branch:** IosVoiceOS-Development

---

## Priority Ordering (CoT)

| # | Item | Module | Scope | Priority | Rationale |
|---|------|--------|-------|----------|-----------|
| 1 | Wire Spatial Canvas + LayoutModeResolver | Cockpit | ~80 lines changed, 3 files | P0 | Completes dead code — all components exist, just need wiring |
| 2 | WorkflowSidebar | Cockpit | ~250 lines, 1 new + 2 modified | P1 | Extends Cockpit while context fresh |
| 3 | Cursor IMU Fix | VoiceCursor | Investigation + fix | P1 | Regression — cursor renders but won't move |
| 4 | VOS v3.0 Compact Format | VoiceOSCore | ~300 lines, plan exists | P2 | Performance optimization, independent |
| 5 | 3rd-party App Support | Cockpit/App | Research + prototype | P3 | Future phase, Android 12L+ |

**ToT Selected:** Cockpit-first (maximize code proximity), then VoiceOS modules.

---

## Item 1: Wire Spatial Canvas + LayoutModeResolver

**Problem:** SpatialCanvas, SpatialViewportController, AndroidSpatialOrientationSource, and LayoutModeResolver are all fully implemented but NEVER INSTANTIATED. Dead code.

**Fix — 3 files, ~80 lines:**

### 1.1 CockpitScreenContent.kt (commonMain)
- Add `spatialController: SpatialViewportController?` parameter (nullable = optional)
- When `layoutMode in LayoutMode.SPATIAL_CAPABLE && spatialController != null`:
  - Wrap LayoutEngine output inside `SpatialCanvas(controller)`
- When not spatial: render LayoutEngine directly (current behavior)
- Add `displayProfile` parameter for LayoutModeResolver filtering

### 1.2 CockpitScreen.kt (androidMain)
- Create `AndroidSpatialOrientationSource` via `remember { }`
- Create `SpatialViewportController` via `remember { }` with screen dimensions
- Wire: `LaunchedEffect { controller.connectToSource(source, this) }`
- Cleanup: `DisposableEffect { onDispose { controller.disconnect() } }`
- Pass controller to CockpitScreenContent

### 1.3 LayoutModeResolver Integration
- In CockpitScreenContent: filter CommandBar layout picker via `LayoutModeResolver.availableModes(displayProfile)`
- Use `LayoutModeResolver.defaultMode(displayProfile)` when creating new sessions
- Enforce `LayoutModeResolver.maxFrames(mode, profile)` in add-frame logic
- Pass displayProfile from `AvanueTheme.displayProfile` in CockpitScreen

---

## Item 2: WorkflowSidebar

**Problem:** WORKFLOW layout renders frames as vertical column but has no step navigation sidebar.

**Design — 1 new file (~250 lines):**

### 2.1 WorkflowSidebar.kt (commonMain/ui/)
- **Tablet/Desktop:** 30/70 horizontal split
  - Left 30%: Numbered step list (clickable, current step highlighted)
  - Right 70%: Active frame content via FrameWindow
- **Phone:** Bottom sheet overlay
  - Main area: Active frame content
  - Collapsed bottom sheet: Step indicator dots
  - Expanded bottom sheet: Full step list
- Step states: `PENDING, ACTIVE, COMPLETED` (visual indicators via AvanueTheme colors)
- `onStepSelected: (Int) -> Unit` callback to CockpitScreenContent

### 2.2 Wire into LayoutEngine
- When `layoutMode == WORKFLOW`: delegate to `WorkflowSidebar` instead of current `WorkflowLayout`
- WorkflowSidebar internally renders the active frame using the same `frameContent` lambda

### 2.3 Wire WorkflowStep Model
- CockpitViewModel: load/save WorkflowSteps via repository
- CockpitScreenContent: pass workflowSteps state down
- WorkflowSidebar: display step titles from model, not just frame index

---

## Item 3: Cursor IMU Fix

**Problem:** VoiceCursor renders and centers correctly but doesn't move with head motion. Analysis doc exists: `Docs/analysis/VoiceCursor/VoiceCursor-Analysis-IMUCursorMotionRegression-260215-V1.md`

**Investigation Pipeline:**
```
IMUManager.onSensorChanged
  → processRotationVector
  → _orientationFlow
  → HeadTrackingBridge.toCursorInputFlow
  → CursorController.connectInputFlow
  → update → _state
```

**Likely Culprits (from analysis):**
1. IMU sensors not starting (60%) — `startTracking()` may not be called or returns false
2. Flow scope cancelled (20%) — coroutine scope may be dead before flow starts collecting
3. Sensor accuracy blocking (10%) — accuracy filter may block initial readings

**Approach:**
1. Add diagnostic logging at each pipeline stage
2. Verify `IMUManager.startTracking()` returns true
3. Verify `_orientationFlow` emits values (collect in test scope)
4. Verify `HeadTrackingBridge` transforms correctly
5. Verify `CursorController.connectInputFlow` is called with active scope
6. Fix root cause, remove diagnostic logging

**Estimated:** 2-4 files modified, ~30-60 lines

---

## Item 4: VOS v3.0 Compact Format

**Problem:** VOS files are JSON (~138 KB each x 5 locales). Plan exists for pipe-delimited v3.0 format with ~60% size reduction.

**Plan doc:** `docs/plans/VoiceOSCore/VoiceOSCore-Plan-VOSCompactFormat-260216-V1.md`

**Implementation — ~300 lines across 3-4 files:**

### 4.1 VosParser.kt — Add v3.0 parsing
- Auto-detect: first char `{` = v2.1 JSON, `#` or `VOS:` = v3.0 compact
- Parse header: `VOS:3.0:en-US:en-US:app`
- Parse commands: `nav_back|go back|navigate back,back,previous screen|Navigate to previous screen`
- Compile category_map and action_map as Kotlin constants (locale-independent)

### 4.2 VosExporter.kt — Add v3.0 export
- Convert VosFile model to pipe-delimited text
- Write header + section markers + commands

### 4.3 Seed Files — Convert existing .vos files
- Convert 5 locale files from v2.1 JSON to v3.0 compact
- Verify round-trip: parse v3.0 → export v3.0 → compare

### 4.4 Tests
- Unit test: parse v3.0 file, verify command count matches v2.1
- Unit test: round-trip v2.1 → v3.0 → parse → verify identical

---

## Item 5: 3rd-party App Support (ActivityEmbedding)

**Problem:** Users want to run 3rd-party apps (Chrome, Maps, etc.) inside Cockpit frames alongside internal content.

**Research + Prototype — future phase:**

### 5.1 Research
- Android 12L+ ActivityEmbedding API capabilities and limitations
- `SplitPairRule`, `SplitPlaceholderRule`, `ActivityFilter`
- WindowManager library `1.3.0+` requirements
- Restrictions: same task, same UID or declared split

### 5.2 Model Extension
- Add `FrameContent.ExternalApp(packageName, activityName, intentExtras)` sealed subclass
- Add to ContentAccent mapping

### 5.3 Prototype
- Use `ActivityEmbedding` to embed a known app (Chrome) in a Cockpit frame
- Detect `isInMultiWindowMode()` → adjust LayoutModeResolver constraints
- Test: launch Chrome in split-left, internal PDF in split-right

### 5.4 Limitations to Document
- Not all apps support embedding
- Same-task restriction limits concurrent activities
- Performance overhead of multiple embedded activities

**Estimated:** Research phase only in this session. Implementation deferred.

---

## Execution Order (YOLO)

| Phase | Item | Est. Lines | Dependencies |
|-------|------|-----------|--------------|
| A | Wire Spatial Canvas + LayoutModeResolver | ~80 | None |
| B | WorkflowSidebar | ~250 | Item 1 (Cockpit context) |
| C | Cursor IMU Fix | ~30-60 | None (different module) |
| D | VOS Compact Format | ~300 | None (different module) |
| E | 3rd-party App Research | ~0 code | Items 1-2 (Cockpit stable) |

**Total estimated:** ~660-690 lines of code + research

---

## Commit Strategy

| Phase | Commit Message |
|-------|---------------|
| A | `feat(cockpit): Wire SpatialCanvas and LayoutModeResolver into CockpitScreenContent` |
| B | `feat(cockpit): Add WorkflowSidebar with step navigation and phone bottom-sheet` |
| C | `fix(voicecursor): Fix IMU head-tracking pipeline for cursor motion` |
| D | `feat(vos): Implement VOS v3.0 compact format parser and exporter` |
| E | `docs(cockpit): ActivityEmbedding research for 3rd-party app support` |
