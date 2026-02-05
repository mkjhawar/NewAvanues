package com.augmentalis.browseravanue.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.browseravanue.data.export.BrowserDataExporter
import com.augmentalis.browseravanue.data.export.BrowserDataImporter
import com.augmentalis.browseravanue.domain.model.Tab
import com.augmentalis.browseravanue.domain.usecase.favorite.AddFavoriteUseCase
import com.augmentalis.browseravanue.domain.usecase.favorite.DeleteFavoriteUseCase
import com.augmentalis.browseravanue.domain.usecase.favorite.GetAllFavoritesUseCase
import com.augmentalis.browseravanue.domain.usecase.navigation.NavigateUseCase
import com.augmentalis.browseravanue.domain.usecase.settings.GetSettingsUseCase
import com.augmentalis.browseravanue.domain.usecase.settings.UpdateSettingsUseCase
import com.augmentalis.browseravanue.domain.usecase.tab.CreateTabUseCase
import com.augmentalis.browseravanue.domain.usecase.tab.DeleteTabUseCase
import com.augmentalis.browseravanue.domain.usecase.tab.GetAllTabsUseCase
import com.augmentalis.browseravanue.domain.usecase.tab.UpdateTabUseCase
import com.augmentalis.browseravanue.domain.usecase.voice.VoiceCommandProcessor
import com.augmentalis.browseravanue.domain.usecase.voice.VoiceOSBridge
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Browser ViewModel
 *
 * Architecture:
 * - MVVM + MVI pattern
 * - Single state flow (BrowserState)
 * - Event-driven (BrowserEvent)
 * - Uses all 10 UseCases
 * - Lifecycle-aware
 *
 * Responsibilities:
 * - Handle user events
 * - Coordinate UseCases
 * - Manage UI state
 * - Expose state to UI
 * - Handle errors
 *
 * State Management:
 * - StateFlow for state (hot, replay last)
 * - Immutable state updates
 * - Combine multiple data sources
 * - Error handling with sealed states
 *
 * Dependencies:
 * - 10 UseCases (tab, favorite, settings, navigation)
 * - VoiceCommandProcessor + VoiceOSBridge
 * - Export/Import services
 *
 * Usage:
 * ```
 * @Composable
 * fun BrowserScreen(viewModel: BrowserViewModel = hiltViewModel()) {
 *     val state by viewModel.state.collectAsState()
 *
 *     when (state) {
 *         is BrowserState.Loading -> LoadingScreen()
 *         is BrowserState.Success -> {
 *             val data = (state as BrowserState.Success)
 *             BrowserContent(
 *                 state = data,
 *                 onEvent = viewModel::onEvent
 *             )
 *         }
 *         is BrowserState.Error -> ErrorScreen(state.message)
 *     }
 * }
 * ```
 */
