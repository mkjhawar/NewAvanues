# Developer Manual — Chapter 102: Speech Recognition Multi-Engine Architecture (KMP)

**Module**: `Modules/SpeechRecognition/`
**Platforms**: Android, iOS, macOS, Desktop (Windows/Linux)
**Dependencies**: whisper.cpp (JNI/cinterop), AvanueUI (download UI), Foundation (settings), Speech.framework (Apple)
**Created**: 2026-02-20
**Updated**: 2026-02-24 — KMP atomicfu thread safety (Section 9), macOS Whisper support, PII-safe logging, totalSegments metric, download retry/backoff (Section 4.5), NSError capture pattern (Section 3.2), WhisperPerformance tests (Section 8.3), memory-aware model selection at runtime (Section 3.6)

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
| **Whisper** | JNI (WhisperEngine) | cinterop (IosWhisperEngine) | cinterop (shared iosMain) | JNI (DesktopWhisperEngine) |
| **Apple Speech** | — | SFSpeechRecognizer | SFSpeechRecognizer | — |
| **Android STT** | Built-in | — | — | — |
| **Vivoka** | VSDK AAR | — | — | — |
| **Google Cloud** | GoogleCloudEngine (VAD_BATCH + STREAMING) | — | — | — |

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

    androidMain/kotlin/.../googlecloud/
        GoogleCloudConfig.kt          # Configuration + validation
        GoogleCloudApiClient.kt       # REST batch recognize
        GoogleCloudStreamingClient.kt # HTTP/2 streaming recognize
        GoogleCloudEngine.kt          # Engine orchestrator

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
        PlatformUtils.macos.kt           # NSLog + atomicfu SynchronizedObject
        SpeechRecognitionServiceFactory.macos.kt

    NOTE: commoncrypto.def was removed (260224) — CommonCrypto imports
    are resolved via platform.CoreCrypto.* in Kotlin/Native without a
    separate cinterop definition.

    desktopMain/kotlin/.../whisper/
        DesktopWhisperEngine.kt         # Desktop engine orchestrator
        DesktopWhisperNative.kt         # Desktop JNI wrapper
        DesktopWhisperAudio.kt          # javax.sound.sampled capture
        DesktopWhisperConfig.kt         # Desktop configuration
        DesktopWhisperModelManager.kt   # HttpURLConnection downloads

    nativeInterop/cinterop/
        whisper.def               # K/N cinterop definition for whisper_bridge
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

**Google Cloud — VAD Batch Mode**:
```
Microphone
    |
    v
[WhisperAudio] --16kHz/16-bit/mono--> Float32 buffer
    |
    v
[WhisperVAD] --speech chunks--> [GoogleCloudEngine.recognizeChunk()]
    |
    v
[GoogleCloudApiClient.recognize()] --REST POST-->
    |
    v
Google Cloud STT v2 API --JSON response-->
    v
[RecognitionResult] --> resultFlow --> SpeechRecognitionService --> VoiceOS
```

**Google Cloud — Streaming Mode**:
```
Microphone
    |
    v
[WhisperAudio] --continuous audio--> Float32 buffer
    |
    v
[GoogleCloudStreamingClient.sendAudioChunk()] --HTTP/2 chunked-->
    |
    v
Google Cloud STT v2 Streaming API --newline-delimited JSON-->
    v
partial + final [RecognitionResult] --> resultFlow --> VoiceOS
```

### 2.3 Engine Lifecycle

```
UNINITIALIZED
    |
    | initialize(config)
    v
LOADING_MODEL  --[memory check → may downgrade model]--> [auto-download if missing]-->
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
    vadSensitivity = 0.6f,                  // 0.0 (least sensitive) → 1.0 (most sensitive)
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
    vadSensitivity = 0.6f,                 // 0.0 (least) → 1.0 (most sensitive)
    silenceThresholdMs = 700,
    minSpeechDurationMs = 300,
    maxChunkDurationMs = 30_000
)

// Auto-tuned (recommended):
val config = IosWhisperConfig.autoTuned(language = "en")
```

**Key differences from Android**: Model storage at `NSSearchPathForDirectoriesInDomains(DocumentDirectory)`, downloads via `NSURLSession.downloadTaskWithRequest` (streams to disk — avoids OOM for large models), audio via `AVAudioEngine` with FIR anti-aliasing filter for downsampling.

**AVAudioEngine Error Handling (Kotlin/Native)**

iOS audio initialization uses `startAndReturnError()` with Kotlin/Native's `memScoped` pattern for safe NSError capture:

```kotlin
@OptIn(BetaInteropApi::class)
memScoped {
    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
    val started = audioEngine.startAndReturnError(errorPtr.ptr)
    if (!started) {
        val error = errorPtr.value
        logError(TAG, "AVAudioEngine start failed: ${error?.localizedDescription}")
    }
}
```

5 call sites use this pattern across `IosWhisperAudio.kt`, `IosSpeechRecognitionService.kt`, and `MacosSpeechRecognitionService.kt`. The `@OptIn(BetaInteropApi::class)` annotation is required for `ObjCObjectVar` access in Kotlin 2.1.0.

### 3.3 Desktop — DesktopWhisperConfig

