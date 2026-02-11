# Chapter 93: Voice Command Pipeline & Localization Architecture

## 1. Architecture Overview

VoiceOS uses a layered pipeline that flows from a single source of command definitions through multiple consumers. The current source is `StaticCommandRegistry` (hardcoded Kotlin); the target architecture replaces this with database-backed VOS seed files per locale.

```
Source of Truth (StaticCommandRegistry / future: DB)
        │
        ├── StaticCommandRegistry.all()
        │       ├── Speech engine vocabulary (allPhrases)
        │       ├── NLU schema (toNluSchema)
        │       └── QuantizedCommand export (allAsQuantized)
        │
        ├── HelpCommandDataProvider.getCategories()
        │       └── 8 categories → Help screen UI
        │
        ├── WebCommandHandler.supportedActions
        │       └── Derived from BROWSER + WEB_GESTURE + TEXT categories
        │
        ├── AndroidGestureHandler.supportedActions
        │       └── Native gesture dispatch (parallel to web JS)
        │
        └── SynonymRegistry.all()
                └── Verb canonicalization for NLU
```

## 2. Command Pipeline (5-Layer)

Voice commands flow through 5 layers from speech recognition to execution:

### Layer 1: CommandActionType (Enum)
Defines WHAT action to perform. Platform-agnostic enum in `CommandModels.kt`.
- 70+ action types across 12 categories
- Each type maps to both web (JS) and native (Android) execution paths
- `isBrowserAction()`, `isSystemAction()`, `isMediaAction()` etc. for routing

### Layer 2: StaticCommandRegistry (Command Definitions)
Maps voice PHRASES to CommandActionTypes. Organized by category lists:
- `navigationCommands` (8) — back, home, scroll, app drawer
- `mediaCommands` (7) — play, pause, volume, track skip
- `systemCommands` (6) — settings, notifications, screenshot, flashlight
- `screenCommands` (6) — brightness, lock, rotate, wifi, bluetooth
- `voiceOSCommands` (8) — mute, wake, dictation, numbers overlay
- `cursorCommands` (3) — show/hide/click cursor
- `appCommands` (8) — open browser/camera/gallery/calculator/etc.
- `appControlCommands` (1) — close app
- `accessibilityCommands` (4) — click, long press, zoom
- `textCommands` (7) — select all, copy, paste, cut, undo, redo, delete
- `readingCommands` (2) — read screen, stop reading
- `inputCommands` (2) — show/hide keyboard
- `browserCommands` (19) — retrain, navigation, forms, swipes, grab/release, rotate
- `webGestureCommands` (27) — pan, tilt, orbit, rotate x/y/z, pinch, fling, throw, scale, etc.

### Layer 3: ActionCoordinator (Routing)
Routes QuantizedCommands to the appropriate handler based on:
1. Command metadata (source="web" → WebCommandHandler)
2. Handler's `canHandle()` check (phrase matching + category)
3. Priority: Web handler > Gesture handler > System handler > App handler

### Layer 4: Handlers (Platform-Specific Execution)
- **WebCommandHandler** → resolves WebActionType → delegates to IWebCommandExecutor
- **AndroidGestureHandler** → dispatches via AndroidGestureDispatcher (AccessibilityService)
- **SystemHandler** → global actions (back, home, recents)
- **AppHandler** → app launch via Intent
- **AndroidCursorHandler** → cursor overlay service

### Layer 5: Execution
- **Web:** JavaScript injection via DOMScraperBridge → WebView evaluateJavascript
- **Android:** GestureDescription API via AccessibilityService.dispatchGesture()

## 3. Gesture Commands

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
| Reset Zoom | RESET_ZOOM | `VOS.gesture.resetZoom()` | "reset zoom", "normal zoom" | — |
| Select Word | SELECT_WORD | `VOS.gesture.selectWord(el)` | "select word", "pick word" | — |
| Clear Selection | CLEAR_SELECTION | `VOS.gesture.clearSelection()` | "clear selection", "deselect" | — |
| Hover Out | HOVER_OUT | `VOS.gesture.hoverOut(el)` | "hover out", "unhover" | — |

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

## 4. Synonym System

### SynonymRegistry (Verb Canonicalization)

The NLU engine uses `SynonymRegistry` to normalize user speech into canonical verbs:

```
User says: "flick down" → canonical: "fling" → CommandActionType.FLING
User says: "lock element" → canonical: "grab" → CommandActionType.GRAB
User says: "squeeze" → canonical: "pinch" → CommandActionType.PINCH
```

33 entries organized by domain:
- **Element interaction** (2): click → tap/press/push/select/hit; long press → long click/hold
- **Scroll** (4): scroll up/down/left/right with swipe/go/page variants
- **Navigation** (4): open, close, back, home with multiple phrasings
- **Search & input** (2): search → find/look for; type → enter/input/write
- **Text editing** (7): delete, copy, paste, cut, undo, redo, select
- **Zoom** (2): zoom in → magnify/enlarge; zoom out → shrink/smaller
- **Media/TTS** (2): mute → silence; read → speak/narrate
- **Gestures** (9): pan, tilt, orbit, fling, throw, pinch, scale, grab, release

### Per-Command Synonyms

