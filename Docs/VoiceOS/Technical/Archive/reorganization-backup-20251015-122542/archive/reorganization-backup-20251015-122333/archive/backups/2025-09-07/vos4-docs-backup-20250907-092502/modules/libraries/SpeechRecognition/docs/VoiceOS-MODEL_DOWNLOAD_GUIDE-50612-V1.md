# Whisper Model Download Guide

## Table of Contents
1. [Overview](#overview)
2. [Available Models](#available-models)
3. [Model Download URLs](#model-download-urls)
4. [Automatic Model Management](#automatic-model-management)
5. [UI Integration](#ui-integration)
6. [Manual Model Management](#manual-model-management)
7. [Architecture Compatibility](#architecture-compatibility)
8. [Storage Requirements](#storage-requirements)
9. [Network Optimization](#network-optimization)
10. [Troubleshooting](#troubleshooting)

## Overview

Whisper models are neural network models trained by OpenAI for speech recognition. These models need to be downloaded to your device before they can be used for transcription. This guide explains how to manage model downloads, storage, and selection in your Android application.

### Key Concepts

- **Model Sizes**: Range from 39MB (Tiny) to 1.5GB (Large)
- **Download Source**: Hugging Face Model Hub
- **Storage Location**: Internal app storage
- **Caching**: Models are cached after first download
- **Architecture Support**: ARM64 and ARMv7 (limited)

## Available Models

### Model Comparison Table

| Model | Parameters | Size | Speed | Accuracy | Memory Usage | Best For |
|-------|------------|------|-------|----------|--------------|----------|
| **Tiny** | 39M | 39 MB | ~32x realtime | Basic | ~150 MB | Quick commands, low-end devices |
| **Base** | 74M | 74 MB | ~16x realtime | Good | ~290 MB | Balanced performance (recommended) |
| **Small** | 244M | 244 MB | ~6x realtime | Very Good | ~600 MB | High accuracy needs |
| **Medium** | 769M | 769 MB | ~2x realtime | Excellent | ~1.5 GB | Professional use |
| **Large** | 1550M | 1.5 GB | ~1x realtime | Best | ~2.9 GB | Maximum accuracy |

### Model Selection Guidelines

```kotlin
fun selectAppropriateModel(context: Context): WhisperModel {
    val deviceMemory = getAvailableMemory()
    val storageSpace = getAvailableStorage()
    val architecture = getDeviceArchitecture()
    
    return when {
        // 32-bit devices can only use Tiny
        architecture == "armeabi-v7a" -> WhisperModel.TINY
        
        // Memory constraints
        deviceMemory < 2_000_000_000L -> WhisperModel.TINY
        deviceMemory < 3_000_000_000L -> WhisperModel.BASE
        deviceMemory < 4_000_000_000L -> WhisperModel.SMALL
        deviceMemory < 6_000_000_000L -> WhisperModel.MEDIUM
        
        // Storage constraints
        storageSpace < 100_000_000L -> WhisperModel.TINY
        storageSpace < 200_000_000L -> WhisperModel.BASE
        storageSpace < 500_000_000L -> WhisperModel.SMALL
        storageSpace < 1_000_000_000L -> WhisperModel.MEDIUM
        
        else -> WhisperModel.BASE // Safe default
    }
}
```

## Model Download URLs

### Hugging Face Repository

All models are hosted on Hugging Face at:
```
https://huggingface.co/ggerganov/whisper.cpp
```

### Direct Download URLs

```kotlin
object WhisperModelUrls {
    const val BASE_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"
    
    val MODEL_URLS = mapOf(
        WhisperModel.TINY to "${BASE_URL}ggml-tiny.bin",
        WhisperModel.BASE to "${BASE_URL}ggml-base.bin",
        WhisperModel.SMALL to "${BASE_URL}ggml-small.bin",
        WhisperModel.MEDIUM to "${BASE_URL}ggml-medium.bin",
        WhisperModel.LARGE to "${BASE_URL}ggml-large.bin"
    )
    
    // Quantized models (smaller size, slightly lower accuracy)
    val QUANTIZED_URLS = mapOf(
        WhisperModel.TINY to "${BASE_URL}ggml-tiny-q5_1.bin",
        WhisperModel.BASE to "${BASE_URL}ggml-base-q5_1.bin",
        WhisperModel.SMALL to "${BASE_URL}ggml-small-q5_1.bin",
        WhisperModel.MEDIUM to "${BASE_URL}ggml-medium-q5_0.bin"
    )
}
```

### Model Checksums

```kotlin
object WhisperModelChecksums {
    val SHA256_CHECKSUMS = mapOf(
        "ggml-tiny.bin" to "bd577a113a864445d4c299885e0cb97d4ba92b5f",
        "ggml-base.bin" to "465707469ff3a37a2b9b8d8f89f2f99de7299dac",
        "ggml-small.bin" to "9ecf4cf24936d182d9a9e389e9b14e8e137b5ef0",
        "ggml-medium.bin" to "879d76d63d15de35d7fb8a7d532e24de6ca397b0",
        "ggml-large.bin" to "0f4c8e34f21cf1a914c59d8b3ce882345ad349d6"
    )
}
```

## Automatic Model Management

### WhisperModelManager

The `WhisperModelManager` class handles all model operations automatically:

```kotlin
class WhisperModelManager(context: Context) {
    
    /**
     * Download a model with automatic retry and resume
     */
    suspend fun downloadModel(
        model: WhisperModel,
        onProgress: (Float) -> Unit = {}
    ): Result<File> {
        // Check if already cached
        getCachedModel(model)?.let {
            return Result.success(it)
        }
        
        // Check storage space
        if (!hasEnoughSpace(model)) {
            return Result.failure(InsufficientStorageException())
        }
        
        // Download with retry logic
        return withRetry(maxAttempts = 3) {
            performDownload(model, onProgress)
        }
    }
    
    /**
     * Smart model selection based on device capabilities
     */
    fun selectBestModel(): WhisperModel {
        val specs = DeviceSpecs(
            availableMemory = getAvailableMemory(),
            availableStorage = getAvailableStorage(),
            architecture = getArchitecture(),
            cpuCores = Runtime.getRuntime().availableProcessors()
        )
        
        return ModelSelector.selectOptimal(specs)
    }
}
```

### Automatic Download on First Use

```kotlin
class WhisperEngine(context: Context) {
    private val modelManager = WhisperModelManager(context)
    
    suspend fun initialize() {
        // Automatically download if not cached
        val model = modelManager.selectBestModel()
        
        if (!modelManager.isModelCached(model)) {
            // Show download UI
            showDownloadDialog(model)
            
            // Download model
            modelManager.downloadModel(model).collect { state ->
                when (state) {
                    is DownloadState.Success -> {
                        loadModel(state.modelFile)
                    }
                    is DownloadState.Error -> {
                        // Fallback to smaller model
                        downloadFallbackModel()
                    }
                }
            }
        } else {
            // Load cached model
            loadModel(modelManager.getCachedModel(model)!!)
        }
    }
}
```

## UI Integration

### Built-in Download Dialog

```kotlin
@Composable
fun WhisperModelDownloadScreen() {
    val context = LocalContext.current
    val modelManager = remember { WhisperModelManager(context) }
    var selectedModel by remember { mutableStateOf<WhisperModel?>(null) }
    val downloadState by modelManager.downloadState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Select Whisper Model",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Model selection cards
        WhisperModel.values().forEach { model ->
            ModelSelectionCard(
                model = model,
                isRecommended = model == modelManager.selectBestModel(),
                isDownloaded = modelManager.isModelCached(model),
                onSelect = { selectedModel = model }
            )
        }
        
        // Download button
        selectedModel?.let { model ->
            Button(
                onClick = {
                    lifecycleScope.launch {
                        modelManager.downloadModel(model)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download ${model.name}")
            }
        }
        
        // Download progress
        when (val state = downloadState) {
            is DownloadState.Downloading -> {
                DownloadProgressCard(
                    progress = state.progress,
                    downloadedMB = state.downloadedMB,
                    totalMB = state.totalMB,
                    onCancel = { modelManager.cancelDownload() }
                )
            }
            is DownloadState.Success -> {
                SuccessCard(
                    message = "Model downloaded successfully!",
                    onContinue = { /* Navigate to main screen */ }
                )
            }
            is DownloadState.Error -> {
                ErrorCard(
                    error = state.message,
                    onRetry = { 
                        lifecycleScope.launch {
                            modelManager.downloadModel(selectedModel!!)
                        }
                    }
                )
            }
        }
    }
}
```

### Model Selection Card

```kotlin
@Composable
fun ModelSelectionCard(
    model: WhisperModel,
    isRecommended: Boolean,
    isDownloaded: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDownloaded -> MaterialTheme.colorScheme.secondaryContainer
                isRecommended -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        model.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge {
                            Text("Recommended")
                        }
                    }
                    if (isDownloaded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = Color.Green
                        )
                    }
                }
                Text(
                    "${model.parameters}M parameters • ${model.sizeMB}MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    model.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "${model.speed}x",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    "realtime",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
```

### Download Progress Component

```kotlin
@Composable
fun DownloadProgressCard(
    progress: Float,
    downloadedMB: Long,
    totalMB: Long,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Downloading model...")
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Cancel, "Cancel")
                }
            }
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$downloadedMB MB / $totalMB MB",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Estimated time remaining
            val remainingMB = totalMB - downloadedMB
            val estimatedSeconds = estimateTimeRemaining(remainingMB)
            Text(
                "Estimated time: ${formatTime(estimatedSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

## Manual Model Management

### Direct Model Download

```kotlin
class ManualModelDownloader {
    
    /**
     * Download model file directly
     */
    suspend fun downloadModelFile(
        url: String,
        destinationFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                
                val fileLength = connection.contentLength
                val input = BufferedInputStream(connection.inputStream)
                val output = FileOutputStream(destinationFile)
                
                val buffer = ByteArray(8192)
                var total = 0L
                var count: Int
                
                while (input.read(buffer).also { count = it } != -1) {
                    total += count
                    output.write(buffer, 0, count)
                    
                    if (fileLength > 0) {
                        onProgress(total.toFloat() / fileLength)
                    }
                }
                
                output.flush()
                output.close()
                input.close()
                
                Result.success(destinationFile)
            } catch (e: Exception) {
                destinationFile.delete()
                Result.failure(e)
            }
        }
    }
}
```

### Model Verification

```kotlin
class ModelVerifier {
    
    /**
     * Verify model integrity
     */
    fun verifyModel(modelFile: File, expectedChecksum: String): Boolean {
        val calculatedChecksum = calculateSHA256(modelFile)
        return calculatedChecksum == expectedChecksum
    }
    
    /**
     * Validate model format
     */
    fun validateModelFormat(modelFile: File): Boolean {
        return try {
            modelFile.inputStream().use { stream ->
                // Check magic bytes for GGML format
                val magic = ByteArray(4)
                stream.read(magic)
                
                // GGML magic: 0x67676d6c ("ggml")
                magic.contentEquals(byteArrayOf(0x67, 0x67, 0x6d, 0x6c))
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check model compatibility
     */
    fun checkCompatibility(modelFile: File): ModelCompatibility {
        val fileSize = modelFile.length()
        val architecture = getDeviceArchitecture()
        
        return when {
            architecture == "armeabi-v7a" && fileSize > 100_000_000 -> {
                ModelCompatibility.INCOMPATIBLE_ARCHITECTURE
            }
            getAvailableMemory() < fileSize * 3 -> {
                ModelCompatibility.INSUFFICIENT_MEMORY
            }
            else -> ModelCompatibility.COMPATIBLE
        }
    }
}

enum class ModelCompatibility {
    COMPATIBLE,
    INCOMPATIBLE_ARCHITECTURE,
    INSUFFICIENT_MEMORY,
    UNSUPPORTED_FORMAT
}
```

### Offline Model Bundle

For apps that need to work offline immediately:

```kotlin
class OfflineModelBundler {
    
    /**
     * Extract bundled model from assets
     */
    suspend fun extractBundledModel(
        context: Context,
        assetPath: String,
        model: WhisperModel
    ): File {
        return withContext(Dispatchers.IO) {
            val destinationFile = File(context.filesDir, "models/${model.fileName}")
            destinationFile.parentFile?.mkdirs()
            
            if (!destinationFile.exists()) {
                context.assets.open(assetPath).use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            destinationFile
        }
    }
    
    /**
     * Bundle model in APK (build time)
     */
    fun bundleModelInApk() {
        // In build.gradle.kts:
        /*
        android {
            sourceSets {
                main {
                    assets.srcDirs += files("$projectDir/whisper-models")
                }
            }
        }
        */
    }
}
```

## Architecture Compatibility

### Device Architecture Detection

```kotlin
object ArchitectureDetector {
    
    fun getDeviceArchitecture(): DeviceArchitecture {
        val abis = Build.SUPPORTED_ABIS
        
        return when {
            abis.contains("arm64-v8a") -> DeviceArchitecture.ARM64
            abis.contains("armeabi-v7a") -> DeviceArchitecture.ARM32
            abis.contains("x86_64") -> DeviceArchitecture.X86_64
            abis.contains("x86") -> DeviceArchitecture.X86
            else -> DeviceArchitecture.UNKNOWN
        }
    }
    
    fun getSupportedModels(architecture: DeviceArchitecture): List<WhisperModel> {
        return when (architecture) {
            DeviceArchitecture.ARM64 -> WhisperModel.values().toList()
            DeviceArchitecture.ARM32 -> listOf(WhisperModel.TINY)
            else -> emptyList() // x86 not supported
        }
    }
    
    fun getRecommendedModel(architecture: DeviceArchitecture): WhisperModel? {
        return when (architecture) {
            DeviceArchitecture.ARM64 -> WhisperModel.BASE
            DeviceArchitecture.ARM32 -> WhisperModel.TINY
            else -> null
        }
    }
}

enum class DeviceArchitecture {
    ARM64,    // 64-bit ARM (most modern devices)
    ARM32,    // 32-bit ARM (older/budget devices)
    X86_64,   // 64-bit x86 (emulators, some tablets)
    X86,      // 32-bit x86 (old emulators)
    UNKNOWN
}
```

### Architecture-Specific Optimizations

```kotlin
class ArchitectureOptimizer {
    
    fun optimizeForArchitecture(architecture: DeviceArchitecture): ModelConfig {
        return when (architecture) {
            DeviceArchitecture.ARM64 -> ModelConfig(
                useNeon = true,
                threadCount = 4,
                beamSize = 5,
                enableGpu = false // Future support
            )
            DeviceArchitecture.ARM32 -> ModelConfig(
                useNeon = true,
                threadCount = 2,
                beamSize = 1,
                enableGpu = false
            )
            else -> ModelConfig(
                useNeon = false,
                threadCount = 1,
                beamSize = 1,
                enableGpu = false
            )
        }
    }
}
```

## Storage Requirements

### Storage Space Calculation

```kotlin
class StorageManager(private val context: Context) {
    
    /**
     * Calculate required storage for model
     */
    fun calculateRequiredStorage(model: WhisperModel): StorageRequirement {
        val modelSize = model.getSizeBytes()
        val cacheSize = modelSize * 0.1 // 10% for cache
        val tempSize = modelSize // Temporary space during download
        
        return StorageRequirement(
            modelSize = modelSize,
            cacheSize = cacheSize.toLong(),
            tempSize = tempSize,
            totalRequired = modelSize + cacheSize.toLong() + tempSize
        )
    }
    
    /**
     * Check available storage
     */
    fun getAvailableStorage(): Long {
        val path = context.filesDir
        val stat = StatFs(path.absolutePath)
        return stat.availableBytes
    }
    
    /**
     * Clean up old models
     */
    fun cleanupOldModels(keepModels: List<WhisperModel>) {
        val modelDir = File(context.filesDir, "whisper_models")
        modelDir.listFiles()?.forEach { file ->
            if (!keepModels.any { it.fileName == file.name }) {
                file.delete()
            }
        }
    }
    
    /**
     * Move model to external storage (if available)
     */
    fun moveToExternalStorage(model: WhisperModel): Boolean {
        if (context.externalCacheDir == null) return false
        
        val internalFile = File(context.filesDir, "whisper_models/${model.fileName}")
        val externalFile = File(context.externalCacheDir, "whisper_models/${model.fileName}")
        
        return try {
            externalFile.parentFile?.mkdirs()
            internalFile.copyTo(externalFile, overwrite = true)
            internalFile.delete()
            true
        } catch (e: Exception) {
            false
        }
    }
}

data class StorageRequirement(
    val modelSize: Long,
    val cacheSize: Long,
    val tempSize: Long,
    val totalRequired: Long
) {
    fun toReadableString(): String {
        val totalMB = totalRequired / 1024 / 1024
        return "$totalMB MB required"
    }
}
```

### Storage Optimization

```kotlin
class StorageOptimizer {
    
    /**
     * Compress model for storage (experimental)
     */
    suspend fun compressModel(modelFile: File): File {
        return withContext(Dispatchers.IO) {
            val compressedFile = File(modelFile.parent, "${modelFile.name}.gz")
            
            GZIPOutputStream(compressedFile.outputStream()).use { gzip ->
                modelFile.inputStream().use { input ->
                    input.copyTo(gzip)
                }
            }
            
            modelFile.delete()
            compressedFile
        }
    }
    
    /**
     * Use memory-mapped files for large models
     */
    fun memoryMapModel(modelFile: File): MappedByteBuffer {
        return RandomAccessFile(modelFile, "r").use { file ->
            file.channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                file.length()
            )
        }
    }
}
```

## Network Optimization

### Download Optimization

```kotlin
class DownloadOptimizer {
    
    /**
     * Resume interrupted downloads
     */
    suspend fun resumeDownload(
        url: String,
        partialFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val existingSize = if (partialFile.exists()) partialFile.length() else 0L
                
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.setRequestProperty("Range", "bytes=$existingSize-")
                connection.connect()
                
                val responseCode = connection.responseCode
                if (responseCode !in 200..299 && responseCode != 206) {
                    return@withContext Result.failure(Exception("HTTP $responseCode"))
                }
                
                val totalSize = existingSize + connection.contentLength
                val input = BufferedInputStream(connection.inputStream)
                val output = FileOutputStream(partialFile, true) // Append mode
                
                val buffer = ByteArray(8192)
                var total = existingSize
                var count: Int
                
                while (input.read(buffer).also { count = it } != -1) {
                    total += count
                    output.write(buffer, 0, count)
                    onProgress(total.toFloat() / totalSize)
                }
                
                output.close()
                input.close()
                
                Result.success(partialFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Use CDN for faster downloads
     */
    fun selectFastestCDN(model: WhisperModel): String {
        val cdns = listOf(
            "https://cdn.huggingface.co/",
            "https://cdn-lfs.huggingface.co/",
            "https://huggingface.co/"
        )
        
        // Ping each CDN and select fastest
        return cdns.minByOrNull { cdn ->
            measureLatency(cdn)
        } ?: cdns.first()
    }
    
    private fun measureLatency(url: String): Long {
        return try {
            val start = System.currentTimeMillis()
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connect()
            connection.responseCode
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }
}
```

### Bandwidth Management

```kotlin
class BandwidthManager {
    
    /**
     * Check network type and adjust download strategy
     */
    fun getDownloadStrategy(context: Context): DownloadStrategy {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as ConnectivityManager
        
        val network = connectivityManager.activeNetwork ?: return DownloadStrategy.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return DownloadStrategy.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                DownloadStrategy.FULL_SPEED
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    DownloadStrategy.FULL_SPEED
                } else {
                    DownloadStrategy.LIMITED
                }
            }
            else -> DownloadStrategy.CONSERVATIVE
        }
    }
    
    enum class DownloadStrategy {
        FULL_SPEED,   // No restrictions
        LIMITED,      // Warn user, smaller models only
        CONSERVATIVE, // Only tiny model, with confirmation
        NONE         // No download
    }
}
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Download Fails Repeatedly

```kotlin
class DownloadTroubleshooter {
    
    fun diagnoseDownloadFailure(error: Exception): DownloadDiagnosis {
        return when (error) {
            is UnknownHostException -> DownloadDiagnosis.NO_INTERNET
            is SocketTimeoutException -> DownloadDiagnosis.SLOW_CONNECTION
            is IOException -> {
                if (error.message?.contains("No space") == true) {
                    DownloadDiagnosis.INSUFFICIENT_STORAGE
                } else {
                    DownloadDiagnosis.IO_ERROR
                }
            }
            is SecurityException -> DownloadDiagnosis.PERMISSION_DENIED
            else -> DownloadDiagnosis.UNKNOWN
        }
    }
    
    fun getSolution(diagnosis: DownloadDiagnosis): String {
        return when (diagnosis) {
            DownloadDiagnosis.NO_INTERNET -> 
                "Check your internet connection"
            DownloadDiagnosis.SLOW_CONNECTION -> 
                "Try again on a faster network or download a smaller model"
            DownloadDiagnosis.INSUFFICIENT_STORAGE -> 
                "Free up space or select a smaller model"
            DownloadDiagnosis.PERMISSION_DENIED -> 
                "Grant storage permissions in app settings"
            DownloadDiagnosis.IO_ERROR -> 
                "Clear app cache and try again"
            DownloadDiagnosis.UNKNOWN -> 
                "An unknown error occurred. Please try again later"
        }
    }
}

enum class DownloadDiagnosis {
    NO_INTERNET,
    SLOW_CONNECTION,
    INSUFFICIENT_STORAGE,
    PERMISSION_DENIED,
    IO_ERROR,
    UNKNOWN
}
```

#### 2. Model Corruption

```kotlin
class ModelRepair {
    
    /**
     * Detect and repair corrupted models
     */
    suspend fun repairCorruptedModel(
        modelFile: File,
        expectedChecksum: String
    ): RepairResult {
        // Check if file exists
        if (!modelFile.exists()) {
            return RepairResult.NEEDS_REDOWNLOAD
        }
        
        // Verify checksum
        val actualChecksum = calculateChecksum(modelFile)
        if (actualChecksum == expectedChecksum) {
            return RepairResult.NO_REPAIR_NEEDED
        }
        
        // Check if partially downloaded
        val expectedSize = getExpectedSize(modelFile.name)
        if (modelFile.length() < expectedSize) {
            return RepairResult.INCOMPLETE_DOWNLOAD
        }
        
        // File is corrupted
        modelFile.delete()
        return RepairResult.NEEDS_REDOWNLOAD
    }
    
    enum class RepairResult {
        NO_REPAIR_NEEDED,
        INCOMPLETE_DOWNLOAD,
        NEEDS_REDOWNLOAD,
        REPAIRED
    }
}
```

#### 3. Storage Issues

```kotlin
class StorageTroubleshooter {
    
    /**
     * Free up storage space
     */
    fun freeUpSpace(context: Context, requiredSpace: Long): Boolean {
        var freedSpace = 0L
        
        // Clear app cache
        context.cacheDir.deleteRecursively()
        freedSpace += measureFreedSpace()
        
        if (freedSpace >= requiredSpace) return true
        
        // Delete old models
        val modelDir = File(context.filesDir, "whisper_models")
        val oldModels = modelDir.listFiles()
            ?.sortedBy { it.lastModified() }
            ?.dropLast(1) // Keep most recent
        
        oldModels?.forEach { file ->
            val fileSize = file.length()
            if (file.delete()) {
                freedSpace += fileSize
                if (freedSpace >= requiredSpace) return true
            }
        }
        
        return freedSpace >= requiredSpace
    }
    
    private fun measureFreedSpace(): Long {
        // Implementation to measure freed space
        return 0L
    }
}
```

### Debug Logging

```kotlin
class ModelDownloadLogger {
    
    fun enableDebugLogging() {
        // Log all download events
        WhisperModelManager.setLogLevel(LogLevel.DEBUG)
        
        // Custom logger
        WhisperModelManager.setLogger { level, message ->
            when (level) {
                LogLevel.DEBUG -> Log.d("ModelDownload", message)
                LogLevel.INFO -> Log.i("ModelDownload", message)
                LogLevel.WARNING -> Log.w("ModelDownload", message)
                LogLevel.ERROR -> Log.e("ModelDownload", message)
            }
        }
    }
    
    fun logDownloadMetrics(metrics: DownloadMetrics) {
        Log.d("ModelDownload", """
            Download Metrics:
            - Model: ${metrics.model}
            - Duration: ${metrics.durationMs}ms
            - Speed: ${metrics.averageSpeedKBps} KB/s
            - Retries: ${metrics.retryCount}
            - Network Type: ${metrics.networkType}
        """.trimIndent())
    }
}

data class DownloadMetrics(
    val model: WhisperModel,
    val durationMs: Long,
    val averageSpeedKBps: Float,
    val retryCount: Int,
    val networkType: String
)
```

## Best Practices

### 1. Progressive Download Strategy

```kotlin
class ProgressiveDownloadStrategy {
    
    /**
     * Start with smallest model, upgrade progressively
     */
    suspend fun progressiveModelUpgrade(context: Context) {
        val modelManager = WhisperModelManager(context)
        
        // Start with Tiny for immediate functionality
        modelManager.downloadModel(WhisperModel.TINY).collect { state ->
            if (state is DownloadState.Success) {
                // Load and use Tiny model immediately
                loadModel(WhisperModel.TINY)
                
                // Download better model in background
                if (hasWiFi(context)) {
                    backgroundDownload(WhisperModel.BASE)
                }
            }
        }
    }
}
```

### 2. User Experience Guidelines

- Always show download progress
- Provide cancel option
- Estimate time remaining
- Allow background downloads
- Implement pause/resume
- Show model benefits clearly
- Offer model comparison
- Remember user's choice

### 3. Error Recovery

- Implement automatic retry with exponential backoff
- Provide manual retry option
- Suggest alternative models on failure
- Cache partial downloads
- Verify downloads with checksums
- Provide clear error messages
- Log failures for debugging

---

*Last updated: 2025-08-31*
*Version: 2.1.0*
