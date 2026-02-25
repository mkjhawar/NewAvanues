# Session Handover - NewAvanues-Handover-260225-1

## Current State
- **Repo:** NewAvanues
- **Branch:** `SpeechEngineRevamp` (synced to origin)
- **Mode:** YOLO (.yolo from /i.fix and /i.plan sessions)
- **Working Directory:** `/Volumes/M-Drive/Coding/NewAvanues`
- **Working Tree:** Clean (0 uncommitted files)

## Task In Progress
AVX (AvaVox) Sherpa-ONNX Command Engine — Phase 2 of Speech Recognition Pipeline Optimization Plan.

## Completed This Session

### 1. Branch Sync (all `*-1M*` branches)
- Merged `SpeechEngineRevamp` → `VoiceOS-1M-SpeechEngine` (fast-forward)
- Merged `VoiceOS-1M-SpeechEngine` → `VoiceOS-1M` (merge commit)
- Pushed all branches to both remotes (origin=GitLab, github=GitHub)
- All 3 branches now contain identical content (VoiceOS-1M has merge commits)

### 2. AVX Sherpa-ONNX Engine Rewrite (commit `158d4d96c`)
Rewrote the entire AVX engine stack to use the official Sherpa-ONNX Kotlin API:

| File | Change |
|------|--------|
| `AvxNative.kt` (Android) | Custom JNI → OnlineRecognizer API, streaming pipeline |
| `DesktopAvxNative.kt` | Custom JNI → OnlineRecognizer API, Desktop lib loading |
| `AvxEngine.kt` (Android) | VAD-chunked → streaming acceptWaveform→decode→endpoint |
| `DesktopAvxEngine.kt` | VAD-chunked → streaming, AON decrypt + unzip extraction |
| `AvxModels.kt` (commonMain) | Real HuggingFace data, tiers (FULL/BILINGUAL/PLANNED), shared types |
| `AvxConfig.kt` (commonMain) | Sherpa-ONNX params: decodingMethod, maxActivePaths, blankPenalty |
| `AvxModelManager.kt` | Multi-file download → zip → AON encrypt pipeline |
| `build.gradle.kts` | Sherpa-ONNX AAR (Android) + classes JAR (Desktop) deps |
| `sherpa-onnx/README.md` | Setup instructions for both platforms |

### 3. Integration Fixes (commit `cae047c0c`)
- `AvxEngine.decryptAndExtractModel` → `suspend` for coroutine I/O
- `AvxModelManager` → AONFormat.wrap() not yet available, stores raw zip (TODO: Crypto Phase 5)
- `AvxPreFilterEngine` → SpeechMode import fix (moved to voiceoscore.speech)
- `PlatformActual` → SpeechEngine.AVX added to AndroidSpeechEngineFactory exhaustive when-branches
- `KmpSpeechEngineAdapter` → AVX enum mapping added

### 4. Chapter 102 Documentation (commit `c769955f3`)
- Section 20.4: Desktop files updated for OnlineRecognizer
- Section 20.6: Multi-file archive pipeline (download+encrypt+extract)
- Section 20.8: Completed/remaining items updated

## Next Steps (CONTINUE THESE)

### Immediate (AVX Engine remaining)
1. **Download Sherpa-ONNX AAR** v1.12.25 from GitHub releases, place in `sherpa-onnx/sherpa-onnx.aar`
2. **Extract Desktop classes JAR**: `unzip sherpa-onnx.aar classes.jar` → rename to `sherpa-onnx-classes.jar`
3. **Download Desktop native libs** (.dylib for macOS) from Sherpa-ONNX release
4. **Compile test**: `./gradlew :Modules:SpeechRecognition:compileDebugKotlinAndroid`
5. **Wire pre-filter into SpeechEngineManager** — AvxPreFilterEngine needs to be activated in VoiceOSCore speech pipeline
6. **AVX settings in UnifiedSettingsScreen** — language selector, pre-filter toggle, model download UI

### Near-term
7. **Whisper JNI**: expose `initial_prompt` parameter in WhisperNative.fullTranscribe()
8. **iOS AVX engine** implementation
9. **End-to-end test**: model download → encrypt → load → transcribe → command match
10. **Memory profiling**: verify AVX + Vivoka both loaded stays under device budget

