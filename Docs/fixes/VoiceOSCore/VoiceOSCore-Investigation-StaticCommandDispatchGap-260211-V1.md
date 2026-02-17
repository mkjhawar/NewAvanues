# VoiceOSCore Investigation: Static Command Dispatch Gap

**Date**: 2026-02-11
**Branch**: `VoiceOSCore-KotlinUpdate`
**Status**: Root cause identified, fix not yet applied
**Severity**: High -- ~70% of defined static commands silently fail to execute

---

## Problem Statement

User confirmed all 107 static commands (from VOS seed files) are registered with the VoiceEngine's speech grammar. However, only a small subset actually execute when spoken. The rest are recognized by speech but produce no action ("Unknown command").

## Test Results Summary

| Category | Total Commands | Working | Failing | Notes |
|----------|--------------|---------|---------|-------|
| Navigation (basic) | 4 | 3 | 1 | "previous screen" not in handler |
| Navigation (scroll) | 4 | 2 | 2 | scroll left/right, page up/down missing |
| Media | 7 | 0 | 7 | No MediaHandler |
| System (settings/notif) | 4 | 3 | 1 | "show settings", "device settings" not in handler |
| System (toggles) | 8 | 0 | 8 | No handler for wifi/bt/brightness/screenshot/flashlight/rotate |
| VoiceOS Control | 10 | 0 | 10 | No VoiceControlHandler |
| App Launch | 8 | 7 | 1 | "open web browser" phrasing mismatch |
| Accessibility | 4 | 0 | 4 | Handler exists but commands lack target AVID |
| Text Editing | 7 | 0 | 7 | No TextHandler |
| Input | 2 | 0 | 2 | No InputHandler |
| App Control | 1 | 0 | 1 | No AppControlHandler |
| Screen | 6 | 0 | 6 | No ScreenHandler |
| Cursor | 3 | 3* | 0 | *Recognized but "Cursor not active" |
| Reading | 2 | 0 | 2 | No ReadingHandler |
| Browser | 14 | 0 | 14 | Requires WebAvanue RPC |
| Web Gesture | 25 | 0 | 25 | Requires WebAvanue RPC |
| **TOTAL** | **109** | **~18** | **~91** | **~83% failure rate** |

---

## Root Cause

### Architecture: Two Disconnected Dispatch Paths

There are two separate command execution pipelines in VoiceOSCore. Only one is wired to the speech recognition output. The other has action implementations but is never invoked.

#### Path A: ActionCoordinator (ACTIVE -- receives speech)

```
Speech (Vivoka/Android STT)
  -> VoiceOSCore.speechResults (Flow<SpeechResult>)
  -> VoiceAvanueAccessibilityService.speechCollectorJob
  -> processVoiceCommand(text, confidence)
  -> VoiceOSAccessibilityService.processVoiceCommand()
  -> ActionCoordinator.processVoiceCommand()
    -> Step 1: Dynamic command registry (web/scraped elements) -- no match for static commands
    -> Step 2: handlerRegistry.canHandle(phrase) -- checks 4 registered handlers
    -> Step 3: VoiceInterpreter keyword fallback -- limited pattern matching
    -> Returns "Unknown command" if all fail
```

**Registered Handlers (AndroidHandlerFactory):**
1. `AndroidGestureHandler` (NAVIGATION) -- tap, click, scroll, swipe, pinch, fling, grab, zoom
2. `SystemHandler` (SYSTEM) -- back, home, recents, notifications, quick settings, power menu, lock
3. `AppHandler` (APP) -- "open {app}" pattern matching
4. `AndroidCursorHandler` (GAZE) -- show/hide/click cursor

**File**: `VoiceOSCoreAndroidFactory.kt:57-65`

#### Path B: CommandManager + ActionFactory (DORMANT -- never receives speech)

