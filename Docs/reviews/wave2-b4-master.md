# Wave 2 Batch 4 — Speech/Audio Layer Master Analysis
# Date: 260222 | Branch: VoiceOS-1M-SpeechEngine
# Reviewer: code-reviewer agent
# Full quality report: docs/reviews/Speech-Review-QualityAnalysis-260222-V1.md

---

## Module: SpeechRecognition
**Files reviewed:** 93 kt files across commonMain / androidMain / iosMain / desktopMain
**Status:** Functional with critical blockers — 4 Critical, 5 High, 8 Medium, 6 Low

### Architecture
- `SpeechEngine` enum + `SpeechRecognitionService` interface + `expect/actual` factory follows KMP pattern correctly.
- `SpeechConfig` validation is thorough. `ServiceState` state machine (`canStart()`, `canStop()`, etc.) is cleanly modelled.
- Platform split is correct: Android has full implementations for all 5 engines (WHISPER, GOOGLE_CLOUD, ANDROID_STT, VOSK, VIVOKA), iOS has APPLE_SPEECH + WHISPER, Desktop has WHISPER.

### Critical Issues
| File | Line | Issue |
|------|------|-------|
| `whisper/ui/WhisperModelDownloadScreen.kt` | 169 | `onDownload = { /* empty */ }` — download button is dead; no ViewModel wiring. |
| `googlecloud/GoogleCloudStreamingClient.kt` | 411-413 | `audioQueue` closed in `stopStreaming()`, never rebuilt on reconnect. After the first 4:50 min rotation all audio is silently discarded. |
| `SdkInitializationManager.kt` | 6 | Rule 7: `Author: VOS4 Development Team` — no `@author Manoj Jhawar`. |

### High Issues
| File | Line | Issue |
|------|------|-------|
| `googlecloud/GoogleCloudStreamingClient.kt` | 234 | `Thread.sleep(10)` inside OkHttp `writeTo()` callback — blocks IO thread, uncancellable. |
| `whisper/WhisperModelManager.kt` | 397-400 | `Environment.getExternalStorageDirectory()` — scoped storage violation, fails on API 30+. |
| `LearningSystem.android.kt` | 30-31 | `learnedCommands` and `confidenceHistory` unsynchronized, mutated from `Dispatchers.IO` coroutines. |
| `VoiceStateManager.kt` | 46-52 | `@Volatile` on `AtomicBoolean` references — redundant; debounce silently drops rapid state transitions. |
| `androidMain/whisper/WhisperAudio.kt` | 258-265 | `subList(0, excess).clear()` is O(n) shift on every read cycle once buffer is full (~960k floats at 60s). |

### Medium Issues
- `GoogleCloudStreamingClient.kt:95` — `H2_PRIOR_KNOWLEDGE + HTTP_1_1` protocol list contradicts h2c-only design.
- `ConfidenceScorer.kt:86` — Comment says "Vivoka uses 0-100 scale" but `VIVOKA_MAX_SCORE = 10000f`; comment is wrong.
- `AndroidSTTEngine.kt:473` — `CoroutineScope` created at destroy-time never saved or cancelled.
- `WhisperModelManager.kt:286-289` — Streams not closed in `finally`; partial-close risk on exception path.
- `WhisperModelManager.kt:398` — `getSharedModelsDir()` called from main thread via `remember {}` in download screen — disk I/O on UI thread.
- `WhisperEngine.kt:205-209` — `stopListening()` launches coroutine on potentially-cancelled scope; final audio chunk may be silently discarded.
- `WhisperVAD.kt:77` — `paddingBuffer` capacity uses `Int * Int / Int`; large `paddingMs` can overflow (use `Long`).
- `WhisperModelDownloadScreen.kt:81-82` — `remember {}` for model list / storage is stale after download; use `produceState` or ViewModel `StateFlow`.

### Low Issues
- `CommandCache.kt:74-78` — "LRU" eviction is actually FIFO (`keys.firstOrNull()` on `mutableMapOf()`). Use `LinkedHashMap(accessOrder=true)`.
- `GoogleCloudStreamingClient.kt:406` — `Math.pow` for integer backoff; use `2.0.pow()` or `shl`.
- `WhisperVAD.kt:265-271` — `computeFrameRMS` divide-by-zero when `end == offset`; add `if (end == offset) return 0f`.
- `RecognitionResult.kt:32` — Verify `currentTimeMillis()` `actual` implementations return wall-clock ms on all platforms.

