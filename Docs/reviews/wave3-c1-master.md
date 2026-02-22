# Wave 3 — Content Modules Master Analysis
**Date:** 260222 | **Session:** wave3-c1 | **Modules:** NoteAvanue, PhotoAvanue, CameraAvanue, ImageAvanue, PDFAvanue

---

## NoteAvanue

**Files:** 16 kt | **Source sets:** commonMain (9), androidMain (5), desktopMain (0 impl)
**Build:** KMP + Compose Multiplatform + compose-rich-editor + SQLDelight + AI/RAG

### Status: Substantially Complete with AVID Gaps

NoteAvanue has a real working implementation: SQLDelight persistence across notes, folders, and attachments; a compose-rich-editor-backed rich text UI; a voice dictation pipeline that routes through `NoteFormatDetector` to detect headings, bullets, numbered lists, checklists, blockquotes, and code blocks from speech prefixes; an `att://` URI scheme for attachment management with thumbnail generation; and a `NoteRAGIndexer` that bridges to the AI/RAG module for on-device semantic note search. The `ModuleCommandCallbacks.noteExecutor` pattern is correctly wired in `NoteEditor.kt` for command-mode voice formatting.

**P0/P1 Issues:**
- `NoteAvanueScreen.kt:123–219` — All interactive elements (Back, Save, Camera, Attach, Dictate) have zero AVID semantics. Zero-tolerance AVID rule violation.
- `NoteEditor.kt:149–162` — All toolbar and action buttons (Bold, Italic, Camera, Attach, Dictate, Save) have zero AVID semantics.
- `NoteEditor.kt:352–356` — `INSERT_TEXT` command uses `setMarkdown(toMarkdown() + text)` to insert dictated text, which clobbers cursor position (should use `addTextAfterSelection`).
- `NoteAvanueScreen.kt:366–379` — `HeadingButton` active-state detection compares `currentSpanStyle.fontSize` to a hardcoded `sp` value. This never matches actual heading paragraphs loaded from Markdown via the library's paragraph-level heading mechanism. All heading buttons always appear inactive.
- `desktopMain` — Declared in build.gradle.kts but **zero implementation files**. Desktop is silently non-functional.

**Notable Positives:**
- `NoteRepositoryImpl.kt` is clean: reactive Flows, proper `mapToList(Dispatchers.Default)`, safe `parseTags()` with exception wrapping, correct transaction usage in `deleteFolder()`.
- `NoteFormatDetector` is a well-structured O(1) FSM with no external dependencies — unit-testable immediately.
- `NoteDictationManager` correctly tracks voice-origin percentage and survives note reloads via `initializeFromNote()`.
- `NoteAudioRecorder` properly branches on API 24 for pause/resume, uses AAC/M4A, and handles the `MediaRecorder` API 31 constructor change. However the API 24 branch check is dead code since `minSdk = 29`.
- `NoteRAGIndexer` uses `Mutex` for concurrency safety and `SupervisorJob` for resilience.

---

## PhotoAvanue

**Files:** 15 kt | **Source sets:** commonMain (10), androidMain (4), desktopMain (0 impl)
**Build:** KMP + CameraX (core/camera2/lifecycle/view/video/extensions) + ExifInterface

### Status: Feature-Rich with ModeChip Bug and Missing Voice Wiring

PhotoAvanue is the most capable content module in scope. It provides a full CameraX pipeline with GPS EXIF tagging, 5-level discrete zoom and exposure controls, flash cycle, lens switching, pause/resume video recording, CameraX Extensions (Bokeh/HDR/Night/FaceRetouch via dynamic `ExtensionsManager` query), and a complete Camera2-interop pro mode with manual ISO, shutter speed, focus distance, white balance, RAW toggle, and stabilization modes. The `IProCameraController` interface cleanly extends `ICameraController` without breaking the base contract. `AndroidLocationProvider` is dual-provider (GPS + Network) and converts to `GpsMetadata` DMS format.

**P0/P1 Issues:**
- `PhotoAvanueScreen.kt:283,289` — `ModeChip` Photo and Video both have `onClick = { }` (empty lambda). Switching capture mode between Photo and Video does **nothing**. This is a functional regression — the camera mode cannot be changed by user interaction. Requires adding `setCaptureMode(CaptureMode)` to `ICameraController`.
- `PhotoAvanueScreen.kt:214,226,232,236` — Zoom In, Zoom Out, Exposure +, Exposure - buttons have no AVID semantics.
- `PhotoAvanueScreen.kt` — No `ModuleCommandCallbacks.photoExecutor` wiring. Despite `ICameraController` exposing `capturePhoto()`, `switchLens()`, `zoomIn()`, `zoomOut()`, `setFlashMode()`, `setZoomLevel()`, `setExposureLevel()`, none of these are reachable from the voice pipeline.
- `AndroidProCameraController.kt:370–385` — `StabilizationMode.OPTICAL` applies `LENS_OPTICAL_STABILIZATION_MODE_ON` (int = 1) via `applyCamera2Control(CONTROL_VIDEO_STABILIZATION_MODE, 1)` — wrong key. OIS uses `LENS_OPTICAL_STABILIZATION_MODE`; EIS uses `CONTROL_VIDEO_STABILIZATION_MODE`. These are separate Camera2 keys.
- `AndroidCameraController.kt:61` — `locationProvider` is public visibility, exposing internal mutable GPS state.
- `desktopMain` — Zero implementation files.

