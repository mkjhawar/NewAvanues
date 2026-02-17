# VOS Command Audit — Full Review of app.vos + web.vos

**Date**: 2026-02-16
**Branch**: `IosVoiceOS-Development`
**Status**: Analysis complete, awaiting approval for implementation
**Scope**: All static voice commands across both domains

---

## Architecture Overview

### Two-Domain VOS System

| Domain | File | Lifecycle | When Active |
|--------|------|-----------|-------------|
| **App** | `.app.vos` | Always | Service start → service stop |
| **Web** | `.web.vos` | Conditional | Browser foreground → browser background |

**Why separate files?** Identical phrases mean different things per context:
- "go back" → App: Android BACK button (SystemHandler) vs Web: `history.back()` (WebCommandHandler)
- "scroll up" → App: accessibility scroll vs Web: JavaScript page scroll
- The `source="web"` metadata triggers ActionCoordinator's priority bypass (line 175-185)

**For VOS export**, both files are exported as part of the browser module's VOS profile per v2.1 split format.

---

## Current Command Inventory

### App Commands (en-US.app.vos) — 62 commands

| Category | Count | Commands |
|----------|-------|----------|
| NAVIGATION | 8 | back, home, recent apps, app drawer, scroll up/down/left/right |
| MEDIA | 7 | play, pause, next, previous, volume up/down/mute |
| SYSTEM | 12 | settings, notifications, clear notif, screenshot, flashlight on/off, brightness up/down, lock, rotate, wifi, bluetooth |
| VOICE_CONTROL | 11 | mute/wake voice, dictation start/stop, show commands, numbers on/off/auto, cursor show/hide/click |
| APP_LAUNCH | 8 | browser, camera, gallery, calculator, calendar, phone, messages, contacts |
| ACCESSIBILITY | 6 | click, long press, zoom in/out, read screen, stop reading |
| TEXT | 7 | select all, copy, paste, cut, undo, redo, delete |
| INPUT | 2 | show/hide keyboard |
| APP_CONTROL | 1 | close app |

### Web Commands (en-US.web.vos) — 45 commands

| Category | Count | Commands |
|----------|-------|----------|
| BROWSER | 18 | retrain, back, forward, refresh, top, bottom, tab next/prev, submit, swipe x4, grab, release, rotate left/right, double tap |
| WEB_GESTURE | 27 | pan x5, tilt x3, orbit x3, rotate x/y/z, pinch in/out, fling x4, throw, scale up/down, reset zoom, select word, clear selection, hover out |

---

## Gap Analysis

### A. Web Commands Missing (Infrastructure Exists)

These have `WebActionType` enum + `DOMScraperBridge` JS scripts but NO voice phrases in `.web.vos`:

| # | Command | WebActionType | DOMScraperBridge | Proposed Phrases | Category |
|---|---------|---------------|------------------|-----------------|----------|
| 1 | **zoom in** | `ZOOM_IN` | `zoomScript("in")` | "zoom in", "zoom closer", "magnify" | BROWSER |
| 2 | **zoom out** | `ZOOM_OUT` | `zoomScript("out")` | "zoom out", "zoom away" | BROWSER |
| 3 | **scroll up** | `SCROLL_PAGE_UP` | `scrollPageScript("up")` | "scroll up", "page up" | BROWSER |
| 4 | **scroll down** | `SCROLL_PAGE_DOWN` | `scrollPageScript("down")` | "scroll down", "page down" | BROWSER |
| 5 | **long press** | `LONG_PRESS` | `longPressScript()` | "long press", "long click", "press and hold" | BROWSER |
| 6 | **hover** | `HOVER` | `hoverScript()` | "hover", "hover over", "mouse over" | WEB_GESTURE |
| 7 | **tap** | `CLICK` (alias) | `clickBySelectorScript()` | "tap", "tap here" | BROWSER |
| 8 | **focus** | `FOCUS` | `focusBySelectorScript()` | "focus", "focus on" | BROWSER |

### B. Web Commands Missing (Infrastructure Partially Missing)

These need new enum values AND/OR DOMScraperBridge scripts:

| # | Command | Needs | Proposed Phrases | Category |
|---|---------|-------|-----------------|----------|
| 9 | **scroll left** | `SCROLL_PAGE_LEFT` WebActionType + scrollPageScript "left" fix | "scroll left" | BROWSER |
| 10 | **scroll right** | `SCROLL_PAGE_RIGHT` WebActionType + scrollPageScript "right" fix | "scroll right" | BROWSER |
| 11 | **drag left** | DRAG + extractDirectionParams + dragDirectionScript | "drag left" | WEB_GESTURE |
| 12 | **drag right** | DRAG + extractDirectionParams + dragDirectionScript | "drag right" | WEB_GESTURE |
| 13 | **drag up** | DRAG + extractDirectionParams + dragDirectionScript | "drag up" | WEB_GESTURE |
| 14 | **drag down** | DRAG + extractDirectionParams + dragDirectionScript | "drag down" | WEB_GESTURE |
| 15 | **start drawing** | `STROKE_START` enum + strokeStartScript | "start drawing", "begin stroke", "draw" | WEB_GESTURE |
| 16 | **stop drawing** | `STROKE_END` enum + strokeEndScript | "stop drawing", "finish drawing", "end stroke" | WEB_GESTURE |
| 17 | **erase** | `ERASE` enum + eraseScript | "erase", "eraser", "erase mode" | WEB_GESTURE |

### C. App Commands — Synonym Gaps

Existing commands that could benefit from additional natural-language synonyms:

| Command | Current Phrases | Proposed Additional Synonyms |
|---------|----------------|------------------------------|
| scroll down | "scroll down", "page down" | "down" |
| scroll up | "scroll up", "page up" | "up" |
| zoom out | "zoom out", "shrink" | "zoom away" |
| play music | "play music", "play", "resume" | "resume music" |
| close app | "close app", "close this", "exit app", "quit" | "exit" |
| take screenshot | "take screenshot", "screenshot", "capture screen" | "screen capture" |
| toggle wifi | "toggle wifi", "wifi on", ... | "wifi toggle" |
| start dictation | "start dictation", "dictation", "type mode" | "begin dictation" |
| stop reading | "stop reading", "stop", "quiet", "be quiet" | "shut up" (colloquial but natural) |

### D. App Commands — Missing Categories

| # | Category | Missing Commands | Notes |
|---|----------|-----------------|-------|
| 1 | APP_LAUNCH | email, maps, music, clock, weather, notes | Common apps users would want to launch |
| 2 | SYSTEM | do not disturb, airplane mode, NFC, mobile data | Require special permissions/APIs |
| 3 | NAVIGATION | forward (app-level) | Less common, may conflict with browser forward |
| 4 | MEDIA | media stop (distinct from pause), play/pause toggle | "stop" is ambiguous with "stop reading" |
| 5 | ACCESSIBILITY | double tap, drag | Element interaction gestures |

---

## Infrastructure Changes Required

### 1. CommandActionType.kt — New Enum Values

```kotlin
// Drawing/Annotation Actions
STROKE_START,  // Begin drawing stroke
STROKE_END,    // End drawing stroke
ERASE,         // Erase at position
```

Also update `isBrowserAction()` to include: `ZOOM_IN, ZOOM_OUT, SCROLL_LEFT, SCROLL_RIGHT, STROKE_START, STROKE_END, ERASE`

### 2. WebActionType (IWebCommandExecutor.kt) — New Enum Values

```kotlin
SCROLL_PAGE_LEFT,   // Horizontal page scroll left
SCROLL_PAGE_RIGHT,  // Horizontal page scroll right
STROKE_START,       // Begin drawing stroke
STROKE_END,         // End drawing stroke
ERASE,              // Erase at position
```

### 3. WebCommandHandler.kt — New Mappings

resolveWebActionType():
```kotlin
CommandActionType.SCROLL_LEFT -> WebActionType.SCROLL_PAGE_LEFT
CommandActionType.SCROLL_RIGHT -> WebActionType.SCROLL_PAGE_RIGHT
CommandActionType.STROKE_START -> WebActionType.STROKE_START
CommandActionType.STROKE_END -> WebActionType.STROKE_END
CommandActionType.ERASE -> WebActionType.ERASE
```

resolveFromPhrase() additions:
```kotlin
"scroll up", "page up" -> SCROLL_PAGE_UP
"scroll down", "page down" -> SCROLL_PAGE_DOWN
"scroll left" -> SCROLL_PAGE_LEFT
"scroll right" -> SCROLL_PAGE_RIGHT
"drag left/right/up/down" -> DRAG (with extractDirectionParams)
"start drawing", "begin stroke", "draw" -> STROKE_START
"stop drawing", "finish drawing" -> STROKE_END
"erase", "eraser" -> ERASE
"long click", "press and hold" -> LONG_PRESS (already has "long press")
"mouse over" -> HOVER (already has "hover")
"tap here" -> CLICK (already has "tap")
"focus on" -> FOCUS (already has "focus")
```

extractDirectionParams() — add DRAG case:
```kotlin
WebActionType.DRAG -> {
    val dist = "100"
    // compute startX+offset, startY+offset
}
```

### 4. WebCommandExecutorImpl.kt — New buildScript() Cases

