# SpeechRecognition P2 Hardening Plan

**Module**: `Modules/SpeechRecognition/`
**Branch**: `VoiceOS-1M-SpeechEngine`
**Mode**: YOLO + ToT
**Date**: 2026-02-24

---

## Context

Sessions 1-13 completed all P0/P1 speech engine hardening (quality 10/10): NLU log migration (406 calls), PII redaction (14 sites), model download retry, memory-aware Whisper model selection, runBlocking removal, and documentation. This P2 plan covers verification plus three enhancement areas.

## Phase 0: Verification Builds

Confirmed sessions 1-13 broke nothing:
- NLU Android compile: PASSED
- NLU desktopTest: PASSED (ran commonTest on JVM)
- Chat desktop compile: FAILED (pre-existing RAG module Compose Runtime issue, unrelated to P2)
- SR desktopTest: PASSED (253 tests)

## Phase 1: VAD Silence Detection Tuning

### ToT Decision
- **Selected**: Combine profile presets (A) + expose constants (D)
- **Rejected**: Adaptive auto-tuning (diminishing returns vs EMA), Neural VAD Silero (4.7MB overkill)

### Implementation
- Created `VADProfile.kt` enum: COMMAND, CONVERSATION (default=original), DICTATION
- Exposed `thresholdAlpha` and `minThreshold` as WhisperVAD constructor params
- Added `vadProfile` field to WhisperConfig, DesktopWhisperConfig, IosWhisperConfig
- Added `effective*` getters that resolve profile -> individual params
- Updated all 4 engines (Whisper, Desktop, iOS, GoogleCloud) to use effective params
- 12 new tests in VADProfileTest

## Phase 2: Google Cloud STT Streaming Fixes

### ToT Decision
- **Selected**: 4 targeted fixes (Branch A)
- **Rejected**: Full overhaul (too risky for P2), Streaming+client VAD (defer to P3)

### Fixes
1. **singleUtterance**: Added to GoogleCloudConfig, serialized in streaming config JSON
2. **Speech events**: Parse END_OF_SINGLE_UTTERANCE, SPEECH_ACTIVITY_* events in parseStreamingResponse()
3. **Bounded channel**: Channel.UNLIMITED -> Channel(64) = 6.4s backlog
4. **Polling optimization**: Thread.sleep(10) -> LockSupport.parkNanos(1ms)

## Phase 3: Performance Dashboard

### ToT Decision
- **Selected**: commonMain metrics model + Compose card in Cockpit (Branch D)
- **Rejected**: Cockpit-only (couples SR internals), Settings section (conflates monitoring/config)

### Implementation
- Created `SpeechMetricsSnapshot.kt` data class with health status (GOOD/WARNING/CRITICAL/IDLE)
- Added `toSnapshot()` to WhisperPerformance (timestampMs as caller-provided param)
- Added `metricsSnapshot: StateFlow<SpeechMetricsSnapshot?>` to WhisperEngine, DesktopWhisperEngine, GoogleCloudEngine
- Added WhisperPerformance instance to GoogleCloudEngine (was missing)
- Created `SpeechPerformanceCard.kt` in Cockpit commonMain
- Added SpeechRecognition dependency to Cockpit build.gradle.kts
- Added `@Transient speechMetrics` to DashboardState
- Wired into DashboardLayout and CockpitViewModel

## Phase 4: Documentation

- Updated Chapter 102 with Sections 16 (VADProfile), 17 (Streaming Hardening), 18 (Dashboard)
- Created this plan document

## Verification Results

| Check | Result |
|-------|--------|
| SR desktopTest (253 tests) | PASSED |
| SR compileDebugKotlinAndroid | PASSED |
| Cockpit compileKotlinDesktop | PASSED |
| NLU compileDebugKotlinAndroid | PASSED |

## Files Created

| File | Purpose |
|------|---------|
| `commonMain/.../whisper/VADProfile.kt` | VAD preset profiles |
| `commonTest/.../whisper/VADProfileTest.kt` | Profile tests (12) |
| `commonMain/.../SpeechMetricsSnapshot.kt` | Immutable metrics snapshot |
| `commonTest/.../SpeechMetricsSnapshotTest.kt` | Snapshot tests (8) |
| `Cockpit/.../ui/SpeechPerformanceCard.kt` | Dashboard metrics card |

## Files Modified

| File | Changes |
|------|---------|
| `WhisperVAD.kt` | thresholdAlpha/minThreshold params, fromProfile() factory |
| `WhisperConfig.kt` | vadProfile + effective getters |
| `DesktopWhisperConfig.kt` | vadProfile + effective getters |
| `IosWhisperConfig.kt` | vadProfile + effective getters |
| `WhisperEngine.kt` | metricsSnapshot StateFlow, effective VAD params |
| `DesktopWhisperEngine.kt` | metricsSnapshot StateFlow, effective VAD params |
| `IosWhisperEngine.kt` | effective VAD params |
| `GoogleCloudEngine.kt` | WhisperPerformance, metricsSnapshot, singleUtterance |
| `GoogleCloudConfig.kt` | singleUtterance field |
| `GoogleCloudStreamingClient.kt` | Speech events, bounded channel, LockSupport |
| `WhisperPerformance.kt` | toSnapshot() method |
| `DashboardState.kt` | @Transient speechMetrics |
| `DashboardLayout.kt` | Speech Engine section |
| `CockpitViewModel.kt` | updateSpeechMetrics(), combine with _speechMetrics |
| `Cockpit/build.gradle.kts` | SpeechRecognition dependency |
| `Chapter 102` | Sections 16-18 |

## Commit Sequence

1. `feat(SpeechRecognition): add VADProfile presets and expose threshold tuning constants`
2. `fix(SpeechRecognition): fix 4 critical Google Cloud streaming issues`
3. `feat(SpeechRecognition): add performance metrics dashboard card with Cockpit integration`
4. `docs: update Chapter 102 for P2 speech engine hardening`
