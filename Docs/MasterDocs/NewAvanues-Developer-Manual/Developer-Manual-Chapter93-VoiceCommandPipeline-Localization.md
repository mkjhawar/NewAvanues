# Chapter 93: Voice Command Pipeline & Localization Architecture

## 1. Architecture Overview

VoiceOS uses a layered pipeline that flows from VOS seed files through a SQLDelight database to multiple runtime consumers. The database is the single source of truth; hardcoded fallback lists exist only for pre-DB-init startup.

```
VOS Seed Files (per locale, split by domain since v2.1)
  assets/localization/commands/en-US.app.vos  (62 app commands)
  assets/localization/commands/en-US.web.vos  (45 web commands)
  assets/localization/commands/es-ES.app.vos
  assets/localization/commands/es-ES.web.vos
  assets/localization/commands/fr-FR.app.vos
  assets/localization/commands/fr-FR.web.vos
  assets/localization/commands/de-DE.app.vos
  assets/localization/commands/de-DE.web.vos
  assets/localization/commands/hi-IN.app.vos
  assets/localization/commands/hi-IN.web.vos
        |
        v
CommandLoader.initializeCommands()  <- Seeds DB on first launch / version bump
        |
        v
commands_static table (SQLDelight)  <- Runtime source of truth
        |
        v
CommandManager.populateStaticRegistryFromDb()
        |
        v
StaticCommandRegistry._dbCommands   <- All consumers read from here
        |
        +-- .all() / .byCategory() / .findByPhrase()
        |       |
        |       +-- Speech engine vocabulary (allPhrases)
        |       +-- NLU schema (toNluSchema)
        |       +-- QuantizedCommand export (allAsQuantized)
        |
        +-- HelpCommandDataProvider.getCategories()
        |       +-- 8 categories -> Help screen UI
        |       +-- Template commands (parametric patterns, static)
        |
        +-- WebCommandHandler.supportedActions
        |       +-- Derived from BROWSER + WEB_GESTURE categories
        |
        +-- AndroidGestureHandler.supportedActions
        |       +-- Native gesture dispatch
        |
        +-- SynonymRegistry.all()
                +-- Verb canonicalization for NLU
```

### DB Loading Flow

1. App starts -> `CommandManager.initialize()` launches IO coroutine
2. `CommandLoader.initializeCommands()` checks version in `database_version` table
3. If version mismatch or empty: parses VOS seed file -> inserts into `commands_static`
4. `CommandManager.loadDatabaseCommands()` builds pattern matching cache
5. `CommandManager.populateStaticRegistryFromDb()` converts `VoiceCommandEntity` -> `StaticCommand` -> calls `StaticCommandRegistry.initialize(commands)`
6. `StaticCommandRegistry.all()` now returns DB-loaded commands
7. `HelpCommandDataProvider` and `WebCommandHandler` automatically reflect DB content

### Fallback Behavior (REMOVED 260212)

**Prior to 260212**: If the database was empty or not yet loaded, `StaticCommandRegistry.all()` returned hardcoded English commands (~800 lines of fallback lists). Once `initialize()` was called with DB data, the hardcoded lists were bypassed.

**Since 260212**: The hardcoded fallback has been completely removed. `StaticCommandRegistry.all()` now returns an empty list if the database is not yet initialized. This eliminates dual-source maintenance burden and enforces `.VOS → CommandLoader → SQLDelight` as the single source of truth.

**CRITICAL**: `CommandLoader.seedFromAssets()` MUST complete before the voice pipeline starts. Services and UI components should verify `StaticCommandRegistry.all().isNotEmpty()` before attempting command resolution.

## 2. Command Pipeline (5-Layer)

Voice commands flow through 5 layers from speech recognition to execution:

### Layer 1: CommandActionType (Enum)
Defines WHAT action to perform. Platform-agnostic enum in `CommandModels.kt`.
- 75+ action types across 15 categories
- Each type maps to both web (JS) and native (Android) execution paths
- `isBrowserAction()`, `isSystemAction()`, `isMediaAction()` etc. for routing

