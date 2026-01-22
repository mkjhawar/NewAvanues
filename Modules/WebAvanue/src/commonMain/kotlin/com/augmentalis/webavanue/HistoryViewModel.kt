package com.augmentalis.webavanue

import com.augmentalis.webavanue.HistoryEntry
import com.augmentalis.webavanue.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/**
 * HistoryViewModel - Manages browsing history state and operations
 *
 * Responsibilities:
 * - Load and observe browsing history
 * - Search history entries
 * - Filter history by date
 * - Clear history (all or by time range)
 * - Add history entries
 *
 * State:
 * - history: List<HistoryEntry> - All history entries (or filtered)
 * - searchQuery: String - Current search query
 * - isLoading: Boolean - Loading state
 * - error: String? - Error message
 */
class HistoryViewModel(
    private val repository: BrowserRepository
) {
    // Coroutine scope
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State: History entries (all or filtered)
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    // State: Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // State: Loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State: Error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * Load all history entries
     */
    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.observeHistory()
                .catch { e ->
                    _error.value = "Failed to load history: ${e.message}"
                    _isLoading.value = false
                }
                .collect { historyList ->
                    _history.value = historyList
                    _isLoading.value = false
                }
        }
    }

    /**
     * Load history entries for a specific date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     */
    fun loadHistoryByDateRange(startDate: Instant, endDate: Instant) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getHistoryByDateRange(startDate, endDate)
                .onSuccess { historyList ->
                    _history.value = historyList
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = "Failed to load history: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Search history by query
     *
     * @param query Search query (searches title and URL)
     */
    fun searchHistory(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            // Empty query, reload all history
            loadHistory()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.searchHistory(query)
                .onSuccess { results ->
                    _history.value = results
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = "Search failed: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Add a history entry
     *
     * @param url URL visited
     * @param title Page title
     */
    fun addHistoryEntry(url: String, title: String) {
        viewModelScope.launch {
            _error.value = null

            val entry = HistoryEntry.create(url = url, title = title)
            repository.addHistoryEntry(entry)
                .onSuccess {
                    // Entry added, will appear via Flow
                }
                .onFailure { e ->
                    _error.value = "Failed to add history entry: ${e.message}"
                }
        }
    }

    /**
     * Delete a specific history entry
     *
     * @param entryId Entry ID to delete
     */
    fun deleteHistoryEntry(entryId: String) {
        viewModelScope.launch {
            _error.value = null

            repository.deleteHistoryEntry(entryId)
                .onSuccess {
                    // Entry deleted via Flow
                }
                .onFailure { e ->
                    _error.value = "Failed to delete entry: ${e.message}"
                }
        }
    }

    /**
     * Clear all history
     */
    fun clearHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.clearAllHistory()
                .onSuccess {
                    _isLoading.value = false
                    // History cleared, Flow will update
                }
                .onFailure { e ->
                    _error.value = "Failed to clear history: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Clear history by time range
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     */
    fun clearHistoryByTimeRange(startTime: Instant, endTime: Instant) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.clearHistoryByDateRange(startTime, endTime)
                .onSuccess {
                    _isLoading.value = false
                    // History cleared, Flow will update
                }
                .onFailure { e ->
                    _error.value = "Failed to clear history: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Get most visited sites
     *
     * @param limit Maximum number of results
     */
    fun getMostVisited(limit: Int = 10) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getMostVisited(limit)
                .onSuccess { results ->
                    _history.value = results
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = "Failed to get most visited: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Clear search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
        loadHistory()
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
