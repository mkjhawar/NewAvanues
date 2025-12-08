# Speech Recognition Engine Split Architecture Plan

**Document Version:** 1.0.0  
**Created:** 2024-08-22  
**Purpose:** Reconstruction of planned engine split architecture  
**Status:** Implementation Guide

## Background
Based on conversation history, each speech recognition engine (Vosk, Vivoka, Google, Android, Azure) was to be split into multiple specialized components following SOLID principles.

## Planned Engine Split Structure

### For Each Engine (Vosk, Vivoka, Google, Android, Azure)

Each engine should have been split into the following components:

```
apps/SpeechRecognition/src/main/java/com/ai/engines/
├── vosk/
│   ├── VoskEngine.kt              # Main engine implementation
│   ├── VoskHandler.kt             # Event and callback handling
│   ├── VoskManager.kt             # Lifecycle and resource management
│   ├── VoskProcessor.kt           # Audio processing pipeline
│   ├── VoskConfig.kt              # Configuration specific to Vosk
│   ├── VoskModels.kt              # Model management
│   ├── VoskUtils.kt               # Utility functions
│   └── VoskConstants.kt           # Constants and enums
├── vivoka/
│   ├── VivokaEngine.kt            # Main Vivoka engine (already exists)
│   ├── VivokaHandler.kt           # Event and callback handling
│   ├── VivokaManager.kt           # Lifecycle and resource management
│   ├── VivokaProcessor.kt         # Audio processing pipeline
│   ├── VivokaConfig.kt            # Configuration specific to Vivoka
│   ├── VivokaLicenseManager.kt   # Vivoka-specific licensing
│   ├── VivokaUtils.kt             # Utility functions
│   └── VivokaConstants.kt         # Constants and enums
├── google/
│   ├── GoogleCloudEngine.kt      # Main Google Cloud engine (from implementations)
│   ├── GoogleHandler.kt          # Event and callback handling
│   ├── GoogleManager.kt          # Lifecycle and resource management
│   ├── GoogleProcessor.kt        # Audio processing pipeline
│   ├── GoogleConfig.kt           # Configuration specific to Google
│   ├── GoogleAuthManager.kt      # Google Cloud authentication
│   ├── GoogleUtils.kt            # Utility functions
│   └── GoogleConstants.kt        # Constants and enums
├── android/
│   ├── AndroidSTTEngine.kt       # Main Android STT engine (from implementations)
│   ├── AndroidHandler.kt         # Event and callback handling
│   ├── AndroidManager.kt         # Lifecycle and resource management
│   ├── AndroidProcessor.kt       # Audio processing pipeline
│   ├── AndroidConfig.kt          # Configuration specific to Android
│   ├── AndroidIntentBuilder.kt   # Android-specific intents
│   ├── AndroidUtils.kt           # Utility functions
│   └── AndroidConstants.kt       # Constants and enums
└── azure/
    ├── AzureEngine.kt             # Main Azure engine (from implementations)
    ├── AzureHandler.kt            # Event and callback handling
    ├── AzureManager.kt            # Lifecycle and resource management
    ├── AzureProcessor.kt          # Audio processing pipeline
    ├── AzureConfig.kt             # Configuration specific to Azure
    ├── AzureAuthManager.kt        # Azure authentication
    ├── AzureUtils.kt              # Utility functions
    └── AzureConstants.kt          # Constants and enums
```

## Component Responsibilities

### 1. Engine.kt (Main Implementation)
```kotlin
class VoskEngine : IRecognitionEngine {
    private val handler = VoskHandler()
    private val manager = VoskManager()
    private val processor = VoskProcessor()
    
    override suspend fun initialize(config: EngineConfig): Boolean {
        return manager.initialize(config)
    }
    
    override suspend fun startRecognition() {
        processor.startProcessing()
    }
}
```

### 2. Handler.kt (Event Handling)
```kotlin
class VoskHandler {
    // Handle recognition results
    fun onResult(result: String) { }
    
    // Handle partial results
    fun onPartialResult(partial: String) { }
    
    // Handle errors
    fun onError(error: Exception) { }
    
    // Handle state changes
    fun onStateChanged(state: RecognitionState) { }
}
```

