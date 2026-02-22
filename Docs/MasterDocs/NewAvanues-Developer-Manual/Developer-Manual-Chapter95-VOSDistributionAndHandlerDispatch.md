# Chapter 95: VOS Distribution System & Handler Dispatch Architecture

## 1. VOS Domain Split (v2.1)

### Why Split?

VOS v2.0 shipped 107 commands as a single monolithic file per locale. Three problems:

1. **Translation quality**: App commands ("go back", "open settings") need native speaker crowd-sourcing, but web gesture terms ("pan left", "orbit") are universal technical vocabulary
2. **No sharing**: Web commands scraped from websites live only in the local session DB — users can't share them
3. **No version control**: No way to track what VOS files exist, their versions, or prevent duplicates

### Domain Classification

| Domain | File Extension | Prefixes | Count | Nature |
|--------|---------------|----------|-------|--------|
| App | `.app.vos` | nav, media, sys, voice, app, acc, text, input, appctl | 62 | Locale-specific, crowd-sourceable |
| Web | `.web.vos` | browser, gesture | 45 | Universal technical terms |

### VOS v2.1 Format (Legacy JSON)

New field `"domain"` in root JSON:

```json
{
  "version": "2.1",
  "locale": "en-US",
  "fallback": "en-US",
  "domain": "app",
  "category_map": { ... },
  "action_map": { ... },
  "meta_map": { ... },
  "commands": [ ... ]
}
```

Web files additionally include provenance metadata when exported:
```json
{
  "domain": "web",
  "source_domain": "google.com",
  "page_title": "Google Search",
  "url_patterns": ["https://google.com/*"]
}
```

### VOS v3.0 Format (Current — Compact)

v3.0 replaces the verbose JSON with a pipe-delimited text format. Three locale-independent maps (`CATEGORY_MAP`, `ACTION_MAP`, `META_MAP`) are compiled as Kotlin constants in `VosParser`, eliminating ~25 KB of per-file duplication.

```
# VOS v3.0 — en-US app commands
# Copyright (c) 2026 Manoj Jhawar, Aman Jhawar — Intelligent Devices LLC
VOS:3.0:en-US:en-US:app

nav_back|go back|navigate back,back,previous screen|Navigate to previous screen
nav_home|go home|home,navigate home,open home|Go to home screen
media_play|play music|play,resume|Play/resume media
```

**Header:** `VOS:{version}:{locale}:{fallback}:{domain}`

**Command:** `{action_id}|{primary_phrase}|{synonyms_csv}|{description}`

**Size reduction:** 57% average across all 10 seed files (128 KB → 55 KB).

**Auto-detection:** `VosParser.parse()` detects format by first non-whitespace character: `{` = JSON v2.1, `#` or `VOS:` = compact v3.0. Both formats work indefinitely.

**Compiled maps in VosParser:**
- `CATEGORY_MAP`: 10 entries (prefix → CommandCategory)
- `ACTION_MAP`: 124 entries (action_id → CommandActionType)
- `META_MAP`: 22 entries (gesture direction/scale/factor metadata)

### CommandLoader Dual-File Loading

`CommandLoader.loadLocale()`:
1. Opens `{locale}.app.vos` from assets
2. Opens `{locale}.web.vos` from assets
3. Parses both via `VosParser.parse()` (auto-detects JSON v2.1 or compact v3.0)
4. Maps `VosParsedCommand` → `VoiceCommandEntity` via `toEntity()`
5. Merges command lists
6. Single `insertBatch()` to DB

Version `requiredVersion = "3.0"` — forces DB reload on app upgrade from v2.1.

**CRITICAL (260212)**: VOS files are now the ONLY source of truth. The hardcoded fallback command lists (~800 lines) have been removed from `StaticCommandRegistry`. If the DB is not initialized, `StaticCommandRegistry.all()` returns an empty list. This enforces `.VOS → DB` as the single source and eliminates dual-source maintenance burden.

```kotlin
companion object {
    const val FILE_EXTENSION_APP = ".app.vos"
    const val FILE_EXTENSION_WEB = ".web.vos"
}
```

`getAvailableLocales()` scans `.app.vos` files and intersects with `.web.vos` to return only complete locale sets.

### Parser Architecture

| Parser | Package | Format | Status |
|--------|---------|--------|--------|
| `VosParser` | commonMain (KMP) | v2.1 JSON + v3.0 compact | **Active** — sole parser, auto-detects format |
| ~~`ArrayJsonParser`~~ | ~~androidMain~~ | ~~v2.1 JSON only~~ | **DELETED 260216** — replaced by VosParser |
| ~~`UnifiedJSONParser`~~ | ~~androidMain~~ | ~~`commands-all.json`~~ | **DELETED 260216** — parsed non-existent file, zero callers |

## 2. VOS File Registry

### Purpose

Track all VOS files (bundled + exported + downloaded) with full provenance metadata for version control, deduplication, and future FTP sync.

