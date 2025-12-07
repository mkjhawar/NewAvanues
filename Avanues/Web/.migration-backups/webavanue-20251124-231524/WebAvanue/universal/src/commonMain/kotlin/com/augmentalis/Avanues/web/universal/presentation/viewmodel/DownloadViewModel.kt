package com.augmentalis.Avanues.web.universal.presentation.viewmodel

import com.augmentalis.webavanue.domain.model.Download
import com.augmentalis.webavanue.domain.model.DownloadStatus
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val repository: BrowserRepository
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
     * @param url URL to download
     * @param filename Filename for the download
     */
    fun addDownload(url: String, filename: String) {
        val download = Download.create(url = url, filename = filename)
        val currentDownloads = _downloads.value.toMutableList()
        currentDownloads.add(0, download)
        _downloads.value = currentDownloads
        updateActiveDownloads()
    }

    /**
     * Update download progress
     *
     * @param downloadId Download ID
     * @param downloadedBytes Bytes downloaded
     * @param totalBytes Total file size
     */
    fun updateProgress(downloadId: String, downloadedBytes: Long, totalBytes: Long) {
        val currentDownloads = _downloads.value.toMutableList()
        val index = currentDownloads.indexOfFirst { it.id == downloadId }
        if (index >= 0) {
            val download = currentDownloads[index]
            currentDownloads[index] = download.copy(
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                status = DownloadStatus.IN_PROGRESS
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
                downloadedBytes = 0,
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
        viewModelScope.cancel()
    }
}
