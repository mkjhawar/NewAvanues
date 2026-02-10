# VoiceOSCore - Differential Diagnosis: Voice Recognition Failures
**Date:** 2026-02-10 | **Branch:** `060226-1-consolidation-framework` vs `VoiceOSCore-CodeCompliance`

---

## Executive Summary

Two distinct failures with **different root causes**:

| # | Symptom | Severity | Root Cause |
|---|---------|----------|------------|
| 1 | Commands not registering with Vivoka on screen change | HIGH | Triple dedup guard in `updateCommands()` silently drops updates |
| 2 | Recognized commands not executing actions | CRITICAL | **Missing `speechResults` Flow collector** — no code bridges Vivoka recognition output to `processCommand()` |

---

## Problem 1: Commands Not Registering Reliably

### What Changed (CodeCompliance → Current)

#### CodeCompliance `VivokaAndroidEngine.updateCommands()`:
```kotlin
// SIMPLE — every call goes through
override suspend fun updateCommands(commands: List<String>): Result<Unit> {
    return try {
        vivokaEngine?.setDynamicCommands(commands)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

#### Current Branch — **TRIPLE DEDUP**:

**Layer 1** — `VoiceOSCore.updateCommands()` (commonMain):
```kotlin
if (newCommands == allRegisteredCommands) {
    return Result.success(Unit)  // ← Skip if set-equal (reasonable)
}
```

**Layer 2** — `VivokaAndroidEngine.updateCommands()` atomic CAS guard:
```kotlin
if (!isUpdatingCommands.compareAndSet(false, true)) {
    return Result.success(Unit)  // ← SILENTLY DROPS if concurrent call in-flight
}
```

**Layer 3** — `VivokaAndroidEngine.updateCommands()` hash guard:
```kotlin
val newHash = commands.sorted().hashCode()
if (newHash == oldHash) {
    return Result.success(Unit)  // ← Skips if hash matches (collision risk)
}
```

### Why This Breaks Command Registration

1. **Layer 2 (CAS guard) is the primary offender.** On rapid screen changes (window state change → content change → click), accessibility events fire within milliseconds. The first `updateCommands()` call sets `isUpdatingCommands=true` and calls `setDynamicCommands()`. While Vivoka's grammar compilation is in progress (can take 50-200ms), the NEXT screen change triggers another `updateCommands()` which hits the CAS guard and **silently returns success without registering the new commands**.

2. **Layer 3 (hash guard)** uses `commands.sorted().hashCode()` — while hash collisions are rare for different command sets, the `.sorted()` makes it deterministic. But the hash comparison happens AFTER the CAS guard, so it rarely triggers.

3. **Layer 1 (set equality)** in VoiceOSCore is the most correct guard — it compares actual content. But it only catches when the merged set (dynamic + app handler phrases) is unchanged.

### The Race Condition Scenario:
```
t=0ms    Screen A → handleScreenChange() → updateCommands(["email", "inbox", "compose"])
t=5ms    Vivoka: setDynamicCommands() starts grammar compilation...
t=10ms   Screen B → handleScreenChange() → updateCommands(["send", "attach", "reply"])
                   ↳ CAS guard: isUpdatingCommands=true → SKIPPED (returns success)
t=150ms  Vivoka: grammar compilation finishes for Screen A commands
         ↳ User is now on Screen B but Vivoka grammar has Screen A commands!
