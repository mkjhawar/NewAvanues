package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.OceanTheme
import com.augmentalis.Avanues.web.universal.presentation.design.OceanComponents
import com.augmentalis.Avanues.web.universal.presentation.design.IconVariant
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
// FIX: Removed TabBar import - now using AddressBar dropdowns
import com.augmentalis.Avanues.web.universal.presentation.ui.tab.TabSwitcherView
import com.augmentalis.Avanues.web.universal.presentation.ui.tab.TabGroupDialog
import com.augmentalis.Avanues.web.universal.presentation.ui.tab.TabGroupAssignmentDialog
import com.augmentalis.Avanues.web.universal.presentation.ui.spatial.SpatialTabSwitcher
import com.augmentalis.Avanues.web.universal.presentation.ui.spatial.SpatialFavoritesShelf
import com.augmentalis.Avanues.web.universal.presentation.ui.components.NetworkStatusIndicator
import com.augmentalis.Avanues.web.universal.presentation.ui.components.NetworkStatus
import com.augmentalis.Avanues.web.universal.presentation.ui.components.rememberNetworkStatusMonitor
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabViewModel
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SettingsViewModel
import com.augmentalis.webavanue.domain.model.BrowserSettings
import kotlinx.coroutines.launch

/**
 * BrowserScreen - Main browser screen
 *
 * Features:
 * - Tab bar with all open tabs
 * - Address bar with navigation controls
 * - WebView container for web content
 * - Navigation to bookmarks/downloads/history/settings
 * - WebXR session indicator and controls (Android only)
 *
 * @param tabViewModel ViewModel for tab management
 * @param xrManager XRManager instance (Android only, nullable for other platforms)
 * @param xrState Current XR state (null for non-Android platforms)
 * @param onNavigateToBookmarks Callback to navigate to bookmarks screen
 * @param onNavigateToDownloads Callback to navigate to downloads screen
 * @param onNavigateToHistory Callback to navigate to history screen
 * @param onNavigateToSettings Callback to navigate to settings screen
 * @param onNavigateToXRSettings Callback to navigate to XR settings screen
 * @param modifier Modifier for customization
 */
