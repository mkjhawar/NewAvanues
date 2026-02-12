# VoiceOSCore-Fix-VoiceControlCallbacksWiring-260211-V1

## Overview

Wire VoiceControlCallbacks in VoiceAvanueAccessibilityService (Phase B gap) and fix 5 reified-type intersection warnings in HUDContentProvider that would become compilation errors in future Kotlin releases.

**Branch**: `VoiceOSCore-KotlinUpdate`
**Commit**: `f68b4a17`

## Changes

### VoiceControlCallbacks Wiring

**File**: `apps/avanues/src/main/kotlin/com/augmentalis/voiceavanue/service/VoiceAvanueAccessibilityService.kt`

Added import for `VoiceControlCallbacks` and wired all 6 callbacks in `onServiceReady()` after VoiceOSCore initialization:

| Callback | Implementation |
|----------|---------------|
| `onMuteVoice` | `voiceOSCore.stopListening()` via `runBlocking` |
| `onWakeVoice` | `voiceOSCore.startListening()` via `runBlocking` |
| `onStartDictation` | `voiceOSCore.stopListening()` — pauses command recognition for keyboard dictation |
| `onStopDictation` | `voiceOSCore.startListening()` — resumes command recognition |
| `onShowCommands` | `OverlayStateManager.setNumbersOverlayMode(ON)` — shows numbered badges |
| `onSetNumbersMode` | Maps "on"/"off"/"auto" string to `OverlayStateManager.NumbersOverlayMode` enum |

Added `VoiceControlCallbacks.clear()` to `onDestroy()` to prevent stale references.

### HUDContentProvider Reified-Type Fix

**File**: `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/managers/hudmanager/provider/HUDContentProvider.kt`

Changed 5 `arrayOf(` calls to `arrayOf<Any>(` in:
- `queryHUDElements()` — line 276 (Int, String, Float, Boolean mix)
- `queryHUDElement()` — line 303 (Int, String, Float, Boolean mix)
- `queryHUDStatus()` — line 327 (String, Float mix)
- `queryHUDConfig()` — line 341 (String, Int, Boolean mix)
- `queryAccessibilitySettings()` — line 384 (Boolean, Float mix)

Without explicit `<Any>`, Kotlin infers `Comparable<*> & Serializable` intersection type for the reified `T` parameter. This will become a compilation error in a future Kotlin release (likely 2.2+).

## Verification

- Full app `compileDebugKotlin`: BUILD SUCCESSFUL
- Zero reified-type warnings remaining in HUDContentProvider