**Code Duplication:**
`AndroidCameraController` and `AndroidProCameraController` share 12 identical method bodies: `capturePhoto`, `startRecording`, `stopRecording`, `pauseRecording`, `resumeRecording`, `setFlashMode`, `zoomIn`, `zoomOut`, `setZoomLevel`, `increaseExposure`, `decreaseExposure`, `setExposureLevel`, `computeAspectRatio`. Total ~250 lines of verbatim duplication. A `BaseCameraController` extract is needed.

**Notable Positives:**
- `CameraState` / `ProCameraState` model hierarchy is clean and fully `@Serializable`.
- `ZoomState`, `ExposureState`, `IsoState`, `ShutterSpeedState`, `FocusState` all compute `normalized` (0.0–1.0 slider value) and `displayText` as computed properties — excellent UI-model separation.
- `GpsMetadata.toDms()` is a correct DMS rational-string implementation.
- `checkExtensionAvailability()` queries all four extension modes dynamically and reflects results into `CameraExtensions` state — no hardcoded capability assumptions.

---

## CameraAvanue

**Files:** 2 kt | **Source sets:** commonMain (1 — model only), androidMain (1 — monolithic composable)
**Build:** KMP + CameraX

### Status: Deprecated Predecessor — Should Be Removed

CameraAvanue is a minimal camera composable that predates PhotoAvanue. It provides photo-only capture with two flash states and front/back lens switching. It is a strict functional subset of `PhotoAvanue.CameraPreview` with the following additional defects:

**P0/P1 Issues:**
- `CameraPreview.kt:201` — Lens switch updates `lensFacing` `remember` state but the camera rebind only occurs inside `AndroidView`'s `update {}` block, which triggers on state change but asynchronously. The `ProcessCameraProvider.getInstance().addListener()` chain in `update` starts a new provider lookup on every lens switch. Camera rebind is deferred and may race against an in-progress capture.
- `CameraPreview.kt` — No `DisposableEffect` / `onDispose` to release the camera provider. The camera HAL stays bound after the composable leaves composition.
- `CameraPreview.kt` — Zero AVID semantics on Flash, Capture, or Switch Lens buttons.
- `build.gradle.kts:1–9` — Build file header says "NoteAvanue — Cross-Platform Rich Notes Module" (copy-paste error).
- Module is a strict subset of PhotoAvanue with bugs PhotoAvanue has already fixed. **Recommend deprecating CameraAvanue entirely.**

**Architecture Note:** `CameraAvanue/commonMain/CameraState.kt` duplicates `PhotoAvanue/commonMain/model/CameraState.kt` with fewer fields (no `zoom`, `exposure`, `extensions`, `pro`, `recording`). The duplication confirms this was an early prototype superseded by PhotoAvanue.

---

## ImageAvanue

**Files:** 4 kt | **Source sets:** commonMain (2), androidMain (1 — UI only), desktopMain (1 — full controller)
**Build:** KMP + Coil (androidMain only) + compose.ui (androidMain only)

### Status: Functional Viewer, Missing Android Controller Implementation

`ImageViewer.kt` (androidMain) is a well-built Compose viewer with pinch-zoom, pan, rotation, flip, double-tap reset, gallery navigation, and filter preview via `ColorMatrix`. Voice commands are correctly wired via `ModuleCommandCallbacks.imageExecutor`. `DesktopImageController.kt` is a complete implementation using `java.nio.file`, `javax.imageio`, and `java.awt.Desktop`.

**P0/P1 Issues:**
- `ImageViewer.kt:174` — `if (imageList.size > 1 || true)` — `|| true` is a debugging artifact making the navigation control bar always visible regardless of gallery size. This is production code.
- `ImageAvanue/androidMain` — No `IImageController` Android implementation. MediaStore gallery loading, share, and delete are unimplemented for Android. Only the Compose UI layer exists.
- `ImageViewer.kt:304` — `ImageFilter.BLUR` and `ImageFilter.SHARPEN` return `null` from `buildColorFilter()` — silently apply no filter with no user feedback. The enum values promise functionality they cannot deliver.

