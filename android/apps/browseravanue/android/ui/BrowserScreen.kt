package com.augmentalis.browseravanue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.browseravanue.presentation.BrowserEvent
import com.augmentalis.browseravanue.presentation.BrowserState
import com.augmentalis.browseravanue.presentation.BrowserViewModel
import com.augmentalis.browseravanue.webview.BrowserWebViewCompose

/**
 * Main browser screen
 *
 * Architecture:
 * - Single activity/screen design
 * - Observes ViewModel state
 * - Renders based on state
 * - Handles all browser UI
 *
 * Layout:
 * ```
 * ┌─────────────────────────┐
 * │   Top Bar (URL + Menu)  │
 * ├─────────────────────────┤
 * │                         │
 * │                         │
 * │      WebView            │
 * │      (Main Content)     │
 * │                         │
 * │                         │
 * ├─────────────────────────┤
 * │  Bottom Bar (Nav)       │
 * └─────────────────────────┘
 * ```
 *
 * Features:
 * - WebView with all enhancements
 * - URL bar with search
 * - Tab switcher overlay
 * - Favorites overlay
 * - Settings overlay
 * - Find in page bar
 * - Progress indicator
 * - Dialogs (SSL, auth, permissions)
 *
 * State Management:
 * - Observes ViewModel.state via collectAsState()
 * - Emits events via viewModel.onEvent()
 * - Recomposes on state changes
 *
 * Usage:
 * ```
 * BrowserScreen(viewModel = viewModel)
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    when (val currentState = state) {
        is BrowserState.Loading -> {
            LoadingScreen()
        }
        is BrowserState.Success -> {
            BrowserContent(
                state = currentState,
                onEvent = viewModel::onEvent,
                modifier = modifier
            )
        }
        is BrowserState.Error -> {
            ErrorScreen(
                message = currentState.message,
                onRetry = { /* TODO: Reload */ }
            )
        }
    }
}

/**
 * Main browser content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserContent(
    state: BrowserState.Success,
    onEvent: (BrowserEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var urlBarText by remember(state.currentTab?.url) {
        mutableStateOf(state.currentTab?.url ?: "")
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrowserTopBar(
                urlText = urlBarText,
                onUrlTextChange = { urlBarText = it },
                onSearch = {
                    onEvent(BrowserEvent.NavigateToUrl(urlBarText))
                },
                onMenuClick = { /* TODO: Show menu */ },
                pageLoading = state.pageLoading,
                pageProgress = state.pageProgress
            )
        },
        bottomBar = {
            BrowserBottomBar(
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                tabCount = state.tabs.size,
                onBackClick = { onEvent(BrowserEvent.GoBack) },
                onForwardClick = { onEvent(BrowserEvent.GoForward) },
                onTabsClick = { onEvent(BrowserEvent.ToggleTabSwitcher) },
                onFavoritesClick = { onEvent(BrowserEvent.ToggleFavorites) },
                onMenuClick = { /* TODO: Show menu */ }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main WebView
            state.currentTab?.let { tab ->
                BrowserWebViewCompose(
                    tab = tab,
                    onPageStarted = { url ->
                        onEvent(BrowserEvent.PageStarted(url))
                    },
                    onPageFinished = { url, title ->
                        onEvent(BrowserEvent.PageFinished(url, title))
                        urlBarText = url // Update URL bar
                    },
                    onProgressChanged = { progress ->
                        onEvent(BrowserEvent.ProgressChanged(progress))
                    },
                    onSslError = { error, handler ->
                        onEvent(BrowserEvent.SslErrorOccurred(error, handler))
                    },
                    onDownloadStart = { url, userAgent, contentDisposition, mimeType, contentLength ->
                        onEvent(BrowserEvent.DownloadStarted(url, userAgent, contentDisposition, mimeType, contentLength))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } ?: run {
                // No tab - show empty state
                EmptyTabScreen(
                    onNewTab = { onEvent(BrowserEvent.NewTab()) }
                )
            }

            // Find in page bar (overlay at bottom)
            if (state.findInPageActive) {
                FindInPageBar(
                    query = state.findInPageQuery,
                    currentMatch = state.findInPageCurrentMatch,
                    totalMatches = state.findInPageTotalMatches,
                    onQueryChange = { query ->
                        onEvent(BrowserEvent.FindInPage(query))
                    },
                    onNext = { onEvent(BrowserEvent.FindNext) },
                    onPrevious = { onEvent(BrowserEvent.FindPrevious) },
                    onClose = { onEvent(BrowserEvent.CloseFindInPage) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }

            // Overlays
            if (state.showTabSwitcher) {
                TabSwitcherOverlay(
                    tabs = state.tabs,
                    currentTabIndex = state.currentTabIndex,
                    onTabClick = { tab ->
                        onEvent(BrowserEvent.SwitchTab(tab.id))
                    },
                    onTabClose = { tab ->
                        onEvent(BrowserEvent.CloseTab(tab.id))
                    },
                    onNewTab = { onEvent(BrowserEvent.NewTab()) },
                    onDismiss = { onEvent(BrowserEvent.ToggleTabSwitcher) }
                )
            }

            if (state.showFavorites) {
                FavoritesOverlay(
                    favorites = state.favorites,
                    onFavoriteClick = { favorite ->
                        onEvent(BrowserEvent.OpenFavorite(favorite.id))
                    },
                    onFavoriteDelete = { favorite ->
                        onEvent(BrowserEvent.RemoveFavorite(favorite.id))
                    },
                    onDismiss = { onEvent(BrowserEvent.ToggleFavorites) }
                )
            }

            if (state.showSettings) {
                SettingsOverlay(
                    settings = state.settings,
                    onSettingsChange = { settings ->
                        onEvent(BrowserEvent.UpdateSettings(settings))
                    },
                    onDismiss = { onEvent(BrowserEvent.ToggleSettings) }
                )
            }

            // Dialogs
            if (state.showSslErrorDialog) {
                SslErrorDialog(
                    message = state.sslErrorMessage ?: "SSL Certificate Error",
                    onProceed = { onEvent(BrowserEvent.SslErrorDecision(true)) },
                    onCancel = { onEvent(BrowserEvent.SslErrorDecision(false)) }
                )
            }

            if (state.showAuthDialog) {
                AuthenticationDialog(
                    host = state.authHost ?: "",
                    realm = state.authRealm,
                    onSubmit = { username, password ->
                        onEvent(BrowserEvent.AuthenticationCredentials(username, password))
                    },
                    onCancel = { onEvent(BrowserEvent.DismissDialog) }
                )
            }
        }
    }
}

/**
 * Top bar with URL and menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserTopBar(
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onMenuClick: () -> Unit,
    pageLoading: Boolean,
    pageProgress: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                MagicSearchField(
                    value = urlText,
                    onValueChange = onUrlTextChange,
                    onSearch = { onSearch() },
                    placeholder = "Search or enter URL",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            },
            actions = {
                MagicIconButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    },
                    onClick = onMenuClick
                )
            }
        )

        // Progress indicator
        if (pageLoading) {
            MagicLinearProgress(
                progress = pageProgress / 100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Bottom navigation bar
 */
@Composable
private fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onTabsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagicBottomBar(modifier = modifier) {
        Spacer(modifier = Modifier.weight(0.5f))

        // Back
        MagicIconButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            },
            onClick = onBackClick,
            enabled = canGoBack
        )

        Spacer(modifier = Modifier.weight(1f))

        // Forward
        MagicIconButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Forward"
                )
            },
            onClick = onForwardClick,
            enabled = canGoForward
        )

        Spacer(modifier = Modifier.weight(1f))

        // Tabs (with count badge)
        BadgedBox(
            badge = {
                Badge {
                    Text(tabCount.toString())
                }
            }
        ) {
            MagicIconButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.AccountBox, // TODO: Better tabs icon
                        contentDescription = "Tabs"
                    )
                },
                onClick = onTabsClick
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Favorites
        MagicIconButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorites"
                )
            },
            onClick = onFavoritesClick
        )

        Spacer(modifier = Modifier.weight(1f))

        // Menu
        MagicIconButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu"
                )
            },
            onClick = onMenuClick
        )

        Spacer(modifier = Modifier.weight(0.5f))
    }
}