### Schema (SQLDelight)

**File**: `Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/VosFileRegistry.sq`

| Column | Type | Purpose |
|--------|------|---------|
| id | INTEGER PK | Auto-increment |
| file_id | TEXT | Domain (web) or locale (app) |
| file_type | TEXT | "app" or "web" |
| file_name | TEXT | e.g., "google.com.web.vos" |
| content_hash | TEXT | SHA-256 for dedup |
| command_count | INTEGER | Number of commands |
| vos_version | TEXT | "2.1" |
| domain | TEXT | Website domain (web only) |
| page_title | TEXT | Page title at scrape time |
| url_patterns | TEXT | JSON array of URL patterns |
| uploader_device_id | TEXT | Anonymized device fingerprint |
| user_agent | TEXT | Browser user agent |
| scrape_duration_ms | INTEGER | Scrape timing |
| scraped_at | INTEGER | When scraped/created |
| uploaded_at | INTEGER | When uploaded to FTP |
| downloaded_at | INTEGER | When downloaded from FTP |
| source | TEXT | 'local', 'downloaded', 'bundled' |
| local_path | TEXT | File path on device |
| is_active | INTEGER | Currently in use? |
| version | INTEGER | Incremental per fileId |

**UNIQUE constraint**: `(file_id, file_type, version)`

### Repository

**Interface**: `IVosFileRegistryRepository` (15 methods)
**Implementation**: `SQLDelightVosFileRegistryRepository`
**Access**: `VoiceOSDatabaseManager.vosFileRegistry`

Key operations:
- `existsByHash(hash)` — dedup check before import/export
- `getLatestVersion(fileId, fileType)` — version tracking
- `deactivateOldVersions(fileId, fileType, keepVersion)` — version management
- `getByDomain(domain)` — find web VOS files for a domain

## 3. VOS Export/Import

### VosFileExporter

**File**: `VoiceOSCore/src/androidMain/.../vos/VosFileExporter.kt`

Serializes `VoiceCommandEntity` lists to VOS v2.1 JSON format.

```kotlin
class VosFileExporter(context: Context, registry: IVosFileRegistryRepository) {
    suspend fun exportAppCommands(locale: String, commands: List<VoiceCommandEntity>): ExportResult
    suspend fun exportWebCommands(domain: String, pageTitle: String, commands: List<VoiceCommandEntity>, ...): ExportResult
}
```

- Saves to `Downloads/commands/{filename}`
- Computes SHA-256 content hash
- Skips export if identical hash already registered
- Auto-increments version per fileId
- Deactivates old versions on new export

### VosFileImporter

**File**: `VoiceOSCore/src/androidMain/.../vos/VosFileImporter.kt`

Parses VOS files and batch-inserts into the voice command DB.

```kotlin
class VosFileImporter(registry: IVosFileRegistryRepository, commandDao: VoiceCommandDaoAdapter) {
    suspend fun importFromFile(filePath: String): ImportResult
    suspend fun importFromContent(content: String, source: String): ImportResult
    suspend fun isDuplicate(contentHash: String): Boolean
}
```

- Dedup by SHA-256 before parsing
- Uses `VosParser.parse()` for parsing (auto-detects JSON v2.1 or compact v3.0)
- Maps `VosParsedCommand` → `VoiceCommandEntity` via `toEntity()`
- Batch insert via `VoiceCommandDaoAdapter.insertBatch()`
- Auto-detects file type (app/web) from filename or parsed `domain` from VosParseResult

## 4. Static Command Dispatch Architecture

### Problem Solved

VOS seed files defined 107+ commands across 11 categories, but originally only 4 handlers existed (AndroidGestureHandler, SystemHandler, AppHandler, AndroidCursorHandler). 7 new handlers were added in v2.1, then NoteCommandHandler (v2.2) and CockpitCommandHandler (v2.3) brought the total to 13. Wave 4 added AnnotationCommandHandler, ImageCommandHandler, VideoCommandHandler, CastCommandHandler, and AICommandHandler, bringing the total to **18 handlers**. All handler categories are fully covered.

### Handler Coverage (18 Handlers)