### Layer 2: StaticCommandRegistry (Command Definitions)
Maps voice PHRASES to CommandActionTypes. Loaded from `commands_static` DB table at runtime (hardcoded fallback pre-init). Organized by CommandCategory:
- `NAVIGATION` (8) -- back, home, scroll, app drawer
- `MEDIA` (7) -- play, pause, volume, track skip
- `SYSTEM` (12) -- settings, notifications, screenshot, flashlight, brightness, wifi, bluetooth
- `VOICE_CONTROL` (10) -- mute, wake, dictation, help, list-commands, cursor (numbers → ACCESSIBILITY)
- `APP_LAUNCH` (8) -- open browser/camera/gallery/calculator/etc.
- `APP_CONTROL` (1) -- close app
- `ACCESSIBILITY` (6) -- click, long press, zoom, read screen
- `TEXT` (7) -- select all, copy, paste, cut, undo, redo, delete
- `INPUT` (2) -- show/hide keyboard
- `BROWSER` (18) -- retrain, navigation, forms, swipes, grab/release, rotate
- `WEB_GESTURE` (27) -- pan, tilt, orbit, rotate x/y/z, pinch, fling, throw, scale, etc.

**Total: 107 commands**

### SpeechMode Enum and Engine Adapter Mapping

`SpeechMode` (in `VoiceOSCore/speech/SpeechMode.kt`) defines how speech is processed:
- `STATIC_COMMAND` — restricted grammar, predefined commands only
- `DYNAMIC_COMMAND` — restricted grammar, UI-scraped commands
- `COMBINED_COMMAND` — both static + dynamic (combining handled by VoiceOSCore coordinator)
- `DICTATION` — continuous speech input, exit commands only in grammar
- `FREE_SPEECH` — unrestricted vocabulary
- `MUTED` — engine stays alive with restricted wake-only grammar (e.g., "wake up voice")
- `HYBRID` — Vivoka-specific auto online/offline switching

**Engine adapter mapping policy (260223):** All three adapters (VivokaAndroidEngine, KmpSpeechEngineAdapter, AppleSpeechEngineAdapter) map `COMBINED_COMMAND` and `MUTED` to `SpeechRecognition.SpeechMode.STATIC_COMMAND`. The combining of static + dynamic commands is a coordinator concern — the engine just receives a restricted grammar via `updateCommands()`. MUTED mode restricts the grammar to wake commands only.

### Layer 3: ActionCoordinator (Routing)
Routes QuantizedCommands to the appropriate handler based on:
1. Command metadata (source="web" -> WebCommandHandler)
2. Handler's `canHandle()` check (phrase matching + category)
3. Priority: Web handler > Gesture handler > System handler > App handler

### Layer 4: Handlers (Platform-Specific Execution)
- **WebCommandHandler** -> resolves WebActionType -> delegates to IWebCommandExecutor
- **AndroidGestureHandler** -> dispatches via AndroidGestureDispatcher (AccessibilityService)
- **SystemHandler** -> global actions (back, home, recents)
- **AppHandler** -> app launch via Intent
- **AndroidCursorHandler** -> cursor overlay service
- **MediaHandler** -> play/pause/next/prev/volume via AudioManager (since v2.1)
- **ScreenHandler** -> brightness/wifi/bluetooth/screenshot/flashlight/rotate/settings (since v2.1)
- **TextHandler** -> select all/copy/paste/cut/undo/redo/delete via AccessibilityNodeInfo (since v2.1)
- **InputHandler** -> show/hide keyboard via SoftKeyboardController (since v2.1)
- **AppControlHandler** -> close/exit app via GLOBAL_ACTION_BACK + HOME (since v2.1)
- **ReadingHandler** -> TTS screen reader via TextToSpeech + accessibility tree (since v2.1)
- **VoiceControlHandler** -> mute/wake/dictation/help/list-commands via VoiceControlCallbacks + visual feedback (since v2.1, numbers removed v2.4)