Each `StaticCommand` has its own phrase list (distinct from verb synonyms):
```kotlin
StaticCommand(
    phrases = listOf("grab", "grab element", "lock", "lock element", "hold", "latch"),
    actionType = CommandActionType.GRAB,
    ...
)
```

Both systems work together: verb synonyms handle isolated verbs ("lock" → "grab"), while command phrases handle multi-word patterns ("lock element" → GRAB directly).

## 5. Help Menu Integration

### 8 Categories

| # | ID | Title | Icon | Color | Commands |
|---|-----|-------|------|-------|----------|
| 1 | navigation | Navigation | navigation | #4285F4 Blue | 7 |
| 2 | app_control | App Control | apps | #34A853 Green | 6 |
| 3 | ui_interaction | UI Interaction | touch_app | #FBBC04 Yellow | 7 |
| 4 | text_input | Text Input | keyboard | #EA4335 Red | 9 |
| 5 | system | System | settings | #9C27B0 Purple | 9 |
| 6 | media | Media | play_circle | #FF5722 Orange | 8 |
| 7 | voiceos | VoiceOS | mic | #00BCD4 Cyan | 8 |
| 8 | web_gestures | Web Gestures | gesture | #E91E63 Pink | 23 |

### HelpCommandDataProvider

Provides structured data for the help screen UI:
- `getCategories()` → 8 HelpCategory objects
- `getQuickReference()` → flat list for table view
- `searchCommands(query)` → filtered by phrase/description
- `getTotalCommandCount()` → sum across all categories
- `getAllPhrases()` → for speech engine registration

### HelpScreenHandler

Manages help screen state and command injection:
- `EXECUTABLE_COMMANDS` set — commands safe to execute on tap (no parameters)
- `onCommandTapped(phrase)` → execute or copy-to-input
- `searchCommands(query)` → delegates to provider
- Tracks recently used commands

## 6. Localization Architecture (Future)

### Target: VOS Seed Files

```
assets/localization/commands/
├── en-US.VOS    ← English (default)
├── es-ES.VOS    ← Spanish
├── fr-FR.VOS    ← French
├── de-DE.VOS    ← German
└── ja-JP.VOS    ← Japanese
```

Each VOS file is a JSON array:
```json
[
  {
    "id": "nav_back",
    "locale": "en-US",
    "trigger": "go back",
    "synonyms": ["back", "navigate back", "previous screen"],
    "action": "BACK",
    "category": "NAVIGATION",
    "description": "Navigate to previous screen"
  }
]
```

### Flow: VOS → DB → Runtime

1. `CommandLoader.initializeCommands()` runs on first launch / version bump
2. Parses VOS seed file for device locale (fallback: en-US)
3. Seeds `commands_static` table in SQLDelight
4. StaticCommandRegistry loads from DB at runtime
5. HelpCommandDataProvider derives categories from DB
6. WebCommandHandler derives supportedActions from registry

### STT Engine Compatibility

The localization system is designed for three STT backends:
- **Vivoka** (embedded, offline) — custom vocabulary from DB phrases
- **Whisper** (local model) — free-form recognition, post-match against DB
- **Google STT** (cloud) — grammar hints from DB phrases

## 7. Adding New Voice Commands

### Step 1: Add CommandActionType
In `CommandModels.kt` → `CommandActionType` enum, add new entry.

### Step 2: Register in StaticCommandRegistry
Add `StaticCommand` to appropriate category list with:
- `phrases` — primary + synonym variations
- `actionType` — the new CommandActionType
- `category` — CommandCategory enum value
- `description` — human-readable
- `metadata` — optional key-value pairs (direction, factor, etc.)

### Step 3: Add to HelpCommandDataProvider
Create `HelpCommand` entry in the appropriate category list.

### Step 4: Add Handler Support

**For web commands:** Add case in `WebCommandHandler.resolveWebActionType()` and `resolveFromPhrase()`. Add corresponding `WebActionType` enum entry and JS implementation in `DOMScraperBridge`.

**For Android commands:** Add case in `AndroidGestureHandler.execute()`. Use `AndroidGestureDispatcher` methods (tap, scroll, fling, pinch, doubleTap, drag) or implement new dispatch method.

### Step 5: Add Verb Synonyms (if new verb)
Add `SynonymEntry` to `SynonymRegistry` for the new verb.

### Step 6: Update EXECUTABLE_COMMANDS
If the command can be executed without parameters, add to `HelpScreenHandler.EXECUTABLE_COMMANDS`.

## 8. Adding New Languages

### Step 1: Create Locale Seed File
Copy `en-US.VOS` to `{locale}.VOS` and translate all trigger/synonym strings.

### Step 2: Register Locale in CommandLoader
Add locale to supported locales list in `CommandLoader`.

### Step 3: Test STT Recognition
Verify the target STT engine (Vivoka/Whisper/Google) recognizes translated phrases.

### Step 4: Update Help Screen
Ensure `HelpCommandDataProvider` loads translated descriptions (from DB) for the active locale.

---

*Chapter 93 | Voice Command Pipeline & Localization Architecture*
*Author: VOS4 Development Team | Created: 2026-02-11*
*Related: Chapter 03 (VoiceOSCore Deep Dive), Chapter 05 (WebAvanue Deep Dive)*