class BrowserViewModel(
    // Tab UseCases
    private val getAllTabsUseCase: GetAllTabsUseCase,
    private val createTabUseCase: CreateTabUseCase,
    private val updateTabUseCase: UpdateTabUseCase,
    private val deleteTabUseCase: DeleteTabUseCase,

    // Favorite UseCases
    private val getAllFavoritesUseCase: GetAllFavoritesUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,

    // Settings & Navigation UseCases
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val navigateUseCase: NavigateUseCase,

    // Voice
    private val voiceCommandProcessor: VoiceCommandProcessor,
    private val voiceOSBridge: VoiceOSBridge,

    // Export/Import
    private val dataExporter: BrowserDataExporter,
    private val dataImporter: BrowserDataImporter
) : ViewModel() {

    // State
    private val _state = MutableStateFlow<BrowserState>(BrowserState.Loading)
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    // Current tab tracking
    private var currentTabId: String? = null

    // SSL/Auth handlers (stored temporarily for dialog callbacks)
    private var pendingSslHandler: android.webkit.SslErrorHandler? = null
    private var pendingAuthHandler: android.webkit.HttpAuthHandler? = null
    private var pendingPermissionRequest: android.webkit.PermissionRequest? = null

    init {
        loadBrowserData()
        observeVoiceCommands()
    }

    /**
     * Handle user events
     */
    fun onEvent(event: BrowserEvent) {
        viewModelScope.launch {
            when (event) {
                // Navigation
                is BrowserEvent.NavigateToUrl -> handleNavigate(event.url)
                is BrowserEvent.Search -> handleSearch(event.query)
                is BrowserEvent.GoBack -> handleGoBack()
                is BrowserEvent.GoForward -> handleGoForward()
                is BrowserEvent.Refresh -> handleRefresh()
                is BrowserEvent.StopLoading -> handleStopLoading()
                is BrowserEvent.GoHome -> handleGoHome()
                is BrowserEvent.NavigateToInternalPage -> handleNavigateToInternal(event.page)

                // Tabs
                is BrowserEvent.NewTab -> handleNewTab(event.url, event.isIncognito)
                is BrowserEvent.CloseTab -> handleCloseTab(event.tabId)
                is BrowserEvent.SwitchTab -> handleSwitchTab(event.tabId)
                is BrowserEvent.NextTab -> handleNextTab()
                is BrowserEvent.PreviousTab -> handlePreviousTab()
                is BrowserEvent.DuplicateTab -> handleDuplicateTab()
                is BrowserEvent.ReorderTabs -> handleReorderTabs(event.fromIndex, event.toIndex)

                // Favorites
                is BrowserEvent.AddToFavorites -> handleAddToFavorites(event.url, event.title, event.folder, event.tags)
                is BrowserEvent.RemoveFavorite -> handleRemoveFavorite(event.favoriteId)
                is BrowserEvent.OpenFavorite -> handleOpenFavorite(event.favoriteId)

                // Settings
                is BrowserEvent.UpdateSettings -> handleUpdateSettings(event.settings)
                is BrowserEvent.ToggleSetting -> handleToggleSetting(event.setting)

                // Page events
                is BrowserEvent.PageStarted -> handlePageStarted(event.url)
                is BrowserEvent.PageFinished -> handlePageFinished(event.url, event.title)
                is BrowserEvent.ProgressChanged -> handleProgressChanged(event.progress)
                is BrowserEvent.TitleReceived -> handleTitleReceived(event.title)
                is BrowserEvent.DownloadStarted -> handleDownloadStarted(event)
                is BrowserEvent.NewTabRequested -> handleNewTabRequested(event.url)

                // Privacy
                is BrowserEvent.ToggleAdBlocking -> handleToggleAdBlocking(event.enabled)
                is BrowserEvent.ToggleDoNotTrack -> handleToggleDoNotTrack(event.enabled)
                is BrowserEvent.ClearBrowsingData -> handleClearBrowsingData(event)

                // Find in page
                is BrowserEvent.FindInPage -> handleFindInPage(event.query)
                is BrowserEvent.FindNext -> handleFindNext()
                is BrowserEvent.FindPrevious -> handleFindPrevious()
                is BrowserEvent.CloseFindInPage -> handleCloseFindInPage()

                // Scroll & Zoom
                is BrowserEvent.Scroll -> handleScroll(event.direction)
                is BrowserEvent.Zoom -> handleZoom(event.direction)

                // Voice
                is BrowserEvent.VoiceCommand -> handleVoiceCommand(event.command)

                // Dialogs
                is BrowserEvent.SslErrorOccurred -> handleSslError(event.error, event.handler)
                is BrowserEvent.SslErrorDecision -> handleSslDecision(event.proceed)
                is BrowserEvent.AuthenticationRequired -> handleAuthRequired(event.host, event.realm, event.handler)
                is BrowserEvent.AuthenticationCredentials -> handleAuthCredentials(event.username, event.password)
                is BrowserEvent.PermissionRequested -> handlePermissionRequest(event.request)
                is BrowserEvent.PermissionDecision -> handlePermissionDecision(event.granted)

                // UI state
                is BrowserEvent.ToggleTabSwitcher -> handleToggleTabSwitcher()
                is BrowserEvent.ToggleFavorites -> handleToggleFavorites()
                is BrowserEvent.ToggleSettings -> handleToggleSettings()
                is BrowserEvent.ShowHistory -> handleShowHistory()
                is BrowserEvent.ShowDownloads -> handleShowDownloads()
                is BrowserEvent.DismissDialog -> handleDismissDialog()

                // Export/Import
                is BrowserEvent.ExportData -> handleExportData(event.includeHistory, event.includeCookies)
                is BrowserEvent.ImportData -> handleImportData(event.jsonData, event.conflictStrategy)
            }
        }
    }

    /**
     * Load initial browser data
     */
    private fun loadBrowserData() {
        viewModelScope.launch {
            try {
                // Combine all data sources
                combine(
                    getAllTabsUseCase(),
                    getAllFavoritesUseCase(),
                    getSettingsUseCase()
                ) { tabs, favorites, settings ->
                    // Create success state
                    BrowserState.Success(
                        tabs = tabs,
                        currentTab = tabs.firstOrNull(),
                        currentTabIndex = 0,
                        favorites = favorites,
                        settings = settings
                    )
                }.collect { successState ->
                    _state.value = successState

                    // Track current tab
                    currentTabId = successState.currentTab?.id
                }
            } catch (e: Exception) {
                _state.value = BrowserState.Error("Failed to load browser data: ${e.message}", e)
            }
        }
    }

    /**
     * Observe voice commands from VoiceOS
     */
    private fun observeVoiceCommands() {
        viewModelScope.launch {
            voiceOSBridge.commandResults.collect { result ->
                updateSuccessState { state ->
                    state.copy(
                        lastVoiceCommand = result.action.name,
                        voiceCommandResult = result.message
                    )
                }
            }
        }
    }

    // ==========================================
    // Event Handlers - Navigation
    // ==========================================

    private suspend fun handleNavigate(url: String) {
        val fullUrl = if (!url.startsWith("http")) "https://$url" else url

        currentTabId?.let { tabId ->
            navigateUseCase(tabId, fullUrl)
        } ?: run {
            // No current tab - create new one
            handleNewTab(fullUrl, false)
        }
    }

    private suspend fun handleSearch(query: String) {
        val settings = getSettingsUseCase().first()
        val searchUrl = settings.searchEngine.getSearchUrl(query)
        handleNavigate(searchUrl)
    }

    private fun handleGoBack() {
        // Handled by WebView directly - just update state
        updateSuccessState { it.copy(canGoBack = false) } // Will be updated by PageFinished
    }

    private fun handleGoForward() {
        // Handled by WebView directly
        updateSuccessState { it.copy(canGoForward = false) }
    }

    private fun handleRefresh() {
        // Handled by WebView directly
    }

    private fun handleStopLoading() {
        // Handled by WebView directly
        updateSuccessState { it.copy(pageLoading = false) }
    }

    private suspend fun handleGoHome() {
        val settings = getSettingsUseCase().first()
        handleNavigate(settings.homepage)
    }

    private fun handleNavigateToInternal(page: InternalPage) {
        updateSuccessState { state ->
            state.copy(
                showFavorites = page == InternalPage.FAVORITES,
                showHistory = page == InternalPage.HISTORY,
                showSettings = page == InternalPage.SETTINGS,
                showDownloads = page == InternalPage.DOWNLOADS
            )
        }
    }

    // ==========================================
    // Event Handlers - Tabs
    // ==========================================

    private suspend fun handleNewTab(url: String?, isIncognito: Boolean) {
        val settings = getSettingsUseCase().first()
        val tabUrl = url ?: settings.homepage

        val tab = Tab(
            url = tabUrl,
            title = "New Tab",
            isIncognito = isIncognito
        )

        createTabUseCase(tab)
        currentTabId = tab.id
    }

    private suspend fun handleCloseTab(tabId: String) {
        deleteTabUseCase(tabId)

        // Switch to next tab if we closed current
        if (tabId == currentTabId) {
            val tabs = getAllTabsUseCase().first()
            currentTabId = tabs.firstOrNull()?.id
        }
    }

    private fun handleSwitchTab(tabId: String) {
        currentTabId = tabId

        updateSuccessState { state ->
            val tabIndex = state.tabs.indexOfFirst { it.id == tabId }
            val currentTab = state.tabs.find { it.id == tabId }

            state.copy(
                currentTab = currentTab,
                currentTabIndex = tabIndex,
                showTabSwitcher = false
            )
        }
    }

    private fun handleNextTab() {
        updateSuccessState { state ->
            val nextIndex = (state.currentTabIndex + 1) % state.tabs.size
            val nextTab = state.tabs.getOrNull(nextIndex)

            currentTabId = nextTab?.id

            state.copy(
                currentTab = nextTab,
                currentTabIndex = nextIndex
            )
        }
    }

    private fun handlePreviousTab() {
        updateSuccessState { state ->
            val prevIndex = if (state.currentTabIndex <= 0) {
                state.tabs.size - 1
            } else {
                state.currentTabIndex - 1
            }
            val prevTab = state.tabs.getOrNull(prevIndex)

            currentTabId = prevTab?.id

            state.copy(
                currentTab = prevTab,
                currentTabIndex = prevIndex
            )
        }
    }

    private suspend fun handleDuplicateTab() {
        currentTabId?.let { tabId ->
            val tabs = getAllTabsUseCase().first()
            val tab = tabs.find { it.id == tabId }

            tab?.let {
                val duplicate = it.copy(
                    id = "", // Will be generated
                    title = "${it.title} (Copy)"
                )
                createTabUseCase(duplicate)
            }
        }
    }

    private suspend fun handleReorderTabs(fromIndex: Int, toIndex: Int) {
        // TODO: Implement tab reordering in database
        // For now, just update local state
    }

    // ==========================================
    // Event Handlers - Favorites
    // ==========================================

    private suspend fun handleAddToFavorites(url: String, title: String, folder: String?, tags: List<String>) {
        addFavoriteUseCase(url, title, folder, tags)
    }

    private suspend fun handleRemoveFavorite(favoriteId: String) {
        deleteFavoriteUseCase(favoriteId)
    }

    private suspend fun handleOpenFavorite(favoriteId: String) {
        val favorites = getAllFavoritesUseCase().first()
        val favorite = favorites.find { it.id == favoriteId }

        favorite?.let {
            handleNavigate(it.url)
        }
    }

    // ==========================================
    // Event Handlers - Settings & Privacy
    // ==========================================

    private suspend fun handleUpdateSettings(settings: com.augmentalis.browseravanue.domain.model.BrowserSettings) {
        updateSettingsUseCase(settings)
    }

    private suspend fun handleToggleSetting(setting: SettingType) {
        val currentSettings = getSettingsUseCase().first()

        val newSettings = when (setting) {
            SettingType.DESKTOP_MODE -> currentSettings.copy(desktopMode = !currentSettings.desktopMode)
            SettingType.DARK_MODE -> currentSettings.copy(darkMode = !currentSettings.darkMode)
            SettingType.AD_BLOCKING -> currentSettings.copy(adBlockingEnabled = !currentSettings.adBlockingEnabled)
            SettingType.DO_NOT_TRACK -> currentSettings.copy(doNotTrackEnabled = !currentSettings.doNotTrackEnabled)
            SettingType.COOKIES -> currentSettings.copy(acceptCookies = !currentSettings.acceptCookies)
            SettingType.JAVASCRIPT -> currentSettings.copy(javascriptEnabled = !currentSettings.javascriptEnabled)
        }

        updateSettingsUseCase(newSettings)
    }

    private fun handleToggleAdBlocking(enabled: Boolean) {
        updateSuccessState { it.copy(adBlockingEnabled = enabled) }
    }

    private fun handleToggleDoNotTrack(enabled: Boolean) {
        updateSuccessState { it.copy(doNotTrackEnabled = enabled) }
    }

    private fun handleClearBrowsingData(event: BrowserEvent.ClearBrowsingData) {
        // TODO: Implement clear browsing data
        // For now, just acknowledge
    }

    // ==========================================
    // Event Handlers - Page Events
    // ==========================================

    private fun handlePageStarted(url: String) {
        updateSuccessState { state ->
            state.copy(
                pageLoading = true,
                pageProgress = 0,
                pageUrl = url
            )
        }
    }

    private suspend fun handlePageFinished(url: String, title: String?) {
        updateSuccessState { state ->
            state.copy(
                pageLoading = false,
                pageProgress = 100,
                pageUrl = url,
                pageTitle = title
            )
        }

        // Update tab
        currentTabId?.let { tabId ->
            val tabs = getAllTabsUseCase().first()
            val tab = tabs.find { it.id == tabId }

            tab?.let {
                val updatedTab = it.copy(
                    url = url,
                    title = title ?: url,
                    lastAccessed = System.currentTimeMillis()
                )
                updateTabUseCase(updatedTab)
            }
        }
    }

    private fun handleProgressChanged(progress: Int) {
        updateSuccessState { it.copy(pageProgress = progress) }
    }

    private fun handleTitleReceived(title: String) {
        updateSuccessState { it.copy(pageTitle = title) }
    }

    private fun handleDownloadStarted(event: BrowserEvent.DownloadStarted) {
        val download = DownloadInfo(
            url = event.url,
            filename = extractFilename(event.contentDisposition, event.url),
            mimeType = event.mimeType,
            contentLength = event.contentLength
        )

        updateSuccessState { state ->
            state.copy(
                pendingDownloads = state.pendingDownloads + download
            )
        }
    }

    private suspend fun handleNewTabRequested(url: String) {
        handleNewTab(url, false)
    }

    // ==========================================
    // Event Handlers - Find, Scroll, Zoom
    // ==========================================

    private fun handleFindInPage(query: String) {
        updateSuccessState { state ->
            state.copy(
                findInPageQuery = query,
                findInPageActive = true
            )
        }
    }

    private fun handleFindNext() {
        // Handled by WebView - results come back via callback
    }

    private fun handleFindPrevious() {
        // Handled by WebView
    }

    private fun handleCloseFindInPage() {
        updateSuccessState { state ->
            state.copy(
                findInPageQuery = "",
                findInPageActive = false,
                findInPageCurrentMatch = 0,
                findInPageTotalMatches = 0
            )
        }
    }

    private fun handleScroll(direction: ScrollDirection) {
        // Handled by WebView directly
    }

    private fun handleZoom(direction: ZoomDirection) {
        // Handled by WebView directly
    }

    // ==========================================
    // Event Handlers - Voice
    // ==========================================

    private suspend fun handleVoiceCommand(command: String) {
        voiceOSBridge.onCommandReceived(command, currentTabId)
    }

    // ==========================================
    // Event Handlers - Dialogs
    // ==========================================

    private fun handleSslError(error: android.net.http.SslError, handler: android.webkit.SslErrorHandler) {
        pendingSslHandler = handler

        updateSuccessState { state ->
            state.copy(
                showSslErrorDialog = true,
                sslErrorMessage = "SSL Error: ${error.primaryError}"
            )
        }
    }

    private fun handleSslDecision(proceed: Boolean) {
        if (proceed) {
            pendingSslHandler?.proceed()
        } else {
            pendingSslHandler?.cancel()
        }

        pendingSslHandler = null

        updateSuccessState { state ->
            state.copy(showSslErrorDialog = false, sslErrorMessage = null)
        }
    }

    private fun handleAuthRequired(host: String, realm: String?, handler: android.webkit.HttpAuthHandler) {
        pendingAuthHandler = handler

        updateSuccessState { state ->
            state.copy(
                showAuthDialog = true,
                authHost = host,
                authRealm = realm
            )
        }
    }

    private fun handleAuthCredentials(username: String, password: String) {
        pendingAuthHandler?.proceed(username, password)
        pendingAuthHandler = null

        updateSuccessState { state ->
            state.copy(
                showAuthDialog = false,
                authHost = null,
                authRealm = null
            )
        }
    }

    private fun handlePermissionRequest(request: android.webkit.PermissionRequest) {
        pendingPermissionRequest = request
        // TODO: Show permission dialog
    }

    private fun handlePermissionDecision(granted: Boolean) {
        if (granted) {
            pendingPermissionRequest?.grant(pendingPermissionRequest?.resources)
        } else {
            pendingPermissionRequest?.deny()
        }

        pendingPermissionRequest = null
    }

    // ==========================================
    // Event Handlers - UI State
    // ==========================================

    private fun handleToggleTabSwitcher() {
        updateSuccessState { state ->
            state.copy(showTabSwitcher = !state.showTabSwitcher)
        }
    }

    private fun handleToggleFavorites() {
        updateSuccessState { state ->
            state.copy(showFavorites = !state.showFavorites)
        }
    }

    private fun handleToggleSettings() {
        updateSuccessState { state ->
            state.copy(showSettings = !state.showSettings)
        }
    }

    private fun handleShowHistory() {
        updateSuccessState { state ->
            state.copy(showHistory = true)
        }
    }

    private fun handleShowDownloads() {
        updateSuccessState { state ->
            state.copy(showDownloads = true)
        }
    }

    private fun handleDismissDialog() {
        updateSuccessState { state ->
            state.copy(
                showSslErrorDialog = false,
                showAuthDialog = false,
                sslErrorMessage = null,
                authHost = null,
                authRealm = null
            )
        }
    }

    // ==========================================
    // Event Handlers - Export/Import
    // ==========================================

    private suspend fun handleExportData(includeHistory: Boolean, includeCookies: Boolean) {
        try {
            val exportFile = dataExporter.exportAll(
                includeHistory = includeHistory,
                includeCookies = includeCookies
            )

            // TODO: Show success message with file path
        } catch (e: Exception) {
            // TODO: Show error message
        }
    }

    private suspend fun handleImportData(
        jsonData: String,
        conflictStrategy: com.augmentalis.browseravanue.data.export.ConflictStrategy
    ) {
        try {
            val result = dataImporter.importFromJson(jsonData, conflictStrategy)

            if (result.success) {
                // Reload browser data to reflect imports
                loadBrowserData()
            }

            // TODO: Show import result message
        } catch (e: Exception) {
            // TODO: Show error message
        }
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    /**
     * Update success state safely
     */
    private fun updateSuccessState(update: (BrowserState.Success) -> BrowserState.Success) {
        val currentState = _state.value
        if (currentState is BrowserState.Success) {
            _state.value = update(currentState)
        }
    }

    /**
     * Extract filename from Content-Disposition header or URL
     */
    private fun extractFilename(contentDisposition: String, url: String): String {
        // Try to extract from Content-Disposition header
        val filenameRegex = "filename=\"?([^\"]+)\"?".toRegex()
        val match = filenameRegex.find(contentDisposition)

        return match?.groupValues?.get(1)
            ?: url.substringAfterLast("/").substringBefore("?")
            ?: "download"
    }
}
