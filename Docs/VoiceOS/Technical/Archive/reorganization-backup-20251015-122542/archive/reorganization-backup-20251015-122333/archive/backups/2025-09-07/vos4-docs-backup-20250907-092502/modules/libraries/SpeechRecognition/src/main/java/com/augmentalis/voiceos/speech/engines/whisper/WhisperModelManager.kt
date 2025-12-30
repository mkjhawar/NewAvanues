/**
 * WhisperModelManager.kt - Manages Whisper model downloads and caching
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-08-31
 * 
 * Handles:
 * - Model downloading from Hugging Face
 * - Model caching and version management
 * - Download progress tracking
 * - Model validation
 */
package com.augmentalis.voiceos.speech.engines.whisper

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import com.augmentalis.voiceos.speech.engines.whisper.WhisperModelSize

/**
 * Download state for model management
 */
sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    data class Downloading(val progress: Float, val downloadedMB: Float, val totalMB: Float) : ModelDownloadState()
    object Verifying : ModelDownloadState()
    data class Completed(val modelPath: String) : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

/**
 * Whisper Model Manager
 * Handles downloading, caching, and management of Whisper models
 */
class WhisperModelManager(private val context: Context) {
    
    companion object {
        private const val TAG = "WhisperModelManager"
        
        // Model download URLs (Hugging Face mirror)
        private const val BASE_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"
        
        // Model file names and checksums
        private val MODEL_FILES = mapOf(
            WhisperModelSize.TINY to ModelInfo(
                "ggml-tiny.bin",
                "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21",
                39f
            ),
            WhisperModelSize.BASE to ModelInfo(
                "ggml-base.bin", 
                "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe",
                74f
            ),
            WhisperModelSize.SMALL to ModelInfo(
                "ggml-small.bin",
                "1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b",
                244f
            ),
            WhisperModelSize.MEDIUM to ModelInfo(
                "ggml-medium.bin",
                "6c14d5adee5f86394037b4e4e8b59f1673442340d7134a57426da8c1175d51e9",
                769f
            ),
            WhisperModelSize.LARGE to ModelInfo(
                "ggml-large-v3.bin",
                "64d182b440b98d5203c4f9bd541544d84c605196c4f7b845dfa11fb23594d1e2",
                1550f
            )
        )
        
        private const val MODELS_DIR = "whisper_models"
        private const val CHUNK_SIZE = 8192 // 8KB chunks for download
        private const val DOWNLOAD_TIMEOUT = 300L // 5 minutes timeout
    }
    
    // Download state flow
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState
    
    // OkHttp client for downloads
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Current download job
    private var downloadJob: Job? = null
    
    // Device architecture detection
    private val supportsArm64: Boolean = Build.SUPPORTED_ABIS.contains("arm64-v8a")
    private val supportsArmV7: Boolean = Build.SUPPORTED_ABIS.contains("armeabi-v7a")
    private val deviceArchitecture: String = when {
        supportsArm64 -> "ARM64"
        supportsArmV7 -> "ARMv7 (32-bit)"
        else -> "Unknown"
    }
    
    /**
     * Check if a model is already downloaded
     */
    fun isModelDownloaded(modelSize: WhisperModelSize): Boolean {
        val modelInfo = MODEL_FILES[modelSize] ?: return false
        val modelFile = getModelFile(modelInfo.fileName)
        
        return modelFile.exists() && validateModel(modelFile, modelInfo.checksum)
    }
    
    /**
     * Get the path to a downloaded model
     */
    fun getModelPath(modelSize: WhisperModelSize): String? {
        val modelInfo = MODEL_FILES[modelSize] ?: return null
        val modelFile = getModelFile(modelInfo.fileName)
        
        return if (modelFile.exists() && validateModel(modelFile, modelInfo.checksum)) {
            modelFile.absolutePath
        } else {
            null
        }
    }
    