| Handler | Category | Commands | Key API | Status |
|---------|----------|----------|---------|--------|
| AndroidGestureHandler | NAVIGATION | scroll, tap, swipe, pinch | GestureDescription API | v1.0 |
| SystemHandler | SYSTEM | back, home, recents, split screen | performGlobalAction() | v1.0 |
| AppHandler | APP | open browser/camera/gallery/etc. | Intent + PackageManager | v1.0 |
| AndroidCursorHandler | GAZE | cursor show/hide/click | CursorOverlayService + IMUManager | v1.0 (IMU wiring v2.2) |
| **MediaHandler** | MEDIA | play, pause, next, prev, volume (13 cmds) | AudioManager + KeyEvent | v2.1 |
| **ScreenHandler** | DEVICE | brightness, wifi, bluetooth, screenshot, flashlight (20 cmds) | Settings.System + CameraManager | v2.1 |
| **TextHandler** | INPUT | select all, copy, paste, cut, undo, redo, delete (8 cmds) | AccessibilityNodeInfo actions | v2.2 |
| **InputHandler** | INPUT | show/hide keyboard (6 cmds) | SoftKeyboardController | v2.1 |
| **AppControlHandler** | APP | close app, exit, quit (4 cmds) | GLOBAL_ACTION_BACK + HOME | v2.1 |
| **ReadingHandler** | ACCESSIBILITY | read screen, stop reading (7 cmds) | TextToSpeech + tree traversal | v2.1 |
| **VoiceControlHandler** | SYSTEM | mute/wake, dictation mode switch, help, list commands (16 cmds) | VoiceControlCallbacks + OverlayStateManager feedback | v2.4 |
| **WebCommandHandler** | BROWSER | 45 web commands | IWebCommandExecutor + DOMScraperBridge | v2.1 |
| **NoteCommandHandler** | NOTE | 23 note commands (format, dictate, navigate) | INoteController + RichTextState | v2.2 |
| **CockpitCommandHandler** | COCKPIT | 20 cockpit commands (frames, layouts, content) | Intent broadcast to CockpitViewModel | v2.3 |
| **AnnotationCommandHandler** | ANNOTATION | 14 annotation commands (draw, colors, undo, save) | ModuleCommandCallbacks.annotationExecutor | v2.4 |
| **ImageCommandHandler** | IMAGE | 16 image commands (rotate, flip, zoom, filter, gallery) | ModuleCommandCallbacks.imageExecutor | v2.3 |
| **VideoCommandHandler** | VIDEO | 12 video commands (play, pause, seek, speed, fullscreen) | ModuleCommandCallbacks.videoExecutor | v2.4 |
| **CastCommandHandler** | CAST | 5 cast commands (start, stop, connect, quality) | ModuleCommandCallbacks.castExecutor | v2.4 |
| **AICommandHandler** | CUSTOM | 5 AI commands (summarize, chat, search, teach) | ModuleCommandCallbacks.aiExecutor | v2.4 |

Bold = new in v2.1+. All 18 handler categories are fully covered.

### Handler Phrase Collision Avoidance (260222 fix)

Module-specific handlers must **prefix their phrases** with the module name to avoid being intercepted by higher-priority general handlers:

| Before (collided) | After (prefixed) | Why |
|-------------------|-------------------|-----|
| "scroll up" (Cockpit) | "frame scroll up" | NAVIGATION handler (priority 2) intercepted before COCKPIT (priority 13) |
| "scroll down" (Cockpit) | "frame scroll down" | Same |
| "zoom in" (Cockpit) | "frame zoom in" | ScreenHandler (priority 7) intercepted |
| "zoom out" (Cockpit) | "frame zoom out" | Same |
| "rotate left" (Image) | "image rotate left" | ScreenHandler prefix-matched "rotate" |
| "rotate right" (Image) | "image rotate right" | Same |

**Rule:** When adding new handler commands, always check if a higher-priority handler already claims a prefix match on the same phrase. If so, add a module-specific prefix.

### Factory Registration

All handlers registered in `AndroidHandlerFactory.createHandlers()`:

```kotlin
override fun createHandlers(): List<IHandler> {
    return listOf(
        AndroidGestureHandler(service),
        SystemHandler(AndroidSystemExecutor(service)),
        AppHandler(AndroidAppLauncher(service)),
        AndroidCursorHandler(service),
        MediaHandler(service),
        ScreenHandler(service),
        TextHandler(service),
        InputHandler(service),
        AppControlHandler(service),
        ReadingHandler(service),
        VoiceControlHandler(service),
        NoteCommandHandler(service),
        CockpitCommandHandler(service)
    )
}
```

### VoiceControlCallbacks Pattern

VoiceControlHandler can't directly access speech engine internals. Uses a static callback registry:

```kotlin
object VoiceControlCallbacks {
    @Volatile var onMuteVoice: (() -> Boolean)? = null
    @Volatile var onWakeVoice: (() -> Boolean)? = null
    @Volatile var onStartDictation: (() -> Boolean)? = null
    @Volatile var onStopDictation: (() -> Boolean)? = null
    @Volatile var onShowCommands: (() -> Boolean)? = null
    @Volatile var onListCommands: (() -> Boolean)? = null
    fun clear() { /* nullify all */ }
}
```

The accessibility service sets these callbacks during `onServiceReady()`. The handler invokes them without direct coupling. All callbacks use `serviceScope.launch` (fire-and-forget) to avoid blocking Main thread.

**Visual feedback (260220):** Every callback calls `OverlayStateManager.showFeedback("message")` before executing the action. The `FeedbackToast` composable in `CommandOverlayService` renders the message at the top of the screen and auto-dismisses after 2 seconds.

