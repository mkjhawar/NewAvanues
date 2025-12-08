# SpeechRecognition API Documentation

## Table of Contents
1. [Core APIs](#core-apis)
2. [Whisper Engine APIs](#whisper-engine-apis)
3. [Common Components](#common-components)
4. [Configuration](#configuration)
5. [Models](#models)
6. [UI Components](#ui-components)

## Core APIs

## Whisper Engine APIs

### WhisperEngine

Main API for OpenAI Whisper speech recognition with native whisper.cpp integration.

#### Initialization

```kotlin
class WhisperEngine(context: Context) {
    
    /**
     * Initialize engine with configuration
     * @param config Speech recognition configuration
     * @return Boolean indicating success
     */
    suspend fun initialize(config: SpeechConfig): Boolean
    
    /**
     * Load a specific Whisper model
     * @param model WhisperModel to load
     * @return Boolean indicating success
     */
    suspend fun loadModel(model: WhisperModel): Boolean
    
    /**
     * Release resources and cleanup
     */
    fun release()
}
```

#### Recognition Methods

```kotlin
/**
 * Start continuous listening
 * @return Boolean indicating success
 */
fun startListening(): Boolean

/**
 * Stop listening
 */
fun stopListening()

/**
 * Transcribe audio data directly
 * @param audioData Float array of audio samples (16kHz)
 * @return RecognitionResult or null
 */
suspend fun transcribe(audioData: FloatArray): RecognitionResult?

/**
 * Transcribe audio file
 * @param audioFile File containing audio
 * @return RecognitionResult or null
 */
suspend fun transcribeFile(audioFile: File): RecognitionResult?

/**
 * Process streaming audio chunk
 * @param audioChunk Float array of audio samples
 * @param isFinal Whether this is the final chunk
 * @return RecognitionResult or null
 */
suspend fun processStream(
    audioChunk: FloatArray,
    isFinal: Boolean = false
): RecognitionResult?
```

#### Configuration

```kotlin
/**
 * Set recognition language
 * @param language ISO language code (e.g., "en", "es", "fr") or "auto"
 */
fun setLanguage(language: String)

/**
 * Enable/disable translation to English
 * @param enable Whether to translate to English
 */
fun enableTranslation(enable: Boolean)

/**
 * Set number of threads for processing
 * @param threads Number of threads (1-8)
 */
fun setThreadCount(threads: Int)

/**
 * Set beam size for beam search
 * @param beamSize Beam size (1-10, higher = more accurate but slower)
 */
fun setBeamSize(beamSize: Int)

/**
 * Enable word-level timestamps
 * @param enable Whether to generate word timestamps
 */
fun enableWordTimestamps(enable: Boolean)

/**
 * Set Voice Activity Detection parameters
 * @param threshold Energy threshold (0.0-1.0)
 * @param speechPadMs Speech padding in milliseconds
 * @param silenceMs Silence duration to stop in milliseconds
 */
fun configureVAD(
    threshold: Float = 0.6f,
    speechPadMs: Int = 300,
    silenceMs: Int = 1000
)
```

#### Result Handling

```kotlin
/**
 * Set result listener for recognition events
 * @param listener Callback for recognition results
 */
fun setResultListener(listener: (RecognitionResult) -> Unit)

/**
 * Set error listener
 * @param listener Callback for errors
 */
fun setErrorListener(listener: (WhisperError) -> Unit)

/**
 * Set progress listener for long operations
 * @param listener Callback for progress updates (0.0-1.0)
 */
fun setProgressListener(listener: (Float) -> Unit)

/**
 * Get recognition results flow
 */
val resultsFlow: Flow<RecognitionResult>
```

### WhisperModelManager

Manages Whisper model downloads, caching, and selection.

#### Model Operations

```kotlin
class WhisperModelManager(context: Context) {
    
    /**
     * Download a Whisper model
     * @param model Model to download
     * @return Flow of DownloadState
     */
    suspend fun downloadModel(model: WhisperModel): Flow<DownloadState>
    
    /**
     * Check if model is cached
     * @param model Model to check
     * @return Boolean indicating if cached
     */
    fun isModelCached(model: WhisperModel): Boolean
    
    /**
     * Get all cached models
     * @return List of cached WhisperModel
     */
    fun getCachedModels(): List<WhisperModel>
    
    /**
     * Delete a cached model
     * @param model Model to delete
     * @return Boolean indicating success
     */
    fun deleteModel(model: WhisperModel): Boolean
    
    /**
     * Clear all cached models
     */
    fun clearCache()
    
    /**
     * Get recommended model for device
     * @return Recommended WhisperModel
     */
    fun getRecommendedModel(): WhisperModel
    
    /**
     * Get available storage space
     * @return Available bytes
     */
    fun getAvailableSpace(): Long
    
    /**
     * Cancel ongoing download
     */
    fun cancelDownload()
    
    /**
     * Observable download state
     */
    val downloadState: StateFlow<DownloadState>
}
```

#### Download States

```kotlin
sealed class DownloadState {
    object Idle : DownloadState()
    
    data class Downloading(
        val progress: Float,        // 0.0 to 1.0
        val downloadedMB: Long,     // Downloaded megabytes
        val totalMB: Long          // Total megabytes
    ) : DownloadState()
    
    data class Success(
        val modelFile: File        // Downloaded model file
    ) : DownloadState()
    
    data class Error(
        val message: String        // Error message
    ) : DownloadState()
}
```

### WhisperJNI

Low-level JNI interface to native whisper.cpp library.

```kotlin
class WhisperJNI {
    /**
     * Initialize native library with model
     * @param modelPath Path to model file
     * @param threads Number of threads
     * @return Boolean indicating success
     */
    external fun initNative(
        modelPath: String,
        threads: Int = 4
    ): Boolean
    
    /**
     * Transcribe audio using native library
     * @param audioData Audio samples as float array
     * @param sampleRate Sample rate (typically 16000)
     * @param language Language code or null for auto-detect
     * @param translate Whether to translate to English
     * @return RecognitionResult or null
     */
    external fun transcribeNative(
        audioData: FloatArray,
        sampleRate: Int,
        language: String? = null,
        translate: Boolean = false
    ): RecognitionResult?
    
    /**
     * Release native resources
     */
    external fun releaseNative()
    
    /**
     * Get model information
     * @return ModelInfo with model details
     */
    external fun getModelInfo(): ModelInfo
    
    data class ModelInfo(
        val name: String,
        val parameters: Long,
        val multilingual: Boolean,
        val languages: List<String>
    )
}
```

### WhisperModels

Model definitions and properties.

```kotlin
enum class WhisperModel(
    val fileName: String,
    val displayName: String,
    val parameters: Int,      // In millions
    val sizeMB: Int,          // Size in megabytes
    val speed: Int,           // Relative to realtime
    val description: String
) {
    TINY(
        fileName = "tiny.bin",
        displayName = "Tiny",
        parameters = 39,
        sizeMB = 39,
        speed = 32,
        description = "Fastest model, good for quick commands"
    ),
    
    BASE(
        fileName = "base.bin",
        displayName = "Base",
        parameters = 74,
        sizeMB = 74,
        speed = 16,
        description = "Balanced performance and accuracy"
    ),
    
    SMALL(
        fileName = "small.bin",
        displayName = "Small",
        parameters = 244,
        sizeMB = 244,
        speed = 6,
        description = "High accuracy for most use cases"
    ),
    
    MEDIUM(
        fileName = "medium.bin",
        displayName = "Medium",
        parameters = 769,
        sizeMB = 769,
        speed = 2,
        description = "Professional quality transcription"
    );
    
    /**
     * Get download URL for this model
     */
    fun getDownloadUrl(): String
    
    /**
     * Get size in bytes
     */
    fun getSizeBytes(): Long = sizeMB * 1024L * 1024L
    
    /**
     * Check if compatible with device architecture
     */
    fun isCompatibleWithDevice(): Boolean
}
```

### RecognitionResult

Enhanced recognition result with Whisper-specific fields.

```kotlin
data class RecognitionResult(
    val text: String,                    // Recognized text
    val confidence: Float,                // Overall confidence (0.0-1.0)
    val language: String?,                // Detected language code
    val languageProbability: Float?,      // Language detection confidence
    val translatedText: String?,          // English translation if enabled
    val segments: List<Segment>?,         // Text segments
    val wordTimestamps: List<WordTimestamp>?, // Word-level timing
    val processingTimeMs: Long,           // Processing duration
    val timestamp: Long = System.currentTimeMillis()
) {
    data class Segment(
        val text: String,
        val startTime: Long,
        val endTime: Long,
        val speaker: Int? = null         // Speaker ID if diarization enabled
    )
    
    data class WordTimestamp(
        val word: String,
        val startTime: Long,              // Start time in milliseconds
        val endTime: Long,                // End time in milliseconds
        val probability: Float            // Word confidence
    )
}
```

### WhisperError

Error types specific to Whisper engine.

```kotlin
sealed class WhisperError : Exception() {
    data class ModelNotFound(
        val model: WhisperModel
    ) : WhisperError()
    
    data class ModelLoadFailed(
        val reason: String
    ) : WhisperError()
    
    object AudioError : WhisperError()
    
    object OutOfMemory : WhisperError()
    
    data class NetworkError(
        val message: String
    ) : WhisperError()
    
    data class InitializationError(
        val message: String
    ) : WhisperError()
}
```

## UI Components

### WhisperModelDownloadDialog

Pre-built Compose dialog for model selection and download.

```kotlin
@Composable
fun WhisperModelDownloadDialog(
    modelManager: WhisperModelManager,
    onDismiss: () -> Unit,
    onModelSelected: (WhisperModel) -> Unit,
    modifier: Modifier = Modifier
)
```

### ModelSelectionCard

Compose card for displaying model information.

```kotlin
@Composable
fun ModelSelectionCard(
    model: WhisperModel,
    isRecommended: Boolean,
    isDownloaded: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
)
```

### DownloadProgressIndicator

Compose component for download progress.

```kotlin
@Composable
fun DownloadProgressIndicator(
    downloadState: DownloadState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
)
```

## Usage Examples

### Basic Whisper Setup

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var whisperEngine: WhisperEngine
    private lateinit var modelManager: WhisperModelManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        whisperEngine = WhisperEngine(this)
        modelManager = WhisperModelManager(this)
        
        lifecycleScope.launch {
            // Download model if needed
            if (!modelManager.isModelCached(WhisperModel.BASE)) {
                modelManager.downloadModel(WhisperModel.BASE).collect { state ->
                    when (state) {
                        is DownloadState.Success -> {
                            initializeWhisper()
                        }
                        is DownloadState.Error -> {
                            showError(state.message)
                        }
                    }
                }
            } else {
                initializeWhisper()
            }
        }
    }
    
    private suspend fun initializeWhisper() {
        val config = SpeechConfig(
            engine = SpeechEngine.WHISPER,
            language = "auto",
            mode = SpeechMode.FREE_SPEECH
        )
        
        if (whisperEngine.initialize(config)) {
            whisperEngine.setResultListener { result ->
                handleResult(result)
            }
            whisperEngine.startListening()
        }
    }
    
    private fun handleResult(result: RecognitionResult) {
        Log.d("Whisper", "Text: ${result.text}")
        Log.d("Whisper", "Language: ${result.language}")
        
        result.wordTimestamps?.forEach { word ->
            Log.d("Whisper", "${word.word} @ ${word.startTime}ms")
        }
    }
}
```

### VivokaService

Primary service for Vivoka VSDK speech recognition.

#### Initialization

```kotlin
class VivokaService(context: Context) {
    
    /**
     * Get singleton instance
     * @param context Application context
     * @return Singleton instance of VivokaService
     */
    companion object {
        @JvmStatic
        fun getInstance(context: Context): VivokaService
    }
    
    /**
     * Initialize service with configuration
     * @param config Speech recognition configuration
     * @return Result<Unit> indicating success or failure
     */
    suspend fun initialize(config: SpeechConfig): Result<Unit>
    
    /**
     * Update configuration
     * @param config New configuration to apply
     * @return Result<Unit> indicating success or failure
     */
    fun updateConfig(config: SpeechConfig): Result<Unit>
}
```

#### Recognition Control

```kotlin
/**
 * Start listening for speech
 * @return Result<Unit> indicating success or failure
 */
fun startListening(): Result<Unit>

/**
 * Stop listening
 * @return Result<Unit> indicating success or failure
 */
fun stopListening(): Result<Unit>

/**
 * Check if service is listening
 * @return Boolean indicating listening state
 */
fun isListening(): Boolean

/**
 * Check if service is ready
 * @return Boolean indicating ready state
 */
fun isReady(): Boolean
```

#### Command Management

```kotlin
/**
 * Set static commands (highest priority)
 * @param commands List of static command strings
 */
fun setStaticCommands(commands: List<String>)

/**
 * Set dynamic commands from UI scraping
 * @param commands List of dynamic command strings
 */
fun setDynamicCommands(commands: List<String>)
```

#### Result Handling

```kotlin
/**
 * Set result listener for recognition events
 * @param listener Callback for speech results
 */
fun setResultListener(listener: (SpeechResult) -> Unit)

/**
 * Result flow for reactive programming
 */
val resultFlow: SharedFlow<SpeechResult>
```

### Common Components

#### CommandCache

```kotlin
class CommandCache {
    /**
     * Set static commands with automatic deduplication
     * @param commands List of commands (max 500)
     */
    fun setStaticCommands(commands: List<String>)
    
    /**
     * Set dynamic commands with automatic deduplication
     * @param commands List of commands (max 200)
     */
    fun setDynamicCommands(commands: List<String>)
    
    /**
     * Add vocabulary word with LRU eviction
     * @param word Vocabulary word to add
     * @param isValid Whether word is valid (default true)
     */
    fun addVocabularyWord(word: String, isValid: Boolean = true)
    
    /**
     * Find matching command in cache
     * Priority: Static > Dynamic > Vocabulary
     * @param text Text to match
     * @return Matched command or null
     */
    fun findMatch(text: String): String?
    
    /**
     * Get all commands for grammar building
     * @return List of all unique commands
     */
    fun getAllCommands(): List<String>
    
    /**
     * Get cache statistics
     * @return CacheStats with counts and metrics
     */
    fun getStats(): CacheStats
}
```

#### TimeoutManager

```kotlin
class TimeoutManager(
    scope: CoroutineScope,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    /**
     * Start timeout timer
     * @param duration Timeout in milliseconds (must be positive)
     * @param onTimeout Callback when timeout occurs
     * @param dispatcher Coroutine dispatcher for callback
     */
    fun startTimeout(
        duration: Long, 
        onTimeout: () -> Unit,
        dispatcher: CoroutineDispatcher = defaultDispatcher
    )
    
    /**
     * Cancel active timeout
     */
    fun cancelTimeout()
    
    /**
     * Reset timeout with same or new duration
     * @param duration New timeout duration (uses previous if not specified)
     * @param onTimeout Callback when timeout occurs
     * @param dispatcher Coroutine dispatcher for callback
     */
    fun resetTimeout(
        duration: Long = currentTimeout, 
        onTimeout: () -> Unit,
        dispatcher: CoroutineDispatcher = defaultDispatcher
    )
    
    /**
     * Get remaining time in milliseconds
     * @return Remaining time or 0 if no timeout active
     */
    fun getRemainingTime(): Long
}
```

#### ResultProcessor

```kotlin
class ResultProcessor(commandCache: CommandCache) {
    /**
     * Process raw recognition result
     * @param text Recognized text
     * @param confidence Confidence score (0-1)
     * @param engine Speech engine used
     * @param isPartial Whether result is partial
     * @param alternatives Alternative recognitions
     * @return Processed result or null if filtered
     */
    suspend fun processResult(
        text: String,
        confidence: Float,
        engine: SpeechEngine,
        isPartial: Boolean = false,
        alternatives: List<String> = emptyList()
    ): SpeechResult?
    
    /**
     * Set confidence threshold
     * @param threshold Minimum confidence (0-1)
     */
    fun setConfidenceThreshold(threshold: Float)
    
    /**
     * Set recognition mode
     * @param mode Speech recognition mode
     */
    fun setMode(mode: SpeechMode)
    
    /**
     * Get processing statistics
     * @return ProcessingStats with metrics
     */
    fun getStatistics(): ProcessingStats
}
```

#### ServiceState

```kotlin
class ServiceState {
    /**
     * Service states
     */
    enum class State {
        UNINITIALIZED,
        INITIALIZING,
        READY,
        LISTENING,
        PROCESSING,
        PAUSED,
        ERROR,
        SHUTDOWN
    }
    
    /**
     * Update service state with validation
     * @param newState New state to transition to
     * @param error Error message if ERROR state
     * @return Boolean indicating if transition was valid
     */
    fun setState(newState: State, error: String? = null): Boolean
    
    /**
     * State flow for observing changes
     */
    val state: StateFlow<State>
    
    /**
     * Check if service is in usable state
     */
    fun isUsable(): Boolean
    
    /**
     * Get state history
     * @return List of state transitions
     */
    fun getHistory(): List<StateTransition>
}
```

### Configuration

#### SpeechConfig

```kotlin
data class SpeechConfig(
    val language: String = "en-US",
    val mode: SpeechMode = SpeechMode.DYNAMIC_COMMAND,
    val enableVAD: Boolean = true,
    val confidenceThreshold: Float = 0.7f,
    val maxRecordingDuration: Long = 30000,
    val timeoutDuration: Long = 5000,
    val engine: SpeechEngine = SpeechEngine.VOSK,
    val cloudApiKey: String? = null,
    val modelPath: String? = null
) {
    // Factory methods
    companion object {
        fun default(): SpeechConfig
        fun vosk(): SpeechConfig
        fun vivoka(): SpeechConfig
        fun googleSTT(): SpeechConfig
        fun googleCloud(apiKey: String): SpeechConfig
    }
    
    // Fluent modification methods
    fun withLanguage(lang: String): SpeechConfig
    fun withEngine(eng: SpeechEngine): SpeechConfig
    fun withMode(m: SpeechMode): SpeechConfig
    fun withVAD(enabled: Boolean): SpeechConfig
    fun withConfidenceThreshold(threshold: Float): SpeechConfig
    fun withTimeout(ms: Long): SpeechConfig
    
    // Validation
    fun validate(): Result<Unit>
    
    // Utility methods
    fun requiresNetwork(): Boolean
    fun requiresModelDownload(): Boolean
    fun getEstimatedMemoryUsage(): Int
}
```

### Models

#### SpeechResult

```kotlin
data class SpeechResult(
    val text: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isPartial: Boolean = false,
    val isFinal: Boolean = true,
    val alternatives: List<String> = emptyList(),
    val engine: String = "",
    val mode: String = "",
    val originalText: String = text
) {
    fun meetsThreshold(threshold: Float): Boolean
    fun isEmpty(): Boolean
    fun getBestAlternative(): String?
}
```

#### SpeechEngine

```kotlin
enum class SpeechEngine {
    WHISPER,      // OpenAI Whisper (39MB-769MB)
    VOSK,         // Open-source offline (~30MB)
    VIVOKA,       // Enterprise hybrid (~60MB)
    GOOGLE_STT,   // Android built-in (~20MB)
    GOOGLE_CLOUD  // Cloud REST API (~15MB, no SDK)
}
```

#### SpeechMode

```kotlin
enum class SpeechMode {
    STATIC_COMMAND,   // Pre-defined commands only
    DYNAMIC_COMMAND,  // UI-scraped commands
    DICTATION,        // Free-form text
    FREE_SPEECH       // Unrestricted speech
}
```

## Error Handling

All methods return `Result<T>` for proper error handling:

```kotlin
val result = vivokaService.initialize(config)
result.fold(
    onSuccess = { /* Handle success */ },
    onFailure = { error -> /* Handle error */ }
)
```

## Thread Safety

All components are thread-safe:
- CommandCache uses synchronized collections
- TimeoutManager uses coroutines with proper synchronization
- ResultProcessor uses thread-safe flows
- ServiceState uses atomic state updates

## Resource Management

Proper cleanup is essential:

```kotlin
// Shutdown service when done
vivokaService.shutdown()

// Clean up timeout manager
timeoutManager.cleanup()

// Clear caches
commandCache.clear()
resultProcessor.clear()
```

## Google Cloud Implementation Notes

### Lightweight REST API Approach

The Google Cloud Speech engine uses a lightweight REST API implementation instead of the heavy Google Cloud SDK:

#### Dependencies
```gradle
// Instead of heavy SDK (50MB+):
// implementation("com.google.cloud:google-cloud-speech:3.0.0")

// We use lightweight alternatives (~500KB total):
implementation("com.squareup.okhttp3:okhttp:4.12.0")  // HTTP client
implementation("com.google.code.gson:gson:2.10.1")    // JSON parsing
```

#### Benefits
- **50MB+ size reduction** in final APK
- **Faster startup** - no heavy SDK initialization
- **On-demand loading** - cloud features only loaded when needed
- **Simple integration** - direct REST API calls

#### Usage Example
```kotlin
// Configure Google Cloud with API key
val config = SpeechConfig.googleCloud(apiKey = "YOUR_API_KEY")
    .withLanguage("en-US")
    .withConfidenceThreshold(0.8f)

// Initialize service (lightweight)
val service = GoogleCloudService.getInstance(context)
service.initialize(config)

// Use like any other engine
service.startListening()
```