    /**
     * Download a Whisper model
     */
    suspend fun downloadModel(modelSize: WhisperModelSize) = withContext(Dispatchers.IO) {
        // Cancel any existing download
        downloadJob?.cancel()
        
        val modelInfo = MODEL_FILES[modelSize] ?: run {
            _downloadState.value = ModelDownloadState.Error("Unknown model size: $modelSize")
            return@withContext
        }
        
        // Check if already downloaded
        if (isModelDownloaded(modelSize)) {
            Log.d(TAG, "Model ${modelSize.modelName} already downloaded")
            _downloadState.value = ModelDownloadState.Completed(getModelPath(modelSize)!!)
            return@withContext
        }
        
        downloadJob = launch {
            try {
                downloadModelFile(modelInfo)
            } catch (e: CancellationException) {
                Log.d(TAG, "Download cancelled")
                _downloadState.value = ModelDownloadState.Idle
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _downloadState.value = ModelDownloadState.Error("Download failed: ${e.message}")
            }
        }
        
        downloadJob?.join()
    }
    
    /**
     * Cancel current download
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        _downloadState.value = ModelDownloadState.Idle
    }
    
    /**
     * Download model file with progress tracking
     */
    private suspend fun downloadModelFile(modelInfo: ModelInfo) = withContext(Dispatchers.IO) {
        val modelFile = getModelFile(modelInfo.fileName)
        val tempFile = File(modelFile.parent, "${modelInfo.fileName}.tmp")
        
        try {
            // Create directories if needed
            modelFile.parentFile?.mkdirs()
            
            // Build download URL
            val url = BASE_URL + modelInfo.fileName
            Log.d(TAG, "Downloading model from: $url")
            
            // Create request
            val request = Request.Builder()
                .url(url)
                .build()
            
            // Execute request
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("Download failed: ${response.code} ${response.message}")
            }
            
            val body = response.body ?: throw IOException("Empty response body")
            val contentLength = body.contentLength()
            val totalMB = contentLength / (1024f * 1024f)
            
            Log.d(TAG, "Downloading ${modelInfo.fileName}: ${totalMB}MB")
            
            // Download with progress
            downloadWithProgress(body, tempFile, totalMB)
            
            // Verify checksum
            _downloadState.value = ModelDownloadState.Verifying
            
            if (!validateModel(tempFile, modelInfo.checksum)) {
                tempFile.delete()
                throw IOException("Model validation failed - checksum mismatch")
            }
            
            // Move temp file to final location
            if (modelFile.exists()) {
                modelFile.delete()
            }
            tempFile.renameTo(modelFile)
            
            Log.i(TAG, "✅ Model downloaded successfully: ${modelFile.absolutePath}")
            _downloadState.value = ModelDownloadState.Completed(modelFile.absolutePath)
            
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }
    
