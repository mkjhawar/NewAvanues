# Code Review: Six Feature Modules Deep Review
**Reviewer:** Code Reviewer Agent
**Date:** 260220
**Branch:** HTTPAvanue (clean, no uncommitted changes)
**Modules Reviewed:** AnnotationAvanue, ImageAvanue, VideoAvanue, NoteAvanue, PDFAvanue, PhotoAvanue, Cockpit
**Scope:** Code quality, logic/flow bugs, architecture, KMP violations, naming/conventions, theme compliance

---

## Executive Summary

Seven modules reviewed across 106 source files. Theme compliance is clean across all seven modules — no `MaterialTheme.colorScheme.*` violations found. The review surfaces **4 HIGH** bugs, **1 CRITICAL** stub, and **22 MEDIUM/LOW** findings. The most urgent items are: a KMP-breaking `String.format()` call in `VideoItem.kt` (commonMain, will not compile on iOS/native), a missing `L` suffix on `AnnotationColors.WHITE` (produces value `-1` instead of white), an ExoPlayer listener leak in `VideoPlayer.kt`, and two silently stubbed methods in `AndroidPdfEngine.kt` that always return empty results. PhotoAvanue has a significant code duplication problem between the standard and Pro camera controllers. CommandBar.kt in Cockpit uses wrong icons for several actions, which will confuse users.

---

## Module 1: AnnotationAvanue

### Summary
Well-structured KMP module. Bezier smoothing and serialization are clean. The main issues are a type constant bug causing white strokes to render incorrectly, an architectural mismatch in undo/redo when using `removeStroke`, duplicate controller code on Desktop, and minor icon confusion in the toolbar.

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `Modules/AnnotationAvanue/src/commonMain/.../model/AnnotationState.kt:63` | `AnnotationColors.WHITE = 0xFFFFFFFF` is missing the `L` suffix. Kotlin infers this as `Int`, which overflows to `-1`. Every other color in the same object correctly uses the `L` suffix (e.g., `BLACK = 0xFF000000L`). White strokes will render as fully-transparent/black. The `PRESETS` list stores `Long` but this value will be auto-widened — color `Long` value sent to Paint will be wrong. | Change to `val WHITE = 0xFFFFFFFFL` |
| MEDIUM | `Modules/AnnotationAvanue/src/androidMain/.../controller/AndroidAnnotationController.kt:44-51` | `removeStroke()` pushes the removed stroke to `undoStack`, but `undo()` (L54-63) ignores `undoStack` entirely — it pops from `strokes.last()` and pushes to `redoStack`. The `undoStack` field in `AnnotationState` is populated but never consumed. This is a silent architectural mismatch: eraser-removed strokes cannot be recovered by undo. | Either make `undo()` pop from `undoStack` when non-empty (for erase operations), or remove the `undoStack` field from `AnnotationState` entirely and document that erase is irreversible. |
| MEDIUM | `Modules/AnnotationAvanue/src/desktopMain/.../controller/DesktopAnnotationController.kt` | `addStroke`, `removeStroke`, `undo`, `redo`, `clear`, `toJson`, `fromJson`, `loadFromStrokesJson`, `toStrokesJson` are copy-pasted verbatim from `AndroidAnnotationController`. Any future bug fix in one class must be manually replicated in the other. | Extract shared logic into a `BaseAnnotationController` in `commonMain` and have both platform controllers extend it. Only the `save()` method (which uses `java.awt.Graphics2D`) needs to remain Desktop-specific. |
| MEDIUM | `Modules/AnnotationAvanue/src/desktopMain/.../controller/DesktopAnnotationController.kt` | `save()` method uses `Math.abs`, `Math.atan2`, `Math.cos`, `Math.sin` (Java static methods) instead of Kotlin equivalents (`kotlin.math.*`). Inconsistent with the rest of the Kotlin codebase. | Replace with `kotlin.math.abs`, `kotlin.math.atan2`, etc. |
| MEDIUM | `Modules/AnnotationAvanue/src/androidMain/.../AnnotationToolbar.kt` | `Icons.Default.Create` is used for both Pen and Highlighter tool buttons. Both buttons appear identical in the toolbar — users cannot visually distinguish them. | Use `Icons.Default.Edit` (or a custom highlight icon) for Highlighter. Consider `Icons.Default.Brush` for pen and `Icons.Default.Highlight` for highlighter. |
| MEDIUM | `Modules/AnnotationAvanue/src/androidMain/.../AnnotationToolbar.kt` | `Icons.Default.Circle` is used for both the Circle drawing tool and the Eraser tool. The eraser should visually indicate removal. | Use `Icons.Default.AutoFixOff` or `Icons.Default.CleaningServices` for Eraser. |
| LOW | `Modules/AnnotationAvanue/src/androidMain/.../AnnotationCanvas.kt` | `strokeCounter` is a file-level `var` (not inside a class or `remember`). While `generateStrokeId()` uses `System.currentTimeMillis()` which is already a reasonable unique key, the counter is shared across all `AnnotationCanvas` composable instances in the process. Multiple canvases simultaneously active would share and increment the same counter. | Move `strokeCounter` inside the composable using `remember`, or eliminate it since `currentTimeMillis()` alone is likely sufficient uniqueness for a single-user drawing app. |

