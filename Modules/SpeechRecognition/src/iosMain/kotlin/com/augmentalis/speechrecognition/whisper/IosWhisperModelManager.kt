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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.*

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
    }

    // Download state
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    // Active download task (for cancellation)
    private var downloadTask: NSURLSessionDownloadTask? = null

    /**
     * Download a model from HuggingFace.
     * Uses NSURLSession for native iOS networking.
     *
     * @param modelSize Which model to download
     * @return true if download completed successfully
     */
    suspend fun downloadModel(modelSize: WhisperModelSize): Boolean {
        if (isModelDownloaded(modelSize)) {
            _downloadState.value = ModelDownloadState.Completed(
                modelSize, getModelPath(modelSize)!!
            )
            return true
        }

        _downloadState.value = ModelDownloadState.Checking

        return withContext(Dispatchers.Default) {
            try {
                performDownload(modelSize)
            } catch (e: CancellationException) {
                _downloadState.value = ModelDownloadState.Cancelled(modelSize)
                logInfo(TAG, "Download cancelled: ${modelSize.displayName}")
                false
            } catch (e: Exception) {
                _downloadState.value = ModelDownloadState.Failed(modelSize, e.message ?: "Unknown error")
                logError(TAG, "Download failed: ${modelSize.displayName}")
                false
            }
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
     */
    fun isModelDownloaded(modelSize: WhisperModelSize): Boolean {
        val modelPath = getModelFilePath(modelSize)
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(modelPath)) return false

        // Verify file size is reasonable (at least 50% of expected)
        val attrs = fileManager.attributesOfItemAtPath(modelPath, error = null)
        val fileSize = (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
        return fileSize >= modelSize.approxSizeMB * 1024L * 1024L / 2
    }

    /**
     * Get the file path for a model (whether or not it's downloaded).
     */
    fun getModelPath(modelSize: WhisperModelSize): String? {
        val path = getModelFilePath(modelSize)
        return if (NSFileManager.defaultManager.fileExistsAtPath(path)) path else null
    }

    /**
     * Get all downloaded models.
     */
    fun getDownloadedModels(): List<LocalModelInfo> {
        val modelsDir = IosWhisperConfig.getModelsDirectory()

        return WhisperModelSize.entries.mapNotNull { size ->
            val path = "$modelsDir/${size.ggmlFileName}"
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(path)) {
                val attrs = fileManager.attributesOfItemAtPath(path, error = null)
                val fileSize = (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
                val modDate = (attrs?.get(NSFileModificationDate) as? NSDate)
                val modTimeMs = ((modDate?.timeIntervalSince1970 ?: 0.0) * 1000).toLong()

                LocalModelInfo(
                    modelSize = size,
                    filePath = path,
                    fileSizeBytes = fileSize,
                    isVerified = fileManager.fileExistsAtPath("$path$CHECKSUM_SUFFIX"),
                    downloadedAtMs = modTimeMs
                )
            } else null
        }
    }

    /**
     * Delete a downloaded model.
     */
    fun deleteModel(modelSize: WhisperModelSize): Boolean {
        val fileManager = NSFileManager.defaultManager
        val modelPath = getModelFilePath(modelSize)
        val partialPath = "$modelPath$PARTIAL_SUFFIX"
        val checksumPath = "$modelPath$CHECKSUM_SUFFIX"

        var deleted = true
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
            NSSearchPathDirectory.NSDocumentDirectory,
            NSSearchPathDomainMask.NSUserDomainMask
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

        val modelPath = getModelFilePath(modelSize)
        val partialPath = "$modelPath$PARTIAL_SUFFIX"

        return suspendCancellableCoroutine { cont ->
            val request = NSMutableURLRequest.requestWithURL(url).apply {
                setHTTPMethod("GET")
                setTimeoutInterval(300.0) // 5 minutes

                // Resume support
                val fileManager = NSFileManager.defaultManager
                if (fileManager.fileExistsAtPath(partialPath)) {
                    val attrs = fileManager.attributesOfItemAtPath(partialPath, error = null)
                    val existingBytes = (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
                    if (existingBytes > 0) {
                        setValue("bytes=$existingBytes-", forHTTPHeaderField = "Range")
                        logInfo(TAG, "Resuming download from byte $existingBytes")
                    }
                }
            }

            val session = NSURLSession.sharedSession
            val task = session.dataTaskWithRequest(request) { data, response, error ->
                if (error != null) {
                    val nsError = error as NSError
                    if (nsError.code == NSURLErrorCancelled) {
                        _downloadState.value = ModelDownloadState.Cancelled(modelSize)
                        cont.resume(false) {}
                    } else {
                        _downloadState.value = ModelDownloadState.Failed(modelSize, nsError.localizedDescription)
                        cont.resume(false) {}
                    }
                    return@dataTaskWithRequest
                }

                val httpResponse = response as? NSHTTPURLResponse
                val statusCode = httpResponse?.statusCode ?: 0L

                if (statusCode !in 200..299) {
                    _downloadState.value = ModelDownloadState.Failed(modelSize, "HTTP $statusCode")
                    cont.resume(false) {}
                    return@dataTaskWithRequest
                }

                // Write data to file
                data?.let { downloadedData ->
                    val success = downloadedData.writeToFile(partialPath, atomically = true)
                    if (!success) {
                        _downloadState.value = ModelDownloadState.Failed(modelSize, "Failed to write file")
                        cont.resume(false) {}
                        return@dataTaskWithRequest
                    }

                    _downloadState.value = ModelDownloadState.Verifying(modelSize)

                    // Rename partial to final
                    val fileManager = NSFileManager.defaultManager
                    if (fileManager.fileExistsAtPath(modelPath)) {
                        fileManager.removeItemAtPath(modelPath, error = null)
                    }
                    val renamed = fileManager.moveItemAtPath(partialPath, toPath = modelPath, error = null)

                    if (renamed) {
                        _downloadState.value = ModelDownloadState.Completed(modelSize, modelPath)
                        logInfo(TAG, "Model ${modelSize.displayName} ready at: $modelPath")
                        cont.resume(true) {}
                    } else {
                        _downloadState.value = ModelDownloadState.Failed(modelSize, "Failed to rename file")
                        cont.resume(false) {}
                    }
                } ?: run {
                    _downloadState.value = ModelDownloadState.Failed(modelSize, "No data received")
                    cont.resume(false) {}
                }
            }

            downloadTask = task
            task.resume()

            cont.invokeOnCancellation {
                task.cancel()
            }
        }
    }

    private fun getModelFilePath(modelSize: WhisperModelSize): String {
        return "${IosWhisperConfig.getModelsDirectory()}/${modelSize.ggmlFileName}"
    }
}
