# Developer Manual — Chapter 98: PhotoAvanue KMP Camera Module

**Module:** `Modules/PhotoAvanue/`
**Package:** `com.augmentalis.photoavanue`
**Platform:** KMP (Android + Desktop, iOS deferred)
**Branch:** `Cockpit-Development`
**Date:** 2026-02-17
**Commit:** `9ed78c3d`

---

## 1. Overview

PhotoAvanue is the cross-platform camera module for the Avanues ecosystem. It provides photo capture, video recording with pause/resume, GPS EXIF tagging, flash control, lens switching, zoom, and exposure compensation.

The module follows the **self-running pattern**: it ships both a standalone screen (`PhotoAvanueScreen`) for independent launch from the app hub, and an embeddable content view (`CameraPreview`) for Cockpit frame embedding.

### Key Capabilities

- Photo capture with GPS EXIF metadata (DMS format)
- Video recording with pause/resume lifecycle
- 5-level discrete zoom and exposure controls
- Flash cycling: OFF → ON → AUTO → TORCH
- Front/back lens switching
- KMP-shared state model (`CameraState` + sub-states)
- Platform-agnostic `ICameraController` interface
- Full SpatialVoice design language integration
- 17 voice commands across 5 locales (en-US, de-DE, es-ES, fr-FR, hi-IN)

---

## 2. Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     commonMain (KMP)                      │
│                                                           │
│  ┌─────────────────┐  ┌──────────────────────────────┐   │
│  │ ICameraController│  │         model/                │   │
│  │   (interface)    │  │  CameraState   ZoomState     │   │
│  │                  │  │  ExposureState RecordingState │   │
│  │  state: StateFlow│  │  GpsMetadata  AspectRatioMode│   │
│  │  capturePhoto()  │  │  CameraLens   FlashMode      │   │
│  │  startRecording()│  │  CaptureMode                 │   │
│  │  zoomIn/Out()    │  └──────────────────────────────┘   │
│  │  exposure+/-()   │                                     │
│  │  setFlashMode()  │  ┌──────────────────────────────┐   │
│  │  switchLens()    │  │    PhotoAvanueScreen          │   │
│  │  release()       │  │  (standalone full-screen UI)  │   │
│  └─────────────────┘  └──────────────────────────────┘   │
│                                                           │
├───────────────────────────┬───────────────────────────────┤
│      androidMain          │        desktopMain            │
│                           │                               │
│  AndroidCameraController  │  (future: DesktopCamera-      │
│    CameraX: Preview +     │   Controller via JavaCV/       │
│    ImageCapture +          │   webcam-capture)              │
│    VideoCapture<Recorder>  │                               │
│                           │                               │
│  AndroidLocationProvider  │                               │
│    Dual GPS + Network     │                               │
│    → GpsMetadata          │                               │
│                           │                               │
│  CameraPreview            │                               │
│    (embeddable Compose)   │                               │
└───────────────────────────┴───────────────────────────────┘
```

### Dependency Chain

```
Foundation (KMP) ←── PhotoAvanue (KMP) ←── Cockpit (KMP)
Logging (KMP)    ←──┘                       ↑
AvanueUI (KMP)   ←──┘                       │
                                        apps/avanues
```

---

## 3. Models & State (commonMain)

### 3.1 CameraState (root state)

```kotlin
@Serializable
data class CameraState(
    val lens: CameraLens = CameraLens.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val zoom: ZoomState = ZoomState(),
    val exposure: ExposureState = ExposureState(),
    val aspectRatio: AspectRatioMode = AspectRatioMode.AUTO,
    val recording: RecordingState = RecordingState(),
    val isCapturing: Boolean = false,
    val hasGpsLocation: Boolean = false,
    val lastCapturedUri: String? = null,
    val error: String? = null
)
```

### 3.2 Sub-State Models

| Model | Key Fields | Computed Properties |
|-------|-----------|-------------------|
| `ZoomState` | currentRatio, minRatio, maxRatio | `stepSize` (range / 5), `currentLevel` (1-5) |
| `ExposureState` | currentIndex, minIndex, maxIndex | `stepSize` (range / 5), `currentLevel` (1-5) |
| `RecordingState` | isRecording, isPaused, durationMs, outputUri | — |
| `GpsMetadata` | latitude, longitude, altitude, timestamp | `latitudeRef`, `longitudeRef`, `altitudeRef`, `latitudeDms()`, `longitudeDms()` |

### 3.3 Enums

| Enum | Values |
|------|--------|
| `CameraLens` | FRONT, BACK |
| `FlashMode` | OFF, ON, AUTO, TORCH |
| `CaptureMode` | PHOTO, VIDEO, SCAN |
| `AspectRatioMode` | AUTO, RATIO_4_3, RATIO_16_9 |
| `RecordingEvent` | STARTED, PAUSED, RESUMED, STOPPED, FINALIZED, ERROR |

---

## 4. ICameraController Interface (commonMain)

Platform-agnostic camera controller contract:

```kotlin
interface ICameraController {
    val state: StateFlow<CameraState>

