# SpeechRecognition-Fix-WhisperEngineFullSweep-260222-V1

## Overview
Full sweep of Whisper engine across all 3 platforms (Android, Desktop, iOS) addressing performance bottlenecks, dead code, incorrect confidence scoring, and iOS-specific platform issues.

## Branch
`VoiceOS-1M-SpeechEngine` — commit `9a481e29`

## Changes Summary

### Tier 1: Critical Fixes

#### 1. WhisperVAD paddingBuffer O(n) → O(1)
**File**: `src/commonMain/.../whisper/WhisperVAD.kt`
**Problem**: `ArrayList<Float>.removeAt(0)` shifts all elements — O(n) per call. Called on every audio frame at real-time rates.
**Fix**: Changed to `ArrayDeque<Float>` with `addLast()`/`removeFirst()` — amortized O(1).
**Also**: Pre-computed `maxPaddingSamples` at init instead of per-call.

#### 2. vadSensitivity Dead Code
**Files**: `WhisperVAD.kt`, `WhisperEngine.kt`, `DesktopWhisperEngine.kt`, `IosWhisperEngine.kt`, `DesktopWhisperConfig.kt`, `IosWhisperConfig.kt`
**Problem**: `WhisperConfig.vadSensitivity` (0.0-1.0) was validated in config but never forwarded to `WhisperVAD`. The hardcoded `noiseFloor * 3f` multiplier ignored it.
**Fix**: Added `vadSensitivity` parameter to WhisperVAD constructor. All 3 engine initializers now pass `config.vadSensitivity`. Sensitivity maps linearly: 0.0 (least sensitive, 5.0x multiplier) → 0.5 (moderate, 3.25x) → 1.0 (most sensitive, 1.5x).

#### 3. Desktop autoTuned() Uses JVM Heap Instead of Physical RAM
**File**: `src/desktopMain/.../whisper/DesktopWhisperConfig.kt`
**Problem**: `Runtime.getRuntime().maxMemory()` returns JVM `-Xmx` (~256MB default), causing TINY model selection on machines with 16GB+ RAM.
**Fix**: Use `OperatingSystemMXBean.totalMemorySize` for physical RAM with fallback to JVM heap.

#### 4. Desktop Audio Buffer O(n²)
**File**: `src/desktopMain/.../whisper/DesktopWhisperAudio.kt`
**Problem**: `ArrayList<Float>.removeAt(0)` in audio drain loop — same O(n) issue as VAD.
**Fix**: Changed to `ArrayDeque<Float>` with `addLast()`/`removeFirst()`.

### Tier 2: Confidence Integration (Unified Scoring)

#### 5. Fake Confidence Masking Recognition Quality
**Files**: `WhisperNative.kt`, `DesktopWhisperNative.kt`, `IosWhisperNative.kt`
**Problem**: `DEFAULT_CONFIDENCE = 0.85f` was returned when token probability JNI/cinterop methods weren't available. Since 0.85 equals the HIGH threshold, ALL results appeared highly confident regardless of actual recognition quality.
**Fix**: Changed to `CONFIDENCE_UNAVAILABLE = -1f` sentinel. Added `hasRealConfidence` tracking in `transcribeToText()`. Reports 0f (REJECT level) when probabilities unavailable — honest representation.

#### 6. ConfidenceScorer Integration (Vivoka/VOSK-compatible)
**Files**: `WhisperEngine.kt`, `DesktopWhisperEngine.kt`, `IosWhisperEngine.kt`
**Fix**: All 3 engines now emit `confidence_level` (HIGH/MEDIUM/LOW/REJECT) and `scoring_method` (WHISPER) in metadata, using the same thresholds as ConfidenceScorer (HIGH >0.85, MEDIUM 0.70-0.85, LOW 0.50-0.70, REJECT <0.50).
- Android: Uses `ConfidenceScorer.createResult()` directly
- Desktop/iOS: Uses inline threshold classification (ConfidenceScorer is in androidMain, depends on SimilarityMatcher)

### Tier 3: iOS Platform Improvements

#### 7. Model Download OOM
**File**: `src/iosMain/.../whisper/IosWhisperModelManager.kt`
**Problem**: `NSURLSession.dataTaskWithRequest` buffers entire response as NSData in RAM. Whisper models are 75MB-1.5GB → OOM crashes.
**Fix**: Switched to `downloadTaskWithRequest` which streams to temp file on disk. Added progress monitoring coroutine (polls every 500ms). Fixed `System.currentTimeMillis()` → `kotlin.system.getTimeNanos()` for K/N compatibility.

#### 8. Audio Aliasing in Downsampling
**File**: `src/iosMain/.../whisper/IosWhisperAudio.kt`
**Problem**: Direct linear interpolation from 48kHz→16kHz without anti-aliasing causes frequency folding artifacts.
**Fix**: Added 15-tap Hamming-windowed sinc FIR low-pass filter applied before interpolation. Cutoff at target Nyquist (8kHz). Coefficients cached per sample-rate pair.

## Not Addressed (JNI Limitation)
- **Translation wiring** (Android/Desktop): `nativeFullTranscribe(contextPtr, numThreads, audioData)` JNI signature has no `language` or `translate` params. Would require C++ changes to `whisper_jni.cpp`. iOS already supports translation via cinterop bridge.

## Files Modified (12)
| File | Source Set | Changes |
|------|-----------|---------|
| WhisperVAD.kt | commonMain | ArrayDeque, vadSensitivity param, sensitivity multiplier |
| WhisperEngine.kt | androidMain | ConfidenceScorer integration, vadSensitivity |
| WhisperNative.kt | androidMain | CONFIDENCE_UNAVAILABLE sentinel, hasRealConfidence |
| DesktopWhisperAudio.kt | desktopMain | ArrayDeque buffer |
| DesktopWhisperConfig.kt | desktopMain | Physical RAM, vadSensitivity field |
| DesktopWhisperEngine.kt | desktopMain | Confidence level metadata, vadSensitivity |
| DesktopWhisperNative.kt | desktopMain | CONFIDENCE_UNAVAILABLE sentinel |
| IosWhisperAudio.kt | iosMain | FIR anti-aliasing filter |
| IosWhisperConfig.kt | iosMain | vadSensitivity field |
| IosWhisperEngine.kt | iosMain | Confidence level metadata, vadSensitivity |
| IosWhisperModelManager.kt | iosMain | downloadTaskWithRequest, progress monitor |
| IosWhisperNative.kt | iosMain | CONFIDENCE_UNAVAILABLE sentinel |

## Testing
- 320 unit tests passing (7 test suites, 0 failures, 0 skipped)
- Compile verified for all platform targets