### Branch sync note
After any new commits on SpeechEngineRevamp, sync back to `VoiceOS-1M-SpeechEngine` and `VoiceOS-1M`:
```bash
git checkout VoiceOS-1M-SpeechEngine && git merge SpeechEngineRevamp --no-edit
git push origin VoiceOS-1M-SpeechEngine && git push github VoiceOS-1M-SpeechEngine
git checkout VoiceOS-1M && git merge VoiceOS-1M-SpeechEngine --no-edit
git push origin VoiceOS-1M && git push github VoiceOS-1M
git checkout SpeechEngineRevamp
```

## Files Modified (this session)
| File | Changes |
|------|---------|
| `Modules/SpeechRecognition/src/commonMain/.../avx/AvxModels.kt` | +207 lines: real model data, tiers, shared types |
| `Modules/SpeechRecognition/src/commonMain/.../avx/AvxConfig.kt` | +77 lines: Sherpa-ONNX params |
| `Modules/SpeechRecognition/src/androidMain/.../avx/AvxNative.kt` | Full rewrite: OnlineRecognizer API |
| `Modules/SpeechRecognition/src/androidMain/.../avx/AvxEngine.kt` | Full rewrite: streaming recognition |
| `Modules/SpeechRecognition/src/androidMain/.../avx/AvxModelManager.kt` | Multi-file download pipeline |
| `Modules/SpeechRecognition/src/desktopMain/.../avx/DesktopAvxNative.kt` | Full rewrite: OnlineRecognizer API |
| `Modules/SpeechRecognition/src/desktopMain/.../avx/DesktopAvxEngine.kt` | Full rewrite: streaming recognition |
| `Modules/SpeechRecognition/build.gradle.kts` | Sherpa-ONNX deps (Android+Desktop) |
| `gradle/libs.versions.toml` | sherpa-onnx = "1.12.25" |
| `sherpa-onnx/README.md` | New: setup instructions |
| `Modules/VoiceOSCore/src/androidMain/.../PlatformActual.kt` | AVX enum in factory |
| `Modules/VoiceOSCore/src/androidMain/.../AvxPreFilterEngine.kt` | Import fix |
| `Modules/VoiceOSCore/src/commonMain/.../speech/KmpSpeechEngineAdapter.kt` | AVX mapping |
| `Docs/MasterDocs/SpeechRecognition/Developer-Manual-Chapter102-WhisperSpeechEngine.md` | Section 20 updates |

## Uncommitted Changes
None — working tree is clean.

## Context for Continuation

### Key architectural decisions
- **Streaming, not VAD-chunked**: AVX uses Sherpa-ONNX's built-in endpoint detection (silence after speech = end of utterance), not external VAD chunking. This gives partial results during speech.
- **Hot words = instant**: Unlike Vivoka grammar (3-8s compile), hot words update instantly via `createStream(hotWordsStr)`.
- **AON encryption deferred**: `AONFormat.wrap()` not yet available in Crypto module. AvxModelManager stores raw zip for now. `AONCodec.unwrap()` handles both raw and AON data, so the read path is forward-compatible.
- **Language tiers**: Only 5 languages have transducer models (EN, ZH, KO, FR, ZH+EN). 11 languages are PLANNED (fall back to Whisper).

### Plan reference
- Full plan: `docs/plans/SpeechRecognition/SpeechRecognition-Plan-AvxCommandEngine-260224-V1.md`
- Also exists as saved plan: `/Users/manoj_mbpm14/.claude/plans/noble-sauteeing-aho.md`
- Chapter 102 Section 20: `Docs/MasterDocs/SpeechRecognition/Developer-Manual-Chapter102-WhisperSpeechEngine.md`

### Previous session context
- ANR crash fix completed (runBlocking mutex contention on Main thread)
- AdaptiveTimingManager implemented (TCP-inspired AIMD timing adaptation)
- Wake word detection implemented (Vivoka grammar-based, IWakeWordCapable interface)

## Quick Resume
Read docs/handover/NewAvanues-Handover-260225-1.md and continue where we left off
