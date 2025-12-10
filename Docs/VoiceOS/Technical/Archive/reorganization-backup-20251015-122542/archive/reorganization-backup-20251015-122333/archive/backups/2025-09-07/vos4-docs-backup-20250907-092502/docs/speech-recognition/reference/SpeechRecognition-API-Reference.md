<!--
filename: SPEECHRECOGNITION_API_REFERENCE.md
created: 2025-01-23 20:45:00 PST
author: VOS4 Development Team
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Complete API documentation for VOS4 Speech Recognition Module
last-modified: 2025-01-23 20:45:00 PST
version: 2.0.0
-->

# Speech Recognition API Reference (VOS4)
> Complete API documentation for VOS4 Speech Recognition Module
> Version: 2.0.0 (Updated for VOS4 Direct Implementation)
> Last Updated: 2025-01-23 20:45:00 PST (Migrated from docs-old)

**Note:** This document has been migrated from `/docs-old/modules/speechrecognition/` and updated for VOS4's direct implementation architecture.

## Overview

VOS4 Speech Recognition module provides unified access to 6 speech recognition engines with direct implementation pattern (no interfaces) and zero overhead architecture.

## Supported Engines

1. **Vosk** - Offline, lightweight
2. **Vivoka** - Cloud-based, high accuracy
3. **Android STT** - System native
4. **Google Cloud STT** - Enterprise grade
5. **Whisper** - OpenAI's model
6. **Azure STT** - Microsoft's service

## Direct Implementation API (VOS4)

### Main Module Class

```kotlin
class SpeechRecognitionModule(private val context: Context) {
    // Lifecycle
    suspend fun initialize(config: UnifiedConfiguration? = null): Boolean
    suspend fun shutdown()
    
    // Recognition Control
    suspend fun startRecognition(
        engine: RecognitionEngine = RecognitionEngine.AUTO,
        mode: RecognitionMode = RecognitionMode.COMMAND
    ): Boolean
    
    suspend fun stopRecognition(): Boolean
    suspend fun pauseRecognition(): Boolean
    suspend fun resumeRecognition(): Boolean
    
    // Configuration
    suspend fun setEngine(engine: RecognitionEngine): Boolean
    suspend fun setLanguage(languageTag: String): Boolean
    suspend fun setVocabulary(commands: List<String>): Boolean
    suspend fun updateConfig(config: UnifiedConfiguration): Boolean
    
    // Results - Direct Flow access
    fun getResults(): Flow<RecognitionResult>
    fun getLastResult(): RecognitionResult?
    
    // State
    fun isRecognizing(): Boolean
    fun getCurrentEngine(): RecognitionEngine
    fun getSupportedEngines(): List<RecognitionEngine>
    fun getEngineCapabilities(engine: RecognitionEngine): EngineCapabilities
}
```

### Engine-Specific Direct Access

```kotlin
// Direct engine access (VOS4 pattern)
class VoskEngine(private val context: Context) {
    suspend fun initializeWithModel(modelPath: String): Boolean
    suspend fun processAudioChunk(audioData: ByteArray): RecognitionResult?
    suspend fun setGrammar(grammar: String): Boolean
    fun isModelLoaded(): Boolean
}

class VivokaEngine(private val context: Context) {
    suspend fun initializeWithApiKey(apiKey: String): Boolean
    suspend fun startCloudRecognition(audioStream: Flow<ByteArray>): Flow<RecognitionResult>
    suspend fun setCloudConfig(config: VivokaConfig): Boolean
}

class AndroidSTTEngine(private val context: Context) {
    suspend fun initializeSystemSTT(): Boolean
    suspend fun startSystemRecognition(intent: Intent): Boolean
    fun getSystemLanguages(): List<String>
}
```

## Data Types

### RecognitionResult
```kotlin
data class RecognitionResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val engine: RecognitionEngine,
    val alternatives: List<Alternative> = emptyList(),
    val language: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val processingTime: Long = 0L
) {
    data class Alternative(
        val text: String,
        val confidence: Float
    )
}
```

### UnifiedConfiguration
```kotlin
data class UnifiedConfiguration(
    val preferredEngine: RecognitionEngine = RecognitionEngine.AUTO,
    val fallbackEngines: List<RecognitionEngine> = emptyList(),
    val language: String = "en-US",
    val mode: RecognitionMode = RecognitionMode.COMMAND,
    
    // Audio settings
    val sampleRate: Int = 16000,
    val channels: Int = 1,
    val bufferSize: Int = 1024,
    
    // Recognition settings
    val confidenceThreshold: Float = 0.7f,
    val maxAlternatives: Int = 3,
    val enablePartialResults: Boolean = true,
    val enableVAD: Boolean = true,
    
    // Engine-specific configs
    val voskConfig: VoskConfig? = null,
    val vivokaConfig: VivokaConfig? = null,
    val androidSTTConfig: AndroidSTTConfig? = null,
    val googleCloudConfig: GoogleCloudConfig? = null,
    val whisperConfig: WhisperConfig? = null,
    val azureConfig: AzureConfig? = null
)
```