```kotlin
val config = DesktopWhisperConfig(
    modelSize = WhisperModelSize.SMALL_EN,  // Desktop can handle larger models
    numThreads = 0,                          // 0 = auto (cores/2, max 8)
    language = "en",
    vadSensitivity = 0.6f                    // 0.0 (least) → 1.0 (most sensitive)
)

// Auto-tuned (uses physical RAM via OperatingSystemMXBean, not JVM heap):
val config = DesktopWhisperConfig.autoTuned(language = "en")
```

### 3.4 Google Cloud — GoogleCloudConfig

```kotlin
val config = GoogleCloudConfig(
    projectId = "my-gcp-project",   // Required
    mode = GoogleCloudMode.VAD_BATCH,  // or STREAMING
    language = "en-US",              // BCP-47 language code
    model = "latest_short",          // "latest_short" for commands, "latest_long" for dictation
    authMode = GoogleCloudAuthMode.FIREBASE_AUTH,  // or API_KEY
    apiKey = null,                   // Required if authMode = API_KEY
    maxAlternatives = 3              // Alternative transcriptions
)

// From unified SpeechConfig:
val config = GoogleCloudConfig.fromSpeechConfig(speechConfig)
```

### 3.5 Model Selection

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

### 3.6 Memory-Aware Runtime Model Selection (Android)

`WhisperConfig.autoTuned()` selects a model based on `totalMem` (total device RAM), which is a **static** value. However, `availMem` (currently free RAM) can be much lower when other apps are running. Loading a model that exceeds available RAM triggers a page fault storm that can cause ANR.

**Runtime guard in `performInitialization()`** (added 2026-02-24, fix for ANR ErrorId `54caaec6`):

Before model loading, `WhisperEngine.performInitialization()` checks `ActivityManager.MemoryInfo`:

| Condition | Action |
|-----------|--------|
| `memInfo.lowMemory == true` | Force TINY model (or TINY_EN if English-only) |
| `availMem < model.minRAMMB` | Auto-downgrade via `WhisperModelSize.forAvailableRAM(availMB, isEnglish)` |
| Otherwise | Use model from config as-is |

```kotlin
// Step 2c in performInitialization():
val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
val memInfo = ActivityManager.MemoryInfo()
activityManager.getMemoryInfo(memInfo)
val availableMB = (memInfo.availMem / (1024 * 1024)).toInt()

if (memInfo.lowMemory) {
    // Force smallest model to prevent ANR
    val tinyModel = if (config.modelSize.isEnglishOnly) WhisperModelSize.TINY_EN else WhisperModelSize.TINY
    config = config.copy(modelSize = tinyModel)
} else if (availableMB < config.modelSize.minRAMMB) {
    // Downgrade to fit available memory
    config = config.copy(modelSize = WhisperModelSize.forAvailableRAM(availableMB, isEnglish))
}
```

**Why this matters**: A device with 8GB total RAM might only have 400MB available at app startup. `autoTuned()` would select SMALL (needs 1024MB), but the runtime check downgrades to BASE (needs 512MB) or TINY (needs 256MB) based on actual availability. This prevents the 146K+ minor page faults that were causing ANR in production.

