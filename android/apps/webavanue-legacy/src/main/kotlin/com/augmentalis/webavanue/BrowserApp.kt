package com.augmentalis.webavanue

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.Tab
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.WebViewComposable
import com.augmentalis.webavanue.WebViewConfig
import com.augmentalis.webavanue.WebViewEvent
import com.augmentalis.webavanue.presentation.*
import kotlinx.coroutines.launch

/**
 * Main browser application composable for WebAvanue.
 * This is the entry point for the browser UI.
 */
@Composable
fun BrowserApp(
    repository: BrowserRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // State management
    val tabs by repository.observeTabs().collectAsState(initial = emptyList())
    val activeTab = tabs.firstOrNull { it.isActive }
    val favorites by repository.observeFavorites().collectAsState(initial = emptyList())
    val settings by repository.observeSettings().collectAsState(
        initial = com.augmentalis.webavanue.BrowserSettings.default()
    )

    // UI state
    var showTabManager by remember { mutableStateOf(false) }
    var showFavorites by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showVoiceCommand by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(activeTab?.url ?: Tab.DEFAULT_URL) }

    Scaffold(
        modifier = modifier,
        topBar = {
            BrowserTopBar(
                currentUrl = currentUrl,
                pageTitle = activeTab?.title ?: "",
                isLoading = false,
                isFavorite = false,
                onUrlChange = { url ->
                    currentUrl = url
                },
                onNavigateBack = {
                    // WebView will handle
                },
                onNavigateForward = {
                    // WebView will handle
                },
                onReload = {
                    // WebView will handle
                },
                onToggleFavorite = {
                    scope.launch {
                        if (activeTab != null) {
                            val isFav = repository.isFavorite(activeTab.url).getOrElse { false }
                            if (isFav) {
                                // Remove favorite
                                favorites.firstOrNull { it.url == activeTab.url }?.let {
                                    repository.removeFavorite(it.id)
                                }
                            } else {
                                // Add favorite
                                repository.addFavorite(
                                    com.augmentalis.webavanue.Favorite.create(
                                        url = activeTab.url,
                                        title = activeTab.title
                                    )
                                )
                            }
                        }
                    }
                },
                onVoiceCommand = {
                    showVoiceCommand = true
                }
            )
        },
        bottomBar = {
            BrowserBottomBar(
                tabCount = tabs.size,
                onTabsClick = {
                    showTabManager = true
                },
                onFavoritesClick = {
                    showFavorites = true
                },
                onSettingsClick = {
                    showSettings = true
                },
                onNewTab = {
                    scope.launch {
                        repository.createTab(
                            Tab.create(
                                url = settings.homePage,
                                isIncognito = false
                            )
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (activeTab != null) {
                WebViewComposable(
                    url = currentUrl,
                    modifier = Modifier.fillMaxSize(),
                    config = WebViewConfig(
                        javaScriptEnabled = settings.enableJavaScript,
                        domStorageEnabled = true,
                        blockNetworkImage = !settings.showImages,
                        safeBrowsingEnabled = true,
                        textZoom = when (settings.fontSize) {
                            com.augmentalis.webavanue.BrowserSettings.FontSize.TINY -> 75
                            com.augmentalis.webavanue.BrowserSettings.FontSize.SMALL -> 87
                            com.augmentalis.webavanue.BrowserSettings.FontSize.MEDIUM -> 100
                            com.augmentalis.webavanue.BrowserSettings.FontSize.LARGE -> 112
                            com.augmentalis.webavanue.BrowserSettings.FontSize.HUGE -> 125
                        }
                    ),
                    onEvent = { event ->
                        when (event) {
                            is WebViewEvent.PageFinished -> {
                                currentUrl = event.url
                                scope.launch {
                                    activeTab?.let { tab ->
                                        repository.updateTab(
                                            tab.copy(url = event.url)
                                        )
                                    }
                                }
                            }
                            is WebViewEvent.TitleReceived -> {
                                scope.launch {
                                    activeTab?.let { tab ->
                                        repository.updateTab(
                                            tab.copy(title = event.title)
                                        )
                                    }
                                }
                            }
                            is WebViewEvent.DownloadRequest -> {
                                // Handle download
                            }
                            else -> {}
                        }
                    }
                )
            } else {
                // No tabs open - show welcome screen
                WelcomeScreen(
                    onNewTab = {
                        scope.launch {
                            repository.createTab(
                                Tab.create(
                                    url = settings.homePage,
                                    isIncognito = false
                                )
                            )
                        }
                    },
                    onNewIncognitoTab = {
                        scope.launch {
                            repository.createTab(
                                Tab.create(
                                    url = settings.homePage,
                                    isIncognito = true
                                )
                            )
                        }
                    }
                )
            }
        }

        // Dialogs and overlays
        if (showTabManager) {
            TabManagerDialog(
                tabs = tabs,
                onTabSelect = { tab ->
                    scope.launch {
                        repository.setActiveTab(tab.id)
                        currentUrl = tab.url
                        showTabManager = false
                    }
                },
                onTabClose = { tab ->
                    scope.launch {
                        repository.closeTab(tab.id)
                    }
                },
                onDismiss = {
                    showTabManager = false
                }
            )
        }

        if (showFavorites) {
            FavoritesDialog(
                favorites = favorites,
                onFavoriteSelect = { favorite ->
                    currentUrl = favorite.url
                    showFavorites = false
                },
                onFavoriteDelete = { favorite ->
                    scope.launch {
                        repository.removeFavorite(favorite.id)
                    }
                },
                onDismiss = {
                    showFavorites = false
                }
            )
        }

        if (showSettings) {
            SettingsDialog(
                settings = settings,
                onSettingsUpdate = { updatedSettings ->
                    scope.launch {
                        repository.updateSettings(updatedSettings)
                    }
                },
                onDismiss = {
                    showSettings = false
                }
            )
        }

        if (showVoiceCommand) {
            VoiceCommandDialog(
                onCommand = { command ->
                    // Process voice command
                    handleVoiceCommand(command, repository, currentUrl) { newUrl ->
                        currentUrl = newUrl
                    }
                    showVoiceCommand = false
                },
                onDismiss = {
                    showVoiceCommand = false
                }
            )
        }
    }
}

/**
 * Handles voice commands
 */
private fun handleVoiceCommand(
    command: String,
    repository: BrowserRepository,
    currentUrl: String,
    onNavigate: (String) -> Unit
) {
    val lowerCommand = command.lowercase()
    when {
        lowerCommand.contains("search") -> {
            val query = command.substringAfter("search").trim()
            onNavigate("https://www.google.com/search?q=$query")
        }
        lowerCommand.contains("go to") -> {
            val site = command.substringAfter("go to").trim()
            val url = if (site.startsWith("http")) site else "https://$site"
            onNavigate(url)
        }
        lowerCommand.contains("bookmark") -> {
            // Add to favorites
        }
        lowerCommand.contains("new tab") -> {
            // Create new tab
        }
        else -> {
            // Default: search for the command
            onNavigate("https://www.google.com/search?q=$command")
        }
    }
}

/**
 * Welcome screen when no tabs are open
 */
@Composable
private fun WelcomeScreen(
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to WebAvanue",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your cross-platform browser powered by Kotlin Multiplatform",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onNewTab) {
                Text("New Tab")
            }
            OutlinedButton(onClick = onNewIncognitoTab) {
                Text("Incognito Tab")
            }
        }
    }
}