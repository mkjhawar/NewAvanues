# SpeechRecognition Plan: AVX (AvaVox) Command Engine

**Date**: 2026-02-24
**Branch**: `SpeechEngineRevamp`
**Status**: Implementation in progress

## Overview

AVX (AvaVox) is a lightweight, multilingual ONNX-based command recognition engine that:
1. Reduces CPU/battery by handling most commands without waking Vivoka
2. Supports 30+ languages with per-language tuned models
3. Targets <5% Command Error Rate via hot words boosting + existing 6-stage matching
4. Uses AON encryption for all ONNX model files

## Architecture

```
Audio (16kHz) -> AVX Engine (ONNX Runtime, ~60-80MB per language)
                    |
              Raw transcription + confidence + N-best alternatives
                    |
              CommandMatchingService (existing 6-stage pipeline)
                    |
              Matched command
```

### Pre-Filter (Android only, where Vivoka is also available)

```
Audio -> AVX Engine (hot words boosted)
             |
         confidence >= 0.85? --YES--> Accept result
             | NO
         Vivoka fallback (grammar-based, higher accuracy)
```

## Phases Implemented

### Phase 1A: Whisper initial_prompt Biasing
- Added `initialPrompt: String?` to `WhisperConfig` (Android) and `DesktopWhisperConfig`
- Added `InitialPromptBuilder` in `WhisperModels.kt` (commonMain) - builds prompt from active commands
- Added `WhisperConfig.forCommandMode()` factory method
- JNI wiring pending (whisper.cpp C-side needs `initial_prompt` param exposed)

### Phase 1B: Distil-Whisper Models
- Added `DISTIL_SMALL_EN` (350MB, 1.2x speed) and `DISTIL_MEDIUM_EN` (700MB, 4.0x speed) to `WhisperModelSize`
- Added `isDistilled` property for model type detection
- Added `WhisperModelSize.forCommandMode()` that prefers Distil models for English
- Standard `forAvailableRAM()` now excludes Distil models (use `forCommandMode()` for those)

### Phase 2: AVX Engine Core
- **commonMain**: `AvxModels.kt` (15 languages, model metadata), `AvxConfig.kt` (hot words, N-best)
- **androidMain**: `AvxEngine.kt` (full lifecycle), `AvxNative.kt` (JNI wrapper), `AvxModelManager.kt` (download + AON)
- **desktopMain**: `DesktopAvxEngine.kt`, `DesktopAvxNative.kt`
- Registered `AVX` in both `SpeechEngine` enums (SpeechRecognition + VoiceOSCore)
- Registered in `SpeechEngineRegistry` with capabilities
- Added `RecognitionEngine.AVX` and `ScoringMethod.AVX_ONNX` to `ConfidenceScorer`

### Phase 3: Pre-Filter Wiring
- Created `AvxPreFilterEngine` in VoiceOSCore androidMain
- Simple if/else: AVX first, Vivoka fallback on low confidence
- `PreFilterMode` enum: DISABLED, COMMAND_ONLY, ALL_EXCEPT_DICTATION
- Metrics tracking for AVX accept rate vs Vivoka fallback rate

### Phase 4: Model Download & Encryption
- `AvxModelManager` handles full pipeline: download -> AONCodec.wrap() -> store
- Storage: `/sdcard/ava-ai-models/avx/` (Android), `~/.augmentalis/models/avx/` (Desktop)
- Naming: `Ava-AvxS-{LangCode}.aon`
- Auto-download: locale + English on first run
- Settings integration: per-language download/delete, storage usage display

## Model Naming Convention

| Example | Meaning |
|---|---|
| `Ava-AvxS-EN.aon` | AVX Small English |
| `Ava-AvxS-FR.aon` | AVX Small French |
| `Ava-AvxS-DE.aon` | AVX Small German |

## Files Created
- `Modules/SpeechRecognition/src/commonMain/.../avx/AvxModels.kt`
- `Modules/SpeechRecognition/src/commonMain/.../avx/AvxConfig.kt`
- `Modules/SpeechRecognition/src/androidMain/.../avx/AvxEngine.kt`
- `Modules/SpeechRecognition/src/androidMain/.../avx/AvxNative.kt`
- `Modules/SpeechRecognition/src/androidMain/.../avx/AvxModelManager.kt`
- `Modules/SpeechRecognition/src/desktopMain/.../avx/DesktopAvxEngine.kt`
- `Modules/SpeechRecognition/src/desktopMain/.../avx/DesktopAvxNative.kt`
- `Modules/VoiceOSCore/src/androidMain/.../AvxPreFilterEngine.kt`

## Files Modified
- `Modules/SpeechRecognition/src/commonMain/.../whisper/WhisperModels.kt` (Distil models, InitialPromptBuilder)
- `Modules/SpeechRecognition/src/commonMain/.../SpeechEngine.kt` (AVX entry)
- `Modules/SpeechRecognition/src/androidMain/.../whisper/WhisperConfig.kt` (initialPrompt, forCommandMode)
- `Modules/SpeechRecognition/src/desktopMain/.../whisper/DesktopWhisperConfig.kt` (initialPrompt, forCommandMode)
- `Modules/SpeechRecognition/src/androidMain/.../ConfidenceScorer.kt` (AVX engine + scoring)
- `Modules/SpeechRecognition/build.gradle.kts` (Crypto dependency)
- `Modules/VoiceOSCore/src/commonMain/.../speech/SpeechEngine.kt` (AVX entry + registry)

## Dependencies
- Sherpa-ONNX AAR (Android): `com.k2fsa.sherpa:onnx-android:x.y.z`
- ONNX Runtime (Desktop): via sherpa-onnx JNI bridge
- AON Codec: `Modules/Crypto/` (already implemented)

## Remaining Work
- [ ] Add sherpa-onnx AAR to Android dependencies (requires version selection)
- [ ] Build native JNI bridge for sherpa-onnx (or use pre-built binaries)
- [ ] Wire pre-filter into VoiceOSCore's SpeechEngineManager
- [ ] Add AVX settings to UnifiedSettingsScreen (language selection, pre-filter toggle)
- [ ] Update Chapter 102 with AVX section
- [ ] Whisper JNI: expose initial_prompt parameter in fullTranscribe
