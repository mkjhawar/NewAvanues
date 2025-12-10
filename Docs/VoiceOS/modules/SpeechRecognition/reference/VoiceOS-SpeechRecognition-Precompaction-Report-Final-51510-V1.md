<!--
filename: SpeechRecognition-Precompaction-Report-Final.md
created: 2025-01-27 20:00:00 PST
author: Manoj Jhawar
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Final comprehensive precompaction report with complete feature analysis
version: 3.0.0
module: SpeechRecognition
location: /VOS4/docs/modules/speechrecognition/
-->

# SpeechRecognition Module - Final Precompaction Report v3.0

## Executive Summary

This document provides the complete implementation plan for compacting the SpeechRecognition module with 4 engines (VOSK, Vivoka, GoogleSTT, GoogleCloud) using shared components while maintaining 100% functional equivalency. Based on thorough research and analysis, this report identifies ALL features that must be preserved.

## 1. Current State Analysis

### Existing Implementations Found

| Engine | Source Location | Status | Key Features |
|--------|----------------|--------|--------------|
| **VOSK** | `/CodeImport/SR6-Hybrid/VoskSpeechRecognitionService.kt` | ✅ Working | Speaker ID, JSON parsing, multiple recognizers |
| **Vivoka** | `/libraries/SpeechRecognition/engines/vivoka/VivokaService.kt` | ✅ Created | Wake word, VSDK integration |
| **GoogleSTT** | `/CodeImport/Archive/SpeechRecognition/engines/googlestt/` | 📦 Archive | Android native, silence detection |
| **GoogleCloud** | `/CodeImport/Archive/SpeechRecognition/engines/googlecloud/` | 📦 Archive | Streaming, word confidence |

### Shared Components Status

| Component | Location | Status | Current Features |
|-----------|----------|--------|-----------------|
| **CommandCache** | `/common/CommandCache.kt` | ✅ Created | Basic command storage |
| **TimeoutManager** | `/common/TimeoutManager.kt` | ✅ Created | Basic timeout |
| **ResultProcessor** | `/common/ResultProcessor.kt` | ✅ Created | Basic processing |
| **ServiceState** | `/common/ServiceState.kt` | ✅ Created | Basic states |

## 2. Complete Feature Matrix (Research-Based)

### 2.1 VOSK Features (Complete List)

```kotlin
// Core Features
✅ Offline-only operation (no network required)
✅ 20+ language support with downloadable models
✅ Model sizes: 50MB (small) to 2GB+ (large)
✅ Zero-latency streaming API
✅ Large vocabulary continuous transcription

// Recognition Features
✅ Multiple Recognizer instances (command + dictation)
✅ Partial results (10-20 second chunks)
✅ Final results with confidence
✅ JSON result structure with per-word data:
   - confidence (0-1 scale)
   - start time (seconds)
   - end time (seconds)
   - word text

// Advanced Features
✅ Speaker Identification via X-Vector (res['spk'])
✅ Speaker Diarization (compare vectors with cosine distance)
✅ Dynamic vocabulary (runtime reconfiguration for small models)
✅ Model adaptation (acoustic + language model)
✅ Similarity matching with threshold (0.6 default)
✅ Custom confidence scaling (5000-9000 in implementation)

// Implementation Details
✅ StorageService.unpack() for model management
✅ RecognitionListener callbacks
✅ Coroutine-based async operations
✅ Thread-safe command lists
```

### 2.2 Vivoka Features (VSDK 6.0.0)

```kotlin
// Core Features
✅ Hybrid online/offline operation
✅ VSDK 6.0.0 AAR integration
✅ ~60MB memory footprint

// Recognition Features
✅ Wake word detection (built-in)
✅ Custom grammar support
✅ Speaker adaptation
✅ Command-specific recognition
✅ Continuous recognition
✅ Mode switching (Command/Dictation/Free)

// Callbacks
✅ onReady, onSpeechStart, onSpeechEnd
✅ onPartialResult, onFinalResult
✅ onAudioLevel monitoring
✅ Error handling with codes
```

### 2.3 Google STT Features (Android Native)