```
CommandManager.executeCommand(command)
  -> matchCommandTextToId(text) -- pattern match against DB
  -> getActionForCommandId(id) -- ActionFactory.createAction(commandId, category)
  -> action.invoke(command) -- execute the dynamic action
```

**ActionFactory supports 16 action types:**
- DynamicNavigationAction (global accessibility actions)
- DynamicVolumeAction (AudioManager media key events)
- DynamicBluetoothAction (SystemActions delegate)
- DynamicWiFiAction (SystemActions delegate)
- DynamicScrollAction (accessibility node scroll + gesture fallback)
- DynamicCursorAction (text cursor movement in edit fields)
- DynamicEditingAction (copy/paste/cut/select_all/undo/redo via accessibility)
- DynamicBrowserAction (placeholder -- not implemented)
- DynamicMediaAction (AudioManager media key events)
- DynamicUIAction (placeholder -- not implemented)
- DynamicInteractionAction (gesture dispatch for tap/long press/swipe)
- DynamicOverlayAction (placeholder -- not implemented)
- DynamicKeyboardAction (InputMethodManager show/hide/toggle)
- DynamicAppAction (PackageManager app launch)
- DynamicPositionAction (placeholder -- not implemented)
- DynamicIntentAction (startActivity with intent action)

**File**: `ActionFactory.kt`

### Why Commands Fail -- Category by Category

#### 1. Media Commands (7 commands, ALL failing)
- VOS defines: `media_play`, `media_pause`, `media_next`, `media_prev`, `media_vol_up`, `media_vol_down`, `media_mute`
- User says: "play music", "pause", "volume up"
- ActionCoordinator Step 2: No handler's `supportedActions` contains "play music", "pause music", "volume up", "volume down", "mute"
- ActionFactory HAS `DynamicMediaAction` and `DynamicVolumeAction` but they're never called
- **Root cause**: No `MediaHandler` in `AndroidHandlerFactory.createHandlers()`

#### 2. System Toggles (8 commands, ALL failing)
- VOS defines: `sys_wifi`, `sys_bluetooth`, `sys_bright_up`, `sys_bright_down`, `sys_screenshot`, `sys_flash_on`, `sys_flash_off`, `sys_rotate`
- User says: "toggle wifi", "wifi on", "take screenshot", "flashlight on", "brightness up"
- SystemHandler's `supportedActions` only has: "go back", "back", "go home", "home", "show recents", "recents", "recent apps", "show notifications", "notifications", "quick settings", "power menu", "power dialog", "lock screen", "lock"
- No wifi/bluetooth/brightness/screenshot/flashlight/rotate
- ActionFactory HAS `DynamicWiFiAction`, `DynamicBluetoothAction` but never called
- **Root cause**: SystemHandler is too narrow; no ScreenHandler exists

#### 3. VoiceOS Control (10 commands, ALL failing)
- VOS defines: `voice_mute`, `voice_wake`, `voice_dict_start`, `voice_dict_stop`, `voice_help`, `voice_num_on`, `voice_num_off`, `voice_num_auto`, `voice_cursor_show`, `voice_cursor_hide`, `voice_cursor_click`
- Cursor commands (last 3) ARE handled by AndroidCursorHandler
- But mute/wake/dictation/help/numbers -- no handler exists
- **Root cause**: No `VoiceControlHandler` registered

#### 4. Text Editing (7 commands, ALL failing)
- VOS defines: `text_copy`, `text_paste`, `text_cut`, `text_select_all`, `text_undo`, `text_redo`, `text_delete`
- ActionFactory HAS `DynamicEditingAction` with full accessibility implementation
- **Root cause**: No `TextHandler` registered

#### 5. Accessibility (4 commands, ALL failing)
- VOS defines: `acc_click`, `acc_long_click`, `acc_zoom_in`, `acc_zoom_out`
- AndroidGestureHandler's `supportedActions` includes "tap", "click", "long press", "zoom in", "zoom out"
- But the VOS synonyms are "click", "tap", "press" (standalone, no target)
- AndroidGestureHandler.canHandle() matches these BUT when executed, the handler expects a target element (AVID bounds) from the command metadata
- Without a target, the handler performs gesture at screen center -- which may not be useful
- **Root cause**: Standalone "click" without target doesn't produce meaningful action