**Notable Positives:**
- `buildColorFilter()` covers six of eight filters with correct `ColorMatrix` implementations: Grayscale (`setToSaturation(0f)`), Sepia (correct 3×3 matrix), High Contrast (1.5× scale with translation), Inverted (−1 diagonal + 255 translation), Brightness Up/Down (additive offset).
- `executeImageCommand()` is correctly factored as a pure function with state passed as lambdas — enables testing without Compose dependencies.
- `IImageController` interface covers the full feature contract: gallery load, navigation, filter, rotate, flip, share, delete, reset.
- `DesktopImageController.loadGallery()` uses `Files.walk().use { }` correctly (stream closed even on exception) and reads image dimensions without full decode via `ImageReader.getWidth(0)/getHeight(0)`.

---

## PDFAvanue

**Files:** 5 kt | **Source sets:** commonMain (3), androidMain (2), desktopMain (0), iosMain (0)
**Build:** KMP + Android PdfRenderer (built-in)

### Status: Core Rendering Works; Search/Text/Voice Are Silent Stubs

`AndroidPdfEngine` provides real PDF rendering via Android's built-in `PdfRenderer` with proper `Mutex` for thread safety (PdfRenderer only allows one open page at a time). URI handling supports both `content://` and `file://` schemes. `PdfViewer.kt` composable renders pages as PNG bitmaps into a Compose `Image` with pinch-to-zoom, page navigation, and lazy rendering triggered by `LaunchedEffect(currentPage, containerWidth, containerHeight)`.

**P0/P1 Issues:**
- `AndroidPdfEngine.kt:83` — `search()` returns `emptyList()` silently. Callers cannot distinguish "no results" from "not implemented." This is a High severity data-integrity issue.
- `AndroidPdfEngine.kt:84` — `extractText()` returns `""` silently. Voice read-aloud of PDF content is impossible and callers receive no signal of failure.
- `PdfViewer.kt:122–130` — Previous, Next, Zoom In, Zoom Out buttons — zero AVID semantics. All PDF navigation is voice-blind.
- `PdfViewer.kt` — No `ModuleCommandCallbacks` voice command wiring.
- `AndroidPdfEngine.kt:29` — `password` parameter accepted but never used. `android.graphics.pdf.PdfRenderer` has no password support. The interface promises something the implementation cannot deliver.
- `PdfViewer.kt:91` — `onDispose { scope.launch { engine.closeCurrent() } }` — the `rememberCoroutineScope()` is cancelled before `onDispose` completes, so the launch is a no-op. The `ParcelFileDescriptor` is never closed, leaking the file descriptor.

**P2 Issues:**
- `PdfViewer.kt:109–110` — `remember(pageBytes) { BitmapFactory.decodeByteArray(...) }` decodes a potentially large bitmap synchronously on the composition thread.
- `PdfViewMode.TWO_PAGE_SPREAD` / `CONTINUOUS_SCROLL` and `isNightMode` are modeled in state but never consumed by the viewer.

**Notable Positives:**
- `renderMutex` in `AndroidPdfEngine` is correctly scoped: it wraps both `openPage()` and `page.close()` inside the lock, preventing concurrent access.
- `PdfPage.aspectRatio` computed property is correct.
- `LaunchedEffect(currentPage, containerWidth, containerHeight)` correctly re-renders on both page change and container resize — handles orientation changes.
- The `(containerWidth * 2).coerceAtMost(4096)` render resolution is a reasonable high-DPI strategy.

---

## Cross-Module Summary Table

| Module | Files | AVID | Voice Wiring | Desktop Impl | P0/P1 Count |
|--------|-------|------|--------------|-------------|-------------|
| NoteAvanue | 16 | Partial (editor only) | Complete (noteExecutor) | Missing | 5 |
| PhotoAvanue | 15 | Partial (~60%) | Missing (no photoExecutor) | Missing | 5 |
| CameraAvanue | 2 | None | None | None | 3 (+ deprecate) |
| ImageAvanue | 4 | Good (viewer) | Complete (imageExecutor) | Complete | 3 |
| PDFAvanue | 5 | None | None | None | 5 |

**Total findings:** 21 P0/P1 issues + 14 P2/P3 issues = 35 findings across 5 modules.

**Highest-priority actions:**
1. Fix PhotoAvanue ModeChip no-op (blocking functional regression)
2. Fix PdfViewer dispose leak (resource leak in production)
3. Surface PDFAvanue `search()`/`extractText()` stubs (silent data loss)
4. Add AVID to NoteAvanue, PhotoAvanue, PDFAvanue interactive elements
5. Wire `ModuleCommandCallbacks.photoExecutor` in PhotoAvanue
6. Deprecate CameraAvanue
