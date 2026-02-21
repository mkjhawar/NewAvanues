# Developer Manual — Chapter 102: Speech Recognition Multi-Engine Architecture (KMP)

**Module**: `Modules/SpeechRecognition/`
**Platforms**: Android, iOS, macOS, Desktop (Windows/Linux)
**Dependencies**: whisper.cpp (JNI/cinterop), AvanueUI (download UI), Foundation (settings), Speech.framework (Apple)
**Created**: 2026-02-20
**Updated**: 2026-02-21 — Added VLM encryption (Section 14), jvmMain shared source set, shared model storage

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

### Platform Coverage Matrix

| Engine | Android | iOS | macOS | Desktop (JVM) |
|---|---|---|---|---|
| **Whisper** | JNI (WhisperEngine) | cinterop (IosWhisperEngine) | — | JNI (DesktopWhisperEngine) |
| **Apple Speech** | — | SFSpeechRecognizer | SFSpeechRecognizer | — |
| **Android STT** | Built-in | — | — | — |
| **Vivoka** | VSDK AAR | — | — | — |
| **Google Cloud** | Planned (Phase F) | — | — | — |

### Why Whisper?

| Feature | Android STT | Vivoka | Whisper | Apple Speech |
|---|---|---|---|---|
| Offline | No | Yes | Yes | Partial |
| Free | Yes | License | Yes | Yes |
| Languages | System-dependent | Configured | 99 languages | System-dependent |
| Accuracy | Good | Excellent | Very Good | Good |
| Custom vocabulary | No | Yes | No (fuzzy match) | No |
| KMP | No | No | Yes (shared VAD/perf) | No (platform-native) |

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
        vsm/VSMFormat.kt          # VSM file format constants + header

    jvmMain/kotlin/.../whisper/    # Shared by androidMain + desktopMain
        vsm/VSMCodec.kt           # AES-256-CTR encrypt/decrypt (javax.crypto)

    androidMain/kotlin/.../whisper/
        WhisperEngine.kt          # Android engine orchestrator
        WhisperNative.kt          # Thread-safe JNI wrapper
        WhisperAudio.kt           # AudioRecord capture pipeline
        WhisperConfig.kt          # Android configuration
        WhisperModelManager.kt    # OkHttp model downloads
        ui/WhisperModelDownloadScreen.kt  # Compose download UI

    iosMain/kotlin/.../whisper/
        IosWhisperEngine.kt       # iOS engine orchestrator (cinterop)
        IosWhisperNative.kt       # whisper_bridge cinterop wrapper
        IosWhisperAudio.kt        # AVAudioEngine 16kHz capture
        IosWhisperConfig.kt       # iOS configuration + auto-tune
        IosWhisperModelManager.kt # NSURLSession model downloads
        vsm/IosVSMCodec.kt        # AES-256-CTR via CommonCrypto cinterop
    iosMain/kotlin/.../
        IosSpeechRecognitionService.kt  # Dual-engine: Apple Speech + Whisper

    macosMain/kotlin/.../
        MacosSpeechRecognitionService.kt  # SFSpeechRecognizer (macOS 10.15+)
        PlatformUtils.macos.kt           # NSLog + atomicfu
        SpeechRecognitionServiceFactory.macos.kt

    desktopMain/kotlin/.../whisper/
        DesktopWhisperEngine.kt         # Desktop engine orchestrator
        DesktopWhisperNative.kt         # Desktop JNI wrapper
        DesktopWhisperAudio.kt          # javax.sound.sampled capture
        DesktopWhisperConfig.kt         # Desktop configuration
        DesktopWhisperModelManager.kt   # HttpURLConnection downloads

    nativeInterop/cinterop/
        whisper.def               # K/N cinterop definition for whisper_bridge
        commoncrypto.def          # K/N cinterop definition for CommonCrypto (VSM)
        whisper_bridge.h          # C header for whisper.cpp bindings
        whisper_bridge.c          # C implementation wrapping whisper.cpp

    main/cpp/jni/whisper/
        jni.c                     # C JNI bridge (WhisperLib native methods)
        whisper.h                 # whisper.cpp header
```

### 2.2 Data Flow

**Android / Desktop (JNI)**:
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

**iOS (cinterop)**:
```
Microphone (AVAudioEngine)
    |
    v
[IosWhisperAudio] --installTapOnBus--> 16kHz Float32 ring buffer
    |
    v
[WhisperVAD] --speech chunks--> [IosWhisperEngine.transcribeChunk()]
    |
    v
