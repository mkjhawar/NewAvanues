<!--
filename: All-Engines-Feature-Matrix-Complete.md
created: 2025-01-27 19:45:00 PST
author: Manoj Jhawar
purpose: Comprehensive feature comparison for all 4 speech engines with complete Vosk research
module: SpeechRecognition
status: Active
version: 3.0.0
updated: 2025-08-28 01:30:00 PDT
changelog:
- 3.0.0: Added implementation status, updated with actual engine capabilities
- 2.0.0: Complete feature matrix with Vosk research
-->

# Speech Recognition Engines - Complete Feature Matrix v3.0

## Implementation Status (2025-08-28)

| Engine | Status | Location | Features |
|--------|--------|----------|----------|
| **VoskEngine** | ✅ Implemented | `/speechengines/VoskEngine.kt` | Full offline, speaker ID |
| **VivokaEngine** | ✅ Implemented | `/speechengines/VivokaEngine.kt` | Wake word, hybrid mode |
| **GoogleSTTEngine** | ✅ Implemented | `/speechengines/GoogleSTTEngine.kt` | 50+ languages, similarity matching |
| **GoogleCloudEngine** | ✅ Implemented | `/speechengines/GoogleCloudEngine.kt` | Premium features, streaming |

## 🔴 CRITICAL: All Features Must Be Preserved - 100% Functional Equivalency

This matrix identifies ALL features across all 4 engines based on comprehensive implementation.

## Core Capabilities Comparison

| Feature Category | VOSK | Vivoka | Google STT (Android) | Google Cloud |
|-----------------|------|--------|---------------------|--------------|
| **Offline Capable** | ✅ Full offline | ✅ Hybrid | ❌ No | ❌ No |
| **Online Required** | ❌ Never | 🔶 Optional | ✅ Always | ✅ Always |
| **Model Download** | ✅ Required (~50MB) | ✅ Required | ❌ No | ❌ No |
| **API Key Required** | ❌ No | ❌ No | ❌ No | ✅ Yes |
| **Memory Usage** | ~30MB | ~60MB | ~20MB | ~15MB |
| **Languages Supported** | 20+ languages | Multiple | Device dependent | 125+ languages |
| **Platform Support** | All (Android/iOS/Server) | Android/iOS | Android only | All platforms |

## Recognition Features - Detailed

| Feature | VOSK | Vivoka | Google STT | Google Cloud |
|---------|------|--------|------------|--------------|
| **Continuous Recognition** | ✅ Large vocabulary | ✅ Continuous | ✅ Continuous | ✅ Streaming |
| **Partial Results** | ✅ onPartialResult (10-20s chunks) | ✅ Supported | ✅ EXTRA_PARTIAL_RESULTS | ✅ Interim results |
| **Final Results** | ✅ onFinalResult | ✅ Supported | ✅ onResults | ✅ Final transcript |
| **Multiple Alternatives** | ❌ Single result | ❌ No | ✅ EXTRA_MAX_RESULTS | ✅ maxAlternatives |
| **Confidence Scores** | ✅ Per-word (0-1) | ✅ 0-1 scale | ✅ Per result | ✅ Word-level confidence |
| **Zero Latency** | ✅ Streaming API | ❌ Processing time | ❌ Network latency | ❌ Network latency |
| **Response Time** | <100ms local | <200ms | 200-500ms | 200-800ms |

## VOSK-Specific Advanced Features

| Feature | Status | Implementation | Notes |
|---------|--------|---------------|-------|
| **Speaker Identification** | ✅ Supported | X-Vector output ('spk') | Compare vectors using cosine distance |
| **Speaker Diarization** | ✅ Basic | Vector comparison | Label words by speaker |
| **Dynamic Vocabulary** | ✅ Small models | Runtime reconfiguration | Big models are static |
| **Grammar Support** | ✅ Via vocabulary | Custom word lists | Domain-specific recognition |
| **Model Adaptation** | ✅ Acoustic + LM | 1 hour data needed | Compile with 32GB RAM |
| **Keyword Spotting** | 🔶 Limited | Via vocabulary | Requires separate search space |
| **Multiple Recognizers** | ✅ Yes | Command + Dictation | Switch at runtime |
| **Vocabulary Caching** | ✅ Yes | In-memory cache | Fast lookup |
| **Similarity Matching** | ✅ Yes | Threshold 0.6 | Fuzzy command matching |
| **JSON Output** | ✅ Yes | Structured results | Parse confidence/timing |

### VOSK Result JSON Structure
```json
{
  "result": [
    {
      "conf": 0.98,      // Confidence (0-1)
      "start": 0.12,     // Start time in seconds
      "end": 0.54,       // End time in seconds
      "word": "hello"    // Recognized word
    }
  ],
  "text": "hello world",  // Full transcript
  "spk": [...]           // Speaker vector (X-Vector)
}
```