### Layer 5: Execution
- **Web:** JavaScript injection via DOMScraperBridge -> WebView evaluateJavascript
- **Android:** GestureDescription API via AccessibilityService.dispatchGesture()

## 3. VOS Seed File Format (v2.1)

### File Location (v2.1 Domain Split)
```
apps/avanues/src/main/assets/localization/commands/
  en-US.app.vos  <- English app commands (62: nav, media, sys, voice, app, acc, text, input, appctl)
  en-US.web.vos  <- English web commands (45: browser, gesture)
  es-ES.app.vos  <- Spanish app commands
  es-ES.web.vos  <- Spanish web commands
  fr-FR.app.vos  <- French app commands
  fr-FR.web.vos  <- French web commands
  de-DE.app.vos  <- German app commands
  de-DE.web.vos  <- German web commands
  hi-IN.app.vos  <- Hindi app commands
  hi-IN.web.vos  <- Hindi web commands
```

**v2.1 Split Rationale**: App commands (62) are locale-specific and crowd-sourceable. Web/gesture commands (45) are universal technical terms shared across locales. The split enables independent versioning and future FTP distribution.

### Format: Compact JSON with Explicit Maps

```json
{
  "version": "2.1",
  "locale": "en-US",
  "fallback": "en-US",
  "domain": "app",
  "category_map": {
    "nav": "NAVIGATION",
    "media": "MEDIA",
    "sys": "SYSTEM",
    "gesture": "WEB_GESTURE"
  },
  "action_map": {
    "nav_back": "BACK",
    "nav_home": "HOME",
    "gesture_pan_left": "PAN"
  },
  "meta_map": {
    "gesture_pan_left": {"direction": "left"},
    "gesture_pinch_in": {"scale": "0.5"}
  },
  "commands": [
    ["nav_back", "go back", ["navigate back", "back", "previous screen"], "Navigate to previous screen"],
    ["gesture_pan_left", "pan left", ["slide view left", "move view left"], "Pan viewport left"]
  ]
}
```

### Command Array Format
Each command is a 4-element array:
```
[command_id, primary_phrase, [synonym1, synonym2, ...], description]
  position 0    position 1          position 2              position 3
```

### v2.1 Root-Level Fields

| Field | Purpose | Example |
|-------|---------|---------|
| `version` | Format version | `"2.1"` |
| `locale` | Locale code | `"en-US"` |
| `fallback` | Fallback locale | `"en-US"` |
| `domain` | **v2.1**: `"app"` or `"web"` | `"app"` |
| `category_map` | Prefix -> CommandCategory name | `"nav"` -> `"NAVIGATION"` |
| `action_map` | Command ID -> CommandActionType name | `"nav_back"` -> `"BACK"` |
| `meta_map` | Command ID -> metadata JSON | `"gesture_pan_left"` -> `{"direction":"left"}` |

### v1.0 Backward Compatibility
Files without `category_map`/`action_map`/`meta_map` still work:
- Category derived from command_id prefix: `"nav_back"` -> prefix `"nav"`
- Action stored as command_id in DB (resolved at runtime via `VoiceCommandEntity.resolvedAction`)
- No metadata for v1.0 commands

### Parser: VosParser (KMP)
Located at `VoiceOSCore/src/commonMain/.../loader/VosParser.kt`
- `VosParser.parse(content, isFallback)` → `VosParseResult.Success` or `VosParseResult.Error`
- Auto-detects format: `{` → JSON v2.1, `#`/`VOS:` → compact v3.0
- Compiled maps as companion constants: `CATEGORY_MAP` (10), `ACTION_MAP` (124), `META_MAP` (22)
- Produces `List<VosParsedCommand>` — mapped to `VoiceCommandEntity` via `toEntity()` extension
- **Replaces ArrayJsonParser** (DELETED 260216) which was Android-only and supported only v2.1 JSON

## 4. Gesture Commands

### Web Gestures (27 commands, WEB_GESTURE category)