**Note**: `forAvailableRAM()` uses a 2x safety margin — it requires `availMem >= 2 * model.minRAMMB` (i.e., filters out any model where `minRAMMB > availableMB / 2`). This accounts for whisper.cpp working memory during inference. The runtime check is conservative and may downgrade even when technically sufficient memory exists, which is the correct tradeoff (slightly worse accuracy vs. app crash).

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
    data class Retrying(
        val modelSize: WhisperModelSize,
        val attempt: Int,
        val maxAttempts: Int,
        val delayMs: Long
    )
    data class Failed(val modelSize, val error)
    data class Cancelled(val modelSize)
}
```

### 4.5 Download Retry with Exponential Backoff

Both `WhisperModelManager` (Android) and `IosWhisperModelManager` (iOS) use exponential backoff for transient download failures:

- **Base delay**: 2 seconds, multiplied by 2^(attempt-1), capped at 30 seconds
- **Maximum attempts**: 3 before transitioning to `Failed` state
- **Progression**: 2s → 4s → 8s (would be 16s, 32s→30s if more attempts were configured)
- **Cancellation handling**: `CancellationException` is re-thrown immediately (never retried) to respect coroutine cancellation
- **State transitions**: `Downloading` → `Retrying(attempt=1, delayMs=2000)` → `Downloading` → `Retrying(attempt=2, delayMs=4000)` → ... → `Failed`
- **UI feedback**: The `ModelDownloadState.Retrying` state is emitted to `modelDownloadState` StateFlow so UI can show retry countdown

---

## 5. Voice Activity Detection (VAD)

The VAD algorithm lives in commonMain and is shared across platforms.

### 5.1 Algorithm

1. **Energy calculation**: RMS of audio samples per frame
2. **Adaptive threshold**: Noise floor estimation from silence, speech threshold = noise floor * sensitivity multiplier (derived from `vadSensitivity`)
3. **State machine**: SILENCE -> SPEECH -> HANGOVER -> SILENCE
4. **Hangover timer**: Prevents premature cutoff during brief pauses
5. **Padding buffer**: `ArrayDeque<Float>` ring buffer preserving pre-speech audio (150ms) for natural starts — O(1) operations via `addLast()`/`removeFirst()`

### 5.2 Configuration

```kotlin
WhisperVAD(
    speechThreshold = 0f,        // 0 = auto-calibrate from noise floor
    vadSensitivity = 0.6f,       // 0.0 (least sensitive, 5.0x multiplier) → 1.0 (most sensitive, 1.5x)
    silenceTimeoutMs = 700,      // Silence before finalizing chunk
    minSpeechDurationMs = 300,   // Minimum valid utterance
    maxSpeechDurationMs = 30000, // Max before forced transcription
    hangoverFrames = 5,          // Frames to wait after speech ends
    paddingMs = 150,             // Pre-speech audio padding
    sampleRate = 16000           // Audio sample rate
)
```

### 5.3 Sensitivity Mapping

The `vadSensitivity` parameter (0.0-1.0) controls the noise floor multiplier for adaptive threshold calculation:

| vadSensitivity | Multiplier | Behavior |
|---|---|---|
| 0.0 (least sensitive) | 5.0x noise floor | Only loud, clear speech triggers detection |
| 0.5 (moderate) | 3.25x noise floor | Balanced sensitivity (default-equivalent) |
| 0.6 (default) | 2.9x noise floor | Slightly more sensitive than moderate |
| 1.0 (most sensitive) | 1.5x noise floor | Triggers on quieter speech, may pick up background noise |

Formula: `multiplier = 5.0 - vadSensitivity * 3.5`

---

## 6. Confidence Scoring

### 6.1 Token-Level Confidence

whisper.cpp exposes per-token probabilities via `whisper_full_get_token_p()`. The engine computes per-segment confidence by averaging all token probabilities within each segment:

```
segment_confidence = sum(token_probabilities) / token_count
overall_confidence = sum(segment_confidences) / segment_count
```

### 6.2 Unavailable Confidence Handling

When native token probability methods are not linked (`UnsatisfiedLinkError` on JNI, or cinterop failure on iOS), the native layer returns `CONFIDENCE_UNAVAILABLE = -1f` sentinel value. The engine then:

1. Tracks a `hasRealConfidence` flag across segments
2. Reports `avgConfidence = 0f` when no real confidence is available (falls into REJECT level)
3. Logs a one-time WARNING (Android only) — subsequent occurrences are silent

This honest reporting (instead of the previous fake `DEFAULT_CONFIDENCE = 0.85f` which fell at the HIGH threshold) lets the unified ConfidenceScorer system properly classify results.

### 6.3 Unified Confidence Levels (Vivoka/VOSK/Whisper)

All engines use the same ConfidenceScorer thresholds, emitting `confidence_level` in metadata:

| Level | Threshold | UI Action | Color |
|---|---|---|---|
| HIGH | >= 0.85 | Execute immediately | Green |
| MEDIUM | 0.70 - 0.85 | Ask confirmation | Yellow |
| LOW | 0.50 - 0.70 | Show alternatives | Orange |
| REJECT | < 0.50 | Command not recognized | Red |

- **Android**: Uses `ConfidenceScorer.createResult()` with `RecognitionEngine.WHISPER`
- **Desktop/iOS**: Uses inline threshold classification (same constants)
- **Metadata**: `confidence_level` (HIGH/MEDIUM/LOW/REJECT) + `scoring_method` (WHISPER)

### 6.4 TranscriptionResult Fields

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
perf.totalSegments            // Cumulative segment count across all transcriptions
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

### 8.3 Test Coverage

- **Test file**: `Modules/SpeechRecognition/src/commonTest/kotlin/com/augmentalis/speechrecognition/whisper/WhisperPerformanceTest.kt`
- **38 tests** covering:
  - Rolling window behavior (window size, oldest eviction)
  - Peak latency tracking
  - Real-time factor (RTF) calculation
  - Success rate computation
  - Empty transcription counting
  - Total segments accumulation
  - Reset behavior
  - Edge cases (zero duration, empty results, single entry)
- **Run command**: `./gradlew :Modules:SpeechRecognition:desktopTest`
- **All tests** are in commonTest (run on JVM via desktopTest)

---

## 9. Thread Safety

### 9.1 Cross-Platform Synchronization (atomicfu)

whisper.cpp contexts are **NOT thread-safe**. All native calls are serialized through synchronized blocks.

**KMP approach**: `WhisperPerformance` (commonMain) extends `kotlinx.atomicfu.locks.SynchronizedObject` and uses `synchronized(this) { }` blocks. This compiles to:
- **JVM**: `java.util.concurrent` intrinsic monitor locks
- **Native (iOS/macOS)**: `pthread_mutex_lock`/`unlock`
- **JS**: No-op (single-threaded)

Mutable fields use `@Volatile` for visibility across threads without requiring the full lock.

### 9.2 JNI Serialization (Android/Desktop)

All JNI calls are serialized through `synchronized(this)` in `WhisperNative`/`DesktopWhisperNative`.

Overhead: ~50ns per uncontended lock + ~5us JNI crossing. This is <0.001% of whisper inference time (200-2000ms).

### 9.3 cinterop Serialization (iOS)

`IosWhisperNative` uses a `SynchronizedObject` lock with `synchronized(lock) { }` blocks. The entire transcribe+read cycle runs atomically — same pattern as JNI but via K/N cinterop.

Pointer handling: Context pointers are stored as `Long` and converted back to `COpaquePointer` via `kotlinx.cinterop.toCPointer<COpaque>()` for each native call. Null guards prevent crashes on 0L sentinel values.

### 9.4 transcribeToText() Atomicity

The entire transcribe+read cycle runs in one synchronized block:
1. `fullTranscribe()` / `whisper_bridge_transcribe()` — run inference
2. `getTextSegmentCount()` / `whisper_bridge_segment_count()` — read segment count
3. For each segment: text, timestamps, token probabilities
4. `getDetectedLanguage()` / `whisper_bridge_detected_language()` — read language

This prevents interleaving from concurrent callers.

### 9.5 PII-Safe Logging

Transcribed text is **never logged verbatim** in production builds. All log statements use character counts instead of raw text:
- `logInfo(TAG, "Transcribed ${result.text.length} chars ...")` (NOT `"Transcribed: '${result.text}'"`)
- This applies to all platforms (Android, iOS, macOS, Desktop)

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

### 10.4 Using Google Cloud STT v2

```kotlin
// In AndroidSpeechRecognitionService.initialize():
when (config.engine) {
    SpeechEngine.GOOGLE_CLOUD -> {
        val gcConfig = GoogleCloudConfig.fromSpeechConfig(config)
        val engine = GoogleCloudEngine(context)
        engine.initialize(gcConfig)
        // Collect engine.resultFlow -> process -> emit to service resultFlow
    }
}
```

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

### Google Cloud: Authentication Failed (401)

Firebase Auth: Ensure a user is signed in (`FirebaseAuth.getInstance().currentUser != null`). The engine automatically refreshes expired tokens on first 401.

API Key: Verify the key is valid and has Speech-to-Text v2 API enabled in GCP Console.

### Google Cloud: Rate Limited (429)

The engine retries with exponential backoff (1s, 2s, 4s). If persistent, check GCP quotas for your project. Consider switching to STREAMING mode for continuous recognition.

### Google Cloud: Streaming Connection Lost

The streaming client auto-reconnects up to 5 times with exponential backoff (1s → 15s). If all attempts fail, the engine emits a network error. Check WiFi connectivity and Google Cloud service status.

### Google Cloud: Empty Results

Common causes:
- Audio too short (below `minSpeechDurationMs` threshold)
- Wrong language code for the spoken language
- Model mismatch: use `latest_short` for commands (< 60s), `latest_long` for dictation

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

---

## 15. Google Cloud Speech-to-Text v2

### 15.1 Overview

Google Cloud STT v2 is a premium cloud recognition engine with two modes: **VAD_BATCH** (WhisperVAD detects speech chunks, each sent as REST recognize request) and **STREAMING** (continuous HTTP/2 chunked transfer with partial+final results). Requires network. Auth via Firebase ID token (Bearer) or API key (query param).

### 15.2 Architecture

```
SpeechConfig (commonMain)
    ├── gcpProjectId
    ├── gcpRecognizerMode
    └── googleCloud() factory
         │
