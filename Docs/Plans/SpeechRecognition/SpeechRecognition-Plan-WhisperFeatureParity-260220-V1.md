# SpeechRecognition — Whisper Feature Parity Plan

**Module**: SpeechRecognition
**Type**: Plan
**Date**: 2026-02-20
**Version**: V1
**Branch**: `VoiceOS-1M-SpeechEngine`
**Status**: Phases 0-E COMPLETE, Phase F pending

---

## Goal

Achieve feature parity with Vivoka (the only fully-wired speech engine) for Whisper (offline, free) and Google Cloud STT (premium cloud), while fixing critical security/stability issues.

## Completed Phases

### Phase 0: Critical Fixes (10 bugs)
**Commit**: `7939e80e`

| # | Fix | Severity |
|---|---|---|
| 0.1 | Hardcoded credentials in FirebaseRemoteConfigRepository | SECURITY |
| 0.2 | Zip path traversal in FileZipManager | SECURITY |
| 0.3 | Global crash handler overrides Crashlytics | CRITICAL |
| 0.4 | VOSK confidence sigmoid inversion | HIGH |
| 0.5 | Silence detection callback fires every 100ms continuously | HIGH |
| 0.6 | FirebaseRemoteConfigRepository singleton broken double-checked locking | HIGH |
| 0.7 | DesktopSpeechRecognitionService reports LISTENING without audio | HIGH |
| 0.8 | AndroidSTTEngine.destroy() cancels scope before recognizer.destroy() | CRITICAL |
| 0.9 | setLanguage() not propagated to engine | MEDIUM |
| 0.10 | VoiceStateManager callback inside write lock (deadlock) | CRITICAL |

### Phase A: Whisper Core Engine (Android)
**Commit**: `700c4a60`

- `WhisperNative.kt`: Thread-safe JNI wrapper for whisper.cpp via WhisperLib
- `WhisperAudio.kt`: AudioRecord 16kHz/16-bit/mono with circular buffer
- `WhisperConfig.kt`: Model selection, path resolution, auto-tuning
- `WhisperEngine.kt`: Full orchestrator — init, listen, transcribe, destroy
- Wired into `AndroidSpeechRecognitionService` as WHISPER engine

### Phase B: VAD-Chunked Pseudo-Streaming
**Commit**: `700c4a60` (included with Phase A)

- `WhisperVAD.kt`: Energy-based VAD with adaptive threshold, hangover timer, padding buffer
- Speech boundary detection: onset -> accumulate -> silence -> transcribe chunk
- Configurable: silence timeout, min speech duration, max chunk duration

### KMP Extraction + Desktop Engine
**Commit**: `e76252d0`

Moved shared types from androidMain to commonMain for cross-platform use:

| File | Location | Content |
|---|---|---|
| `WhisperModels.kt` | commonMain | WhisperEngineState, WhisperModelSize, TranscriptionResult, TranscriptionSegment, VADState |
| `WhisperVAD.kt` | commonMain | Full VAD algorithm (removed android.util.Log dependency) |
| `DesktopWhisperNative.kt` | desktopMain | JNI wrapper with OS-specific library search paths |
| `DesktopWhisperAudio.kt` | desktopMain | javax.sound.sampled audio capture (daemon thread) |
| `DesktopWhisperConfig.kt` | desktopMain | Model dir: ~/.avanues/whisper/models/, up to 8 threads |
| `DesktopWhisperEngine.kt` | desktopMain | Full orchestrator mirroring Android engine |
| `DesktopSpeechRecognitionService.kt` | desktopMain | Rewritten with Whisper delegation |

### Phase C: Model Management + Download UI
**Commit**: `e7c86c6d`

- `ModelDownloadState.kt` (commonMain): Sealed class — Idle, Checking, FetchingMetadata, Downloading (progress/speed/ETA), Verifying, Completed, Failed, Cancelled
- `WhisperModelManager.kt` (Android): OkHttp downloads from HuggingFace with resume support (HTTP Range), SHA256 verification, storage management
- `DesktopWhisperModelManager.kt` (Desktop): HttpURLConnection downloads, same features
- `WhisperModelDownloadScreen.kt` (Android): AvanueTheme Compose UI with AVID voice identifiers
- Auto-download wired into both engines — model downloads transparently on first init

### Phase D: Command Detection Verification
**Status**: No new code needed — SpeechEngineManager already classifies Whisper as continuous engine and wraps with ContinuousSpeechAdapter -> CommandWordDetector.

### Phase E: Polish (Performance, Confidence, Language Detection)
**Commit**: `959d74e6`

