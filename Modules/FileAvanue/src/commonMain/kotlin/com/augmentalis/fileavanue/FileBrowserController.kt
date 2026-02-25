package com.augmentalis.fileavanue

import com.augmentalis.fileavanue.model.FileBrowserState
import com.augmentalis.fileavanue.model.FileCategory
import com.augmentalis.fileavanue.model.FileItem
import com.augmentalis.fileavanue.model.FileSortMode
import com.augmentalis.fileavanue.model.FileViewMode
import com.augmentalis.fileavanue.model.PathSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shared file browser controller — all state management lives in commonMain.
 *
 * Sorting, filtering, selection, breadcrumb navigation, and category browsing
 * are fully platform-agnostic. Platform code only provides:
 * 1. IStorageProvider implementations (actual file I/O)
 * 2. UI composables that observe [state]
 *
 * This design gives ~60% code sharing across Android, Desktop, iOS, and Web.
 */
class FileBrowserController(
    private val providers: List<IStorageProvider>
) {
    private val _state = MutableStateFlow(FileBrowserState())
    val state: StateFlow<FileBrowserState> = _state.asStateFlow()

    val availableProviders: List<IStorageProvider> get() = providers

    private val currentProvider: IStorageProvider?
        get() = providers.find { it.providerId == _state.value.currentProviderId }

    /**
     * Load directory contents at the given path.
     * Updates breadcrumbs, items (sorted/filtered), and storage info.
     */
    suspend fun loadDirectory(path: String) {
        val provider = currentProvider ?: return
        _state.update { it.copy(isLoading = true, error = null, searchQuery = "") }

        try {
            val items = provider.listFiles(path, _state.value.showHidden)
            val sorted = sortItems(items, _state.value.sortMode)
            val segments = provider.getPathSegments(path)
            val storageInfo = provider.getStorageInfo()

            _state.update {
                it.copy(
                    currentPath = path,
                    items = sorted,
                    breadcrumbs = segments,
                    isLoading = false,
                    selectedUris = emptySet(),
                    storageInfo = storageInfo,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load directory") }
        }
    }

    /** Navigate to the root of the current provider. */
    suspend fun navigateToRoot() {
        val provider = currentProvider ?: return
        loadDirectory(provider.getRootPath())
    }

    /** Navigate up one level (pop breadcrumb). */
    suspend fun navigateToParent() {
        val breadcrumbs = _state.value.breadcrumbs
        if (breadcrumbs.size >= 2) {
            loadDirectory(breadcrumbs[breadcrumbs.size - 2].uri)
        }
    }

    /** Navigate to a specific breadcrumb segment. */
    suspend fun navigateToBreadcrumb(segment: PathSegment) {
        loadDirectory(segment.uri)
    }

    /** Toggle selection of a single item. */
    fun selectItem(uri: String) {
        _state.update { state ->
            val newSelection = if (uri in state.selectedUris) {
                state.selectedUris - uri
            } else {
                state.selectedUris + uri
            }
            state.copy(selectedUris = newSelection)
        }
    }

    /** Clear all selections. */
    fun clearSelection() {
        _state.update { it.copy(selectedUris = emptySet()) }
    }

    /** Toggle select all / deselect all. */
    fun toggleSelectAll() {
        _state.update { state ->
            if (state.allSelected) {
                state.copy(selectedUris = emptySet())
            } else {
                state.copy(selectedUris = state.items.map { it.uri }.toSet())
            }
        }
    }

    /** Change sort mode and re-sort current items. */
    fun setSortMode(mode: FileSortMode) {
        _state.update { state ->
            state.copy(
                sortMode = mode,
                items = sortItems(state.items, mode)
            )
        }
    }

    /** Switch between list and grid view. */
    fun setViewMode(mode: FileViewMode) {
        _state.update { it.copy(viewMode = mode) }
    }

    /** Toggle hidden file visibility and reload. */
    suspend fun toggleShowHidden() {
        _state.update { it.copy(showHidden = !it.showHidden) }
        val path = _state.value.currentPath
        if (path.isNotEmpty()) {
            loadDirectory(path)
        }
    }

    /** Switch to a different storage provider and load its root. */
    suspend fun setProvider(providerId: String) {
        val provider = providers.find { it.providerId == providerId } ?: return
        _state.update { it.copy(currentProviderId = providerId) }
        loadDirectory(provider.getRootPath())
    }

    /** Search files from the current path. */
    suspend fun searchFiles(query: String) {
        if (query.isBlank()) {
            // Clear search — reload current directory
            val path = _state.value.currentPath
            if (path.isNotEmpty()) loadDirectory(path)
            return
        }

        val provider = currentProvider ?: return
        _state.update { it.copy(isLoading = true, searchQuery = query) }

        try {
            val results = provider.searchFiles(_state.value.currentPath, query)
            val sorted = sortItems(results, _state.value.sortMode)
            _state.update { it.copy(items = sorted, isLoading = false) }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message ?: "Search failed") }
        }
    }

    /** Load files for a specific category (e.g., all images on device). */
    suspend fun loadCategory(category: FileCategory) {
        val provider = currentProvider ?: return
        _state.update { it.copy(isLoading = true, error = null, searchQuery = "") }

        try {
            val items = provider.getCategoryFiles(category)
            val sorted = sortItems(items, _state.value.sortMode)
            _state.update {
                it.copy(
                    currentPath = "category:${category.name}",
                    items = sorted,
                    breadcrumbs = listOf(
                        PathSegment("Home", ""),
                        PathSegment(category.displayName, "category:${category.name}")
                    ),
                    isLoading = false,
                    selectedUris = emptySet(),
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load category") }
        }
    }

    /** Load recently modified files. */
    suspend fun loadRecent() {
        val provider = currentProvider ?: return
        _state.update { it.copy(isLoading = true, error = null) }

        try {
            val items = provider.getRecentFiles(30)
            _state.update {
                it.copy(
                    currentPath = "category:RECENT",
                    items = items,
                    breadcrumbs = listOf(
                        PathSegment("Home", ""),
                        PathSegment("Recent", "category:RECENT")
                    ),
                    isLoading = false,
                    selectedUris = emptySet(),
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load recent files") }
        }
    }

    /** Delete all selected files. Returns count of successfully deleted files. */
    suspend fun deleteSelected(): Int {
        val provider = currentProvider ?: return 0
        val uris = _state.value.selectedUris.toList()
        var deleted = 0

        for (uri in uris) {
            if (provider.deleteFile(uri)) deleted++
        }

        if (deleted > 0) {
            _state.update { state ->
                state.copy(
                    items = state.items.filterNot { it.uri in uris },
                    selectedUris = emptySet()
                )
            }
        }
        return deleted
    }

    /** Get file counts per category for the dashboard. */
    suspend fun getCategoryCounts(): Map<FileCategory, Int> {
        val provider = currentProvider ?: return emptyMap()
        val counts = mutableMapOf<FileCategory, Int>()
        for (category in FileCategory.entries) {
            try {
                val items = provider.getCategoryFiles(category)
                counts[category] = items.size
            } catch (_: Exception) {
                counts[category] = 0
            }
        }
        return counts
    }

    /**
     * Sort items with directories first, then by the specified mode.
     * Directories are always grouped at the top regardless of sort direction.
     */
    private fun sortItems(items: List<FileItem>, mode: FileSortMode): List<FileItem> {
        val dirs = items.filter { it.isDirectory }
        val files = items.filter { !it.isDirectory }

        val sortedDirs = when (mode) {
            FileSortMode.NAME_ASC -> dirs.sortedBy { it.name.lowercase() }
            FileSortMode.NAME_DESC -> dirs.sortedByDescending { it.name.lowercase() }
            FileSortMode.DATE_ASC -> dirs.sortedBy { it.dateModified }
            FileSortMode.DATE_DESC -> dirs.sortedByDescending { it.dateModified }
            FileSortMode.SIZE_ASC -> dirs.sortedBy { it.name.lowercase() }
            FileSortMode.SIZE_DESC -> dirs.sortedBy { it.name.lowercase() }
            FileSortMode.TYPE_ASC -> dirs.sortedBy { it.name.lowercase() }
        }

        val sortedFiles = when (mode) {
            FileSortMode.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            FileSortMode.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
            FileSortMode.DATE_ASC -> files.sortedBy { it.dateModified }
            FileSortMode.DATE_DESC -> files.sortedByDescending { it.dateModified }
            FileSortMode.SIZE_ASC -> files.sortedBy { it.fileSizeBytes }
            FileSortMode.SIZE_DESC -> files.sortedByDescending { it.fileSizeBytes }
            FileSortMode.TYPE_ASC -> files.sortedBy { it.extension.lowercase() }
        }

        return sortedDirs + sortedFiles
    }
}