**Dictation mode switch (260220):** `onStartDictation` calls `core?.setSpeechMode(SpeechMode.DICTATION, exitCommands)` instead of `stopListening()`. The recognizer stays active with a minimal grammar containing only exit commands ("stop dictation", "end dictation", "command mode"). `onStopDictation` calls `core?.setSpeechMode(SpeechMode.COMBINED_COMMAND)` to restore full grammar.

**Numbers overlay:** Handled exclusively by `NumbersOverlayHandler` (ACCESSIBILITY category) using `NumbersOverlayExecutor`. A `numbersOverlayModeJob` observer in the service watches `OverlayStateManager.numbersOverlayMode` and triggers `invalidateScreenHash()` + `refreshOverlayBadges()` on mode change.

### Dispatch Priority

`ActionCategory.PRIORITY_ORDER`:
```
SYSTEM > NAVIGATION > APP > GAZE > GESTURE > UI > DEVICE > INPUT > MEDIA > ACCESSIBILITY > BROWSER > NOTE > COCKPIT > CUSTOM
```

The `ActionCoordinator` iterates handlers by category priority. First handler that returns `HandlerResult.success()` wins.

### AndroidCursorHandler IMU Wiring (v2.2, 260217)

When the user says "show cursor", `AndroidCursorHandler.showCursor()` starts `CursorOverlayService` then calls `wireServiceDependencies()` which:

1. **CursorActions** — wires voice movement commands ("cursor up/down/left/right") to the service's CursorController
2. **ClickDispatcher** — wires accessibility click dispatch for dwell-click at cursor position
3. **IMU head tracking** — connects `IMUManager.orientationFlow` to `CursorController.connectInputFlow()` via `HeadTrackingBridge.toCursorInputFlow()`

```kotlin
// In wireServiceDependencies():
val imuManager = IMUManager.getInstance(service.applicationContext)
val imuStarted = imuManager.startIMUTracking("cursor_voice")
if (imuStarted) {
    svc.startIMUTracking(imuManager)  // connects flow to controller
}
```

`hideCursor()` calls `stopIMUTracking("cursor_voice")` to release sensor resources. Consumer ID `"cursor_voice"` is independent from the Settings toggle path, allowing concurrent IMU consumers.

**IMU pipeline**: `IMUManager.orientationFlow` → `HeadTrackingBridge.toCursorInputFlow()` → `CursorInput.HeadMovement(pitch, yaw, roll)` → `CursorController.connectInputFlow()` → `update()` → cursor position state.

**Critical fix (260217)**: `IMUManager.startSensors()` gate condition was changed from `&&` (require both gyroscope AND magnetometer) to `||` (either sensor), matching the lazy `rotationSensor` property's OR logic. See fix doc: `Docs/fixes/VoiceOSCore/VoiceOSCore-Fix-CursorIMUHeadTrackingRegression-260217-V1.md`

## 5. Command Pipeline Performance Optimizations

### 5.1 Overview

Three independent optimizations shipped across commits `959b52aa` (P3 dispatcher fix) and
`8b2094b4` (handler cache + suspend variants) reduce voice command latency from 2-5 seconds to
sub-200ms on a Pixel 9 emulator. Each optimization targets a different layer of the pipeline.

### 5.2 Dispatcher: `processVoiceCommand` on `Dispatchers.Default`

**Before:** `ActionCoordinator.processVoiceCommand()` launched on `Dispatchers.Main`. Every call
executed CPU-intensive O(n) phrase matching, fuzzy scoring, and mutex acquisition on the UI thread.

**After:** `processVoiceCommand()` launches on `Dispatchers.Default`. UI callbacks (overlay updates,
feedback toasts) switch back to `Main` via `withContext(Dispatchers.Main)` only where needed.

```kotlin
// ActionCoordinator
fun processVoiceCommand(text: String) {
    scope.launch(Dispatchers.Default) {   // CPU work off main thread
        val result = findAndDispatch(text)
        withContext(Dispatchers.Main) {   // UI update back on main
            onCommandResult(result)
        }
    }
}
```

This change is safe because `processVoiceCommand()` has no UI state reads in the hot path — all
UI interaction is at the result callback boundary.

### 5.3 Handler Registry: Cached Flat Handler List

**Before:** `HandlerRegistry.findHandler()` called `handlers.values.flatten()` on every invocation,
allocating a new `List<IHandler>` for each voice command (18 handlers = 18-element list rebuilt
on every call).

**After:** `HandlerRegistry` stores a pre-computed `_cachedHandlers: List<IHandler>` built once
during `init`. `findHandler()` and `canHandle()` iterate `_cachedHandlers` directly.

```kotlin
class HandlerRegistry {
    private val handlers: Map<ActionCategory, List<IHandler>> = ...
    private val _cachedHandlers: List<IHandler> = handlers.values.flatten()  // built once

    fun findHandler(command: QuantizedCommand): IHandler? =
        _cachedHandlers.firstOrNull { it.canHandle(command.category) && it.handle(command) != null }
}
```

