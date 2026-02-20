# Cockpit Module — Deep Code Review
**Date:** 260220
**Reviewer:** Code-Reviewer Agent
**Scope:** All 47 .kt files in `Modules/Cockpit/src/` (commonMain, androidMain, desktopMain)
**Branch:** HTTPAvanue

---

## Summary

The Cockpit module is a well-structured KMP multi-window UI system implementing 13 layout modes, 17 frame content types, and a spatial head-tracking canvas. The model layer is clean and correct — all 17 `FrameContent` sealed subclasses carry proper `@SerialName` annotations, constant values are correctly typed (`Whiteboard.backgroundColor` is Long with no missing `L` suffix unlike bugs found in sibling modules), and the command bar state machine hierarchy is sound. Rule 7 was checked across all 47 files and is fully clean — no AI, Claude, or "VOS4 Development Team" attributions appear anywhere in headers, comments, or doc strings.

The critical defects cluster in three areas: (1) a KMP portability violation in `CockpitViewModel` using `Dispatchers.Main` in commonMain code that will crash on Desktop; (2) two silent data-loss bugs in `ContentRenderer` where the AI Summary "Generate" button is a Rule 1 stub and the Signature renderer overwrites only a boolean flag while discarding all actual stroke data; and (3) a JVM data race in `SpatialViewportController` on unguarded mutable fields read by the coroutine and written by the UI thread. The `LayoutEngine` RowLayout silently swallows minimize and maximize events, presenting broken controls with no feedback to the user.

High-severity issues include pervasive wrong icons in the command bar action states (six icons wrong across web/video/note action groups), six of seventeen frame types missing from `addFrameOptions()` making them unreachable from the UI, near-total code duplication between `AndroidCockpitRepository` and `DesktopCockpitRepository` (~360 lines duplicated verbatim), a double-animation bug on the `AiSummaryPanel` spinner, missing AVID voice identifiers on `WorkflowSidebar` step rows and `MinimizedTaskbar` restore chips, and no `WebChromeClient` causing silent failure of JS dialogs, file pickers, and geolocation in web frames.

---

