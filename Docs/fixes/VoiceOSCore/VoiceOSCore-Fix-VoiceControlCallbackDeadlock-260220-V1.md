# VoiceOSCore-Fix-VoiceControlCallbackDeadlock-260220-V1

## Problem

24 VoiceControl static voice commands are "identified by Handlers" but perform no visible action on-device:
- Mute/Wake: "mute voice", "stop listening", "voice off", "wake up voice", "start listening", "voice on"
- Dictation: "start dictation", "dictation", "type mode", "stop dictation", "end dictation", "command mode"
- Help: "show voice commands", "what can i say", "help"
- Numbers: "numbers on/off/auto", "show/hide numbers", etc.

## Root Cause Analysis (3 bugs + 2 design gaps)

### Bug 1 (CRITICAL): `runBlocking` deadlock on Main thread

**Location**: `VoiceAvanueAccessibilityService.kt` lines 270-311

All 4 speech control callbacks (`onMuteVoice`, `onWakeVoice`, `onStartDictation`, `onStopDictation`) used `runBlocking { core?.stopListening() }` or `runBlocking { core?.startListening() }`.

These callbacks are invoked from `VoiceControlHandler.execute()`, which is called from `ActionCoordinator.processCommand()`, which is called from the `speechCollectorJob` coroutine running on `serviceScope` (Dispatchers.Main, line 88).

`runBlocking` blocks the Main thread. If the speech engine's `stopListening()`/`startListening()` needs to dispatch to Main (Android's `SpeechRecognizer` must be called from its creation thread, which is Main), the dispatch goes to the Android Handler queue — but Main is blocked by `runBlocking` → **DEADLOCK**.

The `withTimeoutOrNull(5000)` wrapper in `processCommand()` cannot cancel `runBlocking` because `runBlocking` doesn't cooperatively suspend. The entire speech collector coroutine hangs permanently. No further voice commands are processed.

### Bug 2 (MEDIUM): Duplicate handler registration for numbers commands

**Location**: `VoiceControlHandler.kt` + `NumbersOverlayHandler.kt`

Two handlers registered for the same "numbers on/off/auto" phrases:
- `VoiceControlHandler` (category: SYSTEM, priority rank 1)
- `NumbersOverlayHandler` (category: ACCESSIBILITY, priority rank 10)

Since SYSTEM > ACCESSIBILITY in `ActionCategory.PRIORITY_ORDER`, VoiceControlHandler always won the handler scan. Its callback path only called `OverlayStateManager.setNumbersOverlayMode()` but did NOT:
- Clear number assignments on "numbers off" (NumbersOverlayHandler does)
- Invalidate screen hash or trigger re-scan

This caused "numbers off" to leave stale assignments and "numbers on" to show no badges (no re-scan triggered).

### Bug 3 (MEDIUM): No re-scan after numbers mode change

**Location**: `VoiceAvanueAccessibilityService.kt`

The 260219 fix documented in `VoiceOSCore-Fix-OverlayModeBypassAndRescan-260219-V1.md` was supposed to add screen hash invalidation + `refreshOverlayBadges()` after mode changes. The actual callback code never implemented this. When mode changed OFF→ON:
1. `showNumbersOverlayComputed` became `true`
2. But `numberedOverlayItems` was empty (cleared when OFF)
3. Overlay rendered nothing — user perceived "command didn't work"
4. Badges only appeared after the next accessibility event

### Design Gap 1: Dictation stop is self-defeating

"Start dictation" calls `stopListening()`, halting the speech recognizer. Subsequently "stop dictation" can never be recognized because no voice input is being processed. Users are locked in dictation mode until the service restarts or the mic toggle button is pressed.

### Design Gap 2: "Help" shows numbered badges, not a command list

"Show voice commands" / "what can i say" / "help" all set `NumbersOverlayMode.ON`. This shows numbered badges on interactive elements, NOT a list of available voice commands. Semantically misleading but functionally harmless.

## Solution

### Fix 1: Replace `runBlocking` with `serviceScope.launch`

All 4 speech control callbacks now use fire-and-forget `serviceScope.launch { core?.xxx() }` instead of `runBlocking`. The callback returns `true` immediately; the actual speech engine operation executes asynchronously on the service scope without blocking Main.

### Fix 2: Remove numbers commands from VoiceControlHandler

- Removed 9 numbers phrases from `VoiceControlHandler.supportedActions`
- Removed `invokeNumbersMode()` method
- Removed `VoiceControlCallbacks.onSetNumbersMode` field and its wiring in the service
- `NumbersOverlayHandler` (ACCESSIBILITY) is now the sole handler for numbers commands
- NumbersOverlayHandler correctly uses `NumbersOverlayExecutor` which clears assignments on "numbers off"

### Fix 3: Add numbers mode change observer

Added `numbersOverlayModeJob` in the service that observes `OverlayStateManager.numbersOverlayMode` StateFlow. When mode changes:
1. `dynamicCommandGenerator?.invalidateScreenHash()` — forces next `processScreen()` to run
2. `refreshOverlayBadges()` — triggers immediate element scan and badge generation

This ensures badges appear/disappear immediately after ANY path that changes the mode (NumbersOverlayHandler, VoiceControlCallbacks.onShowCommands, UI settings, etc.).

## Files Modified

| File | Change |
|------|--------|
| `apps/avanues/.../VoiceAvanueAccessibilityService.kt` | Replace `runBlocking` → `serviceScope.launch` in 4 callbacks; remove `onSetNumbersMode` wiring; add `numbersOverlayModeJob` observer; cancel in onDestroy |
| `Modules/VoiceOSCore/.../handlers/VoiceControlHandler.kt` | Remove numbers phrases from supportedActions; remove `invokeNumbersMode()`; remove `onSetNumbersMode` from VoiceControlCallbacks |

## Remaining Design Gaps (Not Fixed)

1. **Dictation paradox**: "start dictation" stops the recognizer → "stop dictation" can never fire. Needs either: (a) a minimal wake-word grammar that stays active during dictation, or (b) a physical button to resume. Documented with NOTE comment in the callback.

2. **"Help" semantics**: "show voice commands" shows numbered badges, not a command list. A proper `VoiceCommandsPanel` composable would be needed to show actual command names.

3. **No TTS/toast feedback**: Mute/wake/dictation commands execute silently. Adding TTS or toast confirmation ("Voice muted", "Voice on") would improve UX.

## Verification

After installing:
1. Say "numbers on" → numbered badges should appear immediately on interactive elements
2. Say "numbers off" → badges should disappear immediately
3. Say "numbers auto" → badges show only for lists
4. Say "mute voice" → speech recognition stops (no further voice commands recognized — expected)
5. Say "show voice commands" → numbered badges appear (same as "numbers on")
