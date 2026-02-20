# Speech Recognition Feature Parity — Progress Report

**Module**: `Modules/SpeechRecognition/`
**Branch**: `VoiceOS-1M-SpeechEngine`
**Date**: 2026-02-20
**Based on**: Deep review plan (Phases 0-F)

---

## Completed Phases

### Phase 0: Critical Fixes (10 bugs)
**Commit**: `7939e80e`
- 0.1 Hardcoded Firebase credentials → injected from local.properties/CI env
- 0.2 Zip path traversal → canonical path validation
- 0.3 Global crash handler → removed UncaughtExceptionHandler override
- 0.4 VOSK confidence sigmoid inversion → fixed formula
- 0.5 Silence detection callback → throttled to state-change-only
- 0.6 Firebase singleton DCL → @Volatile + synchronized
- 0.7 Desktop LISTENING without audio → state reflects actual audio capture
- 0.8 AndroidSTT destroy order → recognizer.destroy() before scope.cancel()
- 0.9 setLanguage() propagation → delegates to active engine
- 0.10 VoiceStateManager callback deadlock → copy-on-read outside lock

### Phase A: Whisper Core Engine (Android)
**Commits**: `700c4a60`, `83bfdc50`
- WhisperEngine.kt (538 lines) — full orchestrator with lifecycle state machine
- WhisperNative.kt — thread-safe JNI wrapper (synchronized)
- WhisperAudio.kt — AudioRecord 16kHz capture + Float32 conversion
- WhisperConfig.kt — auto-tuning from device RAM/CPU
- Wired into AndroidSpeechRecognitionService engine switch

### Phase B: VAD-Chunked Pseudo-Streaming
**Commit**: `700c4a60` (bundled with Phase A)
- WhisperVAD.kt (commonMain) — energy-based VAD with adaptive threshold
- Hangover timer, padding buffer, min/max speech duration
- Shared across Android, iOS, Desktop

### Phase C: Model Management + Download UI
**Commit**: `e7c86c6d`
- WhisperModelManager.kt (Android) — OkHttp download, resume, SHA256
- DesktopWhisperModelManager.kt — HttpURLConnection equivalent
- WhisperModelDownloadScreen.kt (Compose) — AvanueTheme + AVID
- ModelDownloadState.kt (commonMain) — sealed class state machine

### Phase D: Command Detection
**Status**: Verified working — ContinuousSpeechAdapter + CommandWordDetector already wire correctly for Whisper. No code changes needed.

### Phase E: Polish
**Commit**: `959d74e6`
- WhisperPerformance.kt (commonMain) — rolling-window metrics
- Token-level confidence via JNI with graceful fallback
- Language detection exposure from whisper.cpp

### KMP Extraction (Desktop)
**Commit**: `e76252d0`
- DesktopWhisperEngine.kt — mirrors Android with javax.sound.sampled audio
- DesktopWhisperNative.kt — JNI wrapper for desktop
- DesktopWhisperAudio.kt — TargetDataLine capture pipeline
- DesktopWhisperConfig.kt — higher thread/model limits for desktop

### iOS Whisper Engine (cinterop)
**Commit**: `71d49659`
- whisper_bridge.h/c — C bridge wrapping whisper.cpp for K/N cinterop
- whisper.def — cinterop definition for Kotlin/Native bindings
- IosWhisperNative.kt — atomicfu-synchronized cinterop wrapper
- IosWhisperAudio.kt — AVAudioEngine with 16kHz downsampling
- IosWhisperConfig.kt — auto-tune from ProcessInfo.physicalMemory
- IosWhisperModelManager.kt — NSURLSession downloads with resume
- IosWhisperEngine.kt — full orchestrator matching Android pattern
- IosSpeechRecognitionService.kt — dual-engine (Apple Speech + Whisper)

### macOS Apple Speech Engine
**Commit**: `93f26741`
- MacosSpeechRecognitionService.kt (~350 lines) — SFSpeechRecognizer
- PlatformUtils.macos.kt — NSLog + atomicfu synchronized collections
- SpeechRecognitionServiceFactory.macos.kt — actual fun factory
- build.gradle.kts — macosX64 + macosArm64 targets, macosMain source set
- Key diff from iOS: no AVAudioSession configuration needed on macOS

### VoiceControl Design Gap Fixes
**Commits**: `cc77462a`, `f6108c3f`
- Dictation mode toggle command handler
- Help overlay semantics
- VoiceControl feedback improvements
- Numbers handler deduplication

---

## Remaining Phases

### Phase F: Google Cloud STT v2 (P4)
- gRPC streaming recognition
- Phrase hints from dynamic commands
- API key management via ICredentialStore
- Settings UI provider

### Other Pending
- VOSK engine (Android + Desktop fallback) — P4
- Scaffold Vivoka for Desktop — P4
- Glass Input Spec review — P5

---

## Commit History (VoiceOS-1M-SpeechEngine, 14 commits)

| Hash | Description |
|---|---|
| `93f26741` | macOS Apple Speech engine via native targets |
| `71d49659` | iOS Whisper engine via cinterop + dual-engine service |
| `20f38a6c` | Whisper feature parity plan + Developer Manual Chapter 102 |
| `3d2115c3` | Fix report V2 + Developer Manual chapters for VoiceControl |
| `f6108c3f` | 3 VoiceControl design gaps (dictation, help, feedback) |
| `959d74e6` | Performance tracking, confidence scoring, language detection |
| `e7c86c6d` | Whisper model management + download UI |
| `f6ccf6b6` | Glass Input spec |
| `79a925d4` | Fix report for VoiceControl callback deadlock |
| `cc77462a` | VoiceControl callback deadlock + numbers handler dedup |
| `e76252d0` | KMP extraction + Desktop Whisper engine |
| `83bfdc50` | 3 compile errors + AI attribution removal |
| `700c4a60` | Whisper offline engine with VAD chunking |
| `7939e80e` | 10 critical fixes (security, stability, deadlock) |

All commits comply with Rule 7 (no AI attribution).
