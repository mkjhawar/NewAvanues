# PhotoAvanue-Plan-ProCameraAndHubSearch-260217-V1

## Overview

Three-feature plan to elevate PhotoAvanue from basic CameraX capture to an Apple Camera-rivaling experience, plus a voice-enabled hub search system.

**Branch:** `Cockpit-Development`
**Modules:** `PhotoAvanue`, `Cockpit` (hub), `VoiceOSCore` (search commands)
**Estimated:** 8 phases, ~45 tasks
**Mode:** `.yolo .swarm .cot .tot`

---

## Feature Matrix

| # | Feature | Scope | Priority |
|---|---------|-------|----------|
| 1 | Apple Camera-like features (CameraX Extensions + Camera2 Pro) | PhotoAvanue | High |
| 2 | Enhanced camera experience (UI polish, haptics, gallery, filters) | PhotoAvanue | Medium |
| 3 | Hub voice-enabled search (documents, apps, features) | Cockpit/Hub, VoiceOSCore | High |

---

## Architectural Principles

### 1. Tiered Controller Abstraction

**Current state:** Single `ICameraController` with basic capture/zoom/exposure.
**Target state:** Three-tier controller hierarchy in commonMain:

```
ICameraController          ← Tier 1: Basic (photo, video, flash, zoom, exposure)
  └── IProCameraController ← Tier 2: Pro (manual ISO, shutter, focus, WB, RAW, burst)
        └── IDepthController ← Tier 3: Depth/AR (depth maps, bokeh from depth)
```

**Why:** Not all devices support pro/depth features. The tiered interface lets consumers check capability at runtime via `controller is IProCameraController`.
**How:** `ICameraController` stays as-is. `IProCameraController` extends it. `IDepthController` extends `IProCameraController`. Android implementations: `AndroidCameraController` (Tier 1, CameraX), `AndroidProCameraController` (Tier 2, Camera2), `AndroidDepthController` (Tier 3, ARCore).

### 2. Cross-Platform Feature Mapping

Based on Apple Camera features the user wants to match:

| Apple Feature | Android Equivalent | Desktop Equivalent | KMP Interface |
|---|---|---|---|
| Portrait Mode (depth bokeh) | CameraX BOKEH extension OR ARCore Depth | N/A (no depth sensor) | `IDepthController.capturePortrait()` |
| Night Mode | CameraX NIGHT extension | N/A | `IExtensionController.captureNight()` |
| HDR | CameraX HDR extension / Ultra HDR | N/A | `IExtensionController.captureHdr()` |
| ProRAW / DNG | Camera2 RAW_SENSOR / CameraX 1.5 RAW | N/A | `IProCameraController.captureRaw()` |
| Live Photos | JPEG + short video clip | N/A | `ICameraController.captureLivePhoto()` |
| Manual ISO | Camera2 SENSOR_SENSITIVITY | N/A | `IProCameraController.setIso()` |
| Manual Shutter | Camera2 SENSOR_EXPOSURE_TIME | N/A | `IProCameraController.setShutterSpeed()` |
| Manual Focus | Camera2 LENS_FOCUS_DISTANCE | N/A | `IProCameraController.setFocusDistance()` |
| Manual White Balance | Camera2 COLOR_CORRECTION_GAINS | N/A | `IProCameraController.setWhiteBalance()` |
| Cinematic Mode | Custom depth-of-field video pipeline | N/A | Future |
| Action Mode (EIS) | CameraX VIDEO_STABILIZATION | N/A | `ICameraController.setStabilization()` |
| Slow-Motion | CameraX 1.5 high-speed recording | N/A | `IProCameraController.setFrameRate()` |
| Macro | Ultra-wide auto-switch at close range | N/A | Device-specific |
| Continuity Camera | N/A (Android) | macOS AVCaptureDevice continuityCamera | `IDesktopCameraController` |

### 3. Hub Search Architecture

**Current state:** Hub shows a static grid of modules. No search.
**Target state:** Universal search bar (voice + text) that finds:

- **Apps/Modules:** PhotoAvanue, WebAvanue, Cockpit, etc.
- **Features:** "night mode", "portrait", "zoom", "flash"
- **Documents:** Recent PDFs, notes, images
- **Voice commands:** "What can I say?" → searchable command list

```
┌──────────────────────────────────────────────┐
│  🔍  "night mode"                    🎤     │  ← Search bar
├──────────────────────────────────────────────┤
│  📸 PhotoAvanue → Night Mode                │  ← Feature result
│  🎤 "enable night mode" (voice command)      │  ← Command result
│  📖 Camera Guide → Night Photography        │  ← Document result
└──────────────────────────────────────────────┘
```

Search index built from: VOS command files + module manifests + recent documents.

---

## Phase 1: CameraX Extensions (Tier 2a — Vendor Features)

Expose CameraX Extensions API for device-dependent computational photography.

### Task 1.1: Extension Capability Detection (commonMain)

New model in `model/CameraExtensions.kt`:

```kotlin
@Serializable
data class CameraExtensions(
    val hasBokeh: Boolean = false,
    val hasHdr: Boolean = false,
    val hasNight: Boolean = false,
    val hasFaceRetouch: Boolean = false,
    val hasUltraHdr: Boolean = false
)
```

Add to `CameraState`: `val extensions: CameraExtensions = CameraExtensions()`

### Task 1.2: IExtensionController Interface (commonMain)

```kotlin
interface IExtensionController : ICameraController {
    val extensionCapabilities: StateFlow<CameraExtensions>
    fun enableBokeh()
    fun enableHdr()
    fun enableNightMode()
    fun enableFaceRetouch()
    fun disableExtension()
}
```

### Task 1.3: AndroidExtensionController (androidMain)

Uses `ExtensionsManager.getInstanceAsync()` + `Camera2CameraInfo` to check availability. Wraps `CameraSelector.Builder.addCameraFilter()` with extension config.

### Task 1.4: Extension UI (commonMain)

Extension mode selector chips in PhotoAvanueScreen (alongside Photo/Video):
- Auto, Portrait, Night, HDR
- Chips only visible if device supports the extension
- Uses `AvanueTheme.colors.primary` for selected state

### Task 1.5: VOS Commands for Extensions

Add to all 5 locale `.app.vos` files:
- `cam_bokeh` — "portrait mode" / "enable bokeh"
- `cam_hdr` — "HDR mode" / "enable HDR"
- `cam_night` — "night mode" / "enable night mode"

---

## Phase 2: Camera2 Pro Controls (Tier 3 — Manual)

Expose Camera2 manual controls for advanced users.

### Task 2.1: ProCameraState Model (commonMain)

```kotlin
@Serializable
data class ProCameraState(
    val isoRange: IntRange = 0..0,
    val currentIso: Int = 0,
    val shutterSpeedRange: LongRange = 0L..0L,  // nanoseconds
    val currentShutterSpeed: Long = 0L,
    val focusDistanceRange: ClosedFloatingPointRange<Float> = 0f..0f,
    val currentFocusDistance: Float = 0f,
    val whiteBalanceMode: WhiteBalanceMode = WhiteBalanceMode.AUTO,
    val isAutoExposureLocked: Boolean = false,
    val isAutoFocusLocked: Boolean = false,
    val rawCaptureSupported: Boolean = false,
    val stabilizationMode: StabilizationMode = StabilizationMode.AUTO
)

@Serializable
enum class WhiteBalanceMode { AUTO, INCANDESCENT, FLUORESCENT, DAYLIGHT, CLOUDY, SHADE, CUSTOM }

@Serializable
enum class StabilizationMode { OFF, AUTO, OIS, EIS }
```

### Task 2.2: IProCameraController Interface (commonMain)

Extends `ICameraController` with manual controls:

```kotlin
interface IProCameraController : ICameraController {
    val proState: StateFlow<ProCameraState>
    fun setIso(value: Int)
    fun setShutterSpeed(nanoseconds: Long)
    fun setFocusDistance(diopters: Float)
    fun setWhiteBalance(mode: WhiteBalanceMode)
    fun lockAutoExposure()
    fun unlockAutoExposure()
    fun lockAutoFocus()
    fun unlockAutoFocus()
    fun captureRaw()
    fun setStabilization(mode: StabilizationMode)
    fun setFrameRate(fps: Int)  // 30, 60, 120, 240
}
```

### Task 2.3: AndroidProCameraController (androidMain)

Uses Camera2 interop via `Camera2CameraInfo` and `Camera2CameraControl`:
- `CaptureRequest.SENSOR_SENSITIVITY` for ISO
- `CaptureRequest.SENSOR_EXPOSURE_TIME` for shutter speed
- `CaptureRequest.LENS_FOCUS_DISTANCE` for manual focus
- `CaptureRequest.COLOR_CORRECTION_GAINS` for white balance
- `ImageCapture.OUTPUT_FORMAT_RAW` for DNG capture (CameraX 1.5)

### Task 2.4: Pro Mode UI (commonMain)

Sliding panels for manual controls:

```
┌─────────────────────────────────────┐
│  PRO MODE                           │
│  ISO:    ├──●──────────────┤  400   │
│  SS:     ├────────●────────┤  1/60  │
│  Focus:  ├──────────────●──┤  ∞     │
│  WB:     [Auto] [☀️] [💡] [☁️]     │
│  [AE🔒]  [AF🔒]  [RAW]             │
└─────────────────────────────────────┘
```

### Task 2.5: VOS Commands for Pro Controls

- `cam_pro_mode` — "pro mode" / "manual mode"
- `cam_iso_up` / `cam_iso_down` — "increase ISO" / "decrease ISO"
- `cam_focus_near` / `cam_focus_far` — "focus closer" / "focus farther"
- `cam_ae_lock` / `cam_af_lock` — "lock exposure" / "lock focus"
- `cam_raw` — "capture raw" / "shoot RAW"
- `cam_slow_mo` — "slow motion" / "120 fps"

---

## Phase 3: ARCore Depth API (Tier 4 — Depth/AR)

### Task 3.1: DepthState Model (commonMain)

```kotlin
@Serializable
data class DepthState(
    val isDepthAvailable: Boolean = false,
    val isDepthActive: Boolean = false,
    val depthConfidence: Float = 0f,
    val bokehIntensity: Float = 0.5f
)
```

### Task 3.2: IDepthController Interface (commonMain)

```kotlin
interface IDepthController : IProCameraController {
    val depthState: StateFlow<DepthState>
    fun enableDepth()
    fun disableDepth()
    fun capturePortrait(bokehIntensity: Float = 0.5f)
    fun setBokehIntensity(intensity: Float)
}
```

### Task 3.3: AndroidDepthController (androidMain)

Uses ARCore Depth API:
- `Frame.acquireDepthImage16Bits()` for depth maps
- Custom shader for depth-based background blur (portrait mode)
- Depth confidence filtering for clean edges
- Device capability check via `ArCoreApk.checkAvailability()`

### Task 3.4: Dependencies

```kotlin
// build.gradle.kts androidMain
implementation("com.google.ar:core:1.43.0")
```

### Task 3.5: VOS Commands for Depth

- `cam_portrait` — "portrait mode" / "depth mode"
- `cam_bokeh_more` / `cam_bokeh_less` — "more blur" / "less blur"

---

## Phase 4: Enhanced Camera UX

### Task 4.1: Capture Haptics & Animation

- Haptic feedback on capture (Android `HapticFeedbackConstants.CONFIRM`)
- Shutter animation (brief white flash overlay + scale-down)
- Video recording pulse animation on timer badge

### Task 4.2: Gallery Thumbnail Preview

- Last-captured thumbnail in bottom-left corner
- Tap to open in system gallery
- Uses `ICameraController.state.lastCapturedUri` to load thumbnail