The cache is correct for the lifetime of the process because handlers are registered only at
startup (in `AndroidHandlerFactory.createHandlers()`) and never added or removed at runtime.

### 5.4 Single `findHandler` Call: Eliminated Duplicate `canHandle`

**Before:** `ActionCoordinator` Step 2 called `handlerRegistry.canHandle(command)` first, then
called `handlerRegistry.findHandler(command)` if the check passed. Each call acquired the
`Mutex.withLock` independently and iterated the handler list twice.

**After:** `canHandle` is removed from the Step 2 hot path. A single `findHandler()` call returns
the handler (non-null = can handle) or null (cannot handle). One mutex acquisition, one list scan.

```kotlin
// Before (two scans, two locks):
if (handlerRegistry.canHandle(command)) {
    val handler = handlerRegistry.findHandler(command)
    handler?.execute(command)
}

// After (one scan, one lock):
val handler = handlerRegistry.findHandler(command)
handler?.execute(command)
```

### 5.5 `StaticCommandRegistry`: O(1) Phrase Index

**Before:** `StaticCommandRegistry.findByPhrase(phrase)` iterated all 275+ commands with a linear
scan on every call.

**After:** `StaticCommandRegistry` builds `_phraseIndex: Map<String, StaticCommand>` during
`initialize()`. `findByPhrase()` is a single `HashMap` lookup (O(1)). `findByPhraseInDomains()`
uses the index for an initial fast-path check before filtering by domain.

```kotlin
private lateinit var _phraseIndex: Map<String, StaticCommand>

fun initialize(commands: List<StaticCommand>) {
    _commands = commands
    _phraseIndex = commands.flatMap { cmd ->
        cmd.phrases.map { phrase -> phrase.lowercase() to cmd }
    }.toMap()
}

fun findByPhrase(phrase: String): StaticCommand? =
    _phraseIndex[phrase.lowercase().trim()]
```

Multi-phrase commands (a single `StaticCommand` with several `phrases`) are indexed under each
phrase independently, so all synonyms resolve in O(1).

### 5.6 `StaticCommandRegistry` Suspend Variants

All query methods now have `suspend` overloads. Callers running inside a coroutine scope use the
suspend variant; legacy `runBlocking` call sites continue using the non-suspend overload unchanged.

| Method | Non-suspend | Suspend |
|--------|-------------|---------|
| All commands | `all()` | `allSuspend()` |
| Find by phrase | `findByPhrase(p)` | `findByPhraseSuspend(p)` |
| Find in domains | `findByPhraseInDomains(p, d)` | `findByPhraseInDomainsSuspend(p, d)` |
| By category | `byCategory(c)` | `byCategorySuspend(c)` |

The suspend variants call `withContext(Dispatchers.Default)` internally, ensuring registry reads
never block the calling coroutine thread even when the non-suspend path is used transitionally.

### 5.7 Combined Latency Impact

| Optimization | Before | After | Mechanism |
|-------------|--------|-------|-----------|
| Dispatcher | 2-5s delay | <200ms | CPU off main thread |
| Handler cache | 13-element list per call | 0 allocations | Pre-computed flat list |
| Single findHandler | 2 mutex locks, 2 scans | 1 lock, 1 scan | Collapsed duplicate canHandle |
| Phrase index | O(275) per command | O(1) | HashMap on initialize |
| Suspend variants | runBlocking blocks thread | structured concurrency | withContext(Default) |

---

## 6. Web Command Routing Architecture

### Problem: 3 Routing Bugs Blocked 45 Static Web Commands

Static BROWSER and WEB_GESTURE commands ("go back", "refresh page", "swipe up") are defined in `.web.vos` files and loaded into `StaticCommandRegistry` — but they were never routed to `WebCommandHandler` when the browser was active. Three interconnected bugs:

**Bug 1 — Static web commands not in dynamic registry**: `webCommandCollectorJob` only registered DOM-scraped element commands via `updateDynamicCommandsBySource("web", ...)`. Static BROWSER/WEB_GESTURE commands from VOS files were never registered as dynamic commands, so `ActionCoordinator` didn't recognize them as web-destined.

**Bug 2 — `extractVerbAndTarget` short-circuits**: When the user says "go back", `extractVerbAndTarget()` checks `StaticCommandRegistry.findByPhrase("go back")` → finds it (NAVIGATION category) → returns `(null, null)`. With `target == null`, dynamic command lookup is skipped entirely, and the command falls through to static handler matching.

**Bug 3 — Priority-based handler stealing**: Even if Bugs 1+2 were fixed, `processCommand()` calls `handlerRegistry.findHandler(command)` which iterates by priority. `SystemHandler` (SYSTEM, priority 1) or `AndroidGestureHandler` (GESTURE, priority 5) steal overlapping phrases ("go back", "swipe up", "zoom in") before `WebCommandHandler` (BROWSER, priority 11) gets a chance.