```kotlin
// Core Features
✅ Android SpeechRecognizer API
✅ Online-only (requires internet)
✅ Device-dependent language support
✅ ~20MB memory footprint

// RecognizerIntent Extras (ALL must be supported)
✅ EXTRA_LANGUAGE_MODEL (FREE_FORM/WEB_SEARCH)
✅ EXTRA_PROMPT (UI prompt display)
✅ EXTRA_MAX_RESULTS (1-10 alternatives)
✅ EXTRA_PARTIAL_RESULTS (enable/disable)
✅ EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS
✅ EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS
✅ EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS

// RecognitionListener Callbacks (ALL required)
✅ onReadyForSpeech(Bundle params)
✅ onBeginningOfSpeech()
✅ onRmsChanged(float rmsdB) - audio level
✅ onBufferReceived(byte[] buffer)
✅ onEndOfSpeech()
✅ onError(int error) - with error codes
✅ onResults(Bundle results)
✅ onPartialResults(Bundle partialResults)
✅ onEvent(int eventType, Bundle params)

// Advanced Features
✅ Silence detection with auto-stop
✅ Dictation timeout handling
✅ Voice activity states (sleep/wake)
✅ Auto-sleep on inactivity
✅ Main thread requirements for SpeechRecognizer
✅ Error recovery with exponential backoff
✅ Android 13+ checkRecognitionSupport()
✅ Locale-based language selection
```

### 2.4 Google Cloud Features

```kotlin
// Core Features
✅ Cloud-based recognition
✅ API key required
✅ 125+ language support
✅ ~15MB memory footprint

// Streaming Features
✅ gRPC streaming (5 minute limit)
✅ 10MB request size limit
✅ Real-time interim results
✅ Flow-based audio streaming

// Recognition Features
✅ maxAlternatives (multiple transcriptions)
✅ Word-level confidence scores
✅ Word timestamps
✅ Language alternatives (4 simultaneous)
✅ Auto language detection
✅ Chirp 2 model support

// Advanced Features
✅ Model adaptation
✅ Speaker diarization
✅ Profanity filtering
✅ Punctuation auto-insertion
✅ Custom vocabulary/phrase hints
✅ Speech contexts
✅ Enhanced noise reduction
✅ Batch transcription
```

## 3. Enhanced Shared Components Design

### 3.1 CommandCache (Enhanced)

```kotlin
class CommandCache {
    // Basic command storage (all engines)
    private val staticCommands: List<String>      // Pre-defined
    private val dynamicCommands: List<String>     // UI-scraped
    private val vocabularyCache: Map<String, Boolean> // LRU cache
    
    // Engine-specific additions
    private val grammarRules: List<String>        // Vivoka grammars
    private val phraseHints: List<String>         // Google Cloud hints
    private val contextPhrases: List<String>      // Google STT context
    
    // Enhanced matching
    fun findMatch(text: String): CommandMatch?
    fun findBestMatch(text: String, threshold: Float): CommandMatch?
    fun getSimilarityScore(text1: String, text2: String): Float
    fun compileGrammar(): String  // For Vivoka
    fun getPhraseHints(): List<String>  // For Google Cloud
}

data class CommandMatch(
    val text: String,
    val confidence: Float,
    val isExactMatch: Boolean,
    val source: CommandSource
)
```

### 3.2 TimeoutManager (Enhanced)

```kotlin
class TimeoutManager {
    // Basic timeouts (all engines)
    fun startTimeout(duration: Long, onTimeout: () -> Unit)
    fun cancelTimeout()
    fun resetTimeout(duration: Long, onTimeout: () -> Unit)
    
    // Silence detection (Google STT)
    fun startSilenceDetection(threshold: Long, onSilence: () -> Unit)
    fun updateSilenceTime(hasSound: Boolean)
    
    // Dictation timeout (Google STT)
    fun startDictationTimeout(duration: Long, onTimeout: () -> Unit)
    
    // Stream timeout (Google Cloud)
    fun startStreamTimeout(duration: Long, onTimeout: () -> Unit)
    
    // Auto-sleep (Google STT)
    fun startAutoSleepTimer(duration: Long, onSleep: () -> Unit)
    
    // Partial result timeout (VOSK)
    fun startPartialTimeout(duration: Long, onPartial: () -> Unit)
}
```

### 3.3 ResultProcessor (Enhanced)

