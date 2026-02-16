# Cockpit Analysis: Task_Cockpit vs Modules/Cockpit Comparison

**Version:** 1.0
**Date:** 2026-02-16
**Purpose:** Evaluate both Cockpit implementations to inform CockpitNG design
**Sources:**
- Task_Cockpit: `/Users/manoj_mbpm14/Downloads/aijunk/Cockpit/Task_Cockpit/`
- Modules/Cockpit: Scattered across `Modules/Rpc/`, `android/apps/cockpit-mvp/`

---

## Executive Summary

| Dimension | Task_Cockpit (Legacy) | Modules/Cockpit (Current) |
|-----------|----------------------|--------------------------|
| **Maturity** | Production-grade, functional | Skeletal / incomplete |
| **UI Framework** | Android Views (XML + Fragment + ViewBinding) | Jetpack Compose (minimal) |
| **Architecture** | Monolithic (tightly coupled) | 3-tier (gRPC distributed) |
| **Total Files** | ~90 Kotlin + XML layouts | ~27 (22 generated models + 3 server + 2 app) |
| **Total LOC** | ~5,000+ (source only) | ~3,500 (mostly generated proto models) |
| **Content Types** | 6 (image, video, PDF, note, camera, web) | 1 (web only, partial) |
| **Layout Modes** | 7 functional modes | 2 (flat + spatial, placeholder-level) |
| **Voice Commands** | Fully integrated | None |
| **Persistence** | Realm DB, full state | None (in-memory only) |
| **Cross-Device** | None | gRPC protocol defined + server implemented |
| **Runnable?** | Yes (standalone APK) | Unlikely (missing Composables, no build.gradle.kts) |

**Verdict:** Task_Cockpit is the working product. Modules/Cockpit is an architectural foundation (good gRPC protocol + server) wrapped around an Android app that appears incomplete or lost significant UI code.

---

## 1. What Task_Cockpit Has That Modules/Cockpit Does NOT

### 1.1 Multi-Content Frame System (CRITICAL GAP)

Task_Cockpit supports 6 distinct content types per frame:

| Content Type | Implementation | Key Features |
|-------------|---------------|--------------|
| **IMAGE_VIEW** | `ImageViewContainer` | Zoom/pan, EXIF metadata, multi-image gallery |
| **VIDEO_PLAYER** | `VideoPlayerContainerWithThumbnail` | Playback controls, position tracking, thumbnails |
| **PDF_VIEW** | `PdfViewContainer` | Page navigation, zoom, bookmarks, goto page |
| **NOTE_VIEW** | `NoteViewContainer` | Rich text editing, auto-save |
| **CAMERA_VIEW** | `CameraViewContainer` | Live feed, zoom, exposure, capture |
| **WEB_VIEW** | `WebViewContainer` | Desktop mode, auth, cursor magnification, touch simulation |

Modules/Cockpit only has WebView content, and even that is partial (Google Docs PDF fallback, placeholder video).

### 1.2 Seven Layout Modes (CRITICAL GAP)

| Mode | Description | Container |
|------|------------|-----------|
| `VIEW_PAGER_COCKPIT` | Horizontal swipe with curved thumbnails | ViewPager2 |
| `FULLSCREEN` | Single frame fills screen | FrameLayout |
| `ROW` | Horizontal scrollable strip | LinearLayout (horizontal) |
| `GRID` | 2x2, 2x3 grid arrangements | GridLayout |
| `GALLERY` | Media-focused grid (images/video only) | GridLayout |
| `MULTI_LEFT` | Large frame left, stack right | ConstraintLayout |
| `MULTI_RIGHT` | Large frame right, stack left | ConstraintLayout |
| `WORKFLOW_MODE` | Vertical step-by-step with numbered frames | LinearLayout (vertical) |

Modules/Cockpit has only: 2D flat (basic positioning) and 3D spatial (curved arc/theater/cylinder presets). Neither supports the multi-pane layout modes Task_Cockpit offers.

### 1.3 Task/Session Management