AndroidSpeechRecognitionService (androidMain)
    └── initializeGoogleCloud()
         │
GoogleCloudEngine (androidMain/googlecloud/)
    ├── WhisperAudio (reused — 16kHz mono PCM)
    ├── WhisperVAD (reused — speech chunk detection)
    ├── GoogleCloudApiClient (batch)
    └── GoogleCloudStreamingClient (streaming)
```

All 4 files in `src/androidMain/kotlin/com/augmentalis/speechrecognition/googlecloud/`:

### 15.3 GoogleCloudConfig

```kotlin
// Configuration for Google Cloud STT v2
data class GoogleCloudConfig(
    val projectId: String,                                    // GCP project ID (required)
    val recognizerName: String = "_",                         // Default recognizer
    val location: String = "global",                          // API location
    val mode: GoogleCloudMode = GoogleCloudMode.VAD_BATCH,    // VAD_BATCH or STREAMING
    val language: String = "en-US",                           // BCP-47 language code
    val model: String = "latest_short",                       // "latest_short" or "latest_long"
    val enableAutoPunctuation: Boolean = true,                // Auto punctuation
    val enableWordTimeOffsets: Boolean = false,               // Word-level timestamps
    val enableWordConfidence: Boolean = false,                // Per-word confidence
    val maxAlternatives: Int = 3,                             // Alternative transcriptions
    val authMode: GoogleCloudAuthMode = GoogleCloudAuthMode.FIREBASE_AUTH,
    val apiKey: String? = null,                               // For API_KEY auth
    val silenceThresholdMs: Int = 700,                        // VAD silence duration
    val minSpeechDurationMs: Int = 300,                       // Min utterance length
    val maxChunkDurationMs: Int = 30_000,                     // Max chunk duration
    val requestTimeoutMs: Long = 30_000,                      // HTTP request timeout
    val connectTimeoutMs: Long = 10_000,                      // Connection timeout
    val maxRetries: Int = 3                                   // Max retry attempts
)

// Mode selection
enum class GoogleCloudMode {
    VAD_BATCH,      // Speech chunks detected locally, each sent as REST request
    STREAMING       // Continuous HTTP/2 streaming with partial results
}

