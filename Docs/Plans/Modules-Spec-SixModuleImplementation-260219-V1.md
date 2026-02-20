# Unified Specification: Six Module Implementation

**Document:** Modules-Spec-SixModuleImplementation-260219-V1.md
**Date:** 2026-02-19
**Branch:** Cockpit-Development
**Mode:** .yolo .swarm .cot .tot
**Scope:** AnnotationAvanue, ImageAvanue, VideoAvanue, RemoteCast, AI, AVA

---

## 1. Executive Summary

Six modules need completion: 4 media/utility Avanues (AnnotationAvanue, ImageAvanue, VideoAvanue, RemoteCast) with shell code needing full implementation, plus 2 infrastructure modules (AI, AVA) that are production-grade but need unification/migration work. All must integrate with Cockpit (already wired at FrameContent level), VoiceOSCore (voice commands + handler dispatch), and AvanueUI v5.1 (SpatialVoice design language).

### Current State

| Module | Files | KMP Score | Status | Priority |
|--------|------:|-----------|--------|----------|
| AnnotationAvanue | 3 | ~15% | Broken stroke persistence | P0 |
| ImageAvanue | 2 | ~10% | Minimal shell | P1 |
| VideoAvanue | 2 | ~10% | Basic player stub | P1 |
| RemoteCast | 2 | ~15% | Shell only | P2 |
| AI (7 sub-modules) | 316+ | ~55% | Production, needs unification | P1 |
| AVA (4 sub-modules) | ~80 | ~60% | Production, needs v5.1 migration | P2 |

---

## 2. Cross-Cutting Changes

### 2.1 ActionCategory Additions

File: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/actions/ActionCategory.kt`

Current: 15 categories (SYSTEM through CUSTOM, priorities 1-15)

**Add before CUSTOM:**

```kotlin
ANNOTATION,  // Priority 15 — drawing, whiteboard, shape tools
IMAGE,       // Priority 16 — gallery, filters, editing
VIDEO,       // Priority 17 — playback, editing, speed
CAST,        // Priority 18 — screen casting, device mirroring
```

CUSTOM moves to priority 19. Update `PRIORITY_ORDER` list accordingly.

### 2.2 CommandActionType Additions

File: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/CommandActionType.kt`

**Annotation Actions (~15):**
```
ANNOTATION_PEN, ANNOTATION_HIGHLIGHTER, ANNOTATION_SHAPE_RECT, ANNOTATION_SHAPE_CIRCLE,
ANNOTATION_SHAPE_ARROW, ANNOTATION_SHAPE_LINE, ANNOTATION_COLOR_PICKER, ANNOTATION_UNDO,
ANNOTATION_REDO, ANNOTATION_CLEAR, ANNOTATION_SAVE, ANNOTATION_SHARE, ANNOTATION_ERASER,
ANNOTATION_PEN_SIZE_UP, ANNOTATION_PEN_SIZE_DOWN
```

**Image Actions (~18):**
```
IMAGE_OPEN, IMAGE_GALLERY, IMAGE_FILTER_GRAYSCALE, IMAGE_FILTER_SEPIA, IMAGE_FILTER_BLUR,
IMAGE_FILTER_SHARPEN, IMAGE_FILTER_BRIGHTNESS, IMAGE_FILTER_CONTRAST, IMAGE_ROTATE_LEFT,
IMAGE_ROTATE_RIGHT, IMAGE_FLIP_H, IMAGE_FLIP_V, IMAGE_CROP, IMAGE_SHARE, IMAGE_DELETE,
IMAGE_INFO, IMAGE_NEXT, IMAGE_PREVIOUS
```

**Video Actions (~12):**
```
VIDEO_PLAY, VIDEO_PAUSE, VIDEO_STOP, VIDEO_SEEK_FWD, VIDEO_SEEK_BACK,
VIDEO_SPEED_UP, VIDEO_SPEED_DOWN, VIDEO_SPEED_NORMAL, VIDEO_FULLSCREEN,
VIDEO_MUTE, VIDEO_UNMUTE, VIDEO_LOOP
```

