package com.augmentalis.Avanues.web.universal.presentation.viewmodel

import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import com.augmentalis.webavanue.domain.repository.SettingsPreset
import com.augmentalis.webavanue.platform.DownloadPathValidator
import com.augmentalis.webavanue.platform.ValidationResult
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
 * ## Overview
 * Provides reactive state management for all browser configuration settings including
 * display preferences, privacy controls, search engine selection, and advanced features.
 * Automatically persists changes to the database via [BrowserRepository].
 *
 * ## State Management
 * All state exposed through Kotlin Flows for reactive UI updates:
 * - [settings]: `StateFlow<BrowserSettings?>` - Current browser settings (null during initial load)
 * - [isLoading]: `StateFlow<Boolean>` - Loading state for async operations
 * - [error]: `StateFlow<String?>` - Error message for failed operations
 * - [saveSuccess]: `StateFlow<Boolean>` - Success indicator for save operations
 *
 * ## Threading Model
 * - **UI updates**: Main dispatcher (via StateFlow)
 * - **Repository calls**: IO dispatcher (repository handles threading)
 * - **Lifecycle**: ViewModel-scoped coroutines (cleaned up via [onCleared])
 *
 * ## Key Responsibilities
 * - **Load settings**: Reactive observation of settings changes via [observeSettings]
 * - **Update settings**: Individual setting updates or bulk settings replacement
 * - **Apply presets**: Quick application of common configurations (Privacy, Performance, Accessibility)
 * - **Validation**: Ensures setting values are within valid ranges (zoom 50-200%, window dimensions, etc.)
 * - **Error handling**: Graceful failure with user-friendly error messages
 *
 * ## Usage Example
 * ```kotlin
 * val viewModel = SettingsViewModel(repository)
 *
 * // Observe settings reactively
 * val settings by viewModel.settings.collectAsState()
 *
 * // Update individual setting
 * viewModel.setDesktopMode(enabled = true)
 *
 * // Apply preset configuration
 * viewModel.applyPreset(SettingsPreset.PRIVACY)
 *
 * // Reset to defaults
 * viewModel.resetToDefaults()
 *
 * // Clean up when done
 * viewModel.onCleared()
 * ```
 *
 * ## Desktop Mode Settings
 * Specialized controls for desktop user agent mode:
 * - [setDesktopModeDefaultZoom]: Adjust zoom level (50-200%)
 * - [setDesktopModeWindowWidth]: Set simulated screen width (800-1920px)
 * - [setDesktopModeWindowHeight]: Set simulated screen height (600-1200px)
 * - [setDesktopModeAutoFitZoom]: Toggle automatic zoom adjustment
 *
 * ## Settings Presets
 * Quick-apply common configurations via [applyPreset]:
 * - **DEFAULT**: Balanced settings for most users
 * - **PRIVACY**: Maximum privacy protection (all blockers enabled, clear on exit)
 * - **PERFORMANCE**: Optimized for speed (hardware acceleration, preloading)
 * - **ACCESSIBILITY**: Enhanced readability (force zoom, large fonts, text reflow)
 *
 * @param repository Data access layer for persisting settings
 * @see BrowserSettings for available settings and their defaults
 * @see SettingsPreset for preset configurations
 */
class SettingsViewModel(
    private val repository: BrowserRepository,
    private val pathValidator: DownloadPathValidator? = null
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

    // State: Search query for filtering settings
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // State: Expanded sections (General expanded by default for better UX)
    private val _expandedSections = MutableStateFlow(setOf("General"))
    val expandedSections: StateFlow<Set<String>> = _expandedSections.asStateFlow()

    // State: Download path validation result
    private val _pathValidation = MutableStateFlow<ValidationResult?>(null)
    val pathValidation: StateFlow<ValidationResult?> = _pathValidation.asStateFlow()

    init {
        observeSettings()
        validateSavedPathOnStartup()
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

    // ==================== Search & Section Management ====================

    /**
     * Set search query for filtering settings
     *
     * When a query is provided, automatically expands all sections to show matches.
     * When query is cleared, maintains current expansion state.
     *
     * @param query Search query text (case-insensitive matching)
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        // Auto-expand all sections when searching
        if (query.isNotBlank()) {
            expandAllSections()
        }
    }

    /**
     * Toggle expansion state of a section
     *
     * Adds section to expanded set if collapsed, removes if expanded.
     *
     * @param sectionName Name of section to toggle (e.g., "General", "Privacy & Security")
     */
    fun toggleSection(sectionName: String) {
        val current = _expandedSections.value.toMutableSet()
        if (current.contains(sectionName)) {
            current.remove(sectionName)
        } else {
            current.add(sectionName)
        }
        _expandedSections.value = current
    }

    /**
     * Expand all sections
     *
     * Useful when searching to show all matches, or for power users who want to see everything.
     */
    fun expandAllSections() {
        _expandedSections.value = setOf(
            "General",
            "Appearance",
            "Privacy & Security",
            "Downloads",
            "Performance",
            "Sync",
            "Bookmarks",
            "Voice & AI",
            "Command Bar",
            "WebXR",
            "Advanced"
        )
    }

    /**
     * Collapse all sections
     *
     * Reduces cognitive load by hiding all settings. Useful after finding what you need.
     */
    fun collapseAllSections() {
        _expandedSections.value = emptySet()
    }

    // ==================== Download Path Validation ====================

    /**
     * Validate saved download path on app startup
     *
     * Runs validation on the saved download path (if any) when the app starts.
     * If the path is no longer valid (e.g., permission revoked, path deleted),
     * automatically reverts to default download location.
     *
     * This ensures users don't encounter download failures due to stale paths.
     */
    private fun validateSavedPathOnStartup() {
        viewModelScope.launch {
            // Wait for settings to load
            settings.collect { currentSettings ->
                if (currentSettings != null) {
                    val downloadPath = currentSettings.downloadPath
                    if (downloadPath != null && pathValidator != null) {
                        val result = pathValidator.validate(downloadPath)
                        _pathValidation.value = result

                        // Auto-revert to default if path is invalid
                        if (!result.isValid) {
                            updateSettings(currentSettings.copy(downloadPath = null))
                        }
                    }
                    // Only validate once on startup
                    return@collect
                }
            }
        }
    }

    /**
     * Validate download path
     *
     * Performs comprehensive validation on the provided download path:
     * - Path exists and is accessible
     * - Path is writable
     * - Available storage space calculation
     * - Low space warning (< 100MB)
     *
     * Results are exposed via [pathValidation] StateFlow for UI to display.
     *
     * @param path Download path to validate (content:// URI on Android)
     */
    fun validateDownloadPath(path: String) {
        if (pathValidator == null) {
            _pathValidation.value = ValidationResult.failure("Path validation not available on this platform")
            return
        }

        viewModelScope.launch {
            _pathValidation.value = pathValidator.validate(path)
        }
    }

    /**
     * Clear path validation state
     *
     * Resets validation result to null. Useful for dismissing validation messages.
     */
    fun clearPathValidation() {
        _pathValidation.value = null
    }

    /**
     * Clean up resources
     */
    fun onCleared() {
        viewModelScope.cancel()
    }
}
