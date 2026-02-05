package com.augmentalis.foundation.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * SearchState - Manages search query state with debounce and callbacks
 *
 * BEFORE (6+ lines):
 * ```
 * private val _searchQuery = MutableStateFlow("")
 * val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
 *
 * fun searchHistory(query: String) {
 *     _searchQuery.value = query
 *     if (query.isBlank()) {
 *         loadHistory()
 *         return
 *     }
 *     viewModelScope.launch {
 *         // perform search
 *     }
 * }
 *
 * fun clearSearch() {
 *     _searchQuery.value = ""
 *     loadHistory()
 * }
 * ```
 *
 * AFTER:
 * ```
 * val search = SearchState(
 *     onEmpty = { loadHistory() },
 *     onSearch = { query -> performSearch(query) }
 * )
 * ```
 *
 * Usage:
 * - Observe query: `search.query.flow`
 * - Execute search: `search.search("term")`
 * - Clear search: `search.clear()`
 */
class SearchState(
    private val onEmpty: suspend () -> Unit = {},
    private val onSearch: suspend (String) -> Unit = {}
) {
    val query = ViewModelState("")

    /**
     * Read-only StateFlow of current query
     */
    val flow: StateFlow<String> = query.flow

    /**
     * Current query value
     */
    val value: String get() = query.value

    /**
     * Execute search with the given query
     *
     * @param scope CoroutineScope for launching search
     * @param searchQuery Query string to search
     */
    fun search(scope: CoroutineScope, searchQuery: String) {
        query.value = searchQuery

        scope.launch {
            if (searchQuery.isBlank()) {
                onEmpty()
            } else {
                onSearch(searchQuery)
            }
        }
    }

    /**
     * Clear search and reset to default state
     */
    fun clear(scope: CoroutineScope) {
        query.value = ""
        scope.launch {
            onEmpty()
        }
    }

    /**
     * Check if search is active
     */
    val isActive: Boolean get() = query.value.isNotBlank()
}

/**
 * SettingsUpdater<T> - Simplifies the repetitive settings copy-update pattern
 *
 * BEFORE (4 lines per setter):
 * ```
 * fun setDesktopMode(enabled: Boolean) {
 *     _settings.value?.let { current ->
 *         updateSettings(current.copy(useDesktopMode = enabled))
 *     }
 * }
 * ```
 *
 * AFTER:
 * ```
 * val settingsUpdater = SettingsUpdater(settingsState) { updated -> repository.updateSettings(updated) }
 *
 * fun setDesktopMode(enabled: Boolean) = settingsUpdater.update { it.copy(useDesktopMode = enabled) }
 * ```
 *
 * Note: This works best with data classes that have copy() method.
 */
class SettingsUpdater<T : Any>(
    private val state: NullableState<T>,
    private val scope: CoroutineScope,
    private val onUpdate: suspend (T) -> Result<Unit>
) {
    /**
     * Update settings using transform function
     *
     * Only performs update if current value is not null.
     */
    fun update(transform: (T) -> T) {
        state.ifPresent { current ->
            val updated = transform(current)
            state.value = updated
            scope.launch {
                onUpdate(updated)
            }
        }
    }

    /**
     * Update with validation
     *
     * @param validate Validation function that returns null if valid, or error message if invalid
     */
    fun updateValidated(
        transform: (T) -> T,
        validate: (T) -> String? = { null },
        onError: (String) -> Unit = {}
    ) {
        state.ifPresent { current ->
            val updated = transform(current)
            val error = validate(updated)
            if (error != null) {
                onError(error)
                return
            }
            state.value = updated
            scope.launch {
                onUpdate(updated)
            }
        }
    }
}