| Command | ActionType | JS Function | Voice Phrases | Parameters |
|---------|-----------|-------------|---------------|------------|
| Pan | PAN | `VOS.gesture.pan(dx,dy)` | "pan", "pan viewport", "move view" | dx, dy (px) |
| Pan Left | PAN | `VOS.gesture.pan(-200,0)` | "pan left", "slide view left" | direction=left |
| Pan Right | PAN | `VOS.gesture.pan(200,0)` | "pan right", "slide view right" | direction=right |
| Pan Up | PAN | `VOS.gesture.pan(0,-200)` | "pan up", "move view up" | direction=up |
| Pan Down | PAN | `VOS.gesture.pan(0,200)` | "pan down", "move view down" | direction=down |
| Tilt | TILT | `VOS.gesture.tilt(angle)` | "tilt", "angle view" | angle (deg) |
| Tilt Up | TILT | `VOS.gesture.tilt(15)` | "tilt up", "angle up" | direction=up |
| Tilt Down | TILT | `VOS.gesture.tilt(-15)` | "tilt down", "angle down" | direction=down |
| Orbit | ORBIT | `VOS.gesture.orbit(dx,dy)` | "orbit", "circle around" | deltaX, deltaY |
| Orbit Left | ORBIT | `VOS.gesture.orbit(-30,0)` | "orbit left", "circle left" | direction=left |
| Orbit Right | ORBIT | `VOS.gesture.orbit(30,0)` | "orbit right", "circle right" | direction=right |
| Rotate X | ROTATE_X | `VOS.gesture.rotateX(angle)` | "rotate x", "flip vertical" | angle (deg) |
| Rotate Y | ROTATE_Y | `VOS.gesture.rotateY(angle)` | "rotate y", "flip horizontal" | angle (deg) |
| Rotate Z | ROTATE_Z | `VOS.gesture.rotateZ(angle)` | "rotate z", "spin" | angle (deg) |
| Pinch In | PINCH | `VOS.gesture.pinch(0.5)` | "pinch in", "squeeze" | scale=0.5 |
| Pinch Out | PINCH | `VOS.gesture.pinch(2.0)` | "pinch out", "spread" | scale=2.0 |
| Fling Up | FLING | `VOS.gesture.fling('up',v)` | "fling up", "flick up" | direction=up |
| Fling Down | FLING | `VOS.gesture.fling('down',v)` | "fling down", "flick down" | direction=down |
| Fling Left | FLING | `VOS.gesture.fling('left',v)` | "fling left", "flick left" | direction=left |
| Fling Right | FLING | `VOS.gesture.fling('right',v)` | "fling right", "flick right" | direction=right |
| Throw | THROW | `VOS.gesture.throwEl(vx,vy)` | "throw", "toss" | velocityX, velocityY |
| Scale Up | SCALE | `VOS.gesture.scale(1.5)` | "scale up", "enlarge" | factor=1.5 |
| Scale Down | SCALE | `VOS.gesture.scale(0.67)` | "scale down", "shrink" | factor=0.67 |
| Reset Zoom | RESET_ZOOM | `VOS.gesture.resetZoom()` | "reset zoom", "normal zoom" | -- |
| Select Word | SELECT_WORD | `VOS.gesture.selectWord(el)` | "select word", "pick word" | -- |
| Clear Selection | CLEAR_SELECTION | `VOS.gesture.clearSelection()` | "clear selection", "deselect" | -- |
| Hover Out | HOVER_OUT | `VOS.gesture.hoverOut(el)` | "hover out", "unhover" | -- |

### Android Native Gesture Mapping