## Issues Table

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Critical | `viewmodel/CockpitViewModel.kt:39` | `Dispatchers.Main` used in KMP commonMain — not guaranteed on Desktop JVM; crashes at runtime | Replace with `Dispatchers.Default` or inject a dispatcher via constructor |
| Critical | `androidMain/.../content/ContentRenderer.kt:203` | `onGenerateSummary = { /* TODO: AI integration deferred */ }` — Generate button in AI Summary frames is a Rule 1 stub; silently does nothing | Wire to AI module or surface a "not available" error; do not ship a silent no-op |
| Critical | `androidMain/.../content/ContentRenderer.kt:142` | Signature renderer updates `isSigned` flag but never writes stroke data to `content.signatureData`; drawn signature is permanently lost on next recomposition | Include `signatureData = strokes` in the `content.copy(...)` call passed to `onContentStateChanged` |
| Critical | `commonMain/.../ui/LayoutEngine.kt:663` | `RowLayout` passes `onMinimize = {}` and `onMaximize = {}` no-op lambdas to every `FrameWindow`; controls appear active but silently do nothing — Rule 1 violation | Forward to the parent `onMinimize`/`onMaximize` callbacks or hide the controls when in Row layout |
| Critical | `commonMain/.../spatial/SpatialViewportController.kt:55` | `screenWidthPx` and `screenHeightPx` are plain `var` fields read inside the orientation coroutine and written from the UI thread — JVM data race, no `@Volatile` | Add `@Volatile` annotation to both fields, or replace with `AtomicReference<Pair<Float,Float>>` |
| High | `androidMain/.../repository/AndroidCockpitRepository.kt` + `desktopMain/.../repository/DesktopCockpitRepository.kt` | ~360 lines of near-identical code duplicated verbatim across both platform repository files — every method copy-pasted | Extract shared logic into a `BaseCockpitRepository` abstract class in commonMain or a shared internal helper; platform files provide only the `DatabaseDriver` |
| High | `commonMain/.../ui/CommandBar.kt:179` | "Forward" web action chip uses `Icons.Default.Language` (globe) instead of a forward-arrow icon | Use `Icons.AutoMirrored.Filled.ArrowForward` |
| High | `commonMain/.../ui/CommandBar.kt:199` | "Rewind" video action uses `Icons.AutoMirrored.Filled.ArrowBack` (backward arrow) — icon direction is indistinguishable from Play and contradicts the label | Use `Icons.Default.FastRewind` |
| High | `commonMain/.../ui/CommandBar.kt:200` | "Play/Pause" video action uses `Icons.AutoMirrored.Filled.ArrowBack` — completely wrong icon | Use `Icons.Default.PlayArrow` or a toggle composable that shows `Icons.Default.Pause` when playing |
| High | `commonMain/.../ui/CommandBar.kt:205` | "Undo" note action uses `Icons.AutoMirrored.Filled.ArrowBack` | Use `Icons.Default.Undo` |
| High | `commonMain/.../ui/CommandBar.kt:206` | "Redo" note action uses `Icons.AutoMirrored.Filled.ArrowBack` | Use `Icons.Default.Redo` |
| High | `commonMain/.../ui/CommandBar.kt:322` | `addFrameOptions()` omits six of seventeen frame types: `Voice`, `Form`, `Map`, `Widget`, `Terminal`, `AiSummary` — users cannot add these frame types from the UI at all | Add the missing entries: `"Voice" to FrameContent.Voice()`, `"Form" to FrameContent.Form()`, `"Map" to FrameContent.Map()`, `"Widget" to FrameContent.Widget()`, `"Terminal" to FrameContent.Terminal()`, `"AI Summary" to FrameContent.AiSummary()` |
| High | `androidMain/.../repository/AndroidCockpitRepository.kt:294` | `importSession()` generates frame IDs as `"${now.toEpochMilliseconds()}_${(0..99999).random()}"` — all frames in the same import batch share the same epoch-ms timestamp and only 100,000 distinct random values; ID collision is realistic for large sessions | Use `uuid4()` or `Random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE)` for the random component |
| High | `androidMain/.../ui/content/AiSummaryPanel.kt:161` | `CircularProgressIndicator` (which already animates internally in indeterminate mode) is additionally wrapped in `.rotate(rotation)` from a `rememberInfiniteTransition`; two independent spin animations compound visually into an irregular fast spin | Remove the outer `.rotate()` modifier; `CircularProgressIndicator` already provides the correct animation |
| High | `androidMain/.../content/ContentRenderer.kt:246` | `WebContentRenderer` sets no `WebChromeClient`; JS `alert()`/`confirm()` dialogs, `<input type="file">` pickers, and geolocation permission requests all silently fail in web frames | Set a `WebChromeClient` that forwards dialogs and handles file choosers |
| High | `commonMain/.../ui/WorkflowSidebar.kt:585` | `StepRow` composable uses `.clickable(onClick = onClick)` with no `semantics` block — zero AVID voice identifiers on workflow step navigation; violates AVID mandate | Add `Modifier.semantics { contentDescription = "Voice: click step ${step.title}"; role = Role.Button }` |
| High | `commonMain/.../ui/MinimizedTaskbar.kt:59` | Minimized frame restore chips use `.clickable { onRestore(frame.id) }` with no semantics block — zero AVID; minimized frames are unreachable by voice | Add `Modifier.semantics { contentDescription = "Voice: restore ${frame.title}"; role = Role.Button }` |
| Medium | `commonMain/.../ui/AiSummaryContent.kt:120` | Summary type selection chips use `.clickable { }` with no semantics block — no AVID on type selector; the androidMain `AiSummaryPanel.kt` has AVID but commonMain version does not | Add semantics to each chip: `contentDescription = "Voice: select ${type.name}"; role = Role.Button` |
| Medium | `commonMain/.../ui/FormContent.kt:226` | `CheckboxField` uses Material3 `Checkbox` with no AVID semantics; `TextInputField` at L253 uses `OutlinedTextField` with no AVID | Wrap each with a semantics modifier; for text fields use `contentDescription = "Voice: input ${field.label}"` |
| Medium | `commonMain/.../ui/FormContent.kt:277` | `emitFieldsUpdate()` calls `FormField.serializer()` on every invocation — `kotlinx.serialization` reflective serializer lookup is not free; called on every keystroke/checkbox toggle | Cache as `private val fieldSerializer = ListSerializer(FormField.serializer())` at class or top-level scope |
| Medium | `commonMain/.../ui/SpatialDiceLayout.kt:73` | Corner frames always rendered with `isSelected = false` hardcoded regardless of `selectedFrameId`; selected border highlight never appears on corner frames | Pass `isSelected = frame.id == selectedFrameId` for corner frames as is done for the center frame |
| Medium | `viewmodel/CockpitViewModel.kt:150` | `removeFrame()` updates `_frames` StateFlow and selected frame before launching the DB `deleteFrame()` coroutine; if the process is killed after UI update but before the coroutine runs, the frame survives in the DB and reappears on next load | Either perform the DB delete synchronously on a background dispatcher before updating UI state, or implement a tombstone/soft-delete mechanism |
| Medium | `androidMain/.../content/ContentRenderer.kt:374` | `calculateBoundingBox()` in `MapContentRenderer`: at zoom=1, `span=180°` producing `west = lon - 180` which can be ≤ -180° — OpenStreetMap ignores or clips invalid bbox coordinates causing a blank or mis-rendered map tile at extreme low zoom | Clamp `west`/`east`/`south`/`north` to valid geographic bounds after computing |
| Medium | `desktopMain/.../spatial/DesktopSpatialOrientationSource.kt:21` | `tracking` is a plain `var Boolean` accessed from `startTracking()` (UI thread) and the timer callback (background thread) without synchronization | Mark `@Volatile` or replace with `AtomicBoolean` |
| Medium | `commonMain/.../ui/WorkflowSidebar.kt:336` | `WorkflowPhoneLayout` shows `StepIndicatorDots` (L338) and `StepListPanel` (L345) as sibling children of `sheetContent`; when the sheet is fully expanded both are visible simultaneously — the dots are visually redundant above the full step list | Hide `StepIndicatorDots` when the sheet is in expanded state using `SheetValue.Expanded` from the `BottomSheetScaffold` state |
| Low | `CockpitConstants.kt` | `DICE_CORNER_WEIGHT = 0.45f` with comment "fraction of total area" is misleading; in `SpatialDiceLayout` the weight is halved (`DICE_CORNER_WEIGHT / 2f`) for each of two corner columns — the constant is actually the sum of both corners | Rename to `DICE_CORNERS_TOTAL_WEIGHT` or update the comment to clarify the split |
| Low | `commonMain/.../model/SessionTemplate.kt` | All 9 `BuiltInTemplates` have `autoLinks = emptyList()` — cross-frame links are defined in the model but no template leverages them | Add cross-frame links to templates where navigation between frames is meaningful (e.g., Research template: PDF ↔ Note link) |
| Low | `commonMain/.../ui/TerminalContent.kt` | `LazyColumn` with `.horizontalScroll()` on its container creates conflicting unbounded measurement constraints (documented Compose limitation); works in practice but can cause measure-layout warnings in debug builds | Consider using a custom scrollable `Column` (non-lazy) with explicit `wrapContentWidth()` for terminal content which is rarely thousands of lines long |
| Low | `commonMain/.../ui/WidgetContent.kt` | `ClockWidget` uses `delay(1000)` in a `LaunchedEffect` loop; actual tick interval is `1000ms + processing time`, causing the displayed clock to drift by a few ms per minute | Use `kotlinx.datetime.Clock.System.now()` on each tick to read actual wall time rather than accumulating delay ticks |

