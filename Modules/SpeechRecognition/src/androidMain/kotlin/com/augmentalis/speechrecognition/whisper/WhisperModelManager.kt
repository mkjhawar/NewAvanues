/**
 * WhisperModelManager.kt - Android Whisper model download and management
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Downloads, verifies, and manages Whisper ggml models from HuggingFace.
 * Uses OkHttp for downloads with progress tracking, resume support,
 * and SHA256 verification.
 *
 * Models stored at: /data/data/<pkg>/files/whisper/models/
 */
package com.augmentalis.speechrecognition.whisper

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Manages Whisper model downloads and local storage on Android.
 *
 * Usage:
 * ```kotlin
 * val manager = WhisperModelManager(context)
 * // Observe download state
 * manager.downloadState.collect { state -> updateUI(state) }
 * // Download a model
 * manager.downloadModel(WhisperModelSize.BASE)
 * // Check what's available
 * val models = manager.getDownloadedModels()
 * ```
 */
class WhisperModelManager(private val context: Context) {

    companion object {
        private const val TAG = "WhisperModelManager"
        private const val MODELS_DIR = "whisper/models"
        private const val PARTIAL_SUFFIX = ".partial"
        private const val CHECKSUM_SUFFIX = ".sha256"
        private const val BUFFER_SIZE = 8192
        private const val SPEED_SAMPLE_INTERVAL_MS = 500L
    }

    // Download state
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    // Active download job (for cancellation)
    private var downloadJob: Job? = null

    // HTTP client with generous timeouts for large model downloads
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Download a model from HuggingFace.
     * Supports resume if a partial download exists.
     * Emits progress via [downloadState].
     *
     * @param modelSize Which model to download
     * @return true if download completed successfully
     */
    suspend fun downloadModel(modelSize: WhisperModelSize): Boolean {
        // Check if already downloaded
        if (isModelDownloaded(modelSize)) {
            _downloadState.value = ModelDownloadState.Completed(
                modelSize, getModelFile(modelSize).absolutePath
            )
            return true
        }

        return try {
            coroutineScope {
                downloadJob = coroutineContext[Job]
                performDownload(modelSize)
            }
        } catch (e: CancellationException) {
            _downloadState.value = ModelDownloadState.Cancelled(modelSize)
            Log.i(TAG, "Download cancelled: ${modelSize.displayName}")
            false
        } catch (e: Exception) {
            _downloadState.value = ModelDownloadState.Failed(modelSize, e.message ?: "Unknown error")
            Log.e(TAG, "Download failed: ${modelSize.displayName}", e)
            false
        }
    }

    /**
     * Cancel an in-progress download.
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    /**
     * Check if a model is downloaded and valid.
     */
    fun isModelDownloaded(modelSize: WhisperModelSize): Boolean {
        val modelFile = getModelFile(modelSize)
        if (!modelFile.exists()) return false
        // Verify file size is reasonable (at least 50% of expected)
        return modelFile.length() >= modelSize.approxSizeMB * 1024L * 1024L / 2
    }