| Web Gesture | Android Equivalent | Dispatch Method |
|-------------|-------------------|-----------------|
| Pan | Scroll (same direction) | `dispatcher.scroll(direction)` |
| Tilt | Scroll (approximation) | `dispatcher.scroll(direction)` |
| Orbit | Scroll (approximation) | `dispatcher.scroll(direction)` |
| Rotate X/Y/Z | Not supported | Graceful failure message |
| Pinch In/Out | Two-stroke pinch gesture | `dispatcher.pinch(scale)` |
| Fling | Fast swipe (100ms, 800px) | `dispatcher.fling(direction)` |
| Throw | Fling (same dispatch) | `dispatcher.fling(direction)` |
| Scale Up/Down | Pinch gesture | `dispatcher.pinch(factor)` |
| Reset Zoom | Pinch to 1.0 | `dispatcher.pinch(1.0)` |
| Select Word | Double-tap | `dispatcher.doubleTap(x, y)` |
| Clear Selection | Tap to deselect | `dispatcher.tap(x, y)` |
| Hover Out | Not supported | Graceful failure message |
| Grab | Long press | `dispatcher.longPress(x, y)` |
| Double Click | Double-tap | `dispatcher.doubleTap(x, y)` |

## 5. Synonym System

### SynonymRegistry (Verb Canonicalization)

The NLU engine uses `SynonymRegistry` to normalize user speech into canonical verbs:

```
User says: "flick down" -> canonical: "fling" -> CommandActionType.FLING
User says: "lock element" -> canonical: "grab" -> CommandActionType.GRAB
User says: "squeeze" -> canonical: "pinch" -> CommandActionType.PINCH
```

33 entries organized by domain:
- **Element interaction** (2): click -> tap/press/push/select/hit; long press -> long click/hold
- **Scroll** (4): scroll up/down/left/right with swipe/go/page variants
- **Navigation** (4): open, close, back, home with multiple phrasings
- **Search & input** (2): search -> find/look for; type -> enter/input/write
- **Text editing** (7): delete, copy, paste, cut, undo, redo, select
- **Zoom** (2): zoom in -> magnify/enlarge; zoom out -> shrink/smaller
- **Media/TTS** (2): mute -> silence; read -> speak/narrate
- **Gestures** (9): pan, tilt, orbit, fling, throw, pinch, scale, grab, release

### Per-Command Synonyms

Each `StaticCommand` in the VOS seed file includes its own synonym list:
```json
["browser_grab", "grab", ["grab this", "grab element", "lock", "lock element", "hold", "latch"], "Grab/start dragging an element"]
```

Both systems work together: verb synonyms handle isolated verbs ("lock" -> "grab"), while command phrases handle multi-word patterns ("lock element" -> GRAB directly).

## 6. Help Menu Integration

### 8 Categories

| # | ID | Title | Icon | Color | Source |
|---|-----|-------|------|-------|--------|
| 1 | navigation | Navigation | navigation | #4285F4 Blue | Registry: NAVIGATION |
| 2 | app_control | App Control | apps | #34A853 Green | Registry: APP_LAUNCH + APP_CONTROL + templates |
| 3 | ui_interaction | UI Interaction | touch_app | #FBBC04 Yellow | Registry: ACCESSIBILITY + templates |
| 4 | text_input | Text Input | keyboard | #EA4335 Red | Registry: TEXT + INPUT + templates |
| 5 | system | System | settings | #9C27B0 Purple | Registry: SYSTEM |
| 6 | media | Media | play_circle | #FF5722 Orange | Registry: MEDIA + templates |
| 7 | voiceos | VoiceOS | mic | #00BCD4 Cyan | Registry: VOICE_CONTROL |
| 8 | web_gestures | Web Gestures | gesture | #E91E63 Pink | Registry: BROWSER + WEB_GESTURE |

### HelpCommandDataProvider (DB-Backed)

Non-template commands derive from `StaticCommandRegistry.byCategory()`. Template commands (with `[element]`, `[text]`, `[number]` patterns) remain static since they document parametric usage.

- `getCategories()` -> 8 HelpCategory objects (registry-derived + templates)
- `getQuickReference()` -> flat list for table view
- `searchCommands(query)` -> filtered by phrase/description
- `getTotalCommandCount()` -> sum across all categories
- `getAllPhrases()` -> for speech engine registration

### HelpScreenHandler