[IosWhisperNative] --cinterop (whisper_bridge.h)--> whisper.cpp
    |                                                     |
    v                                                     v
TranscriptionResult                              whisper_bridge_transcribe()
    v
[RecognitionResult] --> IosSpeechRecognitionService --> VoiceOS
```

**macOS (Apple Speech)**:
```
Microphone (AVAudioEngine — no AVAudioSession needed)
    |
    v
[AVAudioEngine.inputNode.installTapOnBus]
    |
    v
[SFSpeechAudioBufferRecognitionRequest.appendAudioPCMBuffer]
    |
    v
[SFSpeechRecognizer.recognitionTaskWithRequest]
    |                      |-- bestTranscription.formattedString
    |                      |-- segments[].confidence
    v
[RecognitionResult] --> MacosSpeechRecognitionService --> VoiceOS
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

### 3.2 iOS — IosWhisperConfig

```kotlin
val config = IosWhisperConfig(
    modelSize = WhisperModelSize.BASE_EN,  // Recommended for mobile
    language = "en",
    translateToEnglish = false,
    numThreads = 0,                        // 0 = auto (cores/2, max 4)
    silenceThresholdMs = 700,
    minSpeechDurationMs = 300,
    maxChunkDurationMs = 30_000
)

// Auto-tuned (recommended):
val config = IosWhisperConfig.autoTuned(language = "en")
```

**Key difference from Android**: Model storage at `NSSearchPathForDirectoriesInDomains(DocumentDirectory)`, downloads via `NSURLSession`, audio via `AVAudioEngine`.

### 3.3 Desktop — DesktopWhisperConfig

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

Models are stored as encrypted `.vlm` (VoiceOS Language Model) files in a shared location that matches the AI/ALC model storage pattern. See **Section 14** for full encryption details.

**On-device filenames**: `VoiceOS-{Size}-{Lang}.vlm` — no whisper/ggml traces. Size codes: Tin, Bas, Sml, Med. Language: EN (English-only) or MUL (multilingual, 99 languages).

| Platform | Shared VLM Location (Primary) | Legacy Location (Auto-migrated) |
|---|---|---|
| Android | `/sdcard/ava-ai-models/vlm/` | `/data/data/<pkg>/files/whisper/models/` |
| iOS | `Documents/ava-ai-models/vlm/` | `Documents/whisper/models/` |
| Desktop | `~/.augmentalis/models/vlm/` | `~/.avanues/whisper/models/` |

**Check order**: Shared VLM → Legacy .bin → Download from HuggingFace

Legacy `.bin` files are automatically encrypted to `.vlm` and moved to shared storage on first access via `migrateExistingModels()`.

**Example files on disk**:
```
ava-ai-models/vlm/
  VoiceOS-Tin-EN.vlm     (75 MB, English only)
  VoiceOS-Bas-MUL.vlm    (142 MB, 99 languages)
  VoiceOS-Sml-EN.vlm     (466 MB, English only)
```

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

### 11.3 iOS cinterop (whisper_bridge)

The iOS implementation uses Kotlin/Native cinterop instead of JNI. A thin C bridge (`whisper_bridge.h/c`) wraps whisper.cpp functions for K/N consumption.

**Build requirements**:
```bash
cd Modules/Whisper && ./build-xcframework.sh
# Output: libs/ios-arm64/libwhisper_bridge.a (device)
#         libs/ios-sim-arm64/libwhisper_bridge.a (simulator)
```

**cinterop definition** (`src/nativeInterop/cinterop/whisper.def`):
```
headers = whisper_bridge.h
staticLibraries = libwhisper_bridge.a
libraryPaths = libs/ios-arm64  (or ios-sim-arm64)
```

**Bridge functions**:

| C Function | Kotlin Binding | Purpose |
|---|---|---|
| `whisper_bridge_init` | `whisper_bridge_init(path)` | Load model, return context ptr |
| `whisper_bridge_transcribe` | `whisper_bridge_transcribe(ctx, data, len, lang, threads)` | Run inference |
| `whisper_bridge_get_segments_count` | `...get_segments_count(ctx)` | Segment count |
| `whisper_bridge_get_segment_text` | `...get_segment_text(ctx, i)` | Segment text (CPointer) |
| `whisper_bridge_get_segment_t0` | `...get_segment_t0(ctx, i)` | Start time (ms) |
| `whisper_bridge_get_segment_t1` | `...get_segment_t1(ctx, i)` | End time (ms) |
| `whisper_bridge_get_lang` | `...get_lang(ctx)` | Detected language |
| `whisper_bridge_free` | `whisper_bridge_free(ctx)` | Release context |

