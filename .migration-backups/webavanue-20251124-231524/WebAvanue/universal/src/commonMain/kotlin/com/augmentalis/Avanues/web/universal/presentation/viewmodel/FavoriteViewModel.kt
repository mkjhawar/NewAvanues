package com.augmentalis.Avanues.web.universal.presentation.viewmodel

import com.augmentalis.webavanue.domain.model.Favorite
import com.augmentalis.webavanue.domain.model.FavoriteFolder
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * FavoriteViewModel - Manages favorite/bookmark state and operations
 *
 * Responsibilities:
 * - Load and observe favorites
 * - Filter favorites by folder
 * - Search favorites
 * - Add/remove/update favorites
 * - Manage favorite folders
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
) {
    // Coroutine scope
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State: All favorites (or filtered)
    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favorites: StateFlow<List<Favorite>> = _favorites.asStateFlow()

    // State: All folders
    private val _folders = MutableStateFlow<List<FavoriteFolder>>(emptyList())
    val folders: StateFlow<List<FavoriteFolder>> = _folders.asStateFlow()

    // State: Selected folder ID (null = all favorites)
    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

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
        loadFavorites()
        loadFolders()
    }

    /**
     * Load all favorites
     *
     * If folder is selected, loads only favorites in that folder
     */
    fun loadFavorites(folderId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _selectedFolderId.value = folderId

            repository.observeFavorites()
                .catch { e ->
                    _error.value = "Failed to load favorites: ${e.message}"
                    _isLoading.value = false
                }
                .collect { favoriteList ->
                    // Filter by folder if selected
                    val filtered = if (folderId != null) {
                        favoriteList.filter { it.folderId == folderId }
                    } else {
                        favoriteList
                    }
                    _favorites.value = filtered
                    _isLoading.value = false
                }
        }
    }

    /**
     * Load all favorite folders
     */
    fun loadFolders() {
        viewModelScope.launch {
            repository.getAllFolders()
                .onSuccess { folderList ->
                    _folders.value = folderList
                }
                .onFailure { e ->
                    _error.value = "Failed to load folders: ${e.message}"
                }
        }
    }

    /**
     * Search favorites by query
     *
     * @param query Search query (searches title and URL)
     */
    fun searchFavorites(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            // Empty query, reload all favorites
            loadFavorites(_selectedFolderId.value)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.searchFavorites(query)
                .onSuccess { results ->
                    _favorites.value = results
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = "Search failed: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Add a new favorite
     *
     * @param url Favorite URL
     * @param title Favorite title
     * @param favicon Optional favicon URL or data
     * @param description Optional description/notes
     * @param folderId Optional folder ID (null = root)
     */
    fun addFavorite(
        url: String,
        title: String,
        favicon: String? = null,
        description: String? = null,
        folderId: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val favorite = Favorite.create(
                url = url,
                title = title,
                favicon = favicon,
                description = description,
                folderId = folderId
            )
            repository.addFavorite(favorite)
                .onSuccess {
                    _isLoading.value = false
                    // Favorites will update via observeFavorites Flow
                }
                .onFailure { e ->
                    _error.value = "Failed to add favorite: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Remove a favorite
     *
     * @param favoriteId Favorite ID to remove
     */
    fun removeFavorite(favoriteId: String) {
        viewModelScope.launch {
            _error.value = null

            repository.removeFavorite(favoriteId)
                .onSuccess {
                    // Favorite removed via Flow observation
                }
                .onFailure { e ->
                    _error.value = "Failed to remove favorite: ${e.message}"
                }
        }
    }

    /**
     * Update a favorite
     *
     * @param favorite Updated favorite
     */
    fun updateFavorite(favorite: Favorite) {
        viewModelScope.launch {
            _error.value = null

            repository.updateFavorite(favorite)
                .onSuccess {
                    // Update will be reflected via Flow
                }
                .onFailure { e ->
                    _error.value = "Failed to update favorite: ${e.message}"
                }
        }
    }

    /**
     * Move favorite to different folder
     *
     * @param favoriteId Favorite ID
     * @param folderId Target folder ID (null = root)
     */
    fun moveFavoriteToFolder(favoriteId: String, folderId: String?) {
        viewModelScope.launch {
            _error.value = null

            // Get the current favorite
            repository.getFavorite(favoriteId)
                .onSuccess { favorite ->
                    if (favorite != null) {
                        val updated = favorite.copy(folderId = folderId)
                        repository.updateFavorite(updated)
                            .onFailure { e ->
                                _error.value = "Failed to move favorite: ${e.message}"
                            }
                    }
                }
                .onFailure { e ->
                    _error.value = "Failed to find favorite: ${e.message}"
                }
        }
    }

    /**
     * Check if a URL is favorited
     *
     * @param url URL to check
     * @return True if favorited, false otherwise
     */
    suspend fun isFavorite(url: String): Boolean {
        return repository.isFavorite(url).getOrDefault(false)
    }

    /**
     * Create a new folder
     *
     * @param name Folder name
     * @param parentId Parent folder ID (null = root level)
     */
    fun createFolder(name: String, parentId: String? = null) {
        viewModelScope.launch {
            _error.value = null

            val folder = FavoriteFolder.create(name = name, parentId = parentId)
            repository.createFolder(folder)
                .onSuccess {
                    loadFolders() // Reload folders
                }
                .onFailure { e ->
                    _error.value = "Failed to create folder: ${e.message}"
                }
        }
    }

    /**
     * Delete a folder
     *
     * @param folderId Folder ID to delete
     * @param deleteContents Whether to delete favorites in the folder
     */
    fun deleteFolder(folderId: String, deleteContents: Boolean = false) {
        viewModelScope.launch {
            _error.value = null

            repository.deleteFolder(folderId, deleteContents)
                .onSuccess {
                    loadFolders() // Reload folders
                }
                .onFailure { e ->
                    _error.value = "Failed to delete folder: ${e.message}"
                }
        }
    }

    /**
     * Select a folder to filter by
     *
     * @param folderId Folder ID (null = show all)
     */
    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
        loadFavorites(folderId)
    }

    /**
     * Alias for selectFolder - Filter by folder ID
     *
     * @param folderId Folder ID (null = show all)
     */
    fun filterByFolder(folderId: String?) = selectFolder(folderId)

    /**
     * Clear search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
        loadFavorites(_selectedFolderId.value)
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

// Type alias for backward compatibility
typealias BookmarkViewModel = FavoriteViewModel
