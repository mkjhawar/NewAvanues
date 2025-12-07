# SpeechRecognition Module - Developer Manual

**Last Updated**: 2025-10-23 21:19 PDT
**Version**: 2.0.0 (SOLID Refactored)
**Module**: SpeechRecognition Library
**Package**: `com.augmentalis.speechrecognition`, `com.augmentalis.voiceos.speech`

---

## Quick Links
- [User Manual](./user-manual.md)
- [API Reference](./reference/api/)
- [Module Changelog](./changelog/CHANGELOG.md)
- [Architecture Documentation](./architecture/)
- [VOS4 Documentation Standards](/Volumes/M Drive/Coding/vos4/docs/templates/standards/NAMING-CONVENTIONS.md)

---

## Table of Contents

1. [Module Overview](#module-overview)
2. [Architecture](#architecture)
3. [Speech Engine Implementations](#speech-engine-implementations)
   - [VoskEngine](#voskengine)
   - [VivokaEngine](#vivokaengine)
   - [AndroidSTTEngine](#androidsttengine)
   - [WhisperEngine](#whisperengine)
   - [GoogleCloudEngine](#googlecloudengine)
4. [Core Components](#core-components)
5. [Function-by-Function Reference](#function-by-function-reference)
6. [Adding New Engines](#adding-new-engines)
7. [Performance Considerations](#performance-considerations)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

---

## Module Overview

The SpeechRecognition module provides a unified interface for multiple speech recognition engines in VOS4. It follows SOLID principles with specialized components for each engine while sharing common functionality.

### Key Features
- **Multi-Engine Support**: VOSK, Vivoka, Android STT, Whisper, Google Cloud
- **Offline Capability**: VOSK, Vivoka, Whisper work without internet
- **Advanced Features**: Language detection, translation, word timestamps (Whisper)
- **Learning System**: Fuzzy matching with persistent command learning
- **Performance Monitoring**: Built-in metrics and optimization
- **Error Recovery**: Automatic retry and graceful degradation

### Module Statistics
- **Engines**: 5 (VOSK, Vivoka, Android STT, Whisper, Google Cloud)
- **Components**: ~80 Kotlin files
- **Lines of Code**: ~15,000 (refactored from ~25,000)
- **Memory Usage**: 15-230MB depending on engine
- **Languages Supported**: 100+ (engine-dependent)

---

## Architecture

### SOLID Component Design

Each engine follows SOLID principles with specialized components:

```
SpeechRecognition Module
│
├── api/                          # Public API
│   ├── RecognitionResult.kt      # Result data model
│   ├── SpeechListeners.kt        # Callback types
│   └── TTSIntegration.kt         # Text-to-speech
│
├── engines/                      # Engine implementations
│   ├── common/                   # Shared components (DRY principle)
│   │   ├── ServiceState.kt       # State management
│   │   ├── CommandCache.kt       # Command caching
│   │   ├── PerformanceMonitor.kt # Performance tracking
│   │   ├── ErrorRecoveryManager.kt # Error recovery
│   │   ├── ResultProcessor.kt    # Result processing
│   │   ├── LearningSystem.kt     # Learning capabilities
│   │   ├── VoiceStateManager.kt  # Voice state
│   │   ├── AudioStateManager.kt  # Audio state
│   │   ├── TimeoutManager.kt     # Timeout handling
│   │   └── UniversalInitializationManager.kt # Thread-safe init
│   │
│   ├── vosk/                     # VOSK engine (6 components)
│   │   ├── VoskEngine.kt         # Main orchestrator
│   │   ├── VoskConfig.kt         # Configuration
│   │   ├── VoskModel.kt          # Model management
│   │   ├── VoskRecognizer.kt     # Recognition logic
│   │   ├── VoskGrammar.kt        # Grammar handling
│   │   ├── VoskStorage.kt        # Persistence
│   │   └── VoskErrorHandler.kt   # Error handling
│   │
│   ├── vivoka/                   # Vivoka VSDK engine (10 components)
│   │   ├── VivokaEngine.kt       # Main orchestrator
│   │   ├── VivokaConfig.kt       # Configuration
│   │   ├── VivokaModel.kt        # Model management
│   │   ├── VivokaRecognizer.kt   # Recognition logic
│   │   ├── VivokaAudio.kt        # Audio pipeline
│   │   ├── VivokaLearning.kt     # Learning system
│   │   ├── VivokaPerformance.kt  # Performance
│   │   ├── VivokaAssets.kt       # Asset management
│   │   ├── VivokaState.kt        # State tracking
│   │   └── VivokaErrorMapper.kt  # Error mapping
│   │
│   ├── android/                  # Android STT engine (7 components)
│   │   ├── AndroidSTTEngine.kt   # Main orchestrator
│   │   ├── AndroidConfig.kt      # Configuration
│   │   ├── AndroidLanguage.kt    # Language mapping
│   │   ├── AndroidIntent.kt      # Intent creation
│   │   ├── AndroidListener.kt    # Recognition listener
│   │   ├── AndroidRecognizer.kt  # SpeechRecognizer wrapper
│   │   └── AndroidErrorHandler.kt # Error handling
│   │
│   ├── whisper/                  # Whisper engine (6 components)
│   │   ├── WhisperEngine.kt      # Main orchestrator
│   │   ├── WhisperConfig.kt      # Configuration
│   │   ├── WhisperModel.kt       # Model management
│   │   ├── WhisperNative.kt      # Native integration
│   │   ├── WhisperProcessor.kt   # Audio processing
│   │   └── WhisperErrorHandler.kt # Error handling
│   │
│   └── google/                   # Google Cloud engine
│       ├── GoogleCloudEngine.kt  # Main orchestrator
│       ├── GoogleConfig.kt       # Configuration
│       ├── GoogleAuth.kt         # Authentication
│       ├── GoogleStreaming.kt    # Streaming API
│       └── GoogleErrorHandler.kt # Error handling
│
├── utils/                        # Utilities
│   └── SimilarityMatcher.kt      # Fuzzy matching
│
├── confidence/                   # Confidence scoring
│   └── ConfidenceScorer.kt       # Confidence levels
│
├── commands/                     # Command management
│   └── StaticCommands.kt         # Static command sets
│
└── SpeechConfiguration.kt        # Main configuration

```

### Component Responsibilities

#### Shared Components (engines/common/)

**ServiceState.kt** - State Management
- Tracks engine lifecycle: UNINITIALIZED → INITIALIZING → INITIALIZED → LISTENING → PROCESSING → SLEEPING → SHUTDOWN
- Thread-safe state transitions
- State change callbacks

**CommandCache.kt** - Command Caching
- Stores static and dynamic commands
- Provides fast lookup
- Supports fuzzy matching

**PerformanceMonitor.kt** - Performance Tracking
- Records recognition latency
- Tracks success/failure rates
- Memory usage monitoring
- Identifies bottlenecks

**ErrorRecoveryManager.kt** - Error Recovery
- Automatic retry logic
- Exponential backoff
- Graceful degradation
- Memory pressure detection

**ResultProcessor.kt** - Result Processing
- Normalizes results across engines
- Applies confidence thresholds
- Provides alternative suggestions

**LearningSystem.kt** - Learning Capabilities
- Fuzzy command matching
- Persistent command learning
- Vocabulary caching
- Integration with Room database

**VoiceStateManager.kt** - Voice State
- Voice enabled/disabled state
- Sleep/wake functionality
- Dictation mode tracking
- Timeout management

**UniversalInitializationManager.kt** - Thread-Safe Initialization
- Prevents race conditions
- Retry mechanism
- Degraded mode support
- Single initialization guarantee

---

## Speech Engine Implementations

### VoskEngine

**Type**: Offline speech recognition
**Technology**: VOSK API (Kaldi-based)
**Memory**: ~30MB
**Languages**: 20+ offline models available

#### Architecture (6 Components)

```kotlin
VoskEngine (main orchestrator)
  ├── VoskConfig (configuration management)
  ├── VoskModel (model loading and validation)
  ├── VoskRecognizer (recognition logic - command/dictation modes)
  ├── VoskGrammar (grammar generation and vocabulary testing)
  ├── VoskStorage (persistent storage - learned commands)
  └── VoskErrorHandler (error handling and recovery)
```

#### Initialization

```kotlin
suspend fun initialize(config: SpeechConfig): Boolean
```

**Process**:
1. Initialize configuration
2. Load and validate model
3. Initialize grammar system
4. Create recognizers (command + dictation)
5. Start audio pipeline
6. Enable voice if configured

**Thread Safety**: Uses UniversalInitializationManager for race-condition prevention

**Error Handling**:
- Max 3 retries with exponential backoff
- Detailed error logging
- Automatic recovery attempts

#### Key Functions

**setContextPhrases(phrases: List<String>)**
- Updates dynamic command vocabulary
- Rebuilds grammar JSON
- Pre-tests vocabulary against model
- Thread-safe command storage

```kotlin
fun setContextPhrases(phrases: List<String>) {
    registeredCommands = phrases.map { it.lowercase().trim() }
    commandCache.setDynamicCommands(registeredCommands)

    if (!voiceStateManager.isVoiceSleeping()) {
        updateGrammar()  // Async grammar rebuild
    }
}
```

**setStaticCommands(commands: List<String>)**
- Sets static command vocabulary
- Pre-tests vocabulary availability
- Registers with learning system

**changeMode(mode: SpeechMode)**
- Switches between DICTATION and DYNAMIC_COMMAND modes
- Updates recognizer configuration
- Manages voice state transitions

```kotlin
fun changeMode(mode: SpeechMode) {
    when (mode) {
        SpeechMode.DICTATION -> {
            voiceStateManager.enterDictationMode()
            voskRecognizer.switchToDictationMode()
        }
        SpeechMode.DYNAMIC_COMMAND -> {
            voiceStateManager.exitDictationMode()
            voskRecognizer.switchToCommandMode()
        }
    }
}
```

**startListening() / stopListening()**
- Manages recognition lifecycle
- Handles timeout management
- Updates state appropriately

#### Recognition Flow

```
1. User speaks → VOSK processes audio
2. RecognitionListener.onResult(hypothesis) called
3. Parse JSON: { "text": "recognized command" }
4. Process through command matching pipeline:
   - Exact match in registered commands?
   - Learned command from storage?
   - Fuzzy match using SimilarityMatcher (threshold 0.70)?
   - Legacy cache-based match?
5. Create RecognitionResult with confidence scoring
6. Invoke result listener
7. Auto-learn successful fuzzy matches
```

#### Command Matching Strategy

**Tier 1: Exact Match** (confidence: 0.95)
```kotlin
if (registeredCommands.contains(command)) {
    return CommandMatchResult(command, 0.95f, ConfidenceLevel.HIGH, "EXACT")
}
```

**Tier 2: Learned Commands** (confidence: 0.90)
```kotlin
val learnedMatch = voskStorage.getLearnedCommand(command)
if (learnedMatch != null) {
    return CommandMatchResult(learnedMatch, 0.90f, ConfidenceLevel.HIGH, "LEARNED")
}
```

**Tier 3: Fuzzy Matching** (confidence: varies 0.70-0.99)
```kotlin
val fuzzyMatch = SimilarityMatcher.findMostSimilarWithConfidence(
    input = command,
    commands = registeredCommands,
    threshold = 0.70f
)
if (fuzzyMatch != null) {
    val (matched, similarity) = fuzzyMatch
    val level = confidenceScorer.getConfidenceLevel(similarity)
    voskStorage.saveLearnedCommand(command, matched)  // Auto-learn
    return CommandMatchResult(matched, similarity, level, "FUZZY")
}
```

**Tier 4: Cache Fallback** (confidence: varies)
```kotlin
val cacheMatch = commandCache.findMatch(command)
```

**Tier 5: No Match** (confidence: 0.40, level: REJECT)

#### Performance Considerations

**Optimization Tips**:
- Use smaller model for faster recognition (vosk-model-small-en-us-0.15)
- Enable grammar constraints for command mode
- Limit dynamic commands to <500 for best performance
- Pre-test vocabulary before adding commands

**Memory Management**:
- Model loaded once and reused
- Grammar rebuilt only when commands change
- Learned commands cached in memory + persisted

**Best Practices**:
```kotlin
// ✅ Good - commands updated in batch
engine.setContextPhrases(allCommands)

// ❌ Bad - multiple updates
allCommands.forEach { engine.setContextPhrases(listOf(it)) }
```

---

### VivokaEngine

**Type**: Hybrid offline/online speech recognition
**Technology**: Vivoka VSDK
**Memory**: ~60MB
**Languages**: 40+ with downloadable models

#### Architecture (10 Components)

```kotlin
VivokaEngine (main orchestrator)
  ├── VivokaConfig (configuration with SharedPreferences)
  ├── VivokaModel (dynamic model compilation)
  ├── VivokaRecognizer (result processing with ConfidenceScorer)
  ├── VivokaAudio (audio pipeline and silence detection)
  ├── VivokaLearning (Room-based learning system)
  ├── VivokaPerformance (metrics and optimization)
  ├── VivokaAssets (asset extraction and validation)
  ├── VivokaState (state tracking)
  ├── VivokaErrorMapper (VSDK error code mapping)
  └── VivokaInitializationManager (thread-safe VSDK init)
```

#### Initialization

```kotlin
suspend fun initialize(speechConfig: SpeechConfig): Boolean
```

**Process**:
1. Initialize performance monitoring
2. Initialize state management
3. Initialize configuration
4. Extract and validate assets (CRITICAL before VSDK init)
5. Download language models if needed (Firebase integration)
6. Initialize VSDK with VivokaInitializationManager
7. Create recognizer instance
8. Initialize dynamic model with language
9. Initialize audio pipeline
10. Compile initial models with default commands
11. Start audio pipeline
12. Enable voice if configured

**Critical Fix**: Assets MUST be extracted and validated BEFORE calling `Vsdk.init()`. The VivokaInitializationManager prevents "Cannot call 'Vsdk.init' multiple times" errors.

#### Key Functions

**setDynamicCommands(commands: List<String>)**
- Registers commands with model compiler
- Compiles dynamic ASR model
- Updates learning system
- Only compiles if not sleeping

```kotlin
fun setDynamicCommands(commands: List<String>) {
    registeredCommands.clear()
    registeredCommands.addAll(commands)

    model.registerCommands(commands)
    learning.registerCommands(commands)

    if (!voiceStateManager.isVoiceSleeping() && voiceStateManager.isInitialized()) {
        model.compileModelWithCommands(registeredCommands)
    }
}
```

**IRecognizerListener Implementation**

```kotlin
override fun onResult(resultType: RecognizerResultType?, result: String?, isFinal: Boolean)
```

Handles:
- Recognition events (silence, speech detection)
- Result processing (commands, dictation)
- Mode switches (command ↔ dictation)
- Special commands (mute, unmute, dictation start/stop)

**Result Processing Pipeline**:
1. VivokaRecognizer.processRecognitionResult() analyzes result
2. Returns RecognitionProcessingResult with action:
   - REGULAR_COMMAND → handleRegularCommand()
   - MUTE_COMMAND → handleMuteCommand()
   - UNMUTE_COMMAND → handleUnmuteCommand()
   - DICTATION_START → handleDictationStart()
   - DICTATION_END → handleDictationEnd()
3. Learning system enhances commands
4. Performance metrics recorded
5. Result listener invoked

#### Mode Switching

**Command → Dictation**:
```kotlin
handleModeSwitch("FREE_SPEECH_START", dictationModelPath)
  → model.switchToDictationModel(dictationModelPath)
  → voiceStateManager.enterDictationMode()
  → audio.startSilenceDetection(timeout)
```

**Dictation → Command** (CRITICAL FIX for continuous recognition):
```kotlin
handleModeSwitch("STOP_FREE_SPEECH", commandModelPath)
  → model.switchToCommandModel(commandModelPath)
  → voiceStateManager.exitDictationMode()
  → audio.stopSilenceDetection()
```

#### Performance Considerations

**Model Compilation**:
- Compile happens asynchronously
- Limit commands to <1000 for best performance
- Use vocabular filtering for large command sets

**Audio Pipeline**:
- Silence detection for dictation timeout
- Voice activity detection (VAD) built-in
- Continuous recording mode

**Memory Management**:
- Models compiled on-demand
- Learning data synced every 5 minutes
- Memory pressure monitoring

---

### AndroidSTTEngine

**Type**: Online speech recognition
**Technology**: Android SpeechRecognizer API
**Memory**: ~20MB
**Languages**: Depends on Google services (100+)

#### Architecture (7 Components)

```kotlin
AndroidSTTEngine (main orchestrator)
  ├── AndroidConfig (configuration management)
  ├── AndroidLanguage (BCP-47 language mapping)
  ├── AndroidIntent (RecognizerIntent creation)
  ├── AndroidListener (RecognitionListener implementation)
  ├── AndroidRecognizer (SpeechRecognizer wrapper)
  ├── AndroidErrorHandler (error recovery strategies)
  └── Shared components (from engines/common)
```

#### Initialization

```kotlin
suspend fun initialize(context: Context, config: SpeechConfig): Boolean
```

**Process**:
1. Initialize configuration
2. Initialize language mapping
3. Create error handler
4. Create and initialize recognizer
5. Setup listener callbacks
6. Initialize learning system
7. Load learned commands and vocabulary cache

**Thread Safety**: Uses UniversalInitializationManager with shorter timeouts (15s) and degraded mode support

#### Key Functions

**startListening(mode: SpeechMode): Boolean**
```kotlin
fun startListening(mode: SpeechMode): Boolean {
    if (!isServiceInitialized) return false

    scope.launch {
        val language = androidLanguage.getCurrentBcpTag()
        androidRecognizer.startListening(mode, language)
    }
    return true
}
```

**setContextPhrases(phrases: List<String>)**
- Updates CommandCache
- Maintains legacy command lists for wake/sleep
- Updates current registered commands if not sleeping

```kotlin
fun setContextPhrases(phrases: List<String>) {
    commandCache.setDynamicCommands(phrases.filter { it.isNotBlank() })

    registeredCommands.clear()
    registeredCommands.addAll(phrases.map { it.lowercase().trim() })

    if (!androidConfig.isVoiceSleeping()) {
        currentRegisteredCommands.clear()
        currentRegisteredCommands.addAll(registeredCommands)
    }
}
```

**changeMode(mode: SpeechMode): Boolean**
- Switches between FREE_SPEECH and DYNAMIC_COMMAND
- Updates silence checking for dictation
- Restarts recognizer with new mode

#### Recognition Flow

```
1. AndroidListener.onResults(results: Bundle) called
2. Extract first result string
3. Process through handleCommandProcessing():
   - Check if dictation mode → pass through
   - Check if mute command → enter sleep mode
   - Check if start dictation → switch to dictation mode
   - Process as normal command → learning system + fuzzy matching
4. Create RecognitionResult
5. Invoke result listener
```

#### Error Handling

AndroidErrorHandler provides recovery strategies:

```kotlin
enum class RecoveryAction {
    NONE,                    // No recovery needed
    RETRY_IMMEDIATELY,       // Retry now (transient error)
    RETRY_WITH_DELAY,        // Retry after delay (busy/unavailable)
    RESTART_RECOGNITION,     // Full restart
    NOTIFY_USER,            // Cannot recover, notify user
    SWITCH_ENGINE           // Recommend engine switch
}
```

**Error Code Mapping**:
- ERROR_NETWORK → RETRY_WITH_DELAY
- ERROR_NO_MATCH → NONE (expected)
- ERROR_RECOGNIZER_BUSY → RETRY_IMMEDIATELY
- ERROR_INSUFFICIENT_PERMISSIONS → NOTIFY_USER
- ERROR_AUDIO → RESTART_RECOGNITION

#### Performance Considerations

**Network Dependency**:
- Requires internet connection
- Latency varies (50-300ms)
- Fallback to offline engines recommended

**Best Practices**:
```kotlin
// ✅ Check network before using
if (networkAvailable) {
    engine.startListening(SpeechMode.DYNAMIC_COMMAND)
}

// ✅ Handle errors gracefully
engine.setErrorListener { error, code ->
    if (code == SpeechRecognizer.ERROR_NETWORK) {
        // Switch to offline engine
        switchToVoskEngine()
    }
}
```

---

### WhisperEngine

**Type**: Offline speech recognition with advanced features
**Technology**: OpenAI Whisper (whisper.cpp)
**Memory**: 150MB (tiny) - 2500MB (large) depending on model
**Languages**: 99 languages with automatic detection

#### Architecture (6 Components)

```kotlin
WhisperEngine (main orchestrator)
  ├── WhisperConfig (device-optimized configuration)
  ├── WhisperModel (model size management and validation)
  ├── WhisperNative (JNI wrapper for whisper.cpp)
  ├── WhisperProcessor (audio processing, VAD, noise reduction)
  ├── WhisperErrorHandler (comprehensive error handling)
  └── Shared components (from engines/common)
```

#### Initialization

```kotlin
suspend fun initialize(config: SpeechConfig): Boolean
```

**Process**:
1. Initialize shared components (performance, result processor)
2. Initialize error handler
3. Initialize configuration with device optimizations
4. Initialize model management
5. Initialize native library integration
6. Initialize audio processor with VAD
7. Load initial model (based on modelSize config)
8. Initialize learning system
9. Wire components with callbacks
10. Enable voice if configured

**Thread Safety**: Uses UniversalInitializationManager (30s timeout, no degraded mode)

#### Advanced Features

**Language Detection**:
```kotlin
currentWhisperConfig = currentWhisperConfig.copy(
    enableLanguageDetection = true
)

// Result contains detected language
result.language  // e.g., "en", "es", "fr"
```

**Translation to English**:
```kotlin
engine.setTranslationEnabled(true, targetLanguage = "en")

// Result contains translation
result.translation  // e.g., "Buenos días" → "Good morning"
```

**Word-Level Timestamps**:
```kotlin
currentWhisperConfig = currentWhisperConfig.copy(
    enableWordTimestamps = true
)

// Access word timestamps
result.wordTimestamps?.forEach { word ->
    println("${word.word}: ${word.startTime}s - ${word.endTime}s (${word.confidence})")
}
```

#### Key Functions

**startListening()**
```kotlin
fun startListening() {
    engineScope.launch {
        engineMutex.withLock {
            isListening = true
            serviceState.setState(ServiceState.State.LISTENING)

            // Start audio processing with VAD
            whisperProcessor.startProcessing()

            // Reset timeout
            voiceStateManager.updateCommandExecutionTime()
            startTimeoutMonitoring()
        }
    }
}
```

**setDynamicCommands(commands: List<String>)**
```kotlin
fun setDynamicCommands(commands: List<String>) {
    registeredCommands.clear()
    registeredCommands.addAll(commands)

    engineScope.launch {
        commandCache.updateCommands(commands)
        // Learning system automatically uses these for fuzzy matching
    }
}
```

**changeModel(modelSize: WhisperModelSize): Boolean**
```kotlin
suspend fun changeModel(modelSize: WhisperModelSize): Boolean {
    return whisperModel.changeModel(modelSize)
    // Automatically updates native inference
}
```

**Model Sizes**:
- TINY: 75MB, ~32x faster, good for real-time
- BASE: 142MB, balanced performance
- SMALL: 466MB, better accuracy
- MEDIUM: 1500MB, high accuracy
- LARGE: 2900MB, best accuracy (slow on mobile)

#### Recognition Flow

```
1. Audio captured → WhisperProcessor
2. VAD detects speech → processAudioWithInference()
3. WhisperNative.runInference(audioData, params)
4. Native library processes with whisper.cpp
5. WhisperResult returned with segments and timestamps
6. handleWhisperResult():
   - Extract language if detection enabled
   - Extract word timestamps if enabled
   - Apply confidence threshold
   - Route to handleDictationResult() or handleCommandResult()
7. Process through learning system
8. Create RecognitionResult with advanced features
9. Invoke result listener
```

#### Performance Considerations

**Model Selection**:
```kotlin
// Real-time applications
WhisperModelSize.TINY      // 75MB, ~1s latency

// Balanced
WhisperModelSize.BASE      // 142MB, ~2s latency

// Accuracy-focused
WhisperModelSize.SMALL     // 466MB, ~5s latency
```

**Device Optimization**:
```kotlin
whisperConfig.applyDeviceOptimizations()
// Automatically adjusts:
// - Beam size
// - Temperature
// - Noise reduction level
// - VAD sensitivity
// Based on device capabilities
```

**GPU Acceleration**:
```kotlin
currentWhisperConfig = currentWhisperConfig.copy(
    useGPU = true,
    gpuLayers = 32  // Adjust based on GPU memory
)
```

**Memory Management**:
- Model loaded once and cached
- Audio buffer size configurable
- Automatic garbage collection triggers

**Best Practices**:
```kotlin
// ✅ Good - use appropriate model
val modelSize = if (isLowEndDevice) {
    WhisperModelSize.TINY
} else {
    WhisperModelSize.BASE
}

// ✅ Good - enable features selectively
config.copy(
    enableLanguageDetection = needsLanguageDetection,
    enableWordTimestamps = needsWordTiming,
    enableTranslation = false  // Disable if not needed
)

// ❌ Bad - always using largest model
WhisperModelSize.LARGE  // Too slow for real-time
```

---

### GoogleCloudEngine

**Type**: Online speech recognition with advanced features
**Technology**: Google Cloud Speech-to-Text API
**Memory**: ~15MB
**Languages**: 125+ languages

#### Architecture

```kotlin
GoogleCloudEngine (main orchestrator)
  ├── GoogleConfig (API configuration)
  ├── GoogleAuth (authentication and API key management)
  ├── GoogleStreaming (streaming recognition API)
  ├── GoogleTranscript (result parsing)
  ├── GoogleNetwork (network handling)
  └── GoogleErrorHandler (API error handling)
```

#### Key Features
- Real-time streaming recognition
- Automatic punctuation
- Speaker diarization (who spoke when)
- Profanity filtering
- Enhanced models for specific domains

---

## Core Components

### RecognitionResult

**Data Model** for speech recognition results:

```kotlin
data class RecognitionResult(
    val text: String,                           // Recognized text
    val originalText: String = text,            // Original before processing
    val confidence: Float,                      // 0.0 - 1.0
    val timestamp: Long = currentTimeMillis(),  // Recognition time
    val isPartial: Boolean = false,             // Partial result?
    val isFinal: Boolean = true,                // Final result?
    val alternatives: List<String> = emptyList(), // Alternative matches
    val engine: String = "",                    // Engine name
    val mode: String = "",                      // Recognition mode
    val metadata: Map<String, Any> = emptyMap(), // Extra data

    // Advanced features (Whisper)
    val language: String? = null,               // Detected language
    val translation: String? = null,            // Translation if enabled
    val wordTimestamps: List<WordTimestamp>? = null // Word-level timing
)
```

**Helper Methods**:
```kotlin
result.meetsThreshold(0.7f)        // Check confidence
result.getBestText()               // Get best text or alternative
result.isEmpty()                   // Check if empty
result.hasAdvancedFeatures()       // Has language/translation/timestamps?
result.getTotalDuration()          // Total duration from timestamps
result.getHighConfidenceWords(0.8f) // Words above threshold
```

### SpeechConfig

**Configuration** for all engines:

```kotlin
data class SpeechConfig(
    // Common settings
    val language: String = "en-US",
    val mode: SpeechMode = DYNAMIC_COMMAND,
    val enableVAD: Boolean = true,
    val confidenceThreshold: Float = 0.7f,
    val maxRecordingDuration: Long = 30000,
    val timeoutDuration: Long = 5000,
    val dictationTimeout: Long = 2000,
    val voiceTimeoutMinutes: Long = 5,

    // Special commands
    val muteCommand: String = "mute voice",
    val unmuteCommand: String = "voice",
    val startDictationCommand: String = "start dictation",
    val stopDictationCommand: String = "stop dictation",

    // Engine selection
    val engine: SpeechEngine = VOSK,

    // Optional configs
    val cloudApiKey: String? = null,
    val modelPath: String? = null
)
```

**Factory Methods**:
```kotlin
SpeechConfig.vosk()           // VOSK engine with defaults
SpeechConfig.vivoka()         // Vivoka engine
SpeechConfig.googleSTT()      // Android STT
SpeechConfig.googleCloud(key) // Google Cloud with API key
```

**Fluent Modification**:
```kotlin
val config = SpeechConfig.vosk()
    .withLanguage("es-ES")
    .withMode(SpeechMode.DICTATION)
    .withConfidenceThreshold(0.8f)
    .withTimeout(10000)
```

### SimilarityMatcher

**Fuzzy Matching** utility:

```kotlin
object SimilarityMatcher {
    fun findMostSimilarWithConfidence(
        input: String,
        commands: List<String>,
        threshold: Float = 0.70f
    ): Pair<String, Float>?

    fun calculateSimilarity(s1: String, s2: String): Float

    fun levenshteinDistance(s1: String, s2: String): Int

    fun findAllSimilar(
        input: String,
        commands: List<String>,
        threshold: Float = 0.70f,
        maxResults: Int = 5
    ): List<Pair<String, Float>>
}
```

**Example Usage**:
```kotlin
val result = SimilarityMatcher.findMostSimilarWithConfidence(
    input = "opn calcluator",
    commands = listOf("open calculator", "open camera", "open calendar"),
    threshold = 0.70f
)
// Returns: Pair("open calculator", 0.87f)

val similarity = SimilarityMatcher.calculateSimilarity("hello", "helo")
// Returns: 0.8f
```

### ConfidenceScorer

**Confidence Level** classification:

```kotlin
enum class ConfidenceLevel {
    HIGH,      // >= 0.85
    MEDIUM,    // >= 0.70
    LOW,       // >= 0.50
    REJECT     // < 0.50
}

class ConfidenceScorer {
    fun getConfidenceLevel(score: Float): ConfidenceLevel
}
```

---

## Function-by-Function Reference

### Engine Interface (Common to All Engines)

#### Initialization

```kotlin
suspend fun initialize(config: SpeechConfig): Boolean
```
**Purpose**: Initialize the speech engine
**Thread-Safe**: Yes (via UniversalInitializationManager)
**Returns**: `true` if successful, `false` otherwise
**Throws**: No exceptions (errors logged)

**Example**:
```kotlin
val config = SpeechConfig.vosk()
    .withLanguage("en-US")
    .withMode(SpeechMode.DYNAMIC_COMMAND)

val success = voskEngine.initialize(config)
if (success) {
    println("Engine initialized")
} else {
    println("Initialization failed")
}
```

#### Listening Control

```kotlin
fun startListening()
```
**Purpose**: Start listening for speech
**Prerequisites**: Engine must be initialized
**Side Effects**:
- Sets state to LISTENING
- Starts timeout timer
- Begins audio capture

```kotlin
fun stopListening()
```
**Purpose**: Stop listening for speech
**Side Effects**:
- Cancels timeout timer
- Stops audio capture
- Processes any pending audio

**Example**:
```kotlin
engine.startListening()
// User speaks...
engine.stopListening()
```

#### Command Management

```kotlin
fun setContextPhrases(phrases: List<String>)
```
**Purpose**: Set dynamic commands for recognition
**Parameters**:
- `phrases`: List of command strings (will be normalized to lowercase)
**Side Effects**:
- Updates command cache
- Rebuilds grammar (VOSK)
- Recompiles model (Vivoka)
- Registers with learning system

**Example**:
```kotlin
val commands = listOf(
    "open calculator",
    "close window",
    "navigate home"
)
engine.setContextPhrases(commands)
```

```kotlin
fun setStaticCommands(commands: List<String>)
```
**Purpose**: Set static commands (always available)
**Use Case**: System-wide commands, wake words
**Example**:
```kotlin
engine.setStaticCommands(listOf("voice", "mute voice", "help"))
```

#### Mode Control

```kotlin
fun changeMode(mode: SpeechMode)
```
**Purpose**: Switch recognition mode
**Parameters**:
- `mode`: STATIC_COMMAND, DYNAMIC_COMMAND, DICTATION, FREE_SPEECH

**Example**:
```kotlin
// Switch to dictation
engine.changeMode(SpeechMode.DICTATION)

// User dictates text...

// Switch back to commands
engine.changeMode(SpeechMode.DYNAMIC_COMMAND)
```

```kotlin
fun startDictation()
fun stopDictation()
fun isDictationMode(): Boolean
```
**Purpose**: Dictation mode convenience methods

#### Listeners

```kotlin
fun setResultListener(listener: OnSpeechResultListener)
```
**Purpose**: Register callback for recognition results
**Type**: `(RecognitionResult) -> Unit`
**Example**:
```kotlin
engine.setResultListener { result ->
    if (result.meetsThreshold(0.7f)) {
        println("Recognized: ${result.text} (${result.confidence})")
    }
}
```

```kotlin
fun setErrorListener(listener: OnSpeechErrorListener)
```
**Purpose**: Register callback for errors
**Type**: `(String, Int) -> Unit`
**Example**:
```kotlin
engine.setErrorListener { message, code ->
    Log.e("SpeechEngine", "Error $code: $message")
    showUserNotification(message)
}
```

```kotlin
fun setPartialResultListener(listener: (String) -> Unit)
```
**Purpose**: Register callback for partial results (real-time feedback)
**Example**:
```kotlin
engine.setPartialResultListener { partial ->
    updateUIWithPartialText(partial)
}
```

#### Performance & Statistics

```kotlin
fun getPerformanceMetrics(): Map<String, Any>
```
**Purpose**: Get performance statistics
**Returns**: Map with:
- `engineName`: String
- `sessionDuration`: Long (ms)
- `totalRecognitions`: Int
- `successRate`: Float (0.0-1.0)
- `averageLatency`: Long (ms)
- `minLatency`: Long (ms)
- `maxLatency`: Long (ms)
- `currentMemoryUsage`: Long (bytes)
- `performanceState`: String

**Example**:
```kotlin
val metrics = engine.getPerformanceMetrics()
println("Success rate: ${metrics["successRate"]}%")
println("Avg latency: ${metrics["averageLatency"]}ms")
```

```kotlin
fun getLearningStats(): Map<String, Any>
```
**Purpose**: Get learning system statistics
**Returns**: Map with:
- `learnedCommands`: Int
- `vocabularyCache`: Int
- `registeredCommands`: Int

```kotlin
fun resetPerformanceMetrics()
```
**Purpose**: Reset all performance counters

#### Cleanup

```kotlin
fun destroy()
```
**Purpose**: Release all resources
**Must Call**: Before discarding engine reference
**Side Effects**:
- Stops listening
- Releases models
- Closes database connections
- Cancels coroutines
- Frees native resources

**Example**:
```kotlin
override fun onDestroy() {
    engine.destroy()
    super.onDestroy()
}
```

---

## Adding New Engines

### Step 1: Create Engine Package

```
modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/myengine/
```

### Step 2: Define Components

Following SOLID principles, create specialized components:

```kotlin
// 1. Main orchestrator
class MyEngine(private val context: Context) {
    // Shared components
    private val serviceState = ServiceState()
    private val performanceMonitor = PerformanceMonitor("MyEngine")
    private val errorRecoveryManager = ErrorRecoveryManager("MyEngine", context)

    // Engine-specific components
    private lateinit var myConfig: MyEngineConfig
    private lateinit var myRecognizer: MyEngineRecognizer

    suspend fun initialize(config: SpeechConfig): Boolean {
        // Use UniversalInitializationManager
    }
}

// 2. Configuration component
class MyEngineConfig(private val context: Context) {
    fun initialize(config: SpeechConfig): Boolean
    fun getSpeechConfig(): SpeechConfig
}

// 3. Recognizer component
class MyEngineRecognizer {
    fun startListening(): Boolean
    fun stopListening()
    fun processResult(result: Any): RecognitionResult
}

// 4. Error handler component
class MyEngineErrorHandler(
    private val serviceState: ServiceState,
    private val errorRecoveryManager: ErrorRecoveryManager
) {
    fun handleError(code: Int, message: String, exception: Exception?)
}
```

### Step 3: Implement Engine Interface

All engines should provide these core methods:

```kotlin
class MyEngine(private val context: Context) {
    // Lifecycle
    suspend fun initialize(config: SpeechConfig): Boolean
    fun destroy()

    // Listening control
    fun startListening()
    fun stopListening()

    // Command management
    fun setContextPhrases(phrases: List<String>)
    fun setStaticCommands(commands: List<String>)

    // Mode control
    fun changeMode(mode: SpeechMode)

    // Listeners
    fun setResultListener(listener: OnSpeechResultListener)
    fun setErrorListener(listener: OnSpeechErrorListener)

    // Performance
    fun getPerformanceMetrics(): Map<String, Any>
}
```

### Step 4: Use Shared Components

Leverage existing shared components:

```kotlin
private val commandCache = CommandCache()
private val learningSystem = LearningSystem("MyEngine", context)
private val resultProcessor = ResultProcessor()
private val audioStateManager = AudioStateManager("MyEngine")
private val voiceStateManager = VoiceStateManager(context, "MyEngine")
```

### Step 5: Add to SpeechEngine Enum

```kotlin
enum class SpeechEngine {
    VOSK,
    VIVOKA,
    ANDROID_STT,
    WHISPER,
    GOOGLE_CLOUD,
    MY_ENGINE;  // Add here

    fun getDisplayName(): String {
        return when (this) {
            MY_ENGINE -> "My Custom Engine"
            // ...
        }
    }
}
```

### Step 6: Update Factory Methods

```kotlin
companion object {
    fun myEngine() = SpeechConfig(engine = SpeechEngine.MY_ENGINE)
}
```

### Step 7: Add Tests

```kotlin
class MyEngineTest {
    @Test
    fun `initialization succeeds with valid config`() = runTest {
        val config = SpeechConfig.myEngine()
        val engine = MyEngine(context)

        val success = engine.initialize(config)

        assertTrue(success)
    }

    @Test
    fun `command recognition works`() = runTest {
        // Test command recognition
    }
}
```

### Best Practices for New Engines

1. **Follow SOLID Principles**:
   - Single Responsibility: One class, one purpose
   - Open/Closed: Extensible without modification
   - Liskov Substitution: Components interchangeable
   - Interface Segregation: Minimal dependencies
   - Dependency Inversion: Depend on abstractions

2. **Use Shared Components**: Don't reinvent the wheel

3. **Thread Safety**: Use UniversalInitializationManager

4. **Error Handling**: Use ErrorRecoveryManager with retry logic

5. **Performance**: Use PerformanceMonitor for metrics

6. **Learning**: Integrate with LearningSystem

7. **Documentation**: Document all public functions

8. **Testing**: Unit tests + integration tests

---

## Performance Considerations

### Memory Management

**Engine Memory Profiles**:
```
Android STT:  15-20 MB   (lightweight, network-based)
VOSK:         30-50 MB   (small model, offline)
Vivoka:       60-100 MB  (hybrid, compiled models)
Whisper Tiny: 150-200 MB (offline, fast)
Whisper Base: 250-350 MB (offline, balanced)
Whisper Large: 2-3 GB    (offline, best quality)
```

**Best Practices**:
```kotlin
// ✅ Good - release when not needed
override fun onStop() {
    engine.stopListening()
}

override fun onDestroy() {
    engine.destroy()  // Critical!
}

// ✅ Good - use appropriate model size
val modelSize = when {
    availableMemory < 500.MB -> WhisperModelSize.TINY
    availableMemory < 1.GB -> WhisperModelSize.BASE
    else -> WhisperModelSize.SMALL
}

// ❌ Bad - never destroy
class MyActivity {
    val engine = VoskEngine(context)  // Memory leak!
}
```

### CPU/GPU Optimization

**Threading**:
- All engines use coroutines (Dispatchers.IO for heavy work)
- Recognition happens off main thread
- Results delivered on Main dispatcher

**GPU Acceleration** (Whisper):
```kotlin
config.copy(
    useGPU = true,
    gpuLayers = 32
)
```

### Network Optimization

**Android STT / Google Cloud**:
```kotlin
// Check network before starting
if (isNetworkAvailable()) {
    engine.startListening()
} else {
    // Fallback to offline engine
    switchToVoskEngine()
}
```

**Vivoka Hybrid**:
```kotlin
// Automatically switches between offline/online
// based on network availability
config.copy(mode = SpeechMode.HYBRID)
```

### Battery Optimization

**Minimize Active Listening**:
```kotlin
// Use timeout
config.copy(
    voiceTimeoutMinutes = 5  // Auto-sleep after 5 min
)

// Or manual sleep/wake
engine.changeMode(SpeechMode.SLEEPING)
```

**Voice Activity Detection (VAD)**:
```kotlin
config.copy(
    enableVAD = true  // Only process when speech detected
)
```

### Latency Optimization

**Engine Latency Comparison** (approximate):
```
Android STT:   50-300ms  (network-dependent)
VOSK (small):  100-200ms (offline, fast)
Vivoka:        80-150ms  (offline/online)
Whisper Tiny:  500-1000ms (offline)
Whisper Base:  1000-2000ms (offline)
```

**Reduce Latency**:
```kotlin
// Use smaller models
WhisperModelSize.TINY vs LARGE

// Reduce beam size (Whisper)
config.copy(beamSize = 1)  // vs 5

// Disable features if not needed
config.copy(
    enableLanguageDetection = false,
    enableWordTimestamps = false,
    enableTranslation = false
)
```

### Command Set Size

**Performance Impact**:
```
<50 commands:    Negligible impact
50-200 commands: Slight increase in recognition time
200-500 commands: Noticeable impact on grammar rebuild
500-1000 commands: Significant impact, use vocabulary filtering
>1000 commands: Consider dynamic loading or engine switch
```

**Optimization**:
```kotlin
// ✅ Good - update in batches
engine.setContextPhrases(allCommands)

// ✅ Good - use static + dynamic split
engine.setStaticCommands(alwaysAvailable)
engine.setContextPhrases(contextual)

// ❌ Bad - frequent small updates
commands.forEach { engine.setContextPhrases(listOf(it)) }
```

---

## Testing

### Unit Tests

Located in `src/test/java/`:

```kotlin
class VoskEngineTest {
    @Test
    fun `initialization succeeds with valid config`() = runTest {
        val config = SpeechConfig.vosk()
        val engine = VoskEngine(context)

        val success = engine.initialize(config)
        assertTrue(success)
    }

    @Test
    fun `fuzzy matching works correctly`() {
        val result = SimilarityMatcher.findMostSimilarWithConfidence(
            "opn calcluator",
            listOf("open calculator", "open calendar"),
            0.70f
        )

        assertNotNull(result)
        assertEquals("open calculator", result!!.first)
        assertTrue(result.second >= 0.70f)
    }
}
```

### Integration Tests

Located in `src/test/java/`:

```kotlin
@RunWith(AndroidJUnit4::class)
class SpeechRecognitionIntegrationTest {
    @Test
    fun `end to end recognition flow`() = runTest {
        val engine = VoskEngine(context)
        val config = SpeechConfig.vosk()

        engine.initialize(config)

        var receivedResult: RecognitionResult? = null
        engine.setResultListener { result ->
            receivedResult = result
        }

        engine.setContextPhrases(listOf("test command"))
        engine.startListening()

        // Simulate audio input...

        engine.stopListening()

        assertNotNull(receivedResult)
        assertEquals("test command", receivedResult?.text)
    }
}
```

### Performance Tests

```kotlin
class PerformanceTest {
    @Test
    fun `recognition latency under 200ms`() = runTest {
        val engine = VoskEngine(context)
        engine.initialize(SpeechConfig.vosk())

        val startTime = System.currentTimeMillis()

        // Trigger recognition

        val latency = System.currentTimeMillis() - startTime
        assertTrue(latency < 200)
    }
}
```

### Test Utilities

```kotlin
// Mock engines for testing
class MockSpeechEngine : SpeechEngine {
    var mockResults = listOf<RecognitionResult>()

    override suspend fun initialize(config: SpeechConfig) = true

    override fun startListening() {
        mockResults.forEach { resultListener?.invoke(it) }
    }
}

// Test helpers
object SpeechTestUtils {
    fun createMockResult(text: String, confidence: Float = 1.0f) =
        RecognitionResult(
            text = text,
            confidence = confidence,
            isFinal = true
        )
}
```

---

## Troubleshooting

### Common Issues

#### 1. Initialization Fails

**Symptom**: `initialize()` returns `false`

**Causes**:
- Model files missing
- Insufficient permissions
- Assets not extracted (Vivoka)
- VSDK already initialized (Vivoka)

**Solutions**:
```kotlin
// Check logs for specific error
Log.d("SpeechEngine", "Initialization failed - check logs above")

// Verify model path
val modelPath = File(config.modelPath)
if (!modelPath.exists()) {
    Log.e("SpeechEngine", "Model not found: ${config.modelPath}")
}

// Check permissions (Android STT)
if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
    != PackageManager.PERMISSION_GRANTED) {
    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE)
}

// Vivoka VSDK multiple init error
// Solution: Use VivokaInitializationManager (automatic)
```

#### 2. No Recognition Results

**Symptom**: Listening starts but no results received

**Causes**:
- No commands registered
- Commands don't match vocabulary
- Microphone issues
- Background noise

**Solutions**:
```kotlin
// Verify commands registered
engine.setContextPhrases(commands)
Log.d("Commands", "Registered ${commands.size} commands")

// Check vocabulary (VOSK)
val stats = engine.getLearningStats()
Log.d("Vocabulary", "Known: ${stats["knownCommands"]}, Unknown: ${stats["unknownCommands"]}")

// Test microphone
// Record audio separately to verify hardware works

// Enable partial results for debugging
engine.setPartialResultListener { partial ->
    Log.d("Partial", "Heard: $partial")
}
```

#### 3. Low Recognition Accuracy

**Symptom**: Frequent wrong matches

**Causes**:
- Fuzzy matching too aggressive
- Wrong language model
- Poor audio quality
- Ambiguous commands

**Solutions**:
```kotlin
// Increase confidence threshold
config.copy(confidenceThreshold = 0.85f)  // vs 0.70f

// Use exact matching
val exactMatch = registeredCommands.contains(command)
if (!exactMatch) {
    // Reject
}

// Improve audio
config.copy(enableVAD = true)  // Filter silence
whisperConfig.copy(noiseReductionLevel = 0.8f)

// Disambiguate commands
// ❌ Bad
listOf("open", "close", "open app")

// ✅ Good
listOf("open application", "close application", "open menu")
```

#### 4. High Memory Usage

**Symptom**: App crashes with OutOfMemoryError

**Causes**:
- Large Whisper model
- Model not released
- Learning cache grows unbounded

**Solutions**:
```kotlin
// Use smaller model
WhisperModelSize.TINY  // vs LARGE

// Destroy when done
override fun onDestroy() {
    engine.destroy()  // Critical!
}

// Clear learning cache periodically
learningSystem.clearCache()

// Monitor memory
val runtime = Runtime.getRuntime()
val usedMemory = runtime.totalMemory() - runtime.freeMemory()
Log.d("Memory", "Used: ${usedMemory / 1024 / 1024} MB")
```

#### 5. Network Errors (Android STT / Google Cloud)

**Symptom**: ERROR_NETWORK frequent

**Causes**:
- No internet connection
- Slow connection
- Google services unavailable

**Solutions**:
```kotlin
// Check network before starting
fun isNetworkAvailable(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.activeNetworkInfo?.isConnected == true
}

if (!isNetworkAvailable()) {
    // Fallback to offline engine
    switchToVoskEngine()
}

// Handle errors gracefully
engine.setErrorListener { error, code ->
    if (code == SpeechRecognizer.ERROR_NETWORK) {
        Toast.makeText(context, "No internet - switching to offline", LENGTH_SHORT).show()
        switchToOfflineEngine()
    }
}
```

### Debug Logging

Enable verbose logging:

```kotlin
// VoskEngine
System.setProperty("vosk.log.level", "DEBUG")

// Vivoka
com.vivoka.vsdk.Logger.setLogLevel(com.vivoka.vsdk.Logger.LogLevel.DEBUG)

// Android
adb shell setprop log.tag.SpeechRecognizer DEBUG
adb logcat SpeechRecognizer:D *:S

// Whisper
Log.v("WhisperEngine", "Debug info")
```

### Performance Profiling

```kotlin
// Record metrics
val metrics = engine.getPerformanceMetrics()
Log.d("Performance", """
    Success Rate: ${metrics["successRate"]}%
    Avg Latency: ${metrics["averageLatency"]}ms
    Memory: ${metrics["currentMemoryUsage"] / 1024 / 1024} MB
    Bottlenecks: ${metrics["bottlenecks"]}
""".trimIndent())

// Android Profiler
// Use Android Studio → View → Tool Windows → Profiler
// Monitor CPU, Memory, Network during recognition
```

---

## Additional Resources

### Documentation
- [User Manual](./user-manual.md) - End-user guide
- [API Reference](./reference/api/) - Complete API documentation
- [Architecture](./architecture/) - System design docs
- [Changelog](./changelog/CHANGELOG.md) - Version history

### External Documentation
- **VOSK**: https://alphacephei.com/vosk/
- **Vivoka VSDK**: https://console.vivoka.com/docs
- **Whisper**: https://github.com/openai/whisper
- **Android SpeechRecognizer**: https://developer.android.com/reference/android/speech/SpeechRecognizer
- **Google Cloud Speech**: https://cloud.google.com/speech-to-text/docs

### Code Examples
- See `/modules/libraries/SpeechRecognition/src/test/java/` for comprehensive examples
- Integration examples in VoiceOSCore module

---

**Document Version**: 1.0.0
**Created**: 2025-10-23 21:19 PDT
**Author**: Claude Code (VOS4 Documentation Specialist)
**Review Status**: Initial Draft

**Generated with** [Claude Code](https://claude.com/claude-code)

---

## Model Deployment and Path Resolution

**Added**: 2025-11-21
**Version**: 2.1.0
**Feature**: Multi-location model path resolution for all engines

### Overview

Starting with version 2.1.0, the SpeechRecognition module supports **multi-location model path resolution** for all offline engines (Vivoka, Whisper, VOSK). This feature enables flexible model deployment options without requiring models to be bundled in the APK.

### Key Benefits

1. **Smaller APK Size**: Models don't need to be bundled in the APK
2. **Manual Pre-deployment**: Deploy models via ADB or file manager before installation
3. **Persistent Storage**: Models in shared folder survive app uninstall
4. **Faster Development**: Deploy models once, test multiple builds
5. **Flexible Testing**: Switch between model versions without rebuilding

### Architecture

The `SpeechModelPathResolver` class provides universal path resolution for all speech engines:

```kotlin
package com.augmentalis.voiceos.speech.engines.common

class SpeechModelPathResolver(
    private val context: Context,
    private val engineName: String,    // "vivoka", "whisper", "vosk"
    private val modelDirName: String   // "vsdk", "whisper_models", "model"
)
```

### Path Resolution Priority

Models are searched in the following order:

1. **Internal App Storage** (default/current behavior)
   - Path: `/data/data/com.augmentalis.voiceos/files/{model_dir}/`
   - Accessible: App only
   - Survives: Until app uninstall
   - Use case: Bundled models, downloaded models

2. **External App-Specific Storage**
   - Path: `/storage/emulated/0/Android/data/com.augmentalis.voiceos/files/{model_dir}/`
   - Accessible: File manager, ADB
   - Survives: Until app uninstall
   - Use case: Manual deployment during development

3. **Shared Hidden Folder** (NEW - survives uninstall)
   - Path: `/storage/emulated/0/.voiceos/models/{engine}/{model_dir}/`
   - Accessible: File manager, ADB, root
   - Survives: App uninstall/reinstall
   - Use case: Persistent models across builds, team sharing

4. **Download/Extract** (fallback)
   - If not found in any location, engines fall back to:
     - Extracting from APK assets (if bundled)
     - Downloading from network (if supported)

### Engine-Specific Paths

#### Vivoka VSDK

```kotlin
// Example paths checked (in order):
1. /data/data/com.augmentalis.voiceos/files/vsdk/
2. /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk/
3. /storage/emulated/0/.voiceos/models/vivoka/vsdk/
```

Required files in `vsdk/` directory:
- `vivoka.dat`
- `vivoka.ini`
- Language model files (e.g., `model_en.dat`)

#### Whisper

```kotlin
// Example paths checked (in order):
1. /data/data/com.augmentalis.voiceos/files/whisper_models/
2. /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/whisper_models/
3. /storage/emulated/0/.voiceos/models/whisper/whisper_models/
```

Required files:
- `ggml-tiny.bin` (or other model size)

#### VOSK

```kotlin
// Example paths checked (in order):
1. /data/data/com.augmentalis.voiceos/files/model/
2. /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/model/
3. /storage/emulated/0/.voiceos/models/vosk/model/
```

Required structure:
- `model/` directory containing VOSK model files

### Implementation Details

#### Modified Files

1. **`SpeechModelPathResolver.kt`** (NEW)
   - Universal path resolver for all engines
   - 182 lines
   - Location: `engines/common/`

2. **`VivokaInitializer.kt`** (MODIFIED)
   - Uses path resolver before extracting assets
   - Searches all locations before falling back to APK extraction

3. **`WhisperModelManager.kt`** (MODIFIED)
   - `getModelFile()` now uses path resolver
   - Searches all locations before downloading

4. **`VoskModel.kt`** (MODIFIED)
   - `calculateModelPath()` now uses path resolver
   - Searches all locations before extracting

#### Code Example

```kotlin
// How engines use the path resolver
val pathResolver = SpeechModelPathResolver(
    context = context,
    engineName = "vivoka",  // or "whisper", "vosk"
    modelDirName = "vsdk"   // or "whisper_models", "model"
)

// Get all search paths for logging
val paths = pathResolver.getSearchPathsForLogging()
ConditionalLogger.d(TAG) { "Searching: $paths" }

// Resolve path with optional validation
val validationFunction: (File) -> Boolean = { dir ->
    // Check if directory contains required files
    File(dir, "vivoka.dat").exists()
}

val modelDir = pathResolver.resolveModelPath(validationFunction)
ConditionalLogger.i(TAG) { "Using models at: ${modelDir.absolutePath}" }
```

### Manual Model Deployment

#### Option 1: ADB Push (Recommended for Development)

```bash
# Vivoka VSDK
adb push ./vsdk /storage/emulated/0/.voiceos/models/vivoka/vsdk

# Whisper models
adb push ./whisper_models /storage/emulated/0/.voiceos/models/whisper/whisper_models

# VOSK model
adb push ./model /storage/emulated/0/.voiceos/models/vosk/model
```

**Advantages**:
- Models survive app uninstall
- Deploy once, test multiple builds
- Faster development iteration
- No APK bloat

#### Option 2: File Manager

1. Connect device to computer (MTP mode)
2. Navigate to internal storage
3. Create folder structure: `.voiceos/models/{engine}/{model_dir}/`
4. Copy model files into appropriate directory

#### Option 3: Root Access (Advanced)

```bash
# Push to internal app storage (requires root)
adb root
adb remount
adb push ./vsdk /data/data/com.augmentalis.voiceos/files/vsdk
```

### Testing Model Resolution

```kotlin
// Test path resolution
val resolver = SpeechModelPathResolver(context, "vivoka", "vsdk")

// Get all candidate paths
val paths = resolver.getModelCandidatePaths()
paths.forEach { path ->
    Log.d(TAG, "Checking: ${path.absolutePath} - exists: ${path.exists()}")
}

// Check manual deployment path
val manualPath = resolver.getManualDeploymentPath()
Log.d(TAG, "Manual deployment path: ${manualPath.absolutePath}")

// Get ADB push command for documentation
val adbCommand = resolver.getAdbPushCommand()
Log.d(TAG, "Deploy with: $adbCommand")
```

### Logging

All path resolution operations use `ConditionalLogger` for compliance:

```kotlin
ConditionalLogger.i(TAG) { "[vivoka] Found valid model directory at: $path" }
ConditionalLogger.d(TAG) { "[vivoka] No existing models found. Will use primary: $path" }
ConditionalLogger.w(TAG) { "[vivoka] Model validation failed for: $path" }
ConditionalLogger.e(TAG, exception) { "[vivoka] Failed to extract models from assets" }
```

### Migration from Previous Versions

**Before v2.1.0**:
- Models extracted to internal storage only
- Required bundling in APK or network download
- No path resolution

**After v2.1.0**:
- Multi-location search with fallback
- Support for pre-deployed models
- Backward compatible (internal storage still works)
- No code changes required in existing engines

### Troubleshooting

#### Models Not Found

```bash
# Check if models exist in shared location
adb shell ls -la /storage/emulated/0/.voiceos/models/vivoka/vsdk/

# Check permissions
adb shell ls -la /storage/emulated/0/.voiceos/

# Verify file contents
adb shell du -sh /storage/emulated/0/.voiceos/models/*/
```

#### Path Resolution Issues

Enable verbose logging:

```kotlin
// In VivokaConfig.kt or engine-specific config
ConditionalLogger.setLogLevel(ConditionalLogger.Level.DEBUG)
```

Check logs for path resolution:
```
D/VivokaInitializer: Searching for VSDK in locations:
  - /data/data/com.augmentalis.voiceos/files/vsdk
  - /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk
  - /storage/emulated/0/.voiceos/models/vivoka/vsdk
I/VivokaInitializer: Found existing VSDK at: /storage/emulated/0/.voiceos/models/vivoka/vsdk
```

#### Storage Permissions

Ensure app has storage permissions (required for external paths):

```kotlin
// In AndroidManifest.xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />  <!-- Not needed on Android 10+ -->
```

### Performance Considerations

- **First Launch**: Model lookup adds <10ms overhead
- **Subsequent Launches**: Path is cached, negligible overhead
- **Storage I/O**: Minimal impact, models loaded into memory once
- **Memory**: No additional memory overhead

### Security Considerations

1. **Shared Folder**:
   - Hidden folder (`.voiceos`) not visible in default file manager
   - Readable/writable by any app with storage permission
   - Consider encryption for sensitive models

2. **Model Validation**:
   - Each engine validates model integrity
   - Checksum verification (Whisper)
   - Format validation (VOSK, Vivoka)

3. **Path Traversal Prevention**:
   - All paths are resolved through `File.getCanonicalPath()`
   - No user-provided path components

### Future Enhancements

Planned for future versions:

1. **Cloud Sync**: Sync models across devices via cloud storage
2. **Model Versioning**: Auto-update models when new versions available
3. **Compression**: On-the-fly decompression for smaller storage
4. **Differential Updates**: Only download changed model files
5. **Model Marketplace**: Browse and install community models

---

**Section Added**: 2025-11-21
**Author**: Claude Code  
**Review Status**: Production Ready
**Related Commit**: `5c8ebf27`