---

## Module 2: ImageAvanue

### Summary
Solid implementation of a KMP image viewer with a JVM desktop controller. All AVID voice semantics are present. Two bugs: a dead-code `|| true` condition that renders the control bar unconditionally, and a null-returning ternary in the desktop controller that swallows an error condition silently.

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `Modules/ImageAvanue/src/androidMain/.../ImageViewer.kt:146` | `if (imageList.size > 1 \|\| true)` — the `\|\| true` clause makes the entire condition permanently true. The control bar renders even for single-image views. This is clearly leftover debug code. | Remove `\|\| true`. The original intent was `if (imageList.size > 1)`. |
| MEDIUM | `Modules/ImageAvanue/src/desktopMain/.../controller/DesktopImageController.kt:73` | `error = if (items.isEmpty() && Files.exists(galleryRoot)) null else null` — both branches of the ternary return `null`. If the gallery root exists but contains no images, the state's `error` field remains `null` rather than displaying a "No images found" message. A user opening an empty gallery will see a blank screen with no explanation. | Change to `error = if (items.isEmpty() && Files.exists(galleryRoot)) "No images found in gallery" else null` |
| MEDIUM | `Modules/ImageAvanue/src/commonMain/.../model/ImageItem.kt` | `ImageFilter.BLUR` and `ImageFilter.SHARPEN` are defined as valid enum entries but `buildColorFilter()` silently returns `null` for both (comment explains these need `RenderEffect`). However, no error state or toast feedback is surfaced to the user when they select Blur or Sharpen — the filter appears to "do nothing" with no explanation. | Either remove BLUR/SHARPEN from the enum until implemented, or have `ImageViewer` detect a `null` filter result and surface an informational message (e.g., "Blur not supported on this device"). |
| LOW | `Modules/ImageAvanue/src/desktopMain/.../controller/DesktopImageController.kt:166-178` | `shareImage()` silently no-ops if the URI is not a local file path (e.g., `content://` URIs). The function returns without setting an error state. | Add `_state.update { it.copy(error = "Cannot share this image type") }` when the file does not exist or the URI is not a local path. |

---

## Module 3: VideoAvanue

