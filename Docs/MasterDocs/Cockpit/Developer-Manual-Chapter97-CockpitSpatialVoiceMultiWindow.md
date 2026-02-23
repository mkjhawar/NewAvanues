# Developer Manual — Chapter 97: Cockpit SpatialVoice Multi-Window System

**Module:** `Modules/Cockpit/`
**Package:** `com.augmentalis.cockpit`
**Platform:** KMP (Android + Desktop, iOS deferred)
**Branch:** `IosVoiceOS-Development`
**Date:** 2026-02-17 (updated 2026-02-17 — Traffic Lights, Multi-Pane Workflow, ExternalApp)

---

## 1. Overview

The Cockpit is a multi-window content management system that renders multiple content frames (web pages, PDFs, images, videos, notes, camera feeds, etc.) in configurable layout modes. It follows a **KMP-first** architecture where ~80% of code lives in `commonMain` (Compose Multiplatform), with only platform-specific implementations (IMU sensors, Android content renderers) in `androidMain`/`desktopMain`.

The UI follows the **SpatialVoice** design language with full **AvanueUI v5.1** tokenization — all 32 theme combinations (4 palettes x 4 styles x light/dark) are supported.

### Key Capabilities

- **13 layout modes** — from freeform drag-resize to 3D carousel to spatial dice
- **17 content types** — across 3 priority tiers (P0 core, P1 extended, P2 advanced) + ExternalApp
- **Hierarchical command bar** — replaces dropdown menus with animated chip state machine
- **Pseudo-spatial head-tracking** — IMU-driven viewport panning for spatial layouts
- **Device-adaptive layout resolution** — phone/tablet/glass constraints enforced automatically

---

## 2. Architecture

```
┌──────────────────────────────────────────────────────┐
│  CockpitScreen (androidMain thin wrapper, 54 lines)  │
├──────────────────────────────────────────────────────┤
│  CockpitScreenContent (commonMain, KMP shell)        │
│  ┌─ TopAppBar ─── LayoutEngine ─── CommandBar ──┐    │
│  │  SpatialVoice gradient background             │    │
├──┴───────────────────────────────────────────────┴───┤
│  LayoutEngine (router) → 13 Layout Composables       │
├──────────────────────────────────────────────────────┤
│  FrameWindow (container) + ContentRenderer (platform)│
├──────────────────────────────────────────────────────┤
│  Models: CockpitFrame, CockpitSession, FrameContent  │
│  CommandBarState, ContentAccent, SpatialPosition      │
├──────────────────────────────────────────────────────┤
│  ICockpitRepository (SQLDelight persistence)         │
│  CockpitViewModel (state management)                 │
├──────────────────────────────────────────────────────┤
│  SpatialViewportController + ISpatialOrientationSource│
└──────────────────────────────────────────────────────┘
```

### Source Set Organization

| Source Set | Contents |
|-----------|---------|
| `commonMain` | Models (CockpitFrame, PanelRole, FrameContent.ExternalApp), constants, all layout composables, LayoutEngine, CommandBar, CockpitScreenContent, SpatialViewportController, ISpatialOrientationSource, LayoutModeResolver, ICockpitRepository, IExternalAppResolver, ExternalAppContent |
| `androidMain` | CockpitScreen (thin wrapper), ContentRenderer (AndroidView + ExternalApp wiring), AndroidSpatialOrientationSource (IMU), AndroidExternalAppResolver (PackageManager + Intent) |
| `desktopMain` | DesktopSpatialOrientationSource (manual input fallback), DesktopExternalAppResolver (stub) |