### Task 4.3: Aspect Ratio Selector

- 4:3 (default), 16:9, 1:1 chips above mode selector
- Updates `CameraState.aspectRatio` → re-binds camera with new `ResolutionSelector`

### Task 4.4: Grid Overlay

- Rule-of-thirds grid toggle (settings preference)
- Golden ratio option
- Level indicator (gyroscope-based horizon line)

### Task 4.5: Timer Mode

- 3s / 10s countdown timer
- Visual countdown overlay with seconds remaining
- VOS command: `cam_timer` — "set timer" / "3 second timer"

---

## Phase 5: Desktop Camera Support

### Task 5.1: DesktopCameraController (desktopMain)

Uses JavaCV (`OpenCVFrameGrabber`) or webcam-capture library:

```kotlin
class DesktopCameraController : ICameraController {
    // OpenCVFrameGrabber for webcam access
    // Frame → BufferedImage → Compose ImageBitmap
    // Photo: save current frame as JPEG
    // Video: FFmpegFrameRecorder for recording
}
```

### Task 5.2: macOS Continuity Camera Support

For macOS target (future):
- Use `AVCaptureDevice.DiscoverySession` via Kotlin/Native
- Detect `.continuityCamera` device type
- iPhone provides: Centre Stage, Portrait mode, Desk View, Studio Light

### Task 5.3: Windows MediaCapture Support

For Windows target (future):
- Use `Windows.Media.Capture.MediaCapture` via JNI/JNA
- `VideoDeviceController` for manual exposure, white balance, auto-focus
- `MediaFrameReader` for RGB/IR/depth frame streams (RealSense)

### Task 5.4: Dependencies

```kotlin
// build.gradle.kts desktopMain
implementation("org.bytedeco:javacv-platform:1.5.10")
// OR
implementation("com.github.sarxos:webcam-capture:0.3.12")
```

---

## Phase 6: Hub Voice-Enabled Search

### Task 6.1: SearchIndex Model (commonMain)

```kotlin
@Serializable
data class SearchEntry(
    val id: String,
    val type: SearchEntryType,
    val title: String,
    val subtitle: String = "",
    val keywords: List<String> = emptyList(),
    val moduleId: String? = null,
    val actionId: String? = null,
    val uri: String? = null,
    val icon: String? = null
)

@Serializable
enum class SearchEntryType { APP, FEATURE, COMMAND, DOCUMENT, SETTING }
```

### Task 6.2: SearchIndexBuilder (commonMain)

Builds search index from multiple sources:

```kotlin
class SearchIndexBuilder {
    fun indexModules(modules: List<HubModule>): List<SearchEntry>
    fun indexVosCommands(commands: Map<String, VosCommand>): List<SearchEntry>
    fun indexRecentDocuments(documents: List<RecentDocument>): List<SearchEntry>
    fun indexSettings(settings: List<SettingsEntry>): List<SearchEntry>

    fun search(query: String, limit: Int = 10): List<SearchEntry>
}
```

Search algorithm: fuzzy string matching with weighted scoring:
- Exact match in title: weight 1.0
- Keyword match: weight 0.8
- Substring in title: weight 0.6
- Substring in subtitle: weight 0.3

### Task 6.3: HubSearchBar Composable (commonMain)

```kotlin
@Composable
fun HubSearchBar(
    searchIndex: SearchIndexBuilder,
    onResultSelected: (SearchEntry) -> Unit,
    modifier: Modifier = Modifier
)
```

- Text input with microphone button (triggers VOS dictation)
- Live results dropdown as user types/speaks
- Result cards grouped by type (Apps, Features, Commands, Docs)
- AvanueTheme.colors styling with glass effect background

### Task 6.4: Voice Search Integration

- New VOS command: `hub_search` — "search" / "find" / "look for"
- Activates dictation mode focused on search input
- Spoken query gets routed to SearchIndexBuilder
- Results read aloud via TTS (first 3 results)

