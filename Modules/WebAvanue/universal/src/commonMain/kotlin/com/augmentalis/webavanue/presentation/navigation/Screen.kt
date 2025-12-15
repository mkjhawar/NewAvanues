package com.augmentalis.webavanue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import com.augmentalis.webavanue.ui.screen.bookmark.BookmarkListScreen
import com.augmentalis.webavanue.ui.screen.browser.BrowserScreen
import com.augmentalis.webavanue.ui.screen.download.DownloadListScreen
import com.augmentalis.webavanue.ui.screen.history.HistoryScreen
import com.augmentalis.webavanue.ui.screen.settings.SettingsScreen
import com.augmentalis.webavanue.ui.viewmodel.*

/**
 * ViewModelHolder - Holds all ViewModels for the browser app
 * Used to pass ViewModels through Voyager navigation
 */
data class ViewModelHolder(
    val repository: BrowserRepository,
    val tabViewModel: TabViewModel,
    val favoriteViewModel: FavoriteViewModel,
    val downloadViewModel: DownloadViewModel,
    val historyViewModel: HistoryViewModel,
    val settingsViewModel: SettingsViewModel,
    val securityViewModel: SecurityViewModel // PHASE 3
) {
    companion object {
        fun create(
            repository: BrowserRepository,
            secureStorage: SecureStorageProvider? = null,
            downloadQueue: com.augmentalis.webavanue.feature.download.DownloadQueue? = null
        ): ViewModelHolder {
            val securityViewModel = SecurityViewModel(repository, secureStorage)

            return ViewModelHolder(
                repository = repository,
                tabViewModel = TabViewModel(repository),
                favoriteViewModel = FavoriteViewModel(repository),
                downloadViewModel = DownloadViewModel(repository, downloadQueue),
                historyViewModel = HistoryViewModel(repository),
                settingsViewModel = SettingsViewModel(repository),
                securityViewModel = securityViewModel // PHASE 3
            )
        }
    }
}

/**
 * BrowserScreenNav - Main browser screen with tab bar and WebView
 */
data class BrowserScreenNav(
    val viewModels: ViewModelHolder,
    val xrManager: Any? = null,  // XRManager on Android, null on other platforms
    val xrState: Any? = null     // XRManager.XRState on Android, null on other platforms
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        BrowserScreen(
            tabViewModel = viewModels.tabViewModel,
            settingsViewModel = viewModels.settingsViewModel,
            historyViewModel = viewModels.historyViewModel,
            favoriteViewModel = viewModels.favoriteViewModel,
            securityViewModel = viewModels.securityViewModel, // PHASE 3
            downloadViewModel = viewModels.downloadViewModel, // FIX Issue #5: Pass downloadViewModel for download tracking
            xrManager = xrManager,
            xrState = xrState,
            onNavigateToBookmarks = {
                navigator.push(BookmarksScreenNav(viewModels))
            },
            onNavigateToDownloads = {
                navigator.push(DownloadsScreenNav(viewModels))
            },
            onNavigateToHistory = {
                navigator.push(HistoryScreenNav(viewModels))
            },
            onNavigateToSettings = {
                navigator.push(SettingsScreenNav(viewModels))
            },
            onNavigateToXRSettings = {
                navigator.push(XRSettingsScreenNav(viewModels))
            }
        )
    }
}

/**
 * BookmarksScreenNav - Bookmark/Favorite management screen
 */
data class BookmarksScreenNav(
    val viewModels: ViewModelHolder,
    val folderId: String? = null
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Filter by folder if specified
        if (folderId != null) {
            viewModels.favoriteViewModel.filterByFolder(folderId)
        }

        BookmarkListScreen(
            viewModel = viewModels.favoriteViewModel,
            onNavigateBack = {
                if (folderId != null) {
                    viewModels.favoriteViewModel.filterByFolder(null)
                }
                navigator.pop()
            },
            onBookmarkClick = { url ->
                viewModels.tabViewModel.navigateToUrl(url)
                navigator.popUntilRoot()
            }
        )
    }
}

/**
 * DownloadsScreenNav - Download management screen
 */
data class DownloadsScreenNav(
    val viewModels: ViewModelHolder
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        DownloadListScreen(
            viewModel = viewModels.downloadViewModel,
            onNavigateBack = {
                navigator.pop()
            },
            onDownloadClick = { download ->
                // TODO: Open downloaded file
            }
        )
    }
}

/**
 * HistoryScreenNav - Browsing history screen
 */
data class HistoryScreenNav(
    val viewModels: ViewModelHolder
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        HistoryScreen(
            viewModel = viewModels.historyViewModel,
            onNavigateBack = {
                navigator.pop()
            },
            onHistoryClick = { url ->
                viewModels.tabViewModel.navigateToUrl(url)
                navigator.popUntilRoot()
            }
        )
    }
}

/**
 * SettingsScreenNav - Browser settings screen
 */
data class SettingsScreenNav(
    val viewModels: ViewModelHolder
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        SettingsScreen(
            viewModel = viewModels.settingsViewModel,
            onNavigateBack = {
                navigator.pop()
            }
        )
    }
}

/**
 * XRSettingsScreenNav - WebXR settings screen
 * Note: XR features only work on Android, but UI is cross-platform
 */
data class XRSettingsScreenNav(
    val viewModels: ViewModelHolder
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settings by viewModels.settingsViewModel.settings.collectAsState()

        // Only show XR settings if settings are loaded
        settings?.let { currentSettings ->
            com.augmentalis.webavanue.ui.screen.xr.XRSettingsScreen(
                settings = currentSettings,
                onSettingsChange = { newSettings ->
                    viewModels.settingsViewModel.updateSettings(newSettings)
                },
                onNavigateBack = {
                    navigator.pop()
                }
            )
        }
    }
}

/**
 * AboutScreenNav - About screen (app info, version, licenses)
 */
data class AboutScreenNav(
    val viewModels: ViewModelHolder
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // TODO: Create AboutScreen composable
        // For now, just pop back
        navigator.pop()
    }
}

/**
 * ARPreviewScreenNav - AR Layout Preview screen for testing spatial arc layout
 */
data class ARPreviewScreenNav(
    val viewModels: ViewModelHolder
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        com.augmentalis.webavanue.ui.screen.demo.ARLayoutPreview(
            onBack = {
                navigator.pop()
            }
        )
    }
}
