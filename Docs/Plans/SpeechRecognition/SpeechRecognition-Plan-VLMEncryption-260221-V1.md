# SpeechRecognition - VLM Encryption Implementation Report

**Module**: SpeechRecognition
**Type**: Plan (Completed)
**Date**: 2026-02-21
**Version**: V1
**Branch**: VoiceOS-1M-SpeechEngine
**Commits**: `1bfcad30`, `2f7e0846`, `03fe1e53`
**Status**: COMPLETE (all 3 platforms)

---

## Summary

Implemented AES-256-CTR + XOR scramble + Fisher-Yates byte shuffle encryption for Whisper model files across all platforms (Android, iOS, Desktop). On-device files use VoiceOS-branded `.vlm` filenames with zero whisper/ggml traces.

## Architecture

### Crypto Stack
- **AES-256-CTR** encryption with per-block nonce
- **XOR scramble** using SHA-512 derived pattern
- **Fisher-Yates byte shuffle** with seeded PRNG
- **PBKDF2-HMAC-SHA256** key derivation (10,000 iterations)
- **64 KB block size** for chunked processing

### Source Set Layout
```
commonMain/
  whisper/vsm/VSMFormat.kt        # Format constants, header, byte helpers
  whisper/WhisperModels.kt         # vsmName property on WhisperModelSize enum

jvmMain/ (shared by Android + Desktop)
  whisper/vsm/VSMCodec.kt          # javax.crypto encryption/decryption (601 lines)

iosMain/
  whisper/vsm/IosVSMCodec.kt       # CommonCrypto encryption/decryption (538 lines)
  nativeInterop/cinterop/commoncrypto.def  # cinterop definition

androidMain/
  whisper/WhisperConfig.kt         # Updated shared VLM paths
  whisper/WhisperEngine.kt         # Decrypt-before-load flow
  whisper/WhisperModelManager.kt   # Download→encrypt, migration

desktopMain/
  whisper/DesktopWhisperConfig.kt  # Updated shared VLM paths
  whisper/DesktopWhisperEngine.kt  # Decrypt-before-load flow
  whisper/DesktopWhisperModelManager.kt  # Download→encrypt, migration

iosMain/
  whisper/IosWhisperConfig.kt      # Updated shared VLM paths
  whisper/IosWhisperEngine.kt      # Decrypt-before-load flow
  whisper/IosWhisperModelManager.kt  # Download→encrypt, migration
```

### On-Device File Naming

| Model | VLM Filename |
|-------|-------------|
| Tiny Multilingual | `VoiceOS-Tin-MUL.vlm` |
| Tiny English | `VoiceOS-Tin-EN.vlm` |
| Base Multilingual | `VoiceOS-Bas-MUL.vlm` |
| Base English | `VoiceOS-Bas-EN.vlm` |
| Small Multilingual | `VoiceOS-Sml-MUL.vlm` |
| Small English | `VoiceOS-Sml-EN.vlm` |
| Medium Multilingual | `VoiceOS-Med-MUL.vlm` |
| Medium English | `VoiceOS-Med-EN.vlm` |

### Shared Storage Paths

| Platform | Path |
|----------|------|
| Android | `/sdcard/ava-ai-models/vlm/` |
| Desktop | `~/.augmentalis/models/vlm/` |
| iOS | `{Documents}/ava-ai-models/vlm/` |

### Data Flow

```
DOWNLOAD: HuggingFace (.bin) → temp → VSMCodec.encryptFile() → .vlm → delete temp
LOAD:     .vlm → VSMCodec.decryptToTempFile() → temp (vlm_tmp/) → initContext() → delete temp
MIGRATE:  legacy .bin → encryptFile() → .vlm → delete .bin
```

## Key Design Decisions

1. **jvmMain source set**: VSMCodec written once, shared by Android + Desktop via intermediate source set. Zero code duplication.
2. **JavaCompatRandom in IosVSMCodec**: LCG implementation matching `java.util.Random` ensures byte-identical Fisher-Yates shuffles across platforms for cross-platform model compatibility.
3. **Internal magic bytes unchanged**: `0x56534D31` ("VSM1") remains as internal format identifier — only visible when hex-inspecting the file, not in filenames.
4. **Enum lookup for filenames**: `vsmFileName()` does exact enum match for clean names, with fallback for unknown filenames.
5. **VLMFiles/ gitignored**: Raw HuggingFace downloads stored locally for development/testing, never committed.

## Files Changed (17 total)

### New (4 files, ~1400 lines)
- `VSMFormat.kt` (commonMain, 160 lines)
- `VSMCodec.kt` (jvmMain, 601 lines)
- `IosVSMCodec.kt` (iosMain, 538 lines)
- `commoncrypto.def` (nativeInterop, 3 lines)

### Modified (13 files, ~450 lines changed)
- `WhisperModels.kt` — added `vsmName` property
- `build.gradle.kts` — jvmMain + commoncrypto cinterop
- `.gitignore` — VLMFiles/
- 3x Config files — shared VLM paths
- 3x Engine files — decrypt-before-load + vlm_tmp
- 3x ModelManager files — download→encrypt + migration
- Chapter 102 documentation

## Downloaded Models (VLMFiles/)

8 models downloaded from HuggingFace (~4.4 GB total):
- `VLMFiles/EN/` — 4 English-only models (Tiny through Medium)
- `VLMFiles/MUL/` — 4 Multilingual models (Tiny through Medium)

These are raw `.bin` files for development. Production use requires encryption to `.vlm` via VSMCodec.