### 3. Manager.kt (Lifecycle Management)
```kotlin
class VoskManager {
    // Initialize resources
    suspend fun initialize(config: VoskConfig): Boolean
    
    // Load models
    suspend fun loadModel(modelPath: String): Boolean
    
    // Release resources
    fun release()
    
    // Manage memory
    fun optimizeMemory()
}
```

### 4. Processor.kt (Audio Processing)
```kotlin
class VoskProcessor {
    // Process audio buffer
    fun processAudioBuffer(buffer: ByteArray): RecognitionResult
    
    // Start continuous processing
    fun startProcessing()
    
    // Stop processing
    fun stopProcessing()
    
    // Apply audio filters
    fun applyNoiseReduction(audio: ByteArray): ByteArray
}
```

### 5. Config.kt (Configuration)
```kotlin
data class VoskConfig(
    val modelPath: String,
    val sampleRate: Int = 16000,
    val bufferSize: Int = 4096,
    val enablePartialResults: Boolean = true,
    val maxAlternatives: Int = 3,
    val vocabulary: List<String>? = null
)
```

### 6. Models.kt (Model Management)
```kotlin
class VoskModels {
    // Download model
    suspend fun downloadModel(language: String): String
    
    // Verify model integrity
    fun verifyModel(modelPath: String): Boolean
    
    // Get available models
    fun getAvailableModels(): List<ModelInfo>
    
    // Clean old models
    fun cleanupOldModels()
}
```

### 7. Utils.kt (Utilities)
```kotlin
object VoskUtils {
    // Convert audio formats
    fun convertAudioFormat(input: ByteArray, from: AudioFormat, to: AudioFormat): ByteArray
    
    // Calculate confidence scores
    fun calculateConfidence(result: RecognitionResult): Float
    
    // Parse Vosk JSON responses
    fun parseVoskResponse(json: String): RecognitionResult
}
```

### 8. Constants.kt (Constants)
```kotlin
object VoskConstants {
    const val DEFAULT_SAMPLE_RATE = 16000
    const val DEFAULT_BUFFER_SIZE = 4096
    const val MIN_CONFIDENCE = 0.5f
    const val MAX_SILENCE_MS = 2000
    
    enum class VoskState {
        IDLE,
        INITIALIZING,
        READY,
        LISTENING,
        PROCESSING,
        ERROR
    }
}
```

## Implementation Example: VoskEngine Split

### VoskEngine.kt
```kotlin
package com.ai.engines.vosk

import com.ai.engines.IRecognitionEngine
import kotlinx.coroutines.*

class VoskEngine : IRecognitionEngine {
    private val handler = VoskHandler()
    private val manager = VoskManager()
    private val processor = VoskProcessor()
    private val config = VoskConfig()
    
    private var isInitialized = false
    private var recognitionJob: Job? = null
    
    override suspend fun initialize(engineConfig: EngineConfig): Boolean {
        return try {
            // Convert generic config to Vosk-specific
            val voskConfig = config.fromEngineConfig(engineConfig)
            
            // Initialize manager with config
            manager.initialize(voskConfig)
            
            // Setup handler callbacks
            handler.setCallbacks(
                onResult = { result -> processResult(result) },
                onError = { error -> handleError(error) }
            )
            
            // Initialize processor
            processor.initialize(voskConfig)
            
            isInitialized = true
            true
        } catch (e: Exception) {
            handler.onError(e)
            false
        }
    }
    
    override suspend fun startRecognition() {
        if (!isInitialized) {
            throw IllegalStateException("Engine not initialized")
        }
        
        recognitionJob = CoroutineScope(Dispatchers.IO).launch {
            processor.startProcessing { audioData ->
                val result = processAudio(audioData)
                handler.onResult(result)
            }
        }
    }
    
    override fun stopRecognition() {
        recognitionJob?.cancel()
        processor.stopProcessing()
    }
    
    override fun release() {
        stopRecognition()
        manager.release()
        processor.release()
    }
}
```