### Solution: 3-Layer Fix

#### Layer 1: Web Pre-Check in `processVoiceCommand()` (ActionCoordinator)

A full-phrase check for web-source commands runs **before** `extractVerbAndTarget()`:

```kotlin
// Before verb/target extraction:
val webFullPhraseMatch = commandRegistry.findByPhrase(normalizedText)
if (webFullPhraseMatch != null && webFullPhraseMatch.metadata["source"] == "web") {
    return processCommand(webFullPhraseMatch)
}
```

This catches "go back", "refresh page" etc. before they can be short-circuited by the static command check.

#### Layer 2: Web Bypass in `processCommand()` (ActionCoordinator)

For commands with `source="web"` metadata, priority-based `findHandler()` is bypassed. Instead, the BROWSER category handlers are queried directly:

```kotlin
val handler = if (command.metadata["source"] == "web") {
    handlerRegistry.getHandlersForCategory(ActionCategory.BROWSER)
        .firstOrNull { it.canHandle(command) }
        ?: handlerRegistry.findHandler(command)  // Fallback
} else {
    handlerRegistry.findHandler(command)
}
```

This ensures `WebCommandHandler` (BROWSER category) handles web commands even though `SystemHandler` (priority 1) could also match "go back".

#### Layer 3: Register Static Web Commands as Dynamic (VoiceAvanueAccessibilityService)

In `webCommandCollectorJob`, after registering DOM-scraped commands, static BROWSER + WEB_GESTURE commands are also registered with `source="web"` metadata:

```kotlin
val browserStaticCmds = StaticCommandRegistry.byCategoryAsQuantized(CommandCategory.BROWSER)
val gestureStaticCmds = StaticCommandRegistry.byCategoryAsQuantized(CommandCategory.WEB_GESTURE)
val webStaticCommands = (browserStaticCmds + gestureStaticCmds).map { cmd ->
    cmd.copy(metadata = cmd.metadata + mapOf("source" to "web"))
}
voiceOSCore?.actionCoordinator?.updateDynamicCommandsBySource("web_static", webStaticCommands)
```

**Source tag separation**: `"web_static"` keeps static web commands separate from DOM-scraped commands (`"web"`). When the browser closes (`phrases.isEmpty()`), both are cleared:

```kotlin
voiceOSCore?.actionCoordinator?.clearDynamicCommandsBySource("web")
voiceOSCore?.actionCoordinator?.clearDynamicCommandsBySource("web_static")
```

### Context-Sensitive Command Behavior

The same voice phrase routes to different handlers depending on context:

| Phrase | Browser Active | Browser Inactive |
|--------|---------------|-----------------|
| "go back" | WebCommandHandler → browser back | SystemHandler → Android back |
| "swipe up" | WebCommandHandler → JS scroll | AndroidGestureHandler → accessibility gesture |
| "zoom in" | WebCommandHandler → JS zoom | ScreenHandler → display zoom |
| "scroll down" | WebCommandHandler → JS scroll | AndroidGestureHandler → accessibility scroll |

### RETRAIN_PAGE Command

The `RETRAIN_PAGE` action type triggers a fresh DOM scrape of the current web page:

1. **CommandActionType.RETRAIN_PAGE** → mapped in `WebCommandHandler.resolveWebActionType()`
2. **WebActionType.RETRAIN_PAGE** → intercepted in `WebCommandExecutorImpl.executeWebAction()` before JS script generation
3. **BrowserVoiceOSCallback.requestRetrain()** → signals the web engine to re-scrape
4. Phrases: "retrain page", "rescan page", "rescan"

### Files Involved

| File | Role |
|------|------|
| `ActionCoordinator.kt` | Pre-check + bypass routing |
| `VoiceAvanueAccessibilityService.kt` | Register/clear web_static commands |
| `WebCommandHandler.kt` | Map phrases → WebActionType |
| `WebCommandExecutorImpl.kt` | Execute JS or intercept RETRAIN_PAGE |
| `IWebCommandExecutor.kt` | WebActionType enum definition |
| `BrowserVoiceOSCallback.kt` | Bridge between service and WebView |

## 7. Adding New Handlers

1. Create handler class extending `BaseHandler` in `VoiceOSCore/src/androidMain/.../handlers/`
2. Set `override val category: ActionCategory`
3. List phrases in `override val supportedActions: List<String>`
4. Implement `override suspend fun execute(command, params): HandlerResult`
5. Register in `AndroidHandlerFactory.createHandlers()`
6. Add commands to appropriate VOS seed file (`.app.vos` or `.web.vos`) in v3.0 compact format
7. If adding a new action_id prefix: add entry to `VosParser.CATEGORY_MAP`
8. Add action_id → CommandActionType entry to `VosParser.ACTION_MAP`
9. If the command has metadata (direction/scale/factor): add entry to `VosParser.META_MAP`
10. Bump VOS version in `CommandLoader.requiredVersion` to force DB reload

