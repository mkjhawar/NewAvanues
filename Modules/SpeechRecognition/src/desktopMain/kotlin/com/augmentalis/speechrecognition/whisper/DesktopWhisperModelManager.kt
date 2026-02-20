/**
 * DesktopWhisperModelManager.kt - Desktop Whisper model download and management
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Downloads, verifies, and manages Whisper ggml models from HuggingFace.
 * Uses java.net.HttpURLConnection (JVM stdlib) for downloads with progress
 * tracking, resume support, and SHA256 verification.
 *
 * Models stored at: ~/.avanues/whisper/models/
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.logDebug
import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.logWarn
import com.augmentalis.speechrecognition.whisper.vsm.VSMCodec
import com.augmentalis.speechrecognition.whisper.vsm.VSMFormat
import com.augmentalis.speechrecognition.whisper.vsm.vsmFileName
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Manages Whisper model downloads and local storage on Desktop (JVM).
 *
 * Uses HttpURLConnection instead of OkHttp to avoid extra dependencies.
 * Model directory: ~/.avanues/whisper/models/
 *
 * Usage:
 * ```kotlin
 * val manager = DesktopWhisperModelManager()
 * manager.downloadState.collect { state -> updateUI(state) }
 * manager.downloadModel(WhisperModelSize.BASE)
 * ```
 */
class DesktopWhisperModelManager {

    companion object {
        private const val TAG = "DesktopWhisperModelManager"
        private const val PARTIAL_SUFFIX = ".partial"
        private const val CHECKSUM_SUFFIX = ".sha256"
        private const val BUFFER_SIZE = 8192
        private const val SPEED_SAMPLE_INTERVAL_MS = 500L
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 300_000 // 5 minutes

        /** Default model directory under user home */
        val DEFAULT_MODEL_DIR: File = File(
            System.getProperty("user.home", "."),
            ".avanues/whisper/models"
        )
    }

    // Download state
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    // Active download job (for cancellation)
    private var downloadJob: Job? = null