    // Capture
    fun capturePhoto()
    fun startRecording()
    fun stopRecording()
    fun pauseRecording()
    fun resumeRecording()

    // Controls
    fun switchLens()
    fun setFlashMode(mode: FlashMode)
    fun zoomIn()
    fun zoomOut()
    fun setZoomLevel(level: Int)      // 1-5 discrete levels
    fun increaseExposure()
    fun decreaseExposure()
    fun setExposureLevel(level: Int)  // 1-5 discrete levels
    fun setCaptureMode(mode: CaptureMode) // 260222: photo/video/scan mode switching

    // Lifecycle
    fun release()
}
```

### Design Decisions

- **5-level discrete controls** for zoom/exposure (not continuous sliders) — optimized for voice commands ("zoom in" = +1 level)
- **StateFlow-driven** — reactive UI pattern replaces imperative callbacks
- **No CameraX types in interface** — clean KMP abstraction, platform-agnostic consumers
- **Capture mode switching** (260222) — `setCaptureMode()` allows dynamic switching between PHOTO, VIDEO, and SCAN modes

---

## 5. Android Implementation (androidMain)

### 5.1 AndroidCameraController

Implements `ICameraController` using CameraX. Ported from Avenue-Redux's `CameraViewContainer`, converted from imperative FrameLayout to StateFlow pattern.

**Key implementation details:**

| Method | CameraX API | Notes |
|--------|-------------|-------|
| `bindCamera()` | `ProcessCameraProvider.bindToLifecycle()` | Binds Preview + ImageCapture + VideoCapture use cases |
| `capturePhoto()` | `ImageCapture.takePicture()` | Saves to MediaStore with `ImageCapture.Metadata.location` |
| `startRecording()` | `VideoCapture<Recorder>.output.prepareRecording().withAudioEnabled().start()` | FileOutputOptions targeting MediaStore |
| `stopRecording()` | `Recording.stop()` | Triggers FINALIZED event |
| `pauseRecording()` / `resumeRecording()` | `Recording.pause()` / `Recording.resume()` | Android API 24+ |
| `switchLens()` | Re-bind with new `CameraSelector` | Front ↔ Back toggle |
| `setFlashMode()` | `ImageCapture.flashMode` + `CameraControl.enableTorch()` | TORCH mode uses enableTorch(true) |
| `zoomIn()` / `zoomOut()` | `CameraControl.setZoomRatio()` | Steps by `ZoomState.stepSize` |
| `increaseExposure()` / `decreaseExposure()` | `CameraControl.setExposureCompensationIndex()` | Steps by `ExposureState.stepSize` |
| `setCaptureMode()` (260222) | Update `state.copy(captureMode = mode)` | Recording stop guard: if recording active, stop before switching to PHOTO |

**Bug fix (from Avenue-Redux):** Original `setExposure()` always set `minExposure` instead of the computed level. Fixed in `setExposureLevel()` to properly map level 1-5 to the exposure range.

### 5.2 AndroidProCameraController (260222)

`AndroidProCameraController` extends `IProCameraController` (which extends `ICameraController`) with Camera2 interop features: manual ISO, shutter speed, focus distance, white balance presets, CameraX Extensions (Bokeh/HDR/Night), RAW capture, and video stabilization.

**setCaptureMode fix (260222):** Previously missing the `setCaptureMode(CaptureMode)` abstract member from `ICameraController`. Now implements the same logic as `AndroidCameraController`:
- Early return if mode unchanged
- Stop active recording before switching away from video mode
- Reset `RecordingState` and clear errors on mode switch

**Key differences from AndroidCameraController:**
- Supports `setExtensionMode()`, `setProMode()`, `setIso()`, `setShutterSpeed()`, `setFocusDistance()`, `setWhiteBalance()`, `setRawCapture()`, `setStabilization()`
- Uses `Camera2CameraControl` / `Camera2CameraInfo` for Camera2 interop
- Tracks additional `ProCameraState` alongside base `CameraState`

### 5.3 AndroidLocationProvider

Dual GPS + Network location provider for EXIF tagging.

- Update interval: 60s, minimum distance: 0m
- Converts `android.location.Location` → `GpsMetadata` (commonMain model)
- Exposes both `currentMetadata: GpsMetadata?` and `currentLocation: Location?`
- Requires `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` permissions

### 5.4 GpsMetadata DMS Conversion

EXIF GPS tags require Degrees/Minutes/Seconds rational format. The `GpsMetadata` model handles this:

```
Decimal: 37.7749° → DMS: "37/1,46/1,29640/1000"
Rational: 37° 46' 29.64"
```

Replaces Avenue-Redux's `GpsParser` object with a KMP-shared, testable data class.

---

## 6. UI Components

### 6.1 PhotoAvanueScreen (commonMain — standalone)

Full-screen camera experience with SpatialVoice design language:

```
┌────────────────────────────────────────┐
│ ◄  Photo Avanue              📍 GPS   │  ← TopAppBar (transparent)
│                                        │
│                                        │
│                               [+] Zoom │  ← Side panel
│              CAMERA                    │     (zoom/exposure
│              PREVIEW                   │      buttons)
│                                        │
│                                        │
│          [Photo]  [Video]              │  ← Mode selector chips
│                                        │
│    ⚡    [  📸  ]    🔄               │  ← Bottom controls
│  Flash    Capture    Lens              │     (flash/capture/switch)
└────────────────────────────────────────┘
```

- Background: SpatialVoice gradient (`background → surface.copy(0.6f) → background`)
- TopAppBar: transparent with GPS indicator icon
- Mode chips: Photo/Video with animated selection
- Recording overlay: red indicator with MM:SS timer + pause status

### 6.2 CameraPreview (androidMain — embeddable)

Minimal camera view for Cockpit frame embedding:

- No TopAppBar or navigation chrome
- AndroidView wrapping `PreviewView` with CameraX binding
- Bottom control bar: flash cycle, capture/record, lens switch
- **Mode chips:** Photo/Video mode selector with `ModeChip` onClick wired to `controller.setCaptureMode()` (260222)
- Recording indicator overlay with timer
- Permission request UI (AvanueTheme-styled)
- Uses `DisposableEffect` for clean controller lifecycle

---

## 7. Voice Commands (VOS v3.0)

17 commands with prefix `cam_`, category `CAMERA`:

| action_id | en-US Primary | Action Type |
|-----------|--------------|-------------|
| `cam_open` | open photo avanue | OPEN_MODULE |
| `cam_capture` | take photo | CAPTURE_PHOTO |
| `cam_record_start` | start recording | RECORD_START |
| `cam_record_stop` | stop recording | RECORD_STOP |
| `cam_record_pause` | pause recording | RECORD_PAUSE |
| `cam_record_resume` | resume recording | RECORD_RESUME |
| `cam_flip` | flip camera | SWITCH_LENS |
| `cam_flash_on` | flash on | FLASH_ON |
| `cam_flash_off` | flash off | FLASH_OFF |
| `cam_flash_auto` | flash auto | FLASH_AUTO |
| `cam_flash_torch` | torch mode | FLASH_TORCH |
| `cam_zoom_in` | zoom in camera | ZOOM_IN |
| `cam_zoom_out` | zoom out camera | ZOOM_OUT |
| `cam_exposure_up` | more exposure | EXPOSURE_UP |
| `cam_exposure_down` | less exposure | EXPOSURE_DOWN |
| `cam_mode_photo` | photo mode | MODE_PHOTO |
| `cam_mode_video` | video mode | MODE_VIDEO |

All commands translated in 5 locales: en-US, de-DE, es-ES, fr-FR, hi-IN.

VosParser maps: `CATEGORY_MAP["cam"] = "CAMERA"`, plus 17 entries in `ACTION_MAP`.

---

## 8. Self-Running Module Pattern

PhotoAvanue follows the self-running module pattern established for all content modules:

| Composable | Source Set | Purpose | Chrome |
|-----------|-----------|---------|--------|
| `PhotoAvanueScreen` | commonMain | Standalone launch from hub | Full (TopAppBar, side panel, mode selector) |
| `CameraPreview` | androidMain | Cockpit frame embedding | Minimal (bottom bar only) |

**Hub integration:** `HubModule.kt` registers PhotoAvanue with:
- ID: `"photoavanue"`
- Display name: `"Photo"`
- Subtitle: `"Photo & video capture"`
- Accent: module accent color from `AvanueModuleAccents`

---

## 9. Build Configuration

### Plugins Required

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)    // Compose compiler
    alias(libs.plugins.compose)           // JB Compose dependency accessor
    alias(libs.plugins.kotlin.serialization)
}
```

