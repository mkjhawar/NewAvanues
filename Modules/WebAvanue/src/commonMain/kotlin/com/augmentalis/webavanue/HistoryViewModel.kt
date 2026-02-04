package com.augmentalis.webavanue

import com.augmentalis.webavanue.HistoryEntry
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.util.BaseStatefulViewModel
import com.augmentalis.webavanue.util.ListState
import com.augmentalis.webavanue.util.ViewModelState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.datetime.Instant

/**
 * HistoryViewModel - Manages browsing history state and operations
 *
 * Refactored to use StateFlow utilities for reduced boilerplate.
 *
 * State:
 * - history: List<HistoryEntry> - All history entries (or filtered)
 * - searchQuery: String - Current search query
 * - isLoading: Boolean - Loading state
 * - error: String? - Error message
 */
class HistoryViewModel(
    private val repository: BrowserRepository
) : BaseStatefulViewModel() {

    // State: History entries
    private val _history = ListState<HistoryEntry>()
    val history: StateFlow<List<HistoryEntry>> = _history.flow

    // State: Search query
    private val _searchQuery = ViewModelState("")
    val searchQuery: StateFlow<String> = _searchQuery.flow

    // Expose UiState flows
    val isLoading: StateFlow<Boolean> = uiState.isLoading.flow
    val error: StateFlow<String?> = uiState.error.flow

    init {
        loadHistory()
    }

    /**
     * Load all history entries
     */
    fun loadHistory() {
        launch {
            uiState.isLoading.value = true
            uiState.error.clear()

            repository.observeHistory()
                .catch { e ->
                    uiState.error.value = "Failed to load history: ${e.message}"
                    uiState.isLoading.value = false
                }
                .collect { historyList ->
                    _history.replaceAll(historyList)
                    uiState.isLoading.value = false
                }
        }
    }

    /**
     * Load history entries for a specific date range
     */
    fun loadHistoryByDateRange(startDate: Instant, endDate: Instant) {
        execute {
            repository.getHistoryByDateRange(startDate, endDate)
                .onSuccess { _history.replaceAll(it) }
        }
    }

    /**
     * Search history by query
     */
    fun searchHistory(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            loadHistory()
            return
        }

        execute {
            repository.searchHistory(query)
                .onSuccess { _history.replaceAll(it) }
        }
    }

    /**
     * Add a history entry
     */
    fun addHistoryEntry(url: String, title: String) {
        launch {
            uiState.error.clear()
            val entry = HistoryEntry.create(url = url, title = title)
            repository.addHistoryEntry(entry)
                .onFailure { e ->
                    uiState.error.value = "Failed to add history entry: ${e.message}"
                }
        }
    }

    /**
     * Delete a specific history entry
     */
    fun deleteHistoryEntry(entryId: String) {
        launch {
            uiState.error.clear()
            repository.deleteHistoryEntry(entryId)
                .onFailure { e ->
                    uiState.error.value = "Failed to delete entry: ${e.message}"
                }
        }
    }

    /**
     * Clear all history
     */
    fun clearHistory() {
        execute { repository.clearAllHistory() }
    }

    /**
     * Clear history by time range
     */
    fun clearHistoryByTimeRange(startTime: Instant, endTime: Instant) {
        execute { repository.clearHistoryByDateRange(startTime, endTime) }
    }

    /**
     * Get most visited sites
     */
    fun getMostVisited(limit: Int = 10) {
        execute {
            repository.getMostVisited(limit)
                .onSuccess { _history.replaceAll(it) }
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
        uiState.clearError()
    }
}