#### 6. Input Commands (2 commands, ALL failing)
- VOS defines: `input_show_kb`, `input_hide_kb`
- ActionFactory HAS `DynamicKeyboardAction` with InputMethodManager implementation
- **Root cause**: No `InputHandler` registered

#### 7. App Control (1 command, failing)
- VOS defines: `appctl_close`
- **Root cause**: No `AppControlHandler` registered

#### 8. Reading (2 commands, ALL failing)
- VOS defines: `acc_read`, `acc_stop_read`
- **Root cause**: No `ReadingHandler` registered; TTS not integrated

#### 9. Browser & Web Gesture (39 commands, ALL failing)
- These require WebAvanue RPC integration
- **Root cause**: These should route via `CommandManager.executeExternalCommand()` to WebAvanue, but ActionCoordinator doesn't call CommandManager

#### 10. Partially Working: Some Navigation
- "previous screen" fails -- not in SystemHandler's supportedActions (has "go back" but not "previous screen")
- "open recents" fails -- not in SystemHandler's supportedActions (has "show recents" but not "open recents")
- "app drawer", "all apps" fail -- not in any handler's supportedActions
- **Root cause**: VOS synonym phrases don't match handler's hardcoded supportedActions list

---

## The ActionFactory Category Mismatch (Secondary Issue)

Even IF CommandManager were wired to the speech pipeline, there's a secondary bug:

`ActionFactory.createAction(commandId, category)` at line 62 routes based on `category.lowercase()`:
```kotlin
when (category.lowercase()) {
    "go", "navigate", "nav" -> createNavigationAction(commandId)
    "volume", "mute", "unmute" -> createVolumeAction(commandId)
    ...
}
```

But the `category` parameter comes from the database, which stores the FULL category name from the VOS category_map:
- `"nav"` -> `"NAVIGATION"` (stored in DB)
- `"media"` -> `"MEDIA"` (stored in DB)
- `"sys"` -> `"SYSTEM"` (stored in DB)

So `ActionFactory.createAction("media_play", "MEDIA")` receives `category = "MEDIA"`:
- `"media".lowercase()` = `"media"` -> matches line 85 "media" case -> works!

Actually on closer inspection, the category routing in ActionFactory DOES handle the full names:
- Line 85: `"media"` matches media commands
- Line 70: `"system"` matches system commands
- Line 79: `"text"` matches editing commands
- Line 97: `"input"` matches keyboard commands
- Line 82: `"browser"` matches browser commands

So `"MEDIA".lowercase()` = `"media"` -> matches. `"SYSTEM".lowercase()` = `"system"` -> matches. The category routing is actually correct.

But `"NAVIGATION".lowercase()` = `"navigation"` -> does NOT match "go", "navigate", "nav" -> falls to `else` -> `inferActionFromCommandId()` -> this uses commandId content to infer, which usually works (e.g., "nav_back" contains "back").

**Conclusion**: ActionFactory category routing is mostly functional but brittle. The `else` fallback (`inferActionFromCommandId`) catches most cases via commandId content matching.

---

## Fix Options

### Option A: Bridge ActionCoordinator -> CommandManager (Minimal Changes)

Add a Step 2.5 in `ActionCoordinator.processVoiceCommand()`:
```kotlin
// Step 2.5: Try CommandManager pattern matching + ActionFactory
val commandManager = CommandManager.getInstance(context)
val cmdResult = commandManager.executeCommand(Command(
    id = normalizedText,
    text = normalizedText,
    confidence = confidence
))
if (cmdResult.success) {
    return HandlerResult.success(cmdResult.message ?: "Command executed")
}
```