```

### CodeCompliance didn't have this problem:
Simple passthrough — every call reached `setDynamicCommands()`. Vivoka itself handles command replacement atomically.

---

## Problem 2: Recognized Commands Not Executing (CRITICAL)

### The Broken Chain

```
✅ Screen changes → elements extracted → commands generated
✅ Commands registered with ActionCoordinator (for execution routing)
✅ Commands sent to Vivoka via updateCommands() (for speech grammar)
✅ Vivoka recognizes speech → emits RecognitionResult
✅ VivokaAndroidEngine converts → emits SpeechResult to _results SharedFlow
✅ VoiceOSCore.speechResults exposes the Flow
❌ NOBODY COLLECTS FROM speechResults
❌ processCommand() is NEVER CALLED
❌ No action executes
```

### Where the Wire Is Missing

**`VoiceAvanueAccessibilityService.onServiceReady()`** (line 84-116):
```kotlin
serviceScope.launch(Dispatchers.IO) {
    // ... database init, preferences, etc.
    voiceOSCore = VoiceOSCore.createForAndroid(...)
    voiceOSCore?.initialize()
    // ← MISSING: No collection of voiceOSCore.speechResults
    // ← MISSING: No bridge from recognition → processCommand()
}
```

### What SHOULD exist (but doesn't):
```kotlin
// After voiceOSCore?.initialize():
serviceScope.launch {
    voiceOSCore?.speechResults?.collect { result ->
        if (result.isFinal && result.confidence >= devSettings.confidenceThreshold) {
            processVoiceCommand(result.text, result.confidence)
        }
    }
}
```

### The Unused Infrastructure

`SpeechEngineManager` (line 250-281) **HAS** result collection logic:
```kotlin
private fun startResultCollection() {
    resultCollectionJob = scope.launch {
        engine.results.collect { result ->
            if (result.isFinal && !isMuted) {
                _commands.emit(SpeechCommandEvent(...))
            }
        }
    }
}
```

But `VoiceOSCore` **bypasses** `SpeechEngineManager` entirely — it creates the engine directly via `speechEngineFactory.createEngine()` and stores it as `speechEngine`. The `SpeechEngineManager` is never instantiated.

### CodeCompliance Status

The CodeCompliance branch has the **same VoiceOSCore.kt** (zero diff). It ALSO lacks a speechResults collector in its VoiceOSAccessibilityService base class. However:

- CodeCompliance did NOT have `apps/avanues/` (app-level service) — it was used by a different app that may have had its own result collection
- The base class `processVoiceCommand()` exists at line 417 but is only callable externally

---

## Additional Differences (Secondary)

### 1. Import Path Migration
| | CodeCompliance | Current |
|---|---|---|
| VivokaEngine | `com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine` | `com.augmentalis.speechrecognition.vivoka.VivokaEngine` |
| PathResolver | `com.augmentalis.voiceos.speech.engines.vivoka.VivokaPathResolver` | `com.augmentalis.speechrecognition.vivoka.VivokaPathResolver` |
| RecognitionResult | `com.augmentalis.voiceos.speech.api.RecognitionResult` | `com.augmentalis.speechrecognition.RecognitionResult` |

**Risk:** Different module = potentially different behavior, API surface, or bugs.

### 2. Error Listener API Change
```kotlin
// CodeCompliance:
vivokaEngine?.setErrorListener { message, code -> ... }

// Current:
vivokaEngine?.setErrorListener { error -> ... }
```
The underlying VivokaEngine API changed from `(String, Int)` callback to `(SpeechError)` object callback.

### 3. Model Auto-Extraction Added
Current branch adds `initializeEnglishModels()` that auto-extracts from assets if models missing. CodeCompliance just failed with error. This is a **good** change but could mask model issues.

### 4. StaticCommandRegistry Expanded
Current branch adds ~100 new commands (text, screen, cursor, reading, input, app control). The `allPhrases()` list is now much larger. Vivoka grammar compilation takes longer with more commands, which makes the CAS guard problem worse.

### 5. CommandManager System Added (56K+ lines)
Entire `managers/commandmanager/` package is new — parallel command system with its own registry, actions, macros. Potential for confusion about which system handles what.

---

## Recommended Fixes

### Fix 1: Add Speech Result Collection (CRITICAL)

**File:** `apps/avanues/src/main/kotlin/com/augmentalis/voiceavanue/service/VoiceAvanueAccessibilityService.kt`

**Location:** After `voiceOSCore?.initialize()` (line 110)

**What:** Add `speechResults` flow collection that bridges to `processVoiceCommand()`:
```kotlin
voiceOSCore?.initialize()

// Collect speech recognition results and route to command execution
serviceScope.launch {
    voiceOSCore?.speechResults?.collect { result ->
        if (result.isFinal && result.confidence >= devSettings.confidenceThreshold) {
            Log.d(TAG, "Voice: '${result.text}' (${result.confidence})")
            processVoiceCommand(result.text, result.confidence)
        }
    }
}
```

### Fix 2: Remove Aggressive CAS Guard from VivokaAndroidEngine

**File:** `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/VivokaAndroidEngine.kt`

**What:** Remove `isUpdatingCommands` CAS guard. Keep the hash dedup (Layer 3) as a reasonable optimization, but let concurrent calls queue naturally on Vivoka's internal mutex rather than silently dropping them.

**Alternative:** Replace CAS guard with a "latest wins" pattern using a Channel with CONFLATED buffer — this ensures the LAST command set always gets registered even if intermediate ones are skipped.

### Fix 3: Keep VoiceOSCore-Level Dedup (Layer 1)
The `allRegisteredCommands` set comparison in `VoiceOSCore.updateCommands()` is correct and sufficient dedup. Combined with the hash check in VivokaAndroidEngine, it provides two-tier protection without dropping legitimate updates.

---

## Priority Order

1. **Fix 1** (speechResults collector) — Without this, no voice command will EVER execute. This is the showstopper.
2. **Fix 2** (CAS guard removal) — Without this, commands randomly fail to register on screen changes.
3. Verify the `speechrecognition` library VivokaEngine API compatibility