### Build Configuration

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)              // JB Compose Multiplatform
    alias(libs.plugins.kotlin.serialization)
}
```

CommonMain dependencies include `compose.runtime`, `compose.foundation`, `compose.material3`, `compose.materialIconsExtended`, and `project(":Modules:AvanueUI")`. Platform-specific content modules (WebAvanue, PDFAvanue, etc.) remain in `androidMain`.

---

## 3. Model Layer

### CockpitSession

```kotlin
data class CockpitSession(
    val id: String,
    val name: String,
    val layoutMode: LayoutMode,
    val frames: List<CockpitFrame>,
    val selectedFrameId: String?,
    val backgroundUri: String?,           // Custom background
    val visibleFrameCount: Int
)
```

Utility: `frameByNumber(n)`, `frameById(id)`, `numberedFrames` (indexed 1..N).

### CockpitFrame

```kotlin
data class CockpitFrame(
    val id: String,
    val sessionId: String,
    val title: String,
    val content: FrameContent,
    val state: FrameState,
    val spatialPosition: SpatialPosition,  // 3x3 grid position
    val panelRole: PanelRole,              // STEPS, CONTENT, or AUXILIARY
    val contentType: String,
    val accent: ContentAccent,             // Semantic border color
    val isSpatiallyLocked: Boolean         // Pin in spatial viewport
)
```

#### PanelRole (Workflow Multi-Pane)

```kotlin
enum class PanelRole {
    STEPS,      // Left panel — step navigation list
    CONTENT,    // Center panel — main content (default)
    AUXILIARY   // Right panel — supporting content (video call, chat, etc.)
}
```

When any frame has `panelRole == AUXILIARY`, the workflow layout switches from 2-panel to 3-panel mode automatically.

### FrameContent (17 Types)

```
sealed class FrameContent
├── P0 Core (6): Web, Pdf, Image, Video, Note, Camera
├── P1 Extended (7): VoiceNote, Form, Signature, Voice, Map, Whiteboard, Terminal
├── P2 Advanced (3): AiSummary, ScreenCast, Widget
└── Integration (1): ExternalApp
```

Each variant carries content-specific state (URL, page number, zoom level, etc.) and serializes to JSON for DB persistence.

### LayoutMode (14 Modes)

| Mode | Description | Best For |
|------|------------|---------|
| `DASHBOARD` | Launcher/home with module tiles and recent sessions | Default when no session active |
| `FREEFORM` | Drag-and-resize windows | Custom arrangement |
| `GRID` | Responsive column grid | General multi-content |
| `SPLIT_LEFT` | Primary 60% left | Side-by-side comparison |
| `SPLIT_RIGHT` | Primary 60% right | Mirror of SPLIT_LEFT |
| `COCKPIT` | Flight-deck instrument panel | Default for active sessions |
| `T_PANEL` | Primary 60% top | Presentation + notes |
| `MOSAIC` | Primary 50% left + grid right | Media-heavy layouts |
| `FULLSCREEN` | Single frame | Voice-controlled switching |
| `WORKFLOW` | Vertical numbered steps | Checklists, procedures |
| `ROW` | Horizontal equal-width | Timeline, sequential view |
| `CAROUSEL` | 3D perspective HorizontalPager | Default for phones |
| `SPATIAL_DICE` | 4 corners + 1 center (dice-5) | Spatial grid (tablet only) |
| `GALLERY` | Filtered media grid | Image/video browsing |

**Constants:**

- `DEFAULT` = `COCKPIT` — default layout for active sessions with frames
- `SPATIAL_CAPABLE` set: `FREEFORM, COCKPIT, MOSAIC, T_PANEL, SPATIAL_DICE` — layouts that support spatial viewport panning
- `FRAME_LAYOUTS`: All modes except `DASHBOARD` — modes that render frames
- `GALLERY_CONTENT_TYPES`: Content types shown in Gallery mode

### ContentAccent

Maps content types to semantic AvanueTheme color roles:

| Accent | Color Role | Content Types |
|--------|-----------|---------------|
| `PRIMARY` | `AvanueTheme.colors.primary` | Web |
| `SECONDARY` | `AvanueTheme.colors.secondary` | PDF |
| `TERTIARY` | `AvanueTheme.colors.tertiary` | Image |
| `INFO` | `AvanueTheme.colors.info` | Note, VoiceNote |
| `SUCCESS` | `AvanueTheme.colors.success` | Camera |
| `WARNING` | `AvanueTheme.colors.warning` | Video |
| `ERROR` | `AvanueTheme.colors.error` | Terminal |

**ExternalApp** uses `INFO` accent (informational blue).

### SpatialPosition

3x3 grid for spatial layouts:

```
TOP_LEFT    | ABOVE       | TOP_RIGHT
LEFT        | CENTER      | RIGHT
BOTTOM_LEFT | BELOW       | BOTTOM_RIGHT
```

Each position has a `label` for voice commands ("top left", "center", etc.).

### CommandBarState (State Machine)

```
MAIN (root)
├── ADD_FRAME         → content type chips
├── LAYOUT_PICKER     → 13 layout mode chips
├── FRAME_ACTIONS
│   ├── WEB_ACTIONS   → Back, Forward, Refresh, ZoomIn, ZoomOut
│   ├── PDF_ACTIONS   → PrevPage, NextPage, ZoomIn, ZoomOut
│   ├── IMAGE_ACTIONS → ZoomIn, ZoomOut, Rotate
│   ├── VIDEO_ACTIONS → Rewind, Play/Pause, Fullscreen
│   ├── NOTE_ACTIONS  → Undo, Redo
│   └── CAMERA_ACTIONS→ Flip, Capture
├── SCROLL_COMMANDS   → (reserved)
├── ZOOM_COMMANDS     → (reserved)
└── SPATIAL_COMMANDS  → (reserved)
```

Properties: `parent` (back navigation), `isContentSpecific`, `depth` (nesting level).

`CommandBarState.forContentType(typeId)` auto-selects the right state when a frame gains focus.

---

## 4. UI Layer

### CockpitScreenContent (commonMain)

The KMP screen shell providing:

1. **SpatialVoice gradient background** — `verticalGradient(background, surface.copy(0.6f), background)`
2. **TopAppBar** — transparent container, back button, session title
3. **LayoutEngine** — routes to the active layout composable
4. **Status bar** — frame count + current layout mode label
5. **CommandBar** — bottom-docked hierarchical chip bar

Takes a `frameContent: @Composable (CockpitFrame) -> Unit` lambda slot for platform-specific content rendering (ContentRenderer on Android).

### LayoutEngine (commonMain, 727 lines)

Routes the active `LayoutMode` to the corresponding layout composable. All 13 layouts are implemented inline or delegated to dedicated files.

**Callbacks passed through:**

```kotlin
onFrameSelected: (String) -> Unit
onFrameMoved: (String, Float, Float) -> Unit
onFrameResized: (String, Float, Float) -> Unit
onFrameClose: (String) -> Unit
onFrameMinimize: (String) -> Unit
onFrameMaximize: (String) -> Unit
```

**Visibility filtering:** Only frames where `state.isVisible && !state.isMinimized` are rendered.

**WORKFLOW mode** delegates to `WorkflowSidebar` (see below) instead of inline layout.

### CommandBar (commonMain, 313 lines)

Bottom-docked bar with animated chip transitions:

- Horizontal scrollable row of `CommandChip` composables
- State transitions use slide + fade animation
- Back chip appears when `state.depth > 0`
- Layout mode chips show icons via `layoutModeIcon()` mapping
- Content-specific chips appear based on selected frame's content type

### Specialized Layouts

| Layout | File | Key Feature |
|--------|------|------------|
| `CarouselLayout` | `CarouselLayout.kt` (148 lines) | `HorizontalPager` + `graphicsLayer` 3D perspective: center scale 1.0, adjacent 0.8, rotationY ±15deg, alpha 1.0/0.6 |
| `SpatialDiceLayout` | `SpatialDiceLayout.kt` (164 lines) | Dice-5 pattern: center 55% area, 4 corners ~11% each |
| `GalleryLayout` | `GalleryLayout.kt` (102 lines) | `LazyVerticalGrid` filtered to media content types, responsive columns |
| `FreeformCanvas` | `FreeformCanvas.kt` | Drag/resize with snap-to-edge (12dp threshold), z-order management |
| `FrameWindow` | `FrameWindow.kt` | Title bar with traffic lights (red/yellow/green), content-type icon, themed accent border, frame number badge, spatial lock indicator |
| `WorkflowSidebar` | `WorkflowSidebar.kt` (~710 lines) | Multi-pane workflow with 2-panel (30/70) and 3-panel (20/60/20) modes |
| `ExternalAppContent` | `ExternalAppContent.kt` | Cross-platform composable for external app status display and launch |

### FrameWindow — Traffic Light Controls

The window chrome uses macOS-style traffic light dots instead of traditional icon buttons:

```
┌─ [●][●][●] ──── Step 1 ── Web ────── Frame Title ──── ← ──┐
│  red yellow green                                          │
│                                                            │
│  [Content area]                                            │
│                                                            │
└────────────────────────────────────────────── [⊟ resize] ──┘
```

**Traffic Lights** (`TrafficLights` composable):
- **Red** (leftmost, `colors.error`): Close
- **Yellow** (`colors.warning`): Minimize
- **Green** (`colors.success`): Maximize/Restore
- Each dot: 12dp circle, 4dp spacing, 24dp touch target
- On hover (desktop) or press (mobile): icon fades in via `animateFloatAsState`
- Hover detection: `PointerEventType.Enter/Exit` (KMP-compatible, gracefully degrades on mobile)

### WorkflowSidebar (commonMain, ~710 lines)

Multi-pane workflow with adaptive layout based on `PanelRole` composition.

**2-panel mode** (no `AUXILIARY` frames — backward compatible):

**Tablet/Desktop — 30/70 split:**
```
┌──────────┬────────────────────────┐
│  Steps   │                        │
│  1. ●    │   Active Frame         │
│  2. ○    │   (FrameWindow)        │
│  3. ○    │                        │
└──────────┴────────────────────────┘
```

**Phone — Bottom sheet:**
```
┌────────────────────────────┐
│   Active Frame (full screen) │
├────────────────────────────┤
│ Step 2 of 5  ● ● ○ ○ ○    │ ← collapsed peek (56dp)
│ [Expandable step list]     │ ← expanded: full step rows
└────────────────────────────┘
```

**3-panel mode** (when any frame has `panelRole == AUXILIARY`):

**Tablet/Desktop — 20/60/20 split:**
```
┌──────────┬────────────────────────┬──────────┐
│  Steps   │    Main Content        │ Auxiliary │
│  (20%)   │    (60%)               │  (20%)   │
│  1. ●    │    [Pictures]          │  Video   │
│  2. ○    │    [Instructions]      │  Call    │
│  3. ○    │                        │  Notes   │
└──────────┴────────────────────────┴──────────┘
```

**Phone — Tab navigation with HorizontalPager:**
```
┌─ [Steps] [Content] [Auxiliary] ──────────┐
│                                          │
│   ← Swipe between 3 tab pages →         │
│   (steps auto-navigate to content tab)   │
│                                          │
├──────────────────────────────────────────┤
│ Step 2 of 5  ● ● ○ ○ ○                  │
└──────────────────────────────────────────┘
```

**Key composables:**

| Composable | Purpose |
|------------|---------|
| `WorkflowSidebar` | Root — dispatches to 2-panel or 3-panel based on AUXILIARY presence and `DisplayProfile` |
| `WorkflowTabletLayout` | `Row` with 30% step panel + divider + 70% FrameWindow (2-panel) |
| `WorkflowTriPanelLayout` | `Row` with 20/60/20 weighted columns + dividers (3-panel) |
| `WorkflowPhoneLayout` | `BottomSheetScaffold` with step dots peek (2-panel phone) |
| `WorkflowPhoneTabLayout` | `TabRow` + `HorizontalPager` with 3 swipeable pages (3-panel phone) |
| `StepListPanel` | `LazyColumn` with header + `StepRow` items |
| `StepRow` | Number badge (circle) + title + state icon, animated colors |
| `StepIndicatorDots` | Compact dot row for collapsed bottom sheet (max 10 visible) |
| `VerticalDivider` | Thin 1dp divider between panels |

**Step states:** `PENDING` (gray, 0.4 alpha), `ACTIVE` (primary, highlighted border), `COMPLETED` (success/green, checkmark icon)

**Bug fix:** All `onClose`/`onMinimize`/`onMaximize` callbacks are now properly wired from `LayoutEngine` through to `FrameWindow`. Previously these were empty `{}` lambdas.

### LayoutModeResolver (commonMain, 137 lines)

Device-adaptive layout constraints:

```kotlin
object LayoutModeResolver {
    fun defaultMode(profile: DisplayProfile): LayoutMode
    fun isAvailable(mode: LayoutMode, profile: DisplayProfile): Boolean
    fun maxFrames(mode: LayoutMode, profile: DisplayProfile): Int
    fun availableModes(profile: DisplayProfile): List<LayoutMode>
}
```

**Defaults:** Phone → CAROUSEL, Tablet → COCKPIT, Glass → FULLSCREEN.

**Constraints:** SPATIAL_DICE tablet-only, FREEFORM max 4 on phone / 10 on tablet, GALLERY/CAROUSEL/FULLSCREEN allow up to 20 frames.

---

## 5. Spatial Canvas System

### Pipeline

```
IMU (Android) / Mouse+Keyboard (Desktop)
    ↓
