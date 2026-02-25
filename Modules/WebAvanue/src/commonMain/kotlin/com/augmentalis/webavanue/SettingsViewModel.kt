package com.augmentalis.webavanue

import com.augmentalis.webavanue.BrowserSettings
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.SettingsPreset
import com.augmentalis.webavanue.SettingsValidation
import com.augmentalis.webavanue.DownloadPathValidator
import com.augmentalis.webavanue.DownloadValidationResult
import com.augmentalis.foundation.viewmodel.BaseStatefulViewModel
import com.augmentalis.foundation.state.NullableState
import com.augmentalis.foundation.state.ViewModelState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch

/**
 * SettingsViewModel - Manages browser settings state and operations
 *
 * Refactored to use StateFlow utilities for reduced boilerplate.
 *
 * State Management:
 * - settings: Current browser settings (null during initial load)
 * - isLoading/isSaving: Loading states for async operations
 * - error: Error message for failed operations
 * - saveSuccess: Success indicator for save operations
 * - searchQuery: Search filter for settings
 * - expandedSections: Set of expanded section names
 * - pathValidation: Download path validation result
 */
class SettingsViewModel(
    private val repository: BrowserRepository,
    private val pathValidator: DownloadPathValidator? = null
) : BaseStatefulViewModel() {

    // State: Browser settings
    private val _settings = NullableState<BrowserSettings>()
    val settings: StateFlow<BrowserSettings?> = _settings.flow

    // State: Saving indicator
    private val _isSaving = ViewModelState(false)
    val isSaving: StateFlow<Boolean> = _isSaving.flow

    // State: Search query
    private val _searchQuery = ViewModelState("")
    val searchQuery: StateFlow<String> = _searchQuery.flow

    // State: Expanded sections
    private val _expandedSections = ViewModelState(setOf("General"))
    val expandedSections: StateFlow<Set<String>> = _expandedSections.flow

    // State: Path validation
    private val _pathValidation = NullableState<DownloadValidationResult>()
    val pathValidation: StateFlow<DownloadValidationResult?> = _pathValidation.flow

    // Expose UiState flows
    val isLoading: StateFlow<Boolean> = uiState.isLoading.flow
    val error: StateFlow<String?> = uiState.error.flow
    val saveSuccess: StateFlow<Boolean> = uiState.saveSuccess.flow

    init {
        observeSettings()
        validateSavedPathOnStartup()
    }

    private fun observeSettings() {
        launch {
            repository.observeSettings()
                .catch { e -> uiState.error.value = "Failed to load settings: ${e.message}" }
                .collect { _settings.value = it }
        }
    }

    fun loadSettings() {
        execute {
            repository.getSettings()
                .onSuccess { _settings.value = it }
        }
    }

    fun updateSettings(settings: BrowserSettings) {
        launch {
            _isSaving.value = true
            uiState.error.clear()
            uiState.saveSuccess.value = false

            _settings.value = settings
            repository.updateSettings(settings)
                .onSuccess { uiState.saveSuccess.value = true }
                .onFailure { e -> uiState.error.value = "Failed to save settings: ${e.message}" }
            _isSaving.value = false
        }
    }

    // ==================== Settings Setters ====================
    // Simplified using inline settings update pattern

    private inline fun updateSetting(crossinline transform: (BrowserSettings) -> BrowserSettings) {
        _settings.ifPresent { updateSettings(transform(it)) }
    }

    fun toggleDesktopMode() = updateSetting { it.copy(useDesktopMode = !it.useDesktopMode) }
    fun setDesktopMode(enabled: Boolean) = updateSetting { it.copy(useDesktopMode = enabled) }
    fun setDesktopModeDefaultZoom(zoom: Int) = updateSetting { it.copy(desktopModeDefaultZoom = zoom.coerceIn(50, 200)) }
    fun setDesktopModeWindowWidth(width: Int) = updateSetting { it.copy(desktopModeWindowWidth = width.coerceIn(800, 1920)) }
    fun setDesktopModeWindowHeight(height: Int) = updateSetting { it.copy(desktopModeWindowHeight = height.coerceIn(600, 1200)) }
    fun setDesktopModeAutoFitZoom(enabled: Boolean) = updateSetting { it.copy(desktopModeAutoFitZoom = enabled) }
    fun setBlockPopups(enabled: Boolean) = updateSetting { it.copy(blockPopups = enabled) }
    fun setEnableJavaScript(enabled: Boolean) = updateSetting { it.copy(enableJavaScript = enabled) }
    fun setEnableCookies(enabled: Boolean) = updateSetting { it.copy(enableCookies = enabled) }
    fun setTheme(theme: BrowserSettings.Theme) = updateSetting { it.copy(theme = theme) }
    fun setDefaultSearchEngine(searchEngine: BrowserSettings.SearchEngine) = updateSetting { it.copy(defaultSearchEngine = searchEngine) }
    fun setHomepage(homepage: String) = updateSetting { it.copy(homePage = homepage) }
    fun setAutoPlay(autoPlay: BrowserSettings.AutoPlay) = updateSetting { it.copy(autoPlay = autoPlay) }
    fun setBlockAds(enabled: Boolean) = updateSetting { it.copy(blockAds = enabled) }
    fun setBlockTrackers(enabled: Boolean) = updateSetting { it.copy(blockTrackers = enabled) }
    fun setEnableVoiceCommands(enabled: Boolean) = updateSetting { it.copy(enableVoiceCommands = enabled) }

    // ==================== Presets & Reset ====================

    fun applyPreset(preset: SettingsPreset) {
        execute {
            repository.applyPreset(preset)
                .onSuccess { loadSettings() }
        }
    }

    fun resetToDefaults() {
        execute {
            repository.resetSettings()
                .onSuccess { loadSettings() }
        }
    }

    fun clearSaveSuccess() = uiState.clearSuccess()
    fun clearError() = uiState.clearError()

    // ==================== Search & Section Management ====================

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) expandAllSections()
    }

    fun toggleSection(sectionName: String) {
        _expandedSections.update { sections ->
            if (sections.contains(sectionName)) sections - sectionName else sections + sectionName
        }
    }

    fun expandAllSections() {
        _expandedSections.value = setOf(
            "General", "Appearance", "Privacy & Security", "Downloads",
            "Performance", "Sync", "Bookmarks", "Voice & AI", "Command Bar", "WebXR", "Advanced"
        )
    }

    fun collapseAllSections() {
        _expandedSections.value = emptySet()
    }

    // ==================== Download Path Validation ====================

    private fun validateSavedPathOnStartup() {
        launch {
            settings.collect { currentSettings ->
                if (currentSettings != null) {
                    val downloadPath = currentSettings.downloadPath
                    if (downloadPath != null && pathValidator != null) {
                        val result = pathValidator.validate(downloadPath)
                        _pathValidation.value = result

                        if (!result.isValid) {
                            updateSettings(currentSettings.copy(downloadPath = null))
                        }
                    }
                    return@collect
                }
            }
        }
    }

    fun validateDownloadPath(path: String) {
        if (pathValidator == null) {
            _pathValidation.value = DownloadValidationResult.error("Path validation not available on this platform")
            return
        }
        launch { _pathValidation.value = pathValidator.validate(path) }
    }

    fun clearPathValidation() = _pathValidation.clear()
}