    /**
     * Download a model from HuggingFace.
     * Supports resume if a partial download exists.
     * Emits progress via [downloadState].
     *
     * @param modelSize Which model to download
     * @return true if download completed successfully
     */
    suspend fun downloadModel(modelSize: WhisperModelSize): Boolean {
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
            logInfo(TAG, "Download cancelled: ${modelSize.displayName}")
            false
        } catch (e: Exception) {
            _downloadState.value = ModelDownloadState.Failed(modelSize, e.message ?: "Unknown error")
            logError(TAG, "Download failed: ${modelSize.displayName}", e)
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
     * Checks shared VSM storage first, then legacy storage.
     */
    fun isModelDownloaded(modelSize: WhisperModelSize): Boolean {
        // 1. Check shared VSM storage
        val sharedVsm = getSharedVsmFile(modelSize)
        if (sharedVsm.exists() && sharedVsm.length() > 0) return true

        // 2. Check legacy .bin
        val legacyBin = getModelFile(modelSize)
        if (legacyBin.exists() && legacyBin.length() >= modelSize.approxSizeMB * 1024L * 1024L / 2) {
            return true
        }

        return false
    }

    /**
     * Get the file path for a model.
     * Returns shared .vsm path if available, legacy .bin path otherwise.
     */
    fun getModelPath(modelSize: WhisperModelSize): String? {
        // Shared .vsm takes priority
        val sharedVsm = getSharedVsmFile(modelSize)
        if (sharedVsm.exists() && sharedVsm.length() > 0) return sharedVsm.absolutePath

        // Legacy .bin fallback
        val legacyBin = getModelFile(modelSize)
        if (legacyBin.exists()) return legacyBin.absolutePath

        return null
    }

    /**
     * Get all downloaded models (from both shared VSM and legacy locations).
     */
    fun getDownloadedModels(): List<LocalModelInfo> {
        return WhisperModelSize.entries.mapNotNull { size ->
            // Check shared VSM first
            val sharedVsm = getSharedVsmFile(size)
            if (sharedVsm.exists() && sharedVsm.length() > 0) {
                return@mapNotNull LocalModelInfo(
                    modelSize = size,
                    filePath = sharedVsm.absolutePath,
                    fileSizeBytes = sharedVsm.length(),
                    isVerified = true,
                    downloadedAtMs = sharedVsm.lastModified()
                )
            }

            // Legacy .bin fallback
            val dir = getModelsDirectory()
            if (!dir.exists()) return@mapNotNull null
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
     * Delete a downloaded model (from both shared VSM and legacy locations).
     */
    fun deleteModel(modelSize: WhisperModelSize): Boolean {
        var deleted = true

        // Delete shared .vsm
        val sharedVsm = getSharedVsmFile(modelSize)
        if (sharedVsm.exists()) deleted = sharedVsm.delete() && deleted

        // Delete legacy .bin + partial + checksum
        val modelFile = getModelFile(modelSize)
        val partialFile = getPartialFile(modelSize)
        val checksumFile = getChecksumFile(modelSize)
        if (modelFile.exists()) deleted = modelFile.delete() && deleted
        if (partialFile.exists()) deleted = partialFile.delete() && deleted
        if (checksumFile.exists()) deleted = checksumFile.delete() && deleted

        logInfo(TAG, "Deleted model ${modelSize.displayName}: $deleted")
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
     * Recommend the best model based on system memory and storage.
     */
    fun recommendModel(languageCode: String = "en"): WhisperModelSize {
        val maxMemMB = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
        val availableStorageMB = getAvailableStorageMB()
        val isEnglish = languageCode.startsWith("en")

        val candidates = WhisperModelSize.entries
            .filter { it.isEnglishOnly == isEnglish }
            .filter { it.minRAMMB <= maxMemMB / 2 }
            .filter { it.approxSizeMB <= availableStorageMB / 2 }
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
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = "GET"
                if (existingBytes > 0) {
                    setRequestProperty("Range", "bytes=$existingBytes-")
                    logInfo(TAG, "Resuming download from byte $existingBytes")
                }
            }

            try {
                connection.connect()
                val responseCode = connection.responseCode

                if (responseCode != HttpURLConnection.HTTP_OK &&
                    responseCode != HttpURLConnection.HTTP_PARTIAL
                ) {
                    throw IllegalStateException("HTTP $responseCode: ${connection.responseMessage}")
                }

                val isResume = responseCode == HttpURLConnection.HTTP_PARTIAL
                val contentLength = connection.contentLengthLong
                val totalBytes = if (isResume) existingBytes + contentLength else contentLength

                logInfo(TAG, "Downloading ${modelSize.displayName}: ${totalBytes / (1024 * 1024)}MB" +
                        if (isResume) " (resuming from ${existingBytes / (1024 * 1024)}MB)" else "")

                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(partialFile, isResume)
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

                    // Verify download
                    _downloadState.value = ModelDownloadState.Verifying(modelSize)

                    val sha256 = computeSHA256(partialFile)
                    logInfo(TAG, "Download complete. SHA256: $sha256")

                    val knownChecksum = WhisperModelUrls.KNOWN_CHECKSUMS[modelSize]
                    if (knownChecksum != null && sha256 != knownChecksum) {
                        partialFile.delete()
                        throw IllegalStateException(
                            "Checksum mismatch: expected $knownChecksum, got $sha256"
                        )
                    }

                    // Encrypt to VSM format in shared storage
                    val sharedDir = getSharedModelsDir()
                    val vsmFile = getSharedVsmFile(modelSize)
                    logInfo(TAG, "Encrypting to VSM: ${vsmFile.absolutePath}")

                    val codec = VSMCodec()
                    val metadata = mapOf(
                        "model" to modelSize.displayName,
                        "ggml" to modelSize.ggmlFileName,
                        "sha256" to sha256
                    )
                    if (!codec.encryptFile(partialFile.absolutePath, vsmFile.absolutePath, metadata)) {
                        partialFile.delete()
                        throw IllegalStateException("VSM encryption failed for ${modelSize.displayName}")
                    }

                    // Delete the plaintext .bin
                    partialFile.delete()
                    logInfo(TAG, "VSM encryption complete: ${vsmFile.length() / (1024 * 1024)}MB")

                    _downloadState.value = ModelDownloadState.Completed(modelSize, vsmFile.absolutePath)
                    logInfo(TAG, "Model ${modelSize.displayName} ready at: ${vsmFile.absolutePath}")
                    true

                } catch (e: CancellationException) {
                    outputStream.close()
                    inputStream.close()
                    logInfo(TAG, "Download paused at ${bytesDownloaded / (1024 * 1024)}MB (partial kept for resume)")
                    throw e
                } catch (e: Exception) {
                    outputStream.close()
                    inputStream.close()
                    throw e
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun computeSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        FileInputStream(file).use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // --- Shared VSM storage ---

    /** Shared model storage: ~/.augmentalis/models/vsm/ */
    private fun getSharedModelsDir(): File {
        val dir = DesktopWhisperConfig.SHARED_VSM_DIR
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Get the .vsm file in shared storage for a given model size */
    private fun getSharedVsmFile(modelSize: WhisperModelSize): File {
        return File(getSharedModelsDir(), vsmFileName(modelSize.ggmlFileName))
    }

    // --- Legacy storage ---

    private fun getModelsDirectory(): File {
        if (!DEFAULT_MODEL_DIR.exists()) DEFAULT_MODEL_DIR.mkdirs()
        return DEFAULT_MODEL_DIR
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

    // --- Migration ---

    /**
     * Migrate existing legacy .bin models to encrypted .vsm in shared storage.
     * Safe to call multiple times.
     */
    fun migrateExistingModels() {
        val legacyDir = getModelsDirectory()
        if (!legacyDir.exists()) return

        val binFiles = legacyDir.listFiles()?.filter { it.extension == "bin" } ?: return
        if (binFiles.isEmpty()) return

        val codec = VSMCodec()
        for (binFile in binFiles) {
            val vsmFile = File(getSharedModelsDir(), vsmFileName(binFile.name))
            if (vsmFile.exists()) continue

            logInfo(TAG, "Migrating ${binFile.name} → ${vsmFile.name}")
            if (codec.encryptFile(binFile.absolutePath, vsmFile.absolutePath)) {
                binFile.delete()
                logInfo(TAG, "Migration complete: ${vsmFile.name}")
            } else {
                logWarn(TAG, "Migration failed for ${binFile.name}, keeping original")
            }
        }
    }
}