### Summary
The ExoPlayer integration is well-structured. The primary issues are a KMP-breaking `String.format()` call in `commonMain`, a player listener leak, and a minor inconsistency between a comment and actual behavior in the speed chip.

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `Modules/VideoAvanue/src/commonMain/.../model/VideoItem.kt:22-23` | `durationFormatted` calls `String.format()` in `commonMain`. `java.lang.String.format()` is a JVM API and does not exist on Kotlin/Native (iOS/native targets). This will cause a compilation failure on any non-JVM target. The `VideoItem` model is in `commonMain`, meaning it is compiled for all targets. | Replace with manual string formatting using Kotlin stdlib: `"$minutes".padStart(2, '0')` + `":"` + `"$seconds".padStart(2, '0')`, or use a `expect/actual` function. |
| HIGH | `Modules/VideoAvanue/src/androidMain/.../VideoPlayer.kt:107-116` | `LaunchedEffect(exoPlayer)` adds a `Player.Listener` via `exoPlayer.addListener(listener)` but the listener is never removed. When the composable recomposes (e.g., on orientation change or configuration change), a new `LaunchedEffect` block runs, adding another listener. Over time this accumulates multiple listeners firing duplicate state updates. The `DisposableEffect` at L126 only calls `exoPlayer.release()` — it does not call `exoPlayer.removeListener(listener)`. | Change to `DisposableEffect(exoPlayer)` and return `onDispose { exoPlayer.removeListener(listener) }` inside it, or capture `listener` as a `val` outside the effect and remove it in the existing `DisposableEffect`. |
| LOW | `Modules/VideoAvanue/src/androidMain/.../VideoPlayer.kt:224-228` | The `SpeedChip` comment says "cycles through 0.5x, 1.0x, 1.25x, 1.5x, 2.0x" (5 entries) but the `speeds` list has 6 entries: `[0.5, 0.75, 1.0, 1.25, 1.5, 2.0]`. | Update the comment to "cycles through 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x". |
| LOW | `Modules/VideoAvanue/src/desktopMain/.../controller/DesktopVideoController.kt:162-163` | `toggleFullscreen()` uses `println()` to log the fullscreen intent. No actual mechanism exists for the UI to receive this signal — the `state` is not updated. This is a no-op in practice. | Either add a `isFullscreen: Boolean` field to `VideoPlayerState` and update it here, or remove the method until the desktop fullscreen mechanism is designed. |

---

## Module 4: NoteAvanue

### Summary
NoteAvanue is the largest module and overall well-implemented. The main concerns are missing AVID semantics on several interactive elements in `NoteEditor.kt` and `NoteAvanueScreen.kt`, hardcoded `isActive = false` states that give users no visual feedback for formatting state, fragile heading detection via font size comparison, and a speech error handler that logs "will restart" without actually restarting.

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `Modules/NoteAvanue/src/androidMain/.../NoteEditor.kt` | Action buttons for camera, attach, dictate, and save are missing AVID `semantics { contentDescription = "Voice: click ..." }` modifiers. Per project CLAUDE.md, all interactive elements MUST have AVID voice identifiers. These are the primary action triggers in the editor. | Add `Modifier.semantics { contentDescription = "Voice: click [action name]" }` to each `IconButton` and `FloatingActionButton`. |
| HIGH | `Modules/NoteAvanue/src/commonMain/.../NoteAvanueScreen.kt` | TopAppBar back-navigation `IconButton` and save `IconButton` are missing AVID semantics. | Add `Modifier.semantics { contentDescription = "Voice: click back" }` and `"Voice: click save note"` respectively. |
| MEDIUM | `Modules/NoteAvanue/src/commonMain/.../NoteAvanueScreen.kt` | Blockquote button, divider button, and checklist button all have `isActive = false` hardcoded. There is no mechanism for these formatting states to be visually reflected as active even when the cursor is inside a blockquote or checklist item. The user receives no visual feedback about the current formatting context. | Implement active-state detection for these elements using `richTextState.currentParagraphStyle` or `currentSpanStyle`. The heading buttons already attempt this (though fragile — see next row). |
| MEDIUM | `Modules/NoteAvanue/src/commonMain/.../NoteAvanueScreen.kt` | `HeadingButton` computes `isActive` by comparing `richTextState.currentSpanStyle.fontSize == fontSize`. Span font size in compose-rich-editor is stored as an `sp` value, and heading styles may use paragraph-level formatting rather than span-level. This check is unreliable and will frequently return incorrect results (false negative = active heading not highlighted in toolbar). | Check the compose-rich-editor RC13 API for a paragraph-style-aware heading detection approach (e.g., `currentParagraphStyle.textAlign` or a paragraph tag API). |
| MEDIUM | `Modules/NoteAvanue/src/androidMain/.../voice/AndroidNoteVoiceRouter.kt` | `onSpeechError()` in `CONTINUOUS` mode logs `"will auto-restart"` but does not call any restart logic. If speech recognition fails mid-session the recorder simply stops with no recovery. | Call `voiceRecognitionManager.startListening(...)` inside the error handler after a brief delay, or propagate the error state to the UI so the user can manually restart. |
| MEDIUM | `Modules/NoteAvanue/src/commonMain/.../INoteController.kt` | Methods `nextHeading()`, `previousHeading()`, `deleteLine()`, `selectAll()`, `insertParagraph()` are declared in the interface but no platform implementation for these is present in any reviewed file. They appear to be unimplemented interface stubs. | Either implement these methods in `AndroidNoteController` (and desktop equivalent) or remove them from the interface until they are needed. Per the project rule, stubs are not allowed. |
| LOW | `Modules/NoteAvanue/src/commonMain/.../NoteAvanueScreen.kt` | `@Suppress("DEPRECATION")` on bullet list formatting code has no accompanying comment explaining what is deprecated or when it will be resolved. | Add an inline comment: `// TODO(YYMMDD): compose-rich-editor RC13 deprecated X — replace with Y in RC14+` |