Manages help screen state and command injection:
- `EXECUTABLE_COMMANDS` set -- commands safe to execute on tap (no parameters)
- `onCommandTapped(phrase)` -> execute or copy-to-input
- `searchCommands(query)` -> delegates to provider
- Tracks recently used commands

## 7. Adding New Voice Commands

### Step 1: Add CommandActionType
In `CommandModels.kt` -> `CommandActionType` enum, add new entry.

### Step 2: Add to VOS Seed File
In `en-US.app.vos` (for app commands) or `en-US.web.vos` (for web/gesture commands), add the command to the `commands` array, and update `action_map` and optionally `category_map`/`meta_map`:

```json
// In action_map:
"my_new_cmd": "MY_NEW_ACTION"

// In commands array:
["my_new_cmd", "primary phrase", ["synonym1", "synonym2"], "Description"]
```

### Step 3: Force Reload DB
On next app launch with version bump (change `requiredVersion` in CommandLoader), the new command flows automatically through DB -> StaticCommandRegistry -> HelpCommandDataProvider -> WebCommandHandler.

For development: call `CommandLoader.forceReload()` to reload immediately.

### Step 4: Update Hardcoded Fallback (DEPRECATED 260212)
**REMOVED**: Hardcoded fallback lists no longer exist in `StaticCommandRegistry.kt`. All commands MUST be defined in VOS seed files. The registry returns an empty list until `initialize()` is called with DB data.

### Step 5: Add Handler Support

**For web commands:** Add case in `WebCommandHandler.resolveWebActionType()` and `resolveFromPhrase()`. Add corresponding `WebActionType` enum entry and JS implementation in `DOMScraperBridge`.

**For Android commands:** Add case in `AndroidGestureHandler.execute()`. Use `AndroidGestureDispatcher` methods (tap, scroll, fling, pinch, doubleTap, drag) or implement new dispatch method.

### Step 6: Add Verb Synonyms (if new verb)
Add `SynonymEntry` to `SynonymRegistry` for the new verb.

## 7.5 WebAvanue Disambiguation Fix (260222)

WebCommandHandler disambiguation for web elements has been fixed in two key areas:

**1. Disambiguation Match Indexing** — `selectDisambiguationOption()` now properly indexes into `lastDisambiguationMatches` using the selected option number. Previously, the wrong index could select an incorrect element when multiple candidates shared the same phrase.

**2. Nonce Validation in Scrape Results** — `sendScrapeResult()` now validates incoming nonce values against the current session nonce before accepting scraped element updates. This prevents stale scrape results from a previous session/page from corrupting the command registry. Nonce mismatch → log warning + discard.

**3. Google Cloud Audio Queue Rebuild** — The Google Cloud STT audio queue is now properly rebuilt per session/page context. Previously, queue state could persist across page navigations, causing audio from old pages to be processed in new contexts. Now: session change → new queue → fresh audio stream.

These fixes ensure web element command resolution is accurate, resilient to async scraping, and properly scoped to the active page/session.

---

## 8. Multi-Locale Runtime Support

### Supported Locales (v1.0)

| Locale | Language | Phrase Style | Example: "go back" |
|--------|----------|-------------|---------------------|
| en-US | English (US) | Standard English | "go back" |
| es-ES | Spanish (Spain) | Natural spoken Spanish | "ir atras" |
| fr-FR | French (France) | Natural spoken French | "retour" |
| de-DE | German (Germany) | Informal "du" form | "geh zurueck" |
| hi-IN | Hindi (India) | Romanized Hindi | "peeche jao" |

All locale files contain 107 commands with identical `command_id`, `category_map`, `action_map`, and `meta_map` keys. Only the `primary_phrase`, `synonyms`, and `description` strings are translated.

### Locale Switching Architecture

