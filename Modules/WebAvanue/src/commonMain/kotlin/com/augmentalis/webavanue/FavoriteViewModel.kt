package com.augmentalis.webavanue

import com.augmentalis.webavanue.Favorite
import com.augmentalis.webavanue.FavoriteFolder
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.BookmarkImportExport
import com.augmentalis.webavanue.parseHtmlWithData
import com.augmentalis.webavanue.util.BaseStatefulViewModel
import com.augmentalis.webavanue.util.ListState
import com.augmentalis.webavanue.util.ViewModelState
import com.augmentalis.webavanue.util.NullableState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch

/**
 * FavoriteViewModel - Manages favorite/bookmark state and operations
 *
 * Refactored to use StateFlow utilities for reduced boilerplate.
 *
 * State:
 * - favorites: List<Favorite> - All favorites (or filtered)
 * - folders: List<FavoriteFolder> - All favorite folders
 * - selectedFolderId: String? - Currently selected folder filter
 * - searchQuery: String - Current search query
 * - isLoading: Boolean - Loading state
 * - error: String? - Error message
 */
class FavoriteViewModel(
    private val repository: BrowserRepository
) : BaseStatefulViewModel() {

    // State: All favorites (or filtered)
    private val _favorites = ListState<Favorite>()
    val favorites: StateFlow<List<Favorite>> = _favorites.flow

    // State: All folders
    private val _folders = ListState<FavoriteFolder>()
    val folders: StateFlow<List<FavoriteFolder>> = _folders.flow

    // State: Selected folder ID
    private val _selectedFolderId = NullableState<String>()
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.flow

    // State: Search query
    private val _searchQuery = ViewModelState("")
    val searchQuery: StateFlow<String> = _searchQuery.flow

    // Expose UiState flows
    val isLoading: StateFlow<Boolean> = uiState.isLoading.flow
    val error: StateFlow<String?> = uiState.error.flow

    init {
        loadFavorites()
        loadFolders()
    }

    /**
     * Load all favorites (optionally filtered by folder)
     */
    fun loadFavorites(folderId: String? = null) {
        launch {
            uiState.isLoading.value = true
            uiState.error.clear()
            _selectedFolderId.value = folderId

            repository.observeFavorites()
                .catch { e ->
                    uiState.error.value = "Failed to load favorites: ${e.message}"
                    uiState.isLoading.value = false
                }
                .collect { favoriteList ->
                    val filtered = if (folderId != null) {
                        favoriteList.filter { it.folderId == folderId }
                    } else {
                        favoriteList
                    }
                    _favorites.replaceAll(filtered)
                    uiState.isLoading.value = false
                }
        }
    }

    /**
     * Load all favorite folders
     */
    fun loadFolders() {
        launch {
            repository.getAllFolders()
                .onSuccess { _folders.replaceAll(it) }
                .onFailure { e -> uiState.error.value = "Failed to load folders: ${e.message}" }
        }
    }

    /**
     * Search favorites by query
     */
    fun searchFavorites(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            loadFavorites(_selectedFolderId.value)
            return
        }

        execute {
            repository.searchFavorites(query)
                .onSuccess { _favorites.replaceAll(it) }
        }
    }

    /**
     * Add a new favorite with duplicate prevention
     */
    suspend fun addFavorite(
        url: String,
        title: String,
        favicon: String? = null,
        description: String? = null,
        folderId: String? = _selectedFolderId.value
    ): Boolean {
        return try {
            // Check for duplicates
            val isDuplicate = repository.isFavorite(url).getOrDefault(false)
            if (isDuplicate) {
                uiState.error.value = "This page is already in your favorites"
                return false
            }

            uiState.isLoading.value = true
            uiState.error.clear()

            val favorite = Favorite.create(
                url = url,
                title = title,
                favicon = favicon,
                description = description,
                folderId = folderId
            )

            var result = false
            repository.addFavorite(favorite)
                .onSuccess {
                    uiState.isLoading.value = false
                    result = true
                }
                .onFailure { e ->
                    uiState.error.value = "Failed to add favorite: ${e.message}"
                    uiState.isLoading.value = false
                }

            result
        } catch (e: Exception) {
            uiState.error.value = "Error adding favorite: ${e.message}"
            uiState.isLoading.value = false
            println("FavoriteViewModel: Exception in addFavorite: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Remove a favorite
     */
    fun removeFavorite(favoriteId: String) {
        launch {
            uiState.error.clear()
            repository.removeFavorite(favoriteId)
                .onFailure { e -> uiState.error.value = "Failed to remove favorite: ${e.message}" }
        }
    }

    /**
     * Update a favorite
     */
    fun updateFavorite(favorite: Favorite) {
        launch {
            uiState.error.clear()
            repository.updateFavorite(favorite)
                .onFailure { e -> uiState.error.value = "Failed to update favorite: ${e.message}" }
        }
    }

    /**
     * Move favorite to different folder
     */
    fun moveFavoriteToFolder(favoriteId: String, folderId: String?) {
        launch {
            uiState.error.clear()
            repository.getFavorite(favoriteId)
                .onSuccess { favorite ->
                    if (favorite != null) {
                        repository.updateFavorite(favorite.copy(folderId = folderId))
                            .onFailure { e -> uiState.error.value = "Failed to move favorite: ${e.message}" }
                    }
                }
                .onFailure { e -> uiState.error.value = "Failed to find favorite: ${e.message}" }
        }
    }

    /**
     * Check if a URL is favorited
     */
    suspend fun isFavorite(url: String): Boolean {
        return try {
            repository.isFavorite(url).getOrDefault(false)
        } catch (e: Exception) {
            println("FavoriteViewModel: Exception in isFavorite: ${e.message}")
            false
        }
    }

    /**
     * Create a new folder
     */
    fun createFolder(name: String, parentId: String? = null) {
        launch {
            uiState.error.clear()
            val folder = FavoriteFolder.create(name = name, parentId = parentId)
            repository.createFolder(folder)
                .onSuccess { loadFolders() }
                .onFailure { e -> uiState.error.value = "Failed to create folder: ${e.message}" }
        }
    }

    /**
     * Delete a folder
     */
    fun deleteFolder(folderId: String, deleteContents: Boolean = false) {
        launch {
            uiState.error.clear()
            repository.deleteFolder(folderId, deleteContents)
                .onSuccess { loadFolders() }
                .onFailure { e -> uiState.error.value = "Failed to delete folder: ${e.message}" }
        }
    }

    /**
     * Select a folder to filter by
     */
    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
        loadFavorites(folderId)
    }

    fun filterByFolder(folderId: String?) = selectFolder(folderId)

    fun clearSearch() {
        _searchQuery.value = ""
        loadFavorites(_selectedFolderId.value)
    }

    fun clearError() {
        uiState.clearError()
    }

    // ==================== Import/Export Operations ====================

    fun exportBookmarks(): String {
        return BookmarkImportExport.exportToHtml(
            favorites = _favorites.value,
            folders = _folders.value
        )
    }

    suspend fun importBookmarks(
        html: String,
        skipDuplicates: Boolean = true
    ): BookmarkImportExport.ImportResult {
        return try {
            uiState.isLoading.value = true
            uiState.error.clear()

            val existingUrls = _favorites.value.map { it.url }.toSet()

            val importData = BookmarkImportExport.parseHtmlWithData(
                html = html,
                existingUrls = existingUrls,
                skipDuplicates = skipDuplicates
            )

            // Import folders first
            for (folder in importData.folders) {
                repository.createFolder(folder)
                    .onFailure { e -> println("Failed to import folder ${folder.name}: ${e.message}") }
            }

            // Import bookmarks
            var imported = 0
            for (favorite in importData.favorites) {
                repository.addFavorite(favorite)
                    .onSuccess { imported++ }
                    .onFailure { e -> println("Failed to import bookmark ${favorite.title}: ${e.message}") }
            }

            uiState.isLoading.value = false
            importData.result.copy(imported = imported)
        } catch (e: Exception) {
            uiState.error.value = "Import failed: ${e.message}"
            uiState.isLoading.value = false
            BookmarkImportExport.ImportResult(imported = 0, skipped = 0, folders = 0, errors = listOf(e.message ?: "Unknown error"))
        }
    }

    fun generateExportFilename(): String = BookmarkImportExport.generateExportFilename()
}

// Type alias for backward compatibility
typealias BookmarkViewModel = FavoriteViewModel