## 8. SFTP Sync (Phase B) — Implemented

Phase B adds a network sync layer for distributing VOS files between devices and a central SFTP server. Hidden behind a developer toggle in Settings → System → "Developer: VOS Sync".

### Architecture

```
SystemSettingsProvider                    VosSyncScreen
  ├── VOS Sync toggle (on/off)            ├── Connection status card
  ├── SFTP Host / Port / Username         ├── Upload All / Download All
  ├── Remote Path                         ├── Full Sync button
  ├── SSH Key File path                   ├── Progress indicator
  └── [Manage VOS Sync →]                 └── File list (registry entries)
        │                                        │
        └───────── navigates to ─────────────────┘
                                                  │
                                           VosSyncViewModel
                                                  │
                                           VosSyncManager
                                           ├── uploadLocalFiles()
                                           ├── downloadNewFiles()
                                           └── testConnection()
                                                  │
                                           VosSftpClient (JSch)
                                           ├── connect / disconnect
                                           ├── upload / download
                                           └── listFiles / fetchManifest
```

### Key Files

| File | Location | Purpose |
|------|----------|---------|
| `SyncModels.kt` | `VoiceOSCore/.../vos/sync/` | Sealed classes: SftpResult, SftpAuthMode, SyncStatus, SyncProgress, SyncResult, ServerManifest |
| `VosSftpClient.kt` | `VoiceOSCore/.../vos/sync/` | JSch SFTP wrapper: connect/disconnect, upload/download, listFiles, manifest ops. All I/O in Dispatchers.IO, 30s timeout |
| `VosSyncManager.kt` | `VoiceOSCore/.../vos/sync/` | Sync orchestrator: testConnection, uploadLocalFiles (queries getNotUploaded), downloadNewFiles (manifest hash comparison), syncAll |
| `VosSyncViewModel.kt` | `apps/avanues/.../ui/sync/` | @HiltViewModel exposing sync actions, settings flow, registry file list |
| `VosSyncScreen.kt` | `apps/avanues/.../ui/sync/` | Full management UI: connection status, progress, 4 action buttons, file registry list |
| `SyncModule.kt` | `apps/avanues/.../di/` | Hilt DI: VosSftpClient, IVosFileRegistryRepository, VosSyncManager as singletons |

### SFTP Library

Uses `com.github.mwiede:jsch:0.2.16` (modern fork of JSch, actively maintained, Maven Central). Added to both `apps/avanues` and `Modules/VoiceOSCore` build.gradle.kts.

### Manifest-Based Delta Sync

Server-side `manifest.json` tracks all available VOS files with SHA-256 content hashes:

```json
{
  "version": "1.0",
  "files": [
    { "hash": "abc123...", "filename": "en-US.app.vos", "size": 12345, "uploadedAt": 1707600000 }
  ],
  "lastUpdated": 1707600000
}
```

- **Upload**: Queries `getNotUploaded` (WHERE uploaded_at IS NULL AND source = 'local'), uploads each file, updates registry `uploaded_at`, rebuilds manifest
- **Download**: Fetches manifest, compares hashes against local registry, downloads + imports new files via `VosFileImporter`
- **Full Sync**: Upload then download, returns `SyncResult(uploadedCount, downloadedCount, errors)`

### DataStore Keys

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `vos_sync_enabled` | Boolean | false | Master toggle for sync section |
| `vos_sftp_host` | String | "" | Server hostname or IP |
| `vos_sftp_port` | Int | 22 | SFTP port |
| `vos_sftp_username` | String | "" | SSH username |
| `vos_sftp_remote_path` | String | "/vos" | Server directory |
| `vos_sftp_key_path` | String | "" | SSH private key file path |
| `vos_last_sync_time` | Long | null | Timestamp of last sync |

### Auth Modes

- **SSH Key** (preferred): Set `vos_sftp_key_path` to private key file
- **Password** (fallback): Empty password if no key configured

### Navigation

- `AvanueMode.VOS_SYNC` route in `MainActivity.kt`
- Accessed via: Settings → System → Enable "VOS Sync" → "Manage VOS Sync"
- `onNavigateToVosSync` callback plumbed through `UnifiedSettingsScreen` → `SystemSettingsProvider`

### Design Decisions

1. **StrictHostKeyChecking=no**: Dev-only setting for testing without known_hosts management
2. **Importer = null in DI**: VoiceCommandDaoAdapter is runtime-managed, not Hilt-injectable. Download imports wired when importer is available at runtime
3. **Dev toggle**: UI hidden unless developer enables `vosSyncEnabled`
4. **Registry query**: `getNotUploaded` filters `uploaded_at IS NULL AND source = 'local' AND is_active = 1`

### Phase B Completion (commit b4c9e55d)

#### Runtime Importer Wiring