---

## Detailed Findings

### CRITICAL-1: `Dispatchers.Main` in KMP commonMain ViewModel

**File:** `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/viewmodel/CockpitViewModel.kt:39`

`CockpitViewModel` resides in `commonMain` but creates its coroutine scope with `Dispatchers.Main`:

```kotlin
// CockpitViewModel.kt L37-41
private val viewModelJob = SupervisorJob()
private val scope = CoroutineScope(viewModelJob + Dispatchers.Main)
```

`Dispatchers.Main` is only guaranteed to be present in environments that ship a `MainCoroutineDispatcher` — Android and iOS ship one, but Desktop JVM does not. Without a `MainCoroutineDispatcher` installed, `Dispatchers.Main` throws `IllegalStateException: Module with the Main dispatcher is missing` at runtime on Desktop.

**Fix:** Replace with `Dispatchers.Default` (safe on all KMP targets) or inject the dispatcher via constructor and supply `Dispatchers.Main` from the Android/iOS call sites:

```kotlin
// Option A — simplest, correct for all targets:
private val scope = CoroutineScope(viewModelJob + Dispatchers.Default)

// Option B — injectable for testability:
class CockpitViewModel(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ...
) {
    private val scope = CoroutineScope(viewModelJob + dispatcher)
```

---

### CRITICAL-2: AI Summary Generate Button is a Silent Stub