**IosWhisperNative thread safety**: Uses `atomicfu.SynchronizedObject` + `synchronized(lock)` blocks (K/N equivalent of JVM synchronized).

---

## 12. iOS Dual-Engine Architecture

`IosSpeechRecognitionService` supports two engines selected at initialization:

```kotlin
// In IosSpeechRecognitionService.initialize():
when (config.engine) {
    SpeechEngine.APPLE_SPEECH -> initializeAppleSpeech(config)  // Default
    SpeechEngine.WHISPER -> initializeWhisper(config)           // Offline
    else -> initializeAppleSpeech(config)                       // Fallback
}
```

### 12.1 Engine Switching

- Engine is selected once during `initialize()` based on `SpeechConfig.engine`
- All `startListening()`, `stopListening()`, `pause()`, `resume()`, `setLanguage()` calls dispatch to the active engine
- Whisper result/error flows are forwarded through the service's shared flows
- `release()` cleans up whichever engine is active

### 12.2 Apple Speech Features (iOS + macOS)

Both platforms share the same SFSpeechRecognizer API with one key difference:

| Feature | iOS | macOS |
|---|---|---|
| AVAudioSession | Required (setCategory + setActive) | Not needed |
| SFSpeechRecognizer | Available iOS 10+ | Available macOS 10.15+ |
| On-device recognition | Device-dependent | Device-dependent |
| Authorization prompt | Privacy - Speech Recognition | System Settings > Privacy |
| Streaming | Yes (SFSpeechAudioBufferRecognitionRequest) | Yes |
| Partial results | Yes | Yes |
| Multi-locale | Yes (recreate recognizer) | Yes (recreate recognizer) |

### 12.3 macOS Build Configuration

macOS targets are conditional — only compiled when native tasks are invoked:

```kotlin
// build.gradle.kts
if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
    gradle.startParameter.taskNames.any {
        it.contains("ios", ignoreCase = true) ||
        it.contains("macos", ignoreCase = true) ||
        it.contains("Framework", ignoreCase = true)
    }
) {
    macosX64()
    macosArm64()
}
```

**macOS source set**: `macosMain` depends on `commonMain`, includes `kotlinx-atomicfu` and `compose-runtime`.

---

## 13. Troubleshooting

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

### iOS cinterop Linker Error

```
ld: library not found for -lwhisper_bridge
```

**Fix**: Build the static library first:
```bash
cd Modules/Whisper && ./build-xcframework.sh
```
Ensure output `.a` files are at the paths specified in `whisper.def`.

### macOS Speech Recognition Denied

```
Speech recognition denied — check System Settings > Privacy > Speech Recognition
```

**Fix**: Open System Settings > Privacy & Security > Speech Recognition and enable the app. macOS also requires a separate Microphone permission.

---

## 14. VLM Encryption (VoiceOS Language Model)

### 14.1 Overview

Whisper model files (`.bin`, 75MB–1.5GB) are encrypted at rest using a custom `.vlm` container format. This prevents casual extraction of model weights from device storage, matching the same protection level as the AI/ALC module's `.amm`/`.amg`/`.amr` formats. On-device filenames use `VoiceOS-{Size}-{Lang}.vlm` — zero traces of whisper/ggml.

**Extension**: `.vlm` (VoiceOS Language Model)
**On-disk naming**: `VoiceOS-Tin-EN.vlm`, `VoiceOS-Bas-MUL.vlm`, etc.
**Magic bytes**: `0x56534D31` ("VSM1" — internal format identifier)
**Header**: 64 bytes
**Crypto**: AES-256-CTR + XOR scramble (SHA-512 derived) + Fisher-Yates byte shuffle

### 14.2 File Format

```
Offset  Size    Field
0       4       Magic (0x56534D31 = "VSM1" — internal format ID)
4       2       Version (0x0100 = v1.0)
6       2       Flags (0x0001 = encrypted)
8       8       Original file size (LE)
16      8       Encoded file size (LE)
24      4       Block size (65536 = 64KB)
28      4       Block count
32      16      File hash (truncated SHA-256)
48      8       Timestamp (epoch millis, LE)
56      4       Content type (0 = generic)
60      4       Reserved
--- 64 bytes header ---
[optional metadata JSON length (4 bytes) + JSON string]
[encrypted blocks...]
```