- `WhisperPerformance.kt` (commonMain): Rolling-window metrics tracker (latency, RTF, confidence, success rate)
- JNI confidence: `getTextSegmentTokenCount()` + `getTextSegmentTokenProb()` via `whisper_full_get_token_p()`
- JNI language: `getDetectedLanguage()` via `whisper_full_lang_id()` + `whisper_lang_str()`
- Graceful fallback: `UnsatisfiedLinkError` caught, defaults to 0.85f when native methods not yet compiled
- `TranscriptionResult`: Now includes per-segment confidence and detected language
- Both engines updated: real confidence scores, performance tracking, init timing

---

## Remaining Phases

### Phase F: Google Cloud STT v2
**Priority**: P4 (lowest)
**Status**: Not started

- Streaming recognition via gRPC `StreamingRecognizeRequest`
- Phrase hints from dynamic commands
- Word-level timestamps
- Speaker diarization
- API key management via ICredentialStore

---

## File Inventory

### commonMain (shared)
| File | Lines | Purpose |
|---|---|---|
| `whisper/WhisperModels.kt` | 100 | Shared enums, data classes |
| `whisper/WhisperVAD.kt` | ~200 | Voice Activity Detection |
| `whisper/ModelDownloadState.kt` | 96 | Download state machine |
| `whisper/WhisperPerformance.kt` | 185 | Performance metrics tracker |

### androidMain
| File | Lines | Purpose |
|---|---|---|
| `whisper/WhisperEngine.kt` | ~520 | Main engine orchestrator |
| `whisper/WhisperNative.kt` | ~200 | Thread-safe JNI wrapper |
| `whisper/WhisperAudio.kt` | ~200 | AudioRecord capture |
| `whisper/WhisperConfig.kt` | ~140 | Android configuration |
| `whisper/WhisperModelManager.kt` | ~365 | OkHttp model downloads |
| `whisper/ui/WhisperModelDownloadScreen.kt` | ~455 | Compose download UI |
| `WhisperLib.kt` | 70 | Raw JNI declarations |

### desktopMain
| File | Lines | Purpose |
|---|---|---|
| `whisper/DesktopWhisperEngine.kt` | ~480 | Desktop engine orchestrator |
| `whisper/DesktopWhisperNative.kt` | ~240 | Desktop JNI wrapper |
| `whisper/DesktopWhisperAudio.kt` | ~200 | javax.sound capture |
| `whisper/DesktopWhisperConfig.kt` | ~130 | Desktop configuration |
| `whisper/DesktopWhisperModelManager.kt` | ~280 | HttpURLConnection downloads |

### Native C/C++
| File | Lines | Purpose |
|---|---|---|
| `jni/whisper/jni.c` | ~300 | WhisperLib JNI (primary) |
| `whisper_jni.cpp` | ~265 | Legacy JNI (secondary) |

---

## Architecture

```
SpeechRecognitionService (interface)
    |
    +-- AndroidSpeechRecognitionService
    |       |
    |       +-- SpeechEngine.WHISPER  --> WhisperEngine
    |       |       |-- WhisperAudio (AudioRecord capture)
    |       |       |-- WhisperVAD (commonMain, speech chunking)
    |       |       |-- WhisperNative -> WhisperLib (JNI -> whisper.cpp)
    |       |       |-- WhisperModelManager (OkHttp download + verify)
    |       |       +-- WhisperPerformance (commonMain, metrics)
    |       |
    |       +-- SpeechEngine.VIVOKA   --> VivokaEngine (existing)
    |       +-- SpeechEngine.ANDROID  --> AndroidSTTEngine (existing)
    |
    +-- DesktopSpeechRecognitionService
            |
            +-- DesktopWhisperEngine
                    |-- DesktopWhisperAudio (javax.sound capture)
                    |-- WhisperVAD (commonMain, shared)
                    |-- DesktopWhisperNative (JNI, OS-specific paths)
                    |-- DesktopWhisperModelManager (HttpURLConnection)
                    +-- WhisperPerformance (commonMain, shared)
```

## Verification

1. **Phase 0**: All 10 fixes committed, security credentials removed, deadlock resolved
2. **Phase A+B**: WhisperEngine initializes -> starts listening -> VAD chunks speech -> transcribes via JNI -> emits RecognitionResult
3. **Phase C**: Model auto-downloads from HuggingFace if missing, SHA256 verified, resume support
4. **Phase D**: CommandWordDetector fuzzy matches commands from Whisper output (existing infrastructure)
5. **Phase E**: Confidence from token probabilities (with fallback), language detection, performance tracking with rolling averages