/**
 * Find in page bar
 */
@Composable
private fun FindInPageBar(
    query: String,
    currentMatch: Int,
    totalMatches: Int,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagicSurface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MagicSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search field
            MagicTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Find in page",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            MagicSpacer(SpacingSize.SMALL)

            // Match count
            Text(
                text = if (totalMatches > 0) "$currentMatch/$totalMatches" else "0/0",
                style = MaterialTheme.typography.bodySmall
            )

            MagicSpacer(SpacingSize.SMALL)

            // Previous
            MagicIconButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous"
                    )
                },
                onClick = onPrevious,
                enabled = totalMatches > 0
            )

            // Next
            MagicIconButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next"
                    )
                },
                onClick = onNext,
                enabled = totalMatches > 0
            )

            // Close
            MagicIconButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                },
                onClick = onClose
            )
        }
    }
}

/**
 * Empty tab screen (no tabs open)
 */
@Composable
private fun EmptyTabScreen(
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "No tabs open",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Open a new tab to start browsing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MagicSpacer(SpacingSize.MEDIUM)

            MagicButton(
                text = "New Tab",
                onClick = onNewTab
            )
        }
    }
}

/**
 * Loading screen
 */
@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)
        ) {
            MagicCircularProgress()
            Text(
                text = "Loading Browser...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Error screen
 */
@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MagicSpacer(SpacingSize.MEDIUM)

            MagicButton(
                text = "Retry",
                onClick = onRetry
            )
        }
    }
}

/**
 * SSL error dialog
 */
@Composable
private fun SslErrorDialog(
    message: String,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    MagicAlertDialog(
        title = "Security Warning",
        message = "$message\n\nThis site's security certificate is not trusted. Your connection may not be secure.\n\nProceed anyway?",
        confirmText = "Proceed (Unsafe)",
        onConfirm = onProceed,
        dismissText = "Go Back (Safe)",
        onDismissAction = onCancel,
        onDismiss = onCancel
    )
}

/**
 * Authentication dialog
 */
@Composable
private fun AuthenticationDialog(
    host: String,
    realm: String?,
    onSubmit: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    MagicDialog(onDismiss = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MagicSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)
        ) {
            Text(
                text = "Authentication Required",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "The site $host requires authentication",
                style = MaterialTheme.typography.bodyMedium
            )

            realm?.let {
                Text(
                    text = "Realm: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            MagicTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                singleLine = true
            )

            MagicTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MagicSpacing.sm, Alignment.End)
            ) {
                MagicTextButton(
                    text = "Cancel",
                    onClick = onCancel
                )

                MagicButton(
                    text = "Sign In",
                    onClick = { onSubmit(username, password) },
                    enabled = username.isNotBlank()
                )
            }
        }
    }
}