**Cast Actions (~5):**
```
CAST_START, CAST_STOP, CAST_CONNECT, CAST_DISCONNECT, CAST_QUALITY
```

**AI Actions (~5):**
```
AI_SUMMARIZE, AI_CHAT, AI_RAG_SEARCH, AI_TEACH, AI_CLEAR_CONTEXT
```

**Total: ~55 new CommandActionType values**

Add `isAnnotationAction()`, `isImageAction()`, `isVideoAction()`, `isCastAction()`, `isAIAction()` helper functions.

### 2.3 VOS Commands (en-US.app.vos)

**Annotation (~15 commands):**
```
ann_pen|pen tool|pen,draw,freehand|Select pen drawing tool
ann_highlight|highlighter|highlight,highlight tool|Select highlighter tool
ann_shape_rect|draw rectangle|rectangle,rect,box|Draw rectangle shape
ann_shape_circle|draw circle|circle,oval,ellipse|Draw circle shape
ann_shape_arrow|draw arrow|arrow,pointer|Draw arrow shape
ann_shape_line|draw line|line,straight line|Draw straight line
ann_color|color picker|pick color,change color|Open color picker
ann_undo|undo annotation|undo drawing|Undo last annotation stroke
ann_redo|redo annotation|redo drawing|Redo last undone stroke
ann_clear|clear annotations|clear all,erase all|Clear all annotations
ann_save|save annotation|save drawing,export drawing|Save annotation as image
ann_share|share annotation|send drawing|Share annotation
ann_eraser|eraser tool|eraser,erase|Switch to eraser tool
ann_pen_up|thicker pen|bigger pen,pen size up|Increase pen thickness
ann_pen_down|thinner pen|smaller pen,pen size down|Decrease pen thickness
```

**Image (~18 commands):**
```
img_open|open image|view image,show image|Open image viewer
img_gallery|open gallery|gallery,photo gallery,my photos|Open photo gallery
img_grayscale|grayscale|black and white,monochrome|Apply grayscale filter
img_sepia|sepia|vintage,old photo|Apply sepia filter
img_blur|blur image|soften,blur|Apply blur filter
img_sharpen|sharpen image|sharpen,enhance|Apply sharpen filter
img_brightness|adjust brightness|brighten image,darken image|Adjust image brightness
img_contrast|adjust contrast|more contrast,less contrast|Adjust image contrast
img_rotate_left|rotate left|turn left,rotate counter clockwise|Rotate image 90° left
img_rotate_right|rotate right|turn right,rotate clockwise|Rotate image 90° right
img_flip_h|flip horizontal|mirror,flip|Flip image horizontally
img_flip_v|flip vertical|flip upside down|Flip image vertically
img_crop|crop image|crop,trim|Enter crop mode
img_share|share image|send image|Share image
img_delete|delete image|remove image,trash image|Delete image
img_info|image info|image details,exif,metadata|Show image EXIF/metadata
img_next|next image|next photo|View next image in gallery
img_prev|previous image|previous photo,prior image|View previous image
```

**Video (~12 commands):**
```
vid_play|play video|play,resume video|Play/resume video
vid_pause|pause video|pause|Pause video playback
vid_stop|stop video|stop|Stop video playback
vid_fwd|skip forward|fast forward,skip ahead,forward 10|Skip forward 10 seconds
vid_back|skip backward|rewind,skip back,back 10|Skip backward 10 seconds
vid_faster|speed up video|faster,increase speed|Increase playback speed
vid_slower|slow down video|slower,decrease speed|Decrease playback speed
vid_normal_speed|normal speed|reset speed,one x|Reset playback to 1x speed
vid_fullscreen|fullscreen video|full screen,maximize video|Toggle fullscreen mode
vid_mute|mute video|silent,video mute|Mute video audio
vid_unmute|unmute video|video sound on|Unmute video audio
vid_loop|loop video|repeat,loop|Toggle loop playback
```

