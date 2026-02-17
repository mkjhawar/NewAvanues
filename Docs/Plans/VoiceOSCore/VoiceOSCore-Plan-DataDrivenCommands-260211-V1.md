# VoiceOSCore-Plan-DataDrivenCommands-260211-V1

## Overview
Full data-driven voice command system with gesture wiring, help menu evolution, and Android gesture parity.

## Problem
All voice phrases were hardcoded English strings across 4 redundant sources:
- StaticCommandRegistry (~60 commands, hardcoded)
- HelpCommandDataProvider (7 categories, hardcoded duplicates)
- WebCommandHandler.supportedActions (~60 web phrases, hardcoded)
- SynonymRegistry (24 verb entries, hardcoded)

## Architecture: Single Source of Truth
```
StaticCommandRegistry (central registry)
        ↓
┌──────────────────────┬──────────────────────┬──────────────────────┐
│ HelpCommandDataProvider │ WebCommandHandler    │ AndroidGestureHandler │
│ (help screen UI)        │ (web JS execution)   │ (native gestures)     │
│ → 8 categories          │ → derived from reg.  │ → parallel dispatch   │
└──────────────────────┴──────────────────────┴──────────────────────┘
```

## Phase 1 Changes (Implemented)

### 1. CommandCategory.WEB_GESTURE
Added to CommandModels.kt enum.

### 2. StaticCommandRegistry — 27 new web gesture commands
Pan (5 variants), Tilt (3), Orbit (3), Rotate X/Y/Z (3), Pinch in/out (2),
Fling (4 directions), Throw (1), Scale up/down (2), Reset zoom (1),
Select word (1), Clear selection (1), Hover out (1).
Total: 27 new + GRAB synonyms updated (lock, lock element, hold, latch).

### 3. SynonymRegistry — 9 new gesture verb entries
pan, tilt, orbit, fling, throw, pinch, scale, grab, release.

### 4. HelpCommandDataProvider — "Web Gestures" category (8th)
23 help entries with variations, id="web_gestures", color=#E91E63 (Pink).

### 5. WebCommandHandler.supportedActions — Registry-derived
Replaced 60+ hardcoded strings with buildList{} pulling from:
- Element interaction verbs (click, tap, press, etc.)
- BROWSER category phrases (from StaticCommandRegistry)
- WEB_GESTURE category phrases (from StaticCommandRegistry)
- TEXT category phrases (from StaticCommandRegistry)

### 6. HelpScreenHandler.EXECUTABLE_COMMANDS — Extended
Added 24 gesture phrases to the executable commands set.

### 7. Android Gesture Dispatch (parallel to JS)
**AndroidGestureDispatcher** — 4 new methods:
- `doubleTap(x, y)` — two taps with 150ms delay
- `fling(direction)` — fast swipe (100ms, 800px)
- `pinch(scale)` — two-stroke concurrent gesture (zoom in/out)
- `drag(startX, startY, endX, endY)` — long press + move (600ms)

**AndroidGestureHandler** — 20+ new CommandActionType cases:
- DOUBLE_CLICK → doubleTap()
- PINCH → pinch() with scale factor
- FLING/THROW → fling() with direction
- PAN → scroll() (equivalent on Android)
- SCALE → pinch() with factor
- GRAB → longPress(), RELEASE → tap()
- DRAG → drag()
- SELECT_WORD → doubleTap()
- CLEAR_SELECTION → tap()
- ZOOM_IN/OUT → pinch(1.5/0.5)
- SWIPE_* → scroll() with direction
- TILT/ORBIT → scroll() (best approximation)
- ROTATE_X/Y/Z, HOVER_OUT → not supported in native (graceful failure)

## Phase 2: Full DB-Driven Migration (Future)
- en-US.VOS seed file with all commands in JSON
- ICommandPhraseProvider interface
- StaticCommandRegistry loads from commands_static table
- HelpCommandDataProvider loads from DB
- Locale seed files (es-ES, fr-FR, de-DE)
- CommandLoader seeds gesture commands from VOS files

## Verification
- BUILD SUCCESSFUL (compileDebugKotlinAndroid)
- StaticCommandRegistry.all() includes 27 new web gesture commands
- HelpCommandDataProvider.getCategories() returns 8 categories (was 7)
- WebCommandHandler.supportedActions matches registry phrases
- SynonymRegistry.canonicalFor("lock") returns "grab"
- SynonymRegistry.canonicalFor("flick") returns "fling"
- AndroidGestureHandler handles 20+ new CommandActionTypes

## Files Modified
| # | File | Change |
|---|------|--------|
| 1 | CommandModels.kt | WEB_GESTURE enum entry |
| 2 | StaticCommandRegistry.kt | 27 webGestureCommands + GRAB synonyms + all() |
| 3 | SynonymRegistry.kt | 9 gesture verb synonym entries |
| 4 | HelpCommandData.kt | webGestureCommands + webGestureCategory + getCategories() |
| 5 | WebCommandHandler.kt | Registry-derived supportedActions |
| 6 | HelpScreenHandler.kt | Gesture phrases in EXECUTABLE_COMMANDS |
| 7 | AndroidGestureDispatcher.kt | doubleTap, fling, pinch, drag methods |
| 8 | VoiceOSCoreAndroidFactory.kt | 20+ new action type cases in AndroidGestureHandler |
| 9 | Chapter 93 (NEW) | Developer manual chapter |
