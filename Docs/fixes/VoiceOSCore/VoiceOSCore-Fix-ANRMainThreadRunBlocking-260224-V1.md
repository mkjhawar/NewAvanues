# VoiceOSCore Fix: ANR — Main Thread runBlocking Mutex Contention

**Date**: 2026-02-24
**Branch**: `SpeechEngineRevamp`
**Severity**: CRITICAL (P0)
**Status**: Fixed

## Symptoms

- ANR in `com.augmentalis.avanues.debug` (MainActivity)
- Input dispatching timed out: 5001ms for MotionEvent
- Main thread at 91% CPU (83% user + 7.6% kernel)
- 146,745 minor page faults, 532 major page faults
- Speech engine threads active (ASR5_Worker1, AudioRecorder)

## Root Cause

**`CommandRegistry.clearBySource()` uses `runBlocking { withTimeout(5000L) { mutex.withLock { ... } } }` — called from Main thread via `serviceScope` (Dispatchers.Main).**

### Kill Chain

```
1. User touches screen → MotionEvent queued on Main thread
2. Background: handleScreenChange() → updateBySourceSuspend() → ACQUIRES mutex
3. Main thread: webCommandCollectorJob → clearDynamicCommandsBySource("web")
   → commandRegistry.clearBySource() → runBlocking { mutex.withLock { ... } }
   → BLOCKS waiting for mutex (held by background grammar compilation)
4. Main thread blocked 5001ms → ANR
```

### Why the Timeout Matches

`CommandRegistry.clearBySource()` uses `withTimeout(5000L)` — exactly matching the 5001ms ANR timeout. The background coroutine holds the mutex during grammar compilation, and `runBlocking` on Main thread waits the full timeout before giving up.

## Fix Applied

### Fix 1: ActionCoordinator — Use Suspend Variants (CRITICAL)

**File**: `Modules/VoiceOSCore/src/commonMain/.../actions/ActionCoordinator.kt`

Changed three methods from calling deprecated `runBlocking` CommandRegistry methods to suspend variants:

| Method | Before | After |
|---|---|---|
| `clearDynamicCommandsBySource(source)` | `fun` → `commandRegistry.clearBySource()` | `suspend fun` → `commandRegistry.clearBySourceSuspend()` |
| `clearDynamicCommands()` | `fun` → `commandRegistry.clear()` | `suspend fun` → `commandRegistry.clearSuspend()` |
| `dispose()` | `commandRegistry.clear()` | `commandRegistry.clearSuspend()` |

This eliminates the `runBlocking` on Main thread. The suspend variants cooperatively suspend instead of blocking, allowing the Main thread to process input events.

### Fix 2: Move Web Command Collector Off Main (HIGH)

**File**: `apps/avanues/.../service/VoiceAvanueAccessibilityService.kt`

Changed `webCommandCollectorJob` from `serviceScope.launch` (Dispatchers.Main) to `serviceScope.launch(Dispatchers.Default)`. The web command collector doesn't need Main thread — all operations are suspend functions or non-UI.

### Fix 3: Move Locale/Wake-Word Updates Off Main (MEDIUM)

**File**: `apps/avanues/.../service/VoiceAvanueAccessibilityService.kt`

Wrapped `CommandManager.switchLocale()` and `updateWakeWordSettings()` in `withContext(Dispatchers.Default)` inside the `cursorSettingsJob` collector. These are suspend functions that can involve grammar compilation and shouldn't run on Main.

## Files Modified

- `Modules/VoiceOSCore/src/commonMain/.../actions/ActionCoordinator.kt` — suspend variants for clear/dispose
- `apps/avanues/.../service/VoiceAvanueAccessibilityService.kt` — move collectors off Main, add `withContext` import

## Not Fixed (Mitigated)

- `VivokaEngine.destroy()` uses `runBlocking` 3x — already called from `Dispatchers.IO` in `onDestroy()` with 3s timeout. Not ideal but doesn't cause ANR.
- `AccessibilityNodeInfo` tree traversal in `processScreen()` runs on Main — Android framework requirement, can't move off Main. Contributing factor to memory pressure but not the 5s blocker.

## Verification

- [ ] Build: `./gradlew :apps:avanues:assembleDebug` — PASS
- [ ] No `runBlocking` called from Main thread in ActionCoordinator
- [ ] Web command collector runs on Dispatchers.Default
- [ ] Locale/wake-word updates run on Dispatchers.Default
- [ ] ANR no longer reproducible under mutex contention