---

## Module 5: PDFAvanue

### Summary
The core PDF rendering via `PdfRenderer` is correct and mutex-safe. However, two interface methods are silently stubbed, navigation buttons lack AVID semantics, and there are no Desktop or iOS implementations.

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| CRITICAL | `Modules/PDFAvanue/src/androidMain/.../AndroidPdfEngine.kt:83-84` | `search(query)` always returns `emptyList()` and `extractText(pageIndex)` always returns `""`. These are silent stubs — the app compiles and runs but PDF search and text copy always yield nothing. A user tapping "Find in Document" or selecting text receives empty results with no error message. Per the project zero-tolerance rule, these should not be left as stubs. | Implement text extraction using `PdfRenderer.Page.render()` with `RENDER_MODE_FOR_DISPLAY` plus Android's `android.graphics.pdf.PdfDocument` text API (API 35+), or use a third-party PDF library such as PdfBox-Android or iText for `extractText`. For `search`, implement after `extractText` is working. If the API level is insufficient, surface a specific error rather than an empty result. |
| HIGH | `Modules/PDFAvanue/src/androidMain/.../PdfViewer.kt` | Navigation prev/next, zoom in/out, and fit-to-width `IconButton` elements have no AVID `semantics { contentDescription = "Voice: click ..." }` modifiers. | Add `Modifier.semantics { contentDescription = "Voice: click [action]" }` to each control. |
| MEDIUM | `Modules/PDFAvanue/src/androidMain/.../AndroidPdfEngine.kt:59-65` | `getPage(index)` calls `r.openPage(index)` then immediately calls `page.close()` before returning. This is correct for reading metadata (width/height), but every call to `getPage` opens and closes a page descriptor. If called frequently (e.g., in a recomposition loop), this creates unnecessary overhead. | This is acceptable for metadata lookups. Add a `@note` KDoc comment clarifying that this method is metadata-only and not for rendering. Rendering is done via `renderPage`. |
| MEDIUM | `Modules/PDFAvanue/` | No `DesktopPdfEngine` or iOS PDF engine implementation exists. `IPdfEngine` is declared in `commonMain` but only one platform implementation is present. Desktop PDF viewing in Cockpit will fail silently. | Create `DesktopPdfEngine` using Apache PDFBox (`pdfbox-2.x`) in `desktopMain`. For iOS, use `PDFKit` via a native bridge in `iosMain`. |
| LOW | `Modules/PDFAvanue/src/androidMain/.../PdfViewer.kt` | The `Image` composable usage should be verified against the current Compose version. In some versions, importing `androidx.compose.foundation.Image` vs `androidx.compose.ui.graphics.painter.Painter` differs. Confirm the import resolves correctly with the project's Compose Multiplatform version (1.7.3). | Prefer `androidx.compose.foundation.layout.Box` + `coil.compose.AsyncImage` for consistency with the rest of the codebase which uses Coil. |