ISpatialOrientationSource.orientationFlow
    ↓  Flow<SpatialOrientation(yawDegrees, pitchDegrees)>
SpatialViewportController
    ↓  Deadzone filter (±5 degrees)
    ↓  Degree → pixel mapping (30 degrees = 1 screen width)
    ↓  Lerp smoothing (factor 0.15)
    ↓
StateFlow<Offset> → SpatialCanvas graphicsLayer transform
```

### ISpatialOrientationSource (KMP Interface)

```kotlin
interface ISpatialOrientationSource {
    val orientationFlow: Flow<SpatialOrientation>
    fun startTracking(consumerId: String): Boolean
    fun stopTracking(consumerId: String)
    fun isTracking(): Boolean
}

data class SpatialOrientation(
    val yawDegrees: Float,
    val pitchDegrees: Float,
    val timestamp: Long
)
```

Multi-consumer safe via `consumerId` reference counting.

### Platform Implementations

| Platform | Class | Input Source |
|----------|-------|------------|
| Android | `AndroidSpatialOrientationSource` | `IMUPublicAPI` from DeviceManager — converts radians to degrees |
| Desktop | `DesktopSpatialOrientationSource` | Manual: `setOrientation()`, `nudge()`, `reset()` — for mouse drag / keyboard arrows |

### SpatialViewportController

```kotlin
class SpatialViewportController(screenWidthPx: Float, screenHeightPx: Float) {
    val viewportOffset: StateFlow<Offset>
    fun connectToSource(source: ISpatialOrientationSource, scope: CoroutineScope)
    fun disconnect()
    fun lock() / unlock() / toggleLock()
    fun centerView()
    fun applyManualOffset(dx: Float, dy: Float)
    fun updateScreenSize(width: Float, height: Float)
}
```

**Constants:** Deadzone = ±5deg, Sensitivity = 30deg/screen, Lerp factor = 0.15, Grid snap = 3x3.

### SpatialCanvas (Composable)

Wraps layout content with `graphicsLayer { translationX/Y = viewportOffset }`. Provides lock/center FAB controls. Only active when `layoutMode in LayoutMode.SPATIAL_CAPABLE`.

---

## 6. CockpitConstants Reference

| Constant | Value | Purpose |
|----------|-------|---------|
| `MAX_FRAMES` | 20 | Maximum frames per session |
| `MAX_SESSIONS` | 50 | Maximum saved sessions |
| `MIN_FRAME_WIDTH` | 120.dp | Minimum frame dimension |
| `MAX_FRAME_WIDTH` | 2000.dp | Maximum frame dimension |
| `COMMAND_BAR_HEIGHT` | 56.dp | Command bar height |
| `COMMAND_CHIP_HEIGHT` | 48.dp | Individual chip height |
| `SPATIAL_GRID_ROWS` | 3 | Spatial grid rows |
| `SPATIAL_GRID_COLS` | 3 | Spatial grid columns |
| `CAROUSEL_ADJACENT_SCALE` | 0.8f | Off-center carousel scale |
| `DICE_CENTER_WEIGHT` | 0.55f | Center frame area ratio |
| `DICE_CORNER_WEIGHT` | 0.45f / 4 | Per-corner area ratio |
| `SPATIAL_DEADZONE_DEGREES` | 5f | IMU deadzone |
| `SPATIAL_SENSITIVITY` | 30f | Degrees per screen width |
| `SPATIAL_LERP_FACTOR` | 0.15f | Smoothing factor |

---

## 7. Repository & Persistence

### ICockpitRepository (KMP Interface)

```kotlin
interface ICockpitRepository {
    // Sessions
    suspend fun getSessions(): List<CockpitSession>
    suspend fun getSession(id: String): CockpitSession?
    suspend fun saveSession(session: CockpitSession)
    suspend fun deleteSession(id: String)