    /**
     * Get the file path for a model (whether or not it's downloaded).
     */
    fun getModelPath(modelSize: WhisperModelSize): String? {
        val file = getModelFile(modelSize)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Get all downloaded models.
     */
    fun getDownloadedModels(): List<LocalModelInfo> {
        val dir = getModelsDirectory()
        if (!dir.exists()) return emptyList()

        return WhisperModelSize.entries.mapNotNull { size ->
            val file = File(dir, size.ggmlFileName)
            if (file.exists()) {
                LocalModelInfo(
                    modelSize = size,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    isVerified = getChecksumFile(size).exists(),
                    downloadedAtMs = file.lastModified()
                )
            } else null
        }
    }

    /**
     * Delete a downloaded model.
     */
    fun deleteModel(modelSize: WhisperModelSize): Boolean {
        val modelFile = getModelFile(modelSize)
        val partialFile = getPartialFile(modelSize)
        val checksumFile = getChecksumFile(modelSize)

        var deleted = true
        if (modelFile.exists()) deleted = modelFile.delete() && deleted
        if (partialFile.exists()) deleted = partialFile.delete() && deleted
        if (checksumFile.exists()) deleted = checksumFile.delete() && deleted

        Log.i(TAG, "Deleted model ${modelSize.displayName}: $deleted")
        return deleted
    }

    /**
     * Get total disk usage of all downloaded models.
     */
    fun getTotalDiskUsageMB(): Float {
        return getDownloadedModels().sumOf { it.fileSizeBytes.toDouble() }.toFloat() / (1024f * 1024f)
    }

    /**
     * Get available storage on the device.
     */
    fun getAvailableStorageMB(): Long {
        val dir = getModelsDirectory()
        return dir.usableSpace / (1024 * 1024)
    }

    /**
     * Recommend the best model for this device based on RAM and storage.
     */
    fun recommendModel(languageCode: String = "en"): WhisperModelSize {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamMB = (memInfo.totalMem / (1024 * 1024)).toInt()
        val availableStorageMB = getAvailableStorageMB()
        val isEnglish = languageCode.startsWith("en")

        // Pick the best model that fits in both RAM and storage
        val candidates = WhisperModelSize.entries
            .filter { it.isEnglishOnly == isEnglish }
            .filter { it.minRAMMB <= totalRamMB / 2 }
            .filter { it.approxSizeMB <= availableStorageMB / 2 } // leave 50% storage headroom
            .sortedByDescending { it.approxSizeMB }

        return candidates.firstOrNull() ?: if (isEnglish) WhisperModelSize.TINY_EN else WhisperModelSize.TINY
    }

    // --- Private implementation ---

    private suspend fun performDownload(modelSize: WhisperModelSize): Boolean {
        _downloadState.value = ModelDownloadState.Checking

        val url = WhisperModelUrls.getDownloadUrl(modelSize)
        val modelFile = getModelFile(modelSize)
        val partialFile = getPartialFile(modelSize)

        // Check for partial download (resume support)
        val existingBytes = if (partialFile.exists()) partialFile.length() else 0L

        _downloadState.value = ModelDownloadState.FetchingMetadata

        return withContext(Dispatchers.IO) {
            // Build request with Range header for resume
            val requestBuilder = Request.Builder().url(url)
            if (existingBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
                Log.i(TAG, "Resuming download from byte $existingBytes")
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful && response.code != 206) {
                response.close()
                throw IllegalStateException("HTTP ${response.code}: ${response.message}")
            }

            val isResume = response.code == 206
            val contentLength = response.body?.contentLength() ?: -1L
            val totalBytes = if (isResume) existingBytes + contentLength else contentLength

            Log.i(TAG, "Downloading ${modelSize.displayName}: ${totalBytes / (1024 * 1024)}MB" +
                    if (isResume) " (resuming from ${existingBytes / (1024 * 1024)}MB)" else "")

            val body = response.body ?: throw IllegalStateException("Empty response body")

            // Ensure directory exists
            getModelsDirectory()

            val outputStream = FileOutputStream(partialFile, isResume)
            val inputStream = body.byteStream()
            val buffer = ByteArray(BUFFER_SIZE)

            var bytesDownloaded = existingBytes
            var lastSpeedSampleTime = System.currentTimeMillis()
            var lastSpeedSampleBytes = bytesDownloaded
            var currentSpeed = 0L

            try {
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    ensureActive()

                    outputStream.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead

                    // Calculate speed periodically
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSpeedSampleTime
                    if (elapsed >= SPEED_SAMPLE_INTERVAL_MS) {
                        val bytesDelta = bytesDownloaded - lastSpeedSampleBytes
                        currentSpeed = (bytesDelta * 1000) / elapsed
                        lastSpeedSampleTime = now
                        lastSpeedSampleBytes = bytesDownloaded
                    }

                    _downloadState.value = ModelDownloadState.Downloading(
                        modelSize = modelSize,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                        speedBytesPerSec = currentSpeed
                    )
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                response.close()

                // Verify and rename
                _downloadState.value = ModelDownloadState.Verifying(modelSize)

                // Compute SHA256 of downloaded file
                val sha256 = computeSHA256(partialFile)
                Log.i(TAG, "Download complete. SHA256: $sha256")

                // Check against known checksums if available
                val knownChecksum = WhisperModelUrls.KNOWN_CHECKSUMS[modelSize]
                if (knownChecksum != null && sha256 != knownChecksum) {
                    partialFile.delete()
                    throw IllegalStateException(
                        "Checksum mismatch: expected $knownChecksum, got $sha256"
                    )
                }

                // Rename partial to final
                if (modelFile.exists()) modelFile.delete()
                if (!partialFile.renameTo(modelFile)) {
                    throw IllegalStateException("Failed to rename partial file to ${modelFile.name}")
                }

                // Save checksum for future verification
                getChecksumFile(modelSize).writeText(sha256)

                _downloadState.value = ModelDownloadState.Completed(modelSize, modelFile.absolutePath)
                Log.i(TAG, "Model ${modelSize.displayName} ready at: ${modelFile.absolutePath}")
                true

            } catch (e: CancellationException) {
                outputStream.close()
                inputStream.close()
                response.close()
                // Keep partial file for resume
                Log.i(TAG, "Download paused at ${bytesDownloaded / (1024 * 1024)}MB (partial kept for resume)")
                throw e
            } catch (e: Exception) {
                outputStream.close()
                inputStream.close()
                response.close()
                throw e
            }
        }
    }

    private fun computeSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        file.inputStream().use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getModelsDirectory(): File {
        val dir = File(context.filesDir, MODELS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getModelFile(modelSize: WhisperModelSize): File {
        return File(getModelsDirectory(), modelSize.ggmlFileName)
    }

    private fun getPartialFile(modelSize: WhisperModelSize): File {
        return File(getModelsDirectory(), modelSize.ggmlFileName + PARTIAL_SUFFIX)
    }

    private fun getChecksumFile(modelSize: WhisperModelSize): File {
        return File(getModelsDirectory(), modelSize.ggmlFileName + CHECKSUM_SUFFIX)
    }
}
