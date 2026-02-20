# Developer Manual — Chapter 102: Whisper Speech Engine (KMP)

**Module**: `Modules/SpeechRecognition/`
**Platforms**: Android, Desktop (macOS/Windows/Linux)
**Dependencies**: whisper.cpp (JNI), AvanueUI (download UI), Foundation (settings)
**Created**: 2026-02-20

---

## 1. Overview

The Whisper Speech Engine provides **fully offline** speech recognition using OpenAI's Whisper model via the whisper.cpp C library (JNI bridge). It supports:

- Offline transcription (no network required for inference)
- Multiple model sizes (Tiny through Medium, English-only and multilingual)
- VAD-chunked pseudo-streaming (transcribes on silence boundaries)
- Auto model download from HuggingFace with resume support
- Per-segment confidence from token probabilities
- Automatic language detection
- Performance metrics tracking

### Why Whisper?

| Feature | Android STT | Vivoka | Whisper |
|---|---|---|---|
| Offline | No | Yes | Yes |
| Free | Yes | License | Yes |
| Languages | System-dependent | Configured | 99 languages |
| Accuracy | Good | Excellent | Very Good |
| Custom vocabulary | No | Yes | No (but fuzzy match) |
| KMP | No | No | Yes (engine is KMP) |

---

## 2. Architecture

### 2.1 Source Set Layout

```
Modules/SpeechRecognition/src/
    commonMain/kotlin/.../whisper/
        WhisperModels.kt          # Shared enums + data classes
        WhisperVAD.kt             # Voice Activity Detection algorithm
        ModelDownloadState.kt     # Download state machine
        WhisperPerformance.kt     # Performance metrics tracker

    androidMain/kotlin/.../whisper/
        WhisperEngine.kt          # Android engine orchestrator
        WhisperNative.kt          # Thread-safe JNI wrapper
        WhisperAudio.kt           # AudioRecord capture pipeline
        WhisperConfig.kt          # Android configuration
        WhisperModelManager.kt    # OkHttp model downloads
        ui/WhisperModelDownloadScreen.kt  # Compose download UI

    desktopMain/kotlin/.../whisper/
        DesktopWhisperEngine.kt         # Desktop engine orchestrator
        DesktopWhisperNative.kt         # Desktop JNI wrapper
        DesktopWhisperAudio.kt          # javax.sound.sampled capture
        DesktopWhisperConfig.kt         # Desktop configuration
        DesktopWhisperModelManager.kt   # HttpURLConnection downloads

    main/cpp/jni/whisper/
        jni.c                     # C JNI bridge (WhisperLib native methods)
        whisper.h                 # whisper.cpp header
```

### 2.2 Data Flow

```
Microphone
    |
    v
[WhisperAudio] --16kHz/16-bit/mono--> Float32 buffer
    |
    v
[WhisperVAD] --speech chunks--> [WhisperEngine.transcribeChunk()]
    |
    v
[WhisperNative.transcribeToText()] --synchronized JNI-->
    |
    v
[whisper.cpp] --segments+tokens--> TranscriptionResult
    |                                  |-- text
    |                                  |-- segments[].confidence
    |                                  |-- detectedLanguage
    v
[RecognitionResult] --> resultFlow --> SpeechRecognitionService --> VoiceOS
```

### 2.3 Engine Lifecycle

```
UNINITIALIZED
    |
    | initialize(config)
    v
LOADING_MODEL  --[auto-download if missing]-->
    |
    | model loaded + audio initialized
    v
READY
    |
    | startListening()
    v
LISTENING  <--[VAD silence boundary]--> PROCESSING
    |                                        |
    | stopListening()                        | transcribeChunk()
    v                                        |
READY  <-------------------------------------+
    |
    | destroy()
    v
DESTROYED
```

---

## 3. Configuration

### 3.1 Android — WhisperConfig

```kotlin
val config = WhisperConfig(
    modelSize = WhisperModelSize.BASE_EN,  // Model to use
    language = "en",                        // BCP-47 language code
    translateToEnglish = false,             // Whisper translation feature
    numThreads = 0,                         // 0 = auto (cores/2, max 4)
    silenceThresholdMs = 700,               // VAD silence duration
    minSpeechDurationMs = 300,              // Minimum utterance length
    maxChunkDurationMs = 30_000             // Max before forced transcription
)

// Auto-tuned (recommended):
val config = WhisperConfig.autoTuned(context, language = "en")
```

### 3.2 Desktop — DesktopWhisperConfig

```kotlin
val config = DesktopWhisperConfig(
    modelSize = WhisperModelSize.SMALL_EN,  // Desktop can handle larger models
    numThreads = 0,                          // 0 = auto (cores/2, max 8)
    language = "en"
)

// Auto-tuned:
val config = DesktopWhisperConfig.autoTuned(language = "en")
```

### 3.3 Model Selection

