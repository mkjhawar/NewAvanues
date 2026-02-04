package com.augmentalis.webavanue

import com.augmentalis.webavanue.Logger
import com.augmentalis.webavanue.Download
import com.augmentalis.webavanue.DownloadStatus
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.util.BaseStatefulViewModel
import com.augmentalis.webavanue.util.ListState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * DownloadViewModel - Manages download state and operations
 *
 * Refactored to use StateFlow utilities for reduced boilerplate.
 *
 * State:
 * - downloads: List<Download> - All downloads
 * - activeDownloads: List<Download> - Downloads in progress
 * - isLoading: Boolean - Loading state
 * - error: String? - Error message
 */
class DownloadViewModel(
    private val repository: BrowserRepository,
    private val downloadQueue: DownloadQueue? = null
) : BaseStatefulViewModel() {

    // State: All downloads
    private val _downloads = ListState<Download>()
    val downloads: StateFlow<List<Download>> = _downloads.flow

    // State: Active downloads
    private val _activeDownloads = ListState<Download>()
    val activeDownloads: StateFlow<List<Download>> = _activeDownloads.flow

    // Expose UiState flows
    val isLoading: StateFlow<Boolean> = uiState.isLoading.flow
    val error: StateFlow<String?> = uiState.error.flow

    // Blocked file extensions for security
    private val blockedExtensions = setOf("apk", "exe", "sh", "bat", "cmd", "dll", "msi", "scr", "vbs", "js")

    init {
        loadDownloads()
        setupProgressMonitoring()
    }

    /**
     * Setup progress monitoring
     *
     * FIX: Now persists status updates to repository so downloads show correct status
     * after app restart or when viewing download list.
     */
    private fun setupProgressMonitoring() {
        downloadQueue?.let { queue ->
            launch {
                queue.observeAllActive().collect { progressList ->
                    val progressMap = progressList.associateBy { it.downloadId }

                    _downloads.value.forEach { download ->
                        progressMap[download.id]?.let { progress ->
                            val updated = download.copy(
                                downloadedSize = progress.bytesDownloaded,
                                fileSize = progress.bytesTotal,
                                status = progress.status,
                                filepath = progress.localPath,
                                completedAt = if (progress.isComplete) kotlinx.datetime.Clock.System.now() else download.completedAt,
                                lastProgressUpdate = System.currentTimeMillis()
                            )

                            if (download.status != progress.status) {
                                persistDownloadUpdate(updated)
                            }

                            _downloads.updateItem({ it.id == download.id }) { updated }
                        }
                    }
                    Logger.info("DownloadViewModel", "Download progress updated: ${_downloads.size} downloads")
                    updateActiveDownloads()
                }
            }
        }
    }

    private fun persistDownloadUpdate(download: Download) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDownload(download)
                .onSuccess { Logger.info("DownloadViewModel", "Download status persisted: ${download.id} -> ${download.status}") }
                .onFailure { e -> Logger.error("DownloadViewModel", "Failed to persist download status: ${e.message}", e) }
        }
    }

    fun loadDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.isLoading.value = true
            repository.getAllDownloads()
                .onSuccess { downloads ->
                    _downloads.replaceAll(downloads)
                    updateActiveDownloads()
                    Logger.info("DownloadViewModel", "Loaded ${downloads.size} downloads from repository")
                }
                .onFailure { e ->
                    Logger.error("DownloadViewModel", "Failed to load downloads: ${e.message}", e)
                    uiState.error.value = "Failed to load downloads"
                }
            uiState.isLoading.value = false
        }
    }

    /**
     * Add a new download with validation
     */
    fun addDownload(url: String, filename: String): Boolean {
        val sanitizedFilename = validateAndSanitize(url, filename) ?: return false

        Logger.info("DownloadViewModel", "Download accepted: ${Logger.sanitizeFilename(sanitizedFilename)} from ${Logger.sanitizeUrl(url)}")
        val download = Download.create(url = url, filename = sanitizedFilename)
        _downloads.addFirst(download)
        updateActiveDownloads()
        return true
    }

    /**
     * Start a download with full metadata
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
        val sanitizedFilename = validateAndSanitize(url, filename) ?: return null

        Logger.info("DownloadViewModel", "Download started: ${Logger.sanitizeFilename(sanitizedFilename)} from ${Logger.sanitizeUrl(url)}")

        val download = Download.create(
            url = url,
            filename = sanitizedFilename,
            mimeType = mimeType,
            fileSize = fileSize,
            sourcePageUrl = sourcePageUrl,
            sourcePageTitle = sourcePageTitle
        )

        _downloads.addFirst(download)
        updateActiveDownloads()

        viewModelScope.launch(Dispatchers.IO) {
            repository.addDownload(download)
            Logger.info("DownloadViewModel", "Download saved to repository: ${download.id}")

            downloadQueue?.let { queue ->
                val request = DownloadRequest(
                    downloadId = download.id,
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
                } else {
                    Logger.error("DownloadViewModel", "Failed to enqueue download to platform queue", null)
                    uiState.error.value = "Failed to start download"
                }
            } ?: run {
                Logger.warn("DownloadViewModel", "No download queue available - download will not be executed")
            }
        }

        return download.id
    }

    /**
     * Validate URL and sanitize filename (security)
     */
    private fun validateAndSanitize(url: String, filename: String): String? {
        // FIX P1-P9: Validate URL - only allow HTTP(S)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            uiState.error.value = "Only HTTP(S) downloads allowed"
            Logger.warn("DownloadViewModel", "Download rejected: Invalid URL scheme - ${Logger.sanitizeUrl(url)}")
            return null
        }

        // FIX P1-P9: Sanitize filename - prevent path traversal
        val sanitizedFilename = filename
            .replace("..", "")
            .replace("/", "")
            .replace("\\", "")
            .replace("\u0000", "")
            .trim()

        if (sanitizedFilename.isEmpty() || sanitizedFilename.length > 255) {
            uiState.error.value = "Invalid filename"
            Logger.warn("DownloadViewModel", "Download rejected: Invalid filename")
            return null
        }

        // FIX P1-P9: Block dangerous file extensions
        val extension = sanitizedFilename.substringAfterLast(".", "").lowercase()
        if (extension in blockedExtensions) {
            uiState.error.value = "Dangerous file type blocked: $extension"
            Logger.warn("DownloadViewModel", "Download rejected: Blocked extension - $extension")
            return null
        }

        return sanitizedFilename
    }

    fun updateProgress(downloadId: String, downloadedSize: Long, fileSize: Long) {
        _downloads.updateItem({ it.id == downloadId }) { download ->
            download.copy(
                downloadedSize = downloadedSize,
                fileSize = fileSize,
                status = DownloadStatus.DOWNLOADING
            )
        }
        updateActiveDownloads()
    }

    fun cancelDownload(downloadId: String) {
        _downloads.updateItem({ it.id == downloadId }) { it.copy(status = DownloadStatus.CANCELLED) }
        updateActiveDownloads()
    }

    fun retryDownload(downloadId: String) {
        _downloads.updateItem({ it.id == downloadId }) { download ->
            download.copy(status = DownloadStatus.PENDING, downloadedSize = 0, errorMessage = null)
        }
        updateActiveDownloads()
    }

    fun deleteDownload(downloadId: String) {
        _downloads.removeItem { it.id == downloadId }
        updateActiveDownloads()
    }

    fun clearCompletedDownloads() {
        _downloads.removeAll { it.status == DownloadStatus.COMPLETED }
    }

    private fun updateActiveDownloads() {
        _activeDownloads.replaceAll(_downloads.filter { it.isActive })
    }

    fun clearError() {
        uiState.clearError()
    }
}