// Authentication mode
enum class GoogleCloudAuthMode {
    FIREBASE_AUTH,  // Uses Firebase ID token (Bearer header)
    API_KEY         // Uses API key (query parameter)
}

// Factory function
fun SpeechConfig.googleCloud(
    projectId: String,
    apiKey: String? = null,
    streaming: Boolean = false,
    language: String = "en-US"
): GoogleCloudConfig = GoogleCloudConfig(
    projectId = projectId,
    mode = if (streaming) GoogleCloudMode.STREAMING else GoogleCloudMode.VAD_BATCH,
    language = language,
    apiKey = apiKey,
    authMode = if (apiKey != null) GoogleCloudAuthMode.API_KEY else GoogleCloudAuthMode.FIREBASE_AUTH
)

// Factory from unified config
fun GoogleCloudConfig.fromSpeechConfig(config: SpeechConfig): GoogleCloudConfig {
    // Extracts GCP-specific fields from unified SpeechConfig
    return GoogleCloudConfig(
        projectId = config.gcpProjectId ?: throw IllegalArgumentException("gcpProjectId required"),
        mode = config.gcpRecognizerMode ?: GoogleCloudMode.VAD_BATCH,
        language = config.language
    )
}

// Validation
fun GoogleCloudConfig.validate(): Result<Unit> {
    if (projectId.isBlank()) return Result.failure(IllegalArgumentException("projectId required"))
    if (requestTimeoutMs < 1000) return Result.failure(IllegalArgumentException("requestTimeoutMs must be >= 1000"))
    if (maxRetries !in 0..10) return Result.failure(IllegalArgumentException("maxRetries must be 0-10"))
    if (silenceThresholdMs !in 100..5000) return Result.failure(IllegalArgumentException("silenceThresholdMs must be 100-5000"))
    return Result.success(Unit)
}

// URL builders (internal)
fun GoogleCloudConfig.buildRecognizeUrl(): String {
    val baseUrl = "https://speech.googleapis.com/v2/projects/$projectId/locations/$location/recognizers/$recognizerName:recognize"
    return if (authMode == GoogleCloudAuthMode.API_KEY && apiKey != null) {
        "$baseUrl?key=$apiKey"
    } else {
        baseUrl
    }
}