### Test Coverage Gaps
- `WhisperVAD`, `ConfidenceScorer`, `LearningSystem`, `CommandCache` have zero tests despite pure Kotlin logic — add to `commonTest`.

---

## Module: VoiceIsolation
**Files reviewed:** 7 kt files (1 commonMain, 1 androidMain, 2 iosMain+desktopMain stubs, 3 shared models)
**Status:** Android solid; iOS and Desktop are actively misleading stubs — 0 Critical, 0 High, 2 Medium, 0 Low

### Architecture
- `expect class VoiceIsolation` / `expect object VoiceIsolationFactory` KMP pattern is correct.
- `VoiceIsolationConfig` has init-block validation and `@Serializable` annotation — correct.
- `FeatureAvailability` enum (`FULL`, `PARTIAL`, `NONE_AVAILABLE`) is a clean API for capability discovery.

### Android (Correct)
- `NoiseSuppressor.create()`, `AcousticEchoCanceler.create()`, `AutomaticGainControl.create()` all gated on `isAvailable()`.
- `disableAllEffects()` releases all three handles. No resource leak.
- Graceful fallback when hardware effects are unavailable: returns `false` from `initialize()` cleanly.

### iOS / Desktop — Medium Issues
| Platform | Issue |
|----------|-------|
| iOS | `initialize()` returns `true`, sets `isActive = true` in state flow, but wraps `TODO: Initialize AVAudioEngine`. Callers cannot distinguish "isolation active" from "stub passthrough". |
| Desktop | Identical issue: `initialize()` returns `true` with no processing (`TODO: Initialize WebRTC APM`). |

**Fix**: Return `false` from `initialize()` on unimplemented platforms, set `isActive = false`, and emit `FeatureAvailability.NONE_AVAILABLE`. This correctly signals the caller to skip noise suppression processing.

---

## Module: Whisper (Modules/Whisper/)
**Files reviewed:** 12 kt files (all in `examples/` — vendored whisper.cpp Android demo)
**Status:** Not production code — no issues to report; review of actual Whisper integration is under SpeechRecognition

### Note
`Modules/Whisper/` contains only example/sample code from the whisper.cpp Android demo project (`WhisperLib.kt`, `LocalAudioRecorder.kt`, `MainScreenActivity.kt`, and UI composables). None of these files are imported by the production app.

The production Whisper integration lives at:
- `Modules/SpeechRecognition/src/androidMain/.../whisper/WhisperEngine.kt` — pipeline
- `Modules/SpeechRecognition/src/androidMain/.../whisper/WhisperNative.kt` — JNI bridge
- `Modules/SpeechRecognition/src/androidMain/.../whisper/WhisperAudio.kt` — capture
- `Modules/SpeechRecognition/src/androidMain/.../whisper/WhisperModelManager.kt` — download/store
- `Modules/SpeechRecognition/src/commonMain/.../whisper/WhisperVAD.kt` — VAD
- `Modules/SpeechRecognition/src/commonMain/.../whisper/WhisperModels.kt` — types

These are reviewed and findings are recorded under SpeechRecognition above.

---

## Module: Voice / WakeWord
**Files reviewed:** 13 kt files (commonMain interfaces, androidMain service + detector + settings)
**Status:** Service architecture is correct but the underlying detector is a non-functional stub — 4 Critical, 2 High, 1 Medium, 1 Low

### Architecture
- `IWakeWordDetector` interface is clean and lifecycle-correct.
- `WakeWordService` is a well-structured foreground service with battery monitoring, screen-off pause, and proper intent broadcasting.
- `WakeWordViewModel` cleanly surfaces events and errors — correct architecture.
- `WakeWordSettingsRepository` uses DataStore correctly.

### Critical Issues
| File | Line | Issue |
|------|------|-------|
| `IWakeWordDetector.kt` | 3 | Rule 7: `// author: Claude Code` — replace with `Manoj Jhawar`. |
| `IWakeWordSettingsRepository.kt` | 3 | Rule 7: `// author: Claude Code` — replace with `Manoj Jhawar`. |
| `detector/PhonemeWakeWordDetector.kt` | 1 | Rule 7: `// author: Claude Code` — replace with `Manoj Jhawar`. |
| `detector/PhonemeWakeWordDetector.kt` | 76-121 | Stub detector: `initialize()` logs "STUB", transitions to `STOPPED`, returns `Result.Success`. `start()` transitions to `LISTENING`, returns `Result.Success`. Zero audio capture or phoneme extraction. `WakeWordService` launches on top of this, broadcasts `WAKE_WORD_DETECTED` intents that will never arrive. The entire wake-word feature is silently non-functional. Must return `Result.Error(...)` or be gated behind `BuildConfig.FEATURE_WAKE_WORD`. |