---

## Module 6: PhotoAvanue

### Summary
The biggest structural issue in PhotoAvanue is the full code duplication between `AndroidCameraController` and `AndroidProCameraController`. At least 14 methods are verbatim copies. A secondary issue is that mode-switching buttons in `PhotoAvanueScreen` are no-ops (empty lambdas), meaning the user cannot switch between photo and video capture modes via the UI.

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `Modules/PhotoAvanue/src/androidMain/.../PhotoAvanueScreen.kt` (ModeChip at approx. L276-288) | Mode-switching `ModeChip` buttons for Photo and Video have `onClick = { }` — empty lambdas. Tapping the mode chip does nothing. The `captureMode` state is never mutated by user interaction. This means users cannot switch between photo and video capture via the UI. | Wire the `onClick` to `controller.setCaptureMode(CaptureMode.PHOTO)` / `controller.setCaptureMode(CaptureMode.VIDEO)` and ensure `CaptureMode` state is reflected in the chip's `isActive` parameter. |
| HIGH | `Modules/PhotoAvanue/src/androidMain/.../AndroidProCameraController.kt` | `capturePhoto()`, `startRecording()`, `stopRecording()`, `pauseRecording()`, `resumeRecording()`, `switchLens()`, `setFlashMode()`, `zoomIn()`, `zoomOut()`, `setZoomLevel()`, `increaseExposure()`, `decreaseExposure()`, `setExposureLevel()`, `release()`, and `rebindCamera()` are all duplicated verbatim from `AndroidCameraController`. Any bug fix in `AndroidCameraController` must be manually applied to `AndroidProCameraController`. This is a DRY violation of significant scope. | Have `AndroidProCameraController` extend `AndroidCameraController` (or extract a common `BaseCameraController`) and override only the Camera2 interop and Extensions-specific methods (`enableExtension`, `setManualISO`, `setManualShutter`, `setWhiteBalance`, `computeAspectRatio`). |
| MEDIUM | `Modules/PhotoAvanue/src/androidMain/.../AndroidCameraController.kt:79` | `val locationProvider = AndroidLocationProvider(context)` is a public property. `locationProvider` is an internal implementation detail — it exposes the concrete Android class type and allows external callers to acquire a reference to the location subsystem via the camera controller. | Change to `private val locationProvider = AndroidLocationProvider(context)` |
| MEDIUM | `Modules/PhotoAvanue/src/androidMain/.../AndroidProCameraController.kt:79` | Same issue: `val locationProvider = AndroidLocationProvider(context)` is public. | Change to `private val locationProvider = AndroidLocationProvider(context)` |
| MEDIUM | `Modules/PhotoAvanue/src/commonMain/.../PhotoAvanueScreen.kt` | `ModeChip` composable and zoom/exposure `IconButton` elements have no AVID `semantics { contentDescription = "Voice: click ..." }` modifiers. These are primary camera controls. | Add AVID semantics to ModeChip, ZoomIn, ZoomOut, IncreaseExposure, DecreaseExposure buttons. |
| LOW | `Modules/PhotoAvanue/src/androidMain/.../AndroidProCameraController.kt` | `@Suppress("DEPRECATION")` in `computeAspectRatio()` has no accompanying comment explaining which API is deprecated. | Add an inline comment identifying the deprecated symbol and the planned replacement. |

---

## Module 7: Cockpit