```kotlin
class ResultProcessor {
    // Basic processing
    fun normalizeText(text: String): String
    fun createResult(...): SpeechResult
    fun createErrorResult(...): SpeechResult
    
    // Confidence handling (different scales)
    fun normalizeConfidence(value: Float, scale: ConfidenceScale): Float
    fun extractWordConfidence(json: String): List<WordConfidence>  // VOSK
    fun parseBundle(bundle: Bundle): RecognitionData  // Google STT
    
    // Multiple alternatives (Google engines)
    fun processAlternatives(alternatives: List<String>): List<Alternative>
    
    // Speaker features (VOSK)
    fun extractSpeakerVector(json: String): FloatArray?
    fun compareSpeakers(vector1: FloatArray, vector2: FloatArray): Float
    
    // Timing (VOSK, Google Cloud)
    fun extractWordTimings(data: Any): List<WordTiming>
    
    // Result caching
    fun cacheResult(result: SpeechResult)
    fun getLastResult(): SpeechResult?
}

data class WordConfidence(
    val word: String,
    val confidence: Float,
    val start: Float,
    val end: Float
)
```

### 3.4 ServiceState (Enhanced)

```kotlin
class ServiceState {
    // Basic states (all engines)
    enum class State {
        NOT_INITIALIZED,
        INITIALIZING,
        INITIALIZED,
        LISTENING,
        PROCESSING,
        SLEEPING,      // Google STT
        DESTROYING,
        ERROR
    }
    
    // Voice activity states (Google STT)
    enum class VoiceActivity {
        IDLE,
        ACTIVE,
        SLEEPING,
        WAKE_WORD_LISTENING  // Vivoka
    }
    
    // Stream states (Google Cloud)
    enum class StreamState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        STREAMING,
        CLOSING
    }
    
    // Model states (VOSK)
    enum class ModelState {
        NOT_LOADED,
        DOWNLOADING,
        UNPACKING,
        LOADED,
        ERROR
    }
    
    fun updateState(state: State, message: String? = null)
    fun setVoiceActivity(activity: VoiceActivity)
    fun setStreamState(state: StreamState)
    fun setModelState(state: ModelState)
    
    // Main thread callbacks (Google STT)
    fun notifyOnMainThread(callback: () -> Unit)
}
```

## 4. Implementation Plan

### Phase 1: Enhance Shared Components (Day 1)

1. **Update CommandCache.kt**
   - Add grammar support for Vivoka
   - Add phrase hints for Google Cloud
   - Implement similarity matching
   - Add command confidence boosting

2. **Update TimeoutManager.kt**
   - Add silence detection
   - Add dictation timeout
   - Add stream timeout
   - Add auto-sleep timer

3. **Update ResultProcessor.kt**
   - Add confidence normalization
   - Add speaker vector extraction
   - Add word timing extraction
   - Add Bundle parsing for Google STT

4. **Update ServiceState.kt**
   - Add voice activity states
   - Add stream states
   - Add model states
   - Add main thread handling

### Phase 2: Refactor Engines (Day 2-3)

#### VoskService.kt (Update existing)
```kotlin
class VoskService(context: Context) : RecognitionListener {
    // Use enhanced shared components
    private val commandCache = CommandCache()
    private val timeoutManager = TimeoutManager(scope)
    private val resultProcessor = ResultProcessor()
    private val serviceState = ServiceState()
    
    // VOSK-specific
    private var model: Model? = null
    private var commandRecognizer: Recognizer? = null
    private var dictationRecognizer: Recognizer? = null
    
    // New: Speaker identification
    private var lastSpeakerVector: FloatArray? = null
    
    // Implement ALL features from matrix
}
```

#### GoogleSTTService.kt (New)
```kotlin
class GoogleSTTService(context: Context) : RecognitionListener {
    // Use enhanced shared components
    private val commandCache = CommandCache()
    private val timeoutManager = TimeoutManager(scope)
    private val resultProcessor = ResultProcessor()
    private val serviceState = ServiceState()
    
    // Android-specific
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Implement ALL RecognitionListener callbacks
    // Handle ALL RecognizerIntent extras
    // Implement silence detection
    // Implement voice activity management
}
```

#### GoogleCloudService.kt (New)
```kotlin
class GoogleCloudService(context: Context) {
    // Use enhanced shared components
    private val commandCache = CommandCache()
    private val timeoutManager = TimeoutManager(scope)
    private val resultProcessor = ResultProcessor()
    private val serviceState = ServiceState()
    
    // Cloud-specific
    private var streamingClient: SpeechClient? = null
    private var audioFlow: Flow<ByteArray>? = null
    
    // Implement gRPC streaming
    // Handle multiple alternatives
    // Implement word-level confidence
    // Support phrase hints
}
```

### Phase 3: Integration & Testing (Day 4)

1. **Update SpeechManager**
   - Engine factory pattern
   - Seamless switching
   - Configuration validation

2. **Create test suite**
   - Unit tests per engine
   - Feature parity tests
   - Performance benchmarks