### VoskHandler.kt
```kotlin
package com.ai.engines.vosk

import com.ai.api.RecognitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VoskHandler {
    private val _recognitionState = MutableStateFlow(VoskConstants.VoskState.IDLE)
    val recognitionState: StateFlow<VoskConstants.VoskState> = _recognitionState
    
    private var onResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((Exception) -> Unit)? = null
    private var onPartialCallback: ((String) -> Unit)? = null
    
    fun setCallbacks(
        onResult: (String) -> Unit = {},
        onError: (Exception) -> Unit = {},
        onPartial: (String) -> Unit = {}
    ) {
        onResultCallback = onResult
        onErrorCallback = onError
        onPartialCallback = onPartial
    }
    
    fun onResult(result: String) {
        _recognitionState.value = VoskConstants.VoskState.PROCESSING
        onResultCallback?.invoke(result)
        _recognitionState.value = VoskConstants.VoskState.READY
    }
    
    fun onPartialResult(partial: String) {
        onPartialCallback?.invoke(partial)
    }
    
    fun onError(error: Exception) {
        _recognitionState.value = VoskConstants.VoskState.ERROR
        onErrorCallback?.invoke(error)
    }
    
    fun onStateChanged(newState: VoskConstants.VoskState) {
        _recognitionState.value = newState
    }
}
```

### VoskManager.kt
```kotlin
package com.ai.engines.vosk

import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

class VoskManager {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private val models = VoskModels()
    
    suspend fun initialize(config: VoskConfig): Boolean {
        return try {
            // Ensure model exists
            val modelPath = config.modelPath.ifEmpty {
                models.downloadModel(config.language)
            }
            
            // Verify model
            if (!models.verifyModel(modelPath)) {
                throw IllegalStateException("Invalid model at $modelPath")
            }
            
            // Load model
            model = Model(modelPath)
            
            // Create recognizer
            recognizer = Recognizer(model, config.sampleRate.toFloat()).apply {
                if (config.vocabulary != null) {
                    setWords(config.vocabulary.joinToString(" "))
                }
                setMaxAlternatives(config.maxAlternatives)
                setPartialWords(config.enablePartialResults)
            }
            
            true
        } catch (e: Exception) {
            release()
            throw e
        }
    }
    
    fun getRecognizer(): Recognizer {
        return recognizer ?: throw IllegalStateException("Recognizer not initialized")
    }
    
    fun release() {
        recognizer?.close()
        recognizer = null
        model?.close()
        model = null
    }
    
    fun optimizeMemory() {
        // Implement memory optimization
        System.gc()
    }
}
```

## Benefits of Split Architecture

1. **Single Responsibility**: Each class has one clear purpose
2. **Maintainability**: Easier to locate and fix issues
3. **Testability**: Each component can be tested independently
4. **Reusability**: Components can be reused across engines
5. **Flexibility**: Easy to swap implementations
6. **Scalability**: Can add features without affecting other components

## Migration Strategy

1. **Phase 1**: Create folder structure for each engine
2. **Phase 2**: Extract handlers from main engine files
3. **Phase 3**: Extract managers and lifecycle code
4. **Phase 4**: Extract processors and audio handling
5. **Phase 5**: Create engine-specific configurations
6. **Phase 6**: Add utility classes and constants
7. **Phase 7**: Update main engine to use components
8. **Phase 8**: Test each engine independently

## Files to Create (Per Engine)

For complete implementation, create these files for each engine:

### Vosk (8 files)
- `/engines/vosk/VoskEngine.kt`
- `/engines/vosk/VoskHandler.kt`
- `/engines/vosk/VoskManager.kt`
- `/engines/vosk/VoskProcessor.kt`
- `/engines/vosk/VoskConfig.kt`
- `/engines/vosk/VoskModels.kt`
- `/engines/vosk/VoskUtils.kt`
- `/engines/vosk/VoskConstants.kt`

### Vivoka (8 files)
- Similar structure with VivokaLicenseManager.kt

### Google (8 files)
- Similar structure with GoogleAuthManager.kt

### Android (8 files)
- Similar structure with AndroidIntentBuilder.kt

### Azure (8 files)
- Similar structure with AzureAuthManager.kt

**Total: 40 new files** replacing the current 7 monolithic engine files.

---
**Document Status:** Implementation guide for engine split architecture  
**Next Steps:** Begin Phase 1 with folder creation