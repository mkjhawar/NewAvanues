package com.augmentalis.Avanues.web.universal.presentation.viewmodel

import com.augmentalis.webavanue.domain.model.Download
import com.augmentalis.webavanue.domain.model.DownloadStatus
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Filter options for downloads list
 */
enum class DownloadFilter {
    ALL,
    COMPLETED,
    IN_PROGRESS,
    FAILED
}

/**
 * DownloadViewModel - Manages download state and operations
 *
 * Features:
 * - Observe downloads from repository with real-time updates
 * - Filter downloads by status
 * - Add, cancel, retry, delete downloads
 * - Persist downloads to database
 *
 * State:
 * - downloads: List<Download> - All downloads
 * - filteredDownloads: List<Download> - Downloads filtered by current filter
 * - filter: DownloadFilter - Current filter
 * - isLoading: Boolean - Loading state
 * - error: String? - Error message
 */
class DownloadViewModel(
    private val repository: BrowserRepository
) {
    // Coroutine scope
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State: All downloads (from repository)
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())
    val downloads: StateFlow<List<Download>> = _downloads.asStateFlow()

    // State: Current filter
    private val _filter = MutableStateFlow(DownloadFilter.ALL)
    val filter: StateFlow<DownloadFilter> = _filter.asStateFlow()

    // State: Filtered downloads (derived from downloads + filter)
    val filteredDownloads: StateFlow<List<Download>> = combine(
        _downloads, _filter
    ) { downloads, currentFilter ->
        when (currentFilter) {
            DownloadFilter.ALL -> downloads
            DownloadFilter.COMPLETED -> downloads.filter { it.isComplete }
            DownloadFilter.IN_PROGRESS -> downloads.filter { it.isInProgress }
            DownloadFilter.FAILED -> downloads.filter { it.status == DownloadStatus.FAILED }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // State: Active downloads (convenience)
    val activeDownloads: StateFlow<List<Download>> = combine(
        _downloads
    ) { (downloads) ->
        downloads.filter { it.isInProgress }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // State: Loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State: Error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observeDownloads()
    }

    /**
     * Observe downloads from repository
     */
    private fun observeDownloads() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.observeDownloads()
                .catch { e ->
                    _error.value = "Failed to load downloads: ${e.message}"
                    _isLoading.value = false
                }
                .collect { downloadList ->
                    _downloads.value = downloadList
                    _isLoading.value = false
                    println("DownloadViewModel: Loaded ${downloadList.size} downloads")
                }
        }
    }

    /**
     * Start a new download
     *
     * @param url URL to download
     * @param filename Filename for the download
     * @param mimeType Optional MIME type
     * @param fileSize Optional file size (if known)
     * @param sourcePageUrl Optional source page URL
     * @param sourcePageTitle Optional source page title
     * @return The created Download object
     */
    fun startDownload(
        url: String,
        filename: String,
        mimeType: String? = null,
        fileSize: Long = 0,
        sourcePageUrl: String? = null,
        sourcePageTitle: String? = null
    ): Download {
        val download = Download.create(
            url = url,
            filename = filename,
            mimeType = mimeType,
            fileSize = fileSize,
            sourcePageUrl = sourcePageUrl,
            sourcePageTitle = sourcePageTitle
        )

        viewModelScope.launch {
            repository.addDownload(download)
                .onSuccess {
                    println("DownloadViewModel: Download added: ${download.filename}")
                }
                .onFailure { e ->
                    _error.value = "Failed to add download: ${e.message}"
                }
        }

        return download
    }

    /**
     * Add a download (simpler version for WebView callback)
     *
     * @param url URL to download
     * @param filename Filename for the download
     */
    fun addDownload(url: String, filename: String) {
        startDownload(url = url, filename = filename)
    }

    /**
     * Update download progress
     *
     * @param downloadId Download ID
     * @param downloadedSize Bytes downloaded
     * @param status Download status
     */
    fun updateProgress(downloadId: String, downloadedSize: Long, status: DownloadStatus = DownloadStatus.DOWNLOADING) {
        viewModelScope.launch {
            repository.updateDownloadProgress(downloadId, downloadedSize, status)
                .onFailure { e ->
                    println("DownloadViewModel: Failed to update progress: ${e.message}")
                }
        }
    }

    /**
     * Set the Android DownloadManager ID for a download
     *
     * @param downloadId Download ID
     * @param managerId Android DownloadManager ID
     */
    fun setDownloadManagerId(downloadId: String, managerId: Long) {
        viewModelScope.launch {
            repository.setDownloadManagerId(downloadId, managerId)
                .onFailure { e ->
                    println("DownloadViewModel: Failed to set manager ID: ${e.message}")
                }
        }
    }

    /**
     * Mark download as complete
     *
     * @param downloadId Download ID
     * @param filepath Path to downloaded file
     * @param downloadedSize Final file size
     */
    fun completeDownload(downloadId: String, filepath: String, downloadedSize: Long) {
        viewModelScope.launch {
            repository.completeDownload(downloadId, filepath, downloadedSize)
                .onSuccess {
                    println("DownloadViewModel: Download completed: $downloadId")
                }
                .onFailure { e ->
                    _error.value = "Failed to complete download: ${e.message}"
                }
        }
    }

    /**
     * Mark download as failed
     *
     * @param downloadId Download ID
     * @param errorMessage Error message
     */
    fun failDownload(downloadId: String, errorMessage: String) {
        viewModelScope.launch {
            repository.failDownload(downloadId, errorMessage)
                .onSuccess {
                    println("DownloadViewModel: Download failed: $downloadId - $errorMessage")
                }
                .onFailure { e ->
                    _error.value = "Failed to update download status: ${e.message}"
                }
        }
    }

    /**
     * Cancel a download
     *
     * @param downloadId Download ID
     */
    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            _downloads.value.find { it.id == downloadId }?.let { download ->
                val cancelledDownload = download.copy(status = DownloadStatus.CANCELLED)
                repository.updateDownload(cancelledDownload)
                    .onSuccess {
                        println("DownloadViewModel: Download cancelled: $downloadId")
                        // Platform-specific: Cancel via DownloadManager if has ID
                        download.downloadManagerId?.let { managerId ->
                            // This will be handled by platform-specific code
                            println("DownloadViewModel: DownloadManager ID to cancel: $managerId")
                        }
                    }
                    .onFailure { e ->
                        _error.value = "Failed to cancel download: ${e.message}"
                    }
            }
        }
    }

    /**
     * Retry a failed download
     *
     * @param downloadId Download ID
     */
    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            _downloads.value.find { it.id == downloadId }?.let { download ->
                if (download.canRetry) {
                    val retryDownload = download.copy(
                        status = DownloadStatus.PENDING,
                        downloadedSize = 0,
                        errorMessage = null,
                        downloadManagerId = null  // Clear old manager ID
                    )
                    repository.updateDownload(retryDownload)
                        .onSuccess {
                            println("DownloadViewModel: Download queued for retry: $downloadId")
                            // Platform-specific: Start download again via DownloadManager
                        }
                        .onFailure { e ->
                            _error.value = "Failed to retry download: ${e.message}"
                        }
                }
            }
        }
    }

    /**
     * Delete a download
     *
     * @param downloadId Download ID
     */
    fun deleteDownload(downloadId: String) {
        viewModelScope.launch {
            repository.deleteDownload(downloadId)
                .onSuccess {
                    println("DownloadViewModel: Download deleted: $downloadId")
                }
                .onFailure { e ->
                    _error.value = "Failed to delete download: ${e.message}"
                }
        }
    }

    /**
     * Clear all downloads
     */
    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
                .onSuccess {
                    println("DownloadViewModel: All downloads cleared")
                }
                .onFailure { e ->
                    _error.value = "Failed to clear downloads: ${e.message}"
                }
        }
    }

    /**
     * Clear completed downloads only
     */
    fun clearCompletedDownloads() {
        viewModelScope.launch {
            val completedIds = _downloads.value
                .filter { it.isComplete }
                .map { it.id }

            completedIds.forEach { id ->
                repository.deleteDownload(id)
            }
            println("DownloadViewModel: Cleared ${completedIds.size} completed downloads")
        }
    }

    /**
     * Set filter for downloads list
     *
     * @param filter Filter to apply
     */
    fun setFilter(filter: DownloadFilter) {
        _filter.value = filter
    }

    /**
     * Search downloads by filename
     *
     * @param query Search query
     */
    fun searchDownloads(query: String) {
        if (query.isBlank()) {
            // Reset to show all (observe will restore)
            observeDownloads()
            return
        }

        viewModelScope.launch {
            repository.searchDownloads(query)
                .onSuccess { results ->
                    _downloads.value = results
                }
                .onFailure { e ->
                    _error.value = "Search failed: ${e.message}"
                }
        }
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
        viewModelScope.cancel()
    }
}