@Composable
fun BrowserScreen(
    tabViewModel: TabViewModel,
    settingsViewModel: com.augmentalis.Avanues.web.universal.presentation.viewmodel.SettingsViewModel,
    historyViewModel: com.augmentalis.Avanues.web.universal.presentation.viewmodel.HistoryViewModel,
    favoriteViewModel: com.augmentalis.Avanues.web.universal.presentation.viewmodel.FavoriteViewModel,
    securityViewModel: com.augmentalis.Avanues.web.universal.presentation.viewmodel.SecurityViewModel,
    downloadViewModel: com.augmentalis.Avanues.web.universal.presentation.viewmodel.DownloadViewModel? = null,
    xrManager: Any? = null,  // XRManager on Android, null on other platforms
    xrState: Any? = null,    // XRManager.XRState on Android, null on other platforms
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToXRSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeTab by tabViewModel.activeTab.collectAsState()
    val tabs by tabViewModel.tabs.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val favorites by favoriteViewModel.favorites.collectAsState()
    val findInPageState by tabViewModel.findInPageState.collectAsState()

    // Error state for user feedback
    val error by tabViewModel.error.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    // Show error in Snackbar when error state changes
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
            // Clear error after showing
            tabViewModel.clearError()
        }
    }

    // Security dialog states
    val sslErrorState by securityViewModel.sslErrorState.collectAsState()
    val permissionRequestState by securityViewModel.permissionRequestState.collectAsState()
    val jsAlertState by securityViewModel.jsAlertState.collectAsState()
    val jsConfirmState by securityViewModel.jsConfirmState.collectAsState()
    val jsPromptState by securityViewModel.jsPromptState.collectAsState()
    val httpAuthState by securityViewModel.httpAuthState.collectAsState()

    var urlInput by rememberSaveable { mutableStateOf("") }

    // FIX: WebViewController must be tab-specific, not shared across all tabs
    // Each tab needs its own controller instance to avoid cross-tab navigation issues
    // Use remember with activeTab.tab.id as key to create controller per tab
    val webViewController = remember(activeTab?.tab?.id) {
        WebViewController()
    }

    // Voice-first UI state
    var isListening by rememberSaveable { mutableStateOf(false) }
    var showTextCommand by rememberSaveable { mutableStateOf(false) }
    var showVoiceHelp by rememberSaveable { mutableStateOf(false) }
    var isScrollFrozen by rememberSaveable { mutableStateOf(false) }
    var lastVoiceCommand by rememberSaveable { mutableStateOf<String?>(null) }

    // Clear last command after delay
    LaunchedEffect(lastVoiceCommand) {
        if (lastVoiceCommand != null) {
            kotlinx.coroutines.delay(2000L)
            lastVoiceCommand = null
        }
    }

    // Tab switcher state
    var showTabSwitcher by rememberSaveable { mutableStateOf(false) }

    // Spatial z-level views (3D tab switcher and favorites shelf)
    var showSpatialTabSwitcher by rememberSaveable { mutableStateOf(false) }
    var showSpatialFavorites by rememberSaveable { mutableStateOf(false) }

    // Command bar visibility - user-controlled toggle (default hidden for voice-first UX)
    var isCommandBarVisible by rememberSaveable { mutableStateOf(false) }

    // Voice listening state - separate from command bar visibility
    // Note: isVoiceMode removed - command bar visibility is now directly controlled

    // Network status monitoring - shows alert when disconnected
    val networkStatus = rememberNetworkStatusMonitor()

    // Headless browser mode - hides address bar, FAB; shows only web content + command bar
    var isHeadlessMode by rememberSaveable { mutableStateOf(false) }

    // Landscape detection - will be set by BoxWithConstraints
    var isLandscape by remember { mutableStateOf(false) }

    // Desktop mode comes from active tab
    val isDesktopMode = activeTab?.tab?.isDesktopMode ?: false

    // Add Page dialog state
    var showAddPageDialog by rememberSaveable { mutableStateOf(false) }
    var newPageUrl by rememberSaveable { mutableStateOf("") }

    // Basic Auth dialog state
    var showBasicAuthDialog by rememberSaveable { mutableStateOf(false) }
    var authUrl by rememberSaveable { mutableStateOf("") }
    var authRealm by rememberSaveable { mutableStateOf<String?>(null) }

    // Add to Favorites dialog state
    var showAddToFavoritesDialog by rememberSaveable { mutableStateOf(false) }

    // FIX L15: Tab group state - use rememberSaveable to survive config changes
    // Note: TabGroup is a simple data class, should be serializable for rememberSaveable
    // For now, we keep empty list as groups are not persisted yet - but state survives rotation
    var tabGroups by rememberSaveable { mutableStateOf<List<com.augmentalis.webavanue.domain.model.TabGroup>>(emptyList()) }
    var showTabGroupDialog by rememberSaveable { mutableStateOf(false) }
    var showTabGroupAssignmentDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTabForGroupAssignment by rememberSaveable { mutableStateOf<String?>(null) }

    // Coroutine scope for async operations
    val scope = rememberCoroutineScope()

    // Helper to show command bar briefly (for voice commands feedback)
    fun showCommandBarBriefly() {
        if (!isCommandBarVisible) {
            isCommandBarVisible = true
            scope.launch {
                kotlinx.coroutines.delay(3000L)  // Show for 3 seconds
                isCommandBarVisible = false
            }
        }
    }

    // Helper to trigger find in page (for keyboard shortcuts like Ctrl+F)
    // Can be called from Activity level keyboard event handlers
    fun triggerFindInPage() {
        if (!findInPageState.isVisible) {
            tabViewModel.showFindInPage()
        }
    }

    // NOTE: Do NOT call onCleared() on ViewModels here!
    // ViewModels are shared across Voyager screens via ViewModelHolder.
    // Calling onCleared() when BrowserScreen leaves composition (e.g., navigating to Settings)
    // would cancel their viewModelScope, causing all subsequent operations to fail silently.
    // ViewModels should only be cleared when the entire app is destroyed (in BrowserApp or MainActivity).

    // Check if current URL is favorited - use derivedStateOf for reactive updates
    // This ensures the value updates correctly when favorites change (including after orientation change)
    val isFavorite by remember {
        derivedStateOf {
            activeTab?.tab?.url?.let { url ->
                favorites.any { it.url == url }
            } ?: false
        }
    }

    // Update URL input when active tab changes
    LaunchedEffect(activeTab?.tab?.url) {
        urlInput = activeTab?.tab?.url ?: ""
    }

    // Find in page: Trigger search when query changes (with debouncing)
    LaunchedEffect(findInPageState.query, findInPageState.caseSensitive) {
        if (findInPageState.isVisible && findInPageState.query.isNotEmpty()) {
            // Debounce search - wait 300ms after user stops typing
            kotlinx.coroutines.delay(300)
            webViewController.findInPage(
                query = findInPageState.query,
                caseSensitive = findInPageState.caseSensitive
            ) { currentMatch, totalMatches ->
                tabViewModel.updateFindResults(currentMatch, totalMatches)
            }
        } else if (findInPageState.query.isEmpty()) {
            // Clear highlights when query is empty
            webViewController.clearFindMatches()
        }
    }

    // Note: Keyboard shortcuts for find in page:
    // - Ctrl+F / Cmd+F: Open find bar (handled at activity level or via hardware keyboard)
    // - Enter: Next match (handled in FindInPageBar KeyboardActions)
    // - Shift+Enter: Previous match (would need KeyEvent interception)
    // - Escape: Close find bar (would need KeyEvent interception)
    // For full keyboard support, implement onPreviewKeyEvent at activity level

    // Find in page: Navigate when currentMatch changes
    LaunchedEffect(findInPageState.currentMatch) {
        // Skip first emission (initial state)
        if (findInPageState.totalMatches > 0 && findInPageState.currentMatch > 0) {
            // User changed match manually via next/prev buttons
            // Note: WebView automatically scrolls to match via findNext/findPrevious
        }
    }

    // Command bar at bottom, overlays on webpage, auto-hides in voice mode

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Detect landscape orientation based on constraints
        isLandscape = maxWidth > maxHeight

        // Auto-fit zoom when switching to landscape mode
        LaunchedEffect(isLandscape) {
            if (isLandscape) {
                // Apply auto-fit zoom to show full page width in landscape
                webViewController.applyAutoFitZoom()
            }
        }

        // Network status indicator - shows at top when disconnected
        NetworkStatusIndicator(
            status = networkStatus,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(15f)  // Above everything else
        )

        // Main layout: Column with AddressBar at top, then content area with overlay command bar
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Address bar with integrated tabs, favorites, and voice mode toggle
            // Hidden in headless mode for true fullscreen browsing
            androidx.compose.animation.AnimatedVisibility(
                visible = !isHeadlessMode,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                AddressBar(
                url = urlInput,
                canGoBack = activeTab?.canGoBack ?: false,
                canGoForward = activeTab?.canGoForward ?: false,
                isDesktopMode = isDesktopMode,
                isFavorite = isFavorite,
                tabCount = tabs.size,
                tabs = tabs,
                activeTabId = activeTab?.tab?.id,
                favorites = favorites,
                onUrlChange = { newUrl ->
                    urlInput = newUrl
                },
                onGo = {
                    if (urlInput.isNotBlank()) {
                        tabViewModel.navigateToUrl(urlInput)
                    }
                },
                onBack = {
                    webViewController.goBack()
                },
                onForward = {
                    webViewController.goForward()
                },
                onRefresh = {
                    webViewController.reload()
                },
                onDesktopModeToggle = {
                    val newMode = !isDesktopMode
                    webViewController.setDesktopMode(newMode)
                    tabViewModel.setDesktopMode(newMode)
                    // Reload current page to apply new user agent
                    webViewController.reload()
                },
                onFavoriteClick = {
                    // Legacy callback - now handled by dropdown
                    showAddToFavoritesDialog = true
                },
                onTabClick = { tabId ->
                    tabViewModel.switchTab(tabId)
                },
                onTabClose = { tabId ->
                    tabViewModel.closeTab(tabId)
                },
                onNewTab = {
                    // FIX: Show dialog to prompt for URL instead of creating blank tab
                    showAddPageDialog = true
                },
                onFavoriteNavigate = { favorite ->
                    tabViewModel.navigateToUrl(favorite.url)
                },
                onAddFavorite = {
                    // FIX: Use async addFavorite with duplicate prevention and crash protection
                    activeTab?.let { tab ->
                        scope.launch {
                            try {
                                val success = favoriteViewModel.addFavorite(
                                    url = tab.tab.url,
                                    title = tab.tab.title.ifBlank { tab.tab.url },
                                    favicon = tab.tab.favicon
                                )
                                if (!success) {
                                    // Duplicate detected - show error via dialog
                                    showAddToFavoritesDialog = true
                                }
                            } catch (e: Exception) {
                                println("BrowserScreen: Error adding favorite: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }
                },
                onShowFavorites = {
                    showSpatialFavorites = true
                },
                onHistoryClick = onNavigateToHistory,
                onSettingsClick = onNavigateToSettings,
                onTabSwitcherClick = {
                    showTabSwitcher = true
                },
                isCommandBarVisible = isCommandBarVisible,
                onCommandBarToggle = {
                    isCommandBarVisible = !isCommandBarVisible
                },
                isListening = isListening,
                onStartListening = {
                    // Start voice recognition - for now just show indicator
                    // Actual voice recognition will be triggered from Android Activity
                    isListening = true
                    // Show command bar briefly when listening starts
                    showCommandBarBriefly()
                    // Auto-stop after 5 seconds (demo timeout)
                    scope.launch {
                        kotlinx.coroutines.delay(5000L)
                        isListening = false
                    }
                }
                )
            }  // End AnimatedVisibility for AddressBar

            // WebView container with overlay command bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                // WebView fills the space
                activeTab?.let { tabState ->
                    WebViewContainer(
                        tabId = tabState.tab.id,
                        url = tabState.tab.url,
                        controller = webViewController,
                        onUrlChange = { newUrl ->
                            tabViewModel.updateTabUrl(tabState.tab.id, newUrl)
                            urlInput = newUrl
                        },
                        onLoadingChange = { isLoading ->
                            tabViewModel.updateTabLoading(tabState.tab.id, isLoading)
                            // Apply auto-fit zoom when page finishes loading in landscape mode
                            if (!isLoading && isLandscape) {
                                webViewController.applyAutoFitZoom()
                            }
                        },
                        onTitleChange = { title ->
                            tabViewModel.updateTabTitle(tabState.tab.id, title)
                            if (title.isNotBlank() && tabState.tab.url.isNotBlank()) {
                                historyViewModel.addHistoryEntry(url = tabState.tab.url, title = title)
                            }
                        },
                        onProgressChange = { },
                        canGoBack = { canGoBack -> tabViewModel.updateTabNavigation(tabState.tab.id, canGoBack = canGoBack) },
                        canGoForward = { canGoForward -> tabViewModel.updateTabNavigation(tabState.tab.id, canGoForward = canGoForward) },
                        sessionData = tabState.tab.sessionData,
                        onSessionDataChange = { sessionData ->
                            val updatedTab = tabState.tab.copy(sessionData = sessionData)
                            tabViewModel.updateTab(updatedTab)
                        },
                        securityViewModel = securityViewModel,
                        onDownloadStart = downloadViewModel?.let { vm ->
                            { request: DownloadRequest ->
                                vm.startDownload(url = request.url, filename = request.filename, mimeType = request.mimeType, fileSize = request.contentLength, sourcePageUrl = tabState.tab.url, sourcePageTitle = tabState.tab.title)
                            }
                        },
                        initialScale = settings?.initialScale ?: 0.75f,
                        settings = settings,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: EmptyBrowserState(
                    onNewTab = { tabViewModel.createTab(isDesktopMode = settings?.useDesktopMode == true) },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay command bar at bottom (z-level above WebView)
                // FIX: Add zIndex(20f) to ensure command bar appears above WebView
                androidx.compose.animation.AnimatedVisibility(
                    visible = isCommandBarVisible,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(20f)  // Ensure command bar renders above WebView
                ) {
                    HorizontalCommandBarLayout(
                        onBack = { webViewController.goBack() },
                        onForward = { webViewController.goForward() },
                        onHome = {
                            val homeUrl = settings?.homePage ?: "https://www.google.com"
                            tabViewModel.navigateToUrl(homeUrl)
                        },
                        onRefresh = { webViewController.reload() },
                        onScrollUp = { webViewController.scrollUp() },
                        onScrollDown = { webViewController.scrollDown() },
                        onScrollTop = { webViewController.scrollToTop() },
                        onScrollBottom = { webViewController.scrollToBottom() },
                        onFreezePage = {
                            isScrollFrozen = !isScrollFrozen
                            webViewController.setScrollFrozen(isScrollFrozen)
                        },
                        isScrollFrozen = isScrollFrozen,
                        onZoomIn = { webViewController.zoomIn(); tabViewModel.zoomIn() },
                        onZoomOut = { webViewController.zoomOut(); tabViewModel.zoomOut() },
                        onZoomLevel = { level -> webViewController.setZoomLevel(level); tabViewModel.setZoomLevel(level) },
                        onDesktopModeToggle = {
                            val newMode = !isDesktopMode
                            webViewController.setDesktopMode(newMode)
                            tabViewModel.setDesktopMode(newMode)
                            // Reload current page to apply new user agent
                            webViewController.reload()
                        },
                        onFavorite = {
                            activeTab?.let { tab ->
                                scope.launch {
                                    try {
                                        val url = tab.tab.url
                                        val isFavorited = favoriteViewModel.isFavorite(url)
                                        if (isFavorited) {
                                            favorites.find { it.url == url }?.let { fav ->
                                                favoriteViewModel.removeFavorite(fav.id)
                                            }
                                        } else {
                                            favoriteViewModel.addFavorite(url = url, title = tab.tab.title.ifBlank { url }, favicon = tab.tab.favicon)
                                        }
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            }
                        },
                        isDesktopMode = isDesktopMode,
                        onBookmarks = onNavigateToBookmarks,
                        onDownloads = onNavigateToDownloads,
                        onHistory = onNavigateToHistory,
                        onSettings = onNavigateToSettings,
                        onNewTab = { showAddPageDialog = true },
                        onShowTabs = { showSpatialTabSwitcher = true },
                        onShowFavorites = { showSpatialFavorites = true },
                        onHide = { isCommandBarVisible = false },
                        isHeadlessMode = isHeadlessMode,
                        onToggleHeadlessMode = { isHeadlessMode = !isHeadlessMode },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }

                // Reading Mode Overlay (when enabled, covers entire WebView)
                activeTab?.let { tabState ->
                    if (tabState.isReadingMode && tabState.readingModeArticle != null) {
                        ReadingModeView(
                            article = tabState.readingModeArticle,
                            theme = when (settings?.readingModeTheme) {
                                BrowserSettings.ReadingModeTheme.LIGHT -> ReadingModeTheme.LIGHT
                                BrowserSettings.ReadingModeTheme.DARK -> ReadingModeTheme.DARK
                                BrowserSettings.ReadingModeTheme.SEPIA -> ReadingModeTheme.SEPIA
                                else -> ReadingModeTheme.LIGHT
                            },
                            fontSize = settings?.readingModeFontSize ?: 1.0f,
                            fontFamily = when (settings?.readingModeFontFamily) {
                                BrowserSettings.ReadingModeFontFamily.SYSTEM -> ReadingModeFontFamily.SYSTEM
                                BrowserSettings.ReadingModeFontFamily.SERIF -> ReadingModeFontFamily.SERIF
                                BrowserSettings.ReadingModeFontFamily.SANS_SERIF -> ReadingModeFontFamily.SANS_SERIF
                                BrowserSettings.ReadingModeFontFamily.MONOSPACE -> ReadingModeFontFamily.MONOSPACE
                                else -> ReadingModeFontFamily.SYSTEM
                            },
                            lineHeight = settings?.readingModeLineHeight ?: 1.5f,
                            onClose = {
                                tabViewModel.toggleReadingMode()
                            },
                            onThemeChange = { theme ->
                                val newSettings = settings?.copy(
                                    readingModeTheme = when (theme) {
                                        ReadingModeTheme.LIGHT -> BrowserSettings.ReadingModeTheme.LIGHT
                                        ReadingModeTheme.DARK -> BrowserSettings.ReadingModeTheme.DARK
                                        ReadingModeTheme.SEPIA -> BrowserSettings.ReadingModeTheme.SEPIA
                                    }
                                )
                                if (newSettings != null) {
                                    scope.launch {
                                        settingsViewModel.updateSettings(newSettings)
                                    }
                                }
                            },
                            onFontSizeChange = { fontSize ->
                                val newSettings = settings?.copy(readingModeFontSize = fontSize)
                                if (newSettings != null) {
                                    scope.launch {
                                        settingsViewModel.updateSettings(newSettings)
                                    }
                                }
                            },
                            onFontFamilyChange = { fontFamily ->
                                val newSettings = settings?.copy(
                                    readingModeFontFamily = when (fontFamily) {
                                        ReadingModeFontFamily.SYSTEM -> BrowserSettings.ReadingModeFontFamily.SYSTEM
                                        ReadingModeFontFamily.SERIF -> BrowserSettings.ReadingModeFontFamily.SERIF
                                        ReadingModeFontFamily.SANS_SERIF -> BrowserSettings.ReadingModeFontFamily.SANS_SERIF
                                        ReadingModeFontFamily.MONOSPACE -> BrowserSettings.ReadingModeFontFamily.MONOSPACE
                                    }
                                )
                                if (newSettings != null) {
                                    scope.launch {
                                        settingsViewModel.updateSettings(newSettings)
                                    }
                                }
                            },
                            onLineHeightChange = { lineHeight ->
                                val newSettings = settings?.copy(readingModeLineHeight = lineHeight)
                                if (newSettings != null) {
                                    scope.launch {
                                        settingsViewModel.updateSettings(newSettings)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(30f)  // Above command bar
                        )
                    }
                }
            }
        }

        // Text command input (above command bar)
        TextCommandInput(
            visible = showTextCommand,
            onCommand = { command ->
                // Handle text command
                executeTextCommand(
                    command = command,
                    webViewController = webViewController,
                    tabViewModel = tabViewModel,
                    settings = settings,
                    onBookmarks = onNavigateToBookmarks,
                    onDownloads = onNavigateToDownloads,
                    onHistory = onNavigateToHistory,
                    onSettings = onNavigateToSettings
                )
                showTextCommand = false
            },
            onDismiss = { showTextCommand = false },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )

        // Find in page bar (above command bar)
        androidx.compose.animation.AnimatedVisibility(
            visible = findInPageState.isVisible,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(21f)  // Above command bar
        ) {
            FindInPageBar(
                query = findInPageState.query,
                currentMatch = findInPageState.currentMatch,
                totalMatches = findInPageState.totalMatches,
                caseSensitive = findInPageState.caseSensitive,
                onQueryChange = { query ->
                    tabViewModel.updateFindQuery(query)
                },
                onNext = {
                    webViewController.findNext()
                    tabViewModel.findNext()
                },
                onPrevious = {
                    webViewController.findPrevious()
                    tabViewModel.findPrevious()
                },
                onCaseSensitiveToggle = {
                    tabViewModel.toggleFindCaseSensitive()
                },
                onClose = {
                    webViewController.clearFindMatches()
                    tabViewModel.hideFindInPage()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        // XR UI Overlay (Android only) - shows session indicator and warnings
        XROverlay(
            xrState = xrState,
            modifier = Modifier.fillMaxSize()
        )

        // Basic Auth Dialog
        BasicAuthDialog(
            visible = showBasicAuthDialog,
            url = authUrl,
            realm = authRealm,
            onAuthenticate = { username, password, remember ->
                // TODO: Implement authentication handling
                showBasicAuthDialog = false
            },
            onCancel = {
                showBasicAuthDialog = false
            }
        )

        // Add to Favorites Dialog - Shows add or remove based on current favorite status
        AddToFavoritesDialog(
            visible = showAddToFavoritesDialog,
            initialTitle = activeTab?.tab?.title ?: "",
            initialUrl = activeTab?.tab?.url ?: "",
            isFavorited = isFavorite,
            onSave = { title, url, description ->
                scope.launch {
                    try {
                        favoriteViewModel.addFavorite(
                            url = url,
                            title = title,
                            favicon = activeTab?.tab?.favicon,
                            description = description
                        )
                    } catch (e: Exception) {
                        println("BrowserScreen: Error saving favorite from dialog: ${e.message}")
                        e.printStackTrace()
                    }
                }
                showAddToFavoritesDialog = false
            },
            onRemove = {
                // Find and remove the favorite by URL
                val currentUrl = activeTab?.tab?.url
                if (currentUrl != null) {
                    favorites.find { it.url == currentUrl }?.let { favorite ->
                        favoriteViewModel.removeFavorite(favorite.id)
                    }
                }
                showAddToFavoritesDialog = false
            },
            onCancel = {
                showAddToFavoritesDialog = false
            }
        )

        // PHASE 3: Security Dialogs

        // SSL Error Dialog
        sslErrorState?.let { state ->
            com.augmentalis.Avanues.web.universal.presentation.ui.security.SslErrorDialog(
                sslErrorInfo = state.sslErrorInfo,
                onGoBack = state.onGoBack,
                onProceedAnyway = state.onProceedAnyway,
                onDismiss = { securityViewModel.dismissSslErrorDialog() }
            )
        }

        // Permission Request Dialog
        permissionRequestState?.let { state ->
            com.augmentalis.Avanues.web.universal.presentation.ui.security.PermissionRequestDialog(
                permissionRequest = state.permissionRequest,
                onAllow = state.onAllow,
                onDeny = state.onDeny,
                onDismiss = { securityViewModel.dismissPermissionDialog() }
            )
        }

        // JavaScript Alert Dialog
        jsAlertState?.let { state ->
            com.augmentalis.Avanues.web.universal.presentation.ui.security.JavaScriptAlertDialog(
                domain = state.domain,
                message = state.message,
                onDismiss = state.onDismiss
            )
        }

        // JavaScript Confirm Dialog
        jsConfirmState?.let { state ->
            com.augmentalis.Avanues.web.universal.presentation.ui.security.JavaScriptConfirmDialog(
                domain = state.domain,
                message = state.message,
                onConfirm = state.onConfirm,
                onCancel = state.onCancel,
                onDismiss = { securityViewModel.dismissJsConfirmDialog() }
            )
        }

        // JavaScript Prompt Dialog
        jsPromptState?.let { state ->
            com.augmentalis.Avanues.web.universal.presentation.ui.security.JavaScriptPromptDialog(
                domain = state.domain,
                message = state.message,
                defaultValue = state.defaultValue,
                onConfirm = state.onConfirm,
                onCancel = state.onCancel,
                onDismiss = { securityViewModel.dismissJsPromptDialog() }
            )
        }

        // HTTP Authentication Dialog
        httpAuthState?.let { state ->
            com.augmentalis.Avanues.web.universal.presentation.ui.security.HttpAuthenticationDialog(
                authRequest = state.authRequest,
                onAuthenticate = state.onAuthenticate,
                onCancel = state.onCancel,
                onDismiss = { securityViewModel.dismissHttpAuthDialog() }
            )
        }

        // Help FAB (?) - positioned above command bar with higher z-level
        // Shows voice commands help when tapped
        // REWRITTEN: December 2025 - Uses Ocean component system
        // Hidden in headless mode for true fullscreen browsing
        androidx.compose.animation.AnimatedVisibility(
            visible = !isHeadlessMode,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (isCommandBarVisible) 100.dp else 16.dp  // Move up when command bar visible
                )
                .zIndex(20f)  // Elevated z-level above all other UI elements
        ) {
            OceanComponents.FloatingActionButton(
                onClick = { showVoiceHelp = !showVoiceHelp },
                modifier = Modifier
            ) {
                OceanComponents.Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Voice commands help",
                    variant = IconVariant.OnPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Voice commands help dialog - categorized with submenus
        // Supports voice-based navigation: say category names to navigate
        // Interactive - clicking commands executes them
        if (showVoiceHelp && settings != null) {
            com.augmentalis.Avanues.web.universal.voice.VoiceCommandsDialog(
                onDismiss = { showVoiceHelp = false },
                onCommandExecute = { command ->
                    // Execute the command
                    when (command) {
                        // Navigation commands
                        "go back" -> webViewController.goBack()
                        "go forward" -> webViewController.goForward()
                        "go home" -> tabViewModel.navigateToUrl(settings!!.homePage)
                        "refresh" -> webViewController.reload()

                        // Scrolling commands
                        "scroll up" -> webViewController.scrollToTop()
                        "scroll down" -> webViewController.scrollToBottom()
                        "scroll to top" -> webViewController.scrollToTop()
                        "scroll to bottom" -> webViewController.scrollToBottom()
                        "page up" -> webViewController.scrollToTop()
                        "page down" -> webViewController.scrollToBottom()

                        // Tab commands
                        "new tab" -> tabViewModel.createTab()
                        "close tab" -> tabViewModel.closeTab(activeTab?.tab?.id ?: "")
                        "next tab" -> {} // TODO: implement tab switching
                        "previous tab" -> {} // TODO: implement tab switching
                        "reopen tab" -> {} // TODO: implement reopen tab

                        // Zoom commands
                        "zoom in" -> { webViewController.zoomIn(); tabViewModel.zoomIn() }
                        "zoom out" -> { webViewController.zoomOut(); tabViewModel.zoomOut() }
                        "reset zoom" -> { webViewController.setZoomLevel(100); tabViewModel.setZoomLevel(100) }

                        // Mode commands
                        "desktop mode" -> {
                            webViewController.setDesktopMode(true)
                            tabViewModel.setDesktopMode(true)
                            webViewController.reload()
                        }
                        "mobile mode" -> {
                            webViewController.setDesktopMode(false)
                            tabViewModel.setDesktopMode(false)
                            webViewController.reload()
                        }
                        "reader mode" -> {
                            // Extract article and enter reading mode
                            scope.launch {
                                // Execute JavaScript to extract article
                                val extractScript = com.augmentalis.Avanues.web.universal.util.ReadingModeExtractor.getExtractionScript()
                                webViewController.evaluateJavaScript(extractScript) { result ->
                                    // Parse JSON result and set article
                                    try {
                                        // TODO: Parse JSON and create ReadingModeArticle
                                        // For now, toggle reading mode without article (will show error)
                                        tabViewModel.toggleReadingMode()
                                    } catch (e: Exception) {
                                        println("Failed to parse article: ${e.message}")
                                    }
                                }
                            }
                        }
                        "fullscreen" -> {} // TODO: implement fullscreen

                        // Feature commands
                        "bookmark" -> showAddToFavoritesDialog = true
                        "history" -> {} // TODO: show history
                        "downloads" -> {} // TODO: show downloads
                        "settings" -> {} // TODO: show settings
                        "help" -> showVoiceHelp = true
                    }
                }
            )
        }

        // Spatial Tab Switcher (3D z-level view)
        if (showSpatialTabSwitcher) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(25f)  // Above help panel for full immersion
            ) {
                SpatialTabSwitcher(
                    tabs = tabs,
                    activeTabId = activeTab?.tab?.id,
                    onTabSelect = { tabId ->
                        tabViewModel.switchTab(tabId)
                        showSpatialTabSwitcher = false
                    },
                    onTabClose = { tabId ->
                        tabViewModel.closeTab(tabId)
                    },
                    onTabPin = { tabId ->
                        tabViewModel.togglePinned(tabId)
                    },
                    onNewTab = {
                        tabViewModel.createTab(url = "", title = "New Tab", isDesktopMode = settings?.useDesktopMode == true)
                        showSpatialTabSwitcher = false
                    },
                    onDismiss = { showSpatialTabSwitcher = false }
                )
            }
        }

        // Spatial Favorites Shelf (3D carousel view)
        if (showSpatialFavorites) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(25f)  // Above help panel for full immersion
            ) {
                SpatialFavoritesShelf(
                    favorites = favorites,
                    onFavoriteClick = { favorite ->
                        tabViewModel.navigateToUrl(favorite.url)
                        showSpatialFavorites = false
                    },
                    onDismiss = { showSpatialFavorites = false }
                )
            }
        }

        // Add Page Dialog
        if (showAddPageDialog) {
            AddPageDialog(
                url = newPageUrl,
                onUrlChange = { newPageUrl = it },
                onConfirm = {
                    if (newPageUrl.isNotBlank()) {
                        val formattedUrl = if (!newPageUrl.startsWith("http://") && !newPageUrl.startsWith("https://")) {
                            "https://$newPageUrl"
                        } else {
                            newPageUrl
                        }
                        // FIX BUG #4: Apply global desktop mode when creating new tabs
                        tabViewModel.createTab(url = formattedUrl, title = formattedUrl, isDesktopMode = settings?.useDesktopMode == true)
                    } else {
                        tabViewModel.createTab(url = "", title = "New Tab", isDesktopMode = settings?.useDesktopMode == true)
                    }
                    newPageUrl = ""
                    showAddPageDialog = false
                    showTabSwitcher = false  // FIX: Close tab switcher to focus on new tab
                },
                onDismiss = {
                    newPageUrl = ""
                    showAddPageDialog = false
                }
            )
        }

        // Tab Switcher (Chrome-like grid view) - adaptive for portrait/landscape
        if (showTabSwitcher) {
            TabSwitcherView(
                tabs = tabs,
                activeTabId = activeTab?.tab?.id,
                onTabClick = { tabId ->
                    tabViewModel.switchTab(tabId)
                },
                onTabClose = { tabId ->
                    tabViewModel.closeTab(tabId)
                },
                onTabPin = { tabId ->
                    tabViewModel.togglePinned(tabId)
                },
                onNewTab = {
                    showAddPageDialog = true
                },
                onDismiss = {
                    showTabSwitcher = false
                },
                isLandscape = isLandscape,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Tab Group Dialog (create/edit groups)
        TabGroupDialog(
            visible = showTabGroupDialog,
            onSave = { title, color ->
                val newGroup = com.augmentalis.webavanue.domain.model.TabGroup(
                    id = java.util.UUID.randomUUID().toString(),
                    title = title,
                    color = color,
                    isCollapsed = false,
                    position = tabGroups.size,
                    createdAt = kotlinx.datetime.Clock.System.now()
                )
                tabGroups = tabGroups + newGroup
                showTabGroupDialog = false
            },
            onCancel = {
                showTabGroupDialog = false
            }
        )

        // Tab Group Assignment Dialog (assign tab to group)
        TabGroupAssignmentDialog(
            visible = showTabGroupAssignmentDialog,
            tabTitle = selectedTabForGroupAssignment?.let { tabId ->
                tabs.find { it.tab.id == tabId }?.tab?.title ?: ""
            } ?: "",
            groups = tabGroups,
            currentGroupId = selectedTabForGroupAssignment?.let { tabId ->
                tabs.find { it.tab.id == tabId }?.tab?.groupId
            },
            onGroupSelected = { groupId ->
                selectedTabForGroupAssignment?.let { tabId ->
                    val tab = tabs.find { it.tab.id == tabId }?.tab
                    tab?.let {
                        val updatedTab = it.copy(groupId = groupId)
                        tabViewModel.updateTab(updatedTab)
                    }
                }
                showTabGroupAssignmentDialog = false
                selectedTabForGroupAssignment = null
            },
            onCreateNewGroup = {
                showTabGroupDialog = true
            },
            onDismiss = {
                showTabGroupAssignmentDialog = false
                selectedTabForGroupAssignment = null
            }
        )

        // Error Snackbar - displays user-friendly error messages
        // FIX: High Priority - Error user feedback (was silent failures)
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) { data ->
            androidx.compose.material3.Snackbar(
                snackbarData = data,
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer,
                actionColor = androidx.compose.material3.MaterialTheme.colorScheme.error
            )
        }

        // Listening indicator is now part of command bar, not floating overlay
    }
}

/**
 * Execute a text command
 */
private fun executeTextCommand(
    command: String,
    webViewController: WebViewController,
    tabViewModel: TabViewModel,
    settings: com.augmentalis.webavanue.domain.model.BrowserSettings?,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    val normalizedCommand = command.lowercase().trim()

    when {
        normalizedCommand == "back" || normalizedCommand == "go back" -> {
            webViewController.goBack()
        }
        normalizedCommand == "forward" || normalizedCommand == "go forward" -> {
            webViewController.goForward()
        }
        normalizedCommand == "refresh" || normalizedCommand == "reload" -> {
            webViewController.reload()
        }
        normalizedCommand == "home" || normalizedCommand == "go home" -> {
            // FIX: Use configurable home URL from settings
            val homeUrl = settings?.homePage ?: "https://www.google.com"
            tabViewModel.navigateToUrl(homeUrl)
        }
        normalizedCommand == "new tab" -> {
            // FIX BUG #4: Apply global desktop mode when creating new tabs
            tabViewModel.createTab(url = "", title = "New Tab", isDesktopMode = settings?.useDesktopMode == true)
        }
        normalizedCommand == "bookmarks" -> onBookmarks()
        normalizedCommand == "downloads" -> onDownloads()
        normalizedCommand == "history" -> onHistory()
        normalizedCommand == "settings" -> onSettings()
        normalizedCommand.startsWith("go to ") -> {
            val url = normalizedCommand.removePrefix("go to ").trim()
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            tabViewModel.navigateToUrl(formattedUrl)
        }
    }
}

/**
 * WebViewPlaceholder - Placeholder for actual WebView (to be implemented)
 *
 * @param url Current URL
 * @param isLoading Whether page is loading
 * @param modifier Modifier for customization
 */
@Composable
fun WebViewPlaceholder(
    url: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = if (url.isBlank()) "Enter a URL to browse" else url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "WebView will be integrated here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * EmptyBrowserState - Shown when no tabs are open
 *
 * @param onNewTab Callback when new tab button is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun EmptyBrowserState(
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No tabs open",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Create a new tab to start browsing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onNewTab) {
                Text("New Tab")
            }
        }
    }
}

/**
 * AddPageDialog - Dialog for adding a new page with URL input
 *
 * @param url Current URL value
 * @param onUrlChange Callback when URL changes
 * @param onConfirm Callback when user confirms (creates new tab)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun AddPageDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.augmentalis.Avanues.web.universal.presentation.ui.components.OceanDialog(
        onDismissRequest = onDismiss,
        title = "Add New Page",
        modifier = modifier,
        confirmButton = {
            com.augmentalis.Avanues.web.universal.presentation.ui.components.OceanTextButton(
                onClick = onConfirm,
                isPrimary = true
            ) {
                Text("Add Page")
            }
        },
        dismissButton = {
            com.augmentalis.Avanues.web.universal.presentation.ui.components.OceanTextButton(
                onClick = onDismiss,
                isPrimary = false
            ) {
                Text("Cancel")
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Enter a URL or leave blank for a new empty tab",
                style = MaterialTheme.typography.bodyMedium,
                color = com.augmentalis.Avanues.web.universal.presentation.ui.components.OceanDialogDefaults.textSecondary
            )

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("example.com or google.com")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onConfirm() }),
                colors = com.augmentalis.Avanues.web.universal.presentation.ui.components.OceanDialogDefaults.outlinedTextFieldColors()
            )
        }
    }
}