**File:** `Modules/Cockpit/src/androidMain/kotlin/com/augmentalis/cockpit/content/ContentRenderer.kt:199-210`

The `AiSummary` content type branch passes a no-op lambda for `onGenerateSummary`:

```kotlin
is FrameContent.AiSummary -> AiSummaryContent(
    content = content,
    onGenerateSummary = {
        // TODO: AI module integration deferred — wire to Modules/AI:LLM when ready
    },
    onContentStateChanged = { updated -> onContentStateChanged(frame.id, updated) }
)
```

When the user taps "Generate Summary" or "Regenerate" in any AI Summary frame, nothing happens. There is no error shown, no loading state triggered, no log output — the button is visually interactive but functionally dead. This is a Rule 1 violation.

**Fix:** Either wire the callback to the AI module (`Modules/AI/`) before shipping, or surface an explicit error/toast:

```kotlin
onGenerateSummary = {
    // Temporary until AI module is wired:
    onContentStateChanged(
        frame.id,
        content.copy(status = AiSummaryStatus.ERROR, errorMessage = "AI module not connected")
    )
},
```

Do not leave the callback as a silent no-op.

---

### CRITICAL-3: Signature Stroke Data Permanently Lost

**File:** `Modules/Cockpit/src/androidMain/kotlin/com/augmentalis/cockpit/content/ContentRenderer.kt:140-148`

The Signature renderer's completion callback updates only the `isSigned` boolean but discards the actual drawn strokes:

```kotlin
is FrameContent.Signature -> SignatureCaptureContent(
    content = content,
    onComplete = { strokes ->
        onContentStateChanged(
            frame.id,
            content.copy(isSigned = strokes.isNotEmpty())
            // BUG: signatureData never written — strokes are discarded here
        )
    },
    ...
)
```

`FrameContent.Signature` has a `signatureData: String?` field (the JSON-encoded stroke list), but the `copy()` call never sets it. On the next recomposition (which happens on any state change elsewhere), the composable is re-invoked with the original `content` where `signatureData` is still `null` — the drawn signature is gone. The `isSigned = true` flag persists, but there is nothing to render or export.

**Fix:**

```kotlin
onComplete = { strokes ->
    onContentStateChanged(
        frame.id,
        content.copy(
            isSigned = strokes.isNotEmpty(),
            signatureData = Json.encodeToString(strokes)  // serialize strokes
        )
    )
},
```

---

### CRITICAL-4: RowLayout Minimize and Maximize are Silent No-Ops

**File:** `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/ui/LayoutEngine.kt:655-680`

`RowLayout` constructs each `FrameWindow` with hardcoded empty lambdas for minimize and maximize:

```kotlin
FrameWindow(
    frame = frame,
    isSelected = frame.id == selectedFrameId,
    isDraggable = false,
    isResizable = false,
    onSelect = { onFrameSelected(frame.id) },
    onClose = { onFrameClose(frame.id) },
    onMinimize = {},          // SILENT NO-OP
    onMaximize = {},          // SILENT NO-OP
    ...
)
```

