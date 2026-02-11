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

### VOS v2.1 Format Changes

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

### CommandLoader Dual-File Loading

`CommandLoader.loadLocale()` now:
1. Opens `{locale}.app.vos` from assets
2. Opens `{locale}.web.vos` from assets
3. Parses both via `ArrayJsonParser.parseCommandsJson()`
4. Merges command lists
5. Single `insertBatch()` to DB

Version bump 2.0 → 2.1 forces DB reload on app upgrade.

```kotlin
companion object {
    const val FILE_EXTENSION_APP = ".app.vos"
    const val FILE_EXTENSION_WEB = ".web.vos"
}
```

`getAvailableLocales()` scans `.app.vos` files and intersects with `.web.vos` to return only complete locale sets.

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
- Uses `ArrayJsonParser.parseCommandsJson()` for parsing
- Batch insert via `VoiceCommandDaoAdapter.insertBatch()`
- Auto-detects file type (app/web) from filename or JSON `domain` field

## 4. Static Command Dispatch Architecture

### Problem Solved

VOS seed files defined 107 commands across 11 categories, but only 4 handlers existed (AndroidGestureHandler, SystemHandler, AppHandler, AndroidCursorHandler). 91 commands silently returned `HandlerResult.notHandled()`.

### Handler Coverage (11 Handlers)

| Handler | Category | Commands | Key API |
|---------|----------|----------|---------|
| AndroidGestureHandler | GESTURE | scroll, tap, swipe, pinch | GestureDescription API |
| SystemHandler | SYSTEM/NAV | back, home, recents, split screen | performGlobalAction() |
| AppHandler | APP_LAUNCH | open browser/camera/gallery/etc. | Intent + PackageManager |
| AndroidCursorHandler | GAZE | cursor show/hide/click | CursorOverlayService |
| **MediaHandler** | MEDIA | play, pause, next, prev, volume | AudioManager + KeyEvent |
| **ScreenHandler** | DEVICE | brightness, wifi, bluetooth, screenshot, flashlight | Settings.System + CameraManager |
| **TextHandler** | INPUT | select all, copy, paste, cut, undo, redo, delete | AccessibilityNodeInfo actions |
| **InputHandler** | INPUT | show/hide keyboard | SoftKeyboardController |
| **AppControlHandler** | APP | close app, exit, quit | GLOBAL_ACTION_BACK + HOME |
| **ReadingHandler** | ACCESSIBILITY | read screen, stop reading | TextToSpeech + tree traversal |
| **VoiceControlHandler** | UI | mute/wake, dictation, help, numbers | VoiceControlCallbacks |

Bold = new in v2.1.

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
        VoiceControlHandler(service)
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
    @Volatile var onSetNumbersMode: ((String) -> Boolean)? = null
    fun clear() { /* nullify all */ }
}
```

The accessibility service sets these callbacks during `onServiceReady()`. The handler invokes them without direct coupling. **Phase B task**: Wire callbacks in `VoiceAvanueAccessibilityService`.

### Dispatch Priority

`ActionCategory.PRIORITY_ORDER`:
```
SYSTEM > NAVIGATION > APP > GAZE > GESTURE > UI > DEVICE > INPUT > MEDIA > ACCESSIBILITY > BROWSER > CUSTOM
```

The `ActionCoordinator` iterates handlers by category priority. First handler that returns `HandlerResult.success()` wins.

## 5. Adding New Handlers

1. Create handler class extending `BaseHandler` in `VoiceOSCore/src/androidMain/.../handlers/`
2. Set `override val category: ActionCategory`
3. List phrases in `override val supportedActions: List<String>`
4. Implement `override suspend fun execute(command, params): HandlerResult`
5. Register in `AndroidHandlerFactory.createHandlers()`
6. Add commands to appropriate VOS seed file (`.app.vos` or `.web.vos`)
7. Bump VOS version in `CommandLoader` to force DB reload

## 6. Future: FTP Sync (Phase B)

Architecture planned but not yet implemented:

```
VosSyncManager
  ├── VosFtpClient (upload/download with retry)
  ├── manifest.json (server-side index)
  ├── WorkManager job (background auto-sync)
  └── On-demand download (unscraped websites)
```

- Server path: `/vos/app/{locale}/` and `/vos/web/{domain}/`
- Dedup via `content_hash` in manifest
- Three sync modes: manual, background auto-sync, on-demand cache

## 7. Future: In-App Crowd-Sourcing (Phase C)

- `PhraseSuggestionDialog`: Long-press command in Help screen → suggest alternative phrase
- Suggestions stored locally → export for crowd-sourcing review
- GitHub PR workflow for validated translations