**Cast (~5 commands):**
```
cast_start|start casting|cast screen,share screen,begin cast|Start screen casting
cast_stop|stop casting|end cast,stop sharing|Stop screen casting
cast_connect|connect cast|connect device,pair device|Connect to cast device
cast_disconnect|disconnect cast|disconnect device|Disconnect from cast device
cast_quality|cast quality|streaming quality,change quality|Change cast quality
```

**AI (~5 commands):**
```
ai_summarize|summarize|ai summary,generate summary|Generate AI summary
ai_chat|ai chat|talk to ai,ask ai,open ai|Open AI chat
ai_search|ai search|search knowledge,rag search|Search knowledge base
ai_teach|teach ai|train ai,add knowledge|Teach AI new knowledge
ai_clear|clear ai context|reset ai,new conversation|Clear AI conversation context
```

**Total: ~55 new VOS commands + translations for es-ES, fr-FR, de-DE, hi-IN**

### 2.4 AndroidHandlerFactory Registration

Add 4 new handlers after `CockpitCommandHandler`:
```kotlin
AnnotationCommandHandler(service),
ImageCommandHandler(service),
VideoCommandHandler(service),
CastCommandHandler(service),
// AI already wired through Chat module — no new handler needed
```

### 2.5 Cockpit Integration

FrameContent already defines: `Image`, `Video`, `Whiteboard` (annotation), `ScreenCast`, `AiSummary`

ContentRenderer branches exist but are stubs. Each module implementation must:
1. Provide a real `@Composable` function that takes the FrameContent state
2. Wire state updates back to `CockpitRepository` for persistence
3. Respond to voice commands routed via Cockpit's command bar

---

## 3. Module Specifications

### 3.1 AnnotationAvanue

**Existing files:**
- `src/commonMain/.../model/AnnotationState.kt` — state model
- `src/androidMain/.../AnnotationCanvas.kt` — Compose Canvas drawing
- `src/androidMain/.../SignatureCapture.kt` — signature pad

**Problems:**
- Stroke persistence BROKEN — strokes serialize but don't restore (Bezier path data lost)
- No shape tools (only freehand pen)
- No color picker
- No undo/redo
- Signature capture separate from canvas (should share drawing engine)

**Target Architecture:**
```
commonMain/
  model/
    AnnotationState.kt         — State (strokes, tool, color, undo stack)
    Stroke.kt                  — Stroke data class with serializable points
    ShapeTool.kt               — Sealed class: Pen, Highlighter, Rect, Circle, Arrow, Line, Eraser
    AnnotationColor.kt         — Color presets + custom picker state
  controller/
    IAnnotationController.kt   — Interface for undo/redo/clear/save/tool selection
    AnnotationSerializer.kt    — JSON serialization for strokes (fixing persistence)
    BezierSmoother.kt          — Cubic Bezier smoothing for freehand strokes

androidMain/
  ui/
    AnnotationCanvas.kt        — Compose Canvas with gesture detection (REWRITE)
    AnnotationToolbar.kt       — Tool selection bar (pen, shapes, color, eraser, undo)
    ColorPickerSheet.kt        — Bottom sheet color picker (AvanueUI themed)
    SignatureCapture.kt        — Signature pad (extends AnnotationCanvas)
  controller/
    AndroidAnnotationController.kt — Android implementation of IAnnotationController
  handler/
    AnnotationCommandHandler.kt    — Voice command handler

desktopMain/
  controller/
    DesktopAnnotationController.kt
```

**KMP Score Target: ~65%** (drawing primitives and state in commonMain, Canvas rendering platform-specific)

**Key Design Decisions:**
- Stroke stores raw `List<Point>` + tool type + color. Rendering applies Bezier smoothing.
- Undo/redo stack in AnnotationState (immutable list of stroke operations).
- Shape tools emit a single Stroke with `shapeType` metadata — renderer draws shapes.
- Color picker: 12 presets + HSV wheel for custom.
- Persistence: JSON-encode strokes list → store in `FrameContent.Whiteboard.strokesJson`.

