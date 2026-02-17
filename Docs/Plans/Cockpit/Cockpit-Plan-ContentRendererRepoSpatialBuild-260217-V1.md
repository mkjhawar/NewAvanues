# Cockpit Plan: ContentRenderer + Repository + Spatial Polish + Build & Test

**Module:** `Modules/Cockpit/`
**Branch:** `Cockpit-Development`
**Date:** 2026-02-17
**Status:** IN PROGRESS
**Flags:** `.cot .yolo`

---

## Overview

Platforms: Android + Desktop (JVM)
Swarm Recommended: No (sequential deps, single module)
Estimated: 22 tasks, 4 phases
KMP Functionality Score: ~75% shared

---

## CoT: Phase Ordering Rationale

```
Foundation first → Content second → Polish third → Verify last.

1. Repository + ViewModel KMP MUST come first because:
   - CockpitViewModel is in androidMain but has ZERO Android dependencies
   - Moving it to commonMain unblocks Desktop target
   - DesktopCockpitRepository needed for Desktop build
   - 3 unwired DB tables (PinnedFrame, CrossFrameLink, TimelineEvent) are schema-ready
   - importSession() stub blocks session sharing workflow

2. ContentRenderer wiring depends on stable repository because:
   - New content composables may need content-state persistence
   - ViewModel operations (addFrame, updateContentState) used by renderers

3. Spatial polish is independent but benefits from stable module:
   - updateScreenSize() no-op needs real implementation
   - Edge indicators use FrameWindow position data

4. Build & test MUST come last:
   - Validates all prior work compiles
   - Unit tests cover new functionality
```

---

## Phase 1: Repository + ViewModel KMP Lift

### Task 1.1: Move CockpitViewModel to commonMain

**Current:** `src/androidMain/.../viewmodel/CockpitViewModel.kt`
**Target:** `src/commonMain/.../viewmodel/CockpitViewModel.kt`

The ViewModel is a plain Kotlin class using only:
- `ICockpitRepository` (commonMain interface)
- `CoroutineScope`, `Dispatchers.Main`, `SupervisorJob` (all KMP)
- `kotlinx.datetime.Clock` (KMP)
- `MutableStateFlow`/`StateFlow` (KMP)

Zero Android imports. Direct move with no code changes.

### Task 1.2: Create DesktopCockpitRepository

**File:** `src/desktopMain/.../repository/DesktopCockpitRepository.kt` (NEW)

Mirror of `AndroidCockpitRepository` using the same `VoiceOSDatabase` (SQLDelight generates cross-platform code). The Desktop SQLDelight driver (`JdbcSqliteDriver`) is already a dependency in `build.gradle.kts`.

### Task 1.3: Add PinnedFrame to ICockpitRepository

**Schema exists:** `CockpitPinnedFrame.sq` — frame PiP persistence
**Add to interface:** `getPinnedFrames()`, `pinFrame()`, `unpinFrame()`
**Add to both impls:** Android + Desktop repositories

### Task 1.4: Add CrossFrameLink to ICockpitRepository

**Schema exists:** `CockpitCrossFrameLink.sq` — inter-frame action triggers
**Add to interface:** `getCrossFrameLinks(sessionId)`, `saveCrossFrameLink()`, `deleteCrossFrameLink()`
**Add to both impls:** Android + Desktop repositories

### Task 1.5: Add TimelineEvent to ICockpitRepository

**Schema exists:** `CockpitTimelineEvent.sq` — activity audit log
**Add to interface:** `getTimelineEvents(sessionId, limit)`, `logEvent()`, `clearOldEvents()`
**Add to both impls:** Android + Desktop repositories

### Task 1.6: Implement importSession()

**Current:** Returns `null` (stub comment)
**Implement:** Parse JSON from `exportSession()` format → create new session + frames with fresh IDs → save to DB → return new session

---

## Phase 2: ContentRenderer Wiring (5 Placeholder Types)

### Content Renderer Strategy