```kotlin
WebActionType.SCROLL_PAGE_LEFT -> DOMScraperBridge.scrollPageHorizontalScript("left")
WebActionType.SCROLL_PAGE_RIGHT -> DOMScraperBridge.scrollPageHorizontalScript("right")
WebActionType.STROKE_START -> DOMScraperBridge.strokeStartScript(selector)
WebActionType.STROKE_END -> DOMScraperBridge.strokeEndScript()
WebActionType.ERASE -> DOMScraperBridge.eraseScript(selector)
```

### 5. DOMScraperBridge.kt — New JS Scripts

```kotlin
// Horizontal page scroll (existing scrollPageScript only handles up/down)
fun scrollPageHorizontalScript(direction: String): String

// Directional drag (existing dragScript uses absolute coordinates)
fun dragDirectionScript(selector: String, direction: String, distance: String): String

// Drawing gestures (delegates to AvanuesGestures JS library)
fun strokeStartScript(selector: String): String
fun strokeEndScript(): String
fun eraseScript(selector: String): String
```

### 6. en-US.web.vos — 17 New Commands

See Section B above for full list.

### 7. Other Locale .web.vos Files (4 files)

Same 17 commands translated to: es-ES, fr-FR, de-DE, hi-IN

---

## Phrase Overlap Analysis (App vs Web)

These phrases exist in BOTH domains. When browser is active, web version wins via priority bypass:

| Phrase | App Action | Web Action | Conflict? |
|--------|-----------|------------|-----------|
| "go back" | BACK (Android) | PAGE_BACK (JS) | No — web wins in browser |
| "scroll up" | SCROLL_UP (accessibility) | SCROLL_PAGE_UP (JS) | **NEW** — needs web.vos entry |
| "scroll down" | SCROLL_DOWN (accessibility) | SCROLL_PAGE_DOWN (JS) | **NEW** — needs web.vos entry |
| "scroll left" | SCROLL_LEFT (accessibility) | SCROLL_PAGE_LEFT (JS) | **NEW** — needs web.vos entry |
| "scroll right" | SCROLL_RIGHT (accessibility) | SCROLL_PAGE_RIGHT (JS) | **NEW** — needs web.vos entry |
| "zoom in" | ZOOM_IN (device) | ZOOM_IN (JS) | **NEW** — needs web.vos entry |
| "zoom out" | ZOOM_OUT (device) | ZOOM_OUT (JS) | **NEW** — needs web.vos entry |
| "long press" | LONG_CLICK (accessibility) | LONG_PRESS (JS) | **NEW** — needs web.vos entry |
| "tap" | CLICK (accessibility) | CLICK (JS) | **NEW** — needs web.vos entry |
| "copy" / "paste" / "cut" / "select all" | TEXT actions | TEXT actions (JS) | Already in web.vos via action_map |

All overlaps are intentional and handled correctly by the `source="web"` bypass.

---

## Drawing Gestures Design Decision

Drawing gestures (STROKE_START, STROKE_END, ERASE) are web-only for now.

**Future annotation use**: The `CommandActionType` enum values (STROKE_START, STROKE_END, ERASE) are shared across all platforms. When annotation features are added to native Android apps, a separate `AnnotationHandler` would handle these same action types via Android Canvas/accessibility APIs — no .web.vos changes needed.

**Voice-controllable drawing commands:**
- "start drawing" → begins stroke capture at cursor/mouse position
- "stop drawing" → ends current stroke
- "erase" → toggles eraser mode at cursor position

**NOT voice-controllable** (cursor/mouse driven):
- STROKE_MOVE — continuous movement tracked by cursor position

---

## Summary of Proposed Changes

| Layer | Additions | Files |
|-------|-----------|-------|
| CommandActionType enum | +3 (STROKE_START, STROKE_END, ERASE) | CommandActionType.kt |
| WebActionType enum | +5 (SCROLL_PAGE_LEFT/RIGHT, STROKE_START/END, ERASE) | IWebCommandExecutor.kt |
| Handler mappings | +5 resolveWebActionType + ~10 resolveFromPhrase + 1 extractDirectionParams | WebCommandHandler.kt |
| Executor buildScript | +5 cases | WebCommandExecutorImpl.kt |
| DOMScraperBridge JS | +5 new scripts (scrollHoriz, dragDirection, stroke x2, erase) | DOMScraperBridge.kt |
| Web VOS (en-US) | +17 commands (45→62) | en-US.web.vos |
| Web VOS (other locales) | +17 commands per file | es-ES, fr-FR, de-DE, hi-IN .web.vos |
| App VOS synonyms | ~10 additional synonyms (optional) | en-US.app.vos + locales |

**Total: 62 app commands + 62 web commands = 124 static voice commands**
