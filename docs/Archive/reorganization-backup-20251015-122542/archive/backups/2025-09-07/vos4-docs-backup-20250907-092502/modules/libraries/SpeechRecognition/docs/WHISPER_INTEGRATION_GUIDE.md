# Whisper Speech Recognition Integration Guide

## Overview

The Whisper engine provides state-of-the-art offline speech recognition using OpenAI's Whisper model, compiled natively for Android devices. This guide covers integration, model management, and best practices.

## Table of Contents
- [Features](#features)
- [Architecture Support](#architecture-support)
- [Quick Start](#quick-start)
- [Model Management](#model-management)
- [Language Support](#language-support)
- [UI Integration](#ui-integration)
- [Performance Optimization](#performance-optimization)
- [Troubleshooting](#troubleshooting)

## Features

- ✅ **Offline Recognition**: No internet required after model download
- ✅ **99+ Languages**: Automatic language detection
- ✅ **Multiple Model Sizes**: From tiny (39MB) to large (1.5GB)
- ✅ **Real-time Processing**: Stream audio with minimal latency
- ✅ **Translation**: Translate any language to English
- ✅ **Word Timestamps**: Get precise timing for each word
- ✅ **Noise Reduction**: Built-in VAD and noise filtering
- ✅ **Device Optimization**: Automatic model selection based on device

## Architecture Support

| Architecture | Support Level | Recommended Models | Notes |
|-------------|--------------|-------------------|--------|
| ARM64-v8a | ✅ Full | All models | Modern devices (2014+) |
| ARMv7 | ⚠️ Limited | Tiny only | Older 32-bit devices |
| x86/x86_64 | ❌ Removed | N/A | Not supported (reduced APK size) |

## Quick Start

### 1. Add Dependency

```kotlin
// In your app's build.gradle.kts
dependencies {
    implementation(project(":libraries:SpeechRecognition"))
}
```

### 2. Initialize Engine

```kotlin
import com.augmentalis.voiceos.speech.engines.whisper.WhisperEngine
import com.augmentalis.speechrecognition.SpeechConfig

class MyActivity : AppCompatActivity() {
    private lateinit var whisperEngine: WhisperEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Whisper engine
        whisperEngine = WhisperEngine(this)
        
        lifecycleScope.launch {
            val config = SpeechConfig(
                mode = SpeechMode.DYNAMIC_COMMAND,
                confidenceThreshold = 0.7f,
                voiceEnabled = true
            )
            
            val success = whisperEngine.initialize(config)
            if (success) {
                // Engine ready - model will auto-download if needed
                whisperEngine.startListening()
            }
        }
    }
}
```

### 3. Handle Results

```kotlin
whisperEngine.setResultListener { result ->
    Log.d("Whisper", "Recognized: ${result.text}")
    Log.d("Whisper", "Language: ${result.language}")
    Log.d("Whisper", "Confidence: ${result.confidence}")
}

whisperEngine.setErrorListener { message, errorCode ->
    Log.e("Whisper", "Error: $message (Code: $errorCode)")
}
```

## Model Management

### Available Models

| Model | Size | Speed | Accuracy | Memory | Use Case |
|-------|------|-------|----------|--------|----------|
| **Tiny** | 39MB | 32x realtime | Basic | 150MB | Quick commands, low-end devices |
| **Tiny.en** | 39MB | 32x realtime | Good (English) | 150MB | English-only, faster |
| **Base** | 74MB | 16x realtime | Good | 230MB | Balanced performance |
| **Base.en** | 74MB | 16x realtime | Better (English) | 230MB | English-only, recommended |
| **Small** | 244MB | 6x realtime | Very Good | 500MB | High accuracy |
| **Small.en** | 244MB | 6x realtime | Excellent (English) | 500MB | Professional English |
| **Medium** | 769MB | 2x realtime | Excellent | 1.2GB | Maximum accuracy |
| **Medium.en** | 769MB | 2x realtime | Best (English) | 1.2GB | Professional use |

> **Note**: Large models (1.5GB) are not recommended for mobile devices due to memory constraints.

### Automatic Model Selection

The engine automatically selects the best model based on device capabilities:

```kotlin
// Get recommended model for current device
val modelManager = WhisperModelManager(context)
val recommendedModel = modelManager.getRecommendedModel()

Log.d("Whisper", "Recommended model: ${recommendedModel.modelName}")
```

### Manual Model Selection

```kotlin
// Change model size manually
lifecycleScope.launch {
    val success = whisperEngine.changeModel(WhisperModelSize.SMALL)
    if (success) {
        Log.d("Whisper", "Model changed to SMALL")
    }
}
```

### Model Download Management

```kotlin
class ModelDownloadExample {
    private val modelManager = WhisperModelManager(context)
    
    fun downloadModel() {
        lifecycleScope.launch {
            // Check if model is already downloaded
            if (modelManager.isModelDownloaded(WhisperModelSize.BASE)) {
                Log.d("Whisper", "Model already downloaded")
                return@launch
            }
            
            // Monitor download progress
            modelManager.downloadState.collect { state ->
                when (state) {
                    is ModelDownloadState.Downloading -> {
                        updateProgress(state.progress)
                        updateStatus("Downloading: ${state.downloadedMB}MB / ${state.totalMB}MB")
                    }
                    is ModelDownloadState.Verifying -> {
                        updateStatus("Verifying model...")
                    }
                    is ModelDownloadState.Completed -> {
                        updateStatus("Model ready!")
                        initializeWhisper(state.modelPath)
                    }
                    is ModelDownloadState.Error -> {
                        showError(state.message)
                    }
                }
            }
            
            // Start download
            modelManager.downloadModel(WhisperModelSize.BASE)
        }
    }
    
    fun cancelDownload() {
        modelManager.cancelDownload()
    }
    
    fun deleteModel() {
        modelManager.deleteModel(WhisperModelSize.BASE)
    }
    
    fun getModelInfo() {
        val downloadedModels = modelManager.getDownloadedModels()
        val totalSize = modelManager.getTotalDiskUsageMB()
        
        Log.d("Whisper", "Downloaded models: $downloadedModels")
        Log.d("Whisper", "Total disk usage: ${totalSize}MB")
    }
}
```

## Language Support

### Supported Languages (99+)

Whisper supports automatic language detection for 99 languages. Here are the most common:

| Language | Code | Quality | Notes |
|----------|------|---------|-------|
| English | en | Excellent | Best support |
| Spanish | es | Excellent | High accuracy |
| French | fr | Excellent | High accuracy |
| German | de | Excellent | High accuracy |
| Italian | it | Excellent | High accuracy |
| Portuguese | pt | Excellent | High accuracy |
| Russian | ru | Very Good | Good accuracy |
| Chinese | zh | Very Good | Mandarin/Cantonese |
| Japanese | ja | Very Good | Good accuracy |
| Korean | ko | Very Good | Good accuracy |
| Arabic | ar | Good | Multiple dialects |
| Hindi | hi | Good | Good accuracy |
| Turkish | tr | Good | Good accuracy |
| Polish | pl | Good | Good accuracy |
| Dutch | nl | Good | Good accuracy |
| Swedish | sv | Good | Good accuracy |
| Norwegian | no | Good | Good accuracy |
| Danish | da | Good | Good accuracy |
| Finnish | fi | Good | Good accuracy |
| Greek | el | Good | Good accuracy |
| Hebrew | he | Good | Good accuracy |
| Indonesian | id | Good | Good accuracy |
| Malay | ms | Good | Good accuracy |
| Thai | th | Good | Good accuracy |
| Vietnamese | vi | Good | Good accuracy |
| Czech | cs | Good | Good accuracy |
| Romanian | ro | Good | Good accuracy |
| Hungarian | hu | Good | Good accuracy |
| Ukrainian | uk | Good | Good accuracy |

[Full language list](https://github.com/openai/whisper#available-models-and-languages)

### Language Detection

```kotlin
// Automatic language detection
whisperEngine.setResultListener { result ->
    when (result.language) {
        "en" -> Log.d("Whisper", "Detected English")
        "es" -> Log.d("Whisper", "Detected Spanish")
        "fr" -> Log.d("Whisper", "Detected French")
        else -> Log.d("Whisper", "Detected: ${result.language}")
    }
}

// Get detected language after recognition
val detectedLanguage = whisperEngine.getDetectedLanguage()
```

### Force Specific Language

```kotlin
// Configure for specific language (faster, more accurate)
val config = WhisperConfig(
    // ... other settings
    enableLanguageDetection = false
)

// In TranscriptionConfig
val transcriptionConfig = TranscriptionConfig(
    language = "es"  // Force Spanish
)
```

### Translation to English

```kotlin
// Enable translation
whisperEngine.setTranslationEnabled(
    enabled = true,
    targetLanguage = "en"  // Always translates to English
)

// Result will contain both original and translation
whisperEngine.setResultListener { result ->
    Log.d("Whisper", "Original: ${result.text}")
    Log.d("Whisper", "Translation: ${result.translation}")
}
```

## UI Integration

### Model Download UI Component

See [WhisperModelDownloadUI.kt](../src/main/java/com/augmentalis/speechrecognition/ui/WhisperModelDownloadUI.kt) for a complete Compose UI implementation.

Basic usage:

```kotlin
@Composable
fun MyScreen() {
    WhisperModelDownloadDialog(
        modelManager = WhisperModelManager(LocalContext.current),
        onDismiss = { /* Handle dismiss */ },
        onModelSelected = { model ->
            // Initialize Whisper with selected model
        }
    )
}
```

### Progress Indicators

```kotlin
@Composable
fun ModelDownloadProgress(state: ModelDownloadState) {
    when (state) {
        is ModelDownloadState.Idle -> {
            Text("Select a model to download")
        }
        is ModelDownloadState.Downloading -> {
            Column {
                LinearProgressIndicator(
                    progress = state.progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${state.downloadedMB.toInt()}MB / ${state.totalMB.toInt()}MB")
            }
        }
        is ModelDownloadState.Verifying -> {
            CircularProgressIndicator()
            Text("Verifying...")
        }
        is ModelDownloadState.Completed -> {
            Text("✅ Model ready!", color = Color.Green)
        }
        is ModelDownloadState.Error -> {
            Text("❌ ${state.message}", color = Color.Red)
        }
    }
}
```

## Performance Optimization

### Memory Management

```kotlin
// Check available memory before loading model
fun checkMemoryBeforeLoad(): Boolean {
    val runtime = Runtime.getRuntime()
    val availableMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    val availableMB = availableMemory / (1024 * 1024)
    
    return when (whisperConfig.modelSize) {
        WhisperModelSize.TINY -> availableMB >= 150
        WhisperModelSize.BASE -> availableMB >= 230
        WhisperModelSize.SMALL -> availableMB >= 500
        WhisperModelSize.MEDIUM -> availableMB >= 1200
        else -> false
    }
}
```

### Processing Modes

```kotlin
// Configure processing mode based on use case
val config = WhisperConfig(
    processingMode = when (useCase) {
        "commands" -> WhisperProcessingMode.REAL_TIME    // Low latency
        "dictation" -> WhisperProcessingMode.BATCH       // High accuracy
        "conversation" -> WhisperProcessingMode.HYBRID   // Balanced
    },
    
    // Adjust VAD sensitivity
    vadSensitivity = 0.5f,  // 0.0 to 1.0
    
    // Noise reduction
    noiseReductionLevel = 0.7f,  // 0.0 to 1.0
    
    // Beam search for accuracy vs speed
    beamSize = 5,  // Higher = more accurate but slower
    bestOf = 5     // Number of candidates
)
```

### Battery Optimization

```kotlin
// Reduce battery usage for background operation
val batteryOptimizedConfig = WhisperConfig(
    modelSize = WhisperModelSize.TINY,  // Smallest model
    processingMode = WhisperProcessingMode.BATCH,  // Process in batches
    enableGPU = false,  // CPU only (lower power)
    threads = 2  // Limit CPU threads
)
```

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| **Model download fails** | Network issues | Check connectivity, retry download |
| **Out of memory** | Model too large | Use smaller model, close other apps |
| **Slow recognition** | Large model on weak device | Use recommended model size |
| **No audio input** | Permissions | Check RECORD_AUDIO permission |
| **Native library not found** | Architecture mismatch | Ensure ARM device (not x86) |
| **Checksum mismatch** | Corrupted download | Delete model, re-download |

### Debug Logging

```kotlin
// Enable verbose logging
class WhisperDebug {
    init {
        // Check native library
        Log.d("Whisper", "Native available: ${WhisperNative.isAvailable()}")
        
        // Check architecture
        Log.d("Whisper", "Device ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
        
        // Get processing stats
        val stats = whisperEngine.getProcessingStats()
        stats.forEach { (key, value) ->
            Log.d("Whisper", "$key: $value")
        }
    }
}
```

### Performance Metrics

```kotlin
// Monitor performance
whisperEngine.setResultListener { result ->
    // Check processing time
    if (result is TranscriptionResult) {
        Log.d("Whisper", "Processing time: ${result.processingTimeMs}ms")
        
        val audioLength = audioData.size / 16000.0  // seconds
        val rtf = result.processingTimeMs / 1000.0 / audioLength
        Log.d("Whisper", "Real-time factor: ${rtf}x")
    }
}
```

## Best Practices

1. **Model Selection**
   - Use `.en` models for English-only apps (faster, more accurate)
   - Start with BASE model, upgrade if needed
   - Consider device capabilities

2. **Memory Management**
   - Check available memory before model changes
   - Release engine when not needed
   - Use cleanup methods

3. **User Experience**
   - Show download progress
   - Provide model size information
   - Allow manual model selection for power users
   - Cache downloaded models

4. **Network**
   - Download models on WiFi when possible
   - Implement retry logic
   - Verify checksums

5. **Testing**
   - Test on both ARM64 and ARMv7 devices
   - Test with different audio qualities
   - Test offline functionality

## API Reference

See [WhisperEngine API Documentation](./API_WHISPER.md) for complete API reference.

## Sample App

See [WhisperSampleApp](../../../apps/WhisperSample) for a complete implementation example.

## Support

For issues or questions:
- Check the [FAQ](./FAQ.md)
- Report issues on [GitHub/GitLab]
- Contact support@augmentalis.com