The yellow and green traffic-light dots render in the title bar and respond to hover/press animation, but the actions they represent are never executed. The user taps "Minimize" and nothing happens. This is a Rule 1 violation — functionality that appears available is silently broken.

The parent `LayoutEngine` composable does receive `onFrameMinimize` and `onFrameMaximize` callbacks from `CockpitScreen`. They are simply not forwarded.

**Fix:**

```kotlin
onMinimize = { onFrameMinimize(frame.id) },
onMaximize = { onFrameMaximize(frame.id) },
```

If Row layout intentionally disables these controls, hide them:

```kotlin
FrameWindow(
    ...
    showWindowControls = false,  // requires a new FrameWindow param to suppress traffic lights
    ...
)
```

---

### CRITICAL-5: Data Race on SpatialViewportController Screen Dimensions

**File:** `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/spatial/SpatialViewportController.kt:53-56`

Two fields are written from the UI thread (via `updateScreenSize()`) and read from inside the orientation coroutine launched in `connectToSource()`:

```kotlin
// Written from UI thread (CockpitScreen remembers this and calls update on resize):
private var screenWidthPx: Float = screenWidthPx
private var screenHeightPx: Float = screenHeightPx

// Read from the coroutine on every orientation event:
fun connectToSource(source: ISpatialOrientationSource, scope: CoroutineScope) {
    orientationJob = scope.launch {
        source.orientationFlow.collect { orientation ->
            val halfW = screenWidthPx / 2f   // READ — may race with write
            val halfH = screenHeightPx / 2f  // READ — may race with write
            ...
        }
    }
}
```

On the JVM, reads and writes to non-`@Volatile` `Float` fields are not guaranteed to be atomic or visible across threads. In the worst case the coroutine reads a stale value from its CPU cache, causing the viewport to briefly snap to the wrong center position after a screen resize.

**Fix:**

```kotlin
@Volatile private var screenWidthPx: Float = screenWidthPx
@Volatile private var screenHeightPx: Float = screenHeightPx
```

Or replace both fields with a single `@Volatile var screenSize: Pair<Float, Float>` to ensure the pair is read and written atomically as a reference.

---

### HIGH-1: AndroidCockpitRepository and DesktopCockpitRepository — ~360 Lines Duplicated

**Files:**
- `androidMain/.../repository/AndroidCockpitRepository.kt`
- `desktopMain/.../repository/DesktopCockpitRepository.kt`

Both files implement `ICockpitRepository` and are functionally identical. Every method — `loadSession()`, `saveSession()`, `saveFrame()`, `deleteFrame()`, `loadAllSessions()`, `deleteSession()`, `importSession()`, `exportSession()`, and all helper mappers — is a verbatim or near-verbatim copy. The only difference is how the `DatabaseDriver` is initialized (Android uses `AndroidSqliteDriver`, Desktop uses `JdbcSqliteDriver`).

This is a DRY violation of the worst kind: any bug fix or feature change applied to one file must be manually remembered and applied to the other. The `importSession()` ID collision bug (see HIGH-4 below) is present in both files.

**Fix:** Extract a `BaseCockpitRepository(driver: SqlDriver) : ICockpitRepository` into a shared `internal` class in a location accessible to both platform source sets (e.g., a `jvmMain` source set, or commonMain with `expect/actual` for the driver factory). Each platform file reduces to:

```kotlin
// androidMain
class AndroidCockpitRepository(context: Context) : BaseCockpitRepository(
    AndroidSqliteDriver(CockpitDatabase.Schema, context, "cockpit.db")
)

// desktopMain
class DesktopCockpitRepository : BaseCockpitRepository(
    JdbcSqliteDriver("jdbc:sqlite:cockpit.db").also { CockpitDatabase.Schema.create(it) }
)
```

---

### HIGH-2 through HIGH-5: Wrong Icons in CommandBar Action States

**File:** `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/ui/CommandBar.kt`

Five icons in the `CommandBarState.WEB_ACTIONS`, `VIDEO_ACTIONS`, and `NOTE_ACTIONS` states are wrong:

```kotlin
// WEB_ACTIONS L177-183
CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Back", ...)       // correct
CommandChip(Icons.Default.Language,              "Forward", ...)    // WRONG: globe icon
CommandChip(Icons.Default.Language,              "Refresh", ...)    // WRONG: globe icon (same as Forward)

// VIDEO_ACTIONS L198-202
CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Rewind", ...)     // WRONG: same as Back
CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Play/Pause", ...) // WRONG: same as Back

// NOTE_ACTIONS L204-207
CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Undo", ...)       // WRONG: same as Back
CommandChip(Icons.AutoMirrored.Filled.ArrowBack, "Redo", ...)       // WRONG: same as Back
```

In the web action bar "Forward" and "Refresh" both show the globe icon — identical to each other and meaningless for their function. In the video and note bars, "Rewind", "Play/Pause", "Undo", and "Redo" all show a backward arrow — indistinguishable from each other and from the "Back" navigation chip.

**Fix:**

```kotlin
// WEB_ACTIONS
CommandChip(Icons.AutoMirrored.Filled.ArrowBack,    "Back", ...)
CommandChip(Icons.AutoMirrored.Filled.ArrowForward, "Forward", ...)
CommandChip(Icons.Default.Refresh,                  "Refresh", ...)

// VIDEO_ACTIONS
CommandChip(Icons.Default.FastRewind,               "Rewind", ...)
CommandChip(Icons.Default.PlayArrow,                "Play/Pause", ...)
CommandChip(Icons.Default.Fullscreen,               "Fullscreen", ...)

// NOTE_ACTIONS
CommandChip(Icons.Default.Undo,                     "Undo", ...)
CommandChip(Icons.Default.Redo,                     "Redo", ...)
```

---

### HIGH-6: Six Frame Types Missing from `addFrameOptions()`

**File:** `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/ui/CommandBar.kt:322-334`

`addFrameOptions()` lists only 11 of the 17 `FrameContent` subtypes. The following are absent:

```kotlin
// MISSING — users cannot add these frame types from the UI:
// FrameContent.Voice
// FrameContent.Form
// FrameContent.Map
// FrameContent.Widget
// FrameContent.Terminal
// FrameContent.AiSummary
```

`FrameContent.Voice`, `Form`, `Map`, `Widget`, `Terminal`, and `AiSummary` are fully implemented with renderers in `ContentRenderer.kt`, have `contentTypeIcon()` mappings in `FrameWindow.kt`, and have dedicated `CommandBarState` action groups — yet the user has no way to create frames of these types from the UI.

**Fix:** Add the missing entries to `addFrameOptions()`:

```kotlin
fun addFrameOptions(): List<Pair<String, FrameContent>> = listOf(
    "Web"          to FrameContent.Web(),
    "PDF"          to FrameContent.Pdf(),
    "Image"        to FrameContent.Image(),
    "Video"        to FrameContent.Video(),
    "Note"         to FrameContent.Note(),
    "Camera"       to FrameContent.Camera(),
    "Voice Note"   to FrameContent.VoiceNote(),
    "Voice"        to FrameContent.Voice(),       // ADDED
    "Form"         to FrameContent.Form(),         // ADDED
    "Map"          to FrameContent.Map(),           // ADDED
    "Widget"       to FrameContent.Widget(),       // ADDED
    "Terminal"     to FrameContent.Terminal(),     // ADDED
    "AI Summary"   to FrameContent.AiSummary(),   // ADDED
    "Whiteboard"   to FrameContent.Whiteboard(),
    "Signature"    to FrameContent.Signature(),
    "Screen Cast"  to FrameContent.ScreenCast(),
    "External App" to FrameContent.ExternalApp()
)
```

---

### HIGH-7: `importSession()` ID Collision Risk

**File:** `androidMain/.../repository/AndroidCockpitRepository.kt:290-305` (same in DesktopCockpitRepository)

Frame IDs generated during import use:

```kotlin
val newFrameId = "${now.toEpochMilliseconds()}_${(0..99999).random()}"
```

