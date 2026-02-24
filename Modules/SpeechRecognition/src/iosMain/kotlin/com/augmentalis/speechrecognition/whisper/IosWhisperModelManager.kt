/**
 * IosWhisperModelManager.kt - iOS Whisper model download and management
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * Downloads, verifies, and manages Whisper ggml models from HuggingFace.
 * Uses NSURLSession for downloads with progress tracking, resume support,
 * and SHA256 verification via CommonCrypto.
 *
 * Models stored at: {Documents}/whisper/models/
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.logDebug
import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.logWarn
import com.augmentalis.speechrecognition.whisper.vsm.IosVSMCodec
import com.augmentalis.speechrecognition.whisper.vsm.VSMFormat
import com.augmentalis.speechrecognition.whisper.vsm.vsmFileName
import kotlin.time.TimeSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.*
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

/**
 * Manages Whisper model downloads and local storage on iOS.
 *
 * Uses NSURLSession for downloads with Foundation networking.
 * Model directory: {Documents}/whisper/models/
 *
 * Usage:
 * ```kotlin
 * val manager = IosWhisperModelManager()
 * manager.downloadState.collect { state -> updateUI(state) }
 * manager.downloadModel(WhisperModelSize.BASE_EN)
 * ```
 */
@OptIn(ExperimentalForeignApi::class)
class IosWhisperModelManager {

    companion object {
        private const val TAG = "IosWhisperModelManager"
        private const val PARTIAL_SUFFIX = ".partial"
        private const val CHECKSUM_SUFFIX = ".sha256"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 2_000L
        private const val MAX_RETRY_DELAY_MS = 30_000L
    }

    // Download state
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    // Active download task (for cancellation)
    private var downloadTask: NSURLSessionTask? = null

    /**
     * Download a model from HuggingFace with automatic retry on transient failures.
     * Uses exponential backoff: 2s → 4s → 8s (capped at 30s).
     *
     * @param modelSize Which model to download
     * @param maxRetries Max retry attempts (default 3)
     * @return true if download completed successfully
     */
    suspend fun downloadModel(
        modelSize: WhisperModelSize,
        maxRetries: Int = MAX_RETRY_ATTEMPTS
    ): Boolean {
        if (isModelDownloaded(modelSize)) {
            _downloadState.value = ModelDownloadState.Completed(
                modelSize, getModelPath(modelSize)!!
            )
            return true
        }

        _downloadState.value = ModelDownloadState.Checking

        return withContext(Dispatchers.Default) {
            var lastError: String? = null

            for (attempt in 1..maxRetries) {
                try {
                    val success = performDownload(modelSize)
                    if (success) return@withContext true

                    // performDownload returned false — extract error from current state
                    lastError = (_downloadState.value as? ModelDownloadState.Failed)?.error
                        ?: "Download returned false"
                } catch (e: CancellationException) {
                    _downloadState.value = ModelDownloadState.Cancelled(modelSize)
                    logInfo(TAG, "Download cancelled: ${modelSize.displayName}")
                    return@withContext false
                } catch (e: Exception) {
                    lastError = e.message ?: "Unknown error"
                }

                // Retry with exponential backoff (skip delay on final attempt)
                if (attempt < maxRetries) {
                    val delayMs = (INITIAL_RETRY_DELAY_MS * (1L shl (attempt - 1)))
                        .coerceAtMost(MAX_RETRY_DELAY_MS)
                    logWarn(TAG, "Download attempt $attempt/$maxRetries failed: $lastError, retrying in ${delayMs}ms")
                    _downloadState.value = ModelDownloadState.Retrying(
                        modelSize, attempt, maxRetries, delayMs
                    )
                    delay(delayMs)
                }
            }

            _downloadState.value = ModelDownloadState.Failed(
                modelSize, lastError ?: "Failed after $maxRetries attempts"
            )
            logError(TAG, "Download failed after $maxRetries attempts: ${modelSize.displayName}")
            false
        }
    }

    /**
     * Cancel an in-progress download.
     */
    fun cancelDownload() {
        downloadTask?.cancel()
        downloadTask = null
    }

