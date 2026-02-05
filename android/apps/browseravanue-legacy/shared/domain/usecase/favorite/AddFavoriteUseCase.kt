package com.augmentalis.browseravanue.domain.usecase.favorite

import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.model.Favorite
import com.augmentalis.browseravanue.domain.model.Tab
import com.augmentalis.browseravanue.domain.repository.BrowserRepository

/**
 * UseCase: Add a favorite/bookmark
 *
 * Single Responsibility:
 * - Adds URL to favorites
 * - Handles duplicate favorites
 * - Can create from Tab
 *
 * Usage:
 * ```
 * viewModelScope.launch {
 *     addFavoriteUseCase(url, title)
 *         .onSuccess { favorite -> /* Favorite added */ }
 *         .onError { error -> /* Already favorited or error */ }
 * }
 * ```
 */
class AddFavoriteUseCase(
    private val repository: BrowserRepository
) {
    /**
     * Add favorite with URL and title
     *
     * @param url URL to favorite
     * @param title Title for favorite
     * @return BrowserResult<Favorite>
     */
    suspend operator fun invoke(url: String, title: String): BrowserResult<Favorite> {
        return repository.addFavorite(url, title)
    }

    /**
     * Add favorite from current tab
     *
     * @param tab Tab to favorite
     * @return BrowserResult<Favorite>
     */
    suspend fun fromTab(tab: Tab): BrowserResult<Favorite> {
        return repository.addFavoriteFromTab(tab)
    }
}
