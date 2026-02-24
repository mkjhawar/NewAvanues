# VoiceOSCore Fix: ANR Main Thread Dispatcher Saturation

**Date:** 2026-02-25
**Branch:** SpeechEngineRevamp
**Severity:** Critical (P0)
**Symptom:** ANR — "Input dispatching timed out, Waited 5001ms for MotionEvent"

## Root Cause

Both `VoiceOSAccessibilityService.serviceScope` and `VoiceAvanueAccessibilityService.serviceScope` used `Dispatchers.Main`, causing ALL coroutines launched from these scopes to execute on the single UI thread.

When a screen change fires, the following cascade happens on the Main thread simultaneously:
1. **`onCommandsUpdated`** → `VoiceOSCore.updateCommands()` → `VivokaAndroidEngine.updateCommands()` → Vivoka grammar compilation (3-8 seconds blocking)
2. **Speech collector** → `collect { }` on every recognition result
3. **Cursor settings** → `collectLatest { }` on every DataStore emission
4. **Numbers overlay mode** → `collect { }` on mode changes
5. **Debounced scroll/screen change** jobs

CPU profile from ANR log: Main thread at 91% user CPU (83% user + 7.6% kernel), app total 104%.

## Fix

Changed both `serviceScope` instances from `Dispatchers.Main` to `Dispatchers.Default`.

### Files Modified

| File | Change |
|------|--------|
| `VoiceOSAccessibilityService.kt:48` | `Dispatchers.Main` → `Dispatchers.Default` |
| `VoiceOSAccessibilityService.kt:62` | Added `@Volatile` to `lastEventProcessTime` for cross-thread safety |
| `VoiceAvanueAccessibilityService.kt:93` | `Dispatchers.Main` → `Dispatchers.Default` |

### Why Default, Not IO

The service scope does mostly CPU-bound work (command matching, grammar compilation, accessibility tree traversal). `Dispatchers.Default` is sized to CPU core count, ideal for computation. IO-bound operations (DataStore, database) already use explicit `Dispatchers.IO` in their launch blocks.

### Thread Safety

- `lastEventProcessTime` — accessed from Main (accessibility callback) and Default (debounced job). Added `@Volatile`.
- `isServiceReady` — accessed from Main (onServiceConnected/onDestroy) and Default (coroutines). Added `@Volatile`.
- `lastScreenHash` — already `@Volatile`.
- `rootInActiveWindow` — `refreshOverlayBadges()` now captures on Main via `withContext(Dispatchers.Main)`.
- `refreshScreen()` — already wrapped in `withContext(Dispatchers.Main)` for `AccessibilityEvent.obtain()`.
- `onCommandExecuted` — already uses `withContext(Dispatchers.Main)` for UI callbacks.
- `CursorOverlayService.updateConfig()` — now wrapped in `withContext(Dispatchers.Main)` to protect View/LayoutParams mutation.

### Operations That Already Had Correct Dispatchers (Unaffected)

| Code Path | Dispatcher | Status |
|-----------|-----------|--------|
| `handleScreenChange` | `Dispatchers.Default` (explicit) | OK |
| `processVoiceCommand` | `Dispatchers.Default` (explicit) | OK |
| `onServiceReady` init | `Dispatchers.IO` (explicit) | OK |
| `adaptiveTimingPersistJob` | `Dispatchers.IO` (explicit) | OK |
| `webCommandCollectorJob` | `Dispatchers.Default` (explicit) | OK |
| `refreshScreen()` | `withContext(Dispatchers.Main)` | OK |

### Operations Fixed By Scope Change (Were Inheriting Main)

| Code Path | Before | After | Impact |
|-----------|--------|-------|--------|
| `speechCollectorJob` | Main (inherited) | Default | Speech collection off UI thread |
| `onCommandsUpdated` | Main (inherited) | Default | Grammar compilation off UI thread |
| `onScrollSettled` | Main (inherited) | Default | Overlay refresh off UI thread |
| `cursorSettingsJob` | Main (inherited) | Default | Settings processing off UI thread |
| `numbersOverlayModeJob` | Main (inherited) | Default | Mode observer off UI thread |
| VoiceControl callbacks | Main (inherited) | Default | setSpeechMode off UI thread |
| Debounced screen/scroll | Main (inherited) | Default | Debounce delays off UI thread |

## Review Follow-Up (Post-Commit)

Code review and security scan identified 3 HIGH issues fixed in follow-up commit:

| Issue | Fix |
|-------|-----|
| `refreshOverlayBadges()` calls `rootInActiveWindow` off-Main | Made suspend, captures root via `withContext(Dispatchers.Main)` |
| `CursorOverlayService.updateConfig()` mutates View/LayoutParams off-Main | Wrapped in `withContext(Dispatchers.Main)` |
| `isServiceReady` not volatile (race between Main callbacks + Default) | Added `@Volatile` |

### Known Pre-Existing Issues (Not Regressions)

- `handleScreenChange:336` accesses `rootInActiveWindow` inside `Dispatchers.Default` — pre-existing since the method was written (explicit dispatcher at line 334)
- Parent/child `serviceScope` shadowing — both are private, both cancelled in respective `onDestroy()` calls. Architecture debt, not functional bug.
- Debounce TOCTOU on `lastEventProcessTime` — benign race (worst case: one extra event processed)

## Verification

- `./gradlew :Modules:VoiceOSCore:compileDebugKotlinAndroid` — BUILD SUCCESSFUL
- `./gradlew :apps:avanues:compileDebugKotlin` — BUILD SUCCESSFUL

## Commits

1. `07211108b` — Primary fix: serviceScope Main → Default
2. `d1a8842df` — Review follow-up: rootInActiveWindow on Main, updateConfig on Main, @Volatile isServiceReady
