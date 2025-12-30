package com.augmentalis.browseravanue.domain.usecase.favorite

import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.repository.BrowserRepository

/**
 * UseCase: Delete a favorite/bookmark
 *
 * Single Responsibility:
 * - Deletes favorite by ID or URL
 * - Can delete all favorites
 *
 * Usage:
 * ```
 * viewModelScope.launch {
 *     deleteFavoriteUseCase(favoriteId)
 *         .onSuccess { /* Favorite deleted */ }
 *         .onError { error -> /* Show error */ }
 * }
 * ```
 */
class DeleteFavoriteUseCase(
    private val repository: BrowserRepository
) {
    /**
     * Delete favorite by ID
     *
     * @param favoriteId Favorite ID to delete
     * @return BrowserResult<Unit>
     */
    suspend operator fun invoke(favoriteId: String): BrowserResult<Unit> {
        return repository.deleteFavorite(favoriteId)
    }

    /**
     * Delete favorite by URL (unfavorite)
     *
     * @param url URL to unfavorite
     * @return BrowserResult<Unit>
     */
    suspend fun byUrl(url: String): BrowserResult<Unit> {
        return repository.deleteFavoriteByUrl(url)
    }

    /**
     * Delete all favorites
     *
     * @return BrowserResult<Unit>
     */
    suspend fun deleteAll(): BrowserResult<Unit> {
        return repository.deleteAllFavorites()
    }
}
