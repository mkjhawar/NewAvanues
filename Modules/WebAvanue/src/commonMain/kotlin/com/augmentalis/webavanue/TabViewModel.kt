package com.augmentalis.webavanue

import com.augmentalis.foundation.viewmodel.BaseViewModel
import com.augmentalis.foundation.state.ListState
import com.augmentalis.foundation.state.NullableState
import com.augmentalis.foundation.state.ViewModelState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * TabViewModel - Manages browser tab state and operations
 *
 * Refactored to use StateFlow utilities for reduced boilerplate.
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
) : BaseViewModel() {

    // Thread safety
    private val stateMutex = Mutex()

    @kotlin.concurrent.Volatile
    private var isObservingTabs = false

    // State: Tabs
    private val _tabs = ListState<TabUiState>()
    val tabs: StateFlow<List<TabUiState>> = _tabs.flow

    // State: Active tab
    private val _activeTab = NullableState<TabUiState>()
    val activeTab: StateFlow<TabUiState?> = _activeTab.flow

    // State: Loading/Error
    private val _isLoading = ViewModelState(false)
    val isLoading: StateFlow<Boolean> = _isLoading.flow

    private val _error = NullableState<String>()
    val error: StateFlow<String?> = _error.flow

    // State: Settings
    private val _settings = NullableState<BrowserSettings>()
    val settings: StateFlow<BrowserSettings?> = _settings.flow

    // State: Find in Page
    private val _findInPageState = ViewModelState(FindInPageState())
    val findInPageState: StateFlow<FindInPageState> = _findInPageState.flow

    // Private Browsing (exposed from manager)
    val isPrivateModeActive: StateFlow<Boolean> = privateBrowsingManager.isPrivateModeActive
    val privateTabCount: StateFlow<Int> = privateBrowsingManager.privateTabCount

    // Combined UI state for efficient recomposition
    private val _combinedUiState = ViewModelState(CombinedTabUiState())
    val combinedUiState: StateFlow<CombinedTabUiState> = _combinedUiState.flow

    data class CombinedTabUiState(
        val tabs: List<TabUiState> = emptyList(),
        val activeTab: TabUiState? = null,
        val error: String? = null,
        val isLoading: Boolean = false
    )

    // Settings state machine
    private val settingsStateMachine = SettingsStateMachine(viewModelScope)
    val settingsState = settingsStateMachine.state

    companion object {
        private const val MAX_TABS = 100
    }

    init {
        launch {
            try {
                val initialSettings = repository.getSettings().getOrNull()
                _settings.value = initialSettings
                Logger.info("TabViewModel", "Settings loaded eagerly: desktop=${initialSettings?.useDesktopMode}")
                loadTabs()
            } catch (e: Exception) {
                Logger.error("TabViewModel", "Failed to load initial settings: ${e.message}", e)
                _settings.value = BrowserSettings()
                loadTabs()
            }
        }
        loadSettings()
        observeCombinedState()
    }

    private fun observeCombinedState() {
        launch {
            combine(_tabs.flow, _activeTab.flow, _error.flow, _isLoading.flow) { tabs, activeTab, error, isLoading ->
                CombinedTabUiState(tabs = tabs, activeTab = activeTab, error = error, isLoading = isLoading)
            }.collect { _combinedUiState.value = it }
        }
    }

    private fun loadSettings() {
        launch {
            repository.observeSettings()
                .catch { e -> Logger.error("TabViewModel", "Failed to observe settings: ${e.message}", e) }
                .collect { _settings.value = it }
        }
    }

    // ========== Settings State Machine ==========

    suspend fun requestSettingsUpdate(newSettings: BrowserSettings, applyFunction: suspend (BrowserSettings) -> Result<Unit>) {
        stateMutex.withLock { settingsStateMachine.requestUpdate(newSettings, applyFunction) }
    }

    suspend fun retrySettingsUpdate(applyFunction: suspend (BrowserSettings) -> Result<Unit>, maxRetries: Int = 3): Boolean {
        return stateMutex.withLock { settingsStateMachine.retryError(applyFunction, maxRetries) }
    }

    suspend fun resetSettingsStateMachine() {
        stateMutex.withLock { settingsStateMachine.reset() }
    }

    // ========== Tab Loading ==========

    fun loadTabs() {
        launch {
            val shouldStart = stateMutex.withLock {
                if (isObservingTabs) false else { isObservingTabs = true; true }
            }
            if (!shouldStart) return@launch

            _isLoading.value = true
            _error.clear()

            repository.observeTabs()
                .catch { e ->
                    _error.value = "Failed to load tabs: ${e.message}"
                    _isLoading.value = false
                    stateMutex.withLock { isObservingTabs = false }
                }
                .collect { tabList ->
                    try {
                        val existingStates = _tabs.value.associateBy { it.tab.id }
                        val updatedTabs = tabList.map { tab -> existingStates[tab.id]?.copy(tab = tab) ?: TabUiState(tab = tab) }
                        _tabs.replaceAll(updatedTabs)
                        _isLoading.value = false

                        // Restore last active tab
                        if (_activeTab.value == null && updatedTabs.isNotEmpty()) {
                            _activeTab.value = updatedTabs.maxByOrNull { it.tab.lastAccessedAt } ?: updatedTabs.first()
                            Logger.info("TabViewModel", "Restored active tab: ${_activeTab.value?.tab?.title}")
                        }

                        // Create default tab on first launch
                        if (updatedTabs.isEmpty()) {
                            Logger.info("TabViewModel", "No tabs found, creating default tab")
                            val settings = _settings.value
                            createTab(
                                url = settings?.homePage ?: Tab.DEFAULT_URL,
                                title = "Home",
                                setActive = true,
                                isDesktopMode = settings?.useDesktopMode ?: false
                            )
                        }

                        // Update active tab reference
                        _activeTab.ifPresent { active ->
                            updatedTabs.find { it.tab.id == active.tab.id }?.let { _activeTab.value = it }
                        }
                    } catch (e: Exception) {
                        _error.value = "Failed to process tabs: ${e.message}"
                        Logger.error("TabViewModel", "Error in collect: ${e.message}", e)
                    }
                }
        }
    }

    // ========== Tab Operations ==========

    fun createTab(url: String = "", title: String = "New Tab", setActive: Boolean = true, isDesktopMode: Boolean = false, isIncognito: Boolean = false) {
        launch {
            _isLoading.value = true
            _error.clear()

            try {
                if (_tabs.size >= MAX_TABS) {
                    val error = TabError.DatabaseFull(MAX_TABS, _tabs.size)
                    _error.value = error.userMessage
                    _isLoading.value = false
                    return@launch
                }

                val finalUrl = determineNewTabUrl(url)
                val validationResult = UrlValidation.validate(finalUrl, allowBlank = true)
                if (validationResult is UrlValidation.UrlValidationResult.Invalid) {
                    _error.value = validationResult.error.userMessage
                    _isLoading.value = false
                    return@launch
                }

                val normalizedUrl = (validationResult as UrlValidation.UrlValidationResult.Valid).normalizedUrl
                val newTab = Tab.create(url = normalizedUrl, title = if (url.isBlank()) "New Tab" else title, isDesktopMode = isDesktopMode, isIncognito = isIncognito)

                RetryPolicy.STANDARD.execute { repository.createTab(newTab) }
                    .onSuccess { createdTab ->
                        if (createdTab.isIncognito) privateBrowsingManager.registerPrivateTab(createdTab)
                        if (setActive) _activeTab.value = TabUiState(tab = createdTab)
                        _isLoading.value = false
                        Logger.info("TabViewModel", "Tab created: ${createdTab.id}")
                    }
                    .onFailure { e ->
                        _error.value = TabError.DatabaseOperationFailed("createTab", e).userMessage
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = TabError.WebViewCreationFailed(cause = e).userMessage
                _isLoading.value = false
            }
        }
    }

    private fun determineNewTabUrl(url: String): String {
        if (url.isNotBlank()) return url
        return when (_settings.value?.newTabPage) {
            BrowserSettings.NewTabPage.BLANK -> "about:blank"
            BrowserSettings.NewTabPage.HOME_PAGE -> _settings.value?.homePage ?: Tab.DEFAULT_URL
            BrowserSettings.NewTabPage.TOP_SITES -> NewTabUrls.TOP_SITES
            BrowserSettings.NewTabPage.MOST_VISITED -> NewTabUrls.MOST_VISITED
            BrowserSettings.NewTabPage.SPEED_DIAL -> NewTabUrls.SPEED_DIAL
            BrowserSettings.NewTabPage.NEWS_FEED -> NewTabUrls.NEWS_FEED
            null -> _settings.value?.homePage ?: Tab.DEFAULT_URL
        }
    }

    fun closeTab(tabId: String) {
        launch {
            _error.clear()
            if (privateBrowsingManager.isPrivateTab(tabId)) privateBrowsingManager.unregisterPrivateTab(tabId)

            repository.closeTab(tabId)
                .onSuccess {
                    if (_activeTab.value?.tab?.id == tabId) {
                        _activeTab.value = _tabs.filter { it.tab.id != tabId }.firstOrNull()
                    }
                }
                .onFailure { e -> _error.value = "Failed to close tab: ${e.message}" }
        }
    }

    fun closeAllTabs() {
        launch {
            _error.clear()
            repository.closeAllTabs()
                .onSuccess { _activeTab.clear() }
                .onFailure { e -> _error.value = "Failed to close all tabs: ${e.message}" }
        }
    }

    fun togglePinned(tabId: String) {
        launch {
            _error.clear()
            _tabs.find { it.tab.id == tabId }?.let { tabState ->
                val newPinnedState = !tabState.tab.isPinned
                repository.setPinned(tabId, newPinnedState)
                    .onFailure { e -> _error.value = "Failed to ${if (newPinnedState) "pin" else "unpin"} tab: ${e.message}" }
            }
        }
    }

    fun switchTab(tabId: String) {
        launch {
            stateMutex.withLock {
                _tabs.find { it.tab.id == tabId }?.let { tabState ->
                    _activeTab.value = tabState
                    val updatedTab = tabState.tab.copy(lastAccessedAt = kotlinx.datetime.Clock.System.now())
                    repository.updateTab(updatedTab)
                } ?: run {
                    _error.value = TabError.TabNotFound(tabId).userMessage
                }
            }
        }
    }

    fun updateTab(tab: Tab) {
        launch {
            _error.clear()
            repository.updateTab(tab)
                .onSuccess {
                    if (_activeTab.value?.tab?.id == tab.id) {
                        _activeTab.value = _activeTab.value?.copy(tab = tab)
                    }
                }
                .onFailure { e -> _error.value = "Failed to update tab: ${e.message}" }
        }
    }

    fun navigateToUrl(url: String) {
        val validationResult = UrlValidation.validate(url, allowBlank = false)
        if (validationResult is UrlValidation.UrlValidationResult.Invalid) {
            _error.value = validationResult.error.userMessage
            return
        }

        val normalizedUrl = normalizeUrl(url)
        _activeTab.ifPresent { state ->
            updateTab(state.tab.copy(url = normalizedUrl))
        } ?: createTab(normalizedUrl, title = normalizedUrl)
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.removePrefix("http://").removePrefix("HTTP://")
            trimmed.contains("://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> {
                val encoded = encodeUrl(trimmed)
                val searchEngine = _settings.value?.defaultSearchEngine ?: BrowserSettings.SearchEngine.GOOGLE
                if (searchEngine == BrowserSettings.SearchEngine.CUSTOM) {
                    val customUrl = _settings.value?.customSearchEngineUrl ?: ""
                    if (customUrl.isNotBlank() && customUrl.contains("%s")) customUrl.replace("%s", encoded)
                    else "${BrowserSettings.SearchEngine.GOOGLE.baseUrl}?${BrowserSettings.SearchEngine.GOOGLE.queryParam}=$encoded"
                } else {
                    "${searchEngine.baseUrl}?${searchEngine.queryParam}=$encoded"
                }
            }
        }
    }

    // ========== Tab State Updates (Simplified) ==========

    private inline fun updateTabState(tabId: String, crossinline transform: (TabUiState) -> TabUiState) {
        launch {
            stateMutex.withLock {
                val tabState = _tabs.find { it.tab.id == tabId } ?: return@withLock
                val updatedState = transform(tabState)
                _tabs.updateItem({ it.tab.id == tabId }) { updatedState }
                if (_activeTab.value?.tab?.id == tabId) _activeTab.value = updatedState
            }
        }
    }

    private inline fun updateTabStateAndPersist(tabId: String, crossinline transform: (TabUiState) -> Pair<TabUiState, Tab>) {
        launch {
            stateMutex.withLock {
                val tabState = _tabs.find { it.tab.id == tabId } ?: return@withLock
                val (updatedState, updatedTab) = transform(tabState)
                _tabs.updateItem({ it.tab.id == tabId }) { updatedState }
                if (_activeTab.value?.tab?.id == tabId) _activeTab.value = updatedState
                repository.updateTab(updatedTab)
            }
        }
    }

    fun setTabLoading(isLoading: Boolean) {
        _activeTab.ifPresent { state ->
            updateTabState(state.tab.id) { it.copy(isLoading = isLoading) }
        }
    }

    fun setTabTitle(title: String) {
        _activeTab.ifPresent { state -> updateTab(state.tab.copy(title = title)) }
    }

    fun setNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _activeTab.ifPresent { state ->
            updateTabState(state.tab.id) { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
        }
    }

    fun updateTabUrl(tabId: String, url: String) {
        updateTabStateAndPersist(tabId) { state ->
            val updatedTab = state.tab.copy(url = url)
            state.copy(tab = updatedTab) to updatedTab
        }
    }

    fun updateTabLoading(tabId: String, isLoading: Boolean) {
        updateTabState(tabId) { it.copy(isLoading = isLoading) }
    }

    fun updateTabTitle(tabId: String, title: String) {
        updateTabStateAndPersist(tabId) { state ->
            val updatedTab = state.tab.copy(title = title)
            state.copy(tab = updatedTab) to updatedTab
        }
    }

    fun updateTabNavigation(tabId: String, canGoBack: Boolean? = null, canGoForward: Boolean? = null) {
        updateTabState(tabId) { state ->
            state.copy(canGoBack = canGoBack ?: state.canGoBack, canGoForward = canGoForward ?: state.canGoForward)
        }
    }

    fun clearError() = _error.clear()

    // ========== Scroll & Zoom ==========

    fun scrollUp() {}
    fun scrollDown() {}
    fun scrollLeft() {}
    fun scrollRight() {}
    fun scrollToTop() {}
    fun scrollToBottom() {}

    fun updateScrollPosition(x: Int, y: Int) {
        _activeTab.ifPresent { state ->
            launch {
                val updatedTab = state.tab.copy(scrollXPosition = x, scrollYPosition = y)
                repository.updateTab(updatedTab)
                    .onSuccess { _activeTab.value = state.copy(tab = updatedTab) }
                    .onFailure { e -> _error.value = "Failed to save scroll position: ${e.message}" }
            }
        }
    }

    fun zoomIn() {
        _activeTab.ifPresent { state -> setZoomLevel((state.tab.zoomLevel + 1).coerceIn(1, 5)) }
    }

    fun zoomOut() {
        _activeTab.ifPresent { state -> setZoomLevel((state.tab.zoomLevel - 1).coerceIn(1, 5)) }
    }

    fun setZoomLevel(level: Int) {
        _activeTab.ifPresent { state ->
            launch {
                val validatedLevel = level.coerceIn(1, 5)
                val updatedTab = state.tab.copy(zoomLevel = validatedLevel)
                repository.updateTab(updatedTab)
                    .onSuccess { _activeTab.value = state.copy(tab = updatedTab) }
                    .onFailure { e -> _error.value = "Failed to set zoom level: ${e.message}" }
            }
        }
    }

    // ========== Desktop Mode ==========

    fun toggleDesktopMode() {
        _activeTab.ifPresent { state -> setDesktopMode(!state.tab.isDesktopMode) }
    }

    fun setDesktopMode(enabled: Boolean) {
        _activeTab.ifPresent { state ->
            launch {
                val updatedTab = state.tab.copy(isDesktopMode = enabled)
                repository.updateTab(updatedTab)
                    .onSuccess { _activeTab.value = state.copy(tab = updatedTab) }
                    .onFailure { e -> _error.value = "Failed to set desktop mode: ${e.message}" }
            }
        }
    }

    fun freezePage() {}

    // ========== Reading Mode ==========

    fun toggleReadingMode() {
        launch {
            stateMutex.withLock {
                _activeTab.ifPresent { state ->
                    val newReadingMode = !state.isReadingMode
                    val updatedState = state.copy(isReadingMode = newReadingMode)
                    _activeTab.value = updatedState
                    _tabs.updateItem({ it.tab.id == state.tab.id }) { updatedState }
                    Logger.info("TabViewModel", "Reading mode ${if (newReadingMode) "enabled" else "disabled"}")
                }
            }
        }
    }

    fun setReadingModeArticle(article: ReadingModeArticle?) {
        launch {
            stateMutex.withLock {
                _activeTab.ifPresent { state ->
                    val updatedState = state.copy(readingModeArticle = article, isArticleAvailable = article != null)
                    _activeTab.value = updatedState
                    _tabs.updateItem({ it.tab.id == state.tab.id }) { updatedState }
                }
            }
        }
    }

    fun setArticleAvailable(isAvailable: Boolean) {
        launch {
            stateMutex.withLock {
                _activeTab.ifPresent { state ->
                    val updatedState = state.copy(isArticleAvailable = isAvailable)
                    _activeTab.value = updatedState
                    _tabs.updateItem({ it.tab.id == state.tab.id }) { updatedState }
                }
            }
        }
    }

    // ========== Find in Page ==========

    fun showFindInPage(initialQuery: String = "") {
        _findInPageState.update { it.copy(isVisible = true, query = initialQuery) }
    }

    fun hideFindInPage() {
        _findInPageState.value = FindInPageState(isVisible = false)
    }

    fun updateFindQuery(query: String) {
        _findInPageState.update { it.copy(query = query, currentMatch = 0, totalMatches = 0) }
    }

    fun updateFindResults(currentMatch: Int, totalMatches: Int) {
        _findInPageState.update { it.copy(currentMatch = currentMatch, totalMatches = totalMatches) }
    }

    fun toggleFindCaseSensitive() {
        _findInPageState.update { it.copy(caseSensitive = !it.caseSensitive) }
    }

    fun findNext() {
        _findInPageState.update { state ->
            if (state.totalMatches > 0) state.copy(currentMatch = (state.currentMatch + 1) % state.totalMatches)
            else state
        }
    }

    fun findPrevious() {
        _findInPageState.update { state ->
            if (state.totalMatches > 0) {
                val prevMatch = if (state.currentMatch == 0) state.totalMatches - 1 else state.currentMatch - 1
                state.copy(currentMatch = prevMatch)
            } else state
        }
    }

    fun clearFind() {
        _findInPageState.update { it.copy(query = "", currentMatch = 0, totalMatches = 0) }
    }

    // ========== Screenshot ==========

    fun captureScreenshot(
        type: ScreenshotType,
        quality: Int = 80,
        saveToGallery: Boolean = true,
        onProgress: (Float, String) -> Unit = { _, _ -> },
        onComplete: (ScreenshotData, String?) -> Unit,
        onError: (String) -> Unit
    ) {
        _activeTab.ifPresent { state ->
            Logger.info("TabViewModel", "Screenshot capture requested for tab ${state.tab.id}")
        } ?: run {
            Logger.error("TabViewModel", "Cannot capture screenshot: no active tab")
            onError("No active tab to capture")
        }
    }

    // ========== Session Management ==========

    fun saveSession() {
        launch {
            try {
                val tabs = _tabs.value.map { it.tab }.filter { !it.isIncognito }
                if (tabs.isEmpty()) {
                    Logger.info("TabViewModel", "No tabs to save (all private)")
                    return@launch
                }

                val activeTabId = _activeTab.value?.tab?.id
                val session = Session.create(activeTabId = activeTabId, tabCount = tabs.size, isCrashRecovery = false)
                val sessionTabs = tabs.map { tab -> SessionTab.fromTab(session.id, tab, tab.id == activeTabId) }

                repository.saveSession(session, sessionTabs)
                    .onSuccess { Logger.info("TabViewModel", "Session saved: ${session.id}") }
                    .onFailure { e -> Logger.error("TabViewModel", "Failed to save session: ${e.message}", e) }
            } catch (e: Exception) {
                Logger.error("TabViewModel", "Error saving session: ${e.message}", e)
            }
        }
    }

    suspend fun restoreSession(): Boolean {
        return try {
            if (_settings.value?.restoreTabsOnStartup == false) return false
            val session = repository.getLatestSession().getOrNull() ?: return false
            if (session.isCrashRecovery) return false
            val sessionTabs = repository.getSessionTabs(session.id).getOrNull()?.takeIf { it.isNotEmpty() } ?: return false

            repository.closeAllTabs()
            val restoredTabs = sessionTabs.map { it.toTab() }
            restoredTabs.forEach { repository.createTab(it) }
            val activeTab = restoredTabs.find { it.isActive } ?: restoredTabs.firstOrNull()
            activeTab?.let { repository.setActiveTab(it.id) }

            Logger.info("TabViewModel", "Session restored: ${session.id} with ${restoredTabs.size} tabs")
            true
        } catch (e: Exception) {
            Logger.error("TabViewModel", "Failed to restore session: ${e.message}", e)
            false
        }
    }

    suspend fun hasCrashRecoverySession(): Boolean {
        return try {
            repository.getLatestCrashSession().getOrNull() != null
        } catch (e: Exception) { false }
    }

    suspend fun restoreCrashRecoverySession(): Boolean {
        return try {
            val session = repository.getLatestCrashSession().getOrNull() ?: return false
            val sessionTabs = repository.getSessionTabs(session.id).getOrNull()?.takeIf { it.isNotEmpty() } ?: return false

            repository.closeAllTabs()
            val restoredTabs = sessionTabs.map { it.toTab() }
            restoredTabs.forEach { repository.createTab(it) }
            val activeTab = restoredTabs.find { it.isActive } ?: restoredTabs.firstOrNull()
            activeTab?.let { repository.setActiveTab(it.id) }

            Logger.info("TabViewModel", "Crash recovery session restored: ${session.id}")
            true
        } catch (e: Exception) {
            Logger.error("TabViewModel", "Failed to restore crash recovery session: ${e.message}", e)
            false
        }
    }

    suspend fun dismissCrashRecovery() {
        try {
            repository.getLatestCrashSession().getOrNull()?.let { session ->
                repository.deleteSession(session.id)
                Logger.info("TabViewModel", "Crash recovery session dismissed")
            }
        } catch (e: Exception) {
            Logger.error("TabViewModel", "Failed to dismiss crash recovery: ${e.message}", e)
        }
    }

    fun onAppPause() = saveSession()
    suspend fun onAppResume(): Boolean = hasCrashRecoverySession()

    override fun onCleared() {
        saveSession()
        super.onCleared()
    }
}
