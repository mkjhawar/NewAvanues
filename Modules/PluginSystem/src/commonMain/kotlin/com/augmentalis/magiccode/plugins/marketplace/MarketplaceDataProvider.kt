package com.augmentalis.magiccode.plugins.marketplace

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * StateFlow-based data provider for marketplace UI.
 *
 * MarketplaceDataProvider manages marketplace data state for consumption
 * by UI components. It provides reactive StateFlow properties for search
 * results, plugin details, installation progress, and error states.
 *
 * ## State Management
 * The provider exposes several StateFlow properties:
 * - [searchResults]: Current search results
 * - [selectedDetails]: Currently viewed plugin details
 * - [installProgress]: Download/install progress for active operations
 * - [loadingState]: Current loading state
 * - [error]: Most recent error, if any
 *
 * ## Usage
 * ```kotlin
 * val provider = MarketplaceDataProvider(api, cache)
 *
 * // Observe in UI
 * provider.searchResults.collect { results ->
 *     displayResults(results)
 * }
 *
 * provider.loadingState.collect { state ->
 *     showLoadingIndicator(state == LoadingState.LOADING)
 * }
 *
 * // Trigger actions
 * provider.search("voice commands")
 * provider.loadDetails("com.example.plugin")
 * provider.startInstall("com.example.plugin", "1.0.0")
 * ```
 *
 * ## Caching
 * The provider integrates with [MarketplaceCache] for efficient data
 * retrieval. Cached data is used when available, with background
 * refresh for stale data.
 *
 * @param api Marketplace API for data fetching
 * @param cache Marketplace cache for data caching
 * @param scope CoroutineScope for async operations
 * @since 1.0.0
 * @see MarketplaceApi
 * @see MarketplaceCache
 */