## 5. Code Impact Analysis

### Before Compaction
| Component | Lines per Engine | Total (4 engines) |
|-----------|-----------------|-------------------|
| Command Management | 200 | 800 |
| Timeout Logic | 100 | 400 |
| Result Processing | 250 | 1000 |
| State Management | 80 | 320 |
| Callbacks | 150 | 600 |
| **Total Duplicate** | | **3120 lines** |

### After Compaction
| Component | Shared Lines | Engine-Specific | Total |
|-----------|-------------|-----------------|-------|
| CommandCache | 300 | 0 | 300 |
| TimeoutManager | 200 | 0 | 200 |
| ResultProcessor | 400 | 0 | 400 |
| ServiceState | 150 | 0 | 150 |
| VoskService | 0 | 250 | 250 |
| VivokaService | 0 | 200 | 200 |
| GoogleSTTService | 0 | 300 | 300 |
| GoogleCloudService | 0 | 350 | 350 |
| **Total** | **1050** | **1100** | **2150 lines** |

### Net Reduction
- **Lines saved:** 3120 - 2150 = **970 lines (31% reduction)**
- **Duplicate code eliminated:** 2070 lines
- **Maintenance points:** 4 shared components vs 16 separate implementations
- **Testing surface:** Reduced by 60%

## 6. Risk Analysis & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Feature loss | High | 100% feature matrix compliance |
| Performance degradation | Medium | Benchmark before/after |
| Breaking changes | High | Maintain all method signatures |
| Thread safety issues | Medium | Use concurrent collections |
| Main thread violations | High | Handler for Google STT |
| Memory leaks | Medium | Proper cleanup in destroy() |

## 7. Success Metrics

### Quantitative
- ✅ All 4 engines working with shared components
- ✅ 970+ lines of code removed
- ✅ Zero duplicate implementations
- ✅ All features from matrix preserved
- ✅ Performance metrics maintained or improved
- ✅ Memory usage optimized

### Qualitative
- ✅ Single point of maintenance for common logic
- ✅ Consistent behavior across all engines
- ✅ Easier to add new engines
- ✅ Better testability
- ✅ Cleaner architecture

## 8. Migration Checklist

### Pre-Implementation
- [x] Complete feature research
- [x] Document all features
- [x] Design shared components
- [x] Create implementation plan

### Implementation Phase 1
- [ ] Enhance CommandCache
- [ ] Enhance TimeoutManager
- [ ] Enhance ResultProcessor
- [ ] Enhance ServiceState

### Implementation Phase 2
- [ ] Update VoskService with speaker ID
- [ ] Create GoogleSTTService
- [ ] Create GoogleCloudService
- [ ] Update VivokaService

### Implementation Phase 3
- [ ] Update SpeechManager
- [ ] Create test suite
- [ ] Run benchmarks
- [ ] Verify feature parity

### Post-Implementation
- [ ] Performance testing
- [ ] Memory profiling
- [ ] Documentation update
- [ ] Code review

## 9. Feature Preservation Checklist

### VOSK (100% Required)
- [ ] Multiple recognizers
- [ ] Speaker identification
- [ ] Per-word confidence
- [ ] Word timing
- [ ] JSON parsing
- [ ] Partial results
- [ ] Dynamic vocabulary
- [ ] Similarity matching
- [ ] Model management

### Vivoka (100% Required)
- [ ] Wake word detection
- [ ] Grammar support
- [ ] VSDK integration
- [ ] Hybrid mode
- [ ] Speaker adaptation

### Google STT (100% Required)
- [ ] All RecognizerIntent extras
- [ ] All RecognitionListener callbacks
- [ ] Main thread handling
- [ ] Silence detection
- [ ] Voice activity states
- [ ] Error recovery
- [ ] Android 13+ features

### Google Cloud (100% Required)
- [ ] gRPC streaming
- [ ] Multiple alternatives
- [ ] Word confidence
- [ ] Language detection
- [ ] Phrase hints
- [ ] Stream management

## 10. Next Actions

1. **Approve this plan**
2. **Begin Phase 1:** Enhance shared components
3. **Implement engines:** One by one with full features
4. **Test thoroughly:** Ensure 100% parity
5. **Document:** Update all documentation

---

**Status:** Ready for Implementation
**Confidence:** High - thorough research completed
**Risk:** Low - all features documented
**Timeline:** 4 days estimated

**Author:** Manoj Jhawar
**Date:** 2025-01-27
**Version:** Final v3.0