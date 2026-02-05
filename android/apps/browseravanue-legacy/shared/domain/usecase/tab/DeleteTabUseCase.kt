package com.augmentalis.browseravanue.domain.usecase.tab

import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.repository.BrowserRepository

/**
 * UseCase: Delete a browser tab
 *
 * Single Responsibility:
 * - Deletes tab by ID
 * - Handles tab not found errors
 *
 * Usage:
 * ```
 * viewModelScope.launch {
 *     deleteTabUseCase(tabId)
 *         .onSuccess { /* Tab deleted */ }
 *         .onError { error -> /* Show error */ }
 * }
 * ```
 */
class DeleteTabUseCase(
    private val repository: BrowserRepository
) {
    /**
     * Delete tab by ID
     *
     * @param tabId Tab ID to delete
     * @return BrowserResult<Unit>
     */
    suspend operator fun invoke(tabId: String): BrowserResult<Unit> {
        return repository.deleteTab(tabId)
    }

    /**
     * Delete all tabs
     *
     * @return BrowserResult<Unit>
     */
    suspend fun deleteAll(): BrowserResult<Unit> {
        return repository.deleteAllTabs()
    }
}