| Model | Size | Min RAM | Speed | Accuracy | English-Only |
|---|---|---|---|---|---|
| TINY | 75 MB | 256 MB | Fast | Basic | No |
| TINY_EN | 75 MB | 256 MB | Fast | Basic | Yes |
| BASE | 142 MB | 512 MB | Moderate | Good | No |
| BASE_EN | 142 MB | 512 MB | Moderate | Good | Yes |
| SMALL | 466 MB | 1 GB | Slow | Very Good | No |
| SMALL_EN | 466 MB | 1 GB | Slow | Very Good | Yes |
| MEDIUM | 1500 MB | 2 GB | Very Slow | Excellent | No |
| MEDIUM_EN | 1500 MB | 2 GB | Very Slow | Excellent | Yes |

**Auto-selection**: `WhisperModelSize.forAvailableRAM(ramMB, englishOnly)` picks the best model that fits in 50% of available RAM.

---

## 4. Model Management

### 4.1 Model Storage

| Platform | Location |
|---|---|
| Android | `/data/data/<pkg>/files/whisper/models/` |
| Desktop | `~/.avanues/whisper/models/` |

### 4.2 Auto-Download

When `WhisperEngine.initialize()` is called and no model file is found, the engine automatically downloads the configured model from HuggingFace:

```
URL: https://huggingface.co/ggerganov/whisper.cpp/resolve/main/{ggmlFileName}
```

Features:
- **Resume support**: Partial downloads (`.partial` suffix) are preserved and resumed via HTTP Range headers
- **SHA256 verification**: Downloaded files are verified against known checksums
- **Progress tracking**: `ModelDownloadState` flow emits progress, speed, and ETA
- **Cancellation**: `modelManager.cancelDownload()` stops download, keeps partial file

### 4.3 Download UI (Android)

```kotlin
WhisperModelDownloadScreen(
    modelManager = engine.getModelManager(),
    onModelReady = { modelSize -> /* model downloaded */ },
    onDismiss = { /* close screen */ }
)
```

- AvanueTheme-compliant with SpatialVoice gradient background
- AVID voice identifiers on all interactive elements
- Shows model list with size/speed/accuracy/language info
- Download progress bar with speed and ETA
- Storage management (available space, delete models)

### 4.4 ModelDownloadState

```kotlin
sealed class ModelDownloadState {
    data object Idle
    data object Checking
    data object FetchingMetadata
    data class Downloading(
        val modelSize, val bytesDownloaded, val totalBytes, val speedBytesPerSec
    )  // Also exposes: progressPercent, downloadedMB, totalMB, speedMBPerSec, estimatedRemainingSeconds
    data class Verifying(val modelSize)
    data class Completed(val modelSize, val filePath)
    data class Failed(val modelSize, val error)
    data class Cancelled(val modelSize)
}
```

---

## 5. Voice Activity Detection (VAD)

The VAD algorithm lives in commonMain and is shared across platforms.

### 5.1 Algorithm

1. **Energy calculation**: RMS of audio samples per frame
2. **Adaptive threshold**: Noise floor estimation from silence, speech threshold = noise floor * sensitivity multiplier
3. **State machine**: SILENCE -> SPEECH -> HANGOVER -> SILENCE
4. **Hangover timer**: Prevents premature cutoff during brief pauses
5. **Padding buffer**: Preserves pre-speech audio (150ms) for natural starts

### 5.2 Configuration

```kotlin
WhisperVAD(
    speechThreshold = 0f,        // 0 = auto-calibrate from noise floor
    silenceTimeoutMs = 700,      // Silence before finalizing chunk
    minSpeechDurationMs = 300,   // Minimum valid utterance
    maxSpeechDurationMs = 30000, // Max before forced transcription
    hangoverFrames = 5,          // Frames to wait after speech ends
    paddingMs = 150,             // Pre-speech audio padding
    sampleRate = 16000           // Audio sample rate
)
```

---

## 6. Confidence Scoring

### 6.1 Token-Level Confidence

whisper.cpp exposes per-token probabilities via `whisper_full_get_token_p()`. The engine computes per-segment confidence by averaging all token probabilities within each segment:

```
segment_confidence = sum(token_probabilities) / token_count
overall_confidence = sum(segment_confidences) / segment_count
```

### 6.2 Graceful Fallback

If the native JNI methods for confidence are not yet compiled (UnsatisfiedLinkError), the engine falls back to a default confidence of 0.85f. This allows the Kotlin layer to work independently of the native library version.

### 6.3 TranscriptionResult Fields

```kotlin
data class TranscriptionResult(
    val text: String,
    val segments: List<TranscriptionSegment>,
    val processingTimeMs: Long,
    val confidence: Float = 0f,           // Average across all segments
    val detectedLanguage: String? = null   // Auto-detected language code
)

data class TranscriptionSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float = 0f            // Average token probability
)
```

---

## 7. Language Detection

After transcription, whisper.cpp provides the detected language via `whisper_full_lang_id()`. The engine exposes this as `TranscriptionResult.detectedLanguage`.

This is useful when:
- Language is set to "auto" for multilingual environments
- Verifying the user is speaking the expected language
- Adapting command matching to the detected locale

---

## 8. Performance Tracking

### 8.1 WhisperPerformance (commonMain)

Thread-safe rolling-window metrics tracker shared across platforms:

```kotlin
val perf = engine.performance

// After some transcriptions:
perf.getAverageLatencyMs()    // Rolling average processing time
perf.getAverageRTF()          // Real-time factor (< 1.0 = faster than real-time)
perf.getAverageConfidence()   // Rolling average confidence
perf.totalTranscriptions      // Total count
perf.emptyTranscriptions      // Silence/noise count
perf.peakLatencyMs            // Worst-case latency
perf.detectedLanguage         // Last detected language
perf.getMetrics()             // Full map for logging/reporting
```

### 8.2 Key Metrics

| Metric | Description | Good Range |
|---|---|---|
| RTF (Real-Time Factor) | Processing time / audio duration | < 0.5 |
| Latency | Processing time per chunk | < 500ms (TINY), < 2s (SMALL) |
| Confidence | Token probability average | 0.6 - 0.95 |
| Success Rate | Non-empty / total transcriptions | > 80% |

---

## 9. Thread Safety

### 9.1 JNI Serialization

whisper.cpp contexts are **NOT thread-safe**. All native calls are serialized through a single `synchronized(this)` lock in `WhisperNative`/`DesktopWhisperNative`.

Overhead: ~50ns per uncontended lock + ~5us JNI crossing. This is <0.001% of whisper inference time (200-2000ms).

### 9.2 transcribeToText() Atomicity

The entire transcribe+read cycle runs in one synchronized block:
1. `fullTranscribe()` — run inference
2. `getTextSegmentCount()` — read segment count
3. For each segment: `getTextSegment()`, `getTextSegmentT0()`, `getTextSegmentT1()`, token probabilities
4. `getDetectedLanguage()` — read language

This prevents interleaving from concurrent callers.

---

## 10. Integration Guide

### 10.1 Using Whisper in AndroidSpeechRecognitionService

```kotlin
// In AndroidSpeechRecognitionService.initialize():
when (config.engine) {
    SpeechEngine.WHISPER -> {
        val engine = WhisperEngine(context)
        engine.initialize(WhisperConfig.autoTuned(context, config.language))
        // Collect engine.resultFlow -> process -> emit to service resultFlow
    }
}
```

### 10.2 Observing Download Progress

```kotlin
// From UI:
engine.modelDownloadState.collect { state ->
    when (state) {
        is ModelDownloadState.Downloading -> updateProgressBar(state.progressPercent)
        is ModelDownloadState.Completed -> showReady()
        is ModelDownloadState.Failed -> showError(state.error)
    }
}
```

### 10.3 Adding a New Model Size

1. Add entry to `WhisperModelSize` enum in `commonMain/WhisperModels.kt`
2. Update `forAvailableRAM()` selection logic if needed
3. Add SHA256 to `WhisperModelUrls.KNOWN_CHECKSUMS` if known
4. No other changes needed — download UI auto-discovers from enum entries

---

## 11. JNI Native Methods

### 11.1 WhisperLib (com.whispercpp.whisper) — Primary Bridge

| Method | Signature | Purpose |
|---|---|---|
| `initContext` | `(String) -> Long` | Load model from file path |
| `initContextFromAsset` | `(AssetManager, String) -> Long` | Load from APK assets |
| `freeContext` | `(Long) -> Unit` | Release context |
| `fullTranscribe` | `(Long, Int, FloatArray) -> Unit` | Run inference |
| `getTextSegmentCount` | `(Long) -> Int` | Segment count |
| `getTextSegment` | `(Long, Int) -> String` | Segment text |
| `getTextSegmentT0` | `(Long, Int) -> Long` | Segment start time |
| `getTextSegmentT1` | `(Long, Int) -> Long` | Segment end time |
| `getTextSegmentTokenCount` | `(Long, Int) -> Int` | Token count per segment |
| `getTextSegmentTokenProb` | `(Long, Int, Int) -> Float` | Token probability |
| `getDetectedLanguage` | `(Long) -> String` | Detected language code |
| `getSystemInfo` | `() -> String` | System capabilities |
| `benchMemcpy` | `(Int) -> String` | Memory benchmark |
| `benchGgmlMulMat` | `(Int) -> String` | Matrix multiply benchmark |

### 11.2 Desktop Equivalent

Desktop uses private `external fun` declarations with the same semantics, registered under the `DesktopWhisperNative` class namespace.

---

## 12. Troubleshooting

### Model Not Found

```
IllegalStateException: Whisper model not found: ggml-base.en.bin
```

**Fix**: The engine auto-downloads on init. If download fails, check:
- Network connectivity
- Storage space (`getAvailableStorageMB()`)
- HuggingFace availability

### JNI Library Not Found

```
UnsatisfiedLinkError: whisper-jni
```

**Android**: Ensure `whisper-jni.so` is in `jniLibs/{abi}/`
**Desktop**: Place native library in one of:
- `java.library.path`
- Working directory
- `~/.avanues/lib/`

### Confidence Always 0.85

The native confidence methods (token probability) require the JNI library to include the new methods added in Phase E. If using an older native library, the engine gracefully falls back to 0.85f. Recompile the native library to get real confidence scores.