- **Task list screen**: Create, rename, copy, delete, import/export tasks
- **Multiple sessions**: Each task = independent cockpit session with own frames and layout
- **Default task**: "Media Viewer" auto-created for quick-launch content
- **Task factory methods**: `createPdfTask()`, `createWebPageTask()`, `createImageTask()`, `createVideoTask()`, etc.

Modules/Cockpit has no concept of task/session management — it's a single workspace.

### 1.4 Workflow System

- Sequential instructional workflows
- Named steps linked to specific frames
- Step reordering (up/down)
- Workflow direction toggle
- Visual step list with navigation
- Dedicated `WORKFLOW_MODE` layout

Modules/Cockpit has no workflow capability.

### 1.5 Full Voice Command Integration

- Context-sensitive command bar at screen bottom
- Commands adapt based on active fragment, selected frame type, and layout mode
- Hierarchical help dialog
- 11 zoom preset levels
- Scroll direction commands
- Frame navigation (next, previous, goto)
- Layout switching commands
- Web interaction commands (touch, drag, pinch, reload)
- PDF commands (page forward/back, goto page)
- Media controls (play, pause, rewind)

Modules/Cockpit has zero voice integration.

### 1.6 Persistence Layer

- **Realm Database** for all task/frame state
- Persists across app restarts:
  - Frame positions, scroll offsets, zoom levels
  - PDF page numbers
  - Video playback position
  - Web page URLs and scroll position
  - Note content
  - Layout mode per task
  - Frame visibility/maximization state

Modules/Cockpit has in-memory state only (StateFlow in ViewModel, lost on process death).

### 1.7 AiView Abstraction Layer (713 lines)

- Unified container wrapping all content types
- Cursor integration for AR glasses
- Voice command routing to active view
- Unified zoom, scroll, navigation interface
- Metadata display for images/videos
- View type switching and refresh

### 1.8 AR/Glasses Theme System

- Multiple AR-specific themes: `AR_ARGO`, `AR_BLACK`, `AR_UNIVERSAL`
- Custom background images per theme
- Frame stroke and radius customization
- Header logo customization
- Toolbar color theming

### 1.9 Frame Management Features

- Drag-to-reorder frames
- Hide/show (minimized frames list)
- Maximize individual frames to fullscreen
- Frame numbering with title display
- Selection highlighting with border color change
- Thumbnail preview generation for ViewPager mode

### 1.10 Cloud Storage Integration

- Dropbox, OneDrive, Box, Google Drive
- Import content from cloud services into frames
- QR code credential scanning for web auth

---

## 2. What Modules/Cockpit Has That Task_Cockpit Does NOT

### 2.1 Cross-Device gRPC Protocol (SIGNIFICANT)

Complete protocol definition with 10 RPC methods:

| RPC Method | Purpose |
|-----------|---------|
| `RegisterDevice` | Device registration with capabilities |
| `GetDevices` | List devices (filter by online/platform) |
| `SendDeviceCommand` | Control devices (sync, screenshot, status) |
| `GetWindows` | Window listing (filter by device) |
| `SendWindowCommand` | Window control (move, resize, show, hide, focus, close) |
| `SaveLayout` | Persist window arrangement as preset |
| `ApplyLayout` | Restore saved layout |
| `GetLayouts` | List saved layouts |
| `StartSync` | Cross-device synchronization |
| `GetSyncStatus` | Sync progress tracking |
| `StreamDeviceEvents` | Real-time event stream |

Task_Cockpit is Android-only with no network protocol.

### 2.2 Desktop gRPC Server (~1,400 lines, fully implemented)

| Component | Lines | Purpose |
|-----------|-------|---------|
| `CockpitGrpcServer` | 409 | Server lifecycle, health checks, reflection |
| `CockpitServiceImpl` | 675 | All 10 RPC implementations |
| `DeviceRegistry` | 335 | Thread-safe device tracking, event streaming |

Features:
- Device heartbeat monitoring (10s interval, 30s timeout)
- Stale device pruning
- Layout preset storage
- Sync session management
- Configurable server (port, message sizes, timeouts)

### 2.3 Jetpack Compose UI Framework

- Modern declarative UI (vs legacy XML/Fragment)
- Type-safe state management via StateFlow
- Compose animation APIs
- Material Design 3

Task_Cockpit uses legacy Android Views with XML layouts and Fragment-based navigation.

