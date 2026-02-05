package com.augmentalis.browseravanue.domain.usecase.tab

import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.model.Tab
import com.augmentalis.browseravanue.domain.repository.BrowserRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase: Get all browser tabs (reactive)
 *
 * Single Responsibility:
 * - Observes all tabs from repository
 * - Returns reactive Flow for UI updates
 *
 * Usage:
 * ```
 * class BrowserViewModel(
 *     private val getAllTabsUseCase: GetAllTabsUseCase
 * ) : ViewModel() {
 *     val tabs: StateFlow<List<Tab>> = getAllTabsUseCase()
 *         .map { result -> result.getOrDefault(emptyList()) }
 *         .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
 * }
 * ```
 */
class GetAllTabsUseCase(
    private val repository: BrowserRepository
) {
    /**
     * Observe all tabs ordered by most recently accessed
     *
     * @return Flow of BrowserResult<List<Tab>>
     */
    operator fun invoke(): Flow<BrowserResult<List<Tab>>> {
        return repository.observeAllTabs()
    }
}