### High Issues
| File | Line | Issue |
|------|------|-------|
| `service/WakeWordService.kt` | 163-177 | `onDestroy` launches `serviceScope.launch { detector.stop(); detector.cleanup() }` then calls `serviceScope.cancel()`. The coroutine may be cancelled before cleanup runs. Use `runBlocking { }` or chain with `invokeOnCompletion`. |

### Medium Issues
- Duplicate source sets: `src/commonMain/` and `src/main/java/` both contain `IWakeWordDetector.kt` and `WakeWordModels.kt`. The `src/main/java/` copies are Android-only legacy versions. Source of truth is ambiguous.

### Low Issues
- `settings/WakeWordSettingsRepository.kt:60` — `WakeWordKeyword.valueOf()` in DataStore `map {}` throws `IllegalArgumentException` on unrecognised enum name. Use `runCatching { }.getOrDefault(HEY_AVA)`.

---

## Cross-Module Observations

### Rule 7 Violations (4 files)
All must be fixed before merge:
1. `Voice/WakeWord/src/commonMain/.../IWakeWordDetector.kt:3`
2. `Voice/WakeWord/src/commonMain/.../IWakeWordSettingsRepository.kt:3`
3. `Voice/WakeWord/src/main/java/.../detector/PhonemeWakeWordDetector.kt:1`
4. `SpeechRecognition/src/androidMain/.../SdkInitializationManager.kt:6` (`VOS4 Development Team` — no `Manoj Jhawar`)

### Platform Parity Summary
| Module | Android | iOS | Desktop |
|--------|---------|-----|---------|
| SpeechRecognition | Full (5 engines) | APPLE_SPEECH + WHISPER | WHISPER only |
| VoiceIsolation | Full | Stub (returns true) | Stub (returns true) |
| WakeWord | Stub detector (PhonemeWakeWordDetector) | N/A | N/A |

### Known-Stub Inventory Additions (new for MEMORY.md)
- `Voice/WakeWord/.../PhonemeWakeWordDetector.kt` — complete no-op stub, reports LISTENING, never detects. (CRITICAL)
- `VoiceIsolation/.../VoiceIsolation.ios.kt` — stub returning `true` from `initialize()`, claims `isActive = true`. (MEDIUM)
- `VoiceIsolation/.../VoiceIsolation.desktop.kt` — same pattern as iOS. (MEDIUM)
- `SpeechRecognition/.../whisper/ui/WhisperModelDownloadScreen.kt` — dead `onDownload` lambda. (CRITICAL functional bug)
- `SpeechRecognition/.../googlecloud/GoogleCloudStreamingClient.kt` — `audioQueue` not rebuilt after reconnect. (CRITICAL)

---

## Priority Fix Order

| Priority | Module | Fix |
|----------|--------|-----|
| 1 | Voice/WakeWord | Remove all 3 Rule 7 (`Claude Code`) attributions |
| 2 | SpeechRecognition | Remove Rule 7 from `SdkInitializationManager.kt` |
| 3 | Voice/WakeWord | `PhonemeWakeWordDetector`: return `Result.Error` or add `FEATURE_WAKE_WORD` guard |
| 4 | SpeechRecognition | Wire `WhisperModelDownloadScreen` `onDownload` to ViewModel |
| 5 | SpeechRecognition | Rebuild `audioQueue` on each streaming session in `GoogleCloudStreamingClient` |
| 6 | SpeechRecognition | Replace `Thread.sleep(10)` in `writeTo()` |
| 7 | SpeechRecognition | Replace `Environment.getExternalStorageDirectory()` with `getExternalFilesDir(null)` |
| 8 | SpeechRecognition | Synchronize `LearningSystem` collections |
| 9 | VoiceIsolation | iOS/Desktop `initialize()` must return `false`, set `isActive = false` |
| 10 | SpeechRecognition | Add `commonTest` tests for `WhisperVAD`, `ConfidenceScorer`, `LearningSystem`, `CommandCache` |
