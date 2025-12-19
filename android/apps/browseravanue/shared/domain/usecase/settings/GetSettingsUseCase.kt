package com.augmentalis.browseravanue.domain.usecase.settings

import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.model.BrowserSettings
import com.augmentalis.browseravanue.domain.repository.BrowserRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase: Get browser settings (reactive)
 *
 * Single Responsibility:
 * - Observes browser settings from repository
 * - Returns reactive Flow for UI updates
 * - Initializes with defaults if not exists
 *
 * Usage:
 * ```
 * class BrowserViewModel(
 *     private val getSettingsUseCase: GetSettingsUseCase
 * ) : ViewModel() {
 *     val settings: StateFlow<BrowserSettings> = getSettingsUseCase()
 *         .map { result -> result.getOrDefault(BrowserSettings.default()) }
 *         .stateIn(viewModelScope, SharingStarted.Eagerly, BrowserSettings.default())
 * }
 * ```
 */
class GetSettingsUseCase(
    private val repository: BrowserRepository
) {
    /**
     * Observe browser settings (reactive)
     *
     * @return Flow of BrowserResult<BrowserSettings>
     */
    operator fun invoke(): Flow<BrowserResult<BrowserSettings>> {
        return repository.observeSettings()
    }

    /**
     * Get browser settings (one-time query)
     *
     * @return BrowserResult<BrowserSettings>
     */
    suspend fun get(): BrowserResult<BrowserSettings> {
        return repository.getSettings()
    }
}
