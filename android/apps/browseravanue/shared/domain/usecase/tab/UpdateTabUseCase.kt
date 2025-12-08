package com.augmentalis.browseravanue.domain.usecase.tab

import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.model.Tab
import com.augmentalis.browseravanue.domain.repository.BrowserRepository

/**
 * UseCase: Update browser tab
 *
 * Single Responsibility:
 * - Updates tab information
 * - Marks tab as accessed when needed
 *
 * Usage:
 * ```
 * viewModelScope.launch {
 *     val updatedTab = tab.updatePageInfo(newUrl, newTitle)
 *     updateTabUseCase(updatedTab)
 *         .onSuccess { /* Tab updated */ }
 *         .onError { error -> /* Show error */ }
 * }
 * ```
 */
class UpdateTabUseCase(
    private val repository: BrowserRepository
) {
    /**
     * Update tab
     *
     * @param tab Updated tab
     * @return BrowserResult<Unit>
     */
    suspend operator fun invoke(tab: Tab): BrowserResult<Unit> {
        return repository.updateTab(tab)
    }

    /**
     * Mark tab as accessed (updates last accessed timestamp)
     *
     * @param tabId Tab ID
     * @return BrowserResult<Unit>
     */
    suspend fun markAccessed(tabId: String): BrowserResult<Unit> {
        return repository.markTabAccessed(tabId)
    }
}