### 3.2 ImageAvanue

**Existing files:**
- `src/commonMain/.../model/ImageItem.kt` — basic data model
- `src/androidMain/.../ImageViewer.kt` — basic viewer

**Target Architecture:**
```
commonMain/
  model/
    ImageItem.kt               — Enhanced: uri, metadata, thumbnailUri, dateAdded
    ImageFilter.kt             — Sealed class: Grayscale, Sepia, Blur, Sharpen, Brightness, Contrast
    ImageEditorState.kt        — State: currentImage, appliedFilters, rotation, crop rect
  controller/
    IImageController.kt        — Interface: loadGallery, applyFilter, rotate, crop, share, delete

androidMain/
  ui/
    ImageViewer.kt             — REWRITE: pan/zoom, filter preview, EXIF overlay
    ImageGalleryScreen.kt      — Grid gallery with thumbnails (MediaStore query)
    ImageEditorToolbar.kt      — Filter chips, rotate/flip buttons
    ImageFilterPreview.kt      — Before/after filter comparison
  controller/
    AndroidImageController.kt  — MediaStore queries, share intent, delete
    ImageFilterEngine.kt       — RenderScript/GPU filter pipeline
  handler/
    ImageCommandHandler.kt     — Voice command handler

desktopMain/
  controller/
    DesktopImageController.kt  — File system gallery, java.awt filters
```

**KMP Score Target: ~55%** (models + filters defined in commonMain, platform-specific rendering/storage)

**Key Design Decisions:**
- Coil 3 KMP for image loading (supports Android, Desktop, iOS).
- Filters defined as data classes in commonMain. Platform implementations apply them.
- Android gallery queries MediaStore; Desktop reads file system; iOS reads PhotoKit.
- Filter pipeline: chain of `ImageFilter` transforms applied sequentially.
- Gallery screen uses LazyVerticalGrid with Coil AsyncImage.

### 3.3 VideoAvanue

**Existing files:**
- `src/commonMain/.../model/VideoItem.kt` — basic data model
- `src/androidMain/.../VideoPlayer.kt` — basic player

**Target Architecture:**
```
commonMain/
  model/
    VideoItem.kt               — Enhanced: uri, duration, thumbnailUri, codec info
    VideoPlayerState.kt        — State: playing, position, speed, volume, loop, fullscreen
    PlaybackSpeed.kt           — Enum: 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x
  controller/
    IVideoController.kt        — Interface: play, pause, seek, setSpeed, toggleLoop

androidMain/
  ui/
    VideoPlayer.kt             — REWRITE: Media3 ExoPlayer + SpatialVoice controls
    VideoControlBar.kt         — Timeline, speed, fullscreen, loop toggles
  controller/
    AndroidVideoController.kt  — Media3 ExoPlayer wrapper
  handler/
    VideoCommandHandler.kt     — Voice command handler

desktopMain/
  ui/
    DesktopVideoPlayer.kt      — Compose wrapper over javafx-media (or vlcj)
  controller/
    DesktopVideoController.kt  — JavaFX MediaPlayer wrapper
```

**KMP Score Target: ~45%** (models + state in commonMain, player is inherently platform-specific)

**Key Design Decisions:**
- Android: Media3 ExoPlayer (already a dependency for camera module).
- Desktop: JavaFX media or VLC4J (TBD — JavaFX is lighter).
- iOS: AVPlayer wrapped in SwiftUI VideoPlayer.
- Playback speeds: 0.5x to 2x in 0.25x increments.
- Seek increments: 10 seconds forward/backward by voice command.
- Fullscreen: hides Cockpit chrome, shows video with control overlay.

### 3.4 RemoteCast

**Existing files:**
- `src/commonMain/.../model/CastState.kt` — connection state model
- `src/androidMain/.../CastOverlay.kt` — overlay indicator