fun GoogleCloudConfig.buildStreamingUrl(): String {
    val baseUrl = "https://speech.googleapis.com/v2/projects/$projectId/locations/$location/recognizers/$recognizerName:streamingRecognize"
    return if (authMode == GoogleCloudAuthMode.API_KEY && apiKey != null) {
        "$baseUrl?key=$apiKey"
    } else {
        baseUrl
    }
}
```

**~168 lines** — Configuration data class, enums, factories, validation, URL builders.

### 15.4 GoogleCloudApiClient

```kotlin
// REST client for synchronous recognize endpoint (VAD_BATCH mode)
class GoogleCloudApiClient(
    private val config: GoogleCloudConfig,
    private val firebaseAuth: FirebaseAuth? = null
) {
    // Recognizes a single audio chunk (speech detected by WhisperVAD)
    suspend fun recognize(audioFloatArray: FloatArray): RecognizeResponse {
        // Audio conversion: Float32 → Int16 LE PCM → Base64 encoding
        val pcmData = convertFloatToPcm(audioFloatArray)
        val base64Audio = Base64.getEncoder().encodeToString(pcmData)

        // Build v2 REST request JSON
        val requestJson = buildRecognizeRequest(base64Audio)

        // Auth header
        val headers = buildHeaders()

        try {
            // POST request with retries + exponential backoff
            val response = retryWithBackoff(maxRetries = config.maxRetries) {
                postRequest(config.buildRecognizeUrl(), requestJson, headers)
            }

            // Parse response: transcript, confidence, word timestamps, alternatives
            return parseRecognizeResponse(response)
        } catch (e: HttpException) {
            // Error mapping: 400→CHECK_CONFIGURATION, 401/403→REINITIALIZE, 429→RETRY_WITH_BACKOFF, 5xx→RETRY_WITH_BACKOFF
            return RecognizeResponse.Error(mapHttpError(e))
        }
    }

    // Token refresh on 401
    private suspend fun refreshToken(): Boolean {
        if (config.authMode != GoogleCloudAuthMode.FIREBASE_AUTH || firebaseAuth == null) {
            return false
        }
        return try {
            firebaseAuth.currentUser?.getIdToken(forceRefresh = true)?.result != null
        } catch (e: Exception) {
            false
        }
    }

    // Retry logic: Exponential backoff (1s base, 2x multiplier, 10s cap, 20% jitter)
    private suspend inline fun <T> retryWithBackoff(
        maxRetries: Int,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: HttpException) {
                if (e.code == 401) {
                    if (refreshToken()) {
                        // Retry once after refresh
                        try {
                            return block()
                        } catch (e2: Exception) {
                            lastException = e2
                        }
                    }
                }
                if (attempt < maxRetries - 1) {
                    val backoff = minOf(
                        (1000L shl attempt) + Random.nextLong(200),  // 1s, 2s, 4s, 8s + jitter
                        10_000  // cap at 10s
                    )
                    delay(backoff)
                    lastException = e
                }
            }
        }
        throw lastException ?: Exception("All retries exhausted")
    }

    // Build request JSON (v2 schema)
    private fun buildRecognizeRequest(base64Audio: String): String {
        return """
        {
            "config": {
                "autoDecodingConfig": {},
                "languageCodes": ["${config.language}"],
                "model": "${config.model}",
                "features": {
                    "enableAutoPunctuation": ${config.enableAutoPunctuation},
                    "enableWordTimeOffsets": ${config.enableWordTimeOffsets},
                    "enableWordConfidence": ${config.enableWordConfidence}
                },
                "maxAlternatives": ${config.maxAlternatives}
            },
            "audio": {
                "content": "$base64Audio"
            }
        }
        """.trimIndent()
    }

    // Build request headers
    private suspend fun buildHeaders(): Map<String, String> {
        val headers = mutableMapOf(
            "Content-Type" to "application/json"
        )
        if (config.authMode == GoogleCloudAuthMode.FIREBASE_AUTH && firebaseAuth != null) {
            val token = firebaseAuth.currentUser?.getIdToken(cacheLevel = false)?.result?.token
            if (token != null) {
                headers["Authorization"] = "Bearer $token"
            }
        }
        return headers
    }

    // Parse v2 API response
    private fun parseRecognizeResponse(jsonResponse: String): RecognizeResponse {
        val results = parseJson(jsonResponse)  // Results array
        if (results.isEmpty()) {
            return RecognizeResponse.Success(RecognitionResult(text = "", confidence = 0f))
        }
        // Extract transcript, confidence, alternatives
        val transcript = results[0].transcript
        val confidence = results[0].confidence.toFloat()
        return RecognizeResponse.Success(
            RecognitionResult(text = transcript, confidence = confidence)
        )
    }

    // Response wrapper
    sealed class RecognizeResponse {
        data class Success(val result: RecognitionResult) : RecognizeResponse()
        data class Error(val error: RecognitionError) : RecognizeResponse()
    }
}
```

**~409 lines** — REST client with audio conversion, request building, auth, retry logic, response parsing.

### 15.5 GoogleCloudStreamingClient

```kotlin
// HTTP/2 streaming via OkHttp with chunked transfer encoding
class GoogleCloudStreamingClient(
    private val config: GoogleCloudConfig,
    private val firebaseAuth: FirebaseAuth? = null
) {
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)  // Decouples mic from network
    private var streamJob: Job? = null
    private val resultFlow = MutableSharedFlow<RecognitionResult>()

    // Start streaming recognize session
    suspend fun startStreaming() {
        streamJob = coroutineScope {
            launch {
                try {
                    val client = OkHttpClient.Builder()
                        .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
                        .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
                        .readTimeout(config.requestTimeoutMs, TimeUnit.MILLISECONDS)
                        .build()

                    val request = buildStreamingRequest()
                    val response = client.newCall(request).execute()

                    // Read newline-delimited JSON responses (partial + final results)
                    response.body?.source()?.use { source ->
                        val streamStartTime = System.currentTimeMillis()
                        while (!source.exhausted() && System.currentTimeMillis() - streamStartTime < 290_000) {
                            val line = source.readUtf8Line() ?: break
                            val result = parseStreamingResponse(line)
                            resultFlow.emit(result)
                        }
                    }
                } catch (e: IOException) {
                    // Auto-reconnection: exponential backoff (1s → 15s, 5 max attempts)
                    autoReconnect()
                }
            }

            launch {
                // Continuously drain audio buffer and send chunks
                for (chunk in audioQueue) {
                    sendAudioChunk(chunk)
                }
            }
        }
    }

    // Queue audio chunk for streaming
    fun sendAudioChunk(audioData: ByteArray) {
        audioQueue.trySend(audioData)  // Non-blocking
    }

    // Stop streaming — closes queue for this session
    fun stopStreaming() {
        audioQueue.close()
        streamJob?.cancel()
    }

    // IMPORTANT (260222 fix): audioQueue MUST be rebuilt at the start of each
    // streaming session. Google Cloud STT v2 has a ~5 min stream limit; on
    // reconnect, a new session calls startStreaming() which must create a fresh
    // Channel. Without this, the closed channel silently drops all audio after
    // the first session rotation.
    // Fix: audioQueue = Channel(Channel.UNLIMITED) at top of each session start.

    // Collect results
    fun getResults(): Flow<RecognitionResult> = resultFlow.asSharedFlow()

    // Auto-reconnection with backoff
    private suspend fun autoReconnect() {
        var attempt = 0
        while (attempt < 5) {
            val backoff = minOf(
                1000L shl attempt,  // 1s, 2s, 4s, 8s, 16s
                15_000  // cap at 15s
            )
            delay(backoff)
            try {
                startStreaming()
                return
            } catch (e: Exception) {
                attempt++
            }
        }
    }

    // Build streaming request with config message
    private suspend fun buildStreamingRequest(): Request {
        val configMessage = buildConfigMessage()
        val headers = buildHeaders()

        return Request.Builder()
            .url(config.buildStreamingUrl())
            .post(StreamingRequestBody(configMessage, audioQueue))
            .apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }
            .build()
    }

    // Config message (sent first)
    private fun buildConfigMessage(): String {
        return """
        {
            "streamingConfig": {
                "config": {
                    "autoDecodingConfig": {},
                    "languageCodes": ["${config.language}"],
                    "model": "${config.model}",
                    "features": {
                        "enableAutoPunctuation": ${config.enableAutoPunctuation}
                    }
                }
            }
        }
        """.trimIndent()
    }

    // Build headers with auth
    private suspend fun buildHeaders(): Map<String, String> {
        val headers = mutableMapOf(
            "Content-Type" to "application/json"
        )
        if (config.authMode == GoogleCloudAuthMode.FIREBASE_AUTH && firebaseAuth != null) {
            val token = firebaseAuth.currentUser?.getIdToken(cacheLevel = false)?.result?.token
            if (token != null) {
                headers["Authorization"] = "Bearer $token"
            }
        }
        return headers
    }

    // Parse newline-delimited JSON from stream
    private fun parseStreamingResponse(jsonLine: String): RecognitionResult {
        // Response can be partial (interim results) or final
        val isPartial = jsonLine.contains("\"isFinal\": false")
        val transcript = extractTranscript(jsonLine)
        val confidence = extractConfidence(jsonLine)
        return RecognitionResult(text = transcript, confidence = confidence)
    }

    // Custom request body for streaming
    private class StreamingRequestBody(
        private val configMessage: String,
        private val audioQueue: Channel<ByteArray>
    ) : RequestBody() {
        override fun contentType(): MediaType? = MediaType.get("application/json")

        override fun writeTo(sink: BufferedSink) {
            // Write config message first
            sink.writeUtf8(configMessage + "\n")
            sink.flush()

            // Stream audio chunks
            runBlocking {
                for (chunk in audioQueue) {
                    val audioMessage = buildAudioMessage(chunk)
                    sink.writeUtf8(audioMessage + "\n")
                    sink.flush()
                }
            }
        }

        private fun buildAudioMessage(audioData: ByteArray): String {
            val base64Audio = Base64.getEncoder().encodeToString(audioData)
            return """{"audio": {"content": "$base64Audio"}}"""
        }
    }
}
```

**~448 lines** — HTTP/2 streaming client with audio queue, request building, response parsing, auto-reconnect.

### 15.6 GoogleCloudEngine

```kotlin
// Main engine orchestrator mirroring WhisperEngine pattern
class GoogleCloudEngine(
    private val context: Context
) {
    // State machine: UNINITIALIZED → LOADING_MODEL → READY → LISTENING → PROCESSING → READY
    private val stateFlow = MutableStateFlow<WhisperEngineState>(WhisperEngineState.UNINITIALIZED)

    // Components (reused from Whisper)
    private lateinit var audio: WhisperAudio        // Mic capture
    private lateinit var vad: WhisperVAD            // Speech chunk detection
    private lateinit var apiClient: GoogleCloudApiClient  // Batch REST client
    private lateinit var streamingClient: GoogleCloudStreamingClient  // HTTP/2 streaming
    private lateinit var config: GoogleCloudConfig

    // Results flow
    val resultFlow = MutableSharedFlow<RecognitionResult>()

    // Initialize engine
    suspend fun initialize(gcConfig: GoogleCloudConfig) {
        gcConfig.validate().getOrThrow()
        config = gcConfig

        stateFlow.value = WhisperEngineState.LOADING_MODEL

        // Initialize components
        audio = WhisperAudio(context, 16000)  // 16kHz mono
        vad = WhisperVAD(
            silenceTimeoutMs = config.silenceThresholdMs,
            minSpeechDurationMs = config.minSpeechDurationMs,
            maxSpeechDurationMs = config.maxChunkDurationMs
        )
        apiClient = GoogleCloudApiClient(config, FirebaseAuth.getInstance())
        streamingClient = GoogleCloudStreamingClient(config, FirebaseAuth.getInstance())

        stateFlow.value = WhisperEngineState.READY
    }

    // Start listening
    suspend fun startListening() {
        stateFlow.value = WhisperEngineState.LISTENING
        audio.startCapture()

        when (config.mode) {
            GoogleCloudMode.VAD_BATCH -> vadListenLoop()
            GoogleCloudMode.STREAMING -> streamingListenLoop()
        }
    }

    // VAD Batch mode: WhisperVAD detects chunks, each sent as REST request
    private suspend fun vadListenLoop() {
        audio.captureFlow.collect { floatBuffer ->
            vad.processSample(floatBuffer)

            if (vad.hasSpeechChunk()) {
                stateFlow.value = WhisperEngineState.PROCESSING
                val chunk = vad.getChunkAndReset()

                // Convert Float32 to PCM
                val pcmData = convertFloatToPcm(chunk)

                // Send REST request
                val response = apiClient.recognize(chunk)
                when (response) {
                    is GoogleCloudApiClient.RecognizeResponse.Success -> {
                        resultFlow.emit(response.result)
                    }
                    is GoogleCloudApiClient.RecognizeResponse.Error -> {
                        resultFlow.emit(RecognitionResult(text = "", error = response.error))
                    }
                }

                stateFlow.value = WhisperEngineState.READY
            }
        }
    }

    // Streaming mode: Continuous HTTP/2 streaming
    private suspend fun streamingListenLoop() {
        streamingClient.startStreaming()

        audio.captureFlow.collect { floatBuffer ->
            // Convert Float32 to PCM and queue for streaming
            val pcmData = convertFloatToPcm(floatBuffer)
            streamingClient.sendAudioChunk(pcmData)

            // Collect streaming results
            streamingClient.getResults().collect { result ->
                resultFlow.emit(result)
            }
        }
    }

    // Stop listening (flushes remaining audio in VAD_BATCH)
    suspend fun stopListening() {
        audio.stopCapture()
        streamingClient.stopStreaming()

        // Flush remaining audio in VAD_BATCH mode
        if (config.mode == GoogleCloudMode.VAD_BATCH && vad.hasPartialSpeech()) {
            val remainingChunk = vad.getChunkAndReset()
            if (remainingChunk.isNotEmpty()) {
                val response = apiClient.recognize(remainingChunk)
                when (response) {
                    is GoogleCloudApiClient.RecognizeResponse.Success -> {
                        resultFlow.emit(response.result)
                    }
                    is GoogleCloudApiClient.RecognizeResponse.Error -> {
                        resultFlow.emit(RecognitionResult(text = "", error = response.error))
                    }
                }
            }
        }

        stateFlow.value = WhisperEngineState.READY
    }

    // Pause/resume
    suspend fun pause() {
        audio.stopCapture()
    }

    suspend fun resume() {
        audio.startCapture()
    }

    // Cleanup
    fun destroy() {
        audio.release()
        streamingClient.stopStreaming()
        stateFlow.value = WhisperEngineState.DESTROYED
    }

    // Getters
    fun getState(): Flow<WhisperEngineState> = stateFlow.asStateFlow()
}
```

**~470 lines** — Engine orchestrator with state machine, VAD_BATCH and STREAMING listen loops, lifecycle management.

### 15.7 Usage Examples

**VAD Batch Mode** (default, recommended for voice commands):
```kotlin
val config = SpeechConfig.googleCloud(
    projectId = "my-gcp-project",
    apiKey = "optional-api-key",
    streaming = false
)
speechService.initialize(config)
speechService.startListening()
```

**Streaming Mode** (for continuous/dictation):
```kotlin
val config = SpeechConfig.googleCloud(
    projectId = "my-gcp-project",
    streaming = true
)
speechService.initialize(config)
speechService.startListening()
```

**Firebase Auth** (no API key required, uses logged-in user token):
```kotlin
val config = SpeechConfig.googleCloud(
    projectId = "my-gcp-project"
    // No apiKey → automatically uses FIREBASE_AUTH mode
)
```

**From AndroidSpeechRecognitionService**:
```kotlin
when (config.engine) {
    SpeechEngine.GOOGLE_CLOUD -> {
        val gcConfig = GoogleCloudConfig.fromSpeechConfig(config)
        val engine = GoogleCloudEngine(context)
        engine.initialize(gcConfig)
        // Collect engine.resultFlow → process → emit to service resultFlow
    }
}
```

### 15.8 Provisioning

GCP project ID is provided via:
1. **SpeechConfig.googleCloud(projectId = ...)** at runtime
2. **local.properties**: `gcp.speech.project_id=my-project`
3. **Environment variable**: `GCP_SPEECH_PROJECT_ID=my-project`

Firebase Auth is recommended for production — no API key storage required.

### 15.9 Dependencies

- `com.google.firebase:firebase-auth` — from existing Firebase BOM 34.3.0 (already included)
- `com.squareup.okhttp3:okhttp` — for HTTP/2 streaming (already included)
- `com.google.code.gson:gson` — for JSON parsing (already included)

No new dependencies required beyond existing stack.

### 15.10 Phrase Hints (Adaptation)

Google Cloud STT v2 supports **phrase hints** via the `adaptation.phraseSets` field, which biases the recognition model toward expected phrases. This significantly improves command recognition accuracy.

**Integration**: Both `GoogleCloudApiClient.recognize()` and `GoogleCloudStreamingClient.startStreaming()` accept a `phraseHints: List<String>` parameter. The `GoogleCloudEngine` automatically forwards `commandCache.getAllCommands()` (current screen's voice commands) to both clients.

**Wire format** (inside `config`):
```json
{
  "adaptation": {
    "phraseSets": [{
      "phrases": [
        { "value": "click save", "boost": 10.0 },
        { "value": "scroll down", "boost": 10.0 }
      ]
    }]
  }
}
```

- **Boost value**: 10.0 (strong bias toward expected commands)
- **Cap**: 500 phrases per request (Google API limit)
- **Source**: `CommandCache.getAllCommands()` — includes static + dynamic (UI-scraped) commands

### 15.11 Settings Provider

`GoogleCloudSettingsProvider` implements `ModuleSettingsProvider` for the Unified Adaptive Settings screen.

| Section | Settings |
|---------|----------|
| Project Configuration | GCP Project ID, Location |
| Authentication | Auth mode (API Key / Firebase), API key |
| Recognition | Model (latest_short/latest_long), Streaming toggle, Language |
| Advanced | Punctuation, Profanity filter, Word timestamps |

**DataStore keys**: Prefixed with `gcp_stt_` (e.g., `gcp_stt_project_id`, `gcp_stt_api_key`).

**Build dependency**: `implementation(project(":Modules:Foundation"))` added for `ModuleSettingsProvider` interface.