### Summary
Cockpit is the largest and most complex module reviewed. The core architecture (CockpitViewModel, ICockpitRepository, ContentRenderer) is well-designed. The main action items are: incorrect icons in `CommandBar.kt` for several actions (these will cause user confusion), `FrameContent.Voice` being absent from the "Add Frame" menu, full code duplication between the Android and Desktop repository implementations, and a potential ordering issue in `deleteSession()` when creating a replacement session.

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `Modules/Cockpit/src/commonMain/.../ui/CommandBar.kt:179` | `WEB_ACTIONS` "Forward" chip uses `Icons.Default.Language` (globe icon) instead of a forward-navigation icon. The back action at the same line correctly uses `ArrowBack`. Forward and Refresh both display the globe icon, making them visually identical and indistinguishable. | Change "Forward" to `Icons.AutoMirrored.Filled.ArrowForward`. Change "Refresh" to `Icons.Default.Refresh`. |
| HIGH | `Modules/Cockpit/src/commonMain/.../ui/CommandBar.kt:200` | `VIDEO_ACTIONS` "Play/Pause" chip uses `Icons.AutoMirrored.Filled.ArrowBack` — identical to the "Rewind" chip directly above it (L199). Both controls look the same; users cannot distinguish Play/Pause from Rewind. | Change "Play/Pause" to `Icons.Default.PlayArrow` (or toggle between `PlayArrow`/`Pause` based on playback state). |
| HIGH | `Modules/Cockpit/src/commonMain/.../ui/CommandBar.kt:206` | `NOTE_ACTIONS` "Redo" chip uses `Icons.AutoMirrored.Filled.ArrowBack` — identical to the "Undo" chip at L205. Both Undo and Redo display the same left-arrow, making them indistinguishable. | Change "Redo" to `Icons.AutoMirrored.Filled.Redo` (or `Icons.Default.Redo`). |
| HIGH | `Modules/Cockpit/src/commonMain/.../ui/CommandBar.kt` (`addFrameOptions()` function) | `FrameContent.Voice` (audio-only voice frame type) is not included in the `addFrameOptions()` list. Users cannot create an audio-recording frame from the Cockpit UI. `FrameContent.VoiceNote` is present but `Voice` is a distinct type (no transcript). | Add a "Voice Recording" option to `addFrameOptions()` that creates a `FrameContent.Voice(...)` frame. The `contentTypeIcon` function already has a mapping for `FrameContent.Voice`. |
| MEDIUM | `Modules/Cockpit/src/androidMain/.../repository/AndroidCockpitRepository.kt` and `Modules/Cockpit/src/desktopMain/.../repository/DesktopCockpitRepository.kt` | Both repository implementations are nearly identical — full duplication of all SQL-backed CRUD methods, session parsing, frame parsing, JSON serialization, `importSession`, and all helper functions. Any schema change must be applied twice. | Extract shared logic into an abstract `BaseCockpitRepository` in a shared source set (or `jvmMain` since both are JVM targets), containing all SQLDelight queries and JSON serialization. Platform-specific classes override only what differs (e.g., database construction). |
| MEDIUM | `Modules/Cockpit/src/androidMain/.../repository/AndroidCockpitRepository.kt` (`importSession`) | `importSession` generates a new ID with `"${base}_${(0..99999).random()}"`. For a large batch import of many sessions simultaneously, the 5-digit random suffix (100,000 possible values) has a meaningful collision probability. | Use `UUID.randomUUID().toString()` or the existing `CockpitViewModel.generateId()` pattern (epoch millis + `Random.nextLong`) for higher entropy. |
| MEDIUM | `Modules/Cockpit/src/commonMain/.../viewmodel/CockpitViewModel.kt` | `initialize()` has no guard against double initialization. If called twice before the first coroutine completes (e.g., from two lifecycle events), two overlapping `loadSession()` flows could race, leading to inconsistent state. | Add `if (isInitialized) return` check using an `AtomicBoolean` or a `Mutex`. |
| MEDIUM | `Modules/Cockpit/src/commonMain/.../viewmodel/CockpitViewModel.kt` (`deleteSession`) | When deleting the active session and no sessions remain, the code calls `createSession()` (suspend) inside a `scope.launch` block, then directly calls `loadSession()`. Since both are suspend functions launched within the same coroutine but `createSession()` itself launches another coroutine internally, `loadSession()` may execute before `createSession()` completes — the new session ID may not yet exist in the DB when `loadSession` queries it. | Ensure `createSession()` is `await`ed (sequential) before calling `loadSession()`. Use `val newSession = repository.createSession(...)` then `loadSession(newSession.id)` in a single sequential coroutine. |
| MEDIUM | `Modules/Cockpit/src/commonMain/.../ui/FrameWindow.kt` | `resolveAccentColor()` and `contentTypeIcon()` are public top-level functions. They are Cockpit-internal helpers and expose implementation details of the accent/icon mapping to all dependents. | Mark as `internal`. |
| LOW | `Modules/Cockpit/src/androidMain/.../content/ContentRenderer.kt:205` | `// TODO: AI module integration deferred — wire to Modules/AI:LLM when ready` — tracked but open-ended. | Wire a no-op callback for now but file a specific ticket. The deferred note is acceptable as-is given it is documented in MEMORY.md as a known pending item. |
| LOW | `Modules/Cockpit/src/commonMain/.../ui/CommandBar.kt:195` | `IMAGE_ACTIONS` "Rotate" chip uses `Icons.Default.SwapHoriz` (swap arrows pointing left/right). Rotate implies circular motion, not horizontal swap. | Use `Icons.Default.RotateRight` for better semantic clarity. |