### Task 6.5: Recent Documents Provider

- Queries SQLDelight for recently opened PDFs, notes, images
- Integrates with Cockpit session history (CockpitFrame table)
- Provides document entries to SearchIndexBuilder

---

## Phase 7: VOS Command Expansion

All new commands across phases 1-6, added to all 5 locale `.app.vos` files:

| Phase | New Commands | Prefix |
|-------|-------------|--------|
| Phase 1 (Extensions) | cam_bokeh, cam_hdr, cam_night | cam_ |
| Phase 2 (Pro) | cam_pro_mode, cam_iso_up/down, cam_focus_near/far, cam_ae_lock, cam_af_lock, cam_raw, cam_slow_mo | cam_ |
| Phase 3 (Depth) | cam_portrait, cam_bokeh_more/less | cam_ |
| Phase 4 (UX) | cam_timer, cam_grid | cam_ |
| Phase 6 (Search) | hub_search | hub_ |

**Total new commands:** ~18 × 5 locales = 90 entries

---

## Phase 8: Build & Verification

### Task 8.1: Desktop Build Verification
`./gradlew :Modules:PhotoAvanue:compileKotlinDesktop`

### Task 8.2: Android Build Verification
`./gradlew :Modules:PhotoAvanue:compileDebugKotlinAndroid`

### Task 8.3: Cockpit Build Verification
`./gradlew :Modules:Cockpit:compileDebugKotlinAndroid`

### Task 8.4: VOS Parser Verification
Ensure all new `cam_*` and `hub_*` commands parse correctly in VosParser.

### Task 8.5: Device Testing
- Test CameraX Extensions on Pixel/Samsung emulator
- Test Camera2 Pro controls on supported hardware
- Test search in hub with voice input

---

## Execution Strategy (Swarm)

| Agent | Phases | Parallelizable |
|-------|--------|---------------|
| Agent 1 (Camera Core) | Phase 1 + Phase 2 | Sequential (2 depends on 1) |
| Agent 2 (Depth/AR) | Phase 3 | Independent |
| Agent 3 (UX Polish) | Phase 4 | Independent |
| Agent 4 (Desktop) | Phase 5 | Independent |
| Agent 5 (Hub Search) | Phase 6 | Independent |
| Agent 6 (VOS Commands) | Phase 7 | After phases 1-6 |
| Agent 7 (Build/Test) | Phase 8 | After phase 7 |

**Parallel phases:** 2, 3, 4, 5, 6 can all run simultaneously.
**Sequential:** Phase 1 → Phase 2, then Phase 7 → Phase 8.

---

## Cross-Platform Feature Coverage

| Feature | Android | iOS (Future) | Desktop | Web (Future) |
|---------|---------|-------------|---------|-------------|
| Basic capture | CameraX | AVFoundation | JavaCV | MediaDevices API |
| Video recording | CameraX VideoCapture | AVCaptureMovieFileOutput | FFmpegFrameRecorder | MediaRecorder |
| Extensions (HDR/Night/Bokeh) | CameraX ExtensionsManager | Built-in AVCaptureSession | N/A | N/A |
| Pro controls (ISO/SS/Focus/WB) | Camera2 API | AVCaptureDevice manual | N/A | N/A |
| RAW/DNG capture | CameraX 1.5 / Camera2 | AVCapturePhotoOutput RAW | N/A | N/A |
| Depth/Portrait | ARCore Depth API | AVDepthData | N/A | N/A |
| Continuity Camera | N/A | N/A | macOS AVFoundation | N/A |
| Slow-motion | CameraX 1.5 high-speed | AVCaptureSession 120/240fps | N/A | N/A |
| Hub search | Shared (commonMain) | Shared (commonMain) | Shared (commonMain) | Shared (commonMain) |

**KMP Score projection:** 12/20 feature areas in commonMain (60% shared). Platform-specific: CameraX/Camera2, ARCore, AVFoundation, JavaCV, platform permissions.