### 14.3 Encryption Pipeline (Per Block)

```
Raw 64KB block
    |
    v
[XOR Scramble] — SHA-512 hash of (key + blockIndex) generates 64-byte XOR pattern, tiled across block
    |
    v
[Fisher-Yates Shuffle] — deterministic byte permutation seeded from block key
    |
    v
[AES-256-CTR] — per-block nonce = MD5(key + blockIndex), counter starts at 0
    |
    v
Encrypted block written to .vsm file
```

Decryption reverses the pipeline: AES-CTR decrypt → Fisher-Yates unshuffle → XOR unscramble.

### 14.4 Key Derivation

```
Master Seed: "VSM-SPEECH-1.0-VOICEOS-2026-IDL\0" (32 bytes, unique to VSM)
Salt: "VSM-1.0-SALT-2026"
Key Material: MASTER_SEED + fileHash[0:16] + timestamp(8 bytes LE)
KDF: PBKDF2-HMAC-SHA256, 10000 iterations → 32-byte AES key
```

The file hash and timestamp come from the VSM header, making each file's key unique.

### 14.5 On-Device File Naming

| Model Size | English-Only | Multilingual |
|---|---|---|
| Tiny (75 MB) | `VoiceOS-Tin-EN.vlm` | `VoiceOS-Tin-MUL.vlm` |
| Base (142 MB) | `VoiceOS-Bas-EN.vlm` | `VoiceOS-Bas-MUL.vlm` |
| Small (466 MB) | `VoiceOS-Sml-EN.vlm` | `VoiceOS-Sml-MUL.vlm` |
| Medium (1.5 GB) | `VoiceOS-Med-EN.vlm` | `VoiceOS-Med-MUL.vlm` |

Zero traces of whisper, ggml, or any model source. The `ggmlFileName` field in `WhisperModelSize` is ONLY used for HuggingFace download URLs and is never stored on device.

### 14.6 Platform Implementations

| Platform | Codec Class | Crypto Library | Location |
|---|---|---|---|
| Android | `VSMCodec` (shared via jvmMain) | `javax.crypto` | `src/jvmMain/.../vsm/VSMCodec.kt` |
| Desktop | `VSMCodec` (shared via jvmMain) | `javax.crypto` | Same file — zero duplication |
| iOS | `IosVSMCodec` | CommonCrypto (cinterop) | `src/iosMain/.../vsm/IosVSMCodec.kt` |

**Key design**: Android and Desktop share the exact same `VSMCodec.kt` via the `jvmMain` intermediate source set. Bug fixes automatically propagate to both platforms.

**Cross-platform compatibility**: iOS `IosVSMCodec` includes a `JavaCompatRandom` class that reimplements `java.util.Random`'s LCG algorithm (multiplier `0x5DEECE66DL`, 48-bit mask) to produce byte-identical Fisher-Yates shuffles as the JVM implementation. This ensures `.vlm` files created on any platform can be read on any other.

### 14.7 Data Flow

```
CHECK (before any download):
  1. Shared location (ava-ai-models/vlm/*.vlm) → exists? USE IT
  2. Legacy app-private (whisper/models/*.bin) → exists? MIGRATE to .vlm
  3. Nothing found → DOWNLOAD

DOWNLOAD:
  HuggingFace (.bin) → download to temp → VSMCodec.encryptFile() → .vlm → delete temp

LOAD:
  .vlm → VSMCodec.decryptToTempFile() → temp file → initContext(tempPath) → delete temp
```

### 14.8 API

```kotlin
// JVM (Android + Desktop)
val codec = VSMCodec()
codec.encryptFile(inputPath, outputPath, metadata)  // Returns Boolean
codec.decryptToTempFile(vlmPath, tempDir)            // Returns File?
codec.readHeader(vlmPath)                            // Returns VSMHeader?
codec.readMetadata(vlmPath)                          // Returns Map<String,String>

// iOS
val codec = IosVSMCodec()
codec.encryptFile(inputPath, outputPath, metadata)   // Returns Boolean
codec.decryptToTempFile(vlmPath, tempDir)             // Returns String? (path)
```

### 14.9 Migration

Each `ModelManager` has a `migrateExistingModels()` method that:
1. Scans the legacy models directory for `.bin` files
2. Encrypts each to `.vlm` in the shared storage directory
3. Deletes the original `.bin` on success
4. Safe to call multiple times (skips already-migrated files)