Both `kotlin.compose` AND `compose` plugins are required for commonMain Compose code.

### Dependencies

| Source Set | Key Dependencies |
|-----------|-----------------|
| commonMain | compose.runtime, compose.foundation, compose.material3, compose.materialIconsExtended, compose.ui, AvanueUI, Foundation, Logging, kotlinx-serialization |
| androidMain | CameraX (core, camera2, lifecycle, view, video), ExifInterface, activity-compose |
| desktopMain | (inherits commonMain — future: JavaCV/webcam-capture) |

---

## 10. File Inventory

| # | File | Source Set | Lines | Purpose |
|---|------|-----------|-------|---------|
| 1 | `build.gradle.kts` | module | ~95 | Module config with CameraX + Compose deps |
| 2 | `model/CameraState.kt` | commonMain | ~25 | Root state + CameraLens/FlashMode/CaptureMode enums |
| 3 | `model/ZoomState.kt` | commonMain | ~20 | Zoom ratio model with 5-level discretization |
| 4 | `model/ExposureState.kt` | commonMain | ~20 | Exposure compensation with 5-level discretization |
| 5 | `model/RecordingState.kt` | commonMain | ~15 | Video recording lifecycle state |
| 6 | `model/AspectRatioMode.kt` | commonMain | ~5 | AUTO / 4:3 / 16:9 enum |
| 7 | `model/GpsMetadata.kt` | commonMain | ~40 | GPS coords + DMS rational conversion |
| 8 | `ICameraController.kt` | commonMain | ~30 | Platform-agnostic camera interface |
| 9 | `PhotoAvanueScreen.kt` | commonMain | ~340 | Standalone full-screen camera UI |
| 10 | `AndroidCameraController.kt` | androidMain | ~280 | CameraX implementation |
| 11 | `AndroidLocationProvider.kt` | androidMain | ~70 | Dual GPS+Network location |
| 12 | `CameraPreview.kt` | androidMain | ~295 | Embeddable camera for Cockpit |
| 13 | `AndroidProCameraController.kt` | androidMain | ~600 | Pro camera with Camera2 interop (260222: setCaptureMode fix) |

**KMP Score:** 11 commonMain / 15 total files (73% shared). 4 androidMain files are platform-specific: CameraX binding, location provider, preview surface, pro camera.

---

## 11. Future Roadmap

- **Tier 2:** ~~CameraX Extensions~~ DONE — `AndroidProCameraController` supports Bokeh/HDR/Night/FaceRetouch via `ExtensionsManager`
- **Tier 3:** ~~Camera2 Pro Controls~~ DONE — manual ISO, shutter speed, focus distance, white balance presets, RAW capture via Camera2 interop
- **Tier 4:** ARCore Depth API (depth maps, portrait bokeh from depth, AR occlusion)
- **Tier 5:** Desktop camera (JavaCV/webcam-capture), iOS AVFoundation, macOS Continuity Camera
- **Hub search integration:** Voice-searchable camera features and modes