`now` is captured once before the frame loop, so ALL frames in the same session import share the same millisecond timestamp prefix. The random suffix provides only 100,000 possible values (0–99,999). For a session with 20 frames, the probability of at least one collision is approximately `1 - (100000! / (100000-20)!) / 100000^20` ≈ 0.2%. Small but non-zero, and the DB will silently overwrite the colliding frame.

**Fix:** Use high-entropy random IDs:

```kotlin
import kotlin.random.Random
val newFrameId = Random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE).toString()
// or use a UUID library if available in commonMain
```

---

### HIGH-8: Double-Animation Spinner in AiSummaryPanel

**File:** `Modules/Cockpit/src/androidMain/kotlin/com/augmentalis/cockpit/ui/content/AiSummaryPanel.kt:155-170`

```kotlin
val rotation by rememberInfiniteTransition(label = "spinner")
    .animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        ...
    )

CircularProgressIndicator(
    modifier = Modifier
        .size(18.dp)
        .rotate(rotation),   // BUG: external rotation on top of internal animation
    ...
)
```

`CircularProgressIndicator` in indeterminate mode already renders an internal sweep animation. Adding `.rotate(rotation)` from a `rememberInfiniteTransition` (which independently cycles 0°→360°) causes the indicator's visible arc to spin at the combined rate of both animations — producing an irregular, visually jittery spinner that accelerates and decelerates unpredictably.

**Fix:** Remove the external `.rotate()` modifier entirely:

```kotlin
CircularProgressIndicator(
    modifier = Modifier.size(18.dp),  // no .rotate() needed
    color = AvanueTheme.colors.primary,
    strokeWidth = 2.dp
)
```

If a custom rotation effect is desired (e.g., for a bespoke loading icon), use a custom `Canvas`-based drawable rather than composing two competing animations.

---

### HIGH-9: No WebChromeClient in WebContentRenderer

**File:** `Modules/Cockpit/src/androidMain/kotlin/com/augmentalis/cockpit/content/ContentRenderer.kt:246-268`

```kotlin
AndroidView(
    factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() { ... }
            // NO webChromeClient set
        }
    },
    ...
)
```

Without a `WebChromeClient`, all of the following silently fail inside web frames:

- `window.alert()`, `confirm()`, `prompt()` — JS dialogs do nothing
- `<input type="file">` — file picker never opens
- `navigator.geolocation` — permission request never surfaces
- Console messages — not forwarded to logcat

This means any page relying on JS dialogs for confirmation, file upload inputs, or location services is silently broken.

**Fix:** Set a minimal `WebChromeClient`:

```kotlin
webChromeClient = object : WebChromeClient() {
    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        // forward to a Dialog or Snackbar
        result.confirm()
        return true
    }
    override fun onShowFileChooser(
        webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        // launch file picker intent
        return true
    }
}
```

---

### HIGH-10 and HIGH-11: Missing AVID on WorkflowSidebar StepRows and MinimizedTaskbar

**Files:**
- `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/ui/WorkflowSidebar.kt:585-643`
- `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/ui/MinimizedTaskbar.kt:55-75`

**WorkflowSidebar — StepRow:**

```kotlin
@Composable
private fun StepRow(
    frame: CockpitFrame,
    stepNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    ...
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)   // NO semantics block
            ...
    ) { ... }
}
```

Every workflow step is a tappable row but has no voice identifier. Voice-controlled users cannot select individual workflow steps.

**MinimizedTaskbar — frame chips:**

```kotlin
frames.filter { it.state.isMinimized }.forEach { frame ->
    Box(
        modifier = Modifier
            .clickable { onRestore(frame.id) }   // NO semantics block
            ...
    ) { ... }
}
```

Minimized frame restore buttons have no AVID. Once a frame is minimized, voice-controlled users have no way to restore it.

**Fix for both:**

```kotlin
// StepRow
Modifier
    .fillMaxWidth()
    .semantics { contentDescription = "Voice: click step $stepNumber ${frame.title}"; role = Role.Button }
    .clickable(onClick = onClick)

// MinimizedTaskbar chip
Modifier
    .semantics { contentDescription = "Voice: restore ${frame.title}"; role = Role.Button }
    .clickable { onRestore(frame.id) }
```

