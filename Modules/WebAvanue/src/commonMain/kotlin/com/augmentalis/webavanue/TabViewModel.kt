package com.augmentalis.webavanue

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
import kotlin.concurrent.Volatile

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
    private val repository: BrowserRepository,
    private val privateBrowsingManager: PrivateBrowsingManager = PrivateBrowsingManager()
) {
    // Coroutine scope for ViewModel - use SupervisorJob for proper error isolation
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // FIX: Mutex for thread-safe StateFlow updates
    private val stateMutex = Mutex()

    // FIX P0-C1: Volatile Boolean for thread-safe check-and-set (prevents race condition)
    // KMP-compatible alternative to Java's AtomicBoolean
    @Volatile
    private var isObservingTabs = false

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

    // State: Find in Page
    private val _findInPageState = MutableStateFlow(FindInPageState())
    val findInPageState: StateFlow<FindInPageState> = _findInPageState.asStateFlow()

    // State: Private Browsing (exposed from PrivateBrowsingManager)
    val isPrivateModeActive: StateFlow<Boolean> = privateBrowsingManager.isPrivateModeActive
    val privateTabCount: StateFlow<Int> = privateBrowsingManager.privateTabCount

    // PERFORMANCE OPTIMIZATION Phase 2: Combined UI state for efficient recomposition
    // This StateFlow combines tabs + activeTab + error for batched UI updates
    // Use this in UI instead of collecting tabs/activeTab/error separately
    private val _combinedUiState = MutableStateFlow(CombinedTabUiState())
    val combinedUiState: StateFlow<CombinedTabUiState> = _combinedUiState.asStateFlow()

    /**
     * Combined UI state for efficient recomposition
     * Reduces recompositions by batching tab state updates
     */
    data class CombinedTabUiState(
        val tabs: List<TabUiState> = emptyList(),
        val activeTab: TabUiState? = null,
        val error: String? = null,
        val isLoading: Boolean = false
    )

    // FIX L3: Thread-safe state machine for settings updates (prevents race conditions)
    private val settingsStateMachine = SettingsStateMachine(viewModelScope)

    // Expose settings state machine state for UI feedback
    val settingsState = settingsStateMachine.state

    init {
        // FIX P0: Load settings eagerly (blocking) before loading tabs
        // This ensures settings are available when creating the default tab
        // Prevents race condition where tab is created with null settings
        viewModelScope.launch {
            try {
                // Eager load settings first (blocking for initial value)
                val initialSettings = repository.getSettings().getOrNull()
                _settings.value = initialSettings
                Logger.info("TabViewModel", "Settings loaded eagerly: desktop=${initialSettings?.useDesktopMode}, " +
                    "mobilePortrait=${initialSettings?.mobilePortraitScale}, " +
                    "mobileLandscape=${initialSettings?.mobileLandscapeScale}")

                // Now load tabs (settings guaranteed to be available)
                loadTabs()
            } catch (e: Exception) {
                Logger.error("TabViewModel", "Failed to load initial settings: ${e.message}", e)
                // Fall back to default settings
                _settings.value = BrowserSettings()
                loadTabs()
            }
        }

        // Continue observing settings for updates (async)
        loadSettings()
        observeCombinedState()
    }

    /**
     * PERFORMANCE OPTIMIZATION Phase 2: Observe and combine state changes
     * Updates combinedUiState whenever tabs, activeTab, error, or isLoading changes
     */
    private fun observeCombinedState() {
        viewModelScope.launch {
            // Combine multiple state flows into one
            kotlinx.coroutines.flow.combine(
                _tabs,
                _activeTab,
                _error,
                _isLoading
            ) { tabs, activeTab, error, isLoading ->
                CombinedTabUiState(
                    tabs = tabs,
                    activeTab = activeTab,
                    error = error,
                    isLoading = isLoading
                )
            }.collect { combined ->
                _combinedUiState.value = combined
            }
        }
    }

    /**
     * Load settings from repository
     */
    private fun loadSettings() {
        viewModelScope.launch {
            repository.observeSettings()
                .catch { e ->
                    Logger.error("TabViewModel", "Failed to observe settings: ${e.message}", e)
                }
                .collect { settings ->
                    _settings.value = settings
                }
        }
    }

    /**
     * Request settings update through state machine.
     *
     * FIX L3: Use state machine to prevent race conditions during settings updates.
     * This ensures atomic transitions and queues rapid changes.
     * FIX C5: Wrap in mutex to prevent concurrent settings updates.
     *
     * @param newSettings Settings to apply
     * @param applyFunction Suspend function that applies settings (e.g., to WebView)
     */
    suspend fun requestSettingsUpdate(
        newSettings: BrowserSettings,
        applyFunction: suspend (BrowserSettings) -> Result<Unit>
    ) {
        stateMutex.withLock {
            settingsStateMachine.requestUpdate(newSettings, applyFunction)
        }
    }

    /**
     * Retry failed settings update with exponential backoff.
     *
     * FIX C5: Wrap in mutex to prevent concurrent settings updates.
     *
     * @param applyFunction Function to apply settings
     * @param maxRetries Maximum number of retries (default 3)
     * @return true if retry started, false if max retries reached
     */
    suspend fun retrySettingsUpdate(
        applyFunction: suspend (BrowserSettings) -> Result<Unit>,
        maxRetries: Int = 3
    ): Boolean {
        return stateMutex.withLock {
            settingsStateMachine.retryError(applyFunction, maxRetries)
        }
    }

    /**
     * Reset settings state machine (clear any queued updates).
     *
     * FIX C5: Wrap in mutex to prevent concurrent settings updates.
     *
     * Call when navigating away from settings or closing tab.
     */
    suspend fun resetSettingsStateMachine() {
        stateMutex.withLock {
            settingsStateMachine.reset()
        }
    }

    /**
     * Load all tabs from repository
     *
     * Observes tab changes reactively via Flow
     * FIX: Prevents multiple collectors using Mutex for atomic check-and-set
     */
    fun loadTabs() {
        viewModelScope.launch {
            // FIX: Use Mutex for atomic check-and-set to prevent race condition
            // This ensures only one observer can be started even with concurrent calls
            val shouldStart = stateMutex.withLock {
                if (isObservingTabs) {
                    false
                } else {
                    isObservingTabs = true
                    true
                }
            }
            if (!shouldStart) return@launch

            _isLoading.value = true
            _error.value = null

            repository.observeTabs()
                .catch { e ->
                    _error.value = "Failed to load tabs: ${e.message}"
                    _isLoading.value = false
                    stateMutex.withLock { isObservingTabs = false }
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
                            Logger.info("TabViewModel", "Restored active tab: ${_activeTab.value?.tab?.title} (${Logger.sanitizeUrl(_activeTab.value?.tab?.url ?: "")})")
                        }

                        // FIX: Create default tab on first launch
                        // If no tabs exist (first launch or after closing all tabs), create a default tab
                        // This ensures users never see "No tabs open" on startup
                        if (updatedTabs.isEmpty()) {
                            Logger.info("TabViewModel", "No tabs found, creating default tab with startup URL")
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
                        Logger.error("TabViewModel", "Error in collect: ${e.message}", e)
                    }
                }
        }
    }

    /**
     * Create a new tab with comprehensive error handling and URL validation.
     *
     * Features:
     * - URL validation with specific error types
     * - Tab capacity check
     * - Retry mechanism for database operations
     * - User-friendly error messages
     *
     * @param url Initial URL (can be empty for blank tab)
     * @param title Initial title (defaults to "New Tab")
     * @param setActive Whether to set this tab as active immediately
     * @param isDesktopMode Whether to use desktop mode (FIX BUG #4: apply global setting)
     */
    fun createTab(url: String = "", title: String = "New Tab", setActive: Boolean = true, isDesktopMode: Boolean = false, isIncognito: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Step 1: Validate tab capacity (max 100 tabs)
                val currentTabCount = _tabs.value.size
                if (currentTabCount >= MAX_TABS) {
                    val error = TabError.DatabaseFull(
                        maxTabs = MAX_TABS,
                        currentTabs = currentTabCount
                    )
                    _error.value = error.userMessage
                    _isLoading.value = false
                    Logger.error("TabViewModel", error.technicalDetails)
                    return@launch
                }

                // Step 2: Determine final URL based on user settings
                val finalUrl = if (url.isBlank()) {
                    when (_settings.value?.newTabPage) {
                        BrowserSettings.NewTabPage.BLANK -> "about:blank"
                        BrowserSettings.NewTabPage.HOME_PAGE -> _settings.value?.homePage ?: Tab.DEFAULT_URL
                        BrowserSettings.NewTabPage.TOP_SITES -> NewTabUrls.TOP_SITES
                        BrowserSettings.NewTabPage.MOST_VISITED -> NewTabUrls.MOST_VISITED
                        BrowserSettings.NewTabPage.SPEED_DIAL -> NewTabUrls.SPEED_DIAL
                        BrowserSettings.NewTabPage.NEWS_FEED -> NewTabUrls.NEWS_FEED
                        null -> _settings.value?.homePage ?: Tab.DEFAULT_URL
                    }
                } else {
                    url
                }

                // Step 3: Validate URL format
                val validationResult = UrlValidation.validate(finalUrl, allowBlank = true)
                if (validationResult is UrlValidation.UrlValidationResult.Invalid) {
                    _error.value = validationResult.error.userMessage
                    _isLoading.value = false
                    Logger.error("TabViewModel", validationResult.error.technicalDetails)
                    return@launch
                }

                val normalizedUrl = (validationResult as UrlValidation.UrlValidationResult.Valid).normalizedUrl
                val finalTitle = if (url.isBlank()) "New Tab" else title

                // Step 4: Create tab with retry logic for database operations
                val newTab = Tab.create(url = normalizedUrl, title = finalTitle, isDesktopMode = isDesktopMode, isIncognito = isIncognito)

                val retryPolicy = RetryPolicy.STANDARD
                retryPolicy.execute { attempt ->
                    if (attempt > 1) {
                        Logger.info("TabViewModel", "Retrying tab creation (attempt $attempt)")
                    }
                    repository.createTab(newTab)
                }.onSuccess { createdTab ->
                    // Register private tab with manager if incognito
                    if (createdTab.isIncognito) {
                        privateBrowsingManager.registerPrivateTab(createdTab)
                    }

                    if (setActive) {
                        _activeTab.value = TabUiState(tab = createdTab)
                    }
                    _isLoading.value = false
                    Logger.info("TabViewModel", "Tab created successfully: ${createdTab.id} (incognito: ${createdTab.isIncognito})")
                }.onFailure { e ->
                    val error = TabError.DatabaseOperationFailed(
                        operation = "createTab",
                        cause = e
                    )
                    _error.value = error.userMessage
                    _isLoading.value = false
                    Logger.error("TabViewModel", error.technicalDetails, e)
                }
            } catch (e: Exception) {
                // Catch any unexpected errors
                val error = TabError.WebViewCreationFailed(cause = e)
                _error.value = error.userMessage
                _isLoading.value = false
                Logger.error("TabViewModel", "Unexpected error in createTab: ${error.technicalDetails}", e)
            }
        }
    }

    companion object {
        /**
         * Maximum number of tabs allowed (prevents database overflow)
         */
        private const val MAX_TABS = 100
    }

    /**
     * Close a tab by ID
     *
     * @param tabId Tab ID to close
     */
    fun closeTab(tabId: String) {
        viewModelScope.launch {
            _error.value = null

            // Unregister from private browsing manager if it's a private tab
            if (privateBrowsingManager.isPrivateTab(tabId)) {
                privateBrowsingManager.unregisterPrivateTab(tabId)
            }

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
     * Switch to a different tab with error handling
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
                        .onFailure { e ->
                            // Non-critical error - tab switch succeeded but timestamp update failed
                            Logger.warn("TabViewModel", "Failed to update tab timestamp: ${e.message}", e)
                        }
                } else {
                    // Tab not found - provide specific error
                    val error = TabError.TabNotFound(tabId = tabId)
                    _error.value = error.userMessage
                    Logger.error("TabViewModel", error.technicalDetails)
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
     * Navigate active tab to URL with validation
     *
     * Features:
     * - URL validation with specific error types
     * - Automatic URL normalization
     * - Search query handling
     * - Creates new tab if none active
     *
     * @param url URL to navigate to
     */
    fun navigateToUrl(url: String) {
        // Validate URL first
        val validationResult = UrlValidation.validate(url, allowBlank = false)
        if (validationResult is UrlValidation.UrlValidationResult.Invalid) {
            _error.value = validationResult.error.userMessage
            Logger.error("TabViewModel", validationResult.error.technicalDetails)
            return
        }

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
                val encoded = encodeUrl(trimmed)
                val settings = _settings.value
                val searchEngine = settings?.defaultSearchEngine ?: BrowserSettings.SearchEngine.GOOGLE

                if (searchEngine == BrowserSettings.SearchEngine.CUSTOM) {
                    val customUrl = settings?.customSearchEngineUrl ?: ""
                    if (customUrl.isNotBlank() && customUrl.contains("%s")) {
                        customUrl.replace("%s", encoded)
                    } else {
                        "${BrowserSettings.SearchEngine.GOOGLE.baseUrl}?${BrowserSettings.SearchEngine.GOOGLE.queryParam}=$encoded"
                    }
                } else {
                    "${searchEngine.baseUrl}?${searchEngine.queryParam}=$encoded"
                }
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

    // ========== Reading Mode Controls ==========

    /**
     * Toggle reading mode for the active tab
     *
     * When enabled:
     * - Extracts article content using JavaScript injection
     * - Shows ReadingModeView overlay with cleaned content
     * - Hides web content and browser UI
     *
     * When disabled:
     * - Returns to normal browsing view
     * - Preserves scroll position and state
     */
    fun toggleReadingMode() {
        viewModelScope.launch {
            stateMutex.withLock {
                _activeTab.value?.let { state ->
                    val newReadingMode = !state.isReadingMode
                    val updatedState = state.copy(isReadingMode = newReadingMode)
                    _activeTab.value = updatedState

                    // Update in tabs list
                    _tabs.value = _tabs.value.map {
                        if (it.tab.id == state.tab.id) updatedState else it
                    }

                    Logger.info("TabViewModel", "Reading mode ${if (newReadingMode) "enabled" else "disabled"} for tab ${state.tab.id}")
                }
            }
        }
    }

    /**
     * Set reading mode article content for the active tab
     *
     * Called after JavaScript extraction completes
     *
     * @param article Extracted article content
     */
    fun setReadingModeArticle(article: ReadingModeArticle?) {
        viewModelScope.launch {
            stateMutex.withLock {
                _activeTab.value?.let { state ->
                    val updatedState = state.copy(
                        readingModeArticle = article,
                        isArticleAvailable = article != null
                    )
                    _activeTab.value = updatedState

                    // Update in tabs list
                    _tabs.value = _tabs.value.map {
                        if (it.tab.id == state.tab.id) updatedState else it
                    }
                }
            }
        }
    }

    /**
     * Set article availability indicator for the active tab
     *
     * Called after article detection script runs
     *
     * @param isAvailable Whether an article was detected on the page
     */
    fun setArticleAvailable(isAvailable: Boolean) {
        viewModelScope.launch {
            stateMutex.withLock {
                _activeTab.value?.let { state ->
                    val updatedState = state.copy(isArticleAvailable = isAvailable)
                    _activeTab.value = updatedState

                    // Update in tabs list
                    _tabs.value = _tabs.value.map {
                        if (it.tab.id == state.tab.id) updatedState else it
                    }
                }
            }
        }
    }

    // ========== Find in Page ==========

    /**
     * Show find in page bar with optional initial query
     *
     * @param initialQuery Optional initial search query
     */
    fun showFindInPage(initialQuery: String = "") {
        _findInPageState.value = _findInPageState.value.copy(
            isVisible = true,
            query = initialQuery
        )
    }

    /**
     * Hide find in page bar and clear search
     */
    fun hideFindInPage() {
        _findInPageState.value = FindInPageState(isVisible = false)
    }

    /**
     * Update find in page query
     * This will trigger a new search via WebViewController
     *
     * @param query Search query string
     */
    fun updateFindQuery(query: String) {
        _findInPageState.value = _findInPageState.value.copy(
            query = query,
            currentMatch = 0,
            totalMatches = 0
        )
    }

    /**
     * Update find in page results from WebView
     * Called by WebViewController after search completes
     *
     * @param currentMatch Current match index (0-based)
     * @param totalMatches Total number of matches
     */
    fun updateFindResults(currentMatch: Int, totalMatches: Int) {
        _findInPageState.value = _findInPageState.value.copy(
            currentMatch = currentMatch,
            totalMatches = totalMatches
        )
    }

    /**
     * Toggle case sensitivity for find in page
     */
    fun toggleFindCaseSensitive() {
        _findInPageState.value = _findInPageState.value.copy(
            caseSensitive = !_findInPageState.value.caseSensitive
        )
    }

    /**
     * Navigate to next match
     * Wraps around from last to first
     */
    fun findNext() {
        val state = _findInPageState.value
        if (state.totalMatches > 0) {
            val nextMatch = (state.currentMatch + 1) % state.totalMatches
            _findInPageState.value = state.copy(currentMatch = nextMatch)
        }
    }

    /**
     * Navigate to previous match
     * Wraps around from first to last
     */
    fun findPrevious() {
        val state = _findInPageState.value
        if (state.totalMatches > 0) {
            val prevMatch = if (state.currentMatch == 0) {
                state.totalMatches - 1
            } else {
                state.currentMatch - 1
            }
            _findInPageState.value = state.copy(currentMatch = prevMatch)
        }
    }

    /**
     * Clear find in page highlights and reset state
     */
    fun clearFind() {
        _findInPageState.value = _findInPageState.value.copy(
            query = "",
            currentMatch = 0,
            totalMatches = 0
        )
    }

    // ========== Screenshot Capture ==========

    /**
     * Capture a screenshot of the active tab
     *
     * This method provides a ViewModel-level interface for screenshot capture.
     * The actual capture logic is handled by the UI layer with platform-specific WebView access.
     *
     * @param type Screenshot type (VISIBLE_AREA or FULL_PAGE)
     * @param quality JPEG quality (0-100, default 80)
     * @param saveToGallery Whether to save to gallery (default true)
     * @param onProgress Callback for progress updates (progress: Float, message: String)
     * @param onComplete Callback when screenshot is complete (data, filepath)
     * @param onError Callback when screenshot fails (error message)
     */
    fun captureScreenshot(
        type: ScreenshotType,
        quality: Int = 80,
        saveToGallery: Boolean = true,
        onProgress: (Float, String) -> Unit = { _, _ -> },
        onComplete: (ScreenshotData, String?) -> Unit,
        onError: (String) -> Unit
    ) {
        _activeTab.value?.let { state ->
            Logger.info(
                "TabViewModel",
                "Screenshot capture requested for tab ${state.tab.id}: $type, quality=$quality, save=$saveToGallery"
            )
            // UI layer will handle actual capture with WebView reference
            // This method exists to coordinate state updates and provide callback structure
        } ?: run {
            Logger.error("TabViewModel", "Cannot capture screenshot: no active tab")
            onError("No active tab to capture")
        }
    }

    // ========== Session Management ==========

    /**
     * Save current browsing session.
     * Called automatically on app pause/background.
     *
     * Saves all open tabs (except private tabs) to database for restoration later.
     */
    fun saveSession() {
        viewModelScope.launch {
            try {
                val tabs = _tabs.value.map { it.tab }
                val activeTabId = _activeTab.value?.tab?.id

                // Filter out private tabs
                val tabsToSave = tabs.filter { !it.isIncognito }

                if (tabsToSave.isEmpty()) {
                    Logger.info("TabViewModel", "No tabs to save (all private)")
                    return@launch
                }

                // Save through repository
                val session = Session.create(
                    activeTabId = activeTabId,
                    tabCount = tabsToSave.size,
                    isCrashRecovery = false
                )

                val sessionTabs = tabsToSave.map { tab ->
                    SessionTab.fromTab(
                        sessionId = session.id,
                        tab = tab,
                        isActive = tab.id == activeTabId
                    )
                }

                repository.saveSession(session, sessionTabs)
                    .onSuccess {
                        Logger.info("TabViewModel", "Session saved: ${session.id} with ${sessionTabs.size} tabs")
                    }
                    .onFailure { e ->
                        Logger.error("TabViewModel", "Failed to save session: ${e.message}", e)
                    }
            } catch (e: Exception) {
                Logger.error("TabViewModel", "Error saving session: ${e.message}", e)
            }
        }
    }

    /**
     * Restore the most recent browsing session.
     *
     * Loads tabs from the last saved session.
     * Only the active tab is fully loaded initially (lazy loading).
     *
     * @return true if session was restored, false if no session available
     */
    suspend fun restoreSession(): Boolean {
        return try {
            // Check settings
            val settings = _settings.value
            if (settings?.restoreTabsOnStartup == false) {
                Logger.info("TabViewModel", "Session restore disabled in settings")
                return false
            }

            // Get latest session
            val sessionResult = repository.getLatestSession()
            if (sessionResult.isFailure || sessionResult.getOrNull() == null) {
                Logger.info("TabViewModel", "No session to restore")
                return false
            }

            val session = sessionResult.getOrNull() ?: return false

            // Skip crash recovery sessions
            if (session.isCrashRecovery) {
                Logger.info("TabViewModel", "Skipping crash recovery session (use restoreCrashRecoverySession)")
                return false
            }

            // Get session tabs
            val sessionTabsResult = repository.getSessionTabs(session.id)
            if (sessionTabsResult.isFailure || sessionTabsResult.getOrNull()?.isEmpty() != false) {
                Logger.info("TabViewModel", "Session has no tabs to restore")
                return false
            }

            val sessionTabs = sessionTabsResult.getOrNull() ?: return false

            // Close all current tabs
            repository.closeAllTabs()

            // Restore tabs from session
            val restoredTabs = sessionTabs.map { sessionTab ->
                sessionTab.toTab()
            }

            // Create tabs in repository
            restoredTabs.forEach { tab ->
                repository.createTab(tab)
            }

            // Set active tab
            val activeTab = restoredTabs.find { it.isActive } ?: restoredTabs.firstOrNull()
            if (activeTab != null) {
                repository.setActiveTab(activeTab.id)
            }

            Logger.info("TabViewModel", "Session restored: ${session.id} with ${restoredTabs.size} tabs")
            true
        } catch (e: Exception) {
            Logger.error("TabViewModel", "Failed to restore session: ${e.message}", e)
            false
        }
    }

    /**
     * Check if there's a crash recovery session available.
     *
     * @return true if crash recovery session exists
     */
    suspend fun hasCrashRecoverySession(): Boolean {
        return try {
            val result = repository.getLatestCrashSession()
            result.isSuccess && result.getOrNull() != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Restore crash recovery session.
     *
     * @return true if restored successfully
     */
    suspend fun restoreCrashRecoverySession(): Boolean {
        return try {
            val sessionResult = repository.getLatestCrashSession()
            if (sessionResult.isFailure || sessionResult.getOrNull() == null) {
                return false
            }

            val session = sessionResult.getOrNull() ?: return false

            // Get session tabs
            val sessionTabsResult = repository.getSessionTabs(session.id)
            if (sessionTabsResult.isFailure || sessionTabsResult.getOrNull()?.isEmpty() != false) {
                return false
            }

            val sessionTabs = sessionTabsResult.getOrNull() ?: return false

            // Close all current tabs
            repository.closeAllTabs()

            // Restore tabs
            val restoredTabs = sessionTabs.map { it.toTab() }
            restoredTabs.forEach { tab ->
                repository.createTab(tab)
            }

            // Set active tab
            val activeTab = restoredTabs.find { it.isActive } ?: restoredTabs.firstOrNull()
            if (activeTab != null) {
                repository.setActiveTab(activeTab.id)
            }

            Logger.info("TabViewModel", "Crash recovery session restored: ${session.id}")
            true
        } catch (e: Exception) {
            Logger.error("TabViewModel", "Failed to restore crash recovery session: ${e.message}", e)
            false
        }
    }

    /**
     * Dismiss crash recovery session (user chose not to restore).
     */
    suspend fun dismissCrashRecovery() {
        try {
            val crashSessionResult = repository.getLatestCrashSession()
            if (crashSessionResult.isSuccess) {
                val crashSession = crashSessionResult.getOrNull()
                if (crashSession != null) {
                    repository.deleteSession(crashSession.id)
                    Logger.info("TabViewModel", "Crash recovery session dismissed")
                }
            }
        } catch (e: Exception) {
            Logger.error("TabViewModel", "Failed to dismiss crash recovery: ${e.message}", e)
        }
    }

    /**
     * Handle app pause - save current session.
     * Call from Activity.onPause() or similar.
     */
    fun onAppPause() {
        saveSession()
    }

    /**
     * Handle app resume - check for crash recovery.
     * Call from Activity.onResume() or similar.
     *
     * @return true if crash recovery session is available
     */
    suspend fun onAppResume(): Boolean {
        return hasCrashRecoverySession()
    }

    /**
     * Clean up resources
     *
     * Call this when ViewModel is no longer needed
     */
    fun onCleared() {
        // Save session one last time
        saveSession()

        // Cancel all coroutines
        viewModelScope.cancel()
    }
}
