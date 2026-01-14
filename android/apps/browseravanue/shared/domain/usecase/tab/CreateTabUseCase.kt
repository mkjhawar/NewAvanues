package com.augmentalis.browseravanue.domain.usecase.tab

import com.augmentalis.browseravanue.core.BrowserError
import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.model.Tab
import com.augmentalis.browseravanue.domain.repository.BrowserRepository

/**
 * UseCase: Create a new browser tab
 *
 * Single Responsibility:
 * - Validates URL
 * - Creates tab via repository
 * - Returns created tab or error
 *
 * Usage:
 * ```
 * viewModelScope.launch {
 *     createTabUseCase("https://google.com")
 *         .onSuccess { tab -> /* Navigate to tab */ }
 *         .onError { error -> /* Show error */ }
 * }
 * ```
 */
class CreateTabUseCase(
    private val repository: BrowserRepository
) {
    /**
     * Create new tab with URL
     *
     * @param url URL to open (will be validated)
     * @return BrowserResult<Tab>
     */
    suspend operator fun invoke(url: String): BrowserResult<Tab> {
        // Validate URL
        if (url.isBlank()) {
            return BrowserResult.Error(
                BrowserError.InvalidUrl(url, "URL cannot be blank")
            )
        }

        // Ensure URL has protocol
        val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        // Create tab
        return repository.createTab(normalizedUrl)
    }

    /**
     * Create new tab with search query
     *
     * @param query Search query
     * @return BrowserResult<Tab>
     */
    suspend fun withSearch(query: String): BrowserResult<Tab> {
        if (query.isBlank()) {
            return BrowserResult.Error(
                BrowserError.InvalidUrl(query, "Search query cannot be blank")
            )
        }

        return repository.createTabWithSearch(query)
    }
}
