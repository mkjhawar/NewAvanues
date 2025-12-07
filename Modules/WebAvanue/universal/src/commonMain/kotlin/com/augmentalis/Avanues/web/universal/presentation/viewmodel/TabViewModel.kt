package com.augmentalis.Avanues.web.universal.presentation.viewmodel

import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.domain.model.Tab
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UI state wrapper for Tab with additional transient UI properties
 * These are not persisted to database - only for runtime UI state
 */
data class TabUiState(
    val tab: Tab,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
)

/**
 * TabViewModel - Manages browser tab state and operations
 *
 * Responsibilities:
 * - Load and observe all tabs
 * - Track active tab
 * - Create/close tabs
 * - Switch between tabs
 * - Update tab properties (URL, title, etc.)
 *
 * State:
 * - tabs: List<TabUiState> - All open tabs with UI state
 * - activeTab: TabUiState? - Currently active tab
 * - isLoading: Boolean - Global loading state
 * - error: String? - Error message if operation fails
 */
class TabViewModel(
    private val repository: BrowserRepository
) {
    // Coroutine scope for ViewModel - use SupervisorJob for proper error isolation
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // FIX: Mutex for thread-safe StateFlow updates
    private val stateMutex = Mutex()

    // FIX P0-C1: AtomicBoolean for thread-safe check-and-set (prevents race condition)
    private val isObservingTabs = AtomicBoolean(false)

    // State: All tabs with UI state
    private val _tabs = MutableStateFlow<List<TabUiState>>(emptyList())
    val tabs: StateFlow<List<TabUiState>> = _tabs.asStateFlow()

    // State: Active tab
    private val _activeTab = MutableStateFlow<TabUiState?>(null)
    val activeTab: StateFlow<TabUiState?> = _activeTab.asStateFlow()

    // State: Loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State: Error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // State: Settings
    private val _settings = MutableStateFlow<BrowserSettings?>(null)
    val settings: StateFlow<BrowserSettings?> = _settings.asStateFlow()

    init {
        loadTabs()
        loadSettings()
    }

    /**
     * Load settings from repository
     */
    private fun loadSettings() {
        viewModelScope.launch {
            repository.observeSettings()
                .catch { e ->
                    println("Failed to observe settings: ${e.message}")
                }
                .collect { settings ->
                    _settings.value = settings
                }
        }
    }

    /**
     * Load all tabs from repository
     *
     * Observes tab changes reactively via Flow
     * FIX: Prevents multiple collectors with isObservingTabs flag
     */
    fun loadTabs() {
        // FIX P0-C1: Atomic check-and-set prevents race condition
        if (!isObservingTabs.compareAndSet(false, true)) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.observeTabs()
                .catch { e ->
                    _error.value = "Failed to load tabs: ${e.message}"
                    _isLoading.value = false
                    isObservingTabs.set(false)
                }
                .collect { tabList ->
                    // FIX: Wrap entire collect block in try-catch to prevent crashes
                    try {
                        // Preserve UI state for existing tabs, create new state for new tabs
                        val existingStates = _tabs.value.associateBy { it.tab.id }
                        val updatedTabs = tabList.map { tab ->
                            existingStates[tab.id]?.copy(tab = tab)
                                ?: TabUiState(tab = tab)
                        }
                        _tabs.value = updatedTabs
                        _isLoading.value = false

                        // FIX: Restore last active tab on app restart
                        // Set active tab to first tab if none selected
                        if (_activeTab.value == null && updatedTabs.isNotEmpty()) {
                            // Try to find the most recently updated tab (likely the last active)
                            val lastActiveTab = updatedTabs.maxByOrNull { it.tab.lastAccessedAt }
                            _activeTab.value = lastActiveTab ?: updatedTabs.first()

                            // Log for debugging
                            println("TabViewModel: Restored active tab: ${_activeTab.value?.tab?.title} (${_activeTab.value?.tab?.url})")
                        }

                        // FIX: Create default tab on first launch
                        // If no tabs exist (first launch or after closing all tabs), create a default tab
                        // This ensures users never see "No tabs open" on startup
                        if (updatedTabs.isEmpty()) {
                            println("TabViewModel: No tabs found, creating default tab with startup URL")
                            val settings = _settings.value
                            val startupUrl = settings?.homePage ?: Tab.DEFAULT_URL
                            createTab(
                                url = startupUrl,
                                title = "Home",
                                setActive = true,
                                isDesktopMode = settings?.useDesktopMode ?: false
                            )
                        }

                        // Update active tab reference if it changed
                        _activeTab.value?.let { active ->
                            val updated = updatedTabs.find { it.tab.id == active.tab.id }
                            if (updated != null) {
                                _activeTab.value = updated
                            }
                        }
                    } catch (e: Exception) {
                        _error.value = "Failed to process tabs: ${e.message}"
                        println("TabViewModel: Error in collect: ${e.message}")
                    }
                }
        }
    }

    /**
     * Create a new tab
     *
     * @param url Initial URL (can be empty for blank tab)
     * @param title Initial title (defaults to "New Tab")
     * @param setActive Whether to set this tab as active immediately
     * @param isDesktopMode Whether to use desktop mode (FIX BUG #4: apply global setting)
     */
    fun createTab(url: String = "", title: String = "New Tab", setActive: Boolean = true, isDesktopMode: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // FIX: Use homepage from settings if URL is empty, not hardcoded Google
            // Root cause: Tabs with empty URLs were loading hardcoded DEFAULT_URL (google.com)
            // Solution: Use user's configured homepage from settings
            val homepage = _settings.value?.homePage ?: Tab.DEFAULT_URL
            val finalUrl = if (url.isBlank()) homepage else url
            val finalTitle = if (url.isBlank()) "New Tab" else title

            // FIX BUG #4: Apply desktop mode from settings when creating new tabs
            val newTab = Tab.create(url = finalUrl, title = finalTitle, isDesktopMode = isDesktopMode)
            repository.createTab(newTab)
                .onSuccess { createdTab ->
                    if (setActive) {
                        _activeTab.value = TabUiState(tab = createdTab)
                    }
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = "Failed to create tab: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Close a tab by ID
     *
     * @param tabId Tab ID to close
     */
    fun closeTab(tabId: String) {
        viewModelScope.launch {
            _error.value = null

            repository.closeTab(tabId)
                .onSuccess {
                    // If closing active tab, switch to another tab
                    if (_activeTab.value?.tab?.id == tabId) {
                        val remainingTabs = _tabs.value.filter { it.tab.id != tabId }
                        _activeTab.value = remainingTabs.firstOrNull()
                    }
                }
                .onFailure { e ->
                    _error.value = "Failed to close tab: ${e.message}"
                }
        }
    }

    /**
     * Close all tabs
     */
    fun closeAllTabs() {
        viewModelScope.launch {
            _error.value = null

            repository.closeAllTabs()
                .onSuccess {
                    _activeTab.value = null
                }
                .onFailure { e ->
                    _error.value = "Failed to close all tabs: ${e.message}"
                }
        }
    }

    /**
     * Toggle pin state of a tab
     *
     * @param tabId Tab ID to toggle pin state
     */
    fun togglePinned(tabId: String) {
        viewModelScope.launch {
            _error.value = null
            val tabState = _tabs.value.find { it.tab.id == tabId }
            if (tabState != null) {
                val newPinnedState = !tabState.tab.isPinned
                repository.setPinned(tabId, newPinnedState)
                    .onFailure { e ->
                        _error.value = "Failed to ${if (newPinnedState) "pin" else "unpin"} tab: ${e.message}"
                    }
            }
        }
    }

    /**
     * Switch to a different tab
     *
     * @param tabId Tab ID to switch to
     */
    fun switchTab(tabId: String) {
        viewModelScope.launch {
            stateMutex.withLock {
                val tabState = _tabs.value.find { it.tab.id == tabId }
                if (tabState != null) {
                    _activeTab.value = tabState
                    // FIX: Update lastAccessedAt timestamp when switching tabs
                    val updatedTab = tabState.tab.copy(lastAccessedAt = kotlinx.datetime.Clock.System.now())
                    repository.updateTab(updatedTab)
                } else {
                    _error.value = "Tab not found: $tabId"
                }
            }
        }
    }

    /**
     * Update tab properties (persisted data)
     *
     * @param tab Updated tab object
     */
    fun updateTab(tab: Tab) {
        viewModelScope.launch {
            _error.value = null

            repository.updateTab(tab)
                .onSuccess {
                    // Update active tab if it's the same tab
                    if (_activeTab.value?.tab?.id == tab.id) {
                        val currentState = _activeTab.value
                        _activeTab.value = currentState?.copy(tab = tab)
                    }
                }
                .onFailure { e ->
                    _error.value = "Failed to update tab: ${e.message}"
                }
        }
    }

    /**
     * Navigate active tab to URL
     *
     * Automatically handles:
     * - Adding https:// scheme if missing
     * - Upgrading http:// to https:// for security
     * - Search queries (if input doesn't look like a URL)
     *
     * @param url URL to navigate to
     */
    fun navigateToUrl(url: String) {
        val normalizedUrl = normalizeUrl(url)
        _activeTab.value?.let { state ->
            val updated = state.tab.copy(url = normalizedUrl)
            updateTab(updated)
        } ?: run {
            // No active tab, create new one
            createTab(normalizedUrl, title = normalizedUrl)
        }
    }

    /**
     * Normalize URL for navigation
     * - Upgrades http:// to https:// (security)
     * - Adds https:// if no scheme
     * - Converts search queries to configured search engine
     */
    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()

        return when {
            // Already has https://
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed

            // Upgrade http:// to https://
            trimmed.startsWith("http://", ignoreCase = true) -> {
                "https://" + trimmed.removePrefix("http://").removePrefix("HTTP://")
            }

            // Special schemes (file://, about:, etc.) - keep as is
            trimmed.contains("://") -> trimmed

            // Looks like a domain (contains . and no spaces)
            trimmed.contains(".") && !trimmed.contains(" ") -> {
                "https://$trimmed"
            }

            // Treat as search query - use configured search engine
            else -> {
                val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
                val searchEngine = _settings.value?.defaultSearchEngine ?: BrowserSettings.SearchEngine.GOOGLE
                "${searchEngine.baseUrl}?${searchEngine.queryParam}=$encoded"
            }
        }
    }

    /**
     * Update active tab's loading state (UI state only, not persisted)
     *
     * @param isLoading Whether the page is loading
     */
    fun setTabLoading(isLoading: Boolean) {
        viewModelScope.launch {
            stateMutex.withLock {
                _activeTab.value?.let { state ->
                    val updated = state.copy(isLoading = isLoading)
                    _activeTab.value = updated

                    // Update in tabs list
                    _tabs.value = _tabs.value.map {
                        if (it.tab.id == state.tab.id) updated else it
                    }
                }
            }
        }
    }

    /**
     * Update active tab's title
     *
     * @param title New title
     */
    fun setTabTitle(title: String) {
        _activeTab.value?.let { state ->
            val updated = state.tab.copy(title = title)
            updateTab(updated)
        }
    }

    /**
     * Update active tab's navigation state (UI state only, not persisted)
     *
     * @param canGoBack Whether back navigation is available
     * @param canGoForward Whether forward navigation is available
     */
    fun setNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        viewModelScope.launch {
            stateMutex.withLock {
                _activeTab.value?.let { state ->
                    val updated = state.copy(canGoBack = canGoBack, canGoForward = canGoForward)
                    _activeTab.value = updated

                    // Update in tabs list
                    _tabs.value = _tabs.value.map {
                        if (it.tab.id == state.tab.id) updated else it
                    }
                }
            }
        }
    }

    /**
     * Update specific tab's URL (called when WebView navigates)
     *
     * @param tabId Tab ID to update
     * @param url New URL
     */
    fun updateTabUrl(tabId: String, url: String) {
        viewModelScope.launch {
            stateMutex.withLock {
                val tabState = _tabs.value.find { it.tab.id == tabId } ?: return@withLock
                val updatedTab = tabState.tab.copy(url = url)

                // Update local state immediately for responsiveness
                val updatedState = tabState.copy(tab = updatedTab)
                _tabs.value = _tabs.value.map { if (it.tab.id == tabId) updatedState else it }

                if (_activeTab.value?.tab?.id == tabId) {
                    _activeTab.value = updatedState
                }

                // Persist to repository
                repository.updateTab(updatedTab)
            }
        }
    }

    /**
     * Update specific tab's loading state (called when WebView loading changes)
     *
     * @param tabId Tab ID to update
     * @param isLoading Whether page is loading
     */
    fun updateTabLoading(tabId: String, isLoading: Boolean) {
        viewModelScope.launch {
            stateMutex.withLock {
                val tabState = _tabs.value.find { it.tab.id == tabId } ?: return@withLock
                val updatedState = tabState.copy(isLoading = isLoading)

                _tabs.value = _tabs.value.map { if (it.tab.id == tabId) updatedState else it }

                if (_activeTab.value?.tab?.id == tabId) {
                    _activeTab.value = updatedState
                }
            }
        }
    }

    /**
     * Update specific tab's title (called when WebView title changes)
     *
     * @param tabId Tab ID to update
     * @param title New title
     */
    fun updateTabTitle(tabId: String, title: String) {
        viewModelScope.launch {
            stateMutex.withLock {
                val tabState = _tabs.value.find { it.tab.id == tabId } ?: return@withLock
                val updatedTab = tabState.tab.copy(title = title)

                // Update local state immediately
                val updatedState = tabState.copy(tab = updatedTab)
                _tabs.value = _tabs.value.map { if (it.tab.id == tabId) updatedState else it }

                if (_activeTab.value?.tab?.id == tabId) {
                    _activeTab.value = updatedState
                }

                // Persist to repository
                repository.updateTab(updatedTab)
            }
        }
    }

    /**
     * Update specific tab's navigation state (called when WebView navigation state changes)
     *
     * @param tabId Tab ID to update
     * @param canGoBack Whether can navigate back
     * @param canGoForward Whether can navigate forward
     */
    fun updateTabNavigation(
        tabId: String,
        canGoBack: Boolean? = null,
        canGoForward: Boolean? = null
    ) {
        viewModelScope.launch {
            stateMutex.withLock {
                val tabState = _tabs.value.find { it.tab.id == tabId } ?: return@withLock
                val updatedState = tabState.copy(
                    canGoBack = canGoBack ?: tabState.canGoBack,
                    canGoForward = canGoForward ?: tabState.canGoForward
                )

                _tabs.value = _tabs.value.map { if (it.tab.id == tabId) updatedState else it }

                if (_activeTab.value?.tab?.id == tabId) {
                    _activeTab.value = updatedState
                }
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    // ========== Phase 4: Scroll Controls ==========

    /**
     * Scroll up in the active tab's WebView
     *
     * Uses WebViewController to scroll up by a viewport fraction
     */
    fun scrollUp() {
        // Will be implemented with WebViewController reference
        // For now, this is a placeholder for ViewModel-level scroll state management
    }

    /**
     * Scroll down in the active tab's WebView
     *
     * Uses WebViewController to scroll down by a viewport fraction
     */
    fun scrollDown() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Scroll left in the active tab's WebView
     */
    fun scrollLeft() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Scroll right in the active tab's WebView
     */
    fun scrollRight() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Scroll to top of page in the active tab
     */
    fun scrollToTop() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Scroll to bottom of page in the active tab
     */
    fun scrollToBottom() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Update scroll position in database for the active tab
     *
     * @param x Horizontal scroll position
     * @param y Vertical scroll position
     */
    fun updateScrollPosition(x: Int, y: Int) {
        _activeTab.value?.let { state ->
            viewModelScope.launch {
                val updatedTab = state.tab.copy(scrollXPosition = x, scrollYPosition = y)
                repository.updateTab(updatedTab)
                    .onSuccess {
                        _activeTab.value = state.copy(tab = updatedTab)
                    }
                    .onFailure { e ->
                        _error.value = "Failed to save scroll position: ${e.message}"
                    }
            }
        }
    }

    // ========== Phase 4: Zoom Controls ==========

    /**
     * Zoom in on the active tab's WebView
     */
    fun zoomIn() {
        _activeTab.value?.let { state ->
            val newZoomLevel = (state.tab.zoomLevel + 1).coerceIn(1, 5)
            setZoomLevel(newZoomLevel)
        }
    }

    /**
     * Zoom out on the active tab's WebView
     */
    fun zoomOut() {
        _activeTab.value?.let { state ->
            val newZoomLevel = (state.tab.zoomLevel - 1).coerceIn(1, 5)
            setZoomLevel(newZoomLevel)
        }
    }

    /**
     * Set zoom level for the active tab
     *
     * @param level Zoom level (1-5, where 3 is 100%)
     */
    fun setZoomLevel(level: Int) {
        _activeTab.value?.let { state ->
            viewModelScope.launch {
                val validatedLevel = level.coerceIn(1, 5)
                val updatedTab = state.tab.copy(zoomLevel = validatedLevel)
                repository.updateTab(updatedTab)
                    .onSuccess {
                        _activeTab.value = state.copy(tab = updatedTab)
                        // WebViewController.setZoomLevel() will be called from UI layer
                    }
                    .onFailure { e ->
                        _error.value = "Failed to set zoom level: ${e.message}"
                    }
            }
        }
    }

    // ========== Phase 4: Desktop Mode ==========

    /**
     * Toggle desktop mode for the active tab
     */
    fun toggleDesktopMode() {
        _activeTab.value?.let { state ->
            setDesktopMode(!state.tab.isDesktopMode)
        }
    }

    /**
     * Set desktop mode for the active tab
     *
     * @param enabled Whether to enable desktop mode
     */
    fun setDesktopMode(enabled: Boolean) {
        _activeTab.value?.let { state ->
            viewModelScope.launch {
                val updatedTab = state.tab.copy(isDesktopMode = enabled)
                repository.updateTab(updatedTab)
                    .onSuccess {
                        _activeTab.value = state.copy(tab = updatedTab)
                        // WebViewController.setDesktopMode() will be called from UI layer
                    }
                    .onFailure { e ->
                        _error.value = "Failed to set desktop mode: ${e.message}"
                    }
            }
        }
    }

    // ========== Phase 4: Page Freeze ==========

    /**
     * Toggle freeze/unfreeze page scrolling
     */
    fun freezePage() {
        // Will be implemented with WebViewController reference
        // This toggles the frozen state
    }

    /**
     * Clean up resources
     *
     * Call this when ViewModel is no longer needed
     */
    fun onCleared() {
        // Cancel all coroutines
        viewModelScope.cancel()
    }
}
