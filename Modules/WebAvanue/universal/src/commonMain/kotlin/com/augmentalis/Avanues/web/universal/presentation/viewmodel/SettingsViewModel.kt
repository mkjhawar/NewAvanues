package com.augmentalis.Avanues.web.universal.presentation.viewmodel

import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import com.augmentalis.webavanue.domain.repository.SettingsPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * SettingsViewModel - Manages browser settings state and operations
 *
 * Responsibilities:
 * - Load browser settings
 * - Update settings (desktop mode, JavaScript, cookies, etc.)
 * - Toggle individual settings
 * - Apply presets (privacy, performance)
 *
 * State:
 * - settings: BrowserSettings? - Current browser settings
 * - isLoading: Boolean - Loading state
 * - error: String? - Error message
 */
class SettingsViewModel(
    private val repository: BrowserRepository
) {
    // Coroutine scope
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State: Browser settings
    private val _settings = MutableStateFlow<BrowserSettings?>(null)
    val settings: StateFlow<BrowserSettings?> = _settings.asStateFlow()

    // State: Loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State: Error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // State: Save success indicator
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        observeSettings()
    }

    /**
     * Observe settings changes
     */
    private fun observeSettings() {
        viewModelScope.launch {
            repository.observeSettings()
                .catch { e ->
                    _error.value = "Failed to load settings: ${e.message}"
                }
                .collect { browserSettings ->
                    _settings.value = browserSettings
                }
        }
    }

    /**
     * Load browser settings from repository
     */
    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _saveSuccess.value = false

            repository.getSettings()
                .onSuccess { browserSettings ->
                    _settings.value = browserSettings
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = "Failed to load settings: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Update browser settings
     *
     * @param settings Updated settings object
     */
    fun updateSettings(settings: BrowserSettings) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _saveSuccess.value = false

            repository.updateSettings(settings)
                .onSuccess {
                    _settings.value = settings
                    _isLoading.value = false
                    _saveSuccess.value = true
                }
                .onFailure { e ->
                    _error.value = "Failed to save settings: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Toggle desktop mode
     */
    fun toggleDesktopMode() {
        _settings.value?.let { current ->
            updateSettings(current.copy(useDesktopMode = !current.useDesktopMode))
        }
    }

    /**
     * Update desktop mode
     *
     * @param enabled Whether desktop mode is enabled
     */
    fun setDesktopMode(enabled: Boolean) {
        _settings.value?.let { current ->
            updateSettings(current.copy(useDesktopMode = enabled))
        }
    }

    /**
     * Set desktop mode default zoom level
     *
     * @param zoom Zoom level percentage (50-200)
     */
    fun setDesktopModeDefaultZoom(zoom: Int) {
        _settings.value?.let { current ->
            updateSettings(current.copy(desktopModeDefaultZoom = zoom.coerceIn(50, 200)))
        }
    }

    /**
     * Set desktop mode window width
     *
     * @param width Window width in pixels
     */
    fun setDesktopModeWindowWidth(width: Int) {
        _settings.value?.let { current ->
            updateSettings(current.copy(desktopModeWindowWidth = width.coerceIn(800, 1920)))
        }
    }

    /**
     * Set desktop mode window height
     *
     * @param height Window height in pixels
     */
    fun setDesktopModeWindowHeight(height: Int) {
        _settings.value?.let { current ->
            updateSettings(current.copy(desktopModeWindowHeight = height.coerceIn(600, 1200)))
        }
    }

    /**
     * Set desktop mode auto-fit zoom
     *
     * @param enabled Whether auto-fit zoom is enabled
     */
    fun setDesktopModeAutoFitZoom(enabled: Boolean) {
        _settings.value?.let { current ->
            updateSettings(current.copy(desktopModeAutoFitZoom = enabled))
        }
    }

    /**
     * Update popup blocker
     *
     * @param enabled Whether popup blocker is enabled
     */
    fun setBlockPopups(enabled: Boolean) {
        _settings.value?.let { current ->
            updateSettings(current.copy(blockPopups = enabled))
        }
    }

    /**
     * Update JavaScript setting
     *
     * @param enabled Whether JavaScript is enabled
     */
    fun setEnableJavaScript(enabled: Boolean) {
        _settings.value?.let { current ->
            updateSettings(current.copy(enableJavaScript = enabled))
        }
    }

    /**
     * Update cookies setting
     *
     * @param enabled Whether cookies are enabled
     */
    fun setEnableCookies(enabled: Boolean) {
        _settings.value?.let { current ->
            updateSettings(current.copy(enableCookies = enabled))
        }
    }

    /**
     * Update theme
     *
     * @param theme Theme option
     */
    fun setTheme(theme: BrowserSettings.Theme) {
        _settings.value?.let { current ->
            updateSettings(current.copy(theme = theme))
        }
    }

    /**
     * Update default search engine
     *
     * @param searchEngine Search engine
     */
    fun setDefaultSearchEngine(searchEngine: BrowserSettings.SearchEngine) {
        _settings.value?.let { current ->
            updateSettings(current.copy(defaultSearchEngine = searchEngine))
        }
    }

    /**
     * Update homepage URL
     *
     * @param homepage Homepage URL
     */
    fun setHomepage(homepage: String) {
        _settings.value?.let { current ->
            updateSettings(current.copy(homePage = homepage))
        }
    }

    /**
     * Update auto-play setting
     *
     * @param autoPlay Auto-play option
     */
    fun setAutoPlay(autoPlay: BrowserSettings.AutoPlay) {
        _settings.value?.let { current ->
            updateSettings(current.copy(autoPlay = autoPlay))
        }
    }

    /**
     * Update ad blocker setting
     *
     * @param enabled Whether ad blocking is enabled
     */
    fun setBlockAds(enabled: Boolean) {
        _settings.value?.let { current ->
            updateSettings(current.copy(blockAds = enabled))
        }
    }

    /**
     * Update tracker blocker setting
     *
     * @param enabled Whether tracker blocking is enabled
     */
    fun setBlockTrackers(enabled: Boolean) {
        _settings.value?.let { current ->
            updateSettings(current.copy(blockTrackers = enabled))
        }
    }

    /**
     * Update voice commands setting
     *
     * @param enabled Whether voice commands are enabled
     */
    fun setEnableVoiceCommands(enabled: Boolean) {
        _settings.value?.let { current ->
            updateSettings(current.copy(enableVoiceCommands = enabled))
        }
    }

    /**
     * Apply a settings preset
     *
     * @param preset Preset to apply
     */
    fun applyPreset(preset: SettingsPreset) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.applyPreset(preset)
                .onSuccess {
                    _isLoading.value = false
                    loadSettings() // Reload to get new settings
                }
                .onFailure { e ->
                    _error.value = "Failed to apply preset: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Reset to default settings
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.resetSettings()
                .onSuccess {
                    _isLoading.value = false
                    loadSettings() // Reload defaults
                }
                .onFailure { e ->
                    _error.value = "Failed to reset settings: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Clear save success state
     */
    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clean up resources
     */
    fun onCleared() {
        viewModelScope.cancel()
    }
}