---

## Rule 7 Audit

All 47 `.kt` files in `Modules/Cockpit/src/` were checked for AI/Claude attribution in file headers, KDoc comments, inline comments, and function doc strings.

**Result: CLEAN — no Rule 7 violations found.**

No occurrences of:
- `Author: Claude` / `Author: AI` / `Author: AI Assistant`
- `Generated by Claude` / `AI-generated`
- `Author: VOS4 Development Team`
- Any AI co-author trailer

---

## Coverage Confirmation

All 47 files reviewed:

**commonMain — models (15):**
`CommandBarState.kt`, `SpatialPosition.kt`, `FrameContent.kt`, `CockpitFrame.kt`, `CockpitSession.kt`, `LayoutMode.kt`, `TimelineEvent.kt`, `SessionTemplate.kt`, `ContentAccent.kt`, `CrossFrameLink.kt`, `FrameState.kt`, `NoteAttachment.kt`, `PinnedFrame.kt`, `QuickAction.kt`, `WorkflowStep.kt`

**commonMain — core (2):**
`CockpitConstants.kt`, `IExternalAppResolver.kt`

**commonMain — spatial (2):**
`ISpatialOrientationSource.kt`, `SpatialViewportController.kt`

**commonMain — repository (1):**
`ICockpitRepository.kt`

**commonMain — UI (18):**
`LayoutEngine.kt`, `LayoutModeResolver.kt`, `FrameWindow.kt`, `CommandBar.kt`, `SpatialCanvas.kt`, `FreeformCanvas.kt`, `CockpitScreenContent.kt`, `CarouselLayout.kt`, `SpatialDiceLayout.kt`, `GalleryLayout.kt`, `WorkflowSidebar.kt`, `MinimizedTaskbar.kt`, `AiSummaryContent.kt`, `FormContent.kt`, `TerminalContent.kt`, `WidgetContent.kt`, `ExternalAppContent.kt`

**commonMain — viewmodel (1):**
`CockpitViewModel.kt`

**androidMain (6):**
`CockpitScreen.kt`, `ContentRenderer.kt`, `AndroidCockpitRepository.kt`, `AndroidSpatialOrientationSource.kt`, `AndroidExternalAppResolver.kt`, `AiSummaryPanel.kt`

**desktopMain (3):**
`DesktopCockpitRepository.kt`, `DesktopSpatialOrientationSource.kt`, `DesktopExternalAppResolver.kt`

---

## Recommendations

1. **Fix CRITICAL-1 first** (Dispatchers.Main) — the Desktop target will not start at all until this is addressed. One-line fix with immediate impact.

2. **Fix CRITICAL-3 next** (Signature data loss) — data-loss bugs should always be prioritized over UI glitches. Users who draw signatures and save are silently losing their work.

3. **Fix CRITICAL-2 and CRITICAL-4** (AI stub + RowLayout no-ops) — both are Rule 1 violations where the UI presents functionality that does not exist. Either implement or visually disable.

4. **Batch the CommandBar icon and addFrameOptions fixes** (HIGH-2 through HIGH-6) — they are all in the same file and can be addressed in a single commit. The missing frame types (HIGH-6) in particular block entire feature areas from ever being used.

5. **Extract BaseCockpitRepository** (HIGH-1) before adding any further persistence features — the DRY violation will compound with every new DB method.

6. **AVID pass** (HIGH-10, HIGH-11, MEDIUM-1, MEDIUM-2) — run a systematic pass over all clickable elements in the module verifying every one has a semantics block. WorkflowSidebar and MinimizedTaskbar are the worst offenders.

7. **Fix CRITICAL-5** (@Volatile on SpatialViewportController) — two-character fix that eliminates a real JVM data race.

8. **Fix HIGH-8** (AiSummaryPanel double-animation) — remove the `.rotate()` modifier; cosmetic but currently produces visually incorrect spinner behavior.