`VosSyncManager` uses late-binding for `VosFileImporter` since `VoiceCommandDaoAdapter` is a lazy singleton not injectable by Hilt:

```kotlin
class VosSyncManager(
    private val sftpClient: VosSftpClient,
    private val registry: IVosFileRegistryRepository
) {
    @Volatile private var _importer: VosFileImporter? = null
    fun setImporter(importer: VosFileImporter) { _importer = importer }
}
```

Wiring happens in `VoiceAvanueAccessibilityService` after DB initialization via `@EntryPoint`:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncEntryPoint {
    fun vosSyncManager(): VosSyncManager
}
```

#### Security Hardening

- **SftpCredentialStore**: `EncryptedSharedPreferences` with `MasterKey.AES256_GCM` for SFTP password and SSH key passphrase. Fallback to regular SharedPreferences if hardware keystore unavailable. Implements `ICredentialStore` from Foundation KMP (see Chapter 96).
- **Configurable host key checking**: `hostKeyChecking` parameter on `VosSftpClient.connect()` supporting:
  - `"no"` — Accept all keys (dev/testing)
  - `"accept-new"` — Trust on first connect, reject changes
  - `"yes"` — Strict verification against known_hosts
- **Log sanitization**: Username removed from connection logs, file paths masked in progress logs.

| DataStore Key | Type | Default | Purpose |
|--------------|------|---------|---------|
| `vos_sftp_host_key_mode` | String | "no" | SSH host key verification mode |

#### WorkManager Background Sync

`VosSyncWorker` is a `@HiltWorker` `CoroutineWorker` that performs periodic SFTP sync:

- **Constraints**: Requires network connectivity + not low battery
- **Backoff**: Exponential, 30s initial delay, 3 max retries
- **Scheduling**: Configurable interval (1/2/4/8/12/24 hours) via `PeriodicWorkRequestBuilder`
- **Auth**: Reads from `SftpCredentialStore` (ICredentialStore) + `AvanuesSettingsRepository` (ISettingsStore — see Chapter 96)

| DataStore Key | Type | Default | Purpose |
|--------------|------|---------|---------|
| `vos_auto_sync_enabled` | Boolean | false | Enable periodic background sync |
| `vos_sync_interval_hours` | Int | 4 | Sync interval in hours |

#### Dependencies Added (Phase B Completion)

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `androidx.hilt:hilt-work` | 1.1.0 | @HiltWorker support |
| `androidx.hilt:hilt-compiler` | 1.1.0 | @HiltWorker KSP processor |
| `androidx.security:security-crypto` | latest | EncryptedSharedPreferences |

## 9. Phase C Foundation: In-App Crowd-Sourcing

### PhraseSuggestion Database

**File**: `Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/PhraseSuggestion.sq`

| Column | Type | Purpose |
|--------|------|---------|
| id | INTEGER PK | Auto-increment |
| command_id | TEXT NOT NULL | Links to voice command |
| original_phrase | TEXT NOT NULL | Current phrase being improved |
| suggested_phrase | TEXT NOT NULL | User's alternative suggestion |
| locale | TEXT NOT NULL | Language locale (e.g., "es-ES") |
| created_at | INTEGER NOT NULL | Timestamp |
| status | TEXT NOT NULL | "pending", "approved", "rejected" |
| source | TEXT NOT NULL | "user", "crowd", "ai" |

**Repository**: `IPhraseSuggestionRepository` / `SQLDelightPhraseSuggestionRepository`
**Access**: `VoiceOSDatabaseManager.phraseSuggestions`

### PhraseSuggestionDialog

`PhraseSuggestionDialog.kt` — Composable dialog for submitting alternative phrases:
- Shows original phrase and locale
- Text input for suggestion
- Submit/cancel buttons
- Wired into VosSyncScreen with pending count display and export button

### Crowd-Sourcing Workflow (Future)

1. User submits alternative phrase via `PhraseSuggestionDialog`
2. Stored locally with status "pending"
3. `VosSyncViewModel.exportSuggestions()` exports pending suggestions as JSON
4. Exported file uploaded via SFTP sync for review
5. Approved suggestions merged into locale VOS files via GitHub PR workflow

---

*Chapter 95 | VOS Distribution System & Handler Dispatch Architecture*
*Created: 2026-02-11 | Updated: 2026-02-16 (VOS v3.0 compact format, VosParser compiled maps, CommandLoader/Importer migration, web command routing, dead code audit: ArrayJsonParser + UnifiedJSONParser deleted) | Updated: 2026-02-19 (Section 5 — command pipeline performance optimizations: Dispatchers.Default, handler list cache, collapsed canHandle, O(1) phrase index, suspend variants) | Updated: 2026-02-22 (CommandManager import path fix: managers.commandmanager → commandmanager)*
*Related: Chapter 93 (Voice Command Pipeline), Chapter 94 (4-Tier Voice Enablement), Chapter 96 (KMP Foundation)*