**Target Architecture:**
```
commonMain/
  model/
    CastState.kt              — Enhanced: connection status, device info, quality, latency
    CastDevice.kt             — Target device: id, name, address, type
    CastQualityProfile.kt     — LOW (360p/5fps), MEDIUM (720p/15fps), HIGH (1080p/30fps)
  protocol/
    ICastManager.kt           — Interface: startCast, stopCast, connect, disconnect, setQuality
    CastFrameData.kt          — Byte array frame + timestamp + sequence number

androidMain/
  service/
    CastCaptureService.kt     — Foreground service with MediaProjection capture
    ScreenCaptureHelper.kt    — MediaProjection → ImageReader → JPEG frames
  transport/
    MjpegTcpServer.kt         — TCP server streaming MJPEG frames (ktor-network)
    MjpegTcpClient.kt         — TCP client receiving MJPEG frames
  ui/
    CastOverlay.kt            — REWRITE: connection indicator + quality badge
    CastReceiverView.kt       — Composable displaying received MJPEG stream
  controller/
    AndroidCastManager.kt     — Android MediaProjection implementation
  handler/
    CastCommandHandler.kt     — Voice command handler

desktopMain/
  transport/
    DesktopMjpegServer.kt     — Java NIO TCP server
    DesktopMjpegClient.kt     — Java NIO TCP client
  controller/
    DesktopCastManager.kt     — java.awt.Robot screen capture
```

**KMP Score Target: ~35%** (protocol models in commonMain, capture APIs 100% platform-specific)

**Key Design Decisions:**
- MJPEG-over-TCP (simplest streaming protocol — no codec complexity).
- Android capture: MediaProjection API → VirtualDisplay → ImageReader → JPEG encode.
- Desktop capture: java.awt.Robot.createScreenCapture() → JPEG encode.
- Quality profiles control resolution + FPS + JPEG quality.
- Foreground service required on Android for MediaProjection.
- Device discovery: mDNS/Bonjour via existing ktor-network dependency.

### 3.5 AI Module Unification

**Current state:** 7 sub-modules (ALC, LLM, RAG, NLU, Chat, Memory, Teach) with 316+ files.

**Problems:**
- ALC and LLM both implement cloud providers (duplicate: Anthropic, OpenAI, Google, etc.)
- Memory sub-module has NO androidMain implementation
- ALC on-device inference not wired into VoiceOS 4-tier dispatch pipeline
- Cockpit AiSummary frame is a stub — no real LLM wiring

**Changes (NOT a rewrite — targeted unification):**

1. **Unify cloud providers**: ALC's `LLMProviderFactory` becomes the single factory. LLM module's duplicate providers get thin wrappers calling ALC. No code deletion — just routing consolidation.

2. **Memory androidMain**: Add `AndroidMemoryStore.kt` in `Modules/AI/Memory/src/androidMain/` using SQLDelight tables already in Database module.

3. **VoiceOS 4-tier wiring**: In `Modules/AI/Chat/src/main/.../ActionCoordinator.kt`, add dispatch branches for the 5 new AI CommandActionTypes (AI_SUMMARIZE, AI_CHAT, AI_RAG_SEARCH, AI_TEACH, AI_CLEAR_CONTEXT).

4. **Cockpit AiSummary**: In `Modules/Cockpit/src/androidMain/.../ContentRenderer.kt`, wire `FrameContent.AiSummary` to actually call ALC's `ResponseGenerator` with content from `sourceFrameIds`.

**KMP Score stays at ~55%** (mostly wiring, not new code)

### 3.6 AVA Overlay Migration

**Current state:** Overlay module at `Modules/AVA/Overlay/src/main/java/` — NON-KMP (plain Android `src/main`).

**Problems:**
- Uses deprecated glass/water components (`GlassMorphicPanel`, `GlassEffects`)
- Not KMP source sets (`src/main/java` instead of `src/androidMain/kotlin`)
- Theme references use old patterns (should use AvanueTheme v5.1)