### 2.4 KMP-Ready Architecture

- Wire Protocol Buffer models in `commonMain` (cross-platform)
- Desktop server is pure JVM (no Android dependencies)
- Architecture supports iOS/Web clients

Task_Cockpit is Android-only with no cross-platform path.

### 2.5 3D Spatial Layout Presets

- Arc layout (curved window arrangement)
- Theater layout (tiered curved arrangement)
- Cylinder layout (360-degree wrap)
- Head cursor overlay with IMU integration

Task_Cockpit has VIEW_PAGER_COCKPIT with curved thumbnails but not true 3D spatial positioning.

### 2.6 IMU Head Cursor (via DeviceManager)

- Head-based cursor navigation
- Hit detection against spatial windows
- Toggle on/off from UI

Task_Cockpit has cursor support in AiView but via touch magnification, not IMU-based head tracking.

---

## 3. Architectural Comparison

### Task_Cockpit Architecture

```
CockpitActivity
  ├── TaskListFragment (task CRUD)
  ├── TaskFragment (main cockpit display, 1385 lines)
  │   ├── FrameFragment × N (one per visible frame)
  │   │   └── AiView (content type adapter)
  │   │       ├── ImageViewContainer
  │   │       ├── VideoPlayerContainerWithThumbnail
  │   │       ├── PdfViewContainer
  │   │       ├── NoteViewContainer
  │   │       ├── CameraViewContainer
  │   │       └── WebViewContainer
  │   ├── ViewPager2 (cockpit mode)
  │   ├── GridLayout (grid/gallery mode)
  │   ├── ConstraintLayout (multi-left/right)
  │   └── LinearLayout (row/workflow)
  └── WorkflowFragment (step management)
```

**Strengths:** Everything works, battle-tested, feature-complete
**Weaknesses:** Monolithic TaskFragment (1385 lines), Realm DB (deprecated), XML layouts, no cross-platform, tightly coupled

### Modules/Cockpit Architecture

```
Wire Protocol (commonMain KMP)
  └── 22 generated message/service files

Desktop gRPC Server (JVM)
  ├── CockpitGrpcServer (lifecycle)
  ├── CockpitServiceImpl (service logic)
  └── DeviceRegistry (device tracking)

Android MVP App (Compose)
  ├── MainActivity (115 lines)
  ├── TopNavigationBar
  ├── [MISSING] WorkspaceViewModel
  ├── [MISSING] SpatialWorkspaceView
  ├── [MISSING] WorkspaceView
  ├── [MISSING] ControlPanel
  └── [MISSING] HeadCursorOverlay
```

**Strengths:** Clean separation, gRPC protocol, cross-platform ready, Compose
**Weaknesses:** Android app is skeletal, missing critical Composables, no build.gradle.kts for app module, no content type support, no persistence

---

## 4. Technology Stack Comparison

| Component | Task_Cockpit | Modules/Cockpit |
|-----------|-------------|----------------|
| **Language** | Kotlin | Kotlin |
| **UI Framework** | Android Views (XML + Fragment) | Jetpack Compose |
| **Navigation** | Navigation Component + SafeArgs | None (single activity) |
| **DI** | Hilt | None visible |
| **Database** | Realm | None (in-memory) |
| **Networking** | None | gRPC (Wire + Netty) |
| **Image Loading** | Coil + Glide | None |
| **Camera** | CameraX | None |
| **Video** | ExoPlayer (placeholder) | None |
| **PDF** | Android PdfViewer library | Google Docs fallback |
| **State** | Fragment savedInstanceState + Realm | StateFlow (ViewModel) |
| **Build** | Gradle (configured) | Missing build.gradle.kts |
| **Platform** | Android only | KMP (models), JVM (server), Android (app) |

---

## 5. Quality Assessment