    // Frames
    suspend fun getFrames(sessionId: String): List<CockpitFrame>
    suspend fun saveFrame(frame: CockpitFrame)
    suspend fun deleteFrame(frameId: String)
    suspend fun updateFrameContent(frameId: String, newContent: FrameContent)

    // Workflow
    suspend fun getWorkflowSteps(): List<WorkflowStep>
    suspend fun saveWorkflowStep(step: WorkflowStep)

    // Import/Export
    suspend fun exportSession(sessionId: String): String
    suspend fun importSession(json: String): CockpitSession
}

@Serializable
data class SessionExport(
    val session: CockpitSession,
    val frames: List<CockpitFrame>,
    val steps: List<WorkflowStep>
)
```

Export/import uses `SessionExport` for single-pass serialization (no double-encoding). Backed by SQLDelight. Content serialized as JSON. Timestamps in ISO 8601.

**260222 Fix:** `getSessions()` and `getSession(id)` now properly load `workflowSteps` from the database for each session. Previously, `workflowSteps` always returned an empty list, breaking workflow state reconstruction after app restart. Both Android and Desktop implementations have been updated.

### CockpitViewModel Lifecycle (v2.3)

- `createSession()` is `suspend` — awaits DB save before returning the session
- `updateFrameContent(frameId, FrameContent)` — typed updates via `copy()`, no raw JSON
- `dispose()` — cancels `viewModelJob` (SupervisorJob) and auto-save. Call on screen exit
- `setLayoutMode()` / `renameSession()` — propagate changes to `_sessions` list
- `save()` — syncs `selectedFrameId` into `activeSession` before persisting

---

## 8. App Integration

### Android Entry Points

**CockpitScreen.kt** (androidMain, ~90 lines) — creates spatial pipeline and delegates to KMP shell:

```kotlin
@Composable
fun CockpitScreen(viewModel: CockpitViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val displayProfile = AvanueTheme.displayProfile

    // Device-adaptive layout filtering
    val availableModes = remember(displayProfile) {
        LayoutModeResolver.availableModes(displayProfile)
    }

    // Spatial head-tracking pipeline
    val spatialSource = remember { AndroidSpatialOrientationSource(context) }
    val spatialController = remember(screenWidthPx, screenHeightPx) {
        SpatialViewportController(screenWidthPx, screenHeightPx)
    }

    // Lifecycle: connect on enter, disconnect on leave
    DisposableEffect(spatialSource, spatialController) {
        spatialController.connectToSource(spatialSource, scope)
        onDispose { spatialController.disconnect(spatialSource) }
    }

    CockpitScreenContent(
        // ... session state + callbacks ...
        spatialController = spatialController,
        availableLayoutModes = availableModes,
        frameContent = { frame -> ContentRenderer(frame) }
    )
}
```

**CockpitScreenContent** (commonMain) conditionally wraps `LayoutEngine` with `SpatialCanvas` when `layoutMode in LayoutMode.SPATIAL_CAPABLE && spatialController != null`. Non-spatial modes render `LayoutEngine` directly.

The `availableLayoutModes` parameter filters the CommandBar's layout picker to show only modes valid for the current device profile (e.g., phone users won't see SPATIAL_DICE).

**CockpitEntryViewModel** (apps/avanues) — entry point from Hub dashboard.

**SpatialOrbitHub** (apps/avanues) — hub integration with spatial orbit animation.

**HubModule** — Hilt module providing Cockpit dependencies to the app graph.

---

## 9. Theme Integration (AvanueUI v5.1)

### Mandatory Rules

1. **Colors:** `AvanueTheme.colors.*` — NEVER `MaterialTheme.colorScheme.*`
2. **Background:** `Brush.verticalGradient(background, surface.copy(0.6f), background)`
3. **TopAppBar:** `containerColor = Color.Transparent` or `AvanueTheme.colors.surface`
4. **Frame borders:** Resolved via `ContentAccent` enum → `AvanueTheme.colors.{role}`
5. **Appearance:** `AvanueTheme.isDark` for light/dark branching
6. **Components:** Use unified `AvanueCard`, `AvanueButton`, `AvanueChip` where applicable
7. **Touch targets:** `DisplayUtils.minTouchTarget` for device-adaptive sizing

### Color Usage Map

| UI Element | Token |
|-----------|-------|
| Screen background | `AvanueTheme.colors.background` |
| Frame surface | `AvanueTheme.colors.surface` |
| Frame border (selected) | `AvanueTheme.colors.primary` |
| Frame border (content) | `ContentAccent.resolve(AvanueTheme.colors)` |
| Title text | `AvanueTheme.colors.textPrimary` |
| CommandBar chips | `AvanueTheme.colors.primary` / `surface` |
| Status indicators | `AvanueTheme.colors.info` / `success` / `warning` |

---

## 10. Adding New Features

### Adding a New Layout Mode

1. Add enum value to `LayoutMode.kt`
2. Add layout composable in `commonMain/ui/` (or inline in LayoutEngine)
3. Wire in `LayoutEngine.kt` switch statement
4. Add availability rules in `LayoutModeResolver.kt`
5. Add chip icon in `CommandBar.layoutModeIcon()`
6. Add chip label in `CommandBar.layoutModeLabel()`
7. Update `CockpitConstants.kt` if new constants needed

### Adding a New Content Type

1. Add `FrameContent` subclass in `FrameContent.kt` + type constant + add to `ALL_TYPES`
2. Add `ContentAccent` mapping in `ContentAccent.forContentType()`
3. Add icon in `FrameWindow.contentTypeIcon()`
4. Add to `CommandBar.addFrameOptions()`
5. Add `CommandBarState` actions if content-specific controls needed
6. Add rendering in `ContentRenderer.kt` (androidMain)
7. Add content module dependency in `build.gradle.kts` androidMain

### Adding a New Platform

1. Create `{platform}Main/` source set in `build.gradle.kts`
2. Implement `ISpatialOrientationSource` for platform sensors
3. Create platform `CockpitScreen` wrapper providing `ContentRenderer`
4. Add SQLDelight driver dependency

---

## 11. 3rd-Party App Support (Implemented)

Android's security model prevents non-system apps from visually embedding 3rd-party activities. The Cockpit implements a practical approach:

| Scope | Approach | Status |
|-------|----------|--------|
| **Own modules** (NoteAvanue, PhotoAvanue, etc.) | Compose content slots | Implemented |
| **3rd-party apps** | `FLAG_ACTIVITY_LAUNCH_ADJACENT` + split-screen | **Implemented** |
| **Voice control across apps** | AccessibilityService overlay | Implemented |
| **True 3rd-party embedding** | TaskOrganizer / VirtualDisplay | Not viable (requires system UID) |

### Architecture (KMP Split)

```
commonMain:
  FrameContent.ExternalApp(packageName, activityName, label)
  IExternalAppResolver (interface)
  ExternalAppStatus (enum: NOT_INSTALLED, INSTALLED_NO_EMBED, EMBEDDABLE)
  ExternalAppContent (shared composable — status badge + launch button)

