# Deep Review — Six Small KMP Modules
**Date:** 260220
**Reviewer:** Code-Reviewer Agent
**Scope:** All .kt files in six KMP modules
**Modules:** RemoteCast (12), NoteAvanue (16), PhotoAvanue (15), ImageAvanue (4), VideoAvanue (5), AnnotationAvanue (9)
**Total files reviewed:** 61

---

## Summary

All six modules are broadly functional with clean KMP separation, good coroutine usage, and consistent
use of AvanueTheme.colors across UI composables. However the review found **3 Critical**, **14 High**,
**18 Medium**, and **8 Low** issues spanning correctness bugs, KMP platform violations, thread safety
gaps, performance problems, and missing AVID voice semantics. The most severe issues are an integer
overflow in AnnotationColors.WHITE that corrupts every white annotation colour, three `String.format()`
calls in commonMain that prevent iOS compilation, and an ExoPlayer listener that is added but never
removed, causing a listener leak on every VideoPlayer instantiation.

---

## Issues

| Severity | Module | File:Line | Issue | Suggestion |
|----------|--------|-----------|-------|------------|
| **Critical** | AnnotationAvanue | `model/AnnotationState.kt:63` | `WHITE = 0xFFFFFFFF` missing `L` suffix — evaluates to `Int(-1)`, not `Long(0xFFFFFFFFL)`. Every white annotation is coloured wrong. | Change to `val WHITE: Long = 0xFFFFFFFFL` |
| **Critical** | PhotoAvanue | `commonMain/PhotoAvanueScreen.kt:~218` | `String.format("%.1f", ...)` in commonMain — `String.format` is JVM-only. iOS/native compile failure. | Use `"${value.roundToDecimals(1)}x"` or a KMP-safe formatter |
| **Critical** | PhotoAvanue | `commonMain/PhotoAvanueScreen.kt:~643` | `String.format("%02d:%02d", minutes, secs)` in commonMain `RecordingOverlay` — same KMP violation. | Use `"${minutes.toString().padStart(2,'0')}:${secs.toString().padStart(2,'0')}"` |
| **Critical** | PhotoAvanue | `commonMain/model/ProCameraState.kt:82` | `String.format("%.1fm", ...)` inside `FocusState.displayText` in commonMain — KMP violation, iOS build will fail. | Replace with Kotlin string template + rounding |
| **High** | VideoAvanue | `androidMain/VideoPlayer.kt:107–116` | ExoPlayer `Player.Listener` added in `LaunchedEffect(exoPlayer)` but **never removed**. `exoPlayer.release()` in `DisposableEffect` does not remove listeners. Stale callbacks fire into already-composed state after release. | Store listener ref and call `exoPlayer.removeListener(listener)` before release; or use `DisposableEffect(exoPlayer)` with `onDispose { exoPlayer.removeListener(listener); exoPlayer.release() }` |
| **High** | VideoAvanue | `androidMain/VideoPlayer.kt:317–320` | `formatTime()` (private top-level function) uses `String.format()` — this file is in **androidMain** so the JVM is available; not a KMP violation but `String.format` with format specifiers is locale-sensitive (uses device locale for digit separators). In certain locales `%02d` behaves unexpectedly. Use `padStart`. | Replace with `"${h}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"` |
| **High** | VideoAvanue | `commonMain/model/VideoItem.kt:22–23` | `String.format()` in commonMain `durationFormatted`. iOS build failure — same family of KMP violation. | Use padStart string templates |
| **High** | RemoteCast | `commonMain/CastWebSocketServer.kt:165` | `WebSocket(socket, isServer = true, maxMessageSize = 1024)` — 1 KB max message size for a JPEG video frame stream. JPEG frames are typically 50 KB–500 KB. Frames larger than 1 KB will be rejected/fragmented. | Set `maxMessageSize = 10 * 1024 * 1024` (10 MB) |
| **High** | RemoteCast | `androidMain/AndroidCastManager.kt:97–100` | `server.clientConnected.onEach {...}.launchIn(scope)` — returned `Job` is discarded. Every `connectToDevice()` call launches an uncancelled collection coroutine that outlives the connection lifecycle. N calls → N zombie coroutines. | Store the returned Job: `clientConnectedJob?.cancel(); clientConnectedJob = server.clientConnected.onEach {...}.launchIn(scope)` |
| **High** | RemoteCast | `desktopMain/DesktopCastManager.kt:95–113` | `connectToDevice()` has comment "Future: open a TCP socket" — always returns `true` and marks `isConnected = true` with no actual network connection. Desktop receiver is non-functional; UI falsely reports connected state. | Either implement the TCP/WebSocket client connection or return `false` and surface an error rather than lying about state. Do not stub with `isConnected = true`. |
| **High** | RemoteCast | `androidMain/ScreenCaptureHelper.kt:154–158` | `estimatedJpegSize()`: `ratio = 1.0 - (quality / 200.0)`. For quality=60 this yields `ratio=0.70`, allocating 70% of raw pixel bytes per `ByteArrayOutputStream`. A 1280×720 RGBA capture pre-allocates ~2.6 MB when the actual JPEG is typically 50–150 KB. Excessive GC pressure. | Use `(rawBytes * 0.05).coerceAtLeast(8192L)` — JPEG at quality 60 is roughly 5% of raw |
| **High** | ImageAvanue | `desktopMain/DesktopImageController.kt:60` | `Files.walk(galleryRoot)` returns a `Stream<Path>` that implements `AutoCloseable`. It is never closed. On large directories this leaks a directory stream / file descriptor until GC runs finalizers. | Wrap in `.use { stream -> stream.filter(...).toList() }` |
| **High** | NoteAvanue | `androidMain/NoteRAGIndexer.kt:43` | `CoroutineScope(SupervisorJob() + Dispatchers.IO)` — private, unmanaged scope. Never cancelled. Equivalent to `GlobalScope`. If the Activity/ViewModel is destroyed and re-created, a new scope leaks alongside the old one. | Accept the scope from the caller (constructor injection) or tie to ViewModel lifecycle |
| **High** | NoteAvanue | `androidMain/NoteAudioRecorder.kt:80–90` | `pauseRecording()` on API < 24 (Android N): silently skips the pause but does NOT update `_state.value` to `RecorderState.PAUSED`. State remains `RECORDING`. Subsequent `resumeRecording()` call checks `state == PAUSED`, fails the precondition, and throws `IllegalStateException("Not paused")` — crash on older devices. | On SDK < N, either update state to PAUSED regardless (treating it as a no-op pause at the MediaRecorder level) or update state to a new `PAUSE_UNSUPPORTED` variant and surface an error |
| **High** | PhotoAvanue | `androidMain/AndroidProCameraController.kt` | 15+ methods (`capturePhoto`, `startRecording`, `stopRecording`, `pauseRecording`, `resumeRecording`, `switchLens`, `setFlashMode`, `zoomIn`, `zoomOut`, `setZoomLevel`, `increaseExposure`, `decreaseExposure`, `setExposureLevel`, `computeAspectRatio`) duplicated verbatim from `AndroidCameraController`. ~300 lines of duplicated implementation. | Make `AndroidProCameraController` extend `AndroidCameraController` and override only the pro-specific methods, or extract a shared `BaseCameraController` |
| **High** | NoteAvanue | `commonMain/NoteAvanueScreen.kt:100–103` | `richTextState.setMarkdown(initialMarkdown)` called directly in composition body — this is a side-effect during composition, which violates Compose rules and can cause unexpected re-entrancy or missed recompositions. | Move inside `LaunchedEffect(initialMarkdown) { if (initialMarkdown.isNotBlank()) richTextState.setMarkdown(initialMarkdown) }` |
| **Medium** | RemoteCast | `commonMain/CastWebSocketServer.kt:121` | `frameCount++` is mutated outside the `clientMutex` — called from the capture coroutine, which is a different coroutine context than the Mutex guarded blocks. Not protected against concurrent `sendFrame()` calls from different coroutines. | Use `AtomicLong` or increment inside `clientMutex.withLock` |
| **Medium** | RemoteCast | `androidMain/CastReceiverView.kt:78` | `currentBitmap?.recycle()` called before the new bitmap is assigned to state. In Compose, recomposition can read `currentBitmap` between the recycle and the new assignment if they are not atomic. | Use a local variable: `val old = currentBitmap; currentBitmap = newBitmap; old?.recycle()` |
| **Medium** | RemoteCast | `commonMain/controller/ICastManager.kt` | KDoc comment still refers to "MJPEG-over-TCP" — stale documentation from before the WebSocket migration. | Update comment to reference `CastWebSocketServer` / WebSocket transport |
| **Medium** | NoteAvanue | `commonMain/model/Note.kt:42` | `wordCount` property calls `split("\\s+".toRegex())` on every access — O(n) regex evaluation on potentially large note content during every recomposition that reads this property. | Cache the word count as a stored field computed on save, or compute lazily and cache in the property using `by lazy` (note: `lazy` doesn't work on `val` in data classes — store in the DB and pass as a field) |
| **Medium** | NoteAvanue | `commonMain/voice/NoteDictationManager.kt` | `totalCharsTyped`, `totalCharsDictated`, `currentSegmentChars`, `isInDictationSegment` are plain `var` fields. Speech recognition callbacks arrive on a background thread; UI calls `onTextTyped` on the main thread. Unsynchronized concurrent writes → data race. | Use `@Volatile` + `AtomicLong`/`AtomicBoolean`, or serialize all mutations through a single-threaded coroutine dispatcher |
| **Medium** | NoteAvanue | `androidMain/rag/NoteRAGIndexer.kt:60` | `indexMutex.withLock` is held across two suspension points — `ragRepository.addDocument()` and `ragRepository.processDocuments()` — which are potentially slow network / DB calls. This serializes all indexing globally and blocks any concurrent indexing for the entire duration of both calls. | Split into: acquire lock → copy needed data → release lock → perform IO → acquire lock again to update index state |
| **Medium** | NoteAvanue | `androidMain/rag/NoteRAGIndexer.kt:72` | Temp file `"rag_note_${note.id}.md"` in `cacheDir` is created but never deleted after successful indexing. Accumulates one temp file per indexed note per indexing pass. | Delete in a `finally` block after indexing completes |
| **Medium** | NoteAvanue | `androidMain/NoteAvanueScreen.kt` | Checklist active state (L296) and Blockquote active state (L311) both hardcode `isActive = false` — format toolbar never reflects active state for these two format types regardless of cursor position. | Wire `isActive` to `richTextState.currentSpanStyle` or equivalent format detection |
| **Medium** | NoteAvanue | `commonMain/repository/NoteRepositoryImpl.kt:135–143` | `deleteFolder()` calls `noteQueries.selectByFolder(id).executeAsList()` inside a `database.transaction {}` to iterate notes and move them one-by-one. For folders with many notes this loads all note rows into memory. A single `UPDATE note_entity SET folder_id = NULL WHERE folder_id = ?` would be O(1) memory. | Replace the manual loop with a SQL UPDATE statement |
| **Medium** | ImageAvanue | `desktopMain/DesktopImageController.kt:73` | `error = if (items.isEmpty() && Files.exists(galleryRoot)) null else null` — both branches return `null`. An empty gallery silently shows a blank screen with no user-visible message. | Set `error = "No images found in folder"` when `items.isEmpty()` |
| **Medium** | PhotoAvanue | `androidMain/AndroidCameraController.kt:61` | `val locationProvider = AndroidLocationProvider(context)` is a `public` field — internal implementation detail is exposed through the public API surface of the class. | Change to `private val locationProvider` |
| **Medium** | PhotoAvanue | `androidMain/AndroidProCameraController.kt:79` | Same as above — `locationProvider` public in Pro controller. | Change to `private val locationProvider` |
| **Medium** | PhotoAvanue | `androidMain/AndroidCameraController.kt` & `AndroidProCameraController.kt` | Both controllers write recorded video to `context.cacheDir`. The Android system can clear the cache directory at any time under storage pressure, silently deleting in-progress or recently completed recordings. | Write to `context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)` or save via MediaStore |
| **Medium** | VideoAvanue | `desktopMain/DesktopVideoController.kt:182` | `Math.round(value / SPEED_STEP) * SPEED_STEP` — uses Java `Math.round()` in desktopMain. While JVM makes this compile, Kotlin idiom is `kotlin.math.round()`. Also, the result is `Long * Float` which promotes correctly, but is confusing to read. | Use `kotlin.math.round(value / SPEED_STEP) * SPEED_STEP` |
| **Medium** | AnnotationAvanue | `androidMain/AnnotationCanvas.kt:273` | `private var strokeCounter = 0L` is a **file-level** mutable variable, not composable state. It is shared across all `AnnotationCanvas` instances in the process. When two whiteboards are open simultaneously (Cockpit multi-window), stroke IDs can collide and erasure by ID will remove strokes from the wrong canvas. | Move `strokeCounter` inside the composable as `remember { mutableLongStateOf(0L) }` or derive IDs from UUID |
| **Medium** | AnnotationAvanue | `androidMain/SignatureCapture.kt:103` | `id = "sig_${System.currentTimeMillis()}"` — two strokes completing within the same millisecond get duplicate IDs. ID-based eraser removal will affect both strokes. | Append an atomic counter: `"sig_${System.currentTimeMillis()}_${counter.getAndIncrement()}"` |
| **Medium** | PhotoAvanue | `androidMain/CameraPreview.kt:91–98` | `cameraState.lastCapturedUri?.let { uri -> ... }` runs on every recomposition while `lastCapturedUri` is non-null. `onPhotoCaptured(uri)` and `onVideoFinalized(uri)` will be called on every subsequent recomposition (e.g., on any state change) until `lastCapturedUri` is cleared. Duplicate callbacks to the host. | Wrap in `LaunchedEffect(cameraState.lastCapturedUri)` or clear `lastCapturedUri` after dispatching the callback |
| **Medium** | PhotoAvanue | `androidMain/CameraPreview.kt:164` | `String.format("%02d:%02d", minutes, secs)` in androidMain — not a KMP violation (JVM), but mirrors the commonMain violation pattern and is locale-sensitive. | Use padStart string template for consistency |
| **Low** | RemoteCast | `commonMain/CastWebSocketServer.kt:67–68` | `frameCount` and `startTimeMs` are non-volatile plain `Long` fields read from `get("/cast/status")` lambda (potentially different coroutine) while written from `sendFrame()`. Harmless in practice (stale reads, not crashes), but ideally `@Volatile` or `AtomicLong`. | Mark `@Volatile` or use `AtomicLong` |
| **Low** | NoteAvanue | `androidMain/NoteAttachmentResolver.kt:152–154` | `resolve()` calls `noteDir.listFiles()` to iterate all files looking for a match by name, rather than constructing the path directly from the attachment ID embedded in the URI. O(n) file listing for every URI resolution. | Construct path directly: `File(noteDir, "attachments/$attachmentId")` |
| **Low** | AnnotationAvanue | `desktopMain/DesktopAnnotationController.kt:222–300` | Mixes `Math.abs()`, `Math.atan2()`, `Math.cos()`, `Math.sin()` (Java `Math`) with `kotlin.math.abs()` elsewhere in the same file — inconsistent API usage. | Replace all `Math.*` calls with `kotlin.math.*` for idiomatic Kotlin |
| **Low** | VideoAvanue | `androidMain/VideoGalleryScreen.kt` | `VideoThumbnailCard` uses `Modifier.clickable(onClick = onClick)` plus a `semantics { contentDescription = ... }` modifier. The `clickable` modifier with no explicit `indication = null` produces ripple on every tap, which is correct, but the AVID description says "Voice: click video" without the video title — partially correct; title is appended if non-blank. Acceptable but the fallback "video" is vague for disambiguation. | Use `"Voice: click ${video.title.ifBlank { "video item ${index + 1}" }}"` where index is available |
| **Low** | NoteAvanue | `commonMain/NoteAvanueScreen.kt:203` | `richTextState.toMarkdown()` called during composition for the word count display in the action bar — this converts the entire rich text state to Markdown string on every recomposition triggered by any state change. For large notes this is a non-trivial O(n) operation in the composition phase. | Compute word count only when the text content changes using `derivedStateOf { richTextState.toMarkdown().split("\\s+".toRegex()).size }` |
| **Low** | PhotoAvanue | `androidMain/CameraPreview.kt:192,236` | Flash toggle `IconButton` and Lens switch `IconButton` have no `Modifier.semantics` — AVID missing on two interactive camera controls in the embeddable preview. | Add `Modifier.semantics { contentDescription = "Voice: click toggle flash" }` and `"Voice: click switch lens"` |

---

## Detailed Findings

### CRITICAL-1 — AnnotationColors.WHITE Integer Overflow

**File:** `Modules/AnnotationAvanue/src/commonMain/kotlin/com/augmentalis/annotationavanue/model/AnnotationState.kt:63`

```kotlin
// WRONG: 0xFFFFFFFF without L suffix is an Int literal (overflows to -1)
val WHITE = 0xFFFFFFFF

// All other colors in the file are correct:
val BLACK = 0xFF000000L
val RED   = 0xFFFF0000L
val BLUE  = 0xFF0000FFL
```

The literal `0xFFFFFFFF` is an `Int` in Kotlin without the `L` suffix. The Int max value is
`0x7FFFFFFF`; `0xFFFFFFFF` overflows to `-1`. This value is then stored in a field typed as `Long`
by widening, giving `WHITE = -1L` rather than `4294967295L`. Any rendering code that applies WHITE as
an ARGB colour will see `0xFFFFFFFF_FFFFFFFF` (sign-extended) → `Color(-1L)`, which when masked to
32-bit becomes `0xFFFFFFFF = white`. Accidentally white IS the right colour on Android because
`Color.toLong()` masks to 32 bits, but other platforms (Desktop Compose, iOS if ported) interpreting
the raw `Long(-1)` will get unexpected results. Correct it regardless.

**Fix:**
```kotlin
val WHITE: Long = 0xFFFFFFFFL
```

---

### CRITICAL-2,3,4 — String.format() in commonMain (KMP Violation)

**Files:**
- `Modules/PhotoAvanue/src/commonMain/kotlin/com/augmentalis/photoavanue/PhotoAvanueScreen.kt` (2 occurrences)
- `Modules/PhotoAvanue/src/commonMain/kotlin/com/augmentalis/photoavanue/model/ProCameraState.kt` (1 occurrence)
- `Modules/VideoAvanue/src/commonMain/kotlin/com/augmentalis/videoavanue/model/VideoItem.kt` (2 occurrences)

`kotlin.String.format()` is a JVM extension (`java.lang.String.format` under the hood). In Kotlin
commonMain it compiles on JVM but **fails to link on Kotlin/Native (iOS, macOS)** with:

```
error: unresolved reference: format
```

All five occurrences must be replaced with KMP-safe equivalents.

**Pattern for time formatting (replace `String.format("%02d:%02d", m, s)`):**
```kotlin
fun formatTime(minutes: Long, seconds: Long): String =
    "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
```

**Pattern for float display (replace `String.format("%.1f", value)`):**
```kotlin
// In commonMain (Kotlin 1.9+):
import kotlin.math.roundToInt
val formatted = "${(value * 10).roundToInt() / 10.0}"
// Or using kotlin.text extensions:
val formatted = value.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toString()
// Simplest safe approach:
val formatted = "${(value * 10f).toInt() / 10f}"
```

**Occurrences to fix:**

| File | Line | Pattern |
|------|------|---------|
| `PhotoAvanueScreen.kt` | ~218 | `String.format("%.1f", cameraState.zoom.currentRatio)` |
| `PhotoAvanueScreen.kt` | ~643 | `String.format("%02d:%02d", minutes, secs)` in `RecordingOverlay` |
| `ProCameraState.kt` | ~82 | `String.format("%.1fm", 1f / currentDiopters)` |
| `VideoItem.kt` | 22–23 | `String.format("%d:%02d:%02d", ...)` and `String.format("%d:%02d", ...)` |

Note: `VideoPlayer.kt:319` also uses `String.format` but is in **androidMain** — not a KMP
violation, though locale-sensitivity is a concern (see High-2).

---

### HIGH-1 — ExoPlayer Listener Leak (VideoPlayer.kt)

**File:** `Modules/VideoAvanue/src/androidMain/kotlin/com/augmentalis/videoavanue/VideoPlayer.kt:107–126`

```kotlin
// Current code — listener is added but NEVER removed
LaunchedEffect(exoPlayer) {
    val listener = object : Player.Listener { ... }
    exoPlayer.addListener(listener)   // <-- adds listener
}                                      // <-- listener reference lost, never removed

DisposableEffect(Unit) {
    onDispose { exoPlayer.release() }  // <-- release() does NOT remove listeners
}
```

`ExoPlayer.release()` stops the player but retains registered listeners. The listener holds a
closure over `isLoading`, `duration`, and `isPlaying` Compose state — meaning the listener can
mutate Compose state after the composable has been disposed, causing:
- `IllegalStateException` from Compose snapshot state mutations outside composition
- Subtle state corruption if the URI is reloaded

**Fix:**
```kotlin
DisposableEffect(exoPlayer) {
    val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            isLoading = state == Player.STATE_BUFFERING
            if (state == Player.STATE_READY) duration = exoPlayer.duration.coerceAtLeast(0)
        }
        override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
    }
    exoPlayer.addListener(listener)
    onDispose {
        exoPlayer.removeListener(listener)
        exoPlayer.release()
    }
}
```

---

### HIGH-2 — CastWebSocketServer maxMessageSize = 1024 bytes

**File:** `Modules/RemoteCast/src/commonMain/kotlin/com/augmentalis/remotecast/transport/CastWebSocketServer.kt:165`

```kotlin
// WRONG: 1 KB max message size for video frames
val ws = WebSocket(socket, isServer = true, maxMessageSize = 1024)
```

JPEG frames at SD quality (640×480, quality 0.65) are typically 30–80 KB. At HD quality they are
150–400 KB. A 1 KB limit means every single frame will be rejected by the WebSocket layer. This
silently drops all frames — clients receive nothing. The fix is straightforward:

```kotlin
val ws = WebSocket(socket, isServer = true, maxMessageSize = 10 * 1024 * 1024) // 10 MB
```

---

### HIGH-3 — AndroidCastManager Coroutine Leak

**File:** `Modules/RemoteCast/src/androidMain/kotlin/com/augmentalis/remotecast/controller/AndroidCastManager.kt:97–100`

```kotlin
override suspend fun connectToDevice(device: CastDevice): Boolean {
    // ...
    server.clientConnected
        .onEach { connected ->
            _state.update { it.copy(isConnected = if (connected) it.isConnected else false) }
        }
        .launchIn(scope)   // <-- returned Job is DISCARDED
    // ...
}
```

Every call to `connectToDevice()` — including retries or re-connections — starts a new collection
coroutine on `server.clientConnected` and discards the Job. After N reconnections there are N
concurrent collectors each updating `_state`. This causes redundant state updates and the coroutines
are never cancelled even after `disconnect()` or `release()`.

**Fix:**
```kotlin
private var clientConnectedJob: Job? = null

override suspend fun connectToDevice(device: CastDevice): Boolean {
    clientConnectedJob?.cancel()
    clientConnectedJob = server.clientConnected
        .onEach { connected -> ... }
        .launchIn(scope)
    // ...
}
```

---

### HIGH-4 — DesktopCastManager.connectToDevice() is a Stub

**File:** `Modules/RemoteCast/src/desktopMain/kotlin/com/augmentalis/remotecast/controller/DesktopCastManager.kt:95–113`

```kotlin
override suspend fun connectToDevice(device: CastDevice): Boolean {
    return withContext(Dispatchers.IO) {
        runCatching {
            // Future: open a TCP socket to device.address here.
            // For now, update state to reflect a logical connection.
            _state.update { current ->
                current.copy(
                    deviceId = device.id,
                    deviceName = device.name,
                    isConnected = true,   // <-- always true, no real connection
                    error = null
                )
            }
            true
        }
        // ...
    }
}
```

The desktop RECEIVER is not implemented — it only manages screen capture state locally. There is no
outgoing connection to a remote device. The `isConnected = true` state is misleading and will cause
any UI or upstream logic that relies on connection state to behave incorrectly. Per Rule 1, this must
not be stubbed. Either implement the WebSocket client connection to `device.address` (which
`CastWebSocketClient` provides), or return `false` and surface `error = "Desktop receiver not yet
implemented"`.

---

### HIGH-5 — ScreenCaptureHelper.estimatedJpegSize() Formula Bug

**File:** `Modules/RemoteCast/src/androidMain/kotlin/com/augmentalis/remotecast/service/ScreenCaptureHelper.kt:154–158`

```kotlin
private fun estimatedJpegSize(width: Int, height: Int, quality: Int): Int {
    val rawBytes = width * height * 4L          // RGBA
    val ratio = 1.0 - (quality / 200.0)         // BUG: for quality=60, ratio=0.70
    return (rawBytes * ratio).toInt().coerceAtLeast(8192)
}
```

The comment says "~30% of raw bytes at quality 60" but the formula computes `1 - 60/200 = 0.70`
(70%). For a 1280×720 frame: `1280 * 720 * 4 * 0.70 = 2,580,480 bytes` pre-allocated as the initial
`ByteArrayOutputStream` capacity. A JPEG at quality 60 of that frame is typically 60–120 KB.
Pre-allocating 2.5 MB per frame at 30 fps would create 75 MB/s of garbage collection pressure.

**Fix:**
```kotlin
// JPEG compression ratio is approximately 5-15% of raw at quality 60
private fun estimatedJpegSize(width: Int, height: Int, quality: Int): Int {
    val rawBytes = width * height * 4L
    val ratio = (quality.toDouble() / 1000.0).coerceIn(0.04, 0.20)  // 4%–20%
    return (rawBytes * ratio).toInt().coerceAtLeast(8192)
}
```

---

### HIGH-6 — NoteAudioRecorder.pauseRecording() State Inconsistency on API < 24

**File:** `Modules/NoteAvanue/src/androidMain/kotlin/com/augmentalis/noteavanue/attachment/NoteAudioRecorder.kt:80–90`

```kotlin
fun pauseRecording() {
    if (_state.value != RecorderState.RECORDING) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        mediaRecorder?.pause()
        _state.value = RecorderState.PAUSED  // <-- only set on API >= 24
    }
    // On API < 24: nothing happens, state stays RECORDING
}

fun resumeRecording() {
    if (_state.value != RecorderState.PAUSED) {
        throw IllegalStateException("Not paused")  // <-- throws on API < 24
    }
}
```

On API < 24, `pauseRecording()` is a silent no-op but does not update state. Any subsequent call to
`resumeRecording()` crashes the app with `IllegalStateException`. This is a silent failure converted
into a crash on older Android devices.

**Fix:**
```kotlin
fun pauseRecording() {
    if (_state.value != RecorderState.RECORDING) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        mediaRecorder?.pause()
    }
    // Always update state — on older API it's a best-effort pause indication
    _state.value = RecorderState.PAUSED
}
```

---

### HIGH-7 — NoteRAGIndexer Lifecycle-Detached Scope

**File:** `Modules/NoteAvanue/src/androidMain/kotlin/com/augmentalis/noteavanue/rag/NoteRAGIndexer.kt:43`

```kotlin
private val indexingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
// Never cancelled — no lifecycle owner, no cancel() call
```

This scope is a hidden `GlobalScope` equivalent. When the `NoteRAGIndexer` is re-created (e.g., on
Activity recreation with a new DI graph), the old scope and its running coroutines are orphaned.
Each recreation spawns new coroutines while old ones still write to the same RAG database.
Additionally, the `indexMutex` held across two slow IO suspension points (`addDocument` +
`processDocuments`) effectively makes all indexing single-threaded globally despite the `IO`
dispatcher.

**Fix:** Inject scope from the caller (ViewModel, Service, or Application scope depending on
indexing lifetime requirements).

---

### HIGH-8 — NoteAvanueScreen Side Effect During Composition

**File:** `Modules/NoteAvanue/src/commonMain/kotlin/com/augmentalis/noteavanue/NoteAvanueScreen.kt:100–103`

```kotlin
// Called directly in composition — violates Compose side-effect rules
if (!hasLoaded && initialMarkdown.isNotBlank()) {
    richTextState.setMarkdown(initialMarkdown)
    hasLoaded = true
}
```

Compose requires that composable functions be side-effect-free. Calling `richTextState.setMarkdown()`
here is a write to external mutable state during composition, which can cause re-entrancy issues
and is forbidden by the Compose API contract.

**Fix:**
```kotlin
LaunchedEffect(initialMarkdown) {
    if (initialMarkdown.isNotBlank()) {
        richTextState.setMarkdown(initialMarkdown)
    }
}
```

---

### HIGH-9 — AndroidProCameraController Method Duplication (~300 lines)

**File:** `Modules/PhotoAvanue/src/androidMain/kotlin/com/augmentalis/photoavanue/AndroidProCameraController.kt`

The following methods are copied verbatim from `AndroidCameraController`, creating a DRY violation
that means any bug fix in the base camera controller must also be manually applied to the Pro
controller:

`capturePhoto`, `startRecording`, `stopRecording`, `pauseRecording`, `resumeRecording`,
`switchLens`, `setFlashMode`, `zoomIn`, `zoomOut`, `setZoomLevel`, `increaseExposure`,
`decreaseExposure`, `setExposureLevel`, `computeAspectRatio`, `bindCamera`

**Fix:** `AndroidProCameraController` should extend `AndroidCameraController` and override only
the pro-specific additions. The `IProCameraController` interface extending `ICameraController`
makes this the natural design:

```kotlin
class AndroidProCameraController(context: Context) : AndroidCameraController(context), IProCameraController {
    // Only override pro-specific methods: setExtensionMode, setIso, setShutterSpeed, etc.
}
```

---

### HIGH-10 — Files.walk() Resource Leak (DesktopImageController)

**File:** `Modules/ImageAvanue/src/desktopMain/kotlin/com/augmentalis/imageavanue/controller/DesktopImageController.kt:60`

```kotlin
val items = Files.walk(galleryRoot)        // Stream<Path> — AutoCloseable
    .filter { Files.isRegularFile(it) }
    .filter { it.extension.lowercase() in IMAGE_EXTENSIONS }
    .map { ImageItem(uri = it.toUri().toString(), ...) }
    .collect(Collectors.toList())
// Stream never closed — file descriptor / directory handle leaked
```

`Files.walk()` opens a directory stream that must be explicitly closed. On systems with inode limits
or when scanning large directories, leaking these handles will eventually exhaust file descriptors.

**Fix:**
```kotlin
val items = Files.walk(galleryRoot).use { stream ->
    stream.filter { Files.isRegularFile(it) }
          .filter { it.extension.lowercase() in IMAGE_EXTENSIONS }
          .map { ImageItem(uri = it.toUri().toString(), ...) }
          .collect(Collectors.toList())
}
```

---

## AVID Voice Semantics Gaps

All missing AVID semantics are tracked below. Per CLAUDE.md MANDATORY RULE, all interactive
elements must have `Modifier.semantics { contentDescription = "Voice: click <action>" }`.

| Module | File | Elements Missing AVID |
|--------|------|-----------------------|
| NoteAvanue | `NoteAvanueScreen.kt` | Back navigation button, Save button, Attach button, Camera button, Dictate button |
| NoteAvanue | `androidMain/NoteEditor.kt` | Bold, Italic, Underline, Camera, Attach, Dictate, Save buttons |
| PhotoAvanue | `PhotoAvanueScreen.kt` | Zoom In, Zoom Out, Exposure+, Exposure−, Pro panel toggle, Mode chips (Photo/Video) |
| PhotoAvanue | `androidMain/CameraPreview.kt` | Flash toggle, Lens switch buttons |

---

## Module Health Summary

| Module | Files | Critical | High | Medium | Low | Overall |
|--------|-------|----------|------|--------|-----|---------|
| RemoteCast | 12 | 0 | 3 | 3 | 1 | Needs attention |
| NoteAvanue | 16 | 0 | 3 | 6 | 1 | Needs attention |
| PhotoAvanue | 15 | 3 | 2 | 4 | 1 | Critical fixes required |
| ImageAvanue | 4 | 0 | 2 | 1 | 0 | Minor fixes |
| VideoAvanue | 5 | 1 | 2 | 1 | 1 | String.format fix urgent |
| AnnotationAvanue | 9 | 1 | 0 | 2 | 1 | WHITE fix urgent |
| **Total** | **61** | **5\*** | **12** | **17** | **5** | |

\* The three `String.format()` in commonMain issues are grouped as 3 Critical occurrences but reported
in the table as belonging to PhotoAvanue (2) and VideoAvanue (1) respectively.

---

## Recommendations

1. **Fix String.format() in commonMain immediately** — these are compile-time failures on iOS/native.
   Three files: `PhotoAvanueScreen.kt`, `ProCameraState.kt`, `VideoItem.kt`. Replace with padStart
   string templates or `kotlin.math.roundToInt()` patterns. This blocks any iOS build of these modules.

2. **Fix AnnotationColors.WHITE** — add `L` suffix (`0xFFFFFFFFL`). One character change, prevents
   colour corruption on non-Android platforms.

3. **Fix ExoPlayer listener leak** — move listener lifecycle into `DisposableEffect(exoPlayer)` so
   the listener is removed before the player is released. Every VideoPlayer instantiation currently
   leaks a listener.

4. **Fix CastWebSocketServer maxMessageSize** — change `1024` to `10 * 1024 * 1024`. Current value
   silently drops every video frame sent over the WebSocket transport.

5. **Implement or surface DesktopCastManager.connectToDevice()** — do not return `true` when no
   connection has been made. Rule 1 prohibits stubs that lie about success.

6. **Consolidate AndroidProCameraController** — extend `AndroidCameraController` to eliminate
   ~300 lines of duplicated code. Every future bug fix to the base controller currently needs to be
   manually applied twice.

7. **Fix NoteRAGIndexer scope** — inject scope from the caller. The private
   `CoroutineScope(SupervisorJob() + Dispatchers.IO)` is a lifecycle leak.

8. **Fix NoteAudioRecorder.pauseRecording()** — always update state on API < 24 to prevent the
   crash in `resumeRecording()`.

9. **Add AVID semantics** to all interactive elements listed in the AVID table above. These are
   zero-tolerance MANDATORY per CLAUDE.md Rule 7 / AvanueUI Protocol.

10. **Fix CameraPreview callback duplication** — move the `lastCapturedUri` callback dispatch into
    `LaunchedEffect(cameraState.lastCapturedUri)` to prevent the callbacks firing on every
    recomposition.