| Type | Composable Location | Rendering Approach |
|------|--------------------|--------------------|
| Map | commonMain (WebView wrapper in androidMain) | OpenStreetMap via WebView tile URL — no API key |
| Terminal | commonMain (`TerminalContent.kt`) | Monospace LazyColumn with auto-scroll |
| Form | commonMain (`FormContent.kt`) | Dynamic checklist + text fields from JSON schema |
| AiSummary | commonMain (`AiSummaryContent.kt`) | Summary display + source frame chips + type selector |
| Widget | commonMain (`WidgetContent.kt`) | 8 widget types: Clock, Timer, Stopwatch, Compass, etc. |

### Task 2.1: MapContent (androidMain — WebView-based)

**File:** `ContentRenderer.kt` inline (same pattern as `WebContentRenderer`)

Replace `PlaceholderContent("Map", ...)` with:
- WebView loading `https://www.openstreetmap.org/export/embed.html?bbox={lon-delta},{lat-delta},{lon+delta},{lat+delta}&layer=mapnik`
- Computed from `content.latitude`, `content.longitude`, `content.zoomLevel`
- `content.mapType` switches between `mapnik` (standard) and `cyclemap` (terrain)
- Content state callback reports pan/zoom changes
- No API key required — OpenStreetMap is free

### Task 2.2: TerminalContent (commonMain)

**File:** `src/commonMain/.../ui/TerminalContent.kt` (NEW)

Cross-platform composable:
- Monospace font (`FontFamily.Monospace`)
- `LazyColumn` with text items (split by newline)
- Auto-scroll to bottom when `content.autoScroll == true` via `LaunchedEffect`
- Configurable `fontSize` from `content.fontSize`
- Dark background (`colors.surface`) with green/white text
- Line numbers (optional, toggled by tap)
- Selection support via `SelectionContainer`
- Max lines: `content.maxLines` (truncate from top when exceeded)

### Task 2.3: FormContent (commonMain)

**File:** `src/commonMain/.../ui/FormContent.kt` (NEW)

Cross-platform composable:
- Parse `content.fieldsJson` as JSON array of form field objects
- Field types: `checkbox` (toggle), `text` (input), `number` (numeric input), `select` (dropdown)
- Each field: label + input control + validation state
- Progress bar showing `completedCount / totalCount`
- "Add Field" button at bottom
- Auto-save via `onContentStateChanged` callback
- AvanueTheme colors throughout

Field JSON schema:
```json
[
  {"type": "checkbox", "label": "Step completed", "value": true},
  {"type": "text", "label": "Notes", "value": "some text"},
  {"type": "number", "label": "Quantity", "value": 5},
  {"type": "select", "label": "Priority", "value": "high", "options": ["low","medium","high"]}
]
```

### Task 2.4: AiSummaryContent (commonMain)

**File:** `src/commonMain/.../ui/AiSummaryContent.kt` (NEW)

Cross-platform composable:
- Header: "AI Summary" + SummaryType chip selector (BRIEF/DETAILED/ACTION_ITEMS/QA)
- Source frame chips: show IDs from `content.sourceFrameIds` with "Add Source" chip
- Summary text display: `content.summary` in scrollable Text
- "Generate Summary" AvanueButton (calls `onGenerateSummary` callback)
- Auto-refresh indicator when `content.autoRefresh == true`
- Last refreshed timestamp: `content.lastRefreshedAt`
- Empty state: "Select source frames and tap Generate to create a summary"

Note: Actual LLM generation is deferred to a future AI module integration. The UI is complete and functional — it displays summaries and provides the interaction surface.

### Task 2.5: WidgetContent (commonMain)

**File:** `src/commonMain/.../ui/WidgetContent.kt` (NEW)

Cross-platform composable dispatching to 8 widget types:

| WidgetType | Display | Data Source |
|------------|---------|-------------|
| CLOCK | Large digital clock + date | `kotlinx.datetime.Clock.System` |
| TIMER | Countdown timer with start/stop/reset | In-memory coroutine countdown |
| STOPWATCH | Elapsed time with lap support | In-memory coroutine counter |
| COMPASS | Compass rose with degree heading | IMU yaw (android) / static (desktop) |
| BATTERY | Battery percentage + charging indicator | Placeholder text (platform API needed) |
| CONNECTION_STATUS | WiFi/cellular/offline status | Placeholder text (platform API needed) |
| WEATHER | Temperature + conditions | Placeholder text (API needed) |
| GPS_COORDINATES | Lat/lon with precision | Placeholder text (location API needed) |

Clock, Timer, Stopwatch are fully functional (pure Kotlin time APIs).
Compass, Battery, Connection, Weather, GPS show current-value text with "Data unavailable on this platform" fallback — not PlaceholderContent, but real widgets with graceful degradation.

### Task 2.6: Wire all 5 in ContentRenderer

**File:** `ContentRenderer.kt` (androidMain)

Replace each `PlaceholderContent(...)` call with the appropriate composable:
- `is FrameContent.Map` → `MapContentRenderer(content, onContentStateChanged)` (inline WebView)
- `is FrameContent.Terminal` → `TerminalContent(content)`
- `is FrameContent.Form` → `FormContent(content, onContentStateChanged)`
- `is FrameContent.AiSummary` → `AiSummaryContent(content, onContentStateChanged)`
- `is FrameContent.Widget` → `WidgetContent(content)`

---

## Phase 3: Spatial Canvas Polish

### Task 3.1: Fix updateScreenSize() no-op

**File:** `SpatialViewportController.kt`

Current: empty body with comment.
Fix: Store screenWidthPx/screenHeightPx as `var` (currently `val`), update them in `updateScreenSize()`. The next orientation update naturally uses the new dimensions since `connectToSource` references `screenWidthPx/screenHeightPx` directly.

```kotlin
fun updateScreenSize(widthPx: Float, heightPx: Float) {
    screenWidthPx = widthPx
    screenHeightPx = heightPx
}
```

### Task 3.2: Add viewport bounds clamping

**File:** `SpatialViewportController.kt`

Prevent panning beyond content area:
- Add `maxOffsetX` and `maxOffsetY` computed from screen size and content bounds
- Clamp `targetOffset` within `[-maxOffset, +maxOffset]` before lerp
- Default max = 1.5x screen size (allows panning 50% beyond edges)
- `CockpitConstants.SPATIAL_MAX_OFFSET_RATIO = 1.5f`

### Task 3.3: Add edge indicators

**File:** `SpatialCanvas.kt`

Visual arrows showing content exists off-screen:
- 4 edge indicators (top/bottom/left/right)
- Show when `viewportOffset` exceeds threshold in that direction
- Animated fade-in/out via `animateFloatAsState`
- Small arrow icons (`Icons.Default.KeyboardArrowUp/Down/Left/Right`)
- Semi-transparent overlay: `colors.primary.copy(alpha = 0.4f)`
- Only visible when spatial panning is active (not locked)

### Task 3.4: Add configurable sensitivity constants

**File:** `CockpitConstants.kt`

Add tuning constants (keeping existing values as defaults):
```kotlin
/** Sensitivity presets for user preference */
const val SPATIAL_SENSITIVITY_LOW = 45f    // 45 degrees per screen width
const val SPATIAL_SENSITIVITY_MEDIUM = 30f // current default
const val SPATIAL_SENSITIVITY_HIGH = 15f   // very responsive
```

Update `SpatialViewportController` to accept sensitivity as constructor param with default.

---

## Phase 4: Build & Test

### Task 4.1: Verify Android target compiles

Run `./gradlew :Modules:Cockpit:assembleDebug` and fix any compilation errors.

### Task 4.2: Verify Desktop target compiles

Run `./gradlew :Modules:Cockpit:desktopJar` (or equivalent JVM compile task) and fix any compilation errors.

### Task 4.3: Add unit tests for models + ViewModel