---

## Cross-Module Findings

| Severity | Finding | Affected Modules |
|----------|---------|-----------------|
| LOW | AVID voice semantics are missing on multiple interactive elements across NoteAvanue, PDFAvanue, and PhotoAvanue. VoiceOSCore and VideoAvanue are fully covered. AnnotationAvanue and ImageAvanue are covered. | NoteAvanue, PDFAvanue, PhotoAvanue |
| LOW | Theme compliance: ZERO `MaterialTheme.colorScheme.*` violations found in any of the seven reviewed modules. All modules correctly use `AvanueTheme.colors.*`. | All 7 — CLEAN |
| LOW | KMP compliance: ZERO platform imports in `commonMain` across all modules EXCEPT `VideoItem.kt` (the `String.format()` bug identified above). All other `commonMain` files are clean. | VideoAvanue — one violation |

---

## Recommendations

1. **Fix `AnnotationColors.WHITE` immediately** — it silently produces wrong color (Int `-1` vs Long `0xFFFFFFFFL`). This is a data correctness bug affecting any white annotation.

2. **Fix `VideoItem.durationFormatted`** — `String.format()` in `commonMain` will fail to compile on iOS/native targets. Replace with manual pad-start string construction before expanding to iOS.

3. **Fix the ExoPlayer listener leak** — wrap the listener in a `DisposableEffect` that calls `removeListener` on dispose. This will cause multiplying state update callbacks over the app lifecycle.

4. **Implement or remove `AndroidPdfEngine.search()` and `extractText()`** — returning empty/blank results silently violates the zero-tolerance stub rule. If the API level for text extraction is insufficient, set an error state rather than returning empty.

5. **Fix CommandBar icons** — the wrong icons for Forward (globe), Play/Pause (back arrow), and Redo (back arrow) will directly confuse users in daily use. This is a straightforward 3-line fix.

6. **Wire PhotoAvanue mode-switching** — `ModeChip onClick = { }` means users can never switch between Photo and Video mode via the UI. The controller method exists; it just needs to be called.

7. **Consolidate AndroidCameraController / AndroidProCameraController** — 15 duplicated methods is a significant maintenance debt. Extend or compose rather than duplicate.

8. **Extract BaseAnnotationController** — the Desktop and Android annotation controllers are identical except for `save()`. A shared base in `commonMain` eliminates the duplication entirely.

9. **Add AVID semantics to NoteEditor action buttons and PDFViewer navigation** — these are zero-tolerance per CLAUDE.md.

10. **Address `INoteController` unimplemented methods** — `nextHeading()`, `previousHeading()`, `deleteLine()` etc. are declared but not implemented anywhere. Per the no-stubs rule, either implement or remove from the interface.