    /**
     * Check if a model is downloaded and valid.
     * Checks shared VSM storage first, then legacy storage.
     */
    fun isModelDownloaded(modelSize: WhisperModelSize): Boolean {
        val fileManager = NSFileManager.defaultManager

        // 1. Check shared VSM storage
        val sharedVsm = getSharedVsmPath(modelSize)
        if (fileManager.fileExistsAtPath(sharedVsm)) {
            val attrs = fileManager.attributesOfItemAtPath(sharedVsm, error = null)
            val fileSize = (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
            if (fileSize > 0) return true
        }

        // 2. Check legacy storage (.bin)
        val legacyPath = getModelFilePath(modelSize)
        if (!fileManager.fileExistsAtPath(legacyPath)) return false

        val attrs = fileManager.attributesOfItemAtPath(legacyPath, error = null)
        val fileSize = (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
        return fileSize >= modelSize.approxSizeMB * 1024L * 1024L / 2
    }

    /**
     * Get the file path for a model.
     * Returns shared .vsm path if available, legacy .bin path otherwise.
     */
    fun getModelPath(modelSize: WhisperModelSize): String? {
        val fileManager = NSFileManager.defaultManager

        // Shared .vsm takes priority
        val sharedVsm = getSharedVsmPath(modelSize)
        if (fileManager.fileExistsAtPath(sharedVsm)) return sharedVsm

        // Legacy .bin fallback
        val legacyPath = getModelFilePath(modelSize)
        return if (fileManager.fileExistsAtPath(legacyPath)) legacyPath else null
    }

    /**
     * Get all downloaded models (from both shared VSM and legacy locations).
     */
    fun getDownloadedModels(): List<LocalModelInfo> {
        val fileManager = NSFileManager.defaultManager

        return WhisperModelSize.entries.mapNotNull { size ->
            // Check shared VSM first
            val sharedVsm = getSharedVsmPath(size)
            if (fileManager.fileExistsAtPath(sharedVsm)) {
                val attrs = fileManager.attributesOfItemAtPath(sharedVsm, error = null)
                val fileSize = (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
                val modDate = (attrs?.get(NSFileModificationDate) as? NSDate)
                val modTimeMs = ((modDate?.timeIntervalSince1970 ?: 0.0) * 1000).toLong()

                return@mapNotNull LocalModelInfo(
                    modelSize = size,
                    filePath = sharedVsm,
                    fileSizeBytes = fileSize,
                    isVerified = true,
                    downloadedAtMs = modTimeMs
                )
            }

            // Legacy .bin fallback
            val legacyPath = "${ IosWhisperConfig.getModelsDirectory()}/${size.ggmlFileName}"
            if (fileManager.fileExistsAtPath(legacyPath)) {
                val attrs = fileManager.attributesOfItemAtPath(legacyPath, error = null)
                val fileSize = (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
                val modDate = (attrs?.get(NSFileModificationDate) as? NSDate)
                val modTimeMs = ((modDate?.timeIntervalSince1970 ?: 0.0) * 1000).toLong()

                LocalModelInfo(
                    modelSize = size,
                    filePath = legacyPath,
                    fileSizeBytes = fileSize,
                    isVerified = fileManager.fileExistsAtPath("$legacyPath$CHECKSUM_SUFFIX"),
                    downloadedAtMs = modTimeMs
                )
            } else null
        }
    }

    /**
     * Delete a downloaded model (from both shared VSM and legacy locations).
     */
    fun deleteModel(modelSize: WhisperModelSize): Boolean {
        val fileManager = NSFileManager.defaultManager
        var deleted = true

        // Delete shared .vsm
        val sharedVsm = getSharedVsmPath(modelSize)
        if (fileManager.fileExistsAtPath(sharedVsm)) {
            deleted = fileManager.removeItemAtPath(sharedVsm, error = null) && deleted
        }

        // Delete legacy .bin + partial + checksum
        val modelPath = getModelFilePath(modelSize)
        val partialPath = "$modelPath$PARTIAL_SUFFIX"
        val checksumPath = "$modelPath$CHECKSUM_SUFFIX"

        if (fileManager.fileExistsAtPath(modelPath)) {
            deleted = fileManager.removeItemAtPath(modelPath, error = null) && deleted
        }
        if (fileManager.fileExistsAtPath(partialPath)) {
            deleted = fileManager.removeItemAtPath(partialPath, error = null) && deleted
        }
        if (fileManager.fileExistsAtPath(checksumPath)) {
            deleted = fileManager.removeItemAtPath(checksumPath, error = null) && deleted
        }

        logInfo(TAG, "Deleted model ${modelSize.displayName}: $deleted")
        return deleted
    }

    /**
     * Get available storage on the device.
     */
    fun getAvailableStorageMB(): Long {
        val fileManager = NSFileManager.defaultManager
        val documentsDir = fileManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).firstOrNull() as? NSURL ?: return 0L

        val resourceValues = documentsDir.resourceValuesForKeys(
            listOf(NSURLVolumeAvailableCapacityForImportantUsageKey),
            error = null
        )
        val availableBytes = (resourceValues?.get(NSURLVolumeAvailableCapacityForImportantUsageKey) as? NSNumber)?.longValue ?: 0L
        return availableBytes / (1024 * 1024)
    }

    /**
     * Recommend the best model based on device memory and storage.
     */
    fun recommendModel(languageCode: String = "en"): WhisperModelSize {
        val physicalMemMB = (NSProcessInfo.processInfo.physicalMemory / (1024uL * 1024uL)).toInt()
        val availableStorageMB = getAvailableStorageMB()
        val isEnglish = languageCode.startsWith("en")

        val candidates = WhisperModelSize.entries
            .filter { it.isEnglishOnly == isEnglish }
            .filter { it.minRAMMB <= physicalMemMB / 2 }
            .filter { it.approxSizeMB <= availableStorageMB / 2 }
            .sortedByDescending { it.approxSizeMB }

        return candidates.firstOrNull() ?: if (isEnglish) WhisperModelSize.TINY_EN else WhisperModelSize.TINY
    }

    // --- Private implementation ---

    private suspend fun performDownload(modelSize: WhisperModelSize): Boolean {
        _downloadState.value = ModelDownloadState.FetchingMetadata

        val urlString = WhisperModelUrls.getDownloadUrl(modelSize)
        val url = NSURL.URLWithString(urlString) ?: throw IllegalStateException("Invalid URL: $urlString")

        val partialPath = getModelFilePath(modelSize) + PARTIAL_SUFFIX

        return suspendCancellableCoroutine { cont ->
            val request = NSMutableURLRequest.requestWithURL(url).apply {
                setHTTPMethod("GET")
                setTimeoutInterval(300.0) // 5 minutes
            }

            val session = NSURLSession.sharedSession

            // Use downloadTaskWithRequest — streams to temp file on disk instead of
            // buffering entire model (75MB-1.5GB) in memory as NSData.
            val task = session.downloadTaskWithRequest(request) { location, response, error ->
                if (error != null) {
                    val nsError = error as NSError
                    if (nsError.code == NSURLErrorCancelled) {
                        _downloadState.value = ModelDownloadState.Cancelled(modelSize)
                        cont.resume(false) {}
                    } else {
                        _downloadState.value = ModelDownloadState.Failed(modelSize, nsError.localizedDescription)
                        cont.resume(false) {}
                    }
                    return@downloadTaskWithRequest
                }

                val httpResponse = response as? NSHTTPURLResponse
                val statusCode = httpResponse?.statusCode ?: 0L

                if (statusCode !in 200..299) {
                    _downloadState.value = ModelDownloadState.Failed(modelSize, "HTTP $statusCode")
                    cont.resume(false) {}
                    return@downloadTaskWithRequest
                }

                // The download task streams to a temp file — move it before handler returns
                // (temp file is auto-deleted after this callback completes)
                val tempPath = location?.path
                if (tempPath == null) {
                    _downloadState.value = ModelDownloadState.Failed(modelSize, "No temp file from download")
                    cont.resume(false) {}
                    return@downloadTaskWithRequest
                }

                val fm = NSFileManager.defaultManager

                // Move temp file to partial path
                if (fm.fileExistsAtPath(partialPath)) {
                    fm.removeItemAtPath(partialPath, error = null)
                }
                val moveSuccess = fm.moveItemAtPath(tempPath, toPath = partialPath, error = null)
                if (!moveSuccess) {
                    _downloadState.value = ModelDownloadState.Failed(modelSize, "Failed to move downloaded file")
                    cont.resume(false) {}
                    return@downloadTaskWithRequest
                }

                _downloadState.value = ModelDownloadState.Verifying(modelSize)

                // Encrypt to VSM format in shared storage
                val vsmDir = IosWhisperConfig.getSharedVsmDirectory()
                val vsmPath = "$vsmDir/${vsmFileName(modelSize.ggmlFileName)}"
                logInfo(TAG, "Encrypting to VSM: $vsmPath")

                val codec = IosVSMCodec()
                val metadata = mapOf(
                    "model" to modelSize.displayName,
                    "ggml" to modelSize.ggmlFileName,
                    "platform" to "ios"
                )
                if (!codec.encryptFile(partialPath, vsmPath, metadata)) {
                    fm.removeItemAtPath(partialPath, error = null)
                    _downloadState.value = ModelDownloadState.Failed(
                        modelSize, "VSM encryption failed for ${modelSize.displayName}"
                    )
                    cont.resume(false) {}
                    return@downloadTaskWithRequest
                }

                // Delete the plaintext .bin partial file
                fm.removeItemAtPath(partialPath, error = null)
                logInfo(TAG, "VSM encryption complete: $vsmPath")

                _downloadState.value = ModelDownloadState.Completed(modelSize, vsmPath)
                logInfo(TAG, "Model ${modelSize.displayName} ready at: $vsmPath")
                cont.resume(true) {}
            }

            // Launch progress monitoring coroutine
            val progressJob = CoroutineScope(Dispatchers.Default).launch {
                var lastBytesReceived = 0L
                var lastMark = TimeSource.Monotonic.markNow()
                while (isActive) {
                    val received = task.countOfBytesReceived
                    val expected = task.countOfBytesExpectedToReceive
                    if (expected > 0 && received > 0) {
                        val now = TimeSource.Monotonic.markNow()
                        val elapsedMs = (now - lastMark).inWholeMilliseconds
                        val bytesPerSec = if (elapsedMs > 0) {
                            (received - lastBytesReceived) * 1000L / elapsedMs
                        } else 0L
                        lastBytesReceived = received
                        lastMark = now
                        _downloadState.value = ModelDownloadState.Downloading(
                            modelSize, received, expected, bytesPerSec
                        )
                    }
                    delay(500)
                }
            }

            downloadTask = task
            task.resume()

            cont.invokeOnCancellation {
                progressJob.cancel()
                task.cancel()
            }
        }
    }

    private fun getModelFilePath(modelSize: WhisperModelSize): String {
        return "${IosWhisperConfig.getModelsDirectory()}/${modelSize.ggmlFileName}"
    }

    /** Get the .vsm file path in shared storage for a given model size */
    private fun getSharedVsmPath(modelSize: WhisperModelSize): String {
        return "${IosWhisperConfig.getSharedVsmDirectory()}/${vsmFileName(modelSize.ggmlFileName)}"
    }

    /**
     * Migrate existing legacy .bin models to encrypted .vsm in shared storage.
     * Safe to call multiple times.
     */
    fun migrateExistingModels() {
        val fileManager = NSFileManager.defaultManager
        val legacyDir = IosWhisperConfig.getModelsDirectory()

        val contents = fileManager.contentsOfDirectoryAtPath(legacyDir, error = null)
            ?: return
        val binFiles = (contents as List<*>).filterIsInstance<String>()
            .filter { it.endsWith(".bin") }

        if (binFiles.isEmpty()) return

        val codec = IosVSMCodec()
        for (binName in binFiles) {
            val binPath = "$legacyDir/$binName"
            val vsmName = vsmFileName(binName)
            val vsmPath = "${IosWhisperConfig.getSharedVsmDirectory()}/$vsmName"

            if (fileManager.fileExistsAtPath(vsmPath)) continue

            logInfo(TAG, "Migrating $binName → $vsmName")
            if (codec.encryptFile(binPath, vsmPath)) {
                fileManager.removeItemAtPath(binPath, error = null)
                logInfo(TAG, "Migration complete: $vsmName")
            } else {
                logWarn(TAG, "Migration failed for $binName, keeping original")
            }
        }
    }
}
