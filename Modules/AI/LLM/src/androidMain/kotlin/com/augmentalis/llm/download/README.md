# Model Download System

## Overview

The Model Download System enables on-demand ML model downloads to reduce APK size from 160MB to ~8MB.

## Components

### 1. DownloadState.kt
Sealed class hierarchy for download states:
- `Idle` - No download in progress
- `Downloading` - Active download with progress tracking
- `Paused` - Download paused (can be resumed)
- `Completed` - Download finished successfully
- `Error` - Download failed with error details

### 2. ModelDownloadConfig.kt
Model metadata and configuration:
- Model type, version, size
- Download source (HTTP, Hugging Face, Firebase, GCS)
- Priority levels (CRITICAL, HIGH, MEDIUM, LOW)
- Dependencies and checksums
- Predefined configurations in `AVAModelRegistry`

### 3. ModelCacheManager.kt
Local storage management:
- Check if models exist in cache
- Get model file paths
- Calculate cache size
- Delete models
- Verify checksums
- Cleanup old models

### 4. ModelDownloadManager.kt
Core download orchestration:
- Flow-based progress tracking
- Pause/Resume/Cancel support
- Checksum verification
- Network error handling
- Concurrent download management
- Resume interrupted downloads

## Usage Examples

### Basic Download

```kotlin
val context: Context = ...
val cacheManager = ModelCacheManager(context)
val downloadManager = ModelDownloadManager(context, cacheManager)

// Download MobileBERT model
val config = AVAModelRegistry.MOBILEBERT_INT8

downloadManager.downloadModel(config)
    .collect { state ->
        when (state) {
            is DownloadState.Idle -> {
                println("Idle")
            }
            is DownloadState.Downloading -> {
                val progress = state.getProgressPercentage()
                val speed = state.getSpeed()
                println("Downloading: $progress% ($speed)")
            }
            is DownloadState.Paused -> {
                println("Paused at ${state.progress * 100}%")
            }
            is DownloadState.Completed -> {
                println("Downloaded to: ${state.filePath}")
                println("Size: ${state.fileSize} bytes")
            }
            is DownloadState.Error -> {
                println("Error: ${state.message}")
                if (state.canRetry) {
                    println("Can retry from ${state.bytesDownloaded} bytes")
                }
            }
        }
    }
```

### Pause/Resume/Cancel

```kotlin
// Pause
downloadManager.pauseDownload("mobilebert-uncased-int8")

// Resume
downloadManager.resumeDownload("mobilebert-uncased-int8")

// Cancel
downloadManager.cancelDownload("mobilebert-uncased-int8")
```

### Cache Management

```kotlin
val cacheManager = ModelCacheManager(context)

// Check if model is cached
if (cacheManager.isModelCached("mobilebert-uncased-int8")) {
    val path = cacheManager.getModelPath("mobilebert-uncased-int8")
    println("Model at: $path")
}

// Get cache statistics
val stats = cacheManager.getCacheStats()
println("Models: ${stats.modelCount}")
println("Total size: ${stats.getFormattedTotalSize()}")

// Delete a model
cacheManager.deleteModel("old-model-id")

// Clear all cache
cacheManager.clearCache()

// Cleanup old models (30 days)
cacheManager.cleanupOldModels(30L * 24 * 60 * 60 * 1000)
```

### Ensure Model Available

```kotlin
// Download only if not cached
downloadManager.ensureModelAvailable(AVAModelRegistry.MOBILEBERT_INT8)
    .collect { state ->
        when (state) {
            is DownloadState.Completed -> {
                // Model ready to use
                loadModel(state.filePath)
            }
            is DownloadState.Error -> {
                // Handle error
            }
            else -> {
                // Handle other states
            }
        }
    }
```

### Download Multiple Models

```kotlin
val configs = listOf(
    AVAModelRegistry.MOBILEBERT_INT8,
    AVAModelRegistry.MOBILEBERT_VOCAB,
    AVAModelRegistry.GEMMA_2B_IT
)

downloadManager.downloadModels(configs)
    .collect { state ->
        // Receives states for all models sequentially
        println("${state.getModelId()}: $state")
    }
```

## Integration with Existing Model Loaders

### LocalLLMProvider Integration

