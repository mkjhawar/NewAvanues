package com.augmentalis.browseravanue.domain.usecase.favorite

import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.model.Favorite
import com.augmentalis.browseravanue.domain.repository.BrowserRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase: Get all browser favorites (reactive)
 *
 * Single Responsibility:
 * - Observes all favorites from repository
 * - Returns reactive Flow for UI updates
 *
 * Usage:
 * ```
 * class BrowserViewModel(
 *     private val getAllFavoritesUseCase: GetAllFavoritesUseCase
 * ) : ViewModel() {
 *     val favorites: StateFlow<List<Favorite>> = getAllFavoritesUseCase()
 *         .map { result -> result.getOrDefault(emptyList()) }
 *         .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
 * }
 * ```
 */
class GetAllFavoritesUseCase(
    private val repository: BrowserRepository
) {
    /**
     * Observe all favorites ordered by title
     *
     * @return Flow of BrowserResult<List<Favorite>>
     */
    operator fun invoke(): Flow<BrowserResult<List<Favorite>>> {
        return repository.observeAllFavorites()
    }

    /**
     * Observe favorites in specific folder
     *
     * @param folder Folder name (null for root)
     * @return Flow of BrowserResult<List<Favorite>>
     */
    fun byFolder(folder: String?): Flow<BrowserResult<List<Favorite>>> {
        return repository.observeFavoritesByFolder(folder)
    }

    /**
     * Observe all folders
     *
     * @return Flow of BrowserResult<List<String>>
     */
    fun folders(): Flow<BrowserResult<List<String>>> {
        return repository.observeFolders()
    }
}