    /**
     * Download with progress tracking
     */
    private suspend fun downloadWithProgress(
        body: ResponseBody,
        outputFile: File,
        totalMB: Float
    ) = withContext(Dispatchers.IO) {
        
        body.byteStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(CHUNK_SIZE)
                var downloaded = 0L
                var read: Int
                
                while (input.read(buffer).also { read = it } != -1) {
                    // Check for cancellation
                    if (!isActive) {
                        throw CancellationException("Download cancelled")
                    }
                    
                    output.write(buffer, 0, read)
                    downloaded += read
                    
                    // Update progress
                    val downloadedMB = downloaded / (1024f * 1024f)
                    val progress = (downloaded.toFloat() / body.contentLength()) * 100f
                    
                    _downloadState.value = ModelDownloadState.Downloading(
                        progress = progress,
                        downloadedMB = downloadedMB,
                        totalMB = totalMB
                    )
                }
            }
        }
    }
    
    /**
     * Validate model file using SHA256 checksum
     */
    private fun validateModel(file: File, expectedChecksum: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(CHUNK_SIZE)
                var read: Int
                
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            
            val checksum = digest.digest().joinToString("") { "%02x".format(it) }
            val isValid = checksum == expectedChecksum
            
            if (!isValid) {
                Log.w(TAG, "Checksum mismatch: expected=$expectedChecksum, actual=$checksum")
            }
            
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate model", e)
            false
        }
    }
    
    /**
     * Get model file location
     */
    private fun getModelFile(fileName: String): File {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        return File(modelsDir, fileName)
    }
    
    /**
     * Delete a downloaded model
     */
    fun deleteModel(modelSize: WhisperModelSize): Boolean {
        val modelInfo = MODEL_FILES[modelSize] ?: return false
        val modelFile = getModelFile(modelInfo.fileName)
        
        return if (modelFile.exists()) {
            val deleted = modelFile.delete()
            Log.d(TAG, "Model ${modelSize.modelName} deleted: $deleted")
            deleted
        } else {
            false
        }
    }
    
    /**
     * Get available storage space
     */
    fun getAvailableStorageMB(): Long {
        val statFs = android.os.StatFs(context.filesDir.absolutePath)
        val availableBytes = statFs.availableBytes
        return availableBytes / (1024 * 1024)
    }
    
    /**
     * Check if there's enough space for a model
     */
    fun hasEnoughSpace(modelSize: WhisperModelSize): Boolean {
        val modelInfo = MODEL_FILES[modelSize] ?: return false
        val requiredMB = modelInfo.sizeMB * 1.2f // Add 20% buffer
        val availableMB = getAvailableStorageMB()
        
        return availableMB > requiredMB
    }
    
    /**
     * Get all downloaded models
     */
    fun getDownloadedModels(): List<WhisperModelSize> {
        return WhisperModelSize.values().filter { isModelDownloaded(it) }
    }
    
    /**
     * Get recommended model based on device capabilities
     */
    fun getRecommendedModel(): WhisperModelSize {
        // Check device architecture first
        if (!supportsArm64 && supportsArmV7) {
            Log.d(TAG, "Device is ARMv7 (32-bit), recommending TINY model for compatibility")
            return WhisperModelSize.TINY
        }
        
        // Get available memory
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val availableMemoryMB = maxMemory / (1024 * 1024)
        
        // Also check storage space
        val availableStorageMB = getAvailableStorageMB()
        
        Log.d(TAG, "Device architecture: $deviceArchitecture")
        Log.d(TAG, "Available memory: ${availableMemoryMB}MB")
        Log.d(TAG, "Available storage: ${availableStorageMB}MB")
        
        // Recommend based on both memory and storage
        return when {
            availableMemoryMB < 200 || availableStorageMB < 100 -> {
                Log.d(TAG, "Low resources, recommending TINY model")
                WhisperModelSize.TINY
            }
            availableMemoryMB < 400 || availableStorageMB < 200 -> {
                Log.d(TAG, "Medium resources, recommending BASE model")
                WhisperModelSize.BASE
            }
            availableMemoryMB < 800 || availableStorageMB < 500 -> {
                Log.d(TAG, "Good resources, recommending SMALL model")
                WhisperModelSize.SMALL
            }
            else -> {
                // For mobile, we still recommend SMALL as maximum
                // MEDIUM and LARGE are too resource-intensive
                Log.d(TAG, "High resources available, recommending SMALL model (optimal for mobile)")
                WhisperModelSize.SMALL
            }
        }
    }
    
    /**
     * Check if device supports the specified model
     */
    fun isModelSupported(modelSize: WhisperModelSize): Boolean {
        // ARMv7 devices should only use TINY model
        if (!supportsArm64 && supportsArmV7) {
            return modelSize == WhisperModelSize.TINY
        }
        
        // Check if there's enough space
        return hasEnoughSpace(modelSize)
    }
    
    /**
     * Clean up old or corrupted models
     */
    fun cleanupModels() {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists()) return
        
        modelsDir.listFiles()?.forEach { file ->
            // Check if it's a known model file
            val isKnownModel = MODEL_FILES.values.any { it.fileName == file.name }
            
            if (!isKnownModel || file.name.endsWith(".tmp")) {
                Log.d(TAG, "Deleting unknown/temp file: ${file.name}")
                file.delete()
            }
        }
    }
}

/**
 * Model information
 */
private data class ModelInfo(
    val fileName: String,
    val checksum: String,
    val sizeMB: Float
)