### Engine Capabilities
```kotlin
data class EngineCapabilities(
    val engine: RecognitionEngine,
    val supportsOffline: Boolean,
    val supportsOnline: Boolean,
    val supportedLanguages: List<String>,
    val supportedSampleRates: List<Int>,
    val supportedModes: List<RecognitionMode>,
    val maxAudioLengthSeconds: Int?,
    val requiresInternet: Boolean,
    val requiresApiKey: Boolean,
    val memoryUsageMB: Int,
    val batteryUsage: BatteryUsage,
    val accuracy: AccuracyLevel,
    val latencyMs: IntRange
)

enum class BatteryUsage { LOW, MEDIUM, HIGH }
enum class AccuracyLevel { LOW, MEDIUM, HIGH, VERY_HIGH }
```

## Recognition Engines

### RecognitionEngine
```kotlin
enum class RecognitionEngine {
    AUTO,           // Automatic selection
    VOSK,           // Offline, lightweight
    VIVOKA,         // Cloud, high accuracy
    ANDROID_STT,    // System native
    GOOGLE_CLOUD,   // Enterprise grade
    WHISPER,        // OpenAI model
    AZURE           // Microsoft service
}
```

### RecognitionMode
```kotlin
enum class RecognitionMode {
    COMMAND,        // Short commands
    DICTATION,      // Long-form text
    WAKE_WORD,      // Always listening
    CONTINUOUS,     // Continuous recognition
    MIXED          // Adaptive mode
}
```

## Engine-Specific Configurations

### VoskConfig
```kotlin
data class VoskConfig(
    val modelPath: String,
    val grammar: String? = null,
    val enableWords: Boolean = false,
    val enableAlternatives: Boolean = false,
    val maxAlternatives: Int = 1
)
```

### VivokaConfig
```kotlin
data class VivokaConfig(
    val apiKey: String,
    val serverUrl: String = "https://api.vivoka.com",
    val useWebSocket: Boolean = true,
    val enableNLP: Boolean = false,
    val customVocabulary: List<String> = emptyList()
)
```

### GoogleCloudConfig
```kotlin
data class GoogleCloudConfig(
    val apiKey: String,
    val enableAutomaticPunctuation: Boolean = true,
    val enableWordTimeOffsets: Boolean = false,
    val model: String = "default",
    val useEnhancedModel: Boolean = false
)
```

## Usage Examples

### Basic Usage (VOS4 Direct Pattern)
```kotlin
// Initialize module
val speechModule = SpeechRecognitionModule(context)
val success = speechModule.initialize()

// Configure for commands
val config = UnifiedConfiguration(
    preferredEngine = RecognitionEngine.VOSK,
    language = "en-US",
    mode = RecognitionMode.COMMAND,
    confidenceThreshold = 0.8f
)
speechModule.updateConfig(config)

// Start recognition
speechModule.startRecognition()

// Collect results
speechModule.getResults().collect { result ->
    if (result.isFinal && result.confidence >= 0.8f) {
        processCommand(result.text)
    }
}
```

### Multi-Engine Fallback
```kotlin
val config = UnifiedConfiguration(
    preferredEngine = RecognitionEngine.VOSK,
    fallbackEngines = listOf(
        RecognitionEngine.ANDROID_STT,
        RecognitionEngine.VIVOKA
    )
)

// Automatic fallback if Vosk fails
speechModule.updateConfig(config)
```

### Engine-Specific Setup
```kotlin
// Vosk offline setup
val voskConfig = VoskConfig(
    modelPath = "/data/vosk-model-en-us",
    grammar = "one two three four five"
)

val config = UnifiedConfiguration(
    preferredEngine = RecognitionEngine.VOSK,
    voskConfig = voskConfig
)

// Vivoka cloud setup
val vivokaConfig = VivokaConfig(
    apiKey = "your-api-key",
    enableNLP = true,
    customVocabulary = listOf("navigate", "volume", "brightness")
)
```

## Error Handling

### Exception Types
```kotlin
sealed class SpeechRecognitionException(message: String) : Exception(message) {
    class EngineNotAvailable(engine: RecognitionEngine) : 
        SpeechRecognitionException("Engine not available: $engine")
    
    class ModelNotFound(modelPath: String) : 
        SpeechRecognitionException("Model not found: $modelPath")
    
    class NetworkError(cause: Throwable) : 
        SpeechRecognitionException("Network error: ${cause.message}")
    
    class AudioError(message: String) : 
        SpeechRecognitionException("Audio error: $message")
    
    class ConfigurationError(message: String) : 
        SpeechRecognitionException("Configuration error: $message")
}
```

