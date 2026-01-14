package com.augmentalis.browseravanue.domain.usecase.navigation

import com.augmentalis.browseravanue.core.BrowserError
import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.repository.BrowserRepository

/**
 * UseCase: Navigate browser (URL, back, forward, reload)
 *
 * Single Responsibility:
 * - Handles all navigation operations
 * - Validates navigation actions
 *
 * Usage:
 * ```
 * viewModelScope.launch {
 *     navigateUseCase.toUrl(tabId, "https://google.com")
 *         .onSuccess { /* Navigation started */ }
 *         .onError { error -> /* Invalid URL or tab not found */ }
 * }
 * ```
 */
class NavigateUseCase(
    private val repository: BrowserRepository
) {
    /**
     * Navigate to URL
     *
     * @param tabId Tab ID to navigate
     * @param url URL to navigate to
     * @return BrowserResult<Unit>
     */
    suspend fun toUrl(tabId: String, url: String): BrowserResult<Unit> {
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

        return repository.navigateToUrl(tabId, normalizedUrl)
    }

    /**
     * Go back in tab history
     *
     * @param tabId Tab ID
     * @return BrowserResult<Unit>
     */
    suspend fun back(tabId: String): BrowserResult<Unit> {
        return repository.goBack(tabId)
    }

    /**
     * Go forward in tab history
     *
     * @param tabId Tab ID
     * @return BrowserResult<Unit>
     */
    suspend fun forward(tabId: String): BrowserResult<Unit> {
        return repository.goForward(tabId)
    }

    /**
     * Reload current page
     *
     * @param tabId Tab ID
     * @return BrowserResult<Unit>
     */
    suspend fun reload(tabId: String): BrowserResult<Unit> {
        return repository.reload(tabId)
    }
}