| Metric | Task_Cockpit | Modules/Cockpit |
|--------|-------------|----------------|
| **Functionality** | 9/10 (does what it should) | 3/10 (mostly protocol, minimal UI) |
| **Code Quality** | 4/10 (monolithic, legacy patterns) | 7/10 (clean architecture, but incomplete) |
| **Maintainability** | 4/10 (1385-line God Fragment) | 6/10 (modular but missing pieces) |
| **Cross-Platform** | 1/10 (Android-only) | 8/10 (KMP models, gRPC server) |
| **UI/UX Polish** | 7/10 (functional, AR-optimized) | 2/10 (skeleton with TODOs) |
| **Voice Integration** | 8/10 (comprehensive commands) | 0/10 (none) |
| **Persistence** | 8/10 (full state, Realm) | 0/10 (in-memory only) |

---

## 6. CockpitNG Requirements (Synthesized from Both)

### Must Have (from Task_Cockpit)
1. Multi-content frames (image, video, PDF, note, camera, web)
2. Multiple layout modes (cockpit, fullscreen, grid, split-pane, workflow)
3. Task/session management (create, save, load, delete)
4. Full state persistence (SQLDelight, not Realm)
5. Voice command integration (VoiceOS tier system)
6. Frame management (add, remove, reorder, hide, maximize, minimize)
7. AR/glasses theme support (AvanueUI Theme v5.1)

### Must Have (from Modules/Cockpit)
8. Cross-device gRPC protocol (device registration, window sync, layout sharing)
9. Jetpack Compose UI (not legacy Views)
10. KMP architecture (shared models in commonMain)
11. 3D spatial layout presets (arc, theater, cylinder)
12. IMU head cursor navigation

### New for CockpitNG
13. **Freeform window positioning** (drag anywhere, resize freely)
14. **Window snapping** (snap to grid, snap to edges, magnetic guides)
15. **AvanueUI Theme v5.1** (palette + style + appearance axes)
16. **SQLDelight persistence** (replacing Realm)
17. **Unified AVID integration** (voice identifiers on all windows)

---

## 7. Recommendations for CockpitNG

### Architecture
```
CockpitNG/
  src/
    commonMain/     # KMP shared models, state, business logic
    androidMain/    # Compose UI, Android-specific content renderers
    iosMain/        # SwiftUI (future)
    desktopMain/    # Compose Desktop (future)
```

### Key Design Decisions
1. **Compose Multiplatform** for UI (not legacy Views)
2. **SQLDelight** for persistence (KMP-compatible, replacing Realm)
3. **gRPC** for cross-device (keep existing protocol + server)
4. **AvanueUI Theme v5.1** (mandatory per repo rules)
5. **VoiceOS integration** via existing 4-tier voice system
6. **Freeform windows** via Compose `Modifier.offset()` + drag gestures
7. **Content renderers** as plugin interfaces (easy to add new types)

### Migration Strategy
- Port Task_Cockpit's feature set (content types, layout modes, workflows) to Compose
- Integrate Modules/Cockpit's gRPC protocol for cross-device
- Add freeform window positioning as new capability
- Use SQLDelight for persistence instead of Realm
- Wire into VoiceOS for voice commands

---

## Appendix: File Inventories

### Task_Cockpit Key Files
| File | Lines | Purpose |
|------|-------|---------|
| `TaskFragment.kt` | 1,385 | Main cockpit display controller |
| `AiView.kt` | 713 | Content type abstraction |
| `TaskListFragment.kt` | 382 | Task management UI |
| `CockpitActivity.kt` | 259 | Entry point, factory methods |
| `FrameFragment.kt` | 200+ | Frame wrapper |
| `TaskRepository.kt` | ~200 | Realm persistence |
| `TaskModel.kt` | ~150 | Task data model |
| `FrameModel.kt` | ~150 | Frame data model |

### Modules/Cockpit Key Files
| File | Lines | Purpose |
|------|-------|---------|
| `CockpitServiceImpl.kt` | 675 | gRPC service implementation |
| `CockpitGrpcServer.kt` | 409 | Server lifecycle |
| `DeviceRegistry.kt` | 335 | Device tracking |
| `MainActivity.kt` | 115 | Android Compose entry (skeletal) |
| `DeviceInfo.kt` | 280 | Wire protocol model |
| `WindowInfo.kt` | 322 | Wire protocol model |
| 20 more proto models | ~3,000 | Generated Wire classes |

---

**Analysis by:** Manoj Jhawar
**Date:** 2026-02-16