### Error Handling Pattern
```kotlin
try {
    val result = speechModule.startRecognition()
    if (!result) {
        // Handle failure
        Log.e(TAG, "Failed to start recognition")
    }
} catch (e: SpeechRecognitionException.EngineNotAvailable) {
    // Try fallback engine
    speechModule.setEngine(RecognitionEngine.ANDROID_STT)
} catch (e: SpeechRecognitionException.NetworkError) {
    // Switch to offline engine
    speechModule.setEngine(RecognitionEngine.VOSK)
}
```

## Performance Characteristics

### Engine Performance (Typical)
| Engine | Latency | Memory | Battery | Accuracy | Offline |
|--------|---------|--------|---------|----------|---------|
| **Vosk** | 100-300ms | 30MB | Low | Good | ✅ |
| **Vivoka** | 200-500ms | 15MB | Medium | Very High | ❌ |
| **Android STT** | 200-400ms | 20MB | Low | Good | ❌ |
| **Google Cloud** | 300-700ms | 10MB | Low | Very High | ❌ |
| **Whisper** | 500-1000ms | 100MB | High | Very High | ✅ |
| **Azure** | 300-600ms | 10MB | Low | High | ❌ |

### VOS4 Optimizations
- **Direct method calls**: No interface overhead
- **Unified configuration**: Single config object
- **Zero adapters**: Direct engine access
- **Memory pooling**: Shared audio buffers
- **Smart fallback**: Automatic engine switching

## Integration with VOS4 Modules

### CommandsMGR Integration
```kotlin
// Direct integration pattern
class CommandsModule(private val context: Context) {
    private val speechModule = SpeechRecognitionModule(context)
    
    suspend fun initialize() {
        speechModule.initialize()
        
        // Direct result processing
        speechModule.getResults().collect { result ->
            if (result.isFinal) {
                processCommand(result.text)
            }
        }
    }
}
```

### VoiceAccessibility Integration
```kotlin
// Direct accessibility commands
speechModule.getResults().collect { result ->
    if (result.confidence >= 0.8f) {
        when (result.text.lowercase()) {
            "click button" -> accessibilityService.performClick()
            "scroll down" -> accessibilityService.performScroll()
            "go back" -> accessibilityService.performBack()
        }
    }
}
```

### DataMGR Integration (ObjectBox)
```kotlin
@Entity
data class RecognitionHistory(
    @Id var id: Long = 0,
    val text: String,
    val confidence: Float,
    val engine: String,
    val language: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Direct ObjectBox storage
class RecognitionRepository(private val box: Box<RecognitionHistory>) {
    suspend fun saveResult(result: RecognitionResult) = withContext(Dispatchers.IO) {
        val history = RecognitionHistory(
            text = result.text,
            confidence = result.confidence,
            engine = result.engine.name,
            language = result.language ?: "unknown"
        )
        box.put(history)
    }
}
```

## Testing

### Unit Testing
```kotlin
class SpeechRecognitionModuleTest {
    @Test
    fun testDirectModuleInitialization() = runTest {
        val module = SpeechRecognitionModule(mockContext)
        val result = module.initialize()
        assertTrue(result)
    }
    
    @Test
    fun testEngineSelection() = runTest {
        val module = SpeechRecognitionModule(mockContext)
        module.setEngine(RecognitionEngine.VOSK)
        assertEquals(RecognitionEngine.VOSK, module.getCurrentEngine())
    }
}
```

### Integration Testing
```kotlin
@Test
fun testEndToEndRecognition() = runTest {
    val module = SpeechRecognitionModule(context)
    module.initialize()
    
    val results = mutableListOf<RecognitionResult>()
    val job = launch {
        module.getResults().take(1).collect { results.add(it) }
    }
    
    // Simulate audio input
    module.startRecognition()
    // ... provide test audio
    
    job.join()
    assertTrue(results.isNotEmpty())
    assertTrue(results.first().confidence > 0.0f)
}
```

## Migration from VOS3

### Breaking Changes
- Removed `IRecognitionModule` interface
- Direct instantiation instead of factory pattern
- Simplified configuration (single UnifiedConfiguration)
- Direct Flow access for results

### Migration Guide
```kotlin
// VOS3 (Old)
val factory = RecognitionEngineFactory()
val engine: IRecognitionEngine = factory.create(RecognitionEngine.VOSK)
val module: IRecognitionModule = RecognitionModuleImpl(engine)

// VOS4 (New) - Direct implementation
val module = SpeechRecognitionModule(context)
```

## Best Practices

1. **Engine Selection**: Use AUTO for general purpose, specific engines for specialized needs
2. **Configuration**: Set up UnifiedConfiguration once, reuse across sessions
3. **Error Handling**: Always implement fallback engines for reliability
4. **Memory Management**: Monitor memory usage with heavy engines like Whisper
5. **Battery Usage**: Prefer offline engines for battery-sensitive applications
6. **Accuracy vs Speed**: Balance confidence threshold with response time needs

---

*Migrated from docs-old/modules/speechrecognition/SPEECHRECOGNITION_API.md*  
*Updated for VOS4 direct implementation architecture*  
*Last Updated: 2025-01-23*