```kotlin
class LocalLLMProvider(
    private val context: Context
) : LLMProvider {

    private val cacheManager = ModelCacheManager(context)
    private val downloadManager = ModelDownloadManager(context, cacheManager)
    private var alcEngine: ALCEngine? = null

    override suspend fun initialize(config: LLMConfig): Result<Unit> {
        return try {
            // Get model configuration
            val modelConfig = AVAModelRegistry.REGISTRY.getModel(config.modelId)
                ?: return Result.Error(
                    exception = IllegalArgumentException("Unknown model"),
                    message = "Model not found: ${config.modelId}"
                )

            // Ensure model is available (cache-first)
            var modelPath: String? = null
            downloadManager.ensureModelAvailable(modelConfig)
                .collect { state ->
                    when (state) {
                        is DownloadState.Completed -> {
                            modelPath = state.filePath
                        }
                        is DownloadState.Downloading -> {
                            Timber.i("Downloading model: ${state.getProgressPercentage()}%")
                        }
                        is DownloadState.Error -> {
                            throw state.error
                        }
                        else -> {}
                    }
                }

            // Initialize ALC Engine with downloaded model
            val path = modelPath ?: throw IllegalStateException("Model path not found")
            alcEngine = initializeALCEngine(path, config)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Initialization failed: ${e.message}"
            )
        }
    }

    // ... rest of implementation
}
```

### NLU ModelManager Integration

```kotlin
actual class ModelManager(private val context: Context) {

    private val cacheManager = ModelCacheManager(context)
    private val downloadManager = ModelDownloadManager(context, cacheManager)

    actual suspend fun downloadModelsIfNeeded(
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val configs = listOf(
                AVAModelRegistry.MOBILEBERT_INT8,
                AVAModelRegistry.MOBILEBERT_VOCAB
            )

            for (config in configs) {
                downloadManager.ensureModelAvailable(config)
                    .collect { state ->
                        when (state) {
                            is DownloadState.Downloading -> {
                                onProgress(state.progress)
                            }
                            is DownloadState.Completed -> {
                                onProgress(1.0f)
                            }
                            is DownloadState.Error -> {
                                throw state.error
                            }
                            else -> {}
                        }
                    }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to download models: ${e.message}"
            )
        }
    }

    actual fun getModelPath(): String {
        return cacheManager.getModelPath("mobilebert-uncased-int8")
            ?: throw IllegalStateException("Model not found")
    }

    actual fun isModelAvailable(): Boolean {
        return cacheManager.isModelCached("mobilebert-uncased-int8")
    }
}
```

## Model Configuration

### Adding New Models

```kotlin
// Define a new model configuration
val myCustomModel = ModelDownloadConfig(
    id = "my-custom-model",
    name = "My Custom Model",
    type = ModelType.LLM,
    version = "1.0.0",
    description = "Custom model for specific task",
    source = DownloadSource.HuggingFace(
        repoId = "my-org/my-model",
        filename = "model.onnx"
    ),
    size = 50_000_000L, // 50MB
    checksum = "sha256-checksum-here",
    priority = ModelPriority.HIGH,
    isRequired = false,
    dependencies = emptyList(),
    minApiLevel = 21
)

// Add to registry
val customRegistry = ModelRegistry(
    models = AVAModelRegistry.REGISTRY.models + myCustomModel
)
```

### Download Sources

#### HTTP/HTTPS
```kotlin
source = DownloadSource.Http(
    url = "https://example.com/model.bin",
    headers = mapOf(
        "Authorization" to "Bearer token",
        "User-Agent" to "AVA/1.0"
    )
)
```

#### Hugging Face
```kotlin
source = DownloadSource.HuggingFace(
    repoId = "username/repo-name",
    filename = "pytorch_model.bin",
    revision = "main" // or tag/commit
)
```

#### Google Cloud Storage
```kotlin
source = DownloadSource.GoogleCloudStorage(
    bucket = "my-bucket",
    objectPath = "models/model.bin"
)
```

#### Firebase Storage
```kotlin
source = DownloadSource.FirebaseStorage(
    bucket = "my-app.appspot.com",
    path = "models/model.bin"
)
```

## Error Handling

```kotlin
downloadManager.downloadModel(config)
    .collect { state ->
        if (state is DownloadState.Error) {
            when (state.code) {
                ErrorCode.NETWORK_ERROR -> {
                    // Check network connection
                    showRetryDialog()
                }
                ErrorCode.INSUFFICIENT_STORAGE -> {
                    // Prompt user to free space
                    showStorageWarning(config.getFormattedSize())
                }
                ErrorCode.CHECKSUM_MISMATCH -> {
                    // Delete corrupt file and retry
                    cacheManager.deleteModel(state.modelId)
                    retryDownload(config)
                }
                ErrorCode.TIMEOUT -> {
                    // Retry with exponential backoff
                    retryWithBackoff(config)
                }
                else -> {
                    showErrorDialog(state.message)
                }
            }
        }
    }
```

