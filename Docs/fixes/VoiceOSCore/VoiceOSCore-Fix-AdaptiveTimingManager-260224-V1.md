# VoiceOSCore Fix — AdaptiveTimingManager (Hardcoded Timing Values)

**Module**: `Modules/VoiceOSCore/`, `Modules/SpeechRecognition/`
**Branch**: `VoiceOS-1M-SpeechEngine`
**Date**: 2026-02-24
**Version**: V1
**Chapter**: 102 Section 21

---

## Problem

Five compounding latency/accuracy issues in the voice pipeline, all caused by static hardcoded timing values:

| Issue | Root Cause | Impact |
|-------|-----------|--------|
| 200ms hardcoded delay | `VivokaRecognizer.PROCESSING_DELAY = 200L` | Every command delayed 200ms unnecessarily |
| Double confidence gate | Engine uses 0.45f, service hardcodes 0.5f | Commands at 0.45-0.50 silently dropped |
| Scroll debounce too high | Same as content debounce (400-800ms) | Sluggish scroll overlay refresh |
| Static grammar debounce | 300ms hardcoded | Not tuned to actual device compile speed |
| No adaptation | All timing values static | No learning from real conditions |

## Root Cause

All timing values were `private const val` or hardcoded literals with no feedback mechanism. Different devices (Pixel 6 vs Samsung A53) and environments (quiet room vs noisy cafe) need different optimal values, but the pipeline offered zero adaptation.

The confidence gate was particularly insidious: the engine accepted commands at 0.45f confidence but `VoiceOSAccessibilityService.processVoiceCommand()` silently rejected anything below 0.5f — a 5% gap where valid commands were lost.

## Solution

**AdaptiveTimingManager** — a TCP-congestion-control-inspired singleton in `VoiceOSCore/commonMain` (pure Kotlin, KMP-safe).

### Architecture: AIMD (Additive Increase / Multiplicative Decrease)

```
Signals In:                    Timing Values Out:
─────────────                  ──────────────────
commandSuccess()    ──┐        getProcessingDelayMs()    → VivokaRecognizer
commandDuplicate()  ──┤        getConfidenceFloor()      → VoiceOSAccessibilityService
confidenceNearMiss()──┼──→ EMA → getScrollDebounceMs()   → DeviceCapabilityManager
grammarCompiled(ms) ──┤        getSpeechUpdateDebounceMs()→ DeviceCapabilityManager
wakeWordHit(ms)     ──┤        getCommandWindowMs()      → VivokaAndroidEngine
wakeWordTimeout()   ──┘
```

**EMA smoothing** (alpha=0.15, settles in ~15 samples) prevents oscillation. Values are clamped to safe ranges.

### Timing Values

| Value | Start | Min | Max | Adaptation |
|-------|-------|-----|-----|-----------|
| processingDelayMs | 50 | 0 | 300 | *0.95 on success, +25ms on duplicate |
| confidenceFloor | 0.45 | 0.3 | 0.7 | From DeveloperSettings (single source) |
| scrollDebounceMs | 200 | 100 | 500 | Separate from content debounce |
| speechUpdateDebounceMs | 200 | 100 | 500 | EMA of grammar compile time * 0.8 |
| commandWindowMs | 4000 | 2000 | 8000 | EMA of user response time * 1.5 |

### Thread Safety

`@Volatile` fields for visibility. Compound operations intentionally NOT synchronized — rare lost updates are acceptable for a convergent heuristic.

### Persistence

Learned values survive restarts via DataStore keys:
- `adaptive_processing_delay_ms`
- `adaptive_scroll_debounce_ms`
- `adaptive_speech_update_debounce_ms`
- `adaptive_command_window_ms`

## Files Modified

| File | Changes |
|------|---------|
| `VoiceOSCore/.../AdaptiveTimingManager.kt` | **NEW** — 362-line singleton with EMA, AIMD, persistence, snapshot |
| `Foundation/.../SettingsKeys.kt` | +4 adaptive timing DataStore key constants |
| `SpeechRecognition/.../VivokaRecognizer.kt` | `PROCESSING_DELAY` const → `var processingDelayMs` (volatile, settable by engine) |
| `VoiceOSCore/.../VoiceOSAccessibilityService.kt` | Hardcoded `0.5f` → `AdaptiveTimingManager.getConfidenceFloor()` + near-miss tracking |

## Verification

- [ ] VivokaRecognizer uses adaptive delay (not 200ms constant)
- [ ] Confidence floor reads from AdaptiveTimingManager (not hardcoded 0.5f)
- [ ] Near-misses tracked (confidence between floor-0.05 and floor)
- [ ] Persistence round-trip: `toPersistedMap()` → DataStore → `applyPersistedValues()`
- [ ] `reset()` restores all defaults
- [ ] `snapshot()` returns immutable state for dashboard
- [ ] No crashes on concurrent access from multiple coroutines
