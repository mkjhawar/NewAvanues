package com.augmentalis.browseravanue.domain.usecase.settings

import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.model.BrowserSettings
import com.augmentalis.browseravanue.domain.repository.BrowserRepository

/**
 * UseCase: Update browser settings
 *
 * Single Responsibility:
 * - Updates browser settings
 * - Can reset to defaults
 *
 * Usage:
 * ```
 * viewModelScope.launch {
 *     val updated = settings.updateZoomLevel(ZoomLevel.LARGE)
 *     updateSettingsUseCase(updated)
 *         .onSuccess { /* Settings updated */ }
 *         .onError { error -> /* Show error */ }
 * }
 * ```
 */
class UpdateSettingsUseCase(
    private val repository: BrowserRepository
) {
    /**
     * Update browser settings
     *
     * @param settings Updated settings
     * @return BrowserResult<Unit>
     */
    suspend operator fun invoke(settings: BrowserSettings): BrowserResult<Unit> {
        return repository.updateSettings(settings)
    }

    /**
     * Reset settings to defaults
     *
     * @return BrowserResult<Unit>
     */
    suspend fun resetToDefaults(): BrowserResult<Unit> {
        return repository.resetSettings()
    }
}