## Storage Management

### Check Available Space

```kotlin
val availableSpace = cacheManager.getAvailableSpace()
val requiredSpace = config.size + 100 * 1024 * 1024 // +100MB margin

if (availableSpace < requiredSpace) {
    // Prompt user to free space
    val shortfall = requiredSpace - availableSpace
    showStorageWarning("Need ${formatBytes(shortfall)} more space")
}
```

### Cleanup Strategy

```kotlin
// Delete old models (30 days)
cacheManager.cleanupOldModels(30L * 24 * 60 * 60 * 1000)

// Delete specific models
cacheManager.deleteModel("old-model-id")

// Clear entire cache
cacheManager.clearCache()
```

## Testing

### Unit Tests

```kotlin
@Test
fun testDownloadState() {
    val downloading = DownloadState.Downloading(
        modelId = "test-model",
        bytesDownloaded = 50_000_000,
        totalBytes = 100_000_000,
        progress = 0.5f
    )

    assertEquals(50, downloading.getProgressPercentage())
    assertTrue(downloading.isInProgress())
}

@Test
fun testModelConfig() {
    val config = ModelDownloadConfig(
        id = "test",
        name = "Test Model",
        type = ModelType.LLM,
        version = "1.0.0",
        source = DownloadSource.Http("https://example.com/model.bin"),
        size = 1024 * 1024 * 10, // 10MB
        priority = ModelPriority.HIGH,
        isRequired = true
    )

    assertEquals("10 MB", config.getFormattedSize())
    assertTrue(config.isCompatibleWithApiLevel(26))
}
```

### Integration Tests

```kotlin
@Test
fun testDownloadAndCache() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cacheManager = ModelCacheManager(context)
    val downloadManager = ModelDownloadManager(context, cacheManager)

    val config = ModelDownloadConfig(
        id = "test-model",
        name = "Test",
        type = ModelType.LLM,
        version = "1.0.0",
        source = DownloadSource.Http("https://example.com/model.bin"),
        size = 1024,
        priority = ModelPriority.HIGH,
        isRequired = false
    )

    var completed = false
    downloadManager.downloadModel(config)
        .collect { state ->
            if (state is DownloadState.Completed) {
                completed = true
                assertTrue(cacheManager.isModelCached("test-model"))
            }
        }

    assertTrue(completed)
}
```

## Performance Considerations

### Network
- Downloads use 8KB buffer for optimal streaming
- Progress updates throttled to 100ms intervals
- Supports HTTP Range requests for resume
- Connection timeout: 30s
- Read timeout: 30s

### Storage
- Models stored in app private storage
- Directory structure: `<app_files_dir>/models/<model_id>/`
- Temporary files use `.tmp` extension
- Atomic rename after successful download

### Memory
- Streaming downloads (no full file in memory)
- SHA-256 calculated incrementally
- Progress updates use lightweight data classes

## Future Enhancements

1. **Parallel Downloads**: Download multiple models concurrently
2. **Delta Updates**: Download only changed model parts
3. **Compression**: Support compressed models (gzip, zstd)
4. **P2P Sharing**: Share models between devices locally
5. **Smart Caching**: Keep frequently used models, delete unused
6. **Background Downloads**: Use WorkManager for background downloads
7. **Download Scheduling**: Download during WiFi/charging
8. **Model Versioning**: Automatic updates when new versions available

## Dependencies

Already included in LLM module:
- Kotlin Coroutines
- Kotlin Flow
- OkHttp (optional, for advanced HTTP features)
- Timber (logging)
- AndroidX Security (optional, for encrypted downloads)

No additional dependencies required!

## APK Size Reduction

### Before
- APK Size: ~160MB
- Includes bundled models:
  - Gemma 2B: ~1.5GB (not bundled, but referenced)
  - MobileBERT: ~25MB
  - Vocab: ~460KB

### After
- APK Size: ~8MB
- Models downloaded on-demand
- First launch: Download required models (~26MB)
- Optional models: Download as needed

### Reduction
- **95% APK size reduction**
- Faster app installation
- Lower bandwidth for initial download
- User controls which models to download
