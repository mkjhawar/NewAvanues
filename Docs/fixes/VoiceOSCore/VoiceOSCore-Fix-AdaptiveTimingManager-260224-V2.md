# VoiceOSCore Fix: AdaptiveTimingManager P2 Review Fixes

**Date:** 2026-02-24
**Version:** V2 (P2 review findings)
**Branch:** VoiceOS-1M-SpeechEngine
**Follows:** V1 (P0/P1 fixes, commit d7911c455)

## Summary

Addresses remaining P2 findings from the 3-agent swarm review (code-reviewer, security-scanner, code-quality-enforcer) of the AdaptiveTimingManager adaptive voice pipeline system.

## Findings Addressed

### P2 Fix 1: Processing Delay Floor Raised (0L -> 10L)

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../AdaptiveTimingManager.kt`

**Problem:** `PROCESSING_DELAY_MIN = 0L` allowed the delay to drop to zero, which provides no safety buffer against rapid-fire result emission from engine reconnection or partial-result bursts.

**Fix:** Changed `PROCESSING_DELAY_MIN` from `0L` to `10L`. The 10ms floor provides a minimal safety buffer while still being imperceptible to users (human reaction time is ~200ms).

**Risk if unchanged:** Rapid-fire duplicate commands during engine reconnection events.

### P2 Fix 2: Time-Based Decay for Stale Duplicate Penalty

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../AdaptiveTimingManager.kt`

**Problem:** After a burst of duplicates (e.g., from a noisy environment), the processing delay would ramp up toward 300ms but had no automatic mechanism to recover. It only decreased via successful commands (5% per success), meaning it could take 50+ clean commands to return to baseline after a single burst.

**Fix:** Added `DUPLICATE_COOLDOWN_MS = 30_000L` time-based decay:
- `recordCommandDuplicate()` now timestamps `lastDuplicateTimeMs`
- `recordCommandSuccess()` checks: if 30+ seconds since last duplicate, apply extra -15ms reduction
- Cooldown fires once per period (resets `lastDuplicateTimeMs` after applying)
- `reset()` clears `lastDuplicateTimeMs`

**Behavior:** After 30s of clean operation, the next success gets a bonus 15ms reduction on top of the normal 5% shrink. This prevents the delay from being permanently elevated after transient noise bursts.

### P2 Fix 3: Cross-Module Delay Provider Lambda

**Files:**
- `Modules/SpeechRecognition/.../vivoka/VivokaRecognizer.kt`
- `Modules/SpeechRecognition/.../vivoka/VivokaEngine.kt`
- `Modules/VoiceOSCore/.../VivokaAndroidEngine.kt`

**Problem:** VivokaRecognizer (in SpeechRecognition module) reads `processingDelayMs` for the command emission delay, but the value was set via `setProcessingDelay()` in `updateCommands()` — meaning it was only updated on screen changes, not continuously. The recognizer couldn't depend on VoiceOSCore directly (circular dependency).

**Fix:** Lambda provider pattern:
1. `VivokaRecognizer` adds `var processingDelayProvider: (() -> Long)? = null`
2. `startCommandProcessing()` reads: `val effectiveDelay = processingDelayProvider?.invoke() ?: processingDelayMs`
3. `VivokaEngine` adds `setProcessingDelayProvider(provider: () -> Long)`
4. `VivokaAndroidEngine.initialize()` wires: `vivokaEngine?.setProcessingDelayProvider { AdaptiveTimingManager.getProcessingDelayMs() }`
5. Removed manual delay push from `updateCommands()` (no longer needed)

**Behavior:** The recognizer now reads the latest adaptive delay on every command execution, not just when `updateCommands()` runs. No circular module dependency — the lambda is wired at the app-integration layer.

## Previously Fixed (V1, commit d7911c455)

| Priority | Fix | File |
|----------|-----|------|
| P0 | KDoc header — corrected method names | AdaptiveTimingManager.kt |
| P0 | AIMD terminology — "reward"/"backoff" not inverted | AdaptiveTimingManager.kt |
| P0 | Persistence wiring — load on init, 60s timer, persist on destroy | VoiceAvanueAccessibilityService.kt |
| P1 | scrollDebounceMs documented as intentionally static | AdaptiveTimingManager.kt |
| P1 | Unified confidence gate — both service layers use ATM | VoiceAvanueAccessibilityService.kt |
| P1 | Key consolidation — ATM.Keys delegates to SettingsKeys | AdaptiveTimingManager.kt |
| P1 | isDuplicate mutation-on-read fix | AdaptiveTimingManager.kt |
| P2 | FQN cleanup — added import, used getValue() | AvanuesSettingsRepository.kt |

## Files Modified (This Commit)

| File | Changes |
|------|---------|
| `AdaptiveTimingManager.kt` | PROCESSING_DELAY_MIN=10, DUPLICATE_COOLDOWN_MS, time-based decay |
| `VivokaRecognizer.kt` | processingDelayProvider lambda field |
| `VivokaEngine.kt` | setProcessingDelayProvider() method |
| `VivokaAndroidEngine.kt` | Wire provider lambda, remove manual delay push |
| `Chapter102` | Updated sections 21.3, 21.4, 21.5 for P2 changes |

## Verification

1. **Delay floor**: processingDelayMs can never drop below 10ms (was 0ms)
2. **Decay behavior**: After 30s idle from duplicates, next success gets -15ms bonus
3. **Provider wiring**: `VivokaRecognizer.startCommandProcessing()` reads live adaptive value
4. **No circular deps**: SpeechRecognition module has no import of VoiceOSCore
5. **Backward compat**: If provider is null, falls back to static `processingDelayMs` field
