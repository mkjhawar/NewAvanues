# VoiceOSCore-Fix-VoiceControlCallbackDeadlock-260220-V2

## Problem

24 VoiceControl static voice commands are "identified by Handlers" but perform no visible action on-device:
- Mute/Wake: "mute voice", "stop listening", "voice off", "wake up voice", "start listening", "voice on"
- Dictation: "start dictation", "dictation", "type mode", "stop dictation", "end dictation", "command mode"
- Help: "help"
- List Commands: "what can i say", "list commands", "show all commands"
- Numbers: "numbers on/off/auto", "show/hide numbers", etc.

## Root Cause Analysis (3 bugs + 3 design gaps)

### Bug 1 (CRITICAL): `runBlocking` deadlock on Main thread

**Location**: `VoiceAvanueAccessibilityService.kt` lines 270-311

All 4 speech control callbacks used `runBlocking { core?.stopListening() }` or `runBlocking { core?.startListening() }`.

These callbacks are invoked from `VoiceControlHandler.execute()` → `ActionCoordinator.processCommand()` → `speechCollectorJob` coroutine on `serviceScope` (Dispatchers.Main).

`runBlocking` blocks Main. If the speech engine dispatches to Main internally (Android's `SpeechRecognizer` requires its creation thread) → **DEADLOCK**. The `withTimeoutOrNull(5000)` wrapper cannot cancel `runBlocking`. The entire speech collector hangs permanently.

### Bug 2 (MEDIUM): Duplicate handler registration for numbers commands

Two handlers registered for the same "numbers on/off/auto" phrases:
- `VoiceControlHandler` (SYSTEM, priority 1) — always won
- `NumbersOverlayHandler` (ACCESSIBILITY, priority 10) — correct handler

VoiceControlHandler's path did not clear number assignments or trigger re-scan.

### Bug 3 (MEDIUM): No re-scan after numbers mode change

The 260219 fix documented screen hash invalidation but never implemented it. Badges only appeared after the next accessibility event.

### Design Gap 1 (FIXED): Dictation stop is self-defeating

"Start dictation" called `stopListening()`, halting the recognizer entirely. "Stop dictation" could never be recognized. Users were locked until service restart.

### Design Gap 2 (FIXED): "Help" shows badges, not a command list

"show voice commands" / "what can i say" / "help" all set `NumbersOverlayMode.ON`. Semantically misleading — "what can I say" implies showing available commands, not numbered badges.

### Design Gap 3 (FIXED): No feedback for voice control actions

Mute/wake/dictation commands executed silently. Users perceived "nothing happened" even when the action succeeded.

## Solution

### Fix 1: Replace `runBlocking` with `serviceScope.launch`

All 4 speech control callbacks now use `serviceScope.launch { }` (fire-and-forget). Callback returns `true` immediately; speech engine operation executes async without blocking Main.

### Fix 2: Remove numbers commands from VoiceControlHandler

- Removed 9 numbers phrases from `VoiceControlHandler.supportedActions`
- `NumbersOverlayHandler` (ACCESSIBILITY) is now the sole handler for numbers commands
- Uses `NumbersOverlayExecutor` which properly clears assignments on "numbers off"

### Fix 3: Add numbers mode change observer

`numbersOverlayModeJob` observes `OverlayStateManager.numbersOverlayMode` StateFlow. On change:
1. `dynamicCommandGenerator?.invalidateScreenHash()`
2. `refreshOverlayBadges()`

### Fix 4: Dictation mode switch (Design Gap 1)

Replaced `core?.stopListening()` with `core?.setSpeechMode(SpeechMode.DICTATION, exitCommands)`.

New `VoiceOSCore.setSpeechMode()` method:
1. Stops current recognition
2. Reconfigures engine via `updateConfiguration(config.withMode(mode))`
3. In DICTATION mode: registers only exit commands ("stop dictation", "end dictation", "command mode")
4. Restarts recognition with new configuration

On "stop dictation": `core?.setSpeechMode(SpeechMode.COMBINED_COMMAND)` restores full grammar.

The recognizer restarts during mode switch (~500ms gap). The feedback overlay provides visual confirmation during the transition.

### Fix 5: Separate "help" from "list commands" (Design Gap 2)

- **"help"** → shows numbered badges on interactive elements (unchanged behavior)
- **"what can i say"** / **"list commands"** / **"show all commands"** → new `onListCommands` callback showing available command categories
- Removed ambiguous **"show voice commands"** phrase
- Added `VoiceControlCallbacks.onListCommands` callback
- Added `ActionCoordinator.getHandlerCategories()` + `HandlerRegistry.getRegisteredCategories()`

### Fix 6: Visual feedback via overlay toast (Design Gap 3)

- Added `OverlayStateManager.feedbackMessage` StateFlow
- Added `showFeedback(message)` / `clearFeedback()` methods
- Added `FeedbackToast` composable in `CommandOverlayService` — renders at top of screen, auto-dismisses after 2 seconds with fade animation
- All VoiceControlCallbacks now call `OverlayStateManager.showFeedback()`:
  - "mute voice" → "Voice Muted"
  - "voice on" → "Voice Activated"
  - "start dictation" → "Dictation Mode"
  - "stop dictation" → "Command Mode"
  - "help" → "Showing Numbers"
  - "list commands" → "Commands: System, Media, Screen, ..."

## Files Modified

| File | Change |
|------|--------|
| `apps/avanues/.../VoiceAvanueAccessibilityService.kt` | Replace `runBlocking` → `serviceScope.launch`; add `SpeechMode` import; dictation mode switch via `setSpeechMode()`; add feedback calls; add `onListCommands` wiring |
| `apps/avanues/.../CommandOverlayService.kt` | Add `FeedbackToast` composable; update `NumbersOverlayContent` to render feedback independently |
| `Modules/VoiceOSCore/.../handlers/VoiceControlHandler.kt` | Separate help/list-commands phrases; add `onListCommands` to callbacks; remove "show voice commands" |
| `Modules/VoiceOSCore/.../overlay/OverlayStateManager.kt` | Add `feedbackMessage` StateFlow + `showFeedback()` / `clearFeedback()` |
| `Modules/VoiceOSCore/.../VoiceOSCore.kt` | Add `setSpeechMode(mode, exitCommands)` method; add `currentSpeechConfig` field |
| `Modules/VoiceOSCore/.../actions/ActionCoordinator.kt` | Add `getHandlerCategories()` method |
| `Modules/VoiceOSCore/.../handler/HandlerRegistry.kt` | Add `getRegisteredCategories()` method |

## Verification

After installing:
1. Say "mute voice" → green toast "Voice Muted" appears at top, recognition stops
2. Say "voice on" → toast "Voice Activated", recognition resumes
3. Say "start dictation" → toast "Dictation Mode", only exit commands recognized
4. Say "stop dictation" → toast "Command Mode", full grammar restored
5. Say "help" → toast "Showing Numbers", numbered badges appear
6. Say "what can i say" → toast "Commands: System, Media, Screen, ..."
7. Say "numbers on" → badges appear immediately (NumbersOverlayHandler)
8. Say "numbers off" → badges disappear, assignments cleared