**File:** `src/commonTest/.../CockpitModelTest.kt` (NEW)
**File:** `src/commonTest/.../CockpitViewModelTest.kt` (NEW)

Tests:
- FrameContent serialization round-trip (all 17 types)
- LayoutModeResolver: defaultMode, isAvailable, maxFrames per DisplayProfile
- ContentAccent mapping: all type IDs resolve correctly
- CockpitSession utility: frameByNumber, frameById
- PanelRole: 2-panel vs 3-panel detection logic

### Task 4.4: Add unit tests for SpatialViewportController

**File:** `src/commonTest/.../SpatialViewportControllerTest.kt` (NEW)

Tests:
- Deadzone: input within ±5° produces zero offset
- Sensitivity: 30° input maps to 1 screen width
- Lock: locked state freezes viewport
- CenterView: snaps to Offset.Zero
- ManualOffset: accumulates deltas correctly
- Bounds clamping: offset stays within max ratio

---

## File Summary

| # | File | Source Set | Phase | Change |
|---|------|-----------|-------|--------|
| 1 | `CockpitViewModel.kt` | commonMain | 1 | MOVE from androidMain |
| 2 | `DesktopCockpitRepository.kt` | desktopMain | 1 | NEW — JVM SQLite impl |
| 3 | `ICockpitRepository.kt` | commonMain | 1 | ADD PinnedFrame, CrossFrameLink, TimelineEvent methods |
| 4 | `AndroidCockpitRepository.kt` | androidMain | 1 | ADD PinnedFrame, CrossFrameLink, TimelineEvent, importSession impls |
| 5 | `TerminalContent.kt` | commonMain | 2 | NEW — monospace log viewer |
| 6 | `FormContent.kt` | commonMain | 2 | NEW — dynamic checklist/form |
| 7 | `AiSummaryContent.kt` | commonMain | 2 | NEW — summary display + controls |
| 8 | `WidgetContent.kt` | commonMain | 2 | NEW — 8 widget types |
| 9 | `ContentRenderer.kt` | androidMain | 2 | REPLACE 5 PlaceholderContent → real renderers |
| 10 | `SpatialViewportController.kt` | commonMain | 3 | FIX updateScreenSize + ADD bounds clamping |
| 11 | `SpatialCanvas.kt` | commonMain | 3 | ADD edge indicators |
| 12 | `CockpitConstants.kt` | commonMain | 3 | ADD sensitivity presets, max offset ratio |
| 13 | `CockpitModelTest.kt` | commonTest | 4 | NEW — model + resolver tests |
| 14 | `CockpitViewModelTest.kt` | commonTest | 4 | NEW — ViewModel state tests |
| 15 | `SpatialViewportControllerTest.kt` | commonTest | 4 | NEW — spatial pipeline tests |

**KMP Functionality Score: 12/15 areas in commonMain (80% shared)**
- Platform-specific: MapContentRenderer (WebView), AndroidCockpitRepository queries, DesktopCockpitRepository queries

---

## Verification Checklist

- [ ] CockpitViewModel compiles from commonMain (no Android imports)
- [ ] DesktopCockpitRepository passes same interface contract as Android
- [ ] 3 new repo methods work: PinnedFrame, CrossFrameLink, TimelineEvent
- [ ] importSession() round-trips with exportSession()
- [ ] Map shows OpenStreetMap tiles at correct coordinates
- [ ] Terminal displays monospace text with auto-scroll
- [ ] Form renders checklist fields and persists state
- [ ] AiSummary shows type selector and source frame chips
- [ ] Widget displays clock/timer/stopwatch correctly
- [ ] updateScreenSize() updates controller dimensions
- [ ] Edge indicators appear when content is off-screen
- [ ] Viewport clamps within bounds
- [ ] `assembleDebug` succeeds (Android)
- [ ] Desktop JVM task succeeds
- [ ] All unit tests pass

---

*Cockpit-Plan-ContentRendererRepoSpatialBuild-260217-V1*