```
Settings UI (VoiceControlSettingsProvider)
    -> SettingsDropdownRow (5 locales)
    -> AvanuesSettingsRepository.updateVoiceLocale(locale)
    -> DataStore write: "voice_command_locale" key  (see Ch96: ISettingsStore<T>)
        |
        v
VoiceAvanueAccessibilityService (collectLatest observer)
    -> detects locale change via previousVoiceLocale tracking
    -> CommandManager.switchLocale(newLocale)
        |
        v
CommandManager.switchLocale()
    -> CommandLoader.forceReload()     <- clears version, reloads VOS for new locale
    -> loadDatabaseCommands()          <- rebuilds pattern matching cache
    -> StaticCommandRegistry.reset()   <- clears DB-backed command cache
    -> populateStaticRegistryFromDb()  <- repopulates from new locale's DB entries
        |
        v
All consumers auto-update:
    -> Speech grammar (new locale phrases)
    -> Help screen (translated commands)
    -> WebCommandHandler (translated supported actions)
```

### DataStore Key

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `voice_command_locale` | String | `"en-US"` | Active voice command locale |

### Settings UI

Located in `VoiceControlSettingsProvider.kt`:
- `SUPPORTED_LOCALES`: Static list of 5 locale pairs (code -> display name)
- Uses `SettingsDropdownRow` component from AvanueUI
- Changes trigger `repository.updateVoiceLocale(locale)` via coroutine
- Searchable keywords: "language", "locale", "spanish", "french", "german", "hindi"

### Locale Translation Guidelines

When translating VOS files:

1. **Primary phrases**: Use natural spoken language (not formal/literary)
2. **Synonyms**: Include common alternatives a real speaker would use
3. **ASCII-safe**: All triggers must be ASCII-compatible for STT engines (e.g., "zurueck" not "zuruck")
4. **Hindi**: For hi-IN, use romanized Hindi with common English loanwords (e.g., "settings kholo", "volume badhao")
5. **Code-mixed OK**: Speakers naturally mix languages; embrace this for hi-IN especially
6. **Keep IDs identical**: `command_id`, map keys, and `version`/`locale`/`fallback` structure must match en-US exactly

## 9. Adding New Languages

### Step 1: Create Locale Seed Files
Copy `en-US.app.vos` to `{locale}.app.vos` and `en-US.web.vos` to `{locale}.web.vos` in `assets/localization/commands/`.
Translate all `primary_phrase` and `synonym` strings. Keep `command_id`, `action_map`, `category_map`, and `meta_map` keys identical (they are code identifiers, not user-facing).

### Step 2: Add to Supported Locales
In `VoiceControlSettingsProvider.kt`, add the new locale to `SUPPORTED_LOCALES`:
```kotlin
companion object {
    val SUPPORTED_LOCALES = listOf(
        "en-US" to "English (US)",
        "es-ES" to "Espanol (Espana)",
        // ... existing locales ...
        "ja-JP" to "Japanese"  // <- NEW
    )
}
```

### Step 3: CommandLoader Auto-Discovery
`CommandLoader.getAvailableLocales()` scans the `localization/commands/` directory for `.VOS` files. No additional code changes needed -- new locale files are discovered automatically.

### Step 4: Test Locale Switching
1. Build and install the app
2. Open Settings -> Voice Control -> Voice Command Language
3. Select the new locale
4. Verify the accessibility service logs: "Voice commands switched to {locale}"
5. Test voice recognition with translated phrases

### Step 5: Test STT Recognition
Verify the target STT engine (Vivoka/Whisper/Google) recognizes translated phrases. Some engines may require language model configuration.

### Step 6: Verify Help Screen
`HelpCommandDataProvider` automatically reflects the active locale's commands since it derives from `StaticCommandRegistry`, which is populated from the DB after locale switch.

---

*Chapter 93 | Voice Command Pipeline & Localization Architecture*
*Author: VOS4 Development Team | Created: 2026-02-11 | Updated: 2026-02-16 (Parser section updated: VosParser KMP replaces deleted ArrayJsonParser)*
*Related: Chapter 03 (VoiceOSCore Deep Dive), Chapter 05 (WebAvanue Deep Dive), Chapter 94 (4-Tier Voice Enablement), Chapter 95 (VOS Distribution)*