androidMain:
  AndroidExternalAppResolver — PackageManager + Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
  ContentRenderer — wires ExternalApp to ExternalAppContent + resolver

desktopMain:
  DesktopExternalAppResolver — stub (NOT_INSTALLED for all apps)
```

### IExternalAppResolver

```kotlin
interface IExternalAppResolver {
    fun resolveApp(packageName: String): ExternalAppStatus
    fun launchAdjacent(packageName: String, activityName: String = "")
}
```

### Android Resolution Logic

1. `PackageManager.getApplicationInfo()` — check installed
2. API 33+: `ActivityInfo` flags → check `FLAG_ALLOW_UNTRUSTED_ACTIVITY_EMBEDDING`
3. If embeddable → `ExternalAppStatus.EMBEDDABLE` (future inline rendering)
4. If installed but not embeddable → `ExternalAppStatus.INSTALLED_NO_EMBED` → "Open in Split Screen"
5. If not found → `ExternalAppStatus.NOT_INSTALLED` → show install suggestion

### ExternalAppContent (Cross-Platform Composable)

Renders inside FrameWindow:
- App icon placeholder (circle with first letter)
- Package name + label
- Status badge (color-coded: error/warning/success)
- Action button: "Open in Split Screen" or "Open Adjacent"
- All colors from `AvanueTheme.colors`

See full analysis: `Docs/Analysis/Cockpit/Cockpit-Analysis-ActivityEmbedding3rdPartyApps-260217-V1.md`

---

## 12. Updates (260222)

### ContentRenderer Import Fix
`ContentRenderer.kt` imports `CameraPreview` for the `FrameContent.Camera` content type. The import was corrected from the non-existent `com.augmentalis.cameraavanue` package to `com.augmentalis.photoavanue.CameraPreview`, which is the actual composable in the PhotoAvanue module (Chapter 98). The orphaned CameraAvanue module has been deleted — PhotoAvanue is the canonical camera module.

### Build Dependency Fixes
- **BouncyCastle conflict:** `pdfbox-android:2.0.27.0` (in RAG module) transitively pulled `bcprov-jdk15to18:1.72`, conflicting with JSch's `bcprov-jdk18on:1.78.1`. Resolved by excluding the older `jdk15to18` artifacts from pdfbox-android.
- **META-INF merge:** Wildcard `META-INF/versions/*/OSGI-INF/MANIFEST.MF` exclusion added to app packaging to handle JSch/BouncyCastle resource conflicts.

---

## 13. Related Documentation

| Document | Location |
|----------|---------|
| SpatialVoice Redesign Plan | `Docs/Plans/Cockpit/Cockpit-Plan-SpatialVoiceRedesign-260217-V1.md` |
| Multi-Pane + Traffic Lights Plan | `Docs/Plans/Cockpit/Cockpit-Plan-MultiPaneWorkflowTrafficLights-260217-V1.md` |
| Pending Work Items Plan | `Docs/Plans/Cockpit/Cockpit-Plan-PendingWorkItems-260217-V1.md` |
| ActivityEmbedding Research | `Docs/Analysis/Cockpit/Cockpit-Analysis-ActivityEmbedding3rdPartyApps-260217-V1.md` |
| IMU Cursor Fix | `Docs/fixes/VoiceOSCore/VoiceOSCore-Fix-CursorIMUHeadTrackingRegression-260217-V1.md` |
| AvanueUI Theme v5.1 | Chapter 91-92 (`Docs/MasterDocs/AvanueUI/`) |
| Voice Enablement | Chapter 94 (`Docs/MasterDocs/NewAvanues-Developer-Manual/`) |
| Handler Dispatch | Chapter 95 (`Docs/MasterDocs/NewAvanues-Developer-Manual/`) |
| KMP Foundation | Chapter 96 (`Docs/MasterDocs/Foundation/`) |
| Unified Settings | Chapter 90 (`Docs/AVA/ideacode/guides/`) |

---

*Cockpit SpatialVoice Multi-Window System — Chapter 97*
*NewAvanues Developer Manual — Updated 260222 (CameraPreview import fix, build dependency fixes)*
