# SpeechRecognition - Google Cloud STT v2 Implementation

**Date**: 2026-02-21
**Branch**: VoiceOS-1M-SpeechEngine
**Module**: Modules/SpeechRecognition
**Type**: Feature Implementation

---

## Summary

Added Google Cloud Speech-to-Text v2 as a premium cloud recognition engine in the SpeechRecognition KMP module. The implementation supports both VAD-based batch mode (using existing WhisperVAD for speech chunking) and HTTP/2 streaming mode for continuous real-time recognition.

## Architecture

```
SpeechConfig (commonMain)
    ├── gcpProjectId (new field)
    ├── gcpRecognizerMode (new field)
    └── googleCloud() factory (updated)
         │
         ▼
AndroidSpeechRecognitionService (androidMain)
    └── initializeGoogleCloud()
         │
         ▼
GoogleCloudEngine (androidMain/googlecloud/)
    ├── WhisperAudio (reused — 16kHz mono PCM capture)
    ├── WhisperVAD (reused — speech chunk detection for batch mode)
    ├── GoogleCloudApiClient (batch recognize)
    └── GoogleCloudStreamingClient (streaming recognize)
```

## New Files (4 files, ~700 lines)

All in `src/androidMain/kotlin/com/augmentalis/speechrecognition/googlecloud/`:

### 1. GoogleCloudConfig.kt (~150 lines)
- Configuration data class with all GCP STT v2 parameters
- `GoogleCloudMode` enum: VAD_BATCH, STREAMING
- `GoogleCloudAuthMode` enum: FIREBASE_AUTH, API_KEY
- `fromSpeechConfig()` factory — extracts fields from unified SpeechConfig
- `validate()` — checks projectId, auth mode, timeouts
- `buildRecognizeUrl()` / `buildStreamingUrl()` — v2 REST endpoint URLs

### 2. GoogleCloudApiClient.kt (~290 lines)
- REST client for synchronous `recognize` endpoint (VAD_BATCH mode)
- URL: `POST .../v2/projects/{PROJECT}/locations/global/recognizers/_:recognize`
- Auth: Firebase ID token (Bearer header) or API key (query param)
- Audio: Float32 → Int16 LE PCM → Base64 encoding
- Response parsing: transcript, confidence, word timestamps, alternatives
- Error mapping: HTTP status → SpeechError with appropriate recovery actions
- Retry: Exponential backoff (1s base, 2x multiplier, 10s cap, 20% jitter, 3 attempts)
- Token refresh: On 401, calls `getIdToken(forceRefresh=true)` and retries once

### 3. GoogleCloudStreamingClient.kt (~310 lines)
- HTTP/2 streaming client for `streamingRecognize` endpoint (STREAMING mode)
- OkHttp with chunked transfer encoding
- Audio queue via `Channel<ByteArray>(UNLIMITED)` — decouples mic from network
- Sends config message first, then streams audio chunks continuously
- Reads newline-delimited JSON responses (partial + final results)
- Auto-reconnection with exponential backoff (max 5 attempts)
- Stream duration limit: 290s (~4:50, within Google's 5-min limit)
- Results emitted via SharedFlow for engine collection

### 4. GoogleCloudEngine.kt (~280 lines)
- Main engine orchestrator mirroring WhisperEngine pattern
- State machine: UNINITIALIZED → LOADING_MODEL → READY → LISTENING → PROCESSING → READY
- Two listen loops selected by `config.mode`:
  - `vadListenLoop()` — WhisperVAD chunks → apiClient.recognize() → emit result
  - `streamingListenLoop()` — continuous audio → streamingClient.sendAudioChunk() → collect results
- Reuses: WhisperAudio, WhisperVAD, CommandCache, VoiceStateManager
- Full lifecycle: initialize/startListening/stopListening/pause/resume/destroy

## Modified Files (3 files, ~50 lines changed)

### 5. SpeechConfig.kt (commonMain)
- Added `gcpProjectId: String?` and `gcpRecognizerMode: String?` fields
- Updated `googleCloud()` factory: now takes `projectId` (required) and `streaming` params
- Updated `validate()`: requires `gcpProjectId` for GOOGLE_CLOUD engine
- Added fluent methods: `withProjectId()`, `withStreamingMode()`

### 6. AndroidSpeechRecognitionService.kt (androidMain)
- Added `googleCloudEngine: GoogleCloudEngine?` field
- Replaced GOOGLE_CLOUD stub with `initializeGoogleCloud(context, config)`
- Added `initializeGoogleCloud()` method (follows `initializeWhisper()` pattern)
- Updated 7 when branches: startListening, stopListening, pause, resume, setCommands, setMode, isListening
- Updated `release()` to destroy Google Cloud engine

### 7. build.gradle.kts
- Added `implementation("com.google.firebase:firebase-auth")` (uses existing BOM 34.3.0)
- Added `GCP_SPEECH_PROJECT_ID` BuildConfig field from `local.properties` or env var

## Dependencies Added
- `com.google.firebase:firebase-auth` — version from existing Firebase BOM 34.3.0

## API Key / Project ID Provisioning

Via `local.properties`:
```properties
gcp.speech.project_id=your-project-id-here
```

Or environment variable: `GCP_SPEECH_PROJECT_ID`

Firebase Auth is recommended over API key for production use.

## Build Verification

```
./gradlew :Modules:SpeechRecognition:compileDebugKotlinAndroid
```

All 4 new files + 3 modified files compile cleanly.

### Pre-Existing Error Fixes (resolved same session)

**AndroidSTTEngine.kt:471** — `recognizer.destroy()` is `suspend fun` but was called inside `mainHandler.post { }` (non-suspend Runnable). Root cause: `AndroidSTTRecognizer.destroy()` calls `stopListening()` (suspend) then `withContext(Dispatchers.Main) { cleanup() }`. Fix: replaced `mainHandler.post` with `CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).launch` — standalone scope can invoke suspend functions and is independent of the engine scope (which is cancelled immediately after).

**WhisperModelDownloadScreen.kt** — 13 unresolved references to `onBackground`/`onSurface`. Root cause: (1) Missing AvanueUI dependency in build.gradle.kts, (2) Material3-style color names used instead of AvanueUI semantic tokens. Fix: Added `implementation(project(":Modules:AvanueUI"))` dependency. Mapped `onBackground` → `textPrimary`, `onSurface` → `textPrimary`, `onBackground.copy(0.6f)` → `textSecondary`, `onSurface.copy(0.6f)` → `textSecondary`, `onSurface.copy(0.5f)` → `textTertiary`.

Full build passes with zero errors after all fixes.

## Usage

### VAD Batch Mode (default)
```kotlin
val config = SpeechConfig.googleCloud(
    projectId = "my-gcp-project",
    apiKey = "optional-api-key"
)
speechService.initialize(config)
speechService.startListening()
// WhisperVAD detects speech chunks → each sent as batch request → result emitted
```

### Streaming Mode
```kotlin
val config = SpeechConfig.googleCloud(
    projectId = "my-gcp-project",
    streaming = true
)
speechService.initialize(config)
speechService.startListening()
// Continuous audio stream → partial + final results emitted in real-time
```

### Firebase Auth (no API key)
```kotlin
val config = SpeechConfig.googleCloud(
    projectId = "my-gcp-project"
    // No apiKey → automatically uses FIREBASE_AUTH mode
)
```