**Pros**: Minimal code changes, leverages existing ActionFactory implementations
**Cons**: Dual dispatch paths create complexity, ActionFactory actions need AccessibilityService context

### Option B: Expand Handler Registration (Clean Architecture)

Create new `IHandler` implementations for each missing category and register in `AndroidHandlerFactory`:
- `MediaHandler` -- media key events via AudioManager
- `ScreenHandler` -- brightness, wifi, bluetooth, screenshot, flashlight, rotate
- `TextHandler` -- copy, paste, cut, select all, undo, redo, delete via accessibility
- `InputHandler` -- show/hide keyboard via InputMethodManager
- `VoiceControlHandler` -- mute/wake voice, dictation, help overlay, numbers mode
- `AppControlHandler` -- close/kill app
- `ReadingHandler` -- TTS read screen / stop reading

Also expand `SystemHandler.supportedActions` to include synonym phrases from VOS.

**Pros**: Clean architecture, each handler is self-contained, testable
**Cons**: More code to write, some duplication with ActionFactory implementations

### Option C: Hybrid -- Expand SystemHandler + Bridge for ActionFactory (Recommended)

1. Expand `SystemHandler.supportedActions` to include ALL VOS synonym phrases
2. Create `MediaHandler`, `TextHandler`, `InputHandler` -- extract from ActionFactory
3. Bridge browser/web gesture commands to WebAvanue via RPC
4. Add `VoiceControlHandler` for mute/wake/dictation/help/numbers

**Pros**: Combines strengths of both approaches
**Cons**: Moderate effort

---

## Files Involved

| File | Role | Changes Needed |
|------|------|---------------|
| `VoiceOSCoreAndroidFactory.kt:57-65` | Handler registration | Add new handlers |
| `SystemHandler.kt` | System actions | Expand supportedActions |
| `ActionCoordinator.kt:496-522` | Command dispatch | Add CommandManager bridge OR new handler check |
| `ActionFactory.kt` | Dynamic action creation | Fix "NAVIGATION" category routing |
| `en-US.VOS` | Command definitions | Reference for phrases |
| `BaseHandler.kt` | Handler base class | May need canHandle improvements |

## New Files Needed (Option B/C)

| File | Category | Actions |
|------|----------|---------|
| `MediaHandler.kt` | MEDIA | play, pause, next, prev, stop, vol up/down, mute |
| `ScreenHandler.kt` | SYSTEM | brightness, wifi, bluetooth, screenshot, flashlight, rotate |
| `TextHandler.kt` | TEXT | copy, paste, cut, select all, undo, redo, delete |
| `InputHandler.kt` | INPUT | show/hide keyboard |
| `VoiceControlHandler.kt` | VOICE_CONTROL | mute, wake, dictation, help, numbers |
| `AppControlHandler.kt` | APP_CONTROL | close app |
| `ReadingHandler.kt` | ACCESSIBILITY | read screen, stop reading (TTS) |

---

## Recommendation

**Option C (Hybrid)** is the long-term optimal approach:
1. New handlers keep the clean IHandler architecture
2. Most implementation logic already exists in ActionFactory -- extract into handlers
3. VoiceInterpreter rules can bridge VOS synonym phrases to handler-supported phrases
4. Browser/web gesture commands should route via RPC (existing `executeExternalCommand()`)

**Risk if not fixed**: 83% of defined voice commands silently fail, undermining user trust in the voice control system.

---

## Quick Win: VoiceInterpreter Rules

As an interim measure, add VoiceInterpreter rules to map VOS phrases to existing handler phrases:
```kotlin
// Map VOS phrases to SystemHandler phrases
addRule(setOf("navigate back", "previous screen"), "go back")
addRule(setOf("open recents", "app switcher"), "show recents")
addRule(setOf("show settings", "device settings"), "open settings")  // needs SystemHandler expansion
addRule(setOf("notification panel"), "show notifications")
```

This would fix phrase mismatches for existing handlers without new handler code.
