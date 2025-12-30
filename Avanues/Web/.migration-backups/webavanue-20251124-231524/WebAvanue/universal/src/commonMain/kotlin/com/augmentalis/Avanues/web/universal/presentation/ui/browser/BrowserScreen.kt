package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.Avanues.web.universal.presentation.ui.tab.TabBar
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabViewModel

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
    val settings by settingsViewModel.settings.collectAsState()
    val favorites by favoriteViewModel.favorites.collectAsState()
    var urlInput by remember { mutableStateOf("") }
    val webViewController = remember { WebViewController() }

    // Voice-first UI state
    var isListening by remember { mutableStateOf(false) }
    var showTextCommand by remember { mutableStateOf(false) }
    var showVoiceHelp by remember { mutableStateOf(false) }
    var isScrollFrozen by remember { mutableStateOf(false) }

    // Desktop mode comes from active tab
    val isDesktopMode = activeTab?.tab?.isDesktopMode ?: false

    // Add Page dialog state
    var showAddPageDialog by remember { mutableStateOf(false) }
    var newPageUrl by remember { mutableStateOf("") }

    // Basic Auth dialog state
    var showBasicAuthDialog by remember { mutableStateOf(false) }
    var authUrl by remember { mutableStateOf("") }
    var authRealm by remember { mutableStateOf<String?>(null) }

    // Add to Favorites dialog state
    var showAddToFavoritesDialog by remember { mutableStateOf(false) }

    // Check if current URL is favorited
    val isFavorite = remember(activeTab, favorites) {
        activeTab?.tab?.url?.let { url ->
            favorites.any { it.url == url }
        } ?: false
    }

    // Update URL input when active tab changes
    LaunchedEffect(activeTab) {
        urlInput = activeTab?.tab?.url ?: ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main content column
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tab bar
            TabBar(
                viewModel = tabViewModel,
                onNewTab = {
                    // FIX: Show dialog to prompt for URL instead of creating blank tab
                    showAddPageDialog = true
                }
            )

            // Address bar
            AddressBar(
                url = urlInput,
                canGoBack = activeTab?.canGoBack ?: false,
                canGoForward = activeTab?.canGoForward ?: false,
                isDesktopMode = isDesktopMode,
                isFavorite = isFavorite,
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
                },
                onFavoriteClick = {
                    showAddToFavoritesDialog = true
                },
                onBookmarkClick = onNavigateToBookmarks,
                onDownloadClick = onNavigateToDownloads,
                onHistoryClick = onNavigateToHistory,
                onSettingsClick = onNavigateToSettings
            )

            // Favorites bar
            FavoritesBar(
                favorites = favorites,
                onFavoriteClick = { favorite ->
                    tabViewModel.navigateToUrl(favorite.url)
                },
                onFavoriteLongPress = { favorite ->
                    // TODO: Show edit/delete dialog
                },
                onAddFavorite = {
                    // Add current page to favorites
                    activeTab?.let { tab ->
                        favoriteViewModel.addFavorite(
                            url = tab.tab.url,
                            title = tab.tab.title.ifBlank { tab.tab.url },
                            favicon = tab.tab.favicon
                        )
                    }
                }
            )

            // WebView container - fills remaining space
            // FIX: Use key(activeTab.tab.id) to ensure each tab gets its own WebView instance
            // This prevents tabs from sharing the same WebView which caused:
            // 1. Clicking Tab 2 loading its URL in Tab 1
            // 2. All tabs showing the same content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                activeTab?.let { tabState ->
                    key(tabState.tab.id) { // FIX: Force recomposition when tab changes
                        WebViewContainer(
                            url = tabState.tab.url,
                            controller = webViewController,
                            onUrlChange = { newUrl ->
                                tabViewModel.updateTabUrl(tabState.tab.id, newUrl)
                                urlInput = newUrl
                            },
                            onLoadingChange = { isLoading ->
                                tabViewModel.updateTabLoading(tabState.tab.id, isLoading)
                            },
                            onTitleChange = { title ->
                                tabViewModel.updateTabTitle(tabState.tab.id, title)
                                // FIX: Add history entry when page title is loaded (indicates page successfully loaded)
                                if (title.isNotBlank() && tabState.tab.url.isNotBlank()) {
                                    historyViewModel.addHistoryEntry(
                                        url = tabState.tab.url,
                                        title = title
                                    )
                                }
                            },
                            onProgressChange = { progress ->
                                // TODO: Show progress bar
                            },
                            canGoBack = { canGoBack ->
                                tabViewModel.updateTabNavigation(tabState.tab.id, canGoBack = canGoBack)
                            },
                            canGoForward = { canGoForward ->
                                tabViewModel.updateTabNavigation(tabState.tab.id, canGoForward = canGoForward)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } ?: EmptyBrowserState(
                    onNewTab = { tabViewModel.createTab() },
                    modifier = Modifier.fillMaxSize()
                )
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

        // Bottom Command Bar (Voice-first)
        BottomCommandBar(
            onBack = { webViewController.goBack() },
            onForward = { webViewController.goForward() },
            onHome = {
                // FIX: Use configurable home URL from settings instead of about:blank
                val homeUrl = settings?.homePage ?: "https://www.google.com"
                tabViewModel.navigateToUrl(homeUrl)
            },
            onRefresh = { webViewController.reload() },
            onScrollUp = { webViewController.scrollUp() },
            onScrollDown = { webViewController.scrollDown() },
            onScrollTop = { webViewController.scrollToTop() },
            onScrollBottom = { webViewController.scrollToBottom() },
            onScrollLeft = { webViewController.scrollLeft() },
            onScrollRight = { webViewController.scrollRight() },
            onSelect = { webViewController.performClick() },
            onDoubleClick = { webViewController.performDoubleClick() },
            onVoice = { isListening = !isListening },
            onTextCommand = { showTextCommand = !showTextCommand },
            onBookmarks = onNavigateToBookmarks,
            onDownloads = onNavigateToDownloads,
            onHistory = onNavigateToHistory,
            onSettings = onNavigateToSettings,
            onDesktopModeToggle = {
                val newMode = !isDesktopMode
                webViewController.setDesktopMode(newMode)
                tabViewModel.setDesktopMode(newMode)
            },
            onZoomIn = {
                webViewController.zoomIn()
                tabViewModel.zoomIn()
            },
            onZoomOut = {
                webViewController.zoomOut()
                tabViewModel.zoomOut()
            },
            onZoomLevel = { level ->
                webViewController.setZoomLevel(level)
                tabViewModel.setZoomLevel(level)
            },
            onClearCookies = { webViewController.clearCookies() },
            onClearCache = { webViewController.clearCache() },
            onFavorite = {
                // Add current page to favorites (same as Add button in FavoritesBar)
                activeTab?.let { tab ->
                    favoriteViewModel.addFavorite(
                        url = tab.tab.url,
                        title = tab.tab.title.ifBlank { tab.tab.url },
                        favicon = tab.tab.favicon
                    )
                }
            },
            onDragToggle = { /* TODO: Implement drag mode */ },
            onRotateImage = { /* TODO: Implement rotate */ },
            onPinchOpen = { /* TODO: Implement pinch open */ },
            onPinchClose = { /* TODO: Implement pinch close */ },
            onFreezePage = {
                isScrollFrozen = !isScrollFrozen
                webViewController.setScrollFrozen(isScrollFrozen)
            },
            onPreviousTab = {
                val tabs = tabViewModel.tabs.value
                val currentIndex = tabs.indexOfFirst { it.tab.id == activeTab?.tab?.id }
                if (currentIndex > 0) {
                    tabViewModel.switchTab(tabs[currentIndex - 1].tab.id)
                }
            },
            onNextTab = {
                val tabs = tabViewModel.tabs.value
                val currentIndex = tabs.indexOfFirst { it.tab.id == activeTab?.tab?.id }
                if (currentIndex < tabs.size - 1) {
                    tabViewModel.switchTab(tabs[currentIndex + 1].tab.id)
                }
            },
            onNewTab = {
                // FIX: Show dialog to prompt for URL
                showAddPageDialog = true
            },
            onCloseTab = { activeTab?.tab?.id?.let { tabViewModel.closeTab(it) } },
            isListening = isListening,
            isDesktopMode = isDesktopMode,
            isScrollFrozen = isScrollFrozen,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

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

        // Add to Favorites Dialog
        AddToFavoritesDialog(
            visible = showAddToFavoritesDialog,
            initialTitle = activeTab?.tab?.title ?: "",
            initialUrl = activeTab?.tab?.url ?: "",
            onSave = { title, url, description ->
                favoriteViewModel.addFavorite(
                    url = url,
                    title = title,
                    favicon = activeTab?.tab?.favicon,
                    description = description
                )
                showAddToFavoritesDialog = false
            },
            onCancel = {
                showAddToFavoritesDialog = false
            }
        )

        // Voice help button (bottom right)
        IconButton(
            onClick = { showVoiceHelp = !showVoiceHelp },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .size(48.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = androidx.compose.foundation.shape.CircleShape,
                shadowElevation = 4.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Voice commands help",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }
        }

        // Voice commands help panel
        VoiceCommandsPanel(
            visible = showVoiceHelp,
            onDismiss = { showVoiceHelp = false },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 150.dp)
        )

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
                        tabViewModel.createTab(url = formattedUrl, title = formattedUrl)
                    } else {
                        tabViewModel.createTab(url = "", title = "New Tab")
                    }
                    newPageUrl = ""
                    showAddPageDialog = false
                },
                onDismiss = {
                    newPageUrl = ""
                    showAddPageDialog = false
                }
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
            tabViewModel.createTab(url = "", title = "New Tab")
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
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Page",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Enter a URL or leave blank for a new empty tab",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("example.com or google.com")
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Add Page")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

