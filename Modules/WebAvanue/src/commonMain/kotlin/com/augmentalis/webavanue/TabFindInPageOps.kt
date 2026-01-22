package com.augmentalis.webavanue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Find in page operations handler
 *
 * Encapsulates all find-in-page functionality including:
 * - Show/hide find bar
 * - Query management
 * - Navigation between matches
 * - Case sensitivity toggle
 */
class TabFindInPageOps {
    private val _findInPageState = MutableStateFlow(FindInPageState())
    val findInPageState: StateFlow<FindInPageState> = _findInPageState.asStateFlow()

    /**
     * Show find in page bar with optional initial query
     *
     * @param initialQuery Optional initial search query
     */
    fun showFindInPage(initialQuery: String = "") {
        _findInPageState.value = _findInPageState.value.copy(
            isVisible = true,
            query = initialQuery
        )
    }

    /**
     * Hide find in page bar and clear search
     */
    fun hideFindInPage() {
        _findInPageState.value = FindInPageState(isVisible = false)
    }

    /**
     * Update find in page query
     * This will trigger a new search via WebViewController
     *
     * @param query Search query string
     */
    fun updateFindQuery(query: String) {
        _findInPageState.value = _findInPageState.value.copy(
            query = query,
            currentMatch = 0,
            totalMatches = 0
        )
    }

    /**
     * Update find in page results from WebView
     * Called by WebViewController after search completes
     *
     * @param currentMatch Current match index (0-based)
     * @param totalMatches Total number of matches
     */
    fun updateFindResults(currentMatch: Int, totalMatches: Int) {
        _findInPageState.value = _findInPageState.value.copy(
            currentMatch = currentMatch,
            totalMatches = totalMatches
        )
    }

    /**
     * Toggle case sensitivity for find in page
     */
    fun toggleFindCaseSensitive() {
        _findInPageState.value = _findInPageState.value.copy(
            caseSensitive = !_findInPageState.value.caseSensitive
        )
    }

    /**
     * Navigate to next match
     * Wraps around from last to first
     */
    fun findNext() {
        val state = _findInPageState.value
        if (state.totalMatches > 0) {
            val nextMatch = (state.currentMatch + 1) % state.totalMatches
            _findInPageState.value = state.copy(currentMatch = nextMatch)
        }
    }

    /**
     * Navigate to previous match
     * Wraps around from first to last
     */
    fun findPrevious() {
        val state = _findInPageState.value
        if (state.totalMatches > 0) {
            val prevMatch = if (state.currentMatch == 0) {
                state.totalMatches - 1
            } else {
                state.currentMatch - 1
            }
            _findInPageState.value = state.copy(currentMatch = prevMatch)
        }
    }

    /**
     * Clear find in page highlights and reset state
     */
    fun clearFind() {
        _findInPageState.value = _findInPageState.value.copy(
            query = "",
            currentMatch = 0,
            totalMatches = 0
        )
    }
}