**Changes:**

1. **Directory restructure**: Move `src/main/java/` → `src/androidMain/kotlin/` (standard KMP layout).

2. **AvanueUI v5.1 migration** in overlay composables:
   - `GlassMorphicPanel.kt` → use `AvanueTheme.glass.*` tokens
   - `VoiceOrb.kt` → use `AvanueTheme.colors.*` instead of hardcoded colors
   - `OverlayComposables.kt` → use unified components
   - `SuggestionChips.kt` → use `AvanueChip` from unified components

3. **No functional changes** — only theming and directory layout migration.

**KMP Score stays at ~60%** (core:Utils, core:Domain already KMP; Overlay stays Android-only by nature)

---

## 4. Implementation Order

1. **Cross-cutting** — ActionCategory, CommandActionType, VOS commands (all modules depend on these)
2. **AnnotationAvanue** — smallest, fixes P0 persistence bug, unblocks Cockpit whiteboard
3. **ImageAvanue** — builds on annotation overlay, needed for gallery features
4. **VideoAvanue** — similar pattern to ImageAvanue
5. **RemoteCast** — most platform-specific, depends on network transport
6. **AI** — largest scope, mostly existing code unification
7. **AVA** — migration only, least new code, can be done last

---

## 5. Library Dependencies

### New Dependencies

| Library | Module | Version | Purpose |
|---------|--------|---------|---------|
| io.coil-kt.coil3:coil-compose | ImageAvanue | 3.0.4 | KMP image loading |
| io.coil-kt.coil3:coil-network-ktor3 | ImageAvanue | 3.0.4 | Network image loading |
| androidx.media3:media3-exoplayer | VideoAvanue | 1.5.1 | Android video playback |
| androidx.media3:media3-ui | VideoAvanue | 1.5.1 | Video player UI components |
| io.ktor:ktor-network | RemoteCast | 3.0.3 | TCP networking for MJPEG |

### Existing Dependencies (already in project)

| Library | Used By |
|---------|---------|
| kotlinx-serialization-json | All (state serialization) |
| compose-multiplatform | All (UI) |
| sqldelight | AI Memory, VOS commands |
| ktor-client | AI cloud providers |

---

## 6. Testing Strategy

Each module requires:
1. **commonMain unit tests** — state models, serialization, controller logic
2. **androidMain integration tests** — platform-specific controllers
3. **VOS command validation** — all 5 locales parse correctly, handler dispatch works

Test-first priority: Annotation stroke serialization (fixing the persistence bug).

---

## 7. Voice Command Coverage Matrix

| Module | Handler | ActionCategory | Commands | Locales |
|--------|---------|---------------|----------|---------|
| AnnotationAvanue | AnnotationCommandHandler | ANNOTATION | 15 | 5 |
| ImageAvanue | ImageCommandHandler | IMAGE | 18 | 5 |
| VideoAvanue | VideoCommandHandler | VIDEO | 12 | 5 |
| RemoteCast | CastCommandHandler | CAST | 5 | 5 |
| AI | (via ActionCoordinator) | CUSTOM | 5 | 5 |
| AVA | (no new commands) | — | 0 | — |

**Total new commands: 55 × 5 locales = 275 VOS entries**

---

## 8. Cockpit Integration Checklist

For each Avanue module, verify these 5 locations:

| Location | File | What |
|----------|------|------|
| FrameContent | `Cockpit/.../FrameContent.kt` | Sealed class variant ✅ (ALL EXIST) |
| ContentRenderer | `Cockpit/.../ContentRenderer.kt` | when-branch calling module composable |
| FrameWindow icon | `Cockpit/.../FrameWindow.kt` | Icon mapping for frame title bar |
| ContentAccent | `Cockpit/.../ContentAccent.kt` | Module accent color |
| Repository fallback | `Cockpit/.../AndroidCockpitRepository.kt` | Deserialize fallback |

FrameContent already has all types defined. The remaining 4 locations need real implementations (not stubs).