class MarketplaceDataProvider(
    private val api: MarketplaceApi,
    private val cache: MarketplaceCache = MarketplaceCache(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    companion object {
        private const val TAG = "MarketplaceDataProvider"
    }

    // ==================== State Flows ====================

    private val _searchResults = MutableStateFlow<List<PluginListing>>(emptyList())

    /**
     * StateFlow of current search results.
     *
     * Emits empty list initially and after clearing search.
     * Emits search results after successful search.
     */
    val searchResults: StateFlow<List<PluginListing>> = _searchResults.asStateFlow()

    private val _currentQuery = MutableStateFlow<String?>(null)

    /**
     * StateFlow of current search query.
     *
     * Null when no search has been performed.
     */
    val currentQuery: StateFlow<String?> = _currentQuery.asStateFlow()

    private val _currentFilters = MutableStateFlow(SearchFilters())

    /**
     * StateFlow of current search filters.
     */
    val currentFilters: StateFlow<SearchFilters> = _currentFilters.asStateFlow()

    private val _selectedDetails = MutableStateFlow<PluginDetails?>(null)

    /**
     * StateFlow of currently selected plugin details.
     *
     * Null when no plugin is selected for viewing.
     */
    val selectedDetails: StateFlow<PluginDetails?> = _selectedDetails.asStateFlow()

    private val _selectedVersions = MutableStateFlow<List<VersionInfo>>(emptyList())

    /**
     * StateFlow of versions for currently selected plugin.
     */
    val selectedVersions: StateFlow<List<VersionInfo>> = _selectedVersions.asStateFlow()

    private val _installProgress = MutableStateFlow<Map<String, InstallProgress>>(emptyMap())

    /**
     * StateFlow of installation progress by plugin ID.
     *
     * Map is empty when no installations are in progress.
     */
    val installProgress: StateFlow<Map<String, InstallProgress>> = _installProgress.asStateFlow()

    private val _loadingState = MutableStateFlow(LoadingState.IDLE)

    /**
     * StateFlow of current loading state.
     */
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private val _error = MutableStateFlow<MarketplaceError?>(null)

    /**
     * StateFlow of most recent error.
     *
     * Null when no error has occurred or after clearing.
     */
    val error: StateFlow<MarketplaceError?> = _error.asStateFlow()

    // Debounce job for search
    private var searchDebounceJob: Job? = null

    // ==================== Search Operations ====================

    /**
     * Search for plugins with the given query.
     *
     * Updates [searchResults], [currentQuery], and [loadingState].
     * Uses debouncing to prevent excessive API calls during typing.
     *
     * @param query Search query string
     * @param filters Optional search filters
     * @param debounceMs Debounce delay in milliseconds (0 for immediate)
     */
    fun search(
        query: String,
        filters: SearchFilters = _currentFilters.value,
        debounceMs: Long = 300L
    ) {
        // Cancel previous debounce job
        searchDebounceJob?.cancel()

        if (query.isBlank()) {
            clearSearch()
            return
        }

        _currentQuery.value = query
        _currentFilters.value = filters

        searchDebounceJob = scope.launch {
            if (debounceMs > 0) {
                delay(debounceMs)
            }

            performSearch(query, filters)
        }
    }

    /**
     * Update search filters and re-run search.
     *
     * @param filters New search filters
     */
    fun updateFilters(filters: SearchFilters) {
        _currentFilters.value = filters
        val query = _currentQuery.value
        if (!query.isNullOrBlank()) {
            search(query, filters, debounceMs = 0)
        }
    }

    /**
     * Clear search results and query.
     */
    fun clearSearch() {
        searchDebounceJob?.cancel()
        _currentQuery.value = null
        _searchResults.value = emptyList()
        _currentFilters.value = SearchFilters()
        _loadingState.value = LoadingState.IDLE
    }

    /**
     * Refresh current search results.
     *
     * Forces a fresh search bypassing cache.
     */
    fun refreshSearch() {
        val query = _currentQuery.value
        if (!query.isNullOrBlank()) {
            scope.launch {
                cache.invalidateSearches()
                performSearch(query, _currentFilters.value)
            }
        }
    }

    private suspend fun performSearch(query: String, filters: SearchFilters) {
        _loadingState.value = LoadingState.LOADING
        _error.value = null

        PluginLog.d(TAG, "Searching for: $query")

        // Check cache first
        val cached = cache.getCachedSearch(query, filters)
        if (cached != null) {
            _searchResults.value = cached
            _loadingState.value = LoadingState.SUCCESS
            PluginLog.d(TAG, "Search cache hit: ${cached.size} results")
            return
        }

        // Fetch from API
        api.search(query, filters).fold(
            onSuccess = { results ->
                _searchResults.value = results
                _loadingState.value = LoadingState.SUCCESS
                cache.cacheSearch(query, filters, results)
                PluginLog.i(TAG, "Search completed: ${results.size} results for '$query'")
            },
            onFailure = { error ->
                _loadingState.value = LoadingState.ERROR
                _error.value = when (error) {
                    is MarketplaceError -> error
                    else -> MarketplaceError.NetworkError(error)
                }
                PluginLog.e(TAG, "Search failed: ${error.message}")
            }
        )
    }

    // ==================== Details Operations ====================

    /**
     * Load details for a specific plugin.
     *
     * Updates [selectedDetails] and [selectedVersions].
     *
     * @param pluginId Plugin ID to load details for
     */
    fun loadDetails(pluginId: String) {
        scope.launch {
            _loadingState.value = LoadingState.LOADING
            _error.value = null

            PluginLog.d(TAG, "Loading details for: $pluginId")

            // Check cache first
            val cached = cache.getCachedDetails(pluginId)
            if (cached != null) {
                _selectedDetails.value = cached
                loadVersions(pluginId) // Also load versions
                _loadingState.value = LoadingState.SUCCESS
                return@launch
            }

            // Fetch from API
            api.getDetails(pluginId).fold(
                onSuccess = { details ->
                    _selectedDetails.value = details
                    _loadingState.value = LoadingState.SUCCESS
                    cache.cacheDetails(pluginId, details)
                    loadVersions(pluginId)
                    PluginLog.i(TAG, "Loaded details for: $pluginId")
                },
                onFailure = { error ->
                    _loadingState.value = LoadingState.ERROR
                    _error.value = when (error) {
                        is MarketplaceError -> error
                        else -> MarketplaceError.NotFound(pluginId)
                    }
                    PluginLog.e(TAG, "Failed to load details: ${error.message}")
                }
            )
        }
    }

    /**
     * Load versions for a specific plugin.
     *
     * @param pluginId Plugin ID to load versions for
     */
    private fun loadVersions(pluginId: String) {
        scope.launch {
            // Check cache first
            val cached = cache.getCachedVersions(pluginId)
            if (cached != null) {
                _selectedVersions.value = cached
                return@launch
            }

            api.getVersions(pluginId).fold(
                onSuccess = { versions ->
                    _selectedVersions.value = versions
                    cache.cacheVersions(pluginId, versions)
                },
                onFailure = { error ->
                    PluginLog.w(TAG, "Failed to load versions for $pluginId: ${error.message}")
                    // Don't set error state for versions - non-critical
                }
            )
        }
    }

    /**
     * Clear selected plugin details.
     */
    fun clearDetails() {
        _selectedDetails.value = null
        _selectedVersions.value = emptyList()
    }

    /**
     * Refresh selected plugin details.
     *
     * Forces fresh fetch bypassing cache.
     */
    fun refreshDetails() {
        val pluginId = _selectedDetails.value?.listing?.pluginId
        if (pluginId != null) {
            scope.launch {
                cache.invalidate(pluginId)
                loadDetails(pluginId)
            }
        }
    }

    // ==================== Install Operations ====================

    /**
     * Start installation of a plugin.
     *
     * Downloads the plugin and tracks progress in [installProgress].
     *
     * @param pluginId Plugin ID to install
     * @param version Version to install
     * @param onComplete Callback when installation completes
     */
    fun startInstall(
        pluginId: String,
        version: String,
        onComplete: ((Result<PluginPackage>) -> Unit)? = null
    ) {
        scope.launch {
            updateInstallProgress(pluginId, InstallProgress(
                pluginId = pluginId,
                version = version,
                state = InstallState.DOWNLOADING,
                progress = 0f
            ))

            PluginLog.i(TAG, "Starting install: $pluginId v$version")

            // Simulate progress updates
            // In real implementation, this would track actual download progress
            for (i in 1..9) {
                delay(200)
                updateInstallProgress(pluginId, InstallProgress(
                    pluginId = pluginId,
                    version = version,
                    state = InstallState.DOWNLOADING,
                    progress = i * 0.1f
                ))
            }

            // Download package
            api.download(pluginId, version).fold(
                onSuccess = { package_ ->
                    updateInstallProgress(pluginId, InstallProgress(
                        pluginId = pluginId,
                        version = version,
                        state = InstallState.INSTALLING,
                        progress = 1f
                    ))

                    // Simulate installation time
                    delay(500)

                    updateInstallProgress(pluginId, InstallProgress(
                        pluginId = pluginId,
                        version = version,
                        state = InstallState.COMPLETED,
                        progress = 1f
                    ))

                    PluginLog.i(TAG, "Install completed: $pluginId v$version")

                    // Clear progress after delay
                    delay(2000)
                    clearInstallProgress(pluginId)

                    onComplete?.invoke(Result.success(package_))
                },
                onFailure = { error ->
                    updateInstallProgress(pluginId, InstallProgress(
                        pluginId = pluginId,
                        version = version,
                        state = InstallState.FAILED,
                        progress = 0f,
                        error = error.message
                    ))

                    PluginLog.e(TAG, "Install failed: $pluginId - ${error.message}")

                    onComplete?.invoke(Result.failure(error))
                }
            )
        }
    }

    /**
     * Cancel an in-progress installation.
     *
     * @param pluginId Plugin ID to cancel
     */
    fun cancelInstall(pluginId: String) {
        scope.launch {
            val current = _installProgress.value[pluginId]
            if (current != null && current.state.isActive()) {
                updateInstallProgress(pluginId, current.copy(
                    state = InstallState.CANCELLED
                ))
                delay(1000)
                clearInstallProgress(pluginId)
                PluginLog.i(TAG, "Install cancelled: $pluginId")
            }
        }
    }

    private fun updateInstallProgress(pluginId: String, progress: InstallProgress) {
        val current = _installProgress.value.toMutableMap()
        current[pluginId] = progress
        _installProgress.value = current
    }

    private fun clearInstallProgress(pluginId: String) {
        val current = _installProgress.value.toMutableMap()
        current.remove(pluginId)
        _installProgress.value = current
    }

    // ==================== Error Handling ====================

    /**
     * Clear current error.
     */
    fun clearError() {
        _error.value = null
    }

    // ==================== Utility ====================

    /**
     * Clean up resources.
     *
     * Call when provider is no longer needed.
     */
    fun cleanup() {
        searchDebounceJob?.cancel()
        scope.cancel()
        PluginLog.d(TAG, "Cleaned up")
    }

    /**
     * Get the cache for direct access.
     *
     * @return MarketplaceCache instance
     */
    fun getCache(): MarketplaceCache = cache
}

/**
 * Loading state enumeration.
 *
 * Represents the current state of an async operation.
 *
 * @since 1.0.0
 */
enum class LoadingState {
    /** No operation in progress */
    IDLE,

    /** Operation in progress */
    LOADING,

    /** Operation completed successfully */
    SUCCESS,

    /** Operation failed with error */
    ERROR
}

/**
 * Installation state enumeration.
 *
 * @since 1.0.0
 */
enum class InstallState {
    /** Queued for download */
    PENDING,

    /** Download in progress */
    DOWNLOADING,

    /** Verifying package */
    VERIFYING,

    /** Installing package */
    INSTALLING,

    /** Installation completed */
    COMPLETED,

    /** Installation failed */
    FAILED,

    /** Installation cancelled */
    CANCELLED;

    /**
     * Check if this state represents an active operation.
     *
     * @return true if operation is in progress
     */
    fun isActive(): Boolean {
        return this in listOf(PENDING, DOWNLOADING, VERIFYING, INSTALLING)
    }

    /**
     * Check if this state represents completion (success or failure).
     *
     * @return true if operation has ended
     */
    fun isTerminal(): Boolean {
        return this in listOf(COMPLETED, FAILED, CANCELLED)
    }
}

/**
 * Installation progress data.
 *
 * Represents the current progress of a plugin installation.
 *
 * @property pluginId Plugin being installed
 * @property version Version being installed
 * @property state Current installation state
 * @property progress Progress percentage (0.0 to 1.0)
 * @property error Error message if failed
 * @since 1.0.0
 */
data class InstallProgress(
    val pluginId: String,
    val version: String,
    val state: InstallState,
    val progress: Float,
    val error: String? = null
) {
    /**
     * Get progress as percentage string.
     *
     * @return Progress as "XX%" string
     */
    fun getProgressPercentage(): String {
        return "${(progress * 100).toInt()}%"
    }

    /**
     * Get human-readable status string.
     *
     * @return Status description
     */
    fun getStatusText(): String {
        return when (state) {
            InstallState.PENDING -> "Waiting..."
            InstallState.DOWNLOADING -> "Downloading ${getProgressPercentage()}"
            InstallState.VERIFYING -> "Verifying..."
            InstallState.INSTALLING -> "Installing..."
            InstallState.COMPLETED -> "Installed"
            InstallState.FAILED -> "Failed: ${error ?: "Unknown error"}"
            InstallState.CANCELLED -> "Cancelled"
        }
    }
}
