package com.augmentalis.webavanue.ui.viewmodel

import com.augmentalis.webavanue.ui.util.Logger
import com.augmentalis.webavanue.domain.model.Download
import com.augmentalis.webavanue.domain.model.DownloadStatus
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DownloadViewModel - Manages download state and operations
 *
 * Note: BrowserCoreData has basic Download model support added.
 * Repository methods for downloads may need implementation.
 *
 * State:
 * - downloads: List<Download> - All downloads
 * - activeDownloads: List<Download> - Downloads in progress
 * - isLoading: Boolean - Loading state
 * - error: String? - Error message
 */
class DownloadViewModel(
    private val repository: BrowserRepository,
    private val downloadQueue: com.augmentalis.webavanue.download.DownloadQueue? = null,
    private val progressMonitor: com.augmentalis.webavanue.download.DownloadProgressMonitor? = null
) {
    // Coroutine scope
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State: All downloads
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())
    val downloads: StateFlow<List<Download>> = _downloads.asStateFlow()

    // State: Active downloads
    private val _activeDownloads = MutableStateFlow<List<Download>>(emptyList())
    val activeDownloads: StateFlow<List<Download>> = _activeDownloads.asStateFlow()

    // State: Loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State: Error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadDownloads()
        setupProgressMonitoring()
    }

    /**
     * Setup progress monitoring
     */
    private fun setupProgressMonitoring() {
        progressMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.progressFlow.collect { progressMap ->
                    val updatedDownloads = _downloads.value.map { download ->
                        progressMap[download.id]?.let { progress ->
                            download.copy(
                                downloadedSize = progress.bytesDownloaded,
                                fileSize = progress.bytesTotal,
                                downloadSpeed = progress.downloadSpeed,
                                estimatedTimeRemaining = progress.estimatedTimeRemaining,
                                lastProgressUpdate = System.currentTimeMillis()
                            )
                        } ?: download
                    }
                    _downloads.value = updatedDownloads
                    updateActiveDownloads()
                }
            }
        }
    }

    /**
     * Load all downloads
     */
    fun loadDownloads() {
        // Stub: Downloads not yet implemented in repository
        _downloads.value = emptyList()
        _activeDownloads.value = emptyList()
    }

    /**
     * Add a new download
     *
     * FIX P1-P9: Validates URL and filename to prevent path traversal and dangerous files
     *
     * @param url URL to download
     * @param filename Filename for the download
     * @return true if download was added, false if validation failed
     */
    fun addDownload(url: String, filename: String): Boolean {
        // FIX P1-P9: Validate URL - only allow HTTP(S)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _error.value = "Only HTTP(S) downloads allowed"
            Logger.warn("DownloadViewModel", "Download rejected: Invalid URL scheme - ${Logger.sanitizeUrl(url)}")
            return false
        }

        // FIX P1-P9: Sanitize filename - prevent path traversal
        val sanitizedFilename = filename
            .replace("..", "")      // Remove ..
            .replace("/", "")       // Remove /
            .replace("\\", "")      // Remove \
            .replace("\u0000", "")  // Remove null bytes
            .trim()

        if (sanitizedFilename.isEmpty() || sanitizedFilename.length > 255) {
            _error.value = "Invalid filename"
            Logger.warn("DownloadViewModel", "Download rejected: Invalid filename")
            return false
        }

        // FIX P1-P9: Block dangerous file extensions
        val extension = sanitizedFilename.substringAfterLast(".", "").lowercase()
        val blockedExtensions = setOf("apk", "exe", "sh", "bat", "cmd", "dll", "msi", "scr", "vbs", "js")
        if (extension in blockedExtensions) {
            _error.value = "Dangerous file type blocked: $extension"
            Logger.warn("DownloadViewModel", "Download rejected: Blocked extension - $extension")
            return false
        }

        Logger.info("DownloadViewModel", "Download accepted: ${Logger.sanitizeFilename(sanitizedFilename)} from ${Logger.sanitizeUrl(url)}")
        val download = Download.create(url = url, filename = sanitizedFilename)
        val currentDownloads = _downloads.value.toMutableList()
        currentDownloads.add(0, download)
        _downloads.value = currentDownloads
        updateActiveDownloads()
        return true
    }

    /**
     * Start a download with full metadata
     *
     * @param url URL to download
     * @param filename Filename for the download
     * @param mimeType MIME type of the file
     * @param fileSize Expected file size
     * @param sourcePageUrl URL of the page that initiated the download
     * @param sourcePageTitle Title of the page that initiated the download
     * @param customPath Optional custom download path (content:// URI from file picker)
     * @return Download ID if successful, null if validation failed
     */
    fun startDownload(
        url: String,
        filename: String,
        mimeType: String? = null,
        fileSize: Long = 0,
        sourcePageUrl: String? = null,
        sourcePageTitle: String? = null,
        customPath: String? = null
    ): String? {
        // FIX P1-P9: Validate URL - only allow HTTP(S)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _error.value = "Only HTTP(S) downloads allowed"
            Logger.warn("DownloadViewModel", "Download rejected: Invalid URL scheme - ${Logger.sanitizeUrl(url)}")
            return null
        }

        // FIX P1-P9: Sanitize filename - prevent path traversal
        val sanitizedFilename = filename
            .replace("..", "")      // Remove ..
            .replace("/", "")       // Remove /
            .replace("\\", "")      // Remove \
            .replace("\u0000", "")  // Remove null bytes
            .trim()

        if (sanitizedFilename.isEmpty() || sanitizedFilename.length > 255) {
            _error.value = "Invalid filename"
            Logger.warn("DownloadViewModel", "Download rejected: Invalid filename")
            return null
        }

        // FIX P1-P9: Block dangerous file extensions
        val extension = sanitizedFilename.substringAfterLast(".", "").lowercase()
        val blockedExtensions = setOf("apk", "exe", "sh", "bat", "cmd", "dll", "msi", "scr", "vbs", "js")
        if (extension in blockedExtensions) {
            _error.value = "Dangerous file type blocked: $extension"
            Logger.warn("DownloadViewModel", "Download rejected: Blocked extension - $extension")
            return null
        }

        Logger.info("DownloadViewModel", "Download started: ${Logger.sanitizeFilename(sanitizedFilename)} from ${Logger.sanitizeUrl(url)}")

        val download = Download.create(
            url = url,
            filename = sanitizedFilename,
            mimeType = mimeType,
            fileSize = fileSize,
            sourcePageUrl = sourcePageUrl,
            sourcePageTitle = sourcePageTitle
        )

        // Add to local state
        val currentDownloads = _downloads.value.toMutableList()
        currentDownloads.add(0, download)
        _downloads.value = currentDownloads
        updateActiveDownloads()

        // Save to repository and enqueue download (async)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.addDownload(download)
                Logger.info("DownloadViewModel", "Download saved to repository: ${download.id}")

                // Enqueue actual download to platform download queue
                downloadQueue?.let { queue ->
                    val request = com.augmentalis.webavanue.download.DownloadRequest(
                        url = url,
                        filename = sanitizedFilename,
                        mimeType = mimeType,
                        expectedSize = fileSize,
                        sourcePageUrl = sourcePageUrl,
                        sourcePageTitle = sourcePageTitle,
                        customPath = customPath
                    )

                    val queueId = queue.enqueue(request)
                    if (queueId != null) {
                        Logger.info("DownloadViewModel", "Download enqueued to platform queue: $queueId")

                        // Start progress monitoring (if downloadManagerId is available)
                        // Note: downloadManagerId should be stored in the download object
                        // when DownloadQueue.enqueue returns it
                        // For now, we assume queueId is the downloadManagerId
                        val downloadManagerId = queueId.toLongOrNull()
                        if (downloadManagerId != null) {
                            progressMonitor?.startMonitoring(download.id, downloadManagerId)
                            Logger.info("DownloadViewModel", "Started progress monitoring for download: ${download.id}")
                        }
                    } else {
                        Logger.error("DownloadViewModel", "Failed to enqueue download to platform queue", null)
                        _error.value = "Failed to start download"
                    }
                } ?: run {
                    Logger.warn("DownloadViewModel", "No download queue available - download will not be executed")
                }
            } catch (e: Exception) {
                Logger.error("DownloadViewModel", "Failed to save download to repository: ${e.message}", e)
            }
        }

        return download.id
    }

    /**
     * Update download progress
     *
     * @param downloadId Download ID
     * @param downloadedBytes Bytes downloaded
     * @param fileSize Total file size
     */
    fun updateProgress(downloadId: String, downloadedSize: Long, fileSize: Long) {
        val currentDownloads = _downloads.value.toMutableList()
        val index = currentDownloads.indexOfFirst { it.id == downloadId }
        if (index >= 0) {
            val download = currentDownloads[index]
            currentDownloads[index] = download.copy(
                downloadedSize = downloadedSize,
                fileSize = fileSize,
                status = DownloadStatus.DOWNLOADING
            )
            _downloads.value = currentDownloads
            updateActiveDownloads()
        }
    }

    /**
     * Cancel a download
     *
     * @param downloadId Download ID
     */
    fun cancelDownload(downloadId: String) {
        // Stop progress monitoring
        progressMonitor?.stopMonitoring(downloadId)

        val currentDownloads = _downloads.value.toMutableList()
        val index = currentDownloads.indexOfFirst { it.id == downloadId }
        if (index >= 0) {
            val download = currentDownloads[index]
            currentDownloads[index] = download.copy(status = DownloadStatus.CANCELLED)
            _downloads.value = currentDownloads
            updateActiveDownloads()
        }
    }

    /**
     * Retry a failed download
     *
     * @param downloadId Download ID
     */
    fun retryDownload(downloadId: String) {
        val currentDownloads = _downloads.value.toMutableList()
        val index = currentDownloads.indexOfFirst { it.id == downloadId }
        if (index >= 0) {
            val download = currentDownloads[index]
            currentDownloads[index] = download.copy(
                status = DownloadStatus.PENDING,
                downloadedSize = 0,
                errorMessage = null
            )
            _downloads.value = currentDownloads
            updateActiveDownloads()
        }
    }

    /**
     * Delete a download
     *
     * @param downloadId Download ID
     */
    fun deleteDownload(downloadId: String) {
        val currentDownloads = _downloads.value.toMutableList()
        currentDownloads.removeAll { it.id == downloadId }
        _downloads.value = currentDownloads
        updateActiveDownloads()
    }

    /**
     * Clear completed downloads
     */
    fun clearCompletedDownloads() {
        val currentDownloads = _downloads.value.toMutableList()
        currentDownloads.removeAll { it.status == DownloadStatus.COMPLETED }
        _downloads.value = currentDownloads
    }

    /**
     * Update active downloads list
     */
    private fun updateActiveDownloads() {
        _activeDownloads.value = _downloads.value.filter { it.isActive }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clean up resources
     */
    fun onCleared() {
        progressMonitor?.stopAll()
        viewModelScope.cancel()
    }
}