## Language and Model Features

| Feature | VOSK | Vivoka | Google STT | Google Cloud |
|---------|------|--------|------------|--------------|
| **Language Detection** | ❌ Manual selection | ❌ No | ❌ No | ✅ Auto-detect |
| **Multiple Languages** | ❌ One at a time | ❌ One at a time | ❌ One at a time | ✅ 4 simultaneous |
| **Language Format** | "en-US" model files | "en-US" | Locale object | BCP-47 tags |
| **Model Size** | 50MB (small) to 2GB+ | Varies | N/A (cloud) | N/A (cloud) |
| **Model Types** | Small/Big/Dynamic | Single | N/A | Chirp 2, others |
| **Custom Models** | ✅ Can compile | ❌ No | ❌ No | ✅ Model adaptation |
| **Domain Models** | ✅ Medical, Legal, etc | ❌ Generic | ❌ Generic | ✅ Custom domains |

## Mode Support - Complete

| Mode | VOSK | Vivoka | Google STT | Google Cloud |
|------|------|--------|------------|--------------|
| **Command Mode** | ✅ Command recognizer | ✅ Command | ✅ WEB_SEARCH model | ✅ Command phrases |
| **Dictation Mode** | ✅ Dictation recognizer | ✅ Dictation | ✅ FREE_FORM model | ✅ Continuous |
| **Free Speech** | ✅ Same as dictation | ✅ Free | ✅ FREE_FORM | ✅ Default |
| **Mode Switching** | ✅ Runtime switch | ✅ Runtime | ✅ New intent | ✅ Config change |
| **Grammar Mode** | ✅ Via vocabulary | ✅ Grammar file | ❌ No | ✅ Phrase hints |

## Audio Processing Features

| Feature | VOSK | Vivoka | Google STT | Google Cloud |
|---------|------|--------|------------|--------------|
| **Sample Rate** | 8-48kHz (16kHz optimal) | Configurable | Auto-detect | Configurable |
| **Audio Format** | PCM16, WAV | Multiple | PCM16 | Multiple formats |
| **Streaming** | ✅ Real-time | ✅ Yes | ✅ Via mic | ✅ gRPC streaming |
| **File Input** | ✅ WAV files | ✅ Yes | ❌ Mic only | ✅ File upload |
| **VAD** | ✅ Built-in | ✅ Yes | ✅ Auto | ✅ Configurable |
| **Noise Handling** | ✅ Model dependent | ✅ Yes | ✅ Auto | ✅ Enhanced |

## Unique Engine Features - Complete List

### VOSK-Specific (Complete)
```kotlin
// Features to implement:
- StorageService.unpack() for model loading
- Multiple Recognizer instances (command + dictation)
- X-Vector speaker identification (res['spk'])
- Per-word confidence scores (0-1 scale)
- Word timing (start/end timestamps)
- Partial results (10-20 second chunks)
- Dynamic vocabulary for small models
- Vocabulary caching with similarity matching
- JSON result parsing
- Custom confidence scaling (5000-9000 in our impl)
- Model size: 50MB (small) to 2GB+ (large)
- 20+ language support with downloadable models
- Zero-latency streaming API
- Offline-only operation
```

### Vivoka-Specific
```kotlin
// Features to implement:
- Wake word detection (built-in)
- Hybrid online/offline operation
- VSDK 6.0.0 integration
- Custom grammar support
- Speaker adaptation
- Command-specific recognition
- 60MB memory footprint
```

### Google STT-Specific (Android Native)
```kotlin
// Features to implement:
- RecognizerIntent with all extras:
  * EXTRA_LANGUAGE_MODEL (FREE_FORM/WEB_SEARCH)
  * EXTRA_PROMPT (UI prompt display)
  * EXTRA_MAX_RESULTS (1-10 alternatives)
  * EXTRA_PARTIAL_RESULTS (enable/disable)
  * EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS
  * EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS  
  * EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS
- RecognitionListener callbacks:
  * onReadyForSpeech()
  * onBeginningOfSpeech()
  * onRmsChanged(float rmsdB) - audio level
  * onBufferReceived(byte[] buffer)
  * onEndOfSpeech()
  * onError(int error) - error codes
  * onResults(Bundle results)
  * onPartialResults(Bundle partialResults)
  * onEvent(int eventType, Bundle params)
- Silence detection with auto-stop
- Dictation timeout handling
- Voice activity states (sleep/wake)
- Main thread requirements for SpeechRecognizer
- Error recovery with exponential backoff
- Android 13+ checkRecognitionSupport()
- Language availability checking
- Locale-based language selection
```

### Google Cloud-Specific
```kotlin
// Features to implement:
- gRPC streaming (5 minute limit)
- 10MB request size limit
- maxAlternatives (multiple transcriptions)
- Word-level confidence scores
- Word timestamps
- Language alternatives (4 languages)
- Auto language detection
- Chirp 2 model support
- Model adaptation
- Speaker diarization
- Profanity filtering
- Punctuation auto-insertion
- Flow-based audio streaming
- Batch transcription
- Long audio support
- Custom vocabulary/phrase hints
- Speech contexts
- Audio channel selection
- Enhanced noise reduction
```

## Callback/Event Mapping

| Event Type | VOSK | Vivoka | Google STT | Google Cloud |
|------------|------|--------|------------|--------------|
| **Ready** | After model load | onReady | onReadyForSpeech | Stream ready |
| **Start Speaking** | - | onSpeechStart | onBeginningOfSpeech | - |
| **Audio Level** | - | onAudioLevel | onRmsChanged(float) | - |
| **Buffer** | - | - | onBufferReceived | Stream chunks |
| **Partial Result** | onPartialResult(String) | onPartialResult | onPartialResults(Bundle) | Interim results |
| **Final Result** | onResult/onFinalResult | onFinalResult | onResults(Bundle) | Final transcript |
| **End Speech** | - | onSpeechEnd | onEndOfSpeech | End of audio |
| **Error** | onError(Exception) | onError(code) | onError(int) | gRPC errors |
| **Timeout** | onTimeout() | onTimeout | Via silence detection | Stream timeout |
| **Event** | - | - | onEvent(int, Bundle) | - |

## Configuration Requirements

| Config Item | VOSK | Vivoka | Google STT | Google Cloud |
|------------|------|--------|------------|--------------|
| **Language** | Model file path | "en-US" string | Locale object | BCP-47 tag |
| **Timeout** | Manual coroutine Job | Config parameter | Intent extras (ms) | Stream timeout |
| **Vocabulary** | List<String> + cache | Grammar file | - | Phrase hints list |
| **Audio Format** | 16kHz PCM default | Configurable | Auto-detect | Configurable |
| **Network** | Not needed | Check if online | Required check | Required |
| **Confidence** | 0-1 per word | 0-1 overall | 0-10000 scale | 0-1 word level |
| **Model Path** | Required | Optional | N/A | N/A |
| **API Key** | Not needed | Not needed | Not needed | Required |

## Shared Component Requirements (Updated)

### CommandCache (Must Support)
- Static commands list (all engines)
- Dynamic commands list (all engines)
- Vocabulary caching with LRU (VOSK)
- Similarity matching with threshold (VOSK)
- Grammar compilation (Vivoka)
- Phrase hints for context (Google Cloud)
- Command confidence boosting

### TimeoutManager (Must Support)
- Basic timeout (all engines)
- Silence detection timeout (Google STT)
- Dictation timeout (Google STT)
- Stream timeout (Google Cloud)
- Auto-sleep timeout (Google STT)
- Partial result timeout (VOSK)
- Recognition timeout with cancel

### ResultProcessor (Must Support)
- Confidence normalization (different scales)
- Multiple alternatives handling (Google engines)
- Word-level confidence (VOSK, Google Cloud)
- Word timing extraction (VOSK)
- Speaker vector extraction (VOSK)
- Partial vs final results
- JSON parsing (VOSK)
- Bundle parsing (Google STT)
- Error result creation
- Result caching

### ServiceState (Must Support)
- Common states (all engines)
- Sleep/wake states (Google STT)
- Voice activity states (Google STT)
- Stream states (Google Cloud)
- Speaker states (VOSK)
- Model loading states (VOSK)
- Network states (Google engines)
- Main thread callbacks (Google STT)

## Implementation Checklist

### VOSK Implementation Requirements
- [ ] StorageService.unpack() for model management
- [ ] Multiple Recognizer instances
- [ ] Speaker identification via X-Vector
- [ ] Per-word confidence extraction
- [ ] Word timing extraction
- [ ] JSON result parsing
- [ ] Partial result handling (10-20s)
- [ ] Dynamic vocabulary (small models)
- [ ] Similarity matching (0.6 threshold)
- [ ] Model size validation
- [ ] Language model selection

### Google STT Requirements
- [ ] All RecognizerIntent extras
- [ ] All RecognitionListener callbacks
- [ ] Main thread handling
- [ ] Silence detection
- [ ] Error recovery
- [ ] Android 13+ features
- [ ] Locale handling

### Google Cloud Requirements
- [ ] gRPC streaming setup
- [ ] Multiple alternatives
- [ ] Word-level confidence
- [ ] Language detection
- [ ] Phrase hints
- [ ] Error handling

### Vivoka Requirements
- [ ] Wake word detection
- [ ] Grammar support
- [ ] VSDK integration
- [ ] Hybrid mode

---

**CRITICAL:** This is the COMPLETE feature matrix. Missing ANY feature = implementation failure.
**NOTE:** Shared components must be flexible enough to support ALL these features.